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

    /** NOT the game's figure: sqrt((wLeftAndRightSideX + bExtraSideX)^2 / 64
           + (wLeftAndRightSideY + bExtraSideY)^2 / 64) * 8, about 8x what
        TCCompareCCLineToTrackWidth (IDA 0x6CFF) computes. The game shifts each
        side word right by 6 BEFORE adding the extra:
        sqrt(((sideX >> 6) + extraX)^2 + ((sideY >> 6) + extraY)^2) * 8.
        Kept as it is because physicalBound (this / 8) is the road edge the
        line corridors are calibrated against; see the note there. */
    public final double[] trackWidth;

    /** trackWidth - 0x340. Since trackWidth is ~8x the game's, this is NOT
        the game's flag threshold and is nearly vacuous (no Seg of any
        original's line reaches it). The game's own threshold, game width -
        0x340, is the opposite: a band well inside the road that 50-80% of
        each original's Segs lie outside (measured 2026-10-25,
        harnesses/WidthCheck.java). Used only as a loose clamp. */
    public final double[] usableBound;

    /** PHYSICAL road half-width per Seg in wCCLine units: trackWidth / 8
        — the same scale the map draws edges at (width >> 3 from centre).
        Established 2026-07-19: hand-tuned lines average 50-60% of this
        and peak at 0.9-1.36x (kerb clipping); it is the real corridor
        for line generation.
        The game's width is 58-91 units more on every Seg of every original
        (2026-10-25): its extras are built from the width + 0x50
        (TrackSegments, wTrk_WidthPlus0x50), so its figure is this road edge
        plus a fixed 80-unit allowance, not a wider road. Do not "correct"
        this to the game's figure: that would move every corridor outward
        by ~0.4 m with nothing to say the road is wider. */
    public final double[] physicalBound;

    /** How many coaching-table entries the best line may use on this track
        before the game hangs (Track.getCoachingEntryBudget): 63 less the
        pit lane's own entries. */
    public final int coachingBudget;

    public CCLineTrackGeometry(Track track) {
        coachingBudget = track.getCoachingEntryBudget();
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
