#include "ThorAction.h"

#include <algorithm>

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
