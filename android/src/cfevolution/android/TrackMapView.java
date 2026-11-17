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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import cfevolution.edit.TrackMap;
import cfevolution.screens.TrackScreen;

/**
 * Draws a track's map from the core's TrackMap, as the desktop does: the
 * same shapes, colours and drawing order, each coordinate multiplied by the
 * scale and truncated. Pinch to zoom, drag to move, tap to pick a sector.
 */
class TrackMapView extends View
{
    interface Picked
    {
        void picked(int section, int sector);
    }

    private TrackScreen screen;
    private TrackMap map;
    private double scale = 1, tx, ty;
    private boolean fitted;
    private int selTrack = -1, selPit = -1, selLine = -1;
    private Picked picked;
    private final Paint paint = new Paint();
    private final ScaleGestureDetector zoom;
    private final GestureDetector gestures;

    // the desktop's colours (java.awt.Color constants)
    private static final int ORANGE = Color.rgb(255, 200, 0), PINK = Color.rgb(255, 175, 175),
                             GREEN = Color.rgb(0, 255, 0), MAGENTA = Color.rgb(255, 0, 255);

    TrackMapView(Context c)
    {
        super(c);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(0);           // a hairline, as on the desktop
        paint.setAntiAlias(true);
        zoom = new ScaleGestureDetector(c, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            public boolean onScale(ScaleGestureDetector d)
            {
                zoomAt(d.getScaleFactor(), d.getFocusX(), d.getFocusY());
                return true;
            }
        });
        gestures = new GestureDetector(c, new GestureDetector.SimpleOnGestureListener() {
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy)
            {
                tx -= dx;
                ty -= dy;
                invalidate();
                return true;
            }
            public boolean onSingleTapUp(MotionEvent e)
            {
                pickAt(e.getX(), e.getY());
                return true;
            }
            public boolean onDoubleTap(MotionEvent e)
            {
                fit();
                return true;
            }
        });
    }

    void setScreen(TrackScreen s, Picked p)
    {
        screen = s;
        picked = p;
        refresh();
    }

    /** Rebuilds the shapes after the track changed. */
    void refresh()
    {
        map = screen == null ? null : screen.map();
        invalidate();
    }

    void select(int section, int sector)
    {
        selTrack = section == TrackMap.TRACK ? sector : -1;
        selPit = section == TrackMap.PIT ? sector : -1;
        selLine = section == TrackMap.LINE ? sector : -1;
        invalidate();
    }

    /** The whole track in view, as when it was first shown. */
    void fit()
    {
        if (screen == null || getWidth() == 0)
            return;
        double[] f = TrackMap.fit(screen.track, getWidth(), getHeight());
        scale = f[0];
        tx = f[1];
        ty = f[2];
        fitted = true;
        invalidate();
    }

    private void zoomAt(double factor, float px, float py)
    {
        // keep the point under the fingers where it is
        double mx = (px - tx) / scale, my = (ty - py) / scale;
        scale *= factor;
        tx = px - mx * scale;
        ty = py + my * scale;
        invalidate();
    }

    private void pickAt(float px, float py)
    {
        if (map == null)
            return;
        int[] hit = map.pick((px - tx) / scale, (ty - py) / scale, scale);
        if (hit == null)
            return;
        select(hit[0], hit[1]);
        if (picked != null)
            picked.picked(hit[0], hit[1]);
    }

    public boolean onTouchEvent(MotionEvent e)
    {
        zoom.onTouchEvent(e);
        if (!zoom.isInProgress())
            gestures.onTouchEvent(e);
        return true;
    }

    protected void onSizeChanged(int w, int h, int ow, int oh)
    {
        if (!fitted)
            fit();
    }

    private float x(double v) { return (float) ((int) (v * scale) + tx); }
    private float y(double v) { return (float) (ty - (int) (v * scale)); }

    private void quad(Canvas c, double[] q)
    {
        for (int k = 0; k < 4; k++)
        {
            int n = (k + 1) % 4;
            c.drawLine(x(q[2 * k]), y(q[2 * k + 1]), x(q[2 * n]), y(q[2 * n + 1]), paint);
        }
    }

    private void line(Canvas c, double[] l)
    {
        c.drawLine(x(l[0]), y(l[1]), x(l[2]), y(l[3]), paint);
    }

    protected void onDraw(Canvas c)
    {
        c.drawColor(Color.WHITE);
        if (map == null)
            return;
        for (int i = 0; i < map.road.length; i++)
        {
            paint.setColor(map.roadSector[i] == selTrack ? ORANGE : Color.BLACK);
            quad(c, map.road[i]);
            if (!map.layoutMode)
            {
                paint.setColor(map.lineSector[i] == selLine ? GREEN : map.lineOffRoad[i] ? ORANGE : Color.RED);
                line(c, map.line[i]);
            }
        }
        for (int i = 0; i < map.pit.length; i++)
        {
            paint.setColor(map.pitSector[i] == selPit ? PINK : Color.BLUE);
            quad(c, map.pit[i]);
        }
        paint.setColor(MAGENTA);
        for (double[] l : map.preview)
            line(c, l);
    }
}
