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
*/package cfevolution.gui;

import cfevolution.data.image.Picture;
import cfevolution.data.image.PictureCodec;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * The desktop editor's PictureCodec, on ImageIO. Moved out of the core
 * (GameDataset, TrackHorizon) unchanged on 2026-11-16, so pictures read,
 * write and scale exactly as they did.
 */
public class ImageIOPictures implements PictureCodec
{
    public Picture read(byte[] file) throws IOException
    {
        BufferedImage im = ImageIO.read(new ByteArrayInputStream(file));
        return im == null ? null : toPicture(im);
    }

    public byte[] writeIndexedPng(byte[] pixels, int w, int h, int[] palette) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(toImage(pixels, w, h, palette), "png", out);
        return out.toByteArray();
    }

    public byte[] writeRgbPng(int[] rgb, int w, int h) throws IOException
    {
        BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        im.setRGB(0, 0, w, h, rgb, 0, w);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(im, "png", out);
        return out.toByteArray();
    }

    public int[] argbAt(Picture p, int w, int h)
    {
        if (p.width == w && p.height == h)
            return p.argb.clone();
        BufferedImage in = p.source instanceof BufferedImage ? (BufferedImage) p.source : toImage(p);
        BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(in, 0, 0, w, h, null);
        g.dispose();
        return scaled.getRGB(0, 0, w, h, null, 0, w);
    }

    /** A desktop image as a core Picture, with its palette indices if it has a palette. */
    public static Picture toPicture(BufferedImage im)
    {
        int w = im.getWidth(), h = im.getHeight();
        int[] argb = im.getRGB(0, 0, w, h, null, 0, w);
        int[] indices = null, palette = null;
        if (im.getColorModel() instanceof IndexColorModel && im.getRaster().getNumBands() == 1)
        {
            IndexColorModel cm = (IndexColorModel) im.getColorModel();
            palette = new int[cm.getMapSize()];
            for (int i = 0; i < palette.length; i++)
                palette[i] = cm.getRGB(i);
            indices = im.getRaster().getSamples(0, 0, w, h, 0, (int[]) null);
        }
        return new Picture(w, h, argb, indices, palette, im);
    }

    /** An 8-bit indexed image of these palette indices (palette as 0xRRGGBB, 256 entries). */
    public static BufferedImage toImage(byte[] pixels, int w, int h, int[] palette)
    {
        byte[] r = new byte[256], g = new byte[256], b = new byte[256];
        for (int i = 0; i < 256; i++)
        {
            r[i] = (byte) (palette[i] >> 16);
            g[i] = (byte) (palette[i] >> 8);
            b[i] = (byte) palette[i];
        }
        BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_INDEXED, new IndexColorModel(8, 256, r, g, b));
        im.getRaster().setDataElements(0, 0, w, h, pixels);
        return im;
    }

    private static BufferedImage toImage(Picture p)
    {
        BufferedImage im = new BufferedImage(p.width, p.height, BufferedImage.TYPE_INT_ARGB);
        im.setRGB(0, 0, p.width, p.height, p.argb, 0, p.width);
        return im;
    }
}
