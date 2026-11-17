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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Copy-through (2026-11-17, owner's choice): the game folder is one the user
 * chose in the platform's folder picker (on Android, reached by content links,
 * not file paths). The core works with files, so the files it reads are copied
 * into a work folder, the core works there unchanged, and push() writes back
 * every file that is new or changed. Plain Java: the folder is reached through
 * a Store, so this runs and is checked on the desktop (ScreenGate).
 *
 * Copied in: GP.EXE, GPPACKED.EXE, the 16 circuits and the game's car, helmet
 * and pit crew pictures (F1GPDATA.DAT, F1GPDATB.DAT: read for the Liveries
 * page, never changed, so never written back); and the game folder's own
 * files, which are edited too (F1PREFS.DAT, the picture files, XSOUND.BIN,
 * GPSAVES; until 2026-11-18 only before the first-open copy existed, so a
 * second open found them missing). The first-open copy (CFE-BACKUP/original)
 * is not copied in: an empty folder stands for it, so the core leaves it
 * alone and nothing in it is ever written back.
 *
 * Writing back: new files before changed ones, so a backup (GPPACKED.EXE, the
 * first-open copy) is in the folder before the file it keeps is replaced. A
 * new folder is written under a temporary name and renamed when complete, as
 * the core does with the first-open copy, so a failure part-way leaves nothing
 * that would stop the next open from making it again. Files are never deleted.
 */
public final class FolderMirror
{
    /** The user's own folder. Paths are relative to it and '/'-separated; "" is the folder itself. */
    public interface Store
    {
        /** The folder's own name. */
        String name() throws IOException;

        /** What is in a folder, folders ending in '/'; null if there is no such folder. */
        List<String> list(String path) throws IOException;

        byte[] read(String path) throws IOException;

        /** Writes a file whole, replacing it or making it (and any folders it is in). */
        void write(String path, byte[] data) throws IOException;

        /** Renames a file or folder in the same folder. */
        void rename(String path, String newName) throws IOException;
    }

    /** Suffix of a folder being written; ignored when copying in. */
    static final String WRITING = ".cfe-writing";

    private final Store store;
    private final File work, local;
    /** Each file's digest as it is in the user's folder (read or written by us). */
    private final Map<String, String> known = new HashMap<String, String>();
    /** Folders that exist in the user's folder. */
    private final Set<String> folders = new HashSet<String>();

    private FolderMirror(Store store, File work, String name)
    {
        this.store = store;
        this.work = work;
        this.local = new File(work, name);
    }

    /**
     * Copies the game folder's files into a fresh work folder (anything
     * already there is removed first: one game folder at a time).
     */
    public static FolderMirror copyIn(Store store, File work) throws IOException
    {
        String name = store.name();
        if (name == null || name.length() == 0 || name.equals(".") || name.equals("..") || name.contains("/"))
            name = "Game";
        FolderMirror m = new FolderMirror(store, work, name);
        deleteTree(work);
        if (!m.local.mkdirs())
            throw new IOException("could not make " + m.local);
        m.copyInAll();
        return m;
    }

    /** The work copy of the game folder, for the core. */
    public File folder()
    {
        return local;
    }

    private void copyInAll() throws IOException
    {
        List<String> top = store.list("");
        if (top == null)
            throw new IOException("the game folder could not be read");
        folders.add("");
        boolean haveOriginals = false;
        String backup = find(top, GameFolder.BACKUP_FOLDER + "/");
        if (backup != null)
        {
            folders.add(strip(backup));
            List<String> inBackup = store.list(strip(backup));
            String originals = inBackup == null ? null : find(inBackup, GameFolder.ORIGINALS_FOLDER + "/");
            if (originals != null)
            {
                haveOriginals = true;
                String path = strip(backup) + "/" + strip(originals);
                folders.add(path);
                new File(local, path).mkdirs();            // stands for the copy: the core keeps none
            }
        }
        Set<String> wanted = new HashSet<String>(Arrays.asList("GP.EXE", GameFolder.BACKUP_NAME, "F1GPDATA.DAT", "F1GPDATB.DAT"));
        for (int slot = 1; slot <= GameFolder.CIRCUITS; slot++)
            wanted.add(GameFolder.trackFileName(slot));
        wanted.add("F1PREFS.DAT");                          // always: the game folder's own files are edited too
        wanted.addAll(Arrays.asList(GameFolder.PICTURE_FILES));
        wanted.add("XSOUND.BIN");
        for (String n : top)
            if (!n.endsWith("/") && wanted.contains(n.toUpperCase(java.util.Locale.ROOT)))
                copyIn(n);
        String saves = find(top, "GPSAVES/");
        if (saves != null)
            copyInFolder(strip(saves), 1);                  // as GameFolder reads it: GPSAVES and one level in
    }

    private void copyInFolder(String path, int deeper) throws IOException
    {
        List<String> names = store.list(path);
        if (names == null)
            return;
        folders.add(path);
        new File(local, path).mkdirs();
        for (String n : names)
            if (!n.endsWith("/"))
                copyIn(path + "/" + n);
            else if (deeper > 0 && !n.endsWith(WRITING + "/"))
                copyInFolder(path + "/" + strip(n), deeper - 1);
    }

    private void copyIn(String path) throws IOException
    {
        byte[] data = store.read(path);
        File f = new File(local, path);
        f.getParentFile().mkdirs();
        Files.write(f.toPath(), data);
        known.put(path, digest(data));
    }

    /**
     * Writes back every file that is new or changed since it was copied in or
     * last written back; returns the paths written, in order. On failure the
     * rest stay unwritten, and the next push tries them again.
     */
    public List<String> push() throws IOException
    {
        Map<String, byte[]> added = new LinkedHashMap<String, byte[]>(), changed = new LinkedHashMap<String, byte[]>();
        collect(local, "", added, changed);
        List<String> written = new ArrayList<String>();

        // new files, grouped by the first folder on their path that is new to the user's folder
        Map<String, List<String>> newFolders = new LinkedHashMap<String, List<String>>();
        for (String path : added.keySet())
        {
            String top = firstNewFolder(path);
            if (top == null)
                write(path, path, added.get(path), written);
            else
            {
                if (!newFolders.containsKey(top))
                    newFolders.put(top, new ArrayList<String>());
                newFolders.get(top).add(path);
            }
        }
        for (Map.Entry<String, List<String>> e : newFolders.entrySet())
        {
            String top = e.getKey(), temporary = top + WRITING;
            for (String path : e.getValue())
                write(temporary + path.substring(top.length()), null, added.get(path), written);
            store.rename(temporary, name(top));
            for (String path : e.getValue())
            {
                known.put(path, digest(added.get(path)));
                written.set(written.indexOf(temporary + path.substring(top.length())), path);
                for (String p = parent(path); !folders.contains(p); p = parent(p))
                    folders.add(p);
            }
        }
        for (Map.Entry<String, byte[]> e : changed.entrySet())
            write(e.getKey(), e.getKey(), e.getValue(), written);
        return written;
    }

    private void write(String to, String knownAs, byte[] data, List<String> written) throws IOException
    {
        store.write(to, data);
        written.add(to);
        if (knownAs != null)
        {
            known.put(knownAs, digest(data));
            for (String p = parent(knownAs); !folders.contains(p); p = parent(p))
                folders.add(p);
        }
    }

    private void collect(File dir, String prefix, Map<String, byte[]> added, Map<String, byte[]> changed) throws IOException
    {
        File[] files = dir.listFiles();
        if (files == null)
            return;
        Arrays.sort(files);
        for (File f : files)
        {
            String path = prefix + f.getName();
            if (f.isDirectory())
            {
                if (!f.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".tmp"))
                    collect(f, path + "/", added, changed);
            }
            else if (!f.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".tmp"))
            {
                byte[] data = Files.readAllBytes(f.toPath());
                String was = known.get(path);
                if (was == null)
                    added.put(path, data);
                else if (!was.equals(digest(data)))
                    changed.put(path, data);
            }
        }
    }

    /** The outermost folder on path that the user's folder does not have yet, or null. */
    private String firstNewFolder(String path)
    {
        String[] parts = path.split("/");
        String p = "";
        for (int i = 0; i < parts.length - 1; i++)
        {
            p = p.length() == 0 ? parts[i] : p + "/" + parts[i];
            if (!folders.contains(p))
                return p;
        }
        return null;
    }

    private static String parent(String path)
    {
        int i = path.lastIndexOf('/');
        return i < 0 ? "" : path.substring(0, i);
    }

    private static String name(String path)
    {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private static String strip(String folderName)
    {
        return folderName.endsWith("/") ? folderName.substring(0, folderName.length() - 1) : folderName;
    }

    /** The entry named so, ignoring case (as GameFolder does), or null. */
    private static String find(List<String> names, String name)
    {
        for (String n : names)
            if (n.equalsIgnoreCase(name))
                return n;
        return null;
    }

    private static String digest(byte[] data)
    {
        try
        {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder s = new StringBuilder();
            for (byte b : d)
                s.append(String.format("%02x", b & 0xff));
            return s.toString();
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IllegalStateException(e);
        }
    }

    private static void deleteTree(File f)
    {
        File[] inside = f.listFiles();
        if (inside != null)
            for (File g : inside)
                deleteTree(g);
        f.delete();
    }

    // ------------------------------------------------------------------

    /** A Store that is an ordinary folder (the desktop, and checking). */
    public static class DirectoryStore implements Store
    {
        private final File root;

        public DirectoryStore(File root)
        {
            this.root = root;
        }

        public String name()
        {
            return root.getName();
        }

        public List<String> list(String path)
        {
            File[] files = new File(root, path).listFiles();
            if (files == null)
                return null;
            List<String> out = new ArrayList<String>();
            for (File f : files)
                out.add(f.getName() + (f.isDirectory() ? "/" : ""));
            return out;
        }

        public byte[] read(String path) throws IOException
        {
            return Files.readAllBytes(new File(root, path).toPath());
        }

        public void write(String path, byte[] data) throws IOException
        {
            File f = new File(root, path);
            f.getParentFile().mkdirs();
            Files.write(f.toPath(), data);
        }

        public void rename(String path, String newName) throws IOException
        {
            File f = new File(root, path);
            if (!f.renameTo(new File(f.getParentFile(), newName)))
                throw new IOException("could not rename " + path);
        }
    }
}
