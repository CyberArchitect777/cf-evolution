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

package cfevolution.android;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import cfevolution.screens.FolderMirror;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A folder the user chose in Android's folder picker, as a FolderMirror.Store
 * (2026-11-17): reached through its content links, with the lasting access
 * the picker grants to that folder only.
 */
final class PickedFolder implements FolderMirror.Store
{
    private static final String FILE_TYPE = "application/octet-stream";

    private final ContentResolver resolver;
    private final Uri tree;
    /** Document ids by path, as found. */
    private final Map<String, String> ids = new HashMap<String, String>();

    PickedFolder(Context context, Uri tree)
    {
        this.resolver = context.getContentResolver();
        this.tree = tree;
        ids.put("", DocumentsContract.getTreeDocumentId(tree));
    }

    /** The folder's name as the picker shows it, or null. */
    static String displayName(Context context, Uri tree)
    {
        try
        {
            Uri doc = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree));
            Cursor c = context.getContentResolver().query(doc, new String[] { Document.COLUMN_DISPLAY_NAME }, null, null, null);
            if (c == null)
                return null;
            try
            {
                return c.moveToFirst() ? c.getString(0) : null;
            }
            finally
            {
                c.close();
            }
        }
        catch (RuntimeException e)
        {
            return null;
        }
    }

    public String name() throws IOException
    {
        String n = displayName0();
        if (n == null)
            throw new IOException("the game folder could not be reached");
        return n;
    }

    private String displayName0()
    {
        Cursor c = resolver.query(uri(ids.get("")), new String[] { Document.COLUMN_DISPLAY_NAME }, null, null, null);
        if (c == null)
            return null;
        try
        {
            return c.moveToFirst() ? c.getString(0) : null;
        }
        finally
        {
            c.close();
        }
    }

    public List<String> list(String path) throws IOException
    {
        String id = id(path);
        if (id == null)
            return null;
        List<String> out = new ArrayList<String>();
        for (String[] child : children(id))
        {
            boolean folder = Document.MIME_TYPE_DIR.equals(child[2]);
            out.add(child[1] + (folder ? "/" : ""));
            ids.put(join(path, child[1]), child[0]);
        }
        return out;
    }

    public byte[] read(String path) throws IOException
    {
        String id = id(path);
        if (id == null)
            throw new FileNotFoundException(path);
        InputStream in = resolver.openInputStream(uri(id));
        if (in == null)
            throw new IOException("could not read " + path);
        try
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[65536];
            for (int n; (n = in.read(buf)) > 0; )
                out.write(buf, 0, n);
            return out.toByteArray();
        }
        finally
        {
            in.close();
        }
    }

    public void write(String path, byte[] data) throws IOException
    {
        String id = id(path);
        if (id == null)
        {
            String parent = folder(parentOf(path));
            Uri made = DocumentsContract.createDocument(resolver, uri(parent), FILE_TYPE, nameOf(path));
            if (made == null)
                throw new IOException("could not make " + path);
            id = DocumentsContract.getDocumentId(made);
            ids.put(path, id);
        }
        OutputStream out = resolver.openOutputStream(uri(id), "wt");    // "wt": replace the whole file
        if (out == null)
            throw new IOException("could not write " + path);
        try
        {
            out.write(data);
        }
        finally
        {
            out.close();
        }
    }

    public void rename(String path, String newName) throws IOException
    {
        String id = id(path);
        if (id == null)
            throw new FileNotFoundException(path);
        Uri renamed = DocumentsContract.renameDocument(resolver, uri(id), newName);
        if (renamed == null)
            throw new IOException("could not rename " + path);
        // ids under the old name are gone; find them again when needed
        String root = ids.get("");
        ids.clear();
        ids.put("", root);
    }

    /** The folder's id, making it (and the folders it is in) if needed. */
    private String folder(String path) throws IOException
    {
        String id = id(path);
        if (id != null)
            return id;
        String parent = folder(parentOf(path));
        Uri made = DocumentsContract.createDocument(resolver, uri(parent), Document.MIME_TYPE_DIR, nameOf(path));
        if (made == null)
            throw new IOException("could not make " + path);
        id = DocumentsContract.getDocumentId(made);
        ids.put(path, id);
        return id;
    }

    /** The document id of path, or null if there is nothing there. */
    private String id(String path) throws IOException
    {
        String id = ids.get(path);
        if (id != null)
            return id;
        String parent = id(parentOf(path));
        if (parent == null)
            return null;
        for (String[] child : children(parent))
            if (child[1].equals(nameOf(path)))
            {
                ids.put(path, child[0]);
                return child[0];
            }
        return null;
    }

    /** id, name, type of each thing in a folder. */
    private List<String[]> children(String folderId) throws IOException
    {
        Uri list = DocumentsContract.buildChildDocumentsUriUsingTree(tree, folderId);
        Cursor c;
        try
        {
            c = resolver.query(list, new String[] { Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME,
                                                    Document.COLUMN_MIME_TYPE }, null, null, null);
        }
        catch (RuntimeException e)            // access withdrawn, folder gone
        {
            throw new IOException(e.toString());
        }
        if (c == null)
            throw new IOException("the game folder could not be read");
        List<String[]> out = new ArrayList<String[]>();
        try
        {
            while (c.moveToNext())
                out.add(new String[] { c.getString(0), c.getString(1), c.getString(2) });
        }
        finally
        {
            c.close();
        }
        return out;
    }

    private Uri uri(String id)
    {
        return DocumentsContract.buildDocumentUriUsingTree(tree, id);
    }

    private static String join(String path, String name)
    {
        return path.length() == 0 ? name : path + "/" + name;
    }

    private static String parentOf(String path)
    {
        int i = path.lastIndexOf('/');
        return i < 0 ? "" : path.substring(0, i);
    }

    private static String nameOf(String path)
    {
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
