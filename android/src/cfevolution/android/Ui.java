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
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small helpers for screens made in code. */
final class Ui
{
    private Ui()
    {
    }

    static int dp(Context c, int dp)
    {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, c.getResources().getDisplayMetrics()));
    }

    static TextView text(Context c, String s, int sp)
    {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setPadding(0, dp(c, 4), 0, dp(c, 4));
        return t;
    }

    static TextView heading(Context c, String s)
    {
        TextView t = text(c, s, 17);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(c, 14), 0, dp(c, 4));
        return t;
    }

    static Button button(Context c, String s, View.OnClickListener l)
    {
        Button b = new Button(c);
        b.setText(s);
        b.setAllCaps(false);
        b.setOnClickListener(l);
        return b;
    }

    /** A labelled text box for a form; returns the box. */
    static EditText field(LinearLayout form, String label, String value, boolean number)
    {
        Context c = form.getContext();
        form.addView(text(c, label, 14));
        EditText e = new EditText(c);
        e.setSingleLine(true);
        e.setText(value);
        if (number)
            e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
                           | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        form.addView(e);
        return e;
    }

    static LinearLayout column(Context c)
    {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        int p = dp(c, 16);
        l.setPadding(p, p, p, p);
        return l;
    }
}
