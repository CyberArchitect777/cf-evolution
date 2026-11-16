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
 * The installed PictureCodec. When none has been installed, the first one
 * registered through java.util.ServiceLoader is used: the desktop editor's
 * jar registers its own in META-INF/services, so the editor and its harnesses
 * need no setup and the core never names the interface. Another platform
 * installs its own with setCodec (or registers it the same way).
 */
public final class Pictures
{
    private static PictureCodec codec;

    private Pictures()
    {
    }

    public static synchronized void setCodec(PictureCodec c)
    {
        codec = c;
    }

    public static synchronized PictureCodec codec()
    {
        if (codec == null)
        {
            java.util.Iterator<PictureCodec> found = java.util.ServiceLoader.load(PictureCodec.class).iterator();
            if (!found.hasNext())
                throw new IllegalStateException("No picture codec installed: call Pictures.setCodec first");
            codec = found.next();
        }
        return codec;
    }
}
