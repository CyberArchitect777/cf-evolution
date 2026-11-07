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
import cfevolution.data.gamefiles.SetupFile;
import cfevolution.gui.MainGUI;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;

/**
 * A setup file from the game's setup screen: one setup, or a whole
 * season's (qualifying and race for each circuit).
 *
 * @author barrie
 */
public class SetupFileTab extends GameFileTab
{
    private static final String[] COLUMNS = { "Front wing", "Rear wing", "Gear 1", "Gear 2", "Gear 3", "Gear 4",
                                              "Gear 5", "Gear 6", "Tyres", "Brake balance" };
    private final SetupFile setups;

    public SetupFileTab(SetupFile setups, String fileName, MainGUI mainWindow)
    {
        super(setups, fileName, mainWindow);
        this.setups = setups;
        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.add(new JScrollPane(setupTable()), BorderLayout.CENTER);
        if (setups.isSeason())
        {
            final JCheckBox separate = new JCheckBox("Separate race and qualifying setups",
                                                     setups.isSeparateRaceSetups());
            separate.setName("separate");
            separate.setToolTipText("When off, the game uses the qualifying setup in the race too.");
            separate.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (separate.isSelected() != SetupFileTab.this.setups.isSeparateRaceSetups())
                    {
                        SetupFileTab.this.setups.setSeparateRaceSetups(separate.isSelected());
                        setModified(true);
                    }
                }
            });
            content.add(separate, BorderLayout.NORTH);
        }
        add(page(setups.isSeason() ? "Season Setups" : "Setup",
                 setups.isSeason() ? "A qualifying and a race setup for each circuit, as the game's setup screen saves them."
                                   : "One car setup, as the game's setup screen saves it.",
                 content));
    }

    private String rowName(int r)
    {
        if (!setups.isSeason())
            return "Setup";
        return (r % 16 + 1) + "  " + GameFolder.CIRCUIT_NAMES[r % 16] + (r < 16 ? ", qualifying" : ", race");
    }

    private JTable setupTable()
    {
        AbstractTableModel model = new AbstractTableModel() {
            public int getRowCount() { return setups.getSetupCount(); }
            public int getColumnCount() { return COLUMNS.length + 1; }
            public String getColumnName(int c) { return c == 0 ? "" : COLUMNS[c - 1]; }
            public boolean isCellEditable(int r, int c) { return c > 0; }
            public Object getValueAt(int r, int c) {
                if (c == 0) return rowName(r);
                int v = setups.get(r, c - 1);
                if (c - 1 == SetupFile.TYRES) return v < SetupFile.COMPOUNDS.length ? SetupFile.COMPOUNDS[v] : String.valueOf(v);
                return v;
            }
            public void setValueAt(Object v, int r, int c) {
                // The game's own limits (Hrvoje, 2026-11-05)
                int field = c - 1, n;
                if (field == SetupFile.TYRES)
                    n = java.util.Arrays.asList(SetupFile.COMPOUNDS).indexOf(String.valueOf(v).trim().toUpperCase());
                else
                {
                    try { n = Integer.parseInt(String.valueOf(v).trim()); }
                    catch (NumberFormatException e) { n = Integer.MIN_VALUE; }
                }
                int[] setup = new int[SetupFile.SETUP_SIZE];
                for (int i = 0; i < setup.length; i++) setup[i] = setups.get(r, i);
                String problem = SetupFile.problem(setup, field, n);
                if (problem != null)
                {
                    JOptionPane.showMessageDialog(SetupFileTab.this, problem, "Value Not Changed", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (n == setups.get(r, field)) return;
                setups.set(r, field, n);
                fireTableCellUpdated(r, c);
            }
        };
        int[] widths = new int[COLUMNS.length + 1];
        widths[0] = setups.isSeason() ? 200 : 80;
        for (int i = 1; i < widths.length; i++) widths[i] = 76;
        JTable table = table(model, widths);
        table.setName("setups");
        table.getColumnModel().getColumn(1 + SetupFile.TYRES).setCellEditor(new DefaultCellEditor(new JComboBox<String>(SetupFile.COMPOUNDS)));
        return table;
    }

    public String getStatusText()
    {
        return setups.isSeason() ? "Season setup file  ·  16 circuits, qualifying and race" : "Setup file  ·  one setup";
    }
}
