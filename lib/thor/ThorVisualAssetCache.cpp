#include "ThorVisualAssetCache.h"

#include <algorithm>
#include <iterator>
#include <unordered_set>

bool isThorVisualAssetPayloadValid(const ThorVisualAssetPayload & payload)
{
	if(!isThorVisualAssetKey(payload.key))
		return false;
	if(payload.pngBytes.empty())
		return payload.width == 0 && payload.height == 0;
	return payload.width > 0 && payload.height > 0
		&& payload.width <= THOR_VISUAL_ASSET_MAX_DIMENSION && payload.height <= THOR_VISUAL_ASSET_MAX_DIMENSION
		&& payload.pngBytes.size() <= THOR_VISUAL_ASSET_MAX_PAYLOAD_BYTES;
}

std::string_view thorVisualAssetAnimationName(ThorVisualAssetKind kind)
{
	switch(kind)
	{
	case ThorVisualAssetKind::CREATURE:
		return "CPRSMALL";
	case ThorVisualAssetKind::ARTIFACT:
		return "Artifact";
	case ThorVisualAssetKind::HERO:
		return "PortraitsSmall";
	}
	return {};
}

std::vector<std::uint64_t> collectThorHeroMeetingVisualAssetKeys(
	const ThorHeroMeetingArmies & armies, const ThorHeroMeetingArtifacts & artifacts)
{
	std::vector<std::uint64_t> result;
	std::unordered_set<std::uint64_t> seen;
	result.reserve(THOR_MAX_HERO_MEETING_VISUAL_ASSETS);
	seen.reserve(THOR_MAX_HERO_MEETING_VISUAL_ASSETS);
	const auto append = [&](std::uint64_t key)
	{
		if(key == 0)
			return true;
		if(!isThorVisualAssetKey(key))
			return false;
		if(seen.insert(key).second)
			result.push_back(key);
		return result.size() <= THOR_MAX_HERO_MEETING_VISUAL_ASSETS;
	};
	for(const auto key : armies.heroPortraitAssetKeys)
		if(!append(key))
			return {};

	for(const auto & slot : armies.leftSlots)
		if(!append(slot.occupied ? slot.visualAssetKey : 0))
			return {};
	for(const auto & slot : armies.rightSlots)
		if(!append(slot.occupied ? slot.visualAssetKey : 0))
			return {};
	const auto armyAssetCount = result.size();
	if(artifacts.artifactSlots.size() == THOR_HERO_MEETING_ARTIFACT_COUNT)
		for(const auto & artifact : artifacts.artifactSlots)
			if(!append(artifact.occupied ? artifact.visualAssetKey : 0))
			{
				result.resize(armyAssetCount);
				return result;
			}
	return result;
}

std::vector<std::uint64_t> collectThorContextVisualAssetKeys(const ThorContextRecord & context)
{
	if(context.contextId == ThorContextIds::HERO_MEETING)
	{
		if(!context.heroMeetingArmies)
			return {};
		return collectThorHeroMeetingVisualAssetKeys(*context.heroMeetingArmies,
			context.heroMeetingArtifacts.value_or(ThorHeroMeetingArtifacts{}));
	}

	if((context.contextId == ThorContextIds::ADVENTURE_MAP || context.contextId == ThorContextIds::HERO_WINDOW)
		&& isThorHeroPortraitVisualAssetKey(context.heroPortraitAssetKey))
		return {context.heroPortraitAssetKey};
	return {};
}

void ThorVisualAssetCache::touch(StoredEntry & entry)
{
	leastRecentFirst.splice(leastRecentFirst.end(), leastRecentFirst, entry.order);
	entry.order = std::prev(leastRecentFirst.end());
}

void ThorVisualAssetCache::trim()
{
	while(entries.size() > THOR_VISUAL_ASSET_CACHE_MAX_ENTRIES || currentBytes > THOR_VISUAL_ASSET_CACHE_MAX_BYTES)
	{
		const auto oldest = leastRecentFirst.front();
		const auto found = entries.find(oldest);
		if(found != entries.end())
		{
			currentBytes -= found->second.entry.size;
			entries.erase(found);
		}
		leastRecentFirst.pop_front();
	}
}

bool ThorVisualAssetCache::put(ThorVisualAssetPayload payload)
{
	if(!isThorVisualAssetPayloadValid(payload))
		return false;

	std::lock_guard lock(mutex);
	if(auto existing = entries.find(payload.key); existing != entries.end())
	{
		touch(existing->second);
		return true;
	}
	const auto key = payload.key;
	const auto size = payload.pngBytes.size();
	leastRecentFirst.push_back(key);
	auto order = std::prev(leastRecentFirst.end());
	entries.emplace(key, StoredEntry{Entry{std::move(payload), size}, order});
	currentBytes += size;
	trim();
	return entries.contains(key);
}

std::optional<ThorVisualAssetPayload> ThorVisualAssetCache::get(std::uint64_t key)
{
	std::lock_guard lock(mutex);
	auto found = entries.find(key);
	if(found == entries.end())
		return std::nullopt;
	touch(found->second);
	return found->second.entry.payload;
}

std::size_t ThorVisualAssetCache::size() const
{
	std::lock_guard lock(mutex);
	return entries.size();
}

std::size_t ThorVisualAssetCache::bytes() const
{
	std::lock_guard lock(mutex);
	return currentBytes;
}
