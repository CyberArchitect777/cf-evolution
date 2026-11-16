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
*/package cfevolution.data.image;

/**
 * A picture read from a file, in a form the core can use without java.awt
 * (so it runs on Android too; 2026-11-16). Pixels are ARGB ints, as
 * BufferedImage.getRGB gives them. A picture stored with a palette also
 * carries its own palette indices and colours, so a picture the editor
 * exported comes back index for index.
 *
 * Reading and writing files is the platform's job: see PictureCodec.
 */
public final class Picture
{
    public final int width, height;
    /** Every pixel, row by row, as ARGB. */
    public final int[] argb;
    /** Each pixel's palette index, row by row; null if the picture has no palette. */
    public final int[] indices;
    /** The picture's own palette as ARGB (its full size, up to 256); null if none. */
    public final int[] palette;
    /** The platform's own image, kept so its codec can scale it exactly as before. */
    public final Object source;

    public Picture(int width, int height, int[] argb, int[] indices, int[] palette, Object source)
    {
        this.width = width;
        this.height = height;
        this.argb = argb;
        this.indices = indices;
        this.palette = palette;
        this.source = source;
    }

    public boolean isIndexed()
    {
        return indices != null && palette != null;
    }

    public int getArgb(int x, int y)
    {
        return argb[y * width + x];
    }

    public int getIndex(int x, int y)
    {
        return indices[y * width + x];
    }
}
