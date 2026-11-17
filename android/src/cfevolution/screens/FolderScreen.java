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

import cfevolution.data.GameFolder;
import cfevolution.data.executable.Executable;
import cfevolution.edit.GameFilePage;
import cfevolution.edit.GameFolderList;
import cfevolution.edit.GameFolderOpening;
import cfevolution.edit.Message;
import cfevolution.edit.AllCircuitsCars;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The game folder screen (2026-11-17): what the desktop's game folder tab
 * lists for the first Android version (the game program and the 16
 * circuits), opening the folder with the desktop's questions, and opening
 * GP.EXE or a circuit. Plain Java: runs on Android and the desktop JVM.
 */
public final class FolderScreen
{
    public final GameFolder game;

    private FolderScreen(GameFolder game)
    {
        this.game = game;
    }

    /** Row.what for GP.EXE (circuits are 1-16; GameFolderList.OPTIONS, GameFolderList.SAVED + n for the others). */
    public static final int EXECUTABLE = GameFolderList.GAME;

    public String title()
    {
        return game.getFolder().getName();
    }

    /** Every row, worded as the desktop's game folder tab words them. */
    public List<GameFolderList.Row> rows()
    {
        return GameFolderList.rows(game);
    }

    /** The game options, a name file, a setup file, a championship save or the sound effects; null after setting problem[0]. */
    public GameFilePage openFile(File file, Message[] problem)
    {
        try
        {
            if (cfevolution.data.gamefiles.SoundEffectsFile.isSoundEffects(file))
                return new cfevolution.edit.SoundEffectsPage(cfevolution.data.gamefiles.SoundEffectsFile.load(file),
                                                             java.nio.file.Files.readAllBytes(file.toPath()));
            return GameFilePage.open(file, game);
        }
        catch (java.io.IOException e)
        {
            problem[0] = GameFilePage.notOpened(file);
            return null;
        }
    }

    /**
     * Save the Game's Names as a Name File, with the typed name: refused
     * (BAD_NAME), or asked Replace? when the file is there (Replace, the
     * first choice, writes it), then written. savedNames() is the file
     * once written.
     */
    public Flow saveNames(String typed)
    {
        savedNames = null;
        String name = GameFolderList.fileName(typed);
        if (name == null)
            return Flow.tell(GameFolderList.BAD_NAME);
        final File target = GameFolderList.nameFileTarget(game, name);
        if (!target.exists())
            return Flow.tell(writeNames(target));
        return new Flow() {
            {
                show(GameFolderList.replaceQuestion(target));
            }
            protected void next(Message asked, int choice)
            {
                if (asked.options != null && choice == 0)
                {
                    Message m = writeNames(target);
                    if (m != null)
                        show(m);
                }
            }
        };
    }

    /** A picture file (menu helmets, flags, backgrounds, circuit pictures); null after setting problem[0]. */
    public cfevolution.edit.PictureFilePage openPictures(File file, Message[] problem)
    {
        try
        {
            return new cfevolution.edit.PictureFilePage(cfevolution.data.gamefiles.MediaContainer.load(file),
                                                         cfevolution.data.gamefiles.MediaContainer.load(file), file, game);
        }
        catch (IOException e)
        {
            problem[0] = GameFilePage.notOpened(file);
            return null;
        }
    }

    /** Export Dataset; null after setting problem[0]. */
    public cfevolution.edit.DatasetExport exportDataset(Message[] problem)
    {
        return cfevolution.edit.DatasetExport.of(game, problem);
    }

    /** Import Dataset with a file to bring in; null after setting problem[0]. */
    public cfevolution.edit.DatasetImport importDataset(File file, Message[] problem)
    {
        return cfevolution.edit.DatasetImport.of(game, file, problem);
    }

    private File savedNames;

    public File savedNames()
    {
        return savedNames;
    }

    private Message writeNames(File target)
    {
        try
        {
            GameFolderList.writeNameFile(game, target);
            savedNames = target;
            return null;
        }
        catch (java.io.IOException e)
        {
            return GameFolderList.notSaved(target);
        }
    }

    /** Opens GP.EXE; null after setting problem[0] if it cannot be opened. */
    public ExecutableScreen openExecutable(Message[] problem)
    {
        Executable exe = new Executable(game.getExecutable());
        Message m = GameFolderOpening.executableProblem(exe.loadData());
        if (m != null)
        {
            problem[0] = m;
            return null;
        }
        return new ExecutableScreen(exe, game.getExecutable().getAbsoluteFile().getParentFile());
    }

    /** Game folder > Computer Cars at Every Circuit, read afresh from the circuit files. */
    public AllCircuitsCars allCircuits()
    {
        return new AllCircuitsCars(game);
    }

    /** Opens circuit slot 1-16; null after setting problem[0] if it will not load. */
    public TrackScreen openTrack(int slot, Message[] problem)
    {
        TrackScreen t = TrackScreen.open(game.getTrack(slot), problem);
        if (t != null)
            t.circuit = GameFolder.CIRCUIT_NAMES[slot - 1];
        return t;
    }

    // ------------------------------------------------------------------

    /**
     * Opening a folder, with the desktop's questions in the desktop's order
     * (GameFolderOpening): when the flow is done, opened() is the screen, or
     * null if the folder was not opened.
     */
    public static final class Opening extends Flow
    {
        private GameFolder game;
        private FolderScreen opened;
        private int step;

        private static final int UNPACK = 1, BACKUP = 2, FAILED = 3, FINAL = 4;

        Opening(File folder)
        {
            game = GameFolder.open(folder);
            Message notKept = GameFolderOpening.keepOriginals(game);
            if (notKept != null)
                show(notKept);       // then carry on
            else
                afterCopy();
        }

        /** The screen once the flow is done, or null if the folder was not opened. */
        public FolderScreen opened()
        {
            return done() ? opened : null;
        }

        protected void next(Message asked, int choice)
        {
            switch (step)
            {
                case 0:
                    afterCopy();
                    break;
                case UNPACK:
                    if (choice != GameFolderOpening.UNPACK)
                        return;                            // cancelled: not opened
                    try
                    {
                        if (GameFolderOpening.needsBackupChoice(game))
                        {
                            step = BACKUP;
                            show(GameFolderOpening.backupChoice(game));
                            return;
                        }
                    }
                    catch (IOException e)
                    {
                        step = FAILED;
                        show(GameFolderOpening.UNPACK_FAILED);
                        return;
                    }
                    unpack(false);
                    break;
                case BACKUP:
                    if (choice != GameFolderOpening.OVERWRITE && choice != GameFolderOpening.DONT_OVERWRITE)
                        return;                            // cancelled
                    unpack(choice == GameFolderOpening.OVERWRITE);
                    break;
                default:
                    return;                                // FAILED or FINAL: over, not opened
            }
        }

        private void afterCopy()
        {
            if (game.isPacked())
            {
                step = UNPACK;
                show(GameFolderOpening.unpackQuestion());
                return;
            }
            finish();
        }

        private void unpack(boolean replaceBackup)
        {
            GameFolder unpacked = GameFolderOpening.unpack(game, replaceBackup);
            if (unpacked == null)
            {
                step = FAILED;
                show(GameFolderOpening.UNPACK_FAILED);
                return;
            }
            game = unpacked;
            finish();
        }

        private void finish()
        {
            Message problem = GameFolderOpening.problem(game);
            if (problem != null)
            {
                step = FINAL;
                show(problem);
                return;
            }
            opened = new FolderScreen(game);
        }
    }

    /** Starts opening a folder: answer the flow's messages until it is done. */
    public static Opening open(File folder)
    {
        return new Opening(folder);
    }
}
