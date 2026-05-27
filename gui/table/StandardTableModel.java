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
 * StandardTableModel.java
 *
 * Created on 14 June 2005, 02:15
 */

package chequeredflag.gui.table;

import javax.swing.table.*;
import javax.swing.JComboBox;

/**
 *
 * @author barrie
 */
public class StandardTableModel extends AbstractTableModel
{
    
    protected Object[][] tableData;
    
    /** Creates a new instance of StandardTableModel */
    public StandardTableModel() 
    {
    }
    
    public void tableDimension(int rowNumber, int columnNumber)
    {
        tableData = new Object[rowNumber][columnNumber];
    }
    
    public int getRowCount()
    {
        return tableData.length;        
    }
    
    public int getColumnCount()
    {
        return tableData[0].length;
    }
    
    public Class getColumnClass(int columnIndex)
    {
        return tableData[0][columnIndex].getClass();
    }
    
    public Object getValueAt(int row, int column)
    {
        return tableData[row][column];
    }
    
    public void setValueAt(Object aValue, int row, int column)
    {
        if (aValue instanceof Boolean)
        {
            if (((Boolean)aValue).booleanValue() == true)
            {
                tableData[row][column] = new Boolean(true);
            }
            else
            {
                tableData[row][column] = new Boolean(false);
            }
        }
        else
        {
            if (aValue instanceof JComboBox)
            {
                JComboBox comboList = (JComboBox)aValue;
                tableData[row][column] = comboList;
            }
            else
            {
                tableData[row][column] = (String)aValue;
            }
        }
    }
    
    public boolean isCellEditable(int row, int column)
    {
        if (column==0)
        {
            return false;
        }
        else
        {
            return true;
        }
    }
    
}
