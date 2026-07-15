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
            CCLine candidate =
                cfevolution.generator.ccline.refine.RefinementCCLineGenerator.copyLine(current);
            boolean fMutated = (rand.nextInt(2) == 0)
                ? cfevolution.generator.ccline.refine.RefinementCCLineGenerator.mutate(candidate, rand)
                : transferCorrection(candidate, rand);
            if (!fMutated)
                continue;
            CCLineEvaluator.Score s = ev.score(candidate);
            double dCand = polishTotal(ev, s, reference);
            if (dCand <= dCurrent) { // hill climb; plateau moves allowed
                current = candidate;
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
