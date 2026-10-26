/*
 * ThorSaveBrowser.h, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 * License: GNU General Public License v2.0 or later
 */
#pragma once

#include "../../lib/filesystem/ISimpleResourceLoader.h"
#include "../../lib/filesystem/ResourcePath.h"

/// Only the dedicated save mount belongs to this upstream-base Thor installation.
inline std::unordered_set<ResourcePath> findThorSaveFiles(ISimpleResourceLoader & saves, const std::string & prefix)
{
	saves.updateFilteredFiles([&](const std::string & mount) { return mount.starts_with(prefix); });
	return saves.getFilteredFiles([&](const ResourcePath & resource)
	{
		return resource.getType() == EResType::SAVEGAME && resource.getName().starts_with(prefix);
	});
}
