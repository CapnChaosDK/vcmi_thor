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

	bool valid() const
	{
		if(!thorMapDimensionsValid(width, height, levels) || level < 0 || level >= levels
			|| contentRevision == 0 || !rgb || rgb->size() != static_cast<std::size_t>(width) * height * 3)
			return false;
		return markersValid();
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
			&& contentRevision == other.contentRevision && rgb == other.rgb && markers == other.markers;
	}
};
