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

import cfevolution.data.f1gp.Atan2LookupTable;
import cfevolution.data.f1gp.CosLookupTable;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 *     (the nose is +Y), Z up. The twin is another point (0 for none).</li>
 * <li>An edge is two point numbers. A polygon element is a byte (bit 80h
 *     clear) whose low 4 bits are the slot in the team's colour set, then
 *     edge numbers (signed: negative means the edge is walked backwards)
 *     ending 0; with bit 40h set a point number follows (the game's side
 *     test for two polygons back to back).</li>
 * <li>A bitmap element is 80h-9Fh, point, max distance, picture: the
 *     wheels (tyre pictures 0 behind and 33 in front onwards, in the team's
 *     set) and, with bit 10h, the helmet (66-74, in the driver's set; drawn
 *     only when a driver is in the car).</li>
 * </ul>
 * Team 1 is drawn with its own element list (stockObject0_GraphicElements_McLaren,
 * the "McLaren" nose, which has no slot 11), at the same offsets, whenever
 * the car is drawn in 3D (sub_19400 sets wObjFlag_Use3dModelMcLaren for
 * bTeamNr 1). Only the flat pictures used for cars far away show team 1
 * with the usual nose.
 *
 * <p><b>Drawn as the game draws it (since 2026-11-26; Hrvoje wanted the game's
 * own car, not an interpretation).</b> {@link #draw} is a port of GP.EXE's
 * RenderStockObject (IDA 0x17D28) and what it calls, for a car close in front of
 * the chase camera, at the game's own 320x200 pixels: the points turned with the
 * game's cosine table and integer arithmetic, projected as the game projects them
 * (256 across and 221 up per unit of depth, the pitch of the camera only moving
 * the horizon), the twin points' one-pixel nudge, the draw order the game stores
 * for the angle (no depth sorting: back to front as listed, hidden faces left
 * out), the side test of back-to-back polygons, the edges stepped and the rows
 * filled as sub_F859 and RenderPolygon do, and the tyre and helmet pictures
 * chosen, scaled and mirrored as RenderBitmap does. Left out, as they cannot
 * happen to a car alone in the middle of the picture: clipping at the screen's
 * edges, the cockpit's and mirrors' masks, fog (only further away or in the
 * wet), damage and steering (the front tyres turn with the steering).
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

    private final int[] scale;
    /** Per point: {X code, Y code, Z, twin, the point X and Y come from or -1}. */
    private final int[][] points;
    /** Per edge: {first point, second point}. */
    private final int[][] edges;
    /** The points worked out before anything is drawn. */
    private final int[] precalc;
    /** The 32 draw orders, by viewing angle: element offsets, back to front. */
    private final int[][] orders;
    private final Map<Integer, Element> usual, mcLaren;

    private static final class Element
    {
        final int colour;          // polygon: the slot; bitmap: -1
        final int[] edges;         // polygon: signed edge numbers
        final int sidePoint;       // polygon with bit 40h: the point of the side test, else -1
        final int point, picture;  // bitmap
        final int maxDistance;
        final boolean helmet;

        Element(int colour, int[] edges, int sidePoint)
        {
            this.colour = colour;
            this.edges = edges;
            this.sidePoint = sidePoint;
            point = picture = maxDistance = -1;
            helmet = false;
        }

        Element(int point, int maxDistance, int picture, boolean helmet)
        {
            colour = -1;
            edges = null;
            sidePoint = -1;
            this.point = point;
            this.maxDistance = maxDistance;
            this.picture = picture;
            this.helmet = helmet;
        }
    }

    private CarModel(int[] scale, int[][] points, int[][] edges, int[] precalc, int[][] orders,
                     Map<Integer, Element> usual, Map<Integer, Element> mcLaren)
    {
        this.scale = scale;
        this.points = points;
        this.edges = edges;
        this.precalc = precalc;
        this.orders = orders;
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
                edgeTable = word(d, 14) - self, lists = word(d, 0x1C) - self;
            int[] scale = new int[(elements - scales) / 2];
            for (int i = 0; i < scale.length; i++)
                scale[i] = (short) word(d, scales + 2 * i);
            int n = (edgeTable - pts) / 8;
            int[][] points = new int[n][];
            for (int i = 0; i < n; i++)
            {
                int o = pts + 8 * i, x = word(d, o), y = word(d, o + 2), z = (short) word(d, o + 4), twin = word(d, o + 6);
                if (twin >= n)
                    return null;
                if ((x & 0x8000) != 0)
                {
                    int r = x & 0x7FFF;
                    if (r >= i || points[r][4] >= 0)
                        return null;
                    points[i] = new int[] { points[r][0], points[r][1], z, twin, r };
                }
                else
                {
                    if (!isCode(x, scale.length) || !isCode(y, scale.length))
                        return null;
                    points[i] = new int[] { x, y, z, twin, -1 };
                }
            }
            int[][] orders = new int[32][];
            int maxOffset = 0;
            for (int a = 0; a < 32; a++)
            {
                int list = lists + word(d, lists + 2 * a);
                List<Integer> order = new ArrayList<Integer>();
                for (int k = 0; ; k++)
                {
                    int off = word(d, list + 2 * k);
                    if (off == 0x8000)
                        break;
                    order.add(off);
                    maxOffset = Math.max(maxOffset, off);
                }
                orders[a] = new int[order.size()];
                for (int k = 0; k < orders[a].length; k++)
                    orders[a][k] = order.get(k);
            }
            List<Integer> pre = new ArrayList<Integer>();
            int start = elements;
            while ((d[start] & 0xFF) != 0xFF)   // the points worked out first
            {
                if ((d[start] & 0xFF) >= n)
                    return null;
                pre.add(d[start++] & 0xFF);
            }
            start++;
            int[] precalc = new int[pre.size()];
            for (int i = 0; i < precalc.length; i++)
                precalc[i] = pre.get(i);
            int edgeCount = (lists - edgeTable) / 2;
            int[][] edges = new int[edgeCount][];
            for (int e = 0; e < edgeCount; e++)
                edges[e] = new int[] { d[edgeTable + 2 * e] & 0xFF, d[edgeTable + 2 * e + 1] & 0xFF };
            Map<Integer, Element> usual = elements(d, start, orders, edges, n);
            Map<Integer, Element> mcLaren = elements(d, start + MCLAREN_OFFSET, orders, edges, n);
            return usual == null || mcLaren == null ? null : new CarModel(scale, points, edges, precalc, orders, usual, mcLaren);
        }
        catch (RuntimeException e)
        {
            return null;
        }
    }

    private static boolean isCode(int c, int scales)
    {
        return c == 0 || (c % 2 == 0 && ((c >= 2 && c < 2 + 2 * scales) || (c >= 34 && c < 34 + 2 * scales)));
    }

    private static Map<Integer, Element> elements(byte[] d, int base, int[][] orders, int[][] edges, int points)
    {
        Map<Integer, Element> out = new HashMap<Integer, Element>();
        for (int[] order : orders)
            for (int off : order)
            {
                if (out.containsKey(off))
                    continue;
                int p = base + off, b0 = d[p] & 0xFF;
                if ((b0 & 0x80) == 0)
                {
                    List<Integer> list = new ArrayList<Integer>();
                    int q = p + 1;
                    for (; d[q] != 0; q++)
                    {
                        int e = d[q];
                        if (Math.abs(e) >= edges.length || edges[Math.abs(e)][0] >= points || edges[Math.abs(e)][1] >= points)
                            return null;
                        list.add(e);
                    }
                    int side = -1;
                    if ((b0 & 0x40) != 0)
                    {
                        side = d[q + 1] & 0xFF;
                        if (side >= points)
                            return null;
                    }
                    int[] e = new int[list.size()];
                    for (int i = 0; i < e.length; i++)
                        e[i] = list.get(i);
                    out.put(off, new Element(b0 & 0x0F, e, side));
                }
                else if ((b0 & 0x20) == 0)
                {
                    if ((b0 & 0x02) != 0)
                        return null;   // a colour set of its own: never on the car
                    int point = d[p + 1] & 0xFF;
                    if (point >= points)
                        return null;
                    out.put(off, new Element(point, d[p + 2] & 0xFF, d[p + 3] & 0xFF, (b0 & 0x10) != 0));
                }
                else
                    return null;   // a line: never on the car
            }
        return out;
    }

    /**
     * The game's outside cameras, chase (behind) and the one in front looking back,
     * which share their placing (2026-11-25, read in GP.EXE): 1920 units from the
     * car along the ground (FrontRearCameraRotation, word_20957 = F880h, turned
     * 8000h for the one in front) and 384 above it (ChangeCameraView,
     * word_20955 = 180h, the same units as the model's). The game then pitches
     * the view down 400h (5.6 degrees), which only moves the horizon, so the
     * car itself is drawn as a level camera sees it. Checked against the game's
     * chase view: the car's middle 8 degrees below the horizon, as these give.
     */
    public static final int CAMERA_BACK = 1920, CAMERA_UP = 384;
    /** The middle of the car's height. */
    private static final int MIDDLE_HEIGHT = 110;

    /** How far below the camera the car's middle is seen, in degrees (about 8). */
    public static double cameraTilt()
    {
        return Math.toDegrees(Math.atan2(CAMERA_UP - MIDDLE_HEIGHT, CAMERA_BACK));
    }

    /** The projection (RenderStockObject, IDA 0x185C9): screen x is 160 + 256 x / depth, and the
        height is scaled by word_292BC = 6E80h before its 256 / depth, so 221 per unit up. */
    private static final int HEIGHT_SCALE = 0x6E80;
    /** RenderBitmap's height scale for pictures, word_292BE. */
    private static final int PICTURE_HEIGHT_SCALE = 0xDD00;

    /**
     * The picture's size in the game's pixels, {width, height}: room for the
     * car at every heading, with a margin (measured over all 360 degrees of
     * both cars, helmet and tyres included: 148 x 46 pixels).
     */
    public static final int WIDTH = 160, HEIGHT = 54;
    /** Where the car's own position falls in the picture: the middle across, and the
        horizon (the camera's height) this far above the picture's top. */
    private static final int MIDDLE_X = WIDTH / 2, HORIZON = -9;

    /**
     * Draws the car as the game draws it in its chase view (heading 0, from
     * behind) and from the camera in front (180), from the cameras' own distance
     * and height, turned to any heading (Hrvoje, 2026-11-25: one view, the game's
     * cars at the game's camera height, all the way round): 90 looks at its
     * right side, nose to the right. The picture is in the game's own pixels,
     * {@link #WIDTH} x {@link #HEIGHT}, which on the game's 4:3 screen are 1.2
     * times as tall as they are wide. Slots 0-15 are the team's colour set,
     * 16-31 ({@link #HELMET_SLOTS} on) the helmet's, -1 where nothing is drawn.
     *
     * @param sprites the tyres and helmet; without them the car is drawn without them
     * @param mcLarenNose team slot 1's own car, which the game draws for it close up
     * @param helmet whether a driver is in the car (the game draws no helmet otherwise)
     */
    public GameSprites.Sprite draw(GameSprites sprites, double heading, boolean mcLarenNose, boolean helmet)
    {
        int angle = (int) Math.round(heading * 65536 / 360) & 0xFFFF;
        return new Frame(angle, sprites, helmet, mcLarenNose ? mcLaren : usual).draw();
    }

    /** The picture's size, {width, height}, in the game's pixels. */
    public static int[] box()
    {
        return new int[] { WIDTH, HEIGHT };
    }

    /** The game's cosine (4000h = 1) of an angle (10000h a turn), from its table without interpolation,
        as RenderStockObject looks it up: the absolute value, shifted, as an index. */
    private static int cos(int angle)
    {
        angle &= 0xFFFF;
        if ((angle & 0x8000) != 0)
            angle = -angle & 0xFFFF;
        return CosLookupTable.get(angle >>> 3);
    }

    private static int sin(int angle)
    {
        return cos(0x4000 - angle);
    }

    /** One drawing: the points worked out as they are first needed, in the game's order. */
    private final class Frame
    {
        final int angle;
        final GameSprites sprites;
        final boolean helmet;
        final Map<Integer, Element> list;
        final int[] cosScale, sinScale;
        final boolean[] done;
        final int[] x, y, sx, sy;
        final Edge[] edgeDone;
        final byte[] slots = new byte[WIDTH * HEIGHT];

        Frame(int angle, GameSprites sprites, boolean helmet, Map<Integer, Element> list)
        {
            this.angle = angle;
            this.sprites = sprites;
            this.helmet = helmet;
            this.list = list;
            // RenderStockObject's tables of the scale values times the cosine and sine of the angle
            // (the near object's whole units: imul, then the top word after two shifts)
            int c = cos(angle), s = sin(angle);
            cosScale = new int[scale.length];
            sinScale = new int[scale.length];
            for (int i = 0; i < scale.length; i++)
            {
                cosScale[i] = (short) ((scale[i] * c) >> 14);
                sinScale[i] = (short) ((scale[i] * s) >> 14);
            }
            int n = points.length;
            done = new boolean[n];
            x = new int[n];
            y = new int[n];
            sx = new int[n];
            sy = new int[n];
            edgeDone = new Edge[edges.length];
            java.util.Arrays.fill(slots, (byte) -1);
        }

        int byCos(int code)
        {
            return code == 0 ? 0 : code >= 34 ? -cosScale[(code - 34) / 2] : cosScale[(code - 2) / 2];
        }

        int bySin(int code)
        {
            return code == 0 ? 0 : code >= 34 ? -sinScale[(code - 34) / 2] : sinScale[(code - 2) / 2];
        }

        GameSprites.Sprite draw()
        {
            for (int p : precalc)
                point(p, true);
            int[] order = orders[angle >>> 11];   // (angle >> AngleShift 10) & FFFEh, a word offset
            for (int off : order)
            {
                Element e = list.get(off);
                if (e.edges != null)
                    polygon(e);
                else if (sprites != null && (!e.helmet || helmet))
                    bitmap(e);
            }
            return GameSprites.Sprite.of(WIDTH, HEIGHT, slots);
        }

        /** Works a point out unless it already is (sub_1778F): a point taking X and Y from another
            works that one out first, copies its depth and screen x and has its own screen y. */
        void point(int p, boolean always)
        {
            if (done[p] && !always)
                return;
            done[p] = true;
            int r = points[p][4];
            if (r < 0)
            {
                place(p);
                return;
            }
            if (!done[r])
            {
                done[r] = true;
                place(r);
            }
            x[p] = x[r];
            y[p] = y[r];
            sx[p] = sx[r];
            sy[p] = screenY(points[p][2] - CAMERA_UP, y[p]);
        }

        /** A point's place in front of the camera and on the screen. */
        void place(int p)
        {
            int[] pt = points[p];
            x[p] = (short) (byCos(pt[0]) + bySin(pt[1]));
            y[p] = (short) (byCos(pt[1]) - bySin(pt[0]) + CAMERA_BACK);
            sx[p] = MIDDLE_X + (x[p] << 8) / y[p];   // idiv: towards zero
            sy[p] = screenY(pt[2] - CAMERA_UP, y[p]);
            // a point on the same column as its twin, already worked out, moves one pixel right
            int t = pt[3];
            if (t != 0 && done[t] && sx[t] == sx[p])
                sx[p]++;
        }

        /** The height on the screen, rounded as the game rounds it: to the nearest below the
            horizon, but one more whenever the quotient (not the remainder) reaches the depth. */
        int screenY(int z, int depth)
        {
            int h = (z * HEIGHT_SCALE * 2) >> 16;
            int dividend = h << 8;
            int q = dividend / depth, r = dividend % depth;
            int twice = (r << 1) & 0xFFFF;
            if ((r & 0x8000) != 0)
            {
                if ((-twice & 0xFFFF) >= depth)
                    q--;
            }
            else if ((q & 0xFFFF) >= depth)
                q++;
            return HORIZON - (short) q;
        }

        void polygon(Element e)
        {
            List<Edge> ring = new ArrayList<Edge>();
            List<Boolean> down = new ArrayList<Boolean>();
            for (int v : e.edges)
            {
                int k = Math.abs(v);
                Edge edge = edgeDone[k];
                if (edge == null)
                {
                    point(edges[k][0], false);
                    point(edges[k][1], false);
                    edge = edgeDone[k] = new Edge(sx[edges[k][0]], sy[edges[k][0]], sx[edges[k][1]], sy[edges[k][1]]);
                }
                if (edge.flat)
                    continue;
                ring.add(edge);
                down.add(edge.firstHigher ^ (v < 0));
            }
            if (e.sidePoint >= 0)
            {
                int p = e.sidePoint, t = points[p][3];
                point(p, false);
                point(t, false);
                if (sx[p] > sx[t])
                    return;
            }
            fill(ring, down, e.colour);
        }

        /** RenderPolygon's rows for a polygon wholly on the screen: from the lowest corner up, the
            edges walked forwards on the left and backwards on the right, each row filled from the
            left edge's x up to (not including) the right edge's. A polygon facing away fills nothing. */
        void fill(List<Edge> ring, List<Boolean> down, int colour)
        {
            int n = ring.size();
            if (n == 0)
                return;
            int first = 0;
            for (int i = 1; i < n; i++)
                if (ring.get(i).bottom > ring.get(first).bottom)
                    first = i;
            int left, right;
            if (down.get(first))
            {
                right = first;
                left = (first + 1) % n;
            }
            else
            {
                left = first;
                right = (first - 1 + n) % n;
            }
            int row = ring.get(first).bottom;
            int li = 0, ri = 0;
            while (true)
            {
                int ax;
                if (li < ring.get(left).xs.length)
                    ax = ring.get(left).xs[li++];
                else
                {
                    int next = (left + 1) % n;
                    if (next == right || ring.get(next).bottom != row)
                        return;
                    left = next;
                    li = 0;
                    ax = li < ring.get(left).xs.length ? ring.get(left).xs[li++] : -32768;
                }
                int cx;
                while (true)
                {
                    if (ri < ring.get(right).xs.length)
                    {
                        cx = ring.get(right).xs[ri++];
                        break;
                    }
                    int next = (right - 1 + n) % n;
                    if (next == left || ring.get(next).bottom != row)
                        return;
                    right = next;
                    ri = 0;
                }
                span(row - 1, ax, cx, colour);
                row--;
                if (row == 0)
                    return;
            }
        }

        void span(int row, int from, int to, int slot)
        {
            if (row < 0 || row >= HEIGHT)
                return;
            for (int i = Math.max(0, from); i < Math.min(WIDTH, to); i++)
                slots[row * WIDTH + i] = (byte) slot;
        }

        /** A tyre or the helmet (RenderStockObject's bitmap elements, then RenderBitmap). */
        void bitmap(Element e)
        {
            point(e.point, false);
            int p = e.point;
            int depth = y[p] >> 3;   // the near object's units, in the far ones' eighths
            if ((e.maxDistance << 7) < depth)
                return;
            int picture, slotBase;
            boolean mirrored;
            if (e.picture < GameSprites.HELMET_FIRST)
            {
                // the tyres: the angle the wheel is seen from, its own direction from the camera taken off
                int off = sx[p] - MIDDLE_X, a = Math.min(255, Math.abs(off));
                int toward = Atan2LookupTable.get(a * 8);
                if (off < 0)
                    toward = -toward;
                int seen = (angle - toward) & 0x7FFF;
                mirrored = (seen & 0x4000) != 0;
                if (seen > 0x4000)
                    seen = 0x8000 - seen;
                picture = e.picture + ((seen + 0x100) >> 9);
                slotBase = 0;
            }
            else if (e.picture <= GameSprites.HELMET_LAST)
            {
                int seen = (short) angle;
                mirrored = seen < 0;
                int a = Math.abs(seen) & 0xFFFF;
                picture = e.picture + (((a + 0x800) & 0xFFFF) >>> 12);
                slotBase = HELMET_SLOTS;
            }
            else
                return;
            GameSprites.Raw raw = sprites.raw(picture);
            if (raw == null || depth <= 0)
                return;
            long q = ((long) raw.size << 13) / depth;
            int s = (int) (q & 0xFFFF);
            if (s == 0)
                s = 1;
            if (s > 0x8000)
                s = 0x8000;
            if (raw.halfWidth == 0)
                return;
            // the columns' edges on the screen, outwards from the point, each a fraction further
            int half = raw.halfWidth, whole = s >>> 13, part = (s << 3) & 0xFFFF, dir = mirrored ? -1 : 1;
            int[] rightOf = new int[half + 1], leftOf = new int[half + 1];
            int cx = sx[p], dx = sx[p], acc = 0;
            rightOf[0] = clampX(cx);
            for (int i = 1; i <= half; i++)
            {
                acc += part;
                if (acc > 0xFFFF)
                {
                    acc &= 0xFFFF;
                    cx += dir;
                    dx -= dir;
                }
                cx += whole * dir;
                dx -= whole * dir;
                leftOf[i] = clampX(dx);
                rightOf[i] = clampX(cx);
            }
            // the rows: the anchor at the point, each screen row taking the picture row its height reaches
            int v = (int) (((long) s * PICTURE_HEIGHT_SCALE) >>> 16);
            if (v == 0)
                return;
            long m = ((((long) (raw.below & 0xFFFF) * v) << 3) & 0xFFFFFFFFL);
            int below = (int) (m >>> 16);
            int row = (short) (sy[p] + below);
            long inv = 0x1000000L / v;
            if (inv > 0xFFFF)
                inv = 0xFFFF;
            if (inv == 0)
                return;
            long step = inv << 5;
            long at = (-(((inv * below) << 5) & 0xFFFFFFFFL)) & 0xFFFFFFFFL;
            at = (at + ((long) raw.below << 16)) & 0xFFFFFFFFL;
            while ((int) at < 0)
                at = (at + step) & 0xFFFFFFFFL;
            for (int count = row; count >= 0; count--, row--)
            {
                int r = (short) (at >>> 16);
                at = (at + step) & 0xFFFFFFFFL;
                if (r * 2 >= raw.rows * 2)
                    return;
                int[] runs = raw.runs[r];
                for (int k = 0; k + 2 < runs.length; k += 3)
                {
                    int a = column(runs[k + 1], rightOf, leftOf), b = column(runs[k + 2], rightOf, leftOf);
                    if (a == Integer.MIN_VALUE || b == Integer.MIN_VALUE)
                        continue;
                    if (mirrored)
                        span(row, b, a, (runs[k] & 0x0F) + slotBase);
                    else
                        span(row, a, b, (runs[k] & 0x0F) + slotBase);
                }
            }
        }

        int column(int c, int[] rightOf, int[] leftOf)
        {
            if (c >= 0)
                return c < rightOf.length ? rightOf[c] : Integer.MIN_VALUE;
            return -c < leftOf.length ? leftOf[-c] : Integer.MIN_VALUE;
        }

        /** RenderBitmap keeps each column's edge on the screen: 0 to the screen's width. */
        int clampX(int v)
        {
            return v < 0 ? 0 : v > WIDTH ? WIDTH : v;
        }
    }

    /**
     * An edge stepped from its lower end up (sub_F859, IDA 0xF859, for an edge on the
     * screen): its lower row, and the x of each row from the one above it up to its top
     * end, by the game's own error stepping. Level edges take no part in filling.
     */
    private static final class Edge
    {
        final boolean flat, firstHigher;
        final int bottom;
        final int[] xs;

        Edge(int x1, int y1, int x2, int y2)
        {
            flat = y1 == y2;
            firstHigher = y1 < y2;   // the game's flag 40h: the first point is the higher one on the screen
            int yb, yt, xb, xt;
            if (y1 > y2)
            {
                yb = y1; yt = y2; xb = x1; xt = x2;
            }
            else
            {
                yb = y2; yt = y1; xb = x2; xt = x1;
            }
            bottom = yb;
            List<Integer> out = new ArrayList<Integer>();
            if (!flat)
            {
                int dy = yb - yt, ax = xb, bx = yb;
                int dir = xb < xt ? 1 : -1, across = Math.abs(xt - xb);
                if (across >= dy)
                {
                    int err = ~(across >> 1) & 0xFFFF;
                    while (ax != xt)
                    {
                        ax += dir;
                        err += dy;
                        if (err > 0xFFFF)
                        {
                            err = (err & 0xFFFF) - across & 0xFFFF;
                            bx--;
                            out.add(ax);
                        }
                    }
                }
                else
                {
                    int err = ~(dy >> 1) & 0xFFFF;
                    while (bx != yt)
                    {
                        bx--;
                        out.add(ax);
                        err += across;
                        if (err > 0xFFFF)
                        {
                            err = (err & 0xFFFF) - dy & 0xFFFF;
                            ax += dir;
                        }
                    }
                }
            }
            xs = new int[out.size()];
            for (int i = 0; i < xs.length; i++)
                xs[i] = out.get(i);
        }
    }

    /** Whether a drawing of the McLaren nose differs from the usual one: the slots each list paints. */
    public boolean[] slotsUsed(boolean mcLarenNose)
    {
        boolean[] used = new boolean[16];
        for (Element e : (mcLarenNose ? mcLaren : usual).values())
            if (e.edges != null)
                used[e.colour] = true;
        return used;
    }
}
