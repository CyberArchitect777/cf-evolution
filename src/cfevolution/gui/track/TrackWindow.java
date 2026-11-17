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

/*
 * TrackWindow.java
 *
 * Created on 07 April 2005, 03:43
 */

package cfevolution.gui.track;

import cfevolution.edit.Message;
import cfevolution.edit.RandomTrackGeneration;
import cfevolution.edit.TrackCheck;
import cfevolution.edit.TrackTools;
import cfevolution.gui.MessageBoxes;
import cfevolution.gui.*;
import java.io.*;
import cfevolution.data.track.*;
import javax.swing.*;

/**
 * One open track, shown as a tab of the main window: the track tree on the
 * left, the map in the middle and the selected item's properties and
 * commands on the right. Everything is edited in place; nothing opens a
 * window of its own except the generation and export dialogs.
 *
 * @author  barrie
 */
public class TrackWindow extends JPanel implements EditorTab
{
    private Track currentTrack;
    private TrackTreeSelector treeWindow;
    private TrackGraphicalViewer mapWindow;
    private MainGUI appFrame;
    private String fileName;
    private boolean modified;

    private SegmentEditGUI propertyEditor;
    private CommandEditGUI commandEditor;
    private JTabbedPane inspectorTabs;
    private JLabel inspectorHeading;
    private JLabel inspectorHint;
    private JPanel warningBanner;
    private JMenu editMenu;
    private JMenu viewMenu;

    /* Receives a selected track as input */
    public TrackWindow(Track selectedTrack, String trackFileName, MainGUI origContainer)
    {
        super(new java.awt.BorderLayout());
        fileName = trackFileName;
        currentTrack = selectedTrack;
        bestLineRepair = new cfevolution.edit.BestLineRepair(currentTrack);
        appFrame = origContainer;
        initComponents();

        treeWindow = new TrackTreeSelector(this);
        mapWindow = new TrackGraphicalViewer();
        mapWindow.setParentTrackWindow(this);
        mapWindow.setTrack(currentTrack);
        treeWindow.setTrack(currentTrack);
        propertyEditor = new SegmentEditGUI(this, treeWindow);
        commandEditor = new CommandEditGUI(this, treeWindow);

        buildLayout();
        treeWindow.populateTree();
        treeWindow.showBranches();
        buildMenus();
        updateCCLineCoverageStatus();
        applyLegacyOptionsVisibility(); // Legacy best line methods hidden unless enabled in Options
        selectionChanged(0, -1, null);
        modified = false;
    }

    // --- Layout --------------------------------------------------------

    private void buildLayout()
    {
        java.awt.Color line = new java.awt.Color(0xD5D9DF);

        JPanel left = new JPanel(new java.awt.BorderLayout());
        left.add(sectionHeading("Track"), java.awt.BorderLayout.NORTH);
        left.add(treeWindow, java.awt.BorderLayout.CENTER);
        left.setMinimumSize(new java.awt.Dimension(160, 100));

        warningBanner = new JPanel();
        warningBanner.setLayout(new BoxLayout(warningBanner, BoxLayout.Y_AXIS));
        warningBanner.setBackground(new java.awt.Color(0xFFF4CC));
        warningBanner.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new java.awt.Color(0xE8D48A)));
        warningBanner.setVisible(false);
        JPanel centre = new JPanel(new java.awt.BorderLayout());
        centre.add(warningBanner, java.awt.BorderLayout.NORTH);
        centre.add(mapWindow, java.awt.BorderLayout.CENTER);

        inspectorHeading = new JLabel("Nothing selected");
        inspectorHeading.setFont(inspectorHeading.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        inspectorHint = new JLabel(" ");
        inspectorHint.setForeground(new java.awt.Color(0x7A828C));
        JPanel headingBox = new JPanel(new java.awt.GridLayout(2, 1));
        headingBox.add(inspectorHeading);
        headingBox.add(inspectorHint);
        headingBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, line),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        inspectorTabs = new JTabbedPane();
        inspectorTabs.addTab("Properties", propertyEditor);
        inspectorTabs.addTab("Commands", commandEditor);
        JPanel right = new JPanel(new java.awt.BorderLayout());
        right.add(headingBox, java.awt.BorderLayout.NORTH);
        right.add(inspectorTabs, java.awt.BorderLayout.CENTER);
        right.setMinimumSize(new java.awt.Dimension(260, 100));
        right.setPreferredSize(new java.awt.Dimension(360, 100));

        JSplitPane mapAndInspector = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, centre, right);
        mapAndInspector.setResizeWeight(1.0);
        mapAndInspector.setContinuousLayout(true);
        JSplitPane main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, mapAndInspector);
        main.setDividerLocation(230);
        main.setContinuousLayout(true);
        add(main, java.awt.BorderLayout.CENTER);
    }

    static JLabel sectionHeading(String text)
    {
        JLabel heading = new JLabel(text.toUpperCase());
        heading.setFont(heading.getFont().deriveFont(java.awt.Font.BOLD, 11f));
        heading.setForeground(new java.awt.Color(0x6A7380));
        heading.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new java.awt.Color(0xD5D9DF)),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        return heading;
    }

    // --- Selection and the property panel -------------------------------

    /** Called by the tree whenever its selection changes. kind: 0 nothing
        editable, 1 track segment, 2 pit segment, 3 best line segment,
        4 track data headers. Unapplied edits to the previous item are
        applied first, so moving on never loses typing. */
    public void selectionChanged(int kind, int segmentNo, Object node)
    {
        applyPendingEdits();

        boolean commands = false;
        switch (kind)
        {
            case 1:
            case 2:
            {
                TrackSegments segs = kind == 1 ? currentTrack.getTrackSegments() : currentTrack.getPitlaneSegments();
                TrackSegment segment = segs.getAt(segmentNo);
                propertyEditor.editTrackSegment(segment);
                propertyEditor.setEditedNode(node);
                commandEditor.editSegment(segment);
                commands = true;
                inspectorHeading.setText(cfevolution.edit.TrackOutline.heading(kind, segmentNo));
                inspectorHint.setText(cfevolution.edit.TrackOutline.hint(currentTrack, kind, segmentNo));
                break;
            }
            case 3:
            {
                CCLineSegment lineSegment = currentTrack.getCCLine().getAt(segmentNo);
                propertyEditor.editBestLineSegment(lineSegment);
                propertyEditor.setEditedNode(node);
                commandEditor.editSegment(null);
                inspectorHeading.setText(cfevolution.edit.TrackOutline.heading(kind, segmentNo));
                inspectorHint.setText(cfevolution.edit.TrackOutline.hint(currentTrack, kind, segmentNo));
                break;
            }
            case 4:
                propertyEditor.editDataHeaders(currentTrack.getTrackDataHeader());
                propertyEditor.setEditedNode(null);
                commandEditor.editSegment(null);
                inspectorHeading.setText(cfevolution.edit.TrackOutline.HEADERS_HEADING);
                inspectorHint.setText(cfevolution.edit.TrackOutline.HEADERS_HINT);
                break;
            default:
                propertyEditor.showNothing();
                commandEditor.editSegment(null);
                inspectorHeading.setText(cfevolution.edit.TrackOutline.NOTHING_HEADING);
                inspectorHint.setText(cfevolution.edit.TrackOutline.NOTHING_HINT);
                break;
        }
        inspectorTabs.setEnabledAt(1, commands);
        if (!commands)
            inspectorTabs.setSelectedIndex(0);
    }

    // --- EditorTab ------------------------------------------------------

    public JComponent getComponent() { return this; }

    public String getTabTitle() { return new File(fileName).getName(); }

    public String getFilePath() { return fileName; }

    public boolean isModified() { return modified; }

    private void setModified(boolean value)
    {
        modified = value;
        if (appFrame != null)
            appFrame.tabStateChanged(this); // also refreshes the status bar figures
    }

    public String getStatusText()
    {
        double km = currentTrack.getTrackTlu() * 16 * 0.3048 / 1000.0;
        return String.format("Lap %.2f km  ·  %d track segments  ·  %d pit lane segments  ·  %d best line segments",
            km, currentTrack.getTrackSegments().size(), currentTrack.getPitlaneSegments().size(),
            currentTrack.getCCLine().size());
    }

    public JMenu[] getMenus()
    {
        return new JMenu[] { editMenu, viewMenu, toolsMenu };
    }

    public JComponent[] getToolbarItems()
    {
        return new JComponent[] {
            MainGUI.toolbarButton("Add Above", "Add a segment above the selected one (Shift+Insert in the list)",
                new Runnable() { public void run() { treeWindow.addAbove(); } }),
            MainGUI.toolbarButton("Add Below", "Add a segment below the selected one (Insert in the list)",
                new Runnable() { public void run() { treeWindow.addBelow(); } }),
            MainGUI.toolbarButton("Delete", "Delete the selected segment (Delete in the list)",
                new Runnable() { public void run() { treeWindow.deleteSelected(); } }),
            null,
            MainGUI.toolbarButton("Generate Best Line", "Generate the AI's best line by simulating laps (Ctrl+Shift+B)",
                new Runnable() { public void run() {
                    openGenerateDialog(new cfevolution.generator.ccline.geometric.FastestLapCCLineGenerator()); } }),
            MainGUI.toolbarButton("Random Track", "Generate a new random circuit from this one",
                new Runnable() { public void run() { openGenerateTrackDialog(); } }),
        };
    }

    public JMenuItem[] getFileMenuItems()
    {
        JMenuItem export = new JMenuItem("Export Track Map as Image...");
        export.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                new ExportMapImageDialog(TrackWindow.this, mapWindow).setVisible(true);
            }
        });
        return new JMenuItem[] { export };
    }

    /** Shows or hides the legacy best line generation methods (options
        1-3: Constructed Racing Line, Learned from Original Tracks,
        Iterative Refinement) in the Tools menu, based on the "Enable
        legacy options" preference. Fastest Lap Simulation (option 4) is
        always shown. Off by default. */
    public void applyLegacyOptionsVisibility()
    {
        boolean legacy = java.util.prefs.Preferences.userRoot().node("cfevolution")
            .getBoolean("EnableLegacyOptions", false);
        generateLineGeometric.setVisible(legacy);
        generateLineDataFit.setVisible(legacy);
        generateLineRefine.setVisible(legacy);
    }

    /** Race length and the computer cars' pit stop plan, kept in the track file's footer. */
    private void editRaceStrategy()
    {
        applyPendingEdits();
        cfevolution.edit.RaceStrategyForm form = cfevolution.edit.RaceStrategyForm.of(currentTrack.getFooter());
        if (form == null)
        {
            cfevolution.gui.MessageBoxes.show(this, cfevolution.edit.RaceStrategyForm.NOT_AVAILABLE);
            return;
        }
        RaceStrategyDialog dialog = new RaceStrategyDialog(this, form);
        dialog.setVisible(true);
        if (dialog.wasChanged())
            setModified(true);
    }

    /** Tools > Check Track: every check the editor has, with fixes. */
    private void checkTrack()
    {
        applyPendingEdits();
        new TrackCheckDialog(this, currentTrack, new TrackCheckDialog.Fixer() {
            public void fix(int action) {
                fixAction(action);
            }
        }).setVisible(true);
        // The checks recompile the layout (which changes nothing in the file);
        // redraw without marking the track unsaved. Fixes mark it themselves.
        mapWindow.redrawTrackMap();
    }

    /** Runs the fix for a Check Track item or a banner warning (a TrackCheck.ACTION_ code). */
    private void fixAction(int action)
    {
        switch (action)
        {
            case TrackCheck.ACTION_MOVE_TRACK: moveTrackFromEdge(); break;
            case TrackCheck.ACTION_MATCH_HEADING: addEndHeadingSegment(); break;
            case TrackCheck.ACTION_PIT_MOUTHS: setUpPitMouths(); break;
            case TrackCheck.ACTION_GENERATE_LINE:
                openGenerateDialog(new cfevolution.generator.ccline.geometric.FastestLapCCLineGenerator());
                break;
        }
    }

    /** The picture of distant scenery along the horizon (the file's first 4096 bytes). */
    private void editHorizon()
    {
        applyPendingEdits();
        if (HorizonDialog.edit(this, currentTrack))
            setModified(true);
    }

    /** The circuit's car and computer car settings (the CC setup block). */
    private void editCarSettings()
    {
        applyPendingEdits();
        CarSettingsDialog dialog = new CarSettingsDialog(this, new cfevolution.edit.CarSettingsForm(currentTrack.getCCSetup()));
        dialog.setVisible(true);
        if (dialog.wasChanged())
            setModified(true);
    }

    // --- Menus ----------------------------------------------------------

    private JMenuItem item(String text, final Runnable action)
    {
        JMenuItem menuItem = new JMenuItem(text);
        menuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { action.run(); }
        });
        return menuItem;
    }

    private void buildMenus()
    {
        editMenu = new JMenu("Edit");
        editMenu.setMnemonic('E');
        editMenu.add(item("Add Segment Above", new Runnable() { public void run() { treeWindow.addAbove(); } }));
        editMenu.add(item("Add Segment Below", new Runnable() { public void run() { treeWindow.addBelow(); } }));
        editMenu.add(item("Delete Segment", new Runnable() { public void run() { treeWindow.deleteSelected(); } }));
        editMenu.addSeparator();
        JMenuItem apply = item("Apply Property Changes", new Runnable() { public void run() {
            applyPendingEdits(); } });
        apply.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        editMenu.add(apply);
        editMenu.addSeparator();
        editMenu.add(item("Race Strategy...", new Runnable() { public void run() { editRaceStrategy(); } }));
        editMenu.add(item("Car and AI Settings...", new Runnable() { public void run() { editCarSettings(); } }));
        editMenu.add(item("Horizon...", new Runnable() { public void run() { editHorizon(); } }));
        editMenu.addSeparator();
        editMenu.add(removeTrack);
        editMenu.add(removePit);
        editMenu.add(removeLine);

        viewMenu = new JMenu("View");
        viewMenu.setMnemonic('V');
        JMenuItem zoomIn = new JMenuItem(mapWindow.getZoomInAction());
        zoomIn.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_EQUALS, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        JMenuItem zoomOut = new JMenuItem(mapWindow.getZoomOutAction());
        zoomOut.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_MINUS, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        JMenuItem centre = new JMenuItem(mapWindow.getCentreAction());
        centre.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_0, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        viewMenu.add(zoomIn);
        viewMenu.add(zoomOut);
        viewMenu.add(centre);
        JMenuItem fit = new JMenuItem(mapWindow.getFitAction());
        fit.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        viewMenu.add(fit);
        viewMenu.addSeparator();
        viewMenu.add(new JMenuItem(mapWindow.getLayoutAction()));
        viewMenu.addSeparator();
        JMenuItem findList = item("Go to Segment List", new Runnable() { public void run() { treeWindow.focusTree(); } });
        findList.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        viewMenu.add(findList);
    }

    /** Builds the Tools menu items. The Remove All items are shown in the
        Edit menu instead. */
    private void initComponents() {
        toolsMenu = new javax.swing.JMenu();
        removeTrack = new javax.swing.JMenuItem();
        removePit = new javax.swing.JMenuItem();
        removeLine = new javax.swing.JMenuItem();
        generateLineGeometric = new javax.swing.JMenuItem();
        generateLineDataFit = new javax.swing.JMenuItem();
        generateLineRefine = new javax.swing.JMenuItem();
        generateLineFastest = new javax.swing.JMenuItem();
        repairLineItem = new javax.swing.JMenuItem();
        generateRandomTrack = new javax.swing.JMenuItem();
        reduceMinimalTrack = new javax.swing.JMenuItem();
        matchEndHeading = new javax.swing.JMenuItem();
        moveFromEdgeItem = new javax.swing.JMenuItem();

        removeTrack.setText("Remove All Track Segments...");
        removeTrack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeAllTrack(evt);
            }
        });
        removePit.setText("Remove All Pit Segments...");
        removePit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeAllPit(evt);
            }
        });
        removeLine.setText("Remove All Best Line Segments...");
        removeLine.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeAllLine(evt);
            }
        });

        toolsMenu.setText("Tools");
        toolsMenu.setMnemonic('T');
        generateLineGeometric.setText("Generate Best Line (Constructed Racing Line)...");
        generateLineGeometric.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openGenerateDialog(new cfevolution.generator.ccline.geometric.MinCurvatureCCLineGenerator());
            }
        });
        toolsMenu.add(generateLineGeometric);
        generateLineDataFit.setText("Generate Best Line (Learned from Original Tracks)...");
        generateLineDataFit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openGenerateDialog(new cfevolution.generator.ccline.datafit.DataFitCCLineGenerator());
            }
        });
        toolsMenu.add(generateLineDataFit);
        generateLineRefine.setText("Generate Best Line (Iterative Refinement)...");
        generateLineRefine.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openGenerateDialog(new cfevolution.generator.ccline.refine.RefinementCCLineGenerator());
            }
        });
        toolsMenu.add(generateLineRefine);
        generateLineFastest.setText("Generate Best Line (Fastest Lap Simulation)...");
        generateLineFastest.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_B,
            java.awt.event.InputEvent.CTRL_DOWN_MASK | java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        generateLineFastest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openGenerateDialog(new cfevolution.generator.ccline.geometric.FastestLapCCLineGenerator());
            }
        });
        toolsMenu.add(generateLineFastest);
        repairLineItem.setText("Repair Best Line After Edit");
        repairLineItem.setEnabled(false);
        repairLineItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                repairBestLine();
            }
        });
        toolsMenu.add(repairLineItem);
        toolsMenu.addSeparator();
        generateRandomTrack.setText("Generate Random Track...");
        generateRandomTrack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openGenerateTrackDialog();
            }
        });
        toolsMenu.add(generateRandomTrack);
        reduceMinimalTrack.setText("Reduce to Minimal Track...");
        reduceMinimalTrack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reduceToMinimalTrack();
            }
        });
        toolsMenu.add(reduceMinimalTrack);
        matchEndHeading.setText("Add Segment to Match Start/End Heading...");
        matchEndHeading.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addEndHeadingSegment();
            }
        });
        toolsMenu.add(matchEndHeading);
        moveFromEdgeItem.setText("Move Track Away From Map Edge");
        moveFromEdgeItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveTrackFromEdge();
            }
        });
        toolsMenu.add(moveFromEdgeItem);
        toolsMenu.addSeparator();
        JMenuItem checkTrack = new JMenuItem("Check Track...");
        checkTrack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkTrack();
            }
        });
        toolsMenu.add(checkTrack);
    }

    private void removeAllLine(java.awt.event.ActionEvent evt) {
        // Handles line removal menu event
        
        if (MessageBoxes.ask(this, cfevolution.edit.TrackOutline.clearQuestion(cfevolution.edit.TrackOutline.LINE)))
            treeWindow.deleteAllSegments(2);
        
    }

    private void removeAllPit(java.awt.event.ActionEvent evt) {
        // Handles pit removal menu event
        
        if (MessageBoxes.ask(this, cfevolution.edit.TrackOutline.clearQuestion(cfevolution.edit.TrackOutline.PIT)))
            treeWindow.deleteAllSegments(1);
        
    }

    private void removeAllTrack(java.awt.event.ActionEvent evt) {
        // Handles track removal menu event
        
        if (MessageBoxes.ask(this, cfevolution.edit.TrackOutline.clearQuestion(cfevolution.edit.TrackOutline.TRACK)))
            treeWindow.deleteAllSegments(0);
        
    }

    private void closeTrackItemActionPerformed(java.awt.event.ActionEvent evt) {
        // Handle close track menu event
        appFrame.closeTab(this);      
        
    }

    public void updateTrackMap()
    {
        // Send a command through the object model to update the track map.
        // Should really use interfaces for this, but this involves less thinking for now.

        mapWindow.redrawTrackMap();
        updateCCLineCoverageStatus();
        // Every edit path ends here, so this is where a change is noticed
        setModified(true);
    }

    public void updateCCLineCoverageStatus()
    {
        // Warn only when the best line covers fewer Segs than the track has:
        // uncovered Segs keep stale AI line values in the game. Overshoot is
        // normal — 14 of the 16 original tracks deliberately overrun so the
        // line wraps smoothly across the start/finish seam (see BESTLINE.md).
        // Shown in a banner above the map rather than in the title.
        warningBanner.removeAll();
        boolean any = false;
        for (final TrackTools.Banner b : TrackTools.banners(currentTrack))
        {
            addWarning(b.text, b.fixLabel, new Runnable() { public void run() { fixAction(b.action); } });
            any = true;
        }
        warningBanner.setVisible(any);
        warningBanner.revalidate();
        warningBanner.repaint();
    }

    private void addWarning(String text, String fixLabel, final Runnable fix)
    {
        JPanel row = new JPanel(new java.awt.BorderLayout(8, 0));
        row.setOpaque(false);
        JLabel label = new JLabel("\u26a0  " + text);
        label.setForeground(new java.awt.Color(0x6B4E00));
        row.add(label, java.awt.BorderLayout.CENTER);
        JButton button = new JButton(fixLabel);
        button.setFocusable(false);
        button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { fix.run(); }
        });
        row.add(button, java.awt.BorderLayout.EAST);
        row.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 6));
        warningBanner.add(row);
    }

    public void highlightSection(int sectionType, int segmentNo)
    {
        // Sends a command through the object model to highlight a section of the track map
        // Should really use interfaces for this, but this involves less thinking for now.

        mapWindow.selectedSegment(sectionType, segmentNo);

    }

    public void selectSegmentFromMap(int sectionType, int segmentNo)
    {
        // Called when the user clicks on a segment in the track map.
        // sectionType: 1 - Track, 2 - Pit, 3 - CCLine.
        // Selecting the tree node fires the normal selection listener,
        // which highlights the sector on the map in turn.

        treeWindow.selectSegmentNode(sectionType, segmentNo);
    }

    private void openGenerateDialog(cfevolution.generator.ccline.CCLineGenerator generator)
    {
        // Opens the shared best line generation dialog preset to a method
        Message blocked = TrackTools.bestLineBlocker(currentTrack);
        if (blocked != null)
        {
            MessageBoxes.show(this, blocked);
            return;
        }
        // A line generated on a track that crosses the map edge would be
        // built wrongly by the game, so move the track clear first.
        TrackTools.Outcome moved = TrackTools.moveBeforeBestLine(currentTrack);
        if (moved != null)
        {
            if (!moved.changed)
            {
                MessageBoxes.show(this, moved.message);
                return;
            }
            updateTrackMap();
            MessageBoxes.show(this, moved.message);
        }
        GenerateCCLineDialog dialog = new GenerateCCLineDialog(this, currentTrack, generator);
        dialog.setVisible(true);
    }


    private void openGenerateTrackDialog()
    {
        GenerateTrackDialog dialog = new GenerateTrackDialog(this, fileName);
        dialog.setVisible(true);
    }

    /** Applies a generated random layout plus its best line to the open
        track and refreshes every view. Called on the EDT by the dialog. */
    /** Returns how many scenery objects had to be dropped to keep the file
        under the size the game loads (see TrackFileSize): 0 normally, -1 if
        the track could not be brought under it. */
    /** Pit lane Segs of the open track (see Track.MAX_PIT_SEGS). */
    public int getPitSegCount()
    {
        return currentTrack == null ? 0 : currentTrack.getPitSegCount();
    }

    public int applyGeneratedTrack(cfevolution.generator.track.RandomTrackGenerator.Result layout,
                                   CCLine newLine)
    {
        int nTrimmed = RandomTrackGeneration.apply(currentTrack, layout, newLine);
        treeWindow.rebuildTrackNodes();
        highlightSection(1, -1); // no track segment selected any more
        highlightSection(2, -1); // pit selection also stale
        bestLineReplaced(); // rebuilds line nodes, map + title
        return nTrimmed;
    }

    /** Tools > cuts the track down to the stretch carrying the pit lane,
        the grid and the start/finish line, so a short but fully featured
        track can be built up from it. */
    private void reduceToMinimalTrack()
    {
        TrackTools.Plan plan = TrackTools.planMinimal(currentTrack);
        if (plan.failure != null)
        {
            MessageBoxes.show(this, plan.failure);
            return;
        }
        if (!MessageBoxes.ask(this, plan.question))
            return;
        TrackTools.Outcome done = plan.apply();
        if (done.changed)
        {
            treeWindow.rebuildTrackNodes();
            highlightSection(1, -1); // no track segment selected any more
            highlightSection(2, -1); // pit selection also stale
            bestLineReplaced();      // the best line was cleared
        }
        MessageBoxes.show(this, done.message);
    }

    /** Tools > adds one segment at the end so the last segment faces the
        way the first does. A track needs that to work; it does not need
        to close back onto its own start position. */
    private void addEndHeadingSegment()
    {
        TrackTools.Plan plan = TrackTools.planHeading(currentTrack);
        if (!plan.canApply())
        {
            MessageBoxes.show(this, plan.failure != null ? plan.failure : plan.nothingToDo);
            return;
        }
        if (!MessageBoxes.ask(this, plan.question))
            return;
        TrackTools.Outcome done = plan.apply();
        treeWindow.rebuildTrackNodes();
        highlightSection(1, -1);
        updateTrackMap();
        MessageBoxes.show(this, done.message);
    }

    /** Tools > moves a track that runs over the edge of the game's
        coordinate space into the middle of it (see TrackCoordinateEdge). */
    /** Track checker fix: the pit lane objects, joins and wall gaps where the
        lane leaves and rejoins the road (PitMouth). */
    private void setUpPitMouths()
    {
        TrackTools.Outcome done = TrackTools.setUpPitMouths(currentTrack);
        if (done.changed)
        {
            treeWindow.rebuildTrackNodes();
            updateTrackMap();
        }
        MessageBoxes.show(this, done.message);
    }

    private void moveTrackFromEdge()
    {
        TrackTools.Outcome done = TrackTools.moveFromEdge(currentTrack);
        if (done.changed)
            updateTrackMap();
        MessageBoxes.show(this, done.message);
    }


    public void showBestLinePreview(double[] adOffsets)
    {
        mapWindow.setPreviewProfile(adOffsets);
    }

    public void clearBestLinePreview()
    {
        mapWindow.clearPreview();
    }

    /** Replaces the whole best line with a generated one and refreshes
        every view that shows it. */
    public void replaceBestLine(CCLine newLine)
    {
        TrackTools.replaceBestLine(currentTrack, newLine);
        bestLineReplaced();
    }

    /** Refreshes every view of the best line after the whole of it changed. */
    private void bestLineReplaced()
    {
        treeWindow.rebuildBestLineNodes();
        highlightSection(3, -1); // nothing selected any more
        updateTrackMap();
        updateCCLineCoverageStatus();
        // The old line (and any edited-segment reference) is gone
        bestLineRepair.clear();
        repairLineItem.setEnabled(false);
    }

    // --- Repair after a manual best line edit -------------------------
    // Editing one sector shifts every sector after it (the line is
    // stateful). The snapshot taken just before an edit applies lets
    // Tools > Repair refit a bounded window of following sectors back
    // onto the old line via CCLineWindowRepair.

    private cfevolution.edit.BestLineRepair bestLineRepair;

    /** Called by the segment editor just BEFORE a best line edit is
        applied: captures the current (pre-edit) per-Seg line stamps. */
    public void prepareBestLineRepair(cfevolution.data.track.CCLineSegment lineSegment)
    {
        bestLineRepair.prepare(lineSegment);
    }

    /** Called just after the edit is applied. Promotes the snapshot only
        when the segment actually changed, so re-applying identical values
        keeps the original pre-edit target. */
    public void confirmBestLineRepair(boolean fChanged)
    {
        if (bestLineRepair.confirm(fChanged))
            repairLineItem.setEnabled(true);
    }

    private void repairBestLine()
    {
        cfevolution.edit.BestLineRepair.Result r = bestLineRepair.repair();
        if (r == null)
            return;
        if (r.repaired != null)
            replaceBestLine(r.repaired); // also clears the snapshot
        else if (!bestLineRepair.available())
            repairLineItem.setEnabled(false);
        MessageBoxes.show(this, r.message);
    }
    
    /** Warns, after every save, about the two ways a track can load as a
        frozen circuit preview: an overfull coaching table and an oversized
        file. Both are exact limits in the game, so both warnings mean the game
        WILL hang, not that it might.

        WHY A CHECK HERE RATHER THAN A CAP. Only some of the ways a line can be
        produced pass through CCLineQuantizer.capCoachingEntries. Completion
        mode, Repair Best Line After Edit and hand-editing do not, and
        hand-editing never could. Saving is the one place they all meet, so a
        check here covers every route.

        WHY A WARNING RATHER THAN SILENT CORRECTION. Getting under either limit
        means taking something out of the user's track, which should not happen
        without asking. The save goes ahead either way. */
    private void warnIfGameWillHang(File savedFile) {
        MessageBoxes.show(this, TrackTools.saveWarning(currentTrack, savedFile != null ? savedFile.length() : 0));
    }

    /** Applies typing in the property or command panel that has not been
        applied yet. Only real edits are applied: re-applying an untouched
        table can rewrite values it does not model exactly. */
    private void applyPendingEdits()
    {
        if (propertyEditor.hasPendingChanges())
            propertyEditor.applyChanges();
        if (commandEditor.hasPendingChanges())
            commandEditor.applyChanges();
    }

    public boolean save()
    {
        // Saves the current track file to the same file it was loaded from
        applyPendingEdits();
        File trackFile = new File(fileName);
        currentTrack.save(trackFile);
        setModified(false);
        warnIfGameWillHang(trackFile);
        return true;
    }

    public boolean saveAs()
    {
        // Prompts for a filename and then saves the current track file in memory to the specified filename
        applyPendingEdits();
        String selectedFileName;
        FileDialog fileDialog = new FileDialog(appFrame,true);
        try
        {
            selectedFileName = fileDialog.showSaveDialog();
        }
        catch (Exception exceptionError)
        {
            return false; // cancelled
        }
        if (selectedFileName.toLowerCase().endsWith(".dat") == false)
        {
            selectedFileName = selectedFileName + ".DAT";
        }
        File trackFile = new File(selectedFileName);
        if (trackFile.exists() == true)
        {
            JOptionPane.showMessageDialog(this, "The filename you have provided already exists. Please try again.", "Filename already exists", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        currentTrack.save(trackFile);
        // The tab now stands for the new file, so a later Save writes there
        fileName = selectedFileName;
        setModified(false);
        appFrame.updateRecentFileList(selectedFileName);
        warnIfGameWillHang(trackFile);
        return true;
    }

    // Variables declaration
    private javax.swing.JMenuItem generateLineGeometric;
    private javax.swing.JMenuItem generateLineDataFit;
    private javax.swing.JMenuItem generateLineRefine;
    private javax.swing.JMenuItem generateLineFastest;
    private javax.swing.JMenuItem repairLineItem;
    private javax.swing.JMenuItem generateRandomTrack;
    private javax.swing.JMenuItem reduceMinimalTrack;
    private javax.swing.JMenuItem matchEndHeading;
    private javax.swing.JMenuItem moveFromEdgeItem;
    private javax.swing.JMenuItem removeLine;
    private javax.swing.JMenuItem removePit;
    private javax.swing.JMenuItem removeTrack;
    private javax.swing.JMenu toolsMenu;
    // End of variables declaration
}
