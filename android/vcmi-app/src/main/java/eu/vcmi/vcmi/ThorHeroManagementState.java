package eu.vcmi.vcmi;

/** Local Hero deck state. Native owners still validate every submitted pair. */
final class ThorHeroManagementState
{
    ThorHeroManagement management = ThorHeroManagement.EMPTY;
    int tab;
    int page;
    private int ownerId = -1;
    int selectedArmy = -1;
    int selectedArtifact = -1;
    private long revision;
    private String contextId = ThorContextIds.UNKNOWN;
    private long mask;
    boolean touching;
    private boolean cancelled;
    private int pointerId = -1;
    private long touchRevision;
    int pressedTab = -1;

    void updateContext(long revision, String contextId, long mask)
    {
        if (revision != this.revision || !contextId.equals(this.contextId))
        {
            clear();
            if (!ThorContextIds.HERO_WINDOW.equals(contextId))
            {
                ownerId = -1;
                tab = 0;
                page = 0;
            }
        }
        this.revision = revision;
        this.contextId = contextId;
        this.mask = mask;
    }

    void update(ThorHeroManagement updated)
    {
        cancelled = touching;
        selectedArmy = -1;
        selectedArtifact = -1;
        management = ThorContextIds.HERO_WINDOW.equals(contextId) && updated.complete()
                ? updated : ThorHeroManagement.EMPTY;
        if (ownerId != management.heroId)
        {
            tab = 0;
            page = 0;
            ownerId = management.heroId;
        }
        page = Math.min(page, pageCount() - 1);
    }

    void clear()
    {
        cancel();
        management = ThorHeroManagement.EMPTY;
    }

    void cancel()
    {
        touching = false;
        cancelled = false;
        pointerId = -1;
        touchRevision = 0;
        pressedTab = -1;
        selectedArmy = -1;
        selectedArtifact = -1;
    }

    void begin(int pointerId, int pressedTab)
    {
        touching = true;
        cancelled = false;
        touchRevision = revision;
        this.pointerId = pointerId;
        this.pressedTab = pressedTab;
    }

    boolean acceptsPointer(int pointerId, int count)
    {
        if (!ThorContextIds.HERO_WINDOW.equals(contextId) || revision != touchRevision
                || count != 1 || pointerId != this.pointerId)
            cancelled = true;
        return !cancelled;
    }

    boolean finish(boolean valid, boolean sessionCurrent)
    {
        final boolean accepted = touching && !cancelled && valid && sessionCurrent
                && revision == touchRevision && ThorContextIds.HERO_WINDOW.equals(contextId);
        touching = false;
        cancelled = false;
        pointerId = -1;
        pressedTab = -1;
        return accepted;
    }

    void selectTab(int tab)
    {
        if (tab < 0 || tab > 2) return;
        this.tab = tab;
        selectedArmy = -1;
        selectedArtifact = -1;
    }

    int pageCount() { return Math.max(1, (management.artifactPositions.length + 5) / 6); }
    void changePage(int delta) { page = Math.max(0, Math.min(pageCount() - 1, page + delta)); }

    int armyPair(int slot)
    {
        if (!management.complete() || slot < 0 || slot >= ThorHeroManagement.ARMY_SIZE)
            return ThorHeroManagementPair.INVALID;
        if (selectedArmy < 0)
        {
            if (management.creatureIds[slot] >= 0) selectedArmy = slot;
            return ThorHeroManagementPair.INVALID;
        }
        final int pair = ThorHeroManagementPair.encodeArmy(selectedArmy, slot);
        selectedArmy = -1;
        return ThorLobbyScenarioState.isActionEnabled(mask, ThorActionIds.HERO_WINDOW_TRANSFER_STACK)
                ? pair : ThorHeroManagementPair.INVALID;
    }

    int artifactPair(int slot)
    {
        if (!management.complete() || slot < 0 || slot >= management.artifactPositions.length)
            return ThorHeroManagementPair.INVALID;
        final boolean occupied = (management.artifactFlags[slot] & 1) != 0;
        final boolean locked = (management.artifactFlags[slot] & 2) != 0;
        if (selectedArtifact < 0)
        {
            if (occupied && !locked) selectedArtifact = slot;
            return ThorHeroManagementPair.INVALID;
        }
        final int pair = ThorHeroManagementPair.encodeArtifact(selectedArtifact, slot);
        selectedArtifact = -1;
        return !locked && ThorLobbyScenarioState.isActionEnabled(mask, ThorActionIds.HERO_WINDOW_TRANSFER_ARTIFACT)
                ? pair : ThorHeroManagementPair.INVALID;
    }
}
