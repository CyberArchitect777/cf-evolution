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

import java.util.Random;

import cfevolution.generator.ccline.CCLineTrackGeometry;

/**
    Option 4, phase 1: iterative fastest-lap search over the lateral
    profile (2026-07-19, from in-game feedback rounds — heuristic line
    construction kept trading one artefact for another; lap time is an
    objective, so weaving, odd approaches and pointless excursions are
    discarded because they are SLOW, not because a rule forbids them).

    Seeded from the constructed racing line. Candidate moves are smooth
    lateral bumps; a candidate that leaves the corridor (physical road
    minus the user's edge standoff) is discarded outright, a slower one
    is rejected, a faster one is kept — plain greedy selection.

    Lap time model: the standard racing three-pass method. Per point, a
    cornering speed cap from local path curvature (v = sqrt(aLat/kappa)),
    then a forward acceleration pass and a backward braking pass around
    the closed lap; time = sum(ds/v). The constants are plausible-F1 in
    game units (1024 units/TLU = 4.87 m) — the optimised SHAPE is robust
    to their exact values, and they can be calibrated against the IDA
    physics later.
*/
public class LapTimeOptimizer {

    private static final double V_TOP = 18000.0;  // ~300 km/h in units/s
    private static final double A_LAT = 6300.0;   // lateral grip, units/s^2
    private static final double A_ACC = 2100.0;   // acceleration
    private static final double A_BRK = 8400.0;   // braking

    private final CCLineTrackGeometry geo;
    private final int n;
    private final double[] cx, cy, ax, ay;   // centreline + lateral axis
    private final double[] px, py;           // work: world points
    private final double[] vlim, v;          // work: speed profile
    private final double[] ds;               // work: step lengths

    public LapTimeOptimizer(CCLineTrackGeometry geometry) {
        geo = geometry;
        n = geo.segCount;
        cx = new double[n]; cy = new double[n];
        ax = new double[n]; ay = new double[n];
        px = new double[n]; py = new double[n];
        vlim = new double[n]; v = new double[n];
        ds = new double[n];
        for (int i = 0; i < n; i++) {
            cx[i] = geo.posX[i];
            cy[i] = geo.posY[i];
            double dRad = geo.angleZ[i] * 2.0 * Math.PI / 65536.0;
            ax[i] = Math.cos(dRad);
            ay[i] = -Math.sin(dRad);
        }
    }

    /** Optimises the profile in place. Returns {seed lap time, final lap
        time} in model seconds. nEvaluations candidate moves are tried. */
    public double[] optimize(double[] o, double dStandoffFraction,
                             int nEvaluations, Random rand) {
        double[] bound = new double[n];
        for (int i = 0; i < n; i++)
            bound[i] = geo.physicalBound[i] * (1.0 - Math.max(0.05, dStandoffFraction));
        // The seed must be inside the corridor or every move is stillborn
        for (int i = 0; i < n; i++) {
            if (o[i] > bound[i]) o[i] = bound[i];
            else if (o[i] < -bound[i]) o[i] = -bound[i];
        }

        double dSeedTime = lapTime(o);
        double dBest = dSeedTime;
        double[] cand = new double[n];

        for (int e = 0; e < nEvaluations; e++) {
            // Smooth cosine bump: centre, half-width 4..48, amplitude
            // decaying as the search converges
            int nCentre = rand.nextInt(n);
            int nHalf = 4 + rand.nextInt(45);
            double dScale = 1.0 - (double) e / nEvaluations;
            double dAmp = (rand.nextGaussian())
                          * (40.0 + 400.0 * dScale * dScale);
            System.arraycopy(o, 0, cand, 0, n);
            boolean fInside = true;
            for (int k = -nHalf; k <= nHalf; k++) {
                int i = ((nCentre + k) % n + n) % n;
                double w = 0.5 * (1.0 + Math.cos(Math.PI * k / (nHalf + 1)));
                double nv = cand[i] + dAmp * w;
                if (nv > bound[i] || nv < -bound[i]) {
                    fInside = false; // off the allowed corridor: discard
                    break;
                }
                cand[i] = nv;
            }
            if (!fInside)
                continue;
            double dTime = lapTime(cand);
            if (dTime < dBest) {
                dBest = dTime;
                System.arraycopy(cand, 0, o, 0, n);
            }
        }
        return new double[] { dSeedTime, dBest };
    }

    /** Lap time of a lateral profile under the three-pass speed model. */
    public double lapTime(double[] o) {
        // World points
        for (int i = 0; i < n; i++) {
            px[i] = cx[i] + o[i] * ax[i];
            py[i] = cy[i] + o[i] * ay[i];
        }
        // Step lengths and curvature -> cornering speed cap
        for (int i = 0; i < n; i++) {
            int p = (i + n - 1) % n, q = (i + 1) % n;
            double d1x = px[i] - px[p], d1y = py[i] - py[p];
            double d2x = px[q] - px[i], d2y = py[q] - py[i];
            double l1 = Math.sqrt(d1x * d1x + d1y * d1y);
            double l2 = Math.sqrt(d2x * d2x + d2y * d2y);
            double lc = Math.sqrt((d1x + d2x) * (d1x + d2x) + (d1y + d2y) * (d1y + d2y));
            ds[i] = l1;
            double dCross = Math.abs(d1x * d2y - d1y * d2x);
            double dDenom = l1 * l2 * lc;
            double dKappa = (dDenom > 1e-9) ? 2.0 * dCross / dDenom : 0.0;
            vlim[i] = (dKappa > 1e-12)
                ? Math.min(V_TOP, Math.sqrt(A_LAT / dKappa)) : V_TOP;
        }
        // Start at the cornering caps, then relax: forward sweeps enforce
        // reachable acceleration, backward sweeps enforce braking
        // distances; two rounds settle the closed lap
        System.arraycopy(vlim, 0, v, 0, n);
        for (int round = 0; round < 2; round++) {
            for (int i = 0; i < n; i++) {
                int q = (i + 1) % n;
                double vNext = Math.sqrt(v[i] * v[i] + 2.0 * A_ACC * ds[q]);
                if (vNext < v[q])
                    v[q] = vNext;
            }
            for (int i = n - 1; i >= 0; i--) {
                int q = (i + 1) % n;
                double vHere = Math.sqrt(v[q] * v[q] + 2.0 * A_BRK * ds[q]);
                if (vHere < v[i])
                    v[i] = vHere;
            }
        }
        double dTime = 0.0;
        for (int i = 0; i < n; i++)
            dTime += ds[i] / Math.max(1.0, v[i]);
        return dTime;
    }
}
