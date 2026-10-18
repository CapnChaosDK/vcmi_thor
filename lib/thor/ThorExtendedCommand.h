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
	AUTO_START = 1, AUTO_STOP = 2, CAST_SPELL = 3, RETREAT = 4, SURRENDER = 5
};

inline bool known(int command) { return command >= AUTO_START && command <= SURRENDER; }

/// V1 is exactly [version, command, target]. Battle commands require the no-target sentinel.
inline std::optional<int> decode(std::span<const int> payload)
{
	if(payload.size() != 3 || payload[0] != VERSION || !known(payload[1]) || payload[2] != -1)
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
				if(!known(list[i]) || std::find(list.begin(), list.begin() + i, list[i]) != list.begin() + i)
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
