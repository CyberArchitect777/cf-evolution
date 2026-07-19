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
    Completes a partial best line (2026-07-19, user-requested workflow):
    cut the line at a point, hand-edit a sector, then have the remainder
    of the lap auto-generated. The kept sectors are untouched; the tail
    is quantised from the existing line's end state, targeting the chosen
    generation method's profile, and blends back onto the existing line's
    own stamps at the lap wrap so the seam is continuous.
*/
public class CCLineCompletion {

    /** Segs at the lap start whose target is taken from the existing
        line (wrap rejoin zone; the tail's seam overshoot re-stamps
        these, so it must aim at what the kept sectors do there). */
    private static final int WRAP_BLEND = 16;

    /** Builds existing + generated tail. methodSim is the simulation of
        the chosen method's full-lap line (its stamps are the tail's
        target). Returns null when the existing line already covers the
        lap (nothing to complete) or is empty (use plain replace). */
    public static CCLine complete(CCLineTrackGeometry geo, CCLine existing,
                                  CCLineSimulator.Result methodSim, int nOvershoot) {
        if (existing.size() == 0)
            return null;
        CCLineSimulator simulator = new CCLineSimulator(geo);
        CCLineSimulator.Result exSim = simulator.run(existing);
        int nExistingTlu = exSim.walkedTlu;
        int nTargetTlu = geo.segCount + nOvershoot;
        if (nExistingTlu >= nTargetTlu)
            return null;

        // Tail target: the method's line, except the wrap rejoin zone
        // where the kept sectors' own stamps rule
        CCLineLateralProfile profile = new CCLineLateralProfile(geo.segCount);
        for (int i = 0; i < geo.segCount; i++)
            profile.offset[i] = methodSim.ccLine[i];
        for (int i = 0; i < WRAP_BLEND && i < geo.segCount; i++)
            if (exSim.covered[i])
                profile.offset[i] = exSim.ccLine[i];

        // Walk the kept sectors to the tail's start state
        CCLineSimulator.State st = simulator.initialState();
        CCLineSimulator.Result scratch = new CCLineSimulator.Result(geo.segCount);
        for (int i = 1; i <= existing.size(); i++)
            simulator.runSegment(existing.getAt(i), st, scratch);

        CCLine tail = new CCLine();
        new CCLineQuantizer(geo, profile, 0)
            .quantizeFrom(st, nTargetTlu - nExistingTlu, false, tail, null);

        CCLine combined = new CCLine();
        for (int i = 1; i <= existing.size(); i++)
            combined.add(copySegment(existing.getAt(i)));
        for (int i = 1; i <= tail.size(); i++)
            combined.add(tail.getAt(i));
        return combined;
    }

    private static CCLineSegment copySegment(CCLineSegment seg) {
        CCLineSegment copy = new CCLineSegment(seg.getType());
        copy.setTlu(seg.getTlu());
        for (int p = 0; p < 4; p++)
            copy.setParam(p, seg.getParam(p));
        return copy;
    }
}
