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
 * HeaderTableModel.java
 *
 * Created on November 24, 2005, 6:50 PM
 */

package chequeredflag.gui.track;

import chequeredflag.data.track.*;
import chequeredflag.gui.*;
import chequeredflag.gui.table.StandardTableModel;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;

/**
 *
 * @author barrie
 */
public class HeaderTableModel extends StandardTableModel
{
    
    private TrackDataHeader currentHeader;
    private JComboBox curbType;
    
    /** Creates a new instance of HeaderTableModel */
    public HeaderTableModel(TrackDataHeader selectedHeader) 
    {
        // Sets the size of the table for the editing of header information
        // Will need to be updated as more information is added
        
        tableDimension(21,2);
        
        currentHeader = selectedHeader;
        curbType = new JComboBox();
        populateTable();
    }
    
    public void updateTrackData()
    {
        // Updates the track data held in memory with the information currently displayed in the table
                
        currentHeader.setStartAngle(new Integer((String)getValueAt(0,1)).intValue());
        currentHeader.setStartHeight(new Integer((String)getValueAt(1,1)).intValue());
        currentHeader.setStartPos(0, new Integer((String)getValueAt(2,1)).intValue());
        currentHeader.setStartPos(1, new Integer((String)getValueAt(3,1)).intValue());
        currentHeader.setStartPos(2, new Integer((String)getValueAt(4,1)).intValue());
        currentHeader.setStartWidth(new Integer((String)getValueAt(5,1)).intValue());
        currentHeader.setPoleWidth(new Integer((String)getValueAt(6,1)).intValue());
        currentHeader.setPitSide(((Boolean)getValueAt(7,1)).booleanValue());
        currentHeader.setTrSurround(new Integer((String)getValueAt(8,1)).intValue());
        currentHeader.setFenceDistL(new Integer((String)getValueAt(9,1)).intValue());
        currentHeader.setFenceDistR(new Integer((String)getValueAt(10,1)).intValue());
        currentHeader.setUnknown1(new Integer((String)getValueAt(11,1)).intValue());
        currentHeader.setUnknown2(new Integer((String)getValueAt(12,1)).intValue());
        
        if (curbType.getSelectedIndex() == 0)
        {
            currentHeader.setTotalKerbColours(2);
            currentHeader.setKerbColor(true, true, new Integer((String)getValueAt(14,1)).intValue());
            currentHeader.setKerbColor(false, true, new Integer((String)getValueAt(15,1)).intValue());
        }
        else
        {
            currentHeader.setTotalKerbColours(4);
            currentHeader.setKerbColor(true, true, new Integer((String)getValueAt(14,1)).intValue());
            currentHeader.setKerbColor(false, true, new Integer((String)getValueAt(15,1)).intValue());
            currentHeader.setKerbColor(true, false, new Integer((String)getValueAt(16,1)).intValue());
            currentHeader.setKerbColor(false, false, new Integer((String)getValueAt(17,1)).intValue());
        }        
    }
    
    public void getInfoForType(int curbIndex)
    {
        // Populate editable track header based on decisions made using
        // multiple choice controls on the table.
        
        switch (curbIndex)
        {
            case 0: tableDimension(16,2); break;
            case 1: tableDimension(18,2); break;
        }
        
        setValueAt("Starting Angle of First Segment",0,0);
        setValueAt(new String(new Integer(currentHeader.getStartAngle()).toString()),0,1);
        setValueAt("Starting Height of First Segment",1,0);
        setValueAt(new String(new Integer(currentHeader.getStartHeight()).toString()),1,1);
        setValueAt("X Coordinates Of Track Center At Start/Finish Line",2,0);
        setValueAt(new String(new Integer(currentHeader.getStartPos(0)).toString()),2,1);
        setValueAt("Y Coordinates Of Track Center At Start/Finish Line",3,0);
        setValueAt(new String(new Integer(currentHeader.getStartPos(1)).toString()),3,1);
        setValueAt("Z Coordinates Of Track Center At Start/Finish Line",4,0);
        setValueAt(new String(new Integer(currentHeader.getStartPos(2)).toString()),4,1);
        setValueAt("Starting Width of First Segment",5,0);
        setValueAt(new String(new Integer(currentHeader.getStartWidth()).toString()),5,1);
        setValueAt("Pole Width (Unknown Setting)",6,0);
        setValueAt(new String(new Integer(currentHeader.getPoleWidth()).toString()),6,1);
        setValueAt("Pits Located on Left of Track?",7,0);
        setValueAt(new Boolean(currentHeader.getPitSide()),7,1);
        setValueAt("TrSurround (Unknown Setting)",8,0);
        setValueAt(new String(new Integer(currentHeader.getTrSurround()).toString()),8,1);
        setValueAt("Starting Distance of Left Fence",9,0);
        setValueAt(new String(new Integer(currentHeader.getFenceDistL()).toString()),9,1);
        setValueAt("Starting Distance of Rignt Fence",10,0);
        setValueAt(new String(new Integer(currentHeader.getFenceDistR()).toString()),10,1);
        setValueAt("Unknown 1",11,0);
        setValueAt(new String(new Integer(currentHeader.getUnknown1()).toString()),11,1);
        setValueAt("Unknown 2",12,0);
        setValueAt(new String(new Integer(currentHeader.getUnknown2()).toString()),12,1);
        setValueAt("Circuit Kerb Type",13,0);
        setValueAt(curbType,13,1);
        
        if (curbIndex == 0)
        {
            setValueAt("Upper Kerb Colour",14,0);
            setValueAt(new String(new Integer(currentHeader.getKerbColor(true,true)).toString()),14,1);
            setValueAt("Lower Kerb Colour",15,0);
            setValueAt(new String(new Integer(currentHeader.getKerbColor(false,true)).toString()),15,1);
        }
        else
        {
            setValueAt("First Upper Kerb Colour",14,0);
            setValueAt(new String(new Integer(currentHeader.getKerbColor(true,true)).toString()),14,1);
            setValueAt("First Lower Kerb Colour",15,0);
            setValueAt(new String(new Integer(currentHeader.getKerbColor(false,true)).toString()),15,1);
            setValueAt("Second Upper Kerb Colour",16,0);
            setValueAt(new String(new Integer(currentHeader.getKerbColor(true,false)).toString()),16,1);
            setValueAt("Second Lower Kerb Colour",17,0);
            setValueAt(new String(new Integer(currentHeader.getKerbColor(false,false)).toString()),17,1);
        }
        
    }
    
    public void populateTable()
    {
        // Populate the table with track header editing information
        
        curbType.addItem(new String("Single Colour (Excluding White)"));
        curbType.addItem(new String("Dual Colour"));
        
        if (currentHeader.getTotalKerbColours() == 4)
        {
            curbType.setSelectedIndex(1);
        }
        else
        {
            curbType.setSelectedIndex(0);
        }        
        getInfoForType(curbType.getSelectedIndex());
        curbType.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent typeEvent)
            {
                getInfoForType(curbType.getSelectedIndex());
            }
        });
    }
        
}
