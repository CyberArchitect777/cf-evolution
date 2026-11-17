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

package cfevolution.gui;

import cfevolution.data.GameFolder;
import cfevolution.edit.AllCircuitsCars;
import cfevolution.edit.Message;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;

/**
 * The computer cars' cornering at every circuit of a game folder, on one page
 * (Game folder > Computer Cars at Every Circuit). The values, words and rules
 * are edit/AllCircuitsCars's; nothing is saved while a changed track is open
 * in a tab (the owner's choice, 2026-11-08).
 *
 * @author barrie
 */
public class AllCircuitsCarsDialog extends JDialog
{
    private final MainGUI mainWindow;
    private final AllCircuitsCars circuits;
    private final CircuitsModel model = new CircuitsModel();
    private final JTable table = new JTable(model);
    private final JLabel warning = new JLabel();
    private final JComboBox<String> which = new JComboBox<String>(AllCircuitsCars.WHICH);
    private final JSpinner setTo = new JSpinner(new SpinnerNumberModel(100, AllCircuitsCars.MIN, AllCircuitsCars.MAX, 1));
    private final JSpinner addBy = new JSpinner(new SpinnerNumberModel(5, -100, 100, 1));
    private boolean saved;

    public AllCircuitsCarsDialog(MainGUI mainWindow, GameFolder game)
    {
        super(mainWindow, AllCircuitsCars.TITLE, ModalityType.APPLICATION_MODAL);
        this.mainWindow = mainWindow;
        circuits = new AllCircuitsCars(game);

        JLabel intro = new JLabel("<html>" + AllCircuitsCars.INTRO.replace(">", "&gt;") + "</html>");
        intro.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        table.setName("circuits-table");
        table.setRowHeight(table.getRowHeight() + 4);
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(170);
        for (int i = 1; i <= AllCircuitsCars.WORDS.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(150);
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int row, int col)
            {
                Component c = super.getTableCellRendererComponent(t, v, sel, focus, row, col);
                if (!sel)
                    c.setForeground(!circuits.hasTrack(row) ? new Color(0xA0A6AE)
                                    : circuits.isChanged(row, col) ? new Color(0x1A5FB4) : t.getForeground());
                setHorizontalAlignment(col == 0 ? LEFT : CENTER);
                return c;
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        table.setPreferredScrollableViewportSize(new Dimension(640, table.getRowHeight() * GameFolder.CIRCUITS));

        which.setName("bulk-which");
        setTo.setName("bulk-set-value");
        addBy.setName("bulk-add-value");
        JButton setButton = new JButton("Set");
        setButton.setName("bulk-set");
        setButton.setToolTipText(AllCircuitsCars.SET_TIP);
        setButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { bulk(true, ((Number) setTo.getValue()).intValue()); }
        });
        JButton addButton = new JButton("Add");
        addButton.setName("bulk-add");
        addButton.setToolTipText(AllCircuitsCars.ADD_TIP);
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { bulk(false, ((Number) addBy.getValue()).intValue()); }
        });
        JButton reset = new JButton(AllCircuitsCars.UNDO);
        reset.setName("bulk-undo");
        reset.setToolTipText(AllCircuitsCars.UNDO_TIP);
        reset.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                stopEditing();
                circuits.undo();
                refresh();
            }
        });
        JPanel bulk = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        bulk.add(new JLabel("Change"));
        bulk.add(which);
        bulk.add(Box.createHorizontalStrut(8));
        bulk.add(new JLabel("Set to"));
        bulk.add(setTo);
        bulk.add(setButton);
        bulk.add(Box.createHorizontalStrut(8));
        bulk.add(new JLabel("Add"));
        bulk.add(addBy);
        bulk.add(addButton);
        bulk.add(Box.createHorizontalStrut(8));
        bulk.add(reset);
        JLabel bulkNote = new JLabel(AllCircuitsCars.BULK_NOTE);
        bulkNote.setForeground(new Color(0x5A626C));

        warning.setName("grip-warning");
        warning.setForeground(new Color(0xB04A00));
        warning.setText("<html>" + AllCircuitsCars.WARNING + "</html>");

        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[] { bulk, bulkNote, warning })
        {
            c.setAlignmentX(LEFT_ALIGNMENT);
            c.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
            south.add(c);
        }

        JPanel body = new JPanel(new BorderLayout());
        body.setBorder(BorderFactory.createEmptyBorder(12, 14, 4, 14));
        body.add(intro, BorderLayout.NORTH);
        body.add(scroll, BorderLayout.CENTER);
        body.add(south, BorderLayout.SOUTH);

        JButton save = new JButton("Save");
        save.setName("save-circuits");
        save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { save(); }
        });
        JButton close = new JButton("Close");
        close.setName("close-circuits");
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { closeAsked(); }
        });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(save);
        buttons.add(close);
        getRootPane().registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) { closeAsked(); }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { closeAsked(); }
        });

        getContentPane().add(body, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        pack();     // with the warning showing, so it has room when it appears
        refresh();
        setLocationRelativeTo(mainWindow);
    }

    /** Sets (or adds to) the chosen values at the selected circuits, or every circuit. */
    void bulk(boolean set, int value)
    {
        stopEditing();
        circuits.bulk(set, value, which.getSelectedIndex(), table.getSelectedRows());
        refresh();
    }

    private void stopEditing()
    {
        if (table.isEditing())
            table.getCellEditor().stopCellEditing();
    }

    private void refresh()
    {
        model.fireTableRowsUpdated(0, circuits.rows() - 1);
        warning.setVisible(circuits.raised());
    }

    boolean hasChanges()
    {
        return circuits.hasChanges();
    }

    /** Writes the changed circuits, unless one of them is open in a tab. */
    void save()
    {
        stopEditing();
        Message m = circuits.save(new AllCircuitsCars.OpenFiles() {
            public boolean isOpen(File file) { return mainWindow.isFileOpen(file.getPath()); }
        });
        if (m.title.equals("Saved") || m.title.equals("Not Saved"))
            saved = true;
        refresh();
        MessageBoxes.show(this, m);
    }

    private void closeAsked()
    {
        stopEditing();
        if (hasChanges())
        {
            Message q = AllCircuitsCars.CLOSE_QUESTION;
            int choice = JOptionPane.showOptionDialog(this, q.text, q.title,
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, q.options, q.options[1]);
            if (choice != 0)
                return;
        }
        dispose();
    }

    /** True if anything was written to disk while the page was open. */
    public boolean wasSaved()
    {
        return saved;
    }

    private class CircuitsModel extends AbstractTableModel
    {
        public int getRowCount() { return circuits.rows(); }
        public int getColumnCount() { return AllCircuitsCars.COLUMNS.length; }
        public String getColumnName(int col) { return AllCircuitsCars.COLUMNS[col]; }
        public Class<?> getColumnClass(int col) { return col == 0 ? String.class : Object.class; }
        public Object getValueAt(int row, int col) { return circuits.cell(row, col); }
        public boolean isCellEditable(int row, int col) { return circuits.isEditable(row, col); }

        public void setValueAt(Object value, int row, int col)
        {
            circuits.set(row, col, String.valueOf(value));
            refresh();
        }
    }
}
