
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
 * GameDrivers.java
 *
 * Created on 24 February 2007, 00:15
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package chequeredflag.data.executable;

import java.io.*;
import java.util.Vector;

/**
 *
 * @author barrie
 */
public class GameDrivers 
{
    
    private Vector driverNames;
    private int[] qualifyingGrip;
    private int[] raceGrip;
        
    /** Creates a new instance of GameDrivers */
    public GameDrivers() 
    {
        driverNames = new Vector();
        qualifyingGrip = new int[40];
        raceGrip = new int[40];
        for (int x=0;x<40;x++)
        {
            qualifyingGrip[x] = 0;
            raceGrip[x] = 0;
        }
        
    }
    
    public String getDriverName(int nameIndex)
    {
        return (String)driverNames.get(nameIndex);
    }
    
    public int getQualifyingGrip(int gripIndex)
    {
        return qualifyingGrip[gripIndex];
    }
    
    public int getRaceGrip(int gripIndex)
    {
        return raceGrip[gripIndex];
    }
    
    public void setQualifyingGrip(int gripIndex, int gripValue)
    {
        qualifyingGrip[gripIndex] = gripValue;
    }
    
    public void setRaceGrip(int gripIndex, int gripValue)
    {
        raceGrip[gripIndex] = gripValue;
    }
    
    public Vector getAllDriverNames()
    {
        return driverNames;
    }
    
    public void setDriverName(int nameIndex, String nameString)
    {
        driverNames.set(nameIndex, nameString);
    }
    
    public int saveData(RandomAccessFile binaryAccess, int gameVersion)
    {
        byte[] driverQualifyingData = new byte[40];
        byte[] driverRaceData = new byte[40];
        for (int x=0;x<40;x++)
        {
            driverQualifyingData[x] = (byte)qualifyingGrip[x];
            driverRaceData[x] = (byte)raceGrip[x];
        }
        BinaryManager.setDataBytes(gameVersion, binaryAccess, 184036, 183972, 183988, driverQualifyingData);
        BinaryManager.setDataBytes(gameVersion, binaryAccess, 184076, 184012, 184028, driverRaceData);
        byte[] driverByteName = new byte[23];
        int currentOffset = 0;
        for (int x=0;x<40;x++)
        {
            StringBuffer currentString = new StringBuffer((String)driverNames.get(x));
            for (int y=0;y<23;y++)
            {
                if (y < currentString.length())
                {
                    driverByteName[y] = (byte)currentString.charAt(y);
                }
                else
                {
                    driverByteName[y] = (byte)0x00;
                }
            }
            BinaryManager.setDataBytes(gameVersion, binaryAccess, 180250+currentOffset, 180186+currentOffset, 180202+currentOffset, driverByteName);
            currentOffset = currentOffset + 24;            
        }        
        return 0;
    }
    
    public int loadData(RandomAccessFile binaryAccess, int gameVersion)
    {
        
        // Detected grip data European = offset 184036, offset 184076
        // Detected grip data Italian = offset 183972, offset 184012
        // Detected grip data US = offset 183988, offset 184028 
        
        byte[] dataValues;
        int[] startingValue = new int[3];
        startingValue[0] = 180250; // European version
        startingValue[1] = 180186; // Italian version
        startingValue[2] = 180202; // US version
        int currentOffset = 0;
        for (int x=0;x<40;x++)
        {
            int currentByteCount = 0;
            System.out.println(x);
            boolean readEnd = false;
            StringBuffer currentString = new StringBuffer();
            dataValues = BinaryManager.getDataBytes(gameVersion, binaryAccess, startingValue[0]+currentOffset, startingValue[1]+currentOffset, startingValue[2]+currentOffset, 23);
            while (readEnd == false)
            {
                if ((dataValues[currentByteCount] != 0x00) && (currentByteCount < 23))
                {
                    char currentCharacter = (char)dataValues[currentByteCount];
                    currentString.append(currentCharacter);
                }
                else
                {
                    readEnd = true;
                    driverNames.add(currentString.substring(0,currentString.length()));
                }
                currentByteCount++;
            }
            currentOffset = currentOffset + 24;
        }
        dataValues = BinaryManager.getDataBytes(gameVersion, binaryAccess, 184036, 183972, 183988, 40);
        for (int x=0;x<40;x++)
        {
            qualifyingGrip[x] = (int)dataValues[x];
        }
        dataValues = BinaryManager.getDataBytes(gameVersion, binaryAccess, 184076, 184012, 184028, 40);
        for (int x=0;x<40;x++)
        {
            raceGrip[x] = (int)dataValues[x];
        }        
        return 0;
    }
}
