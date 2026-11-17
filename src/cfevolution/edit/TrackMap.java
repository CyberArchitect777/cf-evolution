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

package cfevolution.edit;

import cfevolution.data.track.Seg;
import cfevolution.data.track.Track;
import cfevolution.data.track.TrackBounds;
import cfevolution.data.track.TrackSegments;

/**
 * What the track map shows, as shapes in track coordinates, so every
 * interface draws the same map (2026-11-16, the geometry moved out of
 * TrackPanel.renderMapContent unchanged). Build it from a track that has been
 * recalculated (TrackTools.recalculate).
 *
 * Index i of road, line and lineOffRoad is one Seg, from Seg i to Seg i+1;
 * the desktop draws road i then line i, in order, so where they overlap the
 * later one is on top. To draw, multiply each coordinate by the scale and
 * truncate to an int (as the desktop does), with y increasing upwards.
 *
 * Colours on the desktop: road black (selected sector orange); best line red,
 * orange where it runs well off the road, green for the selected sector; pit
 * lane blue (selected pink); a generated line awaiting Apply magenta. The best
 * line is not drawn in layout mode.
 */
public final class TrackMap
{
    /** Section numbers, as selection uses them. */
    public static final int TRACK = 1, PIT = 2, LINE = 3;

    /** 65536 angle units to a full turn. */
    public static final double ANGLE_SCALE_RAD = 2 * Math.PI / 65536;

    public final boolean layoutMode;
    /** Road quadrilaterals {x0,y0, x1,y1, x2,y2, x3,y3} and the 1-based track sector of each. */
    public final double[][] road;
    public final int[] roadSector;
    /** Best line pieces {x0,y0, x1,y1}, their 1-based line sector, and whether each runs well off the road. */
    public final double[][] line;
    public final int[] lineSector;
    public final boolean[] lineOffRoad;
    /** Pit lane quadrilaterals and their 1-based pit sector. */
    public final double[][] pit;
    public final int[] pitSector;
    /** A generated line awaiting Apply, as pieces {x0,y0, x1,y1}; empty if none. */
    public final double[][] preview;

    private TrackMap(boolean layoutMode, double[][] road, int[] roadSector, double[][] line, int[] lineSector,
                     boolean[] lineOffRoad, double[][] pit, int[] pitSector, double[][] preview)
    {
        this.layoutMode = layoutMode;
        this.road = road;
        this.roadSector = roadSector;
        this.line = line;
        this.lineSector = lineSector;
        this.lineOffRoad = lineOffRoad;
        this.pit = pit;
        this.pitSector = pitSector;
        this.preview = preview;
    }

    /** The map of a track; previewOffsets (one lateral offset per Seg) may be null. */
    public static TrackMap of(Track track, double[] previewOffsets)
    {
        TrackSegments trackSegments = track.getTrackSegments();
        boolean layout = track.getLayoutMode();
        int n = Math.max(0, trackSegments.getMaxTrackSegIndex());
        double[][] road = new double[n][];
        int[] roadSector = new int[n];
        double[][] line = new double[layout ? 0 : n][];
        int[] lineSector = new int[layout ? 0 : n];
        boolean[] lineOffRoad = new boolean[layout ? 0 : n];
        for (int i = 0; i < n; i++)
        {
            Seg seg = trackSegments.getSegAt(i);
            Seg segNext = trackSegments.getSegAt(i + 1);
            road[i] = quad(seg, segNext);
            roadSector[i] = seg.m_nTrackSector;
            if (!layout)
            {
                double[] ccPos = worldPos(seg, seg.getCCLine());
                double[] ccPosNext = worldPos(segNext, segNext.getCCLine());
                line[i] = new double[] { ccPos[0], ccPos[1], ccPosNext[0], ccPosNext[1] };
                lineSector[i] = seg.m_nCCLineSector;
                lineOffRoad[i] = offRoad(seg);
            }
        }

        TrackSegments pitlaneSegments = track.getPitlaneSegments();
        int p = Math.max(0, pitlaneSegments.getMaxTrackSegIndex());
        double[][] pit = new double[p][];
        int[] pitSector = new int[p];
        for (int i = 0; i < p; i++)
        {
            Seg seg = pitlaneSegments.getSegAt(i);
            pit[i] = quad(seg, pitlaneSegments.getSegAt(i + 1));
            pitSector[i] = seg.m_nTrackSector;
        }

        double[][] preview = new double[0][];
        if (previewOffsets != null)
        {
            int nCount = Math.min(previewOffsets.length, trackSegments.getMaxTrackSegIndex());
            preview = new double[Math.max(0, nCount - 1)][];
            for (int i = 0; i < nCount - 1; i++)
            {
                double[] pos = worldPos(trackSegments.getSegAt(i), previewOffsets[i]);
                double[] posNext = worldPos(trackSegments.getSegAt(i + 1), previewOffsets[i + 1]);
                preview[i] = new double[] { pos[0], pos[1], posNext[0], posNext[1] };
            }
        }
        return new TrackMap(layout, road, roadSector, line, lineSector, lineOffRoad, pit, pitSector, preview);
    }

    /** The quadrilateral the map draws for one Seg (each Seg is one TLU). */
    private static double[] quad(Seg seg, Seg segNext)
    {
        double dXDiff = seg.getTrackWidthX() >> 3;
        double dYDiff = seg.getTrackWidthY() >> 3;
        double dXDiffNext = segNext.getTrackWidthX() >> 3;
        double dYDiffNext = segNext.getTrackWidthY() >> 3;
        return new double[] {
            seg.getDisplayX() + dXDiff,         seg.getDisplayY() - dYDiff,
            segNext.getDisplayX() + dXDiffNext, segNext.getDisplayY() - dYDiffNext,
            segNext.getDisplayX() - dXDiffNext, segNext.getDisplayY() + dYDiffNext,
            seg.getDisplayX() - dXDiff,         seg.getDisplayY() + dYDiff };
    }

    /** Where a point offset sideways from a Seg lies, by the game's own lateral formula. */
    public static double[] worldPos(Seg seg, double dOffset)
    {
        double wSegPosY = (double) ((long) (dOffset * seg.getAngleZChangeMulHalfPI()) >> 15);
        double cosA = Math.cos(seg.getAngleZ() * ANGLE_SCALE_RAD);
        double sinA = Math.sin(seg.getAngleZ() * ANGLE_SCALE_RAD);
        return new double[] {
            seg.getDisplayX() + dOffset * cosA + wSegPosY * sinA,
            seg.getDisplayY() + wSegPosY * cosA - dOffset * sinA
        };
    }

    /** True if the best line at this Seg is well off the road: more than
        TrackCheck's limit (twice the road's half-width), so the orange
        stretches are the ones Check Track's warning points to. */
    public static boolean offRoad(Seg seg)
    {
        double wx = seg.getTrackWidthX() + seg.getExtraSideX();
        double wy = seg.getTrackWidthY() + seg.getExtraSideY();
        double halfRoad = Math.sqrt(wx * wx / 64.0 + wy * wy / 64.0);
        return Math.abs(seg.getCCLine()) > TrackCheck.WORST_EXCURSION_WARNING * halfRoad;
    }

    /**
     * The view that fits the whole track into width x height with a border of
     * at least 10 percent each side, as the desktop map does when a track is
     * first shown: {scale, translateX, translateY}. A point (x, y) of the map
     * is drawn at pixel ((int)(x * scale) + translateX, translateY - (int)(y * scale)).
     */
    public static double[] fit(Track track, double width, double height)
    {
        TrackBounds r = track.getF1GPBoundingRectangle();
        double scale = Math.min(width / (r.getWidth() * 1.2), height / (r.getHeight() * 1.2));
        return new double[] { scale, (width / 2.0) - scale * r.getCenterX(), (height / 2.0) + scale * r.getCenterY() };
    }

    /**
     * What a tap at map point (x, y) picks, given the scale it is drawn at:
     * {section, sector} (TRACK, PIT or LINE, 1-based), or null. As on the
     * desktop: the nearest best line point within 8 pixels first, then a pit
     * lane piece, then a road piece.
     */
    public int[] pick(double x, double y, double scale)
    {
        double px = x * scale, py = y * scale;
        if (!layoutMode && line.length > 0)
        {
            double best = 8.0 * 8.0;
            int sector = -1;
            for (int i = 0; i < line.length; i++)
            {
                double dx = line[i][0] * scale - px, dy = line[i][1] * scale - py;
                double d = dx * dx + dy * dy;
                if (d < best)
                {
                    best = d;
                    sector = lineSector[i];
                }
            }
            if (sector > 0)
                return new int[] { LINE, sector };
        }
        for (int i = 0; i < pit.length; i++)
            if (contains(pit[i], x, y))
                return new int[] { PIT, pitSector[i] };
        for (int i = 0; i < road.length; i++)
            if (contains(road[i], x, y))
                return new int[] { TRACK, roadSector[i] };
        return null;
    }

    /** Point in a quadrilateral, by ray crossing. */
    private static boolean contains(double[] q, double x, double y)
    {
        boolean in = false;
        for (int i = 0, j = 3; i < 4; j = i++)
        {
            double xi = q[2 * i], yi = q[2 * i + 1], xj = q[2 * j], yj = q[2 * j + 1];
            if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi)
                in = !in;
        }
        return in;
    }
}
