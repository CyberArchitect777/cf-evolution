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
 * TrackDataHeader.java
 *
 * Created on 16. Februar 2005, 00:12
 */

package chequeredflag.data.track;

import java.io.*;
import chequeredflag.data.*;

/**
 *
 * @author Klaus
 */
public class TrackDataHeader extends CFDataObject {

    /** Creates a new instance of TrackDataHeader */
    public TrackDataHeader() {
        m_anKerbTopColor = new int[4];      // maximum number of 4 colors supported
        m_anKerbBottomColor = new int[4];
    }

    public void load( FileInputStream fis )
    {
        try {
            m_nStartAngle       = loadInt( fis );
            m_nStartHeight      = loadInt( fis );
            m_nStartX           = loadInt( fis );
            m_nStartZ           = loadInt( fis );
            m_nStartY           = loadInt( fis );
            m_nStartWidth       = loadInt( fis );
            m_nPoleWidth        = loadInt( fis );
            m_nPitSide          = fis.read();
            m_nTrSurround       = fis.read();
            m_nFenceDistR       = fis.read();
            m_nFenceDistL       = fis.read();
            m_nKerbCNum         = loadInt( fis );
            if ( m_nKerbCNum > 0 )
            {
                int nValue;
                for ( int i = 0; i < m_nKerbCNum; i++ )
                {
                    nValue = loadInt( fis );
                    if ( nValue == 8 )
                    {
                        // White part of kerb gets different bottom colors, depending
                        // on situation in game (special handling). No bottom color specified.
                        m_anKerbTopColor[ i ] = 8;
                        m_anKerbBottomColor[ i ] = -1;
                    }
                    else
                    {
                        // Not white: store top colour and load bottom color from file
                        m_anKerbTopColor[ i ] = nValue;
                        m_anKerbBottomColor[ i ] = loadInt( fis );
                    }
                }
            }

            // other values calculated from those read from file
            m_dWidth = m_nStartWidth * 2 * Track.s_dWIDTHSCALE;
        }
        catch( IOException ioe )
        {
        }
    }

    public int save( FileOutputStream fos ) throws IOException
    {
        // double-byte values
        write( fos, m_nStartAngle );
        write( fos, m_nStartHeight );
        write( fos, m_nStartX );
        write( fos, m_nStartZ );
        write( fos, m_nStartY );
        write( fos, m_nStartWidth );
        write( fos, m_nPoleWidth );
        // single-byte values
        fos.write( m_nPitSide );
        fos.write( m_nTrSurround );
        fos.write( m_nFenceDistR );
        fos.write( m_nFenceDistL );
        // double-byte values, again
        write( fos, m_nKerbCNum );
        // 20 Bytes written so far
        int nResult = 20;
        for ( int i = 0; i < m_nKerbCNum; i++ )
        {
            // Always write top color
            write( fos, m_anKerbTopColor[ i ] );
            nResult += 2;
            if ( m_anKerbTopColor[ i ] != 8 )
            {
                // not standard white: also write bottom color
                write( fos, m_anKerbBottomColor[ i ] );
                nResult += 2;
            }
        }

/*
        write( fos, m_nUnk1 );
        write( fos, m_nKerbTopColor );
        write( fos, m_nUnk2 );
        write( fos, m_nKerbBottomColor );
        // 28 Bytes written so far
        int nResult = 28;
        if ( m_nKerbCNum == 4 )
        {
            // write two more colours
            write( fos, m_nKerbTopColor2 );
            write( fos, m_nKerbBottomColor2 );
            nResult += 4; // 4 more bytes written
        }
*/
        return nResult;
    }
    
    protected int m_nStartAngle;
    protected int m_nStartHeight;
    protected int m_nStartX, m_nStartY, m_nStartZ;
    protected int m_nStartWidth;
    protected int m_nPoleWidth;
    protected int m_nPitSide;
    protected int m_nTrSurround;
    protected int m_nFenceDistR, m_nFenceDistL;
    protected int m_nKerbCNum;
    protected int m_nUnk1, m_nUnk2; // @@@ to be replaced by m_anKerbTop/BottomColor
    protected int m_nKerbTopColor, m_nKerbBottomColor;  // @@@ to be replaced by m_anKerbTop/BottomColor
    protected int m_nKerbTopColor2, m_nKerbBottomColor2; // @@@ to be replaced by m_anKerbTop/BottomColor
    protected int m_anKerbTopColor[];
    protected int m_anKerbBottomColor[];

    // calculated values
    protected double m_dWidth;
    
    // Unknown1 in fact is the first kerb color (8 = white)
    public void setUnknown1(int nColorIndex)
    {
        // Only for compatibility, will be removed soon (19.12.2005)
        m_anKerbTopColor[ 0 ] = nColorIndex;
        if ( nColorIndex == 8 )
            m_anKerbBottomColor[ 0 ] = -1; // invalid
        else
            // copy value to bottom color, too
            m_anKerbBottomColor[ 0 ] = nColorIndex;
    }
    
    // Unknown2 is the second kerb color (8 = white in all original tracks)
    public void setUnknown2(int nColorIndex)
    {
        // Only for compatibility, will be removed soon (19.12.2005)
        m_anKerbTopColor[ 2 ] = nColorIndex;
        if ( nColorIndex == 8 )
            m_anKerbBottomColor[ 2 ] = -1; // invalid
        else
            // copy value to bottom color, too
            m_anKerbBottomColor[ 2 ] = nColorIndex;
    }
    
    public int getUnknown1()
    {
        // Only for compatibility, now uses color arrays.
        return m_anKerbTopColor[ 0 ];
    }
    
    public int getUnknown2()
    {
        // Only for compatibility, now uses color arrays.
        return m_anKerbTopColor[ 2 ];
    }
    
    public void setTotalKerbColours(int totalKerbColours)
    {
        // Sets the total number of colours used for the kerbs
        
        m_nKerbCNum = totalKerbColours;
    }
    
    public int getTotalKerbColours()
    {
        // Return the total number of colours used for the kerbs
        
        return m_nKerbCNum;
    }
    
    public void setKerbColor(boolean upperSide, boolean firstKerb, int colorValue)
    {
        // to be removed. Now uses the new color arrays
        int nIndex;
        nIndex = 1;
        if ( !firstKerb )
            nIndex += 2;
        if ( upperSide )
            m_anKerbTopColor[ nIndex ] = colorValue;
        else
            m_anKerbBottomColor[ nIndex ] = colorValue;
    }
    
    public int getKerbColor(boolean upperSide, boolean firstKerb)
    {
        // to be removed. Now uses the new color arrays
        int nIndex;
        nIndex = 1;
        if ( !firstKerb )
            nIndex += 2;
        if ( upperSide )
            return m_anKerbTopColor[ nIndex ];
        else
            return m_anKerbBottomColor[ nIndex ];
    }
    
    public int getTrSurround()
    {
        // Return TrSurround data header (currently unknown)
        
        return m_nTrSurround;
    }
    
    public void setTrSurround(int trSurround)
    {
        // Sets TrSurround data header (currently unknown)
        
        m_nTrSurround = trSurround;
    }
    
    public int getStartHeight()
    {
        // Return starting height of first segment
        
        return m_nStartHeight;        
    }
    
    public void setStartHeight(int startingHeight)
    {
        // Sets the starting height of the first segment
        
        m_nStartHeight = startingHeight;        
    }
    
    public int getStartPos(int posIndex)
    {
        // Returns the X, Y or Z coordinates position of the start/finish line
        // Point corresponds to the track center position
        
        int posData = 0;
        
        switch (posIndex)
        {
            case 0: posData = m_nStartX; break;
            case 1: posData = m_nStartY; break;
            case 2: posData = m_nStartZ; break;
        }      
        
        return posData;        
    }
    
    public void setStartPos(int posIndex, int posValue)
    {
        // Sets the X, Y or Z coordinate position of the start/finish line
        // Point corresponds to the track center position
        
        switch (posIndex)
        {
            case 0: m_nStartX = posValue; break;
            case 1: m_nStartY = posValue; break;
            case 2: m_nStartZ = posValue; break;
        }
    }
    
    public int getPoleWidth()
    {
        // Returns the pole width (currently unknown setting)
        
        return m_nPoleWidth;
    }
    
    public void setPoleWidth(int poleWidth)
    {
        // Sets the pole width (currently unknown setting)
        
        m_nPoleWidth = poleWidth;
    }

    public int getStartWidth() {
        return m_nStartWidth;
    }
    
    public void setStartWidth(int startingWidth)
    {
        // Sets the starting width of the first segment
        
        m_nStartWidth = startingWidth;        
    }    

    public int getStartAngle() {
        return m_nStartAngle;
    }
    
    public void setStartAngle(int startingAngle)
    {
        // Sets the starting angle of the first segment
        
        m_nStartAngle = startingAngle;
    }
    

    public boolean getPitSide() {
        if ( m_nPitSide > 0  )
            return true;
        else
            return false;
    }
    
    public void setPitSide(boolean pitSide)
    {
        // Sets the side in which the pitlane exists on the track
        
        if (pitSide == false)
        {
            m_nPitSide = 0;
        }
        else
        {
            m_nPitSide = 1;
        }
    }
    
    public int getFenceDistL() {
        return m_nFenceDistL;
    }
    
    public void setFenceDistL(int fenceDistL)
    {
        // Sets the initial distance between the track and fence on the left side
        
        m_nFenceDistL = fenceDistL;
    }

    public int getFenceDistR() {
        return m_nFenceDistR;
    }
    
    public void setFenceDistR(int fenceDistR)
    {
        // Sets the initial distance between the track and fence on the right side
        
        m_nFenceDistR = fenceDistR;
        
    }
}
