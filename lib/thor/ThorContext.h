#pragma once

#include "../../Global.h"

#include <array>
#include <cstddef>
#include <cstdint>
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
	inline constexpr char ADVENTURE_MAP[] = "ADVENTURE_MAP";
	inline constexpr char HERO_WINDOW[] = "HERO_WINDOW";
	inline constexpr char TOWN_WINDOW[] = "TOWN_WINDOW";
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
using ThorContextDetails = std::array<std::string, THOR_CONTEXT_DETAIL_LINE_COUNT>;
inline constexpr std::size_t THOR_MAX_HEROES = GameConstants::MAX_HEROES_PER_PLAYER;
inline constexpr std::size_t THOR_MAX_TOWNS = 64;
inline constexpr std::size_t THOR_HERO_MEETING_ARMY_SIZE = GameConstants::ARMY_SIZE;
inline constexpr std::size_t THOR_HERO_MEETING_SLOT_KEY_COUNT = THOR_HERO_MEETING_ARMY_SIZE * 2;
inline constexpr std::size_t THOR_HERO_MEETING_EQUIPPED_ARTIFACT_COUNT = 19;
inline constexpr std::size_t THOR_HERO_MEETING_BACKPACK_ARTIFACT_COUNT = 5;
inline constexpr std::size_t THOR_HERO_MEETING_ARTIFACT_COUNT =
	(THOR_HERO_MEETING_EQUIPPED_ARTIFACT_COUNT + THOR_HERO_MEETING_BACKPACK_ARTIFACT_COUNT) * 2;
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

/// One fixed, addressable stack position in a Hero Meeting army.
struct DLL_LINKAGE ThorHeroMeetingSlot
{
	int armyId = -1;
	int slot = -1;
	bool occupied = false;
	int creatureId = -1;
	std::string creatureName;
	int count = 0;
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
	std::uint32_t enabledActionMask = 0;
	std::uint32_t activeActionMask = 0;
	int selectedHeroId = -1;
	std::int64_t actionSubjectId = -1;
	std::uint64_t actionEpoch = 0;
	ThorContextDetails details;
	std::vector<ThorHeroEntry> heroes;
	std::vector<ThorTownEntry> towns;
	std::optional<ThorHeroMeetingArmies> heroMeetingArmies;
	std::optional<ThorHeroMeetingArtifacts> heroMeetingArtifacts;
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
DLL_LINKAGE std::string thorBoundedText(std::string text, std::size_t maximumBytes = 128);
