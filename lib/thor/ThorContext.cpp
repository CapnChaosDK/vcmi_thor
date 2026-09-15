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
	return ThorContextIds::UNKNOWN;
}
