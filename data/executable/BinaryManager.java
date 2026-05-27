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
 * BinaryManager.java
 *
 * Created on 01 January 2007, 03:22
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
public class BinaryManager 
{
    
    /** Creates a new instance of BinaryManager */
    public BinaryManager() 
    {
    }
    
    public static void setDataBytes(int gameVersion, RandomAccessFile openFile, int europeanVersion, int italianVersion, int usVersion, byte[] dataValues)
    {
        try
        {
            switch (gameVersion)
            {
                case 1: openFile.seek(europeanVersion); /*System.out.println("Seeking to: " + europeanVersion);*/ break;
                case 2: openFile.seek(italianVersion); /*System.out.println("Seeking to: " + italianVersion);*/ break;
                case 3: openFile.seek(usVersion); /*System.out.println("Seeking to: " + usVersion);*/ break;
            }
            for (int x=0;x<dataValues.length;x++)
            {
                //System.out.println("Writing value: " + dataValues[x]);
                openFile.write(dataValues[x]);
            }
        }
        catch (Exception exceptionError)
        {
            exceptionError.printStackTrace();
        }
    }
    
    public static byte[] getDataBytes(int gameVersion, RandomAccessFile openFile, int europeanVersion, int italianVersion, int usVersion, int numberBytes)
    {
        byte[] dataValues = new byte[numberBytes];
        try
        {
            for (int x=0;x<numberBytes;x++)
            {
                switch (gameVersion)
                {
                    case 1: openFile.seek(europeanVersion+x); /*System.out.println("Seeking to: " + europeanVersion+x);*/ break;
                    case 2: openFile.seek(italianVersion+x); /*System.out.println("Seeking to: " + italianVersion+x);*/ break;
                    case 3: openFile.seek(usVersion+x); /*System.out.println("Seeking to: " + usVersion+x);*/ break;
                }
                dataValues[x] = (byte)openFile.read();
                //System.out.println("Reading value: " + dataValues[x]);
                //int unsignedByte = (0x000000FF & dataValue); // Convert signed to unsigned byte (still inside integer)
                //short convertedByte = (short)unsignedByte; // Move the integer type to a short
            }
            return dataValues;
        }
        catch (Exception exceptionError)
        {
            exceptionError.printStackTrace();
            byte[] emptyValue = new byte[1];
            emptyValue[0] = 0x00;
            return emptyValue;
        }
    }    
}
