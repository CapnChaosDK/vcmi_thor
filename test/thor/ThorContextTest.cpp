#include "StdInc.h"

#include "../../lib/thor/ThorAction.h"
#include "../../lib/thor/ThorContext.h"

TEST(ThorContextStoreTest, StartsUnknown)
{
	ThorContextStore store;
	const auto context = store.snapshot();
	EXPECT_EQ(context.revision, 0);
	EXPECT_EQ(context.contextId, "UNKNOWN");
}

TEST(ThorContextStoreTest, BuildConfirmationRetainsTownAndBuildingIdentity)
{
	ThorContextStore store;
	ThorContextRecord context;
	context.contextId = ThorContextIds::BUILD_CONFIRMATION;
	context.actionSubjectId = 12;
	context.windowSubjectId = 34;
	context.title = "Town";
	context.details[0] = "Building";
	context.enabledActionMask = thorActionMask(ThorAction::WINDOW_CONFIRM)
		| thorActionMask(ThorAction::WINDOW_CLOSE);
	const auto published = store.publishNext(context);
	EXPECT_EQ(published.actionSubjectId, 12);
	EXPECT_EQ(published.windowSubjectId, 34);
	EXPECT_EQ(published.enabledActionMask, context.enabledActionMask);
	EXPECT_TRUE(isThorActionAllowedInContext(ThorAction::WINDOW_CONFIRM, published.contextId));
	EXPECT_FALSE(isThorActionAllowedInContext(ThorAction::WINDOW_CONFIRM, ThorContextIds::TOWN_HALL));
	EXPECT_TRUE(thorBuildConfirmationOwnerMatches(published, 12, 34, true, true));
	EXPECT_FALSE(thorBuildConfirmationOwnerMatches(published, 13, 34, true, true));
	EXPECT_FALSE(thorBuildConfirmationOwnerMatches(published, 12, 35, true, true));
	EXPECT_FALSE(thorBuildConfirmationOwnerMatches(published, 12, 34, false, true));
	EXPECT_FALSE(thorBuildConfirmationOwnerMatches(published, 12, 34, true, false));
	EXPECT_EQ(store.publishNext(context).revision, published.revision);
}

TEST(ThorContextStoreTest, MenuModalPurposeRequiresExactSemanticSourceAndActiveTopOwner)
{
	int quitOwner = 0;
	int quitParent = 0;
	ThorContextRecord quit;
	quit.contextId = ThorContextIds::MENU_QUIT_CONFIRMATION;
	quit.menuModalType = ThorMenuModalType::QUIT_CONFIRMATION;
	quit.menuModalSourceContext = ThorContextIds::MAIN_MENU;
	quit.nativeOwnerToken = &quitOwner;
	quit.nativeParentToken = &quitParent;
	EXPECT_TRUE(thorMenuModalOwnerMatches(quit, ThorMenuModalType::QUIT_CONFIRMATION,
		ThorContextIds::MAIN_MENU, &quitOwner, &quitParent, true, true));
	EXPECT_FALSE(thorMenuModalOwnerMatches(quit, ThorMenuModalType::HIGH_SCORE_RESET_CONFIRMATION,
		ThorContextIds::MAIN_MENU, &quitOwner, &quitParent, true, true));
	EXPECT_FALSE(thorMenuModalOwnerMatches(quit, ThorMenuModalType::QUIT_CONFIRMATION,
		ThorContextIds::MAIN_MENU_NEW_GAME, &quitOwner, &quitParent, true, true));
	EXPECT_FALSE(thorMenuModalOwnerMatches(quit, ThorMenuModalType::QUIT_CONFIRMATION,
		ThorContextIds::MAIN_MENU, &quitOwner, &quitParent, false, true));
	EXPECT_FALSE(thorMenuModalOwnerMatches(quit, ThorMenuModalType::QUIT_CONFIRMATION,
		ThorContextIds::MAIN_MENU, &quitOwner, &quitParent, true, false));
	EXPECT_FALSE(thorMenuModalOwnerMatches(quit, ThorMenuModalType::QUIT_CONFIRMATION,
		ThorContextIds::MAIN_MENU, &quitParent, &quitParent, true, true));
	EXPECT_FALSE(thorMenuModalOwnerMatches(quit, ThorMenuModalType::QUIT_CONFIRMATION,
		ThorContextIds::MAIN_MENU, &quitOwner, &quitOwner, true, true));

	int resetOwner = 0;
	int resetParent = 0;
	ThorContextRecord reset;
	reset.contextId = ThorContextIds::HIGH_SCORE_RESET_CONFIRMATION;
	reset.menuModalType = ThorMenuModalType::HIGH_SCORE_RESET_CONFIRMATION;
	reset.menuModalSourceContext = ThorContextIds::HIGH_SCORES;
	reset.nativeOwnerToken = &resetOwner;
	reset.nativeParentToken = &resetParent;
	EXPECT_TRUE(thorMenuModalOwnerMatches(reset, ThorMenuModalType::HIGH_SCORE_RESET_CONFIRMATION,
		ThorContextIds::HIGH_SCORES, &resetOwner, &resetParent, true, true));
	EXPECT_FALSE(thorMenuModalOwnerMatches(reset, ThorMenuModalType::HIGH_SCORE_RESET_CONFIRMATION,
		ThorContextIds::MAIN_MENU, &resetOwner, &resetParent, true, true));

	int tutorialOwner = 0;
	int tutorialParent = 0;
	ThorContextRecord tutorial;
	tutorial.contextId = ThorContextIds::TUTORIAL_MISSING_DIALOG;
	tutorial.menuModalType = ThorMenuModalType::MISSING_TUTORIAL_INFORMATION;
	tutorial.menuModalSourceContext = ThorContextIds::MAIN_MENU_LOAD_GAME;
	tutorial.nativeOwnerToken = &tutorialOwner;
	tutorial.nativeParentToken = &tutorialParent;
	EXPECT_TRUE(thorMenuModalOwnerMatches(tutorial, ThorMenuModalType::MISSING_TUTORIAL_INFORMATION,
		ThorContextIds::MAIN_MENU_LOAD_GAME, &tutorialOwner, &tutorialParent, true, true));
	EXPECT_FALSE(thorMenuModalOwnerMatches(tutorial, ThorMenuModalType::MISSING_TUTORIAL_INFORMATION,
		ThorContextIds::HIGH_SCORES, &tutorialOwner, &tutorialParent, true, true));

	ThorContextRecord arbitrary;
	arbitrary.contextId = "UNRELATED_INFO_WINDOW";
	EXPECT_FALSE(thorMenuModalOwnerMatches(arbitrary, ThorMenuModalType::QUIT_CONFIRMATION,
		ThorContextIds::MAIN_MENU, &quitOwner, &quitParent, true, true));
}

TEST(ThorContextStoreTest, MenuModalNativeOwnerChangeAdvancesRevision)
{
	ThorContextStore store;
	ThorContextRecord context;
	context.contextId = ThorContextIds::MENU_QUIT_CONFIRMATION;
	context.menuModalType = ThorMenuModalType::QUIT_CONFIRMATION;
	context.menuModalSourceContext = ThorContextIds::MAIN_MENU;
	int firstOwner = 0;
	int secondOwner = 0;
	context.nativeOwnerToken = &firstOwner;
	context.nativeParentToken = &firstOwner;
	const auto first = store.publishNext(context);
	EXPECT_EQ(store.publishNext(context).revision, first.revision);
	context.nativeOwnerToken = &secondOwner;
	context.nativeParentToken = &secondOwner;
	const auto second = store.publishNext(context);
	EXPECT_GT(second.revision, first.revision);
}

TEST(ThorContextStoreTest, HeroRosterChangesOnceAndClearsOutsideAdventure)
{
	ThorContextStore store;
	ThorContextRecord context;
	context.contextId = ThorContextIds::ADVENTURE_MAP;
	context.heroes = {{4, "Hero", 100, 200, true, false}};
	const auto first = store.publishNext(context);
	EXPECT_EQ(store.publishNext(context).revision, first.revision);
	context.heroes[0].selected = false;
	const auto selected = store.publishNext(context);
	EXPECT_EQ(selected.revision, first.revision + 1);
	context.heroes[0].movement = 80;
	const auto moved = store.publishNext(context);
	EXPECT_EQ(moved.revision, selected.revision + 1);
	context.heroes[0].sleeping = true;
	const auto asleep = store.publishNext(context);
	EXPECT_EQ(asleep.revision, moved.revision + 1);
	context.heroes.push_back({5, "Other", 200, 300, false, false});
	const auto added = store.publishNext(context);
	EXPECT_EQ(added.revision, asleep.revision + 1);
	context.heroes.pop_back();
	EXPECT_EQ(store.publishNext(context).revision, added.revision + 1);
	context.contextId = ThorContextIds::HERO_WINDOW;
	EXPECT_TRUE(store.publishNext(context).heroes.empty());
}

TEST(ThorContextStoreTest, HeroNamesAreBoundedAtUtf8BoundariesAndOverflowFailsClosed)
{
	ThorContextStore store;
	ThorContextRecord context;
	context.contextId = ThorContextIds::ADVENTURE_MAP;
	context.heroes = {{4, std::string(127, 'A') + "\xc3\xa9", 0, 0, false, false}};
	const auto bounded = store.publishNext(context);
	ASSERT_EQ(bounded.heroes.size(), 1);
	EXPECT_EQ(bounded.heroes[0].name, std::string(127, 'A'));
	context.heroes[0].name = "broken\xc3";
	EXPECT_EQ(store.publishNext(context).heroes[0].name, "broken");
	context.heroes.resize(THOR_MAX_HEROES + 1);
	EXPECT_TRUE(store.publishNext(context).heroes.empty());
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

TEST(ThorContextStoreTest, ActionMasksRetainAll64Bits)
{
	ThorContextStore store;
	ThorContextRecord context;
	context.contextId = ThorContextIds::ADVENTURE_MAP;
	context.enabledActionMask = (std::uint64_t{1} << 7) | (std::uint64_t{1} << 40);
	context.activeActionMask = std::uint64_t{1} << 63;
	const auto published = store.publishNext(context);
	const auto snapshot = store.snapshot();
	EXPECT_EQ(snapshot.enabledActionMask, published.enabledActionMask);
	EXPECT_EQ(snapshot.enabledActionMask, (std::uint64_t{1} << 7) | (std::uint64_t{1} << 40));
	EXPECT_EQ(snapshot.activeActionMask, std::uint64_t{1} << 63);
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
	EXPECT_EQ(thorContextIdForInGameContext(ThorInGameContext::TOWN_HALL), ThorContextIds::TOWN_HALL);
	EXPECT_EQ(thorContextIdForInGameContext(ThorInGameContext::TOWN_RECRUITMENT_QUICK), ThorContextIds::TOWN_RECRUITMENT_QUICK);
	EXPECT_EQ(thorContextIdForInGameContext(ThorInGameContext::TOWN_RECRUITMENT_DWELLING), ThorContextIds::TOWN_RECRUITMENT_DWELLING);
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

TEST(ThorContextStoreTest, ActionAvailabilityIsRevisionBoundWithoutChurn)
{
	ThorContextStore store;
	const auto adventureMap = store.publishNext({0, ThorContextIds::ADVENTURE_MAP, "", "", 1});
	const auto unchanged = store.publishNext({0, ThorContextIds::ADVENTURE_MAP, "", "", 1});
	const auto changed = store.publishNext({0, ThorContextIds::ADVENTURE_MAP, "", "", 3});

	EXPECT_EQ(unchanged.revision, adventureMap.revision);
	EXPECT_GT(changed.revision, adventureMap.revision);
	EXPECT_EQ(changed.enabledActionMask, 3);
}

TEST(ThorContextStoreTest, NativeScenarioSelectionChangesInvalidateRenderedRequests)
{
	ThorContextStore store;
	ThorContextRecord scenario;
	scenario.contextId = ThorContextIds::LOBBY_NEW_GAME_SCENARIO;
	scenario.enabledActionMask = thorActionMask(ThorAction::LOBBY_PREVIOUS_SCENARIO)
		| thorActionMask(ThorAction::LOBBY_NEXT_SCENARIO);
	scenario.scenarioSelectionRevision = 10;
	const auto rendered = store.publishNext(scenario);
	const ThorActionRequest renderedTap{.revision = rendered.revision, .action = ThorAction::LOBBY_NEXT_SCENARIO};
	EXPECT_EQ(store.publishNext(scenario).revision, rendered.revision);

	scenario.scenarioSelectionRevision = 11;
	const auto changed = store.publishNext(scenario);
	EXPECT_GT(changed.revision, rendered.revision);
	EXPECT_EQ(changed.enabledActionMask, rendered.enabledActionMask);
	EXPECT_EQ(changed.scenarioSelectionRevision, 11);
	EXPECT_EQ(validateThorActionRequest(renderedTap, changed), ThorActionValidation::STALE_REVISION);
}

TEST(ThorContextStoreTest, NativeCampaignSelectionChangesInvalidateRenderedRequests)
{
	ThorContextStore store;
	ThorContextRecord campaign;
	campaign.contextId = ThorContextIds::CAMPAIGN_BONUS_SELECTION;
	campaign.enabledActionMask = thorActionMask(ThorAction::CAMPAIGN_NEXT_SCENARIO);
	campaign.campaignSelectionRevision = 41;
	const auto rendered = store.publishNext(campaign);
	const ThorActionRequest renderedTap{.revision = rendered.revision,
		.action = ThorAction::CAMPAIGN_NEXT_SCENARIO};
	EXPECT_EQ(store.publishNext(campaign).revision, rendered.revision);

	campaign.campaignSelectionRevision = 42;
	const auto changed = store.publishNext(campaign);
	EXPECT_GT(changed.revision, rendered.revision);
	EXPECT_EQ(changed.enabledActionMask, rendered.enabledActionMask);
	EXPECT_EQ(validateThorActionRequest(renderedTap, changed), ThorActionValidation::STALE_REVISION);
}

TEST(ThorContextStoreTest, BrowserPageAndNativeOnlyIdentityChangesInvalidateOldRows)
{
	ThorContextStore store;
	ThorContextRecord browser;
	browser.contextId = ThorContextIds::CAMPAIGN_BROWSER;
	browser.browserPageCount = 2;
	browser.browserEntries = {{0, "First", true, false, false}};
	browser.browserNativeKeys = {"1:campaign-a"};
	browser.enabledActionMask = thorActionMask(ThorAction::CAMPAIGN_BROWSER_SELECT);
	const auto first = store.publishNext(browser);
	EXPECT_EQ(store.publishNext(browser).revision, first.revision);
	browser.browserPage = 1;
	browser.browserEntries = {{1, "Second", true, false, true}};
	browser.browserNativeKeys = {"2:campaign-b"};
	const auto second = store.publishNext(browser);
	EXPECT_GT(second.revision, first.revision);
	EXPECT_EQ(validateThorActionRequest({first.revision, ThorAction::CAMPAIGN_BROWSER_SELECT, 0}, second),
		ThorActionValidation::STALE_REVISION);
	browser.browserNativeKeys = {"2:reordered"};
	const auto reordered = store.publishNext(browser);
	EXPECT_GT(reordered.revision, second.revision);
	browser.browserEntries.resize(THOR_BROWSER_MAX_ROWS + 1);
	const auto invalid = store.publishNext(browser);
	EXPECT_TRUE(invalid.browserEntries.empty());
}

TEST(ThorContextStoreTest, TownServicePayloadRequiresExactFixedTargets)
{
	ThorContextStore store;
	ThorContextRecord town;
	town.contextId = ThorContextIds::TOWN_WINDOW;
	town.browserPageCount = 1;
	for(int target = 0; target < static_cast<int>(THOR_TOWN_SERVICE_COUNT); ++target)
		town.browserEntries.push_back({target, "", target != 1, false, false});
	town.enabledActionMask = thorActionMask(ThorAction::TOWN_OPEN_SERVICE);
	const auto valid = store.publishNext(town);
	EXPECT_EQ(valid.browserEntries.size(), THOR_TOWN_SERVICE_COUNT);
	EXPECT_NE(valid.enabledActionMask & thorActionMask(ThorAction::TOWN_OPEN_SERVICE), 0);
	town.browserEntries[3].target = 4;
	const auto invalid = store.publishNext(town);
	EXPECT_TRUE(invalid.browserEntries.empty());
	EXPECT_EQ(invalid.enabledActionMask & thorActionMask(ThorAction::TOWN_OPEN_SERVICE), 0);
}

TEST(ThorContextStoreTest, TownHallListIsBoundedRevisionSemanticAndFailClosed)
{
	ThorContextStore store;
	ThorContextRecord hall;
	hall.contextId = ThorContextIds::TOWN_HALL;
	hall.title = "Castle Town";
	hall.actionSubjectId = 44;
	hall.browserPageCount = 1;
	hall.enabledActionMask = thorActionMask(ThorAction::WINDOW_CLOSE)
		| thorActionMask(ThorAction::TOWN_HALL_BUILD);
	hall.browserEntries = {{901, "Village Hall, 500 gold", true, false, false},
		{902, "Tavern, 500 gold", false, false, true}};
	hall.browserNativeKeys = {"901", "902"};
	const auto rendered = store.publishNext(hall);
	EXPECT_EQ(store.publishNext(hall).revision, rendered.revision);
	hall.browserEntries[0].enabled = false; // Resources or prerequisites changed.
	const auto unavailable = store.publishNext(hall);
	EXPECT_GT(unavailable.revision, rendered.revision);
	EXPECT_EQ(validateThorActionRequest({rendered.revision, ThorAction::TOWN_HALL_BUILD, 901}, unavailable),
		ThorActionValidation::STALE_REVISION);
	hall.actionSubjectId = 45; // A different Town owns the same menu choices.
	const auto otherTown = store.publishNext(hall);
	EXPECT_GT(otherTown.revision, unavailable.revision);
	hall.browserEntries.resize(THOR_MAX_TOWN_HALL_BUILDINGS + 1);
	hall.browserNativeKeys.resize(hall.browserEntries.size());
	hall.browserPageCount = static_cast<int>((hall.browserEntries.size() + 4) / 5);
	const auto oversized = store.publishNext(hall);
	EXPECT_TRUE(oversized.browserEntries.empty());
	EXPECT_TRUE(oversized.browserNativeKeys.empty());
	EXPECT_EQ(oversized.enabledActionMask & thorActionMask(ThorAction::TOWN_HALL_BUILD), 0);
}

TEST(ThorContextStoreTest, TownHallRejectsDuplicateOrMismatchedNativeBuildingIdentities)
{
	ThorContextStore store;
	ThorContextRecord hall;
	hall.contextId = ThorContextIds::TOWN_HALL;
	hall.actionSubjectId = 12;
	hall.browserPageCount = 1;
	hall.enabledActionMask = thorActionMask(ThorAction::TOWN_HALL_BUILD)
		| thorActionMask(ThorAction::WINDOW_CLOSE);
	hall.browserEntries = {{1, "First", true, false, false}, {1, "Duplicate", true, false, false}};
	hall.browserNativeKeys = {"1", "1"};
	const auto duplicate = store.publishNext(hall);
	EXPECT_TRUE(duplicate.browserEntries.empty());
	EXPECT_EQ(duplicate.enabledActionMask & thorActionMask(ThorAction::TOWN_HALL_BUILD), 0);
	EXPECT_EQ(duplicate.actionSubjectId, 12);
	EXPECT_EQ(validateThorActionRequest({duplicate.revision, ThorAction::WINDOW_CLOSE}, duplicate), ThorActionValidation::VALID);
	hall.browserEntries[1].target = 2;
	hall.browserNativeKeys[1] = "not-2";
	const auto mismatched = store.publishNext(hall);
	EXPECT_TRUE(mismatched.browserEntries.empty());
	EXPECT_EQ(mismatched.enabledActionMask & thorActionMask(ThorAction::TOWN_HALL_BUILD), 0);
	EXPECT_EQ(mismatched.actionSubjectId, 12);
	EXPECT_EQ(validateThorActionRequest({mismatched.revision, ThorAction::WINDOW_CLOSE}, mismatched), ThorActionValidation::VALID);
}

TEST(ThorContextStoreTest, HeroMeetingRedistributionConsumptionAndRestoreAreSemanticRevisions)
{
	ThorContextStore store;
	ThorContextRecord meeting;
	meeting.contextId = ThorContextIds::HERO_MEETING;
	meeting.enabledActionMask = thorActionMask(ThorAction::HERO_MEETING_REDISTRIBUTE_STACK);
	const auto ready = store.publishNext(meeting);
	const auto unchanged = store.publishNext(meeting);
	EXPECT_EQ(unchanged.revision, ready.revision);
	meeting.actionEpoch = ready.actionEpoch + 1;
	meeting.enabledActionMask = 0;
	const auto consumed = store.publishNext(meeting);
	EXPECT_GT(consumed.revision, ready.revision);
	meeting.enabledActionMask = thorActionMask(ThorAction::HERO_MEETING_REDISTRIBUTE_STACK);
	const auto restored = store.publishNext(meeting);
	EXPECT_GT(restored.revision, consumed.revision);
	EXPECT_EQ(restored.enabledActionMask, thorActionMask(ThorAction::HERO_MEETING_REDISTRIBUTE_STACK));
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

TEST(ThorContextPayloadTest, BoundsTextWithoutSplittingUtf8)
{
	EXPECT_EQ(thorBoundedText("Catherine", 5), "Cathe");
	EXPECT_EQ(thorBoundedText("Crag Hack", 32), "Crag Hack");
	EXPECT_EQ(thorBoundedText("Gelu \xc3\xa9lite", 6), "Gelu ");
}

TEST(ThorContextPayloadTest, BoundsEveryDetailLineWithoutSplittingUtf8)
{
	ThorContextStore store;
	ThorContextRecord context;
	context.contextId = ThorContextIds::HERO_WINDOW;
	context.details = {
		std::string(129, 'a'),
		std::string(128, 'b') + "\xc3\xa9",
		std::string(129, 'c'),
		std::string(129, 'd')
	};

	const auto published = store.publishNext(std::move(context));
	for(const auto & detail : published.details)
		EXPECT_LE(detail.size(), 128);
	EXPECT_EQ(published.details[1], std::string(128, 'b'));
}

TEST(ThorContextStoreTest, HeroMeetingArmySnapshotIsRevisionBoundAndClearsOutsideContext)
{
	ThorContextStore store;
	ThorContextRecord context;
	context.contextId = ThorContextIds::HERO_MEETING;
	ThorHeroMeetingArmies armies;
	armies.leftHeroId = 11;
	armies.rightHeroId = 12;
	armies.leftArmyId = 11;
	armies.rightArmyId = 12;
	armies.leftHeroName = std::string(128, 'L') + "x";
	armies.rightHeroName = "Right";
	for(std::size_t index = 0; index < THOR_HERO_MEETING_ARMY_SIZE; ++index)
	{
		armies.leftSlots[index] = {11, static_cast<int>(index), false, -1, "", 0};
		armies.rightSlots[index] = {12, static_cast<int>(index), false, -1, "", 0};
	}
	armies.leftSlots[0] = {11, 0, true, 4, std::string(128, 'P') + "x", 15};
	context.heroMeetingArmies = armies;
	const auto first = store.publishNext(context);
	const auto unchanged = store.publishNext(context);
	EXPECT_EQ(first.revision, unchanged.revision);
	EXPECT_EQ(first.heroMeetingArmies->leftHeroName.size(), 128);
	EXPECT_EQ(first.heroMeetingArmies->leftSlots[0].creatureName.size(), 128);
	EXPECT_EQ(first.heroMeetingArmies->leftSlots[0].visualAssetKey, thorCreatureVisualAssetKey(4));
	context.heroMeetingArmies->leftSlots[0].count = 16;
	const auto changedCount = store.publishNext(context);
	EXPECT_GT(changedCount.revision, first.revision);
	context.heroMeetingArmies->leftSlots[0] = {11, 0, false, -1, "", 0};
	context.heroMeetingArmies->rightSlots[1] = {12, 1, true, 4, "Pikemen", 16};
	const auto movedStack = store.publishNext(context);
	EXPECT_GT(movedStack.revision, changedCount.revision);
	EXPECT_EQ(movedStack.heroMeetingArmies->rightSlots[1].visualAssetKey, thorCreatureVisualAssetKey(4));
	context.contextId = ThorContextIds::UNKNOWN;
	const auto cleared = store.publishNext(context);
	EXPECT_FALSE(cleared.heroMeetingArmies.has_value());
}

TEST(ThorContextStoreTest, HeroMeetingArtifactSnapshotIsBoundedRevisionBoundAndCleared)
{
	ThorContextStore store;
	ThorContextRecord context;
	context.contextId = ThorContextIds::HERO_MEETING;
	ThorHeroMeetingArtifacts artifacts;
	artifacts.leftHeroId = 11;
	artifacts.rightHeroId = 12;
	artifacts.leftHeroName = "Left";
	artifacts.rightHeroName = "Right";
	for(std::size_t index = 0; index < THOR_HERO_MEETING_ARTIFACT_COUNT; ++index)
	{
		const auto sideOffset = index % (THOR_HERO_MEETING_EQUIPPED_ARTIFACT_COUNT + THOR_HERO_MEETING_BACKPACK_ARTIFACT_COUNT);
		artifacts.artifactSlots.push_back({index < THOR_HERO_MEETING_ARTIFACT_COUNT / 2 ? 11 : 12,
			static_cast<int>(sideOffset), sideOffset >= THOR_HERO_MEETING_EQUIPPED_ARTIFACT_COUNT,
			false, index == 0, "", -1});
	}
	artifacts.artifactSlots[1].occupied = true;
	artifacts.artifactSlots[1].instanceId = 42;
	artifacts.artifactSlots[1].artifactTypeId = 8;
	artifacts.artifactSlots[1].visualAssetKey = thorCreatureVisualAssetKey(123); // Store canonicalizes decorative identity.
	artifacts.artifactSlots[1].name = std::string(128, 'A') + "x";
	context.heroMeetingArtifacts = artifacts;
	const auto first = store.publishNext(context);
	ASSERT_TRUE(first.heroMeetingArtifacts);
	EXPECT_EQ(first.heroMeetingArtifacts->artifactSlots.size(), THOR_HERO_MEETING_ARTIFACT_COUNT);
	EXPECT_EQ(first.heroMeetingArtifacts->artifactSlots[1].name.size(), 128);
	EXPECT_EQ(first.heroMeetingArtifacts->artifactSlots[1].visualAssetKey, thorArtifactVisualAssetKey(8));
	EXPECT_EQ(store.publishNext(context).revision, first.revision);
	context.heroMeetingArtifacts->artifactSlots[1].instanceId = 43;
	const auto replaced = store.publishNext(context);
	EXPECT_EQ(replaced.revision, first.revision + 1);
	EXPECT_EQ(replaced.heroMeetingArtifacts->artifactSlots[1].visualAssetKey, first.heroMeetingArtifacts->artifactSlots[1].visualAssetKey);
	context.heroMeetingArtifacts->artifactSlots[1].artifactTypeId = 9;
	const auto differentType = store.publishNext(context);
	EXPECT_NE(differentType.heroMeetingArtifacts->artifactSlots[1].visualAssetKey,
		first.heroMeetingArtifacts->artifactSlots[1].visualAssetKey);
	context.heroMeetingArtifacts->artifactSlots[1].name = "Changed";
	EXPECT_EQ(store.publishNext(context).revision, differentType.revision + 1);
	context.contextId = ThorContextIds::UNKNOWN;
	EXPECT_FALSE(store.publishNext(context).heroMeetingArtifacts);
}

TEST(ThorContextStoreTest, HeroMeetingArtifactSnapshotRejectsMalformedShapeAndIdentity)
{
	ThorContextStore store;
	ThorContextRecord context;
	context.contextId = ThorContextIds::HERO_MEETING;
	context.heroMeetingArtifacts = ThorHeroMeetingArtifacts{1, 2, "Left", "Right", {}};
	EXPECT_FALSE(store.publishNext(context).heroMeetingArtifacts);

	auto artifacts = ThorHeroMeetingArtifacts{1, 2, "Left", "Right", {}};
	for(std::size_t index = 0; index < THOR_HERO_MEETING_ARTIFACT_COUNT; ++index)
	{
		const auto offset = index % 24;
		artifacts.artifactSlots.push_back({index < 24 ? 1 : 2, static_cast<int>(offset), offset >= 19, false, false, "", -1});
	}
	artifacts.artifactSlots[24].heroId = 1;
	context.heroMeetingArtifacts = artifacts;
	EXPECT_FALSE(store.publishNext(context).heroMeetingArtifacts);
	artifacts.artifactSlots[24].heroId = 2;
	artifacts.artifactSlots[0].occupied = true;
	artifacts.artifactSlots[0].name = "Unidentified";
	context.heroMeetingArtifacts = artifacts;
	EXPECT_FALSE(store.publishNext(context).heroMeetingArtifacts);
}

TEST(ThorContextStoreTest, BattleDashboardSnapshotIsAtomicAndDoesNotChurn)
{
	ThorContextStore store;
	ThorContextRecord battle;
	battle.contextId = ThorContextIds::BATTLE;
	battle.title = std::string(128, 'R') + "\xc3\xa9";
	battle.status = "3";
	battle.details = {std::string(129, '2'), "14", "12", "18 / 30"};
	battle.enabledActionMask = thorActionMask(ThorAction::BATTLE_WAIT)
		| thorActionMask(ThorAction::BATTLE_DEFEND);
	battle.actionSubjectId = 101;

	const auto initial = store.publishNext(battle);
	const auto unchanged = store.publishNext(battle);
	EXPECT_EQ(initial.title, std::string(128, 'R'));
	EXPECT_EQ(initial.details[0], std::string(128, '2'));
	EXPECT_EQ(initial.details[1], "14");
	EXPECT_EQ(initial.details[2], "12");
	EXPECT_EQ(initial.details[3], "18 / 30");
	EXPECT_EQ(unchanged.revision, initial.revision);

	battle.details[0] = "23";
	const auto changedCount = store.publishNext(battle);
	battle.details[1] = "15";
	const auto changedAttack = store.publishNext(battle);
	battle.details[2] = "13";
	const auto changedDefense = store.publishNext(battle);
	battle.details[3] = "17 / 30";
	const auto changedHealth = store.publishNext(battle);
	battle.status = "4";
	const auto changedRound = store.publishNext(battle);
	battle.actionSubjectId = 202;
	const auto changedStack = store.publishNext(battle);

	EXPECT_EQ(changedCount.revision, initial.revision + 1);
	EXPECT_EQ(changedAttack.revision, changedCount.revision + 1);
	EXPECT_EQ(changedDefense.revision, changedAttack.revision + 1);
	EXPECT_EQ(changedHealth.revision, changedDefense.revision + 1);
	EXPECT_EQ(changedRound.revision, changedHealth.revision + 1);
	EXPECT_EQ(changedStack.revision, changedRound.revision + 1);
}

TEST(ThorContextStoreTest, BattleSnapshotClearsOnMissingStackAndContextTransition)
{
	ThorContextStore store;
	ThorContextRecord battle;
	battle.contextId = ThorContextIds::BATTLE;
	battle.title = "Royal Griffin";
	battle.status = "3";
	battle.details = {"24", "14", "12", "18 / 30"};
	battle.actionSubjectId = 101;
	const auto active = store.publishNext(battle);

	battle.title.clear();
	battle.details = {};
	battle.actionSubjectId = -1;
	const auto noActiveStack = store.publishNext(battle);
	EXPECT_EQ(noActiveStack.revision, active.revision + 1);
	EXPECT_TRUE(noActiveStack.title.empty());
	EXPECT_EQ(noActiveStack.details, ThorContextDetails{});
	EXPECT_EQ(noActiveStack.actionSubjectId, -1);

	ThorContextRecord tactics;
	tactics.contextId = ThorContextIds::BATTLE_TACTICS;
	const auto tacticsWithoutStack = store.publishNext(tactics);
	EXPECT_TRUE(tacticsWithoutStack.title.empty());
	EXPECT_TRUE(tacticsWithoutStack.status.empty());
	EXPECT_EQ(tacticsWithoutStack.details, ThorContextDetails{});
	EXPECT_EQ(tacticsWithoutStack.actionSubjectId, -1);

	ThorContextRecord unknown;
	unknown.contextId = ThorContextIds::UNKNOWN;
	unknown.details = {"24", "14", "12", "18 / 30"};
	const auto cleared = store.publishNext(unknown);
	EXPECT_EQ(cleared.details, ThorContextDetails{});
}

TEST(ThorContextStoreTest, HeroDetailsChangeRevisionExactlyOnceWithoutChurn)
{
	ThorContextStore store;
	ThorContextRecord hero;
	hero.contextId = ThorContextIds::HERO_WINDOW;
	hero.title = "Catherine";
	hero.status = "Level 8 Knight";
	hero.details = {
		"Attack 4 · Defense 6",
		"Spell Power 2 · Knowledge 3",
		"Mana 24 / 30 · Experience 11200 / 14700",
		""
	};

	const auto initial = store.publishNext(hero);
	const auto unchanged = store.publishNext(hero);
	hero.details[1] = "Spell Power 3 · Knowledge 3";
	const auto changed = store.publishNext(hero);

	EXPECT_EQ(unchanged.revision, initial.revision);
	EXPECT_EQ(changed.revision, initial.revision + 1);
}

TEST(ThorContextStoreTest, SelectedAdventureAndActiveWindowPortraitsAreRevisionBound)
{
	ThorContextStore store;
	ThorContextRecord adventure;
	adventure.contextId = ThorContextIds::ADVENTURE_MAP;
	adventure.selectedHeroId = 41;
	adventure.heroPortraitAssetKey = thorHeroPortraitVisualAssetKey(7);
	const auto selected = store.publishNext(adventure);
	EXPECT_EQ(store.publishNext(adventure).revision, selected.revision);

	adventure.selectedHeroId = 42;
	adventure.heroPortraitAssetKey = thorHeroPortraitVisualAssetKey(8);
	const auto changedHero = store.publishNext(adventure);
	EXPECT_EQ(changedHero.revision, selected.revision + 1);
	EXPECT_EQ(changedHero.heroPortraitAssetKey, thorHeroPortraitVisualAssetKey(8));
	adventure.heroPortraitAssetKey = thorHeroPortraitVisualAssetKey(9);
	const auto changedPortraitSource = store.publishNext(adventure);
	EXPECT_EQ(changedPortraitSource.revision, changedHero.revision + 1);
	EXPECT_EQ(changedPortraitSource.heroPortraitAssetKey, thorHeroPortraitVisualAssetKey(9));

	ThorContextRecord heroWindow;
	heroWindow.contextId = ThorContextIds::HERO_WINDOW;
	heroWindow.heroPortraitAssetKey = thorHeroPortraitVisualAssetKey(12);
	const auto activeWindowHero = store.publishNext(heroWindow);
	EXPECT_EQ(activeWindowHero.revision, changedPortraitSource.revision + 1);
	EXPECT_EQ(activeWindowHero.heroPortraitAssetKey, thorHeroPortraitVisualAssetKey(12));
	heroWindow.heroPortraitAssetKey = thorHeroPortraitVisualAssetKey(13);
	const auto changedWindowHero = store.publishNext(heroWindow);
	EXPECT_EQ(changedWindowHero.revision, activeWindowHero.revision + 1);
	EXPECT_EQ(changedWindowHero.heroPortraitAssetKey, thorHeroPortraitVisualAssetKey(13));

	ThorContextRecord unknown;
	unknown.contextId = ThorContextIds::UNKNOWN;
	unknown.heroPortraitAssetKey = thorHeroPortraitVisualAssetKey(12);
	const auto cleared = store.publishNext(unknown);
	EXPECT_GT(cleared.revision, changedWindowHero.revision);
	EXPECT_EQ(cleared.heroPortraitAssetKey, 0);
}

TEST(ThorContextStoreTest, HeroMeetingPortraitKeysRetainOrderedHeroAssociation)
{
	ThorContextStore store;
	ThorContextRecord meeting;
	meeting.contextId = ThorContextIds::HERO_MEETING;
	ThorHeroMeetingArmies armies;
	armies.leftHeroId = 10;
	armies.rightHeroId = 11;
	armies.leftArmyId = 10;
	armies.rightArmyId = 11;
	armies.heroPortraitAssetKeys = {thorHeroPortraitVisualAssetKey(30), thorHeroPortraitVisualAssetKey(31)};
	for(std::size_t slot = 0; slot < THOR_HERO_MEETING_ARMY_SIZE; ++slot)
	{
		armies.leftSlots[slot].armyId = 10;
		armies.leftSlots[slot].slot = static_cast<int>(slot);
		armies.rightSlots[slot].armyId = 11;
		armies.rightSlots[slot].slot = static_cast<int>(slot);
	}
	meeting.heroMeetingArmies = armies;
	const auto published = store.publishNext(meeting);
	ASSERT_TRUE(published.heroMeetingArmies);
	EXPECT_EQ(published.heroMeetingArmies->heroPortraitAssetKeys[0], thorHeroPortraitVisualAssetKey(30));
	EXPECT_EQ(published.heroMeetingArmies->heroPortraitAssetKeys[1], thorHeroPortraitVisualAssetKey(31));

	meeting.heroMeetingArmies->heroPortraitAssetKeys[0] = thorCreatureVisualAssetKey(30);
	const auto sanitized = store.publishNext(meeting);
	ASSERT_TRUE(sanitized.heroMeetingArmies);
	EXPECT_EQ(sanitized.heroMeetingArmies->heroPortraitAssetKeys[0], 0);
	EXPECT_EQ(sanitized.heroMeetingArmies->heroPortraitAssetKeys[1], thorHeroPortraitVisualAssetKey(31));
}

TEST(ThorContextStoreTest, ReplacingHeroSnapshotAndClearingCannotRetainDetails)
{
	ThorContextStore store;
	ThorContextRecord catherine;
	catherine.contextId = ThorContextIds::HERO_WINDOW;
	catherine.title = "Catherine";
	catherine.details[0] = "Attack 4 · Defense 6";
	const auto firstHero = store.publishNext(catherine);

	ThorContextRecord cragHack;
	cragHack.contextId = ThorContextIds::HERO_WINDOW;
	cragHack.title = "Crag Hack";
	cragHack.details[0] = "Attack 8 · Defense 3";
	const auto replacementHero = store.publishNext(cragHack);

	ThorContextRecord unknown;
	unknown.contextId = ThorContextIds::UNKNOWN;
	unknown.details = catherine.details;
	const auto cleared = store.publishNext(unknown);

	EXPECT_EQ(replacementHero.revision, firstHero.revision + 1);
	EXPECT_EQ(replacementHero.title, "Crag Hack");
	EXPECT_EQ(cleared.revision, replacementHero.revision + 1);
	EXPECT_EQ(cleared.contextId, ThorContextIds::UNKNOWN);
	EXPECT_EQ(cleared.details, ThorContextDetails{});
}

TEST(ThorContextStoreTest, TownDetailsChangeRevisionExactlyOnceWithoutChurn)
{
	ThorContextStore store;
	ThorContextRecord town;
	town.contextId = ThorContextIds::TOWN_WINDOW;
	town.title = "Castle Stronghold";
	town.status = "Castle";
	town.details = {"2000", "0 / 1", "Catherine", ""};

	const auto initial = store.publishNext(town);
	const auto unchanged = store.publishNext(town);
	town.details[0] = "3000";
	const auto changedIncome = store.publishNext(town);
	town.details[1] = "1 / 1";
	const auto changedBuildProgress = store.publishNext(town);
	town.details[2] = "Crag Hack";
	const auto changedVisitingHero = store.publishNext(town);
	town.details[3] = "Gelu";
	const auto changedGarrisonHero = store.publishNext(town);

	EXPECT_EQ(unchanged.revision, initial.revision);
	EXPECT_EQ(changedIncome.revision, initial.revision + 1);
	EXPECT_EQ(changedBuildProgress.revision, changedIncome.revision + 1);
	EXPECT_EQ(changedVisitingHero.revision, changedBuildProgress.revision + 1);
	EXPECT_EQ(changedGarrisonHero.revision, changedVisitingHero.revision + 1);
}

TEST(ThorContextStoreTest, ReplacingTownSnapshotAndClearingCannotRetainDetails)
{
	ThorContextStore store;
	ThorContextRecord castle;
	castle.contextId = ThorContextIds::TOWN_WINDOW;
	castle.title = "Castle Stronghold";
	castle.status = "Castle";
	castle.details = {"2000", "0 / 1", "Catherine", ""};
	const auto firstTown = store.publishNext(castle);

	ThorContextRecord dungeon;
	dungeon.contextId = ThorContextIds::TOWN_WINDOW;
	dungeon.title = "Dungeons Deep";
	dungeon.status = "Dungeon";
	dungeon.details = {"1000", "1 / 2", "", "Gunnar"};
	const auto replacementTown = store.publishNext(dungeon);

	ThorContextRecord unknown;
	unknown.contextId = ThorContextIds::UNKNOWN;
	unknown.details = castle.details;
	const auto cleared = store.publishNext(unknown);

	EXPECT_EQ(replacementTown.revision, firstTown.revision + 1);
	EXPECT_EQ(replacementTown.details, dungeon.details);
	EXPECT_EQ(cleared.revision, replacementTown.revision + 1);
	EXPECT_EQ(cleared.details, ThorContextDetails{});
}

TEST(ThorContextStoreTest, NonBattleContextsAndUnknownClearActionSubject)
{
	ThorContextStore store;
	ThorContextRecord battle;
	battle.contextId = ThorContextIds::BATTLE;
	battle.actionSubjectId = 123;
	const auto publishedBattle = store.publishNext(battle);
	EXPECT_EQ(publishedBattle.actionSubjectId, 123);

	ThorContextRecord adventure;
	adventure.contextId = ThorContextIds::ADVENTURE_MAP;
	adventure.actionSubjectId = 456;
	const auto publishedAdventure = store.publishNext(adventure);
	EXPECT_EQ(publishedAdventure.actionSubjectId, -1);

	ThorContextRecord unknown;
	unknown.contextId = ThorContextIds::UNKNOWN;
	unknown.actionSubjectId = 789;
	EXPECT_EQ(store.publishNext(unknown).actionSubjectId, -1);
}

TEST(ThorContextPayloadTest, BoundsTownNamesWithoutSplittingUtf8)
{
	ThorContextStore store;
	ThorContextRecord town;
	town.contextId = ThorContextIds::TOWN_WINDOW;
	town.title = std::string(128, 'a') + "\xc3\xa9";
	town.status = std::string(129, 'b');
	town.details[2] = std::string(128, 'c') + "\xc3\xa9";

	const auto published = store.publishNext(std::move(town));
	EXPECT_EQ(published.title, std::string(128, 'a'));
	EXPECT_EQ(published.status, std::string(128, 'b'));
	EXPECT_EQ(published.details[2], std::string(128, 'c'));
}

TEST(ThorContextStoreTest, AdventureInformationChangesRevisionWithoutChurn)
{
	ThorContextStore store;
	const auto initial = store.publishNext({0, ThorContextIds::ADVENTURE_MAP, "Catherine", "1200 / 1500"});
	const auto unchanged = store.publishNext({0, ThorContextIds::ADVENTURE_MAP, "Catherine", "1200 / 1500"});
	const auto moved = store.publishNext({0, ThorContextIds::ADVENTURE_MAP, "Catherine", "900 / 1500"});

	EXPECT_EQ(unchanged.revision, initial.revision);
	EXPECT_GT(moved.revision, initial.revision);
}

TEST(ThorContextStoreTest, AdventureTownRosterIsBoundedAndChangesSemantically)
{
	ThorContextStore store;
	ThorContextRecord context;
	context.contextId = ThorContextIds::ADVENTURE_MAP;
	context.towns = {{1, "Castle Stronghold", true}, {2, "Dungeon Deep", false}};
	const auto initial = store.publishNext(context);
	EXPECT_EQ(store.publishNext(context).revision, initial.revision);

	context.towns[0].selected = false;
	context.towns[1].selected = true;
	const auto selected = store.publishNext(context);
	std::swap(context.towns[0], context.towns[1]);
	const auto reordered = store.publishNext(context);
	context.towns.pop_back();
	const auto removed = store.publishNext(context);
	EXPECT_EQ(selected.revision, initial.revision + 1);
	EXPECT_EQ(reordered.revision, selected.revision + 1);
	EXPECT_EQ(removed.revision, reordered.revision + 1);

	context.towns.resize(THOR_MAX_TOWNS + 1, {99, "Too many", false});
	const auto bounded = store.publishNext(context);
	EXPECT_TRUE(bounded.towns.empty());
	EXPECT_EQ(thorBoundedText(std::string(128, 'a') + "\xc3\xa9"), std::string(128, 'a'));
}

TEST(ThorContextStoreTest, TownRosterClearsOutsideAdventure)
{
	ThorContextStore store;
	ThorContextRecord adventure;
	adventure.contextId = ThorContextIds::ADVENTURE_MAP;
	adventure.towns.push_back({1, "Castle Stronghold", true});
	EXPECT_EQ(store.publishNext(adventure).towns.size(), 1);

	ThorContextRecord townWindow;
	townWindow.contextId = ThorContextIds::TOWN_WINDOW;
	townWindow.towns = adventure.towns;
	EXPECT_TRUE(store.publishNext(townWindow).towns.empty());
}

TEST(ThorContextStoreTest, WindowSubjectChangeInvalidatesNavigationRevision)
{
	ThorContextStore store;
	ThorContextRecord first;
	first.contextId = ThorContextIds::HERO_WINDOW;
	first.title = "Same translated name";
	first.windowSubjectId = 11;
	first.enabledActionMask = thorActionMask(ThorAction::WINDOW_NEXT);
	const auto old = store.publishNext(first);
	first.windowSubjectId = 12;
	const auto next = store.publishNext(first);
	EXPECT_GT(next.revision, old.revision);
	EXPECT_EQ(next.windowSubjectId, 12);
	first.contextId = ThorContextIds::TOWN_WINDOW;
	EXPECT_GT(store.publishNext(first).revision, next.revision);
	first.contextId = ThorContextIds::UNKNOWN;
	EXPECT_EQ(store.publishNext(first).windowSubjectId, -1);
}
