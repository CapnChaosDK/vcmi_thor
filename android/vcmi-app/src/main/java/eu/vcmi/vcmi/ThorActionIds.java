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
    static final int SELECT_HERO = 13;
    static final int SELECT_TOWN = 14;
    static final int HERO_MEETING_MOVE_STACK = 15;
    static final int HERO_MEETING_TRANSFER_STACK = 16;
    static final int HERO_MEETING_ARMY_LEFT_TO_RIGHT = 17;
    static final int HERO_MEETING_ARMY_RIGHT_TO_LEFT = 18;
    static final int HERO_MEETING_SWAP_ARMIES = 19;
    static final int NO_TARGET = -1;

    private ThorActionIds()
    {
    }

    static int maskFor(final int actionId)
    {
        return actionId >= OPEN_KINGDOM_OVERVIEW && actionId <= HERO_MEETING_SWAP_ARMIES
                ? 1 << (actionId - 1) : 0;
    }
}
