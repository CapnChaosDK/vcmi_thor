#pragma once

#include "ThorExtendedCommand.h"
#include <string>

/// Known spells from the exact native book, with native costs and eligibility.
struct DLL_LINKAGE ThorSpellbookRow
{
	int id = -1, level = 0, cost = 0;
	bool combat = false, castable = false;
	std::vector<int> schools;
	std::string name, description;
	bool operator==(const ThorSpellbookRow &) const = default;
};
struct DLL_LINKAGE ThorSpellbook
{
	static constexpr int MAX_ROWS = 256, MAX_SCHOOLS = 32;
	int hero = -1, mana = 0;
	bool battle = false;
	std::vector<int> schools;
	std::vector<std::string> schoolNames;
	std::string heroName;
	std::vector<ThorSpellbookRow> rows;
	bool operator==(const ThorSpellbook &) const = default;
	bool valid() const
	{
		if(hero < 0 || mana < 0 || heroName.empty() || heroName.size() > 256 || rows.size() > MAX_ROWS
			|| schools.size() > MAX_SCHOOLS || schoolNames.size() != schools.size()) return false;
		for(std::size_t i = 0; i < schools.size(); ++i)
			if(schools[i] < 0 || schoolNames[i].empty() || schoolNames[i].size() > 256
				|| std::find(schools.begin(), schools.begin() + i, schools[i]) != schools.begin() + i) return false;
		for(std::size_t i = 0; i < rows.size(); ++i)
		{
			const auto & row = rows[i];
			if(row.id < 0 || row.level < 0 || row.level > 5 || row.cost < 0 || row.name.empty()
				|| row.name.size() > 256 || row.description.size() > 4096 || row.schools.size() > MAX_SCHOOLS
				|| (row.castable && (row.combat != battle || row.cost > mana))
				|| std::any_of(rows.begin(), rows.begin() + i, [&](const auto & other) { return row.id == other.id; })) return false;
			for(std::size_t j = 0; j < row.schools.size(); ++j)
				if(std::find(schools.begin(), schools.end(), row.schools[j]) == schools.end()
					|| std::find(row.schools.begin(), row.schools.begin() + j, row.schools[j]) != row.schools.begin() + j) return false;
		}
		return true;
	}
	bool allows(int command, int target) const
	{
		if(!valid() || !ThorExtended::validTarget(command, target)) return false;
		if(command == ThorExtended::SPELLBOOK_CLOSE) return true;
		return command == ThorExtended::SPELLBOOK_CAST && std::any_of(rows.begin(), rows.end(),
			[&](const auto & row) { return row.id == target && row.castable; });
	}
};
