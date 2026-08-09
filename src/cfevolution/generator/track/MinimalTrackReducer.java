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

import cfevolution.data.track.Command;
import cfevolution.data.track.Seg;
import cfevolution.data.track.Track;
import cfevolution.data.track.TrackSegment;
import cfevolution.data.track.TrackSegments;
import cfevolution.generator.pitlane.PitLaneFitter;

import java.util.Vector;

/**
    Reduces a track to a minimal STRAIGHT canvas that still carries all of
    its start/finish infrastructure: the pit lane, the starting grid and
    the S/F line. The result is a starting point for a creator to build
    corners onto, not a finished track — no best line is produced.

    Four steps:

    1. STRAIGHTEN. Every kept section's curvature and height is zeroed.
       This is what makes the rest simple, and it is why an earlier
       attempt failed: the stretch between the pit connects carries real
       corners on most circuits (Monza's Parabolica runs right through
       it), and trying to build a straight canvas around them meant
       fighting geometry that was never going to cooperate.

    2. CUT. The 0x86 entry and 0x87 exit connects, with a section of
       margin either side, bound what is kept. The kept stretch WRAPS the
       start/finish line — going round the lap the order is always pit
       entry (late) -> grid -> S/F line -> wrap -> pit exit (early) — so
       in file terms a HEAD and a TAIL are kept and the middle removed.

    3. PAD TO A WHOLE COORDINATE PERIOD. World X/Y are 19-bit (see
       Seg.setPos), so positions repeat every 512 TLU. A straight track
       whose length is a multiple of that ends exactly where it started
       in the game's coordinates, so the engine's own closing pass
       (TCRecalcPosToFit, which slides every Seg until the ends meet) has
       nothing to correct and the road is drawn at its true length.
       Without this the road is drawn badly squashed — F1CT12 rendered
       its 310 TLU of track in 79 TLU of space, folded over itself.

       This only works because the track runs along an axis, so the whole
       512 TLU period lands on the coordinate grid. Thirteen of the
       sixteen originals already start at angle 0; the other three are
       set to it.

    4. REBUILD THE PIT LANE'S IN AND OUT. A donor pit lane's entry and
       exit tapers curve to follow its own track's corners, so against a
       straight canvas they would walk the pit lane away from the road.
       PitLaneFitter.neutralizeCurvature replaces them with a balanced
       S-bend out and back — the same treatment the random track
       generator applies for the same reason.

    Heading closure comes free: with every curvature zero the lap turns
    through nothing at all, so the last segment faces the way the first
    does, which is the one thing the game needs in order to wrap.
*/
public class MinimalTrackReducer {

    /** World X/Y are 19-bit, so positions repeat over this distance. */
    private static final int WRAP_PERIOD_TLU = 512;

    /** Padding is added as sections of at most this length — inside the
        191 TLU the file format holds. */
    private static final int PAD_PIECE_TLU = 100;

    /** A marking (0x8A/0x8B) with this MarkingType paints the pit lane's
        dashed in/out lane line — the "yellow line" either side of the
        pit connects (Session 18). */
    private static final int MARKING_TYPE_PIT_PAINT = 3;

    /** The grid is drawn by a marking group with this many dotted lines
        (13 grid boxes per column in the originals). */
    private static final int GRID_MIN_DOTTED_LINES = 10;

    /** How close to the lap end the single-line marking at X=0 must be to
        count as the S/F line (the originals put it on the last TLU). */
    private static final int SF_LINE_WINDOW = 3;

    /** A step longer than this is a coordinate wrap, not road. */
    private static final double WRAP_JUMP_UNITS = 100000;

    /** Outcome of an analysis or a reduction. */
    public static class Result {
        /** False if the track lacks the features needed to find the cut. */
        public boolean ok;
        /** Why the reduction is not possible (null when ok). */
        public String failure;

        /** Last section kept at the head of the file (1-based). */
        public int keepHead;
        /** First section kept at the tail of the file (1-based). */
        public int keepTail;
        public int removedSections;
        public int removedTlu;
        public int originalTlu;
        /** Length of the finished canvas — a whole number of 512 TLU
            coordinate periods. */
        public int resultTlu;
        /** Straight road added to reach that length. */
        public int padTlu;
        /** Sections whose curvature or height was zeroed. */
        public int straightenedSections;
        /** True if the header's start angle had to be squared up. */
        public boolean startAngleChanged;

        /** Distance from the end of the track back to its start, in TLU.
            Should be about zero: the canvas lands on its own coordinates. */
        public double gapTlu;
        /** How far the road's drawn length differs from the length the
            data claims, as a fraction. Should be about zero. */
        public double drawnError;
        /** The same for the pit lane. Never quite zero: the originals
            themselves draw their pit lanes 3-5% long, because the pit is
            stretched to meet its connects. */
        public double pitDrawnError;

        /** Feature positions found, in TLU from the S/F line. */
        public int entryPaintTlu = -1;
        public int entryConnectTlu = -1;
        public int gridTlu = -1;
        public int sfLineTlu = -1;
        public int exitPaintTlu = -1;
        public int exitConnectTlu = -1;

        /** Human-readable notes for the result dialog. */
        public final Vector notes = new Vector();
    }

    /** One located feature: where it is and which section carries it. */
    private static class Feature {
        final int tlu;
        final int section;
        Feature(int nTlu, int nSection) { tlu = nTlu; section = nSection; }
    }

    private final Track track;

    public MinimalTrackReducer(Track theTrack) {
        track = theTrack;
    }

    /** Works out what the reduction would do, without changing anything. */
    public Result analyse() {
        return plan();
    }

    /** Cuts the track down to a straight canvas. The caller is responsible
        for clearing the best line, emptying the camera adjustments and
        recompiling the layout. */
    public Result reduce() {
        Result r = plan();
        if (!r.ok)
            return r;
        apply(r);
        return r;
    }

    /** Finds the pit connects, the grid and the S/F line, and decides
        which sections the cut keeps. */
    private Result plan() {
        Result r = new Result();
        TrackSegments segs = track.getTrackSegments();
        int nLast = lastRealSection(segs);
        if (nLast < 4) {
            r.failure = "The track has too few sections to reduce.";
            return r;
        }

        int[] anStart = sectionStartTlu(segs, nLast);
        r.originalTlu = anStart[nLast] + segs.getAt(nLast).getTlu();

        Feature entryConnect = findCommand(segs, nLast, anStart, 0x86);
        Feature exitConnect  = findCommand(segs, nLast, anStart, 0x87);
        if (entryConnect == null || exitConnect == null) {
            r.failure = "No pit lane connect commands (0x86/0x87) found — "
                      + "this tool needs a track with a pit lane.";
            return r;
        }
        if (exitConnect.section >= entryConnect.section) {
            r.failure = "The pit exit is not before the pit entry in the file, "
                      + "so there is no middle section to remove.";
            return r;
        }

        Feature entryPaint = lastPaintAtOrBefore(segs, nLast, anStart, entryConnect.tlu);
        Feature exitPaint  = lastPaintAtOrBefore(segs, nLast, anStart, exitConnect.tlu);
        Feature grid       = findGrid(segs, nLast, anStart);
        Feature sfLine     = findSfLine(segs, nLast, anStart, r.originalTlu);

        r.entryConnectTlu = entryConnect.tlu;
        r.exitConnectTlu  = exitConnect.tlu;
        if (entryPaint != null) r.entryPaintTlu = entryPaint.tlu;
        if (exitPaint  != null) r.exitPaintTlu  = exitPaint.tlu;
        if (grid       != null) r.gridTlu       = grid.tlu;
        if (sfLine     != null) r.sfLineTlu     = sfLine.tlu;

        // Start of the kept stretch: whichever of the pit entry paint and
        // the grid comes first, then one section earlier. A MarkingType-3
        // group early in the lap belongs to the pit exit, not the entry.
        Feature startFeature = grid;
        if (entryPaint != null && entryPaint.section > exitConnect.section
            && (startFeature == null || entryPaint.tlu < startFeature.tlu))
            startFeature = entryPaint;
        if (startFeature == null)
            startFeature = entryConnect;

        // End of the kept stretch: whichever of the pit exit paint and the
        // exit connect comes last, then one section later. The S/F line is
        // inside the kept region already, on the lap's last TLU.
        Feature endFeature = exitConnect;
        if (exitPaint != null && exitPaint.section > endFeature.section)
            endFeature = exitPaint;

        r.keepTail = Math.max(2, startFeature.section - 1);
        r.keepHead = Math.min(nLast - 1, endFeature.section + 1);

        if (r.keepHead >= r.keepTail - 1) {
            r.failure = "The pit lane and grid already span the whole lap — "
                      + "there is nothing between them to remove.";
            return r;
        }

        int nKeptTlu = 0;
        for (int i = 1; i <= nLast; i++) {
            TrackSegment seg = segs.getAt(i);
            if (i <= r.keepHead || i >= r.keepTail)
                nKeptTlu += seg.getTlu();
            else {
                r.removedSections++;
                r.removedTlu += seg.getTlu();
            }
        }
        r.resultTlu = padTarget(nKeptTlu);
        r.padTlu = r.resultTlu - nKeptTlu;

        noteLostCommands(segs, nLast, r);
        r.ok = true;
        return r;
    }

    /** The finished length: the first whole coordinate period that holds
        everything kept. */
    private static int padTarget(int nKeptTlu) {
        int nPeriods = (nKeptTlu + WRAP_PERIOD_TLU - 1) / WRAP_PERIOD_TLU;
        return Math.max(1, nPeriods) * WRAP_PERIOD_TLU;
    }

    private void apply(Result r) {
        TrackSegments segs = track.getTrackSegments();
        int nLast = lastRealSection(segs);

        // Track width is set by 0x85 commands as the lap runs, so the width
        // the kept tail was authored at may be set by a command the cut is
        // about to delete. Carry the tail's own starting width across.
        int nWidthIntoTail = widthAt(segs, r.keepTail);
        int nWidthAfterHead = widthAt(segs, r.keepHead + 1);

        // 1. Straighten everything that survives.
        for (int i = 1; i <= nLast; i++) {
            if (i > r.keepHead && i < r.keepTail)
                continue;
            TrackSegment seg = segs.getAt(i);
            if (seg.getCurvature() != 0 || seg.getHeightChange() != 0)
                r.straightenedSections++;
            seg.setCurvature(0);
            seg.setHeightChange(0);
        }

        // 2. Remove the middle, from the back so the indices stay valid.
        for (int i = r.keepTail - 1; i > r.keepHead; i--)
            segs.deleteAt(i);

        // 3. Pad at the cut — not at the end, so the grid and the S/F line
        // stay on the lap's last TLU where the game expects them.
        int nInsertAt = r.keepHead + 1;
        int nAt = nInsertAt;
        int nLeft = r.padTlu;
        while (nLeft > 0) {
            int nPiece = Math.min(PAD_PIECE_TLU, nLeft);
            TrackSegment pad = segs.insertAt(nAt++);
            pad.setTlu(nPiece);
            pad.setCurvature(0);
            pad.setHeightChange(0);
            pad.setFlags(0);
            nLeft -= nPiece;
        }
        if (nWidthIntoTail != nWidthAfterHead) {
            TrackSegment first = segs.getAt(nInsertAt);
            if (first != null) {
                first.getCommands().add(new Command(0x85, 0, 1, nWidthIntoTail, 0, 0, 0));
                r.notes.add("Track width carried across the cut (" + nWidthAfterHead
                            + " -> " + nWidthIntoTail + ").");
            }
        }

        // 4. A straight canvas only lands back on its own coordinates if it
        // runs along an axis.
        int nStartAngle = track.getTrackDataHeader().getStartAngle();
        if (nStartAngle % (FULL_TURN / 4) != 0) {
            track.getTrackDataHeader().setStartAngle(0);
            r.startAngleChanged = true;
            r.notes.add("Start angle squared up to 0 (was " + nStartAngle
                        + ") so the straight lands back on its own coordinates.");
        }

        // 5. The donor's pit entry and exit tapers curve to follow its own
        // track's corners; against a straight canvas they would walk the
        // pit lane off the road.
        PitLaneFitter.neutralizeCurvature(track.getPitlaneSegments(),
                                          track.getTrackDataHeader().getPitSide() ? -1 : 1);

        track.setLayoutMode(false);
        track.calculateTrackLayout();
        r.resultTlu = segs.getTotalTlu();
        r.gapTlu = TrackHeadingClosure.gapTlu(track);
        r.drawnError = drawnError(track.getTrackSegments());
        r.pitDrawnError = drawnError(track.getPitlaneSegments());
    }

    private static final int FULL_TURN = 65536;

    /** How far a segment list's DRAWN length is from the length its data
        claims, as a fraction. The engine closes an open lap by sliding
        every Seg until the ends meet, which stretches or squashes every
        piece of road; this measures the result of that. */
    private double drawnError(TrackSegments segs) {
        int nNominal = segs.getTotalTlu();
        if (nNominal <= 0 || segs.getMaxTrackSegIndex() <= 0)
            return 0;
        double dDrawn = 0;
        for (int i = 1; i <= segs.getMaxTrackSegIndex(); i++) {
            Seg p = segs.getSegAt(i - 1), q = segs.getSegAt(i);
            double dStep = Math.hypot(q.getPosX() - p.getPosX(), q.getPosY() - p.getPosY());
            dDrawn += dStep < WRAP_JUMP_UNITS ? dStep : 1024;
        }
        return Math.abs(dDrawn / 1024.0 - nNominal) / nNominal;
    }

    /** Track width in force when the given section starts: the header's
        start width, then every completed 0x85 change before it. */
    private int widthAt(TrackSegments segs, int nSection) {
        int nWidth = track.getTrackDataHeader().getStartWidth();
        for (int i = 1; i < nSection && i < segs.size(); i++) {
            Vector cmds = segs.getAt(i).getCommands();
            for (int c = 0; c < cmds.size(); c++) {
                Command cmd = (Command) cmds.get(c);
                if (cmd.getType() == 0x85)
                    nWidth = (short) cmd.getParam(2);
            }
        }
        return nWidth;
    }

    /** Records command types the cut removes from the track entirely, so
        the user can judge whether any of them mattered. */
    private void noteLostCommands(TrackSegments segs, int nLast, Result r) {
        java.util.TreeSet kept = new java.util.TreeSet();
        java.util.TreeSet cut = new java.util.TreeSet();
        int nCutCommands = 0;
        for (int i = 1; i <= nLast; i++) {
            boolean fKept = i <= r.keepHead || i >= r.keepTail;
            Vector cmds = segs.getAt(i).getCommands();
            for (int c = 0; c < cmds.size(); c++) {
                Integer type = new Integer(((Command) cmds.get(c)).getType());
                if (fKept) {
                    kept.add(type);
                } else {
                    cut.add(type);
                    nCutCommands++;
                }
            }
        }
        cut.removeAll(kept);
        if (nCutCommands > 0)
            r.notes.add(nCutCommands + " commands removed with the cut sections.");
        if (!cut.isEmpty()) {
            StringBuffer sb = new StringBuffer("Command types no longer present anywhere: ");
            for (java.util.Iterator it = cut.iterator(); it.hasNext(); ) {
                sb.append("0x").append(Integer.toHexString(((Integer) it.next()).intValue()).toUpperCase());
                if (it.hasNext()) sb.append(' ');
            }
            r.notes.add(sb.toString());
        }
    }

    // ---- feature location -------------------------------------------------

    /** The vector's last element is the 0xFF terminator, which carries no
        length but may carry commands. */
    private static int lastRealSection(TrackSegments segs) {
        return segs.size() - 1;
    }

    /** TLU at which each section starts, indexed 1-based like getAt(). */
    private static int[] sectionStartTlu(TrackSegments segs, int nLast) {
        int[] anStart = new int[nLast + 2];
        int nCum = 0;
        for (int i = 1; i <= nLast; i++) {
            anStart[i] = nCum;
            nCum += segs.getAt(i).getTlu();
        }
        anStart[nLast + 1] = nCum;
        return anStart;
    }

    private static Feature findCommand(TrackSegments segs, int nLast, int[] anStart, int nType) {
        for (int i = 1; i <= nLast; i++) {
            Vector cmds = segs.getAt(i).getCommands();
            for (int c = 0; c < cmds.size(); c++) {
                Command cmd = (Command) cmds.get(c);
                if (cmd.getType() == nType)
                    return new Feature(anStart[i] + cmd.getParam(0), i);
            }
        }
        return null;
    }

    /** The pit lane paint nearest before a connect, i.e. that connect's own
        dashed lane line. */
    private static Feature lastPaintAtOrBefore(TrackSegments segs, int nLast,
                                               int[] anStart, int nTlu) {
        Feature best = null;
        for (int i = 1; i <= nLast; i++) {
            Vector cmds = segs.getAt(i).getCommands();
            for (int c = 0; c < cmds.size(); c++) {
                Command cmd = (Command) cmds.get(c);
                if (!isMarking(cmd) || cmd.getParam(1) != MARKING_TYPE_PIT_PAINT)
                    continue;
                int nAt = anStart[i] + cmd.getParam(0);
                if (nAt <= nTlu && (best == null || nAt > best.tlu))
                    best = new Feature(nAt, i);
            }
        }
        return best;
    }

    private static Feature findGrid(TrackSegments segs, int nLast, int[] anStart) {
        Feature best = null;
        for (int i = 1; i <= nLast; i++) {
            Vector cmds = segs.getAt(i).getCommands();
            for (int c = 0; c < cmds.size(); c++) {
                Command cmd = (Command) cmds.get(c);
                if (!isMarking(cmd) || cmd.getParam(2) < GRID_MIN_DOTTED_LINES)
                    continue;
                int nAt = anStart[i] + cmd.getParam(0);
                if (best == null || nAt < best.tlu)
                    best = new Feature(nAt, i);
            }
        }
        return best;
    }

    private static Feature findSfLine(TrackSegments segs, int nLast, int[] anStart, int nTotalTlu) {
        for (int i = nLast; i >= 1; i--) {
            Vector cmds = segs.getAt(i).getCommands();
            for (int c = 0; c < cmds.size(); c++) {
                Command cmd = (Command) cmds.get(c);
                if (!isMarking(cmd) || cmd.getParam(2) != 1 || (short) cmd.getParam(3) != 0)
                    continue;
                int nAt = anStart[i] + cmd.getParam(0);
                if (nAt >= nTotalTlu - SF_LINE_WINDOW)
                    return new Feature(nAt, i);
            }
        }
        return null;
    }

    private static boolean isMarking(Command cmd) {
        return cmd.getType() == 0x8A || cmd.getType() == 0x8B;
    }
}
