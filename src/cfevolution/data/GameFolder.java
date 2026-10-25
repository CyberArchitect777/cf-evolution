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

package cfevolution.data;

import cfevolution.data.executable.ExePackUnpacker;
import cfevolution.data.executable.Executable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * A game folder: the GP.EXE the game runs and the 16 circuit files it races,
 * F1CT01.DAT to F1CT16.DAT. Nothing else in the folder is opened.
 *
 * The editor can only change an unpacked GP.EXE, so a folder whose GP.EXE is
 * packed, unknown or missing is refused, with the reason in plain language.
 * A packed GP.EXE can be unpacked in place ({@link #unpackExecutable}), the
 * original kept as GPPACKED.EXE. Unpacked copies under other names are never
 * used, because the game only runs GP.EXE.
 *
 * @author barrie
 */
public class GameFolder
{
    public static final int CIRCUITS = 16;

    /** The country of each slot in the original game, slot 1 first. */
    public static final String[] CIRCUIT_NAMES = {
        "United States", "Brazil", "San Marino", "Monaco", "Canada", "Mexico", "France", "Great Britain",
        "Germany", "Hungary", "Belgium", "Italy", "Portugal", "Spain", "Japan", "Australia" };

    private final File folder;
    private File executable;
    private int build;
    private final File[] tracks = new File[CIRCUITS];
    private String problem;
    private boolean packed;

    /** Where a packed GP.EXE is kept when it is unpacked. */
    public static final String BACKUP_NAME = "GPPACKED.EXE";

    private GameFolder(File folder)
    {
        this.folder = folder;
    }

    /** Looks at a folder. Check {@link #getProblem()} before using it. */
    public static GameFolder open(File folder)
    {
        GameFolder game = new GameFolder(folder.getAbsoluteFile());
        game.examine();
        return game;
    }

    public static String trackFileName(int slot)
    {
        return String.format("F1CT%02d.DAT", slot);
    }

    private void examine()
    {
        if (!folder.isDirectory())
        {
            problem = "The folder " + folder.getPath() + " could not be found.";
            return;
        }
        int found = 0;
        for (int slot = 1; slot <= CIRCUITS; slot++)
        {
            tracks[slot - 1] = fileIgnoringCase(folder, trackFileName(slot));
            if (tracks[slot - 1] != null)
                found++;
        }
        executable = fileIgnoringCase(folder, "GP.EXE");
        if (executable == null)
        {
            problem = found == 0
                ? "This folder does not hold the game. Choose the folder that has GP.EXE and the circuit files "
                  + "F1CT01.DAT to F1CT16.DAT in it."
                : "GP.EXE was not found in this folder. Choose the folder the game is installed in.";
            return;
        }
        build = Executable.identify(executable);
        if (build == 7)
            packed = unpacksToGame();   // packed, but not one of the three sizes known
        if ((build >= 4 && build <= 6) || packed)
        {
            packed = true;
            problem = "GP.EXE in this folder is the packed version, which the editor cannot change. "
                    + "Open Game Folder offers to unpack it, keeping the original as " + BACKUP_NAME + ".";
        }
        else if (build < 1 || build > 3)
            problem = "GP.EXE in this folder is not a version the editor can open. It needs version 1.05 "
                    + "of the game (European, Italian or US), unpacked.";
    }

    private boolean unpacksToGame()
    {
        try
        {
            return Executable.identify(ExePackUnpacker.unpack(Files.readAllBytes(executable.toPath()))) != 7;
        }
        catch (IOException e)
        {
            return false;
        }
    }

    /** The kinds of saved file the editor recognises in GPSAVES. */
    public enum SavedKind { NAMES, SETUP, CHAMPIONSHIP }

    /** A file in GPSAVES (or a folder inside it) the editor can open. */
    public static class SavedFile
    {
        public final File file;
        public final SavedKind kind;
        /** The path below GPSAVES, e.g. "ZONSET/MONZFAST.SET". */
        public final String name;

        SavedFile(File file, SavedKind kind, String name)
        {
            this.file = file;
            this.kind = kind;
            this.name = name;
        }
    }

    /** F1PREFS.DAT, or null if the game has not saved its options yet. */
    public File getPreferences()
    {
        File f = fileIgnoringCase(folder, "F1PREFS.DAT");
        return f != null && f.length() == cfevolution.data.gamefiles.PreferencesFile.SIZE ? f : null;
    }

    /** The 40 driver names in GP.EXE (by car number), or null if it cannot be read. */
    public String[] getDriverNames()
    {
        try
        {
            byte[] exe = Files.readAllBytes(executable.toPath());
            int b = Executable.identify(exe);
            if (b < 1 || b > 3)
                return null;
            int at = new int[] { 180250, 180186, 180202 }[b - 1];   // EU, IT, US
            String[] names = new String[40];
            for (int i = 0; i < 40; i++)
            {
                int start = at + i * 24, end = start;
                while (end < start + 24 && exe[end] != 0)
                    end++;
                names[i] = new String(exe, start, end - start, java.nio.charset.StandardCharsets.ISO_8859_1);
            }
            return names;
        }
        catch (IOException e)
        {
            return null;
        }
    }

    /** The picture files the editor offers, in the order it lists them. */
    public static final String[] PICTURE_FILES = { "HELMETS.DAT", "FLAGS.DAT", "BACKDROP.DAT", "TRACKPIX.DAT" };

    /** One of the picture files, or null if the folder lacks it. */
    public File getPictureFile(String name)
    {
        File f = fileIgnoringCase(folder, name);
        return f != null && cfevolution.data.gamefiles.MediaContainer.isContainer(f) ? f : null;
    }

    /** The sound effects the game uses (XSOUND.BIN, installed from ASOUND.BIN) when they are the
        AdLib and Sound Blaster ones, which are the only ones the editor understands; else null. */
    public File getSoundEffects()
    {
        File f = fileIgnoringCase(folder, "XSOUND.BIN");
        return f != null && cfevolution.data.gamefiles.SoundEffectsFile.isSoundEffects(f) ? f : null;
    }

    /** True if the game has sound effects installed, of whatever card. */
    public boolean hasSoundEffects()
    {
        return fileIgnoringCase(folder, "XSOUND.BIN") != null;
    }

    /** The GPSAVES folder, which the game creates on first use, or null. */
    public File getSavesFolder()
    {
        String[] names = folder.list();
        if (names != null)
            for (String n : names)
                if (n.equalsIgnoreCase("GPSAVES") && new File(folder, n).isDirectory())
                    return new File(folder, n);
        return null;
    }

    /**
     * Name files, set-up files and championship saves in GPSAVES and the
     * folders directly inside it, recognised by size and checksum since the
     * game saves them under any name. Saves made during a race weekend are
     * left out.
     */
    public List<SavedFile> getSavedFiles()
    {
        List<SavedFile> out = new ArrayList<SavedFile>();
        File saves = getSavesFolder();
        if (saves != null)
            collectSaved(saves, "", 0, out);
        return out;
    }

    private static void collectSaved(File dir, String prefix, int depth, List<SavedFile> out)
    {
        File[] files = dir.listFiles();
        if (files == null)
            return;
        java.util.Arrays.sort(files);
        for (File f : files)
            if (f.isFile())
            {
                SavedKind kind = savedKind(f);
                if (kind != null)
                    out.add(new SavedFile(f, kind, prefix + f.getName()));
            }
        if (depth == 0)
            for (File f : files)
                if (f.isDirectory())
                    collectSaved(f, prefix + f.getName() + "/", depth + 1, out);
    }

    private static SavedKind savedKind(File f)
    {
        long size = f.length();
        if (size == cfevolution.data.gamefiles.NameFile.SIZE || size == cfevolution.data.gamefiles.ChampionshipSave.SIZE
            || size == cfevolution.data.gamefiles.SetupFile.SINGLE_SIZE || size == cfevolution.data.gamefiles.SetupFile.SEASON_SIZE)
        {
            try
            {
                if (!cfevolution.data.gamefiles.GameFile.checksumOk(Files.readAllBytes(f.toPath())))
                    return null;
            }
            catch (IOException e)
            {
                return null;
            }
            if (size == cfevolution.data.gamefiles.NameFile.SIZE)
                return SavedKind.NAMES;
            if (size == cfevolution.data.gamefiles.ChampionshipSave.SIZE)
                return SavedKind.CHAMPIONSHIP;
            return SavedKind.SETUP;
        }
        return null;
    }

    /** True if GP.EXE is packed and can be unpacked. */
    public boolean isPacked()
    {
        return packed;
    }

    /** The existing GPPACKED.EXE, or null. */
    public File getBackup()
    {
        return fileIgnoringCase(folder, BACKUP_NAME);
    }

    /** True if GPPACKED.EXE already holds exactly this GP.EXE. */
    public boolean backupMatches() throws IOException
    {
        File backup = getBackup();
        return backup != null && java.util.Arrays.equals(Files.readAllBytes(backup.toPath()),
                                                         Files.readAllBytes(executable.toPath()));
    }

    /**
     * Unpacks GP.EXE in place. The packed file is copied to GPPACKED.EXE
     * first, unless one exists and {@code replaceBackup} is false. Nothing is
     * changed if the unpacked file is not the game. Look at the folder again
     * afterwards with {@link #open}.
     */
    public void unpackExecutable(boolean replaceBackup) throws IOException
    {
        byte[] unpacked = ExePackUnpacker.unpack(Files.readAllBytes(executable.toPath()));
        if (Executable.identify(unpacked) == 7)
            throw new IOException("the unpacked file is not a version of the game the editor knows");
        File temporary = new File(folder, "GPUNPACK.TMP");
        try
        {
            Files.write(temporary.toPath(), unpacked);
            File backup = getBackup();
            if (backup == null || replaceBackup)
                Files.copy(executable.toPath(), (backup != null ? backup : new File(folder, BACKUP_NAME)).toPath(),
                           StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            Files.move(temporary.toPath(), executable.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        finally
        {
            temporary.delete();
        }
    }

    private static File fileIgnoringCase(File folder, String name)
    {
        String[] names = folder.list();
        if (names == null)
            return null;
        for (String n : names)
            if (n.equalsIgnoreCase(name) && new File(folder, n).isFile())
                return new File(folder, n);
        return null;
    }

    /** Why the folder cannot be opened, or null if it can. */
    public String getProblem()
    {
        return problem;
    }

    public File getFolder()
    {
        return folder;
    }

    public File getExecutable()
    {
        return executable;
    }

    /** "European", "Italian" or "US". */
    public String getBuildName()
    {
        switch (build)
        {
            case 1: return "European";
            case 2: return "Italian";
            case 3: return "US";
            default: return "";
        }
    }

    /** The circuit file for slot 1-16, or null if the folder lacks it. */
    public File getTrack(int slot)
    {
        return tracks[slot - 1];
    }
}
