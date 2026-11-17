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
import cfevolution.data.executable.Executable;
import cfevolution.data.gamefiles.GameFile;
import cfevolution.data.gamefiles.NameFile;
import cfevolution.data.gamefiles.SetupFile;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * What the game folder's tab lists, and its name file buttons (2026-11-17,
 * from the desktop's GameWindow and MainGUI, word for word): the game, its
 * options, the 16 circuits, the picture files, the sound effects, the
 * datasets and the saved files, each with what it is; saving GP.EXE's names
 * as a name file; and checking a name file chosen from anywhere.
 */
public final class GameFolderList
{
    /** What a row opens. */
    public static final int NONE = -1, GAME = 0, OPTIONS = 100, PICTURES = 200, SOUNDS = 300, SAVED = 400;

    /** One line: a heading (only the label set), or something that can be opened. */
    public static final class Row
    {
        public final boolean heading;
        public final String number, label, detail;
        /** The file it opens, or null when it is not in the folder. */
        public final File file;
        /** The desktop's name for the row's Open button. */
        public final String key;
        /** GAME, OPTIONS, a circuit slot 1-16, PICTURES + n, SOUNDS, SAVED + n, or NONE. */
        public final int what;

        Row(boolean heading, String number, String label, String detail, File file, String key, int what)
        {
            this.heading = heading;
            this.number = number;
            this.label = label;
            this.detail = detail;
            this.file = file;
            this.key = key;
            this.what = what;
        }
    }

    public static final String TITLE = "Game folder";
    public static final String GAME_HEADING = "Game", CIRCUITS = "Circuits", PICTURES_HEADING = "Pictures", SOUNDS_HEADING = "Sounds",
                               DATASETS = "Datasets", SAVED_HEADING = "Saved files (GPSAVES)";
    public static final String NO_SAVED_FILES = "No name files, setups or championship saves yet.";
    static final String[] KIND_NAMES = { "Name file", "Setup", "Championship save" };
    static final String[] PICTURE_NAMES = { "Menu helmets", "Language flags", "Menu backgrounds", "Circuit pictures" };
    static final String[] PICTURE_DETAILS = { "the helmet beside each driver on the driver menus", "the flags on the language screen",
                                              "the full-screen pictures behind the menus", "each circuit's picture on the circuit menu" };

    public static final String SAVE_NAMES = "Save the Game's Names as a Name File...";
    public static final String SAVE_NAMES_TIP = "Writes the driver, team and engine names in GP.EXE to a name file the game can load";
    public static final String OPEN_NAMES = "Open a Name File...";
    public static final String OPEN_NAMES_TIP = "Opens a name file (.NAM) from any folder to view or change its names";
    public static final String NAME_QUESTION = "Name for the file, up to 8 letters or digits (the game shows DOS names):";
    public static final String NAME_SUGGESTED = "NAMES";
    public static final Message BAD_NAME = Message.warning("Name File Not Saved", "Use 1 to 8 letters, digits, - or _.");

    private GameFolderList()
    {
    }

    /** Under the title: where the game's first files are kept, or null when there is no copy yet. */
    public static String originalsNote(GameFolder game)
    {
        if (!game.getOriginalsFolder().isDirectory())
            return null;
        return "Your game files as they were when the editor first opened this folder are kept in "
            + GameFolder.BACKUP_FOLDER + File.separator + GameFolder.ORIGINALS_FOLDER + ".";
    }

    public static String status(GameFolder game)
    {
        int found = 0;
        for (int slot = 1; slot <= GameFolder.CIRCUITS; slot++)
            if (game.getTrack(slot) != null)
                found++;
        return "Game folder: the game settings and " + found + " of the 16 circuits. Each opens in its own tab.";
    }

    private static Row heading(String label)
    {
        return new Row(true, "", label, "", null, null, NONE);
    }

    /** Every row, in the desktop's order. The Computer Cars at Every Circuit button goes under the
        Circuits heading, the dataset buttons under Datasets and the name file buttons under the
        saved files heading. */
    public static List<Row> rows(GameFolder game)
    {
        List<Row> rows = new ArrayList<Row>();
        rows.add(heading(GAME_HEADING));
        rows.add(new Row(false, "", "Game settings",
            "GP.EXE, version 1.05 " + game.getBuildName() + ": drivers, teams, points, liveries and rules",
            game.getExecutable(), "open-game", GAME));
        File prefs = game.getPreferences();
        rows.add(new Row(false, "", "Game options",
            prefs != null ? "F1PREFS.DAT: opposition, race distance, driving aids, files loaded at start-up"
                          : "F1PREFS.DAT is not here yet: the game writes it with Save Options",
            prefs, "open-options", OPTIONS));

        rows.add(heading(CIRCUITS));
        for (int slot = 1; slot <= GameFolder.CIRCUITS; slot++)
        {
            File track = game.getTrack(slot);
            rows.add(new Row(false, String.valueOf(slot), GameFolder.CIRCUIT_NAMES[slot - 1],
                track != null ? track.getName() : GameFolder.trackFileName(slot) + " is not in this folder",
                track, "open-" + slot, slot));
        }

        rows.add(heading(PICTURES_HEADING));
        for (int i = 0; i < GameFolder.PICTURE_FILES.length; i++)
        {
            File f = game.getPictureFile(GameFolder.PICTURE_FILES[i]);
            rows.add(new Row(false, "", PICTURE_NAMES[i],
                f != null ? GameFolder.PICTURE_FILES[i] + ": " + PICTURE_DETAILS[i] : GameFolder.PICTURE_FILES[i] + " is not in this folder",
                f, "open-" + GameFolder.PICTURE_FILES[i], PICTURES + i));
        }

        rows.add(heading(SOUNDS_HEADING));
        File sounds = game.getSoundEffects();
        rows.add(new Row(false, "", "Sound effects",
            sounds != null ? "XSOUND.BIN: the engine, tyres, passing cars, crashes and pit stops (AdLib and Sound Blaster)"
                           : game.hasSoundEffects() ? "The game is set up for another sound card: only the AdLib and Sound Blaster effects can be edited"
                                                    : "XSOUND.BIN is not here: the game's installer puts it there",
            sounds, "open-sounds", SOUNDS));

        rows.add(heading(DATASETS));
        rows.add(heading(SAVED_HEADING));
        List<GameFolder.SavedFile> saved = game.getSavedFiles();
        for (int i = 0; i < saved.size(); i++)
        {
            GameFolder.SavedFile f = saved.get(i);
            String kind = KIND_NAMES[f.kind.ordinal()];
            if (f.kind == GameFolder.SavedKind.SETUP && f.file.length() == SetupFile.SEASON_SIZE)
                kind = "Season setups";
            rows.add(new Row(false, "", kind, f.name, f.file, "open-saved-" + f.name, SAVED + i));
        }
        return rows;
    }

    // --- name files ------------------------------------------------------------

    /** A typed name made into the file's name ("mynames" -> "MYNAMES"), or null when it cannot be one (BAD_NAME). */
    public static String fileName(String typed)
    {
        String name = typed.trim().toUpperCase();
        if (name.endsWith(".NAM"))
            name = name.substring(0, name.length() - 4);
        return name.matches("[A-Z0-9_-]{1,8}") ? name : null;
    }

    /** Where a name file of that name goes: GPSAVES (made if need be). */
    public static File nameFileTarget(GameFolder game, String name)
    {
        File saves = game.getSavesFolder();
        if (saves == null)
            saves = new File(game.getFolder(), "GPSAVES");
        return new File(saves, name + ".NAM");
    }

    /** Asked when the name file is there already: Replace (0) or Cancel (1). */
    public static Message replaceQuestion(File target)
    {
        return Message.choose(Message.Kind.WARNING, "Replace File", target.getName() + " already exists. Replace it?", "Replace", "Cancel");
    }

    public static Message notSaved(File target)
    {
        return Message.error("Name File Not Saved", target.getName() + " could not be saved.");
    }

    /** Writes GP.EXE's driver, team and engine names as a name file. */
    public static void writeNameFile(GameFolder game, File target) throws IOException
    {
        byte[] exe = java.nio.file.Files.readAllBytes(game.getExecutable().toPath());
        int build = Executable.identify(exe);
        if (build < 1 || build > 3)
            throw new IOException("GP.EXE could not be read");
        int at = new int[] { 180250, 180186, 180202 }[build - 1];   // the driver names, then teams and engines
        target.getParentFile().mkdirs();
        NameFile.fromGame(java.util.Arrays.copyOfRange(exe, at, at + 1480)).save(target);
    }

    /** A name file chosen from anywhere: why it will not open, or null. */
    public static Message nameFileProblem(String fileName, byte[] bytes)
    {
        if (bytes != null && bytes.length == NameFile.SIZE && GameFile.checksumOk(bytes))
            return null;
        return Message.warning("Name File Not Opened", fileName + " is not a name file the game can read, or it is damaged.");
    }
}
