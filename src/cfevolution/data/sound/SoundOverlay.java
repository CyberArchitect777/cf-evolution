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
package cfevolution.data.sound;

/**
 * The game's AdLib sound effects overlay (XSOUND.BIN) run as the game runs
 * it (2026-11-19): its own 8086 code, in a small interpreter of the 16-bit
 * instructions it uses, with its writes to the AdLib's ports (388h, 389h)
 * going to an OPL chip. So a preview plays exactly the file, edits and all.
 *
 * The overlay is loaded at offset 0 of its own segment and addresses its data
 * through CS; the stack is kept apart. A read or write through DS or ES
 * would reach the game's own memory in the game, which this does not have:
 * such accesses read 0 and are counted (strayAccesses), and an instruction
 * outside the interpreted set stops the call with an exception. Both are
 * checked by harnesses/SoundPreviewGate never to happen.
 *
 * Calls are made as GP.EXE makes them (CallOverlayFunction: a far call
 * through the table at 24h); the 300 Hz and 50 Hz ticks as its timer does
 * (IntVector8 / sub_8BAA6: function 1 every tick, function 6 every sixth).
 */
public final class SoundOverlay
{
    /** The overlay's functions, numbered as the game's jump table. */
    public static final int TICK_300 = 1, INIT = 2, SILENCE = 3, START = 4, STOP = 5, TICK_50 = 6;

    private static final int CS = 0x1000, SENTINEL = 0xF00D;
    private static final long STEP_LIMIT = 2000000;

    private final byte[] code = new byte[0x10000];
    private final byte[] stack = new byte[0x10000];
    private final Opl3 chip;
    private int index;                  // the AdLib's selected register

    // registers: AX CX DX BX SP BP SI DI, flags
    private final int[] r = new int[8];
    private int ip;
    private boolean cf, zf, sf, of;
    private int segOverride;            // -1 none, else 0 ES, 1 CS, 2 SS, 3 DS
    private static final int GAME = 0x2000;  // DS and ES as the game calls in: its own data
    private int ds = GAME, es = GAME;
    private int strays;

    /** Sees every register write the overlay makes (for checks). */
    public interface Writes
    {
        void write(int register, int value);
    }

    private Writes writes;

    public void setWrites(Writes w)
    {
        writes = w;
    }

    public SoundOverlay(byte[] file, Opl3 chip)
    {
        System.arraycopy(file, 0, code, 0, Math.min(file.length, code.length));
        this.chip = chip;
    }

    /** Accesses that would have reached the game's own memory (should stay 0). */
    public int strayAccesses()
    {
        return strays;
    }

    /** The overlay's word at offset (the game's inputs are 00h-15h). */
    public int word(int offset)
    {
        return (code[offset] & 0xFF) | ((code[offset + 1] & 0xFF) << 8);
    }

    public void setWord(int offset, int value)
    {
        code[offset] = (byte) value;
        code[offset + 1] = (byte) (value >> 8);
    }

    /** Calls function n of the table at 24h with ax, as CallOverlayFunction does. */
    public void call(int function, int ax)
    {
        int target = word(0x24 + 2 * (function - 1));
        r[0] = ax & 0xFFFF;
        r[4] = 0xFFF0;
        ds = es = GAME;
        push(SENTINEL);                 // far return: CS, then IP
        push(SENTINEL);
        ip = target;
        long steps = 0;
        while (true)
        {
            if (ip == SENTINEL)
                return;
            if (++steps > STEP_LIMIT)
                throw new IllegalStateException("the sound overlay did not return from function " + function);
            step();
        }
    }

    // --- memory -----------------------------------------------------------------

    private int seg(int dflt)
    {
        return segOverride >= 0 ? segOverride : dflt;
    }

    /** ES or DS holding the overlay's segment reach the overlay. */
    private int real(int seg)
    {
        if (seg == 3)
            return ds == CS ? 1 : 3;
        if (seg == 0)
            return es == CS ? 1 : 0;
        return seg;
    }

    private int rd8(int seg, int off)
    {
        off &= 0xFFFF;
        seg = real(seg);
        if (seg == 1)
            return code[off] & 0xFF;
        if (seg == 2)
            return stack[off] & 0xFF;
        strays++;
        return 0;
    }

    private void wr8(int seg, int off, int v)
    {
        off &= 0xFFFF;
        seg = real(seg);
        if (seg == 1)
            code[off] = (byte) v;
        else if (seg == 2)
            stack[off] = (byte) v;
        else
            strays++;
    }

    private int rd16(int seg, int off)  { return rd8(seg, off) | (rd8(seg, off + 1) << 8); }
    private void wr16(int seg, int off, int v) { wr8(seg, off, v); wr8(seg, off + 1, v >> 8); }

    private int fetch8()  { int v = code[ip] & 0xFF; ip = (ip + 1) & 0xFFFF; return v; }
    private int fetch16() { int v = fetch8(); return v | (fetch8() << 8); }

    private void push(int v)
    {
        r[4] = (r[4] - 2) & 0xFFFF;
        wr16(2, r[4], v);
    }

    private int pop()
    {
        int v = rd16(2, r[4]);
        r[4] = (r[4] + 2) & 0xFFFF;
        return v;
    }

    // --- registers --------------------------------------------------------------

    private int reg8(int n)  { return n < 4 ? r[n] & 0xFF : (r[n - 4] >> 8) & 0xFF; }

    private void setReg8(int n, int v)
    {
        v &= 0xFF;
        if (n < 4)
            r[n] = (r[n] & 0xFF00) | v;
        else
            r[n - 4] = (r[n - 4] & 0x00FF) | (v << 8);
    }

    // --- ModR/M -----------------------------------------------------------------

    private int mod, regField, rm, ea, eaSeg;

    private void modrm()
    {
        int b = fetch8();
        mod = b >> 6;
        regField = (b >> 3) & 7;
        rm = b & 7;
        if (mod == 3)
            return;
        int disp = 0;
        int dseg = 3;
        if (mod == 0 && rm == 6)
            disp = fetch16();
        else if (mod == 1)
            disp = (byte) fetch8();
        else if (mod == 2)
            disp = fetch16();
        int base;
        switch (rm)
        {
        case 0: base = r[3] + r[6]; break;
        case 1: base = r[3] + r[7]; break;
        case 2: base = r[5] + r[6]; dseg = 2; break;
        case 3: base = r[5] + r[7]; dseg = 2; break;
        case 4: base = r[6]; break;
        case 5: base = r[7]; break;
        case 6: if (mod == 0) base = 0; else { base = r[5]; dseg = 2; } break;
        default: base = r[3]; break;
        }
        ea = (base + disp) & 0xFFFF;
        eaSeg = seg(dseg);
    }

    private int getRM8()  { return mod == 3 ? reg8(rm) : rd8(eaSeg, ea); }
    private int getRM16() { return mod == 3 ? r[rm] : rd16(eaSeg, ea); }
    private void setRM8(int v)  { if (mod == 3) setReg8(rm, v); else wr8(eaSeg, ea, v); }
    private void setRM16(int v) { if (mod == 3) r[rm] = v & 0xFFFF; else wr16(eaSeg, ea, v); }

    // --- arithmetic -------------------------------------------------------------

    private void szFlags(int v, boolean word)
    {
        int m = word ? 0xFFFF : 0xFF;
        zf = (v & m) == 0;
        sf = (v & (word ? 0x8000 : 0x80)) != 0;
    }

    /** ADD OR ADC SBB AND SUB XOR CMP, as numbered in the opcode map. */
    private int alu(int op, int a, int b, boolean word)
    {
        int m = word ? 0xFFFF : 0xFF, top = word ? 0x8000 : 0x80;
        int res;
        switch (op)
        {
        case 0: case 2:
        {
            int c = op == 2 && cf ? 1 : 0;
            res = a + b + c;
            cf = res > m;
            of = ((a ^ res) & (b ^ res) & top) != 0;
            break;
        }
        case 3: case 5: case 7:
        {
            int c = op == 3 && cf ? 1 : 0;
            res = a - b - c;
            cf = res < 0;
            of = ((a ^ b) & (a ^ res) & top) != 0;
            break;
        }
        case 1: res = a | b; cf = of = false; break;
        case 4: res = a & b; cf = of = false; break;
        default: res = a ^ b; cf = of = false; break;
        }
        res &= m;
        szFlags(res, word);
        return res;
    }

    /** ROL ROR RCL RCR SHL SHR (SAL) SAR by count. */
    private int shift(int op, int v, int count, boolean word)
    {
        int bits = word ? 16 : 8, m = word ? 0xFFFF : 0xFF, top = word ? 0x8000 : 0x80;
        count &= 0x1F;
        if (count == 0)
            return v;
        for (int i = 0; i < count; i++)
        {
            switch (op)
            {
            case 0: { boolean hi = (v & top) != 0; v = ((v << 1) | (hi ? 1 : 0)) & m; cf = hi; break; }
            case 1: { boolean lo = (v & 1) != 0; v = (v >> 1) | (lo ? top : 0); cf = lo; break; }
            case 2: { boolean hi = (v & top) != 0; v = ((v << 1) | (cf ? 1 : 0)) & m; cf = hi; break; }
            case 3: { boolean lo = (v & 1) != 0; v = (v >> 1) | (cf ? top : 0); cf = lo; break; }
            case 4: case 6: { cf = (v & top) != 0; v = (v << 1) & m; break; }
            case 5: { cf = (v & 1) != 0; v = v >> 1; break; }
            default: { cf = (v & 1) != 0; v = (v >> 1) | (v & top); break; }
            }
        }
        if (op >= 4)
            szFlags(v, word);
        if (op == 0 || op == 2 || op == 4 || op == 6)
            of = ((v & top) != 0) != cf;
        else if (op == 1 || op == 3)
            of = ((v ^ (v << 1)) & top) != 0;
        else if (op == 5)
            of = count == 1 && ((v << 1) & top) != 0;      // the operand's old top bit
        else
            of = false;
        return v & m;
        // (bits kept for clarity of the masks above)
    }

    private boolean cond(int c)
    {
        boolean t;
        switch (c >> 1)
        {
        case 0: t = of; break;
        case 1: t = cf; break;
        case 2: t = zf; break;
        case 3: t = cf || zf; break;
        case 4: t = sf; break;
        case 5: throw new IllegalStateException("parity jump at " + Integer.toHexString(ip));
        case 6: t = sf != of; break;
        default: t = zf || sf != of; break;
        }
        return (c & 1) == 0 ? t : !t;
    }

    // --- ports ------------------------------------------------------------------

    private void out(int port, int v)
    {
        if (port == 0x388)
            index = v & 0xFF;
        else if (port == 0x389)
        {
            chip.writeReg(index, v & 0xFF);
            if (writes != null)
                writes.write(index, v & 0xFF);
        }
        // anything else (a VGA palette flash in unused code) has no effect here
    }

    private int in(int port)
    {
        return 0;                       // the overlay only reads 388h to wait
    }

    // --- one instruction -------------------------------------------------------

    private void step()
    {
        segOverride = -1;
        int start = ip;
        int op = fetch8();
        while (op == 0x26 || op == 0x2E || op == 0x36 || op == 0x3E)
        {
            segOverride = (op >> 3) & 3;
            op = fetch8();
        }
        if (op < 0x40 && (op & 7) < 6)
        {
            int kind = op >> 3, form = op & 7;
            switch (form)
            {
            case 0: modrm(); { int v = alu(kind, getRM8(), reg8(regField), false); if (kind != 7) setRM8(v); } return;
            case 1: modrm(); { int v = alu(kind, getRM16(), r[regField], true); if (kind != 7) setRM16(v); } return;
            case 2: modrm(); { int v = alu(kind, reg8(regField), getRM8(), false); if (kind != 7) setReg8(regField, v); } return;
            case 3: modrm(); { int v = alu(kind, r[regField], getRM16(), true); if (kind != 7) r[regField] = v; } return;
            case 4: { int v = alu(kind, r[0] & 0xFF, fetch8(), false); if (kind != 7) setReg8(0, v); } return;
            default: { int v = alu(kind, r[0], fetch16(), true); if (kind != 7) r[0] = v; } return;
            }
        }
        if (op == 0x0E) { push(CS); return; }
        if (op == 0x1E) { push(ds); return; }
        if (op == 0x1F) { ds = pop(); return; }
        if (op == 0x06) { push(es); return; }
        if (op == 0x07) { es = pop(); return; }
        if (op >= 0x40 && op <= 0x4F)
        {
            int n = op & 7;
            boolean c = cf;
            r[n] = alu(op < 0x48 ? 0 : 5, r[n], 1, true);
            cf = c;
            return;
        }
        if (op >= 0x50 && op <= 0x57) { push(op == 0x54 ? r[4] : r[op & 7]); return; }
        if (op >= 0x58 && op <= 0x5F) { r[op & 7] = pop(); return; }
        if (op >= 0xB0 && op <= 0xB7) { setReg8(op & 7, fetch8()); return; }
        if (op >= 0xB8 && op <= 0xBF) { r[op & 7] = fetch16(); return; }
        if (op >= 0x70 && op <= 0x7F)
        {
            int d = (byte) fetch8();
            if (cond(op & 0xF))
                ip = (ip + d) & 0xFFFF;
            return;
        }
        switch (op)
        {
        case 0x80: case 0x82: modrm(); { int v = alu(regField, getRM8(), fetch8(), false); if (regField != 7) setRM8(v); } return;
        case 0x81: modrm(); { int v = alu(regField, getRM16(), fetch16(), true); if (regField != 7) setRM16(v); } return;
        case 0x83: modrm(); { int v = alu(regField, getRM16(), ((byte) fetch8()) & 0xFFFF, true); if (regField != 7) setRM16(v); } return;
        case 0x84: modrm(); alu(4, getRM8(), reg8(regField), false); return;
        case 0x85: modrm(); alu(4, getRM16(), r[regField], true); return;
        case 0x86: modrm(); { int a = getRM8(); setRM8(reg8(regField)); setReg8(regField, a); } return;
        case 0x87: modrm(); { int a = getRM16(); setRM16(r[regField]); r[regField] = a; } return;
        case 0x88: modrm(); setRM8(reg8(regField)); return;
        case 0x89: modrm(); setRM16(r[regField]); return;
        case 0x8A: modrm(); setReg8(regField, getRM8()); return;
        case 0x8B: modrm(); r[regField] = getRM16(); return;
        case 0x8D: modrm(); r[regField] = ea; return;
        case 0x90: return;
        case 0x91: case 0x92: case 0x93: case 0x94: case 0x95: case 0x96: case 0x97:
        { int n = op & 7; int a = r[0]; r[0] = r[n]; r[n] = a; return; }
        case 0x98: r[0] = (byte) r[0] & 0xFFFF; return;
        case 0x99: r[2] = (r[0] & 0x8000) != 0 ? 0xFFFF : 0; return;
        case 0xA0: setReg8(0, rd8(seg(3), fetch16())); return;
        case 0xA1: r[0] = rd16(seg(3), fetch16()); return;
        case 0xA2: wr8(seg(3), fetch16(), r[0]); return;
        case 0xA3: wr16(seg(3), fetch16(), r[0]); return;
        case 0xA8: alu(4, r[0] & 0xFF, fetch8(), false); return;
        case 0xA9: alu(4, r[0], fetch16(), true); return;
        case 0xC3: ip = pop(); return;
        case 0xC6: modrm(); setRM8(fetch8()); return;
        case 0xC7: modrm(); setRM16(fetch16()); return;
        case 0xCB: ip = pop(); pop(); return;
        case 0xC0: modrm(); { int v = getRM8(); setRM8(shift(regField, v, fetch8(), false)); } return;
        case 0xC1: modrm(); { int v = getRM16(); setRM16(shift(regField, v, fetch8(), true)); } return;
        case 0xD0: modrm(); setRM8(shift(regField, getRM8(), 1, false)); return;
        case 0xD1: modrm(); setRM16(shift(regField, getRM16(), 1, true)); return;
        case 0xD2: modrm(); setRM8(shift(regField, getRM8(), r[1] & 0xFF, false)); return;
        case 0xD3: modrm(); setRM16(shift(regField, getRM16(), r[1] & 0xFF, true)); return;
        case 0xE2: { int d = (byte) fetch8(); r[1] = (r[1] - 1) & 0xFFFF; if (r[1] != 0) ip = (ip + d) & 0xFFFF; return; }
        case 0xE4: setReg8(0, in(fetch8())); return;
        case 0xE6: out(fetch8(), r[0]); return;
        case 0xE8: { int d = fetch16(); push(ip); ip = (ip + d) & 0xFFFF; return; }
        case 0xE9: { int d = fetch16(); ip = (ip + d) & 0xFFFF; return; }
        case 0xEB: { int d = (byte) fetch8(); ip = (ip + d) & 0xFFFF; return; }
        case 0xEC: setReg8(0, in(r[2])); return;
        case 0xED: r[0] = in(r[2]); return;
        case 0xEE: out(r[2], r[0] & 0xFF); return;
        case 0xF8: cf = false; return;
        case 0xF9: cf = true; return;
        case 0xFA: case 0xFB: case 0xFC: case 0xFD: return;           // cli sti cld std: nothing here depends on them
        case 0xF6: case 0xF7:
        {
            boolean word = op == 0xF7;
            modrm();
            int v = word ? getRM16() : getRM8();
            switch (regField)
            {
            case 0: alu(4, v, word ? fetch16() : fetch8(), word); return;
            case 2: if (word) setRM16(~v); else setRM8(~v); return;
            case 3: { int res = alu(5, 0, v, word); if (word) setRM16(res); else setRM8(res); return; }
            case 4:
                if (word)
                {
                    long p = (long) r[0] * v;
                    r[0] = (int) (p & 0xFFFF);
                    r[2] = (int) ((p >> 16) & 0xFFFF);
                    cf = of = r[2] != 0;
                }
                else
                {
                    int p = (r[0] & 0xFF) * v;
                    r[0] = p & 0xFFFF;
                    cf = of = (p & 0xFF00) != 0;
                }
                return;
            default: break;
            }
            break;
        }
        case 0xFE:
        {
            modrm();
            boolean c = cf;
            if (regField == 0) setRM8(alu(0, getRM8(), 1, false));
            else if (regField == 1) setRM8(alu(5, getRM8(), 1, false));
            else break;
            cf = c;
            return;
        }
        case 0xFF:
        {
            modrm();
            boolean c = cf;
            switch (regField)
            {
            case 0: setRM16(alu(0, getRM16(), 1, true)); cf = c; return;
            case 1: setRM16(alu(5, getRM16(), 1, true)); cf = c; return;
            case 2: { int t = getRM16(); push(ip); ip = t; return; }
            case 4: ip = getRM16(); return;
            case 6: push(getRM16()); return;
            default: break;
            }
            break;
        }
        default:
            break;
        }
        throw new IllegalStateException("the sound overlay used an instruction this player does not know: "
                                        + Integer.toHexString(op) + " at " + Integer.toHexString(start));
    }
}
