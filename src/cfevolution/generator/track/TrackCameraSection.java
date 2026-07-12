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

package cfevolution.generator.track;

import cfevolution.data.track.Footer;

/**
    The camera definitions section is a list of two-byte ADJUSTMENT
    commands relative to the cameras the game places by default (delete
    camera N, move camera N, switch side) terminated by a 0xFF byte pair.
    ChequeredFlag does not parse it — it sits at the start of the raw
    Footer blob (which is everything after the pit lane sections).

    For a generated track the donor's adjustments would reference cameras
    of the old layout, so we simply empty the list: the game then uses its
    default camera placement, which is valid for any layout.
*/
public class TrackCameraSection {

    /** Replaces the camera adjustment block at the start of the footer
        with an empty list. Returns the number of adjustment command bytes
        removed, or -1 if no terminator was found (footer left untouched). */
    public static int emptyCameraAdjustments(Footer footer) {
        byte[] data = footer.getData();
        int nSize = footer.getDataSize();

        // Every camera adjustment command is two bytes; the list ends with
        // a pair whose first byte is 0xFF.
        int nPos = 0;
        while (nPos + 1 < nSize && (data[nPos] & 0xFF) != 0xFF)
            nPos += 2;
        if (nPos + 1 >= nSize)
            return -1; // no terminator found; leave the footer alone

        if (nPos == 0)
            return 0; // already empty

        // Keep the terminator pair and everything after it
        int nNewSize = nSize - nPos;
        byte[] baNew = new byte[Math.max(nNewSize, 256)];
        System.arraycopy(data, nPos, baNew, 0, nNewSize);
        footer.setData(baNew, nNewSize);
        return nPos;
    }
}
