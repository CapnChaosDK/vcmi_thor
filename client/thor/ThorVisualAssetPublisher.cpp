/*
 * ThorVisualAssetPublisher.cpp, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 *
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 *
 */
#include "StdInc.h"

#include "ThorVisualAssetPublisher.h"

#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)

#include "GameEngine.h"
#include "render/IRenderHandler.h"
#include "render/Canvas.h"
#include "render/CanvasImage.h"
#include "../render/Colors.h"

#include "../../lib/CAndroidVMHelper.h"
#include "../../lib/GameLibrary.h"
#include "../../lib/VCMIDirs.h"
#include "../../lib/filesystem/Filesystem.h"
#include "../../lib/entities/artifact/CArtHandler.h"
#include "../../lib/entities/hero/CHeroHandler.h"
#include "../../lib/CCreatureHandler.h"
#include "../../lib/thor/ThorVisualAssetCache.h"

#include <fstream>

namespace
{
	ThorVisualAssetCache & thorVisualAssetCache()
	{
		static ThorVisualAssetCache cache;
		return cache;
	}

	ThorVisualAssetPayload unavailableThorVisualAsset(std::uint64_t key)
	{
		ThorVisualAssetPayload result;
		result.key = key;
		return result;
	}

	ThorVisualAssetPayload loadThorVisualAsset(std::uint64_t key)
	{
		ThorVisualAssetPayload result;
		result.key = key;
		if(!isThorVisualAssetKey(key) || !LIBRARY)
			return result;
		try
		{
			const auto kind = static_cast<ThorVisualAssetKind>(key >> 56);
			const auto typeId = static_cast<int>((key & 0xffffffffULL) - 1);
			AnimationPath path;
			int frame = -1;
			if(kind == ThorVisualAssetKind::CREATURE)
			{
				if(!LIBRARY->creh || typeId >= static_cast<int>(LIBRARY->creh->objects.size())
					|| !LIBRARY->creh->objects[typeId])
					return result;
				frame = LIBRARY->creh->objects[typeId]->getIconIndex();
			}
			else if(kind == ThorVisualAssetKind::ARTIFACT)
			{
				if(!LIBRARY->arth || typeId >= static_cast<int>(LIBRARY->arth->objects.size())
					|| !LIBRARY->arth->objects[typeId])
					return result;
				frame = LIBRARY->arth->objects[typeId]->getIconIndex();
			}
			else if(kind == ThorVisualAssetKind::HERO)
			{
				if(!LIBRARY->heroh || typeId >= static_cast<int>(LIBRARY->heroh->objects.size())
					|| !LIBRARY->heroh->objects[typeId])
					return result;
				frame = LIBRARY->heroh->objects[typeId]->getIconIndex();
			}
			const auto animationName = thorVisualAssetAnimationName(kind);
			if(frame < 0 || animationName.empty())
				return result;
			path = AnimationPath::builtin(std::string(animationName));

			const auto animation = ENGINE->renderHandler().loadAnimation(path, EImageBlitMode::COLORKEY);
			if(!animation || static_cast<std::size_t>(frame) >= animation->size())
				return result;
			const auto image = animation->getImage(static_cast<std::size_t>(frame), 0, false);
			if(!image || image->width() <= 0 || image->height() <= 0
				|| image->width() > static_cast<int>(THOR_VISUAL_ASSET_MAX_DIMENSION)
				|| image->height() > static_cast<int>(THOR_VISUAL_ASSET_MAX_DIMENSION))
				return result;

			boost::system::error_code error;
			const auto directory = VCMIDirs::get().userCachePath() / "thor-visual-assets";
			boost::filesystem::create_directories(directory, error);
			if(error)
				return result;
			const auto file = directory / boost::filesystem::unique_path("vcmi-thor-visual-%%%%-%%%%.png", error);
			if(error)
				return result;
			struct TemporaryImage
			{
				boost::filesystem::path path;
				~TemporaryImage()
				{
					boost::system::error_code cleanupError;
					boost::filesystem::remove(path, cleanupError);
				}
			} temporary{file};

			const Point dimensions(image->width(), image->height());
			auto bitmap = ENGINE->renderHandler().createImage(dimensions, CanvasScalingPolicy::IGNORE);
			{
				auto canvas = bitmap->getCanvas();
				canvas.drawColor(Rect(Point(0, 0), dimensions), Colors::TRANSPARENCY);
				canvas.draw(image, Point(0, 0));
			}
			bitmap->exportBitmap(file);
			const auto size = boost::filesystem::file_size(file, error);
			if(error || size == 0 || size > THOR_VISUAL_ASSET_MAX_PAYLOAD_BYTES)
				return result;
			std::ifstream input(file.string(), std::ios::binary);
			if(!input)
				return result;
			result.pngBytes.resize(static_cast<std::size_t>(size));
			input.read(reinterpret_cast<char *>(result.pngBytes.data()), static_cast<std::streamsize>(size));
			if(!input || static_cast<std::size_t>(input.gcount()) != result.pngBytes.size())
				return unavailableThorVisualAsset(key);
			result.width = static_cast<std::uint16_t>(image->width());
			result.height = static_cast<std::uint16_t>(image->height());
			if(!isThorVisualAssetPayloadValid(result))
				return unavailableThorVisualAsset(key);
			return result;
		}
		catch(const std::exception &)
		{
			return unavailableThorVisualAsset(key);
		}
	}
}

void publishThorVisualAssets(const ThorContextRecord & context)
{
	const auto keys = collectThorContextVisualAssetKeys(context);
	CAndroidVMHelper android;
	auto & cache = thorVisualAssetCache();
	for(const auto key : keys)
	{
		if(android.hasThorVisualAsset(key))
			continue;
		auto payload = cache.get(key);
		if(!payload)
		{
			payload = loadThorVisualAsset(key);
			cache.put(*payload);
		}
		android.publishThorVisualAsset(context.revision, *payload);
	}
}

#endif
