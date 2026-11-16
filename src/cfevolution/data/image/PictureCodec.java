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

import java.io.IOException;

/**
 * Reads and writes picture files for the core. The core uses no java.awt, so
 * each platform supplies this: the desktop editor's is ImageIO-based
 * (cfevolution.gui.ImageIOPictures); an Android one would use its own
 * decoding. Install one with {@link Pictures#setCodec}.
 */
public interface PictureCodec
{
    /** A picture file (PNG and whatever else the platform reads), or null if it is not one. */
    Picture read(byte[] file) throws IOException;

    /** An 8-bit indexed PNG of these palette indices, carrying the palette (0xRRGGBB, 256 entries). */
    byte[] writeIndexedPng(byte[] pixels, int width, int height, int[] palette) throws IOException;

    /** The picture's pixels at this size as ARGB: its own when the size already matches, else smoothly scaled. */
    int[] argbAt(Picture picture, int width, int height);
}
