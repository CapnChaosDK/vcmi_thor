#pragma once

#include "../../lib/CAndroidVMHelper.h"
#include "../../lib/thor/ThorAction.h"

inline void publishThorReference(const void * owner, const void * parent, ThorReference panel, std::uint64_t epoch)
{
	ThorContextRecord next;
	next.contextId = panel.kind == 1 ? ThorContextIds::QUEST_LOG
		: panel.kind == 2 ? ThorContextIds::SCENARIO_EVENT_JOURNAL : ThorContextIds::PUZZLE_MAP;
	next.nativeOwnerToken = owner;
	next.nativeParentToken = parent;
	next.actionEpoch = epoch;
	if(panel.valid()) next.reference = std::move(panel);
	const auto previous = thorContextStore().snapshot();
	const auto state = thorContextStore().publishNext(std::move(next));
	if(state.revision == previous.revision) return;
	CAndroidVMHelper bridge;
	bridge.publishThorContext(state.revision, state.contextId, {}, {});
	if(state.reference) bridge.publishThorReference(state.revision, *state.reference);
}

inline void withdrawThorReference()
{
	thorActionQueue().clear();
	const auto state = thorContextStore().publishNext({});
	CAndroidVMHelper().publishThorContext(state.revision, state.contextId, {}, {});
}
