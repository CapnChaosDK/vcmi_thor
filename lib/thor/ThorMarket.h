#pragma once

#include "ThorExtendedCommand.h"
#include <limits>
#include <string>
#include <vector>

/// Visible resource rows and the current native slider quote; no trading rules live here.
struct DLL_LINKAGE ThorMarketRow
{
	int id = -1;
	int balance = 0;
	std::string name;
	bool sell = false;
	bool buy = false;
	bool operator==(const ThorMarketRow &) const = default;
};

struct DLL_LINKAGE ThorMarket
{
	static constexpr int MAX_ROWS = 9;
	int status = 0; // 0 ready, 1 pending, 2 rejected, 3 completed
	bool controllable = false;
	int selling = -1, buying = -1;
	int bid = 0, offer = 0, amount = 0, maximum = 0;
	std::vector<ThorMarketRow> rows;
	bool operator==(const ThorMarket &) const = default;
	bool valid() const
	{
		if(status < 0 || status > 3 || rows.empty() || rows.size() > MAX_ROWS
			|| selling < -1 || buying < -1 || bid < 0 || offer < 0 || amount < 0
			|| maximum < amount || (maximum > 0 && (bid == 0 || offer == 0
				|| maximum > std::numeric_limits<int>::max() / bid
				|| maximum > std::numeric_limits<int>::max() / offer)))
			return false;
		for(std::size_t i = 0; i < rows.size(); ++i)
			if(rows[i].id < 0 || rows[i].balance < 0 || rows[i].name.size() > 256
				|| std::any_of(rows.begin(), rows.begin() + i, [&](const auto & row) { return row.id == rows[i].id; }))
				return false;
		return (selling == -1 || has(selling, true)) && (buying == -1 || has(buying, false))
			&& (maximum == 0 || (selling >= 0 && buying >= 0 && selling != buying
				&& std::any_of(rows.begin(), rows.end(), [&](const auto & row)
					{ return row.id == selling && maximum <= row.balance / bid; })));
	}
	bool has(int id, bool sell) const
	{
		return std::any_of(rows.begin(), rows.end(), [&](const auto & row)
			{ return row.id == id && (sell ? row.sell : row.buy); });
	}
	bool allows(int command, int target) const
	{
		if(!valid() || !ThorExtended::validTarget(command, target)) return false;
		if(command == ThorExtended::MARKET_CLOSE) return true;
		if(!controllable || status == 1) return false;
		switch(command)
		{
		case ThorExtended::MARKET_SELL: return has(target, true);
		case ThorExtended::MARKET_BUY: return has(target, false);
		case ThorExtended::MARKET_AMOUNT: return maximum > 0 && target <= maximum;
		case ThorExtended::MARKET_TRADE: return amount > 0 && maximum >= amount;
		case ThorExtended::MARKET_RESET: return true;
		default: return false;
		}
	}
};
