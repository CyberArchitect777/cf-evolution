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

import cfevolution.data.gamefiles.SetupFile;
import cfevolution.data.track.CCSetup;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Edit > Car and AI Settings (moved out of the desktop dialog 2026-11-17, word
 * for word): the 38-byte CC setup block as groups of rows. Nothing changes
 * until apply, and only the values the user changed are written, so stored
 * values that are not whole percentages survive. Each hint gives the range the
 * 16 original circuits use.
 */
public final class CarSettingsForm
{
    public static final String TITLE = "Car and AI Settings";
    public static final int PERCENT = 0, WORD = 1, SETUP_BYTE = 2, TYRE = 3;
    public static final String[] TYRES = { "A", "B", "C", "D" };

    /** One value. */
    public static final class Field
    {
        public final int kind, index, min, max;
        /** The desktop names its box for the row label, and gears gear1..gear6. */
        public final String name;
        /** Index of the value in values(). */
        public final int at;

        Field(int kind, int index, int min, int max, String name, int at)
        {
            this.kind = kind; this.index = index; this.min = min; this.max = max; this.name = name; this.at = at;
        }
    }

    /** A labelled row of one value (or the six gears) and its hint. */
    public static final class Row
    {
        public final String label, hint;
        public final List<Field> fields = new ArrayList<Field>();

        Row(String label, String hint)
        {
            this.label = label;
            this.hint = hint;
        }
    }

    public static final class Group
    {
        public final String title;
        /** A note under the title, or null. */
        public final String note;
        public final List<Row> rows = new ArrayList<Row>();

        Group(String title, String note)
        {
            this.title = title;
            this.note = note;
        }
    }

    private final CCSetup setup;
    private final List<Group> groups = new ArrayList<Group>();
    private final List<Field> fields = new ArrayList<Field>();
    private int[] shown;

    public CarSettingsForm(CCSetup setup)
    {
        this.setup = setup;
        Group all = group("All cars at this circuit", null);
        row(all, "Tyre grip (%)", PERCENT, CCSetup.GRIP, 50, 150, "Originals: 100 to 102");
        row(all, "Engine power (%)", PERCENT, CCSetup.POWER, 10, 200, "Originals: 100 (Mexico 80)");
        row(all, "Air resistance (%)", PERCENT, CCSetup.AIR_RESISTANCE, 10, 200, "Originals: 100 (Mexico 80)");
        row(all, "Tyre wear in qualifying (%)", PERCENT, CCSetup.WEAR_QUALIFYING, 0, 399, "Originals: 73");
        row(all, "Tyre wear in practice and the race (%)", PERCENT, CCSetup.WEAR_OTHER, 0, 399, "Originals: 53 to 207");
        row(all, "Fuel load", WORD, CCSetup.FUEL, 1, 2000, "Originals: 306 to 396");

        Group clock = group("Lap times shown", "The game's clock runs this much faster than real time, so lap times look longer.");
        row(clock, "In practice and qualifying (%)", PERCENT, CCSetup.CLOCK_PRACTICE, 50, 200, "Originals: 100 to 113");
        row(clock, "In the race (%)", PERCENT, CCSetup.CLOCK_RACE, 50, 200, "Originals: 101 to 112");

        Group ai = group("Computer cars", null);
        row(ai, "Cornering in practice and qualifying (%)", PERCENT, CCSetup.AGGRESSION_PRACTICE, 50, 150, "Originals: 98 to 103");
        row(ai, "Cornering in the race (%)", PERCENT, CCSetup.AGGRESSION_RACE, 50, 150, "Originals: 98 to 102");
        row(ai, "Cornering in a wet race (%)", PERCENT, CCSetup.AGGRESSION_WET, 50, 150, "Originals: 98");
        row(ai, "Power at the easier opposition levels (%)", PERCENT, CCSetup.CC_POWER, 10, 200,
            "Originals: 49 to 84. Lower slows them more; Ace is not affected");
        row(ai, "Formation lap gap", WORD, CCSetup.FORMATION_GAP, 0, 4000, "Originals: 128 to 512");
        row(ai, "Braking threshold for bends", WORD, CCSetup.BRAKE_THRESHOLD, 0, 4000, "Originals: 256 to 640");

        Group car = group("Computer cars' setup", null);
        row(car, "Front wing", SETUP_BYTE, CCSetup.FRONT_WING, 0, SetupFile.WING_MAX, "Originals: 18 to 58");
        row(car, "Rear wing", SETUP_BYTE, CCSetup.REAR_WING, 0, SetupFile.WING_MAX, "Originals: 18 to 58");
        Row gears = new Row("Gears", "Originals: 23 to 66");
        // the limits of the game's own setup screen (Hrvoje, 2026-11-05; was 0-104)
        for (int g = 0; g < 6; g++)
            gears.fields.add(field(SETUP_BYTE, CCSetup.GEAR_1 + g, SetupFile.GEAR_MIN, SetupFile.GEAR_MAX, "gear" + (g + 1)));
        car.rows.add(gears);
        row(car, "Tyre compound", TYRE, CCSetup.TYRE, 0, 3, "Originals: C");

        shown = new int[fields.size()];
        for (Field f : fields)
            shown[f.at] = Math.max(f.min, Math.min(f.max, read(f)));
    }

    private Group group(String title, String note)
    {
        Group g = new Group(title, note);
        groups.add(g);
        return g;
    }

    private Field field(int kind, int index, int min, int max, String name)
    {
        Field f = new Field(kind, index, min, max, name, fields.size());
        fields.add(f);
        return f;
    }

    private void row(Group g, String label, int kind, int index, int min, int max, String hint)
    {
        Row r = new Row(label, hint);
        r.fields.add(field(kind, index, min, max, label));
        g.rows.add(r);
    }

    /** The groups in the desktop's order: the left column's two, then the right's two. */
    public List<Group> groups()
    {
        return groups;
    }

    public List<Field> fields()
    {
        return fields;
    }

    private int read(Field f)
    {
        switch (f.kind)
        {
            case PERCENT: return setup.getPercent(f.index);
            case WORD: return setup.getWord(f.index);
            case SETUP_BYTE: return setup.getByte(f.index) - CCSetup.SETUP_OFFSET;
            default: return setup.getByte(f.index) - CCSetup.TYRE_OFFSET;
        }
    }

    /** The value a field starts with (within its limits; the tyre as 0-3). */
    public int shown(Field f)
    {
        return shown[f.at];
    }

    /** As the form shows a value (the tyre as its letter). */
    public String text(Field f, int value)
    {
        return f.kind == TYRE ? TYRES[value] : String.valueOf(value);
    }

    /** OK with these values (one per field, within limits): writes the ones changed; returns whether any were. */
    public boolean apply(int[] values)
    {
        boolean changed = false;
        for (Field f : fields)
        {
            int v = values[f.at];
            if (v == shown[f.at])
                continue;
            switch (f.kind)
            {
                case PERCENT: setup.setPercent(f.index, v); break;
                case WORD: setup.setWord(f.index, v); break;
                case SETUP_BYTE: setup.setByte(f.index, v + CCSetup.SETUP_OFFSET); break;
                default: setup.setByte(f.index, v + CCSetup.TYRE_OFFSET); break;
            }
            changed = true;
        }
        shown = values.clone();
        return changed;
    }

    /** What a field is called in a refusal: its row, and the gear's number. */
    private String what(Field f)
    {
        for (Group g : groups)
            for (Row r : g.rows)
                if (r.fields.contains(f))
                    return (r.fields.size() > 1 ? "Gear " + (r.fields.indexOf(f) + 1) : r.label).replace(" (%)", "");
        return f.name;
    }

    /**
     * As apply, from typed text (Android), one per field: a value that is not
     * a whole number within limits (or a tyre letter A-D) is refused and
     * nothing is written. Returns why, or null; changed[0] says whether anything changed.
     */
    public Message apply(String[] typed, boolean[] changed)
    {
        changed[0] = false;
        int[] v = new int[fields.size()];
        for (Field f : fields)
        {
            String t = String.valueOf(typed[f.at]).trim();
            if (f.kind == TYRE)
            {
                v[f.at] = Arrays.asList(TYRES).indexOf(t.toUpperCase(java.util.Locale.ROOT));
                if (v[f.at] < 0)
                    return Message.warning(ExecutableEdits.NOT_CHANGED, "The tyre compound must be A, B, C or D.");
                continue;
            }
            v[f.at] = Forms.number(t);
            if (v[f.at] < f.min || v[f.at] > f.max)
                return ExecutableEdits.numberProblem(what(f), f.min, f.max);
        }
        changed[0] = apply(v);
        return null;
    }
}
