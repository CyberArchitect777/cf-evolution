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

import java.util.Random;

import cfevolution.data.track.CCLine;

/**
    Smoothness polish pass shared by the generation methods.

    The greedy quantizer tracks its target profile well but steers with
    instant heading kicks — in-game the AI visibly fights such lines
    (2026-07-15 testing: cars thrown wide, crashes). This pass anneals the
    quantised line with smoothness-dominated scoring (boundary heading
    jumps weighted heavily) plus a drift penalty that keeps the line close
    to the profile it was quantised from, using the mutation moves of the
    refinement method.
*/
public class CCLinePolisher {

    /** Diagnostic counters (harness use only): tried, null, accepted,
        rejected-invalid, rejected-score for the conversion move. */
    public static long[] DEBUG_STATS = null;

    /** Weight of profile drift (RMS world units) in the polish score. */
    private static final double DRIFT_WEIGHT = 30.0;
    /** Smoothness weighting during polish (default evaluator uses 1.0). */
    private static final double POLISH_SMOOTHNESS_WEIGHT = 15.0;

    /** Polishes the line in place-ish (returns the best variant found).
        progressFrom/progressTo bound the listener percentage range.
        Returns null only if cancelled. */
    public static CCLine polish(CCLineGeneratorContext context, CCLine line,
                                int nIterations, CCLineProgressListener listener,
                                int nProgressFrom, int nProgressTo) {
        CCLineEvaluator ev = new CCLineEvaluator(context.geometry);
        ev.smoothnessWeight = POLISH_SMOOTHNESS_WEIGHT;

        // Reference profile: what the unpolished line actually does
        CCLineEvaluator.Score seedScore = ev.score(line);
        short[] reference = seedScore.simulation.ccLine.clone();
        int nCurrentOob = seedScore.outOfBounds;

        CCLine current = cfevolution.generator.ccline.refine.RefinementCCLineGenerator.copyLine(line);
        double dCurrent = polishTotal(ev, seedScore, reference);
        CCLine best = current;
        double dBest = dCurrent;

        Random rand = new Random(4242);
        for (int iter = 0; iter < nIterations; iter++) {
            if (listener != null && (iter & 0x7F) == 0) {
                if (listener.isCancelled())
                    return null;
                listener.progress(nProgressFrom
                    + iter * (nProgressTo - nProgressFrom) / nIterations,
                    "Smoothing line... ");
            }
            CCLine candidate;
            boolean fConversion = false;
            switch (rand.nextInt(3)) {
            case 0:
                candidate = cfevolution.generator.ccline.refine.RefinementCCLineGenerator.copyLine(current);
                if (!cfevolution.generator.ccline.refine.RefinementCCLineGenerator.mutate(candidate, rand))
                    candidate = null;
                break;
            case 1:
                candidate = cfevolution.generator.ccline.refine.RefinementCCLineGenerator.copyLine(current);
                if (!transferCorrection(candidate, rand))
                    candidate = null;
                break;
            default:
                candidate = convertKickToArc(context.geometry, current, rand);
                fConversion = true;
                if (DEBUG_STATS != null)
                    DEBUG_STATS[0]++;
                if (candidate == null && DEBUG_STATS != null)
                    DEBUG_STATS[1]++;
                break;
            }
            if (candidate == null)
                continue;
            CCLineEvaluator.Score s = ev.score(candidate);
            double dCand = polishTotal(ev, s, reference);
            if (fConversion && DEBUG_STATS != null) {
                if (dCand <= dCurrent) DEBUG_STATS[2]++;
                else if (!s.isValid()) DEBUG_STATS[3]++;
                else DEBUG_STATS[4]++;
                if (DEBUG_STATS.length > 6) {
                    if (s.uncovered > 0) DEBUG_STATS[5]++;
                    if (s.unsafeRadius > 0) DEBUG_STATS[6]++;
                }
            }
            // Never trade smoothness for new out-of-bounds Segs (same
            // hard rule as the refinement annealer, 2026-07-19)
            if (s.outOfBounds > nCurrentOob)
                continue;
            if (dCand <= dCurrent) { // hill climb; plateau moves allowed
                current = candidate;
                nCurrentOob = s.outOfBounds;
                dCurrent = dCand;
                if (dCand < dBest) {
                    best = candidate;
                    dBest = dCand;
                }
            }
        }
        return best;
    }

    /** Moves heading correction between two consecutive straight sectors
        (c_i += d, c_i+1 -= d). Because the line is stateful, this is the
        only cheap move that reshapes a kick while keeping the downstream
        line anchored — single-sector changes shift everything after them
        and are rejected by the drift penalty. */
    private static boolean transferCorrection(CCLine line, Random rand) {
        int nCount = line.size();
        if (nCount < 3)
            return false;
        int nIndex = 1 + rand.nextInt(nCount - 1);
        cfevolution.data.track.CCLineSegment a = line.getAt(nIndex);
        cfevolution.data.track.CCLineSegment b = line.getAt(nIndex + 1);
        if (CCLineEvaluator.rawRadius(a) != 0 || CCLineEvaluator.rawRadius(b) != 0)
            return false; // corrections mean angle only on straight sectors
        int nParamA = ((a.getType() & 0x80) != 0) ? 1 : 0;
        int nParamB = ((b.getType() & 0x80) != 0) ? 1 : 0;
        int d = (rand.nextInt(2) == 0 ? 1 : -1) * (8 << rand.nextInt(5)); // 8..128
        int nNewA = a.getParam(nParamA) + d;
        int nNewB = b.getParam(nParamB) - d;
        if (nNewA > Short.MAX_VALUE || nNewA < Short.MIN_VALUE
            || nNewB > Short.MAX_VALUE || nNewB < Short.MIN_VALUE)
            return false;
        a.setParam(nParamA, nNewA);
        b.setParam(nParamB, nNewB);
        return true;
    }

    /** Converts (part of) a large heading kick into a tangent arc — a
        straight sector STR(L, K) becomes STR(1, K·f) + ARC(L−1, r), the
        corner move hand-tuned lines use — and repairs the downstream
        window via requantization. The repair is what makes the move
        viable at all: no kick+arc replacement can preserve both end
        position and end heading of a kick-straight (its chord lies along
        the post-kick heading), so without the window re-fit every
        conversion de-anchors the rest of the stateful lap and is
        rejected by the drift guard (Session 12 finding). */
    private static CCLine convertKickToArc(CCLineTrackGeometry geo, CCLine line,
                                           Random rand) {
        int nCount = line.size();
        if (nCount < 2)
            return null;
        // Straight sectors long enough to split, ranked by kick size
        java.util.Vector big = new java.util.Vector();
        for (int i = 1; i <= nCount; i++) {
            cfevolution.data.track.CCLineSegment sg = line.getAt(i);
            if (CCLineEvaluator.rawRadius(sg) != 0 || sg.getTlu() < 4)
                continue;
            int ci = ((sg.getType() & 0x80) != 0) ? 1 : 0;
            if (Math.abs(sg.getParam(ci)) < 512)
                continue; // small kicks are already followable
            big.add(new int[] { i, Math.abs(sg.getParam(ci)) });
        }
        if (big.isEmpty())
            return null;
        for (int a = 0; a < big.size(); a++) // selection sort, descending
            for (int b = a + 1; b < big.size(); b++)
                if (((int[]) big.get(b))[1] > ((int[]) big.get(a))[1]) {
                    Object tmp = big.get(a); big.set(a, big.get(b)); big.set(b, tmp);
                }
        int nPick = Math.min(rand.nextInt(3), big.size() - 1);
        int nIndex = ((int[]) big.get(nPick))[0];

        cfevolution.data.track.CCLineSegment sg = line.getAt(nIndex);
        int ci = ((sg.getType() & 0x80) != 0) ? 1 : 0;
        int nKick = sg.getParam(ci);
        int nLen = sg.getTlu();
        double[] adKeep = { 0.0, 0.25, 0.5 };
        int nTheta = nKick - (int) Math.round(nKick * adKeep[rand.nextInt(3)]);
        if (nTheta == 0)
            return null;
        // A single arc can deliver at most ~1 radian on straight track:
        // theta = L*1024/(8*|raw|) with the Pythagoras-safe worst case
        // |raw| >= L*128. Cap the arc's rotation there and leave the rest
        // in the lead kick — radii below the safe bound made nearly every
        // conversion clamp-invalid before this cap (measured 2026-07-18:
        // 2989 of 3300 attempts on F1CT01).
        double dThetaRad = Math.abs(nTheta) * 2.0 * Math.PI / 65536.0;
        if (dThetaRad > 1.0)
            dThetaRad = 1.0;
        long lRaw = Math.round(((nLen - 1) * 1024.0 / dThetaRad) / 8.0);
        int nDelivered = (int) Math.round(dThetaRad * 65536.0 / (2.0 * Math.PI));
        if (nTheta < 0)
            nDelivered = -nDelivered;
        long r = (nTheta >= 0) ? lRaw : -lRaw;

        // Replacement pair: 1-TLU kick of the remainder + the arc
        cfevolution.data.track.CCLineSegment lead =
            new cfevolution.data.track.CCLineSegment(sg.getType());
        lead.setTlu(1);
        for (int p = 0; p < 4; p++)
            lead.setParam(p, sg.getParam(p));
        lead.setParam(ci, nKick - nDelivered);

        cfevolution.data.track.CCLineSegment arc;
        if (r > Short.MAX_VALUE || r < Short.MIN_VALUE) {
            arc = new cfevolution.data.track.CCLineSegment(0x40);
            arc.setParam(0, 0);
            arc.setParam(1, (int) (r >> 16));
            arc.setParam(2, (int) (short) (r & 0xFFFF));
        }
        else {
            arc = new cfevolution.data.track.CCLineSegment(0x00);
            arc.setParam(0, 0);
            arc.setParam(1, (int) r);
        }
        arc.setTlu(nLen - 1);

        return CCLineWindowRepair.replaceAndRepair(geo, line, nIndex,
            new cfevolution.data.track.CCLineSegment[] { lead, arc },
            CCLineWindowRepair.DEFAULT_WINDOW_TLU, null);
    }

    /** Polish score: smoothness-weighted evaluator total plus drift from
        the reference profile. */
    private static double polishTotal(CCLineEvaluator ev, CCLineEvaluator.Score s,
                                      short[] reference) {
        double dSumSq = 0.0;
        int n = 0;
        for (int i = 0; i < reference.length; i++) {
            if (!s.simulation.covered[i])
                continue;
            double d = s.simulation.ccLine[i] - reference[i];
            dSumSq += d * d;
            n++;
        }
        double dDriftRms = n > 0 ? Math.sqrt(dSumSq / n) : 0.0;
        return s.total() + dDriftRms * DRIFT_WEIGHT;
    }
}
