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
import android.content.DialogInterface;
import android.content.UriPermission;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Toast;
import cfevolution.edit.PixelShape;

/**
 * Options (2026-11-28, the owner: the app needs the desktop's options, saved where they apply).
 * The desktop's Options window holds the look and feel (Swing's, nothing to match here), the game
 * folder (here the list of folders opened before, which Android keeps), the legacy best line
 * methods (not in the app, which has Fastest Lap Simulation only), Clear Recent File List (here:
 * forget the folders opened before) and how the game's pictures are shown. Each choice is saved
 * as it is made.
 */
public class OptionsActivity extends Activity
{
    protected void onCreate(Bundle state)
    {
        super.onCreate(state);
        setTitle("CF Evolution Options");
        LinearLayout col = Ui.column(this);

        col.addView(Ui.heading(this, PixelShape.LABEL));
        col.addView(Ui.text(this, PixelShape.TIP, 13));
        final RadioGroup shape = new RadioGroup(this);
        for (int i = 0; i < PixelShape.CHOICES.length; i++)
        {
            RadioButton b = new RadioButton(this);
            b.setId(i + 1);
            b.setText(PixelShape.CHOICES[i]);
            shape.addView(b);
        }
        shape.check(PixelShape.square() ? 2 : 1);
        shape.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup g, int id) { Settings.setSquarePixels(OptionsActivity.this, id == 2); }
        });
        col.addView(shape);

        col.addView(Ui.heading(this, "Game folders opened before"));
        col.addView(Ui.text(this, "The first screen lists the game folders you have chosen, to open again with one tap.", 13));
        col.addView(Ui.button(this, "Clear the List of Game Folders", new View.OnClickListener() {
            public void onClick(View v) { clearFolders(); }
        }));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(col);
        setContentView(scroll);
    }

    /** The desktop's Clear Recent File List: Android's lasting access to each folder is given back. */
    private void clearFolders()
    {
        new AlertDialog.Builder(this).setTitle("Clear the List of Game Folders")
            .setMessage("Are you sure you want to clear the list of game folders opened before? "
                        + "The folders themselves are not changed; choose one again with Choose Game Folder. "
                        + "A folder open now stays in the list.")
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int which)
                {
                    for (UriPermission p : getContentResolver().getPersistedUriPermissions())
                    {
                        if (p.getUri().equals(Session.openTree))
                            continue;       // the folder open now still saves back through it
                        try
                        {
                            getContentResolver().releasePersistableUriPermission(p.getUri(),
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        }
                        catch (SecurityException e)
                        {
                            // already gone
                        }
                    }
                    Toast.makeText(OptionsActivity.this, "The list of game folders is cleared.", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("No", null).show();
    }
}
