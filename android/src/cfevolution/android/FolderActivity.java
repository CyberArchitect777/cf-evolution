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
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import cfevolution.edit.Message;
import cfevolution.screens.ExecutableScreen;
import cfevolution.screens.FolderScreen;
import cfevolution.screens.TrackScreen;

/** The game folder: GP.EXE and the 16 circuits, as the desktop's game folder tab lists them. */
public class FolderActivity extends Activity
{
    protected void onCreate(Bundle state)
    {
        super.onCreate(state);
        if (Session.folder == null)
        {
            finish();
            return;
        }
        setTitle(Session.folder.title());
        LinearLayout col = Ui.column(this);
        for (final FolderScreen.Row row : Session.folder.rows())
        {
            if (row.heading)
            {
                col.addView(Ui.heading(this, row.label));
                continue;
            }
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            int p = Ui.dp(this, 8);
            item.setPadding(0, p, 0, p);
            TextView label = Ui.text(this, (row.number.length() > 0 ? row.number + "  " : "") + row.label, 16);
            item.addView(label);
            item.addView(Ui.text(this, row.detail, 13));
            if (row.file != null)
            {
                item.setClickable(true);
                item.setBackgroundResource(android.R.drawable.list_selector_background);
                item.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) { open(row); }
                });
            }
            else
                label.setEnabled(false);
            col.addView(item);
        }
        ScrollView scroll = new ScrollView(this);
        scroll.addView(col);
        setContentView(scroll);
    }

    private void open(FolderScreen.Row row)
    {
        Message[] problem = new Message[1];
        if (row.what == FolderScreen.EXECUTABLE)
        {
            ExecutableScreen e = Session.folder.openExecutable(problem);
            if (e == null)
            {
                Dialogs.show(this, problem[0], null);
                return;
            }
            Session.executable = e;
            startActivity(new Intent(this, ExecutableActivity.class));
        }
        else
        {
            TrackScreen t = Session.folder.openTrack(row.what, problem);
            if (t == null)
            {
                Dialogs.show(this, problem[0], null);
                return;
            }
            Session.track = t;
            startActivity(new Intent(this, TrackActivity.class));
        }
    }
}
