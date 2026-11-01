/*
 * CPuzzleWindow.h, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 *
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 *
 */
#pragma once

#include "CWindowObject.h"
#include "../../lib/int3.h"

struct ThorActionRequest;
struct ThorReference;

class CLabel;
class CButton;
class CResDataBar;
class PuzzleMapView;
class GraphicalPrimitiveCanvas;

/// Puzzle screen which gets uncovered when you visit obilisks
class CPuzzleWindow : public CWindowObject
{
private:
#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
	IShowActivatable * thorParent = nullptr;
	std::shared_ptr<const std::vector<std::uint8_t>> thorImage;
	std::uint64_t thorConsumed = 0;
	int thorProgress = 0;
	bool thorCaptureAttempted = false;
#endif
	int3 grailPos;
	std::shared_ptr<PuzzleMapView> mapView;
	std::shared_ptr<CPicture> logo;
	std::shared_ptr<GraphicalPrimitiveCanvas> border;
	std::shared_ptr<CLabel> title;
	std::shared_ptr<CButton> quitb;
	std::shared_ptr<CResDataBar> resDataBar;

	std::vector<std::shared_ptr<CPicture>> piecesToRemove;
	std::vector<std::shared_ptr<CPicture>> visiblePieces;
	ui8 currentAlpha;

public:
#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
	void refreshThorReference();
	bool executeThorReference(const ThorActionRequest & request);
#endif
	void showAll(Canvas & to) override;
	void show(Canvas & to) override;
	void activate() override;
	void deactivate() override;

	CPuzzleWindow(const int3 & grailPos, double discoveredRatio);
};
