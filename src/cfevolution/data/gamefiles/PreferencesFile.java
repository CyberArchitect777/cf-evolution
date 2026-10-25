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
 * F1PREFS.DAT (1,166 bytes), written by the game's Save Options: a copy of
 * the game's settings block at ss:0DA8h-1231h, then the checksums. Offsets
 * here are file offsets; the names are those of our IDA database (e.g.
 * wOppositionStandard at ss:1222h = file 1146) and the F1GP-SDL project's
 * reading (2026-10-25): the game checks only the size, the checksums and the
 * version word (2Ch for 1.05, at 1140), and range-checks nothing else.
 *
 * @author barrie
 */
public class PreferencesFile extends GameFile
{
    public static final int SIZE = 1166;

    public static final int QUICK_RACE_GRID = 1024, QUICK_RACE_CIRCUIT = 1025, QUICK_RACE_DRIVER = 1026,
                            QUICK_RACE_WET = 1027, QUICK_RACE_DISTANCE = 1028;
    public static final int NAME_FILE_PATH = 1034, SETUP_FILE_PATH = 1066, RECORDS_FILE_PATH = 1098, PATH_LENGTH = 32;
    public static final int AUTO_LOAD_NAMES = 1130, AUTO_LOAD_SETUP = 1131, AUTO_LOAD_RECORDS = 1132;
    public static final int OPPOSITION_STANDARD = 1146, OPPOSITION_SPREAD = 1148, RACE_DISTANCE = 1150,
                            QUALIFYING_MINUTES = 1152, PRACTICE_MINUTES = 1154;

    private PreferencesFile(byte[] data)
    {
        this.data = data;
    }

    public static PreferencesFile load(File file) throws IOException
    {
        return new PreferencesFile(read(file, SIZE));
    }
}
