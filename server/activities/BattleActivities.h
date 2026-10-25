/*
 * BattleActivities.h, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 *
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 *
 */
#pragma once

#include "Activity.h"
#include "../../lib/networkPacks/PacksForClientBattle.h"
#include "../../lib/battle/BattleSide.h"

class IBattleInfo;
struct SideInBattle;

class BattleActivity : public Activity
{
public:
	static constexpr ActivityType TYPE = ActivityType::Battle;

	BattleSideArray<ObjectInstanceID> belligerents;

	BattleID battleID;
	std::optional<BattleResult> result;

	BattleActivity(CGameHandler * owner, const IBattleInfo * Bi);
	void notifyObjectAboutRemoval(const IObjectInterface * visitedObject, const CGHeroInstance * visitingHero, const JsonNode & visitState) const override;
	bool blocksPack(const CPackForServer *pack) const override;
};

class BattleResultActivity : public DialogActivity
{
	const IBattleInfo * bi;

public:
	static constexpr ActivityType TYPE = ActivityType::BattleDialog;
	BattleResultActivity(CGameHandler * owner, const IBattleInfo * Bi);
	bool acceptsAnswer(int32_t answer) const override;
	void onRemoval() override;
};
