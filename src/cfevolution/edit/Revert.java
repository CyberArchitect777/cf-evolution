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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Revert File and Revert Folder (Hrvoje, 2026-11-15/21; the owner chose all three
 * levels and "when opened", 2026-11-23): a file goes back to how it was the last
 * time it was opened, a game folder to how its files were when the folder was
 * opened, and either way saves made since are undone. The bytes are kept in
 * memory for as long as the editor runs (the largest file, GP.EXE, is about
 * 320 KB). Revert Page, for GP.EXE's pages, is {@link ExecutableRevert}.
 */
public final class Revert
{
    private static final Map<String, byte[]> files = new HashMap<String, byte[]>();
    private static final Map<String, Map<String, byte[]>> folders = new HashMap<String, Map<String, byte[]>>();

    private Revert()
    {
    }

    private static String key(File f)
    {
        return f.getAbsoluteFile().toPath().normalize().toString();
    }

    // --- Files ----------------------------------------------------------------

    /** Remembers a file as it is now: called whenever it is opened in a tab or screen. */
    public static synchronized void opened(File f)
    {
        if (f == null)
            return;
        cfevolution.data.executable.Executable.forgetWrittenMenuHelmets(f);
        try
        {
            files.put(key(f), Files.readAllBytes(f.toPath()));
        }
        catch (IOException e)
        {
            files.remove(key(f));
        }
    }

    /** True if the file was opened in this run of the editor, so it can be reverted. */
    public static synchronized boolean canRevert(File f)
    {
        return f != null && files.containsKey(key(f));
    }

    /** The file's bytes when it was last opened, or null. */
    public static synchronized byte[] whenOpened(File f)
    {
        byte[] b = f == null ? null : files.get(key(f));
        return b == null ? null : b.clone();
    }

    /** Asked before Revert File: the first option reverts. */
    public static Message fileQuestion(File f)
    {
        return Message.choose(Message.Kind.QUESTION, "Revert File",
            f.getName() + " goes back to how it was when it was opened. Every change made to it since then "
          + "is lost, including any saves.", "Revert", "Cancel");
    }

    /** Writes the file back as it was when opened. Null when done, else what to tell the user. */
    public static Message revertFile(File f)
    {
        byte[] b = whenOpened(f);
        if (b == null)
            return Message.warning("Not Reverted", f.getName() + " has not been opened since the editor started, "
                + "so there is nothing to go back to.");
        try
        {
            Files.write(f.toPath(), b);
            // GP.EXE: driver moves saved since took their menu helmets (HELMETS.DAT) with them; put those back too
            cfevolution.data.executable.Executable.undoWrittenMenuHelmets(f);
            return null;
        }
        catch (IOException e)
        {
            return notReverted(f.getName());
        }
    }

    private static Message notReverted(String what)
    {
        return Message.error("Not Reverted", what + " could not be written, so it was not reverted. "
            + "It may be read-only or in use by another program.");
    }

    // --- Folders ----------------------------------------------------------------

    /** Remembers every file the editor can change in a game folder: called when the folder is opened. */
    public static synchronized void folderOpened(GameFolder game)
    {
        Map<String, byte[]> kept = new LinkedHashMap<String, byte[]>();
        for (File f : game.changeableFiles())
        {
            try
            {
                kept.put(key(f), Files.readAllBytes(f.toPath()));
            }
            catch (IOException e)
            {
                // a file that cannot be read now cannot be put back later
            }
        }
        folders.put(key(game.getFolder()), kept);
    }

    /** True if the folder was opened in this run of the editor. */
    public static synchronized boolean canRevert(GameFolder game)
    {
        return folders.containsKey(key(game.getFolder()));
    }

    /** The folder's files that differ now from when it was opened (or are gone). */
    public static synchronized List<File> folderChanges(GameFolder game)
    {
        List<File> changed = new ArrayList<File>();
        Map<String, byte[]> kept = folders.get(key(game.getFolder()));
        if (kept == null)
            return changed;
        for (Map.Entry<String, byte[]> e : kept.entrySet())
        {
            File f = new File(e.getKey());
            try
            {
                if (!f.isFile() || !Arrays.equals(Files.readAllBytes(f.toPath()), e.getValue()))
                    changed.add(f);
            }
            catch (IOException ex)
            {
                changed.add(f);
            }
        }
        return changed;
    }

    /** A file's name as the folder shows it: GPSAVES files with their folders. */
    public static String nameIn(GameFolder game, File f)
    {
        String base = key(game.getFolder()) + File.separator;
        String k = key(f);
        return k.startsWith(base) ? k.substring(base.length()).replace(File.separatorChar, '/') : f.getName();
    }

    public static final Message NOTHING_TO_REVERT = Message.info("Revert Folder",
        "No file in this folder has changed since it was opened.");

    /** Asked before Revert Folder, naming the files: the first option reverts. */
    public static Message folderQuestion(GameFolder game, List<File> changed)
    {
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < changed.size(); i++)
            names.append(i == 0 ? "" : ", ").append(nameIn(game, changed.get(i)));
        return Message.choose(Message.Kind.QUESTION, "Revert Folder",
            "These files go back to how they were when the folder was opened: " + names + ".\n"
          + "Every change made to them since then is lost, including any saves. Files from this folder that are "
          + "open in the editor are closed first, without saving. Files made since then are kept.", "Revert", "Cancel");
    }

    /** Writes back every changed file as it was when the folder was opened. Null when done. */
    public static Message revertFolder(GameFolder game)
    {
        Map<String, byte[]> kept;
        synchronized (Revert.class)
        {
            kept = folders.get(key(game.getFolder()));
        }
        if (kept == null)
            return NOTHING_TO_REVERT;
        List<String> failed = new ArrayList<String>();
        for (File f : game.changeableFiles())
            cfevolution.data.executable.Executable.forgetWrittenMenuHelmets(f);   // HELMETS.DAT is put back with the rest
        for (File f : folderChanges(game))
        {
            try
            {
                Files.write(f.toPath(), kept.get(key(f)));
            }
            catch (IOException e)
            {
                failed.add(nameIn(game, f));
            }
        }
        return failed.isEmpty() ? null : notReverted(String.join(", ", failed));
    }

    // --- Pages ----------------------------------------------------------------

    /** Asked before Revert Page: the first option reverts. */
    public static Message pageQuestion(String page)
    {
        return Message.choose(Message.Kind.QUESTION, "Revert Page",
            page + " goes back to how it was when GP.EXE was opened. Its changes since then are lost, "
          + "including any saved; the other pages are not changed.", "Revert", "Cancel");
    }
}
