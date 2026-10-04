#pragma once

#include "../../Global.h"

#include <array>
#include <cstddef>
#include <cstdint>
#include <limits>
#include <mutex>
#include <optional>
#include <string>
#include <vector>

#include "../constants/NumericConstants.h"

namespace ThorContextIds
{
	inline constexpr char UNKNOWN[] = "UNKNOWN";
	inline constexpr char MAIN_MENU[] = "MAIN_MENU";
	inline constexpr char MAIN_MENU_NEW_GAME[] = "MAIN_MENU_NEW_GAME";
	inline constexpr char MAIN_MENU_LOAD_GAME[] = "MAIN_MENU_LOAD_GAME";
	inline constexpr char MAIN_MENU_CAMPAIGN[] = "MAIN_MENU_CAMPAIGN";
	inline constexpr char MAIN_MENU_CREDITS[] = "MAIN_MENU_CREDITS";
	inline constexpr char HIGH_SCORES[] = "HIGH_SCORES";
	inline constexpr char MULTI_MODE_NEW_GAME[] = "MULTI_MODE_NEW_GAME";
	inline constexpr char MULTI_MODE_LOAD_GAME[] = "MULTI_MODE_LOAD_GAME";
	inline constexpr char MULTI_PLAYERS_NEW_GAME[] = "MULTI_PLAYERS_NEW_GAME";
	inline constexpr char MULTI_PLAYERS_LOAD_GAME[] = "MULTI_PLAYERS_LOAD_GAME";
	inline constexpr char JOIN_SCREEN_NEW_GAME[] = "JOIN_SCREEN_NEW_GAME";
	inline constexpr char JOIN_SCREEN_LOAD_GAME[] = "JOIN_SCREEN_LOAD_GAME";
	inline constexpr char SIMPLE_JOIN[] = "SIMPLE_JOIN";
	inline constexpr char MENU_QUIT_CONFIRMATION[] = "MENU_QUIT_CONFIRMATION";
	inline constexpr char HIGH_SCORE_RESET_CONFIRMATION[] = "HIGH_SCORE_RESET_CONFIRMATION";
	inline constexpr char TUTORIAL_MISSING_DIALOG[] = "TUTORIAL_MISSING_DIALOG";
	inline constexpr char LOBBY_NEW_GAME[] = "LOBBY_NEW_GAME";
	inline constexpr char LOBBY_NEW_GAME_SCENARIO[] = "LOBBY_NEW_GAME_SCENARIO";
	inline constexpr char LOBBY_NEW_GAME_OPTIONS[] = "LOBBY_NEW_GAME_OPTIONS";
	inline constexpr char LOBBY_NEW_GAME_RANDOM_MAP[] = "LOBBY_NEW_GAME_RANDOM_MAP";
	inline constexpr char LOBBY_NEW_GAME_TURN_OPTIONS[] = "LOBBY_NEW_GAME_TURN_OPTIONS";
	inline constexpr char LOBBY_NEW_GAME_EXTRA_OPTIONS[] = "LOBBY_NEW_GAME_EXTRA_OPTIONS";
	inline constexpr char LOBBY_NEW_GAME_BATTLE_MODE[] = "LOBBY_NEW_GAME_BATTLE_MODE";
	inline constexpr char LOBBY_LOAD_GAME[] = "LOBBY_LOAD_GAME";
	inline constexpr char LOBBY_LOAD_GAME_SCENARIO[] = "LOBBY_LOAD_GAME_SCENARIO";
	inline constexpr char LOBBY_LOAD_GAME_OPTIONS[] = "LOBBY_LOAD_GAME_OPTIONS";
	inline constexpr char LOBBY_LOAD_GAME_TURN_OPTIONS[] = "LOBBY_LOAD_GAME_TURN_OPTIONS";
	inline constexpr char LOBBY_LOAD_GAME_EXTRA_OPTIONS[] = "LOBBY_LOAD_GAME_EXTRA_OPTIONS";
	inline constexpr char LOBBY_CAMPAIGN_LIST[] = "LOBBY_CAMPAIGN_LIST";
	inline constexpr char CAMPAIGN_BONUS_SELECTION[] = "CAMPAIGN_BONUS_SELECTION";
	inline constexpr char CAMPAIGN_BROWSER[] = "CAMPAIGN_BROWSER";
	inline constexpr char ADVENTURE_MAP[] = "ADVENTURE_MAP";
	inline constexpr char HERO_WINDOW[] = "HERO_WINDOW";
	inline constexpr char TOWN_WINDOW[] = "TOWN_WINDOW";
	inline constexpr char TOWN_HALL[] = "TOWN_HALL";
	inline constexpr char BUILD_CONFIRMATION[] = "BUILD_CONFIRMATION";
	inline constexpr char TOWN_RECRUITMENT_QUICK[] = "TOWN_RECRUITMENT_QUICK";
	inline constexpr char TOWN_RECRUITMENT_DWELLING[] = "TOWN_RECRUITMENT_DWELLING";
	inline constexpr char HERO_MEETING[] = "HERO_MEETING";
	inline constexpr char BATTLE[] = "BATTLE";
	inline constexpr char BATTLE_TACTICS[] = "BATTLE_TACTICS";
	inline constexpr char BATTLE_RESULT[] = "BATTLE_RESULT";
	inline constexpr char KINGDOM_OVERVIEW[] = "KINGDOM_OVERVIEW";
	inline constexpr char QUEST_LOG[] = "QUEST_LOG";
	inline constexpr char SCENARIO_EVENT_JOURNAL[] = "SCENARIO_EVENT_JOURNAL";
	inline constexpr char PUZZLE_MAP[] = "PUZZLE_MAP";
	inline constexpr char SAVE_GAME[] = "SAVE_GAME";
}

enum class ThorLobbyMode
{
	UNKNOWN,
	NEW_GAME,
	LOAD_GAME,
	CAMPAIGN_LIST
};

/// Native-only purpose tags for the three explicitly supported menu dialogs.
enum class ThorMenuModalType : std::uint8_t
{
	NONE,
	QUIT_CONFIRMATION,
	HIGH_SCORE_RESET_CONFIRMATION,
	MISSING_TUTORIAL_INFORMATION
};

enum class ThorLobbyTab
{
	UNKNOWN,
	NONE,
	SCENARIO,
	OPTIONS,
	RANDOM_MAP,
	TURN_OPTIONS,
	EXTRA_OPTIONS,
	BATTLE_MODE
};

enum class ThorInGameContext
{
	UNKNOWN,
	ADVENTURE_MAP,
	HERO_WINDOW,
	TOWN_WINDOW,
	TOWN_HALL,
	TOWN_RECRUITMENT_QUICK,
	TOWN_RECRUITMENT_DWELLING,
	HERO_MEETING,
	BATTLE,
	BATTLE_TACTICS,
	BATTLE_RESULT,
	KINGDOM_OVERVIEW,
	QUEST_LOG,
	SCENARIO_EVENT_JOURNAL,
	PUZZLE_MAP,
	SAVE_GAME
};

inline constexpr std::size_t THOR_CONTEXT_DETAIL_LINE_COUNT = 4;
inline constexpr std::size_t THOR_BROWSER_MAX_ROWS = 8;
inline constexpr std::size_t THOR_MAX_TOWN_HALL_BUILDINGS = 64;
inline constexpr std::size_t THOR_MAX_RECRUITMENT_ROWS = 64;
inline constexpr std::size_t THOR_RECRUITMENT_PAGE_SIZE = 5;
inline constexpr std::size_t THOR_MAX_RECRUITMENT_VARIANTS = 16;
inline constexpr std::size_t THOR_TOWN_HALL_PAGE_SIZE = 5;
inline constexpr std::size_t THOR_SAVE_BROWSER_PAGE_SIZE = 5;
using ThorContextDetails = std::array<std::string, THOR_CONTEXT_DETAIL_LINE_COUNT>;
inline constexpr std::size_t THOR_MAX_HEROES = GameConstants::MAX_HEROES_PER_PLAYER;
inline constexpr std::size_t THOR_MAX_TOWNS = 64;
inline constexpr std::size_t THOR_HERO_MEETING_ARMY_SIZE = GameConstants::ARMY_SIZE;
inline constexpr std::size_t THOR_HERO_MEETING_SLOT_KEY_COUNT = THOR_HERO_MEETING_ARMY_SIZE * 2;
inline constexpr std::size_t THOR_HERO_MEETING_EQUIPPED_ARTIFACT_COUNT = 19;
inline constexpr std::size_t THOR_HERO_MEETING_BACKPACK_ARTIFACT_COUNT = 5;
inline constexpr std::size_t THOR_HERO_MEETING_ARTIFACT_COUNT =
	(THOR_HERO_MEETING_EQUIPPED_ARTIFACT_COUNT + THOR_HERO_MEETING_BACKPACK_ARTIFACT_COUNT) * 2;
inline constexpr std::size_t THOR_MAX_HERO_MEETING_VISUAL_ASSETS = THOR_HERO_MEETING_ARMY_SIZE * 2
	+ THOR_HERO_MEETING_ARTIFACT_COUNT + 2; // Two hero portraits may accompany all row assets.
inline constexpr std::size_t THOR_MAX_VISUAL_ASSET_KEYS = THOR_MAX_HERO_MEETING_VISUAL_ASSETS;

enum class ThorVisualAssetKind : std::uint8_t
{
	CREATURE = 1,
	ARTIFACT = 2,
	HERO = 3
};

/// Stable typed visual key. Artifact keys use artifact type IDs; hero keys use portrait-source HeroTypeIDs.
inline constexpr std::uint64_t thorVisualAssetKey(ThorVisualAssetKind kind, int typeId)
{
	return typeId < 0 ? 0 : (static_cast<std::uint64_t>(kind) << 56) | (static_cast<std::uint64_t>(typeId) + 1);
}

inline constexpr std::uint64_t thorCreatureVisualAssetKey(int creatureId)
{
	return thorVisualAssetKey(ThorVisualAssetKind::CREATURE, creatureId);
}

inline constexpr std::uint64_t thorArtifactVisualAssetKey(int artifactTypeId)
{
	return thorVisualAssetKey(ThorVisualAssetKind::ARTIFACT, artifactTypeId);
}

inline constexpr std::uint64_t thorHeroPortraitVisualAssetKey(int portraitSourceHeroTypeId)
{
	return thorVisualAssetKey(ThorVisualAssetKind::HERO, portraitSourceHeroTypeId);
}

inline constexpr bool isThorVisualAssetKey(std::uint64_t key)
{
	const auto kind = static_cast<std::uint8_t>(key >> 56);
	const auto typeId = key & 0xffffffffULL;
	const auto reserved = (key >> 32) & 0xffffffULL;
	return (kind == static_cast<std::uint8_t>(ThorVisualAssetKind::CREATURE)
		|| kind == static_cast<std::uint8_t>(ThorVisualAssetKind::ARTIFACT)
		|| kind == static_cast<std::uint8_t>(ThorVisualAssetKind::HERO))
		&& typeId != 0 && typeId <= static_cast<std::uint64_t>(std::numeric_limits<int>::max()) + 1 && reserved == 0;
}

inline constexpr bool isThorHeroPortraitVisualAssetKey(std::uint64_t key)
{
	return isThorVisualAssetKey(key) && static_cast<std::uint8_t>(key >> 56)
		== static_cast<std::uint8_t>(ThorVisualAssetKind::HERO);
}

struct DLL_LINKAGE ThorHeroEntry
{
	int id = -1;
	std::string name;
	int movement = 0;
	int maximumMovement = 0;
	bool selected = false;
	bool sleeping = false;
	bool operator==(const ThorHeroEntry &) const = default;
};

struct DLL_LINKAGE ThorTownEntry
{
	int id = -1;
	std::string name;
	bool selected = false;
	bool operator==(const ThorTownEntry &) const = default;
};

/// One visible browser row. target is a native list index, never a path or pointer.
struct DLL_LINKAGE ThorBrowserEntry
{
	int target = -1;
	std::string label;
	bool enabled = false;
	bool selected = false;
	bool completed = false;
	bool operator==(const ThorBrowserEntry &) const = default;
};

enum class ThorRecruitmentMode : std::uint8_t
{
	QUICK_TOWN = 1,
	TOWN_DWELLING = 2
};

/// One bounded creature choice owned by the exact native recruitment window.
struct DLL_LINKAGE ThorRecruitmentRow
{
	int target = -1;
	int creatureId = -1;
	std::string name;
	std::string unitCost;
	std::string selectedCost;
	int availableCount = 0;
	int selectedAmount = 0;
	int maximumAmount = 0;
	int variantIndex = 0;
	int variantCount = 1;
	bool enabled = false;
	bool selected = false;
	std::uint64_t visualAssetKey = 0;
	bool armyAvailable = true;
	bool operator==(const ThorRecruitmentRow &) const = default;
};

/// Native-only owner identities and immutable visible state for both Town recruitment windows.
struct DLL_LINKAGE ThorRecruitmentSnapshot
{
	ThorRecruitmentMode mode = ThorRecruitmentMode::QUICK_TOWN;
	int townId = -1;
	int dwellingLevel = -1;
	int destinationArmyId = -1;
	int destinationArmyFreeSlots = 0;
	int selectedTarget = -1;
	std::string townName;
	std::string totalCost;
	bool locallyControllable = false;
	bool canBuy = false;
	std::vector<ThorRecruitmentRow> rows;
	bool operator==(const ThorRecruitmentSnapshot &) const = default;
};

/// One fixed, addressable stack position in a Hero Meeting army.
struct DLL_LINKAGE ThorHeroMeetingSlot
{
	int armyId = -1;
	int slot = -1;
	bool occupied = false;
	int creatureId = -1;
	std::string creatureName;
	int count = 0;
	std::uint64_t visualAssetKey = 0;
	bool operator==(const ThorHeroMeetingSlot &) const = default;
};

/// Read-only Hero Meeting army snapshot. Both arrays always contain exactly seven positions.
struct DLL_LINKAGE ThorHeroMeetingArmies
{
	int leftHeroId = -1;
	int rightHeroId = -1;
	int leftArmyId = -1;
	int rightArmyId = -1;
	std::string leftHeroName;
	std::string rightHeroName;
	bool locallyControllable = false;
	std::array<ThorHeroMeetingSlot, THOR_HERO_MEETING_ARMY_SIZE> leftSlots;
	std::array<ThorHeroMeetingSlot, THOR_HERO_MEETING_ARMY_SIZE> rightSlots;
	std::array<std::uint64_t, 2> heroPortraitAssetKeys{};
	bool operator==(const ThorHeroMeetingArmies &) const = default;
};

struct DLL_LINKAGE ThorHeroMeetingArtifact
{
	int heroId = -1;
	int position = -1;
	bool backpack = false;
	bool occupied = false;
	bool locked = false;
	std::string name;
	int instanceId = -1;
	int artifactTypeId = -1;
	std::uint64_t visualAssetKey = 0;
	bool operator==(const ThorHeroMeetingArtifact &) const = default;
};

/// Read-only snapshot of the equipped slots and five currently visible backpack slots for both heroes.
struct DLL_LINKAGE ThorHeroMeetingArtifacts
{
	int leftHeroId = -1;
	int rightHeroId = -1;
	std::string leftHeroName;
	std::string rightHeroName;
	std::vector<ThorHeroMeetingArtifact> artifactSlots;
	bool operator==(const ThorHeroMeetingArtifacts &) const = default;
};

/// Immutable, read-only context payload reserved for the Thor command deck.
struct DLL_LINKAGE ThorContextRecord
{
	std::uint64_t revision = 0;
	std::string contextId = ThorContextIds::UNKNOWN;
	std::string title;
	std::string status;
	ThorMenuModalType menuModalType = ThorMenuModalType::NONE;
	std::string menuModalSourceContext;
	/// Native-only owner identity. These pointers are never passed to JNI.
	const void * nativeOwnerToken = nullptr;
	const void * nativeParentToken = nullptr;
	std::uint64_t enabledActionMask = 0;
	std::uint64_t activeActionMask = 0;
	int selectedHeroId = -1;
	std::int64_t actionSubjectId = -1;
	std::uint64_t actionEpoch = 0;
	/// Native-only identity of the active Hero or Town window subject.
	int windowSubjectId = -1;
	ThorContextDetails details;
	std::vector<ThorHeroEntry> heroes;
	std::vector<ThorTownEntry> towns;
	int browserPage = 0;
	int browserPageCount = 0;
	std::vector<ThorBrowserEntry> browserEntries;
	/// Native-only identity of the exact ordered list; never sent through JNI.
	std::vector<std::string> browserNativeKeys;
	std::optional<ThorHeroMeetingArmies> heroMeetingArmies;
	std::optional<ThorHeroMeetingArtifacts> heroMeetingArtifacts;
	std::optional<ThorRecruitmentSnapshot> recruitment;
	std::uint64_t heroPortraitAssetKey = 0;
	/// Native-only SelectionTab state token. Never passed through JNI.
	std::uint64_t scenarioSelectionRevision = 0;
	/// Native-only campaign scenario/bonus state token. Never passed through JNI.
	std::uint64_t campaignSelectionRevision = 0;
};

/// Thread-safe latest-record handoff. Consumers must discard revisions older than their last render.
class DLL_LINKAGE ThorContextStore final
{
	mutable std::mutex mutex;
	ThorContextRecord current;

public:
	ThorContextRecord snapshot() const;
	bool publish(ThorContextRecord next);
	ThorContextRecord publishNext(ThorContextRecord next);
};

DLL_LINKAGE ThorContextStore & thorContextStore();
DLL_LINKAGE std::string thorContextIdForMainMenuTab(const std::string & tabName);
DLL_LINKAGE std::string thorContextIdForLobby(ThorLobbyMode mode, ThorLobbyTab tab);
DLL_LINKAGE std::string thorContextIdForInGameContext(ThorInGameContext context);
DLL_LINKAGE bool thorMenuModalOwnerMatches(const ThorContextRecord & context, ThorMenuModalType type,
	const std::string & sourceContext, const void * ownerToken, const void * parentToken, bool active, bool top);
DLL_LINKAGE std::string thorBoundedText(std::string text, std::size_t maximumBytes = 128);
