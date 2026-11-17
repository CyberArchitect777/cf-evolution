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
import cfevolution.data.gamefiles.PreferencesFile;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * F1PREFS.DAT, the options the game saves with Save Options and reads at
 * start-up, from the desktop's PreferencesTab word for word. Meanings are
 * from our IDA names and the F1GP-SDL project's reading (2026-10-25). Each
 * value is written only when it is changed; the on/off options are read as on
 * when bit 7 is set and written as FFh or 0, as the game's own menus write
 * them. A value the editor does not know is shown as "Other" and kept unless
 * changed.
 */
public final class PreferencesPage extends GameFilePage
{
    /** What an item is. */
    public static final int SECTION = 0, CHOICE = 1, NUMBER = 2, TOGGLE = 3, AID = 4, FRAME_RATE = 5, AUTO_LOAD = 6, NOTE = 7;

    static final String[] LEVELS = { "Rookie", "Amateur", "Semi-Pro", "Pro", "Ace" };
    static final String[] SPREADS = { "All the same", "1991 levels", "Random" };
    static final String[] AIDS = { "Automatic brakes (F1)", "Automatic gears (F2)", "Spin recovery (F3)",
                                   "Indestructible (F4)", "Ideal line (F5)", "Suggested gear (F6)" };
    static final int AIDS_AT = 1144, KPH_AT = 1020, TEXTURES_AT = 1022;
    static final int FASTEST = 6, SLOWEST = PreferencesFile.MENU_SLOWEST_PERIOD;

    public static final String FRAME_RATE_LABEL = "Frame rate", FRAME_RATE_HINT = "Faster is smoother if the PC can keep up.";
    public static final String FRAME_RATE_WARNING = "Faster than the game's own Options menu allows. The car may spin "
        + "away at the start of a race; if it does, choose a slower rate.";
    public static final String LOAD = "Load", CHOOSE = "Choose...";

    /** One line of the page. */
    public static final class Item
    {
        public final int kind;
        public final String label, hint;
        /** A choice's items, made when the page opens ("Other (n)" added for a value the editor does not know). */
        public final List<String> items;
        public final int min, max;
        final int at, bit, pathAt;
        final boolean word;

        Item(int kind, String label, String hint, List<String> items, int at, boolean word, int min, int max, int bit, int pathAt)
        {
            this.kind = kind;
            this.label = label;
            this.hint = hint;
            this.items = items == null ? null : Collections.unmodifiableList(items);
            this.at = at;
            this.word = word;
            this.min = min;
            this.max = max;
            this.bit = bit;
            this.pathAt = pathAt;
        }
    }

    public final PreferencesFile prefs;
    private final File file;
    private final List<Item> items = new ArrayList<Item>();

    public PreferencesPage(PreferencesFile prefs, File file)
    {
        super(prefs);
        this.prefs = prefs;
        this.file = file;
        section("Races");
        choice("Opposition standard", LEVELS, PreferencesFile.OPPOSITION_STANDARD, true, null);
        choice("Opposition spread", SPREADS, PreferencesFile.OPPOSITION_SPREAD, true, "1991 levels keeps each team's own engine power.");
        number("Race distance (%)", PreferencesFile.RACE_DISTANCE, true, 5, 100, "5% is the shortest: 3 laps.");
        number("Qualifying session (minutes)", PreferencesFile.QUALIFYING_MINUTES, true, 1, 120, null);
        number("Practice sessions (minutes)", PreferencesFile.PRACTICE_MINUTES, true, 1, 120, null);

        section("Driving aids");
        for (int bit = 0; bit < AIDS.length; bit++)
            items.add(new Item(AID, AIDS[bit], null, null, AIDS_AT, true, 0, 0, bit, 0));
        note("The opposition standard can switch some aids off whatever is set here.");

        section("Display");
        toggle("Speeds in km/h", KPH_AT, true);
        toggle("Textures", TEXTURES_AT, false);
        List<String> rates = new ArrayList<String>();
        for (int p = FASTEST; p <= SLOWEST; p++)
            rates.add(fps(p) + " fps" + (p == PreferencesFile.MENU_FASTEST_PERIOD ? " (the game's fastest)"
                                        : p == SLOWEST ? " (the game's slowest)"
                                        : p < PreferencesFile.MENU_FASTEST_PERIOD ? " (faster than the game offers)" : ""));
        int v = prefs.getWord(PreferencesFile.FRAME_PERIOD);
        if (v < FASTEST || v > SLOWEST)
            rates.add("Other (stored as " + v + ")");   // kept unless changed
        items.add(new Item(FRAME_RATE, FRAME_RATE_LABEL, FRAME_RATE_HINT, rates, PreferencesFile.FRAME_PERIOD, true, 0, 0, 0, 0));

        section("Quick race");
        choice("Circuit", GameFolder.CIRCUIT_NAMES, PreferencesFile.QUICK_RACE_CIRCUIT, false, null);
        number("Driver (car number)", PreferencesFile.QUICK_RACE_DRIVER, false, 1, 40, null);
        number("Race distance (%)", PreferencesFile.QUICK_RACE_DISTANCE, false, 5, 100, null);
        toggle("Wet race", PreferencesFile.QUICK_RACE_WET, false);

        section("Files loaded when the game starts");
        autoLoad("Name file", PreferencesFile.AUTO_LOAD_NAMES, PreferencesFile.NAME_FILE_PATH);
        autoLoad("Setup file", PreferencesFile.AUTO_LOAD_SETUP, PreferencesFile.SETUP_FILE_PATH);
        autoLoad("Lap records file", PreferencesFile.AUTO_LOAD_RECORDS, PreferencesFile.RECORDS_FILE_PATH);
        note("A file in the game folder, for example gpsaves\\MYNAMES.NAM.");
    }

    public String title() { return "Game Options"; }

    public String explanation() { return "The options the game keeps in F1PREFS.DAT and reads when it starts."; }

    public String status() { return "Game options  ·  read by the game when it starts"; }

    public List<Item> items()
    {
        return Collections.unmodifiableList(items);
    }

    /** The first item with this label. */
    public Item item(String label)
    {
        for (Item i : items)
            if (i.kind != SECTION && i.kind != NOTE && i.label.equals(label))
                return i;
        return null;
    }

    private void section(String title)
    {
        items.add(new Item(SECTION, title, null, null, 0, false, 0, 0, 0, 0));
    }

    private void note(String text)
    {
        items.add(new Item(NOTE, text, null, null, 0, false, 0, 0, 0, 0));
    }

    private void choice(String label, String[] names, int at, boolean word, String hint)
    {
        List<String> l = new ArrayList<String>();
        Collections.addAll(l, names);
        int v = get(at, word);
        if (v >= names.length)
            l.add("Other (" + v + ")");   // a value the editor does not know: kept unless changed
        items.add(new Item(CHOICE, label, hint, l, at, word, 0, names.length - 1, 0, 0));
    }

    private void number(String label, int at, boolean word, int min, int max, String hint)
    {
        int v = get(at, word);
        items.add(new Item(NUMBER, label, v < min || v > max ? "Stored as " + v + "; kept unless changed. " + (hint == null ? "" : hint) : hint,
                           null, at, word, min, max, 0, 0));
    }

    private void toggle(String label, int at, boolean word)
    {
        items.add(new Item(TOGGLE, label, null, null, at, word, 0, 0, 0, 0));
    }

    private void autoLoad(String label, int flagAt, int pathAt)
    {
        items.add(new Item(AUTO_LOAD, label, null, null, flagAt, false, 0, 0, 0, pathAt));
    }

    private int get(int at, boolean word)
    {
        return word ? prefs.getWord(at) : prefs.getByte(at);
    }

    private void put(int at, boolean word, int value)
    {
        changed = get(at, word) != value;
        if (!changed)
            return;
        if (word) prefs.setWord(at, value); else prefs.setByte(at, value);
    }

    // --- values ----------------------------------------------------------------

    /** A choice's (or the frame rate's) chosen item. */
    public int chosen(Item i)
    {
        int v = get(i.at, i.word);
        if (i.kind == FRAME_RATE)
            return v >= FASTEST && v <= SLOWEST ? v - FASTEST : i.items.size() - 1;
        return v <= i.max ? v : i.items.size() - 1;
    }

    /** Picks an item; picking "Other" changes nothing. */
    public void choose(Item i, int index)
    {
        changed = false;
        if (i.kind == FRAME_RATE)
        {
            if (index <= SLOWEST - FASTEST)
                put(i.at, true, FASTEST + index);
        }
        else if (index <= i.max)
            put(i.at, i.word, index);
    }

    /** Shown under the frame rate when it is faster than the game's own menu allows. */
    public boolean frameRateWarning()
    {
        int v = prefs.getWord(PreferencesFile.FRAME_PERIOD);
        return v >= FASTEST && v < PreferencesFile.MENU_FASTEST_PERIOD;
    }

    /** A number as its box shows it (a stored value outside the box's range shows as the nearest it allows). */
    public int number(Item i)
    {
        return Math.max(i.min, Math.min(i.max, get(i.at, i.word)));
    }

    /** A number from the desktop's box, which only holds values in range. */
    public void setNumber(Item i, int value)
    {
        put(i.at, i.word, value);
    }

    /** A typed number (a phone has no spinner): the refusal, or null. */
    public Message setNumber(Item i, String typed)
    {
        changed = false;
        Message[] problem = new Message[1];
        Integer n = number(typed, i.min, i.max, i.label.replace(" (%)", ""), problem);
        if (n == null)
            return problem[0];
        put(i.at, i.word, n);
        return null;
    }

    /** An on/off option, a driving aid, or an automatic load's Load box. */
    public boolean on(Item i)
    {
        switch (i.kind)
        {
            case AID: return (prefs.getWord(AIDS_AT) & (1 << i.bit)) != 0;
            case AUTO_LOAD: return prefs.getByte(i.at) == 0xFF;
            default: return i.word ? (prefs.getWord(i.at) & 0x8000) != 0 : (prefs.getByte(i.at) & 0x80) != 0;
        }
    }

    public void setOn(Item i, boolean value)
    {
        switch (i.kind)
        {
            case AID:
            {
                int v = prefs.getWord(AIDS_AT);
                put(AIDS_AT, true, value ? v | (1 << i.bit) : v & ~(1 << i.bit));
                break;
            }
            case AUTO_LOAD: put(i.at, false, value ? 0xFF : 0); break;
            default: put(i.at, i.word, value ? (i.word ? 0xFFFF : 0xFF) : 0);
        }
    }

    /** An automatic load's path, as the game reads it ("gpsaves\MYNAMES.NAM"). */
    public String path(Item i)
    {
        return prefs.getText(i.pathAt, PreferencesFile.PATH_LENGTH);
    }

    /** A typed path: the refusal (the box then shows the stored path again), or null. */
    public Message setPath(Item i, String typed)
    {
        changed = false;
        Message[] problem = new Message[1];
        String t = text(typed, PreferencesFile.PATH_LENGTH - 1, "The path", problem);
        if (t == null)
            return problem[0];
        if (!t.equals(path(i)))
        {
            prefs.setText(i.pathAt, PreferencesFile.PATH_LENGTH, t);
            changed = true;
        }
        return null;
    }

    /** Choose...: a file picked anywhere, as the path the game reads; the refusal when it is outside the game folder. */
    public Message choosePath(Item i, File chosen)
    {
        File game = file.getAbsoluteFile().getParentFile();
        String relative = relativePath(game, chosen);
        if (relative == null || relative.length() > PreferencesFile.PATH_LENGTH - 1)
        {
            changed = false;
            return Message.warning("File Not Chosen", "The game can only load a file inside its own folder, with a path of at most "
                + (PreferencesFile.PATH_LENGTH - 1) + " characters.");
        }
        return setPath(i, relative);
    }

    /** The title of Choose...'s file chooser. */
    public static String chooseTitle(Item i)
    {
        return "Choose the " + i.label.toLowerCase();
    }

    /** Frames per second for a stored period, to one decimal place when not whole. */
    public static String fps(int period)
    {
        return 300 % period == 0 ? String.valueOf(300 / period) : String.valueOf(Math.round(3000.0 / period) / 10.0);
    }

    /** The DOS path of a file relative to the game folder, or null if outside it. */
    public static String relativePath(File game, File file)
    {
        String base = game.getAbsolutePath() + File.separator, path = file.getAbsolutePath();
        if (!path.startsWith(base))
            return null;
        return path.substring(base.length()).replace(File.separatorChar, '\\').toUpperCase();
    }
}
