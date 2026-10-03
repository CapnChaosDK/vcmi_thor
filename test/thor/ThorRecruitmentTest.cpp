/*
 * ThorRecruitmentTest.cpp, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 *
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in the main folder
 *
 */
#include "StdInc.h"

#include "../../lib/thor/ThorAction.h"
#include "../../lib/thor/ThorContext.h"

#include <limits>

namespace
{
	ThorContextRecord makeQuickRecruitmentContext()
	{
		ThorContextRecord context;
		context.contextId = ThorContextIds::TOWN_RECRUITMENT_QUICK;
		context.actionSubjectId = 42;
		context.enabledActionMask = thorActionMask(ThorAction::RECRUITMENT_EDIT)
			| thorActionMask(ThorAction::RECRUITMENT_BUY) | thorActionMask(ThorAction::WINDOW_CLOSE);
		ThorRecruitmentSnapshot recruitment;
		recruitment.mode = ThorRecruitmentMode::QUICK_TOWN;
		recruitment.townId = 42;
		recruitment.destinationArmyId = 99;
		recruitment.destinationArmyFreeSlots = 5;
		recruitment.selectedTarget = 3;
		recruitment.townName = "Town";
		recruitment.totalCost = "300 Gold";
		recruitment.locallyControllable = true;
		recruitment.canBuy = true;
		recruitment.rows = {
			{3, 7, "Pikemen", "100 Gold", "300 Gold", 12, 3, 8, 0, 2, true, true, thorCreatureVisualAssetKey(7)},
			{4, 8, "Archers", "150 Gold", "", 8, 0, 6, 0, 1, true, false, thorCreatureVisualAssetKey(8)}};
		context.recruitment = std::move(recruitment);
		return context;
	}
}

TEST(ThorRecruitmentTest, TypedEditsAndBuyAreBoundToTheExactModeRevisionAndNativeOwner)
{
	auto context = makeQuickRecruitmentContext();
	context.revision = 81;
	EXPECT_TRUE(thorRecruitmentOwnerMatches(context, ThorRecruitmentMode::QUICK_TOWN,
		42, -1, 99, 5, true, true));
	EXPECT_FALSE(thorRecruitmentOwnerMatches(context, ThorRecruitmentMode::QUICK_TOWN,
		42, -1, 99, 5, false, true));
	EXPECT_FALSE(thorRecruitmentOwnerMatches(context, ThorRecruitmentMode::QUICK_TOWN,
		42, -1, 99, 5, true, false));
	EXPECT_FALSE(thorRecruitmentOwnerMatches(context, ThorRecruitmentMode::QUICK_TOWN,
		43, -1, 99, 5, true, true));
	EXPECT_FALSE(thorRecruitmentOwnerMatches(context, ThorRecruitmentMode::QUICK_TOWN,
		42, -1, 98, 5, true, true));
	EXPECT_FALSE(thorRecruitmentOwnerMatches(context, ThorRecruitmentMode::QUICK_TOWN,
		42, -1, 99, 4, true, true));
	EXPECT_FALSE(thorRecruitmentOwnerMatches(context, ThorRecruitmentMode::TOWN_DWELLING,
		42, 0, 99, 5, true, true));
	EXPECT_TRUE(isThorActionAllowedInContext(ThorAction::RECRUITMENT_EDIT, ThorContextIds::TOWN_RECRUITMENT_QUICK));
	EXPECT_TRUE(isThorActionAllowedInContext(ThorAction::RECRUITMENT_BUY, ThorContextIds::TOWN_RECRUITMENT_DWELLING));
	EXPECT_FALSE(isThorActionAllowedInContext(ThorAction::RECRUITMENT_EDIT, ThorContextIds::TOWN_WINDOW));

	ThorActionRequest edit;
	edit.revision = 81;
	edit.action = ThorAction::RECRUITMENT_EDIT;
	edit.targetId = 3;
	edit.recruitmentOperation = ThorRecruitmentOperation::SELECT_ROW;
	EXPECT_EQ(validateThorActionRequest(edit, context), ThorActionValidation::VALID);
	auto consumed = context;
	consumed.revision = 82;
	EXPECT_EQ(validateThorActionRequest(edit, consumed), ThorActionValidation::STALE_REVISION);
	edit.revision = 80;
	EXPECT_EQ(validateThorActionRequest(edit, context), ThorActionValidation::STALE_REVISION);
	edit.revision = 81;
	edit.targetId = 99;
	EXPECT_EQ(validateThorActionRequest(edit, context), ThorActionValidation::INVALID_TARGET);
	edit.targetId = 3;
	edit.recruitmentOperation = ThorRecruitmentOperation::CYCLE_VARIANT;
	EXPECT_EQ(validateThorActionRequest(edit, context), ThorActionValidation::VALID);
	context.recruitment->rows[0].variantCount = 1;
	EXPECT_EQ(validateThorActionRequest(edit, context), ThorActionValidation::INVALID_TARGET);
	context.recruitment->rows[0].variantCount = 2;
	context.recruitment->rows[0].enabled = false;
	EXPECT_EQ(validateThorActionRequest(edit, context), ThorActionValidation::INVALID_TARGET);
	context.recruitment->rows[0].enabled = true;
	ThorContextStore acceptedEditStore;
	auto acceptedEditContext = acceptedEditStore.publishNext(context);
	edit.revision = acceptedEditContext.revision;
	EXPECT_EQ(validateThorActionRequest(edit, acceptedEditContext), ThorActionValidation::VALID);
	++acceptedEditContext.actionEpoch;
	acceptedEditContext = acceptedEditStore.publishNext(acceptedEditContext);
	EXPECT_EQ(validateThorActionRequest(edit, acceptedEditContext), ThorActionValidation::STALE_REVISION);
	edit.revision = 81;

	ThorActionRequest buy;
	buy.revision = 81;
	buy.action = ThorAction::RECRUITMENT_BUY;
	context.recruitment->rows[0].enabled = true;
	EXPECT_EQ(validateThorActionRequest(buy, context), ThorActionValidation::VALID);
	context.recruitment->locallyControllable = false;
	EXPECT_EQ(validateThorActionRequest(buy, context), ThorActionValidation::INVALID_TARGET);
	context.recruitment->locallyControllable = true;
	context.recruitment->canBuy = false;
	EXPECT_EQ(validateThorActionRequest(buy, context), ThorActionValidation::INVALID_TARGET);
	ThorActionRequest close;
	close.revision = 81;
	close.action = ThorAction::WINDOW_CLOSE;
	EXPECT_EQ(validateThorActionRequest(close, context), ThorActionValidation::VALID);
	context.recruitment->canBuy = true;
	context.recruitment->rows[0].enabled = false;
	EXPECT_EQ(validateThorActionRequest(buy, context), ThorActionValidation::INVALID_TARGET);
	context.recruitment->rows[0].enabled = true;
	context.recruitment->rows[0].armyAvailable = false;
	EXPECT_EQ(validateThorActionRequest(buy, context), ThorActionValidation::INVALID_TARGET);
	context.recruitment->rows[0].armyAvailable = true;
	buy.recruitmentOperation = ThorRecruitmentOperation::SELECT_ROW;
	EXPECT_EQ(validateThorActionRequest(buy, context), ThorActionValidation::INVALID_TARGET);

	EXPECT_EQ(thorRecruitmentOperationFromId(0), std::nullopt);
	EXPECT_EQ(thorRecruitmentOperationFromId(1), ThorRecruitmentOperation::SELECT_ROW);
	EXPECT_EQ(thorRecruitmentOperationFromId(8), ThorRecruitmentOperation::CYCLE_VARIANT);
	EXPECT_EQ(thorRecruitmentOperationFromId(9), std::nullopt);
	EXPECT_EQ(thorRecruitmentAmountAfter(ThorRecruitmentOperation::DECREASE_10, 4, 12), 0);
	EXPECT_EQ(thorRecruitmentAmountAfter(ThorRecruitmentOperation::INCREASE_1, 9, 10), 10);
	EXPECT_EQ(thorRecruitmentAmountAfter(ThorRecruitmentOperation::INCREASE_10, 0, 7), 7);
	EXPECT_EQ(thorRecruitmentAmountAfter(ThorRecruitmentOperation::SET_MINIMUM, 5, 10), 0);
	EXPECT_EQ(thorRecruitmentAmountAfter(ThorRecruitmentOperation::SET_MAXIMUM, 5, 10), 10);
	EXPECT_EQ(thorRecruitmentAmountAfter(ThorRecruitmentOperation::INCREASE_10,
		std::numeric_limits<int>::max() - 1, std::numeric_limits<int>::max()), std::numeric_limits<int>::max());
	EXPECT_FALSE(thorRecruitmentAmountAfter(ThorRecruitmentOperation::CYCLE_VARIANT, 0, 1));
}

TEST(ThorRecruitmentTest, SharedSnapshotCoversQuickAndDwellingModesAndChangesRevisionForLiveState)
{
	ThorContextStore store;
	auto context = makeQuickRecruitmentContext();
	const auto first = store.publishNext(context);
	ASSERT_TRUE(first.recruitment);
	EXPECT_EQ(first.recruitment->rows.size(), 2);
	EXPECT_EQ(first.recruitment->rows[0].visualAssetKey, thorCreatureVisualAssetKey(7));
	EXPECT_EQ(store.publishNext(context).revision, first.revision);

	context.recruitment->rows[0].selectedAmount = 4;
	const auto selectionChanged = store.publishNext(context);
	EXPECT_EQ(selectionChanged.revision, first.revision + 1);
	context.recruitment->destinationArmyFreeSlots = 4;
	const auto capacityChanged = store.publishNext(context);
	EXPECT_EQ(capacityChanged.revision, selectionChanged.revision + 1);
	context.recruitment->rows[0].maximumAmount = 7;
	const auto affordabilityChanged = store.publishNext(context);
	EXPECT_EQ(affordabilityChanged.revision, capacityChanged.revision + 1);
	context.recruitment->rows[0].availableCount = 11;
	const auto stockChanged = store.publishNext(context);
	EXPECT_EQ(stockChanged.revision, affordabilityChanged.revision + 1);
	context.recruitment->rows[0].variantIndex = 1;
	const auto variantChanged = store.publishNext(context);
	EXPECT_EQ(variantChanged.revision, stockChanged.revision + 1);
	context.recruitment->rows[0].armyAvailable = false;
	context.recruitment->rows[0].enabled = false;
	context.recruitment->canBuy = false;
	const auto capacityAvailabilityChanged = store.publishNext(context);
	ASSERT_TRUE(capacityAvailabilityChanged.recruitment);
	EXPECT_EQ(capacityAvailabilityChanged.revision, variantChanged.revision + 1);
	EXPECT_FALSE(capacityAvailabilityChanged.recruitment->rows[0].armyAvailable);
	EXPECT_FALSE(capacityAvailabilityChanged.recruitment->rows[0].enabled);
	EXPECT_EQ(capacityAvailabilityChanged.enabledActionMask & thorActionMask(ThorAction::RECRUITMENT_BUY), 0);

	auto dwelling = makeQuickRecruitmentContext();
	dwelling.contextId = ThorContextIds::TOWN_RECRUITMENT_DWELLING;
	dwelling.recruitment->mode = ThorRecruitmentMode::TOWN_DWELLING;
	dwelling.recruitment->dwellingLevel = 2;
	const auto ordinary = store.publishNext(dwelling);
	ASSERT_TRUE(ordinary.recruitment);
	EXPECT_EQ(ordinary.recruitment->mode, ThorRecruitmentMode::TOWN_DWELLING);
	EXPECT_EQ(ordinary.recruitment->dwellingLevel, 2);
}

TEST(ThorRecruitmentTest, MalformedBoundsDuplicatesAndOwnershipFailClosed)
{
	ThorContextStore store;
	auto context = makeQuickRecruitmentContext();
	context.recruitment->rows[0].name = std::string(129, 'x');
	EXPECT_FALSE(store.publishNext(context).recruitment);

	context = makeQuickRecruitmentContext();
	context.recruitment->rows.push_back(context.recruitment->rows[0]);
	EXPECT_FALSE(store.publishNext(context).recruitment);

	context = makeQuickRecruitmentContext();
	context.recruitment->locallyControllable = false;
	context.recruitment->rows[0].enabled = true;
	EXPECT_FALSE(store.publishNext(context).recruitment);

	context = makeQuickRecruitmentContext();
	context.contextId = ThorContextIds::TOWN_RECRUITMENT_DWELLING;
	context.recruitment->mode = ThorRecruitmentMode::TOWN_DWELLING;
	context.recruitment->dwellingLevel = 2;
	context.recruitment->rows[1].selectedAmount = 1;
	context.recruitment->rows[1].maximumAmount = 1;
	EXPECT_FALSE(store.publishNext(context).recruitment);

	context = makeQuickRecruitmentContext();
	context.recruitment->selectedTarget = -1;
	context.recruitment->canBuy = false;
	context.recruitment->rows.clear();
	for(std::size_t index = 0; index < THOR_MAX_RECRUITMENT_ROWS + 1; ++index)
	{
		ThorRecruitmentRow row;
		row.target = static_cast<int>(index);
		row.creatureId = static_cast<int>(index);
		row.name = "Creature";
		context.recruitment->rows.push_back(std::move(row));
	}
	EXPECT_FALSE(store.publishNext(context).recruitment);

	context = makeQuickRecruitmentContext();
	context.recruitment->rows[0].maximumAmount = THOR_MAX_RECRUITMENT_ROWS + 1;
	EXPECT_FALSE(store.publishNext(context).recruitment);
}
