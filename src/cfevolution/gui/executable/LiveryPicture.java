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

import cfevolution.data.executable.GameSprites;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * One of the game's pictures drawn in a colour set, enlarged with square
 * pixels as the game draws it. Hovering names the part under the pointer and
 * clicking reports which of the 16 colours of the set was clicked.
 *
 * @author barrie
 */
public class LiveryPicture extends JComponent
{
    /** Where the colours come from, and what each colour is called. */
    public interface Colours
    {
        /** The palette colour of slot 0-15 of the set being shown. */
        Color colour(int slot);
        /** The part name for a slot, or null if that colour cannot be edited here. */
        String part(int slot);
        void clicked(int slot);
    }

    private GameSprites.Sprite sprite;
    private boolean mirrored;
    private final int maxScale;
    private int boxWidth, boxHeight;
    private final Colours colours;

    /**
     * @param maxScale the largest enlargement; the picture is drawn smaller
     *                 if the space it is given is too small for that
     */
    public LiveryPicture(int maxScale, Colours colours)
    {
        this.maxScale = maxScale;
        this.colours = colours;
        ToolTipManager.sharedInstance().registerComponent(this);
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int slot = slotAt(e.getPoint());
                if (SwingUtilities.isLeftMouseButton(e) && slot >= 0 && LiveryPicture.this.colours.part(slot) != null)
                    LiveryPicture.this.colours.clicked(slot);
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                int slot = slotAt(e.getPoint());
                boolean editable = slot >= 0 && LiveryPicture.this.colours.part(slot) != null;
                setCursor(editable ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            }
        });
    }

    public void setSprite(GameSprites.Sprite sprite, boolean mirrored)
    {
        this.sprite = sprite;
        this.mirrored = mirrored;
        revalidate();
        repaint();
    }

    public GameSprites.Sprite getSprite()
    {
        return sprite;
    }

    /**
     * Keeps room for a picture this size (in the game's pixels), so turning
     * between pictures of different sizes does not move the page around.
     */
    public void setBox(int width, int height)
    {
        boxWidth = width;
        boxHeight = height;
        revalidate();
    }

    private int boxWidth()
    {
        return Math.max(boxWidth, sprite == null ? 0 : sprite.width);
    }

    private int boxHeight()
    {
        return Math.max(boxHeight, sprite == null ? 0 : sprite.height);
    }

    public Dimension getPreferredSize()
    {
        return new Dimension(boxWidth() * maxScale, boxHeight() * maxScale);
    }

    public Dimension getMinimumSize()
    {
        return new Dimension(boxWidth(), boxHeight());
    }

    /** The enlargement that fits the space given, at least 1. */
    private int scale()
    {
        if (boxWidth() == 0 || boxHeight() == 0)
            return 1;
        return Math.max(1, Math.min(maxScale, Math.min(getWidth() / boxWidth(), getHeight() / boxHeight())));
    }

    /** The picture's top-left corner: centred in whatever room the layout gives. */
    private Point origin()
    {
        int scale = scale();
        return new Point((getWidth() - sprite.width * scale) / 2, (getHeight() - sprite.height * scale) / 2);
    }

    /** The slot under a point on screen, or -1 over the background. */
    public int slotAt(Point p)
    {
        if (sprite == null)
            return -1;
        Point o = origin();
        int scale = scale();
        int x = Math.floorDiv(p.x - o.x, scale), y = Math.floorDiv(p.y - o.y, scale);
        if (mirrored)
            x = sprite.width - 1 - x;
        return sprite.slotAt(x, y);
    }

    public String getToolTipText(MouseEvent e)
    {
        int slot = slotAt(e.getPoint());
        if (slot < 0)
            return null;
        String part = colours.part(slot);
        return part == null ? null : part + ": click to change";
    }

    protected void paintComponent(Graphics g)
    {
        if (sprite == null)
            return;
        Point o = origin();
        int scale = scale();
        Color[] set = new Color[16];
        for (int s = 0; s < 16; s++)
            set[s] = colours.colour(s);
        for (int y = 0; y < sprite.height; y++)
            for (int x = 0; x < sprite.width; x++)
            {
                int slot = sprite.slots[y * sprite.width + x];
                if (slot < 0)
                    continue;
                int sx = mirrored ? sprite.width - 1 - x : x;
                g.setColor(set[slot]);
                g.fillRect(o.x + sx * scale, o.y + y * scale, scale, scale);
            }
    }
}
