#pragma once

#include "../../Global.h"

#include <array>
#include <cstddef>
#include <cstdint>
#include <mutex>
#include <optional>

#include "ThorContext.h"

/// Stable semantic commands accepted from the AYN Thor companion display.
enum class ThorAction : std::uint8_t
{
	NONE = 0,
	OPEN_KINGDOM_OVERVIEW = 1,
	OPEN_QUEST_LOG = 2,
	OPEN_PUZZLE_MAP = 3,
	OPEN_SAVE_GAME = 4,
	NEXT_HERO = 5,
	MOVE_HERO = 6,
	TOGGLE_HERO_SLEEP = 7,
	END_TURN = 8,
	BATTLE_WAIT = 9,
	BATTLE_DEFEND = 10,
	BATTLE_TACTICS_NEXT = 11,
	BATTLE_TACTICS_END = 12
};

constexpr std::uint32_t thorActionMask(ThorAction action)
{
	return action == ThorAction::NONE ? 0 : 1U << (static_cast<std::uint8_t>(action) - 1U);
}

DLL_LINKAGE std::optional<ThorAction> thorActionFromId(int actionId);
DLL_LINKAGE bool isThorActionAllowedInAdventureMap(ThorAction action);
DLL_LINKAGE bool isThorActionAllowedInContext(ThorAction action, const std::string & contextId);

struct DLL_LINKAGE ThorActionRequest
{
	std::uint64_t revision = 0;
	ThorAction action = ThorAction::NONE;
};

enum class ThorActionValidation
{
	VALID,
	UNKNOWN_ACTION,
	STALE_REVISION,
	WRONG_CONTEXT,
	UNAVAILABLE
};

DLL_LINKAGE ThorActionValidation validateThorActionRequest(const ThorActionRequest & request, const ThorContextRecord & context);

/// Fixed-capacity handoff between the Android UI thread and the VCMI GUI thread.
/// Overflow deterministically drops the newest request, preserving earlier input order.
class DLL_LINKAGE ThorActionQueue final
{
	static constexpr std::size_t capacity = 16;

	mutable std::mutex mutex;
	std::array<ThorActionRequest, capacity> requests;
	std::size_t first = 0;
	std::size_t count = 0;

public:
	bool submit(ThorActionRequest request);
	std::optional<ThorActionRequest> pop();
	void clear();
	std::size_t size() const;
};

DLL_LINKAGE ThorActionQueue & thorActionQueue();
