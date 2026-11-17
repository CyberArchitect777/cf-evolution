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
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import cfevolution.edit.Message;
import cfevolution.screens.ExecutableScreen;
import cfevolution.screens.FolderScreen;
import cfevolution.screens.TrackScreen;

/** The game folder: GP.EXE and the 16 circuits, as the desktop's game folder tab lists them. */
public class FolderActivity extends Activity
{
    protected void onCreate(Bundle state)
    {
        super.onCreate(state);
        if (Session.folder == null)
        {
            finish();
            return;
        }
        setTitle(Session.folder.title());
        list = Ui.column(this);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(list);
        setContentView(scroll);
        fill();
    }

    private LinearLayout list;

    /** Back from a file: a name file saved there now shows in the list. */
    protected void onResume()
    {
        super.onResume();
        if (list != null)
            fill();
    }

    private void fill()
    {
        LinearLayout col = list;
        col.removeAllViews();
        boolean anySaved = false;
        for (cfevolution.edit.GameFolderList.Row r : Session.folder.rows())
            anySaved |= !r.heading && r.what >= cfevolution.edit.GameFolderList.SAVED;
        for (final cfevolution.edit.GameFolderList.Row row : Session.folder.rows())
        {
            if (row.heading)
            {
                col.addView(Ui.heading(this, row.label));
                if (row.label.equals("Circuits"))
                {
                    col.addView(Ui.button(this, cfevolution.edit.AllCircuitsCars.TITLE, new View.OnClickListener() {
                        public void onClick(View v) { startActivity(new Intent(FolderActivity.this, CircuitsActivity.class)); }
                    }));
                    TextView tip = Ui.text(this, cfevolution.edit.AllCircuitsCars.BUTTON_TIP + ".", 13);
                    tip.setTextColor(android.graphics.Color.GRAY);
                    col.addView(tip);
                }
                if (row.label.equals(cfevolution.edit.GameFolderList.DATASETS))
                {
                    col.addView(Ui.button(this, cfevolution.edit.DatasetExport.TITLE, new View.OnClickListener() {
                        public void onClick(View v) { exportDataset(); }
                    }));
                    col.addView(Ui.button(this, cfevolution.edit.DatasetImport.TITLE, new View.OnClickListener() {
                        public void onClick(View v) { Documents.open(FolderActivity.this, IMPORT_DATASET); }
                    }));
                }
                if (row.label.equals(cfevolution.edit.GameFolderList.SAVED_HEADING))
                {
                    col.addView(Ui.button(this, cfevolution.edit.GameFolderList.SAVE_NAMES, new View.OnClickListener() {
                        public void onClick(View v) { saveNames(cfevolution.edit.GameFolderList.NAME_SUGGESTED); }
                    }));
                    col.addView(Ui.button(this, cfevolution.edit.GameFolderList.OPEN_NAMES, new View.OnClickListener() {
                        public void onClick(View v) { Documents.open(FolderActivity.this, OPEN_NAMES); }
                    }));
                    TextView tip = Ui.text(this, cfevolution.edit.GameFolderList.SAVE_NAMES_TIP + ".", 13);
                    tip.setTextColor(android.graphics.Color.GRAY);
                    col.addView(tip);
                    if (!anySaved)
                        col.addView(Ui.text(this, cfevolution.edit.GameFolderList.NO_SAVED_FILES, 13));
                }
                continue;
            }
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            int p = Ui.dp(this, 8);
            item.setPadding(0, p, 0, p);
            TextView label = Ui.text(this, (row.number.length() > 0 ? row.number + "  " : "") + row.label, 16);
            item.addView(label);
            item.addView(Ui.text(this, row.detail, 13));
            if (row.file != null)
            {
                item.setClickable(true);
                item.setBackgroundResource(android.R.drawable.list_selector_background);
                item.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) { open(row); }
                });
            }
            else
                label.setEnabled(false);
            col.addView(item);
        }
    }

    // --- datasets --------------------------------------------------------------------

    private static final int EXPORT_DATASET = 11, IMPORT_DATASET = 12, OPEN_NAMES = 13;
    private cfevolution.edit.DatasetExport exporting;

    private android.widget.CheckBox groupBox(final cfevolution.data.dataset.DatasetGroup g, boolean ticked, boolean enabled,
                                             final java.util.Map<cfevolution.data.dataset.DatasetGroup, Boolean> ticks)
    {
        android.widget.CheckBox b = new android.widget.CheckBox(this);
        b.setText(cfevolution.edit.DatasetExport.label(g, false));
        b.setChecked(ticked);
        b.setEnabled(enabled);
        b.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(android.widget.CompoundButton c, boolean on) { ticks.put(g, on); }
        });
        return b;
    }

    private void exportDataset()
    {
        Message[] problem = new Message[1];
        final cfevolution.edit.DatasetExport x = Session.folder.exportDataset(problem);
        if (x == null)
        {
            Dialogs.show(this, problem[0], null);
            return;
        }
        exportForm(x);
    }

    private void exportForm(final cfevolution.edit.DatasetExport x)
    {
        LinearLayout form = Ui.column(this);
        form.addView(Ui.text(this, cfevolution.edit.DatasetExport.SAVE_AS, 14));
        android.widget.Spinner format = new android.widget.Spinner(this);
        android.widget.ArrayAdapter<String> formats = new android.widget.ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, cfevolution.edit.DatasetExport.FORMATS);
        formats.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        format.setAdapter(formats);
        format.setSelection(x.format());
        form.addView(format);
        final android.widget.EditText title = Ui.field(form, cfevolution.edit.DatasetExport.TITLE_LABEL, x.title, false);
        form.addView(Ui.heading(this, cfevolution.edit.DatasetExport.INCLUDE));
        final java.util.Map<cfevolution.data.dataset.DatasetGroup, Boolean> ticks = new java.util.EnumMap<cfevolution.data.dataset.DatasetGroup, Boolean>(cfevolution.data.dataset.DatasetGroup.class);
        for (cfevolution.data.dataset.DatasetGroup g : cfevolution.edit.DatasetExport.groups())
        {
            ticks.put(g, x.ticked(g));
            form.addView(groupBox(g, x.ticked(g), x.enabled(g), ticks));
        }
        TextView note = Ui.text(this, x.note().replace("<br>", " "), 13);
        note.setTextColor(android.graphics.Color.GRAY);
        form.addView(note);
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.addView(form);
        final android.app.AlertDialog[] d = new android.app.AlertDialog[1];
        final Runnable take = new Runnable() {
            public void run()
            {
                x.title = title.getText().toString();
                for (java.util.Map.Entry<cfevolution.data.dataset.DatasetGroup, Boolean> e : ticks.entrySet())
                    x.tick(e.getKey(), e.getValue());
            }
        };
        format.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(android.widget.AdapterView<?> p, View v, int position, long id)
            {
                if (position == x.format())
                    return;
                take.run();
                x.setFormat(position);
                d[0].dismiss();
                exportForm(x);
            }
            public void onNothingSelected(android.widget.AdapterView<?> p) { }
        });
        d[0] = new android.app.AlertDialog.Builder(this).setTitle(cfevolution.edit.DatasetExport.TITLE).setView(scroll)
            .setPositiveButton(cfevolution.edit.DatasetExport.OK.replace("...", ""), new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface di, int which)
                {
                    take.run();
                    Message m = x.problem();
                    if (m != null)
                    {
                        Dialogs.show(FolderActivity.this, m, new Runnable() { public void run() { exportForm(x); } });
                        return;
                    }
                    exporting = x;
                    Documents.create(FolderActivity.this, EXPORT_DATASET, x.suggestedName());
                }
            })
            .setNegativeButton("Cancel", null).show();
    }

    protected void onActivityResult(int request, int result, Intent data)
    {
        super.onActivityResult(request, result, data);
        android.net.Uri uri = Documents.picked(result, data);
        if (uri == null || Session.folder == null)
            return;
        String name = Documents.name(this, uri);
        if (request == EXPORT_DATASET && exporting != null)
        {
            java.util.List<String> notes = new java.util.ArrayList<String>();
            try
            {
                byte[] bytes;
                if (exporting.format() == cfevolution.edit.DatasetExport.CFE)
                {
                    java.io.File temp = exporting.writeDataset(new java.io.File(getCacheDir(), exporting.writtenName(name)));
                    bytes = java.nio.file.Files.readAllBytes(temp.toPath());
                    temp.delete();
                }
                else
                    bytes = exporting.carset(notes);
                Documents.write(this, uri, bytes);
                Dialogs.show(this, exporting.saved(name, notes), null);
            }
            catch (java.io.IOException | RuntimeException e)
            {
                Dialogs.show(this, cfevolution.edit.DatasetExport.notSaved(name), null);
            }
            return;
        }
        if (request == OPEN_NAMES)
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
            Message problem = cfevolution.edit.GameFolderList.nameFileProblem(name, bytes);
            if (problem != null)
            {
                Dialogs.show(this, problem, null);
                return;
            }
            try
            {
                java.io.File copy = new java.io.File(getCacheDir(), name.replaceAll("[/\\\\]", "_"));
                java.nio.file.Files.write(copy.toPath(), bytes);
                Message[] p = new Message[1];
                cfevolution.edit.GameFilePage page = Session.folder.openFile(copy, p);
                if (page == null)
                {
                    Dialogs.show(this, p[0], null);
                    return;
                }
                Session.gameFile = page;
                Session.gameFilePath = copy;
                Session.gameFileUri = uri;      // saved back where it came from
                startActivity(new Intent(this, GameFileActivity.class));
            }
            catch (java.io.IOException e)
            {
                Dialogs.show(this, cfevolution.edit.GameFolderList.nameFileProblem(name, null), null);
            }
            return;
        }
        if (request == IMPORT_DATASET)
        {
            java.io.File copy = new java.io.File(getCacheDir(), name.replaceAll("[/\\\\]", "_"));
            Message[] problem = new Message[1];
            cfevolution.edit.DatasetImport x = null;
            try
            {
                java.nio.file.Files.write(copy.toPath(), Documents.read(this, uri));
                x = Session.folder.importDataset(copy, problem);
            }
            catch (java.io.IOException e)
            {
                problem[0] = Message.error(cfevolution.edit.DatasetImport.TITLE, name + " could not be read as a dataset or carset.");
            }
            if (x == null)
                Dialogs.show(this, problem[0], null);
            else
                importForm(x);
        }
    }

    private void importForm(final cfevolution.edit.DatasetImport x)
    {
        LinearLayout form = Ui.column(this);
        form.addView(Ui.text(this, x.heading(false), 14));
        form.addView(Ui.heading(this, cfevolution.edit.DatasetImport.INCLUDE));
        final java.util.Map<cfevolution.data.dataset.DatasetGroup, Boolean> ticks = new java.util.EnumMap<cfevolution.data.dataset.DatasetGroup, Boolean>(cfevolution.data.dataset.DatasetGroup.class);
        final java.util.Map<cfevolution.data.dataset.DatasetGroup, android.widget.CheckBox> boxes = new java.util.EnumMap<cfevolution.data.dataset.DatasetGroup, android.widget.CheckBox>(cfevolution.data.dataset.DatasetGroup.class);
        for (cfevolution.data.dataset.DatasetGroup g : x.groups())
        {
            ticks.put(g, x.ticked(g));
            android.widget.CheckBox b = groupBox(g, x.ticked(g), x.enabled(g), ticks);
            boxes.put(g, b);
            form.addView(b);
        }
        form.addView(Ui.heading(this, cfevolution.edit.DatasetImport.BRING_IN));
        android.widget.RadioGroup bring = new android.widget.RadioGroup(this);
        final android.widget.RadioButton all = new android.widget.RadioButton(this), one = new android.widget.RadioButton(this);
        all.setText(cfevolution.edit.DatasetImport.EVERYTHING);
        one.setText(cfevolution.edit.DatasetImport.ONE_TEAM);
        one.setEnabled(x.canBringOneTeam());
        bring.addView(all);
        bring.addView(one);
        (x.oneTeam() ? one : all).setChecked(true);
        form.addView(bring);
        final android.widget.Spinner from = new android.widget.Spinner(this), to = new android.widget.Spinner(this);
        from.setAdapter(new android.widget.ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, x.fromTeams()));
        to.setAdapter(new android.widget.ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, x.toTeams()));
        form.addView(from);
        form.addView(Ui.text(this, cfevolution.edit.DatasetImport.IN_PLACE_OF, 14));
        form.addView(to);
        TextView oneNote = Ui.text(this, cfevolution.edit.DatasetImport.ONE_TEAM_NOTE, 13);
        oneNote.setTextColor(android.graphics.Color.GRAY);
        form.addView(oneNote);
        final Runnable sync = new Runnable() {
            public void run()
            {
                x.setOneTeam(one.isChecked());
                from.setEnabled(one.isChecked());
                to.setEnabled(one.isChecked());
                for (java.util.Map.Entry<cfevolution.data.dataset.DatasetGroup, android.widget.CheckBox> e : boxes.entrySet())
                    e.getValue().setEnabled(x.enabled(e.getKey()));
            }
        };
        bring.setOnCheckedChangeListener(new android.widget.RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(android.widget.RadioGroup g, int id) { sync.run(); }
        });
        sync.run();
        form.addView(Ui.heading(this, cfevolution.edit.DatasetImport.NAMES));
        final android.widget.CheckBox intoGame = new android.widget.CheckBox(this), intoFile = new android.widget.CheckBox(this);
        intoGame.setText(cfevolution.edit.DatasetImport.NAMES_INTO_GAME);
        intoGame.setChecked(x.namesIntoGame);
        intoFile.setText(cfevolution.edit.DatasetImport.NAMES_INTO_FILE);
        intoFile.setChecked(x.namesIntoFile);
        form.addView(intoGame);
        form.addView(intoFile);
        final android.widget.EditText nameFile = Ui.field(form, "Name file (.NAM)", x.nameFile, false);
        TextView backup = Ui.text(this, cfevolution.edit.DatasetImport.BACKUP_NOTE, 13);
        backup.setTextColor(android.graphics.Color.GRAY);
        form.addView(backup);
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.addView(form);
        new android.app.AlertDialog.Builder(this).setTitle(cfevolution.edit.DatasetImport.TITLE).setView(scroll)
            .setPositiveButton(cfevolution.edit.DatasetImport.OK, new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface di, int which)
                {
                    for (java.util.Map.Entry<cfevolution.data.dataset.DatasetGroup, Boolean> e : ticks.entrySet())
                        x.tick(e.getKey(), e.getValue());
                    x.from = from.getSelectedItemPosition();
                    x.to = to.getSelectedItemPosition();
                    x.namesIntoGame = intoGame.isChecked();
                    x.namesIntoFile = intoFile.isChecked();
                    x.nameFile = nameFile.getText().toString();
                    Message m = x.problem();
                    if (m != null)
                    {
                        Dialogs.show(FolderActivity.this, m, new Runnable() { public void run() { importForm(x); } });
                        return;
                    }
                    Message[] refused = new Message[1];
                    final java.util.List<String> report = x.run(refused);
                    if (report == null)
                    {
                        Dialogs.show(FolderActivity.this, refused[0], null);
                        return;
                    }
                    fill();
                    Session.writeBack(FolderActivity.this, new Runnable() {
                        public void run()
                        {
                            Dialogs.show(FolderActivity.this, Message.info(cfevolution.edit.DatasetImport.TITLE, String.join("\n\n", report)), null);
                        }
                    });
                }
            })
            .setNegativeButton("Cancel", null).show();
    }

    /** Save the Game's Names as a Name File: the name typed, then the desktop's answers. */
    private void saveNames(String suggested)
    {
        LinearLayout form = Ui.column(this);
        final android.widget.EditText box = Ui.field(form, cfevolution.edit.GameFolderList.NAME_QUESTION, suggested, false);
        new android.app.AlertDialog.Builder(this).setTitle(cfevolution.edit.GameFolderList.SAVE_NAMES).setView(form)
            .setPositiveButton("OK", new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface d, int which)
                {
                    final String typed = box.getText().toString();
                    Dialogs.run(FolderActivity.this, Session.folder.saveNames(typed), new Runnable() {
                        public void run()
                        {
                            final java.io.File written = Session.folder.savedNames();
                            if (written == null)
                                return;
                            fill();
                            Session.writeBack(FolderActivity.this, new Runnable() {
                                public void run() { openFile(written); }
                            });
                        }
                    });
                }
            })
            .setNegativeButton("Cancel", null).show();
    }

    private void openFile(java.io.File file)
    {
        Message[] problem = new Message[1];
        cfevolution.edit.GameFilePage page = Session.folder.openFile(file, problem);
        if (page == null)
        {
            Dialogs.show(this, problem[0], null);
            return;
        }
        Session.gameFile = page;
        Session.gameFilePath = file;
        Session.gameFileUri = null;
        startActivity(new Intent(this, GameFileActivity.class));
    }

    private void open(cfevolution.edit.GameFolderList.Row row)
    {
        Message[] problem = new Message[1];
        if (row.what >= cfevolution.edit.GameFolderList.PICTURES && row.what < cfevolution.edit.GameFolderList.SOUNDS)
        {
            cfevolution.edit.PictureFilePage pictures = Session.folder.openPictures(row.file, problem);
            if (pictures == null)
            {
                Dialogs.show(this, problem[0], null);
                return;
            }
            Session.pictureFile = pictures;
            Session.gameFilePath = row.file;
            startActivity(new Intent(this, PictureActivity.class));
            return;
        }
        if (row.what == cfevolution.edit.GameFolderList.OPTIONS || row.what >= cfevolution.edit.GameFolderList.SOUNDS)
        {
            openFile(row.file);
            return;
        }
        if (row.what == FolderScreen.EXECUTABLE)
        {
            ExecutableScreen e = Session.folder.openExecutable(problem);
            if (e == null)
            {
                Dialogs.show(this, problem[0], null);
                return;
            }
            Session.executable = e;
            startActivity(new Intent(this, ExecutableActivity.class));
        }
        else
        {
            TrackScreen t = Session.folder.openTrack(row.what, problem);
            if (t == null)
            {
                Dialogs.show(this, problem[0], null);
                return;
            }
            Session.track = t;
            startActivity(new Intent(this, TrackActivity.class));
        }
    }
}
