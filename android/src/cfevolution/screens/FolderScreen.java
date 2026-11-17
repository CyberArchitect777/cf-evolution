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
import cfevolution.edit.GameFolderOpening;
import cfevolution.edit.Message;
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

    /** One line of the list: a heading, or something that can be opened. */
    public static final class Row
    {
        /** For a heading only the label is set. */
        public final boolean heading;
        public final String number, label, detail;
        /** The file to open, or null when it is not in the folder. */
        public final File file;
        /** EXECUTABLE, or the circuit slot 1-16. */
        public final int what;

        Row(boolean heading, String number, String label, String detail, File file, int what)
        {
            this.heading = heading;
            this.number = number;
            this.label = label;
            this.detail = detail;
            this.file = file;
            this.what = what;
        }
    }

    public static final int EXECUTABLE = 0;

    public String title()
    {
        return game.getFolder().getName();
    }

    /** The rows, worded as the desktop's game folder tab words them. */
    public List<Row> rows()
    {
        List<Row> rows = new ArrayList<Row>();
        rows.add(new Row(true, "", "Game", "", null, -1));
        rows.add(new Row(false, "", "Game settings",
            "GP.EXE, version 1.05 " + game.getBuildName() + ": drivers, teams, points, liveries and rules",
            game.getExecutable(), EXECUTABLE));
        rows.add(new Row(true, "", "Circuits", "", null, -1));
        for (int slot = 1; slot <= GameFolder.CIRCUITS; slot++)
        {
            File track = game.getTrack(slot);
            rows.add(new Row(false, String.valueOf(slot), GameFolder.CIRCUIT_NAMES[slot - 1],
                track != null ? track.getName() : GameFolder.trackFileName(slot) + " is not in this folder",
                track, slot));
        }
        return rows;
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
        return new ExecutableScreen(exe);
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
