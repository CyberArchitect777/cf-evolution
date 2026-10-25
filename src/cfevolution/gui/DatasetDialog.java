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
package cfevolution.gui;

import cfevolution.data.GameFolder;
import cfevolution.data.dataset.Dataset;
import cfevolution.data.dataset.DatasetGroup;
import cfevolution.data.dataset.GameDataset;
import cfevolution.data.dataset.OtherEditorFiles;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Export Dataset and Import Dataset, from the game folder tab: a tick box
 * per group, pictures and circuits unticked until chosen.
 *
 * @author barrie
 */
public class DatasetDialog extends JDialog
{
    private final Map<DatasetGroup, JCheckBox> boxes = new EnumMap<DatasetGroup, JCheckBox>(DatasetGroup.class);
    private boolean accepted;

    private DatasetDialog(Window owner, String title)
    {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private JPanel groupList(Set<DatasetGroup> offered, Set<DatasetGroup> possible)
    {
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 2));
        p.setBorder(BorderFactory.createTitledBorder("Include"));
        for (DatasetGroup g : DatasetGroup.values())
        {
            if (!offered.contains(g))
                continue;
            JCheckBox box = new JCheckBox("<html><b>" + g.label + "</b>: " + g.detail + "</html>", !g.files);
            box.setName("group " + g.name().toLowerCase());
            if (!possible.contains(g))
            {
                box.setSelected(false);
                box.setEnabled(false);
                box.setToolTipText("This game folder does not have the file for this");
            }
            boxes.put(g, box);
            p.add(box);
        }
        return p;
    }

    private Set<DatasetGroup> chosen()
    {
        Set<DatasetGroup> out = java.util.EnumSet.noneOf(DatasetGroup.class);
        for (Map.Entry<DatasetGroup, JCheckBox> e : boxes.entrySet())
            if (e.getValue().isSelected() && e.getValue().isEnabled())
                out.add(e.getKey());
        return out;
    }

    private JPanel buttons(String ok)
    {
        JButton okButton = new JButton(ok);
        okButton.setName("dataset ok");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                if (chosen().isEmpty())
                {
                    JOptionPane.showMessageDialog(DatasetDialog.this, "Tick at least one thing to include.", getTitle(),
                                                  JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                accepted = true;
                dispose();
            }
        });
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { dispose(); }
        });
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        p.add(okButton);
        p.add(cancel);
        getRootPane().setDefaultButton(okButton);
        return p;
    }

    private static JLabel note(String text)
    {
        JLabel l = new JLabel("<html>" + text + "</html>");
        l.setForeground(new Color(0x5A626C));
        return l;
    }

    private void showDialog(JComponent content)
    {
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
        setContentPane(content);
        pack();
        setLocationRelativeTo(getOwner());
        setVisible(true);
    }

    // --- export -----------------------------------------------------------------

    public static void exportDataset(MainGUI main, GameFolder game)
    {
        if (!main.releaseGameFiles(game, false))
            return;
        GameDataset data;
        try
        {
            data = new GameDataset(game);
        }
        catch (IOException e)
        {
            JOptionPane.showMessageDialog(main, "GP.EXE could not be read, so nothing was exported.", "Export Dataset",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        DatasetDialog d = new DatasetDialog(main, "Export Dataset");
        JTextField title = new JTextField(24);
        title.setName("dataset title");
        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.add(new JLabel("Title (optional):"), BorderLayout.WEST);
        top.add(title, BorderLayout.CENTER);
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.add(top, BorderLayout.NORTH);
        content.add(d.groupList(java.util.EnumSet.allOf(DatasetGroup.class), data.available()), BorderLayout.CENTER);
        JPanel south = new JPanel(new BorderLayout());
        south.add(note("The dataset is a text file (.cfe) anyone can read. With pictures or circuits it is saved as a zip<br>"
                       + "holding the text file and folders of PNG pictures and circuit files."), BorderLayout.NORTH);
        south.add(d.buttons("Export..."), BorderLayout.SOUTH);
        content.add(south, BorderLayout.SOUTH);
        d.showDialog(content);
        if (!d.accepted)
            return;

        Set<DatasetGroup> groups = d.chosen();
        boolean zip = false;
        for (DatasetGroup g : groups)
            zip |= g.files;
        JFileChooser chooser = new JFileChooser(game.getFolder().getParentFile());
        chooser.setDialogTitle("Export Dataset");
        chooser.setFileFilter(zip ? new FileNameExtensionFilter("Dataset with pictures (*.zip)", "zip")
                                  : new FileNameExtensionFilter("Dataset (*.cfe)", "cfe"));
        String base = title.getText().trim().replaceAll("[^A-Za-z0-9 _-]", "").trim();
        chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), (base.isEmpty() ? "dataset" : base) + (zip ? ".zip" : Dataset.EXTENSION)));
        if (chooser.showSaveDialog(main) != JFileChooser.APPROVE_OPTION)
            return;
        Dataset dataset = data.export(groups, title.getText());
        File target = chooser.getSelectedFile();
        File written = new File(target.getParentFile(), baseName(target) + (dataset.needsZip() ? ".zip" : Dataset.EXTENSION));
        if (written.exists() && JOptionPane.showOptionDialog(main, written.getName() + " already exists. Replace it?",
                "Replace File", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null,
                new String[] { "Replace", "Cancel" }, "Cancel") != 0)
            return;
        try
        {
            written = dataset.write(target);
        }
        catch (IOException e)
        {
            JOptionPane.showMessageDialog(main, written.getName() + " could not be saved.", "Export Dataset", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JOptionPane.showMessageDialog(main, "The dataset was saved as " + written.getName() + ".", "Export Dataset",
                                      JOptionPane.INFORMATION_MESSAGE);
    }

    private static String baseName(File f)
    {
        String n = f.getName();
        int dot = n.lastIndexOf('.');
        return dot > 0 ? n.substring(0, dot) : n;
    }

    // --- import -----------------------------------------------------------------

    public static void importDataset(MainGUI main, GameFolder game, Runnable afterwards)
    {
        JFileChooser chooser = new JFileChooser(game.getFolder().getParentFile());
        chooser.setDialogTitle("Import Dataset");
        chooser.setFileFilter(new FileNameExtensionFilter("Datasets and car sets (*.cfe, *.zip, F1ED *.1ed, ArgEditor *.arged)",
                                                          "cfe", "zip", "1ed", "arged"));
        if (chooser.showOpenDialog(main) != JFileChooser.APPROVE_OPTION)
            return;
        importDataset(main, game, chooser.getSelectedFile(), afterwards);
    }

    public static void importDataset(MainGUI main, GameFolder game, File file, Runnable afterwards)
    {
        if (!main.releaseGameFiles(game, true))
            return;
        GameDataset data;
        Dataset dataset;
        try
        {
            data = new GameDataset(game);
        }
        catch (IOException e)
        {
            JOptionPane.showMessageDialog(main, "GP.EXE could not be read, so nothing was imported.", "Import Dataset",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        try
        {
            dataset = OtherEditorFiles.read(file, data.helmetPalette());
        }
        catch (IOException | RuntimeException e)
        {
            JOptionPane.showMessageDialog(main, file.getName() + " could not be read as a dataset or car set.", "Import Dataset",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        Set<DatasetGroup> inFile = GameDataset.groupsIn(dataset);
        if (inFile.isEmpty())
        {
            JOptionPane.showMessageDialog(main, file.getName() + " holds nothing the editor can import.", "Import Dataset",
                                          JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        final DatasetDialog d = new DatasetDialog(main, "Import Dataset");
        JPanel content = new JPanel(new BorderLayout(0, 10));
        String madeBy = dataset.get("dataset", "made by"), title = dataset.get("dataset", "title");
        content.add(note("<b>" + file.getName() + "</b>" + (title != null ? ": " + title : "")
                         + (madeBy != null ? "<br>Made with " + madeBy + "." : "")), BorderLayout.NORTH);

        JPanel middle = new JPanel(new BorderLayout(0, 10));
        middle.add(d.groupList(inFile, data.available()), BorderLayout.CENTER);

        // everything, or one team into a chosen slot
        final JRadioButton all = new JRadioButton("Everything ticked", true);
        final JRadioButton one = new JRadioButton("One team:");
        all.setName("bring everything");
        one.setName("bring one team");
        ButtonGroup bring = new ButtonGroup();
        bring.add(all);
        bring.add(one);
        final JComboBox<String> from = new JComboBox<String>(), to = new JComboBox<String>();
        from.setName("from team");
        to.setName("to team");
        final java.util.List<Integer> fromTeams = new java.util.ArrayList<Integer>();
        for (int t = 1; t <= GameDataset.TEAMS; t++)
            if (dataset.hasSection("team " + t))
            {
                String n = dataset.get("team " + t, "name");
                fromTeams.add(t - 1);
                from.addItem(t + "  " + (n == null || n.isEmpty() ? "Team " + t : n));
            }
        for (int t = 0; t < GameDataset.TEAMS; t++)
        {
            String n = data.getExecutable().getGameTeams().getTeamName(t);
            to.addItem((t + 1) + "  " + (n.isEmpty() ? "Team " + (t + 1) : n));
        }
        JPanel teamRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        teamRow.add(one);
        teamRow.add(from);
        teamRow.add(new JLabel("in place of"));
        teamRow.add(to);
        JPanel bringPanel = new JPanel(new GridLayout(0, 1, 0, 2));
        bringPanel.setBorder(BorderFactory.createTitledBorder("Bring in"));
        bringPanel.add(all);
        bringPanel.add(teamRow);
        bringPanel.add(note("A single team brings its name, colours and drivers; its drivers take the cars of the team it replaces."));
        boolean teamsInFile = !fromTeams.isEmpty();
        one.setEnabled(teamsInFile);
        final Runnable sync = new Runnable() {
            public void run()
            {
                from.setEnabled(one.isSelected());
                to.setEnabled(one.isSelected());
                for (Map.Entry<DatasetGroup, JCheckBox> e : d.boxes.entrySet())
                    if (!e.getKey().team)
                        e.getValue().setEnabled(!one.isSelected() && data.available().contains(e.getKey()));
            }
        };
        ActionListener syncer = new ActionListener() { public void actionPerformed(ActionEvent e) { sync.run(); } };
        all.addActionListener(syncer);
        one.addActionListener(syncer);
        sync.run();

        // names: GP.EXE, a name file, or both
        final JCheckBox intoGame = new JCheckBox("Write names into GP.EXE", true);
        final JCheckBox intoFile = new JCheckBox("Also write all the names to a name file in GPSAVES:");
        final JTextField nameFile = new JTextField(baseName(file).toUpperCase().replaceAll("[^A-Z0-9_-]", ""), 9);
        intoGame.setName("names into game");
        intoFile.setName("names into file");
        nameFile.setName("name file");
        if (nameFile.getText().length() > 8)
            nameFile.setText(nameFile.getText().substring(0, 8));
        JPanel namePanel = new JPanel(new GridLayout(0, 1, 0, 2));
        namePanel.setBorder(BorderFactory.createTitledBorder("Names"));
        namePanel.add(intoGame);
        JPanel fileRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        fileRow.add(intoFile);
        fileRow.add(nameFile);
        fileRow.add(new JLabel(".NAM"));
        namePanel.add(fileRow);

        JPanel options = new JPanel(new BorderLayout(0, 10));
        options.add(bringPanel, BorderLayout.NORTH);
        options.add(namePanel, BorderLayout.SOUTH);
        middle.add(options, BorderLayout.SOUTH);
        content.add(middle, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        south.add(note("The game files that change are copied first to " + GameDataset.BACKUP_FOLDER + " in the game folder."), BorderLayout.NORTH);
        south.add(d.buttons("Import"), BorderLayout.SOUTH);
        content.add(south, BorderLayout.SOUTH);
        d.showDialog(content);
        if (!d.accepted)
            return;

        GameDataset.ImportChoice choice = new GameDataset.ImportChoice();
        choice.groups = d.chosen();
        if (one.isSelected() && from.getSelectedIndex() >= 0)
        {
            choice.fromTeam = fromTeams.get(from.getSelectedIndex());
            choice.toTeam = to.getSelectedIndex();
        }
        choice.namesIntoGame = intoGame.isSelected();
        if (intoFile.isSelected())
        {
            String n = nameFile.getText().trim().toUpperCase();
            if (!n.matches("[A-Z0-9_-]{1,8}"))
            {
                JOptionPane.showMessageDialog(main, "The name file needs a name of 1 to 8 letters, digits, - or _. Nothing was imported.",
                                              "Import Dataset", JOptionPane.WARNING_MESSAGE);
                return;
            }
            File saves = game.getSavesFolder();
            if (saves == null)
                saves = new File(game.getFolder(), "GPSAVES");
            choice.nameFile = new File(saves, n + ".NAM");
        }
        List<String> report;
        try
        {
            report = data.importDataset(dataset, choice);
        }
        catch (IOException e)
        {
            JOptionPane.showMessageDialog(main, "The import could not be finished: a game file could not be written. "
                + "The files changed so far are in " + GameDataset.BACKUP_FOLDER + ".", "Import Dataset", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (afterwards != null)
            afterwards.run();
        JTextArea text = new JTextArea(String.join("\n\n", report), Math.min(14, 2 + report.size() * 2), 56);
        text.setName("import report");
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        JOptionPane.showMessageDialog(main, new JScrollPane(text), "Import Dataset", JOptionPane.INFORMATION_MESSAGE);
    }
}
