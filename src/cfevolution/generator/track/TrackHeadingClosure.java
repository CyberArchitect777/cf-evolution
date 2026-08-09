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

package cfevolution.generator.track;

import cfevolution.data.track.Seg;
import cfevolution.data.track.Track;
import cfevolution.data.track.TrackSegment;
import cfevolution.data.track.TrackSegments;

/**
    Makes a track's last segment face the same way as its first, by adding
    one segment at the end.

    A track does not have to form a closed loop to work. It has to leave
    the car pointing the way it started, because the game always wraps
    from the last segment back to the first. Curvature is a per-TLU
    heading rate, so the heading at the end of the lap is the sum of
    length x curvature over every segment, and "same heading" means that
    sum is a whole number of turns (65536 angle units each).

    Zero turns is as valid as one. The reference case is Mike Saunders'
    straight Monza (2000), which works perfectly in game: 57 sections,
    every curvature zeroed, net winding exactly 0.

    What the game does with the position gap
    ----------------------------------------
    Nothing here tries to bring the end back to the start. The game does
    that itself: TCRecalcPosToFit distributes the start-to-end difference
    evenly over every Seg, sliding each one a little so the ends meet.
    The cost is that each piece of road is displaced by a fraction of its
    own length equal to gap / lap length. Measured references: the
    originals sit under 1 TLU of gap (~0%), and the straight Monza — a
    known-good track — carries 165 TLU over a 1190 TLU lap, about 14%.

    World X/Y are 19-bit signed (see Seg.setPos), so positions wrap every
    512 TLU. The gap is therefore always a wrapped figure and can never
    exceed about 360 TLU however open the track is.
*/
public class TrackHeadingClosure {

    /** Angle units in a full turn (65536 = 360 degrees). */
    public static final int FULL_TURN = 65536;

    /** Preferred curvature cap for a segment this class adds: the
        tightest corner in the originals (F1CT04's 3070). The engine
        allows 0x2000, but that is a 45-degree-per-TLU spiral. */
    private static final int PREFERRED_CURV = 3000;

    /** Longest section the file format holds. */
    private static final int MAX_SECTION_TLU = 191;

    /** Gap ratio the straight Monza carries and the game handles without
        trouble — the calibration point for the reports. */
    public static final double MONZA_GAP_RATIO = 0.14;

    /** Heading residual treated as already matching. The originals do not
        land on a whole turn exactly: measured across all 16 they are 2 to
        165 angle units out (F1CT04 is the worst at 0.91 degrees, F1CT06
        the best at 0.01), so anything inside this is as square as a
        hand-built original and adding a segment for it would be noise.
        Mike Saunders' straight Monza is the only track measured at
        exactly 0, because every curvature in it was zeroed by hand. */
    public static final int HEADING_TOLERANCE = 256;

    /** What adding the segment did, or would do. */
    public static class Result {
        public boolean ok;
        /** Why nothing was added (null when ok). */
        public String failure;
        /** True when the track already faced the right way. */
        public boolean alreadyMatching;

        public int netWindingBefore;
        public int netWindingAfter;
        /** Whole turns the closure aimed at (0, 1, -1 ...). */
        public int turnsTarget;
        /** Heading the added segment had to supply, in angle units. */
        public int headingNeeded;
        /** Heading still unmatched afterwards, in angle units. */
        public int headingResidual;

        public int addedTlu;
        public int addedCurvature;
        public int trackTluBefore;
        public int trackTluAfter;
    }

    private TrackHeadingClosure() {
    }

    /** Sum of length x curvature over every section: the heading the lap
        turns through, in angle units. */
    public static int netWinding(TrackSegments segs) {
        int nSum = 0;
        for (int i = 1; i <= lastRealSection(segs); i++) {
            TrackSegment seg = segs.getAt(i);
            nSum += seg.getTlu() * seg.getCurvature();
        }
        return nSum;
    }

    /** Sum of length x height: the pitch the lap ends on. Reported rather
        than corrected — a pitch mismatch tilts the road at the wrap, it
        does not break the wrap the way a heading mismatch does. */
    public static int netPitch(TrackSegments segs) {
        int nSum = 0;
        for (int i = 1; i <= lastRealSection(segs); i++) {
            TrackSegment seg = segs.getAt(i);
            nSum += seg.getTlu() * seg.getHeightChange();
        }
        return nSum;
    }

    /** Whole turns nearest to the heading the lap already turns through —
        the cheapest target, since any whole number of turns leaves the
        start and end facing the same way. */
    public static int nearestWholeTurn(int nWinding) {
        return (int) Math.round(nWinding / (double) FULL_TURN);
    }

    /** True if every section runs straight — no corner anywhere on the
        lap. A best line has nothing to work with on such a track: with no
        curvature there is no cornering speed to trade against, so every
        line through it is equally fast. */
    public static boolean allStraight(TrackSegments segs) {
        for (int i = 1; i <= lastRealSection(segs); i++) {
            if (segs.getAt(i).getCurvature() != 0)
                return false;
        }
        return true;
    }

    /** True if the last segment already faces the way the first does, to
        within the tolerance the originals themselves keep. */
    public static boolean headingMatches(TrackSegments segs) {
        int nWinding = netWinding(segs);
        return Math.abs(nearestWholeTurn(nWinding) * FULL_TURN - nWinding) <= HEADING_TOLERANCE;
    }

    /** Works out what would be added, without changing anything. */
    public static Result analyse(Track track) {
        return plan(track);
    }

    /** Adds one segment at the end of the track so the last segment faces
        the way the first does. Does nothing if it already does. */
    public static Result addClosingSegment(Track track) {
        Result r = plan(track);
        if (!r.ok || r.alreadyMatching)
            return r;

        TrackSegments segs = track.getTrackSegments();
        TrackSegment last = segs.getAt(lastRealSection(segs));
        // insertAt is 1-based and the vector's final element is the 0xFF
        // terminator, so inserting at size() lands just before it.
        TrackSegment added = segs.insertAt(segs.size());
        added.setTlu(r.addedTlu);
        added.setCurvature(r.addedCurvature);
        added.setHeightChange(0);
        added.setFlags(0);
        if (last != null) {
            added.setFenceDistL(last.getFenceDistL());
            added.setFenceDistR(last.getFenceDistR());
        }
        r.netWindingAfter = netWinding(segs);
        r.trackTluAfter = segs.getTotalTlu();
        return r;
    }

    private static Result plan(Track track) {
        Result r = new Result();
        TrackSegments segs = track.getTrackSegments();
        if (lastRealSection(segs) < 1) {
            r.failure = "The track has no segments.";
            return r;
        }
        r.netWindingBefore = netWinding(segs);
        r.netWindingAfter = r.netWindingBefore;
        r.trackTluBefore = segs.getTotalTlu();
        r.trackTluAfter = r.trackTluBefore;
        r.turnsTarget = nearestWholeTurn(r.netWindingBefore);
        r.headingNeeded = r.turnsTarget * FULL_TURN - r.netWindingBefore;

        if (Math.abs(r.headingNeeded) <= HEADING_TOLERANCE) {
            r.ok = true;
            r.alreadyMatching = true;
            return r;
        }

        // One segment, so length x curvature must land on the requirement
        // exactly. Prefer the shortest length that divides it at a
        // real-corner curvature; if nothing divides it, take the closest
        // curvature and report what is left over.
        int nMinTlu = Math.max(1, ceilDiv(Math.abs(r.headingNeeded), PREFERRED_CURV));
        int nTlu = 0;
        for (int i = nMinTlu; i <= MAX_SECTION_TLU; i++) {
            if (r.headingNeeded % i == 0) {
                nTlu = i;
                break;
            }
        }
        if (nTlu > 0) {
            r.addedTlu = nTlu;
            r.addedCurvature = r.headingNeeded / nTlu;
            r.headingResidual = 0;
        } else {
            r.addedTlu = nMinTlu;
            r.addedCurvature = (int) Math.round(r.headingNeeded / (double) nMinTlu);
            r.headingResidual = r.headingNeeded - r.addedTlu * r.addedCurvature;
        }
        r.ok = true;
        return r;
    }

    /** Straight-line distance from the first Seg to the last, in TLU: what
        the game's own fit has to absorb. Measured with the fit switched
        off, then the track is left compiled as it was found. */
    public static double gapTlu(Track track) {
        boolean fWasLayoutMode = track.getLayoutMode();
        track.setLayoutMode(true);
        track.calculateTrackLayout();
        TrackSegments segs = track.getTrackSegments();
        double dGap = 0;
        int nMax = segs.getMaxTrackSegIndex();
        if (nMax > 0) {
            Seg first = segs.getSegAt(0), last = segs.getSegAt(nMax);
            dGap = Math.hypot(last.getPosX() - first.getPosX(),
                              last.getPosY() - first.getPosY()) / 1024.0;
        }
        track.setLayoutMode(fWasLayoutMode);
        track.calculateTrackLayout();
        return dGap;
    }

    /** The gap as a fraction of the lap: how far the game's fit displaces
        each piece of road, relative to its own length. Compare against
        MONZA_GAP_RATIO. */
    public static double gapRatio(Track track) {
        int nTlu = track.getTrackTlu();
        return nTlu > 0 ? gapTlu(track) / nTlu : 0;
    }

    /** A short verdict on a gap ratio, phrased against the straight Monza
        reference rather than any absolute rule — no absolute rule is
        known, and the originals themselves sit at nearly zero. */
    public static String describeGap(double dRatio) {
        if (dRatio <= 0.02)
            return "closed (like the original tracks)";
        if (dRatio <= MONZA_GAP_RATIO * 1.2)
            return "at or under the straight Monza reference, which works in game";
        if (dRatio <= 0.5)
            return "above the straight Monza reference — worth checking in game";
        return "well above the straight Monza reference — expect visible distortion";
    }

    /** The vector's last element is the 0xFF terminator, which carries no
        length but may carry commands. */
    private static int lastRealSection(TrackSegments segs) {
        return segs.size() - 1;
    }

    private static int ceilDiv(int nValue, int nDivisor) {
        return (nValue + nDivisor - 1) / nDivisor;
    }
}
