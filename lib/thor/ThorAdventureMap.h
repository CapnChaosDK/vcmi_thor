/*
 * ThorAdventureMap.h, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 *
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 *
 */
#pragma once

#include "../../Global.h"
#include "../constants/NumericConstants.h"
#include "../constants/EntityIdentifiers.h"
#include <string>

#include <algorithm>
#include <cstdint>
#include <memory>
#include <optional>
#include <vector>

// VCMI's Giant preset is 252, but JSON/mod maps have arbitrary signed dimensions
// and up to 255 levels. This companion deliberately rejects unsupported maps whole.
inline constexpr int THOR_MAP_MAX_SIDE = 512;
inline constexpr int THOR_MAP_MAX_TILES = THOR_MAP_MAX_SIDE * THOR_MAP_MAX_SIDE;
inline constexpr int THOR_MAP_MAX_RGB_BYTES = THOR_MAP_MAX_TILES * 3;
inline constexpr int THOR_MAP_MAX_LEVELS = 2;
inline constexpr int THOR_MAP_MAX_HERO_MARKERS = GameConstants::MAX_HEROES_PER_PLAYER;
inline constexpr int THOR_MAP_MAX_TOWN_MARKERS = 64;
inline constexpr int THOR_MAP_MAX_MARKERS = THOR_MAP_MAX_HERO_MARKERS + THOR_MAP_MAX_TOWN_MARKERS;

struct DLL_LINKAGE ThorMapTile
{
	int x = 0;
	int y = 0;
	int level = 0;
	bool operator==(const ThorMapTile &) const = default;
};

struct DLL_LINKAGE ThorMapViewport
{
	int x = 0;
	int y = 0;
	int width = 0;
	int height = 0;
	bool operator==(const ThorMapViewport &) const = default;
};

struct DLL_LINKAGE ThorMapMarker
{
	int action = 0; // Existing SELECT_HERO (13) / SELECT_TOWN (14).
	int id = -1;
	ThorMapTile tile;
	bool selected = false;
	bool operator==(const ThorMapMarker &) const = default;
};

inline bool thorMapDimensionsValid(int width, int height, int levels)
{
	return width > 0 && height > 0 && width <= THOR_MAP_MAX_SIDE && height <= THOR_MAP_MAX_SIDE
		&& levels >= 1 && levels <= THOR_MAP_MAX_LEVELS;
}

inline bool thorMapTileValid(const ThorMapTile & tile, int width, int height, int levels)
{
	return thorMapDimensionsValid(width, height, levels) && tile.x >= 0 && tile.x < width
		&& tile.y >= 0 && tile.y < height && tile.level >= 0 && tile.level < levels;
}

/// Fixed stride encoding is independent of map shape. All unused bits/coordinates reject.
inline std::optional<int> encodeThorMapTile(const ThorMapTile & tile)
{
	if(!thorMapTileValid(tile, THOR_MAP_MAX_SIDE, THOR_MAP_MAX_SIDE, THOR_MAP_MAX_LEVELS))
		return std::nullopt;
	return tile.level * THOR_MAP_MAX_TILES + tile.y * THOR_MAP_MAX_SIDE + tile.x;
}

inline std::optional<ThorMapTile> decodeThorMapTile(int target)
{
	if(target < 0 || target >= THOR_MAP_MAX_TILES * THOR_MAP_MAX_LEVELS)
		return std::nullopt;
	return ThorMapTile{target % THOR_MAP_MAX_SIDE,
		(target / THOR_MAP_MAX_SIDE) % THOR_MAP_MAX_SIDE, target / THOR_MAP_MAX_TILES};
}

inline ThorMapViewport clampThorMapViewport(ThorMapViewport area, int width, int height)
{
	if(!thorMapDimensionsValid(width, height, 1) || area.width < 0 || area.height < 0)
		return {};
	const auto right = std::clamp<std::int64_t>(static_cast<std::int64_t>(area.x) + area.width, 0, width);
	const auto bottom = std::clamp<std::int64_t>(static_cast<std::int64_t>(area.y) + area.height, 0, height);
	area.x = std::clamp(area.x, 0, width);
	area.y = std::clamp(area.y, 0, height);
	area.width = static_cast<int>(std::max<std::int64_t>(0, right - area.x));
	area.height = static_cast<int>(std::max<std::int64_t>(0, bottom - area.y));
	return area;
}

inline constexpr int THOR_MAP_MAX_OBJECTS = 1024;
inline constexpr int THOR_MAP_MAX_OBJECT_LABEL_BYTES = 256;

// Explicit supported stationary groups. Unknown/mod-specific groups fail closed.
inline int thorMapObjectCategory(MapObjectID type)
{
	switch(type.getNum())
	{
		case Obj::RESOURCE: case Obj::TREASURE_CHEST: case Obj::SEA_CHEST:
		case Obj::ARTIFACT: case Obj::CAMPFIRE: case Obj::SPELL_SCROLL: return 1;
		case Obj::MINE: case Obj::ABANDONED_MINE: return 2;
		case Obj::CREATURE_GENERATOR1: case Obj::CREATURE_GENERATOR2:
		case Obj::CREATURE_GENERATOR3: case Obj::CREATURE_GENERATOR4: return 3;
		case Obj::MONOLITH_ONE_WAY_ENTRANCE: case Obj::MONOLITH_ONE_WAY_EXIT:
		case Obj::MONOLITH_TWO_WAY: case Obj::SUBTERRANEAN_GATE: case Obj::WHIRLPOOL: return 4;
		case Obj::LEARNING_STONE: case Obj::TREE_OF_KNOWLEDGE: case Obj::MAGIC_WELL:
		case Obj::STABLES: case Obj::OBELISK: case Obj::WINDMILL: case Obj::WATER_WHEEL:
		case Obj::SHRINE_OF_MAGIC_INCANTATION: case Obj::SHRINE_OF_MAGIC_GESTURE:
		case Obj::SHRINE_OF_MAGIC_THOUGHT: case Obj::UNIVERSITY: return 5;
		default: return 0;
	}
}

// Strict bounded UTF-8 validation independent of the engine text subsystem.
inline bool thorMapObjectLabelValid(const std::string & label)
{
	if(label.empty() || label.size() > THOR_MAP_MAX_OBJECT_LABEL_BYTES)
		return false;
	for(std::size_t i = 0; i < label.size();)
	{
		const auto first = static_cast<unsigned char>(label[i++]);
		if(first == 0) return false;
		if(first < 0x80) continue;
		int count = 0;
		std::uint32_t point = 0, minimum = 0;
		if(first >= 0xc2 && first <= 0xdf) { count = 1; point = first & 0x1f; minimum = 0x80; }
		else if(first >= 0xe0 && first <= 0xef) { count = 2; point = first & 0x0f; minimum = 0x800; }
		else if(first >= 0xf0 && first <= 0xf4) { count = 3; point = first & 7; minimum = 0x10000; }
		else return false;
		if(label.size() - i < static_cast<std::size_t>(count)) return false;
		while(count--)
		{
			const auto next = static_cast<unsigned char>(label[i++]);
			if((next & 0xc0) != 0x80) return false;
			point = (point << 6) | (next & 0x3f);
		}
		if(point < minimum || point > 0x10ffff || (point >= 0xd800 && point <= 0xdfff)) return false;
	}
	return true;
}

struct DLL_LINKAGE ThorMapObject
{
	int id = -1;
	ThorMapTile tile;
	int category = 0;
	std::string label; // Existing local-player hover information; no arbitrary internals.
	bool operator==(const ThorMapObject &) const = default;
};

struct DLL_LINKAGE ThorAdventureMap
{
	int width = 0;
	int height = 0;
	int level = 0;
	int levels = 0;
	std::uint64_t contentRevision = 0;
	// Shared immutable RGB24 colors, never source terrain, objects, paths or fog data.
	std::shared_ptr<const std::vector<std::uint8_t>> rgb;
	std::vector<ThorMapMarker> markers;
	std::vector<ThorMapObject> objects;
	bool objectsLimited = false;

	bool valid() const
	{
		if(!thorMapDimensionsValid(width, height, levels) || level < 0 || level >= levels
			|| contentRevision == 0 || !rgb || rgb->size() != static_cast<std::size_t>(width) * height * 3)
			return false;
		return markersValid() && objectsValid();
	}

	bool objectsValid() const
	{
		if(objects.size() > THOR_MAP_MAX_OBJECTS)
			return false;
		int previous = -1;
		for(const auto & object : objects)
		{
			if(object.id <= previous || object.category < 1 || object.category > 5
				|| !thorMapTileValid(object.tile, width, height, levels) || object.tile.level != level
				|| !thorMapObjectLabelValid(object.label))
				return false;
			previous = object.id;
		}
		return true;
	}

	bool markersValid() const
	{
		if(!thorMapDimensionsValid(width, height, levels) || markers.size() > THOR_MAP_MAX_MARKERS)
			return false;
		int heroes = 0, towns = 0;
		for(std::size_t index = 0; index < markers.size(); ++index)
		{
			const auto & marker = markers[index];
			if(marker.id < 0 || !thorMapTileValid(marker.tile, width, height, levels))
				return false;
			if(marker.action == 13)
				++heroes;
			else if(marker.action == 14)
				++towns;
			else
				return false;
			for(std::size_t earlier = 0; earlier < index; ++earlier)
				if(markers[earlier].id == marker.id)
					return false;
		}
		return heroes <= THOR_MAP_MAX_HERO_MARKERS && towns <= THOR_MAP_MAX_TOWN_MARKERS;
	}

	// Pixels are compared only when rebuilt by a dirty native minimap event.
	// Camera rectangles have their own cosmetic publication, outside action revisions.
	bool operator==(const ThorAdventureMap & other) const
	{
		return width == other.width && height == other.height && level == other.level && levels == other.levels
			&& contentRevision == other.contentRevision && rgb == other.rgb && markers == other.markers
			&& objects == other.objects && objectsLimited == other.objectsLimited;
	}
};

// Eligibility is evaluated before reading hover text, so hidden records cannot leak labels.
// Input identities must be ascending (as supplied by the callback-visible sorted roster).
template<typename LabelPublisher>
bool appendThorMapObject(ThorAdventureMap & map, int id, ThorMapTile tile, int category,
	bool objectVisible, bool tileVisible, LabelPublisher publishLabel)
{
	if(!objectVisible || !tileVisible || id < 0 || category < 1 || category > 5
		|| tile.level != map.level || !thorMapTileValid(tile, map.width, map.height, map.levels)
		|| (!map.objects.empty() && id <= map.objects.back().id))
		return false;
	if(map.objects.size() == THOR_MAP_MAX_OBJECTS)
	{
		map.objectsLimited = true;
		return true;
	}
	auto label = publishLabel();
	if(!thorMapObjectLabelValid(label))
		label = "Map object";
	std::replace(label.begin(), label.end(), '\n', ' ');
	std::replace(label.begin(), label.end(), '\r', ' ');
	map.objects.push_back({id, tile, category, std::move(label)});
	return false;
}
