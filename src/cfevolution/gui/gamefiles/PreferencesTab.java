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

import cfevolution.data.gamefiles.PreferencesFile;
import cfevolution.edit.PreferencesPage;
import cfevolution.gui.MainGUI;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import javax.swing.*;
import javax.swing.event.*;

/**
 * F1PREFS.DAT: the options the game saves with Save Options and reads at
 * start-up. A view over the core's PreferencesPage (2026-11-17), which holds
 * the rows, their meanings and how each value is written.
 *
 * @author barrie
 */
public class PreferencesTab extends GameFileTab
{
    private final PreferencesPage prefs;
    private final JPanel form = new JPanel(new GridBagLayout());
    private int row;

    public PreferencesTab(PreferencesFile file, String fileName, MainGUI mainWindow)
    {
        this(new PreferencesPage(file, new File(fileName)), fileName, mainWindow);
    }

    public PreferencesTab(PreferencesPage prefs, String fileName, MainGUI mainWindow)
    {
        super(prefs, fileName, mainWindow);
        this.prefs = prefs;
        for (PreferencesPage.Item item : prefs.items())
        {
            switch (item.kind)
            {
                case PreferencesPage.SECTION: section(item.label); break;
                case PreferencesPage.NOTE: note(item.label); break;
                case PreferencesPage.CHOICE: choice(item); break;
                case PreferencesPage.NUMBER: number(item); break;
                case PreferencesPage.FRAME_RATE: frameRate(item); break;
                case PreferencesPage.AUTO_LOAD: autoLoad(item); break;
                default: toggle(item);      // TOGGLE, AID
            }
        }
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridy = row;
        filler.weighty = 1;
        form.add(Box.createGlue(), filler);
        form.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(page(scroll));
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

    private void changed()
    {
        if (prefs.changed())
            setModified(true);
    }

    private void choice(final PreferencesPage.Item item)
    {
        final JComboBox<String> box = new JComboBox<String>(item.items.toArray(new String[0]));
        box.setName(item.label);
        box.setSelectedIndex(prefs.chosen(item));
        box.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                prefs.choose(item, box.getSelectedIndex());
                changed();
            }
        });
        addRow(item.label, box, item.hint);
    }

    private void number(final PreferencesPage.Item item)
    {
        final JSpinner spinner = new JSpinner(new SpinnerNumberModel(prefs.number(item), item.min, item.max, 1));
        spinner.setName(item.label);
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(5);
        spinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                prefs.setNumber(item, (Integer) spinner.getValue());
                changed();
            }
        });
        addRow(item.label, spinner, item.hint);
    }

    /** An on/off option or a driving aid. */
    private void toggle(final PreferencesPage.Item item)
    {
        final JCheckBox box = new JCheckBox();
        box.setName(item.label);
        box.setSelected(prefs.on(item));
        box.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                prefs.setOn(item, box.isSelected());
                changed();
            }
        });
        addRow(item.label, box, null);
    }

    /** The frame rate: the game's own menu range and, with a warning, the faster rates GPFPS allowed. */
    private void frameRate(final PreferencesPage.Item item)
    {
        final JComboBox<String> box = new JComboBox<String>(item.items.toArray(new String[0]));
        box.setName(item.label);
        box.setSelectedIndex(prefs.chosen(item));
        final JLabel warning = new JLabel("<html>" + PreferencesPage.FRAME_RATE_WARNING + "</html>");
        warning.setName("Frame rate warning");
        warning.setForeground(new Color(0xB04A00));
        warning.setVisible(prefs.frameRateWarning());
        box.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                prefs.choose(item, box.getSelectedIndex());
                warning.setVisible(prefs.frameRateWarning());
                changed();
            }
        });
        addRow(item.label, box, item.hint);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row++;
        c.gridx = 1;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 3, 0);
        form.add(warning, c);
    }

    private void autoLoad(final PreferencesPage.Item item)
    {
        final JCheckBox load = new JCheckBox(PreferencesPage.LOAD);
        load.setName(item.label + " load");
        load.setSelected(prefs.on(item));
        final JTextField path = new JTextField(prefs.path(item), 24);
        path.setName(item.label + " path");
        JButton choose = new JButton(PreferencesPage.CHOOSE);
        choose.setName(item.label + " choose");
        load.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                prefs.setOn(item, load.isSelected());
                changed();
            }
        });
        final Runnable storePath = new Runnable() {
            public void run() {
                if (refused(prefs.setPath(item, path.getText())))
                    path.setText(prefs.path(item));
                else
                    changed();
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
                chooser.setDialogTitle(PreferencesPage.chooseTitle(item));
                if (chooser.showOpenDialog(PreferencesTab.this) != JFileChooser.APPROVE_OPTION)
                    return;
                if (refused(prefs.choosePath(item, chooser.getSelectedFile())))
                    return;
                path.setText(prefs.path(item));
                changed();
                if (!load.isSelected())
                    load.doClick();
            }
        });
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        controls.add(load);
        controls.add(path);
        controls.add(choose);
        addRow(item.label, controls, null);
    }
}
