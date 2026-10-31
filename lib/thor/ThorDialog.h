#pragma once

#include "ThorContext.h"

/// Complete UTF-8 text, split into bounded transport chunks. Empty means unsupported;
/// never offer confirmation on a silently truncated question.
inline std::vector<ThorContextDetails> thorDialogPages(const std::string & text)
{
	if(text.empty() || text.size() > 8192 || thorBoundedText(text, text.size()) != text)
		return {};
	std::vector<ThorContextDetails> pages;
	std::size_t position = 0;
	while(position < text.size())
	{
		ThorContextDetails page;
		for(auto & chunk : page)
		{
			if(position == text.size()) break;
			chunk = thorBoundedText(text.substr(position), 96);
			position += chunk.size();
		}
		pages.push_back(std::move(page));
	}
	return pages;
}
