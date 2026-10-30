package eu.vcmi.vcmi;

/** Android-only filtering and browsing; casting is always a native request. */
final class ThorSpellbookJourney
{
    static final int MODE = 1, SCHOOL_PREVIOUS = 2, SCHOOL_NEXT = 3, LEVEL_PREVIOUS = 4, LEVEL_NEXT = 5,
            PREVIOUS = 6, NEXT = 7, CAST = 8, LIST = 9, CLOSE = 10, ROW_FIRST = 100, ROW_LAST = 103;
    static final int PAGE_SIZE = 4, TEXT_PAGE = 120;
    private final ThorControlGesture gesture = new ThorControlGesture(0);
    ThorSpellbookState state = ThorSpellbookState.EMPTY;
    int level = -1, school = -1, page, selected = -1, textPage;
    boolean combat, submitted;
    private int hero = -1;

    void cancel() { gesture.cancel(); submitted = false; }
    void withdraw() { cancel(); state = ThorSpellbookState.EMPTY; }
    void reset() { withdraw(); hero = selected = school = level = -1; page = textPage = 0; combat = false; }
    void update(ThorSpellbookState next)
    {
        cancel();
        if (next == ThorSpellbookState.EMPTY) { withdraw(); return; }
        if (hero != next.hero) { reset(); combat = next.battle; }
        hero = next.hero; state = next;
        boolean foundSchool = school == -1;
        for (int i = 0; i < state.schoolCount(); ++i) if (state.school(i) == school) foundSchool = true;
        if (!foundSchool) school = -1;
        if (selectedRow() < 0) { selected = -1; textPage = 0; }
        page = Math.min(page, pages() - 1); textPage = Math.min(textPage, textPages() - 1);
    }
    boolean matches(int i)
    {
        ThorSpellbookState.Row row = state.row(i);
        return row.combat == combat && (level < 0 || row.level == level) && (school < 0 || row.hasSchool(school));
    }
    int selectedRow()
    { for (int i = 0; i < state.count(); ++i) if (state.row(i).id == selected && matches(i)) return i; return -1; }
    int count() { int n = 0; for (int i = 0; i < state.count(); ++i) if (matches(i)) ++n; return n; }
    int pages() { return Math.max(1, (count() + PAGE_SIZE - 1) / PAGE_SIZE); }
    int row(int control)
    {
        if (control < ROW_FIRST || control > ROW_LAST || selected >= 0 || page < 0 || page >= pages()) return -1;
        int n = page * PAGE_SIZE + control - ROW_FIRST;
        for (int i = 0; i < state.count(); ++i) if (matches(i) && n-- == 0) return i;
        return -1;
    }
    int schoolIndex() { for (int i = 0; i < state.schoolCount(); ++i) if (state.school(i) == school) return i; return -1; }
    String details() { int i = selectedRow(); return i < 0 ? "" : state.row(i).details.replaceAll("\\s+", " "); }
    int textPages() { String s = details(); return Math.max(1, (s.codePointCount(0, s.length()) + TEXT_PAGE - 1) / TEXT_PAGE); }
    String pageText()
    {
        String s = details(); int n = s.codePointCount(0, s.length());
        if (textPage < 0 || textPage >= textPages()) return "";
        return s.substring(s.offsetByCodePoints(0, Math.min(n, textPage * TEXT_PAGE)),
                s.offsetByCodePoints(0, Math.min(n, (textPage + 1) * TEXT_PAGE)));
    }
    boolean enabled(int control)
    {
        if (state == ThorSpellbookState.EMPTY || submitted) return false;
        if (control >= ROW_FIRST && control <= ROW_LAST) return row(control) >= 0;
        switch (control)
        {
            case MODE: return selected < 0;
            case SCHOOL_PREVIOUS: return selected < 0 && schoolIndex() >= 0;
            case SCHOOL_NEXT: return selected < 0 && schoolIndex() + 1 < state.schoolCount();
            case LEVEL_PREVIOUS: return selected < 0 && level >= 0;
            case LEVEL_NEXT: return selected < 0 && level < 5;
            case PREVIOUS: return selected >= 0 ? textPage > 0 : page > 0;
            case NEXT: return selected >= 0 ? textPage + 1 < textPages() : page + 1 < pages();
            case CAST: return selected >= 0 && state.allows(ThorExtendedCommands.SPELLBOOK_CAST, selected);
            case LIST: return selected >= 0;
            case CLOSE: return state.allows(ThorExtendedCommands.SPELLBOOK_CLOSE, -1);
            default: return false;
        }
    }
    void begin(int control, long revision, long session, int pointer, int pointers)
    { gesture.begin(ThorContextIds.SPELLBOOK, control, revision, session, pointer, pointers == 1 && revision == state.revision && enabled(control)); }
    int[] release(int control, long revision, long session, int pointer, int pointers, boolean current)
    {
        if (!gesture.finish(ThorContextIds.SPELLBOOK, control, pointers, pointer, revision, session,
                revision == state.revision && enabled(control), current)) return null;
        if (control >= ROW_FIRST && control <= ROW_LAST) { selected = state.row(row(control)).id; textPage = 0; }
        else if (control == LIST) { selected = -1; textPage = 0; }
        else if (control == MODE) { combat = !combat; page = textPage = 0; }
        else if (control == LEVEL_PREVIOUS || control == LEVEL_NEXT)
        { level += control == LEVEL_PREVIOUS ? -1 : 1; page = textPage = 0; }
        else if (control == SCHOOL_PREVIOUS || control == SCHOOL_NEXT)
        {
            int i = schoolIndex() + (control == SCHOOL_PREVIOUS ? -1 : 1);
            school = i < 0 ? -1 : state.school(i); page = textPage = 0;
        }
        else if (control == PREVIOUS || control == NEXT)
        { int delta = control == PREVIOUS ? -1 : 1; if (selected >= 0) textPage += delta; else page += delta; }
        else return new int[]{control == CLOSE ? ThorExtendedCommands.SPELLBOOK_CLOSE : ThorExtendedCommands.SPELLBOOK_CAST, control == CLOSE ? -1 : selected};
        return null;
    }
}
