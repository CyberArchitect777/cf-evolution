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
import cfevolution.data.dataset.DatasetGroup;
import cfevolution.edit.DatasetExport;
import cfevolution.edit.DatasetImport;
import cfevolution.edit.Message;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Export Dataset and Import Dataset, from the game folder tab: a tick box
 * per group, pictures and circuits unticked until chosen. A view over the
 * core's DatasetExport and DatasetImport (2026-11-17).
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

    private JPanel groupList(List<DatasetGroup> offered, final GroupState state)
    {
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 2));
        p.setBorder(BorderFactory.createTitledBorder(DatasetExport.INCLUDE));
        for (final DatasetGroup g : offered)
        {
            final JCheckBox box = new JCheckBox(DatasetExport.label(g, true));
            box.setName("group " + g.name().toLowerCase());
            if (!state.possible(g))
                box.setToolTipText(DatasetExport.NOT_IN_FOLDER);
            box.addItemListener(new java.awt.event.ItemListener() {      // any change of tick, not only a click (as 0.4.1 read the boxes at OK)
                public void itemStateChanged(java.awt.event.ItemEvent e) { if (state.ticked(g) != box.isSelected()) state.tick(g, box.isSelected()); }
            });
            boxes.put(g, box);
            p.add(box);
        }
        show(state);
        return p;
    }

    /** What both pages offer the boxes. */
    private interface GroupState
    {
        boolean possible(DatasetGroup g);
        boolean enabled(DatasetGroup g);
        boolean ticked(DatasetGroup g);
        void tick(DatasetGroup g, boolean on);
    }

    private void show(GroupState state)
    {
        for (Map.Entry<DatasetGroup, JCheckBox> e : boxes.entrySet())
        {
            e.getValue().setSelected(state.ticked(e.getKey()));
            e.getValue().setEnabled(state.enabled(e.getKey()));
        }
    }

    private JPanel buttons(String ok, final java.util.concurrent.Callable<Message> problem)
    {
        JButton okButton = new JButton(ok);
        okButton.setName("dataset ok");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                Message m;
                try { m = problem.call(); } catch (Exception x) { m = null; }
                if (MessageBoxes.showRefusal(DatasetDialog.this, m))
                    return;
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

    /** Replace? with the desktop's Cancel default; true for Replace. */
    private static boolean replace(Component parent, String name)
    {
        Message q = DatasetExport.replaceQuestion(name);
        return JOptionPane.showOptionDialog(parent, q.text, q.title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
                                            null, q.options, q.options[1]) == 0;
    }

    // --- export -----------------------------------------------------------------

    public static void exportDataset(MainGUI main, GameFolder game)
    {
        if (!main.releaseGameFiles(game, false))
            return;
        Message[] problem = new Message[1];
        final DatasetExport x = DatasetExport.of(game, problem);
        if (x == null)
        {
            MessageBoxes.show(main, problem[0]);
            return;
        }
        final DatasetDialog d = new DatasetDialog(main, DatasetExport.TITLE);
        final JComboBox<String> formatBox = new JComboBox<String>(DatasetExport.FORMATS);
        formatBox.setName("dataset format");
        final JTextField title = new JTextField(24);
        title.setName("dataset title");
        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(0, 0, 6, 8);
        top.add(new JLabel(DatasetExport.SAVE_AS), gc);
        gc.gridx = 1;
        top.add(formatBox, gc);
        gc.gridx = 0;
        gc.gridy = 1;
        top.add(new JLabel(DatasetExport.TITLE_LABEL), gc);
        gc.gridx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        top.add(title, gc);
        final GroupState state = new GroupState() {
            public boolean possible(DatasetGroup g) { return x.possible(g); }
            public boolean enabled(DatasetGroup g) { return x.enabled(g); }
            public boolean ticked(DatasetGroup g) { return x.ticked(g); }
            public void tick(DatasetGroup g, boolean on) { x.tick(g, on); }
        };
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.add(top, BorderLayout.NORTH);
        content.add(d.groupList(DatasetExport.groups(), state), BorderLayout.CENTER);
        JPanel south = new JPanel(new BorderLayout());
        final JLabel formatNote = note(x.note());
        formatNote.setName("format note");
        south.add(formatNote, BorderLayout.NORTH);
        formatBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                x.setFormat(formatBox.getSelectedIndex());
                d.show(state);
                formatNote.setText("<html>" + x.note() + "</html>");
                d.pack();
            }
        });
        south.add(d.buttons(DatasetExport.OK, new java.util.concurrent.Callable<Message>() {
            public Message call() { return x.problem(); }
        }), BorderLayout.SOUTH);
        content.add(south, BorderLayout.SOUTH);
        d.showDialog(content);
        if (!d.accepted)
            return;
        x.title = title.getText();

        JFileChooser chooser = new JFileChooser(game.getFolder().getParentFile());
        chooser.setDialogTitle(DatasetExport.TITLE);
        chooser.setFileFilter(new FileNameExtensionFilter(x.filter(), x.extension().substring(1)));
        chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), x.suggestedName()));
        if (chooser.showSaveDialog(main) != JFileChooser.APPROVE_OPTION)
            return;
        File target = chooser.getSelectedFile();
        File written = new File(target.getParentFile(), x.writtenName(target.getName()));
        if (written.exists() && !replace(main, written.getName()))
            return;
        java.util.List<String> notes = new java.util.ArrayList<String>();
        try
        {
            if (x.format() == DatasetExport.CFE)
                written = x.writeDataset(target);
            else
                java.nio.file.Files.write(written.toPath(), x.carset(notes));
        }
        catch (IOException e)
        {
            MessageBoxes.show(main, DatasetExport.notSaved(written.getName()));
            return;
        }
        MessageBoxes.show(main, x.saved(written.getName(), notes));
    }

    // --- import -----------------------------------------------------------------

    public static void importDataset(MainGUI main, GameFolder game, Runnable afterwards)
    {
        JFileChooser chooser = new JFileChooser(game.getFolder().getParentFile());
        chooser.setDialogTitle(DatasetImport.TITLE);
        chooser.setFileFilter(new FileNameExtensionFilter(DatasetImport.FILTER, "cfe", "zip", "1ed", "arged"));
        if (chooser.showOpenDialog(main) != JFileChooser.APPROVE_OPTION)
            return;
        importDataset(main, game, chooser.getSelectedFile(), afterwards);
    }

    public static void importDataset(MainGUI main, GameFolder game, File file, Runnable afterwards)
    {
        if (!main.releaseGameFiles(game, true))
            return;
        Message[] problem = new Message[1];
        final DatasetImport x = DatasetImport.of(game, file, problem);
        if (x == null)
        {
            MessageBoxes.show(main, problem[0]);
            return;
        }

        final DatasetDialog d = new DatasetDialog(main, DatasetImport.TITLE);
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.add(note(x.heading(true)), BorderLayout.NORTH);

        final GroupState state = new GroupState() {
            public boolean possible(DatasetGroup g) { return x.possible(g); }
            public boolean enabled(DatasetGroup g) { return x.enabled(g); }
            public boolean ticked(DatasetGroup g) { return x.ticked(g); }
            public void tick(DatasetGroup g, boolean on) { x.tick(g, on); }
        };
        JPanel middle = new JPanel(new BorderLayout(0, 10));
        middle.add(d.groupList(x.groups(), state), BorderLayout.CENTER);

        // everything, or one team into a chosen slot
        final JRadioButton all = new JRadioButton(DatasetImport.EVERYTHING, true);
        final JRadioButton one = new JRadioButton(DatasetImport.ONE_TEAM);
        all.setName("bring everything");
        one.setName("bring one team");
        ButtonGroup bring = new ButtonGroup();
        bring.add(all);
        bring.add(one);
        final JComboBox<String> from = new JComboBox<String>(x.fromTeams().toArray(new String[0]));
        final JComboBox<String> to = new JComboBox<String>(x.toTeams().toArray(new String[0]));
        from.setName("from team");
        to.setName("to team");
        JPanel teamRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        teamRow.add(one);
        teamRow.add(from);
        teamRow.add(new JLabel(DatasetImport.IN_PLACE_OF));
        teamRow.add(to);
        JPanel bringPanel = new JPanel(new GridLayout(0, 1, 0, 2));
        bringPanel.setBorder(BorderFactory.createTitledBorder(DatasetImport.BRING_IN));
        bringPanel.add(all);
        bringPanel.add(teamRow);
        bringPanel.add(note(DatasetImport.ONE_TEAM_NOTE));
        one.setEnabled(x.canBringOneTeam());
        final Runnable sync = new Runnable() {
            public void run()
            {
                x.setOneTeam(one.isSelected());
                from.setEnabled(one.isSelected());
                to.setEnabled(one.isSelected());
                for (Map.Entry<DatasetGroup, JCheckBox> e : d.boxes.entrySet())
                    e.getValue().setEnabled(x.enabled(e.getKey()));
            }
        };
        ActionListener syncer = new ActionListener() { public void actionPerformed(ActionEvent e) { sync.run(); } };
        all.addActionListener(syncer);
        one.addActionListener(syncer);
        sync.run();

        // names: GP.EXE, a name file, or both
        final JCheckBox intoGame = new JCheckBox(DatasetImport.NAMES_INTO_GAME, true);
        final JCheckBox intoFile = new JCheckBox(DatasetImport.NAMES_INTO_FILE);
        final JTextField nameFile = new JTextField(x.nameFile, 9);
        intoGame.setName("names into game");
        intoFile.setName("names into file");
        nameFile.setName("name file");
        JPanel namePanel = new JPanel(new GridLayout(0, 1, 0, 2));
        namePanel.setBorder(BorderFactory.createTitledBorder(DatasetImport.NAMES));
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
        south.add(note(DatasetImport.BACKUP_NOTE), BorderLayout.NORTH);
        south.add(d.buttons(DatasetImport.OK, new java.util.concurrent.Callable<Message>() {
            public Message call() { return x.problem(); }
        }), BorderLayout.SOUTH);
        content.add(south, BorderLayout.SOUTH);
        d.showDialog(content);
        if (!d.accepted)
            return;

        x.from = from.getSelectedIndex();
        x.to = to.getSelectedIndex();
        x.namesIntoGame = intoGame.isSelected();
        x.namesIntoFile = intoFile.isSelected();
        x.nameFile = nameFile.getText();
        Message[] refused = new Message[1];
        List<String> report = x.run(refused);
        if (report == null)
        {
            MessageBoxes.show(main, refused[0]);
            return;
        }
        if (afterwards != null)
            afterwards.run();
        JTextArea text = new JTextArea(String.join("\n\n", report), Math.min(14, 2 + report.size() * 2), 56);
        text.setName("import report");
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        JOptionPane.showMessageDialog(main, new JScrollPane(text), DatasetImport.TITLE, JOptionPane.INFORMATION_MESSAGE);
    }
}
