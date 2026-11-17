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
import java.util.ArrayList;
import java.util.List;

/**
 * GP.EXE's pages of number grids, Car Physics and Opposition Levels (moved
 * out of the desktop window 2026-11-17, word for word): sections, each a grid
 * with row and column labels, limits, the original values, and the rules for
 * changing a value. The desktop and Android pages are both built from this.
 */
public final class TablePage
{
    public final String title, description;
    /** The text above the sections (the desktop adds how to see an original value). */
    public final String intro;
    public final List<Section> sections = new ArrayList<Section>();

    private TablePage(String title, String description, String intro)
    {
        this.title = title;
        this.description = description;
        this.intro = intro;
    }

    /** How the desktop shows the originals; Android shows them its own way. */
    public static final String DESKTOP_HINT = "Hover over a value to see the original; changed values are shown in bold.";

    public static final class Section
    {
        public final String title;
        /** May be null. */
        public final String note;
        public final Grid grid;

        Section(String title, String note, Grid grid)
        {
            this.title = title;
            this.note = note;
            this.grid = grid;
        }
    }

    /** Reads and writes the stored numbers; set returns why a value is refused, or null. */
    public interface Cells
    {
        int get(int row, int col);
        int original(int row, int col);
        Message set(int row, int col, int value);
    }

    public static final class Grid
    {
        public final String[] rows, cols;
        public final int min, max;
        private final Cells cells;

        Grid(String[] rows, String[] cols, int min, int max, Cells cells)
        {
            this.rows = rows;
            this.cols = cols;
            this.min = min;
            this.max = max;
            this.cells = cells;
        }

        public int get(int row, int col)
        {
            return cells.get(row, col);
        }

        public int original(int row, int col)
        {
            return cells.original(row, col);
        }

        public boolean isChanged(int row, int col)
        {
            return get(row, col) != original(row, col);
        }

        /** Why typed text is not a value for this grid, or null. */
        public Message check(String text)
        {
            return ExecutableEdits.number(text, min, max) == null ? ExecutableEdits.numberProblem("This value", min, max) : null;
        }

        /** Stores a value already checked; returns why it is refused, or null. */
        public Message put(int row, int col, int value)
        {
            return cells.set(row, col, value);
        }

        /**
         * Typed text: checked, then stored unless it is the value already
         * there. Returns why it was refused, or null. changed[0] (if given)
         * says whether anything was stored.
         */
        public Message set(int row, int col, String text, boolean[] changed)
        {
            if (changed != null)
                changed[0] = false;
            Message m = check(text);
            if (m != null)
                return m;
            int v = ExecutableEdits.number(text, min, max).intValue();
            if (v == get(row, col))
                return null;
            m = put(row, col, v);
            if (m == null && changed != null)
                changed[0] = true;
            return m;
        }
    }

    private void section(String title, String note, Grid grid)
    {
        sections.add(new Section(title, note, grid));
    }

    private static Grid grid(String[] rows, String[] cols, int min, int max, Cells cells)
    {
        return new Grid(rows, cols, min, max, cells);
    }

    /** Cells straight from a table, row-major, as unsigned or signed words. */
    private static Cells words(final GameTables.Table t, final int cols, final boolean signed)
    {
        return new Cells() {
            int fix(int v) { return signed ? (short) v : v; }
            public int get(int r, int c) { return fix(t.word(r * cols + c)); }
            public int original(int r, int c) { return fix(t.originalWord(r * cols + c)); }
            public Message set(int r, int c, int v) { t.setWord(r * cols + c, v); return null; }
        };
    }

    /** One table per row. */
    private static Cells perRow(final GameTables.Table[] rows)
    {
        return new Cells() {
            public int get(int r, int c) { return rows[r].word(c); }
            public int original(int r, int c) { return rows[r].originalWord(c); }
            public Message set(int r, int c, int v) { rows[r].setWord(c, v); return null; }
        };
    }

    public static final String[] LEVELS = { "Rookie", "Amateur", "Semi-Pro", "Pro", "Ace" };
    public static final String[] COMPOUNDS = { "A", "B", "C", "D", "Q", "W" };

    public static final Message WEIGHT_ZERO = Message.warning(ExecutableEdits.NOT_CHANGED,
        "The car weight cannot be 0: the game would crash.");

    /** The Car Physics page. */
    public static TablePage physics(Executable exe)
    {
        final GameTables t = exe.getGameTables();
        TablePage p = new TablePage("Car Physics", "The car's engine, tyres, weight and wings.",
            "These change how every car in the game behaves, yours included.");

        String[] rpm = new String[33];
        for (int i = 0; i < 33; i++)
            rpm[i] = String.valueOf(i * 512);
        p.section("Engine torque at each engine speed (rpm)", "The qualifying engine revs higher. Torque falls to 0 at the rev limit.",
            grid(new String[] { "Race engine", "Qualifying engine" }, rpm, 0, 65535,
                 perRow(new GameTables.Table[] { t.torqueRace, t.torqueQualifying })));
        String[] grip = { "1", "2", "3", "4", "5", "6", "7", "8" };
        p.section("Tyre grip, dry weather", "Eight values for each compound: base grip, then how it warms up, peaks and wears.",
            grid(COMPOUNDS, grip, -32768, 32767, words(t.tyreGripDry, 8, true)));
        p.section("Tyre grip, wet weather", null, grid(COMPOUNDS, grip, -32768, 32767, words(t.tyreGripWet, 8, true)));
        p.section("Lowest grip a worn tyre can fall to", null,
            grid(new String[] { "Grip floor" }, new String[] { "Dry", "Wet" }, 0, 65535, words(t.minimumGrip, 2, false)));
        p.section("Car weight and fuel (lb)", "The weight is stored twice and both copies change together. It cannot be 0, which crashes the game.",
            grid(new String[] { "Car weight", "Fuel" }, new String[] { "Value", "Second value" }, 0, 65535, new Cells() {
                public int get(int r, int c) { return r == 0 ? t.carWeight.word(c) : t.fuel.word(c); }
                public int original(int r, int c) { return r == 0 ? t.carWeight.originalWord(c) : t.fuel.originalWord(c); }
                public Message set(int r, int c, int v) {
                    if (r == 1) { t.fuel.setWord(c, v); return null; }
                    if (v == 0)
                        return WEIGHT_ZERO;
                    t.carWeight.setWord(0, v);
                    t.carWeight.setWord(1, v);
                    return null;
                }
            }));
        String[] wing = new String[9];
        for (int i = 0; i < 9; i++)
            wing[i] = String.valueOf(i * 8);
        p.section("Wings: drag and downforce at each wing setting", null,
            grid(new String[] { "Drag", "Downforce" }, wing, 0, 65535,
                 perRow(new GameTables.Table[] { t.wingDrag, t.wingDownforce })));
        final GameTables.Table[] other = { t.downforceFactor, t.engineBraking };
        p.section("Other car values", null,
            grid(new String[] { "Downforce factor", "Engine braking" }, new String[] { "Value" }, 0, 65535, new Cells() {
                public int get(int r, int c) { return other[r].word(0); }
                public int original(int r, int c) { return other[r].originalWord(0); }
                public Message set(int r, int c, int v) { other[r].setWord(0, v); return null; }
            }));
        String[] revCols = { "Change up", "Change down", "3", "4", "5", "6", "7", "8", "9", "10" };
        p.section("Gear changes (rpm)", "Automatic gear change points: up, down, then the automatic down-shift values.",
            grid(new String[] { "Engine speed" }, revCols, 0, 65535, words(t.revLimits, 10, false)));
        return p;
    }

    /** The Opposition Levels page. */
    public static TablePage opposition(Executable exe)
    {
        final GameTables t = exe.getGameTables();
        TablePage p = new TablePage("Opposition Levels", "The computer cars at Rookie, Amateur, Semi-Pro, Pro and Ace.",
            "How the computer cars differ between the opposition levels chosen in the game's options.");
        String[] rows = { "Random power loss", "Power lost per skill step", "Handling variation, lowest",
                          "Corner speed burst threshold", "Driver variation, lowest", "Driver variation, highest",
                          "Wing setting spread" };
        p.section("At each level", "At Ace a driver's skill costs no power at all.",
            grid(rows, LEVELS, 0, 65535, perRow(new GameTables.Table[] { t.oppPowerDeduction, t.oppRankPower, t.oppOversteerLow,
                t.oppCornerBurst, t.oppVariationLow, t.oppVariationHigh, t.oppWingVariation })));
        final GameTables.Table[] singles = { t.oppOversteerHigh, t.globalCornerSpeed, t.wetRacePower };
        p.section("At every level", null,
            grid(new String[] { "Handling variation, highest", "Corner speed from skill", "Computer car power in the wet" },
                 new String[] { "Value" }, 0, 65535, new Cells() {
                public int get(int r, int c) { return singles[r].word(0); }
                public int original(int r, int c) { return singles[r].originalWord(0); }
                public Message set(int r, int c, int v) { singles[r].setWord(0, v); return null; }
            }));
        return p;
    }
}
