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

package cfevolution.android;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import cfevolution.data.image.IndexedPng;
import cfevolution.data.image.Picture;
import cfevolution.data.image.PictureCodec;
import java.io.IOException;

/**
 * The app's picture codec (2026-11-17): PNG in plain Java from the core
 * (IndexedPng, which keeps an indexed picture's palette indices, as the
 * horizon and the game's pictures need), Android's own decoder for any other
 * picture (JPEG, interlaced PNG...), and Android's smooth scaling.
 */
final class AndroidPictures implements PictureCodec
{
    public Picture read(byte[] file) throws IOException
    {
        Picture p = IndexedPng.read(file);
        if (p != null)
            return p;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inPremultiplied = false;
        o.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap b = BitmapFactory.decodeByteArray(file, 0, file.length, o);
        if (b == null)
            return null;
        int w = b.getWidth(), h = b.getHeight();
        int[] argb = new int[w * h];
        b.getPixels(argb, 0, w, 0, 0, w, h);
        return new Picture(w, h, argb, null, null, b);
    }

    public byte[] writeIndexedPng(byte[] pixels, int width, int height, int[] palette) throws IOException
    {
        return IndexedPng.write(pixels, width, height, palette);
    }

    public byte[] writeRgbPng(int[] rgb, int width, int height) throws IOException
    {
        return IndexedPng.writeRgb(rgb, width, height);
    }

    public int[] argbAt(Picture p, int width, int height)
    {
        if (p.width == width && p.height == height)
            return p.argb.clone();
        Bitmap in = p.source instanceof Bitmap ? (Bitmap) p.source
                  : Bitmap.createBitmap(p.argb, p.width, p.height, Bitmap.Config.ARGB_8888);
        Bitmap s = Bitmap.createScaledBitmap(in, width, height, true);
        int[] out = new int[width * height];
        s.getPixels(out, 0, width, 0, 0, width, height);
        for (int i = 0; i < out.length; i++)
            out[i] |= 0xFF000000;      // as the desktop scales onto an opaque picture
        return out;
    }
}
