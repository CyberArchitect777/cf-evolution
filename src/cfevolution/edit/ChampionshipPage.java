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
import cfevolution.data.gamefiles.ChampionshipSave;

/**
 * A championship saved between races, from the desktop's ChampionshipSaveTab
 * word for word: each driver's finishing places, the next round and each
 * round's weather. Points follow from the places, by the points and "best
 * results" rule of the game folder's GP.EXE.
 */
public final class ChampionshipPage extends GameFilePage
{
    public static final String NEXT_ROUND = "Next round", WET_ROUNDS = "Wet rounds", SEASON_FINISHED = "Season finished";
    /** The places table's first round column (after Car and Driver). */
    public static final int FIRST_ROUND = 2;

    public final ChampionshipSave save;
    private final int[] points;
    private final int bestCounted;

    /** @param points points for 1st, 2nd... from GP.EXE, or null to show none */
    public ChampionshipPage(ChampionshipSave save, int[] points, int bestCounted)
    {
        super(save);
        this.save = save;
        this.points = points;
        this.bestCounted = bestCounted;
    }

    public String title() { return "Championship Save"; }

    public String explanation() { return "Finishing places in each round (0 for none). The points follow from them, as the game counts them."; }

    public String status()
    {
        int next = save.getNextRound();
        return "Championship save  ·  " + (next >= ChampionshipSave.ROUNDS ? "season finished"
                                                : "next round " + (next + 1) + ", " + GameFolder.CIRCUIT_NAMES[next]);
    }

    /** The next round's choices: "1  United States" ... and "Season finished". */
    public static String[] rounds()
    {
        String[] r = new String[ChampionshipSave.ROUNDS + 1];
        for (int i = 0; i < ChampionshipSave.ROUNDS; i++)
            r[i] = (i + 1) + "  " + GameFolder.CIRCUIT_NAMES[i];
        r[ChampionshipSave.ROUNDS] = SEASON_FINISHED;
        return r;
    }

    public int nextRound()
    {
        return Math.min(save.getNextRound(), ChampionshipSave.ROUNDS);
    }

    public void setNextRound(int index)
    {
        changed = index != save.getNextRound();
        if (changed)
            save.setNextRound(index);
    }

    public boolean wet(int round)
    {
        return save.isWet(round);
    }

    public void setWet(int round, boolean on)
    {
        changed = on != save.isWet(round);
        if (changed)
            save.setWet(round, on);
    }

    public boolean showsPoints()
    {
        return points != null;
    }

    /** Car, Driver, the 16 rounds, and Points when GP.EXE could be read. */
    public String[] columns()
    {
        int n = FIRST_ROUND + ChampionshipSave.ROUNDS + (points != null ? 1 : 0);
        String[] c = new String[n];
        c[0] = "Car";
        c[1] = "Driver";
        for (int i = 0; i < ChampionshipSave.ROUNDS; i++)
            c[FIRST_ROUND + i] = String.valueOf(i + 1);
        if (points != null)
            c[n - 1] = "Points";
        return c;
    }

    public static int rows()
    {
        return ChampionshipSave.DRIVERS;
    }

    /** A cell of the places table: the car number, the driver, a place, or the points. */
    public Object cell(int r, int c)
    {
        if (c == 0) return r + 1;
        if (c == 1) return save.getDriver(r + 1);
        if (c == FIRST_ROUND + ChampionshipSave.ROUNDS) return save.getPoints(r + 1, points, bestCounted);
        return save.getPlace(r + 1, c - FIRST_ROUND + 1);
    }

    /** A typed place for car r + 1 in round: the refusal, or null. */
    public Message setPlace(int r, int round, Object typed)
    {
        changed = false;
        Message[] problem = new Message[1];
        Integer n = number(typed, 0, 26, "A finishing place", problem);
        if (n == null)
            return problem[0];
        if (n == save.getPlace(r + 1, round))
            return null;
        save.setPlace(r + 1, round, n);
        changed = true;
        return null;
    }
}
