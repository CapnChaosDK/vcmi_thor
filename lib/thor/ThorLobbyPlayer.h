#pragma once

#include "ThorExtendedCommand.h"
#include "../constants/EntityIdentifiers.h"
#include <string>
#include <array>
#include <vector>

static_assert(PlayerColor::PLAYER_LIMIT_I == 8);

/// Public lobby setup only. Slot identity is PlayerColor, never a display row index.
struct DLL_LINKAGE ThorLobbyPlayer
{
	int slot = -1;
	std::vector<int> nativeState; // option/connection identities, never transported
	int flags = 0; // select, faction, hero, bonus, human, local, pending
	std::array<std::string, 5> labels; // color, assignment, faction, hero, bonus
	bool operator==(const ThorLobbyPlayer &) const = default;
	bool valid() const { return slot >= 0 && slot < PlayerColor::PLAYER_LIMIT_I && flags >= 0 && flags < 128; }
	bool allows(int command) const
	{
		if(!valid() || (flags & 64) || !ThorExtended::playerCommand(command))
			return false;
		const int bit = command == ThorExtended::PLAYER_SELECT ? 1
			: command <= ThorExtended::FACTION_NEXT ? 2 : command <= ThorExtended::HERO_NEXT ? 4 : 8;
		return (flags & bit) != 0;
	}
};

/// Same permission assumptions as OptionsTab arrows and the player flag callback.
struct DLL_LINKAGE ThorLobbyPlayerRules
{
	bool supported = true, guest = false, local = false, human = false, canHuman = false;
	int humanCount = 0;
	bool factionChoice = false, heroChoice = false, pending = false;
	int flags() const
	{
		int result = (human ? 16 : 0) | (local ? 32 : 0) | (pending ? 64 : 0);
		if(!supported) return result;
		if(!guest && canHuman && (!human || humanCount > 1)) result |= 1;
		if(!guest || local) result |= (factionChoice ? 2 : 0) | (heroChoice ? 4 : 0) | 8;
		return result;
	}
};
