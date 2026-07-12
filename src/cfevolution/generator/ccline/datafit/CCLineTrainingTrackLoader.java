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

package cfevolution.generator.ccline.datafit;

import java.io.File;
import java.util.Vector;

import cfevolution.data.track.Track;
import cfevolution.generator.ccline.CCLineSimulator;
import cfevolution.generator.ccline.CCLineTrackGeometry;

/**
    Loads original F1CT*.DAT files from a user-selected folder and turns
    each Seg of each track into one training sample: geometry features
    plus the hand-tuned line's lateral position (normalised by the local
    usable width) as the target. Nothing derived from the game files is
    stored — training happens in memory at generation time.
*/
public class CCLineTrainingTrackLoader {

    /** Feature count produced by extractFeatures(). */
    public static final int FEATURES = 8;

    /** Curvature summing horizons (in Segs / TLU). */
    private static final int[] HORIZONS = { 16, 64, 128 };

    public final Vector trackNames = new Vector();
    public double[][] features = new double[0][];
    public double[] targets = new double[0];

    /** Loads every readable F1CT*.DAT in the folder, optionally skipping
        one file name (for leave-one-out testing). Returns the number of
        tracks loaded. */
    public int load(File folder, String skipFileName) {
        Vector featList = new Vector();
        Vector targList = new Vector();

        File[] files = folder.listFiles();
        if (files == null)
            return 0;
        java.util.Arrays.sort(files);
        for (int fi = 0; fi < files.length; fi++) {
            File f = files[fi];
            String name = f.getName().toUpperCase();
            if (!name.matches("F1CT\\d+\\.DAT"))
                continue;
            if (skipFileName != null && name.equalsIgnoreCase(skipFileName))
                continue;
            try {
                Track t = new Track();
                if (!t.load(f))
                    continue;
                CCLineTrackGeometry geo = new CCLineTrackGeometry(t);
                if (t.getCCLine().size() == 0)
                    continue;
                CCLineSimulator.Result r = new CCLineSimulator(geo).run(t.getCCLine());
                double[][] trackFeatures = extractAllFeatures(geo);
                for (int i = 0; i < geo.segCount; i++) {
                    if (!r.covered[i] || geo.usableBound[i] <= 0)
                        continue;
                    double target = r.ccLine[i] / geo.usableBound[i];
                    if (target > 1.0) target = 1.0;
                    if (target < -1.0) target = -1.0;
                    featList.add(trackFeatures[i]);
                    targList.add(new Double(target));
                }
                trackNames.add(f.getName());
            }
            catch (Exception e) {
                // unreadable file: skip it
            }
        }

        features = new double[featList.size()][];
        targets = new double[targList.size()];
        for (int i = 0; i < featList.size(); i++) {
            features[i] = (double[]) featList.get(i);
            targets[i] = ((Double) targList.get(i)).doubleValue();
        }
        return trackNames.size();
    }

    /** Per-Seg feature vectors for a whole track. */
    public static double[][] extractAllFeatures(CCLineTrackGeometry geo) {
        int n = geo.segCount;
        // Signed per-Seg track curvature (heading delta to the next Seg)
        double[] curv = new double[n];
        for (int i = 0; i < n; i++)
            curv[i] = (short) (geo.angleZ[(i + 1) % n] - geo.angleZ[i]);

        double[][] result = new double[n][];
        for (int i = 0; i < n; i++) {
            double[] f = new double[FEATURES];
            f[0] = curv[i];
            int fi = 1;
            for (int h = 0; h < HORIZONS.length; h++) {
                double ahead = 0, behind = 0;
                for (int d = 1; d <= HORIZONS[h]; d++) {
                    ahead += curv[(i + d) % n];
                    behind += curv[(i + n - d) % n];
                }
                f[fi++] = ahead;
                f[fi++] = behind;
            }
            f[fi] = geo.usableBound[i];
            result[i] = f;
        }
        return result;
    }
}
