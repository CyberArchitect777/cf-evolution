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

import cfevolution.data.track.CCLine;
import cfevolution.generator.ccline.*;

/**
    Approach 2: learned from original tracks.

    Trains at generation time on a folder of original F1CT files: every
    Seg of every training track becomes a (geometry features -> normalised
    lateral position) sample of the hand-tuned line. The current track's
    profile is predicted per Seg by distance-weighted k-nearest-neighbour
    regression, smoothed, and quantised by the shared CCLineQuantizer.
*/
public class DataFitCCLineGenerator implements CCLineGenerator {

    private static final int K = 8;
    private static final double BOUND_MARGIN_FRACTION = 0.3;

    /** Set to skip one training file (leave-one-out testing only). */
    public String skipFileName = null;

    public String getName() {
        return "Learned from Original Tracks";
    }

    public CCLineGenerationResult generate(CCLineGeneratorContext context,
                                           CCLineProgressListener listener) throws Exception {
        if (context.trainingFolder == null)
            throw new Exception("No training folder selected.");

        if (listener != null)
            listener.progress(0, "Loading training tracks...");

        CCLineTrainingTrackLoader training = new CCLineTrainingTrackLoader();
        int nTracks = training.load(context.trainingFolder, skipFileName);
        if (nTracks == 0 || training.features.length == 0)
            throw new Exception("No readable F1CT*.DAT track files with best lines found in "
                                + context.trainingFolder.getPath());

        // z-score normalisation from the training set
        int nf = CCLineTrainingTrackLoader.FEATURES;
        double[] mean = new double[nf], sdev = new double[nf];
        for (int s = 0; s < training.features.length; s++)
            for (int f = 0; f < nf; f++)
                mean[f] += training.features[s][f];
        for (int f = 0; f < nf; f++)
            mean[f] /= training.features.length;
        for (int s = 0; s < training.features.length; s++)
            for (int f = 0; f < nf; f++) {
                double d = training.features[s][f] - mean[f];
                sdev[f] += d * d;
            }
        for (int f = 0; f < nf; f++) {
            sdev[f] = Math.sqrt(sdev[f] / training.features.length);
            if (sdev[f] < 1.0e-9)
                sdev[f] = 1.0;
        }

        CCLineTrackGeometry geo = context.geometry;
        int n = geo.segCount;
        double[][] query = CCLineTrainingTrackLoader.extractAllFeatures(geo);
        CCLineLateralProfile profile = new CCLineLateralProfile(n);

        // Distance-weighted k-NN per Seg
        double[] bestDist = new double[K];
        double[] bestTarget = new double[K];
        for (int i = 0; i < n; i++) {
            if (listener != null && (i & 0x3F) == 0) {
                if (listener.isCancelled())
                    return null;
                listener.progress(5 + i * 75 / n,
                    "Predicting line from " + nTracks + " track(s)...");
            }
            for (int k = 0; k < K; k++)
                bestDist[k] = Double.MAX_VALUE;
            for (int s = 0; s < training.features.length; s++) {
                double dist = 0;
                for (int f = 0; f < nf; f++) {
                    double d = (query[i][f] - training.features[s][f]) / sdev[f];
                    dist += d * d;
                }
                if (dist < bestDist[K - 1]) {
                    int k = K - 1;
                    while (k > 0 && bestDist[k - 1] > dist) {
                        bestDist[k] = bestDist[k - 1];
                        bestTarget[k] = bestTarget[k - 1];
                        k--;
                    }
                    bestDist[k] = dist;
                    bestTarget[k] = training.targets[s];
                }
            }
            double dSumW = 0, dSum = 0;
            for (int k = 0; k < K; k++) {
                double w = 1.0 / (1.0 + bestDist[k]);
                dSumW += w;
                dSum += w * bestTarget[k];
            }
            profile.offset[i] = (dSum / dSumW) * geo.usableBound[i];
        }

        // Smooth the k-NN prediction (it is noisy Seg-to-Seg) and keep a
        // margin for the quantizer's tracking error.
        double[] o = profile.offset;
        double[] smoothed = new double[n];
        for (int pass = 0; pass < 6; pass++) {
            for (int i = 0; i < n; i++) {
                int p2 = (i + n - 2) % n, p1 = (i + n - 1) % n;
                int n1 = (i + 1) % n, n2 = (i + 2) % n;
                smoothed[i] = (o[p2] + 2.0 * o[p1] + 3.0 * o[i] + 2.0 * o[n1] + o[n2]) / 9.0;
            }
            System.arraycopy(smoothed, 0, o, 0, n);
        }
        for (int i = 0; i < n; i++) {
            double dBound = geo.usableBound[i] * (1.0 - BOUND_MARGIN_FRACTION);
            if (o[i] > dBound)
                o[i] = dBound;
            else if (o[i] < -dBound)
                o[i] = -dBound;
        }

        if (listener != null) {
            if (listener.isCancelled())
                return null;
            listener.progress(85, "Quantising into CCLine segments...");
        }

        CCLine ccLine = new CCLineQuantizer(geo, profile, context.seamOvershoot).quantize();
        CCLineEvaluator.Score score = context.evaluator.score(ccLine);

        if (listener != null)
            listener.progress(100, "Done");

        CCLineGenerationResult result = new CCLineGenerationResult(ccLine, score);
        result.addWarning("Trained on " + nTracks + " track(s)");
        if (!score.isValid()) {
            if (score.outOfBounds > 0)
                result.addWarning(score.outOfBounds + " Seg(s) outside the drivable bound");
            if (score.uncovered > 0)
                result.addWarning(score.uncovered + " Seg(s) not covered by the line");
            if (score.unsafeRadius > 0)
                result.addWarning(score.unsafeRadius + " sector(s) with unsafe radius");
        }
        return result;
    }
}
