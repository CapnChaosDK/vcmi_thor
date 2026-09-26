#pragma once

#include "../../constants/EntityIdentifiers.h"

#include <array>
#include <cstddef>

constexpr std::size_t MAX_ARMY_STACK_REDISTRIBUTION_DESTINATIONS = 13;

/// One destination used by an atomic redistribution of a single creature stack.
struct DLL_LINKAGE ArmyStackRedistributionTarget
{
	ObjectInstanceID armyId;
	SlotID slot;
	int amount = 0;
	bool operator==(const ArmyStackRedistributionTarget &) const = default;

	template <typename Handler>
	void serialize(Handler & h)
	{
		h & armyId;
		h & slot;
		h & amount;
	}
};
