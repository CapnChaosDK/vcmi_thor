#include "ThorContext.h"

#include <utility>

ThorContextRecord ThorContextStore::snapshot() const
{
	std::lock_guard lock(mutex);
	return current;
}

bool ThorContextStore::publish(ThorContextRecord next)
{
	std::lock_guard lock(mutex);
	if(next.revision <= current.revision)
		return false;
	if(next.contextId.empty())
		next.contextId = "UNKNOWN";
	current = std::move(next);
	return true;
}

ThorContextStore & thorContextStore()
{
	static ThorContextStore store;
	return store;
}
