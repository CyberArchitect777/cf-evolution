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

package cfevolution.data.image;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * PNG in plain Java (2026-11-17), for platforms whose own picture reader
 * gives colours but not palette indices (Android's). Writes an 8-bit indexed
 * PNG; reads the palette indices of an indexed PNG (bit depth 1, 2, 4 or 8,
 * not interlaced), and the colours of an 8-bit grey, true colour or true
 * colour with alpha PNG (not interlaced). Anything else is left to the
 * platform. The same pixels as the desktop's ImageIO, though not the same
 * bytes (compression differs).
 */
public final class IndexedPng
{
    private static final byte[] SIGNATURE = { (byte) 137, 80, 78, 71, 13, 10, 26, 10 };

    private IndexedPng()
    {
    }

    /** An 8-bit indexed PNG of these palette indices, with the palette (0xRRGGBB). */
    public static byte[] write(byte[] pixels, int width, int height, int[] palette) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(SIGNATURE);
        ByteArrayOutputStream h = new ByteArrayOutputStream();
        int32(h, width);
        int32(h, height);
        h.write(8);      // bit depth
        h.write(3);      // indexed
        h.write(0);
        h.write(0);
        h.write(0);      // not interlaced
        chunk(out, "IHDR", h.toByteArray());
        byte[] plte = new byte[palette.length * 3];
        for (int i = 0; i < palette.length; i++)
        {
            plte[i * 3] = (byte) (palette[i] >> 16);
            plte[i * 3 + 1] = (byte) (palette[i] >> 8);
            plte[i * 3 + 2] = (byte) palette[i];
        }
        chunk(out, "PLTE", plte);
        byte[] raw = new byte[(width + 1) * height];
        for (int y = 0; y < height; y++)
            System.arraycopy(pixels, y * width, raw, y * (width + 1) + 1, width);   // filter 0 on every row
        Deflater d = new Deflater(Deflater.BEST_COMPRESSION);
        d.setInput(raw);
        d.finish();
        ByteArrayOutputStream z = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        while (!d.finished())
            z.write(buf, 0, d.deflate(buf));
        d.end();
        chunk(out, "IDAT", z.toByteArray());
        chunk(out, "IEND", new byte[0]);
        return out.toByteArray();
    }

    /** An 8-bit true colour PNG of these colours (0xRRGGBB, row by row). */
    public static byte[] writeRgb(int[] rgb, int width, int height) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(SIGNATURE);
        ByteArrayOutputStream h = new ByteArrayOutputStream();
        int32(h, width);
        int32(h, height);
        h.write(8);
        h.write(2);      // true colour
        h.write(0);
        h.write(0);
        h.write(0);
        chunk(out, "IHDR", h.toByteArray());
        byte[] raw = new byte[(width * 3 + 1) * height];
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
            {
                int c = rgb[y * width + x], o = y * (width * 3 + 1) + 1 + x * 3;
                raw[o] = (byte) (c >> 16);
                raw[o + 1] = (byte) (c >> 8);
                raw[o + 2] = (byte) c;
            }
        idat(out, raw);
        chunk(out, "IEND", new byte[0]);
        return out.toByteArray();
    }

    private static void idat(ByteArrayOutputStream out, byte[] raw) throws IOException
    {
        Deflater d = new Deflater(Deflater.BEST_COMPRESSION);
        d.setInput(raw);
        d.finish();
        ByteArrayOutputStream z = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        while (!d.finished())
            z.write(buf, 0, d.deflate(buf));
        d.end();
        chunk(out, "IDAT", z.toByteArray());
    }

    private static void int32(ByteArrayOutputStream o, int v)
    {
        o.write(v >>> 24);
        o.write(v >>> 16);
        o.write(v >>> 8);
        o.write(v);
    }

    private static void chunk(ByteArrayOutputStream out, String type, byte[] data) throws IOException
    {
        ByteArrayOutputStream c = new ByteArrayOutputStream();
        int32(c, data.length);
        byte[] t = type.getBytes("US-ASCII");
        c.write(t);
        c.write(data);
        CRC32 crc = new CRC32();
        crc.update(t);
        crc.update(data);
        int32(c, (int) crc.getValue());
        out.write(c.toByteArray());
    }

    private static int at(byte[] b, int o)
    {
        return (b[o] & 0xFF) << 24 | (b[o + 1] & 0xFF) << 16 | (b[o + 2] & 0xFF) << 8 | (b[o + 3] & 0xFF);
    }

    /** A PNG this class reads, as a Picture (indexed: indices and palette as well as colours); else null. */
    public static Picture read(byte[] file)
    {
        if (file == null || file.length < 8)
            return null;
        for (int i = 0; i < 8; i++)
            if (file[i] != SIGNATURE[i])
                return null;
        int width = 0, height = 0, depth = 0, type = -1, interlace = 0;
        int[] palette = null;
        int[] alpha = null;
        ByteArrayOutputStream idat = new ByteArrayOutputStream();
        int o = 8;
        try
        {
            while (o + 8 <= file.length)
            {
                int length = at(file, o);
                String kind = new String(file, o + 4, 4, "US-ASCII");
                int d = o + 8;
                if (length < 0 || d + length > file.length)
                    return null;
                if (kind.equals("IHDR"))
                {
                    width = at(file, d);
                    height = at(file, d + 4);
                    depth = file[d + 8] & 0xFF;
                    type = file[d + 9] & 0xFF;
                    interlace = file[d + 12] & 0xFF;
                }
                else if (kind.equals("PLTE"))
                {
                    palette = new int[length / 3];
                    for (int i = 0; i < palette.length; i++)
                        palette[i] = (file[d + i * 3] & 0xFF) << 16 | (file[d + i * 3 + 1] & 0xFF) << 8 | (file[d + i * 3 + 2] & 0xFF);
                }
                else if (kind.equals("tRNS") && type == 3)
                {
                    alpha = new int[length];
                    for (int i = 0; i < length; i++)
                        alpha[i] = file[d + i] & 0xFF;
                }
                else if (kind.equals("IDAT"))
                    idat.write(file, d, length);
                else if (kind.equals("IEND"))
                    break;
                o = d + length + 4;
            }
        }
        catch (java.io.UnsupportedEncodingException e)
        {
            return null;
        }
        int channels = type == 3 || type == 0 ? 1 : type == 2 ? 3 : type == 6 ? 4 : type == 4 ? 2 : 0;
        boolean indexed = type == 3 && palette != null && (depth == 1 || depth == 2 || depth == 4 || depth == 8);
        boolean colour = (type == 0 || type == 2 || type == 4 || type == 6) && depth == 8;
        if (interlace != 0 || width <= 0 || height <= 0 || (!indexed && !colour) || (long) width * height > 64L * 1024 * 1024)
            return null;
        int bpp = Math.max(1, channels * depth / 8);
        int stride = (width * channels * depth + 7) / 8;
        byte[] raw = new byte[(stride + 1) * height];
        Inflater inf = new Inflater();
        inf.setInput(idat.toByteArray());
        try
        {
            int n = 0;
            while (n < raw.length && !inf.finished())
            {
                int got = inf.inflate(raw, n, raw.length - n);
                if (got == 0 && (inf.needsInput() || inf.needsDictionary()))
                    break;
                n += got;
            }
            if (n < raw.length)
                return null;
        }
        catch (DataFormatException e)
        {
            return null;
        }
        finally
        {
            inf.end();
        }
        byte[] rows = new byte[stride * height];
        byte[] prior = new byte[stride];
        for (int y = 0; y < height; y++)
        {
            int f = raw[y * (stride + 1)] & 0xFF;
            byte[] cur = new byte[stride];
            System.arraycopy(raw, y * (stride + 1) + 1, cur, 0, stride);
            for (int i = 0; i < stride; i++)
            {
                int a = i >= bpp ? cur[i - bpp] & 0xFF : 0, b = prior[i] & 0xFF, c = i >= bpp ? prior[i - bpp] & 0xFF : 0;
                int x = cur[i] & 0xFF;
                switch (f)
                {
                    case 0: break;
                    case 1: x += a; break;
                    case 2: x += b; break;
                    case 3: x += (a + b) >> 1; break;
                    case 4:
                    {
                        int p = a + b - c, pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c);
                        x += pa <= pb && pa <= pc ? a : pb <= pc ? b : c;
                        break;
                    }
                    default: return null;
                }
                cur[i] = (byte) x;
            }
            System.arraycopy(cur, 0, rows, y * stride, stride);
            prior = cur;
        }
        int[] argb = new int[width * height];
        int[] indices = indexed ? new int[width * height] : null;
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
            {
                int p = y * width + x, r = y * stride;
                if (indexed)
                {
                    int bit = x * depth;
                    int v = ((rows[r + bit / 8] & 0xFF) >> (8 - depth - bit % 8)) & ((1 << depth) - 1);
                    indices[p] = v;
                    int a = alpha != null && v < alpha.length ? alpha[v] : 0xFF;
                    argb[p] = a << 24 | (v < palette.length ? palette[v] : 0);
                }
                else
                {
                    int q = r + x * channels;
                    int g = rows[q] & 0xFF;
                    switch (type)
                    {
                        case 0: argb[p] = 0xFF000000 | g << 16 | g << 8 | g; break;
                        case 4: argb[p] = (rows[q + 1] & 0xFF) << 24 | g << 16 | g << 8 | g; break;
                        case 2: argb[p] = 0xFF000000 | g << 16 | (rows[q + 1] & 0xFF) << 8 | (rows[q + 2] & 0xFF); break;
                        default: argb[p] = (rows[q + 3] & 0xFF) << 24 | g << 16 | (rows[q + 1] & 0xFF) << 8 | (rows[q + 2] & 0xFF);
                    }
                }
            }
        int[] pal = null;
        if (indexed)
        {
            pal = new int[palette.length];
            for (int i = 0; i < pal.length; i++)
                pal[i] = (alpha != null && i < alpha.length ? alpha[i] : 0xFF) << 24 | palette[i];
        }
        return new Picture(width, height, argb, indices, pal, null);
    }
}
