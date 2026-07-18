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

package cfevolution.generator.ccline.geometric;

import cfevolution.data.track.CCLine;
import cfevolution.generator.ccline.*;

/**
    Approach 1: geometric optimisation.

    Computes a minimum-curvature line by Gauss-Seidel relaxation on the
    squared second differences of the path, constrained to an
    originals-scale amplitude envelope (AMPLITUDE_FRACTION): the game's
    hand-tuned lines never leave ~15% of the drivable bound, and matching
    that scale is what the AI can actually follow — wide "racing line"
    profiles demand corrections that throw the cars (2026-07-18). The
    resulting lateral profile is quantised into CCLine segments by the
    shared CCLineQuantizer.
*/
public class MinCurvatureCCLineGenerator implements CCLineGenerator {

    /** Extra safety margin inside the game's usable bound (world units).
        Must comfortably exceed the quantizer's typical tracking error
        (~200 units RMS) or the rebuilt line pokes outside the corridor.
        On narrow tracks (Monaco) a fixed margin would eat the whole
        corridor, so it is capped at a fraction of the local bound. */
    private static final double BOUND_MARGIN = 768.0;
    private static final double BOUND_MARGIN_FRACTION = 0.40;

    private static double margin(double dUsableBound) {
        return Math.min(BOUND_MARGIN, dUsableBound * BOUND_MARGIN_FRACTION);
    }

    /** Weight of the path-length term blended into the minimum-curvature
        relaxation. Curvature is indifferent on straights (any parallel
        line scores the same), so a small length pull keeps them stable;
        too much recreates the old taut-string mid-track line. */
    private static final double LENGTH_BLEND = 0.02;

    /** Amplitude envelope as a fraction of the usable bound. Measured
        2026-07-18: every hand-tuned original stays within 12-19% of the
        local bound (max |wCCLine| 1,200-1,900 on bounds of 8,000-13,000)
        — the game's AI is tuned for gentle, near-centre lines, and our
        earlier full-corridor profiles demanded corrections it cannot
        follow. Minimum-curvature shape inside an originals-scale
        envelope is the compatible target. */
    private static final double AMPLITUDE_FRACTION = 0.15;

    private static double bound(double dUsableBound) {
        return Math.min(dUsableBound * AMPLITUDE_FRACTION,
                        dUsableBound - margin(dUsableBound));
    }

    public String getName() {
        return "Geometric (Minimum Curvature)";
    }

    public CCLineGenerationResult generate(CCLineGeneratorContext context,
                                           CCLineProgressListener listener) throws Exception {
        CCLineTrackGeometry geo = context.geometry;
        int n = geo.segCount;

        // Precompute centreline points and the lateral axis per Seg
        double[] cx = new double[n], cy = new double[n];
        double[] axisX = new double[n], axisY = new double[n];
        for (int i = 0; i < n; i++) {
            cx[i] = geo.posX[i];
            cy[i] = geo.posY[i];
            double dRad = geo.angleZ[i] * 2.0 * Math.PI / 65536.0;
            // worldPoint: X += offset * cos(angle), Y -= offset * sin(angle)
            axisX[i] = Math.cos(dRad);
            axisY[i] = -Math.sin(dRad);
        }

        CCLineLateralProfile profile = new CCLineLateralProfile(n);
        double[] o = profile.offset; // starts on the centreline (all zero)

        // Minimum-curvature relaxation over the closed loop (the previous
        // midpoint-pull loop minimised path LENGTH — a taut string).
        // Each sweep solves every point's 1-D quadratic exactly
        // (Gauss-Seidel): with D_j the second difference at j and this
        // point's own contribution removed (a1/a2/a3), the optimum along
        // the lateral axis N is (2*a2·N − a1·N − a3·N) / 6. A small
        // length term (LENGTH_BLEND toward the neighbour midpoint) keeps
        // straights — where curvature is indifferent — well-behaved.
        int nIterations = Math.max(context.iterations, 200);
        double dRelax = Math.min(Math.max(context.smoothingWeight, 0.1), 1.0);
        for (int iter = 0; iter < nIterations; iter++) {
            if (listener != null && (iter & 0x3F) == 0) {
                if (listener.isCancelled())
                    return null;
                listener.progress(iter * 80 / nIterations, "Relaxing racing line...");
            }
            for (int i = 0; i < n; i++) {
                int m2 = (i + n - 2) % n, m1 = (i + n - 1) % n;
                int p1 = (i + 1) % n, p2 = (i + 2) % n;
                double xm2 = cx[m2] + o[m2] * axisX[m2], ym2 = cy[m2] + o[m2] * axisY[m2];
                double xm1 = cx[m1] + o[m1] * axisX[m1], ym1 = cy[m1] + o[m1] * axisY[m1];
                double xp1 = cx[p1] + o[p1] * axisX[p1], yp1 = cy[p1] + o[p1] * axisY[p1];
                double xp2 = cx[p2] + o[p2] * axisX[p2], yp2 = cy[p2] + o[p2] * axisY[p2];
                // Second differences around i with o[i]'s contribution removed
                double a1x = xm2 - 2.0 * xm1 + cx[i];
                double a1y = ym2 - 2.0 * ym1 + cy[i];
                double a2x = xm1 - 2.0 * cx[i] + xp1;
                double a2y = ym1 - 2.0 * cy[i] + yp1;
                double a3x = cx[i] - 2.0 * xp1 + xp2;
                double a3y = cy[i] - 2.0 * yp1 + yp2;
                double dCurvTarget = (2.0 * (a2x * axisX[i] + a2y * axisY[i])
                                      - (a1x * axisX[i] + a1y * axisY[i])
                                      - (a3x * axisX[i] + a3y * axisY[i])) / 6.0;
                // Mild pull toward the neighbour midpoint (path length)
                double dMidTarget = ((xm1 + xp1) / 2.0 - cx[i]) * axisX[i]
                                  + ((ym1 + yp1) / 2.0 - cy[i]) * axisY[i];
                double dTarget = (1.0 - LENGTH_BLEND) * dCurvTarget
                               + LENGTH_BLEND * dMidTarget;
                o[i] += dRelax * (dTarget - o[i]);
                // Constrain to the drivable corridor
                double dBound = bound(geo.usableBound[i]);
                if (dBound < 0)
                    dBound = 0;
                if (o[i] > dBound)
                    o[i] = dBound;
                else if (o[i] < -dBound)
                    o[i] = -dBound;
            }
        }

        // The corridor clamp leaves kinks at corner entries; smooth them
        // out so the quantizer is not forced into abrupt heading kicks.
        double[] smoothed = new double[n];
        for (int pass = 0; pass < 4; pass++) {
            for (int i = 0; i < n; i++) {
                int p2 = (i + n - 2) % n, p1 = (i + n - 1) % n;
                int n1 = (i + 1) % n, n2 = (i + 2) % n;
                smoothed[i] = (o[p2] + 2.0 * o[p1] + 3.0 * o[i] + 2.0 * o[n1] + o[n2]) / 9.0;
            }
            System.arraycopy(smoothed, 0, o, 0, n);
        }
        for (int i = 0; i < n; i++) {
            double dBound = bound(geo.usableBound[i]);
            if (dBound < 0)
                dBound = 0;
            if (o[i] > dBound)
                o[i] = dBound;
            else if (o[i] < -dBound)
                o[i] = -dBound;
        }

        if (listener != null) {
            if (listener.isCancelled())
                return null;
            listener.progress(85, "Quantising into CCLine segments...");
        }

        CCLine ccLine = new CCLineQuantizer(geo, profile, context.seamOvershoot).quantize();

        // Smoothness polish: the greedy quantizer steers with heading
        // kicks that destabilise the AI in-game; anneal them out.
        CCLine polished = CCLinePolisher.polish(context, ccLine,
            Math.max(context.iterations * 4, 10000), listener, 88, 98);
        if (polished == null)
            return null; // cancelled
        ccLine = polished;

        CCLineEvaluator.Score score = context.evaluator.score(ccLine);

        if (listener != null)
            listener.progress(100, "Done");

        CCLineGenerationResult result = new CCLineGenerationResult(ccLine, score);
        if (!score.isValid()) {
            if (score.outOfBounds > 0)
                result.addWarning(score.outOfBounds + " Seg(s) outside the drivable bound");
            if (score.uncovered > 0)
                result.addWarning(score.uncovered + " Seg(s) not covered by the line");
            if (score.unsafeRadius > 0)
                result.addWarning(score.unsafeRadius + " sector(s) with unsafe radius");
        }
        return result;
    }
}
