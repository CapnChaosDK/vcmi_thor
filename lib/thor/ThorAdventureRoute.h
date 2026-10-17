/*
 * ThorAdventureRoute.h, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 *
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 *
 */
#pragma once

#include "ThorAdventureMap.h"

inline constexpr int THOR_ROUTE_MAX_NODES = 4096;
inline constexpr int THOR_ROUTE_CAN_CONFIRM = 1;
inline constexpr int THOR_ROUTE_CAN_CANCEL = 2;
inline constexpr int THOR_ROUTE_CAN_CLEAR = 4;
inline constexpr int THOR_ROUTE_CAN_STOP = 8;
inline constexpr int THOR_ROUTE_CAN_CONTINUE = 16;
inline constexpr int THOR_ROUTE_TRUNCATED = 32;

enum class ThorRouteState : int
{
	NO_HERO = 0, IDLE, PREVIEW, UNREACHABLE, COMMITTED, MOVING, STOPPING,
	READY_NEXT_TURN, PAUSED, FINISHED, INTERRUPTED, CHANGED
};

enum class ThorRouteCommand : int
{
	CONFIRM = 1, CANCEL_PREVIEW, CLEAR_ROUTE, STOP
};

struct DLL_LINKAGE ThorRouteNode
{
	ThorMapTile tile;
	int turns = 0;
	int moveRemains = 0;
	int layer = 0;
	int action = 0;
	bool operator==(const ThorRouteNode &) const = default;
};

struct DLL_LINKAGE ThorAdventureRoute
{
	int heroId = -1;
	ThorRouteState state = ThorRouteState::NO_HERO;
	int flags = 0;
	int width = 0;
	int height = 0;
	int levels = 0;
	ThorMapTile start{-1, -1, -1};
	ThorMapTile destination{-1, -1, -1};
	int movement = 0;
	int limit = 0;
	int arrivalTurns = -1;
	int arrivalRemains = -1;
	int totalNodes = 0;
	// Chronological prefix, including the current hero position. Teleports retain
	// their native action/layer; adjacent coordinates are deliberately not assumed.
	std::vector<ThorRouteNode> nodes;

	bool valid() const
	{
		if(!thorMapDimensionsValid(width, height, levels) || movement < 0 || limit < 0
			|| static_cast<int>(state) < 0 || state > ThorRouteState::CHANGED || flags < 0 || (flags & ~63))
			return false;
		const ThorMapTile absent{-1, -1, -1};
		if(state == ThorRouteState::NO_HERO)
			return heroId == -1 && start == absent && destination == absent && movement == 0 && limit == 0
				&& flags == 0 && nodes.empty() && totalNodes == 0 && arrivalTurns == -1 && arrivalRemains == -1;
		if(heroId < 0 || !thorMapTileValid(start, width, height, levels))
			return false;
		const bool preview = state == ThorRouteState::PREVIEW || state == ThorRouteState::CHANGED;
		const bool committed = state == ThorRouteState::COMMITTED || state == ThorRouteState::READY_NEXT_TURN
			|| state == ThorRouteState::PAUSED || state == ThorRouteState::INTERRUPTED;
		if(((flags & THOR_ROUTE_CAN_CONFIRM) && !preview)
			|| ((flags & THOR_ROUTE_CAN_CANCEL) && !preview && state != ThorRouteState::UNREACHABLE)
			|| ((flags & (THOR_ROUTE_CAN_CLEAR | THOR_ROUTE_CAN_CONTINUE)) && !committed)
			|| ((flags & THOR_ROUTE_CAN_STOP) && state != ThorRouteState::MOVING))
			return false;
		if(nodes.empty())
		{
			if(totalNodes != 0 || arrivalTurns != -1 || arrivalRemains != -1
				|| (flags & (THOR_ROUTE_CAN_CONFIRM | THOR_ROUTE_CAN_CONTINUE | THOR_ROUTE_TRUNCATED)))
				return false;
			if(state == ThorRouteState::IDLE)
				return destination == absent && flags == 0;
			if(state != ThorRouteState::UNREACHABLE && state != ThorRouteState::FINISHED
				&& state != ThorRouteState::INTERRUPTED && state != ThorRouteState::CHANGED
				&& state != ThorRouteState::MOVING && state != ThorRouteState::STOPPING)
				return false;
			return destination == absent || thorMapTileValid(destination, width, height, levels);
		}
		if(state == ThorRouteState::IDLE || state == ThorRouteState::UNREACHABLE
			|| !thorMapTileValid(destination, width, height, levels) || nodes.size() > THOR_ROUTE_MAX_NODES
			|| totalNodes < 2 || totalNodes > width * height * levels * EPathfindingLayer::NUM_LAYERS
			|| static_cast<std::size_t>(totalNodes) < nodes.size()
			|| arrivalTurns < 0 || arrivalTurns >= 255 || arrivalRemains < 0
			|| nodes.front().tile != start || nodes.front().turns != 0)
			return false;
		const bool truncated = static_cast<std::size_t>(totalNodes) > nodes.size();
		if(truncated != static_cast<bool>(flags & THOR_ROUTE_TRUNCATED)
			|| (truncated && nodes.size() != THOR_ROUTE_MAX_NODES))
			return false;
		int previousTurns = 0;
		for(const auto & node : nodes)
		{
			if(!thorMapTileValid(node.tile, width, height, levels) || node.turns < previousTurns
				|| node.turns >= 255 || node.moveRemains < 0 || node.layer < EPathfindingLayer::LAND
				|| node.layer >= EPathfindingLayer::NUM_LAYERS || node.action < 0 || node.action > 9)
				return false;
			previousTurns = node.turns;
		}
		if(nodes.back().turns > arrivalTurns)
			return false;
		return truncated || (nodes.back().tile == destination && nodes.back().turns == arrivalTurns
			&& nodes.back().moveRemains == arrivalRemains);
	}

	bool commandAllowed(int target) const
	{
		if(!valid())
			return false;
		switch(static_cast<ThorRouteCommand>(target))
		{
			case ThorRouteCommand::CONFIRM: return flags & THOR_ROUTE_CAN_CONFIRM;
			case ThorRouteCommand::CANCEL_PREVIEW: return flags & THOR_ROUTE_CAN_CANCEL;
			case ThorRouteCommand::CLEAR_ROUTE: return flags & THOR_ROUTE_CAN_CLEAR;
			case ThorRouteCommand::STOP: return flags & THOR_ROUTE_CAN_STOP;
			default: return false;
		}
	}

	bool operator==(const ThorAdventureRoute &) const = default;
};

// CGPath stores destination first. Compare semantic path data, never transient
// predecessor, heap or generation fields. Confirmation rechecks the entire path,
// including nodes omitted from the bounded display prefix.
template<typename Path>
bool thorRoutePathsEqual(const Path & lhs, const Path & rhs)
{
	if(lhs.nodes.size() != rhs.nodes.size())
		return false;
	for(std::size_t index = 0; index < lhs.nodes.size(); ++index)
	{
		const auto & a = lhs.nodes[index];
		const auto & b = rhs.nodes[index];
		if(a.coord != b.coord || a.layer != b.layer || a.turns != b.turns || a.moveRemains != b.moveRemains
			|| a.action != b.action || a.accessible != b.accessible || a.cost != b.cost)
			return false;
	}
	return true;
}

template<typename Path>
bool thorRouteSetPath(ThorAdventureRoute & route, const Path & path)
{
	route.nodes.clear();
	route.totalNodes = 0;
	route.arrivalTurns = -1;
	route.arrivalRemains = -1;
	route.flags &= ~THOR_ROUTE_TRUNCATED;
	if(!thorMapDimensionsValid(route.width, route.height, route.levels) || path.nodes.size() < 2
		|| path.nodes.size() > static_cast<std::size_t>(route.width * route.height * route.levels * EPathfindingLayer::NUM_LAYERS))
		return false;
	// Validate even the omitted tail, so truncation never hides malformed metadata.
	int previousTurns = 0;
	for(auto it = path.nodes.rbegin(); it != path.nodes.rend(); ++it)
	{
		if(!thorMapTileValid({it->coord.x, it->coord.y, it->coord.z}, route.width, route.height, route.levels)
			|| it->turns < previousTurns || it->turns >= 255 || it->moveRemains < 0
			|| it->layer.getNum() < EPathfindingLayer::LAND || it->layer.getNum() >= EPathfindingLayer::NUM_LAYERS
			|| static_cast<int>(it->action) < 0 || static_cast<int>(it->action) > 9)
			return false;
		previousTurns = it->turns;
	}
	if(path.nodes.back().turns != 0)
		return false;
	route.start = {path.nodes.back().coord.x, path.nodes.back().coord.y, path.nodes.back().coord.z};
	route.destination = {path.nodes.front().coord.x, path.nodes.front().coord.y, path.nodes.front().coord.z};
	route.arrivalTurns = path.nodes.front().turns;
	route.arrivalRemains = path.nodes.front().moveRemains;
	route.totalNodes = static_cast<int>(path.nodes.size());
	route.nodes.reserve(std::min<std::size_t>(path.nodes.size(), THOR_ROUTE_MAX_NODES));
	for(auto it = path.nodes.rbegin(); it != path.nodes.rend() && route.nodes.size() < THOR_ROUTE_MAX_NODES; ++it)
		route.nodes.push_back({{it->coord.x, it->coord.y, it->coord.z}, it->turns, it->moveRemains,
			it->layer.getNum(), static_cast<int>(it->action)});
	if(path.nodes.size() > THOR_ROUTE_MAX_NODES)
		route.flags |= THOR_ROUTE_TRUNCATED;
	return true;
}
