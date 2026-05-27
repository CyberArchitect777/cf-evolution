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
 * Command.java
 *
 * Created on 16. Februar 2005, 23:17
 */

package chequeredflag.data.track;

import java.io.*;
import chequeredflag.data.*;

/**
 *
 * @author Klaus
 */
public class Command extends CFDataObject {

    /** Creates a new instance of Command */
    public Command(int nType, int nParam0) {
        m_nType = nType;
        // Always allocate 6 parameters, regardless of type.
        // This makes type change easy.
        m_nParam = new int[6];
        // store first parameter
        m_nParam[ 0 ] = nParam0;
    }
    
    public Command(int nType, int nParam0, int nParam1, int nParam2, int nParam3, int nParam4, int nParam5)
    {
        // Written by b-za. Allows the creation of a command with a full set of parameters
        
        m_nType = nType;
        
        m_nParam = new int[6];
        
        m_nParam[0] = nParam0;
        m_nParam[1] = nParam1;
        m_nParam[2] = nParam2;
        m_nParam[3] = nParam3;
        m_nParam[4] = nParam4;
        m_nParam[5] = nParam5;        
    }

    public void load( FileInputStream fis )
    {
        try {
            // Number of parameters depends on type
            switch (m_nType)
            {
            case 0x83:  // disable drawing of background picture
            case 0x84:  // enable drawing of background picture
            case 0x86:  // pit start. SDI: connect pitlane start
            case 0x87:  // pit end. SDI: connect pitlane end
            case 0x96:  // Unk. SDI: pitlane start
            case 0x97:  // Unk. SDI: pitlane end
            case 0x9B:  // Unk. SDI: some pitlane marker 1
            case 0x9C:  // Unk. SDI: some pitlane marker 2
            case 0x9D:  // Unk. SDI: some pitlane marker 3
            case 0x9E:  // Unk. SDI: some pitlane marker 4
            case 0x9F:  // Unk. SDI: pitlane fences start
            case 0xA0:  // Unk. SDI: pitlane fences end
            case 0xA1:  // pit lane entry, join right pit lane fence
            case 0xA2:  // pit lane entry, join left pit lane fence
            case 0xA3:  // pit lane exit, join right pit lane fence
            case 0xA4:  // pit lane exit, join left pit lane fence
            case 0xA5:  // Unk. SDI: change the sign of first 2 sector args
            case 0xA8:  // marshal with a flag. SDI: segment at which to show the chequered flag, usually before s/f is visible
                // no further parameters
                break;

            case 0x80:  // object placement
            case 0x81:  // display distance front
            case 0x82:  // display distance back
            case 0x88:  // Unk. SDI: pit parking zone marking 1, arg=length (same as cmd 0x8a/0x8b[8, arg, -, -, 0x102])
            case 0x89:  // Unk. SDI: pit parking zone marking 2, arg=length (same as cmd 0x8a/0x8b[8, arg, -, -, 0x102])
            case 0x8C:  // Unk. SDI: changes something left, arg=length
            case 0x8D:  // Unk. SDI: changes something right, arg=length
            case 0x90:  // Unk. SDI: similar to 0x80 but going backwards arg segments
            case 0x91:  // Unk. SDI: similar to 0x81 but going forwards arg segments
            case 0x92:  // Unk. SDI: marks something (similar to 0xa8), arg=length
            case 0x93:  // Unk. SDI: marks something (similar to 0xa8), arg=length
            case 0x94:  // curve polygon subdivision (1=many polygon, 8=few polygon). SDI: CC coaching left
            case 0x95:  // may be same as 94 (?). SDI: CC coaching right
            case 0x98:  // left fence height change. SDI: arg=1...8
            case 0x99:  // right fence height change. SDI: arg=1...8
            case 0xA9:  // Unk. SDI: sets view distance for pitlane, else 60. Related to 0x81/0x82/0x90/0x91
                // one 2-byte int parameter
                m_nParam[ 1 ] = fis.read() + fis.read() * 256;
                break;

            case 0x85: // track width change
            case 0x8E: // left kerbs begin/length
            case 0x8F: // right kerbs begin/length
            case 0x9A: // Unk. SDI: custom fence height, arg1=index 1...8:left 9...16:right, arg2=height
            case 0xA6: // Unk. SDI: sets some flags in the segment
            case 0xA7: // Unk. SDI: sets some flags in the segment
            case 0xAB: // Unk. SDI: Unused? (will hang if arg1 equal to 42)
                // 2 Parameters
                m_nParam[ 1 ] = fis.read() + fis.read() * 256;
                m_nParam[ 2 ] = fis.read() + fis.read() * 256;
                break;

            case 0xAA: // Unk. SDI: arg1=length for connect pitlane start (backwards), arg2=length for connect pitlane end, arg3=pitlane speed
                // 3 Parameters
                m_nParam[ 1 ] = fis.read() + fis.read() * 256;
                m_nParam[ 2 ] = fis.read() + fis.read() * 256;
                m_nParam[ 3 ] = fis.read() + fis.read() * 256;
                break;

            case 0xAC: // palette change. SDI: arg1=palette index 0...255, arg2=red? arg3=green? arg4=blue? (0...63?)
                // 4 Parameters
                m_nParam[ 1 ] = fis.read() + fis.read() * 256;
                m_nParam[ 2 ] = fis.read() + fis.read() * 256;
                m_nParam[ 3 ] = fis.read() + fis.read() * 256;
                m_nParam[ 4 ] = fis.read() + fis.read() * 256;
                break;

            case 0x8A: // track markings. SDI: arg2=length
            case 0x8B: // starting grid markings. SDI: arg2=length
                // 5 Parameters
                m_nParam[ 1 ] = fis.read() + fis.read() * 256;
                m_nParam[ 2 ] = fis.read() + fis.read() * 256;
                m_nParam[ 3 ] = fis.read() + fis.read() * 256;
                m_nParam[ 4 ] = fis.read() + fis.read() * 256;
                m_nParam[ 5 ] = fis.read() + fis.read() * 256;
                break;
            }
        }
        catch(IOException ioe)
        {
        }
    }

    public int save( FileOutputStream fos ) throws IOException
    {
        int nWritten = 0;
        // First parameter is always there (one byte)
        fos.write( m_nParam[ 0 ] );
        // Write type
        fos.write( m_nType );
        nWritten = 2;
        // Rest depends on type
        switch (m_nType)
        {
        case 0x83:
        case 0x84:
        case 0x86:
        case 0x87:
        case 0x96:
        case 0x97:
        case 0x9B:
        case 0x9C:
        case 0x9D:
        case 0x9E:
        case 0x9F:
        case 0xA0:
        case 0xA1:
        case 0xA2:
        case 0xA3:
        case 0xA4:
        case 0xA5:
        case 0xA8:
            // no further parameters
            break;

        case 0x80:
        case 0x81:
        case 0x82:
        case 0x88:
        case 0x89:
        case 0x8C:
        case 0x8D:
        case 0x90:
        case 0x91:
        case 0x92:
        case 0x93:
        case 0x94:
        case 0x95:
        case 0x98:
        case 0x99:
        case 0xA9:
            // one 2-byte int parameter
            write( fos, m_nParam[ 1 ] );
            nWritten += 2;
            break;

        case 0x85:
        case 0x8E:
        case 0x8F:
        case 0x9A:
        case 0xA6:
        case 0xA7:
        case 0xAB:
            // 2 Parameters
            write( fos, m_nParam[ 1 ] );
            write( fos, m_nParam[ 2 ] );
            nWritten += 4;
            break;

        case 0xAA:
            // 3 Parameters
            write( fos, m_nParam[ 1 ] );
            write( fos, m_nParam[ 2 ] );
            write( fos, m_nParam[ 3 ] );
            nWritten += 6;
            break;

        case 0xAC:
            // 4 Parameters
            write( fos, m_nParam[ 1 ] );
            write( fos, m_nParam[ 2 ] );
            write( fos, m_nParam[ 3 ] );
            write( fos, m_nParam[ 4 ] );
            nWritten += 8;
            break;

        case 0x8A:
        case 0x8B:
            // 5 Parameters
            write( fos, m_nParam[ 1 ] );
            write( fos, m_nParam[ 2 ] );
            write( fos, m_nParam[ 3 ] );
            write( fos, m_nParam[ 4 ] );
            write( fos, m_nParam[ 5 ] );
            nWritten += 10;
            break;
        }
        return nWritten;
    }

    // instance data members
    int m_nType;
    int m_nParam[];

    public int getParam(int nParam) {
        if ( nParam <= 5 )
            return m_nParam[ nParam ];
        else
            return 0;
    }
    
    public void setParam(int nParam, int paramValue)
    {
        // Written by Barrie. Allows the setting of command parameters
        // to match the functionally of the retrieval process. 
        
        m_nParam[nParam] = paramValue;
        
    }

    public int getType() {
        return m_nType;
    }
}
