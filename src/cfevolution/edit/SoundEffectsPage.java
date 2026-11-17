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

import cfevolution.data.gamefiles.SoundEffectsFile;
import java.util.ArrayList;
import java.util.List;

/**
 * The AdLib and Sound Blaster sound effects (XSOUND.BIN), from the desktop's
 * SoundEffectsTab word for word (2026-11-17): each effect's instrument, and
 * the pitch, loudness and timing values the effects code reads. Only those
 * are offered, each written only when it is changed, so the file keeps its
 * size and its code. The ranges keep the code's arithmetic from overflowing.
 */
public final class SoundEffectsPage extends GameFilePage
{
    public static final String[] SOUNDS = { "Engine, tone 1", "Engine, tone 2", "Engine, tone 3", "Tyre squeal",
                                            "Passing car 1", "Passing car 2", "Passing car 3", "Passing car 4",
                                            "Crash", "Wheel gun", "Start signal" };
    static final int TYRES = 3, PASSING = 4, CRASH = 8, WHEEL_GUN = 9;
    public static final String[] WAVEFORMS = { "Sine", "Half sine", "Rectified sine", "Quarter sine" };
    public static final String[] COMBINE = { "The modulator shapes the carrier", "Both are heard" };
    public static final String REVERT = "Revert This Sound";
    public static final String HINT = "Changes are heard in the game with an AdLib or Sound Blaster card.";
    public static final String MODULATOR = "Modulator", CARRIER = "Carrier", SET_BY_GAME = "Set by the game";
    static final int MOD = 0, CAR = 1;

    /** What a row is. */
    public static final int SECTION = 0, NOTE = 1, LINE = 2, HEADINGS = 3;
    /** What a control is. */
    public static final int NUMBER = 0, CHOICE = 1, FLAG = 2;

    /** One value in the file: a word, or some bits of a byte (perhaps counted down from max). */
    public static final class Control
    {
        public final int kind;
        /** The desktop's name for the control. */
        public final String name;
        public final int min, max;
        public final String[] items;
        final int offset, mask;
        final boolean word, inverted;

        Control(int kind, String name, int offset, boolean word, int mask, int min, int max, boolean inverted, String[] items)
        {
            this.kind = kind;
            this.name = name;
            this.offset = offset;
            this.word = word;
            this.mask = mask;
            this.min = min;
            this.max = max;
            this.inverted = inverted;
            this.items = items;
        }
    }

    /** A line of the page: a heading, a note, the Modulator/Carrier headings, or a label with one or two controls and a hint. */
    public static final class Row
    {
        public final int kind;
        public final String label, hint;
        public final Control first, second;
        /** Shown in place of the second control ("Set by the game"), or null. */
        public final String fixed;

        Row(int kind, String label, Control first, Control second, String fixed, String hint)
        {
            this.kind = kind;
            this.label = label;
            this.first = first;
            this.second = second;
            this.fixed = fixed;
            this.hint = hint;
        }
    }

    public final SoundEffectsFile sounds;
    private final byte[] original;

    /** @param original the file's bytes as it was opened, for Revert This Sound */
    public SoundEffectsPage(SoundEffectsFile sounds, byte[] original)
    {
        super(sounds);
        this.sounds = sounds;
        this.original = original.clone();
    }

    public String title() { return "Sound Effects"; }

    public String explanation()
    {
        return "The game's sound effects for AdLib and Sound Blaster cards: each one's instrument, pitch, loudness and timing.";
    }

    public String status() { return "Sound effects  ·  the game reads them when it starts"; }

    // --- the page for a sound -----------------------------------------------------

    private List<Row> rows;
    private List<Integer> bytes;

    private void section(String t) { rows.add(new Row(SECTION, t, null, null, null, null)); }

    private void note(String t) { rows.add(new Row(NOTE, t, null, null, null, null)); }

    private Control control(int kind, String name, int offset, boolean word, int mask, int min, int max, boolean inverted, String[] items)
    {
        bytes.add(offset);
        if (word)
            bytes.add(offset + 1);
        return new Control(kind, name, offset, word, mask, min, max, inverted, items);
    }

    private void word(String label, int offset, int min, int max, String hint)
    {
        int v = sounds.getWord(offset);
        rows.add(new Row(LINE, label, control(NUMBER, label, offset, true, 0, min, max, false, null), null, null,
                         v < min || v > max ? "Stored as " + v + "; kept unless changed." : hint));
    }

    private void bits(String label, int offset, int mask, int min, int max, boolean inverted, String hint)
    {
        rows.add(new Row(LINE, label, control(NUMBER, label, offset, false, mask, min, max, inverted, null), null, null, hint));
    }

    private void pair(String label, int offset, int mask, int min, int max, boolean inverted, String hint)
    {
        rows.add(new Row(LINE, label, control(NUMBER, label + " modulator", offset + MOD, false, mask, min, max, inverted, null),
                         control(NUMBER, label + " carrier", offset + CAR, false, mask, min, max, inverted, null), null, hint));
    }

    private void flagPair(String label, int offset, int bit, String hint)
    {
        rows.add(new Row(LINE, label, control(FLAG, label + " modulator", offset + MOD, false, bit, 0, 1, false, null),
                         control(FLAG, label + " carrier", offset + CAR, false, bit, 0, 1, false, null), null, hint));
    }

    /**
     * The page for sound s (SOUNDS), from the file as it now stands: its own
     * values, then its instrument. Hints for values stored out of range are
     * fixed when the page is made, as on the desktop.
     */
    public List<Row> rows(int s)
    {
        rows = new ArrayList<Row>();
        bytes = new ArrayList<Integer>();
        int record;
        if (s < TYRES)
        {
            section("Engine");
            note("The engine is three tones played together, each following the revs.");
            word("Pitch against the revs", SoundEffectsFile.ENGINE_PITCH[s], 1, 12, "Higher is a higher tone.");
            word("Roughness", SoundEffectsFile.ENGINE_ROUGHNESS[s], 0, 1023, "How much the pitch wobbles at random.");
            word("Loudness off the throttle", SoundEffectsFile.ENGINE_LEVEL_OFF[s], 0, 63, "63 is the loudest.");
            word("Loudness on the throttle", SoundEffectsFile.ENGINE_LEVEL_ON[s], 0, 63, null);
            record = SoundEffectsFile.ENGINE_RECORDS[s];
        }
        else if (s == TYRES)
        {
            section("Tyre squeal");
            word("Pitch", SoundEffectsFile.TYRE_PITCH, 500, 12000, "Higher is a higher squeal.");
            String[] amounts = { "lightest", "light", "some", "medium", "heavy", "heavier", "heaviest" };
            for (int i = 0; i < SoundEffectsFile.TYRE_LEVEL.length; i++)
                bits("Loudness, " + amounts[i] + " sliding", SoundEffectsFile.TYRE_LEVEL[i], 0x3F, 0, 63, false, i == 0 ? "63 is the loudest." : null);
            record = SoundEffectsFile.TYRE_RECORD;
        }
        else if (s < CRASH)
        {
            int v = s - PASSING;
            section("Passing car");
            note("Up to four cars can be heard passing at once, each in one of these voices.");
            word("Pitch", SoundEffectsFile.PASSING_PITCH[v], 500, 12000, "Higher is a higher note. Faster cars sound higher.");
            bits("Greatest pitch drop", SoundEffectsFile.PASSING_MAX_DROP[v], 0xFF, 0, 200, false, "How far the note falls as the car goes by.");
            section("All four passing car voices");
            bits("Delay before the pitch drops", SoundEffectsFile.PASSING_DROP_DELAY, 0xFF, 2, 200, false, "In fiftieths of a second.");
            bits("Speed of the pitch drop", SoundEffectsFile.PASSING_DROP_RATE, 0xFF, 0, 50, false, null);
            record = SoundEffectsFile.PASSING_RECORDS[v];
        }
        else if (s == CRASH)
        {
            section("Crash");
            note("Played when the car you are watching hits another car or a wall.");
            int r = SoundEffectsFile.record(SoundEffectsFile.CRASH_RECORD);
            word("Crunch length", r + SoundEffectsFile.NOISE_LENGTH, 1, 500, "In fiftieths of a second.");
            word("Crunch pitch", r + SoundEffectsFile.NOISE_BASE, 256, 12000, "Higher is a higher crunch.");
            word("Crunch pitch spread", r + SoundEffectsFile.NOISE_SPREAD, 1, 4095, "How far the pitch jumps about.");
            record = SoundEffectsFile.CRASH_RECORD;
        }
        else
        {
            section(SOUNDS[s]);
            note(s == WHEEL_GUN ? "Played at a pit stop: four bursts, a pause, then four more. Its pitch is fixed by the game."
                                : "Played before the start lights. Its pitch is fixed by the game.");
            record = s == WHEEL_GUN ? SoundEffectsFile.WHEEL_GUN_RECORD : SoundEffectsFile.START_RECORD;
        }
        instrument(record);
        List<Row> out = rows;
        lastBytes = bytes;
        return out;
    }

    private List<Integer> lastBytes = new ArrayList<Integer>();

    private void instrument(int k)
    {
        int r = SoundEffectsFile.record(k);
        for (int i = 0; i < SoundEffectsFile.RECORD_SIZE; i++)
            bytes.add(r + i);
        section("Instrument");
        note("Two tones make each instrument: the carrier is what you hear, and the modulator changes its sound.");
        rows.add(new Row(LINE, "How the two combine",
                         control(CHOICE, "How the two combine", r + SoundEffectsFile.FEEDBACK, false, 0x01, 0, 1, true, COMBINE), null, null, null));
        bits("Feedback", r + SoundEffectsFile.FEEDBACK, 0x0E, 0, 7, false, "Higher is harsher.");
        rows.add(new Row(HEADINGS, null, null, null, null, null));
        pair("Frequency multiple", r + SoundEffectsFile.CHARACTER, 0x0F, 0, 15, false, "0 is half the note.");
        rows.add(new Row(LINE, "Volume", control(NUMBER, "Volume modulator", r + SoundEffectsFile.MODULATOR_LEVEL, false, 0x3F, 0, 63, false, null),
                         null, SET_BY_GAME, "63 is the loudest."));
        rows.add(new Row(LINE, "Quieter at high notes",
                         control(NUMBER, "Quieter at high notes modulator", r + SoundEffectsFile.MODULATOR_LEVEL, false, 0xC0, 0, 3, false, null),
                         null, null, "0 is not at all."));
        pair("Attack speed", r + SoundEffectsFile.ENVELOPE, 0xF0, 0, 15, false, "How fast it starts. 15 is the fastest.");
        pair("Decay speed", r + SoundEffectsFile.ENVELOPE, 0x0F, 0, 15, false, "How fast it falls to the held level.");
        pair("Held level", r + SoundEffectsFile.HOLD_RELEASE, 0xF0, 0, 15, true, "15 is the loudest.");
        pair("Release speed", r + SoundEffectsFile.HOLD_RELEASE, 0x0F, 0, 15, false, "How fast it dies away.");
        rows.add(new Row(LINE, "Waveform",
                         control(CHOICE, "Waveform modulator", r + SoundEffectsFile.WAVEFORM, false, 0x03, 0, 3, false, WAVEFORMS),
                         control(CHOICE, "Waveform carrier", r + SoundEffectsFile.WAVEFORM + 1, false, 0x03, 0, 3, false, WAVEFORMS), null, null));
        flagPair("Hold the note", r + SoundEffectsFile.CHARACTER, 0x20, "Otherwise it fades away while playing.");
        flagPair("Tremolo", r + SoundEffectsFile.CHARACTER, 0x80, "A wavering volume.");
        flagPair("Vibrato", r + SoundEffectsFile.CHARACTER, 0x40, "A wavering pitch.");
        flagPair("Shorter at high notes", r + SoundEffectsFile.CHARACTER, 0x10, null);
    }

    // --- values -----------------------------------------------------------------------

    private int stored(Control c)
    {
        int v = c.word ? sounds.getWord(c.offset) : sounds.getBits(c.offset, c.mask);
        return c.inverted ? c.max - v : v;
    }

    /** What a control shows: a number within its range (a stored value outside it shows as the nearest end),
        a choice's index, or 1 or 0 for a tick box. */
    public int value(Control c)
    {
        int v = stored(c);
        if (c.kind == FLAG)
            return v != 0 ? 1 : 0;
        return c.kind == NUMBER ? Math.max(c.min, Math.min(c.max, v)) : v;
    }

    /** A new value from a control that holds only values in range (a tick box: 1 or 0); written only when it differs. */
    public void set(Control c, int value)
    {
        changed = false;
        if (c.kind == FLAG)
        {
            if ((sounds.getBits(c.offset, c.mask) != 0 ? 1 : 0) == value)
                return;
            sounds.setBits(c.offset, c.mask, value);
            changed = true;
            return;
        }
        if (stored(c) == value)
            return;
        int v = c.inverted ? c.max - value : value;
        if (c.word)
            sounds.setWord(c.offset, v);
        else
            sounds.setBits(c.offset, c.mask, v);
        changed = true;
    }

    /** A typed number (a phone has no spinner): the refusal, or null. */
    public Message set(Control c, String typed, String label)
    {
        changed = false;
        Message[] problem = new Message[1];
        Integer n = number(typed, c.min, c.max, label, problem);
        if (n == null)
            return problem[0];
        set(c, n);
        return null;
    }

    /** Revert This Sound: every byte the last page made edits put back as it was when the file was opened. */
    public void revert()
    {
        changed = false;
        for (int offset : lastBytes)
            if (sounds.getByte(offset) != (original[offset] & 0xFF))
            {
                sounds.setByte(offset, original[offset] & 0xFF);
                changed = true;
            }
    }
}
