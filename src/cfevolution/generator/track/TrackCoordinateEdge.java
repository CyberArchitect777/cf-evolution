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
        public String failure;     // why not, or null; plain language
    }

    /** True when the track crosses the edge AND could be moved clear of it.
        A track longer than the coordinate space in some direction (an open
        or straight track, such as the minimal-track canvas or Mike Saunders'
        straight Monza) cannot help crossing; those are left alone and not
        warned about. Needs the layout calculated. */
    public static boolean shouldMove(Track track) {
        return track.crossesCoordinateEdge() && fits(span(track));
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
            r.failure = "This track is longer than the game's map area in one direction,\n"
                      + "as open or straight tracks often are, so it cannot be moved\n"
                      + "clear of the edge. It has been left as it is.";
            return r;
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
}
