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
 * CFDataObject.java
 *
 * Created on 19. Februar 2005, 22:11
 *
 * Serves as a base class for ChequeredFlag (CF) data classes.
 * Provides services for reading and writing of 16 and 32bit values (int, long).
 */

package chequeredflag.data;

import java.io.*;

/**
 *
 * @author Klaus
 */
public class CFDataObject {

    /** Creates a new instance of CFDataObject */
    public CFDataObject() {
    }

    // reading data
    public int loadInt( FileInputStream fis ) throws IOException
    {
        // Convert to short for correct creation of negative values
        return (short) (fis.read() + fis.read() * 256);
    }

    public long loadLong( FileInputStream fis ) throws IOException
    {
        return fis.read() + ( fis.read() << 8 ) + ( fis.read() << 16 ) + ( fis.read() << 24 );
    }

    // writing data
    public int write( FileOutputStream fos, int nValue ) throws IOException
    {
        fos.write( nValue & 0x0FF );
        fos.write( ( nValue >> 8 ) & 0x0FF );
        return 2; // 2 bytes written
    }

    public int write( FileOutputStream fos, long lValue ) throws IOException
    {
        fos.write( (int) lValue & 0x0FF );
        fos.write( (int) ( lValue >> 8 ) & 0x0FF );
        fos.write( (int) ( lValue >> 16 ) & 0x0FF );
        fos.write( (int) ( lValue >> 24 ) & 0x0FF );
        return 4; // 4 bytes written
    }
}
