/*
 * ArmyManagementTest.cpp, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 *
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 */
#include "StdInc.h"

#include "CGameHandler.h"
#include "mock/GameHandlerTestServer.h"
#include "mock/TinyMapGameTest.h"
#include "../../server/activities/MapActivities.h"
#include "../../server/activities/SpellActivities.h"
#include "../../server/activities/ActivityProcessor.h"
#include "lib/CPlayerState.h"
#include "lib/mapObjects/CGHeroInstance.h"
#include "lib/mapObjects/CGPandoraBox.h"
#include "lib/mapObjects/CGTownInstance.h"
#include "lib/mapObjects/MiscObjects.h"
#include "lib/entities/artifact/CArtifactInstance.h"
#include "lib/networkPacks/PacksForClient.h"
#include "lib/networkPacks/PacksForServer.h"

#include <limits>

namespace
{
const PlayerColor red(0);
const PlayerColor blue(1);
const CreatureID pikeman(0);
const CreatureID halberdier(1);
constexpr ui8 swapStacks = 1;
constexpr ui8 mergeStacks = 2;
constexpr ui8 splitStacks = 3;

class RecordingAnswerActivity : public DialogActivity
{
	std::function<void(std::optional<int32_t>)> callback;
public:
	RecordingAnswerActivity(CGameHandler * handler, PlayerColor player, std::function<void(std::optional<int32_t>)> callback)
		: DialogActivity(handler, ActivityType::ScriptDialog), callback(std::move(callback))
	{
		addPlayer(player);
		askQuestion();
	}
	void onRemoval() override { callback(answer); }
};

// Only transport is replaced: requests, authorization, queries, packet application,
// stack/artifact mutation and acknowledgements all use production code.
class ArmyTestServer : public GameHandlerTestServer
{
public:
	using GameHandlerTestServer::GameHandlerTestServer;
	std::vector<bool> results;
	std::vector<int> requestIDs;
	void sendPack(CPackForClient & pack, GameConnectionID connection) override
	{
		GameHandlerTestServer::sendPack(pack, connection);
		if(const auto * applied = dynamic_cast<const PackageApplied *>(&pack))
		{
			results.push_back(applied->result);
			requestIDs.push_back(static_cast<int>(applied->requestID));
		}
	}
};

class ArmyManagementTest : public TinyMapGameTest
{
protected:
	std::unique_ptr<ArmyTestServer> server;
	std::unique_ptr<CGameHandler> handler;
	CGHeroInstance * left = nullptr;
	CGHeroInstance * right = nullptr;

	void SetUp() override
	{
		TinyMapGameTest::SetUp();
		TinyH3M::TinyH3MBuilder builder(EMapFormat::SOD);
		builder.size(36, false).playerActive(red).playerActive(blue)
			.hero({5, 5, 0}, HeroTypeID(0), red).heroGarrison({{pikeman, 10}, {halberdier, 5}})
			.hero({8, 5, 0}, HeroTypeID(1), red).heroGarrison({{pikeman, 20}})
			.hero({20, 20, 0}, HeroTypeID(2), blue).heroGarrison({{pikeman, 1}})
			.playerActive(PlayerColor(2))
			.hero({25, 25, 0}, HeroTypeID(3), PlayerColor(2)).heroGarrison({{pikeman, 1}});
		startWithMap(std::move(builder));
		gameState()->actingPlayers = {red};
		server = std::make_unique<ArmyTestServer>(gameState(), red);
		handler = std::make_unique<CGameHandler>(*server, gameState());
		left = findHeroAt({5, 5, 0});
		right = findHeroAt({8, 5, 0});
		ASSERT_NE(left, nullptr);
		ASSERT_NE(right, nullptr);
	}

	void TearDown() override
	{
		handler.reset();
		server.reset();
		TinyMapGameTest::TearDown();
	}

	template<typename T>
	T * addObject(MapObjectID type, PlayerColor owner)
	{
		auto object = std::make_shared<T>(gameState().get());
		object->ID = type;
		object->id = ObjectInstanceID(static_cast<int>(map()->objects.size()));
		object->tempOwner = owner;
		map()->objects.push_back(object);
		return object.get();
	}

	void exchange(const CArmedInstance * first, const CArmedInstance * second)
	{
		auto activity = std::make_shared<GarrisonDialogActivity>(handler.get(), first->getOwner(), first, second);
		activity->askQuestion();
		handler->activities->addActivity(activity);
	}

	using Snapshot = std::vector<std::tuple<ObjectInstanceID, SlotID, CreatureID, int, const CStackInstance *, ArtifactInstanceID>>;
	Snapshot snapshot() const
	{
		Snapshot result;
		for(const auto & object : map()->objects)
		{
			if(const auto * army = dynamic_cast<const CArmedInstance *>(object.get()))
			{
				for(const auto & [slot, stack] : army->Slots())
					result.emplace_back(army->id, slot, stack->getCreatureID(), stack->getCount(), stack.get(), stack->getArtID(ArtifactPosition::CREATURE_SLOT));
			}
		}
		return result;
	}

	bool request(CPackForServer & pack, PlayerColor player = red, GameConnectionID connection = GameConnectionID::FIRST_CONNECTION)
	{
		pack.player = player;
		pack.requestID = server->results.size() + 1;
		const auto before = server->results.size();
		handler->handleReceivedPack(connection, pack);
		EXPECT_EQ(server->results.size(), before + 1);
		EXPECT_EQ(server->requestIDs.back(), static_cast<int>(pack.requestID));
		return server->results.back();
	}

	void reject(CPackForServer & pack, PlayerColor player = red, GameConnectionID connection = GameConnectionID::FIRST_CONNECTION)
	{
		const auto before = snapshot();
		EXPECT_FALSE(request(pack, player, connection));
		EXPECT_EQ(snapshot(), before);
	}

	ArrangeStacks arrange(ui8 action, SlotID source = SlotID(0), SlotID destination = SlotID(0), int count = 0)
	{
		return ArrangeStacks(action, source, destination, left->id, right->id, count);
	}

	RebalanceStacks move(ObjectInstanceID source, SlotID sourceSlot, ObjectInstanceID destination, SlotID destinationSlot, int count)
	{
		RebalanceStacks result;
		result.srcArmy = source;
		result.srcSlot = sourceSlot;
		result.dstArmy = destination;
		result.dstSlot = destinationSlot;
		result.count = count;
		return result;
	}
};

class CommanderArmyManagementTest : public ArmyManagementTest
{
protected:
	void SetUp() override
	{
		overrideSettingBeforeInit(EGameSettings::MODULE_COMMANDERS, true);
		ArmyManagementTest::SetUp();
		ASSERT_NE(left->getCommander(), nullptr);
	}
};
}

TEST_F(ArmyManagementTest, OwnedHeroesRequireTheirExposedExchange)
{
	auto pack = arrange(splitStacks, SlotID(0), SlotID(0), 24);
	reject(pack);
	exchange(left, right);
	EXPECT_TRUE(request(pack));
	EXPECT_EQ(left->getStackCount(SlotID(0)), 6);
	EXPECT_EQ(right->getStackCount(SlotID(0)), 24);
	handler->activities->topActivity(red)->finish();
	reject(pack);
}

TEST_F(ArmyManagementTest, BuriedQuestionRejectsAnotherPlayersAnswer)
{
	std::optional<int32_t> appliedAnswer = 42;
	auto question = std::make_shared<RecordingAnswerActivity>(handler.get(), blue,
		[&](std::optional<int32_t> answer) { appliedAnswer = answer; });
	auto redCover = std::make_shared<TimerPauseActivity>(handler.get(), red);
	auto blueCover = std::make_shared<TimerPauseActivity>(handler.get(), blue);
	if(question->getActiveQuestionID() == QuestionID::NONE)
		question->askQuestion();
	handler->activities->addActivity(question);
	handler->activities->addActivity(redCover);
	handler->activities->addActivity(blueCover);

	ASSERT_EQ(handler->activities->getActivity(question->getActiveQuestionID(), blue), question);
	EXPECT_EQ(handler->activities->getActivity(question->getActiveQuestionID(), red), nullptr);
	QuestionAnswer reply(question->getActiveQuestionID(), 7);
	EXPECT_FALSE(request(reply, red));
	EXPECT_EQ(handler->activities->topActivity(red), redCover);
	EXPECT_EQ(handler->activities->topActivity(blue), blueCover);

	EXPECT_EQ(appliedAnswer, 42);
	blueCover->finish();
	question->finish();
	redCover->finish();
	EXPECT_EQ(appliedAnswer, std::nullopt);
}

TEST_F(ArmyManagementTest, BuriedQuestionStillStoresItsOwnPlayersAnswer)
{
	std::optional<int32_t> appliedAnswer;
	auto question = std::make_shared<RecordingAnswerActivity>(handler.get(), red,
		[&](std::optional<int32_t> answer) { appliedAnswer = answer; });
	auto cover = std::make_shared<TimerPauseActivity>(handler.get(), red);
	if(question->getActiveQuestionID() == QuestionID::NONE)
		question->askQuestion();
	handler->activities->addActivity(question);
	handler->activities->addActivity(cover);

	// Receipt is acknowledged while the answer waits for its own activity to be exposed.
	QuestionAnswer reply(question->getActiveQuestionID(), 7);
	EXPECT_TRUE(request(reply));
	EXPECT_FALSE(appliedAnswer.has_value());
	cover->finish();
	EXPECT_EQ(appliedAnswer, 7);
	EXPECT_EQ(handler->activities->topActivity(red), nullptr);
}

TEST_F(ArmyManagementTest, SelectionQuestionRejectsMissingAndOutOfRangeAnswers)
{
	BlockingDialog dialog(true, true);
	dialog.player = red;
	dialog.components.resize(2);
	auto question = std::make_shared<BlockingDialogActivity>(handler.get(), dialog);
	if(question->getActiveQuestionID() == QuestionID::NONE)
		question->askQuestion();
	handler->activities->addActivity(question);

	for(const auto answer : {std::optional<int32_t>{}, std::optional<int32_t>{-1}, std::optional<int32_t>{3}})
	{
		QuestionAnswer reply(question->getActiveQuestionID(), answer);
		EXPECT_FALSE(request(reply));
		EXPECT_EQ(handler->activities->topActivity(red), question);
	}

	QuestionAnswer valid(question->getActiveQuestionID(), 2);
	EXPECT_TRUE(request(valid));
	EXPECT_EQ(handler->activities->topActivity(red), nullptr);
}

TEST_F(ArmyManagementTest, LevelUpQuestionRejectsUnofferedAndOutOfRangeAnswers)
{
	HeroLevelUp levelUp;
	levelUp.heroId = left->id;
	levelUp.player = red;
	levelUp.skills = {SecondarySkill::PATHFINDING};
	auto question = std::make_shared<HeroLevelUpPrompt>(handler.get(), left, levelUp);
	QuestionAnswer early(question->getActiveQuestionID(), 0);
	EXPECT_FALSE(request(early));
	handler->activities->addActivity(question);
	ASSERT_NE(question->getActiveQuestionID(), QuestionID::NONE);

	for(const auto answer : {std::optional<int32_t>{}, std::optional<int32_t>{-1}, std::optional<int32_t>{1}})
	{
		QuestionAnswer reply(question->getActiveQuestionID(), answer);
		EXPECT_FALSE(request(reply));
		EXPECT_EQ(handler->activities->topActivity(red), question);
	}
	EXPECT_TRUE(question->acceptsAnswer(0));
}

TEST_F(CommanderArmyManagementTest, CommanderLevelUpQuestionRejectsOutOfRangeAnswers)
{
	CommanderLevelUp levelUp;
	levelUp.heroId = left->id;
	levelUp.player = red;
	levelUp.skills = {0};
	auto question = std::make_shared<CommanderLevelUpPrompt>(handler.get(), left, levelUp);
	handler->activities->addActivity(question);

	for(const auto answer : {std::optional<int32_t>{}, std::optional<int32_t>{-1}, std::optional<int32_t>{1}})
	{
		QuestionAnswer reply(question->getActiveQuestionID(), answer);
		EXPECT_FALSE(request(reply));
		EXPECT_EQ(handler->activities->topActivity(red), question);
	}
	EXPECT_TRUE(question->acceptsAnswer(0));
}

TEST_F(ArmyManagementTest, ExchangePartnerCannotDismissInitiatorsQuestion)
{
	auto partner = findHeroAt({20, 20, 0});
	ASSERT_NE(partner, nullptr);
	auto question = std::make_shared<GarrisonDialogActivity>(handler.get(), red, left, partner);
	question->addPartner(blue);
	if(question->getActiveQuestionID() == QuestionID::NONE)
		question->askQuestion();
	handler->activities->addActivity(question);

	EXPECT_FALSE(handler->answerQuestion(question->getActiveQuestionID(), 0, blue));
	EXPECT_EQ(handler->activities->topActivity(red), question);
	EXPECT_EQ(handler->activities->topActivity(blue), question);
	EXPECT_TRUE(handler->answerQuestion(question->getActiveQuestionID(), 0, red));
	EXPECT_EQ(handler->activities->topActivity(red), nullptr);
	EXPECT_EQ(handler->activities->topActivity(blue), nullptr);
}

TEST_F(ArmyManagementTest, ReverseSplitUsesActualSourceAndExactCount)
{
	exchange(left, right);
	auto pack = arrange(splitStacks, SlotID(0), SlotID(0), 4);
	EXPECT_TRUE(request(pack));
	EXPECT_EQ(left->getStackCount(SlotID(0)), 26);
	EXPECT_EQ(right->getStackCount(SlotID(0)), 4);
	pack.val = 3;
	EXPECT_TRUE(request(pack));
	EXPECT_EQ(left->getStackCount(SlotID(0)), 27);
	EXPECT_EQ(right->getStackCount(SlotID(0)), 3);
}

TEST_F(ArmyManagementTest, SplitMergeSwapAndBulkConserveUnits)
{
	exchange(left, right);
	auto split = arrange(splitStacks, SlotID(0), SlotID(2), 4);
	EXPECT_TRUE(request(split));
	auto merge = arrange(mergeStacks, SlotID(0), SlotID(0));
	EXPECT_TRUE(request(merge));
	auto swap = arrange(swapStacks, SlotID(1), SlotID(0));
	EXPECT_TRUE(request(swap));
	EXPECT_EQ(left->getCreature(SlotID(1)), pikeman.toCreature());
	EXPECT_EQ(right->getCreature(SlotID(0)), halberdier.toCreature());
	BulkMoveArmy all(left->id, right->id, SlotID(1));
	EXPECT_TRUE(request(all));
	EXPECT_EQ(left->stacksCount(), 1);
	EXPECT_EQ(left->getStackCount(SlotID(1)), 1);
	EXPECT_EQ(right->getStackCount(SlotID(2)), 29);
	EXPECT_EQ(right->getStackCount(SlotID(0)), 5);
	EXPECT_EQ(left->tempOwner, red);
	EXPECT_EQ(right->tempOwner, red);
}

TEST_F(ArmyManagementTest, TownAndVisitingHeroNeedNoExchangeDialog)
{
	auto * town = addObject<CGTownInstance>(Obj::TOWN, red);
	town->setVisitingHero(left);
	ASSERT_TRUE(town->setCreature(SlotID(0), pikeman, 5));
	ArrangeStacks pack(mergeStacks, SlotID(0), SlotID(0), town->id, left->id, 0);
	EXPECT_TRUE(request(pack));
	EXPECT_EQ(town->stacksCount(), 0);
	EXPECT_EQ(left->getStackCount(SlotID(0)), 15);
}

TEST_F(ArmyManagementTest, SameArmyActionsAndSmartRebalanceRemainValid)
{
	BulkSplitStack split(left->id, SlotID(0), 1);
	EXPECT_TRUE(request(split));
	EXPECT_EQ(left->stacksCount(), 7);
	BulkMergeStacks merge(left->id, SlotID(0));
	EXPECT_TRUE(request(merge));
	EXPECT_EQ(left->getStackCount(SlotID(0)), 10);
	left->setStackCount(SlotID(0), 2);
	BulkSplitAndRebalanceStack rebalance(left->id, SlotID(0));
	EXPECT_TRUE(request(rebalance));
	EXPECT_EQ(left->getStackCount(SlotID(0)), 1);
	EXPECT_EQ(left->getStackCount(SlotID(2)), 1);
	EXPECT_EQ(left->stacksCount(), 3);
	left->setStackCount(SlotID(0), 10);
	ArrangeStacks internal(splitStacks, SlotID(0), SlotID(2), left->id, left->id, 4);
	EXPECT_TRUE(request(internal));
	EXPECT_EQ(left->getStackCount(SlotID(0)), 7);
	EXPECT_EQ(left->getStackCount(SlotID(2)), 4);
}

TEST_F(ArmyManagementTest, OtherPlayerCannotRearrangeOrRemoveTroops)
{
	right->tempOwner = blue;
	exchange(left, right);
	BulkSplitStack split(right->id, SlotID(0), 1);
	BulkMergeStacks merge(right->id, SlotID(0));
	BulkSplitAndRebalanceStack rebalance(right->id, SlotID(0));
	BulkMoveArmy all(right->id, left->id, SlotID(0));
	for(auto * pack : std::initializer_list<CPackForServer *>{&split, &merge, &rebalance, &all})
		reject(*pack);
	auto swap = arrange(swapStacks, SlotID(2), SlotID(0));
	reject(swap);
	auto reverse = arrange(splitStacks, SlotID(0), SlotID(0), 19);
	reject(reverse);
}

TEST_F(ArmyManagementTest, ConnectionAndActiveTurnAreChecked)
{
	BulkSplitStack split(left->id, SlotID(0), 1);
	reject(split, blue);
	reject(split, red, static_cast<GameConnectionID>(17));
	gameState()->actingPlayers.clear();
	reject(split);
	// An exposed dialog also lets its non-acting participant arrange their own army.
	exchange(left, right);
	EXPECT_TRUE(request(split));
}

TEST_F(ArmyManagementTest, AnotherPlayersQueryAndCoveredQueriesDoNotAuthorize)
{
	right->tempOwner = blue;
	auto * neutral = addObject<CGPandoraBox>(Obj::PANDORAS_BOX, PlayerColor::UNFLAGGABLE);
	ASSERT_TRUE(neutral->setCreature(SlotID(0), pikeman, 8));
	exchange(neutral, right);
	BulkMoveArmy steal(neutral->id, left->id, SlotID(0));
	reject(steal);
	exchange(left, right);
	BulkMoveArmy notInDialog(neutral->id, right->id, SlotID(0));
	reject(notInDialog);
	BlockingDialog dialog;
	dialog.player = red;
	auto blocking = std::make_shared<BlockingDialogActivity>(handler.get(), dialog);
	handler->activities->addActivity(blocking);
	auto owned = arrange(mergeStacks);
	reject(owned);
}

TEST_F(ArmyManagementTest, NeutralRewardCanBeTakenOnlyInPlayersCurrentDialog)
{
	auto * reward = addObject<CGPandoraBox>(Obj::PANDORAS_BOX, PlayerColor::UNFLAGGABLE);
	ASSERT_TRUE(reward->setCreature(SlotID(0), pikeman, 8));
	ASSERT_TRUE(reward->setCreature(SlotID(1), halberdier, 3));
	BulkSplitStack split(reward->id, SlotID(0), 1);
	reject(split);
	ArrangeStacks outside(splitStacks, SlotID(0), SlotID(2), reward->id, reward->id, 1);
	reject(outside);
	exchange(reward, left);
	EXPECT_TRUE(request(split));
	BulkMoveArmy all(reward->id, left->id, SlotID(0));
	EXPECT_TRUE(request(all));
	EXPECT_EQ(reward->stacksCount(), 0); // rewards do not require retaining one unit
	EXPECT_EQ(left->getStackCount(SlotID(0)), 18);
	EXPECT_EQ(left->getStackCount(SlotID(1)), 8);
}

TEST_F(ArmyManagementTest, AlliedArmiesAcceptDepositsButNotWithdrawals)
{
	const auto team = gameState()->players.at(red).team;
	gameState()->players.at(blue).team = team;
	gameState()->teams.at(team).players.insert(blue);
	auto * garrison = addObject<CGGarrison>(Obj::GARRISON, blue);
	garrison->removableUnits = true;
	ASSERT_TRUE(garrison->setCreature(SlotID(1), pikeman, 50));
	exchange(garrison, left);
	ArrangeStacks deposit(splitStacks, SlotID(0), SlotID(1), left->id, garrison->id, 55);
	EXPECT_TRUE(request(deposit));
	EXPECT_EQ(left->getStackCount(SlotID(0)), 5);
	deposit.val = 54;
	reject(deposit);
	BulkMoveArmy steal(garrison->id, left->id, SlotID(1));
	reject(steal);
	EXPECT_EQ(garrison->tempOwner, blue);
}

TEST_F(ArmyManagementTest, LockedGarrisonUsesActualDirectionAndSlots)
{
	auto * garrison = addObject<CGGarrison>(Obj::GARRISON, red);
	garrison->removableUnits = false;
	ASSERT_TRUE(garrison->setCreature(SlotID(1), pikeman, 50));
	exchange(garrison, left);
	ArrangeStacks deposit(splitStacks, SlotID(0), SlotID(1), left->id, garrison->id, 55);
	EXPECT_TRUE(request(deposit));
	deposit.val = 54;
	reject(deposit);
	ArrangeStacks reverseDeposit(splitStacks, SlotID(1), SlotID(0), garrison->id, left->id, 3);
	EXPECT_TRUE(request(reverseDeposit));
	EXPECT_EQ(garrison->getStackCount(SlotID(1)), 57);
	reverseDeposit.val = 4;
	reject(reverseDeposit);
	ArrangeStacks swap(swapStacks, SlotID(1), SlotID(1), garrison->id, left->id, 0);
	reject(swap);
	ArrangeStacks merge(mergeStacks, SlotID(1), SlotID(0), garrison->id, left->id, 0);
	reject(merge);
	BulkMoveArmy all(garrison->id, left->id, SlotID(1));
	reject(all);
	EXPECT_FALSE(handler->disbandCreature(garrison->id, SlotID(1), red));
	// Moving within one's own locked army is still legal.
	ArrangeStacks internal(swapStacks, SlotID(1), SlotID(2), garrison->id, garrison->id, 0);
	EXPECT_TRUE(request(internal));
	EXPECT_EQ(garrison->getStackCount(SlotID(2)), 57);
}

TEST_F(ArmyManagementTest, DialogLockAlsoProtectsNonGarrisonArmies)
{
	auto query = std::make_shared<GarrisonDialogActivity>(handler.get(), red, right, left);
	query->removableUnits = false;
	handler->activities->addActivity(query);
	DisbandCreature disband;
	disband.id = right->id;
	disband.pos = SlotID(0);
	reject(disband);
	auto removal = arrange(splitStacks, SlotID(0), SlotID(0), 19);
	reject(removal);
	auto deposit = arrange(splitStacks, SlotID(0), SlotID(0), 21);
	EXPECT_TRUE(request(deposit));
}

TEST_F(ArmyManagementTest, MissingAndNonArmyIdentitiesAreSafeForEveryBulkRequest)
{
	auto * object = addObject<CGObjectInstance>(Obj::SHIPYARD, red);
	for(auto id : {ObjectInstanceID(-1), ObjectInstanceID(100000), object->id})
	{
		BulkSplitStack split(id, SlotID(0), 1);
		BulkMergeStacks merge(id, SlotID(0));
		BulkSplitAndRebalanceStack rebalance(id, SlotID(0));
		BulkMoveArmy moveSource(id, left->id, SlotID(0));
		BulkMoveArmy moveDestination(left->id, id, SlotID(0));
		ArrangeStacks arrange(swapStacks, SlotID(0), SlotID(0), id, left->id, 0);
		for(auto * pack : std::initializer_list<CPackForServer *>{&split, &merge, &rebalance, &moveSource, &moveDestination, &arrange})
			reject(*pack);
		EXPECT_FALSE(handler->isAllowedExchange(red, id, id));
	}
}

TEST_F(ArmyManagementTest, MissingArrangeTargetsRejectAndAcknowledgeWithoutThrowing)
{
	exchange(left, right);
	const auto removedID = right->id;
	RemoveObject removal(removedID, PlayerColor::NEUTRAL);
	gameState()->apply(removal);
	for(const auto id : {ObjectInstanceID::NONE, removedID, ObjectInstanceID(100000)})
	{
		ArrangeStacks sourceMissing(mergeStacks, SlotID(0), SlotID(0), id, left->id, 0);
		reject(sourceMissing);
		ArrangeStacks destinationMissing(mergeStacks, SlotID(0), SlotID(0), left->id, id, 0);
		reject(destinationMissing);
		EXPECT_FALSE(handler->isAllowedExchange(red, id, left->id));
	}
}

TEST_F(ArmyManagementTest, MissingAndNonShipyardBoatRequestsRejectWithoutSpendingResources)
{
	const auto resources = gameState()->getPlayerState(red)->resources;
	for(const auto id : {ObjectInstanceID::NONE, ObjectInstanceID(100000), left->id})
	{
		EXPECT_FALSE(handler->buildBoat(id, red));
		BuildBoat request;
		request.objid = id;
		reject(request);
		EXPECT_EQ(gameState()->getPlayerState(red)->resources, resources);
	}
}

TEST_F(ArmyManagementTest, InvalidActionsSlotsAndCountsDoNotMutate)
{
	exchange(left, right);
	for(auto action : {swapStacks, mergeStacks, splitStacks, ui8(0), ui8(255)})
	{
		ArrangeStacks self(action, SlotID(0), SlotID(0), left->id, left->id, 1);
		reject(self);
	}
	for(auto slot : {SlotID(-1), SlotID(7), SlotID(100), SlotID(6)})
	{
		BulkSplitStack split(left->id, slot, 1);
		BulkMergeStacks merge(left->id, slot);
		BulkSplitAndRebalanceStack rebalance(left->id, slot);
		BulkMoveArmy all(left->id, right->id, slot);
		for(auto * pack : std::initializer_list<CPackForServer *>{&split, &merge, &rebalance, &all})
			reject(*pack);
	}
	for(int count : {0, -1, std::numeric_limits<int>::min(), 100, std::numeric_limits<int>::max()})
	{
		BulkSplitStack split(left->id, SlotID(0), count);
		reject(split);
		auto exact = arrange(splitStacks, SlotID(0), SlotID(2), count);
		reject(exact);
	}
	auto incompatible = arrange(mergeStacks, SlotID(1), SlotID(0));
	reject(incompatible);
	auto noChange = arrange(splitStacks, SlotID(0), SlotID(0), 20);
	reject(noChange);
	BulkMoveArmy self(left->id, left->id, SlotID(0));
	reject(self);
}

TEST_F(ArmyManagementTest, LastStackRemovalFailsAndBulkRetainsExactlyOne)
{
	left->eraseStack(SlotID(1));
	exchange(left, right);
	auto merge = arrange(mergeStacks);
	reject(merge);
	auto emptySwap = arrange(swapStacks, SlotID(0), SlotID(1));
	reject(emptySwap);
	BulkMoveArmy all(left->id, right->id, SlotID(0));
	EXPECT_TRUE(request(all));
	EXPECT_EQ(left->getStackCount(SlotID(0)), 1);
	EXPECT_EQ(right->getStackCount(SlotID(0)), 29);
	reject(all); // empty bulk plan is a rejection, with an acknowledgement
}

TEST_F(ArmyManagementTest, OverflowInMergeAndEveryBulkPlanIsAtomic)
{
	exchange(left, right);
	right->setStackCount(SlotID(0), std::numeric_limits<int>::max());
	auto merge = arrange(mergeStacks);
	reject(merge);
	BulkMoveArmy all(left->id, right->id, SlotID(0));
	reject(all);
	ASSERT_TRUE(left->setCreature(SlotID(2), pikeman, std::numeric_limits<int>::max()));
	BulkMergeStacks gather(left->id, SlotID(0));
	reject(gather);
	BulkSplitAndRebalanceStack rebalance(left->id, SlotID(0));
	reject(rebalance);
}

TEST_F(ArmyManagementTest, InvalidPacketAndLateBulkDestinationLeaveBothArmiesUntouched)
{
	const auto initial = snapshot();
	BulkRebalanceStacks empty;
	EXPECT_THROW(gameState()->apply(empty), std::runtime_error);
	SwapStacks self;
	self.srcArmy = left->id;
	self.dstArmy = left->id;
	self.srcSlot = SlotID(0);
	self.dstSlot = SlotID(0);
	EXPECT_THROW(gameState()->apply(self), std::runtime_error);
	EXPECT_EQ(snapshot(), initial);
	for(int count : {0, -4, 11, std::numeric_limits<int>::max()})
	{
		auto pack = move(left->id, SlotID(0), right->id, SlotID(0), count);
		const auto before = snapshot();
		EXPECT_THROW(gameState()->apply(pack), std::runtime_error);
		EXPECT_EQ(snapshot(), before);
	}
	for(auto invalid : {
		move(left->id, SlotID(0), left->id, SlotID(0), 1),
		move(left->id, SlotID(0), left->id, SlotID(1), 1),
		move(left->id, SlotID(6), right->id, SlotID(2), 1),
		move(left->id, SlotID(0), right->id, SlotID(7), 1),
		move(left->id, SlotID(0), ObjectInstanceID(100000), SlotID(2), 1)})
	{
		BulkRebalanceStacks bulk;
		bulk.moves.push_back(move(left->id, SlotID(0), right->id, SlotID(2), 2));
		bulk.moves.push_back(invalid);
		const auto before = snapshot();
		EXPECT_THROW(gameState()->apply(bulk), std::runtime_error);
		EXPECT_EQ(snapshot(), before);
	}
}

TEST_F(ArmyManagementTest, CompleteRedistributionAcknowledgesAndRejectsReplayOrLateInvalidDestination)
{
	exchange(left, right);
	RedistributeArmyStack pack;
	pack.leftHero = left->id;
	pack.rightHero = right->id;
	pack.sourceArmy = left->id;
	pack.sourceSlot = SlotID(0);
	pack.expectedCreature = pikeman;
	pack.expectedSourceCount = 10;
	pack.destinationCount = 2;
	pack.destinations[0] = {left->id, SlotID(2), 2};
	pack.destinations[1] = {right->id, SlotID(7), 3};
	reject(pack);
	pack.destinations[1].slot = SlotID(0);
	EXPECT_TRUE(request(pack));
	EXPECT_EQ(left->getStackCount(SlotID(0)), 5);
	EXPECT_EQ(left->getStackCount(SlotID(2)), 2);
	EXPECT_EQ(right->getStackCount(SlotID(0)), 23);
	reject(pack);
	pack.expectedSourceCount = 5;
	pack.destinations[1].amount = 2;
	pack.destinations[0].slot = SlotID(1); // incompatible last destination
	reject(pack);
}

TEST_F(ArmyManagementTest, StackArtifactStaysOnSplitAndFollowsWholeStackAndSwap)
{
	exchange(left, right);
	// Use a non-combination artifact as opaque stack equipment; transfer code
	// must preserve its instance identity independently of mod equipment rules.
	auto * artifact = map()->createArtifact(ArtifactID(7));
	map()->putArtifactInstance(*left->getStackPtr(SlotID(0)), artifact->getId(), ArtifactPosition::CREATURE_SLOT);
	auto split = arrange(splitStacks, SlotID(0), SlotID(2), 2);
	EXPECT_TRUE(request(split));
	EXPECT_EQ(left->getStackPtr(SlotID(0))->getArt(ArtifactPosition::CREATURE_SLOT), artifact);
	EXPECT_EQ(right->getStackPtr(SlotID(2))->getArt(ArtifactPosition::CREATURE_SLOT), nullptr);
	auto merge = arrange(mergeStacks);
	EXPECT_TRUE(request(merge));
	EXPECT_EQ(right->getStackPtr(SlotID(0))->getArt(ArtifactPosition::CREATURE_SLOT), artifact);
	auto swap = arrange(swapStacks, SlotID(1), SlotID(0));
	EXPECT_TRUE(request(swap));
	EXPECT_EQ(left->getStackPtr(SlotID(1))->getArt(ArtifactPosition::CREATURE_SLOT), artifact);
}

TEST_F(ArmyManagementTest, RedistributionRejectsEveryMalformedPlanWithoutPartialChanges)
{
	exchange(left, right);
	RedistributeArmyStack valid;
	valid.leftHero = left->id;
	valid.rightHero = right->id;
	valid.sourceArmy = left->id;
	valid.sourceSlot = SlotID(0);
	valid.expectedCreature = pikeman;
	valid.expectedSourceCount = 10;
	valid.destinationCount = 2;
	valid.destinations[0] = {left->id, SlotID(2), 2};
	valid.destinations[1] = {right->id, SlotID(0), 3};
	for(int count : {0, -1, 10, std::numeric_limits<int>::max()})
	{
		auto bad = valid;
		bad.destinations[1].amount = count;
		reject(bad);
	}
	for(int count : {0, 14, 255})
	{
		auto bad = valid;
		bad.destinationCount = count;
		reject(bad);
	}
	auto bad = valid;
	bad.destinations[1] = bad.destinations[0];
	reject(bad);
	bad = valid;
	bad.sourceSlot = SlotID(6);
	reject(bad);
	bad = valid;
	bad.expectedCreature = halberdier;
	reject(bad);
	bad = valid;
	bad.destinations[1].armyId = ObjectInstanceID(100000);
	reject(bad);
	bad = valid;
	bad.destinations[1] = {left->id, SlotID(0), 1};
	reject(bad);
	right->setStackCount(SlotID(0), std::numeric_limits<int>::max());
	reject(valid);
}

TEST_F(ArmyManagementTest, ArtifactConflictBetweenNonHeroArmiesUsesDestinationStack)
{
	auto * reward = addObject<CGPandoraBox>(Obj::PANDORAS_BOX, PlayerColor::UNFLAGGABLE);
	auto * garrison = addObject<CGGarrison>(Obj::GARRISON, red);
	garrison->removableUnits = true;
	ASSERT_TRUE(reward->setCreature(SlotID(0), pikeman, 3));
	ASSERT_TRUE(garrison->setCreature(SlotID(0), pikeman, 4));
	auto * sourceArt = map()->createArtifact(ArtifactID(7));
	auto * destinationArt = map()->createArtifact(ArtifactID(8));
	map()->putArtifactInstance(*reward->getStackPtr(SlotID(0)), sourceArt->getId(), ArtifactPosition::CREATURE_SLOT);
	map()->putArtifactInstance(*garrison->getStackPtr(SlotID(0)), destinationArt->getId(), ArtifactPosition::CREATURE_SLOT);
	exchange(reward, garrison);
	ArrangeStacks merge(mergeStacks, SlotID(0), SlotID(0), reward->id, garrison->id, 0);
	EXPECT_TRUE(request(merge));
	EXPECT_EQ(reward->stacksCount(), 0);
	EXPECT_EQ(garrison->getStackCount(SlotID(0)), 7);
	EXPECT_EQ(garrison->getStackPtr(SlotID(0))->getArt(ArtifactPosition::CREATURE_SLOT), sourceArt);
}

TEST_F(ArmyManagementTest, CoveredAndCompletedDuplicateRepliesAreAcknowledgedWithoutReapplying)
{
	int applications = 0;
	std::optional<int32_t> applied;
	auto question = std::make_shared<RecordingAnswerActivity>(handler.get(), red,
		[&](std::optional<int32_t> answer) { ++applications; applied = answer; });
	auto cover = std::make_shared<TimerPauseActivity>(handler.get(), red);
	handler->activities->addActivity(question);
	handler->activities->addActivity(cover);
	QuestionAnswer first(question->getActiveQuestionID(), 7);
	QuestionAnswer duplicate(question->getActiveQuestionID(), 8);
	EXPECT_TRUE(request(first));
	EXPECT_TRUE(request(duplicate));
	EXPECT_EQ(applications, 0);
	cover->finish();
	EXPECT_EQ(applications, 1);
	EXPECT_EQ(applied, 7);
	EXPECT_TRUE(request(duplicate));
	EXPECT_EQ(applications, 1);
	EXPECT_EQ(applied, 7);
}

TEST_F(ArmyManagementTest, TeleportQuestionRejectsUnofferedExitsAndKeepsRandomExit)
{
	TeleportDialog dialog;
	dialog.hero = left->id;
	dialog.exits.emplace_back(right->id, right->visitablePos());
	auto question = std::make_shared<TeleportDialogActivity>(handler.get(), dialog);
	question->askQuestion();
	handler->activities->addActivity(question);
	for(const auto answer : {std::optional<int32_t>{}, std::optional<int32_t>{-2}, std::optional<int32_t>{1}})
	{
		QuestionAnswer reply(question->getActiveQuestionID(), answer);
		EXPECT_FALSE(request(reply));
		EXPECT_EQ(handler->activities->topActivity(red), question);
	}
	EXPECT_TRUE(question->acceptsAnswer(0));
	QuestionAnswer randomExit(question->getActiveQuestionID(), -1);
	EXPECT_TRUE(request(randomExit));
	EXPECT_EQ(handler->activities->topActivity(red), nullptr);
}

TEST_F(ArmyManagementTest, PauseAndExchangeQuestionsRejectUnofferedDismissalValues)
{
	auto pause = std::make_shared<TimerPauseActivity>(handler.get(), red);
	pause->askQuestion();
	handler->activities->addActivity(pause);
	QuestionAnswer invalidPause(pause->getActiveQuestionID(), 1);
	EXPECT_FALSE(request(invalidPause));
	EXPECT_EQ(handler->activities->topActivity(red), pause);
	QuestionAnswer resume(pause->getActiveQuestionID(), 0);
	EXPECT_TRUE(request(resume));

	auto exchange = std::make_shared<GarrisonDialogActivity>(handler.get(), red, left, right);
	exchange->askQuestion();
	handler->activities->addActivity(exchange);
	QuestionAnswer invalidDismissal(exchange->getActiveQuestionID(), 1);
	EXPECT_FALSE(request(invalidDismissal));
	EXPECT_EQ(handler->activities->topActivity(red), exchange);
	QuestionAnswer dismiss(exchange->getActiveQuestionID(), 0);
	EXPECT_TRUE(request(dismiss));
}

TEST_F(ArmyManagementTest, ScriptQuestionsValidateOfferedChoicesAndKeepNullCancellation)
{
	BlockingDialog dialog(true, true);
	dialog.player = red;
	dialog.components.resize(2);
	auto question = std::make_shared<ScriptDialogActivity>(handler.get(), dialog);
	question->askQuestion();
	handler->activities->addActivity(question);
	for(const int answer : {-1, 3})
	{
		QuestionAnswer reply(question->getActiveQuestionID(), answer);
		EXPECT_FALSE(request(reply));
		EXPECT_EQ(handler->activities->topActivity(red), question);
	}
	QuestionAnswer cancel(question->getActiveQuestionID(), std::nullopt);
	EXPECT_TRUE(request(cancel));
	EXPECT_EQ(handler->activities->topActivity(red), nullptr);
}
