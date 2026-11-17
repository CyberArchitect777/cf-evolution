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

import cfevolution.edit.RandomTrackGeneration;
import java.awt.*;
import java.io.File;
import javax.swing.*;

import cfevolution.data.track.CCLine;
import cfevolution.data.track.Track;
import cfevolution.generator.ccline.CCLineGenerationResult;
import cfevolution.generator.ccline.CCLineGeneratorContext;
import cfevolution.generator.track.RandomTrackGenerator;
import cfevolution.generator.track.TrackProgressListener;

/**
    Dialog for the random track generator. Generation (layout closure and
    the automatic best line) runs on a background thread against scratch
    Track instances reloaded from the track file; the live track is only
    touched on the EDT when a finished result is applied. Applying is
    destructive to the open track (confirmed once); revert by closing the
    track without saving.
*/
public class GenerateTrackDialog extends JDialog {

    private final TrackWindow trackWindow;
    private final String fileName;

    private JTextField seedField;
    private JTextField lengthField;
    private JTextField cornersField;
    private JTextField elevationField;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JButton generateButton;

    private volatile boolean fCancelled;
    private boolean fConfirmed;

    public GenerateTrackDialog(TrackWindow parentWindow, String trackFileName) {
        super((Frame) null, "Generate Random Track", false);
        trackWindow = parentWindow;
        fileName = trackFileName;
        buildUI();
        pack();
        // The number boxes sit in a GridBagLayout, which shrinks them to
        // nothing once the window is smaller than they need; so the window
        // may not be made smaller than its packed size.
        setMinimumSize(getSize());
        setLocationRelativeTo(parentWindow);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                fCancelled = true;
                dispose();
            }
        });
    }

    private void buildUI() {
        JPanel params = new JPanel(new GridBagLayout());
        params.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(2, 2, 2, 2);
        gc.anchor = GridBagConstraints.WEST;
        // Labels keep their width; the boxes take any extra as the window grows.
        gc.fill = GridBagConstraints.HORIZONTAL;

        gc.gridy = 0;
        gc.gridx = 0;
        gc.weightx = 0;
        params.add(new JLabel("Seed (blank = random):"), gc);
        gc.gridx = 1;
        gc.weightx = 1;
        seedField = new JTextField("", 10);
        params.add(seedField, gc);

        gc.gridy = 1;
        gc.gridx = 0;
        gc.weightx = 0;
        params.add(new JLabel("Lap length (TLU, 300-1300):"), gc);
        gc.gridx = 1;
        gc.weightx = 1;
        lengthField = new JTextField("900", 10);
        params.add(lengthField, gc);

        gc.gridy = 2;
        gc.gridx = 0;
        gc.weightx = 0;
        params.add(new JLabel("Corners (4-30):"), gc);
        gc.gridx = 1;
        gc.weightx = 1;
        cornersField = new JTextField("12", 10);
        params.add(cornersField, gc);

        gc.gridy = 3;
        gc.gridx = 0;
        gc.weightx = 0;
        params.add(new JLabel("Max elevation change (metres, 0 = flat):"), gc);
        gc.gridx = 1;
        gc.weightx = 1;
        elevationField = new JTextField("25", 10);
        params.add(elevationField, gc);

        progressBar = new JProgressBar(0, 100);
        statusLabel = new JLabel("Replaces the open track's layout and best line.", JLabel.CENTER);

        JPanel buttons = new JPanel(new FlowLayout());
        generateButton = new JButton("Generate & Apply");
        generateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                startGeneration();
            }
        });
        buttons.add(generateButton);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                fCancelled = true;
                dispose();
            }
        });
        buttons.add(closeButton);

        JPanel south = new JPanel(new BorderLayout(5, 5));
        south.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        south.add(progressBar, BorderLayout.NORTH);
        south.add(statusLabel, BorderLayout.CENTER);
        south.add(buttons, BorderLayout.SOUTH);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(params, BorderLayout.CENTER);
        getContentPane().add(south, BorderLayout.SOUTH);
    }

    private void startGeneration() {
        final long lSeed;
        final int nTargetTlu, nCorners;
        final double dMaxElevation;
        try {
            String sSeed = seedField.getText().trim();
            lSeed = sSeed.length() == 0 ? RandomTrackGeneration.freshSeed() : Long.parseLong(sSeed);
            nTargetTlu = Integer.parseInt(lengthField.getText().trim());
            nCorners = Integer.parseInt(cornersField.getText().trim());
            dMaxElevation = Double.parseDouble(elevationField.getText().trim());
            RandomTrackGeneration.checkSettings(dMaxElevation);
        }
        catch (NumberFormatException e) {
            statusLabel.setText("Invalid parameter: " + e.getMessage());
            return;
        }

        if (!fConfirmed) {
            if (!cfevolution.gui.MessageBoxes.ask(this, RandomTrackGeneration.CONFIRM))
                return;
            fConfirmed = true;
        }

        fCancelled = false;
        generateButton.setEnabled(false);
        statusLabel.setText("Generating...");

        final TrackProgressListener listener = new TrackProgressListener() {
            public void progress(final int percent, final String message) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        progressBar.setValue(percent);
                        statusLabel.setText(message);
                    }
                });
            }
            public boolean isCancelled() {
                return fCancelled;
            }
        };

        new Thread(new Runnable() {
            public void run() {
                RandomTrackGeneration.Result done = RandomTrackGeneration.generate(
                    fileName, lSeed, nTargetTlu, nCorners, dMaxElevation, listener);
                RandomTrackGenerator.Result layout = done.layout;
                CCLineGenerationResult line = done.line;
                String error = done.error;
                final RandomTrackGenerator.Result finalLayout = layout;
                final CCLineGenerationResult finalLine = line;
                final String finalError = error;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        generationFinished(finalLayout, finalLine, finalError);
                    }
                });
            }
        }, "track-generator").start();
    }

    /** Kept for the harnesses and tools that call it here: see
        RandomTrackGeneration.applySegments (moved 2026-11-16). */
    static void applySegments(Track track, RandomTrackGenerator.Result layout) {
        RandomTrackGeneration.applySegments(track, layout);
    }

    private void generationFinished(RandomTrackGenerator.Result layout,
                                    CCLineGenerationResult line, String error) {
        generateButton.setEnabled(true);
        if (error != null) {
            statusLabel.setText("Failed: " + error);
            progressBar.setValue(0);
            return;
        }
        if (layout == null || line == null) {
            statusLabel.setText("Cancelled.");
            progressBar.setValue(0);
            return;
        }

        int nTrimmed = trackWindow.applyGeneratedTrack(layout, line.ccLine);
        statusLabel.setText(RandomTrackGeneration.status(layout, nTrimmed, trackWindow.getPitSegCount()));
        progressBar.setValue(100);
        // Leave the dialog open: "Generate & Apply" again rerolls the seed
        // (when the seed field is blank) for quick exploration.
    }

}
