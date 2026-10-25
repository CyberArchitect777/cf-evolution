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

import cfevolution.data.GameFolder;
import cfevolution.data.executable.Executable;
import cfevolution.data.executable.GameDrivers;
import cfevolution.data.executable.GameLiveries;
import cfevolution.data.executable.GameOptions;
import cfevolution.data.executable.GameSettings;
import cfevolution.data.executable.GameTables;
import cfevolution.data.executable.GameTeams;
import cfevolution.data.gamefiles.MediaContainer;
import cfevolution.data.gamefiles.NameFile;
import cfevolution.data.gamefiles.PreferencesFile;
import cfevolution.data.gamefiles.SoundEffectsFile;
import cfevolution.data.track.Track;
import cfevolution.gui.gamefiles.ColourReducer;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;

/**
 * Exports a game folder to a {@link Dataset} and imports one into it. Every
 * value goes through the same classes the editor's own pages use, keeps to
 * the same ranges, and is only written if its group is ticked; anything the
 * dataset leaves out is left as it is. Importing copies each file it changes
 * into a dated backup folder first.
 *
 * @author barrie
 */
public class GameDataset
{
    public static final String BACKUP_FOLDER = "CFE-BACKUP";

    public static final int TEAMS = 20, CARS = 40;
    private static final int TEAM_NAME = 12, DRIVER_NAME = 23;

    /** Menu helmets draw in the driver selection background's colours, flags in the title screen's. */
    private static final int DRIVER_SELECTION_PICTURE = 5, TITLE_PICTURE = 3;
    /** Colours 0-31 belong to the menus and are never changed by a picture. */
    private static final int MENU_COLOURS = 32;

    private static final String[] AIDS = { "automatic brakes", "automatic gears", "spin recovery",
                                           "indestructible", "ideal line", "suggested gear" };
    private static final int AIDS_AT = 1144, KPH_AT = 1020, TEXTURES_AT = 1022;

    private final GameFolder game;
    private final Executable exe;
    private PreferencesFile prefs;
    private SoundEffectsFile sounds;
    private final Map<String, MediaContainer> pictures = new LinkedHashMap<String, MediaContainer>();

    /** Reads the game folder's files as they are on disk. */
    public GameDataset(GameFolder game) throws IOException
    {
        this.game = game;
        exe = new Executable(game.getExecutable());
        if (exe.loadData() != 0)
            throw new IOException("GP.EXE could not be read");
        File p = game.getPreferences();
        if (p != null)
            try { prefs = PreferencesFile.load(p); } catch (IOException e) { prefs = null; }
        File s = game.getSoundEffects();
        if (s != null)
            try { sounds = SoundEffectsFile.load(s); } catch (IOException e) { sounds = null; }
        for (String name : GameFolder.PICTURE_FILES)
        {
            File f = game.getPictureFile(name);
            if (f != null)
                try
                {
                    pictures.put(name, MediaContainer.load(f));
                }
                catch (IOException e) { /* left out */ }
        }
    }

    public Executable getExecutable()
    {
        return exe;
    }

    /** Which groups this game folder can give (or take). */
    public Set<DatasetGroup> available()
    {
        Set<DatasetGroup> out = EnumSet.range(DatasetGroup.TEAMS, DatasetGroup.PHYSICS);
        if (prefs != null) out.add(DatasetGroup.OPTIONS);
        if (sounds != null) out.add(DatasetGroup.SOUNDS);
        if (pictures.containsKey("HELMETS.DAT")) out.add(DatasetGroup.HELMET_PICTURES);
        if (pictures.containsKey("FLAGS.DAT")) out.add(DatasetGroup.FLAG_PICTURES);
        if (pictures.containsKey("BACKDROP.DAT")) out.add(DatasetGroup.BACKDROP_PICTURES);
        if (pictures.containsKey("TRACKPIX.DAT")) out.add(DatasetGroup.CIRCUIT_PICTURES);
        for (int slot = 1; slot <= GameFolder.CIRCUITS; slot++)
            if (game.getTrack(slot) != null)
            {
                out.add(DatasetGroup.CIRCUITS);
                break;
            }
        return out;
    }

    /** Which groups a dataset holds. */
    public static Set<DatasetGroup> groupsIn(Dataset d)
    {
        Set<DatasetGroup> out = EnumSet.noneOf(DatasetGroup.class);
        for (String s : d.sectionNames())
        {
            Map<String, String> keys = d.section(s);
            if (s.startsWith("team ") || s.startsWith("driver "))
                for (String k : keys.keySet())
                {
                    if (k.startsWith("car ") || k.startsWith("pit crew ") || k.startsWith("helmet "))
                        out.add(DatasetGroup.COLOURS);
                    else
                        out.add(s.startsWith("team ") ? DatasetGroup.TEAMS : DatasetGroup.DRIVERS);
                }
            else if (s.equals("points")) out.add(DatasetGroup.POINTS);
            else if (s.equals("rules")) out.add(DatasetGroup.RULES);
            else if (s.equals("pit lane") || s.equals("car setups") || s.equals("circuit setups") || s.equals("lap records"))
                out.add(DatasetGroup.TABLES);
            else if (s.equals("car performance")) out.add(DatasetGroup.PHYSICS);
            else if (s.equals("options")) out.add(DatasetGroup.OPTIONS);
            else if (s.equals("sounds")) out.add(DatasetGroup.SOUNDS);
        }
        for (String f : d.filePaths())
        {
            if (f.startsWith("helmets/")) out.add(DatasetGroup.HELMET_PICTURES);
            else if (f.startsWith("flags/")) out.add(DatasetGroup.FLAG_PICTURES);
            else if (f.startsWith("backdrops/")) out.add(DatasetGroup.BACKDROP_PICTURES);
            else if (f.startsWith("circuits/")) out.add(DatasetGroup.CIRCUIT_PICTURES);
            else if (f.startsWith("tracks/")) out.add(DatasetGroup.CIRCUITS);
        }
        return out;
    }

    // ======================================================================
    // Export
    // ======================================================================

    public Dataset export(Set<DatasetGroup> groups, String title)
    {
        Dataset d = new Dataset();
        d.put("dataset", "format", Dataset.FORMAT);
        d.put("dataset", "made by", "CF Evolution");
        if (title != null && !title.trim().isEmpty())
            d.put("dataset", "title", title.trim());
        d.put("dataset", "game", exe.returnGameVersionString());

        GameTeams teams = exe.getGameTeams();
        GameDrivers drivers = exe.getGameDrivers();
        GameLiveries liveries = exe.getGameLiveries();
        for (int t = 0; t < TEAMS; t++)
        {
            String s = "team " + (t + 1);
            if (groups.contains(DatasetGroup.TEAMS))
            {
                d.put(s, "name", teams.getTeamName(t));
                d.put(s, "engine", teams.getEngineName(t));
                d.put(s, "power", teams.getBHPSetting(t));
                d.put(s, "first car", teams.getTeamCar(t, 0));
                d.put(s, "second car", teams.getTeamCar(t, 1));
            }
            if (groups.contains(DatasetGroup.COLOURS))
            {
                for (int p = 0; p < GameLiveries.CAR_PARTS.length; p++)
                    d.put(s, "car " + GameLiveries.CAR_PARTS[p].toLowerCase(), liveries.getCarColour(t, p));
                for (int p = 0; p < GameLiveries.PIT_CREW_PARTS.length; p++)
                    d.put(s, "pit crew " + GameLiveries.PIT_CREW_PARTS[p].toLowerCase(), liveries.getPitCrewColour(t, p));
            }
        }
        for (int c = 0; c < CARS; c++)
        {
            String s = "driver " + (c + 1);
            if (groups.contains(DatasetGroup.DRIVERS))
            {
                d.put(s, "name", drivers.getDriverName(c));
                d.put(s, "qualifying skill", drivers.getQualifyingGrip(c) & 0xFF);
                d.put(s, "race skill", drivers.getRaceGrip(c) & 0xFF);
            }
            if (groups.contains(DatasetGroup.COLOURS))
                for (int p = 0; p < GameLiveries.HELMET_PARTS.length; p++)
                    d.put(s, "helmet " + GameLiveries.HELMET_PARTS[p].toLowerCase(), liveries.getHelmetColour(c, p));
        }
        if (groups.contains(DatasetGroup.POINTS))
            for (int i = 0; i < 26; i++)
                d.put("points", place(i), exe.getGamePoints().getPointValue(i) & 0xFF);
        if (groups.contains(DatasetGroup.RULES))
            exportRules(d);
        if (groups.contains(DatasetGroup.TABLES))
            exportTables(d);
        if (groups.contains(DatasetGroup.PHYSICS))
            for (Map.Entry<String, GameTables.Table> e : physicsTables().entrySet())
            {
                int[] w = new int[e.getValue().words()];
                for (int i = 0; i < w.length; i++)
                    w[i] = e.getValue().word(i);
                d.put("car performance", e.getKey(), Dataset.join(w));
            }
        if (groups.contains(DatasetGroup.OPTIONS) && prefs != null)
            exportOptions(d);
        if (groups.contains(DatasetGroup.SOUNDS) && sounds != null)
            for (SoundSetting s : SOUND_SETTINGS)
                d.put("sounds", s.key, s.get(sounds));
        if (groups.contains(DatasetGroup.SOUNDS) && sounds != null)
            for (int r : INSTRUMENTS)
                d.put("sounds", "instrument " + r, Dataset.join(instrument(r)));
        try
        {
            if (groups.contains(DatasetGroup.HELMET_PICTURES))
                exportSmall(d, "HELMETS.DAT", "helmets/car", DRIVER_SELECTION_PICTURE);
            if (groups.contains(DatasetGroup.FLAG_PICTURES))
                exportSmall(d, "FLAGS.DAT", "flags/flag", TITLE_PICTURE);
            if (groups.contains(DatasetGroup.BACKDROP_PICTURES))
                exportFull(d, "BACKDROP.DAT", "backdrops/backdrop");
            if (groups.contains(DatasetGroup.CIRCUIT_PICTURES))
                exportFull(d, "TRACKPIX.DAT", "circuits/circuit");
            if (groups.contains(DatasetGroup.CIRCUITS))
                for (int slot = 1; slot <= GameFolder.CIRCUITS; slot++)
                {
                    File t = game.getTrack(slot);
                    if (t != null)
                        d.putFile("tracks/" + GameFolder.trackFileName(slot), Files.readAllBytes(t.toPath()));
                }
        }
        catch (IOException e)
        {
            d.addNote("Some pictures or circuits could not be read.");
        }
        return d;
    }

    static String place(int i)
    {
        int n = i + 1;
        if (n >= 4 && n <= 20) return n + "th";
        switch (n % 10)
        {
            case 1: return n + "st";
            case 2: return n + "nd";
            case 3: return n + "rd";
            default: return n + "th";
        }
    }

    // --- rules --------------------------------------------------------------

    private interface Rule
    {
        int get(GameSettings s);
        void set(GameSettings s, int v);
    }

    private static final class RuleSetting
    {
        final String key;
        final int min, max;
        final Rule rule;
        RuleSetting(String key, int min, int max, Rule rule) { this.key = key; this.min = min; this.max = max; this.rule = rule; }
    }

    /** The Game Settings page's numbers, with its ranges. */
    private static final RuleSetting[] RULES = {
        new RuleSetting("player power", 1, 1460, new Rule() {
            public int get(GameSettings s) { return s.getPlayerPower(); } public void set(GameSettings s, int v) { s.setPlayerPower(v); } }),
        new RuleSetting("computer grip level", 1, 100, new Rule() {
            public int get(GameSettings s) { return s.getAIGripLevel(); } public void set(GameSettings s, int v) { s.setAIGripLevel(v); } }),
        new RuleSetting("team power measured against", 100, 2000, new Rule() {
            public int get(GameSettings s) { return s.getTeamPowerReference(); } public void set(GameSettings s, int v) { s.setTeamPowerReference(v); } }),
        new RuleSetting("pit stops in races of at least", 1, 100, new Rule() {
            public int get(GameSettings s) { return s.getPitStopRaceDistance(); } public void set(GameSettings s, int v) { s.setPitStopRaceDistance(v); } }),
        new RuleSetting("most cars in the pits", 1, 26, new Rule() {
            public int get(GameSettings s) { return s.getMaxCarsPitting(); } public void set(GameSettings s, int v) { s.setMaxCarsPitting(v); } }),
        new RuleSetting("shortest race laps", 1, 50, new Rule() {
            public int get(GameSettings s) { return s.getMinimumRaceLaps(); } public void set(GameSettings s, int v) { s.setMinimumRaceLaps(v); } }),
        new RuleSetting("cars out in practice", 1, 26, new Rule() {
            public int get(GameSettings s) { return s.getMaxPracticeRunners(); } public void set(GameSettings s, int v) { s.setMaxPracticeRunners(v); } }),
        new RuleSetting("best results counted", 0, 16, new Rule() {
            public int get(GameSettings s) { return s.getBestResultsCounted(); } public void set(GameSettings s, int v) { s.setBestResultsCounted(v); } }),
        new RuleSetting("chance of rain", 0, 100, new Rule() {
            public int get(GameSettings s) { return s.getRainChance(); } public void set(GameSettings s, int v) { s.setRainChance(v); } }),
        new RuleSetting("retire after hitting a wall", 0, 32767, new Rule() {
            public int get(GameSettings s) { return s.getRetireAfterWall(); } public void set(GameSettings s, int v) { s.setRetireAfterWall(v); } }),
        new RuleSetting("retire after hitting a car", 0, 32767, new Rule() {
            public int get(GameSettings s) { return s.getRetireAfterCar(); } public void set(GameSettings s, int v) { s.setRetireAfterCar(v); } }),
        new RuleSetting("wing breaks after hitting a wall", 0, 32767, new Rule() {
            public int get(GameSettings s) { return s.getDamageAfterWall(); } public void set(GameSettings s, int v) { s.setDamageAfterWall(v); } }),
        new RuleSetting("wing breaks after hitting a car", 0, 32767, new Rule() {
            public int get(GameSettings s) { return s.getDamageAfterCar(); } public void set(GameSettings s, int v) { s.setDamageAfterCar(v); } }),
        new RuleSetting("yellow flags after seconds", 1, 60, new Rule() {
            public int get(GameSettings s) { return s.getYellowFlagSeconds(); } public void set(GameSettings s, int v) { s.setYellowFlagSeconds(v); } }),
        new RuleSetting("retired cars removed after seconds", 1, 60, new Rule() {
            public int get(GameSettings s) { return s.getRetiredRemovedSeconds(); } public void set(GameSettings s, int v) { s.setRetiredRemovedSeconds(v); } }),
    };

    private void exportRules(Dataset d)
    {
        GameSettings s = exe.getGameSettings();
        GameOptions o = exe.getGameOptions();
        for (RuleSetting r : RULES)
        {
            int v = r.rule.get(s);
            if (v >= r.min && v <= r.max)      // the grip level is 0 when another tool changed that code
                d.put("rules", r.key, v);
        }
        d.put("rules", "rain at dry circuits", Dataset.yesNo(s.getRainEverywhere()));
        d.put("rules", "unlimited qualifying tyres", Dataset.yesNo(s.getQualifyingTyres()));
        d.put("rules", "faster menus", Dataset.yesNo(o.getFastFade()));
        d.put("rules", "skip language screen", Dataset.yesNo(o.getLanguageSelection()));
    }

    // --- tables ---------------------------------------------------------------

    private void exportTables(Dataset d)
    {
        GameTables t = exe.getGameTables();
        int[] order = new int[t.pitOrder.words()];
        for (int i = 0; i < order.length; i++)
            order[i] = t.pitOrder.word(i);
        d.put("pit lane", "team order", Dataset.join(order));
        for (int c = 0; c < CARS; c++)
            d.put("car setups", "car " + (c + 1), Dataset.join(bytes(t.carSetups, c * GameTables.SETUP_SIZE, GameTables.SETUP_SIZE)));
        for (int c = 0; c < 32; c++)
            d.put("circuit setups", "circuit " + (c % 16 + 1) + (c < 16 ? "" : " race"),
                  Dataset.join(bytes(t.circuitSetups, c * GameTables.SETUP_SIZE, GameTables.SETUP_SIZE)));
        for (int c = 0; c < 16; c++)
            for (int race = 0; race < 2; race++)
            {
                String k = "circuit " + (c + 1) + (race == 1 ? " race" : " qualifying");
                d.put("lap records", k + " driver", t.getRecordDriver(c, race == 1));
                d.put("lap records", k + " team", t.getRecordTeam(c, race == 1));
                d.put("lap records", k + " time", lapTime(t.getRecordTime(c, race == 1)));
                d.put("lap records", k + " date", GameTables.dateOfDay(t.getRecordDay(c, race == 1)).toString());
            }
    }

    private static int[] bytes(GameTables.Table t, int from, int n)
    {
        int[] out = new int[n];
        for (int i = 0; i < n; i++)
            out[i] = t.getByte(from + i);
        return out;
    }

    static String lapTime(int ms)
    {
        return String.format("%d:%02d.%03d", ms / 60000, ms / 1000 % 60, ms % 1000);
    }

    /** m:ss.mmm, or a plain number of milliseconds; -1 if neither. */
    static int parseLapTime(String s)
    {
        if (s == null)
            return -1;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+):(\\d{1,2})\\.(\\d{1,3})").matcher(s.trim());
        if (m.matches())
        {
            String frac = (m.group(3) + "00").substring(0, 3);
            return Integer.parseInt(m.group(1)) * 60000 + Integer.parseInt(m.group(2)) * 1000 + Integer.parseInt(frac);
        }
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return -1; }
    }

    private Map<String, GameTables.Table> physicsTables()
    {
        GameTables t = exe.getGameTables();
        Map<String, GameTables.Table> m = new LinkedHashMap<String, GameTables.Table>();
        m.put("torque race", t.torqueRace);
        m.put("torque qualifying", t.torqueQualifying);
        m.put("tyre grip dry", t.tyreGripDry);
        m.put("tyre grip wet", t.tyreGripWet);
        m.put("minimum grip", t.minimumGrip);
        m.put("car weight", t.carWeight);
        m.put("fuel", t.fuel);
        m.put("wing drag", t.wingDrag);
        m.put("wing downforce", t.wingDownforce);
        m.put("downforce factor", t.downforceFactor);
        m.put("engine braking", t.engineBraking);
        m.put("rev limits", t.revLimits);
        m.put("wet race power", t.wetRacePower);
        m.put("opposition power deduction", t.oppPowerDeduction);
        m.put("opposition rank power", t.oppRankPower);
        m.put("opposition oversteer low", t.oppOversteerLow);
        m.put("opposition oversteer high", t.oppOversteerHigh);
        m.put("opposition corner burst", t.oppCornerBurst);
        m.put("opposition variation low", t.oppVariationLow);
        m.put("opposition variation high", t.oppVariationHigh);
        m.put("opposition wing variation", t.oppWingVariation);
        m.put("corner speed", t.globalCornerSpeed);
        return m;
    }

    // --- options --------------------------------------------------------------

    private void exportOptions(Dataset d)
    {
        d.put("options", "opposition standard", prefs.getWord(PreferencesFile.OPPOSITION_STANDARD));
        d.put("options", "opposition spread", prefs.getWord(PreferencesFile.OPPOSITION_SPREAD));
        d.put("options", "race distance", prefs.getWord(PreferencesFile.RACE_DISTANCE));
        d.put("options", "qualifying minutes", prefs.getWord(PreferencesFile.QUALIFYING_MINUTES));
        d.put("options", "practice minutes", prefs.getWord(PreferencesFile.PRACTICE_MINUTES));
        for (int b = 0; b < AIDS.length; b++)
            d.put("options", "aid " + AIDS[b], Dataset.yesNo((prefs.getWord(AIDS_AT) & (1 << b)) != 0));
        d.put("options", "km/h", Dataset.yesNo((prefs.getWord(KPH_AT) & 0x8000) != 0));
        d.put("options", "textures", Dataset.yesNo((prefs.getByte(TEXTURES_AT) & 0x80) != 0));
        d.put("options", "quick race circuit", prefs.getByte(PreferencesFile.QUICK_RACE_CIRCUIT) + 1);
        d.put("options", "quick race driver", prefs.getByte(PreferencesFile.QUICK_RACE_DRIVER));
        d.put("options", "quick race distance", prefs.getByte(PreferencesFile.QUICK_RACE_DISTANCE));
        d.put("options", "quick race wet", Dataset.yesNo((prefs.getByte(PreferencesFile.QUICK_RACE_WET) & 0x80) != 0));
    }

    // --- sounds ---------------------------------------------------------------

    private static final class SoundSetting
    {
        final String key;
        final int offset, mask, min, max;   // mask 0 = a word
        SoundSetting(String key, int offset, int mask, int min, int max) { this.key = key; this.offset = offset; this.mask = mask; this.min = min; this.max = max; }
        int get(SoundEffectsFile f) { return mask == 0 ? f.getWord(offset) : f.getBits(offset, mask); }
        void set(SoundEffectsFile f, int v) { if (mask == 0) f.setWord(offset, v); else f.setBits(offset, mask, v); }
    }

    /** The Sounds tab's settings, with its ranges. */
    private static final SoundSetting[] SOUND_SETTINGS;
    private static final int[] INSTRUMENTS = { 1, 2, 3, 4, 5, 7, 8, 9, 10, 11, 12 };
    /** The bytes of an instrument record the Sounds tab edits, and which bits of each. */
    private static final int[] INSTRUMENT_BYTES = { 0, 1, 2, 4, 5, 6, 7, 8, 9, 10 };
    private static final int[] INSTRUMENT_MASKS = { 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0x03, 0x03, 0x0F };

    static
    {
        List<SoundSetting> s = new ArrayList<SoundSetting>();
        for (int i = 0; i < 3; i++)
        {
            String t = "engine tone " + (i + 1) + " ";
            s.add(new SoundSetting(t + "pitch", SoundEffectsFile.ENGINE_PITCH[i], 0, 1, 12));
            s.add(new SoundSetting(t + "roughness", SoundEffectsFile.ENGINE_ROUGHNESS[i], 0, 0, 1023));
            s.add(new SoundSetting(t + "loudness off throttle", SoundEffectsFile.ENGINE_LEVEL_OFF[i], 0, 0, 63));
            s.add(new SoundSetting(t + "loudness on throttle", SoundEffectsFile.ENGINE_LEVEL_ON[i], 0, 0, 63));
        }
        s.add(new SoundSetting("tyre pitch", SoundEffectsFile.TYRE_PITCH, 0, 500, 12000));
        for (int i = 0; i < SoundEffectsFile.TYRE_LEVEL.length; i++)
            s.add(new SoundSetting("tyre loudness " + (i + 1), SoundEffectsFile.TYRE_LEVEL[i], 0x3F, 0, 63));
        for (int i = 0; i < 4; i++)
        {
            s.add(new SoundSetting("passing car " + (i + 1) + " pitch", SoundEffectsFile.PASSING_PITCH[i], 0, 500, 12000));
            s.add(new SoundSetting("passing car " + (i + 1) + " greatest drop", SoundEffectsFile.PASSING_MAX_DROP[i], 0xFF, 0, 200));
        }
        s.add(new SoundSetting("passing car drop delay", SoundEffectsFile.PASSING_DROP_DELAY, 0xFF, 2, 200));
        s.add(new SoundSetting("passing car drop speed", SoundEffectsFile.PASSING_DROP_RATE, 0xFF, 0, 50));
        int crash = SoundEffectsFile.record(SoundEffectsFile.CRASH_RECORD);
        s.add(new SoundSetting("crunch length", crash + SoundEffectsFile.NOISE_LENGTH, 0, 1, 500));
        s.add(new SoundSetting("crunch pitch", crash + SoundEffectsFile.NOISE_BASE, 0, 256, 12000));
        s.add(new SoundSetting("crunch pitch spread", crash + SoundEffectsFile.NOISE_SPREAD, 0, 1, 4095));
        SOUND_SETTINGS = s.toArray(new SoundSetting[0]);
    }

    private int[] instrument(int k)
    {
        int r = SoundEffectsFile.record(k);
        int[] out = new int[INSTRUMENT_BYTES.length];
        for (int i = 0; i < out.length; i++)
            out[i] = sounds.getByte(r + INSTRUMENT_BYTES[i]) & INSTRUMENT_MASKS[i];
        return out;
    }

    // --- pictures -------------------------------------------------------------

    /** The colours the game draws the menu helmets in (for reading other editors' helmet pictures), or null. */
    public int[] helmetPalette()
    {
        return smallPalette(DRIVER_SELECTION_PICTURE);
    }

    private int[] smallPalette(int background)
    {
        MediaContainer b = pictures.get("BACKDROP.DAT");
        if (b != null && b.getItemCount() > background * 2)
            return b.getPalette(background * 2);
        return null;
    }

    private void exportSmall(Dataset d, String file, String prefix, int background) throws IOException
    {
        MediaContainer c = pictures.get(file);
        int[] pal = smallPalette(background);
        if (c == null || pal == null)
            return;
        int n = 0;
        for (int i = 0; i < c.getItemCount(); i++)
            if (c.getItem(i).type == MediaContainer.SMALL_PICTURE)
            {
                n++;
                MediaContainer.Item it = c.getItem(i);
                String name = prefix.startsWith("helmets") ? String.format("%02d", n) : String.valueOf(n);
                d.putFile(prefix + name + ".png", png(c.getSmallPicture(i), it.width, it.height, pal));
            }
    }

    private void exportFull(Dataset d, String file, String prefix) throws IOException
    {
        MediaContainer c = pictures.get(file);
        if (c == null)
            return;
        int n = 0;
        for (int i = 1; i < c.getItemCount(); i++)
            if (c.getItem(i).type == MediaContainer.FULL_SCREEN && c.getItem(i - 1).type == MediaContainer.PALETTE)
            {
                n++;
                String name = file.equals("TRACKPIX.DAT") && n == 17 ? "circuits/selection" : prefix + String.format("%02d", n);
                d.putFile(name + ".png", png(c.getFullScreen(i), 320, 200, c.getPalette(i - 1)));
            }
    }

    /** An 8-bit indexed PNG carrying the palette, so the pixels come back exactly. */
    static byte[] png(byte[] pixels, int w, int h, int[] palette) throws IOException
    {
        byte[] r = new byte[256], g = new byte[256], b = new byte[256];
        for (int i = 0; i < 256; i++)
        {
            r[i] = (byte) (palette[i] >> 16);
            g[i] = (byte) (palette[i] >> 8);
            b[i] = (byte) palette[i];
        }
        BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_INDEXED, new IndexColorModel(8, 256, r, g, b));
        im.getRaster().setDataElements(0, 0, w, h, pixels);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(im, "png", out);
        return out.toByteArray();
    }

    // ======================================================================
    // Import
    // ======================================================================

    /** What to import and where. */
    public static class ImportChoice
    {
        public Set<DatasetGroup> groups = EnumSet.noneOf(DatasetGroup.class);
        /** -1: everything as it is in the dataset. Otherwise a single team (0-19) from the dataset ... */
        public int fromTeam = -1;
        /** ... into this team (0-19), with its drivers taking that team's cars. */
        public int toTeam = -1;
        /** Write driver, team and engine names into GP.EXE. */
        public boolean namesIntoGame = true;
        /** Also (or instead) write the names, after the import, to this name file; null for none. */
        public File nameFile;
    }

    private final List<String> report = new ArrayList<String>();
    private boolean exeChanged, prefsChanged, soundsChanged;
    private final Set<String> picturesChanged = new java.util.LinkedHashSet<String>();
    private final Map<Integer, byte[]> tracksToWrite = new LinkedHashMap<Integer, byte[]>();

    /**
     * Applies a dataset and writes the changed files, after backing them up.
     * Returns plain-language lines saying what was done and what was left out.
     */
    public List<String> importDataset(Dataset d, ImportChoice choice) throws IOException
    {
        report.clear();
        report.addAll(d.getNotes());
        boolean single = choice.fromTeam >= 0 && choice.toTeam >= 0;
        Set<DatasetGroup> groups = EnumSet.noneOf(DatasetGroup.class);
        groups.addAll(choice.groups);
        groups.retainAll(groupsIn(d));
        groups.retainAll(available());
        if (single)
            for (DatasetGroup g : DatasetGroup.values())
                if (!g.team)
                    groups.remove(g);

        // which dataset car goes to which game car, and which team to which team
        int[] carTo = new int[CARS + 1];
        int[] teamTo = new int[TEAMS];
        for (int c = 1; c <= CARS; c++) carTo[c] = c;
        for (int t = 0; t < TEAMS; t++) teamTo[t] = t;
        if (single)
        {
            for (int c = 1; c <= CARS; c++) carTo[c] = 0;
            for (int t = 0; t < TEAMS; t++) teamTo[t] = -1;
            teamTo[choice.fromTeam] = choice.toTeam;
            for (int seat = 0; seat < 2; seat++)
            {
                Integer from = d.getInt("team " + (choice.fromTeam + 1), seat == 0 ? "first car" : "second car");
                int to = exe.getGameTeams().getTeamCar(choice.toTeam, seat);
                if (from != null && from >= 1 && from <= CARS)
                {
                    if (to == 0)
                        report.add("The " + (seat == 0 ? "first" : "second") + " seat of " + teamName(choice.toTeam)
                                   + " is empty, so that driver was not brought in.");
                    else
                        carTo[from] = to;
                }
            }
        }

        String[] oldNames = names();
        if (groups.contains(DatasetGroup.TEAMS)) importTeams(d, teamTo, !single);
        if (groups.contains(DatasetGroup.DRIVERS)) importDrivers(d, carTo);
        if (groups.contains(DatasetGroup.COLOURS)) importColours(d, teamTo, carTo);
        if (groups.contains(DatasetGroup.POINTS)) importPoints(d);
        if (groups.contains(DatasetGroup.RULES)) importRules(d);
        if (groups.contains(DatasetGroup.TABLES)) importTables(d);
        if (groups.contains(DatasetGroup.PHYSICS)) importPhysics(d);
        if (groups.contains(DatasetGroup.OPTIONS)) importOptions(d);
        if (groups.contains(DatasetGroup.SOUNDS)) importSounds(d);
        if (groups.contains(DatasetGroup.HELMET_PICTURES)) importHelmets(d, carTo);
        if (groups.contains(DatasetGroup.FLAG_PICTURES)) importSmall(d, "FLAGS.DAT", "flags/flag", TITLE_PICTURE, "Language flag");
        if (groups.contains(DatasetGroup.BACKDROP_PICTURES)) importFull(d, "BACKDROP.DAT", "backdrops/backdrop", "Menu background");
        if (groups.contains(DatasetGroup.CIRCUIT_PICTURES)) importFull(d, "TRACKPIX.DAT", "circuits/circuit", "Circuit picture");
        if (groups.contains(DatasetGroup.CIRCUITS)) importCircuits(d);

        // names: into a name file, GP.EXE, or both
        String[] newNames = names();
        boolean namesChanged = !java.util.Arrays.equals(oldNames, newNames);
        if (choice.nameFile != null)
        {
            NameFile nf = new NameFile();
            for (int c = 0; c < CARS; c++) nf.setDriver(c, exe.getGameDrivers().getDriverName(c));
            for (int t = 0; t < TEAMS; t++)
            {
                nf.setTeam(t, exe.getGameTeams().getTeamName(t));
                nf.setEngine(t, exe.getGameTeams().getEngineName(t));
            }
            choice.nameFile.getAbsoluteFile().getParentFile().mkdirs();
            backup(choice.nameFile);
            nf.save(choice.nameFile);
            report.add("The names were written to the name file " + choice.nameFile.getName() + ".");
        }
        if (namesChanged && !choice.namesIntoGame)
            setNames(oldNames);
        else if (namesChanged)
            exeChanged = true;

        if (groups.contains(DatasetGroup.TEAMS))
        {
            int seated = seatedCars();
            if (seated < GRID)
                report.add("Only " + seated + " cars have a seat in a team. The game always starts " + GRID + " cars, and with fewer "
                           + "the empty places on the grid are taken by cars that do not race properly. Give at least " + GRID
                           + " cars a seat on the executable's Teams page.");
        }

        writeChanges();
        report.add(0, groups.isEmpty() ? "Nothing was imported." : "Imported: " + labels(groups) + ".");
        return new ArrayList<String>(report);
    }

    /** The cars the game starts in a race; fewer seated cars leave places taken by cars that do not race (seen in the game). */
    public static final int GRID = 26;

    /** How many cars have a seat in a team. */
    public int seatedCars()
    {
        int n = 0;
        for (int t = 0; t < TEAMS; t++)
            for (int seat = 0; seat < 2; seat++)
                if (exe.getGameTeams().getTeamCar(t, seat) != 0)
                    n++;
        return n;
    }

    private static String labels(Set<DatasetGroup> groups)
    {
        StringBuilder sb = new StringBuilder();
        for (DatasetGroup g : groups)
            sb.append(sb.length() == 0 ? "" : ", ").append(g.label.toLowerCase());
        return sb.toString();
    }

    private String teamName(int t)
    {
        String n = exe.getGameTeams().getTeamName(t);
        return n.isEmpty() ? "team " + (t + 1) : n;
    }

    private String[] names()
    {
        String[] n = new String[CARS + 2 * TEAMS];
        for (int c = 0; c < CARS; c++) n[c] = exe.getGameDrivers().getDriverName(c);
        for (int t = 0; t < TEAMS; t++)
        {
            n[CARS + t] = exe.getGameTeams().getTeamName(t);
            n[CARS + TEAMS + t] = exe.getGameTeams().getEngineName(t);
        }
        return n;
    }

    private void setNames(String[] n)
    {
        for (int c = 0; c < CARS; c++) exe.getGameDrivers().setDriverName(c, n[c]);
        for (int t = 0; t < TEAMS; t++)
        {
            exe.getGameTeams().setTeamName(t, n[CARS + t]);
            exe.getGameTeams().setEngineName(t, n[CARS + TEAMS + t]);
        }
    }

    /** A number in range, or null (with a note) if it is out of range; null if absent. */
    private Integer value(Dataset d, String section, String key, int min, int max, String what)
    {
        String raw = d.get(section, key);
        if (raw == null)
            return null;
        Integer v = d.getInt(section, key);
        if (v == null || v < min || v > max)
        {
            report.add(what + " was left as it is: \"" + raw + "\" is not a number from " + min + " to " + max + ".");
            return null;
        }
        return v;
    }

    private String name(Dataset d, String section, String key, int length, String what)
    {
        String v = d.get(section, key);
        if (v == null)
            return null;
        StringBuilder sb = new StringBuilder();
        for (char ch : v.toCharArray())
            sb.append(ch < 256 ? ch : '?');
        String s = sb.toString();
        if (s.length() > length)
        {
            report.add(what + " \"" + s + "\" is too long for the game and was cut to " + length + " letters.");
            s = s.substring(0, length);
        }
        return s;
    }

    private void importTeams(Dataset d, int[] teamTo, boolean lineUps)
    {
        GameTeams teams = exe.getGameTeams();
        int[][] cars = new int[TEAMS][2];
        boolean anyLineUp = false;
        for (int t = 0; t < TEAMS; t++)
        {
            cars[t][0] = teams.getTeamCar(t, 0);
            cars[t][1] = teams.getTeamCar(t, 1);
        }
        for (int t = 0; t < TEAMS; t++)
        {
            int to = teamTo[t];
            if (to < 0)
                continue;
            String s = "team " + (t + 1);
            String n = name(d, s, "name", TEAM_NAME, "The team name");
            if (n != null) teams.setTeamName(to, n);
            n = name(d, s, "engine", TEAM_NAME, "The engine name");
            if (n != null) teams.setEngineName(to, n);
            Integer p = value(d, s, "power", 0, 1300, "The engine power of team " + (t + 1));
            if (p != null) teams.setBHPSetting(to, p);
            if (lineUps)
                for (int seat = 0; seat < 2; seat++)
                {
                    Integer c = value(d, s, seat == 0 ? "first car" : "second car", 0, CARS, "A car number of team " + (t + 1));
                    if (c != null)
                    {
                        cars[to][seat] = c;
                        anyLineUp = true;
                    }
                }
            exeChanged = true;
        }
        if (!anyLineUp)
            return;
        boolean[] used = new boolean[CARS + 1];
        for (int t = 0; t < TEAMS; t++)
            for (int seat = 0; seat < 2; seat++)
            {
                int c = cars[t][seat];
                if (c == 0)
                    continue;
                if (used[c])
                {
                    report.add("The teams' cars were left as they are: car " + c + " is in two seats.");
                    return;
                }
                used[c] = true;
            }
        for (int t = 0; t < TEAMS; t++)
            for (int seat = 0; seat < 2; seat++)
                teams.setTeamCar(t, seat, cars[t][seat]);
    }

    private void importDrivers(Dataset d, int[] carTo)
    {
        GameDrivers drivers = exe.getGameDrivers();
        for (int c = 1; c <= CARS; c++)
        {
            int to = carTo[c];
            if (to == 0)
                continue;
            String s = "driver " + c;
            String n = name(d, s, "name", DRIVER_NAME, "The driver name");
            if (n != null) drivers.setDriverName(to - 1, n);
            Integer q = value(d, s, "qualifying skill", 0, 255, "The qualifying skill of car " + c);
            if (q != null) drivers.setQualifyingGrip(to - 1, q);
            Integer r = value(d, s, "race skill", 0, 255, "The race skill of car " + c);
            if (r != null) drivers.setRaceGrip(to - 1, r);
            exeChanged = true;
        }
    }

    private void importColours(Dataset d, int[] teamTo, int[] carTo)
    {
        GameLiveries l = exe.getGameLiveries();
        for (int t = 0; t < TEAMS; t++)
        {
            if (teamTo[t] < 0)
                continue;
            String s = "team " + (t + 1);
            for (int p = 0; p < GameLiveries.CAR_PARTS.length; p++)
            {
                Integer v = value(d, s, "car " + GameLiveries.CAR_PARTS[p].toLowerCase(), 0, 255, "A car colour");
                if (v != null) l.setCarColour(teamTo[t], p, v);
            }
            for (int p = 0; p < GameLiveries.PIT_CREW_PARTS.length; p++)
            {
                Integer v = value(d, s, "pit crew " + GameLiveries.PIT_CREW_PARTS[p].toLowerCase(), 0, 255, "A pit crew colour");
                if (v != null) l.setPitCrewColour(teamTo[t], p, v);
            }
        }
        for (int c = 1; c <= CARS; c++)
        {
            if (carTo[c] == 0)
                continue;
            for (int p = 0; p < GameLiveries.HELMET_PARTS.length; p++)
            {
                Integer v = value(d, "driver " + c, "helmet " + GameLiveries.HELMET_PARTS[p].toLowerCase(), 0, 255, "A helmet colour");
                if (v != null) l.setHelmetColour(carTo[c] - 1, p, v);
            }
        }
        exeChanged = true;
    }

    private void importPoints(Dataset d)
    {
        for (int i = 0; i < 26; i++)
        {
            Integer v = value(d, "points", place(i), 0, 127, "The points for " + place(i));
            if (v != null) exe.getGamePoints().setPointValue(i, v);
        }
        exeChanged = true;
    }

    private void importRules(Dataset d)
    {
        GameSettings s = exe.getGameSettings();
        for (RuleSetting r : RULES)
        {
            Integer v = value(d, "rules", r.key, r.min, r.max, "The rule \"" + r.key + "\"");
            if (v != null && v != r.rule.get(s))
            {
                if (r.key.equals("computer grip level") && s.getAIGripLevel() == 0)
                {
                    report.add("The computer grip level was left as it is: another tool has changed that part of GP.EXE.");
                    continue;
                }
                r.rule.set(s, v);
            }
        }
        Boolean b = d.getBoolean("rules", "rain at dry circuits");
        if (b != null) s.setRainEverywhere(b);
        b = d.getBoolean("rules", "unlimited qualifying tyres");
        if (b != null) s.setQualifyingTyres(b);
        b = d.getBoolean("rules", "faster menus");
        if (b != null) exe.getGameOptions().setFastFade(b);
        b = d.getBoolean("rules", "skip language screen");
        if (b != null) exe.getGameOptions().setLanguageSelection(b);
        exeChanged = true;
    }

    private void importTables(Dataset d)
    {
        GameTables t = exe.getGameTables();
        int[] order = d.getInts("pit lane", "team order");
        if (order != null)
        {
            boolean ok = order.length == t.pitOrder.words();
            for (int v : order) ok &= v >= 1 && v <= TEAMS;
            if (ok)
                for (int i = 0; i < order.length; i++) t.pitOrder.setWord(i, order[i]);
            else
                report.add("The pit lane order was left as it is: it needs 20 team numbers from 1 to 20.");
        }
        for (int c = 0; c < CARS; c++)
            setBytes(t.carSetups, c * GameTables.SETUP_SIZE, d.getInts("car setups", "car " + (c + 1)), "The set-up of car " + (c + 1));
        for (int c = 0; c < 32; c++)
            setBytes(t.circuitSetups, c * GameTables.SETUP_SIZE,
                     d.getInts("circuit setups", "circuit " + (c % 16 + 1) + (c < 16 ? "" : " race")),
                     "A circuit set-up for " + GameFolder.CIRCUIT_NAMES[c % 16]);
        for (int c = 0; c < 16; c++)
            for (int race = 0; race < 2; race++)
            {
                String k = "circuit " + (c + 1) + (race == 1 ? " race" : " qualifying");
                String v = name(d, "lap records", k + " driver", GameTables.NAME_LENGTH - 1, "The lap record holder");
                if (v != null) t.setRecordDriver(c, race == 1, v);
                v = name(d, "lap records", k + " team", GameTables.TEAM_LENGTH - 1, "The lap record team");
                if (v != null) t.setRecordTeam(c, race == 1, v);
                String time = d.get("lap records", k + " time");
                if (time != null)
                {
                    int ms = parseLapTime(time);
                    if (ms > 0) t.setRecordTime(c, race == 1, ms);
                    else report.add("A lap record time was left as it is: \"" + time + "\" is not a time like 1:21.434.");
                }
                String date = d.get("lap records", k + " date");
                if (date != null)
                {
                    int day = -1;
                    try { day = GameTables.dayOfDate(java.time.LocalDate.parse(date.trim())); } catch (RuntimeException e) { day = -1; }
                    if (day >= 0) t.setRecordDay(c, race == 1, day);
                    else report.add("A lap record date was left as it is: \"" + date + "\" is not a date the game can show (year-month-day).");
                }
            }
        exeChanged = true;
    }

    private void setBytes(GameTables.Table t, int at, int[] v, String what)
    {
        if (v == null)
            return;
        boolean ok = v.length == GameTables.SETUP_SIZE;
        for (int b : v) ok &= b >= 0 && b <= 255;
        if (!ok)
        {
            report.add(what + " was left as it is: it needs " + GameTables.SETUP_SIZE + " numbers from 0 to 255.");
            return;
        }
        for (int i = 0; i < v.length; i++)
            t.setByte(at + i, v[i]);
    }

    private void importPhysics(Dataset d)
    {
        for (Map.Entry<String, GameTables.Table> e : physicsTables().entrySet())
        {
            int[] v = d.getInts("car performance", e.getKey());
            if (v == null)
                continue;
            GameTables.Table t = e.getValue();
            boolean ok = v.length == t.words();
            for (int w : v) ok &= w >= 0 && w <= 0xFFFF;
            if (e.getKey().equals("car weight"))
                ok &= v.length == 2 && v[0] == v[1] && v[0] > 0;     // a weight of 0 crashes the game
            if (!ok)
            {
                report.add("The table \"" + e.getKey() + "\" was left as it is: it needs " + t.words() + " numbers from 0 to 65535"
                           + (e.getKey().equals("car weight") ? ", both the same and not 0." : "."));
                continue;
            }
            for (int i = 0; i < v.length; i++)
                t.setWord(i, v[i]);
        }
        exeChanged = true;
    }

    private void importOptions(Dataset d)
    {
        Integer v;
        if ((v = value(d, "options", "opposition standard", 0, 4, "The opposition standard")) != null) prefs.setWord(PreferencesFile.OPPOSITION_STANDARD, v);
        if ((v = value(d, "options", "opposition spread", 0, 2, "The opposition spread")) != null) prefs.setWord(PreferencesFile.OPPOSITION_SPREAD, v);
        if ((v = value(d, "options", "race distance", 5, 100, "The race distance")) != null) prefs.setWord(PreferencesFile.RACE_DISTANCE, v);
        if ((v = value(d, "options", "qualifying minutes", 1, 120, "The qualifying time")) != null) prefs.setWord(PreferencesFile.QUALIFYING_MINUTES, v);
        if ((v = value(d, "options", "practice minutes", 1, 120, "The practice time")) != null) prefs.setWord(PreferencesFile.PRACTICE_MINUTES, v);
        for (int b = 0; b < AIDS.length; b++)
        {
            Boolean on = d.getBoolean("options", "aid " + AIDS[b]);
            if (on != null)
                prefs.setWord(AIDS_AT, on ? prefs.getWord(AIDS_AT) | (1 << b) : prefs.getWord(AIDS_AT) & ~(1 << b));
        }
        Boolean on = d.getBoolean("options", "km/h");
        if (on != null) prefs.setWord(KPH_AT, on ? prefs.getWord(KPH_AT) | 0x8000 : prefs.getWord(KPH_AT) & 0x7FFF);
        on = d.getBoolean("options", "textures");
        if (on != null) prefs.setByte(TEXTURES_AT, on ? prefs.getByte(TEXTURES_AT) | 0x80 : prefs.getByte(TEXTURES_AT) & 0x7F);
        if ((v = value(d, "options", "quick race circuit", 1, 16, "The quick race circuit")) != null) prefs.setByte(PreferencesFile.QUICK_RACE_CIRCUIT, v - 1);
        if ((v = value(d, "options", "quick race driver", 1, 40, "The quick race driver")) != null) prefs.setByte(PreferencesFile.QUICK_RACE_DRIVER, v);
        if ((v = value(d, "options", "quick race distance", 5, 100, "The quick race distance")) != null) prefs.setByte(PreferencesFile.QUICK_RACE_DISTANCE, v);
        on = d.getBoolean("options", "quick race wet");
        if (on != null)
            prefs.setByte(PreferencesFile.QUICK_RACE_WET, on ? prefs.getByte(PreferencesFile.QUICK_RACE_WET) | 0x80
                                                             : prefs.getByte(PreferencesFile.QUICK_RACE_WET) & 0x7F);
        prefsChanged = true;
    }

    private void importSounds(Dataset d)
    {
        for (SoundSetting s : SOUND_SETTINGS)
        {
            Integer v = value(d, "sounds", s.key, s.min, s.max, "The sound setting \"" + s.key + "\"");
            if (v != null) s.set(sounds, v);
        }
        for (int k : INSTRUMENTS)
        {
            int[] v = d.getInts("sounds", "instrument " + k);
            if (v == null)
                continue;
            boolean ok = v.length == INSTRUMENT_BYTES.length;
            for (int i = 0; ok && i < v.length; i++) ok = v[i] >= 0 && v[i] <= INSTRUMENT_MASKS[i];
            if (!ok)
            {
                report.add("Sound instrument " + k + " was left as it is: its numbers are not ones the game can use.");
                continue;
            }
            int r = SoundEffectsFile.record(k);
            for (int i = 0; i < v.length; i++)
                sounds.setBits(r + INSTRUMENT_BYTES[i], INSTRUMENT_MASKS[i], v[i]);
        }
        soundsChanged = true;
    }

    /** A picture from the dataset as an image, or null (with a note) if it cannot be read. */
    private BufferedImage image(Dataset d, String path, String what)
    {
        byte[] b = d.getFile(path);
        if (b == null)
            return null;
        try
        {
            BufferedImage im = ImageIO.read(new ByteArrayInputStream(b));
            if (im != null)
                return im;
        }
        catch (IOException e) { /* reported below */ }
        report.add(what + " (" + path + ") could not be read as a picture and was left out.");
        return null;
    }

    /** The picture's own palette indices, if it is indexed with these colours (to 6 bits) at every index it uses. */
    private static byte[] exactIndices(BufferedImage im, int w, int h, int[] palette, int first)
    {
        if (!(im.getColorModel() instanceof IndexColorModel) || im.getWidth() != w || im.getHeight() != h)
            return null;
        IndexColorModel cm = (IndexColorModel) im.getColorModel();
        WritableRaster r = im.getRaster();
        byte[] out = new byte[w * h];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
            {
                int i = r.getSample(x, y, 0);
                if (i >= cm.getMapSize() || (palette != null && (i < first || !sameColour(cm.getRGB(i), palette[i]))))
                    return null;
                out[y * w + x] = (byte) i;
            }
        return out;
    }

    private static boolean sameColour(int a, int b)
    {
        return ((a >> 18) & 0x3F) == ((b >> 18) & 0x3F) && ((a >> 10) & 0x3F) == ((b >> 10) & 0x3F) && ((a >> 2) & 0x3F) == ((b >> 2) & 0x3F);
    }

    private static int[] rgb(BufferedImage in, int w, int h)
    {
        if (in.getWidth() != w || in.getHeight() != h)
        {
            BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(in, 0, 0, w, h, null);
            g.dispose();
            in = scaled;
        }
        return in.getRGB(0, 0, w, h, null, 0, w);
    }

    /**
     * A picture file's pixels as palette indices: its own indices if they are
     * this palette's, else matched to the nearest colours; null if it is not a picture.
     */
    static byte[] pictureIndices(byte[] file, int w, int h, int[] pal)
    {
        BufferedImage im;
        try
        {
            im = ImageIO.read(new ByteArrayInputStream(file));
        }
        catch (IOException e)
        {
            return null;
        }
        if (im == null)
            return null;
        byte[] px = exactIndices(im, w, h, pal, 0);
        return px != null ? px : ColourReducer.match(rgb(im, w, h), pal, 0);
    }

    /** Puts a small picture in place: its own indices if they are this palette's, else matched to the nearest colours. */
    private void putSmall(MediaContainer c, int item, BufferedImage im, int[] pal)
    {
        MediaContainer.Item it = c.getItem(item);
        byte[] px = exactIndices(im, it.width, it.height, pal, 0);
        if (px == null)
            px = ColourReducer.match(rgb(im, it.width, it.height), pal, 0);
        c.setSmallPicture(item, px);
    }

    private List<Integer> smallItems(MediaContainer c)
    {
        List<Integer> out = new ArrayList<Integer>();
        for (int i = 0; i < c.getItemCount(); i++)
            if (c.getItem(i).type == MediaContainer.SMALL_PICTURE)
                out.add(i);
        return out;
    }

    private void importHelmets(Dataset d, int[] carTo)
    {
        MediaContainer c = pictures.get("HELMETS.DAT");
        int[] pal = smallPalette(DRIVER_SELECTION_PICTURE);
        if (pal == null)
        {
            report.add("The menu helmets were left out: BACKDROP.DAT, which holds their colours, could not be read.");
            return;
        }
        List<Integer> items = smallItems(c);
        for (int car = 1; car <= CARS; car++)
        {
            int to = carTo[car];
            if (to == 0 || to > items.size())
                continue;
            BufferedImage im = image(d, String.format("helmets/car%02d.png", car), "The menu helmet of car " + car);
            if (im == null)
                continue;
            putSmall(c, items.get(to - 1), im, pal);
            picturesChanged.add("HELMETS.DAT");
        }
    }

    private void importSmall(Dataset d, String file, String prefix, int background, String what)
    {
        MediaContainer c = pictures.get(file);
        int[] pal = smallPalette(background);
        if (pal == null)
        {
            report.add("The " + what.toLowerCase() + "s were left out: BACKDROP.DAT, which holds their colours, could not be read.");
            return;
        }
        List<Integer> items = smallItems(c);
        for (int n = 1; n <= items.size(); n++)
        {
            BufferedImage im = image(d, prefix + n + ".png", what + " " + n);
            if (im == null)
                continue;
            putSmall(c, items.get(n - 1), im, pal);
            picturesChanged.add(file);
        }
    }

    private void importFull(Dataset d, String file, String prefix, String what)
    {
        MediaContainer c = pictures.get(file);
        int n = 0;
        for (int i = 1; i < c.getItemCount(); i++)
        {
            if (c.getItem(i).type != MediaContainer.FULL_SCREEN || c.getItem(i - 1).type != MediaContainer.PALETTE)
                continue;
            n++;
            String path = file.equals("TRACKPIX.DAT") && n == 17 ? "circuits/selection.png" : prefix + String.format("%02d", n) + ".png";
            BufferedImage im = image(d, path, what + " " + n);
            if (im == null)
                continue;
            int[] was = c.getPalette(i - 1);
            // the helmets and flags are drawn in these two backgrounds' colours, so those keep theirs
            boolean keepColours = file.equals("BACKDROP.DAT") && (n - 1 == DRIVER_SELECTION_PICTURE || n - 1 == TITLE_PICTURE);
            int[] pal = was.clone();
            byte[] px;
            if (keepColours)
            {
                px = exactIndices(im, 320, 200, was, 0);
                if (px == null)
                    px = ColourReducer.match(rgb(im, 320, 200), was, MENU_COLOURS);
            }
            else
            {
                // exact if its menu colours are this game's; otherwise colours of its own, as picture import does
                px = exactIndices(im, 320, 200, null, 0);
                if (px != null)
                {
                    IndexColorModel cm = (IndexColorModel) im.getColorModel();
                    boolean menuSame = cm.getMapSize() >= 256;
                    for (int k = 0; menuSame && k < MENU_COLOURS; k++)
                        menuSame = sameColour(cm.getRGB(k), was[k]);
                    if (menuSame)
                        for (int k = MENU_COLOURS; k < 256; k++)
                            pal[k] = cm.getRGB(k) & 0xFFFFFF;
                    else
                        px = null;
                }
                if (px == null)
                {
                    int[] rgb = rgb(im, 320, 200);
                    pal = ColourReducer.palette(rgb, was, MENU_COLOURS);
                    px = ColourReducer.match(rgb, pal, MENU_COLOURS);
                }
            }
            c.setPalette(i - 1, pal);
            if (!c.setFullScreen(i, px))
            {
                c.setPalette(i - 1, was);
                report.add(what + " " + n + " was left as it is: it is too detailed for the game to load.");
                continue;
            }
            picturesChanged.add(file);
        }
    }

    private void importCircuits(Dataset d)
    {
        for (int slot = 1; slot <= GameFolder.CIRCUITS; slot++)
        {
            byte[] b = d.getFile("tracks/" + GameFolder.trackFileName(slot));
            if (b == null)
                continue;
            if (b.length > Track.MAX_FILE_BYTES)
                report.add(GameFolder.trackFileName(slot) + " was left out: it is too large for the game to load.");
            else if (!trackChecksumOk(b))
                report.add(GameFolder.trackFileName(slot) + " was left out: it is damaged (its checksum is wrong).");
            else
                tracksToWrite.put(slot, b);
        }
    }

    /** A track file ends in the same two checksums as the game's other files. */
    static boolean trackChecksumOk(byte[] b)
    {
        return cfevolution.data.gamefiles.GameFile.checksumOk(b);
    }

    // --- writing --------------------------------------------------------------

    private File backupFolder;

    private void backup(File f) throws IOException
    {
        if (!f.isFile())
            return;
        if (backupFolder == null)
        {
            backupFolder = new File(new File(game.getFolder(), BACKUP_FOLDER), new SimpleDateFormat("yyyy-MM-dd-HHmmss").format(new Date()));
            backupFolder.mkdirs();
            report.add("The files changed were copied first to " + BACKUP_FOLDER + File.separator + backupFolder.getName() + " in the game folder.");
        }
        Files.copy(f.toPath(), new File(backupFolder, f.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private void writeChanges() throws IOException
    {
        if (exeChanged)
        {
            backup(game.getExecutable());
            if (!exe.saveData())
                throw new IOException("GP.EXE could not be saved");
        }
        if (prefsChanged)
        {
            backup(game.getPreferences());
            prefs.save(game.getPreferences());
        }
        if (soundsChanged)
        {
            backup(game.getSoundEffects());
            sounds.save(game.getSoundEffects());
        }
        for (String name : picturesChanged)
        {
            File f = game.getPictureFile(name);
            backup(f);
            pictures.get(name).save(f);
        }
        for (Map.Entry<Integer, byte[]> t : tracksToWrite.entrySet())
        {
            File f = game.getTrack(t.getKey());
            if (f == null)
                f = new File(game.getFolder(), GameFolder.trackFileName(t.getKey()));
            backup(f);
            Files.write(f.toPath(), t.getValue());
        }
    }
}
