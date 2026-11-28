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
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import cfevolution.data.executable.GameLiveries;
import cfevolution.data.image.Picture;
import cfevolution.edit.Message;
import cfevolution.edit.PictureFilePage;
import java.io.File;

/**
 * One of the game's picture files on a phone (2026-11-17): pick a picture;
 * a small one can be drawn on (tap to draw or replace a colour, press and
 * hold to pick up a colour); any can be exported as PNG and replaced from a
 * picture. Save writes the file and copies it back.
 */
public class PictureActivity extends Activity
{
    private static final int IMPORT = 1, EXPORT = 2;
    private PictureFilePage page;
    private File file;
    private int picture;
    private boolean modified;
    private PictureView view;
    private TextView colour;

    protected void onCreate(Bundle state)
    {
        super.onCreate(state);
        Session.pictures();
        page = Session.pictureFile;
        file = Session.gameFilePath;
        if (page == null || file == null)
        {
            finish();
            return;
        }
        LinearLayout col = Ui.column(this);
        col.addView(Ui.heading(this, page.title()));
        TextView about = Ui.text(this, page.explanation(), 13);
        about.setTextColor(Color.GRAY);
        col.addView(about);
        Spinner which = new Spinner(this);
        ArrayAdapter<String> names = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, page.names());
        names.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        which.setAdapter(names);
        which.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(android.widget.AdapterView<?> p, View v, int position, long id) { picture = position; view.requestLayout(); view.invalidate(); }
            public void onNothingSelected(android.widget.AdapterView<?> p) { }
        });
        col.addView(which);
        view = new PictureView();
        HorizontalScrollView across = new HorizontalScrollView(this);
        across.addView(view);
        col.addView(across);
        LinearLayout buttons = new LinearLayout(this);
        buttons.addView(Ui.button(this, "Save", click(new Runnable() { public void run() { save(null); } })));
        if (Session.folder != null)
            buttons.addView(Ui.button(this, "Revert File", click(new Runnable() { public void run() {
                Session.revertFile(PictureActivity.this, file, new Runnable() {
                    public void run()
                    {
                        Message[] problem = new Message[1];
                        cfevolution.edit.PictureFilePage again = Session.folder.openPictures(file, problem);
                        if (again == null)
                        {
                            finish();
                            return;
                        }
                        Session.pictureFile = again;
                        recreate();
                    }
                });
            } })));
        buttons.addView(Ui.button(this, PictureFilePage.EXPORT.replace("...", ""), click(new Runnable() {
            public void run() { Documents.create(PictureActivity.this, EXPORT, page.exportName(picture)); }
        })));
        buttons.addView(Ui.button(this, PictureFilePage.IMPORT.replace("...", ""), click(new Runnable() {
            public void run() { Documents.open(PictureActivity.this, IMPORT); }
        })));
        buttons.addView(Ui.button(this, PictureFilePage.REVERT, click(new Runnable() {
            public void run() { changed(page.revert(picture)); }
        })));
        if (page.canClear())        // the menu helmets and flags (Hrvoje, 2026-11-15)
            buttons.addView(Ui.button(this, PictureFilePage.CLEAR, click(new Runnable() {
                public void run() { page.clear(picture); changed(null); }
            })));
        HorizontalScrollView bar = new HorizontalScrollView(this);
        bar.addView(buttons);
        col.addView(bar);
        if (page.small)
        {
            RadioGroup modes = new RadioGroup(this);
            modes.setOrientation(LinearLayout.HORIZONTAL);
            final RadioButton draw = new RadioButton(this), replace = new RadioButton(this);
            draw.setText(PictureFilePage.DRAW);
            replace.setText(PictureFilePage.REPLACE);
            modes.addView(draw);
            modes.addView(replace);
            draw.setChecked(true);
            modes.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                public void onCheckedChanged(RadioGroup g, int id) { page.setReplaceMode(replace.isChecked()); }
            });
            col.addView(modes);
            colour = Ui.text(this, "", 14);
            col.addView(colour);
            col.addView(Ui.button(this, "Colour", click(new Runnable() { public void run() { chooseColour(); } })));
            TextView hint = Ui.text(this, "Tap to use the chosen colour; press and hold a pixel to pick up its colour.", 13);
            hint.setTextColor(Color.GRAY);
            col.addView(hint);
            showColour();
        }
        ScrollView scroll = new ScrollView(this);
        scroll.addView(col);
        setContentView(scroll);
        showTitle();
    }

    private static View.OnClickListener click(final Runnable r)
    {
        return new View.OnClickListener() { public void onClick(View v) { r.run(); } };
    }

    private void showTitle()
    {
        setTitle(file.getName() + (modified ? " *" : ""));
        if (getActionBar() != null)
            getActionBar().setSubtitle(page.status());
    }

    private void showColour()
    {
        if (colour == null)
            return;
        colour.setText("Colour " + page.colour() + "  ■");
        colour.setTextColor(0xFF000000 | page.smallPalette()[page.colour()]);
    }

    private void changed(Message refusal)
    {
        if (refusal != null)
            Dialogs.show(this, refusal, null);
        else if (page.changed())
            modified = true;
        showTitle();
        view.invalidate();
    }

    private void chooseColour()
    {
        int[] pal = page.smallPalette();
        byte[] p = new byte[768];
        for (int i = 0; i < 256; i++)
        {
            p[i * 3] = (byte) ((pal[i] >> 18) & 0x3F);
            p[i * 3 + 1] = (byte) ((pal[i] >> 10) & 0x3F);
            p[i * 3 + 2] = (byte) ((pal[i] >> 2) & 0x3F);
        }
        final PaletteView palette = new PaletteView(this, GameLiveries.ofPalette(p), page.colour(), new PaletteView.Picked() {
            public void colour(int index) { }
        });
        palette.marks = false;      // the race's colour notes are not about the menus' pictures
        LinearLayout box = Ui.column(this);
        box.addView(palette);
        new AlertDialog.Builder(this).setTitle(PictureFilePage.COLOURS_TIP).setView(box)
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w)
                {
                    page.setColour(palette.highlighted());
                    showColour();
                }
            })
            .setNegativeButton("Cancel", null).show();
    }

    protected void onActivityResult(int request, int result, Intent data)
    {
        super.onActivityResult(request, result, data);
        final Uri uri = Documents.picked(result, data);
        if (uri == null || page == null)
            return;
        String name = Documents.name(this, uri);
        if (request == EXPORT)
        {
            try
            {
                Documents.write(this, uri, page.exportPng(picture));
                android.widget.Toast.makeText(this, "Saved " + name + ".", android.widget.Toast.LENGTH_SHORT).show();
            }
            catch (java.io.IOException | RuntimeException e)
            {
                Dialogs.show(this, PictureFilePage.NOT_EXPORTED, null);
            }
            return;
        }
        byte[] bytes = null;
        try
        {
            bytes = Documents.read(this, uri);
        }
        catch (java.io.IOException e)
        {
            bytes = null;
        }
        final Picture in = PictureFilePage.read(bytes);
        if (in == null)
        {
            Dialogs.show(this, PictureFilePage.NOT_A_PICTURE, null);
            return;
        }
        final Message q = page.importQuestion(picture);
        if (q == null)
        {
            changed(page.importPicture(picture, in, false));
            return;
        }
        new AlertDialog.Builder(this).setTitle(q.title).setMessage(q.text)
            .setPositiveButton(q.options[0], new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) { changed(page.importPicture(picture, in, true)); }
            })
            .setNeutralButton(q.options[1], new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) { changed(page.importPicture(picture, in, false)); }
            })
            .setNegativeButton(q.options[2], null).show();
    }

    private void save(final Runnable after)
    {
        Message m = page.save(file);
        if (m != null)
        {
            Dialogs.show(this, m, null);
            return;
        }
        modified = false;
        showTitle();
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
                public void onClick(DialogInterface d, int which) { save(new Runnable() { public void run() { finish(); } }); }
            })
            .setNeutralButton("Don't Save", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int which) { finish(); }
            })
            .setNegativeButton("Cancel", null).show();
    }

    /** The picture as large as fits the screen's width (a whole number of times), drawn without smoothing. */
    private class PictureView extends View
    {
        private final Paint paint = new Paint();
        private final GestureDetector gestures;

        PictureView()
        {
            super(PictureActivity.this);
            paint.setFilterBitmap(false);
            gestures = new GestureDetector(PictureActivity.this, new GestureDetector.SimpleOnGestureListener() {
                public boolean onDown(MotionEvent e) { return true; }
                public boolean onSingleTapUp(MotionEvent e)
                {
                    int s = scale();
                    page.paint(picture, (int) (e.getX() / s), cfevolution.edit.PixelShape.rowAt((int) e.getY(), s));
                    changed(null);
                    return true;
                }
                public void onLongPress(MotionEvent e)
                {
                    int s = scale();
                    page.pick(picture, (int) (e.getX() / s), cfevolution.edit.PixelShape.rowAt((int) e.getY(), s));
                    showColour();
                }
            });
        }

        int scale()
        {
            int screen = getResources().getDisplayMetrics().widthPixels - Ui.dp(PictureActivity.this, 32);
            return Math.max(1, screen / page.width(picture));
        }

        protected void onMeasure(int w, int h)
        {
            // each pixel in the shape chosen in the Options (PixelShape: 4:3 or square)
            setMeasuredDimension(page.width(picture) * scale(), cfevolution.edit.PixelShape.height(page.height(picture), scale()));
        }

        protected void onDraw(Canvas canvas)
        {
            int[] rgb = page.rgb(picture);
            for (int i = 0; i < rgb.length; i++)
                rgb[i] |= 0xFF000000;
            int w = page.width(picture), h = page.height(picture), s = scale();
            Bitmap b = Bitmap.createBitmap(rgb, w, h, Bitmap.Config.ARGB_8888);
            for (int y = 0; y < h; y++)     // row by row, so the rows fall where taps are read
                canvas.drawBitmap(b, new Rect(0, y, w, y + 1),
                                  new Rect(0, cfevolution.edit.PixelShape.top(y, s), w * s, cfevolution.edit.PixelShape.top(y + 1, s)), paint);
        }

        public boolean onTouchEvent(MotionEvent e)
        {
            return gestures.onTouchEvent(e);
        }
    }
}
