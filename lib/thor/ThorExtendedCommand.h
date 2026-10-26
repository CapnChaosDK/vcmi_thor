#pragma once

#include "../../Global.h"

#include <algorithm>
#include <array>
#include <optional>
#include <span>
#include <vector>

/// Separate from ThorAction IDs and their immutable 64-bit mask.
namespace ThorExtended
{
inline constexpr int VERSION = 1;
inline constexpr std::size_t MAX_AVAILABLE = 16;
enum Command : int
{
	AUTO_START = 1, AUTO_STOP = 2, CAST_SPELL = 3, RETREAT = 4, SURRENDER = 5,
	PLAYER_SELECT = 6, FACTION_PREVIOUS = 7, FACTION_NEXT = 8,
	HERO_PREVIOUS = 9, HERO_NEXT = 10, BONUS_PREVIOUS = 11, BONUS_NEXT = 12,
	MARKET_SELL = 13, MARKET_BUY = 14, MARKET_AMOUNT = 15,
	MARKET_TRADE = 16, MARKET_CLOSE = 17, MARKET_RESET = 18
};

inline bool known(int command) { return command >= AUTO_START && command <= MARKET_RESET; }
inline bool marketCommand(int command) { return command >= MARKET_SELL && command <= MARKET_RESET; }

inline bool playerCommand(int command) { return command >= PLAYER_SELECT && command <= BONUS_NEXT; }
inline bool validTarget(int command, int target)
{
	if(command == MARKET_SELL || command == MARKET_BUY || command == MARKET_AMOUNT)
		return target >= 0;
	return known(command) && (playerCommand(command) ? target >= 0 && target < 8 : target == -1);
}

/// V1 is exactly [version, command, target]. Battle commands require the no-target sentinel.
inline std::optional<int> decode(std::span<const int> payload)
{
	if(payload.size() != 3 || payload[0] != VERSION || !validTarget(payload[1], payload[2]))
		return std::nullopt;
	return payload[1];
}

struct DLL_LINKAGE State
{
	int version = VERSION;
	std::vector<int> enabled;
	std::vector<int> active;
	bool operator==(const State &) const = default;
	bool valid() const
	{
		auto validList = [](const auto & list)
		{
			if(list.size() > MAX_AVAILABLE)
				return false;
			for(std::size_t i = 0; i < list.size(); ++i)
				if(!known(list[i]) || playerCommand(list[i]) || marketCommand(list[i]) || std::find(list.begin(), list.begin() + i, list[i]) != list.begin() + i)
					return false;
			return true;
		};
		return version == VERSION && validList(enabled) && validList(active);
	}
	bool allows(int command) const
	{
		return valid() && known(command) && std::find(enabled.begin(), enabled.end(), command) != enabled.end();
	}
};

}
