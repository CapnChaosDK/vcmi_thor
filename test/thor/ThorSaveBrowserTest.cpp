/*
 * ThorSaveBrowserTest.cpp, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 * License: GNU General Public License v2.0 or later
 */
#include "StdInc.h"
#include "../../client/thor/ThorSaveBrowser.h"
#include "../../lib/filesystem/CFilesystemLoader.h"

#include <fstream>

TEST(ThorSaveBrowserTest, UnrestrictedRootRefreshCannotExposeLegacySaves)
{
	const auto directory = boost::filesystem::temp_directory_path() / boost::filesystem::unique_path("thor-save-%%%%-%%%%");
	struct Cleanup
	{
		boost::filesystem::path path;
		~Cleanup() { boost::filesystem::remove_all(path); }
	} cleanup{directory};
	boost::filesystem::create_directories(directory / "Saves");
	boost::filesystem::create_directories(directory / "Saves-upstream");
	const auto legacy = directory / "Saves" / "legacy.vsgm1";
	std::ofstream(legacy.string()) << "preserve legacy bytes";
	std::ofstream((directory / "Saves-upstream" / "fresh.vsgm1").string()) << "fresh save";

	CFilesystemLoader initial("", directory, 1, true);
	CFilesystemLoader saves("SAVES/", directory / "Saves-upstream");
	const ResourcePath legacyId("Saves/legacy", EResType::SAVEGAME);
	const ResourcePath freshId("Saves/fresh", EResType::SAVEGAME);
	EXPECT_FALSE(initial.existsResource(legacyId));
	initial.updateFilteredFiles([](const std::string &) { return true; });
	ASSERT_TRUE(initial.existsResource(legacyId));
	EXPECT_EQ(findThorSaveFiles(saves, "SAVES/"), (std::unordered_set<ResourcePath>{freshId}));

	std::ofstream((directory / "Saves-upstream" / "later.vsgm1").string()) << "later save";
	const ResourcePath laterId("Saves/later", EResType::SAVEGAME);
	EXPECT_EQ(findThorSaveFiles(saves, "SAVES/"), (std::unordered_set<ResourcePath>{freshId, laterId}));
	std::ifstream oldFile(legacy.string());
	EXPECT_EQ(std::string(std::istreambuf_iterator<char>(oldFile), {}), "preserve legacy bytes");
}
