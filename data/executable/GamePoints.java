
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
 * GamePoints.java
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
public class GamePoints 
{
    
    private int[] gamePoints;
        
    /**
     * Creates a new instance of GameOptions
     */
    public GamePoints() 
    {
        gamePoints = new int[26];
        for (int x=0;x<26;x++)
        {
            gamePoints[x] = 0;
        }
    }
    
    public int getPointValue(int pointIndex)
    {
        return gamePoints[pointIndex];
    }
    
    public void setPointValue(int pointIndex, int pointValue)
    {
        gamePoints[pointIndex] = pointValue;
    }
    
    public int saveData(RandomAccessFile binaryAccess, int gameVersion)
    {
        byte[] pointsData = new byte[26];
        for (int x=0;x<26;x++)
        {
            pointsData[x] = (byte)gamePoints[x];
        }
        BinaryManager.setDataBytes(gameVersion, binaryAccess, 183940, 183876, 183892, pointsData);
        return 0;
    }
    
    public int loadData(RandomAccessFile binaryAccess, int gameVersion)
    {
        //byte[] dataValue = new byte[3];
        byte[] dataValues;
        dataValues = BinaryManager.getDataBytes(gameVersion, binaryAccess, 183940, 183876, 183892, 26);
        for (int x=0;x<dataValues.length;x++)
        {
            gamePoints[x] = (int)dataValues[x];
        }
        return 0;
    }       
}
