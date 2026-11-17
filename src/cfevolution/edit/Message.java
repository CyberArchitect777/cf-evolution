package cfevolution.edit;

/**
 * Something the editor tells or asks the user, as plain data, so every
 * interface shows the same words (2026-11-16). The text is plain language
 * (owner, 2026-10-11) and keeps the line breaks the desktop's message boxes
 * use; another interface may let its own dialogs rewrap it.
 */
public final class Message
{
    /** How it looks: news, a warning, an error, or a question. */
    public enum Kind { INFO, WARNING, ERROR, QUESTION }

    public final Kind kind;
    public final String title, text;
    /** True when the user answers Yes or No; otherwise it is only acknowledged. */
    public final boolean yesNo;
    /** Buttons of its own, first the usual choice, when the answer is one of these; else null. */
    public final String[] options;

    public Message(Kind kind, String title, String text, boolean yesNo)
    {
        this(kind, title, text, yesNo, null);
    }

    private Message(Kind kind, String title, String text, boolean yesNo, String[] options)
    {
        this.kind = kind;
        this.title = title;
        this.text = text;
        this.yesNo = yesNo;
        this.options = options;
    }

    /** A question answered with one of these buttons; the interface reports which (0, 1, ...). */
    public static Message choose(Kind look, String title, String text, String... options)
    {
        return new Message(look, title, text, false, options.clone());
    }

    public static Message info(String title, String text)    { return new Message(Kind.INFO, title, text, false); }
    public static Message warning(String title, String text) { return new Message(Kind.WARNING, title, text, false); }
    public static Message error(String title, String text)   { return new Message(Kind.ERROR, title, text, false); }

    /** A yes/no question shown with this look (QUESTION, or WARNING for one that cannot be undone). */
    public static Message ask(Kind look, String title, String text) { return new Message(look, title, text, true); }

    public String toString()
    {
        return kind + (yesNo ? " (yes/no) " : " ") + title + ": " + text;
    }
}
