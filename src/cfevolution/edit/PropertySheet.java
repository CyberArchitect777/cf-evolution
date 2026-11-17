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

import cfevolution.data.track.CCLineSegment;
import cfevolution.data.track.Command;
import cfevolution.data.track.TrackDataHeader;
import cfevolution.data.track.TrackSegment;

/**
 * The property table of the item picked in a track's list (2026-11-17, from
 * the desktop's HeaderTableModel, SegmentTableModel, BestLineTableModel and
 * CmdParamTableModel, word for word): the track data headers, a track or pit
 * segment, a best line segment, or one command's parameters. Each row is a
 * typed number, a tick box or a choice; nothing reaches the track until
 * apply(). Changing a choice rebuilds the rows as the desktop table does.
 */
public final class PropertySheet
{
    /** What a row holds. */
    public static final int NUMBER = 0, FLAG = 1, CHOICE = 2;

    /** What the sheet edits. */
    public static final int HEADERS = 0, SEGMENT = 1, LINE = 2, COMMAND = 3;

    public static final String[] KERB_TYPES = { "Single Colour (Excluding White)", "Dual Colour" };
    public static final String[] LINE_TYPES = { "Normal", "Wider Radius", "Displacement" };

    static final String[] FLAG_NAMES = {
        "Unknown 1 (0x8000)", "Unknown 2 (0x4000)", "No Left Wall", "No Right Wall", "Left Kerb", "Right Kerb",
        "Unknown 3 (0x0200)", "Unknown 4 (0x0100)", "Unknown 5 (0x0080)", "Unknown 6 (0x0040)",
        "Left Non-Parallel Wall", "Right Non-Parallel Wall", "Unknown 7 (0x0008)", "Kerb Type",
        "Left Pit Wall", "Right Pit Wall" };

    public static final String LINE_LENGTH_TITLE = "Best Line Length";

    public final int what;
    private final TrackDataHeader header;
    private final TrackSegment segment;
    private final CCLineSegment line;
    private final Command command;
    private final String[] paramText;

    private String[] labels;
    private int[] kinds;
    private Object[] values;          // String (number), Boolean (tick box) or Integer (choice)
    private boolean lineChanged;

    private PropertySheet(int what, TrackDataHeader header, TrackSegment segment, CCLineSegment line,
                          Command command, String[] paramText)
    {
        this.what = what;
        this.header = header;
        this.segment = segment;
        this.line = line;
        this.command = command;
        this.paramText = paramText;
    }

    public static PropertySheet headers(TrackDataHeader h)
    {
        PropertySheet s = new PropertySheet(HEADERS, h, null, null, null, null);
        s.headerRows(h.getTotalKerbColours() == 4 ? 1 : 0);
        return s;
    }

    public static PropertySheet segment(TrackSegment t)
    {
        PropertySheet s = new PropertySheet(SEGMENT, null, t, null, null, null);
        s.size(21);
        s.number(0, "Segment Length", t.getTlu());
        s.number(1, "Curvature", t.getCurvature());
        s.number(2, "Height Difference", t.getHeightChange());
        s.number(3, "Left Run-Off Area", t.getFenceDistL());
        s.number(4, "Right Run-Off Area", t.getFenceDistR());
        String bits = t.getDetailedFlags();
        for (int x = 0; x < 16; x++)
            s.flag(5 + x, FLAG_NAMES[x], bits.charAt(x) == '1');
        return s;
    }

    public static PropertySheet bestLine(CCLineSegment l)
    {
        PropertySheet s = new PropertySheet(LINE, null, null, l, null, null);
        // A type the list does not name shows as the first choice, as a new list box does
        s.lineRows(l.getType() == 64 ? 1 : l.getType() == 128 ? 2 : 0);
        return s;
    }

    /** One command's parameters, named by paramText (CommandData.getParamArray()). */
    public static PropertySheet command(Command c, String[] paramText)
    {
        PropertySheet s = new PropertySheet(COMMAND, null, null, null, c, paramText.clone());
        s.size(paramText.length);
        for (int x = 0; x < paramText.length; x++)
            s.number(x, paramText[x], c.getParam(x));
        return s;
    }

    // --- rows ---------------------------------------------------------------

    private void size(int n)
    {
        labels = new String[n];
        kinds = new int[n];
        values = new Object[n];
    }

    private void number(int r, String label, int v)
    {
        labels[r] = label;
        kinds[r] = NUMBER;
        values[r] = Integer.toString(v);
    }

    private void flag(int r, String label, boolean v)
    {
        labels[r] = label;
        kinds[r] = FLAG;
        values[r] = Boolean.valueOf(v);
    }

    private void choice(int r, String label, int index)
    {
        labels[r] = label;
        kinds[r] = CHOICE;
        values[r] = Integer.valueOf(index);
    }

    private void headerRows(int kerbType)
    {
        TrackDataHeader h = header;
        size(kerbType == 0 ? 16 : 18);
        number(0, "Starting Angle of First Segment", h.getStartAngle());
        number(1, "Starting Height of First Segment", h.getStartHeight());
        number(2, "X Coordinates Of Track Center At Start/Finish Line", h.getStartPos(0));
        number(3, "Y Coordinates Of Track Center At Start/Finish Line", h.getStartPos(1));
        number(4, "Z Coordinates Of Track Center At Start/Finish Line", h.getStartPos(2));
        number(5, "Starting Width of First Segment", h.getStartWidth());
        number(6, "Pole Width (Unknown Setting)", h.getPoleWidth());
        flag(7, "Pits Located on Left of Track?", h.getPitSide());
        number(8, "TrSurround (Unknown Setting)", h.getTrSurround());
        number(9, "Starting Distance of Left Fence", h.getFenceDistL());
        number(10, "Starting Distance of Rignt Fence", h.getFenceDistR());
        number(11, "Unknown 1", h.getUnknown1());
        number(12, "Unknown 2", h.getUnknown2());
        choice(13, "Circuit Kerb Type", kerbType);
        if (kerbType == 0)
        {
            number(14, "Upper Kerb Colour", h.getKerbColor(true, true));
            number(15, "Lower Kerb Colour", h.getKerbColor(false, true));
        }
        else
        {
            number(14, "First Upper Kerb Colour", h.getKerbColor(true, true));
            number(15, "First Lower Kerb Colour", h.getKerbColor(false, true));
            number(16, "Second Upper Kerb Colour", h.getKerbColor(true, false));
            number(17, "Second Lower Kerb Colour", h.getKerbColor(false, false));
        }
    }

    private void lineRows(int type)
    {
        CCLineSegment l = line;
        size(type == 0 ? 4 : 5);
        choice(0, "Best Line Type", type);
        number(1, "Best Line Length", l.getTlu());
        if (type == 0 || type == 1)
        {
            number(2, "Best Line Correction", l.getParam(0));
            if (type == 0)
                number(3, "Best Line Radius", l.getParam(1));
            else
            {
                number(3, "Best Line High Radius", l.getParam(1));
                number(4, "Best Line Low Radius", l.getParam(2));
            }
        }
        else
        {
            number(2, "Best Line Displacement", l.getParam(0));
            number(3, "Best Line Correction", l.getParam(1));
            number(4, "Best Line Radius", l.getParam(2));
        }
    }

    public int rows()
    {
        return labels.length;
    }

    public String label(int r)
    {
        return labels[r];
    }

    public int kind(int r)
    {
        return kinds[r];
    }

    /** A choice row's options. */
    public String[] options(int r)
    {
        return what == HEADERS ? KERB_TYPES.clone() : LINE_TYPES.clone();
    }

    /** What a number row shows (as typed, until applied). */
    public String text(int r)
    {
        return (String) values[r];
    }

    public boolean ticked(int r)
    {
        return ((Boolean) values[r]).booleanValue();
    }

    public int chosen(int r)
    {
        return ((Integer) values[r]).intValue();
    }

    /** The row as the desktop table shows it: the number, true/false, or the choice. */
    public String shown(int r)
    {
        return kinds[r] == CHOICE ? options(r)[chosen(r)] : String.valueOf(values[r]);
    }

    public void type(int r, String text)
    {
        values[r] = text;
    }

    public void tick(int r, boolean on)
    {
        values[r] = Boolean.valueOf(on);
    }

    /**
     * Picks a choice. The kerb type rebuilds the headers keeping what was
     * typed but not applied; the best line type rebuilds the rows from the
     * segment as it stands, as the desktop does.
     */
    public void choose(int r, int index)
    {
        if (what == HEADERS)
        {
            Object[] typed = values;
            headerRows(index);
            for (int row = 0; row < Math.min(typed.length, values.length); row++)
                if (row != 13 && typed[row] != null && values[row] != null && typed[row].getClass() == values[row].getClass())
                    values[row] = typed[row];
        }
        else
            lineRows(index);
    }

    /** Under a best line segment: the radii that are always safe for the typed length. */
    public String note()
    {
        if (what != LINE)
            return null;
        try
        {
            int tlu = Integer.parseInt(text(1).trim());
            return "Always-safe radius:  0  or  |r| > " + (tlu * 128) + "  (smaller can be valid on curved track)";
        }
        catch (NumberFormatException e)
        {
            return "Always-safe radius:  —";
        }
    }

    // --- applying -----------------------------------------------------------

    private int num(int r)
    {
        return Integer.parseInt(text(r).trim());
    }

    /** Why the typed values cannot be applied (a number row that is not a whole number), else null. */
    public Message problem()
    {
        for (int r = 0; r < rows(); r++)
        {
            if (kinds[r] != NUMBER)
                continue;
            try
            {
                num(r);
            }
            catch (NumberFormatException e)
            {
                String name = what == COMMAND ? "Parameter " + (r + 1) + " (" + labels[r] + ")" : labels[r];
                return Message.warning(ExecutableEdits.NOT_CHANGED, name + " must be a whole number.");
            }
        }
        return null;
    }

    /**
     * Writes the rows into the track (check problem() first). Returns the
     * warning the desktop shows when a best line length was out of range and
     * has been changed, else null.
     */
    public Message apply()
    {
        Message notice = null;
        switch (what)
        {
            case HEADERS:
            {
                TrackDataHeader h = header;
                h.setStartAngle(num(0));
                h.setStartHeight(num(1));
                h.setStartPos(0, num(2));
                h.setStartPos(1, num(3));
                h.setStartPos(2, num(4));
                h.setStartWidth(num(5));
                h.setPoleWidth(num(6));
                h.setPitSide(ticked(7));
                h.setTrSurround(num(8));
                h.setFenceDistL(num(9));
                h.setFenceDistR(num(10));
                h.setUnknown1(num(11));
                h.setUnknown2(num(12));
                // Most circuits store three kerb colour entries, which neither choice
                // describes. Only change the count when the kerb type itself was
                // changed, so applying leaves an untouched list as it was.
                boolean wasDual = h.getTotalKerbColours() == 4;
                boolean dual = chosen(13) == 1;
                if (dual != wasDual)
                    h.setTotalKerbColours(dual ? 4 : 2);
                h.setKerbColor(true, true, num(14));
                h.setKerbColor(false, true, num(15));
                if (dual)
                {
                    h.setKerbColor(true, false, num(16));
                    h.setKerbColor(false, false, num(17));
                }
                break;
            }
            case SEGMENT:
            {
                TrackSegment t = segment;
                t.setTlu(num(0));
                t.setCurvature(num(1));
                t.setHeightChange(num(2));
                t.setFenceDistL(num(3));
                t.setFenceDistR(num(4));
                StringBuilder bits = new StringBuilder();
                for (int x = 0; x < 16; x++)
                    bits.append(ticked(5 + x) ? '1' : '0');
                t.setDetailedFlags(bits.toString());
                break;
            }
            case LINE:
            {
                CCLineSegment l = line;
                int[] before = { l.getType(), l.getTlu(), l.getParam(0), l.getParam(1), l.getParam(2), l.getParam(3) };
                int type = chosen(0);
                l.setType(type == 1 ? 64 : type == 2 ? 128 : 0);
                // The file stores the length as a single byte; save() would silently
                // truncate anything larger, so keep the value within 1-255 here.
                int length = num(1);
                if (length < 1 || length > 255)
                {
                    int clamped = Math.min(255, Math.max(1, length));
                    notice = Message.warning(LINE_LENGTH_TITLE,
                        "Best line segment length must be between 1 and 255\n"
                        + "(the track file stores the length as a single byte).\n"
                        + "The value " + length + " has been changed to " + clamped + ".");
                    type(1, String.valueOf(clamped));
                    length = clamped;
                }
                l.setTlu(length);
                l.setParam(0, num(2));
                l.setParam(1, num(3));
                if (type != 0)
                    l.setParam(2, num(4));
                lineChanged = l.getType() != before[0] || l.getTlu() != before[1] || l.getParam(0) != before[2]
                    || l.getParam(1) != before[3] || l.getParam(2) != before[4] || l.getParam(3) != before[5];
                break;
            }
            default:
                for (int x = 0; x < paramText.length; x++)
                    command.setParam(x, num(x));
        }
        return notice;
    }

    /** After apply() on a best line segment: whether it changed (Repair Best Line After Edit uses it). */
    public boolean lineChanged()
    {
        return lineChanged;
    }

    /** The best line segment this sheet edits, else null. */
    public CCLineSegment lineSegment()
    {
        return line;
    }
}
