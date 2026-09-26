/*
 * ThorVisualAssetPublisher.h, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 *
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 *
 */
#pragma once

#include "../../lib/thor/ThorContext.h"

#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
/// Publishes only assets referenced by this revision-bound context.
void publishThorVisualAssets(const ThorContextRecord & context);
#endif
