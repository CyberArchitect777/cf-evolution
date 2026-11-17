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
import cfevolution.data.gamefiles.ChampionshipSave;
import cfevolution.edit.ChampionshipPage;
import cfevolution.gui.MainGUI;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;

/**
 * A championship saved between races: each driver's finishing places, the
 * next round and each round's weather. Points follow from the places, using
 * the points and "best results" rule of the game folder's GP.EXE. A view over
 * the core's ChampionshipPage (2026-11-17).
 *
 * @author barrie
 */
public class ChampionshipSaveTab extends GameFileTab
{
    private final ChampionshipPage save;

    /** @param points points for 1st, 2nd... from GP.EXE, or null to show none */
    public ChampionshipSaveTab(ChampionshipSave file, String fileName, MainGUI mainWindow, int[] points, int bestCounted)
    {
        this(new ChampionshipPage(file, points, bestCounted), fileName, mainWindow);
    }

    public ChampionshipSaveTab(final ChampionshipPage save, String fileName, MainGUI mainWindow)
    {
        super(save, fileName, mainWindow);
        this.save = save;

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        JPanel next = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        next.add(new JLabel(ChampionshipPage.NEXT_ROUND));
        final JComboBox<String> nextRound = new JComboBox<String>(ChampionshipPage.rounds());
        nextRound.setName("next round");
        nextRound.setSelectedIndex(save.nextRound());
        nextRound.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                save.setNextRound(nextRound.getSelectedIndex());
                if (save.changed())
                    setModified(true);
            }
        });
        next.add(nextRound);
        top.add(next);
        top.add(Box.createVerticalStrut(8));

        JPanel weather = new JPanel(new GridLayout(2, ChampionshipSave.ROUNDS, 2, 0));
        weather.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(0xD5D9DF)),
                                                           ChampionshipPage.WET_ROUNDS));
        for (int r = 1; r <= ChampionshipSave.ROUNDS; r++)
        {
            JLabel n = new JLabel(String.valueOf(r), SwingConstants.CENTER);
            n.setToolTipText(GameFolder.CIRCUIT_NAMES[r - 1]);
            weather.add(n);
        }
        for (int r = 1; r <= ChampionshipSave.ROUNDS; r++)
        {
            final int round = r;
            final JCheckBox wet = new JCheckBox();
            wet.setName("wet " + r);
            wet.setHorizontalAlignment(SwingConstants.CENTER);
            wet.setToolTipText(GameFolder.CIRCUIT_NAMES[r - 1] + ": wet race");
            wet.setSelected(save.wet(r));
            wet.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    save.setWet(round, wet.isSelected());
                    if (save.changed())
                        setModified(true);
                }
            });
            weather.add(wet);
        }
        weather.setAlignmentX(Component.LEFT_ALIGNMENT);
        next.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(weather);
        top.add(Box.createVerticalStrut(8));

        JPanel content = new JPanel(new BorderLayout());
        content.add(top, BorderLayout.NORTH);
        content.add(new JScrollPane(placesTable()), BorderLayout.CENTER);
        add(page(content));
    }

    private JTable placesTable()
    {
        final int first = ChampionshipPage.FIRST_ROUND, last = first + ChampionshipSave.ROUNDS;
        final String[] columns = save.columns();
        AbstractTableModel model = new AbstractTableModel() {
            public int getRowCount() { return ChampionshipPage.rows(); }
            public int getColumnCount() { return columns.length; }
            public String getColumnName(int c) { return columns[c]; }
            public Class<?> getColumnClass(int c) { return c == 1 ? String.class : Integer.class; }
            public boolean isCellEditable(int r, int c) { return c >= first && c < last; }
            public Object getValueAt(int r, int c) { return save.cell(r, c); }
            public void setValueAt(Object v, int r, int c) {
                if (refused(save.setPlace(r, c - first + 1, v)) || !save.changed()) return;
                fireTableRowsUpdated(r, r);
            }
        };
        int[] widths = new int[model.getColumnCount()];
        widths[0] = 40;
        widths[1] = 150;
        for (int i = first; i < last; i++) widths[i] = 34;
        if (save.showsPoints()) widths[last] = 60;
        JTable table = table(model, widths);
        table.setName("places");
        for (int i = first; i < last; i++)
            table.getColumnModel().getColumn(i).setHeaderValue(String.valueOf(i - first + 1));
        return table;
    }
}
