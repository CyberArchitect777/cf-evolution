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
import cfevolution.edit.SoundEffectsPage;
import cfevolution.gui.MainGUI;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import javax.swing.*;
import javax.swing.event.*;

/**
 * The AdLib and Sound Blaster sound effects (XSOUND.BIN, the installed copy
 * of ASOUND.BIN). A view over the core's SoundEffectsPage (2026-11-17), which
 * holds each sound's rows, their ranges and how each value is written.
 *
 * @author barrie
 */
public class SoundEffectsTab extends GameFileTab
{
    private final SoundEffectsPage sounds;
    private final JList<String> list = new JList<String>(SoundEffectsPage.SOUNDS);
    private final JPanel form = new JPanel(new GridBagLayout());
    private int row;

    public SoundEffectsTab(SoundEffectsFile file, String fileName, MainGUI mainWindow) throws IOException
    {
        this(new SoundEffectsPage(file, Files.readAllBytes(new File(fileName).toPath())), fileName, mainWindow);
    }

    private SoundEffectsTab(SoundEffectsPage sounds, String fileName, MainGUI mainWindow)
    {
        super(sounds, fileName, mainWindow);
        this.sounds = sounds;

        list.setName("sounds");
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) { if (!e.getValueIsAdjusting()) fillForm(); }
        });

        JButton revert = new JButton(SoundEffectsPage.REVERT);
        revert.setName("revert sound");
        revert.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SoundEffectsTab.this.sounds.revert();
                changed();
                fillForm();
            }
        });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.add(revert);
        JLabel hint = new JLabel(SoundEffectsPage.HINT);
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
        add(page(split));
        list.setSelectedIndex(0);
    }

    private void changed()
    {
        if (sounds.changed())
            setModified(true);
    }

    /** Builds the page for the chosen sound from the file as it now stands. */
    private void fillForm()
    {
        form.removeAll();
        row = 0;
        for (SoundEffectsPage.Row r : sounds.rows(Math.max(0, list.getSelectedIndex())))
        {
            switch (r.kind)
            {
                case SoundEffectsPage.SECTION: section(r.label); break;
                case SoundEffectsPage.NOTE: note(r.label); break;
                case SoundEffectsPage.HEADINGS:
                {
                    GridBagConstraints c = new GridBagConstraints();
                    c.gridy = row++;
                    c.anchor = GridBagConstraints.WEST;
                    c.insets = new Insets(10, 0, 3, 12);
                    for (int col = 1; col <= 2; col++)
                    {
                        JLabel h = new JLabel(col == 1 ? SoundEffectsPage.MODULATOR : SoundEffectsPage.CARRIER);
                        h.setFont(h.getFont().deriveFont(Font.BOLD));
                        c.gridx = col;
                        form.add(h, c);
                    }
                    break;
                }
                default:
                    line(r.label, control(r.first), r.second != null ? control(r.second) : r.fixed != null ? new JLabel(r.fixed) : null, r.hint);
            }
        }
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridy = row;
        filler.weighty = 1;
        form.add(Box.createGlue(), filler);
        form.revalidate();
        form.repaint();
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

    // --- controls -----------------------------------------------------------

    private JComponent control(final SoundEffectsPage.Control c)
    {
        switch (c.kind)
        {
            case SoundEffectsPage.FLAG:
            {
                final JCheckBox box = new JCheckBox();
                box.setName(c.name);
                box.setSelected(sounds.value(c) == 1);
                box.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) { sounds.set(c, box.isSelected() ? 1 : 0); changed(); }
                });
                return box;
            }
            case SoundEffectsPage.CHOICE:
            {
                final JComboBox<String> box = new JComboBox<String>(c.items);
                box.setName(c.name);
                box.setSelectedIndex(sounds.value(c));
                box.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) { sounds.set(c, box.getSelectedIndex()); changed(); }
                });
                return box;
            }
            default:
            {
                final JSpinner spinner = new JSpinner(new SpinnerNumberModel(sounds.value(c), c.min, c.max, 1));
                spinner.setName(c.name);
                ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(5);
                spinner.addChangeListener(new ChangeListener() {
                    public void stateChanged(ChangeEvent e) { sounds.set(c, (Integer) spinner.getValue()); changed(); }
                });
                return spinner;
            }
        }
    }
}
