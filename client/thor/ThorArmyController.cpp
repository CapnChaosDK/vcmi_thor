/*
 * ThorArmyController.cpp, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 *
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 *
 */
#include "StdInc.h"
#include "../thor/ThorRequestTracker.h"
#include "ThorArmyController.h"

#include "../CPlayerInterface.h"
#include "../GameInstance.h"

#include "../widgets/CExchangeController.h"

#include "../../lib/callback/CCallback.h"
#include "../../lib/mapObjects/CGHeroInstance.h"
#include "../../lib/mapObjects/army/CStackInstance.h"

ThorArmyController::ThorArmyController(ObjectInstanceID hero1, ObjectInstanceID hero2)
	: ThorArmyController(GAME->interface()->cb->getHero(hero1), GAME->interface()->cb->getHero(hero2))
{
}

ThorArmyController::ThorArmyController(const CArmedInstance * army1, const CArmedInstance * army2)
	: left(army1)
	, right(army2)
{
}

bool ThorArmyController::swapArmy()
{
	return captureThorRequest([&] { CExchangeController(left->id, right->id).swapArmy(); }) > 0;
}

void ThorArmyController::moveArmy(bool leftToRight, std::optional<SlotID> heldSlot)
{
	CExchangeController(left->id, right->id).moveArmy(leftToRight, heldSlot);
}

bool ThorArmyController::canMoveStack(bool leftToRight, SlotID sourceSlot) const
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

void ThorArmyController::moveStack(bool leftToRight, SlotID sourceSlot)
{
	CExchangeController(left->id, right->id).moveStack(leftToRight, sourceSlot);
}

bool ThorArmyController::canTransferStack(bool sourceLeft, SlotID sourceSlot, bool destinationLeft,
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
	if(source != destination && (source->isLocked()
		|| (destinationStack && destinationStack->getCreature() != sourceStack->getCreature() && destination->isLocked())))
		return false;
	const auto sourceIsLastRequiredStack = source != destination && source->stacksCount() == 1 && source->needsLastStack();
	if(!destinationStack && sourceIsLastRequiredStack)
		return sourceStack->getCount() > 1;
	if(destinationStack && destinationStack->getCreature() == sourceStack->getCreature())
	{
		if(sourceStack->getCount() > std::numeric_limits<int>::max() - destinationStack->getCount())
			return false;
		if(sourceIsLastRequiredStack)
			return sourceStack->getCount() > 1;
	}
	return true;
}

bool ThorArmyController::transferStack(bool sourceLeft, SlotID sourceSlot, bool destinationLeft,
	SlotID destinationSlot)
{
	return transferStackRequest(sourceLeft, sourceSlot, destinationLeft, destinationSlot) >= 0;
}

int ThorArmyController::transferStackRequest(bool sourceLeft, SlotID sourceSlot, bool destinationLeft,
	SlotID destinationSlot)
{
	if(!canTransferStack(sourceLeft, sourceSlot, destinationLeft, destinationSlot))
		return -1;
	const auto source = sourceLeft ? left : right;
	const auto destination = destinationLeft ? left : right;
	const auto * sourceStack = source->getStackPtr(sourceSlot);
	const auto * destinationStack = destination->getStackPtr(destinationSlot);
	const auto sourceIsLastRequiredStack = source != destination && source->stacksCount() == 1 && source->needsLastStack();
	if(!destinationStack && sourceIsLastRequiredStack)
	{
		const auto amount = sourceStack->getCount() - 1;
		return captureThorRequest([&] { GAME->interface()->cb->splitStack(source, destination, sourceSlot, destinationSlot, amount); });
	}
	if(destinationStack && destinationStack->getCreature() == sourceStack->getCreature())
	{
		if(sourceIsLastRequiredStack)
		{
			const auto amount = sourceStack->getCount() - 1 + destinationStack->getCount();
			return captureThorRequest([&] { GAME->interface()->cb->splitStack(source, destination, sourceSlot, destinationSlot, amount); });
		}
		return captureThorRequest([&] { GAME->interface()->cb->mergeStacks(source, destination, sourceSlot, destinationSlot); });
	}
	return captureThorRequest([&] { GAME->interface()->cb->swapCreatures(source, destination, sourceSlot, destinationSlot); });
}

bool ThorArmyController::canSplitStackExact(bool sourceLeft, SlotID sourceSlot, bool destinationLeft,
	SlotID destinationSlot, int amount) const
{
	const auto source = sourceLeft ? left : right;
	const auto destination = destinationLeft ? left : right;
	if(!source || !destination || !sourceSlot.validSlot() || !destinationSlot.validSlot()
		|| (source == destination && sourceSlot == destinationSlot) || amount < 1
		|| (source != destination && source->isLocked()))
		return false;
	const auto * sourceStack = source->getStackPtr(sourceSlot);
	const auto * destinationStack = destination->getStackPtr(destinationSlot);
	if(!sourceStack || sourceStack->getCount() <= amount
		|| (destinationStack && destinationStack->getCreature() != sourceStack->getCreature()))
		return false;
	const auto destinationCount = destinationStack ? destinationStack->getCount() : 0;
	return destinationCount <= std::numeric_limits<int>::max() - amount;
}

bool ThorArmyController::splitStackExact(bool sourceLeft, SlotID sourceSlot, bool destinationLeft,
	SlotID destinationSlot, int amount)
{
	if(!canSplitStackExact(sourceLeft, sourceSlot, destinationLeft, destinationSlot, amount))
		return false;
	const auto source = sourceLeft ? left : right;
	const auto destination = destinationLeft ? left : right;
	const auto destinationCount = destination->getStackCount(destinationSlot);
	return captureThorRequest([&] { GAME->interface()->cb->splitStack(source, destination, sourceSlot, destinationSlot, destinationCount + amount); }) > 0;
}

int ThorArmyController::swapArtifacts(bool equipped, bool backpack)
{
	return captureThorRequest([&] { CExchangeController(left->id, right->id).swapArtifacts(equipped, backpack); });
}

int ThorArmyController::moveArtifacts(bool leftToRight, bool equipped, bool backpack)
{
	return captureThorRequest([&] { CExchangeController(left->id, right->id).moveArtifacts(leftToRight, equipped, backpack); });
}
