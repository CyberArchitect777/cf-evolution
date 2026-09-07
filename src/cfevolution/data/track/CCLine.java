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
 * CCLine.java
 *
 * Created on 17. Februar 2005, 23:46
 */

package cfevolution.data.track;

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

    public int getCumTlu() {
        int total = 0;
        for (Enumeration e = elements(); e.hasMoreElements(); )
            total += ((CCLineSegment) e.nextElement()).getTlu();
        return total;
    }

    /** The game builds a table of AI coaching data from the best line, one
        entry per qualifying sector, and that table holds only 64. It writes
        the entries first and checks the count afterwards, so a line with too
        many sectors overruns it and the game HANGS on loading the track —
        no error, no crash, just a frozen circuit preview.

        Only curved sectors count. A straight makes no entry, a wide (0x40)
        sector makes no entry, and a curve outside the game's own radius
        window makes no entry. The radius here is the RAW stored word, which
        is getRadius() * 128.

        Established 2026-09-06: the rule is the F1GP-SDL project's reading of
        the game, and reproduces their per-circuit counts on all 16 originals
        exactly. The originals sit at 18-36 against the limit; a generated
        line reached 66. See CLAUDE.md, "A malformed track HANGS the game". */
    public int getCoachingEntryCount() {
        int nEntries = 0;
        for (Enumeration e = elements(); e.hasMoreElements(); ) {
            if (countsTowardsCoachingTable((CCLineSegment) e.nextElement()))
                nEntries++;
        }
        return nEntries;
    }

    /** Whether one sector writes an entry into that table. */
    public static boolean countsTowardsCoachingTable(CCLineSegment seg) {
        int nType = seg.getType();
        if (nType == 0x40)
            return false;                       // wide (32-bit radius) sectors are skipped outright
        // 0x80 is the first sector and carries an extra leading parameter.
        int nRaw = (short) seg.getParam(nType == 0x80 ? 2 : 1);
        if (nRaw == 0)
            return false;                       // a straight
        // The game divides a constant by the radius and skips the sector if
        // that overflows a 16-bit result or comes out under 60 — i.e. if the
        // curve is either impossibly tight or too gentle to be worth coaching.
        long lValue = COACHING_DIVIDEND / Math.abs((long) nRaw);
        return lValue <= 0xFFFF && lValue >= COACHING_MIN_VALUE;
    }

    /** The constant the game divides by the raw radius (0x0014_5F30). */
    private static final long COACHING_DIVIDEND = 1335088L;
    /** Below this the sector is too gently curved to earn an entry. */
    private static final long COACHING_MIN_VALUE = 60L;

    /** The game's coaching table holds 64 entries. We stay below that with
        margin: measured against the real game across 12 generated tracks, the
        three that hung counted 58, 62 and 66 while the nine that loaded
        counted 56 or fewer — so the effective ceiling is lower than 64 by a
        constant 6-8 that is not yet explained (the routine runs twice per
        compile, and the pit lane is the likeliest second source). 54 sits
        clear of the highest count that has ever been observed to load. */
    public static final int MAX_COACHING_ENTRIES = 54;

    /** instance data members */
    protected int m_nCumTlu;
}
