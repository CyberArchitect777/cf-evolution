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

    Computes a minimum-curvature racing line by iterative elastic-band
    relaxation: every point of the line is repeatedly pulled towards the
    midpoint of its neighbours (which straightens the path), constrained
    to the drivable corridor. This naturally produces the classic
    out-in-out apex line. The resulting lateral profile is quantised into
    CCLine segments by the shared CCLineQuantizer.
*/
public class MinCurvatureCCLineGenerator implements CCLineGenerator {

    /** Extra safety margin inside the game's usable bound (world units).
        Must comfortably exceed the quantizer's typical tracking error
        (~200 units RMS) or the rebuilt line pokes outside the corridor.
        On narrow tracks (Monaco) a fixed margin would eat the whole
        corridor, so it is capped at a fraction of the local bound. */
    private static final double BOUND_MARGIN = 512.0;
    private static final double BOUND_MARGIN_FRACTION = 0.35;

    private static double margin(double dUsableBound) {
        return Math.min(BOUND_MARGIN, dUsableBound * BOUND_MARGIN_FRACTION);
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

        // Elastic-band relaxation over the closed loop
        int nIterations = Math.max(context.iterations, 200);
        double dRelax = Math.min(Math.max(context.smoothingWeight, 0.1), 1.0);
        for (int iter = 0; iter < nIterations; iter++) {
            if (listener != null && (iter & 0x3F) == 0) {
                if (listener.isCancelled())
                    return null;
                listener.progress(iter * 80 / nIterations, "Relaxing racing line...");
            }
            for (int i = 0; i < n; i++) {
                int prev = (i == 0) ? n - 1 : i - 1;
                int next = (i == n - 1) ? 0 : i + 1;
                // World position of current line at neighbours
                double px = cx[prev] + o[prev] * axisX[prev];
                double py = cy[prev] + o[prev] * axisY[prev];
                double nx = cx[next] + o[next] * axisX[next];
                double ny = cy[next] + o[next] * axisY[next];
                // Pull towards the neighbour midpoint, projected on this
                // Seg's lateral axis
                double mx = (px + nx) / 2.0 - cx[i];
                double my = (py + ny) / 2.0 - cy[i];
                double dTarget = mx * axisX[i] + my * axisY[i];
                o[i] += dRelax * (dTarget - o[i]);
                // Constrain to the drivable corridor
                double dBound = geo.usableBound[i] - margin(geo.usableBound[i]);
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
            double dBound = geo.usableBound[i] - margin(geo.usableBound[i]);
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
