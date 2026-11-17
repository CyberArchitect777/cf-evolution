package cfevolution.edit;

import cfevolution.data.track.CCLine;
import cfevolution.data.track.Track;
import cfevolution.generator.ccline.CCLineGenerationResult;
import cfevolution.generator.ccline.CCLineGeneratorContext;
import cfevolution.generator.ccline.geometric.FastestLapCCLineGenerator;
import cfevolution.generator.track.RandomTrackGenerator;
import cfevolution.generator.track.TrackCameraSection;
import cfevolution.generator.track.TrackFileSize;
import cfevolution.generator.track.TrackProgressListener;
import java.io.File;

/**
 * Generate Random Track, without the interface (2026-11-16, moved out of
 * GenerateTrackDialog and TrackWindow.applyGeneratedTrack word for word).
 *
 * generate() runs on the caller's thread against scratch tracks reloaded
 * from the donor file, so the open track is untouched until apply(). An
 * interface runs it on a worker, reports progress through the listener, and
 * cancels through listener.isCancelled().
 */
public final class RandomTrackGeneration
{
    /** Whole generations to try before reporting a failure. A seed whose
        layout cannot be closed, whose line cannot be built, or whose line
        runs off the road is simply replaced by another; the status line
        shows the seed used. */
    public static final int MAX_GENERATIONS = 10;

    /** The dialog's starting values. */
    public static final int DEFAULT_TLU = 900, DEFAULT_CORNERS = 12, DEFAULT_ELEVATION = 25;

    /** 1 TLU = 16 feet. */
    private static final double METRES_PER_TLU = 4.8768;

    private static final java.util.Random seedRoller = new java.util.Random();

    private RandomTrackGeneration()
    {
    }

    /** A fresh seed, short enough to note down and type back in. */
    public static long freshSeed()
    {
        synchronized (seedRoller)
        {
            return nextSeed(seedRoller);
        }
    }

    private static long nextSeed(java.util.Random r)
    {
        return 1 + r.nextInt(999999);
    }

    /** The seeds tried for one request: the requested seed, then replacements
        drawn from a generator seeded with it. So a typed seed always leads to
        the same track (until 2026-11-16 the replacements came from the clock,
        and an unworkable seed gave a different track every time). */
    public static long[] seedsToTry(long lSeed)
    {
        java.util.Random r = new java.util.Random(lSeed);
        long[] seeds = new long[MAX_GENERATIONS];
        seeds[0] = lSeed;
        for (int i = 1; i < seeds.length; i++)
            seeds[i] = nextSeed(r);
        return seeds;
    }

    /** Asked once before the first generation replaces the open track. */
    public static final Message CONFIRM = Message.ask(Message.Kind.WARNING, "Generate Random Track",
        "This replaces ALL track segments and the best line of the open track.\n"
        + "To revert, close the track without saving. Continue?");

    /** Checks the typed values as the dialog does; throws NumberFormatException with the reason. */
    public static void checkSettings(double dMaxElevation)
    {
        if (dMaxElevation < 0)
            throw new NumberFormatException("elevation change must be 0 or more");
    }

    /** A finished generation: the layout and its line (both null if it failed
        or was cancelled), and the last failure's reason. */
    public static final class Result
    {
        public final RandomTrackGenerator.Result layout;
        public final CCLineGenerationResult line;
        public final String error;

        Result(RandomTrackGenerator.Result layout, CCLineGenerationResult line, String error)
        {
            this.layout = layout;
            this.line = line;
            this.error = error;
        }

        public boolean cancelled()
        {
            return error == null && (layout == null || line == null);
        }
    }

    /**
     * Generates a layout and its best line from the donor track file, trying
     * further seeds (seedsToTry) when one cannot be made to work.
     */
    public static Result generate(String donorFile, long lSeed, int nTargetTlu, int nCorners,
                                  double dMaxElevation, TrackProgressListener listener)
    {
        RandomTrackGenerator.Result layout = null;
        CCLineGenerationResult line = null;
        String error = null;
        long[] seeds = seedsToTry(lSeed);
        for (int nTry = 0; nTry < seeds.length && !listener.isCancelled(); nTry++)
        {
            long lTrySeed = seeds[nTry];
            error = null;
            try
            {
                // Scratch 1: closure iterations
                Track scratch = new Track();
                if (!scratch.load(new File(donorFile)))
                    throw new Exception("Could not reload " + donorFile + " as scratch track");
                layout = new RandomTrackGenerator(scratch)
                    .generate(lTrySeed, nTargetTlu, nCorners, dMaxElevation, listener);

                if (layout != null)
                {
                    // Scratch 2: carry the final layout, generate the best line on it
                    listener.progress(85, "Working out the best line...");
                    Track lineTrack = new Track();
                    lineTrack.load(new File(donorFile));
                    applySegments(lineTrack, layout);
                    lineTrack.setLayoutMode(false);
                    lineTrack.calculateTrackLayout();
                    CCLineGeneratorContext ctx = new CCLineGeneratorContext(lineTrack);
                    ctx.seamOvershoot = 8;
                    line = new FastestLapCCLineGenerator().generate(ctx, null);
                    if (!listener.isCancelled() && lineRunsOffRoad(lineTrack, line))
                        throw new Exception("the best line could not be kept on the road. Try another seed, length or number of corners.");
                }
                break; // success, or cancelled (layout null)
            }
            catch (Exception e)
            {
                // Retry silently with the next seed; only the last failure is reported.
                error = e.getMessage();
                layout = null;
                line = null;
            }
        }
        if (listener.isCancelled())
            return new Result(null, null, null); // reported as "Cancelled.", not as a failure
        return new Result(layout, line, error);
    }

    /** True when the best line, laid on the track it was made for, would make
        Check Track say it runs well off the road. Generation gives that up and
        tries the next seed rather than hand the user a track that needs
        attention (about 1 workable seed in 15 did, 2026-11-16). */
    public static boolean lineRunsOffRoad(Track lineTrack, CCLineGenerationResult line)
    {
        if (line == null || line.ccLine == null)
            return false;
        CCLine cur = lineTrack.getCCLine();
        cur.clear();
        for (int i = 1; i <= line.ccLine.size(); i++)
            cur.add(line.ccLine.getAt(i));
        lineTrack.calculateCCLine();
        return TrackCheck.lineRunsOffRoad(lineTrack);
    }

    /**
     * Puts a generated layout and its best line into the open track. Returns
     * how many scenery objects had to be dropped to keep the file under the
     * size the game loads (see TrackFileSize): 0 normally, -1 if the track
     * could not be brought under it.
     */
    public static int apply(Track track, RandomTrackGenerator.Result layout, CCLine newLine)
    {
        applySegments(track, layout);
        TrackCameraSection.emptyCameraAdjustments(track.getFooter());
        track.setLayoutMode(false);
        track.calculateTrackLayout();
        TrackTools.replaceBestLine(track, newLine);
        // Last, because the size depends on the best line as well as the layout.
        return TrackFileSize.trimSceneryToFit(track);
    }

    /** The status line once applied. Plain language only (owner, 2026-10-11):
        the closure gap, clearance and donor-command notes meant nothing to
        users. The seed stays, so a track can be made again. */
    public static String status(RandomTrackGenerator.Result layout, int nTrimmed, int nPitSegs)
    {
        StringBuffer status = new StringBuffer();
        status.append("Track generated from seed ").append(layout.seed).append(": a ")
              .append(Math.round(layout.totalTlu * METRES_PER_TLU / 100.0) / 10.0)
              .append(" km lap.");
        if (nTrimmed > 0)
            status.append(" Some scenery was left out so the game can load it.");
        else if (nTrimmed < 0)
            status.append(" Warning: this track is too large for the game to load.");
        // The fitter only ever shortens the donor's pit lane, so this needs a
        // donor already over the limit; no original is (Mexico 205).
        if (nPitSegs > Track.MAX_PIT_SEGS)
            status.append(" Warning: the pit lane is too long for the game to load.");
        return status.toString();
    }

    /** Replaces a track's segment list with the generated one, keeping the
        dummy terminator (stripped of donor commands), and fits the donor
        pit lane's length to the new connect distance. */
    public static void applySegments(Track track, RandomTrackGenerator.Result layout) {
        cfevolution.data.track.TrackSegments segs = track.getTrackSegments();

        cfevolution.data.track.TrackSegment dummy = segs.get(segs.size() - 1);
        dummy.setCommands(new java.util.Vector<cfevolution.data.track.Command>());
        segs.clear();
        // Copies: this runs twice on one layout (the best line is worked out on
        // a scratch track first), and PitMouth.place below splits and edits
        // sections in place. Sharing them cut the applied track short of the
        // one the line was made for, and wrecked the line (2026-10-25).
        for (int i = 0; i < layout.segments.size(); i++)
            segs.add(layout.segments.get(i).copy());
        segs.add(dummy);
        if (layout.moveStart) {
            track.getTrackDataHeader().setStartPos(0, layout.startPos[0]);
            track.getTrackDataHeader().setStartPos(1, layout.startPos[1]);
        }
        cfevolution.generator.pitlane.PitLaneFitter.adjustPitLength(
            track.getPitlaneSegments(), layout.pitDelta);
        // Generated laps wind right (interior on the right), so the pit
        // bulges left; donor pit curves mirror their own track's final
        // corner and would walk the pit off into the void here
        // The offset was measured before generation began: generation uses
        // this very track as its scratch pad, so measuring it here would read
        // a layout the donor never had.
        cfevolution.generator.pitlane.PitLaneFitter.neutralizeCurvature(
            track.getPitlaneSegments(), -1, layout.donorPitOffset);

        // The generated layout has its own hills; the pit lane still carries
        // the donor's. Height is a per-TLU pitch rate the game double-
        // integrates, so mismatched profiles leave the lane metres above or
        // below the road. Must run AFTER the new segments are installed.
        cfevolution.generator.pitlane.PitLaneFitter.followTrackHeights(
            track.getPitlaneSegments(), segs);

        // Now the lane's shape is final: put the pit lane objects, the pit
        // joins and the wall gaps where it leaves and rejoins the road, and the
        // header's pit side where the lane really goes (always the left here;
        // twelve donors said right, so the game opened the wrong wall).
        track.setLayoutMode(false);
        cfevolution.generator.pitlane.PitMouth.place(track);

        // The donor's scenery keeps its distance from the road, which can put
        // it on another part of the new layout (a tree over a generated
        // Adelaide's road, 2026-10-26).
        track.calculateTrackLayout();
        cfevolution.generator.track.SceneryClearance.removeObjectsOverRoad(track);
    }
}
