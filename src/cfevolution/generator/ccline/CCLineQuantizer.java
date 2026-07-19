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

import cfevolution.data.track.CCLine;
import cfevolution.data.track.CCLineSegment;

/**
    Turns a continuous lateral offset profile into integer CCLine segments.

    Works sequentially, mirroring how the game walks the line: at each
    point it tries candidate sector lengths and both sector types —
    straight (radius 0 + heading correction) and tangent arc (radius) —
    seeds the parameters analytically from the profile geometry, refines
    them by local search, and scores every candidate by actually
    simulating it with CCLineSimulator from a snapshot of the walk state.
    The candidate with the lowest per-TLU tracking error is emitted.

    Guarantees on output segments: lengths 1-255 (save-safe), radii never
    trigger the game's Pythagoras failure (verified by simulation — the
    static rule |raw| > length * 128 is only the worst case), wide (0x40)
    type used only when a radius does not fit 16 bits, never combined with
    the first (0x80) type, and total TLU = track TLU + seamOvershoot.
*/
public class CCLineQuantizer {

    private static final double HUGE_ERROR = 1.0e12;
    private static final int[] CANDIDATE_LENGTHS = { 255, 192, 128, 96, 64, 48, 32, 24, 16, 12, 8 };
    private static final double[] RADIUS_VARIANTS = { 1.0, 0.85, 1.2, 0.7, 1.5 };
    private static final int[] CORRECTION_VARIANTS = { 0, -32, 32, -96, 96 };
    /** Error added per unit of straight-sector heading kick (see bestForLength).
        Value chosen by parameter sweep over all 16 original tracks
        (2026-07-12): (0.05, 0.1) was the only combination passing both the
        round-trip gate and geometric validity on every track. */
    private static final double KICK_PENALTY = 0.05;
    private static double kickPenalty(int nCorr) {
        return Math.abs(nCorr) * KICK_PENALTY;
    }
    /** Weight of the end-heading mismatch term (angle units → error units).
        Raising this to parity with position (1.0) was tried 2026-07-18:
        no systematic gain — some tracks' worst jump halved, others
        (F1CT05) more than doubled; the greedy walk is chaotically
        sensitive to it. Kick removal is done structurally by the
        polisher's kick→arc conversion instead. */
    private static final double HEADING_WEIGHT = 0.1;
    /** Relative-heading rotation (line vs track) a sector may perform per
        TLU without penalty. Hand-tuned lines' sector-average rates stay
        under ~1600 angle units/TLU (measured across all 16 originals,
        2026-07-18); the degenerate "whip arcs" the greedy fit used to
        turn hairpins with run 2x-4x beyond. Excess rotation is charged
        like a kick — arcs previously turned for free, which is exactly
        why whip arcs beat honest corner arcs at corner entries. */
    private static final double REL_RATE_ALLOWANCE = 1600.0;
    /** Candidates are hard-rejected beyond this multiple of the physical
        road half-width (deep-kerb range; Monaco's hand-tuned line peaks
        at 1.36x). */
    private static final double KERB_HARD_LIMIT = 1.45;

    /** Profile heading rate (angle units per TLU) above which a Seg counts
        as the core of a corner. 90 deg over 20 TLU is ~820/TLU; gentle
        sweeps and straight-line drift stay well below. */
    private static final double TURN_RATE_THRESHOLD = 400.0;
    /** Detected corners extend outward while the rate stays above this —
        captures the gradual ease-in (hand-tuned lines turn in ~5 TLU
        before the track bends). */
    private static final double TURN_RATE_EASE = 150.0;
    /** Turning/straight runs shorter than this are absorbed (noise). */
    private static final int MIN_RUN = 3;

    private final CCLineTrackGeometry geo;
    private final CCLineSimulator simulator;
    private final double[] profile;      // target offset per Seg
    private final double[] profileHeading; // profile chord heading per Seg (game angle units)
    private final int seamOvershoot;
    private final int[] tluToBoundary;   // per Seg: TLUs until the next turn-in/turn-out

    // Scratch stamping area shared by all candidate simulations
    private final CCLineSimulator.Result scratch;

    // Seam targeting during windowed repair (see quantizeFrom overload)
    private CCLineSimulator.State seamTarget = null;
    private int nSeamTlu = -1;
    /** Weight of the seam position error (world units → error units). */
    private static final double SEAM_POS_WEIGHT = 3.0;
    /** Weight of the seam heading error (angle units → error units). */
    private static final double SEAM_HEADING_WEIGHT = 2.0;

    /** Diagnostic hook: when DEBUG_OUT is set, every candidate evaluated
        while the walk is inside [DEBUG_FROM, DEBUG_TO] (TLU) is logged
        with its error components. Harness use only. */
    public static java.io.PrintStream DEBUG_OUT = null;
    public static int DEBUG_FROM = -1, DEBUG_TO = -1;

    public CCLineQuantizer(CCLineTrackGeometry geometry, CCLineLateralProfile lateralProfile,
                           int seamOvershoot) {
        geo = geometry;
        simulator = new CCLineSimulator(geometry);
        profile = lateralProfile.offset;
        this.seamOvershoot = seamOvershoot;
        scratch = new CCLineSimulator.Result(geometry.segCount);

        // Local chord heading of the profile at every Seg (over a +-4 Seg
        // window) — used to keep candidate sectors heading-continuous with
        // where the profile is going, which damps tracking oscillation.
        int n = geo.segCount;
        profileHeading = new double[n];
        for (int i = 0; i < n; i++) {
            int a = (i + n - 4) % n, b = (i + 4) % n;
            double[] pa = worldPoint(a, profile[a]);
            double[] pb = worldPoint(b, profile[b]);
            profileHeading[i] = Math.atan2(pb[0] - pa[0], pb[1] - pa[1]) * 65536.0 / (2.0 * Math.PI);
        }

        // Corner turn-in points from the profile's heading rate. A sector
        // may never cross one: a straight carried even 2 TLU past a
        // turn-in leaves only degenerate whip-arcs and monster kicks as
        // followers (Session 9/11 hairpin diagnosis). Aligning sector ends
        // with turn-ins lets arcs take whole corners, as hand-tuned lines
        // do. Turn-OUTS deliberately do not constrain: hand-tuned corner
        // arcs run past the geometric corner exit into the straight.
        double[] rate = new double[n];
        boolean[] turning = new boolean[n];
        for (int i = 0; i < n; i++) {
            rate[i] = Math.abs(wrapAngle(profileHeading[(i + 1) % n] - profileHeading[i]));
            turning[i] = rate[i] > TURN_RATE_THRESHOLD;
        }
        // Extend each corner outward while the profile still curves gently
        // (ease-in/ease-out capture)
        boolean[] extended = (boolean[]) turning.clone();
        for (int i = 0; i < n; i++) {
            if (!turning[i])
                continue;
            for (int d = 1; d <= 8; d++) {
                int back = (i - d + n) % n;
                if (turning[back] || rate[back] <= TURN_RATE_EASE)
                    break;
                extended[back] = true;
            }
            for (int d = 1; d <= 8; d++) {
                int fwd = (i + d) % n;
                if (turning[fwd] || rate[fwd] <= TURN_RATE_EASE)
                    break;
                extended[fwd] = true;
            }
        }
        turning = extended;
        // Hysteresis: absorb runs shorter than MIN_RUN into their surroundings
        boolean fChanged = true;
        while (fChanged) {
            fChanged = false;
            for (int i = 0; i < n; i++) {
                int prev = (i + n - 1) % n;
                if (turning[i] == turning[prev])
                    continue; // not a run start
                int nRun = runLength(turning, i);
                if (nRun < MIN_RUN && nRun < n) {
                    for (int k = 0; k < nRun; k++)
                        turning[(i + k) % n] = turning[prev];
                    fChanged = true;
                }
            }
        }
        // Distance from every Seg to the next turn-in (straight->turning
        // transition), skipping the one we may be standing on
        tluToBoundary = new int[n];
        for (int i = 0; i < n; i++) {
            int d = 1;
            while (d < n) {
                int j = (i + d) % n;
                int jPrev = (j - 1 + n) % n;
                if (turning[j] && !turning[jPrev])
                    break;
                d++;
            }
            tluToBoundary[i] = d;
        }
    }

    private static int runLength(boolean[] state, int nFrom) {
        int n = state.length;
        int d = 1;
        while (d < n && state[(nFrom + d) % n] == state[nFrom])
            d++;
        return d;
    }

    /** Builds the quantised CCLine. */
    public CCLine quantize() {
        CCLine ccLine = new CCLine();
        CCLineSimulator.State st = simulator.initialState();
        quantizeFrom(st, geo.segCount + seamOvershoot, true, ccLine);
        return ccLine;
    }

    /** Greedy-quantizes nWindowTlu more TLU from the given walk state,
        appending the emitted sectors to out and advancing the state.
        Used by quantize() for the whole lap and by CCLineWindowRepair to
        rebuild a bounded downstream window after an edit. */
    public void quantizeFrom(CCLineSimulator.State st, int nWindowTlu,
                             boolean fFirst, CCLine out) {
        quantizeFrom(st, nWindowTlu, fFirst, out, null);
    }

    /** As above, additionally steering the window's final sector to end
        as close as possible to seamState (the walk state the sectors
        after the window originally started from). Without this the
        untouched suffix runs from a slightly shifted state and arcs near
        their Pythagoras margin clamp — the repair seam must close in
        position AND heading, not just track the profile. */
    public void quantizeFrom(CCLineSimulator.State st, int nWindowTlu,
                             boolean fFirst, CCLine out,
                             CCLineSimulator.State seamState) {
        int nTargetTlu = st.walkedTlu + nWindowTlu;
        seamTarget = seamState;
        nSeamTlu = nTargetTlu;

        while (st.walkedTlu < nTargetTlu) {
            int nRemaining = nTargetTlu - st.walkedTlu;
            Candidate best = null;

            // Corner alignment: candidates may not cross the next profile
            // turn-in, and the exact run up to it is itself a candidate
            // (so the walk can arrive at a corner precisely).
            int nToBoundary = tluToBoundary[st.walkedTlu % geo.segCount];

            for (int li = 0; li < CANDIDATE_LENGTHS.length; li++) {
                int nLen = CANDIDATE_LENGTHS[li];
                if (nLen > nRemaining || nLen > nToBoundary)
                    continue;
                Candidate c = bestForLength(st, nLen, fFirst);
                if (best == null || c.errorPerTlu < best.errorPerTlu)
                    best = c;
                // A long sector that tracks well is always preferable;
                // stop scanning shorter ones once a good long fit exists.
                if (best.errorPerTlu < 64.0 && best.length >= 64)
                    break;
            }
            if (nToBoundary <= Math.min(nRemaining, 255)) {
                Candidate c = bestForLength(st, nToBoundary, fFirst);
                if (best == null || c.errorPerTlu < best.errorPerTlu)
                    best = c;
            }
            if (nRemaining < 8 || best == null) {
                // Tail shorter than the smallest candidate: emit directly.
                Candidate tail = bestForLength(st, nRemaining, fFirst);
                if (best == null || tail.length == nRemaining)
                    best = tail;
            }

            for (int si = 0; si < best.segments.length; si++) {
                out.add(best.segments[si]);
                simulator.runSegment(best.segments[si], st, scratch);
            }
            fFirst = false;
        }
        seamTarget = null;
    }

    /** Extracts the per-Seg offsets of an existing simulation as a profile
        (used for round-trip testing and to seed refinement). */
    public static CCLineLateralProfile profileFromSimulation(CCLineSimulator.Result r) {
        CCLineLateralProfile p = new CCLineLateralProfile(r.ccLine.length);
        for (int i = 0; i < r.ccLine.length; i++)
            p.offset[i] = r.ccLine[i];
        return p;
    }

    // ------------------------------------------------------------------

    private static class Candidate {
        CCLineSegment[] segments;  // 1 (plain) or 2 (align-kick + arc)
        int length;
        double errorPerTlu;
    }

    /** Best straight/arc candidate of the given length from the given state. */
    private Candidate bestForLength(CCLineSimulator.State st, int nLen, boolean fFirst) {
        int nStartTlu = st.walkedTlu;

        // World geometry of the current position and the profile target
        double[] p0 = worldPoint(st.segIndex, st.wSegPosX);
        int nEndSeg = segAt(nStartTlu + nLen);
        double[] pT = worldPoint(nEndSeg, profile[nEndSeg]);
        double dx = pT[0] - p0[0];
        double dy = pT[1] - p0[1];

        Candidate best = new Candidate();
        best.errorPerTlu = HUGE_ERROR;

        // --- Straight candidates: heading kicks towards two aim points.
        // Direct aim (sector end) corrects the whole lateral error in one
        // sector — essential for recovery, but oscillates when tracking.
        // Look-ahead aim (pure pursuit, beyond the end) converges smoothly
        // but cannot recover from large errors. Seed kicks from both and
        // let the simulated error metric choose. Kicks are instant heading
        // jumps the AI has to absorb, so they carry a mild size penalty —
        // arcs (tangent-continuous) win unless a straight genuinely fits.
        int nLookAhead = Math.max(32, nLen / 2);
        int nAimSeg = segAt(nStartTlu + nLen + nLookAhead);
        double[] pAim = worldPoint(nAimSeg, profile[nAimSeg]);
        double dDirectAngle = Math.atan2(dx, dy) * 65536.0 / (2.0 * Math.PI);
        double dAheadAngle = Math.atan2(pAim[0] - p0[0], pAim[1] - p0[1]) * 65536.0 / (2.0 * Math.PI);
        int nKickDirect = (int) (short) (int) Math.round(dDirectAngle - st.wTmpAngleZ);
        int nKickAhead = (int) (short) (int) Math.round(dAheadAngle - st.wTmpAngleZ);
        for (int ci = 0; ci < CORRECTION_VARIANTS.length; ci++) {
            int nCorr = (short) (nKickDirect + CORRECTION_VARIANTS[ci]);
            trySegments(best, st, single(makeSegment(fFirst, nLen, nCorr, 0)), nLen,
                        kickPenalty(nCorr));
            if (nKickAhead != nKickDirect) {
                nCorr = (short) (nKickAhead + CORRECTION_VARIANTS[ci]);
                trySegments(best, st, single(makeSegment(fFirst, nLen, nCorr, 0)), nLen,
                            kickPenalty(nCorr));
            }
        }

        // --- Arc candidates: tangent circle through the target point ---
        // Direction of current heading in world units (X advances by sin, Y by cos)
        addArcCandidates(best, st, fFirst, nLen, 0, st.wTmpAngleZ, dx, dy, 0);

        // --- Arc candidates seeded from the profile's own turn rate.
        // The chord seed degenerates for turns beyond ~120 degrees (a
        // hairpin's chord is short), which previously left corners to be
        // "turned" by straights with huge instant heading kicks — the
        // in-game car-thrown-wide behaviour (2026-07-15). Radius from the
        // profile heading change over the sector covers exactly that case.
        double dTurn = wrapAngle(profileHeading[nEndSeg] - profileHeading[st.segIndex]);
        long lCurvSeed = 0;
        if (Math.abs(dTurn) > 256.0) {
            double dThetaRad = Math.abs(dTurn) * 2.0 * Math.PI / 65536.0;
            lCurvSeed = Math.round((nLen * 1024.0 / dThetaRad) / 8.0);
            if (lCurvSeed != 0)
                addArcCandidates(best, st, fFirst, nLen, 0, st.wTmpAngleZ, dx, dy, lCurvSeed);
        }

        // --- Composite candidates: 1-TLU align kick + tangent arc.
        // Arcs alone cannot correct a heading error (they are tangent-
        // continuous), which otherwise forces the fit into kicky straights.
        // Hand-tuned lines use exactly this move: a small heading
        // correction, then a curve.
        if (nLen >= 12) {
            int nAlign = (int) (short) (int) Math.round(
                wrapAngle(profileHeading[st.segIndex] - st.wTmpAngleZ));
            if (nAlign != 0) {
                short wAligned = (short) (st.wTmpAngleZ + nAlign);
                addArcCandidates(best, st, fFirst, nLen, nAlign, wAligned, dx, dy, 0);
                if (lCurvSeed != 0)
                    addArcCandidates(best, st, fFirst, nLen, nAlign, wAligned, dx, dy, lCurvSeed);
            }
        }

        // Fallback so a candidate always exists: keep current heading
        if (best.errorPerTlu >= HUGE_ERROR)
            trySegments(best, st, single(makeSegment(fFirst, nLen, 0, 0)), nLen, 0.0);
        if (best.segments == null) {
            best.segments = single(makeSegment(fFirst, nLen, 0, 0));
            best.length = nLen;
            best.errorPerTlu = HUGE_ERROR;
        }
        return best;
    }

    /** Adds tangent-arc candidates for the given start heading. When
        nAlignKick is nonzero the arc is preceded by a 1-TLU straight that
        kicks the heading, and the arc covers the remaining length. The
        radius seed comes from the chord to the target point, or from
        lSeedOverride (profile turn rate) when nonzero. */
    private void addArcCandidates(Candidate best, CCLineSimulator.State st, boolean fFirst,
                                  int nLen, int nAlignKick, short wHeading,
                                  double dx, double dy, long lSeedOverride) {
        int nArcLen = (nAlignKick != 0) ? nLen - 1 : nLen;
        long lRawSeed;
        if (lSeedOverride != 0) {
            lRawSeed = lSeedOverride;
        }
        else {
            double dRad = wHeading * 2.0 * Math.PI / 65536.0;
            double dirX = Math.sin(dRad), dirY = Math.cos(dRad);
            double cross = dirX * dy - dirY * dx;
            if (Math.abs(cross) <= 1.0e-6)
                return;
            double dWorldRadius = (dx * dx + dy * dy) / (2.0 * cross);
            lRawSeed = Math.round(dWorldRadius / 8.0);
        }
        double dPenalty = kickPenalty(nAlignKick);
        for (int ri = 0; ri < RADIUS_VARIANTS.length; ri++) {
            long lRaw = Math.round(lRawSeed * RADIUS_VARIANTS[ri]);
            for (int sign = 0; sign < 2; sign++) {
                long r = (sign == 0) ? lRaw : -lRaw;
                // Radii too small for their arc are rejected by simulation
                // (Pythagoras clamp check in trySegments), not statically —
                // original tracks use radii below the static worst-case rule.
                if (r == 0 || Math.abs(r) > 0x3FFFFFFFL)
                    continue;
                if (nAlignKick == 0) {
                    if (fFirst && (r > Short.MAX_VALUE || r < Short.MIN_VALUE))
                        continue; // first segment carries only a 16-bit radius
                    trySegments(best, st, single(makeSegment(fFirst, nLen, 0, r)), nLen, dPenalty);
                }
                else {
                    CCLineSegment[] pair = new CCLineSegment[] {
                        makeSegment(fFirst, 1, nAlignKick, 0),
                        makeSegment(false, nArcLen, 0, r)
                    };
                    trySegments(best, st, pair, nLen, dPenalty);
                }
            }
            if (lRawSeed == 0)
                break;
        }
    }

    private static CCLineSegment[] single(CCLineSegment seg) {
        return new CCLineSegment[] { seg };
    }

    /** Simulates a candidate (one or two sectors) from a state copy and
        records it if better. */
    private void trySegments(Candidate best, CCLineSimulator.State st,
                             CCLineSegment[] segs, int nLen, double dPenalty) {
        CCLineSimulator.State trial = st.copy();
        int nClampsBefore = scratch.clampCount;
        for (int si = 0; si < segs.length; si++)
            simulator.runSegment(segs[si], trial, scratch);

        // Followability: charge relative-heading rotation beyond the
        // per-TLU allowance like a kick (see REL_RATE_ALLOWANCE).
        int iStart = segAt(st.walkedTlu);
        double dRelStart = wrapAngle((short) (st.wTmpAngleZ - geo.angleZ[iStart]));
        double dRelChange = Math.abs(wrapAngle(
            wrapAngle((short) (trial.wTmpAngleZ - geo.angleZ[segAt(st.walkedTlu + nLen - 1)]))
            - dRelStart));
        dPenalty += Math.max(0.0, dRelChange - REL_RATE_ALLOWANCE * nLen) * KICK_PENALTY;

        double dSumSq = 0.0;
        // A fired Pythagoras clamp means this radius breaks the game's
        // math at this point — reject regardless of tracking error.
        boolean fInvalid = scratch.clampCount > nClampsBefore;
        // Off-road ratchet against the PHYSICAL road edge (the old
        // geo.usableBound check was the game's ~8x-inflated tolerance and
        // never fired — 2026-07-19 scale correction). A hard wall alone
        // caused runaways: once outside, recovery candidates were invalid
        // too and the walk careened in the invalid tier. So: candidates
        // may never be FURTHER out than both the deep-kerb limit and the
        // walk's current excursion — going out is rejected, coming back
        // is always legal.
        int iStartSeg = segAt(st.walkedTlu);
        double dEntryRatio = Math.abs(st.wSegPosX) / Math.max(1.0, geo.physicalBound[iStartSeg]);
        double dRatioLimit = Math.max(KERB_HARD_LIMIT, dEntryRatio + 0.05);
        for (int t = st.walkedTlu; t < st.walkedTlu + nLen; t++) {
            int i = segAt(t);
            double dErr = scratch.ccLine[i] - profile[i];
            dSumSq += dErr * dErr;
            if (Math.abs(scratch.ccLine[i]) >= geo.physicalBound[i] * dRatioLimit)
                fInvalid = true;
        }
        // Anchor the sector end so state stays on the profile
        int iEnd = segAt(st.walkedTlu + nLen - 1);
        double dEndErr = scratch.ccLine[iEnd] - profile[iEnd];
        dSumSq += 3.0 * dEndErr * dEndErr;

        // Heading continuity at the sector end: ending aligned with where
        // the profile is going keeps the next sector's kick small.
        double dHeadErr = wrapAngle(trial.wTmpAngleZ - profileHeading[iEnd]) * HEADING_WEIGHT;
        dSumSq += dHeadErr * dHeadErr;

        // Seam closure during windowed repair: the candidate reaching the
        // splice point must restore the original walk state there
        if (seamTarget != null && trial.walkedTlu == nSeamTlu) {
            double dSeamPos = (trial.wSegPosX - seamTarget.wSegPosX) * SEAM_POS_WEIGHT;
            double dSeamHead = wrapAngle((short) (trial.wTmpAngleZ - seamTarget.wTmpAngleZ))
                               * SEAM_HEADING_WEIGHT;
            dSumSq += dSeamPos * dSeamPos + dSeamHead * dSeamHead;
        }

        double dError = fInvalid ? HUGE_ERROR + dSumSq
                                 : Math.sqrt(dSumSq / (nLen + 4)) + dPenalty;

        if (DEBUG_OUT != null && st.walkedTlu >= DEBUG_FROM && st.walkedTlu <= DEBUG_TO) {
            StringBuffer sb = new StringBuffer();
            for (int si = 0; si < segs.length; si++) {
                CCLineSegment sg = segs[si];
                long r = CCLineEvaluator.rawRadius(sg);
                int ci = ((sg.getType() & 0x80) != 0) ? 1 : 0;
                sb.append(r == 0 ? "STR" : "ARC").append("[l=").append(sg.getTlu())
                  .append(" c=").append(sg.getParam(ci)).append(" r=").append(r).append("] ");
            }
            double dTrackRms = Math.sqrt((dSumSq - 3.0 * dEndErr * dEndErr - dHeadErr * dHeadErr)
                                          / Math.max(nLen, 1));
            DEBUG_OUT.printf("tlu=%d len=%d %-46s rms=%7.0f end=%7.0f head=%7.0f pen=%6.0f inv=%b total=%.0f%n",
                st.walkedTlu, nLen, sb.toString(), dTrackRms, dEndErr,
                dHeadErr / HEADING_WEIGHT, dPenalty, fInvalid, dError);
        }

        if (dError < best.errorPerTlu) {
            best.errorPerTlu = dError;
            best.segments = segs;
            best.length = nLen;
        }
    }

    /** Builds a segment of the right type for the given parameters. */
    private CCLineSegment makeSegment(boolean fFirst, int nLen, int nCorrection, long lRawRadius) {
        CCLineSegment seg;
        if (fFirst) {
            seg = new CCLineSegment(0x80);
            seg.setParam(0, clampShort(Math.round(profile[0])));
            seg.setParam(1, clampShort(nCorrection));
            seg.setParam(2, clampShort(lRawRadius));
        }
        else if (lRawRadius > Short.MAX_VALUE || lRawRadius < Short.MIN_VALUE) {
            // wide radius segment
            seg = new CCLineSegment(0x40);
            seg.setParam(0, clampShort(nCorrection));
            seg.setParam(1, (int) (lRawRadius >> 16));
            seg.setParam(2, (int) (short) (lRawRadius & 0xFFFF));
        }
        else {
            seg = new CCLineSegment(0x00);
            seg.setParam(0, clampShort(nCorrection));
            seg.setParam(1, (int) lRawRadius);
        }
        seg.setTlu(nLen);
        return seg;
    }

    private int segAt(int nTlu) {
        return nTlu % geo.segCount;
    }

    /** World position of a lateral offset at a Seg — same rotation as the
        game's ProcessCCLineSector / TrackPanel.ccLineWorldPos. */
    private double[] worldPoint(int nSeg, double dOffset) {
        double dSegPosY = ((long) (dOffset * geo.angleZChangeMulHalfPI[nSeg])) >> 15;
        double dRad = geo.angleZ[nSeg] * 2.0 * Math.PI / 65536.0;
        double cosA = Math.cos(dRad), sinA = Math.sin(dRad);
        return new double[] {
            geo.posX[nSeg] + dOffset * cosA + dSegPosY * sinA,
            geo.posY[nSeg] + dSegPosY * cosA - dOffset * sinA
        };
    }

    private static int clampShort(long v) {
        if (v > Short.MAX_VALUE) return Short.MAX_VALUE;
        if (v < Short.MIN_VALUE) return Short.MIN_VALUE;
        return (int) v;
    }

    /** Wraps an angle difference into -32768..32767 (16-bit game angle). */
    private static double wrapAngle(double d) {
        d = d % 65536.0;
        if (d > 32768.0) d -= 65536.0;
        if (d < -32768.0) d += 65536.0;
        return d;
    }
}
