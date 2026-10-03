/*
 * QuickRecruitmentWindow.cpp, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 *
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 *
 */
#include "StdInc.h"
#include "QuickRecruitmentWindow.h"
#include "../../lib/mapObjects/CGTownInstance.h"
#include "../CPlayerInterface.h"
#include "../widgets/Buttons.h"
#include "../widgets/CreatureCostBox.h"
#include "../widgets/Slider.h"
#include "../GameEngine.h"
#include "../GameInstance.h"
#include "../gui/Shortcut.h"
#include "../../lib/callback/CCallback.h"
#include "../../lib/ResourceSet.h"
#include "../../lib/CCreatureHandler.h"
#include "CreaturePurchaseCard.h"

#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
#include "ThorRecruitmentSupport.h"
#include "CCastleInterface.h"
#include "../gui/WindowHandler.h"
#include "../../lib/CAndroidVMHelper.h"
#include "../thor/ThorVisualAssetPublisher.h"
#endif


void QuickRecruitmentWindow::setButtons()
{
	setCancelButton();
	setBuyButton();
	setMaxButton();
}

void QuickRecruitmentWindow::setCancelButton()
{
	cancelButton = std::make_shared<CButton>(Point((pos.w / 2) + 48, 418), AnimationPath::builtin("ICN6432.DEF"), CButton::tooltip(), [&](){ close(); }, EShortcut::GLOBAL_CANCEL);
	cancelButton->setImageOrder(0, 1, 2, 3);
}

void QuickRecruitmentWindow::setBuyButton()
{
	buyButton = std::make_shared<CButton>(Point((pos.w / 2) - 32, 418), AnimationPath::builtin("IBY6432.DEF"), CButton::tooltip(), [&](){ purchaseUnits(); }, EShortcut::GLOBAL_ACCEPT);
	buyButton->setImageOrder(0, 1, 2, 3);
}

void QuickRecruitmentWindow::setMaxButton()
{
	maxButton = std::make_shared<CButton>(Point((pos.w/2)-112, 418), AnimationPath::builtin("IRCBTNS.DEF"), CButton::tooltip(), [&](){ maxAllCards(cards); }, EShortcut::RECRUITMENT_MAX);
	maxButton->setImageOrder(0, 1, 2, 3);
}

void QuickRecruitmentWindow::setCreaturePurchaseCards()
{
	int availableAmount = getAvailableCreatures();
	Point position = Point((pos.w - 100*availableAmount - 8*(availableAmount-1))/2,64);
	for (int i = 0; i < town->getTown()->creatures.size(); i++)
	{
		if(!town->getTown()->creatures.at(i).empty() && !town->creatures.at(i).second.empty() && town->creatures[i].first)
		{
			cards.push_back(std::make_shared<CreaturePurchaseCard>(town->creatures[i].second, position,
				town->creatures[i].first, i, this));
			position.x += 108;
		}
	}
	totalCost = std::make_shared<CreatureCostBox>(Rect((this->pos.w/2)-45, position.y+260, 97, 74), "");
}

void QuickRecruitmentWindow::initWindow(Rect startupPosition)
{
	pos.x = startupPosition.x + 238;
	pos.y = startupPosition.y + 45;
	pos.w = 332;
	pos.h = 461;
	int creaturesAmount = getAvailableCreatures();
	if(creaturesAmount > 3)
	{
		pos.w += 108 * (creaturesAmount - 3);
		pos.x -= 55 * (creaturesAmount - 3);
	}
	backgroundTexture = std::make_shared<CFilledTexture>(ImagePath::builtin("DIBOXBCK.pcx"), Rect(0, 0, pos.w, pos.h));
	costBackground = std::make_shared<CPicture>(ImagePath::builtin("QuickRecruitmentWindow/costBackground.png"), pos.w/2-113, 335);
}

void QuickRecruitmentWindow::maxAllCards(std::vector<std::shared_ptr<CreaturePurchaseCard> > cards)
{
	auto allAvailableResources = GAME->interface()->cb->getResourceAmount();
	for(auto i : std::views::reverse(cards))
	{
		si32 maxAmount = i->creatureOnTheCard->maxAmount(allAvailableResources);
		vstd::amin(maxAmount, i->maxAmount);

		i->slider->setAmount(maxAmount);

		if(i->slider->getValue() != maxAmount)
			i->slider->scrollTo(maxAmount);
		else
			i->sliderMoved(maxAmount);

		i->slider->scrollToMax();
		allAvailableResources -= (i->creatureOnTheCard->getFullRecruitCost() * maxAmount);
	}
	maxButton->block(allAvailableResources == GAME->interface()->cb->getResourceAmount());
}


void QuickRecruitmentWindow::purchaseUnits()
{
	int freeSlotsLeft = town->getUpperArmy()->getFreeSlots().size();

	for(auto selected : std::views::reverse(cards))
	{
		if(selected->slider->getValue() == 0)
			continue;

		int level = 0;
		int i = 0;
		for(auto c : town->getTown()->creatures)
		{
			for(auto c2 : c)
				if(c2 == selected->creatureOnTheCard->getId())
					level = i;
			i++;
		}

		CreatureID crid = selected->creatureOnTheCard->getId();
		SlotID dstslot = town->getUpperArmy()->getSlotFor(crid);

		if(town->getUpperArmy()->slotEmpty(dstslot))
		{
			if(freeSlotsLeft == 0)
				continue;
			freeSlotsLeft -= 1;
		}

		if(dstslot.validSlot())
			GAME->interface()->cb->recruitCreatures(town, town->getUpperArmy(), crid, selected->slider->getValue(), level);
	}
	close();
}

int QuickRecruitmentWindow::getAvailableCreatures()
{
	int creaturesAmount = 0;
	for (int i=0; i< town->getTown()->creatures.size(); i++)
		if(!town->getTown()->creatures.at(i).empty() && !town->creatures.at(i).second.empty() && town->creatures[i].first)
			creaturesAmount++;
	return creaturesAmount;
}

void QuickRecruitmentWindow::updateAllSliders()
{
#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
	if(thorRefreshingNativeState)
		return;
#endif
	auto allAvailableResources = GAME->interface()->cb->getResourceAmount();
	for(auto i : std::views::reverse(cards))
		allAvailableResources -= (i->creatureOnTheCard->getFullRecruitCost() * i->slider->getValue());
	for(auto i : cards)
	{
		si32 maxAmount = i->creatureOnTheCard->maxAmount(allAvailableResources);
		vstd::amin(maxAmount, i->maxAmount);
		if(maxAmount < 0)
			continue;
		if(i->slider->getValue() + maxAmount < i->maxAmount)
			i->slider->setAmount(i->slider->getValue() + maxAmount);
		else
			i->slider->setAmount(i->maxAmount);
		i->slider->scrollTo(i->slider->getValue());
	}
	totalCost->createItems(GAME->interface()->cb->getResourceAmount() - allAvailableResources);
	totalCost->set(GAME->interface()->cb->getResourceAmount() - allAvailableResources);
}

QuickRecruitmentWindow::QuickRecruitmentWindow(const CGTownInstance * townd, Rect startupPosition,
	bool thorTownRecruitmentSource)
	: CWindowObject(PLAYER_COLORED | BORDERED),
	town(townd)
#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
	, thorStartupPosition(startupPosition),
	thorTownRecruitmentSource(thorTownRecruitmentSource)
#endif
{
	OBJECT_CONSTRUCTION;
#if !defined(VCMI_ANDROID) || !defined(TARGET_AYN_THOR)
	(void)thorTownRecruitmentSource;
#endif

	initWindow(startupPosition);
	setButtons();
	setCreaturePurchaseCards();
	maxAllCards(cards);

	center();
}

#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
void QuickRecruitmentWindow::activate()
{
	if(isActive())
		return;
	CWindowObject::activate();
	refreshThorNativeState();
}

void QuickRecruitmentWindow::deactivate()
{
	if(!isActive())
		return;
	CWindowObject::deactivate();
	ThorContextRecord empty;
	empty = thorContextStore().publishNext(std::move(empty));
	CAndroidVMHelper().publishThorContext(empty.revision, empty.contextId, empty.title, empty.status);
}

void QuickRecruitmentWindow::rebuildThorCreaturePurchaseCards()
{
	struct SavedCard
	{
		int tierIndex;
		int creatureId;
		int selectedAmount;
	};
	std::vector<SavedCard> savedCards;
	savedCards.reserve(cards.size());
	for(const auto & card : cards)
		savedCards.push_back({card->tierIndex, card->creatureOnTheCard->getId().getNum(), card->slider->getValue()});
	const int previousSelectedTarget = selectedThorTarget;

	OBJECT_CONSTRUCTION;
	thorRefreshingNativeState = true;
	cards.clear();
	initWindow(thorStartupPosition);
	setButtons();
	setCreaturePurchaseCards();
	bool selectedTargetStillAvailable = false;
	for(const auto & card : cards)
	{
		const auto previous = std::find_if(savedCards.begin(), savedCards.end(), [&](const SavedCard & saved)
		{
			return saved.tierIndex == card->tierIndex;
		});
		if(previous == savedCards.end())
			continue;

		const auto & variants = town->creatures[static_cast<std::size_t>(card->tierIndex)].second;
		const auto previousVariant = std::find_if(variants.begin(), variants.end(), [&](const CreatureID & id)
		{
			return id.getNum() == previous->creatureId;
		});
		if(previousVariant != variants.end())
		{
			const int targetVariant = static_cast<int>(previousVariant - variants.begin());
			for(int attempt = 0; attempt < card->variantCount()
				&& card->currentVariantIndex() != targetVariant; ++attempt)
				card->switchVariant();
		}
		card->slider->scrollTo(std::min(previous->selectedAmount, card->slider->getAmount()));
		selectedTargetStillAvailable |= card->tierIndex == previousSelectedTarget;
	}
	selectedThorTarget = selectedTargetStillAvailable ? previousSelectedTarget : -1;
	thorRefreshingNativeState = false;
	center();
}

void QuickRecruitmentWindow::clampThorPlanToAvailableResources()
{
	if(!GAME || !GAME->interface() || !GAME->interface()->cb)
		return;
	thorRefreshingNativeState = true;
	auto availableResources = GAME->interface()->cb->getResourceAmount();
	for(auto & card : std::views::reverse(cards))
	{
		int maximum = std::max(0, card->creatureOnTheCard->maxAmount(availableResources));
		maximum = std::min(maximum, std::max(0, card->maxAmount));
		const int amount = std::min(card->slider->getValue(), maximum);
		card->slider->scrollTo(amount);
		availableResources -= card->creatureOnTheCard->getFullRecruitCost() * amount;
	}
	ResourceSet selectedCost;
	for(const auto & card : cards)
		selectedCost += card->creatureOnTheCard->getFullRecruitCost() * card->slider->getValue();
	const auto playerResources = GAME->interface()->cb->getResourceAmount();
	for(const auto & card : cards)
	{
		const auto rowCost = card->creatureOnTheCard->getFullRecruitCost() * card->slider->getValue();
		const auto resourcesAfterOtherRows = playerResources - (selectedCost - rowCost);
		int maximum = std::max(0, card->creatureOnTheCard->maxAmount(resourcesAfterOtherRows));
		maximum = std::min(maximum, std::max(0, card->maxAmount));
		maximum = std::max(maximum, card->slider->getValue());
		card->slider->setAmount(maximum);
	}
	thorRefreshingNativeState = false;
	totalCost->createItems(selectedCost);
	totalCost->set(selectedCost);
}

bool QuickRecruitmentWindow::hasOwningTownWindow() const
{
	const auto castleWindows = ENGINE->windows().findWindows<CCastleInterface>();
	return std::any_of(castleWindows.begin(), castleWindows.end(),
		[&](const auto & window) { return window && window->town == town; });
}

void QuickRecruitmentWindow::refreshThorNativeState()
{
	if(!isActive() || ENGINE->windows().topWindow<QuickRecruitmentWindow>().get() != this)
		return;
	if(!thorTownRecruitmentSource || !hasOwningTownWindow() || !GAME || !GAME->interface() || !GAME->interface()->cb)
	{
		ThorContextRecord empty;
		publishThorRecruitmentContext(std::move(empty));
		return;
	}

	const auto resources = GAME->interface()->cb->getResourceAmount();
	std::vector<int> stocks;
	stocks.reserve(town->creatures.size());
	for(const auto & tier : town->creatures)
		stocks.push_back(tier.first);
	std::vector<int> variants;
	variants.reserve(cards.size());
	for(const auto & card : cards)
		variants.push_back(card->creatureOnTheCard->getId().getNum());
	if(!lastThorResources || *lastThorResources != resources || lastThorStocks != stocks || lastThorVariants != variants)
	{
		std::vector<int> currentTiers;
		std::vector<int> refreshedTiers;
		currentTiers.reserve(cards.size());
		for(const auto & card : cards)
			currentTiers.push_back(card->tierIndex);
		for(std::size_t index = 0; index < town->creatures.size(); ++index)
			if(!town->getTown()->creatures.at(index).empty() && !town->creatures[index].second.empty()
				&& stocks[index] > 0)
				refreshedTiers.push_back(static_cast<int>(index));
		if(lastThorStocks != stocks && currentTiers != refreshedTiers)
			rebuildThorCreaturePurchaseCards();
		else
		{
			thorRefreshingNativeState = true;
			for(auto & card : cards)
			{
				const int stock = card->tierIndex >= 0 && static_cast<std::size_t>(card->tierIndex) < stocks.size()
					? std::max(0, stocks[static_cast<std::size_t>(card->tierIndex)]) : 0;
				card->maxAmount = stock;
				card->slider->setAmount(stock);
				card->slider->scrollTo(std::min(card->slider->getValue(), stock));
			}
			thorRefreshingNativeState = false;
		}
		lastThorResources = resources;
		lastThorStocks = std::move(stocks);
		lastThorVariants.clear();
		lastThorVariants.reserve(cards.size());
		for(const auto & card : cards)
			lastThorVariants.push_back(card->creatureOnTheCard->getId().getNum());
	}
	clampThorPlanToAvailableResources();
	publishThorContext();
}

void QuickRecruitmentWindow::publishThorContext()
{
	ThorContextRecord context;
	context.contextId = ThorContextIds::TOWN_RECRUITMENT_QUICK;
	context.title = GAME->translator().translate(town->getNameTextID());
	context.actionSubjectId = town->id.getNum();
	context.actionEpoch = thorActionEpoch;
	context.enabledActionMask = thorActionMask(ThorAction::WINDOW_CLOSE);
	ThorRecruitmentSnapshot recruitment;
	recruitment.mode = ThorRecruitmentMode::QUICK_TOWN;
	recruitment.townId = town->id.getNum();
	recruitment.destinationArmyId = town->getUpperArmy()->id.getNum();
	recruitment.destinationArmyFreeSlots = static_cast<int>(town->getUpperArmy()->getFreeSlots().size());
	recruitment.dwellingLevel = -1;
	recruitment.selectedTarget = selectedThorTarget;
	recruitment.townName = context.title;
	recruitment.locallyControllable = GAME->interface()->playerID == town->tempOwner && GAME->interface()->makingTurn;
	const auto army = town->getUpperArmy();
	int freeSlotsLeft = recruitment.destinationArmyFreeSlots;
	std::vector<bool> armyAvailable(cards.size(), false);
	bool canBuyCurrentPlan = false;
	for(std::size_t reverseIndex = cards.size(); reverseIndex > 0; --reverseIndex)
	{
		const std::size_t index = reverseIndex - 1;
		const auto & card = cards[index];
		const auto destinationSlot = army->getSlotFor(card->creatureOnTheCard->getId());
		if(!destinationSlot.validSlot())
			continue;
		if(!army->slotEmpty(destinationSlot))
			armyAvailable[index] = true;
		else if(freeSlotsLeft > 0)
		{
			armyAvailable[index] = true;
			if(card->slider->getValue() > 0)
				--freeSlotsLeft;
		}
		if(card->slider->getValue() <= 0)
			continue;
		canBuyCurrentPlan |= armyAvailable[index];
	}
	ResourceSet totalCost;
	for(std::size_t cardIndex = 0; cardIndex < cards.size(); ++cardIndex)
	{
		const auto & card = cards[cardIndex];
		ThorRecruitmentRow row;
		row.target = card->tierIndex;
		row.creatureId = card->creatureOnTheCard->getId().getNum();
		row.name = card->creatureOnTheCard->getNameSingularTranslated();
		row.availableCount = std::max(0, card->maxAmount);
		row.selectedAmount = std::clamp(card->slider->getValue(), 0, std::max(0, card->slider->getAmount()));
		row.maximumAmount = std::clamp(card->slider->getAmount(), 0, row.availableCount);
		row.variantIndex = card->currentVariantIndex();
		row.variantCount = card->variantCount();
		row.selected = row.target == selectedThorTarget;
		row.armyAvailable = armyAvailable[cardIndex];
		row.enabled = recruitment.locallyControllable && row.availableCount > 0 && row.armyAvailable;
		row.unitCost = formatThorRecruitmentCost(card->creatureOnTheCard->getFullRecruitCost());
		const auto selectionCost = card->creatureOnTheCard->getFullRecruitCost() * row.selectedAmount;
		row.selectedCost = formatThorRecruitmentCost(selectionCost);
		row.visualAssetKey = thorCreatureVisualAssetKey(row.creatureId);
		totalCost += selectionCost;
		recruitment.rows.push_back(std::move(row));
	}
	recruitment.canBuy = recruitment.locallyControllable && canBuyCurrentPlan
		&& GAME->interface()->cb->getResourceAmount().canAfford(totalCost);
	recruitment.totalCost = formatThorRecruitmentCost(totalCost);
	context.recruitment = std::move(recruitment);
	if(context.recruitment->locallyControllable)
	{
		context.enabledActionMask |= thorActionMask(ThorAction::RECRUITMENT_EDIT);
		if(context.recruitment->canBuy)
			context.enabledActionMask |= thorActionMask(ThorAction::RECRUITMENT_BUY);
	}
	publishThorRecruitmentContext(std::move(context));
}

bool QuickRecruitmentWindow::matchesThorContext(const ThorContextRecord & context) const
{
	if(!thorTownRecruitmentSource || !hasOwningTownWindow())
		return false;
	const auto army = town->getUpperArmy();
	return thorRecruitmentOwnerMatches(context, ThorRecruitmentMode::QUICK_TOWN, town->id.getNum(), -1,
		army->id.getNum(), static_cast<int>(army->getFreeSlots().size()), isActive(),
		ENGINE->windows().topWindow<QuickRecruitmentWindow>().get() == this);
}

bool QuickRecruitmentWindow::executeThorAction(const ThorActionRequest & request)
{
	const auto before = thorContextStore().snapshot();
	refreshThorNativeState();
	const auto current = thorContextStore().snapshot();
	if(current.revision != before.revision || !matchesThorContext(current)
		|| validateThorActionRequest(request, current) != ThorActionValidation::VALID)
		return false;
	if(request.action == ThorAction::WINDOW_CLOSE)
	{
		close();
		return true;
	}
	if(request.action == ThorAction::RECRUITMENT_BUY)
	{
		if(!current.recruitment->locallyControllable || !current.recruitment->canBuy)
			return false;
		purchaseUnits();
		return true;
	}
	if(request.action != ThorAction::RECRUITMENT_EDIT)
		return false;
	if(thorActionEpoch == std::numeric_limits<std::uint64_t>::max())
		return false;
	auto card = std::find_if(cards.begin(), cards.end(), [&](const auto & value)
	{
		return value && value->tierIndex == request.targetId;
	});
	if(card == cards.end() || !current.recruitment->locallyControllable)
		return false;
	if(request.recruitmentOperation == ThorRecruitmentOperation::CYCLE_VARIANT)
	{
		if((*card)->variantCount() <= 1 || (*card)->currentVariantIndex() < 0
			|| (*card)->currentVariantIndex() >= (*card)->variantCount())
			return false;
		(*card)->switchVariant();
	}
	else if(request.recruitmentOperation != ThorRecruitmentOperation::SELECT_ROW)
	{
		const auto amount = thorRecruitmentAmountAfter(request.recruitmentOperation,
			(*card)->slider->getValue(), (*card)->slider->getAmount());
		if(!amount)
			return false;
		(*card)->slider->scrollTo(*amount);
	}
	selectedThorTarget = request.targetId;
	++thorActionEpoch;
	refreshThorNativeState();
	return true;
}
#endif
