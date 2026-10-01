#pragma once

#include "../../Global.h"

#include <array>
#include <cstddef>
#include <cstdint>
#include <limits>
#include <mutex>
#include <optional>
#include <span>
#include <string_view>
#include <utility>

#include "ThorContext.h"
#include "../mapObjects/army/ArmyStackRedistribution.h"

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
	HERO_MEETING_TRANSFER_ARTIFACT = 21,
	HERO_MEETING_REDISTRIBUTE_STACK = 22,
	HERO_MEETING_ARTIFACTS_LEFT_TO_RIGHT = 23,
	HERO_MEETING_ARTIFACTS_RIGHT_TO_LEFT = 24,
	HERO_MEETING_SWAP_ARTIFACTS = 25,
	LOBBY_SET_DIFFICULTY = 26,
	LOBBY_START_GAME = 27,
	LOBBY_BACK = 28,
	LOBBY_PREVIOUS_SCENARIO = 29,
	LOBBY_NEXT_SCENARIO = 30,
	MAIN_MENU_CHOICE_1 = 31,
	MAIN_MENU_CHOICE_2 = 32,
	MAIN_MENU_CHOICE_3 = 33,
	MAIN_MENU_CHOICE_4 = 34,
	MAIN_MENU_CHOICE_5 = 35,
	CAMPAIGN_PREVIOUS_SCENARIO = 36,
	CAMPAIGN_NEXT_SCENARIO = 37,
	CAMPAIGN_SELECT_BONUS_1 = 38,
	CAMPAIGN_SELECT_BONUS_2 = 39,
	CAMPAIGN_SELECT_BONUS_3 = 40,
	CAMPAIGN_START = 41,
	CAMPAIGN_BACK = 42,
	CAMPAIGN_BROWSER_SELECT = 43,
	CAMPAIGN_BROWSER_PREVIOUS_PAGE = 44,
	CAMPAIGN_BROWSER_NEXT_PAGE = 45,
	CAMPAIGN_BROWSER_BACK = 46,
	LOAD_BROWSER_SELECT = 47,
	LOAD_BROWSER_PREVIOUS_PAGE = 48,
	LOAD_BROWSER_NEXT_PAGE = 49,
	WINDOW_PREVIOUS = 50,
	WINDOW_NEXT = 51,
	WINDOW_CLOSE = 52
};

inline constexpr std::uint8_t THOR_MAX_ACTION_ID = static_cast<std::uint8_t>(ThorAction::WINDOW_CLOSE);
static_assert(THOR_MAX_ACTION_ID <= std::numeric_limits<std::uint64_t>::digits,
	"Thor action IDs must fit in the 64-bit action-mask contract");

constexpr std::uint64_t thorActionMask(ThorAction action)
{
	const auto actionId = static_cast<std::uint8_t>(action);
	return actionId == 0 || actionId > std::numeric_limits<std::uint64_t>::digits
		? 0 : std::uint64_t{1} << (actionId - 1U);
}

DLL_LINKAGE std::optional<ThorAction> thorActionFromId(int actionId);
DLL_LINKAGE bool isThorActionAllowedInAdventureMap(ThorAction action);
DLL_LINKAGE bool isThorActionAllowedInContext(ThorAction action, const std::string & contextId);
DLL_LINKAGE bool thorWindowOwnerMatches(const ThorContextRecord & context,
	const std::string & expectedContext, int subjectId, bool active, bool top);
/// getHeroSerial is one-based; getHeroBySerial accepts a zero-based index.
DLL_LINKAGE std::optional<int> thorHeroWindowAdjacentIndex(int oneBasedSerial, int visibleCount, ThorAction action);
struct DLL_LINKAGE ThorMainMenuChoice
{
	std::size_t index;
	std::string_view command;
};
DLL_LINKAGE std::optional<ThorMainMenuChoice> thorMainMenuChoice(const std::string & contextId, ThorAction action);
DLL_LINKAGE bool thorMainMenuChoiceMatches(const ThorMainMenuChoice & choice,
	std::size_t configuredIndex, std::string_view command);
struct DLL_LINKAGE ThorMainMenuButtonState
{
	std::size_t configuredIndex;
	std::string_view command;
	bool executable;
};
DLL_LINKAGE bool thorMainMenuChoiceAvailable(const ThorMainMenuChoice & choice,
	std::span<const ThorMainMenuButtonState> buttons);
DLL_LINKAGE bool thorMainMenuTabMatches(const std::string & contextId, std::size_t index,
	std::span<const std::string> tabNames, std::size_t creditsIndex);
DLL_LINKAGE std::optional<std::size_t> thorMainMenuTabIndex(const std::string & contextId,
	std::size_t creditsIndex);
DLL_LINKAGE std::string_view thorMainMenuNavigationTarget(std::string_view command);
DLL_LINKAGE std::optional<std::size_t> thorAdjacentScenarioPosition(
	std::span<const std::uint8_t> selectableEntries, std::size_t currentPosition, ThorAction action);
/// Preserves the native filtered/sorted order while excluding folders and invalid saves.
DLL_LINKAGE std::vector<std::size_t> thorSelectableBrowserPositions(
	std::span<const std::uint8_t> selectableEntries);
DLL_LINKAGE bool isThorActionHapticEligible(ThorAction action);
DLL_LINKAGE bool isThorActionHapticDeferredUntilServerResult(ThorAction action);
DLL_LINKAGE bool isThorActionArtifactMutation(ThorAction action);
DLL_LINKAGE bool thorHeroMeetingArtifactsChanged(
	const ThorHeroMeetingArtifacts & before, const ThorHeroMeetingArtifacts & after);

enum class ThorBulkArtifactOperation : std::uint8_t { LEFT_TO_RIGHT, RIGHT_TO_LEFT, SWAP };
DLL_LINKAGE std::optional<ThorBulkArtifactOperation> thorBulkArtifactOperation(ThorAction action);
DLL_LINKAGE bool canExecuteThorBulkArtifactAction(const ThorContextRecord & context,
	const ThorHeroMeetingArtifacts & currentArtifacts, bool makingTurn, bool pickedArtifact,
	int leftOwner, int rightOwner, int playerId);
DLL_LINKAGE bool shouldRestoreThorBulkArtifactActions(bool serverSuccess, std::uint64_t enabledActionMask);

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

/// One destination in a bounded Hero Meeting redistribution plan.
struct DLL_LINKAGE ThorHeroMeetingRedistributionTarget
{
	int armyId = -1;
	int slot = -1;
	int amount = 0;
	bool operator==(const ThorHeroMeetingRedistributionTarget &) const = default;
};

constexpr std::size_t THOR_MAX_HERO_MEETING_REDISTRIBUTION_DESTINATIONS =
	static_cast<std::size_t>(THOR_HERO_MEETING_SLOT_KEY_COUNT) - 1;
static_assert(THOR_MAX_HERO_MEETING_REDISTRIBUTION_DESTINATIONS == MAX_ARMY_STACK_REDISTRIBUTION_DESTINATIONS);

/// Dedicated action-22 payload. The fixed target array bounds both JNI input and game-thread transport.
struct DLL_LINKAGE ThorHeroMeetingRedistributionRequest
{
	std::uint64_t revision = 0;
	ThorAction action = ThorAction::HERO_MEETING_REDISTRIBUTE_STACK;
	int leftHeroId = -1;
	int rightHeroId = -1;
	int sourceArmyId = -1;
	int sourceSlot = -1;
	int sourceCreatureId = -1;
	int sourceCount = 0;
	std::size_t destinationCount = 0;
	std::array<ThorHeroMeetingRedistributionTarget, THOR_MAX_HERO_MEETING_REDISTRIBUTION_DESTINATIONS> destinations{};
};

/// Decodes equal-length bounded JNI arrays into the fixed native request structure.
DLL_LINKAGE std::optional<ThorHeroMeetingRedistributionRequest> decodeThorHeroMeetingRedistributionRequest(
	std::uint64_t revision, int leftHeroId, int rightHeroId, int sourceArmyId, int sourceSlot,
	int sourceCreatureId, int sourceCount, std::span<const int> destinationArmyIds,
	std::span<const int> destinationSlots, std::span<const int> amounts);

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

DLL_LINKAGE ThorActionValidation validateThorLobbyActionRequest(const ThorActionRequest & request,
	const ThorContextRecord & context, bool exactTopOwner, bool scenarioTabActive, bool authoritative,
	bool mapAvailable, bool startAvailable, bool scenarioNavigationAvailable);

struct DLL_LINKAGE ThorActionAcceptance
{
	std::uint64_t revision = 0;
	ThorAction action = ThorAction::NONE;
};

DLL_LINKAGE std::optional<ThorActionAcceptance> thorActionAcceptance(
	const ThorActionRequest & request, ThorActionValidation validation, bool executed);

DLL_LINKAGE ThorActionValidation validateThorActionRequest(const ThorActionRequest & request, const ThorContextRecord & context);
DLL_LINKAGE ThorActionValidation validateThorHeroMeetingRedistributionRequest(
	const ThorHeroMeetingRedistributionRequest & request, const ThorContextRecord & context);

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

/// Separate fixed-capacity queue for the larger, bounded action-22 allocation payload.
class DLL_LINKAGE ThorHeroMeetingRedistributionQueue final
{
	static constexpr std::size_t capacity = 4;

	mutable std::mutex mutex;
	std::array<ThorHeroMeetingRedistributionRequest, capacity> requests;
	std::size_t first = 0;
	std::size_t count = 0;

public:
	bool submit(ThorHeroMeetingRedistributionRequest request);
	std::optional<ThorHeroMeetingRedistributionRequest> pop();
	void clear();
	std::size_t size() const;
};

DLL_LINKAGE ThorHeroMeetingRedistributionQueue & thorHeroMeetingRedistributionQueue();
