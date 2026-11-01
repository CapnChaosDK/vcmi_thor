package eu.vcmi.vcmi;

/** Native page navigation, local reading mode and one-shot gesture/session boundary. */
final class ThorReferenceJourney
{
    static final int QUESTS = 1, EVENTS = 2, PREVIOUS = 3, NEXT = 4, READ = 5, LIST = 6,
            CENTER = 7, CLOSE = 8, ROW_FIRST = 100, ROW_LAST = 104;
    private final ThorControlGesture gesture = new ThorControlGesture(0);
    ThorReferenceState state = ThorReferenceState.EMPTY;
    boolean reading, submitted;
    private int kind, selected = -1, nativeTextPage;
    int localPage, localPages = 1;
    private boolean lastPage;
    void cancel() { gesture.cancel(); }
    void withdraw() { cancel(); submitted = false; state = ThorReferenceState.EMPTY; }
    void reset() { withdraw(); reading = false; kind = 0; selected = -1; localPage = 0; localPages = 1; lastPage = false; }
    void update(ThorReferenceState next)
    {
        cancel(); submitted = false;
        if (next == ThorReferenceState.EMPTY || next.kind() != kind) reading = false;
        if (next.selected() != selected || next.textPage() != nativeTextPage)
            localPage = lastPage ? Integer.MAX_VALUE : 0;
        lastPage = false;
        selected = next.selected(); nativeTextPage = next.textPage();
        state = next;
        kind = next.kind();
    }
    int command(int control)
    {
        switch (control)
        {
            case QUESTS: return ThorExtendedCommands.REFERENCE_QUESTS;
            case EVENTS: return ThorExtendedCommands.REFERENCE_EVENTS;
            case PREVIOUS: return reading ? ThorExtendedCommands.REFERENCE_TEXT_PREVIOUS : ThorExtendedCommands.REFERENCE_PREVIOUS;
            case NEXT: return reading ? ThorExtendedCommands.REFERENCE_TEXT_NEXT : ThorExtendedCommands.REFERENCE_NEXT;
            case CENTER: return ThorExtendedCommands.REFERENCE_CENTER;
            case CLOSE: return ThorExtendedCommands.REFERENCE_CLOSE;
            default: return control >= ROW_FIRST && control <= ROW_LAST ? ThorExtendedCommands.REFERENCE_SELECT : 0;
        }
    }
    int target(int control)
    { int row = control - ROW_FIRST; return row >= 0 && row < state.count() ? state.target(row) : -1; }
    boolean enabled(int control)
    {
        if (state == ThorReferenceState.EMPTY || submitted) return false;
        if (control == READ) return state.selected() >= 0 && state.textPages() > 0;
        if (control == LIST) return reading;
        if (reading && control == NEXT && localPage < localPages - 1) return true;
        if (reading && control == PREVIOUS && localPage > 0) return true;
        return state.allows(command(control), target(control));
    }
    void begin(int control, long revision, long session, int pointer, int count)
    { cancel(); if (count == 1) gesture.begin(state.context(), control, revision, session, pointer, enabled(control) && revision == state.revision); }
    int[] release(int control, long revision, long session, int pointer, int count, boolean current)
    {
        if (!gesture.finish(state.context(), control, count, pointer, revision, session, enabled(control), current)) return null;
        if (control == READ) { reading = true; return null; }
        if (control == LIST) { reading = false; return null; }
        if (reading && control == NEXT && localPage < localPages - 1) { ++localPage; return null; }
        if (reading && control == PREVIOUS && localPage > 0) { --localPage; return null; }
        if (reading && control == PREVIOUS) lastPage = true;
        return new int[]{command(control), target(control)};
    }
}
