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
 * right-click to pick up a colour, and export or import it as a PNG or as
 * ArgTrack's .arghoriz, or take another track's. Changes
 * reach the track only with OK.
 */
public class HorizonDialog extends JDialog
{
    private static final int SCALE_X = 2, SCALE_Y = 10;
    private static final String PREF_FOLDER = "HorizonGameFolder";

    private final Track track;
    private final cfevolution.edit.HorizonPage page;
    private final GameLiveries colours;
    private final byte[] work;          // the page's picture (the capture reads it)
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
            cfevolution.gui.MessageBoxes.show(parent, cfevolution.edit.HorizonPage.NEEDS_GAME);
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Choose the game folder");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION)
                return false;
            exe = TrackHorizon.findGameExe(null, chooser.getSelectedFile());
            if (exe == null)
            {
                cfevolution.gui.MessageBoxes.show(parent, cfevolution.edit.HorizonPage.NO_GAME_EXE);
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
        super(owner, cfevolution.edit.HorizonPage.TITLE, ModalityType.APPLICATION_MODAL);
        this.track = track;
        this.page = new cfevolution.edit.HorizonPage(track, palette);
        this.colours = page.colours;
        this.work = page.work();

        canvas = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                for (int y = 0; y < TrackHorizon.HEIGHT; y++)
                    for (int x = 0; x < TrackHorizon.WIDTH; x++)
                    {
                        g.setColor(new java.awt.Color(colours.getPaletteRgb(work[y * TrackHorizon.WIDTH + x] & 0xFF)));
                        g.fillRect(x * SCALE_X, y * SCALE_Y, SCALE_X, SCALE_Y);
                    }
            }
        };
        canvas.setName("horizonCanvas");
        canvas.setPreferredSize(new Dimension(TrackHorizon.WIDTH * SCALE_X, TrackHorizon.HEIGHT * SCALE_Y));
        canvas.setToolTipText(cfevolution.edit.HorizonPage.CANVAS_TIP);
        MouseAdapter paint = new MouseAdapter() {
            public void mousePressed(MouseEvent e) { at(e); }
            public void mouseDragged(MouseEvent e) { if (!SwingUtilities.isRightMouseButton(e)) at(e); }
            private void at(MouseEvent e) {
                int x = e.getX() / SCALE_X, y = e.getY() / SCALE_Y;
                if (x < 0 || x >= TrackHorizon.WIDTH || y < 0 || y >= TrackHorizon.HEIGHT)
                    return;
                if (SwingUtilities.isRightMouseButton(e))
                {
                    page.pick(x, y);
                    showColour();
                    return;
                }
                page.paint(x, y);
                canvas.repaint();
            }
        };
        canvas.addMouseListener(paint);
        canvas.addMouseMotionListener(paint);

        colourButton = new JButton(cfevolution.edit.HorizonPage.COLOUR);
        colourButton.setName("horizonColour");
        colourButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                int c = PaletteChooser.choose(HorizonDialog.this, cfevolution.edit.HorizonPage.CHOOSER_TITLE, colours, page.current());
                if (c >= 0)
                {
                    page.setCurrent(c);
                    showColour();
                }
            }
        });
        showColour();
        JButton export = new JButton(cfevolution.edit.HorizonPage.EXPORT);
        export.setName("horizonExport");
        export.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { exportPng(); }
        });
        JButton importButton = new JButton(cfevolution.edit.HorizonPage.IMPORT);
        importButton.setName("horizonImport");
        importButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { importPng(); }
        });

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        tools.add(colourButton);
        tools.add(export);
        tools.add(importButton);

        JLabel about = new JLabel("<html><div style='width:520px'>" + cfevolution.edit.HorizonPage.ABOUT + "</div></html>");
        about.setForeground(new Color(0x4A525C));
        about.setBorder(BorderFactory.createEmptyBorder(8, 10, 4, 10));

        JButton ok = new JButton("OK");
        ok.setName("horizonOk");
        ok.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                changed = page.ok();
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
        final int current = page.current();
        colourButton.setIcon(new Icon() {
            public void paintIcon(Component cmp, Graphics g, int x, int y) {
                g.setColor(new java.awt.Color(colours.getPaletteRgb(current)));
                g.fillRect(x, y, 14, 14);
                g.setColor(Color.DARK_GRAY);
                g.drawRect(x, y, 13, 13);
            }
            public int getIconWidth() { return 14; }
            public int getIconHeight() { return 14; }
        });
        colourButton.setToolTipText(page.colourTip());
    }

    private void exportPng()
    {
        File dir = track.getFile() == null ? null : track.getFile().getAbsoluteFile().getParentFile();
        JFileChooser chooser = new JFileChooser(dir);
        chooser.setDialogTitle(cfevolution.edit.HorizonPage.EXPORT_TITLE);
        final javax.swing.filechooser.FileNameExtensionFilter png =
            new javax.swing.filechooser.FileNameExtensionFilter(cfevolution.edit.HorizonPage.PNG_FILTER, "png");
        final javax.swing.filechooser.FileNameExtensionFilter arg =
            new javax.swing.filechooser.FileNameExtensionFilter(cfevolution.edit.HorizonPage.RAW_FILTER, "arghoriz");
        chooser.addChoosableFileFilter(png);
        chooser.addChoosableFileFilter(arg);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(png);
        chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), page.exportName(false)));
        chooser.addPropertyChangeListener(JFileChooser.FILE_FILTER_CHANGED_PROPERTY, new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent e) {
                JFileChooser c = (JFileChooser) e.getSource();
                c.setSelectedFile(new File(c.getCurrentDirectory(), page.exportName(c.getFileFilter() == arg)));
            }
        });
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        File f = chooser.getSelectedFile();
        String name = f.getName().toLowerCase();
        boolean raw = name.endsWith(".arghoriz") || (!name.endsWith(".png") && chooser.getFileFilter() == arg);
        if (raw && !name.endsWith(".arghoriz"))
            f = new File(f.getParentFile(), f.getName() + ".arghoriz");
        else if (!raw && !name.endsWith(".png"))
            f = new File(f.getParentFile(), f.getName() + ".png");
        try
        {
            java.nio.file.Files.write(f.toPath(), page.export(raw));
        }
        catch (java.io.IOException e)
        {
            cfevolution.gui.MessageBoxes.show(this, cfevolution.edit.HorizonPage.notExported(f.getName()));
        }
    }

    private void importPng()
    {
        File dir = track.getFile() == null ? null : track.getFile().getAbsoluteFile().getParentFile();
        JFileChooser chooser = new JFileChooser(dir);
        chooser.setDialogTitle(cfevolution.edit.HorizonPage.IMPORT_TITLE);
        javax.swing.filechooser.FileNameExtensionFilter all = new javax.swing.filechooser.FileNameExtensionFilter(
            cfevolution.edit.HorizonPage.IMPORT_FILTER, "png", "arghoriz", "dat");
        chooser.addChoosableFileFilter(all);
        chooser.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(cfevolution.edit.HorizonPage.PNG_FILTER, "png"));
        chooser.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(cfevolution.edit.HorizonPage.RAW_FILTER, "arghoriz"));
        chooser.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(cfevolution.edit.HorizonPage.TRACK_FILTER, "dat"));
        chooser.setFileFilter(all);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        File f = chooser.getSelectedFile();
        byte[] bytes = null;
        try
        {
            bytes = java.nio.file.Files.readAllBytes(f.toPath());
        }
        catch (java.io.IOException e) { /* reported by the page */ }
        if (cfevolution.gui.MessageBoxes.showRefusal(this, page.importFile(f.getName(), bytes)))
            return;
        canvas.repaint();
    }
}
