/*
 * ThorAdventureMapTest.cpp, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 *
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 *
 */
#include "StdInc.h"

#include "../../lib/thor/ThorAction.h"
#include "../../lib/thor/ThorAdventureMap.h"
#include "../../client/adventureMap/MinimapColor.h"

#include <limits>

namespace
{
	ThorAdventureMap makeMap(int width = 12, int height = 8, int levels = 2)
	{
		ThorAdventureMap map;
		map.width = width;
		map.height = height;
		map.levels = levels;
		map.contentRevision = 1;
		map.rgb = std::make_shared<const std::vector<std::uint8_t>>(static_cast<std::size_t>(width) * height * 3, 0);
		return map;
	}

	ThorContextRecord makeContext(int width = 12, int height = 8, int levels = 2)
	{
		ThorContextRecord context;
		context.revision = 42;
		context.contextId = ThorContextIds::ADVENTURE_MAP;
		context.enabledActionMask = thorActionMask(ThorAction::ADVENTURE_CENTER_VIEW)
			| thorActionMask(ThorAction::ADVENTURE_SET_MAP_LEVEL)
			| thorActionMask(ThorAction::SELECT_HERO) | thorActionMask(ThorAction::SELECT_TOWN);
		context.heroes = {{4, "Hero", 100, 200, true, false}};
		context.towns = {{9, "Town", false}};
		context.adventureMap = makeMap(width, height, levels);
		context.adventureMap->markers = {{13, 4, {2, 3, 0}, true}, {14, 9, {5, 6, 1}, false}};
		if(levels == 1)
			context.adventureMap->markers[1].tile.level = 0;
		return context;
	}

	ThorActionRequest centerRequest(int x, int y, int level = 0)
	{
		ThorActionRequest request;
		request.revision = 42;
		request.action = ThorAction::ADVENTURE_CENTER_VIEW;
		request.targetId = encodeThorMapTile({x, y, level}).value_or(-1);
		return request;
	}

	ThorActionValidation validateLive(const ThorActionRequest & request, const ThorContextRecord & context,
		bool active = true, bool top = true, bool authority = true)
	{
		const auto & map = *context.adventureMap;
		return validateThorAdventureMapRequest(request, context, active, top, authority,
			map.width, map.height, map.levels, map.level);
	}
}

TEST(ThorContextAdventureMapTest, NavigationIdentifiersPreserveMaskAndFreeBudget)
{
	EXPECT_EQ(static_cast<int>(ThorAction::ADVENTURE_CENTER_VIEW), 58);
	EXPECT_EQ(static_cast<int>(ThorAction::ADVENTURE_SET_MAP_LEVEL), 59);
	EXPECT_EQ(thorActionMask(ThorAction::ADVENTURE_CENTER_VIEW), std::uint64_t{1} << 57);
	EXPECT_EQ(thorActionMask(ThorAction::ADVENTURE_SET_MAP_LEVEL), std::uint64_t{1} << 58);
	EXPECT_EQ(THOR_MAX_ACTION_ID, 59);
	EXPECT_LE(THOR_MAX_ACTION_ID, std::numeric_limits<std::uint64_t>::digits);
	for(int id = 60; id <= 64; ++id)
		EXPECT_FALSE(thorActionFromId(id));
	for(const auto action : {ThorAction::ADVENTURE_CENTER_VIEW, ThorAction::ADVENTURE_SET_MAP_LEVEL})
	{
		EXPECT_TRUE(isThorActionAllowedInAdventureMap(action));
		for(const auto * context : {ThorContextIds::HERO_WINDOW, ThorContextIds::TOWN_WINDOW,
			ThorContextIds::BATTLE, ThorContextIds::UNKNOWN, ThorContextIds::MAIN_MENU})
			EXPECT_FALSE(isThorActionAllowedInContext(action, context));
		EXPECT_FALSE(isThorActionHapticEligible(action));
		ThorActionRequest request;
		request.revision = 42;
		request.action = action;
		EXPECT_FALSE(thorActionAcceptance(request, ThorActionValidation::VALID, true));
	}
}

TEST(ThorContextAdventureMapTest, DimensionsBoundSignedModMapsBeforeAllocation)
{
	EXPECT_TRUE(thorMapDimensionsValid(252, 252, 2));
	EXPECT_TRUE(thorMapDimensionsValid(300, 123, 1));
	EXPECT_TRUE(thorMapDimensionsValid(512, 512, 2));
	EXPECT_EQ(THOR_MAP_MAX_RGB_BYTES, 786432);
	for(const auto invalid : {0, -1, 513, std::numeric_limits<int>::max()})
	{
		EXPECT_FALSE(thorMapDimensionsValid(invalid, 12, 1));
		EXPECT_FALSE(thorMapDimensionsValid(12, invalid, 1));
	}
	for(const auto levels : {-1, 0, 3, 255})
		EXPECT_FALSE(thorMapDimensionsValid(12, 8, levels));
	EXPECT_TRUE(makeMap(512, 512).valid());
}

TEST(ThorContextAdventureMapTest, FixedTileEncodingRoundTripsBothLevelsAndAllEdges)
{
	for(int level = 0; level < 2; ++level)
		for(int y : {0, 1, 7, 511})
			for(int x : {0, 1, 11, 511})
			{
				const ThorMapTile tile{x, y, level};
				const auto encoded = encodeThorMapTile(tile);
				ASSERT_TRUE(encoded);
				EXPECT_EQ(decodeThorMapTile(*encoded), tile);
			}
	for(const ThorMapTile tile : {ThorMapTile{-1, 0, 0}, {0, -1, 0}, {512, 0, 0},
		{0, 512, 0}, {0, 0, -1}, {0, 0, 2}})
		EXPECT_FALSE(encodeThorMapTile(tile));
	for(int target : {-1, THOR_MAP_MAX_TILES * 2, std::numeric_limits<int>::max()})
		EXPECT_FALSE(decodeThorMapTile(target));
}

TEST(ThorContextAdventureMapTest, ViewportIntersectionClampsRectangularMapAndOverflow)
{
	EXPECT_EQ(clampThorMapViewport({2, 3, 5, 4}, 12, 8), (ThorMapViewport{2, 3, 5, 4}));
	EXPECT_EQ(clampThorMapViewport({-4, -3, 10, 8}, 12, 8), (ThorMapViewport{0, 0, 6, 5}));
	EXPECT_EQ(clampThorMapViewport({10, 6, 10, 10}, 12, 8), (ThorMapViewport{10, 6, 2, 2}));
	EXPECT_EQ(clampThorMapViewport({-20, -20, 100, 100}, 12, 8), (ThorMapViewport{0, 0, 12, 8}));
	EXPECT_EQ(clampThorMapViewport({20, 20, 5, 5}, 12, 8), (ThorMapViewport{12, 8, 0, 0}));
	EXPECT_EQ(clampThorMapViewport({1, 1, std::numeric_limits<int>::max(),
		std::numeric_limits<int>::max()}, 12, 8), (ThorMapViewport{1, 1, 11, 7}));
	EXPECT_EQ(clampThorMapViewport({0, 0, -1, 4}, 12, 8), ThorMapViewport{});
	EXPECT_EQ(clampThorMapViewport({0, 0, 4, 4}, 0, 8), ThorMapViewport{});
}

TEST(ThorContextAdventureMapTest, PayloadRejectsIncompleteOrOversizedColors)
{
	const auto original = makeMap();
	ASSERT_TRUE(original.valid());
	for(const auto size : {std::size_t{0}, original.rgb->size() - 1, original.rgb->size() + 1})
	{
		auto invalid = original;
		invalid.rgb = std::make_shared<const std::vector<std::uint8_t>>(size, 0);
		EXPECT_FALSE(invalid.valid());
	}
	auto invalid = original;
	invalid.rgb.reset();
	EXPECT_FALSE(invalid.valid());
	invalid = original;
	invalid.contentRevision = 0;
	EXPECT_FALSE(invalid.valid());
	invalid = original;
	invalid.width = 513;
	EXPECT_FALSE(invalid.valid());
	invalid = original;
	invalid.level = invalid.levels;
	EXPECT_FALSE(invalid.valid());
}

TEST(ThorContextAdventureMapTest, FogNeverEvaluatesObjectsOrTerrainAndVisibleColorsMatchMinimap)
{
	struct Terrain
	{
		int minimapBlocked = 20;
		int minimapUnblocked = 30;
	};
	struct Tile
	{
		Terrain terrain;
		std::vector<int> blockingObjects;
		bool isBlocked = false;
		bool isVisitable = false;
		bool blocked() const { return isBlocked; }
		bool visitable() const { return isVisitable; }
		const Terrain * getTerrain() const { return &terrain; }
	};
	int evaluations = 0;
	const auto objectColor = [&](int id) -> std::optional<int>
	{
		++evaluations;
		return id == 7 ? std::optional<int>{40} : std::nullopt;
	};
	EXPECT_EQ(minimapTileColor(static_cast<const Tile *>(nullptr), 0, objectColor), 0);
	EXPECT_EQ(evaluations, 0);
	Tile tile;
	EXPECT_EQ(minimapTileColor(&tile, 0, objectColor), 30);
	tile.isBlocked = true;
	EXPECT_EQ(minimapTileColor(&tile, 0, objectColor), 20);
	tile.isVisitable = true;
	EXPECT_EQ(minimapTileColor(&tile, 0, objectColor), 30);
	tile.blockingObjects = {3}; // Unavailable/uncolored object reveals no metadata.
	EXPECT_EQ(minimapTileColor(&tile, 0, objectColor), 30);
	tile.blockingObjects = {3, 7, 8};
	evaluations = 0;
	EXPECT_EQ(minimapTileColor(&tile, 0, objectColor), 40);
	EXPECT_EQ(evaluations, 2); // Same first visible owner-color precedence as native minimap.
}

TEST(ThorContextAdventureMapTest, MarkerTypesCountsIdentitiesAndCoordinatesAreBounded)
{
	auto map = makeMap();
	map.markers = {{13, 4, {0, 0, 0}, true}, {14, 9, {11, 7, 1}, false}};
	ASSERT_TRUE(map.valid());
	auto beforeColorAllocation = map;
	beforeColorAllocation.rgb.reset();
	EXPECT_TRUE(beforeColorAllocation.markersValid());
	beforeColorAllocation.markers[0].tile.x = -1;
	EXPECT_FALSE(beforeColorAllocation.markersValid());
	for(const ThorMapMarker marker : {ThorMapMarker{12, 5, {1, 1, 0}, false},
		{13, -1, {1, 1, 0}, false}, {14, 5, {-1, 0, 0}, false},
		{14, 5, {12, 0, 0}, false}, {14, 5, {0, 8, 0}, false}, {14, 5, {0, 0, 2}, false}})
	{
		auto invalid = map;
		invalid.markers.push_back(marker);
		EXPECT_FALSE(invalid.valid());
	}
	auto duplicate = map;
	duplicate.markers.push_back(map.markers[0]);
	EXPECT_FALSE(duplicate.valid());
	map.markers.clear();
	for(int index = 0; index < 8; ++index)
		map.markers.push_back({13, index, {0, 0, 0}, false});
	for(int index = 0; index < 64; ++index)
		map.markers.push_back({14, 100 + index, {0, 0, 0}, false});
	EXPECT_TRUE(map.valid());
	map.markers.push_back({14, 200, {0, 0, 0}, false});
	EXPECT_FALSE(map.valid());
	map.markers.resize(9);
	map.markers[8] = {13, 50, {0, 0, 0}, false};
	EXPECT_FALSE(map.valid());
	map.markers.clear();
	for(int index = 0; index < 65; ++index)
		map.markers.push_back({14, 100 + index, {0, 0, 0}, false});
	EXPECT_FALSE(map.valid());
}

TEST(ThorContextAdventureMapTest, CenterAcceptsSquareRectangularCornersAndRejectsOutsideCurrentLevel)
{
	for(const auto dimensions : {std::pair{12, 8}, std::pair{8, 12}, std::pair{12, 12}})
	{
		const auto context = makeContext(dimensions.first, dimensions.second);
		for(int x : {0, dimensions.first - 1})
			for(int y : {0, dimensions.second - 1})
				EXPECT_EQ(validateLive(centerRequest(x, y), context), ThorActionValidation::VALID);
		EXPECT_EQ(validateLive(centerRequest(dimensions.first, 0), context), ThorActionValidation::INVALID_TARGET);
		EXPECT_EQ(validateLive(centerRequest(0, dimensions.second), context), ThorActionValidation::INVALID_TARGET);
		EXPECT_EQ(validateLive(centerRequest(0, 0, 1), context), ThorActionValidation::INVALID_TARGET);
		EXPECT_EQ(validateLive(centerRequest(-1, 0), context), ThorActionValidation::INVALID_TARGET);
	}
	auto underground = makeContext();
	underground.adventureMap->level = 1;
	EXPECT_EQ(validateLive(centerRequest(11, 7, 1), underground), ThorActionValidation::VALID);
	EXPECT_EQ(validateLive(centerRequest(11, 7, 0), underground), ThorActionValidation::INVALID_TARGET);
}

TEST(ThorContextAdventureMapTest, NavigationRequiresFreshAvailableContextAndExactLiveOwner)
{
	const auto context = makeContext();
	auto request = centerRequest(2, 3);
	EXPECT_NE(validateLive(request, context, false), ThorActionValidation::VALID);
	EXPECT_NE(validateLive(request, context, true, false), ThorActionValidation::VALID);
	EXPECT_NE(validateLive(request, context, true, true, false), ThorActionValidation::VALID);
	EXPECT_NE(validateThorAdventureMapRequest(request, context, true, true, true, 11, 8, 2, 0), ThorActionValidation::VALID);
	EXPECT_NE(validateThorAdventureMapRequest(request, context, true, true, true, 12, 7, 2, 0), ThorActionValidation::VALID);
	EXPECT_NE(validateThorAdventureMapRequest(request, context, true, true, true, 12, 8, 1, 0), ThorActionValidation::VALID);
	EXPECT_NE(validateThorAdventureMapRequest(request, context, true, true, true, 12, 8, 2, 1), ThorActionValidation::VALID);
	request.revision = 41;
	EXPECT_EQ(validateLive(request, context), ThorActionValidation::STALE_REVISION);
	request.revision = 42;
	auto changed = context;
	changed.enabledActionMask = 0;
	EXPECT_EQ(validateLive(request, changed), ThorActionValidation::UNAVAILABLE);
	changed = context;
	changed.contextId = ThorContextIds::HERO_WINDOW;
	EXPECT_EQ(validateThorActionRequest(request, changed), ThorActionValidation::WRONG_CONTEXT);
	changed = context;
	changed.adventureMap.reset();
	EXPECT_EQ(validateThorActionRequest(request, changed), ThorActionValidation::INVALID_TARGET);
}

TEST(ThorContextAdventureMapTest, NavigationRejectsMalformedAndUnrelatedPayloadFields)
{
	const auto context = makeContext();
	for(int target : {-1, THOR_MAP_MAX_TILES * 2, std::numeric_limits<int>::max()})
	{
		auto request = centerRequest(0, 0);
		request.targetId = target;
		EXPECT_EQ(validateLive(request, context), ThorActionValidation::INVALID_TARGET);
	}
	int ThorActionRequest::* const fields[] = {&ThorActionRequest::sourceArmyId, &ThorActionRequest::sourceSlot,
		&ThorActionRequest::destinationArmyId, &ThorActionRequest::destinationSlot, &ThorActionRequest::amount};
	for(const auto action : {ThorAction::ADVENTURE_CENTER_VIEW, ThorAction::ADVENTURE_SET_MAP_LEVEL})
	{
		for(auto field : fields)
		{
			auto request = centerRequest(0, 0);
			request.action = action;
			if(action == ThorAction::ADVENTURE_SET_MAP_LEVEL)
				request.targetId = 1;
			request.*field = 0;
			EXPECT_EQ(validateLive(request, context), ThorActionValidation::INVALID_TARGET);
		}
		auto request = centerRequest(0, 0);
		request.action = action;
		if(action == ThorAction::ADVENTURE_SET_MAP_LEVEL)
			request.targetId = 1;
		request.recruitmentOperation = ThorRecruitmentOperation::SELECT_ROW;
		EXPECT_EQ(validateLive(request, context), ThorActionValidation::INVALID_TARGET);
	}
}

TEST(ThorContextAdventureMapTest, LevelChangeRejectsNoOpUnknownAndAbsentUnderground)
{
	auto context = makeContext();
	ThorActionRequest request;
	request.revision = 42;
	request.action = ThorAction::ADVENTURE_SET_MAP_LEVEL;
	request.targetId = 1;
	EXPECT_EQ(validateLive(request, context), ThorActionValidation::VALID);
	EXPECT_NE(validateLive(request, context, false), ThorActionValidation::VALID);
	EXPECT_NE(validateLive(request, context, true, false), ThorActionValidation::VALID);
	EXPECT_NE(validateLive(request, context, true, true, false), ThorActionValidation::VALID);
	for(int target : {-1, 0, 2, 255, std::numeric_limits<int>::max()})
	{
		request.targetId = target;
		EXPECT_EQ(validateLive(request, context), ThorActionValidation::INVALID_TARGET);
	}
	request.targetId = 1;
	EXPECT_EQ(validateLive(request, makeContext(12, 8, 1)), ThorActionValidation::INVALID_TARGET);
	context.adventureMap->level = 1;
	request.targetId = 0;
	EXPECT_EQ(validateLive(request, context), ThorActionValidation::VALID);
	request.revision = 41;
	EXPECT_EQ(validateLive(request, context), ThorActionValidation::STALE_REVISION);
}

TEST(ThorContextAdventureMapTest, StoreClearsInvalidMapAndUnpublishedMarkerIdentity)
{
	ThorContextStore store;
	auto context = makeContext();
	ASSERT_TRUE(store.publishNext(context).adventureMap);
	context.contextId = ThorContextIds::HERO_WINDOW;
	EXPECT_FALSE(store.publishNext(context).adventureMap);
	context = makeContext();
	context.adventureMap->markers[0].id = 20;
	EXPECT_FALSE(store.publishNext(context).adventureMap);
	context = makeContext();
	context.adventureMap->markers[0].action = 14;
	EXPECT_FALSE(store.publishNext(context).adventureMap);
	context = makeContext();
	context.adventureMap->markers[0].selected = false;
	EXPECT_FALSE(store.publishNext(context).adventureMap);
	context = makeContext();
	context.adventureMap->width = 513;
	const auto oversized = store.publishNext(context);
	EXPECT_FALSE(oversized.adventureMap);
	EXPECT_EQ(oversized.enabledActionMask & thorActionMask(ThorAction::ADVENTURE_CENTER_VIEW), 0);
	EXPECT_EQ(oversized.enabledActionMask & thorActionMask(ThorAction::ADVENTURE_SET_MAP_LEVEL), 0);
	EXPECT_NE(oversized.enabledActionMask & thorActionMask(ThorAction::SELECT_HERO), 0);
	EXPECT_NE(oversized.enabledActionMask & thorActionMask(ThorAction::SELECT_TOWN), 0);
}

TEST(ThorContextAdventureMapTest, ContentAndMarkerChangesInvalidateActionsWithoutPixelCopy)
{
	ThorContextStore store;
	auto context = makeContext();
	const auto initial = store.publishNext(context);
	ASSERT_TRUE(initial.adventureMap);
	EXPECT_EQ(initial.adventureMap->rgb, context.adventureMap->rgb);
	EXPECT_EQ(store.publishNext(context).revision, initial.revision);
	EXPECT_EQ(store.snapshot().adventureMap->rgb, initial.adventureMap->rgb);
	context.adventureMap->markers[0].tile.x += 1;
	const auto moved = store.publishNext(context);
	EXPECT_GT(moved.revision, initial.revision);
	EXPECT_EQ(moved.adventureMap->rgb, initial.adventureMap->rgb);
	context.adventureMap->markers[0].selected = false;
	context.heroes[0].selected = false;
	const auto selected = store.publishNext(context);
	EXPECT_GT(selected.revision, moved.revision);
	context.adventureMap->contentRevision += 1;
	const auto revealed = store.publishNext(context);
	EXPECT_GT(revealed.revision, selected.revision);
	EXPECT_EQ(validateThorActionRequest(centerRequest(0, 0), revealed), ThorActionValidation::STALE_REVISION);
	context.adventureMap->level = 1;
	EXPECT_GT(store.publishNext(context).revision, revealed.revision);
}

TEST(ThorContextAdventureMapTest, StationaryInformationRejectsUnsupportedCategoriesAndInvalidRecords)
{
	EXPECT_EQ(thorMapObjectCategory(Obj::RESOURCE), 1);
	EXPECT_EQ(thorMapObjectCategory(Obj::MINE), 2);
	EXPECT_EQ(thorMapObjectCategory(Obj::CREATURE_GENERATOR1), 3);
	EXPECT_EQ(thorMapObjectCategory(Obj::SUBTERRANEAN_GATE), 4);
	EXPECT_EQ(thorMapObjectCategory(Obj::LEARNING_STONE), 5);
	EXPECT_EQ(thorMapObjectCategory(Obj::HERO), 0);
	EXPECT_EQ(thorMapObjectCategory(Obj::TOWN), 0);
	EXPECT_EQ(thorMapObjectCategory(Obj::EVENT), 0);
	EXPECT_EQ(thorMapObjectCategory(MapObjectID(99999)), 0);
	auto map = makeMap();
	map.objects = {{7, {3, 4, 0}, 1, "Resource"}};
	EXPECT_TRUE(map.valid());
	for(const ThorMapObject invalid : {ThorMapObject{-1, {3, 4, 0}, 1, "Resource"},
		ThorMapObject{7, {12, 4, 0}, 1, "Resource"}, ThorMapObject{7, {3, 4, 1}, 1, "Resource"},
		ThorMapObject{7, {3, 4, 0}, 6, "Resource"}, ThorMapObject{7, {3, 4, 0}, 1, ""},
		ThorMapObject{7, {3, 4, 0}, 1, std::string(257, 'a')},
		ThorMapObject{7, {3, 4, 0}, 1, std::string("a\0b", 3)},
		ThorMapObject{7, {3, 4, 0}, 1, std::string(1, static_cast<char>(0xff))}})
	{
		map.objects = {invalid};
		EXPECT_FALSE(map.valid());
	}
	map.objects = {{7, {3, 4, 0}, 1, "Resource"}, {7, {4, 4, 0}, 1, "Other"}};
	EXPECT_FALSE(map.valid());
	map.objects = {{8, {3, 4, 0}, 1, "Resource"}, {7, {4, 4, 0}, 1, "Other"}};
	EXPECT_FALSE(map.valid());
	map.objects.clear();
	for(int id = 0; id <= THOR_MAP_MAX_OBJECTS; ++id)
		map.objects.push_back({id, {0, 0, 0}, 1, "Resource"});
	EXPECT_FALSE(map.valid());
	map.objects.pop_back();
	EXPECT_TRUE(map.valid());
}

TEST(ThorContextAdventureMapTest, InformationChangesAdvanceRevisionWithoutPixelChurn)
{
	ThorContextStore store;
	auto context = makeContext();
	context.adventureMap->objects = {{7, {3, 4, 0}, 1, "Resource"}};
	const auto first = store.publishNext(context);
	auto changed = context;
	changed.adventureMap->objects[0].label = "Resource (visited)";
	const auto second = store.publishNext(changed);
	EXPECT_GT(second.revision, first.revision);
	EXPECT_EQ(second.adventureMap->rgb, first.adventureMap->rgb);
	changed.adventureMap->objects.clear(); // visibility withdrawal/removal with identical colors
	const auto withdrawn = store.publishNext(changed);
	EXPECT_GT(withdrawn.revision, second.revision);
	EXPECT_TRUE(withdrawn.adventureMap->objects.empty());
	changed.adventureMap->objects = {{8, {4, 2, 0}, 2, "Mine"}};
	const auto added = store.publishNext(changed);
	EXPECT_GT(added.revision, withdrawn.revision);
}

TEST(ThorContextAdventureMapTest, ObjectLabelsValidateUnicodeWithoutTruncatingCodepoints)
{
	EXPECT_TRUE(thorMapObjectLabelValid(std::string("\xf0\x9f\x98\x80")));
	EXPECT_TRUE(thorMapObjectLabelValid(std::string("\xe6\x97\xa5")));
	EXPECT_FALSE(thorMapObjectLabelValid(std::string("\xc0\xaf")));
	EXPECT_FALSE(thorMapObjectLabelValid(std::string("\xed\xa0\x80")));
	EXPECT_FALSE(thorMapObjectLabelValid(std::string("\xf4\x90\x80\x80")));
	EXPECT_FALSE(thorMapObjectLabelValid(std::string("\xf0\x9f")));
}

TEST(ThorContextAdventureMapTest, VisibilityGateWithdrawsFoggedObjectsBeforeReadingInformation)
{
	auto map = makeMap();
	int labelsRead = 0;
	auto label = [&]() { ++labelsRead; return std::string("Mine"); };
	EXPECT_FALSE(appendThorMapObject(map, 1, {3, 2, 0}, 2, false, true, label));
	EXPECT_FALSE(appendThorMapObject(map, 1, {3, 2, 0}, 2, true, false, label));
	EXPECT_FALSE(appendThorMapObject(map, 1, {3, 2, 1}, 2, true, true, label));
	EXPECT_FALSE(appendThorMapObject(map, 1, {3, 2, 0}, 0, true, true, label));
	EXPECT_EQ(labelsRead, 0);
	EXPECT_TRUE(map.objects.empty());
	EXPECT_FALSE(appendThorMapObject(map, 1, {3, 2, 0}, 2, true, true, label));
	EXPECT_EQ(labelsRead, 1);
	ASSERT_EQ(map.objects.size(), 1);
	EXPECT_EQ(map.objects.front().label, "Mine");
	map.objects.clear(); // Rebuild the next visibility-safe immutable snapshot.
	EXPECT_FALSE(appendThorMapObject(map, 1, {3, 2, 0}, 2, true, false, label));
	EXPECT_EQ(labelsRead, 1);
	EXPECT_TRUE(map.objects.empty());
	EXPECT_FALSE(appendThorMapObject(map, 2, {4, 2, 0}, 2, true, true, [] { return std::string("Changed mine"); }));
	EXPECT_EQ(map.objects.front().id, 2);
	EXPECT_EQ(map.objects.front().label, "Changed mine");
}

TEST(ThorContextAdventureMapTest, StationaryPublicationLimitIsDeterministicAndDoesNotReadOverflowLabels)
{
	auto map = makeMap();
	int labelsRead = 0;
	auto label = [&]() { ++labelsRead; return std::string("Resource"); };
	for(int id = 0; id < THOR_MAP_MAX_OBJECTS; ++id)
		EXPECT_FALSE(appendThorMapObject(map, id, {0, 0, 0}, 1, true, true, label));
	EXPECT_FALSE(map.objectsLimited);
	EXPECT_TRUE(appendThorMapObject(map, THOR_MAP_MAX_OBJECTS, {0, 0, 0}, 1, true, true, label));
	EXPECT_TRUE(map.objectsLimited);
	EXPECT_EQ(labelsRead, THOR_MAP_MAX_OBJECTS);
	EXPECT_EQ(map.objects.size(), THOR_MAP_MAX_OBJECTS);
	EXPECT_EQ(map.objects.front().id, 0);
	EXPECT_EQ(map.objects.back().id, THOR_MAP_MAX_OBJECTS - 1);
	EXPECT_FALSE(appendThorMapObject(map, 0, {0, 0, 0}, 1, true, true, label));
	EXPECT_EQ(labelsRead, THOR_MAP_MAX_OBJECTS);
}
