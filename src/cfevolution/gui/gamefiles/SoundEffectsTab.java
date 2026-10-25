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
package cfevolution.gui.gamefiles;

import cfevolution.data.gamefiles.SoundEffectsFile;
import cfevolution.gui.MainGUI;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;

/**
 * The AdLib and Sound Blaster sound effects (XSOUND.BIN, the installed copy
 * of ASOUND.BIN): each effect's instrument, and the pitch, loudness and
 * timing values the effects code reads. Only those are offered, and each is
 * written only when it is changed, so the file keeps its size and its code.
 * The ranges keep the code's arithmetic from overflowing.
 *
 * @author barrie
 */
public class SoundEffectsTab extends GameFileTab
{
    private static final String[] SOUNDS = { "Engine, tone 1", "Engine, tone 2", "Engine, tone 3", "Tyre squeal",
                                             "Passing car 1", "Passing car 2", "Passing car 3", "Passing car 4",
                                             "Crash", "Wheel gun", "Start signal" };
    private static final int TYRES = 3, PASSING = 4, CRASH = 8, WHEEL_GUN = 9, START = 10;
    private static final String[] WAVEFORMS = { "Sine", "Half sine", "Rectified sine", "Quarter sine" };
    private static final String[] COMBINE = { "The modulator shapes the carrier", "Both are heard" };
    private static final int MOD = 0, CAR = 1;

    private final SoundEffectsFile sounds;
    private final byte[] original;
    private final JList<String> list = new JList<String>(SOUNDS);
    private final JPanel form = new JPanel(new GridBagLayout());
    private int row;
    /** The bytes the current page edits, so Revert This Sound can restore them. */
    private final List<Integer> pageBytes = new ArrayList<Integer>();

    public SoundEffectsTab(SoundEffectsFile sounds, String fileName, MainGUI mainWindow) throws IOException
    {
        super(sounds, fileName, mainWindow);
        this.sounds = sounds;
        this.original = Files.readAllBytes(new File(fileName).toPath());

        list.setName("sounds");
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) { if (!e.getValueIsAdjusting()) fillForm(); }
        });

        JButton revert = new JButton("Revert This Sound");
        revert.setName("revert sound");
        revert.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { revertSound(); }
        });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.add(revert);
        JLabel hint = new JLabel("Changes are heard in the game with an AdLib or Sound Blaster card.");
        hint.setForeground(new Color(0x6A7380));
        buttons.add(hint);

        form.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        JPanel centre = new JPanel(new BorderLayout(0, 4));
        centre.add(buttons, BorderLayout.NORTH);
        centre.add(scroll, BorderLayout.CENTER);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(list), centre);
        split.setDividerLocation(200);
        add(page("Sound Effects", "The game's sound effects for AdLib and Sound Blaster cards: each one's instrument, "
                 + "pitch, loudness and timing.", split));
        list.setSelectedIndex(0);
    }

    /** Builds the page for the chosen sound from the file as it now stands. */
    private void fillForm()
    {
        form.removeAll();
        pageBytes.clear();
        row = 0;
        int s = Math.max(0, list.getSelectedIndex());
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
                bits("Loudness, " + amounts[i] + " sliding", SoundEffectsFile.TYRE_LEVEL[i], 0x3F, 0, 63, false,
                     i == 0 ? "63 is the loudest." : null);
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

        GridBagConstraints filler = new GridBagConstraints();
        filler.gridy = row;
        filler.weighty = 1;
        form.add(Box.createGlue(), filler);
        form.revalidate();
        form.repaint();
    }

    private void instrument(int k)
    {
        int r = SoundEffectsFile.record(k);
        for (int i = 0; i < SoundEffectsFile.RECORD_SIZE; i++)
            pageBytes.add(r + i);
        section("Instrument");
        note("Two tones make each instrument: the carrier is what you hear, and the modulator changes its sound.");
        combo("How the two combine", null, r + SoundEffectsFile.FEEDBACK, 0x01, COMBINE, true);
        bits("Feedback", r + SoundEffectsFile.FEEDBACK, 0x0E, 0, 7, false, "Higher is harsher.");

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row++;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(10, 0, 3, 12);
        for (int col = 1; col <= 2; col++)
        {
            JLabel h = new JLabel(col == 1 ? "Modulator" : "Carrier");
            h.setFont(h.getFont().deriveFont(Font.BOLD));
            c.gridx = col;
            form.add(h, c);
        }
        pair("Frequency multiple", r + SoundEffectsFile.CHARACTER, 0x0F, 0, 15, false, "0 is half the note.");
        JSpinner volume = spinner("Volume modulator", r + SoundEffectsFile.MODULATOR_LEVEL, false, 0x3F, 0, 63, false);
        line("Volume", volume, new JLabel("Set by the game"), "63 is the loudest.");
        JSpinner scaling = spinner("Quieter at high notes modulator", r + SoundEffectsFile.MODULATOR_LEVEL, false, 0xC0, 0, 3, false);
        line("Quieter at high notes", scaling, null, "0 is not at all.");
        pair("Attack speed", r + SoundEffectsFile.ENVELOPE, 0xF0, 0, 15, false, "How fast it starts. 15 is the fastest.");
        pair("Decay speed", r + SoundEffectsFile.ENVELOPE, 0x0F, 0, 15, false, "How fast it falls to the held level.");
        pair("Held level", r + SoundEffectsFile.HOLD_RELEASE, 0xF0, 0, 15, true, "15 is the loudest.");
        pair("Release speed", r + SoundEffectsFile.HOLD_RELEASE, 0x0F, 0, 15, false, "How fast it dies away.");
        line("Waveform", combo(null, "Waveform modulator", r + SoundEffectsFile.WAVEFORM, 0x03, WAVEFORMS, false),
             combo(null, "Waveform carrier", r + SoundEffectsFile.WAVEFORM + 1, 0x03, WAVEFORMS, false), null);
        flagPair("Hold the note", r + SoundEffectsFile.CHARACTER, 0x20, "Otherwise it fades away while playing.");
        flagPair("Tremolo", r + SoundEffectsFile.CHARACTER, 0x80, "A wavering volume.");
        flagPair("Vibrato", r + SoundEffectsFile.CHARACTER, 0x40, "A wavering pitch.");
        flagPair("Shorter at high notes", r + SoundEffectsFile.CHARACTER, 0x10, null);
    }

    // --- rows ---------------------------------------------------------------

    private void section(String title)
    {
        JLabel heading = new JLabel(title.toUpperCase());
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 11f));
        heading.setForeground(new Color(0x6A7380));
        heading.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(row == 0 ? 0 : 14, 0, 4, 0),
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xD5D9DF))));
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row++;
        c.gridwidth = 4;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(heading, c);
    }

    private void note(String text)
    {
        JLabel n = new JLabel(text);
        n.setForeground(new Color(0x6A7380));
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row++;
        c.gridwidth = 4;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(2, 0, 2, 0);
        form.add(n, c);
    }

    /** A row: label, one or two controls, then a hint. */
    private void line(String label, JComponent first, JComponent second, String hint)
    {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row++;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 0, 3, 12);
        JLabel l = new JLabel(label);
        l.setPreferredSize(new Dimension(210, l.getPreferredSize().height));
        form.add(l, c);
        c.gridx = 1;
        form.add(first, c);
        if (second != null)
        {
            c.gridx = 2;
            if (second instanceof JLabel)
                second.setForeground(new Color(0x7A828C));
            form.add(second, c);
        }
        c.gridx = 3;
        c.weightx = 1;
        JLabel h = new JLabel(hint == null ? "" : hint);
        h.setForeground(new Color(0x6A7380));
        form.add(h, c);
    }

    private void word(String label, int offset, int min, int max, String hint)
    {
        int v = sounds.getWord(offset);
        line(label, spinner(label, offset, true, 0, min, max, false), null,
             v < min || v > max ? "Stored as " + v + "; kept unless changed." : hint);
    }

    private void bits(String label, int offset, int mask, int min, int max, boolean inverted, String hint)
    {
        line(label, spinner(label, offset, false, mask, min, max, inverted), null, hint);
    }

    private void pair(String label, int offset, int mask, int min, int max, boolean inverted, String hint)
    {
        line(label, spinner(label + " modulator", offset + MOD, false, mask, min, max, inverted),
             spinner(label + " carrier", offset + CAR, false, mask, min, max, inverted), hint);
    }

    private void flagPair(String label, int offset, int bit, String hint)
    {
        line(label, check(label + " modulator", offset + MOD, bit), check(label + " carrier", offset + CAR, bit), hint);
    }

    // --- controls -----------------------------------------------------------

    private int get(int offset, boolean word, int mask, int max, boolean inverted)
    {
        int v = word ? sounds.getWord(offset) : sounds.getBits(offset, mask);
        return inverted ? max - v : v;
    }

    /** Writes a value only when it differs from what is stored. */
    private void put(int offset, boolean word, int mask, int max, boolean inverted, int value)
    {
        if (get(offset, word, mask, max, inverted) == value)
            return;
        int stored = inverted ? max - value : value;
        if (word)
            sounds.setWord(offset, stored);
        else
            sounds.setBits(offset, mask, stored);
        setModified(true);
    }

    private JSpinner spinner(String name, final int offset, final boolean word, final int mask, int min, final int max,
                             final boolean inverted)
    {
        pageBytes.add(offset);
        if (word)
            pageBytes.add(offset + 1);
        int v = get(offset, word, mask, max, inverted);
        final JSpinner spinner = new JSpinner(new SpinnerNumberModel(Math.max(min, Math.min(max, v)), min, max, 1));
        spinner.setName(name);
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(5);
        spinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) { put(offset, word, mask, max, inverted, (Integer) spinner.getValue()); }
        });
        return spinner;
    }

    private JCheckBox check(String name, final int offset, final int bit)
    {
        pageBytes.add(offset);
        final JCheckBox box = new JCheckBox();
        box.setName(name);
        box.setSelected(sounds.getBits(offset, bit) != 0);
        box.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { put(offset, false, bit, 1, false, box.isSelected() ? 1 : 0); }
        });
        return box;
    }

    /** A choice; with a label it is a row of its own, otherwise the control is returned for a pair. */
    private JComboBox<String> combo(String label, String name, final int offset, final int mask, String[] items,
                                    final boolean inverted)
    {
        pageBytes.add(offset);
        final int max = items.length - 1;
        final JComboBox<String> box = new JComboBox<String>(items);
        box.setName(label != null ? label : name);
        box.setSelectedIndex(get(offset, false, mask, max, inverted));
        box.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { put(offset, false, mask, max, inverted, box.getSelectedIndex()); }
        });
        if (label != null)
            line(label, box, null, null);
        return box;
    }

    /** Puts back every byte this page edits as it was when the file was opened. */
    private void revertSound()
    {
        boolean changed = false;
        for (int offset : pageBytes)
            if (sounds.getByte(offset) != (original[offset] & 0xFF))
            {
                sounds.setByte(offset, original[offset] & 0xFF);
                changed = true;
            }
        if (changed)
            setModified(true);
        fillForm();
    }

    public String getStatusText()
    {
        return "Sound effects  ·  the game reads them when it starts";
    }
}
