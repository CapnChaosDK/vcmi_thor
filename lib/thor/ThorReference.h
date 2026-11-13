#pragma once

#include "ThorExtendedCommand.h"
#include <cstdint>
#include <memory>
#include <string>

/// Only the native journal's visible text and the fully masked puzzle image cross JNI.
/// No map coordinates, quest model, paths or unmasked puzzle pixels are transported.
struct DLL_LINKAGE ThorReference
{
	static constexpr int ROWS_PER_PAGE = 5, MAX_ITEMS = 10000, WIDTH = 592, HEIGHT = 544;
	int kind = 0; // 1 quests, 2 events, 3 puzzle
	int page = 0, pages = 0, selected = -1, textPage = 0, textPages = 0;
	int flags = 8; // quests, events, center, close
	int width = 0, height = 0, progress = 0;
	std::vector<int> targets;
	std::vector<std::string> labels;
	std::string text;
	std::shared_ptr<const std::vector<std::uint8_t>> rgb;
	bool operator==(const ThorReference &) const = default;
	bool valid() const
	{
		if(kind < 1 || kind > 3 || page < 0 || pages < 0 || pages > MAX_ITEMS / ROWS_PER_PAGE
			|| (pages == 0 ? page != 0 : page >= pages) || selected < -1 || selected >= MAX_ITEMS
			|| textPage < 0 || textPages < 0 || textPages > 512 || (textPages == 0 ? textPage != 0 : textPage >= textPages)
			|| (textPages == 0 && !text.empty()) || ((flags & 4) && selected < 0)
			|| flags < 0 || flags > 15 || !(flags & 8) || progress < 0 || progress > 100
			|| targets.size() > ROWS_PER_PAGE || targets.size() != labels.size() || text.size() > 384)
			return false;
		for(std::size_t i = 0; i < targets.size(); ++i)
			if(targets[i] != page * ROWS_PER_PAGE + static_cast<int>(i) || targets[i] >= MAX_ITEMS || labels[i].empty() || labels[i].size() > 128)
				return false;
		if(kind == 3)
			return pages == 0 && targets.empty() && selected == -1 && flags == 8 && textPages == 0
				&& ((width == 0 && height == 0 && !rgb) || (width == WIDTH && height == HEIGHT && rgb && rgb->size() == WIDTH * HEIGHT * 3));
		return width == 0 && height == 0 && !rgb && progress == 0
			&& (pages != 0 ? selected < pages * ROWS_PER_PAGE : targets.empty() && selected == -1);
	}
	bool allows(int command, int target) const
	{
		if(!valid() || !ThorExtended::validTarget(command, target)) return false;
		switch(command)
		{
		case ThorExtended::REFERENCE_SELECT: return std::find(targets.begin(), targets.end(), target) != targets.end();
		case ThorExtended::REFERENCE_PREVIOUS: return page > 0;
		case ThorExtended::REFERENCE_NEXT: return page + 1 < pages;
		case ThorExtended::REFERENCE_TEXT_PREVIOUS: return textPage > 0;
		case ThorExtended::REFERENCE_TEXT_NEXT: return textPage + 1 < textPages;
		case ThorExtended::REFERENCE_QUESTS: return kind == 2 && (flags & 1);
		case ThorExtended::REFERENCE_EVENTS: return kind == 1 && (flags & 2);
		case ThorExtended::REFERENCE_CENTER: return flags & 4;
		case ThorExtended::REFERENCE_CLOSE: return flags & 8;
		default: return false;
		}
	}
};
