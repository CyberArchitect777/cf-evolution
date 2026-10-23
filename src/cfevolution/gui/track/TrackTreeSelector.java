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
 * TrackTreeSelector.java
 *
 * Created on 24 March 2005, 01:26
 */

package cfevolution.gui.track;

import cfevolution.data.track.*;
import javax.swing.tree.*;
import javax.swing.*;

/**
 *
 * @author  barrie
 */
public class TrackTreeSelector extends javax.swing.JPanel {
    
    private DefaultMutableTreeNode rootNode;
    private Track currentTrack;
    private TrackWindow parentTrackWindow;
    
    private DefaultMutableTreeNode mainTrackSegmentNode;
    private DefaultMutableTreeNode mainPitSegmentNode;
    private DefaultMutableTreeNode mainLineSegmentNode;
        
    /**
     * Creates new form TrackTreeSelector
     */
    public TrackTreeSelector(TrackWindow currentTrackWindow) {
        super(new java.awt.BorderLayout());
        initComponents();
        setDefaultElement();    
        createTree();
        parentTrackWindow = currentTrackWindow;
    }

    /** Expands the root so the four branch headings show, without opening
        the long segment lists. */
    public void showBranches()
    {
        trackDetails.expandPath(new TreePath(rootNode));
    }

    public void addAbove()
    {
        insertSegment(true);
    }

    public void addBelow()
    {
        insertSegment(false);
    }

    public void deleteSelected()
    {
        deleteButtonActionPerformed(null);
    }

    public void focusTree()
    {
        trackDetails.requestFocusInWindow();
    }

    /** Re-reads one segment node's direction label, e.g. after its
        curvature was edited. */
    public void updateNodeLabel(Object node)
    {
        if (!(node instanceof DefaultMutableTreeNode))
            return;
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) node;
        if (selectedNode.getParent() == null)
            return; // node no longer in the tree
        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) selectedNode.getParent();
        String text = selectedNode.toString();
        int spaceIndex = text.indexOf(" ");
        if (spaceIndex < 0)
            return;
        int segmentNo = Integer.parseInt(text.substring(0, spaceIndex));
        String direction = null;
        if (parentNode == mainTrackSegmentNode)
            direction = getSegmentDirection(currentTrack.getTrackSegments().getAt(segmentNo));
        else if (parentNode == mainPitSegmentNode)
            direction = getSegmentDirection(currentTrack.getPitlaneSegments().getAt(segmentNo));
        else if (parentNode == mainLineSegmentNode)
            direction = getBestLineDirection(currentTrack.getCCLine().getAt(segmentNo));
        if (direction == null)
            return;
        selectedNode.setUserObject(text.substring(0, spaceIndex) + " " + direction);
        ((DefaultTreeModel)(trackDetails.getModel())).nodeChanged(selectedNode);
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
            trackSegmentNode[x] = new DefaultMutableTreeNode(Integer.toString(x+1) + " " + getSegmentDirection(selectedTrackSegment));
            mainTrackSegmentNode.add(trackSegmentNode[x]);
        }       
        TrackSegments currentPitSegments = currentTrack.getPitlaneSegments();
        DefaultMutableTreeNode[] pitSegmentNode = new DefaultMutableTreeNode[currentPitSegments.size()];
        for (int x=0;x<currentPitSegments.size();x++)
        {
            TrackSegment selectedPitSegment = currentPitSegments.getAt(x+1);
            pitSegmentNode[x] = new DefaultMutableTreeNode(Integer.toString(x+1) + " " + getSegmentDirection(selectedPitSegment));
            mainPitSegmentNode.add(pitSegmentNode[x]);
        }    
        CCLine currentLineSegments = currentTrack.getCCLine();
        DefaultMutableTreeNode[] lineSegmentNode = new DefaultMutableTreeNode[currentLineSegments.size()];
        for (int x=0;x<currentLineSegments.size();x++)
        {
            CCLineSegment selectedLineSegment = currentLineSegments.getAt(x+1);
            lineSegmentNode[x] = new DefaultMutableTreeNode(Integer.toString(x+1) + " " + getBestLineDirection(selectedLineSegment));
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
     */
    private void initComponents() {
        treeViewScroll = new javax.swing.JScrollPane();
        trackDetails = new javax.swing.JTree();
        trackDetails.setShowsRootHandles(true);
        trackDetails.setRootVisible(false);
        trackDetails.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        trackDetails.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                setSelectedPaths(evt);
            }
        });
        trackDetails.setCellRenderer(new SegmentNodeRenderer());

        // Keys and a right-click menu replace the old button block
        javax.swing.JPopupMenu popup = new javax.swing.JPopupMenu();
        javax.swing.JMenuItem above = new javax.swing.JMenuItem("Add Segment Above");
        above.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { addAbove(); }
        });
        javax.swing.JMenuItem below = new javax.swing.JMenuItem("Add Segment Below");
        below.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { addBelow(); }
        });
        javax.swing.JMenuItem delete = new javax.swing.JMenuItem("Delete Segment");
        delete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { deleteSelected(); }
        });
        popup.add(above);
        popup.add(below);
        popup.addSeparator();
        popup.add(delete);
        trackDetails.setComponentPopupMenu(popup);
        trackDetails.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger() || javax.swing.SwingUtilities.isRightMouseButton(e))
                {
                    TreePath path = trackDetails.getPathForLocation(e.getX(), e.getY());
                    if (path != null)
                        trackDetails.setSelectionPath(path);
                }
            }
        });
        bindKey(java.awt.event.KeyEvent.VK_INSERT, 0, "addBelow", new Runnable() { public void run() { addBelow(); } });
        bindKey(java.awt.event.KeyEvent.VK_INSERT, java.awt.event.InputEvent.SHIFT_DOWN_MASK, "addAbove", new Runnable() { public void run() { addAbove(); } });
        bindKey(java.awt.event.KeyEvent.VK_DELETE, 0, "deleteSegment", new Runnable() { public void run() { deleteSelected(); } });

        treeViewScroll.setViewportView(trackDetails);
        treeViewScroll.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        add(treeViewScroll, java.awt.BorderLayout.CENTER);
    }

    private void bindKey(int keyCode, int modifiers, String name, final Runnable runnable)
    {
        trackDetails.getInputMap(javax.swing.JComponent.WHEN_FOCUSED).put(
            javax.swing.KeyStroke.getKeyStroke(keyCode, modifiers), name);
        trackDetails.getActionMap().put(name, new javax.swing.AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { runnable.run(); }
        });
    }

    /** Draws segment nodes with a small coloured mark for their direction,
        and the four branches in bold. */
    private static class SegmentNodeRenderer extends DefaultTreeCellRenderer
    {
        public java.awt.Component getTreeCellRendererComponent(javax.swing.JTree tree, Object value,
            boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus)
        {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            String text = String.valueOf(node.getUserObject());
            setFont(tree.getFont().deriveFont(node.getLevel() == 1 ? java.awt.Font.BOLD : java.awt.Font.PLAIN));
            setIcon(null);
            if (node.getLevel() == 2)
            {
                java.awt.Color mark = text.endsWith("Turn Left") ? new java.awt.Color(0x2F6FBF)
                                    : text.endsWith("Turn Right") ? new java.awt.Color(0xC2553A)
                                    : new java.awt.Color(0x9AA3AD);
                setIcon(new DotIcon(mark));
            }
            return this;
        }
    }

    private static class DotIcon implements javax.swing.Icon
    {
        private final java.awt.Color colour;
        DotIcon(java.awt.Color colour) { this.colour = colour; }
        public int getIconWidth() { return 10; }
        public int getIconHeight() { return 10; }
        public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y)
        {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(colour);
            g2.fillOval(x + 1, y + 1, 8, 8);
            g2.dispose();
        }
    }

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
    
    private void deleteButtonActionPerformed(java.awt.event.ActionEvent evt) {
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
                int segmentNo = Integer.parseInt((selectedNode.toString()).substring(0,spaceIndex));
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
        
    }

    private void addBelowButtonActionPerformed(java.awt.event.ActionEvent evt) {
        // Adds a new segment below the currently selected one
        
        insertSegment(false);
        
    }

    private void addAboveButtonActionPerformed(java.awt.event.ActionEvent evt) {
        // Adds a new segment above the currently selected one
        
        insertSegment(true);
        
    }

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
                int segmentNo = Integer.parseInt((selectedNode.toString()).substring(0,spaceIndex));
                if (((parentNode.toString()).equals("Track Segments")) || ((parentNode.toString()).equals("Pit Segments")))
                {
                    TrackSegments currentTrackSegments = new TrackSegments();
                    if ((parentNode.toString()).equals("Track Segments"))
                    {
                       currentTrackSegments = currentTrack.getTrackSegments();
                       TrackSegment newTrackSegment = currentTrackSegments.insertAt(segmentNo+positionCorrection);
                       mainTrackSegmentNode.insert(new DefaultMutableTreeNode(Integer.toString(200) + " " + getSegmentDirection(newTrackSegment)),(segmentNo-1)+positionCorrection);
                       int[] entryAdditions =  {(segmentNo-1)+positionCorrection};
                       ((DefaultTreeModel)(trackDetails.getModel())).nodesWereInserted(mainTrackSegmentNode, entryAdditions);
                       reIndexNodes(0);
                    }
                    else
                    {
                       currentTrackSegments = currentTrack.getPitlaneSegments();
                       TrackSegment newPitSegment = currentTrackSegments.insertAt(segmentNo+positionCorrection);
                       mainPitSegmentNode.insert(new DefaultMutableTreeNode(Integer.toString(200) + " " + getSegmentDirection(newPitSegment)),(segmentNo-1)+positionCorrection);
                       int[] entryAdditions =  {(segmentNo-1)+positionCorrection};
                       ((DefaultTreeModel)(trackDetails.getModel())).nodesWereInserted(mainPitSegmentNode, entryAdditions);
                       reIndexNodes(1);
                    }                
                }
                else
                {
                    CCLine currentLines = currentTrack.getCCLine();
                    CCLineSegment newLineSegment = currentLines.insertAt(segmentNo+positionCorrection);
                    mainLineSegmentNode.insert(new DefaultMutableTreeNode(Integer.toString(200) + " " + getBestLineDirection(newLineSegment)),(segmentNo-1)+positionCorrection);
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
                selectedNode.setUserObject(new String(Integer.toString(x+1) + " " + (selectedNode.toString()).substring(spaceIndex+1)));
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
                    selectedNode.setUserObject(new String(Integer.toString(x+1) + " " + (selectedNode.toString()).substring(spaceIndex+1)));
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
                        selectedNode.setUserObject(new String(Integer.toString(x+1) + " " + (selectedNode.toString()).substring(spaceIndex+1)));
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
    
    
    public void updateCurrentNode()
    {
        TreePath fullPath = trackDetails.getSelectionPath();
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)fullPath.getLastPathComponent();
        int spaceIndex = ((selectedNode.toString()).indexOf(" "));
        int segmentNo = Integer.parseInt((selectedNode.toString()).substring(0,spaceIndex));
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
    
    public void rebuildTrackNodes()
    {
        // Rebuilds the track segment branch of the tree from the current
        // track data, e.g. after a generated layout replaced all segments.
        // Mirrors the track segment loop of populateTree().

        mainTrackSegmentNode.removeAllChildren();
        TrackSegments currentTrackSegments = currentTrack.getTrackSegments();
        for (int x = 0; x < currentTrackSegments.size(); x++)
        {
            TrackSegment selectedTrackSegment = currentTrackSegments.getAt(x + 1);
            mainTrackSegmentNode.add(new DefaultMutableTreeNode(
                Integer.toString(x + 1) + " " + getSegmentDirection(selectedTrackSegment)));
        }
        ((DefaultTreeModel)(trackDetails.getModel())).nodeStructureChanged(mainTrackSegmentNode);
    }

    public void rebuildBestLineNodes()
    {
        // Rebuilds the best line branch of the tree from the current track
        // data, e.g. after a generated line replaced the whole CCLine.
        // Mirrors the best line loop of populateTree().

        mainLineSegmentNode.removeAllChildren();
        CCLine currentLineSegments = currentTrack.getCCLine();
        for (int x = 0; x < currentLineSegments.size(); x++)
        {
            CCLineSegment selectedLineSegment = currentLineSegments.getAt(x + 1);
            mainLineSegmentNode.add(new DefaultMutableTreeNode(
                Integer.toString(x + 1) + " " + getBestLineDirection(selectedLineSegment)));
        }
        ((DefaultTreeModel)(trackDetails.getModel())).nodeStructureChanged(mainLineSegmentNode);
    }

    public void selectSegmentNode(int sectionType, int segmentNo)
    {
        // Selects the given segment node (1-based) in the tree, e.g. after
        // the user clicks on an object in the track map.
        // sectionType: 1 - Track, 2 - Pit, 3 - CCLine.
        // Setting the selection fires the normal selection listener, which
        // highlights the sector on the track map.
        // Nodes are matched on their leading number rather than child index
        // so this stays correct if node text and order ever drift apart.

        DefaultMutableTreeNode parentNode;
        switch (sectionType)
        {
            case 1: parentNode = mainTrackSegmentNode; break;
            case 2: parentNode = mainPitSegmentNode; break;
            case 3: parentNode = mainLineSegmentNode; break;
            default: return;
        }

        for (int x = 0; x < parentNode.getChildCount(); x++)
        {
            DefaultMutableTreeNode segmentNode = (DefaultMutableTreeNode)parentNode.getChildAt(x);
            String nodeText = segmentNode.toString();
            try
            {
                int spaceIndex = nodeText.indexOf(" ");
                if (Integer.parseInt(nodeText.substring(0, spaceIndex)) == segmentNo)
                {
                    TreePath segmentPath = new TreePath(segmentNode.getPath());
                    trackDetails.setSelectionPath(segmentPath);
                    trackDetails.scrollPathToVisible(segmentPath);
                    return;
                }
            }
            catch (Exception exceptionError)
            {
                // Node text was not in the expected "<number> <direction>" form; skip it
            }
        }
    }

    private void currentTreePath()
    {
        // Calculates the current tree node selected, highlights it on the
        // map and shows it in the property panel.
        TreePath fullPath = trackDetails.getSelectionPath();
        notifySelection(fullPath);
        try 
        {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)fullPath.getLastPathComponent();
            if (selectedNode.getLevel() != 0)
            {
                TreePath parentPath = fullPath.getParentPath();
                DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)parentPath.getLastPathComponent();
                int spaceIndex = ((selectedNode.toString()).indexOf(" "));
                int segmentNo = Integer.parseInt((selectedNode.toString()).substring(0,spaceIndex));
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
    
    /** Tells the track window what is selected: the headers, a track, pit
        or best line segment, or nothing editable. */
    private void notifySelection(TreePath fullPath)
    {
        if (fullPath == null)
        {
            parentTrackWindow.selectionChanged(0, -1, null);
            return;
        }
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) fullPath.getLastPathComponent();
        if (selectedNode.getLevel() == 1)
        {
            parentTrackWindow.selectionChanged(selectedNode.toString().equals("Track Headers") ? 4 : 0, -1, selectedNode);
            return;
        }
        if (selectedNode.getLevel() != 2)
        {
            parentTrackWindow.selectionChanged(0, -1, null);
            return;
        }
        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) selectedNode.getParent();
        String text = selectedNode.toString();
        int segmentNo = Integer.parseInt(text.substring(0, text.indexOf(" ")));
        int kind = parentNode == mainTrackSegmentNode ? 1 : parentNode == mainPitSegmentNode ? 2 : 3;
        parentTrackWindow.selectionChanged(kind, segmentNo, selectedNode);
    }

    private void setSelectedPaths(javax.swing.event.TreeSelectionEvent evt) {
        
        
        // Notifies another method that the tree node selected has changed
               
        currentTreePath();
        
        
    }
    
    
    // Variables declaration
    private javax.swing.JTree trackDetails;
    private javax.swing.JScrollPane treeViewScroll;
    // End of variables declaration
    
}
