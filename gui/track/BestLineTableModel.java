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
 * BestLineTableModel.java
 *
 * Created on 14 June 2005, 01:50
 */

package chequeredflag.gui.track;

import chequeredflag.data.track.*;
import chequeredflag.gui.*;
import chequeredflag.gui.table.StandardTableModel;
import java.util.Vector;
import javax.swing.JComboBox;

/**
 *
 * @author barrie
 */
public class BestLineTableModel extends StandardTableModel
{
    
    private CCLineSegment currentLineSegment;
    private JComboBox typeList;
    
    /** Creates a new instance of BestLineTableModel */
    public BestLineTableModel(CCLineSegment selectedLineSegment) 
    {
        // Sets the size of the table for the editing of this item
        // Will need to be updated as more information is added
        
        tableDimension(4,2);
        
        currentLineSegment = selectedLineSegment;
        typeList = new JComboBox();
        
        populateTable(); 
    }
    
    public void updateTrackData()
    {
        // Updates the track data held in memory with the information currently displayed in the table
        
        JComboBox currentList = (JComboBox)getValueAt(0,1);
        switch (currentList.getSelectedIndex())
        {
            case 0: currentLineSegment.setType(0); break;
            case 1: currentLineSegment.setType(64); break;
            case 2: currentLineSegment.setType(128); break;
        }
        
        currentLineSegment.setTlu(new Integer((String)getValueAt(1,1)).intValue());
                
        if (currentList.getSelectedIndex() == 0 || currentList.getSelectedIndex() == 1)
        {
            currentLineSegment.setParam(0, new Integer((String)getValueAt(2,1)).intValue());
            if (currentList.getSelectedIndex() == 0)
            {
                currentLineSegment.setParam(1, new Integer((String)getValueAt(3,1)).intValue());
            }
            else
            {
                currentLineSegment.setParam(1, new Integer((String)getValueAt(3,1)).intValue());
                currentLineSegment.setParam(2, new Integer((String)getValueAt(4,1)).intValue());
            }
        }
        else
        {
            currentLineSegment.setParam(0, new Integer((String)getValueAt(2,1)).intValue());
            currentLineSegment.setParam(1, new Integer((String)getValueAt(3,1)).intValue());
            if (currentList.getSelectedIndex() == 2)
            {
                currentLineSegment.setParam(2, new Integer((String)getValueAt(4,1)).intValue());
            }
            else
            {
                currentLineSegment.setParam(2, new Integer((String)getValueAt(4,1)).intValue());
                currentLineSegment.setParam(3, new Integer((String)getValueAt(5,1)).intValue());
            }
        }        
    }
    
    public void getInfoForType(int segmentType)
    {
        switch (segmentType)
        {
            case 0: tableDimension(4,2); break;
            case 1: tableDimension(5,2); break;
            case 2: tableDimension(5,2); break;
            case 3: tableDimension(6,2); break;
        }
        setValueAt("Best Line Type",0,0);
        setValueAt(typeList,0,1);
        setValueAt("Best Line Length",1,0);
        setValueAt(new String(new Integer(currentLineSegment.getTlu()).toString()),1,1);
        
        if ((segmentType == 0) || (segmentType == 1))
        {
            setValueAt("Best Line Correction",2,0);
            setValueAt(new String(new Integer(currentLineSegment.getParam(0)).toString()),2,1);
            if (segmentType == 0)
            {
                setValueAt("Best Line Radius",3,0);
                setValueAt(new String(new Integer(currentLineSegment.getParam(1)).toString()),3,1);
            }
            else
            {
                setValueAt("Best Line High Radius",3,0);
                setValueAt(new String(new Integer(currentLineSegment.getParam(1)).toString()),3,1);
                setValueAt("Best Line Low Radius",4,0);
                setValueAt(new String(new Integer(currentLineSegment.getParam(2)).toString()),4,1);
            }
        }
        else
        {
            setValueAt("Best Line Displacement",2,0);
            setValueAt(new String(new Integer(currentLineSegment.getParam(0)).toString()),2,1);
            setValueAt("Best Line Correction",3,0);
            setValueAt(new String(new Integer(currentLineSegment.getParam(1)).toString()),3,1);
            if (segmentType == 2)
            {
                setValueAt("Best Line Radius",4,0);
                setValueAt(new String(new Integer(currentLineSegment.getParam(2)).toString()),4,1);
            }
            else
            {
                setValueAt("Best Line High Radius",4,0);
                setValueAt(new String(new Integer(currentLineSegment.getParam(2)).toString()),4,1);
                setValueAt("Best Line Low Radius",5,0);
                setValueAt(new String(new Integer(currentLineSegment.getParam(3)).toString()),5,1);
            }
        }
        this.fireTableDataChanged();
    }    
    
    public void populateTable()
    {
        
        // Populate the table with track segment editing information
        
        typeList.addItem(new String("Normal"));
        typeList.addItem(new String("Wider Radius"));
        typeList.addItem(new String("Displacement"));
        //typeList.addItem(new String("Combined")); // Possible, but unused in original game.
        
        switch (currentLineSegment.getType())
        {
            case 0: typeList.setSelectedIndex(0); break;
            case 64: typeList.setSelectedIndex(1); break; // 0x40
            case 128: typeList.setSelectedIndex(2); break; // 0x80
            //case 160: typeList.setSelectedIndex(3); // 0xa0. Possible, but unused in original game.
        }
        
        getInfoForType(typeList.getSelectedIndex());
        typeList.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent typeEvent)
            {
                getInfoForType(typeList.getSelectedIndex());
            }
        });
    }
    
}
