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

import cfevolution.data.track.CCLine;
import cfevolution.data.track.CCLineSegment;

/**
    Scores a candidate CCLine by simulating it with CCLineSimulator and
    applying the game-derived validity rules plus soft quality terms.
    Lower score is better; a score below HARD_PENALTY has no hard
    violations at all.
*/
public class CCLineEvaluator {

    /** Penalty per hard violation — dominates all soft terms. */
    public static final double HARD_PENALTY = 1000000.0;

    /** Score breakdown for one candidate. */
    public static class Score {
        /** Segs where the line is outside the drivable bound (game rule:
            abs >= width - 0x340). NOT a hard violation: the original
            hand-tuned lines exceed the bound at kerb-clipping apexes and
            the game merely flags it (Seg.flags_12) — so it is weighted as
            a soft penalty and reported, not treated as invalid. */
        public int outOfBounds;
        /** Segs never stamped (CCLine TLU < track TLU — breaks the AI there). */
        public int uncovered;
        /** Times the simulation hit the Pythagoras clamp: the radius was
            too small for its arc at that point and the game would compute
            sqrt garbage. NOTE: this is the real unsafe-radius signal — the
            static rule |raw| > length * 128 is only the worst case (arc
            over straight track) and original tracks violate it freely on
            curved track, where the rotating track frame keeps the forward
            distance bounded. */
        public int unsafeRadius;
        /** Sum of relative-heading jumps at sector boundaries (16-bit angle units). */
        public double boundaryJumps;
        /** RMS per-Seg path curvature (16-bit angle units per TLU) — the speed proxy. */
        public double curvatureRms;
        /** Simulation result the score was computed from. */
        public CCLineSimulator.Result simulation;

        /** True when nothing AI-breaking is present. Out-of-bounds Segs do
            not affect validity (see outOfBounds). */
        public boolean isValid() {
            return uncovered == 0 && unsafeRadius == 0;
        }

        // Weights copied from the evaluator that produced this score
        double wOutOfBounds, wSmoothness, wCurvature;

        public double total() {
            return (uncovered + unsafeRadius) * HARD_PENALTY
                 + outOfBounds * wOutOfBounds
                 + boundaryJumps * wSmoothness
                 + curvatureRms * wCurvature;
        }
    }

    // Soft term weights (defaults). Instance-configurable so the polish
    // pass can weight smoothness heavily. Relative comparison is what
    // matters for the optimisers; absolute values are only reported.
    public double outOfBoundsWeight = 2000.0;
    public double smoothnessWeight = 1.0;
    public double curvatureWeight = 10.0;

    private final CCLineTrackGeometry geo;
    private final CCLineSimulator simulator;

    public CCLineEvaluator(CCLineTrackGeometry geometry) {
        geo = geometry;
        simulator = new CCLineSimulator(geometry);
    }

    public Score score(CCLine candidate) {
        Score s = new Score();
        s.wOutOfBounds = outOfBoundsWeight;
        s.wSmoothness = smoothnessWeight;
        s.wCurvature = curvatureWeight;
        CCLineSimulator.Result r = simulator.run(candidate);
        s.simulation = r;

        // Border validity and coverage per Seg
        for (int i = 0; i < geo.segCount; i++) {
            if (!r.covered[i])
                s.uncovered++;
            else if (Math.abs(r.ccLine[i]) >= geo.usableBound[i])
                s.outOfBounds++;
        }

        // Unsafe radii are detected by the simulation itself: every
        // Pythagoras clamp activation marks a point where the game's own
        // math would produce garbage (see Score.unsafeRadius).
        s.unsafeRadius = r.clampCount;

        // Smoothness at sector joins and path curvature (speed proxy).
        // Path heading per Seg = track heading + relative angle.
        double dCurvSquares = 0.0;
        for (int i = 0; i < geo.segCount; i++) {
            int j = i + 1;
            if (j >= geo.segCount)
                j = 0;
            if (!r.covered[i] || !r.covered[j])
                continue;
            short headingDiff = (short) ((geo.angleZ[j] + r.ccLineRAngle[j])
                                       - (geo.angleZ[i] + r.ccLineRAngle[i]));
            dCurvSquares += (double) headingDiff * (double) headingDiff;
            if (r.ccLineSector[i] != r.ccLineSector[j]) {
                short jump = (short) (r.ccLineRAngle[j] - r.ccLineRAngle[i]);
                s.boundaryJumps += Math.abs(jump);
            }
        }
        s.curvatureRms = Math.sqrt(dCurvSquares / geo.segCount);

        return s;
    }

    /** Raw radius of a segment (32-bit combined for wide-radius types). */
    public static long rawRadius(CCLineSegment seg) {
        int nType = seg.getType();
        if ((nType & 0x80) != 0 && (nType & 0x40) != 0)
            return ((long) seg.getParam(2) << 16) | (seg.getParam(3) & 0xFFFFL);
        if ((nType & 0x80) != 0)
            return seg.getParam(2);
        if ((nType & 0x40) != 0)
            return ((long) seg.getParam(1) << 16) | (seg.getParam(2) & 0xFFFFL);
        return seg.getParam(1);
    }
}
