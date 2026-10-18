/*
 * BattleAnimationWait.h, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 *
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 *
 */
#pragma once

// The caller owns the interface lock. Only wait releases it; stage dispatch and
// queue inspection must run with it held. Keep draining stages added by stages
// or by animation completion, including stages that start no animation.
template<typename Wait, typename Busy, typename Pending, typename Execute>
void waitForBattleAnimations(Wait wait, Busy busy, Pending pending, Execute execute)
{
	wait();
	while(pending())
	{
		execute();
		if(busy())
			wait();
	}
}
