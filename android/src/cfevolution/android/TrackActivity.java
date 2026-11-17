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
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import cfevolution.edit.Message;
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
                status.setText((section == TrackMap.TRACK ? "Track segment " : section == TrackMap.PIT ? "Pit lane segment "
                                : "Best line segment ") + sector);
            }
        });
        col.addView(mapView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        status = Ui.text(this, "Pinch to zoom, drag to move, double-tap to see the whole track.", 13);
        int p = Ui.dp(this, 8);
        status.setPadding(p, p / 2, p, p / 2);
        col.addView(status);

        LinearLayout tools = new LinearLayout(this);
        tools.addView(Ui.button(this, "Save", click(new Runnable() { public void run() { save(null); } })));
        tools.addView(Ui.button(this, "Check Track", click(new Runnable() { public void run() { checkTrack(); } })));
        tools.addView(Ui.button(this, "Generate Best Line", click(new Runnable() { public void run() { bestLine(); } })));
        tools.addView(Ui.button(this, "Random Track", click(new Runnable() { public void run() { randomTrack(); } })));
        tools.addView(Ui.button(this, "Move Away From Map Edge", click(new Runnable() { public void run() { flow(screen.moveFromEdge()); } })));
        tools.addView(Ui.button(this, "Match Start/End Heading", click(new Runnable() { public void run() { flow(screen.matchHeading()); } })));
        tools.addView(Ui.button(this, "Reduce to Minimal Track", click(new Runnable() { public void run() { flow(screen.reduceToMinimal()); } })));
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
        mapView.refresh();
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

    // --- Saving ------------------------------------------------------------

    private void save(final Runnable after)
    {
        Message warning = screen.save();
        refresh();
        status.setText("Saved " + screen.title() + ".");
        Dialogs.show(this, warning, after);
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
