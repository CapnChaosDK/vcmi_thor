#include "ThorAction.h"

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
	default:
		return std::nullopt;
	}
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
		|| action == ThorAction::END_TURN;
}

ThorActionValidation validateThorActionRequest(const ThorActionRequest & request, const ThorContextRecord & context)
{
	if(!thorActionFromId(static_cast<int>(request.action)))
		return ThorActionValidation::UNKNOWN_ACTION;
	if(request.revision != context.revision)
		return ThorActionValidation::STALE_REVISION;
	if(context.contextId != ThorContextIds::ADVENTURE_MAP || !isThorActionAllowedInAdventureMap(request.action))
		return ThorActionValidation::WRONG_CONTEXT;
	if((context.enabledActionMask & thorActionMask(request.action)) == 0)
		return ThorActionValidation::UNAVAILABLE;
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
