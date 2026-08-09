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
import cfevolution.data.track.Command;
import cfevolution.data.track.Track;
import cfevolution.data.track.TrackSegment;
import cfevolution.data.track.TrackSegments;

/**
    Two constraints on where the finished best line may run.

    KEEPING INSIDE THE ROAD. The optimiser works on a lateral profile held
    inside a corridor, but the quantiser only approximates that profile
    with the arcs the file format allows, and nothing used to check the
    finished line back against the corridor. Measured on a track built
    from the minimal canvas: the profile was held to 0.85x the road
    half-width, and the quantised line came out at exactly 1.00x on the
    approach to the first corner — on the road edge, where the game stops
    drawing the line, and followed by a 1035-unit (4.9 m) snap across the
    road in a single TLU. Requantising against the walked result pulls the
    profile back where it breaches and tries again.

    KEEPING CLEAR OF THE PIT EXIT. A car rejoining from the pits merges
    into the racing line, and if the line sits on the pit side there the
    lead car lifts for it. Measured on the same track: the line held
    0.35-0.42x toward the pit side straight through the merge. The profile
    is pushed off the pit side for a window after the exit connect, tapered
    at both ends so no step is introduced.
*/
public class CCLineCorridor {

    /** Requantisation passes allowed before the best result so far is
        taken. Breaches shrink quickly; more passes rarely help. */
    private static final int MAX_REFIT_PASSES = 6;

    /** How far into the road a breach is pulled, as a fraction of the
        excess. Over-correcting slightly is what stops the next pass
        breaching in the same place. */
    private static final double REFIT_OVERSHOOT = 1.35;

    /** Segs either side of a breach that are eased along with it, so the
        correction does not itself become a kink. */
    private static final int REFIT_BLEND_TLU = 6;

    /** TLU after the pit exit connect kept clear of the pit side. */
    private static final int PIT_EXIT_CLEAR_TLU = 30;

    /** TLU over which the pit clearance is faded in and out. */
    private static final int PIT_EXIT_TAPER_TLU = 15;

    private CCLineCorridor() {
    }

    /** Quantises the profile, then keeps requantising while the walked
        line strays outside the corridor the profile was held to. Returns
        the best line found — the one with the smallest worst breach. */
    public static CCLine quantizeWithinCorridor(CCLineTrackGeometry geo,
                                                CCLineLateralProfile profile,
                                                int nSeamOvershoot,
                                                double dEdgeStandoff) {
        double[] adBound = new double[geo.segCount];
        for (int i = 0; i < geo.segCount; i++)
            adBound[i] = geo.physicalBound[i] * (1.0 - Math.max(0.05, dEdgeStandoff));

        CCLine best = null;
        double dBestExcess = Double.MAX_VALUE;
        double[] adWork = new double[profile.offset.length];
        System.arraycopy(profile.offset, 0, adWork, 0, adWork.length);

        for (int nPass = 0; nPass < MAX_REFIT_PASSES; nPass++) {
            System.arraycopy(adWork, 0, profile.offset, 0, adWork.length);
            CCLine candidate = new CCLineQuantizer(geo, profile, nSeamOvershoot).quantize();
            CCLineSimulator.Result r = new CCLineSimulator(geo).run(candidate);

            double dWorst = 0;
            for (int i = 0; i < geo.segCount; i++) {
                if (!r.covered[i] || adBound[i] <= 0)
                    continue;
                double dExcess = Math.abs(r.ccLine[i]) - adBound[i];
                if (dExcess > dWorst)
                    dWorst = dExcess;
            }
            if (dWorst < dBestExcess) {
                dBestExcess = dWorst;
                best = candidate;
            }
            if (dWorst <= 0)
                break;

            // Pull the profile in wherever the walked line broke out, and
            // ease the correction into its neighbours.
            for (int i = 0; i < geo.segCount; i++) {
                if (!r.covered[i] || adBound[i] <= 0)
                    continue;
                double dExcess = Math.abs(r.ccLine[i]) - adBound[i];
                if (dExcess <= 0)
                    continue;
                double dPull = REFIT_OVERSHOOT * dExcess * (r.ccLine[i] < 0 ? 1 : -1);
                for (int k = -REFIT_BLEND_TLU; k <= REFIT_BLEND_TLU; k++) {
                    int j = ((i + k) % geo.segCount + geo.segCount) % geo.segCount;
                    double dWeight = 0.5 * (1.0 + Math.cos(Math.PI * k / (REFIT_BLEND_TLU + 1)));
                    adWork[j] += dPull * dWeight;
                    if (adWork[j] > adBound[j]) adWork[j] = adBound[j];
                    else if (adWork[j] < -adBound[j]) adWork[j] = -adBound[j];
                }
            }
        }
        return best;
    }

    /** Pushes the profile off the pit side for a window after the pit exit
        connect, so a car rejoining does not merge into the racing line.
        Does nothing if the track has no pit exit. */
    public static void keepClearOfPitExit(Track track, CCLineTrackGeometry geo,
                                          CCLineLateralProfile profile) {
        int nExit = findConnectTlu(track, 0x87);
        if (nExit < 0 || geo.segCount <= 0)
            return;
        // wCCLine is positive towards the right, and getPitSide() is true
        // when the pits are on the left.
        int nPitSign = track.getTrackDataHeader().getPitSide() ? -1 : 1;

        int nSpan = PIT_EXIT_CLEAR_TLU + 2 * PIT_EXIT_TAPER_TLU;
        for (int k = -PIT_EXIT_TAPER_TLU; k < nSpan - PIT_EXIT_TAPER_TLU; k++) {
            int i = ((nExit + k) % geo.segCount + geo.segCount) % geo.segCount;
            double dStrength = 1.0;
            if (k < 0)
                dStrength = 1.0 + (double) k / PIT_EXIT_TAPER_TLU;
            else if (k > PIT_EXIT_CLEAR_TLU)
                dStrength = 1.0 - (double) (k - PIT_EXIT_CLEAR_TLU) / PIT_EXIT_TAPER_TLU;
            if (dStrength <= 0)
                continue;
            // Full strength means "not on the pit side at all"; the taper
            // lets it return to the pit side gradually.
            double dCeiling = (1.0 - dStrength) * geo.physicalBound[i];
            double dOnPitSide = profile.offset[i] * nPitSign;
            if (dOnPitSide > dCeiling)
                profile.offset[i] = nPitSign * dCeiling;
        }
    }

    /** TLU of the first command of the given type, or -1. */
    private static int findConnectTlu(Track track, int nType) {
        TrackSegments segs = track.getTrackSegments();
        int nCum = 0;
        for (int i = 1; i <= segs.size() - 1; i++) {
            TrackSegment seg = segs.getAt(i);
            for (int c = 0; c < seg.getCommands().size(); c++) {
                Command cmd = (Command) seg.getCommands().get(c);
                if (cmd.getType() == nType)
                    return nCum + cmd.getParam(0);
            }
            nCum += seg.getTlu();
        }
        return -1;
    }
}
