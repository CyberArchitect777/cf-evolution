/*
 * CF Evolution: An editor for Formula One Grand Prix/World Circuit
 * Copyright (C) 2005-2007  The Chequered Flag Development Team
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/

package cfevolution.data.executable;

import java.io.*;

/**
 * The game's flat images ("bitmaps"): tyres, road signs, trees, marshals, the
 * pit crew, the drivers' helmets and the far-away car pictures. They live in
 * F1GPDATA.DAT and F1GPDATB.DAT in the game folder, not in the executable,
 * and each pixel holds a SLOT (0-15) in a 16-byte colour set rather than a
 * colour, which is how one picture is drawn in every team's colours.
 *
 * Format, read from GP.EXE's RenderBitmap (IDA 0x10E58) and
 * SetupF1gpDatABAddresses (0x10CB1):
 *   F1GPDATA.DAT starts with 230 entries of 4 bytes: a 16-bit offset, then a
 *   word whose top bit means the image is in F1GPDATB.DAT.
 *   An image: word size (negative without bit 4000h = same as image low byte),
 *   word rows * 2, word half-width, word rows below the anchor point, then a word
 *   per row (bottom row first) giving that row's offset from the image start.
 *   A row: a control byte (0 ends the row, slot = (control & 7Eh) / 2 - 2),
 *   then a signed start column and a signed end column, relative to the anchor;
 *   each further control byte either starts a new run (top bit set: start and
 *   end follow) or continues from the last end (only an end follows).
 *
 * Which images matter here (checked against the stock colours): cars from
 * behind to in front along one side, 176-220 (team car colours); helmets from
 * behind to in front, 66-74 (helmet colours); the pit crew, 155-162 and
 * 166-169 (pit crew colours). The game mirrors them for the other side.
 *
 * @author barrie
 */
public class GameSprites
{
    public static final int COUNT = 230;
    public static final int CAR_FIRST = 176, CAR_LAST = 220;
    public static final int HELMET_FIRST = 66, HELMET_LAST = 74;
    public static final int PIT_CREW_MAIN = 169;
    public static final int[] PIT_CREW_POSES = { 157, 160, 166, 167 };

    /** One decoded image: a slot per pixel, -1 where nothing is drawn. */
    public static class Sprite
    {
        public final int width, height;
        public final byte[] slots;

        Sprite(int width, int height, byte[] slots)
        {
            this.width = width;
            this.height = height;
            this.slots = slots;
        }

        public int slotAt(int x, int y)
        {
            if (x < 0 || y < 0 || x >= width || y >= height)
                return -1;
            return slots[y * width + x];
        }
    }

    private final byte[] dataA, dataB;
    private final File folder;

    private GameSprites(byte[] dataA, byte[] dataB, File folder)
    {
        this.dataA = dataA;
        this.dataB = dataB;
        this.folder = folder;
    }

    public File getFolder()
    {
        return folder;
    }

    /**
     * Looks for the two files in each folder given, in order, and returns the
     * first pair that reads correctly, or null.
     */
    public static GameSprites find(File... folders)
    {
        for (File folder : folders)
        {
            if (folder == null || !folder.isDirectory())
                continue;
            File a = fileIgnoringCase(folder, "F1GPDATA.DAT");
            File b = fileIgnoringCase(folder, "F1GPDATB.DAT");
            if (a == null || b == null)
                continue;
            try
            {
                GameSprites sprites = new GameSprites(readAll(a), readAll(b), folder);
                if (sprites.isValid())
                    return sprites;
            }
            catch (IOException e)
            {
                // try the next folder
            }
        }
        return null;
    }

    private static File fileIgnoringCase(File folder, String name)
    {
        String[] names = folder.list();
        if (names == null)
            return null;
        for (String n : names)
            if (n.equalsIgnoreCase(name))
                return new File(folder, n);
        return null;
    }

    private static byte[] readAll(File f) throws IOException
    {
        byte[] data = new byte[(int) f.length()];
        DataInputStream in = new DataInputStream(new FileInputStream(f));
        try
        {
            in.readFully(data);
        }
        finally
        {
            in.close();
        }
        return data;
    }

    /** Every image the editor shows must decode, or the files are not the game's. */
    private boolean isValid()
    {
        if (dataA.length < COUNT * 4)
            return false;
        int[] needed = { CAR_FIRST, CAR_LAST, HELMET_FIRST, HELMET_LAST, PIT_CREW_MAIN };
        for (int i : needed)
        {
            Sprite s = get(i);
            if (s == null || s.width < 4 || s.height < 4)
                return false;
        }
        return true;
    }

    private static int word(byte[] d, int o)
    {
        return (d[o] & 0xFF) | ((d[o + 1] & 0xFF) << 8);
    }

    /** Decodes image {@code index}, or returns null if it is empty or unreadable. */
    public Sprite get(int index)
    {
        try
        {
            byte[] d = null;
            int o = 0;
            for (int hops = 0; hops < 8; hops++)
            {
                if (index < 0 || index >= COUNT)
                    return null;
                int offset = word(dataA, index * 4);
                d = (word(dataA, index * 4 + 2) & 0x8000) != 0 ? dataB : dataA;
                o = offset;
                int size = word(d, o);
                if ((size & 0x8000) == 0)
                    break;
                if ((size & 0x4000) != 0)
                    return null;
                index = size & 0xFF;   // the same picture as another image
            }
            int rows = word(d, o + 2) / 2;
            // First pass: the columns used, so the picture can be cropped
            int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
            for (int pass = 0; pass < 2; pass++)
            {
                int width = maxX - minX;
                byte[] slots = pass == 1 ? new byte[width * rows] : null;
                if (slots != null)
                    java.util.Arrays.fill(slots, (byte) -1);
                for (int r = 0; r < rows; r++)
                {
                    int p = o + word(d, o + 8 + r * 2);
                    int control = d[p++] & 0xFF;
                    if (control == 0)
                        continue;
                    int start = d[p++];
                    while (true)
                    {
                        int end = d[p++];
                        int slot = ((control & 0x7E) - 4) / 2;
                        if (pass == 0)
                        {
                            if (end > start)
                            {
                                minX = Math.min(minX, start);
                                maxX = Math.max(maxX, end);
                            }
                        }
                        else
                        {
                            int y = rows - 1 - r;   // rows are stored bottom first
                            for (int x = start; x < end; x++)
                                slots[y * width + x - minX] = (byte) (slot & 0x0F);
                        }
                        control = d[p++] & 0xFF;
                        if (control == 0)
                            break;
                        if ((control & 0x80) != 0)
                            start = d[p++];
                        else
                            start = end;
                    }
                }
                if (pass == 0 && minX >= maxX)
                    return null;
                if (pass == 1)
                    return new Sprite(width, rows, slots);
            }
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            // a damaged file: no picture
        }
        return null;
    }
}
