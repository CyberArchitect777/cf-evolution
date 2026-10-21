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
import cfevolution.gui.*;
import java.util.Vector;
import javax.swing.*;

/**
 *
 * @author  barrie
 */
public class CommandEditGUI extends javax.swing.JInternalFrame 
{
    
    private Vector<String> availableListData;
    private Vector<String> currentListData;
    private CommandDataStore availableDataset;
    private TrackSegment trackSegment;
    private Vector<Command> currentSegmentCommands;
    private JDesktopPane parentFrame;
    private TrackWindow parentTrackWindow;
    private TrackTreeSelector parentObjectWindow;
    
    /** Creates new form CommandEditGUI */
    public CommandEditGUI(TrackSegment currentSegment, JDesktopPane containerFrame, TrackWindow currentTrackWindow, TrackTreeSelector currentObjectWindow) 
    {
        initComponents();
        parentFrame = containerFrame;
        availableListData = new Vector<String>();
        currentListData = new Vector<String>();
        currentSegmentCommands = new Vector<Command>();
        availableDataset = new CommandDataStore();
        trackSegment = currentSegment;
        for (int x=0;x<trackSegment.getCommands().size();x++)
        {
            currentSegmentCommands.add(trackSegment.getCommands().get(x));
        }
        parentTrackWindow = currentTrackWindow;
        parentObjectWindow = currentObjectWindow;
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     */
    private void initComponents() {
        currentPanel = new javax.swing.JPanel();
        currentText = new javax.swing.JLabel();
        currentScrollPane = new javax.swing.JScrollPane();
        currentList = new javax.swing.JList<>();
        availablePanel = new javax.swing.JPanel();
        availableText = new javax.swing.JLabel();
        availableScrollPane = new javax.swing.JScrollPane();
        availableList = new javax.swing.JList<>();
        buttonPanel = new javax.swing.JPanel();
        addCommand = new javax.swing.JButton();
        removeCommand = new javax.swing.JButton();
        EditParams = new javax.swing.JButton();
        exitPanel = new javax.swing.JPanel();
        updateCommands = new javax.swing.JButton();
        cancelChanges = new javax.swing.JButton();

        getContentPane().setLayout(new java.awt.GridLayout(3, 1));

        setResizable(true);
        setPreferredSize(new java.awt.Dimension(447, 350));
        currentPanel.setLayout(new java.awt.BorderLayout());

        currentText.setText("Current Segment Commands (Number Of Parameters)");
        currentPanel.add(currentText, java.awt.BorderLayout.NORTH);

        currentScrollPane.setViewportView(currentList);

        currentPanel.add(currentScrollPane, java.awt.BorderLayout.CENTER);

        getContentPane().add(currentPanel);

        availablePanel.setLayout(new java.awt.BorderLayout());

        availableText.setText("Available Commands (Number Of Parameters)");
        availableText.setPreferredSize(new java.awt.Dimension(333, 15));
        availablePanel.add(availableText, java.awt.BorderLayout.NORTH);

        availableScrollPane.setViewportView(availableList);

        availablePanel.add(availableScrollPane, java.awt.BorderLayout.CENTER);

        getContentPane().add(availablePanel);

        buttonPanel.setLayout(new java.awt.GridLayout(4, 1, 5, 0));

        addCommand.setText("Add Available Command");
        addCommand.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addCommandActionPerformed(evt);
            }
        });

        buttonPanel.add(addCommand);

        removeCommand.setText("Remove Segment Command");
        removeCommand.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeCommandActionPerformed(evt);
            }
        });

        buttonPanel.add(removeCommand);

        EditParams.setText("Edit Command Parameters");
        EditParams.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EditParamsActionPerformed(evt);
            }
        });

        buttonPanel.add(EditParams);

        exitPanel.setLayout(new java.awt.GridLayout(1, 2));

        updateCommands.setText("Update Segment");
        updateCommands.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateCommandsActionPerformed(evt);
            }
        });

        exitPanel.add(updateCommands);

        cancelChanges.setText("Cancel Changes");
        cancelChanges.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelChangesActionPerformed(evt);
            }
        });

        exitPanel.add(cancelChanges);

        buttonPanel.add(exitPanel);

        getContentPane().add(buttonPanel);

        pack();
    }

    private void EditParamsActionPerformed(java.awt.event.ActionEvent evt) {
        // Edit the parameters of the selected current command
        
        if (currentList.getSelectedIndex() == -1)
        {
            JOptionPane.showMessageDialog(this, "No current segment has been selected", "Error", JOptionPane.ERROR_MESSAGE);
        }
        else
        {
            String commandString = currentListData.get(currentList.getSelectedIndex());
            CommandData commandData = availableDataset.getCommandByType(convertHexToInt(commandString.substring(0,4)));
            if (commandData.getNumberOfParams() == 0)
            {
                JOptionPane.showMessageDialog(this, "There are no parameters to edit for this command", "Error", JOptionPane.ERROR_MESSAGE);
            }
            else
            {
                Command selectedCommand = currentSegmentCommands.get(currentList.getSelectedIndex());
                SegmentEditGUI cmdParamEditor = new SegmentEditGUI(parentTrackWindow, parentObjectWindow);
                cmdParamEditor.editCmdParams(selectedCommand, commandData.getParamArray());
                cmdParamEditor.setVisible(true);
                cmdParamEditor.setTitle("Editing Parameters of Command " + convertIntToHex(selectedCommand.getType()));
                parentFrame.add(cmdParamEditor);
                cmdParamEditor.toFront();
                try
                {
                    cmdParamEditor.setSelected(true);
                }
                catch (Exception exceptionError)
                {
                    exceptionError.printStackTrace();
                }
            }
        }
        
    }

    private void updateCommandsActionPerformed(java.awt.event.ActionEvent evt) {
        // Updates a segment with the specified set of commands
        
        trackSegment.setCommands(currentSegmentCommands);
        parentTrackWindow.updateTrackMap();
        try
        {
            this.setClosed(true);
        }
        catch (Exception errorReport)
        {
            // This is a normal result of the closing of an internal window. Nothing should be done.
        }
        
        
    }

    private void removeCommandActionPerformed(java.awt.event.ActionEvent evt) {
        // Removes a command from the current segment
        
        if (currentList.getSelectedIndex() == -1)
        {
            JOptionPane.showMessageDialog(this, "No current segment has been selected", "Error", JOptionPane.ERROR_MESSAGE);
        }
        else
        {
            currentSegmentCommands.remove(currentList.getSelectedIndex());
            currentListData.remove(currentList.getSelectedIndex());
            currentList.setListData(currentListData);
        }
                
    }

    private void addCommandActionPerformed(java.awt.event.ActionEvent evt) {
        // Adds a new command to the current segment
        
        if (availableList.getSelectedIndex() == -1)
        {
            JOptionPane.showMessageDialog(this, "No available segment has been selected", "Error", JOptionPane.ERROR_MESSAGE);
        }
        else
        {
            String commandString = availableListData.get(availableList.getSelectedIndex());
            Command newCommand = new Command(convertHexToInt(commandString.substring(0,4)), 0, 0, 0, 0, 0, 0);
            currentListData.add(commandString);
            currentSegmentCommands.add(newCommand);
            currentList.setListData(currentListData);
        }
                
    }

    private void cancelChangesActionPerformed(java.awt.event.ActionEvent evt) {
        // Cancels all changes that have been made and closes the window
        
        try
        {
            this.setClosed(true);
        }
        catch (Exception errorReport)
        {
            // This is a normal result of the closing of an internal window. Nothing should be done.
        }
    }
    
    public void loadAllCommands()
    {
        // Load both all available commands and those associated with this particular track segment.
        
        for (int x=0;x<availableDataset.getCommandListSize();x++)
        {
            CommandData selectedCommand = availableDataset.getCommandByIndex(x);
            availableListData.add(convertIntToHex(selectedCommand.getCommandType()) + " " + "(" + selectedCommand.getNumberOfParams() + ") - "+ selectedCommand.getCommandDesc());
        }
        
        availableList.setListData(availableListData);
        
        Vector<Command> segmentCommands = trackSegment.getCommands();
        
        for (int x=0;x<segmentCommands.size();x++)
        {
            Command selectedCommand = segmentCommands.get(x);
            CommandData detectedCommand = availableDataset.getCommandByType(selectedCommand.getType());
            currentListData.add(convertIntToHex(detectedCommand.getCommandType()) + " " + "(" + detectedCommand.getNumberOfParams() + ") - " + detectedCommand.getCommandDesc());
        }
        
        currentList.setListData(currentListData);
        
    }
    
    public String convertIntToHex(int integerValue)
    {
        String hexCode = new String(Integer.toHexString(integerValue)).toUpperCase();
        return "0x" + hexCode;
    }
    
    public int convertHexToInt(String hexValue)
    {
        String bareHexValue = hexValue.substring(2);
        return Integer.parseInt(bareHexValue, 16);
    }
    
    
    // Variables declaration
    private javax.swing.JButton EditParams;
    private javax.swing.JButton addCommand;
    private javax.swing.JList<String> availableList;
    private javax.swing.JPanel availablePanel;
    private javax.swing.JScrollPane availableScrollPane;
    private javax.swing.JLabel availableText;
    private javax.swing.JPanel buttonPanel;
    private javax.swing.JButton cancelChanges;
    private javax.swing.JList<String> currentList;
    private javax.swing.JPanel currentPanel;
    private javax.swing.JScrollPane currentScrollPane;
    private javax.swing.JLabel currentText;
    private javax.swing.JPanel exitPanel;
    private javax.swing.JButton removeCommand;
    private javax.swing.JButton updateCommands;
    // End of variables declaration
    
}
