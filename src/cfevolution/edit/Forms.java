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

/** Small helpers for the forms in this package. */
final class Forms
{
    private Forms()
    {
    }

    /** A typed whole number (a trailing % allowed), or Integer.MIN_VALUE. */
    static int number(String typed)
    {
        try
        {
            return Integer.parseInt(String.valueOf(typed).trim().replace("%", ""));   // as the desktop table always parsed
        }
        catch (NumberFormatException e)
        {
            return Integer.MIN_VALUE;
        }
    }
}
