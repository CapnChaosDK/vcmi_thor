/*
 * ThorRecruitmentSupport.cpp, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 *
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 *
 */
#include "StdInc.h"
#include "ThorRecruitmentSupport.h"

#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)

#include "../GameEngine.h"
#include "../thor/ThorWindowPublisher.h"
#include "../../lib/GameLibrary.h"
#include "../../lib/entities/ResourceTypeHandler.h"

std::string formatThorRecruitmentCost(const ResourceSet & cost)
{
	std::string result;
	for(const GameResID resource : LIBRARY->resourceTypeHandler->getAllObjects())
	{
		const auto amount = cost[resource];
		if(amount <= 0)
			continue;
		if(!result.empty())
			result += ", ";
		result += std::to_string(amount) + " " + resource.toResource()->getNameTranslated();
	}
	return thorBoundedText(std::move(result));
}

void publishThorRecruitmentContext(ThorContextRecord next)
{
	publishThorWindowContext(std::move(next));
}

#endif
