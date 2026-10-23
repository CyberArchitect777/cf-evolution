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
 * Main.java
 *
 * Created on 22 February 2005, 22:23
 */

package cfevolution;

import cfevolution.gui.*;

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
        Preferences cheqFlagData = userData.node("cfevolution");
        // The look is applied before the window is built, so nothing has
        // to be redrawn in a second look straight after start-up.
        String interfaceType = cheqFlagData.get(ThemeManager.PREF_KEY, "CF Evolution");
        new ThemeManager().setUserInterfaceType(interfaceType, null);
        mainScreen = new MainGUI();
        mainScreen.setVisible(true);
    }
       
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { new Main(); }
        });
    }
    
}
