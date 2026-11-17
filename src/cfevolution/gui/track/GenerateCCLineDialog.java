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

import cfevolution.edit.BestLineGeneration;

import java.awt.*;
import java.io.File;
import java.util.prefs.Preferences;
import javax.swing.*;

import cfevolution.data.track.Track;
import cfevolution.generator.ccline.*;
import cfevolution.generator.ccline.datafit.DataFitCCLineGenerator;

/**
    Shared dialog for the three automatic best line generation methods.
    Generation runs on a background thread with progress and cancel; the
    candidate line is drawn on the track map (magenta) so the user can
    inspect it before Apply replaces the track's best line.
*/
public class GenerateCCLineDialog extends JDialog {

    private static final String PREF_TRAINING_FOLDER = "ccline.trainingFolder";

    private final TrackWindow trackWindow;
    private final Track track;
    private final CCLineGenerator generator;
    private final boolean fNeedsTrainingFolder;

    private JTextField iterationsField;
    private JTextField overshootField;
    private JTextField standoffField;
    private JRadioButton replaceModeButton;
    private JRadioButton completeModeButton;
    private JTextField folderField;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JButton generateButton;
    private JButton applyButton;
    private JButton discardButton;

    private volatile boolean fCancelled;
    private Thread workerThread;
    private CCLineGenerationResult result;

    public GenerateCCLineDialog(TrackWindow parentWindow, Track currentTrack,
                                CCLineGenerator lineGenerator) {
        super((Frame) null, "Generate Best Line — " + lineGenerator.getName(), false);
        trackWindow = parentWindow;
        track = currentTrack;
        generator = lineGenerator;
        fNeedsTrainingFolder = lineGenerator instanceof DataFitCCLineGenerator;
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
                closeDialog();
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
        int row = 0;

        gc.gridy = row++;
        gc.gridx = 0;
        gc.weightx = 0;
        params.add(new JLabel("Iterations:"), gc);
        gc.gridx = 1;
        gc.weightx = 1;
        iterationsField = new JTextField("2000", 8);
        params.add(iterationsField, gc);

        gc.gridy = row++;
        gc.gridx = 0;
        gc.weightx = 0;
        params.add(new JLabel("Seam overshoot (TLU):"), gc);
        gc.gridx = 1;
        gc.weightx = 1;
        overshootField = new JTextField("8", 8);
        params.add(overshootField, gc);

        gc.gridy = row++;
        gc.gridx = 0;
        gc.weightx = 0;
        params.add(new JLabel("Edge standoff (% of half-width):"), gc);
        gc.gridx = 1;
        gc.weightx = 1;
        standoffField = new JTextField("15", 8);
        params.add(standoffField, gc);

        gc.gridy = row++;
        gc.gridx = 0;
        gc.weightx = 0;
        params.add(new JLabel("Mode:"), gc);
        gc.gridx = 1;
        gc.weightx = 1;
        replaceModeButton = new JRadioButton("Replace whole line", true);
        completeModeButton = new JRadioButton("Complete existing line");
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(replaceModeButton);
        modeGroup.add(completeModeButton);
        JPanel modePanel = new JPanel(new GridLayout(2, 1, 0, 0));
        modePanel.add(replaceModeButton);
        modePanel.add(completeModeButton);
        params.add(modePanel, gc);

        if (fNeedsTrainingFolder) {
            gc.gridy = row++;
            gc.gridx = 0;
        gc.weightx = 0;
            params.add(new JLabel("Original tracks folder:"), gc);
            gc.gridx = 1;
        gc.weightx = 1;
            folderField = new JTextField(
                Preferences.userRoot().node("cfevolution").get(PREF_TRAINING_FOLDER, ""), 20);
            params.add(folderField, gc);
            gc.gridx = 2;
        gc.weightx = 0;
            JButton browse = new JButton("Browse...");
            browse.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    chooseFolder();
                }
            });
            params.add(browse, gc);
        }

        progressBar = new JProgressBar(0, 100);
        statusLabel = new JLabel("Ready.", JLabel.CENTER);

        JPanel buttons = new JPanel(new FlowLayout());
        generateButton = new JButton("Generate");
        generateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                startGeneration();
            }
        });
        buttons.add(generateButton);

        applyButton = new JButton("Apply");
        applyButton.setEnabled(false);
        applyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                applyResult();
            }
        });
        buttons.add(applyButton);

        discardButton = new JButton("Discard");
        discardButton.setEnabled(false);
        discardButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                discardResult();
            }
        });
        buttons.add(discardButton);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                closeDialog();
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


    private void chooseFolder() {
        JFileChooser chooser = new JFileChooser(folderField.getText());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Folder containing original F1CT*.DAT files");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            folderField.setText(chooser.getSelectedFile().getPath());
    }

    private void startGeneration() {
        final CCLineGeneratorContext context;
        try {
            // checked in the same order as always, so the same problem is reported first
            int nIterations = Integer.parseInt(iterationsField.getText().trim());
            int nOvershoot = BestLineGeneration.checkOvershoot(Integer.parseInt(overshootField.getText().trim()));
            int nStandoff = BestLineGeneration.checkStandoff(Integer.parseInt(standoffField.getText().trim()));
            File training = null;
            if (fNeedsTrainingFolder) {
                String path = folderField.getText().trim();
                if (path.length() == 0) {
                    statusLabel.setText(BestLineGeneration.NEED_TRAINING_FOLDER);
                    return;
                }
                training = new File(path);
            }
            context = BestLineGeneration.settings(track, nIterations, nOvershoot, nStandoff, training);
            if (training != null)
                Preferences.userRoot().node("cfevolution").put(PREF_TRAINING_FOLDER, training.getPath());
        }
        catch (NumberFormatException e) {
            statusLabel.setText("Invalid parameter: " + e.getMessage());
            return;
        }

        fCancelled = false;
        result = null;
        generateButton.setEnabled(false);
        applyButton.setEnabled(false);
        discardButton.setEnabled(false);
        trackWindow.clearBestLinePreview();

        final CCLineProgressListener listener = new CCLineProgressListener() {
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

        final boolean fComplete = completeModeButton.isSelected();
        workerThread = new Thread(new Runnable() {
            public void run() {
                BestLineGeneration.Run done = BestLineGeneration.run(generator, context, track, fComplete, listener);
                final CCLineGenerationResult finalResult = done.result;
                final String finalError = done.error;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        generationFinished(finalResult, finalError);
                    }
                });
            }
        }, "ccline-generator");
        workerThread.start();
    }

    private void generationFinished(CCLineGenerationResult generated, String error) {
        generateButton.setEnabled(true);
        if (error != null) {
            statusLabel.setText(BestLineGeneration.status(track, null, error));
            progressBar.setValue(0);
            return;
        }
        if (generated == null) {
            statusLabel.setText(BestLineGeneration.status(track, null, null));
            progressBar.setValue(0);
            return;
        }
        result = generated;
        progressBar.setValue(100);

        statusLabel.setText(BestLineGeneration.status(track, result, null));

        trackWindow.showBestLinePreview(result.previewOffsets);
        applyButton.setEnabled(true);
        discardButton.setEnabled(true);
    }

    private void applyResult() {
        if (result == null)
            return;
        trackWindow.clearBestLinePreview();
        trackWindow.replaceBestLine(result.ccLine);
        result = null;
        dispose();
    }

    private void discardResult() {
        result = null;
        trackWindow.clearBestLinePreview();
        applyButton.setEnabled(false);
        discardButton.setEnabled(false);
        statusLabel.setText("Discarded.");
        progressBar.setValue(0);
    }

    private void closeDialog() {
        fCancelled = true;
        trackWindow.clearBestLinePreview();
        dispose();
    }
}
