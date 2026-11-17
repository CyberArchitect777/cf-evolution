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
 * Game folder > Import Dataset (2026-11-17, from the desktop's DatasetDialog
 * word for word): a dataset, or an F1ED or ArgEditor carset, read; a tick box
 * per group it holds; everything ticked or one team in place of another; the
 * names into GP.EXE and perhaps a name file; then the import and its report.
 */
public final class DatasetImport
{
    public static final String TITLE = "Import Dataset";
    public static final String FILTER = "Datasets and carsets (*.cfe, *.zip, F1ED *.1ed, ArgEditor *.arged)";
    public static final String INCLUDE = "Include", BRING_IN = "Bring in", EVERYTHING = "Everything ticked", ONE_TEAM = "One team:",
                               IN_PLACE_OF = "in place of", NAMES = "Names", NAMES_INTO_GAME = "Write names into GP.EXE",
                               NAMES_INTO_FILE = "Also write all the names to a name file in GPSAVES:", OK = "Import";
    public static final String ONE_TEAM_NOTE = "A single team brings its name, colours and drivers; its drivers take the cars of the team it replaces.";
    public static final String BACKUP_NOTE = "The game files that change are copied first to " + GameDataset.BACKUP_FOLDER + " in the game folder.";
    public static final Message NO_GAME = Message.error(TITLE, "GP.EXE could not be read, so nothing was imported.");
    public static final Message NOTHING_TICKED = Message.info(TITLE, "Tick at least one thing to include.");
    public static final Message BAD_NAME_FILE = Message.warning(TITLE, "The name file needs a name of 1 to 8 letters, digits, - or _. Nothing was imported.");
    public static final Message NOT_FINISHED = Message.error(TITLE, "The import could not be finished: a game file could not be written. "
        + "The files changed so far are in " + GameDataset.BACKUP_FOLDER + ".");

    public final GameFolder game;
    public final GameDataset data;
    public final Dataset dataset;
    public final String fileName;
    private final Set<DatasetGroup> inFile, possible;
    private final Map<DatasetGroup, Boolean> ticked = new EnumMap<DatasetGroup, Boolean>(DatasetGroup.class);
    private final List<Integer> fromTeams = new ArrayList<Integer>();
    private final List<String> fromNames = new ArrayList<String>(), toNames = new ArrayList<String>();
    private boolean oneTeam;
    public int from, to;
    public boolean namesIntoGame = true, namesIntoFile;
    public String nameFile;

    private DatasetImport(GameFolder game, GameDataset data, Dataset dataset, String fileName)
    {
        this.game = game;
        this.data = data;
        this.dataset = dataset;
        this.fileName = fileName;
        inFile = GameDataset.groupsIn(dataset);
        possible = data.available();
        for (DatasetGroup g : DatasetGroup.values())
            ticked.put(g, inFile.contains(g) && possible.contains(g) && !g.files);
        for (int t = 1; t <= GameDataset.TEAMS; t++)
            if (dataset.hasSection("team " + t))
            {
                String n = dataset.get("team " + t, "name");
                fromTeams.add(t - 1);
                fromNames.add(t + "  " + (n == null || n.isEmpty() ? "Team " + t : n));
            }
        for (int t = 0; t < GameDataset.TEAMS; t++)
        {
            String n = data.getExecutable().getGameTeams().getTeamName(t);
            toNames.add((t + 1) + "  " + (n.isEmpty() ? "Team " + (t + 1) : n));
        }
        String base = baseName(fileName).toUpperCase().replaceAll("[^A-Z0-9_-]", "");
        nameFile = base.length() > 8 ? base.substring(0, 8) : base;
    }

    private static String baseName(String n)
    {
        int dot = n.lastIndexOf('.');
        return dot > 0 ? n.substring(0, dot) : n;
    }

    /**
     * Reads a file to import; null after setting problem[0] when GP.EXE cannot
     * be read, the file is not a dataset or carset, or it holds nothing the
     * editor can import.
     */
    public static DatasetImport of(GameFolder game, File file, Message[] problem)
    {
        GameDataset data;
        try
        {
            data = new GameDataset(game);
        }
        catch (IOException e)
        {
            problem[0] = NO_GAME;
            return null;
        }
        Dataset dataset;
        try
        {
            dataset = OtherEditorFiles.read(file, data.helmetPalette());
        }
        catch (IOException | RuntimeException e)
        {
            problem[0] = Message.error(TITLE, file.getName() + " could not be read as a dataset or carset.");
            return null;
        }
        if (GameDataset.groupsIn(dataset).isEmpty())
        {
            problem[0] = Message.info(TITLE, file.getName() + " holds nothing the editor can import.");
            return null;
        }
        return new DatasetImport(game, data, dataset, file.getName());
    }

    /** The line at the top: the file, its title, and what made it (with the desktop's line break as "<br>"). */
    public String heading(boolean html)
    {
        String madeBy = dataset.get("dataset", "made by"), title = dataset.get("dataset", "title");
        String name = html ? "<b>" + fileName + "</b>" : fileName;
        return name + (title != null ? ": " + title : "") + (madeBy != null ? (html ? "<br>" : "\n") + "Made with " + madeBy + "." : "");
    }

    /** The groups offered: those the file holds. */
    public List<DatasetGroup> groups()
    {
        List<DatasetGroup> l = new ArrayList<DatasetGroup>();
        for (DatasetGroup g : DatasetGroup.values())
            if (inFile.contains(g))
                l.add(g);
        return l;
    }

    public boolean possible(DatasetGroup g)
    {
        return possible.contains(g);
    }

    /** A box can be ticked if the folder has the file for it and, bringing one team, it belongs to a team. */
    public boolean enabled(DatasetGroup g)
    {
        return possible.contains(g) && (!oneTeam || g.team);
    }

    public boolean ticked(DatasetGroup g)
    {
        return ticked.get(g);
    }

    public void tick(DatasetGroup g, boolean on)
    {
        if (enabled(g))
            ticked.put(g, on);
    }

    /** The teams in the file ("3  Tyrrell") and in the game, for One team. */
    public List<String> fromTeams()
    {
        return java.util.Collections.unmodifiableList(fromNames);
    }

    public List<String> toTeams()
    {
        return java.util.Collections.unmodifiableList(toNames);
    }

    /** One team can be brought only when the file has teams. */
    public boolean canBringOneTeam()
    {
        return !fromTeams.isEmpty();
    }

    public boolean oneTeam()
    {
        return oneTeam;
    }

    public void setOneTeam(boolean on)
    {
        oneTeam = on && canBringOneTeam();
    }

    public Set<DatasetGroup> chosen()
    {
        Set<DatasetGroup> out = EnumSet.noneOf(DatasetGroup.class);
        for (DatasetGroup g : DatasetGroup.values())
            if (inFile.contains(g) && ticked.get(g) && enabled(g))
                out.add(g);
        return out;
    }

    /** OK: NOTHING_TICKED when nothing would be brought in, else null. */
    public Message problem()
    {
        return chosen().isEmpty() ? NOTHING_TICKED : null;
    }

    /** Imports (after problem()): the report, one paragraph per line; a refusal or failure goes in refused[0] and the report is null. */
    public List<String> run(Message[] refused)
    {
        GameDataset.ImportChoice choice = new GameDataset.ImportChoice();
        choice.groups = chosen();
        if (oneTeam && from >= 0 && from < fromTeams.size())
        {
            choice.fromTeam = fromTeams.get(from);
            choice.toTeam = to;
        }
        choice.namesIntoGame = namesIntoGame;
        if (namesIntoFile)
        {
            String n = nameFile.trim().toUpperCase();
            if (!n.matches("[A-Z0-9_-]{1,8}"))
            {
                refused[0] = BAD_NAME_FILE;
                return null;
            }
            File saves = game.getSavesFolder();
            if (saves == null)
                saves = new File(game.getFolder(), "GPSAVES");
            choice.nameFile = new File(saves, n + ".NAM");
        }
        try
        {
            return data.importDataset(dataset, choice);
        }
        catch (IOException e)
        {
            refused[0] = NOT_FINISHED;
            return null;
        }
    }
}
