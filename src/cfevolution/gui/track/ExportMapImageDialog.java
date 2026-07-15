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

package cfevolution.gui.track;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
    Exports the track map to a PNG image, either fitting the whole track
    into the image or reproducing the current pan/zoom view.
*/
public class ExportMapImageDialog extends JDialog {

    private static final String PREF_EXPORT_FOLDER = "export.mapFolder";

    private final TrackGraphicalViewer mapWindow;

    private JTextField widthField;
    private JTextField heightField;
    private JRadioButton wholeTrackButton;
    private JRadioButton currentViewButton;
    private JLabel statusLabel;

    public ExportMapImageDialog(TrackWindow parentWindow, TrackGraphicalViewer mapViewer) {
        super((Frame) null, "Export Track Map as Image", false);
        mapWindow = mapViewer;
        buildUI();
        pack();
        setLocationRelativeTo(parentWindow);
    }

    private void buildUI() {
        JPanel params = new JPanel(new GridBagLayout());
        params.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(2, 2, 2, 2);
        gc.anchor = GridBagConstraints.WEST;

        gc.gridy = 0;
        gc.gridx = 0;
        params.add(new JLabel("Width (pixels):"), gc);
        gc.gridx = 1;
        widthField = new JTextField("2000", 8);
        params.add(widthField, gc);

        gc.gridy = 1;
        gc.gridx = 0;
        params.add(new JLabel("Height (pixels):"), gc);
        gc.gridx = 1;
        heightField = new JTextField("2000", 8);
        params.add(heightField, gc);

        wholeTrackButton = new JRadioButton("Whole track (auto-fit)", true);
        currentViewButton = new JRadioButton("Current view (pan/zoom as shown)");
        ButtonGroup group = new ButtonGroup();
        group.add(wholeTrackButton);
        group.add(currentViewButton);
        gc.gridy = 2;
        gc.gridx = 0;
        gc.gridwidth = 2;
        params.add(wholeTrackButton, gc);
        gc.gridy = 3;
        params.add(currentViewButton, gc);

        statusLabel = new JLabel(" ", JLabel.CENTER);

        JPanel buttons = new JPanel(new FlowLayout());
        JButton exportButton = new JButton("Export...");
        exportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                exportImage();
            }
        });
        buttons.add(exportButton);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dispose();
            }
        });
        buttons.add(closeButton);

        JPanel south = new JPanel(new BorderLayout(5, 5));
        south.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        south.add(statusLabel, BorderLayout.CENTER);
        south.add(buttons, BorderLayout.SOUTH);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(params, BorderLayout.CENTER);
        getContentPane().add(south, BorderLayout.SOUTH);
    }

    private void exportImage() {
        int nWidth, nHeight;
        try {
            nWidth = Integer.parseInt(widthField.getText().trim());
            nHeight = Integer.parseInt(heightField.getText().trim());
            if (nWidth < 64 || nWidth > 8192 || nHeight < 64 || nHeight > 8192)
                throw new NumberFormatException("size must be 64-8192 pixels");
        }
        catch (NumberFormatException e) {
            statusLabel.setText("Invalid size: " + e.getMessage());
            return;
        }

        Preferences prefs = Preferences.userRoot().node("cfevolution");
        JFileChooser chooser = new JFileChooser(prefs.get(PREF_EXPORT_FOLDER, ""));
        chooser.setDialogTitle("Export Track Map as Image");
        chooser.setFileFilter(new FileNameExtensionFilter("PNG images", "png"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".png"))
            file = new File(file.getParentFile(), file.getName() + ".png");
        if (file.exists()) {
            int nOption = JOptionPane.showConfirmDialog(this,
                file.getName() + " already exists. Overwrite?",
                "Export Track Map", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (nOption != JOptionPane.YES_OPTION)
                return;
        }
        prefs.put(PREF_EXPORT_FOLDER, file.getParent());

        try {
            BufferedImage image = mapWindow.renderMapImage(nWidth, nHeight,
                                                           wholeTrackButton.isSelected());
            ImageIO.write(image, "png", file);
            statusLabel.setText("Exported " + file.getName()
                + " (" + nWidth + "×" + nHeight + ")");
        }
        catch (Exception e) {
            statusLabel.setText("Export failed: " + e.getMessage());
        }
    }
}
