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
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * When the app stops on an error, what went wrong is kept and shown the next
 * time it starts, with a button to copy it (2026-11-21: the Liveries page
 * stopped the app on the owner's phone, and with no computer attached there
 * was no other way to see why).
 */
final class CrashReport
{
    private static final String FILE = "last-error.txt";
    private static boolean installed;

    private CrashReport()
    {
    }

    /** The error as text: what it was and where, for sending on. */
    static String describe(Context c, Throwable t)
    {
        StringWriter s = new StringWriter();
        t.printStackTrace(new PrintWriter(s));
        String version = "";
        try
        {
            android.content.pm.PackageInfo info = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            version = " " + info.versionName + ", build " + info.versionCode;
        }
        catch (Exception ignored)
        {
            // the version is only a help
        }
        return "CF Evolution" + version + ", Android " + android.os.Build.VERSION.RELEASE + "\n" + s;
    }

    static void install(final Context c)
    {
        if (installed)
            return;
        installed = true;
        final File file = new File(c.getApplicationContext().getFilesDir(), FILE);
        final Thread.UncaughtExceptionHandler before = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread thread, Throwable error)
            {
                try
                {
                    FileOutputStream out = new FileOutputStream(file);
                    out.write(describe(c, error).getBytes("UTF-8"));
                    out.close();
                }
                catch (Exception ignored)
                {
                    // nothing more can be done
                }
                if (before != null)
                    before.uncaughtException(thread, error);
            }
        });
    }

    /** Shows the error kept from last time, if there is one, and forgets it. */
    static void showLast(Activity a)
    {
        File file = new File(a.getFilesDir(), FILE);
        if (!file.isFile())
            return;
        String text;
        try
        {
            text = new String(java.nio.file.Files.readAllBytes(file.toPath()), "UTF-8");
        }
        catch (Exception e)
        {
            text = "(the details could not be read)";
        }
        file.delete();
        show(a, "The app stopped last time", text);
    }

    /** The details of an error, selectable, with Copy. */
    static void show(final Activity a, String title, final String text)
    {
        TextView t = Ui.text(a, "Please send this to the developer:\n\n" + text, 12);
        t.setTextIsSelectable(true);
        int pad = Ui.dp(a, 12);
        t.setPadding(pad, pad, pad, pad);
        ScrollView s = new ScrollView(a);
        s.addView(t);
        new AlertDialog.Builder(a).setTitle(title).setView(s)
            .setPositiveButton("Copy", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w)
                {
                    ClipboardManager clip = (ClipboardManager) a.getSystemService(Context.CLIPBOARD_SERVICE);
                    clip.setPrimaryClip(ClipData.newPlainText("CF Evolution error", text));
                }
            })
            .setNegativeButton("Close", null)
            .show();
    }
}
