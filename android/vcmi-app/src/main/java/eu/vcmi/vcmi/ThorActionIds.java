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
    static final int HERO_MEETING_SPLIT_STACK = 20;
    static final int HERO_MEETING_TRANSFER_ARTIFACT = 21;
    static final int HERO_MEETING_REDISTRIBUTE_STACK = 22;
    static final int HERO_MEETING_ARTIFACTS_LEFT_TO_RIGHT = 23;
    static final int HERO_MEETING_ARTIFACTS_RIGHT_TO_LEFT = 24;
    static final int HERO_MEETING_SWAP_ARTIFACTS = 25;
    static final int LOBBY_SET_DIFFICULTY = 26;
    static final int LOBBY_START_GAME = 27;
    static final int LOBBY_BACK = 28;
    static final int LOBBY_PREVIOUS_SCENARIO = 29;
    static final int LOBBY_NEXT_SCENARIO = 30;
    static final int MAIN_MENU_CHOICE_1 = 31;
    static final int MAIN_MENU_CHOICE_2 = 32;
    static final int MAIN_MENU_CHOICE_3 = 33;
    static final int MAIN_MENU_CHOICE_4 = 34;
    static final int MAIN_MENU_CHOICE_5 = 35;
    static final int CAMPAIGN_PREVIOUS_SCENARIO = 36;
    static final int CAMPAIGN_NEXT_SCENARIO = 37;
    static final int CAMPAIGN_SELECT_BONUS_1 = 38;
    static final int CAMPAIGN_SELECT_BONUS_2 = 39;
    static final int CAMPAIGN_SELECT_BONUS_3 = 40;
    static final int CAMPAIGN_START = 41;
    static final int CAMPAIGN_BACK = 42;
    static final int CAMPAIGN_BROWSER_SELECT = 43;
    static final int CAMPAIGN_BROWSER_PREVIOUS_PAGE = 44;
    static final int CAMPAIGN_BROWSER_NEXT_PAGE = 45;
    static final int CAMPAIGN_BROWSER_BACK = 46;
    static final int LOAD_BROWSER_SELECT = 47;
    static final int LOAD_BROWSER_PREVIOUS_PAGE = 48;
    static final int LOAD_BROWSER_NEXT_PAGE = 49;
    static final int WINDOW_PREVIOUS = 50;
    static final int WINDOW_NEXT = 51;
    static final int WINDOW_CLOSE = 52;
    static final int TOWN_OPEN_SERVICE = 53;
    static final int TOWN_HALL_BUILD = 54;
    static final int RECRUITMENT_EDIT = 55;
    static final int RECRUITMENT_BUY = 56;
    static final int WINDOW_CONFIRM = 57;
    static final int ADVENTURE_CENTER_VIEW = 58;
    static final int ADVENTURE_SET_MAP_LEVEL = 59;
    static final int HERO_WINDOW_TRANSFER_STACK = 60;
    static final int HERO_WINDOW_TRANSFER_ARTIFACT = 61;
    static final int MAX_ACTION_ID = HERO_WINDOW_TRANSFER_ARTIFACT;
    static final int LOCAL_CONTROL = -1;
    static final int NO_TARGET = -1;

    static
    {
        if (MAX_ACTION_ID > Long.SIZE)
            throw new ExceptionInInitializerError("Thor action IDs exceed the 64-bit mask contract");
    }

    private ThorActionIds()
    {
    }

    static long maskFor(final int actionId)
    {
        return actionId >= OPEN_KINGDOM_OVERVIEW && actionId <= MAX_ACTION_ID
                ? bitForActionId(actionId) : 0L;
    }

    static long bitForActionId(final int actionId)
    {
        return actionId >= OPEN_KINGDOM_OVERVIEW && actionId <= Long.SIZE
                ? 1L << (actionId - 1) : 0L;
    }

    static int artifactBulkActionForButton(final int index)
    {
        switch (index)
        {
            case 0: return HERO_MEETING_ARTIFACTS_LEFT_TO_RIGHT;
            case 1: return HERO_MEETING_SWAP_ARTIFACTS;
            case 2: return HERO_MEETING_ARTIFACTS_RIGHT_TO_LEFT;
            default: return NONE;
        }
    }
}
