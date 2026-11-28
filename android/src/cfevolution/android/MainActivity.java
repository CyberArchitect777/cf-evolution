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
import android.content.UriPermission;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import cfevolution.edit.Message;
import cfevolution.screens.FolderMirror;
import cfevolution.screens.FolderScreen;
import java.io.File;
import java.util.List;

/**
 * The start screen: choosing the game folder in Android's folder picker, and
 * opening it. The app can reach the folders chosen there and nothing else; the
 * game's files are copied in for the core and changes written back
 * (FolderMirror, owner's choice 2026-11-17, replacing "All files access").
 */
public class MainActivity extends Activity
{
    private static final int CHOOSE = 1;

    static final Message NOT_READ = Message.error("Game Folder Not Opened",
        "The game folder could not be read. Choose it again with Choose Game Folder.");

    private LinearLayout before;

    protected void onCreate(Bundle state)
    {
        super.onCreate(state);
        CrashReport.install(this);
        Session.pictures();
        Settings.load(this);
        setTitle("CF Evolution");
        int pad = Ui.dp(this, 16);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(pad, pad, pad, pad);

        col.addView(Ui.text(this, "Choose the folder your copy of Formula One Grand Prix (or World Circuit) is in: "
            + "the one with GP.EXE and the circuit files F1CT01.DAT to F1CT16.DAT. Copy it onto this device first, "
            + "for example into Documents. CF Evolution can then use that folder and no other.", 15));
        col.addView(Ui.button(this, "Choose Game Folder", new View.OnClickListener() {
            public void onClick(View v) { choose(); }
        }));

        col.addView(Ui.button(this, "Options", new View.OnClickListener() {
            public void onClick(View v) { startActivity(new Intent(MainActivity.this, OptionsActivity.class)); }
        }));

        col.addView(Ui.heading(this, "Game folders opened before"));
        before = new LinearLayout(this);
        before.setOrientation(LinearLayout.VERTICAL);
        col.addView(before);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(col);
        setContentView(scroll);
        CrashReport.showLast(this);     // what stopped the app last time, if anything
    }

    protected void onResume()
    {
        super.onResume();
        listBefore();
    }

    private void choose()
    {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                   | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        startActivityForResult(i, CHOOSE);
    }

    protected void onActivityResult(int code, int result, Intent data)
    {
        if (code != CHOOSE || result != RESULT_OK || data == null || data.getData() == null)
            return;
        Uri tree = data.getData();
        try
        {
            getContentResolver().takePersistableUriPermission(tree,
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }
        catch (SecurityException e)
        {
            // still usable now; it will not be offered next time
        }
        open(tree);
    }

    /** The folders the picker has given lasting access to. */
    private void listBefore()
    {
        before.removeAllViews();
        List<UriPermission> kept = getContentResolver().getPersistedUriPermissions();
        int shown = 0;
        for (final UriPermission p : kept)
        {
            if (!p.isReadPermission() || !p.isWritePermission())
                continue;
            String name = PickedFolder.displayName(this, p.getUri());
            if (name == null)
                continue;                                   // gone, or no longer allowed
            before.addView(Ui.button(this, name, new View.OnClickListener() {
                public void onClick(View v) { open(p.getUri()); }
            }));
            shown++;
        }
        if (shown == 0)
            before.addView(Ui.text(this, "None yet.", 14));
    }

    private void open(final Uri tree)
    {
        final PickedFolder store = new PickedFolder(this, tree);
        final FolderMirror[] mirror = new FolderMirror[1];
        Session.slowly(this, "Reading the game folder...", new Session.Work() {
            public void run() throws Exception { mirror[0] = FolderMirror.copyIn(store, new File(getFilesDir(), "game")); }
        }, new Session.Done() {
            public void done(Exception failed)
            {
                if (failed != null)
                    Dialogs.show(MainActivity.this, NOT_READ, null);
                else
                    opened(mirror[0], tree);
            }
        });
    }

    /** The files are in: open the folder as the desktop does, then write back what opening changed. */
    private void opened(final FolderMirror mirror, final Uri tree)
    {
        Session.mirror = mirror;
        final FolderScreen.Opening opening = FolderScreen.open(mirror.folder());
        Dialogs.run(this, opening, new Runnable() {
            public void run()
            {
                // the first-open copy, an unpacked GP.EXE and its backup, even when not opened in the end
                Session.writeBack(MainActivity.this, new Runnable() {
                    public void run()
                    {
                        if (opening.opened() == null)
                            return;
                        Session.folder = opening.opened();
                        Session.openTree = tree;
                        startActivity(new Intent(MainActivity.this, FolderActivity.class));
                    }
                });
            }
        });
    }
}
