package eu.vcmi.vcmi;

/** Bounded current-page data and hit regions shared by the two native browser owners. */
final class ThorBrowserState
{
    static final int MAX_ROWS = 8;
    static final int SAVE_ROWS = 5;
    static final int CONTROL_NONE = 0;
    static final int CONTROL_FIRST_ROW = 1;
    static final int CONTROL_PREVIOUS = 9;
    static final int CONTROL_NEXT = 10;
    static final int CONTROL_PRIMARY = 11;
    static final int CONTROL_BACK = 12;
    static final ThorBrowserState EMPTY = new ThorBrowserState(0, 0, new int[0], new String[0], new int[0]);

    final int page;
    final int pageCount;
    final int[] targets;
    final String[] labels;
    final int[] flags;

    private ThorBrowserState(final int page, final int pageCount, final int[] targets,
                             final String[] labels, final int[] flags)
    {
        this.page = page;
        this.pageCount = pageCount;
        this.targets = targets;
        this.labels = labels;
        this.flags = flags;
    }

    static ThorBrowserState copyOf(final String contextId, final int page, final int pageCount,
                                   final int[] targets, final String[] labels, final int[] flags)
    {
        final boolean campaign = ThorContextIds.CAMPAIGN_BROWSER.equals(contextId);
        final boolean load = ThorContextIds.LOBBY_LOAD_GAME_SCENARIO.equals(contextId);
        if ((!campaign && !load) || page < 0 || pageCount < 0 || pageCount > 2000
                || (pageCount == 0 ? page != 0 : page >= pageCount)
                || targets == null || labels == null || flags == null
                || targets.length != labels.length || targets.length != flags.length
                || targets.length > (campaign ? MAX_ROWS : SAVE_ROWS)
                || (pageCount == 0 && targets.length != 0))
            return EMPTY;
        for (int index = 0; index < targets.length; ++index)
        {
            if (targets[index] < 0 || targets[index] > 10000 || labels[index] == null
                    || labels[index].length() > 128 || (flags[index] & ~7) != 0)
                return EMPTY;
            for (int earlier = 0; earlier < index; ++earlier)
                if (targets[index] == targets[earlier])
                    return EMPTY;
        }
        return new ThorBrowserState(page, pageCount, targets.clone(), labels.clone(), flags.clone());
    }

    int rowCount()
    {
        return targets.length;
    }

    boolean enabled(final int row)
    {
        return row >= 0 && row < flags.length && (flags[row] & 1) != 0;
    }

    boolean selected(final int row)
    {
        return row >= 0 && row < flags.length && (flags[row] & 2) != 0;
    }

    boolean completed(final int row)
    {
        return row >= 0 && row < flags.length && (flags[row] & 4) != 0;
    }

    static int actionForControl(final String contextId, final int control)
    {
        final boolean campaign = ThorContextIds.CAMPAIGN_BROWSER.equals(contextId);
        if (!campaign && !ThorContextIds.LOBBY_LOAD_GAME_SCENARIO.equals(contextId))
            return ThorActionIds.NONE;
        if (control >= CONTROL_FIRST_ROW && control < CONTROL_FIRST_ROW + MAX_ROWS)
            return campaign ? ThorActionIds.CAMPAIGN_BROWSER_SELECT : ThorActionIds.LOAD_BROWSER_SELECT;
        if (control == CONTROL_PREVIOUS)
            return campaign ? ThorActionIds.CAMPAIGN_BROWSER_PREVIOUS_PAGE : ThorActionIds.LOAD_BROWSER_PREVIOUS_PAGE;
        if (control == CONTROL_NEXT)
            return campaign ? ThorActionIds.CAMPAIGN_BROWSER_NEXT_PAGE : ThorActionIds.LOAD_BROWSER_NEXT_PAGE;
        if (control == CONTROL_PRIMARY)
            return campaign ? ThorActionIds.NONE : ThorActionIds.LOBBY_START_GAME;
        if (control == CONTROL_BACK)
            return campaign ? ThorActionIds.CAMPAIGN_BROWSER_BACK : ThorActionIds.LOBBY_BACK;
        return ThorActionIds.NONE;
    }

    static float[] boundsForControl(final String contextId, final int control,
                                    final float width, final float height)
    {
        final boolean campaign = ThorContextIds.CAMPAIGN_BROWSER.equals(contextId);
        final boolean load = ThorContextIds.LOBBY_LOAD_GAME_SCENARIO.equals(contextId);
        if ((!campaign && !load) || width <= 0f || height <= 0f)
            return new float[]{0f, 0f, 0f, 0f};
        final float side = width * 0.055f;
        final float gap = width * 0.018f;
        if (control >= CONTROL_FIRST_ROW && control < CONTROL_FIRST_ROW + (campaign ? MAX_ROWS : SAVE_ROWS))
        {
            final int row = control - CONTROL_FIRST_ROW;
            final int columns = campaign ? 2 : 1;
            final int line = row / columns;
            final int column = row % columns;
            final float cellWidth = (width - side * 2f - gap * (columns - 1)) / columns;
            final float lineHeight = height * (campaign ? 0.143f : 0.114f);
            final float top = height * 0.215f + line * lineHeight;
            final float left = side + column * (cellWidth + gap);
            return new float[]{left, top, left + cellWidth, top + lineHeight * 0.92f};
        }
        if (control >= CONTROL_PREVIOUS && control <= CONTROL_BACK)
        {
            final int index = control - CONTROL_PREVIOUS;
            if (campaign && control == CONTROL_PRIMARY)
                return new float[]{0f, 0f, 0f, 0f};
            final float buttonWidth = (width - side * 2f - gap) / 2f;
            if (campaign && control == CONTROL_BACK)
                return new float[]{side, height * 0.91f, width - side, height * 0.985f};
            final int column = index % 2;
            final int line = index / 2;
            final float top = height * (campaign ? 0.82f : 0.81f) + line * height * 0.085f;
            final float bottom = top + height * 0.077f;
            return new float[]{side + column * (buttonWidth + gap), top,
                    side + column * (buttonWidth + gap) + buttonWidth, bottom};
        }
        return new float[]{0f, 0f, 0f, 0f};
    }

    static int controlAt(final String contextId, final float x, final float y,
                         final float width, final float height, final int rowCount)
    {
        for (int row = 0; row < rowCount; ++row)
        {
            final float[] bounds = boundsForControl(contextId, CONTROL_FIRST_ROW + row, width, height);
            if (x >= bounds[0] && x <= bounds[2] && y >= bounds[1] && y <= bounds[3])
                return CONTROL_FIRST_ROW + row;
        }
        for (int control = CONTROL_PREVIOUS; control <= CONTROL_BACK; ++control)
        {
            final float[] bounds = boundsForControl(contextId, control, width, height);
            if (bounds[2] > bounds[0] && x >= bounds[0] && x <= bounds[2]
                    && y >= bounds[1] && y <= bounds[3])
                return control;
        }
        return CONTROL_NONE;
    }
}
