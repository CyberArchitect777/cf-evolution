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
 * ExecutableWindow.java
 *
 * Created on 31 December 2006, 04:21
 */
package cfevolution.gui.executable;

import cfevolution.data.executable.*;
import cfevolution.gui.*;
import java.awt.*;
import java.io.File;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * One open game executable, shown as a tab of the main window. A short
 * list on the left picks the page; drivers, teams and points are tables
 * edited directly in their cells, and the settings are tick boxes. Every
 * change goes straight into memory and marks the tab as unsaved.
 *
 * @author  barrie
 */
public class ExecutableWindow extends JPanel implements EditorTab
{
    private static final String[] PAGES = { "Drivers", "Teams", "Points", "Game Settings" };

    private MainGUI appFrame;
    private String fileName;
    private Executable currentExecutable;
    private boolean modified;
    private CardLayout cards;
    private JPanel pageContainer;
    private JList<String> pageList;

    public ExecutableWindow(Executable selectedExecutable, String executableFileName, MainGUI origContainer)
    {
        super(new BorderLayout());
        currentExecutable = selectedExecutable;
        fileName = executableFileName;
        appFrame = origContainer;

        cards = new CardLayout();
        pageContainer = new JPanel(cards);
        pageContainer.add(driversPage(), PAGES[0]);
        pageContainer.add(teamsPage(), PAGES[1]);
        pageContainer.add(pointsPage(), PAGES[2]);
        pageContainer.add(settingsPage(), PAGES[3]);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebar(), pageContainer);
        split.setDividerLocation(220);
        split.setContinuousLayout(true);
        add(split, BorderLayout.CENTER);
        pageList.setSelectedIndex(0);
    }

    // --- Sidebar ----------------------------------------------------------

    private JComponent sidebar()
    {
        pageList = new JList<String>(PAGES);
        pageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pageList.setFixedCellHeight(30);
        pageList.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        pageList.setCellRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 8));
                return this;
            }
        });
        pageList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent e) {
                if (pageList.getSelectedValue() != null)
                    cards.show(pageContainer, pageList.getSelectedValue());
            }
        });

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xD5D9DF)),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        JLabel version = new JLabel(currentExecutable.returnGameVersionString());
        version.setFont(version.getFont().deriveFont(Font.BOLD));
        JLabel file = new JLabel(new File(fileName).getName());
        file.setForeground(new Color(0x6A7380));
        file.setToolTipText(fileName);
        info.add(new JLabel("Game version"));
        info.add(version);
        info.add(Box.createVerticalStrut(6));
        info.add(file);

        JPanel side = new JPanel(new BorderLayout());
        side.add(sectionHeading("Executable"), BorderLayout.NORTH);
        side.add(pageList, BorderLayout.CENTER);
        side.add(info, BorderLayout.SOUTH);
        side.setBackground(Color.WHITE);
        side.setMinimumSize(new Dimension(160, 100));
        return side;
    }

    private static JLabel sectionHeading(String text)
    {
        JLabel heading = new JLabel(text.toUpperCase());
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 11f));
        heading.setForeground(new Color(0x6A7380));
        heading.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xD5D9DF)),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        return heading;
    }

    /** A page: title, one line of explanation, then the content. */
    private JPanel page(String title, String explanation, JComponent content)
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

    private JTable table(AbstractTableModel model, int[] widths)
    {
        JTable table = new JTable(model);
        table.setRowHeight(24);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        table.getTableHeader().setReorderingAllowed(false);
        table.setDefaultRenderer(Object.class, new StripedRenderer());
        table.setDefaultRenderer(Integer.class, new StripedRenderer());
        for (int i = 0; i < widths.length; i++)
        {
            if (widths[i] > 0)
            {
                table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
                table.getColumnModel().getColumn(i).setMaxWidth(widths[i] * 2);
            }
        }
        model.addTableModelListener(new javax.swing.event.TableModelListener() {
            public void tableChanged(javax.swing.event.TableModelEvent e) { setModified(true); }
        });
        return table;
    }

    /** Light striping, numbers on the right, read-only cells greyed. */
    private static class StripedRenderer extends DefaultTableCellRenderer
    {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focus, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focus, row, column);
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            setHorizontalAlignment(value instanceof Number ? RIGHT : LEFT);
            if (!selected)
            {
                setBackground(row % 2 == 0 ? Color.WHITE : new Color(0xF6F7F9));
                setForeground(table.isCellEditable(row, column) ? Color.BLACK : new Color(0x7A828C));
            }
            return this;
        }
    }

    /** Parses a typed number, or returns null with a message when it is
        not a whole number inside [min, max]. */
    private Integer number(Object value, int min, int max, String what)
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

    private String name(Object value, int maxLength, String what)
    {
        String text = String.valueOf(value);
        if (text.length() > maxLength)
        {
            JOptionPane.showMessageDialog(this, what + " can be at most " + maxLength + " characters long.",
                "Value Not Changed", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return text;
    }

    // --- Drivers ----------------------------------------------------------

    private JComponent driversPage()
    {
        final GameDrivers drivers = currentExecutable.getGameDrivers();
        AbstractTableModel model = new AbstractTableModel() {
            private final String[] columns = { "No.", "Driver", "Qualifying skill", "Race skill" };
            public int getRowCount() { return 40; }
            public int getColumnCount() { return columns.length; }
            public String getColumnName(int c) { return columns[c]; }
            public Class<?> getColumnClass(int c) { return c == 1 ? String.class : Integer.class; }
            public boolean isCellEditable(int r, int c) { return c > 0; }
            public Object getValueAt(int r, int c) {
                switch (c) {
                    case 0: return r + 1;
                    case 1: return drivers.getDriverName(r);
                    // Stored as bytes; shown 0-255 rather than as negative numbers
                    case 2: return drivers.getQualifyingGrip(r) & 0xFF;
                    default: return drivers.getRaceGrip(r) & 0xFF;
                }
            }
            public void setValueAt(Object v, int r, int c) {
                if (c == 1) {
                    String text = name(v, 23, "A driver's name");
                    if (text == null || text.equals(drivers.getDriverName(r))) return;
                    drivers.setDriverName(r, text);
                } else {
                    // 0 would make the game treat a driver as better than the best
                    Integer n = number(v, 1, 255, "Skill");
                    if (n == null || n.intValue() == (Integer) getValueAt(r, c)) return;
                    if (c == 2) drivers.setQualifyingGrip(r, n); else drivers.setRaceGrip(r, n);
                }
                fireTableCellUpdated(r, c);
            }
        };
        JTable table = table(model, new int[] { 50, 0, 130, 110 });
        return page("Drivers",
            "Names and skill for all 40 car numbers. Lower skill numbers are faster drivers (1 is the best).",
            new JScrollPane(table));
    }

    // --- Teams ------------------------------------------------------------

    private JComponent teamsPage()
    {
        final GameTeams teams = currentExecutable.getGameTeams();
        AbstractTableModel model = new AbstractTableModel() {
            private final String[] columns = { "No.", "Team", "Engine", "Engine power (BHP)" };
            public int getRowCount() { return 20; }
            public int getColumnCount() { return columns.length; }
            public String getColumnName(int c) { return columns[c]; }
            public Class<?> getColumnClass(int c) { return c == 1 || c == 2 ? String.class : Integer.class; }
            public boolean isCellEditable(int r, int c) { return c > 0; }
            public Object getValueAt(int r, int c) {
                switch (c) {
                    case 0: return r + 1;
                    case 1: return teams.getTeamName(r);
                    case 2: return teams.getEngineName(r);
                    default: return teams.getBHPSetting(r);
                }
            }
            public void setValueAt(Object v, int r, int c) {
                if (c == 1 || c == 2) {
                    String text = name(v, 12, c == 1 ? "A team name" : "An engine name");
                    if (text == null || text.equals(getValueAt(r, c))) return;
                    if (c == 1) teams.setTeamName(r, text); else teams.setEngineName(r, text);
                } else {
                    Integer n = number(v, 0, 1300, "Engine power");
                    if (n == null || n.intValue() == teams.getBHPSetting(r)) return;
                    teams.setBHPSetting(r, n);
                }
                fireTableCellUpdated(r, c);
            }
        };
        JTable table = table(model, new int[] { 50, 0, 0, 150 });
        return page("Teams",
            "Team and engine names, and each team's engine power. Power differences only show with \"1991 Levels\" opposition.",
            new JScrollPane(table));
    }

    // --- Points -----------------------------------------------------------

    private static String ordinal(int n)
    {
        int lastTwo = n % 100;
        String suffix = (lastTwo >= 11 && lastTwo <= 13) ? "th"
                      : n % 10 == 1 ? "st" : n % 10 == 2 ? "nd" : n % 10 == 3 ? "rd" : "th";
        return n + suffix;
    }

    private JComponent pointsPage()
    {
        final GamePoints points = currentExecutable.getGamePoints();
        AbstractTableModel model = new AbstractTableModel() {
            public int getRowCount() { return 26; }
            public int getColumnCount() { return 2; }
            public String getColumnName(int c) { return c == 0 ? "Finishing position" : "Points"; }
            public Class<?> getColumnClass(int c) { return c == 0 ? String.class : Integer.class; }
            public boolean isCellEditable(int r, int c) { return c == 1; }
            public Object getValueAt(int r, int c) {
                return c == 0 ? ordinal(r + 1) : (Object) (points.getPointValue(r) & 0xFF);
            }
            public void setValueAt(Object v, int r, int c) {
                Integer n = number(v, 0, 127, "Points");
                if (n == null || n.intValue() == (points.getPointValue(r) & 0xFF)) return;
                points.setPointValue(r, n);
                fireTableCellUpdated(r, c);
            }
        };
        JTable table = table(model, new int[] { 160, 100 });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(300, 400));
        JPanel narrow = new JPanel(new BorderLayout());
        narrow.add(scroll, BorderLayout.WEST);
        return page("Points", "Championship points for each finishing position.", narrow);
    }

    // --- Settings ---------------------------------------------------------

    private JComponent settingsPage()
    {
        final GameSettings settings = currentExecutable.getGameSettings();
        final GameOptions options = currentExecutable.getGameOptions();

        final JCheckBox tyres = new JCheckBox("Allow unlimited qualifying tyres", settings.getQualifyingTyres());
        tyres.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                settings.setQualifyingTyres(tyres.isSelected()); setModified(true); }
        });
        final JCheckBox fade = new JCheckBox("Move through the menus faster", options.getFastFade());
        fade.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                options.setFastFade(fade.isSelected()); setModified(true); }
        });
        final JCheckBox language = new JCheckBox("Skip the language selection screen", options.getLanguageSelection());
        language.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                options.setLanguageSelection(language.isSelected()); setModified(true); }
        });

        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.add(group("Racing", tyres, null));
        box.add(Box.createVerticalStrut(12));
        box.add(group("Starting the game", fade, language));
        JLabel note = new JLabel("The two start-up settings change the game program only. They are not part of a car set.");
        note.setForeground(new Color(0x6A7380));
        note.setBorder(BorderFactory.createEmptyBorder(8, 2, 0, 0));
        note.setAlignmentX(LEFT_ALIGNMENT);
        box.add(note);
        JPanel holder = new JPanel(new BorderLayout());
        holder.add(box, BorderLayout.NORTH);
        return page("Game Settings", "Changes to how the game behaves.", holder);
    }

    private JPanel group(String title, JCheckBox first, JCheckBox second)
    {
        JPanel panel = new JPanel(new GridLayout(second == null ? 1 : 2, 1, 0, 4));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(0xD5D9DF)), title),
            BorderFactory.createEmptyBorder(4, 8, 8, 8)));
        panel.add(first);
        if (second != null)
            panel.add(second);
        panel.setAlignmentX(LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(520, panel.getPreferredSize().height));
        return panel;
    }

    // --- EditorTab ----------------------------------------------------------

    private void setModified(boolean value)
    {
        modified = value;
        if (appFrame != null)
            appFrame.tabStateChanged(this);
    }

    public JComponent getComponent() { return this; }

    public String getTabTitle() { return new File(fileName).getName(); }

    public String getFilePath() { return fileName; }

    public boolean isModified() { return modified; }

    public boolean save()
    {
        if (!currentExecutable.saveData())
        {
            JOptionPane.showMessageDialog(this, "The executable could not be saved.", "Save Failed",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        setModified(false);
        return true;
    }

    public boolean saveAs()
    {
        JOptionPane.showMessageDialog(this,
            "An executable is always saved back to the file it was opened from.\n"
          + "To keep the original, copy the file before opening it here.",
            "Save Executable", JOptionPane.INFORMATION_MESSAGE);
        return false;
    }

    public JMenu[] getMenus() { return new JMenu[0]; }

    public JComponent[] getToolbarItems() { return new JComponent[0]; }

    public JMenuItem[] getFileMenuItems() { return new JMenuItem[0]; }

    public String getStatusText()
    {
        return currentExecutable.returnGameVersionString() + "  ·  40 drivers  ·  20 teams";
    }
}
