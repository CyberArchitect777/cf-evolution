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
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import cfevolution.data.track.TrackHorizon;
import cfevolution.edit.HorizonPage;
import cfevolution.edit.LiveriesPage;

/**
 * Edit > Horizon on a phone (2026-11-17): the picture enlarged; tap to paint
 * in the chosen colour, press and hold to pick up a colour; Colour opens the
 * palette; export as PNG or .arghoriz, import either or another track's
 * horizon. OK puts the picture into the circuit; Back drops it. Underneath,
 * the whole picture as the game draws it (2026-11-20); tapping it brings that
 * part into the close-up.
 */
public class HorizonActivity extends Activity
{
    private static final int IMPORT = 1, EXPORT_PNG = 2, EXPORT_RAW = 3;
    private HorizonPage page;
    private HorizonView view;
    private PreviewView preview;
    private HorizontalScrollView across;
    private TextView colour;

    protected void onCreate(Bundle state)
    {
        super.onCreate(state);
        Session.pictures();
        page = Session.horizon;
        if (page == null || Session.track == null)
        {
            finish();
            return;
        }
        setTitle(HorizonPage.TITLE);
        LinearLayout col = Ui.column(this);
        view = new HorizonView();
        across = new HorizontalScrollView(this);
        across.addView(view);
        col.addView(across);
        TextView caption = Ui.text(this, HorizonPage.PREVIEW, 13);
        caption.setTextColor(Color.GRAY);
        col.addView(caption);
        preview = new PreviewView();
        col.addView(preview);
        across.getViewTreeObserver().addOnScrollChangedListener(new android.view.ViewTreeObserver.OnScrollChangedListener() {
            public void onScrollChanged() { preview.invalidate(); }
        });
        colour = Ui.text(this, "", 14);
        col.addView(colour);
        LinearLayout buttons = new LinearLayout(this);
        buttons.addView(Ui.button(this, HorizonPage.COLOUR.replace("...", ""), click(new Runnable() { public void run() { chooseColour(); } })));
        buttons.addView(Ui.button(this, "Export PNG", click(new Runnable() { public void run() { Documents.create(HorizonActivity.this, EXPORT_PNG, page.exportName(false)); } })));
        buttons.addView(Ui.button(this, "Export .arghoriz", click(new Runnable() { public void run() { Documents.create(HorizonActivity.this, EXPORT_RAW, page.exportName(true)); } })));
        buttons.addView(Ui.button(this, HorizonPage.IMPORT.replace("...", ""), click(new Runnable() { public void run() { Documents.open(HorizonActivity.this, IMPORT); } })));
        HorizontalScrollView bar = new HorizontalScrollView(this);
        bar.addView(buttons);
        col.addView(bar);
        TextView hint = Ui.text(this, "Tap to paint in the chosen colour; press and hold to pick up a colour.", 13);
        hint.setTextColor(Color.GRAY);
        col.addView(hint);
        TextView about = Ui.text(this, HorizonPage.ABOUT, 13);
        about.setTextColor(Color.GRAY);
        col.addView(about);
        LinearLayout end = new LinearLayout(this);
        end.addView(Ui.button(this, "OK", click(new Runnable() {
            public void run()
            {
                Session.track.applyHorizon(page);
                finish();
            }
        })));
        end.addView(Ui.button(this, "Cancel", click(new Runnable() { public void run() { finish(); } })));
        col.addView(end);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(col);
        setContentView(scroll);
        showColour();
    }

    private static View.OnClickListener click(final Runnable r)
    {
        return new View.OnClickListener() { public void onClick(View v) { r.run(); } };
    }

    private void showColour()
    {
        colour.setText("Colour " + page.current() + "  ■");
        colour.setTextColor(0xFF000000 | page.colours.getPaletteRgb(page.current()));
        view.invalidate();
        preview.invalidate();
    }

    private void chooseColour()
    {
        LinearLayout box = Ui.column(this);
        final TextView about = Ui.text(this, LiveriesPage.about(page.current()), 14);
        final PaletteView palette = new PaletteView(this, page.colours, page.current(), new PaletteView.Picked() {
            public void colour(int index) { }
        });
        box.addView(palette);
        box.addView(about);
        palette.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent e)
            {
                palette.onTouchEvent(e);
                about.setText(LiveriesPage.about(palette.highlighted()));
                return true;
            }
        });
        ScrollView scroll = new ScrollView(this);
        scroll.addView(box);
        new AlertDialog.Builder(this).setTitle(HorizonPage.CHOOSER_TITLE).setView(scroll)
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w)
                {
                    page.setCurrent(palette.highlighted());
                    showColour();
                }
            })
            .setNegativeButton("Cancel", null).show();
    }

    protected void onActivityResult(int request, int result, Intent data)
    {
        super.onActivityResult(request, result, data);
        Uri uri = Documents.picked(result, data);
        if (uri == null || page == null)
            return;
        String name = Documents.name(this, uri);
        if (request == IMPORT)
        {
            byte[] bytes = null;
            try
            {
                bytes = Documents.read(this, uri);
            }
            catch (java.io.IOException e)
            {
                bytes = null;
            }
            Dialogs.show(this, page.importFile(name, bytes), null);
            view.invalidate();
            preview.invalidate();
            return;
        }
        try
        {
            Documents.write(this, uri, page.export(request == EXPORT_RAW));
            android.widget.Toast.makeText(this, "Saved " + name + ".", android.widget.Toast.LENGTH_SHORT).show();
        }
        catch (java.io.IOException | RuntimeException e)
        {
            Dialogs.show(this, HorizonPage.notExported(name), null);
        }
    }

    private Bitmap picture()
    {
        int[] rgb = page.rgb();
        for (int i = 0; i < rgb.length; i++)
            rgb[i] |= 0xFF000000;
        return Bitmap.createBitmap(rgb, TrackHorizon.WIDTH, TrackHorizon.HEIGHT, Bitmap.Config.ARGB_8888);
    }

    /** The whole picture across the screen at the shape the game draws it, with a frame round
        the part the close-up shows; a tap or drag brings that part into the close-up. */
    private class PreviewView extends View
    {
        private final Paint paint = new Paint(), frame = new Paint();
        private final int edge = Ui.dp(HorizonActivity.this, 2);

        PreviewView()
        {
            super(HorizonActivity.this);
            paint.setFilterBitmap(false);
            frame.setStyle(Paint.Style.STROKE);
            frame.setStrokeWidth(edge);
        }

        protected void onMeasure(int w, int h)
        {
            int width = MeasureSpec.getSize(w);
            if (MeasureSpec.getMode(w) == MeasureSpec.UNSPECIFIED || width == 0)
                width = getResources().getDisplayMetrics().widthPixels;
            setMeasuredDimension(width, HorizonPage.previewHeight(width - 2 * edge) + 2 * edge);
        }

        protected void onDraw(Canvas canvas)
        {
            int width = getWidth() - 2 * edge, high = getHeight() - 2 * edge;
            canvas.drawBitmap(picture(), null, new Rect(edge, edge, edge + width, edge + high), paint);
            int whole = view.getWidth();
            if (whole <= 0)
                return;
            float x0 = edge + (float) across.getScrollX() * width / whole;
            float x1 = edge + (float) Math.min(whole, across.getScrollX() + across.getWidth()) * width / whole;
            frame.setColor(Color.BLACK);
            canvas.drawRect(x0 - edge / 2f, edge / 2f, x1 + edge / 2f, getHeight() - edge / 2f, frame);
            frame.setColor(Color.YELLOW);
            canvas.drawRect(x0 + edge / 2f, edge * 1.5f, x1 - edge / 2f, getHeight() - edge * 1.5f, frame);
        }

        public boolean onTouchEvent(MotionEvent e)
        {
            int action = e.getActionMasked();
            if (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_MOVE)
                return action == MotionEvent.ACTION_UP;
            getParent().requestDisallowInterceptTouchEvent(true);
            int column = HorizonPage.previewColumn((int) e.getX() - edge, getWidth() - 2 * edge);
            across.scrollTo(column * view.getWidth() / TrackHorizon.WIDTH - across.getWidth() / 2, 0);
            return true;
        }
    }

    /** The picture, each pixel 15 by 18 density-independent pixels. */
    private class HorizonView extends View
    {
        // 5 wide to 6 tall, as the game shows a pixel on a 4:3 screen (owner, 2026-11-20; square from
        // 2026-11-18, 4 by 24 before that)
        // (15 by 15 with square pixels chosen in the Options: PixelShape, 2026-11-28)
        private final int cellW = Ui.dp(HorizonActivity.this, 15),
                          cellH = Ui.dp(HorizonActivity.this, cfevolution.edit.PixelShape.square() ? 15 : 18);
        private final Paint paint = new Paint();
        private final GestureDetector gestures;

        HorizonView()
        {
            super(HorizonActivity.this);
            paint.setFilterBitmap(false);
            gestures = new GestureDetector(HorizonActivity.this, new GestureDetector.SimpleOnGestureListener() {
                public boolean onDown(MotionEvent e) { return true; }
                public boolean onSingleTapUp(MotionEvent e)
                {
                    page.paint((int) (e.getX() / cellW), (int) (e.getY() / cellH));
                    invalidate();
                    preview.invalidate();
                    return true;
                }
                public void onLongPress(MotionEvent e)
                {
                    page.pick((int) (e.getX() / cellW), (int) (e.getY() / cellH));
                    showColour();
                }
            });
        }

        protected void onMeasure(int w, int h)
        {
            setMeasuredDimension(TrackHorizon.WIDTH * cellW, TrackHorizon.HEIGHT * cellH);
        }

        protected void onDraw(Canvas canvas)
        {
            canvas.drawBitmap(picture(), null, new Rect(0, 0, getWidth(), getHeight()), paint);
        }

        public boolean onTouchEvent(MotionEvent e)
        {
            return gestures.onTouchEvent(e);
        }
    }
}
