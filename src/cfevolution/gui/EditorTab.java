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

import javax.swing.JComponent;
import javax.swing.JMenu;

/**
 * One open file in the main window's tabs: a track or an executable.
 * The main window asks the active tab for its menus, toolbar buttons and
 * status text, and asks every tab before closing it.
 */
public interface EditorTab
{
    /** The component shown in the tab. */
    JComponent getComponent();

    /** Short name for the tab, normally the file name. */
    String getTabTitle();

    /** Full path of the open file, shown as the tab's tooltip. */
    String getFilePath();

    /** True when there are changes that have not been saved. */
    boolean isModified();

    /** Saves to the file it was opened from. Returns true on success. */
    boolean save();

    /** Saves to a new file. Returns true on success. */
    boolean saveAs();

    /** Menus this kind of file adds between File and Help. */
    JMenu[] getMenus();

    /** Buttons this kind of file adds to the main toolbar (null entries
        become separators). */
    JComponent[] getToolbarItems();

    /** Items this kind of file adds to the File menu, e.g. map export. */
    javax.swing.JMenuItem[] getFileMenuItems();

    /** One line describing the open file, for the status bar. */
    String getStatusText();
}
