package cfevolution.edit;

import cfevolution.data.track.CCLine;
import cfevolution.data.track.Track;
import cfevolution.data.track.TrackSegments;
import cfevolution.generator.pitlane.PitMouth;
import cfevolution.generator.track.MinimalTrackReducer;
import cfevolution.generator.track.TrackCameraSection;
import cfevolution.generator.track.TrackCoordinateEdge;
import cfevolution.generator.track.TrackHeadingClosure;
import java.util.ArrayList;
import java.util.List;

/**
 * The track tools every interface offers, without the interface (2026-11-16,
 * moved out of TrackWindow word for word): moving a track away from the map
 * edge, matching the start and end heading, reducing to a minimal track,
 * setting up the pit lane's entrance and exit, what blocks best line
 * generation, the banner warnings and the warning after saving.
 *
 * Each changes only the Track; the interface then refreshes its own views
 * and shows the Message. A tool that asks first comes as a Plan: show its
 * failure, or its question, then call apply().
 */
public final class TrackTools
{
    private TrackTools()
    {
    }

    /** What a tool did: whether the track changed, and what to tell the user (may be null). */
    public static final class Outcome
    {
        public final boolean changed;
        public final Message message;

        Outcome(boolean changed, Message message)
        {
            this.changed = changed;
            this.message = message;
        }
    }

    /** A tool that may need the user's yes first. */
    public abstract static class Plan
    {
        /** Why it cannot be done (nothing else applies), or null. */
        public Message failure;
        /** Shown instead of asking when nothing needs doing, or null. */
        public Message nothingToDo;
        /** The yes/no question to ask before apply(), or null. */
        public Message question;

        public boolean canApply()
        {
            return failure == null && nothingToDo == null;
        }

        /** Does it. Only after the user said yes to the question, if there was one. */
        public abstract Outcome apply();
    }

    /** What a view of the track does before drawing it: lays the track out and
        works out the best line along it. The desktop map does it on every
        redraw (TrackPanel.redrawMap), including right after a track is opened
        and after every change, so another interface showing a track must do the
        same to start each operation from the state the desktop editor does. */
    public static void recalculate(Track track)
    {
        track.calculateTrackLayout();
        track.calculateCCLine();
    }

    // ------------------------------------------------------------------
    // Move Track Away From Map Edge

    public static final String MOVE_TITLE = "Move Track Away From Map Edge";

    public static Outcome moveFromEdge(Track track)
    {
        if (!track.crossesCoordinateEdge())
            return new Outcome(false, Message.info(MOVE_TITLE,
                "This track is already clear of the edge of the game's map area.\n"
              + "Nothing needs to change."));
        TrackCoordinateEdge.Result result = TrackCoordinateEdge.moveToMiddle(track);
        if (result.failure != null)
            return new Outcome(false, Message.warning(MOVE_TITLE, result.failure));
        if (result.open)
            return new Outcome(true, Message.info(MOVE_TITLE,
                "This track is longer than the game's map area, so it always crosses\n"
              + "the edge somewhere. It has been moved so that it crosses on a straight,\n"
              + "away from the corners, where the game builds the best line correctly.\n\n"
              + "Generate the best line again: the old one was made for the old position.\n"
              + "Save the track to keep the change."));
        return new Outcome(true, Message.info(MOVE_TITLE,
            "The track has been moved to the middle of the game's map area.\n"
          + "It ran over the edge, which makes the game build a wrong best line.\n"
          + "Its shape, pit lane and scenery are unchanged, and the game will\n"
          + "now build the best line correctly.\n\n"
          + "Save the track to keep the change."));
    }

    // ------------------------------------------------------------------
    // Add Segment to Match Start/End Heading

    public static final String HEADING_TITLE = "Match Start/End Heading";

    public static Plan planHeading(final Track track)
    {
        Plan p = new Plan() {
            public Outcome apply()
            {
                TrackHeadingClosure.Result result = TrackHeadingClosure.addClosingSegment(track);
                track.setLayoutMode(false);
                track.calculateTrackLayout();
                StringBuffer msg = new StringBuffer();
                msg.append("Added a ").append(result.addedTlu).append(" TLU segment at the end.\n")
                   .append("The end of the track now lines up with the start.\n");
                appendGapReport(track, msg);
                int nCcTlu = track.getCCLineTlu();
                if (nCcTlu > 0 && nCcTlu < track.getTrackTlu())
                    msg.append("\nThe best line no longer reaches the end of the track. Use\n")
                       .append("Tools > Generate Best Line with \"Complete existing line\" selected.");
                return new Outcome(true, Message.info(HEADING_TITLE, msg.toString()));
            }
        };
        TrackHeadingClosure.Result plan = TrackHeadingClosure.analyse(track);
        if (!plan.ok)
        {
            p.failure = Message.warning(HEADING_TITLE, plan.failure);
            return p;
        }
        if (plan.alreadyMatching)
        {
            StringBuffer msg = new StringBuffer();
            msg.append("The end of the track already lines up with the start.\n")
               .append("Nothing needs adding.\n");
            appendGapReport(track, msg);
            p.nothingToDo = Message.info(HEADING_TITLE, msg.toString());
            return p;
        }
        double dDegrees = plan.headingNeeded * 360.0 / TrackHeadingClosure.FULL_TURN;
        StringBuffer ask = new StringBuffer();
        ask.append("The end of the track faces ")
           .append(Math.round(Math.abs(dDegrees) * 10) / 10.0)
           .append(" degrees away from the start.\n\n")
           .append("Add one curved segment, ").append(plan.addedTlu)
           .append(" TLU long, at the end of the track to line them up?");
        p.question = Message.ask(Message.Kind.QUESTION, HEADING_TITLE, ask.toString());
        return p;
    }

    /** Adds the start-to-end gap and what it means to a report. The game
        closes the gap itself by sliding every Seg, so the figure that
        matters is the gap relative to the lap length. */
    static void appendGapReport(Track track, StringBuffer msg)
    {
        double dGap = TrackHeadingClosure.gapTlu(track);
        double dRatio = track.getTrackTlu() > 0 ? dGap / track.getTrackTlu() : 0;
        // Plain-language bands of TrackHeadingClosure.describeGap (owner, 2026-10-11).
        if (dRatio <= 0.02)
            return;
        if (dRatio <= TrackHeadingClosure.MONZA_GAP_RATIO * 1.2)
            msg.append("\nThe end of the track does not quite meet the start. The game\n")
               .append("joins them by stretching the road slightly, which works fine.\n");
        else if (dRatio <= 0.5)
            msg.append("\nThe end of the track is some way from the start. The game joins\n")
               .append("them by stretching the road, which may look odd: worth checking\n")
               .append("in the game.\n");
        else
            msg.append("\nThe end of the track is far from the start. The game will stretch\n")
               .append("the road a lot to join them, so expect visible distortion.\n");
    }

    // ------------------------------------------------------------------
    // Reduce to Minimal Track

    public static final String MINIMAL_TITLE = "Reduce to Minimal Track";

    public static Plan planMinimal(final Track track)
    {
        final MinimalTrackReducer reducer = new MinimalTrackReducer(track);
        Plan p = new Plan() {
            public Outcome apply()
            {
                MinimalTrackReducer.Result result = reducer.reduce();
                if (!result.ok)
                    return new Outcome(false, Message.warning(MINIMAL_TITLE, result.failure));
                TrackCameraSection.emptyCameraAdjustments(track.getFooter());
                track.setLayoutMode(false);
                track.calculateTrackLayout();
                clearBestLine(track);
                // Plain language only (owner, 2026-10-11): the cut, padding and
                // drawn-length figures are in MinimalTrackReducer.Result for harnesses.
                StringBuffer msg = new StringBuffer();
                msg.append("The track is now a straight carrying the pit lane, the starting\n")
                   .append("grid and the start/finish line.\n\n")
                   .append("The best line was cleared. Once the track is the shape you want,\n")
                   .append("use Tools > Generate Best Line.");
                return new Outcome(true, Message.info(MINIMAL_TITLE, msg.toString()));
            }
        };
        MinimalTrackReducer.Result plan = reducer.analyse();
        if (!plan.ok)
        {
            p.failure = Message.warning(MINIMAL_TITLE, plan.failure);
            return p;
        }
        StringBuffer ask = new StringBuffer();
        ask.append("Cut this track down to a straight that keeps only the pit lane,\n")
           .append("the starting grid and the start/finish line?\n\n")
           .append("The rest of the lap is removed and any remaining corners are\n")
           .append("straightened. The best line is cleared too: build the track back\n")
           .append("up, then generate a new one.\n\n")
           .append("This cannot be undone. Continue?");
        p.question = Message.ask(Message.Kind.WARNING, MINIMAL_TITLE, ask.toString());
        return p;
    }

    /** Removes every best line sector but the first, as Remove All Best Line
        Segments always has (a track keeps one sector). */
    public static void clearBestLine(Track track)
    {
        CCLine line = track.getCCLine();
        int n = line.size();
        for (int x = 1; x < n; x++)
            line.deleteAt(2);
    }

    // ------------------------------------------------------------------
    // Check Track's pit lane fix

    public static final String PIT_MOUTHS_TITLE = "Pit Lane Entrance and Exit";

    public static Outcome setUpPitMouths(Track track)
    {
        if (!PitMouth.place(track))
            return new Outcome(false, Message.warning(PIT_MOUTHS_TITLE,
                "The editor could not find where the pit lane leaves and rejoins the track,\n"
              + "so nothing was changed."));
        track.calculateCCLine();
        return new Outcome(true, Message.info(PIT_MOUTHS_TITLE,
            "The pit lane's entrance and exit have been set up where the pit lane\n"
          + "leaves and rejoins the track, the way the original circuits do it.\n"
          + "The shape of the track and the pit lane is unchanged.\n\n"
          + "Save the track to keep the change."));
    }

    // ------------------------------------------------------------------
    // Best line generation: what has to be true first

    public static final String CANNOT_GENERATE_TITLE = "Cannot Generate Best Line";

    /** Why a best line cannot sensibly be generated for this track, or null
        when it can. Checked before generation, so it covers every method.

        Two cases, both of which produced a meaningless line rather than an
        error before: a track with no corners at all, where every line is
        equally fast and the generator returns an almost empty one; and a
        track whose ends do not face the same way, which the game cannot
        wrap from the last segment back to the first — the line is walked
        in world coordinates, so it needs the end to line up with the
        start. */
    public static Message bestLineBlocker(Track track)
    {
        TrackSegments segs = track.getTrackSegments();
        if (TrackHeadingClosure.allStraight(segs))
            return Message.error(CANNOT_GENERATE_TITLE,
                   "This track has no corners — every segment runs straight.\n\n"
                 + "A best line only earns anything through corners, so there is\n"
                 + "nothing here for it to work with. Build some corners first,\n"
                 + "then generate the line.");
        if (!TrackHeadingClosure.headingMatches(segs))
        {
            int nWinding = TrackHeadingClosure.netWinding(segs);
            int nTurns = TrackHeadingClosure.nearestWholeTurn(nWinding);
            int nOut = nTurns * TrackHeadingClosure.FULL_TURN - nWinding;
            double dDegrees = nOut * 360.0 / TrackHeadingClosure.FULL_TURN;
            return Message.error(CANNOT_GENERATE_TITLE,
                   "The last segment does not face the way the first one does —\n"
                 + "they are " + (Math.round(Math.abs(dDegrees) * 10) / 10.0)
                 + " degrees apart.\n\n"
                 + "A best line needs the two ends to line up. Run Tools > \"Add Segment\n"
                 + "to Match Start/End Heading\" first, then generate the line.");
        }
        return null;
    }

    /** A line generated on a track that crosses the map edge would be built
        wrongly by the game, so the track is moved clear first. Null when it
        did not need moving; otherwise an Outcome whose message is an error
        (generation must not go ahead) or the news that it was moved. */
    public static Outcome moveBeforeBestLine(Track track)
    {
        if (!TrackCoordinateEdge.shouldMove(track))
            return null;
        TrackCoordinateEdge.Result moved = TrackCoordinateEdge.moveToMiddle(track);
        if (moved.failure != null)
            return new Outcome(false, Message.error(CANNOT_GENERATE_TITLE, moved.failure));
        return new Outcome(true, Message.info("Generate Best Line", moved.open
              ? "This track crossed the edge of the game's map area at a corner, which\n"
              + "makes the game build its best line wrongly there. It has been moved so\n"
              + "that it crosses on a straight first. Nothing else about the track has\n"
              + "changed."
              : "This track ran over the edge of the game's map area, which makes\n"
              + "the game build its best line wrongly. It has been moved to the\n"
              + "middle of the map area first. Nothing else about the track has\n"
              + "changed."));
    }

    /** Puts a generated (or repaired) line in place of the whole best line. */
    public static void replaceBestLine(Track track, CCLine newLine)
    {
        CCLine current = track.getCCLine();
        current.clear();
        for (int i = 1; i <= newLine.size(); i++)
            current.add(newLine.getAt(i));
        track.calculateCCLine();
    }

    // ------------------------------------------------------------------
    // Warnings shown above the map

    /** A warning shown above the map, with the fix its button runs (a TrackCheck.ACTION_ code). */
    public static final class Banner
    {
        public final String text, fixLabel;
        public final int action;

        Banner(String text, String fixLabel, int action)
        {
            this.text = text;
            this.fixLabel = fixLabel;
            this.action = action;
        }
    }

    public static List<Banner> banners(Track track)
    {
        List<Banner> out = new ArrayList<Banner>();
        int ccTlu = track.getCCLineTlu();
        int trackTlu = track.getTrackTlu();
        // The game builds a wrong best line on a track that runs over the
        // edge of its coordinate space (see TrackCoordinateEdge).
        if (TrackCoordinateEdge.shouldMove(track))
            out.add(new Banner(TrackCoordinateEdge.isOpen(track)
                ? "This track crosses the edge of the game's map area near a corner, so the game's AI line will be wrong there."
                : "This track runs over the edge of the game's map area, so the game's AI line will be wrong.",
                "Move Track", TrackCheck.ACTION_MOVE_TRACK));
        if (ccTlu > 0 && ccTlu < trackTlu)
            out.add(new Banner("The best line stops " + (trackTlu - ccTlu) + " TLU short of the end of the track, so the AI has no line there.",
                "Generate Best Line", TrackCheck.ACTION_GENERATE_LINE));
        return out;
    }

    // ------------------------------------------------------------------
    // After saving

    /** The warning after a save when the game will freeze on this track, or
        null. Both limits are exact in the game, so this means the game WILL
        hang, not that it might.

        WHY A CHECK AT SAVING RATHER THAN A CAP. Only some of the ways a line
        can be produced pass through CCLineQuantizer.capCoachingEntries.
        Completion mode, Repair Best Line After Edit and hand-editing do not,
        and hand-editing never could. Saving is the one place they all meet,
        so a check there covers every route.

        WHY A WARNING RATHER THAN SILENT CORRECTION. Getting under either limit
        means taking something out of the user's track, which should not happen
        without asking. The save goes ahead either way. */
    public static Message saveWarning(Track track, long savedFileBytes)
    {
        if (track == null)
            return null;
        StringBuffer msg = new StringBuffer();

        if (track.getCCLine() != null)
        {
            int nEntries = track.getCCLine().getCoachingEntryCount();
            int nBudget = track.getCoachingEntryBudget();
            if (nEntries > nBudget)
            {
                int nPit = track.getPitCoachingEntryCount();
                msg.append("The best line uses ").append(nEntries)
                   .append(" entries of the game's coaching table, and the pit lane\n")
                   .append("uses ").append(nPit).append(". The table has 64 slots, so this track can have\n")
                   .append("at most ").append(nBudget).append(" in its best line.\n\n")
                   .append("The table holds one entry per corner of the racing line and\n")
                   .append("drives the suggested-gear display. Remove or straighten ")
                   .append(nEntries - nBudget).append(" curve(s)\n")
                   .append("in the best line, or regenerate it.\n\n");
            }
        }

        int nTrackSegs = track.getTrackSegCount();
        if (nTrackSegs > Track.MAX_TRACK_SEGS)
            msg.append("The track is ").append(nTrackSegs).append(" TLU long. The game allows at most ")
               .append(Track.MAX_TRACK_SEGS).append(",\n")
               .append("so it needs to lose ").append(nTrackSegs - Track.MAX_TRACK_SEGS)
               .append(" TLU. Shorten some straights or remove a corner.\n\n");

        int nPitSegs = track.getPitSegCount();
        if (nPitSegs > Track.MAX_PIT_SEGS)
            msg.append("The pit lane is ").append(nPitSegs).append(" TLU long. The game allows at most ")
               .append(Track.MAX_PIT_SEGS).append(",\n")
               .append("so it needs to lose ").append(nPitSegs - Track.MAX_PIT_SEGS)
               .append(" TLU. Shorten its straight sections.\n\n");

        if (savedFileBytes > Track.MAX_FILE_BYTES)
            msg.append("The file is ").append(savedFileBytes).append(" bytes. The game loads at most ")
               .append(Track.MAX_FILE_BYTES).append(",\n")
               .append("so it needs to shrink by ").append(savedFileBytes - Track.MAX_FILE_BYTES)
               .append(" bytes. Removing scenery objects or\n")
               .append("best line sectors is the easiest way.\n\n");

        if (msg.length() == 0)
            return null;
        msg.append("The track has been saved, but the game will freeze on the circuit\n")
           .append("preview screen if it is loaded as it is.");
        return Message.warning("This Track Will Not Load in the Game", msg.toString());
    }
}
