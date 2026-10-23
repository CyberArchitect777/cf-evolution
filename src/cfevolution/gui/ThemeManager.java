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
 * ThemeManager.java
 *
 * Created on 10 December 2006, 23:43
 */

package cfevolution.gui;

import javax.swing.*;
import java.awt.Component;

/**
 *
 * @author barrie
 */
public class ThemeManager 
{
    
    /** Creates a new instance of ThemeManager */
    public ThemeManager() 
    {
        
    }
    
    public void setUserInterfaceType(String interfaceMode, java.awt.Component visualDisplay)
    {
        if (interfaceMode.compareTo("CF Evolution") == 0)
        {
            applyEditorTheme();
            if (visualDisplay != null) SwingUtilities.updateComponentTreeUI(visualDisplay);
        }
        else
        if (interfaceMode.compareTo("Metal") == 0)
        {
            try
            {
                javax.swing.plaf.metal.MetalLookAndFeel.setCurrentTheme(new javax.swing.plaf.metal.OceanTheme());
                UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
                if (visualDisplay != null) SwingUtilities.updateComponentTreeUI(visualDisplay);
            }
            catch (Exception exceptionError)
            {
                exceptionError.printStackTrace();
            }
        }
        else
        {
            if (interfaceMode.compareTo("Motif") == 0)
            {
                try
                {
                    UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
                    if (visualDisplay != null) SwingUtilities.updateComponentTreeUI(visualDisplay);
                }
                catch (Exception exceptionError)
                {
                    exceptionError.printStackTrace();
                }
            }
            else
            {
                if (interfaceMode.compareTo("System Default") == 0)
                {
                    try
                    {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                        if (visualDisplay != null) SwingUtilities.updateComponentTreeUI(visualDisplay);
                    }
                    catch (Exception exceptionError)
                    {
                        exceptionError.printStackTrace();
                    }
                }
                else
                {
                    if (interfaceMode.compareTo("Windows") == 0)
                    {
                        try
                        {
                            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
                            if (visualDisplay != null) SwingUtilities.updateComponentTreeUI(visualDisplay);                        
                        }
                        catch (Exception exceptionError)
                        {
                            exceptionError.printStackTrace();
                        }
                    }
                    else
                    {
                        if (interfaceMode.compareTo("GTK") == 0)
                        {
                            try
                            {
                                UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
                                if (visualDisplay != null) SwingUtilities.updateComponentTreeUI(visualDisplay);                        
                            }
                            catch (Exception exceptionError)
                            {
                                exceptionError.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }
    

    /** Preference holding the chosen look. A new key rather than the old
        "CFUIMode", so existing installs start on the editor's own look. */
    public static final String PREF_KEY = "CFTheme";

    /** Accent colour used by the editor's own components (selection,
        welcome page buttons, banners). */
    public static final java.awt.Color ACCENT = new java.awt.Color(0x2F6FBF);

    /** The editor's default look: Metal with flat, quiet colours and
        plain fonts, so it looks current without an extra library. */
    public static void applyEditorTheme()
    {
        try
        {
            UIManager.put("swing.boldMetal", Boolean.FALSE);
            javax.swing.plaf.metal.MetalLookAndFeel.setCurrentTheme(new EditorMetalTheme());
            UIManager.setLookAndFeel(new javax.swing.plaf.metal.MetalLookAndFeel());
            java.awt.Color line = new java.awt.Color(0xD5D9DF);
            UIManager.put("Table.rowHeight", 22);
            UIManager.put("Tree.rowHeight", 22);
            UIManager.put("Table.gridColor", new java.awt.Color(0xE6E8EC));
            UIManager.put("Table.showGrid", Boolean.TRUE);
            UIManager.put("TableHeader.cellBorder", BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 1, line),
                BorderFactory.createEmptyBorder(3, 6, 3, 6)));
            UIManager.put("SplitPane.dividerSize", 6);
            UIManager.put("SplitPane.border", BorderFactory.createEmptyBorder());
            UIManager.put("SplitPaneDivider.border", BorderFactory.createEmptyBorder());
            UIManager.put("ScrollPane.border", BorderFactory.createLineBorder(line));
            UIManager.put("TabbedPane.contentBorderInsets", new java.awt.Insets(1, 0, 0, 0));
            UIManager.put("TabbedPane.tabInsets", new java.awt.Insets(4, 10, 4, 6));
            UIManager.put("TabbedPane.selected", java.awt.Color.WHITE);
            UIManager.put("TabbedPane.borderHightlightColor", line);
            UIManager.put("ToolBar.border", BorderFactory.createEmptyBorder(3, 6, 3, 6));
            UIManager.put("Button.margin", new java.awt.Insets(3, 10, 3, 10));
            UIManager.put("ToolTip.background", new java.awt.Color(0xFFFFF0));
        }
        catch (Exception exceptionError)
        {
            exceptionError.printStackTrace();
        }
    }

    private static class EditorMetalTheme extends javax.swing.plaf.metal.DefaultMetalTheme
    {
        private final javax.swing.plaf.ColorUIResource primary1 = new javax.swing.plaf.ColorUIResource(0x5A6B80);
        private final javax.swing.plaf.ColorUIResource primary2 = new javax.swing.plaf.ColorUIResource(0x9FB8D8);
        private final javax.swing.plaf.ColorUIResource primary3 = new javax.swing.plaf.ColorUIResource(0xCFE0F5);
        private final javax.swing.plaf.ColorUIResource secondary1 = new javax.swing.plaf.ColorUIResource(0xA4ABB5);
        private final javax.swing.plaf.ColorUIResource secondary2 = new javax.swing.plaf.ColorUIResource(0xD5D9DF);
        private final javax.swing.plaf.ColorUIResource secondary3 = new javax.swing.plaf.ColorUIResource(0xF2F3F5);
        private final javax.swing.plaf.FontUIResource control =
            new javax.swing.plaf.FontUIResource("Dialog", java.awt.Font.PLAIN, 13);
        private final javax.swing.plaf.FontUIResource small =
            new javax.swing.plaf.FontUIResource("Dialog", java.awt.Font.PLAIN, 12);

        public String getName() { return "CF Evolution"; }
        protected javax.swing.plaf.ColorUIResource getPrimary1() { return primary1; }
        protected javax.swing.plaf.ColorUIResource getPrimary2() { return primary2; }
        protected javax.swing.plaf.ColorUIResource getPrimary3() { return primary3; }
        protected javax.swing.plaf.ColorUIResource getSecondary1() { return secondary1; }
        protected javax.swing.plaf.ColorUIResource getSecondary2() { return secondary2; }
        protected javax.swing.plaf.ColorUIResource getSecondary3() { return secondary3; }
        public javax.swing.plaf.FontUIResource getControlTextFont() { return control; }
        public javax.swing.plaf.FontUIResource getMenuTextFont() { return control; }
        public javax.swing.plaf.FontUIResource getUserTextFont() { return control; }
        public javax.swing.plaf.FontUIResource getSystemTextFont() { return control; }
        public javax.swing.plaf.FontUIResource getWindowTitleFont() { return control; }
        public javax.swing.plaf.FontUIResource getSubTextFont() { return small; }
    }
}
