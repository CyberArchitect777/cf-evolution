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

import java.util.Vector;

import cfevolution.data.track.Command;
import cfevolution.data.track.Seg;
import cfevolution.data.track.Track;
import cfevolution.data.track.TrackSegment;
import cfevolution.data.track.TrackSegments;

/**
    Removes scenery that a generated track has put on another part of its own
    road (2026-11-01).

    A random track keeps the donor's objects at the same share of the lap, and
    each object keeps its sideways distance from the road, which is often large
    (grandstands and trees 1,000-1,500, against 32 for the road edge). Where the
    new layout runs back past that spot, the object stands on the road: a
    generated Adelaide had a tree there, the cars driving under its canopy, and
    grandstands across two other stretches.

    The game draws an object from the Seg it is anchored to, offset sideways by
    its definition's +4 word, d, as d/32 of the Seg's edge vector (F1GP-SDL,
    2026-10-10). The originals place some objects over another stretch on
    purpose, bridges above all (Adelaide has one within a tenth of a half-width
    of the road's centre), so this is for generated tracks only, where nothing
    is on purpose.
*/
public class SceneryClearance {

    /** Segs either side of the anchor that count as the object's own road. */
    private static final int OWN_ROAD_SEGS = 8;

    /** Nearer than this many half-widths to another stretch's centre is too
        near: the road, plus half of it again for the object's own size. */
    private static final double CLEAR_HALF_WIDTHS = 1.5;

    /** Removes every scenery object standing on or beside another part of
        the lap. The track must be laid out. Objects below definition 0x40
        (the pit lane connections and the game's own templates) stay.
        Returns the number removed. */
    public static int removeObjectsOverRoad(Track track) {
        TrackSegments segs = track.getTrackSegments();
        int n = segs.getMaxTrackSegIndex() + 1;
        if (n <= 2 * OWN_ROAD_SEGS)
            return 0;
        int nRemoved = 0, nLap = 0;
        for (int i = 0; i < segs.size(); i++) {
            TrackSegment ts = segs.get(i);
            Vector<Command> cmds = ts.getCommands();
            for (int c = cmds.size() - 1; c >= 0; c--) {
                Command cmd = cmds.get(c);
                if (cmd.getType() != 0x80 || cmd.getParam(1) < 0x40)
                    continue;
                int nAt = (nLap + (cmd.getParam(0) & 0xFF)) % n;
                if (overAnotherRoad(segs, n, nAt, track.getObjectSideOffset(cmd.getParam(1)))) {
                    cmds.remove(c);
                    nRemoved++;
                }
            }
            nLap += ts.getTlu();
        }
        return nRemoved;
    }

    private static boolean overAnotherRoad(TrackSegments segs, int n, int nAt, int nSide) {
        Seg s = segs.getSegAt(nAt);
        double dX = s.getPosX() + s.getTrackWidthX() / 8.0 * nSide / 32.0;
        double dY = s.getPosY() + s.getTrackWidthY() / 8.0 * nSide / 32.0;
        for (int j = 0; j < n; j++) {
            int nGap = Math.abs(j - nAt);
            if (Math.min(nGap, n - nGap) <= OWN_ROAD_SEGS)
                continue;
            Seg o = segs.getSegAt(j);
            double dHalf = Math.hypot(o.getTrackWidthX(), o.getTrackWidthY()) / 8.0;
            double dDx = TrackSegments.shortWay((int) Math.round(o.getPosX() - dX));
            double dDy = TrackSegments.shortWay((int) Math.round(o.getPosY() - dY));
            if (Math.hypot(dDx, dDy) < CLEAR_HALF_WIDTHS * dHalf)
                return true;
        }
        return false;
    }
}
