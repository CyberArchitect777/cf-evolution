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
 * CommandEditGUI.java
 *
 * Created on 29 September 2006, 21:46
 */

package cfevolution.gui.track;

import cfevolution.data.track.*;
import java.awt.*;
import java.util.Vector;
import javax.swing.*;

/**
 * The commands of the selected track or pit segment, inside the track
 * tab. Adding and removing a command takes effect straight away, like
 * adding or deleting a segment; a command's parameters are edited in the
 * table underneath and written with Apply.
 *
 * @author  barrie
 */
public class CommandEditGUI extends JPanel
{
    private CommandDataStore availableDataset;
    private TrackSegment trackSegment;
    private Vector<Command> currentSegmentCommands;
    private DefaultListModel<String> currentListData;
    private TrackWindow parentTrackWindow;
    private SegmentEditGUI paramEditor;
    private JLabel paramHeading;

    public CommandEditGUI(TrackWindow currentTrackWindow, TrackTreeSelector currentObjectWindow)
    {
        super(new BorderLayout());
        parentTrackWindow = currentTrackWindow;
        availableDataset = new CommandDataStore();
        currentSegmentCommands = new Vector<Command>();
        currentListData = new DefaultListModel<String>();
        paramEditor = new SegmentEditGUI(currentTrackWindow, currentObjectWindow);
        paramEditor.setAfterApply(new Runnable() {
            public void run() { refreshList(); }
        });
        initComponents();
        editSegment(null);
    }

    /** Shows the commands of a segment, or an empty list for null. */
    public void editSegment(TrackSegment currentSegment)
    {
        trackSegment = currentSegment;
        currentSegmentCommands = new Vector<Command>();
        if (trackSegment != null)
            currentSegmentCommands.addAll(trackSegment.getCommands());
        refreshList();
        boolean enabled = trackSegment != null;
        currentList.setEnabled(enabled);
        availableCombo.setEnabled(enabled);
        addCommand.setEnabled(enabled);
        removeCommand.setEnabled(enabled);
        showParams();
    }

    public boolean hasPendingChanges()
    {
        return paramEditor.hasPendingChanges();
    }

    public void applyChanges()
    {
        paramEditor.applyChanges();
    }

    private String describe(int type)
    {
        CommandData data = availableDataset.getCommandByType(type);
        if (data == null)
            return convertIntToHex(type);
        return data.getCommandDesc() + "  (" + convertIntToHex(type) + ")";
    }

    private void refreshList()
    {
        int selected = currentList == null ? -1 : currentList.getSelectedIndex();
        currentListData.clear();
        for (int x = 0; x < currentSegmentCommands.size(); x++)
            currentListData.addElement(describe(currentSegmentCommands.get(x).getType()));
        if (selected >= 0 && selected < currentListData.size())
            currentList.setSelectedIndex(selected);
        countLabel.setText(trackSegment == null ? "Select a track or pit segment to see its commands."
            : currentSegmentCommands.size() == 0 ? "This segment has no commands."
            : currentSegmentCommands.size() + (currentSegmentCommands.size() == 1 ? " command" : " commands"));
    }

    private void showParams()
    {
        int index = currentList.getSelectedIndex();
        if (index < 0 || index >= currentSegmentCommands.size())
        {
            paramHeading.setText("Parameters");
            paramEditor.showNothing();
            return;
        }
        Command selectedCommand = currentSegmentCommands.get(index);
        CommandData commandData = availableDataset.getCommandByType(selectedCommand.getType());
        if (commandData == null || commandData.getNumberOfParams() == 0)
        {
            paramHeading.setText("Parameters: this command has none");
            paramEditor.showNothing();
            return;
        }
        paramHeading.setText("Parameters of " + commandData.getCommandDesc());
        paramEditor.editCmdParams(selectedCommand, commandData.getParamArray());
    }

    private void commitList()
    {
        trackSegment.setCommands(new Vector<Command>(currentSegmentCommands));
        parentTrackWindow.updateTrackMap();
    }

    private void initComponents() {
        currentList = new JList<String>(currentListData);
        currentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        currentList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent e) {
                if (!e.getValueIsAdjusting())
                {
                    if (paramEditor.hasPendingChanges())
                        paramEditor.applyChanges();
                    showParams();
                }
            }
        });
        JScrollPane listScroll = new JScrollPane(currentList);
        listScroll.setPreferredSize(new Dimension(200, 150));

        countLabel = new JLabel(" ");
        countLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 4, 2));

        availableCombo = new JComboBox<String>();
        for (int x = 0; x < availableDataset.getCommandListSize(); x++)
        {
            CommandData data = availableDataset.getCommandByIndex(x);
            availableCombo.addItem(data.getCommandDesc() + "  (" + convertIntToHex(data.getCommandType()) + ")");
        }
        addCommand = new JButton("Add");
        addCommand.setToolTipText("Add the chosen command to this segment");
        addCommand.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                int index = availableCombo.getSelectedIndex();
                if (index < 0)
                    return;
                CommandData data = availableDataset.getCommandByIndex(index);
                currentSegmentCommands.add(new Command(data.getCommandType(), 0, 0, 0, 0, 0, 0));
                commitList();
                refreshList();
                currentList.setSelectedIndex(currentSegmentCommands.size() - 1);
            }
        });
        removeCommand = new JButton("Remove");
        removeCommand.setToolTipText("Remove the selected command from this segment");
        removeCommand.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                int index = currentList.getSelectedIndex();
                if (index < 0)
                    return;
                currentSegmentCommands.remove(index);
                commitList();
                refreshList();
                showParams();
            }
        });

        JPanel addRow = new JPanel(new BorderLayout(6, 0));
        addRow.add(availableCombo, BorderLayout.CENTER);
        JPanel addButtons = new JPanel(new GridLayout(1, 2, 6, 0));
        addButtons.add(addCommand);
        addButtons.add(removeCommand);
        addRow.add(addButtons, BorderLayout.EAST);
        addRow.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        JPanel top = new JPanel(new BorderLayout());
        top.add(countLabel, BorderLayout.NORTH);
        top.add(listScroll, BorderLayout.CENTER);
        top.add(addRow, BorderLayout.SOUTH);
        top.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        paramHeading = new JLabel("Parameters");
        paramHeading.setFont(paramHeading.getFont().deriveFont(Font.BOLD));
        paramHeading.setBorder(BorderFactory.createEmptyBorder(6, 6, 4, 6));
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(paramHeading, BorderLayout.NORTH);
        bottom.add(paramEditor, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
        split.setResizeWeight(0.45);
        split.setBorder(BorderFactory.createEmptyBorder());
        add(split, BorderLayout.CENTER);
    }

    public String convertIntToHex(int integerValue)
    {
        String hexCode = new String(Integer.toHexString(integerValue)).toUpperCase();
        return "0x" + hexCode;
    }

    // Variables declaration
    private JButton addCommand;
    private JComboBox<String> availableCombo;
    private JLabel countLabel;
    private JList<String> currentList;
    private JButton removeCommand;
    // End of variables declaration
}
