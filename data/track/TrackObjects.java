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
 * TrackObjects.java
 *
 * Created on 14. Februar 2005, 23:14
 */

package chequeredflag.data.track;

import java.io.*;
import chequeredflag.data.*;

/**
 *
 * @author Klaus
 */
public class TrackObjects extends CFDataObject {

    /** Creates a new instance of TrackObjects */
    public TrackObjects() {
    }

    public void load(FileInputStream fis, int nSize)
    {
        try {
            // read number of objects
            m_nNumObj = loadInt( fis );
            // create object storage
            m_baData = new byte[ nSize ];
            // read from file
            fis.read(m_baData);
        }
        catch( IOException ioe)
        {
        }
    }

    public int save( FileOutputStream fos ) throws IOException
    {
        // write number of objects
        write( fos, m_nNumObj );
        // write objects
        fos.write( m_baData );
        // return number of written bytes
        return 2 + m_baData.length;
    }

    // data members
    protected byte m_baData[];
    protected int m_nNumObj;
}
