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

package cfevolution.generator.pitlane;

import java.util.Enumeration;

import cfevolution.data.track.Command;
import cfevolution.data.track.TrackSegment;
import cfevolution.data.track.TrackSegments;

/**
    Fits the donor pit lane to a generated layout.

    The originals obey a tight invariant: the pit lane's total TLU equals
    the track distance from the 0x86 entry connect to the 0x87 exit
    connect within -13..+1 TLU (measured across all donors, 2026-07-19).
    Random tracks previously placed the connects wherever the donor's
    offsets landed, leaving mismatches up to ~90 TLU — the game then
    computes garbage pit geometry (the map's pit "spike") and crashes
    when a session starts from the pit lane.

    This fitter picks entry/exit offsets on the approach and S/F
    straights so the distance matches the donor pit's length, and when
    the straights cannot span it, adjusts the pit lane's plain straight
    sections (never the 55-TLU garage block, recognised by its 0x88/0x89
    parking-zone command).
*/
public class PitLaneFitter {

    /** The originals' pit lanes run up to this much shorter than the
        connect distance (never longer than +1). Aim mid-range. */
    private static final int TARGET_SLACK = 7;

    /** Placement result: connect offsets and any pit length change. */
    public static class Plan {
        /** Offset of the 0x86 entry connect into the approach straight. */
        public int entryOffset;
        /** Offset of the 0x87 exit connect into the S/F straight. */
        public int exitOffset;
        /** TLU to add to (negative: remove from) the pit lane straights. */
        public int pitDelta;
    }

    /** Computes connect placement for a generated layout. nSfLen is the
        S/F straight's length (first segment), nApproachLen the approach
        straight's (last segment), nPitTlu the donor pit lane's total. */
    public static Plan plan(int nSfLen, int nApproachLen, int nPitTlu) {
        Plan plan = new Plan();
        int nWantD = nPitTlu + TARGET_SLACK;

        // Feasible distance range with entry on the approach straight and
        // exit on the S/F straight (margins keep the connects off the
        // segment ends)
        int nMinD = 8;                                // entry late, exit early
        int nMaxD = (nApproachLen - 4) + (nSfLen - 8);
        int nD = Math.max(nMinD, Math.min(nMaxD, nWantD));
        plan.pitDelta = (nD - TARGET_SLACK) - nPitTlu; // 0 when nWantD fit

        // Prefer a donor-like exit a third of the way down the S/F
        // straight, then solve the entry position
        plan.exitOffset = Math.min(nSfLen / 3, Math.max(4, nD - nApproachLen + 4));
        plan.entryOffset = nApproachLen - (nD - plan.exitOffset);
        if (plan.entryOffset < 4) {
            plan.exitOffset += 4 - plan.entryOffset;
            plan.entryOffset = 4;
        }
        if (plan.exitOffset > nSfLen - 8) {
            plan.entryOffset += plan.exitOffset - (nSfLen - 8);
            plan.exitOffset = nSfLen - 8;
        }
        return plan;
    }

    /** Applies the plan's pit length change by adjusting plain straight
        sections (curvature 0, length >= 3, not the garage block). The
        delta is spread across eligible sections respecting 2..191. */
    public static void adjustPitLength(TrackSegments pitSegments, int nDelta) {
        if (nDelta == 0)
            return;
        java.util.Vector eligible = new java.util.Vector();
        for (Enumeration e = pitSegments.elements(); e.hasMoreElements(); ) {
            TrackSegment ts = (TrackSegment) e.nextElement();
            if (ts.getCurvature() != 0 || ts.getTlu() < 3)
                continue;
            if (hasCommand(ts, 0x88) || hasCommand(ts, 0x89))
                continue; // garage block stays 55 TLU (18 boxes)
            eligible.add(ts);
        }
        if (eligible.isEmpty())
            return;
        int nRemaining = nDelta;
        int nGuard = 0;
        while (nRemaining != 0 && nGuard++ < 1000) {
            boolean fChanged = false;
            for (int i = 0; i < eligible.size() && nRemaining != 0; i++) {
                TrackSegment ts = (TrackSegment) eligible.get(i);
                if (nRemaining > 0 && ts.getTlu() < 191) {
                    ts.setTlu(ts.getTlu() + 1);
                    nRemaining--;
                    fChanged = true;
                }
                else if (nRemaining < 0 && ts.getTlu() > 2) {
                    ts.setTlu(ts.getTlu() - 1);
                    nRemaining++;
                    fChanged = true;
                }
            }
            if (!fChanged)
                break; // capacity exhausted; leave the rest
        }
    }

    /** Peak heading of the entry/exit S-bends (game angle units, ~22 deg).
        Sets how far the garage straight sits from the track. */
    private static final int S_BEND_PEAK = 0x1000;

    /** Rewrites the pit curvatures into a net-zero shape for connects
        that both sit on one straight: entry S-bend out, straight garage,
        exit S-bend back in. The donors' pit curves mirror their own
        track's final corner (the pit departs BEFORE it and cuts inside)
        — on a generated layout with both connects on the S/F straight
        those curves march the pit off into the void until the 19-bit
        position space wraps (the "pit spike", and the pit-start crash).
        nSide +1/-1 selects which side of the track the pit bulges to.
        All sections, commands, flags and the garage block are kept. */
    public static void neutralizeCurvature(TrackSegments pitSegments, int nSide) {
        // Real sections in order (skip the trailing dummy, TLU 0)
        java.util.Vector sections = new java.util.Vector();
        int nGarage = -1;
        for (Enumeration e = pitSegments.elements(); e.hasMoreElements(); ) {
            TrackSegment ts = (TrackSegment) e.nextElement();
            if (ts.getTlu() <= 0)
                continue;
            if (hasCommand(ts, 0x88) || hasCommand(ts, 0x89))
                nGarage = sections.size();
            sections.add(ts);
        }
        int nCount = sections.size();
        if (nCount < 4)
            return;
        if (nGarage < 0)
            nGarage = nCount / 2;

        for (int i = 0; i < nCount; i++)
            ((TrackSegment) sections.get(i)).setCurvature(0);

        // Entry S: bend out on the first pre-garage section pair, bend
        // back straight on the pair before the garage. Exit mirrored.
        bendPair(sections, 0, Math.max(1, nGarage - 2), nSide);
        bendPair(sections, Math.min(nGarage + 1, nCount - 2), nCount - 1, -nSide);
    }

    /** Applies +peak on section a and a TLU-balanced opposite bend on
        section b so the heading returns to the straight's. */
    private static void bendPair(java.util.Vector sections, int a, int b, int nSide) {
        TrackSegment tsA = (TrackSegment) sections.get(a);
        TrackSegment tsB = (TrackSegment) sections.get(b);
        int nCurvA = nSide * S_BEND_PEAK / Math.max(1, tsA.getTlu());
        tsA.setCurvature(clampCurv(nCurvA));
        long lTotalA = (long) clampCurv(nCurvA) * tsA.getTlu();
        int nCurvB = (int) (-lTotalA / Math.max(1, tsB.getTlu()));
        tsB.setCurvature(clampCurv(nCurvB));
    }

    private static int clampCurv(int nCurv) {
        if (nCurv > 0x2000) return 0x2000;
        if (nCurv < -0x2000) return -0x2000;
        return nCurv;
    }

    /** Total TLU of the pit lane sections. */
    public static int pitTlu(TrackSegments pitSegments) {
        int nTotal = 0;
        for (Enumeration e = pitSegments.elements(); e.hasMoreElements(); )
            nTotal += ((TrackSegment) e.nextElement()).getTlu();
        return nTotal;
    }

    private static boolean hasCommand(TrackSegment ts, int nType) {
        for (Enumeration c = ts.getCommands().elements(); c.hasMoreElements(); )
            if (((Command) c.nextElement()).getType() == nType)
                return true;
        return false;
    }
}
