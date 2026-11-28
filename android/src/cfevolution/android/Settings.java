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


package cfevolution.android;

import android.content.Context;
import android.content.SharedPreferences;
import cfevolution.edit.PixelShape;

/**
 * The app's own saved settings (2026-11-28), the counterpart of the desktop's Preferences node
 * "cfevolution", under the same names where a setting is the same: so far the game pictures'
 * shape (PixelShape.PREF_KEY). The game folders opened before are kept by Android itself, as
 * the folder picker's lasting permissions (MainActivity).
 */
final class Settings
{
    private static final String FILE = "cfevolution";

    private Settings()
    {
    }

    private static SharedPreferences store(Context c)
    {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    /** Puts the saved settings into effect (at start-up). */
    static void load(Context c)
    {
        PixelShape.setSquare(PixelShape.squareFromSaved(store(c).getString(PixelShape.PREF_KEY, "")));
    }

    static void setSquarePixels(Context c, boolean square)
    {
        store(c).edit().putString(PixelShape.PREF_KEY, PixelShape.saved(square)).apply();
        PixelShape.setSquare(square);
    }
}
