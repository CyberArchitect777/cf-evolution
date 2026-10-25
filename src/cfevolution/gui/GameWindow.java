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
 * nothing to save; each file saves from its own tab.
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

        JLabel heading = new JLabel("Game folder");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 18f));
        JLabel path = new JLabel(game.getFolder().getPath());
        path.setForeground(new Color(0x5A626C));
        JPanel top = new JPanel(new BorderLayout(0, 4));
        top.add(heading, BorderLayout.NORTH);
        top.add(path, BorderLayout.CENTER);
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

    private static final String[] KIND_NAMES = { "Name file", "Set-up", "Championship save" };

    /** Lists everything in the folder the editor can open; called again when files are added. */
    public void fillRows()
    {
        rows.removeAll();
        int y = 0;
        addHeading(rows, y++, "Game");
        addRow(rows, y++, "", "Game settings",
               "GP.EXE, version 1.05 " + game.getBuildName() + ": drivers, teams, points, liveries and rules",
               game.getExecutable(), "open-game", new Runnable() {
                   public void run() { openGame(); } });
        final File prefs = game.getPreferences();
        addRow(rows, y++, "", "Game options",
               prefs != null ? "F1PREFS.DAT: opposition, race distance, driving aids, files loaded at start-up"
                             : "F1PREFS.DAT is not here yet: the game writes it with Save Options",
               prefs, "open-options", new Runnable() {
                   public void run() { GameWindow.this.mainWindow.openGameFile(prefs, game); } });

        addHeading(rows, y++, "Circuits");
        for (int slot = 1; slot <= GameFolder.CIRCUITS; slot++)
        {
            final File track = game.getTrack(slot);
            addRow(rows, y++, String.valueOf(slot), GameFolder.CIRCUIT_NAMES[slot - 1],
                   track != null ? track.getName() : GameFolder.trackFileName(slot) + " is not in this folder",
                   track, "open-" + slot, new Runnable() {
                       public void run() { GameWindow.this.mainWindow.openTrackFile(track.getPath()); } });
        }

        addHeading(rows, y++, "Pictures");
        String[] pictureNames = { "Menu helmets", "Language flags", "Menu backgrounds", "Circuit pictures" };
        String[] pictureDetails = { "the helmet beside each driver on the driver menus", "the flags on the language screen",
                                    "the full-screen pictures behind the menus", "each circuit's picture on the circuit menu" };
        for (int i = 0; i < GameFolder.PICTURE_FILES.length; i++)
        {
            final File f = game.getPictureFile(GameFolder.PICTURE_FILES[i]);
            addRow(rows, y++, "", pictureNames[i],
                   f != null ? GameFolder.PICTURE_FILES[i] + ": " + pictureDetails[i]
                             : GameFolder.PICTURE_FILES[i] + " is not in this folder",
                   f, "open-" + GameFolder.PICTURE_FILES[i], new Runnable() {
                       public void run() { GameWindow.this.mainWindow.openGameFile(f, game); } });
        }

        addHeading(rows, y++, "Sounds");
        final File sounds = game.getSoundEffects();
        addRow(rows, y++, "", "Sound effects",
               sounds != null ? "XSOUND.BIN: the engine, tyres, passing cars, crashes and pit stops (AdLib and Sound Blaster)"
                              : game.hasSoundEffects() ? "The game is set up for another sound card: only the AdLib and Sound Blaster effects can be edited"
                                                       : "XSOUND.BIN is not here: the game's installer puts it there",
               sounds, "open-sounds", new Runnable() {
                   public void run() { GameWindow.this.mainWindow.openGameFile(sounds, game); } });

        addHeading(rows, y++, "Saved files (GPSAVES)");
        JButton saveNames = new JButton("Save the Game's Names as a Name File...");
        saveNames.setName("save-names");
        saveNames.setToolTipText("Writes the driver, team and engine names in GP.EXE to a name file the game can load");
        saveNames.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { saveNamesFile(); }
        });
        GridBagConstraints b = new GridBagConstraints();
        b.gridy = y++;
        b.gridx = 1;
        b.gridwidth = 3;
        b.anchor = GridBagConstraints.WEST;
        b.insets = new Insets(3, 0, 6, 0);
        rows.add(saveNames, b);
        java.util.List<GameFolder.SavedFile> saved = game.getSavedFiles();
        if (saved.isEmpty())
        {
            JLabel none = new JLabel("No name files, set-ups or championship saves yet.");
            none.setForeground(new Color(0xA0A6AE));
            GridBagConstraints c = new GridBagConstraints();
            c.gridy = y++;
            c.gridx = 1;
            c.gridwidth = 3;
            c.anchor = GridBagConstraints.WEST;
            rows.add(none, c);
        }
        for (final GameFolder.SavedFile f : saved)
        {
            String kind = KIND_NAMES[f.kind.ordinal()];
            if (f.kind == GameFolder.SavedKind.SETUP && f.file.length() == cfevolution.data.gamefiles.SetupFile.SEASON_SIZE)
                kind = "Season set-ups";
            addRow(rows, y++, "", kind, f.name, f.file, "open-saved-" + f.name, new Runnable() {
                public void run() { GameWindow.this.mainWindow.openGameFile(f.file, game); } });
        }

        GridBagConstraints filler = new GridBagConstraints();
        filler.gridy = y;
        filler.weighty = 1;
        rows.add(Box.createGlue(), filler);
        rows.revalidate();
        rows.repaint();
    }

    /** Asks for a short name and writes GP.EXE's names to GPSAVES as a .NAM file. */
    private void saveNamesFile()
    {
        String name = JOptionPane.showInputDialog(this,
            "Name for the file, up to 8 letters or digits (the game shows DOS names):", "NAMES");
        if (name == null)
            return;
        name = name.trim().toUpperCase();
        if (name.endsWith(".NAM"))
            name = name.substring(0, name.length() - 4);
        if (!name.matches("[A-Z0-9_-]{1,8}"))
        {
            JOptionPane.showMessageDialog(this, "Use 1 to 8 letters, digits, - or _.", "Name File Not Saved",
                                          JOptionPane.WARNING_MESSAGE);
            return;
        }
        File saves = game.getSavesFolder();
        if (saves == null)
            saves = new File(game.getFolder(), "GPSAVES");
        File target = new File(saves, name + ".NAM");
        if (target.exists() && JOptionPane.showOptionDialog(this, target.getName() + " already exists. Replace it?",
                "Replace File", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null,
                new String[] { "Replace", "Cancel" }, "Cancel") != 0)
            return;
        try
        {
            mainWindow.writeNameFileFromGame(game, target);
        }
        catch (java.io.IOException ex)
        {
            JOptionPane.showMessageDialog(this, target.getName() + " could not be saved.", "Name File Not Saved",
                                          JOptionPane.ERROR_MESSAGE);
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
        int found = 0;
        for (int slot = 1; slot <= GameFolder.CIRCUITS; slot++)
            if (game.getTrack(slot) != null)
                found++;
        return "Game folder: the game settings and " + found + " of the 16 circuits. Each opens in its own tab.";
    }
}
