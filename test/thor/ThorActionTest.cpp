#include "StdInc.h"

#include "../../lib/thor/ThorAction.h"

#include <limits>
#include <thread>

TEST(ThorActionTest, MapsOnlyStablePublicIdentifiers)
{
	EXPECT_EQ(thorActionFromId(0), std::nullopt);
	EXPECT_EQ(thorActionFromId(1), ThorAction::OPEN_KINGDOM_OVERVIEW);
	EXPECT_EQ(thorActionFromId(2), ThorAction::OPEN_QUEST_LOG);
	EXPECT_EQ(thorActionFromId(3), ThorAction::OPEN_PUZZLE_MAP);
	EXPECT_EQ(thorActionFromId(4), ThorAction::OPEN_SAVE_GAME);
	EXPECT_EQ(thorActionFromId(5), ThorAction::NEXT_HERO);
	EXPECT_EQ(thorActionFromId(6), ThorAction::MOVE_HERO);
	EXPECT_EQ(thorActionFromId(7), ThorAction::TOGGLE_HERO_SLEEP);
	EXPECT_EQ(thorActionFromId(8), ThorAction::END_TURN);
	EXPECT_EQ(thorActionFromId(9), ThorAction::BATTLE_WAIT);
	EXPECT_EQ(thorActionFromId(10), ThorAction::BATTLE_DEFEND);
	EXPECT_EQ(thorActionFromId(11), ThorAction::BATTLE_TACTICS_NEXT);
	EXPECT_EQ(thorActionFromId(12), ThorAction::BATTLE_TACTICS_END);
	EXPECT_EQ(thorActionFromId(13), ThorAction::SELECT_HERO);
	EXPECT_EQ(thorActionFromId(14), ThorAction::SELECT_TOWN);
	EXPECT_EQ(thorActionFromId(15), ThorAction::HERO_MEETING_MOVE_STACK);
	EXPECT_EQ(thorActionFromId(16), ThorAction::HERO_MEETING_TRANSFER_STACK);
	EXPECT_EQ(thorActionFromId(17), ThorAction::HERO_MEETING_ARMY_LEFT_TO_RIGHT);
	EXPECT_EQ(thorActionFromId(18), ThorAction::HERO_MEETING_ARMY_RIGHT_TO_LEFT);
	EXPECT_EQ(thorActionFromId(19), ThorAction::HERO_MEETING_SWAP_ARMIES);
	EXPECT_EQ(thorActionFromId(20), ThorAction::HERO_MEETING_SPLIT_STACK);
	EXPECT_EQ(thorActionFromId(21), std::nullopt);
	EXPECT_EQ(thorActionFromId(-1), std::nullopt);
}

TEST(ThorActionTest, HeroMeetingPairEncodingCoversEveryCrossArmySlotPair)
{
	for(int source = 0; source < static_cast<int>(THOR_HERO_MEETING_ARMY_SIZE * 2); ++source)
	{
		for(int destination = 0; destination < static_cast<int>(THOR_HERO_MEETING_ARMY_SIZE * 2); ++destination)
		{
			const auto encoded = encodeThorHeroMeetingTransferPair(source, destination);
			if(source / 7 == destination / 7)
			{
				EXPECT_FALSE(encoded.has_value());
				continue;
			}
			ASSERT_TRUE(encoded.has_value());
			EXPECT_EQ(*encoded, source * 14 + destination);
			const auto decoded = decodeThorHeroMeetingTransferPair(*encoded);
			ASSERT_TRUE(decoded.has_value());
			const ThorHeroMeetingTransferPair expected{source, destination, source < 7, source % 7,
				destination < 7, destination % 7};
			EXPECT_EQ(*decoded, expected);
		}
	}
}

TEST(ThorActionTest, HeroMeetingPairEncodingRejectsMalformedAndOverflowValues)
{
	EXPECT_FALSE(encodeThorHeroMeetingTransferPair(-1, 7));
	EXPECT_FALSE(encodeThorHeroMeetingTransferPair(14, 0));
	EXPECT_FALSE(encodeThorHeroMeetingTransferPair(3, 3));
	EXPECT_FALSE(encodeThorHeroMeetingTransferPair(0, 6));
	EXPECT_FALSE(encodeThorHeroMeetingTransferPair(7, 7));
	EXPECT_TRUE(encodeThorHeroMeetingTransferPair(0, 7));
	EXPECT_FALSE(decodeThorHeroMeetingTransferPair(-1));
	EXPECT_FALSE(decodeThorHeroMeetingTransferPair(196));
	EXPECT_FALSE(decodeThorHeroMeetingTransferPair(std::numeric_limits<int>::max()));
	EXPECT_FALSE(decodeThorHeroMeetingTransferPair(3 * 14 + 3));
	EXPECT_FALSE(decodeThorHeroMeetingTransferPair(3 * 14 + 4));
}

TEST(ThorActionTest, HeroMeetingMoveAllAvailabilityRequiresARealStackChange)
{
	ThorHeroMeetingArmies armies;
	for(std::size_t index = 0; index < THOR_HERO_MEETING_ARMY_SIZE; ++index)
	{
		armies.leftSlots[index] = {1, static_cast<int>(index), false, -1, {}, 0};
		armies.rightSlots[index] = {2, static_cast<int>(index), false, -1, {}, 0};
	}
	EXPECT_FALSE(canThorHeroMeetingMoveArmy(armies, true));
	EXPECT_FALSE(canThorHeroMeetingMoveArmy(armies, false));

	armies.rightSlots[0] = {2, 0, true, 3, "Pikemen", 1};
	EXPECT_FALSE(canThorHeroMeetingMoveArmy(armies, false)); // The source hero must keep its final unit.
	armies.rightSlots[0].count = 2;
	EXPECT_TRUE(canThorHeroMeetingMoveArmy(armies, false));

	for(std::size_t index = 0; index < THOR_HERO_MEETING_ARMY_SIZE; ++index)
		armies.leftSlots[index] = {1, static_cast<int>(index), true, static_cast<int>(index + 10), "Other", 2};
	EXPECT_FALSE(canThorHeroMeetingMoveArmy(armies, false)); // No free destination or matching creature.
	armies.leftSlots[6].creatureId = 3;
	EXPECT_TRUE(canThorHeroMeetingMoveArmy(armies, false)); // A full destination can still merge.
	armies.rightSlots[0].count = 1;
	EXPECT_FALSE(canThorHeroMeetingMoveArmy(armies, false));
	armies.rightSlots[1] = {2, 1, true, 4, "Archers", 1};
	EXPECT_TRUE(canThorHeroMeetingMoveArmy(armies, false)); // The other stack can remain while Pikemen move.
}

TEST(ThorActionTest, AllowsOnlySliceTwelveAdventureActions)
{
	EXPECT_FALSE(isThorActionAllowedInAdventureMap(ThorAction::NONE));
	EXPECT_TRUE(isThorActionAllowedInAdventureMap(ThorAction::OPEN_KINGDOM_OVERVIEW));
	EXPECT_TRUE(isThorActionAllowedInAdventureMap(ThorAction::OPEN_QUEST_LOG));
	EXPECT_TRUE(isThorActionAllowedInAdventureMap(ThorAction::OPEN_PUZZLE_MAP));
	EXPECT_TRUE(isThorActionAllowedInAdventureMap(ThorAction::OPEN_SAVE_GAME));
	EXPECT_TRUE(isThorActionAllowedInAdventureMap(ThorAction::NEXT_HERO));
	EXPECT_TRUE(isThorActionAllowedInAdventureMap(ThorAction::MOVE_HERO));
	EXPECT_TRUE(isThorActionAllowedInAdventureMap(ThorAction::TOGGLE_HERO_SLEEP));
	EXPECT_TRUE(isThorActionAllowedInAdventureMap(ThorAction::END_TURN));
	EXPECT_TRUE(isThorActionAllowedInAdventureMap(ThorAction::SELECT_HERO));
	EXPECT_TRUE(isThorActionAllowedInAdventureMap(ThorAction::SELECT_TOWN));
	EXPECT_FALSE(isThorActionAllowedInAdventureMap(static_cast<ThorAction>(99)));
}

TEST(ThorActionTest, AllowsOnlyActionsForTheirExactContext)
{
	EXPECT_TRUE(isThorActionAllowedInContext(ThorAction::OPEN_SAVE_GAME, ThorContextIds::ADVENTURE_MAP));
	EXPECT_FALSE(isThorActionAllowedInContext(ThorAction::BATTLE_WAIT, ThorContextIds::ADVENTURE_MAP));
	EXPECT_TRUE(isThorActionAllowedInContext(ThorAction::BATTLE_WAIT, ThorContextIds::BATTLE));
	EXPECT_TRUE(isThorActionAllowedInContext(ThorAction::BATTLE_DEFEND, ThorContextIds::BATTLE));
	EXPECT_FALSE(isThorActionAllowedInContext(ThorAction::BATTLE_TACTICS_NEXT, ThorContextIds::BATTLE));
	EXPECT_TRUE(isThorActionAllowedInContext(ThorAction::BATTLE_TACTICS_NEXT, ThorContextIds::BATTLE_TACTICS));
	EXPECT_TRUE(isThorActionAllowedInContext(ThorAction::BATTLE_TACTICS_END, ThorContextIds::BATTLE_TACTICS));
	EXPECT_FALSE(isThorActionAllowedInContext(ThorAction::BATTLE_WAIT, ThorContextIds::BATTLE_RESULT));
	EXPECT_FALSE(isThorActionAllowedInContext(ThorAction::BATTLE_DEFEND, ThorContextIds::HERO_WINDOW));
	EXPECT_TRUE(isThorActionAllowedInContext(ThorAction::HERO_MEETING_MOVE_STACK, ThorContextIds::HERO_MEETING));
	EXPECT_TRUE(isThorActionAllowedInContext(ThorAction::HERO_MEETING_TRANSFER_STACK, ThorContextIds::HERO_MEETING));
	EXPECT_TRUE(isThorActionAllowedInContext(ThorAction::HERO_MEETING_SPLIT_STACK, ThorContextIds::HERO_MEETING));
	EXPECT_FALSE(isThorActionAllowedInContext(ThorAction::HERO_MEETING_SPLIT_STACK, ThorContextIds::ADVENTURE_MAP));
	EXPECT_FALSE(isThorActionAllowedInContext(ThorAction::HERO_MEETING_TRANSFER_STACK, ThorContextIds::ADVENTURE_MAP));
}

TEST(ThorActionTest, GameplayActionsUseExplicitMasks)
{
	EXPECT_EQ(thorActionMask(ThorAction::NEXT_HERO), 16);
	EXPECT_EQ(thorActionMask(ThorAction::MOVE_HERO), 32);
	EXPECT_EQ(thorActionMask(ThorAction::TOGGLE_HERO_SLEEP), 64);
	EXPECT_EQ(thorActionMask(ThorAction::END_TURN), 128);
	EXPECT_EQ(thorActionMask(ThorAction::BATTLE_WAIT), 256);
	EXPECT_EQ(thorActionMask(ThorAction::BATTLE_DEFEND), 512);
	EXPECT_EQ(thorActionMask(ThorAction::BATTLE_TACTICS_NEXT), 1024);
	EXPECT_EQ(thorActionMask(ThorAction::BATTLE_TACTICS_END), 2048);
	EXPECT_EQ(thorActionMask(ThorAction::SELECT_HERO), 4096);
	EXPECT_EQ(thorActionMask(ThorAction::SELECT_TOWN), 8192);
	EXPECT_EQ(thorActionMask(ThorAction::HERO_MEETING_MOVE_STACK), 16384);
	EXPECT_EQ(thorActionMask(ThorAction::HERO_MEETING_TRANSFER_STACK), 32768);
	EXPECT_EQ(thorActionMask(ThorAction::HERO_MEETING_ARMY_LEFT_TO_RIGHT), 65536);
	EXPECT_EQ(thorActionMask(ThorAction::HERO_MEETING_ARMY_RIGHT_TO_LEFT), 131072);
	EXPECT_EQ(thorActionMask(ThorAction::HERO_MEETING_SWAP_ARMIES), 262144);
	EXPECT_EQ(thorActionMask(ThorAction::HERO_MEETING_SPLIT_STACK), 524288);
}

TEST(ThorActionTest, HeroMeetingExactSplitValidatesEveryDetailedField)
{
	ThorContextRecord context;
	context.revision = 22;
	context.contextId = ThorContextIds::HERO_MEETING;
	context.enabledActionMask = thorActionMask(ThorAction::HERO_MEETING_SPLIT_STACK);
	ThorHeroMeetingArmies armies;
	armies.leftArmyId = armies.leftHeroId = 10;
	armies.rightArmyId = armies.rightHeroId = 20;
	armies.locallyControllable = true;
	for(int slot = 0; slot < 7; ++slot)
	{
		armies.leftSlots[slot] = {10, slot, false, -1, {}, 0};
		armies.rightSlots[slot] = {20, slot, false, -1, {}, 0};
	}
	armies.leftSlots[0] = {10, 0, true, 3, "Pikemen", 12};
	armies.leftSlots[2] = {10, 2, true, 3, "Pikemen", 4};
	armies.rightSlots[1] = {20, 1, true, 3, "Pikemen", 5};
	armies.rightSlots[2] = {20, 2, true, 4, "Archers", 5};
	context.heroMeetingArmies = armies;
	ThorActionRequest request{22, ThorAction::HERO_MEETING_SPLIT_STACK, -1, 10, 0, 20, 0, 1};
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::VALID); // Empty opposite slot.
	request.destinationSlot = 1;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::VALID); // Matching opposite stack.
	request.destinationArmyId = 10;
	request.destinationSlot = 2;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::VALID); // Matching same-army stack.
	request.destinationArmyId = 20;
	request.destinationSlot = 2;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::INVALID_TARGET); // Different creature.
	request.destinationSlot = 0;
	request.amount = 0;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::INVALID_TARGET);
	request.amount = 11;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::VALID);
	request.amount = 12;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::INVALID_TARGET);
	request.amount = 1;
	request.sourceSlot = 1;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::INVALID_TARGET); // Empty source.
	request.sourceSlot = 0;
	context.heroMeetingArmies->leftSlots[0].count = 1;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::INVALID_TARGET);
	context.heroMeetingArmies->leftSlots[0].count = 12;
	request.destinationArmyId = 99;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::INVALID_TARGET);
	request.destinationArmyId = 20;
	request.revision = 21;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::STALE_REVISION);
	request.revision = 22;
	context.enabledActionMask = 0;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::UNAVAILABLE);
	context.enabledActionMask = thorActionMask(ThorAction::HERO_MEETING_SPLIT_STACK);
	context.contextId = ThorContextIds::BATTLE;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::WRONG_CONTEXT);
}

TEST(ThorActionTest, HeroMeetingQuickMoveRequiresPublishedSourceOnly)
{
	ThorContextRecord context;
	context.revision = 20;
	context.contextId = ThorContextIds::HERO_MEETING;
	context.enabledActionMask = thorActionMask(ThorAction::HERO_MEETING_MOVE_STACK);
	ThorHeroMeetingArmies armies;
	armies.leftHeroId = 1;
	armies.rightHeroId = 2;
	armies.leftArmyId = 1;
	armies.rightArmyId = 2;
	armies.locallyControllable = true;
	for(std::size_t index = 0; index < THOR_HERO_MEETING_ARMY_SIZE; ++index)
	{
		armies.leftSlots[index].armyId = 1;
		armies.leftSlots[index].slot = static_cast<int>(index);
		armies.rightSlots[index].armyId = 2;
		armies.rightSlots[index].slot = static_cast<int>(index);
	}
	armies.leftSlots[0] = {1, 0, true, 3, "Pikemen", 12};
	armies.rightSlots[6] = {2, 6, true, 4, "Archers", 8};
	context.heroMeetingArmies = armies;
	ThorActionRequest request;
	request.revision = 20;
	request.action = ThorAction::HERO_MEETING_MOVE_STACK;
	request.targetId = 0;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::VALID);
	request.targetId = 13;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::VALID);
	request.targetId = 1; // Empty published source.
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::INVALID_TARGET);
	request.targetId = -1;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::INVALID_TARGET);
	request.targetId = 14;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::INVALID_TARGET);
	request.targetId = 0;
	request.sourceArmyId = 1; // Action 15 carries no endpoint fields.
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::INVALID_TARGET);
	request.sourceArmyId = -1;
	request.sourceSlot = 0;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::INVALID_TARGET);
	request.sourceSlot = -1;
	request.destinationArmyId = 2;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::INVALID_TARGET);
	request.destinationArmyId = -1;
	request.destinationSlot = 6;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::INVALID_TARGET);
	request.destinationSlot = -1;
	context.heroMeetingArmies->leftSlots[0].slot = 1;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::INVALID_TARGET);
	context.heroMeetingArmies->leftSlots[0].slot = 0;
	context.heroMeetingArmies->leftSlots[0].armyId = 2;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::INVALID_TARGET);
	context.heroMeetingArmies->leftSlots[0].armyId = 1;
	request.revision -= 1;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::STALE_REVISION);
	request.revision += 1;
	context.enabledActionMask = 0;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::UNAVAILABLE);
	context.enabledActionMask = thorActionMask(ThorAction::HERO_MEETING_MOVE_STACK);
	context.contextId = ThorContextIds::ADVENTURE_MAP;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::WRONG_CONTEXT);
	context.contextId = ThorContextIds::HERO_MEETING;
	context.heroMeetingArmies->locallyControllable = false;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::INVALID_TARGET);
}

TEST(ThorActionTest, HeroMeetingDragTransferRequiresValidPublishedOppositeArmyEndpoints)
{
	ThorContextRecord context;
	context.revision = 20;
	context.contextId = ThorContextIds::HERO_MEETING;
	context.enabledActionMask = thorActionMask(ThorAction::HERO_MEETING_TRANSFER_STACK);
	ThorHeroMeetingArmies armies;
	armies.leftHeroId = armies.leftArmyId = 1;
	armies.rightHeroId = armies.rightArmyId = 2;
	armies.locallyControllable = true;
	for(std::size_t index = 0; index < THOR_HERO_MEETING_ARMY_SIZE; ++index)
	{
		armies.leftSlots[index] = {1, static_cast<int>(index), false, -1, {}, 0};
		armies.rightSlots[index] = {2, static_cast<int>(index), false, -1, {}, 0};
	}
	armies.leftSlots[0] = {1, 0, true, 3, "Pikemen", 12};
	context.heroMeetingArmies = armies;
	ThorActionRequest request;
	request.revision = context.revision;
	request.action = ThorAction::HERO_MEETING_TRANSFER_STACK;
	request.targetId = *encodeThorHeroMeetingTransferPair(0, 13);
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::VALID);
	request.targetId = 6; // decodes to a same-side pair
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::INVALID_TARGET);
	request.targetId = 196;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::INVALID_TARGET);
	request.targetId = *encodeThorHeroMeetingTransferPair(7, 0); // right-to-left source is empty
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::INVALID_TARGET);
	request.targetId = *encodeThorHeroMeetingTransferPair(0, 13);
	request.sourceSlot = 0; // action 16 accepts only the bounded encoded pair field
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::INVALID_TARGET);
	request.sourceSlot = -1;
	request.revision -= 1;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::STALE_REVISION);
	request.revision += 1;
	context.heroMeetingArmies->locallyControllable = false;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::INVALID_TARGET);
	context.heroMeetingArmies->locallyControllable = true;
	context.heroMeetingArmies->leftSlots[0].occupied = false;
	context.heroMeetingArmies->leftSlots[0].creatureId = -1;
	context.heroMeetingArmies->leftSlots[0].count = 0;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::INVALID_TARGET);
	context.heroMeetingArmies->leftSlots[0].occupied = true;
	context.heroMeetingArmies->leftSlots[0].creatureId = 3;
	context.heroMeetingArmies->leftSlots[0].count = 12;
	context.heroMeetingArmies->rightSlots[6].armyId = 9;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::INVALID_TARGET);
	context.heroMeetingArmies->rightSlots[6].armyId = 2;
	context.heroMeetingArmies->rightSlots[6].slot = 5;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::INVALID_TARGET);
	context.heroMeetingArmies->rightSlots[6].slot = 6;
	context.enabledActionMask = 0;
	EXPECT_EQ(validateThorActionRequest(request, context), ThorActionValidation::UNAVAILABLE);
}

TEST(ThorActionTest, TownTargetMustMatchPublishedRosterAndRevision)
{
	ThorContextRecord context;
	context.revision = 8;
	context.contextId = ThorContextIds::ADVENTURE_MAP;
	context.enabledActionMask = thorActionMask(ThorAction::SELECT_TOWN);
	context.towns.push_back({42, "Castle Stronghold", false});
	EXPECT_EQ(validateThorActionRequest({8, ThorAction::SELECT_TOWN, 42}, context), ThorActionValidation::VALID);
	EXPECT_EQ(validateThorActionRequest({8, ThorAction::SELECT_TOWN}, context), ThorActionValidation::INVALID_TARGET);
	EXPECT_EQ(validateThorActionRequest({8, ThorAction::SELECT_TOWN, 43}, context), ThorActionValidation::INVALID_TARGET);
	EXPECT_EQ(validateThorActionRequest({7, ThorAction::SELECT_TOWN, 42}, context), ThorActionValidation::STALE_REVISION);
	context.contextId = ThorContextIds::TOWN_WINDOW;
	EXPECT_EQ(validateThorActionRequest({8, ThorAction::SELECT_TOWN, 42}, context), ThorActionValidation::WRONG_CONTEXT);
	context.contextId = ThorContextIds::ADVENTURE_MAP;
	context.enabledActionMask = 0;
	EXPECT_EQ(validateThorActionRequest({8, ThorAction::SELECT_TOWN, 42}, context), ThorActionValidation::UNAVAILABLE);
	context.enabledActionMask = thorActionMask(ThorAction::SELECT_TOWN);
	context.towns.clear();
	EXPECT_EQ(validateThorActionRequest({8, ThorAction::SELECT_TOWN, 42}, context), ThorActionValidation::INVALID_TARGET);
}

TEST(ThorActionTest, TargetMustMatchPublishedHeroAndRevision)
{
	ThorContextRecord context;
	context.revision = 7;
	context.contextId = ThorContextIds::ADVENTURE_MAP;
	context.enabledActionMask = thorActionMask(ThorAction::SELECT_HERO);
	context.heroes.push_back({42, "Hero", 100, 200, false, false});
	EXPECT_EQ(validateThorActionRequest({7, ThorAction::SELECT_HERO, 42}, context), ThorActionValidation::VALID);
	EXPECT_EQ(validateThorActionRequest({7, ThorAction::SELECT_HERO}, context), ThorActionValidation::INVALID_TARGET);
	EXPECT_EQ(validateThorActionRequest({7, ThorAction::SELECT_HERO, 43}, context), ThorActionValidation::INVALID_TARGET);
	EXPECT_EQ(validateThorActionRequest({6, ThorAction::SELECT_HERO, 42}, context), ThorActionValidation::STALE_REVISION);
	context.contextId = ThorContextIds::HERO_WINDOW;
	EXPECT_EQ(validateThorActionRequest({7, ThorAction::SELECT_HERO, 42}, context), ThorActionValidation::WRONG_CONTEXT);
	EXPECT_EQ(validateThorActionRequest({7, ThorAction::OPEN_SAVE_GAME, 42}, context), ThorActionValidation::WRONG_CONTEXT);
	context.contextId = ThorContextIds::ADVENTURE_MAP;
	context.heroes.clear(); // ownership/removal refresh invalidates the published target
	EXPECT_EQ(validateThorActionRequest({7, ThorAction::SELECT_HERO, 42}, context), ThorActionValidation::INVALID_TARGET);
}

TEST(ThorActionTest, ValidatesRevisionContextAndAvailability)
{
	ThorContextRecord context;
	context.revision = 12;
	context.contextId = ThorContextIds::ADVENTURE_MAP;
	context.enabledActionMask = thorActionMask(ThorAction::OPEN_SAVE_GAME);

	EXPECT_EQ(validateThorActionRequest({12, ThorAction::OPEN_SAVE_GAME}, context), ThorActionValidation::VALID);
	EXPECT_EQ(validateThorActionRequest({12, ThorAction::OPEN_SAVE_GAME, 999}, context), ThorActionValidation::VALID);
	EXPECT_EQ(validateThorActionRequest({11, ThorAction::OPEN_SAVE_GAME}, context), ThorActionValidation::STALE_REVISION);
	EXPECT_EQ(validateThorActionRequest({12, ThorAction::OPEN_PUZZLE_MAP}, context), ThorActionValidation::UNAVAILABLE);
	context.contextId = ThorContextIds::HERO_WINDOW;
	EXPECT_EQ(validateThorActionRequest({12, ThorAction::OPEN_SAVE_GAME}, context), ThorActionValidation::WRONG_CONTEXT);
	EXPECT_EQ(validateThorActionRequest({12, static_cast<ThorAction>(99)}, context), ThorActionValidation::UNKNOWN_ACTION);
}

TEST(ThorActionTest, ValidatesBattleContextsSeparately)
{
	ThorContextRecord context;
	context.revision = 42;
	context.contextId = ThorContextIds::BATTLE;
	context.enabledActionMask = thorActionMask(ThorAction::BATTLE_WAIT);

	EXPECT_EQ(validateThorActionRequest({42, ThorAction::BATTLE_WAIT}, context), ThorActionValidation::VALID);
	EXPECT_EQ(validateThorActionRequest({42, ThorAction::BATTLE_DEFEND}, context), ThorActionValidation::UNAVAILABLE);
	EXPECT_EQ(validateThorActionRequest({42, ThorAction::OPEN_SAVE_GAME}, context), ThorActionValidation::WRONG_CONTEXT);
	context.actionSubjectId = 101; // read-only opponent information does not enable commands
	context.enabledActionMask = 0;
	EXPECT_EQ(validateThorActionRequest({42, ThorAction::BATTLE_WAIT}, context), ThorActionValidation::UNAVAILABLE);

	context.contextId = ThorContextIds::BATTLE_TACTICS;
	context.enabledActionMask = thorActionMask(ThorAction::BATTLE_TACTICS_END);
	EXPECT_EQ(validateThorActionRequest({42, ThorAction::BATTLE_TACTICS_END}, context), ThorActionValidation::VALID);
	EXPECT_EQ(validateThorActionRequest({42, ThorAction::BATTLE_WAIT}, context), ThorActionValidation::WRONG_CONTEXT);

	context.contextId = ThorContextIds::BATTLE_RESULT;
	context.enabledActionMask = thorActionMask(ThorAction::BATTLE_WAIT);
	EXPECT_EQ(validateThorActionRequest({42, ThorAction::BATTLE_WAIT}, context), ThorActionValidation::WRONG_CONTEXT);
}

TEST(ThorActionTest, ActiveActionStateCreatesOneNewRevision)
{
	ThorContextStore store;
	ThorContextRecord context;
	context.contextId = ThorContextIds::ADVENTURE_MAP;
	context.enabledActionMask = thorActionMask(ThorAction::TOGGLE_HERO_SLEEP);
	const auto awake = store.publishNext(context);
	EXPECT_EQ(awake.revision, 1);
	EXPECT_EQ(store.publishNext(context).revision, awake.revision);

	context.activeActionMask = thorActionMask(ThorAction::TOGGLE_HERO_SLEEP);
	const auto asleep = store.publishNext(context);
	EXPECT_EQ(asleep.revision, awake.revision + 1);
	EXPECT_EQ(store.publishNext(context).revision, asleep.revision);
}

TEST(ThorActionTest, ChangedSelectedHeroRejectsStaleAction)
{
	ThorContextStore store;
	ThorContextRecord context;
	context.contextId = ThorContextIds::ADVENTURE_MAP;
	context.enabledActionMask = thorActionMask(ThorAction::MOVE_HERO);
	context.selectedHeroId = 4;
	const auto firstHero = store.publishNext(context);

	context.selectedHeroId = 9;
	const auto secondHero = store.publishNext(context);
	EXPECT_EQ(secondHero.revision, firstHero.revision + 1);
	EXPECT_EQ(validateThorActionRequest({firstHero.revision, ThorAction::MOVE_HERO}, secondHero), ThorActionValidation::STALE_REVISION);
}

TEST(ThorActionTest, ConsumedActionInvalidatesItsRenderedRevision)
{
	ThorContextStore store;
	ThorContextRecord context;
	context.contextId = ThorContextIds::ADVENTURE_MAP;
	context.enabledActionMask = thorActionMask(ThorAction::MOVE_HERO);
	const auto rendered = store.publishNext(context);

	++context.actionEpoch;
	const auto consumed = store.publishNext(context);
	EXPECT_EQ(consumed.revision, rendered.revision + 1);
	EXPECT_EQ(validateThorActionRequest({rendered.revision, ThorAction::MOVE_HERO}, consumed), ThorActionValidation::STALE_REVISION);
}

TEST(ThorActionTest, ConsumedTownActionInvalidatesItsRenderedRevision)
{
	ThorContextStore store;
	ThorContextRecord context;
	context.contextId = ThorContextIds::ADVENTURE_MAP;
	context.enabledActionMask = thorActionMask(ThorAction::SELECT_TOWN);
	context.towns.push_back({4, "Castle Stronghold", true});
	const auto rendered = store.publishNext(context);

	++context.actionEpoch;
	const auto consumed = store.publishNext(context);
	EXPECT_EQ(consumed.revision, rendered.revision + 1);
	EXPECT_EQ(validateThorActionRequest({rendered.revision, ThorAction::SELECT_TOWN, 4}, consumed),
		ThorActionValidation::STALE_REVISION);
}

TEST(ThorActionTest, BattleActionSubjectChangesRevisionWithoutMaskChurn)
{
	ThorContextStore store;
	ThorContextRecord context;
	context.contextId = ThorContextIds::BATTLE;
	context.enabledActionMask = thorActionMask(ThorAction::BATTLE_WAIT) | thorActionMask(ThorAction::BATTLE_DEFEND);
	context.actionSubjectId = 101;
	const auto firstStack = store.publishNext(context);
	EXPECT_EQ(store.publishNext(context).revision, firstStack.revision);

	context.actionSubjectId = 202;
	const auto secondStack = store.publishNext(context);
	EXPECT_EQ(secondStack.revision, firstStack.revision + 1);
	EXPECT_EQ(validateThorActionRequest({firstStack.revision, ThorAction::BATTLE_WAIT}, secondStack), ThorActionValidation::STALE_REVISION);
}

TEST(ThorActionTest, ConsumingBattleActionClearsRenderedAvailability)
{
	ThorContextStore store;
	ThorContextRecord context;
	context.contextId = ThorContextIds::BATTLE;
	context.enabledActionMask = thorActionMask(ThorAction::BATTLE_DEFEND);
	context.actionSubjectId = 7;
	const auto rendered = store.publishNext(context);

	++context.actionEpoch;
	context.enabledActionMask = 0;
	const auto consumed = store.publishNext(context);
	EXPECT_GT(consumed.revision, rendered.revision);
	EXPECT_EQ(consumed.enabledActionMask, 0);
	EXPECT_EQ(validateThorActionRequest({rendered.revision, ThorAction::BATTLE_DEFEND}, consumed), ThorActionValidation::STALE_REVISION);
}

TEST(ThorActionQueueTest, KeepsRequestsBoundedAndOrdered)
{
	ThorActionQueue queue;
	for(std::uint64_t revision = 1; revision <= 16; ++revision)
		EXPECT_TRUE(queue.submit({revision, ThorAction::OPEN_SAVE_GAME}));

	EXPECT_FALSE(queue.submit({17, ThorAction::OPEN_SAVE_GAME}));
	EXPECT_EQ(queue.size(), 16);

	for(std::uint64_t revision = 1; revision <= 16; ++revision)
	{
		const auto request = queue.pop();
		ASSERT_TRUE(request.has_value());
		EXPECT_EQ(request->revision, revision);
	}
	EXPECT_FALSE(queue.pop().has_value());
}

TEST(ThorActionQueueTest, ConcurrentSubmissionsRemainBounded)
{
	ThorActionQueue queue;
	auto submit = [&queue](std::uint64_t firstRevision)
	{
		for(std::uint64_t revision = firstRevision; revision < firstRevision + 32; ++revision)
			queue.submit({revision, ThorAction::OPEN_SAVE_GAME});
	};

	std::thread first(submit, 1);
	std::thread second(submit, 100);
	first.join();
	second.join();

	EXPECT_EQ(queue.size(), 16);
	queue.clear();
	EXPECT_EQ(queue.size(), 0);
}
