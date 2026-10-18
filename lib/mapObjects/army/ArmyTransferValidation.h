/*
 * ArmyTransferValidation.h, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 *
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 */
#pragma once

#include "CArmedInstance.h"
#include "../../networkPacks/PacksForClient.h"

#include <algorithm>
#include <array>
#include <cstdint>
#include <map>
#include <limits>
#include <span>

/// Simulate the complete ordered packet before any stack or artifact is changed.
/// Authorization belongs to the server; this shared boundary checks state integrity.
template<typename ResolveArmy>
bool isValidArmyTransferPlan(std::span<const RebalanceStacks> moves, ResolveArmy resolveArmy)
{
	struct Stack
	{
		const CCreature * creature = nullptr;
		int64_t count = 0;
	};
	struct Army
	{
		std::array<Stack, GameConstants::ARMY_SIZE> stacks;
		bool needsLastStack = false;
	};
	std::map<ObjectInstanceID, Army> armies;
	const auto load = [&](ObjectInstanceID id)
	{
		if(armies.count(id))
			return true;
		const auto * source = resolveArmy(id);
		if(!source)
			return false;
		Army army;
		army.needsLastStack = source->needsLastStack() && source->stacksCount() != 0;
		for(const auto & [slot, stack] : source->Slots())
		{
			if(!slot.validSlot() || !stack->getCreature() || stack->getCount() <= 0)
				return false;
			army.stacks[slot.getNum()] = {stack->getCreature(), stack->getCount()};
		}
		armies.emplace(id, std::move(army));
		return true;
	};
	if(moves.empty())
		return false;
	for(const auto & move : moves)
	{
		if(!move.srcSlot.validSlot() || !move.dstSlot.validSlot() || move.count <= 0
			|| (move.srcArmy == move.dstArmy && move.srcSlot == move.dstSlot)
			|| !load(move.srcArmy) || !load(move.dstArmy))
			return false;
		auto & source = armies.at(move.srcArmy).stacks[move.srcSlot.getNum()];
		auto & destination = armies.at(move.dstArmy).stacks[move.dstSlot.getNum()];
		if(!source.creature || source.count < move.count
			|| (destination.count && destination.creature != source.creature)
			|| destination.count + move.count > std::numeric_limits<si32>::max())
			return false;
		destination.creature = source.creature;
		destination.count += move.count;
		source.count -= move.count;
		if(!source.count)
			source.creature = nullptr;
	}
	for(const auto & [id, army] : armies)
	{
		if(army.needsLastStack && std::none_of(army.stacks.begin(), army.stacks.end(), [](const Stack & stack)
		{
			return stack.count > 0;
		}))
			return false;
	}
	return true;
}
