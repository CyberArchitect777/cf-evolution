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

        JPanel rows = new JPanel(new GridBagLayout());
        rows.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 16));
        int y = 0;
        addHeading(rows, y++, "Game");
        addRow(rows, y++, "", "Game settings",
               "GP.EXE, version 1.05 " + game.getBuildName() + ": drivers, teams, points, liveries and rules",
               game.getExecutable(), "open-game", new Runnable() {
                   public void run() { openGame(); } });
        addHeading(rows, y++, "Circuits");
        for (int slot = 1; slot <= GameFolder.CIRCUITS; slot++)
        {
            final File track = game.getTrack(slot);
            addRow(rows, y++, String.valueOf(slot), GameFolder.CIRCUIT_NAMES[slot - 1],
                   track != null ? track.getName() : GameFolder.trackFileName(slot) + " is not in this folder",
                   track, "open-" + slot, new Runnable() {
                       public void run() { GameWindow.this.mainWindow.openTrackFile(track.getPath()); } });
        }
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridy = y;
        filler.weighty = 1;
        rows.add(Box.createGlue(), filler);

        JScrollPane scroll = new JScrollPane(rows);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
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
