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

package cfevolution.data.executable;

import java.io.*;

/**
 * The executable's larger data tables that no other editor offers (added
 * 2026-10-24 from the F1GP-SDL reference, every one read back in all three
 * unpacked builds first): the pit lane team order, the default car setups,
 * the default lap records, the car physics tables and the opposition level
 * tables.
 *
 * Every table is kept as the raw bytes read and written back whole, so a
 * table the user does not touch saves byte-identically. All are data, so
 * they sit 64 bytes earlier in the Italian build and 48 in the US build.
 *
 * Two areas, both European file offsets:
 *  - "ss" data (seg004): file = 173520 + ss offset. Pit order ss:28D8h; car
 *    setups ss:1286h (40 x 10); per-circuit setups ss:1418h (16 x 10, then
 *    16 x 10 more used when race and qualifying setups are separate, as the
 *    word before them, ss:1416h, says); lap
 *    records ss:2312h (16 x 90: circuit word, then qualifying and race, each a
 *    24-byte driver, 14-byte team, millisecond time and a day count from
 *    1 January 1978).
 *  - seg003 data: file = IDA address + 5264. The car and opposition tables.
 *
 * @author barrie
 */
public class GameTables
{
    private static final int SS_BASE = 173520;
    private static final int IDA_TO_FILE = 5264;

    /** A raw table: where it is (European file offset) and its bytes. */
    public static class Table
    {
        public final int offset, length;
        byte[] data;
        byte[] original;

        Table(int offset, int length)
        {
            this.offset = offset;
            this.length = length;
        }

        public int word(int i) { return (data[i * 2] & 0xFF) | ((data[i * 2 + 1] & 0xFF) << 8); }
        public void setWord(int i, int v) { data[i * 2] = (byte) v; data[i * 2 + 1] = (byte) (v >> 8); }
        public int originalWord(int i) { return (original[i * 2] & 0xFF) | ((original[i * 2 + 1] & 0xFF) << 8); }
        public int getByte(int i) { return data[i] & 0xFF; }
        public void setByte(int i, int v) { data[i] = (byte) v; }
        public int originalByte(int i) { return original[i] & 0xFF; }
        /** Takes every value from another copy of the same table (Revert Page, 2026-11-23). */
        public void copyFrom(Table t) { data = t.data.clone(); }
        public int words() { return length / 2; }
        /** Swaps two blocks of size bytes, block a and block b (a car's setup when a driver moves, 2026-11-23). */
        public void swapBlocks(int a, int b, int size)
        {
            for (int i = 0; i < size; i++)
            {
                byte t = data[a * size + i];
                data[a * size + i] = data[b * size + i];
                data[b * size + i] = t;
            }
        }

    }

    private static Table ss(int ssOffset, int length) { return new Table(SS_BASE + ssOffset, length); }
    private static Table ida(int address, int words) { return new Table(address + IDA_TO_FILE, words * 2); }

    // Pit lane (abPitPosTeamNrTable): 20 BYTES, entry n is the team number (1-based) whose cars use
    // pit box n, box 0 at the pit lane entrance. The game places wNumTeams = 18 teams, so only entries
    // 0-17 are boxes; 18 and 19 (19, 20 as shipped) are kept as they are. Read by the garage set-up
    // (sub_47E9) and by a car joining practice or qualifying (InitCarPitPos); a team missing from the
    // table gets no box and a team listed twice is handled inconsistently, so the editor keeps 0-17 to
    // each of 1-18 once. A team with no cars leaves its box empty. Specification and in-game test from
    // F1GP-SDL, 2026-11-15; offsets checked in all twelve executables in EXE versions/.
    public final Table pitOrder = ss(0x28D8, 20);
    public static final int PIT_BOXES = 18;
    /** The shipped order, the same in all three builds. */
    public static final int[] PIT_ORDER_1991 = { 1, 14, 10, 3, 2, 15, 8, 6, 4, 5, 7, 9, 11, 12, 13, 16, 17, 18 };

    // Setups: front wing, rear wing, six gears, tyre compound (0 = A), brake balance
    public static final int SETUP_SIZE = 10;
    public final Table carSetups = ss(0x1286, 40 * SETUP_SIZE);
    public final Table circuitSetups = ss(0x1418, 32 * SETUP_SIZE);
    // Separate race and qualifying setups, the game's starting choice: FFFFh on, 0 off (ss:1416h; the
    // game's options change it while it runs, and a season setup file loaded at start-up replaces it)
    public final Table separateSetups = ss(0x1416, 2);

    // Lap records
    public static final int RECORD_SIZE = 90, SESSION_SIZE = 44, NAME_LENGTH = 24, TEAM_LENGTH = 14;
    public final Table lapRecords = ss(0x2312, 16 * RECORD_SIZE);

    // The car (seg003)
    public final Table torqueRace = ida(0x212B7, 33);
    public final Table torqueQualifying = ida(0x212F9, 33);
    public final Table tyreGripDry = ida(0x20FAB, 48);
    public final Table tyreGripWet = ida(0x2100B, 48);
    public final Table minimumGrip = ida(0x2106F, 2);      // dry, wet
    public final Table carWeight = ida(0x2107F, 2);        // two copies, kept equal
    public final Table fuel = ida(0x21083, 2);             // outside a race, starting load
    public final Table wingDrag = ida(0x211B5, 9);
    public final Table wingDownforce = ida(0x21219, 9);
    public final Table downforceFactor = ida(0x2122B, 1);
    public final Table engineBraking = ida(0x21231, 1);
    public final Table revLimits = ida(0x2124D, 10);       // up-shift, down-shift target, then per-gear auto down-shift

    // Opposition levels (Rookie .. Ace), seg003
    public final Table oppPowerDeduction = ida(0x215FD, 5);
    public final Table oppRankPower = ida(0x21607, 5);
    public final Table oppOversteerLow = ida(0x21611, 5);
    public final Table oppOversteerHigh = ida(0x2161D, 1);
    public final Table oppCornerBurst = ida(0x21623, 5);
    public final Table oppVariationLow = ida(0x2162D, 5);
    public final Table oppVariationHigh = ida(0x21637, 5);
    public final Table globalCornerSpeed = ida(0x21647, 1);
    public final Table oppWingVariation = ida(0x217A7, 5);
    public final Table wetRacePower = ida(0x2135B, 1);

    private final Table[] all = { pitOrder, carSetups, circuitSetups, separateSetups, lapRecords, torqueRace, torqueQualifying,
        tyreGripDry, tyreGripWet, minimumGrip, carWeight, fuel, wingDrag, wingDownforce, downforceFactor,
        engineBraking, revLimits, oppPowerDeduction, oppRankPower, oppOversteerLow, oppOversteerHigh,
        oppCornerBurst, oppVariationLow, oppVariationHigh, globalCornerSpeed, oppWingVariation, wetRacePower };

    public GameTables()
    {
    }

    // --- Pit lane order ----------------------------------------------------

    /** The team number using a pit box (0 = the box at the pit lane entrance). */
    public int getPitBoxTeam(int box) { return pitOrder.getByte(box); }

    /** True if the boxes hold each team 1-18 exactly once. */
    public static boolean isValidPitOrder(int[] teams)
    {
        if (teams == null || teams.length != PIT_BOXES)
            return false;
        boolean[] seen = new boolean[PIT_BOXES + 1];
        for (int t : teams)
        {
            if (t < 1 || t > PIT_BOXES || seen[t])
                return false;
            seen[t] = true;
        }
        return true;
    }

    public int[] getPitOrder()
    {
        int[] teams = new int[PIT_BOXES];
        for (int i = 0; i < PIT_BOXES; i++) teams[i] = pitOrder.getByte(i);
        return teams;
    }

    public boolean isPitOrderValid() { return isValidPitOrder(getPitOrder()); }

    /** True if the boxes differ from what was loaded. */
    public boolean isPitOrderChanged()
    {
        for (int i = 0; i < PIT_BOXES; i++)
            if (pitOrder.getByte(i) != pitOrder.originalByte(i)) return true;
        return false;
    }

    /** Sets the boxes (entries 18 and 19 are left alone). Returns false, changing nothing, unless the
        order holds each team 1-18 exactly once. */
    public boolean setPitOrder(int[] teams)
    {
        if (!isValidPitOrder(teams))
            return false;
        for (int i = 0; i < PIT_BOXES; i++) pitOrder.setByte(i, teams[i]);
        return true;
    }

    /** Puts a team in a box. If the team already has another box, the two swap, so a valid order
        stays valid; otherwise (only in an order that was invalid when loaded) the box is just set. */
    public void setPitBoxTeam(int box, int team)
    {
        int was = pitOrder.getByte(box);
        if (team == was)
            return;
        for (int i = 0; i < PIT_BOXES; i++)
        {
            if (i != box && pitOrder.getByte(i) == team)
            {
                pitOrder.setByte(i, was);
                break;
            }
        }
        pitOrder.setByte(box, team);
    }

    public void swapPitBoxes(int a, int b)
    {
        int t = pitOrder.getByte(a);
        pitOrder.setByte(a, pitOrder.getByte(b));
        pitOrder.setByte(b, t);
    }

    public void resetPitOrder() { setPitOrder(PIT_ORDER_1991); }

    // --- Lap records --------------------------------------------------------

    private int sessionStart(int circuit, boolean race)
    {
        return circuit * RECORD_SIZE + 2 + (race ? SESSION_SIZE : 0);
    }

    private String text(int at, int length)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length && lapRecords.data[at + i] != 0; i++)
            sb.append((char) (lapRecords.data[at + i] & 0xFF));
        return sb.toString();
    }

    private void setText(int at, int length, String value)
    {
        for (int i = 0; i < length; i++)
            lapRecords.data[at + i] = (byte) (i < value.length() && i < length - 1 ? value.charAt(i) : 0);
    }

    public String getRecordDriver(int circuit, boolean race) { return text(sessionStart(circuit, race), NAME_LENGTH); }
    public String getRecordTeam(int circuit, boolean race) { return text(sessionStart(circuit, race) + NAME_LENGTH, TEAM_LENGTH); }

    public void setRecordDriver(int circuit, boolean race, String name)
    {
        if (!name.equals(getRecordDriver(circuit, race)))
            setText(sessionStart(circuit, race), NAME_LENGTH, name);
    }

    public void setRecordTeam(int circuit, boolean race, String team)
    {
        if (!team.equals(getRecordTeam(circuit, race)))
            setText(sessionStart(circuit, race) + NAME_LENGTH, TEAM_LENGTH, team);
    }

    /** Lap time in milliseconds. */
    public int getRecordTime(int circuit, boolean race)
    {
        int at = sessionStart(circuit, race) + NAME_LENGTH + TEAM_LENGTH;
        byte[] d = lapRecords.data;
        return (d[at] & 0xFF) | ((d[at + 1] & 0xFF) << 8) | ((d[at + 2] & 0xFF) << 16) | ((d[at + 3] & 0xFF) << 24);
    }

    public void setRecordTime(int circuit, boolean race, int ms)
    {
        int at = sessionStart(circuit, race) + NAME_LENGTH + TEAM_LENGTH;
        for (int i = 0; i < 4; i++)
            lapRecords.data[at + i] = (byte) (ms >> (8 * i));
    }

    /** Date as days since 1 January 1978. */
    public int getRecordDay(int circuit, boolean race)
    {
        int at = sessionStart(circuit, race) + NAME_LENGTH + TEAM_LENGTH + 4;
        return (lapRecords.data[at] & 0xFF) | ((lapRecords.data[at + 1] & 0xFF) << 8);
    }

    public void setRecordDay(int circuit, boolean race, int day)
    {
        int at = sessionStart(circuit, race) + NAME_LENGTH + TEAM_LENGTH + 4;
        lapRecords.data[at] = (byte) day;
        lapRecords.data[at + 1] = (byte) (day >> 8);
    }

    // The game's calendar: day 0 is 1 January 1978, but the game has no
    // 29 February 2000 (it treats every century year as a common year), so
    // from March 2000 it shows dates one day later than the real calendar.
    // Seen in the game 2026-10-24: a day count for 24 Oct 2026 shows 25.10.2026.
    private static final java.time.LocalDate DAY_ZERO = java.time.LocalDate.of(1978, 1, 1);
    private static final java.time.LocalDate LEAP_2000 = java.time.LocalDate.of(2000, 2, 29);

    /** The date the game shows for a stored day count. */
    public static java.time.LocalDate dateOfDay(int day)
    {
        java.time.LocalDate real = DAY_ZERO.plusDays(day);
        return real.isBefore(LEAP_2000) ? real : real.plusDays(1);
    }

    /** The day count that makes the game show this date, or -1 if it cannot. */
    public static int dayOfDate(java.time.LocalDate date)
    {
        if (date.equals(LEAP_2000))
            return -1;
        long day = java.time.temporal.ChronoUnit.DAYS.between(DAY_ZERO, date);
        if (date.isAfter(LEAP_2000))
            day--;
        return day < 0 || day > 0xFFFF ? -1 : (int) day;
    }

    // --- Load and save ------------------------------------------------------

    public int loadData(RandomAccessFile binaryAccess, int gameVersion)
    {
        for (Table t : all)
        {
            t.data = BinaryManager.getDataBytes(gameVersion, binaryAccess, t.offset, t.offset - 64, t.offset - 48, t.length);
            if (t.data == null || t.data.length != t.length)
                return 1;
            t.original = t.data.clone();
        }
        return 0;
    }

    public int saveData(RandomAccessFile binaryAccess, int gameVersion)
    {
        for (Table t : all)
            BinaryManager.setDataBytes(gameVersion, binaryAccess, t.offset, t.offset - 64, t.offset - 48, t.data);
        return 0;
    }
}
