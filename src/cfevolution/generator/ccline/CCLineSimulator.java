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

import java.util.Enumeration;

import cfevolution.data.f1gp.CosLookupTable;
import cfevolution.data.f1gp.F1GPMath;
import cfevolution.data.track.CCLine;
import cfevolution.data.track.CCLineSegment;

/**
    Standalone re-implementation of Track.calculateGameCCLine /
    ProcessCCLineSector / ProcessCCLineSegment operating on
    CCLineTrackGeometry arrays instead of live Seg objects.

    Exists so candidate CCLines can be evaluated on a background thread
    without mutating the Track the editor is displaying.

    IMPORTANT: this must stay bit-exact with the corresponding code in
    Track.java (including the Pythagoras clamp, which is a deliberate
    editor-side deviation from the game). The headless fidelity harness
    compares both implementations Seg-by-Seg on the original tracks —
    re-run it whenever either side changes.
*/
public class CCLineSimulator {

    /** Per-Seg results of one simulation run. */
    public static class Result {
        /** Lateral position stamped per Seg (Seg.wCCLine equivalent). */
        public final short[] ccLine;
        /** Heading relative to track per Seg (Seg.wCCLineRAngle equivalent). */
        public final int[] ccLineRAngle;
        /** 1-based CCLine sector per Seg (Seg.m_nCCLineSector equivalent), 0 if never stamped. */
        public final int[] ccLineSector;
        /** True for Segs stamped at least once. */
        public final boolean[] covered;
        /** Total TLU walked (sum of all sector lengths, game rule). */
        public int walkedTlu;
        /** Times the Pythagoras clamp fired (radius too small for its arc
            at that point — the game would compute sqrt garbage there). */
        public int clampCount;

        public Result(int segCount) {
            ccLine = new short[segCount];
            ccLineRAngle = new int[segCount];
            ccLineSector = new int[segCount];
            covered = new boolean[segCount];
            walkedTlu = 0;
        }
    }

    /**
        Walk state between CCLine sectors. Snapshot/restore via copy() lets
        the quantizer try candidate sectors from a fixed prefix without
        re-simulating it.
    */
    public static class State {
        public short wSegPosX;
        public short wTmpAngleZ;
        public int segIndex;     // current track Seg
        public int sector;       // sectors walked so far (1-based stamps)
        public int walkedTlu;

        public State copy() {
            State s = new State();
            s.wSegPosX = wSegPosX;
            s.wTmpAngleZ = wTmpAngleZ;
            s.segIndex = segIndex;
            s.sector = sector;
            s.walkedTlu = walkedTlu;
            return s;
        }
    }

    private final CCLineTrackGeometry geo;

    // Working state, mirroring the "globals" in Track.java
    private int length, radius;
    private short shiftx;
    private short wTmpAngleZ;
    private short wSegPosX;
    private int tmpX = 0, tmpY = 0;
    private int tmpCos = 0, tmpSin = 0;
    private int tmp1 = 0, tmp2 = 0, tmp5 = 0, tmp6 = 0;
    private Result activeResult;

    public CCLineSimulator(CCLineTrackGeometry geometry) {
        geo = geometry;
    }

    /** State at the very start of the walk (game seeds the heading from Seg 0). */
    public State initialState() {
        State st = new State();
        st.wTmpAngleZ = geo.angleZ[0];
        return st;
    }

    /** Runs the game-accurate CCLine walk and returns the per-Seg stamps. */
    public Result run(CCLine ccLine) {
        Result result = new Result(geo.segCount);
        State st = initialState();
        for (Enumeration e = ccLine.elements(); e.hasMoreElements(); )
            runSegment((CCLineSegment) e.nextElement(), st, result);
        result.walkedTlu = st.walkedTlu;
        return result;
    }

    /**
        Walks a single CCLine sector from the given state, stamping into
        result (which may be null when only the end state matters). The
        state is advanced in place. Body is the sector loop of
        Track.calculateGameCCLine, unchanged.
    */
    public void runSegment(CCLineSegment cclineSeg, State st, Result result) {
        activeResult = result;
        wSegPosX = st.wSegPosX;
        wTmpAngleZ = st.wTmpAngleZ;
        int nTrackSegNum = st.segIndex;
        int nCCLineSector = ++st.sector;

        int nShift;
        int nParam = 0;

        if ((cclineSeg.getType() & 0x80) != 0)   // starting sector
            wSegPosX = (short) cclineSeg.getParam(nParam++);

        // shift or correction value: modification at start of sector.
        nShift = cclineSeg.getParam(nParam++);

        radius = cclineSeg.getParam(nParam++);

        if ((cclineSeg.getType() & 0x40) != 0) {
            // 32bit radius
            radius = (radius << 16) | (cclineSeg.getParam(nParam++) & 0x0FFFF);
        }

        radius <<= 3; // * 8

        // shift longitudinal or by angle
        if (radius != 0)  // curve segment
            shiftx = (short) (nShift << 2);
        else {  // straight segment
            shiftx = 0;
            wTmpAngleZ += (short) nShift;
        }

        length = ((cclineSeg.getType() & 0x3f) << 8) | cclineSeg.getTlu();
        st.walkedTlu += length;

        processCCLineSector(nTrackSegNum);

        for (int i = 0; i < length; i++) {
            if (result != null) {
                result.ccLineRAngle[nTrackSegNum] = wTmpAngleZ - geo.angleZ[nTrackSegNum];
                result.ccLine[nTrackSegNum] = wSegPosX;
                result.ccLineSector[nTrackSegNum] = nCCLineSector;
                result.covered[nTrackSegNum] = true;
            }

            nTrackSegNum++;
            if (nTrackSegNum >= geo.segCount)
                nTrackSegNum = 0;

            processCCLineSegment(nTrackSegNum);
        }

        st.wSegPosX = wSegPosX;
        st.wTmpAngleZ = wTmpAngleZ;
        st.segIndex = nTrackSegNum;
    }

    // calculate tmpX and tmpY: coordinates
    private void processCCLineSector(int nSeg) {
        short wSegPosY = (short) ((wSegPosX * geo.angleZChangeMulHalfPI[nSeg]) >> 15);

        tmpSin = F1GPMath.LookupSin(geo.angleZ[nSeg]);
        tmpCos = F1GPMath.LookupCos(geo.angleZ[nSeg]);

        tmp5 = ((wSegPosY * tmpCos) - (wSegPosX * tmpSin)) >> 14; // sin/cos scaled by 14 bits
        tmp6 = ((wSegPosX * tmpCos) + (wSegPosY * tmpSin)) >> 14;

        tmp5 += geo.posY[nSeg];
        tmp6 += geo.posX[nSeg];

        // wTmpAngleZ and shiftx are values of current ccLine sector.
        tmp5 += (F1GPMath.LookupCos((short) wTmpAngleZ) * shiftx) >> 14;
        tmpY = tmp5;

        tmp6 += (F1GPMath.LookupSin((short) wTmpAngleZ) * shiftx) >> 14;
        tmpX = tmp6;

        if (radius != 0) {

            int nAngle;
            if (radius >= 0)
                nAngle = wTmpAngleZ + 0x4000;   // + 90 degrees
            else
                nAngle = wTmpAngleZ - 0x4000;   // - 90 degrees

            // results stored in tmpSin and tmpCos (32bit values: sin/cos shifted by 30 bits)
            sinAndCosBig(nAngle);

            long ll = (long) tmpSin * (long) Math.abs( radius );
            tmpX += (ll >> 30);

            ll = (long) tmpCos * (long) Math.abs( radius );
            tmpY += (ll >> 30);
        }
    }

    // Calculate position and angle of ccLine at next track Seg start.
    private void processCCLineSegment(int nSeg) {
        int nXDiff = tmpX - geo.posX[nSeg];
        int nYDiff = tmpY - geo.posY[nSeg];

        int invPI = 0x517D; // 0x10000 / PI
        int a1 = geo.angleZ[nSeg] - (((geo.angleZChangeMulHalfPI[nSeg] >> 1) * invPI) >> 15);
        tmpCos = a1; // actually an angle!

        // sinBig( tmpCos ) stored in tmpCos
        // cosBig( tmpCos ) stored in tmpSin
        sinAndCosBig(tmpCos);

        tmp5 = (int) ((((long) tmpCos * (long) nXDiff) - ((long) tmpSin * (long) nYDiff)) >> 30);
        tmp6 = (int) ((((long) tmpCos * (long) nYDiff) + ((long) tmpSin * (long) nXDiff)) >> 30);

        if (radius == 0) {
            // ccLine straight
            int tmp = wTmpAngleZ - a1;
            tmp1 = F1GPMath.LookupSinbig((short) tmp);
            tmp2 = F1GPMath.LookupCosbig((short) tmp);
            long lTemp = (long) tmp1 * (long) tmp6;
            if ( tmp2 == 0 )
            {
                // Makes no sense, but I think this happens in the game (KS)
                tmp6 = (int) (lTemp & (long) 0x0000FFFF);
            }
            else
            {
                tmp6 = (int) (lTemp / ((long) tmp2));
            }
            wSegPosX = (short) (tmp5 - tmp6);
        }
        else
        {
            // Pythagoras: radius^2 - tmp6^2. Can go negative for very small radii
            // (arc subtends > 90 deg in one TLU), which causes sqrt64 to produce
            // garbage. Clamp to 0 so the result degrades gracefully instead.
            long ll = (((long) radius * (long) radius) - ((long) tmp6 * (long) tmp6));
            if (ll < 0) {
                ll = 0;
                if (activeResult != null)
                    activeResult.clampCount++;
            }
            tmp1 = (int) F1GPMath.sqrt64(ll);

            if (radius < 0)
                tmp1 = -tmp1;

            wSegPosX = (short) (tmp5 - tmp1);

            int r = radius;
            if (r < 0)
                    r = -r;

            for (; r >= 0x7F00; r >>= 1) {
                tmp1 >>= 1;
                tmp6 >>= 1;
            }

            int a2 = F1GPMath.LookupAtan2(tmp1, tmp6);

            if (radius < 0)
                a2 = a2 + 0x4000;
            else
                a2 = a2 - 0x4000;

            wTmpAngleZ = (short) (a1 + a2);
        }
    }

    private void getOppositeEdgeLength() {
            long tmp = 0x1000000000000000l - ((long) tmpCos * (long) tmpCos);

            if (tmp < 0)
                    tmp = 0;

            tmpSin = (int) F1GPMath.sqrt64(tmp);
    }

    /**
        Calculate 32Bit Sin and Cos from a 16 bit angle value.
        Identical to Track.sinAndCosBig — see the fidelity note in the
        class comment.
    */
    private void sinAndCosBig(int nAngle) {
            short oldCos = (short) nAngle;
            short index = (short) ((-nAngle) + (short) 0x4000);

            if (index < 0)
                index = (short) -index;

            int i = (index >> 2) & 0x3FFE; // also remove sign bits that could be present (e.g. nAngle = C000h)
            short val = CosLookupTable.get(i / 2);

            if (val < 0)
                val = (short) -val;

            if (val < 0x2000) {
                // Calculate Cos from Sin
                tmpCos = F1GPMath.LookupSinbig((short) nAngle);

                if (oldCos < 0)
                        oldCos = (short) -oldCos;

                int j = (oldCos >> 2) & 0x3FFE;// also remove sign bits that could be present
                oldCos = CosLookupTable.get(j / 2);
                getOppositeEdgeLength();

                if (oldCos < 0)
                        tmpSin = -tmpSin;

                // swap tmpCos and tmpSin
                int temp = tmpSin;
                tmpSin = tmpCos;
                tmpCos = temp;
            } else {
                // Calculate Sin from Cos
                tmpCos = F1GPMath.LookupCosbig((short) nAngle);
                oldCos = (short) (-oldCos + 0x4000);

                if (oldCos < 0)
                    oldCos = (short) -oldCos;

                int j = (oldCos >> 2) & 0x3FFE; // also remove sign bits that could be present
                oldCos = CosLookupTable.get(j / 2);
                getOppositeEdgeLength();

                if (oldCos < 0)
                    tmpSin = -tmpSin;
            }
    }
}
