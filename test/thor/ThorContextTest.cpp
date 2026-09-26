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
	context.heroMeetingArmies->leftSlots[0].count = 16;
	const auto changedCount = store.publishNext(context);
	EXPECT_GT(changedCount.revision, first.revision);
	context.heroMeetingArmies->leftSlots[0] = {11, 0, false, -1, "", 0};
	context.heroMeetingArmies->rightSlots[1] = {12, 1, true, 4, "Pikemen", 16};
	const auto movedStack = store.publishNext(context);
	EXPECT_GT(movedStack.revision, changedCount.revision);
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
	artifacts.artifactSlots[1].name = std::string(128, 'A') + "x";
	context.heroMeetingArtifacts = artifacts;
	const auto first = store.publishNext(context);
	ASSERT_TRUE(first.heroMeetingArtifacts);
	EXPECT_EQ(first.heroMeetingArtifacts->artifactSlots.size(), THOR_HERO_MEETING_ARTIFACT_COUNT);
	EXPECT_EQ(first.heroMeetingArtifacts->artifactSlots[1].name.size(), 128);
	EXPECT_EQ(store.publishNext(context).revision, first.revision);
	context.heroMeetingArtifacts->artifactSlots[1].instanceId = 43;
	const auto replaced = store.publishNext(context);
	EXPECT_EQ(replaced.revision, first.revision + 1);
	context.heroMeetingArtifacts->artifactSlots[1].name = "Changed";
	EXPECT_EQ(store.publishNext(context).revision, replaced.revision + 1);
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
