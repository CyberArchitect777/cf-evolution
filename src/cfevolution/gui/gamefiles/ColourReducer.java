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
package cfevolution.gui.gamefiles;

/**
 * Turns an imported picture into palette indices: a palette of its own by
 * median cut, and nearest-colour matching. The first {@code keep} colours of
 * a palette are the menus' own and are left as they are and never used for
 * the picture.
 *
 * @author barrie
 */
public class ColourReducer
{
    private ColourReducer()
    {
    }

    /** A palette for the picture: colours 0..keep-1 from {@code base}, the rest by median cut. */
    public static int[] palette(int[] rgb, int[] base, int keep)
    {
        int[] pal = base.clone();
        java.util.List<int[]> boxes = new java.util.ArrayList<int[]>();
        boxes.add(rgb.clone());
        int wanted = 256 - keep;
        while (boxes.size() < wanted)
        {
            // split the box with the widest colour range
            int best = -1, bestRange = 0, bestChannel = 0;
            for (int b = 0; b < boxes.size(); b++)
            {
                int[] box = boxes.get(b);
                if (box.length < 2)
                    continue;
                for (int ch = 0; ch < 3; ch++)
                {
                    int lo = 255, hi = 0;
                    for (int c : box)
                    {
                        int v = (c >> (16 - 8 * ch)) & 0xFF;
                        lo = Math.min(lo, v);
                        hi = Math.max(hi, v);
                    }
                    if (hi - lo > bestRange)
                    {
                        bestRange = hi - lo;
                        best = b;
                        bestChannel = ch;
                    }
                }
            }
            if (best < 0)
                break;
            int[] box = boxes.remove(best);
            final int shift = 16 - 8 * bestChannel;
            Integer[] sorted = new Integer[box.length];
            for (int i = 0; i < box.length; i++) sorted[i] = box[i];
            java.util.Arrays.sort(sorted, new java.util.Comparator<Integer>() {
                public int compare(Integer a, Integer b) { return ((a >> shift) & 0xFF) - ((b >> shift) & 0xFF); }
            });
            int half = sorted.length / 2;
            int[] a = new int[half], b = new int[sorted.length - half];
            for (int i = 0; i < sorted.length; i++)
                if (i < half) a[i] = sorted[i]; else b[i - half] = sorted[i];
            boxes.add(a);
            boxes.add(b);
        }
        for (int i = 0; i < boxes.size() && keep + i < 256; i++)
        {
            long r = 0, g = 0, bl = 0;
            int[] box = boxes.get(i);
            for (int c : box)
            {
                r += (c >> 16) & 0xFF;
                g += (c >> 8) & 0xFF;
                bl += c & 0xFF;
            }
            int n = Math.max(1, box.length);
            // the game keeps 6 bits a channel
            pal[keep + i] = ((int) (r / n) & 0xFC) << 16 | ((int) (g / n) & 0xFC) << 8 | ((int) (bl / n) & 0xFC);
        }
        return pal;
    }

    /** The nearest palette colour (from index {@code first} up) for each pixel. */
    public static byte[] match(int[] rgb, int[] pal, int first)
    {
        byte[] out = new byte[rgb.length];
        java.util.Map<Integer, Integer> cache = new java.util.HashMap<Integer, Integer>();
        for (int i = 0; i < rgb.length; i++)
        {
            int c = rgb[i] & 0xFFFFFF;
            Integer hit = cache.get(c);
            if (hit == null)
            {
                int best = first, bestDistance = Integer.MAX_VALUE;
                for (int p = first; p < 256; p++)
                {
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
                hit = best;
                cache.put(c, hit);
            }
            out[i] = (byte) (int) hit;
        }
        return out;
    }
}
