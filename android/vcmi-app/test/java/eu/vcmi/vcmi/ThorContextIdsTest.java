package eu.vcmi.vcmi;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ThorContextIdsTest
{
    @Test
    public void identifiersMatchNativeContract()
    {
        assertEquals("UNKNOWN", ThorContextIds.UNKNOWN);
        assertEquals("MAIN_MENU", ThorContextIds.MAIN_MENU);
        assertEquals("MAIN_MENU_NEW_GAME", ThorContextIds.MAIN_MENU_NEW_GAME);
        assertEquals("MAIN_MENU_LOAD_GAME", ThorContextIds.MAIN_MENU_LOAD_GAME);
        assertEquals("MAIN_MENU_CAMPAIGN", ThorContextIds.MAIN_MENU_CAMPAIGN);
        assertEquals("MAIN_MENU_CREDITS", ThorContextIds.MAIN_MENU_CREDITS);
        assertEquals("LOBBY_NEW_GAME", ThorContextIds.LOBBY_NEW_GAME);
        assertEquals("LOBBY_NEW_GAME_SCENARIO", ThorContextIds.LOBBY_NEW_GAME_SCENARIO);
        assertEquals("LOBBY_NEW_GAME_OPTIONS", ThorContextIds.LOBBY_NEW_GAME_OPTIONS);
        assertEquals("LOBBY_NEW_GAME_RANDOM_MAP", ThorContextIds.LOBBY_NEW_GAME_RANDOM_MAP);
        assertEquals("LOBBY_NEW_GAME_TURN_OPTIONS", ThorContextIds.LOBBY_NEW_GAME_TURN_OPTIONS);
        assertEquals("LOBBY_NEW_GAME_EXTRA_OPTIONS", ThorContextIds.LOBBY_NEW_GAME_EXTRA_OPTIONS);
        assertEquals("LOBBY_NEW_GAME_BATTLE_MODE", ThorContextIds.LOBBY_NEW_GAME_BATTLE_MODE);
        assertEquals("LOBBY_LOAD_GAME", ThorContextIds.LOBBY_LOAD_GAME);
        assertEquals("LOBBY_LOAD_GAME_SCENARIO", ThorContextIds.LOBBY_LOAD_GAME_SCENARIO);
        assertEquals("LOBBY_LOAD_GAME_OPTIONS", ThorContextIds.LOBBY_LOAD_GAME_OPTIONS);
        assertEquals("LOBBY_LOAD_GAME_TURN_OPTIONS", ThorContextIds.LOBBY_LOAD_GAME_TURN_OPTIONS);
        assertEquals("LOBBY_LOAD_GAME_EXTRA_OPTIONS", ThorContextIds.LOBBY_LOAD_GAME_EXTRA_OPTIONS);
        assertEquals("LOBBY_CAMPAIGN_LIST", ThorContextIds.LOBBY_CAMPAIGN_LIST);
        assertEquals("ADVENTURE_MAP", ThorContextIds.ADVENTURE_MAP);
        assertEquals("HERO_WINDOW", ThorContextIds.HERO_WINDOW);
        assertEquals("TOWN_WINDOW", ThorContextIds.TOWN_WINDOW);
        assertEquals("HERO_MEETING", ThorContextIds.HERO_MEETING);
        assertEquals("BATTLE", ThorContextIds.BATTLE);
        assertEquals("BATTLE_TACTICS", ThorContextIds.BATTLE_TACTICS);
        assertEquals("BATTLE_RESULT", ThorContextIds.BATTLE_RESULT);
        assertEquals("KINGDOM_OVERVIEW", ThorContextIds.KINGDOM_OVERVIEW);
        assertEquals("QUEST_LOG", ThorContextIds.QUEST_LOG);
        assertEquals("SCENARIO_EVENT_JOURNAL", ThorContextIds.SCENARIO_EVENT_JOURNAL);
        assertEquals("PUZZLE_MAP", ThorContextIds.PUZZLE_MAP);
        assertEquals("SAVE_GAME", ThorContextIds.SAVE_GAME);
    }

    @Test
    public void adventureUtilityContextsUseBoundedResources()
    {
        assertNotEquals(0, R.string.thor_context_kingdom_overview);
        assertNotEquals(0, R.string.thor_context_kingdom_overview_status);
        assertNotEquals(0, R.string.thor_context_quest_log);
        assertNotEquals(0, R.string.thor_context_quest_log_status);
        assertNotEquals(0, R.string.thor_context_scenario_event_journal);
        assertNotEquals(0, R.string.thor_context_scenario_event_journal_status);
        assertNotEquals(0, R.string.thor_context_puzzle_map);
        assertNotEquals(0, R.string.thor_context_puzzle_map_status);
        assertNotEquals(0, R.string.thor_context_save_game);
        assertNotEquals(0, R.string.thor_context_save_game_status);
    }
}
