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

import cfevolution.data.executable.Executable;
import cfevolution.data.executable.GameDrivers;
import cfevolution.data.executable.GameTables;
import cfevolution.data.executable.GameTeams;
import java.util.ArrayList;
import java.util.List;

/**
 * The Drivers and Teams redesign (Hrvoje, 2026-11-15 and 11-21; built 2026-11-23).
 *
 * Moving a driver to another car number (Move Up, Move Down, Swap With) takes
 * with him everything GP.EXE keeps by car number: his name, his qualifying and
 * race skill, his helmet colours and his Quick Race setup. He stays in the team
 * seat he had: the line-ups follow the new number. His menu helmet picture
 * (HELMETS.DAT, which the game shows by car number) goes with him too, written
 * when GP.EXE is saved (Hrvoje, 2026-11-28: swapping a team's two drivers left
 * their menu helmets behind). Moving a team, or choosing drivers for seats,
 * keeps the car numbers, so the menu helmets already followed. Championship
 * saves are other files and are left alone.
 *
 * Moving a team to another team number takes its name, engine name, engine power,
 * both seats (and so its drivers and their skills), its car and pit crew colours,
 * and its pit box: the pit lane keeps each box where it is and changes the
 * number in it. A team moved between the 18 raced teams and the two spare slots
 * (19, 20) does not take a pit box with it, since the spare slots have none: the
 * team moving into the raced 18 takes over the box of the one moving out.
 *
 * Skill is shown on the Teams page, by seat: choosing another driver for a seat
 * gives him the seat's skill (the driver he replaces keeps his own). A driver
 * chosen for a seat while he is in another one is asked about: swap the two
 * drivers (each takes the other's seat and its skill), move him and leave his old
 * seat empty, or cancel.
 *
 * Every move is logged in the Executable, so Revert Page can undo them
 * (ExecutableRevert): a swap done twice is undone, and the driver and team moves
 * do not interfere with each other.
 */
public final class LineUpMoves
{
    /** The kinds of move in Executable.getMoves(): {kind, a, b}. */
    public static final int DRIVER = 0, TEAM = 1;

    public static final String SWAP = "Swap Drivers", MOVE = "Move Driver", CANCEL = "Cancel";

    private LineUpMoves()
    {
    }

    // --- Moving drivers and teams --------------------------------------------

    /** Swaps two car numbers' drivers (0-39), with all they carry; logged for Revert Page. */
    public static void swapDrivers(Executable exe, int a, int b)
    {
        if (a == b)
            return;
        apply(exe, DRIVER, a, b);
        exe.getMoves().add(new int[] { DRIVER, a, b });
    }

    /** Swaps two team numbers' teams (0-19), with all they carry; logged for Revert Page. */
    public static void swapTeams(Executable exe, int a, int b)
    {
        if (a == b)
            return;
        apply(exe, TEAM, a, b);
        exe.getMoves().add(new int[] { TEAM, a, b });
    }

    /** Moves a driver one car number up (towards 1) or down, swapping with the neighbour;
        returns where he is now (unchanged at either end). */
    public static int moveDriver(Executable exe, int car, boolean up)
    {
        int to = up ? car - 1 : car + 1;
        if (to < 0 || to >= ExecutableEdits.DRIVERS)
            return car;
        swapDrivers(exe, car, to);
        return to;
    }

    /** As moveDriver, for a team. */
    public static int moveTeam(Executable exe, int team, boolean up)
    {
        int to = up ? team - 1 : team + 1;
        if (to < 0 || to >= ExecutableEdits.TEAMS)
            return team;
        swapTeams(exe, team, to);
        return to;
    }

    /** One swap, unlogged. Each is its own inverse, which Revert Page relies on. */
    static void apply(Executable exe, int kind, int a, int b)
    {
        if (kind == DRIVER)
        {
            GameDrivers d = exe.getGameDrivers();
            String name = d.getDriverName(a);
            d.setDriverName(a, d.getDriverName(b));
            d.setDriverName(b, name);
            int q = d.getQualifyingGrip(a), r = d.getRaceGrip(a);
            d.setQualifyingGrip(a, d.getQualifyingGrip(b));
            d.setRaceGrip(a, d.getRaceGrip(b));
            d.setQualifyingGrip(b, q);
            d.setRaceGrip(b, r);
            exe.getGameLiveries().swapHelmets(a, b);
            exe.swapMenuHelmets(a, b);                 // HELMETS.DAT, written when GP.EXE is saved
            exe.getGameTables().carSetups.swapBlocks(a, b, GameTables.SETUP_SIZE);
            GameTeams t = exe.getGameTeams();          // the driver keeps his seat under his new number
            for (int team = 0; team < ExecutableEdits.TEAMS; team++)
                for (int s = 0; s < 2; s++)
                {
                    int car = t.getTeamCar(team, s);
                    if (car == a + 1)
                        t.setTeamCar(team, s, b + 1);
                    else if (car == b + 1)
                        t.setTeamCar(team, s, a + 1);
                }
        }
        else
        {
            GameTeams t = exe.getGameTeams();
            String name = t.getTeamName(a), engine = t.getEngineName(a);
            int power = t.getBHPSetting(a), first = t.getTeamCar(a, 0), second = t.getTeamCar(a, 1);
            t.setTeamName(a, t.getTeamName(b));
            t.setEngineName(a, t.getEngineName(b));
            t.setBHPSetting(a, t.getBHPSetting(b));
            t.setTeamCar(a, 0, t.getTeamCar(b, 0));
            t.setTeamCar(a, 1, t.getTeamCar(b, 1));
            t.setTeamName(b, name);
            t.setEngineName(b, engine);
            t.setBHPSetting(b, power);
            t.setTeamCar(b, 0, first);
            t.setTeamCar(b, 1, second);
            exe.getGameLiveries().swapCars(a, b);
            exe.getGameLiveries().swapPitCrews(a, b);
            if (a < GameTables.PIT_BOXES && b < GameTables.PIT_BOXES)
            {
                GameTables tables = exe.getGameTables();
                for (int box = 0; box < GameTables.PIT_BOXES; box++)
                {
                    int team = tables.getPitBoxTeam(box);
                    if (team == a + 1)
                        tables.pitOrder.setByte(box, b + 1);
                    else if (team == b + 1)
                        tables.pitOrder.setByte(box, a + 1);
                }
            }
        }
    }

    /**
     * Undoes the logged moves (newest first), runs restore, then makes again the
     * moves of the kinds kept, and keeps only those in the log. Revert Page puts a
     * page back by position after undoing the moves, so the values return to the
     * drivers and teams they belonged to wherever the moves kept put them.
     */
    static void withMovesUndone(Executable exe, boolean keepDriverMoves, boolean keepTeamMoves, Runnable restore)
    {
        List<int[]> log = new ArrayList<int[]>(exe.getMoves());
        for (int i = log.size() - 1; i >= 0; i--)
            apply(exe, log.get(i)[0], log.get(i)[1], log.get(i)[2]);
        restore.run();
        exe.getMoves().clear();
        for (int[] m : log)
            if (m[0] == DRIVER ? keepDriverMoves : keepTeamMoves)
            {
                apply(exe, m[0], m[1], m[2]);
                exe.getMoves().add(m);
            }
    }

    /** The question for Swap With: the drivers or teams to choose from, by number. */
    public static String[] driverChoices(Executable exe)
    {
        String[] c = new String[ExecutableEdits.DRIVERS];
        for (int i = 0; i < c.length; i++)
            c[i] = (i + 1) + "  " + exe.getGameDrivers().getDriverName(i);
        return c;
    }

    public static String[] teamChoices(Executable exe)
    {
        String[] c = new String[ExecutableEdits.TEAMS];
        for (int i = 0; i < c.length; i++)
            c[i] = (i + 1) + "  " + exe.getGameTeams().getTeamName(i);
        return c;
    }

    public static final String DRIVER_MOVES_TIP = "Moving a driver to another number takes his name, skill, helmet colours, "
        + "Quick Race setup and menu helmet picture with him, and he stays in his team.";
    public static final String TEAM_MOVES_TIP = "Moving a team to another number takes its names, engine power, drivers, "
        + "car and pit crew colours and pit box with it.";

    // --- Seats, and skill by seat -----------------------------------------------

    /** The seat (team * 2 + seat) a car drives in, or -1. */
    public static int seatOf(GameTeams teams, int car)
    {
        if (car <= 0)
            return -1;
        for (int t = 0; t < ExecutableEdits.TEAMS; t++)
            for (int s = 0; s < 2; s++)
                if (teams.getTeamCar(t, s) == car)
                    return t * 2 + s;
        return -1;
    }

    /** The skill of the driver in a seat (0-255), or -1 when the seat is empty. */
    public static int seatSkill(Executable exe, int team, int seat, boolean race)
    {
        int car = exe.getGameTeams().getTeamCar(team, seat);
        return car == 0 ? -1 : ExecutableEdits.getSkill(exe.getGameDrivers(), car - 1, race);
    }

    /** Sets the skill of the driver in a seat; an empty seat has none to set. */
    public static Message setSeatSkill(Executable exe, int team, int seat, boolean race, String text)
    {
        int car = exe.getGameTeams().getTeamCar(team, seat);
        if (car == 0)
            return Message.warning(ExecutableEdits.NOT_CHANGED, "This seat is empty: choose a driver for it first.");
        return ExecutableEdits.setSkill(exe.getGameDrivers(), car - 1, race, text);
    }

    /**
     * Asked when the car chosen for a seat already drives in another seat; null when
     * it does not (or nothing changes). Its options are SWAP (only when this seat has
     * a driver to swap), MOVE and CANCEL; give the one chosen to placeDriver.
     */
    public static Message duplicateQuestion(Executable exe, int team, int seat, int car)
    {
        GameTeams teams = exe.getGameTeams();
        int from = seatOf(teams, car);
        if (car <= 0 || from < 0 || from == team * 2 + seat)
            return null;
        int fromTeam = from / 2, here = teams.getTeamCar(team, seat);
        String driver = car + "  " + exe.getGameDrivers().getDriverName(car - 1).trim();
        String fromName = teams.getTeamName(fromTeam).trim();
        String seatThere = fromTeam == team ? "the other seat of " + fromName : fromName;
        String text = "Car " + driver + " already drives for " + fromName + ".\n";
        if (here != 0)
            return Message.choose(Message.Kind.QUESTION, "Driver Already in a Team", text
                + SWAP + ": car " + here + "  " + exe.getGameDrivers().getDriverName(here - 1).trim() + " takes his place in " + seatThere + ".\n"
                + MOVE + ": his seat in " + seatThere + " is left empty.\n"
                + "Each driver takes the skill of the seat he goes to.", SWAP, MOVE, CANCEL);
        return Message.choose(Message.Kind.QUESTION, "Driver Already in a Team", text
            + MOVE + ": his seat in " + seatThere + " is left empty.", MOVE, CANCEL);
    }

    /**
     * Puts a car (1-40, or 0 for none) in a seat. The seat keeps its skill: the
     * driver put in it is given the skill of the driver he replaces. When the car
     * drives in another seat, how says what to do (SWAP, MOVE or CANCEL, from
     * duplicateQuestion; anything else is CANCEL). Returns true if anything changed.
     */
    public static boolean placeDriver(Executable exe, int team, int seat, int car, String how)
    {
        GameTeams teams = exe.getGameTeams();
        GameDrivers drivers = exe.getGameDrivers();
        int here = teams.getTeamCar(team, seat);
        if (car < 0 || car > ExecutableEdits.DRIVERS || car == here)
            return false;
        int from = seatOf(teams, car);
        boolean swap = false;
        if (car != 0 && from >= 0)
        {
            if (SWAP.equals(how) && here != 0)
                swap = true;
            else if (!MOVE.equals(how) && !(SWAP.equals(how) && here == 0))
                return false;
        }
        if (car != 0 && here != 0)
        {
            int q = drivers.getQualifyingGrip(here - 1), r = drivers.getRaceGrip(here - 1);
            if (swap)
            {
                drivers.setQualifyingGrip(here - 1, drivers.getQualifyingGrip(car - 1));
                drivers.setRaceGrip(here - 1, drivers.getRaceGrip(car - 1));
            }
            drivers.setQualifyingGrip(car - 1, q);
            drivers.setRaceGrip(car - 1, r);
        }
        if (from >= 0 && car != 0)
            teams.setTeamCar(from / 2, from % 2, swap ? here : 0);
        teams.setTeamCar(team, seat, car);
        return true;
    }
}
