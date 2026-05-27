/*
 * Chequered Flag: An editor for Formula One Grand Prix/World Circuit
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

package chequeredflag.data.f1gp;

/*
 * User: Rene
 * Date: 18-okt-2005
 * Time: 22:11:20
 * $Id: F1GPMath.java,v 1.2 2007/04/11 21:33:59 ksix Exp $
 */

public class F1GPMath {
	public static void F1GPMathMain(String[] args) {
		// prove of inaccurateness of f1gp's sqrt function:
		// often the result is 1 too low.
		testSqrt64(0);
		testSqrt64(1);
		testSqrt64(2);
		testSqrt64(3);
		testSqrt64(4);
		testSqrt64(5);
		testSqrt64(6);
		testSqrt64(7);
		testSqrt64(8);
		testSqrt64(9);
		testSqrt64(10);
		testSqrt64(16);
		testSqrt64(123);
		testSqrt64(256);
		testSqrt64(1024);
		testSqrt64(0x1000);
		testSqrt64(0x10000);
		testSqrt64(0x10001);
		testSqrt64(0x10002);
		testSqrt64(0x100000);
		testSqrt64(0x1000000);
		testSqrt64(0x10000000);
	}

	private static void testSqrt64(long v) {
		long s = v * v;
		System.out.println(v + ": sqrt(" + s + ") = " + sqrt64(s) + " (" + Math.sqrt(s) + ")");
	}

	public static long sqrt32(long in) {
		char val = 0;
		long tmp = in;

		while (tmp < 0x40000000l) {
			val++;
			tmp = (tmp << 2) & 0xffffffffl;
			if (tmp == 0)
				return in & 0xffff0000l;
		}

		tmp >>>= 1;
		char c = (char) ((tmp >>> 16) + 0x8000);

		for (int cnt = 2; cnt >= 0; --cnt)
			c = (char) ((c >>> 1) + tmp / c);

		return c >>> (val & 0xff);
	}

	public static long sqrt64(long in) {
		if ((in & 0xffffffff00000000l) == 0)
			return sqrt32(in);

		char val = 0;
		long tmp = in;

		while ((tmp >>> 32) < 0x40000000l) {
			val++;
			tmp <<= 2;
			if (tmp == 0)
				return 0;
		}

		tmp >>>= 1;
		long c = (tmp >>> 32) + 0x80000000l;

		for (int cnt = 4; cnt >= 0; --cnt)
			c = ((c >>> 1) + tmp / c) & 0xffffffffl;

		return c >>> (val & 0xff);
	}

	public static short LookupCos(short x) {
		if ((x & 0x8000) != 0)
			x = (short) -x;

		int tmp = x & 7;
		int i = (x >> 2) & 0xFFFE;

		short v1 = CosLookupTable.get(i / 2 + 1);
		short v2 = CosLookupTable.get(i / 2);
		v1 = (short) (v1 - v2);

		int m = (short) (tmp) * v1;
		return (short) ((m >> 3) + v2);
	}


	public static short LookupSin(short x) {
		x = (short) ((-x) + (short) 0x4000);
		return LookupCos(x);
	}

        /**
            Just like LookupCos, but without interpolation.
        */
        public static short LookupCosRaw(short x) {
		if ((x & 0x8000) != 0)
			x = (short) -x;

		int i = (x >> 2) & 0xFFFE;

		return CosLookupTable.get(i / 2);
        }

        /**
            Lookup without interpolation.
        */
        public static short LookupSinRaw(short x) {
		x = (short) ((-x) + (short) 0x4000);
		return LookupCosRaw(x);
        }


	public static int LookupCosbig(short x) {
		if (x < 0)
			x = (short) -x;

		int tmp = x & 7;
		int i = (x >> 2) & 0x3FFE;
		short v1 = CosLookupTable.get(i / 2 + 1);
		short v2 = CosLookupTable.get(i / 2);
		v1 = (short) (v1 - v2);

		int m = (short) (tmp) * v1;
		return (m << 13) + (v2 << 16);
	}


	public static int LookupSinbig(short x) {
		x = (short) ((-x) + (short) 0x4000);
		return LookupCosbig(x);
	}


	public static int LookupAtan2(int x, int y) {
		int a;
		int oldx = x;
		int oldy = y;

		if ((oldx & 0x8000) != 0)
			oldx = -oldx & 0xffff;

		if ((oldy & 0x8000) != 0)
			oldy = -oldy & 0xffff;

		x = (x ^ y) & 0xffff;
		int tmp = x;

		if (oldx >= oldy) {
			int val = (short) oldy << 11;
			a = val;

			if ((short) oldx < 0)
				a = 0;
			else {
				if ((val >> 16) < oldx)
					oldx = (val / oldx) & 0xffff;

				a = (-Atan2LookupTable.data[oldx] + 0x4000) & 0xffff;

				if ((tmp & 0x8000) != 0)
					a = -a & 0xffff;

				if ((short) y < 0)
					a = (a + 0x8000) & 0xffff;
			}
		} else {
			int val = (short) (oldx) << 11;
			a = val;

			if ((short) oldy < 0)
				a = 0;
			else {
				if ((val >> 16) < oldy)
					oldy = (val / oldy) & 0xffff;

				a = Atan2LookupTable.data[oldy];

				if ((tmp & 0x8000) != 0)
					a = -a & 0xffff;

				if ((short) y < 0)
					a = (a + 0x8000) & 0xffff;
			}
		}

		return a;
	}
}
