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
 * CommandDataStore.java
 *
 * Created on 14 August 2006, 04:31
 */

package chequeredflag.data.track;

import java.util.ArrayList;

/**
 *
 * @author barrie
 */
public class CommandDataStore 
{
    
    private ArrayList commandCollection;
    
    /** Creates a new instance of CommandDataStore */
    public CommandDataStore() 
    {
        // Initialises the command data collection
        
        commandCollection = new ArrayList();
        populateDataStore();
        
    }
    
    public ArrayList createCollectionFromArray(String[] stringData)
    {
        ArrayList newCollection = new ArrayList();
        for (int x=0;x<stringData.length;x++)
        {
            newCollection.add(stringData[x]);
        }
        return newCollection;
    }
    
    public ArrayList createCollectionFromArray(String stringData)
    {
        ArrayList newCollection = new ArrayList();
        newCollection.add(stringData);
        return newCollection;
    }
    
    public CommandData getCommandByIndex(int commandIndex)
    {
        CommandData selectedCommand = (CommandData)commandCollection.get(commandIndex);
        return selectedCommand;        
    }
    
    public CommandData getCommandByType(int commandType)
    {
        CommandData selectedCommand = (CommandData)commandCollection.get(0);
        
        for (int x=0;x<commandCollection.size();x++)
        {
            CommandData currentCommand = (CommandData)commandCollection.get(x);
            if (currentCommand.getCommandType() == commandType)
            {
                selectedCommand = currentCommand;
            }
        }
        
        return selectedCommand;
    }
    
    public int getCommandListSize()
    {
        return commandCollection.size();
    }
    
    public void populateDataStore()
    {
        // Populates the data store with all known track segment
        // command information.
        
        String[] paramEightZero = {"Unknown", "Unknown"};
        commandCollection.add(new CommandData(0x80, createCollectionFromArray(paramEightZero), "Object placement"));
        String[] paramEightOne = {"Unknown", "Unknown"};
        commandCollection.add(new CommandData(0x81, createCollectionFromArray(paramEightOne), "Display distance front"));
        String[] paramEightTwo = {"Unknown", "Unknown"};
        commandCollection.add(new CommandData(0x82, createCollectionFromArray(paramEightTwo), "Display distance back"));
        commandCollection.add(new CommandData(0x83, createCollectionFromArray("Unknown"), "Disable drawing of background picture"));
        commandCollection.add(new CommandData(0x84, createCollectionFromArray("Unknown"), "Enable drawing of background picture"));
        String[] paramEightFive = {"Unknown", "Unknown", "Unknown"};
        commandCollection.add(new CommandData(0x85, createCollectionFromArray(paramEightFive), "Track width change"));
        commandCollection.add(new CommandData(0x86, createCollectionFromArray("Unknown"), "Connect pitline start"));
        commandCollection.add(new CommandData(0x87, createCollectionFromArray("Unknown"), "Connect pitlane end"));
        String[] paramEightEight = {"Unknown", "Length of pit parking zone marking 1?"};
        commandCollection.add(new CommandData(0x88, createCollectionFromArray(paramEightEight), "Pit parking zone marking 1"));
        String[] paramEightNine = {"Unknown", "Length of pit parking zone marking 2?"};
        commandCollection.add(new CommandData(0x89, createCollectionFromArray(paramEightNine), "Pit parking zone marking 2"));
        String[] paramEightA = {"Unknown", "Unknown", "Length of track markings?", "Unknown", "Unknown", "Unknown"};
        commandCollection.add(new CommandData(0x8A, createCollectionFromArray(paramEightA), "Track markings"));
        String[] paramEightB = {"Unknown", "Unknown", "Width of grid markings/length of grid slots?", "Unknown", "Unknown", "Unknown"};
        commandCollection.add(new CommandData(0x8B, createCollectionFromArray(paramEightB), "Starting grid markings"));
        String[] paramEightC = {"Unknown", "Length of unknown element change on the left?"};
        commandCollection.add(new CommandData(0x8C, createCollectionFromArray(paramEightC), "Changes unknown element on the left")); 
        String[] paramEightD = {"Unknown", "Length of unknown element change on the right?"};
        commandCollection.add(new CommandData(0x8D, createCollectionFromArray(paramEightD), "Changes unknown element on the right"));
        String[] paramEightE = {"Unknown", "Unknown", "Unknown"};
        commandCollection.add(new CommandData(0x8E, createCollectionFromArray(paramEightE), "Left kerb starts"));
        String[] paramEightF = {"Unknown", "Unknown", "Unknown"};
        commandCollection.add(new CommandData(0x8F, createCollectionFromArray(paramEightF), "Right kerb starts"));
        String[] paramNineZero = {"Unknown", "Unknown"};
        commandCollection.add(new CommandData(0x90, createCollectionFromArray(paramNineZero), "Reverse object placement?"));
        String[] paramNineOne = {"Unknown", "Unknown"};
        commandCollection.add(new CommandData(0x91, createCollectionFromArray(paramNineOne), "Unknown"));
        String[] paramNineTwo = {"Unknown", "Length of zone in which unknown action 1 occurs?"};
        commandCollection.add(new CommandData(0x92, createCollectionFromArray(paramNineTwo), "Segment in which unknown action 1 is taken?"));
        String[] paramNineThree = {"Unknown", "Length of zone in which unknown action 2 occurs?"};
        commandCollection.add(new CommandData(0x93, createCollectionFromArray(paramNineThree), "Segment in which unknown action 2 is taken?"));
        String[] paramNineFour = {"Unknown", "Left coaching factor? (1-8)"};
        commandCollection.add(new CommandData(0x94, createCollectionFromArray(paramNineFour), "Computer car coaching left"));
        String[] paramNineFive = {"Unknown", "Right coaching factor? (1-8)"};
        commandCollection.add(new CommandData(0x95, createCollectionFromArray(paramNineFive), "Computer car coaching right"));
        commandCollection.add(new CommandData(0x96, createCollectionFromArray("Unknown"), "Pitlane start"));
        commandCollection.add(new CommandData(0x97, createCollectionFromArray("Unknown"), "Pitlane end"));
        String[] paramNineEight = {"Unknown", "Left fence height modification amount (1-8)"};
        commandCollection.add(new CommandData(0x98, createCollectionFromArray(paramNineEight), "Left fence height change"));
        String[] paramNineNine = {"Unknown", "Right fence height modification amount (1-8)"};
        commandCollection.add(new CommandData(0x99, createCollectionFromArray(paramNineNine), "Right fence height change"));
        String[] paramNineA = {"Unknown", "Fence position in comparison to the track? (Left=1-8, Right=9-16)", "Fence height"};
        commandCollection.add(new CommandData(0x9A, createCollectionFromArray(paramNineA), "Custom fence height"));
        commandCollection.add(new CommandData(0x9B, createCollectionFromArray("Unknown"), "Unknown pitlane marker 1"));
        commandCollection.add(new CommandData(0x9C, createCollectionFromArray("Unknown"), "Unknown pitlane marker 2"));
        commandCollection.add(new CommandData(0x9D, createCollectionFromArray("Unknown"), "Unknown pitlane marker 3"));
        commandCollection.add(new CommandData(0x9E, createCollectionFromArray("Unknown"), "Unknown pitlane marker 4"));
        commandCollection.add(new CommandData(0x9F, createCollectionFromArray("Unknown"), "Pitlane fences starts"));
        commandCollection.add(new CommandData(0xA0, createCollectionFromArray("Unknown"), "Pitlane fences end"));
        commandCollection.add(new CommandData(0xA1, createCollectionFromArray("Unknown"), "Pit lane entry, join right pit lane fence"));
        commandCollection.add(new CommandData(0xA2, createCollectionFromArray("Unknown"), "Pit lane entry, join left pit lane fence"));
        commandCollection.add(new CommandData(0xA3, createCollectionFromArray("Unknown"), "Pit lane exit, join right pit lane fence"));
        commandCollection.add(new CommandData(0xA4, createCollectionFromArray("Unknown"), "Pit lane exit, join left pit lane fence"));
        commandCollection.add(new CommandData(0xA5, createCollectionFromArray("Unknown"), "Change the sign of the first two sector arguments"));
        String[] paramASix = {"Unknown", "Unknown", "Unknown"};
        commandCollection.add(new CommandData(0xA6, createCollectionFromArray(paramASix), "Set unknown segment flags 1"));
        String[] paramASeven = {"Unknown", "Unknown", "Unknown"};
        commandCollection.add(new CommandData(0xA7, createCollectionFromArray(paramASeven), "Set unknown segment flags 2"));
        commandCollection.add(new CommandData(0xA8, createCollectionFromArray("Unknown"), "Segment at which the chequered flag is shown."));
        String[] paramANine = {"Unknown", "Distance in which the pitlane can be seen? (Default=60)"};
        commandCollection.add(new CommandData(0xA9, createCollectionFromArray(paramANine), "View distance for pitlane (Optional)"));
        String[] paramAA = {"Unknown", "Length for computer cars to angle towards pitlane (backwards)?", "Length for computer cars to angle out of pits?", "Computer car pitlane speed?"};
        commandCollection.add(new CommandData(0xAA, createCollectionFromArray(paramAA), "Computer cars pitlane modifier?"));
        String[] paramAB = {"Unknown", "Unknown, will crash if set to 42", "Unknown"};
        commandCollection.add(new CommandData(0xAB, createCollectionFromArray(paramAB), "Unknown, may be unused"));
        String[] paramAC = {"Unknown", "Palette Index (0-255)", "Red Colour Value? (0-63?)", "Green Colour Value (0-63?)", "Blue Colour Value (0-63?)"};
        commandCollection.add(new CommandData(0xAC, createCollectionFromArray(paramAC), "Palette Change"));
        
    }
}
        
       
