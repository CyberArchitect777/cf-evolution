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

import cfevolution.data.track.TrackSegment;
import cfevolution.data.track.Seg;
import cfevolution.data.track.Track;
import cfevolution.data.track.TrackDataHeader;
import cfevolution.data.track.TrackSegments;

/**
    Moves a track that runs over the edge of the game's coordinate space
    back into the middle of it.

    World X/Y are 19-bit and wrap every 512 TLU, so a track can run off one
    side of the space and carry on from the other. The road itself is fine
    with that, but the best line is not: the game builds the line by
    subtracting each Seg's position from the line's own as plain 32-bit
    values (ProcessCCLineSegment, IDA 0x90B40), with no allowance for the
    wrap, so every Seg past the edge gets a line value that is far out.
    CF Evolution copies that arithmetic exactly, so it shows the same wrong
    line the game would build.

    None of the 16 originals crosses the edge. Custom tracks can: all 12 of
    a tester's GP2-derived tracks that drew "broken" crossed it (2026-10-19).

    The cure is to move the whole track by changing its start point in the
    header. Nothing else in the file is positional (objects, the pit lane,
    cameras and the best line are all placed relative to the Segs), and a
    track that stays clear of the edge gets an identical best line wherever
    it sits — measured on Monaco and Silverstone at several start points.
    The start point is in 1/8 world units, so a move keeps the fine position
    bits and the line comes out bit-identical.
*/
public class TrackCoordinateEdge {

    /** Clearance kept between the track and the edge, in world units (2 TLU). */
    private static final int MARGIN = 2048;

    public static class Result {
        public boolean moved;      // the start point was changed
        public boolean open;       // too long to clear the edge: crossings put on straights instead
        public String failure;     // why not, or null; plain language
    }

    /** True when the track crosses the edge and moving it would help: it can
        be moved clear of the edge, or (an open track, longer than the
        coordinate space in some direction) it crosses the edge at or near a
        corner and could cross it on a straight instead. Needs the layout
        calculated.

        OPEN TRACKS (2026-10-25). A track longer than the space, such as the
        minimal-track canvas or the owner's Straight Kink Check track, cannot
        help crossing. Until this date they were left alone, but the turn one
        snap on the Kink Check track (1,035 units, the line blinking out in the
        game) IS its crossing, which sits inside turn one at Seg 405. With the
        start slid so the crossing lands on the straight and the line generated
        again, the worst step on the whole lap is 378 and turn one's 323, both
        under the AI's 640 dead-band; at the original placement a regenerated
        line steps 2,218 there. A best line belongs to its placement: moving an
        open track needs a new line. */
    public static boolean shouldMove(Track track) {
        if (!track.crossesCoordinateEdge())
            return false;
        if (fits(span(track)))
            return true;
        return isOpenTrackCrossingAtCorner(track);
    }

    /** True for an open track whose crossings could be moved further from
        corners than they are, and are now closer than CORNER_CLEARANCE. */
    public static boolean isOpenTrackCrossingAtCorner(Track track) {
        if (!track.crossesCoordinateEdge() || fits(span(track)))
            return false;
        int[] avoid = avoidDistance(track);
        int now = Math.min(crossingScore(track, avoid, 0), crossingScore(track, avoid, 1));
        return now < CORNER_CLEARANCE;
    }

    /** How far, in Segs, an open track's edge crossings should be from any
        corner and from the start/finish line. */
    private static final int CORNER_CLEARANCE = 24;

    /** True when the track is longer than the coordinate space in some
        direction, so it cannot be moved clear of the edge. */
    public static boolean isOpen(Track track) {
        return !fits(span(track));
    }

    /** {width, height} of the track and pit lane, unwrapped. */
    private static double[] span(Track track) {
        double[] b = bounds(track);
        return new double[] { b[1] - b[0], b[3] - b[2] };
    }

    private static boolean fits(double[] adSpan) {
        int nRoom = TrackSegments.COORD_WRAP - 2 * MARGIN;
        return adSpan[0] < nRoom && adSpan[1] < nRoom;
    }

    /** {minX, maxX, minY, maxY} over the display positions of track and pit. */
    private static double[] bounds(Track track) {
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        TrackSegments[] lists = { track.getTrackSegments(), track.getPitlaneSegments() };
        for (TrackSegments segs : lists) {
            for (int i = 0; i <= segs.getMaxTrackSegIndex(); i++) {
                Seg s = segs.getSegAt(i);
                minX = Math.min(minX, s.getDisplayX());
                maxX = Math.max(maxX, s.getDisplayX());
                minY = Math.min(minY, s.getDisplayY());
                maxY = Math.max(maxY, s.getDisplayY());
            }
        }
        return new double[] { minX, maxX, minY, maxY };
    }

    /** Moves the track to the middle of the coordinate space if it, or its
        pit lane, crosses the edge. Leaves the layout recalculated. */
    public static Result moveToMiddle(Track track) {
        Result r = new Result();
        track.calculateTrackLayout();
        if (!track.crossesCoordinateEdge())
            return r;
        double[] b = bounds(track);
        double minX = b[0], maxX = b[1], minY = b[2], maxY = b[3];
        if (!fits(new double[] { maxX - minX, maxY - minY })) {
            if (!isOpenTrackCrossingAtCorner(track)) {
                r.failure = "This track is longer than the game's map area in one direction,\n"
                          + "as open or straight tracks often are, so it always crosses the\n"
                          + "edge somewhere. It already crosses on a straight, away from the\n"
                          + "corners, so it has been left as it is.";
                return r;
            }
            return placeOpenTrack(track, r, minX, maxX, minY, maxY);
        }

        // Seg 0's display position is its real one, so shifting by minus
        // the centre puts the centre at the origin.
        TrackDataHeader header = track.getTrackDataHeader();
        int nOldX = header.getStartPos(0), nOldY = header.getStartPos(1);
        header.setStartPos(0, nOldX - (int) Math.round((minX + maxX) / 2.0 / 8.0));
        header.setStartPos(1, nOldY - (int) Math.round((minY + maxY) / 2.0 / 8.0));
        track.calculateTrackLayout();
        if (track.crossesCoordinateEdge()) {
            header.setStartPos(0, nOldX);
            header.setStartPos(1, nOldY);
            track.calculateTrackLayout();
            r.failure = "The track could not be moved clear of the edge of the game's\n"
                      + "map area.";
            return r;
        }
        r.moved = true;
        return r;
    }

    /** An open track: each axis it fits in is centred as usual; along an axis
        it does not fit in, the start is slid (in whole TLU) to put every
        crossing as far as possible from corners and the start/finish line,
        preferring placements where the pit lane does not cross. */
    private static Result placeOpenTrack(Track track, Result r,
                                         double minX, double maxX, double minY, double maxY) {
        TrackDataHeader header = track.getTrackDataHeader();
        int nRoom = TrackSegments.COORD_WRAP - 2 * MARGIN;
        int[] avoid = avoidDistance(track);
        double[] mins = { minX, minY }, maxs = { maxX, maxY };
        for (int axis = 0; axis < 2; axis++) {
            int nOld = header.getStartPos(axis);
            if (maxs[axis] - mins[axis] < nRoom) {
                header.setStartPos(axis, nOld - (int) Math.round((mins[axis] + maxs[axis]) / 2.0 / 8.0));
                continue;
            }
            int nBest = nOld, nBestScore = Integer.MIN_VALUE;
            for (int s = 0; s < TrackSegments.COORD_WRAP / 1024; s++) {
                header.setStartPos(axis, nOld + s * 128);
                track.calculateTrackLayout();
                int score = crossingScore(track, avoid, axis);
                if (score > nBestScore) { nBestScore = score; nBest = nOld + s * 128; }
            }
            header.setStartPos(axis, nBest);
        }
        track.calculateTrackLayout();
        r.moved = true;
        r.open = true;
        return r;
    }

    /** Per track Seg: how many Segs to the nearest Seg of a curved section or
        to the start/finish line (the line's seam and overshoot sit there). */
    private static int[] avoidDistance(Track track) {
        TrackSegments segs = track.getTrackSegments();
        int n = segs.getMaxTrackSegIndex() + 1;
        boolean[] bad = new boolean[n];
        int t = 0;
        for (int i = 0; i < segs.size(); i++) {
            TrackSegment ts = segs.get(i);
            for (int k = 0; k < ts.getTlu() && t < n; k++, t++)
                if (ts.getCurvature() != 0) bad[t] = true;
        }
        bad[0] = true;
        int[] d = new int[n];
        for (int i = 0; i < n; i++) {
            int best = n;
            for (int k = 0; k < n; k++) if (bad[k]) {
                int g = Math.abs(i - k);
                best = Math.min(best, Math.min(g, n - g));
            }
            d[i] = best;
        }
        return d;
    }

    /** The smallest avoidDistance over the track's crossings on one axis
        (0 = X, 1 = Y), less a large penalty when the pit lane crosses on it.
        A track with no crossing on that axis scores the most. */
    private static int crossingScore(Track track, int[] avoid, int axis) {
        int score = Integer.MAX_VALUE / 2;
        TrackSegments segs = track.getTrackSegments();
        int n = Math.min(avoid.length, segs.getMaxTrackSegIndex() + 1);
        for (int i = 1; i < n; i++)
            if (crosses(segs.getSegAt(i - 1), segs.getSegAt(i), axis))
                score = Math.min(score, avoid[i]);
        TrackSegments pit = track.getPitlaneSegments();
        for (int i = 1; i <= pit.getMaxTrackSegIndex(); i++)
            if (crosses(pit.getSegAt(i - 1), pit.getSegAt(i), axis)) {
                score -= 100000;
                break;
            }
        return score;
    }

    private static boolean crosses(Seg a, Seg b, int axis) {
        int d = axis == 0 ? b.getPosX() - a.getPosX() : b.getPosY() - a.getPosY();
        return Math.abs(d) > TrackSegments.COORD_WRAP / 2;
    }
}
