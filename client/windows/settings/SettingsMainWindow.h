/*
 * SettingsMainContainer.h, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 *
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 *
 */

#pragma once

#include "gui/InterfaceObjectConfigurable.h"

#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
#include "../../../lib/thor/ThorAction.h"
#include <functional>
#include <map>
#include <vector>
#endif

class BattleInterface;
class CTabbedInt;
enum class EUserEvent;

/// <summary>
///Original H3 settings were replaced with this window in Github PR #1540. New options added are intended to be clientside settings.
///By design, most new options that alter gameplay UI from now on should be added to "gameTweaks" config key.
/// </summary>
class SettingsMainWindow : public InterfaceObjectConfigurable
{
#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
	struct ThorControl
	{
		std::string key;
		std::string label;
		bool enabled = false;
		bool selected = false;
		std::function<bool()> invoke;
	};
	std::vector<ThorControl> thorControls;
	bool thorControlsValid = true;
	std::map<std::string, JsonNode> thorConfigs;
	int thorPage = 0;
	std::uint64_t thorEpoch = 0;
	std::vector<std::string> thorLastState;
	std::uint64_t thorStateRevision = 0;
	void collectThorControls(InterfaceObjectConfigurable & source, const std::string & configPath);
public:
	void activate() override;
	void deactivate() override;
	void publishThorContext();
	bool matchesThorContext(const ThorContextRecord & context) const;
	bool executeThorAction(const ThorActionRequest & request);
#endif
private:
	BattleInterface * parentBattleInterface;
	std::shared_ptr<CTabbedInt> tabContentArea;

	std::shared_ptr<CIntObject> createTab(size_t index);
	void openTab(size_t index);


	void loadGameButtonCallback();
	void saveGameButtonCallback();
	void quitGameButtonCallback();
	void backButtonCallback();
	void restartGameButtonCallback();
	void mainMenuButtonCallback();
public:
	SettingsMainWindow(BattleInterface * parentBattleInterface = nullptr);

	void close(); //TODO: copypaste of WindowBase::close(), consider changing Windowbase to IWindowbase with default close() implementation and changing WindowBase inheritance to CIntObject + IWindowBase
	void showAll(Canvas & to) override;
	void onScreenResize() override;
};

