/*
 * SpellActivities.cpp, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 *
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 *
 */
#include "StdInc.h"
#include "SpellActivities.h"

#include "ActivityProcessor.h"
#include "LuaScriptActivity.h"

#include "../CGameHandler.h"
#include "../ServerSpellCastEnvironment.h"

#include "../../lib/callback/IGameInfoCallback.h"
#include "../../lib/mapObjects/CGHeroInstance.h"
#include "../../lib/mapObjects/CGTownInstance.h"
#include "../../lib/spells/CSpell.h"
#include "../../lib/spells/ISpellMechanics.h"
#include "../../lib/spells/Problem.h"
#include "../../lib/networkPacks/PacksForClient.h"

TownSelectionActivity::TownSelectionActivity(CGameHandler * owner, PlayerColor player, SpellID spell, ObjectInstanceID caster, std::vector<ObjectInstanceID> offeredTowns)
	: DialogActivity(owner, TYPE)
	, spell(spell)
	, caster(caster)
	, offeredTowns(std::move(offeredTowns))
{
	addPlayer(player);
}

bool TownSelectionActivity::acceptsAnswerWithoutValue() const
{
	// The player may close the window instead of picking a town, which cancels the cast
	return true;
}

bool TownSelectionActivity::acceptsAnswer(int32_t answer) const
{
	return vstd::contains(offeredTowns, ObjectInstanceID(answer));
}

void TownSelectionActivity::onRemoval()
{
	if(!answer)
		return; // window was closed without choosing a town

	const ObjectInstanceID chosen(*answer);

	// Both may have been removed while the dialog was open
	const auto * town = gh->gameInfo().getTown(chosen);
	const auto * hero = gh->gameInfo().getHero(caster);

	if(!town || !hero || hero->getOwner() != getPlayers().front())
		return;

	AdventureSpellCastParameters parameters;
	parameters.caster = hero;
	parameters.pos = town->visitablePos();

	// Revalidate the current caster and destination, then complete the pending cast.
	const auto & mechanics = spell.toSpell()->getAdventureMechanics();
	spells::detail::ProblemImpl problem;
	if(mechanics.canBeCastAt(problem, &gh->gameInfo(), hero, parameters.pos))
		mechanics.performCast(gh->spellEnv.get(), parameters);
}

ScriptDialogActivity::ScriptDialogActivity(CGameHandler * owner, const BlockingDialog & dialog)
	: DialogActivity(owner, TYPE), selection(dialog.selection()), choices(dialog.components.size())
{
	addPlayer(dialog.player);
}

bool ScriptDialogActivity::acceptsAnswer(int32_t answer) const
{
	return answer >= 0 && (selection ? static_cast<size_t>(answer) <= choices : answer <= 1);
}

bool ScriptDialogActivity::acceptsAnswerWithoutValue() const
{
	return true;
}

void ScriptDialogActivity::onRemoval()
{
	// This dialog was pushed directly on top of the script that is waiting for the answer,
	// and has already been popped by now, so that script is the current top. A type search
	// would pick the wrong one when two map events paused at the same time.
	if(auto * script = owner->activityAs<LuaScriptActivity>(owner->topActivity(getPlayers().front())))
		script->setPendingAnswer(answer);
}
