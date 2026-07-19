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

import cfevolution.data.track.Seg;
import cfevolution.data.track.Track;
import cfevolution.data.track.TrackSegments;

/**
    Immutable per-Seg snapshot of the track geometry a CCLine generator
    needs. Extracted once from a Track so generation and evaluation can
    run on a background thread without touching the live track data.
*/
public class CCLineTrackGeometry {

    /** Number of fine Segs (= total track TLU). */
    public final int segCount;

    /** 19-bit world position per Seg (Seg.getPosX/getPosY). */
    public final int[] posX;
    public final int[] posY;

    /** Track heading per Seg (16-bit game angle). */
    public final short[] angleZ;

    /** Lateral curvature correction factor per Seg. */
    public final int[] angleZChangeMulHalfPI;

    /** Drivable track half-width per Seg, from the game's own formula:
        sqrt((wLeftAndRightSideX + bExtraSideX)^2 / 64
           + (wLeftAndRightSideY + bExtraSideY)^2 / 64) * 8 */
    public final double[] trackWidth;

    /** The game's tolerance bound per Seg: trackWidth - 0x340. This is
        what TCCompareCCLineToTrackWidth checks — but it is ~8x the
        physical road (see physicalBound) and nearly vacuous; treat it as
        the game's flag threshold, not the road edge. */
    public final double[] usableBound;

    /** PHYSICAL road half-width per Seg in wCCLine units: trackWidth / 8
        — the same scale the map draws edges at (width >> 3 from centre).
        Established 2026-07-19: hand-tuned lines average 50-60% of this
        and peak at 0.9-1.36x (kerb clipping); it is the real corridor
        for line generation. */
    public final double[] physicalBound;

    public CCLineTrackGeometry(Track track) {
        TrackSegments segs = track.getTrackSegments();
        segCount = segs.getMaxTrackSegIndex() + 1;

        posX = new int[segCount];
        posY = new int[segCount];
        angleZ = new short[segCount];
        angleZChangeMulHalfPI = new int[segCount];
        trackWidth = new double[segCount];
        usableBound = new double[segCount];
        physicalBound = new double[segCount];

        for (int i = 0; i < segCount; i++) {
            Seg seg = segs.getSegAt(i);
            posX[i] = seg.getPosX();
            posY[i] = seg.getPosY();
            angleZ[i] = (short) seg.getAngleZ();
            angleZChangeMulHalfPI[i] = seg.getAngleZChangeMulHalfPI();
            double wx = seg.getTrackWidthX() + seg.getExtraSideX();
            double wy = seg.getTrackWidthY() + seg.getExtraSideY();
            trackWidth[i] = Math.sqrt(wx * wx / 64.0 + wy * wy / 64.0) * 8.0;
            usableBound[i] = trackWidth[i] - 0x340;
            physicalBound[i] = trackWidth[i] / 8.0;
        }
    }
}
