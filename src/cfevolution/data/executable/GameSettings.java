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
 * GameSettings.java
 *
 * Created on 11 February 2007, 01:16
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package cfevolution.data.executable;

import java.io.*;

/**
 * Race rules and settings held in the game executable: qualifying tyres,
 * the player's engine power, AI grip, the weather, damage and the race
 * control timers.
 *
 * Offsets follow ArgData's GpExeFile and were checked byte for byte in all
 * three unpacked builds. The code sites (power, grip, rain, timers) do not
 * move between builds; the damage table is data and moves with the other
 * tables (Italian -64, US -48). Every value is kept as the raw word read
 * from the file, so a value the user leaves alone is written back unchanged.
 *
 * @author barrie
 */
public class GameSettings 
{
    // `mov cx,4000h` in GetPlayerCarPower: raw = 22 * BHP + 632 (716 BHP = 4000h)
    private static final int PLAYER_POWER = 24600;
    // 5 bytes in the AI grip set-up: stock `jns +3 / mov ax,4000h`, or the
    // ArgData patch `nop / add ax,n` that gives every car a fixed grip level
    private static final int AI_GRIP = 24935;
    private static final byte[] AI_GRIP_STOCK = { 0x79, 0x03, (byte)0xB8, 0x00, 0x40 };
    private static final byte[] AI_GRIP_PATCH = { (byte)0x90, (byte)0x81, (byte)0xC0 };
    // `cmp ax,0F0h` in GetRandomWeather: a random byte at or above it is wet
    private static final int RAIN_CHANCE = 63146;
    // high byte of `test word [0174h],4000h`: 40h keeps dry the circuits
    // whose track file asks for it (Phoenix); 0 ignores that request
    private static final int RAIN_EVERYWHERE = 111746;
    // word immediate of `cmp word [bp+1226h],4Bh` in InitCarPitStopStrategy:
    // computer cars only plan a pit stop in a race of at least this % distance
    private static final int PIT_STOP_DISTANCE = 24867;
    // millisecond immediates in the race control code
    private static final int YELLOW_FLAG_MS = 28269;
    private static final int RETIRED_REMOVED_MS = 28383;
    // damage thresholds (European offsets; the word at +4 is not edited)
    private static final int RETIRE_WALL = 131948;
    private static final int RETIRE_CAR = 131950;
    private static final int DAMAGE_CAR = 131954;
    private static final int DAMAGE_WALL = 131956;

    private boolean qualifyingTyres;
    private int playerPowerRaw;
    private int aiGripLevel;            // 1 = the game's own setting, 0 = not recognised
    private int aiGripLevelLoaded;
    private int rainChanceRaw;
    private int rainEverywhereByte;
    private int pitStopDistance;
    private int yellowFlagMs;
    private int retiredRemovedMs;
    private int retireAfterWall;
    private int retireAfterCar;
    private int damageAfterWall;
    private int damageAfterCar;
        
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

    /** The player's engine power in BHP (716 in the original game). */
    public int getPlayerPower()
    {
        return (playerPowerRaw - 632) / 22;
    }

    public void setPlayerPower(int bhp)
    {
        if (bhp != getPlayerPower())
            playerPowerRaw = bhp * 22 + 632;
    }

    /** 1 is the game's own behaviour; 2-100 is a fixed grip level for
        every car. 0 when the code there was changed by another tool. */
    public int getAIGripLevel()
    {
        return aiGripLevel;
    }

    public void setAIGripLevel(int level)
    {
        aiGripLevel = level;
    }

    /** Chance of rain at a race, in percent (6 in the original game). */
    public int getRainChance()
    {
        return (int) Math.round(100.0 * (256 - rainChanceRaw) / 256.0);
    }

    public void setRainChance(int percent)
    {
        // Rounded down, so 6% gives the original 240 back (ArgData rounds
        // to nearest and writes 241); every percentage still reads back as set
        if (percent != getRainChance())
            rainChanceRaw = (100 - percent) * 256 / 100;
    }

    /** True when it may also rain at circuits that normally stay dry. */
    public boolean getRainEverywhere()
    {
        return rainEverywhereByte == 0;
    }

    public void setRainEverywhere(boolean newValue)
    {
        if (newValue != getRainEverywhere())
            rainEverywhereByte = newValue ? 0x00 : 0x40;
    }

    /** Shortest race, as % of full distance, in which computer cars plan a pit stop. */
    public int getPitStopRaceDistance()
    {
        return pitStopDistance;
    }

    public void setPitStopRaceDistance(int percent)
    {
        pitStopDistance = percent;
    }

    public int getYellowFlagSeconds()
    {
        return yellowFlagMs / 1000;
    }

    public void setYellowFlagSeconds(int seconds)
    {
        if (seconds != getYellowFlagSeconds())
            yellowFlagMs = seconds * 1000;
    }

    public int getRetiredRemovedSeconds()
    {
        return retiredRemovedMs / 1000;
    }

    public void setRetiredRemovedSeconds(int seconds)
    {
        if (seconds != getRetiredRemovedSeconds())
            retiredRemovedMs = seconds * 1000;
    }

    public int getRetireAfterWall() { return retireAfterWall; }
    public int getRetireAfterCar() { return retireAfterCar; }
    public int getDamageAfterWall() { return damageAfterWall; }
    public int getDamageAfterCar() { return damageAfterCar; }
    public void setRetireAfterWall(int value) { retireAfterWall = value; }
    public void setRetireAfterCar(int value) { retireAfterCar = value; }
    public void setDamageAfterWall(int value) { damageAfterWall = value; }
    public void setDamageAfterCar(int value) { damageAfterCar = value; }

    private static byte[] word(int value)
    {
        return new byte[] { (byte) value, (byte) (value >> 8) };
    }

    private static int readWord(RandomAccessFile binaryAccess, int gameVersion, int europeanOffset, int shift)
    {
        int italian = shift == 0 ? europeanOffset : europeanOffset - 64;
        int us = shift == 0 ? europeanOffset : europeanOffset - 48;
        byte[] data = BinaryManager.getDataBytes(gameVersion, binaryAccess, europeanOffset, italian, us, 2);
        return (data[0] & 0xFF) | ((data[1] & 0xFF) << 8);
    }

    private static void writeWord(RandomAccessFile binaryAccess, int gameVersion, int europeanOffset, int shift, int value)
    {
        int italian = shift == 0 ? europeanOffset : europeanOffset - 64;
        int us = shift == 0 ? europeanOffset : europeanOffset - 48;
        BinaryManager.setDataBytes(gameVersion, binaryAccess, europeanOffset, italian, us, word(value));
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

        writeWord(binaryAccess, gameVersion, PLAYER_POWER, 0, playerPowerRaw);
        // Only rewrite the grip code when it was changed here, so a patch
        // from another tool that is not recognised is left alone
        if (aiGripLevel != aiGripLevelLoaded && aiGripLevel >= 1)
        {
            if (aiGripLevel == 1)
                BinaryManager.setDataBytes(gameVersion, binaryAccess, AI_GRIP, AI_GRIP, AI_GRIP, AI_GRIP_STOCK);
            else
            {
                byte[] patch = new byte[5];
                System.arraycopy(AI_GRIP_PATCH, 0, patch, 0, 3);
                System.arraycopy(word(aiGripLevel * 100 - 100), 0, patch, 3, 2);
                BinaryManager.setDataBytes(gameVersion, binaryAccess, AI_GRIP, AI_GRIP, AI_GRIP, patch);
            }
            aiGripLevelLoaded = aiGripLevel;
        }
        writeWord(binaryAccess, gameVersion, RAIN_CHANCE, 0, rainChanceRaw);
        BinaryManager.setDataBytes(gameVersion, binaryAccess, RAIN_EVERYWHERE, RAIN_EVERYWHERE, RAIN_EVERYWHERE,
                                   new byte[] { (byte) rainEverywhereByte });
        writeWord(binaryAccess, gameVersion, PIT_STOP_DISTANCE, 0, pitStopDistance);
        writeWord(binaryAccess, gameVersion, YELLOW_FLAG_MS, 0, yellowFlagMs);
        writeWord(binaryAccess, gameVersion, RETIRED_REMOVED_MS, 0, retiredRemovedMs);
        writeWord(binaryAccess, gameVersion, RETIRE_WALL, 1, retireAfterWall);
        writeWord(binaryAccess, gameVersion, RETIRE_CAR, 1, retireAfterCar);
        writeWord(binaryAccess, gameVersion, DAMAGE_CAR, 1, damageAfterCar);
        writeWord(binaryAccess, gameVersion, DAMAGE_WALL, 1, damageAfterWall);
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

        playerPowerRaw = readWord(binaryAccess, gameVersion, PLAYER_POWER, 0);
        dataValues = BinaryManager.getDataBytes(gameVersion, binaryAccess, AI_GRIP, AI_GRIP, AI_GRIP, 5);
        if (java.util.Arrays.equals(dataValues, AI_GRIP_STOCK))
            aiGripLevel = 1;
        else if (dataValues[0] == AI_GRIP_PATCH[0] && dataValues[1] == AI_GRIP_PATCH[1] && dataValues[2] == AI_GRIP_PATCH[2])
            aiGripLevel = ((dataValues[3] & 0xFF) | ((dataValues[4] & 0xFF) << 8)) / 100 + 1;
        else
            aiGripLevel = 0;
        aiGripLevelLoaded = aiGripLevel;
        rainChanceRaw = readWord(binaryAccess, gameVersion, RAIN_CHANCE, 0);
        rainEverywhereByte = BinaryManager.getDataBytes(gameVersion, binaryAccess,
            RAIN_EVERYWHERE, RAIN_EVERYWHERE, RAIN_EVERYWHERE, 1)[0] & 0xFF;
        pitStopDistance = readWord(binaryAccess, gameVersion, PIT_STOP_DISTANCE, 0);
        yellowFlagMs = readWord(binaryAccess, gameVersion, YELLOW_FLAG_MS, 0);
        retiredRemovedMs = readWord(binaryAccess, gameVersion, RETIRED_REMOVED_MS, 0);
        retireAfterWall = readWord(binaryAccess, gameVersion, RETIRE_WALL, 1);
        retireAfterCar = readWord(binaryAccess, gameVersion, RETIRE_CAR, 1);
        damageAfterCar = readWord(binaryAccess, gameVersion, DAMAGE_CAR, 1);
        damageAfterWall = readWord(binaryAccess, gameVersion, DAMAGE_WALL, 1);
        return 0;
    }       
}
