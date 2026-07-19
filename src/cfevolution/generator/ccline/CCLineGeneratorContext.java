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

package cfevolution.generator.ccline;

import java.io.File;

import cfevolution.data.track.Track;

/**
    Everything a generator needs for one run: the track, its extracted
    geometry, and the user-tunable parameters from the generation dialog.
*/
public class CCLineGeneratorContext {

    public final Track track;
    public final CCLineTrackGeometry geometry;
    public final CCLineEvaluator evaluator;

    /** Iteration budget for the iterative methods. */
    public int iterations = 2000;

    /** Extra TLU appended past the track end so the line wraps smoothly
        across the start/finish seam (original tracks use 0-44). */
    public int seamOvershoot = 0;

    /** Geometric method: distance kept from the physical road edge, as a
        fraction of the local half-width (0.15 = leave 15% margin). */
    public double edgeStandoff = 0.15;

    /** 0..1: how strongly the geometric method smooths towards minimum
        curvature vs staying near the track centre. */
    public double smoothingWeight = 0.9;

    /** Folder containing original F1CT*.DAT files (data-fit method only). */
    public File trainingFolder = null;

    public CCLineGeneratorContext(Track track) {
        this.track = track;
        this.geometry = new CCLineTrackGeometry(track);
        this.evaluator = new CCLineEvaluator(geometry);
    }
}
