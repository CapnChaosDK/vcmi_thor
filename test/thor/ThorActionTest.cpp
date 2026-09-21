#include "StdInc.h"

#include "../../lib/thor/ThorAction.h"

#include <thread>

TEST(ThorActionTest, MapsOnlyStablePublicIdentifiers)
{
	EXPECT_EQ(thorActionFromId(0), std::nullopt);
	EXPECT_EQ(thorActionFromId(1), ThorAction::OPEN_KINGDOM_OVERVIEW);
	EXPECT_EQ(thorActionFromId(2), ThorAction::OPEN_QUEST_LOG);
	EXPECT_EQ(thorActionFromId(3), ThorAction::OPEN_PUZZLE_MAP);
	EXPECT_EQ(thorActionFromId(4), ThorAction::OPEN_SAVE_GAME);
	EXPECT_EQ(thorActionFromId(5), ThorAction::NEXT_HERO);
	EXPECT_EQ(thorActionFromId(6), ThorAction::MOVE_HERO);
	EXPECT_EQ(thorActionFromId(7), ThorAction::TOGGLE_HERO_SLEEP);
	EXPECT_EQ(thorActionFromId(8), ThorAction::END_TURN);
	EXPECT_EQ(thorActionFromId(9), ThorAction::BATTLE_WAIT);
	EXPECT_EQ(thorActionFromId(10), ThorAction::BATTLE_DEFEND);
	EXPECT_EQ(thorActionFromId(11), ThorAction::BATTLE_TACTICS_NEXT);
	EXPECT_EQ(thorActionFromId(12), ThorAction::BATTLE_TACTICS_END);
	EXPECT_EQ(thorActionFromId(13), ThorAction::SELECT_HERO);
	EXPECT_EQ(thorActionFromId(14), ThorAction::SELECT_TOWN);
	EXPECT_EQ(thorActionFromId(15), ThorAction::HERO_MEETING_TRANSFER_STACK);
	EXPECT_EQ(thorActionFromId(16), ThorAction::HERO_MEETING_ARMY_LEFT_TO_RIGHT);
	EXPECT_EQ(thorActionFromId(17), ThorAction::HERO_MEETING_ARMY_RIGHT_TO_LEFT);
	EXPECT_EQ(thorActionFromId(18), ThorAction::HERO_MEETING_SWAP_ARMIES);
	EXPECT_EQ(thorActionFromId(19), std::nullopt);
	EXPECT_EQ(thorActionFromId(-1), std::nullopt);
}

TEST(ThorActionTest, AllowsOnlySliceTwelveAdventureActions)
{
	EXPECT_FALSE(isThorActionAllowedInAdventureMap(ThorAction::NONE));
	EXPECT_TRUE(isThorActionAllowedInAdventureMap(ThorAction::OPEN_KINGDOM_OVERVIEW));
	EXPECT_TRUE(isThorActionAllowedInAdventureMap(ThorAction::OPEN_QUEST_LOG));
	EXPECT_TRUE(isThorActionAllowedInAdventureMap(ThorAction::OPEN_PUZZLE_MAP));
	EXPECT_TRUE(isThorActionAllowedInAdventureMap(ThorAction::OPEN_SAVE_GAME));
	EXPECT_TRUE(isThorActionAllowedInAdventureMap(ThorAction::NEXT_HERO));
	EXPECT_TRUE(isThorActionAllowedInAdventureMap(ThorAction::MOVE_HERO));
	EXPECT_TRUE(isThorActionAllowedInAdventureMap(ThorAction::TOGGLE_HERO_SLEEP));
	EXPECT_TRUE(isThorActionAllowedInAdventureMap(ThorAction::END_TURN));
	EXPECT_TRUE(isThorActionAllowedInAdventureMap(ThorAction::SELECT_HERO));
	EXPECT_TRUE(isThorActionAllowedInAdventureMap(ThorAction::SELECT_TOWN));
	EXPECT_FALSE(isThorActionAllowedInAdventureMap(static_cast<ThorAction>(99)));
}

TEST(ThorActionTest, AllowsOnlyActionsForTheirExactContext)
{
	EXPECT_TRUE(isThorActionAllowedInContext(ThorAction::OPEN_SAVE_GAME, ThorContextIds::ADVENTURE_MAP));
	EXPECT_FALSE(isThorActionAllowedInContext(ThorAction::BATTLE_WAIT, ThorContextIds::ADVENTURE_MAP));
	EXPECT_TRUE(isThorActionAllowedInContext(ThorAction::BATTLE_WAIT, ThorContextIds::BATTLE));
	EXPECT_TRUE(isThorActionAllowedInContext(ThorAction::BATTLE_DEFEND, ThorContextIds::BATTLE));
	EXPECT_FALSE(isThorActionAllowedInContext(ThorAction::BATTLE_TACTICS_NEXT, ThorContextIds::BATTLE));
	EXPECT_TRUE(isThorActionAllowedInContext(ThorAction::BATTLE_TACTICS_NEXT, ThorContextIds::BATTLE_TACTICS));
	EXPECT_TRUE(isThorActionAllowedInContext(ThorAction::BATTLE_TACTICS_END, ThorContextIds::BATTLE_TACTICS));
	EXPECT_FALSE(isThorActionAllowedInContext(ThorAction::BATTLE_WAIT, ThorContextIds::BATTLE_RESULT));
	EXPECT_FALSE(isThorActionAllowedInContext(ThorAction::BATTLE_DEFEND, ThorContextIds::HERO_WINDOW));
}

TEST(ThorActionTest, GameplayActionsUseExplicitMasks)
{
	EXPECT_EQ(thorActionMask(ThorAction::NEXT_HERO), 16);
	EXPECT_EQ(thorActionMask(ThorAction::MOVE_HERO), 32);
	EXPECT_EQ(thorActionMask(ThorAction::TOGGLE_HERO_SLEEP), 64);
	EXPECT_EQ(thorActionMask(ThorAction::END_TURN), 128);
	EXPECT_EQ(thorActionMask(ThorAction::BATTLE_WAIT), 256);
	EXPECT_EQ(thorActionMask(ThorAction::BATTLE_DEFEND), 512);
	EXPECT_EQ(thorActionMask(ThorAction::BATTLE_TACTICS_NEXT), 1024);
	EXPECT_EQ(thorActionMask(ThorAction::BATTLE_TACTICS_END), 2048);
	EXPECT_EQ(thorActionMask(ThorAction::SELECT_HERO), 4096);
	EXPECT_EQ(thorActionMask(ThorAction::SELECT_TOWN), 8192);
	EXPECT_EQ(thorActionMask(ThorAction::HERO_MEETING_TRANSFER_STACK), 16384);
	EXPECT_EQ(thorActionMask(ThorAction::HERO_MEETING_ARMY_LEFT_TO_RIGHT), 32768);
	EXPECT_EQ(thorActionMask(ThorAction::HERO_MEETING_ARMY_RIGHT_TO_LEFT), 65536);
	EXPECT_EQ(thorActionMask(ThorAction::HERO_MEETING_SWAP_ARMIES), 131072);
}

TEST(ThorActionTest, HeroMeetingTransferRequiresPublishedEndpoints)
{
	ThorContextRecord context;
	context.revision = 20;
	context.contextId = ThorContextIds::HERO_MEETING;
	context.enabledActionMask = thorActionMask(ThorAction::HERO_MEETING_TRANSFER_STACK);
	ThorHeroMeetingArmies armies;
	armies.leftHeroId = 1;
	armies.rightHeroId = 2;
	armies.leftArmyId = 1;
	armies.rightArmyId = 2;
	armies.locallyControllable = true;
	for(std::size_t index = 0; index < THOR_HERO_MEETING_ARMY_SIZE; ++index)
	{
		armies.leftSlots[index].armyId = 1;
		armies.leftSlots[index].slot = static_cast<int>(index);
		armies.rightSlots[index].armyId = 2;
		armies.rightSlots[index].slot = static_cast<int>(index);
	}
	armies.leftSlots[0] = {1, 0, true, 3, "Pikemen", 12};
	context.heroMeetingArmies = armies;
	ThorActionRequest request;
	request.revision = 20;
	request.action = ThorAction::HERO_MEETING_TRANSFER_STACK;
	request.sourceArmyId = 1;
	request.sourceSlot = 0;
	request.destinationArmyId = 2;
	request.destinationSlot = 6;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::VALID);
	request.sourceSlot = 7;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::INVALID_TARGET);
	request.sourceSlot = 0;
	request.destinationArmyId = 9;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::INVALID_TARGET);
	request.destinationArmyId = 2;
	request.destinationSlot = 7;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::INVALID_TARGET);
	request.destinationSlot = 6;
	request.sourceArmyId = 2;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::INVALID_TARGET);
	context.enabledActionMask = 0;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::UNAVAILABLE);
	context.enabledActionMask = thorActionMask(ThorAction::HERO_MEETING_TRANSFER_STACK);
	context.contextId = ThorContextIds::ADVENTURE_MAP;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::WRONG_CONTEXT);
}

TEST(ThorActionTest, TownTargetMustMatchPublishedRosterAndRevision)
{
	ThorContextRecord context;
	context.revision = 8;
	context.contextId = ThorContextIds::ADVENTURE_MAP;
	context.enabledActionMask = thorActionMask(ThorAction::SELECT_TOWN);
	context.towns.push_back({42, "Castle Stronghold", false});
	EXPECT_EQ(validateThorActionRequest({8, ThorAction::SELECT_TOWN, 42}, context), ThorActionValidation::VALID);
	EXPECT_EQ(validateThorActionRequest({8, ThorAction::SELECT_TOWN}, context), ThorActionValidation::INVALID_TARGET);
	EXPECT_EQ(validateThorActionRequest({8, ThorAction::SELECT_TOWN, 43}, context), ThorActionValidation::INVALID_TARGET);
	EXPECT_EQ(validateThorActionRequest({7, ThorAction::SELECT_TOWN, 42}, context), ThorActionValidation::STALE_REVISION);
	context.contextId = ThorContextIds::TOWN_WINDOW;
	EXPECT_EQ(validateThorActionRequest({8, ThorAction::SELECT_TOWN, 42}, context), ThorActionValidation::WRONG_CONTEXT);
	context.contextId = ThorContextIds::ADVENTURE_MAP;
	context.enabledActionMask = 0;
	EXPECT_EQ(validateThorActionRequest({8, ThorAction::SELECT_TOWN, 42}, context), ThorActionValidation::UNAVAILABLE);
	context.enabledActionMask = thorActionMask(ThorAction::SELECT_TOWN);
	context.towns.clear();
	EXPECT_EQ(validateThorActionRequest({8, ThorAction::SELECT_TOWN, 42}, context), ThorActionValidation::INVALID_TARGET);
}

TEST(ThorActionTest, TargetMustMatchPublishedHeroAndRevision)
{
	ThorContextRecord context;
	context.revision = 7;
	context.contextId = ThorContextIds::ADVENTURE_MAP;
	context.enabledActionMask = thorActionMask(ThorAction::SELECT_HERO);
	context.heroes.push_back({42, "Hero", 100, 200, false, false});
	EXPECT_EQ(validateThorActionRequest({7, ThorAction::SELECT_HERO, 42}, context), ThorActionValidation::VALID);
	EXPECT_EQ(validateThorActionRequest({7, ThorAction::SELECT_HERO}, context), ThorActionValidation::INVALID_TARGET);
	EXPECT_EQ(validateThorActionRequest({7, ThorAction::SELECT_HERO, 43}, context), ThorActionValidation::INVALID_TARGET);
	EXPECT_EQ(validateThorActionRequest({6, ThorAction::SELECT_HERO, 42}, context), ThorActionValidation::STALE_REVISION);
	context.contextId = ThorContextIds::HERO_WINDOW;
	EXPECT_EQ(validateThorActionRequest({7, ThorAction::SELECT_HERO, 42}, context), ThorActionValidation::WRONG_CONTEXT);
	EXPECT_EQ(validateThorActionRequest({7, ThorAction::OPEN_SAVE_GAME, 42}, context), ThorActionValidation::WRONG_CONTEXT);
	context.contextId = ThorContextIds::ADVENTURE_MAP;
	context.heroes.clear(); // ownership/removal refresh invalidates the published target
	EXPECT_EQ(validateThorActionRequest({7, ThorAction::SELECT_HERO, 42}, context), ThorActionValidation::INVALID_TARGET);
}

TEST(ThorActionTest, ValidatesRevisionContextAndAvailability)
{
	ThorContextRecord context;
	context.revision = 12;
	context.contextId = ThorContextIds::ADVENTURE_MAP;
	context.enabledActionMask = thorActionMask(ThorAction::OPEN_SAVE_GAME);

	EXPECT_EQ(validateThorActionRequest({12, ThorAction::OPEN_SAVE_GAME}, context), ThorActionValidation::VALID);
	EXPECT_EQ(validateThorActionRequest({12, ThorAction::OPEN_SAVE_GAME, 999}, context), ThorActionValidation::VALID);
	EXPECT_EQ(validateThorActionRequest({11, ThorAction::OPEN_SAVE_GAME}, context), ThorActionValidation::STALE_REVISION);
	EXPECT_EQ(validateThorActionRequest({12, ThorAction::OPEN_PUZZLE_MAP}, context), ThorActionValidation::UNAVAILABLE);
	context.contextId = ThorContextIds::HERO_WINDOW;
	EXPECT_EQ(validateThorActionRequest({12, ThorAction::OPEN_SAVE_GAME}, context), ThorActionValidation::WRONG_CONTEXT);
	EXPECT_EQ(validateThorActionRequest({12, static_cast<ThorAction>(99)}, context), ThorActionValidation::UNKNOWN_ACTION);
}

TEST(ThorActionTest, ValidatesBattleContextsSeparately)
{
	ThorContextRecord context;
	context.revision = 42;
	context.contextId = ThorContextIds::BATTLE;
	context.enabledActionMask = thorActionMask(ThorAction::BATTLE_WAIT);

	EXPECT_EQ(validateThorActionRequest({42, ThorAction::BATTLE_WAIT}, context), ThorActionValidation::VALID);
	EXPECT_EQ(validateThorActionRequest({42, ThorAction::BATTLE_DEFEND}, context), ThorActionValidation::UNAVAILABLE);
	EXPECT_EQ(validateThorActionRequest({42, ThorAction::OPEN_SAVE_GAME}, context), ThorActionValidation::WRONG_CONTEXT);
	context.actionSubjectId = 101; // read-only opponent information does not enable commands
	context.enabledActionMask = 0;
	EXPECT_EQ(validateThorActionRequest({42, ThorAction::BATTLE_WAIT}, context), ThorActionValidation::UNAVAILABLE);

	context.contextId = ThorContextIds::BATTLE_TACTICS;
	context.enabledActionMask = thorActionMask(ThorAction::BATTLE_TACTICS_END);
	EXPECT_EQ(validateThorActionRequest({42, ThorAction::BATTLE_TACTICS_END}, context), ThorActionValidation::VALID);
	EXPECT_EQ(validateThorActionRequest({42, ThorAction::BATTLE_WAIT}, context), ThorActionValidation::WRONG_CONTEXT);

	context.contextId = ThorContextIds::BATTLE_RESULT;
	context.enabledActionMask = thorActionMask(ThorAction::BATTLE_WAIT);
	EXPECT_EQ(validateThorActionRequest({42, ThorAction::BATTLE_WAIT}, context), ThorActionValidation::WRONG_CONTEXT);
}

TEST(ThorActionTest, ActiveActionStateCreatesOneNewRevision)
{
	ThorContextStore store;
	ThorContextRecord context;
	context.contextId = ThorContextIds::ADVENTURE_MAP;
	context.enabledActionMask = thorActionMask(ThorAction::TOGGLE_HERO_SLEEP);
	const auto awake = store.publishNext(context);
	EXPECT_EQ(awake.revision, 1);
	EXPECT_EQ(store.publishNext(context).revision, awake.revision);

	context.activeActionMask = thorActionMask(ThorAction::TOGGLE_HERO_SLEEP);
	const auto asleep = store.publishNext(context);
	EXPECT_EQ(asleep.revision, awake.revision + 1);
	EXPECT_EQ(store.publishNext(context).revision, asleep.revision);
}

TEST(ThorActionTest, ChangedSelectedHeroRejectsStaleAction)
{
	ThorContextStore store;
	ThorContextRecord context;
	context.contextId = ThorContextIds::ADVENTURE_MAP;
	context.enabledActionMask = thorActionMask(ThorAction::MOVE_HERO);
	context.selectedHeroId = 4;
	const auto firstHero = store.publishNext(context);

	context.selectedHeroId = 9;
	const auto secondHero = store.publishNext(context);
	EXPECT_EQ(secondHero.revision, firstHero.revision + 1);
	EXPECT_EQ(validateThorActionRequest({firstHero.revision, ThorAction::MOVE_HERO}, secondHero), ThorActionValidation::STALE_REVISION);
}

TEST(ThorActionTest, ConsumedActionInvalidatesItsRenderedRevision)
{
	ThorContextStore store;
	ThorContextRecord context;
	context.contextId = ThorContextIds::ADVENTURE_MAP;
	context.enabledActionMask = thorActionMask(ThorAction::MOVE_HERO);
	const auto rendered = store.publishNext(context);

	++context.actionEpoch;
	const auto consumed = store.publishNext(context);
	EXPECT_EQ(consumed.revision, rendered.revision + 1);
	EXPECT_EQ(validateThorActionRequest({rendered.revision, ThorAction::MOVE_HERO}, consumed), ThorActionValidation::STALE_REVISION);
}

TEST(ThorActionTest, ConsumedTownActionInvalidatesItsRenderedRevision)
{
	ThorContextStore store;
	ThorContextRecord context;
	context.contextId = ThorContextIds::ADVENTURE_MAP;
	context.enabledActionMask = thorActionMask(ThorAction::SELECT_TOWN);
	context.towns.push_back({4, "Castle Stronghold", true});
	const auto rendered = store.publishNext(context);

	++context.actionEpoch;
	const auto consumed = store.publishNext(context);
	EXPECT_EQ(consumed.revision, rendered.revision + 1);
	EXPECT_EQ(validateThorActionRequest({rendered.revision, ThorAction::SELECT_TOWN, 4}, consumed),
		ThorActionValidation::STALE_REVISION);
}

TEST(ThorActionTest, BattleActionSubjectChangesRevisionWithoutMaskChurn)
{
	ThorContextStore store;
	ThorContextRecord context;
	context.contextId = ThorContextIds::BATTLE;
	context.enabledActionMask = thorActionMask(ThorAction::BATTLE_WAIT) | thorActionMask(ThorAction::BATTLE_DEFEND);
	context.actionSubjectId = 101;
	const auto firstStack = store.publishNext(context);
	EXPECT_EQ(store.publishNext(context).revision, firstStack.revision);

	context.actionSubjectId = 202;
	const auto secondStack = store.publishNext(context);
	EXPECT_EQ(secondStack.revision, firstStack.revision + 1);
	EXPECT_EQ(validateThorActionRequest({firstStack.revision, ThorAction::BATTLE_WAIT}, secondStack), ThorActionValidation::STALE_REVISION);
}

TEST(ThorActionTest, ConsumingBattleActionClearsRenderedAvailability)
{
	ThorContextStore store;
	ThorContextRecord context;
	context.contextId = ThorContextIds::BATTLE;
	context.enabledActionMask = thorActionMask(ThorAction::BATTLE_DEFEND);
	context.actionSubjectId = 7;
	const auto rendered = store.publishNext(context);

	++context.actionEpoch;
	context.enabledActionMask = 0;
	const auto consumed = store.publishNext(context);
	EXPECT_GT(consumed.revision, rendered.revision);
	EXPECT_EQ(consumed.enabledActionMask, 0);
	EXPECT_EQ(validateThorActionRequest({rendered.revision, ThorAction::BATTLE_DEFEND}, consumed), ThorActionValidation::STALE_REVISION);
}

TEST(ThorActionQueueTest, KeepsRequestsBoundedAndOrdered)
{
	ThorActionQueue queue;
	for(std::uint64_t revision = 1; revision <= 16; ++revision)
		EXPECT_TRUE(queue.submit({revision, ThorAction::OPEN_SAVE_GAME}));

	EXPECT_FALSE(queue.submit({17, ThorAction::OPEN_SAVE_GAME}));
	EXPECT_EQ(queue.size(), 16);

	for(std::uint64_t revision = 1; revision <= 16; ++revision)
	{
		const auto request = queue.pop();
		ASSERT_TRUE(request.has_value());
		EXPECT_EQ(request->revision, revision);
	}
	EXPECT_FALSE(queue.pop().has_value());
}

TEST(ThorActionQueueTest, ConcurrentSubmissionsRemainBounded)
{
	ThorActionQueue queue;
	auto submit = [&queue](std::uint64_t firstRevision)
	{
		for(std::uint64_t revision = firstRevision; revision < firstRevision + 32; ++revision)
			queue.submit({revision, ThorAction::OPEN_SAVE_GAME});
	};

	std::thread first(submit, 1);
	std::thread second(submit, 100);
	first.join();
	second.join();

	EXPECT_EQ(queue.size(), 16);
	queue.clear();
	EXPECT_EQ(queue.size(), 0);
}
