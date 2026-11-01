#pragma once

#include "../../lib/thor/ThorAction.h"

/// MainGUI-only one-shot boundary shared by the native Save adapter and its tests.
/// The caller refreshes its native snapshot before entry; callbacks remain upstream-owned.
class ThorSaveController
{
	std::uint64_t consumedRevision = 0;

public:
	template<typename Execute>
	bool execute(const ThorActionRequest & request, const ThorContextRecord & context,
		const void * owner, const void * parent, bool live, Execute callback)
	{
		if(!live || !owner || !parent || context.contextId != ThorContextIds::SAVE_GAME
			|| context.nativeOwnerToken != owner || context.nativeParentToken != parent
			|| request.revision <= consumedRevision
			|| validateThorActionRequest(request, context) != ThorActionValidation::VALID)
			return false;
		consumedRevision = request.revision;
		return callback();
	}
};
