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
 * TrackSegment.java
 *
 * Created on 16. Februar 2005, 22:56
 */

package chequeredflag.data.track;

import java.io.*;
import java.util.*;
import chequeredflag.data.*;

/**
 *
 * @author Klaus
 */
public class TrackSegment extends CFDataObject {

    /** Creates a new instance of TrackSegment */
          
    public TrackSegment() {
        m_nType = 0;
        m_nTlu = 0;
        m_nCurvature = 0;
        m_nFlags = 0;
        m_nHeightChange = 0;
        m_nFenceDistL = 0;
        m_nFenceDistR = 0;
        m_nSign = 0;
        m_Commands = new Vector();
        m_dPosXStart = 0.0;
        m_dPosYStart = 0.0;
        m_dPosXEnd = 0.0;
        m_dPosYEnd = 0.0;
        m_dPosXCenter = 0.0;
        m_dPosYCenter = 0.0;
        m_dPosXMid = 0.0;
        m_dPosYMid = 0.0;
        m_dRadius = 0.0;
        m_nAngleStart = 0;
        m_nAngleEnd = 0;
        m_nWidthStart = 0;
        m_nWidthEnd = 0;
        m_nWidthChangeLength = 0;
        m_nWidthChangeEnd = 0;        
    }

    public void load( FileInputStream fis )
    {
        // Read a track segment from track file.
        // Structure in file: 0..n commands, followed by track segment layout data.
        try
        {
            // determine type of next object
            int nTlu = fis.read();
            int nType = fis.read();
            while ( ( nType != 0 ) && ( nType != 0xFF ) )
            {
                // This is a command. Read in and store into list
                Command cmd = new Command(nType, nTlu);
                cmd.load(fis);
                m_Commands.add( cmd );
                // determine type of next object
                nTlu = fis.read();
                nType = fis.read();
            }
            // All commands read. Now fill in track layout data.
            m_nType = nType;
            if ( nType == 0 )
            {
                // normal track segment
                m_nTlu          = nTlu;
                m_nCurvature    = loadInt( fis );	// SDI: start angle Z
                m_nHeightChange = loadInt( fis );	// SDI: start angle X
                m_nFlags        = loadInt( fis );
                m_nFenceDistR   = fis.read();
                m_nFenceDistL   = fis.read();
            }
            else if ( nType == 0xFF )
            {
                // End of list reached
                // There is nothing more to read from the file.
                // Nonetheless this segment may carry command information that
                // that was already gathered above.
                m_nSign = 4; // this indicates the "empty" track segment
            }
        }
        catch( IOException ioe )
        {
        }
    }

    public int getSign()
    {
        return m_nSign;
    }

    public int save( FileOutputStream fos ) throws IOException
    {
        int nWritten = 0;
        // first, save the command list.
        for ( Enumeration e = m_Commands.elements(); e.hasMoreElements(); )
            nWritten += ((Command) e.nextElement()).save(fos);
        // save own properties
        if (m_nType != 0xFF)
        {
            // Standard track segment
            fos.write( m_nTlu );
            fos.write( m_nType );
            if ( m_nSign < 0 )
                write( fos, 0 - m_nCurvature ); // store negative value
            else
                write( fos, m_nCurvature );
            write( fos, m_nHeightChange );
            write( fos, m_nFlags );
            // FenceDistR/L only one byte
            fos.write( m_nFenceDistR );
            fos.write( m_nFenceDistL );
        }

        return nWritten;
    }

    // calculate coordinates and angles depending on start values.
    public void calculateLayout(double dPosX, double dPosY, int nWidthStart, int nAngleStart, int nWidthChangeLength, int nWidthChangeEnd) {
        double dAngle;
        double ANGLE_SCALE = (2 * Math.PI) / 65536;

        m_dPosXStart = dPosX;
        m_dPosYStart = dPosY;
        m_nAngleStart = nAngleStart;
        m_nWidthStart = nWidthStart;

        // resulting direction
        m_nAngleEnd = m_nAngleStart - m_nTlu * m_nCurvature;

        // end coordinates
        dAngle = m_nAngleStart * ANGLE_SCALE;
        if ( m_nCurvature == 0 )
        {
            // Straight
            m_dPosXEnd = m_dPosXStart - (double) m_nTlu * Math.sin( dAngle );
            m_dPosYEnd = m_dPosYStart + (double) m_nTlu * Math.cos( dAngle );
        }
        else
        {
            // Curve: first calculate center of circle
            m_dRadius = 1 / (m_nCurvature * ANGLE_SCALE);
            m_dPosXCenter = m_dPosXStart + m_dRadius * Math.cos( dAngle );
            m_dPosYCenter = m_dPosYStart + m_dRadius * Math.sin( dAngle );
            // from center, calculate end point
            dAngle = m_nAngleEnd * ANGLE_SCALE;
            m_dPosXEnd = m_dPosXCenter - m_dRadius * Math.cos( dAngle );
            m_dPosYEnd = m_dPosYCenter - m_dRadius * Math.sin( dAngle );
        }

        // examine commands for track width change
        Command cmd = findCommand( 0x85 );
        if ( cmd != null )
        {
            nWidthChangeLength = cmd.getParam(1);
            nWidthChangeEnd    = cmd.getParam(2); // width on each side of middle, in width scales = 0.0047625 meters
        }
        // track width change detected?
	if (nWidthChangeLength > 0)
	{
            if (nWidthChangeLength > m_nTlu)
            {
                // width change to be continued on next segment
                // calculate width at end of segment
		m_nWidthEnd = m_nWidthStart + m_nTlu * (nWidthChangeEnd - m_nWidthStart) / nWidthChangeLength;
                // store values needed by next segment
                m_nWidthChangeEnd = nWidthChangeEnd;
                m_nWidthChangeLength = nWidthChangeLength - m_nTlu;
		// need flag to indicate width change ends in middle of segment? @@@
	    }
            else
            {
                // whole change takes place on this segment
		if( nWidthChangeLength < m_nTlu)
                {
                    // change complete in the middle of the segment.
                    // calculate point where the change ends.
                    if(m_nCurvature != 0)
                    {
                        // curved segment
                        double dAngleMid = m_nAngleStart + nWidthChangeLength * m_nCurvature;
			m_dPosXMid = m_dPosXCenter - m_dRadius * Math.cos(dAngleMid * ANGLE_SCALE);
			m_dPosYMid = m_dPosYCenter + m_dRadius * Math.sin(dAngleMid * ANGLE_SCALE);
                    }
                    else
                    {
                        // straight
			m_dPosXMid = m_dPosXStart - nWidthChangeLength * Math.sin(m_nAngleStart * ANGLE_SCALE);
			m_dPosYMid = m_dPosYStart + nWidthChangeLength * Math.cos(m_nAngleStart * ANGLE_SCALE);
                    }
		}
                m_nWidthEnd = nWidthChangeEnd;
                // nothing to do on next segment
                m_nWidthChangeLength = 0;
                m_nWidthChangeEnd = 0;
            }
        }
        else
        {
            // No width change
            m_nWidthEnd = m_nWidthStart;
            // nothing to do on next segment
            m_nWidthChangeLength = 0;
            m_nWidthChangeEnd = 0;
        }
    }

    public Command findCommand(int nType) {
        for ( Enumeration e = m_Commands.elements(); e.hasMoreElements(); )
        {
            Command cmd = (Command) e.nextElement();
            if ( cmd.getType() == nType )
                return cmd;
        }
        // did not find a command of desired type
        return null;
    }

    /** methods for access to internal data */
    public int getType()
    { return m_nType; }
    public void setType( int nType )
    { m_nType = nType; }

    public int getTlu()
    { return m_nTlu; }
    public void setTlu( int nTlu )
    { m_nTlu = nTlu; }

    public int getCurvature()
    { return m_nCurvature; }
    public void setCurvature( int nCurvature )
    { m_nCurvature = nCurvature; }

    public int getHeightChange()
    { return m_nHeightChange; }
    public void setHeightChange( int nHeightChange )
    { m_nHeightChange = nHeightChange; }

    public int getFenceDistL()
    { return m_nFenceDistL; }
    public void setFenceDistL( int nFenceDistL )
    { m_nFenceDistL = nFenceDistL; }

    public int getFenceDistR()
    { return m_nFenceDistR; }
    public void setFenceDistR( int nFenceDistR )
    { m_nFenceDistR = nFenceDistR; }

    public int getFlags()
    { return m_nFlags; }
    public void setFlags( int nFlags )
    { m_nFlags = nFlags; }

    // can only be read
    public Vector getCommands()
    { return m_Commands; }
    
    public void setCommands(Vector newCommands)
    {
        // Written by Barrie. New routine to allow the integration of new track commands.
        
        m_Commands = newCommands;
        
    }

    // all calculated members can only be read! Their value changes
    // when "calculateLayout" is called.
    public double getPosXStart()
    { return m_dPosXStart; }
    public double getPosXEnd()
    { return m_dPosXEnd; }

    public double getPosYStart()
    { return m_dPosYStart; }
    public double getPosYEnd()
    { return m_dPosYEnd; }

    public double getPosXCenter()
    { return m_dPosXCenter; }
    public double getPosYCenter()
    { return m_dPosYCenter; }

    public double getPosXMid()
    { return m_dPosXMid; }
    public double getPosYMid()
    { return m_dPosYMid; }

    public int getWidthChangeLength()
    { return m_nWidthChangeLength; }
    public int getWidthChangeEnd()
    { return m_nWidthChangeEnd; }

    public int getWidthStart()
    { return m_nWidthStart; }
    public int getWidthEnd()
    { return m_nWidthEnd; }

    public int getAngleStart()
    { return m_nAngleStart; }
    public int getAngleEnd()
    { return m_nAngleEnd; }

    public double getRadius()
    { return m_dRadius; }
    
    public void setDetailedFlags(String binaryCode)
    {
        // Implemented by Barrie. Takes an easier binary interpretion of flag values
        // and converts them to the native format used by the game before storing them.
        
        int currentTotal = 0;
        int currentBinaryValue = 32768;
        for (int x=0;x<16;x++)
        {
            char uniqueCode = binaryCode.charAt(x);
            if (uniqueCode == '1')
            {
                currentTotal = currentTotal + currentBinaryValue;
            }
            currentBinaryValue = currentBinaryValue / 2;            
        }
        
        setFlags(currentTotal);
        
    }
    
    public String getDetailedFlags()
    {
        // Implemented by Barrie. Outputs an easier interpretion of the flags variable
        
        StringBuffer binaryCode = new StringBuffer();
        int totalLeft = getFlags();
        int currentLimit = 32768;
        boolean stopFlag = false;
        while (stopFlag == false)
        {
            if (totalLeft >= currentLimit)
            {
                binaryCode.append("1");
                totalLeft=totalLeft-currentLimit;
            }
            else
            {
                binaryCode.append("0");
            }
            if (currentLimit > 1)
            {
                currentLimit=currentLimit/2;
            }
            else
            {
                stopFlag = true;
            }
        }        
        //return "1111111111111111"; //
        return binaryCode.toString();
    }

    // instance data members
    protected int m_nType;      // segment type
    protected int m_nSign;      // derived from Curvature, also used as flag
    protected int m_nTlu;       // length in Track Length Units
    protected int m_nCurvature; // higher values make sharper curves -> 1/r
    protected int m_nHeightChange; // Curvature in Z direction
    protected int m_nFlags;     // bit coded attributes like kerbs, fences etc.
    protected int m_nFenceDistL, m_nFenceDistR; // distance between track and fence
    protected Vector m_Commands; // List of command objects associated with track segment

    // calculated values
    protected double m_dPosXStart, m_dPosYStart;   // coordinates for start of segment
    protected double m_dPosXEnd, m_dPosYEnd;       // coordinates for end of segment
    protected double m_dPosXCenter, m_dPosYCenter; // coordinates for center of circle (curved segments)
    protected double m_dPosXMid, m_dPosYMid;       // coordinates for point where track width change ends
                                                   // when not same as end of segment.
    protected double m_dRadius;                    // radius of circle (middle of track)
    protected int m_nAngleStart, m_nAngleEnd;   // angle = direction at start and end of segment
    protected int m_nWidthStart, m_nWidthEnd;   // track width at start and end of segment
    protected int m_nWidthChangeLength, m_nWidthChangeEnd; // when track width changes across segment border,
                                                           // these members hold the values needed by the next segment.

}
