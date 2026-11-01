/*
 * CSavingScreen.h, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 *
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 *
 */
#pragma once

#include "CSelectionBase.h"
#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
#include "../../lib/thor/ThorSave.h"
#include "../thor/ThorSaveController.h"
#endif

struct StartInfo;
class CMapInfo;

class CSelectionBase;

class CSavingScreen : public CSelectionBase
{
#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
	IShowActivatable * thorParent = nullptr;
	ThorSaveController thorController;
	std::chrono::steady_clock::time_point thorNextRefresh;
	int thorPage = 0;
	bool thorSavePending = false;
	std::string thorFolder, thorFilename;
	std::uint64_t thorEpoch = 0, thorSelectionRevision = 0;
	std::vector<ThorSaveEntryIdentity> thorList;
#endif
public:
#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
	void publishThorContext(bool force = true);
	bool executeThorAction(const ThorActionRequest & request);
#endif
	std::shared_ptr<CMapInfo> localMi;

	CSavingScreen();

	void changeSelection(std::shared_ptr<CMapInfo> to);
	void saveGame();
	void activate() override;
	void deactivate() override;

	const CMapInfo * getMapInfo() override;
	const StartInfo * getStartInfo() override;
	
protected:
	void close() override;
};
