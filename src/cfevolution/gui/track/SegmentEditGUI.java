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
 * TrackSegmentEditGUI.java
 *
 * Created on 22 May 2005, 22:40
 */

package cfevolution.gui.track;

import cfevolution.data.track.*;
import cfevolution.gui.table.SelectionTableCellEditor;
import cfevolution.gui.table.SelectionTableCellRenderer;
import cfevolution.gui.table.TextTableCellEditor;
import cfevolution.gui.table.TextTableCellRenderer;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;

/**
 * Property editor for the item selected in the track tree: the track data
 * headers, a track or pit segment, a best line segment, or one command's
 * parameters. It sits inside the track tab rather than opening a window.
 * Edits are held in the table until Apply; Revert reloads the item.
 *
 * @author  barrie
 */
public class SegmentEditGUI extends JPanel
{
    private TrackWindow parentTrackWindow;
    private TrackTreeSelector parentObjectWindow;
    private JLabel statusLabel;
    private boolean pendingChanges;

    // What is being edited, so Revert can reload it and Apply can relabel
    // the right tree node even after the selection has moved on.
    private Object editedObject;
    private String[] editedParamText;
    private Object editedNode;
    private Runnable afterApply;

    public SegmentEditGUI(TrackWindow currentTrackWindow, TrackTreeSelector currentObjectWindow)
    {
        super(new BorderLayout());
        initComponents();
        parentTrackWindow = currentTrackWindow;
        parentObjectWindow = currentObjectWindow;
        setPending(false);
        statusLabel = new JLabel(" ");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.ITALIC));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        statusLabel.setVisible(false);
        JPanel southWrapper = new JPanel(new BorderLayout());
        southWrapper.add(statusLabel, BorderLayout.NORTH);
        southWrapper.add(buttonPanel, BorderLayout.CENTER);
        add(southWrapper, BorderLayout.SOUTH);
    }

    /** The tree node whose label should follow this item's direction. */
    public void setEditedNode(Object node)
    {
        editedNode = node;
    }

    /** Run after each Apply (the command editor uses it to refresh its list). */
    public void setAfterApply(Runnable runnable)
    {
        afterApply = runnable;
    }

    /** The models leave their columns unnamed ("A", "B"). */
    private void labelColumns()
    {
        javax.swing.table.TableColumnModel columns = segmentTable.getColumnModel();
        if (columns.getColumnCount() >= 2)
        {
            columns.getColumn(0).setHeaderValue("Property");
            columns.getColumn(1).setHeaderValue("Value");
            segmentTable.getTableHeader().repaint();
        }
    }

    private void prepareTable()
    {
        segmentTable.setRowHeight(22);
        segmentTable.setDefaultRenderer(String.class, new TextTableCellRenderer());
        segmentTable.setDefaultRenderer(JComboBox.class, new SelectionTableCellRenderer());
        segmentTable.setDefaultEditor(String.class, new TextTableCellEditor());
        segmentTable.setDefaultEditor(JComboBox.class, new SelectionTableCellEditor());
    }

    /** Shows a property sheet from the core (cfevolution.edit.PropertySheet). */
    private void edit(cfevolution.edit.PropertySheet sheet, Object edited)
    {
        prepareTable();
        final PropertyTableModel model = new PropertyTableModel(sheet);
        segmentTable.setModel(model);
        labelColumns();
        if (sheet.note() != null)
        {
            statusLabel.setText("<html>" + sheet.note() + "</html>");
            statusLabel.setVisible(true);
            model.addTableModelListener(new TableModelListener() {
                public void tableChanged(TableModelEvent e) {
                    if (segmentTable.getModel() == model)
                        statusLabel.setText("<html>" + model.getSheet().note() + "</html>");
                }
            });
        }
        else
            statusLabel.setVisible(false);
        editedObject = edited;
        setPending(false);
    }

    public void editTrackSegment(TrackSegment trackSegment)
    {
        edit(cfevolution.edit.PropertySheet.segment(trackSegment), trackSegment);
    }

    public void editDataHeaders(TrackDataHeader trackHeaders)
    {
        edit(cfevolution.edit.PropertySheet.headers(trackHeaders), trackHeaders);
    }

    public void editBestLineSegment(CCLineSegment lineSegment)
    {
        edit(cfevolution.edit.PropertySheet.bestLine(lineSegment), lineSegment);
    }

    public void editCmdParams(Command currentCommand, String[] paramText)
    {
        edit(cfevolution.edit.PropertySheet.command(currentCommand, paramText), currentCommand);
        editedParamText = paramText;
    }

    /** Shows a short note instead of a table, e.g. when a branch is selected. */
    public void showNothing()
    {
        segmentTable.setModel(new javax.swing.table.DefaultTableModel());
        statusLabel.setVisible(false);
        editedObject = null;
        editedNode = null;
        setPending(false);
    }

    private void initComponents() {
        tableScroll = new JScrollPane();
        segmentTable = new cfevolution.gui.table.JExtendedTable();
        buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        applyButton = new JButton("Apply");
        revertButton = new JButton("Revert");

        segmentTable.setModel(new javax.swing.table.DefaultTableModel());
        segmentTable.setFillsViewportHeight(true);
        segmentTable.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                // A cell editor starting or stopping means the user typed into the table
                if ("tableCellEditor".equals(evt.getPropertyName()) && editedObject != null)
                    setPending(true);
            }
        });
        tableScroll.setViewportView(segmentTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        add(tableScroll, BorderLayout.CENTER);

        applyButton.setToolTipText("Write these values into the track (Ctrl+Enter)");
        applyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                applyChanges();
            }
        });
        revertButton.setToolTipText("Throw away the values typed here and show the track's values again");
        revertButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                revertChanges();
            }
        });
        buttonPanel.add(revertButton);
        buttonPanel.add(applyButton);

        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
            KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, java.awt.event.InputEvent.CTRL_DOWN_MASK), "apply");
        getActionMap().put("apply", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { applyChanges(); }
        });
    }

    private void setPending(boolean pending)
    {
        pendingChanges = pending;
        applyButton.setEnabled(pending);
        revertButton.setEnabled(pending);
    }

    public boolean hasPendingChanges()
    {
        return pendingChanges;
    }

    private void stopEditing()
    {
        if (segmentTable.isEditing() && segmentTable.getCellEditor() != null)
            segmentTable.getCellEditor().stopCellEditing();
    }

    /** Writes the table's values into the track. */
    public void applyChanges()
    {
        stopEditing();
        if (editedObject == null)
            return;
        if (!updateData())
            return;                      // refused: the typing stays, to be corrected
        setPending(false);
        if (afterApply != null)
            afterApply.run();
    }

    /** Reloads the edited item, discarding anything typed. */
    public void revertChanges()
    {
        if (segmentTable.isEditing() && segmentTable.getCellEditor() != null)
            segmentTable.getCellEditor().cancelCellEditing();
        Object node = editedNode;
        if (editedObject instanceof TrackSegment)
            editTrackSegment((TrackSegment) editedObject);
        else if (editedObject instanceof TrackDataHeader)
            editDataHeaders((TrackDataHeader) editedObject);
        else if (editedObject instanceof CCLineSegment)
            editBestLineSegment((CCLineSegment) editedObject);
        else if (editedObject instanceof Command)
            editCmdParams((Command) editedObject, editedParamText);
        editedNode = node;
    }

    private void relabelNode()
    {
        if (editedNode != null)
            parentObjectWindow.updateNodeLabel(editedNode);
    }

    /** Writes the table into the track; false (after saying why) when a value is not a number. */
    public boolean updateData()
    {
        if (!(segmentTable.getModel() instanceof PropertyTableModel))
            return true;
        PropertyTableModel model = (PropertyTableModel) segmentTable.getModel();
        cfevolution.edit.PropertySheet sheet = model.getSheet();
        cfevolution.edit.Message problem = sheet.problem();
        if (problem != null)
        {
            cfevolution.gui.MessageBoxes.show(this, problem);
            return false;
        }
        // Before a best line edit, snapshot the line so Tools > Repair can
        // refit the following sectors onto the old line
        if (sheet.what == cfevolution.edit.PropertySheet.LINE)
            parentTrackWindow.prepareBestLineRepair(sheet.lineSegment());
        cfevolution.edit.Message notice = sheet.apply();
        model.showSheet();
        cfevolution.gui.MessageBoxes.show(null, notice);
        if (sheet.what == cfevolution.edit.PropertySheet.LINE)
            parentTrackWindow.confirmBestLineRepair(sheet.lineChanged());
        parentTrackWindow.updateTrackMap();
        if (sheet.what == cfevolution.edit.PropertySheet.SEGMENT || sheet.what == cfevolution.edit.PropertySheet.LINE)
            relabelNode();
        return true;
    }

    // Variables declaration
    private JButton applyButton;
    private JPanel buttonPanel;
    private JButton revertButton;
    private cfevolution.gui.table.JExtendedTable segmentTable;
    private JScrollPane tableScroll;
    // End of variables declaration
}
