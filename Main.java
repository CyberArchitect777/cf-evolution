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
 * Main.java
 *
 * Created on 22 February 2005, 22:23
 */

package chequeredflag;

import chequeredflag.gui.*;

import javax.swing.*;
import java.util.prefs.Preferences;

/**
 *
 * @author barrie
 */
public class Main 
{
    
    private MainGUI mainScreen;
    
    /** Creates a new instance of Main */
    public Main() 
    {
        Preferences userData = Preferences.userRoot();
        Preferences cheqFlagData = userData.node("chequeredflag");
        String interfaceType = cheqFlagData.get("CFUIMode", "System Default");
        mainScreen = new MainGUI();
        ThemeManager userInterfaceTheme = new ThemeManager();
        userInterfaceTheme.setUserInterfaceType(interfaceType, mainScreen);        
    }
       
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
        Main CheqFlag = new Main();
    }
    
}
