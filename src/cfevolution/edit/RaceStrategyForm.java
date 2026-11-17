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

import cfevolution.data.track.Footer;
import cfevolution.data.track.RaceStrategy;

/**
 * Edit > Race Strategy (moved out of the desktop dialog 2026-11-17, word for
 * word): laps in a full race, the first pit stop lap and the chance a computer
 * car plans a stop. Nothing changes until apply, and only the values the user
 * changed are written, so a value outside the limits (from another tool)
 * survives untouched.
 */
public final class RaceStrategyForm
{
    public static final String TITLE = "Race Strategy";
    public static final int LAPS = 0, FIRST_STOP = 1, CHANCE = 2;
    public static final String[] LABELS = { "Laps in a full race", "First pit stop on lap", "Chance a computer car plans a pit stop (%)" };
    public static final String[] HINTS = {
        "The original circuits have 44 to 81. Shorter race distances use a share of these.",
        "For a full race; shorter races stop at the same point in the race. The originals stop about a third to half way.",
        "Monaco, Imola and Adelaide have 0, so hardly any car stops. Only in dry races of at least "
        + "75% distance (the executable editor can change that)." };
    public static final int[] MIN = { 3, 1, 0 }, MAX = { 150, 149, 100 };

    public static final Message NOT_AVAILABLE = Message.warning(TITLE, "This track file does not have race settings to change.");

    private final Footer footer;
    private final int[] shown = new int[3];

    /** Null for a footer without the settings (NOT_AVAILABLE says so). */
    public static RaceStrategyForm of(Footer footer)
    {
        return RaceStrategy.isAvailable(footer) ? new RaceStrategyForm(footer) : null;
    }

    private RaceStrategyForm(Footer footer)
    {
        this.footer = footer;
        shown[LAPS] = clamp(LAPS, RaceStrategy.getRaceLaps(footer));
        shown[FIRST_STOP] = clamp(FIRST_STOP, RaceStrategy.getFirstStopLap(footer));
        shown[CHANCE] = RaceStrategy.getStopChancePercent(footer);
    }

    private static int clamp(int i, int v)
    {
        return Math.max(MIN[i], Math.min(MAX[i], v));
    }

    /** The value the form starts with. */
    public int shown(int i)
    {
        return shown[i];
    }

    public static Message stopTooLate(int laps)
    {
        return Message.warning(TITLE, "The first pit stop has to come before the last lap. Choose a lap below " + laps + ".");
    }

    /** OK with these values (each within MIN..MAX). Returns why it was refused, or null; changed[0] says whether anything changed. */
    public Message apply(int[] values, boolean[] changed)
    {
        changed[0] = false;
        if (values[FIRST_STOP] >= values[LAPS])
            return stopTooLate(values[LAPS]);
        if (values[LAPS] != shown[LAPS]) { RaceStrategy.setRaceLaps(footer, values[LAPS]); changed[0] = true; }
        if (values[FIRST_STOP] != shown[FIRST_STOP]) { RaceStrategy.setFirstStopLap(footer, values[FIRST_STOP]); changed[0] = true; }
        if (values[CHANCE] != shown[CHANCE]) { RaceStrategy.setStopChancePercent(footer, values[CHANCE]); changed[0] = true; }
        return null;
    }

    /** As apply, from typed text (Android): a value that is not a whole number within limits is refused. */
    public Message apply(String[] typed, boolean[] changed)
    {
        changed[0] = false;
        int[] v = new int[3];
        for (int i = 0; i < 3; i++)
        {
            v[i] = Forms.number(typed[i]);
            if (v[i] < MIN[i] || v[i] > MAX[i])
                return ExecutableEdits.numberProblem(LABELS[i].replace(" (%)", ""), MIN[i], MAX[i]);
        }
        return apply(v, changed);
    }
}
