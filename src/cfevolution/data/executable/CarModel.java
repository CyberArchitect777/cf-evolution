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

import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * The game's 3D car, the one drawn close to the camera (2026-11-21, for
 * Hrvoje's close-up view of the liveries). Read only: the editor never
 * changes it.
 *
 * It is GP.EXE's stockObject0 (IDA seg003:75E1h, EU file 159873; IT -64,
 * US -48, the same shift as the colour tables), an InternalTrackObject:
 * far pointers (all in the same segment) to 14 scale values, 84 points,
 * 125 edges and the elements, and 32 draw orders by viewing angle.
 * <ul>
 * <li>A point is X, Y, Z, twin (words). X and Y pick a scale value: 0 is
 *     zero, 2 + 2i is +scale[i], 34 + 2i is -scale[i]; Z is the height as it
 *     is. A point whose X has the top bit set takes X and Y from point
 *     (X &amp; 7FFFh) and has its own Z. X is across the car, Y along it
 *     (the nose is +Y), Z up.</li>
 * <li>An edge is two point numbers. A polygon element is a byte (bit 80h
 *     clear) whose low 4 bits are the slot in the team's colour set, then
 *     edge numbers (signed: negative means the edge is walked backwards)
 *     ending 0; with bit 40h set a point number follows (the game's side
 *     test for two polygons back to back).</li>
 * <li>A bitmap element is 80h-9Fh, point, max distance, picture: the
 *     wheels (tyre pictures 0 and 33 onwards, in the team's set; the editor
 *     draws cylinders there instead) and, with bit 10h, the helmet (66-74, in
 *     the driver's set; drawn only when a driver is in the car).</li>
 * </ul>
 * Team 1 is drawn with its own element list (stockObject0_GraphicElements_McLaren,
 * the "McLaren" nose, which has no slot 11), at the same offsets, whenever
 * the car is drawn in 3D (sub_19400 sets wObjFlag_Use3dModelMcLaren for
 * bTeamNr 1). Only the flat pictures used for cars far away show team 1
 * with the usual nose.
 *
 * Every polygon of both lists faces outward with the same winding, so
 * hidden faces can be left out by winding alone; with a depth buffer the
 * car can then be drawn from any angle, not only the game's 32.
 */
public final class CarModel
{
    /** Where the model starts in each build, and how many bytes to read (to the far draw orders, where the near ones end). */
    private static final int EU = 159873, IT = EU - 64, US = EU - 48, LENGTH = 0xA09;
    private static final int SELF = 0x75E1;   // the object's own offset in its segment (EU, IT and US alike)
    /** The McLaren list starts this far after the usual one. */
    private static final int MCLAREN_OFFSET = 0x161;

    /** The colour sets a drawing's slots come from: 0-15 the team's set, 16-31 the helmet's. */
    public static final int HELMET_SLOTS = 16;

    private final double[][] points;
    private final List<Element> usual, mcLaren;

    private static final class Element
    {
        final int colour;          // polygon: the slot; bitmap: -1
        final boolean twinned;
        final int[] corners;       // polygon: point numbers
        final int point, picture;  // bitmap
        final boolean helmet;

        Element(int colour, boolean twinned, int[] corners)
        {
            this.colour = colour;
            this.twinned = twinned;
            this.corners = corners;
            point = picture = -1;
            helmet = false;
        }

        Element(int point, int picture, boolean helmet)
        {
            colour = -1;
            twinned = false;
            corners = null;
            this.point = point;
            this.picture = picture;
            this.helmet = helmet;
        }
    }

    private CarModel(double[][] points, List<Element> usual, List<Element> mcLaren)
    {
        this.points = points;
        this.usual = usual;
        this.mcLaren = mcLaren;
    }

    /** The model in an open GP.EXE, or null if it is not where it should be. */
    public static CarModel read(RandomAccessFile file, int gameVersion)
    {
        return parse(BinaryManager.getDataBytes(gameVersion, file, EU, IT, US, LENGTH));
    }

    private static int word(byte[] d, int o)
    {
        return (d[o] & 0xFF) | ((d[o + 1] & 0xFF) << 8);
    }

    /** The model from the bytes starting at the object, or null if they do not hold it. */
    public static CarModel parse(byte[] d)
    {
        try
        {
            if (d == null || d.length < LENGTH || d[0] != 0 || d[1] != 2)
                return null;
            int self = word(d, 2) - 0x2A;   // the scale values follow the header and its far fields
            if (self != SELF)
                return null;
            int segment = word(d, 4);
            for (int p = 2; p <= 14; p += 4)
                if (word(d, p + 2) != segment)
                    return null;
            int scales = word(d, 2) - self, elements = word(d, 6) - self, pts = word(d, 10) - self,
                edges = word(d, 14) - self, lists = word(d, 0x1C) - self;
            int[] scale = new int[(elements - scales) / 2];
            for (int i = 0; i < scale.length; i++)
                scale[i] = (short) word(d, scales + 2 * i);
            int n = (edges - pts) / 8;
            double[][] points = new double[n][3];
            for (int i = 0; i < n; i++)
            {
                int o = pts + 8 * i, x = word(d, o), y = word(d, o + 2), z = word(d, o + 4);
                if ((x & 0x8000) != 0)
                {
                    int r = x & 0x7FFF;
                    if (r >= i)
                        return null;
                    points[i][0] = points[r][0];
                    points[i][1] = points[r][1];
                }
                else
                {
                    points[i][0] = coordinate(x, scale);
                    points[i][1] = coordinate(y, scale);
                }
                points[i][2] = z;
            }
            // The element offsets: every one some draw order uses
            TreeSet<Integer> offsets = new TreeSet<Integer>();
            for (int a = 0; a < 32; a++)
            {
                int list = lists + word(d, lists + 2 * a);
                for (int k = 0; ; k++)
                {
                    int off = word(d, list + 2 * k);
                    if (off == 0x8000)
                        break;
                    offsets.add(off);
                }
            }
            int start = elements;
            while ((d[start] & 0xFF) != 0xFF)   // the points worked out first
                start++;
            start++;
            List<Element> usual = elements(d, start, offsets, edges, n);
            List<Element> mcLaren = elements(d, start + MCLAREN_OFFSET, offsets, edges, n);
            return usual == null || mcLaren == null ? null : new CarModel(points, usual, mcLaren);
        }
        catch (RuntimeException e)
        {
            return null;
        }
    }

    private static double coordinate(int c, int[] scale)
    {
        if (c == 0)
            return 0;
        return c >= 34 ? -scale[(c - 34) / 2] : scale[(c - 2) / 2];
    }

    private static List<Element> elements(byte[] d, int base, TreeSet<Integer> offsets, int edges, int points)
    {
        List<Element> out = new ArrayList<Element>();
        for (int off : offsets)
        {
            int p = base + off, b0 = d[p] & 0xFF;
            if ((b0 & 0x80) == 0)
            {
                List<Integer> corners = new ArrayList<Integer>();
                for (int q = p + 1; d[q] != 0; q++)
                {
                    int e = d[q], v = Math.abs(e);
                    int corner = d[edges + 2 * v + (e > 0 ? 0 : 1)] & 0xFF;
                    if (corner >= points)
                        return null;
                    corners.add(corner);
                }
                int[] c = new int[corners.size()];
                for (int i = 0; i < c.length; i++)
                    c[i] = corners.get(i);
                out.add(new Element(b0 & 0x0F, (b0 & 0x40) != 0, c));
            }
            else if ((b0 & 0x20) == 0)
            {
                int point = d[p + 1] & 0xFF;
                if (point >= points)
                    return null;
                out.add(new Element(point, d[p + 3] & 0xFF, (b0 & 0x10) != 0));
            }
        }
        return out;
    }

    /**
     * Draws the car. Heading 0 looks at it from behind, 90 from its right
     * (nose to the right of the picture), 180 from in front; tilt raises the
     * view from level (0) to looking down (90). Returns a picture whose slots
     * are 0-15 for the team's colour set and 16-31 ({@link #HELMET_SLOTS} on)
     * for the helmet's, -1 where nothing is drawn, sized to {@link #box}.
     *
     * @param sprites the wheels and helmet; without them the car is drawn without them
     * @param unitsPerPixel how many of the model's units make one pixel
     */
    public GameSprites.Sprite draw(GameSprites sprites, double heading, double tilt, boolean mcLarenNose, boolean helmet,
                                   double unitsPerPixel)
    {
        int[] box = box(unitsPerPixel);
        int width = box[0], height = box[1];
        byte[] slots = new byte[width * height];
        java.util.Arrays.fill(slots, (byte) -1);
        double[] depth = new double[width * height];
        java.util.Arrays.fill(depth, Double.NEGATIVE_INFINITY);
        double h = Math.toRadians(heading), t = Math.toRadians(tilt);
        double[] toward = { Math.sin(h) * Math.cos(t), -Math.cos(h) * Math.cos(t), Math.sin(t) };
        double[] right = { Math.cos(h), Math.sin(h), 0 };
        double[] up = { toward[1] * right[2] - toward[2] * right[1], toward[2] * right[0] - toward[0] * right[2],
                        toward[0] * right[1] - toward[1] * right[0] };
        double k = 1 / unitsPerPixel, cx = width / 2.0, cy = height / 2.0;
        double[][] q = new double[points.length][3];
        for (int i = 0; i < points.length; i++)
        {
            q[i][0] = cx + k * dot(points[i], right);
            q[i][1] = cy - k * (dot(points[i], up) - MIDDLE_HEIGHT * Math.cos(t));
            q[i][2] = dot(points[i], toward);
        }
        Canvas c = new Canvas(width, height, slots, depth);
        for (Element e : mcLarenNose ? mcLaren : usual)
        {
            if (e.corners != null)
            {
                if (e.corners.length < 3 || area(q, e.corners) < 0)
                    continue;   // facing away
                for (int i = 1; i + 1 < e.corners.length; i++)
                    c.triangle(q[e.corners[0]], q[e.corners[i]], q[e.corners[i + 1]], e.colour);
            }
            else if (isTyre(e))
                tyre(c, e, right, up, toward, k, cx, cy - k * -MIDDLE_HEIGHT * Math.cos(t));
            else if (sprites != null && (!e.helmet || helmet))
                bitmap(c, sprites, e, q[e.point], heading, tilt, k);
        }
        return GameSprites.Sprite.of(width, height, slots);
    }

    private static boolean isTyre(Element e)
    {
        return e.picture >= 0 && e.picture < GameSprites.HELMET_FIRST;
    }

    /**
     * The game's tyres are flat pictures drawn for a camera about the car's own
     * height. Seen from above or at an angle they cannot turn with the car and
     * look twisted against it (owner, 2026-11-22), so the close-up draws each one
     * as a cylinder instead, axle across the car: sized from the game's own tyre
     * pictures (32 rows high, from behind 18 pixels wide in front and 26 behind,
     * at a tyre picture's 154 / 33.6 units a pixel), black with the grey rim mark
     * (slot 10) of the pictures on its outer face.
     */
    private static final double TYRE_RADIUS = 73, FRONT_TYRE_WIDTH = 82, REAR_TYRE_WIDTH = 119;
    private static final int TYRE_SIDES = 20;
    private static final int TYRE_SLOT = 0, RIM_SLOT = 10;

    private void tyre(Canvas c, Element e, double[] right, double[] up, double[] toward, double k, double cx, double cy)
    {
        double[] centre = points[e.point];
        boolean front = e.picture >= 33;
        double half = (front ? FRONT_TYRE_WIDTH : REAR_TYRE_WIDTH) / 2;
        double outer = centre[0] >= 0 ? 1 : -1;     // the side away from the car
        double[][][] ring = new double[2][TYRE_SIDES][];
        for (int side = 0; side < 2; side++)
            for (int i = 0; i < TYRE_SIDES; i++)
            {
                double a = 2 * Math.PI * i / TYRE_SIDES;
                double[] p = { centre[0] + (side == 0 ? -half : half), centre[1] + TYRE_RADIUS * Math.cos(a),
                               centre[2] + TYRE_RADIUS * Math.sin(a) };
                ring[side][i] = project(p, right, up, toward, k, cx, cy);
            }
        for (int i = 0; i < TYRE_SIDES; i++)       // the tread
        {
            int j = (i + 1) % TYRE_SIDES;
            c.triangle(ring[0][i], ring[0][j], ring[1][j], TYRE_SLOT);
            c.triangle(ring[0][i], ring[1][j], ring[1][i], TYRE_SLOT);
        }
        for (int side = 0; side < 2; side++)       // the two faces
            for (int i = 1; i + 1 < TYRE_SIDES; i++)
                c.triangle(ring[side][0], ring[side][i], ring[side][i + 1], TYRE_SLOT);
        // the rim mark on the outer face, just proud of it so it shows over the face
        double x = centre[0] + outer * (half + 0.5);
        for (int i = 0; i < TYRE_SIDES; i++)
        {
            double a0 = 2 * Math.PI * i / TYRE_SIDES, a1 = 2 * Math.PI * (i + 1) / TYRE_SIDES;
            double r0 = TYRE_RADIUS * 0.45, r1 = TYRE_RADIUS * 0.58;
            double[] p00 = project(new double[] { x, centre[1] + r0 * Math.cos(a0), centre[2] + r0 * Math.sin(a0) }, right, up, toward, k, cx, cy);
            double[] p01 = project(new double[] { x, centre[1] + r1 * Math.cos(a0), centre[2] + r1 * Math.sin(a0) }, right, up, toward, k, cx, cy);
            double[] p10 = project(new double[] { x, centre[1] + r0 * Math.cos(a1), centre[2] + r0 * Math.sin(a1) }, right, up, toward, k, cx, cy);
            double[] p11 = project(new double[] { x, centre[1] + r1 * Math.cos(a1), centre[2] + r1 * Math.sin(a1) }, right, up, toward, k, cx, cy);
            c.triangle(p00, p01, p11, RIM_SLOT);
            c.triangle(p00, p11, p10, RIM_SLOT);
        }
    }

    private static double[] project(double[] p, double[] right, double[] up, double[] toward, double k, double cx, double cy)
    {
        return new double[] { cx + k * dot(p, right), cy - k * dot(p, up), dot(p, toward) };
    }

    /** The middle of the car's height, so it turns about its middle rather than the ground. */
    private static final double MIDDLE_HEIGHT = 110;
    /** The car fits in this radius, across and along, whichever way it faces; the wheels' and wings' tips included. */
    private static final double RADIUS = 600, TOP = 260;
    /** The steepest tilt {@link #box} keeps room for. */
    public static final int MAX_TILT = 30;

    /** The picture's size, {width, height}: room for the car at any heading and any tilt up to {@link #MAX_TILT}. */
    public static int[] box(double unitsPerPixel)
    {
        double s = Math.sin(Math.toRadians(MAX_TILT));
        int w = (int) Math.ceil(2 * RADIUS / unitsPerPixel) + 2;
        int h = (int) Math.ceil(2 * (TOP / 2 + RADIUS * s + 40) / unitsPerPixel) + 2;
        return new int[] { w, h };
    }

    private static double dot(double[] a, double[] b)
    {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    /** Positive for a polygon facing the viewer (screen y runs down). */
    private static double area(double[][] q, int[] corners)
    {
        double a = 0;
        for (int i = 0; i < corners.length; i++)
        {
            double[] u = q[corners[i]], v = q[corners[(i + 1) % corners.length]];
            a += u[0] * v[1] - v[0] * u[1];
        }
        return a;
    }

    /** Sizes of the game's pictures: the model's units per picture pixel is size / this (measured: the
        side picture of the car, 130 pixels for 1028 units at size 266). */
    private static final double SIZE_PER_UNIT = 266 * 130 / 1028.0;
    private static final int HELMET_SIZE = 116;

    /** The helmet: the game's picture for the angle it is seen from (RenderCar's choice, IDA 0x18348),
        66 + (|view angle| + 800h) >> 12 in the driver's set, mirrored on the car's left. (The tyres were
        pictures too until 2026-11-22: tyre 0 behind / 33 in front + (angle folded to 0-4000h, + 100h) >> 9,
        mirrored when the folded view is past 90 degrees; now drawn as cylinders, see {@link #tyre}.) */
    private static void bitmap(Canvas c, GameSprites sprites, Element e, double[] at, double heading, double tilt, double k)
    {
        if (e.picture < GameSprites.HELMET_FIRST || e.picture > GameSprites.HELMET_LAST)
            return;
        double a = ((heading % 360) + 360) % 360;
        if (a > 180)
            a -= 360;   // -180..180: negative is the car's left side
        int picture = Math.min(GameSprites.HELMET_LAST,
                               GameSprites.HELMET_FIRST + (int) ((Math.abs(a) / 180 * 0x8000 + 0x800) / 0x1000));
        boolean mirrored = a < 0;
        GameSprites.Sprite s = sprites.get(picture);
        if (s == null)
            return;
        double px = HELMET_SIZE / SIZE_PER_UNIT * k;   // screen pixels per picture pixel
        double z = at[2] + 30;   // just in front of its point, the middle of the helmet
        for (int y = 0; y < s.height; y++)
            for (int x = 0; x < s.width; x++)
            {
                int slot = s.slotAt(mirrored ? s.width - 1 - x : x, y);
                if (slot < 0)
                    continue;
                int x0 = (int) Math.round(at[0] + (x - s.width / 2.0) * px), x1 = (int) Math.round(at[0] + (x + 1 - s.width / 2.0) * px);
                int y0 = (int) Math.round(at[1] + (y - s.height / 2.0) * px), y1 = (int) Math.round(at[1] + (y + 1 - s.height / 2.0) * px);
                for (int yy = y0; yy < Math.max(y1, y0 + 1); yy++)
                    for (int xx = x0; xx < Math.max(x1, x0 + 1); xx++)
                        c.put(xx, yy, z, slot + HELMET_SLOTS);
            }
    }

    /** A picture being drawn, with a depth per pixel (larger is nearer). */
    private static final class Canvas
    {
        final int width, height;
        final byte[] slots;
        final double[] depth;

        Canvas(int width, int height, byte[] slots, double[] depth)
        {
            this.width = width;
            this.height = height;
            this.slots = slots;
            this.depth = depth;
        }

        void put(int x, int y, double z, int slot)
        {
            if (x < 0 || y < 0 || x >= width || y >= height)
                return;
            int i = y * width + x;
            if (z >= depth[i])
            {
                depth[i] = z;
                slots[i] = (byte) slot;
            }
        }

        void triangle(double[] a, double[] b, double[] c, int slot)
        {
            double area = (b[0] - a[0]) * (c[1] - a[1]) - (c[0] - a[0]) * (b[1] - a[1]);
            if (Math.abs(area) < 1e-9)
                return;
            int minX = (int) Math.floor(Math.min(a[0], Math.min(b[0], c[0])));
            int maxX = (int) Math.ceil(Math.max(a[0], Math.max(b[0], c[0])));
            int minY = (int) Math.floor(Math.min(a[1], Math.min(b[1], c[1])));
            int maxY = (int) Math.ceil(Math.max(a[1], Math.max(b[1], c[1])));
            for (int y = Math.max(0, minY); y <= Math.min(height - 1, maxY); y++)
                for (int x = Math.max(0, minX); x <= Math.min(width - 1, maxX); x++)
                {
                    double px = x + 0.5, py = y + 0.5;
                    double w0 = ((b[0] - px) * (c[1] - py) - (c[0] - px) * (b[1] - py)) / area;
                    double w1 = ((c[0] - px) * (a[1] - py) - (a[0] - px) * (c[1] - py)) / area;
                    double w2 = 1 - w0 - w1;
                    if (w0 < -1e-6 || w1 < -1e-6 || w2 < -1e-6)
                        continue;
                    put(x, y, w0 * a[2] + w1 * b[2] + w2 * c[2], slot);
                }
        }
    }

    /** Whether a drawing of the McLaren nose differs from the usual one: the slots each list paints. */
    public boolean[] slotsUsed(boolean mcLarenNose)
    {
        boolean[] used = new boolean[16];
        for (Element e : mcLarenNose ? mcLaren : usual)
            if (e.corners != null)
                used[e.colour] = true;
        return used;
    }
}
