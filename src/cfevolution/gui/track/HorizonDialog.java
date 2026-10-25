/*
 * CF Evolution: An editor for Formula One Grand Prix/World Circuit
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
 */

package cfevolution.gui.track;

import cfevolution.data.executable.GameLiveries;
import cfevolution.data.track.Track;
import cfevolution.data.track.TrackHorizon;
import cfevolution.gui.MainGUI;
import cfevolution.gui.executable.PaletteChooser;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import javax.swing.*;

/**
 * Edit > Horizon (2026-10-25): the picture of distant scenery the game draws
 * along the horizon. Shown enlarged; click to paint in the chosen colour,
 * right-click to pick up a colour, and export or import it as a PNG. Changes
 * reach the track only with OK.
 */
public class HorizonDialog extends JDialog
{
    private static final int SCALE_X = 2, SCALE_Y = 10;
    private static final String PREF_FOLDER = "HorizonGameFolder";

    private final Track track;
    private final byte[] palette;
    private final GameLiveries colours;
    private final byte[] work;
    private int current = TrackHorizon.SKY;
    private boolean changed;
    private final JPanel canvas;
    private final JButton colourButton;

    /** Opens the dialog, finding the game's palette first (asking for the game
        folder if need be). Returns true if the horizon was changed. */
    public static boolean edit(Component parent, Track track)
    {
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userRoot().node("cfevolution");
        File exe = TrackHorizon.findGameExe(track.getFile(),
            folder(prefs.get(PREF_FOLDER, "")), folder(prefs.get(MainGUI.PREF_GAME_FOLDER, "")));
        if (exe == null)
        {
            JOptionPane.showMessageDialog(parent,
                "The horizon is drawn in the game's own colours, which are in its GP.EXE.\n"
              + "Choose the folder the game is installed in.", "Horizon", JOptionPane.INFORMATION_MESSAGE);
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Choose the game folder");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION)
                return false;
            exe = TrackHorizon.findGameExe(null, chooser.getSelectedFile());
            if (exe == null)
            {
                JOptionPane.showMessageDialog(parent, "That folder does not have the game's GP.EXE.",
                                              "Horizon", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            prefs.put(PREF_FOLDER, chooser.getSelectedFile().getPath());
        }
        HorizonDialog d = new HorizonDialog(SwingUtilities.getWindowAncestor(parent), track, TrackHorizon.readPalette(exe));
        d.setVisible(true);
        return d.changed;
    }

    private static File folder(String path)
    {
        return path.isEmpty() ? null : new File(path);
    }

    HorizonDialog(Window owner, Track track, byte[] palette)
    {
        super(owner, "Horizon", ModalityType.APPLICATION_MODAL);
        this.track = track;
        this.palette = palette;
        this.colours = GameLiveries.ofPalette(palette);
        this.work = java.util.Arrays.copyOf(track.getHorizon(), TrackHorizon.BYTES);

        canvas = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                for (int y = 0; y < TrackHorizon.HEIGHT; y++)
                    for (int x = 0; x < TrackHorizon.WIDTH; x++)
                    {
                        g.setColor(colours.getPaletteColour(work[y * TrackHorizon.WIDTH + x] & 0xFF));
                        g.fillRect(x * SCALE_X, y * SCALE_Y, SCALE_X, SCALE_Y);
                    }
            }
        };
        canvas.setName("horizonCanvas");
        canvas.setPreferredSize(new Dimension(TrackHorizon.WIDTH * SCALE_X, TrackHorizon.HEIGHT * SCALE_Y));
        canvas.setToolTipText("Click to paint in the chosen colour; right-click to pick up a colour");
        MouseAdapter paint = new MouseAdapter() {
            public void mousePressed(MouseEvent e) { at(e); }
            public void mouseDragged(MouseEvent e) { if (!SwingUtilities.isRightMouseButton(e)) at(e); }
            private void at(MouseEvent e) {
                int x = e.getX() / SCALE_X, y = e.getY() / SCALE_Y;
                if (x < 0 || x >= TrackHorizon.WIDTH || y < 0 || y >= TrackHorizon.HEIGHT)
                    return;
                if (SwingUtilities.isRightMouseButton(e))
                {
                    current = work[y * TrackHorizon.WIDTH + x] & 0xFF;
                    showColour();
                    return;
                }
                work[y * TrackHorizon.WIDTH + x] = (byte) current;
                canvas.repaint();
            }
        };
        canvas.addMouseListener(paint);
        canvas.addMouseMotionListener(paint);

        colourButton = new JButton("Colour...");
        colourButton.setName("horizonColour");
        colourButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                int c = PaletteChooser.choose(HorizonDialog.this, "Horizon colour", colours, current);
                if (c >= 0)
                {
                    current = c;
                    showColour();
                }
            }
        });
        showColour();
        JButton export = new JButton("Export PNG...");
        export.setName("horizonExport");
        export.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { exportPng(); }
        });
        JButton importButton = new JButton("Import PNG...");
        importButton.setName("horizonImport");
        importButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { importPng(); }
        });

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        tools.add(colourButton);
        tools.add(export);
        tools.add(importButton);

        JLabel about = new JLabel("<html><div style='width:520px'>The distant scenery the game draws along the "
            + "horizon, 512 by 8 pixels. The picture covers a quarter turn, so it is seen four times round a "
            + "lap. Nothing in it is see-through: the sky is painted in the game's sky colour (the one every "
            + "original uses), which changes with the weather. Colours marked in the colour chooser change "
            + "with the circuit or the weather, and in the rain the game greys the whole picture.</div></html>");
        about.setForeground(new Color(0x4A525C));
        about.setBorder(BorderFactory.createEmptyBorder(8, 10, 4, 10));

        JButton ok = new JButton("OK");
        ok.setName("horizonOk");
        ok.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                byte[] h = HorizonDialog.this.track.getHorizon();
                if (!java.util.Arrays.equals(h, work))
                {
                    System.arraycopy(work, 0, h, 0, TrackHorizon.BYTES);
                    changed = true;
                }
                dispose();
            }
        });
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { dispose(); }
        });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(ok);
        buttons.add(cancel);

        JPanel centre = new JPanel(new BorderLayout());
        centre.add(tools, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(canvas, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                                             JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scroll.setPreferredSize(new Dimension(Math.min(1040, TrackHorizon.WIDTH * SCALE_X + 4),
                                              TrackHorizon.HEIGHT * SCALE_Y + 24));
        centre.add(scroll, BorderLayout.CENTER);
        centre.add(about, BorderLayout.SOUTH);

        getContentPane().add(centre, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(ok);
        pack();
        setLocationRelativeTo(owner);
    }

    private void showColour()
    {
        Color c = colours.getPaletteColour(current);
        colourButton.setIcon(new Icon() {
            public void paintIcon(Component cmp, Graphics g, int x, int y) {
                g.setColor(colours.getPaletteColour(current));
                g.fillRect(x, y, 14, 14);
                g.setColor(Color.DARK_GRAY);
                g.drawRect(x, y, 13, 13);
            }
            public int getIconWidth() { return 14; }
            public int getIconHeight() { return 14; }
        });
        colourButton.setToolTipText("The colour clicks paint in (" + current + "); choose another");
    }

    private void exportPng()
    {
        File dir = track.getFile() == null ? null : track.getFile().getAbsoluteFile().getParentFile();
        JFileChooser chooser = new JFileChooser(dir);
        chooser.setDialogTitle("Export the horizon");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PNG picture (*.png)", "png"));
        String base = track.getFile() == null ? "horizon" : track.getFile().getName().replaceAll("\\.[^.]*$", "");
        chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), base + "-horizon.png"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        File f = chooser.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".png"))
            f = new File(f.getParentFile(), f.getName() + ".png");
        try
        {
            javax.imageio.ImageIO.write(TrackHorizon.toImage(work, palette), "png", f);
        }
        catch (java.io.IOException e)
        {
            JOptionPane.showMessageDialog(this, f.getName() + " could not be saved.", "Export", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void importPng()
    {
        File dir = track.getFile() == null ? null : track.getFile().getAbsoluteFile().getParentFile();
        JFileChooser chooser = new JFileChooser(dir);
        chooser.setDialogTitle("Import a horizon");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PNG picture (*.png)", "png"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        byte[] h = null;
        try
        {
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(chooser.getSelectedFile());
            if (img != null)
                h = TrackHorizon.fromImage(img, palette);
        }
        catch (java.io.IOException e) { /* reported below */ }
        if (h == null)
        {
            JOptionPane.showMessageDialog(this,
                "A horizon picture must be 512 by 8 pixels (or that size enlarged a whole number of\n"
              + "times, such as 1024 by 16). Nothing was changed.", "Import", JOptionPane.WARNING_MESSAGE);
            return;
        }
        System.arraycopy(h, 0, work, 0, TrackHorizon.BYTES);
        canvas.repaint();
    }
}
