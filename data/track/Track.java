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
 * Track.java
 *
 * Created on 9. Februar 2005, 23:12
 */

package chequeredflag.data.track;

import java.io.*;
import java.nio.channels.*;
import java.util.*;
import java.awt.*;


import chequeredflag.data.f1gp.CosLookupTable;
import chequeredflag.data.f1gp.F1GPMath;

/**
 *
 * @author Klaus
 */
public class Track {

    /** Creates a new instance of Track */
    public Track() {
        m_baBackground = new byte[4096];
        m_FileHeader = new TrackFileHeader();
        m_Objects = new TrackObjects();
        m_DataHeader = new TrackDataHeader();
        m_TrackSegments = new TrackSegments();
        m_CCLine = new CCLine();
        m_CCSetup = new CCSetup();
        m_PitlaneSegments = new TrackSegments();
        m_Footer = new Footer();

        // In layout mode, Segs are not moved to make start and finish fit together.
        m_fLayoutMode = true;
    }

    public boolean load( File file )
    {
        
        // Modified by Barrie to return the outcome of the track loading process
        // Will return true if normal, false if loading resulted in an exception error
        
        boolean loadSuccess = true;
        
        long lPos; // Position in file
        FileChannel fc;
        try {
            // remember file where track was loaded from.
            m_File = file;

            // Create file input stream
            FileInputStream fis = new FileInputStream( file );
            // Get channel to be able to determine file position later,
            // mainly for debugging and reference purpose.
            fc = fis.getChannel();

            // read background picture
            fis.read(m_baBackground);

            // read file header
            m_FileHeader.load(fis);

            // objects
            m_Objects.load(fis, m_FileHeader.m_nTrackDataOffset);

            // track data header: track width, starting angle etc.
            m_DataHeader.load(fis);

            // track segment data
            m_TrackSegments.load(fis);

            // CCLine data
            m_CCLine.load(fis);

            // CCSetup data
            m_CCSetup.load(fis);

            // pitlane segment data
            m_PitlaneSegments.load(fis);
            // Always read in one additional segment, like TrackSegments.
            // Pitlane does not use this segment to carry commands, so drop it.
            m_PitlaneSegments.remove(m_PitlaneSegments.size() - 1);

            lPos = m_PitlaneSegments.size();
            lPos = fc.position();

            // footer, which means all the rest, mainly unknown data
            int nCount = m_Footer.load(fis);

            // 10 bytes before end of file is the number of laps for full race distance
            m_nLapNumIndex = nCount - 10;

            // Close input stream
            fis.close();
        }
        catch( Exception exceptionError ) // Modified to catch all exceptions
        {
            loadSuccess = false;
        };
        // do all necessary calculations
        
        if (loadSuccess == false)
        {
            return false;
        }
        else
        {
            calculateTrackLayout();
            calculateCCLine();
            return true;
        }
    }

    // Save back to file where it was loaded from
    public int save()
    {
        return save( m_File );
    }

    // Save to some, maybe new, file
    public int save( File file )
    {
        int nBytesWritten = 0;
        try
        {
            FileOutputStream fos = new FileOutputStream(file);

            // Background graphics
            fos.write(m_baBackground);
            nBytesWritten = m_baBackground.length;

            // save track file header. This will be modified later as we know the
            // correct offset values for the newly written file.
            nBytesWritten += m_FileHeader.save(fos);

            // objects
            nBytesWritten += m_Objects.save(fos);

            // track data header
            nBytesWritten += m_DataHeader.save(fos);

            // track segments
            nBytesWritten += m_TrackSegments.save(fos);

            // CCLine data
            nBytesWritten += m_CCLine.save(fos);

            // CCSetup data
            nBytesWritten += m_CCSetup.save(fos);

            // pitlane segment data
            nBytesWritten += m_PitlaneSegments.save(fos);

            // Footer, which means all the rest, mainly unknown data.
            // Prior to writing, set checksum to 0. It will be recalculated later.
            m_Footer.setChecksum( 0 );
            nBytesWritten += m_Footer.save(fos);

            // new offset to checksum
            // determine position in file
            FileChannel fc = fos.getChannel();
            long lPos = fc.position();

            // Set checksum offset
            m_FileHeader.setChecksumOffset( lPos - 4 );

            // write offset also to file
            fc.position( 4104 ); // this is a FIXED place in the file
            m_FileHeader.saveChecksumOffset( fos );

            fos.close();

            // calculate new checksum
            calculateChecksum( file );
        }
        catch(IOException ioe)
        {
        }
        
        return nBytesWritten;
    }

    protected void calculateChecksum( File file )
    {
        try
        {
            // Open file for reading and writing
            RandomAccessFile raf = new RandomAccessFile( file, "rw" );

            try
            {
                // Get file length
                long lLength = raf.length();

                // Two checksums are calculated from the bytes of the file, except
                // the last 4 bytes which will hold the checksum.
                int nChecksum1 = 0, nChecksum2 = 0;
                int nChar;
                for ( long lPos = 0; lPos < lLength - 4; lPos++ )
                {
                    nChar = raf.read();
                    // First sum is a simple sum
                    nChecksum1 += nChar;
                    // Checksum 2: add char, then left rotate by 3
                    // can not directly rotate 16 bit value...
                    nChecksum2 = ((nChecksum2 << 3) & 0x0FFF8) + ((nChecksum2 >> 13) & 0x07);
                    nChecksum2 = nChecksum2 + nChar;
                }

                // write both checksums to file
                nChecksum1 &= 0x0FFFF;
                nChecksum2 &= 0x0FFFF;
                // must be stored low byte first, so writeShort will not work.
                raf.write( nChecksum1 & 0x0FF );
                raf.write( (nChecksum1 >> 8) & 0x0FF );
                raf.write( nChecksum2 & 0x0FF );
                raf.write( (nChecksum2 >> 8) & 0x0FF );

                // close the file
                raf.close();
            }
            catch( IOException ioe )
            {
            }
        }
        catch( FileNotFoundException fnfe )
        {
        }
    }

    /** Methods to get several parts of the track object */
    public TrackDataHeader getTrackDataHeader()
    {
        return m_DataHeader;
    }

    public TrackSegments getTrackSegments()
    {
        return m_TrackSegments;
    }

    public TrackSegments getPitlaneSegments()
    {
        return m_PitlaneSegments;
    }

    public CCLine getCCLine()
    {
        return m_CCLine;
    }

    public CCSetup getCCSetup()
    {
        return m_CCSetup;
    }

    public TrackObjects getTrackObjects()
    {
        return m_Objects;
    }

    public boolean getLayoutMode()
    {
        return m_fLayoutMode;
    }

    public boolean setLayoutMode( boolean fLayoutMode )
    {
        boolean fOldLayoutMode = m_fLayoutMode;
        m_fLayoutMode = fLayoutMode;
        return fLayoutMode;
    }

    /** Data members */
    protected File m_File;    // File where track was loaded from

    protected byte m_baBackground[];
    protected int m_nLapNumIndex;
    protected TrackFileHeader m_FileHeader;
    protected TrackObjects m_Objects;
    protected TrackDataHeader m_DataHeader;
    protected TrackSegments m_TrackSegments;
    protected CCLine m_CCLine;
    protected CCSetup m_CCSetup;
    protected TrackSegments m_PitlaneSegments;
    protected Footer m_Footer;

    protected boolean m_fLayoutMode;

    /** statics */
    // factor for converting width units in meters - probably fractions of feet
    final static double s_dWIDTHSCALE = 0.0047625;
    // factor for converting angle units in radians.
    final static double s_dANGLE_SCALE = (2 * Math.PI) / 65536;
    // factor for converting radius units to tlu
    final static double s_dRADIUS_SCALE = 1 / 128.0;

    public void calculateTrackLayout()
    {
        // Layout of the track segments.
        int nStartPosX, nStartPosY;
        nStartPosX = m_DataHeader.getStartPos( 0 ) << 3;
        nStartPosY = m_DataHeader.getStartPos( 1 ) << 3;
        m_TrackSegments.calculateTrackLayout(
            m_DataHeader.getStartWidth(),
            m_DataHeader.getStartAngle(),
            nStartPosX,
            nStartPosY, // start coordinates
            m_fLayoutMode
            );

        // Layout of the pit lane.
        // List of track segments is used to find
        // position and direction of pit entry.
        m_PitlaneSegments.calculatePitlaneLayout(
            m_TrackSegments, m_DataHeader.getPitSide(),
            m_fLayoutMode
            );
    };

    /**
        Intersect ccLine segment with end of track segment.
        Parameters: CCline segment
                    point and angle of track at end of CCLine segment.
        Calculate end point and angle for ccLineSegment and store into segment.
    */
    public void intersect( CCLineSegment ccLineSegment, double dTrackPosX, double dTrackPosY, double dTrackAngle )
    {
        double dRadius = ccLineSegment.getRadius();
        double dCClineAngle = ccLineSegment.getAngleStart();

        // Calculations based on code from Kristof Kaly-Kulai (DOS Track editor)
        if ( ccLineSegment.isStraight() )
        {
            ccLineSegment.setPosXCenter(0.0);
            ccLineSegment.setPosYCenter(0.0);
            // angle of segment can be modified by "shift" value
            ccLineSegment.setAngleStart( ccLineSegment.getAngleStart()
                                         - s_dANGLE_SCALE * ccLineSegment.getShift() );
            // straight segment can not be shifted in driving direction
            ccLineSegment.setShiftSegment( null );
            double dAux;
            dAux = Math.sin( ccLineSegment.getAngleStart() ) * Math.sin( dTrackAngle )
                   + Math.cos( ccLineSegment.getAngleStart() ) * Math.cos( dTrackAngle );
            if ( Math.abs(dAux) < 1e-10 )
            {
                ccLineSegment.setS( 100.0 ); // sense???
                ccLineSegment.setLength( ccLineSegment.getTlu() );
            }
            else
            {
                ccLineSegment.setLength( (Math.cos( dTrackAngle ) * (dTrackPosY - ccLineSegment.getPosYStart()))
                                         - (Math.sin( dTrackAngle ) * (dTrackPosX - ccLineSegment.getPosXStart())));
                ccLineSegment.setS( (-Math.sin( ccLineSegment.getAngleStart() ) * (dTrackPosY - ccLineSegment.getPosYStart())
                                     -Math.cos( ccLineSegment.getAngleStart() ) * (dTrackPosX - ccLineSegment.getPosXStart()))
                                    / dAux );
            }
            ccLineSegment.setAngleEnd( ccLineSegment.getAngleStart() );
            ccLineSegment.setPosXEnd( dTrackPosX + ccLineSegment.getS() * Math.cos( dTrackAngle ) );
            ccLineSegment.setPosYEnd( dTrackPosY + ccLineSegment.getS() * Math.sin( dTrackAngle ) );
        }
        else
        {
            int i;
            double dS = 0.0;

            // ccLineSegment is curved
            // Check if shift value is present
            if ( ccLineSegment.getShift() > 0 )
            {
                // Shift segment along driving direction. Insert small auxiliary
                // segment to fill the gap.
                CCLineSegment ccLineAux = new CCLineSegment(1); // always special type 1 (aux)
                // Shift segment is always straight
                ccLineAux.setRadius(0.0);
                ccLineAux.setPosXCenter(0.0);
                ccLineAux.setPosYCenter(0.0);
                // starts at starting position of current ccline segment
                ccLineAux.setPosXStart( ccLineSegment.getPosXStart());
                ccLineAux.setPosYStart( ccLineSegment.getPosYStart());
                ccLineAux.setAngleStart( ccLineSegment.getAngleStart() );
                ccLineAux.setAngleEnd( ccLineSegment.getAngleStart() );
                // length is calculated from shift value
                double dLength = ccLineSegment.getShift() / 256.0;
                ccLineSegment.setLength( dLength );
                ccLineSegment.setS( 0.0 );
                // calculate end point
                ccLineAux.setPosXEnd( ccLineAux.getPosXStart() - dLength * Math.sin( ccLineAux.getAngleStart() ));
                ccLineAux.setPosYEnd( ccLineAux.getPosYStart() + dLength * Math.cos( ccLineAux.getAngleStart() ));
                // store into current segment
                ccLineSegment.setShiftSegment( ccLineAux );
                // move current segment to end of shift segment
                ccLineSegment.setPosXStart( ccLineAux.getPosXEnd() );
                ccLineSegment.setPosYStart( ccLineAux.getPosYEnd() );
            }
            else
            {
                // no shift segment
                ccLineSegment.setShiftSegment( null );
            }

            // Calculate center of circle
            ccLineSegment.setPosXCenter( ccLineSegment.getPosXStart()
                                         + ccLineSegment.getRadius() * Math.cos( ccLineSegment.getAngleStart() ));
            ccLineSegment.setPosYCenter( ccLineSegment.getPosYStart()
                                         + ccLineSegment.getRadius() * Math.sin( ccLineSegment.getAngleStart() ));

            // calculate end points and angles
            double adAlfa[] = new double[ 2 ];  // variable names are terrible, but I cant figure out what they mean, so
                                                // I kept the names of the DOS version.
            double adS[] = new double[ 2 ];
            double dCotTrackAngle, dA, dC, dF, dF2;
            if ( Math.abs( Math.cos( dTrackAngle )) < Math.abs( Math.sin( dTrackAngle )) )
            {
                dCotTrackAngle = Math.cos( dTrackAngle ) / Math.sin( dTrackAngle );
                dA = 1.0 / Math.sqrt( 1 + dCotTrackAngle * dCotTrackAngle );
                dC = dA * ( dTrackPosX - ccLineSegment.getPosXCenter()
                            + dCotTrackAngle * (ccLineSegment.getPosYCenter() - dTrackPosY) )
                        / ccLineSegment.getRadius();
                dC = -dC; // negate sign
                dF = Math.atan2( -dA, dCotTrackAngle * dA );
                if ( Math.abs( dC ) > 1.0 )
                {
                    ccLineSegment.setS( 100.0 ); // @@@ sense of value S??
                    ccLineSegment.setAngleEnd( ccLineSegment.getAngleStart() );
                    ccLineSegment.setLength( 1.0 ); // @@@ sense of length??
                    ccLineSegment.setPosXEnd( ccLineSegment.getPosXStart() - Math.sin( ccLineSegment.getAngleStart()));
                    ccLineSegment.setPosYEnd( ccLineSegment.getPosYStart() + Math.cos( ccLineSegment.getAngleStart()));
                    // Segment and all following segments get "invalid" marker (?)
                    ccLineSegment.setValid(false);
                    return;
                }
                else
                {
                    dF2 = Math.asin( dC );
                }
                adAlfa[ 0 ] = -dF2 - dF;
                adS[ 0 ] = (ccLineSegment.getPosYCenter() - dTrackPosY
                           - ccLineSegment.getRadius() * Math.sin( adAlfa[ 0 ]))
                          / Math.sin( dTrackAngle );
                adAlfa[ 1 ] = Math.PI + dF2 - dF;
                adS[ 1 ] = (ccLineSegment.getPosYCenter() - dTrackPosY
                           - ccLineSegment.getRadius() * Math.sin( adAlfa[ 1 ]))
                          / Math.sin( dTrackAngle );
            }
            else
            {
                double dTanTrackAngle;
                dTanTrackAngle = Math.tan( dTrackAngle );
                dA = 1.0 / Math.sqrt( 1 + dTanTrackAngle * dTanTrackAngle );
                dC = dA * ( dTrackPosY - ccLineSegment.getPosYCenter()
                            + dTanTrackAngle * ( ccLineSegment.getPosXCenter() - dTrackPosX ))
                        / ccLineSegment.getRadius();
                dC = -dC; // negate sign
                dF = Math.atan2( dTanTrackAngle * dA, -dA );
                if ( Math.abs( dC ) > 1.0 )
                {
                    // ccLine left track???
                    ccLineSegment.setS( 100.0 );
                    ccLineSegment.setLength( 1.0 );
                    ccLineSegment.setAngleEnd( ccLineSegment.getAngleStart() );
                    ccLineSegment.setPosXEnd( ccLineSegment.getPosXStart() - Math.sin( ccLineSegment.getAngleStart() ));
                    ccLineSegment.setPosYEnd( ccLineSegment.getPosYStart() + Math.cos( ccLineSegment.getAngleStart() ));
                    // Mark segment and all followers as invalid (see above)
                    ccLineSegment.setValid(false);
                    return;
                }
                dF2 = Math.asin( dC );
                adAlfa[ 0 ] = -dF2 - dF;
                adS[ 0 ] = ( ccLineSegment.getPosXCenter() - dTrackPosX
                             - ccLineSegment.getRadius() * Math.cos( adAlfa[ 0 ] ))
                           / Math.cos( dTrackAngle );
                adAlfa[ 1 ] = Math.PI + dF2 - dF;
                adS[ 1 ] = ( ccLineSegment.getPosXCenter() - dTrackPosX
                             - ccLineSegment.getRadius() * Math.cos( adAlfa[ 1 ] ))
                           / Math.cos( dTrackAngle );
            }

            // Possibly modify angles
            for( i = 0; i < 2; i++ )
            {
                while( (adAlfa[ i ] - ccLineSegment.getAngleStart()) > (2.0 * Math.PI) )
                {
                    adAlfa[ i ] -= 2.0 * Math.PI;
                }
                while( (adAlfa[ i ] - ccLineSegment.getAngleStart()) <= (-2.0 * Math.PI) )
                {
                    adAlfa[ i ] += 2.0 * Math.PI;
                }
                if ( ccLineSegment.turnsRight() )
                {
                    while( -adAlfa[ i ] <= -ccLineSegment.getAngleStart() )
                    {
                        adAlfa[ i ] -= 2.0 * Math.PI;
                    }
                }
                else
                {
                    // turns left
                    while( adAlfa[ i ] <= ccLineSegment.getAngleStart() )
                    {
                        adAlfa[ i ] += 2.0 * Math.PI;
                    }
                }
            }
            
            if ( ((ccLineSegment.getPosYStart() - dTrackPosY) * Math.cos( dTrackAngle )
                   - (ccLineSegment.getPosXStart() - dTrackPosX) * Math.sin( dTrackAngle )) > 0.0 )
            {
                if ( (ccLineSegment.turnsRight() && ( -adAlfa[ 0 ] < -adAlfa[ 1 ] ))
                     || (ccLineSegment.turnsLeft() && ( adAlfa[ 0 ] < adAlfa[ 1 ] )))
                {
                    ccLineSegment.setAngleEnd( adAlfa[ 1 ] );
                    ccLineSegment.setS( adS[ 1 ] );
                }
                else
                {
                    ccLineSegment.setAngleEnd( adAlfa[ 0 ] );
                    ccLineSegment.setS( adS[ 0 ] );
                }
            }
            else
            {
                if ( (ccLineSegment.turnsRight() && ( -adAlfa[ 0 ] < -adAlfa[ 1 ] ))
                     || (ccLineSegment.turnsLeft() && ( adAlfa[ 0 ] < adAlfa[ 1 ] )) )
                {
                    ccLineSegment.setAngleEnd( adAlfa[ 0 ] );
                    ccLineSegment.setS( adS[ 0 ] );
                }
                else
                {
                    ccLineSegment.setAngleEnd( adAlfa[ 1 ] );
                    ccLineSegment.setS( adS[ 1 ] );
                }
            }
            // calculate length
            ccLineSegment.setLength( Math.abs(( ccLineSegment.getAngleEnd() - ccLineSegment.getAngleStart()) * ccLineSegment.getRadius()));

            // calculate end points once again from "S" value.
            // Places end point on end of track subsegment???
            ccLineSegment.setPosXEnd( dTrackPosX + ccLineSegment.getS() * Math.cos( dTrackAngle ));
            ccLineSegment.setPosYEnd( dTrackPosY + ccLineSegment.getS() * Math.sin( dTrackAngle ));
        }
    }

    // Calculate coordinates for start/end/center points of CCLine segments.
    // 2006-12-09 Klaus: added exception handler just in case something goes wrong in the calculations.
    public void calculateCCLine()
    {
        try {
            if ( m_CCLine.size() == 0 )
                // empty CCLine
                return;

            // To calculate the CCline, both CCline data itself and
            // track segment data is needed.
            CCLineSegment ccLineSegment;
            TrackSegment trackSegment;
            int nCumulatedTrackTlu = 0, nCumulatedCCLineTlu = 0;
            int nTrackSegmentIndex = 1, nCCLineSegmentIndex = 1;
            double dXPos, dYPos, dOffset, dAngleStart, dAngleEnd, dRadius;

            // Get the first segments
            ccLineSegment = m_CCLine.getAt( nCCLineSegmentIndex++ );
            nCumulatedCCLineTlu = ccLineSegment.getTlu();
            trackSegment = m_TrackSegments.getAt( nTrackSegmentIndex++ );
            nCumulatedTrackTlu = trackSegment.getTlu();

            // First ccLine segment is related to the first track segment:
            // starting point is on start/finish line, with an offset to the
            // middle of the road.
            dXPos = trackSegment.getPosXStart();
            dYPos = trackSegment.getPosYStart();
            // calculate offset from middle of the track
            dOffset = ccLineSegment.getParam( 0 ) / 1024.0 ; // given in width scales, i.e. 1/1024 tlu
            // calculate starting angle in radiens
            dAngleStart = trackSegment.getAngleStart() * s_dANGLE_SCALE;
            ccLineSegment.setAngleStart( dAngleStart );
            ccLineSegment.setPosXStart( dXPos + Math.cos( dAngleStart ) * dOffset );
            ccLineSegment.setPosYStart( dYPos + Math.sin( dAngleStart ) * dOffset );

            do {
                // retrieve starting values
                dXPos = ccLineSegment.getPosXStart();
                dYPos = ccLineSegment.getPosYStart();
                dAngleStart = ccLineSegment.getAngleStart();

                // calculate radius
                dRadius = ccLineSegment.calculateRadius();

                // calculate end point and end angle.
                // TLU value from CCLine segment is used for TrackSegments lookup, not
                // direct length calculations. Intersect CCLine segment with end
                // of corresponding track segment to find end point. For curved segments,
                // this calculation also gives the end angle.

                // Find the track segment where the CCLine segment ends.
                while ( nCumulatedCCLineTlu > nCumulatedTrackTlu )
                {
                    trackSegment = m_TrackSegments.getAt( nTrackSegmentIndex++ );
                    if ( trackSegment == null )
                    {
                        // end of track reached
                        nCumulatedTrackTlu = nCumulatedCCLineTlu;
                        // previous TrackSegmentIndex pointed already behind the list and
                        // was incremented once. Last track segment is a dummy segment, so
                        // subtract 3 to get the last real segment.
                        nTrackSegmentIndex = nTrackSegmentIndex - 3;
                        if ( nTrackSegmentIndex < 1 )
                            nTrackSegmentIndex = 1;
                        trackSegment = m_TrackSegments.getAt( nTrackSegmentIndex );
                    }
                    else
                    {
                        nCumulatedTrackTlu += trackSegment.getTlu();
                    }
                }

                // Calculate angle of track segment border at the end of the ccLine segment.
                double dTrackAngle = trackSegment.getAngleEnd()
                                     - (nCumulatedCCLineTlu - nCumulatedTrackTlu) * trackSegment.getCurvature();
                dTrackAngle = dTrackAngle * s_dANGLE_SCALE;

                // Calculate point on the track segment border at the end of the ccLine segment.
                double dTrackPosX;
                double dTrackPosY;
                if ( trackSegment.getCurvature() == 0 )
                {
                    // straight
                    dTrackPosX = trackSegment.getPosXStart()
                                 - (nCumulatedCCLineTlu - nCumulatedTrackTlu + trackSegment.getTlu() ) * Math.sin( dTrackAngle );
                    dTrackPosY = trackSegment.getPosYStart()
                                 + (nCumulatedCCLineTlu - nCumulatedTrackTlu + trackSegment.getTlu() ) * Math.cos( dTrackAngle );
                }
                else
                {
                    // curved: use center and radius for calculations
                    dTrackPosX = trackSegment.getPosXCenter()
                                 - trackSegment.getRadius() * Math.cos( dTrackAngle );
                    dTrackPosY = trackSegment.getPosYCenter()
                                 - trackSegment.getRadius() * Math.sin( dTrackAngle );
                }

                // Intersect CCLine with end of track segment. Sets values
                // for end point and end angle in ccline segment.
                intersect( ccLineSegment, dTrackPosX, dTrackPosY, dTrackAngle );

                // get next ccline segment
                if ( nCCLineSegmentIndex <= m_CCLine.size() )
                {
                    // get end values of last segment
                    dXPos = ccLineSegment.getPosXEnd();
                    dYPos = ccLineSegment.getPosYEnd();
                    dAngleEnd = ccLineSegment.getAngleEnd();
                    // get next segment
                    ccLineSegment = m_CCLine.getAt( nCCLineSegmentIndex++ );
                    // add tlu's to cumulated value
                    nCumulatedCCLineTlu += ccLineSegment.getTlu();
                    // transfer end values of previous segment to new one.
                    ccLineSegment.setPosXStart( dXPos );
                    ccLineSegment.setPosYStart( dYPos );
                    ccLineSegment.setAngleStart( dAngleEnd );
                }
                else
                    ccLineSegment = null;
            } while ( ccLineSegment != null );
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        // new calculations retrieved from game
        calculateGameCCLine();
    }

    // "Globals"
    private int length, radius;
    private short shiftx;
    private short wTmpAngleZ;
    private short wSegPosX;

    /**
      CCLine calculations retrieved from game by Rene.
    */

    protected void calculateGameCCLine()
    {
        if ( m_fLayoutMode )
            // no CCLine in (track/pit) layout mode.
            return;

        int nTrackSegNum = 0;
        wTmpAngleZ = m_TrackSegments.getSegAt( nTrackSegNum ).wAngleZ;
        CCLineSegment cclineSeg;
        int nCCLineSector = 0;

        for (Enumeration e = m_CCLine.elements(); e.hasMoreElements(); ) {
                cclineSeg = (CCLineSegment) e.nextElement();

                int nShift;
                int nParam = 0;

                nCCLineSector++;

                if ((cclineSeg.m_nType & 0x80) != 0)   // starting sector
                        wSegPosX = (short) cclineSeg.m_nParam[ nParam++ ];

                // shift or correction value: modification at start of sector.
                nShift = cclineSeg.m_nParam[ nParam++ ];

                radius = cclineSeg.m_nParam[ nParam++ ];

                if ((cclineSeg.m_nType & 0x40) != 0) {
                        // 32bit radius
                        radius = (radius << 16) | (cclineSeg.m_nParam[ nParam++ ] & 0x0FFFF);
                }

                radius <<= 3; // * 8

                // shift longitudinal or by angle
                if (radius != 0)  // curve segment
                        shiftx = (short) (nShift << 2);
                else {	// straight segment
                        shiftx = 0;
                        wTmpAngleZ += (short) nShift;
                }

                length = ((cclineSeg.m_nType & 0x3f) << 8) | cclineSeg.m_nTlu;

                Seg seg = m_TrackSegments.getSegAt( nTrackSegNum );
                ProcessCCLineSector(seg);

                try {
                    for (int i = 0; i < length; i++) {
                            seg.wCCLineRAngle = wTmpAngleZ - seg.wAngleZ;
                            seg.wCCLine = wSegPosX;
                            seg.m_nCCLineSector = nCCLineSector;

                            nTrackSegNum++;
                            if (nTrackSegNum >= m_TrackSegments.nSegNumber)
                                    nTrackSegNum = 1;	// cyclic (should restart at 0??)

                            seg = m_TrackSegments.getSegAt( nTrackSegNum );
                            ProcessCCLineSegment(seg);
                    }
                }
                catch( Exception exc )
                {
                    exc.printStackTrace();                    
                }
        }
    }

    /**
        ProcessCCLineSector
    */

    private int tmpX = 0, tmpY = 0; // "Globals"
    private int tmpCos = 0, tmpSin = 0;
    private int tmp1 = 0, tmp2 = 0, tmp5 = 0, tmp6 = 0;

    // calculate tmpX and tmpY: coordinates
    private void ProcessCCLineSector(Seg pSeg) {
        short wSegPosY = (short) ((wSegPosX * pSeg.wAngleZChangeMulHalfPI) >> 15);

        tmpSin = F1GPMath.LookupSin(pSeg.wAngleZ);
        tmpCos = F1GPMath.LookupCos(pSeg.wAngleZ);

        tmp5 = ((wSegPosY * tmpCos) - (wSegPosX * tmpSin)) >> 14; // sin/cos scaled by 14 bits
        tmp6 = ((wSegPosX * tmpCos) + (wSegPosY * tmpSin)) >> 14;

        tmp5 += pSeg.getPosY();
        tmp6 += pSeg.getPosX();

        // wTmpAngleZ and shiftx are values of current ccLine sector.
        tmp5 += (F1GPMath.LookupCos((short) wTmpAngleZ) * shiftx) >> 14;
        tmpY = tmp5;

        tmp6 += (F1GPMath.LookupSin((short) wTmpAngleZ) * shiftx) >> 14;
        tmpX = tmp6;

        if (radius != 0) {

            int nAngle;
            if (radius >= 0)
                nAngle = wTmpAngleZ + 0x4000;   // + 90 degrees
            else
                nAngle = wTmpAngleZ - 0x4000;   // - 90 degrees

            // results stored in tmpSin and tmpCos (32bit values: sin/cos shifted by 30 bits)
            sinAndCosBig(nAngle);

            long ll = (long) tmpSin * (long) Math.abs( radius );
            //    tmpX += ((ll >> 16) * 4) >> 16;
            tmpX += (ll >> 30);

            ll = (long) tmpCos * (long) Math.abs( radius );
            //    tmpY += ((ll >> 16) * 4) >> 16;
            tmpY += (ll >> 30);
        }
    }

    /**
        ProcessCCLineSegment.
        Uses "Globals" tmpX, tmpY.
        Modifies "Globals" wTmpAngleZ, wSegPosX.
        Calculate position and angle of ccLine at next track Seg start.
    */

    void ProcessCCLineSegment( Seg seg )
    {
        int nXDiff = tmpX - seg.getPosX();
        int nYDiff = tmpY - seg.getPosY();

        int invPI = 0x517D; // 0x10000 / PI
        int a1 = seg.wAngleZ - (((seg.wAngleZChangeMulHalfPI >> 1) * invPI) >> 15);
        tmpCos = a1; // actually an angle!

        // convert to degrees for debugging...
        double dAngleDeg = ((double) a1 / 0x10000) * 360.0;

        // sinBig( tmpCos ) stored in tmpCos
        // cosBig( tmpCos ) stored in tmpSin
        sinAndCosBig(tmpCos);

        tmp5 = (int) ((((long) tmpCos * (long) nXDiff) - ((long) tmpSin * (long) nYDiff)) >> 30);
        tmp6 = (int) ((((long) tmpCos * (long) nYDiff) + ((long) tmpSin * (long) nXDiff)) >> 30);

        if (radius == 0) {
            // ccLine straight
            int tmp = wTmpAngleZ - a1;
            tmp1 = F1GPMath.LookupSinbig((short) tmp);
            tmp2 = F1GPMath.LookupCosbig((short) tmp);
            long lTemp = (long) tmp1 * (long) tmp6;
            if ( tmp2 == 0 )
            {
                // Makes no sense, but I think this happens in the game (KS)
                tmp6 = (int) (lTemp & (long) 0x0000FFFF);
            }
            else
            {
                tmp6 = (int) (lTemp / ((long) tmp2));
            }
            wSegPosX = (short) (tmp5 - tmp6);
        }
        else
        {
            // Pythagoras
            long ll = (((long) radius * (long) radius) - ((long) tmp6 * (long) tmp6));
            tmp1 = (int) F1GPMath.sqrt64(ll);

            if (radius < 0)
                tmp1 = -tmp1;

            wSegPosX = (short) (tmp5 - tmp1);

            int r = radius;
            if (r < 0)
                    r = -r;

            for (; r >= 0x7F00; r >>= 1) {
                tmp1 >>= 1;
                tmp6 >>= 1;
            }

            int a2 = F1GPMath.LookupAtan2(tmp1, tmp6);

            if (radius < 0)
                a2 = a2 + 0x4000;
            else
                a2 = a2 - 0x4000;

            wTmpAngleZ = (short) (a1 + a2);
        }
    }

    private void getOppositeEdgeLength() {
            long tmp = 0x1000000000000000l - ((long) tmpCos * (long) tmpCos);

            if (tmp < 0)
                    tmp = 0;

            tmpSin = (int) F1GPMath.sqrt64(tmp);
    }


    /**
        Calculate 32Bit Sin and Cos from tmpCos (16 bit angle value).
        If Cos is below 0.5, Sin is read from the lookup tables and Cos is
        calculated from it.
        Otherwise Cos is read from lookup table and Sin is calculated from it.
        Returns results in tmpSin and tmpCos (shifted by 30 bits).
    */
    private void sinAndCosBig(int nAngle) {
            short oldCos = (short) nAngle;
            short index = (short) ((-nAngle) + (short) 0x4000);

            if (index < 0)
                index = (short) -index;

            int i = (index >> 2) & 0x3FFE; // also remove sign bits that could be present (e.g. nAngle = C000h)
            short val = CosLookupTable.get(i / 2);

            if (val < 0)
                val = (short) -val;

            if (val < 0x2000) {
                // Calculate Cos from Sin
                tmpCos = F1GPMath.LookupSinbig((short) nAngle);

                if (oldCos < 0)
                        oldCos = (short) -oldCos;

                int j = (oldCos >> 2) & 0x3FFE;// also remove sign bits that could be present
                oldCos = CosLookupTable.get(j / 2);
                getOppositeEdgeLength();

                if (oldCos < 0)
                        tmpSin = -tmpSin;

                // swap tmpCos and tmpSin
                int temp = tmpSin;
                tmpSin = tmpCos;
                tmpCos = temp;
            } else {
                // Calculate Sin from Cos
                tmpCos = F1GPMath.LookupCosbig((short) nAngle);
                oldCos = (short) (-oldCos + 0x4000);

                if (oldCos < 0)
                    oldCos = (short) -oldCos;

                int j = (oldCos >> 2) & 0x3FFE; // also remove sign bits that could be present
                oldCos = CosLookupTable.get(j / 2);
                getOppositeEdgeLength();

                if (oldCos < 0)
                    tmpSin = -tmpSin;
            }
    }

    /**
        Returns rectangle that contains all track graphics
        based on F1GP in-game calculations (Seg objects).
    */

    public Rectangle getF1GPBoundingRectangle()
    {
        double dMinX, dMinY, dMaxX, dMaxY, dX, dY;
        TrackSegments trackSegments = getTrackSegments();
        Seg seg;
        // Initialize coordinates with first point
        seg = trackSegments.getSegAt( 1 );
        dMinX = seg.getPosX();
        dMaxX = dMinX;
        dMinY = seg.getPosY();
        dMaxY = dMinY;
        // search for min/max values
        for ( int i = 1; i <= trackSegments.getMaxTrackSegIndex(); i++ )
        {
            seg = trackSegments.getSegAt( i );
            dX = seg.getPosX();
            if ( dX > dMaxX )
                dMaxX = dX;
            if ( dX < dMinX )
                dMinX = dX;
            dY = seg.getPosY();
            if ( dY > dMaxY )
                dMaxY = dY;
            if ( dY < dMinY )
                dMinY = dY;
        } // for
        // Put data into rectangle
        Rectangle r = new Rectangle( new Double(dMinX).intValue(),
                                     new Double(dMinY).intValue(),
                                     new Double(dMaxX - dMinX).intValue(),
                                     new Double(dMaxY - dMinY).intValue() );
        return r;
    }

    // Returns a rectangle that contains the whole track graphics.
    // x/y coordinates represent the minimum coordinates and heigth/width
    // give the size of the rectangle needed to contain the track.
    public Rectangle getBoundingRectangle()
    {
        double dMinX, dMinY, dMaxX, dMaxY, dX, dY;
        TrackSegments trackSegments = getTrackSegments();
        TrackSegment trackSegment;
        // Initialize coordinates with first point
        trackSegment = trackSegments.getAt( 1 );
        dMinX = trackSegment.getPosXStart();
        dMaxX = dMinX;
        dMinY = trackSegment.getPosYStart();
        dMaxY = dMinY;
        // search for min/max values
        for ( int i = 1; i <= trackSegments.size(); i++ )
        {
            trackSegment = trackSegments.getAt( i );
            dX = trackSegment.getPosXStart();
            if ( dX > dMaxX )
                dMaxX = dX;
            if ( dX < dMinX )
                dMinX = dX;
            dY = trackSegment.getPosYStart();
            if ( dY > dMaxY )
                dMaxY = dY;
            if ( dY < dMinY )
                dMinY = dY;
            dX = trackSegment.getPosXEnd();
            if ( dX > dMaxX )
                dMaxX = dX;
            if ( dX < dMinX )
                dMinX = dX;
            dY = trackSegment.getPosYEnd();
            if ( dY > dMaxY )
                dMaxY = dY;
            if ( dY < dMinY )
                dMinY = dY;
        } // for
        // Put data into rectangle
        Rectangle r = new Rectangle( new Double(dMinX).intValue(),
                                     new Double(dMinY).intValue(),
                                     new Double(dMaxX - dMinX).intValue(),
                                     new Double(dMaxY - dMinY).intValue() );
        return r;
    }
}
