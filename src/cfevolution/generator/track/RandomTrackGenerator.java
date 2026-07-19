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

    /** Generates a closed random layout. Returns null only if cancelled. */
    public Result generate(long lSeed, int nTargetTlu, int nCorners,
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
        Vector donorCommands = harvestCommands(scratch.getTrackSegments());
        Vector donorScenery = harvestPositionedCommands(scratch.getTrackSegments());
        int nDonorTotalTlu = donorTotalTlu(scratch.getTrackSegments());
        Vector donorSfMarkings = harvestSfMarkings(scratch.getTrackSegments(),
                                                   nDonorTotalTlu);
        TrackSegment donorFirst = scratch.getTrackSegments().getAt(1);
        int nFenceR = donorFirst != null ? donorFirst.getFenceDistR() : 2;
        int nFenceL = donorFirst != null ? donorFirst.getFenceDistL() : 2;

        Random rand = new Random(lSeed);
        TrackLayoutClosure closure = new TrackLayoutClosure(scratch);

        Vector prims = null;
        boolean fAccepted = false;
        double dGap = Double.MAX_VALUE;
        int nAttempt;
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
            fAccepted = dGap <= TrackLayoutClosure.GAP_TARGET
                && totalTlu(prims) <= SEG_BUDGET
                && closure.aspectRatio() <= TrackLayoutClosure.MAX_ASPECT
                && !closure.selfIntersects();
        }
        if (!fAccepted)
            throw new Exception("Could not generate an acceptable layout after "
                                + (nAttempt - 1) + " attempts (closure, compactness and"
                                + " self-intersection gates)");

        if (listener != null)
            listener.progress(80, "Layout closed (gap " + (long) dGap + " units)");

        // Materialise the final segment list with commands attached
        Result result = new Result();
        result.seed = lSeed;
        result.closureGap = dGap;
        result.totalTlu = totalTlu(prims);
        for (int i = 0; i < prims.size(); i++) {
            TrackLayoutClosure.Prim p = (TrackLayoutClosure.Prim) prims.get(i);
            TrackSegment ts = new TrackSegment();
            ts.setTlu(p.tlu);
            ts.setCurvature(p.curv);
            ts.setFenceDistR(nFenceR);
            ts.setFenceDistL(nFenceL);
            // Countdown boards and arrows are section flags drawn by the
            // game on the approach, set on the corner's first section
            // (originals' pattern: bit 3 = 300/200/100, bit 6 = arrow)
            long lTurn = Math.abs((long) p.tlu * p.curv);
            if (p.curv != 0 && lTurn >= 0x2000) {
                int nFlags = 0x8;
                if (lTurn >= 0x4000)
                    nFlags |= 0x40;
                ts.setFlags(nFlags);
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

        // Donor-like short sections: long generated straights carried 40+
        // commands each and the game silently drops objects beyond a
        // per-section budget (proven by the ABTEST A/B: the same commands
        // through the same pipeline render fine on the donor's 67 short
        // sections — 2026-07-19). Splitting also creates the small wall
        // removal windows at the pit connects that the originals have
        // (without them the pit mouth is walled shut: invisible wall on
        // entry, wing damage on exit — in-game finding).
        int nExitTlu = pitPlan.exitOffset;
        int nEntryTlu = result.totalTlu
            - ((TrackLayoutClosure.Prim) prims.get(prims.size() - 1)).tlu
            + pitPlan.entryOffset;
        splitSections(result, nExitTlu, nEntryTlu);

        return result;
    }

    /** Longest section kept when splitting straights (donors average ~12
        TLU per section; command capacity is the binding reason). */
    private static final int MAX_STRAIGHT_SECTION = 32;
    /** Pit connect wall windows: [connect-2, connect+3), flags 0x2000
        (remove left wall — generated pits bulge left). */
    private static final int WALL_WINDOW_BEFORE = 2;
    private static final int WALL_WINDOW_AFTER = 3;

    private void splitSections(Result result, int nExitTlu, int nEntryTlu) {
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
