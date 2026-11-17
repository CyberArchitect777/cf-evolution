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
import cfevolution.data.executable.GameDrivers;
import cfevolution.data.executable.GameTables;
import cfevolution.data.gamefiles.SetupFile;
import java.util.Arrays;

/**
 * GP.EXE's Setups page (moved out of the desktop window 2026-11-17, word for
 * word): the qualifying, race and Quick Race setups as the page shows them,
 * the rules for changing a value (the game's own, SetupFile.problem), the
 * separate setups switch, and copying a setup to other rows, to every row, or
 * between qualifying and race. The desktop and Android pages are both built
 * from this. (The game's setup files are still the desktop window's.)
 *
 * Only human drivers' cars read these; the computer cars read the circuit
 * file's CC setup (Car.bID bit 7 picks arCarSetups or strTrkCCSetup).
 * sub_4A9E copies the circuit's setup over all 40 at the start of a weekend
 * and of main-menu Practice, and the race setup at the warm-up when they are
 * separate (sub_4A8B). DoQuickRace does not copy, so a quick race run first in
 * a run of the game uses these (GP1E). Saved games carry them. Hrvoje,
 * 2026-11-02; the tools, 2026-11-03.
 */
public final class SetupsPage
{
    public static final String TITLE = "Setups";

    /** The description, a line each; the desktop shows them all, the last being about its own row selection. */
    public static final String[] DESCRIPTION = {
        "The setups the game gives human drivers' cars. The computer cars do not use these:",
        "theirs are in each circuit file (open a circuit, then Edit &gt; Car and AI Settings).",
        "With separate setups, the race setup is used from the warm-up on; otherwise the one setup is used all weekend.",
        "Quick Race setup: each car's setup in a quick race started before any other session since the game was loaded.",
        "A race weekend or practice replaces them with the circuit's setup until the game is restarted.",
        "If the game's options load a setup file when the game starts, that file's setups are used instead.",
        "Select several rows (Shift or Ctrl and click) to paste or load onto them all." };

    public static final String[] COLUMNS = { "Front wing", "Rear wing", "Gear 1", "Gear 2", "Gear 3", "Gear 4", "Gear 5", "Gear 6",
                                             "Tyres", "Brake balance" };

    /** Beside the desktop's setup file buttons. */
    public static final String SETUP_FILES = "the game's own setup files (Load-Save Menu, and the setup screen)";

    public static final int QUALIFYING = 0, RACE = 1, QUICK_RACE = 2;

    public static final String SEPARATE = "Separate race and qualifying setups";
    public static final String SEPARATE_TIP = "The game's own option, as it is when the game starts. When off, the one setup is used all weekend.";

    private final GameTables tables;
    private final GameDrivers drivers;

    public SetupsPage(GameTables tables, GameDrivers drivers)
    {
        this.tables = tables;
        this.drivers = drivers;
    }

    private GameTables.Table source(int tab)
    {
        return tab == QUICK_RACE ? tables.carSetups : tables.circuitSetups;
    }

    private static int firstRow(int tab)
    {
        return tab == RACE ? 16 : 0;
    }

    /** The tab's title, as it reads with or without separate setups. */
    public String tabTitle(int tab)
    {
        return tab == QUALIFYING ? (isSeparate() ? "Qualifying setup" : "Setup") : tab == RACE ? "Race setup" : "Quick Race setup";
    }

    /** The race tab is used only with separate setups. */
    public boolean isTabUsed(int tab)
    {
        return tab != RACE || isSeparate();
    }

    public int rows(int tab)
    {
        return tab == QUICK_RACE ? 40 : 16;
    }

    /** A row's label: the circuit, or the car number and its driver. */
    public String rowLabel(int tab, int r)
    {
        return tab == QUICK_RACE ? (r + 1) + "  " + drivers.getDriverName(r) : GameFolder.CIRCUIT_NAMES[r];
    }

    /** A row's name without its car number ("Brazil", "Carlos Sanchez"). */
    public String rowName(int tab, int r)
    {
        return rowLabel(tab, r).replaceAll("^[0-9]+ +", "");
    }

    private int at(int tab, int r, int c)
    {
        return (firstRow(tab) + r) * GameTables.SETUP_SIZE + c;
    }

    /** What the page shows for a value, c being 0-9 (COLUMNS). */
    public String value(int tab, int r, int c)
    {
        int v = source(tab).getByte(at(tab, r, c));
        if (c == SetupFile.TYRES)
            return v < SetupFile.COMPOUNDS.length ? SetupFile.COMPOUNDS[v] : String.valueOf(v);
        if (c == SetupFile.BRAKE_BALANCE)
            return String.valueOf((int) (byte) v);
        return String.valueOf(v);
    }

    /** Typed text for a value, c being 0-9: the game's own limits. Returns why it was refused, or null. */
    public Message set(int tab, int r, int c, String typed, boolean[] changed)
    {
        if (changed != null)
            changed[0] = false;
        int n;
        if (c == SetupFile.TYRES)
            n = Arrays.asList(SetupFile.COMPOUNDS).indexOf(String.valueOf(typed).trim().toUpperCase());
        else
        {
            try
            {
                n = Integer.parseInt(String.valueOf(typed).trim());
            }
            catch (NumberFormatException e)
            {
                n = Integer.MIN_VALUE;
            }
        }
        String problem = SetupFile.problem(read(tab, r), c, n);
        if (problem != null)
            return Message.warning(ExecutableEdits.NOT_CHANGED, problem);
        n &= 0xFF;
        if (n == source(tab).getByte(at(tab, r, c)))
            return null;
        source(tab).setByte(at(tab, r, c), n);
        if (changed != null)
            changed[0] = true;
        return null;
    }

    public boolean isSeparate()
    {
        return tables.separateSetups.word(0) != 0;
    }

    public void setSeparate(boolean on)
    {
        tables.separateSetups.setWord(0, on ? 0xFFFF : 0);
    }

    /** A row's whole setup. */
    public int[] read(int tab, int r)
    {
        int[] setup = new int[GameTables.SETUP_SIZE];
        for (int i = 0; i < setup.length; i++)
            setup[i] = source(tab).getByte((firstRow(tab) + r) * GameTables.SETUP_SIZE + i);
        return setup;
    }

    /** Gives each of these rows the setup. */
    public void write(int tab, int[] rows, int[] setup)
    {
        for (int r : rows)
            for (int i = 0; i < setup.length; i++)
                source(tab).setByte((firstRow(tab) + r) * GameTables.SETUP_SIZE + i, setup[i]);
    }

    /** What the page says after Copy. */
    public String copiedText(int tab, int r)
    {
        return "Copied: " + tabTitle(tab).toLowerCase() + ", " + rowName(tab, r);
    }

    /** Apply to All's label on a tab. */
    public static String applyAllLabel(int tab)
    {
        return tab == QUICK_RACE ? "Apply to All Cars" : "Apply to All Circuits";
    }

    /** Said by Apply to All with no row chosen. */
    public static Message chooseRowFirst(int tab)
    {
        return Message.info(applyAllLabel(tab), "Select the row whose setup the others should have.");
    }

    /** Gives every row of the tab row r's setup. */
    public void applyToAll(int tab, int r)
    {
        int[] all = new int[rows(tab)];
        for (int i = 0; i < all.length; i++)
            all[i] = i;
        write(tab, all, read(tab, r));
    }

    public static final String TO_RACE = "Copy Qualifying to Race", TO_QUALIFYING = "Copy Race to Qualifying";

    /** Asked before copying every circuit's setup between sessions (OK / Cancel). */
    public static Message betweenQuestion(boolean toRace)
    {
        return Message.choose(Message.Kind.QUESTION, toRace ? TO_RACE : TO_QUALIFYING,
            "Replace all 16 " + (toRace ? "race" : "qualifying") + " setups with the " + (toRace ? "qualifying" : "race") + " setups?",
            "OK", "Cancel");
    }

    /** Gives every circuit's race setup its qualifying setup (or the other way). */
    public void copyBetween(boolean toRace)
    {
        for (int c = 0; c < 16; c++)
            write(toRace ? RACE : QUALIFYING, new int[] { c }, read(toRace ? QUALIFYING : RACE, c));
    }
}
