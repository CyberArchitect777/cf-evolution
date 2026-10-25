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

package cfevolution.data.executable;

import java.awt.Color;
import java.io.*;

/**
 * Colour schemes held in the game executable: each team's car, each
 * driver's helmet and each team's pit crew, plus the game's palette so the
 * editor can show the colours as the game draws them.
 *
 * Every colour is a palette index. Offsets and the meaning of each byte
 * follow ArgData (CarColors, HelmetColors, PitCrewColors); the palette is
 * arTCPalette (from the F1GP-SDL reference). All were checked in the three
 * unpacked builds. Each table is kept whole and written back whole, so the
 * bytes the editor does not offer are never changed. (ArgData's own writer
 * rewrites bytes 0, 9 and 10 of every car with fixed values, losing each
 * team's own byte 9.)
 *
 * @author barrie
 */
public class GameLiveries
{
    public static final int TEAMS = 20;
    public static final int DRIVERS = 40;

    /** Car parts, and which of the 16 bytes of a team's car colours each uses. */
    public static final String[] CAR_PARTS = {
        "Wings", "Wing endplates", "Rear wing sides", "Nose top", "Nose angle", "Nose sides",
        "Cockpit front", "Cockpit sides", "Sidepods", "Sidepod tops",
        "Engine cover", "Engine cover sides", "Engine cover rear" };
    public static final int[] CAR_BYTES = { 7, 3, 4, 8, 11, 5, 2, 12, 6, 15, 1, 13, 14 };

    public static final String[] HELMET_PARTS = {
        "Visor", "Visor surround", "Stripe 1", "Stripe 2", "Stripe 3", "Stripe 4", "Stripe 5",
        "Stripe 6", "Stripe 7", "Stripe 8", "Stripe 9", "Stripe 10", "Stripe 11", "Stripe 12", "Stripe 13" };
    public static final int[] HELMET_BYTES = { 0, 6, 2, 3, 4, 5, 7, 8, 9, 10, 11, 12, 13, 14, 15 };

    public static final String[] PIT_CREW_PARTS = { "Shirt", "Shirt trim", "Trousers", "Trouser trim", "Socks" };
    public static final int[] PIT_CREW_BYTES = { 6, 5, 11, 4, 3 };

    private byte[] palette = new byte[768];
    private byte[] carColours = new byte[TEAMS * 16];
    private byte[] helmetColours = new byte[DRIVERS * 16];
    private byte[] pitCrewColours = new byte[TEAMS * 16];

    public GameLiveries()
    {
    }

    /** Just a palette (768 bytes, 6-bit), for drawing and choosing colours
        outside the executable tab, e.g. a track's horizon. */
    public static GameLiveries ofPalette(byte[] palette)
    {
        GameLiveries l = new GameLiveries();
        System.arraycopy(palette, 0, l.palette, 0, 768);
        return l;
    }

    /** The game's colour for a palette index (stored as 6-bit values). */
    public Color getPaletteColour(int index)
    {
        int r = palette[index * 3] & 0x3F;
        int g = palette[index * 3 + 1] & 0x3F;
        int b = palette[index * 3 + 2] & 0x3F;
        return new Color((r << 2) | (r >> 4), (g << 2) | (g >> 4), (b << 2) | (b >> 4));
    }

    public int getCarColour(int team, int part)
    {
        return carColours[team * 16 + CAR_BYTES[part]] & 0xFF;
    }

    public void setCarColour(int team, int part, int colour)
    {
        carColours[team * 16 + CAR_BYTES[part]] = (byte) colour;
    }

    /** Helmets are listed by car number: helmet 0 belongs to car 1. */
    public int getHelmetColour(int driver, int part)
    {
        return helmetColours[driver * 16 + HELMET_BYTES[part]] & 0xFF;
    }

    public void setHelmetColour(int driver, int part, int colour)
    {
        helmetColours[driver * 16 + HELMET_BYTES[part]] = (byte) colour;
    }

    public int getPitCrewColour(int team, int part)
    {
        return pitCrewColours[team * 16 + PIT_CREW_BYTES[part]] & 0xFF;
    }

    public void setPitCrewColour(int team, int part, int colour)
    {
        pitCrewColours[team * 16 + PIT_CREW_BYTES[part]] = (byte) colour;
    }

    /**
     * Any of the 16 colours of a set, including those not offered for editing:
     * the game's pictures use them all.
     */
    public int getCarSetColour(int team, int slot)
    {
        return carColours[team * 16 + slot] & 0xFF;
    }

    public int getHelmetSetColour(int driver, int slot)
    {
        return helmetColours[driver * 16 + slot] & 0xFF;
    }

    public int getPitCrewSetColour(int team, int slot)
    {
        return pitCrewColours[team * 16 + slot] & 0xFF;
    }

    public int saveData(RandomAccessFile binaryAccess, int gameVersion)
    {
        BinaryManager.setDataBytes(gameVersion, binaryAccess, 184116, 184052, 184068, carColours);
        BinaryManager.setDataBytes(gameVersion, binaryAccess, 184436, 184372, 184388, helmetColours);
        BinaryManager.setDataBytes(gameVersion, binaryAccess, 185076, 185012, 185028, pitCrewColours);
        return 0;
    }

    public int loadData(RandomAccessFile binaryAccess, int gameVersion)
    {
        palette = BinaryManager.getDataBytes(gameVersion, binaryAccess, 142419, 142355, 142371, 768);
        carColours = BinaryManager.getDataBytes(gameVersion, binaryAccess, 184116, 184052, 184068, TEAMS * 16);
        helmetColours = BinaryManager.getDataBytes(gameVersion, binaryAccess, 184436, 184372, 184388, DRIVERS * 16);
        pitCrewColours = BinaryManager.getDataBytes(gameVersion, binaryAccess, 185076, 185012, 185028, TEAMS * 16);
        return palette.length == 768 ? 0 : 1;
    }
}
