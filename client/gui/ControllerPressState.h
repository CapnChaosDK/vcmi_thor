/*
 * ControllerPressState.h, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 */
#pragma once

#include "Shortcut.h"
#include <map>
#include <memory>
#include <optional>
#include <string>
#include <utility>
#include <vector>

class IShowActivatable;

/// Physical presses belong to their original window until released or canceled.
/// Canceled entries remain latched, so repeated events cannot arm the next window.
class ControllerPressState
{
public:
	using Identity = std::pair<int, std::string>;
	struct Press
	{
		std::vector<EShortcut> shortcuts;
		std::weak_ptr<IShowActivatable> window;
		bool canceled = false;
		bool popup = false;
	};
private:
	std::map<Identity, Press> presses;
public:
	bool press(const Identity & id, const std::vector<EShortcut> & shortcuts, const std::shared_ptr<IShowActivatable> & window)
	{
		return presses.emplace(id, Press{shortcuts, window, !window}).second;
	}
	void suppress(const Identity & id)
	{
		presses[id].canceled = true;
	}
	bool active(const Identity & id, const std::shared_ptr<IShowActivatable> & window) const
	{
		const auto found = presses.find(id);
		return found != presses.end() && !found->second.canceled && window && found->second.window.lock() == window;
	}
	std::optional<Press> release(const Identity & id, const std::shared_ptr<IShowActivatable> & window)
	{
		const auto found = presses.find(id);
		if(found == presses.end())
			return std::nullopt;
		const auto result = found->second;
		presses.erase(found);
		if(result.canceled || !window || result.window.lock() != window)
			return std::nullopt;
		return result;
	}
	void transferPopup(const Identity & id, const std::shared_ptr<IShowActivatable> & popup)
	{
		const auto found = presses.find(id);
		if(found != presses.end())
		{
			found->second.window = popup;
			found->second.popup = true;
			found->second.canceled = false;
		}
	}
	template<typename Cancel>
	void cancel(const std::shared_ptr<IShowActivatable> & window, Cancel callback)
	{
		std::vector<Press> canceled;
		for(auto & [id, press] : presses)
		{
			if(!press.canceled && window && press.window.lock() == window)
				canceled.push_back(press);
			press.canceled = true;
		}
		// Mark everything first; callbacks may close windows and reenter cancellation.
		for(const auto & press : canceled)
			callback(press);
	}
	void forget(int instance)
	{
		std::erase_if(presses, [instance](const auto & entry) { return entry.first.first == instance; });
	}
};
