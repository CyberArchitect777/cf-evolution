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
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import cfevolution.edit.CarSettingsForm;
import cfevolution.edit.Message;
import cfevolution.edit.PropertySheet;
import cfevolution.edit.SegmentCommands;
import cfevolution.edit.TrackOutline;
import cfevolution.edit.RaceStrategyForm;
import cfevolution.edit.RandomTrackGeneration;
import cfevolution.edit.TrackCheck;
import cfevolution.edit.TrackMap;
import cfevolution.edit.TrackTools;
import cfevolution.generator.ccline.CCLineProgressListener;
import cfevolution.generator.track.TrackProgressListener;
import cfevolution.screens.Flow;
import cfevolution.screens.TrackScreen;

/**
 * One circuit: its map, the warnings above it, Check Track, the track tools,
 * best line generation and random tracks, and saving. Everything it does is
 * the desktop editor's own, through the TrackScreen model.
 */
public class TrackActivity extends Activity
{
    private TrackScreen screen;
    private TrackMapView mapView;
    private LinearLayout banners;
    private TextView status;
    private HorizontalScrollView pickedBar;
    private Button commandsButton, aboveButton, belowButton, deleteButton, repairButton;
    private static final String MAP_HINT = "Pinch to zoom, drag to move, double-tap to see the whole track. Tap a segment to pick it.";
    private volatile boolean cancelled;

    protected void onCreate(Bundle state)
    {
        super.onCreate(state);
        screen = Session.track;
        if (screen == null)
        {
            finish();
            return;
        }
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);

        banners = new LinearLayout(this);
        banners.setOrientation(LinearLayout.VERTICAL);
        col.addView(banners);

        mapView = new TrackMapView(this);
        mapView.setScreen(screen, new TrackMapView.Picked() {
            public void picked(int section, int sector)
            {
                screen.pick(section, sector);       // TrackMap and TrackOutline number the lists alike
                showPicked();
            }
        });
        col.addView(mapView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        status = Ui.text(this, MAP_HINT, 13);
        int p = Ui.dp(this, 8);
        status.setPadding(p, p / 2, p, p / 2);
        col.addView(status);

        // what can be done with the picked part
        LinearLayout pickedTools = new LinearLayout(this);
        pickedTools.addView(Ui.button(this, "Properties", click(new Runnable() { public void run() { properties(); } })));
        commandsButton = Ui.button(this, "Commands", click(new Runnable() { public void run() { commands(); } }));
        pickedTools.addView(commandsButton);
        aboveButton = Ui.button(this, "Add Above", click(new Runnable() { public void run() { flow(screen.addSegment(true)); } }));
        pickedTools.addView(aboveButton);
        belowButton = Ui.button(this, "Add Below", click(new Runnable() { public void run() { flow(screen.addSegment(false)); } }));
        pickedTools.addView(belowButton);
        deleteButton = Ui.button(this, "Delete", click(new Runnable() { public void run() { flow(screen.deleteSegment()); } }));
        pickedTools.addView(deleteButton);
        pickedBar = new HorizontalScrollView(this);
        pickedBar.addView(pickedTools);
        col.addView(pickedBar);

        LinearLayout tools = new LinearLayout(this);
        tools.addView(Ui.button(this, "Save", click(new Runnable() { public void run() { save(null); } })));
        tools.addView(Ui.button(this, "Segments", click(new Runnable() { public void run() { segments(); } })));
        tools.addView(Ui.button(this, "Check Track", click(new Runnable() { public void run() { checkTrack(); } })));
        tools.addView(Ui.button(this, "Generate Best Line", click(new Runnable() { public void run() { bestLine(); } })));
        repairButton = Ui.button(this, "Repair Best Line After Edit", click(new Runnable() { public void run() { flow(screen.repairLine()); } }));
        tools.addView(repairButton);
        tools.addView(Ui.button(this, "Random Track", click(new Runnable() { public void run() { randomTrack(); } })));
        tools.addView(Ui.button(this, "Move Away From Map Edge", click(new Runnable() { public void run() { flow(screen.moveFromEdge()); } })));
        tools.addView(Ui.button(this, "Match Start/End Heading", click(new Runnable() { public void run() { flow(screen.matchHeading()); } })));
        tools.addView(Ui.button(this, "Reduce to Minimal Track", click(new Runnable() { public void run() { flow(screen.reduceToMinimal()); } })));
        tools.addView(Ui.button(this, cfevolution.edit.HorizonPage.TITLE, click(new Runnable() { public void run() { horizon(); } })));
        tools.addView(Ui.button(this, RaceStrategyForm.TITLE, click(new Runnable() { public void run() { raceStrategy(null); } })));
        tools.addView(Ui.button(this, CarSettingsForm.TITLE, click(new Runnable() { public void run() { carSettings(null); } })));
        HorizontalScrollView bar = new HorizontalScrollView(this);
        bar.addView(tools);
        col.addView(bar);
        setContentView(col);
        refresh();
    }

    private static View.OnClickListener click(final Runnable r)
    {
        return new View.OnClickListener() { public void onClick(View v) { r.run(); } };
    }

    /** After any change: the map, the warnings and the title. */
    private void refresh()
    {
        setTitle(screen.title() + (screen.isModified() ? " *" : ""));
        if (getActionBar() != null)
            getActionBar().setSubtitle(screen.circuit != null ? screen.file.getName() : null);
        mapView.refresh();
        showPicked();
        repairButton.setEnabled(screen.canRepairLine());
        banners.removeAllViews();
        for (final TrackTools.Banner b : screen.banners())
        {
            LinearLayout row = new LinearLayout(this);
            row.setBackgroundColor(Color.rgb(0xFF, 0xF4, 0xCC));
            int p = Ui.dp(this, 6);
            row.setPadding(p, p, p, p);
            TextView t = Ui.text(this, "⚠  " + b.text, 13);
            t.setTextColor(Color.rgb(0x6B, 0x4E, 0x00));
            row.addView(t, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            row.addView(Ui.button(this, b.fixLabel, click(new Runnable() { public void run() { fix(b.action); } })));
            banners.addView(row);
        }
    }

    // --- The picked part: properties, commands, adding and deleting ------------

    /** The picked part under the map, and what can be done with it. */
    private void showPicked()
    {
        int kind = screen.pickedKind();
        boolean segment = screen.pickedNo() > 0;
        status.setText(kind == TrackOutline.NOTHING ? MAP_HINT : screen.heading() + "  \u00b7  " + screen.hint());
        pickedBar.setVisibility(kind == TrackOutline.NOTHING ? View.GONE : View.VISIBLE);
        commandsButton.setVisibility(screen.pickedSegment() != null ? View.VISIBLE : View.GONE);
        aboveButton.setVisibility(segment ? View.VISIBLE : View.GONE);
        belowButton.setVisibility(segment ? View.VISIBLE : View.GONE);
        deleteButton.setVisibility(segment ? View.VISIBLE : View.GONE);
        mapView.select(kind, screen.pickedNo());
    }

    /** The track's lists, as the desktop's: the headers, the three kinds of segment, and Remove All. */
    private void segments()
    {
        final int[] kinds = { TrackOutline.TRACK, TrackOutline.PIT, TrackOutline.LINE };
        String[] items = new String[7];
        items[0] = TrackOutline.HEADERS_BRANCH;
        for (int i = 0; i < 3; i++)
            items[1 + i] = TrackOutline.branch(kinds[i]) + " (" + TrackOutline.count(screen.track, kinds[i]) + ")";
        items[4] = "Remove All Track Segments...";
        items[5] = "Remove All Pit Segments...";
        items[6] = "Remove All Best Line Segments...";
        new AlertDialog.Builder(this).setTitle(TrackOutline.ROOT)
            .setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int which)
                {
                    if (which == 0)
                    {
                        screen.pick(TrackOutline.HEADERS, -1);
                        refresh();
                        properties();
                    }
                    else if (which <= 3)
                        segmentList(kinds[which - 1]);
                    else
                        flow(screen.clearSegments(kinds[which - 4]));
                }
            })
            .setNegativeButton("Close", null).show();
    }

    private void segmentList(final int kind)
    {
        new AlertDialog.Builder(this).setTitle(TrackOutline.branch(kind))
            .setItems(screen.list(kind), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int which)
                {
                    screen.pick(kind, which + 1);
                    refresh();
                }
            })
            .setNegativeButton("Close", null).show();
    }

    /** OK on a property sheet: the refusal or the notice, or null. */
    private interface SheetOk
    {
        Message ok(PropertySheet sheet);
    }

    private void properties()
    {
        PropertySheet sheet = screen.properties();
        if (sheet != null)
            sheetDialog(screen.heading(), sheet, new SheetOk() {
                public Message ok(PropertySheet s) { return screen.applyProperties(s); }
            }, null, null, null);
    }

    /**
     * A property sheet as a form: numbers to type, tick boxes, and a choice
     * that rebuilds the form. What is typed goes into the sheet; OK writes it
     * (a refusal reopens the form as typed), Cancel drops it.
     */
    private void sheetDialog(final String title, final PropertySheet sheet, final SheetOk ok,
                             String extraLabel, final Runnable extra, final Runnable afterOk)
    {
        LinearLayout form = Ui.column(this);
        final AlertDialog[] dialog = new AlertDialog[1];
        final TextView note = sheet.note() != null ? hint(sheet.note()) : null;
        for (int r = 0; r < sheet.rows(); r++)
        {
            final int row = r;
            switch (sheet.kind(r))
            {
                case PropertySheet.FLAG:
                {
                    CheckBox box = new CheckBox(this);
                    box.setText(sheet.label(r));
                    box.setChecked(sheet.ticked(r));
                    box.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        public void onCheckedChanged(CompoundButton b, boolean on) { sheet.tick(row, on); }
                    });
                    form.addView(box);
                    break;
                }
                case PropertySheet.CHOICE:
                {
                    form.addView(Ui.text(this, sheet.label(r), 14));
                    Spinner choice = new Spinner(this);
                    ArrayAdapter<String> items = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, sheet.options(r));
                    items.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    choice.setAdapter(items);
                    choice.setSelection(sheet.chosen(r));
                    choice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        public void onItemSelected(AdapterView<?> parent, View v, int position, long id)
                        {
                            if (position == sheet.chosen(row))
                                return;
                            sheet.choose(row, position);         // the rows change with the choice
                            dialog[0].dismiss();
                            sheetDialog(title, sheet, ok, null, extra, afterOk);
                        }
                        public void onNothingSelected(AdapterView<?> parent) { }
                    });
                    form.addView(choice);
                    break;
                }
                default:
                {
                    EditText box = Ui.field(form, sheet.label(r), sheet.text(r), true);
                    box.addTextChangedListener(new TextWatcher() {
                        public void beforeTextChanged(CharSequence c, int a, int b, int n) { }
                        public void onTextChanged(CharSequence c, int a, int b, int n) { }
                        public void afterTextChanged(Editable e)
                        {
                            sheet.type(row, e.toString());
                            if (note != null)
                                note.setText(sheet.note());
                        }
                    });
                }
            }
        }
        if (note != null)
            form.addView(note);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);
        AlertDialog.Builder b = new AlertDialog.Builder(this).setTitle(title).setView(scroll)
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int which)
                {
                    Message m = ok.ok(sheet);
                    final boolean refused = sheet.problem() != null;
                    refresh();
                    Dialogs.show(TrackActivity.this, m, new Runnable() {
                        public void run()
                        {
                            if (refused)
                                sheetDialog(title, sheet, ok, null, extra, afterOk);
                            else if (afterOk != null)
                                afterOk.run();
                        }
                    });
                }
            })
            .setNegativeButton("Cancel", null);
        if (extra != null)
            b.setNeutralButton(extraLabel != null ? extraLabel : "Remove", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int which) { extra.run(); }
            });
        dialog[0] = b.show();
    }

    /** The picked segment's commands: tap one for its parameters, or add one. */
    private void commands()
    {
        if (screen.pickedSegment() == null)
            return;
        new AlertDialog.Builder(this).setTitle(screen.heading() + ": " + screen.commandsText())
            .setItems(screen.commands(), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int which) { command(which); }
            })
            .setPositiveButton(SegmentCommands.ADD + "...", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int which) { addCommand(); }
            })
            .setNegativeButton("Close", null).show();
    }

    private void addCommand()
    {
        new AlertDialog.Builder(this).setTitle(SegmentCommands.ADD_TIP)
            .setItems(SegmentCommands.available(), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int which)
                {
                    int at = screen.addCommand(which);
                    refresh();
                    command(at);
                }
            })
            .setNegativeButton("Cancel", null).show();
    }

    /** One command: its parameters to change, and Remove. */
    private void command(final int index)
    {
        final Runnable remove = new Runnable() {
            public void run()
            {
                screen.removeCommand(index);
                refresh();
                commands();
            }
        };
        final Runnable back = new Runnable() { public void run() { commands(); } };
        PropertySheet p = screen.commandProperties(index);
        if (p == null)
        {
            new AlertDialog.Builder(this).setTitle(screen.commands()[index]).setMessage(screen.commandHeading(index))
                .setNeutralButton(SegmentCommands.REMOVE, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int which) { remove.run(); }
                })
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int which) { back.run(); }
                }).show();
            return;
        }
        sheetDialog(screen.commandHeading(index), p, new SheetOk() {
            public Message ok(PropertySheet s) { return screen.applyCommand(s); }
        }, SegmentCommands.REMOVE, remove, back);
    }

    private void flow(Flow f)
    {
        Dialogs.run(this, f, new Runnable() { public void run() { refresh(); } });
    }

    private void fix(int action)
    {
        if (action == TrackCheck.ACTION_GENERATE_LINE)
            bestLine();
        else
            flow(screen.fix(action));
    }

    // --- Check Track -------------------------------------------------------

    private void checkTrack()
    {
        TrackCheck.Report r = screen.check();
        LinearLayout col = Ui.column(this);
        TextView verdict = Ui.heading(this, TrackCheck.verdict(r.overall()));
        col.addView(verdict);
        final AlertDialog[] dialog = new AlertDialog[1];
        for (final TrackCheck.Item it : r.items)
        {
            String mark = it.status == TrackCheck.ERROR ? "✖ " : it.status == TrackCheck.WARNING ? "⚠ " : "✔ ";
            col.addView(Ui.text(this, mark + it.area + ": " + it.title, 15));
            if (it.detail != null && it.detail.length() > 0 && it.status != TrackCheck.OK)
                col.addView(Ui.text(this, Dialogs.text(it.detail), 13));
            if (it.action != TrackCheck.ACTION_NONE)
                col.addView(Ui.button(this, it.action == TrackCheck.ACTION_GENERATE_LINE ? "Generate..." : "Fix",
                    click(new Runnable() { public void run() { dialog[0].dismiss(); fix(it.action); } })));
        }
        ScrollView scroll = new ScrollView(this);
        scroll.addView(col);
        dialog[0] = new AlertDialog.Builder(this).setTitle("Check Track").setView(scroll)
            .setPositiveButton("Close", null).show();
    }

    // --- Best line ---------------------------------------------------------

    private void bestLine()
    {
        final TrackScreen.Preparation prep = screen.prepareBestLine();
        Dialogs.run(this, prep, new Runnable() {
            public void run()
            {
                refresh();
                if (prep.ready())
                    generateLine();
            }
        });
    }

    private void generateLine()
    {
        final Working w = new Working("Generate Best Line");
        cancelled = false;
        new Thread(new Runnable() {
            public void run()
            {
                final String result = screen.generateBestLine(false, new CCLineProgressListener() {
                    public void progress(int percent, String message) { w.progress(percent, message); }
                    public boolean isCancelled() { return cancelled; }
                });
                runOnUiThread(new Runnable() {
                    public void run()
                    {
                        w.dismiss();
                        refresh();
                        if (!screen.hasPendingLine())
                        {
                            Dialogs.show(TrackActivity.this, Message.info("Generate Best Line", result), null);
                            return;
                        }
                        new AlertDialog.Builder(TrackActivity.this).setTitle("Generate Best Line")
                            .setMessage(result.replace("Apply or Discard", "Apply or Discard (the new line is in magenta)"))
                            .setCancelable(false)
                            .setPositiveButton("Apply", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface d, int which) { screen.applyBestLine(); refresh(); }
                            })
                            .setNegativeButton("Discard", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface d, int which) { screen.discardBestLine(); refresh(); }
                            })
                            .show();
                    }
                });
            }
        }, "ccline-generator").start();
    }

    // --- Random track ------------------------------------------------------

    private void randomTrack()
    {
        Dialogs.run(this, screen.confirmRandom(), new Runnable() {
            public void run()
            {
                if (screen.randomConfirmed())
                    randomSettings();
            }
        });
    }

    private void randomSettings()
    {
        LinearLayout form = Ui.column(this);
        final EditText seed = Ui.field(form, "Seed (blank for a new one)", "", true);
        final EditText length = Ui.field(form, "Length (TLU)", String.valueOf(RandomTrackGeneration.DEFAULT_TLU), true);
        final EditText corners = Ui.field(form, "Corners", String.valueOf(RandomTrackGeneration.DEFAULT_CORNERS), true);
        final EditText elevation = Ui.field(form, "Most height change", String.valueOf(RandomTrackGeneration.DEFAULT_ELEVATION), true);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);
        new AlertDialog.Builder(this).setTitle("Generate Random Track").setView(scroll)
            .setPositiveButton("Generate & Apply", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int which)
                {
                    generateRandom(seed.getText().toString(), length.getText().toString(),
                                   corners.getText().toString(), elevation.getText().toString());
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void generateRandom(final String seed, final String length, final String corners, final String elevation)
    {
        final Working w = new Working("Generate Random Track");
        cancelled = false;
        new Thread(new Runnable() {
            public void run()
            {
                final TrackScreen.RandomRun run = screen.generateRandom(seed, length, corners, elevation, new TrackProgressListener() {
                    public void progress(int percent, String message) { w.progress(percent, message); }
                    public boolean isCancelled() { return cancelled; }
                });
                runOnUiThread(new Runnable() {
                    public void run()
                    {
                        w.dismiss();
                        String result = screen.applyRandom(run);
                        refresh();
                        mapView.fit();
                        status.setText(result);
                        Dialogs.show(TrackActivity.this, Message.info("Generate Random Track", result), null);
                    }
                });
            }
        }, "track-generator").start();
    }

    /** A progress box with Cancel, for the generators. */
    private final class Working
    {
        private final AlertDialog dialog;
        private final ProgressBar bar;
        private final TextView text;

        Working(String title)
        {
            LinearLayout col = Ui.column(TrackActivity.this);
            bar = new ProgressBar(TrackActivity.this, null, android.R.attr.progressBarStyleHorizontal);
            bar.setMax(100);
            col.addView(bar);
            text = Ui.text(TrackActivity.this, "Starting...", 14);
            col.addView(text);
            dialog = new AlertDialog.Builder(TrackActivity.this).setTitle(title).setView(col).setCancelable(false)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int which) { cancelled = true; }
                })
                .show();
        }

        void progress(final int percent, final String message)
        {
            runOnUiThread(new Runnable() {
                public void run()
                {
                    bar.setProgress(percent);
                    text.setText(message);
                }
            });
        }

        void dismiss()
        {
            if (dialog.isShowing())
                dialog.dismiss();
        }
    }

    // --- Edit > Race Strategy, Edit > Car and AI Settings -----------------------

    private TextView hint(String text)
    {
        TextView h = Ui.text(this, text, 12);
        h.setTextColor(Color.GRAY);
        return h;
    }

    /** A form dialog; OK hands over the typed values, Cancel drops them. */
    private interface Typed
    {
        void ok(String[] values);
    }

    private void formDialog(String title, LinearLayout form, final EditText[] boxes, final Typed typed)
    {
        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);
        new AlertDialog.Builder(this).setTitle(title).setView(scroll)
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int which)
                {
                    String[] v = new String[boxes.length];
                    for (int i = 0; i < v.length; i++)
                        v[i] = boxes[i].getText().toString();
                    typed.ok(v);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    /** Laps, first stop and stop chance; again shows what was typed after a refusal. */
    private void raceStrategy(String[] again)
    {
        final RaceStrategyForm f = screen.raceStrategy();
        if (f == null)
        {
            Dialogs.show(this, RaceStrategyForm.NOT_AVAILABLE, null);
            return;
        }
        LinearLayout form = Ui.column(this);
        final EditText[] boxes = new EditText[3];
        for (int i = 0; i < 3; i++)
        {
            boxes[i] = Ui.field(form, RaceStrategyForm.LABELS[i], again != null ? again[i] : String.valueOf(f.shown(i)), true);
            form.addView(hint(RaceStrategyForm.HINTS[i]));
        }
        formDialog(RaceStrategyForm.TITLE, form, boxes, new Typed() {
            public void ok(final String[] v)
            {
                Message m = screen.applyRaceStrategy(f, v);
                refresh();
                Dialogs.show(TrackActivity.this, m, m == null ? null : new Runnable() {
                    public void run() { raceStrategy(v); }
                });
            }
        });
    }

    /** The circuit's car and computer car settings, in the desktop's groups. */
    private void carSettings(String[] again)
    {
        final CarSettingsForm f = screen.carSettings();
        LinearLayout form = Ui.column(this);
        final EditText[] boxes = new EditText[f.fields().size()];
        for (CarSettingsForm.Group g : f.groups())
        {
            form.addView(Ui.heading(this, g.title));
            if (g.note != null)
                form.addView(hint(g.note));
            for (CarSettingsForm.Row r : g.rows)
            {
                for (int i = 0; i < r.fields.size(); i++)
                {
                    CarSettingsForm.Field x = r.fields.get(i);
                    String label = r.fields.size() > 1 ? "Gear " + (i + 1) : r.label;
                    if (x.kind == CarSettingsForm.TYRE)
                        label += " (A, B, C or D)";
                    boxes[x.at] = Ui.field(form, label, again != null ? again[x.at] : f.text(x, f.shown(x)), x.kind != CarSettingsForm.TYRE);
                }
                form.addView(hint(r.hint));
            }
        }
        formDialog(CarSettingsForm.TITLE, form, boxes, new Typed() {
            public void ok(final String[] v)
            {
                Message m = screen.applyCarSettings(f, v);
                refresh();
                Dialogs.show(TrackActivity.this, m, m == null ? null : new Runnable() {
                    public void run() { carSettings(v); }
                });
            }
        });
    }

    // --- Edit > Horizon --------------------------------------------------------

    private void horizon()
    {
        cfevolution.edit.HorizonPage page = screen.horizon();
        if (page == null)
        {
            Dialogs.show(this, cfevolution.edit.HorizonPage.NO_GAME_EXE, null);
            return;
        }
        Session.horizon = page;
        startActivity(new android.content.Intent(this, HorizonActivity.class));
    }

    protected void onResume()
    {
        super.onResume();
        if (screen != null && mapView != null)
            refresh();
    }

    // --- Saving ------------------------------------------------------------

    private void save(final Runnable after)
    {
        final Message warning = screen.save();
        refresh();
        Session.writeBack(this, new Runnable() {
            public void run()
            {
                status.setText("Saved " + screen.title() + ".");
                Dialogs.show(TrackActivity.this, warning, after);
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
            .setMessage(screen.title() + " has changes that have not been saved.")
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
