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

package cfevolution.data.track;

/**
 * The rectangle a track's drawing fits in, in track coordinates.
 * Replaces java.awt.Rectangle in the core (2026-11-16) so the core runs
 * where java.awt does not, such as Android. The getters behave as
 * Rectangle's do, so drawing code that used one keeps its arithmetic.
 */
public final class TrackBounds
{
    public final int x, y, width, height;

    public TrackBounds(int x, int y, int width, int height)
    {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public double getX()       { return x; }
    public double getY()       { return y; }
    public double getWidth()   { return width; }
    public double getHeight()  { return height; }
    public double getCenterX() { return x + width / 2.0; }
    public double getCenterY() { return y + height / 2.0; }

    public String toString()
    {
        return "TrackBounds[x=" + x + ",y=" + y + ",width=" + width + ",height=" + height + "]";
    }
}
