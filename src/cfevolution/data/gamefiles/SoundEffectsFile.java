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
package cfevolution.data.gamefiles;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * The AdLib (and Sound Blaster) sound effects, ASOUND.BIN, which the game's
 * installer copies to XSOUND.BIN: program code that drives the OPL2 chip,
 * and the settings it reads, then the two checksums. Only the settings are
 * edited, in place, so the file keeps its size (F1GP-SDL's port accepts this
 * file only at its original size). Layout read from the code here and by the
 * F1GP-SDL project, 2026-10-25.
 *
 * The 13 instruments are 24-byte records from 94h: modulator and carrier
 * values for registers 20h, 60h, 80h and E0h, the modulator's 40h stored as
 * a volume (XOR 3Fh), the carrier's level coming from the code, C0h stored
 * with its connection bit inverted, and for the crash a random-pitch noise
 * (duration, mask, base). Pitches are linear values the code turns into a
 * block and F-number by halving.
 *
 * @author barrie
 */
public class SoundEffectsFile extends GameFile
{
    public static final int SIZE = 3121;

    /** The code's entry points at 24h; checked so another file of this size is not taken for it. */
    private static final int[] ENTRY_POINTS = { 0x057F, 0x0258, 0x0353, 0x0532, 0x0556, 0x05C5 };
    private static final int ENTRY_TABLE = 0x24;

    public static final int RECORDS = 13, RECORD_SIZE = 0x18;
    private static final int RECORD_TABLE = 0x94;

    /** In a record: the two operators' 20h, 60h, 80h and E0h values (modulator first). In 20h: 80h tremolo,
        40h vibrato, 20h hold the note, 10h shorter at high notes, 0Fh frequency multiple. In 60h: attack in
        the top four bits, decay below. In 80h: sustain level (0 loudest) on top, release below. */
    public static final int CHARACTER = 0x0, ENVELOPE = 0x4, HOLD_RELEASE = 0x6, WAVEFORM = 0x8;
    /** In a record: the modulator's 40h as stored (low six bits a volume), and C0h as stored. */
    public static final int MODULATOR_LEVEL = 0x2, FEEDBACK = 0xA;
    /** In a record: the random-pitch noise's duration (fiftieths of a second), mask and base. */
    public static final int NOISE_LENGTH = 0x10, NOISE_SPREAD = 0x12, NOISE_BASE = 0x14;

    /** Engine: the three tones' pitch against the revs, roughness, and loudness off and on the throttle. */
    public static final int[] ENGINE_PITCH = { 0x34, 0x36, 0x38 }, ENGINE_ROUGHNESS = { 0x52, 0x54, 0x56 },
                              ENGINE_LEVEL_OFF = { 0x3A, 0x3C, 0x3E }, ENGINE_LEVEL_ON = { 0x40, 0x42, 0x44 };
    /** Tyres: base pitch, and a loudness byte per amount of sliding (table at 58h, entries 0 and 8-13 used). */
    public static final int TYRE_PITCH = 0x6C;
    public static final int[] TYRE_LEVEL = { 0x58, 0x60, 0x61, 0x62, 0x63, 0x64, 0x65 };
    /** Passing cars: each voice's base pitch and greatest drop; the shared drop delay and rate (bytes). */
    public static final int[] PASSING_PITCH = { 0x6E, 0x70, 0x72, 0x74 }, PASSING_MAX_DROP = { 0x8C, 0x8D, 0x8E, 0x8F };
    public static final int PASSING_DROP_DELAY = 0x76, PASSING_DROP_RATE = 0x77;

    /** Which instrument each effect uses. Records 0 and 6 are used by nothing. */
    public static final int[] ENGINE_RECORDS = { 3, 8, 9 }, PASSING_RECORDS = { 10, 11, 12, 1 };
    public static final int TYRE_RECORD = 2, START_RECORD = 4, CRASH_RECORD = 5, WHEEL_GUN_RECORD = 7;

    private SoundEffectsFile(byte[] data)
    {
        this.data = data;
    }

    public static boolean isSoundEffects(byte[] d)
    {
        if (d.length != SIZE || !checksumOk(d))
            return false;
        for (int i = 0; i < ENTRY_POINTS.length; i++)
            if (word(d, ENTRY_TABLE + 2 * i) != ENTRY_POINTS[i])
                return false;
        return true;
    }

    public static boolean isSoundEffects(File file)
    {
        try
        {
            return file.length() == SIZE && isSoundEffects(Files.readAllBytes(file.toPath()));
        }
        catch (IOException e)
        {
            return false;
        }
    }

    public static SoundEffectsFile load(File file) throws IOException
    {
        byte[] d = Files.readAllBytes(file.toPath());
        if (!isSoundEffects(d))
            throw new IOException("not the AdLib sound effects");
        return new SoundEffectsFile(d);
    }

    /** Where record k starts. */
    public static int record(int k)
    {
        return RECORD_TABLE + RECORD_SIZE * k;
    }

    /** Some bits of a byte, shifted down. */
    public int getBits(int offset, int mask)
    {
        return (getByte(offset) & mask) >> Integer.numberOfTrailingZeros(mask);
    }

    /** Sets some bits of a byte, leaving the others as stored. */
    public void setBits(int offset, int mask, int value)
    {
        setByte(offset, (getByte(offset) & ~mask) | ((value << Integer.numberOfTrailingZeros(mask)) & mask));
    }
}
