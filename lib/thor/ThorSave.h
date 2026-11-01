#pragma once

#include "ThorContext.h"

/// Transport limits only; native CTextInput owns filename filtering and save semantics.
inline constexpr std::size_t THOR_SAVE_FILENAME_BYTES = 240;
inline constexpr std::size_t THOR_SAVE_LIST_LIMIT = 10000;

inline bool thorSaveFilenamePayloadValid(const std::string & text)
{
	return !text.empty() && text.size() <= THOR_SAVE_FILENAME_BYTES
		&& text != "." && text != ".."
		&& std::none_of(text.begin(), text.end(), [](unsigned char c) { return c < 32 || c == 127; })
		&& thorBoundedText(text, text.size()) == text;
}

/// Native-only list identity. A replaced object, changed path or metadata invalidates
/// every old positional token, including changes outside the currently published page.
struct ThorSaveEntryIdentity
{
	const void * object = nullptr;
	std::string key, label, date;
	bool folder = false, valid = false;
	bool operator==(const ThorSaveEntryIdentity &) const = default;
};
