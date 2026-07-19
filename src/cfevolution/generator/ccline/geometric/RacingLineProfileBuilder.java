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

package cfevolution.generator.ccline.geometric;

import java.util.Vector;

import cfevolution.generator.ccline.CCLineLateralProfile;
import cfevolution.generator.ccline.CCLineTrackGeometry;

/**
    Constructed out-in-out racing line profile.

    Corners are detected from the track's own curvature; each significant
    corner gets three keyframes — entry rail on the outside edge, apex on
    the inside edge, exit rail on the outside edge — all at a standoff
    from the PHYSICAL road edge (geometry.physicalBound, i.e. width/8 in
    wCCLine units; the game's usableBound is ~8x the real road and must
    not be used here). Keyframes are joined with smoothstep easing and a
    light smoothing pass, which yields gentle diagonals across the
    straights between corners — the approach positioning the AI needs to
    carry speed (2026-07-19 in-game feedback).
*/
public class RacingLineProfileBuilder {

    /** Track heading change per TLU that counts as corner core. */
    private static final int CORNER_RATE = 200;
    /** Total turn below which a corner is ignored (line stays put). */
    private static final int MIN_CORNER_TURN = 0x600; // ~8 degrees
    /** Corner runs separated by fewer TLU than this merge (compound). */
    private static final int MERGE_GAP = 6;
    /** Longest approach/exit transition (TLU). */
    private static final int MAX_TRANSITION = 48;
    /** Opposite-sign corners closer than this get their amplitude scaled
        down (ramping with the gap): a full out-in-out swing through a
        tight S demands transients the quantizer cannot deliver inside a
        narrow road — F1CT12's 65-left-into-79-right on a +-1124 road was
        the diagnosed runaway site (2026-07-19). */
    private static final int S_COMBO_GAP = 24;
    private static final double S_COMBO_MIN_SCALE = 0.35;
    /** Cap on the apex inset as a fraction of the corner's own turn
        radius. Cutting inside a corner tightens the line's radius below
        the track's; at max-curvature hairpins an 80%-of-road inset
        demands turn rates the sector primitives cannot deliver and the
        quantizer walk explodes mid-corner (F1CT03/04/11 diagnosis,
        2026-07-19). 0.25 keeps the line's radius within ~25% of the
        track's. */
    private static final double APEX_INSET_RADIUS_FRACTION = 0.25;

    /** Builds the lateral profile. standoffFraction is the fixed distance
        kept from the physical road edge, as a fraction of the half-width
        (e.g. 0.15 leaves 15% of the half-road as margin). */
    public static CCLineLateralProfile build(CCLineTrackGeometry geo,
                                             double standoffFraction) {
        int n = geo.segCount;

        // --- corner runs from track heading deltas -------------------
        int[] dA = new int[n];
        for (int i = 0; i < n; i++)
            dA[i] = (short) (geo.angleZ[(i + 1) % n] - geo.angleZ[i]);

        Vector corners = new Vector(); // int[]{start, end, totalTurn}
        int i = 0;
        while (i < n) {
            if (Math.abs(dA[i]) < CORNER_RATE) {
                i++;
                continue;
            }
            int sign = dA[i] > 0 ? 1 : -1;
            int start = i, turn = 0;
            int gap = 0, end = i;
            while (i < n) {
                if (Math.abs(dA[i]) >= CORNER_RATE && (dA[i] > 0 ? 1 : -1) == sign) {
                    turn += dA[i];
                    end = i;
                    gap = 0;
                }
                else if (++gap > MERGE_GAP)
                    break;
                i++;
            }
            if (Math.abs(turn) >= MIN_CORNER_TURN)
                corners.add(new int[] { start, end, turn });
        }

        CCLineLateralProfile profile = new CCLineLateralProfile(n);
        double[] o = profile.offset;
        if (corners.isEmpty())
            return profile; // straight-ish track: centreline

        // --- keyframes: entry rail, apex, exit rail per corner --------
        // Keyframes are (tlu, offsetFraction of the local rail); actual
        // offsets are scaled by the local physical bound at evaluation
        // so width changes are respected.
        int nCorners = corners.size();
        double dRail = 1.0 - Math.max(0.05, standoffFraction);

        // Amplitude scale per corner: tight opposite-sign combinations
        // (S/chicane) ramp down toward S_COMBO_MIN_SCALE
        double[] adScale = new double[nCorners];
        for (int c = 0; c < nCorners; c++)
            adScale[c] = 1.0;
        for (int c = 0; c < nCorners; c++) {
            int[] run = (int[]) corners.get(c);
            int[] next = (int[]) corners.get((c + 1) % nCorners);
            if ((run[2] > 0) == (next[2] > 0))
                continue; // same-direction corners keep full amplitude
            int gap = ((next[0] - run[1]) % n + n) % n;
            if (gap >= S_COMBO_GAP)
                continue;
            double dScale = Math.max(S_COMBO_MIN_SCALE, (double) gap / S_COMBO_GAP);
            adScale[c] = Math.min(adScale[c], dScale);
            adScale[(c + 1) % nCorners] = Math.min(adScale[(c + 1) % nCorners], dScale);
        }

        // Keyframes in strict lap order: entry rail, apex, exit rail per
        // corner. Transitions are clamped to strictly less than half the
        // gap to the neighbouring corner so consecutive keyframes can
        // never cross — crossed keyframes made paintSpan run the long
        // way around the lap and shredded half the tracks (2026-07-19).
        int[] kfTlu = new int[nCorners * 3];
        double[] kfFrac = new double[nCorners * 3];
        for (int c = 0; c < nCorners; c++) {
            int[] run = (int[]) corners.get(c);
            int mid = midpointOfTurn(dA, run[0], run[1], n);
            double inside = insideSign(geo, run[0], run[1], mid, n);

            int[] prev = (int[]) corners.get((c + nCorners - 1) % nCorners);
            int[] next = (int[]) corners.get((c + 1) % nCorners);
            int gapBefore = ((run[0] - prev[1]) % n + n) % n;
            int gapAfter = ((next[0] - run[1]) % n + n) % n;
            int tIn = Math.min(MAX_TRANSITION, Math.max(0, gapBefore / 2 - 1));
            int tOut = Math.min(MAX_TRANSITION, Math.max(0, gapAfter / 2 - 1));

            double dAmp = dRail * adScale[c];
            // Apex inset limited by the corner's own turn radius: the
            // line's radius must stay near the track's at tight corners
            int nRunLen = ((run[1] - run[0]) % n + n) % n + 1;
            double dRate = Math.abs((double) run[2]) / nRunLen; // units/TLU
            double dApexAmp = dAmp;
            if (dRate > 1.0) {
                double dTrackRadius = 65536.0 * 1024.0 / (2.0 * Math.PI * dRate);
                double dInsetCap = APEX_INSET_RADIUS_FRACTION * dTrackRadius
                                   / Math.max(1.0, geo.physicalBound[mid]);
                dApexAmp = Math.min(dApexAmp, dInsetCap);
            }
            kfTlu[c * 3] = (run[0] - tIn + n) % n;
            kfFrac[c * 3] = -inside * dAmp;          // entry rail (outside)
            kfTlu[c * 3 + 1] = mid;
            kfFrac[c * 3 + 1] = inside * dApexAmp;   // apex (inside)
            kfTlu[c * 3 + 2] = (run[1] + tOut) % n;
            kfFrac[c * 3 + 2] = -inside * dAmp;      // exit rail (outside)
        }

        // --- rasterise: one smoothstep span between each consecutive
        // keyframe pair around the lap (each Seg painted exactly once) ---
        double[] frac = new double[n];
        int nKf = nCorners * 3;
        for (int k = 0; k < nKf; k++) {
            int kNext = (k + 1) % nKf;
            paintSpan(frac, kfTlu[k], kfTlu[kNext], kfFrac[k], kfFrac[kNext], n);
        }

        for (int k = 0; k < n; k++)
            o[k] = frac[k] * geo.physicalBound[k];

        // Light smoothing (kink removal at keyframes), then re-clamp
        double[] sm = new double[n];
        for (int pass = 0; pass < 4; pass++) {
            for (int k = 0; k < n; k++) {
                int p2 = (k + n - 2) % n, p1 = (k + n - 1) % n;
                int n1 = (k + 1) % n, n2 = (k + 2) % n;
                sm[k] = (o[p2] + 2.0 * o[p1] + 3.0 * o[k] + 2.0 * o[n1] + o[n2]) / 9.0;
            }
            System.arraycopy(sm, 0, o, 0, n);
        }
        for (int k = 0; k < n; k++) {
            double dBound = geo.physicalBound[k] * dRail;
            if (o[k] > dBound) o[k] = dBound;
            else if (o[k] < -dBound) o[k] = -dBound;
        }
        return profile;
    }

    /** TLU where half the corner's total turn is done (curvature-weighted
        apex position). */
    private static int midpointOfTurn(int[] dA, int start, int end, int n) {
        long total = 0;
        for (int k = start; ; k = (k + 1) % n) {
            total += Math.abs(dA[k]);
            if (k == end) break;
        }
        long acc = 0;
        for (int k = start; ; k = (k + 1) % n) {
            acc += Math.abs(dA[k]);
            if (acc * 2 >= total || k == end)
                return k;
        }
    }

    /** Which offset sign is the inside of the corner: the chord midpoint
        between entry and exit lies inside the curve; project it onto the
        lateral axis at the apex. */
    private static double insideSign(CCLineTrackGeometry geo, int start, int end,
                                     int mid, int n) {
        double chordX = (geo.posX[start] + geo.posX[(end + 1) % n]) / 2.0;
        double chordY = (geo.posY[start] + geo.posY[(end + 1) % n]) / 2.0;
        double dx = chordX - geo.posX[mid];
        double dy = chordY - geo.posY[mid];
        double rad = geo.angleZ[mid] * 2.0 * Math.PI / 65536.0;
        // lateral axis: worldPoint adds offset*cos to X and -offset*sin to Y
        double proj = dx * Math.cos(rad) - dy * Math.sin(rad);
        return proj >= 0 ? 1.0 : -1.0;
    }

    /** Writes a smoothstep-eased span fracFrom -> fracTo over [from..to]
        (cyclic; inclusive endpoints). */
    private static void paintSpan(double[] frac, int from, int to,
                                  double fracFrom, double fracTo, int n) {
        int len = ((to - from) % n + n) % n;
        if (len == 0) {
            frac[from] = fracTo;
            return;
        }
        for (int s = 0; s <= len; s++) {
            double t = (double) s / len;
            double e = t * t * (3.0 - 2.0 * t); // smoothstep
            frac[(from + s) % n] = fracFrom + (fracTo - fracFrom) * e;
        }
    }
}
