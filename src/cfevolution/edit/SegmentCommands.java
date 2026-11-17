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

import cfevolution.data.track.Command;
import cfevolution.data.track.CommandData;
import cfevolution.data.track.CommandDataStore;
import cfevolution.data.track.TrackSegment;
import java.util.Vector;

/**
 * The commands of a track or pit segment (2026-11-17, from the desktop's
 * CommandEditGUI, word for word): the list and its count, the commands that
 * can be added, adding and removing one (straight away, like adding a
 * segment), and a command's parameters as a PropertySheet.
 */
public final class SegmentCommands
{
    public static final String NONE_PICKED = "Select a track or pit segment to see its commands.";
    public static final String NO_COMMANDS = "This segment has no commands.";
    public static final String PARAMETERS = "Parameters", NO_PARAMETERS = "Parameters: this command has none";
    public static final String ADD = "Add", REMOVE = "Remove";
    public static final String ADD_TIP = "Add the chosen command to this segment";
    public static final String REMOVE_TIP = "Remove the selected command from this segment";

    private static final CommandDataStore STORE = new CommandDataStore();

    private SegmentCommands()
    {
    }

    public static String hex(int value)
    {
        return "0x" + Integer.toHexString(value).toUpperCase();
    }

    /** A command as the list names it. A type the editor does not know is named after the first
        known command, as the desktop's list always has done. */
    public static String describe(int type)
    {
        CommandData data = STORE.getCommandByType(type);
        if (data == null)
            return hex(type);
        return data.getCommandDesc() + "  (" + hex(type) + ")";
    }

    /** The commands that can be added, in order. */
    public static String[] available()
    {
        String[] out = new String[STORE.getCommandListSize()];
        for (int x = 0; x < out.length; x++)
        {
            CommandData data = STORE.getCommandByIndex(x);
            out[x] = data.getCommandDesc() + "  (" + hex(data.getCommandType()) + ")";
        }
        return out;
    }

    /** The segment's commands as the list shows them (empty for null). */
    public static String[] list(TrackSegment s)
    {
        if (s == null)
            return new String[0];
        Vector<Command> c = s.getCommands();
        String[] out = new String[c.size()];
        for (int x = 0; x < out.length; x++)
            out[x] = describe(c.get(x).getType());
        return out;
    }

    /** The line above the list. */
    public static String countText(TrackSegment s)
    {
        if (s == null)
            return NONE_PICKED;
        int n = s.getCommands().size();
        return n == 0 ? NO_COMMANDS : n + (n == 1 ? " command" : " commands");
    }

    /** The heading over a command's parameters (index -1: none picked). */
    public static String paramHeading(TrackSegment s, int index)
    {
        if (s == null || index < 0 || index >= s.getCommands().size())
            return PARAMETERS;
        CommandData data = STORE.getCommandByType(s.getCommands().get(index).getType());
        if (data == null || data.getNumberOfParams() == 0)
            return NO_PARAMETERS;
        return "Parameters of " + data.getCommandDesc();
    }

    /** A command's parameters, or null when it has none (or none is picked). */
    public static PropertySheet params(TrackSegment s, int index)
    {
        if (s == null || index < 0 || index >= s.getCommands().size())
            return null;
        Command c = s.getCommands().get(index);
        CommandData data = STORE.getCommandByType(c.getType());
        if (data == null || data.getNumberOfParams() == 0)
            return null;
        return PropertySheet.command(c, data.getParamArray());
    }

    /** Adds available()[choice], all parameters 0, at the end; returns its place in the list. */
    public static int add(TrackSegment s, int choice)
    {
        CommandData data = STORE.getCommandByIndex(choice);
        Vector<Command> c = new Vector<Command>(s.getCommands());
        c.add(new Command(data.getCommandType(), 0, 0, 0, 0, 0, 0));
        s.setCommands(c);
        return c.size() - 1;
    }

    public static void remove(TrackSegment s, int index)
    {
        Vector<Command> c = new Vector<Command>(s.getCommands());
        c.remove(index);
        s.setCommands(c);
    }
}
