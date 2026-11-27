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

package cfevolution.screens;

import cfevolution.data.executable.Executable;
import cfevolution.data.executable.GameDrivers;
import cfevolution.data.executable.GamePoints;
import cfevolution.data.executable.GameTables;
import cfevolution.data.executable.GameTeams;
import java.util.List;
import cfevolution.edit.ExecutableEdits;
import cfevolution.edit.LineUpMoves;
import cfevolution.edit.ExecutableSaving;
import cfevolution.edit.GameSettingsPage;
import cfevolution.edit.LapRecordsPage;
import cfevolution.edit.LiveriesPage;
import cfevolution.data.executable.GameSprites;
import java.io.File;
import cfevolution.edit.SetupsPage;
import cfevolution.edit.TablePage;
import cfevolution.edit.Message;

/**
 * GP.EXE's screen (2026-11-17), with the pages of the first Android version:
 * drivers, teams, points and the pit lane; then Liveries (LiveriesPage), Game Settings (GameSettingsPage),
 * Setups (SetupsPage), Lap Records (LapRecordsPage), Car Physics and
 * Opposition Levels (TablePage). Every rule and message is the
 * desktop's own (ExecutableEdits, GameTables, ExecutableSaving). Each setter
 * takes what the user typed and returns the message to show when it is not
 * allowed, or null. Plain Java: runs on Android and the desktop JVM.
 */
public final class ExecutableScreen
{
    public final Executable exe;
    private boolean modified;

    /** The pages, in the desktop's order (those not here yet left out). */
    public static final String[] PAGES = { "Drivers", "Teams", "Points", LiveriesPage.TITLE, GameSettingsPage.TITLE, "Pit Lane",
        SetupsPage.TITLE, LapRecordsPage.TITLE, "Car Physics", "Opposition Levels" };

    /** The page descriptions the desktop shows (plain text). */
    public static final String[] PAGE_TEXT = {
        "Names for all 40 car numbers. Skill is on the Teams page, beside each seat.",
        "Team and engine names, each team's engine power, which cars drive for it and their skill. Lower skill numbers are faster drivers: 1 is the game's best, and 0 faster still. Power differences only show with \"1991 Levels\" opposition. The game races the first 18 teams. A new line-up takes effect when a new championship is started.",
        "Championship points for each finishing position.",
        LiveriesPage.DESCRIPTION.replace("Click", "Tap"),
        GameSettingsPage.DESCRIPTION,
        "Which team uses each pit box, starting at the pit lane entrance. Choosing a team for a box swaps it with the team already there. Used at every circuit. A team with no cars in a session leaves its box empty; the other teams do not move up.",
        setupsText(),
        LapRecordsPage.DESCRIPTION,
        "The car's engine, tyres, weight and wings.",
        "The computer cars at Rookie, Amateur, Semi-Pro, Pro and Ace." };

    /** The folder GP.EXE is in, where the game's pictures are. */
    private final File folder;
    /** GP.EXE itself, for Revert File and Revert Page. */
    public final File file;

    ExecutableScreen(Executable exe, File file)
    {
        this.exe = exe;
        this.file = file;
        this.folder = file.getAbsoluteFile().getParentFile();
    }

    // --- Revert Page (Hrvoje, the owner, 2026-11-23) --------------------------

    /** The question for Revert Page on page n (on Liveries, the tab shown): the first option reverts. */
    public Message revertPageQuestion(int n, int liveriesTab)
    {
        return cfevolution.edit.Revert.pageQuestion(cfevolution.edit.ExecutableRevert.pageName(PAGES[n], n, liveriesTab));
    }

    /** Puts page n (on Liveries, one tab) back as it was when GP.EXE was opened; the message when it cannot, else null. */
    public Message revertPage(int n, int liveriesTab)
    {
        Executable old = cfevolution.edit.ExecutableRevert.whenOpened(file);
        if (old == null)
            return Message.warning("Not Reverted", "How GP.EXE was when it was opened is not known, so this page cannot be reverted.");
        cfevolution.edit.ExecutableRevert.page(exe, old, n, liveriesTab);
        modified = true;
        return null;
    }

    public String title()
    {
        return "GP.EXE " + exe.returnGameVersionString();
    }

    public boolean isModified()
    {
        return modified;
    }

    private Message edited(Message problem)
    {
        if (problem == null)
            modified = true;
        return problem;
    }

    // --- Drivers ----------------------------------------------------------

    private GameDrivers drivers()
    {
        return exe.getGameDrivers();
    }

    public String driverName(int car)          { return drivers().getDriverName(car); }
    public int skill(int car, boolean race)    { return ExecutableEdits.getSkill(drivers(), car, race); }

    public Message setDriverName(int car, String text)
    {
        return edited(ExecutableEdits.setDriverName(drivers(), car, text));
    }

    public Message setSkill(int car, boolean race, String text)
    {
        return edited(ExecutableEdits.setSkill(drivers(), car, race, text));
    }

    // --- Teams ------------------------------------------------------------

    private GameTeams teams()
    {
        return exe.getGameTeams();
    }

    public String teamName(int team)           { return teams().getTeamName(team); }
    public String engineName(int team)         { return teams().getEngineName(team); }
    public int power(int team)                 { return teams().getBHPSetting(team); }
    public int teamCar(int team, int seat)     { return teams().getTeamCar(team, seat); }

    /** "None", or the car number and its driver, as the desktop lists cars. */
    public String carLabel(int car)
    {
        return car == 0 ? "None" : car + "  " + driverName(car - 1);
    }

    public Message setTeamName(int team, String text)   { return edited(ExecutableEdits.setTeamName(teams(), team, text)); }
    public Message setEngineName(int team, String text) { return edited(ExecutableEdits.setEngineName(teams(), team, text)); }
    public Message setPower(int team, String text)      { return edited(ExecutableEdits.setPower(teams(), team, text)); }
    public Message setTeamCar(int team, int seat, int car) { return edited(ExecutableEdits.setTeamCar(teams(), team, seat, car)); }

    // --- Moving drivers and teams, skill by seat (Hrvoje, 2026-11-15; built 2026-11-23) ---

    /** A driver or team one place up or down; returns where it is now. */
    public int moveDriver(int car, boolean up)
    {
        int to = LineUpMoves.moveDriver(exe, car, up);
        if (to != car)
            modified = true;
        return to;
    }

    public int moveTeam(int team, boolean up)
    {
        int to = LineUpMoves.moveTeam(exe, team, up);
        if (to != team)
            modified = true;
        return to;
    }

    public void swapDrivers(int a, int b)
    {
        if (a != b) { LineUpMoves.swapDrivers(exe, a, b); modified = true; }
    }

    public void swapTeams(int a, int b)
    {
        if (a != b) { LineUpMoves.swapTeams(exe, a, b); modified = true; }
    }

    public String[] driverChoices() { return LineUpMoves.driverChoices(exe); }
    public String[] teamChoices()   { return LineUpMoves.teamChoices(exe); }

    /** The skill beside a seat, or -1 when the seat is empty. */
    public int seatSkill(int team, int seat, boolean race) { return LineUpMoves.seatSkill(exe, team, seat, race); }

    public Message setSeatSkill(int team, int seat, boolean race, String text)
    {
        return edited(LineUpMoves.setSeatSkill(exe, team, seat, race, text));
    }

    /** Asked before a car that drives in another seat is put in this one; null if nothing to ask. */
    public Message seatQuestion(int team, int seat, int car) { return LineUpMoves.duplicateQuestion(exe, team, seat, car); }

    /** Puts a car in a seat; how is the option chosen from seatQuestion (or null when none was asked). */
    public boolean placeDriver(int team, int seat, int car, String how)
    {
        boolean changed = LineUpMoves.placeDriver(exe, team, seat, car, how == null ? LineUpMoves.MOVE : how);
        if (changed)
            modified = true;
        return changed;
    }

    // --- Points -----------------------------------------------------------

    public String position(int row)            { return ExecutableEdits.ordinal(row + 1); }
    public int points(int row)                 { return ExecutableEdits.getPoints(exe.getGamePoints(), row); }

    public Message setPoints(int row, String text)
    {
        GamePoints p = exe.getGamePoints();
        return edited(ExecutableEdits.setPoints(p, row, text));
    }

    // --- Pit lane ---------------------------------------------------------

    private GameTables tables()
    {
        return exe.getGameTables();
    }

    public static final int PIT_BOXES = GameTables.PIT_BOXES;

    public String pitBoxLabel(int box)
    {
        return "Pit box " + (box + 1) + (box == 0 ? "  (pit lane entrance)" : "");
    }

    /** The team (1-based) in box 0-17. */
    public int pitBoxTeam(int box)             { return tables().getPitBoxTeam(box); }

    public String teamLabel(int team)
    {
        return team + "  " + (team >= 1 && team <= 20 ? teamName(team - 1) : "?");
    }

    /** Puts a team in a box, swapping it with the team already there. */
    public void setPitBoxTeam(int box, int team)
    {
        tables().setPitBoxTeam(box, team);
        modified = true;
    }

    /** Moves a box's team one box nearer the entrance (up) or further away; returns its new box. */
    public int movePitBox(int box, boolean up)
    {
        int other = up ? box - 1 : box + 1;
        if (other < 0 || other >= PIT_BOXES)
            return box;
        tables().swapPitBoxes(box, other);
        modified = true;
        return other;
    }

    public void resetPitOrder()
    {
        tables().resetPitOrder();
        modified = true;
    }

    /** The warning the desktop shows above the boxes, or null when the order is valid. */
    public String pitWarning()
    {
        return tables().isPitOrderValid() ? null
            : "This order has a team missing or listed twice, so the game would not give every team its own box. "
            + "Choose a team for each box, or use Reset to 1991 Order.";
    }

    // --- Game Settings ------------------------------------------------------

    private List<GameSettingsPage.Group> settings;

    /** The Game Settings page's groups (the desktop's, from the core). */
    public List<GameSettingsPage.Group> settings()
    {
        if (settings == null)
            settings = GameSettingsPage.groups(exe);
        return settings;
    }

    /** A number typed for a setting; the message when it is refused, else null. */
    public Message setSetting(GameSettingsPage.Item item, String text)
    {
        int before = item.get();
        Message m = item.set(text);
        if (m == null && item.get() != before)
            modified = true;
        return m;
    }

    /** Turns a switch on or off. */
    public void toggle(GameSettingsPage.Item item)
    {
        item.set(item.isOn() ? 0 : 1);
        modified = true;
    }

    /** The Setups description as plain paragraphs, without the desktop's own line about selecting rows. */
    private static String setupsText()
    {
        String[] d = SetupsPage.DESCRIPTION;
        StringBuilder s = new StringBuilder(d[0] + " " + d[1]);
        for (int i = 2; i < d.length - 1; i++)
            s.append(" ").append(d[i]);
        return s.toString().replace("&gt;", ">");
    }

    // --- Liveries ----------------------------------------------------------

    private LiveriesPage liveries;
    private GameSprites sprites;
    private boolean spritesLooked;

    public LiveriesPage liveries()
    {
        if (liveries == null)
            liveries = new LiveriesPage(exe.getGameLiveries(), teams(), drivers());
        return liveries;
    }

    /** Save Names into the Executable (Hrvoje, 2026-11-21): off, saving leaves GP.EXE's own names. */
    public boolean saveNames()
    {
        return exe.getSaveNames();
    }

    public void setSaveNames(boolean on)
    {
        exe.setSaveNames(on);
        if (on && exe.namesNotInFile())     // saving now writes names GP.EXE lacks (2026-11-22)
            modified = true;
    }

    /** Names shown that leaving would lose: Save Names into the Executable is off and they are not in GP.EXE. */
    public boolean namesOnlyShown()
    {
        return !exe.getSaveNames() && exe.namesNotInFile();
    }

    /** The game's 3D car for the close-up picture (read from GP.EXE), or null. */
    public cfevolution.data.executable.CarModel carModel()
    {
        return exe.getCarModel();
    }

    /** The close-up car of a team at a heading step and tilt (null without the game's pictures or the model). */
    /** A team's car from the game's camera height; usualDesign shows team slot 1 as the game draws it further away. */
    public GameSprites.Sprite closeUp(int team, int headingStep, boolean usualDesign)
    {
        return carModel() == null || sprites() == null ? null : LiveriesPage.closeUp(carModel(), sprites(), team, headingStep, usualDesign);
    }

    /** The game's pictures (F1GPDATA.DAT and F1GPDATB.DAT beside GP.EXE), or null if the folder lacks them. */
    public GameSprites sprites()
    {
        if (!spritesLooked)
        {
            sprites = folder == null ? null : GameSprites.find(folder);
            spritesLooked = true;
        }
        return sprites;
    }

    /** A part's palette colour (0-255). */
    public void setLiveryColour(int tab, int row, int part, int colour)
    {
        if (liveries().set(tab, row, part, colour))
            modified = true;
    }

    /** The design last copied on each tab, shared by every GP.EXE opened, as on the desktop. */
    private static final java.util.Map<String, int[]> copiedDesigns = new java.util.HashMap<String, int[]>();
    private static final java.util.Map<String, String> copiedNames = new java.util.HashMap<String, String>();

    public void copyDesign(int tab, int row)
    {
        copiedDesigns.put(LiveriesPage.kind(tab), liveries().design(tab, row));
        copiedNames.put(LiveriesPage.kind(tab), liveries().copiedText(tab, row));
    }

    /** "Copied: ..." for the tab, or null if nothing has been copied there. */
    public String copiedText(int tab)
    {
        return copiedNames.get(LiveriesPage.kind(tab));
    }

    public void pasteDesign(int tab, int[] rows)
    {
        int[] design = copiedDesigns.get(LiveriesPage.kind(tab));
        if (design != null && liveries().paste(tab, rows, design))
            modified = true;
    }

    /** Pastes the given design (for the screen check, which keeps its own). */
    public void pasteDesign(int tab, int[] rows, int[] design)
    {
        if (liveries().paste(tab, rows, design))
            modified = true;
    }

    /** Clear Design: every part of these rows white (Hrvoje, 2026-11-15). */
    public void clearDesign(int tab, int[] rows)
    {
        if (liveries().clear(tab, rows))
            modified = true;
    }

    /** The colour last copied with Copy Colour (shared by every tab, as on the desktop), or -1. */
    private static int copiedColour = -1;

    public void copyColour(int tab, int row, int part)
    {
        copiedColour = liveries().get(tab, row, part);
    }

    public int copiedColour()
    {
        return copiedColour;
    }

    /** Paste Colour: the copied colour on one part of row r. */
    public void pasteColour(int tab, int row, int part)
    {
        if (copiedColour >= 0 && liveries().paint(tab, new int[] { row }, part, copiedColour))
            modified = true;
    }

    /** One of ArgEditor's files for row r; the message when refused, else null. */
    public Message importLivery(int tab, int row, String name, byte[] file)
    {
        Message m = liveries().importFile(tab, row, name, file);
        if (m == null)
            modified = true;
        return m;
    }

    public byte[] exportLivery(int tab, int row)
    {
        return liveries().exportFile(tab, row);
    }

    // --- Setups -----------------------------------------------------------

    private SetupsPage setups;

    public SetupsPage setups()
    {
        if (setups == null)
            setups = new SetupsPage(tables(), drivers());
        return setups;
    }

    /** Typed text for one value of a setup (column 0-9); the message when refused, else null. */
    public Message setSetup(int tab, int row, int col, String text)
    {
        boolean[] changed = new boolean[1];
        Message m = setups().set(tab, row, col, text, changed);
        if (changed[0])
            modified = true;
        return m;
    }

    /**
     * A whole row typed at once (10 values). Gears must rise one to the next,
     * so values are tried until none more can be taken; the first refusal
     * left is returned, or null when every value was taken.
     */
    public Message setSetupRow(int tab, int row, String[] values)
    {
        boolean[] done = new boolean[values.length];
        Message first = null;
        for (int pass = 0; pass < values.length; pass++)
        {
            boolean progress = false;
            first = null;
            for (int c = 0; c < values.length; c++)
            {
                if (done[c])
                    continue;
                Message m = setSetup(tab, row, c, values[c]);
                if (m == null)
                {
                    done[c] = true;
                    progress = true;
                }
                else if (first == null)
                    first = m;
            }
            if (first == null || !progress)
                break;
        }
        return first;
    }

    public void toggleSeparate()
    {
        setups().setSeparate(!setups().isSeparate());
        modified = true;
    }

    public void writeSetups(int tab, int[] rows, int[] setup)
    {
        setups().write(tab, rows, setup);
        modified = true;
    }

    public void applySetupToAll(int tab, int row)
    {
        setups().applyToAll(tab, row);
        modified = true;
    }

    public void copySetupsBetween(boolean toRace)
    {
        setups().copyBetween(toRace);
        modified = true;
    }

    // --- Lap Records, Car Physics, Opposition Levels -------------------------

    private LapRecordsPage records;
    private TablePage physics, opposition;

    public LapRecordsPage records()
    {
        if (records == null)
            records = new LapRecordsPage(tables());
        return records;
    }

    /** Typed text for a lap record (row a circuit, column as LapRecordsPage.COLUMNS); the message when refused, else null. */
    public Message setRecord(int row, int col, String text)
    {
        boolean[] changed = new boolean[1];
        Message m = records().set(row, col, text, changed);
        if (changed[0])
            modified = true;
        return m;
    }

    public TablePage physics()
    {
        if (physics == null)
            physics = TablePage.physics(exe);
        return physics;
    }

    public TablePage opposition()
    {
        if (opposition == null)
            opposition = TablePage.opposition(exe);
        return opposition;
    }

    /** Typed text for a grid's cell; the message when refused, else null. */
    public Message setCell(TablePage.Grid grid, int row, int col, String text)
    {
        boolean[] changed = new boolean[1];
        Message m = grid.set(row, col, text, changed);
        if (changed[0])
            modified = true;
        return m;
    }

    // --- Saving -----------------------------------------------------------

    /** Saves GP.EXE; returns the message when it was not saved, or null. */
    public Message save()
    {
        Message m = ExecutableSaving.save(exe);
        if (m == null)
            modified = false;
        return m;
    }
}
