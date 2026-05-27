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
 * CCLine.java
 *
 * Created on 17. Februar 2005, 23:46
 */

package chequeredflag.data.track;

import java.io.*;
import java.util.*;


/**
 *
 * @author Klaus
 */
public class CCLine extends Vector {

    /** Creates a new instance of CCLine */
    public CCLine() {
    }

    public void load( FileInputStream fis)
    {
        int nType;
        int nTlu;
        // initialize cumulated length
        m_nCumTlu = 0;
        try {
            nTlu = fis.read();
            nType = fis.read();
            while ( nTlu != 0 )
            {
                // Create CCLine segment
                CCLineSegment seg = new CCLineSegment( nType );
                // transfer length
                seg.setTlu( nTlu );
                // add tlu value to cumulated length
                m_nCumTlu += nTlu;
                // load other data from file
                seg.load( fis );
                // Store segment in list
                add(seg);
                // read next type/tlu data
                nTlu = fis.read();
                nType = fis.read();
            }
        }
        catch( IOException ioe )
        {
        }
    }

    public int save(FileOutputStream fos) throws IOException
    {
        int nBytesWritten = 0;
        for( Enumeration e = elements(); e.hasMoreElements(); )
        {
            nBytesWritten += ((CCLineSegment) e.nextElement()).save(fos);
        }
        // save end of list pattern
        fos.write( 0 ); // Type
        fos.write( 0 ); // Tlu
        nBytesWritten += 2;
        return nBytesWritten;
    }

    // retrieves CCLineSegment by 1-based index
    public CCLineSegment getAt( int nIndex )
    {
        if ( ( nIndex > elementCount ) || ( nIndex < 1 ) )
            return null;
        else
            //return (CCLineSegment) elementAt( nIndex + 1 );
            // Code bug found above by barrie. Suspected line is below
            return (CCLineSegment) elementAt( nIndex - 1);
    }

    /** inserts new CCline segment at given index (1-based).
        returns newly created segment. */
    public CCLineSegment insertAt( int i )
    {
        CCLineSegment newSeg;
        // creating a straight segment of length 1.
        newSeg = new CCLineSegment( 0 );
        newSeg.m_nTlu = 1;
        if ( i > elementCount )
            add( newSeg );
        else
        {
            try {
                add( i - 1, newSeg );
            }
            catch ( ArrayIndexOutOfBoundsException e )
            {
                newSeg = null;
            }
        }
        //return null;
        // Code bug found above by barrie. Suspected line is below
        return newSeg;
    }

    /** delete segment at given position (1-based) */
    public void deleteAt(int i)
    {
        try {
            remove( i - 1 );
        }
        catch( ArrayIndexOutOfBoundsException e )
        {
        }
    }

    /** instance data members */
    protected int m_nCumTlu;
}
