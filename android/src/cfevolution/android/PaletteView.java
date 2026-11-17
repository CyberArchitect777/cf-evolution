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
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;
import cfevolution.data.executable.GameLiveries;
import cfevolution.edit.LiveriesPage;

/**
 * The game's 256 palette colours as a 16 x 16 grid (the desktop's
 * PaletteChooser). A filled corner mark: the game changes that colour in a
 * race; an open one: worth knowing. Tapping a colour highlights it.
 */
class PaletteView extends View
{
    interface Picked
    {
        void colour(int index);
    }

    private final GameLiveries palette;
    private final Picked picked;
    private int highlighted;
    /** Whether the colours the game changes in a race are marked (the liveries and the horizon). */
    boolean marks = true;
    private final Paint fill = new Paint(), line = new Paint();

    PaletteView(Context c, GameLiveries palette, int current, Picked picked)
    {
        super(c);
        this.palette = palette;
        this.picked = picked;
        highlighted = current;
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(Ui.dp(c, 1));
        line.setAntiAlias(true);
        fill.setAntiAlias(true);
    }

    int highlighted()
    {
        return highlighted;
    }

    protected void onMeasure(int w, int h)
    {
        int width = MeasureSpec.getSize(w);
        setMeasuredDimension(width, width);
    }

    protected void onDraw(Canvas canvas)
    {
        float cell = getWidth() / 16f, mark = cell * 0.4f;
        for (int i = 0; i < 256; i++)
        {
            float x = (i % 16) * cell, y = (i / 16) * cell;
            fill.setStyle(Paint.Style.FILL);
            fill.setColor(0xFF000000 | palette.getPaletteRgb(i));
            canvas.drawRect(x + 1, y + 1, x + cell - 1, y + cell - 1, fill);
            boolean warn = marks && LiveriesPage.specialUse(i) != null, aside = marks && LiveriesPage.note(i) != null;
            if (warn || aside)
            {
                Path p = new Path();
                p.moveTo(x + 1, y + 1);
                p.lineTo(x + 1 + mark, y + 1);
                p.lineTo(x + 1, y + 1 + mark);
                p.close();
                fill.setColor(Color.WHITE);
                if (warn)
                    canvas.drawPath(p, fill);
                line.setColor(Color.BLACK);
                canvas.drawPath(p, line);
            }
        }
        float x = (highlighted % 16) * cell, y = (highlighted / 16) * cell;
        line.setColor(Color.BLACK);
        line.setStrokeWidth(Ui.dp(getContext(), 2));
        canvas.drawRect(x, y, x + cell, y + cell, line);
        line.setColor(Color.WHITE);
        canvas.drawRect(x + Ui.dp(getContext(), 2), y + Ui.dp(getContext(), 2), x + cell - Ui.dp(getContext(), 2),
                        y + cell - Ui.dp(getContext(), 2), line);
        line.setStrokeWidth(Ui.dp(getContext(), 1));
    }

    public boolean onTouchEvent(MotionEvent e)
    {
        float cell = getWidth() / 16f;
        int col = (int) (e.getX() / cell), row = (int) (e.getY() / cell);
        if (col >= 0 && col < 16 && row >= 0 && row < 16)
        {
            highlighted = row * 16 + col;
            invalidate();
            if (e.getAction() == MotionEvent.ACTION_UP)
                picked.colour(highlighted);
        }
        return true;
    }
}
