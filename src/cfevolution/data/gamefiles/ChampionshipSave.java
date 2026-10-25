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
 * A championship saved between races (3,578 bytes): a plain copy of the
 * game's season block, ss:121Ch-2011h, then the checksums (F1GP-SDL,
 * 2026-10-25). What is offered:
 *   - each driver's finishing place in each of the 16 rounds (a byte per
 *     round at 828 + 16 x (car - 1), 0 = none); points are not stored, the
 *     game counts them from the places;
 *   - the next round (word at 1Ah);
 *   - each round's weather (words at 3Ch: 0 dry, 96 wet);
 *   - the driver, team and engine names (the 1,480 bytes of a name file at 2094).
 * The save kind (word at 16h) is never changed. Saves made during a race
 * weekend are larger (26,736 bytes) and are not opened.
 *
 * @author barrie
 */
public class ChampionshipSave extends GameFile
{
    public static final int SIZE = 3578, ROUNDS = 16, DRIVERS = 40;
    public static final int WET = 96;
    private static final int NEXT_ROUND = 0x1A, WEATHER = 0x3C, PLACES = 828, NAMES = 2094;

    private ChampionshipSave(byte[] data)
    {
        this.data = data;
    }

    public static ChampionshipSave load(File file) throws IOException
    {
        return new ChampionshipSave(read(file, SIZE));
    }

    /** True if the file is a between-races save with the right checksums. */
    public static boolean isChampionshipSave(File file)
    {
        if (file.length() != SIZE)
            return false;
        try
        {
            read(file, SIZE);
            return true;
        }
        catch (IOException e)
        {
            return false;
        }
    }

    /** Finishing place of car 1-40 in round 1-16, 0 for none. */
    public int getPlace(int car, int round) { return getByte(PLACES + (car - 1) * ROUNDS + round - 1); }

    public void setPlace(int car, int round, int place) { setByte(PLACES + (car - 1) * ROUNDS + round - 1, place); }

    /** The next round, 0-15 (16 once the season is over). */
    public int getNextRound() { return getWord(NEXT_ROUND); }

    public void setNextRound(int round) { setWord(NEXT_ROUND, round); }

    public boolean isWet(int round) { return getWord(WEATHER + (round - 1) * 2) != 0; }

    public void setWet(int round, boolean wet) { setWord(WEATHER + (round - 1) * 2, wet ? WET : 0); }

    public String getDriver(int car) { return getText(NAMES + (car - 1) * 24, 24); }

    public void setDriver(int car, String name) { setText(NAMES + (car - 1) * 24, 24, name); }

    /**
     * A driver's points, counted as the game does: the points for each
     * place, adding only the best {@code bestCounted} results (0 = all).
     */
    public int getPoints(int car, int[] pointsForPlace, int bestCounted)
    {
        int[] scores = new int[ROUNDS];
        for (int round = 1; round <= ROUNDS; round++)
        {
            int place = getPlace(car, round);
            scores[round - 1] = place >= 1 && place <= pointsForPlace.length ? pointsForPlace[place - 1] : 0;
        }
        java.util.Arrays.sort(scores);
        int total = 0;
        int counted = bestCounted <= 0 || bestCounted > ROUNDS ? ROUNDS : bestCounted;
        for (int i = 0; i < counted; i++)
            total += scores[ROUNDS - 1 - i];
        return total;
    }
}
