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
 * MainGUI.java
 *
 * Created on 22 February 2005, 22:28
 */
package cfevolution.gui;

import cfevolution.gui.executable.ExecutableWindow;
import cfevolution.gui.track.TrackWindow;
import cfevolution.data.track.*;
import cfevolution.data.gui.*;
import cfevolution.data.executable.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.*;

/**
 * The editor's single window. Each open track or executable is a tab;
 * the menus, toolbar and status bar follow whichever tab is in front.
 * With nothing open, a start page offers the common first steps.
 *
 * @author  barrie
 */
public class MainGUI extends JFrame
{
    private static final String WELCOME = "welcome";
    private static final String EDITORS = "editors";

    private final List<EditorTab> tabs = new ArrayList<EditorTab>();
    private JTabbedPane tabPane;
    private JPanel centre;
    private CardLayout centreCards;
    private JToolBar toolbar;
    private JLabel statusText;
    private JLabel statusSaved;
    private JPanel recentPanel;
    private JButton saveButton;

    public MainGUI()
    {
        super("CF Evolution");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { quitCFEvolution(); }
        });

        tabPane = new JTabbedPane();
        tabPane.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent e) { activeTabChanged(); }
        });

        centreCards = new CardLayout();
        centre = new JPanel(centreCards);
        centre.add(createWelcomePage(), WELCOME);
        centre.add(tabPane, EDITORS);

        toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xD5D9DF)),
            BorderFactory.createEmptyBorder(3, 6, 3, 6)));

        JPanel content = new JPanel(new BorderLayout());
        content.add(toolbar, BorderLayout.NORTH);
        content.add(centre, BorderLayout.CENTER);
        content.add(createStatusBar(), BorderLayout.SOUTH);
        setContentPane(content);

        setTransferHandler(new FileDropHandler());
        fitToScreen(1280, 820);
        activeTabChanged();
    }

    /** Sizes the window to the preferred size, but never larger than the
        usable screen (without the taskbar), and centres it there. A window
        taller than the screen puts its title bar, with the minimise,
        maximise and close buttons, off the top. On a screen too small for
        the preferred size the window starts maximised. */
    private void fitToScreen(int width, int height)
    {
        Rectangle usable = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int w = Math.min(width, usable.width);
        int h = Math.min(height, usable.height);
        setBounds(usable.x + (usable.width - w) / 2, usable.y + (usable.height - h) / 2, w, h);
        if (w < width || h < height)
            setExtendedState(getExtendedState() | MAXIMIZED_BOTH);
    }

    // --- Building blocks ------------------------------------------------

    /** A flat toolbar button running the given action. */
    public static JButton toolbarButton(String text, String tooltip, final Runnable action)
    {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { action.run(); }
        });
        return button;
    }

    private JMenuItem menuItem(String text, KeyStroke key, final Runnable action)
    {
        JMenuItem item = new JMenuItem(text);
        if (key != null)
            item.setAccelerator(key);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { action.run(); }
        });
        return item;
    }

    private static KeyStroke ctrl(int key)
    {
        return KeyStroke.getKeyStroke(key, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    }

    private static KeyStroke ctrlShift(int key)
    {
        return KeyStroke.getKeyStroke(key, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | InputEvent.SHIFT_DOWN_MASK);
    }

    private JComponent createStatusBar()
    {
        statusText = new JLabel(" ");
        statusSaved = new JLabel(" ");
        JPanel bar = new JPanel(new BorderLayout());
        bar.add(statusText, BorderLayout.CENTER);
        bar.add(statusSaved, BorderLayout.EAST);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xD5D9DF)),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        statusText.setForeground(new Color(0x4A525C));
        return bar;
    }

    private JComponent createWelcomePage()
    {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xD5D9DF)),
            BorderFactory.createEmptyBorder(28, 36, 28, 36)));

        JLabel title = new JLabel("CF Evolution");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 30f));
        title.setForeground(new Color(0xC23B22));
        JLabel subtitle = new JLabel("Track and game editor for Formula One Grand Prix / World Circuit");
        subtitle.setForeground(new Color(0x5A626C));
        card.add(left(title));
        card.add(Box.createVerticalStrut(4));
        card.add(left(subtitle));
        card.add(Box.createVerticalStrut(24));

        JPanel buttons = new JPanel(new GridLayout(1, 2, 12, 0));
        buttons.setOpaque(false);
        buttons.add(bigButton("Open Track", "An F1CTnn.DAT circuit file", new Runnable() {
            public void run() { loadTrack(); } }));
        buttons.add(bigButton("Open Game Executable", "Drivers, teams, points and settings", new Runnable() {
            public void run() { openExecutable(); } }));
        buttons.setMaximumSize(new Dimension(560, 74));
        card.add(left(buttons));
        card.add(Box.createVerticalStrut(24));

        JLabel recentTitle = new JLabel("Recent tracks");
        recentTitle.setFont(recentTitle.getFont().deriveFont(Font.BOLD));
        card.add(left(recentTitle));
        card.add(Box.createVerticalStrut(6));
        recentPanel = new JPanel();
        recentPanel.setOpaque(false);
        recentPanel.setLayout(new BoxLayout(recentPanel, BoxLayout.Y_AXIS));
        card.add(left(recentPanel));
        card.add(Box.createVerticalStrut(20));

        JLabel tip = new JLabel("<html>You can also drop track or executable files onto this window.<br>"
            + "Ctrl+O opens a track, Ctrl+E an executable, Ctrl+S saves, Ctrl+W closes a tab.</html>");
        tip.setForeground(new Color(0x7A828C));
        card.add(left(tip));

        JPanel page = new JPanel(new GridBagLayout());
        page.setBackground(new Color(0xF2F3F5));
        page.add(card);
        return page;
    }

    private static JComponent left(JComponent c)
    {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    private JButton bigButton(String title, String detail, final Runnable action)
    {
        JButton button = new JButton("<html><b style='font-size:12px'>" + title + "</b><br><span style='color:#5A626C'>"
                                     + detail + "</span></html>");
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xB8C6D8)),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        button.setBackground(new Color(0xF3F7FC));
        button.setFocusable(false);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { action.run(); }
        });
        return button;
    }

    /** The last two folders of a path, so long paths stay readable. */
    private static String shortFolder(String path)
    {
        File parent = new File(path).getParentFile();
        if (parent == null)
            return "";
        File grand = parent.getParentFile();
        return (grand != null && grand.getParentFile() != null ? "\u2026" + File.separator + grand.getName() + File.separator : "")
             + parent.getName();
    }

    private void refreshWelcomeRecent()
    {
        recentPanel.removeAll();
        RecentFiles usedFiles = new RecentFiles();
        int shown = 0;
        for (int i = 0; i < 4; i++)
        {
            final String path = usedFiles.getRecentFilePath(i);
            if (path == null || path.length() == 0)
                continue;
            JButton link = new JButton("<html><span style='color:#2F6FBF'>" + usedFiles.getRecentFileName(i)
                                       + "</span>&nbsp;&nbsp;<span style='color:#8A929C'>" + shortFolder(path)
                                       + "</span></html>");
            link.setToolTipText(path);
            link.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
            link.setContentAreaFilled(false);
            link.setFocusable(false);
            link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            // Full width with the text on the left, so the list lines up
            // under its heading instead of centring itself
            link.setHorizontalAlignment(SwingConstants.LEFT);
            link.setMaximumSize(new Dimension(Integer.MAX_VALUE, link.getPreferredSize().height));
            link.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) { openTrackFile(path); }
            });
            recentPanel.add(left(link));
            shown++;
        }
        if (shown == 0)
        {
            JLabel none = new JLabel("None yet");
            none.setForeground(new Color(0x8A929C));
            recentPanel.add(left(none));
        }
        recentPanel.revalidate();
    }

    // --- Tabs -----------------------------------------------------------

    private EditorTab activeTab()
    {
        int index = tabPane.getSelectedIndex();
        return index < 0 ? null : tabs.get(index);
    }

    private void addTab(EditorTab tab)
    {
        tabs.add(tab);
        tabPane.addTab(tab.getTabTitle(), tab.getComponent());
        int index = tabPane.getTabCount() - 1;
        tabPane.setTabComponentAt(index, new TabHeader(tab));
        tabPane.setToolTipTextAt(index, tab.getFilePath());
        tabPane.setSelectedIndex(index);
        activeTabChanged();
    }

    /** Selects an already open file's tab; true if there was one. */
    private boolean selectIfOpen(String fileName)
    {
        String wanted = new File(fileName).getAbsolutePath();
        for (int i = 0; i < tabs.size(); i++)
        {
            if (new File(tabs.get(i).getFilePath()).getAbsolutePath().equals(wanted))
            {
                tabPane.setSelectedIndex(i);
                return true;
            }
        }
        return false;
    }

    /** Called by a tab when it is changed or saved. */
    public void tabStateChanged(EditorTab tab)
    {
        int index = tabs.indexOf(tab);
        if (index >= 0)
        {
            Component header = tabPane.getTabComponentAt(index);
            if (header instanceof TabHeader)
                ((TabHeader) header).refresh();
            tabPane.setToolTipTextAt(index, tab.getFilePath());
        }
        if (tab == activeTab())
            refreshStatus();
        refreshTitle();
    }

    /** Closes a tab, asking first if it has unsaved changes. Returns false
        when the user cancels. */
    public boolean closeTab(EditorTab tab)
    {
        int index = tabs.indexOf(tab);
        if (index < 0)
            return true;
        tabPane.setSelectedIndex(index);
        if (tab.isModified())
        {
            int choice = JOptionPane.showOptionDialog(this,
                "Save the changes to " + tab.getTabTitle() + " before closing it?",
                "Unsaved Changes", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null,
                new String[] { "Save", "Don't Save", "Cancel" }, "Save");
            if (choice == 0)
            {
                if (!tab.save())
                    return false;
            }
            else if (choice != 1)
                return false;
        }
        tabs.remove(index);
        tabPane.removeTabAt(index);
        activeTabChanged();
        return true;
    }

    private void activeTabChanged()
    {
        boolean any = !tabs.isEmpty();
        centreCards.show(centre, any ? EDITORS : WELCOME);
        if (!any)
            refreshWelcomeRecent();
        rebuildMenus();
        rebuildToolbar();
        refreshStatus();
        refreshTitle();
    }

    private void refreshStatus()
    {
        EditorTab tab = activeTab();
        if (tab == null)
        {
            statusText.setText("Open a track or the game executable to start.");
            statusSaved.setText(" ");
            return;
        }
        statusText.setText(tab.getStatusText());
        statusSaved.setText(tab.isModified() ? "● Unsaved changes" : "All changes saved");
        statusSaved.setForeground(tab.isModified() ? new Color(0xB5651D) : new Color(0x5A8F4E));
        if (saveButton != null)
            saveButton.setEnabled(true);
    }

    private void refreshTitle()
    {
        EditorTab tab = activeTab();
        setTitle(tab == null ? "CF Evolution"
                 : (tab.isModified() ? "● " : "") + tab.getTabTitle() + " — CF Evolution");
    }

    private void rebuildToolbar()
    {
        toolbar.removeAll();
        toolbar.add(toolbarButton("Open Track", "Open a track file (Ctrl+O)", new Runnable() {
            public void run() { loadTrack(); } }));
        toolbar.add(toolbarButton("Open Executable", "Open the game executable (Ctrl+E)", new Runnable() {
            public void run() { openExecutable(); } }));
        saveButton = toolbarButton("Save", "Save the file in front (Ctrl+S)", new Runnable() {
            public void run() { saveActive(); } });
        saveButton.setEnabled(activeTab() != null);
        toolbar.add(saveButton);
        EditorTab tab = activeTab();
        if (tab != null)
        {
            JComponent[] items = tab.getToolbarItems();
            if (items.length > 0)
                toolbar.addSeparator();
            for (int i = 0; i < items.length; i++)
            {
                if (items[i] == null)
                    toolbar.addSeparator();
                else
                    toolbar.add(items[i]);
            }
        }
        toolbar.revalidate();
        toolbar.repaint();
    }

    private void rebuildMenus()
    {
        JMenuBar bar = new JMenuBar();
        EditorTab tab = activeTab();

        JMenu file = new JMenu("File");
        file.setMnemonic('F');
        file.add(menuItem("Open Track...", ctrl(KeyEvent.VK_O), new Runnable() { public void run() { loadTrack(); } }));
        file.add(menuItem("Open Executable...", ctrl(KeyEvent.VK_E), new Runnable() { public void run() { openExecutable(); } }));
        JMenu recent = new JMenu("Open Recent");
        RecentFiles usedFiles = new RecentFiles();
        for (int i = 0; i < 4; i++)
        {
            final String path = usedFiles.getRecentFilePath(i);
            if (path == null || path.length() == 0)
                continue;
            recent.add(menuItem(usedFiles.getRecentFileName(i), null, new Runnable() {
                public void run() { openTrackFile(path); } }));
        }
        recent.setEnabled(recent.getItemCount() > 0);
        file.add(recent);
        file.addSeparator();
        JMenuItem save = menuItem("Save", ctrl(KeyEvent.VK_S), new Runnable() { public void run() { saveActive(); } });
        JMenuItem saveAs = menuItem("Save As...", ctrlShift(KeyEvent.VK_S), new Runnable() {
            public void run() { EditorTab t = activeTab(); if (t != null) t.saveAs(); } });
        JMenuItem close = menuItem("Close", ctrl(KeyEvent.VK_W), new Runnable() {
            public void run() { EditorTab t = activeTab(); if (t != null) closeTab(t); } });
        save.setEnabled(tab != null);
        saveAs.setEnabled(tab != null);
        close.setEnabled(tab != null);
        file.add(save);
        file.add(saveAs);
        if (tab != null)
        {
            JMenuItem[] extra = tab.getFileMenuItems();
            if (extra.length > 0)
                file.addSeparator();
            for (int i = 0; i < extra.length; i++)
                file.add(extra[i]);
        }
        file.addSeparator();
        file.add(close);
        file.addSeparator();
        file.add(menuItem("Options...", null, new Runnable() { public void run() { openOptionsWindow(); } }));
        file.addSeparator();
        file.add(menuItem("Exit", ctrl(KeyEvent.VK_Q), new Runnable() { public void run() { quitCFEvolution(); } }));
        bar.add(file);

        if (tab != null)
        {
            JMenu[] menus = tab.getMenus();
            for (int i = 0; i < menus.length; i++)
                bar.add(menus[i]);
        }

        JMenu help = new JMenu("Help");
        help.setMnemonic('H');
        help.add(menuItem("About CF Evolution", null, new Runnable() {
            public void run() { new AboutGUI(MainGUI.this).setVisible(true); } }));
        bar.add(help);

        setJMenuBar(bar);
        bar.revalidate();
    }

    private void saveActive()
    {
        EditorTab tab = activeTab();
        if (tab != null)
            tab.save();
    }

    /** The header of one tab: its name, a dot while unsaved, and a close button. */
    private class TabHeader extends JPanel
    {
        private final EditorTab tab;
        private final JLabel label = new JLabel();

        TabHeader(final EditorTab tab)
        {
            super(new FlowLayout(FlowLayout.LEFT, 0, 0));
            this.tab = tab;
            setOpaque(false);
            label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
            add(label);
            JButton close = new JButton("×");
            close.setToolTipText("Close (Ctrl+W)");
            close.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
            close.setContentAreaFilled(false);
            close.setFocusable(false);
            close.setFont(close.getFont().deriveFont(15f));
            close.setForeground(new Color(0x7A828C));
            close.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) { closeTab(tab); }
            });
            add(close);
            refresh();
        }

        void refresh()
        {
            label.setText((tab.isModified() ? "● " : "") + tab.getTabTitle());
            label.setFont(label.getFont().deriveFont(tab.isModified() ? Font.BOLD : Font.PLAIN));
        }
    }

    // --- Opening files --------------------------------------------------

    private void loadTrack()
    {
        // Prompts for a track file and opens it in a new tab
        FileDialog fileDialog = new FileDialog(this, true);
        fileDialog.setFilterType(1);
        String fileName;
        try
        {
            Preferences cheqFlagData = Preferences.userRoot().node("cfevolution");
            fileDialog.setInitialDirectory(cheqFlagData.get("LastLoadedTrackDir", ""));
            fileName = fileDialog.showOpenDialog();
        }
        catch (Exception exceptionError)
        {
            return; // cancelled
        }
        openTrackFile(fileName);
    }

    public void openTrackFile(String fileName)
    {
        if (selectIfOpen(fileName))
            return;
        File trackFile = new File(fileName);
        Preferences cheqFlagData = Preferences.userRoot().node("cfevolution");
        cheqFlagData.put("LastLoadedTrackDir", String.valueOf(trackFile.getParent()));
        Track currentTrack = new Track();
        if (currentTrack.load(trackFile))
        {
            updateRecentFileList(fileName);
            addTab(new TrackWindow(currentTrack, fileName, this));
        }
        else
        {
            JOptionPane.showMessageDialog(this, "There was a problem loading the track file specified. \nIt is likely that this is either not a track file, or that the track itself is corrupt.\nPlease try another valid track file", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Opens the executable named in Options, or asks for one (and
        remembers it) when none has been chosen yet. */
    private void openExecutable()
    {
        Preferences cheqFlagData = Preferences.userRoot().node("cfevolution");
        String fileName = cheqFlagData.get("EXELocation", "");
        FileDialog fileDialog = new FileDialog(this, true);
        fileDialog.setFilterType(2);
        fileDialog.setInitialDirectory(fileName);
        try
        {
            fileName = fileDialog.showOpenDialog();
        }
        catch (Exception exceptionError)
        {
            return; // cancelled
        }
        if (openExecutableFile(fileName))
            cheqFlagData.put("EXELocation", fileName);
    }

    public boolean openExecutableFile(String fileName)
    {
        if (selectIfOpen(fileName))
            return true;
        Executable currentExecutable = new Executable(new File(fileName));
        int loadSuccess = currentExecutable.loadData();
        switch (loadSuccess)
        {
            case 0:
                addTab(new ExecutableWindow(currentExecutable, fileName, this));
                return true;
            case 1: JOptionPane.showMessageDialog(this, "The file specified is not a known F1GP/WC executable file. \nThis may be because you have selected the wrong file, or \nbecause this executable has not been updated to version \n1.05. In the latter case, upgrading your copy of F1GP/WC to \n1.05 will resolve the problem.", "Incompatible Executable Detected", JOptionPane.ERROR_MESSAGE); break;
            case 2: JOptionPane.showMessageDialog(this, "An unknown error has occurred during the loading of this \nexecutable file. It is likely that this is either not a \nF1GP/WC executable file, or that the executable itself is \ncorrupt.Please try a valid executable file", "Unknown Error", JOptionPane.ERROR_MESSAGE); break;
            case 3: JOptionPane.showMessageDialog(this, "The executable file specified is a 1.05 packed version \nof F1GP/WC. An unpacked version of this file is required for \nediting in CF Evolution. Please see the documentation \nfor more details", "Packed Executable Detected", JOptionPane.ERROR_MESSAGE); break;
        }
        return false;
    }

    /** Opens files dropped on the window: .EXE as an executable, anything
        else as a track. */
    private class FileDropHandler extends TransferHandler
    {
        public boolean canImport(TransferSupport support)
        {
            return support.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
        }

        public boolean importData(TransferSupport support)
        {
            try
            {
                @SuppressWarnings("unchecked")
                List<File> files = (List<File>) support.getTransferable()
                    .getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
                for (File f : files)
                {
                    if (f.getName().toLowerCase().endsWith(".exe"))
                        openExecutableFile(f.getPath());
                    else
                        openTrackFile(f.getPath());
                }
                return true;
            }
            catch (Exception e)
            {
                return false;
            }
        }
    }

    // --- Options, recent files, quitting ----------------------------------

    public void openOptionsWindow()
    {
        new OptionsWindow(this).setVisible(true);
    }

    /** Re-applies the "Enable legacy options" preference to every open
        track's Tools menu (called when the option is changed). */
    public void refreshLegacyOptions()
    {
        for (EditorTab tab : tabs)
            if (tab instanceof TrackWindow)
                ((TrackWindow) tab).applyLegacyOptionsVisibility();
    }

    /** Adds a file to the recent list (an empty name only refreshes it). */
    public void updateRecentFileList(String newFileName)
    {
        new RecentFiles().addNewFile(newFileName);
        rebuildMenus();
        if (tabs.isEmpty())
            refreshWelcomeRecent();
    }

    public void quitCFEvolution()
    {
        // Closes every tab (each asks about unsaved changes), then exits
        while (!tabs.isEmpty())
        {
            if (!closeTab(tabs.get(tabs.size() - 1)))
                return;
        }
        System.exit(0);
    }
}
