#pragma once

#include "../../Global.h"

#include <cstdint>
#include <mutex>
#include <string>

/// Immutable, read-only context payload reserved for the Thor command deck.
struct DLL_LINKAGE ThorContextRecord
{
	std::uint64_t revision = 0;
	std::string contextId = "UNKNOWN";
	std::string title;
	std::string status;
};

/// Thread-safe latest-record handoff. Consumers must discard revisions older than their last render.
class DLL_LINKAGE ThorContextStore final
{
	mutable std::mutex mutex;
	ThorContextRecord current;

public:
	ThorContextRecord snapshot() const;
	bool publish(ThorContextRecord next);
};

DLL_LINKAGE ThorContextStore & thorContextStore();
