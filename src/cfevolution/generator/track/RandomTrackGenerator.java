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

import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

import cfevolution.data.track.Command;
import cfevolution.data.track.Track;
import cfevolution.data.track.TrackSegment;
import cfevolution.data.track.TrackSegments;

/**
    Random track layout generator (v1).

    Produces a flat, constant-width closed circuit: a long start/finish
    straight followed by randomly sized corners and straights, closed in
    heading and position by TrackLayoutClosure against the editor's own
    track compilation. The scratch Track (a reload of the donor file) is
    used for compile-in-the-loop closure so the live track is never
    touched from the background thread.

    The donor's required commands (pit connection, fence joins, marshal,
    view distances, palette) are harvested with their original arguments
    and re-attached around the new start/finish straight; the grid/S-F
    marking group (0x8A/0x8B) is re-attached at its donor distance before
    the S/F line, and scenery (0x80/0x81/0x82) at proportional lap
    positions. Countdown boards come from section flags on corner
    sections (bit 3 = 300/200/100, bit 6 = arrow), the originals'
    mechanism. Pit lane sections themselves are kept from the donor —
    the game computes pit placement from the sector carrying the entry
    command, so a donor pit on the guaranteed-long S/F straight stays
    valid. See docs/BESTLINE.md and DEVELOPMENT.md Session 3/5.
*/
public class RandomTrackGenerator {

    /** Result of one generation run. */
    public static class Result {
        /** Generated track segments (without the dummy terminator). */
        public final Vector segments = new Vector();
        public int totalTlu;
        public double closureGap;
        public long seed;
        /** TLU to add to/remove from the donor pit lane straights so the
            pit length matches the connect distance (PitLaneFitter). */
        public int pitDelta;
        /** How far the donor's pit lane sat from the donor's road, in world
            units — measured before generation overwrote the scratch track,
            and the distance the rebuilt pit lane is aimed at. */
        public double donorPitOffset;
        /** Closest the lap comes to itself, as a multiple of the full road
            width (parts less than 30 TLU apart along the track excluded).
            1.0 means the road surfaces just touch; the accepted layout is
            always above TrackLayoutClosure.CLEARANCE_ROAD_WIDTHS. */
        public double clearanceRoadWidths;
        public final Vector warnings = new Vector();
    }

    /** Commands harvested from the donor and re-attached to the new layout.
        Grouped by where they belong relative to the start/finish line. */
    private static final int[] SF_COMMANDS = {
        0x87,       // pit lane exit connect
        0xA3, 0xA4, // pit exit fence joins
        0x81, 0x82, // view distances
        0xAA,       // pit lane connect lengths / speed
        0xAB,       // required, purpose unknown
        0xAC        // palette
    };
    private static final int[] APPROACH_COMMANDS = {
        0x86,       // pit lane entry connect
        0xA1, 0xA2, // pit entry fence joins
        0xA8        // marshal with chequered flag
    };

    private static final int SEG_BUDGET = 1300; // hard engine budget is ~1420

    /** Donor lap-end window scanned for the grid/S-F marking group (the
        originals paint the grid ~40 TLU before the line). */
    private static final int SF_MARKING_WINDOW = 60;

    private final Track scratch;

    public RandomTrackGenerator(Track scratchTrack) {
        scratch = scratchTrack;
    }

    /** Default elevation range cap for callers that don't specify one
        (harnesses, older callers) — a donor-typical mid-range hill. */
    private static final double DEFAULT_MAX_ELEVATION_METRES = 25.0;

    /** Generates a closed random layout with the default elevation cap.
        Returns null only if cancelled. */
    public Result generate(long lSeed, int nTargetTlu, int nCorners,
                           TrackProgressListener listener) throws Exception {
        return generate(lSeed, nTargetTlu, nCorners, DEFAULT_MAX_ELEVATION_METRES, listener);
    }

    /** Generates a closed random layout. dMaxElevationMetres is the
        user-facing cap on peak-to-trough elevation over the whole lap
        (0 = flat, street-circuit style, e.g. Phoenix — the user's own
        reference for scenery that doesn't suit hills); values are
        clamped to ELEVATION_HARD_CEILING_M regardless, since generation
        without any cap was found to drive the pitch angle (and hence the
        Z position, stored as a 16-bit short like X/Y) into wraparound —
        a real overflow bug, not just "too hilly" (2026-07-22, from the
        user's Phoenix-scenery report). Returns null only if cancelled. */
    public Result generate(long lSeed, int nTargetTlu, int nCorners,
                           double dMaxElevationMetres,
                           TrackProgressListener listener) throws Exception {
        if (nTargetTlu > SEG_BUDGET)
            nTargetTlu = SEG_BUDGET;
        if (nTargetTlu < 300)
            nTargetTlu = 300;
        if (nCorners < 4)
            nCorners = 4;
        if (nCorners > 30)
            nCorners = 30;

        // Harvest donor properties BEFORE the scratch segments are replaced
        // — including where the donor's pit lane sits relative to its own
        // road, which the pit rebuild aims at later. Generated tracks inherit
        // the donor's width (dHalfRoad below reads it from Seg 0), so that
        // distance carries over. This MUST be taken here: generation uses the
        // track as its scratch pad, so by the time the layout is applied the
        // donor's own geometry is long gone.
        double dDonorPitOffset = cfevolution.generator.pitlane.PitLaneFitter.measurePitOffset(
            scratch.getTrackSegments(), scratch.getPitlaneSegments());
        Vector donorCommands = harvestCommands(scratch.getTrackSegments());
        Vector donorScenery = harvestPositionedCommands(scratch.getTrackSegments());
        int nDonorTotalTlu = donorTotalTlu(scratch.getTrackSegments());
        Vector donorSfMarkings = harvestSfMarkings(scratch.getTrackSegments(),
                                                   nDonorTotalTlu);
        int[] anLaneTemplate = harvestLaneTemplate(scratch.getTrackSegments());
        double[] adKerbTemplate = harvestKerbTemplate(scratch.getTrackSegments());
        double dSignFraction = harvestSignFraction(scratch.getTrackSegments());
        double dHalfRoad = 0;
        if (scratch.getTrackSegments().getMaxTrackSegIndex() > 0) {
            cfevolution.data.track.Seg seg0 = scratch.getTrackSegments().getSegAt(0);
            double wx = seg0.getTrackWidthX() + seg0.getExtraSideX();
            double wy = seg0.getTrackWidthY() + seg0.getExtraSideY();
            dHalfRoad = Math.sqrt(wx * wx / 64.0 + wy * wy / 64.0);
        }
        TrackSegment donorFirst = scratch.getTrackSegments().getAt(1);
        int nFenceR = donorFirst != null ? donorFirst.getFenceDistR() : 2;
        int nFenceL = donorFirst != null ? donorFirst.getFenceDistL() : 2;

        Random rand = new Random(lSeed);
        TrackLayoutClosure closure = new TrackLayoutClosure(scratch);

        // Overlap clearance is measured against the donor's own road
        // width; fall back to a typical original if the donor gave us
        // nothing to measure.
        double dClearanceHalfRoad = dHalfRoad > 0 ? dHalfRoad : 1500.0;

        Vector prims = null;
        boolean fAccepted = false;
        double dGap = Double.MAX_VALUE;
        double dClearance = 0;
        int nAttempt;
        int nRejectedOverlap = 0;
        for (nAttempt = 1; nAttempt <= 300 && !fAccepted; nAttempt++) {
            if (listener != null) {
                if (listener.isCancelled())
                    return null;
                listener.progress(Math.min(nAttempt / 4, 75), "Layout attempt " + nAttempt + "...");
            }
            prims = buildPrimitives(rand, nTargetTlu, nCorners);
            int nHeadResidual = TrackLayoutClosure.closeHeading(prims, 0x10000);
            if (Math.abs(nHeadResidual) > TrackLayoutClosure.HEADING_TOLERANCE)
                continue;
            if (totalTlu(prims) > SEG_BUDGET)
                continue;
            dGap = closure.closePosition(prims);
            if (dGap > TrackLayoutClosure.GAP_TARGET
                || totalTlu(prims) > SEG_BUDGET
                || closure.aspectRatio() > TrackLayoutClosure.MAX_ASPECT
                || closure.selfIntersects())
                continue;
            // Overlap gate: a layout whose centreline never crosses itself
            // can still run alongside itself closely enough for the two
            // road surfaces to merge — that is what the crossing test
            // above cannot see, and what produced the overlapping tracks
            // reported in-game (2026-07-27). Rejecting here simply draws
            // another layout, which is what the retry loop is for.
            dClearance = closure.minClearance(TrackLayoutClosure.CLEARANCE_WINDOW_TLU);
            if (dClearance < TrackLayoutClosure.requiredClearance(dClearanceHalfRoad)) {
                nRejectedOverlap++;
                continue;
            }
            fAccepted = true;
        }
        if (!fAccepted)
            throw new Exception("Could not generate an acceptable layout after "
                                + (nAttempt - 1) + " attempts (closure, compactness,"
                                + " self-intersection and overlap gates; "
                                + nRejectedOverlap + " rejected for overlapping themselves)");

        if (listener != null)
            listener.progress(80, "Layout closed (gap " + (long) dGap + " units)");

        // Materialise the final segment list with commands attached
        Result result = new Result();
        result.seed = lSeed;
        result.donorPitOffset = dDonorPitOffset;
        result.closureGap = dGap;
        result.totalTlu = totalTlu(prims);
        result.clearanceRoadWidths = dClearance / (2.0 * dClearanceHalfRoad);
        for (int i = 0; i < prims.size(); i++) {
            TrackLayoutClosure.Prim p = (TrackLayoutClosure.Prim) prims.get(i);
            TrackSegment ts = new TrackSegment();
            ts.setTlu(p.tlu);
            ts.setCurvature(p.curv);
            ts.setFenceDistR(nFenceR);
            ts.setFenceDistL(nFenceL);
            // Road signs are NOT decided here — see assignSigns below. They
            // depend on how much straight precedes the corner, which cannot
            // be known until the whole lap exists.
            //
            // CORNERS ARE EMITTED AS TWO SECTIONS, split at the apex. Kerb
            // flags are per SECTION, and the owner's kerbing pattern needs the
            // apex as a boundary: the inside kerb runs from the apex outwards,
            // not from the turn-in. A single-section corner cannot express
            // that. Splitting costs nothing geometrically — same curvature,
            // same total TLU — and it puts generated tracks at ~2.0 sections
            // per corner, inside the originals' own 1.50-3.00. splitSections()
            // later leaves curved sections alone, so this is the only place a
            // corner is ever divided.
            if (p.curv != 0 && p.tlu >= 2) {
                int nFirst = p.tlu / 2;
                ts.setTlu(nFirst);
                result.segments.add(ts);
                TrackSegment apexOn = new TrackSegment();
                apexOn.setTlu(p.tlu - nFirst);
                apexOn.setCurvature(p.curv);
                apexOn.setFenceDistR(nFenceR);
                apexOn.setFenceDistL(nFenceL);
                result.segments.add(apexOn);
                continue;
            }
            result.segments.add(ts);
        }
        // Pit connect placement: the pit lane's length must match the
        // entry->exit distance (originals: within -13..+1 TLU) or the
        // game computes garbage pit geometry and crashes on pit starts
        int nPitTlu = cfevolution.generator.pitlane.PitLaneFitter.pitTlu(
            scratch.getPitlaneSegments());
        cfevolution.generator.pitlane.PitLaneFitter.Plan pitPlan =
            cfevolution.generator.pitlane.PitLaneFitter.plan(
                ((TrackLayoutClosure.Prim) prims.get(0)).tlu,
                ((TrackLayoutClosure.Prim) prims.get(prims.size() - 1)).tlu,
                nPitTlu);
        result.pitDelta = pitPlan.pitDelta;
        if (pitPlan.pitDelta != 0)
            result.warnings.add("Pit lane length adjusted by " + pitPlan.pitDelta
                                + " TLU to fit the layout");

        attachCommands(result, donorCommands, pitPlan);
        attachScenery(result, donorScenery, nDonorTotalTlu);
        attachSfMarkings(result, donorSfMarkings, nDonorTotalTlu);

        // Rolling elevation (donor-calibrated; random tracks were dead
        // flat since v1). Heights are per-TLU gradients and the lap must
        // close on sum(len*h) ~ 0 — all 16 originals close within +-21.
        assignHeights(result, rand, dMaxElevationMetres);

        // Pit lane paint: the dashed in/out lane lines are MarkingType-3
        // 0x8A commands on the MAIN track ~5-12 TLU before each connect,
        // on the pit's side (all donors; in-game round 4 found them
        // missing). Reuse the donor's own dash count/lateral magnitude,
        // sign forced to our left-side pit.
        int nExitTlu = pitPlan.exitOffset;
        int nEntryTlu = result.totalTlu
            - ((TrackLayoutClosure.Prim) prims.get(prims.size() - 1)).tlu
            + pitPlan.entryOffset;
        attachPitLaneMarkings(result, anLaneTemplate, nEntryTlu, nExitTlu);

        // Donor-like short sections: long generated straights carried 40+
        // commands each and the game silently drops objects beyond a
        // per-section budget (proven by the ABTEST A/B: the same commands
        // through the same pipeline render fine on the donor's 67 short
        // sections — 2026-07-19). Splitting also creates the small wall
        // removal windows at the pit connects that the originals have
        // (without them the pit mouth is walled shut: invisible wall on
        // entry, wing damage on exit — in-game finding).
        // KERBS IN TWO PHASES, AROUND THE SPLIT, and the ordering is
        // load-bearing rather than tidy.
        //
        // planKerbs decides which corners are kerbed and writes the INSIDE
        // kerb straight away — corner sections are never split, so those flags
        // survive untouched. It also returns the absolute TLU at which each
        // EXIT kerb should stop, which splitSections then cuts at, so the kerb
        // can end exactly there instead of being rounded up to a whole 32 TLU
        // piece. applyExitKerbs writes the outside kerb afterwards, once those
        // pieces exist.
        //
        // The exit kerb cannot simply be written before the split: pieces after
        // the first get a FRESH flags word (only the wall-window bit), so a
        // kerb on a long straight would survive on the first piece and vanish
        // from the rest.
        Vector kerbPlans = planKerbs(result, adKerbTemplate, dHalfRoad, rand);
        splitSections(result, nExitTlu, nEntryTlu, kerbCutPositions(kerbPlans));
        applyExitKerbs(result, kerbPlans);

        // Road signs last: they only ever touch a corner's first section, which
        // the split leaves alone, and they OR into the flags word so they
        // compose with the kerbs.
        assignSigns(result, dSignFraction, rand);

        return result;
    }

    /** Donor-calibrated gradient cap (originals: typical max 43-98) —
        bounds the per-TLU pitch RATE for smoothness. This is independent
        of (and much smaller a constraint than) the overall elevation
        RANGE cap below: a modest gradient sustained over a long, mostly
        one-signed stretch still integrates into a huge total climb (see
        ELEVATION_HARD_CEILING_M). */
    private static final int MAX_GRADIENT = 60;

    /** World Z units per metre — same 1024-units-per-TLU-per-4.87m scale
        as X/Y (nPosChangeZ uses the identical LookupSinRaw*1024>>14
        formula), per CLAUDE.md. */
    private static final double Z_UNITS_PER_METRE = 210.0;

    /** Hard safety ceiling on peak-to-trough elevation, regardless of the
        user's requested cap: wPosZ is a 16-bit short like X/Y, and an
        uncapped sinusoidal height profile was found to drive it into
        wraparound (measured peak-to-trough of ~310m against a ~155m
        representable half-range — 2026-07-22). Comfortably under the
        original tracks' own observed maximum (~55m, Silverstone-style). */
    private static final double ELEVATION_HARD_CEILING_M = 90.0;

    /** Assigns a rolling elevation profile: 2-4 sinusoidal gradient waves
        with whole numbers of cycles per lap (so the elevation integral
        closes by construction), rounded per section, S/F and approach
        straights kept flat (the grid), then a closure pass keeps
        sum(len*h) within the originals' +-21 tolerance. The whole
        profile is then uniformly rescaled (re-simulating the actual
        pitch/Z stepping, not just the per-TLU gradient) until its real
        peak-to-trough range is within the requested cap — dMaxMetres <=
        0 means flat (no hills at all, e.g. for street-circuit-style
        scenery that doesn't suit elevation change, the user's own
        Phoenix example). */
    private void assignHeights(Result result, Random rand, double dMaxMetres) {
        int nCount = result.segments.size();
        double dCapMetres = Math.min(Math.max(dMaxMetres, 0.0), ELEVATION_HARD_CEILING_M);
        if (dCapMetres <= 0.5) {
            for (int i = 0; i < nCount; i++)
                ((TrackSegment) result.segments.get(i)).setHeightChange(0);
            return;
        }
        double dCapUnits = dCapMetres * Z_UNITS_PER_METRE;

        int nTotal = result.totalTlu;
        int nWaves = 2 + rand.nextInt(3);
        double[] adAmp = new double[nWaves];
        double[] adPhase = new double[nWaves];
        int[] anCycles = new int[nWaves];
        for (int w = 0; w < nWaves; w++) {
            adAmp[w] = 10.0 + rand.nextInt(30);
            adPhase[w] = rand.nextDouble();
            anCycles[w] = 1 + rand.nextInt(3);
        }

        double dScale = 1.0;
        for (int nPass = 0; nPass < 8; nPass++) {
            int nCum = 0;
            for (int i = 0; i < nCount; i++) {
                TrackSegment ts = (TrackSegment) result.segments.get(i);
                double dMid = (nCum + ts.getTlu() / 2.0) / nTotal;
                double h = 0.0;
                for (int w = 0; w < nWaves; w++)
                    h += adAmp[w] * Math.sin(2.0 * Math.PI * (anCycles[w] * dMid + adPhase[w]));
                h *= dScale;
                int nH = (int) Math.round(h);
                if (nH > MAX_GRADIENT) nH = MAX_GRADIENT;
                if (nH < -MAX_GRADIENT) nH = -MAX_GRADIENT;
                if (i == 0 || i == nCount - 1)
                    nH = 0; // grid and pit approach stay flat
                ts.setHeightChange(nH);
                nCum += ts.getTlu();
            }
            closeHeightSum(result);

            long[] anRange = simulateElevationRange(result.segments);
            long lActual = anRange[1] - anRange[0];
            if (lActual <= dCapUnits || lActual == 0)
                break;
            // The pitch angle scales linearly with the height array, but
            // Z (the sine of an accumulated angle) does not once angles
            // stop being small — converge with a damped ratio rather
            // than a single-shot linear guess.
            dScale *= Math.sqrt(dCapUnits / lActual);
        }
    }

    /** Nudges mid-lap sections by one gradient unit at a time until the
        length-weighted sum of heights (~ the net pitch angle at lap end)
        is within the originals' own +-16..21 tolerance. */
    private void closeHeightSum(Result result) {
        int nCount = result.segments.size();
        long lResidual = 0;
        for (int i = 0; i < nCount; i++) {
            TrackSegment ts = (TrackSegment) result.segments.get(i);
            lResidual += (long) ts.getTlu() * ts.getHeightChange();
        }
        int nGuard = 0;
        while (Math.abs(lResidual) > 16 && nGuard++ < 10000) {
            boolean fChanged = false;
            for (int i = 1; i < nCount - 1 && Math.abs(lResidual) > 16; i++) {
                TrackSegment ts = (TrackSegment) result.segments.get(i);
                int nLen = ts.getTlu();
                if (nLen > Math.abs(lResidual))
                    continue; // too coarse; a finer section will fix it
                if (lResidual > 0 && ts.getHeightChange() > -MAX_GRADIENT) {
                    ts.setHeightChange(ts.getHeightChange() - 1);
                    lResidual -= nLen;
                    fChanged = true;
                }
                else if (lResidual < 0 && ts.getHeightChange() < MAX_GRADIENT) {
                    ts.setHeightChange(ts.getHeightChange() + 1);
                    lResidual += nLen;
                    fChanged = true;
                }
            }
            if (!fChanged)
                break; // no section fine enough left; leave the residual
        }
    }

    /** Standalone elevation-range estimate: replicates the game's pitch
        accumulation + Z-position stepping (TCProcessTrackSectorPass1 —
        wTCAbsAngleX accumulates the per-TLU height field exactly like
        wTCAbsAngleZ accumulates curvature, then
        Z += LookupSinRaw(pitch)*1024>>14 per TLU). Simplified: skips the
        sector-boundary half-step phase alignment TrackSegments.java uses
        for curvature/height, since that shifts the profile by under one
        TLU and does not materially change the aggregate peak-to-trough
        range this is used to cap — this is an internal diagnostic for
        scaling, not a value stamped anywhere or gated for bit-fidelity. */
    private static long[] simulateElevationRange(Vector segments) {
        int nAngleX = 0;
        long lZ = 0, lMin = 0, lMax = 0;
        for (int s = 0; s < segments.size(); s++) {
            TrackSegment ts = (TrackSegment) segments.get(s);
            int nH = ts.getHeightChange();
            for (int i = 0; i < ts.getTlu(); i++) {
                nAngleX += nH;
                int nPosChangeZ = cfevolution.data.f1gp.F1GPMath.LookupSinRaw((short) nAngleX);
                lZ += (nPosChangeZ * 1024) >> 14;
                if (lZ < lMin) lMin = lZ;
                if (lZ > lMax) lMax = lZ;
            }
        }
        return new long[] { lMin, lMax };
    }

    /** Roads narrower than this (physical half-width, wCCLine units)
        get no kerbs at all (user rule: not on minimal-width streets). */
    private static final double KERB_MIN_HALF_ROAD = 1100.0;

    /** Donor kerb style: {fraction of corners kerbed, low-kerb fraction,
        mean kerb run TLU, share of kerb TLU sitting on STRAIGHT sections}.

        The last two were added 2026-09-16. Measured across the 16 originals,
        every one of them kerbs its straights — 16-66% of each circuit's kerb
        TLU, median ~43% — with mean run lengths of 13.5-326.5 TLU. The old
        code could not reproduce either, because it skipped straights outright:
        generated tracks came out at 0% on straights with runs of 11.7-22.3. */
    private double[] harvestKerbTemplate(TrackSegments donorSegments) {
        int nCorners = 0, nKerbRuns = 0, nKerbSecs = 0, nLow = 0;
        int nPrevSign = 0;
        boolean fPrevKerb = false;
        int nKerbTluStraight = 0, nKerbTluTotal = 0, nRunTluSum = 0;
        for (Enumeration e = donorSegments.elements(); e.hasMoreElements(); ) {
            TrackSegment ts = (TrackSegment) e.nextElement();
            if (ts.getTlu() <= 0)
                continue;
            int nSign = ts.getCurvature() == 0 ? 0 : (ts.getCurvature() > 0 ? 1 : -1);
            if (nSign != 0 && nSign != nPrevSign)
                nCorners++;
            nPrevSign = nSign;
            boolean fKerb = (ts.getFlags() & 0xC00) != 0;
            if (fKerb) {
                nKerbSecs++;
                nKerbTluTotal += ts.getTlu();
                nRunTluSum += ts.getTlu();
                if (nSign == 0)
                    nKerbTluStraight += ts.getTlu();
                if (!fPrevKerb)
                    nKerbRuns++;
                if ((ts.getFlags() & 0x4) != 0)
                    nLow++;
            }
            fPrevKerb = fKerb;
        }
        double dDensity = nCorners > 0 ? Math.min(1.0, (double) nKerbRuns / nCorners) : 0.0;
        double dLow = nKerbSecs > 0 ? (double) nLow / nKerbSecs : 0.0;
        double dRun = nKerbRuns > 0 ? (double) nRunTluSum / nKerbRuns : 0.0;
        double dStraight = nKerbTluTotal > 0
                         ? (double) nKerbTluStraight / nKerbTluTotal : 0.0;
        return new double[] { dDensity, dLow, dRun, dStraight };
    }

    /** One planned exit kerb: where the bend ends and where its outside kerb
        should stop, both as absolute TLU from the lap start, plus the flag bits
        to write. Positions are in TLU rather than section indices because
        splitSections renumbers every section between planning and applying. */
    private static class KerbPlan {
        int cornerEndTlu;
        int exitEndTlu;
        int flags;
    }

    /** Decides which corners are kerbed, writes the INSIDE kerb, and plans
        each exit kerb — to the pattern the project owner specified
        (2026-09-16):

          * the INSIDE of the corner is kerbed FROM THE APEX OUTWARDS, high
            (not the low-kerb bit) — which is why corners are emitted as two
            sections split at the apex, since kerb flags are per section;
          * where that inside kerb stops, an OUTSIDE kerb starts and runs onto
            the exit;
          * there is NO kerb on the outside of the corner entry.

        That pattern also explains the measurement it was reconciled against:
        the outside exit kerb is what puts 16-66% of the originals' kerb TLU on
        straight sections, which the previous corners-only code — which skipped
        `curvature == 0` outright — could never produce.

        Side convention (established from all 16 originals, 2026-07-19):
        positive curvature = right turn, so the inside is the right, 0x400; the
        outside of a right turn is the left, 0x800.

        Density is the donor's, drawn ONCE PER CORNER rather than once per
        section — with corners now spanning two sections a per-section draw
        would kerb half a corner at random. */
    private Vector planKerbs(Result result, double[] adTemplate, double dHalfRoad,
                             Random rand) {
        Vector plans = new Vector();
        if (dHalfRoad < KERB_MIN_HALF_ROAD || adTemplate[0] <= 0.0)
            return plans;
        int n = result.segments.size();
        double dMeanRun = adTemplate.length > 2 ? adTemplate[2] : 0.0;

        // absolute TLU at the start of each section
        int[] anStartTlu = new int[n];
        int nCum = 0;
        for (int i = 0; i < n; i++) {
            anStartTlu[i] = nCum;
            nCum += ((TrackSegment) result.segments.get(i)).getTlu();
        }

        for (int i = 0; i < n; i++) {
            TrackSegment ts = (TrackSegment) result.segments.get(i);
            if (ts.getCurvature() == 0)
                continue;
            TrackSegment prev = (TrackSegment) result.segments.get((i - 1 + n) % n);
            if (prev.getCurvature() != 0
                && (prev.getCurvature() > 0) == (ts.getCurvature() > 0))
                continue;                             // mid-corner, not a start
            int nEnd = i;
            for (int k = i + 1; k < n; k++) {
                TrackSegment u = (TrackSegment) result.segments.get(k);
                if (u.getCurvature() == 0
                    || (u.getCurvature() > 0) != (ts.getCurvature() > 0))
                    break;
                nEnd = k;
            }
            if (rand.nextDouble() >= adTemplate[0])
                continue;

            boolean fRightTurn = ts.getCurvature() > 0;
            int nInside  = fRightTurn ? 0x400 : 0x800;
            int nOutside = fRightTurn ? 0x800 : 0x400;

            // INSIDE, apex outwards: the second half of the bend. A corner
            // that is a single section (too short to split) is kerbed whole —
            // there is no apex boundary available on it.
            int nApex = (i == nEnd) ? i : i + (nEnd - i + 1) / 2;
            int nInsideTlu = 0;
            for (int k = nApex; k <= nEnd; k++) {
                TrackSegment u = (TrackSegment) result.segments.get(k);
                u.setFlags(u.getFlags() | nInside);   // high: no 0x4
                nInsideTlu += u.getTlu();
            }

            // OUTSIDE: length set by the donor's own SHARE of kerb TLU on
            // straights, not by its mean run length. Sizing it from the mean
            // run was the first attempt and it overshot badly — 70-87% of kerb
            // TLU landed on straights against the originals' 16-66% — because
            // a generated corner is short (~15-23 TLU, so ~8-12 TLU from the
            // apex out) and filling up to a 50-65 TLU run leaves nearly all of
            // it on the straight. The originals reach their share with much
            // more CORNER TLU kerbed, their bends being longer.
            //
            // Solving share = exit / (inside + exit) for the exit length
            // reproduces the balance directly. The donor's mean run is kept as
            // an upper bound, so a donor that kerbs briefly is not made to
            // kerb further than it does.
            double dShare = adTemplate.length > 3 ? adTemplate[3] : 0.4;
            if (dShare > 0.75) dShare = 0.75;
            if (dShare < 0.0)  dShare = 0.0;
            int nWant = (int) Math.round(nInsideTlu * dShare / (1.0 - dShare));
            int nCap = (int) Math.round(dMeanRun) - nInsideTlu;
            if (nCap > 0 && nWant > nCap)
                nWant = nCap;
            if (nWant <= 0)
                continue;

            // How much straight is actually available before the next bend or
            // an existing kerb — the plan must not run past either.
            int nAvail = 0;
            for (int k = nEnd + 1; k < nEnd + 1 + n; k++) {
                TrackSegment u = (TrackSegment) result.segments.get(k % n);
                if (u.getCurvature() != 0)
                    break;
                if ((u.getFlags() & 0xC00) != 0)
                    break;
                nAvail += u.getTlu();
            }
            if (nAvail <= 0)
                continue;
            if (nWant > nAvail)
                nWant = nAvail;

            KerbPlan plan = new KerbPlan();
            plan.cornerEndTlu = anStartTlu[nEnd]
                              + ((TrackSegment) result.segments.get(nEnd)).getTlu();
            plan.exitEndTlu = plan.cornerEndTlu + nWant;
            // low-kerb bit drawn once for the run, not per section
            plan.flags = nOutside | (rand.nextDouble() < adTemplate[1] ? 0x4 : 0);
            plans.add(plan);
        }
        return plans;
    }

    /** The TLU positions splitSections should cut at, so each exit kerb can
        end where it was planned to rather than at a 32 TLU piece boundary. */
    private int[] kerbCutPositions(Vector plans) {
        int[] an = new int[plans.size()];
        for (int i = 0; i < plans.size(); i++)
            an[i] = ((KerbPlan) plans.get(i)).exitEndTlu;
        return an;
    }

    /** Writes the outside exit kerbs planned by planKerbs, now that the split
        has created a section boundary at each planned end. Works in absolute
        TLU because the split renumbered everything. */
    private void applyExitKerbs(Result result, Vector plans) {
        int n = result.segments.size();
        int[] anStartTlu = new int[n];
        int nCum = 0;
        for (int i = 0; i < n; i++) {
            anStartTlu[i] = nCum;
            nCum += ((TrackSegment) result.segments.get(i)).getTlu();
        }
        for (int p = 0; p < plans.size(); p++) {
            KerbPlan plan = (KerbPlan) plans.get(p);
            for (int i = 0; i < n; i++) {
                TrackSegment ts = (TrackSegment) result.segments.get(i);
                if (ts.getCurvature() != 0)
                    continue;
                int nStart = anStartTlu[i];
                int nStop = nStart + ts.getTlu();
                if (nStart >= plan.cornerEndTlu && nStop <= plan.exitEndTlu)
                    ts.setFlags(ts.getFlags() | plan.flags);
            }
        }
    }

    /* ---------------------------------------------------------------- signs */

    /** TLU of approach the game needs per sign in a countdown.

        `TCPreprocessTrackSectorPass2` (0x8F294) walks BACKWARDS from the
        corner doing `sub bx, 18 * size Seg` before anchoring each sign in the
        sequence, so sign n sits 18*n Segs upstream. At 1 TLU = 16 ft = 4.88 m
        that is ~88 m a step, which is how the game approximates its
        "100/200/300" boards. Measured against the 16 originals, the approach
        straight before a marked corner matches: median 42 TLU for a one-sign
        arrow, 53 for the two-sign arrow/100, 114 for the three-sign
        countdown. */
    private static final int SIGN_TLU_PER_STEP = 18;

    /** Flag combinations, indexed by how many signs fit. The game turns these
        into sequences through its `roadSigns` table (asm:63402), 8 rows of
        object ids terminated by 0xFF:

            0x40        arrow
            0x80        arrow, 100m
            0x40|0x80   arrow, 200m, 100m
            0x8         300m, 200m, 100m
            0x8|0x40    300m, arrow, 100m

        **0x8|0x80 and 0x8|0x40|0x80 land on the two rows IDA labels "invalid
        combination" and draw NOTHING AT ALL** — they are not a richer
        countdown, so neither may ever be emitted.

        Chosen here: one sign -> arrow; two -> arrow/100; three -> the full
        300/arrow/100, which is what the originals overwhelmingly use for a
        long approach (24 corners, against 2 for the arrowless 300/200/100).
        The side is NOT ours to pick — the engine selects a different object
        set from the corner's own direction at 0x8F294. */
    private static final int[] SIGN_COMBO_BY_STEPS = { 0, 0x40, 0x80, 0x8 | 0x40 };

    /** Fraction of the donor's corners that carry any road sign. The
        originals mark 70 of 289 corners (24%), 219 carrying nothing, so
        markings are RARE and runway alone must not be treated as sufficient:
        plenty of unmarked corners have a long approach (up to 163 TLU). */
    private double harvestSignFraction(TrackSegments donorSegments) {
        int nCorners = 0, nMarked = 0, nPrevSign = 0;
        boolean fMarked = false;
        for (Enumeration e = donorSegments.elements(); e.hasMoreElements(); ) {
            TrackSegment ts = (TrackSegment) e.nextElement();
            if (ts.getTlu() <= 0)
                continue;
            int nSign = ts.getCurvature() == 0 ? 0 : (ts.getCurvature() > 0 ? 1 : -1);
            if (nSign != 0 && nSign != nPrevSign) {
                nCorners++;
                fMarked = false;
            }
            if (nSign != 0 && !fMarked && (ts.getFlags() & 0xC8) != 0) {
                nMarked++;
                fMarked = true;
            }
            nPrevSign = nSign;
        }
        return nCorners > 0 ? (double) nMarked / nCorners : 0.0;
    }

    /** Puts the countdown on the corners that have the runway for it.

        Two conditions, both taken from the originals. RUNWAY decides WHICH
        combination a corner can carry — 18 TLU of preceding straight per sign
        (see SIGN_TLU_PER_STEP), so 54 TLU for the full three-sign countdown.
        RATE decides HOW MANY corners are marked at all, from the donor's own
        fraction, because the originals leave 76% of corners bare.

        The corners with the most runway win, which is the same ordering the
        measurement found: signed corners in the originals average a 104 TLU
        approach against 23 for unsigned.

        Flags are ORed onto the corner's FIRST section (the game draws the
        signs upstream from there itself), so this composes with the kerb bits
        assignKerbs has already written. The runway walk WRAPS the lap, as the
        game's own placement loop does when it runs back past the S/F line. */
    private void assignSigns(Result result, double dFraction, Random rand) {
        int n = result.segments.size();
        if (n < 3 || dFraction <= 0.0)
            return;

        // Corner starts, in lap order, with the straight TLU before each
        int[] anStart = new int[n];
        int[] anRunway = new int[n];
        int nCorners = 0;
        for (int i = 0; i < n; i++) {
            TrackSegment ts = (TrackSegment) result.segments.get(i);
            if (ts.getCurvature() == 0)
                continue;
            int nPrev = (i - 1 + n) % n;
            TrackSegment prev = (TrackSegment) result.segments.get(nPrev);
            if (prev.getCurvature() != 0
                && (prev.getCurvature() > 0) == (ts.getCurvature() > 0))
                continue;               // mid-corner, not a corner start
            int nRun = 0;
            for (int k = 1; k < n; k++) {
                TrackSegment back = (TrackSegment)
                    result.segments.get((i - k + n) % n);
                if (back.getCurvature() != 0)
                    break;
                nRun += back.getTlu();
            }
            anStart[nCorners] = i;
            anRunway[nCorners] = nRun;
            nCorners++;
        }
        if (nCorners == 0)
            return;

        // How many to mark, at the donor's rate — at least one if it marks any
        int nTarget = (int) Math.round(dFraction * nCorners);
        if (nTarget <= 0)
            nTarget = 1;
        if (nTarget > nCorners)
            nTarget = nCorners;

        // Rank by runway, descending (simple selection: nCorners is ~12-30)
        boolean[] afTaken = new boolean[nCorners];
        for (int pick = 0; pick < nTarget; pick++) {
            int nBest = -1;
            for (int c = 0; c < nCorners; c++)
                if (!afTaken[c] && (nBest < 0 || anRunway[c] > anRunway[nBest]))
                    nBest = c;
            if (nBest < 0)
                break;
            afTaken[nBest] = true;

            int nSteps = anRunway[nBest] / SIGN_TLU_PER_STEP;
            if (nSteps <= 0)
                continue;               // no room even for one sign
            if (nSteps >= SIGN_COMBO_BY_STEPS.length)
                nSteps = SIGN_COMBO_BY_STEPS.length - 1;
            int nFlags = SIGN_COMBO_BY_STEPS[nSteps];
            if (nFlags == 0)
                continue;
            TrackSegment ts = (TrackSegment) result.segments.get(anStart[nBest]);
            ts.setFlags(ts.getFlags() | nFlags);
        }
    }

    /** Donor's dashed-lane template {dashes, |lateral|} from its first
        MarkingType-3 command; fallback 8 dashes at 400. Must run BEFORE
        the closure loop replaces the scratch's donor segments. */
    private int[] harvestLaneTemplate(TrackSegments donorSegments) {
        for (Enumeration e = donorSegments.elements(); e.hasMoreElements(); ) {
            TrackSegment ts = (TrackSegment) e.nextElement();
            for (Enumeration c = ts.getCommands().elements(); c.hasMoreElements(); ) {
                Command cmd = (Command) c.nextElement();
                if ((cmd.getType() == 0x8A || cmd.getType() == 0x8B)
                    && cmd.getParam(1) == 3)
                    // loaded command params are unsigned 16-bit; sign via short
                    return new int[] { Math.max(3, (short) cmd.getParam(2)),
                                       Math.max(90, Math.abs((short) cmd.getParam(3))) };
            }
        }
        return new int[] { 8, 400 };
    }

    /** Places the dashed pit in/out lane paint (MarkingType 3) before the
        entry and exit connects. Our generated pit is on the LEFT, so the
        lateral position is negative. */
    private void attachPitLaneMarkings(Result result, int[] anLaneTemplate,
                                       int nEntryTlu, int nExitTlu) {
        Command entryLane = new Command(0x8A, 0, 3, anLaneTemplate[0],
                                        -anLaneTemplate[1], 0, 257);
        Command exitLane = new Command(0x8A, 0, 3, anLaneTemplate[0],
                                       -anLaneTemplate[1], 0, 257);
        placeCommandAt(result, entryLane, nEntryTlu - 8);
        placeCommandAt(result, exitLane, nExitTlu - 5);
    }

    /** Longest section kept when splitting straights (donors average ~12
        TLU per section; command capacity is the binding reason). */
    private static final int MAX_STRAIGHT_SECTION = 32;
    /** Pit connect wall windows: [connect-2, connect+3), flags 0x2000
        (remove left wall — generated pits bulge left). */
    private static final int WALL_WINDOW_BEFORE = 2;
    private static final int WALL_WINDOW_AFTER = 3;

    private void splitSections(Result result, int nExitTlu, int nEntryTlu,
                               int[] anExtraCuts) {
        Vector split = new Vector();
        int nCum = 0;
        for (int i = 0; i < result.segments.size(); i++) {
            TrackSegment ts = (TrackSegment) result.segments.get(i);
            int nLen = ts.getTlu();
            if (ts.getCurvature() != 0 || nLen <= MAX_STRAIGHT_SECTION
                || nLen < 2 * WALL_WINDOW_BEFORE) {
                split.add(ts);
                nCum += nLen;
                continue;
            }
            // Cut points within this segment (relative): wall windows
            // that fall inside it, then even chunks between
            java.util.TreeSet cuts = new java.util.TreeSet();
            int[] anConnects = { nExitTlu, nEntryTlu };
            for (int c = 0; c < anConnects.length; c++) {
                int nRel = anConnects[c] - nCum;
                if (nRel - WALL_WINDOW_BEFORE > 0 && nRel + WALL_WINDOW_AFTER < nLen) {
                    cuts.add(new Integer(nRel - WALL_WINDOW_BEFORE));
                    cuts.add(new Integer(nRel + WALL_WINDOW_AFTER));
                }
            }
            // Extra cuts requested by a caller — planKerbs asks for one where
            // each exit kerb should stop, so the kerb can end at the length the
            // donor's own straight-kerb share implies instead of being rounded
            // to a whole MAX_STRAIGHT_SECTION piece. Without this the section
            // grid is 32 TLU while a generated corner's inside kerb is ~10, so
            // one piece overshoots threefold (measured: 69-80% of kerb TLU on
            // straights against the originals' 16-66%) and the nearest
            // alternative is no exit kerb at all.
            if (anExtraCuts != null) {
                for (int c = 0; c < anExtraCuts.length; c++) {
                    int nRel = anExtraCuts[c] - nCum;
                    if (nRel > 0 && nRel < nLen)
                        cuts.add(new Integer(nRel));
                }
            }
            // Even chunks: subdivide every stretch between existing cuts
            java.util.Vector bounds = new java.util.Vector();
            bounds.add(new Integer(0));
            for (java.util.Iterator it = cuts.iterator(); it.hasNext(); )
                bounds.add(it.next());
            bounds.add(new Integer(nLen));
            java.util.TreeSet all = new java.util.TreeSet(bounds);
            for (int b = 0; b + 1 < bounds.size(); b++) {
                int a = ((Integer) bounds.get(b)).intValue();
                int z = ((Integer) bounds.get(b + 1)).intValue();
                int nSpan = z - a;
                if (nSpan > MAX_STRAIGHT_SECTION) {
                    int nPieces = (nSpan + MAX_STRAIGHT_SECTION - 1) / MAX_STRAIGHT_SECTION;
                    for (int k = 1; k < nPieces; k++)
                        all.add(new Integer(a + nSpan * k / nPieces));
                }
            }

            // Materialise the pieces, migrating commands by offset
            Integer[] anBounds = (Integer[]) all.toArray(new Integer[0]);
            for (int b = 0; b + 1 < anBounds.length; b++) {
                int a = anBounds[b].intValue();
                int z = anBounds[b + 1].intValue();
                TrackSegment piece = (b == 0) ? ts : new TrackSegment();
                if (b > 0) {
                    piece.setCurvature(0);
                    piece.setFenceDistR(ts.getFenceDistR());
                    piece.setFenceDistL(ts.getFenceDistL());
                    // gradient is per-TLU: every piece carries it
                    piece.setHeightChange(ts.getHeightChange());
                }
                piece.setTlu(z - a);
                // Wall window? (piece covers a connect's window exactly)
                for (int c = 0; c < anConnects.length; c++) {
                    int nRel = anConnects[c] - nCum;
                    if (a == nRel - WALL_WINDOW_BEFORE && z == nRel + WALL_WINDOW_AFTER)
                        piece.setFlags(piece.getFlags() | 0x2000);
                }
                if (b > 0)
                    split.add(piece);
                else
                    split.add(ts);
            }
            // Command migration: collect the original's commands once,
            // then deal to pieces by offset
            Vector cmds = new Vector(ts.getCommands());
            ts.setCommands(new Vector());
            int nPieceIndexBase = split.size() - (anBounds.length - 1);
            for (int ci = 0; ci < cmds.size(); ci++) {
                Command cmd = (Command) cmds.get(ci);
                int nOff = Math.min(cmd.getParam(0), nLen - 1);
                for (int b = 0; b + 1 < anBounds.length; b++) {
                    int a = anBounds[b].intValue();
                    int z = anBounds[b + 1].intValue();
                    if (nOff >= a && nOff < z) {
                        cmd.setParam(0, nOff - a);
                        ((TrackSegment) split.get(nPieceIndexBase + b)).getCommands().add(cmd);
                        break;
                    }
                }
            }
            nCum += nLen;
        }
        result.segments.clear();
        for (int i = 0; i < split.size(); i++)
            result.segments.add(split.get(i));
    }

    // ------------------------------------------------------------------

    /** Random primitive sequence: S/F straight, then corners alternating
        with straights. Curvatures within the engine limit; the sequence
        is intentionally rough — closure adjusts it afterwards. */
    private Vector buildPrimitives(Random rand, int nTargetTlu, int nCorners) {
        Vector prims = new Vector();
        int nSfLen = 120 + rand.nextInt(60);
        prims.add(new TrackLayoutClosure.Prim(nSfLen, 0));
        int nBudget = nTargetTlu - nSfLen;

        // Rough split of the remaining budget over corners + straights
        int nPerCorner = nBudget / nCorners;
        for (int i = 0; i < nCorners; i++) {
            // corner: total turn 30..150 degrees, either direction but
            // biased so the lap winds one full turn to the right
            int nTurnDeg = 30 + rand.nextInt(121);
            boolean fRight = rand.nextInt(10) < 7;
            int nTurn = (int) Math.round(nTurnDeg * 65536.0 / 360.0) * (fRight ? 1 : -1);
            int nCornerTlu = 6 + rand.nextInt(25);
            int nCurv = nTurn / nCornerTlu;
            if (nCurv > 0x2000) nCurv = 0x2000;
            if (nCurv < -0x2000) nCurv = -0x2000;
            if (nCurv == 0) nCurv = fRight ? 100 : -100;
            prims.add(new TrackLayoutClosure.Prim(nCornerTlu, nCurv));

            int nStraight = Math.max(4, nPerCorner - nCornerTlu
                                        + rand.nextInt(21) - 10);
            if (nStraight > 120) // long straights caused Session 7 elongation
                nStraight = 120;
            prims.add(new TrackLayoutClosure.Prim(nStraight, 0));
        }
        // The approach straight carries the pit entry group and the grid
        // markings (donor grids sit ~40 TLU before S/F) — keep it long
        // enough for both
        TrackLayoutClosure.Prim last =
            (TrackLayoutClosure.Prim) prims.get(prims.size() - 1);
        if (last.tlu < 60)
            last.tlu = 60;
        return prims;
    }

    private static int totalTlu(Vector prims) {
        int nTotal = 0;
        for (int i = 0; i < prims.size(); i++)
            nTotal += ((TrackLayoutClosure.Prim) prims.get(i)).tlu;
        return nTotal;
    }

    /** First instance of each command type found on the donor's segments. */
    private Vector harvestCommands(TrackSegments donorSegments) {
        Vector harvested = new Vector();
        boolean[] afSeen = new boolean[256];
        for (Enumeration e = donorSegments.elements(); e.hasMoreElements(); ) {
            TrackSegment ts = (TrackSegment) e.nextElement();
            for (Enumeration c = ts.getCommands().elements(); c.hasMoreElements(); ) {
                Command cmd = (Command) c.nextElement();
                int nType = cmd.getType();
                if (nType >= 0 && nType < 256 && !afSeen[nType]) {
                    afSeen[nType] = true;
                    harvested.add(copyCommand(cmd));
                }
            }
        }
        return harvested;
    }

    private static Command copyCommand(Command cmd) {
        return new Command(cmd.getType(), cmd.getParam(0), cmd.getParam(1),
                           cmd.getParam(2), cmd.getParam(3), cmd.getParam(4),
                           cmd.getParam(5));
    }

    /** Attaches the harvested donor commands around the new S/F straight:
        exit-side group on the first segment, entry-side group on the last
        (the approach straight). Offsets (param 0) are clamped into the
        carrying segment's length; the pit connects (0x86/0x87) get the
        fitter's computed offsets so the pit length matches. */
    private void attachCommands(Result result, Vector donorCommands,
                                cfevolution.generator.pitlane.PitLaneFitter.Plan pitPlan) {
        TrackSegment sfSegment = (TrackSegment) result.segments.get(0);
        TrackSegment approach = (TrackSegment) result.segments.get(result.segments.size() - 1);

        for (int i = 0; i < SF_COMMANDS.length; i++) {
            Command cmd = findCommand(donorCommands, SF_COMMANDS[i]);
            if (cmd == null) {
                result.warnings.add("Donor has no 0x"
                    + Integer.toHexString(SF_COMMANDS[i]).toUpperCase() + " command");
                continue;
            }
            if (cmd.getType() == 0x87)
                cmd.setParam(0, pitPlan.exitOffset);
            clampOffset(cmd, sfSegment.getTlu());
            sfSegment.getCommands().add(cmd);
        }
        for (int i = 0; i < APPROACH_COMMANDS.length; i++) {
            Command cmd = findCommand(donorCommands, APPROACH_COMMANDS[i]);
            if (cmd == null) {
                result.warnings.add("Donor has no 0x"
                    + Integer.toHexString(APPROACH_COMMANDS[i]).toUpperCase() + " command");
                continue;
            }
            if (cmd.getType() == 0x86)
                cmd.setParam(0, pitPlan.entryOffset);
            clampOffset(cmd, approach.getTlu());
            approach.getCommands().add(cmd);
        }
    }

    /** A donor command with its lap-relative TLU position. */
    private static class PositionedCommand {
        final Command cmd;
        final int tlu;
        PositionedCommand(Command cmd, int tlu) { this.cmd = cmd; this.tlu = tlu; }
    }

    /** All donor scenery/view commands (0x80/0x81/0x82) with positions. */
    private Vector harvestPositionedCommands(TrackSegments donorSegments) {
        Vector list = new Vector();
        int nCumTlu = 0;
        for (Enumeration e = donorSegments.elements(); e.hasMoreElements(); ) {
            TrackSegment ts = (TrackSegment) e.nextElement();
            for (Enumeration c = ts.getCommands().elements(); c.hasMoreElements(); ) {
                Command cmd = (Command) c.nextElement();
                int nType = cmd.getType();
                if (nType == 0x80 || nType == 0x81 || nType == 0x82)
                    list.add(new PositionedCommand(copyCommand(cmd),
                                                   nCumTlu + cmd.getParam(0)));
            }
            nCumTlu += ts.getTlu();
        }
        return list;
    }

    private static int donorTotalTlu(TrackSegments donorSegments) {
        int nTotal = 0;
        for (Enumeration e = donorSegments.elements(); e.hasMoreElements(); )
            nTotal += ((TrackSegment) e.nextElement()).getTlu();
        return nTotal;
    }

    /** Restores donor-density scenery and draw distance ("mist" fix):
        every donor 0x80/0x81/0x82 command is re-attached at the same
        proportional lap position on the generated track. The game anchors
        one object per Seg (TCAnchorObject stores a single bObjectID), so
        colliding object TLUs are nudged to a nearby free TLU instead of
        silently overwriting each other. */
    private void attachScenery(Result result, Vector donorScenery,
                               int nDonorTotalTlu) {
        if (donorScenery.isEmpty() || nDonorTotalTlu <= 0)
            return;
        boolean[] afObjectTlu = new boolean[result.totalTlu];
        for (int i = 0; i < donorScenery.size(); i++) {
            PositionedCommand pc = (PositionedCommand) donorScenery.get(i);
            int nNewTlu = (int) ((long) pc.tlu * result.totalTlu / nDonorTotalTlu);
            if (pc.cmd.getType() == 0x80) {
                nNewTlu = findFreeObjectTlu(afObjectTlu, nNewTlu);
                if (nNewTlu < 0)
                    continue;
                afObjectTlu[nNewTlu] = true;
            }
            placeCommandAt(result, copyCommand(pc.cmd), nNewTlu);
        }
    }

    /** Nearest lap TLU (within 3) not already carrying an object. */
    private static int findFreeObjectTlu(boolean[] afTaken, int nTlu) {
        int n = afTaken.length;
        for (int d = 0; d <= 3; d++) {
            int nUp = ((nTlu + d) % n + n) % n;
            if (!afTaken[nUp])
                return nUp;
            int nDown = ((nTlu - d) % n + n) % n;
            if (!afTaken[nDown])
                return nDown;
        }
        return -1;
    }

    /** Grid + start/finish line markings: every 0x8A/0x8B command in the
        donor's final stretch before the S/F line. The originals draw the
        grid as a marking group there — e.g. Monaco carries 0x8A (x +768)
        and 0x8B (x -768) with 13 dotted lines each (the two grid columns)
        about 40 TLU out, plus a single-line 0x8B on the line itself. */
    private Vector harvestSfMarkings(TrackSegments donorSegments,
                                     int nDonorTotalTlu) {
        Vector list = new Vector();
        int nCumTlu = 0;
        for (Enumeration e = donorSegments.elements(); e.hasMoreElements(); ) {
            TrackSegment ts = (TrackSegment) e.nextElement();
            for (Enumeration c = ts.getCommands().elements(); c.hasMoreElements(); ) {
                Command cmd = (Command) c.nextElement();
                int nType = cmd.getType();
                int nTlu = nCumTlu + cmd.getParam(0);
                if ((nType == 0x8A || nType == 0x8B)
                    && nDonorTotalTlu - nTlu <= SF_MARKING_WINDOW)
                    list.add(new PositionedCommand(copyCommand(cmd), nTlu));
            }
            nCumTlu += ts.getTlu();
        }
        return list;
    }

    /** Re-attaches the S/F marking group at the same distance before the
        new S/F line, i.e. on the approach straight. */
    private void attachSfMarkings(Result result, Vector markings,
                                  int nDonorTotalTlu) {
        if (markings.isEmpty()) {
            result.warnings.add("Donor has no grid markings near start/finish");
            return;
        }
        for (int i = 0; i < markings.size(); i++) {
            PositionedCommand pc = (PositionedCommand) markings.get(i);
            placeCommandAt(result, copyCommand(pc.cmd),
                           result.totalTlu - (nDonorTotalTlu - pc.tlu));
        }
    }

    /** Attaches a command to the segment containing the given lap TLU. */
    private void placeCommandAt(Result result, Command cmd, int nTlu) {
        int nTotal = result.totalTlu;
        if (nTotal <= 0)
            return;
        nTlu = ((nTlu % nTotal) + nTotal) % nTotal;
        int nCum = 0;
        for (int i = 0; i < result.segments.size(); i++) {
            TrackSegment ts = (TrackSegment) result.segments.get(i);
            if (nTlu < nCum + ts.getTlu()) {
                int nOffset = nTlu - nCum;
                if (nOffset > 255)
                    nOffset = 255;
                cmd.setParam(0, nOffset);
                ts.getCommands().add(cmd);
                return;
            }
            nCum += ts.getTlu();
        }
    }

    private static Command findCommand(Vector commands, int nType) {
        for (int i = 0; i < commands.size(); i++) {
            Command cmd = (Command) commands.get(i);
            if (cmd.getType() == nType)
                return cmd;
        }
        return null;
    }

    private static void clampOffset(Command cmd, int nSegmentTlu) {
        if (cmd.getParam(0) >= nSegmentTlu)
            cmd.setParam(0, Math.max(0, nSegmentTlu - 1));
    }
}
