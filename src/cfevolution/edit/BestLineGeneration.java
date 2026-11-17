package cfevolution.edit;

import cfevolution.data.track.CCLine;
import cfevolution.data.track.Track;
import cfevolution.generator.ccline.CCLineCompletion;
import cfevolution.generator.ccline.CCLineGenerationResult;
import cfevolution.generator.ccline.CCLineGenerator;
import cfevolution.generator.ccline.CCLineGeneratorContext;
import cfevolution.generator.ccline.CCLineProgressListener;
import cfevolution.generator.ccline.CCLineSimulator;
import cfevolution.generator.ccline.CCLineTrackGeometry;
import java.io.File;

/**
 * Generating a best line, without the interface (2026-11-16, moved out of
 * GenerateCCLineDialog word for word): the settings and their limits, the run
 * itself (replace the whole line, or complete the existing one), and what to
 * tell the user about the result. Applying it is TrackTools.replaceBestLine.
 *
 * Before generating, an interface checks TrackTools.bestLineBlocker and
 * TrackTools.moveBeforeBestLine, as the desktop editor does.
 */
public final class BestLineGeneration
{
    /** The dialog's starting values. */
    public static final int DEFAULT_ITERATIONS = 2000, DEFAULT_OVERSHOOT = 8, DEFAULT_STANDOFF_PERCENT = 15;

    /** Message when the "learned from original tracks" method has no folder. */
    public static final String NEED_TRAINING_FOLDER = "Please select the folder with original track files.";

    private BestLineGeneration()
    {
    }

    /**
     * The generator's settings for this track, checked as the dialog checks
     * them. Throws NumberFormatException with the reason (shown as
     * "Invalid parameter: " + reason) for a value out of range.
     */
    public static CCLineGeneratorContext settings(Track track, int iterations, int seamOvershoot,
                                                  int edgeStandoffPercent, File trainingFolder)
    {
        CCLineGeneratorContext context = new CCLineGeneratorContext(track);
        context.iterations = iterations;
        context.seamOvershoot = checkOvershoot(seamOvershoot);
        context.edgeStandoff = checkStandoff(edgeStandoffPercent) / 100.0;
        if (trainingFolder != null)
            context.trainingFolder = trainingFolder;
        return context;
    }

    /** Seam overshoot in TLU, 0-255; throws NumberFormatException otherwise. */
    public static int checkOvershoot(int n)
    {
        if (n < 0 || n > 255)
            throw new NumberFormatException("seam overshoot must be 0-255");
        return n;
    }

    /** Edge standoff in percent of the half-width, 5-60; throws NumberFormatException otherwise. */
    public static int checkStandoff(int n)
    {
        if (n < 5 || n > 60)
            throw new NumberFormatException("edge standoff must be 5-60 (%)");
        return n;
    }

    /** A finished run: the line (null if it failed or was cancelled) and the reason it failed. */
    public static final class Run
    {
        public final CCLineGenerationResult result;
        public final String error;

        Run(CCLineGenerationResult result, String error)
        {
            this.result = result;
            this.error = error;
        }

        public boolean cancelled()
        {
            return result == null && error == null;
        }
    }

    /**
     * Generates a line. With complete, the existing sectors are kept and only
     * the remaining lap is generated, targeting the method's line and rejoining
     * the kept line's own stamps at the wrap. Runs on the caller's thread: an
     * interface runs it on a worker and reports progress through the listener.
     */
    public static Run run(CCLineGenerator generator, CCLineGeneratorContext context, Track track,
                          boolean complete, CCLineProgressListener listener)
    {
        CCLineGenerationResult generated = null;
        String error = null;
        try
        {
            generated = generator.generate(context, listener);
            if (complete && generated != null)
            {
                CCLine completed = CCLineCompletion.complete(context.geometry, track.getCCLine(),
                    generated.score.simulation, context.seamOvershoot);
                if (completed == null)
                    error = "Nothing to complete: the existing line is empty"
                          + " or already covers the whole lap.";
                else
                    generated = new CCLineGenerationResult(completed, context.evaluator.score(completed));
            }
        }
        catch (Exception e)
        {
            error = e.getMessage();
        }
        return new Run(generated, error);
    }

    /** The status line after a run, plain language only (owner, 2026-10-11). The
        score, the out-of-bounds count and the generators' own diagnostics meant
        nothing to users; the one judgement that matters is whether the line
        stays on the road. */
    public static String status(Track track, Run run)
    {
        return status(track, run.result, run.error);
    }

    public static String status(Track track, CCLineGenerationResult result, String error)
    {
        if (error != null)
            return "Failed: " + error;
        if (result == null)
            return "Cancelled.";
        if (lineQualityWarning(track, result) != null)
            return "Done, but parts of this line run well off the road."
                 + " Check the preview on the map before applying.";
        return "Done. Check the preview on the map, then Apply or Discard.";
    }

    /** Warning text when the generated line behaves far outside anything
        the original tracks do, or null when it does not.

        On a track whose end does not return to its start, the game slides
        every Seg to close the gap, so the world path the generators
        optimise is a distorted version of the road and the line can come
        out badly. Nothing else catches it: `CCLineEvaluator` scores such
        a line valid — measured on a line straying 7.9x off the road.

        The test is on the line itself rather than on how open the track
        is, because the two turned out to be unrelated (Session 25,
        measured over all 16 reduced tracks: the worst line of the set
        came from one of the more closed tracks, while the two most open
        produced clean lines).

        Thresholds are set from what the originals actually do: their
        hand-tuned lines peak at 1.05x the physical road with kicks up to
        683, and lines generated on them peak at 1.27x with kicks up to
        922. Reduced tracks legitimately reach kicks of about 2,400, so
        the bar sits above that. */
    public static String lineQualityWarning(Track track, CCLineGenerationResult generated)
    {
        if (generated == null || generated.score == null || generated.score.simulation == null)
            return null;
        CCLineTrackGeometry geo = new CCLineTrackGeometry(track);
        CCLineSimulator.Result sim = generated.score.simulation;
        if (sim.ccLine.length < geo.segCount)
            return null;
        double dWorst = 0;
        int nMaxKick = 0;
        for (int i = 0; i < geo.segCount; i++)
        {
            if (geo.physicalBound[i] > 0)
                dWorst = Math.max(dWorst, Math.abs(sim.ccLine[i]) / geo.physicalBound[i]);
            int j = i + 1 >= geo.segCount ? 0 : i + 1;
            nMaxKick = Math.max(nMaxKick, Math.abs(sim.ccLine[j] - sim.ccLine[i]));
        }
        if (dWorst <= WORST_EXCURSION_WARNING && nMaxKick <= MAX_KICK_WARNING)
            return null;
        return "WARNING: the line runs to "
             + (Math.round(dWorst * 100) / 100.0) + "x the road half-width"
             + " with a biggest sideways step of " + nMaxKick
             + " (originals stay near 1.0x and under 1,000). The generator"
             + " optimises the road as the game compiles it, and on a track"
             + " whose end does not return to its start that shape is"
             + " distorted. Check the preview before applying.";
    }

    /** Excursion, as a multiple of the physical road half-width, past which
        a generated line is reported as suspect. */
    private static final double WORST_EXCURSION_WARNING = 2.0;

    /** Largest acceptable sideways step between adjacent Segs. */
    private static final int MAX_KICK_WARNING = 3000;
}
