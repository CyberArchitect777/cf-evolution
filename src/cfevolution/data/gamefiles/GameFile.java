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
 * A small game file kept raw: its bytes are read whole, edited in place and
 * written back with the game's two checksums in the last four bytes (a byte
 * sum, then a rotate-left-3-and-add sum, both 16-bit). Bytes the editor does
 * not offer are never changed.
 *
 * @author barrie
 */
public abstract class GameFile
{
    protected byte[] data;

    /** True if the last four bytes hold the checksums of the rest. */
    public static boolean checksumOk(byte[] d)
    {
        if (d.length < 4)
            return false;
        int[] sums = checksums(d);
        return sums[0] == word(d, d.length - 4) && sums[1] == word(d, d.length - 2);
    }

    private static int[] checksums(byte[] d)
    {
        int sum = 0, rotated = 0;
        for (int i = 0; i < d.length - 4; i++)
        {
            int c = d[i] & 0xFF;
            sum += c;
            rotated = ((rotated << 3) & 0xFFF8) + ((rotated >> 13) & 0x07) + c;
        }
        return new int[] { sum & 0xFFFF, rotated & 0xFFFF };
    }

    /** Reads a file of the given size whose checksums are right. */
    protected static byte[] read(File file, int size) throws IOException
    {
        byte[] d = Files.readAllBytes(file.toPath());
        if (d.length != size || !checksumOk(d))
            throw new IOException("not the expected kind of file");
        return d;
    }

    public void save(File file) throws IOException
    {
        int[] sums = checksums(data);
        setWord(data.length - 4, sums[0]);
        setWord(data.length - 2, sums[1]);
        Files.write(file.toPath(), data);
    }

    public byte[] getBytes()
    {
        return data.clone();
    }

    protected static int word(byte[] d, int o)
    {
        return (d[o] & 0xFF) | ((d[o + 1] & 0xFF) << 8);
    }

    public int getByte(int offset)
    {
        return data[offset] & 0xFF;
    }

    public void setByte(int offset, int value)
    {
        data[offset] = (byte) value;
    }

    public int getWord(int offset)
    {
        return word(data, offset);
    }

    public void setWord(int offset, int value)
    {
        data[offset] = (byte) value;
        data[offset + 1] = (byte) (value >> 8);
    }

    /** Text in a fixed field, up to the first zero byte. */
    public String getText(int offset, int length)
    {
        int end = offset;
        while (end < offset + length && data[end] != 0)
            end++;
        return new String(data, offset, end - offset, java.nio.charset.StandardCharsets.ISO_8859_1);
    }

    /** Writes text into a fixed field, zero-filling the rest; one zero byte always ends it. */
    public void setText(int offset, int length, String text)
    {
        byte[] b = text.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        for (int i = 0; i < length; i++)
            data[offset + i] = i < b.length && i < length - 1 ? b[i] : 0;
    }
}
