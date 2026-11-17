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

package cfevolution.screens;

import cfevolution.data.executable.Executable;
import cfevolution.data.executable.GameDrivers;
import cfevolution.data.executable.GamePoints;
import cfevolution.data.executable.GameTables;
import cfevolution.data.executable.GameTeams;
import cfevolution.edit.ExecutableEdits;
import cfevolution.edit.ExecutableSaving;
import cfevolution.edit.Message;

/**
 * GP.EXE's screen (2026-11-17), with the pages of the first Android version:
 * drivers, teams, points and the pit lane. Every rule and message is the
 * desktop's own (ExecutableEdits, GameTables, ExecutableSaving). Each setter
 * takes what the user typed and returns the message to show when it is not
 * allowed, or null. Plain Java: runs on Android and the desktop JVM.
 */
public final class ExecutableScreen
{
    public final Executable exe;
    private boolean modified;

    public static final String[] PAGES = { "Drivers", "Teams", "Points", "Pit Lane" };

    /** The page descriptions the desktop shows (plain text). */
    public static final String[] PAGE_TEXT = {
        "Names and skill for all 40 car numbers. Lower skill numbers are faster drivers: 1 is the game's best, and 0, which some editors use, is faster still.",
        "Team and engine names, each team's engine power, and which cars drive for it. Power differences only show with \"1991 Levels\" opposition. The game races the first 18 teams. A new line-up takes effect when a new championship is started.",
        "Championship points for each finishing position.",
        "Which team uses each pit box, starting at the pit lane entrance. Choosing a team for a box swaps it with the team already there. Used at every circuit. A team with no cars in a session leaves its box empty; the other teams do not move up." };

    ExecutableScreen(Executable exe)
    {
        this.exe = exe;
    }

    public String title()
    {
        return "GP.EXE " + exe.returnGameVersionString();
    }

    public boolean isModified()
    {
        return modified;
    }

    private Message edited(Message problem)
    {
        if (problem == null)
            modified = true;
        return problem;
    }

    // --- Drivers ----------------------------------------------------------

    private GameDrivers drivers()
    {
        return exe.getGameDrivers();
    }

    public String driverName(int car)          { return drivers().getDriverName(car); }
    public int skill(int car, boolean race)    { return ExecutableEdits.getSkill(drivers(), car, race); }

    public Message setDriverName(int car, String text)
    {
        return edited(ExecutableEdits.setDriverName(drivers(), car, text));
    }

    public Message setSkill(int car, boolean race, String text)
    {
        return edited(ExecutableEdits.setSkill(drivers(), car, race, text));
    }

    // --- Teams ------------------------------------------------------------

    private GameTeams teams()
    {
        return exe.getGameTeams();
    }

    public String teamName(int team)           { return teams().getTeamName(team); }
    public String engineName(int team)         { return teams().getEngineName(team); }
    public int power(int team)                 { return teams().getBHPSetting(team); }
    public int teamCar(int team, int seat)     { return teams().getTeamCar(team, seat); }

    /** "None", or the car number and its driver, as the desktop lists cars. */
    public String carLabel(int car)
    {
        return car == 0 ? "None" : car + "  " + driverName(car - 1);
    }

    public Message setTeamName(int team, String text)   { return edited(ExecutableEdits.setTeamName(teams(), team, text)); }
    public Message setEngineName(int team, String text) { return edited(ExecutableEdits.setEngineName(teams(), team, text)); }
    public Message setPower(int team, String text)      { return edited(ExecutableEdits.setPower(teams(), team, text)); }
    public Message setTeamCar(int team, int seat, int car) { return edited(ExecutableEdits.setTeamCar(teams(), team, seat, car)); }

    // --- Points -----------------------------------------------------------

    public String position(int row)            { return ExecutableEdits.ordinal(row + 1); }
    public int points(int row)                 { return ExecutableEdits.getPoints(exe.getGamePoints(), row); }

    public Message setPoints(int row, String text)
    {
        GamePoints p = exe.getGamePoints();
        return edited(ExecutableEdits.setPoints(p, row, text));
    }

    // --- Pit lane ---------------------------------------------------------

    private GameTables tables()
    {
        return exe.getGameTables();
    }

    public static final int PIT_BOXES = GameTables.PIT_BOXES;

    public String pitBoxLabel(int box)
    {
        return "Pit box " + (box + 1) + (box == 0 ? "  (pit lane entrance)" : "");
    }

    /** The team (1-based) in box 0-17. */
    public int pitBoxTeam(int box)             { return tables().getPitBoxTeam(box); }

    public String teamLabel(int team)
    {
        return team + "  " + (team >= 1 && team <= 20 ? teamName(team - 1) : "?");
    }

    /** Puts a team in a box, swapping it with the team already there. */
    public void setPitBoxTeam(int box, int team)
    {
        tables().setPitBoxTeam(box, team);
        modified = true;
    }

    /** Moves a box's team one box nearer the entrance (up) or further away; returns its new box. */
    public int movePitBox(int box, boolean up)
    {
        int other = up ? box - 1 : box + 1;
        if (other < 0 || other >= PIT_BOXES)
            return box;
        tables().swapPitBoxes(box, other);
        modified = true;
        return other;
    }

    public void resetPitOrder()
    {
        tables().resetPitOrder();
        modified = true;
    }

    /** The warning the desktop shows above the boxes, or null when the order is valid. */
    public String pitWarning()
    {
        return tables().isPitOrderValid() ? null
            : "This order has a team missing or listed twice, so the game would not give every team its own box. "
            + "Choose a team for each box, or use Reset to 1991 Order.";
    }

    // --- Saving -----------------------------------------------------------

    /** Saves GP.EXE; returns the message when it was not saved, or null. */
    public Message save()
    {
        Message m = ExecutableSaving.save(exe);
        if (m == null)
            modified = false;
        return m;
    }
}
