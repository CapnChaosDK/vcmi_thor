/*
 * InfoCallbackTest.cpp, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 *
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 */
#include "StdInc.h"

#include "mock/TinyMapGameTest.h"
#include "lib/callback/CCallback.h"
#include "lib/callback/EditorCallback.h"
#include "lib/mapObjects/CGHeroInstance.h"
#include "lib/mapObjects/CGTownInstance.h"
#include "lib/mapObjects/MiscObjects.h"
#include "lib/networkPacks/PacksForClient.h"

#include <limits>

namespace
{
const PlayerColor red(0);
const PlayerColor blue(1);

class InfoCallbackTest : public TinyMapGameTest
{
protected:
	void SetUp() override
	{
		TinyMapGameTest::SetUp();
		TinyH3M::TinyH3MBuilder builder(EMapFormat::SOD);
		builder.size(36, false).playerActive(red).playerActive(blue)
			.hero({5, 5, 0}, HeroTypeID(0), red).heroGarrison({{CreatureID(0), 10}})
			.hero({25, 25, 0}, HeroTypeID(1), blue).heroGarrison({{CreatureID(0), 10}});
		startWithMap(std::move(builder));
	}
};
}

TEST_F(InfoCallbackTest, MissingIdentitiesAreOptionalOrThrowAccordingToContract)
{
	const auto callback = makeCallback(red);
	for(const auto id : {ObjectInstanceID::NONE, ObjectInstanceID(100000), ObjectInstanceID(std::numeric_limits<int>::max())})
	{
		EXPECT_EQ(callback->tryGetObj(id), nullptr);
		EXPECT_THROW(callback->getObj(id), std::runtime_error);
		EXPECT_EQ(callback->getHero(id), nullptr);
		EXPECT_EQ(callback->getTown(id), nullptr);
	}
}

TEST_F(InfoCallbackTest, VisibilityWithdrawalAndRestorationNeverExposeHiddenObjects)
{
	const auto callback = makeCallback(red);
	const auto * enemy = findHeroByOwner(blue);
	ASSERT_NE(enemy, nullptr);
	const auto id = enemy->id;
	const auto tile = enemy->visitablePos();
	revealMap(red);
	EXPECT_EQ(callback->getObj(id), enemy);
	EXPECT_EQ(callback->getHero(id), enemy);
	EXPECT_NE(callback->getTile(tile), nullptr);
	setMapVisibility(red, false);
	EXPECT_EQ(callback->tryGetObj(id), nullptr);
	EXPECT_EQ(callback->getHero(id), nullptr);
	EXPECT_EQ(callback->tryGetTile(tile), nullptr);
	EXPECT_EQ(callback->getTopObj(tile), nullptr);
	EXPECT_THROW(callback->getObj(id), std::runtime_error);
	EXPECT_THROW(callback->getTile(tile), std::runtime_error);
	EXPECT_THROW(callback->getVisitableObjs(tile), std::runtime_error);
	revealMap(red);
	EXPECT_EQ(callback->tryGetObj(id), enemy);
	EXPECT_NE(callback->tryGetTile(tile), nullptr);
}

TEST_F(InfoCallbackTest, OwnedObjectRemainsAccessibleInFogUntilOwnershipChanges)
{
	const auto callback = makeCallback(red);
	auto * hero = findHeroByOwner(red);
	ASSERT_NE(hero, nullptr);
	setMapVisibility(red, false);
	EXPECT_EQ(callback->tryGetObj(hero->id), hero);
	EXPECT_EQ(callback->getHero(hero->id), hero);
	EXPECT_EQ(callback->getTown(hero->id), nullptr);
	EXPECT_EQ(callback->tryGetTile(hero->visitablePos()), nullptr);
	hero->tempOwner = blue;
	EXPECT_EQ(callback->tryGetObj(hero->id), nullptr);
	EXPECT_EQ(callback->getHero(hero->id), nullptr);
	EXPECT_THROW(callback->getObj(hero->id), std::runtime_error);
}

TEST_F(InfoCallbackTest, RemovedObjectCannotBeResolvedFromItsPublishedIdentity)
{
	const auto callback = makeCallback(red);
	const auto * hero = findHeroByOwner(red);
	ASSERT_NE(hero, nullptr);
	const auto id = hero->id;
	ASSERT_EQ(callback->getObj(id), hero);
	RemoveObject removal(id, PlayerColor::NEUTRAL);
	gameState()->apply(removal);
	EXPECT_EQ(callback->tryGetObj(id), nullptr);
	EXPECT_EQ(callback->getHero(id), nullptr);
	EXPECT_THROW(callback->getObj(id), std::runtime_error);
}

TEST_F(InfoCallbackTest, PlayerStateAccessAndMissingStatusHaveExplicitContracts)
{
	const auto callback = makeCallback(red);
	EXPECT_NE(callback->getPlayerState(red), nullptr);
	EXPECT_EQ(callback->tryGetPlayerState(blue), nullptr);
	EXPECT_THROW(callback->getPlayerState(blue), std::runtime_error);
	EXPECT_NE(gameState()->tryGetPlayerState(blue), nullptr);
	for(const auto player : {PlayerColor::NEUTRAL, PlayerColor(7)})
	{
		EXPECT_EQ(callback->tryGetPlayerState(player), nullptr);
		EXPECT_THROW(callback->getPlayerState(player), std::runtime_error);
		EXPECT_EQ(callback->getPlayerStatus(player), EPlayerStatus::WRONG);
		EXPECT_EQ(gameState()->tryGetPlayerState(player), nullptr);
		EXPECT_THROW(gameState()->getPlayerState(player), std::runtime_error);
	}
}

TEST_F(InfoCallbackTest, TeleportExitProbeRejectsMissingAndHiddenIdentities)
{
	const auto callback = makeCallback(red);
	const auto * hero = findHeroByOwner(red);
	const auto * enemy = findHeroByOwner(blue);
	ASSERT_NE(hero, nullptr);
	ASSERT_NE(enemy, nullptr);
	setMapVisibility(red, false);
	EXPECT_TRUE(CGTeleport::getPassableExits(*callback, hero, {ObjectInstanceID::NONE, ObjectInstanceID(100000), enemy->id}).empty());
}

TEST_F(InfoCallbackTest, EditorAndGameplayTileLookupsRejectEveryMapBoundary)
{
	const auto callback = makeCallback(red);
	EditorCallback editor(map());
	revealMap(red);
	const int3 valid(5, 5, 0);
	EXPECT_EQ(editor.getTile(valid), callback->getTile(valid));
	EXPECT_EQ(editor.tryGetTile(valid), callback->tryGetTile(valid));
	for(const auto tile : {int3(-1, 5, 0), int3(36, 5, 0), int3(5, 36, 0), int3(5, 5, 1)})
	{
		EXPECT_EQ(callback->tryGetTile(tile), nullptr);
		EXPECT_EQ(callback->getTopObj(tile), nullptr);
		EXPECT_THROW(callback->getTile(tile), std::runtime_error);
		EXPECT_EQ(editor.tryGetTile(tile), nullptr);
		EXPECT_THROW(editor.getTile(tile), std::runtime_error);
	}
}
