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

package cfevolution.screens;

import cfevolution.data.track.Track;
import cfevolution.edit.BestLineGeneration;
import cfevolution.edit.GameFolderOpening;
import cfevolution.edit.Message;
import cfevolution.edit.RandomTrackGeneration;
import cfevolution.edit.TrackCheck;
import cfevolution.edit.TrackMap;
import cfevolution.edit.TrackTools;
import cfevolution.generator.ccline.CCLineGenerationResult;
import cfevolution.generator.ccline.CCLineProgressListener;
import cfevolution.generator.ccline.geometric.FastestLapCCLineGenerator;
import cfevolution.generator.track.TrackProgressListener;
import java.io.File;
import java.util.List;

/**
 * One circuit's screen (2026-11-17): the map, its warnings, Check Track and
 * its fixes, the track tools, best line generation and random tracks, and
 * saving, for the first Android version. Every action is the desktop's own
 * (cfevolution.edit), and after each change the track is recalculated as the
 * desktop map does when it redraws, so each step starts from the state the
 * desktop editor would be in. Plain Java: runs on Android and the desktop JVM.
 *
 * The two generators take a while: call generateBestLine and generateRandom
 * on a worker thread, and everything else on the interface's own thread.
 */
public final class TrackScreen
{
    public final File file;
    public final Track track;
    private boolean modified;
    private CCLineGenerationResult pendingLine;
    private boolean randomConfirmed;

    private TrackScreen(File file, Track track)
    {
        this.file = file;
        this.track = track;
        TrackTools.recalculate(track);           // as the desktop map does when it first shows the track
    }

    /** Opens a track file; null after setting problem[0] if it will not load. */
    public static TrackScreen open(File file, Message[] problem)
    {
        Track t = new Track();
        if (file == null || !t.load(file))
        {
            problem[0] = GameFolderOpening.TRACK_NOT_LOADED;
            return null;
        }
        return new TrackScreen(file, t);
    }

    public String title()
    {
        return file.getName();
    }

    public boolean isModified()
    {
        return modified;
    }

    /** What to draw, including a generated line awaiting Apply. */
    public TrackMap map()
    {
        return TrackMap.of(track, pendingLine != null ? pendingLine.previewOffsets : null);
    }

    /** The warnings shown above the map, each with the fix its button runs (fix()). */
    public List<TrackTools.Banner> banners()
    {
        return TrackTools.banners(track);
    }

    /** Check Track's report: every check, with a verdict (TrackCheck.verdict). */
    public TrackCheck.Report check()
    {
        return TrackCheck.check(track);
    }

    private void changed()
    {
        TrackTools.recalculate(track);
        modified = true;
    }

    /** A fix from Check Track or a warning (TrackCheck.ACTION_*). Generating a
        line (ACTION_GENERATE_LINE) is prepareBestLine + generateBestLine instead. */
    public Flow fix(int action)
    {
        switch (action)
        {
            case TrackCheck.ACTION_MOVE_TRACK: return moveFromEdge();
            case TrackCheck.ACTION_MATCH_HEADING: return matchHeading();
            case TrackCheck.ACTION_PIT_MOUTHS: return pitMouths();
            default: return Flow.tell();
        }
    }

    public Flow moveFromEdge()
    {
        TrackTools.Outcome done = TrackTools.moveFromEdge(track);
        if (done.changed)
            changed();
        return Flow.tell(done.message);
    }

    public Flow pitMouths()
    {
        TrackTools.Outcome done = TrackTools.setUpPitMouths(track);
        if (done.changed)
            changed();
        return Flow.tell(done.message);
    }

    public Flow matchHeading()
    {
        return planned(TrackTools.planHeading(track));
    }

    public Flow reduceToMinimal()
    {
        return planned(TrackTools.planMinimal(track));
    }

    /** A tool that may ask first: failure or nothing-to-do, else ask, then do it and report. */
    private Flow planned(final TrackTools.Plan plan)
    {
        if (!plan.canApply())
            return Flow.tell(plan.failure != null ? plan.failure : plan.nothingToDo);
        return new Flow() {
            {
                show(plan.question);
            }
            private boolean asked = true;
            protected void next(Message m, int choice)
            {
                if (!asked || choice != YES)
                    return;
                asked = false;
                TrackTools.Outcome done = plan.apply();
                if (done.changed)
                    changed();
                show(done.message);
            }
        };
    }

    // --- Best line --------------------------------------------------------

    /**
     * Before generating: why it cannot be done (the flow ends with ready
     * false), or the news that the track was moved clear of the map edge
     * first. Generate only when ready() is true once the flow is done.
     */
    public static final class Preparation extends Flow
    {
        private boolean ready;

        public boolean ready()
        {
            return done() && ready;
        }

        protected void next(Message asked, int choice)
        {
        }
    }

    public Preparation prepareBestLine()
    {
        Preparation p = new Preparation();
        Message blocked = TrackTools.bestLineBlocker(track);
        if (blocked != null)
        {
            p.show(blocked);
            return p;
        }
        TrackTools.Outcome moved = TrackTools.moveBeforeBestLine(track);
        if (moved != null)
        {
            if (moved.changed)
            {
                changed();
                p.ready = true;
            }
            p.show(moved.message);
            return p;
        }
        p.ready = true;
        return p;
    }

    /** Generates a line with the dialog's starting settings (worker thread).
        Returns the status line; on success the line is shown on the map until
        applyBestLine or discardBestLine. complete keeps the existing sectors. */
    public String generateBestLine(boolean complete, CCLineProgressListener progress)
    {
        pendingLine = null;
        BestLineGeneration.Run run = BestLineGeneration.run(new FastestLapCCLineGenerator(),
            BestLineGeneration.settings(track, BestLineGeneration.DEFAULT_ITERATIONS,
                BestLineGeneration.DEFAULT_OVERSHOOT, BestLineGeneration.DEFAULT_STANDOFF_PERCENT, null),
            track, complete, progress);
        pendingLine = run.result;
        return BestLineGeneration.status(track, run);
    }

    public boolean hasPendingLine()
    {
        return pendingLine != null;
    }

    public void applyBestLine()
    {
        if (pendingLine == null)
            return;
        TrackTools.replaceBestLine(track, pendingLine.ccLine);
        pendingLine = null;
        changed();
    }

    public void discardBestLine()
    {
        pendingLine = null;
    }

    // --- Random track -----------------------------------------------------

    /** Asked once, before the first random track replaces this one. Generate
        only if the flow ends with a Yes (randomConfirmed()). */
    public Flow confirmRandom()
    {
        if (randomConfirmed)
            return Flow.tell();
        return new Flow() {
            {
                show(RandomTrackGeneration.CONFIRM);
            }
            protected void next(Message asked, int choice)
            {
                randomConfirmed = choice == YES;
            }
        };
    }

    public boolean randomConfirmed()
    {
        return randomConfirmed;
    }

    /** A finished random generation, to apply with applyRandom. */
    public static final class RandomRun
    {
        /** Set when there is nothing to apply: "Invalid parameter: ...", "Failed: ..." or "Cancelled.". */
        public final String problem;
        final RandomTrackGeneration.Result result;

        RandomRun(String problem, RandomTrackGeneration.Result result)
        {
            this.problem = problem;
            this.result = result;
        }
    }

    /**
     * Generates a random track from this circuit's saved file (worker thread),
     * as Generate &amp; Apply does; the open track is not touched until
     * applyRandom. The seed may be blank for a fresh one.
     */
    public RandomRun generateRandom(String seedText, String tluText, String cornersText, String elevationText,
                                    TrackProgressListener progress)
    {
        final long seed;
        final int tlu, corners;
        final double elevation;
        try
        {
            String s = seedText.trim();
            seed = s.length() == 0 ? RandomTrackGeneration.freshSeed() : Long.parseLong(s);
            tlu = Integer.parseInt(tluText.trim());
            corners = Integer.parseInt(cornersText.trim());
            elevation = Double.parseDouble(elevationText.trim());
            RandomTrackGeneration.checkSettings(elevation);
        }
        catch (NumberFormatException e)
        {
            return new RandomRun("Invalid parameter: " + e.getMessage(), null);
        }
        RandomTrackGeneration.Result r = RandomTrackGeneration.generate(file.getPath(), seed, tlu, corners, elevation, progress);
        if (r.error != null)
            return new RandomRun("Failed: " + r.error, null);
        if (r.cancelled())
            return new RandomRun("Cancelled.", null);
        return new RandomRun(null, r);
    }

    /** Puts a generated track in place of this one (the interface's own thread,
        as the desktop applies it); returns the status line. */
    public String applyRandom(RandomRun run)
    {
        if (run.problem != null)
            return run.problem;
        int trimmed = RandomTrackGeneration.apply(track, run.result.layout, run.result.line.ccLine);
        pendingLine = null;
        changed();
        return RandomTrackGeneration.status(run.result.layout, trimmed, track.getPitSegCount());
    }

    // --- Saving -----------------------------------------------------------

    /** Saves to the file it came from; returns the warning the desktop shows
        after saving when the game will not load it, or null. */
    public Message save()
    {
        track.save(file);
        modified = false;
        return TrackTools.saveWarning(track, file.length());
    }
}
