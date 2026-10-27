package eu.vcmi.vcmi;

/** Local list/level/detail navigation and one-shot revision/session-bound input. */
final class ThorTownServiceJourney
{
    static final int LEVEL_PREVIOUS = 1, LEVEL_NEXT = 2, PREVIOUS = 3, NEXT = 4,
            ACT = 5, LIST = 6, CLOSE = 7, ROW_FIRST = 100, ROW_LAST = 103;
    static final int PAGE_SIZE = 4, TEXT_PAGE = 160;
    private final ThorControlGesture gesture = new ThorControlGesture(0);
    ThorTownServiceState state = ThorTownServiceState.EMPTY;
    int level, page, selected = -1, textPage;
    private int owner = -1, kind;
    boolean submitted;

    void cancel() { gesture.cancel(); submitted = false; }
    void withdraw() { cancel(); state = ThorTownServiceState.EMPTY; }
    void reset() { withdraw(); level = page = textPage = kind = 0; selected = owner = -1; }
    void update(ThorTownServiceState next)
    {
        cancel(); submitted = false;
        if (next == ThorTownServiceState.EMPTY || kind != next.kind() || owner != next.owner()) reset();
        state = next;
        kind = next.kind(); owner = next.owner();
        if (state.kind() == ThorTownServiceState.BLACKSMITH) selected = state.id(0);
        if (selectedRow() < 0) { selected = -1; textPage = 0; }
        page = Math.min(page, pages() - 1);
        textPage = Math.min(textPage, textPages() - 1);
    }
    int selectedRow()
    {
        for (int row = 0; row < state.count(); ++row) if (state.id(row) == selected) return row;
        return -1;
    }
    int count()
    {
        int count = 0;
        for (int row = 0; row < state.count(); ++row) if (level == 0 || state.level(row) == level) ++count;
        return count;
    }
    int pages() { return Math.max(1, (count() + PAGE_SIZE - 1) / PAGE_SIZE); }
    int row(int control)
    {
        if (control < ROW_FIRST || control > ROW_LAST || page < 0 || page >= pages() || selected >= 0) return -1;
        int index = page * PAGE_SIZE + control - ROW_FIRST;
        for (int row = 0; row < state.count(); ++row)
            if ((level == 0 || state.level(row) == level) && index-- == 0) return row;
        return -1;
    }
    String details()
    {
        int row = selectedRow();
        return (row < 0 ? state.summary() : state.details(row)).replaceAll("\\s+", " ");
    }
    int textPages() { return Math.max(1, (details().codePointCount(0, details().length()) + TEXT_PAGE - 1) / TEXT_PAGE); }
    String pageText()
    {
        String value = details();
        int count = value.codePointCount(0, value.length());
        if (textPage < 0 || textPage >= textPages()) return "";
        return value.substring(value.offsetByCodePoints(0, Math.min(count, textPage * TEXT_PAGE)),
                value.offsetByCodePoints(0, Math.min(count, (textPage + 1) * TEXT_PAGE)));
    }
    int command() { return state.kind() == ThorTownServiceState.BLACKSMITH ? ThorExtendedCommands.BLACKSMITH_BUY : ThorExtendedCommands.GUILD_RESEARCH; }
    boolean enabled(int control)
    {
        if (state == ThorTownServiceState.EMPTY || submitted) return false;
        if (control >= ROW_FIRST && control <= ROW_LAST) return row(control) >= 0;
        switch (control)
        {
            case LEVEL_PREVIOUS: return state.kind() == ThorTownServiceState.GUILD && selected < 0 && level > 0;
            case LEVEL_NEXT: return state.kind() == ThorTownServiceState.GUILD && selected < 0 && level < 5;
            case PREVIOUS: return selected >= 0 || count() == 0 ? textPage > 0 : page > 0;
            case NEXT: return selected >= 0 || count() == 0 ? textPage + 1 < textPages() : page + 1 < pages();
            case ACT: return selected >= 0 && state.allows(command(), selected);
            case LIST: return state.kind() == ThorTownServiceState.GUILD && selected >= 0;
            case CLOSE: return state.allows(ThorExtendedCommands.SERVICE_CLOSE, -1);
            default: return false;
        }
    }
    void begin(int control, long revision, long session, int pointer, int pointers)
    { gesture.begin(state.context(), control, revision, session, pointer, pointers == 1 && revision == state.revision && enabled(control)); }

    int[] release(int control, long revision, long session, int pointer, int pointers, boolean current)
    {
        if (!gesture.finish(state.context(), control, pointers, pointer, revision, session,
                revision == state.revision && enabled(control), current)) return null;
        if (control >= ROW_FIRST && control <= ROW_LAST) { selected = state.id(row(control)); textPage = 0; }
        else if (control == LIST) { selected = -1; textPage = 0; }
        else if (control == LEVEL_PREVIOUS || control == LEVEL_NEXT)
        { level += control == LEVEL_PREVIOUS ? -1 : 1; page = textPage = 0; }
        else if (control == PREVIOUS || control == NEXT)
        {
            int delta = control == PREVIOUS ? -1 : 1;
            if (selected >= 0 || count() == 0) textPage += delta; else page += delta;
        }
        else return new int[]{control == CLOSE ? ThorExtendedCommands.SERVICE_CLOSE : command(), control == CLOSE ? -1 : selected};
        return null;
    }
}
