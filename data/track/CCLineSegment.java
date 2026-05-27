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
 * CCLineSegment.java
 *
 * Created on 17. Februar 2005, 23:51
 */

package chequeredflag.data.track;

import java.io.*;
import chequeredflag.data.*;

/**
 *
 * @author Klaus
 */
public class CCLineSegment extends CFDataObject {

    /** Creates a new instance of CCLineSegment */
    public CCLineSegment(int nType) {
        m_nType = nType;
        m_nTlu = 0;
        // create always full length of parameter array (3)
        // BARRIE: Changed 3 to 4 to account for dimensioning error
        m_nParam = new int[ 4 ];

        // initialise calculated members
        m_bValid = true;
        m_dAngleStart = 0.0;
        m_dAngleEnd = 0.0;
        m_dLength = 0.0;
        m_dPosXCenter = 0.0;
        m_dPosYCenter = 0.0;
        m_dPosXStart = 0.0;
        m_dPosYStart = 0.0;
        m_dPosXEnd = 0.0;
        m_dPosYEnd = 0.0;
        m_dRadius = 0.0;
        m_segShift = null;
    }

    public void load( FileInputStream fis)
    {
        try
        {
            // All CCLine segments have at least two parameters
            m_nParam[ 0 ] = loadInt( fis );
            m_nParam[ 1 ] = loadInt( fis );
            // Rest depends on CCLine segment type
            if ( m_nType != 0 )
            {
                // Should be a type 0x80 (first segment) or 0x40 (32bit radius) segment
                m_nParam[ 2 ] = loadInt( fis );
            }
        }
        catch( IOException ioe )
        {
        }
    }

    // accessor methods for internal data
    public int getTlu()
    { return m_nTlu; }
    public void setTlu( int nTlu )
    { m_nTlu = nTlu; }

    public int getType()
    { return m_nType; }
    public void setType( int nType )
    { m_nType = nType; }

    public int getShift()
    {
        int nShift;
        if ( ( m_nType & 0x80 ) > 0 )
            nShift = m_nParam[1];
        else
            nShift = m_nParam[0];
        return nShift;
    }

    // Params are stored and retrieved by 0-based index
    public int getParam( int nIndex )
    { return m_nParam[ nIndex ]; }
    public void setParam( int nIndex, int nValue )
    { m_nParam[ nIndex ] = nValue; }

    // save object to disk
    public int save(FileOutputStream fos) throws IOException
    {
        int nBytesWritten = 0;
        // Write length and type
        fos.write( m_nTlu );
        fos.write( m_nType );
        nBytesWritten += 2;
        // All CCLine segments have at least two parameters
        write( fos, m_nParam[ 0 ] );
        write( fos, m_nParam[ 1 ] );
        nBytesWritten += 4;
        // Rest depends on CCLine segment type
        if ( m_nType != 0 )
        {
            // Should be a type 0x80 (first segment) or 0x40 (32bit radius) segment
            write( fos, m_nParam[ 2 ] );
            nBytesWritten += 2;
        }
        return nBytesWritten;
    }

    public double getRadius() {
        return m_dRadius;
    }

    public void setRadius(double dRadius) {
        m_dRadius = dRadius;
    }

    public double getPosXStart() {
        return m_dPosXStart;
    }

    public void setPosXStart(double dPosXStart) {
        m_dPosXStart = dPosXStart;
    }

    public double getPosYStart() {
        return m_dPosYStart;
    }

    public void setPosYStart(double dPosYStart) {
        m_dPosYStart = dPosYStart;
    }

    public double getPosXEnd() {
        return m_dPosXEnd;
    }

    public void setPosXEnd(double dPosXEnd) {
        m_dPosXEnd = dPosXEnd;
    }

    public double getPosYEnd() {
        return m_dPosYEnd;
    }

    public void setPosYEnd(double dPosYEnd) {
        m_dPosYEnd = dPosYEnd;
    }

    public double getPosXCenter() {
        return m_dPosXCenter;
    }

    public void setPosXCenter(double dPosXCenter) {
        m_dPosXCenter = dPosXCenter;
    }

    public double getPosYCenter() {
        return m_dPosYCenter;
    }

    public void setPosYCenter(double dPosYCenter) {
        m_dPosYCenter = dPosYCenter;
    }

    public double getAngleStart() {
        return m_dAngleStart;
    }

    public void setAngleStart(double dAngleStart ) {
        m_dAngleStart = dAngleStart;
    }

    public double getAngleEnd() {
        return m_dAngleEnd;
    }

    public void setAngleEnd(double dAngleEnd ) {
        m_dAngleEnd = dAngleEnd;
    }

    public double getLength() {
        return m_dLength;
    }

    public void setLength( double dLength ) {
        m_dLength = dLength;
    }

    public double getS() {
        return m_dS;
    }

    public void setS( double dS ) {
        m_dS = dS;
    }

    /** Returns true if segment is straight, else false.
        Decision is made upon params read from disk so it
        will always work, even when radius is not calculated
        yet.
    */
    public boolean isStraight() {
        switch ( m_nType )
        {
        case 0x00:
            // normal segment: radius in second param
            return m_nParam[ 1 ] == 0;

        case 0x80:
            // first segment in ccline: radius is in third param
            return m_nParam[ 2 ] == 0;

        case 0x40:
            // segment with 32 bit radius stored in second and third param.
            // Note: type 0x40 segment should never be straight because
            // 32bit radius makes no sense in this case.
            return (m_nParam[ 1 ] == 0 ) && ( m_nParam[ 2 ] == 0);
        }
        // When this point is reached, type of CCline segment is unknown...
        return true; // can't tell, but return something...
    }

    /** Returns true if segment turns to the right, else false.
    */
    public boolean turnsRight() {
        switch ( m_nType )
        {
        case 0x00:
            // normal segment: radius in second param
        case 0x40:
            // segment with 32 bit radius stored in second and third param.
            // most significant word stored in second param, so this
            // decides if turning left or right just as in normal segment
            if ( m_nParam[ 1 ] > 0 )
                return true;
            else
                return false;

        case 0x80:
            // first segment in ccline: radius is in third param
            if ( m_nParam[ 2 ] > 0 )
                return true;
            else
                return false;
        }
        // When this point is reached, type of CCline segment is unknown...
        return true; // can't tell, but return something...
    }

    /** Returns true if segment turns to the left, else false
    */

    public boolean turnsLeft() {
        if ( isStraight() )
            return false;
        return !turnsRight();
    }

    /** Segment can be shifted by a (small) straight segment.
        This is used to move a curved CCline segment by a fraction of
        a TLU along the driving direction.
    */
    public void setShiftSegment( CCLineSegment seg) {
        m_segShift = seg;
    }

    public CCLineSegment getShiftSegment() {
        return m_segShift;
    }

    /** Segment can be invalidated to be excluded from display in the track map.
    */
    public boolean isValid() {
        return m_bValid;
    }

    public void setValid( boolean bValid ) {
        m_bValid = bValid;
    }

    /** Calculate the radius from raw params, depending on segment type.
        Scale radius to tlu units and store internally.
        Returns radius.
    */
    // factor for converting radius units to tlu
    final static double s_dRADIUS_SCALE = 1 / 128.0;

    public double calculateRadius() {
        double dRadius;
        if ( ( getType() & 0x40 ) != 0 )
        {
            // 32Bit radius: lower 16 bits is UNSIGNED!
            int nLower = getParam( 2 );
            if ( nLower < 0 )
                // make it an unsigned 16 bit value
                nLower = nLower & 0x0FFFF;
            dRadius = getParam( 1 ) << 16 + nLower;
        }
        else if ( ( getType() & 0x80 ) != 0 )
        {
            // Type 0x80 segment (first in list) carries radius in param 2
            dRadius = getParam( 2 );
        }
        else
        {
            // normal type 0 segment
            dRadius = getParam( 1 );
        }
        // scale to tlu and store into segment.
        dRadius = dRadius * s_dRADIUS_SCALE;
        setRadius( dRadius );
        return dRadius;
    }

    // instance data members
    protected int m_nType, m_nTlu;
    protected int m_nParam[];

    // calculated values
    private double m_dRadius;
    private double m_dPosXStart, m_dPosYStart;
    private double m_dPosXEnd, m_dPosYEnd;
    private double m_dPosXCenter, m_dPosYCenter;
    private double m_dAngleStart, m_dAngleEnd;
    private double m_dLength, m_dS; // not exactly sure if we need this (KS 12 Sep 06)
    private CCLineSegment m_segShift;
    private boolean m_bValid;
}
