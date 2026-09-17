package eu.vcmi.vcmi;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
    }
}
