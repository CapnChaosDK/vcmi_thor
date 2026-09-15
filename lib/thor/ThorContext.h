#pragma once

#include "../../Global.h"

#include <cstdint>
#include <mutex>
#include <string>

namespace ThorContextIds
{
	inline constexpr char UNKNOWN[] = "UNKNOWN";
	inline constexpr char MAIN_MENU[] = "MAIN_MENU";
	inline constexpr char MAIN_MENU_NEW_GAME[] = "MAIN_MENU_NEW_GAME";
	inline constexpr char MAIN_MENU_LOAD_GAME[] = "MAIN_MENU_LOAD_GAME";
	inline constexpr char MAIN_MENU_CAMPAIGN[] = "MAIN_MENU_CAMPAIGN";
	inline constexpr char MAIN_MENU_CREDITS[] = "MAIN_MENU_CREDITS";
}

/// Immutable, read-only context payload reserved for the Thor command deck.
struct DLL_LINKAGE ThorContextRecord
{
	std::uint64_t revision = 0;
	std::string contextId = ThorContextIds::UNKNOWN;
	std::string title;
	std::string status;
};

/// Thread-safe latest-record handoff. Consumers must discard revisions older than their last render.
class DLL_LINKAGE ThorContextStore final
{
	mutable std::mutex mutex;
	ThorContextRecord current;

public:
	ThorContextRecord snapshot() const;
	bool publish(ThorContextRecord next);
};

DLL_LINKAGE ThorContextStore & thorContextStore();
DLL_LINKAGE std::string thorContextIdForMainMenuTab(const std::string & tabName);
