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
import cfevolution.edit.LiveriesPage;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * The palette kept beside the Liveries picture (Hrvoje, 2026-11-21: opening
 * and closing a chooser for each part was impractical). The selected part's
 * colour is framed; clicking a colour gives it to the selected part. Colours
 * the game changes in a race carry the chooser's corner marks, and pointing at
 * one says why.
 */
public class PaletteGrid extends JPanel
{
    public interface Listener
    {
        void picked(int index);
        /** The colour under the pointer, or -1 when it leaves. */
        void pointing(int index);
    }

    private final GameLiveries liveries;
    private final int cell;
    private final Listener listener;
    private int selected = -1, pointed = -1;

    public PaletteGrid(GameLiveries liveries, int cell, Listener listener)
    {
        this.liveries = liveries;
        this.cell = cell;
        this.listener = listener;
        setOpaque(false);
        ToolTipManager.sharedInstance().registerComponent(this);
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                int i = indexAt(e.getPoint());
                if (i >= 0 && SwingUtilities.isLeftMouseButton(e))
                    PaletteGrid.this.listener.picked(i);
            }
            public void mouseExited(MouseEvent e) { point(-1); }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) { point(indexAt(e.getPoint())); }
        });
    }

    private void point(int i)
    {
        if (i == pointed)
            return;
        pointed = i;
        repaint();
        listener.pointing(i);
    }

    /** Frames the selected part's colour (-1: none). */
    public void setSelected(int index)
    {
        selected = index;
        repaint();
    }

    public int getSelected()
    {
        return selected;
    }

    public int cell()
    {
        return cell;
    }

    public Dimension getPreferredSize()
    {
        return new Dimension(16 * cell, 16 * cell);
    }

    public Dimension getMinimumSize()
    {
        return getPreferredSize();
    }

    public String getToolTipText(MouseEvent e)
    {
        int i = indexAt(e.getPoint());
        return i < 0 ? null : LiveriesPage.about(i);
    }

    public int indexAt(Point p)
    {
        int col = p.x / cell, row = p.y / cell;
        if (p.x < 0 || p.y < 0 || col > 15 || row > 15)
            return -1;
        return row * 16 + col;
    }

    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        for (int i = 0; i < 256; i++)
        {
            g.setColor(new Color(liveries.getPaletteRgb(i)));
            g.fillRect((i % 16) * cell + 1, (i / 16) * cell + 1, cell - 2, cell - 2);
        }
        int m = Math.max(4, cell * 8 / 22);
        for (int i = 0; i < 256; i++)
        {
            boolean warn = LiveriesPage.specialUse(i) != null;
            if (!warn && LiveriesPage.note(i) == null)
                continue;
            int x = (i % 16) * cell + 1, y = (i / 16) * cell + 1;
            int[] xs = { x, x + m, x }, ys = { y, y, y + m };
            g.setColor(Color.WHITE);
            if (warn)
                g.fillPolygon(xs, ys, 3);
            g.setColor(Color.BLACK);
            g.drawPolygon(xs, ys, 3);
        }
        if (pointed >= 0 && pointed != selected)
            outline(g, pointed, new Color(0x6A7380), Color.WHITE);
        if (selected >= 0)
            outline(g, selected, Color.BLACK, Color.WHITE);
    }

    private void outline(Graphics g, int index, Color outer, Color inner)
    {
        int x = (index % 16) * cell, y = (index / 16) * cell;
        g.setColor(outer);
        g.drawRect(x, y, cell - 1, cell - 1);
        g.setColor(inner);
        g.drawRect(x + 1, y + 1, cell - 3, cell - 3);
    }
}
