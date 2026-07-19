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

import cfevolution.generator.ccline.*;

/**
    Approach 4: fastest-lap simulation (2026-07-19, project owner's
    design after three in-game rounds showed rule-based construction
    trading one artefact for another).

    Seeds from the constructed racing line, then iteratively searches for
    the fastest simulated lap (LapTimeOptimizer): smooth lateral moves,
    candidates leaving the corridor — the physical road minus
    context.edgeStandoff — are discarded outright, faster laps are kept.
    The winning profile is quantised and polished like every other
    method. On the 16 originals this generates lines with hand-tuned
    amplitude character and near-hand followability (16/16 gate,
    DEVELOPMENT.md Session 17).
*/
public class FastestLapCCLineGenerator implements CCLineGenerator {

    public String getName() {
        return "Fastest Lap Simulation";
    }

    public CCLineGenerationResult generate(CCLineGeneratorContext context,
                                           CCLineProgressListener listener) throws Exception {
        CCLineTrackGeometry geo = context.geometry;

        if (listener != null)
            listener.progress(5, "Constructing seed racing line...");
        CCLineLateralProfile profile =
            RacingLineProfileBuilder.build(geo, context.edgeStandoff);

        if (listener != null) {
            if (listener.isCancelled())
                return null;
            listener.progress(20, "Simulating for the fastest lap...");
        }
        LapTimeOptimizer optimizer = new LapTimeOptimizer(geo);
        double[] adTimes = optimizer.optimize(profile.offset, context.edgeStandoff,
            Math.max(context.iterations * 20, 40000), new java.util.Random(1234));

        return MinCurvatureCCLineGenerator.quantizeAndPolish(
            context, listener, profile, adTimes);
    }
}
