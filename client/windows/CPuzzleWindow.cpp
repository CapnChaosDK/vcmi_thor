/*
 * CPuzzleWindow.cpp, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 *
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 *
 */
#include "StdInc.h"
#include "CPuzzleWindow.h"

#include "../CPlayerInterface.h"
#include "../adventureMap/CResDataBar.h"
#include "../GameEngine.h"
#include "../GameInstance.h"
#include "../gui/TextAlignment.h"
#include "../gui/Shortcut.h"
#include "../mapView/MapView.h"
#include "media/ISoundPlayer.h"
#include "../widgets/Buttons.h"
#include "../widgets/Images.h"
#include "../widgets/TextControls.h"
#include "../widgets/GraphicalPrimitiveCanvas.h"

#include "../../lib/callback/CCallback.h"
#include "../../lib/entities/faction/CFaction.h"
#include "../../lib/entities/faction/CTownHandler.h"
#include "../../lib/texts/CGeneralTextHandler.h"
#include "../../lib/StartInfo.h"
#include "../../lib/GameLibrary.h"

#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
#include "../thor/ThorReferenceSupport.h"
#include "../thor/ThorPuzzleSnapshot.h"
#include "../gui/WindowHandler.h"
#include "render/Canvas.h"
#endif


CPuzzleWindow::CPuzzleWindow(const int3 & GrailPos, double discoveredRatio)
	: CWindowObject(PLAYER_COLORED | BORDERED, ImagePath::builtin("PUZZLE")),
	grailPos(GrailPos),
	currentAlpha(ColorRGBA::ALPHA_OPAQUE)
{
#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
	thorProgress = static_cast<int>(std::clamp(discoveredRatio, 0.0, 1.0) * 100);
#endif
	OBJECT_CONSTRUCTION;

	ENGINE->sound().playSound(soundBase::OBELISK);

	quitb = std::make_shared<CButton>(Point(670, 538), AnimationPath::builtin("IOK6432.DEF"), CButton::tooltip(LIBRARY->generaltexth->allTexts[599]), std::bind(&CPuzzleWindow::close, this), EShortcut::GLOBAL_RETURN);
	quitb->setBorderColor(Colors::METALLIC_GOLD);

	mapView = std::make_shared<PuzzleMapView>(Point(8,8), Point(592, 544), grailPos);
	mapView->needFullUpdate = true;

	logo = std::make_shared<CPicture>(ImagePath::builtin("PUZZLOGO"), 607, 3);
	title = std::make_shared<CLabel>(700, 95, FONT_BIG, ETextAlignment::CENTER, Colors::YELLOW, LIBRARY->generaltexth->allTexts[463]);
	resDataBar = std::make_shared<CResDataBar>(ImagePath::builtin("ARESBAR.bmp"), 3, 575, 32, 2, 85, 85);

	FactionID faction = GAME->interface()->cb->getStartInfo()->playerInfos.find(GAME->interface()->playerID)->second.castle;

	auto & puzzleMap = faction.toFaction()->puzzleMap;

	for(auto & elem : puzzleMap)
	{
		const SPuzzleInfo & info = elem;

		auto piece = std::make_shared<CPicture>(info.filename, info.position.x + 1, info.position.y);
		piece->needRefresh = true;

		//piece that will slowly disappear
		if(info.whenUncovered <= GameConstants::PUZZLE_MAP_PIECES * discoveredRatio)
		{
			piecesToRemove.push_back(piece);
			piece->recActions = piece->recActions & ~SHOWALL;
		}
		else
		{
			visiblePieces.push_back(piece);
		}
	}

	border = std::make_shared<GraphicalPrimitiveCanvas>(Rect(Point(6,6), Point(596, 548)));
	for(int i = 0; i < 3; i++)
		border->addRectangle(Point(i, i), Point(border->pos.w - i * 2, border->pos.h - i * 2), Colors::BLACK);
}

void CPuzzleWindow::activate()
{
	if(isActive())
		return;

	CWindowObject::activate();

#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
	refreshThorReference();
#endif
}

void CPuzzleWindow::deactivate()
{
	if(!isActive())
		return;

	CWindowObject::deactivate();

#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
	withdrawThorReference();
#endif
}

void CPuzzleWindow::showAll(Canvas & to)
{
	CWindowObject::showAll(to);
}

void CPuzzleWindow::show(Canvas & to)
{
	constexpr int animSpeed = 2;

	if(currentAlpha < animSpeed)
	{
		piecesToRemove.clear();
	}
	else
	{
		//update disappearing puzzles
		for(auto & piece : piecesToRemove)
			piece->setAlpha(currentAlpha);
		currentAlpha -= animSpeed;
	}
	CWindowObject::show(to);

	if(mapView->needFullUpdate && piecesToRemove.empty())
		mapView->needFullUpdate = false;
}

#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
void CPuzzleWindow::refreshThorReference()
{
	if(!isActive()) return;
	const auto stack = ENGINE->windows().findWindows<IShowActivatable>();
	if(!thorParent && stack.size() > 1) thorParent = stack[stack.size() - 2].get();
	if(!ENGINE->windows().isTopWindowWithParent(this, thorParent)) return;
	// One software snapshot after the native reveal finishes. Draw the map and ALL
	// remaining covers into the same surface before reading any transport pixels.
	// Never render an unmasked map into the payload or publish native grail coordinates.
	if(!thorCaptureAttempted && piecesToRemove.empty())
	{
		thorCaptureAttempted = true;
		const Rect area = mapView->pos;
		if(area.w == ThorReference::WIDTH && area.h == ThorReference::HEIGHT
			&& area.x >= 0 && area.y >= 0 && area.x + area.w <= 4096 && area.y + area.h <= 4096)
		{
			Canvas canvas(Point(area.x + area.w, area.y + area.h), CanvasScalingPolicy::IGNORE);
			thorImage = thorPuzzleSnapshot(piecesToRemove.empty(), visiblePieces.size(),
				[&]() { mapView->showAll(canvas); },
				[&](std::size_t i) { visiblePieces[i]->showAll(canvas); },
				[&](int x, int y) { return canvas.getPixel(Point(area.x + x, area.y + y)); });
		}
	}
	ThorReference panel;
	panel.kind = 3;
	panel.progress = thorProgress;
	if(thorImage) { panel.rgb = thorImage; panel.width = ThorReference::WIDTH; panel.height = ThorReference::HEIGHT; }
	publishThorReference(this, thorParent, std::move(panel), 0);
}

bool CPuzzleWindow::executeThorReference(const ThorActionRequest & request)
{
	refreshThorReference();
	const auto state = thorContextStore().snapshot();
	if(!isActive() || !ENGINE->windows().isTopWindowWithParent(this, thorParent)
		|| state.nativeOwnerToken != this || state.nativeParentToken != thorParent
		|| request.revision <= thorConsumed || validateThorActionRequest(request, state) != ThorActionValidation::VALID
		|| request.commandId != ThorExtended::REFERENCE_CLOSE) return false;
	thorConsumed = request.revision;
	close();
	return true;
}
#endif
