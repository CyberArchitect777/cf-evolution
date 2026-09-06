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
import cfevolution.data.track.Seg;
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

    /** Fallback peak heading of the entry/exit S-bends (game angle units,
        ~22 deg), used only when no target offset is known.

        This was the FIXED peak until 2026-09-04, and using it alone is what
        put the garage straight in the wrong place: the counter-bend does not
        land until section nGarage-2, so the lane runs a 22.5 degree diagonal
        away from the road for that whole stretch and the offset it reaches
        scales with the TLU gap to the counter-bend rather than with the road.
        Measured across all 16 originals, that put the garage 5.29-19.74x the
        road half-width out against the donors' own 2.68-7.83x. */
    private static final int S_BEND_PEAK = 0x1000;

    /** A section's heading change per TLU is capped here by the game
        (max angle Z change per segment). */
    private static final int MAX_CURVATURE = 0x2000;

    /** World units per TLU. */
    private static final double UNITS_PER_TLU = 1024.0;

    private static final int FULL_TURN = 65536;

    /** World X/Y are 19-bit, so differences must be taken modulo this. */
    private static final int WRAP_UNITS = 1 << 19;

    /**
        How far the furthest pit Seg sits from the nearest point of the track
        centreline — i.e. where the garage straight is parked, in world units.

        Both callers of neutralizeCurvature use this on the DONOR's geometry,
        before the track is replaced or straightened, because that is the only
        moment the original pit-to-road relationship still exists. Feeding the
        answer back in as the target is what keeps a rebuilt pit lane sitting
        where that circuit's pit lane always sat.

        Differencing is modular: positions wrap every 512 TLU, so a plain
        subtraction across a wrap boundary reads as a huge distance.
    */
    public static double measurePitOffset(TrackSegments trackSegments, TrackSegments pitSegments) {
        if (trackSegments == null || pitSegments == null)
            return 0;

        int nTrack = trackSegments.getMaxTrackSegIndex() + 1;
        int nPit = pitSegments.getMaxTrackSegIndex() + 1;
        if (nTrack <= 0 || nPit <= 0)
            return 0;

        double dWorst = 0;
        for (int p = 0; p < nPit; p++) {
            Seg ps = pitSegments.getSegAt(p);
            if (ps == null)
                continue;
            double dBest = Double.MAX_VALUE;
            for (int s = 0; s < nTrack; s++) {
                Seg ts = trackSegments.getSegAt(s);
                if (ts == null)
                    continue;
                double dx = wrapDelta(ps.getPosX() - ts.getPosX());
                double dy = wrapDelta(ps.getPosY() - ts.getPosY());
                double d = Math.sqrt(dx * dx + dy * dy);
                if (d < dBest)
                    dBest = d;
            }
            if (dBest != Double.MAX_VALUE && dBest > dWorst)
                dWorst = dBest;
        }
        return dWorst;
    }

    /** Shortest signed difference in the 19-bit wrapping coordinate space. */
    private static double wrapDelta(int nDelta) {
        nDelta &= (WRAP_UNITS - 1);
        if (nDelta >= WRAP_UNITS / 2)
            nDelta -= WRAP_UNITS;
        return nDelta;
    }

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
        neutralizeCurvature(pitSegments, nSide, 0);
    }

    /**
        As above, but aims the garage straight at dTargetOffset world units
        from the track instead of letting the offset fall out of a fixed bend
        angle. Pass the donor's own measured pit offset: the peak heading is
        then solved for it, so the rebuilt lane sits where that track's pit
        lane always sat. A value <= 0 falls back to the old fixed peak.
    */
    public static void neutralizeCurvature(TrackSegments pitSegments, int nSide,
                                           double dTargetOffset) {
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
        bendPair(sections, 0, Math.max(1, nGarage - 2), nSide, dTargetOffset);
        bendPair(sections, Math.min(nGarage + 1, nCount - 2), nCount - 1, -nSide, dTargetOffset);
    }

    /** Applies +peak on section a and a TLU-balanced opposite bend on
        section b so the heading returns to the straight's.

        The lane leaves the road over section a, holds the peak heading
        across everything between (those sections were zeroed above), and
        comes back parallel over section b — so the lateral distance it
        travels is the peak's sine over half of a, all of the middle and
        half of b. Given a target for that distance the peak is just:

            sin(peak) = target / (spannedTlu * 1024)

        which is what keeps the offset tied to where the pit belongs rather
        than to how far apart these two sections happen to be. */
    private static void bendPair(java.util.Vector sections, int a, int b, int nSide,
                                 double dTargetOffset) {
        TrackSegment tsA = (TrackSegment) sections.get(a);
        TrackSegment tsB = (TrackSegment) sections.get(b);

        int nPeak = S_BEND_PEAK;
        if (dTargetOffset > 0) {
            double dSpanTlu = tsA.getTlu() / 2.0 + tsB.getTlu() / 2.0;
            for (int i = a + 1; i < b; i++)
                dSpanTlu += ((TrackSegment) sections.get(i)).getTlu();

            if (dSpanTlu > 0) {
                double dSin = dTargetOffset / (dSpanTlu * UNITS_PER_TLU);
                if (dSin > 1.0)
                    dSin = 1.0;   // target unreachable over this span; go as steep as possible
                nPeak = (int) Math.round(Math.asin(dSin) / (2 * Math.PI) * FULL_TURN);
            }
        }

        int nCurvA = nSide * nPeak / Math.max(1, tsA.getTlu());
        tsA.setCurvature(clampCurv(nCurvA));
        long lTotalA = (long) clampCurv(nCurvA) * tsA.getTlu();
        int nCurvB = (int) (-lTotalA / Math.max(1, tsB.getTlu()));
        tsB.setCurvature(clampCurv(nCurvB));
    }

    private static int clampCurv(int nCurv) {
        if (nCurv > MAX_CURVATURE) return MAX_CURVATURE;
        if (nCurv < -MAX_CURVATURE) return -MAX_CURVATURE;
        return nCurv;
    }

    /**
        Flattens the pit lane's elevation, for callers that have flattened the
        track it runs beside.

        The height field is a per-TLU PITCH RATE that the game double-integrates
        into elevation, so a pit lane keeping its donor's height field beside a
        road that no longer climbs ends up at a completely different altitude —
        measured on the 16 originals reduced to a straight canvas, up to 2,692
        units (12.8 m) below the road on F1CT02, 856 (4.1 m) on Monaco. Only the
        five donors whose pit height field is already all-zero (F1CT01, F1CT03,
        F1CT06, F1CT12, F1CT13) escaped it, which is why a reduced Monza looked
        right while nearly everything else did not.
    */
    public static void flattenHeights(TrackSegments pitSegments) {
        if (pitSegments == null)
            return;
        for (Enumeration e = pitSegments.elements(); e.hasMoreElements(); )
            ((TrackSegment) e.nextElement()).setHeightChange(0);
    }

    /**
        Gives the pit lane the same elevation profile as the road it runs
        beside, for callers whose track HAS hills — the generated-track
        counterpart to flattenHeights.

        The pit lane runs from the 0x86 entry connect, forward through the
        start/finish wrap, to the 0x87 exit connect. Each pit section is mapped
        onto the stretch of road it sits next to and takes that stretch's mean
        height field, so the two climb together. Without this a generated track
        gets its own hills while the pit keeps the donor's, and the lane ends up
        metres above or below the road.

        Returns the worst per-section height difference applied, or -1 if the
        connects could not be found.
    */
    public static int followTrackHeights(TrackSegments pitSegments, TrackSegments trackSegments) {
        if (pitSegments == null || trackSegments == null)
            return -1;

        int nTotal = trackSegments.getTotalTlu();
        int nEntry = findConnectTlu(trackSegments, 0x86);
        int nExit = findConnectTlu(trackSegments, 0x87);
        if (nTotal <= 0 || nEntry < 0 || nExit < 0)
            return -1;

        int nConnectDist = ((nExit - nEntry) + nTotal) % nTotal;
        int nPitTotal = pitTlu(pitSegments);
        if (nConnectDist <= 0 || nPitTotal <= 0)
            return -1;

        // Per-TLU height field of the road.
        int[] anHeight = new int[nTotal];
        int nAt = 0;
        for (Enumeration e = trackSegments.elements(); e.hasMoreElements(); ) {
            TrackSegment ts = (TrackSegment) e.nextElement();
            for (int k = 0; k < ts.getTlu() && nAt < nTotal; k++)
                anHeight[nAt++] = ts.getHeightChange();
        }

        // Cumulative road pitch at each pit TLU, mapped proportionally (the pit
        // runs a few TLU short of the connect distance by design).
        long[] alRoadCum = new long[nPitTotal + 1];
        for (int p = 0; p < nPitTotal; p++) {
            int nRoad = nEntry + (int) ((long) p * nConnectDist / nPitTotal);
            alRoadCum[p + 1] = alRoadCum[p] + anHeight[((nRoad % nTotal) + nTotal) % nTotal];
        }

        // Assign per-section heights with the rounding error CARRIED FORWARD.
        // Height is an integer per-TLU rate, so rounding each section on its own
        // loses the profile entirely on gentle gradients — the first attempt at
        // this flattened the pit lane to zero because every section's mean
        // rounded away.
        int nWorst = 0;
        int nPitAt = 0;
        long lApplied = 0;
        for (Enumeration e = pitSegments.elements(); e.hasMoreElements(); ) {
            TrackSegment ts = (TrackSegment) e.nextElement();
            if (ts.getTlu() <= 0)
                continue;

            int nEnd = Math.min(nPitAt + ts.getTlu(), nPitTotal);
            long lNeed = alRoadCum[nEnd] - lApplied;
            int nHeight = (int) Math.round((double) lNeed / ts.getTlu());

            nWorst = Math.max(nWorst, Math.abs(nHeight - ts.getHeightChange()));
            ts.setHeightChange(nHeight);
            lApplied += (long) nHeight * ts.getTlu();
            nPitAt += ts.getTlu();
        }
        return nWorst;
    }

    /** TLU of the first command of this type in a segment list. */
    private static int findConnectTlu(TrackSegments segments, int nType) {
        int nAt = 0;
        for (Enumeration e = segments.elements(); e.hasMoreElements(); ) {
            TrackSegment ts = (TrackSegment) e.nextElement();
            for (Enumeration c = ts.getCommands().elements(); c.hasMoreElements(); ) {
                Command cmd = (Command) c.nextElement();
                if (cmd.getType() == nType)
                    return nAt + cmd.getParam(0);
            }
            nAt += ts.getTlu();
        }
        return -1;
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
