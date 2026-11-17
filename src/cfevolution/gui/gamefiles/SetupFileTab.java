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

import cfevolution.data.gamefiles.SetupFile;
import cfevolution.edit.SetupFilePage;
import cfevolution.gui.MainGUI;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;

/**
 * A setup file from the game's setup screen: one setup, or a whole
 * season's (qualifying and race for each circuit). A view over the core's
 * SetupFilePage (2026-11-17).
 *
 * @author barrie
 */
public class SetupFileTab extends GameFileTab
{
    private final SetupFilePage setups;

    public SetupFileTab(SetupFile file, String fileName, MainGUI mainWindow)
    {
        this(new SetupFilePage(file), fileName, mainWindow);
    }

    public SetupFileTab(SetupFilePage setups, String fileName, MainGUI mainWindow)
    {
        super(setups, fileName, mainWindow);
        this.setups = setups;
        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.add(new JScrollPane(setupTable()), BorderLayout.CENTER);
        if (setups.isSeason())
        {
            final JCheckBox separate = new JCheckBox(SetupFilePage.SEPARATE, setups.separate());
            separate.setName("separate");
            separate.setToolTipText(SetupFilePage.SEPARATE_TIP);
            separate.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    SetupFileTab.this.setups.setSeparate(separate.isSelected());
                    if (SetupFileTab.this.setups.changed())
                        setModified(true);
                }
            });
            content.add(separate, BorderLayout.NORTH);
        }
        add(page(content));
    }

    private JTable setupTable()
    {
        final String[] columns = SetupFilePage.COLUMNS;
        AbstractTableModel model = new AbstractTableModel() {
            public int getRowCount() { return setups.rows(); }
            public int getColumnCount() { return columns.length + 1; }
            public String getColumnName(int c) { return c == 0 ? "" : columns[c - 1]; }
            public boolean isCellEditable(int r, int c) { return c > 0; }
            public Object getValueAt(int r, int c) {
                if (c == 0) return setups.rowName(r);
                return setups.cell(r, c - 1);
            }
            public void setValueAt(Object v, int r, int c) {
                if (refused(setups.set(r, c - 1, v)) || !setups.changed()) return;
                fireTableCellUpdated(r, c);
            }
        };
        int[] widths = new int[columns.length + 1];
        widths[0] = setups.isSeason() ? 200 : 80;
        for (int i = 1; i < widths.length; i++) widths[i] = 76;
        JTable table = table(model, widths);
        table.setName("setups");
        table.getColumnModel().getColumn(1 + SetupFile.TYRES).setCellEditor(new DefaultCellEditor(new JComboBox<String>(SetupFile.COMPOUNDS)));
        return table;
    }
}
