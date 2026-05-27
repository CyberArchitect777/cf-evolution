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
 * TextTableCellEditor.java
 *
 * Created on 04 June 2005, 01:04
 */

package chequeredflag.gui.table;

import javax.swing.*;
import java.awt.*;
import javax.swing.table.*;

/**
 *
 * @author barrie
 */
public class TextTableCellEditor extends AbstractCellEditor implements TableCellEditor
{
    
    private JTextField cellField;
    
    /** Creates a new instance of TextTableCellEditor */
    public TextTableCellEditor() 
    {
        cellField = new JTextField();
    }
    
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
    {
        cellField.setHorizontalAlignment(SwingConstants.CENTER);
        cellField.setOpaque(true);
        Font labelFont = cellField.getFont();
        cellField.setFont(new Font("SansSerif", Font.PLAIN, labelFont.getSize()));
        cellField.setText((String)value);
        return cellField;
    }
    
    public Object getCellEditorValue()
    {
        return cellField.getText();
    }
}
