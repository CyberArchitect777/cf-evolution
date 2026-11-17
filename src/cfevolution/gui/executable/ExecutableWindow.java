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
import cfevolution.data.gamefiles.SetupFile;
import cfevolution.edit.GameSettingsPage;
import cfevolution.edit.LapRecordsPage;
import cfevolution.edit.LiveriesPage;
import cfevolution.edit.SetupsPage;
import cfevolution.edit.TablePage;
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
                                            "Pit Lane", "Setups", "Lap Records", "Car Physics", "Opposition Levels" };

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
        if (table.getAutoResizeMode() == JTable.AUTO_RESIZE_OFF)
        {
            // A table scrolling sideways just grows or shrinks; the trick below
            // would size the column back to the table's old width
            table.revalidate();
        }
        else
        {
            table.getTableHeader().setResizingColumn(column);   // the other columns give way, not this one
            table.doLayout();
            table.getTableHeader().setResizingColumn(null);
        }
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
                cfevolution.edit.Message problem = c == 1
                    ? cfevolution.edit.ExecutableEdits.setDriverName(drivers, r, String.valueOf(v))
                    : cfevolution.edit.ExecutableEdits.setSkill(drivers, r, c == 3, String.valueOf(v));
                if (problem != null) { cfevolution.gui.MessageBoxes.show(ExecutableWindow.this, problem); return; }
                fireTableCellUpdated(r, c);
            }
        };
        JTable table = table(model, new int[] { 50, 0, 130, 110 });
        nameModels.add(model);
        return page("Drivers",
            "Names and skill for all 40 car numbers. Lower skill numbers are faster drivers: 1 is the game's best, and 0, which some editors use, is faster still.",
            withNameFileButtons(new JScrollPane(table), "drivers"));
    }

    // --- Teams ------------------------------------------------------------

    // The line-up rules (18 raced teams, at least 26 cars) are in cfevolution.edit.ExecutableEdits.

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
                cfevolution.edit.Message problem;
                if (c == 1)
                    problem = cfevolution.edit.ExecutableEdits.setTeamName(teams, r, String.valueOf(v));
                else if (c == 2)
                    problem = cfevolution.edit.ExecutableEdits.setEngineName(teams, r, String.valueOf(v));
                else if (c == 3)
                    problem = cfevolution.edit.ExecutableEdits.setPower(teams, r, String.valueOf(v));
                else
                    problem = cfevolution.edit.ExecutableEdits.setTeamCar(teams, r, c - 4, carNumberOf(v));
                if (problem != null) { cfevolution.gui.MessageBoxes.show(ExecutableWindow.this, problem); return; }
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
        nameModels.add(model);
        return page("Teams",
            "<html>Team and engine names, each team's engine power, and which cars drive for it. "
          + "Power differences only show with \"1991 Levels\" opposition.<br>"
          + "The game races the first 18 teams. A new line-up takes effect when a new championship is started.</html>",
            withNameFileButtons(new JScrollPane(table), "teams"));
    }

    // --- Names from and to a name file (Hrvoje, 2026-11-05) -----------------

    /** The Drivers and Teams tables, refreshed when names come in from a name file. */
    private final java.util.List<AbstractTableModel> nameModels = new java.util.ArrayList<AbstractTableModel>();

    /** A page's table with the name file buttons beneath it. */
    private JComponent withNameFileButtons(JComponent table, String page)
    {
        JButton load = new JButton("Load Names from a Name File...");
        load.setName("load-names " + page);
        load.setToolTipText("Replace the driver, team and engine names here with those in a name file (.NAM)");
        load.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { loadNamesFromFile(); }
        });
        JButton save = new JButton("Save Names as a Name File...");
        save.setName("save-names " + page);
        save.setToolTipText("Write the driver, team and engine names shown here to a name file the game can load");
        save.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { saveNamesToFile(); }
        });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttons.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        buttons.add(load);
        buttons.add(Box.createHorizontalStrut(8));
        buttons.add(save);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(table, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    /** Where name files are looked for first: the game's GPSAVES folder, or the executable's own. */
    private File nameFileFolder()
    {
        File folder = new File(fileName).getAbsoluteFile().getParentFile();
        String[] names = folder == null ? null : folder.list();
        if (names != null)
            for (String n : names)
                if (n.equalsIgnoreCase("GPSAVES") && new File(folder, n).isDirectory())
                    return new File(folder, n);
        return folder;
    }

    private static final javax.swing.filechooser.FileNameExtensionFilter NAME_FILES =
        new javax.swing.filechooser.FileNameExtensionFilter("Name files (*.NAM)", "nam", "NAM");

    /** Asks for a name file and which names to take from it, then puts them into this executable. */
    private void loadNamesFromFile()
    {
        JFileChooser chooser = new JFileChooser(nameFileFolder());
        chooser.setDialogTitle("Load Names from a Name File");
        chooser.setFileFilter(NAME_FILES);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        File f = chooser.getSelectedFile();
        cfevolution.data.gamefiles.NameFile names;
        try
        {
            names = cfevolution.data.gamefiles.NameFile.load(f);
        }
        catch (java.io.IOException ex)
        {
            JOptionPane.showMessageDialog(this, f.getName() + " is not a name file the game can read, or it is damaged.",
                "Names Not Loaded", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JCheckBox drivers = new JCheckBox("Driver names", true), teams = new JCheckBox("Team names", true),
                  engines = new JCheckBox("Engine names", true);
        drivers.setName("load drivers");
        teams.setName("load teams");
        engines.setName("load engines");
        JPanel ask = new JPanel();
        ask.setLayout(new BoxLayout(ask, BoxLayout.Y_AXIS));
        ask.add(new JLabel("Which names should come from " + f.getName() + "?"));
        ask.add(Box.createVerticalStrut(6));
        ask.add(drivers);
        ask.add(teams);
        ask.add(engines);
        ask.add(Box.createVerticalStrut(6));
        ask.add(new JLabel("<html><font color='#6A7380'>They replace the names here. Save GP.EXE to keep them.</font></html>"));
        if (JOptionPane.showOptionDialog(this, ask, "Load Names", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, new String[] { "Load", "Cancel" }, "Load") != 0)
            return;
        if (!drivers.isSelected() && !teams.isSelected() && !engines.isSelected())
            return;
        GameDrivers gameDrivers = currentExecutable.getGameDrivers();
        GameTeams gameTeams = currentExecutable.getGameTeams();
        if (drivers.isSelected())
            for (int i = 0; i < cfevolution.data.gamefiles.NameFile.DRIVERS; i++)
                gameDrivers.setDriverName(i, names.getDriver(i));
        for (int i = 0; i < cfevolution.data.gamefiles.NameFile.TEAMS; i++)
        {
            if (teams.isSelected())
                gameTeams.setTeamName(i, names.getTeam(i));
            if (engines.isSelected())
                gameTeams.setEngineName(i, names.getEngine(i));
        }
        for (AbstractTableModel m : nameModels)
            m.fireTableDataChanged();       // also marks the tab as changed
        repaint();                          // the Liveries page names its rows by team and driver
    }

    /** Writes the names shown here (saved or not) to a name file. */
    private void saveNamesToFile()
    {
        JFileChooser chooser = new JFileChooser(nameFileFolder());
        chooser.setDialogTitle("Save Names as a Name File");
        chooser.setFileFilter(NAME_FILES);
        chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), "NAMES.NAM"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        File f = chooser.getSelectedFile();
        String base = f.getName().replaceAll("(?i)\\.nam$", "");
        if (!base.matches("[A-Za-z0-9_-]{1,8}"))
        {
            JOptionPane.showMessageDialog(this, "The game only lists files whose names are 1 to 8 letters, digits, - or _. "
                + "Choose a shorter name.", "Name File Not Saved", JOptionPane.WARNING_MESSAGE);
            return;
        }
        f = new File(f.getParentFile(), base.toUpperCase() + ".NAM");
        if (f.exists() && JOptionPane.showOptionDialog(this, f.getName() + " already exists. Replace it?",
                "Replace File", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null,
                new String[] { "Replace", "Cancel" }, "Cancel") != 0)
            return;
        cfevolution.data.gamefiles.NameFile names = new cfevolution.data.gamefiles.NameFile();
        for (int i = 0; i < cfevolution.data.gamefiles.NameFile.DRIVERS; i++)
            names.setDriver(i, currentExecutable.getGameDrivers().getDriverName(i));
        for (int i = 0; i < cfevolution.data.gamefiles.NameFile.TEAMS; i++)
        {
            names.setTeam(i, currentExecutable.getGameTeams().getTeamName(i));
            names.setEngine(i, currentExecutable.getGameTeams().getEngineName(i));
        }
        try
        {
            names.save(f);
        }
        catch (java.io.IOException ex)
        {
            JOptionPane.showMessageDialog(this, f.getName() + " could not be saved.", "Name File Not Saved",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /** The car number at the start of a line-up label, 0 for "None", -1 if unreadable. */
    private static int carNumberOf(Object value)
    {
        return cfevolution.edit.ExecutableEdits.carNumber(String.valueOf(value));
    }

    // --- Liveries (the page's rules and words are edit/LiveriesPage) ------

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
            cfevolution.gui.MessageBoxes.show(parent, LiveriesPage.PICTURES_NOT_FOUND);
            return;
        }
        java.util.prefs.Preferences.userRoot().node("cfevolution").put(PREF_GAME_FOLDER, found.getFolder().getPath());
        sprites = found;
        for (Runnable r : spriteListeners)
            r.run();
    }

    private JComponent liveriesPage()
    {
        LiveriesPage page = new LiveriesPage(currentExecutable.getGameLiveries(), currentExecutable.getGameTeams(),
                                             currentExecutable.getGameDrivers());
        sprites = findSprites();
        JTabbedPane tabs = new JTabbedPane();
        for (int tab = 0; tab < LiveriesPage.TAB_TITLES.length; tab++)
            tabs.addTab(LiveriesPage.TAB_TITLES[tab], colourTable(page, tab));
        return page(LiveriesPage.TITLE, LiveriesPage.DESCRIPTION, tabs);
    }

    private JComponent colourTable(final LiveriesPage page, final int tab)
    {
        final GameLiveries liveries = page.liveries();
        final String[] parts = LiveriesPage.parts(tab);
        final AbstractTableModel model = new AbstractTableModel() {
            public int getRowCount() { return page.rows(tab); }
            public int getColumnCount() { return parts.length + 1; }
            public String getColumnName(int c) {
                if (c == 0) return "";
                // A word to a line so the columns stay narrow: on two lines
                // "Wing endplates", "Sidepods" and the three "Engine cover"
                // columns were cut off at 1400 px (2026-10-26)
                return "<html><center>" + parts[c - 1].replace(" ", "<br>");
            }
            public Class<?> getColumnClass(int c) { return c == 0 ? String.class : Integer.class; }
            public boolean isCellEditable(int r, int c) { return false; }
            public Object getValueAt(int r, int c) { return c == 0 ? page.rowName(tab, r) : (Object) page.get(tab, r, c - 1); }
        };
        final JTable table = table(model, new int[0]);
        // Room for part names of up to three lines
        table.setTableHeader(new javax.swing.table.JTableHeader(table.getColumnModel()) {
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d.height = Math.max(d.height, getFontMetrics(getFont()).getHeight() * 3 + 8);
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
        table.getColumnModel().getColumn(0).setPreferredWidth(130);
        table.getColumnModel().getColumn(0).setMinWidth(110);
        DefaultTableCellRenderer swatch = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                           boolean focus, int row, int column) {
                super.getTableCellRendererComponent(t, "", selected, focus, row, column);
                setBackground(new java.awt.Color(liveries.getPaletteRgb((Integer) value)));
                setBorder(focus ? BorderFactory.createLineBorder(Color.BLACK, 2)
                                : BorderFactory.createMatteBorder(2, 2, 2, 2, row % 2 == 0 ? Color.WHITE : new Color(0xF6F7F9)));
                setToolTipText(parts[column - 1] + ": click to change");
                return this;
            }
        };
        // Each colour column keeps room for the longest word of its name. At
        // 1400 px the table squeezed them to 48 px, cutting off "endplates",
        // "Cockpit" and "Sidepods" (2026-11-01); now it scrolls sideways instead.
        javax.swing.table.TableCellRenderer headRenderer = table.getTableHeader().getDefaultRenderer();
        for (int c = 1; c < model.getColumnCount(); c++)
        {
            javax.swing.table.TableColumn column = table.getColumnModel().getColumn(c);
            column.setCellRenderer(swatch);
            int heading = headRenderer.getTableCellRendererComponent(table, column.getHeaderValue(),
                                                                     false, false, -1, c).getPreferredSize().width;
            column.setMinWidth(Math.max(40, heading + 6));
            column.setPreferredWidth(Math.max(64, heading + 6));
        }

        final Runnable choose = new Runnable() {
            public void run() {
                int r = table.getSelectedRow(), c = table.getSelectedColumn();
                if (r < 0 || c < 1)
                    return;
                int colour = PaletteChooser.choose(table, page.chooserTitle(tab, r, c - 1), liveries, page.get(tab, r, c - 1));
                if (page.set(tab, r, c - 1, colour))
                    model.fireTableCellUpdated(r, c);
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

        JPanel panel = new JPanel(new BorderLayout());
        final JScrollPane tableScroll = new JScrollPane(table);
        // Fill the width when the columns fit; scroll sideways when they do not
        tableScroll.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
                int needed = 0;
                for (int c = 0; c < table.getColumnCount(); c++)
                    needed += table.getColumnModel().getColumn(c).getMinWidth();
                int mode = tableScroll.getViewport().getWidth() < needed
                    ? JTable.AUTO_RESIZE_OFF : JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS;
                if (table.getAutoResizeMode() != mode)
                    table.setAutoResizeMode(mode);
            }
        });
        panel.add(tableScroll, BorderLayout.CENTER);
        panel.add(picturePanel(page, tab, table, model, choose), BorderLayout.EAST);
        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        south.add(copyPasteButtons(page, tab, table, model));
        if (LiveriesPage.hasFile(tab))
            south.add(colourFileButtons(page, tab, table, model));
        panel.add(south, BorderLayout.SOUTH);
        return panel;
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
    private JComponent copyPasteButtons(final LiveriesPage page, final int tab, final JTable table, final AbstractTableModel model)
    {
        final String kind = LiveriesPage.kind(tab);
        final JButton paste = new JButton("Paste");
        paste.setName("paste " + kind);
        paste.setToolTipText(LiveriesPage.pasteTip(tab));
        paste.setEnabled(copiedDesigns.containsKey(kind));
        final JLabel copied = new JLabel(" ");
        copied.setForeground(new Color(0x6A7380));
        final Runnable copy = new Runnable() {
            public void run() {
                int r = table.getSelectedRow();
                if (r < 0)
                    return;
                copiedDesigns.put(kind, page.design(tab, r));
                paste.setEnabled(true);
                copied.setText(page.copiedText(tab, r));
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
                    page.paste(tab, new int[] { r }, design);
                    model.fireTableRowsUpdated(r, r);
                }
            }
        };
        JButton copyButton = new JButton("Copy");
        copyButton.setName("copy " + kind);
        copyButton.setToolTipText(LiveriesPage.copyTip(tab));
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
        p.add(new JLabel("<html><font color='#6A7380'>" + LiveriesPage.copyPasteNote(tab) + "</font></html>"));
        p.add(copied);
        return p;
    }

    /** Import and export of the selected row's colours as ArgEditor's .car or .hel file. */
    private JComponent colourFileButtons(final LiveriesPage page, final int tab, final JTable table, final AbstractTableModel model)
    {
        final String kind = LiveriesPage.kind(tab);
        final javax.swing.filechooser.FileNameExtensionFilter filter =
            new javax.swing.filechooser.FileNameExtensionFilter(LiveriesPage.fileFilter(tab), kind);
        JButton in = new JButton(LiveriesPage.importLabel(tab));
        in.setName("import " + kind);
        in.setToolTipText(LiveriesPage.importTip(tab));
        in.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                int r = Math.max(0, table.getSelectedRow());
                JFileChooser chooser = new JFileChooser(new File(fileName).getAbsoluteFile().getParentFile());
                chooser.setDialogTitle(page.importTitle(tab, r));
                chooser.setFileFilter(filter);
                if (chooser.showOpenDialog(table) != JFileChooser.APPROVE_OPTION)
                    return;
                byte[] b = null;
                try
                {
                    b = java.nio.file.Files.readAllBytes(chooser.getSelectedFile().toPath());
                }
                catch (java.io.IOException ex) { /* reported below */ }
                cfevolution.edit.Message refused = page.importFile(tab, r, chooser.getSelectedFile().getName(), b);
                if (refused != null)
                {
                    cfevolution.gui.MessageBoxes.show(table, refused);
                    return;
                }
                model.fireTableRowsUpdated(r, r);
            }
        });
        JButton out = new JButton(LiveriesPage.exportLabel(tab));
        out.setName("export " + kind);
        out.setToolTipText(LiveriesPage.exportTip(tab));
        out.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                int r = Math.max(0, table.getSelectedRow());
                JFileChooser chooser = new JFileChooser(new File(fileName).getAbsoluteFile().getParentFile());
                chooser.setDialogTitle(page.exportTitle(tab, r));
                chooser.setFileFilter(filter);
                chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), page.exportName(tab, r)));
                if (chooser.showSaveDialog(table) != JFileChooser.APPROVE_OPTION)
                    return;
                File f = chooser.getSelectedFile();
                if (!f.getName().toLowerCase().endsWith("." + kind))
                    f = new File(f.getParentFile(), f.getName() + "." + kind);
                try
                {
                    java.nio.file.Files.write(f.toPath(), page.exportFile(tab, r));
                }
                catch (java.io.IOException ex)
                {
                    cfevolution.gui.MessageBoxes.show(table, LiveriesPage.notSaved(tab, f.getName()));
                }
            }
        });
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        p.add(in);
        p.add(out);
        p.add(new JLabel("<html><font color='#6A7380'>" + LiveriesPage.fileNote(tab) + "</font></html>"));
        return p;
    }

    /**
     * The game's own pictures in the colours of the selected row. Clicking a
     * part selects its cell and opens the colour chooser, exactly as clicking
     * the cell does.
     */
    private JComponent picturePanel(final LiveriesPage page, final int tab, final JTable table,
                                    final AbstractTableModel model, final Runnable choose)
    {
        final JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(0xD5D9DF)),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        panel.setPreferredSize(new Dimension(360, 100));

        final JLabel title = new JLabel(" ");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        final java.util.List<LiveryPicture> shown = new java.util.ArrayList<LiveryPicture>();

        final LiveryPicture.Colours colours = new LiveryPicture.Colours() {
            int row() { return Math.max(0, table.getSelectedRow()); }
            public Color colour(int slot) { return new java.awt.Color(page.liveries().getPaletteRgb(page.setColour(tab, row(), slot))); }
            public String part(int slot) {
                int p = LiveriesPage.partOfSlot(tab, slot);
                return p < 0 ? null : LiveriesPage.parts(tab)[p];
            }
            public void clicked(int slot) {
                int p = LiveriesPage.partOfSlot(tab, slot);
                if (p < 0)
                    return;
                table.changeSelection(row(), p + 1, false, false);
                choose.run();
            }
        };

        final Runnable refresh = new Runnable() {
            public void run() {
                title.setText(page.rowName(tab, Math.max(0, table.getSelectedRow())));
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
                else if (LiveriesPage.turns(tab))
                    panel.add(turningPicture(tab, colours, shown), BorderLayout.CENTER);
                else
                    panel.add(steppedPicture(tab, colours, shown), BorderLayout.CENTER);
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
    private JComponent turningPicture(final int tab, LiveryPicture.Colours colours, java.util.List<LiveryPicture> shown)
    {
        final LiveryPicture picture = new LiveryPicture(LiveriesPage.scale(tab), colours);
        picture.setName("liveryPicture");
        shown.add(picture);
        int[] box = LiveriesPage.box(tab, sprites);
        picture.setBox(box[0], box[1]);
        final JSlider turn = new JSlider(0, LiveriesPage.positions(tab) - 1, LiveriesPage.startPosition(tab));
        turn.setName("liveryTurn");
        final Runnable show = new Runnable() {
            public void run() {
                int v = turn.getValue();
                picture.setSprite(sprites.get(LiveriesPage.pictureAt(tab, v)), LiveriesPage.mirroredAt(tab, v));
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
        below.add(new JLabel(LiveriesPage.TURN), BorderLayout.WEST);
        below.add(turn, BorderLayout.CENTER);
        below.add(pictureHint(LiveriesPage.PICTURE_HINT), BorderLayout.SOUTH);

        JPanel holder = new JPanel(new BorderLayout());
        holder.add(pictureBox(stage, below), BorderLayout.NORTH);
        return holder;
    }

    private static JPanel pictureBox(JComponent stage, JComponent below)
    {
        JPanel box = new JPanel(new BorderLayout(0, 8));
        box.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        box.add(stage, BorderLayout.NORTH);
        box.add(below, BorderLayout.CENTER);
        return box;
    }

    /** One picture at a time, with Previous and Next buttons stepping through the rest. */
    private JComponent steppedPicture(final int tab, LiveryPicture.Colours colours, java.util.List<LiveryPicture> shown)
    {
        final LiveryPicture picture = new LiveryPicture(LiveriesPage.scale(tab), colours);
        picture.setName("liveryPicture");
        shown.add(picture);
        // Room for the largest picture, so the buttons below stay put whichever is shown (Hrvoje, 2026-11-05:
        // on the pit crew tab they jumped up and down with each picture's height)
        int[] box = LiveriesPage.box(tab, sprites);
        picture.setBox(box[0], box[1]);
        final int count = LiveriesPage.positions(tab);
        final int[] index = { LiveriesPage.startPosition(tab) };
        final JLabel number = new JLabel("", SwingConstants.CENTER);
        number.setName("liveryPictureNumber");
        final JButton previous = new JButton("◀"), next = new JButton("▶");
        previous.setName("liveryPrevious");
        next.setName("liveryNext");
        previous.setToolTipText("Previous picture");
        next.setToolTipText("Next picture");
        final Runnable show = new Runnable() {
            public void run() {
                picture.setSprite(sprites.get(LiveriesPage.pictureAt(tab, index[0])), false);
                number.setText(LiveriesPage.pictureNumber(tab, index[0]));
            }
        };
        previous.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                index[0] = (index[0] + count - 1) % count;
                show.run();
            }
        });
        next.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                index[0] = (index[0] + 1) % count;
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
        buttons.add(number, BorderLayout.CENTER);
        buttons.add(next, BorderLayout.EAST);
        JPanel below = new JPanel(new BorderLayout());
        below.add(buttons, BorderLayout.NORTH);
        below.add(pictureHint(LiveriesPage.PICTURE_HINT), BorderLayout.SOUTH);

        JPanel holder = new JPanel(new BorderLayout());
        holder.add(pictureBox(stage, below), BorderLayout.NORTH);
        return holder;
    }

    /** Shown when the game's picture files are not beside the executable. */
    private JComponent missingPictures(final JComponent parent)
    {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        JLabel text = pictureHint(LiveriesPage.MISSING_PICTURES);
        text.setAlignmentX(0f);
        JButton find = new JButton(LiveriesPage.CHOOSE_GAME_FOLDER);
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
                cfevolution.edit.Message problem = cfevolution.edit.ExecutableEdits.setPoints(points, r, String.valueOf(v));
                if (problem != null) { cfevolution.gui.MessageBoxes.show(ExecutableWindow.this, problem); return; }
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
        // the groups, numbers, switches and notes are the core's (GameSettingsPage), shared with Android
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        java.util.List<GameSettingsPage.Group> groups = GameSettingsPage.groups(currentExecutable);
        for (int g = 0; g < groups.size(); g++)
        {
            GameSettingsPage.Group group = groups.get(g);
            JPanel panel = group(group.title);
            for (final GameSettingsPage.Item item : group.items)
            {
                if (item.kind == GameSettingsPage.Kind.NUMBER)
                    row(panel, item.label, item.min, item.max, new Setting() {
                        public int get() { return item.get(); }
                        public void set(int v) { item.set(v); } },
                        item.hint);
                else if (item.kind == GameSettingsPage.Kind.SWITCH)
                {
                    final JCheckBox check = new JCheckBox(item.label, item.isOn());
                    check.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent e) {
                            item.set(check.isSelected() ? 1 : 0); setModified(true); }
                    });
                    panel.add(check, wide());
                }
                else
                    note(panel, item.label);
            }
            box.add(panel);
            if (g < groups.size() - 1)
                box.add(Box.createVerticalStrut(12));
        }

        JPanel holder = new JPanel(new BorderLayout());
        holder.add(box, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(holder);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return page(GameSettingsPage.TITLE, GameSettingsPage.DESCRIPTION, scroll);
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

    // --- Number tables (setups, physics, opposition) ------------------------

    /** One of the core's grids (TablePage): a row label, then a column per value. */
    private JComponent grid(final TablePage.Grid g, int labelWidth, int cellWidth)
    {
        final String[] rows = g.rows, cols = g.cols;
        final AbstractTableModel model = new AbstractTableModel() {
            public int getRowCount() { return rows.length; }
            public int getColumnCount() { return cols.length + 1; }
            public String getColumnName(int c) { return c == 0 ? "" : cols[c - 1]; }
            public Class<?> getColumnClass(int c) { return c == 0 ? String.class : Integer.class; }
            public boolean isCellEditable(int r, int c) { return c > 0; }
            public Object getValueAt(int r, int c) { return c == 0 ? rows[r] : (Object) g.get(r, c - 1); }
            public void setValueAt(Object v, int r, int c) {
                cfevolution.edit.Message m = g.check(String.valueOf(v));
                if (m != null) { cfevolution.gui.MessageBoxes.show(ExecutableWindow.this, m); return; }
                int n = Integer.parseInt(String.valueOf(v).trim());
                if (n == g.get(r, c - 1)) return;
                m = g.put(r, c - 1, n);
                if (m != null) cfevolution.gui.MessageBoxes.show(ExecutableWindow.this, m);
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
                int orig = g.original(r, c - 1);
                ((JComponent) comp).setToolTipText("Original: " + orig);
                if (!sel) comp.setFont(comp.getFont().deriveFont(g.get(r, c - 1) != orig ? Font.BOLD : Font.PLAIN));
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

    /** A page of the core's grids (TablePage), each section's grid with its label and cell widths. */
    private JComponent tablePage(TablePage p, int[][] widths)
    {
        JPanel box = verticalBox();
        JLabel intro = new JLabel("<html>" + p.intro + " " + TablePage.DESKTOP_HINT + "</html>");
        intro.setAlignmentX(LEFT_ALIGNMENT);
        box.add(intro);
        for (int i = 0; i < p.sections.size(); i++)
        {
            TablePage.Section sec = p.sections.get(i);
            section(box, sec.title, sec.note, grid(sec.grid, widths[i][0], widths[i][1]));
        }
        return page(p.title, p.description, scrollingBox(box));
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

    // --- Pit lane -----------------------------------------------------------

    private JComponent pitLanePage()
    {
        // The game's 18 pit boxes (GameTables.pitOrder); the table's two spare entries are not shown
        // and are saved as they were. Every change here swaps two boxes, so an order that holds each
        // team once keeps doing so (F1GP-SDL specification, 2026-11-15).
        final GameTables tables = currentExecutable.getGameTables();
        final GameTeams teams = currentExecutable.getGameTeams();
        final int boxes = GameTables.PIT_BOXES;
        final AbstractTableModel model = new AbstractTableModel() {
            public int getRowCount() { return boxes; }
            public int getColumnCount() { return 2; }
            public String getColumnName(int c) { return c == 0 ? "Pit box" : "Team"; }
            public boolean isCellEditable(int r, int c) { return c == 1; }
            public Object getValueAt(int r, int c) {
                if (c == 0) return "Pit box " + (r + 1) + (r == 0 ? "  (pit lane entrance)" : "");
                return teamLabel(teams, tables.getPitBoxTeam(r));
            }
            public void setValueAt(Object v, int r, int c) {
                int team;
                try { team = Integer.parseInt(String.valueOf(v).trim().split(" ")[0]); }
                catch (NumberFormatException e) { return; }
                if (team < 1 || team > boxes || team == tables.getPitBoxTeam(r)) return;
                tables.setPitBoxTeam(r, team);
                fireTableDataChanged();
            }
        };
        final JTable table = table(model, new int[] { 230, 260 });
        table.setName("pit lane order");
        // The choices are rebuilt each time, so a team renamed on the Teams page shows its new name
        final JComboBox<String> chooser = new JComboBox<String>();
        table.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(chooser) {
            public Component getTableCellEditorComponent(JTable t, Object value, boolean selected, int row, int column) {
                chooser.removeAllItems();
                for (int i = 1; i <= boxes; i++) chooser.addItem(teamLabel(teams, i));
                return super.getTableCellEditorComponent(t, value, selected, row, column);
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(500, 24 * boxes + 30));

        final JLabel warning = new JLabel();
        warning.setName("pit lane warning");
        warning.setForeground(new Color(0xB03A2E));
        warning.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        final Runnable showWarning = new Runnable() {
            public void run() {
                boolean ok = tables.isPitOrderValid();
                warning.setText(ok ? "" : "<html>This order has a team missing or listed twice, so the game would not give every team "
                                          + "its own box.<br>Choose a team for each box, or use Reset to 1991 Order.</html>");
                warning.setVisible(!ok);
            }
        };
        showWarning.run();
        model.addTableModelListener(new javax.swing.event.TableModelListener() {
            public void tableChanged(javax.swing.event.TableModelEvent e) { showWarning.run(); }
        });

        final JButton up = new JButton("Move Up");
        up.setName("pit box up");
        up.setToolTipText("Move the selected team one box nearer the pit lane entrance");
        final JButton down = new JButton("Move Down");
        down.setName("pit box down");
        down.setToolTipText("Move the selected team one box further along the pit lane");
        JButton reset = new JButton("Reset to 1991 Order");
        reset.setName("pit order reset");
        reset.setToolTipText("Put the teams back in the boxes the game came with");
        final Runnable enableMoves = new Runnable() {
            public void run() {
                int r = table.getSelectedRow();
                up.setEnabled(r > 0);
                down.setEnabled(r >= 0 && r < boxes - 1);
            }
        };
        enableMoves.run();
        table.getSelectionModel().addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent e) { enableMoves.run(); }
        });
        java.awt.event.ActionListener move = new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                int r = table.getSelectedRow();
                int to = r + (e.getSource() == up ? -1 : 1);
                if (r < 0 || to < 0 || to >= boxes) return;
                if (table.isEditing()) table.getCellEditor().stopCellEditing();
                tables.swapPitBoxes(r, to);
                model.fireTableDataChanged();
                table.setRowSelectionInterval(to, to);
            }
        };
        up.addActionListener(move);
        down.addActionListener(move);
        reset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (table.isEditing()) table.getCellEditor().stopCellEditing();
                int r = table.getSelectedRow();
                tables.resetPitOrder();
                model.fireTableDataChanged();
                if (r >= 0) table.setRowSelectionInterval(r, r);
            }
        });

        JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 6));
        buttons.add(up);
        buttons.add(down);
        buttons.add(reset);
        JPanel side = new JPanel(new BorderLayout());
        side.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        side.add(buttons, BorderLayout.NORTH);
        JPanel row = new JPanel(new BorderLayout());
        row.add(scroll, BorderLayout.WEST);
        row.add(side, BorderLayout.CENTER);
        JPanel content = new JPanel(new BorderLayout());
        content.add(warning, BorderLayout.NORTH);
        JPanel narrow = new JPanel(new BorderLayout());
        narrow.add(row, BorderLayout.WEST);
        content.add(narrow, BorderLayout.CENTER);
        JPanel top = new JPanel(new BorderLayout());
        top.add(content, BorderLayout.NORTH);
        return page("Pit Lane", "<html>Which team uses each pit box, starting at the pit lane entrance. Choosing a team for a box swaps "
                    + "it with the team already there.<br>Used at every circuit. A team with no cars in a session leaves its box "
                    + "empty; the other teams do not move up.</html>", top);
    }

    private static String teamLabel(GameTeams teams, int team)
    {
        return team + "  " + (team >= 1 && team <= 20 ? teams.getTeamName(team - 1) : "?");
    }

    // --- Setups ------------------------------------------------------------

    /** The setup last copied, shared by every tab and executable, so one can go from a car to a circuit
        or from one game to another. */
    private static int[] copiedSetup;

    private JComponent setupsPage()
    {
        final GameTables tables = currentExecutable.getGameTables();
        // the values, rules, wording and copying are the core's (SetupsPage, which notes which cars read
        // these), shared with Android; the game's setup files are still this window's
        final SetupsPage setups = new SetupsPage(tables, currentExecutable.getGameDrivers());
        final JTabbedPane tabs = new JTabbedPane();
        final JTable[] grids = {
            setupTable(setups, SetupsPage.QUALIFYING, "qualifying setups"),
            setupTable(setups, SetupsPage.RACE, "race setups"),
            setupTable(setups, SetupsPage.QUICK_RACE, "quick race setups") };
        final GameTables.Table[] sources = { tables.circuitSetups, tables.circuitSetups, tables.carSetups };
        final int[] firstRows = { 0, 16, 0 };
        for (int tab = 0; tab < 3; tab++)
            tabs.addTab(setups.tabTitle(tab), new JScrollPane(grids[tab]));

        final JCheckBox separate = new JCheckBox(SetupsPage.SEPARATE, setups.isSeparate());
        separate.setName("separate setups");
        separate.setToolTipText(SetupsPage.SEPARATE_TIP);
        final JButton toRace = new JButton(SetupsPage.TO_RACE);
        final JButton toQualifying = new JButton(SetupsPage.TO_QUALIFYING);
        final Runnable showSeparate = new Runnable() {
            public void run() {
                boolean on = separate.isSelected();
                tabs.setTitleAt(0, setups.tabTitle(SetupsPage.QUALIFYING));
                tabs.setEnabledAt(1, on);
                tabs.setToolTipTextAt(1, on ? null : "Used only when race and qualifying setups are separate");
                if (!on && tabs.getSelectedIndex() == 1)
                    tabs.setSelectedIndex(0);
                toRace.setEnabled(on);
                toQualifying.setEnabled(on);
            }
        };
        showSeparate.run();
        separate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                setups.setSeparate(separate.isSelected());
                setModified(true);
                showSeparate.run();
            }
        });

        // Copy, Paste and Apply to All work on the tab showing
        final JButton paste = new JButton("Paste");
        paste.setName("paste setup");
        paste.setToolTipText("Give every selected row the copied setup");
        paste.setEnabled(copiedSetup != null);
        final JButton applyAll = new JButton(SetupsPage.applyAllLabel(SetupsPage.QUALIFYING));
        applyAll.setName("apply setup to all");
        final JLabel copied = new JLabel(" ");
        copied.setForeground(new Color(0x6A7380));
        tabs.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent e) {
                boolean cars = tabs.getSelectedIndex() == 2;
                applyAll.setText(SetupsPage.applyAllLabel(tabs.getSelectedIndex()));
                applyAll.setToolTipText("Give every " + (cars ? "car" : "circuit") + " on this tab the selected row's setup");
            }
        });
        applyAll.setToolTipText("Give every circuit on this tab the selected row's setup");
        final Runnable copy = new Runnable() {
            public void run() {
                int tab = tabs.getSelectedIndex();
                int r = grids[tab].getSelectedRow();
                if (r < 0)
                    return;
                copiedSetup = setups.read(tab, r);
                paste.setEnabled(true);
                copied.setText(setups.copiedText(tab, r));
            }
        };
        final Runnable pasteRun = new Runnable() {
            public void run() {
                int tab = tabs.getSelectedIndex();
                if (copiedSetup != null)
                {
                    setups.write(tab, grids[tab].getSelectedRows(), copiedSetup);
                    redraw(grids[tab], grids[tab].getSelectedRows());
                }
            }
        };
        JButton copyButton = new JButton("Copy");
        copyButton.setName("copy setup");
        copyButton.setToolTipText("Copy the selected row's setup, to paste on this tab or another");
        copyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { copy.run(); }
        });
        paste.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { pasteRun.run(); }
        });
        applyAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                int tab = tabs.getSelectedIndex();
                JTable grid = grids[tab];
                if (grid.getSelectedRow() < 0)
                {
                    cfevolution.gui.MessageBoxes.show(grid, SetupsPage.chooseRowFirst(tab));
                    return;
                }
                setups.applyToAll(tab, grid.getSelectedRow());
                int[] all = new int[grid.getRowCount()];
                for (int r = 0; r < all.length; r++) all[r] = r;
                redraw(grid, all);
            }
        });
        int menuKey = java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        for (JTable grid : grids)
        {
            grid.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, menuKey), "copySetup");
            grid.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, menuKey), "pasteSetup");
            grid.getActionMap().put("copySetup", new AbstractAction() {
                public void actionPerformed(java.awt.event.ActionEvent e) { copy.run(); }
            });
            grid.getActionMap().put("pasteSetup", new AbstractAction() {
                public void actionPerformed(java.awt.event.ActionEvent e) { pasteRun.run(); }
            });
        }

        toRace.setName("copy qualifying to race");
        toRace.setToolTipText("Give every circuit's race setup its qualifying setup");
        toQualifying.setName("copy race to qualifying");
        toQualifying.setToolTipText("Give every circuit's qualifying setup its race setup");
        java.awt.event.ActionListener between = new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                boolean race = e.getSource() == toRace;
                if (cfevolution.gui.MessageBoxes.choose(tabs, SetupsPage.betweenQuestion(race)) != 0)
                    return;
                setups.copyBetween(race);
                int[] all = new int[16];
                for (int c = 0; c < 16; c++) all[c] = c;
                redraw(grids[race ? 1 : 0], all);
            }
        };
        toRace.addActionListener(between);
        toQualifying.addActionListener(between);

        JPanel edit = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        edit.add(copyButton);
        edit.add(paste);
        edit.add(applyAll);
        edit.add(toRace);
        edit.add(toQualifying);
        edit.add(copied);

        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        south.add(edit);
        south.add(setupFileButtons(tabs, grids, sources, firstRows, tables, separate, showSeparate));

        JPanel content = new JPanel(new BorderLayout(0, 6));
        content.add(separate, BorderLayout.NORTH);
        content.add(tabs, BorderLayout.CENTER);
        content.add(south, BorderLayout.SOUTH);
        return page(SetupsPage.TITLE, "<html>" + String.join("<br>", SetupsPage.DESCRIPTION) + "</html>", content);
    }

    private static int[] readSetup(GameTables.Table t, int row)
    {
        int[] setup = new int[GameTables.SETUP_SIZE];
        for (int i = 0; i < setup.length; i++)
            setup[i] = t.getByte(row * GameTables.SETUP_SIZE + i);
        return setup;
    }

    /** Puts one setup into each of the given rows of a tab and redraws them. */
    private static void writeSetups(JTable grid, GameTables.Table t, int firstRow, int[] rows, int[] setup)
    {
        for (int r : rows)
        {
            for (int i = 0; i < setup.length; i++)
                t.setByte((firstRow + r) * GameTables.SETUP_SIZE + i, setup[i]);
            ((AbstractTableModel) grid.getModel()).fireTableRowsUpdated(r, r);
        }
    }

    /** The game's own setup files: one setup (any row) or a season's (every circuit, both sessions). */
    private JComponent setupFileButtons(final JTabbedPane tabs, final JTable[] grids, final GameTables.Table[] sources,
                                        final int[] firstRows, final GameTables tables, final JCheckBox separate,
                                        final Runnable showSeparate)
    {
        final File exeFolder = new File(fileName).getAbsoluteFile().getParentFile();
        final File saves = new File(exeFolder, "GPSAVES").isDirectory() ? new File(exeFolder, "GPSAVES") : exeFolder;

        JButton loadOne = new JButton("Load Setup File...");
        loadOne.setName("load setup file");
        loadOne.setToolTipText("Give the selected rows a setup saved from the game's setup screen");
        loadOne.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                int tab = tabs.getSelectedIndex();
                int[] rows = grids[tab].getSelectedRows();
                if (rows.length == 0)
                {
                    JOptionPane.showMessageDialog(tabs, "Select the " + (tab == 2 ? "cars" : "circuits") + " to load the setup onto.",
                        "Load Setup File", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                File f = chooseSetupFile(saves, "Load Setup File", null);
                if (f == null)
                    return;
                cfevolution.data.gamefiles.SetupFile file = loadSetupFile(f, false);
                if (file == null)
                    return;
                int[] setup = new int[GameTables.SETUP_SIZE];
                for (int i = 0; i < setup.length; i++) setup[i] = file.getByte(i);
                writeSetups(grids[tab], sources[tab], firstRows[tab], rows, setup);
            }
        });
        JButton saveOne = new JButton("Save Setup File...");
        saveOne.setName("save setup file");
        saveOne.setToolTipText("Save the selected row's setup as a file the game's setup screen can load");
        saveOne.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                int tab = tabs.getSelectedIndex();
                int r = grids[tab].getSelectedRow();
                if (r < 0)
                {
                    JOptionPane.showMessageDialog(tabs, "Select the " + (tab == 2 ? "car" : "circuit") + " whose setup to save.",
                        "Save Setup File", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                String name = String.valueOf(grids[tab].getValueAt(r, 0)).replaceAll("^[0-9]+ +", "").replaceAll("[^A-Za-z0-9]", "");
                File f = chooseSetupFile(saves, "Save Setup File", name.length() > 8 ? name.substring(0, 8).toUpperCase() : name.toUpperCase());
                if (f == null)
                    return;
                cfevolution.data.gamefiles.SetupFile file = cfevolution.data.gamefiles.SetupFile.create(false);
                int[] setup = readSetup(sources[tab], firstRows[tab] + r);
                for (int i = 0; i < setup.length; i++) file.setByte(i, setup[i]);
                saveSetupFile(file, f);
            }
        });
        JButton loadSeason = new JButton("Load Season Setups...");
        loadSeason.setName("load season setups");
        loadSeason.setToolTipText("Give every circuit the qualifying and race setups of a season setup file");
        loadSeason.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                File f = chooseSetupFile(saves, "Load Season Setups", null);
                if (f == null)
                    return;
                cfevolution.data.gamefiles.SetupFile file = loadSetupFile(f, true);
                if (file == null)
                    return;
                for (int i = 0; i < 32 * GameTables.SETUP_SIZE; i++)
                    tables.circuitSetups.setByte(i, file.getByte(2 + i));
                // the game takes the file's choice of separate setups with it, as here
                separate.setSelected(file.isSeparateRaceSetups());
                tables.separateSetups.setWord(0, file.isSeparateRaceSetups() ? 0xFFFF : 0);
                showSeparate.run();
                ((AbstractTableModel) grids[0].getModel()).fireTableDataChanged();
                ((AbstractTableModel) grids[1].getModel()).fireTableDataChanged();
            }
        });
        JButton saveSeason = new JButton("Save Season Setups...");
        saveSeason.setName("save season setups");
        saveSeason.setToolTipText("Save every circuit's qualifying and race setups as one file, as the game's Save Current Car Setups does");
        saveSeason.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                File f = chooseSetupFile(saves, "Save Season Setups", "SEASON");
                if (f == null)
                    return;
                cfevolution.data.gamefiles.SetupFile file = cfevolution.data.gamefiles.SetupFile.create(true);
                file.setSeparateRaceSetups(separate.isSelected());
                for (int i = 0; i < 32 * GameTables.SETUP_SIZE; i++)
                    file.setByte(2 + i, tables.circuitSetups.getByte(i));
                saveSetupFile(file, f);
            }
        });
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        p.add(loadOne);
        p.add(saveOne);
        p.add(loadSeason);
        p.add(saveSeason);
        p.add(new JLabel("<html><font color='#6A7380'>" + SetupsPage.SETUP_FILES + "</font></html>"));
        return p;
    }

    private File chooseSetupFile(File folder, String title, String suggested)
    {
        JFileChooser chooser = new JFileChooser(folder);
        chooser.setDialogTitle(title);
        if (suggested == null)
            return chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile() : null;
        chooser.setSelectedFile(new File(folder, suggested));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return null;
        File f = chooser.getSelectedFile();
        if (f.exists() && JOptionPane.showConfirmDialog(this, f.getName() + " already exists. Replace it?", title,
                                                        JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
            return null;
        return f;
    }

    /** The file as a setup file of the kind wanted, or null after saying why not. */
    private cfevolution.data.gamefiles.SetupFile loadSetupFile(File f, boolean season)
    {
        try
        {
            if (cfevolution.data.gamefiles.SetupFile.isSetupFile(f))
            {
                cfevolution.data.gamefiles.SetupFile file = cfevolution.data.gamefiles.SetupFile.load(f);
                if (file.isSeason() == season)
                    return file;
                JOptionPane.showMessageDialog(this, f.getName() + (season
                    ? " holds one setup, not a season's. Use Load Setup File for it."
                    : " holds a season's setups, not one. Use Load Season Setups for it."),
                    "Nothing Loaded", JOptionPane.WARNING_MESSAGE);
                return null;
            }
        }
        catch (java.io.IOException e) { /* reported below */ }
        JOptionPane.showMessageDialog(this, f.getName() + " is not a setup file saved by the game, so nothing was changed.",
            "Nothing Loaded", JOptionPane.WARNING_MESSAGE);
        return null;
    }

    private void saveSetupFile(cfevolution.data.gamefiles.SetupFile file, File f)
    {
        try
        {
            file.save(f);
        }
        catch (java.io.IOException e)
        {
            JOptionPane.showMessageDialog(this, f.getName() + " could not be saved.", "Not Saved", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Redraws these rows of a setups tab, which marks the tab changed. */
    private static void redraw(JTable grid, int[] rows)
    {
        for (int r : rows)
            ((AbstractTableModel) grid.getModel()).fireTableRowsUpdated(r, r);
    }

    private JTable setupTable(final SetupsPage setups, final int tab, String name)
    {
        final String[] cols = SetupsPage.COLUMNS;
        final AbstractTableModel model = new AbstractTableModel() {
            public int getRowCount() { return setups.rows(tab); }
            public int getColumnCount() { return cols.length + 1; }
            public String getColumnName(int c) { return c == 0 ? "" : cols[c - 1]; }
            public boolean isCellEditable(int r, int c) { return c > 0; }
            public Object getValueAt(int r, int c) {
                if (c == 0) return setups.rowLabel(tab, r);
                String v = setups.value(tab, r, c - 1);
                return c - 1 == SetupFile.TYRES ? (Object) v : (Object) Integer.valueOf(v);
            }
            public void setValueAt(Object v, int r, int c) {
                // The game's own limits (Hrvoje, 2026-11-05: wings went to 255, the game's screen stops at 64)
                boolean[] changed = new boolean[1];
                cfevolution.edit.Message m = setups.set(tab, r, c - 1, String.valueOf(v), changed);
                if (m != null) cfevolution.gui.MessageBoxes.show(ExecutableWindow.this, m);
                if (changed[0]) fireTableCellUpdated(r, c);
            }
        };
        int[] widths = new int[cols.length + 1];
        widths[0] = 170;
        for (int i = 1; i < widths.length; i++) widths[i] = 76;
        widths[10] = 104;   // room for "Brake balance"
        JTable table = table(model, widths);
        table.setName(name);
        // tyres chosen from the game's four compounds (Hrvoje, 2026-11-05)
        table.getColumnModel().getColumn(9).setCellEditor(new DefaultCellEditor(new JComboBox<String>(SetupFile.COMPOUNDS)));
        // several rows at once, so one Paste or Load covers them
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        return table;
    }

    // --- Lap records --------------------------------------------------------

    private JComponent lapRecordsPage()
    {
        // what each cell shows and the rules for changing it are the core's (LapRecordsPage), shared with Android
        final LapRecordsPage records = new LapRecordsPage(currentExecutable.getGameTables());
        final String[] cols = LapRecordsPage.COLUMNS;
        final AbstractTableModel model = new AbstractTableModel() {
            public int getRowCount() { return LapRecordsPage.ROWS; }
            public int getColumnCount() { return cols.length; }
            public String getColumnName(int c) { return cols[c]; }
            public boolean isCellEditable(int r, int c) { return c > 0; }
            public Object getValueAt(int r, int c) { return records.value(r, c); }
            public void setValueAt(Object v, int r, int c) {
                boolean[] changed = new boolean[1];
                cfevolution.edit.Message m = records.set(r, c, String.valueOf(v), changed);
                if (m != null) cfevolution.gui.MessageBoxes.show(ExecutableWindow.this, m);
                if (changed[0]) fireTableCellUpdated(r, c);
            }
        };
        JTable table = table(model, new int[] { 110, 150, 100, 80, 100, 150, 100, 80, 100 });
        return page(LapRecordsPage.TITLE, LapRecordsPage.DESCRIPTION, new JScrollPane(table));
    }

    // --- Car physics --------------------------------------------------------

    private JComponent physicsPage()
    {
        return tablePage(TablePage.physics(currentExecutable),
            new int[][] { { 150, 56 }, { 150, 70 }, { 150, 70 }, { 150, 70 }, { 150, 100 }, { 150, 64 }, { 150, 80 }, { 150, 70 } });
    }

    // --- Opposition levels --------------------------------------------------

    private JComponent oppositionPage()
    {
        return tablePage(TablePage.opposition(currentExecutable), new int[][] { { 220, 90 }, { 220, 90 } });
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
        cfevolution.edit.Message notSaved = cfevolution.edit.ExecutableSaving.save(currentExecutable);
        if (notSaved != null)
        {
            cfevolution.gui.MessageBoxes.show(this, notSaved);
            return false;
        }
        setModified(false);
        return true;
    }

    public boolean saveAs()
    {
        cfevolution.gui.MessageBoxes.show(this, cfevolution.edit.ExecutableSaving.SAVE_AS);
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
