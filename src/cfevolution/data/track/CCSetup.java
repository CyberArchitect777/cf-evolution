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

/*
 * CCSetup.java
 *
 * Created on 17. Februar 2005, 23:47
 */

package cfevolution.data.track;

import java.io.*;
import cfevolution.data.*;

/**
 * The 38-byte computer car set-up and behaviour block of a track file
 * ("CC setup", the game's strTrkCCSetup and wTrk* values at seg003:0204h).
 *
 * Kept as the raw bytes read, so it always saves back exactly as loaded;
 * the accessors below read and write single fields. Layout (F1GP-SDL's
 * reading of TCLoadCCSetup, IDA 0x8FCCC; the field names are the game's):
 *   bytes 0-7  front wing, rear wing, gears 1-6, each stored + 151
 *   byte 8     tyre compound, stored + 52 (0 = A .. 3 = D; C on every original)
 *   byte 9     brake balance
 *   then 14 words, most a factor where 16384 (4000h) = 100%:
 *   grip, CC aggression non-race, CC aggression race, time factor non-race
 *   (the race clock's scale, which sets how long lap times look), power,
 *   air resistance, tyre wear qualifying, tyre wear other sessions (an
 *   UNSIGNED factor: Germany's 33896 is 207%), fuel load, time factor race,
 *   CC power (only below Ace: (value - 4000h) x (4 - opposition level) is
 *   added to the computer cars' power), CC wet race aggression, formation
 *   gap, and a heading-step threshold the computer cars brake by.
 *
 * Originally Klaus's field-by-field class; rewritten 2026-10-24.
 *
 * @author Klaus
 */
public class CCSetup extends CFDataObject {

    public static final int SIZE = 38;

    /** Bytes 0-9. */
    public static final int FRONT_WING = 0, REAR_WING = 1, GEAR_1 = 2, TYRE = 8, BRAKE_BALANCE = 9;
    /** Stored offsets of the set-up bytes. */
    public static final int SETUP_OFFSET = 151, TYRE_OFFSET = 52;

    /** Words 0-13, after the ten set-up bytes. */
    public static final int GRIP = 0, AGGRESSION_PRACTICE = 1, AGGRESSION_RACE = 2, CLOCK_PRACTICE = 3,
        POWER = 4, AIR_RESISTANCE = 5, WEAR_QUALIFYING = 6, WEAR_OTHER = 7, FUEL = 8, CLOCK_RACE = 9,
        CC_POWER = 10, AGGRESSION_WET = 11, FORMATION_GAP = 12, BRAKE_THRESHOLD = 13;

    /** The factor value meaning 100%. */
    public static final int FULL = 0x4000;

    private final byte[] m_baData = new byte[SIZE];

    public CCSetup() {
    }

    public void load( FileInputStream fis)
    {
        try {
            for (int i = 0; i < SIZE; i++)
            {
                int b = fis.read();
                m_baData[i] = (byte) (b < 0 ? 0 : b);
            }
        }
        catch( IOException ioe )
        {
        }
    }

    public int save(FileOutputStream fos) throws IOException
    {
        fos.write(m_baData);
        return SIZE;
    }

    /** A raw set-up byte, 0-9. */
    public int getByte(int index)
    {
        return m_baData[index] & 0xFF;
    }

    public void setByte(int index, int value)
    {
        m_baData[index] = (byte) value;
    }

    /** A raw word, 0-13 (unsigned). */
    public int getWord(int index)
    {
        int o = 10 + index * 2;
        return (m_baData[o] & 0xFF) | ((m_baData[o + 1] & 0xFF) << 8);
    }

    public void setWord(int index, int value)
    {
        int o = 10 + index * 2;
        m_baData[o] = (byte) value;
        m_baData[o + 1] = (byte) (value >> 8);
    }

    /** A factor word as a whole percentage of 100% (16384). */
    public int getPercent(int index)
    {
        return Math.round(getWord(index) * 100f / FULL);
    }

    /** Sets a factor word from a percentage, only if the percentage shown
        changes, so values that are not whole percentages survive. */
    public void setPercent(int index, int percent)
    {
        if (percent != getPercent(index))
            setWord(index, Math.min(0xFFFF, percent * FULL / 100));
    }
}
