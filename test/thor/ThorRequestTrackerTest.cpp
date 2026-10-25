/*
 * ThorRequestTrackerTest.cpp, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 * License: GNU General Public License v2.0 or later
 */
#include "../../client/thor/ThorRequestTracker.h"
#include <gtest/gtest.h>
#include <stdexcept>
#include <thread>

TEST(ThorRequestTrackerTest, MissingAndSubmittedRequests)
{
	EXPECT_EQ(captureThorRequest([] {}), -1);
	EXPECT_EQ(captureThorRequest([] { ThorRequests::submitted(73); }), 73);
	EXPECT_EQ(ThorRequests::capturedRequest, nullptr);
}

TEST(ThorRequestTrackerTest, NestedAndFailedCallbacksRestoreTheOuterCapture)
{
	const auto outer = captureThorRequest([]
	{
		EXPECT_EQ(captureThorRequest([] { ThorRequests::submitted(3); }), 3);
		EXPECT_THROW(captureThorRequest([] { throw std::runtime_error("submission failed"); }), std::runtime_error);
		ThorRequests::submitted(7);
	});
	EXPECT_EQ(outer, 7);
	EXPECT_EQ(ThorRequests::capturedRequest, nullptr);
}

TEST(ThorRequestTrackerTest, OtherThreadsCannotCompleteTheCurrentRequest)
{
	EXPECT_EQ(captureThorRequest([]
	{
		std::thread unrelated([] { ThorRequests::submitted(99); });
		unrelated.join();
	}), -1);
}
