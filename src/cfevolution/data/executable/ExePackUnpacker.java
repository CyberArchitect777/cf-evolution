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

import java.io.IOException;

/**
 * Unpacks an EXEPACK-compressed DOS program, which is how the game ships
 * GP.EXE, producing the file the IUP unpacker produces, so every offset the
 * executable editor uses holds.
 *
 * EXEPACK keeps the program as blocks read backwards from the end of the
 * packed data: each block ends in a command byte (B0h: one byte repeated,
 * B2h: bytes copied as they are; bit 0 marks the last block) and a length,
 * and whatever precedes the last block is already in place. Its variables
 * (real IP, CS, SP, SS, unpacked length in paragraphs, "RB") sit at the start
 * of its code, and the relocations follow its "Packed file is corrupt"
 * message as 16 counted lists, one per 64 KB.
 *
 * The output header is IUP's: relocations from 20h with "IUP*" before them,
 * padded to whole paragraphs, and MINALLOC = packed MINALLOC + packed image
 * paragraphs (rounded down) - unpacked paragraphs. Checked on all six packed
 * builds (retail and no-manual-check, EU, IT and US): the result equals IUP's
 * except four bytes just below the program's starting stack pointer, where
 * IUP leaves some of its own stack. Those differ from one IUP run to another
 * (FF 02 02 73 in one set of files here, 98 02 06 73 in another), the program
 * overwrites them before reading them, and here they stay zero as EXEPACK
 * leaves them.
 *
 * @author barrie
 */
public class ExePackUnpacker
{
    private static final byte[] MESSAGE = "Packed file is corrupt".getBytes();

    private ExePackUnpacker()
    {
    }

    private static int word(byte[] d, int o)
    {
        return (d[o] & 0xFF) | ((d[o + 1] & 0xFF) << 8);
    }

    private static void putWord(byte[] d, int o, int v)
    {
        d[o] = (byte) v;
        d[o + 1] = (byte) (v >> 8);
    }

    /** True if the file is an EXEPACK-compressed program. */
    public static boolean isPacked(byte[] file)
    {
        try
        {
            unpack(file);
            return true;
        }
        catch (IOException e)
        {
            return false;
        }
    }

    /** The unpacked program, or an IOException if the file is not EXEPACK. */
    public static byte[] unpack(byte[] file) throws IOException
    {
        try
        {
            return unpackOrFail(file);
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            throw new IOException("damaged packed file");
        }
        catch (NegativeArraySizeException e)
        {
            throw new IOException("damaged packed file");
        }
    }

    private static byte[] unpackOrFail(byte[] file) throws IOException
    {
        if (file.length < 32 || file[0] != 'M' || file[1] != 'Z')
            throw new IOException("not a DOS program");
        int lastPage = word(file, 2), pages = word(file, 4), headerParas = word(file, 8);
        int minAlloc = word(file, 10), maxAlloc = word(file, 12), packedCs = word(file, 22);
        int size = pages * 512 - (lastPage != 0 ? 512 - lastPage : 0);
        int imageStart = headerParas * 16;
        if (size > file.length || imageStart >= size)
            throw new IOException("truncated");
        int imageLength = size - imageStart;
        int stub = imageStart + packedCs * 16;

        // EXEPACK's variables: IP, CS, (scratch), its own size, SP, SS, unpacked paragraphs, "RB"
        int signature;
        if (file[stub + 14] == 'R' && file[stub + 15] == 'B')
            signature = 14;
        else if (file[stub + 16] == 'R' && file[stub + 17] == 'B')
            signature = 16;
        else
            throw new IOException("not EXEPACK");
        int realIp = word(file, stub), realCs = word(file, stub + 2), stubSize = word(file, stub + 6);
        int realSp = word(file, stub + 8), realSs = word(file, stub + 10), unpackedParas = word(file, stub + 12);

        // the relocations, after the message
        int table = indexOf(file, MESSAGE, stub, Math.min(file.length, stub + stubSize));
        if (table < 0)
            throw new IOException("no relocation table");
        table += MESSAGE.length;
        int count = 0;
        for (int segment = 0, p = table; segment < 16; segment++)
        {
            int n = word(file, p);
            count += n;
            p += 2 + n * 2;
        }

        // the program, unpacked backwards from the end of the packed data
        byte[] program = new byte[unpackedParas * 16];
        int packedLength = packedCs * 16;
        System.arraycopy(file, imageStart, program, 0, Math.min(packedLength, program.length));
        int i = imageStart + packedLength;
        while ((file[i - 1] & 0xFF) == 0xFF)
            i--;
        int j = program.length;
        while (true)
        {
            int command = file[--i] & 0xFF;
            i -= 2;
            int length = word(file, i);
            if ((command & 0xFE) == 0xB0)
            {
                byte value = file[--i];
                for (int k = 0; k < length; k++)
                    program[--j] = value;
            }
            else if ((command & 0xFE) == 0xB2)
            {
                i -= length;
                j -= length;
                System.arraycopy(file, i, program, j, length);
            }
            else
                throw new IOException("damaged packed data");
            if ((command & 1) != 0)
                break;
        }
        if (i < imageStart)
            throw new IOException("damaged packed data");

        // IUP's header
        int newHeaderParas = (32 + 4 * count + 15) / 16;
        int total = newHeaderParas * 16 + program.length;
        byte[] out = new byte[total];
        out[0] = 'M';
        out[1] = 'Z';
        putWord(out, 2, total % 512);
        putWord(out, 4, (total + 511) / 512);
        putWord(out, 6, count);
        putWord(out, 8, newHeaderParas);
        putWord(out, 10, minAlloc + imageLength / 16 - unpackedParas);
        putWord(out, 12, maxAlloc);
        putWord(out, 14, realSs);
        putWord(out, 16, realSp);
        putWord(out, 20, realIp);
        putWord(out, 22, realCs);
        putWord(out, 24, 0x20);
        out[0x1C] = 'I';
        out[0x1D] = 'U';
        out[0x1E] = 'P';
        out[0x1F] = '*';
        int o = 0x20;
        for (int segment = 0, p = table; segment < 16; segment++)
        {
            int n = word(file, p);
            p += 2;
            for (int k = 0; k < n; k++, p += 2, o += 4)
            {
                putWord(out, o, word(file, p));
                putWord(out, o + 2, segment * 0x1000);
            }
        }
        System.arraycopy(program, 0, out, newHeaderParas * 16, program.length);
        return out;
    }

    private static int indexOf(byte[] d, byte[] what, int from, int to)
    {
        for (int i = from; i + what.length <= to; i++)
        {
            int k = 0;
            while (k < what.length && d[i + k] == what[k])
                k++;
            if (k == what.length)
                return i;
        }
        return -1;
    }
}
