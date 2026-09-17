package eu.vcmi.vcmi;

final class ThorActionIds
{
    static final int NONE = 0;
    static final int OPEN_KINGDOM_OVERVIEW = 1;
    static final int OPEN_QUEST_LOG = 2;
    static final int OPEN_PUZZLE_MAP = 3;
    static final int OPEN_SAVE_GAME = 4;

    private ThorActionIds()
    {
    }

    static int maskFor(final int actionId)
    {
        return actionId >= OPEN_KINGDOM_OVERVIEW && actionId <= OPEN_SAVE_GAME
                ? 1 << (actionId - 1) : 0;
    }
}
