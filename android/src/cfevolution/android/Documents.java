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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Android's own file pickers: a file to read from anywhere, or a new file to write (2026-11-17). */
final class Documents
{
    private Documents()
    {
    }

    /** Asks for a file to read; the answer comes to onActivityResult with this request. */
    static void open(Activity a, int request)
    {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        a.startActivityForResult(i, request);
    }

    /** Asks where to write a new file, suggesting a name. */
    static void create(Activity a, int request, String name)
    {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(name.toLowerCase().endsWith(".png") ? "image/png" : name.toLowerCase().endsWith(".zip") ? "application/zip" : "application/octet-stream");
        i.putExtra(Intent.EXTRA_TITLE, name);
        a.startActivityForResult(i, request);
    }

    /** The picked file, if the answer was a file. */
    static Uri picked(int result, Intent data)
    {
        return result == Activity.RESULT_OK && data != null ? data.getData() : null;
    }

    static byte[] read(Activity a, Uri uri) throws IOException
    {
        InputStream in = a.getContentResolver().openInputStream(uri);
        if (in == null)
            throw new IOException("nothing to read");
        try
        {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            for (int n; (n = in.read(buf)) > 0; )
                bytes.write(buf, 0, n);
            return bytes.toByteArray();
        }
        finally
        {
            in.close();
        }
    }

    static void write(Activity a, Uri uri, byte[] data) throws IOException
    {
        OutputStream out = a.getContentResolver().openOutputStream(uri, "wt");
        if (out == null)
            throw new IOException("nowhere to write");
        try
        {
            out.write(data);
        }
        finally
        {
            out.close();
        }
    }

    /** A picked file's name as the picker shows it. */
    static String name(Activity a, Uri uri)
    {
        try
        {
            android.database.Cursor c = a.getContentResolver().query(uri, new String[] { android.provider.OpenableColumns.DISPLAY_NAME }, null, null, null);
            if (c != null)
                try
                {
                    if (c.moveToFirst() && c.getString(0) != null)
                        return c.getString(0);
                }
                finally
                {
                    c.close();
                }
        }
        catch (RuntimeException e)
        {
            // named below
        }
        return "The file";
    }
}
