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

/**
 * How the game's pictures are shown (owner, 2026-11-28, an option on the desktop and on Android):
 * in their original 4:3 shape, as the game's 320 x 200 screen filled a 4:3 monitor, each pixel
 * 1.2 times as tall as it is wide (the default, and how the editor showed the Cars tab's car and
 * the horizon before the option existed); or with square pixels, as DOSBox shows the game unless
 * its aspect correction is on.
 *
 * <p>The game's own pictures show the 4:3 shape is the intended one: the side-on tyre pictures are
 * stored 38 x 32 and are round only that way, and the 3D drawing scales 256 across to 221 up.
 *
 * <p>The setting is held here for every view to read; each interface saves it under
 * {@link #PREF_KEY} (the desktop in its Preferences, Android in its own settings) and sets it at
 * start-up. The arithmetic below is shared so both draw, and map clicks back, alike: a game
 * pixel shown {@code across} screen pixels wide is {@code across} tall when square and
 * {@code across * 6 / 5} tall in 4:3, rows starting at whole screen pixels.
 */
public final class PixelShape
{
    /** The saved setting's name, and its two values. */
    public static final String PREF_KEY = "PixelShape";
    public static final String FOUR_BY_THREE = "4:3", SQUARE = "square";

    /** The choices as the Options show them, 4:3 first. */
    public static final String[] CHOICES = {
        "4:3, the original shape",
        "Square pixels"
    };
    public static final String LABEL = "Game pictures";
    public static final String TIP = "How the game's pictures (cars, helmets, pit crews, picture files and horizons) are shown. "
        + "The game was made for 4:3 screens, where each pixel was a little taller than wide; "
        + "square pixels look like the game in DOSBox when its aspect correction is off (DOSBox's default).";

    private static volatile boolean square;

    private PixelShape()
    {
    }

    public static boolean square()
    {
        return square;
    }

    public static void setSquare(boolean isSquare)
    {
        square = isSquare;
    }

    /** The setting from its saved value: anything but "square" (including nothing saved yet) is 4:3. */
    public static boolean squareFromSaved(String saved)
    {
        return SQUARE.equals(saved);
    }

    public static String saved(boolean isSquare)
    {
        return isSquare ? SQUARE : FOUR_BY_THREE;
    }

    /** The screen row where game row {@code row} starts, each game pixel {@code across} wide. */
    public static int top(int row, int across)
    {
        return top(row, across, square);
    }

    public static int top(int row, int across, boolean isSquare)
    {
        return isSquare ? row * across : Math.floorDiv(row * across * 6, 5);
    }

    /** How tall {@code rows} game rows are on the screen. */
    public static int height(int rows, int across)
    {
        return top(rows, across);
    }

    /** The game row a screen row falls in (negative above the picture). */
    public static int rowAt(int y, int across)
    {
        return rowAt(y, across, square);
    }

    public static int rowAt(int y, int across, boolean isSquare)
    {
        if (across <= 0)
            return 0;
        if (isSquare)
            return Math.floorDiv(y, across);
        // the last row r with r * across * 6 / 5 <= y
        return Math.floorDiv(5 * (y + 1) - 1, 6 * across);
    }

    /** How much taller than wide a game pixel is shown: 1 or 1.2. */
    public static double tall()
    {
        return square ? 1.0 : 1.2;
    }
}
