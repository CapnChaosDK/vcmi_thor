#pragma once

#include "ThorContext.h"

namespace ThorExtended
{
inline bool battleOwnerMatches(const ThorContextRecord & context, const void * window,
	const void * participant, bool active, bool top)
{
	return active && top && window && participant && context.contextId == ThorContextIds::BATTLE
		&& context.nativeOwnerToken == window && context.nativeParentToken == participant;
}

/// Native eligibility inputs; Android receives only the resulting command lists.
struct DLL_LINKAGE BattleAvailability
{
	bool owner = false, local = false, opening = true, tactics = false, blocked = true, targeting = false;
	bool makingTurn = false, autoOn = false, aiExists = false, endWithConfirmation = false;
	bool canCast = false, canFlee = false, canSurrender = false, criticalHero = true;
};

inline State battleState(const BattleAvailability & a)
{
	State state;
	if(a.autoOn)
		state.active.push_back(AUTO_START);
	if(!a.owner || !a.local || a.opening || a.tactics || a.targeting)
		return state;
	// Normal native auto-combat remains stoppable while its AI blocks the battle UI.
	if(a.autoOn && a.aiExists && !a.endWithConfirmation)
		state.enabled.push_back(AUTO_STOP);
	if(a.blocked || !a.makingTurn || a.autoOn)
		return state;
	if(!a.aiExists)
		state.enabled.push_back(AUTO_START);
	if(a.canCast)
		state.enabled.push_back(CAST_SPELL);
	if(!a.criticalHero && a.canFlee)
		state.enabled.push_back(RETREAT);
	if(!a.criticalHero && a.canSurrender)
		state.enabled.push_back(SURRENDER);
	return state;
}
}
