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
import cfevolution.data.executable.GameOptions;
import cfevolution.data.executable.GameSettings;
import java.util.ArrayList;
import java.util.List;

/**
 * GP.EXE's Game Settings page (moved out of the desktop window 2026-11-17,
 * word for word): its groups, in order, each a list of numbers (label,
 * limits, hint), switches and notes. The desktop and Android pages are both
 * built from this, so they offer the same settings in the same words.
 */
public final class GameSettingsPage
{
    public static final String TITLE = "Game Settings";
    public static final String DESCRIPTION = "Changes to how the game behaves.";

    private GameSettingsPage()
    {
    }

    public enum Kind { NUMBER, SWITCH, NOTE }

    /** Reads and writes one stored value (a switch is 0 or 1). */
    public interface Value
    {
        int get();
        void set(int v);
    }

    /** One line of a group. */
    public static final class Item
    {
        public final Kind kind;
        /** The label; for a note, its text. */
        public final String label;
        /** For a number: its limits and the hint beside it. */
        public final int min, max;
        public final String hint;
        private final Value value;

        Item(Kind kind, String label, int min, int max, String hint, Value value)
        {
            this.kind = kind;
            this.label = label;
            this.min = min;
            this.max = max;
            this.hint = hint;
            this.value = value;
        }

        /** The stored value. */
        public int get()
        {
            return value.get();
        }

        /** The value shown: the stored one held within the limits. */
        public int shown()
        {
            return kind == Kind.NUMBER ? Math.max(min, Math.min(max, value.get())) : value.get();
        }

        public boolean isOn()
        {
            return value.get() != 0;
        }

        /** Stores a value; returns true if it changed. */
        public boolean set(int v)
        {
            if (v == value.get())
                return false;
            value.set(v);
            return true;
        }

        /** A number typed as text: refused with a message unless whole and within the limits; null when stored. */
        public Message set(String text)
        {
            Integer n = ExecutableEdits.number(text, min, max);
            if (n == null)
                return ExecutableEdits.numberProblem("This value", min, max);
            set(n.intValue());
            return null;
        }
    }

    public static final class Group
    {
        public final String title;
        public final List<Item> items = new ArrayList<Item>();

        Group(String title)
        {
            this.title = title;
        }

        Group number(String label, int min, int max, Value v, String hint)
        {
            items.add(new Item(Kind.NUMBER, label, min, max, hint, v));
            return this;
        }

        Group toggle(String label, Value v)
        {
            items.add(new Item(Kind.SWITCH, label, 0, 1, null, v));
            return this;
        }

        Group note(String text)
        {
            items.add(new Item(Kind.NOTE, text, 0, 0, null, null));
            return this;
        }
    }

    /** The page's groups for this GP.EXE, in order. */
    public static List<Group> groups(Executable exe)
    {
        final GameSettings s = exe.getGameSettings();
        final GameOptions o = exe.getGameOptions();
        List<Group> groups = new ArrayList<Group>();

        groups.add(new Group("Your car")
            .number("Engine power (BHP)", 1, 1460, new Value() {
                public int get() { return s.getPlayerPower(); }
                public void set(int v) { s.setPlayerPower(v); } },
                "The original game gives you 716."));

        Group computerCars = new Group("Computer cars");
        if (s.getAIGripLevel() >= 1)
            computerCars.number("Grip level", 1, 100, new Value() {
                public int get() { return s.getAIGripLevel(); }
                public void set(int v) { s.setAIGripLevel(v); } },
                "1 is the original game. Higher gives all cars more grip, yours too.");
        else
            computerCars.note("The grip setting in this file was changed by another tool, so it is left as it is.");
        computerCars.number("Team engine power measured against (BHP)", 100, 2000, new Value() {
            public int get() { return s.getTeamPowerReference(); }
            public void set(int v) { s.setTeamPowerReference(v); } },
            "The original game has 716. Raising it slows every team.");
        groups.add(computerCars);

        groups.add(new Group("Racing")
            .number("Computer cars make pit stops in races of at least (%)", 1, 100, new Value() {
                public int get() { return s.getPitStopRaceDistance(); }
                public void set(int v) { s.setPitStopRaceDistance(v); } },
                "The original game has 75. Shorter races have no pit stops.")
            .number("Most cars in the pits at once", 1, 26, new Value() {
                public int get() { return s.getMaxCarsPitting(); }
                public void set(int v) { s.setMaxCarsPitting(v); } },
                "The original game has 7. Team-mates never stop together.")
            .number("Shortest race (laps)", 1, 50, new Value() {
                public int get() { return s.getMinimumRaceLaps(); }
                public void set(int v) { s.setMinimumRaceLaps(v); } },
                "The original game has 3.")
            .number("Computer cars out at once in practice and qualifying", 1, 26, new Value() {
                public int get() { return s.getMaxPracticeRunners(); }
                public void set(int v) { s.setMaxPracticeRunners(v); } },
                "The original game has 16. The whole field is 26.")
            .toggle("Allow unlimited qualifying tyres", new Value() {
                public int get() { return s.getQualifyingTyres() ? 1 : 0; }
                public void set(int v) { s.setQualifyingTyres(v != 0); } }));

        groups.add(new Group("Championship")
            .number("Best results that count", 0, 16, new Value() {
                public int get() { return s.getBestResultsCounted(); }
                public void set(int v) { s.setBestResultsCounted(v); } },
                "The original game has 16, every race. 0 also counts every race."));

        groups.add(new Group("Weather")
            .number("Chance of rain (%)", 0, 100, new Value() {
                public int get() { return s.getRainChance(); }
                public void set(int v) { s.setRainChance(v); } },
                "The original game has 6. 0 means it never rains.")
            .toggle("Allow rain at circuits that are always dry (Phoenix)", new Value() {
                public int get() { return s.getRainEverywhere() ? 1 : 0; }
                public void set(int v) { s.setRainEverywhere(v != 0); } }));

        groups.add(new Group("Damage")
            .note("How hard a hit has to be. Higher numbers make the cars tougher.")
            .number("Retire after hitting a wall", 0, 32767, new Value() {
                public int get() { return s.getRetireAfterWall(); }
                public void set(int v) { s.setRetireAfterWall(v); } }, "Original: 7424")
            .number("Retire after hitting another car", 0, 32767, new Value() {
                public int get() { return s.getRetireAfterCar(); }
                public void set(int v) { s.setRetireAfterCar(v); } }, "Original: 8192")
            .number("Break a wing after hitting a wall", 0, 32767, new Value() {
                public int get() { return s.getDamageAfterWall(); }
                public void set(int v) { s.setDamageAfterWall(v); } }, "Original: 1792")
            .number("Break a wing after hitting another car", 0, 32767, new Value() {
                public int get() { return s.getDamageAfterCar(); }
                public void set(int v) { s.setDamageAfterCar(v); } }, "Original: 1792"));

        groups.add(new Group("Stopped and retired cars")
            .number("Yellow flags for a stopped car after (seconds)", 1, 60, new Value() {
                public int get() { return s.getYellowFlagSeconds(); }
                public void set(int v) { s.setYellowFlagSeconds(v); } }, "Original: 20")
            .number("Retired cars removed after (seconds)", 1, 60, new Value() {
                public int get() { return s.getRetiredRemovedSeconds(); }
                public void set(int v) { s.setRetiredRemovedSeconds(v); } }, "Original: 15"));

        groups.add(new Group("Starting the game")
            .toggle("Move through the menus faster", new Value() {
                public int get() { return o.getFastFade() ? 1 : 0; }
                public void set(int v) { o.setFastFade(v != 0); } })
            .toggle("Skip the language selection screen", new Value() {
                public int get() { return o.getLanguageSelection() ? 1 : 0; }
                public void set(int v) { o.setLanguageSelection(v != 0); } })
            .note("These two change the game program only. They are not part of a carset."));
        return groups;
    }
}
