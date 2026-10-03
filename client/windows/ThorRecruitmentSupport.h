/*
 * ThorRecruitmentSupport.h, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 *
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 *
 */
#pragma once

#include "../../lib/ResourceSet.h"

#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
#include "../../lib/thor/ThorContext.h"

std::string formatThorRecruitmentCost(const ResourceSet & cost);
void publishThorRecruitmentContext(ThorContextRecord context);
#endif
