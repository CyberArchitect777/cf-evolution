/*
 * Chequered Flag: An editor for Formula One Grand Prix/World Circuit
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
 * TrackTreeSelector.java
 *
 * Created on 24 March 2005, 01:26
 */

package chequeredflag.gui.track;

import chequeredflag.data.track.*;
import javax.swing.tree.*;
import javax.swing.*;

/**
 *
 * @author  barrie
 */
public class TrackTreeSelector extends javax.swing.JInternalFrame {
    
    private DefaultMutableTreeNode rootNode;
    private Track currentTrack;
    private JDesktopPane parentFrame;
    private TrackWindow parentTrackWindow;
    
    private DefaultMutableTreeNode mainTrackSegmentNode;
    private DefaultMutableTreeNode mainPitSegmentNode;
    private DefaultMutableTreeNode mainLineSegmentNode;
        
    /**
     * Creates new form TrackTreeSelector
     */
    public TrackTreeSelector(JDesktopPane containerFrame, TrackWindow currentTrackWindow) {
        initComponents();
        setDefaultElement();    
        createTree();
        parentFrame = containerFrame;
        parentTrackWindow = currentTrackWindow;
    }
    
    public void populateTree()
    {
        // Creates tree elements from information obtained from the current track
        
        DefaultMutableTreeNode mainTrackHeaderNode = new DefaultMutableTreeNode("Track Headers");
        mainTrackSegmentNode = new DefaultMutableTreeNode("Track Segments");
        mainPitSegmentNode = new DefaultMutableTreeNode("Pit Segments");
        mainLineSegmentNode = new DefaultMutableTreeNode("Best Line Segments");
        rootNode.add(mainTrackHeaderNode);
        rootNode.add(mainTrackSegmentNode);
        rootNode.add(mainPitSegmentNode);
        rootNode.add(mainLineSegmentNode);
        TrackSegments currentTrackSegments = currentTrack.getTrackSegments();
        DefaultMutableTreeNode[] trackSegmentNode = new DefaultMutableTreeNode[currentTrackSegments.size()];
        for (int x=0;x<currentTrackSegments.size();x++)
        {
            TrackSegment selectedTrackSegment = currentTrackSegments.getAt(x+1);
            trackSegmentNode[x] = new DefaultMutableTreeNode(new Integer(x+1).toString() + " " + getSegmentDirection(selectedTrackSegment));
            mainTrackSegmentNode.add(trackSegmentNode[x]);
        }       
        TrackSegments currentPitSegments = currentTrack.getPitlaneSegments();
        DefaultMutableTreeNode[] pitSegmentNode = new DefaultMutableTreeNode[currentPitSegments.size()];
        for (int x=0;x<currentPitSegments.size();x++)
        {
            TrackSegment selectedPitSegment = currentPitSegments.getAt(x+1);
            pitSegmentNode[x] = new DefaultMutableTreeNode(new Integer(x+1).toString() + " " + getSegmentDirection(selectedPitSegment));
            mainPitSegmentNode.add(pitSegmentNode[x]);
        }    
        CCLine currentLineSegments = currentTrack.getCCLine();
        DefaultMutableTreeNode[] lineSegmentNode = new DefaultMutableTreeNode[currentLineSegments.size()];
        for (int x=0;x<currentLineSegments.size();x++)
        {
            CCLineSegment selectedLineSegment = currentLineSegments.getAt(x+1);
            lineSegmentNode[x] = new DefaultMutableTreeNode(new Integer(x+1).toString() + " " + getBestLineDirection(selectedLineSegment));
            mainLineSegmentNode.add(lineSegmentNode[x]);
        }
    }
    
    public String getBestLineDirection(CCLineSegment currentSegment)
    {
        
        // Returns the CC Line direction from the passed segment
        
        if (currentSegment.getType() == 128) // 0x80 in hexidecimal 
        {
            if (currentSegment.getParam(2) == 0)
            {
                return "Straight";
            }
            else
            {
                if (currentSegment.getParam(2) < 0)
                {
                    return "Turn Left";
                }
                else
                {
                    return "Turn Right";
                }
            }
        }
        else
        {
            if (currentSegment.getParam(1) == 0)
            {
                return "Straight";
            }
            else
            {
                if (currentSegment.getParam(1) < 0)
                {
                    return "Turn Left";
                }
                else
                {
                    return "Turn Right";
                }
            }
            
        }
    }
    
    public String getSegmentDirection(TrackSegment currentSegment)
    {
        
        // Returns the direction from the passed segment
        
        if (currentSegment.getCurvature() < 0)
        {
            return "Turn Left";
        }
        else
        {
            if (currentSegment.getCurvature() > 0)
            {
                return "Turn Right";
            }
            else
            {
                return "Straight";
            }
        }
    }
    
    public void setTrack(Track selectedTrack)
    {
        // Passes the current track 
        
        currentTrack = selectedTrack;
    }
    
    private void setDefaultElement()
    {
        // Adds a single root node to the tree as part of initial configuration.
        rootNode = new DefaultMutableTreeNode("Track Data");        
    }
    
    private void createTree()
    {
        // Writes all current tree nodes to the tree interface
        
        trackDetails.setModel(new DefaultTreeModel(rootNode));
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        treeViewScroll = new javax.swing.JScrollPane();
        trackDetails = new javax.swing.JTree();
        buttonPanel = new javax.swing.JPanel();
        largeButtonPanel = new javax.swing.JPanel();
        editSegmentButton = new javax.swing.JButton();
        otherButtonPanel = new javax.swing.JPanel();
        editCommandsButton = new javax.swing.JButton();
        addAboveButton = new javax.swing.JButton();
        addBelowButton = new javax.swing.JButton();
        deleteButton = new javax.swing.JButton();

        setTitle("Object View");
        trackDetails.setShowsRootHandles(true);
        trackDetails.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                setSelectedPaths(evt);
            }
        });

        treeViewScroll.setViewportView(trackDetails);

        getContentPane().add(treeViewScroll, java.awt.BorderLayout.CENTER);

        buttonPanel.setLayout(new java.awt.GridLayout(2, 2));

        largeButtonPanel.setLayout(new java.awt.GridLayout(1, 1));

        editSegmentButton.setText("Edit Segment");
        editSegmentButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editObject(evt);
            }
        });

        largeButtonPanel.add(editSegmentButton);

        buttonPanel.add(largeButtonPanel);

        otherButtonPanel.setLayout(new java.awt.GridLayout(2, 2));

        editCommandsButton.setText("Edit Commands");
        editCommandsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editCommandsButtonActionPerformed(evt);
            }
        });

        otherButtonPanel.add(editCommandsButton);

        addAboveButton.setText("Add Section Above");
        addAboveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addAboveButtonActionPerformed(evt);
            }
        });

        otherButtonPanel.add(addAboveButton);

        addBelowButton.setText("Add Section Below");
        addBelowButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addBelowButtonActionPerformed(evt);
            }
        });

        otherButtonPanel.add(addBelowButton);

        deleteButton.setText("Delete Section");
        deleteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteButtonActionPerformed(evt);
            }
        });

        otherButtonPanel.add(deleteButton);

        buttonPanel.add(otherButtonPanel);

        getContentPane().add(buttonPanel, java.awt.BorderLayout.SOUTH);

        pack();
    }//GEN-END:initComponents

    public void deleteAllSegments(int segmentType)
    {
        // Delete all nodes (apart from one) for the given segment type.
        
        int deleteType = 0;
        
        if ((segmentType == 0) || (segmentType == 1))
        {
            TrackSegments currentTrackSegments = new TrackSegments();
            if (segmentType == 0)
            {
                currentTrackSegments = currentTrack.getTrackSegments();
                int originalSegmentNumber = currentTrackSegments.size();
                for (int x=1;x<originalSegmentNumber;x++)
                {
                    currentTrackSegments.deleteAt(2);
                    mainTrackSegmentNode.remove(1);
                }
                ((DefaultTreeModel)(trackDetails.getModel())).nodeStructureChanged(mainTrackSegmentNode);
                deleteType = 1;
                parentTrackWindow.updateTrackMap();
                parentTrackWindow.highlightSection(deleteType, -1); // Passing rogue detection value as no node will now be selected        
            }
            else
            {
                currentTrackSegments = currentTrack.getPitlaneSegments();
                int originalSegmentNumber = currentTrackSegments.size();
                for (int x=1;x<originalSegmentNumber;x++)
                {
                    currentTrackSegments.deleteAt(2);
                    mainPitSegmentNode.remove(1);
                }
                ((DefaultTreeModel)(trackDetails.getModel())).nodeStructureChanged(mainPitSegmentNode);
                deleteType = 2;
                parentTrackWindow.updateTrackMap();
                parentTrackWindow.highlightSection(deleteType, -1); // Passing rogue detection value as no node will now be selected        
            }
        }        
        else
        {
            CCLine currentLines = currentTrack.getCCLine();
            int originalLineNumber = currentLines.size();
            for (int x=1;x<originalLineNumber;x++)
            {
                currentLines.deleteAt(2);
                mainLineSegmentNode.remove(1);
            }
            ((DefaultTreeModel)(trackDetails.getModel())).nodeStructureChanged(mainLineSegmentNode);
            deleteType = 3;
            parentTrackWindow.updateTrackMap();
            parentTrackWindow.highlightSection(deleteType, -1); // Passing rogue detection value as no node will now be selected                    
        }
    }
    
    private void deleteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteButtonActionPerformed
        // Deletes the current segment
        
        TreePath fullPath = trackDetails.getSelectionPath();
        try
        {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)fullPath.getLastPathComponent();
            if (selectedNode.getLevel() > 1)
            {
                int deleteType = 0;
                TreePath parentPath = fullPath.getParentPath();
                DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)parentPath.getLastPathComponent();
                int spaceIndex = ((selectedNode.toString()).indexOf(" "));
                int segmentNo = new Integer((selectedNode.toString()).substring(0,spaceIndex)).intValue();
                if (((parentNode.toString()).equals("Track Segments")) || ((parentNode.toString()).equals("Pit Segments")))
                {
                    TrackSegments currentTrackSegments = new TrackSegments();
                    if ((parentNode.toString()).equals("Track Segments"))
                    {
                        currentTrackSegments = currentTrack.getTrackSegments();
                        if (currentTrackSegments.size() == 1)
                        {
                            JOptionPane.showMessageDialog(this, "The last remaining track segment cannot be deleted.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                        else
                        {
                            currentTrackSegments.deleteAt(segmentNo);
                            mainTrackSegmentNode.remove(segmentNo-1);
                            int[] entryDeletions = {(segmentNo-1)};
                            String[] dataDeletions = {selectedNode.toString()};
                            ((DefaultTreeModel)(trackDetails.getModel())).nodesWereRemoved(mainTrackSegmentNode, entryDeletions, dataDeletions);
                            reIndexNodes(0);
                            deleteType = 1;
                        }
                    }
                    else
                    {
                        currentTrackSegments = currentTrack.getPitlaneSegments();
                        if (currentTrackSegments.size() == 1)
                        {
                            JOptionPane.showMessageDialog(this, "The last remaining pit segment cannot be deleted.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                        else
                        {
                            currentTrackSegments.deleteAt(segmentNo);
                            mainPitSegmentNode.remove(segmentNo-1);
                            int[] entryDeletions = {(segmentNo-1)};
                            String[] dataDeletions = {selectedNode.toString()};
                            ((DefaultTreeModel)(trackDetails.getModel())).nodesWereRemoved(mainPitSegmentNode, entryDeletions, dataDeletions);
                            reIndexNodes(1);
                            deleteType = 2;
                        }
                    }
                }
                else
                {
                    CCLine currentLines = currentTrack.getCCLine();
                    if (currentLines.size() == 1)
                    {
                        JOptionPane.showMessageDialog(this, "The last remaining best line segment cannot be deleted.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                    else
                    {
                        currentLines.deleteAt(segmentNo);
                        mainLineSegmentNode.remove(segmentNo-1);
                        int[] entryDeletions = {(segmentNo-1)};
                        String[] dataDeletions = {selectedNode.toString()};
                        ((DefaultTreeModel)(trackDetails.getModel())).nodesWereRemoved(mainLineSegmentNode, entryDeletions, dataDeletions);
                        reIndexNodes(2);
                        deleteType = 3;
                    }
                }
                parentTrackWindow.updateTrackMap();
                parentTrackWindow.highlightSection(deleteType, -1); // Passing rogue detection value as no node will now be selected
            }
            else
            {
                JOptionPane.showMessageDialog(this, "Deleting sections is only possible in the track, pit or best line branches", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        catch (Exception exceptionError)
        {
            // No tree item selected, so exception thrown
        }                       
        
    }//GEN-LAST:event_deleteButtonActionPerformed

    private void addBelowButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addBelowButtonActionPerformed
        // Adds a new segment below the currently selected one
        
        insertSegment(false);
        
    }//GEN-LAST:event_addBelowButtonActionPerformed

    private void addAboveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addAboveButtonActionPerformed
        // Adds a new segment above the currently selected one
        
        insertSegment(true);
        
    }//GEN-LAST:event_addAboveButtonActionPerformed

    private void insertSegment(boolean aboveSelected)
    {
        // Adds a new segment above or below the selected item depending on the boolean parameter 
        
        int positionCorrection = 0;
        if (aboveSelected == false)
        {
            positionCorrection = 1;
        }
        TreePath fullPath = trackDetails.getSelectionPath();
        try
        {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)fullPath.getLastPathComponent();
            if (selectedNode.getLevel() > 1)
            {
                TreePath parentPath = fullPath.getParentPath();
                DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)parentPath.getLastPathComponent();
                int spaceIndex = ((selectedNode.toString()).indexOf(" "));
                int segmentNo = new Integer((selectedNode.toString()).substring(0,spaceIndex)).intValue();
                if (((parentNode.toString()).equals("Track Segments")) || ((parentNode.toString()).equals("Pit Segments")))
                {
                    TrackSegments currentTrackSegments = new TrackSegments();
                    if ((parentNode.toString()).equals("Track Segments"))
                    {
                       currentTrackSegments = currentTrack.getTrackSegments();
                       TrackSegment newTrackSegment = currentTrackSegments.insertAt(segmentNo+positionCorrection);
                       mainTrackSegmentNode.insert(new DefaultMutableTreeNode(new Integer(200).toString() + " " + getSegmentDirection(newTrackSegment)),(segmentNo-1)+positionCorrection);
                       int[] entryAdditions =  {(segmentNo-1)+positionCorrection};
                       ((DefaultTreeModel)(trackDetails.getModel())).nodesWereInserted(mainTrackSegmentNode, entryAdditions);
                       reIndexNodes(0);
                    }
                    else
                    {
                       currentTrackSegments = currentTrack.getPitlaneSegments();
                       TrackSegment newPitSegment = currentTrackSegments.insertAt(segmentNo+positionCorrection);
                       mainPitSegmentNode.insert(new DefaultMutableTreeNode(new Integer(200).toString() + " " + getSegmentDirection(newPitSegment)),(segmentNo-1)+positionCorrection);
                       int[] entryAdditions =  {(segmentNo-1)+positionCorrection};
                       ((DefaultTreeModel)(trackDetails.getModel())).nodesWereInserted(mainPitSegmentNode, entryAdditions);
                       reIndexNodes(1);
                    }                
                }
                else
                {
                    CCLine currentLines = currentTrack.getCCLine();
                    CCLineSegment newLineSegment = currentLines.insertAt(segmentNo+positionCorrection);
                    mainLineSegmentNode.insert(new DefaultMutableTreeNode(new Integer(200).toString() + " " + getBestLineDirection(newLineSegment)),(segmentNo-1)+positionCorrection);
                    int[] entryAdditions =  {(segmentNo-1)+positionCorrection};
                    ((DefaultTreeModel)(trackDetails.getModel())).nodesWereInserted(mainLineSegmentNode, entryAdditions);
                    reIndexNodes(2);
                }
            parentTrackWindow.updateTrackMap();
            currentTreePath();
            }
            else
            {
                JOptionPane.showMessageDialog(this, "Adding new sections is only possible in the track, pit or best line branches", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        catch (Exception exceptionError)
        {
            // No tree item selected, so exception thrown
        }
        
        //((DefaultTreeModel)trackDetails.getModel()).reload();
        //((DefaultTreeModel)(trackDetails.getModel())).node.nodeChanged(selectedNode);
        //trackDetails.validate();
        //trackDetails.setVisible(false);
        //trackDetails.setVisible(true);       
    }
    
    public void reIndexNodes(int nodeType)
    {
        int treeSize = 0;
        switch (nodeType)
        {
            case 0: treeSize = mainTrackSegmentNode.getChildCount(); break;
            case 1: treeSize = mainPitSegmentNode.getChildCount(); break;
            case 2: treeSize = mainLineSegmentNode.getChildCount(); break;
        }
        if (nodeType == 0)
        {
            for (int x=0;x<treeSize;x++)
            {
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)mainTrackSegmentNode.getChildAt(x);
                int spaceIndex = ((selectedNode.toString()).indexOf(" "));
                selectedNode.setUserObject(new String(new Integer(x+1).toString() + " " + (selectedNode.toString()).substring(spaceIndex+1)));
                try
                {
                    ((DefaultTreeModel)(trackDetails.getModel())).nodeChanged(selectedNode);
                }
                catch (Exception exceptionError)
                {
                    // Arrayindexoutofbounds for last element caught here. No obvious reason for it.
                    // This may be a Java bug with the current version (1.4.2) I'm using.
                }
            }            
        }
        else
        {
            if (nodeType == 1)
            {
                for (int x=0;x<treeSize;x++)
                {
                    DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)mainPitSegmentNode.getChildAt(x);
                    int spaceIndex = ((selectedNode.toString()).indexOf(" "));
                    selectedNode.setUserObject(new String(new Integer(x+1).toString() + " " + (selectedNode.toString()).substring(spaceIndex+1)));
                    try
                    {
                        ((DefaultTreeModel)(trackDetails.getModel())).nodeChanged(selectedNode);
                    }
                    catch (Exception exceptionError)
                    {
                        // Arrayindexoutofbounds for last element caught here. No obvious reason for it.
                        // This may be a Java bug with the current version (1.4.2) I'm using.
                    }
                }
            }
            else
            {
                if (nodeType == 2)
                {
                    for (int x=0;x<treeSize;x++)
                    {
                        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)mainLineSegmentNode.getChildAt(x);
                        int spaceIndex = ((selectedNode.toString()).indexOf(" "));
                        selectedNode.setUserObject(new String(new Integer(x+1).toString() + " " + (selectedNode.toString()).substring(spaceIndex+1)));
                        try
                        {
                            ((DefaultTreeModel)(trackDetails.getModel())).nodeChanged(selectedNode);
                        }
                        catch (Exception exceptionError)
                        {
                            // Arrayindexoutofbounds for last element caught here. No obvious reason for it.
                            // This may be a Java bug with the current version (1.4.2) I'm using.
                        }
                    }
                }
            }
        }
        trackDetails.validate();
        trackDetails.setVisible(false);
        trackDetails.setVisible(true);        
    }
    
    
    private void editCommandsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editCommandsButtonActionPerformed
        // Edit the current set of commands for the segment selected on the tree. Called by the 'Edit Commands' button
        
        boolean objectNotEditable = false;
        
        TreePath fullPath = trackDetails.getSelectionPath();
        try
        {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)fullPath.getLastPathComponent();
            if (selectedNode.getLevel() > 1)
            {
                    TreePath parentPath = fullPath.getParentPath();
                    DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)parentPath.getLastPathComponent();
                    int spaceIndex = ((selectedNode.toString()).indexOf(" "));
                    int segmentNo = new Integer((selectedNode.toString()).substring(0,spaceIndex)).intValue();
                    if (((parentNode.toString()).equals("Track Segments")) || ((parentNode.toString()).equals("Pit Segments")))
                    {
                        TrackSegments currentTrackSegments = new TrackSegments();
                        if ((parentNode.toString()).equals("Track Segments"))
                        {
                            currentTrackSegments = currentTrack.getTrackSegments();
                        }
                        else
                        {
                            currentTrackSegments = currentTrack.getPitlaneSegments();   
                        }
                        TrackSegment selectedTrackSegment = currentTrackSegments.getAt(segmentNo);
                        CommandEditGUI commandEditor = new CommandEditGUI(selectedTrackSegment, parentFrame, parentTrackWindow, this);
                        commandEditor.loadAllCommands();
                        commandEditor.setVisible(true);
                        if ((parentNode.toString()).equals("Track Segments"))
                        {
                            commandEditor.setTitle("Editing Commands for Track Segment " + segmentNo);
                        }
                        else
                        {
                            commandEditor.setTitle("Editing Commands for Pit Segment " + segmentNo);
                        }
                        parentFrame.add(commandEditor);
                        commandEditor.toFront();
                        try
                        {
                            commandEditor.setSelected(true);
                        }
                        catch (Exception exceptionError)
                        {
                            exceptionError.printStackTrace();
                        }
                    }
                    else
                    {
                        objectNotEditable = true;
                    }        
            }
            else
            {  
                objectNotEditable = true;
            }
        
            if (objectNotEditable == true)
            {
                JOptionPane.showMessageDialog(this, "Command editing is only available for individual track or pit track segments", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        catch (Exception exceptionError)
        {
            // No tree item selected, so exception thrown
        }
        
    }//GEN-LAST:event_editCommandsButtonActionPerformed

    private void editObject(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editObject
        // Edit the current object selected on the tree. Called natively by Edit button
        
        TreePath fullPath = trackDetails.getSelectionPath();
        try
        {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)fullPath.getLastPathComponent();
            if (selectedNode.toString().equals("Track Headers"))
            {
                TrackDataHeader currentHeaders = currentTrack.getTrackDataHeader();
                SegmentEditGUI headersEditor = new SegmentEditGUI(parentTrackWindow, this);
                headersEditor.editDataHeaders(currentHeaders);
                headersEditor.setVisible(true);
                headersEditor.setTitle("Editing Track Data Headers");
                parentFrame.add(headersEditor);
                headersEditor.toFront();
                try
                {
                    headersEditor.setSelected(true);
                }
                catch (Exception exceptionError)
                {
                    exceptionError.printStackTrace();
                }
            }
            else
            {
                if (selectedNode.getLevel() > 1)
                {
                    TreePath parentPath = fullPath.getParentPath();
                    DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)parentPath.getLastPathComponent();
                    int spaceIndex = ((selectedNode.toString()).indexOf(" "));
                    int segmentNo = new Integer((selectedNode.toString()).substring(0,spaceIndex)).intValue();
                    if (((parentNode.toString()).equals("Track Segments")) || ((parentNode.toString()).equals("Pit Segments")))
                    {
                        TrackSegments currentTrackSegments = new TrackSegments();
                        if ((parentNode.toString()).equals("Track Segments"))
                        {
                            currentTrackSegments = currentTrack.getTrackSegments();
                        }
                        else
                        {
                            currentTrackSegments = currentTrack.getPitlaneSegments();   
                        }
                        TrackSegment selectedTrackSegment = currentTrackSegments.getAt(segmentNo);
                        SegmentEditGUI segmentEditor = new SegmentEditGUI(parentTrackWindow, this);
                        segmentEditor.editTrackSegment(selectedTrackSegment);
                        segmentEditor.setVisible(true);
                        if ((parentNode.toString()).equals("Track Segments"))
                        {
                            segmentEditor.setTitle("Editing Track Segment " + segmentNo);
                        }
                        else
                        {
                            segmentEditor.setTitle("Editing Pit Segment " + segmentNo);
                        }
                        parentFrame.add(segmentEditor);
                        segmentEditor.toFront();
                        try
                        {
                            segmentEditor.setSelected(true);
                        }
                        catch (Exception exceptionError)
                        {
                            exceptionError.printStackTrace();
                        }
                    }
                    else
                    {
                        if ((parentNode.toString()).equals("Best Line Segments"))
                        {   
                            CCLine currentLines = currentTrack.getCCLine();
                            CCLineSegment selectedBestLine = currentLines.getAt(segmentNo);
                            SegmentEditGUI segmentEditor = new SegmentEditGUI(parentTrackWindow, this);
                            segmentEditor.editBestLineSegment(selectedBestLine);
                            segmentEditor.setVisible(true);
                            segmentEditor.setTitle("Editing Best Line Segment " + segmentNo);
                            parentFrame.add(segmentEditor);
                            segmentEditor.toFront();
                            try
                            {
                                segmentEditor.setSelected(true);
                            }
                            catch (Exception exceptionError)
                            {
                                exceptionError.printStackTrace();
                            }
                        }                         
                    }
                }
                else
                {
                    JOptionPane.showMessageDialog(this, "Segment editing is only available for either track headers or individual track, pit or best line segments", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
        catch (Exception exceptionError)
        {
            // No tree item selected, so exception thrown
        }
    }//GEN-LAST:event_editObject

    public void updateCurrentNode()
    {
        TreePath fullPath = trackDetails.getSelectionPath();
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)fullPath.getLastPathComponent();
        int spaceIndex = ((selectedNode.toString()).indexOf(" "));
        int segmentNo = new Integer((selectedNode.toString()).substring(0,spaceIndex)).intValue();
        TreePath parentPath = fullPath.getParentPath();
        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)parentPath.getLastPathComponent();
        if (((parentNode.toString()).equals("Track Segments")) || ((parentNode.toString()).equals("Pit Segments")))
        {
            TrackSegments currentTrackSegments = new TrackSegments();
            if ((parentNode.toString()).equals("Track Segments"))
            {
                currentTrackSegments = currentTrack.getTrackSegments();
            }
            else
            {
                currentTrackSegments = currentTrack.getPitlaneSegments();   
            }
            TrackSegment selectedTrackSegment = currentTrackSegments.getAt(segmentNo);
            selectedNode.setUserObject(new String((selectedNode.toString()).substring(0, spaceIndex) + " " + getSegmentDirection(selectedTrackSegment)));
        }
        else
        {
            if ((parentNode.toString()).equals("Best Line Segments"))
            {   
                CCLine currentLines = currentTrack.getCCLine();
                CCLineSegment selectedBestLine = currentLines.getAt(segmentNo);
                selectedNode.setUserObject(new String((selectedNode.toString()).substring(0, spaceIndex) + " " + getBestLineDirection(selectedBestLine)));
            }
        }
        ((DefaultTreeModel)(trackDetails.getModel())).nodeChanged(selectedNode);
        trackDetails.validate();
        trackDetails.setVisible(false);
        trackDetails.setVisible(true);
    }
    
    private void currentTreePath()
    {
        // Calculates the current tree node selected.
    
        TreePath fullPath = trackDetails.getSelectionPath();
        try 
        {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)fullPath.getLastPathComponent();
            if (selectedNode.getLevel() != 0)
            {
                TreePath parentPath = fullPath.getParentPath();
                DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)parentPath.getLastPathComponent();
                int spaceIndex = ((selectedNode.toString()).indexOf(" "));
                int segmentNo = new Integer((selectedNode.toString()).substring(0,spaceIndex)).intValue();
                if (parentNode.toString() == "Track Segments")
                {
                    parentTrackWindow.highlightSection(1, segmentNo);
                }
                else
                {
                    if (parentNode.toString() == "Pit Segments")
                    {
                        parentTrackWindow.highlightSection(2, segmentNo);
                    }
                    else
                    {
                        parentTrackWindow.highlightSection(3, segmentNo);
                    }
                }
            }
        }
        catch (Exception exceptionError)
        {
            // No tree item selected, so exception thrown
        }
    }
    
    private void setSelectedPaths(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_setSelectedPaths
        
        
        // Notifies another method that the tree node selected has changed
               
        currentTreePath();
        
        
    }//GEN-LAST:event_setSelectedPaths
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addAboveButton;
    private javax.swing.JButton addBelowButton;
    private javax.swing.JPanel buttonPanel;
    private javax.swing.JButton deleteButton;
    private javax.swing.JButton editCommandsButton;
    private javax.swing.JButton editSegmentButton;
    private javax.swing.JPanel largeButtonPanel;
    private javax.swing.JPanel otherButtonPanel;
    private javax.swing.JTree trackDetails;
    private javax.swing.JScrollPane treeViewScroll;
    // End of variables declaration//GEN-END:variables
    
}
