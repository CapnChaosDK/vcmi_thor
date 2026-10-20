/*
 * ControllerInputState.h, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 */
#pragma once

#include <algorithm>
#include <array>
#include <cmath>
#include <cstdint>

namespace ControllerInput
{
/// Symmetric calibrated dead zone, shared by SDL2 and SDL3.
inline double axisValue(int value, double deadZone, double fullZone)
{
	const double ratio = static_cast<double>(value) / (value < 0 ? 32768.0 : 32767.0);
	deadZone = std::clamp(deadZone, 0.0, 0.99);
	fullZone = std::clamp(fullZone, deadZone + 0.001, 1.0);
	return std::copysign(std::clamp((std::abs(ratio) - deadZone) / (fullZone - deadZone), 0.0, 1.0), ratio);
}

/// A canceled stick must pass through neutral before it can navigate a new context.
class AxisLatch
{
	std::array<bool, 2> held{};
	bool blocked = false;
public:
	void cancel() { blocked = held[0] || held[1]; }
	bool update(double value, int component = 0)
	{
		held[component] = std::abs(value) > 0.0001;
		if(!held[0] && !held[1])
			blocked = false;
		return !blocked;
	}
};

/// At most one step per frame, including after a long frame; no accumulated burst.
inline bool repeatReady(uint32_t elapsed, uint32_t & remaining)
{
	if(remaining <= elapsed)
		return true;
	remaining -= elapsed;
	return false;
}
}
