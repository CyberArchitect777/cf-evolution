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

package cfevolution.data.track;

/**
 * The race and pit stop settings in a track file. They sit in the 28-byte
 * block just before the checksum, at the end of the raw Footer: laps for a
 * full race, the lap of the first planned pit stop, and the chance that a
 * computer car plans a stop (0..4000h, 4000h = every car).
 *
 * What the game does with them (F1GP-SDL's reading, checked against our own
 * disassembly for the chance): race laps = full laps x race distance, at
 * least 3, and they also set the starting fuel; the first stop lap is scaled
 * by the race distance the same way; and at the start of a DRY race of at
 * least 75% distance (a number in the executable) each car plans a stop when
 * a random byte x 64 is no more than the chance. So a chance of 0 still lets
 * about 1 car in 256 plan a stop.
 *
 * Values are only rewritten when they change, and the chance is kept exactly
 * as stored unless the percentage shown changes: several originals do not
 * store whole percentages (Spa's is 87.5%).
 *
 * @author barrie
 */
public class RaceStrategy
{
    /** Offsets counted back from the end of the footer (which ends with the 4-byte checksum). */
    private static final int LAPS_FROM_END = 10;
    private static final int FIRST_STOP_FROM_END = 8;
    private static final int CHANCE_FROM_END = 6;
    private static final int FULL_CHANCE = 0x4000;

    private RaceStrategy()
    {
    }

    /** False for a footer too short to hold the block (a damaged file). */
    public static boolean isAvailable(Footer footer)
    {
        return footer.getDataSize() >= 32;
    }

    private static int word(Footer footer, int fromEnd)
    {
        byte[] d = footer.getData();
        int o = footer.getDataSize() - fromEnd;
        return (d[o] & 0xFF) | ((d[o + 1] & 0xFF) << 8);
    }

    private static void setWord(Footer footer, int fromEnd, int value)
    {
        byte[] d = footer.getData();
        int o = footer.getDataSize() - fromEnd;
        d[o] = (byte) value;
        d[o + 1] = (byte) (value >> 8);
    }

    public static int getRaceLaps(Footer footer)
    {
        return word(footer, LAPS_FROM_END);
    }

    public static void setRaceLaps(Footer footer, int laps)
    {
        setWord(footer, LAPS_FROM_END, laps);
    }

    public static int getFirstStopLap(Footer footer)
    {
        return word(footer, FIRST_STOP_FROM_END);
    }

    public static void setFirstStopLap(Footer footer, int lap)
    {
        setWord(footer, FIRST_STOP_FROM_END, lap);
    }

    /** The chance a computer car plans a pit stop, as a whole percentage. */
    public static int getStopChancePercent(Footer footer)
    {
        return Math.round(Math.min(word(footer, CHANCE_FROM_END), FULL_CHANCE) * 100f / FULL_CHANCE);
    }

    public static void setStopChancePercent(Footer footer, int percent)
    {
        if (percent != getStopChancePercent(footer))
            setWord(footer, CHANCE_FROM_END, percent * FULL_CHANCE / 100);
    }
}
