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
