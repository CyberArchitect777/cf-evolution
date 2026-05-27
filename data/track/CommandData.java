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
 * CommandData.java
 *
 * Created on 14 August 2006, 04:02
 */

package chequeredflag.data.track;

import java.util.ArrayList;

/**
 *
 * @author barrie
 */
public class CommandData {
    
    private int commandType;
    private ArrayList paramInfo;
    private String commandDesc;
    
        
    /** Creates a new instance of CommandData */
    public CommandData(int newCommandType, ArrayList newParamInfo, String newCommandDesc)
    {
        // Constructor. Initialises a new CommandData object.
        
        commandType = newCommandType;
        paramInfo = newParamInfo;
        commandDesc = newCommandDesc;
    }
    
    public int getNumberOfParams()
    {
        // Returns the number of parameters contained by this command.
        
        return paramInfo.size();
    }
    
    public int getCommandType()
    {
        // Returns an integer value of the command type.
        
        return commandType;
    }
    
    public String getParamInfo(int paramIndex)
    {
        // Returns the specified command parameter.
        
        return (String)paramInfo.get(paramIndex);
    }
    
    public String[] getParamArray()
    {
        // Returns a string array containing all command parameter information.
        
        String[] paramText = new String[paramInfo.size()];
        for (int x=0;x<paramInfo.size();x++)
        {
            paramText[x] = (String)paramInfo.get(x);
        }
        
        return paramText;        
    }
    
    public String getCommandDesc()
    {
        // Returns the command description.
        
        return commandDesc;
    }
    
}
