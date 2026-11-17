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
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import cfevolution.edit.AllCircuitsCars;
import cfevolution.edit.Message;
import java.util.ArrayList;
import java.util.List;

/**
 * Game folder > Computer Cars at Every Circuit: the computer cars' cornering at
 * all 16 circuits on one screen (the desktop's page; the values, words and
 * rules are AllCircuitsCars'). Tap a circuit to change its three values; tick
 * circuits to set or add to several at once.
 */
public class CircuitsActivity extends Activity
{
    private AllCircuitsCars page;
    private LinearLayout col;
    private final List<Integer> ticked = new ArrayList<Integer>();
    private int which;

    protected void onCreate(Bundle state)
    {
        super.onCreate(state);
        if (Session.folder == null)
        {
            finish();
            return;
        }
        setTitle(AllCircuitsCars.TITLE);
        page = Session.folder.allCircuits();
        col = Ui.column(this);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(col);
        setContentView(scroll);
        show();
    }

    private void show()
    {
        col.removeAllViews();
        col.addView(Ui.text(this, AllCircuitsCars.INTRO, 13));
        if (page.raised())
        {
            TextView w = Ui.text(this, AllCircuitsCars.WARNING, 13);
            w.setTextColor(0xFFB04A00);
            col.addView(w);
        }
        LinearLayout buttons = new LinearLayout(this);
        buttons.addView(Ui.button(this, "Save", new View.OnClickListener() {
            public void onClick(View v) { save(); }
        }));
        buttons.addView(Ui.button(this, "Set or Add...", new View.OnClickListener() {
            public void onClick(View v) { bulk(); }
        }));
        buttons.addView(Ui.button(this, AllCircuitsCars.UNDO, new View.OnClickListener() {
            public void onClick(View v) { page.undo(); show(); }
        }));
        android.widget.HorizontalScrollView bar = new android.widget.HorizontalScrollView(this);
        bar.addView(buttons);
        col.addView(bar);
        TextView how = Ui.text(this, "Tap a circuit to change its values. Tick circuits for Set or Add; it changes every circuit when none is ticked.", 13);
        how.setTextColor(Color.GRAY);
        col.addView(how);
        for (int i = 0; i < page.rows(); i++)
        {
            final int r = i;
            LinearLayout line = new LinearLayout(this);
            line.setGravity(android.view.Gravity.CENTER_VERTICAL);
            CheckBox tick = new CheckBox(this);
            tick.setChecked(ticked.contains(r));
            tick.setEnabled(page.hasTrack(r));
            tick.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(android.widget.CompoundButton b, boolean on)
                {
                    ticked.remove(Integer.valueOf(r));
                    if (on)
                        ticked.add(r);
                }
            });
            line.addView(tick);
            StringBuilder s = new StringBuilder(page.cell(r, 0)).append("\n     ");
            if (page.hasTrack(r))
                for (int c = 1; c <= 3; c++)
                    s.append(c > 1 ? ",  " : "").append(AllCircuitsCars.WHICH[c]).append(" ").append(page.cell(r, c))
                     .append(page.isChanged(r, c) ? " *" : "");
            else
                s.append(page.cell(r, 1));
            TextView t = Ui.text(this, s.toString(), 15);
            if (!page.hasTrack(r))
                t.setTextColor(0xFFA0A6AE);
            t.setBackgroundResource(android.R.drawable.list_selector_background);
            t.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { if (page.hasTrack(r)) edit(r); }
            });
            line.addView(t, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            col.addView(line);
        }
        TextView key = Ui.text(this, "* changed, not saved yet", 12);
        key.setTextColor(Color.GRAY);
        col.addView(key);
    }

    private void edit(final int r)
    {
        LinearLayout form = Ui.column(this);
        final EditText[] boxes = new EditText[3];
        for (int c = 1; c <= 3; c++)
            boxes[c - 1] = Ui.field(form, AllCircuitsCars.TITLES[c - 1], page.cell(r, c), true);
        TextView note = Ui.text(this, "From " + AllCircuitsCars.MIN + " to " + AllCircuitsCars.MAX + ".", 12);
        note.setTextColor(Color.GRAY);
        form.addView(note);
        new AlertDialog.Builder(this).setTitle(page.cell(r, 0)).setView(form)
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w)
                {
                    for (int c = 1; c <= 3; c++)
                        page.set(r, c, boxes[c - 1].getText().toString());
                    show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void bulk()
    {
        LinearLayout form = Ui.column(this);
        form.addView(Ui.text(this, "Change", 14));
        final android.widget.Spinner pick = new android.widget.Spinner(this);
        android.widget.ArrayAdapter<String> names = new android.widget.ArrayAdapter<String>(this,
            android.R.layout.simple_spinner_item, AllCircuitsCars.WHICH);
        names.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        pick.setAdapter(names);
        pick.setSelection(which);
        form.addView(pick);
        final EditText value = Ui.field(form, "Value (Set gives this percentage; Add adds it, a minus number lowers)", "100", true);
        TextView note = Ui.text(this, ticked.isEmpty() ? "No circuit is ticked, so every circuit changes."
                                                       : ticked.size() + (ticked.size() == 1 ? " circuit is" : " circuits are") + " ticked.", 12);
        note.setTextColor(Color.GRAY);
        form.addView(note);
        final int[] rows = new int[ticked.size()];
        for (int i = 0; i < rows.length; i++)
            rows[i] = ticked.get(i);
        new AlertDialog.Builder(this).setTitle("Set or Add").setView(form)
            .setPositiveButton("Set", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) { apply(true, value, pick, rows); }
            })
            .setNeutralButton("Add", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) { apply(false, value, pick, rows); }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void apply(boolean set, EditText value, android.widget.Spinner pick, int[] rows)
    {
        which = pick.getSelectedItemPosition();
        int v;
        try
        {
            v = Integer.parseInt(value.getText().toString().trim());
        }
        catch (NumberFormatException e)
        {
            Dialogs.show(this, Message.warning("Not Changed", "The value must be a whole number."), null);
            return;
        }
        page.bulk(set, v, which, rows);
        show();
    }

    private void save()
    {
        final Message m = page.save(null);
        show();
        if (m.title.equals("Saved"))
            Session.writeBack(this, new Runnable() {
                public void run() { Dialogs.show(CircuitsActivity.this, m, null); }
            });
        else
            Dialogs.show(this, m, null);
    }

    public void onBackPressed()
    {
        if (!page.hasChanges())
        {
            super.onBackPressed();
            return;
        }
        Message q = AllCircuitsCars.CLOSE_QUESTION;
        new AlertDialog.Builder(this).setTitle(q.title).setMessage(q.text)
            .setPositiveButton(q.options[0], new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) { finish(); }
            })
            .setNegativeButton(q.options[1], null)
            .show();
    }
}
