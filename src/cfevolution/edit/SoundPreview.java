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

import cfevolution.data.sound.Opl3;
import cfevolution.data.sound.SoundOverlay;
import java.util.Random;

/**
 * Hearing a sound effect as the game plays it (2026-11-19): the sound effects
 * file's own code (SoundOverlay) on an emulated AdLib chip (Nuked OPL3),
 * given what the game gives it, and called when the game's timer calls it.
 * A preview plays the file as it stands, unsaved edits included.
 *
 * What the game gives it (GP.EXE, IDA export): the engine note in word 00h,
 * written before every 50 Hz tick as the revs (500-14873) plus a random
 * 0-7Fh; a throttle state in word 10h (0Ch on, 0Bh off); for sliding tyres,
 * a slip value 8-13 in word 12h and 80h plus a random 0-0Fh in word 0Ah,
 * the squeal restarted one call in four; for a passing car, its speed held to
 * 1000h-3FFFh, divided by 64, plus a random 0-3Fh, in word 02h + 2 x voice;
 * word 0Eh 0 or 1 with the wheel gun. The timer runs at 300 Hz: function 1
 * every tick, function 6 every sixth.
 *
 * The randomness is the game's kind, not its exact numbers, and seeded, so a
 * preview is the same every time.
 */
public final class SoundPreview
{
    /** The previews' sample rate: 16-bit mono. */
    public static final int RATE = 44100;
    public static final String PLAY = "Play", STOP = "Stop";
    public static final Message NOT_PLAYED = Message.error("Play", "The sound could not be played on this computer.");

    private static final int TICK_RATE = 300, PER_TICK = RATE / TICK_RATE;
    /** Louder than the chip's own level, the same for every sound, so their loudness compares as in the game. */
    private static final int GAIN = 4;
    /** km/h to the game's speed units (MPH=(v*0x2ba)/0x10000, KPH=(v*0x464)/0x10000). */
    private static int speed(int kmh) { return kmh * 0x10000 / 0x464; }

    /** What the preview of page s (SoundEffectsPage.SOUNDS) plays, in a sentence. */
    public static String about(int s)
    {
        if (s < SoundEffectsPage.TYRES)
            return "The engine, all three tones together: revving up on the throttle, then easing off.";
        if (s == SoundEffectsPage.TYRES)
            return "Tyres sliding, more and more, then gripping again.";
        if (s < SoundEffectsPage.CRASH)
            return "A car passing at 250 km/h in this voice.";
        if (s == SoundEffectsPage.CRASH)
            return "The crash.";
        if (s == SoundEffectsPage.WHEEL_GUN)
            return "The wheel gun at a pit stop.";
        return "The start signal.";
    }

    /** Page s's sound, played from the file's bytes, as 16-bit samples at RATE. */
    public static short[] play(byte[] file, int s)
    {
        Run run = new Run(file);
        if (s < SoundEffectsPage.TYRES)
            run.engine();
        else if (s == SoundEffectsPage.TYRES)
            run.tyres();
        else if (s < SoundEffectsPage.CRASH)
            run.passing(s - SoundEffectsPage.PASSING);
        else if (s == SoundEffectsPage.CRASH)
            run.effect(4, 3.0);
        else if (s == SoundEffectsPage.WHEEL_GUN)
        {
            run.overlay.setWord(0x0E, 0);
            run.effect(3, 3.0);
        }
        else
            run.effect(2, 2.2);
        return run.samples();
    }

    /** As play, and how many accesses would have reached the game's own memory (for checks). */
    static int strays(byte[] file, int s)
    {
        Run run = new Run(file);
        if (s < SoundEffectsPage.TYRES) run.engine();
        else if (s == SoundEffectsPage.TYRES) run.tyres();
        else if (s < SoundEffectsPage.CRASH) run.passing(s - SoundEffectsPage.PASSING);
        else run.effect(s == SoundEffectsPage.CRASH ? 4 : s == SoundEffectsPage.WHEEL_GUN ? 3 : 2, 2.0);
        return run.overlay.strayAccesses();
    }

    /** The samples as a WAV file (for saving or for a player that wants one). */
    public static byte[] wav(short[] samples)
    {
        int data = samples.length * 2;
        byte[] w = new byte[44 + data];
        put(w, 0, "RIFF");
        le32(w, 4, 36 + data);
        put(w, 8, "WAVEfmt ");
        le32(w, 16, 16);
        le16(w, 20, 1);
        le16(w, 22, 1);
        le32(w, 24, RATE);
        le32(w, 28, RATE * 2);
        le16(w, 32, 2);
        le16(w, 34, 16);
        put(w, 36, "data");
        le32(w, 40, data);
        for (int i = 0; i < samples.length; i++)
            le16(w, 44 + 2 * i, samples[i]);
        return w;
    }

    private static void put(byte[] b, int at, String s) { for (int i = 0; i < 4; i++) b[at + i] = (byte) s.charAt(i); }
    private static void le16(byte[] b, int at, int v) { b[at] = (byte) v; b[at + 1] = (byte) (v >> 8); }
    private static void le32(byte[] b, int at, int v) { le16(b, at, v); le16(b, at + 2, v >> 16); }

    /** One preview: the chip, the overlay, the game's timer. */
    private static final class Run
    {
        final Opl3 chip = new Opl3(RATE);
        final SoundOverlay overlay;
        final Random random = new Random(1991);
        short[] out = new short[RATE * 8];
        int length, tick;
        int revs = 500;
        /** Called before each 50 Hz tick, as the game's frame code and timer set the inputs. */
        Runnable frame;
        final int[] buf = new int[2];

        Run(byte[] file)
        {
            overlay = new SoundOverlay(file, chip);
            overlay.call(SoundOverlay.INIT, 0);
        }

        /** The game's timer for so many seconds. */
        void run(double seconds)
        {
            int ticks = (int) Math.round(seconds * TICK_RATE);
            for (int t = 0; t < ticks; t++)
            {
                overlay.call(SoundOverlay.TICK_300, 0);
                if (++tick % 6 == 0)
                {
                    if (frame != null)
                        frame.run();
                    overlay.call(SoundOverlay.TICK_50, 0);
                }
                for (int i = 0; i < PER_TICK; i++)
                {
                    chip.generateResampled(buf);
                    if (length == out.length)
                        out = java.util.Arrays.copyOf(out, out.length * 2);
                    int v = buf[0] * GAIN;
                    out[length++] = (short) Math.max(-32768, Math.min(32767, v));
                }
            }
        }

        short[] samples()
        {
            return java.util.Arrays.copyOf(out, length);
        }

        /** The engine note as the timer writes it: the revs plus a random 0-7Fh, at least 500. */
        void engineNote()
        {
            overlay.setWord(0x00, Math.max(500, revs + (random.nextInt(0x80))));
        }

        void engine()
        {
            overlay.setWord(0x10, 0x0C);            // on the throttle
            revs = 2000;
            overlay.setWord(0x00, revs);
            overlay.call(SoundOverlay.START, 0);
            frame = new Runnable() { public void run() { engineNote(); } };
            run(0.6);                                // ticking over
            for (int i = 0; i < 125; i++)            // up to the limit in 2.5 s
            {
                revs = 2000 + (14873 - 2000) * i / 125;
                run(0.02);
            }
            revs = 14873;
            run(0.6);
            overlay.setWord(0x10, 0x0B);            // off the throttle
            for (int i = 0; i < 100; i++)            // back down in 2 s
            {
                revs = 14873 - (14873 - 2500) * i / 100;
                run(0.02);
            }
            revs = 2500;
            run(0.5);
            overlay.call(SoundOverlay.STOP, 5);
            run(0.3);
        }

        void tyres()
        {
            final int[] slip = { 8 };
            frame = new Runnable() {
                public void run()
                {
                    overlay.setWord(0x12, slip[0]);
                    overlay.setWord(0x0A, 0x80 + random.nextInt(0x10));
                    if (random.nextInt(4) == 0)
                        overlay.call(SoundOverlay.START, 1);
                }
            };
            for (slip[0] = 8; slip[0] <= 13; slip[0]++)
                run(0.4);
            frame = null;
            overlay.call(SoundOverlay.STOP, 1);
            run(0.5);
        }

        void passing(int voice)
        {
            int v = Math.max(0x1000, Math.min(0x3FFF, speed(250)));
            overlay.setWord(0x02 + 2 * voice, (v >> 6) + random.nextInt(0x40));
            overlay.call(SoundOverlay.START, 6 + voice);
            run(2.5);
        }

        void effect(int effect, double seconds)
        {
            overlay.call(SoundOverlay.START, effect);
            run(seconds);
        }
    }
}
