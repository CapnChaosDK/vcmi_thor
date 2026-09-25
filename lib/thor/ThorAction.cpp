#include "ThorAction.h"

#include <algorithm>

std::optional<int> encodeThorHeroMeetingTransferPair(int sourceKey, int destinationKey)
{
	constexpr int slotKeyCount = static_cast<int>(THOR_HERO_MEETING_SLOT_KEY_COUNT);
	if(sourceKey < 0 || sourceKey >= slotKeyCount || destinationKey < 0 || destinationKey >= slotKeyCount
		|| sourceKey / static_cast<int>(THOR_HERO_MEETING_ARMY_SIZE)
			== destinationKey / static_cast<int>(THOR_HERO_MEETING_ARMY_SIZE))
		return std::nullopt;
	return sourceKey * slotKeyCount + destinationKey;
}

std::optional<ThorHeroMeetingTransferPair> decodeThorHeroMeetingTransferPair(int encodedPair)
{
	constexpr int slotKeyCount = static_cast<int>(THOR_HERO_MEETING_SLOT_KEY_COUNT);
	if(encodedPair < 0 || encodedPair >= slotKeyCount * slotKeyCount)
		return std::nullopt;
	const auto sourceKey = encodedPair / slotKeyCount;
	const auto destinationKey = encodedPair % slotKeyCount;
	const auto verifiedPair = encodeThorHeroMeetingTransferPair(sourceKey, destinationKey);
	if(!verifiedPair || *verifiedPair != encodedPair)
		return std::nullopt;
	const auto slotsPerArmy = static_cast<int>(THOR_HERO_MEETING_ARMY_SIZE);
	return ThorHeroMeetingTransferPair{sourceKey, destinationKey, sourceKey < slotsPerArmy,
		sourceKey % slotsPerArmy, destinationKey < slotsPerArmy, destinationKey % slotsPerArmy};
}

bool canThorHeroMeetingMoveArmy(const ThorHeroMeetingArmies & armies, bool leftToRight)
{
	const auto & source = leftToRight ? armies.leftSlots : armies.rightSlots;
	const auto & destination = leftToRight ? armies.rightSlots : armies.leftSlots;
	const auto occupied = std::count_if(source.begin(), source.end(), [](const auto & slot) { return slot.occupied; });
	if(occupied == 0)
		return false;
	if(occupied == 1)
	{
		const auto onlyStack = std::find_if(source.begin(), source.end(), [](const auto & slot) { return slot.occupied; });
		if(onlyStack->count <= 1)
			return false;
	}

	const bool hasFreeDestination = std::any_of(destination.begin(), destination.end(),
		[](const auto & slot) { return !slot.occupied; });
	return std::any_of(source.begin(), source.end(), [&](const auto & stack)
	{
		return stack.occupied && (hasFreeDestination || std::any_of(destination.begin(), destination.end(),
			[&](const auto & target) { return target.occupied && target.creatureId == stack.creatureId; }));
	});
}

bool canThorHeroMeetingSplitStack(const ThorHeroMeetingArmies & armies, int sourceArmyId, int sourceSlot,
	int destinationArmyId, int destinationSlot, int amount)
{
	if(!armies.locallyControllable || sourceSlot < 0 || destinationSlot < 0
		|| sourceSlot >= static_cast<int>(THOR_HERO_MEETING_ARMY_SIZE)
		|| destinationSlot >= static_cast<int>(THOR_HERO_MEETING_ARMY_SIZE)
		|| (sourceArmyId != armies.leftArmyId && sourceArmyId != armies.rightArmyId)
		|| (destinationArmyId != armies.leftArmyId && destinationArmyId != armies.rightArmyId)
		|| (sourceArmyId == destinationArmyId && sourceSlot == destinationSlot) || amount < 1)
		return false;
	const auto & source = (sourceArmyId == armies.leftArmyId ? armies.leftSlots : armies.rightSlots)[sourceSlot];
	const auto & destination = (destinationArmyId == armies.leftArmyId ? armies.leftSlots : armies.rightSlots)[destinationSlot];
	return source.occupied && source.armyId == sourceArmyId && source.slot == sourceSlot && source.count > amount
		&& destination.armyId == destinationArmyId && destination.slot == destinationSlot
		&& (!destination.occupied || destination.creatureId == source.creatureId);
}

std::optional<ThorAction> thorActionFromId(int actionId)
{
	switch(actionId)
	{
	case static_cast<int>(ThorAction::OPEN_KINGDOM_OVERVIEW):
		return ThorAction::OPEN_KINGDOM_OVERVIEW;
	case static_cast<int>(ThorAction::OPEN_QUEST_LOG):
		return ThorAction::OPEN_QUEST_LOG;
	case static_cast<int>(ThorAction::OPEN_PUZZLE_MAP):
		return ThorAction::OPEN_PUZZLE_MAP;
	case static_cast<int>(ThorAction::OPEN_SAVE_GAME):
		return ThorAction::OPEN_SAVE_GAME;
	case static_cast<int>(ThorAction::NEXT_HERO):
		return ThorAction::NEXT_HERO;
	case static_cast<int>(ThorAction::MOVE_HERO):
		return ThorAction::MOVE_HERO;
	case static_cast<int>(ThorAction::TOGGLE_HERO_SLEEP):
		return ThorAction::TOGGLE_HERO_SLEEP;
	case static_cast<int>(ThorAction::END_TURN):
		return ThorAction::END_TURN;
	case static_cast<int>(ThorAction::BATTLE_WAIT):
		return ThorAction::BATTLE_WAIT;
	case static_cast<int>(ThorAction::BATTLE_DEFEND):
		return ThorAction::BATTLE_DEFEND;
	case static_cast<int>(ThorAction::BATTLE_TACTICS_NEXT):
		return ThorAction::BATTLE_TACTICS_NEXT;
	case static_cast<int>(ThorAction::BATTLE_TACTICS_END):
		return ThorAction::BATTLE_TACTICS_END;
	case static_cast<int>(ThorAction::SELECT_HERO):
		return ThorAction::SELECT_HERO;
	case static_cast<int>(ThorAction::SELECT_TOWN):
		return ThorAction::SELECT_TOWN;
	case static_cast<int>(ThorAction::HERO_MEETING_MOVE_STACK):
		return ThorAction::HERO_MEETING_MOVE_STACK;
	case static_cast<int>(ThorAction::HERO_MEETING_TRANSFER_STACK):
		return ThorAction::HERO_MEETING_TRANSFER_STACK;
	case static_cast<int>(ThorAction::HERO_MEETING_ARMY_LEFT_TO_RIGHT):
		return ThorAction::HERO_MEETING_ARMY_LEFT_TO_RIGHT;
	case static_cast<int>(ThorAction::HERO_MEETING_ARMY_RIGHT_TO_LEFT):
		return ThorAction::HERO_MEETING_ARMY_RIGHT_TO_LEFT;
	case static_cast<int>(ThorAction::HERO_MEETING_SWAP_ARMIES):
		return ThorAction::HERO_MEETING_SWAP_ARMIES;
	case static_cast<int>(ThorAction::HERO_MEETING_SPLIT_STACK):
		return ThorAction::HERO_MEETING_SPLIT_STACK;
	default:
		return std::nullopt;
	}
}

bool isThorActionAllowedInContext(ThorAction action, const std::string & contextId)
{
	if(contextId == ThorContextIds::ADVENTURE_MAP)
		return isThorActionAllowedInAdventureMap(action);
	if(contextId == ThorContextIds::BATTLE)
		return action == ThorAction::BATTLE_WAIT || action == ThorAction::BATTLE_DEFEND;
	if(contextId == ThorContextIds::BATTLE_TACTICS)
		return action == ThorAction::BATTLE_TACTICS_NEXT || action == ThorAction::BATTLE_TACTICS_END;
	if(contextId == ThorContextIds::HERO_MEETING)
		return action == ThorAction::HERO_MEETING_MOVE_STACK || action == ThorAction::HERO_MEETING_TRANSFER_STACK
			|| action == ThorAction::HERO_MEETING_ARMY_LEFT_TO_RIGHT
			|| action == ThorAction::HERO_MEETING_ARMY_RIGHT_TO_LEFT || action == ThorAction::HERO_MEETING_SWAP_ARMIES
			|| action == ThorAction::HERO_MEETING_SPLIT_STACK;
	return false;
}

bool isThorActionAllowedInAdventureMap(ThorAction action)
{
	return action == ThorAction::OPEN_KINGDOM_OVERVIEW
		|| action == ThorAction::OPEN_QUEST_LOG
		|| action == ThorAction::OPEN_PUZZLE_MAP
		|| action == ThorAction::OPEN_SAVE_GAME
		|| action == ThorAction::NEXT_HERO
		|| action == ThorAction::MOVE_HERO
		|| action == ThorAction::TOGGLE_HERO_SLEEP
		|| action == ThorAction::END_TURN
		|| action == ThorAction::SELECT_HERO
		|| action == ThorAction::SELECT_TOWN;
}

ThorActionValidation validateThorActionRequest(const ThorActionRequest & request, const ThorContextRecord & context)
{
	if(!thorActionFromId(static_cast<int>(request.action)))
		return ThorActionValidation::UNKNOWN_ACTION;
	if(request.revision != context.revision)
		return ThorActionValidation::STALE_REVISION;
	if(!isThorActionAllowedInContext(request.action, context.contextId))
		return ThorActionValidation::WRONG_CONTEXT;
	if((context.enabledActionMask & thorActionMask(request.action)) == 0)
		return ThorActionValidation::UNAVAILABLE;
	if(request.action == ThorAction::SELECT_HERO || request.action == ThorAction::SELECT_TOWN)
	{
		if(request.targetId < 0)
			return ThorActionValidation::INVALID_TARGET;
		const auto hasTarget = request.action == ThorAction::SELECT_HERO
			? std::any_of(context.heroes.begin(), context.heroes.end(), [&](const auto & hero) { return hero.id == request.targetId; })
			: std::any_of(context.towns.begin(), context.towns.end(), [&](const auto & town) { return town.id == request.targetId; });
		if(!hasTarget)
			return ThorActionValidation::INVALID_TARGET;
	}
	if(request.action == ThorAction::HERO_MEETING_MOVE_STACK)
	{
		if(!context.heroMeetingArmies || !context.heroMeetingArmies->locallyControllable || request.targetId < 0
			|| request.targetId >= static_cast<int>(THOR_HERO_MEETING_SLOT_KEY_COUNT)
			|| request.sourceArmyId != -1 || request.sourceSlot != -1
			|| request.destinationArmyId != -1 || request.destinationSlot != -1 || request.amount != -1)
			return ThorActionValidation::INVALID_TARGET;
		const auto & armies = *context.heroMeetingArmies;
		const auto slotsPerArmy = static_cast<int>(THOR_HERO_MEETING_ARMY_SIZE);
		const auto sourceIsLeft = request.targetId < slotsPerArmy;
		const auto sourceSlot = request.targetId % slotsPerArmy;
		const auto sourceArmyId = sourceIsLeft ? armies.leftArmyId : armies.rightArmyId;
		const auto & source = (sourceIsLeft ? armies.leftSlots : armies.rightSlots)[sourceSlot];
		if(!source.occupied || source.armyId != sourceArmyId || source.slot != sourceSlot)
			return ThorActionValidation::INVALID_TARGET;
	}
	if(request.action == ThorAction::HERO_MEETING_TRANSFER_STACK)
	{
		if(!context.heroMeetingArmies || !context.heroMeetingArmies->locallyControllable || request.sourceArmyId != -1
			|| request.sourceSlot != -1 || request.destinationArmyId != -1 || request.destinationSlot != -1
			|| request.amount != -1)
			return ThorActionValidation::INVALID_TARGET;
		const auto pair = decodeThorHeroMeetingTransferPair(request.targetId);
		if(!pair)
			return ThorActionValidation::INVALID_TARGET;
		const auto & armies = *context.heroMeetingArmies;
		const auto & source = (pair->sourceIsLeft ? armies.leftSlots : armies.rightSlots)[pair->sourceSlot];
		const auto & destination = (pair->destinationIsLeft ? armies.leftSlots : armies.rightSlots)[pair->destinationSlot];
		const int sourceArmyId = pair->sourceIsLeft ? armies.leftArmyId : armies.rightArmyId;
		const int destinationArmyId = pair->destinationIsLeft ? armies.leftArmyId : armies.rightArmyId;
		if(!source.occupied || source.armyId != sourceArmyId || source.slot != pair->sourceSlot
			|| destination.armyId != destinationArmyId || destination.slot != pair->destinationSlot)
			return ThorActionValidation::INVALID_TARGET;
	}
	if(request.action == ThorAction::HERO_MEETING_SPLIT_STACK)
	{
		if(request.targetId != -1 || !context.heroMeetingArmies
			|| !canThorHeroMeetingSplitStack(*context.heroMeetingArmies, request.sourceArmyId, request.sourceSlot,
				request.destinationArmyId, request.destinationSlot, request.amount))
			return ThorActionValidation::INVALID_TARGET;
	}
	if((request.action == ThorAction::HERO_MEETING_ARMY_LEFT_TO_RIGHT || request.action == ThorAction::HERO_MEETING_ARMY_RIGHT_TO_LEFT
		|| request.action == ThorAction::HERO_MEETING_SWAP_ARMIES)
		&& (!context.heroMeetingArmies || !context.heroMeetingArmies->locallyControllable))
		return ThorActionValidation::INVALID_TARGET;
	return ThorActionValidation::VALID;
}

bool ThorActionQueue::submit(ThorActionRequest request)
{
	std::lock_guard lock(mutex);
	if(count == capacity)
		return false;

	requests[(first + count) % capacity] = request;
	++count;
	return true;
}

std::optional<ThorActionRequest> ThorActionQueue::pop()
{
	std::lock_guard lock(mutex);
	if(count == 0)
		return std::nullopt;

	auto result = requests[first];
	first = (first + 1) % capacity;
	--count;
	return result;
}

void ThorActionQueue::clear()
{
	std::lock_guard lock(mutex);
	first = 0;
	count = 0;
}

std::size_t ThorActionQueue::size() const
{
	std::lock_guard lock(mutex);
	return count;
}

ThorActionQueue & thorActionQueue()
{
	static ThorActionQueue queue;
	return queue;
}
