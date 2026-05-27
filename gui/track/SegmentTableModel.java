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
 * SegmentTableModel.java
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
public class SegmentTableModel extends StandardTableModel
{
    
    private TrackSegment currentSegment;
    
    /** Creates a new instance of SegmentTableModel */
    /** This class is designed to provide a database like interface for */
    /** F1GP/WC track segments */
    public SegmentTableModel(TrackSegment selectedSegment) 
    {
        // Sets the size of the table for the editing of this item
        // Will need to be updated as more information is added
        
        tableDimension(21,2);
        
        currentSegment = selectedSegment;
        populateTable();        
    }
    
    public void updateTrackData()
    {
        // Updates the track data held in memory with the information currently displayed in the table
        
        currentSegment.setTlu(new Integer((String)getValueAt(0,1)).intValue());
        currentSegment.setCurvature(new Integer((String)getValueAt(1,1)).intValue());
        currentSegment.setHeightChange(new Integer((String)getValueAt(2,1)).intValue());
        currentSegment.setFenceDistL(new Integer((String)getValueAt(3,1)).intValue());
        currentSegment.setFenceDistR(new Integer((String)getValueAt(4,1)).intValue());
        StringBuffer newBinaryCode = new StringBuffer();
        for (int x=0;x<16;x++)
        {
            boolean currentCode = ((Boolean)getValueAt(5+x,1)).booleanValue();
            if (currentCode == true)
            {
                newBinaryCode.append("1");
            }
            else
            {
                newBinaryCode.append("0");
            }
        }
        
        currentSegment.setDetailedFlags(newBinaryCode.toString());        
        
    }
    
    public void populateTable()
    {
        
        // Populate the table with track segment editing information
        
        String binaryCode = currentSegment.getDetailedFlags();
                
        setValueAt("Segment Length",0,0);
        setValueAt(new String(new Integer(currentSegment.getTlu()).toString()),0,1);
        setValueAt("Curvature",1,0);
        setValueAt(new String(new Integer(currentSegment.getCurvature()).toString()),1,1);
        setValueAt("Height Difference",2,0);
        setValueAt(new String(new Integer(currentSegment.getHeightChange()).toString()),2,1);
        setValueAt("Left Run-Off Area",3,0);
        setValueAt(new String(new Integer(currentSegment.getFenceDistL()).toString()),3,1);
        setValueAt("Right Run-Off Area",4,0);
        setValueAt(new String(new Integer(currentSegment.getFenceDistR()).toString()),4,1);
        setValueAt("Unknown 1 (0x8000)",5,0);
        setValueAt(getTrackFlagState(binaryCode, 0),5,1);
        setValueAt("Unknown 2 (0x4000)",6,0);
        setValueAt(getTrackFlagState(binaryCode, 1),6,1);
        setValueAt("No Left Wall",7,0);
        setValueAt(getTrackFlagState(binaryCode, 2),7,1);
        setValueAt("No Right Wall",8,0);
        setValueAt(getTrackFlagState(binaryCode, 3),8,1);
        setValueAt("Left Kerb",9,0);
        setValueAt(getTrackFlagState(binaryCode, 4),9,1);
        setValueAt("Right Kerb",10,0);
        setValueAt(getTrackFlagState(binaryCode, 5),10,1);
        setValueAt("Unknown 3 (0x0200)",11,0);
        setValueAt(getTrackFlagState(binaryCode, 6),11,1);
        setValueAt("Unknown 4 (0x0100)",12,0);
        setValueAt(getTrackFlagState(binaryCode, 7),12,1);
        setValueAt("Unknown 5 (0x0080)",13,0);
        setValueAt(getTrackFlagState(binaryCode, 8),13,1);
        setValueAt("Unknown 6 (0x0040)",14,0);
        setValueAt(getTrackFlagState(binaryCode, 9),14,1);
        setValueAt("Left Non-Parallel Wall",15,0);
        setValueAt(getTrackFlagState(binaryCode, 10),15,1);
        setValueAt("Right Non-Parallel Wall",16,0);
        setValueAt(getTrackFlagState(binaryCode, 11),16,1);
        setValueAt("Unknown 7 (0x0008)",17,0);
        setValueAt(getTrackFlagState(binaryCode, 12),17,1);
        setValueAt("Kerb Type",18,0);
        setValueAt(getTrackFlagState(binaryCode, 13),18,1);
        setValueAt("Left Pit Wall",19,0);
        setValueAt(getTrackFlagState(binaryCode, 14),19,1);
        setValueAt("Right Pit Wall",20,0);
        setValueAt(getTrackFlagState(binaryCode, 15),20,1);
        
    }
    
    public Boolean getTrackFlagState(String binaryCode, int flagNo)
    {
        
        // Refine the track segment flag information down to providing the state of one individual selected flag
        // (simulated binary digit 0 to 15)
        
        char uniqueCode = binaryCode.charAt(flagNo);
        if (uniqueCode == '1')
        {
            return new Boolean(true);
        }
        else
        {
            return new Boolean(false);
        }       
    }    
}
