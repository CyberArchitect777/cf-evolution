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
 * CmdParamTableModel.java
 *
 * Created on 13 May 2005, 23:55
 */

package chequeredflag.gui.track;
import chequeredflag.data.track.*;
import chequeredflag.gui.*;
import chequeredflag.gui.table.StandardTableModel;

/**
 *
 * @author barrie
 */
public class CmdParamTableModel extends StandardTableModel
{
    
    private Command currentCommand;
    private String[] paramText;
    
    /** Creates a new instance of CmdParamTableModel */
    /** This class is designed to provide a database like interface for */
    /** F1GP/WC track command parameters */
    public CmdParamTableModel(Command selectedCommand, String[] selectedParamText) 
    {
        // Sets the size of the table for the editing of this item
        // Will need to be updated as more information is added
        
        currentCommand = selectedCommand;
        paramText = selectedParamText;
        
        tableDimension(paramText.length,2);
      
        populateTable();        
    }
    
    public void updateTrackData()
    {
        // Updates the track data held in memory with the information currently displayed in the table
        
        for (int x=0;x<paramText.length;x++)
        {
            currentCommand.setParam(x, new Integer((String)getValueAt(x,1)).intValue());
        }
        
    }
    
    public void populateTable()
    {
        
        // Populate the table with track segment editing information
        
        for (int x=0;x<paramText.length;x++)
        {
            setValueAt(paramText[x],x, 0);
            setValueAt(new String(new Integer(currentCommand.getParam(x)).toString()),x,1);
        }        
    }
    
}
