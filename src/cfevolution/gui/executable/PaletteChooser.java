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

package cfevolution.gui.executable;

import cfevolution.data.executable.GameLiveries;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Picks one of the game's 256 palette colours: a 16 x 16 grid of swatches
 * drawn from the executable's own palette. Click a colour to choose it, or
 * move with the arrow keys and press Enter. Colours the game changes in a
 * race are marked, with the reason shown on hover (Hrvoje, 2026-10-24).
 */
public class PaletteChooser extends JDialog
{
    private static final int CELL = 22;

    /** What a race palette colour is used for, or null for an ordinary colour. From the F1GP-SDL
        project's palette code, which reproduces the game's dry and wet palettes exactly (2026-10-25):
        the game rebuilds the ground (16-31) for each circuit and textures 18 and 26, overwrites the sky
        (128-135, 232-239) and the distant scenery shades (141-151, 158, 160-167), and draws 27 as the
        crowd. A car painted in any of these looks wrong in the race. */
    static String specialUse(int index)
    {
        if (index == 27)
            return "The crowd in the grandstands: a car painted in it shows crowd speckles in a race and grey in practice.";
        if (index >= 16 && index <= 23)
            return "Grass. The game rebuilds it for each circuit and for its textures, so a car painted in it changes from track to track.";
        if (index >= 24 && index <= 31)
            return "Tarmac. The game rebuilds it for each circuit and for its textures, so a car painted in it changes from track to track.";
        if ((index >= 128 && index <= 135) || (index >= 232 && index <= 239))
            return "Sky. The game replaces it with the sky's colours in a race.";
        if ((index >= 141 && index <= 151) || index == 158 || (index >= 160 && index <= 167))
            return "Scenery in the distance. The game works it out from other colours in a race, so it does not look like this.";
        return null;
    }

    /** A note about a colour that is fine to use but behaves differently, or null. */
    static String note(int index)
    {
        if (index >= 192 && index <= 199)
            return "Stays bright in the distance in dry weather, where other colours fade.";
        return null;
    }

    private final GameLiveries liveries;
    private int chosen = -1;
    private int highlighted;

    private PaletteChooser(Window owner, String title, GameLiveries palette, int current)
    {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        liveries = palette;
        highlighted = current;

        final JPanel grid = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                for (int i = 0; i < 256; i++) {
                    int x = (i % 16) * CELL, y = (i / 16) * CELL;
                    g.setColor(liveries.getPaletteColour(i));
                    g.fillRect(x + 1, y + 1, CELL - 2, CELL - 2);
                }
                for (int i = 0; i < 256; i++)
                    if (specialUse(i) != null)
                        mark(g, i, true);
                    else if (note(i) != null)
                        mark(g, i, false);
                outline(g, highlighted, Color.BLACK, Color.WHITE);
            }
            public Dimension getPreferredSize() { return new Dimension(16 * CELL, 16 * CELL); }
            public String getToolTipText(MouseEvent e) {
                int i = indexAt(e.getPoint());
                String about = i < 0 ? null : specialUse(i) != null ? specialUse(i) : note(i);
                return i < 0 ? null : "Colour " + i + (about != null ? ": " + about : "");
            }
        };
        grid.setName("palette");
        ToolTipManager.sharedInstance().registerComponent(grid);
        grid.setFocusable(true);
        grid.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                int i = indexAt(e.getPoint());
                if (i >= 0) { chosen = i; dispose(); }
            }
        });
        grid.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                int i = indexAt(e.getPoint());
                if (i >= 0 && i != highlighted) { highlighted = i; grid.repaint(); }
            }
        });
        grid.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                int step = 0;
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT: step = -1; break;
                    case KeyEvent.VK_RIGHT: step = 1; break;
                    case KeyEvent.VK_UP: step = -16; break;
                    case KeyEvent.VK_DOWN: step = 16; break;
                    case KeyEvent.VK_ENTER: case KeyEvent.VK_SPACE: chosen = highlighted; dispose(); return;
                    case KeyEvent.VK_ESCAPE: dispose(); return;
                    default: return;
                }
                highlighted = (highlighted + step + 256) % 256;
                grid.repaint();
            }
        });

        JLabel hint = new JLabel("<html>Click a colour, or use the arrow keys and Enter. Esc cancels.<br>"
            + "A filled corner mark: the game changes that colour in a race, so avoid it. An open mark: worth knowing.<br>"
            + "Point at a colour to see why. In the rain, colours 0 to 207 all turn greyer.</html>");
        hint.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.add(hint, BorderLayout.NORTH);
        content.add(grid, BorderLayout.CENTER);
        setContentPane(content);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
        grid.requestFocusInWindow();
    }

    private static void outline(Graphics g, int index, Color outer, Color inner)
    {
        int x = (index % 16) * CELL, y = (index / 16) * CELL;
        g.setColor(outer);
        g.drawRect(x, y, CELL - 1, CELL - 1);
        g.setColor(inner);
        g.drawRect(x + 1, y + 1, CELL - 3, CELL - 3);
    }

    /** A small corner triangle edged in black so it shows on any colour: filled white to warn, open to note. */
    private static void mark(Graphics g, int index, boolean filled)
    {
        int x = (index % 16) * CELL + 1, y = (index / 16) * CELL + 1;
        int[] xs = { x, x + 8, x }, ys = { y, y, y + 8 };
        g.setColor(Color.WHITE);
        if (filled)
            g.fillPolygon(xs, ys, 3);
        else
            g.drawPolygon(new int[] { x + 1, x + 7, x + 1 }, new int[] { y + 1, y + 1, y + 7 }, 3);
        g.setColor(Color.BLACK);
        g.drawPolygon(xs, ys, 3);
    }

    private static int indexAt(Point p)
    {
        int col = p.x / CELL, row = p.y / CELL;
        if (col < 0 || col > 15 || row < 0 || row > 15)
            return -1;
        return row * 16 + col;
    }

    /** Shows the chooser and returns the chosen palette index, or -1 if cancelled. */
    public static int choose(Component parent, String title, GameLiveries palette, int current)
    {
        PaletteChooser chooser = new PaletteChooser(SwingUtilities.getWindowAncestor(parent), title, palette, current);
        chooser.setVisible(true);
        return chooser.chosen;
    }
}
