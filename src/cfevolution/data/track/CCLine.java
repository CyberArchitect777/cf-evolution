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

    /** The OLD calibrated limit, from before the pit lane's share could be
        computed. Kept only as the fallback for a track with no pit lane to
        measure and for harnesses that compare against it. Every production
        path now uses the exact per-track budget,
        Track.getCoachingEntryBudget(). */
    public static final int MAX_COACHING_ENTRIES = 54;

    /** Slots in the game's coaching table (unk_21663). The track pass and the
        pit pass fill it with one shared index, and the game hangs when that
        index reaches this value — so track + pit entries must be at most 63.
        Confirmed in the game 2026-10-10: a total of 63 loads, 64 hangs, and
        the rule separates all twelve load-tested generated tracks. */
    public static final int COACHING_TABLE_SIZE = 64;

    /** The parking-zone angles GP.EXE writes into the pit lane's
        wCCLineRAngle at anchor+23 .. anchor+34 BEFORE the pit pass
        (TCModPitlaneParkZoneCCLineRAngle, table at seg003:0866h). Not in the
        track file. They always add exactly two pit entries. */
    private static final int[] PIT_PARKING_ANGLES =
        { 0, 0, 0, 12000, 18000, 24000, -4000, -8000, -12000, -16000, 0, 0 };

    /** Distance in Segs from the 0x88/0x89 parking command to the first
        parking-zone angle. */
    private static final int PIT_PARKING_OFFSET = 23;

    /** How many coaching-table entries the game's PIT pass makes.

        anHeading holds each pit Seg's wAngleZ; nParkingAnchor is the pit Seg
        the 0x88/0x89 command stands at, or -1 if there is none. The rule is
        the F1GP-SDL project's reading of TCGenerateCCCoachingData's pit
        branch, reproduced here on all 16 originals: walk from Seg 1, find a
        run of equal heading steps, make one entry if the step is at least
        60, then skip the run AND the step after it. Seg 0 and the last Seg
        are never read. Independent of the best line. */
    public static int countPitPassEntries(int[] anHeading, int nParkingAnchor) {
        int n = anHeading.length;
        int[] h = new int[n];
        for (int i = 0; i < n; i++)
            h[i] = anHeading[i];
        if (nParkingAnchor >= 0) {
            for (int k = 0; k < PIT_PARKING_ANGLES.length; k++) {
                int i = nParkingAnchor + PIT_PARKING_OFFSET + k;
                if (i < n)
                    h[i] = (short) (h[i] + PIT_PARKING_ANGLES[k]);
            }
        }
        int nEnd = n - 2, j = 1, nEntries = 0;
        while (j < nEnd) {
            int nStep = (short) (h[j + 1] - h[j]), m = 1;
            while (j + m < nEnd && (short) (h[j + m + 1] - h[j + m]) == nStep)
                m++;
            if (Math.abs(nStep) >= COACHING_MIN_VALUE)
                nEntries++;
            j += m + 1;
        }
        return nEntries;
    }

    /** instance data members */
    protected int m_nCumTlu;
}
