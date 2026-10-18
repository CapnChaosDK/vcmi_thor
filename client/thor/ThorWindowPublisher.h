/*
 * ThorWindowPublisher.h, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 *
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 *
 */
#pragma once

#include "../../lib/thor/ThorContext.h"
#include <utility>

/// MainGUI-only publication for the four compatible window families. The bridge
/// parameter permits recording the actual transport sequence in host-native tests.
/// Owners retain snapshot construction, live validation and lifecycle withdrawal.
template<typename Bridge, typename VisualPublisher>
void publishThorWindowContext(ThorContextStore & store, ThorContextRecord next,
	Bridge & bridge, VisualPublisher publishVisuals, bool invalidateActions = false)
{
	const bool hero = next.contextId == ThorContextIds::HERO_WINDOW;
	const bool town = next.contextId == ThorContextIds::TOWN_WINDOW;
	const bool meeting = next.contextId == ThorContextIds::HERO_MEETING;
	const bool recruitment = next.contextId == ThorContextIds::TOWN_RECRUITMENT_QUICK
		|| next.contextId == ThorContextIds::TOWN_RECRUITMENT_DWELLING;
	// Quick Recruitment also uses this path to withdraw an unsupported owner.
	if(!hero && !town && !meeting && !recruitment && next.contextId != ThorContextIds::UNKNOWN)
		return;

	const auto previous = store.snapshot();
	if(hero || town || meeting)
		next.actionEpoch = previous.contextId == next.contextId
			? previous.actionEpoch + (invalidateActions ? 1 : 0) : 0;
	const auto context = store.publishNext(std::move(next));
	if(context.revision == previous.revision)
		return;

	bridge.publishThorContext(context.revision, context.contextId, context.title, context.status,
		meeting ? ThorContextDetails{} : context.details, hero ? context.heroPortraitAssetKey : 0);
	// Hero Meeting must deliver both payloads before enabling actions. The other
	// families keep their existing action-before-payload callback order.
	if(meeting)
	{
		bridge.publishThorHeroMeetingArmies(context.revision, *context.heroMeetingArmies);
		bridge.publishThorHeroMeetingArtifacts(context.revision,
			context.heroMeetingArtifacts.value_or(ThorHeroMeetingArtifacts{}));
	}
	bridge.publishThorActionState(context.revision, context.enabledActionMask,
		hero || town ? 0 : context.activeActionMask);
	if(hero)
		bridge.publishThorHeroManagement(context.revision, *context.heroManagement);
	if(town)
	{
		bridge.publishThorBrowser(context.revision, 0, 1, context.browserEntries);
		if(context.townManagement)
			bridge.publishThorTownManagement(context.revision, *context.townManagement);
	}
	if(recruitment && context.recruitment)
		bridge.publishThorRecruitment(context.revision, *context.recruitment);
	if(hero || meeting || (recruitment && context.recruitment))
		publishVisuals(context);
}

#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
void publishThorWindowContext(ThorContextRecord next, bool invalidateActions = false);
#endif
