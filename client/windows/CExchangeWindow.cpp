/*
 * CExchangeWindow.cpp, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 *
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 *
 */
#include "StdInc.h"
#include "CExchangeWindow.h"

#include "CHeroBackpackWindow.h"

#include "../CPlayerInterface.h"

#include "../GameEngine.h"
#include "../GameInstance.h"
#include "../gui/CursorHandler.h"
#include "../gui/Shortcut.h"
#include "../gui/WindowHandler.h"

#include "../widgets/CGarrisonInt.h"
#include "../widgets/Images.h"
#include "../widgets/Buttons.h"
#include "../widgets/TextControls.h"

#include "render/IRenderHandler.h"

#include "../../lib/CSkillHandler.h"
#include "../../lib/GameLibrary.h"
#include "../../lib/callback/CCallback.h"
#include "../../lib/entities/hero/CHeroHandler.h"
#include "../../lib/filesystem/Filesystem.h"
#include "../../lib/mapObjects/CGHeroInstance.h"
#include "../../lib/mapObjects/army/CStackInstance.h"
#include "../../lib/texts/CGeneralTextHandler.h"
#include "../../lib/texts/TextOperations.h"

#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
#include "../../lib/CAndroidVMHelper.h"
#include "../../lib/thor/ThorContext.h"

#endif

static const std::string QUICK_EXCHANGE_BG = "quick-exchange/TRADEQE";

static bool isQuickExchangeLayoutAvailable()
{
	return CResourceHandler::get()->existsResource(ImagePath::builtin("SPRITES/" + QUICK_EXCHANGE_BG));
}

CExchangeWindow::CExchangeWindow(ObjectInstanceID hero1, ObjectInstanceID hero2, QueryID queryID)
	: CWindowObject(PLAYER_COLORED | BORDERED, ImagePath::builtin(isQuickExchangeLayoutAvailable() ? QUICK_EXCHANGE_BG : (ENGINE->isRoeData() ? "TRADE" : "TRADE2"))),
	controller(hero1, hero2)
{
	const bool qeLayout = isQuickExchangeLayoutAvailable();

	OBJECT_CONSTRUCTION;
	addUsedEvents(KEYBOARD);

	heroInst[0] = GAME->interface()->cb->getHero(hero1);
	heroInst[1] = GAME->interface()->cb->getHero(hero2);

	auto genTitle = [](const CGHeroInstance * h)
	{
		MetaString title = MetaString::createFromTextID("core.genrltxt.138"); // %s, Level %d %s
		title.replaceTextID(h->getNameTextID());
		title.replaceNumber(h->level);
		title.replaceTextID(h->getClassNameTextID());
		return title.toString(&GAME->translator());
	};

	titles[0] = std::make_shared<CLabel>(147, qeLayout ? 21 : 25, FONT_SMALL, ETextAlignment::CENTER, Colors::WHITE, genTitle(heroInst[0]));
	titles[1] = std::make_shared<CLabel>(653, qeLayout ? 21 : 25, FONT_SMALL, ETextAlignment::CENTER, Colors::WHITE, genTitle(heroInst[1]));

	for(int g = 0; g < 4; ++g)
	{
		if (qeLayout)
			primSkillImages.push_back(std::make_shared<CAnimImage>(AnimationPath::builtin("PSKIL32"), g, Rect(389, 12 + 26 * g, 22, 22)));
		else
			primSkillImages.push_back(std::make_shared<CAnimImage>(AnimationPath::builtin("PSKIL32"), g, 0, 385, 19 + 36 * g));
	}

	for(int leftRight : {0, 1})
	{
		const CGHeroInstance * hero = heroInst.at(leftRight);

		for(int m=0; m<GameConstants::PRIMARY_SKILLS; ++m)
			primSkillValues[leftRight].push_back(std::make_shared<CLabel>(352 + (qeLayout ? 96 : 93) * leftRight, (qeLayout ? 22 : 35) + (qeLayout ? 26 : 36) * m, FONT_SMALL, ETextAlignment::CENTER, Colors::WHITE));


		for(int m=0; m < std::min(static_cast<int>(hero->secSkills.size()), 8); ++m)
			secSkills[leftRight].push_back(std::make_shared<CSecSkillPlace>(Point(32 + 36 * m + 454 * leftRight, qeLayout ? 80 : 88), CSecSkillPlace::ImageSize::SMALL,
				hero->secSkills[m].first, hero->secSkills[m].second));

		specImages[leftRight] = std::make_shared<CAnimImage>(AnimationPath::builtin("UN32"), hero->getHeroType()->imageIndex, 0, 67 + 490 * leftRight, qeLayout ? 41 : 45);

		expImages[leftRight] = std::make_shared<CAnimImage>(AnimationPath::builtin("PSKIL32"), 4, 0, 103 + 490 * leftRight, qeLayout ? 41 : 45);
		expValues[leftRight] = std::make_shared<CLabel>(119 + 490 * leftRight, qeLayout ? 66 : 71, FONT_SMALL, ETextAlignment::CENTER, Colors::WHITE);

		manaImages[leftRight] = std::make_shared<CAnimImage>(AnimationPath::builtin("PSKIL32"), 5, 0, 139 + 490 * leftRight, qeLayout ? 41 : 45);
		manaValues[leftRight] = std::make_shared<CLabel>(155 + 490 * leftRight, qeLayout ? 66 : 71, FONT_SMALL, ETextAlignment::CENTER, Colors::WHITE);
	}

	artifs[0] = std::make_shared<CArtifactsOfHeroMain>(Point(-334, 151));
	artifs[0]->clickPressedCallback = [this, hero = heroInst[0]](const CArtPlace & artPlace, const Point & cursorPosition){clickPressedOnArtPlace(hero, artPlace.slot, true, false, false, cursorPosition);};
	artifs[0]->showPopupCallback = [this, heroArts = artifs[0]](CArtPlace & artPlace, const Point & cursorPosition){showArtifactPopup(*heroArts, artPlace, cursorPosition);};
	artifs[0]->gestureCallback = [this, hero = heroInst[0]](const CArtPlace & artPlace, const Point & cursorPosition){showQuickBackpackWindow(hero, artPlace.slot, cursorPosition);};
	artifs[0]->setHero(heroInst[0]);
	artifs[1] = std::make_shared<CArtifactsOfHeroMain>(Point(98, 151));
	artifs[1]->clickPressedCallback = [this, hero = heroInst[1]](const CArtPlace & artPlace, const Point & cursorPosition){clickPressedOnArtPlace(hero, artPlace.slot, true, false, false, cursorPosition);};
	artifs[1]->showPopupCallback = [this, heroArts = artifs[1]](CArtPlace & artPlace, const Point & cursorPosition){showArtifactPopup(*heroArts, artPlace, cursorPosition);};
	artifs[1]->gestureCallback = [this, hero = heroInst[1]](const CArtPlace & artPlace, const Point & cursorPosition){showQuickBackpackWindow(hero, artPlace.slot, cursorPosition);};
	artifs[1]->setHero(heroInst[1]);


	addSet(artifs[0]);
	addSet(artifs[1]);

	for(int g=0; g<4; ++g)
	{
		primSkillAreas.push_back(std::make_shared<LRClickableAreaWTextComp>());
		if (qeLayout)
			primSkillAreas[g]->pos = Rect(Point(pos.x + 324, pos.y + 12 + 26 * g), Point(152, 22));
		else
			primSkillAreas[g]->pos = Rect(Point(pos.x + 329, pos.y + 19 + 36 * g), Point(140, 32));
		MetaString hoverText;
		hoverText.appendTextID("core.heroscrn.1");
		hoverText.replaceTextID("core.priskill", g);

		primSkillAreas[g]->text = GAME->translator().translate("core.arraytxt", 2 + g);
		primSkillAreas[g]->component = Component( ComponentType::PRIM_SKILL, PrimarySkill(g));
		primSkillAreas[g]->hoverText = hoverText.toString(&GAME->translator());
	}

	//heroes related thing
	for(int b=0; b < heroInst.size(); b++)
	{
		const CGHeroInstance * hero = heroInst.at(b);

		heroAreas[b] = std::make_shared<CHeroArea>(257 + 228 * b + (qeLayout ? 1 : 0), qeLayout ? 10 : 13, hero);
		heroAreas[b]->addClickCallback([this, hero]() -> void
									   {
										   if(getPickedArtifact() == nullptr)
											   GAME->interface()->openHeroWindow(hero);
									   });

		specialtyAreas[b] = std::make_shared<LRClickableAreaWText>();
		specialtyAreas[b]->pos = Rect(Point(pos.x + 69 + 490 * b, pos.y + (qeLayout ? 41 : 45)), Point(32, 32));
		specialtyAreas[b]->hoverText = LIBRARY->generaltexth->translate("core.heroscrn.27");
		specialtyAreas[b]->text = hero->getHeroType()->getSpecialtyDescriptionTranslated();

		experienceAreas[b] = std::make_shared<LRClickableAreaWText>();
		experienceAreas[b]->pos = Rect(Point(pos.x + 105 + 490 * b, pos.y + (qeLayout ? 41 : 45)), Point(32, 32));
		experienceAreas[b]->hoverText = LIBRARY->generaltexth->translate("core.heroscrn.9");
		MetaString experienceText = MetaString::createFromTextID("core.genrltxt.2");
		experienceText.replaceNumber(hero->level);
		experienceText.replaceNumber(LIBRARY->heroh->reqExp(hero->level+1));
		experienceText.replaceNumber(hero->exp);
		experienceAreas[b]->text = experienceText.toString(&GAME->translator());

		spellPointsAreas[b] = std::make_shared<LRClickableAreaWText>();
		spellPointsAreas[b]->pos = Rect(Point(pos.x + 141 + 490 * b, pos.y + (qeLayout ? 41 : 45)), Point(32, 32));
		spellPointsAreas[b]->hoverText = LIBRARY->generaltexth->translate("core.heroscrn.22");
		MetaString spellPointsText = MetaString::createFromTextID("core.genrltxt.205");
		spellPointsText.replaceTextID(hero->getNameTextID());
		spellPointsText.replaceNumber(hero->mana);
		spellPointsText.replaceNumber(hero->manaLimit());
		spellPointsAreas[b]->text = spellPointsText.toString(&GAME->translator());

		morale[b] = std::make_shared<MoraleLuckBox>(true, Rect(Point(176 + 490 * b, 39), Point(32, 32)), true);
		luck[b] = std::make_shared<MoraleLuckBox>(false,  Rect(Point(212 + 490 * b, 39), Point(32, 32)), true);
	}

	quit = std::make_shared<CButton>(Point(732, 567), AnimationPath::builtin("IOKAY.DEF"), LIBRARY->generaltexth->zelp[600], std::bind(&CExchangeWindow::close, this), EShortcut::GLOBAL_ACCEPT);
	if(queryID.getNum() > 0)
		quit->addCallback([=](){ GAME->interface()->cb->selectionMade(0, queryID); });

	questlogButton[0] = std::make_shared<CButton>(Point( qeLayout ? 8 : 10, qeLayout ? 39 : 44), AnimationPath::builtin("hsbtns4.def"), CButton::tooltip(LIBRARY->generaltexth->translate("core.heroscrn.0")), std::bind(&CExchangeWindow::questLogShortcut, this));
	questlogButton[1] = std::make_shared<CButton>(Point(740, qeLayout ? 39 : 44), AnimationPath::builtin("hsbtns4.def"), CButton::tooltip(LIBRARY->generaltexth->translate("core.heroscrn.0")), std::bind(&CExchangeWindow::questLogShortcut, this));
	questlogButton[0]->block(!GAME->interface()->hasJournalEntries());
	questlogButton[1]->block(!GAME->interface()->hasJournalEntries());

	Rect barRect(5, 578, 725, 18);
	statusbar = CGStatusBar::create(std::make_shared<CPicture>(background->getSurface(), barRect, 5, 578));

	//garrison interface

	garr = std::make_shared<CGarrisonInt>(Point(69, qeLayout ? 120 : 131), 4, Point(418,0), heroInst[0], heroInst[1], true, true);
	auto splitButtonCallback = [&](){ garr->splitClick(); };
	garr->addSplitBtn(std::make_shared<CButton>( Point( 10, qeLayout ? 122 : 132), AnimationPath::builtin("TSBTNS.DEF"), CButton::tooltip(LIBRARY->generaltexth->translate("core.tcommand.3")), splitButtonCallback, EShortcut::HERO_ARMY_SPLIT));
	garr->addSplitBtn(std::make_shared<CButton>( Point(744, qeLayout ? 122 : 132), AnimationPath::builtin("TSBTNS.DEF"), CButton::tooltip(LIBRARY->generaltexth->translate("core.tcommand.3")), splitButtonCallback, EShortcut::HERO_ARMY_SPLIT));

	if(qeLayout)
	{
		buttonMoveUnitsFromLeftToRight = std::make_shared<CButton>(
			Point(325, 118),
			AnimationPath::builtin("quick-exchange/armRight.DEF"),
			CButton::tooltip(LIBRARY->generaltexth->translate("vcmi.quickExchange.moveAllUnits")),
			[this](){ this->moveUnitsShortcut(true); });

		buttonMoveUnitsFromRightToLeft = std::make_shared<CButton>(
			Point(425, 118),
			AnimationPath::builtin("quick-exchange/armLeft.DEF"),
			CButton::tooltip(LIBRARY->generaltexth->translate("vcmi.quickExchange.moveAllUnits")),
			[this](){ this->moveUnitsShortcut(false); });

		buttonMoveArtifactsFromLeftToRight = std::make_shared<CButton>(
			Point(325, 154), AnimationPath::builtin("quick-exchange/artRight.DEF"),
			CButton::tooltip(LIBRARY->generaltexth->translate("vcmi.quickExchange.moveAllArtifacts")),
			[this](){ this->moveArtifactsCallback(true);});

		buttonMoveArtifactsFromRightToLeft = std::make_shared<CButton>(
			Point(425, 154), AnimationPath::builtin("quick-exchange/artLeft.DEF"),
			CButton::tooltip(LIBRARY->generaltexth->translate("vcmi.quickExchange.moveAllArtifacts")),
			[this](){ this->moveArtifactsCallback(false);});

		exchangeUnitsButton = std::make_shared<CButton>(
			Point(377, 118),
			AnimationPath::builtin("quick-exchange/swapAll.DEF"),
			CButton::tooltip(LIBRARY->generaltexth->translate("vcmi.quickExchange.swapAllUnits")),
			[this](){ controller.swapArmy(); });

		exchangeArtifactsButton  = std::make_shared<CButton>(
			Point(377, 154),
			AnimationPath::builtin("quick-exchange/swapAll.DEF"),
			CButton::tooltip(LIBRARY->generaltexth->translate("vcmi.quickExchange.swapAllArtifacts")),
			[this](){ this->swapArtifactsCallback(); });

		backpackButtonLeft = std::make_shared<CButton>(
			Point(325, 518),
			AnimationPath::builtin("heroBackpack"),
			CButton::tooltipLocalized("vcmi.heroWindow.openBackpack"),
			[this](){ this->backpackShortcut(true); });

		backpackButtonRight = std::make_shared<CButton>(
			Point(419, 518),
			AnimationPath::builtin("heroBackpack"),
			CButton::tooltipLocalized("vcmi.heroWindow.openBackpack"),
			[this](){ this->backpackShortcut(false); });

		backpackButtonLeft->setOverlay(std::make_shared<CPicture>(ImagePath::builtin("heroWindow/backpackButtonIcon")));
		backpackButtonRight->setOverlay(std::make_shared<CPicture>(ImagePath::builtin("heroWindow/backpackButtonIcon")));

		auto leftHeroBlock = heroInst[0]->tempOwner != GAME->interface()->cb->getPlayerID();
		auto rightHeroBlock = heroInst[1]->tempOwner != GAME->interface()->cb->getPlayerID();

		buttonMoveUnitsFromLeftToRight->block(leftHeroBlock);
		buttonMoveUnitsFromRightToLeft->block(rightHeroBlock);
		buttonMoveArtifactsFromLeftToRight->block(leftHeroBlock);
		buttonMoveArtifactsFromRightToLeft->block(rightHeroBlock);

		exchangeUnitsButton->block(leftHeroBlock || rightHeroBlock);
		exchangeArtifactsButton->block(leftHeroBlock || rightHeroBlock);

		backpackButtonLeft->block(leftHeroBlock);
		backpackButtonRight->block(rightHeroBlock);

		for(int i = 0; i < GameConstants::ARMY_SIZE; i++)
		{
			moveUnitFromRightToLeftButtons.push_back(
				std::make_shared<CButton>(
					Point(483 + 36 * i, 155),
					AnimationPath::builtin("quick-exchange/unitLeft.DEF"),
					CButton::tooltip(LIBRARY->generaltexth->translate("vcmi.quickExchange.moveUnit")),
					[this, i]() { creatureArrowButtonCallback(false, SlotID(i)); }));
			moveUnitFromRightToLeftButtons.back()->block(leftHeroBlock);

			moveUnitFromLeftToRightButtons.push_back(
				std::make_shared<CButton>(
					Point(65 + 36 * i, 155),
					AnimationPath::builtin("quick-exchange/unitRight.DEF"),
					CButton::tooltip(LIBRARY->generaltexth->translate("vcmi.quickExchange.moveUnit")),
					[this, i]() { creatureArrowButtonCallback(true, SlotID(i)); }));
			moveUnitFromLeftToRightButtons.back()->block(rightHeroBlock);
		}
	}

	CExchangeWindow::updateArtifacts();
}

void CExchangeWindow::activate()
{
	if(isActive())
		return;

	CStatusbarWindow::activate();

#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
	updateThorActionState();
#endif
}

void CExchangeWindow::deactivate()
{
	if(!isActive())
		return;

	CStatusbarWindow::deactivate();

#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
	ThorContextRecord context;
	context = thorContextStore().publishNext(std::move(context));
	CAndroidVMHelper().publishThorContext(context.revision, context.contextId, context.title, context.status);
	CAndroidVMHelper().publishThorActionState(context.revision, context.enabledActionMask, context.activeActionMask);
#endif
}

void CExchangeWindow::creatureArrowButtonCallback(bool leftToRight, SlotID slotId)
{
	if (ENGINE->isKeyboardAltDown())
		controller.moveArmy(leftToRight, slotId);
	else if (ENGINE->isKeyboardCtrlDown())
		controller.moveSingleStackCreature(leftToRight, slotId, true);
	else if (ENGINE->isKeyboardShiftDown())
		controller.moveSingleStackCreature(leftToRight, slotId, false);
	else
		controller.moveStack(leftToRight, slotId);
}

void CExchangeWindow::moveArtifactsCallback(bool leftToRight)
{
	bool moveEquipped = !ENGINE->isKeyboardShiftDown();
	bool moveBackpack = !ENGINE->isKeyboardCmdDown();
	controller.moveArtifacts(leftToRight, moveEquipped, moveBackpack);
};

void CExchangeWindow::swapArtifactsCallback()
{
	bool moveEquipped = !ENGINE->isKeyboardShiftDown();
	bool moveBackpack = !ENGINE->isKeyboardCmdDown();
	controller.swapArtifacts(moveEquipped, moveBackpack);
}

void CExchangeWindow::moveUnitsShortcut(bool leftToRight)
{
	std::optional<SlotID> slotId = std::nullopt;
	if(const auto * slot = getSelectedSlotID())
		slotId = slot->getSlot();
	controller.moveArmy(leftToRight, slotId);
};

void CExchangeWindow::backpackShortcut(bool leftHero)
{
	ENGINE->windows().createAndPushWindow<CHeroBackpackWindow>(heroInst[leftHero ? 0 : 1], artSets);
};

void CExchangeWindow::keyPressed(EShortcut key)
{
	switch (key)
	{
		case EShortcut::EXCHANGE_ARMY_TO_LEFT:
			moveUnitsShortcut(false);
		break;
		case EShortcut::EXCHANGE_ARMY_TO_RIGHT:
			moveUnitsShortcut(true);
		break;
		case EShortcut::EXCHANGE_ARMY_SWAP:
			controller.swapArmy();
		break;
		case EShortcut::EXCHANGE_ARTIFACTS_TO_LEFT:
			controller.moveArtifacts(false, true, true);
		break;
		case EShortcut::EXCHANGE_ARTIFACTS_TO_RIGHT:
			controller.moveArtifacts(true, true, true);
		break;
		case EShortcut::EXCHANGE_ARTIFACTS_SWAP:
			controller.swapArtifacts(true, true);
		break;
		case EShortcut::EXCHANGE_EQUIPPED_TO_LEFT:
			controller.moveArtifacts(false, true, false);
		break;
		case EShortcut::EXCHANGE_EQUIPPED_TO_RIGHT:
			controller.moveArtifacts(true, true, false);
		break;
		case EShortcut::EXCHANGE_EQUIPPED_SWAP:
			controller.swapArtifacts(true, false);
		break;
		case EShortcut::EXCHANGE_BACKPACK_TO_LEFT:
			controller.moveArtifacts(false, false, true);
		break;
		case EShortcut::EXCHANGE_BACKPACK_TO_RIGHT:
			controller.moveArtifacts(true, false, true);
		break;
		case EShortcut::EXCHANGE_BACKPACK_SWAP:
			controller.swapArtifacts(false, true);
		break;
		case EShortcut::EXCHANGE_BACKPACK_LEFT:
			backpackShortcut(true);
		break;
		case EShortcut::EXCHANGE_BACKPACK_RIGHT:
			backpackShortcut(false);
		break;
	}
}

const CGarrisonSlot * CExchangeWindow::getSelectedSlotID() const
{
	return garr->getSelection();
}

void CExchangeWindow::updateGarrisons()
{
	garr->recreateSlots();

	updateArtifacts();

#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
	if(isActive())
		updateThorActionState();
#endif
}

#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
namespace
{
	ThorHeroMeetingArmies thorHeroMeetingArmies(const std::array<const CGHeroInstance *, 2> & heroes)
	{
		ThorHeroMeetingArmies result;
		result.leftHeroId = heroes[0] ? heroes[0]->id.getNum() : -1;
		result.rightHeroId = heroes[1] ? heroes[1]->id.getNum() : -1;
		result.leftArmyId = result.leftHeroId;
		result.rightArmyId = result.rightHeroId;
		if(!heroes[0] || !heroes[1])
			return result;
		result.leftHeroName = heroes[0]->getObjectName().toString(&GAME->translator());
		result.rightHeroName = heroes[1]->getObjectName().toString(&GAME->translator());
		result.locallyControllable = GAME->interface()->makingTurn
			&& heroes[0]->tempOwner == GAME->interface()->playerID && heroes[1]->tempOwner == GAME->interface()->playerID;
		for(std::size_t side = 0; side < heroes.size(); ++side)
		{
			auto & slots = side == 0 ? result.leftSlots : result.rightSlots;
			const auto armyId = side == 0 ? result.leftArmyId : result.rightArmyId;
			for(std::size_t index = 0; index < slots.size(); ++index)
			{
				auto & slot = slots[index];
				slot.armyId = armyId;
				slot.slot = static_cast<int>(index);
				const auto * stack = heroes[side]->getStackPtr(SlotID(static_cast<int>(index)));
				if(!stack)
					continue;
				slot.occupied = true;
				slot.creatureId = stack->getCreatureID().getNum();
				slot.creatureName = stack->getCreature()->getNamePluralTranslated();
				slot.count = stack->getCount();
			}
		}
		return result;
	}
}

void CExchangeWindow::updateThorActionState(bool invalidateActions)
{
	ThorContextRecord context;
	context.contextId = ThorContextIds::HERO_MEETING;
	context.heroMeetingArmies = thorHeroMeetingArmies(heroInst);
	if(context.heroMeetingArmies->locallyControllable && !invalidateActions)
	{
		context.enabledActionMask = thorActionMask(ThorAction::HERO_MEETING_MOVE_STACK)
			| thorActionMask(ThorAction::HERO_MEETING_TRANSFER_STACK)
			| thorActionMask(ThorAction::HERO_MEETING_SWAP_ARMIES)
			| thorActionMask(ThorAction::HERO_MEETING_SPLIT_STACK);
		if(canThorHeroMeetingMoveArmy(*context.heroMeetingArmies, true))
			context.enabledActionMask |= thorActionMask(ThorAction::HERO_MEETING_ARMY_LEFT_TO_RIGHT);
		if(canThorHeroMeetingMoveArmy(*context.heroMeetingArmies, false))
			context.enabledActionMask |= thorActionMask(ThorAction::HERO_MEETING_ARMY_RIGHT_TO_LEFT);
	}
	const auto previous = thorContextStore().snapshot();
	context.actionEpoch = previous.contextId == ThorContextIds::HERO_MEETING ? previous.actionEpoch + (invalidateActions ? 1 : 0) : 0;
	context = thorContextStore().publishNext(std::move(context));
	if(context.revision == previous.revision)
		return;
	CAndroidVMHelper().publishThorContext(context.revision, context.contextId, context.title, context.status);
	CAndroidVMHelper().publishThorHeroMeetingArmies(context.revision, *context.heroMeetingArmies);
	CAndroidVMHelper().publishThorActionState(context.revision, context.enabledActionMask, context.activeActionMask);
}

bool CExchangeWindow::matchesThorContext(const ThorContextRecord & context) const
{
	if(!isActive() || ENGINE->windows().topWindow<CExchangeWindow>().get() != this
		|| context.contextId != ThorContextIds::HERO_MEETING || !context.heroMeetingArmies)
		return false;
	const auto & armies = *context.heroMeetingArmies;
	return heroInst[0] && heroInst[1] && armies.leftHeroId == heroInst[0]->id.getNum()
		&& armies.rightHeroId == heroInst[1]->id.getNum() && armies.leftArmyId == heroInst[0]->id.getNum()
		&& armies.rightArmyId == heroInst[1]->id.getNum();
}

bool CExchangeWindow::executeThorAction(const ThorActionRequest & request)
{
	const auto context = thorContextStore().snapshot();
	if(!matchesThorContext(context) || validateThorActionRequest(request, context) != ThorActionValidation::VALID)
		return false;
	const auto rejectWithoutCallback = [this]()
	{
		// A rejected request has no garrison callback to restore the lower action state.
		updateThorActionState(true);
		updateThorActionState();
		return false;
	};
	if(!context.heroMeetingArmies->locallyControllable || thorHeroMeetingArmies(heroInst) != *context.heroMeetingArmies)
		return rejectWithoutCallback();
	if(request.action == ThorAction::HERO_MEETING_MOVE_STACK
		&& !controller.canMoveStack(request.targetId < static_cast<int>(THOR_HERO_MEETING_ARMY_SIZE),
			SlotID(request.targetId % static_cast<int>(THOR_HERO_MEETING_ARMY_SIZE))))
		return rejectWithoutCallback();
	if(request.action == ThorAction::HERO_MEETING_TRANSFER_STACK)
	{
		const auto pair = decodeThorHeroMeetingTransferPair(request.targetId);
		if(!pair)
			return false;
		if(!controller.canTransferStack(pair->sourceIsLeft, SlotID(pair->sourceSlot),
			pair->destinationIsLeft, SlotID(pair->destinationSlot)))
			return rejectWithoutCallback();
	}
	if(request.action == ThorAction::HERO_MEETING_SPLIT_STACK)
	{
		const auto & armies = *context.heroMeetingArmies;
		const bool sourceLeft = request.sourceArmyId == armies.leftArmyId;
		const bool destinationLeft = request.destinationArmyId == armies.leftArmyId;
		if(!controller.canSplitStackExact(sourceLeft, SlotID(request.sourceSlot), destinationLeft,
			SlotID(request.destinationSlot), request.amount))
			return rejectWithoutCallback();
	}

	// Consume this rendered action epoch before invoking any callback that can mutate army state.
	updateThorActionState(true);
	bool executed = false;
	switch(request.action)
	{
	case ThorAction::HERO_MEETING_MOVE_STACK:
		controller.moveStack(request.targetId < static_cast<int>(THOR_HERO_MEETING_ARMY_SIZE),
			SlotID(request.targetId % static_cast<int>(THOR_HERO_MEETING_ARMY_SIZE)));
		executed = true;
		break;
	case ThorAction::HERO_MEETING_TRANSFER_STACK:
	{
		const auto pair = decodeThorHeroMeetingTransferPair(request.targetId);
		if(pair)
		{
			executed = controller.transferStack(pair->sourceIsLeft, SlotID(pair->sourceSlot),
				pair->destinationIsLeft, SlotID(pair->destinationSlot));
		}
		break;
	}
	case ThorAction::HERO_MEETING_ARMY_LEFT_TO_RIGHT:
		controller.moveArmy(true, std::nullopt);
		executed = true;
		break;
	case ThorAction::HERO_MEETING_ARMY_RIGHT_TO_LEFT:
		controller.moveArmy(false, std::nullopt);
		executed = true;
		break;
	case ThorAction::HERO_MEETING_SWAP_ARMIES:
		controller.swapArmy();
		executed = true;
		break;
	case ThorAction::HERO_MEETING_SPLIT_STACK:
	{
		const auto & armies = *context.heroMeetingArmies;
		executed = controller.splitStackExact(request.sourceArmyId == armies.leftArmyId, SlotID(request.sourceSlot),
			request.destinationArmyId == armies.leftArmyId, SlotID(request.destinationSlot), request.amount);
		break;
	}
	default:
		break;
	}
	return executed;
}
#endif

bool CExchangeWindow::holdsGarrison(const CArmedInstance * army)
{
	return garr->upperArmy() == army || garr->lowerArmy() == army;
}

void CExchangeWindow::questLogShortcut()
{
	ENGINE->cursor().dragAndDropCursor(nullptr);
	GAME->interface()->showQuestLog();
}

void CExchangeWindow::updateArtifacts()
{
	const bool qeLayout = isQuickExchangeLayoutAvailable();

	OBJECT_CONSTRUCTION;

	CWindowWithArtifacts::updateArtifacts();

	for(size_t leftRight : {0, 1})
	{
		const CGHeroInstance * hero = heroInst.at(leftRight);

		for(int m=0; m<GameConstants::PRIMARY_SKILLS; ++m)
		{
			auto value = heroInst[leftRight]->getPrimSkillLevel(static_cast<PrimarySkill>(m));
			primSkillValues[leftRight][m]->setText(std::to_string(value));
		}

		int slots = 8;
		bool isMoreSkillsThanSlots = hero->secSkills.size() > slots;
		for(int m=0; m < std::min(static_cast<int>(hero->secSkills.size()), 8); ++m)
		{
			if(m == slots - 1)
			{
				if(isMoreSkillsThanSlots)
				{
					Rect r(Point(32 + 36 * m + 454 * leftRight, qeLayout ? 83 : 88), Point(34, 28));
					secSkillsFull[leftRight] = std::make_shared<CMultiLineLabel>(r, EFonts::FONT_MEDIUM, ETextAlignment::CENTER, Colors::WHITE, "...");
					secSkillsFullArea[leftRight] = std::make_shared<LRClickableAreaWText>(r, LIBRARY->generaltexth->translate("vcmi.kingdomOverview.secSkillOverflow.hover"), LIBRARY->generaltexth->translate("vcmi.kingdomOverview.secSkillOverflow.help"));
					secSkills[leftRight][m]->setSkill(SecondarySkill::NONE);
					continue;
				}
				else
				{
					secSkillsFull[leftRight].reset();
					secSkillsFullArea[leftRight].reset();
				}
			}

			int id = hero->secSkills[m].first;
			int level = hero->secSkills[m].second;

			secSkills[leftRight][m]->setSkill(id, level);
		}

		expValues[leftRight]->setText(TextOperations::formatMetric(hero->exp, 3));
		manaValues[leftRight]->setText(TextOperations::formatMetric(hero->mana, 3));

		morale[leftRight]->set(hero);
		luck[leftRight]->set(hero);
	}
}
