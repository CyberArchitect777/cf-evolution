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

import cfevolution.data.GameFolder;
import cfevolution.data.executable.GameTables;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GP.EXE's Lap Records page (moved out of the desktop window 2026-11-17, word
 * for word): for each circuit, the qualifying and race record's driver, team,
 * time and date as the page shows them, and the rules for changing them.
 */
public final class LapRecordsPage
{
    public static final String TITLE = "Lap Records";
    public static final String DESCRIPTION = "The lap records the game starts with, for qualifying and the race at each circuit.";
    public static final String[] COLUMNS = { "Circuit", "Qualifying: driver", "Team", "Time", "Date", "Race: driver", "Team", "Time", "Date" };
    public static final int ROWS = 16;

    public static final Message BAD_TIME = Message.warning(ExecutableEdits.NOT_CHANGED,
        "A lap time must look like 1:21.434 (minutes, seconds, thousandths).");
    public static final Message BAD_DATE = Message.warning(ExecutableEdits.NOT_CHANGED,
        "A date must look like 10 Mar 1991 or 10/3/1991, from 1978 onwards. The game has no 29 February 2000.");

    private static final DateTimeFormatter DATES = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK);

    private final GameTables tables;

    public LapRecordsPage(GameTables tables)
    {
        this.tables = tables;
    }

    /** A lap time as the page shows it: 1:21.434. */
    public static String lapTime(int ms)
    {
        return String.format("%d:%02d.%03d", ms / 60000, (ms / 1000) % 60, ms % 1000);
    }

    /** What the page shows in row r (a circuit), column c (COLUMNS). */
    public String value(int r, int c)
    {
        if (c == 0)
            return GameFolder.CIRCUIT_NAMES[r];
        boolean race = c >= 5;
        switch ((c - 1) % 4)
        {
            case 0: return tables.getRecordDriver(r, race);
            case 1: return tables.getRecordTeam(r, race);
            case 2: return lapTime(tables.getRecordTime(r, race));
            default: return GameTables.dateOfDay(tables.getRecordDay(r, race)).format(DATES);
        }
    }

    /**
     * Typed text for row r, column c (1-8): stored if allowed. Returns why it
     * was refused, or null; changed[0] (if given) says whether anything changed.
     */
    public Message set(int r, int c, String typed, boolean[] changed)
    {
        if (changed != null)
            changed[0] = false;
        boolean race = c >= 5;
        String text = String.valueOf(typed).trim();
        switch ((c - 1) % 4)
        {
            case 0:
                if (text.length() > GameTables.NAME_LENGTH - 1)
                    return ExecutableEdits.nameProblem("A driver's name", GameTables.NAME_LENGTH - 1);
                if (text.equals(tables.getRecordDriver(r, race)))
                    return null;
                tables.setRecordDriver(r, race, text);
                break;
            case 1:
                if (text.length() > GameTables.TEAM_LENGTH - 1)
                    return ExecutableEdits.nameProblem("A team name", GameTables.TEAM_LENGTH - 1);
                if (text.equals(tables.getRecordTeam(r, race)))
                    return null;
                tables.setRecordTeam(r, race, text);
                break;
            case 2:
            {
                Matcher m = Pattern.compile("(\\d+):(\\d{1,2})\\.(\\d{1,3})").matcher(text);
                int ms = -1;
                if (m.matches() && Integer.parseInt(m.group(2)) < 60)
                {
                    String frac = (m.group(3) + "00").substring(0, 3);
                    ms = Integer.parseInt(m.group(1)) * 60000 + Integer.parseInt(m.group(2)) * 1000 + Integer.parseInt(frac);
                }
                if (ms <= 0 || ms >= 60 * 60000)
                    return BAD_TIME;
                if (ms == tables.getRecordTime(r, race))
                    return null;
                tables.setRecordTime(r, race, ms);
                break;
            }
            default:
            {
                LocalDate d = null;
                for (String pattern : new String[] { "d MMM yyyy", "d/M/yyyy", "d-M-yyyy", "yyyy-M-d" })
                    try
                    {
                        d = LocalDate.parse(text, DateTimeFormatter.ofPattern(pattern, Locale.UK));
                        break;
                    }
                    catch (DateTimeParseException e)
                    {
                        // next pattern
                    }
                int day = d == null ? -1 : GameTables.dayOfDate(d);
                if (day < 0)
                    return BAD_DATE;
                if (day == tables.getRecordDay(r, race))
                    return null;
                tables.setRecordDay(r, race, day);
            }
        }
        if (changed != null)
            changed[0] = true;
        return null;
    }
}
