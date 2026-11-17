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
import java.io.File;
import java.io.IOException;

/**
 * Opening a game folder, without the interface (2026-11-16, moved out of
 * MainGUI.openGameFolder word for word). The steps, in order:
 *
 *   GameFolder game = GameFolder.open(folder);
 *   show(keepOriginals(game));                    // a warning, or null
 *   if (game.isPacked()) {
 *       ask UNPACK (Unpack / Cancel); stop on Cancel
 *       boolean replace = false;
 *       if (needsBackupChoice(game)) {
 *           ask backupChoice(game) (Overwrite / Don't Overwrite / Cancel); stop on Cancel
 *           replace = answer == Overwrite;
 *       }
 *       game = unpack(game, replace); if null, show UNPACK_FAILED and stop
 *   }
 *   if (problem(game) != null) show it and stop;
 *   the folder is open.
 *
 * The texts carry no line breaks: the desktop wraps them at 70 characters.
 */
public final class GameFolderOpening
{
    private GameFolderOpening()
    {
    }

    /** Before anything can change, the first time, a copy of the game files
        is kept (a packed GP.EXE is kept packed in the copy). Returns the
        warning to show when the copy could not be kept, else null. */
    public static Message keepOriginals(GameFolder game)
    {
        if (game.getProblem() == null || game.isPacked())
        {
            try
            {
                game.keepOriginals();
            }
            catch (IOException e)
            {
                return Message.warning("Copy Not Kept", "A copy of the game files could not be kept in "
                    + GameFolder.BACKUP_FOLDER + File.separator + GameFolder.ORIGINALS_FOLDER
                    + ". The folder is opened anyway. Nothing in it has been changed.");
            }
        }
        return null;
    }

    public static final int UNPACK = 0, CANCEL_UNPACK = 1;

    /** Asked when GP.EXE is packed: Unpack (0) or Cancel (1). */
    public static Message unpackQuestion()
    {
        return Message.choose(Message.Kind.QUESTION, "Unpack GP.EXE",
            "GP.EXE in this folder is the packed version, which the editor cannot change.\n\n"
            + "Unpack it now? The original is kept as " + GameFolder.BACKUP_NAME + ". The game runs the unpacked GP.EXE "
            + "in the same way.", "Unpack", "Cancel");
    }

    /** True when a different GPPACKED.EXE is already there, so the user must choose. */
    public static boolean needsBackupChoice(GameFolder game) throws IOException
    {
        return game.getBackup() != null && !game.backupMatches();
    }

    public static final int OVERWRITE = 0, DONT_OVERWRITE = 1, CANCEL_BACKUP = 2;

    /** Overwrite (0), Don't Overwrite (1) or Cancel (2). */
    public static Message backupChoice(GameFolder game)
    {
        return Message.choose(Message.Kind.WARNING, "Backup Already Exists",
            game.getBackup().getName() + " already exists in this folder. Overwrite it with the GP.EXE "
            + "that is there now?\n\nIf you do not, " + game.getBackup().getName() + " is kept as it is "
            + "and the current GP.EXE is not kept.", "Overwrite", "Don't Overwrite", "Cancel");
    }

    public static final Message UNPACK_FAILED = Message.error("Unpack GP.EXE",
        "GP.EXE could not be unpacked, so nothing was changed.");

    /** Unpacks GP.EXE (keeping the original) and looks at the folder again;
        null if it failed (show UNPACK_FAILED). */
    public static GameFolder unpack(GameFolder game, boolean replaceBackup)
    {
        try
        {
            game.unpackExecutable(replaceBackup);
        }
        catch (IOException e)
        {
            return null;
        }
        return GameFolder.open(game.getFolder());
    }

    /** Why a GP.EXE could not be opened, for Executable.loadData's result (null for 0, loaded). */
    public static Message executableProblem(int loadResult)
    {
        switch (loadResult)
        {
            case 0: return null;
            case 1: return Message.error("Incompatible Executable Detected", "The file specified is not a known F1GP/WC executable file. \nThis may be because you have selected the wrong file, or \nbecause this executable has not been updated to version \n1.05. In the latter case, upgrading your copy of F1GP/WC to \n1.05 will resolve the problem.");
            case 3: return Message.error("Packed Executable Detected", "The executable file specified is a 1.05 packed version \nof F1GP/WC. An unpacked version of this file is required for \nediting in CF Evolution. Please see the documentation \nfor more details");
            default: return Message.error("Unknown Error", "An unknown error has occurred during the loading of this \nexecutable file. It is likely that this is either not a \nF1GP/WC executable file, or that the executable itself is \ncorrupt.Please try a valid executable file");
        }
    }

    /** When a track file will not load. */
    public static final Message TRACK_NOT_LOADED = Message.error("Error", "There was a problem loading the track file specified. \nIt is likely that this is either not a track file, or that the track itself is corrupt.\nPlease try another valid track file");

    /** Why the folder cannot be opened, or null. */
    public static Message problem(GameFolder game)
    {
        return game.getProblem() == null ? null : Message.error("Game Folder Not Opened", game.getProblem());
    }
}
