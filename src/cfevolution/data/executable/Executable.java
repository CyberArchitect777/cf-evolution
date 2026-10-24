/*
 * CF Evolution: An editor for Formula One Grand Prix/World Circuit
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

package cfevolution.data.executable;

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
    private GameLiveries gameLiveries;
    private GameTables gameTables;
    
    /** Creates a new instance of Executable */
    public Executable(File loadedFile)
    {
        gameFile = loadedFile;
        gameVersion = identify(gameFile);
        
        gameOptions = new GameOptions();
        gameSettings = new GameSettings();
        gamePoints = new GamePoints();
        gameDrivers = new GameDrivers();
        gameTeams = new GameTeams();
        gameLiveries = new GameLiveries();
        gameTables = new GameTables();
    }
    
    /*
     * Builds are recognised by their CONTENT (2026-10-24), not the file size,
     * so a file another tool has extended still opens and a different file
     * that happens to have the right size does not. Two fingerprints no tool
     * edits must both be where the build puts them:
     *  - the start of InitNumRaceLaps in seg001, which sits at a different
     *    place in each build (`shr ax,1 / cmp word [bp+1226h],5 / jnz / mov`);
     *  - wNumTeams, the unnamed word and wHighestCarId (18, 17, 35) in seg004,
     *    at the data offsets every table here uses (IT -64, US -48).
     * The packed builds compress those tables, so they are still told apart by
     * size, only to explain that they cannot be edited.
     */
    private static final byte[] CODE_PRINT = { (byte)0xD1, (byte)0xE8, (byte)0x81, (byte)0xBE, 0x26, 0x12, 0x05, 0x00, 0x75, 0x03, (byte)0xB8 };
    private static final int[] CODE_AT = { 112168, 112124, 112132 };
    private static final byte[] DATA_PRINT = { 0x12, 0x00, 0x11, 0x00, 0x23, 0x00 };
    private static final int[] DATA_AT = { 183970, 183906, 183922 };
    private static final String[] BUILD_NAMES = { "European", "Italian", "US" };

    /** 1-3 unpacked EU/IT/US, 4-6 packed EU/IT/US, 7 not recognised. */
    public static int identify(File file)
    {
        try
        {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            try
            {
                for (int build = 0; build < 3; build++)
                    if (matches(raf, CODE_AT[build], CODE_PRINT) && matches(raf, DATA_AT[build], DATA_PRINT))
                    {
                        System.out.println("1.05 " + BUILD_NAMES[build] + " unpacked version detected");
                        return build + 1;
                    }
            }
            finally
            {
                raf.close();
            }
        }
        catch (IOException e)
        {
            return 7;
        }
        switch ((int) file.length())
        {
            case 321878: return 4;
            case 321748: return 5;
            case 321716: return 6;
            default: return 7;
        }
    }

    private static boolean matches(RandomAccessFile raf, int offset, byte[] print) throws IOException
    {
        if (raf.length() < offset + print.length)
            return false;
        byte[] read = new byte[print.length];
        raf.seek(offset);
        raf.readFully(read);
        return java.util.Arrays.equals(read, print);
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

    public GameLiveries getGameLiveries()
    {
        return gameLiveries;
    }

    public GameTables getGameTables()
    {
        return gameTables;
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
            operationSuccess = operationSuccess + gameLiveries.saveData(binaryAccess, gameVersion);
            operationSuccess = operationSuccess + gameTables.saveData(binaryAccess, gameVersion);
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
                    operationSuccess = operationSuccess + gameLiveries.loadData(binaryAccess, gameVersion);
                    operationSuccess = operationSuccess + gameTables.loadData(binaryAccess, gameVersion);
                    binaryAccess.close();
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
