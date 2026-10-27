/*
 * ThorTavernSupport.cpp, part of VCMI engine
 * Authors: listed in file AUTHORS in main folder
 * License: GNU General Public License v2.0 or later
 */
#include "StdInc.h"
#include "GUIClasses.h"

#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
#include "../GameEngine.h"
#include "../GameInstance.h"
#include "../Client.h"
#include "../CServerHandler.h"
#include "../CPlayerInterface.h"
#include "../gui/WindowHandler.h"
#include "../widgets/Buttons.h"
#include "../widgets/TextControls.h"
#include "../../lib/CAndroidVMHelper.h"
#include "../../lib/CConfigHandler.h"
#include "../../lib/IGameSettings.h"
#include "../../lib/callback/CCallback.h"
#include "../../lib/mapObjects/CGHeroInstance.h"
#include "../../lib/mapObjects/CGTownInstance.h"
#include "../../lib/gameState/CGameState.h"
#include "../../lib/gameState/TavernHeroesPool.h"

void CTavernWindow::activate()
{
	if(isActive()) return;
	CStatusbarWindow::activate();
	refreshThorTavern(true);
}

void CTavernWindow::deactivate()
{
	if(!isActive()) return;
	CStatusbarWindow::deactivate();
	thorActionQueue().clear();
	const auto state = thorContextStore().publishNext({});
	CAndroidVMHelper().publishThorContext(state.revision, state.contextId, {}, {});
}

void CTavernWindow::refreshThorTavern(bool invalidate)
{
	if(!isActive() || ENGINE->windows().topWindow<CTavernWindow>().get() != this) return;
	const auto previous = thorContextStore().snapshot();
	ThorContextRecord next;
	const auto cb = GAME->interface()->cb;
	const auto live = cb->tryGetObj(thorTavernId);
	// Do not dereference a captured object/hero until the current native offers match.
	if(live && live == tavernObj && GAME->interface() == thorTavernInterface)
	{
		auto offered = cb->getAvailableHeroes(live);
		if(offered.size() < 2) offered.resize(2, nullptr);
		if(offered.size() == 2 && offered[0] == h1->h && offered[1] == h2->h)
		{
			ThorTavern value;
			value.cost = GameConstants::HERO_GOLD_COST;
			value.gold = cb->getResourceAmount(EGameResID::GOLD);
			value.rumor = thorBoundedText(rumor->label->getText(), 1024);
			const auto town = dynamic_cast<const CGTownInstance *>(live);
			value.controllable = GAME->interface()->makingTurn && !settings["session"]["spectate"].Bool()
				&& (!town || town->getOwner() == GAME->interface()->playerID);
			value.canGuild = !thiefGuild->isBlocked() && !settings["session"]["spectate"].Bool();
			for(int i = 0; i < 2; ++i)
				if(offered[i])
				{
					const auto portrait = i == 0 ? h1 : h2;
					const auto id = offered[i]->getHeroTypeID().getNum();
					value.rows.push_back({id, 1, thorBoundedText(GAME->translator().translate(offered[i]->getNameTextID()), 256),
						thorBoundedText(portrait->description, 1024)});
					if(selected == i) value.selected = id;
				}
			if(!value.controllable) value.reason = 1;
			else if(value.gold < value.cost) value.reason = 2;
			else if(cb->howManyHeroes(true) >= cb->getSettings().getInteger(EGameSettings::HEROES_PER_PLAYER_TOTAL_CAP)
				|| cb->howManyHeroes(false) >= cb->getSettings().getInteger(EGameSettings::HEROES_PER_PLAYER_ON_MAP_CAP)) value.reason = 3;
			else if(town && town->getVisitingHero()) value.reason = 4;
			else if(value.selected == -1) value.reason = 5;
			else if(recruit->isBlocked()) value.reason = 6;
			value.canRecruit = value.reason == 0;
			if(cb->getSettings().getBoolean(EGameSettings::HEROES_TAVERN_INVITE))
			{
				const auto & pool = GAME->server().client->gameState().heroesPool;
				const auto & unused = pool->unusedHeroesFromPool();
				bool currentInvitation = heroToInvite == nullptr;
				for(const auto & [id, hero] : inviteableHeroes)
				{
					const auto current = unused.find(id);
					if(current == unused.end() || current->second != hero || !pool->isHeroAvailableFor(id, live->getOwner())) continue;
					currentInvitation |= heroToInvite == hero;
					auto row = std::find_if(value.rows.begin(), value.rows.end(), [&](const auto & item) { return item.id == id.getNum(); });
					if(row == value.rows.end()) value.rows.push_back({id.getNum(), 2, thorBoundedText(GAME->translator().translate(hero->getNameTextID()), 256), {}});
					else row->flags |= 2;
					if(thorInvitationChosen && heroToInvite == hero) value.invited = id.getNum();
					if(value.rows.size() > ThorTavern::MAX_ROWS) break;
				}
				if(!currentInvitation) { value.canRecruit = false; value.reason = 6; }
			}
			if(value.valid())
			{
				next.contextId = ThorContextIds::TAVERN;
				next.nativeOwnerToken = this;
				next.actionEpoch = previous.nativeOwnerToken == this ? previous.actionEpoch + (invalidate ? 1 : 0) : 0;
				next.title = title->getText();
				next.tavern = std::move(value);
			}
		}
	}
	const auto state = thorContextStore().publishNext(std::move(next));
	if(state.revision == previous.revision) return;
	CAndroidVMHelper bridge;
	bridge.publishThorContext(state.revision, state.contextId, state.title, {});
	if(state.tavern) bridge.publishThorTavern(state.revision, *state.tavern);
}

bool CTavernWindow::executeThorTavern(const ThorActionRequest & request)
{
	refreshThorTavern();
	const auto current = thorContextStore().snapshot();
	if(current.nativeOwnerToken != this || validateThorActionRequest(request, current) != ThorActionValidation::VALID) return false;
	if(request.commandId == ThorExtended::TAVERN_CLOSE) { close(); return true; }
	if(request.commandId == ThorExtended::TAVERN_GUILD) { thievesguildb(); return true; }
	if(request.commandId == ThorExtended::TAVERN_RECRUIT)
	{
		// Keep upstream's request, immediate close, server rejection and parent callback.
		recruitb();
		return true;
	}
	if(request.commandId == ThorExtended::TAVERN_SELECT)
	{
		const auto portrait = h1->h && h1->h->getHeroTypeID().getNum() == request.targetId ? h1 : h2;
		if(!portrait->h || portrait->h->getHeroTypeID().getNum() != request.targetId) return false;
		portrait->clickPressed(Point());
	}
	else if(request.commandId == ThorExtended::TAVERN_INVITE)
	{
		const auto hero = inviteableHeroes.find(HeroTypeID(request.targetId));
		if(hero == inviteableHeroes.end()) return false;
		heroToInvite = hero->second;
		thorInvitationChosen = true;
		addInvite(); // Same native UI update as the existing invitation picker callback.
	}
	else return false;
	refreshThorTavern(true);
	return true;
}
#endif
