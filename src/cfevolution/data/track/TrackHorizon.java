/*
 * CF Evolution: An editor for Formula One Grand Prix/World Circuit
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
 */

package cfevolution.data.track;

import cfevolution.data.executable.ExePackUnpacker;
import cfevolution.data.executable.Executable;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.IOException;

/**
 * A track's horizon picture (2026-10-25): the file's first 4096 bytes, 512
 * columns by 8 rows of palette indices, row by row (ArgDocs). No Swing here.
 *
 * How the game draws it (F1GP-SDL, read from the code and matched by their port
 * on every circuit, dry and wet; verbatim in
 * docs/f1gp-sdl-exchange/2026-10-25-horizon-reply.md):
 * <ul>
 * <li>rows 1:1, image row 0 at the top, ending on the horizon line; it moves up
 *     and down with the camera but never tilts;</li>
 * <li>512 columns cover a QUARTER turn (column = (yaw >> 5) & 1FFh, 4000h =
 *     90 degrees), so the picture repeats four times round a full turn;</li>
 * <li>no transparency: every byte is drawn. 239, which fills the sky on all 16
 *     originals, only looks transparent because it is one of the sky entries
 *     the game sets for the weather;</li>
 * <li>all 4096 bytes are picture; any value is accepted and none can hang the
 *     game. The checksum is recomputed on save as for any edit.</li>
 * </ul>
 * Colours that do not stay as painted are those PaletteChooser marks: the sky,
 * the per-circuit fog groups and ground, and in the wet every index is
 * translated through the game's spray table.
 */
public final class TrackHorizon
{
    public static final int WIDTH = 512, HEIGHT = 8, BYTES = WIDTH * HEIGHT;

    /** The sky entry every original fills its sky with. */
    public static final int SKY = 239;

    /** arTCPalette, the race palette, in the three unpacked builds (EU, IT, US). */
    private static final int[] PALETTE_AT = { 142419, 142355, 142371 };

    private TrackHorizon() { }

    /** The race palette (768 bytes, 6-bit) from a GP.EXE, packed or unpacked,
        or null if the file is not a 1.05 build the editor knows. */
    public static byte[] readPalette(File gameExe)
    {
        try
        {
            byte[] exe = java.nio.file.Files.readAllBytes(gameExe.toPath());
            if (ExePackUnpacker.isPacked(exe))
                exe = ExePackUnpacker.unpack(exe);
            int build = Executable.identify(exe);
            if (build < 1 || build > 3)
                return null;
            byte[] pal = new byte[768];
            System.arraycopy(exe, PALETTE_AT[build - 1], pal, 0, 768);
            return pal;
        }
        catch (IOException e)
        {
            return null;
        }
        catch (RuntimeException e)
        {
            return null;
        }
    }

    /** The GP.EXE to take the palette from: in the track's own folder first,
        then in the given folders (may be null). Null if none is found. */
    public static File findGameExe(File trackFile, File... others)
    {
        java.util.List<File> folders = new java.util.ArrayList<File>();
        if (trackFile != null && trackFile.getAbsoluteFile().getParentFile() != null)
            folders.add(trackFile.getAbsoluteFile().getParentFile());
        for (File f : others)
            if (f != null)
                folders.add(f);
        for (File folder : folders)
        {
            File[] list = folder.listFiles();
            if (list == null)
                continue;
            for (File f : list)
                if (f.getName().equalsIgnoreCase("GP.EXE") && readPalette(f) != null)
                    return f;
        }
        return null;
    }

    /** 8-bit RGB of a 6-bit palette. */
    public static int[] rgb(byte[] palette)
    {
        int[] out = new int[256];
        for (int i = 0; i < 256; i++)
        {
            int r = palette[i * 3] & 0x3F, g = palette[i * 3 + 1] & 0x3F, b = palette[i * 3 + 2] & 0x3F;
            out[i] = ((r << 2) | (r >> 4)) << 16 | ((g << 2) | (g >> 4)) << 8 | ((b << 2) | (b >> 4));
        }
        return out;
    }

    /** True for entries the game changes per circuit or with the weather in the
        dry (sky, fog groups, ground, the crowd). F1GP-SDL, 2026-10-25. */
    public static boolean changesInRace(int i)
    {
        return (i >= 16 && i <= 31) || (i >= 128 && i <= 135) || (i >= 141 && i <= 151) || i == 158
            || (i >= 160 && i <= 167) || (i >= 232 && i <= 239);
    }

    /** The horizon as an 8-bit indexed image in the race palette. */
    public static BufferedImage toImage(byte[] horizon, byte[] palette)
    {
        byte[] r = new byte[256], g = new byte[256], b = new byte[256];
        int[] c = rgb(palette);
        for (int i = 0; i < 256; i++)
        {
            r[i] = (byte) (c[i] >> 16);
            g[i] = (byte) (c[i] >> 8);
            b[i] = (byte) c[i];
        }
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_INDEXED,
                                              new IndexColorModel(8, 256, r, g, b));
        img.getRaster().setDataElements(0, 0, WIDTH, HEIGHT, java.util.Arrays.copyOf(horizon, BYTES));
        return img;
    }

    /**
     * A picture as horizon bytes, or null if its size is not 512 x 8 or a whole
     * multiple of it (which is read one pixel per block). An indexed picture
     * whose palette gives the game's colour at every index it uses keeps its
     * indices exactly, as an exported horizon does. Otherwise each colour goes
     * to the nearest palette entry that stays as painted in the race; the sky's
     * own colour goes to 239, so a sky painted in it still follows the weather.
     */
    public static byte[] fromImage(BufferedImage img, byte[] palette)
    {
        int w = img.getWidth(), h = img.getHeight();
        if (w % WIDTH != 0 || h % HEIGHT != 0 || w / WIDTH != h / HEIGHT || w == 0)
            return null;
        int k = w / WIDTH;
        int[] pal = rgb(palette);
        byte[] out = new byte[BYTES];

        if (img.getColorModel() instanceof IndexColorModel && img.getRaster().getNumBands() == 1)
        {
            IndexColorModel cm = (IndexColorModel) img.getColorModel();
            boolean exact = cm.getMapSize() <= 256;
            for (int y = 0; y < HEIGHT && exact; y++)
                for (int x = 0; x < WIDTH && exact; x++)
                {
                    int idx = img.getRaster().getSample(x * k, y * k, 0);
                    out[y * WIDTH + x] = (byte) idx;
                    exact = (cm.getRGB(idx) & 0xFCFCFC) == (pal[idx] & 0xFCFCFC);
                }
            if (exact)
                return out;
        }

        java.util.Map<Integer, Integer> cache = new java.util.HashMap<Integer, Integer>();
        for (int y = 0; y < HEIGHT; y++)
            for (int x = 0; x < WIDTH; x++)
            {
                int c = img.getRGB(x * k, y * k) & 0xFFFFFF;
                Integer hit = cache.get(c);
                if (hit == null)
                {
                    hit = nearest(c, pal);
                    cache.put(c, hit);
                }
                out[y * WIDTH + x] = (byte) (int) hit;
            }
        return out;
    }

    /**
     * Horizon bytes from a raw horizon file: ArgTrack's .arghoriz (exactly the
     * 4096 bytes, checked 2026-10-25 against a file ArgTrack saved) or a track
     * file, whose first 4096 bytes are its horizon; ArgTrack's Load Horizon
     * takes either the same way. Null if the file is shorter than a horizon.
     */
    public static byte[] fromHorizonFile(byte[] file)
    {
        return file == null || file.length < BYTES ? null : java.util.Arrays.copyOf(file, BYTES);
    }

    private static int nearest(int c, int[] pal)
    {
        if ((c & 0xFCFCFC) == (pal[SKY] & 0xFCFCFC))
            return SKY;
        int best = 0, bestDistance = Integer.MAX_VALUE;
        for (int p = 0; p < 256; p++)
        {
            if (changesInRace(p))
                continue;
            int dr = ((c >> 16) & 0xFF) - ((pal[p] >> 16) & 0xFF);
            int dg = ((c >> 8) & 0xFF) - ((pal[p] >> 8) & 0xFF);
            int db = (c & 0xFF) - (pal[p] & 0xFF);
            int d = 3 * dr * dr + 4 * dg * dg + 2 * db * db;
            if (d < bestDistance)
            {
                bestDistance = d;
                best = p;
            }
        }
        return best;
    }
}
