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

import cfevolution.data.dataset.OtherEditorFiles;

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
    private static final String[] PAGES = { "Drivers", "Teams", "Points", "Liveries", "Game Settings",
                                            "Pit Lane", "Set-ups", "Lap Records", "Car Physics", "Opposition Levels" };

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
        pageContainer.add(liveriesPage(), PAGES[3]);
        pageContainer.add(settingsPage(), PAGES[4]);
        pageContainer.add(pitLanePage(), PAGES[5]);
        pageContainer.add(setupsPage(), PAGES[6]);
        pageContainer.add(lapRecordsPage(), PAGES[7]);
        pageContainer.add(physicsPage(), PAGES[8]);
        pageContainer.add(oppositionPage(), PAGES[9]);

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
        fitColumnOnDoubleClick(table);
        return table;
    }

    /** Double-clicking the border between two column headings widens or narrows the column on its left
        to fit its heading and contents, as spreadsheets do (Hrvoje, 2026-10-24). */
    static void fitColumnOnDoubleClick(final JTable table)
    {
        table.getTableHeader().addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() != 2 || !SwingUtilities.isLeftMouseButton(e))
                    return;
                javax.swing.table.JTableHeader header = table.getTableHeader();
                for (int c = 0; c < table.getColumnCount(); c++)
                {
                    Rectangle r = header.getHeaderRect(c);
                    if (Math.abs(e.getX() - (r.x + r.width)) <= 3)
                    {
                        fitColumn(table, c);
                        return;
                    }
                }
            }
        });
    }

    static void fitColumn(JTable table, int c)
    {
        javax.swing.table.TableColumn column = table.getColumnModel().getColumn(c);
        javax.swing.table.TableCellRenderer headRenderer = column.getHeaderRenderer() != null
            ? column.getHeaderRenderer() : table.getTableHeader().getDefaultRenderer();
        int width = headRenderer.getTableCellRendererComponent(table, column.getHeaderValue(), false, false, -1, c)
                                .getPreferredSize().width;
        for (int r = 0; r < table.getRowCount(); r++)
            width = Math.max(width, table.prepareRenderer(table.getCellRenderer(r, c), r, c).getPreferredSize().width);
        width += 10;
        column.setMinWidth(Math.min(column.getMinWidth(), width));
        if (column.getMaxWidth() < width)
            column.setMaxWidth(width);
        column.setPreferredWidth(width);
        column.setWidth(width);
        table.getTableHeader().setResizingColumn(column);   // the other columns give way, not this one
        table.doLayout();
        table.getTableHeader().setResizingColumn(null);
        table.getTableHeader().repaint();
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
                    // The game uses skill - 1 as a signed rank (GetDriverRank, IDA 0x4C4E), so 0 is one step
                    // better than 1. Other editors write 0, so it is accepted (owner, 2026-10-25).
                    Integer n = number(v, 0, 255, "Skill");
                    if (n == null || n.intValue() == (Integer) getValueAt(r, c)) return;
                    if (c == 2) drivers.setQualifyingGrip(r, n); else drivers.setRaceGrip(r, n);
                }
                fireTableCellUpdated(r, c);
            }
        };
        JTable table = table(model, new int[] { 50, 0, 130, 110 });
        return page("Drivers",
            "Names and skill for all 40 car numbers. Lower skill numbers are faster drivers: 1 is the game's best, and 0, which some editors use, is faster still.",
            new JScrollPane(table));
    }

    // --- Teams ------------------------------------------------------------

    /** The game races the first 18 teams only; 19 and 20 are spare slots. */
    private static final int RACED_TEAMS = 18;
    /** The field: fewer entered cars than this and a race cannot be filled. */
    private static final int MIN_ENTERED_CARS = 26;

    private String carLabel(int carNumber)
    {
        if (carNumber == 0)
            return "None";
        return carNumber + "  " + currentExecutable.getGameDrivers().getDriverName(carNumber - 1);
    }

    private JComponent teamsPage()
    {
        final GameTeams teams = currentExecutable.getGameTeams();
        final AbstractTableModel model = new AbstractTableModel() {
            private final String[] columns = { "No.", "Team", "Engine", "Engine power (BHP)", "First car", "Second car" };
            public int getRowCount() { return 20; }
            public int getColumnCount() { return columns.length; }
            public String getColumnName(int c) { return columns[c]; }
            public Class<?> getColumnClass(int c) { return c == 0 || c == 3 ? Integer.class : String.class; }
            public boolean isCellEditable(int r, int c) { return c > 0; }
            public Object getValueAt(int r, int c) {
                switch (c) {
                    case 0: return r + 1;
                    case 1: return teams.getTeamName(r);
                    case 2: return teams.getEngineName(r);
                    case 3: return teams.getBHPSetting(r);
                    default: return carLabel(teams.getTeamCar(r, c - 4));
                }
            }
            public void setValueAt(Object v, int r, int c) {
                if (c == 1 || c == 2) {
                    String text = name(v, 12, c == 1 ? "A team name" : "An engine name");
                    if (text == null || text.equals(getValueAt(r, c))) return;
                    if (c == 1) teams.setTeamName(r, text); else teams.setEngineName(r, text);
                } else if (c == 3) {
                    Integer n = number(v, 0, 1300, "Engine power");
                    if (n == null || n.intValue() == teams.getBHPSetting(r)) return;
                    teams.setBHPSetting(r, n);
                } else {
                    int car = carNumberOf(v);
                    if (car < 0 || car == teams.getTeamCar(r, c - 4)) return;
                    if (!lineUpAllowed(teams, r, c - 4, car)) return;
                    teams.setTeamCar(r, c - 4, car);
                }
                fireTableCellUpdated(r, c);
            }
        };
        JTable table = table(model, new int[] { 50, 0, 0, 140, 170, 170 });
        final JComboBox<String> cars = new JComboBox<String>();
        javax.swing.table.TableCellEditor carEditor = new DefaultCellEditor(cars) {
            public Component getTableCellEditorComponent(JTable t, Object value, boolean selected, int row, int column) {
                // Names can change on the Drivers page, so the list is rebuilt each time
                cars.removeAllItems();
                for (int n = 0; n <= 40; n++)
                    cars.addItem(carLabel(n));
                return super.getTableCellEditorComponent(t, value, selected, row, column);
            }
        };
        table.getColumnModel().getColumn(4).setCellEditor(carEditor);
        table.getColumnModel().getColumn(5).setCellEditor(carEditor);
        return page("Teams",
            "<html>Team and engine names, each team's engine power, and which cars drive for it. "
          + "Power differences only show with \"1991 Levels\" opposition.<br>"
          + "The game races the first 18 teams. A new line-up takes effect when a new championship is started.</html>",
            new JScrollPane(table));
    }

    /** The car number at the start of a line-up label, 0 for "None", -1 if unreadable. */
    private static int carNumberOf(Object value)
    {
        String text = String.valueOf(value).trim();
        if (text.equals("None") || text.length() == 0)
            return 0;
        int end = 0;
        while (end < text.length() && Character.isDigit(text.charAt(end)))
            end++;
        if (end == 0)
            return -1;
        int n = Integer.parseInt(text.substring(0, end));
        return n <= 40 ? n : -1;
    }

    /** Refuses a line-up change that puts a car in two seats or leaves too
        few cars to fill a race, saying why. */
    private boolean lineUpAllowed(GameTeams teams, int team, int slot, int car)
    {
        int entered = 0;
        for (int t = 0; t < 20; t++)
        {
            for (int s = 0; s < 2; s++)
            {
                int existing = (t == team && s == slot) ? car : teams.getTeamCar(t, s);
                if (car != 0 && existing == car && !(t == team && s == slot))
                {
                    JOptionPane.showMessageDialog(this, "Car " + car + " already drives for "
                        + teams.getTeamName(t) + ". Take it out of that team first.",
                        "Line-up Not Changed", JOptionPane.WARNING_MESSAGE);
                    return false;
                }
                if (t < RACED_TEAMS && existing != 0)
                    entered++;
            }
        }
        if (entered < MIN_ENTERED_CARS)
        {
            JOptionPane.showMessageDialog(this, "The game needs at least " + MIN_ENTERED_CARS
                + " cars in the first " + RACED_TEAMS + " teams to fill a race.",
                "Line-up Not Changed", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    // --- Liveries ---------------------------------------------------------

    /** One table of colours: a row per team or driver, a column per part. */
    private interface ColourTable
    {
        int rows();
        String rowName(int row);
        String[] parts();
        /** Which of the set's 16 colours each part is. */
        int[] slots();
        int get(int row, int part);
        void set(int row, int part, int colour);
        /** Any of the set's 16 colours, for drawing the pictures. */
        int setColour(int row, int slot);
    }

    /** The game's pictures for one tab: either one picture the user can turn, or several to step through. */
    private static class Pictures
    {
        final int first, last, start, scale;
        final int[] group;

        /** Pictures first..last show the subject turning from behind to in front. */
        Pictures(int first, int last, int start, int scale)
        {
            this.first = first; this.last = last; this.start = start; this.scale = scale; this.group = null;
        }

        /** Pictures shown one at a time, with buttons to step between them. */
        Pictures(int[] pictures)
        {
            this.first = pictures[0]; this.last = pictures[0]; this.start = 0; this.scale = 2; this.group = pictures;
        }
    }

    private static final String PREF_GAME_FOLDER = "ExecutableGameFolder";

    /** The game's pictures, or null until they are found. */
    private GameSprites sprites;
    private final java.util.List<Runnable> spriteListeners = new java.util.ArrayList<Runnable>();

    private GameSprites findSprites()
    {
        File exeFolder = new File(fileName).getAbsoluteFile().getParentFile();
        String remembered = java.util.prefs.Preferences.userRoot().node("cfevolution").get(PREF_GAME_FOLDER, "");
        return GameSprites.find(exeFolder, exeFolder == null ? null : exeFolder.getParentFile(),
                                remembered.isEmpty() ? null : new File(remembered));
    }

    private void chooseGameFolder(Component parent)
    {
        JFileChooser chooser = new JFileChooser(new File(fileName).getAbsoluteFile().getParentFile());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Choose the game folder");
        if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION)
            return;
        GameSprites found = GameSprites.find(chooser.getSelectedFile());
        if (found == null)
        {
            JOptionPane.showMessageDialog(parent,
                "That folder does not have the game's F1GPDATA.DAT and F1GPDATB.DAT files.\n"
                + "Choose the folder the game is installed in.",
                "Pictures not found", JOptionPane.WARNING_MESSAGE);
            return;
        }
        java.util.prefs.Preferences.userRoot().node("cfevolution").put(PREF_GAME_FOLDER, found.getFolder().getPath());
        sprites = found;
        for (Runnable r : spriteListeners)
            r.run();
    }

    private JComponent liveriesPage()
    {
        final GameLiveries liveries = currentExecutable.getGameLiveries();
        final GameTeams teams = currentExecutable.getGameTeams();
        final GameDrivers drivers = currentExecutable.getGameDrivers();
        sprites = findSprites();

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Cars", colourTable(liveries, new ColourTable() {
            public int rows() { return GameLiveries.TEAMS; }
            public String rowName(int r) { return teams.getTeamName(r); }
            public String[] parts() { return GameLiveries.CAR_PARTS; }
            public int[] slots() { return GameLiveries.CAR_BYTES; }
            public int get(int r, int p) { return liveries.getCarColour(r, p); }
            public void set(int r, int p, int c) { liveries.setCarColour(r, p, c); }
            public int setColour(int r, int s) { return liveries.getCarSetColour(r, s); }
        }, new Pictures(GameSprites.CAR_FIRST, GameSprites.CAR_LAST, 212, 6), CAR_FILE));
        tabs.addTab("Helmets", colourTable(liveries, new ColourTable() {
            public int rows() { return GameLiveries.DRIVERS; }
            public String rowName(int r) { return (r + 1) + "  " + drivers.getDriverName(r); }
            public String[] parts() { return GameLiveries.HELMET_PARTS; }
            public int[] slots() { return GameLiveries.HELMET_BYTES; }
            public int get(int r, int p) { return liveries.getHelmetColour(r, p); }
            public void set(int r, int p, int c) { liveries.setHelmetColour(r, p, c); }
            public int setColour(int r, int s) { return liveries.getHelmetSetColour(r, s); }
        }, new Pictures(GameSprites.HELMET_FIRST, GameSprites.HELMET_LAST, 70, 12), HELMET_FILE));
        tabs.addTab("Pit crews", colourTable(liveries, new ColourTable() {
            public int rows() { return GameLiveries.TEAMS; }
            public String rowName(int r) { return teams.getTeamName(r); }
            public String[] parts() { return GameLiveries.PIT_CREW_PARTS; }
            public int[] slots() { return GameLiveries.PIT_CREW_BYTES; }
            public int get(int r, int p) { return liveries.getPitCrewColour(r, p); }
            public void set(int r, int p, int c) { liveries.setPitCrewColour(r, p, c); }
            public int setColour(int r, int s) { return liveries.getPitCrewSetColour(r, s); }
        }, new Pictures(GameSprites.PIT_CREW), null));
        return page("Liveries",
            "Colours of each team's cars and pit crew, and each driver's helmet. Click a colour, or part of the picture, to change it.",
            tabs);
    }

    /** ArgEditor's file for one team's car colours, or one driver's helmet colours. */
    private static final String CAR_FILE = "car", HELMET_FILE = "hel";

    private JComponent colourTable(final GameLiveries liveries, final ColourTable source, final Pictures pictures,
                                   final String fileKind)
    {
        final String[] parts = source.parts();
        final AbstractTableModel model = new AbstractTableModel() {
            public int getRowCount() { return source.rows(); }
            public int getColumnCount() { return parts.length + 1; }
            public String getColumnName(int c) {
                if (c == 0) return "";
                // Two-word part names go on two lines so the columns stay narrow
                String part = parts[c - 1];
                int space = part.indexOf(' ');
                return space < 0 ? part : "<html><center>" + part.substring(0, space) + "<br>" + part.substring(space + 1);
            }
            public Class<?> getColumnClass(int c) { return c == 0 ? String.class : Integer.class; }
            public boolean isCellEditable(int r, int c) { return false; }
            public Object getValueAt(int r, int c) { return c == 0 ? source.rowName(r) : (Object) source.get(r, c - 1); }
        };
        final JTable table = table(model, new int[0]);
        // Room for the two-line part names
        table.setTableHeader(new javax.swing.table.JTableHeader(table.getColumnModel()) {
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d.height = Math.max(d.height, 40);
                return d;
            }
            // the full part name, for a column too narrow to show it (Hrvoje, 2026-10-24)
            public String getToolTipText(java.awt.event.MouseEvent e) {
                int c = columnAtPoint(e.getPoint());
                return c > 0 ? parts[c - 1] + " (double-click the border on its right to fit the column)" : null;
            }
        });
        ToolTipManager.sharedInstance().registerComponent(table.getTableHeader());
        table.getTableHeader().setReorderingAllowed(false);
        fitColumnOnDoubleClick(table);
        table.setCellSelectionEnabled(true);
        // several rows at once, so one Paste covers them (Shift/Ctrl+click)
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.getColumnModel().getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(170);
        table.getColumnModel().getColumn(0).setMinWidth(120);
        DefaultTableCellRenderer swatch = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                           boolean focus, int row, int column) {
                super.getTableCellRendererComponent(t, "", selected, focus, row, column);
                setBackground(liveries.getPaletteColour((Integer) value));
                setBorder(focus ? BorderFactory.createLineBorder(Color.BLACK, 2)
                                : BorderFactory.createMatteBorder(2, 2, 2, 2, row % 2 == 0 ? Color.WHITE : new Color(0xF6F7F9)));
                setToolTipText(parts[column - 1] + ": click to change");
                return this;
            }
        };
        for (int c = 1; c < model.getColumnCount(); c++)
        {
            table.getColumnModel().getColumn(c).setCellRenderer(swatch);
            table.getColumnModel().getColumn(c).setPreferredWidth(64);
        }

        final Runnable choose = new Runnable() {
            public void run() {
                int r = table.getSelectedRow(), c = table.getSelectedColumn();
                if (r < 0 || c < 1)
                    return;
                int colour = PaletteChooser.choose(table, source.rowName(r) + ": " + parts[c - 1],
                                                   liveries, source.get(r, c - 1));
                if (colour >= 0 && colour != source.get(r, c - 1))
                {
                    source.set(r, c - 1, colour);
                    model.fireTableCellUpdated(r, c);
                }
            }
        };
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && table.columnAtPoint(e.getPoint()) > 0)
                    choose.run();
            }
        });
        Action chooseAction = new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { choose.run(); }
        };
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
             .put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0), "chooseColour");
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
             .put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_SPACE, 0), "chooseColour");
        table.getActionMap().put("chooseColour", chooseAction);

        JPanel tab = new JPanel(new BorderLayout());
        tab.add(new JScrollPane(table), BorderLayout.CENTER);
        tab.add(picturePanel(liveries, source, pictures, table, model, choose), BorderLayout.EAST);
        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        south.add(copyPasteButtons(source, table, model, fileKind == null ? "crew" : fileKind));
        if (fileKind != null)
            south.add(colourFileButtons(source, table, model, fileKind));
        tab.add(south, BorderLayout.SOUTH);
        return tab;
    }

    /** The design last copied on each tab (car, hel, crew), as the offered parts' colours.
        Shared by every executable tab, so a design can go from one game to another. */
    private static final java.util.Map<String, int[]> copiedDesigns = new java.util.HashMap<String, int[]>();

    /**
     * Copy the selected row's design and paste it onto every selected row
     * (Hrvoje, 2026-10-24: "the copy-paste features for the designs are also
     * extremely useful"). Only the parts the page offers are copied; the bytes it
     * keeps as the game has them stay with each row. Ctrl+C / Ctrl+V do the same.
     */
    private JComponent copyPasteButtons(final ColourTable source, final JTable table, final AbstractTableModel model,
                                        final String kind)
    {
        final String what = kind.equals(CAR_FILE) ? "car colours" : kind.equals(HELMET_FILE) ? "helmet" : "pit crew colours";
        final String rowWord = kind.equals(HELMET_FILE) ? "driver" : "team";
        final JButton paste = new JButton("Paste");
        paste.setName("paste " + kind);
        paste.setToolTipText("Give every selected " + rowWord + " the copied " + what);
        paste.setEnabled(copiedDesigns.containsKey(kind));
        final JLabel copied = new JLabel(" ");
        copied.setForeground(new Color(0x6A7380));

        final Runnable copy = new Runnable() {
            public void run() {
                int r = table.getSelectedRow();
                if (r < 0)
                    return;
                int[] design = new int[source.parts().length];
                for (int p = 0; p < design.length; p++)
                    design[p] = source.get(r, p);
                copiedDesigns.put(kind, design);
                paste.setEnabled(true);
                copied.setText("Copied: " + source.rowName(r).replaceAll("^[0-9]+ +", ""));
            }
        };
        final Runnable pasteRun = new Runnable() {
            public void run() {
                int[] design = copiedDesigns.get(kind);
                int[] rows = table.getSelectedRows();
                if (design == null || rows.length == 0)
                    return;
                for (int r : rows)
                {
                    for (int p = 0; p < design.length; p++)
                        if (source.get(r, p) != design[p])
                            source.set(r, p, design[p]);
                    model.fireTableRowsUpdated(r, r);
                }
            }
        };
        JButton copyButton = new JButton("Copy");
        copyButton.setName("copy " + kind);
        copyButton.setToolTipText("Copy the selected " + rowWord + "'s " + what);
        copyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { copy.run(); }
        });
        paste.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { pasteRun.run(); }
        });
        int menuKey = java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, menuKey), "copyDesign");
        table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, menuKey), "pasteDesign");
        table.getActionMap().put("copyDesign", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { copy.run(); }
        });
        table.getActionMap().put("pasteDesign", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { pasteRun.run(); }
        });

        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        p.add(copyButton);
        p.add(paste);
        p.add(new JLabel("<html><font color='#6A7380'>the selected " + rowWord + "'s " + what
            + " onto other " + rowWord + "s (select several to paste onto them all)</font></html>"));
        p.add(copied);
        return p;
    }

    /** Import and export of the selected row's colours as ArgEditor's .car or .hel file. */
    private JComponent colourFileButtons(final ColourTable source, final JTable table, final AbstractTableModel model,
                                         final String kind)
    {
        final boolean car = kind.equals(CAR_FILE);
        final String what = car ? "car colours" : "helmet colours";
        final javax.swing.filechooser.FileNameExtensionFilter filter =
            new javax.swing.filechooser.FileNameExtensionFilter("ArgEditor " + what + " (*." + kind + ")", kind);
        JButton in = new JButton("Import ." + kind + "...");
        in.setName("import " + kind);
        in.setToolTipText("Bring in " + what + " saved by ArgEditor, for the selected " + (car ? "team" : "driver"));
        in.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                int r = Math.max(0, table.getSelectedRow());
                JFileChooser chooser = new JFileChooser(new File(fileName).getAbsoluteFile().getParentFile());
                chooser.setDialogTitle("Import " + what + " for " + source.rowName(r));
                chooser.setFileFilter(filter);
                if (chooser.showOpenDialog(table) != JFileChooser.APPROVE_OPTION)
                    return;
                int[] colours = null;
                try
                {
                    byte[] b = java.nio.file.Files.readAllBytes(chooser.getSelectedFile().toPath());
                    colours = car ? OtherEditorFiles.readCarFile(b) : OtherEditorFiles.readHelmetFile(b);
                }
                catch (java.io.IOException ex) { /* reported below */ }
                if (colours == null)
                {
                    JOptionPane.showMessageDialog(table, chooser.getSelectedFile().getName() + " is not an ArgEditor ." + kind
                        + " file, so nothing was changed.", "Import ." + kind, JOptionPane.WARNING_MESSAGE);
                    return;
                }
                // only the colours the page offers; the others stay as the game has them
                int[] slots = source.slots();
                for (int p = 0; p < slots.length; p++)
                    source.set(r, p, colours[slots[p]]);
                model.fireTableRowsUpdated(r, r);
            }
        });
        JButton out = new JButton("Export ." + kind + "...");
        out.setName("export " + kind);
        out.setToolTipText("Save the selected " + (car ? "team's " : "driver's ") + what + " for ArgEditor");
        out.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                int r = Math.max(0, table.getSelectedRow());
                JFileChooser chooser = new JFileChooser(new File(fileName).getAbsoluteFile().getParentFile());
                chooser.setDialogTitle("Export " + what + " of " + source.rowName(r));
                chooser.setFileFilter(filter);
                String base = source.rowName(r).replaceAll("^[0-9]+ +", "").replaceAll("[^A-Za-z0-9 _-]", "").trim();
                chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), (base.isEmpty() ? kind : base) + "." + kind));
                if (chooser.showSaveDialog(table) != JFileChooser.APPROVE_OPTION)
                    return;
                File f = chooser.getSelectedFile();
                if (!f.getName().toLowerCase().endsWith("." + kind))
                    f = new File(f.getParentFile(), f.getName() + "." + kind);
                int[] colours = new int[16];
                for (int s = 0; s < 16; s++)
                    colours[s] = source.setColour(r, s);
                try
                {
                    java.nio.file.Files.write(f.toPath(), car ? OtherEditorFiles.writeCarFile(colours) : OtherEditorFiles.writeHelmetFile(colours));
                }
                catch (java.io.IOException ex)
                {
                    JOptionPane.showMessageDialog(table, f.getName() + " could not be saved.", "Export ." + kind, JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        p.add(in);
        p.add(out);
        p.add(new JLabel("<html><font color='#6A7380'>for the selected " + (car ? "team" : "driver") + ", in ArgEditor's file</font></html>"));
        return p;
    }

    /**
     * The game's own pictures in the colours of the selected row. Clicking a
     * part selects its cell and opens the colour chooser, exactly as clicking
     * the cell does.
     */
    private JComponent picturePanel(final GameLiveries liveries, final ColourTable source, final Pictures pictures,
                                    final JTable table, final AbstractTableModel model, final Runnable choose)
    {
        final JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(0xD5D9DF)),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        panel.setPreferredSize(new Dimension(360, 100));

        final JLabel title = new JLabel(" ");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        final int[] slots = source.slots();
        final java.util.List<LiveryPicture> shown = new java.util.ArrayList<LiveryPicture>();

        final LiveryPicture.Colours colours = new LiveryPicture.Colours() {
            int row() { return Math.max(0, table.getSelectedRow()); }
            public Color colour(int slot) { return liveries.getPaletteColour(source.setColour(row(), slot)); }
            public String part(int slot) {
                for (int p = 0; p < slots.length; p++)
                    if (slots[p] == slot)
                        return source.parts()[p];
                return null;
            }
            public void clicked(int slot) {
                for (int p = 0; p < slots.length; p++)
                    if (slots[p] == slot)
                    {
                        table.changeSelection(row(), p + 1, false, false);
                        choose.run();
                        return;
                    }
            }
        };

        final Runnable refresh = new Runnable() {
            public void run() {
                title.setText(source.rowName(Math.max(0, table.getSelectedRow())));
                for (LiveryPicture p : shown)
                    p.repaint();
            }
        };
        table.getSelectionModel().addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent e) { refresh.run(); }
        });
        model.addTableModelListener(new javax.swing.event.TableModelListener() {
            public void tableChanged(javax.swing.event.TableModelEvent e) { refresh.run(); }
        });

        final Runnable build = new Runnable() {
            public void run() {
                panel.removeAll();
                shown.clear();
                panel.add(title, BorderLayout.NORTH);
                if (sprites == null)
                    panel.add(missingPictures(panel), BorderLayout.CENTER);
                else if (pictures.group == null)
                    panel.add(turningPicture(pictures, colours, shown), BorderLayout.CENTER);
                else
                    panel.add(steppedPicture(pictures, colours, shown), BorderLayout.CENTER);
                refresh.run();
                panel.revalidate();
                panel.repaint();
            }
        };
        spriteListeners.add(build);
        build.run();
        return panel;
    }

    private static final Color PICTURE_BACKGROUND = new Color(0x9AA3AD);

    private static JLabel pictureHint(String text)
    {
        JLabel hint = new JLabel("<html>" + text + "</html>");
        hint.setForeground(new Color(0x6A7380));
        hint.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        return hint;
    }

    /** One picture and a slider that turns it all the way round. */
    private JComponent turningPicture(final Pictures pictures, LiveryPicture.Colours colours,
                                      java.util.List<LiveryPicture> shown)
    {
        final LiveryPicture picture = new LiveryPicture(pictures.scale, colours);
        picture.setName("liveryPicture");
        shown.add(picture);
        // Behind, round one side to in front, then the game's mirror image back round the other
        final int n = pictures.last - pictures.first + 1;
        int boxW = 0, boxH = 0;
        for (int i = pictures.first; i <= pictures.last; i++)
        {
            GameSprites.Sprite s = sprites.get(i);
            if (s != null) { boxW = Math.max(boxW, s.width); boxH = Math.max(boxH, s.height); }
        }
        picture.setBox(boxW, boxH);
        final JSlider turn = new JSlider(0, 2 * n - 2, pictures.start - pictures.first);
        turn.setName("liveryTurn");
        final Runnable show = new Runnable() {
            public void run() {
                int v = turn.getValue();
                if (v < n)
                    picture.setSprite(sprites.get(pictures.first + v), false);
                else
                    picture.setSprite(sprites.get(pictures.first + 2 * n - 2 - v), true);
            }
        };
        turn.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent e) { show.run(); }
        });
        show.run();

        JPanel stage = new JPanel(new BorderLayout());
        stage.setBackground(PICTURE_BACKGROUND);
        stage.setBorder(BorderFactory.createEmptyBorder(16, 8, 16, 8));
        stage.add(picture, BorderLayout.CENTER);

        JPanel below = new JPanel(new BorderLayout());
        below.add(new JLabel("Turn"), BorderLayout.WEST);
        below.add(turn, BorderLayout.CENTER);
        below.add(pictureHint("Click part of the picture to change its colour."), BorderLayout.SOUTH);

        JPanel box = new JPanel(new BorderLayout(0, 8));
        box.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        box.add(stage, BorderLayout.NORTH);
        box.add(below, BorderLayout.CENTER);
        JPanel holder = new JPanel(new BorderLayout());
        holder.add(box, BorderLayout.NORTH);
        return holder;
    }

    /** One picture at a time, with Previous and Next buttons stepping through the rest. */
    private JComponent steppedPicture(final Pictures pictures, LiveryPicture.Colours colours,
                                      java.util.List<LiveryPicture> shown)
    {
        final LiveryPicture picture = new LiveryPicture(pictures.scale, colours);
        picture.setName("liveryPicture");
        shown.add(picture);
        final int[] index = { pictures.start };
        final JLabel count = new JLabel("", SwingConstants.CENTER);
        count.setName("liveryPictureNumber");
        final JButton previous = new JButton("\u25C0"), next = new JButton("\u25B6");
        previous.setName("liveryPrevious");
        next.setName("liveryNext");
        previous.setToolTipText("Previous picture");
        next.setToolTipText("Next picture");
        final Runnable show = new Runnable() {
            public void run() {
                picture.setSprite(sprites.get(pictures.group[index[0]]), false);
                count.setText("Picture " + (index[0] + 1) + " of " + pictures.group.length);
            }
        };
        previous.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                index[0] = (index[0] + pictures.group.length - 1) % pictures.group.length;
                show.run();
            }
        });
        next.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                index[0] = (index[0] + 1) % pictures.group.length;
                show.run();
            }
        });
        show.run();

        JPanel stage = new JPanel(new BorderLayout());
        stage.setBackground(PICTURE_BACKGROUND);
        stage.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));
        stage.add(picture, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new BorderLayout(6, 0));
        buttons.add(previous, BorderLayout.WEST);
        buttons.add(count, BorderLayout.CENTER);
        buttons.add(next, BorderLayout.EAST);
        JPanel below = new JPanel(new BorderLayout());
        below.add(buttons, BorderLayout.NORTH);
        below.add(pictureHint("Click part of the picture to change its colour."), BorderLayout.SOUTH);

        JPanel box = new JPanel(new BorderLayout(0, 8));
        box.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        box.add(stage, BorderLayout.NORTH);
        box.add(below, BorderLayout.CENTER);
        JPanel holder = new JPanel(new BorderLayout());
        holder.add(box, BorderLayout.NORTH);
        return holder;
    }

    /** Shown when the game's picture files are not beside the executable. */
    private JComponent missingPictures(final JComponent parent)
    {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        JLabel text = pictureHint("The pictures are drawn from the game's own files, which are not in this "
            + "executable's folder. Choose the folder the game is installed in to see them.");
        text.setAlignmentX(0f);
        JButton find = new JButton("Choose Game Folder...");
        find.setName("chooseGameFolder");
        find.setAlignmentX(0f);
        find.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { chooseGameFolder(parent); }
        });
        box.add(text);
        box.add(Box.createVerticalStrut(10));
        box.add(find);
        JPanel holder = new JPanel(new BorderLayout());
        holder.add(box, BorderLayout.NORTH);
        return holder;
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

    /** Reads and writes one whole-number setting. */
    private interface Setting
    {
        int get();
        void set(int value);
    }

    private JComponent settingsPage()
    {
        final GameSettings settings = currentExecutable.getGameSettings();
        final GameOptions options = currentExecutable.getGameOptions();

        final JCheckBox tyres = new JCheckBox("Allow unlimited qualifying tyres", settings.getQualifyingTyres());
        tyres.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                settings.setQualifyingTyres(tyres.isSelected()); setModified(true); }
        });
        final JCheckBox rainEverywhere = new JCheckBox("Allow rain at circuits that are always dry (Phoenix)",
                                                       settings.getRainEverywhere());
        rainEverywhere.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                settings.setRainEverywhere(rainEverywhere.isSelected()); setModified(true); }
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

        JPanel yourCar = group("Your car");
        row(yourCar, "Engine power (BHP)", 1, 1460, new Setting() {
            public int get() { return settings.getPlayerPower(); }
            public void set(int v) { settings.setPlayerPower(v); } },
            "The original game gives you 716.");
        box.add(yourCar);
        box.add(Box.createVerticalStrut(12));

        JPanel computerCars = group("Computer cars");
        if (settings.getAIGripLevel() >= 1)
            row(computerCars, "Grip level", 1, 100, new Setting() {
                public int get() { return settings.getAIGripLevel(); }
                public void set(int v) { settings.setAIGripLevel(v); } },
                "1 is the original game. Higher gives every car more grip in corners, yours too.");
        else
            note(computerCars, "The grip setting in this file was changed by another tool, so it is left as it is.");
        row(computerCars, "Team engine power measured against (BHP)", 100, 2000, new Setting() {
            public int get() { return settings.getTeamPowerReference(); }
            public void set(int v) { settings.setTeamPowerReference(v); } },
            "The original game has 716. Raising it slows every team.");
        box.add(computerCars);
        box.add(Box.createVerticalStrut(12));

        JPanel racing = group("Racing");
        row(racing, "Computer cars make pit stops in races of at least (%)", 1, 100, new Setting() {
            public int get() { return settings.getPitStopRaceDistance(); }
            public void set(int v) { settings.setPitStopRaceDistance(v); } },
            "The original game has 75. Shorter races have no pit stops.");
        row(racing, "Most cars in the pits at once", 1, 26, new Setting() {
            public int get() { return settings.getMaxCarsPitting(); }
            public void set(int v) { settings.setMaxCarsPitting(v); } },
            "The original game has 7. Team-mates never stop together.");
        row(racing, "Shortest race (laps)", 1, 50, new Setting() {
            public int get() { return settings.getMinimumRaceLaps(); }
            public void set(int v) { settings.setMinimumRaceLaps(v); } },
            "The original game has 3.");
        row(racing, "Computer cars out at once in practice and qualifying", 1, 26, new Setting() {
            public int get() { return settings.getMaxPracticeRunners(); }
            public void set(int v) { settings.setMaxPracticeRunners(v); } },
            "The original game has 16. The whole field is 26.");
        racing.add(tyres, wide());
        box.add(racing);
        box.add(Box.createVerticalStrut(12));

        JPanel championship = group("Championship");
        row(championship, "Best results that count", 0, 16, new Setting() {
            public int get() { return settings.getBestResultsCounted(); }
            public void set(int v) { settings.setBestResultsCounted(v); } },
            "The original game has 16, every race. 0 also counts every race.");
        box.add(championship);
        box.add(Box.createVerticalStrut(12));

        JPanel weather = group("Weather");
        row(weather, "Chance of rain (%)", 0, 100, new Setting() {
            public int get() { return settings.getRainChance(); }
            public void set(int v) { settings.setRainChance(v); } },
            "The original game has 6. 0 means it never rains.");
        weather.add(rainEverywhere, wide());
        box.add(weather);
        box.add(Box.createVerticalStrut(12));

        JPanel damage = group("Damage");
        note(damage, "How hard a hit has to be. Higher numbers make the cars tougher.");
        row(damage, "Retire after hitting a wall", 0, 32767, new Setting() {
            public int get() { return settings.getRetireAfterWall(); }
            public void set(int v) { settings.setRetireAfterWall(v); } }, "Original: 7424");
        row(damage, "Retire after hitting another car", 0, 32767, new Setting() {
            public int get() { return settings.getRetireAfterCar(); }
            public void set(int v) { settings.setRetireAfterCar(v); } }, "Original: 8192");
        row(damage, "Break a wing after hitting a wall", 0, 32767, new Setting() {
            public int get() { return settings.getDamageAfterWall(); }
            public void set(int v) { settings.setDamageAfterWall(v); } }, "Original: 1792");
        row(damage, "Break a wing after hitting another car", 0, 32767, new Setting() {
            public int get() { return settings.getDamageAfterCar(); }
            public void set(int v) { settings.setDamageAfterCar(v); } }, "Original: 1792");
        box.add(damage);
        box.add(Box.createVerticalStrut(12));

        JPanel marshals = group("Stopped and retired cars");
        row(marshals, "Yellow flags for a stopped car after (seconds)", 1, 60, new Setting() {
            public int get() { return settings.getYellowFlagSeconds(); }
            public void set(int v) { settings.setYellowFlagSeconds(v); } }, "Original: 20");
        row(marshals, "Retired cars removed after (seconds)", 1, 60, new Setting() {
            public int get() { return settings.getRetiredRemovedSeconds(); }
            public void set(int v) { settings.setRetiredRemovedSeconds(v); } }, "Original: 15");
        box.add(marshals);
        box.add(Box.createVerticalStrut(12));

        JPanel starting = group("Starting the game");
        starting.add(fade, wide());
        starting.add(language, wide());
        note(starting, "These two change the game program only. They are not part of a car set.");
        box.add(starting);

        JPanel holder = new JPanel(new BorderLayout());
        holder.add(box, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(holder);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return page("Game Settings", "Changes to how the game behaves.", scroll);
    }

    private static GridBagConstraints wide()
    {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridwidth = 3;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(2, 0, 2, 0);
        return c;
    }

    private JPanel group(String title)
    {
        JPanel panel = new JPanel(new GridBagLayout()) {
            public Dimension getMaximumSize() { return new Dimension(900, getPreferredSize().height); }
        };
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(0xD5D9DF)), title),
            BorderFactory.createEmptyBorder(4, 8, 8, 8)));
        panel.setAlignmentX(LEFT_ALIGNMENT);
        return panel;
    }

    private static void note(JPanel group, String text)
    {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(0x6A7380));
        group.add(label, wide());
    }

    /** A labelled number field limited to [min, max]; a change goes straight
        into memory and marks the tab unsaved. */
    private void row(JPanel group, String label, int min, int max, final Setting setting, String hint)
    {
        int value = Math.max(min, Math.min(max, setting.get()));
        final JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, min, max, 1));
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "#");
        editor.getTextField().setColumns(6);
        spinner.setEditor(editor);
        spinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent e) {
                int n = ((Number) spinner.getValue()).intValue();
                if (n != setting.get()) { setting.set(n); setModified(true); }
            }
        });
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 0, 3, 12);
        group.add(new JLabel(label), c);
        c.gridx = 1;
        group.add(spinner, c);
        c.gridx = 2;
        c.weightx = 1;
        JLabel hintLabel = new JLabel(hint);
        hintLabel.setForeground(new Color(0x6A7380));
        group.add(hintLabel, c);
    }

    // --- Number tables (set-ups, physics, opposition) ------------------------

    /** One grid of stored numbers: a row label, then a column per value. */
    private interface Cells
    {
        int get(int row, int col);
        int original(int row, int col);
        void set(int row, int col, int value);
    }

    /** Cells straight from a table, row-major, as unsigned or signed words. */
    private static Cells words(final GameTables.Table t, final int cols, final boolean signed)
    {
        return new Cells() {
            int fix(int v) { return signed ? (short) v : v; }
            public int get(int r, int c) { return fix(t.word(r * cols + c)); }
            public int original(int r, int c) { return fix(t.originalWord(r * cols + c)); }
            public void set(int r, int c, int v) { t.setWord(r * cols + c, v); }
        };
    }

    private JComponent grid(final String[] rows, final String[] cols, final int min, final int max, final Cells cells,
                            int labelWidth, int cellWidth)
    {
        final AbstractTableModel model = new AbstractTableModel() {
            public int getRowCount() { return rows.length; }
            public int getColumnCount() { return cols.length + 1; }
            public String getColumnName(int c) { return c == 0 ? "" : cols[c - 1]; }
            public Class<?> getColumnClass(int c) { return c == 0 ? String.class : Integer.class; }
            public boolean isCellEditable(int r, int c) { return c > 0; }
            public Object getValueAt(int r, int c) { return c == 0 ? rows[r] : (Object) cells.get(r, c - 1); }
            public void setValueAt(Object v, int r, int c) {
                Integer n = number(v, min, max, "This value");
                if (n == null || n.intValue() == cells.get(r, c - 1)) return;
                cells.set(r, c - 1, n);
                fireTableCellUpdated(r, c);
            }
        };
        int[] widths = new int[cols.length + 1];
        widths[0] = labelWidth;
        for (int i = 1; i < widths.length; i++) widths[i] = cellWidth;
        final JTable table = table(model, widths) ;
        table.setDefaultRenderer(Integer.class, new StripedRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean focus, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, value, sel, focus, r, c);
                int orig = cells.original(r, c - 1);
                ((JComponent) comp).setToolTipText("Original: " + orig);
                if (!sel) comp.setFont(comp.getFont().deriveFont(cells.get(r, c - 1) != orig ? Font.BOLD : Font.PLAIN));
                return comp;
            }
        });
        boolean wide = labelWidth + cols.length * cellWidth > 900;
        if (wide) table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(Math.min(900, labelWidth + cols.length * cellWidth + 4),
                                              24 * rows.length + table.getTableHeader().getPreferredSize().height + 4 + (wide ? 18 : 0)));
        scroll.setAlignmentX(LEFT_ALIGNMENT);
        scroll.setMaximumSize(new Dimension(Math.min(900, labelWidth + cols.length * cellWidth + 4), scroll.getPreferredSize().height));
        return scroll;
    }

    /** A titled section of a scrolling page of grids. */
    private static void section(JPanel box, String title, String note, JComponent content)
    {
        JLabel t = new JLabel(title);
        t.setFont(t.getFont().deriveFont(Font.BOLD));
        t.setBorder(BorderFactory.createEmptyBorder(12, 0, 2, 0));
        t.setAlignmentX(LEFT_ALIGNMENT);
        box.add(t);
        if (note != null)
        {
            JLabel n = new JLabel(note);
            n.setForeground(new Color(0x6A7380));
            n.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
            n.setAlignmentX(LEFT_ALIGNMENT);
            box.add(n);
        }
        box.add(content);
    }

    private static JComponent scrollingBox(JPanel box)
    {
        JPanel holder = new JPanel(new BorderLayout());
        holder.add(box, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(holder);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private static JPanel verticalBox()
    {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        return box;
    }

    private static final String[] CIRCUITS = { "United States", "Brazil", "San Marino", "Monaco", "Canada", "Mexico",
        "France", "Great Britain", "Germany", "Hungary", "Belgium", "Italy", "Portugal", "Spain", "Japan", "Australia" };
    private static final String[] LEVELS = { "Rookie", "Amateur", "Semi-Pro", "Pro", "Ace" };
    private static final String[] COMPOUNDS = { "A", "B", "C", "D", "Q", "W" };

    // --- Pit lane -----------------------------------------------------------

    private JComponent pitLanePage()
    {
        final GameTables.Table order = currentExecutable.getGameTables().pitOrder;
        final GameTeams teams = currentExecutable.getGameTeams();
        final AbstractTableModel model = new AbstractTableModel() {
            public int getRowCount() { return 20; }
            public int getColumnCount() { return 2; }
            public String getColumnName(int c) { return c == 0 ? "Position along the pit lane" : "Team"; }
            public boolean isCellEditable(int r, int c) { return c == 1; }
            public Object getValueAt(int r, int c) {
                if (c == 0) return (r + 1) + (r >= 18 ? "  (not used: 18 pit boxes)" : "");
                return teamLabel(teams, order.getByte(r));
            }
            public void setValueAt(Object v, int r, int c) {
                int team = Integer.parseInt(String.valueOf(v).trim().split(" ")[0]);
                int was = order.getByte(r);
                if (team == was) return;
                // Keep every team exactly once: the team chosen swaps places with this one
                for (int i = 0; i < 20; i++)
                    if (order.getByte(i) == team) order.setByte(i, was);
                order.setByte(r, team);
                fireTableDataChanged();
            }
        };
        JTable table = table(model, new int[] { 260, 260 });
        String[] choices = new String[20];
        for (int i = 0; i < 20; i++) choices[i] = teamLabel(teams, i + 1);
        table.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(new JComboBox<String>(choices)));
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(540, 520));
        JPanel narrow = new JPanel(new BorderLayout());
        narrow.add(scroll, BorderLayout.WEST);
        return page("Pit Lane", "The order of the teams' pit boxes, from the start of the pit lane. Choosing a team swaps it with the one it replaces.", narrow);
    }

    private static String teamLabel(GameTeams teams, int team)
    {
        return team + "  " + (team >= 1 && team <= 20 ? teams.getTeamName(team - 1) : "?");
    }

    // --- Set-ups ------------------------------------------------------------

    private JComponent setupsPage()
    {
        final GameTables tables = currentExecutable.getGameTables();
        final GameDrivers drivers = currentExecutable.getGameDrivers();
        String[] carRows = new String[40];
        for (int i = 0; i < 40; i++) carRows[i] = (i + 1) + "  " + drivers.getDriverName(i);
        String[] circuitRows = CIRCUITS;
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Each car", setupGrid(carRows, tables.carSetups, 0));
        tabs.addTab("Each circuit", setupGrid(circuitRows, tables.circuitSetups, 0));
        tabs.addTab("Each circuit, other session", setupGrid(circuitRows, tables.circuitSetups, 16));
        return page("Set-ups",
            "Starting car set-ups: each car's default, and each circuit's. The second circuit set is used when race and qualifying set-ups are kept separate.",
            tabs);
    }

    private JComponent setupGrid(final String[] rows, final GameTables.Table t, final int firstRow)
    {
        final String[] cols = { "Front wing", "Rear wing", "Gear 1", "Gear 2", "Gear 3", "Gear 4", "Gear 5", "Gear 6", "Tyres", "Brake balance" };
        final AbstractTableModel model = new AbstractTableModel() {
            int at(int r, int c) { return (firstRow + r) * GameTables.SETUP_SIZE + c - 1; }
            public int getRowCount() { return rows.length; }
            public int getColumnCount() { return cols.length + 1; }
            public String getColumnName(int c) { return c == 0 ? "" : cols[c - 1]; }
            public boolean isCellEditable(int r, int c) { return c > 0; }
            public Object getValueAt(int r, int c) {
                if (c == 0) return rows[r];
                int v = t.getByte(at(r, c));
                if (c == 9) return v < COMPOUNDS.length ? COMPOUNDS[v] : String.valueOf(v);
                if (c == 10) return (int) (byte) v;
                return v;
            }
            public void setValueAt(Object v, int r, int c) {
                int n;
                if (c == 9)
                {
                    n = java.util.Arrays.asList(COMPOUNDS).indexOf(String.valueOf(v).trim().toUpperCase());
                    if (n < 0)
                    {
                        JOptionPane.showMessageDialog(ExecutableWindow.this, "Tyres must be one of A, B, C, D, Q or W.",
                            "Value Not Changed", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }
                else
                {
                    Integer i = c == 10 ? number(v, -127, 127, "Brake balance") : number(v, 0, 255, cols[c - 1]);
                    if (i == null) return;
                    n = i & 0xFF;
                }
                if (n == t.getByte(at(r, c))) return;
                t.setByte(at(r, c), n);
                fireTableCellUpdated(r, c);
            }
        };
        int[] widths = new int[cols.length + 1];
        widths[0] = 170;
        for (int i = 1; i < widths.length; i++) widths[i] = 76;
        return new JScrollPane(table(model, widths));
    }

    // --- Lap records --------------------------------------------------------

    private static String lapTime(int ms)
    {
        return String.format("%d:%02d.%03d", ms / 60000, (ms / 1000) % 60, ms % 1000);
    }

    private JComponent lapRecordsPage()
    {
        final GameTables tables = currentExecutable.getGameTables();
        final String[] cols = { "Circuit", "Qualifying: driver", "Team", "Time", "Date", "Race: driver", "Team", "Time", "Date" };
        final java.time.format.DateTimeFormatter dates = java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.UK);
        final AbstractTableModel model = new AbstractTableModel() {
            public int getRowCount() { return 16; }
            public int getColumnCount() { return cols.length; }
            public String getColumnName(int c) { return cols[c]; }
            public boolean isCellEditable(int r, int c) { return c > 0; }
            public Object getValueAt(int r, int c) {
                if (c == 0) return CIRCUITS[r];
                boolean race = c >= 5;
                switch ((c - 1) % 4)
                {
                    case 0: return tables.getRecordDriver(r, race);
                    case 1: return tables.getRecordTeam(r, race);
                    case 2: return lapTime(tables.getRecordTime(r, race));
                    default: return GameTables.dateOfDay(tables.getRecordDay(r, race)).format(dates);
                }
            }
            public void setValueAt(Object v, int r, int c) {
                boolean race = c >= 5;
                String text = String.valueOf(v).trim();
                switch ((c - 1) % 4)
                {
                    case 0: {
                        String n = name(text, GameTables.NAME_LENGTH - 1, "A driver's name");
                        if (n == null || n.equals(tables.getRecordDriver(r, race))) return;
                        tables.setRecordDriver(r, race, n);
                        break;
                    }
                    case 1: {
                        String n = name(text, GameTables.TEAM_LENGTH - 1, "A team name");
                        if (n == null || n.equals(tables.getRecordTeam(r, race))) return;
                        tables.setRecordTeam(r, race, n);
                        break;
                    }
                    case 2: {
                        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+):(\\d{1,2})\\.(\\d{1,3})").matcher(text);
                        int ms = -1;
                        if (m.matches() && Integer.parseInt(m.group(2)) < 60)
                        {
                            String frac = (m.group(3) + "00").substring(0, 3);
                            ms = Integer.parseInt(m.group(1)) * 60000 + Integer.parseInt(m.group(2)) * 1000 + Integer.parseInt(frac);
                        }
                        if (ms <= 0 || ms >= 60 * 60000)
                        {
                            JOptionPane.showMessageDialog(ExecutableWindow.this,
                                "A lap time must look like 1:21.434 (minutes, seconds, thousandths).",
                                "Value Not Changed", JOptionPane.WARNING_MESSAGE);
                            return;
                        }
                        if (ms == tables.getRecordTime(r, race)) return;
                        tables.setRecordTime(r, race, ms);
                        break;
                    }
                    default: {
                        java.time.LocalDate d = null;
                        for (String pattern : new String[] { "d MMM yyyy", "d/M/yyyy", "d-M-yyyy", "yyyy-M-d" })
                            try { d = java.time.LocalDate.parse(text, java.time.format.DateTimeFormatter.ofPattern(pattern, java.util.Locale.UK)); break; }
                            catch (java.time.format.DateTimeParseException e) { /* next pattern */ }
                        int day = d == null ? -1 : GameTables.dayOfDate(d);
                        if (day < 0)
                        {
                            JOptionPane.showMessageDialog(ExecutableWindow.this,
                                "A date must look like 10 Mar 1991 or 10/3/1991, from 1978 onwards. The game has no 29 February 2000.",
                                "Value Not Changed", JOptionPane.WARNING_MESSAGE);
                            return;
                        }
                        if (day == tables.getRecordDay(r, race)) return;
                        tables.setRecordDay(r, race, day);
                    }
                }
                fireTableCellUpdated(r, c);
            }
        };
        JTable table = table(model, new int[] { 110, 150, 100, 80, 100, 150, 100, 80, 100 });
        return page("Lap Records", "The lap records the game starts with, for qualifying and the race at each circuit.",
                    new JScrollPane(table));
    }

    // --- Car physics --------------------------------------------------------

    private JComponent physicsPage()
    {
        final GameTables t = currentExecutable.getGameTables();
        JPanel box = verticalBox();
        JLabel intro = new JLabel("<html>These change how every car in the game behaves, yours included. Hover over a value to see "
            + "the original; changed values are shown in bold.</html>");
        intro.setAlignmentX(LEFT_ALIGNMENT);
        box.add(intro);

        String[] rpm = new String[33];
        for (int i = 0; i < 33; i++) rpm[i] = String.valueOf(i * 512);
        section(box, "Engine torque at each engine speed (rpm)", "The qualifying engine revs higher. Torque falls to 0 at the rev limit.",
            grid(new String[] { "Race engine", "Qualifying engine" }, rpm, 0, 65535, new Cells() {
                GameTables.Table of(int r) { return r == 0 ? t.torqueRace : t.torqueQualifying; }
                public int get(int r, int c) { return of(r).word(c); }
                public int original(int r, int c) { return of(r).originalWord(c); }
                public void set(int r, int c, int v) { of(r).setWord(c, v); }
            }, 150, 56));
        String[] grip = { "1", "2", "3", "4", "5", "6", "7", "8" };
        section(box, "Tyre grip, dry weather", "Eight values for each compound: base grip, then how it warms up, peaks and wears.",
            grid(COMPOUNDS, grip, -32768, 32767, words(t.tyreGripDry, 8, true), 150, 70));
        section(box, "Tyre grip, wet weather", null, grid(COMPOUNDS, grip, -32768, 32767, words(t.tyreGripWet, 8, true), 150, 70));
        section(box, "Lowest grip a worn tyre can fall to", null,
            grid(new String[] { "Grip floor" }, new String[] { "Dry", "Wet" }, 0, 65535, words(t.minimumGrip, 2, false), 150, 70));
        section(box, "Car weight and fuel (lb)", "The weight is stored twice and both copies change together. It cannot be 0, which crashes the game.",
            grid(new String[] { "Car weight", "Fuel" }, new String[] { "Value", "Second value" }, 0, 65535, new Cells() {
                public int get(int r, int c) { return r == 0 ? t.carWeight.word(c) : t.fuel.word(c); }
                public int original(int r, int c) { return r == 0 ? t.carWeight.originalWord(c) : t.fuel.originalWord(c); }
                public void set(int r, int c, int v) {
                    if (r == 1) { t.fuel.setWord(c, v); return; }
                    if (v == 0)
                    {
                        JOptionPane.showMessageDialog(ExecutableWindow.this, "The car weight cannot be 0: the game would crash.",
                            "Value Not Changed", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    t.carWeight.setWord(0, v);
                    t.carWeight.setWord(1, v);
                }
            }, 150, 100));
        String[] wing = new String[9];
        for (int i = 0; i < 9; i++) wing[i] = String.valueOf(i * 8);
        section(box, "Wings: drag and downforce at each wing setting", null,
            grid(new String[] { "Drag", "Downforce" }, wing, 0, 65535, new Cells() {
                GameTables.Table of(int r) { return r == 0 ? t.wingDrag : t.wingDownforce; }
                public int get(int r, int c) { return of(r).word(c); }
                public int original(int r, int c) { return of(r).originalWord(c); }
                public void set(int r, int c, int v) { of(r).setWord(c, v); }
            }, 150, 64));
        section(box, "Other car values", null,
            grid(new String[] { "Downforce factor", "Engine braking" }, new String[] { "Value" }, 0, 65535, new Cells() {
                GameTables.Table of(int r) { return r == 0 ? t.downforceFactor : t.engineBraking; }
                public int get(int r, int c) { return of(r).word(0); }
                public int original(int r, int c) { return of(r).originalWord(0); }
                public void set(int r, int c, int v) { of(r).setWord(0, v); }
            }, 150, 80));
        String[] revCols = { "Change up", "Change down", "3", "4", "5", "6", "7", "8", "9", "10" };
        section(box, "Gear changes (rpm)", "Automatic gear change points: up, down, then the automatic down-shift values.",
            grid(new String[] { "Engine speed" }, revCols, 0, 65535, words(t.revLimits, 10, false), 150, 70));
        return page("Car Physics", "The car's engine, tyres, weight and wings.", scrollingBox(box));
    }

    // --- Opposition levels --------------------------------------------------

    private JComponent oppositionPage()
    {
        final GameTables t = currentExecutable.getGameTables();
        JPanel box = verticalBox();
        JLabel intro = new JLabel("<html>How the computer cars differ between the opposition levels chosen in the game's options. "
            + "Hover over a value to see the original; changed values are shown in bold.</html>");
        intro.setAlignmentX(LEFT_ALIGNMENT);
        box.add(intro);
        final GameTables.Table[] rowsTables = { t.oppPowerDeduction, t.oppRankPower, t.oppOversteerLow, t.oppCornerBurst,
                                                t.oppVariationLow, t.oppVariationHigh, t.oppWingVariation };
        String[] rows = { "Random power loss", "Power lost per skill step", "Handling variation, lowest",
                          "Corner speed burst threshold", "Driver variation, lowest", "Driver variation, highest",
                          "Wing setting spread" };
        section(box, "At each level", "At Ace a driver's skill costs no power at all.",
            grid(rows, LEVELS, 0, 65535, new Cells() {
                public int get(int r, int c) { return rowsTables[r].word(c); }
                public int original(int r, int c) { return rowsTables[r].originalWord(c); }
                public void set(int r, int c, int v) { rowsTables[r].setWord(c, v); }
            }, 220, 90));
        final GameTables.Table[] singles = { t.oppOversteerHigh, t.globalCornerSpeed, t.wetRacePower };
        section(box, "At every level", null,
            grid(new String[] { "Handling variation, highest", "Corner speed from skill", "Computer car power in the wet" },
                 new String[] { "Value" }, 0, 65535, new Cells() {
                public int get(int r, int c) { return singles[r].word(0); }
                public int original(int r, int c) { return singles[r].originalWord(0); }
                public void set(int r, int c, int v) { singles[r].setWord(0, v); }
            }, 220, 90));
        return page("Opposition Levels", "The computer cars at Rookie, Amateur, Semi-Pro, Pro and Ace.", scrollingBox(box));
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
