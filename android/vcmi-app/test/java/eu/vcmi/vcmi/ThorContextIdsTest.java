package eu.vcmi.vcmi;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

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
        assertNotEquals(0, R.string.thor_context_adventure_map_hero_status);
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

    @Test
    public void heroDashboardUsesFixedDetailContract()
    {
        assertEquals(4, ThorContextDetails.COUNT);
        assertNotEquals(0, R.string.thor_context_hero);
        assertNotEquals(0, R.string.thor_context_hero_status);
    }

    @Test
    public void townDashboardUsesFixedDetailContractAndResources()
    {
        assertEquals(4, ThorContextDetails.COUNT);
        assertNotEquals(0, R.string.thor_context_town);
        assertNotEquals(0, R.string.thor_context_town_status);
        assertNotEquals(0, R.string.thor_town_income);
        assertNotEquals(0, R.string.thor_town_income_value);
        assertNotEquals(0, R.string.thor_town_buildings);
        assertNotEquals(0, R.string.thor_town_visiting_hero);
        assertNotEquals(0, R.string.thor_town_garrison_hero);
        assertNotEquals(0, R.string.thor_town_none);
    }

    @Test
    public void adventureActionIdsUseStableExplicitMasks()
    {
        assertEquals(0, ThorActionIds.NONE);
        assertEquals(1, ThorActionIds.OPEN_KINGDOM_OVERVIEW);
        assertEquals(2, ThorActionIds.OPEN_QUEST_LOG);
        assertEquals(3, ThorActionIds.OPEN_PUZZLE_MAP);
        assertEquals(4, ThorActionIds.OPEN_SAVE_GAME);
        assertEquals(5, ThorActionIds.NEXT_HERO);
        assertEquals(6, ThorActionIds.MOVE_HERO);
        assertEquals(7, ThorActionIds.TOGGLE_HERO_SLEEP);
        assertEquals(8, ThorActionIds.END_TURN);
		assertEquals(9, ThorActionIds.BATTLE_WAIT);
		assertEquals(10, ThorActionIds.BATTLE_DEFEND);
		assertEquals(11, ThorActionIds.BATTLE_TACTICS_NEXT);
		assertEquals(12, ThorActionIds.BATTLE_TACTICS_END);
		assertEquals(13, ThorActionIds.SELECT_HERO);
		assertEquals(4096, ThorActionIds.maskFor(ThorActionIds.SELECT_HERO));
		assertEquals(14, ThorActionIds.SELECT_TOWN);
		assertEquals(8192, ThorActionIds.maskFor(ThorActionIds.SELECT_TOWN));
		assertEquals(-1, ThorActionIds.NO_TARGET);
        assertEquals(1, ThorActionIds.maskFor(ThorActionIds.OPEN_KINGDOM_OVERVIEW));
        assertEquals(2, ThorActionIds.maskFor(ThorActionIds.OPEN_QUEST_LOG));
        assertEquals(4, ThorActionIds.maskFor(ThorActionIds.OPEN_PUZZLE_MAP));
        assertEquals(8, ThorActionIds.maskFor(ThorActionIds.OPEN_SAVE_GAME));
        assertEquals(16, ThorActionIds.maskFor(ThorActionIds.NEXT_HERO));
        assertEquals(32, ThorActionIds.maskFor(ThorActionIds.MOVE_HERO));
        assertEquals(64, ThorActionIds.maskFor(ThorActionIds.TOGGLE_HERO_SLEEP));
        assertEquals(128, ThorActionIds.maskFor(ThorActionIds.END_TURN));
		assertEquals(256, ThorActionIds.maskFor(ThorActionIds.BATTLE_WAIT));
		assertEquals(512, ThorActionIds.maskFor(ThorActionIds.BATTLE_DEFEND));
		assertEquals(1024, ThorActionIds.maskFor(ThorActionIds.BATTLE_TACTICS_NEXT));
		assertEquals(2048, ThorActionIds.maskFor(ThorActionIds.BATTLE_TACTICS_END));
		assertEquals(15, ThorActionIds.HERO_MEETING_MOVE_STACK);
		assertEquals(16384, ThorActionIds.maskFor(ThorActionIds.HERO_MEETING_MOVE_STACK));
		assertEquals(0, ThorActionIds.maskFor(99));
    }

    @Test
    public void battleActionsUseSeparateStableMasksAndResources()
    {
        assertNotEquals(ThorActionIds.maskFor(ThorActionIds.BATTLE_WAIT),
                ThorActionIds.maskFor(ThorActionIds.NEXT_HERO));
        assertNotEquals(ThorActionIds.maskFor(ThorActionIds.BATTLE_TACTICS_END),
                ThorActionIds.maskFor(ThorActionIds.END_TURN));
        assertNotEquals(0, R.string.thor_action_wait);
        assertNotEquals(0, R.string.thor_action_defend);
        assertNotEquals(0, R.string.thor_action_next_unit);
        assertNotEquals(0, R.string.thor_action_start_battle);
    }

    @Test
    public void battleDashboardUsesAllBoundedDetailSlotsAndLocalLabels()
    {
        assertEquals(4, ThorContextDetails.COUNT);
        assertNotEquals(0, R.string.thor_battle_round_value);
        assertNotEquals(0, R.string.thor_battle_count);
        assertNotEquals(0, R.string.thor_battle_attack);
        assertNotEquals(0, R.string.thor_battle_defense);
        assertNotEquals(0, R.string.thor_battle_hp);
        assertNotEquals(0, R.string.thor_battle_no_active_unit);
        assertNotEquals(0, R.string.thor_battle_not_available);
        assertEquals(ThorActionIds.maskFor(ThorActionIds.BATTLE_WAIT)
                        | ThorActionIds.maskFor(ThorActionIds.BATTLE_DEFEND),
                256 | 512);
        assertEquals(ThorActionIds.maskFor(ThorActionIds.BATTLE_TACTICS_NEXT)
                        | ThorActionIds.maskFor(ThorActionIds.BATTLE_TACTICS_END),
                1024 | 2048);
    }

    @Test
    public void adventureGameplayDeckUsesBoundedStateAwareResources()
    {
        assertNotEquals(0, R.string.thor_action_section_gameplay);
        assertNotEquals(0, R.string.thor_action_section_utilities);
        assertNotEquals(0, R.string.thor_action_next_hero);
        assertNotEquals(0, R.string.thor_action_move_hero);
        assertNotEquals(0, R.string.thor_action_sleep_hero);
        assertNotEquals(0, R.string.thor_action_wake_hero);
        assertNotEquals(0, R.string.thor_action_end_turn);
        assertNotEquals(0, R.string.thor_tab_actions);
        assertNotEquals(0, R.string.thor_tab_heroes);
        assertNotEquals(0, R.string.thor_tab_towns);
        assertNotEquals(0, R.string.thor_no_heroes);
        assertNotEquals(0, R.string.thor_no_towns);
        assertNotEquals(0, R.string.thor_town_previous);
        assertNotEquals(0, R.string.thor_town_next);
        assertNotEquals(0, R.string.thor_town_page);
        assertNotEquals(0, R.string.thor_hero_sleeping);
        assertEquals(8, ThorHeroRoster.MAX_HEROES);
        assertEquals(0, ThorHeroRoster.copyOf(new int[9], new String[9], new int[9], new int[9], new int[9]).ids.length);
        final ThorHeroRoster roster = ThorHeroRoster.copyOf(new int[]{42}, new String[]{"Hero"},
                new int[]{100}, new int[]{200}, new int[]{1});
        assertEquals(42, roster.ids[0]);
        assertEquals("Hero", roster.names[0]);
        assertEquals(64, ThorTownRoster.MAX_TOWNS);
        assertEquals(0, ThorTownRoster.copyOf(new int[65], new String[65], new int[65]).ids.length);
        final ThorTownRoster towns = ThorTownRoster.copyOf(new int[]{21}, new String[]{"Castle Stronghold"}, new int[]{1});
        assertEquals(21, towns.ids[0]);
        assertEquals("Castle Stronghold", towns.names[0]);
    }

    @Test
    public void adventureHeaderTabsAndCardsUseSeparateBands()
    {
        assertEquals(2, ThorAdventureLayout.HERO_COLUMNS);
        assertEquals(4, ThorAdventureLayout.HERO_ROWS);
        assertEquals(5, ThorAdventureLayout.TOWN_ROWS_PER_PAGE);
        assertTrue(ThorAdventureLayout.TITLE < ThorAdventureLayout.STATUS);
        assertTrue(ThorAdventureLayout.STATUS < ThorAdventureLayout.DIVIDER);
        assertTrue(ThorAdventureLayout.TAB_END < ThorAdventureLayout.CONTENT_START);

        // On the measured 1080 x 1240 panel, the status stays above the divider,
        // the tabs end before the gameplay heading, and four hero rows remain touch-sized.
        final float frameHeight = 1142f;
        final float divider = frameHeight * ThorAdventureLayout.DIVIDER;
        final float tabBottom = divider + (frameHeight - divider) * ThorAdventureLayout.TAB_END;
        final float contentTop = divider + (frameHeight - divider) * ThorAdventureLayout.CONTENT_START;
        assertTrue(frameHeight * ThorAdventureLayout.STATUS + 35f < divider);
        assertTrue(tabBottom + 35f < contentTop);
        assertTrue((frameHeight - contentTop) / ThorAdventureLayout.HERO_ROWS > 140f);
    }
}
