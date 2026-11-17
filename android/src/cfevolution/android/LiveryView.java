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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import cfevolution.data.executable.GameSprites;
import cfevolution.edit.LiveriesPage;

/**
 * One of the game's pictures in a row's colours, enlarged with square pixels
 * (the desktop's LiveryPicture). Tapping a part reports which part it is.
 */
class LiveryView extends View
{
    interface Tapped
    {
        void part(int part);
    }

    private final LiveriesPage page;
    private final GameSprites sprites;
    private final Tapped tapped;
    private int tab, row, position;
    private int[] box = { 1, 1 };
    private Bitmap bitmap;
    private GameSprites.Sprite sprite;
    private final Paint pixels = new Paint();

    LiveryView(Context c, LiveriesPage page, GameSprites sprites, Tapped tapped)
    {
        super(c);
        this.page = page;
        this.sprites = sprites;
        this.tapped = tapped;
        pixels.setFilterBitmap(false);
        pixels.setAntiAlias(false);
        setBackgroundColor(0xFF9AA3AD);
    }

    void show(int tab, int row, int position)
    {
        if (tab != this.tab || box == null)
            box = LiveriesPage.box(tab, sprites);
        this.tab = tab;
        this.row = row;
        this.position = position;
        sprite = sprites.get(LiveriesPage.pictureAt(tab, position));
        bitmap = sprite == null ? null
            : Bitmap.createBitmap(page.pixels(tab, row, sprite, LiveriesPage.mirroredAt(tab, position)), sprite.width, sprite.height,
                                  Bitmap.Config.ARGB_8888);
        requestLayout();
        invalidate();
    }

    /**
     * Square pixels, as large as fills a little over half the screen's height
     * (2026-11-18: fitted to the width, the car was a low strip); a picture
     * then wider than the screen scrolls sideways (the activity puts this in
     * a HorizontalScrollView).
     */
    private int scale()
    {
        int tall = (int) (getResources().getDisplayMetrics().heightPixels * 0.55);
        return Math.max(1, tall / Math.max(1, box[1]));
    }

    protected void onMeasure(int w, int h)
    {
        int across = MeasureSpec.getSize(w);
        if (across == 0)
            across = getResources().getDisplayMetrics().widthPixels - Ui.dp(getContext(), 32);
        setMeasuredDimension(Math.max(across, box[0] * scale() + Ui.dp(getContext(), 16)),
                             box[1] * scale() + Ui.dp(getContext(), 24));
    }

    protected void onDraw(Canvas canvas)
    {
        if (bitmap == null)
            return;
        int s = scale();
        int left = (getWidth() - sprite.width * s) / 2, top = (getHeight() - sprite.height * s) / 2;
        canvas.drawBitmap(bitmap, null, new Rect(left, top, left + sprite.width * s, top + sprite.height * s), pixels);
    }

    public boolean onTouchEvent(MotionEvent e)
    {
        if (sprite == null)
            return false;
        if (e.getAction() == MotionEvent.ACTION_UP)
        {
            int s = scale();
            int left = (getWidth() - sprite.width * s) / 2, top = (getHeight() - sprite.height * s) / 2;
            int x = (int) Math.floor((e.getX() - left) / s), y = (int) Math.floor((e.getY() - top) / s);
            int part = LiveriesPage.partAt(tab, sprite, LiveriesPage.mirroredAt(tab, position), x, y);
            if (part >= 0)
                tapped.part(part);
        }
        return true;
    }
}
