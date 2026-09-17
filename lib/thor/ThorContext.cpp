#include "ThorContext.h"

#include <utility>

namespace
{
	bool sameSemanticState(const ThorContextRecord & lhs, const ThorContextRecord & rhs)
	{
		return lhs.contextId == rhs.contextId
			&& lhs.title == rhs.title
			&& lhs.status == rhs.status
			&& lhs.enabledActionMask == rhs.enabledActionMask;
	}
}

ThorContextRecord ThorContextStore::snapshot() const
{
	std::lock_guard lock(mutex);
	return current;
}

bool ThorContextStore::publish(ThorContextRecord next)
{
	std::lock_guard lock(mutex);
	if(next.revision <= current.revision)
		return false;
	if(next.contextId.empty())
		next.contextId = ThorContextIds::UNKNOWN;
	current = std::move(next);
	return true;
}

ThorContextRecord ThorContextStore::publishNext(ThorContextRecord next)
{
	std::lock_guard lock(mutex);
	if(next.contextId.empty())
		next.contextId = ThorContextIds::UNKNOWN;
	if(sameSemanticState(current, next))
		return current;
	next.revision = current.revision + 1;
	current = std::move(next);
	return current;
}

ThorContextStore & thorContextStore()
{
	static ThorContextStore store;
	return store;
}

std::string thorContextIdForMainMenuTab(const std::string & tabName)
{
	if(tabName == "main")
		return ThorContextIds::MAIN_MENU;
	if(tabName == "new")
		return ThorContextIds::MAIN_MENU_NEW_GAME;
	if(tabName == "load")
		return ThorContextIds::MAIN_MENU_LOAD_GAME;
	if(tabName == "campaign")
		return ThorContextIds::MAIN_MENU_CAMPAIGN;
	if(tabName == "credits")
		return ThorContextIds::MAIN_MENU_CREDITS;
	return ThorContextIds::UNKNOWN;
}

std::string thorContextIdForLobby(ThorLobbyMode mode, ThorLobbyTab tab)
{
	switch(mode)
	{
	case ThorLobbyMode::NEW_GAME:
		switch(tab)
		{
		case ThorLobbyTab::NONE:
			return ThorContextIds::LOBBY_NEW_GAME;
		case ThorLobbyTab::SCENARIO:
			return ThorContextIds::LOBBY_NEW_GAME_SCENARIO;
		case ThorLobbyTab::OPTIONS:
			return ThorContextIds::LOBBY_NEW_GAME_OPTIONS;
		case ThorLobbyTab::RANDOM_MAP:
			return ThorContextIds::LOBBY_NEW_GAME_RANDOM_MAP;
		case ThorLobbyTab::TURN_OPTIONS:
			return ThorContextIds::LOBBY_NEW_GAME_TURN_OPTIONS;
		case ThorLobbyTab::EXTRA_OPTIONS:
			return ThorContextIds::LOBBY_NEW_GAME_EXTRA_OPTIONS;
		case ThorLobbyTab::BATTLE_MODE:
			return ThorContextIds::LOBBY_NEW_GAME_BATTLE_MODE;
		default:
			return ThorContextIds::UNKNOWN;
		}
	case ThorLobbyMode::LOAD_GAME:
		switch(tab)
		{
		case ThorLobbyTab::NONE:
			return ThorContextIds::LOBBY_LOAD_GAME;
		case ThorLobbyTab::SCENARIO:
			return ThorContextIds::LOBBY_LOAD_GAME_SCENARIO;
		case ThorLobbyTab::OPTIONS:
			return ThorContextIds::LOBBY_LOAD_GAME_OPTIONS;
		case ThorLobbyTab::TURN_OPTIONS:
			return ThorContextIds::LOBBY_LOAD_GAME_TURN_OPTIONS;
		case ThorLobbyTab::EXTRA_OPTIONS:
			return ThorContextIds::LOBBY_LOAD_GAME_EXTRA_OPTIONS;
		default:
			return ThorContextIds::UNKNOWN;
		}
	case ThorLobbyMode::CAMPAIGN_LIST:
		return tab == ThorLobbyTab::SCENARIO ? ThorContextIds::LOBBY_CAMPAIGN_LIST : ThorContextIds::UNKNOWN;
	default:
		return ThorContextIds::UNKNOWN;
	}
}

std::string thorContextIdForInGameContext(ThorInGameContext context)
{
	switch(context)
	{
	case ThorInGameContext::ADVENTURE_MAP:
		return ThorContextIds::ADVENTURE_MAP;
	case ThorInGameContext::HERO_WINDOW:
		return ThorContextIds::HERO_WINDOW;
	case ThorInGameContext::TOWN_WINDOW:
		return ThorContextIds::TOWN_WINDOW;
	case ThorInGameContext::HERO_MEETING:
		return ThorContextIds::HERO_MEETING;
	case ThorInGameContext::BATTLE:
		return ThorContextIds::BATTLE;
	case ThorInGameContext::BATTLE_TACTICS:
		return ThorContextIds::BATTLE_TACTICS;
	case ThorInGameContext::BATTLE_RESULT:
		return ThorContextIds::BATTLE_RESULT;
	case ThorInGameContext::KINGDOM_OVERVIEW:
		return ThorContextIds::KINGDOM_OVERVIEW;
	case ThorInGameContext::QUEST_LOG:
		return ThorContextIds::QUEST_LOG;
	case ThorInGameContext::SCENARIO_EVENT_JOURNAL:
		return ThorContextIds::SCENARIO_EVENT_JOURNAL;
	case ThorInGameContext::PUZZLE_MAP:
		return ThorContextIds::PUZZLE_MAP;
	case ThorInGameContext::SAVE_GAME:
		return ThorContextIds::SAVE_GAME;
	default:
		return ThorContextIds::UNKNOWN;
	}
}
