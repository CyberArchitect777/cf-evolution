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

package cfevolution.generator.ccline;

/**
    Continuous target lateral offset per Seg, in wCCLine units
    (positive/negative = either side of the track centre). This is the
    intermediate representation all generators produce before the shared
    quantizer turns it into CCLine segments.
*/
public class CCLineLateralProfile {

    /** One entry per Seg. */
    public final double[] offset;

    public CCLineLateralProfile(int segCount) {
        offset = new double[segCount];
    }

    public int size() {
        return offset.length;
    }

    /** Clamp every offset into the track's usable bound (with margin). */
    public void clampToBounds(CCLineTrackGeometry geo, double margin) {
        for (int i = 0; i < offset.length; i++) {
            double bound = geo.usableBound[i] - margin;
            if (bound < 0)
                bound = 0;
            if (offset[i] > bound)
                offset[i] = bound;
            else if (offset[i] < -bound)
                offset[i] = -bound;
        }
    }
}
