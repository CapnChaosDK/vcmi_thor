#include "StdInc.h"

#include "../../lib/thor/ThorVisualAssetCache.h"

namespace
{
	ThorVisualAssetPayload payload(std::uint64_t key, std::size_t bytes = 24)
	{
		ThorVisualAssetPayload result;
		result.key = key;
		result.width = 4;
		result.height = 4;
		result.pngBytes.resize(bytes, 0x2a);
		return result;
	}

	ThorHeroMeetingArmies armiesWithOneCreature(std::uint64_t key)
	{
		ThorHeroMeetingArmies armies;
		armies.leftArmyId = 1;
		armies.rightArmyId = 2;
		for(std::size_t index = 0; index < THOR_HERO_MEETING_ARMY_SIZE; ++index)
		{
			armies.leftSlots[index] = {1, static_cast<int>(index), false, -1, "", 0};
			armies.rightSlots[index] = {2, static_cast<int>(index), false, -1, "", 0};
		}
		armies.leftSlots[0] = {1, 0, true, 7, "Creature", 3, key};
		return armies;
	}

	ThorHeroMeetingArtifacts emptyArtifacts()
	{
		ThorHeroMeetingArtifacts artifacts;
		artifacts.leftHeroId = 1;
		artifacts.rightHeroId = 2;
		for(std::size_t index = 0; index < THOR_HERO_MEETING_ARTIFACT_COUNT; ++index)
		{
			const auto position = index % 24;
			artifacts.artifactSlots.push_back({index < 24 ? 1 : 2, static_cast<int>(position), position >= 19,
				false, false, "", -1});
		}
		return artifacts;
	}
}

TEST(ThorVisualAssetTest, StableKeysSeparateKindsAndUseArtifactTypeIdentity)
{
	const auto creatureKey = thorCreatureVisualAssetKey(7);
	const auto artifactKey = thorArtifactVisualAssetKey(7);
	ASSERT_TRUE(isThorVisualAssetKey(creatureKey));
	ASSERT_TRUE(isThorVisualAssetKey(artifactKey));
	EXPECT_NE(creatureKey, artifactKey);
	EXPECT_EQ(thorArtifactVisualAssetKey(7), thorArtifactVisualAssetKey(7));
	EXPECT_NE(thorArtifactVisualAssetKey(7), thorArtifactVisualAssetKey(8));
	EXPECT_FALSE(isThorVisualAssetKey(thorArtifactVisualAssetKey(-1)));
	EXPECT_FALSE(isThorVisualAssetKey((1ULL << 56) | (1ULL << 40) | 1ULL));
}

TEST(ThorVisualAssetTest, PayloadDimensionsAndBytesAreExplicitlyBounded)
{
	auto candidate = payload(thorCreatureVisualAssetKey(1));
	EXPECT_TRUE(isThorVisualAssetPayloadValid(candidate));
	candidate.width = static_cast<std::uint16_t>(THOR_VISUAL_ASSET_MAX_DIMENSION + 1);
	EXPECT_FALSE(isThorVisualAssetPayloadValid(candidate));
	candidate = payload(thorCreatureVisualAssetKey(1), THOR_VISUAL_ASSET_MAX_PAYLOAD_BYTES + 1);
	EXPECT_FALSE(isThorVisualAssetPayloadValid(candidate));
	EXPECT_TRUE(isThorVisualAssetPayloadValid({thorArtifactVisualAssetKey(2)})); // bounded negative-cache marker
	EXPECT_FALSE(isThorVisualAssetPayloadValid({0}));
}

TEST(ThorVisualAssetTest, SnapshotKeysStayWithTheirOccupiedSlotsAndDuplicateTypesShareOneAsset)
{
	const auto sharedCreature = thorCreatureVisualAssetKey(7);
	auto armies = armiesWithOneCreature(sharedCreature);
	auto artifacts = emptyArtifacts();
	artifacts.artifactSlots[0].occupied = true;
	artifacts.artifactSlots[0].instanceId = 400;
	artifacts.artifactSlots[0].artifactTypeId = 11;
	artifacts.artifactSlots[0].visualAssetKey = thorArtifactVisualAssetKey(11);
	artifacts.artifactSlots[1].occupied = true;
	artifacts.artifactSlots[1].instanceId = 401;
	artifacts.artifactSlots[1].artifactTypeId = 11;
	artifacts.artifactSlots[1].visualAssetKey = thorArtifactVisualAssetKey(11);

	const auto keys = collectThorHeroMeetingVisualAssetKeys(armies, artifacts);
	ASSERT_EQ(keys.size(), 2);
	EXPECT_EQ(keys[0], sharedCreature);
	EXPECT_EQ(keys[1], thorArtifactVisualAssetKey(11));

	artifacts.artifactSlots[1].artifactTypeId = 12;
	artifacts.artifactSlots[1].visualAssetKey = thorArtifactVisualAssetKey(12);
	EXPECT_EQ(collectThorHeroMeetingVisualAssetKeys(armies, artifacts).size(), 3);
}

TEST(ThorVisualAssetTest, AssetCollectionFailsClosedOnTooManySlotsOrInvalidKeys)
{
	const auto armies = armiesWithOneCreature(thorCreatureVisualAssetKey(1));
	auto artifacts = emptyArtifacts();
	artifacts.artifactSlots.push_back({});
	EXPECT_EQ(collectThorHeroMeetingVisualAssetKeys(armies, artifacts),
		std::vector<std::uint64_t>{thorCreatureVisualAssetKey(1)});
	artifacts = emptyArtifacts();
	artifacts.artifactSlots[0].occupied = true;
	artifacts.artifactSlots[0].visualAssetKey = 9;
	EXPECT_EQ(collectThorHeroMeetingVisualAssetKeys(armies, artifacts),
		std::vector<std::uint64_t>{thorCreatureVisualAssetKey(1)});
}

TEST(ThorVisualAssetTest, DuplicateCacheInsertReusesBytesAndLruEvictionStaysBounded)
{
	ThorVisualAssetCache cache;
	const auto firstKey = thorCreatureVisualAssetKey(0);
	auto original = payload(firstKey);
	ASSERT_TRUE(cache.put(original));
	auto duplicate = payload(firstKey);
	duplicate.pngBytes.assign(24, 0x55);
	ASSERT_TRUE(cache.put(duplicate));
	EXPECT_EQ(cache.size(), 1);
	EXPECT_EQ(cache.get(firstKey), original);
	EXPECT_TRUE(cache.get(firstKey).has_value());

	for(int id = 1; id < static_cast<int>(THOR_VISUAL_ASSET_CACHE_MAX_ENTRIES); ++id)
		ASSERT_TRUE(cache.put(payload(thorArtifactVisualAssetKey(id))));
	EXPECT_EQ(cache.size(), THOR_VISUAL_ASSET_CACHE_MAX_ENTRIES);
	EXPECT_TRUE(cache.get(firstKey).has_value()); // Refresh the older entry immediately before eviction.
	ASSERT_TRUE(cache.put(payload(thorArtifactVisualAssetKey(static_cast<int>(THOR_VISUAL_ASSET_CACHE_MAX_ENTRIES)))));
	EXPECT_EQ(cache.size(), THOR_VISUAL_ASSET_CACHE_MAX_ENTRIES);
	EXPECT_TRUE(cache.get(firstKey).has_value());
	EXPECT_FALSE(cache.get(thorArtifactVisualAssetKey(1)).has_value());
	EXPECT_LE(cache.bytes(), THOR_VISUAL_ASSET_CACHE_MAX_BYTES);
}

TEST(ThorVisualAssetTest, ByteBudgetEvictsOldPayloadsAndNegativeResultsUseNoImageMemory)
{
	ThorVisualAssetCache cache;
	for(int id = 0; id < 33; ++id)
		ASSERT_TRUE(cache.put(payload(thorCreatureVisualAssetKey(id), THOR_VISUAL_ASSET_MAX_PAYLOAD_BYTES)));
	EXPECT_LE(cache.bytes(), THOR_VISUAL_ASSET_CACHE_MAX_BYTES);
	EXPECT_LT(cache.size(), 33);
	ASSERT_TRUE(cache.put({thorArtifactVisualAssetKey(1)}));
	EXPECT_EQ(cache.get(thorArtifactVisualAssetKey(1)), ThorVisualAssetPayload{thorArtifactVisualAssetKey(1)});
}
