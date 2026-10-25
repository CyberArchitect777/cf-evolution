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
package cfevolution.data.gamefiles;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * The game's picture files (HELMETS.DAT, FLAGS.DAT, BACKDROP.DAT,
 * TRACKPIX.DAT and others): "f1pcanim", an item count, then 14 bytes per
 * item (type, offset, length, width, height), then the items packed in order
 * (ArgDocs, media containers). Types: 1774 small run-length pictures, 1769
 * full-screen pictures, 1776 a 768-byte palette (6-bit RGB), 1768 animation
 * frames. Items are kept as stored and only re-encoded when changed, so an
 * unchanged file saves byte-identically. No checksum.
 *
 * Small pictures (1774): a signed byte n; n < 0 repeats the next byte 1 - n
 * times, otherwise the next n + 1 bytes are single pixels. The game's own
 * files never let a run cross a row and never hold more than 32 single
 * pixels in one go, and the encoder here keeps to both.
 *
 * Full-screen pictures (1769), from the F1GP-SDL project's reading of
 * sub_72B1B (2026-10-25), checked here by decoding all 35 in BACKDROP.DAT and
 * TRACKPIX.DAT to 64,000 pixels: 320 x 200, row by row, compressed with a
 * variant of LZW. 12-bit codes, two to three bytes (a little-endian word
 * whose low 12 bits are the first code and top nibble the high bits of the
 * second, then the second code's low byte). Codes below 100h are pixels;
 * 100h-FFFh name a table of 3,840 strings, each "the previous string plus
 * the first pixel of this one". Before every code but the first, one entry is
 * added; when the table is full it wraps and overwrites the oldest. No clear
 * code, no end code: decoding stops at 64,000 pixels. The original game's
 * largest picture is 38,859 bytes, so nothing larger is written.
 *
 * @author barrie
 */
public class MediaContainer
{
    public static final int SMALL_PICTURE = 1774, FULL_SCREEN = 1769, PALETTE = 1776, ANIMATION = 1768;
    private static final byte[] SIGNATURE = "f1pcanim".getBytes();
    private static final int MAX_SINGLES = 32;
    public static final int SCREEN_PIXELS = 64000, TABLE_SIZE = 3840;
    /** The largest full-screen picture in the original game's files (BACKDROP.DAT). */
    public static final int MAX_FULL_SCREEN_BYTES = 38859;

    public static class Item
    {
        public final int type, width, height;
        byte[] data;

        Item(int type, int width, int height, byte[] data)
        {
            this.type = type;
            this.width = width;
            this.height = height;
            this.data = data;
        }

        public byte[] getData() { return data.clone(); }
    }

    private final List<Item> items = new ArrayList<Item>();

    public static boolean isContainer(File file)
    {
        try
        {
            byte[] start = new byte[8];
            java.io.InputStream in = new java.io.FileInputStream(file);
            try
            {
                return in.read(start) == 8 && java.util.Arrays.equals(start, SIGNATURE);
            }
            finally
            {
                in.close();
            }
        }
        catch (IOException e)
        {
            return false;
        }
    }

    public static MediaContainer load(File file) throws IOException
    {
        return load(Files.readAllBytes(file.toPath()));
    }

    /** A picture file's contents, already read (e.g. from a zip). */
    public static MediaContainer load(byte[] d) throws IOException
    {
        if (d.length < 10 || !java.util.Arrays.equals(java.util.Arrays.copyOf(d, 8), SIGNATURE))
            throw new IOException("not a picture file");
        MediaContainer c = new MediaContainer();
        int count = word(d, 8);
        for (int i = 0; i < count; i++)
        {
            int at = 10 + i * 14;
            int offset = dword(d, at + 2), length = dword(d, at + 6);
            if (offset < 0 || length < 0 || offset + length > d.length)
                throw new IOException("damaged picture file");
            c.items.add(new Item((short) word(d, at), word(d, at + 10), word(d, at + 12),
                                 java.util.Arrays.copyOfRange(d, offset, offset + length)));
        }
        return c;
    }

    /** Writes the header and the items packed in order, as the game's own files are laid out. */
    public void save(File file) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(SIGNATURE);
        writeWord(out, items.size());
        int offset = 10 + items.size() * 14;
        for (Item item : items)
        {
            writeWord(out, item.type);
            writeDword(out, offset);
            writeDword(out, item.data.length);
            writeWord(out, item.width);
            writeWord(out, item.height);
            offset += item.data.length;
        }
        for (Item item : items)
            out.write(item.data);
        Files.write(file.toPath(), out.toByteArray());
    }

    public int getItemCount() { return items.size(); }

    public Item getItem(int i) { return items.get(i); }

    /** The palette as 256 RGB colours (8-bit), from a 1776 item. */
    public int[] getPalette(int i)
    {
        byte[] p = items.get(i).data;
        int[] rgb = new int[256];
        for (int c = 0; c < 256 && c * 3 + 2 < p.length; c++)
        {
            int r = p[c * 3] & 0x3F, g = p[c * 3 + 1] & 0x3F, b = p[c * 3 + 2] & 0x3F;
            rgb[c] = ((r << 2 | r >> 4) << 16) | ((g << 2 | g >> 4) << 8) | (b << 2 | b >> 4);
        }
        return rgb;
    }

    /** A small picture's pixels (palette indices), row by row. */
    public byte[] getSmallPicture(int i)
    {
        Item item = items.get(i);
        byte[] out = new byte[item.width * item.height];
        byte[] b = item.data;
        int p = 0, k = 0;
        while (k < b.length && p < out.length)
        {
            int n = b[k++];
            if (n < 0)
            {
                byte v = b[k++];
                for (int r = 0; r < 1 - n && p < out.length; r++)
                    out[p++] = v;
            }
            else
                for (int r = 0; r <= n && p < out.length && k < b.length; r++)
                    out[p++] = b[k++];
        }
        return out;
    }

    /** Replaces a small picture's pixels, re-encoding it (only if they changed). */
    public void setSmallPicture(int i, byte[] pixels)
    {
        Item item = items.get(i);
        if (java.util.Arrays.equals(pixels, getSmallPicture(i)))
            return;
        item.data = encodeSmall(pixels, item.width);
    }

    /** Replaces a palette (8-bit RGB colours, stored as the game's 6-bit values). */
    public void setPalette(int i, int[] rgb)
    {
        byte[] p = new byte[768];
        for (int c = 0; c < 256; c++)
        {
            p[c * 3] = (byte) (((rgb[c] >> 16) & 0xFF) >> 2);
            p[c * 3 + 1] = (byte) (((rgb[c] >> 8) & 0xFF) >> 2);
            p[c * 3 + 2] = (byte) ((rgb[c] & 0xFF) >> 2);
        }
        if (!java.util.Arrays.equals(p, items.get(i).data))
            items.get(i).data = p;
    }

    /** A full-screen picture's 64,000 pixels, row by row. */
    public byte[] getFullScreen(int i)
    {
        return decodeFullScreen(items.get(i).data);
    }

    /**
     * Replaces a full-screen picture (only if it changed). Returns false,
     * changing nothing, if it compresses to more than the original game's
     * largest picture.
     */
    public boolean setFullScreen(int i, byte[] pixels)
    {
        if (java.util.Arrays.equals(pixels, getFullScreen(i)))
            return true;
        byte[] encoded = encodeFullScreen(pixels);
        if (encoded.length > MAX_FULL_SCREEN_BYTES)
            return false;
        items.get(i).data = encoded;
        return true;
    }

    static byte[] decodeFullScreen(byte[] b)
    {
        byte[] out = new byte[SCREEN_PIXELS + 4096 * 16];
        int[] start = new int[TABLE_SIZE], length = new int[TABLE_SIZE];
        int n = 0, next = 0, prevStart = -1, prevLength = 0, k = 0;
        boolean second = false;
        int nibble = 0;
        while (n < SCREEN_PIXELS && k < b.length)
        {
            int code;
            if (!second)
            {
                if (k + 1 >= b.length)
                    break;
                int w = (b[k] & 0xFF) | ((b[k + 1] & 0xFF) << 8);
                k += 2;
                code = w & 0xFFF;
                nibble = w >> 12;
            }
            else
                code = (b[k++] & 0xFF) | (nibble << 8);
            second = !second;
            int here = n;
            if (prevStart >= 0)
            {
                start[next] = prevStart;
                length[next] = prevLength + 1;
                next = (next + 1) % TABLE_SIZE;
            }
            if (code < 256)
            {
                out[n++] = (byte) code;
                prevLength = 1;
            }
            else
            {
                int s = start[code - 256], len = length[code - 256];
                if (n + len > out.length)
                    len = out.length - n;
                for (int c = 0; c < len; c++)
                    out[n + c] = out[s + c];
                n += len;
                prevLength = len;
            }
            prevStart = here;
        }
        return java.util.Arrays.copyOf(out, SCREEN_PIXELS);
    }

    /** LZW mirroring the decoder exactly, the table wrap included. */
    static byte[] encodeFullScreen(byte[] px)
    {
        java.util.Map<String, Integer> codes = new java.util.HashMap<String, Integer>();
        String[] entries = new String[TABLE_SIZE];
        String all = new String(px, java.nio.charset.StandardCharsets.ISO_8859_1);
        java.util.List<Integer> out = new java.util.ArrayList<Integer>();
        int next = 0, p = 0;
        String prev = null;
        while (p < all.length())
        {
            // the decoder adds "previous + first pixel of this" before reading
            // each code, so the encoder does too, before choosing the code
            if (prev != null)
            {
                String entry = prev + all.charAt(p);
                if (entries[next] != null && Integer.valueOf(next).equals(codes.get(entries[next])))
                    codes.remove(entries[next]);
                entries[next] = entry;
                codes.put(entry, next);
                next = (next + 1) % TABLE_SIZE;
            }
            // the longest string at p the table holds (a single pixel always works)
            int len = 1, code = px[p] & 0xFF;
            while (p + len < all.length())
            {
                Integer c = codes.get(all.substring(p, p + len + 1));
                if (c == null)
                    break;
                len++;
                code = c + 256;
            }
            out.add(code);
            prev = all.substring(p, p + len);
            p += len;
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int i = 0; i < out.size(); i += 2)
        {
            int a = out.get(i), b = i + 1 < out.size() ? out.get(i + 1) : 0;
            int w = a | ((b >> 8) << 12);
            bytes.write(w);
            bytes.write(w >> 8);
            if (i + 1 < out.size())
                bytes.write(b);
        }
        return bytes.toByteArray();
    }

    static byte[] encodeSmall(byte[] px, int width)
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int row = 0; row < px.length; row += width)
        {
            int end = Math.min(row + width, px.length), p = row;
            while (p < end)
            {
                int run = 1;
                while (p + run < end && px[p + run] == px[p] && run < 129)
                    run++;
                if (run >= 2)
                {
                    out.write(1 - run);
                    out.write(px[p]);
                    p += run;
                    continue;
                }
                int start = p;   // single pixels, up to the next pair
                while (p < end && p - start < MAX_SINGLES && (p + 1 >= end || px[p + 1] != px[p]))
                    p++;
                if (p == start)
                    p++;
                out.write(p - start - 1);
                out.write(px, start, p - start);
            }
        }
        return out.toByteArray();
    }

    private static int word(byte[] d, int o) { return (d[o] & 0xFF) | ((d[o + 1] & 0xFF) << 8); }

    private static int dword(byte[] d, int o) { return word(d, o) | (word(d, o + 2) << 16); }

    private static void writeWord(ByteArrayOutputStream out, int v) { out.write(v); out.write(v >> 8); }

    private static void writeDword(ByteArrayOutputStream out, int v) { writeWord(out, v); writeWord(out, v >> 16); }
}
