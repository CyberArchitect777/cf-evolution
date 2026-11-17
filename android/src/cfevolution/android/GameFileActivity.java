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
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import cfevolution.data.GameFolder;
import cfevolution.edit.ChampionshipPage;
import cfevolution.edit.GameFilePage;
import cfevolution.edit.Message;
import cfevolution.edit.NameFilePage;
import cfevolution.edit.PreferencesPage;
import cfevolution.edit.SetupFilePage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * One of the game folder's own files: the game options, a name file, a setup
 * file or a championship save, laid out from the core's page with the
 * desktop's wording. Tick boxes and lists change the file at once; numbers
 * and names are typed in a small form (a refusal shows the core's words, then
 * the form again as typed). Save writes the file and copies it back.
 */
public class GameFileActivity extends Activity
{
    private GameFilePage page;
    private File file;
    private LinearLayout col;
    private boolean modified;
    private int nameKind, sound;

    protected void onCreate(Bundle state)
    {
        super.onCreate(state);
        page = Session.gameFile;
        file = Session.gameFilePath;
        if (page == null || file == null)
        {
            finish();
            return;
        }
        col = Ui.column(this);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(col);
        setContentView(scroll);
        build();
    }

    private static View.OnClickListener click(final Runnable r)
    {
        return new View.OnClickListener() { public void onClick(View v) { r.run(); } };
    }

    private TextView hint(String text)
    {
        TextView h = Ui.text(this, text, 12);
        h.setTextColor(Color.GRAY);
        return h;
    }

    /** After a change the page made (or refused). */
    private void changed()
    {
        if (page.changed())
            modified = true;
        setTitle(file.getName() + (modified ? " *" : ""));
    }

    /** The whole screen again, from the page. */
    private void build()
    {
        col.removeAllViews();
        setTitle(file.getName() + (modified ? " *" : ""));
        if (getActionBar() != null)
            getActionBar().setSubtitle(page.status());
        col.addView(Ui.heading(this, page.title()));
        col.addView(hint(page.explanation()));
        col.addView(Ui.button(this, "Save", click(new Runnable() { public void run() { save(null); } })));
        if (page instanceof NameFilePage)
            names((NameFilePage) page);
        else if (page instanceof SetupFilePage)
            setups((SetupFilePage) page);
        else if (page instanceof ChampionshipPage)
            championship((ChampionshipPage) page);
        else if (page instanceof cfevolution.edit.SoundEffectsPage)
            sounds((cfevolution.edit.SoundEffectsPage) page);
        else
            options((PreferencesPage) page);
    }

    /** A line to tap, with a second line under it. */
    private void item(String text, String detail, final Runnable tap)
    {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 6);
        row.setPadding(0, p, 0, p);
        row.addView(Ui.text(this, text, 15));
        if (detail != null)
            row.addView(hint(detail));
        row.setClickable(true);
        row.setBackgroundResource(android.R.drawable.list_selector_background);
        row.setOnClickListener(click(tap));
        col.addView(row);
    }

    private Spinner spinner(String[] items, int chosen, final Picked picked)
    {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> a = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(a);
        s.setSelection(chosen);
        final int[] was = { chosen };
        s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id)
            {
                if (position == was[0])
                    return;
                was[0] = position;
                picked.picked(position);
            }
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        return s;
    }

    private interface Picked
    {
        void picked(int index);
    }

    private CheckBox tick(String label, boolean on, final Picked changed)
    {
        CheckBox b = new CheckBox(this);
        b.setText(label);
        b.setChecked(on);
        b.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton c, boolean value) { changed.picked(value ? 1 : 0); }
        });
        return b;
    }

    /** OK on a form: the refusal, or null. */
    private interface Typed
    {
        Message ok(String[] values);
    }

    /** A small form; a refusal shows why, then the form again as typed. */
    private void form(final String title, final String[] labels, final String[] values, final boolean[] numbers, final Typed typed)
    {
        LinearLayout form = Ui.column(this);
        final EditText[] boxes = new EditText[labels.length];
        for (int i = 0; i < labels.length; i++)
            boxes[i] = Ui.field(form, labels[i], values[i], numbers[i]);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);
        new AlertDialog.Builder(this).setTitle(title).setView(scroll)
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int which)
                {
                    final String[] v = new String[boxes.length];
                    for (int i = 0; i < v.length; i++)
                        v[i] = boxes[i].getText().toString();
                    Message m = typed.ok(v);
                    build();
                    Dialogs.show(GameFileActivity.this, m, m == null ? null : new Runnable() {
                        public void run() { form(title, labels, v, numbers, typed); }
                    });
                }
            })
            .setNegativeButton("Cancel", null).show();
    }

    private static boolean[] all(int n, boolean value)
    {
        boolean[] b = new boolean[n];
        java.util.Arrays.fill(b, value);
        return b;
    }

    // --- a name file -----------------------------------------------------------

    private void names(final NameFilePage p)
    {
        col.addView(spinner(NameFilePage.TAB_TITLES, nameKind, new Picked() {
            public void picked(int index) { nameKind = index; build(); }
        }));
        final int kind = nameKind;
        final String what = NameFilePage.columns(kind)[1];
        for (int r = 0; r < NameFilePage.rows(kind); r++)
        {
            final int row = r;
            item((r + 1) + "  " + p.get(kind, r), null, new Runnable() {
                public void run()
                {
                    form(what + " " + (row + 1), new String[] { what }, new String[] { p.get(kind, row) }, new boolean[1], new Typed() {
                        public Message ok(String[] v)
                        {
                            Message m = p.set(kind, row, v[0]);
                            changed();
                            return m;
                        }
                    });
                }
            });
        }
    }

    // --- a setup file ----------------------------------------------------------

    private void setups(final SetupFilePage p)
    {
        if (p.isSeason())
            col.addView(tick(SetupFilePage.SEPARATE, p.separate(), new Picked() {
                public void picked(int on) { p.setSeparate(on == 1); changed(); }
            }));
        if (p.isSeason())
            col.addView(hint(SetupFilePage.SEPARATE_TIP));
        for (int r = 0; r < p.rows(); r++)
        {
            final int row = r;
            StringBuilder gears = new StringBuilder();
            for (int f = 2; f < 8; f++)
                gears.append(f > 2 ? " " : "").append(p.cell(r, f));
            item(p.rowName(r), "Wings " + p.cell(r, 0) + ", " + p.cell(r, 1) + "  ·  gears " + gears + "  ·  tyres " + p.cell(r, 8)
                 + "  ·  brake balance " + p.cell(r, 9), new Runnable() {
                public void run() { setup(p, row, null); }
            });
        }
    }

    /** One setup's ten values in one form. They are tried in turns, so a whole set of new gears goes in
        whatever order the game's gear rule needs; the first value it still refuses is said. */
    private void setup(final SetupFilePage p, final int row, String[] again)
    {
        String[] labels = SetupFilePage.COLUMNS.clone();
        labels[8] += " (A, B, C or D)";
        String[] values = new String[10];
        boolean[] numbers = all(10, true);
        numbers[8] = false;
        for (int f = 0; f < 10; f++)
            values[f] = again != null ? again[f] : String.valueOf(p.cell(row, f));
        form(p.rowName(row), labels, values, numbers, new Typed() {
            public Message ok(String[] v)
            {
                List<Integer> left = new ArrayList<Integer>();
                for (int f = 0; f < 10; f++)
                    left.add(f);
                Message refused = null;
                boolean any = false;
                for (boolean progress = true; progress && !left.isEmpty(); )
                {
                    progress = false;
                    refused = null;
                    for (int i = 0; i < left.size(); i++)
                    {
                        Message m = p.set(row, left.get(i), v[left.get(i)]);
                        any |= p.changed();
                        if (m == null)
                        {
                            left.remove(i--);
                            progress = true;
                        }
                        else if (refused == null)
                            refused = m;
                    }
                }
                if (any)
                    modified = true;
                return left.isEmpty() ? null : refused;
            }
        });
    }

    // --- a championship save -----------------------------------------------------

    private void championship(final ChampionshipPage p)
    {
        col.addView(Ui.text(this, ChampionshipPage.NEXT_ROUND, 14));
        col.addView(spinner(ChampionshipPage.rounds(), p.nextRound(), new Picked() {
            public void picked(int index) { p.setNextRound(index); changed(); build(); }
        }));
        col.addView(Ui.heading(this, ChampionshipPage.WET_ROUNDS));
        for (int r = 1; r <= 16; r++)
        {
            final int round = r;
            col.addView(tick(r + "  " + GameFolder.CIRCUIT_NAMES[r - 1], p.wet(r), new Picked() {
                public void picked(int on) { p.setWet(round, on == 1); changed(); }
            }));
        }
        col.addView(Ui.heading(this, "Finishing places"));
        String[] columns = p.columns();
        for (int r = 0; r < ChampionshipPage.rows(); r++)
        {
            final int row = r;
            StringBuilder places = new StringBuilder();
            for (int round = 1; round <= 16; round++)
                places.append(round > 1 ? " " : "").append(p.cell(r, ChampionshipPage.FIRST_ROUND + round - 1));
            String points = p.showsPoints() ? "  ·  " + p.cell(r, columns.length - 1) + " points" : "";
            item((r + 1) + "  " + p.cell(r, 1), places + points, new Runnable() {
                public void run() { places(p, row); }
            });
        }
    }

    private void places(final ChampionshipPage p, final int row)
    {
        String[] labels = new String[16], values = new String[16];
        for (int i = 0; i < 16; i++)
        {
            labels[i] = (i + 1) + "  " + GameFolder.CIRCUIT_NAMES[i];
            values[i] = String.valueOf(p.cell(row, ChampionshipPage.FIRST_ROUND + i));
        }
        form((row + 1) + "  " + p.cell(row, 1), labels, values, all(16, true), new Typed() {
            public Message ok(String[] v)
            {
                for (int i = 0; i < 16; i++)
                {
                    Message m = p.setPlace(row, i + 1, v[i]);
                    changed();
                    if (m != null)
                        return m;
                }
                return null;
            }
        });
    }

    // --- the sound effects ------------------------------------------------------------

    private void sounds(final cfevolution.edit.SoundEffectsPage p)
    {
        col.addView(spinner(cfevolution.edit.SoundEffectsPage.SOUNDS, sound, new Picked() {
            public void picked(int index) { sound = index; build(); }
        }));
        col.addView(Ui.button(this, cfevolution.edit.SoundEffectsPage.REVERT, click(new Runnable() {
            public void run() { p.revert(); changed(); build(); }
        })));
        col.addView(hint(cfevolution.edit.SoundEffectsPage.HINT));
        for (cfevolution.edit.SoundEffectsPage.Row r : p.rows(sound))
        {
            switch (r.kind)
            {
                case cfevolution.edit.SoundEffectsPage.SECTION: col.addView(Ui.heading(this, r.label)); break;
                case cfevolution.edit.SoundEffectsPage.NOTE: col.addView(hint(r.label)); break;
                case cfevolution.edit.SoundEffectsPage.HEADINGS: break;
                default:
                {
                    boolean pair = r.second != null || r.fixed != null;
                    soundControl(p, r.first, r.label + (pair ? ", " + cfevolution.edit.SoundEffectsPage.MODULATOR.toLowerCase() : ""), r.second == null ? r.hint : null);
                    if (r.second != null)
                        soundControl(p, r.second, r.label + ", " + cfevolution.edit.SoundEffectsPage.CARRIER.toLowerCase(), r.hint);
                    else if (r.fixed != null)
                        col.addView(hint(r.label + ", " + cfevolution.edit.SoundEffectsPage.CARRIER.toLowerCase() + ": " + r.fixed));
                }
            }
        }
    }

    private void soundControl(final cfevolution.edit.SoundEffectsPage p, final cfevolution.edit.SoundEffectsPage.Control c,
                              final String label, String hint)
    {
        switch (c.kind)
        {
            case cfevolution.edit.SoundEffectsPage.FLAG:
                col.addView(tick(label, p.value(c) == 1, new Picked() {
                    public void picked(int on) { p.set(c, on); changed(); }
                }));
                break;
            case cfevolution.edit.SoundEffectsPage.CHOICE:
                col.addView(Ui.text(this, label, 14));
                col.addView(spinner(c.items, p.value(c), new Picked() {
                    public void picked(int index) { p.set(c, index); changed(); }
                }));
                break;
            default:
                item(label + ":  " + p.value(c), null, new Runnable() {
                    public void run()
                    {
                        form(label, new String[] { label + " (" + c.min + " to " + c.max + ")" }, new String[] { String.valueOf(p.value(c)) },
                             new boolean[] { true }, new Typed() {
                            public Message ok(String[] v)
                            {
                                Message m = p.set(c, v[0], label);
                                changed();
                                return m;
                            }
                        });
                    }
                });
        }
        if (hint != null)
            col.addView(hint(hint));
    }

    // --- the game options ----------------------------------------------------------

    private void options(final PreferencesPage p)
    {
        for (final PreferencesPage.Item i : p.items())
        {
            switch (i.kind)
            {
                case PreferencesPage.SECTION: col.addView(Ui.heading(this, i.label)); break;
                case PreferencesPage.NOTE: col.addView(hint(i.label)); break;
                case PreferencesPage.CHOICE:
                case PreferencesPage.FRAME_RATE:
                {
                    col.addView(Ui.text(this, i.label, 14));
                    col.addView(spinner(i.items.toArray(new String[0]), p.chosen(i), new Picked() {
                        public void picked(int index) { p.choose(i, index); changed(); build(); }
                    }));
                    if (i.hint != null)
                        col.addView(hint(i.hint));
                    if (i.kind == PreferencesPage.FRAME_RATE && p.frameRateWarning())
                    {
                        TextView w = Ui.text(this, PreferencesPage.FRAME_RATE_WARNING, 13);
                        w.setTextColor(Color.rgb(0xB0, 0x4A, 0x00));
                        col.addView(w);
                    }
                    break;
                }
                case PreferencesPage.NUMBER:
                    item(i.label + ":  " + p.number(i), i.hint, new Runnable() {
                        public void run()
                        {
                            form(i.label, new String[] { i.label + " (" + i.min + " to " + i.max + ")" }, new String[] { String.valueOf(p.number(i)) },
                                 new boolean[] { true }, new Typed() {
                                public Message ok(String[] v)
                                {
                                    Message m = p.setNumber(i, v[0]);
                                    changed();
                                    return m;
                                }
                            });
                        }
                    });
                    break;
                case PreferencesPage.AUTO_LOAD:
                    col.addView(tick(PreferencesPage.LOAD + " " + i.label.toLowerCase(), p.on(i), new Picked() {
                        public void picked(int on) { p.setOn(i, on == 1); changed(); }
                    }));
                    item(p.path(i), null, new Runnable() {
                        public void run()
                        {
                            form(i.label, new String[] { i.label }, new String[] { p.path(i) }, new boolean[1], new Typed() {
                                public Message ok(String[] v)
                                {
                                    Message m = p.setPath(i, v[0]);
                                    changed();
                                    return m;
                                }
                            });
                        }
                    });
                    break;
                default:
                    col.addView(tick(i.label, p.on(i), new Picked() {
                        public void picked(int on) { p.setOn(i, on == 1); changed(); }
                    }));
            }
        }
    }

    // --- saving -----------------------------------------------------------------------

    private void save(final Runnable after)
    {
        Message m = page.save(file);
        if (m != null)
        {
            Dialogs.show(this, m, null);
            return;
        }
        modified = false;
        build();
        if (Session.gameFileUri != null)
        {
            try
            {
                Documents.write(this, Session.gameFileUri, java.nio.file.Files.readAllBytes(file.toPath()));
            }
            catch (java.io.IOException | RuntimeException e)
            {
                Dialogs.show(this, Message.error("Save Failed", file.getName() + " could not be saved."), null);
                return;
            }
            if (after != null)
                after.run();
            return;
        }
        Session.writeBack(this, after);
    }

    public void onBackPressed()
    {
        if (!modified)
        {
            super.onBackPressed();
            return;
        }
        new AlertDialog.Builder(this).setTitle("Unsaved Changes")
            .setMessage(file.getName() + " has changes that have not been saved.")
            .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int which)
                {
                    save(new Runnable() { public void run() { finish(); } });
                }
            })
            .setNeutralButton("Don't Save", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int which) { finish(); }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
