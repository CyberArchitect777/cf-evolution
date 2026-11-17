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

import cfevolution.data.GameFolder;
import cfevolution.data.track.CCSetup;
import cfevolution.data.track.Track;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Game folder > Computer Cars at Every Circuit (moved out of the desktop
 * dialog 2026-11-17, word for word): the computer cars' cornering at every
 * circuit of a game folder, on one page (the job GPCSetup and GPCWinset did):
 * the three CC aggression words of each track's CC setup, which ArgData calls
 * LateBrakingFactor. The same values as Edit > Car and AI Settings, but for
 * all 16 tracks at once, with set and add for many at a time.
 *
 * Only the 38-byte CC setup of a changed track is written, in place, and the
 * checksum resealed (Track.saveCCSetupOnly). Nothing is saved while a changed
 * track is open in a tab, so the tab cannot later write its own copy over
 * these changes (the owner's choice, 2026-11-08).
 */
public final class AllCircuitsCars
{
    public static final String TITLE = "Computer Cars at Every Circuit";
    /** The three cornering words, in column order, with their column titles. */
    public static final int[] WORDS = { CCSetup.AGGRESSION_PRACTICE, CCSetup.AGGRESSION_RACE, CCSetup.AGGRESSION_WET };
    public static final String[] TITLES = { "Practice and qualifying (%)", "Race (%)", "Wet race (%)" };
    public static final String[] COLUMNS = { "Circuit", TITLES[0], TITLES[1], TITLES[2] };
    /** The range Edit > Car and AI Settings allows. */
    public static final int MIN = 50, MAX = 150;

    public static final String INTRO = "How hard the computer cars corner at each circuit, as in Edit > Car and AI Settings. "
        + "Higher means they brake later and take bends faster. The original circuits use 98 to 103.";
    public static final String[] WHICH = { "All three", "Practice and qualifying", "Race", "Wet race" };
    public static final String SET_TIP = "Gives the chosen values this percentage at every circuit (or the selected circuits)";
    public static final String ADD_TIP = "Adds this much to the chosen values at every circuit (or the selected circuits); use a minus number to lower them";
    public static final String UNDO = "Undo Changes";
    public static final String UNDO_TIP = "Puts back the values the circuits had when this page opened";
    public static final String BULK_NOTE = "Applies to the selected circuits, or to every circuit when none is selected.";
    public static final String WARNING = "Computer cars that corner harder crash and spin more often. If they do, raising the AI grip level "
        + "on the Game Settings page of GP.EXE helps them stay on the road.";
    public static final String BUTTON_TIP = "How hard the computer cars corner at each circuit, changed for all of them on one page";

    public static final Message NOTHING_CHANGED = Message.info("Nothing to Save", "Nothing has been changed.");
    /** Asked when closing with changes (Close Without Saving / Keep Editing; the desktop's default is Keep Editing). */
    public static final Message CLOSE_QUESTION = Message.choose(Message.Kind.WARNING, "Unsaved Changes",
        "Close without saving the changes?", "Close Without Saving", "Keep Editing");

    /** Whether a file is open, changed or not, somewhere it could be saved from (the desktop's tabs). */
    public interface OpenFiles
    {
        boolean isOpen(File file);
    }

    /** One circuit: its track, or why there is none, and the values shown. */
    private static final class Circuit
    {
        final int slot;
        final File file;
        final Track track;           // null if the file is missing or unreadable
        final String problem;
        final int[] loaded = new int[WORDS.length], shown = new int[WORDS.length];

        Circuit(int slot, File file)
        {
            this.slot = slot;
            this.file = file;
            Track t = null;
            String why = null;
            if (file == null)
                why = GameFolder.trackFileName(slot) + " is not in this folder";
            else
            {
                t = new Track();
                if (!t.load(file))
                {
                    t = null;
                    why = file.getName() + " could not be read";
                }
            }
            track = t;
            problem = why;
            for (int i = 0; i < WORDS.length; i++)
                loaded[i] = shown[i] = track != null ? track.getCCSetup().getPercent(WORDS[i]) : 0;
        }

        boolean changed()
        {
            return !Arrays.equals(shown, loaded);
        }
    }

    private final List<Circuit> circuits = new ArrayList<Circuit>();

    public AllCircuitsCars(GameFolder game)
    {
        for (int slot = 1; slot <= GameFolder.CIRCUITS; slot++)
            circuits.add(new Circuit(slot, game.getTrack(slot)));
    }

    public int rows()
    {
        return circuits.size();
    }

    /** What the table shows: column 0 the circuit, 1-3 the values (or why there are none). */
    public String cell(int row, int col)
    {
        Circuit c = circuits.get(row);
        if (col == 0)
            return c.slot + "  " + GameFolder.CIRCUIT_NAMES[c.slot - 1];
        if (c.track == null)
            return col == 1 ? c.problem : "";
        return String.valueOf(c.shown[col - 1]);
    }

    public boolean hasTrack(int row)
    {
        return circuits.get(row).track != null;
    }

    public boolean isEditable(int row, int col)
    {
        return col > 0 && hasTrack(row);
    }

    /** Whether a value differs from the one the page opened with (the desktop shows it blue). */
    public boolean isChanged(int row, int col)
    {
        Circuit c = circuits.get(row);
        return col > 0 && c.track != null && c.shown[col - 1] != c.loaded[col - 1];
    }

    /** Typed text for a value: kept within MIN..MAX; anything not a number leaves the old value. */
    public void set(int row, int col, String typed)
    {
        int v = Forms.number(typed);
        if (v != Integer.MIN_VALUE)
            circuits.get(row).shown[col - 1] = Math.max(MIN, Math.min(MAX, v));
    }

    /**
     * Set to (or add) a value at the given rows, or every circuit when rows is
     * empty; which: 0 all three, 1-3 one column (WHICH).
     */
    public void bulk(boolean set, int value, int which, int[] rows)
    {
        int[] sorted = rows.clone();
        Arrays.sort(sorted);
        for (int r = 0; r < circuits.size(); r++)
        {
            Circuit c = circuits.get(r);
            if (c.track == null || (sorted.length > 0 && Arrays.binarySearch(sorted, r) < 0))
                continue;
            for (int i = 0; i < WORDS.length; i++)
                if (which == 0 || which == i + 1)
                    c.shown[i] = Math.max(MIN, Math.min(MAX, set ? value : c.shown[i] + value));
        }
    }

    /** Puts back the values the page opened with. */
    public void undo()
    {
        for (Circuit c : circuits)
            System.arraycopy(c.loaded, 0, c.shown, 0, WORDS.length);
    }

    /** Whether any value is raised above what the page opened with (WARNING is shown). */
    public boolean raised()
    {
        for (Circuit c : circuits)
            for (int i = 0; i < WORDS.length; i++)
                if (c.track != null && c.shown[i] > c.loaded[i])
                    return true;
        return false;
    }

    public boolean hasChanges()
    {
        for (Circuit c : circuits)
            if (c.changed())
                return true;
        return false;
    }

    /** Writes the changed circuits, unless one of them is open; returns what to tell the user. */
    public Message save(OpenFiles open)
    {
        List<Circuit> changed = new ArrayList<Circuit>();
        List<String> opened = new ArrayList<String>();
        for (Circuit c : circuits)
            if (c.changed())
            {
                changed.add(c);
                if (open != null && open.isOpen(c.file))
                    opened.add(GameFolder.CIRCUIT_NAMES[c.slot - 1] + " (" + c.file.getName() + ")");
            }
        if (changed.isEmpty())
            return NOTHING_CHANGED;
        if (!opened.isEmpty())
            return Message.warning("Close the Circuit's Tab First", "Nothing was saved, because "
                + (opened.size() == 1 ? "this circuit is" : "these circuits are") + " open in a tab:\n\n" + String.join("\n", opened)
                + "\n\nClose " + (opened.size() == 1 ? "its tab" : "their tabs") + " and save again, so the tab cannot save its own copy over these changes.");
        List<String> failed = new ArrayList<String>();
        for (Circuit c : changed)
        {
            CCSetup setup = c.track.getCCSetup();
            for (int i = 0; i < WORDS.length; i++)
                if (c.shown[i] != c.loaded[i])
                    setup.setPercent(WORDS[i], c.shown[i]);
            if (c.track.saveCCSetupOnly())
                System.arraycopy(c.shown, 0, c.loaded, 0, WORDS.length);
            else
                failed.add(c.file.getName());
        }
        if (failed.isEmpty())
            return Message.info("Saved", "Saved " + changed.size() + (changed.size() == 1 ? " circuit." : " circuits."));
        return Message.error("Not Saved", "These files could not be saved:\n\n" + String.join("\n", failed));
    }
}
