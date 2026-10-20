/*
 * BattleControllerInputTest.cpp, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 */
#include "../../Global.h"
#include <gtest/gtest.h>
#include "../../client/battle/BattleControllerInput.h"
#include "../../client/gui/ControllerInputState.h"
#include "../../client/gui/ControllerPressState.h"
#include "../../client/gui/CIntObject.h"

void PrintTo(const BattleHex & hex, std::ostream * stream)
{
	*stream << hex.toInt();
}

namespace
{
class InputWindow : public IShowActivatable
{
public:
	void activate() override {}
	void deactivate() override {}
	void redraw() override {}
	void show(Canvas &) override {}
	void showAll(Canvas &) override {}
	bool isPopupWindow() const override { return false; }
	void onScreenResize() override {}
};

BattleControllerInput::Selection selection(PossiblePlayerBattleAction::Actions action = PossiblePlayerBattleAction::MOVE_STACK)
{
	return {17, BattleHex(8, 5), std::nullopt, BattleHex::NONE, action, false, false};
}

struct Unit
{
	uint32_t id;
	Point position;
	bool eligible = true;
	uint32_t unitId() const { return id; }
};

/// Drives the production input gates; the native action owner supplies legality
/// and submission, represented here by a deterministic oracle, not copied combat rules.
struct BattleInputJourney
{
	std::shared_ptr<IShowActivatable> window = std::make_shared<InputWindow>();
	ControllerPressState presses;
	BattleControllerInput::Confirmation confirm;
	std::optional<BattleControllerInput::Selection> preview = selection();
	std::vector<PossiblePlayerBattleAction> submitted;
	void press()
	{
		if(presses.press({3, "a"}, {EShortcut::GLOBAL_ACCEPT}, window))
			confirm.press(preview);
	}
	void release()
	{
		if(presses.release({3, "a"}, window) && confirm.release(preview))
			submitted.push_back(preview->action);
	}
	void cancel()
	{
		presses.cancel(window, [&](const auto &) { confirm.cancel(); });
		confirm.cancel();
	}
};
}

TEST(BattleControllerInput, AdjacentHexesFollowNativeStaggeredGrid)
{
	for(int y : {4, 5})
	{
		const BattleHex origin(8, y);
		const std::array<std::array<double, 2>, 6> axes = {{{-0.5, -1}, {0.5, -1}, {1, 0}, {0.5, 1}, {-0.5, 1}, {-1, 0}}};
		for(size_t i = 0; i < axes.size(); ++i)
			EXPECT_EQ(BattleControllerInput::adjacentHex(origin, axes[i]), origin.cloneInDirection(static_cast<BattleHex::EDir>(i)));
	}
}

TEST(BattleControllerInput, EveryEdgeIsBoundedWithoutWrappingOrSliding)
{
	for(int y = 0; y < GameConstants::BFIELD_HEIGHT; ++y)
	{
		EXPECT_EQ(BattleControllerInput::adjacentHex(BattleHex(0, y), {-1, 0}), BattleHex(0, y));
		EXPECT_EQ(BattleControllerInput::adjacentHex(BattleHex(1, y), {-1, 0}), BattleHex(1, y));
		EXPECT_EQ(BattleControllerInput::adjacentHex(BattleHex(15, y), {1, 0}), BattleHex(15, y));
		EXPECT_EQ(BattleControllerInput::adjacentHex(BattleHex(16, y), {1, 0}), BattleHex(16, y));
	}
	for(int x = 0; x < GameConstants::BFIELD_WIDTH; ++x)
	{
		EXPECT_EQ(BattleControllerInput::adjacentHex(BattleHex(x, 0), {-0.5, -1}), BattleHex(x, 0));
		EXPECT_EQ(BattleControllerInput::adjacentHex(BattleHex(x, 10), {0.5, 1}), BattleHex(x, 10));
	}
	EXPECT_EQ(BattleControllerInput::adjacentHex(BattleHex::INVALID, {1, 0}), BattleHex::INVALID);
	EXPECT_EQ(BattleControllerInput::adjacentHex(BattleHex(8, 5), {0, 0}), BattleHex(8, 5));
}

TEST(BattleControllerInput, UnitSelectionUsesDirectionEligibilityDistanceAndStableTieBreak)
{
	Unit left{1, {-20, 0}}, near{7, {10, 0}}, tie{2, {10, 0}}, dead{0, {5, 0}, false}, side{3, {1, 20}};
	std::vector<const Unit *> units{&left, &near, &tie, &dead, &side};
	const auto center = [](auto unit) { return unit->position; };
	const auto eligible = [](auto unit) { return unit->eligible; };
	EXPECT_EQ(BattleControllerInput::unitInDirection(units, Point(), {1, 0}, center, eligible), &tie);
	EXPECT_EQ(BattleControllerInput::unitInDirection(units, Point(), {-1, 0}, center, eligible), &left);
	EXPECT_EQ(BattleControllerInput::unitInDirection(units, Point(100, 0), {1, 0}, center, eligible), nullptr);
	EXPECT_EQ(BattleControllerInput::unitInDirection(units, Point(), {0, 0}, center, eligible), nullptr);
}

TEST(BattleControllerInput, AttackDirectionCyclesOnlyNativeLegalApproaches)
{
	std::array<Point, 8> points;
	points.fill(Point::makeInvalid());
	EXPECT_EQ(BattleControllerInput::nextAttackDirection(points, BattleHex::NONE, 1), -1);
	points[0] = points[2] = points[7] = Point(1, 1);
	EXPECT_EQ(BattleControllerInput::nextAttackDirection(points, BattleHex::TOP_LEFT, 1), 2);
	EXPECT_EQ(BattleControllerInput::nextAttackDirection(points, BattleHex::RIGHT, 1), 7);
	EXPECT_EQ(BattleControllerInput::nextAttackDirection(points, BattleHex::BOTTOM, 1), 0);
	EXPECT_EQ(BattleControllerInput::nextAttackDirection(points, BattleHex::TOP_LEFT, -1), 7);
}

TEST(BattleControllerInput, MoveMeleeRangedAndTacticsConfirmExactlyOnce)
{
	for(const auto action : {PossiblePlayerBattleAction::MOVE_STACK, PossiblePlayerBattleAction::WALK_AND_ATTACK,
		PossiblePlayerBattleAction::ATTACK, PossiblePlayerBattleAction::SHOOT, PossiblePlayerBattleAction::MOVE_TACTICS})
	{
		BattleInputJourney journey;
		journey.preview = selection(action);
		journey.press();
		journey.press(); // repeated button down is not a second confirmation
		EXPECT_TRUE(journey.submitted.empty());
		journey.release();
		journey.release();
		ASSERT_EQ(journey.submitted.size(), 1);
		EXPECT_EQ(journey.submitted.front().get(), action);
	}
}

TEST(BattleControllerInput, UnavailableDestinationAndRejectedOrExpiredActionCannotConfirm)
{
	BattleInputJourney journey;
	journey.preview.reset(); // native owner rejects an obstacle or unreachable destination
	journey.press();
	journey.preview = selection();
	journey.release();
	EXPECT_TRUE(journey.submitted.empty());
	journey.press();
	journey.preview.reset(); // native state changes before release
	journey.release();
	EXPECT_TRUE(journey.submitted.empty());
}

TEST(BattleControllerInput, ChangedActorHexTargetDirectionActionOrSpellCancelsConfirmation)
{
	const auto initial = selection();
	std::vector<BattleControllerInput::Selection> changes(7, initial);
	changes[0].actor++;
	changes[1].hex = BattleHex(9, 5);
	changes[2].target = 42;
	changes[3].direction = BattleHex::LEFT;
	changes[4].action = PossiblePlayerBattleAction::SHOOT;
	changes[5].heroSpell = true;
	changes[6].creatureSpell = true;
	for(const auto & changed : changes)
	{
		BattleInputJourney journey;
		journey.press();
		journey.preview = changed;
		journey.release();
		EXPECT_TRUE(journey.submitted.empty());
	}
}

TEST(BattleControllerInput, SpellIdentityIsBoundToConfirmation)
{
	BattleInputJourney journey;
	journey.preview->action = {PossiblePlayerBattleAction::AIMED_SPELL_CREATURE, SpellID(1)};
	journey.press();
	journey.preview->action = {PossiblePlayerBattleAction::AIMED_SPELL_CREATURE, SpellID(2)};
	journey.release();
	EXPECT_TRUE(journey.submitted.empty());
}

TEST(BattleControllerInput, AllLifecycleCancellationsRequireFreshPressEvenWithSameActor)
{
	for(const auto reason : {"turn", "action rejected", "modal", "spellbook", "result", "mouse/touch takeover",
		"disconnect", "display detach", "pause", "battle exit"})
	{
		SCOPED_TRACE(reason);
		BattleInputJourney journey;
		journey.press();
		journey.cancel();
		journey.press(); // held/repeated press while parent is restored must remain canceled
		journey.release();
		EXPECT_TRUE(journey.submitted.empty());
		journey.press();
		journey.release();
		EXPECT_EQ(journey.submitted.size(), 1);
	}
}

TEST(BattleControllerInput, ModalReleaseCannotSubmitInChildOrRestoredParent)
{
	BattleInputJourney journey;
	const auto parent = journey.window;
	journey.press();
	journey.cancel();
	journey.window = std::make_shared<InputWindow>();
	journey.release();
	journey.window = parent;
	journey.release();
	EXPECT_TRUE(journey.submitted.empty());
}

TEST(ControllerInput, DeadZoneIsSymmetricAndInvalidCalibrationIsBounded)
{
	EXPECT_DOUBLE_EQ(ControllerInput::axisValue(0, .2, .9), 0);
	EXPECT_DOUBLE_EQ(ControllerInput::axisValue(1000, .2, .9), 0);
	EXPECT_DOUBLE_EQ(ControllerInput::axisValue(-1000, .2, .9), 0);
	EXPECT_DOUBLE_EQ(ControllerInput::axisValue(-32768, .2, .9), -1);
	EXPECT_DOUBLE_EQ(ControllerInput::axisValue(32767, .2, .9), 1);
	EXPECT_NEAR(ControllerInput::axisValue(-16384, .2, .9), -ControllerInput::axisValue(16384, .2, .9), .0001);
	EXPECT_TRUE(std::isfinite(ControllerInput::axisValue(20000, .8, .2)));
}

TEST(ControllerInput, HeldStickNeedsNeutralAfterBoundaryIncludingRepeatedCancellation)
{
	ControllerInput::AxisLatch latch;
	EXPECT_TRUE(latch.update(.8));
	latch.cancel();
	EXPECT_FALSE(latch.update(.7));
	latch.cancel();
	EXPECT_FALSE(latch.update(-.8));
	EXPECT_TRUE(latch.update(0));
	EXPECT_TRUE(latch.update(-.8));
}

TEST(ControllerInput, RepeatDelayHasNoLongFrameBurst)
{
	uint32_t remaining = 0;
	EXPECT_TRUE(ControllerInput::repeatReady(16, remaining));
	remaining = 320;
	EXPECT_FALSE(ControllerInput::repeatReady(319, remaining));
	EXPECT_TRUE(ControllerInput::repeatReady(1, remaining));
	remaining = 110;
	EXPECT_TRUE(ControllerInput::repeatReady(5000, remaining));
	remaining = 110;
	EXPECT_FALSE(ControllerInput::repeatReady(16, remaining));
}

TEST(ControllerInput, WindowOwnershipSurvivesRemapAndDoesNotTransferRelease)
{
	ControllerPressState presses;
	const auto parent = std::make_shared<InputWindow>(), child = std::make_shared<InputWindow>();
	ASSERT_TRUE(presses.press({1, "a"}, {EShortcut::GLOBAL_ACCEPT}, parent));
	EXPECT_FALSE(presses.press({1, "a"}, {EShortcut::GLOBAL_CANCEL}, child));
	EXPECT_FALSE(presses.release({1, "a"}, child));
	EXPECT_FALSE(presses.release({1, "a"}, parent));
	ASSERT_TRUE(presses.press({1, "a"}, {EShortcut::GLOBAL_ACCEPT}, parent));
	auto released = presses.release({1, "a"}, parent);
	ASSERT_TRUE(released);
	EXPECT_EQ(released->shortcuts, (std::vector{EShortcut::GLOBAL_ACCEPT}));
}

TEST(ControllerInput, CancellationIsReentrantAndNeverSynthesizesRelease)
{
	ControllerPressState presses;
	const auto window = std::make_shared<InputWindow>();
	presses.press({1, "a"}, {EShortcut::GLOBAL_ACCEPT}, window);
	presses.press({1, "back"}, {EShortcut::BATTLE_END_WITH_AUTOCOMBAT}, window);
	int canceled = 0;
	presses.cancel(window, [&](const auto &)
	{
		++canceled;
		presses.cancel(window, [&](const auto &) { ADD_FAILURE(); });
	});
	EXPECT_EQ(canceled, 2);
	EXPECT_FALSE(presses.release({1, "a"}, window));
	EXPECT_FALSE(presses.release({1, "back"}, window));
}

TEST(ControllerInput, InformationalPopupOwnsOnlyItsOpeningRightClick)
{
	ControllerPressState presses;
	const auto parent = std::make_shared<InputWindow>(), popup = std::make_shared<InputWindow>();
	presses.press({1, "y"}, {EShortcut::MOUSE_RIGHT}, parent);
	presses.cancel(parent, [](const auto &) {});
	presses.transferPopup({1, "y"}, popup);
	const auto release = presses.release({1, "y"}, popup);
	ASSERT_TRUE(release);
	EXPECT_TRUE(release->popup);
}

TEST(ControllerInput, DisconnectForgetsOnlyRemovedControllerAndReconnectRequiresNewPress)
{
	ControllerPressState presses;
	const auto window = std::make_shared<InputWindow>();
	presses.press({1, "a"}, {EShortcut::GLOBAL_ACCEPT}, window);
	presses.press({2, "b"}, {EShortcut::GLOBAL_CANCEL}, window);
	presses.forget(1);
	EXPECT_FALSE(presses.release({1, "a"}, window));
	EXPECT_TRUE(presses.release({2, "b"}, window));
	EXPECT_TRUE(presses.press({1, "a"}, {EShortcut::GLOBAL_ACCEPT}, window));
}

TEST(ControllerInput, HeldButtonOnConnectionOrRemapIsSuppressedUntilRelease)
{
	ControllerPressState presses;
	const auto window = std::make_shared<InputWindow>();
	presses.suppress({1, "a"});
	EXPECT_FALSE(presses.press({1, "a"}, {EShortcut::GLOBAL_ACCEPT}, window));
	EXPECT_FALSE(presses.release({1, "a"}, window));
	EXPECT_TRUE(presses.press({1, "a"}, {EShortcut::GLOBAL_ACCEPT}, window));
	EXPECT_TRUE(presses.release({1, "a"}, window));
}

TEST(BattleControllerInput, OffGridActorsStartOnPlayableGrid)
{
	EXPECT_EQ(BattleControllerInput::initialHex(BattleHex(3, 4)).toInt(), BattleHex(3, 4).toInt());
	EXPECT_TRUE(BattleControllerInput::initialHex(BattleHex::CASTLE_CENTRAL_TOWER).isAvailable());
	EXPECT_TRUE(BattleControllerInput::initialHex(BattleHex::INVALID).isAvailable());
}

TEST(BattleControllerInput, NativeButtonsPreserveCommandRoutesWithoutMouseGameplay)
{
	EXPECT_TRUE(BattleControllerInput::capturesShortcut(EShortcut::GLOBAL_ACCEPT));
	EXPECT_TRUE(BattleControllerInput::capturesShortcut(EShortcut::GLOBAL_CANCEL));
	EXPECT_FALSE(BattleControllerInput::capturesShortcut(EShortcut::BATTLE_PREVIOUS_ATTACK_DIRECTION));
	EXPECT_FALSE(BattleControllerInput::capturesShortcut(EShortcut::BATTLE_NEXT_ATTACK_DIRECTION));
	EXPECT_FALSE(BattleControllerInput::capturesShortcut(EShortcut::BATTLE_DEFEND));
	EXPECT_FALSE(BattleControllerInput::capturesShortcut(EShortcut::BATTLE_WAIT));
	for(const auto & [bindings, expected] : std::vector<std::pair<std::vector<EShortcut>, EShortcut>>{
		{{EShortcut::GLOBAL_ACCEPT, EShortcut::GLOBAL_RETURN, EShortcut::BATTLE_TACTICS_END}, EShortcut::GLOBAL_ACCEPT},
		{{EShortcut::GLOBAL_CANCEL, EShortcut::GLOBAL_RETURN}, EShortcut::GLOBAL_CANCEL},
		{{EShortcut::BATTLE_END_WITH_AUTOCOMBAT}, EShortcut::BATTLE_AUTOCOMBAT},
		{{EShortcut::MOUSE_LEFT}, EShortcut::BATTLE_DEFEND},
		{{EShortcut::MOUSE_RIGHT}, EShortcut::BATTLE_WAIT}})
	{
		auto shortcuts = bindings;
		EXPECT_TRUE(BattleControllerInput::translateShortcuts(shortcuts, true, false));
		EXPECT_EQ(shortcuts, (std::vector{expected}));
	}
	for(const auto route : {EShortcut::BATTLE_AUTOCOMBAT, EShortcut::BATTLE_CAST_SPELL,
		EShortcut::BATTLE_USE_CREATURE_SPELL, EShortcut::BATTLE_RETREAT, EShortcut::BATTLE_SURRENDER})
	{
		std::vector keys{route};
		EXPECT_FALSE(BattleControllerInput::translateShortcuts(keys, true, false));
		EXPECT_EQ(keys, (std::vector{route}));
	}
	std::vector cursorKeys{EShortcut::MOUSE_LEFT};
	EXPECT_FALSE(BattleControllerInput::translateShortcuts(cursorKeys, false, false));
	EXPECT_EQ(cursorKeys, (std::vector{EShortcut::MOUSE_LEFT}));
	std::vector endAuto{EShortcut::BATTLE_END_WITH_AUTOCOMBAT};
	EXPECT_FALSE(BattleControllerInput::translateShortcuts(endAuto, false, false));
	EXPECT_FALSE(BattleControllerInput::translateShortcuts(endAuto, true, false, true));
	EXPECT_EQ(endAuto, (std::vector{EShortcut::BATTLE_END_WITH_AUTOCOMBAT}));
}

TEST(BattleControllerInput, MeleeShouldersCannotAccidentallyWaitOrDefend)
{
	std::vector left{EShortcut::BATTLE_PREVIOUS_ATTACK_DIRECTION, EShortcut::BATTLE_DEFEND};
	std::vector right{EShortcut::BATTLE_NEXT_ATTACK_DIRECTION, EShortcut::BATTLE_WAIT, EShortcut::BATTLE_TACTICS_END};
	EXPECT_TRUE(BattleControllerInput::translateShortcuts(left, true, true));
	EXPECT_TRUE(BattleControllerInput::translateShortcuts(right, true, true));
	EXPECT_EQ(left, (std::vector{EShortcut::BATTLE_PREVIOUS_ATTACK_DIRECTION}));
	EXPECT_EQ(right, (std::vector{EShortcut::BATTLE_NEXT_ATTACK_DIRECTION}));
	std::vector tactics{EShortcut::BATTLE_NEXT_ATTACK_DIRECTION, EShortcut::BATTLE_WAIT, EShortcut::BATTLE_TACTICS_END};
	const auto original = tactics;
	EXPECT_FALSE(BattleControllerInput::translateShortcuts(tactics, true, false));
	EXPECT_EQ(tactics, original); // native tactics availability still owns End/Wait
}

TEST(BattleControllerInput, NavigatePreviewConfirmThenRestoreFreshIntentAfterTurn)
{
	BattleInputJourney journey;
	const auto destination = BattleControllerInput::adjacentHex(journey.preview->hex, {1, 0});
	journey.preview->hex = destination;
	journey.press();
	journey.release();
	ASSERT_EQ(journey.submitted.size(), 1);
	journey.press();
	journey.cancel();
	journey.preview->actor++;
	journey.preview->hex = BattleControllerInput::initialHex(BattleHex(2, 4));
	journey.release();
	EXPECT_EQ(journey.submitted.size(), 1);
	journey.press();
	journey.release();
	EXPECT_EQ(journey.submitted.size(), 2);
}

TEST(ControllerInput, EntireStickMustReachNeutralBeforeRearming)
{
	ControllerInput::AxisLatch latch;
	EXPECT_TRUE(latch.update(.8, 0));
	EXPECT_TRUE(latch.update(.8, 1));
	latch.cancel();
	EXPECT_FALSE(latch.update(0, 0));
	EXPECT_FALSE(latch.update(-.8, 0));
	EXPECT_FALSE(latch.update(0, 1));
	EXPECT_TRUE(latch.update(0, 0));
	EXPECT_TRUE(latch.update(-.8, 1));
}
