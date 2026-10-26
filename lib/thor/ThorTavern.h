#pragma once

#include "ThorExtendedCommand.h"
#include <string>

/// Bounded visible Tavern choices. Availability is supplied by the native owner.
struct DLL_LINKAGE ThorTavernRow
{
	int id = -1;
	int flags = 0; // 1 offered, 2 inviteable
	std::string name, description;
	bool operator==(const ThorTavernRow &) const = default;
};

struct DLL_LINKAGE ThorTavern
{
	static constexpr int MAX_ROWS = 258;
	int selected = -1, invited = -1, cost = 0, gold = 0, reason = 0;
	bool controllable = false, canRecruit = false, canGuild = false;
	std::string rumor;
	std::vector<ThorTavernRow> rows;
	bool operator==(const ThorTavern &) const = default;
	bool has(int id, int flag) const
	{
		return std::any_of(rows.begin(), rows.end(), [&](const auto & row)
			{ return row.id == id && (row.flags & flag) != 0; });
	}
	bool valid() const
	{
		if(rows.size() > MAX_ROWS || selected < -1 || invited < -1 || cost < 0 || gold < 0
			|| reason < 0 || reason > 6 || rumor.size() > 1024
			|| (selected != -1 && !has(selected, 1)) || (invited != -1 && !has(invited, 2))
			|| (canRecruit && (!controllable || selected < 0 || gold < cost || reason != 0))) return false;
		int offered = 0, invitations = 0;
		for(std::size_t i = 0; i < rows.size(); ++i)
		{
			const auto & row = rows[i];
			if(row.id < 0 || row.flags < 1 || row.flags > 3 || row.name.empty() || row.name.size() > 256
				|| row.description.size() > 1024 || std::any_of(rows.begin(), rows.begin() + i,
					[&](const auto & other) { return other.id == row.id; })) return false;
			offered += (row.flags & 1) != 0;
			invitations += (row.flags & 2) != 0;
		}
		return offered <= 2 && invitations <= 256;
	}
	bool allows(int command, int target) const
	{
		if(!valid() || !ThorExtended::validTarget(command, target)) return false;
		switch(command)
		{
		case ThorExtended::TAVERN_CLOSE: return true;
		case ThorExtended::TAVERN_GUILD: return canGuild;
		case ThorExtended::TAVERN_SELECT: return controllable && has(target, 1);
		case ThorExtended::TAVERN_INVITE: return controllable && has(target, 2);
		case ThorExtended::TAVERN_RECRUIT: return canRecruit;
		default: return false;
		}
	}
};
