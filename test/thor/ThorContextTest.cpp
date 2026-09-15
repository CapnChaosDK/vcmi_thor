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
