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

package cfevolution.edit;

import cfevolution.data.track.CCLine;
import cfevolution.data.track.CCLineSegment;
import cfevolution.data.track.Track;
import cfevolution.data.track.TrackSegment;
import cfevolution.data.track.TrackSegments;

/**
 * A track's list of parts (2026-11-17, from the desktop's TrackTreeSelector
 * and TrackWindow, word for word): the track data headers and the track, pit
 * and best line segments, each named by its number and direction; adding a
 * segment above or below one, deleting one, and clearing a whole list; and
 * the heading shown over the picked item's properties.
 */
public final class TrackOutline
{
    /** The branches, as the desktop's selection kinds number them. */
    public static final int NOTHING = 0, TRACK = 1, PIT = 2, LINE = 3, HEADERS = 4;

    public static final String ROOT = "Track Data";
    public static final String HEADERS_BRANCH = "Track Headers", TRACK_BRANCH = "Track Segments",
                               PIT_BRANCH = "Pit Segments", LINE_BRANCH = "Best Line Segments";

    public static final Message LAST_TRACK = Message.error("Error", "The last remaining track segment cannot be deleted.");
    public static final Message LAST_PIT = Message.error("Error", "The last remaining pit segment cannot be deleted.");
    public static final Message LAST_LINE = Message.error("Error", "The last remaining best line segment cannot be deleted.");
    public static final Message ADD_NOT_HERE = Message.error("Error", "Adding new sections is only possible in the track, pit or best line branches");
    public static final Message DELETE_NOT_HERE = Message.error("Error", "Deleting sections is only possible in the track, pit or best line branches");

    public static final Message CLEAR_TRACK = Message.ask(Message.Kind.WARNING, "Clear Track Segments", "Are you sure you want to remove all track segments?");
    public static final Message CLEAR_PIT = Message.ask(Message.Kind.WARNING, "Clear Pit Segments", "Are you sure you want to remove all pit segments?");
    public static final Message CLEAR_LINE = Message.ask(Message.Kind.WARNING, "Clear Best Line", "Are you sure you want to remove all best line segments?");

    public static final String NOTHING_HEADING = "Nothing selected", NOTHING_HINT = "Pick a segment in the list or click the map";
    public static final String HEADERS_HEADING = "Track data headers", HEADERS_HINT = "Settings that apply to the whole track";

    private TrackOutline()
    {
    }

    public static String branch(int kind)
    {
        switch (kind)
        {
            case TRACK: return TRACK_BRANCH;
            case PIT: return PIT_BRANCH;
            case LINE: return LINE_BRANCH;
            case HEADERS: return HEADERS_BRANCH;
            default: return ROOT;
        }
    }

    public static int count(Track t, int kind)
    {
        switch (kind)
        {
            case TRACK: return t.getTrackSegments().size();
            case PIT: return t.getPitlaneSegments().size();
            case LINE: return t.getCCLine().size();
            default: return 0;
        }
    }

    static TrackSegments segments(Track t, int kind)
    {
        return kind == TRACK ? t.getTrackSegments() : t.getPitlaneSegments();
    }

    /** A track or pit segment (1-based). */
    public static TrackSegment segment(Track t, int kind, int n)
    {
        return segments(t, kind).getAt(n);
    }

    public static CCLineSegment lineSegment(Track t, int n)
    {
        return t.getCCLine().getAt(n);
    }

    public static String direction(TrackSegment s)
    {
        return s.getCurvature() < 0 ? "Turn Left" : s.getCurvature() > 0 ? "Turn Right" : "Straight";
    }

    public static String direction(CCLineSegment s)
    {
        int radius = s.getType() == 128 ? s.getParam(2) : s.getParam(1);
        return radius == 0 ? "Straight" : radius < 0 ? "Turn Left" : "Turn Right";
    }

    /** A segment as the list names it: "12 Turn Left". */
    public static String label(Track t, int kind, int n)
    {
        return n + " " + (kind == LINE ? direction(lineSegment(t, n)) : direction(segment(t, kind, n)));
    }

    /** The heading over the picked item's properties. */
    public static String heading(int kind, int n)
    {
        switch (kind)
        {
            case TRACK: return "Track segment " + n;
            case PIT: return "Pit lane segment " + n;
            case LINE: return "Best line segment " + n;
            case HEADERS: return HEADERS_HEADING;
            default: return NOTHING_HEADING;
        }
    }

    /** The line under the heading: which way it turns and how long it is. */
    public static String hint(Track t, int kind, int n)
    {
        switch (kind)
        {
            case TRACK:
            case PIT:
            {
                TrackSegment s = segment(t, kind, n);
                int c = s.getCurvature();
                return (c < 0 ? "Turns left" : c > 0 ? "Turns right" : "Straight") + " · " + s.getTlu() + " TLU";
            }
            case LINE:
            {
                CCLineSegment s = lineSegment(t, n);
                return direction(s) + " · " + s.getTlu() + " TLU";
            }
            case HEADERS: return HEADERS_HINT;
            default: return NOTHING_HINT;
        }
    }

    /** The properties of the picked item, or null when it has none (a branch, or nothing). */
    public static PropertySheet sheet(Track t, int kind, int n)
    {
        switch (kind)
        {
            case TRACK:
            case PIT: return PropertySheet.segment(segment(t, kind, n));
            case LINE: return PropertySheet.bestLine(lineSegment(t, n));
            case HEADERS: return PropertySheet.headers(t.getTrackDataHeader());
            default: return null;
        }
    }

    /**
     * Adds a new segment (length 1, straight) above or below segment n and
     * returns the number of the segment that was picked, which moves down one
     * when the new one goes above it.
     */
    public static int insert(Track t, int kind, int n, boolean above)
    {
        int at = above ? n : n + 1;
        if (kind == LINE)
            t.getCCLine().insertAt(at);
        else
            segments(t, kind).insertAt(at);
        return above ? n + 1 : n;
    }

    /** Deletes segment n; the message when it is the last one (nothing is deleted), else null. */
    public static Message delete(Track t, int kind, int n)
    {
        if (count(t, kind) == 1)
            return kind == TRACK ? LAST_TRACK : kind == PIT ? LAST_PIT : LAST_LINE;
        if (kind == LINE)
            t.getCCLine().deleteAt(n);
        else
            segments(t, kind).deleteAt(n);
        return null;
    }

    /** After a delete: the segment to pick next (the one now at n, or the last). */
    public static int afterDelete(Track t, int kind, int n)
    {
        return Math.min(n, count(t, kind));
    }

    /** The question Edit > Clear ... asks first. */
    public static Message clearQuestion(int kind)
    {
        return kind == TRACK ? CLEAR_TRACK : kind == PIT ? CLEAR_PIT : CLEAR_LINE;
    }

    /** Removes every segment of a list but the first. */
    public static void clear(Track t, int kind)
    {
        if (kind == LINE)
        {
            CCLine lines = t.getCCLine();
            for (int x = lines.size(); x > 1; x--)
                lines.deleteAt(2);
        }
        else
        {
            TrackSegments s = segments(t, kind);
            for (int x = s.size(); x > 1; x--)
                s.deleteAt(2);
        }
    }
}
