/*
 * ThorSpellbookSupport.cpp, part of VCMI engine
 * Authors: listed in file AUTHORS in main folder
 * License: GNU General Public License v2.0 or later
 */
#include "StdInc.h"
#include "CSpellWindow.h"

#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
#include "../GameEngine.h"
#include "../GameInstance.h"
#include "../CPlayerInterface.h"
#include "../battle/BattleInterface.h"
#include "../gui/WindowHandler.h"
#include "../../lib/CAndroidVMHelper.h"
#include "../../lib/CConfigHandler.h"
#include "../../lib/GameLibrary.h"
#include "../../lib/callback/CCallback.h"
#include "../../lib/battle/CPlayerBattleCallback.h"
#include "../../lib/mapObjects/CGHeroInstance.h"
#include "../../lib/spells/CSpell.h"
#include "../../lib/spells/ISpellMechanics.h"
#include "../../lib/spells/adventure/AdventureSpellEffect.h"
#include "../../lib/spells/Problem.h"
#include "../../lib/spells/SpellSchoolHandler.h"

void CSpellWindow::activate()
{
	if(isActive()) return;
	CWindowObject::activate();
	refreshThorSpellbook(true);
}

void CSpellWindow::deactivate()
{
	if(!isActive()) return;
	CWindowObject::deactivate();
	thorActionQueue().clear();
	const auto state = thorContextStore().publishNext({});
	CAndroidVMHelper().publishThorContext(state.revision, state.contextId, {}, {});
}

void CSpellWindow::refreshThorSpellbook(bool invalidate, bool force)
{
	if(!isActive() || ENGINE->windows().topWindow<CSpellWindow>().get() != this) return;
	const auto previous = thorContextStore().snapshot();
	const auto now = std::chrono::steady_clock::now();
	// Spell legality may inspect native battle targets. Bound polling work, but
	// always recompute immediately before execution and owner activation.
	if(!invalidate && !force && previous.nativeOwnerToken == this
		&& now - thorSpellbookPublication < std::chrono::milliseconds(100)) return;
	thorSpellbookPublication = now;
	ThorContextRecord next;
	// External spell selectors (e.g. editor/selection callbacks) retain their native UI.
	const auto hero = GAME->interface() == myInt
		? dynamic_cast<const CGHeroInstance *>(myInt->cb->tryGetObj(thorHeroId)) : nullptr;
	if(!onSpellSelect && hero && hero == myHero && hero->getOwner() == myInt->playerID)
	{
		ThorSpellbook book;
		book.hero = hero->id.getNum();
		book.heroName = thorBoundedText(GAME->translator().translate(hero->getNameTextID()), 256);
		book.mana = hero->mana;
		book.battle = myInt->battleInt != nullptr;
		const bool active = !settings["session"]["spectate"].Bool() && !myInt->castleInt
			&& (book.battle ? myInt->battleInt->makingTurn() && myInt->battleInt->currentHero() == hero
				&& myInt->battleInt->getBattle()->battleCanCastSpell(hero, spells::Mode::HERO) == ESpellCastProblem::OK : myInt->makingTurn);
		for(const auto spell : mySpells)
		{
			if(book.rows.size() > ThorSpellbook::MAX_ROWS) break;
			if(!hero->canCastThisSpell(spell) || spell->isCreatureAbility()) continue;
			ThorSpellbookRow row;
			row.id = spell->id.getNum(); row.level = spell->getLevel();
			row.cost = myInt->cb->getSpellCost(spell, hero); row.combat = spell->isCombat();
			row.name = thorBoundedText(spell->getNameTranslated(), 256);
			row.description = spell->getDescriptionTranslated(hero->getSpellSchoolLevel(spell));
			const auto damage = myInt->cb->estimateSpellDamage(spell, hero);
			if(damage != 0 && spell->id != SpellID::TITANS_LIGHTNING_BOLT)
			{
				auto text = MetaString::createFromTextID("core.genrltxt.343");
				text.replaceNumber(damage);
				row.description += text.toString(&GAME->translator());
			}
			row.description = thorBoundedText(row.description, 4096);
			spell->forEachSchool([&](const SpellSchool & school, bool &)
			{
				row.schools.push_back(school.getNum());
				if(std::find(book.schools.begin(), book.schools.end(), school.getNum()) == book.schools.end())
				{
					book.schools.push_back(school.getNum());
					book.schoolNames.push_back(thorBoundedText(LIBRARY->spellSchoolHandler->getById(school)->getNameTranslated(), 256));
				}
			});
			row.castable = active && row.cost <= book.mana && row.combat == book.battle
				&& spell->isCombat() != spell->isAdventure();
			if(row.castable)
			{
				spells::detail::ProblemImpl problem;
				row.castable = book.battle
					? spell->canBeCast(problem, myInt->battleInt->getBattle().get(), spells::Mode::HERO, hero)
					: spell->getAdventureMechanics().canBeCast(problem, myInt->cb.get(), hero);
			}
			book.rows.push_back(std::move(row));
		}
		if(book.valid())
		{
			next.contextId = ThorContextIds::SPELLBOOK;
			next.nativeOwnerToken = this;
			next.spellbook = std::move(book);
			next.actionEpoch = previous.nativeOwnerToken == this ? previous.actionEpoch + (invalidate ? 1 : 0) : 0;
		}
	}
	const auto state = thorContextStore().publishNext(std::move(next));
	if(state.revision == previous.revision) return;
	CAndroidVMHelper bridge;
	bridge.publishThorContext(state.revision, state.contextId, {}, {});
	if(state.spellbook) bridge.publishThorSpellbook(state.revision, *state.spellbook);
}

bool CSpellWindow::executeThorSpellbook(const ThorActionRequest & request)
{
	refreshThorSpellbook(false, true);
	const auto current = thorContextStore().snapshot();
	if(current.nativeOwnerToken != this || validateThorActionRequest(request, current) != ThorActionValidation::VALID) return false;
	if(request.commandId == ThorExtended::SPELLBOOK_CLOSE) { fexitb(); return true; }
	const auto found = std::find_if(mySpells.begin(), mySpells.end(), [&](const auto spell) { return spell->id.getNum() == request.targetId; });
	if(found == mySpells.end()) return false;
	const auto spell = *found;
	// Invalidate before invoking the same native route used by an upper spell slot.
	// It may close this book, enter targeting, or cover it with an error dialog.
	refreshThorSpellbook(true);
	selectSpell(spell, myHero->getSpellSchoolLevel(spell));
	if(isActive() && ENGINE->windows().topWindow<CSpellWindow>().get() == this) refreshThorSpellbook(true);
	return true;
}
#endif
