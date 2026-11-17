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

package cfevolution.gui;

import cfevolution.edit.Message;
import java.awt.Component;
import javax.swing.JOptionPane;

/** Shows the core's Messages as Swing message boxes (2026-11-16). */
public final class MessageBoxes
{
    private MessageBoxes()
    {
    }

    private static int look(Message.Kind kind)
    {
        switch (kind)
        {
            case WARNING:  return JOptionPane.WARNING_MESSAGE;
            case ERROR:    return JOptionPane.ERROR_MESSAGE;
            case QUESTION: return JOptionPane.QUESTION_MESSAGE;
            default:       return JOptionPane.INFORMATION_MESSAGE;
        }
    }

    /** Shows a message to acknowledge; does nothing for null. */
    public static void show(Component parent, Message m)
    {
        if (m != null)
            JOptionPane.showMessageDialog(parent, m.text, m.title, look(m.kind));
    }

    /** Shows a refusal, if there is one; true when there was. */
    public static boolean showRefusal(Component parent, Message m)
    {
        show(parent, m);
        return m != null;
    }

    /** Asks with the message's own buttons (Message.choose); the button chosen, the first being 0, or -1 if closed. */
    public static int choose(Component parent, Message m)
    {
        return JOptionPane.showOptionDialog(parent, m.text, m.title, JOptionPane.DEFAULT_OPTION, look(m.kind), null,
                                            m.options, m.options[0]);
    }

    /** Asks a yes/no question; true for Yes. */
    public static boolean ask(Component parent, Message m)
    {
        return JOptionPane.showConfirmDialog(parent, m.text, m.title, JOptionPane.YES_NO_OPTION, look(m.kind))
               == JOptionPane.YES_OPTION;
    }
}
