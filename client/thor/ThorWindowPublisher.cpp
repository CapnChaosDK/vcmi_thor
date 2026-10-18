/*
 * ThorWindowPublisher.cpp, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 *
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 *
 */
#include "StdInc.h"
#include "ThorWindowPublisher.h"

#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
#include "ThorVisualAssetPublisher.h"
#include "../../lib/CAndroidVMHelper.h"

void publishThorWindowContext(ThorContextRecord next, bool invalidateActions)
{
	CAndroidVMHelper bridge;
	publishThorWindowContext(thorContextStore(), std::move(next), bridge, publishThorVisualAssets, invalidateActions);
}
#endif
