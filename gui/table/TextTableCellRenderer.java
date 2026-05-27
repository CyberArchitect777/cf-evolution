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
 * TextTableCellRenderer.java
 *
 * Created on 23 May 2005, 01:42
 */

package chequeredflag.gui.table;

import javax.swing.*;
import java.awt.*;
import javax.swing.table.*;

/**
 *
 * @author barrie
 */
public class TextTableCellRenderer extends DefaultTableCellRenderer implements TableCellRenderer
{
    private JLabel cellLabel = new JLabel();
        
    /** Creates a new instance of TextTableCellRenderer */
    public TextTableCellRenderer() 
    {
        cellLabel = new JLabel();
    }
    
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
    {
        cellLabel.setText((String)value);
        cellLabel.setHorizontalAlignment(SwingConstants.CENTER);
        cellLabel.setOpaque(true);
        Font labelFont = cellLabel.getFont();
        cellLabel.setFont(new Font("SansSerif", Font.PLAIN, labelFont.getSize()));
        
        if (isSelected)
        {
            cellLabel.setForeground(table.getSelectionForeground());
            cellLabel.setBackground(table.getSelectionBackground());
        }
        else 
        {
            cellLabel.setForeground(table.getForeground());
            cellLabel.setBackground(table.getBackground());
        } 
        
        return cellLabel;       
    }
    
      
}
