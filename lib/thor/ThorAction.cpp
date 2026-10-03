#include "ThorAction.h"

#include <algorithm>
#include <limits>

std::optional<int> encodeThorHeroMeetingArtifactPair(int sourceKey, int destinationKey)
{
	constexpr int count = static_cast<int>(THOR_HERO_MEETING_ARTIFACT_COUNT);
	constexpr int perHero = count / 2;
	if(sourceKey < 0 || destinationKey < 0 || sourceKey >= count || destinationKey >= count
		|| sourceKey / perHero == destinationKey / perHero)
		return std::nullopt;
	return sourceKey * count + destinationKey;
}

std::optional<std::pair<int, int>> decodeThorHeroMeetingArtifactPair(int encodedPair)
{
	constexpr int count = static_cast<int>(THOR_HERO_MEETING_ARTIFACT_COUNT);
	if(encodedPair < 0 || encodedPair >= count * count)
		return std::nullopt;
	const int source = encodedPair / count;
	const int destination = encodedPair % count;
	if(!encodeThorHeroMeetingArtifactPair(source, destination))
		return std::nullopt;
	return std::pair{source, destination};
}

std::optional<int> encodeThorHeroMeetingTransferPair(int sourceKey, int destinationKey)
{
	constexpr int slotKeyCount = static_cast<int>(THOR_HERO_MEETING_SLOT_KEY_COUNT);
	if(sourceKey < 0 || sourceKey >= slotKeyCount || destinationKey < 0 || destinationKey >= slotKeyCount
		|| sourceKey / static_cast<int>(THOR_HERO_MEETING_ARMY_SIZE)
			== destinationKey / static_cast<int>(THOR_HERO_MEETING_ARMY_SIZE))
		return std::nullopt;
	return sourceKey * slotKeyCount + destinationKey;
}

std::optional<ThorHeroMeetingTransferPair> decodeThorHeroMeetingTransferPair(int encodedPair)
{
	constexpr int slotKeyCount = static_cast<int>(THOR_HERO_MEETING_SLOT_KEY_COUNT);
	if(encodedPair < 0 || encodedPair >= slotKeyCount * slotKeyCount)
		return std::nullopt;
	const auto sourceKey = encodedPair / slotKeyCount;
	const auto destinationKey = encodedPair % slotKeyCount;
	const auto verifiedPair = encodeThorHeroMeetingTransferPair(sourceKey, destinationKey);
	if(!verifiedPair || *verifiedPair != encodedPair)
		return std::nullopt;
	const auto slotsPerArmy = static_cast<int>(THOR_HERO_MEETING_ARMY_SIZE);
	return ThorHeroMeetingTransferPair{sourceKey, destinationKey, sourceKey < slotsPerArmy,
		sourceKey % slotsPerArmy, destinationKey < slotsPerArmy, destinationKey % slotsPerArmy};
}

bool canThorHeroMeetingMoveArmy(const ThorHeroMeetingArmies & armies, bool leftToRight)
{
	const auto & source = leftToRight ? armies.leftSlots : armies.rightSlots;
	const auto & destination = leftToRight ? armies.rightSlots : armies.leftSlots;
	const auto occupied = std::count_if(source.begin(), source.end(), [](const auto & slot) { return slot.occupied; });
	if(occupied == 0)
		return false;
	if(occupied == 1)
	{
		const auto onlyStack = std::find_if(source.begin(), source.end(), [](const auto & slot) { return slot.occupied; });
		if(onlyStack->count <= 1)
			return false;
	}

	const bool hasFreeDestination = std::any_of(destination.begin(), destination.end(),
		[](const auto & slot) { return !slot.occupied; });
	return std::any_of(source.begin(), source.end(), [&](const auto & stack)
	{
		return stack.occupied && (hasFreeDestination || std::any_of(destination.begin(), destination.end(),
			[&](const auto & target) { return target.occupied && target.creatureId == stack.creatureId; }));
	});
}

bool canThorHeroMeetingSplitStack(const ThorHeroMeetingArmies & armies, int sourceArmyId, int sourceSlot,
	int destinationArmyId, int destinationSlot, int amount)
{
	if(!armies.locallyControllable || sourceSlot < 0 || destinationSlot < 0
		|| sourceSlot >= static_cast<int>(THOR_HERO_MEETING_ARMY_SIZE)
		|| destinationSlot >= static_cast<int>(THOR_HERO_MEETING_ARMY_SIZE)
		|| (sourceArmyId != armies.leftArmyId && sourceArmyId != armies.rightArmyId)
		|| (destinationArmyId != armies.leftArmyId && destinationArmyId != armies.rightArmyId)
		|| (sourceArmyId == destinationArmyId && sourceSlot == destinationSlot) || amount < 1)
		return false;
	const auto & source = (sourceArmyId == armies.leftArmyId ? armies.leftSlots : armies.rightSlots)[sourceSlot];
	const auto & destination = (destinationArmyId == armies.leftArmyId ? armies.leftSlots : armies.rightSlots)[destinationSlot];
	return source.occupied && source.armyId == sourceArmyId && source.slot == sourceSlot && source.count > amount
		&& destination.armyId == destinationArmyId && destination.slot == destinationSlot
		&& (!destination.occupied || destination.creatureId == source.creatureId);
}

std::optional<ThorHeroMeetingRedistributionRequest> decodeThorHeroMeetingRedistributionRequest(
	std::uint64_t revision, int leftHeroId, int rightHeroId, int sourceArmyId, int sourceSlot,
	int sourceCreatureId, int sourceCount, std::span<const int> destinationArmyIds,
	std::span<const int> destinationSlots, std::span<const int> amounts)
{
	const auto destinationCount = destinationArmyIds.size();
	if(revision == 0 || leftHeroId < 0 || rightHeroId < 0 || leftHeroId == rightHeroId
		|| (sourceArmyId != leftHeroId && sourceArmyId != rightHeroId)
		|| sourceSlot < 0 || sourceSlot >= static_cast<int>(THOR_HERO_MEETING_ARMY_SIZE)
		|| sourceCreatureId < 0 || sourceCount < 2 || destinationCount == 0
		|| destinationCount > THOR_MAX_HERO_MEETING_REDISTRIBUTION_DESTINATIONS
		|| destinationSlots.size() != destinationCount || amounts.size() != destinationCount)
		return std::nullopt;

	ThorHeroMeetingRedistributionRequest request;
	request.revision = revision;
	request.leftHeroId = leftHeroId;
	request.rightHeroId = rightHeroId;
	request.sourceArmyId = sourceArmyId;
	request.sourceSlot = sourceSlot;
	request.sourceCreatureId = sourceCreatureId;
	request.sourceCount = sourceCount;
	request.destinationCount = destinationCount;
	std::int64_t total = 0;
	for(std::size_t index = 0; index < destinationCount; ++index)
	{
		const auto armyId = destinationArmyIds[index];
		const auto slot = destinationSlots[index];
		const auto amount = amounts[index];
		if((armyId != leftHeroId && armyId != rightHeroId) || slot < 0
			|| slot >= static_cast<int>(THOR_HERO_MEETING_ARMY_SIZE) || amount <= 0
			|| (armyId == sourceArmyId && slot == sourceSlot))
			return std::nullopt;
		for(std::size_t earlier = 0; earlier < index; ++earlier)
		{
			if(request.destinations[earlier].armyId == armyId && request.destinations[earlier].slot == slot)
				return std::nullopt;
		}
		total += amount;
		if(total > static_cast<std::int64_t>(sourceCount) - 1)
			return std::nullopt;
		request.destinations[index] = {armyId, slot, amount};
	}
	return request;
}

std::optional<ThorAction> thorActionFromId(int actionId)
{
	switch(actionId)
	{
	case static_cast<int>(ThorAction::OPEN_KINGDOM_OVERVIEW):
		return ThorAction::OPEN_KINGDOM_OVERVIEW;
	case static_cast<int>(ThorAction::OPEN_QUEST_LOG):
		return ThorAction::OPEN_QUEST_LOG;
	case static_cast<int>(ThorAction::OPEN_PUZZLE_MAP):
		return ThorAction::OPEN_PUZZLE_MAP;
	case static_cast<int>(ThorAction::OPEN_SAVE_GAME):
		return ThorAction::OPEN_SAVE_GAME;
	case static_cast<int>(ThorAction::NEXT_HERO):
		return ThorAction::NEXT_HERO;
	case static_cast<int>(ThorAction::MOVE_HERO):
		return ThorAction::MOVE_HERO;
	case static_cast<int>(ThorAction::TOGGLE_HERO_SLEEP):
		return ThorAction::TOGGLE_HERO_SLEEP;
	case static_cast<int>(ThorAction::END_TURN):
		return ThorAction::END_TURN;
	case static_cast<int>(ThorAction::BATTLE_WAIT):
		return ThorAction::BATTLE_WAIT;
	case static_cast<int>(ThorAction::BATTLE_DEFEND):
		return ThorAction::BATTLE_DEFEND;
	case static_cast<int>(ThorAction::BATTLE_TACTICS_NEXT):
		return ThorAction::BATTLE_TACTICS_NEXT;
	case static_cast<int>(ThorAction::BATTLE_TACTICS_END):
		return ThorAction::BATTLE_TACTICS_END;
	case static_cast<int>(ThorAction::SELECT_HERO):
		return ThorAction::SELECT_HERO;
	case static_cast<int>(ThorAction::SELECT_TOWN):
		return ThorAction::SELECT_TOWN;
	case static_cast<int>(ThorAction::HERO_MEETING_MOVE_STACK):
		return ThorAction::HERO_MEETING_MOVE_STACK;
	case static_cast<int>(ThorAction::HERO_MEETING_TRANSFER_STACK):
		return ThorAction::HERO_MEETING_TRANSFER_STACK;
	case static_cast<int>(ThorAction::HERO_MEETING_ARMY_LEFT_TO_RIGHT):
		return ThorAction::HERO_MEETING_ARMY_LEFT_TO_RIGHT;
	case static_cast<int>(ThorAction::HERO_MEETING_ARMY_RIGHT_TO_LEFT):
		return ThorAction::HERO_MEETING_ARMY_RIGHT_TO_LEFT;
	case static_cast<int>(ThorAction::HERO_MEETING_SWAP_ARMIES):
		return ThorAction::HERO_MEETING_SWAP_ARMIES;
	case static_cast<int>(ThorAction::HERO_MEETING_SPLIT_STACK):
		return ThorAction::HERO_MEETING_SPLIT_STACK;
	case static_cast<int>(ThorAction::HERO_MEETING_TRANSFER_ARTIFACT):
		return ThorAction::HERO_MEETING_TRANSFER_ARTIFACT;
	case static_cast<int>(ThorAction::HERO_MEETING_REDISTRIBUTE_STACK):
		return ThorAction::HERO_MEETING_REDISTRIBUTE_STACK;
	case static_cast<int>(ThorAction::HERO_MEETING_ARTIFACTS_LEFT_TO_RIGHT):
		return ThorAction::HERO_MEETING_ARTIFACTS_LEFT_TO_RIGHT;
	case static_cast<int>(ThorAction::HERO_MEETING_ARTIFACTS_RIGHT_TO_LEFT):
		return ThorAction::HERO_MEETING_ARTIFACTS_RIGHT_TO_LEFT;
	case static_cast<int>(ThorAction::HERO_MEETING_SWAP_ARTIFACTS):
		return ThorAction::HERO_MEETING_SWAP_ARTIFACTS;
	case static_cast<int>(ThorAction::LOBBY_SET_DIFFICULTY):
		return ThorAction::LOBBY_SET_DIFFICULTY;
	case static_cast<int>(ThorAction::LOBBY_START_GAME):
		return ThorAction::LOBBY_START_GAME;
	case static_cast<int>(ThorAction::LOBBY_BACK):
		return ThorAction::LOBBY_BACK;
	case static_cast<int>(ThorAction::LOBBY_PREVIOUS_SCENARIO):
		return ThorAction::LOBBY_PREVIOUS_SCENARIO;
	case static_cast<int>(ThorAction::LOBBY_NEXT_SCENARIO):
		return ThorAction::LOBBY_NEXT_SCENARIO;
	case static_cast<int>(ThorAction::MAIN_MENU_CHOICE_1):
		return ThorAction::MAIN_MENU_CHOICE_1;
	case static_cast<int>(ThorAction::MAIN_MENU_CHOICE_2):
		return ThorAction::MAIN_MENU_CHOICE_2;
	case static_cast<int>(ThorAction::MAIN_MENU_CHOICE_3):
		return ThorAction::MAIN_MENU_CHOICE_3;
	case static_cast<int>(ThorAction::MAIN_MENU_CHOICE_4):
		return ThorAction::MAIN_MENU_CHOICE_4;
	case static_cast<int>(ThorAction::MAIN_MENU_CHOICE_5):
		return ThorAction::MAIN_MENU_CHOICE_5;
	case static_cast<int>(ThorAction::CAMPAIGN_PREVIOUS_SCENARIO):
		return ThorAction::CAMPAIGN_PREVIOUS_SCENARIO;
	case static_cast<int>(ThorAction::CAMPAIGN_NEXT_SCENARIO):
		return ThorAction::CAMPAIGN_NEXT_SCENARIO;
	case static_cast<int>(ThorAction::CAMPAIGN_SELECT_BONUS_1):
		return ThorAction::CAMPAIGN_SELECT_BONUS_1;
	case static_cast<int>(ThorAction::CAMPAIGN_SELECT_BONUS_2):
		return ThorAction::CAMPAIGN_SELECT_BONUS_2;
	case static_cast<int>(ThorAction::CAMPAIGN_SELECT_BONUS_3):
		return ThorAction::CAMPAIGN_SELECT_BONUS_3;
	case static_cast<int>(ThorAction::CAMPAIGN_START):
		return ThorAction::CAMPAIGN_START;
	case static_cast<int>(ThorAction::CAMPAIGN_BACK):
		return ThorAction::CAMPAIGN_BACK;
	case static_cast<int>(ThorAction::CAMPAIGN_BROWSER_SELECT): return ThorAction::CAMPAIGN_BROWSER_SELECT;
	case static_cast<int>(ThorAction::CAMPAIGN_BROWSER_PREVIOUS_PAGE): return ThorAction::CAMPAIGN_BROWSER_PREVIOUS_PAGE;
	case static_cast<int>(ThorAction::CAMPAIGN_BROWSER_NEXT_PAGE): return ThorAction::CAMPAIGN_BROWSER_NEXT_PAGE;
	case static_cast<int>(ThorAction::CAMPAIGN_BROWSER_BACK): return ThorAction::CAMPAIGN_BROWSER_BACK;
	case static_cast<int>(ThorAction::LOAD_BROWSER_SELECT): return ThorAction::LOAD_BROWSER_SELECT;
	case static_cast<int>(ThorAction::LOAD_BROWSER_PREVIOUS_PAGE): return ThorAction::LOAD_BROWSER_PREVIOUS_PAGE;
	case static_cast<int>(ThorAction::LOAD_BROWSER_NEXT_PAGE): return ThorAction::LOAD_BROWSER_NEXT_PAGE;
	case static_cast<int>(ThorAction::WINDOW_PREVIOUS): return ThorAction::WINDOW_PREVIOUS;
	case static_cast<int>(ThorAction::WINDOW_NEXT): return ThorAction::WINDOW_NEXT;
	case static_cast<int>(ThorAction::WINDOW_CLOSE): return ThorAction::WINDOW_CLOSE;
	case static_cast<int>(ThorAction::TOWN_OPEN_SERVICE): return ThorAction::TOWN_OPEN_SERVICE;
	case static_cast<int>(ThorAction::TOWN_HALL_BUILD): return ThorAction::TOWN_HALL_BUILD;
	default:
		return std::nullopt;
	}
}

std::optional<ThorTownService> thorTownServiceFromTarget(int targetId)
{
	if(targetId < 0 || targetId >= static_cast<int>(THOR_TOWN_SERVICE_COUNT))
		return std::nullopt;
	return static_cast<ThorTownService>(targetId);
}

bool isThorActionAllowedInContext(ThorAction action, const std::string & contextId)
{
	if(thorMainMenuChoice(contextId, action))
		return true;
	if(contextId == ThorContextIds::ADVENTURE_MAP)
		return isThorActionAllowedInAdventureMap(action);
	if(contextId == ThorContextIds::HERO_WINDOW)
		return action >= ThorAction::WINDOW_PREVIOUS && action <= ThorAction::WINDOW_CLOSE;
	if(contextId == ThorContextIds::TOWN_WINDOW)
		return (action >= ThorAction::WINDOW_PREVIOUS && action <= ThorAction::WINDOW_CLOSE)
			|| action == ThorAction::TOWN_OPEN_SERVICE;
	if(contextId == ThorContextIds::TOWN_HALL)
		return action == ThorAction::TOWN_HALL_BUILD || action == ThorAction::WINDOW_CLOSE;
	if(contextId == ThorContextIds::BATTLE)
		return action == ThorAction::BATTLE_WAIT || action == ThorAction::BATTLE_DEFEND;
	if(contextId == ThorContextIds::BATTLE_TACTICS)
		return action == ThorAction::BATTLE_TACTICS_NEXT || action == ThorAction::BATTLE_TACTICS_END;
	if(contextId == ThorContextIds::HERO_MEETING)
		return action == ThorAction::HERO_MEETING_MOVE_STACK || action == ThorAction::HERO_MEETING_TRANSFER_STACK
			|| action == ThorAction::HERO_MEETING_ARMY_LEFT_TO_RIGHT
			|| action == ThorAction::HERO_MEETING_ARMY_RIGHT_TO_LEFT || action == ThorAction::HERO_MEETING_SWAP_ARMIES
			|| action == ThorAction::HERO_MEETING_SPLIT_STACK || action == ThorAction::HERO_MEETING_TRANSFER_ARTIFACT
			|| action == ThorAction::HERO_MEETING_REDISTRIBUTE_STACK
			|| action == ThorAction::HERO_MEETING_ARTIFACTS_LEFT_TO_RIGHT
			|| action == ThorAction::HERO_MEETING_ARTIFACTS_RIGHT_TO_LEFT
			|| action == ThorAction::HERO_MEETING_SWAP_ARTIFACTS;
	if(contextId == ThorContextIds::LOBBY_NEW_GAME_SCENARIO || contextId == ThorContextIds::LOBBY_LOAD_GAME_SCENARIO)
		return (contextId == ThorContextIds::LOBBY_NEW_GAME_SCENARIO && action == ThorAction::LOBBY_SET_DIFFICULTY)
			|| action == ThorAction::LOBBY_START_GAME
			|| action == ThorAction::LOBBY_BACK || action == ThorAction::LOBBY_PREVIOUS_SCENARIO
			|| action == ThorAction::LOBBY_NEXT_SCENARIO
			|| (contextId == ThorContextIds::LOBBY_LOAD_GAME_SCENARIO
				&& action >= ThorAction::LOAD_BROWSER_SELECT && action <= ThorAction::LOAD_BROWSER_NEXT_PAGE);
	if(contextId == ThorContextIds::CAMPAIGN_BONUS_SELECTION)
		return action >= ThorAction::CAMPAIGN_PREVIOUS_SCENARIO && action <= ThorAction::CAMPAIGN_BACK;
	if(contextId == ThorContextIds::CAMPAIGN_BROWSER)
		return action >= ThorAction::CAMPAIGN_BROWSER_SELECT && action <= ThorAction::CAMPAIGN_BROWSER_BACK;
	return false;
}

bool thorWindowOwnerMatches(const ThorContextRecord & context,
	const std::string & expectedContext, int subjectId, bool active, bool top)
{
	return active && top && subjectId >= 0 && context.windowSubjectId == subjectId
		&& (expectedContext == ThorContextIds::HERO_WINDOW || expectedContext == ThorContextIds::TOWN_WINDOW)
		&& context.contextId == expectedContext;
}

std::optional<int> thorHeroWindowAdjacentIndex(int oneBasedSerial, int visibleCount, ThorAction action)
{
	if(oneBasedSerial < 1 || visibleCount < 1 || visibleCount > static_cast<int>(THOR_MAX_HEROES)
		|| oneBasedSerial > visibleCount)
		return std::nullopt;
	int index = oneBasedSerial - 1;
	if(action == ThorAction::WINDOW_PREVIOUS)
		--index;
	else if(action == ThorAction::WINDOW_NEXT)
		++index;
	else
		return std::nullopt;
	return index >= 0 && index < visibleCount ? std::optional<int>{index} : std::nullopt;
}

std::optional<ThorMainMenuChoice> thorMainMenuChoice(const std::string & contextId, ThorAction action)
{
	const int choice = static_cast<int>(action) - static_cast<int>(ThorAction::MAIN_MENU_CHOICE_1);
	if(choice < 0 || choice > 4)
		return std::nullopt;
	if(contextId == ThorContextIds::MAIN_MENU_CREDITS)
		return choice == 0 ? std::optional<ThorMainMenuChoice>{{0, "credits back"}} : std::nullopt;
	constexpr std::array<std::string_view, 5> main{"to new", "to load", "highscores", "to credits", "exit"};
	constexpr std::array<std::string_view, 5> newGame{"start single", "start multi", "to campaign", "start tutorial", "to main"};
	constexpr std::array<std::string_view, 5> loadGame{"load single", "load multi", "load campaign", "load tutorial", "to main"};
	constexpr std::array<std::string_view, 5> campaign{"campaigns sod", "campaigns roe", "campaigns ab", "start campaign", "to new"};
	if(contextId == ThorContextIds::MAIN_MENU)
		return ThorMainMenuChoice{static_cast<std::size_t>(choice), main[choice]};
	if(contextId == ThorContextIds::MAIN_MENU_NEW_GAME)
		return ThorMainMenuChoice{static_cast<std::size_t>(choice), newGame[choice]};
	if(contextId == ThorContextIds::MAIN_MENU_LOAD_GAME)
		return ThorMainMenuChoice{static_cast<std::size_t>(choice), loadGame[choice]};
	if(contextId == ThorContextIds::MAIN_MENU_CAMPAIGN)
		return ThorMainMenuChoice{static_cast<std::size_t>(choice), campaign[choice]};
	return std::nullopt;
}

bool thorMainMenuChoiceMatches(const ThorMainMenuChoice & choice,
	std::size_t configuredIndex, std::string_view command)
{
	return configuredIndex == choice.index && command == choice.command;
}

bool thorMainMenuChoiceAvailable(const ThorMainMenuChoice & choice,
	std::span<const ThorMainMenuButtonState> buttons)
{
	const auto found = std::find_if(buttons.begin(), buttons.end(), [&](const auto & button)
	{
		return button.configuredIndex == choice.index;
	});
	return found != buttons.end() && found->executable
		&& thorMainMenuChoiceMatches(choice, found->configuredIndex, found->command);
}

bool thorMainMenuTabMatches(const std::string & contextId, std::size_t index,
	std::span<const std::string> tabNames, std::size_t creditsIndex)
{
	if(tabNames.size() != creditsIndex + 1 || tabNames.empty() || tabNames[0] != "main")
		return false;
	std::string_view expected;
	if(contextId == ThorContextIds::MAIN_MENU)
		expected = "main";
	else if(contextId == ThorContextIds::MAIN_MENU_NEW_GAME)
		expected = "new";
	else if(contextId == ThorContextIds::MAIN_MENU_LOAD_GAME)
		expected = "load";
	else if(contextId == ThorContextIds::MAIN_MENU_CAMPAIGN)
		expected = "campaign";
	else if(contextId == ThorContextIds::MAIN_MENU_CREDITS)
		expected = "credits";
	else
		return false;
	const auto expectedIndex = thorMainMenuTabIndex(contextId, creditsIndex);
	return expectedIndex && index == *expectedIndex && index < tabNames.size() && tabNames[index] == expected
		&& std::count(tabNames.begin(), tabNames.end(), expected) == 1;
}

std::optional<std::size_t> thorMainMenuTabIndex(const std::string & contextId, std::size_t creditsIndex)
{
	if(contextId == ThorContextIds::MAIN_MENU) return 0;
	if(contextId == ThorContextIds::MAIN_MENU_NEW_GAME) return 1;
	if(contextId == ThorContextIds::MAIN_MENU_LOAD_GAME) return 2;
	if(contextId == ThorContextIds::MAIN_MENU_CAMPAIGN) return 3;
	if(contextId == ThorContextIds::MAIN_MENU_CREDITS) return creditsIndex;
	return std::nullopt;
}

std::string_view thorMainMenuNavigationTarget(std::string_view command)
{
	if(command == "to main") return ThorContextIds::MAIN_MENU;
	if(command == "to new") return ThorContextIds::MAIN_MENU_NEW_GAME;
	if(command == "to load") return ThorContextIds::MAIN_MENU_LOAD_GAME;
	if(command == "to campaign") return ThorContextIds::MAIN_MENU_CAMPAIGN;
	if(command == "to credits") return ThorContextIds::MAIN_MENU_CREDITS;
	return {};
}

std::optional<std::size_t> thorAdjacentScenarioPosition(
	std::span<const std::uint8_t> selectableEntries, std::size_t currentPosition, ThorAction action)
{
	if(currentPosition >= selectableEntries.size() || selectableEntries[currentPosition] == 0
		|| (action != ThorAction::LOBBY_PREVIOUS_SCENARIO && action != ThorAction::LOBBY_NEXT_SCENARIO
			&& action != ThorAction::CAMPAIGN_PREVIOUS_SCENARIO && action != ThorAction::CAMPAIGN_NEXT_SCENARIO))
		return std::nullopt;

	if(action == ThorAction::LOBBY_PREVIOUS_SCENARIO || action == ThorAction::CAMPAIGN_PREVIOUS_SCENARIO)
	{
		for(std::size_t position = currentPosition; position > 0;)
		{
			--position;
			if(selectableEntries[position] != 0)
				return position;
		}
		return std::nullopt;
	}

	for(std::size_t position = currentPosition + 1; position < selectableEntries.size(); ++position)
		if(selectableEntries[position] != 0)
			return position;
	return std::nullopt;
}

std::vector<std::size_t> thorSelectableBrowserPositions(std::span<const std::uint8_t> selectableEntries)
{
	std::vector<std::size_t> positions;
	if(selectableEntries.size() > 10000)
		return positions;
	for(std::size_t index = 0; index < selectableEntries.size(); ++index)
		if(selectableEntries[index] != 0)
			positions.push_back(index);
	return positions;
}

bool isThorActionAllowedInAdventureMap(ThorAction action)
{
	return action == ThorAction::OPEN_KINGDOM_OVERVIEW
		|| action == ThorAction::OPEN_QUEST_LOG
		|| action == ThorAction::OPEN_PUZZLE_MAP
		|| action == ThorAction::OPEN_SAVE_GAME
		|| action == ThorAction::NEXT_HERO
		|| action == ThorAction::MOVE_HERO
		|| action == ThorAction::TOGGLE_HERO_SLEEP
		|| action == ThorAction::END_TURN
		|| action == ThorAction::SELECT_HERO
		|| action == ThorAction::SELECT_TOWN;
}

std::optional<ThorBulkArtifactOperation> thorBulkArtifactOperation(ThorAction action)
{
	switch(action)
	{
	case ThorAction::HERO_MEETING_ARTIFACTS_LEFT_TO_RIGHT:
		return ThorBulkArtifactOperation::LEFT_TO_RIGHT;
	case ThorAction::HERO_MEETING_ARTIFACTS_RIGHT_TO_LEFT:
		return ThorBulkArtifactOperation::RIGHT_TO_LEFT;
	case ThorAction::HERO_MEETING_SWAP_ARTIFACTS:
		return ThorBulkArtifactOperation::SWAP;
	default:
		return std::nullopt;
	}
}

bool canExecuteThorBulkArtifactAction(const ThorContextRecord & context,
	const ThorHeroMeetingArtifacts & currentArtifacts, bool makingTurn, bool pickedArtifact,
	int leftOwner, int rightOwner, int playerId)
{
	return context.contextId == ThorContextIds::HERO_MEETING && context.heroMeetingArtifacts
		&& *context.heroMeetingArtifacts == currentArtifacts && makingTurn && !pickedArtifact
		&& leftOwner == playerId && rightOwner == playerId;
}

bool shouldRestoreThorBulkArtifactActions(bool serverSuccess, std::uint64_t enabledActionMask)
{
	return !serverSuccess || enabledActionMask == 0;
}

bool isThorActionHapticEligible(ThorAction action)
{
	return action >= ThorAction::MOVE_HERO && action <= ThorAction::HERO_MEETING_SWAP_ARTIFACTS
		&& action != ThorAction::NEXT_HERO && action != ThorAction::SELECT_HERO
		&& action != ThorAction::SELECT_TOWN;
}

bool isThorActionHapticDeferredUntilServerResult(ThorAction action)
{
	return action == ThorAction::HERO_MEETING_REDISTRIBUTE_STACK
		|| action == ThorAction::HERO_MEETING_TRANSFER_ARTIFACT
		|| thorBulkArtifactOperation(action).has_value();
}

bool isThorActionArtifactMutation(ThorAction action)
{
	return action == ThorAction::HERO_MEETING_TRANSFER_ARTIFACT
		|| thorBulkArtifactOperation(action).has_value();
}

bool thorHeroMeetingArtifactsChanged(const ThorHeroMeetingArtifacts & before,
	const ThorHeroMeetingArtifacts & after)
{
	if(before.leftHeroId != after.leftHeroId || before.rightHeroId != after.rightHeroId
		|| before.artifactSlots.size() != after.artifactSlots.size())
		return true;
	for(std::size_t index = 0; index < before.artifactSlots.size(); ++index)
	{
		const auto & oldSlot = before.artifactSlots[index];
		const auto & newSlot = after.artifactSlots[index];
		if(oldSlot.heroId != newSlot.heroId || oldSlot.position != newSlot.position
			|| oldSlot.occupied != newSlot.occupied || oldSlot.instanceId != newSlot.instanceId)
			return true;
	}
	return false;
}

std::optional<ThorActionAcceptance> thorActionAcceptance(
	const ThorActionRequest & request, ThorActionValidation validation, bool executed)
{
	if(validation != ThorActionValidation::VALID || !executed || request.revision == 0
		|| !isThorActionHapticEligible(request.action))
		return std::nullopt;
	return ThorActionAcceptance{request.revision, request.action};
}

ThorActionValidation validateThorActionRequest(const ThorActionRequest & request, const ThorContextRecord & context)
{
	if(!thorActionFromId(static_cast<int>(request.action)))
		return ThorActionValidation::UNKNOWN_ACTION;
	if(request.revision != context.revision)
		return ThorActionValidation::STALE_REVISION;
	if(!isThorActionAllowedInContext(request.action, context.contextId))
		return ThorActionValidation::WRONG_CONTEXT;
	if((context.enabledActionMask & thorActionMask(request.action)) == 0)
		return ThorActionValidation::UNAVAILABLE;
	if(request.action == ThorAction::LOBBY_SET_DIFFICULTY)
	{
		if(request.targetId < 0 || request.targetId > 4 || request.sourceArmyId != -1 || request.sourceSlot != -1
			|| request.destinationArmyId != -1 || request.destinationSlot != -1 || request.amount != -1)
			return ThorActionValidation::INVALID_TARGET;
	}
	else if(request.action == ThorAction::LOBBY_START_GAME || request.action == ThorAction::LOBBY_BACK
		|| request.action == ThorAction::LOBBY_PREVIOUS_SCENARIO || request.action == ThorAction::LOBBY_NEXT_SCENARIO)
	{
		if(request.targetId != -1 || request.sourceArmyId != -1 || request.sourceSlot != -1
			|| request.destinationArmyId != -1 || request.destinationSlot != -1 || request.amount != -1)
			return ThorActionValidation::INVALID_TARGET;
	}
	else if(request.action == ThorAction::CAMPAIGN_BROWSER_SELECT || request.action == ThorAction::LOAD_BROWSER_SELECT)
	{
		if(request.targetId < 0 || request.sourceArmyId != -1 || request.sourceSlot != -1
			|| request.destinationArmyId != -1 || request.destinationSlot != -1 || request.amount != -1
			|| std::none_of(context.browserEntries.begin(), context.browserEntries.end(), [&](const auto & entry)
			{
				return entry.target == request.targetId && entry.enabled;
			}))
			return ThorActionValidation::INVALID_TARGET;
	}
	else if((request.action >= ThorAction::CAMPAIGN_PREVIOUS_SCENARIO && request.action <= ThorAction::CAMPAIGN_BACK)
		|| (request.action >= ThorAction::CAMPAIGN_BROWSER_PREVIOUS_PAGE && request.action <= ThorAction::CAMPAIGN_BROWSER_BACK)
		|| request.action == ThorAction::LOAD_BROWSER_PREVIOUS_PAGE || request.action == ThorAction::LOAD_BROWSER_NEXT_PAGE
		|| (request.action >= ThorAction::WINDOW_PREVIOUS && request.action <= ThorAction::WINDOW_CLOSE))
	{
		if(request.targetId != -1 || request.sourceArmyId != -1 || request.sourceSlot != -1
			|| request.destinationArmyId != -1 || request.destinationSlot != -1 || request.amount != -1)
			return ThorActionValidation::INVALID_TARGET;
	}
	else if(request.action == ThorAction::TOWN_OPEN_SERVICE)
	{
		if(!thorTownServiceFromTarget(request.targetId) || request.sourceArmyId != -1 || request.sourceSlot != -1
			|| request.destinationArmyId != -1 || request.destinationSlot != -1 || request.amount != -1
			|| std::none_of(context.browserEntries.begin(), context.browserEntries.end(), [&](const auto & entry)
			{
				return entry.target == request.targetId && entry.enabled;
			}))
			return ThorActionValidation::INVALID_TARGET;
	}
	else if(request.action == ThorAction::TOWN_HALL_BUILD)
	{
		if(request.targetId < 0 || request.sourceArmyId != -1 || request.sourceSlot != -1
			|| request.destinationArmyId != -1 || request.destinationSlot != -1 || request.amount != -1
			|| context.actionSubjectId < 0 || context.browserEntries.size() > THOR_MAX_TOWN_HALL_BUILDINGS
			|| context.browserNativeKeys.size() != context.browserEntries.size())
			return ThorActionValidation::INVALID_TARGET;
		const auto found = std::find_if(context.browserEntries.begin(), context.browserEntries.end(), [&](const auto & entry)
		{
			return entry.target == request.targetId && entry.enabled && !entry.completed;
		});
		if(found == context.browserEntries.end())
			return ThorActionValidation::INVALID_TARGET;
		const auto index = static_cast<std::size_t>(found - context.browserEntries.begin());
		if(context.browserNativeKeys[index] != std::to_string(request.targetId))
			return ThorActionValidation::INVALID_TARGET;
	}
	else if(thorMainMenuChoice(context.contextId, request.action))
	{
		if(request.targetId != -1 || request.sourceArmyId != -1 || request.sourceSlot != -1
			|| request.destinationArmyId != -1 || request.destinationSlot != -1 || request.amount != -1)
			return ThorActionValidation::INVALID_TARGET;
	}
	if(request.action == ThorAction::SELECT_HERO || request.action == ThorAction::SELECT_TOWN)
	{
		if(request.targetId < 0)
			return ThorActionValidation::INVALID_TARGET;
		const auto hasTarget = request.action == ThorAction::SELECT_HERO
			? std::any_of(context.heroes.begin(), context.heroes.end(), [&](const auto & hero) { return hero.id == request.targetId; })
			: std::any_of(context.towns.begin(), context.towns.end(), [&](const auto & town) { return town.id == request.targetId; });
		if(!hasTarget)
			return ThorActionValidation::INVALID_TARGET;
	}
	if(request.action == ThorAction::HERO_MEETING_MOVE_STACK)
	{
		if(!context.heroMeetingArmies || !context.heroMeetingArmies->locallyControllable || request.targetId < 0
			|| request.targetId >= static_cast<int>(THOR_HERO_MEETING_SLOT_KEY_COUNT)
			|| request.sourceArmyId != -1 || request.sourceSlot != -1
			|| request.destinationArmyId != -1 || request.destinationSlot != -1 || request.amount != -1)
			return ThorActionValidation::INVALID_TARGET;
		const auto & armies = *context.heroMeetingArmies;
		const auto slotsPerArmy = static_cast<int>(THOR_HERO_MEETING_ARMY_SIZE);
		const auto sourceIsLeft = request.targetId < slotsPerArmy;
		const auto sourceSlot = request.targetId % slotsPerArmy;
		const auto sourceArmyId = sourceIsLeft ? armies.leftArmyId : armies.rightArmyId;
		const auto & source = (sourceIsLeft ? armies.leftSlots : armies.rightSlots)[sourceSlot];
		if(!source.occupied || source.armyId != sourceArmyId || source.slot != sourceSlot)
			return ThorActionValidation::INVALID_TARGET;
	}
	if(request.action == ThorAction::HERO_MEETING_TRANSFER_STACK)
	{
		if(!context.heroMeetingArmies || !context.heroMeetingArmies->locallyControllable || request.sourceArmyId != -1
			|| request.sourceSlot != -1 || request.destinationArmyId != -1 || request.destinationSlot != -1
			|| request.amount != -1)
			return ThorActionValidation::INVALID_TARGET;
		const auto pair = decodeThorHeroMeetingTransferPair(request.targetId);
		if(!pair)
			return ThorActionValidation::INVALID_TARGET;
		const auto & armies = *context.heroMeetingArmies;
		const auto & source = (pair->sourceIsLeft ? armies.leftSlots : armies.rightSlots)[pair->sourceSlot];
		const auto & destination = (pair->destinationIsLeft ? armies.leftSlots : armies.rightSlots)[pair->destinationSlot];
		const int sourceArmyId = pair->sourceIsLeft ? armies.leftArmyId : armies.rightArmyId;
		const int destinationArmyId = pair->destinationIsLeft ? armies.leftArmyId : armies.rightArmyId;
		if(!source.occupied || source.armyId != sourceArmyId || source.slot != pair->sourceSlot
			|| destination.armyId != destinationArmyId || destination.slot != pair->destinationSlot)
			return ThorActionValidation::INVALID_TARGET;
	}
	if(request.action == ThorAction::HERO_MEETING_SPLIT_STACK)
	{
		if(request.targetId != -1 || !context.heroMeetingArmies
			|| !canThorHeroMeetingSplitStack(*context.heroMeetingArmies, request.sourceArmyId, request.sourceSlot,
				request.destinationArmyId, request.destinationSlot, request.amount))
			return ThorActionValidation::INVALID_TARGET;
	}
	if(request.action == ThorAction::HERO_MEETING_REDISTRIBUTE_STACK)
		return ThorActionValidation::INVALID_TARGET; // Action 22 requires its dedicated bounded payload.
	if(request.action == ThorAction::HERO_MEETING_ARTIFACTS_LEFT_TO_RIGHT
		|| request.action == ThorAction::HERO_MEETING_ARTIFACTS_RIGHT_TO_LEFT
		|| request.action == ThorAction::HERO_MEETING_SWAP_ARTIFACTS)
	{
		if(!context.heroMeetingArmies || !context.heroMeetingArmies->locallyControllable
			|| !context.heroMeetingArtifacts
			|| context.heroMeetingArtifacts->artifactSlots.size() != THOR_HERO_MEETING_ARTIFACT_COUNT
			|| context.heroMeetingArtifacts->leftHeroId != context.heroMeetingArmies->leftHeroId
			|| context.heroMeetingArtifacts->rightHeroId != context.heroMeetingArmies->rightHeroId
			|| request.targetId != -1 || request.sourceArmyId != -1
			|| request.sourceSlot != -1 || request.destinationArmyId != -1
			|| request.destinationSlot != -1 || request.amount != -1)
			return ThorActionValidation::INVALID_TARGET;
	}
	if(request.action == ThorAction::HERO_MEETING_TRANSFER_ARTIFACT)
	{
		if(!context.heroMeetingArtifacts || request.sourceArmyId != -1 || request.sourceSlot != -1
			|| request.destinationArmyId != -1 || request.destinationSlot != -1 || request.amount != -1)
			return ThorActionValidation::INVALID_TARGET;
		const auto pair = decodeThorHeroMeetingArtifactPair(request.targetId);
		if(!pair)
			return ThorActionValidation::INVALID_TARGET;
		const auto & artifacts = context.heroMeetingArtifacts->artifactSlots;
		if(artifacts.size() != THOR_HERO_MEETING_ARTIFACT_COUNT || !artifacts[pair->first].occupied
			|| artifacts[pair->first].locked || artifacts[pair->second].locked)
			return ThorActionValidation::INVALID_TARGET;
	}
	if((request.action == ThorAction::HERO_MEETING_ARMY_LEFT_TO_RIGHT || request.action == ThorAction::HERO_MEETING_ARMY_RIGHT_TO_LEFT
		|| request.action == ThorAction::HERO_MEETING_SWAP_ARMIES)
		&& (!context.heroMeetingArmies || !context.heroMeetingArmies->locallyControllable))
		return ThorActionValidation::INVALID_TARGET;
	return ThorActionValidation::VALID;
}

ThorActionValidation validateThorLobbyActionRequest(const ThorActionRequest & request,
	const ThorContextRecord & context, bool exactTopOwner, bool scenarioTabActive, bool authoritative,
	bool mapAvailable, bool startAvailable, bool scenarioNavigationAvailable)
{
	const auto requestValidation = validateThorActionRequest(request, context);
	if(requestValidation != ThorActionValidation::VALID)
		return requestValidation;
	if(!exactTopOwner || !scenarioTabActive)
		return ThorActionValidation::WRONG_CONTEXT;
	if(request.action == ThorAction::LOBBY_SET_DIFFICULTY && (!authoritative || !mapAvailable))
		return ThorActionValidation::UNAVAILABLE;
	if(request.action == ThorAction::LOBBY_START_GAME && (!mapAvailable || !startAvailable))
		return ThorActionValidation::UNAVAILABLE;
	if((request.action == ThorAction::LOBBY_PREVIOUS_SCENARIO || request.action == ThorAction::LOBBY_NEXT_SCENARIO)
		&& (!authoritative || !mapAvailable || !scenarioNavigationAvailable))
		return ThorActionValidation::UNAVAILABLE;
	return ThorActionValidation::VALID;
}

ThorActionValidation validateThorHeroMeetingRedistributionRequest(
	const ThorHeroMeetingRedistributionRequest & request, const ThorContextRecord & context)
{
	if(request.action != ThorAction::HERO_MEETING_REDISTRIBUTE_STACK)
		return ThorActionValidation::UNKNOWN_ACTION;
	if(request.revision != context.revision)
		return ThorActionValidation::STALE_REVISION;
	if(!isThorActionAllowedInContext(request.action, context.contextId))
		return ThorActionValidation::WRONG_CONTEXT;
	if((context.enabledActionMask & thorActionMask(request.action)) == 0)
		return ThorActionValidation::UNAVAILABLE;
	if(!context.heroMeetingArmies || !context.heroMeetingArmies->locallyControllable
		|| request.destinationCount == 0
		|| request.destinationCount > THOR_MAX_HERO_MEETING_REDISTRIBUTION_DESTINATIONS
		|| request.leftHeroId < 0 || request.rightHeroId < 0 || request.leftHeroId == request.rightHeroId
		|| request.leftHeroId != context.heroMeetingArmies->leftHeroId
		|| request.rightHeroId != context.heroMeetingArmies->rightHeroId
		|| request.sourceCount < 2 || request.sourceCreatureId < 0 || request.sourceSlot < 0
		|| request.sourceSlot >= static_cast<int>(THOR_HERO_MEETING_ARMY_SIZE)
		|| (request.sourceArmyId != request.leftHeroId && request.sourceArmyId != request.rightHeroId))
		return ThorActionValidation::INVALID_TARGET;

	const auto & armies = *context.heroMeetingArmies;
	const bool sourceIsLeft = request.sourceArmyId == request.leftHeroId;
	const auto & source = (sourceIsLeft ? armies.leftSlots : armies.rightSlots)[request.sourceSlot];
	if(source.armyId != request.sourceArmyId || source.slot != request.sourceSlot || !source.occupied
		|| source.creatureId != request.sourceCreatureId || source.count != request.sourceCount)
		return ThorActionValidation::INVALID_TARGET;

	std::int64_t total = 0;
	for(std::size_t index = 0; index < request.destinationCount; ++index)
	{
		const auto & target = request.destinations[index];
		if((target.armyId != request.leftHeroId && target.armyId != request.rightHeroId)
			|| target.slot < 0 || target.slot >= static_cast<int>(THOR_HERO_MEETING_ARMY_SIZE)
			|| target.amount <= 0 || (target.armyId == request.sourceArmyId && target.slot == request.sourceSlot))
			return ThorActionValidation::INVALID_TARGET;
		for(std::size_t earlier = 0; earlier < index; ++earlier)
		{
			if(request.destinations[earlier].armyId == target.armyId
				&& request.destinations[earlier].slot == target.slot)
				return ThorActionValidation::INVALID_TARGET;
		}
		const bool targetIsLeft = target.armyId == request.leftHeroId;
		const auto & destination = (targetIsLeft ? armies.leftSlots : armies.rightSlots)[target.slot];
		if(destination.armyId != target.armyId || destination.slot != target.slot
			|| (destination.occupied && (destination.creatureId != request.sourceCreatureId
				|| destination.count <= 0 || destination.count > std::numeric_limits<int>::max() - target.amount)))
			return ThorActionValidation::INVALID_TARGET;
		total += target.amount;
		if(total > static_cast<std::int64_t>(request.sourceCount) - 1)
			return ThorActionValidation::INVALID_TARGET;
	}
	return total > 0 ? ThorActionValidation::VALID : ThorActionValidation::INVALID_TARGET;
}

bool ThorActionQueue::submit(ThorActionRequest request)
{
	std::lock_guard lock(mutex);
	if(count == capacity)
		return false;

	requests[(first + count) % capacity] = request;
	++count;
	return true;
}

std::optional<ThorActionRequest> ThorActionQueue::pop()
{
	std::lock_guard lock(mutex);
	if(count == 0)
		return std::nullopt;

	auto result = requests[first];
	first = (first + 1) % capacity;
	--count;
	return result;
}

void ThorActionQueue::clear()
{
	std::lock_guard lock(mutex);
	first = 0;
	count = 0;
}

std::size_t ThorActionQueue::size() const
{
	std::lock_guard lock(mutex);
	return count;
}

ThorActionQueue & thorActionQueue()
{
	static ThorActionQueue queue;
	return queue;
}

bool ThorHeroMeetingRedistributionQueue::submit(ThorHeroMeetingRedistributionRequest request)
{
	if(request.action != ThorAction::HERO_MEETING_REDISTRIBUTE_STACK
		|| request.destinationCount == 0
		|| request.destinationCount > THOR_MAX_HERO_MEETING_REDISTRIBUTION_DESTINATIONS)
		return false;
	std::array<int, THOR_MAX_HERO_MEETING_REDISTRIBUTION_DESTINATIONS> armyIds{};
	std::array<int, THOR_MAX_HERO_MEETING_REDISTRIBUTION_DESTINATIONS> slots{};
	std::array<int, THOR_MAX_HERO_MEETING_REDISTRIBUTION_DESTINATIONS> amounts{};
	for(std::size_t index = 0; index < request.destinationCount; ++index)
	{
		armyIds[index] = request.destinations[index].armyId;
		slots[index] = request.destinations[index].slot;
		amounts[index] = request.destinations[index].amount;
	}
	const auto decoded = decodeThorHeroMeetingRedistributionRequest(request.revision, request.leftHeroId,
		request.rightHeroId, request.sourceArmyId, request.sourceSlot, request.sourceCreatureId,
		request.sourceCount,
		std::span<const int>(armyIds.data(), request.destinationCount),
		std::span<const int>(slots.data(), request.destinationCount),
		std::span<const int>(amounts.data(), request.destinationCount));
	if(!decoded)
		return false;
	request = *decoded;
	std::lock_guard lock(mutex);
	if(count == capacity)
		return false;
	requests[(first + count) % capacity] = request;
	++count;
	return true;
}

std::optional<ThorHeroMeetingRedistributionRequest> ThorHeroMeetingRedistributionQueue::pop()
{
	std::lock_guard lock(mutex);
	if(count == 0)
		return std::nullopt;
	auto request = requests[first];
	first = (first + 1) % capacity;
	--count;
	return request;
}

void ThorHeroMeetingRedistributionQueue::clear()
{
	std::lock_guard lock(mutex);
	first = 0;
	count = 0;
}

std::size_t ThorHeroMeetingRedistributionQueue::size() const
{
	std::lock_guard lock(mutex);
	return count;
}

ThorHeroMeetingRedistributionQueue & thorHeroMeetingRedistributionQueue()
{
	static ThorHeroMeetingRedistributionQueue queue;
	return queue;
}
