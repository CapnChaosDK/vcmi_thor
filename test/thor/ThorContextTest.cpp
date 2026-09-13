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
	EXPECT_EQ(store.snapshot().contextId, "UNKNOWN");
}
