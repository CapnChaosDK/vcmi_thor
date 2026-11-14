/*
 * CMarketWindow.cpp, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 *
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 *
 */

#include "StdInc.h"
#include "CMarketWindow.h"

#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
#include "../../lib/CAndroidVMHelper.h"
#include "../thor/ThorRequestTracker.h"
#include "../gui/WindowHandler.h"
#include "../../lib/json/JsonNode.h"
#include "../../lib/CConfigHandler.h"
#include "../../lib/thor/ThorDialog.h"
#include "InfoWindows.h"
#endif

#include "../GameEngine.h"
#include "../GameInstance.h"
#include "../gui/Shortcut.h"

#include "../widgets/Buttons.h"
#include "../widgets/TextControls.h"
#include "../widgets/markets/CAltarArtifacts.h"
#include "../widgets/markets/CAltarCreatures.h"
#include "../widgets/markets/CArtifactsBuying.h"
#include "../widgets/markets/CArtifactsSelling.h"
#include "../widgets/markets/CFreelancerGuild.h"
#include "../widgets/markets/CMarketResources.h"
#include "../widgets/markets/CTransferResources.h"

#include "../CPlayerInterface.h"

#include "../../lib/GameLibrary.h"
#include "../../lib/callback/CCallback.h"
#include "../../lib/entities/building/CBuilding.h"
#include "../../lib/entities/artifact/CArtifact.h"
#include "../../lib/entities/artifact/CArtifactInstance.h"
#include "../../lib/networkPacks/ArtifactLocation.h"
#include "../../lib/entities/ResourceTypeHandler.h"
#include "../../lib/mapObjects/CGHeroInstance.h"
#include "../../lib/mapObjects/CGMarket.h"
#include "../../lib/mapObjects/CGTownInstance.h"
#include "../../lib/texts/CGeneralTextHandler.h"

CMarketWindow::CMarketWindow(const IMarket * market, const CGHeroInstance * hero, const std::function<void()> & onWindowClosed, EMarketMode mode, bool allowResourceTradeWhenNotMakingTurn, CPlayerInterface * resourceTradeInterface)
	: CWindowObject(PLAYER_COLORED)
	, windowClosedCallback(onWindowClosed)
	, allowResourceTradeWhenNotMakingTurn(allowResourceTradeWhenNotMakingTurn)
	, resourceTradeInterface(resourceTradeInterface ? resourceTradeInterface : GAME->interface())
{
	assert(mode == EMarketMode::RESOURCE_RESOURCE || mode == EMarketMode::RESOURCE_PLAYER || mode == EMarketMode::CREATURE_RESOURCE ||
		mode == EMarketMode::RESOURCE_ARTIFACT || mode == EMarketMode::ARTIFACT_RESOURCE || mode == EMarketMode::ARTIFACT_EXP ||
		mode == EMarketMode::CREATURE_EXP);
	
	OBJECT_CONSTRUCTION;

	if(mode == EMarketMode::RESOURCE_RESOURCE)
		createMarketResources(market, hero);
	else if(mode == EMarketMode::RESOURCE_PLAYER)
		createTransferResources(market, hero);
	else if(mode == EMarketMode::CREATURE_RESOURCE)
		createFreelancersGuild(market, hero);
	else if(mode == EMarketMode::RESOURCE_ARTIFACT)
		createArtifactsBuying(market, hero);
	else if(mode == EMarketMode::ARTIFACT_RESOURCE)
		createArtifactsSelling(market, hero);
	else if(mode == EMarketMode::ARTIFACT_EXP)
		createAltarArtifacts(market, hero);
	else if(mode == EMarketMode::CREATURE_EXP)
		createAltarCreatures(market, hero);

	statusbar = CGStatusBar::create(std::make_shared<CPicture>(background->getSurface(), Rect(8, pos.h - 26, pos.w - 16, 19), 8, pos.h - 26));
}

void CMarketWindow::updateArtifacts()
{
	update();
}

void CMarketWindow::updateGarrisons()
{
	update();
}

void CMarketWindow::updateResources()
{
	update();
}

void CMarketWindow::updateExperience()
{
	update();
}

void CMarketWindow::update()
{
	CWindowWithArtifacts::updateArtifacts();
	assert(marketWidget);
	marketWidget->update();
}

void CMarketWindow::close()
{
	// Detach the callback before closing: CWindowObject::close() can destroy this window,
	// and the callback may reopen dialogs/windows, so it must run at most once after close.
	auto callback = std::exchange(windowClosedCallback, nullptr);

	CWindowObject::close();

	if(callback)
		callback();
}

bool CMarketWindow::holdsGarrison(const CArmedInstance * army)
{
	assert(marketWidget);
	return marketWidget->hero == army;
}

void CMarketWindow::createChangeModeButtons(EMarketMode currentMode, const IMarket * market, const CGHeroInstance * hero)
{
	auto isButtonVisible = [currentMode, market, hero](EMarketMode modeButton) -> bool
	{
		if(currentMode == modeButton)
			return false;

		if(!market->allowsTrade(modeButton))
			return false;

		if(currentMode == EMarketMode::ARTIFACT_EXP && modeButton != EMarketMode::CREATURE_EXP)
			return false;
		if(currentMode == EMarketMode::CREATURE_EXP && modeButton != EMarketMode::ARTIFACT_EXP)
			return false;

		if(modeButton == EMarketMode::RESOURCE_RESOURCE || modeButton == EMarketMode::RESOURCE_PLAYER)
		{
			if(const auto town = dynamic_cast<const CGTownInstance*>(market))
				return town->getOwner() == GAME->interface()->playerID;
			else
				return true;
		}
		else
		{
			return hero != nullptr;
		}
	};

	changeModeButtons.clear();
#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
	thorMarketModeNames.clear();
#endif
	auto buttonPos = Point(18, 520);

	auto addButton = [this, &buttonPos](const AnimationPath & picPath, const std::pair<std::string, std::string> & buttonHelpContainer,
		const std::function<void()> & pressButtonFunctor, EShortcut shortcut)
	{
		changeModeButtons.emplace_back(std::make_shared<CButton>(buttonPos, picPath, buttonHelpContainer, pressButtonFunctor, shortcut));
#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
		thorMarketModeNames.push_back(buttonHelpContainer.first);
#endif
		buttonPos -= Point(0, buttonHeightWithMargin);
	};

	if(isButtonVisible(EMarketMode::RESOURCE_PLAYER))
		addButton(AnimationPath::builtin("TPMRKBU1.DEF"), LIBRARY->generaltexth->zelp[612], std::bind(&CMarketWindow::createTransferResources, this, market, hero), EShortcut::MARKET_RESOURCE_PLAYER);
	if(isButtonVisible(EMarketMode::ARTIFACT_RESOURCE))
		addButton(AnimationPath::builtin("TPMRKBU3.DEF"), LIBRARY->generaltexth->zelp[613], std::bind(&CMarketWindow::createArtifactsSelling, this, market, hero), EShortcut::MARKET_ARTIFACT_RESOURCE);
	if(isButtonVisible(EMarketMode::RESOURCE_ARTIFACT))
		addButton(AnimationPath::builtin("TPMRKBU2.DEF"), LIBRARY->generaltexth->zelp[598], std::bind(&CMarketWindow::createArtifactsBuying, this, market, hero), EShortcut::MARKET_RESOURCE_ARTIFACT);

	buttonPos = Point(516, 520 - buttonHeightWithMargin);
	if(isButtonVisible(EMarketMode::CREATURE_RESOURCE))
		addButton(AnimationPath::builtin("TPMRKBU4.DEF"), LIBRARY->generaltexth->zelp[599], std::bind(&CMarketWindow::createFreelancersGuild, this, market, hero), EShortcut::MARKET_CREATURE_RESOURCE);
	if(isButtonVisible(EMarketMode::RESOURCE_RESOURCE))
		addButton(AnimationPath::builtin("TPMRKBU5.DEF"), LIBRARY->generaltexth->zelp[605], std::bind(&CMarketWindow::createMarketResources, this, market, hero), EShortcut::MARKET_RESOURCE_RESOURCE);
	
	buttonPos = Point(516, 421);
	if(isButtonVisible(EMarketMode::CREATURE_EXP))
	{
		addButton(AnimationPath::builtin("ALTSACC.DEF"), LIBRARY->generaltexth->zelp[572], std::bind(&CMarketWindow::createAltarCreatures, this, market, hero), EShortcut::MARKET_CREATURE_EXPERIENCE);
		if(marketWidget->hero->getAlignment() == EAlignment::GOOD)
			changeModeButtons.back()->block(true);
	}
	if(isButtonVisible(EMarketMode::ARTIFACT_EXP))
	{
		addButton(AnimationPath::builtin("ALTART.DEF"), LIBRARY->generaltexth->zelp[580], std::bind(&CMarketWindow::createAltarArtifacts, this, market, hero), EShortcut::MARKET_ARTIFACT_EXPERIENCE);
		if(marketWidget->hero->getAlignment() == EAlignment::EVIL)
			changeModeButtons.back()->block(true);
	}
}

void CMarketWindow::initWidgetInternals(const EMarketMode mode, const std::pair<std::string, std::string> & quitButtonHelpContainer)
{
#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
	thorMarketMode = mode;
#endif
	background->center();
	pos = background->pos;
	marketWidget->setRedrawParent(true);
	marketWidget->moveTo(pos.topLeft());

	createChangeModeButtons(mode, marketWidget->market, marketWidget->hero);
	quitButton = std::make_shared<CButton>(quitButtonPos, AnimationPath::builtin("IOK6432.DEF"),
		quitButtonHelpContainer, [this](){close();}, EShortcut::GLOBAL_RETURN);
	redraw();
}

std::string CMarketWindow::getMarketTitle(const ObjectInstanceID marketId, const EMarketMode mode) const
{
	assert(GAME->interface()->cb->getMarket(marketId));
	assert(vstd::contains(GAME->interface()->cb->getMarket(marketId)->availableModes(), mode));

	if(const auto town = GAME->interface()->cb->getTown(marketId))
	{
		for(const auto & buildingId : town->getBuildings())
		{
			const auto & building = town->getTown()->buildings.at(buildingId);
			if(vstd::contains(building->marketModes, mode))
				return building->getNameTranslated();
		}
	}
	return GAME->interface()->cb->getObj(marketId)->getObjectName().toString(&GAME->translator());
}

ImagePath CMarketWindow::getImagePathBasedOnResources(std::string name)
{
	int res = LIBRARY->resourceTypeHandler->getAllObjects().size();
	if(res == 8)
		name += "-R8";
	else if(res > 8)
		name += "-R9";
	return ImagePath::builtin(name);
}

void CMarketWindow::createArtifactsBuying(const IMarket * market, const CGHeroInstance * hero)
{
	OBJECT_CONSTRUCTION;

	auto image = getImagePathBasedOnResources("TPMRKABS");

	background = createBg(image, PLAYER_COLORED);
	marketWidget = std::make_shared<CArtifactsBuying>(market, hero, getMarketTitle(market->getObjInstanceID(), EMarketMode::RESOURCE_ARTIFACT));
	initWidgetInternals(EMarketMode::RESOURCE_ARTIFACT, LIBRARY->generaltexth->zelp[600]);
}

void CMarketWindow::createArtifactsSelling(const IMarket * market, const CGHeroInstance * hero)
{
	OBJECT_CONSTRUCTION;

	auto image = getImagePathBasedOnResources("TPMRKASS");

	background = createBg(image, PLAYER_COLORED);
	// Create image that copies part of background containing slot MISC_1 into position of slot MISC_5
	artSlotBack = std::make_shared<CPicture>(background->getSurface(), Rect(20, 187, 47, 47), 0, 0);
	artSlotBack->moveTo(pos.topLeft() + Point(18, 339));
	auto artsSellingMarket = std::make_shared<CArtifactsSelling>(market, hero, getMarketTitle(market->getObjInstanceID(), EMarketMode::ARTIFACT_RESOURCE));
	artSets.clear();
	const auto heroArts = artsSellingMarket->getAOHset();
	addSet(heroArts);
	marketWidget = artsSellingMarket;
	initWidgetInternals(EMarketMode::ARTIFACT_RESOURCE, LIBRARY->generaltexth->zelp[600]);
}

void CMarketWindow::createMarketResources(const IMarket * market, const CGHeroInstance * hero)
{
	OBJECT_CONSTRUCTION;

	auto image = getImagePathBasedOnResources("TPMRKRES");

	background = createBg(image, PLAYER_COLORED);
	background->setPlayerColor(resourceTradeInterface->playerID);
	marketWidget = std::make_shared<CMarketResources>(market, hero, allowResourceTradeWhenNotMakingTurn, resourceTradeInterface);
	initWidgetInternals(EMarketMode::RESOURCE_RESOURCE, LIBRARY->generaltexth->zelp[600]);
}

void CMarketWindow::createFreelancersGuild(const IMarket * market, const CGHeroInstance * hero)
{
	OBJECT_CONSTRUCTION;

	auto image = getImagePathBasedOnResources("TPMRKCRS");

	background = createBg(image, PLAYER_COLORED);
	marketWidget = std::make_shared<CFreelancerGuild>(market, hero);
	initWidgetInternals(EMarketMode::CREATURE_RESOURCE, LIBRARY->generaltexth->zelp[600]);
}

void CMarketWindow::createTransferResources(const IMarket * market, const CGHeroInstance * hero)
{
	OBJECT_CONSTRUCTION;

	auto image = getImagePathBasedOnResources("TPMRKPTS");

	background = createBg(image, PLAYER_COLORED);
	marketWidget = std::make_shared<CTransferResources>(market, hero);
	initWidgetInternals(EMarketMode::RESOURCE_PLAYER, LIBRARY->generaltexth->zelp[600]);
}

void CMarketWindow::createAltarArtifacts(const IMarket * market, const CGHeroInstance * hero)
{
	OBJECT_CONSTRUCTION;

	background = createBg(ImagePath::builtin("ALTRART2.bmp"), PLAYER_COLORED);
	auto altarArtifactsStorage = std::make_shared<CAltarArtifacts>(market, hero);
	marketWidget = altarArtifactsStorage;
	artSets.clear();
	const auto heroArts = altarArtifactsStorage->getAOHset();
	heroArts->clickPressedCallback = [this, heroArts](const CArtPlace & artPlace, const Point & cursorPosition)
	{
		clickPressedOnArtPlace(heroArts->getHero(), artPlace.slot, true, true, false, cursorPosition);
	};
	heroArts->showPopupCallback = [this, heroArts](CArtPlace & artPlace, const Point & cursorPosition)
	{
		showArtifactPopup(*heroArts, artPlace, cursorPosition);
	};
	heroArts->gestureCallback = [this, heroArts](const CArtPlace & artPlace, const Point & cursorPosition)
	{
		showQuickBackpackWindow(heroArts->getHero(), artPlace.slot, cursorPosition);
	};
	addSet(heroArts);
	initWidgetInternals(EMarketMode::ARTIFACT_EXP, LIBRARY->generaltexth->zelp[568]);
	updateExperience();
	quitButton->addCallback([altarArtifactsStorage](){altarArtifactsStorage->putBackArtifacts();});
}

void CMarketWindow::createAltarCreatures(const IMarket * market, const CGHeroInstance * hero)
{
	OBJECT_CONSTRUCTION;

	background = createBg(ImagePath::builtin("ALTARMON.bmp"), PLAYER_COLORED);
	marketWidget = std::make_shared<CAltarCreatures>(market, hero);
	initWidgetInternals(EMarketMode::CREATURE_EXP, LIBRARY->generaltexth->zelp[568]);
	updateExperience();
}

#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
void CMarketWindow::activate()
{
	if(isActive()) return;
	CStatusbarWindow::activate();
	refreshThorMarket();
}

void CMarketWindow::deactivate()
{
	if(!isActive()) return;
	CStatusbarWindow::deactivate();
	thorActionQueue().clear();
	const auto state = thorContextStore().publishNext({});
	CAndroidVMHelper().publishThorContext(state.revision, state.contextId, {}, {});
}

void CMarketWindow::refreshThorMarket(bool invalidate)
{
	if(!isActive() || ENGINE->windows().topWindow<CMarketWindow>().get() != this) return;
	const auto previous = thorContextStore().snapshot();
	ThorContextRecord next;
	const auto widget = std::dynamic_pointer_cast<CMarketResources>(marketWidget);
	if(!widget || thorBrowseMarketModes)
	{
		publishThorSpecialMarket();
		return;
	}
	if(widget && widget->market && resourceTradeInterface == GAME->interface())
	{
		const auto marketId = widget->market->getObjInstanceID();
		const auto live = dynamic_cast<const IMarket *>(GAME->interface()->cb->tryGetObj(marketId));
		if(live == widget->market && live->allowsTrade(EMarketMode::RESOURCE_RESOURCE))
		{
			next.contextId = ThorContextIds::RESOURCE_MARKET;
			next.nativeOwnerToken = this;
			next.actionEpoch = previous.nativeOwnerToken == this ? previous.actionEpoch + (invalidate ? 1 : 0) : 0;
			next.title = getMarketTitle(marketId, EMarketMode::RESOURCE_RESOURCE);
			ThorMarket value;
			value.status = thorPendingTrade >= 0 ? 1 : thorTradeStatus;
			value.controllable = GAME->interface()->makingTurn && !settings["session"]["spectate"].Bool();
			const auto town = dynamic_cast<const CGTownInstance *>(live);
			value.controllable = value.controllable && (!town || town->getOwner() == GAME->interface()->playerID)
				&& (!widget->hero || (GAME->interface()->cb->tryGetObj(widget->hero->id) == widget->hero
					&& widget->hero->getOwner() == GAME->interface()->playerID));
			for(const auto & slot : widget->bidTradePanel->slots)
			{
				if(slot->id < 0 || slot->type != EType::RESOURCE) continue;
				ThorMarketRow row;
				row.id = slot->id;
				row.balance = GAME->interface()->cb->getResourceAmount(GameResID(row.id));
				row.name = thorBoundedText(LIBRARY->resourceTypeHandler->getById(GameResID(row.id))->getNameTranslated(), 256);
				row.sell = true;
				row.buy = std::any_of(widget->offerTradePanel->slots.begin(), widget->offerTradePanel->slots.end(),
					[&](const auto & other) { return other->id == row.id && other->type == EType::RESOURCE; });
				value.rows.push_back(std::move(row));
			}
			value.selling = widget->bidTradePanel->getHighlightedItemId();
			value.buying = widget->offerTradePanel->getHighlightedItemId();
			if(value.selling >= 0 && value.buying >= 0 && value.selling != value.buying
				&& value.has(value.selling, true) && value.has(value.buying, false)
				&& std::all_of(value.rows.begin(), value.rows.end(), [](const auto & row) { return row.balance >= 0; }))
			{
				const bool quoted = live->getOffer(value.selling, value.buying, value.bid, value.offer, EMarketMode::RESOURCE_RESOURCE);
				if(quoted && value.bid > 0 && value.offer > 0)
				{
					value.maximum = std::min({widget->offerSlider->getAmount(),
						GAME->interface()->cb->getResourceAmount(GameResID(value.selling)) / value.bid,
						std::numeric_limits<int>::max() / value.bid,
						(std::numeric_limits<int>::max() - GAME->interface()->cb->getResourceAmount(GameResID(value.buying))) / value.offer});
					const auto amount = widget->offerSlider->getValue();
					value.amount = amount <= value.maximum ? amount : 0;
					if(value.bid != widget->bidQty || value.offer != widget->offerQty)
					{
						value.maximum = value.amount = 0;
						if(value.status != 1) value.status = 2;
					}
				}
				else value.bid = value.offer = 0;
			}
			if(value.valid()) next.market = std::move(value);
			else next = {};
		}
	}
	const auto state = thorContextStore().publishNext(std::move(next));
	if(state.revision == previous.revision) return;
	CAndroidVMHelper bridge;
	bridge.publishThorContext(state.revision, state.contextId, state.title, {});
	if(state.market) bridge.publishThorMarket(state.revision, *state.market);
}

bool CMarketWindow::executeThorMarket(const ThorActionRequest & request)
{
	refreshThorMarket();
	const auto current = thorContextStore().snapshot();
	if(current.nativeOwnerToken != this || validateThorActionRequest(request, current) != ThorActionValidation::VALID)
		return false;
	if(request.commandId == ThorExtended::MARKET_MODES)
		return openThorMarketModes(request);
	if(request.commandId == ThorExtended::MARKET_CLOSE)
	{
		close();
		return true;
	}
	const auto widget = std::dynamic_pointer_cast<CMarketResources>(marketWidget);
	if(!widget || !current.market) return false;
	thorTradeStatus = 0;
	if(request.commandId == ThorExtended::MARKET_SELL || request.commandId == ThorExtended::MARKET_BUY)
	{
		const auto panel = request.commandId == ThorExtended::MARKET_SELL ? widget->bidTradePanel : widget->offerTradePanel;
		const auto slot = std::find_if(panel->slots.begin(), panel->slots.end(),
			[&](const auto & row) { return row->id == request.targetId && row->type == EType::RESOURCE; });
		if(slot == panel->slots.end() || !(*slot)->clickPressedCallback) return false;
		(*slot)->clickPressedCallback(*slot);
	}
	else if(request.commandId == ThorExtended::MARKET_AMOUNT)
		widget->offerSlider->scrollTo(request.targetId);
	else if(request.commandId == ThorExtended::MARKET_RESET)
		widget->deselect();
	else if(request.commandId == ThorExtended::MARKET_TRADE)
	{
		// The quote is current; retain the existing widget's exact selection, slider and callback.
		if(widget->bidQty != current.market->bid || widget->offerQty != current.market->offer
			|| widget->offerSlider->getValue() != current.market->amount || widget->deal->isBlocked())
		{
			refreshThorMarket(true);
			return false;
		}
		thorTradeRevision = request.revision;
		thorPendingTrade = captureThorRequest([&] { widget->makeDeal(); });
		if(thorPendingTrade < 0) thorTradeStatus = 2;
	}
	refreshThorMarket(true);
	return true;
}

void CMarketWindow::completeThorMarket(int requestId, bool success)
{
	if(thorPendingTrade != requestId) return;
	thorPendingTrade = -1;
	thorTradeStatus = success ? 3 : 2;
	if(success && !thorSpecialMarketRequest) CAndroidVMHelper().acknowledgeThorCommand(thorTradeRevision, ThorExtended::MARKET_TRADE);
	refreshThorMarket(true);
}

std::vector<std::shared_ptr<CTradeableItem>> CMarketWindow::thorMarketSlots() const
{
	std::vector<std::shared_ptr<CTradeableItem>> slots;
	for(const auto & panel : {marketWidget->bidTradePanel, marketWidget->offerTradePanel})
		if(panel)
			for(const auto & slot : panel->slots)
				if(slot && slot->id >= 0)
					slots.push_back(slot);
	return slots;
}

std::vector<ArtifactPosition> CMarketWindow::thorMarketArtifacts(bool staged) const
{
	std::vector<ArtifactPosition> positions;
	const auto * hero = marketWidget->hero;
	const auto * storage = marketWidget->market->getArtifactsStorage();
	if(!hero || (staged && !storage))
		return positions;
	const auto append = [&](ArtifactPosition position) {
		if(const auto * artifact = (staged ? storage : static_cast<const CArtifactSet *>(hero))->getArt(position);
			artifact && artifact->getType()->isTradable()
			&& (!storage || artifact->canBePutAt(staged ? static_cast<const CArtifactSet *>(hero) : storage)))
			positions.push_back(position);
	};
	if(!staged)
		for(const auto & entry : hero->artifactsWorn)
			if(entry.first != ArtifactPosition::TRANSITION_POS)
				append(entry.first);
	const auto & backpack = staged ? storage->artifactsInBackpack : hero->artifactsInBackpack;
	for(std::size_t index = 0; index < std::min<std::size_t>(backpack.size(), 512); ++index)
		append(ArtifactPosition(ArtifactPosition::BACKPACK_START + index));
	return positions;
}

void CMarketWindow::publishThorSpecialMarket()
{
	auto * player = GAME->interface();
	if(!player || !isActive() || !ENGINE->windows().isTopWindow(this))
		return;
	const auto * market = marketWidget->market;
	const auto * live = dynamic_cast<const IMarket *>(player->cb->tryGetObj(market->getObjInstanceID()));
	const auto * hero = marketWidget->hero;
	const auto * town = dynamic_cast<const CGTownInstance *>(live);
	const auto altar = std::dynamic_pointer_cast<CExperienceAltar>(marketWidget);
	const auto artifactAltar = std::dynamic_pointer_cast<CAltarArtifacts>(marketWidget);
	const auto artifactSelling = std::dynamic_pointer_cast<CArtifactsSelling>(marketWidget);
	const auto slider = std::dynamic_pointer_cast<CMarketSlider>(marketWidget);
	const auto slots = thorMarketSlots();
	if(live != market || !live->allowsTrade(thorMarketMode))
	{
		publishThorEncounterContext();
		return;
	}
	const bool ready = player->makingTurn
		&& !settings["session"]["spectate"].Bool() && thorPendingTrade < 0 && thorMarketArtifactRequest < 0
		&& (!town || town->getOwner() == player->playerID)
		&& (!hero || (player->cb->tryGetObj(hero->id) == hero && hero->getOwner() == player->playerID));
	const bool holdingArtifact = artifactAltar && hero->getArt(ArtifactPosition::TRANSITION_POS);
	auto summary = getMarketTitle(market->getObjInstanceID(), thorMarketMode);
	if(const auto trader = std::dynamic_pointer_cast<CMarketTraderText>(marketWidget);
		trader && trader->traderText && trader->traderText->label)
		summary += "\n" + trader->traderText->label->getText();
	if(slider)
		summary += "\n" + std::to_string(slider->offerSlider->getValue()) + " / " + std::to_string(slider->offerSlider->getAmount());
	if(altar && altar->expForHero)
		summary += "\n" + LIBRARY->generaltexth->allTexts[478] + " " + altar->expForHero->getText();
	const auto pages = thorEncounterPages(summary);
	if(pages.empty())
	{
		publishThorEncounterContext();
		return;
	}
	ThorContextRecord context;
	context.contextId = ThorContextIds::ENCOUNTER_CHOICE;
	context.nativeOwnerToken = this;
	context.nativeParentToken = ENGINE->windows().thorParentOfTopWindow(this);
	context.enabledActionMask = thorActionMask(ThorAction::WINDOW_CLOSE);
	if(thorPendingTrade >= 0 || thorMarketArtifactRequest >= 0)
		context.activeActionMask = thorActionMask(ThorAction::WINDOW_CONFIRM);
	thorEncounterText(context, pages, thorMarketTextPage);
	if(!thorBrowseMarketModes && ready && !holdingArtifact && marketWidget->deal && !marketWidget->deal->isBlocked() && !marketWidget->deal->isDisabled())
		context.enabledActionMask |= thorActionMask(ThorAction::WINDOW_CONFIRM);
	struct Row { int target; std::string label; bool enabled; bool selected; };
	std::vector<Row> rows;
	std::string fingerprint = std::to_string(static_cast<int>(thorMarketMode));
	for(std::size_t index = 0; index < slots.size(); ++index)
	{
		if(artifactAltar || thorBrowseMarketModes)
			break;
		const auto & slot = slots[index];
		std::string label;
		switch(slot->type)
		{
		case EType::RESOURCE: label = GameResID(slot->id).toResource()->getNameTranslated(); break;
		case EType::PLAYER: label = GAME->translator().translate("vcmi.capitalColors", slot->id); break;
		case EType::ARTIFACT:
		case EType::ARTIFACT_TYPE: label = ArtifactID(slot->id).toEntity(LIBRARY)->getNameTranslated(); break;
		case EType::CREATURE: label = CreatureID(slot->id).toCreature()->getNamePluralTranslated(); break;
		}
		if(slot->subtitle && !slot->subtitle->getText().empty())
			label += " · " + slot->subtitle->getText();
		std::replace(label.begin(), label.end(), '\n', ' ');
		const auto bid = marketWidget->bidTradePanel;
		const auto offer = marketWidget->offerTradePanel;
		const bool selected = (bid && bid->highlightedSlot == slot) || (offer && offer->highlightedSlot == slot);
		fingerprint += "/" + std::to_string(slot->id) + ":" + std::to_string(slot->serial) + (selected ? "*" : "");
		rows.push_back({static_cast<int>(index), std::move(label), ready && !holdingArtifact && bool(slot->clickPressedCallback), selected});
	}
	if((artifactAltar || artifactSelling) && live == market)
	{
		const auto * storage = market->getArtifactsStorage();
		for(const bool staged : {false, true})
		{
			if(staged && !artifactAltar)
				continue;
			for(const auto position : thorMarketArtifacts(staged))
			{
				const auto * artifact = (staged ? storage : static_cast<const CArtifactSet *>(hero))->getArt(position);
				const auto target = (staged ? 4000 : 2000) + position.getNum();
				fingerprint += "/" + std::to_string(target) + ":" + std::to_string(artifact->getId().getNum());
				const bool selected = staged || (artifactSelling && artifactSelling->thorSelectedArtifact() == position);
				rows.push_back({target, artifact->getType()->getNameTranslated(), ready && !holdingArtifact, selected});
			}
		}
	}
	if(!thorBrowseMarketModes && slider && slider->offerSlider->getAmount() > 0)
	{
		const auto amount = slider->offerSlider->getValue();
		const auto maximum = slider->offerSlider->getAmount();
		rows.push_back({1000, "−10", ready && amount > 0, false});
		rows.push_back({1001, "−1", ready && amount > 0, false});
		rows.push_back({1002, "+1", ready && amount < maximum, false});
		rows.push_back({1003, "+10", ready && amount < maximum, false});
		rows.push_back({1004, LIBRARY->generaltexth->allTexts[473], ready && amount < maximum, false});
	}
	if(altar && altar->sacrificeAllButton)
		rows.push_back({1005, LIBRARY->generaltexth->zelp[571].first,
			ready && !holdingArtifact && !altar->sacrificeAllButton->isBlocked()
				&& (artifactAltar || !altar->deal || altar->deal->isBlocked())
				&& (artifactAltar || hero->stacksCount() > 0), false});
	if(artifactAltar)
		rows.push_back({1006, LIBRARY->generaltexth->zelp[570].first,
			ready && !holdingArtifact && !hero->artifactsInBackpack.empty(), false});
	for(std::size_t index = 0; index < changeModeButtons.size(); ++index)
		rows.push_back({1100 + static_cast<int>(index), thorMarketModeNames[index],
			ready && !holdingArtifact && !changeModeButtons[index]->isBlocked(), false});
	const int count = static_cast<int>(rows.size());
	context.browserPageCount = std::max(1, (count + THOR_ENCOUNTER_PAGE_SIZE - 1) / THOR_ENCOUNTER_PAGE_SIZE);
	thorMarketPage = std::clamp(thorMarketPage, 0, context.browserPageCount - 1);
	context.browserPage = thorMarketPage;
	if(thorMarketPage > 0)
		context.enabledActionMask |= thorActionMask(ThorAction::LOAD_BROWSER_PREVIOUS_PAGE);
	if(thorMarketPage + 1 < context.browserPageCount)
		context.enabledActionMask |= thorActionMask(ThorAction::LOAD_BROWSER_NEXT_PAGE);
	for(int index = thorMarketPage * THOR_ENCOUNTER_PAGE_SIZE;
		index < std::min(count, (thorMarketPage + 1) * THOR_ENCOUNTER_PAGE_SIZE); ++index)
	{
		auto & row = rows[index];
		row.label = thorBoundedText(std::move(row.label));
		if(!thorEncounterLabelValid(row.label))
		{
			publishThorEncounterContext();
			return;
		}
		context.browserEntries.push_back({row.target, std::move(row.label), row.enabled, row.selected});
		context.browserNativeKeys.push_back(fingerprint);
		if(row.enabled)
			context.enabledActionMask |= thorActionMask(ThorAction::LOAD_BROWSER_SELECT);
	}
	publishThorEncounterContext(std::move(context));
}

bool CMarketWindow::executeThorSpecialMarket(const ThorActionRequest & request)
{
	publishThorSpecialMarket();
	const auto context = thorContextStore().snapshot();
	if(context.nativeOwnerToken != this || validateThorActionRequest(request, context) != ThorActionValidation::VALID)
		return false;
	if(request.action == ThorAction::WINDOW_CLOSE)
	{
		if(thorBrowseMarketModes)
		{
			thorBrowseMarketModes = false;
			thorMarketPage = 0;
			thorMarketTextPage = 0;
			refreshThorMarket();
			return true;
		}
		auto button = quitButton;
		return !button->isDisabled() && button->invokeThorCallback();
	}
	if(request.action == ThorAction::WINDOW_CONFIRM)
	{
		thorSpecialMarketRequest = true;
		thorTradeStatus = 0;
		thorPendingTrade = captureThorRequest([&] { marketWidget->makeDeal(); });
		if(thorPendingTrade < 0)
			thorTradeStatus = 2;
	}
	else if(request.action == ThorAction::LOAD_BROWSER_SELECT)
	{
		const int target = request.targetId;
		if(target < 1000)
		{
			const auto slots = thorMarketSlots();
			if(target >= slots.size())
				return false;
			slots[target]->clickPressedCallback(slots[target]);
		}
		else if(target <= 1004)
		{
			const auto slider = std::dynamic_pointer_cast<CMarketSlider>(marketWidget);
			constexpr int deltas[] = {-10, -1, 1, 10};
			const auto maximum = slider->offerSlider->getAmount();
			const auto amount = slider->offerSlider->getValue();
			slider->offerSlider->scrollTo(target == 1004 ? maximum
				: std::clamp<std::int64_t>(static_cast<std::int64_t>(amount) + deltas[target - 1000], 0, maximum));
		}
		else if(target == 1005 || target == 1006)
		{
			if(const auto altar = std::dynamic_pointer_cast<CAltarArtifacts>(marketWidget))
				thorMarketArtifactRequest = captureThorRequest([&] {
					if(target == 1006)
						altar->sacrificeBackpack();
					else
						altar->sacrificeAll();
				});
			else
				std::dynamic_pointer_cast<CExperienceAltar>(marketWidget)->sacrificeAll();
		}
		else if(target >= 2000)
		{
			const bool staged = target >= 4000;
			const auto position = ArtifactPosition(target - (staged ? 4000 : 2000));
			if(!vstd::contains(thorMarketArtifacts(staged), position))
				return false;
			if(const auto selling = std::dynamic_pointer_cast<CArtifactsSelling>(marketWidget))
			{
				if(!selling->selectThorArtifact(position))
					return false;
				publishThorSpecialMarket();
				return true;
			}
			const auto hero = marketWidget->hero->id;
			const auto altar = marketWidget->market->getObjInstanceID();
			const auto source = staged ? ArtifactLocation(altar, position) : ArtifactLocation(hero, position);
			const auto destination = staged ? ArtifactLocation(hero, ArtifactPosition::FIRST_AVAILABLE)
				: ArtifactLocation(altar, ArtifactPosition::ALTAR);
			thorMarketArtifactRequest = captureThorRequest([&] { GAME->interface()->cb->swapArtifacts(source, destination); });
		}
		else
		{
			auto button = changeModeButtons[target - 1100];
			thorBrowseMarketModes = false;
			thorMarketPage = 0;
			thorMarketTextPage = 0;
			const bool changed = !button->isDisabled() && button->invokeThorCallback();
			refreshThorMarket();
			return changed;
		}
	}
	else if(request.action == ThorAction::LOAD_BROWSER_PREVIOUS_PAGE)
		--thorMarketPage;
	else if(request.action == ThorAction::LOAD_BROWSER_NEXT_PAGE)
		++thorMarketPage;
	else if(request.action == ThorAction::WINDOW_PREVIOUS)
		--thorMarketTextPage;
	else if(request.action == ThorAction::WINDOW_NEXT)
		++thorMarketTextPage;
	else
		return false;
	publishThorSpecialMarket();
	return true;
}

bool CMarketWindow::openThorMarketModes(const ThorActionRequest & request)
{
	if(thorPendingTrade >= 0)
		return false;
	const auto context = thorContextStore().snapshot();
	if(context.nativeOwnerToken != this || context.contextId != ThorContextIds::RESOURCE_MARKET
		|| validateThorActionRequest(request, context) != ThorActionValidation::VALID)
		return false;
	thorBrowseMarketModes = true;
	publishThorSpecialMarket();
	return true;
}

void CMarketWindow::completeThorMarketArtifact(int requestId)
{
	if(thorMarketArtifactRequest == requestId)
	{
		thorMarketArtifactRequest = -1;
		refreshThorMarket(true);
	}
}
#endif
