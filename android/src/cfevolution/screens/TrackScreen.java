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
import cfevolution.data.track.TrackSegment;
import cfevolution.edit.BestLineRepair;
import cfevolution.edit.PropertySheet;
import cfevolution.edit.SegmentCommands;
import cfevolution.edit.TrackOutline;
import cfevolution.edit.BestLineGeneration;
import cfevolution.edit.GameFolderOpening;
import cfevolution.edit.Message;
import cfevolution.edit.CarSettingsForm;
import cfevolution.edit.RaceStrategyForm;
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
    private final BestLineRepair repair;
    private int pickedKind = TrackOutline.NOTHING, pickedNo = -1;

    private TrackScreen(File file, Track track)
    {
        this.file = file;
        this.track = track;
        this.repair = new BestLineRepair(track);
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

    /** The circuit's name when opened from a game folder ("Monaco"), else null. */
    public String circuit;

    /** The circuit's name, or the file's when it has none. */
    public String title()
    {
        return circuit != null ? circuit : file.getName();
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

    /** After a change to whole lists: nothing stays picked, and when the best
        line was replaced, Repair Best Line After Edit has nothing to go on. */
    private void replaced(boolean line)
    {
        changed();
        pick(TrackOutline.NOTHING, -1);
        if (line)
            repair.clear();
    }

    // --- The track's list, a part's properties, a segment's commands ---------

    /** Picks the headers (n ignored), a track, pit or best line segment (1-based), or NOTHING. */
    public void pick(int kind, int n)
    {
        boolean segment = kind == TrackOutline.TRACK || kind == TrackOutline.PIT || kind == TrackOutline.LINE;
        if (segment && (n < 1 || n > TrackOutline.count(track, kind)))
            kind = TrackOutline.NOTHING;
        pickedKind = kind;
        pickedNo = segment && kind != TrackOutline.NOTHING ? n : -1;
    }

    public int pickedKind()
    {
        return pickedKind;
    }

    public int pickedNo()
    {
        return pickedNo;
    }

    /** "Track segment 12", and under it "Turns left · 40 TLU". */
    public String heading()
    {
        return TrackOutline.heading(pickedKind, pickedNo);
    }

    public String hint()
    {
        return TrackOutline.hint(track, pickedKind, pickedNo);
    }

    /** A list's segments as the desktop names them ("12 Turn Left"). */
    public String[] list(int kind)
    {
        String[] out = new String[TrackOutline.count(track, kind)];
        for (int i = 0; i < out.length; i++)
            out[i] = TrackOutline.label(track, kind, i + 1);
        return out;
    }

    /** The picked part's properties to edit, or null when nothing is picked. */
    public PropertySheet properties()
    {
        return TrackOutline.sheet(track, pickedKind, pickedNo);
    }

    /**
     * OK on the properties: the refusal when a value is not a number (nothing
     * is written; sheet.problem() says the same), else writes them and returns
     * the notice about a best line length brought into range, or null.
     */
    public Message applyProperties(PropertySheet sheet)
    {
        Message problem = sheet.problem();
        if (problem != null)
            return problem;
        if (sheet.what == PropertySheet.LINE)
            repair.prepare(sheet.lineSegment());
        Message notice = sheet.apply();
        if (sheet.what == PropertySheet.LINE)
            repair.confirm(sheet.lineChanged());
        changed();
        return notice;
    }

    /** Adds a segment above or below the picked one, which stays picked. */
    public Flow addSegment(boolean above)
    {
        if (pickedNo < 1)
            return Flow.tell(TrackOutline.ADD_NOT_HERE);
        pickedNo = TrackOutline.insert(track, pickedKind, pickedNo, above);
        changed();
        return Flow.tell();
    }

    /** Deletes the picked segment; the next one is picked. */
    public Flow deleteSegment()
    {
        if (pickedNo < 1)
            return Flow.tell(TrackOutline.DELETE_NOT_HERE);
        Message refused = TrackOutline.delete(track, pickedKind, pickedNo);
        if (refused != null)
            return Flow.tell(refused);
        changed();
        pick(pickedKind, TrackOutline.afterDelete(track, pickedKind, pickedNo));
        return Flow.tell();
    }

    /** Edit > Remove All ... Segments: asks, then leaves only the first. */
    public Flow clearSegments(final int kind)
    {
        return new Flow() {
            {
                show(TrackOutline.clearQuestion(kind));
            }
            protected void next(Message asked, int choice)
            {
                if (choice != YES)
                    return;
                pick(TrackOutline.NOTHING, -1);
                TrackOutline.clear(track, kind);
                changed();
            }
        };
    }

    /** The picked track or pit segment, else null (only those have commands). */
    public TrackSegment pickedSegment()
    {
        return pickedKind == TrackOutline.TRACK || pickedKind == TrackOutline.PIT
            ? TrackOutline.segment(track, pickedKind, pickedNo) : null;
    }

    public String commandsText()
    {
        return SegmentCommands.countText(pickedSegment());
    }

    public String[] commands()
    {
        return SegmentCommands.list(pickedSegment());
    }

    /** "Parameters of Track markings", or that it has none. */
    public String commandHeading(int index)
    {
        return SegmentCommands.paramHeading(pickedSegment(), index);
    }

    /** A command's parameters, or null when it has none. */
    public PropertySheet commandProperties(int index)
    {
        return SegmentCommands.params(pickedSegment(), index);
    }

    /** OK on a command's parameters: the refusal, else null. */
    public Message applyCommand(PropertySheet sheet)
    {
        Message problem = sheet.problem();
        if (problem != null)
            return problem;
        sheet.apply();
        changed();
        return null;
    }

    /** Adds SegmentCommands.available()[choice] to the picked segment; returns where it is in the list. */
    public int addCommand(int choice)
    {
        int at = SegmentCommands.add(pickedSegment(), choice);
        changed();
        return at;
    }

    public void removeCommand(int index)
    {
        SegmentCommands.remove(pickedSegment(), index);
        changed();
    }

    // --- Edit > Horizon ---------------------------------------------------------

    /** The horizon to paint, in the colours of the GP.EXE beside the track; null when there is none. */
    public cfevolution.edit.HorizonPage horizon()
    {
        return cfevolution.edit.HorizonPage.of(track);
    }

    /** OK on the horizon: the picture into the track. */
    public void applyHorizon(cfevolution.edit.HorizonPage page)
    {
        if (page.ok())
            modified = true;
    }

    // --- Tools > Repair Best Line After Edit -----------------------------------

    public boolean canRepairLine()
    {
        return repair.available();
    }

    /** Refits the sectors after an edited best line segment onto the line as it was. */
    public Flow repairLine()
    {
        BestLineRepair.Result r = repair.repair();
        if (r == null)
            return Flow.tell();
        if (r.repaired != null)
        {
            TrackTools.replaceBestLine(track, r.repaired);
            replaced(true);
        }
        return Flow.tell(r.message);
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

    // --- Edit > Race Strategy, Edit > Car and AI Settings ----------------------

    /** The race settings as a form, or null for a file without them (RaceStrategyForm.NOT_AVAILABLE says so). */
    public RaceStrategyForm raceStrategy()
    {
        return RaceStrategyForm.of(track.getFooter());
    }

    /** OK with typed values; the message when refused, else null. */
    public Message applyRaceStrategy(RaceStrategyForm form, String[] typed)
    {
        boolean[] changed = new boolean[1];
        Message m = form.apply(typed, changed);
        if (changed[0])
            modified = true;
        return m;
    }

    public CarSettingsForm carSettings()
    {
        return new CarSettingsForm(track.getCCSetup());
    }

    /** OK with typed values, one per field (the tyre as A-D); the message when refused, else null. */
    public Message applyCarSettings(CarSettingsForm form, String[] typed)
    {
        boolean[] changed = new boolean[1];
        Message m = form.apply(typed, changed);
        if (changed[0])
            modified = true;
        return m;
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
            replaced(false);
        return Flow.tell(done.message);
    }

    public Flow matchHeading()
    {
        return planned(TrackTools.planHeading(track), false);
    }

    public Flow reduceToMinimal()
    {
        return planned(TrackTools.planMinimal(track), true);
    }

    /** A tool that may ask first: failure or nothing-to-do, else ask, then do it and report. */
    private Flow planned(final TrackTools.Plan plan, final boolean line)
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
                    replaced(line);         // Reduce to Minimal Track clears the best line
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
        replaced(true);
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
        replaced(true);
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
