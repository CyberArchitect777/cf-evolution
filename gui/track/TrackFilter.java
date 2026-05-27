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
 * TrackFilter.java
 *
 * Created on 18 March 2005, 23:36
 */

package chequeredflag.gui.track;

import javax.swing.filechooser.FileFilter;
import java.io.*;

/**
 *
 * @author barrie
 */
public class TrackFilter extends FileFilter
{
    
    /** Creates a new instance of TrackFilter */
    public TrackFilter() 
    {
        
    }
    
    public boolean accept(File trackFilter)
    {
        //return (trackFilter.getName().toLowerCase().startsWith("f1ct") && trackFilter.getName().toLowerCase().endsWith(".dat")) || trackFilter.isDirectory();
        return trackFilter.getName().toLowerCase().endsWith(".dat") || trackFilter.isDirectory();
    }
    
    public String getDescription()
    {
        return "F1GP/WC Track File";
    }
    
}
