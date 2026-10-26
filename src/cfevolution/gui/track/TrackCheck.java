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

package cfevolution.gui.track;

import cfevolution.data.track.*;
import cfevolution.generator.track.TrackCoordinateEdge;
import cfevolution.generator.track.TrackHeadingClosure;
import java.util.ArrayList;
import java.util.List;

/**
 * The track checker (Tools > Check Track): every problem the editor knows
 * how to detect, with a verdict for the whole track. No Swing here, so a
 * harness can run it headlessly. Started from the uitest7 prototype's
 * TrackHealth (2026-10-22), without its step rail.
 *
 * Messages are plain language (owner, 2026-10-11): they say what is wrong
 * and what to do, never the figures behind the check.
 */
public class TrackCheck
{
    public static final int OK = 1, WARNING = 2, ERROR = 3;

    /** What a check's button does. */
    public static final int ACTION_NONE = 0, ACTION_MOVE_TRACK = 1, ACTION_MATCH_HEADING = 2, ACTION_GENERATE_LINE = 3, ACTION_PIT_MOUTHS = 4;

    /** Same thresholds as GenerateCCLineDialog.lineQualityWarning, which
        were calibrated so that no original trips them. */
    static final double WORST_EXCURSION_WARNING = 2.0;
    private static final int MAX_KICK_WARNING = 3000;

    public static class Item
    {
        public final String area, title, detail;
        public final int status, action;

        Item(String area, int status, String title, String detail, int action)
        {
            this.area = area;
            this.status = status;
            this.title = title;
            this.detail = detail;
            this.action = action;
        }
    }

    public static class Report
    {
        public final List<Item> items = new ArrayList<Item>();

        public int overall()
        {
            int worst = OK;
            for (Item i : items)
                worst = Math.max(worst, i.status);
            return worst;
        }

        public int count(int status)
        {
            int n = 0;
            for (Item i : items)
                if (i.status == status)
                    n++;
            return n;
        }

        void ok(String area, String title) { items.add(new Item(area, OK, title, null, ACTION_NONE)); }
        void add(String area, int status, String title, String detail, int action)
        {
            items.add(new Item(area, status, title, detail, action));
        }
    }

    public static Report check(Track track)
    {
        Report r = new Report();
        TrackSegments segs = track.getTrackSegments();
        String layout = "Layout", pit = "Pit lane", line = "Best line", file = "File";

        // --- Layout -------------------------------------------------------
        if (track.getTrackSegCount() > Track.MAX_TRACK_SEGS)
            r.add(layout, ERROR, "The track is too long for the game",
                "The game will freeze while loading it. Shorten some straights or remove a corner.", ACTION_NONE);
        else
            r.ok(layout, "The track's length fits the game");

        if (!TrackHeadingClosure.allStraight(segs) && !TrackHeadingClosure.headingMatches(segs))
            r.add(layout, WARNING, "The end of the track does not point the way the start does",
                "A best line cannot be built until it does. One extra curved segment at the end will line them up.",
                ACTION_MATCH_HEADING);
        else
            r.ok(layout, "The end of the track lines up with the start");

        if (TrackCoordinateEdge.shouldMove(track) && TrackCoordinateEdge.isOpen(track))
            r.add(layout, WARNING, "The track crosses the edge of the game's map area near a corner",
                "The game would build a wrong line for the computer cars there. Moving the track puts the crossing "
              + "on a straight; then generate the best line again.", ACTION_MOVE_TRACK);
        else if (TrackCoordinateEdge.shouldMove(track))
            r.add(layout, WARNING, "The track runs over the edge of the game's map area",
                "The game would build a wrong line for the computer cars. Moving the track changes nothing else about it.",
                ACTION_MOVE_TRACK);
        else
            r.ok(layout, "The track sits inside the game's map area");

        // gapRatio recompiles the layout, which clears the best line stamped
        // into the Segs; compile the line again so the checks below see it.
        double dRatio = TrackHeadingClosure.gapRatio(track);
        track.calculateCCLine();
        if (dRatio > 0.5)
            r.add(layout, WARNING, "The end of the track is far from the start",
                "The game will stretch the road a lot to join them, so expect visible distortion.", ACTION_NONE);
        else if (dRatio > TrackHeadingClosure.MONZA_GAP_RATIO * 1.2)
            r.add(layout, WARNING, "The end of the track is some way from the start",
                "The game joins them by stretching the road, which may look odd. Worth trying in the game.", ACTION_NONE);
        else
            r.ok(layout, "The end of the track meets the start");

        // --- Pit lane -----------------------------------------------------
        int nPitSegs = track.getPitSegCount();
        if (nPitSegs > Track.MAX_PIT_SEGS)
            r.add(pit, ERROR, "The pit lane is too long for the game",
                "The game will freeze while loading it. Shorten the pit lane's straight sections.", ACTION_NONE);
        else if (nPitSegs > 0)
            r.ok(pit, "The pit lane's length fits the game");

        // Only for tracks that already have the pit lane objects (an older random
        // track, say); adding them to a converted track that has none can make the
        // game draw faults (PitMouth.hasPitObjects).
        if (nPitSegs > 0 && !track.getLayoutMode()
            && cfevolution.generator.pitlane.PitMouth.hasPitObjects(track))
        {
            if (cfevolution.generator.pitlane.PitMouth.looksRight(track))
                r.ok(pit, "The pit lane's entrance and exit are set up for the game");
            else
                r.add(pit, WARNING, "The pit lane's entrance and exit are not set up the way the game expects",
                    "The game may not draw the pit lane where it leaves the track, or may draw a wall across it. "
                  + "The editor can set them up from the pit lane's shape.", ACTION_PIT_MOUTHS);
        }

        // --- Best line ----------------------------------------------------
        int ccTlu = track.getCCLineTlu();
        if (track.getCCLine().size() == 0 || ccTlu == 0)
            r.add(line, WARNING, "There is no best line",
                "The computer cars follow the best line round the lap. Generate one.", ACTION_GENERATE_LINE);
        else
        {
            if (ccTlu < track.getTrackTlu())
                r.add(line, WARNING, "The best line stops before the end of the lap",
                    "The computer cars have no line to follow on the last part of the track.", ACTION_GENERATE_LINE);
            else
                r.ok(line, "The best line covers the whole lap");

            if (!track.getLayoutMode() && lineRunsOffRoad(track))
                r.add(line, WARNING, "Parts of the best line run well off the road",
                    "The computer cars will struggle there. The orange stretches on the map show where.", ACTION_GENERATE_LINE);
            else
                r.ok(line, "The best line stays on the road");

            if (track.getCCLine().getCoachingEntryCount() > track.getCoachingEntryBudget())
                r.add(line, ERROR, "The best line has too many curves for the game",
                    "The game will freeze while loading it. Generating the line again keeps it within the limit.",
                    ACTION_GENERATE_LINE);
            else
                r.ok(line, "The best line fits the game's limits");
        }

        // --- File ---------------------------------------------------------
        long lSize = track.measureSavedSize();
        if (lSize > Track.MAX_FILE_BYTES)
            r.add(file, ERROR, "The track holds more than the game can load",
                "The game will freeze while loading it. Remove some scenery objects to make room.", ACTION_NONE);
        else if (lSize > 0)
            r.ok(file, "The track fits in the game's memory");

        return r;
    }

    /** True when the stored line strays as far as GenerateCCLineDialog's
        quality warning allows. Reads the line as the editor compiled it. */
    static boolean lineRunsOffRoad(Track track)
    {
        cfevolution.generator.ccline.CCLineTrackGeometry geo =
            new cfevolution.generator.ccline.CCLineTrackGeometry(track);
        TrackSegments segs = track.getTrackSegments();
        int n = Math.min(geo.segCount, segs.getMaxTrackSegIndex() + 1);
        if (n < 2)
            return false;
        double dWorst = 0;
        int nMaxKick = 0;
        for (int i = 0; i < n; i++)
        {
            int cc = segs.getSegAt(i).getCCLine();
            if (geo.physicalBound[i] > 0)
                dWorst = Math.max(dWorst, Math.abs(cc) / geo.physicalBound[i]);
            int j = i + 1 >= n ? 0 : i + 1;
            nMaxKick = Math.max(nMaxKick, Math.abs(segs.getSegAt(j).getCCLine() - cc));
        }
        return dWorst > WORST_EXCURSION_WARNING || nMaxKick > MAX_KICK_WARNING;
    }

    /** One-line verdict for the whole track. */
    public static String verdict(int overall)
    {
        if (overall == ERROR)
            return "Will not load in the game";
        if (overall == WARNING)
            return "Needs attention";
        return "Ready to race";
    }
}
