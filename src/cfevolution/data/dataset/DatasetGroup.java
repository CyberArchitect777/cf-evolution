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
package cfevolution.data.dataset;

/**
 * The parts of a dataset that are exported and imported separately, each
 * with its own tick box.
 *
 * @author barrie
 */
public enum DatasetGroup
{
    TEAMS("Teams", "names, engines, engine power and which cars drive for each team", true, false),
    DRIVERS("Drivers", "names and qualifying and race skill", true, false),
    COLOURS("Colours", "car, helmet and pit crew colours", true, false),
    POINTS("Points", "the points for each finishing place", false, false),
    RULES("Rules", "the Game Settings page: power, grip, rain, pit stops, race rules, damage, marshals", false, false),
    TABLES("Pit lane, set-ups and lap records", "pit lane order, car and circuit set-ups, lap records", false, false),
    PHYSICS("Car performance tables", "engine, tyre, weight, wing and opposition tables", false, false),
    OPTIONS("Game options", "F1PREFS.DAT: opposition, race distance, driving aids and display", false, false),
    SOUNDS("Sound effects", "the AdLib and Sound Blaster sound effects", false, false),
    HELMET_PICTURES("Menu helmets", "the helmet beside each driver on the menus (pictures)", true, true),
    FLAG_PICTURES("Language flags", "the flags on the language screen (pictures)", false, true),
    BACKDROP_PICTURES("Menu backgrounds", "the full-screen pictures behind the menus (pictures)", false, true),
    CIRCUIT_PICTURES("Circuit pictures", "each circuit's picture on the circuit menu (pictures)", false, true),
    CIRCUITS("Circuits", "the 16 circuit files, copied whole", false, true);

    public final String label, detail;
    /** True for the groups that belong to a team and its drivers (the ones a single team can bring). */
    public final boolean team;
    /** True for the groups stored as further files, which make the dataset a zip. */
    public final boolean files;

    DatasetGroup(String label, String detail, boolean team, boolean files)
    {
        this.label = label;
        this.detail = detail;
        this.team = team;
        this.files = files;
    }
}
