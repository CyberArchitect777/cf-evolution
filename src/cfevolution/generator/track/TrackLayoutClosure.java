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

package cfevolution.generator.track;

import java.util.Vector;

import cfevolution.data.track.Seg;
import cfevolution.data.track.Track;
import cfevolution.data.track.TrackSegment;
import cfevolution.data.track.TrackSegments;

/**
    Loop closure for generated track layouts.

    Heading closure is exact integer arithmetic (sum of tlu * curvature
    must reach the winding target). Position closure is solved against the
    editor's own track compilation: the primitives are applied to a
    scratch Track, compiled WITHOUT the game's closing correction
    (layout mode true skips TCRecalcPosToFit), the end-to-start gap is
    measured from the compiled Segs, and the lengths of two straights with
    well-separated headings are adjusted to cancel it. The remaining
    residual (target: below the worst natural residual of the original
    tracks, ~1 TLU) is left to TCRecalcPosToFit, exactly as the originals
    leave theirs.
*/
public class TrackLayoutClosure {

    /** One layout primitive: a straight (curvature 0) or an arc. */
    public static class Prim {
        public int tlu;
        public int curv;

        public Prim(int tlu, int curv) {
            this.tlu = tlu;
            this.curv = curv;
        }
    }

    /** Worst gap accepted as closed (world units; originals reach ~1043). */
    public static final double GAP_TARGET = 700.0;

    /** Heading residual tolerance (game angle units; originals reach ~200). */
    public static final int HEADING_TOLERANCE = 64;

    private static final int MIN_STRAIGHT = 4;
    private static final int MAX_LENGTH = 191;   // longest section in original data
    private static final int MAX_CURV = 0x2000;  // engine limit

    private final Track scratch;

    public TrackLayoutClosure(Track scratchTrack) {
        scratch = scratchTrack;
    }

    /** Drives the summed turn to the winding target by spreading integer
        curvature corrections over the arcs. Returns the residual. */
    public static int closeHeading(Vector prims, int nTarget) {
        for (int pass = 0; pass < 64; pass++) {
            long lNet = 0;
            for (int i = 0; i < prims.size(); i++) {
                Prim p = (Prim) prims.get(i);
                lNet += (long) p.tlu * p.curv;
            }
            long lDelta = nTarget - lNet;
            if (Math.abs(lDelta) <= HEADING_TOLERANCE)
                return (int) lDelta;
            boolean fChanged = false;
            for (int i = 0; i < prims.size() && Math.abs(lDelta) > HEADING_TOLERANCE; i++) {
                Prim p = (Prim) prims.get(i);
                if (p.curv == 0)
                    continue;
                // Correcting this arc's curvature by d changes the net by tlu * d
                int d = (int) (lDelta / p.tlu);
                int nMaxStep = 128; // spread gently over many arcs
                if (d > nMaxStep) d = nMaxStep;
                if (d < -nMaxStep) d = -nMaxStep;
                if (d == 0)
                    d = lDelta > 0 ? 1 : -1;
                int nNew = p.curv + d;
                // keep the arc's character: same sign, inside engine limit
                if (nNew == 0 || Integer.signum(nNew) != Integer.signum(p.curv)
                    || Math.abs(nNew) > MAX_CURV)
                    continue;
                p.curv = nNew;
                lDelta -= (long) p.tlu * d;
                fChanged = true;
            }
            if (!fChanged)
                break;
        }
        long lNet = 0;
        for (int i = 0; i < prims.size(); i++) {
            Prim p = (Prim) prims.get(i);
            lNet += (long) p.tlu * p.curv;
        }
        return (int) (nTarget - lNet);
    }

    /** Applies the primitives to the scratch track and compiles WITHOUT
        the closing correction. */
    public void compile(Vector prims) {
        TrackSegments segs = scratch.getTrackSegments();

        // Preserve the donor's dummy terminator segment (last element),
        // stripped of its commands.
        TrackSegment dummy = (TrackSegment) segs.get(segs.size() - 1);
        dummy.setCommands(new Vector());

        segs.clear();
        for (int i = 0; i < prims.size(); i++) {
            Prim p = (Prim) prims.get(i);
            TrackSegment ts = new TrackSegment();
            ts.setTlu(p.tlu);
            ts.setCurvature(p.curv);
            segs.add(ts);
        }
        segs.add(dummy);

        scratch.setLayoutMode(true); // compile without TCRecalcPosToFit
        scratch.calculateTrackLayout();
    }

    /** End-to-start gap of the compiled layout: distance from the position
        one TLU beyond the last Seg to the first Seg. Returns {gapX, gapY}. */
    public double[] measureGap() {
        TrackSegments segs = scratch.getTrackSegments();
        Seg first = segs.getSegAt(0);
        Seg last = segs.getSegAt(segs.getMaxTrackSegIndex());
        double dRad = last.getAngleZ() * 2.0 * Math.PI / 65536.0;
        double dNextX = last.getPosX() + Math.sin(dRad) * 1024.0;
        double dNextY = last.getPosY() + Math.cos(dRad) * 1024.0;
        return new double[] { first.getPosX() - dNextX, first.getPosY() - dNextY };
    }

    /** Iteratively adjusts straight lengths until the compiled gap is at
        most GAP_TARGET. Returns the final gap length (may exceed the
        target if closure failed — caller retries with a new layout). */
    public double closePosition(Vector prims) {
        double dGap = Double.MAX_VALUE;
        for (int iter = 0; iter < 12; iter++) {
            compile(prims);
            double[] g = measureGap();
            dGap = Math.sqrt(g[0] * g[0] + g[1] * g[1]);
            if (dGap <= GAP_TARGET)
                return dGap;

            // Heading of each straight, from the compiled Segs
            TrackSegments segs = scratch.getTrackSegments();
            int nCumTlu = 0;
            int nBestA = -1, nBestB = -1;
            double dBestCross = 0;
            double[][] adDir = new double[prims.size()][];
            for (int i = 0; i < prims.size(); i++) {
                Prim p = (Prim) prims.get(i);
                if (p.curv == 0 && i > 0 && p.tlu >= MIN_STRAIGHT) {
                    double dRad = segs.getSegAt(nCumTlu).getAngleZ() * 2.0 * Math.PI / 65536.0;
                    adDir[i] = new double[] { Math.sin(dRad) * 1024.0, Math.cos(dRad) * 1024.0 };
                }
                nCumTlu += p.tlu;
            }
            // Pick the straight pair with the most orthogonal headings
            for (int a = 0; a < prims.size(); a++) {
                if (adDir[a] == null)
                    continue;
                for (int b = a + 1; b < prims.size(); b++) {
                    if (adDir[b] == null)
                        continue;
                    double dCross = Math.abs(adDir[a][0] * adDir[b][1] - adDir[a][1] * adDir[b][0]);
                    if (dCross > dBestCross) {
                        dBestCross = dCross;
                        nBestA = a;
                        nBestB = b;
                    }
                }
            }
            if (nBestA < 0 || dBestCross < 1.0)
                return dGap; // no usable straight pair

            // Solve dLa * dirA + dLb * dirB = gap
            double[] uA = adDir[nBestA], uB = adDir[nBestB];
            double dDet = uA[0] * uB[1] - uA[1] * uB[0];
            double dLa = (g[0] * uB[1] - g[1] * uB[0]) / dDet;
            double dLb = (uA[0] * g[1] - uA[1] * g[0]) / dDet;

            Prim pa = (Prim) prims.get(nBestA);
            Prim pb = (Prim) prims.get(nBestB);
            int nLa = clampLength(pa.tlu + (int) Math.round(dLa));
            int nLb = clampLength(pb.tlu + (int) Math.round(dLb));
            if (nLa == pa.tlu && nLb == pb.tlu)
                return dGap; // clamped into a fixed point; give up
            pa.tlu = nLa;
            pb.tlu = nLb;
        }
        compile(prims);
        double[] g = measureGap();
        return Math.sqrt(g[0] * g[0] + g[1] * g[1]);
    }

    private static int clampLength(int nLen) {
        if (nLen < MIN_STRAIGHT) return MIN_STRAIGHT;
        if (nLen > MAX_LENGTH) return MAX_LENGTH;
        return nLen;
    }
}
