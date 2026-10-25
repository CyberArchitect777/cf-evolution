/*
 * CF Evolution: An editor for Formula One Grand Prix/World Circuit
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
 */

package cfevolution.generator.pitlane;

import cfevolution.data.track.*;
import java.util.Vector;

/**
 * The two places where the pit lane visibly leaves and rejoins the road (the
 * "pit mouths"), and what a track needs there for the game to draw them.
 *
 * Every original track has the same group at each mouth (measured 2026-10-25,
 * harnesses/PitMouth.java), a few TLU inside the 0x86/0x87 connects, where the
 * lane's centre is first nine tenths of a road half-width from the road's:
 * <ul>
 * <li>an object using object setting 0 (entry) or 1 (exit). The game does not
 *     draw these as objects: reaching one draws the PIT LANE scene
 *     (sub_1929A -> sub_19075). Without them the pit lane is never drawn from
 *     the track. Objects 0/1 sat 6 before to 8 after the entry mouth, and
 *     16 before to 4 after the exit mouth;</li>
 * <li>a one-TLU section with the pit-side wall hidden (flag 0x2000 left,
 *     0x1000 right);</li>
 * <li>the pit-join commands, 0xA1 and 0xA2 at the entry and 0xA4 and 0xA3 at
 *     the exit (TCPitlaneJoin), which mark the Seg where the pit wall meets
 *     the track wall, on the side the header's pit flags give.</li>
 * </ul>
 *
 * Random tracks before 2026-10-25 put the whole group at the connects (or, for
 * the exit joins, at the start line), on the wrong side for twelve of the
 * donors. GP2 conversions made with ArgTrack carry no objects 0/1 at all; see
 * hasPitObjects for why the checker does not add them there.
 */
public final class PitMouth
{
    /** Where the group goes relative to the measured mouths: the originals'
        medians. */
    private static final int ENTRY_GAP_AFTER_OUT = 2, EXIT_GAP_BEFORE_IN = 3;

    /** How far from a measured mouth the originals' groups sat, plus a little. */
    private static final int OBJ0_BEFORE = 8, OBJ0_AFTER = 12, OBJ1_BEFORE = 20, OBJ1_AFTER = 6;
    private static final int JOIN_ENTRY_BEFORE = 4, JOIN_ENTRY_AFTER = 13, JOIN_EXIT_BEFORE = 9, JOIN_EXIT_AFTER = 4;

    /** How far from a connect to look for the mouth. */
    private static final int SEARCH_TLU = 80;

    private static final int SIGN_FLAGS = 0xC8;
    private static final int WALL_RIGHT = 0x1000, WALL_LEFT = 0x2000;

    private PitMouth() { }

    /** Connects and mouths as lap TLUs; -1 where not found. */
    public static class Mouths
    {
        public int entry = -1, exit = -1, out = -1, in = -1;
        /** Which side the lane leaves on, from the geometry: +1 left, -1 right, 0 unknown. */
        public int laneSide;
    }

    /** Needs the layout calculated (Track.calculateTrackLayout). */
    public static Mouths measure(Track track)
    {
        Mouths m = new Mouths();
        TrackSegments segs = track.getTrackSegments();
        TrackSegments pit = track.getPitlaneSegments();
        int lap = track.getTrackTlu();
        int segN = segs.getMaxTrackSegIndex() + 1, pitN = pit.getMaxTrackSegIndex() + 1;
        if (lap <= 0 || segN < 2 || pitN < 2)
            return m;
        int start = 0;
        for (int i = 0; i < segs.size(); i++)
        {
            TrackSegment ts = segs.get(i);
            for (Command c : ts.getCommands())
            {
                if (c.getType() == 0x86 && m.entry < 0) m.entry = (start + (c.getParam(0) & 0xFF)) % lap;
                if (c.getType() == 0x87 && m.exit < 0) m.exit = (start + (c.getParam(0) & 0xFF)) % lap;
            }
            start += ts.getTlu();
        }
        if (m.entry < 0 || m.exit < 0)
            return m;
        m.out = mouth(segs, segN, pit, pitN, m.entry, +1);
        m.in = mouth(segs, segN, pit, pitN, m.exit, -1);
        if (m.out >= 0)
            m.laneSide = sideOf(segs, segN, m.out, pit, pitN);
        return m;
    }

    /** True when any 0x80 command uses object setting 0 or 1. GP2 conversions
        made with ArgTrack carry none, and adding them is NOT safe there: on two
        of them (Snetterton, Daytona) the game then drew grey slabs across the
        sky near the pit lane for 5-15 s a lap, while a generated track with the
        same fix drew normally (rig, 2026-10-25). Why is not yet known. */
    public static boolean hasPitObjects(Track track)
    {
        for (TrackSegment ts : track.getTrackSegments())
            for (Command c : ts.getCommands())
                if (c.getType() == 0x80 && (c.getParam(1) == 0 || c.getParam(1) == 16))
                    return true;
        return false;
    }

    /** True when the track has objects 0 and 1 and both pit-join pairs near
        its mouths, and its header puts the pits on the side the lane leaves on. */
    public static boolean looksRight(Track track)
    {
        Mouths m = measure(track);
        if (m.entry < 0 || m.exit < 0)
            return true;        // no pit lane to judge
        if (m.laneSide != 0 && (m.laneSide > 0) != track.getTrackDataHeader().getPitSide())
            return false;
        int lap = track.getTrackTlu();
        int o0 = -1, o1 = -1, a12 = -1, a34 = -1;
        int start = 0;
        TrackSegments segs = track.getTrackSegments();
        int outRef = m.out >= 0 ? m.out : m.entry, inRef = m.in >= 0 ? m.in : m.exit;
        for (int i = 0; i < segs.size(); i++)
        {
            TrackSegment ts = segs.get(i);
            for (Command c : ts.getCommands())
            {
                int at = (start + (c.getParam(0) & 0xFF)) % lap;
                int type = c.getType();
                if (type == 0x80 && c.getParam(1) == 0 && within(at, outRef, OBJ0_BEFORE, OBJ0_AFTER, lap)) o0 = at;
                if (type == 0x80 && c.getParam(1) == 16 && within(at, inRef, OBJ1_BEFORE, OBJ1_AFTER, lap)) o1 = at;
                if ((type == 0xA1 || type == 0xA2) && within(at, outRef, JOIN_ENTRY_BEFORE, JOIN_ENTRY_AFTER, lap)) a12 = at;
                if ((type == 0xA3 || type == 0xA4) && within(at, inRef, JOIN_EXIT_BEFORE, JOIN_EXIT_AFTER, lap)) a34 = at;
            }
            start += ts.getTlu();
        }
        return o0 >= 0 && o1 >= 0 && a12 >= 0 && a34 >= 0;
    }

    /**
     * Rebuilds both mouth groups from the geometry: sets the header's pit side
     * to the side the lane leaves on, removes any objects 0/1 and pit-join
     * commands, and places them as the originals do, splitting sections to
     * make the one-TLU wall gaps. Commands stay in the section they were in,
     * so every Seg they act on is unchanged. Recalculates the layout.
     *
     * @return false (and nothing changed) when the track has no pit lane
     *         or the mouths cannot be found
     */
    public static boolean place(Track track)
    {
        track.calculateTrackLayout();
        Mouths m = measure(track);
        if (m.entry < 0 || m.exit < 0 || m.out < 0 || m.in < 0)
            return false;
        if (m.laneSide != 0)
            track.getTrackDataHeader().setPitSide(m.laneSide > 0);
        int wall = track.getTrackDataHeader().getPitSide() ? WALL_LEFT : WALL_RIGHT;
        int lap = track.getTrackTlu();
        TrackSegments segs = track.getTrackSegments();

        for (int i = 0; i < segs.size(); i++)
        {
            Vector<Command> keep = new Vector<Command>();
            for (Command c : segs.get(i).getCommands())
            {
                int type = c.getType();
                boolean pitObject = type == 0x80 && (c.getParam(1) == 0 || c.getParam(1) == 16);
                if (!pitObject && (type < 0xA1 || type > 0xA4))
                    keep.add(c);
            }
            segs.get(i).setCommands(keep);
        }

        int g = (m.out + ENTRY_GAP_AFTER_OUT) % lap;
        int h = ((m.in - EXIT_GAP_BEFORE_IN) % lap + lap) % lap;
        for (int cut : new int[] { g, g + 1, h, h + 1 })
            splitAt(segs, cut % lap);

        TrackSegment entryGap = sectionStartingAt(segs, g), exitGap = sectionStartingAt(segs, h);
        entryGap.setFlags(entryGap.getFlags() | wall);
        exitGap.setFlags(exitGap.getFlags() | wall);
        entryGap.getCommands().add(new Command(0xA1, 0));
        sectionStartingAt(segs, (g + 1) % lap).getCommands().add(new Command(0xA2, 0));
        exitGap.getCommands().add(new Command(0xA4, 0));
        sectionStartingAt(segs, (h + 1) % lap).getCommands().add(new Command(0xA3, 0));

        boolean[] taken = objectSegs(segs, lap);
        placeObject(segs, freeNear(taken, (g + 1) % lap, lap), 0, lap);
        placeObject(segs, freeNear(taken, ((h - 1) % lap + lap) % lap, lap), 16, lap);

        track.calculateTrackLayout();
        return true;
    }

    // ------------------------------------------------------------------

    /** First track Seg walking away from the connect (dir +1 from the entry,
        -1 from the exit) where the lane's centre is MOUTH_FRACTION of a road
        half-width out; failing that, where it is furthest out. F1CT07's exit
        lane never quite reaches a half-width (0.99 at most). */
    private static int mouth(TrackSegments segs, int segN, TrackSegments pit, int pitN, int connect, int dir)
    {
        int furthest = -1;
        double furthestRatio = 0;
        for (int k = 0; k < SEARCH_TLU; k++)
        {
            int t = ((connect + dir * k) % segN + segN) % segN;
            double ratio = nearestPitDistance(segs.getSegAt(t), pit, pitN) / halfWidth(segs.getSegAt(t));
            if (ratio > MOUTH_FRACTION)
                return t;
            if (ratio > furthestRatio) { furthestRatio = ratio; furthest = t; }
        }
        return furthest;
    }

    private static final double MOUTH_FRACTION = 0.9;

    private static double halfWidth(Seg s)
    {
        return Math.hypot(s.getTrackWidthX(), s.getTrackWidthY()) / 8.0;
    }

    private static int wrap(int d) { return (d << 13) >> 13; }

    private static double nearestPitDistance(Seg s, TrackSegments pit, int pitN)
    {
        double best = Double.MAX_VALUE;
        for (int i = 0; i < pitN; i++)
        {
            Seg p = pit.getSegAt(i);
            best = Math.min(best, Math.hypot(wrap(p.getPosX() - s.getPosX()), wrap(p.getPosY() - s.getPosY())));
        }
        return best;
    }

    /** +1 when the nearest pit Seg lies to the left of the direction of travel. */
    private static int sideOf(TrackSegments segs, int segN, int t, TrackSegments pit, int pitN)
    {
        Seg s = segs.getSegAt(t), next = segs.getSegAt((t + 1) % segN);
        Seg best = null;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < pitN; i++)
        {
            Seg p = pit.getSegAt(i);
            double d = Math.hypot(wrap(p.getPosX() - s.getPosX()), wrap(p.getPosY() - s.getPosY()));
            if (d < bestD) { bestD = d; best = p; }
        }
        double dx = wrap(next.getPosX() - s.getPosX()), dy = wrap(next.getPosY() - s.getPosY());
        double vx = wrap(best.getPosX() - s.getPosX()), vy = wrap(best.getPosY() - s.getPosY());
        double cross = dx * vy - dy * vx;
        return cross == 0 ? 0 : (cross > 0 ? LEFT_IS_POSITIVE_CROSS : -LEFT_IS_POSITIVE_CROSS);
    }

    /** Calibrated on the originals: F1CT01/02/05/07 (pits left) against the rest. */
    private static final int LEFT_IS_POSITIVE_CROSS = 1;

    private static boolean within(int at, int ref, int before, int after, int lap)
    {
        int d = ((at - ref) % lap + lap) % lap;
        if (d > lap / 2) d -= lap;
        return d >= -before && d <= after;
    }

    /** Makes a section boundary at lap TLU t (0 always is one). Verge widths
        are interpolated and sign flags stay on the first piece. The second
        piece carries 0xA5, the game's mark for a split section: without it a
        new section shifts the heading pipeline (a lost unit on an odd
        curvature, and wrong road-edge offsets after a curve, seen on F1CT03),
        with it every Seg comes out as the unsplit section gave (PitMouthGate). */
    static void splitAt(TrackSegments segs, int t)
    {
        int start = 0;
        for (int i = 0; i < segs.size(); i++)
        {
            TrackSegment ts = segs.get(i);
            int len = ts.getTlu();
            if (t == start || len <= 0)
                return;
            if (t < start + len)
            {
                int r = t - start;
                TrackSegment prev = i > 0 ? segs.get(i - 1) : segs.get(segs.size() - 2);
                TrackSegment piece = new TrackSegment();
                piece.setTlu(len - r);
                piece.setCurvature(ts.getCurvature());
                piece.setHeightChange(ts.getHeightChange());
                piece.setFlags(ts.getFlags() & ~SIGN_FLAGS);
                piece.setFenceDistR(ts.getFenceDistR());
                piece.setFenceDistL(ts.getFenceDistL());
                piece.getCommands().add(new Command(0xA5, 0));
                ts.setTlu(r);
                ts.setFenceDistR(prev.getFenceDistR() + (ts.getFenceDistR() - prev.getFenceDistR()) * r / len);
                ts.setFenceDistL(prev.getFenceDistL() + (ts.getFenceDistL() - prev.getFenceDistL()) * r / len);
                segs.add(i + 1, piece);
                return;
            }
            start += len;
        }
    }

    private static TrackSegment sectionStartingAt(TrackSegments segs, int t)
    {
        int start = 0;
        for (int i = 0; i < segs.size(); i++)
        {
            if (start == t && segs.get(i).getTlu() > 0)
                return segs.get(i);
            start += segs.get(i).getTlu();
        }
        return segs.get(0);
    }

    private static boolean[] objectSegs(TrackSegments segs, int lap)
    {
        boolean[] taken = new boolean[lap];
        int start = 0;
        for (int i = 0; i < segs.size(); i++)
        {
            for (Command c : segs.get(i).getCommands())
                if (c.getType() == 0x80)
                    taken[(start + (c.getParam(0) & 0xFF)) % lap] = true;
            start += segs.get(i).getTlu();
        }
        return taken;
    }

    /** A Seg near t carrying no other object: two objects on one Seg leave only the later one. */
    private static int freeNear(boolean[] taken, int t, int lap)
    {
        for (int d = 0; d <= 4; d++)
        {
            if (!taken[(t + d) % lap]) return (t + d) % lap;
            if (!taken[((t - d) % lap + lap) % lap]) return ((t - d) % lap + lap) % lap;
        }
        return t;
    }

    private static void placeObject(TrackSegments segs, int t, int reference, int lap)
    {
        int start = 0;
        for (int i = 0; i < segs.size(); i++)
        {
            int len = segs.get(i).getTlu();
            if (len > 0 && t >= start && t < start + len)
            {
                segs.get(i).getCommands().add(new Command(0x80, t - start, reference, 0, 0, 0, 0));
                return;
            }
            start += len;
        }
    }
}
