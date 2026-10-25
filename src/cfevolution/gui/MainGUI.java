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
import cfevolution.data.GameFolder;
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
 * The editor's single window. Each open game folder, track or executable
 * is a tab;
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
    private JPanel gameFolderPanel;

    /** The game folder opened last, offered again on the start page. */
    public static final String PREF_GAME_FOLDER = "GameFolder";
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
        buttons.add(bigButton("Open Game Folder", "Circuits, drivers, teams and settings", new Runnable() {
            public void run() { openGameFolder(); } }));
        buttons.add(bigButton("Open Track", "An F1CTnn.DAT circuit file", new Runnable() {
            public void run() { loadTrack(); } }));
        buttons.setMaximumSize(new Dimension(560, 74));
        card.add(left(buttons));
        card.add(Box.createVerticalStrut(24));

        JLabel gameTitle = new JLabel("Game folder");
        gameTitle.setFont(gameTitle.getFont().deriveFont(Font.BOLD));
        card.add(left(gameTitle));
        card.add(Box.createVerticalStrut(6));
        gameFolderPanel = new JPanel();
        gameFolderPanel.setOpaque(false);
        gameFolderPanel.setLayout(new BoxLayout(gameFolderPanel, BoxLayout.Y_AXIS));
        card.add(left(gameFolderPanel));
        card.add(Box.createVerticalStrut(16));

        JLabel recentTitle = new JLabel("Recent tracks");
        recentTitle.setFont(recentTitle.getFont().deriveFont(Font.BOLD));
        card.add(left(recentTitle));
        card.add(Box.createVerticalStrut(6));
        recentPanel = new JPanel();
        recentPanel.setOpaque(false);
        recentPanel.setLayout(new BoxLayout(recentPanel, BoxLayout.Y_AXIS));
        card.add(left(recentPanel));
        card.add(Box.createVerticalStrut(20));

        JLabel tip = new JLabel("<html>You can also drop the game folder or track files onto this window.<br>"
            + "Ctrl+G opens the game folder, Ctrl+O a track, Ctrl+S saves, Ctrl+W closes a tab.</html>");
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

    /** A link in the start page's lists. */
    private static JButton link(String name, String where, String tooltip, final Runnable action)
    {
        JButton link = new JButton("<html><span style='color:#2F6FBF'>" + name
                                   + "</span>&nbsp;&nbsp;<span style='color:#8A929C'>" + where + "</span></html>");
        link.setToolTipText(tooltip);
        link.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        link.setContentAreaFilled(false);
        link.setFocusable(false);
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // Full width with the text on the left, so the list lines up
        // under its heading instead of centring itself
        link.setHorizontalAlignment(SwingConstants.LEFT);
        link.setMaximumSize(new Dimension(Integer.MAX_VALUE, link.getPreferredSize().height));
        link.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { action.run(); }
        });
        return link;
    }

    /** The last game folder, if it is still there. */
    private static String lastGameFolder()
    {
        String folder = Preferences.userRoot().node("cfevolution").get(PREF_GAME_FOLDER, "");
        return folder.length() > 0 && new File(folder).isDirectory() ? folder : null;
    }

    private void refreshWelcomeRecent()
    {
        gameFolderPanel.removeAll();
        final String folder = lastGameFolder();
        if (folder != null)
            gameFolderPanel.add(left(link(new File(folder).getName(), shortFolder(folder), folder, new Runnable() {
                public void run() { openGameFolder(folder); } })));
        else
        {
            JLabel none = new JLabel("None yet");
            none.setForeground(new Color(0x8A929C));
            gameFolderPanel.add(left(none));
        }
        gameFolderPanel.revalidate();

        recentPanel.removeAll();
        RecentFiles usedFiles = new RecentFiles();
        int shown = 0;
        for (int i = 0; i < 4; i++)
        {
            final String path = usedFiles.getRecentFilePath(i);
            if (path == null || path.length() == 0)
                continue;
            recentPanel.add(left(link(usedFiles.getRecentFileName(i), shortFolder(path), path, new Runnable() {
                public void run() { openTrackFile(path); } })));
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
            statusText.setText("Open the game folder or a track to start.");
            statusSaved.setText(" ");
            return;
        }
        statusText.setText(tab.getStatusText());
        statusSaved.setText(tab instanceof GameWindow ? " " : tab.isModified() ? "● Unsaved changes" : "All changes saved");
        statusSaved.setForeground(tab.isModified() ? new Color(0xB5651D) : new Color(0x5A8F4E));
        if (saveButton != null)
            saveButton.setEnabled(canSave(tab));
    }

    /** A game folder tab has nothing of its own to save. */
    private static boolean canSave(EditorTab tab)
    {
        return tab != null && !(tab instanceof GameWindow);
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
        toolbar.add(toolbarButton("Open Game Folder", "Open the game's folder: its circuits and GP.EXE (Ctrl+G)", new Runnable() {
            public void run() { openGameFolder(); } }));
        toolbar.add(toolbarButton("Open Track", "Open a track file (Ctrl+O)", new Runnable() {
            public void run() { loadTrack(); } }));
        saveButton = toolbarButton("Save", "Save the file in front (Ctrl+S)", new Runnable() {
            public void run() { saveActive(); } });
        saveButton.setEnabled(canSave(activeTab()));
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
        file.add(menuItem("Open Game Folder...", ctrl(KeyEvent.VK_G), new Runnable() { public void run() { openGameFolder(); } }));
        file.add(menuItem("Open Track...", ctrl(KeyEvent.VK_O), new Runnable() { public void run() { loadTrack(); } }));
        JMenu recent = new JMenu("Open Recent");
        final String folder = lastGameFolder();
        if (folder != null)
        {
            recent.add(menuItem("Game folder: " + new File(folder).getName(), null, new Runnable() {
                public void run() { openGameFolder(folder); } }));
            recent.addSeparator();
        }
        RecentFiles usedFiles = new RecentFiles();
        for (int i = 0; i < 4; i++)
        {
            final String path = usedFiles.getRecentFilePath(i);
            if (path == null || path.length() == 0)
                continue;
            recent.add(menuItem(usedFiles.getRecentFileName(i), null, new Runnable() {
                public void run() { openTrackFile(path); } }));
        }
        if (recent.getMenuComponentCount() > 0 && recent.getMenuComponent(recent.getMenuComponentCount() - 1) instanceof JSeparator)
            recent.remove(recent.getMenuComponentCount() - 1);
        recent.setEnabled(recent.getItemCount() > 0);
        file.add(recent);
        file.addSeparator();
        JMenuItem save = menuItem("Save", ctrl(KeyEvent.VK_S), new Runnable() { public void run() { saveActive(); } });
        JMenuItem saveAs = menuItem("Save As...", ctrlShift(KeyEvent.VK_S), new Runnable() {
            public void run() { EditorTab t = activeTab(); if (t != null) t.saveAs(); } });
        JMenuItem close = menuItem("Close", ctrl(KeyEvent.VK_W), new Runnable() {
            public void run() { EditorTab t = activeTab(); if (t != null) closeTab(t); } });
        save.setEnabled(canSave(tab));
        saveAs.setEnabled(canSave(tab));
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

    /** Asks for the game folder, starting at the one opened last. */
    private void openGameFolder()
    {
        FileDialog fileDialog = new FileDialog(this, true);
        fileDialog.setFolderMode(Preferences.userRoot().node("cfevolution").get(PREF_GAME_FOLDER, ""));
        String folder;
        try
        {
            folder = fileDialog.showOpenDialog();
        }
        catch (Exception exceptionError)
        {
            return; // cancelled
        }
        openGameFolder(folder);
    }

    /** Opens a game folder in a tab: its GP.EXE, which must be unpacked (a
        packed one is offered for unpacking), and its 16 circuits. Returns
        false, after saying why, if it cannot. */
    public boolean openGameFolder(String folder)
    {
        GameFolder game = GameFolder.open(new File(folder));
        for (int i = 0; i < tabs.size(); i++)
        {
            if (tabs.get(i) instanceof GameWindow
                && ((GameWindow) tabs.get(i)).getGameFolder().getFolder().equals(game.getFolder()))
            {
                tabPane.setSelectedIndex(i);
                return true;
            }
        }
        if (game.isPacked())
        {
            game = offerToUnpack(game);
            if (game == null)
                return false;
        }
        if (game.getProblem() != null)
        {
            JOptionPane.showMessageDialog(this, wrap(game.getProblem(), 70), "Game Folder Not Opened", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        Preferences.userRoot().node("cfevolution").put(PREF_GAME_FOLDER, game.getFolder().getPath());
        addTab(new GameWindow(game, this));
        return true;
    }

    /** Asks to unpack a packed GP.EXE, keeping the original as GPPACKED.EXE,
        and does it. Returns the folder looked at again, or null if the user
        cancelled or it failed (after saying so). */
    private GameFolder offerToUnpack(GameFolder game)
    {
        String backupName = GameFolder.BACKUP_NAME;
        int choice = JOptionPane.showOptionDialog(this, wrap(
            "GP.EXE in this folder is the packed version, which the editor cannot change.\n\n"
            + "Unpack it now? The original is kept as " + backupName + ". The game runs the unpacked GP.EXE "
            + "in the same way.", 70),
            "Unpack GP.EXE", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
            new String[] { "Unpack", "Cancel" }, "Unpack");
        if (choice != 0)
            return null;
        boolean replaceBackup = false;
        try
        {
            if (game.getBackup() != null && !game.backupMatches())
            {
                choice = JOptionPane.showOptionDialog(this, wrap(
                    game.getBackup().getName() + " already exists in this folder. Overwrite it with the GP.EXE "
                    + "that is there now?\n\nIf you do not, " + game.getBackup().getName() + " is kept as it is "
                    + "and the current GP.EXE is not kept.", 70),
                    "Backup Already Exists", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null,
                    new String[] { "Overwrite", "Don't Overwrite", "Cancel" }, "Don't Overwrite");
                if (choice != 0 && choice != 1)
                    return null;
                replaceBackup = choice == 0;
            }
            game.unpackExecutable(replaceBackup);
        }
        catch (java.io.IOException e)
        {
            JOptionPane.showMessageDialog(this, "GP.EXE could not be unpacked, so nothing was changed.",
                "Unpack GP.EXE", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        return GameFolder.open(game.getFolder());
    }

    /** Opens one of the game folder's other files (a picture file, options,
        name file, set-up file or championship save) in a tab. */
    public boolean openGameFile(File file, GameFolder game)
    {
        if (selectIfOpen(file.getPath()))
            return true;
        String path = file.getPath();
        try
        {
            long size = file.length();
            EditorTab tab;
            if (cfevolution.data.gamefiles.MediaContainer.isContainer(file))
                tab = new cfevolution.gui.gamefiles.PictureFileTab(cfevolution.data.gamefiles.MediaContainer.load(file), path, this, game);
            else if (cfevolution.data.gamefiles.SoundEffectsFile.isSoundEffects(file))
                tab = new cfevolution.gui.gamefiles.SoundEffectsTab(cfevolution.data.gamefiles.SoundEffectsFile.load(file), path, this);
            else if (size == cfevolution.data.gamefiles.PreferencesFile.SIZE)
                tab = new cfevolution.gui.gamefiles.PreferencesTab(cfevolution.data.gamefiles.PreferencesFile.load(file), path, this);
            else if (size == cfevolution.data.gamefiles.NameFile.SIZE)
                tab = new cfevolution.gui.gamefiles.NameFileTab(cfevolution.data.gamefiles.NameFile.load(file), path, this);
            else if (size == cfevolution.data.gamefiles.ChampionshipSave.SIZE)
            {
                int[] points = null;
                int best = 0;
                Executable exe = new Executable(game.getExecutable());
                if (exe.loadData() == 0)
                {
                    points = new int[26];
                    for (int i = 0; i < points.length; i++)
                        points[i] = exe.getGamePoints().getPointValue(i);
                    best = exe.getGameSettings().getBestResultsCounted();
                }
                tab = new cfevolution.gui.gamefiles.ChampionshipSaveTab(cfevolution.data.gamefiles.ChampionshipSave.load(file),
                                                                        path, this, points, best);
            }
            else
                tab = new cfevolution.gui.gamefiles.SetupFileTab(cfevolution.data.gamefiles.SetupFile.load(file), path, this);
            addTab(tab);
            return true;
        }
        catch (java.io.IOException e)
        {
            JOptionPane.showMessageDialog(this, file.getName() + " could not be opened. It may be damaged.",
                "File Not Opened", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /** Writes the names in a game folder's GP.EXE (as saved) to a name file:
        the 1,480 bytes there are laid out exactly as a name file is. */
    public void writeNameFileFromGame(GameFolder game, File target) throws java.io.IOException
    {
        byte[] exe = java.nio.file.Files.readAllBytes(game.getExecutable().toPath());
        int build = Executable.identify(exe);
        if (build < 1 || build > 3)
            throw new java.io.IOException("GP.EXE could not be read");
        int at = new int[] { 180250, 180186, 180202 }[build - 1];   // the driver names, then teams and engines
        target.getParentFile().mkdirs();
        cfevolution.data.gamefiles.NameFile.fromGame(java.util.Arrays.copyOfRange(exe, at, at + 1480)).save(target);
    }

    /** Breaks a message into lines of about {@code width} characters. */
    private static String wrap(String text, int width)
    {
        StringBuilder out = new StringBuilder();
        for (String paragraph : text.split("\n", -1))
        {
            int line = 0;
            for (String word : paragraph.split(" "))
            {
                if (line > 0 && line + word.length() > width)
                {
                    out.append('\n');
                    line = 0;
                }
                else if (line > 0)
                {
                    out.append(' ');
                    line++;
                }
                out.append(word);
                line += word.length();
            }
            out.append('\n');
        }
        return out.toString().trim();
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

    /** Opens what is dropped on the window: a folder as the game folder, an
        .EXE as the game folder it is in, anything else as a track. */
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
                    if (f.isDirectory())
                        openGameFolder(f.getPath());
                    else if (f.getName().toLowerCase().endsWith(".exe"))
                        openGameFolder(f.getAbsoluteFile().getParent());
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
