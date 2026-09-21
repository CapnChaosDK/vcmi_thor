#include "ThorContext.h"

#include <utility>

namespace
{
	bool sameSemanticState(const ThorContextRecord & lhs, const ThorContextRecord & rhs)
	{
		return lhs.contextId == rhs.contextId
			&& lhs.title == rhs.title
			&& lhs.status == rhs.status
			&& lhs.details == rhs.details
			&& lhs.enabledActionMask == rhs.enabledActionMask
			&& lhs.activeActionMask == rhs.activeActionMask
			&& lhs.selectedHeroId == rhs.selectedHeroId
			&& lhs.actionSubjectId == rhs.actionSubjectId
			&& lhs.actionEpoch == rhs.actionEpoch
			&& lhs.heroes == rhs.heroes
			&& lhs.towns == rhs.towns
			&& lhs.heroMeetingArmies == rhs.heroMeetingArmies;
	}

	void normalizeActionSubject(ThorContextRecord & context)
	{
		if(context.contextId != ThorContextIds::BATTLE && context.contextId != ThorContextIds::BATTLE_TACTICS)
			context.actionSubjectId = -1;
	}

	void boundTextFields(ThorContextRecord & context)
	{
		context.title = thorBoundedText(std::move(context.title));
		context.status = thorBoundedText(std::move(context.status));
		for(auto & detail : context.details)
			detail = thorBoundedText(std::move(detail));
		if(context.contextId != ThorContextIds::ADVENTURE_MAP || context.heroes.size() > THOR_MAX_HEROES)
			context.heroes.clear();
		if(context.contextId != ThorContextIds::ADVENTURE_MAP || context.towns.size() > THOR_MAX_TOWNS)
			context.towns.clear();
		if(context.contextId != ThorContextIds::HERO_MEETING)
			context.heroMeetingArmies.reset();
		for(auto & hero : context.heroes)
			hero.name = thorBoundedText(std::move(hero.name));
		for(auto & town : context.towns)
			town.name = thorBoundedText(std::move(town.name));
		if(!context.heroMeetingArmies)
			return;
		auto & armies = *context.heroMeetingArmies;
		const bool validIds = armies.leftHeroId >= 0 && armies.rightHeroId >= 0 && armies.leftHeroId != armies.rightHeroId
			&& armies.leftArmyId >= 0 && armies.rightArmyId >= 0 && armies.leftArmyId != armies.rightArmyId;
		if(!validIds)
		{
			context.heroMeetingArmies.reset();
			return;
		}
		armies.leftHeroName = thorBoundedText(std::move(armies.leftHeroName));
		armies.rightHeroName = thorBoundedText(std::move(armies.rightHeroName));
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
				if(!slot.occupied)
					slot.creatureName.clear();
			}
			return true;
		};
		if(!normalizeSlots(armies.leftSlots, armies.leftArmyId) || !normalizeSlots(armies.rightSlots, armies.rightArmyId))
			context.heroMeetingArmies.reset();
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
