/*
 * Nuked OPL3
 * Copyright (C) 2013-2020 Nuke.YKT
 *
 * This file is part of Nuked OPL3.
 *
 * Nuked OPL3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1
 * of the License, or (at your option) any later version.
 *
 * Nuked OPL3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Nuked OPL3. If not, see <https://www.gnu.org/licenses/>.
 *
 *  Nuked OPL3 emulator.
 *  Thanks:
 *      MAME Development Team(Jarek Burczynski, Tatsuyuki Satoh):
 *          Feedback and Rhythm part calculation information.
 *      forums.submarine.org.uk(carbon14, opl3):
 *          Tremolo and phase generator calculation information.
 *      OPLx decapsulated(Matthew Gambrell, Olli Niemitalo):
 *          OPL2 ROMs.
 *      siliconpr0n.org(John McMaster, digshadow):
 *          YMF262 and VRC VII decaps and die shots.
 *
 * version: 1.8
 *
 * Java port for CF Evolution (2026-11-19), line for line from opl3.c 1.8
 * (github.com/nukeykt/Nuked-OPL3), used under the LGPL 2.1's section 3 as
 * part of a GPL v2 program. Built as the C is by default: no stereo
 * extension, with the channel sample delay quirk; the buffered register
 * writes are left out (registers are written directly). The C's pointers
 * become references (Src); its unsigned and narrow integers are kept by
 * masking. Checked sample for sample against the C (harnesses/OplGate).
 */
package cfevolution.data.sound;

/** The YMF262 (OPL3) chip, which also plays everything an AdLib's OPL2 does. */
public final class Opl3
{
    /** The chip's own sample rate (14.31818 MHz / 288). */
    public static final int RATE = 49716;
    private static final int RSM_FRAC = 10;
    private static final int CH_2OP = 0, CH_4OP = 1, CH_4OP2 = 2, CH_DRUM = 3;
    private static final int EGK_NORM = 0x01, EGK_DRUM = 0x02;
    private static final int EG_ATTACK = 0, EG_DECAY = 1, EG_SUSTAIN = 2, EG_RELEASE = 3;

    private static final int[] LOGSIN = {
        0x859, 0x6c3, 0x607, 0x58b, 0x52e, 0x4e4, 0x4a6, 0x471, 0x443, 0x41a, 0x3f5, 0x3d3, 0x3b5, 0x398, 0x37e, 0x365,
        0x34e, 0x339, 0x324, 0x311, 0x2ff, 0x2ed, 0x2dc, 0x2cd, 0x2bd, 0x2af, 0x2a0, 0x293, 0x286, 0x279, 0x26d, 0x261,
        0x256, 0x24b, 0x240, 0x236, 0x22c, 0x222, 0x218, 0x20f, 0x206, 0x1fd, 0x1f5, 0x1ec, 0x1e4, 0x1dc, 0x1d4, 0x1cd,
        0x1c5, 0x1be, 0x1b7, 0x1b0, 0x1a9, 0x1a2, 0x19b, 0x195, 0x18f, 0x188, 0x182, 0x17c, 0x177, 0x171, 0x16b, 0x166,
        0x160, 0x15b, 0x155, 0x150, 0x14b, 0x146, 0x141, 0x13c, 0x137, 0x133, 0x12e, 0x129, 0x125, 0x121, 0x11c, 0x118,
        0x114, 0x10f, 0x10b, 0x107, 0x103, 0x0ff, 0x0fb, 0x0f8, 0x0f4, 0x0f0, 0x0ec, 0x0e9, 0x0e5, 0x0e2, 0x0de, 0x0db,
        0x0d7, 0x0d4, 0x0d1, 0x0cd, 0x0ca, 0x0c7, 0x0c4, 0x0c1, 0x0be, 0x0bb, 0x0b8, 0x0b5, 0x0b2, 0x0af, 0x0ac, 0x0a9,
        0x0a7, 0x0a4, 0x0a1, 0x09f, 0x09c, 0x099, 0x097, 0x094, 0x092, 0x08f, 0x08d, 0x08a, 0x088, 0x086, 0x083, 0x081,
        0x07f, 0x07d, 0x07a, 0x078, 0x076, 0x074, 0x072, 0x070, 0x06e, 0x06c, 0x06a, 0x068, 0x066, 0x064, 0x062, 0x060,
        0x05e, 0x05c, 0x05b, 0x059, 0x057, 0x055, 0x053, 0x052, 0x050, 0x04e, 0x04d, 0x04b, 0x04a, 0x048, 0x046, 0x045,
        0x043, 0x042, 0x040, 0x03f, 0x03e, 0x03c, 0x03b, 0x039, 0x038, 0x037, 0x035, 0x034, 0x033, 0x031, 0x030, 0x02f,
        0x02e, 0x02d, 0x02b, 0x02a, 0x029, 0x028, 0x027, 0x026, 0x025, 0x024, 0x023, 0x022, 0x021, 0x020, 0x01f, 0x01e,
        0x01d, 0x01c, 0x01b, 0x01a, 0x019, 0x018, 0x017, 0x017, 0x016, 0x015, 0x014, 0x014, 0x013, 0x012, 0x011, 0x011,
        0x010, 0x00f, 0x00f, 0x00e, 0x00d, 0x00d, 0x00c, 0x00c, 0x00b, 0x00a, 0x00a, 0x009, 0x009, 0x008, 0x008, 0x007,
        0x007, 0x007, 0x006, 0x006, 0x005, 0x005, 0x005, 0x004, 0x004, 0x004, 0x003, 0x003, 0x003, 0x002, 0x002, 0x002,
        0x002, 0x001, 0x001, 0x001, 0x001, 0x001, 0x001, 0x001, 0x000, 0x000, 0x000, 0x000, 0x000, 0x000, 0x000, 0x000
    };

    private static final int[] EXP = {
        0x7fa, 0x7f5, 0x7ef, 0x7ea, 0x7e4, 0x7df, 0x7da, 0x7d4, 0x7cf, 0x7c9, 0x7c4, 0x7bf, 0x7b9, 0x7b4, 0x7ae, 0x7a9,
        0x7a4, 0x79f, 0x799, 0x794, 0x78f, 0x78a, 0x784, 0x77f, 0x77a, 0x775, 0x770, 0x76a, 0x765, 0x760, 0x75b, 0x756,
        0x751, 0x74c, 0x747, 0x742, 0x73d, 0x738, 0x733, 0x72e, 0x729, 0x724, 0x71f, 0x71a, 0x715, 0x710, 0x70b, 0x706,
        0x702, 0x6fd, 0x6f8, 0x6f3, 0x6ee, 0x6e9, 0x6e5, 0x6e0, 0x6db, 0x6d6, 0x6d2, 0x6cd, 0x6c8, 0x6c4, 0x6bf, 0x6ba,
        0x6b5, 0x6b1, 0x6ac, 0x6a8, 0x6a3, 0x69e, 0x69a, 0x695, 0x691, 0x68c, 0x688, 0x683, 0x67f, 0x67a, 0x676, 0x671,
        0x66d, 0x668, 0x664, 0x65f, 0x65b, 0x657, 0x652, 0x64e, 0x649, 0x645, 0x641, 0x63c, 0x638, 0x634, 0x630, 0x62b,
        0x627, 0x623, 0x61e, 0x61a, 0x616, 0x612, 0x60e, 0x609, 0x605, 0x601, 0x5fd, 0x5f9, 0x5f5, 0x5f0, 0x5ec, 0x5e8,
        0x5e4, 0x5e0, 0x5dc, 0x5d8, 0x5d4, 0x5d0, 0x5cc, 0x5c8, 0x5c4, 0x5c0, 0x5bc, 0x5b8, 0x5b4, 0x5b0, 0x5ac, 0x5a8,
        0x5a4, 0x5a0, 0x59c, 0x599, 0x595, 0x591, 0x58d, 0x589, 0x585, 0x581, 0x57e, 0x57a, 0x576, 0x572, 0x56f, 0x56b,
        0x567, 0x563, 0x560, 0x55c, 0x558, 0x554, 0x551, 0x54d, 0x549, 0x546, 0x542, 0x53e, 0x53b, 0x537, 0x534, 0x530,
        0x52c, 0x529, 0x525, 0x522, 0x51e, 0x51b, 0x517, 0x514, 0x510, 0x50c, 0x509, 0x506, 0x502, 0x4ff, 0x4fb, 0x4f8,
        0x4f4, 0x4f1, 0x4ed, 0x4ea, 0x4e7, 0x4e3, 0x4e0, 0x4dc, 0x4d9, 0x4d6, 0x4d2, 0x4cf, 0x4cc, 0x4c8, 0x4c5, 0x4c2,
        0x4be, 0x4bb, 0x4b8, 0x4b5, 0x4b1, 0x4ae, 0x4ab, 0x4a8, 0x4a4, 0x4a1, 0x49e, 0x49b, 0x498, 0x494, 0x491, 0x48e,
        0x48b, 0x488, 0x485, 0x482, 0x47e, 0x47b, 0x478, 0x475, 0x472, 0x46f, 0x46c, 0x469, 0x466, 0x463, 0x460, 0x45d,
        0x45a, 0x457, 0x454, 0x451, 0x44e, 0x44b, 0x448, 0x445, 0x442, 0x43f, 0x43c, 0x439, 0x436, 0x433, 0x430, 0x42d,
        0x42a, 0x428, 0x425, 0x422, 0x41f, 0x41c, 0x419, 0x416, 0x414, 0x411, 0x40e, 0x40b, 0x408, 0x406, 0x403, 0x400
    };

    private static final int[] MT = { 1, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 20, 24, 24, 30, 30 };
    private static final int[] KSLROM = { 0, 32, 40, 45, 48, 51, 53, 55, 56, 58, 59, 60, 61, 62, 63, 64 };
    private static final int[] KSLSHIFT = { 8, 1, 2, 0 };
    private static final int[][] EG_INCSTEP = { { 0, 0, 0, 0 }, { 1, 0, 0, 0 }, { 1, 0, 1, 0 }, { 1, 1, 1, 0 } };
    private static final int[] AD_SLOT = {
        0, 1, 2, 3, 4, 5, -1, -1, 6, 7, 8, 9, 10, 11, -1, -1,
        12, 13, 14, 15, 16, 17, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
    };
    private static final int[] CH_SLOT = { 0, 1, 2, 6, 7, 8, 12, 13, 14, 18, 19, 20, 24, 25, 26, 30, 31, 32 };

    /** What a C int16_t pointer pointed at: nothing (the chip's zeromod), a slot's out, or a slot's fbmod. */
    private static final class Src
    {
        final Slot slot;
        final boolean fb;
        Src(Slot slot, boolean fb) { this.slot = slot; this.fb = fb; }
        int get() { return slot == null ? 0 : fb ? slot.fbmod : slot.out; }
    }

    private static final Src ZERO = new Src(null, false);

    private final class Slot
    {
        Channel channel;
        int out, fbmod, prout;          // int16
        Src mod = ZERO;
        final Src outSrc = new Src(this, false), fbSrc = new Src(this, true);
        int egRout, egOut;              // uint16
        int egGen, egKsl;               // uint8
        boolean trem;                   // reads the chip's tremolo, else zeromod (0)
        int regVib, regType, regKsr, regMult, regKsl, regTl, regAr, regDr, regSl, regRr, regWf, key;
        int pgReset;
        int pgPhase;                    // uint32 (wraps as int)
        int pgPhaseOut;                 // uint16
        int slotNum;
    }

    private final class Channel
    {
        final Slot[] slotz = new Slot[2];
        Channel pair;
        final Src[] out = { ZERO, ZERO, ZERO, ZERO };
        int chtype, fNum, fNumReg, block, blockReg, fb, con, alg, ksv;
        int cha, chb, chc, chd;         // uint16 masks
        int chNum;
    }

    private final Channel[] channel = new Channel[18];
    private final Slot[] slot = new Slot[36];
    private int timer;                  // uint16
    private long egTimer;               // uint64 (36 bits used)
    private int egTimerrem, egState, egAdd, egTimerLo;
    private int newm, nts, rhy, vibpos, vibshift, tremolo, tremolopos, tremoloshift;
    private int noise;                  // uint32
    private final int[] mixbuff = new int[4];
    private int rmHhBit2, rmHhBit3, rmHhBit7, rmHhBit8, rmTcBit3, rmTcBit5;
    private int rateratio, samplecnt;
    private final int[] oldsamples = new int[4], samples = new int[4];

    /** A chip reset for output at the given rate (Opl3.RATE for the chip's own). */
    public Opl3(int samplerate)
    {
        reset(samplerate);
    }

    // --- envelope generator ------------------------------------------------------

    private static int calcExp(int level)
    {
        if (level > 0x1fff)
            level = 0x1fff;
        return (EXP[level & 0xff] << 1) >> (level >> 8);
    }

    private static int envelopeSin(int wf, int phase, int envelope)
    {
        int out = 0, neg = 0;
        phase &= 0x3ff;
        switch (wf)
        {
        case 0:
            if ((phase & 0x200) != 0) neg = 0xffff;
            out = (phase & 0x100) != 0 ? LOGSIN[(phase & 0xff) ^ 0xff] : LOGSIN[phase & 0xff];
            return (short) (calcExp(out + (envelope << 3)) ^ neg);
        case 1:
            if ((phase & 0x200) != 0) out = 0x1000;
            else if ((phase & 0x100) != 0) out = LOGSIN[(phase & 0xff) ^ 0xff];
            else out = LOGSIN[phase & 0xff];
            return (short) calcExp(out + (envelope << 3));
        case 2:
            out = (phase & 0x100) != 0 ? LOGSIN[(phase & 0xff) ^ 0xff] : LOGSIN[phase & 0xff];
            return (short) calcExp(out + (envelope << 3));
        case 3:
            out = (phase & 0x100) != 0 ? 0x1000 : LOGSIN[phase & 0xff];
            return (short) calcExp(out + (envelope << 3));
        case 4:
            if ((phase & 0x300) == 0x100) neg = 0xffff;
            if ((phase & 0x200) != 0) out = 0x1000;
            else if ((phase & 0x80) != 0) out = LOGSIN[((phase ^ 0xff) << 1) & 0xff];
            else out = LOGSIN[(phase << 1) & 0xff];
            return (short) (calcExp(out + (envelope << 3)) ^ neg);
        case 5:
            if ((phase & 0x200) != 0) out = 0x1000;
            else if ((phase & 0x80) != 0) out = LOGSIN[((phase ^ 0xff) << 1) & 0xff];
            else out = LOGSIN[(phase << 1) & 0xff];
            return (short) calcExp(out + (envelope << 3));
        case 6:
            if ((phase & 0x200) != 0) neg = 0xffff;
            return (short) (calcExp(envelope << 3) ^ neg);
        default:
            if ((phase & 0x200) != 0)
            {
                neg = 0xffff;
                phase = (phase & 0x1ff) ^ 0x1ff;
            }
            out = (phase << 3) & 0xffff;
            return (short) (calcExp(out + (envelope << 3)) ^ neg);
        }
    }

    private static void envelopeUpdateKSL(Slot slot)
    {
        int ksl = (short) ((KSLROM[slot.channel.fNum >> 6] << 2) - ((0x08 - slot.channel.block) << 5));
        if (ksl < 0)
            ksl = 0;
        slot.egKsl = ksl & 0xff;
    }

    private void envelopeCalc(Slot slot)
    {
        int regRate = 0, reset = 0;
        slot.egOut = (slot.egRout + (slot.regTl << 2) + (slot.egKsl >> KSLSHIFT[slot.regKsl]) + (slot.trem ? tremolo : 0)) & 0xffff;
        if (slot.key != 0 && slot.egGen == EG_RELEASE)
        {
            reset = 1;
            regRate = slot.regAr;
        }
        else
        {
            switch (slot.egGen)
            {
            case EG_ATTACK: regRate = slot.regAr; break;
            case EG_DECAY: regRate = slot.regDr; break;
            case EG_SUSTAIN: if (slot.regType == 0) regRate = slot.regRr; break;
            case EG_RELEASE: regRate = slot.regRr; break;
            }
        }
        slot.pgReset = reset;
        int ks = slot.channel.ksv >> ((slot.regKsr ^ 1) << 1);
        boolean nonzero = regRate != 0;
        int rate = (ks + (regRate << 2)) & 0xff;
        int rateHi = rate >> 2, rateLo = rate & 0x03;
        if ((rateHi & 0x10) != 0)
            rateHi = 0x0f;
        int egShift = (rateHi + egAdd) & 0xff;
        int shift = 0;
        if (nonzero)
        {
            if (rateHi < 12)
            {
                if (egState != 0)
                {
                    switch (egShift)
                    {
                    case 12: shift = 1; break;
                    case 13: shift = (rateLo >> 1) & 0x01; break;
                    case 14: shift = rateLo & 0x01; break;
                    default: break;
                    }
                }
            }
            else
            {
                shift = ((rateHi & 0x03) + EG_INCSTEP[rateLo][egTimerLo]) & 0xff;
                if ((shift & 0x04) != 0)
                    shift = 0x03;
                if (shift == 0)
                    shift = egState;
            }
        }
        int egRout = slot.egRout;
        int egInc = 0;
        boolean egOff = false;
        if (reset != 0 && rateHi == 0x0f)
            egRout = 0x00;
        if ((slot.egRout & 0x1f8) == 0x1f8)
            egOff = true;
        if (slot.egGen != EG_ATTACK && reset == 0 && egOff)
            egRout = 0x1ff;
        switch (slot.egGen)
        {
        case EG_ATTACK:
            if (slot.egRout == 0)
                slot.egGen = EG_DECAY;
            else if (slot.key != 0 && shift > 0 && rateHi != 0x0f)
                egInc = (short) (~slot.egRout >> (4 - shift));
            break;
        case EG_DECAY:
            if ((slot.egRout >> 4) == slot.regSl)
                slot.egGen = EG_SUSTAIN;
            else if (!egOff && reset == 0 && shift > 0)
                egInc = 1 << (shift - 1);
            break;
        case EG_SUSTAIN:
        case EG_RELEASE:
            if (!egOff && reset == 0 && shift > 0)
                egInc = 1 << (shift - 1);
            break;
        }
        slot.egRout = (egRout + egInc) & 0x1ff;
        if (reset != 0)
            slot.egGen = EG_ATTACK;
        if (slot.key == 0)
            slot.egGen = EG_RELEASE;
    }

    private static void envelopeKeyOn(Slot slot, int type)  { slot.key |= type; }
    private static void envelopeKeyOff(Slot slot, int type) { slot.key &= ~type & 0xff; }

    // --- phase generator ---------------------------------------------------------

    private void phaseGenerate(Slot slot)
    {
        int fNum = slot.channel.fNum;
        if (slot.regVib != 0)
        {
            int range = (fNum >> 7) & 7;
            int vp = vibpos;
            if ((vp & 3) == 0)
                range = 0;
            else if ((vp & 1) != 0)
                range >>= 1;
            range >>= vibshift;
            if ((vp & 4) != 0)
                range = -range;
            fNum = (fNum + range) & 0xffff;
        }
        int basefreq = (fNum << slot.channel.block) >>> 1;
        int phase = (slot.pgPhase >>> 9) & 0xffff;
        if (slot.pgReset != 0)
            slot.pgPhase = 0;
        slot.pgPhase += (basefreq * MT[slot.regMult]) >>> 1;
        int nz = noise;
        slot.pgPhaseOut = phase;
        if (slot.slotNum == 13)
        {
            rmHhBit2 = (phase >> 2) & 1;
            rmHhBit3 = (phase >> 3) & 1;
            rmHhBit7 = (phase >> 7) & 1;
            rmHhBit8 = (phase >> 8) & 1;
        }
        if (slot.slotNum == 17 && (rhy & 0x20) != 0)
        {
            rmTcBit3 = (phase >> 3) & 1;
            rmTcBit5 = (phase >> 5) & 1;
        }
        if ((rhy & 0x20) != 0)
        {
            int rmXor = (rmHhBit2 ^ rmHhBit7) | (rmHhBit3 ^ rmTcBit5) | (rmTcBit3 ^ rmTcBit5);
            switch (slot.slotNum)
            {
            case 13:
                slot.pgPhaseOut = rmXor << 9;
                if ((rmXor ^ (nz & 1)) != 0)
                    slot.pgPhaseOut |= 0xd0;
                else
                    slot.pgPhaseOut |= 0x34;
                break;
            case 16:
                slot.pgPhaseOut = (rmHhBit8 << 9) | ((rmHhBit8 ^ (nz & 1)) << 8);
                break;
            case 17:
                slot.pgPhaseOut = (rmXor << 9) | 0x80;
                break;
            default:
                break;
            }
        }
        int nBit = ((nz >>> 14) ^ nz) & 0x01;
        noise = (nz >>> 1) | (nBit << 22);
    }

    // --- slot -------------------------------------------------------------------

    private void slotWrite20(Slot slot, int data)
    {
        slot.trem = ((data >> 7) & 0x01) != 0;
        slot.regVib = (data >> 6) & 0x01;
        slot.regType = (data >> 5) & 0x01;
        slot.regKsr = (data >> 4) & 0x01;
        slot.regMult = data & 0x0f;
    }

    private static void slotWrite40(Slot slot, int data)
    {
        slot.regKsl = (data >> 6) & 0x03;
        slot.regTl = data & 0x3f;
        envelopeUpdateKSL(slot);
    }

    private static void slotWrite60(Slot slot, int data)
    {
        slot.regAr = (data >> 4) & 0x0f;
        slot.regDr = data & 0x0f;
    }

    private static void slotWrite80(Slot slot, int data)
    {
        slot.regSl = (data >> 4) & 0x0f;
        if (slot.regSl == 0x0f)
            slot.regSl = 0x1f;
        slot.regRr = data & 0x0f;
    }

    private void slotWriteE0(Slot slot, int data)
    {
        slot.regWf = data & 0x07;
        if (newm == 0x00)
            slot.regWf &= 0x03;
    }

    private static void slotGenerate(Slot slot)
    {
        slot.out = envelopeSin(slot.regWf, (slot.pgPhaseOut + slot.mod.get()) & 0xffff, slot.egOut);
    }

    private static void slotCalcFB(Slot slot)
    {
        if (slot.channel.fb != 0x00)
            slot.fbmod = (short) ((slot.prout + slot.out) >> (0x09 - slot.channel.fb));
        else
            slot.fbmod = 0;
        slot.prout = slot.out;
    }

    // --- channel ----------------------------------------------------------------

    private void channelUpdateRhythm(int data)
    {
        rhy = data & 0x3f;
        if ((rhy & 0x20) != 0)
        {
            Channel channel6 = channel[6], channel7 = channel[7], channel8 = channel[8];
            channel6.out[0] = channel6.slotz[1].outSrc;
            channel6.out[1] = channel6.slotz[1].outSrc;
            channel6.out[2] = ZERO;
            channel6.out[3] = ZERO;
            channel7.out[0] = channel7.slotz[0].outSrc;
            channel7.out[1] = channel7.slotz[0].outSrc;
            channel7.out[2] = channel7.slotz[1].outSrc;
            channel7.out[3] = channel7.slotz[1].outSrc;
            channel8.out[0] = channel8.slotz[0].outSrc;
            channel8.out[1] = channel8.slotz[0].outSrc;
            channel8.out[2] = channel8.slotz[1].outSrc;
            channel8.out[3] = channel8.slotz[1].outSrc;
            for (int chnum = 6; chnum < 9; chnum++)
                channel[chnum].chtype = CH_DRUM;
            channelSetupAlg(channel6);
            channelSetupAlg(channel7);
            channelSetupAlg(channel8);
            if ((rhy & 0x01) != 0) envelopeKeyOn(channel7.slotz[0], EGK_DRUM); else envelopeKeyOff(channel7.slotz[0], EGK_DRUM);
            if ((rhy & 0x02) != 0) envelopeKeyOn(channel8.slotz[1], EGK_DRUM); else envelopeKeyOff(channel8.slotz[1], EGK_DRUM);
            if ((rhy & 0x04) != 0) envelopeKeyOn(channel8.slotz[0], EGK_DRUM); else envelopeKeyOff(channel8.slotz[0], EGK_DRUM);
            if ((rhy & 0x08) != 0) envelopeKeyOn(channel7.slotz[1], EGK_DRUM); else envelopeKeyOff(channel7.slotz[1], EGK_DRUM);
            if ((rhy & 0x10) != 0)
            {
                envelopeKeyOn(channel6.slotz[0], EGK_DRUM);
                envelopeKeyOn(channel6.slotz[1], EGK_DRUM);
            }
            else
            {
                envelopeKeyOff(channel6.slotz[0], EGK_DRUM);
                envelopeKeyOff(channel6.slotz[1], EGK_DRUM);
            }
        }
        else
        {
            for (int chnum = 6; chnum < 9; chnum++)
            {
                channel[chnum].chtype = CH_2OP;
                channelSetupAlg(channel[chnum]);
                envelopeKeyOff(channel[chnum].slotz[0], EGK_DRUM);
                envelopeKeyOff(channel[chnum].slotz[1], EGK_DRUM);
            }
        }
    }

    private void channelUpdateFrequency(Channel ch)
    {
        ch.ksv = ((ch.block << 1) | ((ch.fNum >> (0x09 - nts)) & 0x01)) & 0xff;
        envelopeUpdateKSL(ch.slotz[0]);
        envelopeUpdateKSL(ch.slotz[1]);
    }

    private void channelRestoreFrequency(Channel ch)
    {
        ch.fNum = ch.fNumReg;
        ch.block = ch.blockReg;
        channelUpdateFrequency(ch);
    }

    private void channelSync4Op(Channel ch)
    {
        ch.pair.fNum = ch.fNum;
        ch.pair.block = ch.block;
        channelUpdateFrequency(ch.pair);
    }

    private void channelWriteA0(Channel ch, int data)
    {
        ch.fNumReg = (ch.fNumReg & 0x300) | data;
        if (ch.chtype == CH_4OP2)
            return;
        channelRestoreFrequency(ch);
        if (ch.chtype == CH_4OP)
            channelSync4Op(ch);
    }

    private void channelWriteB0(Channel ch, int data)
    {
        ch.fNumReg = (ch.fNumReg & 0xff) | ((data & 0x03) << 8);
        ch.blockReg = (data >> 2) & 0x07;
        if (ch.chtype == CH_4OP2)
            return;
        channelRestoreFrequency(ch);
        if (ch.chtype == CH_4OP)
            channelSync4Op(ch);
    }

    private void channelSetupAlg(Channel ch)
    {
        if (ch.chtype == CH_DRUM)
        {
            if (ch.chNum == 7 || ch.chNum == 8)
            {
                ch.slotz[0].mod = ZERO;
                ch.slotz[1].mod = ZERO;
                return;
            }
            switch (ch.alg & 0x01)
            {
            case 0x00:
                ch.slotz[0].mod = ch.slotz[0].fbSrc;
                ch.slotz[1].mod = ch.slotz[0].outSrc;
                break;
            case 0x01:
                ch.slotz[0].mod = ch.slotz[0].fbSrc;
                ch.slotz[1].mod = ZERO;
                break;
            }
            return;
        }
        if ((ch.alg & 0x08) != 0)
            return;
        if ((ch.alg & 0x04) != 0)
        {
            Channel p = ch.pair;
            p.out[0] = ZERO;
            p.out[1] = ZERO;
            p.out[2] = ZERO;
            p.out[3] = ZERO;
            switch (ch.alg & 0x03)
            {
            case 0x00:
                p.slotz[0].mod = p.slotz[0].fbSrc;
                p.slotz[1].mod = p.slotz[0].outSrc;
                ch.slotz[0].mod = p.slotz[1].outSrc;
                ch.slotz[1].mod = ch.slotz[0].outSrc;
                ch.out[0] = ch.slotz[1].outSrc;
                ch.out[1] = ZERO;
                ch.out[2] = ZERO;
                ch.out[3] = ZERO;
                break;
            case 0x01:
                p.slotz[0].mod = p.slotz[0].fbSrc;
                p.slotz[1].mod = p.slotz[0].outSrc;
                ch.slotz[0].mod = ZERO;
                ch.slotz[1].mod = ch.slotz[0].outSrc;
                ch.out[0] = p.slotz[1].outSrc;
                ch.out[1] = ch.slotz[1].outSrc;
                ch.out[2] = ZERO;
                ch.out[3] = ZERO;
                break;
            case 0x02:
                p.slotz[0].mod = p.slotz[0].fbSrc;
                p.slotz[1].mod = ZERO;
                ch.slotz[0].mod = p.slotz[1].outSrc;
                ch.slotz[1].mod = ch.slotz[0].outSrc;
                ch.out[0] = p.slotz[0].outSrc;
                ch.out[1] = ch.slotz[1].outSrc;
                ch.out[2] = ZERO;
                ch.out[3] = ZERO;
                break;
            case 0x03:
                p.slotz[0].mod = p.slotz[0].fbSrc;
                p.slotz[1].mod = ZERO;
                ch.slotz[0].mod = p.slotz[1].outSrc;
                ch.slotz[1].mod = ZERO;
                ch.out[0] = p.slotz[0].outSrc;
                ch.out[1] = ch.slotz[0].outSrc;
                ch.out[2] = ch.slotz[1].outSrc;
                ch.out[3] = ZERO;
                break;
            }
        }
        else
        {
            switch (ch.alg & 0x01)
            {
            case 0x00:
                ch.slotz[0].mod = ch.slotz[0].fbSrc;
                ch.slotz[1].mod = ch.slotz[0].outSrc;
                ch.out[0] = ch.slotz[1].outSrc;
                ch.out[1] = ZERO;
                ch.out[2] = ZERO;
                ch.out[3] = ZERO;
                break;
            case 0x01:
                ch.slotz[0].mod = ch.slotz[0].fbSrc;
                ch.slotz[1].mod = ZERO;
                ch.out[0] = ch.slotz[0].outSrc;
                ch.out[1] = ch.slotz[1].outSrc;
                ch.out[2] = ZERO;
                ch.out[3] = ZERO;
                break;
            }
        }
    }

    private void channelUpdateAlg(Channel ch)
    {
        ch.alg = ch.con;
        if (ch.chtype == CH_4OP)
        {
            ch.pair.alg = 0x04 | (ch.con << 1) | ch.pair.con;
            ch.alg = 0x08;
            channelSetupAlg(ch.pair);
        }
        else if (ch.chtype == CH_4OP2)
        {
            ch.alg = 0x04 | (ch.pair.con << 1) | ch.con;
            ch.pair.alg = 0x08;
            channelSetupAlg(ch);
        }
        else
            channelSetupAlg(ch);
    }

    private void channelWriteC0(Channel ch, int data)
    {
        ch.fb = (data & 0x0e) >> 1;
        ch.con = data & 0x01;
        channelUpdateAlg(ch);
        if (newm != 0)
        {
            ch.cha = ((data >> 4) & 0x01) != 0 ? 0xffff : 0;
            ch.chb = ((data >> 5) & 0x01) != 0 ? 0xffff : 0;
            ch.chc = ((data >> 6) & 0x01) != 0 ? 0xffff : 0;
            ch.chd = ((data >> 7) & 0x01) != 0 ? 0xffff : 0;
        }
        else
        {
            ch.cha = ch.chb = 0xffff;
            ch.chc = ch.chd = 0;
        }
    }

    private static void channelKeyOn(Channel ch)
    {
        if (ch.chtype == CH_4OP)
        {
            envelopeKeyOn(ch.slotz[0], EGK_NORM);
            envelopeKeyOn(ch.slotz[1], EGK_NORM);
            envelopeKeyOn(ch.pair.slotz[0], EGK_NORM);
            envelopeKeyOn(ch.pair.slotz[1], EGK_NORM);
        }
        else if (ch.chtype == CH_2OP || ch.chtype == CH_DRUM)
        {
            envelopeKeyOn(ch.slotz[0], EGK_NORM);
            envelopeKeyOn(ch.slotz[1], EGK_NORM);
        }
    }

    private static void channelKeyOff(Channel ch)
    {
        if (ch.chtype == CH_4OP)
        {
            envelopeKeyOff(ch.slotz[0], EGK_NORM);
            envelopeKeyOff(ch.slotz[1], EGK_NORM);
            envelopeKeyOff(ch.pair.slotz[0], EGK_NORM);
            envelopeKeyOff(ch.pair.slotz[1], EGK_NORM);
        }
        else if (ch.chtype == CH_2OP || ch.chtype == CH_DRUM)
        {
            envelopeKeyOff(ch.slotz[0], EGK_NORM);
            envelopeKeyOff(ch.slotz[1], EGK_NORM);
        }
    }

    private void channelSet4Op(int data)
    {
        for (int bit = 0; bit < 6; bit++)
        {
            int chnum = bit;
            if (bit >= 3)
                chnum += 9 - 3;
            if (((data >> bit) & 0x01) != 0)
            {
                channel[chnum].chtype = CH_4OP;
                channel[chnum + 3].chtype = CH_4OP2;
                channelSync4Op(channel[chnum]);
                channelUpdateAlg(channel[chnum]);
            }
            else
            {
                channel[chnum].chtype = CH_2OP;
                channel[chnum + 3].chtype = CH_2OP;
                channelRestoreFrequency(channel[chnum + 3]);
                channelUpdateAlg(channel[chnum]);
                channelUpdateAlg(channel[chnum + 3]);
            }
        }
    }

    private static int clipSample(int sample)
    {
        if (sample > 32767)
            sample = 32767;
        else if (sample < -32768)
            sample = -32768;
        return sample;
    }

    private void processSlot(Slot s)
    {
        slotCalcFB(s);
        envelopeCalc(s);
        phaseGenerate(s);
        slotGenerate(s);
    }

    // --- output -----------------------------------------------------------------

    /** One sample at the chip's own rate into buf4: left, right, and OPL3's two other outputs. */
    public void generate4Ch(int[] buf4)
    {
        buf4[1] = clipSample(mixbuff[1]);
        buf4[3] = clipSample(mixbuff[3]);
        for (int ii = 0; ii < 15; ii++)
            processSlot(slot[ii]);
        int mix0 = 0, mix1 = 0;
        for (int ii = 0; ii < 18; ii++)
        {
            Channel ch = channel[ii];
            int accm = (short) (ch.out[0].get() + ch.out[1].get() + ch.out[2].get() + ch.out[3].get());
            mix0 += (short) (accm & ch.cha);
            mix1 += (short) (accm & ch.chc);
        }
        mixbuff[0] = mix0;
        mixbuff[2] = mix1;
        for (int ii = 15; ii < 18; ii++)
            processSlot(slot[ii]);
        buf4[0] = clipSample(mixbuff[0]);
        buf4[2] = clipSample(mixbuff[2]);
        for (int ii = 18; ii < 33; ii++)
            processSlot(slot[ii]);
        mix0 = mix1 = 0;
        for (int ii = 0; ii < 18; ii++)
        {
            Channel ch = channel[ii];
            int accm = (short) (ch.out[0].get() + ch.out[1].get() + ch.out[2].get() + ch.out[3].get());
            mix0 += (short) (accm & ch.chb);
            mix1 += (short) (accm & ch.chd);
        }
        mixbuff[1] = mix0;
        mixbuff[3] = mix1;
        for (int ii = 33; ii < 36; ii++)
            processSlot(slot[ii]);

        if ((timer & 0x3f) == 0x3f)
            tremolopos = (tremolopos + 1) % 210;
        if (tremolopos < 105)
            tremolo = tremolopos >> tremoloshift;
        else
            tremolo = (210 - tremolopos) >> tremoloshift;
        if ((timer & 0x3ff) == 0x3ff)
            vibpos = (vibpos + 1) & 7;
        timer = (timer + 1) & 0xffff;

        if (egState != 0)
        {
            int shift = 0;
            while (shift < 13 && ((egTimer >> shift) & 1) == 0)
                shift++;
            egAdd = shift > 12 ? 0 : shift + 1;
            egTimerLo = (int) (egTimer & 0x3);
        }
        if (egTimerrem != 0 || egState != 0)
        {
            if (egTimer == 0xfffffffffL)
            {
                egTimer = 0;
                egTimerrem = 1;
            }
            else
            {
                egTimer++;
                egTimerrem = 0;
            }
        }
        egState ^= 1;
    }

    /** One sample at the chip's own rate: buf[0] left, buf[1] right. */
    public void generate(int[] buf)
    {
        int[] s = new int[4];
        generate4Ch(s);
        buf[0] = s[0];
        buf[1] = s[1];
    }

    /** One sample at the rate given to the constructor (linear interpolation, as the C). */
    public void generateResampled(int[] buf)
    {
        while (samplecnt >= rateratio)
        {
            System.arraycopy(samples, 0, oldsamples, 0, 4);
            generate4Ch(samples);
            samplecnt -= rateratio;
        }
        buf[0] = (short) ((oldsamples[0] * (rateratio - samplecnt) + samples[0] * samplecnt) / rateratio);
        buf[1] = (short) ((oldsamples[1] * (rateratio - samplecnt) + samples[1] * samplecnt) / rateratio);
        samplecnt += 1 << RSM_FRAC;
    }

    public void reset(int samplerate)
    {
        for (int i = 0; i < 36; i++)
            slot[i] = new Slot();
        for (int i = 0; i < 18; i++)
            channel[i] = new Channel();
        timer = 0; egTimer = 0; egTimerrem = 0; egState = 0; egAdd = 0; egTimerLo = 0;
        newm = 0; nts = 0; rhy = 0; vibpos = 0; tremolo = 0; tremolopos = 0;
        rmHhBit2 = rmHhBit3 = rmHhBit7 = rmHhBit8 = rmTcBit3 = rmTcBit5 = 0;
        java.util.Arrays.fill(mixbuff, 0);
        java.util.Arrays.fill(oldsamples, 0);
        java.util.Arrays.fill(samples, 0);
        samplecnt = 0;
        for (int slotnum = 0; slotnum < 36; slotnum++)
        {
            Slot s = slot[slotnum];
            s.mod = ZERO;
            s.egRout = 0x1ff;
            s.egOut = 0x1ff;
            s.egGen = EG_RELEASE;
            s.trem = false;
            s.slotNum = slotnum;
        }
        for (int channum = 0; channum < 18; channum++)
        {
            Channel ch = channel[channum];
            int local = CH_SLOT[channum];
            ch.slotz[0] = slot[local];
            ch.slotz[1] = slot[local + 3];
            slot[local].channel = ch;
            slot[local + 3].channel = ch;
            if ((channum % 9) < 3)
                ch.pair = channel[channum + 3];
            else if ((channum % 9) < 6)
                ch.pair = channel[channum - 3];
            ch.out[0] = ZERO;
            ch.out[1] = ZERO;
            ch.out[2] = ZERO;
            ch.out[3] = ZERO;
            ch.chtype = CH_2OP;
            ch.cha = 0xffff;
            ch.chb = 0xffff;
            ch.chNum = channum;
            channelSetupAlg(ch);
        }
        noise = 1;
        rateratio = (int) (((long) samplerate << RSM_FRAC) / RATE);
        tremoloshift = 4;
        vibshift = 1;
    }

    /** Writes a register: 0x000-0x0FF the first bank (all an OPL2 has), 0x100-0x1FF the second. */
    public void writeReg(int reg, int v)
    {
        v &= 0xff;
        int high = (reg >> 8) & 0x01;
        int regm = reg & 0xff;
        switch (regm & 0xf0)
        {
        case 0x00:
            if (high != 0)
            {
                switch (regm & 0x0f)
                {
                case 0x04: channelSet4Op(v); break;
                case 0x05: newm = v & 0x01; break;
                }
            }
            else if ((regm & 0x0f) == 0x08)
                nts = (v >> 6) & 0x01;
            break;
        case 0x20:
        case 0x30:
            if (AD_SLOT[regm & 0x1f] >= 0)
                slotWrite20(slot[18 * high + AD_SLOT[regm & 0x1f]], v);
            break;
        case 0x40:
        case 0x50:
            if (AD_SLOT[regm & 0x1f] >= 0)
                slotWrite40(slot[18 * high + AD_SLOT[regm & 0x1f]], v);
            break;
        case 0x60:
        case 0x70:
            if (AD_SLOT[regm & 0x1f] >= 0)
                slotWrite60(slot[18 * high + AD_SLOT[regm & 0x1f]], v);
            break;
        case 0x80:
        case 0x90:
            if (AD_SLOT[regm & 0x1f] >= 0)
                slotWrite80(slot[18 * high + AD_SLOT[regm & 0x1f]], v);
            break;
        case 0xe0:
        case 0xf0:
            if (AD_SLOT[regm & 0x1f] >= 0)
                slotWriteE0(slot[18 * high + AD_SLOT[regm & 0x1f]], v);
            break;
        case 0xa0:
            if ((regm & 0x0f) < 9)
                channelWriteA0(channel[9 * high + (regm & 0x0f)], v);
            break;
        case 0xb0:
            if (regm == 0xbd && high == 0)
            {
                tremoloshift = (((v >> 7) ^ 1) << 1) + 2;
                vibshift = ((v >> 6) & 0x01) ^ 1;
                channelUpdateRhythm(v);
            }
            else if ((regm & 0x0f) < 9)
            {
                Channel ch = channel[9 * high + (regm & 0x0f)];
                channelWriteB0(ch, v);
                if ((v & 0x20) != 0)
                    channelKeyOn(ch);
                else
                    channelKeyOff(ch);
            }
            break;
        case 0xc0:
            if ((regm & 0x0f) < 9)
                channelWriteC0(channel[9 * high + (regm & 0x0f)], v);
            break;
        }
    }
}
