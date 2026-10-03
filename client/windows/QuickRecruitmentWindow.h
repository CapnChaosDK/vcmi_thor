/*
 * QuickRecruitmentWindow.h, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 *
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 *
 */
#pragma once

#include "CWindowObject.h"

#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
#include "../../lib/thor/ThorAction.h"
#include "../../lib/thor/ThorContext.h"
#include "../../lib/ResourceSet.h"
#endif

class CGTownInstance;

class CButton;
class CreatureCostBox;
class CreaturePurchaseCard;
class CFilledTexture;

class QuickRecruitmentWindow : public CWindowObject
{
public:
	int getAvailableCreatures();
	void updateAllSliders();
	QuickRecruitmentWindow(const CGTownInstance * townd, Rect startupPosition, bool thorTownRecruitmentSource = false);
	#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
	void activate() override;
	void deactivate() override;
	void rebuildThorCreaturePurchaseCards();
	void clampThorPlanToAvailableResources();
	void refreshThorNativeState();
	void publishThorContext();
	bool hasOwningTownWindow() const;
	bool matchesThorContext(const ThorContextRecord & context) const;
	bool executeThorAction(const ThorActionRequest & request);
	#endif

private:
	void initWindow(Rect startupPosition);

	void setButtons();
	void setCancelButton();
	void setBuyButton();
	void setMaxButton();

	void setCreaturePurchaseCards();

	void maxAllCards(std::vector<std::shared_ptr<CreaturePurchaseCard>> cards);
	void maxAllSlidersAmount(std::vector<std::shared_ptr<CreaturePurchaseCard>> cards);
	void purchaseUnits();

	const CGTownInstance * town;
	std::shared_ptr<CButton> maxButton;
	std::shared_ptr<CButton> buyButton;
	std::shared_ptr<CButton> cancelButton;
	std::shared_ptr<CreatureCostBox> totalCost;
	std::vector<std::shared_ptr<CreaturePurchaseCard>> cards;
	std::shared_ptr<CFilledTexture> backgroundTexture;
	std::shared_ptr<CPicture> costBackground;
#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
	Rect thorStartupPosition;
	bool thorTownRecruitmentSource = false;
	std::uint64_t thorActionEpoch = 0;
	int selectedThorTarget = -1;
	bool thorRefreshingNativeState = false;
	std::optional<ResourceSet> lastThorResources;
	std::vector<int> lastThorStocks;
	std::vector<int> lastThorVariants;
#endif
};
