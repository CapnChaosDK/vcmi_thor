#include "ThorContext.h"
#include "ThorAction.h"

#include <utility>

namespace
{
	bool sameSemanticState(const ThorContextRecord & lhs, const ThorContextRecord & rhs)
	{
		return lhs.contextId == rhs.contextId
			&& lhs.menuModalType == rhs.menuModalType
			&& lhs.menuModalSourceContext == rhs.menuModalSourceContext
			&& lhs.nativeOwnerToken == rhs.nativeOwnerToken
			&& lhs.nativeParentToken == rhs.nativeParentToken
			&& lhs.title == rhs.title
			&& lhs.status == rhs.status
			&& lhs.details == rhs.details
			&& lhs.enabledActionMask == rhs.enabledActionMask
			&& lhs.activeActionMask == rhs.activeActionMask
			&& lhs.scenarioSelectionRevision == rhs.scenarioSelectionRevision
			&& lhs.campaignSelectionRevision == rhs.campaignSelectionRevision
			&& lhs.browserPage == rhs.browserPage
			&& lhs.browserPageCount == rhs.browserPageCount
			&& lhs.browserEntries == rhs.browserEntries
			&& lhs.browserNativeKeys == rhs.browserNativeKeys
			&& lhs.selectedHeroId == rhs.selectedHeroId
			&& lhs.heroPortraitAssetKey == rhs.heroPortraitAssetKey
			&& lhs.actionSubjectId == rhs.actionSubjectId
			&& lhs.actionEpoch == rhs.actionEpoch
			&& lhs.windowSubjectId == rhs.windowSubjectId
			&& lhs.heroes == rhs.heroes
			&& lhs.towns == rhs.towns
			&& lhs.adventureMap == rhs.adventureMap
			&& lhs.heroMeetingArmies == rhs.heroMeetingArmies
			&& lhs.heroMeetingArtifacts == rhs.heroMeetingArtifacts
			&& lhs.heroManagement == rhs.heroManagement
			&& lhs.townManagement == rhs.townManagement
			&& lhs.recruitment == rhs.recruitment;
	}

	void normalizeActionSubject(ThorContextRecord & context)
	{
		if(context.contextId != ThorContextIds::BATTLE && context.contextId != ThorContextIds::BATTLE_TACTICS
			&& context.contextId != ThorContextIds::TOWN_HALL
			&& context.contextId != ThorContextIds::BUILD_CONFIRMATION
			&& context.contextId != ThorContextIds::TOWN_RECRUITMENT_QUICK
			&& context.contextId != ThorContextIds::TOWN_RECRUITMENT_DWELLING)
			context.actionSubjectId = -1;
		if(context.contextId != ThorContextIds::HERO_WINDOW && context.contextId != ThorContextIds::TOWN_WINDOW
			&& context.contextId != ThorContextIds::BUILD_CONFIRMATION)
			context.windowSubjectId = -1;
	}

	void boundTextFields(ThorContextRecord & context)
	{
		context.title = thorBoundedText(std::move(context.title));
		context.status = thorBoundedText(std::move(context.status));
		context.menuModalSourceContext = thorBoundedText(std::move(context.menuModalSourceContext), 64);
		if(context.menuModalType == ThorMenuModalType::NONE)
		{
			context.menuModalSourceContext.clear();
			context.nativeOwnerToken = nullptr;
			context.nativeParentToken = nullptr;
		}
		for(auto & detail : context.details)
			detail = thorBoundedText(std::move(detail));
		if(context.contextId != ThorContextIds::ADVENTURE_MAP || context.heroes.size() > THOR_MAX_HEROES)
			context.heroes.clear();
		if(context.contextId != ThorContextIds::ADVENTURE_MAP || context.towns.size() > THOR_MAX_TOWNS)
			context.towns.clear();
		if(context.contextId != ThorContextIds::ADVENTURE_MAP || !context.adventureMap || !context.adventureMap->valid()
			|| std::any_of(context.adventureMap->markers.begin(), context.adventureMap->markers.end(),
				[&](const auto & marker)
				{
					return marker.action == static_cast<int>(ThorAction::SELECT_HERO)
						? std::none_of(context.heroes.begin(), context.heroes.end(),
							[&](const auto & hero) { return hero.id == marker.id && hero.selected == marker.selected; })
						: std::none_of(context.towns.begin(), context.towns.end(),
							[&](const auto & town) { return town.id == marker.id && town.selected == marker.selected; });
				}))
		{
			context.adventureMap.reset();
			context.enabledActionMask &= ~(thorActionMask(ThorAction::ADVENTURE_CENTER_VIEW)
				| thorActionMask(ThorAction::ADVENTURE_SET_MAP_LEVEL));
		}
		if((context.contextId != ThorContextIds::ADVENTURE_MAP && context.contextId != ThorContextIds::HERO_WINDOW)
			|| !isThorHeroPortraitVisualAssetKey(context.heroPortraitAssetKey))
			context.heroPortraitAssetKey = 0;
		if(context.contextId != ThorContextIds::HERO_MEETING)
		{
			context.heroMeetingArmies.reset();
			context.heroMeetingArtifacts.reset();
		}
		if(context.contextId != ThorContextIds::HERO_WINDOW)
			context.heroManagement.reset();
		if(context.contextId != ThorContextIds::TOWN_RECRUITMENT_QUICK
			&& context.contextId != ThorContextIds::TOWN_RECRUITMENT_DWELLING)
			context.recruitment.reset();
		if(context.recruitment)
		{
			auto & recruitment = *context.recruitment;
			const bool quick = context.contextId == ThorContextIds::TOWN_RECRUITMENT_QUICK;
			bool valid = recruitment.townId >= 0 && recruitment.destinationArmyId >= 0
				&& recruitment.destinationArmyFreeSlots >= 0
				&& recruitment.destinationArmyFreeSlots <= static_cast<int>(GameConstants::ARMY_SIZE)
				&& recruitment.rows.size() <= THOR_MAX_RECRUITMENT_ROWS
				&& (quick ? recruitment.mode == ThorRecruitmentMode::QUICK_TOWN && recruitment.dwellingLevel == -1
					: recruitment.mode == ThorRecruitmentMode::TOWN_DWELLING && recruitment.dwellingLevel >= 0)
				&& recruitment.townName.size() <= 128 && recruitment.totalCost.size() <= 128;
			std::size_t selectedRows = 0;
			bool selectedTargetFound = recruitment.selectedTarget == -1;
			for(std::size_t index = 0; valid && index < recruitment.rows.size(); ++index)
			{
				auto & row = recruitment.rows[index];
				valid = row.target >= 0 && row.creatureId >= 0 && !row.name.empty() && row.name.size() <= 128
					&& row.unitCost.size() <= 128 && row.selectedCost.size() <= 128
					&& row.availableCount >= 0 && row.selectedAmount >= 0 && row.maximumAmount >= 0
					&& row.selectedAmount <= row.maximumAmount && row.maximumAmount <= row.availableCount
					&& row.variantCount >= 1 && row.variantCount <= static_cast<int>(THOR_MAX_RECRUITMENT_VARIANTS)
					&& row.variantIndex >= 0 && row.variantIndex < row.variantCount
					&& (!row.enabled || row.armyAvailable)
					&& (recruitment.mode != ThorRecruitmentMode::TOWN_DWELLING
						|| row.selected || row.selectedAmount == 0)
					&& (row.visualAssetKey == 0 || row.visualAssetKey == thorCreatureVisualAssetKey(row.creatureId));
				if(!valid)
					break;
				row.name = thorBoundedText(std::move(row.name));
				row.unitCost = thorBoundedText(std::move(row.unitCost));
				row.selectedCost = thorBoundedText(std::move(row.selectedCost));
				row.visualAssetKey = thorCreatureVisualAssetKey(row.creatureId);
				selectedRows += row.selected;
				selectedTargetFound |= row.target == recruitment.selectedTarget && row.selected;
				for(std::size_t earlier = 0; earlier < index; ++earlier)
					valid = valid && recruitment.rows[earlier].target != row.target;
			}
			valid = valid && (recruitment.selectedTarget == -1 || selectedTargetFound)
				&& selectedRows <= 1
				&& (recruitment.selectedTarget == -1 ? selectedRows == 0 : selectedRows == 1)
				&& (!recruitment.canBuy || (recruitment.locallyControllable
					&& std::any_of(recruitment.rows.begin(), recruitment.rows.end(),
						[](const auto & row) { return row.selectedAmount > 0 && row.enabled && row.armyAvailable; })))
				&& (recruitment.locallyControllable || std::none_of(recruitment.rows.begin(), recruitment.rows.end(),
					[](const auto & row) { return row.enabled; }));
			if(!valid)
			{
				context.recruitment.reset();
				context.enabledActionMask &= ~(thorActionMask(ThorAction::RECRUITMENT_EDIT)
					| thorActionMask(ThorAction::RECRUITMENT_BUY)
					| thorActionMask(ThorAction::WINDOW_CLOSE));
			}
			else
			{
				recruitment.townName = thorBoundedText(std::move(recruitment.townName));
				recruitment.totalCost = thorBoundedText(std::move(recruitment.totalCost));
				if(!recruitment.locallyControllable)
					context.enabledActionMask &= ~(thorActionMask(ThorAction::RECRUITMENT_EDIT)
						| thorActionMask(ThorAction::RECRUITMENT_BUY));
				else if(!recruitment.canBuy)
					context.enabledActionMask &= ~thorActionMask(ThorAction::RECRUITMENT_BUY);
			}
		}
		for(auto & hero : context.heroes)
			hero.name = thorBoundedText(std::move(hero.name));
		for(auto & town : context.towns)
			town.name = thorBoundedText(std::move(town.name));
		bool exactTownServiceTargets = context.browserEntries.size() == THOR_TOWN_SERVICE_COUNT;
		for(std::size_t index = 0; exactTownServiceTargets && index < context.browserEntries.size(); ++index)
			exactTownServiceTargets = context.browserEntries[index].target == static_cast<int>(index);
		const bool townServices = context.contextId == ThorContextIds::TOWN_WINDOW
			&& context.browserPage == 0 && context.browserPageCount == 1
			&& exactTownServiceTargets
			&& context.browserNativeKeys.empty();
		const bool townHall = context.contextId == ThorContextIds::TOWN_HALL
			&& context.actionSubjectId >= 0 && context.browserPage == 0
			&& context.browserPageCount == static_cast<int>((context.browserEntries.size()
				+ THOR_TOWN_HALL_PAGE_SIZE - 1) / THOR_TOWN_HALL_PAGE_SIZE)
			&& context.browserNativeKeys.size() == context.browserEntries.size();
		const bool nativeBrowser = (context.contextId == ThorContextIds::CAMPAIGN_BROWSER
			|| context.contextId == ThorContextIds::LOBBY_LOAD_GAME_SCENARIO)
			&& context.browserNativeKeys.size() == context.browserEntries.size();
		if((!townServices && !nativeBrowser && !townHall)
			|| context.browserEntries.size() > (context.contextId == ThorContextIds::TOWN_HALL
				? THOR_MAX_TOWN_HALL_BUILDINGS : THOR_BROWSER_MAX_ROWS)
			|| context.browserPage < 0 || context.browserPageCount < 0
			|| (context.browserPageCount != 0 && context.browserPage >= context.browserPageCount)
			|| (context.browserPageCount == 0 && !context.browserEntries.empty()))
		{
			context.enabledActionMask &= ~(thorActionMask(ThorAction::CAMPAIGN_BROWSER_SELECT)
				| thorActionMask(ThorAction::CAMPAIGN_BROWSER_PREVIOUS_PAGE)
				| thorActionMask(ThorAction::CAMPAIGN_BROWSER_NEXT_PAGE)
				| thorActionMask(ThorAction::LOAD_BROWSER_SELECT)
				| thorActionMask(ThorAction::LOAD_BROWSER_PREVIOUS_PAGE)
				| thorActionMask(ThorAction::LOAD_BROWSER_NEXT_PAGE)
				| thorActionMask(ThorAction::TOWN_OPEN_SERVICE)
				| thorActionMask(ThorAction::TOWN_HALL_BUILD));
			context.browserEntries.clear();
			context.browserNativeKeys.clear();
			context.browserPage = 0;
			context.browserPageCount = 0;
		}
		for(auto & entry : context.browserEntries)
			entry.label = thorBoundedText(std::move(entry.label));
		if(context.contextId == ThorContextIds::TOWN_HALL)
		{
			for(std::size_t index = 0; index < context.browserEntries.size(); ++index)
			{
				const auto & entry = context.browserEntries[index];
				if(entry.target < 0 || (entry.enabled && entry.completed)
					|| context.browserNativeKeys[index] != std::to_string(entry.target)
					|| std::any_of(context.browserEntries.begin(), context.browserEntries.begin() + index,
						[&](const auto & earlier) { return earlier.target == entry.target; }))
				{
					context.enabledActionMask &= ~thorActionMask(ThorAction::TOWN_HALL_BUILD);
					context.browserEntries.clear();
					context.browserNativeKeys.clear();
					context.browserPageCount = 0;
					break;
				}
			}
		}
		if(context.heroMeetingArmies)
		{
			auto & armies = *context.heroMeetingArmies;
			const bool validIds = armies.leftHeroId >= 0 && armies.rightHeroId >= 0 && armies.leftHeroId != armies.rightHeroId
				&& armies.leftArmyId >= 0 && armies.rightArmyId >= 0 && armies.leftArmyId != armies.rightArmyId;
			if(!validIds)
			{
				context.heroMeetingArmies.reset();
				context.heroMeetingArtifacts.reset();
				return;
			}
			armies.leftHeroName = thorBoundedText(std::move(armies.leftHeroName));
			armies.rightHeroName = thorBoundedText(std::move(armies.rightHeroName));
			for(auto & key : armies.heroPortraitAssetKeys)
				if(key != 0 && !isThorHeroPortraitVisualAssetKey(key))
					key = 0;
			auto normalizeSlots = [](auto & slots, int armyId)
			{
				for(std::size_t index = 0; index < slots.size(); ++index)
				{
					auto & slot = slots[index];
					if(slot.armyId != armyId || slot.slot != static_cast<int>(index) || slot.count < 0
						|| (slot.occupied && (slot.creatureId < 0 || slot.count <= 0))
						|| (!slot.occupied && (slot.creatureId != -1 || slot.count != 0)))
						return false;
					slot.creatureName = thorBoundedText(std::move(slot.creatureName));
					if(slot.occupied)
						slot.visualAssetKey = thorCreatureVisualAssetKey(slot.creatureId);
					else
					{
						slot.creatureName.clear();
						slot.visualAssetKey = 0;
					}
				}
				return true;
			};
			if(!normalizeSlots(armies.leftSlots, armies.leftArmyId) || !normalizeSlots(armies.rightSlots, armies.rightArmyId))
				context.heroMeetingArmies.reset();
		}

		if(context.heroMeetingArtifacts)
		{
		auto & artifacts = *context.heroMeetingArtifacts;
		if(artifacts.leftHeroId < 0 || artifacts.rightHeroId < 0 || artifacts.leftHeroId == artifacts.rightHeroId
			|| artifacts.artifactSlots.size() != THOR_HERO_MEETING_ARTIFACT_COUNT)
		{
			context.heroMeetingArtifacts.reset();
			return;
		}
		artifacts.leftHeroName = thorBoundedText(std::move(artifacts.leftHeroName));
		artifacts.rightHeroName = thorBoundedText(std::move(artifacts.rightHeroName));
		for(std::size_t index = 0; index < artifacts.artifactSlots.size(); ++index)
		{
			auto & slot = artifacts.artifactSlots[index];
			const auto sideOffset = index % (THOR_HERO_MEETING_EQUIPPED_ARTIFACT_COUNT + THOR_HERO_MEETING_BACKPACK_ARTIFACT_COUNT);
			const bool backpack = sideOffset >= THOR_HERO_MEETING_EQUIPPED_ARTIFACT_COUNT;
			const int expectedHero = index < artifacts.artifactSlots.size() / 2 ? artifacts.leftHeroId : artifacts.rightHeroId;
			if(slot.heroId != expectedHero || slot.position != static_cast<int>(sideOffset)
				|| slot.backpack != backpack || (slot.occupied ? slot.instanceId < 0 : (!slot.name.empty() || slot.instanceId != -1)))
			{
				context.heroMeetingArtifacts.reset();
				return;
			}
			slot.name = thorBoundedText(std::move(slot.name));
			if(slot.occupied)
				slot.visualAssetKey = thorArtifactVisualAssetKey(slot.artifactTypeId);
			else
			{
				slot.artifactTypeId = -1;
				slot.visualAssetKey = 0;
			}
		}
		}
		if(context.heroManagement)
		{
			auto & hero = *context.heroManagement;
			bool valid = context.contextId == ThorContextIds::HERO_WINDOW && hero.heroId >= 0
				&& hero.heroId == context.windowSubjectId && hero.armySlots.size() == THOR_HERO_MEETING_ARMY_SIZE
				&& hero.artifactSlots.size() >= THOR_HERO_MEETING_EQUIPPED_ARTIFACT_COUNT
				&& hero.artifactSlots.size() <= THOR_HERO_MANAGEMENT_ARTIFACT_COUNT;
			hero.heroName = thorBoundedText(std::move(hero.heroName));
			for(std::size_t index = 0; valid && index < hero.armySlots.size(); ++index)
			{
				auto & slot = hero.armySlots[index];
				valid = slot.armyId == hero.heroId && slot.slot == static_cast<int>(index) && slot.count >= 0
					&& (slot.occupied ? slot.creatureId >= 0 && slot.count > 0
						: slot.creatureId == -1 && slot.count == 0);
				if(valid)
				{
					slot.creatureName = slot.occupied ? thorBoundedText(std::move(slot.creatureName)) : std::string{};
					slot.visualAssetKey = slot.occupied ? thorCreatureVisualAssetKey(slot.creatureId) : 0;
				}
			}
			for(std::size_t index = 0; valid && index < hero.artifactSlots.size(); ++index)
			{
				auto & slot = hero.artifactSlots[index];
				const bool backpack = index >= THOR_HERO_MEETING_EQUIPPED_ARTIFACT_COUNT;
				valid = slot.heroId == hero.heroId
					&& slot.position == static_cast<int>(index)
					&& slot.backpack == backpack
					&& (slot.occupied ? slot.instanceId >= 0 && slot.artifactTypeId >= 0
						: slot.name.empty() && slot.instanceId == -1);
				if(valid)
				{
					slot.name = thorBoundedText(std::move(slot.name));
					slot.visualAssetKey = slot.occupied ? thorArtifactVisualAssetKey(slot.artifactTypeId) : 0;
				}
			}
			if(!valid)
			{
				context.heroManagement.reset();
				context.enabledActionMask &= ~(thorActionMask(ThorAction::HERO_WINDOW_TRANSFER_STACK)
					| thorActionMask(ThorAction::HERO_WINDOW_TRANSFER_ARTIFACT));
			}
		}
		if(context.contextId != ThorContextIds::TOWN_WINDOW)
			context.townManagement.reset();
		if(context.townManagement)
		{
			auto & town = *context.townManagement;
			bool valid = context.contextId == ThorContextIds::TOWN_WINDOW && town.townId >= 0
				&& town.townId == context.windowSubjectId && town.garrisonArmyId >= 0
				&& (town.visitingHeroId < 0 || town.visitingHeroId != town.garrisonArmyId);
			town.visitingHeroName = thorBoundedText(std::move(town.visitingHeroName));
			for(std::size_t index = 0; valid && index < THOR_HERO_MEETING_ARMY_SIZE; ++index)
			{
				auto & garrison = town.garrisonSlots[index];
				valid = garrison.armyId == town.garrisonArmyId && garrison.slot == static_cast<int>(index)
					&& garrison.count >= 0 && (garrison.occupied ? garrison.creatureId >= 0 && garrison.count > 0
						: garrison.creatureId == -1 && garrison.count == 0);
				if(valid)
				{
					garrison.creatureName = garrison.occupied ? thorBoundedText(std::move(garrison.creatureName)) : std::string{};
					garrison.visualAssetKey = garrison.occupied ? thorCreatureVisualAssetKey(garrison.creatureId) : 0;
				}
				auto & visiting = town.visitingSlots[index];
				valid = valid && visiting.armyId == town.visitingHeroId && visiting.slot == static_cast<int>(index)
					&& visiting.count >= 0 && (visiting.occupied ? town.visitingHeroId >= 0 && visiting.creatureId >= 0
						&& visiting.count > 0 : visiting.creatureId == -1 && visiting.count == 0);
				if(valid)
				{
					visiting.creatureName = visiting.occupied ? thorBoundedText(std::move(visiting.creatureName)) : std::string{};
					visiting.visualAssetKey = visiting.occupied ? thorCreatureVisualAssetKey(visiting.creatureId) : 0;
				}
			}
			if(!valid)
			{
				context.townManagement.reset();
				context.enabledActionMask &= ~thorActionMask(ThorAction::TOWN_WINDOW_TRANSFER_STACK);
			}
		}
	}
}

ThorContextRecord ThorContextStore::snapshot() const
{
	std::lock_guard lock(mutex);
	return current;
}

bool ThorContextStore::publish(ThorContextRecord next)
{
	std::lock_guard lock(mutex);
	if(next.revision <= current.revision)
		return false;
	if(next.contextId.empty())
		next.contextId = ThorContextIds::UNKNOWN;
	if(next.contextId == ThorContextIds::UNKNOWN)
		next.details = {};
	normalizeActionSubject(next);
	boundTextFields(next);
	current = std::move(next);
	return true;
}

ThorContextRecord ThorContextStore::publishNext(ThorContextRecord next)
{
	std::lock_guard lock(mutex);
	if(next.contextId.empty())
		next.contextId = ThorContextIds::UNKNOWN;
	if(next.contextId == ThorContextIds::UNKNOWN)
		next.details = {};
	normalizeActionSubject(next);
	boundTextFields(next);
	if(sameSemanticState(current, next))
		return current;
	next.revision = current.revision + 1;
	current = std::move(next);
	return current;
}

bool thorMenuModalOwnerMatches(const ThorContextRecord & context, ThorMenuModalType type,
	const std::string & sourceContext, const void * ownerToken, const void * parentToken, bool active, bool top)
{
	if(!active || !top || type == ThorMenuModalType::NONE || context.menuModalType != type
		|| context.menuModalSourceContext != sourceContext || context.nativeOwnerToken != ownerToken
		|| context.nativeParentToken != parentToken)
		return false;
	switch(type)
	{
	case ThorMenuModalType::QUIT_CONFIRMATION:
		return context.contextId == ThorContextIds::MENU_QUIT_CONFIRMATION
			&& sourceContext == ThorContextIds::MAIN_MENU;
	case ThorMenuModalType::HIGH_SCORE_RESET_CONFIRMATION:
		return context.contextId == ThorContextIds::HIGH_SCORE_RESET_CONFIRMATION
			&& sourceContext == ThorContextIds::HIGH_SCORES;
	case ThorMenuModalType::MISSING_TUTORIAL_INFORMATION:
		return context.contextId == ThorContextIds::TUTORIAL_MISSING_DIALOG
			&& (sourceContext == ThorContextIds::MAIN_MENU_NEW_GAME || sourceContext == ThorContextIds::MAIN_MENU_LOAD_GAME);
	case ThorMenuModalType::NONE:
		return false;
	}
	return false;
}

ThorContextStore & thorContextStore()
{
	static ThorContextStore store;
	return store;
}

std::string thorContextIdForMainMenuTab(const std::string & tabName)
{
	if(tabName == "main")
		return ThorContextIds::MAIN_MENU;
	if(tabName == "new")
		return ThorContextIds::MAIN_MENU_NEW_GAME;
	if(tabName == "load")
		return ThorContextIds::MAIN_MENU_LOAD_GAME;
	if(tabName == "campaign")
		return ThorContextIds::MAIN_MENU_CAMPAIGN;
	if(tabName == "credits")
		return ThorContextIds::MAIN_MENU_CREDITS;
	return ThorContextIds::UNKNOWN;
}

std::string thorContextIdForLobby(ThorLobbyMode mode, ThorLobbyTab tab)
{
	switch(mode)
	{
	case ThorLobbyMode::NEW_GAME:
		switch(tab)
		{
		case ThorLobbyTab::NONE:
			return ThorContextIds::LOBBY_NEW_GAME;
		case ThorLobbyTab::SCENARIO:
			return ThorContextIds::LOBBY_NEW_GAME_SCENARIO;
		case ThorLobbyTab::OPTIONS:
			return ThorContextIds::LOBBY_NEW_GAME_OPTIONS;
		case ThorLobbyTab::RANDOM_MAP:
			return ThorContextIds::LOBBY_NEW_GAME_RANDOM_MAP;
		case ThorLobbyTab::TURN_OPTIONS:
			return ThorContextIds::LOBBY_NEW_GAME_TURN_OPTIONS;
		case ThorLobbyTab::EXTRA_OPTIONS:
			return ThorContextIds::LOBBY_NEW_GAME_EXTRA_OPTIONS;
		case ThorLobbyTab::BATTLE_MODE:
			return ThorContextIds::LOBBY_NEW_GAME_BATTLE_MODE;
		default:
			return ThorContextIds::UNKNOWN;
		}
	case ThorLobbyMode::LOAD_GAME:
		switch(tab)
		{
		case ThorLobbyTab::NONE:
			return ThorContextIds::LOBBY_LOAD_GAME;
		case ThorLobbyTab::SCENARIO:
			return ThorContextIds::LOBBY_LOAD_GAME_SCENARIO;
		case ThorLobbyTab::OPTIONS:
			return ThorContextIds::LOBBY_LOAD_GAME_OPTIONS;
		case ThorLobbyTab::TURN_OPTIONS:
			return ThorContextIds::LOBBY_LOAD_GAME_TURN_OPTIONS;
		case ThorLobbyTab::EXTRA_OPTIONS:
			return ThorContextIds::LOBBY_LOAD_GAME_EXTRA_OPTIONS;
		default:
			return ThorContextIds::UNKNOWN;
		}
	case ThorLobbyMode::CAMPAIGN_LIST:
		return tab == ThorLobbyTab::SCENARIO ? ThorContextIds::LOBBY_CAMPAIGN_LIST : ThorContextIds::UNKNOWN;
	default:
		return ThorContextIds::UNKNOWN;
	}
}

std::string thorContextIdForInGameContext(ThorInGameContext context)
{
	switch(context)
	{
	case ThorInGameContext::ADVENTURE_MAP:
		return ThorContextIds::ADVENTURE_MAP;
	case ThorInGameContext::HERO_WINDOW:
		return ThorContextIds::HERO_WINDOW;
	case ThorInGameContext::TOWN_WINDOW:
		return ThorContextIds::TOWN_WINDOW;
	case ThorInGameContext::TOWN_HALL:
		return ThorContextIds::TOWN_HALL;
	case ThorInGameContext::TOWN_RECRUITMENT_QUICK:
		return ThorContextIds::TOWN_RECRUITMENT_QUICK;
	case ThorInGameContext::TOWN_RECRUITMENT_DWELLING:
		return ThorContextIds::TOWN_RECRUITMENT_DWELLING;
	case ThorInGameContext::HERO_MEETING:
		return ThorContextIds::HERO_MEETING;
	case ThorInGameContext::BATTLE:
		return ThorContextIds::BATTLE;
	case ThorInGameContext::BATTLE_TACTICS:
		return ThorContextIds::BATTLE_TACTICS;
	case ThorInGameContext::BATTLE_RESULT:
		return ThorContextIds::BATTLE_RESULT;
	case ThorInGameContext::KINGDOM_OVERVIEW:
		return ThorContextIds::KINGDOM_OVERVIEW;
	case ThorInGameContext::QUEST_LOG:
		return ThorContextIds::QUEST_LOG;
	case ThorInGameContext::SCENARIO_EVENT_JOURNAL:
		return ThorContextIds::SCENARIO_EVENT_JOURNAL;
	case ThorInGameContext::PUZZLE_MAP:
		return ThorContextIds::PUZZLE_MAP;
	case ThorInGameContext::SAVE_GAME:
		return ThorContextIds::SAVE_GAME;
	default:
		return ThorContextIds::UNKNOWN;
	}
}

std::string thorBoundedText(std::string text, std::size_t maximumBytes)
{
	std::size_t validBytes = 0;
	while(validBytes < text.size() && validBytes < maximumBytes)
	{
		const auto firstByte = static_cast<unsigned char>(text[validBytes]);
		const std::size_t codePointBytes = firstByte < 0x80 ? 1 : firstByte >= 0xc2 && firstByte <= 0xdf ? 2
			: firstByte >= 0xe0 && firstByte <= 0xef ? 3 : firstByte >= 0xf0 && firstByte <= 0xf4 ? 4 : 0;
		if(codePointBytes == 0 || validBytes + codePointBytes > maximumBytes || validBytes + codePointBytes > text.size())
			break;
		bool valid = true;
		for(std::size_t index = 1; index < codePointBytes; ++index)
			valid &= (static_cast<unsigned char>(text[validBytes + index]) & 0xc0) == 0x80;
		if(codePointBytes > 1)
		{
			const auto second = static_cast<unsigned char>(text[validBytes + 1]);
			valid &= !(firstByte == 0xe0 && second < 0xa0) && !(firstByte == 0xed && second >= 0xa0)
				&& !(firstByte == 0xf0 && second < 0x90) && !(firstByte == 0xf4 && second >= 0x90);
		}
		if(!valid || firstByte == 0)
			break;
		validBytes += codePointBytes;
	}
	text.resize(validBytes);
	return text;
}
