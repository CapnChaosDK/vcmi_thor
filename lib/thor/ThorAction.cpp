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
			|| action == ThorAction::HERO_MEETING_ARMY_RIGHT_TO_LEFT || action == ThorAction::HERO_MEETING_SWAP_ARMIES;
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
		if(!context.heroMeetingArmies || !context.heroMeetingArmies->locallyControllable || request.targetId != -1
			|| request.sourceSlot < 0
			|| request.sourceSlot >= static_cast<int>(THOR_HERO_MEETING_ARMY_SIZE) || request.destinationSlot < 0
			|| request.destinationSlot >= static_cast<int>(THOR_HERO_MEETING_ARMY_SIZE))
			return ThorActionValidation::INVALID_TARGET;
		const auto & armies = *context.heroMeetingArmies;
		const auto sourceIsLeft = request.sourceArmyId == armies.leftArmyId;
		const auto destinationIsLeft = request.destinationArmyId == armies.leftArmyId;
		if((!sourceIsLeft && request.sourceArmyId != armies.rightArmyId)
			|| (!destinationIsLeft && request.destinationArmyId != armies.rightArmyId)
			|| (request.sourceArmyId == request.destinationArmyId && request.sourceSlot == request.destinationSlot))
			return ThorActionValidation::INVALID_TARGET;
		const auto & source = (sourceIsLeft ? armies.leftSlots : armies.rightSlots)[request.sourceSlot];
		const auto & destination = (destinationIsLeft ? armies.leftSlots : armies.rightSlots)[request.destinationSlot];
		if(!source.occupied || source.armyId != request.sourceArmyId || destination.armyId != request.destinationArmyId)
			return ThorActionValidation::INVALID_TARGET;
	}
	if(request.action == ThorAction::HERO_MEETING_TRANSFER_STACK)
	{
		if(!context.heroMeetingArmies || !context.heroMeetingArmies->locallyControllable || request.sourceArmyId != -1
			|| request.sourceSlot != -1 || request.destinationArmyId != -1 || request.destinationSlot != -1)
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
