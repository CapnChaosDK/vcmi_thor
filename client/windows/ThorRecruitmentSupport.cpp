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
#include "../thor/ThorVisualAssetPublisher.h"
#include "../../lib/CAndroidVMHelper.h"
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
	const auto previous = thorContextStore().snapshot();
	const auto context = thorContextStore().publishNext(std::move(next));
	if(context.revision == previous.revision)
		return;

	CAndroidVMHelper bridge;
	bridge.publishThorContext(context.revision, context.contextId, context.title, context.status, context.details);
	bridge.publishThorActionState(context.revision, context.enabledActionMask, context.activeActionMask);
	if(context.recruitment)
	{
		bridge.publishThorRecruitment(context.revision, *context.recruitment);
		publishThorVisualAssets(context);
	}
}

#endif
