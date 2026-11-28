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
    /** A game options, name, setup or championship file open in GameFileActivity, and where it is. */
    static cfevolution.edit.GameFilePage gameFile;
    static java.io.File gameFilePath;
    /** Where a file opened from outside the game folder goes back to when saved (Open a Name File), else null. */
    static android.net.Uri gameFileUri;
    /** The horizon being painted in HorizonActivity, and the picture file open in PictureActivity. */
    static cfevolution.edit.HorizonPage horizon;
    static cfevolution.edit.PictureFilePage pictureFile;
    /** The chosen folder's files, copied in for the core; written back after each change. */
    static FolderMirror mirror;
    /** The picked folder the open game folder came from (kept when Options clear the list of folders). */
    static android.net.Uri openTree;

    static final Message NOT_WRITTEN = Message.error("Changes Not Written",
        "The changes could not be written to the game folder. They are kept here: save again to try once more. "
        + "If it keeps happening, open the game folder again from the start screen.");

    private Session()
    {
    }

    /** The picture codec the core uses on Android (installed once; every screen that needs pictures calls this). */
    static void pictures()
    {
        cfevolution.data.image.Pictures.setCodec(new AndroidPictures());
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
    /** A question whose first option goes ahead (the second cancels). */
    static void choose(Activity a, Message q, final Runnable first)
    {
        new android.app.AlertDialog.Builder(a).setTitle(q.title).setMessage(q.text)
            .setPositiveButton(q.options[0], new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface d, int w) { first.run(); }
            })
            .setNegativeButton(q.options[1], null).show();
    }

    /** Revert File (2026-11-23): asks, puts the file back as it was when opened, writes that to the game
        folder, then opens it again (reopen). */
    static void revertFile(final Activity a, final java.io.File f, final Runnable reopen)
    {
        choose(a, cfevolution.edit.Revert.fileQuestion(f), new Runnable() {
            public void run()
            {
                Message failed = cfevolution.edit.Revert.revertFile(f);
                if (failed != null)
                    Dialogs.show(a, failed, null);
                else
                    writeBack(a, reopen);
            }
        });
    }

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
