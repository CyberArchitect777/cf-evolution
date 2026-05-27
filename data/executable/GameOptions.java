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
 * GameOptions.java
 *
 * Created on 01 January 2007, 03:16
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
public class GameOptions 
{
    
    private boolean fastFade;
    private boolean languageSelection;
    
    /**
     * Creates a new instance of GameOptions
     */
    public GameOptions() 
    {
        fastFade = false;
        languageSelection = false;
    }
    
    public boolean getFastFade()
    {
        return fastFade;
    }
    
    public boolean getLanguageSelection()
    {
        return languageSelection;
    }
    
    public void setFastFade(boolean newValue)
    {
        fastFade = newValue;
    }
    
    public void setLanguageSelection(boolean newValue)
    {
        languageSelection = newValue;
    }
    
    public int saveData(RandomAccessFile binaryAccess, int gameVersion)
    {
        byte[] fastFadeData = new byte[10];
        if (fastFade == true)
        {
            for (int x=0;x<10;x++)
            {
                fastFadeData[x] = (byte)0x90;
            }
        }
        else
        {
            fastFadeData[0] = (byte)0xEC;
            fastFadeData[1] = (byte)0xA8;
            fastFadeData[2] = 0x08;
            fastFadeData[3] = 0x75;
            fastFadeData[4] = (byte)0xFB;
            fastFadeData[5] = (byte)0xEC;
            fastFadeData[6] = (byte)0xA8;
            fastFadeData[7] = 0x08;
            fastFadeData[8] = 0x74;
            fastFadeData[9] = (byte)0xFB;           
        }
        BinaryManager.setDataBytes(gameVersion, binaryAccess, 578506, 578362, 578346, fastFadeData);
        byte[] languageSelectionData1 = new byte[1];
        byte[] languageSelectionData2 = new byte[1];
        byte[] languageSelectionData3 = new byte[1];
        if (languageSelection == true)
        {
            languageSelectionData1[0] = 0x00;
            languageSelectionData2[0] = 0x00;
            languageSelectionData3[0] = 0x00;
        }
        else
        {
            if ((gameVersion == 2) || (gameVersion == 5))
            {
                languageSelectionData1[0] = 0x7D;
            }
            else
            {
                languageSelectionData1[0] = (byte)0xA8;
            }
            languageSelectionData2[0] = 0x2C;
            languageSelectionData3[0] = 0x1F;                    
        }
        BinaryManager.setDataBytes(gameVersion, binaryAccess, 433528, 433414, 433480, languageSelectionData1);
        BinaryManager.setDataBytes(gameVersion, binaryAccess, 433538, 433474, 433490, languageSelectionData2);
        BinaryManager.setDataBytes(gameVersion, binaryAccess, 433555, 433491, 433507, languageSelectionData3);
        return 0;
    }
    
    public int loadData(RandomAccessFile binaryAccess, int gameVersion)
    {
        //byte[] dataValue = new byte[3];
        byte[] dataValues;
        dataValues = BinaryManager.getDataBytes(gameVersion, binaryAccess, 578506, 578362, 578346, 1);
        System.out.println(dataValues);
        if (dataValues[0] == (byte)0x90)
        {
            fastFade = true;
        }
        else
        {
            fastFade = false;
        }
        dataValues = BinaryManager.getDataBytes(gameVersion, binaryAccess, 433538, 433474, 433490, 1);
        if (dataValues[0] == 0x00)
        {
            languageSelection = true;
        }
        else
        {
            languageSelection = false;
        }
        return 0;
    }       
}
