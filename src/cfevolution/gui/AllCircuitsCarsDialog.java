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
import cfevolution.data.track.CCSetup;
import cfevolution.data.track.Track;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;

/**
 * The computer cars' cornering at every circuit of a game folder, on one page
 * (the job GPCSetup and GPCWinset did): the three CC aggression words of each
 * track's CC setup, which ArgData calls LateBrakingFactor. The same values as
 * Edit > Car and AI Settings, but for all 16 tracks at once, with buttons to
 * set or raise them everywhere.
 *
 * Only the 38-byte CC setup of a changed track is written, in place, and the
 * checksum resealed (Track.saveCCSetupOnly). Nothing is saved while a changed
 * track is open in a tab, so the tab cannot later write its own copy over
 * these changes (the owner's choice, 2026-11-08).
 *
 * @author barrie
 */
public class AllCircuitsCarsDialog extends JDialog
{
    /** The three cornering words, in column order, with their column titles. */
    static final int[] WORDS = { CCSetup.AGGRESSION_PRACTICE, CCSetup.AGGRESSION_RACE, CCSetup.AGGRESSION_WET };
    static final String[] TITLES = { "Practice and qualifying (%)", "Race (%)", "Wet race (%)" };
    /** The range Edit > Car and AI Settings allows. */
    static final int MIN = 50, MAX = 150;

    /** One circuit: its track, or why there is none, and the values shown. */
    static class Circuit
    {
        final int slot;
        final File file;
        final Track track;           // null if the file is missing or unreadable
        final String problem;
        final int[] loaded = new int[WORDS.length], shown = new int[WORDS.length];

        Circuit(int slot, File file)
        {
            this.slot = slot;
            this.file = file;
            Track t = null;
            String why = null;
            if (file == null)
                why = GameFolder.trackFileName(slot) + " is not in this folder";
            else
            {
                t = new Track();
                if (!t.load(file))
                {
                    t = null;
                    why = file.getName() + " could not be read";
                }
            }
            track = t;
            problem = why;
            for (int i = 0; i < WORDS.length; i++)
                loaded[i] = shown[i] = track != null ? track.getCCSetup().getPercent(WORDS[i]) : 0;
        }

        boolean changed()
        {
            for (int i = 0; i < WORDS.length; i++)
                if (shown[i] != loaded[i])
                    return true;
            return false;
        }
    }

    private final MainGUI mainWindow;
    private final List<Circuit> circuits = new ArrayList<Circuit>();
    private final CircuitsModel model = new CircuitsModel();
    private final JTable table = new JTable(model);
    private final JLabel warning = new JLabel();
    private final JComboBox<String> which = new JComboBox<String>(new String[] {
        "All three", "Practice and qualifying", "Race", "Wet race" });
    private final JSpinner setTo = new JSpinner(new SpinnerNumberModel(100, MIN, MAX, 1));
    private final JSpinner addBy = new JSpinner(new SpinnerNumberModel(5, -100, 100, 1));
    private boolean saved;

    public AllCircuitsCarsDialog(MainGUI mainWindow, GameFolder game)
    {
        super(mainWindow, "Computer Cars at Every Circuit", ModalityType.APPLICATION_MODAL);
        this.mainWindow = mainWindow;
        for (int slot = 1; slot <= GameFolder.CIRCUITS; slot++)
            circuits.add(new Circuit(slot, game.getTrack(slot)));

        JLabel intro = new JLabel("<html>How hard the computer cars corner at each circuit, as in Edit &gt; Car and AI Settings. "
            + "Higher means they brake later and take bends faster. The original circuits use 98 to 103.</html>");
        intro.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        table.setName("circuits-table");
        table.setRowHeight(table.getRowHeight() + 4);
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(170);
        for (int i = 1; i <= WORDS.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(150);
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int row, int col)
            {
                Component c = super.getTableCellRendererComponent(t, v, sel, focus, row, col);
                Circuit ci = circuits.get(row);
                if (!sel)
                    c.setForeground(ci.track == null ? new Color(0xA0A6AE)
                                    : col > 0 && ci.shown[col - 1] != ci.loaded[col - 1] ? new Color(0x1A5FB4) : t.getForeground());
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
        setButton.setToolTipText("Gives the chosen values this percentage at every circuit (or the selected circuits)");
        setButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { bulk(true, ((Number) setTo.getValue()).intValue()); }
        });
        JButton addButton = new JButton("Add");
        addButton.setName("bulk-add");
        addButton.setToolTipText("Adds this much to the chosen values at every circuit (or the selected circuits); use a minus number to lower them");
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { bulk(false, ((Number) addBy.getValue()).intValue()); }
        });
        JButton reset = new JButton("Undo Changes");
        reset.setName("bulk-undo");
        reset.setToolTipText("Puts back the values the circuits had when this page opened");
        reset.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                stopEditing();
                for (Circuit c : circuits)
                    System.arraycopy(c.loaded, 0, c.shown, 0, WORDS.length);
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
        JLabel bulkNote = new JLabel("Applies to the selected circuits, or to every circuit when none is selected.");
        bulkNote.setForeground(new Color(0x5A626C));

        warning.setName("grip-warning");
        warning.setForeground(new Color(0xB04A00));
        warning.setText("<html>Computer cars that corner harder crash and spin more often. If they do, raising the AI grip level "
            + "on the Game Settings page of GP.EXE helps them stay on the road.</html>");

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
        int[] rows = table.getSelectedRows();
        int pick = which.getSelectedIndex();
        for (int r = 0; r < circuits.size(); r++)
        {
            Circuit c = circuits.get(r);
            if (c.track == null || (rows.length > 0 && java.util.Arrays.binarySearch(rows, r) < 0))
                continue;
            for (int i = 0; i < WORDS.length; i++)
                if (pick == 0 || pick == i + 1)
                    c.shown[i] = Math.max(MIN, Math.min(MAX, set ? value : c.shown[i] + value));
        }
        refresh();
    }

    private void stopEditing()
    {
        if (table.isEditing())
            table.getCellEditor().stopCellEditing();
    }

    private void refresh()
    {
        model.fireTableRowsUpdated(0, circuits.size() - 1);
        boolean raised = false;
        for (Circuit c : circuits)
            for (int i = 0; i < WORDS.length; i++)
                raised |= c.track != null && c.shown[i] > c.loaded[i];
        warning.setVisible(raised);
    }

    boolean hasChanges()
    {
        for (Circuit c : circuits)
            if (c.changed())
                return true;
        return false;
    }

    /** Writes the changed circuits, unless one of them is open in a tab. */
    void save()
    {
        stopEditing();
        List<Circuit> changed = new ArrayList<Circuit>();
        List<String> open = new ArrayList<String>();
        for (Circuit c : circuits)
            if (c.changed())
            {
                changed.add(c);
                if (mainWindow.isFileOpen(c.file.getPath()))
                    open.add(GameFolder.CIRCUIT_NAMES[c.slot - 1] + " (" + c.file.getName() + ")");
            }
        if (changed.isEmpty())
        {
            JOptionPane.showMessageDialog(this, "Nothing has been changed.", "Nothing to Save", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (!open.isEmpty())
        {
            JOptionPane.showMessageDialog(this, "Nothing was saved, because " + (open.size() == 1 ? "this circuit is" : "these circuits are")
                + " open in a tab:\n\n" + String.join("\n", open)
                + "\n\nClose " + (open.size() == 1 ? "its tab" : "their tabs") + " and save again, so the tab cannot save its own copy over these changes.",
                "Close the Circuit's Tab First", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<String> failed = new ArrayList<String>();
        for (Circuit c : changed)
        {
            CCSetup setup = c.track.getCCSetup();
            for (int i = 0; i < WORDS.length; i++)
                if (c.shown[i] != c.loaded[i])
                    setup.setPercent(WORDS[i], c.shown[i]);
            if (c.track.saveCCSetupOnly())
                System.arraycopy(c.shown, 0, c.loaded, 0, WORDS.length);
            else
                failed.add(c.file.getName());
        }
        saved = true;
        refresh();
        if (failed.isEmpty())
            JOptionPane.showMessageDialog(this, "Saved " + changed.size() + (changed.size() == 1 ? " circuit." : " circuits."),
                                          "Saved", JOptionPane.INFORMATION_MESSAGE);
        else
            JOptionPane.showMessageDialog(this, "These files could not be saved:\n\n" + String.join("\n", failed),
                                          "Not Saved", JOptionPane.ERROR_MESSAGE);
    }

    private void closeAsked()
    {
        stopEditing();
        if (hasChanges())
        {
            int choice = JOptionPane.showOptionDialog(this, "Close without saving the changes?", "Unsaved Changes",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null,
                new String[] { "Close Without Saving", "Keep Editing" }, "Keep Editing");
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
        public int getRowCount() { return circuits.size(); }
        public int getColumnCount() { return WORDS.length + 1; }
        public String getColumnName(int col) { return col == 0 ? "Circuit" : TITLES[col - 1]; }
        public Class<?> getColumnClass(int col) { return col == 0 ? String.class : Object.class; }

        public Object getValueAt(int row, int col)
        {
            Circuit c = circuits.get(row);
            if (col == 0)
                return c.slot + "  " + GameFolder.CIRCUIT_NAMES[c.slot - 1];
            if (c.track == null)
                return col == 1 ? c.problem : "";
            return String.valueOf(c.shown[col - 1]);
        }

        public boolean isCellEditable(int row, int col)
        {
            return col > 0 && circuits.get(row).track != null;
        }

        public void setValueAt(Object value, int row, int col)
        {
            try
            {
                int v = Integer.parseInt(String.valueOf(value).trim().replace("%", ""));
                circuits.get(row).shown[col - 1] = Math.max(MIN, Math.min(MAX, v));
            }
            catch (NumberFormatException e)
            {
                // not a number: the old value stays
            }
            refresh();
        }
    }
}
