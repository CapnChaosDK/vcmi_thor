#pragma once

#include "ThorExtendedCommand.h"
#include <string>

/// Visible native Blacksmith offer or Mage Guild scrolls. No gameplay rules live here.
struct DLL_LINKAGE ThorTownServiceRow
{
	int id = -1, level = 0;
	bool actionable = false;
	std::string name, description;
	bool operator==(const ThorTownServiceRow &) const = default;
};

struct DLL_LINKAGE ThorTownServicePanel
{
	static constexpr int MAX_ROWS = 128;
	int kind = 0; // 1 Blacksmith, 2 Mage Guild
	int owner = -1, cost = 0, gold = 0;
	std::string summary;
	std::vector<ThorTownServiceRow> rows;
	bool operator==(const ThorTownServicePanel &) const = default;
	bool valid() const
	{
		if((kind != 1 && kind != 2) || owner < 0 || cost < 0 || gold < 0
			|| summary.size() > 2048 || rows.size() > MAX_ROWS || (kind == 1 && rows.size() != 1))
			return false;
		for(std::size_t i = 0; i < rows.size(); ++i)
		{
			const auto & row = rows[i];
			if(row.id < 0 || row.level < 0 || row.level > 5 || (kind == 1 && row.level != 0)
				|| (kind == 2 && row.level == 0) || row.name.empty() || row.name.size() > 256
				|| row.description.size() > 4096 || (kind == 1 && row.actionable && gold < cost)
				|| std::any_of(rows.begin(), rows.begin() + i, [&](const auto & other) { return row.id == other.id; }))
				return false;
		}
		return true;
	}
	bool allows(int command, int target) const
	{
		if(!valid() || !ThorExtended::validTarget(command, target)) return false;
		if(command == ThorExtended::SERVICE_CLOSE) return true;
		if(command != (kind == 1 ? ThorExtended::BLACKSMITH_BUY : ThorExtended::GUILD_RESEARCH)) return false;
		return std::any_of(rows.begin(), rows.end(), [&](const auto & row) { return row.id == target && row.actionable; });
	}
};
