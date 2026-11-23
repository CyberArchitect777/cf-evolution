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

import cfevolution.data.executable.GameDrivers;
import cfevolution.data.executable.GamePoints;
import cfevolution.data.executable.GameTeams;

/**
 * The rules for editing the executable's drivers, teams and points, without
 * the interface (2026-11-16, moved out of ExecutableWindow's pages word for
 * word). Each setter takes what the user typed, changes the table only when
 * the value is allowed and different, and returns the message to show when
 * it is not allowed (null otherwise).
 */
public final class ExecutableEdits
{
    public static final int DRIVERS = 40, TEAMS = 20, POINTS = 26;
    /** The game races the first 18 teams only; 19 and 20 are spare slots. */
    public static final int RACED_TEAMS = 18;
    /** The field: fewer entered cars than this and a race cannot be filled. */
    public static final int MIN_ENTERED_CARS = 26;
    public static final int DRIVER_NAME_LENGTH = 23, TEAM_NAME_LENGTH = 12;
    public static final int MAX_SKILL = 255, MAX_POWER = 1300, MAX_POINTS = 127;

    public static final String NOT_CHANGED = "Value Not Changed", LINE_UP_NOT_CHANGED = "Line-up Not Changed";

    /** The cars in the first 18 teams, the ones the game races. */
    public static int enteredCars(GameTeams teams)
    {
        int entered = 0;
        for (int t = 0; t < RACED_TEAMS; t++)
            for (int s = 0; s < 2; s++)
                if (teams.getTeamCar(t, s) != 0)
                    entered++;
        return entered;
    }

    /**
     * Asked before closing GP.EXE when Save Names into the Executable is off and
     * the names shown are not the ones in GP.EXE (2026-11-22): they would be lost.
     * The options are the platform's own, the first keeping the names.
     */
    public static Message namesNotInExe(String... options)
    {
        return Message.choose(Message.Kind.WARNING, "Names Not Saved",
            "The driver, team or engine names shown here are not in GP.EXE, because Save Names into the "
          + "Executable is off. They will be lost when GP.EXE is closed.", options);
    }

    /** On the Drivers and Teams pages, one setting for both (Hrvoje, 2026-11-16/21). */
    public static final String SAVE_NAMES_INTO_EXE = "Save Names into the Executable";
    public static final String SAVE_NAMES_INTO_EXE_TIP = "On: saving writes the driver, team and engine names shown here into GP.EXE. "
        + "Off: GP.EXE keeps its own names, and names typed here or loaded from a name file are only shown in the editor "
        + "(save them as a name file to keep them).";

    private ExecutableEdits()
    {
    }

    /** The whole number typed, if it is from min to max; else null. */
    public static Integer number(String text, int min, int max)
    {
        try
        {
            int n = Integer.parseInt(String.valueOf(text).trim());
            if (n >= min && n <= max)
                return n;
        }
        catch (NumberFormatException e)
        {
            // not a number
        }
        return null;
    }

    public static Message numberProblem(String what, int min, int max)
    {
        return Message.warning(NOT_CHANGED, what + " must be a whole number from " + min + " to " + max + ".");
    }

    public static Message nameProblem(String what, int maxLength)
    {
        return Message.warning(NOT_CHANGED, what + " can be at most " + maxLength + " characters long.");
    }

    // --- Drivers ----------------------------------------------------------

    public static Message setDriverName(GameDrivers drivers, int row, String text)
    {
        if (text.length() > DRIVER_NAME_LENGTH)
            return nameProblem("A driver's name", DRIVER_NAME_LENGTH);
        if (!text.equals(drivers.getDriverName(row)))
            drivers.setDriverName(row, text);
        return null;
    }

    /** Skill as shown, 0-255 (stored as a byte). */
    public static int getSkill(GameDrivers drivers, int row, boolean race)
    {
        return (race ? drivers.getRaceGrip(row) : drivers.getQualifyingGrip(row)) & 0xFF;
    }

    /** The game uses skill - 1 as a signed rank (GetDriverRank, IDA 0x4C4E), so 0
        is one step better than 1. Other editors write 0, so it is accepted
        (owner, 2026-10-25). */
    public static Message setSkill(GameDrivers drivers, int row, boolean race, String text)
    {
        Integer n = number(text, 0, MAX_SKILL);
        if (n == null)
            return numberProblem("Skill", 0, MAX_SKILL);
        if (n.intValue() != getSkill(drivers, row, race))
        {
            if (race)
                drivers.setRaceGrip(row, n);
            else
                drivers.setQualifyingGrip(row, n);
        }
        return null;
    }

    // --- Teams ------------------------------------------------------------

    public static Message setTeamName(GameTeams teams, int row, String text)
    {
        if (text.length() > TEAM_NAME_LENGTH)
            return nameProblem("A team name", TEAM_NAME_LENGTH);
        if (!text.equals(teams.getTeamName(row)))
            teams.setTeamName(row, text);
        return null;
    }

    public static Message setEngineName(GameTeams teams, int row, String text)
    {
        if (text.length() > TEAM_NAME_LENGTH)
            return nameProblem("An engine name", TEAM_NAME_LENGTH);
        if (!text.equals(teams.getEngineName(row)))
            teams.setEngineName(row, text);
        return null;
    }

    public static Message setPower(GameTeams teams, int row, String text)
    {
        Integer n = number(text, 0, MAX_POWER);
        if (n == null)
            return numberProblem("Engine power", 0, MAX_POWER);
        if (n.intValue() != teams.getBHPSetting(row))
            teams.setBHPSetting(row, n);
        return null;
    }

    /** The car chosen for a seat: "None" or blank is 0, otherwise the number it
        starts with (as in "13  Nigel Mansell"), up to 40; -1 if it is not one. */
    public static int carNumber(String typed)
    {
        String text = String.valueOf(typed).trim();
        if (text.equals("None") || text.length() == 0)
            return 0;
        int end = 0;
        while (end < text.length() && Character.isDigit(text.charAt(end)))
            end++;
        if (end == 0)
            return -1;
        int n = Integer.parseInt(text.substring(0, end));
        return n <= DRIVERS ? n : -1;
    }

    /** Puts a car (1-40, or 0 for none) in a team's first (0) or second (1) seat,
        if no other team has it. Since 2026-11-22 (Hrvoje, the owner) the first 18 teams
        may fall below 26 cars while line-ups are rearranged: saving checks it
        ({@link ExecutableSaving#save}), not each edit. */
    public static Message setTeamCar(GameTeams teams, int team, int slot, int car)
    {
        if (car < 0 || car == teams.getTeamCar(team, slot))
            return null;
        int entered = 0;
        for (int t = 0; t < TEAMS; t++)
        {
            for (int s = 0; s < 2; s++)
            {
                int existing = (t == team && s == slot) ? car : teams.getTeamCar(t, s);
                if (car != 0 && existing == car && !(t == team && s == slot))
                    return Message.warning(LINE_UP_NOT_CHANGED, "Car " + car + " already drives for "
                        + teams.getTeamName(t) + ". Take it out of that team first.");
            }
        }
        teams.setTeamCar(team, slot, car);
        return null;
    }

    // --- Points -----------------------------------------------------------

    public static int getPoints(GamePoints points, int row)
    {
        return points.getPointValue(row) & 0xFF;
    }

    public static Message setPoints(GamePoints points, int row, String text)
    {
        Integer n = number(text, 0, MAX_POINTS);
        if (n == null)
            return numberProblem("Points", 0, MAX_POINTS);
        if (n.intValue() != getPoints(points, row))
            points.setPointValue(row, n);
        return null;
    }

    /** "1st", "2nd", "3rd", "4th" ... "11th", "21st". */
    public static String ordinal(int n)
    {
        int lastTwo = n % 100;
        String suffix = (lastTwo >= 11 && lastTwo <= 13) ? "th"
                      : n % 10 == 1 ? "st" : n % 10 == 2 ? "nd" : n % 10 == 3 ? "rd" : "th";
        return n + suffix;
    }
}
