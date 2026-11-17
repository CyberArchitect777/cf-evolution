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

import cfevolution.data.GameFolder;
import cfevolution.data.gamefiles.ColourReducer;
import cfevolution.data.gamefiles.MediaContainer;
import cfevolution.data.image.Picture;
import cfevolution.data.image.Pictures;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The game's picture files (2026-11-17, from the desktop's PictureFileTab
 * word for word): HELMETS.DAT (the menu helmets, one per car), FLAGS.DAT,
 * BACKDROP.DAT (the menu backgrounds) and TRACKPIX.DAT (each circuit's
 * picture on the circuit menu). Small pictures can be drawn on; every picture
 * can be exported to PNG and replaced from one.
 *
 * The menu helmets are drawn in the palette of the driver selection
 * background (BACKDROP.DAT picture 6) and the language flags in the title
 * screen's (picture 4), as ArgDocs says and as drawing them confirms. The
 * first 32 colours of every menu palette are the menus' own (F1GP-SDL) and
 * are never changed.
 */
public final class PictureFilePage
{
    /** What BACKDROP.DAT's pictures are (ArgDocs, and looking at them). */
    static final String[] BACKDROP_NAMES = {
        "Benetton (title sequence)", "Marshal waving a flag (between races)", "Honda engine (constructors' table)",
        "Title screen", "Two Lotuses", "Mansell's helmet (driver selection)", "Pit board", "Mansell in the pits (Esc in a session)",
        "Car from behind", "Car in the pits (setup screen)", "Car in the pits, wheels off (setup screen)",
        "Car in the pits, bodywork off (setup screen)", "Senna with champagne", "Nose of a car",
        "Senna and a Williams in the rain", "McLaren and Williams at Monaco (main menu)", "Three drivers on the podium",
        "Drivers together" };
    static final int FIRST_UI_COLOURS = 32;
    static final int DRIVER_SELECTION_PICTURE = 5, TITLE_PICTURE = 3;

    public static final String EXPORT = "Export PNG...", IMPORT = "Import PNG...", REVERT = "Revert Picture";
    public static final String DRAW = "Draw", REPLACE = "Replace a colour", COLOURS_TIP = "Choose a colour";
    public static final String HINT = "Click to use the chosen colour; right-click a pixel to pick up its colour.";
    public static final Message TOO_DETAILED = Message.warning("Picture Not Changed", "This picture is too detailed for the game to load. Try a simpler one.");
    public static final Message NOT_A_PICTURE = Message.error("Import Failed", "That file is not a picture the editor can read.");
    public static final Message NOT_EXPORTED = Message.error("Export Failed", "The picture could not be saved.");
    /** Asked before a full-screen picture is imported (unless the helmets or flags are drawn in its colours). */
    public static final Message COLOURS_QUESTION = Message.choose(Message.Kind.QUESTION, "Import Picture",
        "Give this picture colours of its own (best for a new picture), or match it to the colours it has now?",
        "New Colours", "Keep Colours", "Cancel");

    public final MediaContainer pictures;
    private final MediaContainer original;
    private final File file;
    private final String upper;
    public final boolean small;
    private final List<Integer> items = new ArrayList<Integer>();
    private final List<String> names = new ArrayList<String>();
    private int[] smallPalette;
    private int colour = 15;
    private boolean replaceMode;
    private boolean changed;

    /**
     * @param original the file as it was opened (Revert Picture)
     * @param game the folder, for the helmets' driver names and the palettes; may be null
     */
    public PictureFilePage(MediaContainer pictures, MediaContainer original, File file, GameFolder game)
    {
        this.pictures = pictures;
        this.original = original;
        this.file = file;
        upper = file.getName().toUpperCase();
        small = pictures.getItemCount() > 0 && pictures.getItem(0).type == MediaContainer.SMALL_PICTURE;
        String[] drivers = game != null && upper.startsWith("HELMETS") ? game.getDriverNames() : null;
        for (int i = 0; i < pictures.getItemCount(); i++)
        {
            int type = pictures.getItem(i).type;
            if (small ? type != MediaContainer.SMALL_PICTURE : type != MediaContainer.FULL_SCREEN)
                continue;
            int n = items.size();
            items.add(i);
            if (upper.startsWith("HELMETS"))
                names.add(drivers != null && n < drivers.length && drivers[n].length() > 0 ? (n + 1) + "  " + drivers[n] : "Car " + (n + 1));
            else if (upper.startsWith("TRACKPIX"))
                names.add(n < 16 ? (n + 1) + "  " + GameFolder.CIRCUIT_NAMES[n] : (n + 1) + "  Circuit selection screen");
            else if (upper.startsWith("FLAGS"))
                names.add("Flag " + (n + 1));
            else if (upper.startsWith("BACKDROP"))
                names.add((n + 1) + "  " + (n < BACKDROP_NAMES.length ? BACKDROP_NAMES[n] : "Picture"));
            else
                names.add("Picture " + (n + 1));
        }
        if (small)
            smallPalette = defaultSmallPalette(game);
    }

    public String title()
    {
        return small ? (upper.startsWith("HELMETS") ? "Menu Helmets" : "Flags")
                     : (upper.startsWith("TRACKPIX") ? "Circuit Pictures" : "Menu Backgrounds");
    }

    public String explanation()
    {
        if (small)
            return upper.startsWith("HELMETS")
                ? "The helmets on the driver menus, one for each car. They are pictures: the helmet colours in GP.EXE do not change them."
                : "The flags on the language screen when the European version starts.";
        return upper.startsWith("TRACKPIX")
            ? "Each circuit's picture on the circuit menu, and the circuit selection screen. Replace one with Import PNG (320 x 200)."
            : "The menu backgrounds. Replace one with Import PNG (320 x 200).";
    }

    public String status()
    {
        return names.size() + " pictures  ·  " + (small ? "draw on them, or import PNG" : "320 x 200, import PNG to replace");
    }

    public List<String> names()
    {
        return java.util.Collections.unmodifiableList(names);
    }

    /** Whether the last change changed the file. */
    public boolean changed()
    {
        return changed;
    }

    // --- palettes ---------------------------------------------------------------

    private MediaContainer backdrops(GameFolder game)
    {
        File[] candidates = { new File(file.getAbsoluteFile().getParentFile(), "BACKDROP.DAT"),
                              game == null ? null : new File(game.getFolder(), "BACKDROP.DAT") };
        for (File f : candidates)
            if (f != null && f.isFile())
            {
                if (f.getAbsolutePath().equals(file.getAbsolutePath()))
                    return pictures;
                try { return MediaContainer.load(f); } catch (IOException e) { /* try the next */ }
            }
        return null;
    }

    /** Helmets: the driver selection background's palette; flags: the title screen's. */
    private int[] defaultSmallPalette(GameFolder game)
    {
        MediaContainer b = backdrops(game);
        int background = upper.startsWith("FLAGS") ? TITLE_PICTURE : DRIVER_SELECTION_PICTURE;
        if (b != null && b.getItemCount() > background * 2)
            return b.getPalette(background * 2);
        int[] grey = new int[256];
        for (int i = 0; i < 256; i++) grey[i] = i * 0x010101;
        return grey;
    }

    /** The colours the small pictures are drawn in (0xRRGGBB). */
    public int[] smallPalette()
    {
        return smallPalette;
    }

    /** The picture (in names()) that is the file's item i, or -1. */
    public int pictureOfItem(int item)
    {
        return items.indexOf(item);
    }

    private int item(int picture)
    {
        return items.get(picture);
    }

    private int[] paletteOf(int item)
    {
        return small ? smallPalette : pictures.getPalette(item - 1);
    }

    // --- the picture -----------------------------------------------------------------

    public int width(int picture)
    {
        return small ? pictures.getItem(item(picture)).width : 320;
    }

    public int height(int picture)
    {
        return small ? pictures.getItem(item(picture)).height : 200;
    }

    private byte[] pixels(int item)
    {
        return small ? pictures.getSmallPicture(item) : pictures.getFullScreen(item);
    }

    /** A picture's colours (0xRRGGBB), row by row. */
    public int[] rgb(int picture)
    {
        int item = item(picture);
        byte[] px = pixels(item);
        int[] pal = paletteOf(item);
        int[] out = new int[px.length];
        for (int i = 0; i < px.length; i++)
            out[i] = pal[px[i] & 0xFF];
        return out;
    }

    public int colour()
    {
        return colour;
    }

    public void setColour(int c)
    {
        if (c >= 0 && c < 256)
            colour = c;
    }

    public boolean replaceMode()
    {
        return replaceMode;
    }

    /** Draw (false), or Replace a colour (true). */
    public void setReplaceMode(boolean on)
    {
        replaceMode = on;
    }

    /** A right-click on a small picture: its colour there becomes the chosen one. */
    public void pick(int picture, int x, int y)
    {
        if (!small || x < 0 || y < 0 || x >= width(picture) || y >= height(picture))
            return;
        colour = pixels(item(picture))[y * width(picture) + x] & 0xFF;
    }

    /** A click on a small picture: the pixel (or, replacing, every pixel of its colour) in the chosen colour. */
    public void paint(int picture, int x, int y)
    {
        changed = false;
        if (!small || x < 0 || y < 0 || x >= width(picture) || y >= height(picture))
            return;
        int item = item(picture);
        byte[] px = pixels(item);
        int at = y * width(picture) + x;
        if (replaceMode)
        {
            byte old = px[at];
            for (int i = 0; i < px.length; i++)
                if (px[i] == old)
                    px[i] = (byte) colour;
        }
        else
            px[at] = (byte) colour;
        setPixels(item, px);
    }

    /** The refusal when a full-screen picture is too detailed, else null. Marks the file changed (as the desktop does) when it is written. */
    private Message setPixels(int item, byte[] px)
    {
        changed = false;
        if (small)
            pictures.setSmallPicture(item, px);
        else if (!pictures.setFullScreen(item, px))
            return TOO_DETAILED;
        changed = true;
        return null;
    }

    /** Revert Picture: as it was when the file was opened; the refusal, or null. */
    public Message revert(int picture)
    {
        int item = item(picture);
        byte[] px = small ? original.getSmallPicture(item) : original.getFullScreen(item);
        if (!small)
            pictures.setPalette(item - 1, original.getPalette(item - 1));
        return setPixels(item, px);
    }

    // --- PNG ----------------------------------------------------------------------------

    /** The name offered for an export: "HELMETS-3.png". */
    public String exportName(int picture)
    {
        return file.getName().replaceAll("\\..*$", "") + "-" + (picture + 1) + ".png";
    }

    /** The picture as a true colour PNG. */
    public byte[] exportPng(int picture) throws IOException
    {
        return Pictures.codec().writeRgbPng(rgb(picture), width(picture), height(picture));
    }

    /** A picture file read with the platform's codec, or null (NOT_A_PICTURE). */
    public static Picture read(byte[] bytes)
    {
        try
        {
            return bytes == null ? null : Pictures.codec().read(bytes);
        }
        catch (IOException e)
        {
            return null;
        }
    }

    /** Asked before an import into this picture, or null when nothing is asked (a small picture, or a background the helmets or flags use). */
    public Message importQuestion(int picture)
    {
        return small || helmetsUseIt(item(picture)) ? null : COLOURS_QUESTION;
    }

    private boolean helmetsUseIt(int item)
    {
        return upper.startsWith("BACKDROP") && (item == DRIVER_SELECTION_PICTURE * 2 + 1 || item == TITLE_PICTURE * 2 + 1);
    }

    /**
     * Puts a picture in place of this one: scaled to fit if its size differs,
     * then matched to the nearest colours; a full-screen picture gets colours
     * of its own when newColours (the answer to importQuestion) says so.
     * The refusal (too detailed: nothing changed), or null.
     */
    public Message importPicture(int picture, Picture in, boolean newColours)
    {
        changed = false;
        int item = item(picture);
        int w = width(picture), h = height(picture);
        int[] rgb = Pictures.codec().argbAt(in, w, h);
        int[] pal = paletteOf(item).clone();
        if (!small && newColours && !helmetsUseIt(item))
            pal = ColourReducer.palette(rgb, pal, FIRST_UI_COLOURS);
        byte[] px = ColourReducer.match(rgb, pal, small ? 0 : FIRST_UI_COLOURS);
        if (!small)
        {
            int[] was = pictures.getPalette(item - 1);
            pictures.setPalette(item - 1, pal);
            if (!pictures.setFullScreen(item, px))
            {
                pictures.setPalette(item - 1, was);
                return TOO_DETAILED;
            }
            changed = true;
            return null;
        }
        return setPixels(item, px);
    }

    /** Saves the file; the message when it could not be, else null. */
    public Message save(File to)
    {
        try
        {
            pictures.save(to);
            return null;
        }
        catch (IOException e)
        {
            return Message.error("Save Failed", to.getName() + " could not be saved.");
        }
    }
}
