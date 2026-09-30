/*
 * CCampaignScreen.cpp, part of VCMI engine
 *
 * Authors: listed in file AUTHORS in main folder
 *
 * License: GNU General Public License v2.0 or later
 * Full text of license available in license.txt file, in main folder
 *
 */

#include "StdInc.h"
#include "CCampaignScreen.h"

#include "CMainMenu.h"

#include "../CPlayerInterface.h"
#include "../CServerHandler.h"
#include "../GameEngine.h"
#include "../gui/Shortcut.h"
#include "media/IMusicPlayer.h"
#include "render/Canvas.h"
#include "../widgets/CComponent.h"
#include "../widgets/Buttons.h"
#include "../widgets/MiscWidgets.h"
#include "../widgets/ObjectLists.h"
#include "../widgets/TextControls.h"
#include "../widgets/VideoWidget.h"
#include "../windows/GUIClasses.h"
#include "../windows/InfoWindows.h"
#include "../windows/CWindowObject.h"

#include "../../lib/CConfigHandler.h"
#include "../../lib/CCreatureHandler.h"
#include "../../lib/CSkillHandler.h"
#include "../../lib/GameLibrary.h"
#include "../../lib/IGameSettings.h"
#include "../../lib/campaign/CampaignHandler.h"
#include "../../lib/filesystem/Filesystem.h"
#include "../../lib/mapObjects/CGHeroInstance.h"
#include "../../lib/mapping/CMapService.h"
#include "../../lib/texts/CGeneralTextHandler.h"
#include "../../lib/texts/CompositeTranslator.h"
#include "../GameInstance.h"
#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
#include "../gui/WindowHandler.h"
#include "../../lib/CAndroidVMHelper.h"
#include "../../lib/thor/ThorAction.h"
#include "../../lib/thor/ThorContext.h"
#endif

CCampaignScreen::CCampaignScreen(const JsonNode & config, std::string name)
	: CWindowObject(BORDERED), campaignSet(name)
{
	OBJECT_CONSTRUCTION;
	
	const auto& campaigns = config[name]["items"].Vector();

	// Define mapping of background name -> campaigns per page
	const std::unordered_map<std::string, int> campaignsPerPageMap = {
		{"CampaignBackground4", 4},
		{"CampaignBackground5", 5},
		{"CampaignBackground6", 6},
		{"CampaignBackground7", 7},
		{"CAMPBACK", 7},
		{"CAMPBKX2", 7},
		{"CampaignBackground8", 8}
	};

	// Process images and check if name is in the map
	for (const JsonNode& node : config[name]["images"].Vector())
	{
		images.push_back(CMainMenu::createPicture(node));

		std::string imageName = node["name"].String();
		auto it = campaignsPerPageMap.find(imageName);
		if (it != campaignsPerPageMap.end())
		{
			campaignsPerPage = it->second;
		}
	}

	if (!images.empty())
	{
		images[0]->center(); // move background to center
		moveTo(images[0]->pos.topLeft()); // move everything else to center
		images[0]->moveTo(pos.topLeft()); // restore moved twice background
		pos = images[0]->pos; // fix height\width of this window
	}
	
	for (const auto& node : campaigns)
	{
		auto button = std::make_shared<CCampaignButton>(node, config, campaignSet, this);
		button->enable();
		campButtons.push_back(button);
	}

	maxPages = (campaigns.size() + campaignsPerPage - 1) / campaignsPerPage;
	
	if (!config[name]["nextbutton"].isNull())
	{
		buttonNext = std::make_shared<CButton>(
			Point(config[name]["nextbutton"]["x"].Integer(), config[name]["nextbutton"]["y"].Integer()),
			AnimationPath::fromJson(config[name]["nextbutton"]["name"]),
			std::make_pair("", ""),
			[this, name]() { switchPage(1); }
		);
		buttonNext->setHoverable(true);
		buttonNext->disable();
	}

	if (!config[name]["backbutton"].isNull())
	{
		buttonPrev = std::make_shared<CButton>(
			Point(config[name]["backbutton"]["x"].Integer(), config[name]["backbutton"]["y"].Integer()),
			AnimationPath::fromJson(config[name]["backbutton"]["name"]),
			std::make_pair("", ""),
			[this, name]() { switchPage(-1); }
		);
		buttonPrev->setHoverable(true);
		buttonPrev->disable();
	}

	page = std::make_shared<CLabel>(10, 570, FONT_MEDIUM, ETextAlignment::BOTTOMLEFT, Colors::YELLOW, "");

	if (!config[name]["exitbutton"].isNull())
	{
		buttonBack = createExitButton(config[name]["exitbutton"]);
		buttonBack->setHoverable(true);
	}

	updateCampaignButtons(config);
}

void CCampaignScreen::activate()
{
	ENGINE->music().playMusic(AudioPath::builtin("Music/MainMenu"), true, false);

	CWindowObject::activate();
#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
	publishThorContext();
#endif
}

void CCampaignScreen::deactivate()
{
	CWindowObject::deactivate();
#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
	for(const auto & button : campButtons)
		button->hover(false);
	thorActionQueue().clear();
	ThorContextRecord context;
	context = thorContextStore().publishNext(std::move(context));
	CAndroidVMHelper().publishThorContext(context.revision, context.contextId, context.title, context.status);
#endif
}

std::shared_ptr<CButton> CCampaignScreen::createExitButton(const JsonNode & button)
{
	std::pair<std::string, std::string> help;
	if(!button["help"].isNull() && button["help"].Float() > 0)
		help = LIBRARY->generaltexth->zelp[(size_t)button["help"].Float()];

	return std::make_shared<CButton>(Point((int)button["x"].Float(), (int)button["y"].Float()), AnimationPath::fromJson(button["name"]), help, [this](){ close();}, EShortcut::GLOBAL_CANCEL);
}

CCampaignScreen::CCampaignButton::CCampaignButton(const JsonNode & config, const JsonNode & parentConfig,
	std::string campaignSet, CCampaignScreen * screen)
	: screen(screen), campaignSet(campaignSet)
{
	OBJECT_CONSTRUCTION;

	pos.x += static_cast<int>(config["x"].Float());
	pos.y += static_cast<int>(config["y"].Float());
	pos.w = 200;
	pos.h = 116;

	campFile = config["file"].String();
	campaignId = config["id"].Integer();
	videoPath = VideoPath::fromJson(config["video"]);

	status = CCampaignScreen::ENABLED;

	if(CResourceHandler::get()->existsResource(ResourcePath(campFile, EResType::CAMPAIGN)))
	{
		auto header = CampaignHandler::getHeader(campFile);
		// the header is local to this scope, so its own texts are the only place its name lives
		CompositeTranslator translator;
		translator.install(header->getTexts());
		hoverText = header->getNameTranslated(&translator);

		if (persistentStorage["completedCampaigns"][header->getFilename()].Bool())
			status = CCampaignScreen::COMPLETED;
	}
	else
	{
		status = CCampaignScreen::DISABLED;
	}

	for(const JsonNode & node : parentConfig[campaignSet]["items"].Vector())
	{
		for(const JsonNode & requirement : config["requires"].Vector())
		{
			if(node["id"].Integer() == requirement.Integer())
				if(!persistentStorage["completedCampaigns"][node["file"].String()].Bool())
					status = CCampaignScreen::DISABLED;
		}
	}

	if(LIBRARY->engineSettings()->getBoolean(EGameSettings::CAMPAIGN_UNLOCK_ALL))
		status = CCampaignScreen::ENABLED;

	if(status != CCampaignScreen::DISABLED)
	{
		addUsedEvents(LCLICK | HOVER);
		graphicsImage = std::make_shared<CPicture>(ImagePath::fromJson(config["image"]));
		hoverLabel = std::make_shared<CLabel>(pos.w / 2, pos.h + 20, FONT_MEDIUM, ETextAlignment::CENTER, Colors::YELLOW, "");
		parent->addChild(hoverLabel.get());
	}

	if(status == CCampaignScreen::COMPLETED)
		graphicsCompleted = std::make_shared<CPicture>(ImagePath::builtin("CAMPCHK"));
}

void CCampaignScreen::CCampaignButton::clickReleased(const Point & cursorPosition)
{
	CMainMenu::openCampaignLobby(campFile, campaignSet);
}

void CCampaignScreen::CCampaignButton::hover(bool on)
{
	OBJECT_CONSTRUCTION;
#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
	const bool hadVideo = videoPlayer != nullptr;
#endif

	if (on && !videoPath.empty())
		videoPlayer = std::make_shared<VideoWidget>(Point(), videoPath, false);
	else
		videoPlayer.reset();

	if(hoverLabel)
	{
		if(on)
			hoverLabel->setText(hoverText); // Shows the name of the campaign when you get into the bounds of the button
		else
			hoverLabel->setText(" ");
	}
#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
	if(screen && hadVideo != (videoPlayer != nullptr))
		screen->publishThorContext();
#endif
}

void CCampaignScreen::switchPage(int delta)
{
	currentPage += delta;
	currentPage = std::clamp(currentPage, 0, maxPages - 1);

	const auto& campaignConfig = CMainMenuConfig::get().getCampaigns();

	updateCampaignButtons(campaignConfig);
}

void CCampaignScreen::updateCampaignButtons(const JsonNode & parentConfig)
{
	const auto& campaigns = parentConfig[campaignSet]["items"].Vector();

	int minId = (currentPage * campaignsPerPage) + 1;
	int maxId = minId + campaignsPerPage - 1;

	for(size_t i = 0; i < campButtons.size(); ++i)
	{
		int campaignId = campaigns[i]["id"].Integer();

		if(campaignId >= minId && campaignId <= maxId)
			campButtons[i]->enable();
		else
		{
			campButtons[i]->hover(false); // A disabled button cannot receive the hover-off event later.
			campButtons[i]->disable();
		}

		if(!CResourceHandler::get()->existsResource(ResourcePath(campaigns[i]["file"].String(), EResType::CAMPAIGN)))
		{
			campButtons[i]->disable();
			logGlobal->warn("Campaign %s doesn't exist", campaigns[i]["file"].String());
		}
	}

	if(buttonNext && buttonPrev)
	{
		page->setText(std::to_string(currentPage + 1) + "/" + std::to_string(maxPages));

		if (maxId < campaigns.size())
			buttonNext->enable();
		else
			buttonNext->disable();

		if (currentPage > 0)
			buttonPrev->enable();
		else
			buttonPrev->disable();
	}

	redraw();
#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
	publishThorContext();
#endif
}

#if defined(VCMI_ANDROID) && defined(TARGET_AYN_THOR)
void CCampaignScreen::publishThorContext()
{
	if(!isActive() || ENGINE->windows().topWindow<CCampaignScreen>().get() != this)
		return;
	ThorContextRecord context;
	context.contextId = ThorContextIds::CAMPAIGN_BROWSER;
	if(buttonBack)
		context.enabledActionMask = thorActionMask(ThorAction::CAMPAIGN_BROWSER_BACK);
	const auto & campaignItems = CMainMenuConfig::get().getCampaigns()[campaignSet]["items"];
	if(!campaignItems.isVector())
	{
		const auto previous = thorContextStore().snapshot();
		context = thorContextStore().publishNext(std::move(context));
		if(context.revision != previous.revision)
			thorActionQueue().clear();
		CAndroidVMHelper().publishThorContext(context.revision, context.contextId, context.title, context.status);
		CAndroidVMHelper().publishThorActionState(context.revision, context.enabledActionMask, 0);
		CAndroidVMHelper().publishThorBrowser(context.revision, 0, 0, {});
		return;
	}
	const auto & campaigns = campaignItems.Vector();
	const bool validPage = campaignsPerPage > 0 && campaignsPerPage <= static_cast<int>(THOR_BROWSER_MAX_ROWS)
		&& !campaigns.empty() && campaigns.size() <= 128 && campaigns.size() == campButtons.size()
		&& maxPages == static_cast<int>((campaigns.size() + campaignsPerPage - 1) / campaignsPerPage)
		&& currentPage >= 0 && currentPage < maxPages;
	if(validPage)
	{
		std::vector<bool> seen(campaigns.size() + 1);
		bool validItems = true;
		for(std::size_t index = 0; index < campaigns.size(); ++index)
		{
			const int id = campaigns[index]["id"].Integer();
			const std::string file = campaigns[index]["file"].String();
			if(id < 1 || id > static_cast<int>(campaigns.size()) || seen[id] || file.empty()
				|| id != campButtons[index]->campaignId || file != campButtons[index]->campFile)
			{
				validItems = false;
				break;
			}
			seen[id] = true;
		}
		if(validItems)
		{
			context.browserPage = currentPage;
			context.browserPageCount = maxPages;
			const int firstId = currentPage * campaignsPerPage + 1;
			for(std::size_t index = 0; index < campaigns.size(); ++index)
			{
				const int id = campButtons[index]->campaignId;
				if(id < firstId || id >= firstId + campaignsPerPage)
					continue;
				const auto & button = *campButtons[index];
				const bool enabled = button.status != DISABLED
					&& CResourceHandler::get()->existsResource(ResourcePath(button.campFile, EResType::CAMPAIGN));
				context.browserEntries.push_back({static_cast<int>(index), button.hoverText, enabled,
					false, button.status == COMPLETED});
				context.browserNativeKeys.push_back(std::to_string(id) + ":" + button.campFile);
				if(enabled)
					context.enabledActionMask |= thorActionMask(ThorAction::CAMPAIGN_BROWSER_SELECT);
			}
			if(currentPage > 0 && buttonPrev)
				context.enabledActionMask |= thorActionMask(ThorAction::CAMPAIGN_BROWSER_PREVIOUS_PAGE);
			if(currentPage + 1 < maxPages && buttonNext)
				context.enabledActionMask |= thorActionMask(ThorAction::CAMPAIGN_BROWSER_NEXT_PAGE);
		}
	}
	if(std::any_of(campButtons.begin(), campButtons.end(), [](const auto & button)
		{
			return button->videoPlayer != nullptr;
		}))
		context.enabledActionMask = 0;
	const auto previous = thorContextStore().snapshot();
	context = thorContextStore().publishNext(std::move(context));
	if(context.revision != previous.revision)
		thorActionQueue().clear();
	CAndroidVMHelper bridge;
	bridge.publishThorContext(context.revision, context.contextId, context.title, context.status);
	bridge.publishThorActionState(context.revision, context.enabledActionMask, 0);
	bridge.publishThorBrowser(context.revision, context.browserPage, context.browserPageCount, context.browserEntries);
}

bool CCampaignScreen::executeThorAction(const ThorActionRequest & request)
{
	const auto before = thorContextStore().snapshot();
	publishThorContext();
	const auto context = thorContextStore().snapshot();
	if(!isActive() || ENGINE->windows().topWindow<CCampaignScreen>().get() != this
		|| before.revision != context.revision
		|| validateThorActionRequest(request, context) != ThorActionValidation::VALID)
		return false;
	if(request.action == ThorAction::CAMPAIGN_BROWSER_PREVIOUS_PAGE
		|| request.action == ThorAction::CAMPAIGN_BROWSER_NEXT_PAGE)
	{
		const int delta = request.action == ThorAction::CAMPAIGN_BROWSER_NEXT_PAGE ? 1 : -1;
		if(context.browserPage + delta < 0 || context.browserPage + delta >= context.browserPageCount)
			return false;
		switchPage(delta);
		return true;
	}
	if(request.action == ThorAction::CAMPAIGN_BROWSER_BACK)
	{
		close();
		return true;
	}
	if(request.action != ThorAction::CAMPAIGN_BROWSER_SELECT || request.targetId < 0
		|| request.targetId >= static_cast<int>(campButtons.size()))
		return false;
	const auto row = std::find_if(context.browserEntries.begin(), context.browserEntries.end(), [&](const auto & entry)
	{
		return entry.target == request.targetId && entry.enabled;
	});
	if(row == context.browserEntries.end())
		return false;
	const auto rowIndex = static_cast<std::size_t>(row - context.browserEntries.begin());
	auto & button = *campButtons[request.targetId];
	if(rowIndex >= context.browserNativeKeys.size()
		|| context.browserNativeKeys[rowIndex] != std::to_string(button.campaignId) + ":" + button.campFile
		|| button.status == DISABLED
		|| !CResourceHandler::get()->existsResource(ResourcePath(button.campFile, EResType::CAMPAIGN)))
		return false;
	button.clickReleased(Point());
	return true;
}
#endif
