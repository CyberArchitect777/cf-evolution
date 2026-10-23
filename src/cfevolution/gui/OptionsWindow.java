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
 * OptionsWindow.java
 *
 * Created on 10 December 2006, 21:30
 */

package cfevolution.gui;

import java.util.prefs.Preferences;
import cfevolution.data.executable.Executable;
import java.io.*;
import javax.swing.*;
import cfevolution.data.gui.RecentFiles;

/**
 *
 * @author  barrie
 */
public class OptionsWindow extends javax.swing.JDialog 
{
    
    private MainGUI mainWindow;
    
    /** Creates new form OptionsWindow */
    public OptionsWindow(MainGUI passedMainWindow) 
    {
        super(passedMainWindow, "CF Evolution Options", true);
        initComponents();
        mainWindow = passedMainWindow;
        ((javax.swing.JComponent) getContentPane()).setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setSize(560,260);
        setLocationRelativeTo(passedMainWindow);
        interfaceModeSelector.addItem("CF Evolution");
        interfaceModeSelector.addItem("System Default");
        interfaceModeSelector.addItem("Metal");
        if (System.getProperty("os.name").toUpperCase().indexOf("WINDOWS") != -1)
        {
            interfaceModeSelector.addItem("Windows");            
        }
        if (System.getProperty("os.name").toUpperCase().indexOf("LINUX") != -1)
        {
            interfaceModeSelector.addItem("GTK");
        }
        interfaceModeSelector.addItem("Motif");
        Preferences userData = Preferences.userRoot();
        Preferences cheqFlagData = userData.node("cfevolution");
        String interfaceSetting = cheqFlagData.get(ThemeManager.PREF_KEY, "CF Evolution");
        if (interfaceSetting.compareTo("") != 0)
        {
            interfaceModeSelector.setSelectedItem(interfaceSetting);
        }
        executableLocation.setText(cheqFlagData.get("EXELocation", ""));
        legacyOptionsCheckBox.setSelected(cheqFlagData.getBoolean("EnableLegacyOptions", false));

    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     */
    private void initComponents() {
        lookFeelPanel = new javax.swing.JPanel();
        interfaceModeLabel = new javax.swing.JLabel();
        interfaceModeSelector = new javax.swing.JComboBox<>();
        executablePanel = new javax.swing.JPanel();
        executableLabelPanel = new javax.swing.JPanel();
        executableLabel = new javax.swing.JLabel();
        executableLocationPanel = new javax.swing.JPanel();
        executableLocation = new javax.swing.JTextField();
        executableSelector = new javax.swing.JButton();
        legacyOptionsPanel = new javax.swing.JPanel();
        legacyOptionsLabel = new javax.swing.JLabel();
        legacyOptionsCheckBox = new javax.swing.JCheckBox();
        clearRecent = new javax.swing.JButton();
        buttonPanel = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        getContentPane().setLayout(new java.awt.GridLayout(5, 1, 5, 5));

        lookFeelPanel.setLayout(new java.awt.GridLayout(1, 2, 5, 5));

        interfaceModeLabel.setText("Look and Feel Mode");
        lookFeelPanel.add(interfaceModeLabel);

        lookFeelPanel.add(interfaceModeSelector);

        getContentPane().add(lookFeelPanel);

        executablePanel.setLayout(new java.awt.GridLayout(1, 2, 5, 5));

        executableLabelPanel.setLayout(new java.awt.GridLayout(1, 1));

        executableLabel.setText("Location of F1GP/WC Executable");
        executableLabelPanel.add(executableLabel);

        executablePanel.add(executableLabelPanel);

        executableLocationPanel.setLayout(new java.awt.BorderLayout(5, 0));

        executableLocationPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 0, 5, 0));
        executableLocation.setColumns(20);
        executableLocationPanel.add(executableLocation, java.awt.BorderLayout.CENTER);

        executableSelector.setText("...");
        executableSelector.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                executableSelectorActionPerformed(evt);
            }
        });

        executableLocationPanel.add(executableSelector, java.awt.BorderLayout.EAST);

        executablePanel.add(executableLocationPanel);

        getContentPane().add(executablePanel);

        legacyOptionsPanel.setLayout(new java.awt.GridLayout(1, 2, 5, 5));

        legacyOptionsLabel.setText("Enable legacy options");
        legacyOptionsPanel.add(legacyOptionsLabel);

        legacyOptionsCheckBox.setToolTipText("Shows the older best line generation methods (Constructed Racing Line, Learned from Original Tracks, Iterative Refinement) in a track window's Tools menu. When off, only Fastest Lap Simulation is shown.");
        legacyOptionsPanel.add(legacyOptionsCheckBox);

        getContentPane().add(legacyOptionsPanel);

        clearRecent.setText("Clear Recent File List");
        clearRecent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearRecentActionPerformed(evt);
            }
        });

        getContentPane().add(clearRecent);

        buttonPanel.setLayout(new java.awt.GridLayout(1, 3, 5, 5));

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        buttonPanel.add(okButton);

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        buttonPanel.add(cancelButton);

        getContentPane().add(buttonPanel);

        pack();
    }

    private void clearRecentActionPerformed(java.awt.event.ActionEvent evt) {
        // Clears the recent file list
        
        int optionSelected = JOptionPane.showConfirmDialog(this, "Are you sure you want to immediately clear the recent file list?", "Clear Recent File List", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (optionSelected == JOptionPane.YES_OPTION)
        {
            RecentFiles usedFiles = new RecentFiles();
            usedFiles.clearRecentFiles();
            mainWindow.updateRecentFileList(""); // Passing empty new file which is flagged up as invalid in this method
        }
        
    }

    private void executableSelectorActionPerformed(java.awt.event.ActionEvent evt) {
        // Selects a executable filename
        
        FileDialog fileDialog = new FileDialog(mainWindow,true);  
        fileDialog.setFilterType(2);
        fileDialog.setInitialDirectory(executableLocation.getText());
        String fileName = new String();
        boolean cancelCalled = false;
        try
        {
            fileName = fileDialog.showOpenDialog();
        }
        catch (Exception exceptionError)
        {
            cancelCalled = true;
        }
        if (cancelCalled == false)
        {
            Executable gameExecutable = new Executable(new File(fileName));
            int exitCode = gameExecutable.loadData();
            switch(exitCode)
            {
                case 0: executableLocation.setText(fileName); break;
                case 1: JOptionPane.showMessageDialog(this, "The file specified is not a known F1GP/WC executable file. This may be because you have \nselected the wrong file, or because this executable has not been updated to version 1.05. \nIn the latter case, upgrading your copy of F1GP/WC to 1.05 will resolve the problem.", "Incompatible Executable Detected", JOptionPane.ERROR_MESSAGE); break;
                case 2: JOptionPane.showMessageDialog(this, "An unknown error has occurred during the loading of this executable file. \nIt is likely that this is either not a F1GP/WC executable file, or that the executable itself is corrupt.\nPlease try a valid executable file", "Unknown Error", JOptionPane.ERROR_MESSAGE); break;
                case 3: JOptionPane.showMessageDialog(this, "The executable file specified is a 1.05 packed version of F1GP/WC. \nAn unpacked version of this file is required for editing in CF Evolution. \nPlease see the documentation for more details", "Packed Executable Detected", JOptionPane.ERROR_MESSAGE); break;
            }
        }        
    }
       
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {
        // Saves all changes and applies chosen settings to the editor
        
        Preferences userData = Preferences.userRoot();
        Preferences cheqFlagData = userData.node("cfevolution");
        String selectedMode = (String)interfaceModeSelector.getSelectedItem();
        cheqFlagData.put(ThemeManager.PREF_KEY, selectedMode);
        ThemeManager userInterfaceTheme = new ThemeManager();
        userInterfaceTheme.setUserInterfaceType(selectedMode, mainWindow);
        cheqFlagData.put("EXELocation", executableLocation.getText());
        cheqFlagData.putBoolean("EnableLegacyOptions", legacyOptionsCheckBox.isSelected());
        mainWindow.refreshLegacyOptions();
        dispose();
           
    }

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {
        // Cancels any changes made and closes the form
        
        dispose();
    }
    
    
    // Variables declaration
    private javax.swing.JPanel buttonPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton clearRecent;
    private javax.swing.JPanel legacyOptionsPanel;
    private javax.swing.JLabel legacyOptionsLabel;
    private javax.swing.JCheckBox legacyOptionsCheckBox;
    private javax.swing.JLabel executableLabel;
    private javax.swing.JPanel executableLabelPanel;
    private javax.swing.JTextField executableLocation;
    private javax.swing.JPanel executableLocationPanel;
    private javax.swing.JPanel executablePanel;
    private javax.swing.JButton executableSelector;
    private javax.swing.JLabel interfaceModeLabel;
    private javax.swing.JComboBox<String> interfaceModeSelector;
    private javax.swing.JPanel lookFeelPanel;
    private javax.swing.JButton okButton;
    // End of variables declaration
    
}
