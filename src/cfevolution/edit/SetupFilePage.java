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
import cfevolution.data.gamefiles.SetupFile;

/**
 * A setup file from the game's setup screen, from the desktop's SetupFileTab
 * word for word: one setup, or a whole season's (qualifying and race for each
 * circuit), with the game's own limits.
 */
public final class SetupFilePage extends GameFilePage
{
    public static final String[] COLUMNS = { "Front wing", "Rear wing", "Gear 1", "Gear 2", "Gear 3", "Gear 4",
                                             "Gear 5", "Gear 6", "Tyres", "Brake balance" };
    public static final String SEPARATE = "Separate race and qualifying setups";
    public static final String SEPARATE_TIP = "When off, the game uses the qualifying setup in the race too.";

    public final SetupFile setups;

    public SetupFilePage(SetupFile setups)
    {
        super(setups);
        this.setups = setups;
    }

    public String title() { return setups.isSeason() ? "Season Setups" : "Setup"; }

    public String explanation()
    {
        return setups.isSeason() ? "A qualifying and a race setup for each circuit, as the game's setup screen saves them."
                                 : "One car setup, as the game's setup screen saves it.";
    }

    public String status()
    {
        return setups.isSeason() ? "Season setup file  ·  16 circuits, qualifying and race" : "Setup file  ·  one setup";
    }

    public int rows()
    {
        return setups.getSetupCount();
    }

    public String rowName(int r)
    {
        if (!setups.isSeason())
            return "Setup";
        return (r % 16 + 1) + "  " + GameFolder.CIRCUIT_NAMES[r % 16] + (r < 16 ? ", qualifying" : ", race");
    }

    /** A field (0 front wing ... 8 tyres, 9 brake balance) as the table shows it: a number, or the compound A-D. */
    public Object cell(int r, int field)
    {
        int v = setups.get(r, field);
        if (field == SetupFile.TYRES)
            return v < SetupFile.COMPOUNDS.length ? SetupFile.COMPOUNDS[v] : String.valueOf(v);
        return v;
    }

    /** A typed value, within the game's own limits (Hrvoje, 2026-11-05): the refusal, or null. */
    public Message set(int r, int field, Object typed)
    {
        changed = false;
        int n;
        if (field == SetupFile.TYRES)
            n = java.util.Arrays.asList(SetupFile.COMPOUNDS).indexOf(String.valueOf(typed).trim().toUpperCase());
        else
        {
            try { n = Integer.parseInt(String.valueOf(typed).trim()); }
            catch (NumberFormatException e) { n = Integer.MIN_VALUE; }
        }
        int[] setup = new int[SetupFile.SETUP_SIZE];
        for (int i = 0; i < setup.length; i++)
            setup[i] = setups.get(r, i);
        String problem = SetupFile.problem(setup, field, n);
        if (problem != null)
            return Message.warning(NOT_CHANGED, problem);
        if (n == setups.get(r, field))
            return null;
        setups.set(r, field, n);
        changed = true;
        return null;
    }

    public boolean isSeason()
    {
        return setups.isSeason();
    }

    public boolean separate()
    {
        return setups.isSeparateRaceSetups();
    }

    public void setSeparate(boolean on)
    {
        changed = on != setups.isSeparateRaceSetups();
        if (changed)
            setups.setSeparateRaceSetups(on);
    }
}
