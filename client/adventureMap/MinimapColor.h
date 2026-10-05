/*
 * MinimapColor.h, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 *
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 *
 */
#pragma once

/// Only callback-visible tiles reach object or terrain color resolution. The same
/// helper serves CMinimap and Thor, and can be tested without player-installed data.
template<typename Tile, typename Color, typename ResolveObjectColor>
Color minimapTileColor(const Tile * tile, Color hidden, ResolveObjectColor objectColor)
{
	if(!tile)
		return hidden;
	for(const auto & objectID : tile->blockingObjects)
	{
		const auto color = objectColor(objectID);
		if(color)
			return *color;
	}
	return tile->blocked() && !tile->visitable()
		? tile->getTerrain()->minimapBlocked : tile->getTerrain()->minimapUnblocked;
}
