#include "StdInc.h"

#include "../../client/thor/ThorWindowPublisher.h"
#include "../../lib/thor/ThorAction.h"

namespace
{
	struct RecordingBridge
	{
		std::vector<std::string> calls;
		std::vector<std::uint64_t> revisions;
		ThorContextRecord sent;

		void record(const std::string & name, std::uint64_t revision)
		{
			calls.push_back(name);
			revisions.push_back(revision);
		}
		void publishThorContext(std::uint64_t revision, const std::string & id, const std::string & title,
			const std::string & status, const ThorContextDetails & details, std::uint64_t portrait)
		{
			record("context", revision);
			sent.contextId = id;
			sent.title = title;
			sent.status = status;
			sent.details = details;
			sent.heroPortraitAssetKey = portrait;
		}
		void publishThorActionState(std::uint64_t revision, std::uint64_t enabled, std::uint64_t active)
		{
			record("actions", revision);
			sent.enabledActionMask = enabled;
			sent.activeActionMask = active;
		}
		void publishThorBrowser(std::uint64_t revision, int page, int count, const std::vector<ThorBrowserEntry> & entries)
		{
			record("browser", revision);
			sent.browserPage = page;
			sent.browserPageCount = count;
			sent.browserEntries = entries;
		}
		void publishThorHeroManagement(std::uint64_t revision, const ThorHeroManagement & value)
		{
			record("hero", revision);
			sent.heroManagement = value;
		}
		void publishThorTownManagement(std::uint64_t revision, const ThorTownManagement & value)
		{
			record("town", revision);
			sent.townManagement = value;
		}
		void publishThorHeroMeetingArmies(std::uint64_t revision, const ThorHeroMeetingArmies & value)
		{
			record("armies", revision);
			sent.heroMeetingArmies = value;
		}
		void publishThorHeroMeetingArtifacts(std::uint64_t revision, const ThorHeroMeetingArtifacts & value)
		{
			record("artifacts", revision);
			sent.heroMeetingArtifacts = value;
		}
		void publishThorRecruitment(std::uint64_t revision, const ThorRecruitmentSnapshot & value)
		{
			record("recruitment", revision);
			sent.recruitment = value;
		}
	};

	ThorContextRecord windowContext(const std::string & id)
	{
		ThorContextRecord context;
		context.contextId = id;
		context.title = std::string(160, 'T');
		context.details[0] = "Details";
		context.windowSubjectId = 12;
		if(id == ThorContextIds::HERO_WINDOW)
		{
			context.heroPortraitAssetKey = thorHeroPortraitVisualAssetKey(7);
			context.heroManagement.emplace();
			auto & hero = *context.heroManagement;
			hero.heroId = 12;
			for(std::size_t i = 0; i < hero.armySlots.size(); ++i)
			{
				hero.armySlots[i].armyId = 12;
				hero.armySlots[i].slot = static_cast<int>(i);
			}
			hero.artifactSlots.resize(THOR_HERO_MEETING_EQUIPPED_ARTIFACT_COUNT);
			for(std::size_t i = 0; i < hero.artifactSlots.size(); ++i)
			{
				hero.artifactSlots[i].heroId = 12;
				hero.artifactSlots[i].position = static_cast<int>(i);
			}
		}
		if(id == ThorContextIds::TOWN_WINDOW)
		{
			context.browserPageCount = 1;
			for(std::size_t i = 0; i < THOR_TOWN_SERVICE_COUNT; ++i)
				context.browserEntries.push_back({static_cast<int>(i), "Service", true});
		}
		if(id == ThorContextIds::HERO_MEETING)
		{
			context.heroMeetingArmies.emplace();
			auto & armies = *context.heroMeetingArmies;
			armies.leftHeroId = 12;
			armies.rightHeroId = 13;
			armies.leftArmyId = 12;
			armies.rightArmyId = 13;
			for(std::size_t i = 0; i < armies.leftSlots.size(); ++i)
			{
				armies.leftSlots[i].armyId = 12;
				armies.rightSlots[i].armyId = 13;
				armies.leftSlots[i].slot = armies.rightSlots[i].slot = static_cast<int>(i);
			}
		}
		if(id == ThorContextIds::TOWN_RECRUITMENT_QUICK || id == ThorContextIds::TOWN_RECRUITMENT_DWELLING)
		{
			context.recruitment.emplace();
			context.recruitment->townId = 12;
			context.recruitment->destinationArmyId = 12;
			if(id == ThorContextIds::TOWN_RECRUITMENT_DWELLING)
			{
				context.recruitment->mode = ThorRecruitmentMode::TOWN_DWELLING;
				context.recruitment->dwellingLevel = 0;
			}
		}
		return context;
	}

	class ThorContextWindowPublisherTest : public testing::Test
	{
	protected:
		ThorContextStore store;
		RecordingBridge bridge;
		void publish(ThorContextRecord context, bool invalidate = false)
		{
			publishThorWindowContext(store, std::move(context), bridge,
				[&](const auto & published) { bridge.record("visuals", published.revision); }, invalidate);
		}
	};
}

TEST_F(ThorContextWindowPublisherTest, PreservesEachFamilySequenceAndNormalizedPayload)
{
	const std::vector<std::pair<std::string, std::vector<std::string>>> cases = {
		{ThorContextIds::HERO_WINDOW, {"context", "actions", "hero", "visuals"}},
		{ThorContextIds::TOWN_WINDOW, {"context", "actions", "browser"}},
		{ThorContextIds::HERO_MEETING, {"context", "armies", "artifacts", "actions", "visuals"}},
		{ThorContextIds::TOWN_RECRUITMENT_QUICK, {"context", "actions", "recruitment", "visuals"}},
		{ThorContextIds::TOWN_RECRUITMENT_DWELLING, {"context", "actions", "recruitment", "visuals"}}};
	for(const auto & [id, expected] : cases)
	{
		SCOPED_TRACE(id);
		bridge = {};
		auto next = windowContext(id);
		next.activeActionMask = 1;
		publish(next);
		const auto current = store.snapshot();
		EXPECT_EQ(bridge.calls, expected);
		EXPECT_EQ(bridge.revisions, std::vector<std::uint64_t>(expected.size(), current.revision));
		EXPECT_EQ(bridge.sent.title, std::string(128, 'T'));
		EXPECT_EQ(bridge.sent.heroManagement, current.heroManagement);
		EXPECT_EQ(bridge.sent.recruitment, current.recruitment);
		EXPECT_EQ(bridge.sent.browserEntries, current.browserEntries);
		EXPECT_EQ(bridge.sent.heroPortraitAssetKey, current.heroPortraitAssetKey);
		EXPECT_EQ(bridge.sent.activeActionMask,
			id == ThorContextIds::HERO_WINDOW || id == ThorContextIds::TOWN_WINDOW ? 0 : current.activeActionMask);
		if(id == ThorContextIds::HERO_MEETING)
		{
			EXPECT_EQ(bridge.sent.heroMeetingArmies, current.heroMeetingArmies);
			EXPECT_TRUE(bridge.sent.heroMeetingArtifacts.has_value()); // explicit empty fallback
			EXPECT_EQ(bridge.sent.details, ThorContextDetails{});
		}
		else
			EXPECT_EQ(bridge.sent.details, current.details);
		bridge = {};
		publish(next);
		EXPECT_TRUE(bridge.calls.empty());
		EXPECT_EQ(store.snapshot().revision, current.revision);
		// Differences beyond the normalized text bound must not churn either.
		auto same = next;
		same.title.back() = 'X';
		publish(same);
		EXPECT_TRUE(bridge.calls.empty());
		same.status = "Changed";
		publish(same);
		EXPECT_EQ(store.snapshot().revision, current.revision + 1);
		EXPECT_EQ(bridge.sent.status, "Changed");
	}
}

TEST_F(ThorContextWindowPublisherTest, InvalidationAdvancesOnlyWindowEpochsAndUsesNormalizedMasks)
{
	for(const auto * id : {ThorContextIds::HERO_WINDOW, ThorContextIds::TOWN_WINDOW, ThorContextIds::HERO_MEETING})
	{
		auto context = windowContext(id);
		context.enabledActionMask = thorActionMask(ThorAction::WINDOW_CLOSE) | (std::uint64_t{1} << 63);
		publish(context);
		const auto previous = store.snapshot();
		publish(context, true);
		EXPECT_EQ(store.snapshot().revision, previous.revision + 1);
		EXPECT_EQ(store.snapshot().actionEpoch, previous.actionEpoch + 1);
		EXPECT_EQ(bridge.sent.enabledActionMask, store.snapshot().enabledActionMask);
		// Bit 64 belongs to Adventure: the store must strip it for these windows.
		EXPECT_EQ(bridge.sent.enabledActionMask & (std::uint64_t{1} << 63), 0);
		bridge = {};
		publish(context);
		EXPECT_TRUE(bridge.calls.empty());
	}
	auto recruitment = windowContext(ThorContextIds::TOWN_RECRUITMENT_QUICK);
	recruitment.actionEpoch = 9; // recruitment's owner supplies its own epoch
	publish(recruitment, true);
	EXPECT_EQ(store.snapshot().actionEpoch, 9);
}

TEST_F(ThorContextWindowPublisherTest, WithdrawalAndParentRestorationDiscardChildPayloadsAndEpoch)
{
	auto hero = windowContext(ThorContextIds::HERO_WINDOW);
	publish(hero, true);
	publish(hero, true);
	publish(windowContext(ThorContextIds::TOWN_RECRUITMENT_QUICK));
	EXPECT_FALSE(store.snapshot().heroManagement);
	bridge = {};
	publish(ThorContextRecord{}); // Quick Recruitment's unsupported-owner withdrawal
	const auto hidden = store.snapshot();
	EXPECT_FALSE(hidden.recruitment);
	EXPECT_EQ(hidden.enabledActionMask, 0);
	EXPECT_EQ(bridge.calls, (std::vector<std::string>{"context", "actions"}));
	bridge = {};
	publish(hero);
	EXPECT_EQ(store.snapshot().actionEpoch, 0);
	EXPECT_FALSE(store.snapshot().recruitment);
	EXPECT_EQ(bridge.revisions.front(), hidden.revision + 1);
	EXPECT_EQ(bridge.calls, (std::vector<std::string>{"context", "actions", "hero", "visuals"}));
	const auto revision = store.snapshot().revision;
	bridge = {};
	publish(windowContext(ThorContextIds::ADVENTURE_MAP));
	EXPECT_TRUE(bridge.calls.empty());
	EXPECT_EQ(store.snapshot().revision, revision); // specialized owners cannot use this path
}

TEST_F(ThorContextWindowPublisherTest, InvalidRecruitmentClearsPayloadAndDoesNotPublishVisuals)
{
	auto context = windowContext(ThorContextIds::TOWN_RECRUITMENT_QUICK);
	context.enabledActionMask = thorActionMask(ThorAction::WINDOW_CLOSE);
	publish(context);
	context.recruitment->townId = -1;
	bridge = {};
	publish(context);
	EXPECT_FALSE(store.snapshot().recruitment);
	EXPECT_EQ(bridge.sent.enabledActionMask, 0);
	EXPECT_EQ(bridge.calls, (std::vector<std::string>{"context", "actions"}));
}

TEST_F(ThorContextWindowPublisherTest, TownManagementIsOptionalAndFollowsTheBrowserWithoutVisualPublication)
{
	auto context = windowContext(ThorContextIds::TOWN_WINDOW);
	context.townManagement.emplace();
	auto & town = *context.townManagement;
	town.townId = 12;
	town.garrisonArmyId = 12;
	for(std::size_t i = 0; i < town.garrisonSlots.size(); ++i)
	{
		town.garrisonSlots[i].armyId = 12;
		town.garrisonSlots[i].slot = town.visitingSlots[i].slot = static_cast<int>(i);
	}
	publish(context);
	EXPECT_EQ(bridge.calls, (std::vector<std::string>{"context", "actions", "browser", "town"}));
	ASSERT_TRUE(store.snapshot().townManagement);
	EXPECT_EQ(bridge.sent.townManagement, store.snapshot().townManagement);
	bridge = {};
	context.townManagement.reset();
	publish(context);
	EXPECT_FALSE(store.snapshot().townManagement);
	EXPECT_EQ(bridge.calls, (std::vector<std::string>{"context", "actions", "browser"}));
}

TEST_F(ThorContextWindowPublisherTest, PayloadChangesAndConsumedActionsPublishTheNewRevision)
{
	auto context = windowContext(ThorContextIds::HERO_WINDOW);
	context.enabledActionMask = thorActionMask(ThorAction::WINDOW_CLOSE);
	publish(context);
	const auto first = store.snapshot();
	context.heroManagement->heroName = "Renamed hero";
	publish(context);
	ASSERT_TRUE(bridge.sent.heroManagement);
	EXPECT_EQ(bridge.sent.heroManagement->heroName, "Renamed hero");
	EXPECT_EQ(store.snapshot().revision, first.revision + 1);
	context.enabledActionMask = 0;
	bridge = {};
	publish(context, true);
	EXPECT_EQ(bridge.sent.enabledActionMask, 0);
	EXPECT_EQ(store.snapshot().actionEpoch, first.actionEpoch + 1);
	EXPECT_EQ(store.snapshot().revision, first.revision + 2);
	EXPECT_EQ(bridge.revisions, std::vector<std::uint64_t>(4, first.revision + 2));
}
