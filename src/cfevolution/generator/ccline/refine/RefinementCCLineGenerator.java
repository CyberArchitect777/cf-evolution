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

package cfevolution.generator.ccline.refine;

import java.util.Random;

import cfevolution.data.track.CCLine;
import cfevolution.data.track.CCLineSegment;
import cfevolution.generator.ccline.*;
import cfevolution.generator.ccline.geometric.MinCurvatureCCLineGenerator;

/**
    Approach 3: simulator-driven iterative refinement.

    Simulated annealing directly on the CCLine parameters, scored by the
    game-accurate CCLineEvaluator: each iteration perturbs one sector
    (radius, correction, or a length shift against its neighbour, keeping
    the total TLU constant) or splits a long sector, and keeps the change
    if it improves the score — or occasionally even if it does not, with
    a probability that falls as the run cools. Seeded from the track's
    existing CCLine, or from the geometric method when there is none.

    This is the honest in-editor equivalent of the "reinforcement
    learning" idea: it learns by interacting with the exact code path the
    game uses to interpret the line. A physics-based lap-time reward
    remains a documented future consideration.
*/
public class RefinementCCLineGenerator implements CCLineGenerator {

    public String getName() {
        return "Iterative Refinement";
    }

    public CCLineGenerationResult generate(CCLineGeneratorContext context,
                                           CCLineProgressListener listener) throws Exception {
        CCLineEvaluator evaluator = context.evaluator;

        // Seed: current line, or geometric output for an empty track
        CCLine current;
        if (context.track.getCCLine().size() > 0) {
            current = copyLine(context.track.getCCLine());
        }
        else {
            if (listener != null)
                listener.progress(0, "No existing line — seeding from geometric method...");
            CCLineGenerationResult seed =
                new MinCurvatureCCLineGenerator().generate(context, listener);
            if (seed == null)
                return null;
            current = seed.ccLine;
        }

        CCLineEvaluator.Score currentScore = evaluator.score(current);
        double dCurrent = currentScore.total();
        CCLine best = copyLine(current);
        CCLineEvaluator.Score bestScore = currentScore;
        double dBest = dCurrent;
        double dSeedTotal = dCurrent;

        int nIterations = Math.max(context.iterations, 100);
        Random rand = new Random(42); // reproducible runs
        double dStartTemp = Math.max(dCurrent * 0.001, 100.0);

        for (int iter = 0; iter < nIterations; iter++) {
            if (listener != null && (iter & 0x3F) == 0) {
                if (listener.isCancelled())
                    return null;
                listener.progress(iter * 100 / nIterations,
                    "Annealing... score " + (long) dBest);
            }
            double dTemp = dStartTemp * (1.0 - (double) iter / nIterations);

            CCLine candidate = copyLine(current);
            if (!mutate(candidate, rand))
                continue;

            double dCand = evaluator.score(candidate).total();
            double dDelta = dCand - dCurrent;
            if (dDelta <= 0
                || (dTemp > 0 && rand.nextDouble() < Math.exp(-dDelta / dTemp))) {
                current = candidate;
                dCurrent = dCand;
                if (dCand < dBest) {
                    best = copyLine(candidate);
                    dBest = dCand;
                }
            }
        }

        bestScore = evaluator.score(best);
        if (listener != null)
            listener.progress(100, "Done");

        CCLineGenerationResult result = new CCLineGenerationResult(best, bestScore);
        result.addWarning(String.format("Score %d -> %d (lower is better)",
            (long) dSeedTotal, (long) bestScore.total()));
        if (!bestScore.isValid()) {
            if (bestScore.outOfBounds > 0)
                result.addWarning(bestScore.outOfBounds + " Seg(s) outside the drivable bound");
            if (bestScore.uncovered > 0)
                result.addWarning(bestScore.uncovered + " Seg(s) not covered by the line");
            if (bestScore.unsafeRadius > 0)
                result.addWarning(bestScore.unsafeRadius + " sector(s) with unsafe radius");
        }
        return result;
    }

    /** Applies one random mutation. Returns false when the drawn move was
        not applicable (caller just draws again next iteration). */
    private boolean mutate(CCLine line, Random rand) {
        int nCount = line.size();
        if (nCount == 0)
            return false;
        int nIndex = 1 + rand.nextInt(nCount); // 1-based
        CCLineSegment seg = line.getAt(nIndex);
        int nMove = rand.nextInt(4);

        switch (nMove) {
        case 0: { // perturb radius (multiplicative; a radius that breaks the
                  // game's arc math is caught by the evaluator's clamp count)
            long lRaw = CCLineEvaluator.rawRadius(seg);
            if (lRaw == 0)
                return false;
            double dFactor = 1.0 + (rand.nextDouble() - 0.5) * 0.2;
            long lNew = Math.round(lRaw * dFactor);
            if (lNew == 0 || Math.abs(lNew) > 0x3FFFFFFFL)
                return false;
            return setRadius(seg, lNew);
        }
        case 1: { // perturb correction
            int nParamIndex = ((seg.getType() & 0x80) != 0) ? 1 : 0;
            int nCorr = seg.getParam(nParamIndex) + (rand.nextInt(129) - 64);
            if (nCorr > Short.MAX_VALUE || nCorr < Short.MIN_VALUE)
                return false;
            seg.setParam(nParamIndex, nCorr);
            return true;
        }
        case 2: { // shift length to the next sector (total TLU unchanged)
            if (nIndex >= nCount)
                return false;
            CCLineSegment next = line.getAt(nIndex + 1);
            int nShift = rand.nextInt(9) - 4;
            int nLen = seg.getTlu() + nShift;
            int nNextLen = next.getTlu() - nShift;
            if (nLen < 1 || nLen > 255 || nNextLen < 1 || nNextLen > 255)
                return false;
            seg.setTlu(nLen);
            next.setTlu(nNextLen);
            return true;
        }
        default: { // split a long sector in half
            int nLen = seg.getTlu();
            if (nLen < 16 || (seg.getType() & 0x80) != 0)
                return false;
            int nHalf = nLen / 2;
            CCLineSegment tail = copySegment(seg);
            // corrections apply at sector start; the tail continues without one
            tail.setParam(((tail.getType() & 0x80) != 0) ? 1 : 0, 0);
            seg.setTlu(nHalf);
            tail.setTlu(nLen - nHalf);
            line.add(nIndex, tail); // Vector insert after seg (0-based index nIndex)
            return true;
        }
        }
    }

    /** Writes a raw radius back into the segment's parameters, keeping the
        segment type consistent (16-bit vs wide). Type changes between
        0x00 and 0x40 are allowed; a first (0x80) segment stays 16-bit. */
    private boolean setRadius(CCLineSegment seg, long lRaw) {
        int nType = seg.getType();
        if ((nType & 0x80) != 0) {
            if (lRaw > Short.MAX_VALUE || lRaw < Short.MIN_VALUE)
                return false;
            seg.setParam(2, (int) lRaw);
            return true;
        }
        if (lRaw > Short.MAX_VALUE || lRaw < Short.MIN_VALUE) {
            seg.setType(0x40);
            seg.setParam(1, (int) (lRaw >> 16));
            seg.setParam(2, (int) (short) (lRaw & 0xFFFF));
        }
        else {
            seg.setType(0x00);
            seg.setParam(1, (int) lRaw);
        }
        return true;
    }

    private CCLine copyLine(CCLine line) {
        CCLine copy = new CCLine();
        for (int i = 1; i <= line.size(); i++)
            copy.add(copySegment(line.getAt(i)));
        return copy;
    }

    private CCLineSegment copySegment(CCLineSegment seg) {
        CCLineSegment copy = new CCLineSegment(seg.getType());
        copy.setTlu(seg.getTlu());
        for (int p = 0; p < 4; p++)
            copy.setParam(p, seg.getParam(p));
        return copy;
    }
}
