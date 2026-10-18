/*
 * BattleAnimationWaitTest.cpp, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 *
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 *
 */
#include <gtest/gtest.h>

#include "../../client/battle/BattleAnimationWait.h"

#include <deque>
#include <functional>
#include <stdexcept>
#include <string>
#include <utility>
#include <vector>

namespace
{
	struct AnimationWaitHarness
	{
		bool busy = false;
		bool interfaceLocked = true;
		int waits = 0;
		std::deque<std::function<void()>> stages;
		std::function<void()> onCompletion;
		std::vector<std::string> calls;

		void run()
		{
			waitForBattleAnimations(
				[this]()
				{
					EXPECT_TRUE(interfaceLocked);
					interfaceLocked = false;
					++waits;
					calls.push_back("wait");
					busy = false;
					if(onCompletion)
						onCompletion();
					interfaceLocked = true;
				},
				[this]() { EXPECT_TRUE(interfaceLocked); return busy; },
				[this]() { EXPECT_TRUE(interfaceLocked); return !stages.empty(); },
				[this]()
				{
					EXPECT_TRUE(interfaceLocked);
					EXPECT_FALSE(busy);
					auto action = std::move(stages.front());
					stages.pop_front();
					calls.push_back("stage");
					action();
				});
			EXPECT_TRUE(interfaceLocked);
			EXPECT_FALSE(busy);
			EXPECT_TRUE(stages.empty());
		}
	};
}

TEST(BattleAnimationWait, WaitsForRunningAnimationWithNoPendingStages)
{
	AnimationWaitHarness harness;
	harness.busy = true;
	harness.run();
	EXPECT_EQ(harness.waits, 1);
}

TEST(BattleAnimationWait, DrainsSynchronousStagesWithoutWaitingAgain)
{
	AnimationWaitHarness harness;
	harness.stages.push_back([]() {});
	harness.stages.push_back([]() {});
	harness.run();
	EXPECT_EQ(harness.calls, (std::vector<std::string>{"wait", "stage", "stage"}));
}

TEST(BattleAnimationWait, WaitsForEachAnimationBeforeDispatchingNextStage)
{
	AnimationWaitHarness harness;
	harness.stages.push_back([&]() { harness.busy = true; });
	harness.stages.push_back([&]() { harness.busy = true; });
	harness.run();
	EXPECT_EQ(harness.calls, (std::vector<std::string>{"wait", "stage", "wait", "stage", "wait"}));
}

TEST(BattleAnimationWait, DrainsStagesAddedBySynchronousStage)
{
	AnimationWaitHarness harness;
	int reactions = 0;
	harness.stages.push_back([&]()
	{
		harness.stages.push_back([&]() { ++reactions; });
	});
	harness.run();
	EXPECT_EQ(reactions, 1);
	EXPECT_EQ(harness.waits, 1);
}

TEST(BattleAnimationWait, DrainsReactionsQueuedOnAnimationCompletion)
{
	AnimationWaitHarness harness;
	harness.busy = true;
	harness.onCompletion = [&]()
	{
		EXPECT_FALSE(harness.interfaceLocked);
		if(harness.waits <= 2)
			harness.stages.push_back([&]() { harness.busy = true; });
	};
	harness.run();
	EXPECT_EQ(harness.waits, 3);
}

TEST(BattleAnimationWait, PropagatesTerminationWithoutDispatchingAnotherStage)
{
	int dispatched = 0;
	int waits = 0;
	EXPECT_THROW(waitForBattleAnimations(
		[&]()
		{
			if(++waits == 2)
				throw std::runtime_error("terminated");
		},
		[]() { return true; },
		[]() { return true; },
		[&]() { ++dispatched; }), std::runtime_error);
	EXPECT_EQ(dispatched, 1);
}
