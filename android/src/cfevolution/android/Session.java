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
import cfevolution.edit.Message;
import cfevolution.screens.ExecutableScreen;
import cfevolution.screens.FolderMirror;
import cfevolution.screens.FolderScreen;
import cfevolution.screens.TrackScreen;

/** What is open, shared by the screens (one game folder at a time). */
final class Session
{
    static FolderScreen folder;
    static TrackScreen track;
    static ExecutableScreen executable;
    /** The chosen folder's files, copied in for the core; written back after each change. */
    static FolderMirror mirror;

    static final Message NOT_WRITTEN = Message.error("Changes Not Written",
        "The changes could not be written to the game folder. They are kept here: save again to try once more. "
        + "If it keeps happening, open the game folder again from the start screen.");

    private Session()
    {
    }

    /** Something slow off the screen's thread, with a note while it runs. */
    interface Work
    {
        void run() throws Exception;
    }

    /** Runs work on a worker with a note showing; then done(null) or done(the failure) on the screen's thread. */
    static void slowly(final Activity a, String note, final Work work, final Done done)
    {
        final AlertDialog wait = new AlertDialog.Builder(a).setMessage(note).setCancelable(false).create();
        wait.show();
        new Thread(new Runnable() {
            public void run()
            {
                Exception failed = null;
                try
                {
                    work.run();
                }
                catch (Exception e)
                {
                    failed = e;
                }
                final Exception f = failed;
                a.runOnUiThread(new Runnable() {
                    public void run()
                    {
                        if (!a.isFinishing())
                            wait.dismiss();
                        done.done(f);
                    }
                });
            }
        }, "CF Evolution files").start();
    }

    interface Done
    {
        void done(Exception failed);
    }

    /** Writes the changed files back to the game folder; then runs after (may be null), or says they were not written. */
    static void writeBack(final Activity a, final Runnable after)
    {
        if (mirror == null)
        {
            if (after != null)
                after.run();
            return;
        }
        slowly(a, "Writing the changes to the game folder...", new Work() {
            public void run() throws Exception { mirror.push(); }
        }, new Done() {
            public void done(Exception failed)
            {
                if (failed == null)
                {
                    if (after != null)
                        after.run();
                }
                else
                    Dialogs.show(a, NOT_WRITTEN, null);
            }
        });
    }
}
