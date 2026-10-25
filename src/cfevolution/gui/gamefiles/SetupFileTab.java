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
 * A set-up file from the game's set-up screen: one set-up, or a whole
 * season's (qualifying and race for each circuit).
 *
 * @author barrie
 */
public class SetupFileTab extends GameFileTab
{
    private static final String[] COLUMNS = { "Front wing", "Rear wing", "Gear 1", "Gear 2", "Gear 3", "Gear 4",
                                              "Gear 5", "Gear 6", "Tyres", "Brake balance" };
    private static final String[] COMPOUNDS = { "A", "B", "C", "D" };
    private final SetupFile setups;

    public SetupFileTab(SetupFile setups, String fileName, MainGUI mainWindow)
    {
        super(setups, fileName, mainWindow);
        this.setups = setups;
        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.add(new JScrollPane(setupTable()), BorderLayout.CENTER);
        if (setups.isSeason())
        {
            final JCheckBox separate = new JCheckBox("Separate race and qualifying set-ups",
                                                     setups.isSeparateRaceSetups());
            separate.setName("separate");
            separate.setToolTipText("When off, the game uses the qualifying set-up in the race too.");
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
        add(page(setups.isSeason() ? "Season Set-ups" : "Set-up",
                 setups.isSeason() ? "A qualifying and a race set-up for each circuit, as the game's set-up screen saves them."
                                   : "One car set-up, as the game's set-up screen saves it.",
                 content));
    }

    private String rowName(int r)
    {
        if (!setups.isSeason())
            return "Set-up";
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
                if (c - 1 == SetupFile.TYRES) return v < COMPOUNDS.length ? COMPOUNDS[v] : String.valueOf(v);
                return v;
            }
            public void setValueAt(Object v, int r, int c) {
                int field = c - 1, n;
                if (field == SetupFile.TYRES)
                {
                    n = java.util.Arrays.asList(COMPOUNDS).indexOf(String.valueOf(v).trim().toUpperCase());
                    if (n < 0)
                    {
                        JOptionPane.showMessageDialog(SetupFileTab.this, "Tyres must be one of A, B, C or D.",
                            "Value Not Changed", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }
                else
                {
                    Integer i = field == SetupFile.BRAKE_BALANCE ? number(v, -127, 127, "Brake balance")
                                                                 : number(v, 0, 255, COLUMNS[field]);
                    if (i == null) return;
                    n = i;
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
        return table;
    }

    public String getStatusText()
    {
        return setups.isSeason() ? "Season set-up file  ·  16 circuits, qualifying and race" : "Set-up file  ·  one set-up";
    }
}
