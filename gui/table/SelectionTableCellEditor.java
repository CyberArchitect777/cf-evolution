/*
 * Chequered Flag: An editor for Formula One Grand Prix/World Circuit
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

/*
 * SelectionTableCellEditor.java
 *
 * Created on 07 July 2005, 00:48
 */

package chequeredflag.gui.table;

import javax.swing.*;
import java.awt.*;
import javax.swing.table.*;

/**
 *
 * @author barrie
 */
public class SelectionTableCellEditor extends AbstractCellEditor implements TableCellEditor
{
    
    private JComboBox selectionList;
    
    /** Creates a new instance of SelectionTableCellEditor */
    public SelectionTableCellEditor() 
    {
        selectionList = new JComboBox();
    }
    
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
    {
        selectionList = (JComboBox)value;
        selectionList.setAlignmentX(SwingConstants.CENTER);
        selectionList.setOpaque(true);
        
        return selectionList;
    }
    
    public Object getCellEditorValue()
    {
        return selectionList;
    }
}
