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

package cfevolution.edit;

import cfevolution.data.GameFolder;
import cfevolution.data.dataset.Dataset;
import cfevolution.data.dataset.DatasetGroup;
import cfevolution.data.dataset.GameDataset;
import cfevolution.data.dataset.OtherEditorFiles;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Game folder > Export Dataset (2026-11-17, from the desktop's DatasetDialog
 * word for word): what to save as (a dataset, or an ArgEditor or F1ED
 * carset), a title, a tick box per group (pictures and circuits unticked
 * until chosen; another editor's carset holds what it holds), the file's
 * name, and the messages.
 */
public final class DatasetExport
{
    public static final String TITLE = "Export Dataset";
    public static final String[] FORMATS = { "CF Evolution dataset (.cfe)", "ArgEditor carset (.arged)", "F1ED carset (.1ED)" };
    public static final int CFE = 0, ARGEDITOR = 1, F1ED = 2;
    public static final String SAVE_AS = "Save as:", TITLE_LABEL = "Title (optional):", INCLUDE = "Include", OK = "Export...";
    public static final String NOT_IN_FOLDER = "This game folder does not have the file for this";
    public static final String CFE_NOTE = "The dataset is a text file (.cfe) anyone can read. With pictures or circuits it is saved as a zip<br>"
        + "holding the text file and folders of PNG pictures and circuit files.";
    public static final String ARGEDITOR_NOTE = "For ArgEditor: the first 18 teams with their drivers, the colours and the points.<br>"
        + "Drivers without a seat, teams 19 and 20, and anything else are left out.";
    public static final String F1ED_NOTE = "For F1ED: all 20 teams and 40 drivers, the colours, and the game settings F1ED has.<br>"
        + "F1ED has no points table; anything else is left out.";
    public static final Message NO_GAME = Message.error(TITLE, "GP.EXE could not be read, so nothing was exported.");
    public static final Message NOTHING_TICKED = Message.info(TITLE, "Tick at least one thing to include.");

    public final GameFolder game;
    public final GameDataset data;
    private final Set<DatasetGroup> possible;
    private final Map<DatasetGroup, Boolean> ticked = new EnumMap<DatasetGroup, Boolean>(DatasetGroup.class);
    private final Map<DatasetGroup, Boolean> enabled = new EnumMap<DatasetGroup, Boolean>(DatasetGroup.class);
    private int format = CFE;
    public String title = "";

    private DatasetExport(GameFolder game, GameDataset data)
    {
        this.game = game;
        this.data = data;
        this.possible = data.available();
        for (DatasetGroup g : DatasetGroup.values())
        {
            boolean can = possible.contains(g);
            ticked.put(g, can && !g.files);
            enabled.put(g, can);
        }
    }

    /** Export Dataset for this folder; null after setting problem[0] (NO_GAME) when GP.EXE cannot be read. */
    public static DatasetExport of(GameFolder game, Message[] problem)
    {
        try
        {
            return new DatasetExport(game, new GameDataset(game));
        }
        catch (IOException e)
        {
            problem[0] = NO_GAME;
            return null;
        }
    }

    /** A group's tick box text: its name in bold, then what it holds. */
    public static String label(DatasetGroup g, boolean html)
    {
        return html ? "<html><b>" + g.label + "</b>: " + g.detail + "</html>" : g.label + ": " + g.detail;
    }

    public boolean possible(DatasetGroup g)
    {
        return possible.contains(g);
    }

    public boolean ticked(DatasetGroup g)
    {
        return ticked.get(g);
    }

    public boolean enabled(DatasetGroup g)
    {
        return enabled.get(g);
    }

    public void tick(DatasetGroup g, boolean on)
    {
        if (enabled.get(g))
            ticked.put(g, on);
    }

    public int format()
    {
        return format;
    }

    /** The note under the groups for the format chosen (with the desktop's line break as "<br>"). */
    public String note()
    {
        return format == ARGEDITOR ? ARGEDITOR_NOTE : format == F1ED ? F1ED_NOTE : CFE_NOTE;
    }

    private Set<DatasetGroup> fixedGroups()
    {
        return format == ARGEDITOR ? OtherEditorFiles.ARGEDITOR_GROUPS : format == F1ED ? OtherEditorFiles.F1ED_GROUPS : null;
    }

    /** Picks the format: the boxes are ticked and greyed for it (another editor's carset holds what it holds). */
    public void setFormat(int f)
    {
        format = f;
        Set<DatasetGroup> fixed = fixedGroups();
        for (DatasetGroup g : DatasetGroup.values())
        {
            boolean can = possible.contains(g);
            if (fixed == null)
            {
                enabled.put(g, can);
                ticked.put(g, can && !g.files);
            }
            else if (fixed.contains(g))
            {
                enabled.put(g, false);
                ticked.put(g, true);
            }
            else
            {
                enabled.put(g, format == ARGEDITOR && g == DatasetGroup.HELMET_PICTURES && can);
                ticked.put(g, false);
            }
        }
    }

    /** What will be saved: a carset's own groups, and whatever is ticked and can be. */
    public Set<DatasetGroup> chosen()
    {
        Set<DatasetGroup> out = EnumSet.noneOf(DatasetGroup.class);
        if (fixedGroups() != null)
            out.addAll(fixedGroups());
        for (DatasetGroup g : DatasetGroup.values())
            if (ticked.get(g) && enabled.get(g))
                out.add(g);
        return out;
    }

    /** OK: NOTHING_TICKED when nothing would be saved, else null. */
    public Message problem()
    {
        return chosen().isEmpty() ? NOTHING_TICKED : null;
    }

    // --- the file -----------------------------------------------------------------------

    /** True when a dataset with these groups is a zip (pictures or circuits). */
    public boolean zip()
    {
        boolean zip = false;
        for (DatasetGroup g : chosen())
            zip |= g.files;
        return zip;
    }

    /** The file chooser's filter: "Dataset (*.cfe)", "Dataset with pictures (*.zip)" or the carset's. */
    public String filter()
    {
        if (format != CFE)
            return FORMATS[format].replace("(.", "(*.");
        return zip() ? "Dataset with pictures (*.zip)" : "Dataset (*.cfe)";
    }

    /** The extension the file gets: ".cfe", ".zip", ".arged" or ".1ED". */
    public String extension()
    {
        if (format != CFE)
            return format == ARGEDITOR ? ".arged" : ".1ED";
        return zip() ? ".zip" : Dataset.EXTENSION;
    }

    /** The name offered: the title made safe, or "dataset" / "carset". */
    public String suggestedName()
    {
        String base = title.trim().replaceAll("[^A-Za-z0-9 _-]", "").trim();
        return (base.isEmpty() ? (format == CFE ? "dataset" : "carset") : base) + extension();
    }

    private static String baseName(String n)
    {
        int dot = n.lastIndexOf('.');
        return dot > 0 ? n.substring(0, dot) : n;
    }

    /** The name the file is written as, from the name chosen: its own extension replaced. */
    public String writtenName(String chosen)
    {
        if (format != CFE)
            return baseName(chosen) + extension();
        return baseName(chosen) + (data.export(chosen(), title).needsZip() ? ".zip" : Dataset.EXTENSION);
    }

    public static Message replaceQuestion(String name)
    {
        return Message.choose(Message.Kind.WARNING, "Replace File", name + " already exists. Replace it?", "Replace", "Cancel");
    }

    public static Message notSaved(String name)
    {
        return Message.error(TITLE, name + " could not be saved.");
    }

    /** A carset's bytes, with what was left out added to notes; for a dataset use writeDataset. */
    public byte[] carset(List<String> notes)
    {
        Dataset dataset = data.export(chosen(), title);
        return format == ARGEDITOR ? OtherEditorFiles.writeArgEditor(dataset, data.helmetPalette(), notes)
                                   : OtherEditorFiles.writeF1Ed(dataset, data.getExecutable().getGameLiveries(), notes);
    }

    /** Writes the dataset to the chosen file (named as writtenName says); returns the file written. */
    public File writeDataset(File chosen) throws IOException
    {
        return data.export(chosen(), title).write(chosen);
    }

    /** Said once the file is written. */
    public Message saved(String name, List<String> notes)
    {
        if (format == CFE)
            return Message.info(TITLE, "The dataset was saved as " + name + ".");
        StringBuilder msg = new StringBuilder("The carset was saved as " + name + ".");
        for (String n : notes)
            msg.append("\n").append(n);
        return Message.info(TITLE, msg.toString());
    }

    /** Every group, in the dialog's order. */
    public static List<DatasetGroup> groups()
    {
        List<DatasetGroup> l = new ArrayList<DatasetGroup>();
        for (DatasetGroup g : DatasetGroup.values())
            l.add(g);
        return l;
    }
}
