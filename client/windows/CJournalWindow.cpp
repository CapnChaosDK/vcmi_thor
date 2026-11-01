/*
 * CJournalWindow.cpp, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 *
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 *
 */
#include "StdInc.h"
#include "CJournalWindow.h"
#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
#include "../thor/ThorReferenceSupport.h"
#include "../../lib/thor/ThorDialog.h"
#include "../gui/WindowHandler.h"
#endif

#include "../CPlayerInterface.h"
#include "../GameEngine.h"
#include "../GameInstance.h"
#include "../gui/Shortcut.h"
#include "render/Canvas.h"
#include "../render/Colors.h"
#include "../widgets/Buttons.h"
#include "../widgets/CComponent.h"
#include "../widgets/Slider.h"

#include "../../lib/GameLibrary.h"
#include "../../lib/callback/CCallback.h"
#include "../../lib/gameState/QuestInfo.h"
#include "../../lib/gameState/ScenarioEventJournalEntry.h"
#include "../../lib/texts/CGeneralTextHandler.h"

JournalLabel::JournalLabel(const Rect & position, const std::string & text)
	: CMultiLineLabel(position, FONT_SMALL, ETextAlignment::TOPLEFT, Colors::WHITE, text)
{
}

void JournalLabel::clickPressed(const Point & cursorPosition)
{
	callback();
}

void JournalLabel::showAll(Canvas & to)
{
	CMultiLineLabel::showAll(to);
}

JournalWindow::JournalWindow(EJournalMode mode)
	: CWindowObject(PLAYER_COLORED | BORDERED, ImagePath::builtin("questDialog"))
	, mode(mode)
{
	OBJECT_CONSTRUCTION;

	description = std::make_shared<CTextBox>("", Rect(205, DESCRIPTION_TOP, 385, DESCRIPTION_HEIGHT), CSlider::BROWN, FONT_MEDIUM, ETextAlignment::TOPLEFT, Colors::WHITE);
	ok = std::make_shared<CButton>(Point(539, 398), AnimationPath::builtin("IOKAY.DEF"), LIBRARY->generaltexth->zelp[445], std::bind(&JournalWindow::close, this), EShortcut::GLOBAL_RETURN);
	auto questsTab = std::make_shared<CToggleButton>(Point(193, 18), AnimationPath::builtin("settingsWindow/button190"), CButton::tooltip(), nullptr);
	auto eventsTab = std::make_shared<CToggleButton>(Point(411, 18), AnimationPath::builtin("settingsWindow/button190"), CButton::tooltip(), nullptr);
	questsTab->setTextOverlay(LIBRARY->generaltexth->translate("vcmi.adventureMap.journal.quests"), FONT_SMALL, Colors::YELLOW);
	eventsTab->setTextOverlay(LIBRARY->generaltexth->translate("vcmi.adventureMap.journal.events"), FONT_SMALL, Colors::YELLOW);

	questsTab->block(!GAME->interface()->hasDisplayableQuests());
	eventsTab->block(!GAME->interface()->hasScenarioEventJournalEntries());
#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
	thorQuestsTab = questsTab;
	thorEventsTab = eventsTab;
#endif

	journalTabs = std::make_shared<CToggleGroup>([this](int selectedMode)
	{
		if(selectedMode == static_cast<int>(this->mode))
			return;

		close();
		if(selectedMode == static_cast<int>(EJournalMode::QUESTS))
			GAME->interface()->showQuestLog();
		else
			GAME->interface()->showScenarioEventJournal();
	});
	journalTabs->addToggle(static_cast<int>(EJournalMode::QUESTS), questsTab);
	journalTabs->addToggle(static_cast<int>(EJournalMode::EVENTS), eventsTab);
	journalTabs->setSelected(static_cast<int>(mode));

	slider = std::make_shared<CSlider>(Point(166, 195), 191, std::bind(&JournalWindow::sliderMoved, this, _1), VISIBLE_ITEM_COUNT, 0, 0, Orientation::VERTICAL, CSlider::BROWN);
	slider->setPanningStep(32);
}

void JournalWindow::initializeItems()
{
	OBJECT_CONSTRUCTION;

	for(size_t i = 0; i < getItemCount(); ++i)
	{
		auto label = std::make_shared<JournalLabel>(Rect(13, 195, 149, 31), getItemText(i));
		label->callback = [this, i]()
		{
			selectItem(i);
			redraw();
		};
		labels.push_back(label);
	}

	const int firstVisible = std::max(0, static_cast<int>(getItemCount()) - VISIBLE_ITEM_COUNT);
	recreateItemList(firstVisible);
	if(getItemCount() != 0) selectItem(getItemCount() - 1);

	slider->setAmount(static_cast<int>(getItemCount()));
	if(getItemCount() > VISIBLE_ITEM_COUNT)
	{
		slider->block(false);
		slider->scrollTo(firstVisible);
	}
	else
	{
		slider->block(true);
		slider->scrollToMin();
	}
}

void JournalWindow::selectItem(size_t itemIndex)
{
#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
	thorPage = static_cast<int>(itemIndex) / ThorReference::PAGE_SIZE;
	thorTextPage = 0;
#endif
	selectedLabel = static_cast<int>(itemIndex);
	onItemSelected(itemIndex);
}

void JournalWindow::setContent(const std::string & text, std::vector<std::shared_ptr<CComponent>> components, int componentAreaHeight)
{
#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
	thorText = text;
	for(const auto & component : components)
		thorText += "\n" + component->getSubtitle() + "\n" + component->getDescription();
#endif
	if(description->slider)
		description->slider->scrollToMin();
	description->setText(text);
	componentsBox.reset();

	int descriptionHeight = DESCRIPTION_HEIGHT;
	if(!components.empty())
	{
		descriptionHeight -= componentAreaHeight;
		OBJECT_CONSTRUCTION;
		componentsBox = std::make_shared<CComponentBox>(std::move(components), Rect(205, DESCRIPTION_TOP + descriptionHeight + 15, 385, 115));
	}
	description->resize(Point(385, descriptionHeight));
}

void JournalWindow::recreateItemList(int firstVisible)
{
	for(size_t i = 0; i < labels.size(); ++i)
	{
		labels[i]->pos = Rect(pos.x + 14, pos.y + 195 + (static_cast<int>(i) - firstVisible) * 32, 151, 31);
		if(static_cast<int>(i) >= firstVisible && static_cast<int>(i) < firstVisible + VISIBLE_ITEM_COUNT)
			labels[i]->enable();
		else
			labels[i]->disable();
	}
	updateMinimap();
}

void JournalWindow::sliderMoved(int newPosition)
{
	recreateItemList(newPosition);
	redraw();
}

void JournalWindow::showAll(Canvas & to)
{
	CWindowObject::showAll(to);
	if(selectedLabel < 0 || selectedLabel >= labels.size())
		return;

	// Keep the established quest-log rendering unchanged; event rows are clipped
	// when scrolled out because their previous positions may overlap the map view.
	if(mode == EJournalMode::EVENTS && labels[selectedLabel]->isDisabled())
		return;

	Rect selection = Rect::createAround(labels[selectedLabel]->pos, 1);
	selection.x -= 2;
	selection.w += 2;
	to.drawBorder(selection, Colors::METALLIC_GOLD);
}

#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
void JournalWindow::refreshThorReference(bool force)
{
	if(!isActive()) return;
	const auto stack = ENGINE->windows().findWindows<IShowActivatable>();
	if(!thorParent && stack.size() > 1) thorParent = stack[stack.size() - 2].get();
	if(!ENGINE->windows().isTopWindowWithParent(this, thorParent)) return;
	const auto now = std::chrono::steady_clock::now();
	if(!force && now < thorNextRefresh) return;
	thorNextRefresh = now + std::chrono::milliseconds(100);
	ThorReference panel;
	panel.kind = mode == EJournalMode::QUESTS ? 1 : 2;
	panel.flags = 8 | (!thorQuestsTab->isBlocked() ? 1 : 0)
		| (!thorEventsTab->isBlocked() ? 2 : 0);
	if(getItemCount() <= ThorReference::MAX_ITEMS)
	{
		panel.pages = (static_cast<int>(getItemCount()) + ThorReference::PAGE_SIZE - 1) / ThorReference::PAGE_SIZE;
		thorPage = std::clamp(thorPage, 0, std::max(0, panel.pages - 1));
		panel.page = thorPage;
		panel.selected = selectedLabel;
		for(int i = thorPage * ThorReference::PAGE_SIZE; i < std::min(static_cast<int>(getItemCount()), (thorPage + 1) * ThorReference::PAGE_SIZE); ++i)
		{
			panel.targets.push_back(i);
			panel.labels.push_back(thorBoundedText(getItemText(i), 128));
		}
		const auto pages = thorDialogPages(thorText);
		panel.textPages = static_cast<int>(pages.size());
		thorTextPage = std::clamp(thorTextPage, 0, std::max(0, panel.textPages - 1));
		panel.textPage = thorTextPage;
		if(!pages.empty()) for(const auto & part : pages[thorTextPage]) panel.text += part;
		if(selectedLabel >= 0 && thorCanCenter()) panel.flags |= 4;
	}
	else panel.pages = -1; // Unsupported lists must not masquerade as empty.
	publishThorReference(this, thorParent, std::move(panel), thorEpoch);
}

bool JournalWindow::executeThorReference(const ThorActionRequest & request)
{
	refreshThorReference(true);
	const auto state = thorContextStore().snapshot();
	if(!isActive() || !ENGINE->windows().isTopWindowWithParent(this, thorParent)
		|| state.nativeOwnerToken != this || state.nativeParentToken != thorParent
		|| request.revision <= thorConsumed || validateThorActionRequest(request, state) != ThorActionValidation::VALID) return false;
	thorConsumed = request.revision;
	++thorEpoch;
	switch(request.commandId)
	{
	case ThorExtended::REFERENCE_SELECT:
		selectItem(request.targetId);
		slider->scrollTo(std::max(0, std::min(request.targetId, static_cast<int>(getItemCount()) - VISIBLE_ITEM_COUNT)));
		redraw();
		break;
	case ThorExtended::REFERENCE_PREVIOUS: --thorPage; break;
	case ThorExtended::REFERENCE_NEXT: ++thorPage; break;
	case ThorExtended::REFERENCE_TEXT_PREVIOUS: --thorTextPage; break;
	case ThorExtended::REFERENCE_TEXT_NEXT: ++thorTextPage; break;
	case ThorExtended::REFERENCE_QUESTS: journalTabs->setSelected(static_cast<int>(EJournalMode::QUESTS)); return true;
	case ThorExtended::REFERENCE_EVENTS: journalTabs->setSelected(static_cast<int>(EJournalMode::EVENTS)); return true;
	case ThorExtended::REFERENCE_CENTER: thorCenter(); break;
	case ThorExtended::REFERENCE_CLOSE: close(); return true;
	default: return false;
	}
	refreshThorReference(true);
	return true;
}
#endif
