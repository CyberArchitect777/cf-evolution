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

import cfevolution.data.gamefiles.NameFile;
import cfevolution.edit.NameFilePage;
import cfevolution.gui.MainGUI;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;

/**
 * A name file (.NAM): the driver, team and engine names the game loads from
 * its Load/Save menu, or at start-up when the preferences say so. A view over
 * the core's NameFilePage (2026-11-17).
 *
 * @author barrie
 */
public class NameFileTab extends GameFileTab
{
    private final NameFilePage names;

    public NameFileTab(NameFile file, String fileName, MainGUI mainWindow)
    {
        this(new NameFilePage(file), fileName, mainWindow);
    }

    public NameFileTab(NameFilePage names, String fileName, MainGUI mainWindow)
    {
        super(names, fileName, mainWindow);
        this.names = names;
        JTabbedPane tabs = new JTabbedPane();
        for (int kind = 0; kind < NameFilePage.TAB_TITLES.length; kind++)
            tabs.addTab(NameFilePage.TAB_TITLES[kind], new JScrollPane(nameTable(kind)));
        add(page(tabs));
    }

    private JTable nameTable(final int kind)
    {
        final String[] columns = NameFilePage.columns(kind);
        AbstractTableModel model = new AbstractTableModel() {
            public int getRowCount() { return NameFilePage.rows(kind); }
            public int getColumnCount() { return 2; }
            public String getColumnName(int c) { return columns[c]; }
            public Class<?> getColumnClass(int c) { return c == 0 ? Integer.class : String.class; }
            public boolean isCellEditable(int r, int c) { return c == 1; }
            public Object getValueAt(int r, int c) {
                if (c == 0) return r + 1;
                return names.get(kind, r);
            }
            public void setValueAt(Object v, int r, int c) {
                if (refused(names.set(kind, r, v)) || !names.changed()) return;
                fireTableCellUpdated(r, c);
            }
        };
        JTable table = table(model, new int[] { 50, 0 });
        table.setName(NameFilePage.tableName(kind));
        return table;
    }
}
