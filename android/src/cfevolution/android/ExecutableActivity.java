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
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import cfevolution.edit.GameSettingsPage;
import cfevolution.edit.LapRecordsPage;
import cfevolution.edit.LiveriesPage;
import cfevolution.edit.Message;
import cfevolution.edit.SetupsPage;
import cfevolution.edit.TablePage;
import cfevolution.screens.ExecutableScreen;

/**
 * GP.EXE: every page of the desktop's executable tab. Tap a row to change
 * it; the rules and messages are the desktop editor's own (ExecutableScreen).
 */
public class ExecutableActivity extends Activity
{
    private ExecutableScreen screen;
    private LinearLayout page;
    private int current;
    private int selectedBox = -1;
    private int setupTab;
    private int[] copiedSetup;
    private String copiedText;

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
        tabs.addView(Ui.button(this, "Revert File", new View.OnClickListener() {
            public void onClick(View v) { revertFile(); }
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
        CrashReport.install(this);
        try
        {
            build(n);
        }
        catch (RuntimeException e)
        {
            // a page that cannot be built says why, rather than stopping the app (2026-11-21)
            page.removeAllViews();
            page.addView(Ui.heading(this, ExecutableScreen.PAGES[n]));
            TextView t = Ui.text(this, "This page could not be shown. Please send this to the developer:\n\n"
                + CrashReport.describe(this, e), 12);
            t.setTextIsSelectable(true);
            page.addView(t);
        }
    }

    private void build(int n)
    {
        current = n;
        setTitle(screen.title() + (screen.isModified() ? " *" : ""));
        page.removeAllViews();
        page.addView(Ui.heading(this, ExecutableScreen.PAGES[n]));
        page.addView(Ui.text(this, ExecutableScreen.PAGE_TEXT[n], 13));
        String name = ExecutableScreen.PAGES[n];
        if (name.equals("Drivers"))
            drivers();
        else if (name.equals("Teams"))
            teams();
        else if (name.equals("Points"))
            points();
        else if (name.equals(LiveriesPage.TITLE))
            liveries();
        else if (name.equals(GameSettingsPage.TITLE))
            settings();
        else if (name.equals(SetupsPage.TITLE))
            setups();
        else if (name.equals(LapRecordsPage.TITLE))
            lapRecords();
        else if (name.equals("Car Physics"))
            tables(screen.physics());
        else if (name.equals("Opposition Levels"))
            tables(screen.opposition());
        else
            pitLane();
        // Revert Page (Hrvoje, the owner, 2026-11-23): this page, or the Liveries tab shown
        Button revert = Ui.button(this, "Revert This Page", new View.OnClickListener() {
            public void onClick(View v) { revertPage(); }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(this, 20);
        page.addView(revert, lp);
    }

    private void revertPage()
    {
        final int n = current, tab = liveryTab;
        Session.choose(this, screen.revertPageQuestion(n, tab), new Runnable() {
            public void run()
            {
                Message m = screen.revertPage(n, tab);
                again();
                if (m != null)
                    Dialogs.show(ExecutableActivity.this, m, null);
            }
        });
    }

    private void revertFile()
    {
        Session.revertFile(this, screen.file, new Runnable() {
            public void run()
            {
                Message[] problem = new Message[1];
                ExecutableScreen e = Session.folder == null ? null : Session.folder.openExecutable(problem);
                if (e == null)
                {
                    finish();
                    return;
                }
                Session.executable = e;
                recreate();
            }
        });
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
        form(title, labels, values, numbers, new String[labels.length][], apply);
    }

    /** As above; a field with choices is a drop-down, its value the chosen place in the list. */
    private void form(String title, String[] labels, String[] values, boolean[] numbers, String[][] choices, final Apply apply)
    {
        LinearLayout f = Ui.column(this);
        final EditText[] boxes = new EditText[labels.length];
        final Spinner[] lists = new Spinner[labels.length];
        for (int i = 0; i < labels.length; i++)
            if (choices[i] == null)
                boxes[i] = Ui.field(f, labels[i], values[i], numbers[i]);
            else
            {
                TextView label = Ui.text(this, labels[i], 14);
                label.setTextColor(Color.GRAY);
                f.addView(label);
                lists[i] = new Spinner(this);
                ArrayAdapter<String> a = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, choices[i]);
                a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                lists[i].setAdapter(a);
                lists[i].setSelection(Integer.parseInt(values[i]));
                f.addView(lists[i]);
            }
        ScrollView s = new ScrollView(this);
        s.addView(f);
        new AlertDialog.Builder(this).setTitle(title).setView(s)
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int which)
                {
                    String[] typed = new String[boxes.length];
                    for (int i = 0; i < boxes.length; i++)
                        typed[i] = lists[i] != null ? String.valueOf(lists[i].getSelectedItemPosition()) : boxes[i].getText().toString();
                    Dialogs.show(ExecutableActivity.this, apply.apply(typed), null);
                    again();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    // --- Drivers ------------------------------------------------------------

    /** The driver or team selected on the Drivers or Teams page; a second tap edits it. */
    private int selectedDriver = -1, selectedTeam = -1;

    /** Move Up, Move Down and Swap With... for the Drivers or Teams page (2026-11-23). */
    private void moveButtons(final boolean teams)
    {
        LinearLayout buttons = new LinearLayout(this);
        buttons.addView(Ui.button(this, "Move Up", new View.OnClickListener() {
            public void onClick(View v) { move(teams, true); }
        }));
        buttons.addView(Ui.button(this, "Move Down", new View.OnClickListener() {
            public void onClick(View v) { move(teams, false); }
        }));
        buttons.addView(Ui.button(this, "Swap With...", new View.OnClickListener() {
            public void onClick(View v) { swapWith(teams); }
        }));
        page.addView(buttons);
        TextView tip = Ui.text(this, (teams ? cfevolution.edit.LineUpMoves.TEAM_MOVES_TIP : cfevolution.edit.LineUpMoves.DRIVER_MOVES_TIP)
            + " Tap a " + (teams ? "team" : "driver") + " to select it, and tap it again to change it.", 13);
        tip.setTextColor(Color.GRAY);
        page.addView(tip);
    }

    private void move(boolean teams, boolean up)
    {
        int r = teams ? selectedTeam : selectedDriver;
        if (r < 0)
        {
            Dialogs.show(this, Message.info("Nothing Moved", "Select a " + (teams ? "team" : "driver") + " first."), null);
            return;
        }
        if (teams)
            selectedTeam = screen.moveTeam(r, up);
        else
            selectedDriver = screen.moveDriver(r, up);
        again();
    }

    private void swapWith(final boolean teams)
    {
        final int r = teams ? selectedTeam : selectedDriver;
        if (r < 0)
        {
            Dialogs.show(this, Message.info("Nothing Moved", "Select a " + (teams ? "team" : "driver") + " first."), null);
            return;
        }
        final String[] choices = teams ? screen.teamChoices() : screen.driverChoices();
        new AlertDialog.Builder(this).setTitle("Swap " + choices[r].trim() + " with")
            .setItems(choices, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int which)
                {
                    if (teams) { screen.swapTeams(r, which); selectedTeam = which; }
                    else { screen.swapDrivers(r, which); selectedDriver = which; }
                    again();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void drivers()
    {
        moveButtons(false);
        for (int i = 0; i < 40; i++)
        {
            final int car = i;
            TextView t = row((car + 1) + "  " + (screen.driverName(car).trim().length() > 0 ? screen.driverName(car) : "(no name)"), new Runnable() {
                public void run()
                {
                    if (selectedDriver != car)
                    {
                        selectedDriver = car;
                        again();
                        return;
                    }
                    form("Car " + (car + 1), new String[] { "Driver" }, new String[] { screen.driverName(car) },
                        new boolean[] { false }, new Apply() {
                            public Message apply(String[] v) { return screen.setDriverName(car, v[0]); }
                        });
                }
            });
            if (car == selectedDriver)
                t.setBackgroundColor(Color.rgb(0xD0, 0xE4, 0xFF));
        }
    }

    // --- Teams --------------------------------------------------------------

    private String seatText(int team, int seat)
    {
        int car = screen.teamCar(team, seat);
        return screen.carLabel(car) + (car == 0 ? "" : " (" + screen.seatSkill(team, seat, false) + ", " + screen.seatSkill(team, seat, true) + ")");
    }

    private void teams()
    {
        moveButtons(true);
        for (int i = 0; i < 20; i++)
        {
            final int team = i;
            TextView t = row((team + 1) + "  " + screen.teamName(team) + " (" + screen.engineName(team) + ", " + screen.power(team)
                + " BHP)\n     " + seatText(team, 0) + "  /  " + seatText(team, 1),
                new Runnable() {
                    public void run()
                    {
                        if (selectedTeam != team)
                        {
                            selectedTeam = team;
                            again();
                            return;
                        }
                        editTeam(team);
                    }
                });
            if (team == selectedTeam)
                t.setBackgroundColor(Color.rgb(0xD0, 0xE4, 0xFF));
        }
        TextView note = Ui.text(this, "Skill is shown after each driver: qualifying, then race.", 13);
        note.setTextColor(Color.GRAY);
        page.addView(note);
    }

    private String skillText(int team, int seat, boolean race)
    {
        int skill = screen.seatSkill(team, seat, race);
        return skill < 0 ? "" : String.valueOf(skill);
    }

    private void editTeam(final int team)
    {
        String[] cars = new String[41];             // as the desktop's list: None, then 1-40 with the driver
        for (int n = 0; n <= 40; n++)
            cars[n] = screen.carLabel(n);
        form("Team " + (team + 1), new String[] { "Team", "Engine", "Engine power (BHP)",
                "First car", "Qualifying skill", "Race skill", "Second car", "Qualifying skill", "Race skill" },
            new String[] { screen.teamName(team), screen.engineName(team), String.valueOf(screen.power(team)),
                           String.valueOf(screen.teamCar(team, 0)), skillText(team, 0, false), skillText(team, 0, true),
                           String.valueOf(screen.teamCar(team, 1)), skillText(team, 1, false), skillText(team, 1, true) },
            new boolean[] { false, false, true, true, true, true, true, true, true },
            new String[][] { null, null, null, cars, null, null, cars, null, null }, new Apply() {
                public Message apply(String[] v)
                {
                    Message m = screen.setTeamName(team, v[0]);
                    if (m == null) m = screen.setEngineName(team, v[1]);
                    if (m == null) m = screen.setPower(team, v[2]);
                    if (m == null) m = skills(team, 0, v[4], v[5]);
                    if (m == null) m = skills(team, 1, v[7], v[8]);
                    if (m == null)
                        seats(team, new int[] { Integer.parseInt(v[3]), Integer.parseInt(v[6]) }, 0);
                    return m;
                }
            });
    }

    /** The skill typed beside a seat (before its driver changes: the seat keeps it). */
    private Message skills(int team, int seat, String qualifying, String race)
    {
        if (screen.teamCar(team, seat) == 0)
            return null;
        Message m = qualifying.equals(skillText(team, seat, false)) ? null : screen.setSeatSkill(team, seat, false, qualifying);
        if (m == null && !race.equals(skillText(team, seat, true)))
            m = screen.setSeatSkill(team, seat, true, race);
        return m;
    }

    /** The cars chosen for the seats, one at a time: one already in another seat is asked about first. */
    private void seats(final int team, final int[] cars, final int seat)
    {
        if (seat >= cars.length)
        {
            again();
            return;
        }
        final Message question = screen.seatQuestion(team, seat, cars[seat]);
        if (question == null)
        {
            screen.placeDriver(team, seat, cars[seat], null);
            seats(team, cars, seat + 1);
            return;
        }
        final String[] how = { cfevolution.edit.LineUpMoves.CANCEL };
        AlertDialog.Builder b = new AlertDialog.Builder(this).setTitle(question.title).setMessage(question.text).setCancelable(false);
        DialogInterface.OnClickListener[] pick = new DialogInterface.OnClickListener[question.options.length];
        for (int i = 0; i < pick.length; i++)
        {
            final String option = question.options[i];
            pick[i] = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int which) { how[0] = option; }
            };
        }
        b.setPositiveButton(question.options[0], pick[0]);
        if (pick.length == 2)
            b.setNegativeButton(question.options[1], pick[1]);
        else
        {
            b.setNeutralButton(question.options[1], pick[1]);
            b.setNegativeButton(question.options[2], pick[2]);
        }
        AlertDialog d = b.create();
        d.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface di)
            {
                screen.placeDriver(team, seat, cars[seat], how[0]);
                seats(team, cars, seat + 1);
            }
        });
        d.show();
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

    // --- Game Settings -------------------------------------------------------

    private void settings()
    {
        for (GameSettingsPage.Group g : screen.settings())
        {
            TextView title = Ui.text(this, g.title, 16);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            title.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 4));
            page.addView(title);
            for (final GameSettingsPage.Item it : g.items)
            {
                if (it.kind == GameSettingsPage.Kind.NUMBER)
                {
                    row(it.label + "    " + it.shown(), new Runnable() {
                        public void run()
                        {
                            form(g(it.label), new String[] { it.label }, new String[] { String.valueOf(it.shown()) },
                                new boolean[] { true }, new Apply() {
                                    public Message apply(String[] v) { return screen.setSetting(it, v[0]); }
                                });
                        }
                    });
                    TextView hint = Ui.text(this, it.hint, 12);
                    hint.setTextColor(Color.GRAY);
                    page.addView(hint);
                }
                else if (it.kind == GameSettingsPage.Kind.SWITCH)
                {
                    CheckBox box = new CheckBox(this);
                    box.setText(it.label);
                    box.setTextSize(15);
                    box.setChecked(it.isOn());
                    box.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        public void onCheckedChanged(CompoundButton b, boolean on)
                        {
                            if (on != it.isOn())
                            {
                                screen.toggle(it);
                                setTitle(screen.title() + " *");
                            }
                        }
                    });
                    page.addView(box);
                }
                else
                {
                    TextView note = Ui.text(this, it.label, 13);
                    note.setTextColor(Color.GRAY);
                    page.addView(note);
                }
            }
        }
    }

    /** A dialog title: the label without its unit in brackets. */
    private static String g(String label)
    {
        int i = label.lastIndexOf(" (");
        return i > 0 && label.endsWith(")") ? label.substring(0, i) : label;
    }

    // --- Setups ------------------------------------------------------------

    private void setups()
    {
        final SetupsPage p = screen.setups();
        if (!p.isTabUsed(setupTab))
            setupTab = SetupsPage.QUALIFYING;
        CheckBox separate = new CheckBox(this);
        separate.setText(SetupsPage.SEPARATE);
        separate.setChecked(p.isSeparate());
        separate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean on) { screen.toggleSeparate(); again(); }
        });
        page.addView(separate);
        TextView tip = Ui.text(this, SetupsPage.SEPARATE_TIP, 12);
        tip.setTextColor(Color.GRAY);
        page.addView(tip);

        LinearLayout tabs = new LinearLayout(this);
        for (int t = 0; t < 3; t++)
        {
            final int tab = t;
            Button b = Ui.button(this, p.tabTitle(t), new View.OnClickListener() {
                public void onClick(View v) { setupTab = tab; again(); }
            });
            b.setEnabled(p.isTabUsed(t));
            if (t == setupTab)
                b.setTypeface(Typeface.DEFAULT_BOLD);
            tabs.addView(b);
        }
        HorizontalScrollView tabBar = new HorizontalScrollView(this);
        tabBar.addView(tabs);
        page.addView(tabBar);

        if (p.isSeparate())
        {
            LinearLayout between = new LinearLayout(this);
            for (final boolean toRace : new boolean[] { true, false })
                between.addView(Ui.button(this, toRace ? SetupsPage.TO_RACE : SetupsPage.TO_QUALIFYING, new View.OnClickListener() {
                    public void onClick(View v)
                    {
                        Message q = SetupsPage.betweenQuestion(toRace);
                        Dialogs.ask(ExecutableActivity.this, q.title, q.text, q.options[0], q.options[1], new Runnable() {
                            public void run() { screen.copySetupsBetween(toRace); again(); }
                        });
                    }
                }));
            HorizontalScrollView bar = new HorizontalScrollView(this);
            bar.addView(between);
            page.addView(bar);
        }
        TextView how = Ui.text(this, "Tap a row to change its setup. Touch and hold a row to copy its setup, paste a copied one, "
            + "or give every row on this tab its setup." + (copiedText != null ? " " + copiedText + "." : ""), 13);
        how.setTextColor(Color.GRAY);
        page.addView(how);

        for (int i = 0; i < p.rows(setupTab); i++)
        {
            final int r = i;
            final int tab = setupTab;
            StringBuilder line = new StringBuilder(p.rowLabel(tab, r));
            line.append("\n     wings ").append(p.value(tab, r, 0)).append(" / ").append(p.value(tab, r, 1)).append(",  gears");
            for (int c = 2; c < 8; c++)
                line.append(" ").append(p.value(tab, r, c));
            line.append(",  tyres ").append(p.value(tab, r, 8)).append(",  brake balance ").append(p.value(tab, r, 9));
            TextView row = row(line.toString(), new Runnable() {
                public void run()
                {
                    String[] values = new String[SetupsPage.COLUMNS.length];
                    boolean[] numbers = new boolean[values.length];
                    for (int c = 0; c < values.length; c++)
                    {
                        values[c] = p.value(tab, r, c);
                        numbers[c] = c != 8;
                    }
                    form(p.tabTitle(tab) + ": " + p.rowName(tab, r), SetupsPage.COLUMNS, values, numbers, new Apply() {
                        public Message apply(String[] v) { return screen.setSetupRow(tab, r, v); }
                    });
                }
            });
            row.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v)
                {
                    setupMenu(tab, r);
                    return true;
                }
            });
        }
    }

    private void setupMenu(final int tab, final int r)
    {
        final SetupsPage p = screen.setups();
        final String[] items = copiedSetup == null
            ? new String[] { "Copy", SetupsPage.applyAllLabel(tab) }
            : new String[] { "Copy", "Paste", SetupsPage.applyAllLabel(tab) };
        new AlertDialog.Builder(this).setTitle(p.rowName(tab, r))
            .setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int which)
                {
                    String chosen = items[which];
                    if (chosen.equals("Copy"))
                    {
                        copiedSetup = p.read(tab, r);
                        copiedText = p.copiedText(tab, r);
                    }
                    else if (chosen.equals("Paste"))
                        screen.writeSetups(tab, new int[] { r }, copiedSetup);
                    else
                        screen.applySetupToAll(tab, r);
                    again();
                }
            })
            .show();
    }

    // --- Liveries ------------------------------------------------------------

    private int liveryTab;
    private final int[] liveryRow = new int[3];
    private final int[] liveryPosition = { LiveriesPage.startPosition(0), LiveriesPage.startPosition(1), LiveriesPage.startPosition(2) };
    /** The part last tapped on each tab, shown highlighted; helmets by team; unused teams hidden (Hrvoje, 2026-11-15/16). */
    private final int[] liveryPart = { -1, -1, -1 };
    private boolean helmetsByTeam;
    private final boolean[] hideUnusedTeams = new boolean[3];
    private int fileTab, fileRow;
    private static final int IMPORT_FILE = 41, EXPORT_FILE = 42;

    private void liveries()
    {
        final LiveriesPage p = screen.liveries();
        final int tab = liveryTab;
        LinearLayout tabs = new LinearLayout(this);
        for (int t = 0; t < LiveriesPage.TAB_TITLES.length; t++)
        {
            final int n = t;
            Button b = Ui.button(this, LiveriesPage.TAB_TITLES[t], new View.OnClickListener() {
                public void onClick(View v) { liveryTab = n; again(); }
            });
            if (t == tab)
                b.setTypeface(Typeface.DEFAULT_BOLD);
            tabs.addView(b);
        }
        page.addView(tabs);

        final int row = liveryRow[tab];
        TextView title = Ui.heading(this, p.rowName(tab, row));
        page.addView(title);
        if (screen.sprites() == null)
        {
            TextView none = Ui.text(this, "The game's picture files (F1GPDATA.DAT and F1GPDATB.DAT) are not in this game folder, "
                + "so the pictures cannot be shown. The colours can still be changed below.", 13);
            none.setTextColor(Color.GRAY);
            page.addView(none);
        }
        else
            try
            {
                liveryPicture(p, tab, row);
            }
            catch (RuntimeException e)
            {
                // the picture alone fails: the colours below still work, and the error can be sent on
                TextView t = Ui.text(this, "The picture could not be shown. Please send this to the developer:\n\n"
                    + CrashReport.describe(this, e), 12);
                t.setTextIsSelectable(true);
                page.addView(t);
            }

        // the palette, always shown (Hrvoje, 2026-11-21): it colours the part selected below or on the picture
        // (-1 until a part is tapped: the first part is then the one selected, 2026-11-22; -1 used as a part stopped the page)
        if (liveryPart[tab] < 0 || liveryPart[tab] >= LiveriesPage.parts(tab).length)
            liveryPart[tab] = 0;
        final int selectedPart = liveryPart[tab];
        TextView selected = Ui.text(this, LiveriesPage.parts(tab)[selectedPart] + ": " + LiveriesPage.about(p.get(tab, row, selectedPart))
            + "\nTap a colour to give it to this part.", 13);
        selected.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 4));
        page.addView(selected);
        page.addView(new PaletteView(this, p.liveries(), p.get(tab, row, selectedPart), new PaletteView.Picked() {
            public void colour(int index) { screen.setLiveryColour(tab, row, selectedPart, index); again(); }
        }));

        // the selected row's parts: tap one to select it for the palette
        for (int i = 0; i < LiveriesPage.parts(tab).length; i++)
        {
            final int part = i;
            LinearLayout line = new LinearLayout(this);
            line.setGravity(Gravity.CENTER_VERTICAL);
            int pad = Ui.dp(this, 6);
            line.setPadding(0, pad, 0, pad);
            line.setBackgroundResource(android.R.drawable.list_selector_background);
            if (part == liveryPart[tab])
                line.setBackgroundColor(0x3326A6FF);     // the part last tapped, here or on the picture
            line.addView(swatch(p.liveries().getPaletteRgb(p.get(tab, row, part)), 28));
            TextView name = Ui.text(this, "  " + LiveriesPage.parts(tab)[part] + "  (colour " + p.get(tab, row, part) + ")", 15);
            line.addView(name);
            line.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { liveryPart[tab] = part; again(); }
            });
            line.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) { colourMenu(tab, row, part); return true; }
            });
            page.addView(line);
        }

        String copied = screen.copiedText(tab);
        TextView how = Ui.text(this, "Tap a " + LiveriesPage.rowWord(tab) + " below to show it above. Touch and hold one to copy its "
            + LiveriesPage.what(tab) + ", paste copied ones" + (LiveriesPage.hasFile(tab) ? ", or use ArgEditor's ." + LiveriesPage.kind(tab) + " file" : "")
            + ". Touch and hold a part above to copy or paste one colour, or for the large chooser." + (copied != null ? " " + copied + "." : ""), 13);
        how.setTextColor(Color.GRAY);
        how.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 4));
        page.addView(how);

        // the helmets' order, or hiding the teams with no drivers
        if (tab == LiveriesPage.HELMETS)
            page.addView(Ui.button(this, "Order: " + LiveriesPage.ORDERS[helmetsByTeam ? 1 : 0], new View.OnClickListener() {
                public void onClick(View v) { helmetsByTeam = !helmetsByTeam; again(); }
            }));
        else
            page.addView(Ui.button(this, hideUnusedTeams[tab] ? "Show All Teams" : LiveriesPage.HIDE_UNUSED, new View.OnClickListener() {
                public void onClick(View v)
                {
                    if (hideUnusedTeams[tab] || p.order(tab, false, true).length > 0)
                        hideUnusedTeams[tab] = !hideUnusedTeams[tab];
                    again();
                }
            }));

        for (final int r : p.order(tab, helmetsByTeam, hideUnusedTeams[tab]))
        {
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.VERTICAL);
            int pad = Ui.dp(this, 6);
            line.setPadding(0, pad, 0, pad);
            line.setBackgroundResource(android.R.drawable.list_selector_background);
            TextView name = Ui.text(this, p.rowName(tab, r, helmetsByTeam), 15);
            if (r == row)
                name.setTypeface(Typeface.DEFAULT_BOLD);
            line.addView(name);
            LinearLayout colours = new LinearLayout(this);
            for (int part = 0; part < LiveriesPage.parts(tab).length; part++)
                colours.addView(swatch(p.liveries().getPaletteRgb(p.get(tab, r, part)), 16));
            line.addView(colours);
            line.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { liveryRow[tab] = r; again(); }
            });
            line.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) { liveryMenu(tab, r); return true; }
            });
            page.addView(line);
        }
    }

    private View swatch(int rgb, int size)
    {
        View v = new View(this);
        v.setBackgroundColor(0xFF000000 | rgb);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(this, size), Ui.dp(this, size));
        lp.setMargins(0, 0, Ui.dp(this, 2), 0);
        v.setLayoutParams(lp);
        return v;
    }

    /** The Cars tab's car (Hrvoje, 2026-11-21; one view since 11-25): its heading, and team slot 1's design shown. */
    private int closeUpHeading = LiveriesPage.START_HEADING;
    private boolean usualDesign;

    private void liveryPicture(final LiveriesPage p, final int tab, final int row)
    {
        if (tab == LiveriesPage.CARS && screen.carModel() != null)
        {
            TextView about = Ui.text(this, LiveriesPage.CAR_TIP + ".", 13);
            about.setTextColor(Color.GRAY);
            page.addView(about);
            if (row == 0)
            {
                TextView note = Ui.text(this, LiveriesPage.TEAM_ONE_NOTE, 13);
                note.setTextColor(0xFF8A5A00);
                page.addView(note);
                LinearLayout designs = new LinearLayout(this);
                for (final boolean usual : new boolean[] { false, true })
                {
                    Button b = Ui.button(this, usual ? LiveriesPage.USUAL_DESIGN : LiveriesPage.MCLAREN_DESIGN, new View.OnClickListener() {
                        public void onClick(View v) { usualDesign = usual; again(); }
                    });
                    if (usual == usualDesign)
                        b.setTypeface(Typeface.DEFAULT_BOLD);
                    designs.addView(b);
                }
                page.addView(designs);
            }
            closeUpPicture(p, row);
            return;
        }
        final LiveryView view = new LiveryView(this, p, screen.sprites(), new LiveryView.Tapped() {
            public void part(int part) { liveryPart[tab] = part; again(); }
        });
        view.show(tab, row, liveryPosition[tab]);
        page.addView(view);
        if (LiveriesPage.turns(tab))
        {
            LinearLayout turn = new LinearLayout(this);
            turn.setGravity(Gravity.CENTER_VERTICAL);
            turn.addView(Ui.text(this, LiveriesPage.TURN + "  ", 14));
            final android.widget.SeekBar bar = new android.widget.SeekBar(this);
            bar.setMax(LiveriesPage.positions(tab) - 1);
            bar.setProgress(liveryPosition[tab]);
            bar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(android.widget.SeekBar s, int v, boolean user)
                {
                    liveryPosition[tab] = v;
                    view.show(tab, row, v);
                }
                public void onStartTrackingTouch(android.widget.SeekBar s) { }
                public void onStopTrackingTouch(android.widget.SeekBar s) { }
            });
            turn.addView(bar, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            page.addView(turn);
            LinearLayout views = new LinearLayout(this);      // the usual views (Hrvoje, 2026-11-16)
            String[] names = LiveriesPage.views(tab);
            for (int i = 0; i < names.length; i++)
            {
                final int which = i;
                views.addView(Ui.button(this, names[i], new View.OnClickListener() {
                    public void onClick(View v) { bar.setProgress(LiveriesPage.viewPosition(tab, which)); }
                }));
            }
            page.addView(views);
        }
        else
        {
            LinearLayout step = new LinearLayout(this);
            step.setGravity(Gravity.CENTER_VERTICAL);
            final TextView number = Ui.text(this, LiveriesPage.pictureNumber(tab, liveryPosition[tab]), 14);
            number.setGravity(Gravity.CENTER);
            for (final int by : new int[] { -1, 1 })
            {
                Button b = Ui.button(this, by < 0 ? "◀" : "▶", new View.OnClickListener() {
                    public void onClick(View v)
                    {
                        int n = LiveriesPage.positions(tab);
                        liveryPosition[tab] = (liveryPosition[tab] + by + n) % n;
                        view.show(tab, row, liveryPosition[tab]);
                        number.setText(LiveriesPage.pictureNumber(tab, liveryPosition[tab]));
                    }
                });
                if (by > 0)
                    step.addView(number, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                step.addView(b);
            }
            page.addView(step);
        }
        TextView hint = Ui.text(this, "Tap part of the picture to select it, then a colour below.", 13);
        hint.setTextColor(Color.GRAY);
        page.addView(hint);
    }

    /** The car with a Turn bar and the Front, Side and Rear views. */
    private void closeUpPicture(final LiveriesPage p, final int row)
    {
        final int tab = LiveriesPage.CARS;
        final LiveryView view = new LiveryView(this, p, screen.sprites(), new LiveryView.Tapped() {
            public void part(int part) { liveryPart[tab] = part; again(); }
        });
        view.showCloseUp(row, screen.closeUp(row, closeUpHeading, usualDesign));
        page.addView(view);
        LinearLayout line = new LinearLayout(this);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.addView(Ui.text(this, LiveriesPage.TURN + "  ", 14));
        final android.widget.SeekBar bar = new android.widget.SeekBar(this);
        bar.setMax(LiveriesPage.HEADINGS - 1);
        bar.setProgress(closeUpHeading);
        bar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(android.widget.SeekBar s, int v, boolean user)
            {
                closeUpHeading = v;
                view.showCloseUp(row, screen.closeUp(row, closeUpHeading, usualDesign));
            }
            public void onStartTrackingTouch(android.widget.SeekBar s) { }
            public void onStopTrackingTouch(android.widget.SeekBar s) { }
        });
        line.addView(bar, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        page.addView(line);
        LinearLayout views = new LinearLayout(this);
        for (int i = 0; i < LiveriesPage.CLOSE_UP_VIEWS.length; i++)
        {
            final int which = i;
            views.addView(Ui.button(this, LiveriesPage.CLOSE_UP_VIEWS[i], new View.OnClickListener() {
                public void onClick(View v) { bar.setProgress(LiveriesPage.closeUpHeading(which)); }
            }));
        }
        page.addView(views);
        TextView hint = Ui.text(this, "Tap part of the picture to select it, then a colour below.", 13);
        hint.setTextColor(Color.GRAY);
        page.addView(hint);
    }

    /** The palette: tap a colour to see what it is, then OK to use it. */
    private void chooseColour(final int tab, final int row, final int part)
    {
        final LiveriesPage p = screen.liveries();
        liveryPart[tab] = part;
        LinearLayout box = Ui.column(this);
        TextView hint = Ui.text(this, "Tap a colour, then OK. A filled corner mark: the game changes that colour in a race, so avoid it. "
            + "An open mark: worth knowing. In the rain, colours 0 to 207 all turn greyer.", 13);
        hint.setTextColor(Color.GRAY);
        box.addView(hint);
        final TextView about = Ui.text(this, LiveriesPage.about(p.get(tab, row, part)), 14);
        final PaletteView palette = new PaletteView(this, p.liveries(), p.get(tab, row, part), new PaletteView.Picked() {
            public void colour(int index) { }
        });
        box.addView(palette);
        box.addView(about);
        palette.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, android.view.MotionEvent e)
            {
                palette.onTouchEvent(e);
                about.setText(LiveriesPage.about(palette.highlighted()));
                return true;
            }
        });
        ScrollView scroll = new ScrollView(this);
        scroll.addView(box);
        new AlertDialog.Builder(this).setTitle(p.chooserTitle(tab, row, part)).setView(scroll)
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w)
                {
                    screen.setLiveryColour(tab, row, part, palette.highlighted());
                    again();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    /** The large chooser with its notes, from the menu of a held part. */
    private static final String CHOOSE_COLOUR = "Choose Colour...";

    /** Touch and hold a part: Copy Colour, Paste Colour (Hrvoje, 2026-11-16), or the large chooser. */
    private void colourMenu(final int tab, final int row, final int part)
    {
        liveryPart[tab] = part;
        final String[] list = screen.copiedColour() >= 0
            ? new String[] { LiveriesPage.COPY_COLOUR, LiveriesPage.PASTE_COLOUR + " (colour " + screen.copiedColour() + ")", CHOOSE_COLOUR }
            : new String[] { LiveriesPage.COPY_COLOUR, CHOOSE_COLOUR };
        new AlertDialog.Builder(this).setTitle(screen.liveries().chooserTitle(tab, row, part))
            .setItems(list, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int which)
                {
                    if (list[which].equals(CHOOSE_COLOUR))
                    {
                        chooseColour(tab, row, part);
                        return;
                    }
                    if (which == 0)
                        screen.copyColour(tab, row, part);
                    else
                        screen.pasteColour(tab, row, part);
                    again();
                }
            })
            .show();
    }

    private void liveryMenu(final int tab, final int r)
    {
        final LiveriesPage p = screen.liveries();
        java.util.List<String> items = new java.util.ArrayList<String>();
        items.add("Copy");
        if (screen.copiedText(tab) != null)
            items.add("Paste");
        items.add(LiveriesPage.CLEAR);
        if (LiveriesPage.hasFile(tab))
        {
            items.add(LiveriesPage.importLabel(tab));
            items.add(LiveriesPage.exportLabel(tab));
        }
        final String[] list = items.toArray(new String[0]);
        new AlertDialog.Builder(this).setTitle(p.rowName(tab, r))
            .setItems(list, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int which)
                {
                    String chosen = list[which];
                    fileTab = tab;
                    fileRow = r;
                    if (chosen.equals("Copy"))
                        screen.copyDesign(tab, r);
                    else if (chosen.equals("Paste"))
                        screen.pasteDesign(tab, new int[] { r });
                    else if (chosen.equals(LiveriesPage.CLEAR))
                        screen.clearDesign(tab, new int[] { r });
                    else if (chosen.equals(LiveriesPage.importLabel(tab)))
                    {
                        android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT);
                        i.addCategory(android.content.Intent.CATEGORY_OPENABLE);
                        i.setType("*/*");
                        startActivityForResult(i, IMPORT_FILE);
                        return;
                    }
                    else
                    {
                        android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT);
                        i.addCategory(android.content.Intent.CATEGORY_OPENABLE);
                        i.setType("application/octet-stream");
                        i.putExtra(android.content.Intent.EXTRA_TITLE, p.exportName(tab, r));
                        startActivityForResult(i, EXPORT_FILE);
                        return;
                    }
                    liveryRow[tab] = r;
                    again();
                }
            })
            .show();
    }

    /** A picked file's name as the picker shows it. */
    private String fileName(android.net.Uri uri)
    {
        try
        {
            android.database.Cursor c = getContentResolver().query(uri, new String[] { android.provider.OpenableColumns.DISPLAY_NAME },
                                                                   null, null, null);
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

    protected void onActivityResult(int request, int result, android.content.Intent data)
    {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null || data.getData() == null || screen == null)
            return;
        android.net.Uri uri = data.getData();
        String name = fileName(uri);
        try
        {
            if (request == IMPORT_FILE)
            {
                java.io.InputStream in = getContentResolver().openInputStream(uri);
                java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                for (int n; in != null && (n = in.read(buf)) > 0; )
                    bytes.write(buf, 0, n);
                if (in != null)
                    in.close();
                liveryRow[fileTab] = fileRow;
                again();
                Dialogs.show(this, screen.importLivery(fileTab, fileRow, name, bytes.toByteArray()), new Runnable() {
                    public void run() { again(); }
                });
            }
            else if (request == EXPORT_FILE)
            {
                java.io.OutputStream out = getContentResolver().openOutputStream(uri, "wt");
                out.write(screen.exportLivery(fileTab, fileRow));
                out.close();
                android.widget.Toast.makeText(this, "Saved " + name + ".", android.widget.Toast.LENGTH_SHORT).show();
            }
        }
        catch (java.io.IOException | RuntimeException e)
        {
            Dialogs.show(this, request == IMPORT_FILE ? LiveriesPage.notArgEditorFile(fileTab, name) : LiveriesPage.notSaved(fileTab, name), null);
        }
    }

    // --- Lap Records -------------------------------------------------------

    private static final String[] RECORD_FIELDS = { "Qualifying: driver", "Qualifying: team", "Qualifying: time", "Qualifying: date",
                                                    "Race: driver", "Race: team", "Race: time", "Race: date" };

    private void lapRecords()
    {
        final LapRecordsPage records = screen.records();
        for (int i = 0; i < LapRecordsPage.ROWS; i++)
        {
            final int r = i;
            row(records.value(r, 0) + "\n     Qualifying: " + records.value(r, 1) + ", " + records.value(r, 2) + ", "
                + records.value(r, 3) + ", " + records.value(r, 4) + "\n     Race: " + records.value(r, 5) + ", "
                + records.value(r, 6) + ", " + records.value(r, 7) + ", " + records.value(r, 8), new Runnable() {
                public void run()
                {
                    String[] values = new String[8];
                    for (int c = 0; c < 8; c++)
                        values[c] = records.value(r, c + 1);
                    form(records.value(r, 0), RECORD_FIELDS, values, new boolean[8], new Apply() {
                        public Message apply(String[] v)
                        {
                            for (int c = 0; c < 8; c++)
                            {
                                Message m = screen.setRecord(r, c + 1, v[c]);
                                if (m != null)
                                    return m;
                            }
                            return null;
                        }
                    });
                }
            });
        }
    }

    // --- Car Physics, Opposition Levels: grids --------------------------------

    private void tables(TablePage p)
    {
        TextView intro = Ui.text(this, p.intro + " Changed values are shown in bold; tap a value to change it or see the original.", 13);
        page.addView(intro);
        for (TablePage.Section sec : p.sections)
        {
            TextView title = Ui.text(this, sec.title, 16);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            title.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 4));
            page.addView(title);
            if (sec.note != null)
            {
                TextView note = Ui.text(this, sec.note, 13);
                note.setTextColor(Color.GRAY);
                page.addView(note);
            }
            page.addView(grid(sec));
        }
    }

    /** A grid as a table that scrolls sideways: column labels, then a row label and its values. */
    private View grid(final TablePage.Section sec)
    {
        final TablePage.Grid g = sec.grid;
        TableLayout table = new TableLayout(this);
        int p = Ui.dp(this, 6);
        TableRow head = new TableRow(this);
        head.addView(new TextView(this));
        for (String col : g.cols)
        {
            TextView t = Ui.text(this, col, 12);
            t.setTextColor(Color.GRAY);
            t.setPadding(p, p, p, p);
            t.setGravity(Gravity.END);
            head.addView(t);
        }
        table.addView(head);
        for (int i = 0; i < g.rows.length; i++)
        {
            final int r = i;
            TableRow row = new TableRow(this);
            TextView label = Ui.text(this, g.rows[r], 14);
            label.setPadding(0, p, p, p);
            row.addView(label);
            for (int j = 0; j < g.cols.length; j++)
            {
                final int c = j;
                TextView cell = Ui.text(this, String.valueOf(g.get(r, c)), 14);
                cell.setPadding(p, p, p, p);
                cell.setGravity(Gravity.END);
                if (g.isChanged(r, c))
                    cell.setTypeface(Typeface.DEFAULT_BOLD);
                cell.setBackgroundResource(android.R.drawable.list_selector_background);
                cell.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v)
                    {
                        String what = g.rows[r] + (g.cols.length > 1 ? ", " + g.cols[c] : "");
                        form(sec.title, new String[] { what + " (original " + g.original(r, c) + ")" },
                            new String[] { String.valueOf(g.get(r, c)) }, new boolean[] { true }, new Apply() {
                                public Message apply(String[] v) { return screen.setCell(g, r, c, v[0]); }
                            });
                    }
                });
                row.addView(cell);
            }
            table.addView(row);
        }
        HorizontalScrollView sideways = new HorizontalScrollView(this);
        sideways.addView(table);
        return sideways;
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
            finish();
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
