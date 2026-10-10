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

import java.util.Vector;

import cfevolution.data.track.Command;
import cfevolution.data.track.Track;
import cfevolution.data.track.TrackSegment;
import cfevolution.data.track.TrackSegments;

/**
    Keeps a track file under the size the game will load
    (Track.MAX_FILE_BYTES, 20,515 bytes). Over it, the game freezes on the
    circuit preview with no message.

    Generated tracks are the ones at risk. They keep the donor's object
    shapes, are cut into more sections than the donor, and carry a longer best
    line, so they come out larger. Measured 2026-10-10: Monaco-donor tracks
    reached 19,941 bytes, and Monaco itself is 20,497. The cheapest thing to
    give up is scenery, so this drops object placements (command 0x80),
    spread across the lap, until the file fits.

    Objects 0 and 1 are never dropped. In the game they are the two pit-lane
    connections, which tell the renderer where the pit lane becomes visible.
*/
public class TrackFileSize {

    /** Drops scenery placements until the track fits. Each pass takes one
        object from the section carrying the most, so the thinning is spread
        across the lap instead of clearing one stretch.
        Returns the number of objects removed: 0 if the track already fits,
        -1 if it still does not fit once nothing removable is left. */
    public static int trimSceneryToFit(Track track) {
        long lSize = track.measureSavedSize();
        if (lSize < 0 || lSize <= Track.MAX_FILE_BYTES)
            return 0;

        TrackSegments segs = track.getTrackSegments();
        int nRemoved = 0;
        while (lSize > Track.MAX_FILE_BYTES) {
            // Every 0x80 command is 4 bytes on disk. Remove what the excess
            // needs, then measure again: other sections round differently.
            long lNeeded = (lSize - Track.MAX_FILE_BYTES + 3) / 4;
            for (long k = 0; k < lNeeded; k++) {
                if (!removeOneObject(segs))
                    return -1;
                nRemoved++;
            }
            lSize = track.measureSavedSize();
            if (lSize < 0)
                return -1;
        }
        return nRemoved;
    }

    /** Removes the last removable object from the section that has the
        most. Returns false if there are none left. */
    private static boolean removeOneObject(TrackSegments segs) {
        Vector best = null;
        int nBestCount = 0;
        for (int i = 0; i < segs.size(); i++) {
            Vector cmds = ((TrackSegment) segs.get(i)).getCommands();
            int nCount = 0;
            for (int c = 0; c < cmds.size(); c++)
                if (isRemovableObject((Command) cmds.get(c)))
                    nCount++;
            if (nCount > nBestCount) {
                nBestCount = nCount;
                best = cmds;
            }
        }
        if (best == null)
            return false;
        for (int c = best.size() - 1; c >= 0; c--) {
            if (isRemovableObject((Command) best.get(c))) {
                best.remove(c);
                return true;
            }
        }
        return false;
    }

    /** A 0x80 placement of anything but objects 0 and 1 (the pit-lane
        connections). The object index is the definition offset >> 4. */
    private static boolean isRemovableObject(Command cmd) {
        return cmd.getType() == 0x80 && (cmd.getParam(1) >> 4) >= 2;
    }
}
