/*
 * ThorRequestTracker.h, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 * License: GNU General Public License v2.0 or later
 */
#pragma once

#include <utility>

namespace ThorRequests
{
inline thread_local int * capturedRequest = nullptr;

inline void submitted(int requestId)
{
	if(capturedRequest)
		*capturedRequest = requestId;
}
}

/// Observe the synchronous upstream callback's request ID without changing its API.
template<typename Submit>
int captureThorRequest(Submit && submit)
{
	int requestId = -1;
	struct Capture
	{
		int * previous;
		explicit Capture(int & result) : previous(ThorRequests::capturedRequest)
		{
			ThorRequests::capturedRequest = &result;
		}
		~Capture() { ThorRequests::capturedRequest = previous; }
	} capture(requestId);
	std::forward<Submit>(submit)();
	return requestId;
}
