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

import cfevolution.data.dataset.OtherEditorFiles;
import cfevolution.data.executable.CarModel;
import cfevolution.data.executable.GameDrivers;
import cfevolution.data.executable.GameLiveries;
import cfevolution.data.executable.GameSprites;
import cfevolution.data.executable.GameTeams;

/**
 * GP.EXE's Liveries page (moved out of the desktop window 2026-11-17, word for
 * word): the Cars, Helmets and Pit crews tabs, a row per team or driver and a
 * colour per part; copying a design to other rows; ArgEditor's .car and .hel
 * files; which of the game's pictures each tab shows and how they turn; and
 * what the palette's special colours are for. The desktop and Android pages
 * are both built from this.
 */
public final class LiveriesPage
{
    public static final String TITLE = "Liveries";
    public static final String DESCRIPTION =
        "Colours of each team's cars and pit crew, and each driver's helmet. Click a colour, or part of the picture, to change it.";

    public static final int CARS = 0, HELMETS = 1, PIT_CREWS = 2;
    public static final String[] TAB_TITLES = { "Cars", "Helmets", "Pit crews" };

    /** ArgEditor's file for one team's car colours, or one driver's helmet colours; the pit crew tab has none. */
    public static final String CAR_FILE = "car", HELMET_FILE = "hel";

    public static final String PICTURE_HINT = "Click part of the picture to change its colour.";
    public static final String TURN = "Turn";
    public static final String MISSING_PICTURES = "The pictures are drawn from the game's own files, which are not in this "
        + "executable's folder. Choose the folder the game is installed in to see them.";
    public static final String CHOOSE_GAME_FOLDER = "Choose Game Folder...";
    public static final Message PICTURES_NOT_FOUND = Message.warning("Pictures not found",
        "That folder does not have the game's F1GPDATA.DAT and F1GPDATB.DAT files.\n"
        + "Choose the folder the game is installed in.");

    private final GameLiveries liveries;
    private final GameTeams teams;
    private final GameDrivers drivers;

    public LiveriesPage(GameLiveries liveries, GameTeams teams, GameDrivers drivers)
    {
        this.liveries = liveries;
        this.teams = teams;
        this.drivers = drivers;
    }

    public GameLiveries liveries()
    {
        return liveries;
    }

    public int rows(int tab)
    {
        return tab == HELMETS ? GameLiveries.DRIVERS : GameLiveries.TEAMS;
    }

    /** A row's label: the team, or the car number and its driver. */
    public String rowName(int tab, int r)
    {
        return tab == HELMETS ? (r + 1) + "  " + drivers.getDriverName(r) : teams.getTeamName(r);
    }

    /** A row's name without its car number. */
    public String plainName(int tab, int r)
    {
        return rowName(tab, r).replaceAll("^[0-9]+ +", "");
    }

    public static String[] parts(int tab)
    {
        return tab == CARS ? GameLiveries.CAR_PARTS : tab == HELMETS ? GameLiveries.HELMET_PARTS : GameLiveries.PIT_CREW_PARTS;
    }

    /** Which of the set's 16 colours each part is. */
    public static int[] slots(int tab)
    {
        return tab == CARS ? GameLiveries.CAR_BYTES : tab == HELMETS ? GameLiveries.HELMET_BYTES : GameLiveries.PIT_CREW_BYTES;
    }

    /** The part a colour of the set is, or -1 if the page does not offer it. */
    public static int partOfSlot(int tab, int slot)
    {
        int[] slots = slots(tab);
        for (int p = 0; p < slots.length; p++)
            if (slots[p] == slot)
                return p;
        return -1;
    }

    /** A part's palette colour (0-255). */
    public int get(int tab, int r, int part)
    {
        return tab == CARS ? liveries.getCarColour(r, part) : tab == HELMETS ? liveries.getHelmetColour(r, part)
                                                                             : liveries.getPitCrewColour(r, part);
    }

    /** Gives a part a palette colour; returns whether it changed. */
    public boolean set(int tab, int r, int part, int colour)
    {
        if (colour < 0 || colour > 255 || colour == get(tab, r, part))
            return false;
        if (tab == CARS)
            liveries.setCarColour(r, part, colour);
        else if (tab == HELMETS)
            liveries.setHelmetColour(r, part, colour);
        else
            liveries.setPitCrewColour(r, part, colour);
        return true;
    }

    /** Any of the set's 16 colours, for drawing the pictures. */
    public int setColour(int tab, int r, int slot)
    {
        return tab == CARS ? liveries.getCarSetColour(r, slot) : tab == HELMETS ? liveries.getHelmetSetColour(r, slot)
                                                                                : liveries.getPitCrewSetColour(r, slot);
    }

    /** The title of the colour chooser for a part. */
    public String chooserTitle(int tab, int r, int part)
    {
        return rowName(tab, r) + ": " + parts(tab)[part];
    }

    // --- Copy and paste ---------------------------------------------------

    /** The key each tab's copied design is kept under (shared by every GP.EXE open, so a design can go from one game to another). */
    public static String kind(int tab)
    {
        return tab == CARS ? CAR_FILE : tab == HELMETS ? HELMET_FILE : "crew";
    }

    public static String what(int tab)
    {
        return tab == CARS ? "car colours" : tab == HELMETS ? "helmet" : "pit crew colours";
    }

    public static String rowWord(int tab)
    {
        return tab == HELMETS ? "driver" : "team";
    }

    public static String copyTip(int tab)
    {
        return "Copy the selected " + rowWord(tab) + "'s " + what(tab);
    }

    public static String pasteTip(int tab)
    {
        return "Give every selected " + rowWord(tab) + " the copied " + what(tab);
    }

    /** Beside the desktop's Copy and Paste buttons. */
    public static String copyPasteNote(int tab)
    {
        return "the selected " + rowWord(tab) + "'s " + what(tab) + " onto other " + rowWord(tab)
            + "s (select several to paste onto them all)";
    }

    /** A row's design: the colours of the parts the page offers (the bytes it keeps as the game has them are left out). */
    public int[] design(int tab, int r)
    {
        int[] design = new int[parts(tab).length];
        for (int p = 0; p < design.length; p++)
            design[p] = get(tab, r, p);
        return design;
    }

    /** What the page says after Copy. */
    public String copiedText(int tab, int r)
    {
        return "Copied: " + plainName(tab, r);
    }

    /** Gives each of these rows the design; returns whether anything changed. */
    public boolean paste(int tab, int[] rows, int[] design)
    {
        boolean changed = false;
        for (int r : rows)
            for (int p = 0; p < design.length; p++)
                changed |= set(tab, r, p, design[p]);
        return changed;
    }

    // --- Clearing, one colour at a time, and the rows' order (Hrvoje, 2026-11-15/16) ---

    /** White: the colour the game's own liveries use for white (187 times; 46 is a shade greyer). */
    public static final int WHITE = 47;
    public static final String CLEAR = "Clear Design";
    public static final String COPY_COLOUR = "Copy Colour", PASTE_COLOUR = "Paste Colour";
    public static final String HIDE_UNUSED = "Hide Unused Teams";
    public static final String HIDE_UNUSED_TIP = "Leave out teams with no drivers: the game does not show them";
    public static final String[] ORDERS = { "By car number", "By team" };
    public static final String ORDER_TIP = "List the helmets by car number, or team by team (each team's first driver, then its second)";

    public static String clearTip(int tab)
    {
        return "Make every part of the selected " + rowWord(tab) + "s' " + what(tab) + " white";
    }

    /** Every offered part of these rows white; returns whether anything changed. */
    public boolean clear(int tab, int[] rows)
    {
        boolean changed = false;
        for (int r : rows)
            for (int p = 0; p < parts(tab).length; p++)
                changed |= set(tab, r, p, WHITE);
        return changed;
    }

    /** Paste Colour: one part of these rows in a colour; returns whether anything changed. */
    public boolean paint(int tab, int[] rows, int part, int colour)
    {
        boolean changed = false;
        for (int r : rows)
            changed |= set(tab, r, part, colour);
        return changed;
    }

    /** Whether a team has a driver, so the game shows it. */
    public boolean used(int team)
    {
        return teams.getTeamCar(team, 0) != 0 || teams.getTeamCar(team, 1) != 0;
    }

    /** The team a car number drives for, or -1. */
    public int teamOf(int driver)
    {
        for (int t = 0; t < GameLiveries.TEAMS; t++)
            for (int seat = 0; seat < 2; seat++)
                if (teams.getTeamCar(t, seat) == driver + 1)
                    return t;
        return -1;
    }

    /**
     * The rows in the order a tab lists them. Helmets by team: each team's
     * first driver then its second, team by team, then the drivers in no team
     * by car number. Cars and pit crews may leave out the teams with no drivers.
     */
    public int[] order(int tab, boolean byTeam, boolean hideUnused)
    {
        java.util.List<Integer> rows = new java.util.ArrayList<Integer>();
        if (tab == HELMETS)
        {
            boolean[] listed = new boolean[GameLiveries.DRIVERS];
            if (byTeam)
                for (int t = 0; t < GameLiveries.TEAMS; t++)
                    for (int seat = 0; seat < 2; seat++)
                    {
                        int car = teams.getTeamCar(t, seat);
                        if (car >= 1 && car <= GameLiveries.DRIVERS && !listed[car - 1])
                        {
                            listed[car - 1] = true;
                            rows.add(car - 1);
                        }
                    }
            for (int d = 0; d < GameLiveries.DRIVERS; d++)
                if (!listed[d])
                    rows.add(d);
        }
        else
            for (int t = 0; t < GameLiveries.TEAMS; t++)
                if (!hideUnused || used(t))
                    rows.add(t);
        int[] out = new int[rows.size()];
        for (int i = 0; i < out.length; i++)
            out[i] = rows.get(i);
        return out;
    }

    /** A row's label in a list ordered by team: the helmet's team after the driver. */
    public String rowName(int tab, int r, boolean byTeam)
    {
        if (tab != HELMETS || !byTeam)
            return rowName(tab, r);
        int t = teamOf(r);
        return rowName(tab, r) + (t < 0 ? "  (no team)" : "  (" + teams.getTeamName(t) + ")");
    }

    // --- ArgEditor's files ------------------------------------------------

    /** Whether the tab has a file of ArgEditor's (cars and helmets do). */
    public static boolean hasFile(int tab)
    {
        return tab != PIT_CREWS;
    }

    private static String fileWhat(int tab)
    {
        return tab == CARS ? "car colours" : "helmet colours";
    }

    public static String importLabel(int tab)
    {
        return "Import ." + kind(tab) + "...";
    }

    public static String exportLabel(int tab)
    {
        return "Export ." + kind(tab) + "...";
    }

    public static String importTip(int tab)
    {
        return "Bring in " + fileWhat(tab) + " saved by ArgEditor, for the selected " + (tab == CARS ? "team" : "driver");
    }

    public static String exportTip(int tab)
    {
        return "Save the selected " + (tab == CARS ? "team's " : "driver's ") + fileWhat(tab) + " for ArgEditor";
    }

    /** Beside the desktop's import and export buttons. */
    public static String fileNote(int tab)
    {
        return "for the selected " + (tab == CARS ? "team" : "driver") + ", in ArgEditor's file";
    }

    /** The file chooser's description of the files. */
    public static String fileFilter(int tab)
    {
        return "ArgEditor " + fileWhat(tab) + " (*." + kind(tab) + ")";
    }

    public String importTitle(int tab, int r)
    {
        return "Import " + fileWhat(tab) + " for " + rowName(tab, r);
    }

    public String exportTitle(int tab, int r)
    {
        return "Export " + fileWhat(tab) + " of " + rowName(tab, r);
    }

    /** The name an export suggests: the team or driver, with the file's ending. */
    public String exportName(int tab, int r)
    {
        String base = plainName(tab, r).replaceAll("[^A-Za-z0-9 _-]", "").trim();
        return (base.isEmpty() ? kind(tab) : base) + "." + kind(tab);
    }

    /**
     * Gives row r the colours in one of ArgEditor's files (only the parts the
     * page offers; the others stay as the game has them). Returns why it was
     * refused, or null.
     */
    public Message importFile(int tab, int r, String fileName, byte[] file)
    {
        int[] colours = file == null ? null : tab == CARS ? OtherEditorFiles.readCarFile(file) : OtherEditorFiles.readHelmetFile(file);
        if (colours == null)
            return notArgEditorFile(tab, fileName);
        int[] slots = slots(tab);
        for (int p = 0; p < slots.length; p++)
            set(tab, r, p, colours[slots[p]]);
        return null;
    }

    public static Message notArgEditorFile(int tab, String fileName)
    {
        return Message.warning("Import ." + kind(tab), fileName + " is not an ArgEditor ." + kind(tab) + " file, so nothing was changed.");
    }

    /** Row r's whole colour set as ArgEditor's file. */
    public byte[] exportFile(int tab, int r)
    {
        int[] colours = new int[16];
        for (int s = 0; s < 16; s++)
            colours[s] = setColour(tab, r, s);
        return tab == CARS ? OtherEditorFiles.writeCarFile(colours) : OtherEditorFiles.writeHelmetFile(colours);
    }

    public static Message notSaved(int tab, String fileName)
    {
        return Message.error("Export ." + kind(tab), fileName + " could not be saved.");
    }

    // --- The game's pictures ----------------------------------------------

    /** Whether the tab's pictures turn (one picture turned with a slider) rather than being stepped through. */
    public static boolean turns(int tab)
    {
        return tab != PIT_CREWS;
    }

    private static int first(int tab)
    {
        return tab == CARS ? GameSprites.CAR_FIRST : GameSprites.HELMET_FIRST;
    }

    private static int last(int tab)
    {
        return tab == CARS ? GameSprites.CAR_LAST : GameSprites.HELMET_LAST;
    }

    /** How much the desktop enlarges the tab's pictures, at most. */
    public static int scale(int tab)
    {
        return tab == CARS ? 6 : tab == HELMETS ? 12 : 2;
    }

    /**
     * How many positions the tab's pictures have: for a turning picture, behind,
     * round one side to in front, then the game's mirror image back round the
     * other; for stepped pictures, one each.
     */
    public static int positions(int tab)
    {
        return turns(tab) ? 2 * (last(tab) - first(tab) + 1) - 1 : GameSprites.PIT_CREW.length;
    }

    /** The position shown first. */
    public static int startPosition(int tab)
    {
        return tab == CARS ? 212 - GameSprites.CAR_FIRST : tab == HELMETS ? 70 - GameSprites.HELMET_FIRST : 0;
    }

    /** The game's picture at a position. */
    public static int pictureAt(int tab, int position)
    {
        if (!turns(tab))
            return GameSprites.PIT_CREW[position];
        int n = last(tab) - first(tab) + 1;
        return position < n ? first(tab) + position : first(tab) + 2 * n - 2 - position;
    }

    /** Whether the picture at a position is drawn mirrored. */
    public static boolean mirroredAt(int tab, int position)
    {
        return turns(tab) && position >= last(tab) - first(tab) + 1;
    }

    /** The views a tab's turning picture can jump to (Hrvoje, 2026-11-16); none for the pit crews. */
    public static String[] views(int tab)
    {
        return tab == CARS ? new String[] { "Front", "Side", "Rear" }
             : tab == HELMETS ? new String[] { "Front", "Left Side", "Right Side" } : new String[0];
    }

    /** The position of a view. Cars: in front 44, the side 36 (the widest picture, nose to the right),
        behind 0. Helmets: in front 8; facing left (12) shows the driver's left side, facing right (4) his right. */
    public static int viewPosition(int tab, int view)
    {
        return tab == CARS ? new int[] { 44, 36, 0 }[view] : new int[] { 8, 12, 4 }[view];
    }

    // --- The car: the game's 3D model at the game's camera (Hrvoje, 2026-11-21 and 11-25) ---

    /**
     * Since 2026-11-25 the Cars tab has one view: the game's 3D car as its chase
     * camera and the camera in front see it, from that height, turning all the
     * way round. Team slot 1 has two designs: the game draws it close up with its
     * own McLaren-shaped car (one part fewer: no Nose angle) and further away with
     * the usual car's pictures; a switch shows either.
     */
    public static final String CAR_TIP = "The car as the game draws it in the chase view and the view from in front, "
        + "from the same height, with the first driver's helmet";
    public static final String MCLAREN_DESIGN = "Close-up Design", USUAL_DESIGN = "Distant Design";
    public static final String MCLAREN_DESIGN_TIP = "Team slot 1's own car, which the game draws close to the camera: "
        + "its nose has no Nose angle part";
    public static final String USUAL_DESIGN_TIP = "Team slot 1 further from the camera, where the game draws it like every other team's car";
    /** Shown on the Cars tab while team slot 1 is selected. */
    public static final String TEAM_ONE_NOTE = "Team slot 1 has two designs. Close to the camera the game draws its own car, "
        + "whose nose has no Nose angle part, so that colour is not seen there. Further away it looks like every other "
        + "team's car. The buttons show either.";
    public static final String[] CLOSE_UP_VIEWS = { "Front", "Side", "Rear" };
    /** Each view's heading: 180 in front, 90 the right side with the nose to the right, 0 behind. */
    private static final int[] CLOSE_UP_VIEW_HEADINGS = { 180, 90, 0 };
    /** The car turns in steps of this many degrees. */
    public static final int HEADING_STEP = 5;
    public static final int HEADINGS = 360 / HEADING_STEP;
    /** The heading shown first: in front and to the side, where nearly every part shows. */
    public static final int START_HEADING = 140 / HEADING_STEP;
    /**
     * The car is drawn in the game's own pixels, which the game's 4:3 screen shows 1.2
     * times as tall as wide; the editor enlarges each one to 2.5 pixels across and 3
     * down (as the game looks on an 800 x 600 screen), so the car keeps its shape.
     */
    private static final int ACROSS_TIMES_2 = 5, DOWN = 3;

    /** Heading step (0 to HEADINGS - 1) of a view. */
    public static int closeUpHeading(int view)
    {
        return CLOSE_UP_VIEW_HEADINGS[view] / HEADING_STEP;
    }

    /** Whose helmet the close-up car wears: the team's first driver, else its second, else -1 (a plain helmet). */
    public int helmetDriver(int team)
    {
        for (int seat = 0; seat < 2; seat++)
        {
            int car = teams.getTeamCar(team, seat);
            if (car >= 1 && car <= GameLiveries.DRIVERS)
                return car - 1;
        }
        return -1;
    }

    /** A team's car, drawn as the game draws its 3D car from the game's camera height, enlarged. Team
        slot 1 is drawn with its own (McLaren) car unless usualDesign asks for the one the game uses further away. */
    public static GameSprites.Sprite closeUp(CarModel model, GameSprites sprites, int team, int headingStep, boolean usualDesign)
    {
        return enlarged(model.draw(sprites, headingStep * HEADING_STEP, team == 0 && !usualDesign, true));
    }

    /** A picture in the game's pixels made larger without smoothing, in the game's proportions. */
    static GameSprites.Sprite enlarged(GameSprites.Sprite game)
    {
        int width = game.width * ACROSS_TIMES_2 / 2, height = game.height * DOWN;
        byte[] slots = new byte[width * height];
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                slots[y * width + x] = (byte) game.slotAt(x * 2 / ACROSS_TIMES_2, y / DOWN);
        return GameSprites.Sprite.of(width, height, slots);
    }

    /** The size of every close-up drawing, {width, height}. */
    public static int[] closeUpBox()
    {
        int[] box = CarModel.box();
        return new int[] { box[0] * ACROSS_TIMES_2 / 2, box[1] * DOWN };
    }

    /**
     * The palette colour of a picture's slot: 0-15 from the row's own set;
     * on the Cars tab 16-31 are the close-up car's helmet, from its driver's
     * set (a plain white helmet with a dark visor when the team has no driver).
     */
    public int pictureColour(int tab, int r, int slot)
    {
        if (slot < CarModel.HELMET_SLOTS)
            return setColour(tab, r, slot);
        int driver = tab == CARS ? helmetDriver(r) : -1;
        int s = slot - CarModel.HELMET_SLOTS;
        if (driver < 0)
            return s == 0 ? 0 : WHITE;
        return liveries.getHelmetSetColour(driver, s);
    }

    /** What stepped pictures say under the picture. */
    public static String pictureNumber(int tab, int position)
    {
        return "Picture " + (position + 1) + " of " + positions(tab);
    }

    /** The largest of the tab's pictures, {width, height}, so turning or stepping does not move the page around. */
    public static int[] box(int tab, GameSprites sprites)
    {
        int w = 0, h = 0;
        for (int i = 0; i < positions(tab); i++)
        {
            GameSprites.Sprite s = sprites.get(pictureAt(tab, i));
            if (s != null)
            {
                w = Math.max(w, s.width);
                h = Math.max(h, s.height);
            }
        }
        return new int[] { w, h };
    }

    /** The part under a pixel of a picture as drawn (mirrored or not), or -1 where there is none to change. */
    public static int partAt(int tab, GameSprites.Sprite sprite, boolean mirrored, int x, int y)
    {
        if (sprite == null)
            return -1;
        int slot = sprite.slotAt(mirrored ? sprite.width - 1 - x : x, y);
        return slot < 0 ? -1 : partOfSlot(tab, slot);
    }

    /** The picture in row r's colours as 0xAARRGGBB pixels, a row at a time; transparent where the game draws nothing. */
    public int[] pixels(int tab, int r, GameSprites.Sprite sprite, boolean mirrored)
    {
        int[] set = new int[32];
        for (int s = 0; s < set.length; s++)
            set[s] = 0xFF000000 | liveries.getPaletteRgb(pictureColour(tab, r, s));
        int[] out = new int[sprite.width * sprite.height];
        for (int y = 0; y < sprite.height; y++)
            for (int x = 0; x < sprite.width; x++)
            {
                int slot = sprite.slots[y * sprite.width + x];
                if (slot >= 0)
                    out[y * sprite.width + (mirrored ? sprite.width - 1 - x : x)] = set[slot];
            }
        return out;
    }

    // --- The palette ------------------------------------------------------

    /** The colour chooser's instructions on the desktop. */
    public static final String CHOOSER_HINT = "<html>Click a colour, or use the arrow keys and Enter. Esc cancels.<br>"
        + "A filled corner mark: the game changes that colour in a race, so avoid it. An open mark: worth knowing.<br>"
        + "Point at a colour to see why. In the rain, colours 0 to 207 all turn greyer.</html>";

    /** What a race palette colour is used for, or null for an ordinary colour. From the F1GP-SDL
        project's palette code, which reproduces the game's dry and wet palettes exactly (2026-10-25):
        the game rebuilds the ground (16-31) for each circuit and textures 18 and 26, overwrites the sky
        (128-135, 232-239) and the distant scenery shades (141-151, 158, 160-167), and draws 27 as the
        crowd. A car painted in any of these looks wrong in the race. */
    public static String specialUse(int index)
    {
        if (index == 27)
            return "The crowd in the grandstands: a car painted in it shows crowd speckles in a race and grey in practice.";
        if (index >= 16 && index <= 23)
            return "Grass. The game rebuilds it for each circuit and for its textures, so a car painted in it changes from track to track.";
        if (index >= 24 && index <= 31)
            return "Tarmac. The game rebuilds it for each circuit and for its textures, so a car painted in it changes from track to track.";
        if ((index >= 128 && index <= 135) || (index >= 232 && index <= 239))
            return "Sky. The game replaces it with the sky's colours in a race.";
        if ((index >= 141 && index <= 151) || index == 158 || (index >= 160 && index <= 167))
            return "Scenery in the distance. The game works it out from other colours in a race, so it does not look like this.";
        return null;
    }

    /** A note about a colour that is fine to use but behaves differently, or null. */
    public static String note(int index)
    {
        if (index >= 192 && index <= 199)
            return "Stays bright in the distance in dry weather, where other colours fade.";
        return null;
    }

    /** What pointing at a colour says. */
    public static String about(int index)
    {
        String about = specialUse(index) != null ? specialUse(index) : note(index);
        return "Colour " + index + (about != null ? ": " + about : "");
    }
}
