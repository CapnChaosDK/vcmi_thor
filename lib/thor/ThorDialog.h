#pragma once

#include "ThorContext.h"
#include "ThorAction.h"

#include <algorithm>

/// Complete UTF-8 text, split into bounded transport chunks. Empty means unsupported;
/// never offer confirmation on a silently truncated question.
inline std::vector<ThorContextDetails> thorDialogPages(const std::string & text,
	std::size_t chunkBytes = 96, int linesPerPage = 0)
{
	if(chunkBytes < 4 || chunkBytes > 96 || text.empty() || text.size() > 8192 || thorBoundedText(text, text.size()) != text)
		return {};
	std::vector<ThorContextDetails> pages;
	std::size_t position = 0;
	while(position < text.size())
	{
		ThorContextDetails page;
		int lines = 0;
		for(auto & chunk : page)
		{
			if(position == text.size() || (linesPerPage && lines >= linesPerPage)) break;
			chunk = thorBoundedText(text.substr(position), chunkBytes);
			if(linesPerPage)
			{
				for(std::size_t i = 0; i < chunk.size(); ++i)
				{
					if(chunk[i] == '\n' && ++lines == linesPerPage)
					{
						chunk.resize(i + 1);
						break;
					}
				}
			}
			position += chunk.size();
		}
		pages.push_back(std::move(page));
	}
	return pages;
}

inline std::vector<ThorContextDetails> thorEncounterPages(const std::string & text)
{
	// The choice rows leave a shorter reading area than an ordinary dialog.
	return thorDialogPages(text, 64, 5);
}

inline constexpr int THOR_ENCOUNTER_PAGE_SIZE = 4;
inline constexpr int THOR_ENCOUNTER_MAX_CHOICES = 4096;

inline bool thorEncounterLabelValid(const std::string & label)
{
	return !label.empty() && thorBoundedText(label, 128) == label;
}

template<typename TargetAt>
int thorEncounterIndex(int begin, int end, int target, TargetAt targetAt)
{
	for(int i = begin; i < end; ++i)
		if(targetAt(i) == target)
			return i;
	return -1;
}

inline void thorEncounterText(ThorContextRecord & context, const std::vector<ThorContextDetails> & pages, int & page)
{
	page = std::clamp(page, 0, std::max(0, static_cast<int>(pages.size()) - 1));
	if(pages.empty())
		return;
	context.details = pages[page];
	context.title = std::to_string(page + 1) + " / " + std::to_string(pages.size());
	if(page > 0)
		context.enabledActionMask |= thorActionMask(ThorAction::WINDOW_PREVIOUS);
	if(page + 1 < pages.size())
		context.enabledActionMask |= thorActionMask(ThorAction::WINDOW_NEXT);
}

inline bool thorEncounterOwnerMatches(const ThorContextRecord & context, const void * owner, const void * parent, bool active)
{
	return active && owner && parent && context.contextId == ThorContextIds::ENCOUNTER_CHOICE
		&& context.nativeOwnerToken == owner && context.nativeParentToken == parent;
}
