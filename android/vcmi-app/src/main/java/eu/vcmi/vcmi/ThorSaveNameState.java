package eu.vcmi.vcmi;

import java.nio.charset.StandardCharsets;

/** One transient draft. Only its opening revision/session may submit, once. */
final class ThorSaveNameState
{
    static final int MAX_BYTES = 240;
    private long revision, session;
    private boolean active;

    void begin(final long revision, final long session)
    {
        this.revision = revision;
        this.session = session;
        active = revision > 0 && session > 0;
    }

    static boolean valid(final String text)
    {
        if (text == null || text.isEmpty() || text.length() > MAX_BYTES || text.equals(".") || text.equals("..")
                || text.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) return false;
        for (int i = 0; i < text.length(); ++i)
        {
            final char c = text.charAt(i);
            if (c < 32 || c == 127 || Character.isLowSurrogate(c)) return false;
            if (Character.isHighSurrogate(c)
                    && (++i >= text.length() || !Character.isLowSurrogate(text.charAt(i)))) return false;
        }
        return true;
    }

    boolean current(final long revision, final long session)
    {
        return active && this.revision == revision && this.session == session;
    }

    boolean commit(final String text, final long revision, final long session)
    {
        if (!current(revision, session) || !valid(text)) return false;
        active = false;
        return true;
    }

    void cancel() { active = false; }
}
