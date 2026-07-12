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

import java.util.Vector;

import cfevolution.data.track.CCLine;

/**
    Output of one generator run: the candidate line, its evaluated score,
    per-Seg lateral offsets for the map preview, and any warnings for the
    dialog to show.
*/
public class CCLineGenerationResult {

    public final CCLine ccLine;
    public final CCLineEvaluator.Score score;
    /** Simulated lateral offset per Seg (wCCLine units) for preview drawing. */
    public final double[] previewOffsets;
    public final Vector warnings = new Vector();

    public CCLineGenerationResult(CCLine ccLine, CCLineEvaluator.Score score) {
        this.ccLine = ccLine;
        this.score = score;
        CCLineSimulator.Result r = score.simulation;
        previewOffsets = new double[r.ccLine.length];
        for (int i = 0; i < r.ccLine.length; i++)
            previewOffsets[i] = r.ccLine[i];
    }

    public void addWarning(String warning) {
        warnings.add(warning);
    }
}
