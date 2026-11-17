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

package cfevolution.edit;

import cfevolution.data.gamefiles.NameFile;

/**
 * A name file (.NAM), from the desktop's NameFileTab word for word: the
 * driver names by car number, and the team and engine names in the order the
 * game lists them.
 */
public final class NameFilePage extends GameFilePage
{
    public static final int DRIVERS = 0, TEAMS = 1, ENGINES = 2;
    public static final String[] TAB_TITLES = { "Drivers", "Teams", "Engines" };
    static final String[] WHAT = { "Driver", "Team", "Engine" };

    public final NameFile names;

    public NameFilePage(NameFile names)
    {
        super(names);
        this.names = names;
    }

    public String title() { return "Name File"; }

    public String explanation() { return "Driver names by car number, and team and engine names in the order the game lists them."; }

    public String status() { return "Name file  ·  40 drivers  ·  20 teams and engines"; }

    public static int rows(int kind)
    {
        return kind == DRIVERS ? NameFile.DRIVERS : NameFile.TEAMS;
    }

    /** The table's two column names: "Car" or "No.", then "Driver", "Team" or "Engine". */
    public static String[] columns(int kind)
    {
        return new String[] { kind == DRIVERS ? "Car" : "No.", WHAT[kind] };
    }

    /** The desktop's name for the table: "drivers", "teams" or "engines". */
    public static String tableName(int kind)
    {
        return WHAT[kind].toLowerCase() + "s";
    }

    public static int maxLength(int kind)
    {
        return (kind == DRIVERS ? NameFile.DRIVER_LENGTH : NameFile.TEAM_LENGTH) - 1;
    }

    public String get(int kind, int row)
    {
        return kind == DRIVERS ? names.getDriver(row) : kind == TEAMS ? names.getTeam(row) : names.getEngine(row);
    }

    /** A typed name: the refusal, or null (changed() says whether it changed anything). */
    public Message set(int kind, int row, Object typed)
    {
        changed = false;
        Message[] problem = new Message[1];
        String t = text(typed, maxLength(kind), "A name", problem);
        if (t == null)
            return problem[0];
        if (t.equals(get(kind, row)))
            return null;
        if (kind == DRIVERS) names.setDriver(row, t); else if (kind == TEAMS) names.setTeam(row, t); else names.setEngine(row, t);
        changed = true;
        return null;
    }
}
