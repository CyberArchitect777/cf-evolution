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
import cfevolution.gui.MainGUI;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;

/**
 * A name file (.NAM): the driver, team and engine names the game loads from
 * its Load/Save menu, or at start-up when the preferences say so.
 *
 * @author barrie
 */
public class NameFileTab extends GameFileTab
{
    private final NameFile names;

    public NameFileTab(NameFile names, String fileName, MainGUI mainWindow)
    {
        super(names, fileName, mainWindow);
        this.names = names;
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Drivers", new JScrollPane(nameTable(NameFile.DRIVERS, "Driver", NameFile.DRIVER_LENGTH - 1, 0)));
        tabs.addTab("Teams", new JScrollPane(nameTable(NameFile.TEAMS, "Team", NameFile.TEAM_LENGTH - 1, 1)));
        tabs.addTab("Engines", new JScrollPane(nameTable(NameFile.TEAMS, "Engine", NameFile.TEAM_LENGTH - 1, 2)));
        add(page("Name File", "Driver names by car number, and team and engine names in the order the game lists them.",
                 tabs));
    }

    private JTable nameTable(final int rows, final String what, final int maxLength, final int kind)
    {
        AbstractTableModel model = new AbstractTableModel() {
            public int getRowCount() { return rows; }
            public int getColumnCount() { return 2; }
            public String getColumnName(int c) { return c == 0 ? (kind == 0 ? "Car" : "No.") : what; }
            public Class<?> getColumnClass(int c) { return c == 0 ? Integer.class : String.class; }
            public boolean isCellEditable(int r, int c) { return c == 1; }
            public Object getValueAt(int r, int c) {
                if (c == 0) return r + 1;
                return kind == 0 ? names.getDriver(r) : kind == 1 ? names.getTeam(r) : names.getEngine(r);
            }
            public void setValueAt(Object v, int r, int c) {
                String t = text(v, maxLength, "A name");
                if (t == null || t.equals(getValueAt(r, c))) return;
                if (kind == 0) names.setDriver(r, t); else if (kind == 1) names.setTeam(r, t); else names.setEngine(r, t);
                fireTableCellUpdated(r, c);
            }
        };
        JTable table = table(model, new int[] { 50, 0 });
        table.setName(what.toLowerCase() + "s");
        return table;
    }

    public String getStatusText()
    {
        return "Name file  ·  40 drivers  ·  20 teams and engines";
    }
}
