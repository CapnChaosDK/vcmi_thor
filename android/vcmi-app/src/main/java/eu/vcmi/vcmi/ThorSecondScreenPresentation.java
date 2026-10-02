package eu.vcmi.vcmi;

import android.app.Presentation;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.Display;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;

final class ThorSecondScreenPresentation extends Presentation
{
    interface HapticsChangeListener
    {
        void onHapticsChanged(boolean enabled);
    }

    interface SessionValidity
    {
        boolean isCurrent(long sessionId);
    }

    // Measured AYN Thor lower panel reference: 1080 x 1240 px at approximately 369 dpi.
    // The shell scales from these proportions and remains safe on other presentation displays.
    private static final float REFERENCE_WIDTH = 1080f;
    private static final float REFERENCE_HEIGHT = 1240f;
    private ThorFoundationView foundationView;
    private final ThorVisualAssetCache<Bitmap> visualAssets;
    private final HapticsChangeListener hapticsChangeListener;
    private final boolean hapticsEnabled;
    private final long presentationSessionId;
    private final SessionValidity sessionValidity;

    ThorSecondScreenPresentation(final Context context, final Display display,
                                 final ThorVisualAssetCache<Bitmap> visualAssets,
                                 final boolean hapticsEnabled, final HapticsChangeListener hapticsChangeListener,
                                 final long presentationSessionId, final SessionValidity sessionValidity)
    {
        super(context, display);
        this.visualAssets = visualAssets;
        this.hapticsEnabled = hapticsEnabled;
        this.hapticsChangeListener = hapticsChangeListener;
        this.presentationSessionId = presentationSessionId;
        this.sessionValidity = sessionValidity;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        final Window window = getWindow();
        if (window != null)
        {
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }

        foundationView = new ThorFoundationView(getContext(), visualAssets, hapticsEnabled,
                hapticsChangeListener, presentationSessionId, sessionValidity);
        foundationView.setContentDescription(getContext().getString(R.string.thor_deck_title));
        setContentView(foundationView);
    }

    void updateContext(final long revision, final String contextId, final String title, final String status,
                       final long heroPortraitAssetKey, final String[] detailLines,
                       final long enabledActionMask, final long activeActionMask)
    {
        if (foundationView != null)
            foundationView.updateContext(revision, contextId, title, status, heroPortraitAssetKey, detailLines,
                    enabledActionMask, activeActionMask);
    }

    void updateHeroes(final ThorHeroRoster roster)
    {
        if (foundationView != null)
            foundationView.updateHeroes(roster);
    }

    void updateTowns(final ThorTownRoster roster)
    {
        if (foundationView != null)
            foundationView.updateTowns(roster);
    }

    void updateBrowser(final ThorBrowserState browser)
    {
        if (foundationView != null)
            foundationView.updateBrowser(browser);
    }

    void updateHeroMeetingArmies(final ThorHeroMeetingArmies armies)
    {
        if (foundationView != null)
            foundationView.updateHeroMeetingArmies(armies);
    }

    void updateHeroMeetingArtifacts(final ThorHeroMeetingArtifacts artifacts)
    {
        if (foundationView != null)
            foundationView.updateHeroMeetingArtifacts(artifacts);
    }

    void invalidateVisualAssets()
    {
        if (foundationView != null)
            foundationView.invalidate();
    }

    void clearTransientState()
    {
        if (foundationView != null)
            foundationView.clearTransientState();
    }

    void setHapticsEnabled(final boolean enabled, final boolean preview)
    {
        if (foundationView != null)
            foundationView.setHapticsEnabled(enabled, preview);
    }

    void performAcceptedHaptic()
    {
        if (foundationView != null)
            foundationView.performAcceptedHaptic();
    }

    int getAdventureTab()
    {
        return foundationView == null ? 0 : foundationView.adventureTab;
    }

    void setAdventureTab(final int value)
    {
        if (foundationView != null)
            foundationView.adventureTab = value;
    }

    private static final class ThorFoundationView extends View
    {
        private static final int BACKGROUND = Color.rgb(30, 31, 28);
        private static final int STONE_DARK = Color.rgb(55, 57, 52);
        private static final int STONE_LIGHT = Color.rgb(105, 105, 94);
        private static final int PARCHMENT = Color.rgb(189, 166, 119);
        private static final int PARCHMENT_DARK = Color.rgb(91, 69, 42);
        private static final int GOLD = Color.rgb(205, 166, 66);
        private static final int TEXT = Color.rgb(244, 229, 184);

        private final HapticsChangeListener hapticsChangeListener;
        private final long presentationSessionId;
        private final SessionValidity sessionValidity;
        private boolean hapticsEnabled;
        private final ThorHapticsToggleGesture hapticsToggleGesture = new ThorHapticsToggleGesture();

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final ThorVisualAssetCache<Bitmap> visualAssets;
        private String title;
        private String status;
        private long revision;
        private long heroPortraitAssetKey;
        private String contextId = ThorContextIds.UNKNOWN;
        private final String[] detailLines = new String[ThorContextDetails.COUNT];
        private long enabledActionMask;
        private long activeActionMask;
        private ThorHeroRoster heroes = ThorHeroRoster.EMPTY;
        private ThorTownRoster towns = ThorTownRoster.EMPTY;
        private ThorBrowserState browser = ThorBrowserState.EMPTY;
        private ThorHeroMeetingArmies heroMeetingArmies = ThorHeroMeetingArmies.EMPTY;
        private ThorHeroMeetingArtifacts heroMeetingArtifacts = ThorHeroMeetingArtifacts.EMPTY;
        private final ThorHeroMeetingModeState heroMeetingMode = new ThorHeroMeetingModeState();
        private int artifactPage;
        private int selectedArtifact = -1;
        private int adventureTab;
        private int townPage;
        private int selectedMeetingSlot = -1;
        private boolean pendingMeetingAction;
        private final ThorHeroMeetingGesture heroMeetingGesture = new ThorHeroMeetingGesture();
        private final ThorHeroMeetingArtifactGesture artifactGesture = new ThorHeroMeetingArtifactGesture();
        private final ThorHeroMeetingSplitState heroMeetingSplit = new ThorHeroMeetingSplitState();
        private final ThorHeroMeetingRedistributionState heroMeetingRedistribution =
                new ThorHeroMeetingRedistributionState();
        private final int touchSlop;
        private boolean heroMeetingTouchSequence;
        private boolean heroMeetingTouchCancelled;
        private long heroMeetingTouchRevision;
        private int heroMeetingPointerId = -1;
        private float artifactTouchDownX;
        private float artifactTouchDownY;
        private boolean artifactTouchMovedWithoutSource;
        private final ThorLobbyScenarioGesture lobbyScenarioGesture = new ThorLobbyScenarioGesture();
        private final ThorMainMenuGesture mainMenuGesture = new ThorMainMenuGesture();
        private final ThorBrowserGesture browserGesture = new ThorBrowserGesture();
        private final ThorWindowNavigation windowNavigation = new ThorWindowNavigation();
        private final ThorTownServices townServices = new ThorTownServices();
        private final Runnable heroMeetingLongPress = () ->
        {
            if (!heroMeetingRedistribution.isActive()
                    && heroMeetingMode.mode() == ThorHeroMeetingModeState.ARMY
                    && heroMeetingGesture.activateLongPress(revision, ThorContextIds.HERO_MEETING.equals(contextId)))
            {
                final int source = heroMeetingGesture.sourceKey();
                if (heroMeetingSplit.begin(revision, source, heroMeetingArmies.counts[source]))
                {
                    selectedMeetingSlot = source;
                    setContentDescription(commandDeckDescription());
                    invalidate();
                }
            }
        };

        ThorFoundationView(final Context context, final ThorVisualAssetCache<Bitmap> visualAssets,
                           final boolean hapticsEnabled, final HapticsChangeListener hapticsChangeListener,
                           final long presentationSessionId, final SessionValidity sessionValidity)
        {
            super(context);
            this.visualAssets = visualAssets;
            this.hapticsEnabled = hapticsEnabled;
            this.hapticsChangeListener = hapticsChangeListener;
            this.presentationSessionId = presentationSessionId;
            this.sessionValidity = sessionValidity;
            title = context.getString(R.string.thor_deck_title);
            status = context.getString(R.string.thor_deck_status);
            touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
            setBackgroundColor(BACKGROUND);
            setClickable(true);
            setFocusable(false);
            setHapticFeedbackEnabled(true);
        }

        void clearTransientState()
        {
            heroMeetingArmies = ThorHeroMeetingArmies.EMPTY;
            heroMeetingArtifacts = ThorHeroMeetingArtifacts.EMPTY;
            heroPortraitAssetKey = 0L;
            selectedArtifact = -1;
            selectedMeetingSlot = -1;
            pendingMeetingAction = false;
            heroMeetingSplit.cancel();
            heroMeetingRedistribution.cancel();
            cancelHeroMeetingGesture();
            cancelLobbyScenarioTouch();
            cancelMainMenuTouch();
            browserGesture.cancel();
            windowNavigation.cancel();
            townServices.cancel();
            invalidate();
        }

        void updateContext(final long revision, final String contextId, final String publishedTitle,
                           final String publishedStatus, final long publishedHeroPortraitAssetKey,
                           final String[] publishedDetails,
                           final long enabledActionMask, final long activeActionMask)
        {
            final boolean retainMeetingArmies = ThorHeroMeetingGesture.retainsArmies(
                    this.revision, this.contextId, revision, contextId);
            if (revision != this.revision || !contextId.equals(this.contextId))
            {
                cancelLobbyScenarioTouch();
                cancelMainMenuTouch();
                browserGesture.cancel();
                windowNavigation.cancel();
                townServices.cancel();
                heroes = ThorHeroRoster.EMPTY;
                towns = ThorTownRoster.EMPTY;
                heroMeetingArmies = ThorHeroMeetingArmies.EMPTY;
                heroMeetingArtifacts = ThorHeroMeetingArtifacts.EMPTY;
                browser = ThorBrowserState.EMPTY;
                artifactPage = 0;
                selectedArtifact = -1;
                townPage = 0;
                selectedMeetingSlot = -1;
                pendingMeetingAction = false;
                heroMeetingSplit.cancel();
                heroMeetingRedistribution.cancel();
                cancelHeroMeetingGesture();
            }
            if ((ThorContextIds.LOBBY_NEW_GAME_SCENARIO.equals(contextId)
                    || ThorContextIds.LOBBY_LOAD_GAME_SCENARIO.equals(contextId)
                    || ThorContextIds.CAMPAIGN_BONUS_SELECTION.equals(contextId))
                    && this.enabledActionMask != enabledActionMask)
                cancelLobbyScenarioTouch();
            if (ThorMainMenuState.choiceCount(contextId) > 0
                    && this.enabledActionMask != enabledActionMask)
                cancelMainMenuTouch();
            if ((ThorContextIds.CAMPAIGN_BROWSER.equals(contextId)
                    || ThorContextIds.LOBBY_LOAD_GAME_SCENARIO.equals(contextId))
                    && this.enabledActionMask != enabledActionMask)
                browserGesture.cancel();
            if (ThorWindowNavigation.isWindow(contextId) && this.enabledActionMask != enabledActionMask)
                windowNavigation.cancel();
            if (ThorContextIds.TOWN_WINDOW.equals(contextId) && this.enabledActionMask != enabledActionMask)
                townServices.cancel();
            this.revision = revision;
            this.contextId = contextId;
            heroPortraitAssetKey = publishedHeroPortraitAssetKey;
            heroMeetingMode.update(revision, ThorContextIds.HERO_MEETING.equals(contextId));
            if (!ThorContextIds.ADVENTURE_MAP.equals(contextId))
            {
                adventureTab = 0;
                heroes = ThorHeroRoster.EMPTY;
                towns = ThorTownRoster.EMPTY;
                townPage = 0;
            }
            if (!retainMeetingArmies)
            {
                heroMeetingArmies = ThorHeroMeetingArmies.EMPTY;
                selectedMeetingSlot = -1;
                pendingMeetingAction = false;
                heroMeetingSplit.cancel();
                heroMeetingRedistribution.cancel();
                cancelHeroMeetingGesture();
            }
            this.enabledActionMask = enabledActionMask;
            this.activeActionMask = activeActionMask;
            for (int index = 0; index < detailLines.length; ++index)
                detailLines[index] = publishedDetails != null && index < publishedDetails.length
                        ? ThorContextDetails.orEmpty(publishedDetails[index]) : "";
            if (ThorContextIds.MAIN_MENU.equals(contextId))
            {
                title = getContext().getString(R.string.thor_context_main_menu);
                status = getContext().getString(R.string.thor_context_main_menu_status);
            }
            else if (ThorContextIds.MAIN_MENU_NEW_GAME.equals(contextId))
            {
                title = getContext().getString(R.string.thor_context_new_game);
                status = getContext().getString(R.string.thor_context_new_game_status);
            }
            else if (ThorContextIds.MAIN_MENU_LOAD_GAME.equals(contextId))
            {
                title = getContext().getString(R.string.thor_context_load_game);
                status = getContext().getString(R.string.thor_context_load_game_status);
            }
            else if (ThorContextIds.MAIN_MENU_CAMPAIGN.equals(contextId))
            {
                title = getContext().getString(R.string.thor_context_campaign);
                status = getContext().getString(R.string.thor_context_campaign_status);
            }
            else if (ThorContextIds.MAIN_MENU_CREDITS.equals(contextId))
            {
                title = getContext().getString(R.string.thor_context_credits);
                status = getContext().getString(R.string.thor_context_credits_status);
            }
            else if (ThorContextIds.LOBBY_NEW_GAME.equals(contextId))
            {
                title = getContext().getString(R.string.thor_context_new_game);
                status = getContext().getString(R.string.thor_context_lobby_new_game_status);
            }
            else if (ThorContextIds.LOBBY_NEW_GAME_SCENARIO.equals(contextId))
            {
                title = ThorLobbyScenarioState.scenarioName(publishedTitle,
                        getContext().getString(R.string.thor_lobby_no_scenario));
                status = getContext().getString(R.string.thor_context_lobby_new_game_scenario_status);
            }
            else if (ThorContextIds.LOBBY_NEW_GAME_OPTIONS.equals(contextId))
            {
                title = getContext().getString(R.string.thor_context_new_game);
                status = getContext().getString(R.string.thor_context_lobby_new_game_options_status);
            }
            else if (ThorContextIds.LOBBY_NEW_GAME_RANDOM_MAP.equals(contextId))
            {
                title = getContext().getString(R.string.thor_context_random_map);
                status = getContext().getString(R.string.thor_context_lobby_random_map_status);
            }
            else if (ThorContextIds.LOBBY_NEW_GAME_TURN_OPTIONS.equals(contextId))
            {
                title = getContext().getString(R.string.thor_context_turn_options);
                status = getContext().getString(R.string.thor_context_lobby_new_game_turn_options_status);
            }
            else if (ThorContextIds.LOBBY_NEW_GAME_EXTRA_OPTIONS.equals(contextId))
            {
                title = getContext().getString(R.string.thor_context_extra_options);
                status = getContext().getString(R.string.thor_context_lobby_new_game_extra_options_status);
            }
            else if (ThorContextIds.LOBBY_NEW_GAME_BATTLE_MODE.equals(contextId))
            {
                title = getContext().getString(R.string.thor_context_battle_mode);
                status = getContext().getString(R.string.thor_context_lobby_battle_mode_status);
            }
            else if (ThorContextIds.LOBBY_LOAD_GAME.equals(contextId))
            {
                title = getContext().getString(R.string.thor_context_load_game);
                status = getContext().getString(R.string.thor_context_lobby_load_game_status);
            }
            else if (ThorContextIds.LOBBY_LOAD_GAME_SCENARIO.equals(contextId))
            {
                title = ThorLobbyScenarioState.scenarioName(publishedTitle,
                        getContext().getString(R.string.thor_lobby_no_save));
                status = getContext().getString(R.string.thor_context_lobby_load_game_scenario_status);
            }
            else if (ThorContextIds.LOBBY_LOAD_GAME_OPTIONS.equals(contextId))
            {
                title = getContext().getString(R.string.thor_context_load_game);
                status = getContext().getString(R.string.thor_context_lobby_load_game_options_status);
            }
            else if (ThorContextIds.LOBBY_LOAD_GAME_TURN_OPTIONS.equals(contextId))
            {
                title = getContext().getString(R.string.thor_context_turn_options);
                status = getContext().getString(R.string.thor_context_lobby_load_game_turn_options_status);
            }
            else if (ThorContextIds.LOBBY_LOAD_GAME_EXTRA_OPTIONS.equals(contextId))
            {
                title = getContext().getString(R.string.thor_context_extra_options);
                status = getContext().getString(R.string.thor_context_lobby_load_game_extra_options_status);
            }
            else if (ThorContextIds.LOBBY_CAMPAIGN_LIST.equals(contextId))
            {
                title = getContext().getString(R.string.thor_context_campaign);
                status = getContext().getString(R.string.thor_context_lobby_campaign_list_status);
            }
            else if (ThorContextIds.CAMPAIGN_BONUS_SELECTION.equals(contextId))
            {
                title = ThorLobbyScenarioState.scenarioName(publishedTitle,
                        getContext().getString(R.string.thor_campaign_no_scenario));
                status = ThorLobbyScenarioState.scenarioName(publishedStatus,
                        getContext().getString(R.string.thor_context_campaign));
            }
            else if (ThorContextIds.CAMPAIGN_BROWSER.equals(contextId))
            {
                title = getContext().getString(R.string.thor_campaign_browser_title);
                status = getContext().getString(R.string.thor_campaign_browser_status);
            }
            else if (ThorContextIds.ADVENTURE_MAP.equals(contextId))
            {
                title = getContext().getString(R.string.thor_context_adventure_map);
                status = publishedTitle.isEmpty()
                        ? getContext().getString(R.string.thor_context_adventure_map_status)
                        : getContext().getString(R.string.thor_context_adventure_map_hero_status,
                                publishedTitle, publishedStatus);
            }
            else if (ThorContextIds.HERO_WINDOW.equals(contextId))
            {
                title = publishedTitle.isEmpty() ? getContext().getString(R.string.thor_context_hero) : publishedTitle;
                status = publishedStatus.isEmpty() ? getContext().getString(R.string.thor_context_hero_status) : publishedStatus;
            }
            else if (ThorContextIds.TOWN_WINDOW.equals(contextId))
            {
                title = publishedTitle.isEmpty() ? getContext().getString(R.string.thor_context_town) : publishedTitle;
                status = publishedStatus.isEmpty() ? getContext().getString(R.string.thor_context_town_status) : publishedStatus;
            }
            else if (ThorContextIds.HERO_MEETING.equals(contextId))
            {
                title = getContext().getString(R.string.thor_context_hero_meeting);
                status = getContext().getString(R.string.thor_context_hero_meeting_status);
            }
            else if (ThorContextIds.BATTLE.equals(contextId))
            {
                title = publishedTitle.isEmpty()
                        ? getContext().getString(R.string.thor_battle_no_active_unit) : publishedTitle;
                status = publishedStatus;
            }
            else if (ThorContextIds.BATTLE_TACTICS.equals(contextId))
            {
                title = publishedTitle.isEmpty()
                        ? getContext().getString(R.string.thor_battle_no_active_unit) : publishedTitle;
                status = publishedStatus;
            }
            else if (ThorContextIds.BATTLE_RESULT.equals(contextId))
            {
                title = getContext().getString(R.string.thor_context_battle_result);
                status = getContext().getString(R.string.thor_context_battle_result_status);
            }
            else if (ThorContextIds.KINGDOM_OVERVIEW.equals(contextId))
            {
                title = getContext().getString(R.string.thor_context_kingdom_overview);
                status = getContext().getString(R.string.thor_context_kingdom_overview_status);
            }
            else if (ThorContextIds.QUEST_LOG.equals(contextId))
            {
                title = getContext().getString(R.string.thor_context_quest_log);
                status = getContext().getString(R.string.thor_context_quest_log_status);
            }
            else if (ThorContextIds.SCENARIO_EVENT_JOURNAL.equals(contextId))
            {
                title = getContext().getString(R.string.thor_context_scenario_event_journal);
                status = getContext().getString(R.string.thor_context_scenario_event_journal_status);
            }
            else if (ThorContextIds.PUZZLE_MAP.equals(contextId))
            {
                title = getContext().getString(R.string.thor_context_puzzle_map);
                status = getContext().getString(R.string.thor_context_puzzle_map_status);
            }
            else if (ThorContextIds.SAVE_GAME.equals(contextId))
            {
                title = getContext().getString(R.string.thor_context_save_game);
                status = getContext().getString(R.string.thor_context_save_game_status);
            }
            else
            {
                title = publishedTitle.isEmpty() ? getContext().getString(R.string.thor_deck_title) : publishedTitle;
                status = publishedStatus.isEmpty() ? getContext().getString(R.string.thor_deck_status) : publishedStatus;
            }
            setContentDescription(commandDeckDescription());
            invalidate();
        }

        void updateBrowser(final ThorBrowserState publishedBrowser)
        {
            browserGesture.cancel();
            townServices.cancel();
            browser = publishedBrowser == null ? ThorBrowserState.EMPTY : publishedBrowser;
            invalidate();
        }

        void updateHeroes(final ThorHeroRoster roster)
        {
            heroes = ThorContextIds.ADVENTURE_MAP.equals(contextId) ? roster : ThorHeroRoster.EMPTY;
            setContentDescription(commandDeckDescription());
            invalidate();
        }

        void updateTowns(final ThorTownRoster roster)
        {
            towns = ThorContextIds.ADVENTURE_MAP.equals(contextId) ? roster : ThorTownRoster.EMPTY;
            syncTownPageToSelection();
            setContentDescription(commandDeckDescription());
            invalidate();
        }

        void updateHeroMeetingArmies(final ThorHeroMeetingArmies armies)
        {
            removeCallbacks(heroMeetingLongPress);
            heroMeetingGesture.cancel();
            heroMeetingSplit.cancel();
            selectedMeetingSlot = -1;
            final ThorHeroMeetingArmies updatedArmies = ThorContextIds.HERO_MEETING.equals(contextId) && armies.complete()
                    ? armies : ThorHeroMeetingArmies.EMPTY;
            if (heroMeetingRedistribution.isActive()
                    && !heroMeetingRedistribution.isForCurrentArmies(updatedArmies))
                heroMeetingRedistribution.cancel();
            heroMeetingArmies = updatedArmies;
            if (!heroMeetingArmies.complete())
            {
                heroMeetingRedistribution.cancel();
            }
            pendingMeetingAction = false;
            setContentDescription(commandDeckDescription());
            invalidate();
        }

        void updateHeroMeetingArtifacts(final ThorHeroMeetingArtifacts artifacts)
        {
            selectedArtifact = -1;
            artifactGesture.cancel();
            if (heroMeetingTouchSequence && heroMeetingMode.mode() == ThorHeroMeetingModeState.ARTIFACTS)
                heroMeetingTouchCancelled = true;
            heroMeetingArtifacts = ThorContextIds.HERO_MEETING.equals(contextId) && artifacts.complete()
                    ? artifacts : ThorHeroMeetingArtifacts.EMPTY;
            setContentDescription(commandDeckDescription());
            invalidate();
        }

        @Override
        protected void onDraw(final Canvas canvas)
        {
            super.onDraw(canvas);

            final float density = getResources().getDisplayMetrics().density;
            final float referenceScale = Math.min(getWidth() / REFERENCE_WIDTH, getHeight() / REFERENCE_HEIGHT);
            final float margin = Math.max(16f * density,
                    Math.max(Math.min(getWidth(), getHeight()) * 0.035f,
                            Math.min(getWidth(), getHeight()) * 0.045f * referenceScale));
            final float bevel = Math.max(3f * density, margin * 0.16f);
            final float contentWidth = Math.max(0f, getWidth() - margin * 2f);
            final float contentHeight = Math.max(0f, getHeight() - margin * 2f);
            if (contentWidth <= 0f || contentHeight <= 0f)
                return;

            final RectF frame = new RectF(margin, margin, getWidth() - margin, getHeight() - margin);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(STONE_DARK);
            canvas.drawRect(frame, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(bevel);
            paint.setColor(STONE_LIGHT);
            canvas.drawRect(frame, paint);
            paint.setStrokeWidth(Math.max(1f, bevel * 0.35f));
            paint.setColor(GOLD);
            canvas.drawRect(new RectF(frame.left + bevel, frame.top + bevel, frame.right - bevel, frame.bottom - bevel), paint);

            final boolean battleDashboard = ThorContextIds.BATTLE.equals(contextId)
                    || ThorContextIds.BATTLE_TACTICS.equals(contextId);
            final boolean adventure = ThorContextIds.ADVENTURE_MAP.equals(contextId);
            final boolean lobbyScenario = ThorContextIds.LOBBY_NEW_GAME_SCENARIO.equals(contextId)
                    || ThorContextIds.LOBBY_LOAD_GAME_SCENARIO.equals(contextId);
            final boolean campaignBonus = ThorContextIds.CAMPAIGN_BONUS_SELECTION.equals(contextId);
            final boolean browserContext = ThorContextIds.CAMPAIGN_BROWSER.equals(contextId)
                    || ThorContextIds.LOBBY_LOAD_GAME_SCENARIO.equals(contextId);
            final float dividerY = frame.top + contentHeight * (battleDashboard ? 0.55f
                    : adventure ? ThorAdventureLayout.DIVIDER : browserContext ? 0.20f
                    : lobbyScenario || campaignBonus ? 0.48f : 0.34f);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(PARCHMENT_DARK);
            canvas.drawRect(frame.left + bevel * 2f, frame.top + bevel * 2f, frame.right - bevel * 2f, dividerY - bevel, paint);
            paint.setColor(PARCHMENT);
            canvas.drawRect(frame.left + bevel * 2f, dividerY + bevel, frame.right - bevel * 2f, frame.bottom - bevel * 2f, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(2f, bevel * 0.55f));
            paint.setColor(GOLD);
            canvas.drawLine(frame.left + bevel * 2f, dividerY, frame.right - bevel * 2f, dividerY, paint);

            if (battleDashboard)
            {
                drawBattleDashboard(canvas, frame, dividerY, bevel, density);
            }
            else if (browserContext)
            {
                drawBrowserDashboard(canvas, frame, density);
            }
            else if (lobbyScenario)
            {
                drawLobbyScenarioDashboard(canvas, frame, dividerY, bevel, density);
            }
            else if (campaignBonus)
            {
                drawCampaignDashboard(canvas, frame, dividerY, bevel, density);
            }
            else if (ThorMainMenuState.choiceCount(contextId) > 0)
            {
                drawMainMenuDashboard(canvas, frame, dividerY, density);
            }
            else if (ThorContextIds.HERO_WINDOW.equals(contextId))
            {
                drawHeroDashboard(canvas, frame, dividerY, bevel, density);
                drawWindowNavigation(canvas, frame, bevel, density);
            }
            else if (ThorContextIds.TOWN_WINDOW.equals(contextId))
            {
                drawTownDashboard(canvas, frame, dividerY, bevel, density);
                drawTownServices(canvas, frame, density);
                drawWindowNavigation(canvas, frame, bevel, density);
            }
            else if (ThorContextIds.HERO_MEETING.equals(contextId))
            {
                drawHeroMeeting(canvas, frame, bevel, density);
            }
            else
            {
                paint.setStyle(Paint.Style.FILL);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setFakeBoldText(true);
                paint.setColor(TEXT);
                final float titleY = frame.top + contentHeight * (adventure ? ThorAdventureLayout.TITLE : 0.18f);
                boolean portraitDrawn = false;
                float portraitTextLeft = 0f;
                if (adventure && ThorVisualAssetKey.isHeroPortrait(heroPortraitAssetKey))
                {
                    final float iconSize = Math.min(52f * density, frame.height() * 0.12f);
                    final RectF icon = new RectF(frame.left + bevel * 3f, titleY - iconSize * 0.5f,
                            frame.left + bevel * 3f + iconSize, titleY + iconSize * 0.5f);
                    portraitDrawn = drawVisualAsset(canvas, heroPortraitAssetKey, icon);
                    if (portraitDrawn)
                        portraitTextLeft = icon.right + bevel;
                }
                if (adventure)
                {
                    final float titleLeft = portraitDrawn ? portraitTextLeft : frame.left + bevel * 3f;
                    final float titleRight = Math.min(frame.right - bevel * 3f,
                            hapticsToggleBounds().left - bevel);
                    drawFittedText(canvas, title, (titleLeft + titleRight) * 0.5f, titleY,
                            Math.max(0f, titleRight - titleLeft), Math.min(42f * density, contentHeight * 0.09f));
                }
                else if (portraitDrawn)
                {
                    drawFittedText(canvas, title, (portraitTextLeft + frame.right - bevel * 3f) * 0.5f,
                            titleY, frame.right - bevel * 3f - portraitTextLeft,
                            Math.min(42f * density, contentHeight * 0.09f));
                }
                else
                    drawFittedText(canvas, title, getWidth() * 0.5f, titleY, contentWidth * 0.82f,
                            Math.min(42f * density, contentHeight * 0.09f));

                paint.setFakeBoldText(false);
                paint.setColor(adventure ? TEXT : PARCHMENT_DARK);
                drawFittedText(canvas, status, getWidth() * 0.5f,
                        adventure ? frame.top + contentHeight * ThorAdventureLayout.STATUS
                                : dividerY + contentHeight * 0.075f,
                        contentWidth * 0.78f, Math.min(30f * density, contentHeight * 0.055f));
            }

            if (ThorContextIds.ADVENTURE_MAP.equals(contextId))
            {
                drawAdventureTabs(canvas, frame, dividerY, bevel, density);
                if (adventureTab == 1)
                    drawHeroes(canvas, frame, dividerY, bevel, density);
                else if (adventureTab == 2)
                    drawTowns(canvas, frame, dividerY, bevel, density);
                else
                    drawAdventureActions(canvas, frame, dividerY, bevel, density);
            }
            else if (ThorContextIds.BATTLE.equals(contextId) || ThorContextIds.BATTLE_TACTICS.equals(contextId))
                drawBattleActions(canvas, frame, dividerY, bevel, density);
            drawHapticsToggle(canvas, density);
        }

        @Override
        public boolean onTouchEvent(final android.view.MotionEvent event)
        {
            final int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN
                    && hapticsToggleGesture.begin(hapticsToggleBounds().contains(event.getX(), event.getY())))
                return true;
            if (hapticsToggleGesture.isActive())
            {
                if (action == MotionEvent.ACTION_UP)
                {
                    final boolean toggle = hapticsToggleGesture.finish(
                            hapticsToggleBounds().contains(event.getX(), event.getY()));
                    if (toggle)
                    {
                        hapticsEnabled = !hapticsEnabled;
                        hapticsChangeListener.onHapticsChanged(hapticsEnabled);
                        invalidate();
                        performClick();
                    }
                }
                else if (action == MotionEvent.ACTION_CANCEL)
                    hapticsToggleGesture.cancel();
                else if (action == MotionEvent.ACTION_POINTER_DOWN)
                    hapticsToggleGesture.pointerAdded();
                return true;
            }
            if (ThorContextIds.CAMPAIGN_BROWSER.equals(contextId)
                    || ThorContextIds.LOBBY_LOAD_GAME_SCENARIO.equals(contextId))
                return handleBrowserTouch(event);
            if (ThorContextIds.TOWN_WINDOW.equals(contextId) && (townServices.isActive()
                    || townServiceAt(event.getX(), event.getY()) != ThorTownServices.NONE))
                return handleTownServiceTouch(event);
            if (ThorWindowNavigation.isWindow(contextId))
                return handleWindowNavigationTouch(event);
            if (ThorContextIds.LOBBY_NEW_GAME_SCENARIO.equals(contextId)
                    || ThorContextIds.CAMPAIGN_BONUS_SELECTION.equals(contextId))
                return handleLobbyScenarioTouch(event);
            if (ThorMainMenuState.choiceCount(contextId) > 0)
                return handleMainMenuTouch(event);
            if (heroMeetingTouchSequence && action == MotionEvent.ACTION_DOWN)
            {
                cancelHeroMeetingGesture();
                heroMeetingTouchSequence = false;
                heroMeetingTouchCancelled = false;
            }
            if (heroMeetingTouchSequence)
            {
                if (!ThorContextIds.HERO_MEETING.equals(contextId) || revision != heroMeetingTouchRevision)
                    cancelHeroMeetingGesture();
                if (heroMeetingMode.mode() == ThorHeroMeetingModeState.ARTIFACTS
                        && (event.getPointerCount() != 1 || event.getPointerId(0) != heroMeetingPointerId))
                    cancelHeroMeetingGesture();
                if (action == MotionEvent.ACTION_MOVE)
                {
                    if (!heroMeetingTouchCancelled)
                    {
                        if (heroMeetingMode.mode() == ThorHeroMeetingModeState.ARTIFACTS)
                        {
                            if (!artifactGesture.isArmed())
                            {
                                final float dx = event.getX() - artifactTouchDownX;
                                final float dy = event.getY() - artifactTouchDownY;
                                artifactTouchMovedWithoutSource |= dx * dx + dy * dy > touchSlop * touchSlop;
                            }
                            updateArtifactGesture(event.getX(), event.getY());
                        }
                        else
                            updateHeroMeetingGesture(event.getX(), event.getY());
                    }
                    return true;
                }
                if (action == MotionEvent.ACTION_UP)
                {
                    if (!heroMeetingTouchCancelled)
                    {
                        if (heroMeetingMode.mode() == ThorHeroMeetingModeState.ARTIFACTS)
                            finishArtifactGesture(event.getX(), event.getY());
                        else
                            finishHeroMeetingGesture(event.getX(), event.getY());
                    }
                    else
                        clearHeroMeetingSelection();
                    heroMeetingTouchSequence = false;
                    heroMeetingTouchCancelled = false;
                    heroMeetingPointerId = -1;
                    artifactTouchMovedWithoutSource = false;
                    return true;
                }
                if (action == MotionEvent.ACTION_CANCEL)
                {
                    cancelHeroMeetingGesture();
                    heroMeetingTouchSequence = false;
                    heroMeetingTouchCancelled = false;
                    heroMeetingPointerId = -1;
                    artifactTouchMovedWithoutSource = false;
                    return true;
                }
                if (action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_POINTER_UP)
                {
                    cancelHeroMeetingGesture();
                    return true;
                }
                return true;
            }

            if (!ThorContextIds.ADVENTURE_MAP.equals(contextId)
                    && !ThorContextIds.BATTLE.equals(contextId)
                    && !ThorContextIds.BATTLE_TACTICS.equals(contextId)
                    && !ThorContextIds.HERO_MEETING.equals(contextId))
                return false;

            if (ThorContextIds.HERO_MEETING.equals(contextId))
            {
                if (action == MotionEvent.ACTION_DOWN)
                {
                    heroMeetingTouchSequence = true;
                    heroMeetingTouchCancelled = false;
                    heroMeetingTouchRevision = revision;
                    heroMeetingPointerId = event.getPointerId(0);
                    if (heroMeetingMode.mode() == ThorHeroMeetingModeState.ARTIFACTS)
                    {
                        artifactTouchDownX = event.getX();
                        artifactTouchDownY = event.getY();
                        artifactTouchMovedWithoutSource = false;
                        beginArtifactGesture(event.getX(), event.getY());
                    }
                    else
                        beginHeroMeetingGesture(event.getX(), event.getY());
                }
                return true;
            }

            if (event.getAction() == MotionEvent.ACTION_UP)
            {
                if (ThorContextIds.ADVENTURE_MAP.equals(contextId))
                {
                    final int tab = tabAt(event.getX(), event.getY());
                    if (tab >= 0)
                    {
                        adventureTab = tab;
                        setContentDescription(commandDeckDescription());
                        invalidate();
                        performClick();
                        return true;
                    }
                    if (adventureTab == 1)
                    {
                        final int row = heroAt(event.getX(), event.getY());
                        if (row >= 0 && isActionEnabled(ThorActionIds.SELECT_HERO))
                        {
                            performClick();
                            NativeMethods.submitThorAction(revision, ThorActionIds.SELECT_HERO, heroes.ids[row]);
                        }
                        return true;
                    }
                    if (adventureTab == 2)
                    {
                        if (townPageCount() > 1 && townPreviousBounds().contains(event.getX(), event.getY()))
                        {
                            townPage = Math.max(0, townPage - 1);
                            invalidate();
                            performClick();
                            return true;
                        }
                        if (townPageCount() > 1 && townNextBounds().contains(event.getX(), event.getY()))
                        {
                            townPage = Math.min(townPageCount() - 1, townPage + 1);
                            invalidate();
                            performClick();
                            return true;
                        }
                        final int row = townAt(event.getX(), event.getY());
                        if (row >= 0 && isActionEnabled(ThorActionIds.SELECT_TOWN))
                        {
                            performClick();
                            NativeMethods.submitThorAction(revision, ThorActionIds.SELECT_TOWN, towns.ids[row]);
                        }
                        return true;
                    }
                }
                final int actionId = actionAt(event.getX(), event.getY());
                if (actionId != ThorActionIds.NONE && isActionEnabled(actionId))
                {
                    performClick();
                    NativeMethods.submitThorAction(revision, actionId, ThorActionIds.NO_TARGET);
                }
            }
            return true;
        }

        @Override
        public boolean performClick()
        {
            super.performClick();
            return true;
        }

        void setHapticsEnabled(final boolean enabled, final boolean preview)
        {
            hapticsEnabled = enabled;
            invalidate();
            if (preview)
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        }

        void performAcceptedHaptic()
        {
            if (hapticsEnabled)
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        }

        private RectF hapticsToggleBounds()
        {
            final RectF frame = adventureFrame();
            final float density = getResources().getDisplayMetrics().density;
            final float bevel = adventureBevel();
            final float width = Math.min(138f * density, frame.width() * 0.36f);
            final float height = 36f * density;
            return new RectF(frame.right - bevel * 3f - width, frame.top + bevel * 2f,
                    frame.right - bevel * 3f, frame.top + bevel * 2f + height);
        }

        private void drawHapticsToggle(final Canvas canvas, final float density)
        {
            final RectF button = hapticsToggleBounds();
            final float bevel = adventureBevel();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(hapticsEnabled ? STONE_DARK : PARCHMENT_DARK);
            canvas.drawRoundRect(button, bevel, bevel, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1f, bevel * 0.35f));
            paint.setColor(GOLD);
            canvas.drawRoundRect(button, bevel, bevel, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setFakeBoldText(true);
            paint.setColor(TEXT);
            paint.setTextAlign(Paint.Align.CENTER);
            drawFittedText(canvas, getContext().getString(hapticsEnabled
                    ? R.string.thor_haptics_on : R.string.thor_haptics_off), button.centerX(), button.centerY(),
                    button.width() * 0.9f, Math.min(16f * density, button.height() * 0.48f));
            paint.setFakeBoldText(false);
        }

        private void drawHeroMeeting(final Canvas canvas, final RectF frame, final float bevel, final float density)
        {
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(true);
            paint.setColor(TEXT);
            drawFittedText(canvas, title, frame.centerX(), frame.top + frame.height() * 0.08f, frame.width() * 0.8f,
                    Math.min(38f * density, frame.height() * 0.05f));
            paint.setFakeBoldText(false);
            paint.setColor(PARCHMENT_DARK);
            drawFittedText(canvas, status, frame.centerX(), frame.top + frame.height() * 0.13f, frame.width() * 0.78f,
                    Math.min(24f * density, frame.height() * 0.035f));
            drawHeroMeetingModes(canvas, frame, bevel, density);
            if (heroMeetingMode.mode() == ThorHeroMeetingModeState.ARTIFACTS)
            {
                drawHeroMeetingArtifacts(canvas, frame, bevel, density);
                return;
            }
            if (!heroMeetingArmies.complete())
                return;

            final String[] names = heroMeetingArmies.heroNames;
            for (int side = 0; side < 2; ++side)
            {
                final RectF heading = heroMeetingHeadingBounds(side, frame, bevel);
                drawHeroMeetingHeading(canvas, heading, names[side],
                        heroMeetingArmies.heroPortraitAssetKeys[side], bevel, density);
                for (int slot = 0; slot < 7; ++slot)
                {
                    final int index = side * 7 + slot;
                    final RectF row = heroMeetingSlotBounds(index, frame, bevel);
                    final boolean occupied = (heroMeetingArmies.flags[index] & 1) != 0;
                    final boolean redistributionActive = heroMeetingRedistribution.isActive();
                    final boolean compatibleDestination = redistributionActive
                            && heroMeetingRedistribution.isCompatibleDestination(index, heroMeetingArmies);
                    final boolean selected = selectedMeetingSlot == index
                            || (redistributionActive && heroMeetingRedistribution.selectedDestination() == index);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(selected ? PARCHMENT_DARK : STONE_DARK);
                    canvas.drawRoundRect(row, bevel, bevel, paint);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(selected ? bevel * 0.8f : bevel * 0.3f);
                    paint.setColor(selected ? GOLD : (compatibleDestination ? Color.rgb(104, 174, 111) : STONE_LIGHT));
                    canvas.drawRoundRect(row, bevel, bevel, paint);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(selected ? TEXT : (occupied ? TEXT : PARCHMENT_DARK));
                    final int planned = redistributionActive ? heroMeetingRedistribution.allocationFor(index) : 0;
                    final String value;
                    if (occupied && planned > 0)
                        value = getContext().getString(R.string.thor_redistribution_planned,
                                heroMeetingArmies.creatureNames[index], heroMeetingArmies.counts[index], planned);
                    else if (occupied)
                        value = getContext().getString(R.string.thor_army_stack_value,
                                heroMeetingArmies.creatureNames[index], heroMeetingArmies.counts[index]);
                    else
                        value = planned > 0
                                ? getContext().getString(R.string.thor_redistribution_empty_planned, planned)
                                : getContext().getString(R.string.thor_army_empty);
                    if (occupied)
                    {
                        final float iconSize = Math.min(row.height() * 0.78f, row.width() * 0.1f);
                        final float left = row.left + Math.max(bevel * 0.55f, 5f);
                        final RectF iconBounds = new RectF(left, row.centerY() - iconSize * 0.5f,
                                left + iconSize, row.centerY() + iconSize * 0.5f);
                        drawVisualAsset(canvas, heroMeetingArmies.visualAssetKeys[index], iconBounds);
                        final float textLeft = iconBounds.right + Math.max(bevel * 0.7f, 7f);
                        drawEllipsizedText(canvas, value, (textLeft + row.right) * 0.5f, row.centerY(),
                                (row.right - textLeft) * 0.94f, Math.min(22f * density, row.height() * 0.55f));
                    }
                    else
                        drawEllipsizedText(canvas, value, row.centerX(), row.centerY(), row.width() * 0.88f,
                                Math.min(22f * density, row.height() * 0.55f));
                }
            }
            if (heroMeetingSplit.stage() != ThorHeroMeetingSplitState.Stage.NONE)
            {
                paint.setColor(PARCHMENT_DARK);
                final int instruction = heroMeetingSplit.stage() == ThorHeroMeetingSplitState.Stage.DESTINATION
                        ? R.string.thor_split_select_destination : R.string.thor_split_amount;
                drawFittedText(canvas, getContext().getString(instruction), frame.centerX(),
                        frame.top + frame.height() * 0.775f, frame.width() * 0.8f,
                        Math.min(22f * density, frame.height() * 0.03f));
            }
            if (heroMeetingRedistribution.isActive())
            {
                drawHeroMeetingRedistributionEditor(canvas, frame, bevel, density);
                return;
            }
            if (heroMeetingSplit.stage() == ThorHeroMeetingSplitState.Stage.AMOUNT)
            {
                drawHeroMeetingSplitEditor(canvas, frame, bevel, density);
                return;
            }
            if (heroMeetingSplit.stage() == ThorHeroMeetingSplitState.Stage.DESTINATION)
            {
                drawHeroMeetingSplitDestinationActions(canvas, frame, bevel, density);
                return;
            }
            final int[] actions = {ThorActionIds.HERO_MEETING_ARMY_LEFT_TO_RIGHT,
                    ThorActionIds.HERO_MEETING_SWAP_ARMIES, ThorActionIds.HERO_MEETING_ARMY_RIGHT_TO_LEFT};
            final int[] labels = {R.string.thor_army_move_right, R.string.thor_army_swap, R.string.thor_army_move_left};
            for (int index = 0; index < actions.length; ++index)
            {
                final RectF button = heroMeetingActionBounds(index, frame, bevel);
                final boolean enabled = isActionEnabled(actions[index]);
                paint.setColor(enabled ? STONE_DARK : Color.rgb(76, 74, 67));
                canvas.drawRoundRect(button, bevel, bevel, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(Math.max(2f, bevel * 0.35f));
                paint.setColor(enabled ? GOLD : PARCHMENT_DARK);
                canvas.drawRoundRect(button, bevel, bevel, paint);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(enabled ? TEXT : PARCHMENT_DARK);
                drawFittedText(canvas, getContext().getString(labels[index]), button.centerX(), button.centerY(),
                        button.width() * 0.85f, Math.min(22f * density, button.height() * 0.48f));
            }
            drawHeroMeetingDragFeedback(canvas, frame, bevel, density);
        }

        private void drawHeroMeetingModes(final Canvas canvas, final RectF frame, final float bevel, final float density)
        {
            final int[] labels = {R.string.thor_meeting_army, R.string.thor_meeting_artifacts};
            for (int index = 0; index < 2; ++index)
            {
                final RectF tab = heroMeetingModeBounds(index, frame, bevel);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(heroMeetingMode.mode() == index ? STONE_DARK : STONE_LIGHT);
                canvas.drawRoundRect(tab, bevel, bevel, paint);
                paint.setColor(TEXT);
                drawFittedText(canvas, getContext().getString(labels[index]), tab.centerX(), tab.centerY(),
                        tab.width() * 0.85f, Math.min(20f * density, tab.height() * 0.55f));
            }
        }

        private void drawHeroMeetingArtifacts(final Canvas canvas, final RectF frame, final float bevel,
                                               final float density)
        {
            if (!heroMeetingArtifacts.complete())
                return;
            for (int side = 0; side < 2; ++side)
            {
                final RectF heading = heroMeetingHeadingBounds(side, frame, bevel);
                final long portraitKey = heroMeetingArmies.complete()
                        ? heroMeetingArmies.heroPortraitAssetKeys[side] : 0L;
                drawHeroMeetingHeading(canvas, heading, heroMeetingArtifacts.heroNames[side], portraitKey, bevel, density);
                for (int row = 0; row < 6; ++row)
                {
                    final int local = artifactPage * 6 + row;
                    if (local >= ThorHeroMeetingArtifacts.PER_HERO)
                        continue;
                    final int index = side * ThorHeroMeetingArtifacts.PER_HERO + local;
                    final RectF bounds = heroMeetingArtifactBounds(side, row, frame, bevel);
                    final boolean occupied = (heroMeetingArtifacts.flags[index] & 1) != 0;
                    final boolean locked = (heroMeetingArtifacts.flags[index] & 2) != 0;
                    final boolean backpack = (heroMeetingArtifacts.flags[index] & 4) != 0;
                    paint.setColor(locked ? Color.rgb(76, 74, 67) : STONE_DARK);
                    canvas.drawRoundRect(bounds, bevel, bevel, paint);
                    if (selectedArtifact == index || artifactGesture.isDragging()
                            && artifactGesture.sourceKey() == index)
                    {
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(Math.max(2f, bevel * 0.4f));
                        paint.setColor(GOLD);
                        canvas.drawRoundRect(bounds, bevel, bevel, paint);
                        paint.setStyle(Paint.Style.FILL);
                    }
                    paint.setColor(occupied ? TEXT : PARCHMENT_DARK);
                    final String prefix = backpack ? getContext().getString(R.string.thor_artifact_backpack)
                            : getContext().getString(R.string.thor_artifact_equipped);
                    final String value = prefix + " " + (occupied ? heroMeetingArtifacts.names[index]
                            : getContext().getString(R.string.thor_army_empty))
                            + (locked ? " " + getContext().getString(R.string.thor_artifact_locked) : "");
                    if (occupied)
                    {
                        final float iconSize = Math.min(bounds.height() * 0.72f, bounds.width() * 0.08f);
                        final float left = bounds.left + Math.max(bevel * 0.55f, 5f);
                        final RectF iconBounds = new RectF(left, bounds.centerY() - iconSize * 0.5f,
                                left + iconSize, bounds.centerY() + iconSize * 0.5f);
                        drawVisualAsset(canvas, heroMeetingArtifacts.visualAssetKeys[index], iconBounds);
                        final float textLeft = iconBounds.right + Math.max(bevel * 0.7f, 7f);
                        drawEllipsizedText(canvas, value, (textLeft + bounds.right) * 0.5f, bounds.centerY(),
                                (bounds.right - textLeft) * 0.95f, Math.min(20f * density, bounds.height() * 0.5f));
                    }
                    else
                        drawEllipsizedText(canvas, value, bounds.centerX(), bounds.centerY(), bounds.width() * 0.9f,
                                Math.min(20f * density, bounds.height() * 0.5f));
                }
            }
            final String page = (artifactPage + 1) + " / 4";
            for (int index = 0; index < 2; ++index)
            {
                final RectF button = heroMeetingArtifactPageBounds(index, frame, bevel);
                paint.setColor(STONE_DARK);
                canvas.drawRoundRect(button, bevel, bevel, paint);
                paint.setColor(TEXT);
                drawFittedText(canvas, getContext().getString(index == 0 ? R.string.thor_previous : R.string.thor_next),
                        button.centerX(), button.centerY(), button.width() * 0.85f,
                        Math.min(20f * density, button.height() * 0.48f));
            }
            paint.setColor(TEXT);
            drawFittedText(canvas, page, frame.centerX(), frame.top + frame.height() * 0.815f,
                    frame.width() * 0.3f, 20f * density);
            if (selectedArtifact >= 0)
            {
                paint.setColor(GOLD);
                final String selected = getContext().getString(R.string.thor_artifact_selected,
                        heroMeetingArtifacts.names[selectedArtifact]);
                drawEllipsizedText(canvas, selected, frame.centerX(), frame.top + frame.height() * 0.85f,
                        frame.width() * 0.3f, 17f * density);
            }
            final int[] labels = {R.string.thor_artifact_move_right, R.string.thor_artifact_swap,
                    R.string.thor_artifact_move_left};
            for (int index = 0; index < labels.length; ++index)
            {
                final RectF button = heroMeetingArtifactActionBounds(index, frame, bevel);
                final boolean enabled = isActionEnabled(ThorActionIds.artifactBulkActionForButton(index))
                        && !pendingMeetingAction;
                paint.setColor(enabled ? STONE_DARK : Color.rgb(76, 74, 67));
                canvas.drawRoundRect(button, bevel, bevel, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(Math.max(2f, bevel * 0.35f));
                paint.setColor(enabled ? GOLD : PARCHMENT_DARK);
                canvas.drawRoundRect(button, bevel, bevel, paint);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(enabled ? TEXT : PARCHMENT_DARK);
                drawFittedText(canvas, getContext().getString(labels[index]), button.centerX(), button.centerY(),
                        button.width() * 0.86f, Math.min(20f * density, button.height() * 0.45f));
            }
            drawArtifactDragFeedback(canvas, frame, bevel, density);
        }

        private RectF heroMeetingArtifactPageBounds(final int index, final RectF frame, final float bevel)
        {
            final float width = frame.width() * 0.32f;
            final float left = index == 0 ? frame.left + bevel * 3f : frame.right - bevel * 3f - width;
            return new RectF(left, frame.top + frame.height() * 0.785f, left + width,
                    frame.top + frame.height() * 0.88f);
        }

        private RectF heroMeetingArtifactActionBounds(final int index, final RectF frame, final float bevel)
        {
            final float gap = Math.max(bevel, 8f);
            final float width = (frame.width() - bevel * 6f - gap * 2f) / 3f;
            final float left = frame.left + bevel * 3f + index * (width + gap);
            return new RectF(left, frame.top + frame.height() * 0.89f, left + width,
                    frame.top + frame.height() * 0.99f);
        }

        private void drawArtifactDragFeedback(final Canvas canvas, final RectF frame,
                                              final float bevel, final float density)
        {
            if (!artifactGesture.isDragging())
                return;
            final int source = artifactGesture.sourceKey();
            final int destination = artifactAt(artifactGesture.pointerX(), artifactGesture.pointerY(), frame, bevel);
            if (ThorHeroMeetingArtifactPair.encode(source, destination) != ThorHeroMeetingArtifactPair.INVALID
                    && (heroMeetingArtifacts.flags[destination] & 2) == 0)
            {
                final RectF target = heroMeetingArtifactBounds(destination / ThorHeroMeetingArtifacts.PER_HERO,
                        destination % ThorHeroMeetingArtifacts.PER_HERO - artifactPage * 6, frame, bevel);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(Math.max(3f * density, bevel * 0.8f));
                paint.setColor(GOLD);
                canvas.drawRoundRect(target, bevel, bevel, paint);
                paint.setStyle(Paint.Style.FILL);
            }
            final String label = heroMeetingArtifacts.names[source];
            paint.setColor(PARCHMENT_DARK);
            paint.setTextSize(22f * density);
            final float width = Math.min(frame.width() * 0.58f, paint.measureText(label) + bevel * 4f);
            final float height = Math.max(38f * density, bevel * 3f);
            final float centerX = Math.max(frame.left + width * 0.5f,
                    Math.min(frame.right - width * 0.5f, artifactGesture.pointerX()));
            final float centerY = Math.max(frame.top + height * 0.5f,
                    Math.min(frame.bottom - height * 0.5f, artifactGesture.pointerY() - height));
            final RectF cursor = new RectF(centerX - width * 0.5f, centerY - height * 0.5f,
                    centerX + width * 0.5f, centerY + height * 0.5f);
            canvas.drawRoundRect(cursor, bevel, bevel, paint);
            paint.setColor(TEXT);
            drawEllipsizedText(canvas, label, centerX, centerY, width * 0.86f,
                    Math.min(22f * density, height * 0.56f));
        }

        private RectF heroMeetingModeBounds(final int mode, final RectF frame, final float bevel)
        {
            final float width = frame.width() * 0.18f;
            final float left = frame.centerX() + (mode == 0 ? -width - bevel : bevel);
            return new RectF(left, frame.top + frame.height() * 0.115f, left + width,
                    frame.top + frame.height() * 0.155f);
        }

        private RectF heroMeetingArtifactBounds(final int side, final int row, final RectF frame, final float bevel)
        {
            final RectF heading = heroMeetingHeadingBounds(side, frame, bevel);
            final float gap = Math.max(bevel * 0.7f, 6f);
            final float bottom = frame.top + frame.height() * 0.78f;
            final float height = (bottom - heading.bottom - gap * 5f) / 6f;
            final float top = heading.bottom + gap + row * (height + gap);
            return new RectF(heading.left, top, heading.right, top + height);
        }

        private RectF heroMeetingHeadingBounds(final int side, final RectF frame, final float bevel)
        {
            final float gap = Math.max(bevel, 8f);
            final float width = (frame.width() - bevel * 6f - gap) * 0.5f;
            final float left = frame.left + bevel * 3f + side * (width + gap);
            final float top = frame.top + frame.height() * 0.16f;
            return new RectF(left, top, left + width, top + frame.height() * 0.05f);
        }

        private void drawHeroMeetingHeading(final Canvas canvas, final RectF heading, final String heroName,
                                           final long portraitKey, final float bevel, final float density)
        {
            float textLeft = heading.left;
            if (ThorVisualAssetKey.isHeroPortrait(portraitKey))
            {
                final float iconSize = Math.min(heading.height() * 0.9f, heading.width() * 0.14f);
                final RectF icon = new RectF(heading.left, heading.centerY() - iconSize * 0.5f,
                        heading.left + iconSize, heading.centerY() + iconSize * 0.5f);
                if (drawVisualAsset(canvas, portraitKey, icon))
                    textLeft = icon.right + bevel * 0.5f;
            }
            paint.setColor(PARCHMENT_DARK);
            paint.setFakeBoldText(true);
            drawEllipsizedText(canvas, heroName, (textLeft + heading.right) * 0.5f, heading.centerY(),
                    (heading.right - textLeft) * 0.94f, Math.min(26f * density, heading.height() * 0.6f));
            paint.setFakeBoldText(false);
        }

        private RectF heroMeetingSlotBounds(final int index, final RectF frame, final float bevel)
        {
            final int side = index / 7;
            final int row = index % 7;
            final RectF heading = heroMeetingHeadingBounds(side, frame, bevel);
            final float gap = Math.max(bevel * 0.7f, 6f);
            final float bottom = frame.top + frame.height() * 0.75f;
            final float height = (bottom - heading.bottom - gap * 6f) / 7f;
            final float top = heading.bottom + gap + row * (height + gap);
            return new RectF(heading.left, top, heading.right, top + height);
        }

        private RectF heroMeetingActionBounds(final int index, final RectF frame, final float bevel)
        {
            final float gap = Math.max(bevel, 8f);
            final float width = (frame.width() - bevel * 6f - gap * 2f) / 3f;
            final float left = frame.left + bevel * 3f + index * (width + gap);
            final float top = frame.top + frame.height() * 0.80f;
            return new RectF(left, top, left + width, frame.bottom - bevel * 3f);
        }

        private RectF heroMeetingSplitButtonBounds(final int index, final RectF frame, final float bevel)
        {
            final float gap = Math.max(bevel * 0.5f, 5f);
            final float width = (frame.width() - bevel * 6f - gap * 5f) / 6f;
            final float left = frame.left + bevel * 3f + index * (width + gap);
            final float top = frame.top + frame.height() * 0.82f;
            return new RectF(left, top, left + width, frame.bottom - bevel * 3f);
        }

        private RectF heroMeetingPromptButtonBounds(final int index, final RectF frame, final float bevel)
        {
            final float gap = Math.max(bevel, 8f);
            final float width = (frame.width() - bevel * 6f - gap) * 0.5f;
            final float left = frame.left + bevel * 3f + index * (width + gap);
            final float top = frame.top + frame.height() * 0.83f;
            return new RectF(left, top, left + width, frame.bottom - bevel * 3f);
        }

        private void drawHeroMeetingSplitDestinationActions(final Canvas canvas, final RectF frame,
                                                             final float bevel, final float density)
        {
            final int[] labels = {R.string.thor_redistribution_enter, R.string.thor_split_cancel};
            for (int index = 0; index < labels.length; ++index)
            {
                final RectF button = heroMeetingPromptButtonBounds(index, frame, bevel);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(STONE_DARK);
                canvas.drawRoundRect(button, bevel, bevel, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(Math.max(2f, bevel * 0.35f));
                paint.setColor(index == 0 && !isActionEnabled(ThorActionIds.HERO_MEETING_REDISTRIBUTE_STACK)
                        ? PARCHMENT_DARK : GOLD);
                canvas.drawRoundRect(button, bevel, bevel, paint);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(TEXT);
                drawFittedText(canvas, getContext().getString(labels[index]), button.centerX(), button.centerY(),
                        button.width() * 0.86f, Math.min(24f * density, button.height() * 0.45f));
            }
        }

        private void drawHeroMeetingRedistributionEditor(final Canvas canvas, final RectF frame,
                                                          final float bevel, final float density)
        {
            final int source = heroMeetingRedistribution.sourceKey();
            final String sourceName = heroMeetingArmies.creatureNames[source];
            final String instruction = getContext().getString(R.string.thor_redistribution_instructions,
                    sourceName, heroMeetingRedistribution.sourceCount());
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(PARCHMENT_DARK);
            drawFittedText(canvas, instruction, frame.centerX(), frame.top + frame.height() * 0.777f,
                    frame.width() * 0.92f, Math.min(20f * density, frame.height() * 0.027f));
            final String totals = getContext().getString(R.string.thor_redistribution_totals,
                    heroMeetingRedistribution.totalAllocated(), heroMeetingRedistribution.maximumAllocated(),
                    heroMeetingRedistribution.remaining());
            drawFittedText(canvas, totals, frame.centerX(), frame.top + frame.height() * 0.808f,
                    frame.width() * 0.88f, Math.min(19f * density, frame.height() * 0.025f));

            final int[] labels = {R.string.thor_split_minus_ten, R.string.thor_split_minus_one,
                    R.string.thor_split_plus_one, R.string.thor_split_plus_ten,
                    R.string.thor_split_cancel, R.string.thor_redistribution_confirm};
            final boolean canAdjust = heroMeetingRedistribution.selectedDestination() >= 0;
            final boolean canConfirm = heroMeetingRedistribution.canConfirm(revision, heroMeetingArmies)
                    && isActionEnabled(ThorActionIds.HERO_MEETING_REDISTRIBUTE_STACK);
            for (int index = 0; index < labels.length; ++index)
            {
                final RectF button = heroMeetingSplitButtonBounds(index, frame, bevel);
                final boolean enabled = index == 4 || (index == 5 ? canConfirm : canAdjust);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(enabled ? STONE_DARK : Color.rgb(76, 74, 67));
                canvas.drawRoundRect(button, bevel, bevel, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(Math.max(2f, bevel * 0.35f));
                paint.setColor(enabled ? GOLD : PARCHMENT_DARK);
                canvas.drawRoundRect(button, bevel, bevel, paint);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(enabled ? TEXT : PARCHMENT_DARK);
                final String label = index == 5
                        ? getContext().getString(labels[index]) + " " + heroMeetingRedistribution.totalAllocated()
                        : getContext().getString(labels[index]);
                drawFittedText(canvas, label, button.centerX(), button.centerY(), button.width() * 0.86f,
                        Math.min(19f * density, button.height() * 0.44f));
            }
        }

        private void drawHeroMeetingSplitEditor(final Canvas canvas, final RectF frame, final float bevel,
                                                 final float density)
        {
            final int[] labels = {R.string.thor_split_minus_ten, R.string.thor_split_minus_one,
                    R.string.thor_split_plus_one, R.string.thor_split_plus_ten,
                    R.string.thor_split_cancel, R.string.thor_split_confirm};
            for (int index = 0; index < labels.length; ++index)
            {
                final RectF button = heroMeetingSplitButtonBounds(index, frame, bevel);
                paint.setColor(STONE_DARK);
                canvas.drawRoundRect(button, bevel, bevel, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(Math.max(2f, bevel * 0.35f));
                paint.setColor(GOLD);
                canvas.drawRoundRect(button, bevel, bevel, paint);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(TEXT);
                final String label = index == 5
                        ? getContext().getString(labels[index]) + " " + heroMeetingSplit.amount()
                        : getContext().getString(labels[index]);
                drawFittedText(canvas, label, button.centerX(), button.centerY(), button.width() * 0.86f,
                        Math.min(20f * density, button.height() * 0.45f));
            }
        }

        private int heroMeetingSlotAt(final float x, final float y, final RectF frame, final float bevel)
        {
            for (int index = 0; index < ThorHeroMeetingArmies.SLOT_COUNT; ++index)
                if (heroMeetingSlotBounds(index, frame, bevel).contains(x, y))
                    return index;
            return -1;
        }

        private int artifactAt(final float x, final float y, final RectF frame, final float bevel)
        {
            for (int side = 0; side < 2; ++side)
                for (int row = 0; row < 6; ++row)
                {
                    final int local = artifactPage * 6 + row;
                    if (local < ThorHeroMeetingArtifacts.PER_HERO
                            && heroMeetingArtifactBounds(side, row, frame, bevel).contains(x, y))
                        return side * ThorHeroMeetingArtifacts.PER_HERO + local;
                }
            return -1;
        }

        private void beginArtifactGesture(final float x, final float y)
        {
            if (pendingMeetingAction || !heroMeetingArtifacts.complete())
                return;
            final int source = artifactAt(x, y, adventureFrame(), adventureBevel());
            final boolean legal = source >= 0 && (heroMeetingArtifacts.flags[source] & 3) == 1
                    && isActionEnabled(ThorActionIds.HERO_MEETING_TRANSFER_ARTIFACT);
            artifactGesture.begin(revision, source, legal, x, y);
        }

        private void updateArtifactGesture(final float x, final float y)
        {
            if (artifactGesture.move(x, y, touchSlop, revision,
                    ThorContextIds.HERO_MEETING.equals(contextId)
                            && heroMeetingMode.mode() == ThorHeroMeetingModeState.ARTIFACTS))
            {
                selectedArtifact = -1;
                invalidate();
            }
        }

        private void finishArtifactGesture(final float x, final float y)
        {
            if (!artifactGesture.isArmed())
            {
                final float dx = x - artifactTouchDownX;
                final float dy = y - artifactTouchDownY;
                if (!artifactTouchMovedWithoutSource && dx * dx + dy * dy <= touchSlop * touchSlop)
                    handleHeroMeetingTap(x, y);
                return;
            }
            final int source = artifactGesture.sourceKey();
            final boolean wasDragging = artifactGesture.isDragging();
            artifactGesture.move(x, y, touchSlop, revision,
                    ThorContextIds.HERO_MEETING.equals(contextId)
                            && heroMeetingMode.mode() == ThorHeroMeetingModeState.ARTIFACTS);
            final int destination = artifactAt(x, y, adventureFrame(), adventureBevel());
            final ThorHeroMeetingArtifactGesture.Kind result = artifactGesture.finish(revision,
                    ThorContextIds.HERO_MEETING.equals(contextId)
                            && heroMeetingMode.mode() == ThorHeroMeetingModeState.ARTIFACTS, destination);
            if (result == ThorHeroMeetingArtifactGesture.Kind.TAP)
                handleHeroMeetingTap(x, y);
            else if (result == ThorHeroMeetingArtifactGesture.Kind.DROP && !pendingMeetingAction
                    && heroMeetingArtifacts.complete()
                    && (heroMeetingArtifacts.flags[source] & 3) == 1
                    && (heroMeetingArtifacts.flags[destination] & 2) == 0
                    && isActionEnabled(ThorActionIds.HERO_MEETING_TRANSFER_ARTIFACT))
            {
                final int pair = ThorHeroMeetingArtifactPair.encode(source, destination);
                if (pair != ThorHeroMeetingArtifactPair.INVALID)
                {
                    NativeMethods.submitThorAction(revision, ThorActionIds.HERO_MEETING_TRANSFER_ARTIFACT, pair);
                    pendingMeetingAction = true;
                    performClick();
                }
            }
            if (wasDragging || result != ThorHeroMeetingArtifactGesture.Kind.TAP)
            {
                setContentDescription(commandDeckDescription());
                invalidate();
            }
        }

        private void beginHeroMeetingGesture(final float x, final float y)
        {
            if (heroMeetingMode.mode() != ThorHeroMeetingModeState.ARMY || heroMeetingRedistribution.isActive())
                return;
            if (heroMeetingSplit.stage() != ThorHeroMeetingSplitState.Stage.NONE)
                return;
            if (pendingMeetingAction || !heroMeetingArmies.complete() || !heroMeetingArmies.locallyControllable)
                return;
            final int sourceKey = heroMeetingSlotAt(x, y, adventureFrame(), adventureBevel());
            final boolean movable = sourceKey >= 0 && (heroMeetingArmies.flags[sourceKey] & 1) != 0
                    && isActionEnabled(ThorActionIds.HERO_MEETING_TRANSFER_STACK);
            if (heroMeetingGesture.begin(revision, sourceKey, movable, x, y)
                    && heroMeetingArmies.counts[sourceKey] >= 2
                    && isActionEnabled(ThorActionIds.HERO_MEETING_SPLIT_STACK))
                postDelayed(heroMeetingLongPress, ViewConfiguration.getLongPressTimeout());
        }

        private void updateHeroMeetingGesture(final float x, final float y)
        {
            final boolean wasDragging = heroMeetingGesture.isDragging();
            final boolean dragging = heroMeetingGesture.move(x, y, touchSlop, revision,
                    ThorContextIds.HERO_MEETING.equals(contextId));
            if (dragging)
            {
                removeCallbacks(heroMeetingLongPress);
                selectedMeetingSlot = heroMeetingGesture.sourceKey();
            }
            else if (wasDragging)
                selectedMeetingSlot = -1;
            if (dragging || wasDragging)
                invalidate();
        }

        private void finishHeroMeetingGesture(final float x, final float y)
        {
            removeCallbacks(heroMeetingLongPress);
            if (!heroMeetingGesture.isArmed())
            {
                handleHeroMeetingTap(x, y);
                return;
            }

            heroMeetingGesture.move(x, y, touchSlop, revision,
                    ThorContextIds.HERO_MEETING.equals(contextId));
            if (!heroMeetingGesture.isArmed())
            {
                clearHeroMeetingSelection();
                return;
            }

            final int destinationKey = heroMeetingSlotAt(x, y, adventureFrame(), adventureBevel());
            final ThorHeroMeetingGesture.Result result = heroMeetingGesture.finish(revision,
                    ThorContextIds.HERO_MEETING.equals(contextId), destinationKey);
            selectedMeetingSlot = -1;
            if (result.kind == ThorHeroMeetingGesture.Kind.TAP)
            {
                submitHeroMeetingQuickTap(result.sourceKey);
                return;
            }
            if (result.kind == ThorHeroMeetingGesture.Kind.LONG_PRESS)
            {
                selectedMeetingSlot = result.sourceKey;
                setContentDescription(commandDeckDescription());
                invalidate();
                return;
            }
            if (result.kind == ThorHeroMeetingGesture.Kind.DROP && !pendingMeetingAction
                    && isActionEnabled(ThorActionIds.HERO_MEETING_TRANSFER_STACK))
            {
                final int encodedPair = ThorHeroMeetingTransferPair.encode(result.sourceKey, result.destinationKey);
                if (encodedPair != ThorHeroMeetingTransferPair.INVALID)
                {
                    NativeMethods.submitThorAction(revision, ThorActionIds.HERO_MEETING_TRANSFER_STACK, encodedPair);
                    pendingMeetingAction = true;
                    performClick();
                }
            }
            selectedMeetingSlot = -1;
            setContentDescription(commandDeckDescription());
            invalidate();
        }

        private void cancelHeroMeetingGesture()
        {
            selectedArtifact = -1;
            artifactGesture.cancel();
            removeCallbacks(heroMeetingLongPress);
            heroMeetingGesture.cancel();
            heroMeetingSplit.cancel();
            heroMeetingRedistribution.cancel();
            selectedMeetingSlot = -1;
            if (heroMeetingTouchSequence)
                heroMeetingTouchCancelled = true;
            invalidate();
        }

        private void clearHeroMeetingSelection()
        {
            artifactGesture.cancel();
            removeCallbacks(heroMeetingLongPress);
            heroMeetingGesture.cancel();
            heroMeetingSplit.cancel();
            heroMeetingRedistribution.cancel();
            selectedMeetingSlot = -1;
            invalidate();
        }

        private void drawHeroMeetingDragFeedback(final Canvas canvas, final RectF frame,
                                                  final float bevel, final float density)
        {
            if (!heroMeetingGesture.isDragging())
                return;

            final int destinationKey = heroMeetingSlotAt(heroMeetingGesture.pointerX(), heroMeetingGesture.pointerY(), frame, bevel);
            if (ThorHeroMeetingTransferPair.encode(heroMeetingGesture.sourceKey(), destinationKey)
                    != ThorHeroMeetingTransferPair.INVALID)
            {
                final RectF target = heroMeetingSlotBounds(destinationKey, frame, bevel);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(Math.max(3f * density, bevel * 0.8f));
                paint.setColor(GOLD);
                canvas.drawRoundRect(target, bevel, bevel, paint);
            }

            final int sourceKey = heroMeetingGesture.sourceKey();
            final String label = heroMeetingArmies.creatureNames[sourceKey] + " × " + heroMeetingArmies.counts[sourceKey];
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(PARCHMENT_DARK);
            paint.setTextSize(22f * density);
            final float width = Math.min(frame.width() * 0.58f, paint.measureText(label) + bevel * 4f);
            final float height = Math.max(38f * density, bevel * 3f);
            final float centerX = Math.max(frame.left + width * 0.5f,
                    Math.min(frame.right - width * 0.5f, heroMeetingGesture.pointerX()));
            final float centerY = Math.max(frame.top + height * 0.5f,
                    Math.min(frame.bottom - height * 0.5f, heroMeetingGesture.pointerY() - height));
            final RectF cursor = new RectF(centerX - width * 0.5f, centerY - height * 0.5f,
                    centerX + width * 0.5f, centerY + height * 0.5f);
            canvas.drawRoundRect(cursor, bevel, bevel, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(2f, bevel * 0.45f));
            paint.setColor(GOLD);
            canvas.drawRoundRect(cursor, bevel, bevel, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(TEXT);
            drawEllipsizedText(canvas, label, centerX, centerY, width * 0.86f, Math.min(22f * density, height * 0.56f));
        }

        @Override
        protected void onDetachedFromWindow()
        {
            removeCallbacks(heroMeetingLongPress);
            cancelLobbyScenarioTouch();
            cancelMainMenuTouch();
            browserGesture.cancel();
            heroMeetingArmies = ThorHeroMeetingArmies.EMPTY;
            heroMeetingArtifacts = ThorHeroMeetingArtifacts.EMPTY;
            heroMeetingGesture.cancel();
            artifactGesture.cancel();
            selectedArtifact = -1;
            heroMeetingSplit.cancel();
            heroMeetingRedistribution.cancel();
            selectedMeetingSlot = -1;
            heroMeetingTouchSequence = false;
            heroMeetingTouchCancelled = false;
            heroMeetingPointerId = -1;
            super.onDetachedFromWindow();
        }

        @Override
        protected void onWindowVisibilityChanged(final int visibility)
        {
            super.onWindowVisibilityChanged(visibility);
            if (visibility != View.VISIBLE)
            {
                cancelLobbyScenarioTouch();
                cancelMainMenuTouch();
                browserGesture.cancel();
                cancelHeroMeetingGesture();
            }
        }

        private void handleHeroMeetingTap(final float x, final float y)
        {
            final RectF frame = adventureFrame();
            final float bevel = adventureBevel();
            for (int mode = 0; mode < 2; ++mode)
            {
                if (heroMeetingModeBounds(mode, frame, bevel).contains(x, y))
                {
                    heroMeetingMode.select(revision, mode);
                    artifactPage = 0;
                    selectedArtifact = -1;
                    clearHeroMeetingSelection();
                    performClick();
                    setContentDescription(commandDeckDescription());
                    invalidate();
                    return;
                }
            }
            if (heroMeetingMode.mode() == ThorHeroMeetingModeState.ARTIFACTS)
            {
                if (heroMeetingArtifacts.complete() && !pendingMeetingAction)
                {
                    for (int side = 0; side < 2; ++side)
                    {
                        for (int row = 0; row < 6; ++row)
                        {
                            final int local = artifactPage * 6 + row;
                            if (local >= ThorHeroMeetingArtifacts.PER_HERO
                                    || !heroMeetingArtifactBounds(side, row, frame, bevel).contains(x, y))
                                continue;
                            final int index = side * ThorHeroMeetingArtifacts.PER_HERO + local;
                            if ((heroMeetingArtifacts.flags[index] & 2) != 0)
                                return;
                            if (selectedArtifact < 0)
                            {
                                if ((heroMeetingArtifacts.flags[index] & 1) == 0
                                        || !isActionEnabled(ThorActionIds.HERO_MEETING_TRANSFER_ARTIFACT))
                                    return;
                                selectedArtifact = index;
                            }
                            else if (selectedArtifact == index)
                                selectedArtifact = -1;
                            else
                            {
                                final int pair = ThorHeroMeetingArtifactPair.encode(selectedArtifact, index);
                                selectedArtifact = -1;
                                if (pair != ThorHeroMeetingArtifactPair.INVALID
                                        && isActionEnabled(ThorActionIds.HERO_MEETING_TRANSFER_ARTIFACT))
                                {
                                    NativeMethods.submitThorAction(revision,
                                            ThorActionIds.HERO_MEETING_TRANSFER_ARTIFACT, pair);
                                    pendingMeetingAction = true;
                                }
                            }
                            performClick();
                            invalidate();
                            return;
                        }
                    }
                }
                for (int index = 0; index < 2; ++index)
                {
                    if (!heroMeetingArtifactPageBounds(index, frame, bevel).contains(x, y))
                        continue;
                    if (index == 0)
                        artifactPage = Math.max(0, artifactPage - 1);
                    else
                        artifactPage = Math.min(3, artifactPage + 1);
                    performClick();
                    setContentDescription(commandDeckDescription());
                    invalidate();
                    return;
                }
                for (int index = 0; index < 3; ++index)
                {
                    final int action = ThorActionIds.artifactBulkActionForButton(index);
                    if (!heroMeetingArtifactActionBounds(index, frame, bevel).contains(x, y)
                            || pendingMeetingAction || !isActionEnabled(action))
                        continue;
                    NativeMethods.submitThorAction(revision, action, ThorActionIds.NO_TARGET);
                    pendingMeetingAction = true;
                    selectedArtifact = -1;
                    performClick();
                    setContentDescription(commandDeckDescription());
                    invalidate();
                    return;
                }
                return;
            }
            if (!heroMeetingArmies.complete() || pendingMeetingAction)
                return;
            if (heroMeetingRedistribution.isActive())
            {
                handleHeroMeetingRedistributionTap(x, y, frame, bevel);
                return;
            }
            if (heroMeetingSplit.stage() == ThorHeroMeetingSplitState.Stage.AMOUNT)
            {
                handleHeroMeetingSplitEditorTap(x, y, frame, bevel);
                return;
            }
            for (int index = 0; index < ThorHeroMeetingArmies.SLOT_COUNT; ++index)
            {
                if (!heroMeetingSlotBounds(index, frame, bevel).contains(x, y))
                    continue;
                if (heroMeetingSplit.stage() == ThorHeroMeetingSplitState.Stage.DESTINATION)
                {
                    if (index == heroMeetingSplit.sourceKey())
                    {
                        heroMeetingSplit.cancel();
                        selectedMeetingSlot = -1;
                        setContentDescription(commandDeckDescription());
                        invalidate();
                        return;
                    }
                    if (heroMeetingSplit.selectDestination(revision, index, heroMeetingArmies))
                        selectedMeetingSlot = heroMeetingSplit.sourceKey();
                    setContentDescription(commandDeckDescription());
                    invalidate();
                    return;
                }
                submitHeroMeetingQuickTap(index);
                return;
            }
            if (heroMeetingSplit.stage() == ThorHeroMeetingSplitState.Stage.DESTINATION)
            {
                for (int index = 0; index < 2; ++index)
                {
                    if (!heroMeetingPromptButtonBounds(index, frame, bevel).contains(x, y))
                        continue;
                    if (index == 0 && isActionEnabled(ThorActionIds.HERO_MEETING_REDISTRIBUTE_STACK))
                    {
                        final int source = heroMeetingSplit.sourceKey();
                        if (heroMeetingRedistribution.begin(revision, source, heroMeetingArmies))
                        {
                            heroMeetingSplit.cancel();
                            selectedMeetingSlot = source;
                        }
                    }
                    else if (index == 1)
                    {
                        heroMeetingSplit.cancel();
                        selectedMeetingSlot = -1;
                    }
                    performClick();
                    setContentDescription(commandDeckDescription());
                    invalidate();
                    return;
                }
                return;
            }
            final int[] actions = {ThorActionIds.HERO_MEETING_ARMY_LEFT_TO_RIGHT,
                    ThorActionIds.HERO_MEETING_SWAP_ARMIES, ThorActionIds.HERO_MEETING_ARMY_RIGHT_TO_LEFT};
            for (int index = 0; index < actions.length; ++index)
            {
                if (heroMeetingActionBounds(index, frame, bevel).contains(x, y) && isActionEnabled(actions[index]))
                {
                    NativeMethods.submitThorAction(revision, actions[index], ThorActionIds.NO_TARGET);
                    pendingMeetingAction = true;
                    selectedMeetingSlot = -1;
                    performClick();
                    invalidate();
                    return;
                }
            }
        }

        private void handleHeroMeetingSplitEditorTap(final float x, final float y, final RectF frame,
                                                      final float bevel)
        {
            for (int index = 0; index < 6; ++index)
            {
                if (!heroMeetingSplitButtonBounds(index, frame, bevel).contains(x, y))
                    continue;
                if (index < 4)
                {
                    heroMeetingSplit.adjust(new int[]{-10, -1, 1, 10}[index]);
                }
                else if (index == 4)
                {
                    heroMeetingSplit.cancel();
                    selectedMeetingSlot = -1;
                }
                else if (isActionEnabled(ThorActionIds.HERO_MEETING_SPLIT_STACK))
                {
                    final int source = heroMeetingSplit.sourceKey();
                    final int destination = heroMeetingSplit.destinationKey();
                    NativeMethods.submitThorHeroMeetingSplit(revision, heroMeetingArmies.armyIds[source / 7], source % 7,
                            heroMeetingArmies.armyIds[destination / 7], destination % 7, heroMeetingSplit.amount());
                    pendingMeetingAction = true;
                    heroMeetingSplit.cancel();
                    selectedMeetingSlot = -1;
                }
                performClick();
                setContentDescription(commandDeckDescription());
                invalidate();
                return;
            }
        }

        private void handleHeroMeetingRedistributionTap(final float x, final float y, final RectF frame,
                                                         final float bevel)
        {
            if (!heroMeetingRedistribution.isCurrent(revision)
                    || !ThorContextIds.HERO_MEETING.equals(contextId)
                    || !heroMeetingArmies.complete())
            {
                heroMeetingRedistribution.cancel();
                selectedMeetingSlot = -1;
                setContentDescription(commandDeckDescription());
                invalidate();
                return;
            }
            for (int index = 0; index < ThorHeroMeetingArmies.SLOT_COUNT; ++index)
            {
                if (!heroMeetingSlotBounds(index, frame, bevel).contains(x, y))
                    continue;
                if (heroMeetingRedistribution.selectDestination(revision, index, heroMeetingArmies))
                    selectedMeetingSlot = heroMeetingRedistribution.sourceKey();
                performClick();
                setContentDescription(commandDeckDescription());
                invalidate();
                return;
            }
            for (int index = 0; index < 6; ++index)
            {
                if (!heroMeetingSplitButtonBounds(index, frame, bevel).contains(x, y))
                    continue;
                if (index < 4)
                {
                    heroMeetingRedistribution.adjust(revision, new int[]{-10, -1, 1, 10}[index], heroMeetingArmies);
                }
                else if (index == 4)
                {
                    heroMeetingRedistribution.cancel();
                    selectedMeetingSlot = -1;
                }
                else if (heroMeetingRedistribution.canConfirm(revision, heroMeetingArmies)
                        && isActionEnabled(ThorActionIds.HERO_MEETING_REDISTRIBUTE_STACK))
                {
                    final int[] destinationArmyIds = heroMeetingRedistribution.destinationArmyIds(revision,
                            heroMeetingArmies);
                    final int[] destinationSlots = heroMeetingRedistribution.destinationSlots(revision,
                            heroMeetingArmies);
                    final int[] amounts = heroMeetingRedistribution.amounts(revision, heroMeetingArmies);
                    if (destinationArmyIds.length > 0 && destinationArmyIds.length == destinationSlots.length
                            && destinationArmyIds.length == amounts.length
                            && destinationArmyIds.length <= ThorHeroMeetingRedistributionState.MAX_DESTINATIONS)
                    {
                        final int source = heroMeetingRedistribution.sourceKey();
                        if (NativeMethods.submitThorHeroMeetingRedistribution(revision,
                                heroMeetingArmies.leftHeroId, heroMeetingArmies.rightHeroId,
                                heroMeetingArmies.armyIds[source / 7], source % 7,
                                heroMeetingArmies.creatureIds[source], heroMeetingArmies.counts[source],
                                destinationArmyIds, destinationSlots, amounts))
                        {
                            pendingMeetingAction = true;
                            heroMeetingRedistribution.cancel();
                            selectedMeetingSlot = -1;
                        }
                    }
                }
                performClick();
                setContentDescription(commandDeckDescription());
                invalidate();
                return;
            }
        }

        private void submitHeroMeetingQuickTap(final int sourceKey)
        {
            if (pendingMeetingAction || !heroMeetingArmies.complete() || !heroMeetingArmies.locallyControllable
                    || sourceKey < 0 || sourceKey >= ThorHeroMeetingArmies.SLOT_COUNT
                    || (heroMeetingArmies.flags[sourceKey] & 1) == 0
                    || !isActionEnabled(ThorActionIds.HERO_MEETING_MOVE_STACK))
                return;
            NativeMethods.submitThorAction(revision, ThorActionIds.HERO_MEETING_MOVE_STACK, sourceKey);
            pendingMeetingAction = true;
            performClick();
            setContentDescription(commandDeckDescription());
            invalidate();
        }

        private void drawMainMenuDashboard(final Canvas canvas, final RectF frame, final float dividerY,
                                           final float density)
        {
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(true);
            paint.setColor(TEXT);
            drawFittedText(canvas, title, frame.centerX(), frame.top + frame.height() * 0.16f,
                    frame.width() * 0.78f, Math.min(42f * density, frame.height() * 0.09f));
            paint.setFakeBoldText(false);
            paint.setColor(PARCHMENT_DARK);
            drawFittedText(canvas, status, frame.centerX(), frame.top + frame.height() * 0.27f,
                    frame.width() * 0.78f, Math.min(27f * density, frame.height() * 0.055f));

            for (int control = 1; control <= ThorMainMenuState.choiceCount(contextId); ++control)
            {
                final int actionId = ThorMainMenuState.actionForControl(contextId, control);
                final float[] bounds = ThorMainMenuState.boundsForControl(contextId, control,
                        frame.width(), frame.height(), dividerY - frame.top);
                final RectF button = new RectF(frame.left + bounds[0], frame.top + bounds[1],
                        frame.left + bounds[2], frame.top + bounds[3]);
                drawLobbyScenarioButton(canvas, button,
                        getContext().getString(ThorMainMenuState.labelForControl(contextId, control)),
                        isActionEnabled(actionId), false, density);
            }
        }

        private RectF browserControlBounds(final int control, final RectF frame)
        {
            final float[] bounds = ThorBrowserState.boundsForControl(contextId, control,
                    frame.width(), frame.height());
            return new RectF(frame.left + bounds[0], frame.top + bounds[1],
                    frame.left + bounds[2], frame.top + bounds[3]);
        }

        private void drawBrowserDashboard(final Canvas canvas, final RectF frame, final float density)
        {
            final boolean campaign = ThorContextIds.CAMPAIGN_BROWSER.equals(contextId);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(true);
            paint.setColor(TEXT);
            drawFittedText(canvas, campaign ? title : getContext().getString(R.string.thor_context_load_game),
                    frame.centerX(), frame.top + frame.height() * 0.065f, frame.width() * 0.78f,
                    Math.min(36f * density, frame.height() * 0.060f));
            paint.setFakeBoldText(false);
            drawFittedText(canvas, getContext().getString(R.string.thor_browser_page,
                            browser.pageCount == 0 ? 0 : browser.page + 1, browser.pageCount),
                    frame.centerX(), frame.top + frame.height() * 0.115f, frame.width() * 0.72f,
                    Math.min(26f * density, frame.height() * 0.04f));
            if (!campaign)
            {
                drawFittedText(canvas, title, frame.centerX(), frame.top + frame.height() * 0.157f,
                        frame.width() * 0.82f, Math.min(27f * density, frame.height() * 0.042f));
                drawFittedText(canvas, detailLines[0] + "   " + detailLines[1] + "   " + detailLines[3],
                        frame.centerX(), frame.top + frame.height() * 0.19f,
                        frame.width() * 0.84f, Math.min(22f * density, frame.height() * 0.035f));
            }
            for (int row = 0; row < browser.rowCount(); ++row)
            {
                String label = browser.labels[row];
                if (label.isEmpty())
                    label = getContext().getString(R.string.thor_browser_unavailable);
                if (browser.completed(row))
                    label += "  " + getContext().getString(R.string.thor_browser_completed);
                drawLobbyScenarioButton(canvas,
                        browserControlBounds(ThorBrowserState.CONTROL_FIRST_ROW + row, frame),
                        label, browser.enabled(row) && isActionEnabled(campaign
                                ? ThorActionIds.CAMPAIGN_BROWSER_SELECT : ThorActionIds.LOAD_BROWSER_SELECT),
                        browser.selected(row), density);
            }
            drawLobbyScenarioButton(canvas, browserControlBounds(ThorBrowserState.CONTROL_PREVIOUS, frame),
                    getContext().getString(R.string.thor_browser_previous_page),
                    browserControlEnabled(ThorBrowserState.CONTROL_PREVIOUS), false, density);
            drawLobbyScenarioButton(canvas, browserControlBounds(ThorBrowserState.CONTROL_NEXT, frame),
                    getContext().getString(R.string.thor_browser_next_page),
                    browserControlEnabled(ThorBrowserState.CONTROL_NEXT), false, density);
            if (!campaign)
                drawLobbyScenarioButton(canvas, browserControlBounds(ThorBrowserState.CONTROL_PRIMARY, frame),
                        getContext().getString(R.string.thor_lobby_load),
                        isActionEnabled(ThorActionIds.LOBBY_START_GAME), false, density);
            drawLobbyScenarioButton(canvas, browserControlBounds(ThorBrowserState.CONTROL_BACK, frame),
                    getContext().getString(R.string.thor_lobby_back),
                    isActionEnabled(campaign ? ThorActionIds.CAMPAIGN_BROWSER_BACK : ThorActionIds.LOBBY_BACK),
                    false, density);
        }

        private void drawLobbyScenarioDashboard(final Canvas canvas, final RectF frame, final float dividerY,
                                                final float bevel, final float density)
        {
            final float contentHeight = frame.height();
            final float left = frame.left + bevel * 3f;
            final float width = frame.width() - bevel * 6f;
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(true);
            paint.setColor(TEXT);
            drawFittedText(canvas, getContext().getString(ThorContextIds.LOBBY_LOAD_GAME_SCENARIO.equals(contextId)
                            ? R.string.thor_context_load_game : R.string.thor_context_new_game), frame.centerX(),
                    frame.top + contentHeight * 0.09f, frame.width() * 0.78f,
                    Math.min(27f * density, contentHeight * 0.048f));
            drawFittedText(canvas, title, frame.centerX(), frame.top + contentHeight * 0.20f,
                    frame.width() * 0.84f, Math.min(43f * density, contentHeight * 0.078f));
            paint.setFakeBoldText(false);
            paint.setColor(PARCHMENT_DARK);
            drawFittedText(canvas, status, frame.centerX(), frame.top + contentHeight * 0.285f,
                    frame.width() * 0.82f, Math.min(27f * density, contentHeight * 0.05f));

            final String mapSize = detailLines[0].isEmpty() ? getContext().getString(R.string.thor_battle_not_available)
                    : detailLines[0];
            final String players = detailLines[1].isEmpty() ? getContext().getString(R.string.thor_battle_not_available)
                    : detailLines[1];
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setFakeBoldText(true);
            drawFittedText(canvas, getContext().getString(R.string.thor_lobby_map_size), left,
                    frame.top + contentHeight * 0.365f, width * 0.34f,
                    Math.min(24f * density, contentHeight * 0.044f));
            drawFittedText(canvas, getContext().getString(R.string.thor_lobby_players), left,
                    frame.top + contentHeight * 0.435f, width * 0.34f,
                    Math.min(24f * density, contentHeight * 0.044f));
            paint.setTextAlign(Paint.Align.RIGHT);
            paint.setFakeBoldText(false);
            drawFittedText(canvas, mapSize, frame.right - bevel * 3f,
                    frame.top + contentHeight * 0.365f, width * 0.54f,
                    Math.min(25f * density, contentHeight * 0.046f));
            drawFittedText(canvas, players, frame.right - bevel * 3f,
                    frame.top + contentHeight * 0.435f, width * 0.54f,
                    Math.min(25f * density, contentHeight * 0.046f));

            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(true);
            paint.setColor(PARCHMENT_DARK);
            final boolean loadGame = ThorContextIds.LOBBY_LOAD_GAME_SCENARIO.equals(contextId);
            if (loadGame)
                drawFittedText(canvas, detailLines[3], frame.centerX(),
                        dividerY + (frame.bottom - dividerY) * 0.40f, frame.width() * 0.76f,
                        Math.min(25f * density, (frame.bottom - dividerY) * 0.055f));
            else
                drawFittedText(canvas, getContext().getString(R.string.thor_lobby_difficulty), frame.centerX(),
                        dividerY + (frame.bottom - dividerY) * 0.255f, frame.width() * 0.76f,
                        Math.min(25f * density, (frame.bottom - dividerY) * 0.055f));

            drawLobbyScenarioButton(canvas,
                    lobbyScenarioControlBounds(ThorLobbyScenarioState.CONTROL_PREVIOUS_SCENARIO, frame, dividerY),
                    getContext().getString(R.string.thor_lobby_previous_scenario),
                    isActionEnabled(ThorActionIds.LOBBY_PREVIOUS_SCENARIO), false, density);
            drawLobbyScenarioButton(canvas,
                    lobbyScenarioControlBounds(ThorLobbyScenarioState.CONTROL_NEXT_SCENARIO, frame, dividerY),
                    getContext().getString(R.string.thor_lobby_next_scenario),
                    isActionEnabled(ThorActionIds.LOBBY_NEXT_SCENARIO), false, density);

            final int difficulty = ThorLobbyScenarioState.difficultyIndex(detailLines);
            final int[] difficultyLabels = {
                    R.string.thor_lobby_difficulty_easy,
                    R.string.thor_lobby_difficulty_normal,
                    R.string.thor_lobby_difficulty_hard,
                    R.string.thor_lobby_difficulty_expert,
                    R.string.thor_lobby_difficulty_impossible
            };
            final boolean canSetDifficulty = isActionEnabled(ThorActionIds.LOBBY_SET_DIFFICULTY);
            for (int index = 0; !loadGame && index < ThorLobbyScenarioState.DIFFICULTY_COUNT; ++index)
            {
                final RectF bounds = lobbyScenarioControlBounds(
                        ThorLobbyScenarioState.CONTROL_DIFFICULTY_FIRST + index, frame, dividerY);
                drawLobbyScenarioButton(canvas, bounds, getContext().getString(difficultyLabels[index]),
                        canSetDifficulty, index == difficulty, density);
            }
            drawLobbyScenarioButton(canvas,
                    lobbyScenarioControlBounds(ThorLobbyScenarioState.CONTROL_START, frame, dividerY),
                    getContext().getString(loadGame ? R.string.thor_lobby_load : R.string.thor_lobby_start),
                    isActionEnabled(ThorActionIds.LOBBY_START_GAME), false, density);
            drawLobbyScenarioButton(canvas,
                    lobbyScenarioControlBounds(ThorLobbyScenarioState.CONTROL_BACK, frame, dividerY),
                    getContext().getString(R.string.thor_lobby_back),
                    isActionEnabled(ThorActionIds.LOBBY_BACK), false, density);
        }

        private RectF lobbyScenarioControlBounds(final int control, final RectF frame, final float dividerY)
        {
            final float[] bounds = ThorLobbyScenarioState.boundsForControl(control, frame.width(), frame.height(),
                    dividerY - frame.top);
            return new RectF(frame.left + bounds[0], frame.top + bounds[1],
                    frame.left + bounds[2], frame.top + bounds[3]);
        }

        private void drawCampaignDashboard(final Canvas canvas, final RectF frame, final float dividerY,
                                           final float bevel, final float density)
        {
            final float contentHeight = frame.height();
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(true);
            paint.setColor(TEXT);
            drawFittedText(canvas, status, frame.centerX(), frame.top + contentHeight * 0.10f,
                    frame.width() * 0.82f, Math.min(27f * density, contentHeight * 0.048f));
            drawFittedText(canvas, title, frame.centerX(), frame.top + contentHeight * 0.22f,
                    frame.width() * 0.86f, Math.min(43f * density, contentHeight * 0.078f));
            paint.setColor(PARCHMENT_DARK);
            drawFittedText(canvas, getContext().getString(R.string.thor_campaign_choose_bonus), frame.centerX(),
                    frame.top + contentHeight * 0.36f, frame.width() * 0.82f,
                    Math.min(25f * density, contentHeight * 0.046f));

            drawLobbyScenarioButton(canvas,
                    lobbyScenarioControlBounds(ThorLobbyScenarioState.CONTROL_PREVIOUS_SCENARIO, frame, dividerY),
                    getContext().getString(R.string.thor_campaign_previous_scenario),
                    isActionEnabled(ThorActionIds.CAMPAIGN_PREVIOUS_SCENARIO), false, density);
            drawLobbyScenarioButton(canvas,
                    lobbyScenarioControlBounds(ThorLobbyScenarioState.CONTROL_NEXT_SCENARIO, frame, dividerY),
                    getContext().getString(R.string.thor_campaign_next_scenario),
                    isActionEnabled(ThorActionIds.CAMPAIGN_NEXT_SCENARIO), false, density);
            for (int index = 0; index < 3; ++index)
            {
                final int control = ThorLobbyScenarioState.CONTROL_DIFFICULTY_FIRST + index;
                final int action = ThorActionIds.CAMPAIGN_SELECT_BONUS_1 + index;
                if (detailLines[index].isEmpty() && !isActionEnabled(action))
                    continue;
                drawLobbyScenarioButton(canvas, lobbyScenarioControlBounds(control, frame, dividerY),
                        detailLines[index], isActionEnabled(action), isActionActive(action), density);
            }
            drawLobbyScenarioButton(canvas,
                    lobbyScenarioControlBounds(ThorLobbyScenarioState.CONTROL_START, frame, dividerY),
                    getContext().getString(R.string.thor_lobby_start),
                    isActionEnabled(ThorActionIds.CAMPAIGN_START), false, density);
            drawLobbyScenarioButton(canvas,
                    lobbyScenarioControlBounds(ThorLobbyScenarioState.CONTROL_BACK, frame, dividerY),
                    getContext().getString(R.string.thor_lobby_back),
                    isActionEnabled(ThorActionIds.CAMPAIGN_BACK), false, density);
        }

        private void drawLobbyScenarioButton(final Canvas canvas, final RectF bounds, final String label,
                                             final boolean enabled, final boolean selected, final float density)
        {
            final float radius = Math.max(4f * density, bounds.height() * 0.08f);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(selected ? Color.rgb(105, 81, 37) : enabled ? STONE_DARK : Color.rgb(76, 74, 67));
            canvas.drawRoundRect(bounds, radius, radius, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(selected ? Math.max(3f, radius * 0.28f) : Math.max(2f, radius * 0.18f));
            paint.setColor(selected || enabled ? GOLD : PARCHMENT_DARK);
            canvas.drawRoundRect(bounds, radius, radius, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(enabled || selected);
            paint.setColor(enabled || selected ? TEXT : PARCHMENT_DARK);
            drawFittedText(canvas, label, bounds.centerX(), bounds.centerY(), bounds.width() * 0.88f,
                    Math.min(25f * density, bounds.height() * 0.36f));
            paint.setFakeBoldText(false);
        }

        private void drawHeroDashboard(final Canvas canvas, final RectF frame, final float dividerY,
                                       final float bevel, final float density)
        {
            final float contentHeight = frame.height();
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(true);
            paint.setColor(TEXT);
            drawFittedText(canvas, getContext().getString(R.string.thor_context_hero), frame.centerX(),
                    frame.top + contentHeight * 0.09f, frame.width() * 0.8f,
                    Math.min(24f * density, contentHeight * 0.045f));
            final float titleY = frame.top + contentHeight * 0.20f;
            boolean portraitDrawn = false;
            float portraitTextLeft = 0f;
            if (ThorVisualAssetKey.isHeroPortrait(heroPortraitAssetKey))
            {
                final float iconSize = Math.min(76f * density, contentHeight * 0.16f);
                final RectF icon = new RectF(frame.left + bevel * 3f, titleY - iconSize * 0.5f,
                        frame.left + bevel * 3f + iconSize, titleY + iconSize * 0.5f);
                portraitDrawn = drawVisualAsset(canvas, heroPortraitAssetKey, icon);
                if (portraitDrawn)
                    portraitTextLeft = icon.right + bevel;
            }
            if (portraitDrawn)
            {
                drawFittedText(canvas, title, (portraitTextLeft + frame.right - bevel * 3f) * 0.5f, titleY,
                        frame.right - bevel * 3f - portraitTextLeft, Math.min(42f * density, contentHeight * 0.075f));
            }
            else
                drawFittedText(canvas, title, frame.centerX(), titleY,
                        frame.width() * 0.82f, Math.min(42f * density, contentHeight * 0.075f));

            paint.setFakeBoldText(false);
            paint.setColor(TEXT);
            drawFittedText(canvas, status, frame.centerX(), frame.top + contentHeight * 0.29f,
                    frame.width() * 0.8f, Math.min(28f * density, contentHeight * 0.052f));

            paint.setColor(PARCHMENT_DARK);
            final float detailsTop = dividerY + bevel * 3f;
            final float detailsHeight = frame.top + frame.height() * 0.87f - detailsTop;
            for (int index = 0; index < 3; ++index)
            {
                drawFittedText(canvas, detailLines[index], frame.centerX(),
                        detailsTop + detailsHeight * (index + 0.5f) / 3f, frame.width() * 0.84f,
                        Math.min(29f * density, detailsHeight * 0.16f));
            }
        }

        private void drawTownDashboard(final Canvas canvas, final RectF frame, final float dividerY,
                                       final float bevel, final float density)
        {
            final float contentHeight = frame.height();
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(true);
            paint.setColor(TEXT);
            drawFittedText(canvas, getContext().getString(R.string.thor_context_town), frame.centerX(),
                    frame.top + contentHeight * 0.09f, frame.width() * 0.8f,
                    Math.min(24f * density, contentHeight * 0.045f));
            drawFittedText(canvas, title, frame.centerX(), frame.top + contentHeight * 0.20f,
                    frame.width() * 0.82f, Math.min(42f * density, contentHeight * 0.075f));

            paint.setFakeBoldText(false);
            drawFittedText(canvas, status, frame.centerX(), frame.top + contentHeight * 0.29f,
                    frame.width() * 0.8f, Math.min(28f * density, contentHeight * 0.052f));

            final String[] labels = {
                    getContext().getString(R.string.thor_town_income),
                    getContext().getString(R.string.thor_town_buildings),
                    getContext().getString(R.string.thor_town_visiting_hero),
                    getContext().getString(R.string.thor_town_garrison_hero)
            };
            final String[] values = {
                    getContext().getString(R.string.thor_town_income_value, detailLines[0]),
                    detailLines[1],
                    townHeroName(detailLines[2]),
                    townHeroName(detailLines[3])
            };
            final float gap = Math.max(bevel * 1.25f, 10f);
            final float left = frame.left + bevel * 3f;
            final float top = dividerY + bevel * 3f;
            final float availableWidth = frame.width() - bevel * 6f;
            final float availableHeight = frame.top + frame.height() * 0.56f - top;
            final float cellWidth = (availableWidth - gap) / 2f;
            final float cellHeight = (availableHeight - gap) / 2f;

            for (int index = 0; index < labels.length; ++index)
            {
                final float cellLeft = left + (index % 2) * (cellWidth + gap);
                final float cellTop = top + (index / 2) * (cellHeight + gap);
                final RectF cell = new RectF(cellLeft, cellTop, cellLeft + cellWidth, cellTop + cellHeight);
                paint.setColor(PARCHMENT_DARK);
                paint.setFakeBoldText(true);
                drawFittedText(canvas, labels[index], cell.centerX(), cell.top + cell.height() * 0.30f,
                        cell.width() * 0.9f, Math.min(23f * density, cell.height() * 0.20f));
                paint.setFakeBoldText(false);
                drawFittedText(canvas, values[index], cell.centerX(), cell.top + cell.height() * 0.68f,
                        cell.width() * 0.9f, Math.min(30f * density, cell.height() * 0.27f));
            }
        }

        private void drawTownServices(final Canvas canvas, final RectF frame, final float density)
        {
            final int[] labels = {R.string.thor_town_service_hall, R.string.thor_town_service_recruit,
                    R.string.thor_town_service_tavern, R.string.thor_town_service_mage_guild,
                    R.string.thor_town_service_marketplace};
            for (int service = 0; service < ThorTownServices.SERVICE_COUNT; ++service)
            {
                final float[] box = ThorTownServices.bounds(service, frame.width(), frame.height());
                drawLobbyScenarioButton(canvas, new RectF(frame.left + box[0], frame.top + box[1],
                                frame.left + box[2], frame.top + box[3]),
                        getContext().getString(labels[service]), townServiceEnabled(service), false, density);
            }
        }

        private boolean townServiceEnabled(final int service)
        {
            if (!isActionEnabled(ThorActionIds.TOWN_OPEN_SERVICE))
                return false;
            for (int row = 0; row < browser.rowCount(); ++row)
                if (browser.targets[row] == service)
                    return browser.enabled(row);
            return false;
        }

        private int townServiceAt(final float x, final float y)
        {
            final RectF frame = adventureFrame();
            return ThorTownServices.serviceAt(x - frame.left, y - frame.top, frame.width(), frame.height());
        }

        private boolean handleTownServiceTouch(final MotionEvent event)
        {
            final int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN)
            {
                final int service = townServiceAt(event.getX(), event.getY());
                townServices.begin(service, revision, presentationSessionId, event.getPointerId(0),
                        townServiceEnabled(service));
                return true;
            }
            if (!townServices.isActive())
                return true;
            if (event.getPointerCount() != 1 || event.getPointerId(0) != townServices.pointerId()
                    || action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_POINTER_UP
                    || action == MotionEvent.ACTION_CANCEL)
            {
                townServices.cancel();
                return true;
            }
            if (action == MotionEvent.ACTION_UP)
            {
                final int service = townServices.service();
                final long submittedRevision = townServices.revision();
                if (townServices.finish(townServiceAt(event.getX(), event.getY()), revision,
                        presentationSessionId, event.getPointerId(0), event.getPointerCount(),
                        townServiceEnabled(service),
                        sessionValidity != null && sessionValidity.isCurrent(presentationSessionId)))
                {
                    performClick();
                    NativeMethods.submitThorAction(submittedRevision, ThorActionIds.TOWN_OPEN_SERVICE, service);
                }
            }
            return true;
        }

        private String townHeroName(final String heroName)
        {
            return heroName.isEmpty() ? getContext().getString(R.string.thor_town_none) : heroName;
        }

        private void drawWindowNavigation(final Canvas canvas, final RectF frame,
                                          final float bevel, final float density)
        {
            final int[] labels = {R.string.thor_previous, R.string.thor_next, R.string.thor_lobby_back};
            for (int control = ThorWindowNavigation.PREVIOUS; control <= ThorWindowNavigation.CLOSE; ++control)
            {
                final float[] box = ThorWindowNavigation.bounds(control, frame.width(), frame.height());
                final RectF bounds = new RectF(frame.left + box[0], frame.top + box[1],
                        frame.left + box[2], frame.top + box[3]);
                final boolean enabled = isActionEnabled(ThorWindowNavigation.actionFor(control));
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(enabled ? STONE_DARK : Color.rgb(76, 74, 67));
                canvas.drawRoundRect(bounds, bevel, bevel, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(Math.max(2f, bevel * 0.4f));
                paint.setColor(enabled ? GOLD : PARCHMENT_DARK);
                canvas.drawRoundRect(bounds, bevel, bevel, paint);
                paint.setStyle(Paint.Style.FILL);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setFakeBoldText(enabled);
                paint.setColor(enabled ? TEXT : PARCHMENT_DARK);
                drawFittedText(canvas, getContext().getString(labels[control]), bounds.centerX(),
                        bounds.centerY(), bounds.width() * 0.88f,
                        Math.min(30f * density, bounds.height() * 0.36f));
            }
            paint.setFakeBoldText(false);
        }

        private int windowNavigationControlAt(final float x, final float y)
        {
            final RectF frame = adventureFrame();
            return ThorWindowNavigation.controlAt(x - frame.left, y - frame.top,
                    frame.width(), frame.height());
        }

        private boolean handleWindowNavigationTouch(final MotionEvent event)
        {
            final int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN)
            {
                final int control = windowNavigationControlAt(event.getX(), event.getY());
                windowNavigation.begin(contextId, control, revision, presentationSessionId,
                        event.getPointerId(0), isActionEnabled(ThorWindowNavigation.actionFor(control)));
                return true;
            }
            if (!windowNavigation.isActive())
                return true;
            if (event.getPointerCount() != 1 || event.getPointerId(0) != windowNavigation.pointerId()
                    || action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_POINTER_UP
                    || action == MotionEvent.ACTION_CANCEL)
            {
                windowNavigation.cancel();
                return true;
            }
            if (action == MotionEvent.ACTION_UP)
            {
                final int control = windowNavigation.control();
                final long submittedRevision = windowNavigation.revision();
                if (windowNavigation.finish(contextId, windowNavigationControlAt(event.getX(), event.getY()),
                        revision, presentationSessionId, event.getPointerId(0), event.getPointerCount(),
                        isActionEnabled(ThorWindowNavigation.actionFor(control)),
                        sessionValidity != null && sessionValidity.isCurrent(presentationSessionId)))
                {
                    performClick();
                    NativeMethods.submitThorAction(submittedRevision,
                            ThorWindowNavigation.actionFor(control), ThorActionIds.NO_TARGET);
                }
            }
            return true;
        }

        private void drawBattleDashboard(final Canvas canvas, final RectF frame, final float dividerY,
                                         final float bevel, final float density)
        {
            final boolean tactics = ThorContextIds.BATTLE_TACTICS.equals(contextId);
            final float contentHeight = frame.height();
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(true);
            paint.setColor(TEXT);
            drawFittedText(canvas, getContext().getString(tactics ? R.string.thor_context_battle_tactics
                            : R.string.thor_context_battle), frame.centerX(), frame.top + contentHeight * 0.08f,
                    frame.width() * 0.84f, Math.min(24f * density, contentHeight * 0.042f));

            paint.setFakeBoldText(false);
            final String state = tactics ? getContext().getString(R.string.thor_context_battle_tactics_status)
                    : status.isEmpty() ? getContext().getString(R.string.thor_context_battle_status)
                    : getContext().getString(R.string.thor_battle_round_value, status);
            drawFittedText(canvas, state, frame.centerX(), frame.top + contentHeight * 0.15f,
                    frame.width() * 0.84f, Math.min(27f * density, contentHeight * 0.048f));

            paint.setFakeBoldText(true);
            drawFittedText(canvas, title, frame.centerX(), frame.top + contentHeight * 0.27f,
                    frame.width() * 0.84f, Math.min(42f * density, contentHeight * 0.07f));

            final String[] labels = {
                    getContext().getString(R.string.thor_battle_count),
                    getContext().getString(R.string.thor_battle_attack),
                    getContext().getString(R.string.thor_battle_defense),
                    getContext().getString(R.string.thor_battle_hp)
            };
            final float gap = Math.max(bevel * 1.1f, 8f);
            final float left = frame.left + bevel * 3f;
            final float top = frame.top + contentHeight * 0.35f;
            final float availableWidth = frame.width() - bevel * 6f;
            final float availableHeight = dividerY - bevel * 2f - top;
            final float cellWidth = (availableWidth - gap) / 2f;
            final float cellHeight = (availableHeight - gap) / 2f;
            paint.setColor(TEXT);
            for (int index = 0; index < labels.length; ++index)
            {
                final float cellLeft = left + (index % 2) * (cellWidth + gap);
                final float cellTop = top + (index / 2) * (cellHeight + gap);
                final RectF cell = new RectF(cellLeft, cellTop, cellLeft + cellWidth, cellTop + cellHeight);
                paint.setFakeBoldText(true);
                drawFittedText(canvas, labels[index], cell.centerX(), cell.top + cell.height() * 0.31f,
                        cell.width() * 0.9f, Math.min(22f * density, cell.height() * 0.22f));
                paint.setFakeBoldText(false);
                drawFittedText(canvas, battleValue(index), cell.centerX(), cell.top + cell.height() * 0.70f,
                        cell.width() * 0.9f, Math.min(30f * density, cell.height() * 0.30f));
            }
        }

        private String battleValue(final int index)
        {
            return detailLines[index].isEmpty() ? getContext().getString(R.string.thor_battle_not_available)
                    : detailLines[index];
        }

        private RectF tabBounds(final int index, final RectF frame, final float dividerY, final float bevel)
        {
            final float left = frame.left + bevel * 3f;
            final float gap = Math.max(bevel, 8f);
            final float width = (frame.width() - bevel * 6f - gap * 2f) / 3f;
            final float top = dividerY + bevel * 2f;
            return new RectF(left + index * (width + gap), top,
                    left + index * (width + gap) + width,
                    dividerY + (frame.bottom - dividerY) * ThorAdventureLayout.TAB_END);
        }

        private int tabAt(final float x, final float y)
        {
            final RectF frame = adventureFrame();
            final float bevel = adventureBevel();
            final float dividerY = frame.top + frame.height() * ThorAdventureLayout.DIVIDER;
            for (int index = 0; index < 3; ++index)
                if (tabBounds(index, frame, dividerY, bevel).contains(x, y))
                    return index;
            return -1;
        }

        private RectF adventureFrame()
        {
            final float density = getResources().getDisplayMetrics().density;
            final float scale = Math.min(getWidth() / REFERENCE_WIDTH, getHeight() / REFERENCE_HEIGHT);
            final float margin = Math.max(16f * density, Math.max(Math.min(getWidth(), getHeight()) * 0.035f,
                    Math.min(getWidth(), getHeight()) * 0.045f * scale));
            return new RectF(margin, margin, getWidth() - margin, getHeight() - margin);
        }

        private float adventureBevel()
        {
            return Math.max(3f * getResources().getDisplayMetrics().density, adventureFrame().left * 0.16f);
        }

        private void drawAdventureTabs(final Canvas canvas, final RectF frame, final float dividerY,
                                       final float bevel, final float density)
        {
            final int[] labels = {R.string.thor_tab_actions, R.string.thor_tab_heroes, R.string.thor_tab_towns};
            for (int index = 0; index < labels.length; ++index)
            {
                final RectF tab = tabBounds(index, frame, dividerY, bevel);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(index == adventureTab ? STONE_DARK : PARCHMENT_DARK);
                canvas.drawRoundRect(tab, bevel, bevel, paint);
                paint.setColor(TEXT);
                paint.setFakeBoldText(index == adventureTab);
                paint.setTextAlign(Paint.Align.CENTER);
                drawFittedText(canvas, getContext().getString(labels[index]),
                        tab.centerX(), tab.centerY(), tab.width() * 0.85f, Math.min(26f * density, tab.height() * 0.55f));
            }
            paint.setFakeBoldText(false);
        }

        private RectF heroBounds(final int index, final RectF frame, final float dividerY, final float bevel)
        {
            final float gap = Math.max(bevel, 8f);
            final float top = actionTop(frame, dividerY, bevel);
            final float left = frame.left + bevel * 3f;
            final float width = (frame.width() - bevel * 6f - gap) / ThorAdventureLayout.HERO_COLUMNS;
            final float height = (frame.bottom - bevel * 3f - top - gap * (ThorAdventureLayout.HERO_ROWS - 1))
                    / ThorAdventureLayout.HERO_ROWS;
            final int column = index % ThorAdventureLayout.HERO_COLUMNS;
            final int row = index / ThorAdventureLayout.HERO_COLUMNS;
            return new RectF(left + column * (width + gap), top + row * (height + gap),
                    left + column * (width + gap) + width, top + row * (height + gap) + height);
        }

        private int heroAt(final float x, final float y)
        {
            final RectF frame = adventureFrame();
            final float bevel = adventureBevel();
            final float dividerY = frame.top + frame.height() * ThorAdventureLayout.DIVIDER;
            for (int index = 0; index < heroes.ids.length; ++index)
                if (heroBounds(index, frame, dividerY, bevel).contains(x, y))
                    return index;
            return -1;
        }

        private void drawHeroes(final Canvas canvas, final RectF frame, final float dividerY,
                                final float bevel, final float density)
        {
            paint.setTextAlign(Paint.Align.CENTER);
            if (heroes.ids.length == 0)
            {
                paint.setColor(PARCHMENT_DARK);
                drawFittedText(canvas, getContext().getString(R.string.thor_no_heroes), frame.centerX(),
                        (dividerY + frame.bottom) * 0.55f, frame.width() * 0.8f, 30f * density);
            }
            for (int index = 0; index < heroes.ids.length; ++index)
            {
                final RectF row = heroBounds(index, frame, dividerY, bevel);
                final boolean selected = (heroes.flags[index] & 1) != 0;
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(STONE_DARK);
                canvas.drawRoundRect(row, bevel, bevel, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(selected ? bevel * 0.8f : bevel * 0.3f);
                paint.setColor(selected ? GOLD : STONE_LIGHT);
                canvas.drawRoundRect(row, bevel, bevel, paint);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(TEXT);
                paint.setFakeBoldText(selected);
                final boolean sleeping = (heroes.flags[index] & 2) != 0;
                final String name = (selected ? "● " : "") + heroes.names[index];
                drawEllipsizedText(canvas, name, row.centerX(), row.top + row.height() * (sleeping ? 0.22f : 0.31f),
                        row.width() * 0.87f, Math.min(27f * density, row.height() * 0.24f));
                paint.setFakeBoldText(false);
                drawFittedText(canvas, heroes.movement[index] + " / " + heroes.maximum[index], row.centerX(),
                        row.top + row.height() * (sleeping ? 0.53f : 0.71f), row.width() * 0.8f,
                        Math.min(23f * density, row.height() * 0.23f));
                if (sleeping)
                    drawFittedText(canvas, getContext().getString(R.string.thor_hero_sleeping), row.centerX(),
                            row.top + row.height() * 0.82f, row.width() * 0.8f,
                            Math.min(21f * density, row.height() * 0.19f));
            }
        }

        private int townPageCount()
        {
            return Math.max(1, (towns.ids.length + ThorAdventureLayout.TOWN_ROWS_PER_PAGE - 1)
                    / ThorAdventureLayout.TOWN_ROWS_PER_PAGE);
        }

        private void syncTownPageToSelection()
        {
            final int pages = townPageCount();
            townPage = Math.min(Math.max(0, townPage), pages - 1);
            for (int index = 0; index < towns.ids.length; ++index)
                if ((towns.flags[index] & 1) != 0)
                {
                    townPage = index / ThorAdventureLayout.TOWN_ROWS_PER_PAGE;
                    return;
                }
        }

        private RectF townBounds(final int pageRow, final RectF frame, final float dividerY, final float bevel)
        {
            final float gap = Math.max(bevel, 8f);
            final float top = actionTop(frame, dividerY, bevel);
            final float left = frame.left + bevel * 3f;
            final float width = frame.width() - bevel * 6f;
            final float navigationHeight = Math.max(34f * getResources().getDisplayMetrics().density,
                    (frame.bottom - top) * 0.11f);
            final float bottom = frame.bottom - bevel * 3f - navigationHeight - gap;
            final float height = (bottom - top - gap * (ThorAdventureLayout.TOWN_ROWS_PER_PAGE - 1))
                    / ThorAdventureLayout.TOWN_ROWS_PER_PAGE;
            final float rowTop = top + pageRow * (height + gap);
            return new RectF(left, rowTop, left + width, rowTop + height);
        }

        private RectF townPreviousBounds()
        {
            final RectF frame = adventureFrame();
            final float bevel = adventureBevel();
            final float dividerY = frame.top + frame.height() * ThorAdventureLayout.DIVIDER;
            final RectF lastRow = townBounds(ThorAdventureLayout.TOWN_ROWS_PER_PAGE - 1, frame, dividerY, bevel);
            final float gap = Math.max(bevel, 8f);
            final float top = lastRow.bottom + gap;
            final float width = (lastRow.width() - gap * 2f) * 0.30f;
            return new RectF(lastRow.left, top, lastRow.left + width, frame.bottom - bevel * 3f);
        }

        private RectF townNextBounds()
        {
            final RectF previous = townPreviousBounds();
            final RectF frame = adventureFrame();
            final float bevel = adventureBevel();
            return new RectF(frame.right - bevel * 3f - previous.width(), previous.top,
                    frame.right - bevel * 3f, previous.bottom);
        }

        private int townAt(final float x, final float y)
        {
            final RectF frame = adventureFrame();
            final float bevel = adventureBevel();
            final float dividerY = frame.top + frame.height() * ThorAdventureLayout.DIVIDER;
            final int first = townPage * ThorAdventureLayout.TOWN_ROWS_PER_PAGE;
            final int count = Math.min(ThorAdventureLayout.TOWN_ROWS_PER_PAGE, towns.ids.length - first);
            for (int row = 0; row < count; ++row)
                if (townBounds(row, frame, dividerY, bevel).contains(x, y))
                    return first + row;
            return -1;
        }

        private void drawTowns(final Canvas canvas, final RectF frame, final float dividerY,
                               final float bevel, final float density)
        {
            paint.setTextAlign(Paint.Align.CENTER);
            if (towns.ids.length == 0)
            {
                paint.setColor(PARCHMENT_DARK);
                drawFittedText(canvas, getContext().getString(R.string.thor_no_towns), frame.centerX(),
                        (dividerY + frame.bottom) * 0.55f, frame.width() * 0.8f, 30f * density);
                return;
            }

            final int first = townPage * ThorAdventureLayout.TOWN_ROWS_PER_PAGE;
            final int count = Math.min(ThorAdventureLayout.TOWN_ROWS_PER_PAGE, towns.ids.length - first);
            for (int rowIndex = 0; rowIndex < count; ++rowIndex)
            {
                final int index = first + rowIndex;
                final RectF row = townBounds(rowIndex, frame, dividerY, bevel);
                final boolean selected = (towns.flags[index] & 1) != 0;
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(STONE_DARK);
                canvas.drawRoundRect(row, bevel, bevel, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(selected ? bevel * 0.8f : bevel * 0.3f);
                paint.setColor(selected ? GOLD : STONE_LIGHT);
                canvas.drawRoundRect(row, bevel, bevel, paint);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(TEXT);
                paint.setFakeBoldText(selected);
                drawEllipsizedText(canvas, (selected ? "● " : "") + towns.names[index], row.centerX(), row.centerY(),
                        row.width() * 0.87f, Math.min(28f * density, row.height() * 0.45f));
            }
            paint.setFakeBoldText(false);

            if (townPageCount() > 1)
            {
                final RectF previous = townPreviousBounds();
                final RectF next = townNextBounds();
                drawTownPageControl(canvas, previous, R.string.thor_town_previous, townPage > 0, bevel, density);
                drawTownPageControl(canvas, next, R.string.thor_town_next, townPage + 1 < townPageCount(), bevel, density);
                paint.setColor(PARCHMENT_DARK);
                paint.setTextAlign(Paint.Align.CENTER);
                drawFittedText(canvas, getContext().getString(R.string.thor_town_page, townPage + 1, townPageCount()),
                        frame.centerX(), previous.centerY(), frame.width() * 0.28f, Math.min(20f * density, previous.height() * 0.5f));
            }
        }

        private void drawTownPageControl(final Canvas canvas, final RectF bounds, final int label, final boolean enabled,
                                         final float bevel, final float density)
        {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(enabled ? STONE_DARK : Color.rgb(76, 74, 67));
            canvas.drawRoundRect(bounds, bevel, bevel, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(2f, bevel * 0.3f));
            paint.setColor(enabled ? GOLD : PARCHMENT_DARK);
            canvas.drawRoundRect(bounds, bevel, bevel, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(enabled ? TEXT : PARCHMENT_DARK);
            paint.setTextAlign(Paint.Align.CENTER);
            drawFittedText(canvas, getContext().getString(label), bounds.centerX(), bounds.centerY(), bounds.width() * 0.75f,
                    Math.min(20f * density, bounds.height() * 0.5f));
        }

        private void drawAdventureActions(final Canvas canvas, final RectF frame, final float dividerY,
                                          final float bevel, final float density)
        {
            final int[] actions = {
                    ThorActionIds.NEXT_HERO,
                    ThorActionIds.MOVE_HERO,
                    ThorActionIds.TOGGLE_HERO_SLEEP,
                    ThorActionIds.END_TURN,
                    ThorActionIds.OPEN_KINGDOM_OVERVIEW,
                    ThorActionIds.OPEN_QUEST_LOG,
                    ThorActionIds.OPEN_PUZZLE_MAP,
                    ThorActionIds.OPEN_SAVE_GAME
            };
            final String[] labels = {
                    getContext().getString(R.string.thor_action_next_hero),
                    getContext().getString(R.string.thor_action_move_hero),
                    isActionActive(ThorActionIds.TOGGLE_HERO_SLEEP)
                            ? getContext().getString(R.string.thor_action_wake_hero)
                            : getContext().getString(R.string.thor_action_sleep_hero),
                    getContext().getString(R.string.thor_action_end_turn),
                    getContext().getString(R.string.thor_action_kingdom),
                    getContext().getString(R.string.thor_action_quest_log),
                    getContext().getString(R.string.thor_action_puzzle_map),
                    getContext().getString(R.string.thor_action_save_game)
            };

            final float actionTop = actionTop(frame, dividerY, bevel);
            final float sectionTextSize = Math.min(21f * density, (frame.bottom - dividerY) * 0.045f);
            paint.setStyle(Paint.Style.FILL);
            paint.setFakeBoldText(true);
            paint.setColor(PARCHMENT_DARK);
            paint.setTextAlign(Paint.Align.LEFT);
            drawFittedText(canvas, getContext().getString(R.string.thor_action_section_gameplay),
                    frame.left + bevel * 3f, actionTop - sectionTextSize * 0.7f, frame.width() * 0.4f, sectionTextSize);
            final RectF utilityFirstButton = actionBounds(4, frame, dividerY, bevel);
            drawFittedText(canvas, getContext().getString(R.string.thor_action_section_utilities),
                    frame.left + bevel * 3f, utilityFirstButton.top - sectionGap(frame, dividerY, bevel) * 0.5f,
                    frame.width() * 0.4f, sectionTextSize);
            paint.setTextAlign(Paint.Align.CENTER);

            for (int index = 0; index < actions.length; ++index)
            {
                final RectF button = actionBounds(index, frame, dividerY, bevel);
                final boolean enabled = isActionEnabled(actions[index]);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(enabled ? STONE_DARK : Color.rgb(76, 74, 67));
                canvas.drawRoundRect(button, bevel, bevel, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(Math.max(2f, bevel * 0.4f));
                paint.setColor(enabled ? GOLD : PARCHMENT_DARK);
                canvas.drawRoundRect(button, bevel, bevel, paint);
                paint.setStyle(Paint.Style.FILL);
                paint.setFakeBoldText(enabled);
                paint.setColor(enabled ? TEXT : PARCHMENT_DARK);
                drawFittedText(canvas, labels[index], button.centerX(), button.centerY(), button.width() * 0.84f,
                        Math.min(28f * density, button.height() * 0.28f));
            }
            paint.setFakeBoldText(false);
        }

        private void drawBattleActions(final Canvas canvas, final RectF frame, final float dividerY,
                                       final float bevel, final float density)
        {
            final boolean tactics = ThorContextIds.BATTLE_TACTICS.equals(contextId);
            final int[] actions = tactics
                    ? new int[]{ThorActionIds.BATTLE_TACTICS_NEXT, ThorActionIds.BATTLE_TACTICS_END}
                    : new int[]{ThorActionIds.BATTLE_WAIT, ThorActionIds.BATTLE_DEFEND};
            final String[] labels = tactics
                    ? new String[]{getContext().getString(R.string.thor_action_next_unit),
                    getContext().getString(R.string.thor_action_start_battle)}
                    : new String[]{getContext().getString(R.string.thor_action_wait),
                    getContext().getString(R.string.thor_action_defend)};

            for (int index = 0; index < actions.length; ++index)
            {
                final RectF button = battleActionBounds(index, frame, dividerY, bevel);
                final boolean enabled = isActionEnabled(actions[index]);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(enabled ? STONE_DARK : Color.rgb(76, 74, 67));
                canvas.drawRoundRect(button, bevel, bevel, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(Math.max(2f, bevel * 0.4f));
                paint.setColor(enabled ? GOLD : PARCHMENT_DARK);
                canvas.drawRoundRect(button, bevel, bevel, paint);
                paint.setStyle(Paint.Style.FILL);
                paint.setFakeBoldText(enabled);
                paint.setColor(enabled ? TEXT : PARCHMENT_DARK);
                drawFittedText(canvas, labels[index], button.centerX(), button.centerY(), button.width() * 0.84f,
                        Math.min(34f * density, button.height() * 0.28f));
            }
            paint.setFakeBoldText(false);
        }

        private RectF actionBounds(final int index, final RectF frame, final float dividerY, final float bevel)
        {
            final float gap = Math.max(bevel * 1.25f, 10f);
            final float left = frame.left + bevel * 3f;
            final float right = frame.right - bevel * 3f;
            final float top = actionTop(frame, dividerY, bevel);
            final float bottom = frame.bottom - bevel * 3f;
            final float columnWidth = (right - left - gap) / 2f;
            final float sectionGap = sectionGap(frame, dividerY, bevel);
            final float rowHeight = (bottom - top - gap * 3f - sectionGap) / 4f;
            final int column = index % 2;
            final int row = index / 2;
            final float rowTop = top + row * (rowHeight + gap) + (row >= 2 ? sectionGap - gap : 0f);
            return new RectF(left + column * (columnWidth + gap), rowTop,
                    left + column * (columnWidth + gap) + columnWidth, rowTop + rowHeight);
        }

        private float actionTop(final RectF frame, final float dividerY, final float bevel)
        {
            return dividerY + (frame.bottom - dividerY) * (ThorContextIds.ADVENTURE_MAP.equals(contextId)
                    ? ThorAdventureLayout.CONTENT_START : 0.13f) + bevel;
        }

        private float sectionGap(final RectF frame, final float dividerY, final float bevel)
        {
            return Math.max(bevel * 4f, (frame.bottom - dividerY) * 0.09f);
        }

        private int lobbyScenarioControlAt(final float x, final float y)
        {
            final float density = getResources().getDisplayMetrics().density;
            final float referenceScale = Math.min(getWidth() / REFERENCE_WIDTH, getHeight() / REFERENCE_HEIGHT);
            final float margin = Math.max(16f * density,
                    Math.max(Math.min(getWidth(), getHeight()) * 0.035f,
                            Math.min(getWidth(), getHeight()) * 0.045f * referenceScale));
            final RectF frame = new RectF(margin, margin, getWidth() - margin, getHeight() - margin);
            final float dividerY = frame.top + frame.height() * 0.48f;
            return ThorLobbyScenarioState.controlAt(x - frame.left, y - frame.top,
                    frame.width(), frame.height(), dividerY - frame.top);
        }

        private int browserControlAt(final float x, final float y)
        {
            final RectF frame = adventureFrame();
            return ThorBrowserState.controlAt(contextId, x - frame.left, y - frame.top,
                    frame.width(), frame.height(), browser.rowCount());
        }

        private int browserTarget(final int control)
        {
            final int row = control - ThorBrowserState.CONTROL_FIRST_ROW;
            return control >= ThorBrowserState.CONTROL_FIRST_ROW && row < browser.rowCount()
                    ? browser.targets[row] : ThorActionIds.NO_TARGET;
        }

        private boolean browserControlEnabled(final int control)
        {
            final int actionId = ThorBrowserState.actionForControl(contextId, control);
            if (actionId == ThorActionIds.NONE || !isActionEnabled(actionId))
                return false;
            if (control == ThorBrowserState.CONTROL_PREVIOUS)
                return browser.pageCount > 0 && browser.page > 0;
            if (control == ThorBrowserState.CONTROL_NEXT)
                return browser.pageCount > 0 && browser.page + 1 < browser.pageCount;
            if (control >= ThorBrowserState.CONTROL_FIRST_ROW
                    && control < ThorBrowserState.CONTROL_FIRST_ROW + ThorBrowserState.MAX_ROWS)
                return browser.enabled(control - ThorBrowserState.CONTROL_FIRST_ROW);
            return true;
        }

        private boolean handleBrowserTouch(final MotionEvent event)
        {
            final int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN)
            {
                final int control = browserControlAt(event.getX(), event.getY());
                browserGesture.begin(contextId, control, browserTarget(control), revision,
                        presentationSessionId, event.getPointerId(0), browserControlEnabled(control));
                return true;
            }
            if (!browserGesture.isActive())
                return true;
            if (event.getPointerCount() != 1 || event.getPointerId(0) != browserGesture.pointerId()
                    || action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_POINTER_UP)
            {
                browserGesture.cancel();
                return true;
            }
            if (action == MotionEvent.ACTION_CANCEL)
            {
                browserGesture.cancel();
                return true;
            }
            if (action != MotionEvent.ACTION_UP)
                return true;
            final int control = browserGesture.control();
            final int actionId = ThorBrowserState.actionForControl(contextId, control);
            final int target = browserGesture.target();
            final long submittedRevision = browserGesture.revision();
            final long submittedSession = browserGesture.session();
            final boolean accepted = browserGesture.finish(contextId,
                    browserControlAt(event.getX(), event.getY()),
                    browserTarget(browserControlAt(event.getX(), event.getY())),
                    event.getPointerCount(), event.getPointerId(0), revision, presentationSessionId,
                    browserControlEnabled(control), sessionValidity != null
                            && sessionValidity.isCurrent(submittedSession));
            if (accepted)
            {
                performClick();
                NativeMethods.submitThorAction(submittedRevision, actionId, target);
            }
            return true;
        }

        private int mainMenuControlAt(final float x, final float y)
        {
            final RectF frame = adventureFrame();
            final float dividerY = frame.height() * 0.34f;
            return ThorMainMenuState.controlAt(contextId, x - frame.left, y - frame.top,
                    frame.width(), frame.height(), dividerY);
        }

        private void cancelMainMenuTouch()
        {
            mainMenuGesture.cancel();
        }

        private boolean handleMainMenuTouch(final MotionEvent event)
        {
            final int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN)
            {
                final int control = mainMenuControlAt(event.getX(), event.getY());
                final int actionId = ThorMainMenuState.actionForControl(contextId, control);
                mainMenuGesture.begin(contextId, control, revision, presentationSessionId,
                        event.getPointerId(0), actionId != ThorActionIds.NONE && isActionEnabled(actionId));
                return true;
            }
            if (!mainMenuGesture.isActive())
                return true;
            if (event.getPointerCount() != 1 || event.getPointerId(0) != mainMenuGesture.pointerId()
                    || action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_POINTER_UP)
            {
                cancelMainMenuTouch();
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE)
                return true;
            if (action == MotionEvent.ACTION_CANCEL)
            {
                cancelMainMenuTouch();
                return true;
            }
            if (action != MotionEvent.ACTION_UP)
                return true;

            final int control = mainMenuGesture.control();
            final int releasedControl = mainMenuControlAt(event.getX(), event.getY());
            final long submittedRevision = mainMenuGesture.revision();
            final long submittedSession = mainMenuGesture.session();
            final int actionId = ThorMainMenuState.actionForControl(contextId, control);
            final boolean enabledAtUp = actionId != ThorActionIds.NONE && isActionEnabled(actionId);
            final boolean sessionIsCurrent = sessionValidity != null && sessionValidity.isCurrent(submittedSession);
            if (mainMenuGesture.finish(contextId, releasedControl, event.getPointerCount(), event.getPointerId(0),
                    revision, presentationSessionId, enabledAtUp, sessionIsCurrent))
            {
                performClick();
                NativeMethods.submitThorAction(submittedRevision, actionId, ThorActionIds.NO_TARGET);
            }
            return true;
        }

        private void cancelLobbyScenarioTouch()
        {
            lobbyScenarioGesture.cancel();
        }

        private boolean handleLobbyScenarioTouch(final MotionEvent event)
        {
            final int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN)
            {
                final int control = lobbyScenarioControlAt(event.getX(), event.getY());
                final int actionId = ThorContextIds.CAMPAIGN_BONUS_SELECTION.equals(contextId)
                        ? ThorCampaignState.actionForControl(control)
                        : ThorLobbyScenarioState.actionForControl(control);
                lobbyScenarioGesture.begin(control, revision, presentationSessionId, event.getPointerId(0),
                        actionId != ThorActionIds.NONE && isActionEnabled(actionId));
                return true;
            }
            if (!lobbyScenarioGesture.isActive())
                return true;
            if (event.getPointerCount() != 1 || event.getPointerId(0) != lobbyScenarioGesture.pointerId()
                    || action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_POINTER_UP)
            {
                cancelLobbyScenarioTouch();
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE)
                return true;
            if (action == MotionEvent.ACTION_CANCEL)
            {
                cancelLobbyScenarioTouch();
                return true;
            }
            if (action != MotionEvent.ACTION_UP)
                return true;

            final int control = lobbyScenarioGesture.control();
            final int releasedControl = lobbyScenarioControlAt(event.getX(), event.getY());
            final long submittedRevision = lobbyScenarioGesture.revision();
            final long submittedSession = lobbyScenarioGesture.session();
            final boolean campaignBonus = ThorContextIds.CAMPAIGN_BONUS_SELECTION.equals(contextId);
            final int actionId = campaignBonus ? ThorCampaignState.actionForControl(control)
                    : ThorLobbyScenarioState.actionForControl(control);
            final boolean actionEnabledAtUp = actionId != ThorActionIds.NONE && isActionEnabled(actionId);
            final boolean sessionIsCurrent = sessionValidity != null && sessionValidity.isCurrent(submittedSession);
            if (!lobbyScenarioGesture.finish(releasedControl, event.getPointerCount(), event.getPointerId(0),
                    revision, presentationSessionId, actionEnabledAtUp, sessionIsCurrent))
                return true;

            final int targetId = campaignBonus ? ThorActionIds.NO_TARGET
                    : ThorLobbyScenarioState.difficultyTargetForControl(control);
            if (targetId != ThorActionIds.NO_TARGET
                    && !ThorLobbyScenarioState.shouldDispatchDifficulty(
                            ThorLobbyScenarioState.difficultyIndex(detailLines), targetId))
                return true;
            if (actionId != ThorActionIds.NONE && isActionEnabled(actionId))
            {
                performClick();
                NativeMethods.submitThorAction(submittedRevision, actionId, targetId);
            }
            return true;
        }

        private int actionAt(final float x, final float y)
        {
            final float density = getResources().getDisplayMetrics().density;
            final float referenceScale = Math.min(getWidth() / REFERENCE_WIDTH, getHeight() / REFERENCE_HEIGHT);
            final float margin = Math.max(16f * density,
                    Math.max(Math.min(getWidth(), getHeight()) * 0.035f,
                            Math.min(getWidth(), getHeight()) * 0.045f * referenceScale));
            final RectF frame = new RectF(margin, margin, getWidth() - margin, getHeight() - margin);
            final float bevel = Math.max(3f * density, margin * 0.16f);
            final boolean battleDashboard = ThorContextIds.BATTLE.equals(contextId)
                    || ThorContextIds.BATTLE_TACTICS.equals(contextId);
            final float dividerY = frame.top + (getHeight() - margin * 2f) * (battleDashboard ? 0.55f
                    : ThorAdventureLayout.DIVIDER);
            if (ThorContextIds.BATTLE.equals(contextId) || ThorContextIds.BATTLE_TACTICS.equals(contextId))
            {
                final int[] actions = ThorContextIds.BATTLE_TACTICS.equals(contextId)
                        ? new int[]{ThorActionIds.BATTLE_TACTICS_NEXT, ThorActionIds.BATTLE_TACTICS_END}
                        : new int[]{ThorActionIds.BATTLE_WAIT, ThorActionIds.BATTLE_DEFEND};
                for (int index = 0; index < actions.length; ++index)
                    if (battleActionBounds(index, frame, dividerY, bevel).contains(x, y))
                        return actions[index];
                return ThorActionIds.NONE;
            }

            final int[] actions = {
                    ThorActionIds.NEXT_HERO,
                    ThorActionIds.MOVE_HERO,
                    ThorActionIds.TOGGLE_HERO_SLEEP,
                    ThorActionIds.END_TURN,
                    ThorActionIds.OPEN_KINGDOM_OVERVIEW,
                    ThorActionIds.OPEN_QUEST_LOG,
                    ThorActionIds.OPEN_PUZZLE_MAP,
                    ThorActionIds.OPEN_SAVE_GAME
            };
            for (int index = 0; index < actions.length; ++index)
                if (actionBounds(index, frame, dividerY, bevel).contains(x, y))
                    return actions[index];
            return ThorActionIds.NONE;
        }

        private RectF battleActionBounds(final int index, final RectF frame, final float dividerY, final float bevel)
        {
            final float gap = Math.max(bevel * 1.5f, 12f);
            final float left = frame.left + bevel * 3f;
            final float right = frame.right - bevel * 3f;
            final float top = actionTop(frame, dividerY, bevel);
            final float bottom = frame.bottom - bevel * 3f;
            final float buttonHeight = (bottom - top - gap) / 2f;
            final float buttonTop = top + index * (buttonHeight + gap);
            return new RectF(left, buttonTop, right, buttonTop + buttonHeight);
        }

        private boolean isActionEnabled(final int actionId)
        {
            return ThorLobbyScenarioState.isActionEnabled(enabledActionMask, actionId);
        }

        private boolean isActionActive(final int actionId)
        {
            return (activeActionMask & ThorActionIds.maskFor(actionId)) != 0;
        }

        private String difficultyLabel(final int difficulty)
        {
            switch (difficulty)
            {
                case 0: return getContext().getString(R.string.thor_lobby_difficulty_easy);
                case 1: return getContext().getString(R.string.thor_lobby_difficulty_normal);
                case 2: return getContext().getString(R.string.thor_lobby_difficulty_hard);
                case 3: return getContext().getString(R.string.thor_lobby_difficulty_expert);
                case 4: return getContext().getString(R.string.thor_lobby_difficulty_impossible);
                default: return getContext().getString(R.string.thor_battle_not_available);
            }
        }

        private String commandDeckDescription()
        {
            if (ThorMainMenuState.choiceCount(contextId) > 0)
            {
                final StringBuilder description = new StringBuilder(title).append(". ").append(status);
                for (int control = 1; control <= ThorMainMenuState.choiceCount(contextId); ++control)
                {
                    final int actionId = ThorMainMenuState.actionForControl(contextId, control);
                    if (isActionEnabled(actionId))
                        description.append(". ").append(getContext().getString(
                                ThorMainMenuState.labelForControl(contextId, control)));
                }
                return description.toString();
            }
            if (ThorContextIds.LOBBY_NEW_GAME_SCENARIO.equals(contextId)
                    || ThorContextIds.LOBBY_LOAD_GAME_SCENARIO.equals(contextId))
            {
                final boolean loadGame = ThorContextIds.LOBBY_LOAD_GAME_SCENARIO.equals(contextId);
                final String difficulty = difficultyLabel(ThorLobbyScenarioState.difficultyIndex(detailLines));
                return title + ". " + status + ". " + getContext().getString(R.string.thor_lobby_map_size)
                        + " " + detailLines[0] + ". " + getContext().getString(R.string.thor_lobby_players)
                        + " " + detailLines[1] + ". " + (loadGame ? "" : getContext().getString(R.string.thor_lobby_difficulty)
                        + " " + difficulty + ". ") + getContext().getString(R.string.thor_lobby_previous_scenario)
                        + ", " + getContext().getString(R.string.thor_lobby_next_scenario) + ", "
                        + getContext().getString(loadGame ? R.string.thor_lobby_load : R.string.thor_lobby_start) + ", "
                        + getContext().getString(R.string.thor_lobby_back);
            }
            if (ThorContextIds.CAMPAIGN_BONUS_SELECTION.equals(contextId))
            {
                final StringBuilder description = new StringBuilder(status).append(". ").append(title)
                        .append(". ").append(getContext().getString(R.string.thor_campaign_choose_bonus));
                for (final String detail : detailLines)
                    if (!detail.isEmpty())
                        description.append(". ").append(detail);
                return description.append(". ")
                        .append(getContext().getString(R.string.thor_campaign_previous_scenario)).append(", ")
                        .append(getContext().getString(R.string.thor_campaign_next_scenario)).append(", ")
                        .append(getContext().getString(R.string.thor_lobby_start)).append(", ")
                        .append(getContext().getString(R.string.thor_lobby_back)).toString();
            }

            if (ThorContextIds.HERO_WINDOW.equals(contextId))
                return title + ". " + status + ". " + detailLines[0] + ". " + detailLines[1] + ". " + detailLines[2];

            if (ThorContextIds.TOWN_WINDOW.equals(contextId))
                return title + ". " + status + ". "
                        + getContext().getString(R.string.thor_town_income) + " " + detailLines[0] + ". "
                        + getContext().getString(R.string.thor_town_buildings) + " " + detailLines[1] + ". "
                        + getContext().getString(R.string.thor_town_visiting_hero) + " " + townHeroName(detailLines[2]) + ". "
                        + getContext().getString(R.string.thor_town_garrison_hero) + " " + townHeroName(detailLines[3]);

            if (ThorContextIds.BATTLE.equals(contextId))
                return title + ". " + (status.isEmpty() ? getContext().getString(R.string.thor_context_battle_status)
                        : getContext().getString(R.string.thor_battle_round_value, status)) + ". "
                        + getContext().getString(R.string.thor_battle_count) + " " + battleValue(0) + ". "
                        + getContext().getString(R.string.thor_battle_attack) + " " + battleValue(1) + ". "
                        + getContext().getString(R.string.thor_battle_defense) + " " + battleValue(2) + ". "
                        + getContext().getString(R.string.thor_battle_hp) + " " + battleValue(3) + ". "
                        + getContext().getString(R.string.thor_action_wait) + ", "
                        + getContext().getString(R.string.thor_action_defend);

            if (ThorContextIds.BATTLE_TACTICS.equals(contextId))
                return title + ". " + getContext().getString(R.string.thor_context_battle_tactics_status) + ". "
                        + getContext().getString(R.string.thor_battle_count) + " " + battleValue(0) + ". "
                        + getContext().getString(R.string.thor_battle_attack) + " " + battleValue(1) + ". "
                        + getContext().getString(R.string.thor_battle_defense) + " " + battleValue(2) + ". "
                        + getContext().getString(R.string.thor_battle_hp) + " " + battleValue(3) + ". "
                        + getContext().getString(R.string.thor_action_next_unit) + ", "
                        + getContext().getString(R.string.thor_action_start_battle);

            if (ThorContextIds.HERO_MEETING.equals(contextId))
            {
                if (heroMeetingMode.mode() == ThorHeroMeetingModeState.ARTIFACTS)
                {
                    final StringBuilder description = new StringBuilder(title).append(". ")
                            .append(getContext().getString(R.string.thor_meeting_artifacts));
                    if (heroMeetingArtifacts.complete())
                        for (int side = 0; side < 2; ++side)
                        {
                            description.append(". ").append(heroMeetingArtifacts.heroNames[side]);
                            for (int row = 0; row < 6; ++row)
                            {
                                final int local = artifactPage * 6 + row;
                                if (local >= ThorHeroMeetingArtifacts.PER_HERO)
                                    break;
                                final int index = side * ThorHeroMeetingArtifacts.PER_HERO + local;
                                description.append(". ").append((heroMeetingArtifacts.flags[index] & 1) != 0
                                        ? heroMeetingArtifacts.names[index]
                                        : getContext().getString(R.string.thor_army_empty));
                                if ((heroMeetingArtifacts.flags[index] & 2) != 0)
                                    description.append(" ").append(getContext().getString(R.string.thor_artifact_locked));
                            }
                        }
                    return description.toString();
                }
                if (heroMeetingRedistribution.isActive())
                    return title + ". " + getContext().getString(R.string.thor_redistribution_instructions,
                            heroMeetingArmies.creatureNames[heroMeetingRedistribution.sourceKey()],
                            heroMeetingRedistribution.sourceCount()) + ". "
                            + getContext().getString(R.string.thor_redistribution_totals,
                                    heroMeetingRedistribution.totalAllocated(),
                                    heroMeetingRedistribution.maximumAllocated(),
                                    heroMeetingRedistribution.remaining());
                if (heroMeetingSplit.stage() == ThorHeroMeetingSplitState.Stage.DESTINATION)
                    return title + ". " + getContext().getString(R.string.thor_split_select_destination) + ". "
                            + getContext().getString(R.string.thor_redistribution_enter) + ", "
                            + getContext().getString(R.string.thor_split_cancel);
                if (heroMeetingSplit.stage() == ThorHeroMeetingSplitState.Stage.AMOUNT)
                    return title + ". " + getContext().getString(R.string.thor_split_amount) + " "
                            + heroMeetingSplit.amount() + ". "
                            + getContext().getString(R.string.thor_split_minus_ten) + ", "
                            + getContext().getString(R.string.thor_split_minus_one) + ", "
                            + getContext().getString(R.string.thor_split_plus_one) + ", "
                            + getContext().getString(R.string.thor_split_plus_ten) + ", "
                            + getContext().getString(R.string.thor_split_cancel)
                            + ", " + getContext().getString(R.string.thor_split_confirm);
                return title + ". " + status + ". " + getContext().getString(R.string.thor_split_accessibility);
            }

            if (!ThorContextIds.ADVENTURE_MAP.equals(contextId))
                return title + ". " + status;

            if (adventureTab == 1)
            {
                final StringBuilder description = new StringBuilder(getContext().getString(R.string.thor_tab_heroes));
                if (heroes.ids.length == 0)
                    return description.append(". ").append(getContext().getString(R.string.thor_no_heroes)).toString();
                for (int index = 0; index < heroes.ids.length; ++index)
                    description.append(". ").append(heroes.names[index]).append(" ")
                            .append(heroes.movement[index]).append(" / ").append(heroes.maximum[index])
                            .append((heroes.flags[index] & 1) != 0 ? " ●" : "")
                            .append((heroes.flags[index] & 2) != 0 ? " " + getContext().getString(R.string.thor_hero_sleeping) : "");
                return description.toString();
            }

            if (adventureTab == 2)
            {
                final StringBuilder description = new StringBuilder(getContext().getString(R.string.thor_tab_towns));
                if (towns.ids.length == 0)
                    return description.append(". ").append(getContext().getString(R.string.thor_no_towns)).toString();
                for (int index = 0; index < towns.ids.length; ++index)
                    description.append(". ").append(towns.names[index])
                            .append((towns.flags[index] & 1) != 0 ? " ●" : "");
                return description.toString();
            }

            return title + ". " + status + ". "
                    + getContext().getString(R.string.thor_action_next_hero) + ", "
                    + getContext().getString(R.string.thor_action_move_hero) + ", "
                    + (isActionActive(ThorActionIds.TOGGLE_HERO_SLEEP)
                            ? getContext().getString(R.string.thor_action_wake_hero)
                            : getContext().getString(R.string.thor_action_sleep_hero)) + ", "
                    + getContext().getString(R.string.thor_action_end_turn) + ", "
                    + getContext().getString(R.string.thor_action_kingdom) + ", "
                    + getContext().getString(R.string.thor_action_quest_log) + ", "
                    + getContext().getString(R.string.thor_action_puzzle_map) + ", "
                    + getContext().getString(R.string.thor_action_save_game);
        }

        private boolean drawVisualAsset(final Canvas canvas, final long key, final RectF area)
        {
            final Bitmap bitmap = visualAssets.get(key);
            if (bitmap == null || bitmap.isRecycled() || !ThorVisualAssetKey.isValid(key))
                return false;
            final float scale = Math.min(area.width() / bitmap.getWidth(), area.height() / bitmap.getHeight());
            if (!(scale > 0f) || !Float.isFinite(scale))
                return false;
            final float width = bitmap.getWidth() * scale;
            final float height = bitmap.getHeight() * scale;
            final RectF destination = new RectF(area.centerX() - width * 0.5f, area.centerY() - height * 0.5f,
                    area.centerX() + width * 0.5f, area.centerY() + height * 0.5f);
            canvas.drawBitmap(bitmap, null, destination, iconPaint);
            return true;
        }

        private void drawFittedText(final Canvas canvas, final String text, final float centerX, final float baseline,
                                    final float maxWidth, final float preferredSize)
        {
            float textSize = preferredSize;
            paint.setTextSize(textSize);
            while (textSize > 12f && paint.measureText(text) > maxWidth)
            {
                textSize -= 1f;
                paint.setTextSize(textSize);
            }
            canvas.drawText(text, centerX, baseline - (paint.ascent() + paint.descent()) * 0.5f, paint);
        }

        private void drawEllipsizedText(final Canvas canvas, final String text, final float centerX,
                                       final float baseline, final float maxWidth, final float preferredSize)
        {
            float textSize = preferredSize;
            paint.setTextSize(textSize);
            while (textSize > preferredSize * 0.72f && paint.measureText(text) > maxWidth)
            {
                textSize -= 1f;
                paint.setTextSize(textSize);
            }
            String fitted = text;
            if (paint.measureText(fitted) > maxWidth)
            {
                int end = fitted.length();
                while (end > 0 && paint.measureText(fitted, 0, end) + paint.measureText("…") > maxWidth)
                    end = fitted.offsetByCodePoints(end, -1);
                fitted = fitted.substring(0, end) + "…";
            }
            canvas.drawText(fitted, centerX, baseline - (paint.ascent() + paint.descent()) * 0.5f, paint);
        }
    }
}
