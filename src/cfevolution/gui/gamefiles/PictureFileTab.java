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
package cfevolution.gui.gamefiles;

import cfevolution.data.GameFolder;
import cfevolution.data.gamefiles.MediaContainer;
import cfevolution.edit.Message;
import cfevolution.edit.PictureFilePage;
import cfevolution.gui.EditorTab;
import cfevolution.gui.MainGUI;
import cfevolution.gui.MessageBoxes;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.swing.*;

/**
 * The game's picture files: HELMETS.DAT, FLAGS.DAT, BACKDROP.DAT and
 * TRACKPIX.DAT. A view over the core's PictureFilePage (2026-11-17), which
 * holds the names, palettes, drawing, PNG export and import.
 *
 * @author barrie
 */
public class PictureFileTab extends JPanel implements EditorTab
{
    private final PictureFilePage page;
    private String fileName;
    private final MainGUI mainWindow;
    private boolean modified;

    private final JList<String> list;
    private final Preview preview = new Preview();
    private final JRadioButton draw = new JRadioButton(PictureFilePage.DRAW, true), replace = new JRadioButton(PictureFilePage.REPLACE);
    private final Swatches swatches = new Swatches();

    public PictureFileTab(MediaContainer pictures, String fileName, MainGUI mainWindow, GameFolder game) throws IOException
    {
        super(new BorderLayout());
        this.fileName = fileName;
        this.mainWindow = mainWindow;
        this.page = new PictureFilePage(pictures, MediaContainer.load(new File(fileName)), new File(fileName), game);
        list = new JList<String>(page.names().toArray(new String[0]));
        list.setName("pictures");
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent e) { preview.repaint(); }
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.add(button(PictureFilePage.EXPORT, "export", new Runnable() { public void run() { exportPicture(); } }));
        buttons.add(button(PictureFilePage.IMPORT, "import", new Runnable() { public void run() { importPicture(); } }));
        buttons.add(button(PictureFilePage.REVERT, "revert", new Runnable() { public void run() { revertPicture(); } }));
        if (page.canClear())       // the menu helmets and flags (Hrvoje, 2026-11-15)
        {
            JButton clear = button(PictureFilePage.CLEAR, "clear", new Runnable() { public void run() { clearPicture(); } });
            clear.setToolTipText(PictureFilePage.CLEAR_TIP);
            buttons.add(clear);
        }

        JPanel centre = new JPanel(new BorderLayout(0, 8));
        centre.add(buttons, BorderLayout.NORTH);
        centre.add(preview, BorderLayout.CENTER);
        if (page.small)
        {
            JPanel tools = new JPanel(new BorderLayout(0, 6));
            JPanel modes = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            ButtonGroup group = new ButtonGroup();
            group.add(draw);
            group.add(replace);
            draw.setName("draw");
            replace.setName("replace");
            ActionListener mode = new ActionListener() {
                public void actionPerformed(ActionEvent e) { page.setReplaceMode(replace.isSelected()); }
            };
            draw.addActionListener(mode);
            replace.addActionListener(mode);
            modes.add(draw);
            modes.add(replace);
            JLabel hint = new JLabel(PictureFilePage.HINT);
            hint.setForeground(new Color(0x6A7380));
            modes.add(hint);
            tools.add(modes, BorderLayout.NORTH);
            tools.add(swatches, BorderLayout.CENTER);
            centre.add(tools, BorderLayout.SOUTH);
        }

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(list), centre);
        split.setDividerLocation(260);
        add(GameFileTab.page(page.title(), page.explanation(), split));
        list.setSelectedIndex(0);
    }

    private static JButton button(String text, String name, final Runnable action)
    {
        JButton b = new JButton(text);
        b.setName(name);
        b.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { action.run(); } });
        return b;
    }

    private BufferedImage image(int picture)
    {
        int w = page.width(picture), h = page.height(picture);
        BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        im.setRGB(0, 0, w, h, page.rgb(picture), 0, w);
        return im;
    }

    private class Preview extends JComponent
    {
        Preview()
        {
            setName("picture");
            setPreferredSize(new Dimension(660, 420));
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { paintAt(e); }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) { if (draw.isSelected() && !SwingUtilities.isRightMouseButton(e)) paintAt(e); }
            });
        }

        Rectangle area()
        {
            int picture = list.getSelectedIndex();
            if (picture < 0)
                return null;
            int w = page.width(picture), h = page.height(picture);
            int scale = Math.max(1, Math.min(getWidth() / w, getHeight() / h));
            return new Rectangle((getWidth() - w * scale) / 2, (getHeight() - h * scale) / 2, w * scale, h * scale);
        }

        protected void paintComponent(Graphics g)
        {
            int picture = list.getSelectedIndex();
            Rectangle a = area();
            if (picture < 0 || a == null)
                return;
            g.drawImage(image(picture), a.x, a.y, a.width, a.height, null);
        }

        void paintAt(MouseEvent e)
        {
            int picture = list.getSelectedIndex();
            Rectangle a = area();
            if (!page.small || picture < 0 || a == null || !a.contains(e.getPoint()))
                return;
            int scale = a.width / page.width(picture);
            int x = (e.getX() - a.x) / scale, y = (e.getY() - a.y) / scale;
            if (SwingUtilities.isRightMouseButton(e))
            {
                page.pick(picture, x, y);
                swatches.repaint();
                return;
            }
            page.paint(picture, x, y);
            changed(null);
        }
    }

    /** The 256 colours of the palette the small pictures are drawn in. */
    private class Swatches extends JComponent
    {
        Swatches()
        {
            setName("colours");
            setPreferredSize(new Dimension(16 * 32, 8 * 14));
            setToolTipText(PictureFilePage.COLOURS_TIP);
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    int c = (e.getY() / 14) * 32 + e.getX() * 32 / Math.max(1, getWidth());
                    if (c >= 0 && c < 256) { page.setColour(c); repaint(); }
                }
            });
        }

        protected void paintComponent(Graphics g)
        {
            int w = getWidth() / 32;
            int[] pal = page.smallPalette();
            int colour = page.colour();
            for (int c = 0; c < 256; c++)
            {
                g.setColor(new Color(pal[c]));
                g.fillRect((c % 32) * w, (c / 32) * 14, w, 14);
            }
            g.setColor(Color.BLACK);
            g.drawRect((colour % 32) * w, (colour / 32) * 14, w - 1, 13);
            g.setColor(Color.WHITE);
            g.drawRect((colour % 32) * w + 1, (colour / 32) * 14 + 1, w - 3, 11);
        }
    }

    /** After a change: the refusal shown, or the tab marked changed. */
    private void changed(Message refusal)
    {
        if (MessageBoxes.showRefusal(this, refusal))
            return;
        if (page.changed())
            setModified(true);
        preview.repaint();
    }

    private void revertPicture()
    {
        int picture = list.getSelectedIndex();
        if (picture >= 0)
            changed(page.revert(picture));
    }

    private void clearPicture()
    {
        int picture = list.getSelectedIndex();
        if (picture < 0)
            return;
        page.clear(picture);
        changed(null);
    }

    // --- PNG ------------------------------------------------------------------

    private void exportPicture()
    {
        int picture = list.getSelectedIndex();
        if (picture < 0)
            return;
        JFileChooser chooser = new JFileChooser(new File(fileName).getAbsoluteFile().getParentFile());
        chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), page.exportName(picture)));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        try
        {
            java.nio.file.Files.write(chooser.getSelectedFile().toPath(), page.exportPng(picture));
        }
        catch (IOException e)
        {
            MessageBoxes.show(this, PictureFilePage.NOT_EXPORTED);
        }
    }

    private void importPicture()
    {
        int picture = list.getSelectedIndex();
        if (picture < 0)
            return;
        JFileChooser chooser = new JFileChooser(new File(fileName).getAbsoluteFile().getParentFile());
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        byte[] bytes;
        try
        {
            bytes = java.nio.file.Files.readAllBytes(chooser.getSelectedFile().toPath());
        }
        catch (IOException e)
        {
            bytes = null;
        }
        cfevolution.data.image.Picture in = PictureFilePage.read(bytes);
        if (in == null)
        {
            MessageBoxes.show(this, PictureFilePage.NOT_A_PICTURE);
            return;
        }
        importInto(picture, in, null);
    }

    /** As importInto, for a desktop image and the file's item number (GuiSweep uses it). */
    public void importImage(int item, BufferedImage in, Boolean newColours)
    {
        importInto(page.pictureOfItem(item), cfevolution.gui.ImageIOPictures.toPicture(in), newColours);
    }

    /** Puts a picture in place of this one, asking first (newColours null) when the page says to. */
    private void importInto(int picture, cfevolution.data.image.Picture in, Boolean newColours)
    {
        Message q = page.importQuestion(picture);
        boolean fresh = false;
        if (q != null)
        {
            if (newColours == null)
            {
                int choice = JOptionPane.showOptionDialog(this, q.text, q.title, JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, q.options, q.options[0]);
                if (choice != 0 && choice != 1)
                    return;
                newColours = choice == 0;
            }
            fresh = newColours;
        }
        changed(page.importPicture(picture, in, fresh));
    }

    // --- EditorTab ----------------------------------------------------------

    private void setModified(boolean value)
    {
        modified = value;
        if (mainWindow != null)
            mainWindow.tabStateChanged(this);
    }

    public JComponent getComponent() { return this; }

    public String getTabTitle() { return new File(fileName).getName(); }

    public String getFilePath() { return fileName; }

    public boolean isModified() { return modified; }

    public boolean save()
    {
        if (MessageBoxes.showRefusal(this, page.save(new File(fileName))))
            return false;
        setModified(false);
        return true;
    }

    public boolean saveAs()
    {
        cfevolution.gui.FileDialog dialog = new cfevolution.gui.FileDialog(mainWindow, true);
        dialog.setInitialDirectory(new File(fileName).getAbsoluteFile().getParent());
        try
        {
            String target = dialog.showSaveDialog();
            String was = fileName;
            fileName = target;
            if (!save())
            {
                fileName = was;
                return false;
            }
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public JMenu[] getMenus() { return new JMenu[0]; }

    public JComponent[] getToolbarItems() { return new JComponent[0]; }

    public JMenuItem[] getFileMenuItems() { return new JMenuItem[0]; }

    public String getStatusText()
    {
        return page.status();
    }
}
