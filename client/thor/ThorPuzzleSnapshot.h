#pragma once

#include "../../lib/thor/ThorReference.h"

/// MainGUI-only capture transaction: no transport reads until the native map AND
/// every remaining covering piece have been drawn. Never capture during a reveal.
template<typename DrawMap, typename DrawCover, typename ReadPixel>
std::shared_ptr<const std::vector<std::uint8_t>> thorPuzzleSnapshot(bool revealFinished,
	std::size_t covers, DrawMap drawMap, DrawCover drawCover, ReadPixel readPixel)
{
	if(!revealFinished) return {};
	drawMap();
	for(std::size_t i = 0; i < covers; ++i) drawCover(i);
	auto rgb = std::make_shared<std::vector<std::uint8_t>>();
	rgb->reserve(ThorReference::WIDTH * ThorReference::HEIGHT * 3);
	for(int y = 0; y < ThorReference::HEIGHT; ++y)
		for(int x = 0; x < ThorReference::WIDTH; ++x)
		{
			const auto pixel = readPixel(x, y);
			rgb->insert(rgb->end(), {pixel.r, pixel.g, pixel.b});
		}
	return rgb;
}
