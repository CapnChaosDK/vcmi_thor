package eu.vcmi.vcmi;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
        assertEquals("CAMPAIGN_BONUS_SELECTION", ThorContextIds.CAMPAIGN_BONUS_SELECTION);
        assertEquals("CAMPAIGN_BROWSER", ThorContextIds.CAMPAIGN_BROWSER);
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
		assertEquals(16, ThorActionIds.HERO_MEETING_TRANSFER_STACK);
		assertEquals(17, ThorActionIds.HERO_MEETING_ARMY_LEFT_TO_RIGHT);
		assertEquals(18, ThorActionIds.HERO_MEETING_ARMY_RIGHT_TO_LEFT);
		assertEquals(19, ThorActionIds.HERO_MEETING_SWAP_ARMIES);
		assertEquals(20, ThorActionIds.HERO_MEETING_SPLIT_STACK);
		assertEquals(16384, ThorActionIds.maskFor(ThorActionIds.HERO_MEETING_MOVE_STACK));
		assertEquals(32768, ThorActionIds.maskFor(ThorActionIds.HERO_MEETING_TRANSFER_STACK));
		assertEquals(65536, ThorActionIds.maskFor(ThorActionIds.HERO_MEETING_ARMY_LEFT_TO_RIGHT));
		assertEquals(131072, ThorActionIds.maskFor(ThorActionIds.HERO_MEETING_ARMY_RIGHT_TO_LEFT));
		assertEquals(262144, ThorActionIds.maskFor(ThorActionIds.HERO_MEETING_SWAP_ARMIES));
		assertEquals(524288, ThorActionIds.maskFor(ThorActionIds.HERO_MEETING_SPLIT_STACK));
		assertEquals(21, ThorActionIds.HERO_MEETING_TRANSFER_ARTIFACT);
		assertEquals(22, ThorActionIds.HERO_MEETING_REDISTRIBUTE_STACK);
		assertEquals(2097152, ThorActionIds.maskFor(ThorActionIds.HERO_MEETING_REDISTRIBUTE_STACK));
		assertEquals(23, ThorActionIds.HERO_MEETING_ARTIFACTS_LEFT_TO_RIGHT);
		assertEquals(24, ThorActionIds.HERO_MEETING_ARTIFACTS_RIGHT_TO_LEFT);
		assertEquals(25, ThorActionIds.HERO_MEETING_SWAP_ARTIFACTS);
		assertEquals(26, ThorActionIds.LOBBY_SET_DIFFICULTY);
		assertEquals(27, ThorActionIds.LOBBY_START_GAME);
		assertEquals(28, ThorActionIds.LOBBY_BACK);
		assertEquals(29, ThorActionIds.LOBBY_PREVIOUS_SCENARIO);
		assertEquals(30, ThorActionIds.LOBBY_NEXT_SCENARIO);
		assertEquals(53, ThorActionIds.MAX_ACTION_ID);
		assertEquals(36, ThorActionIds.CAMPAIGN_PREVIOUS_SCENARIO);
		assertEquals(42, ThorActionIds.CAMPAIGN_BACK);
		assertEquals(43, ThorActionIds.CAMPAIGN_BROWSER_SELECT);
		assertEquals(44, ThorActionIds.CAMPAIGN_BROWSER_PREVIOUS_PAGE);
		assertEquals(45, ThorActionIds.CAMPAIGN_BROWSER_NEXT_PAGE);
		assertEquals(46, ThorActionIds.CAMPAIGN_BROWSER_BACK);
		assertEquals(47, ThorActionIds.LOAD_BROWSER_SELECT);
		assertEquals(48, ThorActionIds.LOAD_BROWSER_PREVIOUS_PAGE);
		assertEquals(49, ThorActionIds.LOAD_BROWSER_NEXT_PAGE);
		assertEquals(1L << 48, ThorActionIds.maskFor(ThorActionIds.LOAD_BROWSER_NEXT_PAGE));
		assertEquals(ThorActionIds.CAMPAIGN_PREVIOUS_SCENARIO, ThorCampaignState.actionForControl(
				ThorLobbyScenarioState.CONTROL_PREVIOUS_SCENARIO));
		assertEquals(ThorActionIds.CAMPAIGN_SELECT_BONUS_1 + 2, ThorCampaignState.actionForControl(
				ThorLobbyScenarioState.CONTROL_DIFFICULTY_FIRST + 2));
		assertEquals(ThorActionIds.NONE, ThorCampaignState.actionForControl(
				ThorLobbyScenarioState.CONTROL_DIFFICULTY_FIRST + 3));
		assertEquals(2, ThorCampaignState.bonusIndexForControl(
				ThorLobbyScenarioState.CONTROL_DIFFICULTY_FIRST + 2));
		assertEquals(1L << 24, ThorActionIds.maskFor(ThorActionIds.HERO_MEETING_SWAP_ARTIFACTS));
		assertEquals(1L << 25, ThorActionIds.maskFor(ThorActionIds.LOBBY_SET_DIFFICULTY));
		assertEquals(1L << 26, ThorActionIds.maskFor(ThorActionIds.LOBBY_START_GAME));
		assertEquals(1L << 27, ThorActionIds.maskFor(ThorActionIds.LOBBY_BACK));
		assertEquals(1L << 28, ThorActionIds.maskFor(ThorActionIds.LOBBY_PREVIOUS_SCENARIO));
		assertEquals(1L << 29, ThorActionIds.maskFor(ThorActionIds.LOBBY_NEXT_SCENARIO));
		assertEquals(Long.MIN_VALUE, ThorActionIds.bitForActionId(64));
		assertEquals(1L << 30, ThorActionIds.maskFor(31));
		assertEquals(0L, ThorActionIds.bitForActionId(65));
		assertEquals(0, ThorActionIds.maskFor(99));
		for (int id = 1; id <= 30; ++id)
			assertEquals(1L << (id - 1), ThorActionIds.maskFor(id));
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
    public void lobbyActionsStaySilentAndUseTheSingleDifficultyAction()
    {
        assertFalse(ThorHapticState.isEligible(ThorContextIds.LOBBY_NEW_GAME_SCENARIO,
                ThorActionIds.LOBBY_SET_DIFFICULTY));
        assertFalse(ThorHapticState.isEligible(ThorContextIds.LOBBY_NEW_GAME_SCENARIO,
                ThorActionIds.LOBBY_START_GAME));
        assertFalse(ThorHapticState.isEligible(ThorContextIds.LOBBY_NEW_GAME_SCENARIO,
                ThorActionIds.LOBBY_BACK));
        assertFalse(ThorHapticState.isEligible(ThorContextIds.LOBBY_NEW_GAME_SCENARIO,
                ThorActionIds.LOBBY_PREVIOUS_SCENARIO));
        assertFalse(ThorHapticState.isEligible(ThorContextIds.LOBBY_NEW_GAME_SCENARIO,
                ThorActionIds.LOBBY_NEXT_SCENARIO));
        assertEquals(1L << 25, ThorActionIds.maskFor(ThorActionIds.LOBBY_SET_DIFFICULTY));
        assertEquals(1L << 28, ThorActionIds.maskFor(ThorActionIds.LOBBY_PREVIOUS_SCENARIO));
        assertEquals(1L << 29, ThorActionIds.maskFor(ThorActionIds.LOBBY_NEXT_SCENARIO));
        for (int difficulty = 0; difficulty <= 4; ++difficulty)
            assertEquals(difficulty, ThorLobbyScenarioState.difficultyTargetForIndex(difficulty));
        assertEquals(-1, ThorLobbyScenarioState.difficultyTargetForIndex(5));
    }

    @Test
    public void lobbyScenarioCompanionUsesLocalizedLabels()
    {
        assertNotEquals(0, R.string.thor_lobby_no_scenario);
        assertNotEquals(0, R.string.thor_lobby_map_size);
        assertNotEquals(0, R.string.thor_lobby_players);
        assertNotEquals(0, R.string.thor_lobby_difficulty);
        assertNotEquals(0, R.string.thor_lobby_previous_scenario);
        assertNotEquals(0, R.string.thor_lobby_next_scenario);
        assertNotEquals(0, R.string.thor_lobby_start);
        assertNotEquals(0, R.string.thor_lobby_back);
        assertNotEquals(0, R.string.thor_lobby_difficulty_easy);
        assertNotEquals(0, R.string.thor_lobby_difficulty_normal);
        assertNotEquals(0, R.string.thor_lobby_difficulty_hard);
        assertNotEquals(0, R.string.thor_lobby_difficulty_expert);
        assertNotEquals(0, R.string.thor_lobby_difficulty_impossible);
    }

    @Test
    public void lobbyScenarioStateCopiesBoundedSummaryAndSelectedDifficulty()
    {
        assertEquals("Scenario", ThorLobbyScenarioState.scenarioName("Scenario", "Fallback"));
        assertEquals("Fallback", ThorLobbyScenarioState.scenarioName("", "Fallback"));
        assertEquals("Fallback", ThorLobbyScenarioState.scenarioName(null, "Fallback"));
        assertEquals(-1, ThorLobbyScenarioState.difficultyIndex(null));
        assertEquals(-1, ThorLobbyScenarioState.difficultyIndex(new String[]{"128x128", "6/4"}));
        for (int difficulty = 0; difficulty < ThorLobbyScenarioState.DIFFICULTY_COUNT; ++difficulty)
        {
            assertEquals(difficulty, ThorLobbyScenarioState.difficultyIndex(
                    new String[]{"128x128", "6/4", Integer.toString(difficulty)}));
            assertEquals(difficulty, ThorLobbyScenarioState.difficultyTargetForIndex(difficulty));
            assertFalse(ThorLobbyScenarioState.shouldDispatchDifficulty(difficulty, difficulty));
        }
        assertEquals(-1, ThorLobbyScenarioState.difficultyIndex(
                new String[]{"128x128", "6/4", "5"}));
    }

    @Test
    public void lobbyScenarioControlsHaveOnlyExplicitTouchRegions()
    {
        final float width = 1000f;
        final float height = 1160f;
        final float divider = 0.48f * height;
        for (int control = ThorLobbyScenarioState.CONTROL_PREVIOUS_SCENARIO;
             control <= ThorLobbyScenarioState.CONTROL_BACK; ++control)
        {
            final float[] bounds = ThorLobbyScenarioState.boundsForControl(control, width, height, divider);
            final int hit = ThorLobbyScenarioState.controlAt((bounds[0] + bounds[2]) / 2f,
                    (bounds[1] + bounds[3]) / 2f, width, height, divider);
            assertEquals(control, hit);
        }
        assertEquals(ThorLobbyScenarioState.CONTROL_NONE,
                ThorLobbyScenarioState.controlAt(0f, 0f, width, height, divider));
        final float[] firstDifficulty = ThorLobbyScenarioState.boundsForControl(
                ThorLobbyScenarioState.CONTROL_DIFFICULTY_FIRST, width, height, divider);
        final float[] start = ThorLobbyScenarioState.boundsForControl(
                ThorLobbyScenarioState.CONTROL_START, width, height, divider);
        assertTrue(firstDifficulty[3] < start[1]);
        assertEquals(ThorLobbyScenarioState.CONTROL_NONE,
                ThorLobbyScenarioState.controlAt((firstDifficulty[0] + firstDifficulty[2]) / 2f,
                        (firstDifficulty[3] + start[1]) / 2f, width, height, divider));

        assertEquals(ThorActionIds.LOBBY_SET_DIFFICULTY,
                ThorLobbyScenarioState.actionForControl(ThorLobbyScenarioState.CONTROL_DIFFICULTY_FIRST + 2));
        assertEquals(ThorActionIds.LOBBY_PREVIOUS_SCENARIO,
                ThorLobbyScenarioState.actionForControl(ThorLobbyScenarioState.CONTROL_PREVIOUS_SCENARIO));
        assertEquals(ThorActionIds.LOBBY_NEXT_SCENARIO,
                ThorLobbyScenarioState.actionForControl(ThorLobbyScenarioState.CONTROL_NEXT_SCENARIO));
        assertEquals(2, ThorLobbyScenarioState.difficultyTargetForControl(
                ThorLobbyScenarioState.CONTROL_DIFFICULTY_FIRST + 2));
        assertEquals(ThorActionIds.LOBBY_START_GAME,
                ThorLobbyScenarioState.actionForControl(ThorLobbyScenarioState.CONTROL_START));
        assertEquals(ThorActionIds.LOBBY_BACK,
                ThorLobbyScenarioState.actionForControl(ThorLobbyScenarioState.CONTROL_BACK));
        assertFalse(ThorLobbyScenarioState.isActionEnabled(0L, ThorActionIds.LOBBY_START_GAME));
        assertFalse(ThorLobbyScenarioState.isActionEnabled(0L, ThorActionIds.LOBBY_PREVIOUS_SCENARIO));
        assertFalse(ThorLobbyScenarioState.isActionEnabled(0L, ThorActionIds.LOBBY_NEXT_SCENARIO));
        assertTrue(ThorLobbyScenarioState.isActionEnabled(
                ThorActionIds.maskFor(ThorActionIds.LOBBY_START_GAME), ThorActionIds.LOBBY_START_GAME));
        assertTrue(ThorLobbyScenarioState.isActionEnabled(
                ThorActionIds.maskFor(ThorActionIds.LOBBY_PREVIOUS_SCENARIO), ThorActionIds.LOBBY_PREVIOUS_SCENARIO));
        assertTrue(ThorLobbyScenarioState.isActionEnabled(
                ThorActionIds.maskFor(ThorActionIds.LOBBY_NEXT_SCENARIO), ThorActionIds.LOBBY_NEXT_SCENARIO));

        final float[] previous = ThorLobbyScenarioState.boundsForControl(
                ThorLobbyScenarioState.CONTROL_PREVIOUS_SCENARIO, width, height, divider);
        final float[] next = ThorLobbyScenarioState.boundsForControl(
                ThorLobbyScenarioState.CONTROL_NEXT_SCENARIO, width, height, divider);
        assertFalse(overlaps(previous, next));
        for (int control = ThorLobbyScenarioState.CONTROL_DIFFICULTY_FIRST;
             control <= ThorLobbyScenarioState.CONTROL_BACK; ++control)
        {
            assertFalse(overlaps(previous, ThorLobbyScenarioState.boundsForControl(control, width, height, divider)));
            assertFalse(overlaps(next, ThorLobbyScenarioState.boundsForControl(control, width, height, divider)));
        }
        assertTrue(previous[1] > divider);
    }

    @Test
    public void lobbyScenarioGestureRejectsDisabledOutsideStaleAndMultiPointerTaps()
    {
        final int previous = ThorLobbyScenarioState.CONTROL_PREVIOUS_SCENARIO;
        ThorLobbyScenarioGesture gesture = new ThorLobbyScenarioGesture();
        gesture.begin(previous, 12, 4, 0, true);
        assertTrue(gesture.finish(previous, 1, 0, 12, 4, true, true));
        for (int control : new int[]{ThorLobbyScenarioState.CONTROL_DIFFICULTY_FIRST,
                ThorLobbyScenarioState.CONTROL_START, ThorLobbyScenarioState.CONTROL_BACK})
        {
            gesture.begin(control, 12, 4, 0, true);
            assertTrue(gesture.finish(control, 1, 0, 12, 4, true, true));
        }

        gesture.begin(previous, 12, 4, 0, false);
        assertFalse(gesture.finish(previous, 1, 0, 12, 4, true, true));
        gesture.begin(previous, 12, 4, 0, true);
        assertFalse(gesture.finish(previous, 1, 0, 12, 4, false, true));
        gesture.begin(ThorLobbyScenarioState.CONTROL_NONE, 12, 4, 0, true);
        assertFalse(gesture.finish(ThorLobbyScenarioState.CONTROL_NONE, 1, 0, 12, 4, true, true));
        gesture.begin(previous, 12, 4, 0, true);
        assertFalse(gesture.finish(ThorLobbyScenarioState.CONTROL_NEXT_SCENARIO, 1, 0, 12, 4, true, true));
        gesture.begin(previous, 12, 4, 0, true);
        assertFalse(gesture.finish(previous, 1, 0, 13, 4, true, true));
        gesture.begin(previous, 12, 4, 0, true);
        assertFalse(gesture.finish(previous, 1, 0, 12, 5, true, true));
        gesture.begin(previous, 12, 4, 0, true);
        assertFalse(gesture.finish(previous, 1, 0, 12, 4, true, false));
        gesture.begin(previous, 12, 4, 0, true);
        assertFalse(gesture.finish(previous, 2, 0, 12, 4, true, true));
        gesture.begin(previous, 12, 4, 0, true);
        assertFalse(gesture.finish(previous, 1, 1, 12, 4, true, true));
        gesture.begin(previous, 12, 4, 0, true);
        gesture.cancel();
        assertFalse(gesture.isActive());
        assertFalse(gesture.finish(previous, 1, 0, 12, 4, true, true));
    }

    private static boolean overlaps(final float[] first, final float[] second)
    {
        return first[0] < second[2] && first[2] > second[0]
                && first[1] < second[3] && first[3] > second[1];
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

    @Test
    public void heroMeetingSnapshotIsFixedAndCopySafe()
    {
        final int[] creatureIds = new int[ThorHeroMeetingArmies.SLOT_COUNT];
        final int[] counts = new int[ThorHeroMeetingArmies.SLOT_COUNT];
        final String[] creatureNames = new String[ThorHeroMeetingArmies.SLOT_COUNT];
        final int[] flags = new int[ThorHeroMeetingArmies.SLOT_COUNT];
        for (int index = 0; index < ThorHeroMeetingArmies.SLOT_COUNT; ++index)
        {
            creatureIds[index] = -1;
            creatureNames[index] = "";
        }
        creatureIds[0] = 3;
        counts[0] = 12;
        creatureNames[0] = "Pikemen";
        flags[0] = 1;
        final ThorHeroMeetingArmies armies = ThorHeroMeetingArmies.copyOf(1, 2,
                new String[]{"Left", "Right"}, new int[]{1, 2}, creatureIds, counts, creatureNames, flags, 1);
        creatureIds[0] = 99;
        assertTrue(armies.complete());
        assertEquals(14, armies.creatureIds.length);
        assertEquals(3, armies.creatureIds[0]);
        assertEquals(0, ThorHeroMeetingArmies.copyOf(1, 2, new String[]{"Left", "Right"}, new int[]{1, 2},
                new int[13], counts, creatureNames, flags, 1).creatureIds.length);
    }
}
