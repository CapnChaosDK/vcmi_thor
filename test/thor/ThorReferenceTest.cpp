#include "StdInc.h"
#include "../../lib/thor/ThorAction.h"
#include "../../lib/thor/ThorDialog.h"
#include "../../client/thor/ThorPuzzleSnapshot.h"
#include "../../lib/Color.h"

namespace
{
	ThorContextRecord journal()
	{
		ThorContextRecord c;
		c.contextId = ThorContextIds::QUEST_LOG;
		c.nativeOwnerToken = reinterpret_cast<void *>(1);
		c.nativeParentToken = reinterpret_cast<void *>(2);
		ThorReference p;
		p.kind = 1; p.pages = 2; p.selected = 0; p.flags = 15;
		p.targets = {0, 1, 2, 3, 4}; p.labels = {"A", "B", "C", "D", "E"};
		p.text = "Native description"; p.textPages = 3;
		c.reference = p;
		return c;
	}
	ThorActionRequest request(std::uint64_t revision, int command, int target = -1)
	{
		ThorActionRequest r;
		r.revision = revision; r.commandVersion = 1; r.commandId = command; r.targetId = target;
		return r;
	}
}

TEST(ThorReferenceTest, NativeJournalAuthorityAndOffPageTargets)
{
	ThorContextStore store;
	const auto c = store.publishNext(journal());
	EXPECT_EQ(validateThorActionRequest(request(c.revision, ThorExtended::REFERENCE_SELECT, 4), c), ThorActionValidation::VALID);
	EXPECT_EQ(validateThorActionRequest(request(c.revision, ThorExtended::REFERENCE_SELECT, 5), c), ThorActionValidation::UNAVAILABLE);
	EXPECT_EQ(validateThorActionRequest(request(c.revision, ThorExtended::REFERENCE_PREVIOUS), c), ThorActionValidation::UNAVAILABLE);
	EXPECT_EQ(validateThorActionRequest(request(c.revision, ThorExtended::REFERENCE_NEXT), c), ThorActionValidation::VALID);
	EXPECT_EQ(validateThorActionRequest(request(c.revision, ThorExtended::REFERENCE_TEXT_NEXT), c), ThorActionValidation::VALID);
	EXPECT_EQ(validateThorActionRequest(request(c.revision, ThorExtended::REFERENCE_QUESTS), c), ThorActionValidation::UNAVAILABLE);
	EXPECT_EQ(validateThorActionRequest(request(c.revision, ThorExtended::REFERENCE_EVENTS), c), ThorActionValidation::VALID);
}

TEST(ThorReferenceTest, SelectionTextParentEpochAndOwnerInvalidateOldReleases)
{
	ThorContextStore store;
	const auto first = store.publishNext(journal());
	const auto r = request(first.revision, ThorExtended::REFERENCE_CENTER);
	EXPECT_EQ(validateThorActionRequest(r, first), ThorActionValidation::VALID);
	for(int field = 0; field < 6; ++field)
	{
		auto changed = first;
		if(field == 0) changed.reference->selected = 2;
		if(field == 1) changed.reference->text = "Changed native requirements";
		if(field == 2) changed.nativeParentToken = reinterpret_cast<void *>(3);
		if(field == 3) changed.nativeOwnerToken = reinterpret_cast<void *>(4);
		if(field == 4) ++changed.actionEpoch;
		if(field == 5) changed.reference->labels[0] = "Replaced row";
		EXPECT_EQ(validateThorActionRequest(r, store.publishNext(changed)), ThorActionValidation::STALE_REVISION);
	}
}

TEST(ThorReferenceTest, BoundsAndForeignContextFailClosed)
{
	ThorContextStore store;
	auto c = journal(); c.reference->targets[0] = 9;
	EXPECT_FALSE(store.publishNext(c).reference);
	c = journal(); c.reference->text.assign(385, 'x');
	EXPECT_FALSE(store.publishNext(c).reference);
	c = journal(); c.contextId = ThorContextIds::ADVENTURE_MAP;
	EXPECT_FALSE(store.publishNext(c).reference);
	c = journal(); c.reference->flags &= ~4;
	const auto noLocation = store.publishNext(c);
	EXPECT_EQ(validateThorActionRequest(request(noLocation.revision, ThorExtended::REFERENCE_CENTER), noLocation), ThorActionValidation::UNAVAILABLE);
}

TEST(ThorReferenceTest, PuzzleOnlyPublishesExactBoundedFinalImageAndClose)
{
	ThorReference p; p.kind = 3; p.progress = 50;
	EXPECT_TRUE(p.valid());
	EXPECT_TRUE(p.allows(ThorExtended::REFERENCE_CLOSE, -1));
	EXPECT_FALSE(p.allows(ThorExtended::REFERENCE_CENTER, -1));
	p.width = ThorReference::WIDTH; p.height = ThorReference::HEIGHT;
	p.rgb = std::make_shared<const std::vector<std::uint8_t>>(p.width * p.height * 3, 40);
	EXPECT_TRUE(p.valid());
	++p.width; EXPECT_FALSE(p.valid());
	p.width = ThorReference::WIDTH; p.flags |= 4; EXPECT_FALSE(p.valid());
}

TEST(ThorReferenceTest, FullUnicodeDescriptionIsPagedWithoutTruncation)
{
	std::string text;
	for(int i = 0; i < 100; ++i) text += "Quest 😀 requirements\n";
	std::string rebuilt;
	for(const auto & page : thorDialogPages(text)) for(const auto & chunk : page) rebuilt += chunk;
	EXPECT_EQ(rebuilt, text);
	EXPECT_TRUE(thorDialogPages(std::string(8193, 'x')).empty());
}

TEST(ThorReferenceTest, CaptureCannotReadBeforeRevealAndAllCovers)
{
	int stage = 0;
	auto drawMap = [&]() { EXPECT_EQ(stage, 0); stage = 1; };
	auto drawCover = [&](std::size_t i) { EXPECT_EQ(stage, 1 + i); ++stage; };
	auto readPixel = [&](int, int) { EXPECT_EQ(stage, 4); return ColorRGBA(12, 34, 56); };
	EXPECT_FALSE(thorPuzzleSnapshot(false, 3, drawMap, drawCover, readPixel));
	EXPECT_EQ(stage, 0);
	const auto rgb = thorPuzzleSnapshot(true, 3, drawMap, drawCover, readPixel);
	ASSERT_TRUE(rgb);
	EXPECT_EQ(rgb->size(), ThorReference::WIDTH * ThorReference::HEIGHT * 3);
	EXPECT_EQ((*rgb)[0], 12); EXPECT_EQ((*rgb)[1], 34); EXPECT_EQ((*rgb)[2], 56);
}
