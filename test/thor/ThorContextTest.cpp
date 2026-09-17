#include "StdInc.h"

#include "../../lib/thor/ThorContext.h"

TEST(ThorContextStoreTest, StartsUnknown)
{
	ThorContextStore store;
	const auto context = store.snapshot();
	EXPECT_EQ(context.revision, 0);
	EXPECT_EQ(context.contextId, "UNKNOWN");
}

TEST(ThorContextStoreTest, AcceptsOnlyNewerRevision)
{
	ThorContextStore store;
	EXPECT_TRUE(store.publish({2, "MAIN_MENU", "", ""}));
	EXPECT_FALSE(store.publish({1, "UNKNOWN", "", ""}));
	EXPECT_EQ(store.snapshot().contextId, "MAIN_MENU");
}

TEST(ThorContextStoreTest, EmptyIdentifierFallsBackToUnknown)
{
	ThorContextStore store;
	EXPECT_TRUE(store.publish({1, "", "", ""}));
	EXPECT_EQ(store.snapshot().contextId, ThorContextIds::UNKNOWN);
}

TEST(ThorContextStoreTest, GeneratesGloballyMonotonicRevisions)
{
	ThorContextStore store;
	EXPECT_EQ(store.publishNext({0, "MAIN_MENU", "", ""}).revision, 1);
	EXPECT_EQ(store.publishNext({0, "LOBBY_NEW_GAME", "", ""}).revision, 2);
	EXPECT_EQ(store.publishNext({0, "ADVENTURE_MAP", "", ""}).revision, 3);
	EXPECT_EQ(store.publishNext({0, "BATTLE_TACTICS", "", ""}).revision, 4);
	EXPECT_EQ(store.publishNext({0, "BATTLE", "", ""}).revision, 5);
}

TEST(ThorContextStoreTest, AssignsNewRevisionWhenClearingToUnknown)
{
	ThorContextStore store;
	const auto known = store.publishNext({0, ThorContextIds::HERO_WINDOW, "", ""});
	const auto unknown = store.publishNext({0, ThorContextIds::UNKNOWN, "", ""});

	EXPECT_GT(unknown.revision, known.revision);
	EXPECT_EQ(unknown.contextId, ThorContextIds::UNKNOWN);
}

TEST(ThorContextMappingTest, MapsApprovedMainMenuTabs)
{
	EXPECT_EQ(thorContextIdForMainMenuTab("main"), ThorContextIds::MAIN_MENU);
	EXPECT_EQ(thorContextIdForMainMenuTab("new"), ThorContextIds::MAIN_MENU_NEW_GAME);
	EXPECT_EQ(thorContextIdForMainMenuTab("load"), ThorContextIds::MAIN_MENU_LOAD_GAME);
	EXPECT_EQ(thorContextIdForMainMenuTab("campaign"), ThorContextIds::MAIN_MENU_CAMPAIGN);
	EXPECT_EQ(thorContextIdForMainMenuTab("credits"), ThorContextIds::MAIN_MENU_CREDITS);
}

TEST(ThorContextMappingTest, UnsupportedMainMenuTabsFallBackToUnknown)
{
	EXPECT_EQ(thorContextIdForMainMenuTab(""), ThorContextIds::UNKNOWN);
	EXPECT_EQ(thorContextIdForMainMenuTab("load/"), ThorContextIds::UNKNOWN);
	EXPECT_EQ(thorContextIdForMainMenuTab("Campaign"), ThorContextIds::UNKNOWN);
	EXPECT_EQ(thorContextIdForMainMenuTab("campaign/"), ThorContextIds::UNKNOWN);
	EXPECT_EQ(thorContextIdForMainMenuTab("Credits"), ThorContextIds::UNKNOWN);
	EXPECT_EQ(thorContextIdForMainMenuTab("credits/"), ThorContextIds::UNKNOWN);
}

TEST(ThorContextMappingTest, MapsApprovedLobbyContexts)
{
	EXPECT_EQ(thorContextIdForLobby(ThorLobbyMode::NEW_GAME, ThorLobbyTab::NONE), ThorContextIds::LOBBY_NEW_GAME);
	EXPECT_EQ(thorContextIdForLobby(ThorLobbyMode::NEW_GAME, ThorLobbyTab::SCENARIO), ThorContextIds::LOBBY_NEW_GAME_SCENARIO);
	EXPECT_EQ(thorContextIdForLobby(ThorLobbyMode::NEW_GAME, ThorLobbyTab::OPTIONS), ThorContextIds::LOBBY_NEW_GAME_OPTIONS);
	EXPECT_EQ(thorContextIdForLobby(ThorLobbyMode::NEW_GAME, ThorLobbyTab::RANDOM_MAP), ThorContextIds::LOBBY_NEW_GAME_RANDOM_MAP);
	EXPECT_EQ(thorContextIdForLobby(ThorLobbyMode::NEW_GAME, ThorLobbyTab::TURN_OPTIONS), ThorContextIds::LOBBY_NEW_GAME_TURN_OPTIONS);
	EXPECT_EQ(thorContextIdForLobby(ThorLobbyMode::NEW_GAME, ThorLobbyTab::EXTRA_OPTIONS), ThorContextIds::LOBBY_NEW_GAME_EXTRA_OPTIONS);
	EXPECT_EQ(thorContextIdForLobby(ThorLobbyMode::NEW_GAME, ThorLobbyTab::BATTLE_MODE), ThorContextIds::LOBBY_NEW_GAME_BATTLE_MODE);
	EXPECT_EQ(thorContextIdForLobby(ThorLobbyMode::LOAD_GAME, ThorLobbyTab::NONE), ThorContextIds::LOBBY_LOAD_GAME);
	EXPECT_EQ(thorContextIdForLobby(ThorLobbyMode::LOAD_GAME, ThorLobbyTab::SCENARIO), ThorContextIds::LOBBY_LOAD_GAME_SCENARIO);
	EXPECT_EQ(thorContextIdForLobby(ThorLobbyMode::LOAD_GAME, ThorLobbyTab::OPTIONS), ThorContextIds::LOBBY_LOAD_GAME_OPTIONS);
	EXPECT_EQ(thorContextIdForLobby(ThorLobbyMode::LOAD_GAME, ThorLobbyTab::TURN_OPTIONS), ThorContextIds::LOBBY_LOAD_GAME_TURN_OPTIONS);
	EXPECT_EQ(thorContextIdForLobby(ThorLobbyMode::LOAD_GAME, ThorLobbyTab::EXTRA_OPTIONS), ThorContextIds::LOBBY_LOAD_GAME_EXTRA_OPTIONS);
	EXPECT_EQ(thorContextIdForLobby(ThorLobbyMode::CAMPAIGN_LIST, ThorLobbyTab::SCENARIO), ThorContextIds::LOBBY_CAMPAIGN_LIST);
}

TEST(ThorContextMappingTest, RejectsUnsupportedLobbyContexts)
{
	EXPECT_EQ(thorContextIdForLobby(ThorLobbyMode::UNKNOWN, ThorLobbyTab::SCENARIO), ThorContextIds::UNKNOWN);
	EXPECT_EQ(thorContextIdForLobby(ThorLobbyMode::NEW_GAME, ThorLobbyTab::UNKNOWN), ThorContextIds::UNKNOWN);
	EXPECT_EQ(thorContextIdForLobby(ThorLobbyMode::LOAD_GAME, ThorLobbyTab::RANDOM_MAP), ThorContextIds::UNKNOWN);
	EXPECT_EQ(thorContextIdForLobby(ThorLobbyMode::LOAD_GAME, ThorLobbyTab::BATTLE_MODE), ThorContextIds::UNKNOWN);
	EXPECT_EQ(thorContextIdForLobby(ThorLobbyMode::CAMPAIGN_LIST, ThorLobbyTab::OPTIONS), ThorContextIds::UNKNOWN);
	EXPECT_EQ(thorContextIdForLobby(ThorLobbyMode::CAMPAIGN_LIST, ThorLobbyTab::NONE), ThorContextIds::UNKNOWN);
}

TEST(ThorContextMappingTest, MapsApprovedInGameContexts)
{
	EXPECT_EQ(thorContextIdForInGameContext(ThorInGameContext::ADVENTURE_MAP), ThorContextIds::ADVENTURE_MAP);
	EXPECT_EQ(thorContextIdForInGameContext(ThorInGameContext::HERO_WINDOW), ThorContextIds::HERO_WINDOW);
	EXPECT_EQ(thorContextIdForInGameContext(ThorInGameContext::TOWN_WINDOW), ThorContextIds::TOWN_WINDOW);
	EXPECT_EQ(thorContextIdForInGameContext(ThorInGameContext::HERO_MEETING), ThorContextIds::HERO_MEETING);
	EXPECT_EQ(thorContextIdForInGameContext(ThorInGameContext::BATTLE), ThorContextIds::BATTLE);
	EXPECT_EQ(thorContextIdForInGameContext(ThorInGameContext::BATTLE_TACTICS), ThorContextIds::BATTLE_TACTICS);
	EXPECT_EQ(thorContextIdForInGameContext(ThorInGameContext::BATTLE_RESULT), ThorContextIds::BATTLE_RESULT);
	EXPECT_EQ(thorContextIdForInGameContext(ThorInGameContext::KINGDOM_OVERVIEW), ThorContextIds::KINGDOM_OVERVIEW);
	EXPECT_EQ(thorContextIdForInGameContext(ThorInGameContext::QUEST_LOG), ThorContextIds::QUEST_LOG);
	EXPECT_EQ(thorContextIdForInGameContext(ThorInGameContext::SCENARIO_EVENT_JOURNAL), ThorContextIds::SCENARIO_EVENT_JOURNAL);
	EXPECT_EQ(thorContextIdForInGameContext(ThorInGameContext::PUZZLE_MAP), ThorContextIds::PUZZLE_MAP);
	EXPECT_EQ(thorContextIdForInGameContext(ThorInGameContext::SAVE_GAME), ThorContextIds::SAVE_GAME);
}

TEST(ThorContextMappingTest, RejectsUnsupportedInGameContexts)
{
	EXPECT_EQ(thorContextIdForInGameContext(ThorInGameContext::UNKNOWN), ThorContextIds::UNKNOWN);
	EXPECT_EQ(thorContextIdForInGameContext(static_cast<ThorInGameContext>(99)), ThorContextIds::UNKNOWN);
}

TEST(ThorContextStoreTest, TacticsTransitionReceivesNewerRevision)
{
	ThorContextStore store;
	const auto tactics = store.publishNext({0, ThorContextIds::BATTLE_TACTICS, "", ""});
	const auto battle = store.publishNext({0, ThorContextIds::BATTLE, "", ""});

	EXPECT_GT(battle.revision, tactics.revision);
	EXPECT_EQ(battle.contextId, ThorContextIds::BATTLE);
}

TEST(ThorContextStoreTest, AdventureUtilityTransitionsReceiveNewerRevisions)
{
	ThorContextStore store;
	const auto adventureMap = store.publishNext({0, ThorContextIds::ADVENTURE_MAP, "", ""});
	const auto kingdomOverview = store.publishNext({0, ThorContextIds::KINGDOM_OVERVIEW, "", ""});
	const auto unknown = store.publishNext({0, ThorContextIds::UNKNOWN, "", ""});
	const auto restoredAdventureMap = store.publishNext({0, ThorContextIds::ADVENTURE_MAP, "", ""});

	EXPECT_LT(adventureMap.revision, kingdomOverview.revision);
	EXPECT_LT(kingdomOverview.revision, unknown.revision);
	EXPECT_LT(unknown.revision, restoredAdventureMap.revision);
	EXPECT_EQ(unknown.contextId, ThorContextIds::UNKNOWN);
}

TEST(ThorContextStoreTest, AdventureUtilityContextsReceiveNewerRevisions)
{
	ThorContextStore store;
	const auto adventureMap = store.publishNext({0, ThorContextIds::ADVENTURE_MAP, "", ""});
	const std::array contextIds = {
		ThorContextIds::QUEST_LOG,
		ThorContextIds::SCENARIO_EVENT_JOURNAL,
		ThorContextIds::PUZZLE_MAP,
		ThorContextIds::SAVE_GAME
	};

	for(const auto * contextId : contextIds)
	{
		const auto context = store.publishNext({0, contextId, "", ""});
		const auto unknown = store.publishNext({0, ThorContextIds::UNKNOWN, "", ""});
		const auto restoredAdventureMap = store.publishNext({0, ThorContextIds::ADVENTURE_MAP, "", ""});

		EXPECT_GT(context.revision, adventureMap.revision);
		EXPECT_GT(unknown.revision, context.revision);
		EXPECT_GT(restoredAdventureMap.revision, unknown.revision);
		EXPECT_EQ(unknown.contextId, ThorContextIds::UNKNOWN);
	}
}
