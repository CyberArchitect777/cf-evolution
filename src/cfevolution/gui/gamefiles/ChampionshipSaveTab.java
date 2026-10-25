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
import cfevolution.gui.MainGUI;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;

/**
 * A championship saved between races: each driver's finishing places, the
 * next round and each round's weather. Points follow from the places, using
 * the points and "best results" rule of the game folder's GP.EXE.
 *
 * @author barrie
 */
public class ChampionshipSaveTab extends GameFileTab
{
    private final ChampionshipSave save;
    private final int[] points;
    private final int bestCounted;

    /** @param points points for 1st, 2nd... from GP.EXE, or null to show none */
    public ChampionshipSaveTab(ChampionshipSave save, String fileName, MainGUI mainWindow, int[] points, int bestCounted)
    {
        super(save, fileName, mainWindow);
        this.save = save;
        this.points = points;
        this.bestCounted = bestCounted;

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        JPanel next = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        next.add(new JLabel("Next round"));
        String[] rounds = new String[ChampionshipSave.ROUNDS + 1];
        for (int i = 0; i < ChampionshipSave.ROUNDS; i++)
            rounds[i] = (i + 1) + "  " + GameFolder.CIRCUIT_NAMES[i];
        rounds[ChampionshipSave.ROUNDS] = "Season finished";
        final JComboBox<String> nextRound = new JComboBox<String>(rounds);
        nextRound.setName("next round");
        nextRound.setSelectedIndex(Math.min(save.getNextRound(), ChampionshipSave.ROUNDS));
        nextRound.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (nextRound.getSelectedIndex() != ChampionshipSaveTab.this.save.getNextRound())
                {
                    ChampionshipSaveTab.this.save.setNextRound(nextRound.getSelectedIndex());
                    setModified(true);
                }
            }
        });
        next.add(nextRound);
        top.add(next);
        top.add(Box.createVerticalStrut(8));

        JPanel weather = new JPanel(new GridLayout(2, ChampionshipSave.ROUNDS, 2, 0));
        weather.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(0xD5D9DF)),
                                                           "Wet rounds"));
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
            wet.setSelected(save.isWet(r));
            wet.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (wet.isSelected() != ChampionshipSaveTab.this.save.isWet(round))
                    {
                        ChampionshipSaveTab.this.save.setWet(round, wet.isSelected());
                        setModified(true);
                    }
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
        add(page("Championship Save",
                 "Finishing places in each round (0 for none). The points follow from them, as the game counts them.",
                 content));
    }

    private JTable placesTable()
    {
        final int first = 2, last = first + ChampionshipSave.ROUNDS;
        AbstractTableModel model = new AbstractTableModel() {
            public int getRowCount() { return ChampionshipSave.DRIVERS; }
            public int getColumnCount() { return last + (points != null ? 1 : 0); }
            public String getColumnName(int c) {
                if (c == 0) return "Car";
                if (c == 1) return "Driver";
                if (c == last) return "Points";
                return String.valueOf(c - first + 1);
            }
            public Class<?> getColumnClass(int c) { return c == 1 ? String.class : Integer.class; }
            public boolean isCellEditable(int r, int c) { return c >= first && c < last; }
            public Object getValueAt(int r, int c) {
                if (c == 0) return r + 1;
                if (c == 1) return save.getDriver(r + 1);
                if (c == last) return save.getPoints(r + 1, points, bestCounted);
                return save.getPlace(r + 1, c - first + 1);
            }
            public void setValueAt(Object v, int r, int c) {
                Integer n = number(v, 0, 26, "A finishing place");
                if (n == null || n == save.getPlace(r + 1, c - first + 1)) return;
                save.setPlace(r + 1, c - first + 1, n);
                fireTableRowsUpdated(r, r);
            }
        };
        int[] widths = new int[model.getColumnCount()];
        widths[0] = 40;
        widths[1] = 150;
        for (int i = first; i < last; i++) widths[i] = 34;
        if (points != null) widths[last] = 60;
        JTable table = table(model, widths);
        table.setName("places");
        for (int i = first; i < last; i++)
            table.getColumnModel().getColumn(i).setHeaderValue(String.valueOf(i - first + 1));
        return table;
    }

    public String getStatusText()
    {
        int next = save.getNextRound();
        return "Championship save  ·  " + (next >= ChampionshipSave.ROUNDS ? "season finished"
                                                : "next round " + (next + 1) + ", " + GameFolder.CIRCUIT_NAMES[next]);
    }
}
