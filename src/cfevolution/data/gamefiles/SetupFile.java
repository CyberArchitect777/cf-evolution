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
package cfevolution.data.gamefiles;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * A setup file saved from the game's setup screen. A single setup is 14
 * bytes; a whole season's is 326: a word (FFFFh when race and qualifying
 * setups are kept separate), 16 qualifying setups, then 16 race setups,
 * one per circuit (ArgDocs, setups). Each setup is 10 raw bytes: front
 * wing, rear wing, six gear ratios, tyre compound (0-3 = A-D) and a signed
 * brake balance. The game saves them under any name, so they are recognised
 * by size and checksum.
 *
 * @author barrie
 */
public class SetupFile extends GameFile
{
    public static final int SINGLE_SIZE = 14, SEASON_SIZE = 326, SETUP_SIZE = 10;
    public static final int FRONT_WING = 0, REAR_WING = 1, GEAR_1 = 2, TYRES = 8, BRAKE_BALANCE = 9;

    /** The game's own limits, from its setup screen's sliders (GP.EXE 1.05 EU, seg008 menu data around
        IDA 0x66D16: each slider's minimum and maximum at +16h/+17h). Wings 0-64, every gear 16-80 and
        above the gear before it and below the one after, tyres A-D (four buttons), brake balance -32 to
        32. Read 2026-11-05 after Hrvoje found wings allowed up to 255. */
    public static final int WING_MAX = 64, GEAR_MIN = 16, GEAR_MAX = 80, BALANCE_LIMIT = 32, COMPOUND_COUNT = 4;
    public static final String[] COMPOUNDS = { "A", "B", "C", "D" };

    /**
     * Why the game would not accept this value for this field of a 10-byte setup, in plain words, or
     * null if it would. The setup's other gears are taken as they are, as the game's screen does.
     */
    public static String problem(int[] setup, int field, int value)
    {
        if (field == FRONT_WING || field == REAR_WING)
            return value < 0 || value > WING_MAX ? "Wings must be from 0 to " + WING_MAX + "." : null;
        if (field == BRAKE_BALANCE)
            return value < -BALANCE_LIMIT || value > BALANCE_LIMIT
                ? "Brake balance must be from -" + BALANCE_LIMIT + " to " + BALANCE_LIMIT + "." : null;
        if (field == TYRES)
            return value < 0 || value >= COMPOUND_COUNT ? "Tyres must be A, B, C or D." : null;
        if (value < GEAR_MIN || value > GEAR_MAX)
            return "Gears must be from " + GEAR_MIN + " to " + GEAR_MAX + ".";
        int gear = field - GEAR_1 + 1;
        if (gear > 1 && value <= setup[field - 1])
            return "Gear " + gear + " must be higher than gear " + (gear - 1) + " (" + setup[field - 1] + "), as in the game's setup screen.";
        if (gear < 6 && value >= setup[field + 1])
            return "Gear " + gear + " must be lower than gear " + (gear + 1) + " (" + setup[field + 1] + "), as in the game's setup screen.";
        return null;
    }

    private SetupFile(byte[] data)
    {
        this.data = data;
    }

    /** A new, empty setup file: one setup, or a season's (checksums are set when it is saved). */
    public static SetupFile create(boolean season)
    {
        return new SetupFile(new byte[season ? SEASON_SIZE : SINGLE_SIZE]);
    }

    public static SetupFile load(File file) throws IOException
    {
        long size = file.length();
        return new SetupFile(read(file, size == SEASON_SIZE ? SEASON_SIZE : SINGLE_SIZE));
    }

    /** True if the file looks like a setup file of either kind. */
    public static boolean isSetupFile(File file)
    {
        long size = file.length();
        if (size != SINGLE_SIZE && size != SEASON_SIZE)
            return false;
        try
        {
            return checksumOk(Files.readAllBytes(file.toPath()));
        }
        catch (IOException e)
        {
            return false;
        }
    }

    public boolean isSeason()
    {
        return data.length == SEASON_SIZE;
    }

    /** 1 for a single setup, 32 for a season (16 qualifying, then 16 race). */
    public int getSetupCount()
    {
        return isSeason() ? 32 : 1;
    }

    private int at(int setup, int field)
    {
        return (isSeason() ? 2 : 0) + setup * SETUP_SIZE + field;
    }

    /** A field's stored value; the brake balance is signed. */
    public int get(int setup, int field)
    {
        int v = getByte(at(setup, field));
        return field == BRAKE_BALANCE ? (byte) v : v;
    }

    public void set(int setup, int field, int value)
    {
        setByte(at(setup, field), value);
    }

    public boolean isSeparateRaceSetups()
    {
        return isSeason() && getWord(0) != 0;
    }

    public void setSeparateRaceSetups(boolean separate)
    {
        setWord(0, separate ? 0xFFFF : 0);
    }
}
