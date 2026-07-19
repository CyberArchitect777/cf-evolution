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
import java.io.File;
import javax.swing.*;

import cfevolution.data.track.CCLine;
import cfevolution.data.track.Track;
import cfevolution.generator.ccline.CCLineGenerationResult;
import cfevolution.generator.ccline.CCLineGeneratorContext;
import cfevolution.generator.ccline.geometric.MinCurvatureCCLineGenerator;
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
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JButton generateButton;

    private volatile boolean fCancelled;
    private boolean fConfirmed;
    private final java.util.Random seedRoller = new java.util.Random();

    public GenerateTrackDialog(TrackWindow parentWindow, String trackFileName) {
        super((Frame) null, "Generate Random Track", false);
        trackWindow = parentWindow;
        fileName = trackFileName;
        buildUI();
        pack();
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

        gc.gridy = 0;
        gc.gridx = 0;
        params.add(new JLabel("Seed (blank = random):"), gc);
        gc.gridx = 1;
        seedField = new JTextField("", 10);
        params.add(seedField, gc);

        gc.gridy = 1;
        gc.gridx = 0;
        params.add(new JLabel("Lap length (TLU, 300-1300):"), gc);
        gc.gridx = 1;
        lengthField = new JTextField("900", 10);
        params.add(lengthField, gc);

        gc.gridy = 2;
        gc.gridx = 0;
        params.add(new JLabel("Corners (4-30):"), gc);
        gc.gridx = 1;
        cornersField = new JTextField("12", 10);
        params.add(cornersField, gc);

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
        try {
            String sSeed = seedField.getText().trim();
            lSeed = sSeed.length() == 0 ? seedRoller.nextLong() : Long.parseLong(sSeed);
            nTargetTlu = Integer.parseInt(lengthField.getText().trim());
            nCorners = Integer.parseInt(cornersField.getText().trim());
        }
        catch (NumberFormatException e) {
            statusLabel.setText("Invalid parameter: " + e.getMessage());
            return;
        }

        if (!fConfirmed) {
            int nOption = JOptionPane.showConfirmDialog(this,
                "This replaces ALL track segments and the best line of the open track.\n"
                + "To revert, close the track without saving. Continue?",
                "Generate Random Track", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (nOption != JOptionPane.YES_OPTION)
                return;
            fConfirmed = true;
        }

        fCancelled = false;
        generateButton.setEnabled(false);
        statusLabel.setText("Generating (seed " + lSeed + ")...");

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
                RandomTrackGenerator.Result layout = null;
                CCLineGenerationResult line = null;
                String error = null;
                try {
                    // Scratch 1: closure iterations
                    Track scratch = new Track();
                    if (!scratch.load(new File(fileName)))
                        throw new Exception("Could not reload " + fileName + " as scratch track");
                    layout = new RandomTrackGenerator(scratch)
                        .generate(lSeed, nTargetTlu, nCorners, listener);

                    if (layout != null) {
                        // Scratch 2: carry the final layout, generate the best line on it
                        listener.progress(85, "Generating best line...");
                        Track lineTrack = new Track();
                        lineTrack.load(new File(fileName));
                        applySegments(lineTrack, layout);
                        lineTrack.setLayoutMode(false);
                        lineTrack.calculateTrackLayout();
                        CCLineGeneratorContext ctx = new CCLineGeneratorContext(lineTrack);
                        ctx.seamOvershoot = 8;
                        line = new MinCurvatureCCLineGenerator().generate(ctx, null);
                    }
                }
                catch (Exception e) {
                    error = e.getMessage();
                }
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

    /** Replaces a track's segment list with the generated one, keeping the
        dummy terminator (stripped of donor commands), and fits the donor
        pit lane's length to the new connect distance. */
    static void applySegments(Track track, RandomTrackGenerator.Result layout) {
        cfevolution.data.track.TrackSegments segs = track.getTrackSegments();
        cfevolution.data.track.TrackSegment dummy =
            (cfevolution.data.track.TrackSegment) segs.get(segs.size() - 1);
        dummy.setCommands(new java.util.Vector());
        segs.clear();
        for (int i = 0; i < layout.segments.size(); i++)
            segs.add(layout.segments.get(i));
        segs.add(dummy);
        cfevolution.generator.pitlane.PitLaneFitter.adjustPitLength(
            track.getPitlaneSegments(), layout.pitDelta);
        // Generated laps wind right (interior on the right), so the pit
        // bulges left; donor pit curves mirror their own track's final
        // corner and would walk the pit off into the void here
        cfevolution.generator.pitlane.PitLaneFitter.neutralizeCurvature(
            track.getPitlaneSegments(), -1);
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

        trackWindow.applyGeneratedTrack(layout, line.ccLine);

        StringBuffer status = new StringBuffer();
        status.append("Applied: seed ").append(layout.seed)
              .append(", ").append(layout.totalTlu).append(" TLU, closure gap ")
              .append((long) layout.closureGap).append(" units");
        for (int i = 0; i < layout.warnings.size(); i++)
            status.append(" — ").append(layout.warnings.get(i));
        statusLabel.setText(status.toString());
        progressBar.setValue(100);
        // Leave the dialog open: "Generate & Apply" again rerolls the seed
        // (when the seed field is blank) for quick exploration.
    }
}
