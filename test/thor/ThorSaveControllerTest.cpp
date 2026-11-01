#include "StdInc.h"
#include "../../client/thor/ThorSaveController.h"
#include "../../lib/thor/ThorSave.h"

namespace
{
	struct SaveJourney
	{
		int owner = 1, parent = 2, saves = 0, closes = 0, selections = 0, names = 0;
		ThorContextStore store;
		ThorSaveController controller;
		ThorContextRecord context;
		SaveJourney()
		{
			context.contextId = ThorContextIds::SAVE_GAME;
			context.nativeOwnerToken = &owner;
			context.nativeParentToken = &parent;
			context.browserPageCount = 3;
			context.browserEntries = {{4, "保存.vsgm1", true, true}};
			context.browserNativeKeys = {"Saves/test"};
			context.enabledActionMask = thorActionMask(ThorAction::WINDOW_NEXT) | thorActionMask(ThorAction::WINDOW_CONFIRM)
				| thorActionMask(ThorAction::WINDOW_CLOSE) | thorActionMask(ThorAction::LOAD_BROWSER_SELECT)
				| thorActionMask(ThorAction::LOAD_BROWSER_NEXT_PAGE);
			context = store.publishNext(context);
		}
		ThorActionRequest request(ThorAction action)
		{
			ThorActionRequest r;
			r.revision = context.revision;
			r.action = action;
			return r;
		}
		bool run(const ThorActionRequest & request, bool live = true)
		{
			return controller.execute(request, context, &owner, &parent, live, [&]()
			{
				if(request.action == ThorAction::WINDOW_CONFIRM) ++saves;
				if(request.action == ThorAction::WINDOW_CLOSE) ++closes;
				if(request.action == ThorAction::LOAD_BROWSER_SELECT) ++selections;
				if(request.action == ThorAction::WINDOW_NEXT) { ++names; context.details[0] = request.text; }
				++context.actionEpoch;
				context = store.publishNext(context);
				return true;
			});
		}
	};
}

TEST(ThorActionSaveJourney, FilenameSelectionSaveAndCancelUseOneShotCallbacks)
{
	SaveJourney flow;
	auto edit = flow.request(ThorAction::WINDOW_NEXT);
	edit.text = "Nyt spil 保存 😀";
	ASSERT_TRUE(flow.run(edit));
	EXPECT_FALSE(flow.run(edit));
	EXPECT_EQ(flow.names, 1);
	auto select = flow.request(ThorAction::LOAD_BROWSER_SELECT);
	select.targetId = 4;
	ASSERT_TRUE(flow.run(select));
	EXPECT_FALSE(flow.run(select));
	EXPECT_EQ(flow.selections, 1);
	auto save = flow.request(ThorAction::WINDOW_CONFIRM);
	ASSERT_TRUE(flow.run(save));
	EXPECT_FALSE(flow.run(save));
	EXPECT_EQ(flow.saves, 1);
	ASSERT_TRUE(flow.run(flow.request(ThorAction::WINDOW_CLOSE)));
	EXPECT_EQ(flow.closes, 1);
}

TEST(ThorActionSaveJourney, ChangedOwnerParentListAndUnavailableActionsFailClosed)
{
	SaveJourney flow;
	auto old = flow.request(ThorAction::WINDOW_CONFIRM);
	int other = 7;
	EXPECT_FALSE(flow.controller.execute(old, flow.context, &other, &flow.parent, true, [] { return true; }));
	EXPECT_FALSE(flow.controller.execute(old, flow.context, &flow.owner, &other, true, [] { return true; }));
	EXPECT_FALSE(flow.run(old, false));
	flow.context.browserNativeKeys[0] = "Saves/replaced";
	flow.context = flow.store.publishNext(flow.context);
	EXPECT_FALSE(flow.run(old));
	auto select = flow.request(ThorAction::LOAD_BROWSER_SELECT);
	select.targetId = 3;
	EXPECT_FALSE(flow.run(select));
	flow.context.enabledActionMask = thorActionMask(ThorAction::WINDOW_CLOSE);
	flow.context = flow.store.publishNext(flow.context);
	EXPECT_FALSE(flow.run(flow.request(ThorAction::WINDOW_CONFIRM)));
	EXPECT_TRUE(flow.run(flow.request(ThorAction::WINDOW_CLOSE)));
	EXPECT_EQ(flow.saves, 0);
}

TEST(ThorActionSaveJourney, EmptySingleAndPagedSnapshotsWithdrawOnModalAndRestoreParent)
{
	SaveJourney flow;
	const auto parent = flow.context;
	EXPECT_EQ(parent.nativeOwnerToken, &flow.owner);
	EXPECT_EQ(parent.browserEntries.size(), 1);
	EXPECT_EQ(parent.browserPageCount, 3);
	ThorContextRecord modal;
	modal.contextId = ThorContextIds::DIALOG_CONFIRMATION;
	modal.menuModalType = ThorMenuModalType::CONFIRMATION;
	modal.nativeOwnerToken = &flow.parent;
	modal.nativeParentToken = &flow.owner;
	modal = flow.store.publishNext(modal);
	EXPECT_TRUE(modal.browserEntries.empty());
	flow.context = flow.store.publishNext(parent);
	EXPECT_GT(flow.context.revision, modal.revision);
	EXPECT_FALSE(flow.run({parent.revision, ThorAction::WINDOW_CONFIRM}));
	flow.context.browserEntries.clear();
	flow.context.browserNativeKeys.clear();
	flow.context.browserPageCount = 1;
	flow.context.enabledActionMask = thorActionMask(ThorAction::WINDOW_NEXT) | thorActionMask(ThorAction::WINDOW_CLOSE);
	flow.context = flow.store.publishNext(flow.context);
	EXPECT_TRUE(flow.context.browserEntries.empty());
	EXPECT_FALSE(flow.run(flow.request(ThorAction::WINDOW_CONFIRM)));
}

TEST(ThorActionSaveJourney, FilenameBoundsUnicodeAndMalformedPayloads)
{
	for(const auto & text : std::vector<std::string>{"", ".", "..", std::string(241, 'x'), "bad\nname", std::string("a\0b", 3), "\xc0\xaf", "\xed\xa0\x80"})
		EXPECT_FALSE(thorSaveFilenamePayloadValid(text));
	EXPECT_TRUE(thorSaveFilenamePayloadValid(std::string(240, 'x')));
	EXPECT_TRUE(thorSaveFilenamePayloadValid("保存 😀"));
	SaveJourney flow;
	auto bad = flow.request(ThorAction::WINDOW_NEXT);
	EXPECT_FALSE(flow.run(bad));
	bad.text = std::string(241, 'x');
	EXPECT_FALSE(flow.run(bad));
	ThorActionQueue queue;
	EXPECT_FALSE(queue.submit(bad));
	bad = flow.request(ThorAction::WINDOW_CONFIRM);
	bad.text = "unexpected";
	EXPECT_FALSE(flow.run(bad));
	bad = flow.request(ThorAction::WINDOW_NEXT);
	bad.text = "valid";
	bad.targetId = 4;
	EXPECT_FALSE(flow.run(bad));
	EXPECT_TRUE(flow.run(flow.request(ThorAction::WINDOW_CLOSE)));
}

TEST(ThorActionSaveJourney, ConsumesRevisionEvenWhenCallbackRejects)
{
	SaveJourney flow;
	auto request = flow.request(ThorAction::WINDOW_CONFIRM);
	EXPECT_FALSE(flow.controller.execute(request, flow.context, &flow.owner, &flow.parent, true, [] { return false; }));
	EXPECT_FALSE(flow.run(request));
	EXPECT_EQ(flow.saves, 0);
	++flow.context.actionEpoch;
	flow.context = flow.store.publishNext(flow.context);
	EXPECT_TRUE(flow.run(flow.request(ThorAction::WINDOW_CLOSE)));
}

TEST(ThorActionSaveJourney, MalformedSnapshotsDisableSaveAndFilenameActions)
{
	SaveJourney flow;
	flow.context.browserEntries.push_back(flow.context.browserEntries[0]);
	flow.context.browserNativeKeys.push_back("duplicate");
	flow.context = flow.store.publishNext(flow.context);
	EXPECT_EQ(flow.context.enabledActionMask, 0);
	EXPECT_TRUE(flow.context.browserEntries.empty());
	EXPECT_FALSE(flow.run(flow.request(ThorAction::WINDOW_CONFIRM)));
}

TEST(ThorActionSaveJourney, LoadRemainsOnNativeLobbyAuthorityAfterDeparture)
{
	SaveJourney flow;
	const auto oldSave = flow.request(ThorAction::WINDOW_CONFIRM);
	flow.context.contextId = ThorContextIds::LOBBY_LOAD_GAME_SCENARIO;
	flow.context.enabledActionMask = thorActionMask(ThorAction::LOBBY_BACK) | thorActionMask(ThorAction::LOBBY_START_GAME)
		| thorActionMask(ThorAction::LOAD_BROWSER_SELECT);
	flow.context = flow.store.publishNext(flow.context);
	EXPECT_FALSE(flow.run(oldSave));
	EXPECT_EQ(validateThorActionRequest({flow.context.revision, ThorAction::LOBBY_START_GAME}, flow.context), ThorActionValidation::VALID);
	EXPECT_EQ(validateThorActionRequest({flow.context.revision, ThorAction::LOBBY_BACK}, flow.context), ThorActionValidation::VALID);
	EXPECT_EQ(validateThorActionRequest({flow.context.revision, ThorAction::LOAD_BROWSER_SELECT, 4}, flow.context), ThorActionValidation::VALID);
	flow.context.enabledActionMask &= ~thorActionMask(ThorAction::LOBBY_START_GAME);
	flow.context = flow.store.publishNext(flow.context);
	EXPECT_EQ(validateThorActionRequest({flow.context.revision, ThorAction::LOBBY_START_GAME}, flow.context), ThorActionValidation::UNAVAILABLE);
}
