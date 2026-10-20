/*
 * BattleControllerInput.h, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 */
#pragma once

#include "../../lib/constants/EntityIdentifiers.h"
#include "../../lib/battle/BattleHex.h"
#include "../../lib/battle/PossiblePlayerBattleAction.h"
#include "../../lib/Point.h"
#include "../gui/Shortcut.h"

/// Input intent only: legality, previews and submission remain in BattleActionsController.
namespace BattleControllerInput
{
inline BattleHex initialHex(BattleHex actor)
{
	return actor.isAvailable() ? actor : BattleHex(GameConstants::BFIELD_WIDTH / 2, GameConstants::BFIELD_HEIGHT / 2);
}

inline BattleHex adjacentHex(BattleHex hex, const std::array<double, 2> & axis)
{
	if(!hex.isValid() || (axis[0] == 0 && axis[1] == 0))
		return hex;
	const std::array<Point, 6> directions = {{{-22, -42}, {22, -42}, {44, 0}, {22, 42}, {-22, 42}, {-44, 0}}};
	size_t selected = 0;
	double best = std::numeric_limits<double>::lowest();
	for(size_t index = 0; index < directions.size(); ++index)
	{
		const auto & delta = directions[index];
		const double alignment = (delta.x * axis[0] + delta.y * axis[1]) / delta.length();
		if(alignment > best)
		{
			best = alignment;
			selected = index;
		}
	}
	try
	{
		const auto target = hex.cloneInDirection(static_cast<BattleHex::EDir>(selected));
		return target.isAvailable() ? target : hex;
	}
	catch(const std::out_of_range &)
	{
		return hex; // never wrap rows or slide along an outward boundary
	}
}

/// The caller supplies current live units, their native geometry and eligibility.
template<typename Range, typename Center, typename Eligible>
auto unitInDirection(const Range & units, const Point & origin, const std::array<double, 2> & axis,
	Center center, Eligible eligible) -> typename Range::value_type
{
	typename Range::value_type selected = nullptr;
	std::tuple<int, double, uint32_t> selectedRank;
	const double axisLengthSquared = axis[0] * axis[0] + axis[1] * axis[1];
	for(const auto unit : units)
	{
		if(!eligible(unit))
			continue;
		const Point delta = center(unit) - origin;
		const int distance = delta.lengthSquared();
		const double dot = delta.x * axis[0] + delta.y * axis[1];
		if(dot <= 0 || distance == 0)
			continue;
		const double alignment = dot * dot / (distance * axisLengthSquared);
		if(alignment < 0.5)
			continue;
		const auto rank = std::make_tuple(distance, -alignment, unit->unitId());
		if(!selected || rank < selectedRank)
		{
			selected = unit;
			selectedRank = rank;
		}
	}
	return selected;
}

inline int nextAttackDirection(const std::array<Point, 8> & points, BattleHex::EDir current, int step)
{
	const std::array<int, 8> clockwise = {0, 6, 1, 2, 3, 7, 4, 5};
	auto index = static_cast<int>(std::find(clockwise.begin(), clockwise.end(), current) - clockwise.begin());
	for(size_t count = 0; count < clockwise.size(); ++count)
	{
		index = (index + step + 8) % 8;
		if(points[clockwise[index]].isValid())
			return clockwise[index];
	}
	return -1;
}

inline bool capturesShortcut(EShortcut key)
{
	// Direction aliases must still reach native Wait/Defend/tactics handlers
	// when no melee approach is being selected.
	return key == EShortcut::GLOBAL_ACCEPT || key == EShortcut::GLOBAL_CANCEL || key == EShortcut::BATTLE_TOGGLE_CURSOR_MODE;
}

/// Resolve context before dispatch; native gameplay buttons never synthesize clicks.
inline bool translateShortcuts(std::vector<EShortcut> & shortcuts, bool native, bool melee, bool tactics = false)
{
	const auto contains = [&](EShortcut key) { return std::find(shortcuts.begin(), shortcuts.end(), key) != shortcuts.end(); };
	if(contains(EShortcut::BATTLE_TOGGLE_CURSOR_MODE))
		shortcuts = {EShortcut::BATTLE_TOGGLE_CURSOR_MODE};
	else if(!native)
		return false;
	else if(melee && contains(EShortcut::BATTLE_PREVIOUS_ATTACK_DIRECTION))
		shortcuts = {EShortcut::BATTLE_PREVIOUS_ATTACK_DIRECTION};
	else if(melee && contains(EShortcut::BATTLE_NEXT_ATTACK_DIRECTION))
		shortcuts = {EShortcut::BATTLE_NEXT_ATTACK_DIRECTION};
	else if(contains(EShortcut::GLOBAL_ACCEPT))
		shortcuts = {EShortcut::GLOBAL_ACCEPT};
	else if(contains(EShortcut::GLOBAL_CANCEL))
		shortcuts = {EShortcut::GLOBAL_CANCEL};
	else if(!tactics && contains(EShortcut::BATTLE_END_WITH_AUTOCOMBAT))
		shortcuts = {EShortcut::BATTLE_AUTOCOMBAT};
	else if(contains(EShortcut::MOUSE_LEFT))
		shortcuts = {EShortcut::BATTLE_DEFEND};
	else if(contains(EShortcut::MOUSE_RIGHT))
		shortcuts = {EShortcut::BATTLE_WAIT};
	else
		return false;
	return true;
}

struct Selection
{
	uint32_t actor;
	BattleHex hex;
	std::optional<uint32_t> target;
	BattleHex::EDir direction;
	PossiblePlayerBattleAction action;
	bool heroSpell;
	bool creatureSpell;
	bool operator==(const Selection &) const = default;
};

/// Never retains a unit pointer. Release consumes the press even when state changed.
class Confirmation
{
	std::optional<Selection> pending;
public:
	bool cancel() { return std::exchange(pending, std::nullopt).has_value(); }
	void press(std::optional<Selection> selection) { pending = std::move(selection); }
	bool release(const std::optional<Selection> & current)
	{
		auto pressed = std::exchange(pending, std::nullopt);
		return pressed && current && *pressed == *current;
	}
};
}
