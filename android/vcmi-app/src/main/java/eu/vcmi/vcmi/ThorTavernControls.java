package eu.vcmi.vcmi;

/** Local control IDs never overlap the four published-row control IDs. */
final class ThorTavernControls
{
    static final int OFFERS = 1, DETAILS = 2, INVITATIONS = 3, RUMOR = 4;
    static final int PREVIOUS = 5, NEXT = 6, RECRUIT = 7, GUILD = 8, CLOSE = 9;
    static final int ROW_FIRST = 100, ROW_LAST = 103, PAGE_SIZE = 4, TEXT_PAGE = 160;

    static int count(ThorTavernState state, int tab)
    {
        int count = 0, flag = tab == INVITATIONS ? 2 : 1;
        for (int row = 0; row < state.count(); ++row) if ((state.flags(row) & flag) != 0) ++count;
        return count;
    }
    static int row(ThorTavernState state, int tab, int page, int control)
    {
        if ((tab != OFFERS && tab != INVITATIONS) || control < ROW_FIRST || control > ROW_LAST || page < 0) return -1;
        long target = (long) page * PAGE_SIZE + control - ROW_FIRST;
        int flag = tab == INVITATIONS ? 2 : 1;
        for (int row = 0; row < state.count(); ++row)
            if ((state.flags(row) & flag) != 0 && target-- == 0) return row;
        return -1;
    }
    static String text(ThorTavernState state, int tab)
    {
        if (tab == RUMOR) return state.rumor();
        for (int row = 0; row < state.count(); ++row) if (state.id(row) == state.selected()) return state.details(row);
        return "";
    }
    static int pages(ThorTavernState state, int tab)
    {
        if (tab == OFFERS || tab == INVITATIONS) return Math.max(1, (count(state, tab) + PAGE_SIZE - 1) / PAGE_SIZE);
        String text = text(state, tab);
        return Math.max(1, (text.codePointCount(0, text.length()) + TEXT_PAGE - 1) / TEXT_PAGE);
    }
    static String pageText(ThorTavernState state, int tab, int page)
    {
        String text = text(state, tab);
        int total = text.codePointCount(0, text.length());
        int start = text.offsetByCodePoints(0, (int) Math.min(total, Math.max(0L, (long) page * TEXT_PAGE)));
        int end = text.offsetByCodePoints(start, Math.min(TEXT_PAGE, text.codePointCount(start, text.length())));
        return text.substring(start, end);
    }
    static int command(int control, int tab)
    {
        if (control >= ROW_FIRST && control <= ROW_LAST)
            return tab == INVITATIONS ? ThorExtendedCommands.TAVERN_INVITE : ThorExtendedCommands.TAVERN_SELECT;
        if (control == RECRUIT) return ThorExtendedCommands.TAVERN_RECRUIT;
        if (control == GUILD) return ThorExtendedCommands.TAVERN_GUILD;
        if (control == CLOSE) return ThorExtendedCommands.TAVERN_CLOSE;
        return 0;
    }
    static int target(ThorTavernState state, int tab, int page, int control)
    {
        int row = row(state, tab, page, control);
        return row < 0 ? -1 : state.id(row);
    }
    static boolean enabled(ThorTavernState state, int tab, int page, int control)
    {
        if (state == ThorTavernState.EMPTY) return false;
        if (control >= OFFERS && control <= RUMOR) return true;
        if (control == PREVIOUS) return page > 0;
        if (control == NEXT) return page + 1 < pages(state, tab);
        return state.allows(command(control, tab), target(state, tab, page, control));
    }
    static float[] bounds(int control, ThorPresentationLayout layout)
    {
        float left = layout.margin + layout.bevel * 3, width = layout.right - layout.bevel * 3 - left;
        float top = layout.margin, height = layout.contentHeight;
        if (control >= OFFERS && control <= RUMOR)
        {
            float x = left + (control - OFFERS) * width / 4;
            return new float[]{x, top + height * .16f, x + width * .24f, top + height * .26f};
        }
        if (control >= ROW_FIRST && control <= ROW_LAST)
        {
            float y = top + height * (.29f + (control - ROW_FIRST) * .105f);
            return new float[]{left, y, left + width, y + height * .095f};
        }
        if (control == PREVIOUS || control == NEXT)
        {
            float x = left + (control == PREVIOUS ? 0 : width * .76f);
            return new float[]{x, top + height * .75f, x + width * .24f, top + height * .84f};
        }
        if (control >= RECRUIT && control <= CLOSE)
        {
            float x = left + (control - RECRUIT) * width / 3;
            return new float[]{x, top + height * .87f, x + width * .32f, top + height * .97f};
        }
        return null;
    }
    static int at(float x, float y, ThorPresentationLayout layout, int tab)
    {
        for (int control = OFFERS; control <= CLOSE; ++control) if (inside(x, y, bounds(control, layout))) return control;
        if (tab == OFFERS || tab == INVITATIONS)
            for (int control = ROW_FIRST; control <= ROW_LAST; ++control) if (inside(x, y, bounds(control, layout))) return control;
        return 0;
    }
    private static boolean inside(float x, float y, float[] b)
    { return b != null && x >= b[0] && y >= b[1] && x < b[2] && y < b[3]; }
}
