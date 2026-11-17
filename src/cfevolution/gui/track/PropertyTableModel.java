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

package cfevolution.gui.track;

import cfevolution.edit.PropertySheet;
import cfevolution.gui.table.StandardTableModel;
import javax.swing.JComboBox;

/**
 * The property table beside the map, as a view over a core PropertySheet
 * (2026-11-17; it replaces HeaderTableModel, SegmentTableModel,
 * BestLineTableModel and CmdParamTableModel). Numbers are text cells, tick
 * boxes are true/false cells and a choice is a list box in the cell; what is
 * typed goes into the sheet at once, and reaches the track on Apply.
 */
public class PropertyTableModel extends StandardTableModel
{
    private final PropertySheet sheet;
    private JComboBox<String> choice;

    public PropertyTableModel(PropertySheet sheet)
    {
        this.sheet = sheet;
        fill();
    }

    public PropertySheet getSheet()
    {
        return sheet;
    }

    private void fill()
    {
        tableDimension(sheet.rows(), 2);
        for (int r = 0; r < sheet.rows(); r++)
        {
            tableData[r][0] = sheet.label(r);
            switch (sheet.kind(r))
            {
                case PropertySheet.FLAG: tableData[r][1] = Boolean.valueOf(sheet.ticked(r)); break;
                case PropertySheet.CHOICE: tableData[r][1] = choiceBox(r); break;
                default: tableData[r][1] = sheet.text(r);
            }
        }
    }

    /** The one list box a sheet has, made once; picking rebuilds the rows. */
    private JComboBox<String> choiceBox(final int row)
    {
        if (choice == null)
        {
            choice = new JComboBox<String>();
            for (String s : sheet.options(row))
                choice.addItem(s);
            choice.setSelectedIndex(sheet.chosen(row));
            choice.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e)
                {
                    sheet.choose(row, choice.getSelectedIndex());
                    fill();
                    if (sheet.what == PropertySheet.HEADERS)
                    {
                        // the second kerb colours appear (or go) at once (Hrvoje, 2026-10-24)
                        javax.swing.SwingUtilities.invokeLater(new Runnable() {
                            public void run() { fireTableDataChanged(); }
                        });
                    }
                    else
                        fireTableDataChanged();
                }
            });
        }
        return choice;
    }

    public void setValueAt(Object value, int row, int column)
    {
        super.setValueAt(value, row, column);
        if (column != 1)
            return;
        if (value instanceof Boolean)
            sheet.tick(row, ((Boolean) value).booleanValue());
        else if (value instanceof String)
            sheet.type(row, (String) value);
        if (sheet.what == PropertySheet.LINE && row == 1)
            fireTableCellUpdated(row, column);     // the note under the table follows the length
    }

    /** After Apply: shows numbers the sheet changed (a best line length brought into range). */
    public void showSheet()
    {
        for (int r = 0; r < sheet.rows(); r++)
            if (sheet.kind(r) == PropertySheet.NUMBER && !sheet.text(r).equals(tableData[r][1]))
                setValueAt(sheet.text(r), r, 1);
    }
}
