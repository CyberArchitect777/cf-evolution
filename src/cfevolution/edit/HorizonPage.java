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

import cfevolution.data.executable.GameLiveries;
import cfevolution.data.image.Picture;
import cfevolution.data.image.Pictures;
import cfevolution.data.track.Track;
import cfevolution.data.track.TrackHorizon;
import java.io.File;
import java.io.IOException;

/**
 * Edit > Horizon (2026-11-17, from the desktop's HorizonDialog word for
 * word): the picture of distant scenery the game draws along the horizon,
 * painted in the game's own colours from GP.EXE. Painting, picking up a
 * colour, exporting as PNG or ArgTrack's .arghoriz, importing either or
 * another track's horizon; nothing reaches the track until ok().
 */
public final class HorizonPage
{
    public static final String TITLE = "Horizon";
    public static final String ABOUT = "The distant scenery the game draws along the "
        + "horizon, 512 by 8 pixels. The picture covers a quarter turn, so it is seen four times round a "
        + "lap. Nothing in it is see-through: the sky is painted in the game's sky colour (the one every "
        + "original uses), which changes with the weather. Colours marked in the colour chooser change "
        + "with the circuit or the weather, and in the rain the game greys the whole picture.";
    public static final String PREVIEW = "As seen in the game: the whole picture. Click a part to see it close up.";
    public static final String PREVIEW_TIP = "The whole horizon at the shape the game draws it; a screen shows about 320 of its 512 columns "
        + "at once. The frame marks the part shown close up above.";
    public static final String CANVAS_TIP = "Click to paint in the chosen colour; right-click to pick up a colour";
    public static final String COLOUR = "Colour...", EXPORT = "Export...", IMPORT = "Import...", CHOOSER_TITLE = "Horizon colour";
    public static final String EXPORT_TITLE = "Export the horizon", IMPORT_TITLE = "Import a horizon";
    public static final String PNG_FILTER = "PNG picture (*.png)", RAW_FILTER = "ArgTrack horizon (*.arghoriz)";
    public static final String IMPORT_FILTER = "Horizons (*.png, *.arghoriz, track files *.dat)", TRACK_FILTER = "Another track's horizon (*.dat)";

    public static final Message NEEDS_GAME = Message.info(TITLE, "The horizon is drawn in the game's own colours, which are in its GP.EXE.\n"
        + "Choose the folder the game is installed in.");
    public static final Message NO_GAME_EXE = Message.warning(TITLE, "That folder does not have the game's GP.EXE.");
    public static final Message BAD_PICTURE = Message.warning("Import", "A horizon picture must be 512 by 8 pixels (or that size enlarged a whole number of\n"
        + "times, such as 1024 by 16). Nothing was changed.");

    private final Track track;
    public final byte[] palette;
    public final GameLiveries colours;
    private final byte[] work;
    private int current = TrackHorizon.SKY;

    public HorizonPage(Track track, byte[] palette)
    {
        this.track = track;
        this.palette = palette;
        this.colours = GameLiveries.ofPalette(palette);
        this.work = java.util.Arrays.copyOf(track.getHorizon(), TrackHorizon.BYTES);
    }

    /** The page for a track, with the palette of the GP.EXE beside it (or in one of the other folders), or null when none is found. */
    public static HorizonPage of(Track track, File... folders)
    {
        File exe = TrackHorizon.findGameExe(track.getFile(), folders);
        return exe == null ? null : new HorizonPage(track, TrackHorizon.readPalette(exe));
    }

    /** The picture as painted so far, one palette index per pixel. */
    public byte[] work()
    {
        return work;
    }

    /** The picture's colours (0xRRGGBB), 512 x 8. */
    public int[] rgb()
    {
        int[] out = new int[TrackHorizon.BYTES];
        for (int i = 0; i < out.length; i++)
            out[i] = colours.getPaletteRgb(work[i] & 0xFF);
        return out;
    }

    /** The height of a preview showing the whole picture across width pixels, at the shape the
        game draws it: one screen pixel per picture pixel each way, on a 320 by 200 screen shown
        4:3, so a picture pixel is 5 wide to 6 tall (2026-11-20). */
    public static int previewHeight(int width)
    {
        int across = TrackHorizon.WIDTH * 5;
        return Math.max(1, (width * TrackHorizon.HEIGHT * 6 + across / 2) / across);
    }

    /** The picture's column under x in a preview width pixels across. */
    public static int previewColumn(int x, int width)
    {
        if (width <= 0)
            return 0;
        return Math.max(0, Math.min(TrackHorizon.WIDTH - 1, (int) ((long) x * TrackHorizon.WIDTH / width)));
    }

    public int current()
    {
        return current;
    }

    public void setCurrent(int colour)
    {
        current = colour;
    }

    /** The colour button's tip. */
    public String colourTip()
    {
        return "The colour clicks paint in (" + current + "); choose another";
    }

    private static boolean inside(int x, int y)
    {
        return x >= 0 && x < TrackHorizon.WIDTH && y >= 0 && y < TrackHorizon.HEIGHT;
    }

    /** A click: the pixel in the chosen colour. */
    public void paint(int x, int y)
    {
        if (inside(x, y))
            work[y * TrackHorizon.WIDTH + x] = (byte) current;
    }

    /** A right-click: the pixel's colour becomes the chosen one. */
    public void pick(int x, int y)
    {
        if (inside(x, y))
            current = work[y * TrackHorizon.WIDTH + x] & 0xFF;
    }

    /** OK: the picture into the track; true if that changed it. */
    public boolean ok()
    {
        byte[] h = track.getHorizon();
        if (java.util.Arrays.equals(h, work))
            return false;
        System.arraycopy(work, 0, h, 0, TrackHorizon.BYTES);
        return true;
    }

    /** The name offered for an export: "F1CT03-horizon.png" or ".arghoriz". */
    public String exportName(boolean raw)
    {
        String base = track.getFile() == null ? "horizon" : track.getFile().getName().replaceAll("\\.[^.]*$", "");
        return base + "-horizon" + (raw ? ".arghoriz" : ".png");
    }

    /** Exported as an indexed PNG in the race palette (comes back exactly), or as ArgTrack's raw 4096 bytes. */
    public byte[] export(boolean raw) throws IOException
    {
        return raw ? work.clone() : TrackHorizon.toPng(work, palette);
    }

    public static Message notExported(String fileName)
    {
        return Message.error("Export", fileName + " could not be saved.");
    }

    /** Imports a PNG (by its name), or a .arghoriz or track file: the refusal (nothing changed), or null. */
    public Message importFile(String fileName, byte[] bytes)
    {
        boolean picture = fileName.toLowerCase().endsWith(".png");
        byte[] h = null;
        if (picture)
        {
            try
            {
                Picture img = bytes == null ? null : Pictures.codec().read(bytes);
                if (img != null)
                    h = TrackHorizon.fromPicture(img, palette);
            }
            catch (IOException e)
            {
                h = null;
            }
        }
        else
            h = TrackHorizon.fromHorizonFile(bytes);
        if (h == null)
            return picture ? BAD_PICTURE : Message.warning("Import", fileName + " is too short to hold a horizon. Nothing was changed.");
        System.arraycopy(h, 0, work, 0, TrackHorizon.BYTES);
        return null;
    }
}
