#pragma once

#include "ThorContext.h"

#include <list>
#include <mutex>
#include <optional>
#include <unordered_map>

inline constexpr std::size_t THOR_VISUAL_ASSET_MAX_DIMENSION = 64;
inline constexpr std::size_t THOR_VISUAL_ASSET_MAX_PAYLOAD_BYTES = 32 * 1024;
inline constexpr std::size_t THOR_VISUAL_ASSET_CACHE_MAX_ENTRIES = 64;
inline constexpr std::size_t THOR_VISUAL_ASSET_CACHE_MAX_BYTES = 1024 * 1024;

/// One decoded player-installed icon encoded as bounded PNG bytes for Android BitmapFactory.
struct DLL_LINKAGE ThorVisualAssetPayload
{
	std::uint64_t key = 0;
	std::uint16_t width = 0;
	std::uint16_t height = 0;
	std::vector<std::uint8_t> pngBytes;
	bool operator==(const ThorVisualAssetPayload &) const = default;
};

DLL_LINKAGE bool isThorVisualAssetPayloadValid(const ThorVisualAssetPayload & payload);
DLL_LINKAGE std::vector<std::uint64_t> collectThorHeroMeetingVisualAssetKeys(
	const ThorHeroMeetingArmies & armies, const ThorHeroMeetingArtifacts & artifacts);

/// Small process-local LRU for encoded player assets. Empty payloads cache a failed lookup.
class DLL_LINKAGE ThorVisualAssetCache final
{
	struct Entry
	{
		ThorVisualAssetPayload payload;
		std::size_t size = 0;
	};

	mutable std::mutex mutex;
	std::list<std::uint64_t> leastRecentFirst;
	struct StoredEntry
	{
		Entry entry;
		std::list<std::uint64_t>::iterator order;
	};
	std::unordered_map<std::uint64_t, StoredEntry> entries;
	std::size_t currentBytes = 0;

	void touch(StoredEntry & entry);
	void trim();

public:
	/// Adds a valid payload or a negative-cache marker (key set, zero dimensions, no bytes).
	/// Existing entries are reused and moved to the newest position.
	bool put(ThorVisualAssetPayload payload);
	std::optional<ThorVisualAssetPayload> get(std::uint64_t key);
	std::size_t size() const;
	std::size_t bytes() const;
};
