/*
 * CExchangeController.cpp, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 *
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 *
 */
#include "StdInc.h"
#include "CExchangeController.h"

#include "../CPlayerInterface.h"
#include "../GameInstance.h"

#include "CGarrisonInt.h"

#include "../../lib/callback/CCallback.h"
#include "../../lib/mapObjects/CGHeroInstance.h"
#include "../../lib/mapObjects/army/CStackInstance.h"

CExchangeController::CExchangeController(ObjectInstanceID hero1, ObjectInstanceID hero2)
	: left(GAME->interface()->cb->getHero(hero1))
	, right(GAME->interface()->cb->getHero(hero2))
{
}

void CExchangeController::swapArmy()
{
	const auto & leftSlots = left->Slots();
	const auto & rightSlots = right->Slots();

	auto leftIt = leftSlots.begin();
	auto rightIt = rightSlots.begin();

	// Swap slots that are full in both armies
	// [A] [B] => [B] [A]
	for (SlotID slotID(0); slotID < GameConstants::ARMY_SIZE; ++slotID)
	{
		if (left->hasStackAtSlot(slotID) && right->hasStackAtSlot(slotID))
			GAME->interface()->cb->swapCreatures(left, right, slotID, slotID);
	}

	// Swap pairs of stacks in different slots and correct their positions
	// [A] [ ]    [B] [ ]    [ ] [A]
	//         =>         =>
	// [ ] [B]    [ ] [A]    [B] [ ]
	for (;;)
	{
		while (leftIt != leftSlots.end() && right->hasStackAtSlot(leftIt->first))
			leftIt++;

		while (rightIt != rightSlots.end() && left->hasStackAtSlot(rightIt->first))
			rightIt++;

		if (leftIt == leftSlots.end() || rightIt == rightSlots.end())
			break;

		GAME->interface()->cb->swapCreatures(left, right, leftIt->first, rightIt->first);

		GAME->interface()->cb->swapCreatures(left, left, leftIt->first, rightIt->first);
		GAME->interface()->cb->swapCreatures(right, right, rightIt->first, leftIt->first);

		leftIt++;
		rightIt++;
	}

	// Move remaining unpaired stacks (if armies size is different)
	// [A] [ ] => [ ] [A]
	for(; leftIt != leftSlots.end(); leftIt++)
		if (!right->hasStackAtSlot(leftIt->first))
			GAME->interface()->cb->swapCreatures(left, right, leftIt->first, leftIt->first);

	for(; rightIt != rightSlots.end(); rightIt++)
		if (!left->hasStackAtSlot(rightIt->first))
			GAME->interface()->cb->swapCreatures(left, right, rightIt->first, rightIt->first);
}

void CExchangeController::moveArmy(bool leftToRight, std::optional<SlotID> heldSlot)
{
	const auto source = leftToRight ? left : right;
	const auto target = leftToRight ? right : left;

	if(!heldSlot.has_value())
	{
		const auto & weakestSlot = vstd::minElementByFun(source->Slots(),
			[](const auto & s) -> int
			{
				return s.second->getCreatureID().toCreature()->getAIValue();
			});
		heldSlot = weakestSlot->first;
	}
	
	if (source->getCreature(heldSlot.value()) == nullptr)
		return;

	GAME->interface()->cb->bulkMoveArmy(source->id, target->id, heldSlot.value());
}

bool CExchangeController::canMoveStack(bool leftToRight, SlotID sourceSlot) const
{
	const auto source = leftToRight ? left : right;
	const auto target = leftToRight ? right : left;
	if(!source || !target || !sourceSlot.validSlot())
		return false;
	const auto * creature = source->getCreature(sourceSlot);
	if(!creature)
		return false;
	const auto targetSlot = target->getSlotFor(creature);
	return targetSlot.validSlot() && canTransferStack(leftToRight, sourceSlot, !leftToRight, targetSlot);
}

void CExchangeController::moveStack(bool leftToRight, SlotID sourceSlot)
{
	const auto source = leftToRight ? left : right;
	const auto target = leftToRight ? right : left;
	auto creature = source->getCreature(sourceSlot);

	if(creature == nullptr)
		return;

	SlotID targetSlot = target->getSlotFor(creature);
	if(targetSlot.validSlot())
	{
		if(source->stacksCount() == 1 && source->needsLastStack())
		{
			GAME->interface()->cb->splitStack(source, target, sourceSlot, targetSlot,
				target->getStackCount(targetSlot) + source->getStackCount(sourceSlot) - 1);
		}
		else
		{
			GAME->interface()->cb->mergeOrSwapStacks(source, target, sourceSlot, targetSlot);
		}
	}
}

bool CExchangeController::canTransferStack(bool sourceLeft, SlotID sourceSlot, bool destinationLeft,
	SlotID destinationSlot) const
{
	const auto source = sourceLeft ? left : right;
	const auto destination = destinationLeft ? left : right;
	if(!source || !destination || !sourceSlot.validSlot() || !destinationSlot.validSlot()
		|| (sourceSlot == destinationSlot && source == destination))
		return false;
	const auto * sourceStack = source->getStackPtr(sourceSlot);
	if(!sourceStack)
		return false;
	const auto * destinationStack = destination->getStackPtr(destinationSlot);
	const auto sourceIsLastRequiredStack = source != destination && source->stacksCount() == 1 && source->needsLastStack();
	if(!destinationStack && sourceIsLastRequiredStack)
		return sourceStack->getCount() > 1;
	if(destinationStack && destinationStack->getCreature() == sourceStack->getCreature())
	{
		if(sourceIsLastRequiredStack)
			return sourceStack->getCount() > 1;
	}
	return true;
}

bool CExchangeController::transferStack(bool sourceLeft, SlotID sourceSlot, bool destinationLeft,
	SlotID destinationSlot)
{
	if(!canTransferStack(sourceLeft, sourceSlot, destinationLeft, destinationSlot))
		return false;
	const auto source = sourceLeft ? left : right;
	const auto destination = destinationLeft ? left : right;
	const auto * sourceStack = source->getStackPtr(sourceSlot);
	const auto * destinationStack = destination->getStackPtr(destinationSlot);
	const auto sourceIsLastRequiredStack = source != destination && source->stacksCount() == 1 && source->needsLastStack();
	if(!destinationStack && sourceIsLastRequiredStack)
	{
		const auto amount = sourceStack->getCount() - 1;
		GAME->interface()->cb->splitStack(source, destination, sourceSlot, destinationSlot, amount);
		return true;
	}
	if(destinationStack && destinationStack->getCreature() == sourceStack->getCreature())
	{
		if(sourceIsLastRequiredStack)
		{
			const auto amount = sourceStack->getCount() - 1 + destinationStack->getCount();
			GAME->interface()->cb->splitStack(source, destination, sourceSlot, destinationSlot, amount);
		}
		else
		{
			GAME->interface()->cb->mergeStacks(source, destination, sourceSlot, destinationSlot);
		}
		return true;
	}
	GAME->interface()->cb->swapCreatures(source, destination, sourceSlot, destinationSlot);
	return true;
}

bool CExchangeController::canSplitStackExact(bool sourceLeft, SlotID sourceSlot, bool destinationLeft,
	SlotID destinationSlot, int amount) const
{
	const auto source = sourceLeft ? left : right;
	const auto destination = destinationLeft ? left : right;
	if(!source || !destination || !sourceSlot.validSlot() || !destinationSlot.validSlot()
		|| (source == destination && sourceSlot == destinationSlot) || amount < 1)
		return false;
	const auto * sourceStack = source->getStackPtr(sourceSlot);
	const auto * destinationStack = destination->getStackPtr(destinationSlot);
	if(!sourceStack || sourceStack->getCount() <= amount
		|| (destinationStack && destinationStack->getCreature() != sourceStack->getCreature()))
		return false;
	const auto destinationCount = destinationStack ? destinationStack->getCount() : 0;
	return destinationCount <= std::numeric_limits<int>::max() - amount;
}

bool CExchangeController::splitStackExact(bool sourceLeft, SlotID sourceSlot, bool destinationLeft,
	SlotID destinationSlot, int amount)
{
	if(!canSplitStackExact(sourceLeft, sourceSlot, destinationLeft, destinationSlot, amount))
		return false;
	const auto source = sourceLeft ? left : right;
	const auto destination = destinationLeft ? left : right;
	const auto destinationCount = destination->getStackCount(destinationSlot);
	GAME->interface()->cb->splitStack(source, destination, sourceSlot, destinationSlot, destinationCount + amount);
	return true;
}

bool CExchangeController::canRedistributeStack(ObjectInstanceID sourceArmy, SlotID sourceSlot,
	CreatureID expectedCreature, int expectedSourceCount,
	const std::vector<ArmyStackRedistributionTarget> & destinations) const
{
	const auto sourceIsLeft = left && sourceArmy == left->id;
	const auto sourceIsRight = right && sourceArmy == right->id;
	if((!sourceIsLeft && !sourceIsRight) || !sourceSlot.validSlot() || expectedCreature == CreatureID::NONE
		|| expectedSourceCount < 2 || destinations.empty()
		|| destinations.size() > MAX_ARMY_STACK_REDISTRIBUTION_DESTINATIONS)
		return false;
	const auto source = sourceIsLeft ? left : right;
	const auto * sourceStack = source->getStackPtr(sourceSlot);
	if(!sourceStack || sourceStack->getCreatureID() != expectedCreature || sourceStack->getCount() != expectedSourceCount)
		return false;

	std::int64_t total = 0;
	for(std::size_t index = 0; index < destinations.size(); ++index)
	{
		const auto & target = destinations[index];
		const bool destinationIsLeft = left && target.armyId == left->id;
		const bool destinationIsRight = right && target.armyId == right->id;
		if((!destinationIsLeft && !destinationIsRight) || !target.slot.validSlot() || target.amount <= 0
			|| (target.armyId == sourceArmy && target.slot == sourceSlot))
			return false;
		for(std::size_t earlier = 0; earlier < index; ++earlier)
		{
			if(destinations[earlier].armyId == target.armyId && destinations[earlier].slot == target.slot)
				return false;
		}
		if(!canSplitStackExact(sourceIsLeft, sourceSlot, destinationIsLeft, target.slot, target.amount))
			return false;
		total += target.amount;
		if(total > static_cast<std::int64_t>(expectedSourceCount) - 1)
			return false;
	}
	return total > 0;
}

int CExchangeController::redistributeStack(ObjectInstanceID sourceArmy, SlotID sourceSlot,
	CreatureID expectedCreature, int expectedSourceCount,
	const std::vector<ArmyStackRedistributionTarget> & destinations)
{
	if(!canRedistributeStack(sourceArmy, sourceSlot, expectedCreature, expectedSourceCount, destinations))
		return -1;
	return GAME->interface()->cb->redistributeStack(left->id, right->id, sourceArmy, sourceSlot, expectedCreature,
		expectedSourceCount, destinations);
}

void CExchangeController::moveSingleStackCreature(bool leftToRight, SlotID sourceSlot, bool forceEmptySlotTarget)
{
	const auto source = leftToRight ? left : right;
	const auto target = leftToRight ? right : left;
	auto creature = source->getCreature(sourceSlot);

	if(creature == nullptr || source->stacksCount() == 1)
		return;

	SlotID targetSlot = forceEmptySlotTarget ? target->getFreeSlot() : target->getSlotFor(creature);
	if(targetSlot.validSlot())
	{
		GAME->interface()->cb->splitStack(source, target, sourceSlot, targetSlot, target->getStackCount(targetSlot) + 1);
	}
}

void CExchangeController::swapArtifacts(bool equipped, bool baclpack)
{
	GAME->interface()->cb->bulkMoveArtifacts(left->id, right->id, true, equipped, baclpack);
}

void CExchangeController::moveArtifacts(bool leftToRight, bool equipped, bool baclpack)
{
	const auto source = leftToRight ? left : right;
	const auto target = leftToRight ? right : left;

	GAME->interface()->cb->bulkMoveArtifacts(source->id, target->id, false, equipped, baclpack);
}
