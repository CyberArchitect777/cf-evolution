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
package cfevolution.data.dataset;

import cfevolution.data.executable.GameLiveries;
import cfevolution.data.gamefiles.MediaContainer;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the car sets of other editors into a {@link Dataset}, in the same
 * terms a .cfe uses, so they import the same way: F1ED's .1ED (an ini file)
 * and ArgEditor's .arged (JSON), on their own or in the zips they are shared
 * in, which may also hold a whole HELMETS.DAT. Layouts in
 * docs/GAME-DATASET.md, from the community sets in the project's Carsets
 * folder and ArgData's colour maps.
 *
 * @author barrie
 */
public class OtherEditorFiles
{
    private OtherEditorFiles()
    {
    }

    /** True for the file kinds read here and by {@link Dataset#read}. */
    public static boolean canRead(File f)
    {
        String n = f.getName().toLowerCase();
        return n.endsWith(Dataset.EXTENSION) || n.endsWith(".zip") || n.endsWith(".1ed") || n.endsWith(".arged");
    }

    /**
     * Reads a .cfe, .1ED, .arged, or a zip holding one of them.
     * helmetPalette (the colours the game draws menu helmets in) turns
     * helmet pictures carried as raw pixels into pictures; null leaves them out.
     */
    public static Dataset read(File file, int[] helmetPalette) throws IOException
    {
        byte[] d = Files.readAllBytes(file.toPath());
        String name = file.getName().toLowerCase();
        if (!Dataset.isZip(d))
        {
            if (name.endsWith(".1ed"))
                return readF1Ed(Dataset.text(d));
            if (name.endsWith(".arged"))
                return readArgEditor(Dataset.text(d), helmetPalette);
            return Dataset.read(file);
        }
        Map<String, byte[]> entries = Dataset.unzip(d);
        for (String path : entries.keySet())
            if (path.toLowerCase().endsWith(Dataset.EXTENSION))
                return Dataset.read(file);
        Dataset out = null;
        byte[] helmets = null;
        for (Map.Entry<String, byte[]> e : entries.entrySet())
        {
            String p = e.getKey().toLowerCase();
            if (out == null && p.endsWith(".1ed"))
                out = readF1Ed(Dataset.text(e.getValue()));
            else if (out == null && p.endsWith(".arged"))
                out = readArgEditor(Dataset.text(e.getValue()), helmetPalette);
            else if (p.endsWith("helmets.dat"))
                helmets = e.getValue();
        }
        if (out == null)
            throw new IOException("no car set in the zip");
        if (helmets != null)
            addHelmetsFile(out, helmets, helmetPalette);
        return out;
    }

    // --- F1ED -----------------------------------------------------------------

    static Dataset readF1Ed(String text)
    {
        Dataset ini = Dataset.parse(text);     // its sections and keys, lower-cased
        Dataset d = new Dataset();
        d.put("dataset", "made by", "F1ED");
        for (int c = 1; c <= GameDataset.CARS; c++)
        {
            String[] f = fields(ini.get("drivers", String.valueOf(c)));
            if (f == null || f.length < 3)
                continue;
            String s = "driver " + c;
            d.put(s, "name", f[0]);
            d.put(s, "qualifying skill", f[1]);
            d.put(s, "race skill", f[2]);
            if (f.length > 3 && f[3].equals("1"))
                d.addNote("F1ED's choice of which drivers start under human control was not imported.");
        }
        for (int t = 1; t <= GameDataset.TEAMS; t++)
        {
            String[] f = fields(ini.get("teams", String.valueOf(t)));
            if (f == null || f.length < 5)
                continue;
            String s = "team " + t;
            d.put(s, "name", f[0]);
            d.put(s, "engine", f[1]);
            d.put(s, "power", f[2]);
            d.put(s, "first car", f[3]);
            d.put(s, "second car", f[4]);
        }
        for (int t = 1; t <= GameDataset.TEAMS; t++)
        {
            putColours(d, "team " + t, "car ", GameLiveries.CAR_PARTS, GameLiveries.CAR_BYTES, fields(ini.get("carcolours", String.valueOf(t))));
            putColours(d, "team " + t, "pit crew ", GameLiveries.PIT_CREW_PARTS, GameLiveries.PIT_CREW_BYTES, fields(ini.get("pitcolours", String.valueOf(t))));
        }
        for (int c = 1; c <= GameDataset.CARS; c++)
            putColours(d, "driver " + c, "helmet ", GameLiveries.HELMET_PARTS, GameLiveries.HELMET_BYTES, fields(ini.get("helmetcolours", String.valueOf(c))));
        if (ini.hasSection("f1ed"))
        {
            copy(ini, "aigrip", d, "computer grip level");
            copy(ini, "humanbhp", d, "player power");
            // Damage1-4 in GP.EXE's order: retire after a wall, after a car, then the wing after a car, after a wall
            copy(ini, "damage1", d, "retire after hitting a wall");
            copy(ini, "damage2", d, "retire after hitting a car");
            copy(ini, "damage3", d, "wing breaks after hitting a car");
            copy(ini, "damage4", d, "wing breaks after hitting a wall");
            copy(ini, "yellowflag", d, "yellow flags after seconds");
            copy(ini, "removecrashedcar", d, "retired cars removed after seconds");
            copy(ini, "wetrace", d, "chance of rain");
            if (ini.get("f1ed", "qtyres") != null)
                d.put("rules", "unlimited qualifying tyres", Dataset.yesNo("1".equals(ini.get("f1ed", "qtyres"))));
            if (ini.get("f1ed", "wetracephoenix") != null)
                d.put("rules", "rain at dry circuits", Dataset.yesNo("1".equals(ini.get("f1ed", "wetracephoenix"))));
        }
        if (ini.hasSection("computercarsetup"))
            d.addNote("F1ED's computer car settings for each circuit were not imported (they belong in the circuit files).");
        return d;
    }

    private static String[] fields(String line)
    {
        if (line == null)
            return null;
        String[] f = line.split(",", -1);
        for (int i = 0; i < f.length; i++)
            f[i] = f[i].trim();
        return f;
    }

    private static void putColours(Dataset d, String section, String prefix, String[] parts, int[] bytes, String[] values)
    {
        if (values == null || values.length < 16)
            return;
        for (int p = 0; p < parts.length; p++)
            d.put(section, prefix + parts[p].toLowerCase(), values[bytes[p]]);
    }

    private static void copy(Dataset ini, String key, Dataset d, String rule)
    {
        String v = ini.get("f1ed", key);
        if (v != null)
            d.put("rules", rule, v.trim());
    }

    // --- ArgEditor --------------------------------------------------------------

    /** ArgEditor's names for the car colours, and the byte of the 16 each is (ArgData's Car.cs). */
    private static final String[] ARG_CAR = { "EngineCover", "CockpitFront", "FrontWingEndplate", "RearWingSide", "NoseSide",
        "Sidepod", "FrontAndRearWing", "NoseTop", "NoseAngle", "CockpitSide", "EngineCoverSide", "EngineCoverRear", "SidepodTop" };
    private static final int[] ARG_CAR_BYTES = { 1, 2, 3, 4, 5, 6, 7, 8, 11, 12, 13, 14, 15 };
    private static final String[] ARG_PIT = { "ShirtPrimary", "ShirtSecondary", "PantsPrimary", "PantsSecondary", "Socks" };
    private static final int[] ARG_PIT_BYTES = { 6, 5, 11, 4, 3 };
    private static final String[] ARG_HELMET = { "Visor", "VisorSurround", "Stripe1", "Stripe2", "Stripe3", "Stripe4", "Stripe5",
        "Stripe6", "Stripe7", "Stripe8", "Stripe9", "Stripe10", "Stripe11", "Stripe12", "Stripe13" };
    private static final int[] ARG_HELMET_BYTES = { 0, 6, 2, 3, 4, 5, 7, 8, 9, 10, 11, 12, 13, 14, 15 };

    static Dataset readArgEditor(String text, int[] helmetPalette) throws IOException
    {
        Object root = new Json(text).value();
        if (!(root instanceof Map))
            throw new IOException("not an ArgEditor file");
        Map<?, ?> m = (Map<?, ?>) root;
        Dataset d = new Dataset();
        d.put("dataset", "made by", "ArgEditor");
        List<?> teams = list(m.get("Teams"));
        List<?> drivers = list(m.get("Drivers"));
        boolean pictureLeftOut = false;
        for (int t = 0; t < teams.size() && t < GameDataset.TEAMS; t++)
        {
            Map<?, ?> team = map(teams.get(t));
            String s = "team " + (t + 1);
            put(d, s, "name", team.get("Name"));
            put(d, s, "engine", team.get("Engine"));
            put(d, s, "power", team.get("Horsepower"));
            Map<?, ?> car = map(team.get("CarColors"));
            for (int i = 0; i < ARG_CAR.length; i++)
                put(d, s, "car " + part(GameLiveries.CAR_PARTS, GameLiveries.CAR_BYTES, ARG_CAR_BYTES[i]), car.get(ARG_CAR[i]));
            Map<?, ?> pit = map(team.get("PitCrewColors"));
            for (int i = 0; i < ARG_PIT.length; i++)
                put(d, s, "pit crew " + part(GameLiveries.PIT_CREW_PARTS, GameLiveries.PIT_CREW_BYTES, ARG_PIT_BYTES[i]), pit.get(ARG_PIT[i]));
        }
        // drivers come two to a team, in team order: that is the line-up
        for (int j = 0; j < drivers.size() && j / 2 < teams.size() && j / 2 < GameDataset.TEAMS; j++)
        {
            Map<?, ?> driver = map(drivers.get(j));
            int number = number(driver.get("Number"));
            d.put("team " + (j / 2 + 1), j % 2 == 0 ? "first car" : "second car", number);
            if (number < 1 || number > GameDataset.CARS)
                continue;
            String s = "driver " + number;
            put(d, s, "name", driver.get("Name"));
            put(d, s, "qualifying skill", driver.get("QualifyingGripLevel"));
            put(d, s, "race skill", driver.get("RaceGripLevel"));
            Map<?, ?> helmet = map(driver.get("HelmetColors"));
            for (int i = 0; i < ARG_HELMET.length; i++)
                put(d, s, "helmet " + part(GameLiveries.HELMET_PARTS, GameLiveries.HELMET_BYTES, ARG_HELMET_BYTES[i]), helmet.get(ARG_HELMET[i]));
            Object image = driver.get("MenuImage");
            if (image instanceof String && !((String) image).isEmpty())
            {
                byte[] px;
                try { px = java.util.Base64.getDecoder().decode((String) image); } catch (IllegalArgumentException e) { px = null; }
                if (px != null && px.length == 48 * 48 && helmetPalette != null)
                    d.putFile(String.format("helmets/car%02d.png", number), GameDataset.png(px, 48, 48, helmetPalette));
                else
                    pictureLeftOut = true;
            }
        }
        if (pictureLeftOut)
            d.addNote("Some of ArgEditor's menu helmet pictures could not be read and were left out.");
        Map<?, ?> points = map(m.get("PointsSystem"));
        for (int i = 0; i < 26; i++)
        {
            Object v = points.get("PointsFor" + GameDataset.place(i));
            if (v != null)
                d.put("points", GameDataset.place(i), number(v));
        }
        return d;
    }

    /** The editor's name (lower case) for the colour at a byte of the 16. */
    private static String part(String[] parts, int[] bytes, int b)
    {
        for (int p = 0; p < bytes.length; p++)
            if (bytes[p] == b)
                return parts[p].toLowerCase();
        throw new IllegalArgumentException("byte " + b);
    }

    private static List<?> list(Object o)
    {
        return o instanceof List ? (List<?>) o : new ArrayList<Object>();
    }

    private static Map<?, ?> map(Object o)
    {
        return o instanceof Map ? (Map<?, ?>) o : new LinkedHashMap<Object, Object>();
    }

    private static int number(Object o)
    {
        return o instanceof Number ? ((Number) o).intValue() : -1;
    }

    private static void put(Dataset d, String section, String key, Object v)
    {
        if (v instanceof Number)
            d.put(section, key, ((Number) v).intValue());
        else if (v instanceof String)
            d.put(section, key, (String) v);
    }

    // --- a whole HELMETS.DAT, as shared with some car sets ----------------------

    private static void addHelmetsFile(Dataset d, byte[] file, int[] palette) throws IOException
    {
        if (palette == null)
        {
            d.addNote("The car set's menu helmets were left out.");
            return;
        }
        MediaContainer c;
        try
        {
            c = MediaContainer.load(file);
        }
        catch (IOException e)
        {
            d.addNote("The car set's HELMETS.DAT could not be read, so its menu helmets were left out.");
            return;
        }
        int n = 0;
        for (int i = 0; i < c.getItemCount() && n < GameDataset.CARS; i++)
            if (c.getItem(i).type == MediaContainer.SMALL_PICTURE && c.getItem(i).width == 48 && c.getItem(i).height == 48)
            {
                n++;
                String path = String.format("helmets/car%02d.png", n);
                if (d.getFile(path) == null)      // a picture in the car set file itself comes first
                    d.putFile(path, GameDataset.png(c.getSmallPicture(i), 48, 48, palette));
            }
    }

    // --- JSON, as much as these files need ---------------------------------------

    static final class Json
    {
        private final String s;
        private int i;

        Json(String s)
        {
            this.s = s;
        }

        Object value() throws IOException
        {
            space();
            if (i >= s.length())
                throw new IOException("unexpected end");
            char c = s.charAt(i);
            if (c == '{')
            {
                Map<String, Object> m = new LinkedHashMap<String, Object>();
                i++;
                space();
                if (peek('}')) return m;
                do
                {
                    space();
                    String k = string();
                    space();
                    expect(':');
                    m.put(k, value());
                    space();
                }
                while (peek(','));
                expect('}');
                return m;
            }
            if (c == '[')
            {
                List<Object> l = new ArrayList<Object>();
                i++;
                space();
                if (peek(']')) return l;
                do
                {
                    l.add(value());
                    space();
                }
                while (peek(','));
                expect(']');
                return l;
            }
            if (c == '"')
                return string();
            if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
            if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
            if (s.startsWith("null", i)) { i += 4; return null; }
            int start = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0)
                i++;
            if (start == i)
                throw new IOException("unexpected '" + c + "'");
            String n = s.substring(start, i);
            try
            {
                return n.contains(".") || n.contains("e") || n.contains("E") ? (Object) Double.valueOf(n) : (Object) Long.valueOf(n);
            }
            catch (NumberFormatException e)
            {
                throw new IOException("bad number " + n);
            }
        }

        private String string() throws IOException
        {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (i < s.length())
            {
                char c = s.charAt(i++);
                if (c == '"')
                    return sb.toString();
                if (c != '\\')
                {
                    sb.append(c);
                    continue;
                }
                if (i >= s.length())
                    break;
                char e = s.charAt(i++);
                switch (e)
                {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'u':
                        if (i + 4 > s.length()) throw new IOException("bad escape");
                        sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        i += 4;
                        break;
                    default: sb.append(e);
                }
            }
            throw new IOException("unfinished text");
        }

        private void space()
        {
            while (i < s.length() && Character.isWhitespace(s.charAt(i)))
                i++;
        }

        private boolean peek(char c)
        {
            space();
            if (i < s.length() && s.charAt(i) == c)
            {
                i++;
                return true;
            }
            return false;
        }

        private void expect(char c) throws IOException
        {
            space();
            if (i >= s.length() || s.charAt(i) != c)
                throw new IOException("expected '" + c + "'");
            i++;
        }
    }
}
