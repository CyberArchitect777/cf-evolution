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
package cfevolution.gui.gamefiles;

import cfevolution.data.gamefiles.GameFile;
import cfevolution.gui.EditorTab;
import cfevolution.gui.FileDialog;
import cfevolution.gui.MainGUI;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import javax.swing.*;
import javax.swing.table.*;

/**
 * What the tabs for the game's smaller files share: saving with the
 * checksums resealed, Save As, the unsaved-changes state, and the page and
 * table styling of the executable tab.
 *
 * @author barrie
 */
public abstract class GameFileTab extends JPanel implements EditorTab
{
    protected final GameFile gameFile;
    protected String fileName;
    protected final MainGUI mainWindow;
    private boolean modified;

    protected GameFileTab(GameFile gameFile, String fileName, MainGUI mainWindow)
    {
        super(new BorderLayout());
        this.gameFile = gameFile;
        this.fileName = fileName;
        this.mainWindow = mainWindow;
    }

    protected void setModified(boolean value)
    {
        modified = value;
        if (mainWindow != null)
            mainWindow.tabStateChanged(this);
    }

    /** A page: title, one line of explanation, then the content. */
    protected static JPanel page(String title, String explanation, JComponent content)
    {
        JLabel heading = new JLabel(title);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 18f));
        JLabel text = new JLabel(explanation);
        text.setForeground(new Color(0x5A626C));
        JPanel top = new JPanel(new BorderLayout(0, 4));
        top.add(heading, BorderLayout.NORTH);
        top.add(text, BorderLayout.CENTER);
        top.setBorder(BorderFactory.createEmptyBorder(14, 16, 10, 16));
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(top, BorderLayout.NORTH);
        JPanel body = new JPanel(new BorderLayout());
        body.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 16));
        body.add(content, BorderLayout.CENTER);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    protected JTable table(AbstractTableModel model, int[] widths)
    {
        JTable table = new JTable(model);
        table.setRowHeight(24);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        table.getTableHeader().setReorderingAllowed(false);
        DefaultTableCellRenderer striped = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                           boolean focus, int row, int column) {
                super.getTableCellRendererComponent(t, value, selected, focus, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                setHorizontalAlignment(value instanceof Number ? RIGHT : LEFT);
                if (!selected)
                {
                    setBackground(row % 2 == 0 ? Color.WHITE : new Color(0xF6F7F9));
                    setForeground(t.isCellEditable(row, column) ? Color.BLACK : new Color(0x7A828C));
                }
                return this;
            }
        };
        table.setDefaultRenderer(Object.class, striped);
        table.setDefaultRenderer(Integer.class, striped);
        for (int i = 0; i < widths.length; i++)
            if (widths[i] > 0)
            {
                table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
                table.getColumnModel().getColumn(i).setMaxWidth(widths[i] * 2);
            }
        model.addTableModelListener(new javax.swing.event.TableModelListener() {
            public void tableChanged(javax.swing.event.TableModelEvent e) { setModified(true); }
        });
        return table;
    }

    /** A typed whole number inside [min, max], or null after saying why not. */
    protected Integer number(Object value, int min, int max, String what)
    {
        try
        {
            int n = Integer.parseInt(String.valueOf(value).trim());
            if (n >= min && n <= max)
                return n;
        }
        catch (NumberFormatException e)
        {
            // falls through to the message
        }
        JOptionPane.showMessageDialog(this, what + " must be a whole number from " + min + " to " + max + ".",
            "Value Not Changed", JOptionPane.WARNING_MESSAGE);
        return null;
    }

    /** Typed text no longer than maxLength, or null after saying why not. */
    protected String text(Object value, int maxLength, String what)
    {
        String text = String.valueOf(value);
        boolean plain = true;
        for (char c : text.toCharArray())
            plain &= c >= 32 && c < 256;
        if (text.length() > maxLength || !plain)
        {
            JOptionPane.showMessageDialog(this, what + " can be at most " + maxLength + " plain characters long.",
                "Value Not Changed", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return text;
    }

    // --- EditorTab ----------------------------------------------------------

    public JComponent getComponent() { return this; }

    public String getTabTitle() { return new File(fileName).getName(); }

    public String getFilePath() { return fileName; }

    public boolean isModified() { return modified; }

    public boolean save()
    {
        try
        {
            gameFile.save(new File(fileName));
        }
        catch (IOException e)
        {
            JOptionPane.showMessageDialog(this, new File(fileName).getName() + " could not be saved.", "Save Failed",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        setModified(false);
        return true;
    }

    public boolean saveAs()
    {
        FileDialog dialog = new FileDialog(mainWindow, true);
        dialog.setInitialDirectory(new File(fileName).getAbsoluteFile().getParent());
        String target;
        try
        {
            target = dialog.showSaveDialog();
        }
        catch (Exception e)
        {
            return false; // cancelled
        }
        String was = fileName;
        fileName = target;
        if (!save())
        {
            fileName = was;
            return false;
        }
        return true;
    }

    public JMenu[] getMenus() { return new JMenu[0]; }

    public JComponent[] getToolbarItems() { return new JComponent[0]; }

    public JMenuItem[] getFileMenuItems() { return new JMenuItem[0]; }
}
