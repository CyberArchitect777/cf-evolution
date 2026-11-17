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

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import cfevolution.screens.FolderScreen;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The start screen: where the game folder is, and opening it. The core reads
 * and writes the game's files by path, so the app needs access to the folder
 * they are in.
 */
public class MainActivity extends Activity
{
    private static final String PREFS = "cfevolution", LAST_FOLDER = "last game folder";

    private EditText folderField;
    private LinearLayout found;
    private Button access;

    protected void onCreate(Bundle state)
    {
        super.onCreate(state);
        setTitle("CF Evolution");
        int pad = Ui.dp(this, 16);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(pad, pad, pad, pad);

        col.addView(Ui.text(this, "Open the folder your copy of Formula One Grand Prix (or World Circuit) is in: "
            + "the one with GP.EXE and the circuit files F1CT01.DAT to F1CT16.DAT. Copy it onto this device first, "
            + "for example into Documents.", 15));

        access = Ui.button(this, "Allow Access to Files", new View.OnClickListener() {
            public void onClick(View v) { askForAccess(); }
        });
        col.addView(access);

        col.addView(Ui.heading(this, "Game folder"));
        folderField = new EditText(this);
        folderField.setSingleLine(true);
        folderField.setText(getSharedPreferences(PREFS, MODE_PRIVATE).getString(LAST_FOLDER,
            new File(Environment.getExternalStorageDirectory(), "Documents").getPath()));
        col.addView(folderField);
        col.addView(Ui.button(this, "Open Game Folder", new View.OnClickListener() {
            public void onClick(View v) { open(new File(folderField.getText().toString().trim())); }
        }));

        col.addView(Ui.heading(this, "Game folders found on this device"));
        found = new LinearLayout(this);
        found.setOrientation(LinearLayout.VERTICAL);
        col.addView(found);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(col);
        setContentView(scroll);
    }

    protected void onResume()
    {
        super.onResume();
        access.setVisibility(hasAccess() ? View.GONE : View.VISIBLE);
        listFound();
    }

    private boolean hasAccess()
    {
        if (Build.VERSION.SDK_INT >= 30)
            return Environment.isExternalStorageManager();
        return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void askForAccess()
    {
        if (Build.VERSION.SDK_INT >= 30)
        {
            Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                  Uri.parse("package:" + getPackageName()));
            startActivity(i);
        }
        else
            requestPermissions(new String[] { Manifest.permission.READ_EXTERNAL_STORAGE,
                                              Manifest.permission.WRITE_EXTERNAL_STORAGE }, 1);
    }

    public void onRequestPermissionsResult(int code, String[] permissions, int[] results)
    {
        onResume();
    }

    /** Folders with a GP.EXE, up to three levels below the device's shared storage. */
    private void listFound()
    {
        found.removeAllViews();
        if (!hasAccess())
        {
            found.addView(Ui.text(this, "Allow access to files to look for game folders.", 14));
            return;
        }
        List<File> folders = new ArrayList<File>();
        look(Environment.getExternalStorageDirectory(), 0, folders);
        if (folders.isEmpty())
            found.addView(Ui.text(this, "None found. Type the folder's path above.", 14));
        for (final File f : folders)
            found.addView(Ui.button(this, f.getPath(), new View.OnClickListener() {
                public void onClick(View v) { folderField.setText(f.getPath()); open(f); }
            }));
    }

    private static void look(File dir, int depth, List<File> out)
    {
        File[] list = dir.listFiles();
        if (list == null || out.size() >= 20)
            return;
        for (File f : list)
            if (f.isFile() && f.getName().equalsIgnoreCase("GP.EXE"))
            {
                out.add(dir);
                break;
            }
        if (depth >= 3)
            return;
        for (File f : list)
            if (f.isDirectory() && !f.getName().startsWith(".") && !f.getName().equals("Android")
                && !f.getName().equals("CFE-BACKUP"))
                look(f, depth + 1, out);
    }

    private void open(final File folder)
    {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(LAST_FOLDER, folder.getPath()).apply();
        final FolderScreen.Opening opening = FolderScreen.open(folder);
        Dialogs.run(this, opening, new Runnable() {
            public void run()
            {
                if (opening.opened() == null)
                    return;
                Session.folder = opening.opened();
                startActivity(new Intent(MainActivity.this, FolderActivity.class));
            }
        });
    }
}
