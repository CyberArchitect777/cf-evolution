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
    Windowed requantization repair for the stateful best line.

    Because every CCLine sector starts from the position/heading the
    previous one ended with, changing one sector shifts the entire rest
    of the lap — the cascade that makes both manual editing and local
    kick→arc conversion painful (no kick+arc replacement can preserve
    both end position and end heading of a kick-straight, so any local
    structural change de-anchors everything downstream).

    This class evades the cascade: it replaces one sector, then
    re-quantizes only a bounded downstream window against the line's own
    previous stamped offsets, so the walk reconnects with the untouched
    remainder of the lap at a sector boundary. The window absorbs any
    TLU difference of the replacement, keeping every later sector at its
    original lap position.
*/
public class CCLineWindowRepair {

    /** Default downstream window (TLU) re-fitted after a change. */
    public static final int DEFAULT_WINDOW_TLU = 64;

    /** Replaces sector nSector (1-based) of line with the given segments
        and re-quantizes a following window of at least nWindowTlu against
        targetOffsets (per-Seg lateral offsets; null = the line's own
        current stamps). Returns the repaired line, or null when the
        geometry does not allow it (window would be empty, or the
        replacement runs past the line's end). The result's total TLU
        always equals the original line's. */
    public static CCLine replaceAndRepair(CCLineTrackGeometry geo, CCLine line,
                                          int nSector, CCLineSegment[] replacement,
                                          int nWindowTlu, double[] targetOffsets) {
        int nCount = line.size();
        if (nSector < 1 || nSector > nCount || replacement == null || replacement.length == 0)
            return null;

        CCLineSimulator simulator = new CCLineSimulator(geo);

        // Target profile: what the line does now (before the change)
        if (targetOffsets == null) {
            CCLineSimulator.Result r = simulator.run(line);
            targetOffsets = new double[r.ccLine.length];
            for (int i = 0; i < r.ccLine.length; i++)
                targetOffsets[i] = r.ccLine[i];
        }

        // Cumulative TLU after each sector (index 0 = before sector 1)
        int[] anCum = new int[nCount + 1];
        for (int i = 1; i <= nCount; i++)
            anCum[i] = anCum[i - 1] + line.getAt(i).getTlu();
        int nTotalTlu = anCum[nCount];

        int nReplTlu = 0;
        for (int i = 0; i < replacement.length; i++)
            nReplTlu += replacement[i].getTlu();

        int nWindowStart = anCum[nSector - 1] + nReplTlu;
        if (nWindowStart >= nTotalTlu)
            return null;

        CCLineSimulator.Result scratch = new CCLineSimulator.Result(geo.segCount);
        CCLineLateralProfile profile = new CCLineLateralProfile(geo.segCount);
        System.arraycopy(targetOffsets, 0, profile.offset, 0, geo.segCount);
        CCLineQuantizer quantizer = new CCLineQuantizer(geo, profile, 0);

        // Walk state at the window start (untouched prefix + replacement)
        CCLineSimulator.State stStart = simulator.initialState();
        for (int i = 1; i < nSector; i++)
            simulator.runSegment(line.getAt(i), stStart, scratch);
        for (int i = 0; i < replacement.length; i++)
            simulator.runSegment(replacement[i], stStart, scratch);

        // Try growing windows until the seam closes tightly — a heavily
        // disturbed entry state needs more runway to re-converge onto the
        // old line. Keep the attempt with the best closure.
        CCLine bestWindow = null;
        int nBestSplice = -1;
        double dBestMismatch = Double.MAX_VALUE;
        int nSplice = nSector;
        for (int nAttempt = 0; nAttempt < 3 && nSplice < nCount + 1; nAttempt++) {
            // Splice sector: first boundary giving at least the requested
            // window (after absorbing the replacement's TLU delta);
            // reaching the lap end re-fits the tail instead.
            int nWant = nWindowTlu << nAttempt;
            nSplice = nSector;
            while (nSplice < nCount && anCum[nSplice] - nWindowStart < nWant)
                nSplice++;
            int nWindow = anCum[nSplice] - nWindowStart;
            if (nWindow <= 0)
                break;

            // The walk state the suffix originally started from — the
            // window must end there or the suffix runs shifted (borderline
            // arcs then clamp; the cascade this class exists to stop)
            CCLineSimulator.State seam = null;
            if (nSplice < nCount) {
                seam = simulator.initialState();
                for (int i = 1; i <= nSplice; i++)
                    simulator.runSegment(line.getAt(i), seam, scratch);
            }

            CCLine window = new CCLine();
            CCLineSimulator.State st = stStart.copy();
            quantizer.quantizeFrom(st, nWindow, false, window, seam);

            double dMismatch = 0.0;
            if (seam != null)
                dMismatch = Math.abs(st.wSegPosX - seam.wSegPosX)
                          + 2.0 * Math.abs((short) (st.wTmpAngleZ - seam.wTmpAngleZ));
            if (bestWindow == null || dMismatch < dBestMismatch) {
                bestWindow = window;
                nBestSplice = nSplice;
                dBestMismatch = dMismatch;
            }
            if (seam == null || dMismatch <= SEAM_CLOSED)
                break;
        }
        if (bestWindow == null)
            return null;

        // Assemble: prefix + replacement + window + untouched suffix
        CCLine repaired = new CCLine();
        for (int i = 1; i < nSector; i++)
            repaired.add(copySegment(line.getAt(i)));
        for (int i = 0; i < replacement.length; i++)
            repaired.add(replacement[i]);
        for (int i = 1; i <= bestWindow.size(); i++)
            repaired.add(bestWindow.getAt(i));
        for (int i = nBestSplice + 1; i <= nCount; i++)
            repaired.add(copySegment(line.getAt(i)));
        return repaired;
    }

    /** Seam mismatch (|Δpos| + 2·|Δheading|) considered fully closed. */
    private static final double SEAM_CLOSED = 300.0;

    private static CCLineSegment copySegment(CCLineSegment seg) {
        CCLineSegment copy = new CCLineSegment(seg.getType());
        copy.setTlu(seg.getTlu());
        for (int p = 0; p < 4; p++)
            copy.setParam(p, seg.getParam(p));
        return copy;
    }
}
