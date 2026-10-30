/*
 * ThorTownServiceSupport.cpp, part of VCMI engine
 * Authors: listed in file AUTHORS in main folder
 * License: GNU General Public License v2.0 or later
 */
#include "StdInc.h"
#include "CCastleInterface.h"

#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
#include "../GameEngine.h"
#include "../GameInstance.h"
#include "../CPlayerInterface.h"
#include "../gui/WindowHandler.h"
#include "../widgets/Buttons.h"
#include "../widgets/TextControls.h"
#include "../../lib/CAndroidVMHelper.h"
#include "../../lib/CConfigHandler.h"
#include "../../lib/IGameSettings.h"
#include "../../lib/callback/CCallback.h"
#include "../../lib/entities/artifact/CArtifact.h"
#include "../../lib/entities/building/CBuilding.h"
#include "../../lib/mapObjects/CGHeroInstance.h"
#include "../../lib/mapObjects/CGTownInstance.h"
#include "../../lib/spells/CSpell.h"

namespace
{
	void withdrawService()
	{
		thorActionQueue().clear();
		const auto state = thorContextStore().publishNext({});
		CAndroidVMHelper().publishThorContext(state.revision, state.contextId, {}, {});
	}

	bool controlsTown(const CGTownInstance * town)
	{
		return town && town->getOwner() == GAME->interface()->playerID
			&& GAME->interface()->makingTurn && !settings["session"]["spectate"].Bool();
	}

	void publishService(const void * owner, ThorTownServicePanel panel, bool invalidate)
	{
		const auto previous = thorContextStore().snapshot();
		ThorContextRecord next;
		if(panel.valid())
		{
			next.contextId = panel.kind == 1 ? ThorContextIds::BLACKSMITH : ThorContextIds::MAGE_GUILD;
			next.nativeOwnerToken = owner;
			next.actionEpoch = previous.nativeOwnerToken == owner ? previous.actionEpoch + (invalidate ? 1 : 0) : 0;
			next.servicePanel = std::move(panel);
		}
		const auto state = thorContextStore().publishNext(std::move(next));
		if(state.revision == previous.revision) return;
		CAndroidVMHelper bridge;
		bridge.publishThorContext(state.revision, state.contextId, {}, {});
		if(state.servicePanel) bridge.publishThorServicePanel(state.revision, *state.servicePanel);
	}
}

void CBlacksmithDialog::activate()
{
	if(isActive()) return;
	CStatusbarWindow::activate();
	refreshThorService(true);
}

void CBlacksmithDialog::deactivate()
{
	if(!isActive()) return;
	CStatusbarWindow::deactivate();
	withdrawService();
}

void CBlacksmithDialog::refreshThorService(bool invalidate)
{
	if(!isActive() || ENGINE->windows().topWindow<CBlacksmithDialog>().get() != this) return;
	ThorTownServicePanel panel;
	const auto cb = GAME->interface()->cb;
	const auto hero = dynamic_cast<const CGHeroInstance *>(cb->tryGetObj(thorBlacksmithHero));
	const auto town = dynamic_cast<const CGTownInstance *>(cb->tryGetObj(thorBlacksmithTown));
	if(GAME->interface() == thorBlacksmithInterface && hero && town
		&& town->getVisitingHero() == hero && hero->getVisitedTown() == town
		&& town->getWarMachineInBuilding(BuildingID::BLACKSMITH) == thorBlacksmithArtifact)
	{
		panel.kind = 1;
		panel.owner = town->id.getNum();
		panel.cost = thorBlacksmithArtifact.toArtifact()->getPrice();
		panel.gold = cb->getResourceAmount(EGameResID::GOLD);
		panel.summary = thorBoundedText(GAME->translator().translate(hero->getNameTextID()), 2048);
		const bool available = controlsTown(town) && hero->getOwner() == GAME->interface()->playerID
			&& hero->getReplacedWarMachine(thorBlacksmithArtifact) == thorBlacksmithExisting
			&& hero->canRecruitWarMachine(thorBlacksmithArtifact) && panel.gold >= panel.cost && !buy->isBlocked();
		panel.rows.push_back({thorBlacksmithArtifact.getNum(), 0, available,
			thorBoundedText(thorBlacksmithArtifact.toArtifact()->getNameTranslated(), 256),
			thorBoundedText(title->getText(), 4096)});
	}
	publishService(this, std::move(panel), invalidate);
}

bool CBlacksmithDialog::executeThorService(const ThorActionRequest & request)
{
	refreshThorService();
	const auto current = thorContextStore().snapshot();
	if(current.nativeOwnerToken != this || validateThorActionRequest(request, current) != ThorActionValidation::VALID) return false;
	if(request.commandId == ThorExtended::SERVICE_CLOSE) { close(); return true; }
	if(request.commandId != ThorExtended::BLACKSMITH_BUY) return false;
	// Preserve native close, optional replacement confirmation and server request.
	return buy->invokeThorCallback();
}

void CMageGuildScreen::activate()
{
	if(isActive()) return;
	CStatusbarWindow::activate();
	refreshThorService(true);
}

void CMageGuildScreen::deactivate()
{
	if(!isActive()) return;
	CStatusbarWindow::deactivate();
	withdrawService();
}

void CMageGuildScreen::refreshThorService(bool invalidate)
{
	if(!isActive() || ENGINE->windows().topWindow<CMageGuildScreen>().get() != this) return;
	ThorTownServicePanel panel;
	const auto town = dynamic_cast<const CGTownInstance *>(GAME->interface()->cb->tryGetObj(townId));
	if(GAME->interface() == thorGuildInterface && town && town->getOwner() == GAME->interface()->playerID)
	{
		panel.kind = 2;
		panel.owner = townId.getNum();
		const bool research = controlsTown(town) && town->spellResearchAllowed
			&& GAME->interface()->cb->getSettings().getBoolean(EGameSettings::TOWNS_SPELL_RESEARCH);
		for(const auto & scroll : spells)
		{
			const auto spell = scroll->spell;
			if(spell->getLevel() < 1 || spell->getLevel() > town->mageGuildLevel()
				|| vstd::find_pos(town->spells[spell->getLevel() - 1], spell->id) == -1)
			{
				panel.kind = 0; // A stale upper scroll must never remain a lower target.
				break;
			}
			panel.rows.push_back({spell->id.getNum(), spell->getLevel(), research,
				thorBoundedText(spell->getNameTranslated(), 256), thorBoundedText(spell->getDescriptionTranslated(0), 4096)});
		}
		// Aurora Borealis uses native summary scrolls rather than revealing a spell list.
		if(!auroraBorealisScrolls.empty())
		{
			MetaString text;
			text.appendTextID("core.genrltxt.714");
			text.replaceRawString(town->getTown()->getSpecialBuilding(BuildingSubID::AURORA_BOREALIS)->getNameTranslated());
			panel.summary = thorBoundedText(text.toString(&GAME->translator()), 2048);
		}
	}
	publishService(this, std::move(panel), invalidate);
}

bool CMageGuildScreen::executeThorService(const ThorActionRequest & request)
{
	refreshThorService();
	const auto current = thorContextStore().snapshot();
	if(current.nativeOwnerToken != this || validateThorActionRequest(request, current) != ThorActionValidation::VALID) return false;
	if(request.commandId == ThorExtended::SERVICE_CLOSE) { close(); return true; }
	if(request.commandId != ThorExtended::GUILD_RESEARCH) return false;
	const auto scroll = std::find_if(spells.begin(), spells.end(), [&](const auto & item) { return item->spell->id.getNum() == request.targetId; });
	if(scroll == spells.end()) return false;
	// The existing scroll owns research cost, limits, confirmation and failure dialogs.
	(*scroll)->clickPressed(Point());
	return true;
}
#endif
