#include "ThorContext.h"

#include <utility>

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
	next.revision = current.revision + 1;
	if(next.contextId.empty())
		next.contextId = ThorContextIds::UNKNOWN;
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
	default:
		return ThorContextIds::UNKNOWN;
	}
}
