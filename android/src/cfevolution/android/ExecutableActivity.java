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
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import cfevolution.edit.Message;
import cfevolution.screens.ExecutableScreen;

/**
 * GP.EXE: the Drivers, Teams, Points and Pit Lane pages. Tap a row to change
 * it; the rules and messages are the desktop editor's own (ExecutableScreen).
 */
public class ExecutableActivity extends Activity
{
    private ExecutableScreen screen;
    private LinearLayout page;
    private int current;
    private int selectedBox = -1;

    protected void onCreate(Bundle state)
    {
        super.onCreate(state);
        screen = Session.executable;
        if (screen == null)
        {
            finish();
            return;
        }
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout tabs = new LinearLayout(this);
        for (int i = 0; i < ExecutableScreen.PAGES.length; i++)
        {
            final int n = i;
            tabs.addView(Ui.button(this, ExecutableScreen.PAGES[i], new View.OnClickListener() {
                public void onClick(View v) { show(n); }
            }));
        }
        tabs.addView(Ui.button(this, "Save", new View.OnClickListener() {
            public void onClick(View v) { save(null); }
        }));
        HorizontalScrollView bar = new HorizontalScrollView(this);
        bar.addView(tabs);
        col.addView(bar);
        page = Ui.column(this);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(page);
        col.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(col);
        show(0);
    }

    private void show(int n)
    {
        current = n;
        setTitle(screen.title() + (screen.isModified() ? " *" : ""));
        page.removeAllViews();
        page.addView(Ui.heading(this, ExecutableScreen.PAGES[n]));
        page.addView(Ui.text(this, ExecutableScreen.PAGE_TEXT[n], 13));
        switch (n)
        {
            case 0: drivers(); break;
            case 1: teams(); break;
            case 2: points(); break;
            default: pitLane(); break;
        }
    }

    private void again()
    {
        show(current);
    }

    private TextView row(String text, final Runnable onTap)
    {
        TextView t = Ui.text(this, text, 15);
        int p = Ui.dp(this, 8);
        t.setPadding(0, p, 0, p);
        t.setBackgroundResource(android.R.drawable.list_selector_background);
        t.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { onTap.run(); } });
        page.addView(t);
        return t;
    }

    /** A form of text boxes; OK applies each change in turn and stops at the first refused. */
    private interface Apply
    {
        Message apply(String[] values);
    }

    private void form(String title, String[] labels, String[] values, boolean[] numbers, final Apply apply)
    {
        LinearLayout f = Ui.column(this);
        final EditText[] boxes = new EditText[labels.length];
        for (int i = 0; i < labels.length; i++)
            boxes[i] = Ui.field(f, labels[i], values[i], numbers[i]);
        ScrollView s = new ScrollView(this);
        s.addView(f);
        new AlertDialog.Builder(this).setTitle(title).setView(s)
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int which)
                {
                    String[] typed = new String[boxes.length];
                    for (int i = 0; i < boxes.length; i++)
                        typed[i] = boxes[i].getText().toString();
                    Dialogs.show(ExecutableActivity.this, apply.apply(typed), null);
                    again();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    // --- Drivers ------------------------------------------------------------

    private void drivers()
    {
        for (int i = 0; i < 40; i++)
        {
            final int car = i;
            row((car + 1) + "  " + screen.driverName(car) + "    qualifying " + screen.skill(car, false)
                + ", race " + screen.skill(car, true), new Runnable() {
                public void run()
                {
                    form("Car " + (car + 1), new String[] { "Driver", "Qualifying skill", "Race skill" },
                        new String[] { screen.driverName(car), String.valueOf(screen.skill(car, false)), String.valueOf(screen.skill(car, true)) },
                        new boolean[] { false, true, true }, new Apply() {
                            public Message apply(String[] v)
                            {
                                Message m = screen.setDriverName(car, v[0]);
                                if (m == null) m = screen.setSkill(car, false, v[1]);
                                if (m == null) m = screen.setSkill(car, true, v[2]);
                                return m;
                            }
                        });
                }
            });
        }
    }

    // --- Teams --------------------------------------------------------------

    private void teams()
    {
        for (int i = 0; i < 20; i++)
        {
            final int team = i;
            row((team + 1) + "  " + screen.teamName(team) + " (" + screen.engineName(team) + ", " + screen.power(team)
                + " BHP)\n     " + screen.carLabel(screen.teamCar(team, 0)) + "  /  " + screen.carLabel(screen.teamCar(team, 1)),
                new Runnable() {
                    public void run()
                    {
                        form("Team " + (team + 1), new String[] { "Team", "Engine", "Engine power (BHP)", "First car (0 for none)", "Second car (0 for none)" },
                            new String[] { screen.teamName(team), screen.engineName(team), String.valueOf(screen.power(team)),
                                           String.valueOf(screen.teamCar(team, 0)), String.valueOf(screen.teamCar(team, 1)) },
                            new boolean[] { false, false, true, true, true }, new Apply() {
                                public Message apply(String[] v)
                                {
                                    Message m = screen.setTeamName(team, v[0]);
                                    if (m == null) m = screen.setEngineName(team, v[1]);
                                    if (m == null) m = screen.setPower(team, v[2]);
                                    if (m == null) m = car(team, 0, v[3]);
                                    if (m == null) m = car(team, 1, v[4]);
                                    return m;
                                }
                            });
                    }
                });
        }
    }

    private Message car(int team, int seat, String typed)
    {
        int car = cfevolution.edit.ExecutableEdits.carNumber(typed);
        if (car < 0)
            return cfevolution.edit.ExecutableEdits.numberProblem("A car number", 0, 40);
        return screen.setTeamCar(team, seat, car);
    }

    // --- Points -------------------------------------------------------------

    private void points()
    {
        for (int i = 0; i < 26; i++)
        {
            final int r = i;
            row(screen.position(r) + "    " + screen.points(r) + " points", new Runnable() {
                public void run()
                {
                    form(screen.position(r) + " place", new String[] { "Points" }, new String[] { String.valueOf(screen.points(r)) },
                        new boolean[] { true }, new Apply() {
                            public Message apply(String[] v) { return screen.setPoints(r, v[0]); }
                        });
                }
            });
        }
    }

    // --- Pit lane -----------------------------------------------------------

    private void pitLane()
    {
        String warning = screen.pitWarning();
        if (warning != null)
        {
            TextView w = Ui.text(this, warning, 14);
            w.setTextColor(Color.RED);
            page.addView(w);
        }
        LinearLayout buttons = new LinearLayout(this);
        buttons.addView(Ui.button(this, "Move Up", new View.OnClickListener() {
            public void onClick(View v) { if (selectedBox > 0) { selectedBox = screen.movePitBox(selectedBox, true); again(); } }
        }));
        buttons.addView(Ui.button(this, "Move Down", new View.OnClickListener() {
            public void onClick(View v) { if (selectedBox >= 0) { selectedBox = screen.movePitBox(selectedBox, false); again(); } }
        }));
        buttons.addView(Ui.button(this, "Reset to 1991 Order", new View.OnClickListener() {
            public void onClick(View v) { screen.resetPitOrder(); again(); }
        }));
        page.addView(buttons);
        for (int i = 0; i < ExecutableScreen.PIT_BOXES; i++)
        {
            final int box = i;
            TextView t = row(screen.pitBoxLabel(box) + "    " + screen.teamLabel(screen.pitBoxTeam(box)), new Runnable() {
                public void run()
                {
                    if (selectedBox == box)
                        chooseTeam(box);
                    else
                    {
                        selectedBox = box;
                        again();
                    }
                }
            });
            if (box == selectedBox)
                t.setBackgroundColor(Color.rgb(0xD0, 0xE4, 0xFF));
        }
        page.addView(Ui.text(this, "Tap a box to select it, and tap it again to choose its team.", 13));
    }

    private void chooseTeam(final int box)
    {
        String[] teams = new String[18];
        for (int t = 1; t <= 18; t++)
            teams[t - 1] = screen.teamLabel(t);
        new AlertDialog.Builder(this).setTitle(screen.pitBoxLabel(box))
            .setItems(teams, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int which) { screen.setPitBoxTeam(box, which + 1); again(); }
            })
            .show();
    }

    // --- Saving -------------------------------------------------------------

    private void save(final Runnable after)
    {
        Message m = screen.save();
        again();
        if (m != null)
        {
            Dialogs.show(this, m, null);
            return;
        }
        Session.writeBack(this, new Runnable() {
            public void run()
            {
                android.widget.Toast.makeText(ExecutableActivity.this, "Saved GP.EXE.", android.widget.Toast.LENGTH_SHORT).show();
                if (after != null)
                    after.run();
            }
        });
    }

    public void onBackPressed()
    {
        if (!screen.isModified())
        {
            super.onBackPressed();
            return;
        }
        new AlertDialog.Builder(this).setTitle("Unsaved Changes")
            .setMessage("GP.EXE has changes that have not been saved.")
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
