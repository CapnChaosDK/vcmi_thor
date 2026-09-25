package eu.vcmi.vcmi;

/** Revision-scoped, presentation-local state for selecting and editing an exact split. */
final class ThorHeroMeetingSplitState
{
    enum Stage { NONE, DESTINATION, AMOUNT }

    private Stage stage = Stage.NONE;
    private long revision;
    private int sourceKey = -1;
    private int destinationKey = -1;
    private int amount;
    private int maximum;

    boolean begin(final long renderedRevision, final int key, final int sourceCount)
    {
        cancel();
        if (renderedRevision <= 0 || key < 0 || key >= ThorHeroMeetingArmies.SLOT_COUNT || sourceCount < 2)
            return false;
        revision = renderedRevision;
        sourceKey = key;
        stage = Stage.DESTINATION;
        return true;
    }

    boolean selectDestination(final long currentRevision, final int key, final ThorHeroMeetingArmies armies)
    {
        if (stage != Stage.DESTINATION || currentRevision != revision || !armies.complete()
                || key < 0 || key >= ThorHeroMeetingArmies.SLOT_COUNT || key == sourceKey)
            return false;
        final boolean occupied = (armies.flags[key] & 1) != 0;
        if (occupied && armies.creatureIds[key] != armies.creatureIds[sourceKey])
            return false;
        destinationKey = key;
        maximum = armies.counts[sourceKey] - 1;
        amount = Math.max(1, Math.min(maximum, armies.counts[sourceKey] / 2));
        stage = Stage.AMOUNT;
        return true;
    }

    void adjust(final int delta)
    {
        if (stage == Stage.AMOUNT)
            amount = (int) Math.max(1L, Math.min((long) maximum, (long) amount + delta));
    }

    boolean isCurrent(final long currentRevision)
    {
        return stage != Stage.NONE && revision == currentRevision;
    }

    void cancel()
    {
        stage = Stage.NONE;
        revision = 0;
        sourceKey = destinationKey = -1;
        amount = maximum = 0;
    }

    Stage stage() { return stage; }
    int sourceKey() { return sourceKey; }
    int destinationKey() { return destinationKey; }
    int amount() { return amount; }
}
