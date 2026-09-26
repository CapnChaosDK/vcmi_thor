#pragma once

#include "../../Global.h"

#include <array>
#include <cstddef>
#include <cstdint>
#include <mutex>
#include <optional>
#include <utility>

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
	BATTLE_TACTICS_END = 12,
	SELECT_HERO = 13,
	SELECT_TOWN = 14,
	HERO_MEETING_MOVE_STACK = 15,
	HERO_MEETING_TRANSFER_STACK = 16,
	HERO_MEETING_ARMY_LEFT_TO_RIGHT = 17,
	HERO_MEETING_ARMY_RIGHT_TO_LEFT = 18,
	HERO_MEETING_SWAP_ARMIES = 19,
	HERO_MEETING_SPLIT_STACK = 20,
	HERO_MEETING_TRANSFER_ARTIFACT = 21
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
	int targetId = -1;
	int sourceArmyId = -1;
	int sourceSlot = -1;
	int destinationArmyId = -1;
	int destinationSlot = -1;
	int amount = -1;
};

/// A directional pair of fixed Hero Meeting slot keys. Keys 0-6 are left, 7-13 are right.
struct DLL_LINKAGE ThorHeroMeetingTransferPair
{
	int sourceKey = -1;
	int destinationKey = -1;
	bool sourceIsLeft = false;
	int sourceSlot = -1;
	bool destinationIsLeft = false;
	int destinationSlot = -1;
	bool operator==(const ThorHeroMeetingTransferPair &) const = default;
};

/// Action-16 payload: sourceKey * 14 + destinationKey, bounded to [0, 195].
/// Same-side pairs and identical endpoints are rejected by both helpers.
DLL_LINKAGE std::optional<int> encodeThorHeroMeetingTransferPair(int sourceKey, int destinationKey);
DLL_LINKAGE std::optional<ThorHeroMeetingTransferPair> decodeThorHeroMeetingTransferPair(int encodedPair);

/// Action 21 encodes two distinct artifact row keys in [0, 47].
DLL_LINKAGE std::optional<int> encodeThorHeroMeetingArtifactPair(int sourceKey, int destinationKey);
DLL_LINKAGE std::optional<std::pair<int, int>> decodeThorHeroMeetingArtifactPair(int encodedPair);

/// Mirrors whether native bulkMoveArmy can produce at least one stack change for this published army snapshot.
DLL_LINKAGE bool canThorHeroMeetingMoveArmy(const ThorHeroMeetingArmies & armies, bool leftToRight);
DLL_LINKAGE bool canThorHeroMeetingSplitStack(const ThorHeroMeetingArmies & armies,
	int sourceArmyId, int sourceSlot, int destinationArmyId, int destinationSlot, int amount);

enum class ThorActionValidation
{
	VALID,
	UNKNOWN_ACTION,
	STALE_REVISION,
	WRONG_CONTEXT,
	UNAVAILABLE,
	INVALID_TARGET
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
