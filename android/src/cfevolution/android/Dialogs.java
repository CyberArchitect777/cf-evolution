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
import cfevolution.edit.Message;
import cfevolution.screens.Flow;

/**
 * Shows the core's Messages as Android dialogs, and runs a screen model's
 * Flow one message at a time (Android's dialogs do not wait for an answer).
 */
final class Dialogs
{
    private Dialogs()
    {
    }

    /** Line breaks in the desktop's text are for its narrow message boxes; a
        single break becomes a space here, and paragraphs stay. */
    static String text(String s)
    {
        return s.replace("\n\n", "\u0000").replace(" \n", " ").replace("\n", " ").replace("\u0000", "\n\n");
    }

    /** Shows a message; then runs after (which may be null). Null shows nothing. */
    static void show(Activity a, Message m, final Runnable after)
    {
        if (m == null)
        {
            if (after != null)
                after.run();
            return;
        }
        run(a, Flow.tell(m), after);
    }

    /** Runs a flow to its end, then runs done (which may be null). */
    static void run(final Activity a, final Flow flow, final Runnable done)
    {
        if (flow.done())
        {
            if (done != null)
                done.run();
            return;
        }
        final Message m = flow.message();
        final int[] choice = { -1 };
        AlertDialog.Builder b = new AlertDialog.Builder(a)
            .setTitle(m.title)
            .setMessage(text(m.text))
            .setCancelable(false);
        if (m.options != null)
        {
            // up to three buttons: the first, the second and (if any) the third
            b.setPositiveButton(m.options[0], answer(choice, 0));
            if (m.options.length == 2)
                b.setNegativeButton(m.options[1], answer(choice, 1));
            else if (m.options.length >= 3)
            {
                b.setNeutralButton(m.options[1], answer(choice, 1));
                b.setNegativeButton(m.options[2], answer(choice, 2));
            }
        }
        else if (m.yesNo)
        {
            b.setPositiveButton("Yes", answer(choice, Flow.YES));
            b.setNegativeButton("No", answer(choice, Flow.NO));
        }
        else
            b.setPositiveButton("OK", answer(choice, Flow.OK));
        AlertDialog d = b.create();
        d.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface di)
            {
                flow.answer(choice[0] < 0 ? (m.yesNo ? Flow.NO : m.options != null ? m.options.length - 1 : Flow.OK) : choice[0]);
                run(a, flow, done);
            }
        });
        d.show();
    }

    private static DialogInterface.OnClickListener answer(final int[] choice, final int value)
    {
        return new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface di, int which)
            {
                choice[0] = value;
            }
        };
    }

    /** A yes/no question of the app's own; yes runs onYes. */
    static void ask(Activity a, String title, String text, String yes, String no, final Runnable onYes)
    {
        new AlertDialog.Builder(a).setTitle(title).setMessage(text)
            .setPositiveButton(yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface di, int which) { onYes.run(); }
            })
            .setNegativeButton(no, null)
            .show();
    }
}
