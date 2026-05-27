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
 * CCSetup.java
 *
 * Created on 17. Februar 2005, 23:47
 */

package chequeredflag.data.track;

import java.io.*;
import chequeredflag.data.*;

/**
 *
 * @author Klaus
 */
public class CCSetup extends CFDataObject {

    /** Creates a new instance of CCSetup */
    public CCSetup() {
        m_nGearRatio = new int[ 7 ]; // Gear ratios 1..6
    }

    public void load( FileInputStream fis)
    {
        try {
            m_nFrontWing = fis.read();
            m_nRearWing = fis.read();
            for ( int i = 1; i <= 6; i++ )
                m_nGearRatio[ i ] = fis.read();
            m_nTyreType = fis.read();
            m_nBrakeBalance = fis.read();
            // From here values are 2 bytes!
            m_nGrip = fis.read() + fis.read() * 256;
            m_nQA = fis.read() + fis.read() * 256;
            m_nRA = fis.read() + fis.read() * 256;
            m_nQT = fis.read() + fis.read() * 256;
            m_nAccel = fis.read() + fis.read() * 256;
            m_nAirRes = fis.read() + fis.read() * 256;
            m_nUnk1 = fis.read() + fis.read() * 256;
            m_nTW1 = fis.read() + fis.read() * 256;
            m_nTW2 = fis.read() + fis.read() * 256;
            m_nRT = fis.read() + fis.read() * 256;
            m_nUnk2 = fis.read() + fis.read() * 256;;
            m_nWetRA = fis.read() + fis.read() * 256;
            m_nUnk3 = fis.read() + fis.read() * 256;
            m_nUnk4 = fis.read() + fis.read() * 256;
        }
        catch( IOException ioe )
        {
        }
    }

    public int save(FileOutputStream fos) throws IOException
    {
        // writing single bytes
        fos.write( m_nFrontWing );
        fos.write( m_nRearWing );
        for ( int i = 1; i <= 6; i++ )
            fos.write( m_nGearRatio[ i ] );
        fos.write( m_nTyreType );
        fos.write( m_nBrakeBalance );
        // From here values are 2 bytes!
        write( fos, m_nGrip );
        write( fos, m_nQA );
        write( fos, m_nRA );
        write( fos, m_nQT );
        write( fos, m_nAccel );
        write( fos, m_nAirRes );
        write( fos, m_nUnk1 );
        write( fos, m_nTW1 );
        write( fos, m_nTW2 );
        write( fos, m_nRT );
        write( fos, m_nUnk2 );
        write( fos, m_nWetRA );
        write( fos, m_nUnk3 );
        write( fos, m_nUnk4 );
        return 38; // number of bytes written
    }

    // instance data members
    protected int m_nFrontWing, m_nRearWing;
    protected int m_nGearRatio[];
    protected int m_nTyreType;
    protected int m_nBrakeBalance;
    protected int m_nGrip;
    protected int m_nQA, m_nRA, m_nQT;
    protected int m_nAccel, m_nAirRes;
    protected int m_nUnk1, m_nUnk2, m_nUnk3, m_nUnk4;
    protected int m_nTW1, m_nTW2, m_nRT;
    protected int m_nWetRA;
}
