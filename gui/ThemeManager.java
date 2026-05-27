/*
 * Chequered Flag: An editor for Formula One Grand Prix/World Circuit
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

package chequeredflag.gui;

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
    
    public void setUserInterfaceType(String interfaceMode, MainGUI visualDisplay)
    {
        if (interfaceMode.compareTo("Metal") == 0)
        {
            try
            {
                UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
                SwingUtilities.updateComponentTreeUI(visualDisplay);
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
                    SwingUtilities.updateComponentTreeUI(visualDisplay);
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
                        SwingUtilities.updateComponentTreeUI(visualDisplay);
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
                            SwingUtilities.updateComponentTreeUI(visualDisplay);                        
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
                                SwingUtilities.updateComponentTreeUI(visualDisplay);                        
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
    
}
