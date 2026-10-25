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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /** F1ED's Damage1-4, as its own labels number them. */
    private static final String[] F1ED_DAMAGE = { "retire after hitting a wall", "retire after hitting a car",
        "wing breaks after hitting a wall", "wing breaks after hitting a car" };

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
            // Damage1-4 as F1ED's own labels number them (its form's DamageLabel index 1-4)
            for (int i = 0; i < F1ED_DAMAGE.length; i++)
                copy(ini, "damage" + (i + 1), d, F1ED_DAMAGE[i]);
            copy(ini, "yellowflag", d, "yellow flags after seconds");
            copy(ini, "removecrashedcar", d, "retired cars removed after seconds");
            copy(ini, "wetrace", d, "chance of rain");
            if (ini.get("f1ed", "qtyres") != null)
                d.put("rules", "unlimited qualifying tyres", Dataset.yesNo("1".equals(ini.get("f1ed", "qtyres"))));
            if (ini.get("f1ed", "wetracephoenix") != null)
                d.put("rules", "rain at dry circuits", Dataset.yesNo("1".equals(ini.get("f1ed", "wetracephoenix"))));
            if (ini.get("f1ed", "fastfade") != null)       // the same patch as the Game Settings page's faster menus
                d.put("rules", "faster menus", Dataset.yesNo("1".equals(ini.get("f1ed", "fastfade"))));
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

    // --- writing F1ED and ArgEditor car sets ---------------------------------------

    /** What an F1ED car set holds; all of it is always written. */
    public static final Set<DatasetGroup> F1ED_GROUPS =
        EnumSet.of(DatasetGroup.TEAMS, DatasetGroup.DRIVERS, DatasetGroup.COLOURS, DatasetGroup.RULES);
    /** What an ArgEditor car set always holds; menu helmet pictures may be added. */
    public static final Set<DatasetGroup> ARGEDITOR_GROUPS =
        EnumSet.of(DatasetGroup.TEAMS, DatasetGroup.DRIVERS, DatasetGroup.COLOURS, DatasetGroup.POINTS);
    /** ArgEditor's teams; two drivers each. */
    public static final int ARGEDITOR_TEAMS = 18;

    /**
     * A dataset as an F1ED car set (.1ED). The colours a dataset does not
     * name (bytes 0, 9 and 10 of a car's) are taken from base, the game's own
     * colour sets, so F1ED writes them back unchanged; base may be null.
     * Notes on anything left out are added to notes.
     */
    public static byte[] writeF1Ed(Dataset d, GameLiveries base, List<String> notes)
    {
        StringBuilder sb = new StringBuilder();
        line(sb, "[Drivers]");
        for (int c = 1; c <= GameDataset.CARS; c++)
        {
            String s = "driver " + c;
            line(sb, c + "=" + f1EdName(d.get(s, "name")) + "," + num(d, s, "qualifying skill", 0) + ","
                 + num(d, s, "race skill", 0) + ",0");
        }
        line(sb, "[Teams]");
        for (int t = 1; t <= GameDataset.TEAMS; t++)
        {
            String s = "team " + t;
            line(sb, t + "=" + f1EdName(d.get(s, "name")) + "," + f1EdName(d.get(s, "engine")) + "," + num(d, s, "power", 0) + ","
                 + num(d, s, "first car", 0) + "," + num(d, s, "second car", 0));
        }
        line(sb, "[CarColours]");
        for (int t = 1; t <= GameDataset.TEAMS; t++)
            line(sb, t + "=" + colourLine(d, "team " + t, "car ", GameLiveries.CAR_PARTS, GameLiveries.CAR_BYTES,
                                          base == null ? null : carSet(base, t - 1)));
        line(sb, "[PitColours]");
        for (int t = 1; t <= GameDataset.TEAMS; t++)
            line(sb, t + "=" + colourLine(d, "team " + t, "pit crew ", GameLiveries.PIT_CREW_PARTS, GameLiveries.PIT_CREW_BYTES,
                                          base == null ? null : pitSet(base, t - 1)));
        line(sb, "[HelmetColours]");
        for (int c = 1; c <= GameDataset.CARS; c++)
            line(sb, c + "=" + colourLine(d, "driver " + c, "helmet ", GameLiveries.HELMET_PARTS, GameLiveries.HELMET_BYTES,
                                          base == null ? null : helmetSet(base, c - 1)));
        line(sb, "[F1Ed]");
        Integer v = d.getInt("rules", "computer grip level");
        if (v != null) line(sb, "AIGrip=" + v);
        v = d.getInt("rules", "player power");
        if (v != null)
        {
            line(sb, "HumanBHP=" + v);
            line(sb, "BHPLegalise=" + (v == 716 ? 1 : 0));      // F1ED's lock at the game's own 716
        }
        for (int i = 0; i < F1ED_DAMAGE.length; i++)
        {
            v = d.getInt("rules", F1ED_DAMAGE[i]);
            if (v != null) line(sb, "Damage" + (i + 1) + "=" + v);
        }
        v = d.getInt("rules", "yellow flags after seconds");
        if (v != null) line(sb, "YellowFlag=" + v);
        v = d.getInt("rules", "retired cars removed after seconds");
        if (v != null) line(sb, "RemoveCrashedCar=" + v);
        Boolean b = d.getBoolean("rules", "faster menus");
        if (b != null) line(sb, "FastFade=" + (b ? 1 : 0));
        b = d.getBoolean("rules", "unlimited qualifying tyres");
        if (b != null) line(sb, "QTyres=" + (b ? 1 : 0));
        b = d.getBoolean("rules", "rain at dry circuits");
        if (b != null) line(sb, "WetRacePhoenix=" + (b ? 1 : 0));
        v = d.getInt("rules", "chance of rain");
        if (v != null) line(sb, "WetRace=" + v);
        if (d.hasSection("points"))
            notes.add("An F1ED car set has no points table, so the points were left out.");
        return sb.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    /**
     * A dataset as an ArgEditor car set (.arged, FileVersion 3). ArgEditor
     * keeps 18 teams and only the drivers in them, so anything else is left
     * out with a note. helmetPalette (may be null) turns the menu helmet
     * pictures back into the game's colours.
     */
    public static byte[] writeArgEditor(Dataset d, int[] helmetPalette, List<String> notes)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{\r\n  \"FileVersion\": 3,\r\n  \"Teams\": [");
        boolean[] seated = new boolean[GameDataset.CARS + 1];
        int[][] lineUp = new int[ARGEDITOR_TEAMS][2];
        for (int t = 0; t < ARGEDITOR_TEAMS; t++)
        {
            String s = "team " + (t + 1);
            sb.append(t == 0 ? "\r\n" : ",\r\n").append("    {\r\n");
            sb.append("      \"Name\": ").append(jsonString(d.get(s, "name"))).append(",\r\n");
            sb.append("      \"Engine\": ").append(jsonString(d.get(s, "engine"))).append(",\r\n");
            sb.append("      \"Horsepower\": ").append(num(d, s, "power", 0)).append(",\r\n");
            sb.append("      \"CarColors\": {");
            for (int i = 0; i < ARG_CAR.length; i++)
                field(sb, i, 8, ARG_CAR[i], num(d, s, "car " + part(GameLiveries.CAR_PARTS, GameLiveries.CAR_BYTES, ARG_CAR_BYTES[i]), 0));
            sb.append("\r\n      },\r\n      \"PitCrewColors\": {");
            for (int i = 0; i < ARG_PIT.length; i++)
                field(sb, i, 8, ARG_PIT[i], num(d, s, "pit crew " + part(GameLiveries.PIT_CREW_PARTS, GameLiveries.PIT_CREW_BYTES, ARG_PIT_BYTES[i]), 0));
            sb.append("\r\n      }\r\n    }");
            for (int seat = 0; seat < 2; seat++)
            {
                int c = num(d, s, seat == 0 ? "first car" : "second car", 0);
                if (c < 1 || c > GameDataset.CARS || seated[c])
                    c = 0;
                lineUp[t][seat] = c;
                seated[c] = c > 0;
            }
        }
        sb.append("\r\n  ],\r\n  \"Drivers\": [");
        boolean pictureLeftOut = false;
        for (int j = 0; j < 2 * ARGEDITOR_TEAMS; j++)
        {
            int c = lineUp[j / 2][j % 2];
            String s = "driver " + c;
            sb.append(j == 0 ? "\r\n" : ",\r\n").append("    {\r\n");
            sb.append("      \"Number\": ").append(c).append(",\r\n");
            sb.append("      \"Name\": ").append(jsonString(c == 0 ? "" : d.get(s, "name"))).append(",\r\n");
            sb.append("      \"RaceGripLevel\": ").append(c == 0 ? 1 : num(d, s, "race skill", 1)).append(",\r\n");
            sb.append("      \"QualifyingGripLevel\": ").append(c == 0 ? 1 : num(d, s, "qualifying skill", 1)).append(",\r\n");
            sb.append("      \"HelmetColors\": {");
            for (int i = 0; i < ARG_HELMET.length; i++)
            {
                // an empty seat gets ArgEditor's own plain helmet
                int plain = ARG_HELMET_BYTES[i] == 0 ? 0 : ARG_HELMET_BYTES[i] == 6 ? 6 : 8;
                int v = c == 0 ? plain : num(d, s, "helmet " + part(GameLiveries.HELMET_PARTS, GameLiveries.HELMET_BYTES, ARG_HELMET_BYTES[i]), plain);
                field(sb, i, 8, ARG_HELMET[i], v);
            }
            sb.append("\r\n      },\r\n      \"MenuImage\": ");
            String image = "";
            byte[] png = c == 0 ? null : d.getFile(String.format("helmets/car%02d.png", c));
            if (png != null)
            {
                byte[] px = helmetPalette == null ? null : GameDataset.pictureIndices(png, 48, 48, helmetPalette);
                if (px != null)
                    image = java.util.Base64.getEncoder().encodeToString(px);
                else
                    pictureLeftOut = true;
            }
            sb.append(jsonString(image)).append("\r\n    }");
        }
        sb.append("\r\n  ],\r\n  \"PointsSystem\": {");
        for (int i = 0; i < 26; i++)
            field(sb, i, 4, "PointsFor" + GameDataset.place(i), num(d, "points", GameDataset.place(i), 0));
        sb.append("\r\n  }\r\n}");

        boolean extraTeams = false;
        for (int t = ARGEDITOR_TEAMS + 1; t <= GameDataset.TEAMS; t++)
            extraTeams |= num(d, "team " + t, "first car", 0) > 0 || num(d, "team " + t, "second car", 0) > 0;
        if (extraTeams)
            notes.add("ArgEditor holds 18 teams, so teams 19 and 20 and their drivers were left out.");
        int unseated = 0;
        for (int c = 1; c <= GameDataset.CARS; c++)
            if (!seated[c] && d.get("driver " + c, "name") != null && !d.get("driver " + c, "name").trim().isEmpty())
                unseated++;
        if (unseated > 0)
            notes.add("ArgEditor keeps only the drivers in the teams, so " + unseated + " driver" + (unseated == 1 ? "" : "s")
                      + " without a seat " + (unseated == 1 ? "was" : "were") + " left out.");
        if (pictureLeftOut)
            notes.add("Some menu helmet pictures could not be read and were left out.");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void line(StringBuilder sb, String s)
    {
        sb.append(s).append("\r\n");
    }

    private static int num(Dataset d, String section, String key, int otherwise)
    {
        Integer v = d.getInt(section, key);
        return v == null ? otherwise : v;
    }

    /** A name for an F1ED line, which separates its fields with commas. */
    private static String f1EdName(String s)
    {
        return s == null ? "" : s.replace(',', ' ');
    }

    private static int[] carSet(GameLiveries l, int team)
    {
        int[] v = new int[16];
        for (int i = 0; i < 16; i++) v[i] = l.getCarSetColour(team, i);
        return v;
    }

    private static int[] pitSet(GameLiveries l, int team)
    {
        int[] v = new int[16];
        for (int i = 0; i < 16; i++) v[i] = l.getPitCrewSetColour(team, i);
        return v;
    }

    private static int[] helmetSet(GameLiveries l, int car)
    {
        int[] v = new int[16];
        for (int i = 0; i < 16; i++) v[i] = l.getHelmetSetColour(car, i);
        return v;
    }

    /** 16 colours in the game's byte order: start (or zeros), with the dataset's named parts put in. */
    private static String colourLine(Dataset d, String section, String prefix, String[] parts, int[] bytes, int[] start)
    {
        int[] v = start == null ? new int[16] : start.clone();
        for (int p = 0; p < parts.length; p++)
        {
            Integer x = d.getInt(section, prefix + parts[p].toLowerCase());
            if (x != null) v[bytes[p]] = x;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++)
            sb.append(i == 0 ? "" : ",").append(v[i]);
        return sb.toString();
    }

    private static void field(StringBuilder sb, int i, int indent, String key, int value)
    {
        sb.append(i == 0 ? "\r\n" : ",\r\n");
        for (int k = 0; k < indent; k++)
            sb.append(' ');
        sb.append('"').append(key).append("\": ").append(value);
    }

    private static String jsonString(String s)
    {
        if (s == null)
            s = "";
        StringBuilder sb = new StringBuilder("\"");
        for (char ch : s.toCharArray())
        {
            if (ch == '"' || ch == '\\')
                sb.append('\\').append(ch);
            else if (ch < 0x20)
                sb.append(String.format("\\u%04x", (int) ch));
            else
                sb.append(ch);
        }
        return sb.append('"').toString();
    }

    // --- ArgEditor's single colour files: .car (a team's car), .hel (a helmet) -------

    /** Which of the game's 16 car colours each byte of a .car file holds (read from files ArgEditor saved). */
    private static final int[] CAR_FILE_ORDER = { 0, 7, 3, 9, 4, 6, 15, 10, 5, 1, 13, 14, 12, 2, 8, 11 };
    private static final String CAR_FILE_MARK = "TKF1", HELMET_FILE_MARK = "TKHE";

    /** A team's 16 car colours (the game's order) as a .car file. */
    public static byte[] writeCarFile(int[] colours)
    {
        byte[] out = mark(CAR_FILE_MARK);
        for (int i = 0; i < 16; i++)
            out[4 + i] = (byte) colours[CAR_FILE_ORDER[i]];
        return out;
    }

    /** A .car file's 16 car colours in the game's order, or null if it is not one. */
    public static int[] readCarFile(byte[] file)
    {
        if (!marked(file, CAR_FILE_MARK))
            return null;
        int[] out = new int[16];
        for (int i = 0; i < 16; i++)
            out[CAR_FILE_ORDER[i]] = file[4 + i] & 0xFF;
        return out;
    }

    /** A driver's 16 helmet colours (the game's order, which a .hel file keeps) as a .hel file. */
    public static byte[] writeHelmetFile(int[] colours)
    {
        byte[] out = mark(HELMET_FILE_MARK);
        for (int i = 0; i < 16; i++)
            out[4 + i] = (byte) colours[i];
        return out;
    }

    /** A .hel file's 16 helmet colours, or null if it is not one. */
    public static int[] readHelmetFile(byte[] file)
    {
        if (!marked(file, HELMET_FILE_MARK))
            return null;
        int[] out = new int[16];
        for (int i = 0; i < 16; i++)
            out[i] = file[4 + i] & 0xFF;
        return out;
    }

    private static byte[] mark(String m)
    {
        byte[] out = new byte[20];
        System.arraycopy(m.getBytes(StandardCharsets.ISO_8859_1), 0, out, 0, 4);
        return out;
    }

    private static boolean marked(byte[] file, String m)
    {
        return file != null && file.length == 20 && new String(file, 0, 4, StandardCharsets.ISO_8859_1).equals(m);
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
