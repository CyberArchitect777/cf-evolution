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
    Approach 1: geometric construction.

    Builds a constructed out-in-out racing line (RacingLineProfileBuilder)
    within the physical road at a configurable edge standoff
    (context.edgeStandoff) and quantises it into CCLine segments. Kept as
    its own method (rule-based, no objective) so it can be compared
    in-game against the fastest-lap simulation (option 4), which uses
    this construction as its seed.
*/
public class MinCurvatureCCLineGenerator implements CCLineGenerator {

    public String getName() {
        return "Geometric (Constructed Racing Line)";
    }

    public CCLineGenerationResult generate(CCLineGeneratorContext context,
                                           CCLineProgressListener listener) throws Exception {
        if (listener != null)
            listener.progress(5, "Constructing racing line...");

        // Constructed out-in-out profile within the PHYSICAL road at the
        // requested edge standoff (see RacingLineProfileBuilder — earlier
        // energy-relaxation profiles either hugged the middle or targeted
        // the game's 8x-inflated tolerance bound, i.e. off the real road)
        CCLineLateralProfile profile =
            RacingLineProfileBuilder.build(context.geometry, context.edgeStandoff);

        return quantizeAndPolish(context, listener, profile, null);
    }

    /** Shared tail of the geometric-family methods: profile -> quantize
        -> smoothness polish -> score. adLapTimes (nullable) is the
        {seed, optimised} lap-time pair reported by the fastest-lap
        method. */
    static CCLineGenerationResult quantizeAndPolish(CCLineGeneratorContext context,
                                                    CCLineProgressListener listener,
                                                    CCLineLateralProfile profile,
                                                    double[] adLapTimes) throws Exception {
        if (listener != null) {
            if (listener.isCancelled())
                return null;
            listener.progress(85, "Quantising into CCLine segments...");
        }

        // Keep the line out of the way of cars rejoining from the pits.
        CCLineCorridor.keepClearOfPitExit(context.track, context.geometry, profile);

        // NOT requantised against the corridor. That was tried (see
        // CCLineCorridor.quantizeWithinCorridor) to stop the line reaching
        // the road edge on corner approaches, and measured: it improved the
        // worst amplitude on three originals but made it worse on the track
        // the problem was reported from, and left the lateral snap that
        // actually upsets the AI untouched. The snap comes from where the
        // quantiser places its sector boundaries at a corner, not from the
        // corridor, so it needs fixing there.
        CCLine ccLine = new CCLineQuantizer(context.geometry, profile,
            context.seamOvershoot).quantize();

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
        if (adLapTimes != null)
            result.addWarning(String.format("Lap time model: %.2fs seed -> %.2fs optimised",
                new Object[] { new Double(adLapTimes[0]), new Double(adLapTimes[1]) }));
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
