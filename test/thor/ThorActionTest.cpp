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
	EXPECT_EQ(thorActionFromId(9), std::nullopt);
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
	EXPECT_FALSE(isThorActionAllowedInAdventureMap(static_cast<ThorAction>(99)));
}

TEST(ThorActionTest, GameplayActionsUseExplicitMasks)
{
	EXPECT_EQ(thorActionMask(ThorAction::NEXT_HERO), 16);
	EXPECT_EQ(thorActionMask(ThorAction::MOVE_HERO), 32);
	EXPECT_EQ(thorActionMask(ThorAction::TOGGLE_HERO_SLEEP), 64);
	EXPECT_EQ(thorActionMask(ThorAction::END_TURN), 128);
}

TEST(ThorActionTest, ValidatesRevisionContextAndAvailability)
{
	ThorContextRecord context;
	context.revision = 12;
	context.contextId = ThorContextIds::ADVENTURE_MAP;
	context.enabledActionMask = thorActionMask(ThorAction::OPEN_SAVE_GAME);

	EXPECT_EQ(validateThorActionRequest({12, ThorAction::OPEN_SAVE_GAME}, context), ThorActionValidation::VALID);
	EXPECT_EQ(validateThorActionRequest({11, ThorAction::OPEN_SAVE_GAME}, context), ThorActionValidation::STALE_REVISION);
	EXPECT_EQ(validateThorActionRequest({12, ThorAction::OPEN_PUZZLE_MAP}, context), ThorActionValidation::UNAVAILABLE);
	context.contextId = ThorContextIds::HERO_WINDOW;
	EXPECT_EQ(validateThorActionRequest({12, ThorAction::OPEN_SAVE_GAME}, context), ThorActionValidation::WRONG_CONTEXT);
	EXPECT_EQ(validateThorActionRequest({12, static_cast<ThorAction>(99)}, context), ThorActionValidation::UNKNOWN_ACTION);
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
