/*
 * SettingsMainContainer.cpp, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 *
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 *
 */

#include "StdInc.h"
#include "SettingsMainWindow.h"

#include "AdventureOptionsTab.h"
#include "BattleOptionsTab.h"
#include "GeneralOptionsTab.h"
#include "OtherOptionsTab.h"

#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
#include "../../../lib/CAndroidVMHelper.h"
#include "../../widgets/Slider.h"
#include "../../widgets/TextControls.h"
#endif

#include "CMT.h"
#include "../../../lib/texts/CGeneralTextHandler.h"
#include "../../../lib/GameLibrary.h"
#include "CPlayerInterface.h"
#include "CServerHandler.h"
#include "../../../lib/filesystem/ResourcePath.h"
#include "../../GameEngine.h"
#include "../../GameInstance.h"
#include "gui/WindowHandler.h"
#include "render/Canvas.h"
#include "lobby/CSavingScreen.h"
#include "widgets/Buttons.h"
#include "widgets/Images.h"
#include "widgets/ObjectLists.h"
#include "windows/CMessage.h"

SettingsMainWindow::SettingsMainWindow(BattleInterface * parentBattleUi) : InterfaceObjectConfigurable()
{
	OBJECT_CONSTRUCTION;

	const JsonNode config(JsonPath::builtin("config/widgets/settings/settingsMainContainer.json"));
	addCallback("activateSettingsTab", [this](int tabId) { openTab(tabId); });
	addCallback("loadGame", [this](int) { loadGameButtonCallback(); });
	addCallback("saveGame", [this](int) { saveGameButtonCallback(); });
	addCallback("restartGame", [this](int) { restartGameButtonCallback(); });
	addCallback("quitGame", [this](int) { quitGameButtonCallback(); });
	addCallback("returnToMainMenu", [this](int) { mainMenuButtonCallback(); });
	addCallback("closeWindow", [this](int) { backButtonCallback(); });
	build(config);

	addUsedEvents(INPUT_MODE_CHANGE);

	std::shared_ptr<CIntObject> background = widget<CIntObject>("background");
	pos.w = background->pos.w;
	pos.h = background->pos.h;
	pos = center();

	std::shared_ptr<CButton> loadButton = widget<CButton>("loadButton");
	assert(loadButton);

	std::shared_ptr<CButton> saveButton = widget<CButton>("saveButton");
	assert(saveButton);

	std::shared_ptr<CButton> restartButton = widget<CButton>("restartButton");
	assert(restartButton);

	loadButton->block(GAME->server().isGuest());
	saveButton->block(GAME->server().isGuest() || parentBattleUi);
	restartButton->block(GAME->server().isGuest());

	int defaultTabIndex = 0;
	if(parentBattleUi != nullptr)
		defaultTabIndex = 2;
	else if(settings["general"]["lastSettingsTab"].isNumber())
		defaultTabIndex = settings["general"]["lastSettingsTab"].Integer();

	parentBattleInterface = parentBattleUi;
	tabContentArea = std::make_shared<CTabbedInt>(std::bind(&SettingsMainWindow::createTab, this, _1), Point(0, 0), defaultTabIndex);
	tabContentArea->setRedrawParent(true);

	std::shared_ptr<CToggleGroup> mainTabs = widget<CToggleGroup>("settingsTabs");
	mainTabs->setSelected(defaultTabIndex);
	
	GAME->interface()->gamePause(true);
}

std::shared_ptr<CIntObject> SettingsMainWindow::createTab(size_t index)
{
	switch(index)
	{
		case 0:
			return std::make_shared<GeneralOptionsTab>();
		case 1:
			return std::make_shared<AdventureOptionsTab>();
		case 2:
			return std::make_shared<BattleOptionsTab>(parentBattleInterface);
		case 3:
			return std::make_shared<OtherOptionsTab>();
		default:
			logGlobal->error("Wrong settings tab ID!");
			return std::make_shared<GeneralOptionsTab>();
	}
}

void SettingsMainWindow::openTab(size_t index)
{
	tabContentArea->setActive(index);
	CIntObject::redraw();

	Settings lastUsedTab = settings.write["general"]["lastSettingsTab"];
	lastUsedTab->Integer() = index;
#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
	thorPage = 0;
	publishThorContext();
#endif
}

void SettingsMainWindow::close()
{
	if(!ENGINE->windows().isTopWindow(this))
		logGlobal->error("Only top interface must be closed");
	
	GAME->interface()->gamePause(false);
	ENGINE->windows().popWindows(1);
}

void SettingsMainWindow::quitGameButtonCallback()
{
	GAME->interface()->showYesNoDialog(
		LIBRARY->generaltexth->allTexts[578],
		[this]()
		{
			close();
			ENGINE->user().onShutdownRequested(false);
		},
		nullptr
	);
}

void SettingsMainWindow::backButtonCallback()
{
	close();
}

void SettingsMainWindow::mainMenuButtonCallback()
{
	GAME->interface()->showYesNoDialog(
		LIBRARY->generaltexth->allTexts[578],
		[this]()
		{
			close();
			GAME->server().endGameplay();
			GAME->mainmenu()->menu->switchToTab("main");
		},
		0
	);
}

void SettingsMainWindow::loadGameButtonCallback()
{
	close();
	GAME->interface()->proposeLoadingGame();
}

void SettingsMainWindow::saveGameButtonCallback()
{
	close();
	ENGINE->windows().createAndPushWindow<CSavingScreen>();
}

void SettingsMainWindow::restartGameButtonCallback()
{
	GAME->interface()->showYesNoDialog(
		LIBRARY->generaltexth->allTexts[67],
		[this]()
		{
			close();
			ENGINE->dispatchMainThread([](){
				GAME->server().sendRestartGame();
			});
		},
		0
	);
}

void SettingsMainWindow::showAll(Canvas & to)
{
#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
	publishThorContext();
#endif
	auto color = GAME->interface() ? GAME->interface()->playerID : PlayerColor(1);
	if(settings["session"]["spectate"].Bool())
		color = PlayerColor(1); // TODO: Spectator shouldn't need special code for UI colors

	CIntObject::showAll(to);
	CMessage::drawBorder(color, to, pos.w+28, pos.h+29, pos.x-14, pos.y-15);
}

void SettingsMainWindow::onScreenResize()
{
	InterfaceObjectConfigurable::onScreenResize();

	auto tab = std::dynamic_pointer_cast<GeneralOptionsTab>(tabContentArea->getItem());

	if (tab)
		tab->updateResolutionSelector();
}

#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
namespace
{
	constexpr int thorSettingsPageSize = 5;
	constexpr std::size_t thorSettingsMaximumControls = 10000;

	bool settingsControlEnabled(const CIntObject & object)
	{
		for(auto * current = &object; current; current = current->parent)
			if(current->isDisabled())
				return false;
		return true;
	}

	void collectSettingsLabels(CIntObject & object, std::vector<CLabel *> & labels)
	{
		if(auto * label = dynamic_cast<CLabel *>(&object))
			if(!label->getText().empty())
				labels.push_back(label);
		for(auto * child : object.children)
			collectSettingsLabels(*child, labels);
	}

	const JsonNode * findSettingsConfig(const JsonNode & node, const std::string & name)
	{
		if(node.isStruct())
		{
			if(node["name"].String() == name)
				return &node;
			for(const auto & item : node.Struct())
				if(const auto * found = findSettingsConfig(item.second, name))
					return found;
		}
		else if(node.isVector())
			for(const auto & item : node.Vector())
				if(const auto * found = findSettingsConfig(item, name))
					return found;
		return nullptr;
	}
}

void SettingsMainWindow::collectThorControls(InterfaceObjectConfigurable & source, const std::string & configPath)
{
	if(!thorControlsValid || source.widgets.size() > thorSettingsMaximumControls)
	{
		thorControlsValid = false;
		return;
	}
	auto cached = thorConfigs.find(configPath);
	if(cached == thorConfigs.end())
		cached = thorConfigs.emplace(configPath, JsonNode(JsonPath::builtin(configPath))).first;
	const JsonNode & config = cached->second;
	std::vector<CLabel *> labels;
	collectSettingsLabels(source, labels);
	std::vector<std::pair<std::string, std::shared_ptr<CIntObject>>> controls;
	for(const auto & entry : source.widgets)
		if(entry.second && (dynamic_cast<CButton *>(entry.second.get())
			|| dynamic_cast<CToggleGroup *>(entry.second.get())
			|| dynamic_cast<CSlider *>(entry.second.get())))
			controls.push_back(entry);
	std::stable_sort(controls.begin(), controls.end(), [](const auto & lhs, const auto & rhs)
	{
		return std::tie(lhs.second->pos.y, lhs.second->pos.x, lhs.first)
			< std::tie(rhs.second->pos.y, rhs.second->pos.x, rhs.first);
	});
	for(const auto & [name, object] : controls)
	{
		if(thorControls.size() >= thorSettingsMaximumControls || name.size() > 256)
		{
			thorControlsValid = false;
			return;
		}
		std::string label;
		if(const auto * entry = findSettingsConfig(config, name))
			label = source.readHintText((*entry)["help"]).first;
		// Several upstream settings intentionally have no tooltip. Reuse their
		// visible label, in the same column and closest row, rather than an ID.
		if(label.empty())
		{
			int best = std::numeric_limits<int>::max();
			for(auto * candidate : labels)
			{
				const int dx = candidate->pos.x - object->pos.x;
				const int dy = candidate->pos.y - object->pos.y;
				if(std::abs(dx) > 220 || std::abs(dy) > 45)
					continue;
				const int distance = std::abs(dy) * 10 + std::abs(dx);
				if(distance < best)
				{
					best = distance;
					label = candidate->getText();
				}
			}
		}
		if(name == "musicSlider")
			label = LIBRARY->generaltexth->translate("core.genrltxt.394");
		else if(name == "soundVolumeSlider")
			label = LIBRARY->generaltexth->translate("core.genrltxt.395");
		if(auto group = std::dynamic_pointer_cast<CToggleGroup>(object))
		{
			for(const auto & [value, base] : group->buttons)
			{
				if(thorControls.size() >= thorSettingsMaximumControls)
				{
					thorControlsValid = false;
					return;
				}
				auto button = std::dynamic_pointer_cast<CToggleButton>(base);
				if(!button)
					continue;
				std::vector<CLabel *> choiceLabels;
				collectSettingsLabels(*button, choiceLabels);
				std::string choice = choiceLabels.empty() ? std::string() : choiceLabels.front()->getText();
				if(choice.empty())
					if(const auto * groupConfig = findSettingsConfig(config, name))
					{
						int optionIndex = -1;
						for(const auto & option : (*groupConfig)["items"].Vector())
						{
							optionIndex = option["index"].isNull() ? optionIndex + 1 : option["index"].Integer();
							if(optionIndex == value)
								choice = source.readHintText(option["help"]).first;
						}
					}
				if(choice.empty())
					choice = std::to_string(value);
				const std::string rowLabel = name == "settingsTabs" ? choice : label + ": " + choice;
				thorControls.push_back({configPath + ":" + name + ":" + std::to_string(value),
					rowLabel,
					!rowLabel.empty() && settingsControlEnabled(*object) && settingsControlEnabled(*button) && !button->isBlocked(),
					group->getSelected() == value,
					[group, button, value]()
					{
						if(!settingsControlEnabled(*group) || !settingsControlEnabled(*button) || button->isBlocked())
							return false;
						group->setSelected(value);
						return true;
					}});
			}
		}
		else if(auto slider = std::dynamic_pointer_cast<CSlider>(object))
		{
			for(const int delta : {-1, 1})
			{
				// scrollBy changes one native slider position, including nonlinear
				// sliders; getValue remains the native displayed/settings value.
				const int position = slider->CSlider::getValue();
				const bool within = delta < 0 ? position > 0
					: position < slider->getAmount() - slider->getCapacity();
				const bool blocked = std::any_of(slider->children.begin(), slider->children.end(), [](const auto * child)
				{
					auto * button = dynamic_cast<CButton *>(const_cast<CIntObject *>(child));
					return button && button->isBlocked();
				});
				const bool enabled = settingsControlEnabled(*slider) && !blocked && within;
				thorControls.push_back({configPath + ":" + name + ":" + std::to_string(delta),
					label + ": " + std::to_string(slider->getValue()) + (delta < 0 ? "  −" : "  +"),
					enabled, false, [slider, delta]()
					{
						if(!settingsControlEnabled(*slider))
							return false;
						slider->scrollBy(delta);
						return true;
					}});
			}
		}
		else if(auto button = std::dynamic_pointer_cast<CButton>(object))
		{
			auto toggle = std::dynamic_pointer_cast<CToggleButton>(button);
			thorControls.push_back({configPath + ":" + name, label,
				!label.empty() && settingsControlEnabled(*button) && !button->isBlocked(), toggle && toggle->isSelected(),
				[button, toggle]()
				{
					if(!settingsControlEnabled(*button) || button->isBlocked())
						return false;
					if(toggle)
					{
						toggle->setSelected(!toggle->isSelected());
						return true;
					}
					return button->invokeThorCallback();
				}});
		}
	}
}

void SettingsMainWindow::activate()
{
	InterfaceObjectConfigurable::activate();
	publishThorContext();
}

void SettingsMainWindow::deactivate()
{
	InterfaceObjectConfigurable::deactivate();
	thorControls.clear();
	thorActionQueue().clear();
	ThorContextRecord context;
	context = thorContextStore().publishNext(std::move(context));
	CAndroidVMHelper().publishThorContext(context.revision, context.contextId, context.title, context.status);
	CAndroidVMHelper().publishThorActionState(context.revision, 0, 0);
}

void SettingsMainWindow::publishThorContext()
{
	if(!isActive() || !ENGINE->windows().isTopWindow(this))
		return;
	thorControls.clear();
	thorControlsValid = true;
	collectThorControls(*this, "config/widgets/settings/settingsMainContainer.json");
	static const std::array<std::string, 4> tabConfigs = {
		"config/widgets/settings/generalOptionsTab.json", "config/widgets/settings/adventureOptionsTab.json",
		"config/widgets/settings/battleOptionsTab.json", "config/widgets/settings/otherOptionsTab.json"};
	auto tab = std::dynamic_pointer_cast<InterfaceObjectConfigurable>(tabContentArea->getItem());
	if(tab && tabContentArea->getActive() < tabConfigs.size())
		collectThorControls(*tab, tabConfigs[tabContentArea->getActive()]);
	ThorContextRecord context;
	if(!thorControlsValid || thorControls.size() > thorSettingsMaximumControls)
	{
		thorControls.clear();
		thorActionQueue().clear();
		context = thorContextStore().publishNext(std::move(context));
		CAndroidVMHelper().publishThorContext(context.revision, context.contextId, context.title, context.status);
		CAndroidVMHelper().publishThorActionState(context.revision, 0, 0);
		return;
	}
	context.contextId = ThorContextIds::SETTINGS;
	static const std::array<std::string, 4> tabTitles = {"generalTab", "adventureTab", "battleTab", "generalTab"};
	context.title = LIBRARY->generaltexth->translate("vcmi.settingsMainWindow."
		+ tabTitles.at(std::min(tabContentArea->getActive(), tabTitles.size() - 1)) + ".hover");
	context.nativeOwnerToken = this;
	context.actionEpoch = thorEpoch;
	context.browserPageCount = std::max(1, static_cast<int>((thorControls.size() + thorSettingsPageSize - 1) / thorSettingsPageSize));
	thorPage = std::clamp(thorPage, 0, context.browserPageCount - 1);
	context.browserPage = thorPage;
	const auto back = widget<CButton>("closeSettingsButton");
	if(back && settingsControlEnabled(*back) && !back->isBlocked())
		context.enabledActionMask = thorActionMask(ThorAction::WINDOW_CLOSE);
	if(thorPage > 0)
		context.enabledActionMask |= thorActionMask(ThorAction::LOAD_BROWSER_PREVIOUS_PAGE);
	if(thorPage + 1 < context.browserPageCount)
		context.enabledActionMask |= thorActionMask(ThorAction::LOAD_BROWSER_NEXT_PAGE);
	std::vector<std::string> fullState;
	for(std::size_t i = 0; i < thorControls.size(); ++i)
	{
		auto & control = thorControls[i];
		if(control.label.size() > 128)
		{
			control.label = thorBoundedText(std::move(control.label), 128);
			control.enabled = false;
		}
		fullState.push_back(control.key + ":" + control.label
			+ (control.enabled ? ":1" : ":0") + (control.selected ? ":1" : ":0"));
		if(i / thorSettingsPageSize != static_cast<std::size_t>(thorPage))
			continue;
		context.browserEntries.push_back({static_cast<int>(i), control.label, control.enabled, control.selected});
		context.browserNativeKeys.push_back(control.key);
		if(control.enabled)
			context.enabledActionMask |= thorActionMask(ThorAction::LOAD_BROWSER_SELECT);
	}
	if(fullState != thorLastState)
	{
		thorLastState = std::move(fullState);
		++thorStateRevision;
	}
	context.scenarioSelectionRevision = thorStateRevision;
	const auto previous = thorContextStore().snapshot().revision;
	context = thorContextStore().publishNext(std::move(context));
	if(context.revision == previous)
		return;
	CAndroidVMHelper bridge;
	bridge.publishThorContext(context.revision, context.contextId, context.title, context.status);
	bridge.publishThorActionState(context.revision, context.enabledActionMask, 0);
	bridge.publishThorBrowser(context.revision, context.browserPage, context.browserPageCount, context.browserEntries);
}

bool SettingsMainWindow::matchesThorContext(const ThorContextRecord & context) const
{
	return isActive() && ENGINE->windows().isTopWindow(const_cast<SettingsMainWindow *>(this))
		&& context.contextId == ThorContextIds::SETTINGS && context.nativeOwnerToken == this;
}

bool SettingsMainWindow::executeThorAction(const ThorActionRequest & request)
{
	publishThorContext();
	const auto context = thorContextStore().snapshot();
	if(!matchesThorContext(context) || validateThorActionRequest(request, context) != ThorActionValidation::VALID)
		return false;
	std::function<bool()> invoke;
	if(request.action == ThorAction::WINDOW_CLOSE)
		invoke = [back = widget<CButton>("closeSettingsButton")]()
		{ return back && settingsControlEnabled(*back) && back->invokeThorCallback(); };
	else if(request.action == ThorAction::LOAD_BROWSER_PREVIOUS_PAGE || request.action == ThorAction::LOAD_BROWSER_NEXT_PAGE)
		invoke = [this, action = request.action]()
		{
			thorPage += action == ThorAction::LOAD_BROWSER_NEXT_PAGE ? 1 : -1;
			return true;
		};
	else if(request.action == ThorAction::LOAD_BROWSER_SELECT)
	{
		const auto row = std::find_if(context.browserEntries.begin(), context.browserEntries.end(),
			[&request](const auto & entry) { return entry.target == request.targetId && entry.enabled; });
		if(row == context.browserEntries.end() || request.targetId < 0
			|| static_cast<std::size_t>(request.targetId) >= thorControls.size())
			return false;
		invoke = thorControls[request.targetId].invoke;
	}
	if(!invoke)
		return false;
	// Invoke from a local copy: opening a child deactivates this owner and
	// clears the registry. Never execute a std::function being destroyed.
	++thorEpoch;
	thorActionQueue().clear();
	const bool result = invoke();
	publishThorContext();
	return result;
}
#endif
