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
import cfevolution.data.gamefiles.ChampionshipSave;
import cfevolution.data.gamefiles.GameFile;
import cfevolution.data.gamefiles.MediaContainer;
import cfevolution.data.gamefiles.NameFile;
import cfevolution.data.gamefiles.PreferencesFile;
import cfevolution.data.gamefiles.SetupFile;
import cfevolution.data.gamefiles.SoundEffectsFile;
import java.io.File;
import java.io.IOException;

/**
 * What the pages for the game's smaller files share (2026-11-17, from the
 * desktop's GameFileTab and MainGUI.openGameFile, word for word): the title
 * and line of explanation, the status line, the refusals for a number or a
 * name that does not fit, and saving with the checksums resealed. The pages
 * are NameFilePage, SetupFilePage, ChampionshipPage and PreferencesPage.
 */
public abstract class GameFilePage
{
    public static final String NOT_CHANGED = ExecutableEdits.NOT_CHANGED;

    public final GameFile gameFile;
    /** Set by each change that changed something (the tab marks itself changed). */
    protected boolean changed;

    protected GameFilePage(GameFile gameFile)
    {
        this.gameFile = gameFile;
    }

    public abstract String title();

    public abstract String explanation();

    public abstract String status();

    /** Whether the last change changed the file (false after a refusal, or the same value again). */
    public boolean changed()
    {
        return changed;
    }

    /** A typed whole number inside [min, max], or null; problem[0] says why not. */
    public static Integer number(Object value, int min, int max, String what, Message[] problem)
    {
        try
        {
            int n = Integer.parseInt(String.valueOf(value).trim());
            if (n >= min && n <= max)
                return n;
        }
        catch (NumberFormatException e)
        {
            // falls through to the message
        }
        problem[0] = Message.warning(NOT_CHANGED, what + " must be a whole number from " + min + " to " + max + ".");
        return null;
    }

    /** Typed text no longer than maxLength, of plain characters, or null; problem[0] says why not. */
    public static String text(Object value, int maxLength, String what, Message[] problem)
    {
        String text = String.valueOf(value);
        boolean plain = true;
        for (char c : text.toCharArray())
            plain &= c >= 32 && c < 256;
        if (text.length() > maxLength || !plain)
        {
            problem[0] = Message.warning(NOT_CHANGED, what + " can be at most " + maxLength + " plain characters long.");
            return null;
        }
        return text;
    }

    /** Saves with the checksums resealed; the message when it could not be, else null. */
    public Message save(File file)
    {
        try
        {
            gameFile.save(file);
            return null;
        }
        catch (IOException e)
        {
            return Message.error("Save Failed", file.getName() + " could not be saved.");
        }
    }

    public static Message notOpened(File file)
    {
        return Message.error("File Not Opened", file.getName() + " could not be opened. It may be damaged.");
    }

    /** True for the picture and sound files, which have their own pages (not these). */
    public static boolean isPictureOrSound(File file)
    {
        return MediaContainer.isContainer(file) || SoundEffectsFile.isSoundEffects(file);
    }

    /**
     * Opens the game options, a name file, a championship save or a setup
     * file, told apart as the desktop does (by size; anything else is taken
     * as a setup file). Call isPictureOrSound first. A championship save's
     * points come from the folder's GP.EXE.
     */
    public static GameFilePage open(File file, GameFolder game) throws IOException
    {
        long size = file.length();
        if (size == PreferencesFile.SIZE)
            return new PreferencesPage(PreferencesFile.load(file), file);
        if (size == NameFile.SIZE)
            return new NameFilePage(NameFile.load(file));
        if (size == ChampionshipSave.SIZE)
        {
            int[] points = null;
            int best = 0;
            Executable exe = new Executable(game.getExecutable());
            if (exe.loadData() == 0)
            {
                points = new int[26];
                for (int i = 0; i < points.length; i++)
                    points[i] = exe.getGamePoints().getPointValue(i);
                best = exe.getGameSettings().getBestResultsCounted();
            }
            return new ChampionshipPage(ChampionshipSave.load(file), points, best);
        }
        return new SetupFilePage(SetupFile.load(file));
    }
}
