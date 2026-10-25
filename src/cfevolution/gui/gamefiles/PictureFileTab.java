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
import cfevolution.gui.EditorTab;
import cfevolution.gui.MainGUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * The game's picture files: HELMETS.DAT (the menu helmets, one per car),
 * FLAGS.DAT, BACKDROP.DAT (the menu backgrounds) and TRACKPIX.DAT (each
 * circuit's picture on the circuit menu). Small pictures can be drawn on
 * directly; every picture can be exported to PNG and replaced from one.
 *
 * The menu helmets are drawn in the palette of the driver selection
 * background (BACKDROP.DAT picture 6) and the language flags in the title
 * screen's (picture 4), as ArgDocs says and as drawing them confirms. The
 * first 32 colours of every menu palette are the menus' own (F1GP-SDL) and
 * are never changed.
 *
 * @author barrie
 */
public class PictureFileTab extends JPanel implements EditorTab
{
    /** What BACKDROP.DAT's pictures are (ArgDocs, and looking at them). */
    private static final String[] BACKDROP_NAMES = {
        "Benetton (title sequence)", "Marshal waving a flag (between races)", "Honda engine (constructors' table)",
        "Title screen", "Two Lotuses", "Mansell's helmet (driver selection)", "Pit board", "Mansell in the pits (Esc in a session)",
        "Car from behind", "Car in the pits (set-up screen)", "Car in the pits, wheels off (set-up screen)",
        "Car in the pits, bodywork off (set-up screen)", "Senna with champagne", "Nose of a car",
        "Senna and a Williams in the rain", "McLaren and Williams at Monaco (main menu)", "Three drivers on the podium",
        "Drivers together" };
    private static final int FIRST_UI_COLOURS = 32;
    private static final int DRIVER_SELECTION_PICTURE = 5, TITLE_PICTURE = 3;

    private final MediaContainer pictures;
    private String fileName;
    private final MainGUI mainWindow;
    private final GameFolder game;
    private final boolean small;
    private final List<Integer> items = new ArrayList<Integer>();
    private final List<String> names = new ArrayList<String>();
    private final MediaContainer original;
    private boolean modified;

    private final JList<String> list;
    private final Preview preview = new Preview();
    private int[] smallPalette;
    private int colour = 15;
    private final JRadioButton draw = new JRadioButton("Draw", true), replace = new JRadioButton("Replace a colour");
    private final Swatches swatches = new Swatches();

    public PictureFileTab(MediaContainer pictures, String fileName, MainGUI mainWindow, GameFolder game) throws IOException
    {
        super(new BorderLayout());
        this.pictures = pictures;
        this.fileName = fileName;
        this.mainWindow = mainWindow;
        this.game = game;
        this.original = MediaContainer.load(new File(fileName));
        String file = new File(fileName).getName().toUpperCase();
        small = pictures.getItemCount() > 0 && pictures.getItem(0).type == MediaContainer.SMALL_PICTURE;
        String[] drivers = game != null && file.startsWith("HELMETS") ? game.getDriverNames() : null;
        for (int i = 0; i < pictures.getItemCount(); i++)
        {
            int type = pictures.getItem(i).type;
            if (small ? type != MediaContainer.SMALL_PICTURE : type != MediaContainer.FULL_SCREEN)
                continue;
            int n = items.size();
            items.add(i);
            if (file.startsWith("HELMETS"))
                names.add(drivers != null && n < drivers.length && drivers[n].length() > 0
                          ? (n + 1) + "  " + drivers[n] : "Car " + (n + 1));
            else if (file.startsWith("TRACKPIX"))
                names.add(n < 16 ? (n + 1) + "  " + GameFolder.CIRCUIT_NAMES[n] : (n + 1) + "  Circuit selection screen");
            else if (file.startsWith("FLAGS"))
                names.add("Flag " + (n + 1));
            else if (file.startsWith("BACKDROP"))
                names.add((n + 1) + "  " + (n < BACKDROP_NAMES.length ? BACKDROP_NAMES[n] : "Picture"));
            else
                names.add("Picture " + (n + 1));
        }
        list = new JList<String>(names.toArray(new String[0]));
        list.setName("pictures");
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent e) { preview.repaint(); }
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.add(button("Export PNG...", "export", new Runnable() { public void run() { exportPicture(); } }));
        buttons.add(button("Import PNG...", "import", new Runnable() { public void run() { importPicture(); } }));
        buttons.add(button("Revert Picture", "revert", new Runnable() { public void run() { revertPicture(); } }));

        JPanel centre = new JPanel(new BorderLayout(0, 8));
        centre.add(buttons, BorderLayout.NORTH);
        centre.add(preview, BorderLayout.CENTER);
        String explanation;
        if (small)
        {
            smallPalette = defaultSmallPalette(file);
            JPanel tools = new JPanel(new BorderLayout(0, 6));
            JPanel modes = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            ButtonGroup group = new ButtonGroup();
            group.add(draw);
            group.add(replace);
            draw.setName("draw");
            replace.setName("replace");
            modes.add(draw);
            modes.add(replace);
            JLabel hint = new JLabel("Click to use the chosen colour; right-click a pixel to pick up its colour.");
            hint.setForeground(new Color(0x6A7380));
            modes.add(hint);
            tools.add(modes, BorderLayout.NORTH);
            tools.add(swatches, BorderLayout.CENTER);
            centre.add(tools, BorderLayout.SOUTH);
            explanation = file.startsWith("HELMETS")
                ? "The helmets on the driver menus, one for each car. They are pictures: the helmet colours in GP.EXE do not change them."
                : "The flags on the language screen when the European version starts.";
        }
        else
            explanation = file.startsWith("TRACKPIX")
                ? "Each circuit's picture on the circuit menu, and the circuit selection screen. Replace one with Import PNG (320 x 200)."
                : "The menu backgrounds. Replace one with Import PNG (320 x 200).";

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(list), centre);
        split.setDividerLocation(260);
        add(GameFileTab.page(small ? (file.startsWith("HELMETS") ? "Menu Helmets" : "Flags")
                                   : (file.startsWith("TRACKPIX") ? "Circuit Pictures" : "Menu Backgrounds"),
                             explanation, split));
        list.setSelectedIndex(0);
    }

    private static JButton button(String text, String name, final Runnable action)
    {
        JButton b = new JButton(text);
        b.setName(name);
        b.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { action.run(); } });
        return b;
    }

    // --- palettes -----------------------------------------------------------

    private MediaContainer backdrops()
    {
        File[] candidates = { new File(new File(fileName).getAbsoluteFile().getParentFile(), "BACKDROP.DAT"),
                              game == null ? null : new File(game.getFolder(), "BACKDROP.DAT") };
        for (File f : candidates)
            if (f != null && f.isFile())
            {
                if (f.getAbsolutePath().equals(new File(fileName).getAbsolutePath()))
                    return pictures;
                try { return MediaContainer.load(f); } catch (IOException e) { /* try the next */ }
            }
        return null;
    }

    /** Helmets: the driver selection background's palette; flags: the title screen's. */
    private int[] defaultSmallPalette(String file)
    {
        MediaContainer b = backdrops();
        int background = file.startsWith("FLAGS") ? TITLE_PICTURE : DRIVER_SELECTION_PICTURE;
        if (b != null && b.getItemCount() > background * 2)
            return b.getPalette(background * 2);
        int[] grey = new int[256];
        for (int i = 0; i < 256; i++) grey[i] = i * 0x010101;
        return grey;
    }

    private int[] paletteOf(int item)
    {
        return small ? smallPalette : pictures.getPalette(item - 1);
    }

    // --- the picture ----------------------------------------------------------

    private int selectedItem()
    {
        int i = list.getSelectedIndex();
        return i < 0 ? -1 : items.get(i);
    }

    private byte[] pixels(int item)
    {
        return small ? pictures.getSmallPicture(item) : pictures.getFullScreen(item);
    }

    private BufferedImage image(int item)
    {
        MediaContainer.Item it = pictures.getItem(item);
        int w = small ? it.width : 320, h = small ? it.height : 200;
        byte[] px = pixels(item);
        int[] pal = paletteOf(item);
        BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                im.setRGB(x, y, pal[px[y * w + x] & 0xFF]);
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
            int item = selectedItem();
            if (item < 0)
                return null;
            MediaContainer.Item it = pictures.getItem(item);
            int w = small ? it.width : 320, h = small ? it.height : 200;
            int scale = Math.max(1, Math.min(getWidth() / w, getHeight() / h));
            return new Rectangle((getWidth() - w * scale) / 2, (getHeight() - h * scale) / 2, w * scale, h * scale);
        }

        protected void paintComponent(Graphics g)
        {
            int item = selectedItem();
            Rectangle a = area();
            if (item < 0 || a == null)
                return;
            g.drawImage(image(item), a.x, a.y, a.width, a.height, null);
        }

        void paintAt(MouseEvent e)
        {
            int item = selectedItem();
            Rectangle a = area();
            if (!small || item < 0 || a == null || !a.contains(e.getPoint()))
                return;
            MediaContainer.Item it = pictures.getItem(item);
            int scale = a.width / it.width;
            int x = (e.getX() - a.x) / scale, y = (e.getY() - a.y) / scale;
            byte[] px = pixels(item);
            int at = y * it.width + x;
            if (SwingUtilities.isRightMouseButton(e))
            {
                colour = px[at] & 0xFF;
                swatches.repaint();
                return;
            }
            if (replace.isSelected())
            {
                byte old = px[at];
                for (int i = 0; i < px.length; i++)
                    if (px[i] == old)
                        px[i] = (byte) colour;
            }
            else
                px[at] = (byte) colour;
            setPixels(item, px);
        }
    }

    /** The 256 colours of the palette the small pictures are drawn in. */
    private class Swatches extends JComponent
    {
        Swatches()
        {
            setName("colours");
            setPreferredSize(new Dimension(16 * 32, 8 * 14));
            setToolTipText("Choose a colour");
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    int c = (e.getY() / 14) * 32 + e.getX() * 32 / Math.max(1, getWidth());
                    if (c >= 0 && c < 256) { colour = c; repaint(); }
                }
            });
        }

        protected void paintComponent(Graphics g)
        {
            int w = getWidth() / 32;
            for (int c = 0; c < 256; c++)
            {
                g.setColor(new Color(smallPalette[c]));
                g.fillRect((c % 32) * w, (c / 32) * 14, w, 14);
            }
            g.setColor(Color.BLACK);
            g.drawRect((colour % 32) * w, (colour / 32) * 14, w - 1, 13);
            g.setColor(Color.WHITE);
            g.drawRect((colour % 32) * w + 1, (colour / 32) * 14 + 1, w - 3, 11);
        }
    }

    private void setPixels(int item, byte[] px)
    {
        if (small)
            pictures.setSmallPicture(item, px);
        else if (!pictures.setFullScreen(item, px))
        {
            JOptionPane.showMessageDialog(this, "This picture is too detailed for the game to load. Try a simpler one.",
                "Picture Not Changed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        setModified(true);
        preview.repaint();
    }

    private void revertPicture()
    {
        int item = selectedItem();
        if (item < 0)
            return;
        byte[] px = small ? original.getSmallPicture(item) : original.getFullScreen(item);
        if (!small)
            pictures.setPalette(item - 1, original.getPalette(item - 1));
        setPixels(item, px);
    }

    // --- PNG ------------------------------------------------------------------

    private void exportPicture()
    {
        int item = selectedItem();
        if (item < 0)
            return;
        JFileChooser chooser = new JFileChooser(new File(fileName).getAbsoluteFile().getParentFile());
        chooser.setSelectedFile(new File(chooser.getCurrentDirectory(),
            new File(fileName).getName().replaceAll("\\..*$", "") + "-" + (list.getSelectedIndex() + 1) + ".png"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        try
        {
            ImageIO.write(image(item), "png", chooser.getSelectedFile());
        }
        catch (IOException e)
        {
            JOptionPane.showMessageDialog(this, "The picture could not be saved.", "Export Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void importPicture()
    {
        int item = selectedItem();
        if (item < 0)
            return;
        JFileChooser chooser = new JFileChooser(new File(fileName).getAbsoluteFile().getParentFile());
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        BufferedImage in;
        try
        {
            in = ImageIO.read(chooser.getSelectedFile());
        }
        catch (IOException e)
        {
            in = null;
        }
        if (in == null)
        {
            JOptionPane.showMessageDialog(this, "That file is not a picture the editor can read.", "Import Failed",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        importImage(item, in, null);
    }

    /**
     * Puts a picture in place of item: scaled to fit if its size differs, then
     * matched to the nearest colours. For a full-screen picture, newColours
     * (or the user, if null) decides whether it gets a palette of its own.
     */
    public void importImage(int item, BufferedImage in, Boolean newColours)
    {
        MediaContainer.Item it = pictures.getItem(item);
        int w = small ? it.width : 320, h = small ? it.height : 200;
        if (in.getWidth() != w || in.getHeight() != h)
        {
            BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(in, 0, 0, w, h, null);
            g.dispose();
            in = scaled;
        }
        int[] rgb = in.getRGB(0, 0, w, h, null, 0, w);
        int[] pal = paletteOf(item).clone();
        if (!small)
        {
            // the helmets and flags are drawn in these two backgrounds' colours, so those stay
            boolean helmetsUseIt = new File(fileName).getName().toUpperCase().startsWith("BACKDROP")
                                   && (item == DRIVER_SELECTION_PICTURE * 2 + 1 || item == TITLE_PICTURE * 2 + 1);
            if (newColours == null)
            {
                if (helmetsUseIt)
                    newColours = false;
                else
                {
                    int choice = JOptionPane.showOptionDialog(this,
                        "Give this picture colours of its own (best for a new picture), or match it to the colours it has now?",
                        "Import Picture", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                        new String[] { "New Colours", "Keep Colours", "Cancel" }, "New Colours");
                    if (choice != 0 && choice != 1)
                        return;
                    newColours = choice == 0;
                }
            }
            if (newColours && !helmetsUseIt)
                pal = ColourReducer.palette(rgb, pal, FIRST_UI_COLOURS);
        }
        byte[] px = ColourReducer.match(rgb, pal, small ? 0 : FIRST_UI_COLOURS);
        if (!small)
        {
            int[] was = pictures.getPalette(item - 1);
            pictures.setPalette(item - 1, pal);
            if (!pictures.setFullScreen(item, px))
            {
                pictures.setPalette(item - 1, was);
                JOptionPane.showMessageDialog(this, "This picture is too detailed for the game to load. Try a simpler one.",
                    "Picture Not Changed", JOptionPane.WARNING_MESSAGE);
                return;
            }
            setModified(true);
            preview.repaint();
        }
        else
            setPixels(item, px);
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
        try
        {
            pictures.save(new File(fileName));
        }
        catch (IOException e)
        {
            JOptionPane.showMessageDialog(this, new File(fileName).getName() + " could not be saved.", "Save Failed",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
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
        return names.size() + " pictures  ·  " + (small ? "draw on them, or import PNG" : "320 x 200, import PNG to replace");
    }
}
