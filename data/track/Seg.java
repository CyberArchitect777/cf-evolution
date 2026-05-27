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

package chequeredflag.data.track;

/*
 * User: Rene
 * Date: 18-okt-2005
 * Time: 22:11:29
 * $Id: Seg.java,v 1.8 2007/04/26 19:54:05 ksix Exp $
 */

public class Seg {
	short wAngleZ;
	short wAngleXChase;
	short wPosX;
	short wPosZ;
	short wPosY;
        
        short wLeftAndRightSideX;   // X/Y distance of track border from reference point (middle of the track)
        short wLeftAndRightSideY;
        byte  bExtraSideX;          // X/Y distance of run-off area from reference point (middle of the track)
        byte  bExtraSideY;

	int   wAngleZChangeMulHalfPI;
	int   wCCLine;
	int   wCCLineRAngle;
	short bPosFine;	// 3 bits for x in low nibble, 3 bits for y in high nibble

        // Number of corresponding track/pit sector.
        // Used only here (not in game) for highlighting.
        public int m_nTrackSector;
        public int m_nCCLineSector; // same for CCLine

        public Seg() {
            wAngleZ = 0;
            wAngleXChase = 0;
            wPosX = 0;
            wPosZ = 0;
            wPosY = 0;

            wLeftAndRightSideX = 0;
            wLeftAndRightSideY = 0;
            bExtraSideX = 0;
            bExtraSideY = 0;

            wAngleZChangeMulHalfPI = 0;
            wCCLine = 0;
            wCCLineRAngle = 0;
            bPosFine = 0;

            m_nTrackSector = 0;
            m_nCCLineSector = 0;
        }

        // for hiding details of x/y pos storage (bPosFine)
        public int getPosX() { return ((int) wPosX << 3) | bPosFine & 0x07; };
        public int getPosY() { return ((int) wPosY << 3) | ((bPosFine >> 4) & 0x07); };
        public int getPosZ() { return wPosZ; };

        /**
            F1GP uses 19 bit for x and y coordinates: 16 bits in wPosX/Y, and 3 in bPosFine.
            We do the same so values can be compared directly with game.
        */
        public void setPos( int nNewPosX, int nNewPosY, int nNewPosZ )
        {
            bPosFine = (short) ((nNewPosX & 0x07) | ((nNewPosY & 0x07) << 4));
            wPosX = (short) (nNewPosX >> 3);
            wPosY = (short) (nNewPosY >> 3);
            wPosZ = (short) nNewPosZ;
        };

        public int getAngleZ()
        { return wAngleZ; };

        public int getAngleXChase()
        { return wAngleXChase; }

        public short getTrackWidthX() { return wLeftAndRightSideX; };
        public short getTrackWidthY() { return wLeftAndRightSideY; };
        public void setTrackWidthX( short wTrackWidthX ) { wLeftAndRightSideX = wTrackWidthX; };
        public void setTrackWidthY( short wTrackWidthY ) { wLeftAndRightSideY = wTrackWidthY; };

        public byte getExtraSideX() { return bExtraSideX; };
        public byte getExtraSideY() { return bExtraSideY; };
        public void setExtraSideX( byte bExtraSideX ) { bExtraSideX = bExtraSideX; };
        public void setExtraSideY( byte bExtraSideY ) { bExtraSideX = bExtraSideX; };


        public int getCCLine()
        { return wCCLine; };

        public int getCCLineRAngle()
        { return wCCLineRAngle; }

}
