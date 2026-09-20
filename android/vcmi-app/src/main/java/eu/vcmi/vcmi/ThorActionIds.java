package eu.vcmi.vcmi;

final class ThorActionIds
{
    static final int NONE = 0;
    static final int OPEN_KINGDOM_OVERVIEW = 1;
    static final int OPEN_QUEST_LOG = 2;
    static final int OPEN_PUZZLE_MAP = 3;
    static final int OPEN_SAVE_GAME = 4;
    static final int NEXT_HERO = 5;
    static final int MOVE_HERO = 6;
    static final int TOGGLE_HERO_SLEEP = 7;
    static final int END_TURN = 8;
    static final int BATTLE_WAIT = 9;
    static final int BATTLE_DEFEND = 10;
    static final int BATTLE_TACTICS_NEXT = 11;
    static final int BATTLE_TACTICS_END = 12;

    private ThorActionIds()
    {
    }

    static int maskFor(final int actionId)
    {
        return actionId >= OPEN_KINGDOM_OVERVIEW && actionId <= BATTLE_TACTICS_END
                ? 1 << (actionId - 1) : 0;
    }
}
