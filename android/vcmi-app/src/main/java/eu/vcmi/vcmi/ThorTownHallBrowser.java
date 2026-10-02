package eu.vcmi.vcmi;

/** Presentation-local paging and selection for one immutable Town Hall revision. */
final class ThorTownHallBrowser
{
    static final int PAGE_SIZE = 5;
    private ThorBrowserState rows = ThorBrowserState.EMPTY;
    private int page;
    private int selectedTarget = ThorActionIds.NO_TARGET;
    private long revision;
    private long session;
    private String contextId = ThorContextIds.UNKNOWN;

    void update(final String context, final long newRevision, final long newSession,
                final ThorBrowserState newRows)
    {
        if (!ThorContextIds.TOWN_HALL.equals(context) || newRevision <= 0L || newSession <= 0L
                || newRows == null || newRows.rowCount() > ThorBrowserState.MAX_ROWS)
        {
            reset();
            return;
        }
        if (!contextId.equals(context) || revision != newRevision || session != newSession)
        {
            page = 0;
            selectedTarget = ThorActionIds.NO_TARGET;
        }
        contextId = context;
        revision = newRevision;
        session = newSession;
        rows = newRows;
        page = rows.pageCount == 0 ? 0 : Math.min(page, rows.pageCount - 1);
        if (selectedTarget != ThorActionIds.NO_TARGET && rowForTarget(selectedTarget) < 0)
            selectedTarget = ThorActionIds.NO_TARGET;
    }

    void reset()
    {
        rows = ThorBrowserState.EMPTY;
        page = 0;
        selectedTarget = ThorActionIds.NO_TARGET;
        revision = 0L;
        session = 0L;
        contextId = ThorContextIds.UNKNOWN;
    }

    int page() { return page; }
    int pageCount() { return rows.pageCount; }
    int selectedTarget() { return selectedTarget; }
    int rowCount() { return Math.min(PAGE_SIZE, Math.max(0, rows.rowCount() - page * PAGE_SIZE)); }

    int targetAt(final int visibleRow)
    {
        final int index = page * PAGE_SIZE + visibleRow;
        return visibleRow >= 0 && visibleRow < rowCount() ? rows.targets[index] : ThorActionIds.NO_TARGET;
    }

    String labelAt(final int visibleRow)
    {
        final int index = page * PAGE_SIZE + visibleRow;
        return visibleRow >= 0 && visibleRow < rowCount() ? rows.labels[index] : "";
    }

    boolean buildableAt(final int visibleRow)
    {
        final int index = page * PAGE_SIZE + visibleRow;
        return visibleRow >= 0 && visibleRow < rowCount() && rows.enabled(index) && !rows.completed(index);
    }

    boolean builtAt(final int visibleRow)
    {
        final int index = page * PAGE_SIZE + visibleRow;
        return visibleRow >= 0 && visibleRow < rowCount() && rows.completed(index);
    }

    boolean select(final int target)
    {
        if (rowForTarget(target) < 0)
            return false;
        selectedTarget = target;
        return true;
    }

    boolean isSelectedAt(final int visibleRow)
    {
        return targetAt(visibleRow) == selectedTarget && selectedTarget != ThorActionIds.NO_TARGET;
    }

    boolean canBuildSelected()
    {
        final int row = rowForTarget(selectedTarget);
        return row >= 0 && rows.enabled(row) && !rows.completed(row);
    }

    boolean previousPage()
    {
        if (page <= 0)
            return false;
        --page;
        selectedTarget = ThorActionIds.NO_TARGET;
        return true;
    }

    boolean nextPage()
    {
        if (page + 1 >= rows.pageCount)
            return false;
        ++page;
        selectedTarget = ThorActionIds.NO_TARGET;
        return true;
    }

    private int rowForTarget(final int target)
    {
        for (int index = 0; index < rows.rowCount(); ++index)
            if (rows.targets[index] == target)
                return index;
        return -1;
    }
}
