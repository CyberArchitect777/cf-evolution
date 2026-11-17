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

package cfevolution.screens;

import cfevolution.edit.Message;

/**
 * A conversation with the user (2026-11-17): show message(), get an answer,
 * pass it to answer(), and repeat until message() is null. Android's dialogs
 * do not wait, so the screen models ask one question at a time like this
 * rather than calling back into the interface.
 *
 * Answers: for a message that is only acknowledged, OK (0); for a yes/no
 * question, YES (0) or NO (1); for one with options of its own, the index of
 * the button pressed.
 */
public abstract class Flow
{
    public static final int OK = 0, YES = 0, NO = 1;

    private Message message;

    /** What to show now, or null when the conversation is over. */
    public final Message message()
    {
        return message;
    }

    public final boolean done()
    {
        return message == null;
    }

    /** The user's answer to message(). */
    public final void answer(int choice)
    {
        Message asked = message;
        message = null;
        if (asked != null)
            next(asked, choice);
    }

    /** Shows this next. */
    protected final void show(Message m)
    {
        message = m;
    }

    /** Called with the answer to what was shown; show the next message, or nothing to finish. */
    protected abstract void next(Message asked, int choice);

    /** A conversation of messages that only need acknowledging, one after
        another; nulls are skipped. With none, it is over at once. */
    public static Flow tell(Message... messages)
    {
        return new Tell(messages);
    }

    private static final class Tell extends Flow
    {
        private final java.util.List<Message> left = new java.util.ArrayList<Message>();

        Tell(Message[] messages)
        {
            for (Message m : messages)
                if (m != null)
                    left.add(m);
            if (!left.isEmpty())
                show(left.remove(0));
        }

        protected void next(Message asked, int choice)
        {
            if (!left.isEmpty())
                show(left.remove(0));
        }
    }
}
