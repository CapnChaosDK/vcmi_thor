/*
 * CSavingScreen.cpp, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 *
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 *
 */
#include "StdInc.h"

#include "CSavingScreen.h"
#include "SelectionTab.h"

#include "../CPlayerInterface.h"
#include "../GameEngine.h"
#include "../GameInstance.h"
#include "../gui/Shortcut.h"
#include "../gui/WindowHandler.h"
#include "../widgets/Buttons.h"
#include "../widgets/CTextInput.h"

#include "../../lib/callback/CCallback.h"
#include "../../lib/texts/CGeneralTextHandler.h"
#include "../../lib/StartInfo.h"
#include "../../lib/filesystem/Filesystem.h"
#include "../../lib/filesystem/SavegamePath.h"
#include "../../lib/mapping/CMapInfo.h"
#include "../../lib/mapping/CMapHeader.h"
#include "../../lib/GameLibrary.h"

#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
#include "../../lib/CAndroidVMHelper.h"
#include "../../lib/thor/ThorContext.h"

namespace
{
	void publishThorInGameContext(ThorInGameContext inGameContext)
	{
		ThorContextRecord context;
		context.contextId = thorContextIdForInGameContext(inGameContext);
		context = thorContextStore().publishNext(std::move(context));
		CAndroidVMHelper().publishThorContext(context.revision, context.contextId, context.title, context.status);
	}
}
#endif

CSavingScreen::CSavingScreen()
	: CSelectionBase(ESelectionScreen::saveGame)
{
	OBJECT_CONSTRUCTION;
	center(pos);
	localMi = std::make_shared<CMapInfo>();
	localMi->mapHeader = std::unique_ptr<CMapHeader>(new CMapHeader(*GAME->interface()->cb->getMapHeader()));

	tabSel = std::make_shared<SelectionTab>(screenType);
	tabSel->curFolder = SavegamePath::getGameDirectoryName(
		*GAME->interface()->cb->getStartInfo(),
		*GAME->interface()->cb->getMapHeader());
	tabSel->callOnSelect = std::bind(&CSavingScreen::changeSelection, this, _1);
	tabSel->toggleMode();
	curTab = tabSel;
		
	buttonStart = std::make_shared<CButton>(Point(411, 535), AnimationPath::builtin("SCNRSAV.DEF"), LIBRARY->generaltexth->zelp[103], std::bind(&CSavingScreen::saveGame, this), EShortcut::LOBBY_SAVE_GAME);
	
	GAME->interface()->gamePause(true);
}

void CSavingScreen::activate()
{
	if(isActive())
		return;

	CSelectionBase::activate();

#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
	const auto stack = ENGINE->windows().findWindows<IShowActivatable>();
	if(!thorParent && stack.size() > 1) thorParent = stack[stack.size() - 2].get();
	thorSavePending = false;
	publishThorContext();
#endif
}

void CSavingScreen::deactivate()
{
	if(!isActive())
		return;

	CSelectionBase::deactivate();

#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
	thorActionQueue().clear();
	publishThorInGameContext(ThorInGameContext::UNKNOWN);
#endif
}

const CMapInfo * CSavingScreen::getMapInfo()
{
	return localMi.get();
}

const StartInfo * CSavingScreen::getStartInfo()
{
	if (localMi)
		return localMi->scenarioOptionsOfSave.get();
	return GAME->interface()->cb->getStartInfo();
}

void CSavingScreen::changeSelection(std::shared_ptr<CMapInfo> to)
{
	if(localMi == to)
		return;

	localMi = to;
	card->changeSelection();
	card->redraw();
}

void CSavingScreen::close()
{
	GAME->interface()->gamePause(false);
	CSelectionBase::close();
}

void CSavingScreen::saveGame()
{
	if(!(tabSel && tabSel->inputName && tabSel->inputName->getText().size()))
		return;

	std::string path = "Saves/" + tabSel->curFolder + tabSel->inputName->getText();

	auto overWrite = [this, path]() -> void
	{
		tabSel->rememberSave(path);
		GAME->interface()->cb->save(path, true);
		close();
	};

	auto confirmOverwrite = [this, path, overWrite]()
	{
		if(CResourceHandler::get("local")->existsResource(ResourcePath(path, EResType::SAVEGAME)))
		{
			MetaString hlp = MetaString::createFromTextID("core.genrltxt.493"); //%s exists. Overwrite?
			hlp.replaceRawString(tabSel->inputName->getText());
			GAME->interface()->showYesNoDialog(hlp.toString(&GAME->translator()), overWrite, nullptr);
		}
		else
		{
			overWrite();
		}
	};

	if(SavegamePath::isAutosaveName(tabSel->inputName->getText()))
	{
		const std::string warning = LIBRARY->generaltexth->translate("vcmi.savingScreen.autosaveNameWarning");
		GAME->interface()->showYesNoDialog(warning, confirmOverwrite, nullptr);
	}
	else
		confirmOverwrite();
}

#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
void CSavingScreen::publishThorContext(bool force)
{
	if(!isActive() || !ENGINE->windows().isTopWindowWithParent(this, thorParent))
		return;
	const auto now = std::chrono::steady_clock::now();
	if(!force && now < thorNextRefresh) return;
	thorNextRefresh = now + std::chrono::milliseconds(100);
	ThorContextRecord context;
	context.contextId = ThorContextIds::SAVE_GAME;
	context.nativeOwnerToken = this;
	context.nativeParentToken = thorParent;
	context.enabledActionMask = thorActionMask(ThorAction::WINDOW_CLOSE);
	if(tabSel && curTab == tabSel && tabSel->inputName && tabSel->curItems.size() <= THOR_SAVE_LIST_LIMIT)
	{
		std::vector<ThorSaveEntryIdentity> identity;
		for(const auto & item : tabSel->curItems)
			identity.push_back(item ? ThorSaveEntryIdentity{item.get(), item->fileURI, item->isFolder ? item->folderName : item->name, item->date,
				item->isFolder, bool(item->mapHeader)} : ThorSaveEntryIdentity{});
		if(identity != thorList || thorFolder != tabSel->curFolder)
		{
			thorList = std::move(identity);
			thorFolder = tabSel->curFolder;
			++thorEpoch;
		}
		if(thorSelectionRevision != tabSel->getThorScenarioSelectionRevision())
		{
			thorSelectionRevision = tabSel->getThorScenarioSelectionRevision();
			thorPage = static_cast<int>(tabSel->selectionPos / THOR_SAVE_BROWSER_PAGE_SIZE);
		}
		context.scenarioSelectionRevision = thorSelectionRevision;
		context.browserPageCount = std::max(1, static_cast<int>((thorList.size() + THOR_SAVE_BROWSER_PAGE_SIZE - 1) / THOR_SAVE_BROWSER_PAGE_SIZE));
		thorPage = std::clamp(thorPage, 0, context.browserPageCount - 1);
		context.browserPage = thorPage;
		context.enabledActionMask |= thorActionMask(ThorAction::WINDOW_NEXT);
		if(thorPage > 0) context.enabledActionMask |= thorActionMask(ThorAction::LOAD_BROWSER_PREVIOUS_PAGE);
		if(thorPage + 1 < context.browserPageCount) context.enabledActionMask |= thorActionMask(ThorAction::LOAD_BROWSER_NEXT_PAGE);
		for(std::size_t i = thorPage * THOR_SAVE_BROWSER_PAGE_SIZE;
			i < std::min(thorList.size(), (thorPage + 1) * THOR_SAVE_BROWSER_PAGE_SIZE); ++i)
		{
			const auto & item = tabSel->curItems[i];
			const bool enabled = item && (item->isFolder || item->mapHeader);
			context.browserEntries.push_back({static_cast<int>(i), item ? (item->isFolder ? "[" + item->folderName + "]" : item->name) : "", enabled,
				item && !item->isFolder && tabSel->selectionPos == i});
			context.browserNativeKeys.push_back(item ? item->fileURI : "");
			if(enabled) context.enabledActionMask |= thorActionMask(ThorAction::LOAD_BROWSER_SELECT);
		}
		const auto & filename = tabSel->inputName->getText();
		if(thorFilename != filename)
		{
			thorFilename = filename;
			++thorEpoch;
		}
		if(thorSaveFilenamePayloadValid(filename))
		{
			std::size_t offset = 0;
			for(int chunk = 0; chunk < 3; ++chunk)
			{
				context.details[chunk] = thorBoundedText(filename.substr(offset), 96);
				offset += context.details[chunk].size();
			}
			if(buttonStart && !buttonStart->isBlocked() && !buttonStart->isDisabled() && tabSel->inputName->isActive())
				context.enabledActionMask |= thorActionMask(ThorAction::WINDOW_CONFIRM);
		}
		if(auto selected = tabSel->getSelectedMapInfo(); selected && selected->mapHeader)
		{
			context.title = selected->name;
			context.status = selected->date;
			context.details[3] = selected->getNameTranslated(&GAME->translator()) + "  "
				+ std::to_string(selected->mapHeader->width) + " x " + std::to_string(selected->mapHeader->height);
		}
	}
	if(thorSavePending) context.enabledActionMask = 0;
	context.actionEpoch = thorEpoch;
	const auto previous = thorContextStore().snapshot();
	context = thorContextStore().publishNext(std::move(context));
	if(context.revision == previous.revision) return;
	thorActionQueue().clear();
	CAndroidVMHelper bridge;
	bridge.publishThorContext(context.revision, context.contextId, context.title, context.status, context.details);
	bridge.publishThorActionState(context.revision, context.enabledActionMask, 0);
	bridge.publishThorBrowser(context.revision, context.browserPage, context.browserPageCount, context.browserEntries);
}

bool CSavingScreen::executeThorAction(const ThorActionRequest & request)
{
	publishThorContext();
	const auto context = thorContextStore().snapshot();
	return thorController.execute(request, context, this, thorParent,
		isActive() && ENGINE->windows().isTopWindowWithParent(this, thorParent) && GAME->interface(), [&]()
	{
		// Consume before any native callback, including unchanged selection/filename and cancelled prompts.
		++thorEpoch;
		switch(request.action)
		{
		case ThorAction::LOAD_BROWSER_PREVIOUS_PAGE: --thorPage; break;
		case ThorAction::LOAD_BROWSER_NEXT_PAGE: ++thorPage; break;
		case ThorAction::LOAD_BROWSER_SELECT:
			if(!tabSel || request.targetId < 0 || static_cast<std::size_t>(request.targetId) >= tabSel->curItems.size())
				break;
			tabSel->selectAbs(request.targetId);
			break;
		case ThorAction::WINDOW_NEXT:
			// No SDL focus or synthetic input: use the exact filename widget's own filter.
			tabSel->inputName->setThorText(request.text);
			break;
		case ThorAction::WINDOW_CONFIRM:
			thorSavePending = true;
			publishThorContext();
			saveGame();
			return true;
		case ThorAction::WINDOW_CLOSE:
			close();
			return true;
		default: return false;
		}
		publishThorContext();
		return true;
	});
}
#endif
