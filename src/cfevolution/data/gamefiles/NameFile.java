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

/**
 * A name file (.NAM, 1,484 bytes): 40 driver names by car number (24 bytes
 * each), then 20 team and 20 engine names in menu order (13 bytes each), then
 * the checksums. The same 1,480 bytes the game keeps in GP.EXE and in a
 * championship save (ArgDocs, names).
 *
 * @author barrie
 */
public class NameFile extends GameFile
{
    public static final int SIZE = 1484;
    public static final int DRIVERS = 40, TEAMS = 20;
    public static final int DRIVER_LENGTH = 24, TEAM_LENGTH = 13;
    private static final int TEAMS_AT = 960, ENGINES_AT = 1220;

    public NameFile()
    {
        data = new byte[SIZE];
    }

    /** A name file holding these 1,480 bytes (as GP.EXE keeps its names). */
    public static NameFile fromGame(byte[] names)
    {
        NameFile f = new NameFile();
        System.arraycopy(names, 0, f.data, 0, SIZE - 4);
        return f;
    }

    public static NameFile load(File file) throws IOException
    {
        NameFile f = new NameFile();
        f.data = read(file, SIZE);
        return f;
    }

    public String getDriver(int i) { return getText(i * DRIVER_LENGTH, DRIVER_LENGTH); }

    public void setDriver(int i, String name) { setText(i * DRIVER_LENGTH, DRIVER_LENGTH, name); }

    public String getTeam(int i) { return getText(TEAMS_AT + i * TEAM_LENGTH, TEAM_LENGTH); }

    public void setTeam(int i, String name) { setText(TEAMS_AT + i * TEAM_LENGTH, TEAM_LENGTH, name); }

    public String getEngine(int i) { return getText(ENGINES_AT + i * TEAM_LENGTH, TEAM_LENGTH); }

    public void setEngine(int i, String name) { setText(ENGINES_AT + i * TEAM_LENGTH, TEAM_LENGTH, name); }
}
