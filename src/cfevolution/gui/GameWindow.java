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
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import javax.swing.*;

/**
 * The tab for an open game folder: the game's settings (GP.EXE) and its 16
 * circuits, each opening in its own tab. Nothing is edited here, so there is
 * nothing to save; each file saves from its own tab, and the computer cars'
 * cornering at every circuit saves from its own page (AllCircuitsCarsDialog).
 *
 * @author barrie
 */
public class GameWindow extends JPanel implements EditorTab
{
    private final GameFolder game;
    private final MainGUI mainWindow;

    public GameWindow(GameFolder game, MainGUI mainWindow)
    {
        super(new BorderLayout());
        this.game = game;
        this.mainWindow = mainWindow;

        JLabel heading = new JLabel(cfevolution.edit.GameFolderList.TITLE);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 18f));
        JLabel path = new JLabel(game.getFolder().getPath());
        path.setForeground(new Color(0x5A626C));
        JPanel top = new JPanel(new BorderLayout(0, 4));
        top.add(heading, BorderLayout.NORTH);
        top.add(path, BorderLayout.CENTER);
        String note = cfevolution.edit.GameFolderList.originalsNote(game);
        if (note != null)
        {
            JLabel originals = new JLabel(note);
            originals.setForeground(new Color(0x5A626C));
            top.add(originals, BorderLayout.SOUTH);
        }
        top.setBorder(BorderFactory.createEmptyBorder(14, 16, 10, 16));
        add(top, BorderLayout.NORTH);

        rows.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 16));
        JScrollPane scroll = new JScrollPane(rows);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
        fillRows();
    }

    private final JPanel rows = new JPanel(new GridBagLayout());

    /** Lists everything in the folder the editor can open; called again when files are added. */
    public void fillRows()
    {
        rows.removeAll();
        int y = 0;
        boolean anySaved = false;
        for (final cfevolution.edit.GameFolderList.Row row : cfevolution.edit.GameFolderList.rows(game))
        {
            if (!row.heading)
            {
                anySaved |= row.what >= cfevolution.edit.GameFolderList.SAVED;
                addRow(rows, y++, row.number, row.label, row.detail, row.file, row.key, new Runnable() {
                    public void run() { open(row); } });
                continue;
            }
            addHeading(rows, y++, row.label);
            if (row.label.equals(cfevolution.edit.GameFolderList.CIRCUITS))
            {
                JButton allCircuits = new JButton(cfevolution.edit.AllCircuitsCars.TITLE + "...");
                allCircuits.setName("all-circuits-cars");
                allCircuits.setToolTipText(cfevolution.edit.AllCircuitsCars.BUTTON_TIP);
                allCircuits.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        new AllCircuitsCarsDialog(GameWindow.this.mainWindow, game).setVisible(true);
                    }
                });
                addWide(y++, allCircuits, 6);
            }
            else if (row.label.equals(cfevolution.edit.GameFolderList.DATASETS))
            {
                JButton exportData = new JButton("Export Dataset...");
                exportData.setName("export-dataset");
                exportData.setToolTipText("Saves teams, drivers, colours, rules, pictures and more from this game as a dataset to share");
                exportData.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) { DatasetDialog.exportDataset(GameWindow.this.mainWindow, game); }
                });
                JButton importData = new JButton("Import Dataset...");
                importData.setName("import-dataset");
                importData.setToolTipText("Brings a dataset, or an F1ED or ArgEditor carset, into this game");
                importData.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        DatasetDialog.importDataset(GameWindow.this.mainWindow, game, new Runnable() { public void run() { fillRows(); } });
                    }
                });
                // Revert Folder (Hrvoje, the owner, 2026-11-23): every changed file back to when the folder was opened
                JButton revertFolder = new JButton("Revert Folder...");
                revertFolder.setName("revert-folder");
                revertFolder.setToolTipText("Puts every file the editor has changed in this folder back as it was when the folder was opened");
                revertFolder.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) { GameWindow.this.mainWindow.revertFolder(GameWindow.this); }
                });
                addWide(y++, buttons(exportData, importData, revertFolder), 6);
            }
            else if (row.label.equals(cfevolution.edit.GameFolderList.SAVED_HEADING))
            {
                JButton saveNames = new JButton(cfevolution.edit.GameFolderList.SAVE_NAMES);
                saveNames.setName("save-names");
                saveNames.setToolTipText(cfevolution.edit.GameFolderList.SAVE_NAMES_TIP);
                saveNames.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) { saveNamesFile(); }
                });
                // Any name file, wherever it is, not only those in GPSAVES (Hrvoje, 2026-11-05)
                JButton openNames = new JButton(cfevolution.edit.GameFolderList.OPEN_NAMES);
                openNames.setName("open-names");
                openNames.setToolTipText(cfevolution.edit.GameFolderList.OPEN_NAMES_TIP);
                openNames.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) { openNamesFile(); }
                });
                addWide(y++, buttons(saveNames, openNames), 6);
                if (game.getSavedFiles().isEmpty())
                {
                    JLabel none = new JLabel(cfevolution.edit.GameFolderList.NO_SAVED_FILES);
                    none.setForeground(new Color(0xA0A6AE));
                    addWide(y++, none, 0);
                }
            }
        }

        GridBagConstraints filler = new GridBagConstraints();
        filler.gridy = y;
        filler.weighty = 1;
        rows.add(Box.createGlue(), filler);
        rows.revalidate();
        rows.repaint();
    }

    private void open(cfevolution.edit.GameFolderList.Row row)
    {
        if (row.what == cfevolution.edit.GameFolderList.GAME)
            openGame();
        else if (row.what >= 1 && row.what <= GameFolder.CIRCUITS)
            mainWindow.openTrackFile(row.file.getPath());
        else
            mainWindow.openGameFile(row.file, game);
    }

    private static JPanel buttons(JButton... all)
    {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        for (int i = 0; i < all.length; i++)
        {
            if (i > 0)
                p.add(Box.createHorizontalStrut(8));
            p.add(all[i]);
        }
        return p;
    }

    /** Something across the name and detail columns, under a heading. */
    private void addWide(int y, JComponent c, int bottom)
    {
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = y;
        g.gridx = 1;
        g.gridwidth = 3;
        g.anchor = GridBagConstraints.WEST;
        if (bottom > 0)
            g.insets = new Insets(3, 0, bottom, 0);
        rows.add(c, g);
    }

    /** Asks for a name file anywhere and opens it in a tab. */
    private void openNamesFile()
    {
        File saves = game.getSavesFolder();
        JFileChooser chooser = new JFileChooser(saves != null ? saves : game.getFolder());
        chooser.setDialogTitle("Open a Name File");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Name files (*.NAM)", "nam", "NAM"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        File f = chooser.getSelectedFile();
        byte[] bytes = null;
        try
        {
            if (f.isFile() && f.length() == cfevolution.data.gamefiles.NameFile.SIZE)
                bytes = java.nio.file.Files.readAllBytes(f.toPath());
        }
        catch (java.io.IOException ex)
        {
            bytes = null;
        }
        cfevolution.edit.Message problem = cfevolution.edit.GameFolderList.nameFileProblem(f.getName(), bytes);
        if (problem != null)
        {
            MessageBoxes.show(this, problem);
            return;
        }
        mainWindow.openGameFile(f, game);
    }

    /** Asks for a short name and writes GP.EXE's names to GPSAVES as a .NAM file. */
    private void saveNamesFile()
    {
        String typed = JOptionPane.showInputDialog(this, cfevolution.edit.GameFolderList.NAME_QUESTION,
                                                   cfevolution.edit.GameFolderList.NAME_SUGGESTED);
        if (typed == null)
            return;
        String name = cfevolution.edit.GameFolderList.fileName(typed);
        if (name == null)
        {
            MessageBoxes.show(this, cfevolution.edit.GameFolderList.BAD_NAME);
            return;
        }
        File target = cfevolution.edit.GameFolderList.nameFileTarget(game, name);
        cfevolution.edit.Message replace = cfevolution.edit.GameFolderList.replaceQuestion(target);
        if (target.exists() && JOptionPane.showOptionDialog(this, replace.text, replace.title, JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE, null, replace.options, replace.options[1]) != 0)
            return;
        try
        {
            mainWindow.writeNameFileFromGame(game, target);
        }
        catch (java.io.IOException ex)
        {
            MessageBoxes.show(this, cfevolution.edit.GameFolderList.notSaved(target));
            return;
        }
        fillRows();
        mainWindow.openGameFile(target, game);
    }

    private void openGame()
    {
        mainWindow.openExecutableFile(game.getExecutable().getPath());
    }

    private static void addHeading(JPanel rows, int y, String text)
    {
        JLabel heading = new JLabel(text.toUpperCase());
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 11f));
        heading.setForeground(new Color(0x6A7380));
        heading.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(y == 0 ? 0 : 14, 0, 4, 0),
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xD5D9DF))));
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = y;
        c.gridwidth = 4;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        rows.add(heading, c);
    }

    private static void addRow(JPanel rows, int y, String number, String name, String detail, File file,
                               String buttonName, final Runnable open)
    {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = y;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 0, 3, 12);

        JLabel numberLabel = new JLabel(number);
        numberLabel.setForeground(new Color(0x6A7380));
        numberLabel.setPreferredSize(new Dimension(24, numberLabel.getPreferredSize().height));
        c.gridx = 0;
        rows.add(numberLabel, c);

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
        nameLabel.setPreferredSize(new Dimension(150, nameLabel.getPreferredSize().height));
        c.gridx = 1;
        rows.add(nameLabel, c);

        JLabel detailLabel = new JLabel(detail);
        detailLabel.setForeground(file != null ? new Color(0x5A626C) : new Color(0xA0A6AE));
        if (file != null)
            detailLabel.setToolTipText(file.getPath());
        c.gridx = 2;
        c.weightx = 1;
        rows.add(detailLabel, c);

        JButton button = new JButton("Open");
        button.setName(buttonName);
        button.setEnabled(file != null);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { open.run(); }
        });
        c.gridx = 3;
        c.weightx = 0;
        c.insets = new Insets(3, 0, 3, 0);
        rows.add(button, c);
    }

    public GameFolder getGameFolder()
    {
        return game;
    }

    // --- EditorTab ----------------------------------------------------------

    public JComponent getComponent()
    {
        return this;
    }

    public String getTabTitle()
    {
        return game.getFolder().getName();
    }

    public String getFilePath()
    {
        return game.getFolder().getPath();
    }

    public boolean isModified()
    {
        return false;
    }

    public boolean save()
    {
        return true;
    }

    public boolean saveAs()
    {
        return false;
    }

    public JMenu[] getMenus()
    {
        return new JMenu[0];
    }

    public JComponent[] getToolbarItems()
    {
        return new JComponent[0];
    }

    public JMenuItem[] getFileMenuItems()
    {
        return new JMenuItem[0];
    }

    public String getStatusText()
    {
        return cfevolution.edit.GameFolderList.status(game);
    }
}
