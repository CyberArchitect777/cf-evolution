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
 * GameSettings.java
 *
 * Created on 11 February 2007, 01:16
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package chequeredflag.data.executable;

import java.io.*;

/**
 *
 * @author barrie
 */
public class GameSettings 
{
    
    private boolean qualifyingTyres;
        
    /**
     * Creates a new instance of GameOptions
     */
    public GameSettings() 
    {
        qualifyingTyres = false;
    }
    
    public boolean getQualifyingTyres()
    {
        return qualifyingTyres;
    }
    
    public void setQualifyingTyres(boolean newValue)
    {
        qualifyingTyres = newValue;
    }
    
    public int saveData(RandomAccessFile binaryAccess, int gameVersion)
    {
        byte[] qualifyingTyresData1 = new byte[2];
        byte[] qualifyingTyresData2 = new byte[1];
        if (qualifyingTyres == true)
        {
            qualifyingTyresData1[0] = (byte)0x80;
            qualifyingTyresData1[1] = 0x75;
            qualifyingTyresData2[0] = 0x40;
        }
        else
        {
            qualifyingTyresData1[0] = 0x40;
            qualifyingTyresData1[1] = 0x74;
            qualifyingTyresData2[0] = 0x04;
        }
        BinaryManager.setDataBytes(gameVersion, binaryAccess, 48808, 48808, 48808, qualifyingTyresData1);
        BinaryManager.setDataBytes(gameVersion, binaryAccess, 48835, 48835, 48835, qualifyingTyresData2);
        return 0;
    }
    
    public int loadData(RandomAccessFile binaryAccess, int gameVersion)
    {
        //byte[] dataValue = new byte[3];
        byte[] dataValues;
        qualifyingTyres = false;
        dataValues = BinaryManager.getDataBytes(gameVersion, binaryAccess, 48808, 48808, 48808, 1);
        if (dataValues[0] == (byte)0x80)
        {
            qualifyingTyres = true;
        }
        else
        {
            qualifyingTyres = false;
        }
        return 0;
    }       
}
