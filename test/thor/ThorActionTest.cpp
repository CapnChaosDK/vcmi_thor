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
	EXPECT_EQ(thorActionFromId(5), std::nullopt);
	EXPECT_EQ(thorActionFromId(-1), std::nullopt);
}

TEST(ThorActionTest, AllowsOnlySliceElevenAdventureActions)
{
	EXPECT_FALSE(isThorActionAllowedInAdventureMap(ThorAction::NONE));
	EXPECT_TRUE(isThorActionAllowedInAdventureMap(ThorAction::OPEN_KINGDOM_OVERVIEW));
	EXPECT_TRUE(isThorActionAllowedInAdventureMap(ThorAction::OPEN_QUEST_LOG));
	EXPECT_TRUE(isThorActionAllowedInAdventureMap(ThorAction::OPEN_PUZZLE_MAP));
	EXPECT_TRUE(isThorActionAllowedInAdventureMap(ThorAction::OPEN_SAVE_GAME));
	EXPECT_FALSE(isThorActionAllowedInAdventureMap(static_cast<ThorAction>(99)));
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
