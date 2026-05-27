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
 * Executable.java
 *
 * Created on 30 December 2006, 20:25
 */

package chequeredflag.data.executable;

import java.io.*;

/**
 *
 * @author barrie
 */
public class Executable 
{
    
    private int gameVersion;
    private File gameFile;
    private GameOptions gameOptions;
    private GameSettings gameSettings;
    private GamePoints gamePoints;
    private GameDrivers gameDrivers;
    private GameTeams gameTeams;
    
    /** Creates a new instance of Executable */
    public Executable(File loadedFile)
    {
        gameFile = loadedFile;
        int executableSize = (int)gameFile.length();     
                
        switch(executableSize)
        {
            case 600480: gameVersion = 1; System.out.println("1.05 European unpacked version detected"); break;
            case 600336: gameVersion = 2; System.out.println("1.05 Italian unpacked version detected"); break;
            case 600320: gameVersion = 3; System.out.println("1.05 US unpacked version detected"); break;
            case 321878: gameVersion = 4; System.out.println("1.05 European packed version detected"); break;
            case 321748: gameVersion = 5; System.out.println("1.05 Italian packed version detected"); break;
            case 321716: gameVersion = 6; System.out.println("1.05 US packed version detected"); break;
            default: gameVersion = 7; break;
        }    
        
        gameOptions = new GameOptions();
        gameSettings = new GameSettings();
        gamePoints = new GamePoints();
        gameDrivers = new GameDrivers();
        gameTeams = new GameTeams();
    }
    
    public int returnGameVersionID()
    {
        return gameVersion;
    }
    
    public String returnGameVersionString()
    {
        switch (gameVersion)
        {
            case 1: return "1.05 European version";
            case 2: return "1.05 Italian version";
            case 3: return "1.05 US version";
            default: return "Unknown version";
        }
    }   
    
    public GameOptions getGameOptions()
    {
        return gameOptions;
    }
    
    public GameSettings getGameSettings()
    {
        return gameSettings;
    }
    
    public GamePoints getGamePoints()
    {
        return gamePoints;
    }
    
    public GameDrivers getGameDrivers()
    {
        return gameDrivers;
    }
    
    public GameTeams getGameTeams()
    {
        return gameTeams;
    }
    
    public boolean saveData()
    {
        // Saves data back into a F1GP/WC executable file. Returns a code to indicate the completed saving status
        // Code True - Success, False - Failure
        
        String filePath = gameFile.getPath();
        try
        {
            RandomAccessFile binaryAccess = new RandomAccessFile(filePath, "rw");
            int operationSuccess = 0;
            operationSuccess = operationSuccess + gameOptions.saveData(binaryAccess, gameVersion);
            operationSuccess = operationSuccess + gameSettings.saveData(binaryAccess, gameVersion);
            operationSuccess = operationSuccess + gamePoints.saveData(binaryAccess, gameVersion);
            operationSuccess = operationSuccess + gameDrivers.saveData(binaryAccess, gameVersion);
            operationSuccess = operationSuccess + gameTeams.saveData(binaryAccess, gameVersion);
            binaryAccess.close();
            if (operationSuccess == 0)
            {
                return true;
            }
            else
            {
                return false;
            }            
        }
        catch (Exception exceptionError)
        {
            exceptionError.printStackTrace();
            return false;
        }            
        
    }
    
    public int loadData()
    {
        // Loads an F1GP/WC executable file. Returns a code to indicate the completed loading status
        // Code: 0 - Success, 1 - Invalid File, 2 - Unknown Error, 3 - Packed Version
        
        if (gameVersion > 6)
        {
            return 1;
        }
        else
        {
            if (gameVersion > 3)
            {
                return 3;
            }
            else
            {
                String filePath = gameFile.getPath();
                try
                {
                    RandomAccessFile binaryAccess = new RandomAccessFile(filePath, "r");
                    int operationSuccess = 0;
                    operationSuccess = operationSuccess + gameOptions.loadData(binaryAccess, gameVersion);
                    operationSuccess = operationSuccess + gameSettings.loadData(binaryAccess, gameVersion);
                    operationSuccess = operationSuccess + gamePoints.loadData(binaryAccess, gameVersion);
                    operationSuccess = operationSuccess + gameDrivers.loadData(binaryAccess, gameVersion);
                    operationSuccess = operationSuccess + gameTeams.loadData(binaryAccess, gameVersion);
                    if (operationSuccess == 0)
                    {
                        return 0;
                    }
                    else
                    {
                        return 2;
                    }
                }
                catch (Exception exceptionError)
                {
                    return 2;
                }            
            }
        }
    }    
}
