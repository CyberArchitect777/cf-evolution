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
 * A set-up file saved from the game's set-up screen. A single set-up is 14
 * bytes; a whole season's is 326: a word (FFFFh when race and qualifying
 * set-ups are kept separate), 16 qualifying set-ups, then 16 race set-ups,
 * one per circuit (ArgDocs, setups). Each set-up is 10 raw bytes: front
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

    private SetupFile(byte[] data)
    {
        this.data = data;
    }

    public static SetupFile load(File file) throws IOException
    {
        long size = file.length();
        return new SetupFile(read(file, size == SEASON_SIZE ? SEASON_SIZE : SINGLE_SIZE));
    }

    /** True if the file looks like a set-up file of either kind. */
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

    /** 1 for a single set-up, 32 for a season (16 qualifying, then 16 race). */
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
