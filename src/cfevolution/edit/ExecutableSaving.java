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

import cfevolution.data.executable.Executable;
import cfevolution.data.executable.GameTables;

/**
 * Saving GP.EXE, without the interface (2026-11-16, moved out of
 * ExecutableWindow.save word for word).
 */
public final class ExecutableSaving
{
    private ExecutableSaving()
    {
    }

    /** Saves, unless something in it must not be written. Returns what to tell
        the user when it was not saved, or null when it was. */
    public static Message save(Executable exe)
    {
        // Never write a changed pit lane order that leaves a team without its own box. An invalid
        // order that came in the file and was not touched is written back as it was.
        GameTables tables = exe.getGameTables();
        if (tables.isPitOrderChanged() && !tables.isPitOrderValid())
            return Message.warning("Not Saved",
                "The executable was not saved: the pit lane order has a team missing or listed twice.\n"
              + "On the Pit Lane page, choose a team for each box or use Reset to 1991 Order.");
        // At least 26 cars in the first 18 teams, checked on saving so line-ups can be rearranged
        // freely (2026-11-22). A file that arrived with fewer is saved as it was if its line-ups were not touched.
        int entered = ExecutableEdits.enteredCars(exe.getGameTeams());
        if (exe.lineUpChanged() && entered < ExecutableEdits.MIN_ENTERED_CARS)
            return Message.warning("Not Saved",
                "The executable was not saved: the first " + ExecutableEdits.RACED_TEAMS + " teams have " + entered
              + " cars between them, and the game needs at least " + ExecutableEdits.MIN_ENTERED_CARS + " to fill a race.\n"
              + "On the Teams page, put more cars in those teams.");
        if (!exe.saveData())
            return Message.error("Save Failed", "The executable could not be saved.");
        return null;
    }

    /** Shown for Save As: an executable is saved where it was opened from. */
    public static final Message SAVE_AS = Message.info("Save Executable",
        "An executable is always saved back to the file it was opened from.\n"
      + "To keep the original, copy the file before opening it here.");
}
