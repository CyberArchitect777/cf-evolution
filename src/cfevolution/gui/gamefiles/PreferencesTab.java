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

import cfevolution.data.GameFolder;
import cfevolution.data.gamefiles.PreferencesFile;
import cfevolution.gui.MainGUI;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import javax.swing.*;
import javax.swing.event.*;

/**
 * F1PREFS.DAT: the options the game saves with Save Options and reads at
 * start-up. Meanings are from our IDA names and the F1GP-SDL project's
 * reading (2026-10-25). Each value is written only when it is changed; the
 * on/off options are read as on when bit 7 is set and written as FFh or 0,
 * as the game's own menus write them.
 *
 * @author barrie
 */
public class PreferencesTab extends GameFileTab
{
    private static final String[] LEVELS = { "Rookie", "Amateur", "Semi-Pro", "Pro", "Ace" };
    private static final String[] SPREADS = { "All the same", "1991 levels", "Random" };
    private static final String[] AIDS = { "Automatic brakes (F1)", "Automatic gears (F2)", "Spin recovery (F3)",
                                           "Indestructible (F4)", "Ideal line (F5)", "Suggested gear (F6)" };
    private static final int AIDS_AT = 1144, KPH_AT = 1020, TEXTURES_AT = 1022;

    private final PreferencesFile prefs;
    private final JPanel form = new JPanel(new GridBagLayout());
    private int row;

    public PreferencesTab(PreferencesFile prefs, String fileName, MainGUI mainWindow)
    {
        super(prefs, fileName, mainWindow);
        this.prefs = prefs;

        section("Races");
        choice("Opposition standard", LEVELS, PreferencesFile.OPPOSITION_STANDARD, true, null);
        choice("Opposition spread", SPREADS, PreferencesFile.OPPOSITION_SPREAD, true,
               "1991 levels keeps each team's own engine power.");
        number("Race distance (%)", PreferencesFile.RACE_DISTANCE, true, 5, 100, "5% is the shortest: 3 laps.");
        number("Qualifying session (minutes)", PreferencesFile.QUALIFYING_MINUTES, true, 1, 120, null);
        number("Practice sessions (minutes)", PreferencesFile.PRACTICE_MINUTES, true, 1, 120, null);

        section("Driving aids");
        for (int bit = 0; bit < AIDS.length; bit++)
            aid(AIDS[bit], bit);
        note("The opposition standard can switch some aids off whatever is set here.");

        section("Display");
        toggle("Speeds in km/h", KPH_AT, true);
        toggle("Textures", TEXTURES_AT, false);

        section("Quick race");
        choice("Circuit", GameFolder.CIRCUIT_NAMES, PreferencesFile.QUICK_RACE_CIRCUIT, false, null);
        number("Driver (car number)", PreferencesFile.QUICK_RACE_DRIVER, false, 1, 40, null);
        number("Race distance (%)", PreferencesFile.QUICK_RACE_DISTANCE, false, 5, 100, null);
        toggle("Wet race", PreferencesFile.QUICK_RACE_WET, false);

        section("Files loaded when the game starts");
        autoLoad("Name file", PreferencesFile.AUTO_LOAD_NAMES, PreferencesFile.NAME_FILE_PATH);
        autoLoad("Set-up file", PreferencesFile.AUTO_LOAD_SETUP, PreferencesFile.SETUP_FILE_PATH);
        autoLoad("Lap records file", PreferencesFile.AUTO_LOAD_RECORDS, PreferencesFile.RECORDS_FILE_PATH);
        note("A file in the game folder, for example gpsaves\\MYNAMES.NAM.");

        GridBagConstraints filler = new GridBagConstraints();
        filler.gridy = row;
        filler.weighty = 1;
        form.add(Box.createGlue(), filler);
        form.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(page("Game Options", "The options the game keeps in F1PREFS.DAT and reads when it starts.", scroll));
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
        c.gridwidth = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(heading, c);
    }

    private void addRow(String label, JComponent control, String hint)
    {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row++;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 0, 3, 12);
        JLabel l = new JLabel(label);
        l.setPreferredSize(new Dimension(230, l.getPreferredSize().height));
        form.add(l, c);
        c.gridx = 1;
        form.add(control, c);
        c.gridx = 2;
        c.weightx = 1;
        JLabel h = new JLabel(hint == null ? "" : hint);
        h.setForeground(new Color(0x6A7380));
        form.add(h, c);
    }

    private void note(String text)
    {
        JLabel n = new JLabel(text);
        n.setForeground(new Color(0x6A7380));
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row++;
        c.gridwidth = 3;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(2, 0, 2, 0);
        form.add(n, c);
    }

    private int get(int at, boolean word)
    {
        return word ? prefs.getWord(at) : prefs.getByte(at);
    }

    private void put(int at, boolean word, int value)
    {
        if (get(at, word) == value)
            return;
        if (word) prefs.setWord(at, value); else prefs.setByte(at, value);
        setModified(true);
    }

    private void choice(String label, String[] items, final int at, final boolean word, String hint)
    {
        final JComboBox<String> box = new JComboBox<String>(items);
        box.setName(label);
        int v = get(at, word);
        if (v < items.length)
            box.setSelectedIndex(v);
        else
        {
            box.addItem("Other (" + v + ")");   // a value the editor does not know: kept unless changed
            box.setSelectedIndex(items.length);
        }
        final int known = items.length;
        box.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (box.getSelectedIndex() < known)
                    put(at, word, box.getSelectedIndex());
            }
        });
        addRow(label, box, hint);
    }

    private void number(String label, final int at, final boolean word, int min, int max, String hint)
    {
        int v = get(at, word);
        final JSpinner spinner = new JSpinner(new SpinnerNumberModel(Math.max(min, Math.min(max, v)), min, max, 1));
        spinner.setName(label);
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(5);
        spinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) { put(at, word, (Integer) spinner.getValue()); }
        });
        addRow(label, spinner, v < min || v > max ? "Stored as " + v + "; kept unless changed. " + (hint == null ? "" : hint) : hint);
    }

    /** An on/off option: on when bit 7 (of the byte, or of the word's high byte) is set. */
    private void toggle(String label, final int at, final boolean word)
    {
        final JCheckBox box = new JCheckBox();
        box.setName(label);
        box.setSelected(word ? (prefs.getWord(at) & 0x8000) != 0 : (prefs.getByte(at) & 0x80) != 0);
        box.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                put(at, word, box.isSelected() ? (word ? 0xFFFF : 0xFF) : 0);
            }
        });
        addRow(label, box, null);
    }

    private void aid(String label, final int bit)
    {
        final JCheckBox box = new JCheckBox();
        box.setName(label);
        box.setSelected((prefs.getWord(AIDS_AT) & (1 << bit)) != 0);
        box.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int v = prefs.getWord(AIDS_AT);
                put(AIDS_AT, true, box.isSelected() ? v | (1 << bit) : v & ~(1 << bit));
            }
        });
        addRow(label, box, null);
    }

    private void autoLoad(final String label, final int flagAt, final int pathAt)
    {
        final JCheckBox load = new JCheckBox("Load");
        load.setName(label + " load");
        load.setSelected(prefs.getByte(flagAt) == 0xFF);
        final JTextField path = new JTextField(prefs.getText(pathAt, PreferencesFile.PATH_LENGTH), 24);
        path.setName(label + " path");
        JButton choose = new JButton("Choose...");
        choose.setName(label + " choose");
        load.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { put(flagAt, false, load.isSelected() ? 0xFF : 0); }
        });
        final Runnable storePath = new Runnable() {
            public void run() {
                String t = text(path.getText(), PreferencesFile.PATH_LENGTH - 1, "The path");
                if (t == null)
                {
                    path.setText(prefs.getText(pathAt, PreferencesFile.PATH_LENGTH));
                    return;
                }
                if (!t.equals(prefs.getText(pathAt, PreferencesFile.PATH_LENGTH)))
                {
                    prefs.setText(pathAt, PreferencesFile.PATH_LENGTH, t);
                    setModified(true);
                }
            }
        };
        path.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { storePath.run(); }
        });
        path.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) { storePath.run(); }
        });
        choose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                File game = new File(fileName).getAbsoluteFile().getParentFile();
                File saves = new File(game, "GPSAVES");
                JFileChooser chooser = new JFileChooser(saves.isDirectory() ? saves : game);
                chooser.setDialogTitle("Choose the " + label.toLowerCase());
                if (chooser.showOpenDialog(PreferencesTab.this) != JFileChooser.APPROVE_OPTION)
                    return;
                String relative = relativePath(game, chooser.getSelectedFile());
                if (relative == null || relative.length() > PreferencesFile.PATH_LENGTH - 1)
                {
                    JOptionPane.showMessageDialog(PreferencesTab.this,
                        "The game can only load a file inside its own folder, with a path of at most "
                        + (PreferencesFile.PATH_LENGTH - 1) + " characters.", "File Not Chosen",
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
                path.setText(relative);
                storePath.run();
                if (!load.isSelected())
                    load.doClick();
            }
        });
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        controls.add(load);
        controls.add(path);
        controls.add(choose);
        addRow(label, controls, null);
    }

    /** The DOS path of a file relative to the game folder, or null if outside it. */
    static String relativePath(File game, File file)
    {
        String base = game.getAbsolutePath() + File.separator, path = file.getAbsolutePath();
        if (!path.startsWith(base))
            return null;
        return path.substring(base.length()).replace(File.separatorChar, '\\').toUpperCase();
    }

    public String getStatusText()
    {
        return "Game options  ·  read by the game when it starts";
    }
}
