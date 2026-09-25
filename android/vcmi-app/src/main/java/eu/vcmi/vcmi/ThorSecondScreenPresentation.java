package eu.vcmi.vcmi;

import android.app.Presentation;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;

final class ThorSecondScreenPresentation extends Presentation
{
    // Measured AYN Thor lower panel reference: 1080 x 1240 px at approximately 369 dpi.
    // The shell scales from these proportions and remains safe on other presentation displays.
    private static final float REFERENCE_WIDTH = 1080f;
    private static final float REFERENCE_HEIGHT = 1240f;
    private ThorFoundationView foundationView;

    ThorSecondScreenPresentation(final Context context, final Display display)
    {
        super(context, display);
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

        foundationView = new ThorFoundationView(getContext());
        foundationView.setContentDescription(getContext().getString(R.string.thor_deck_title));
        setContentView(foundationView);
    }

    void updateContext(final long revision, final String contextId, final String title, final String status,
                       final String[] detailLines, final int enabledActionMask, final int activeActionMask)
    {
        if (foundationView != null)
            foundationView.updateContext(revision, contextId, title, status, detailLines,
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

    void updateHeroMeetingArmies(final ThorHeroMeetingArmies armies)
    {
        if (foundationView != null)
            foundationView.updateHeroMeetingArmies(armies);
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

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private String title;
        private String status;
        private long revision;
        private String contextId = ThorContextIds.UNKNOWN;
        private final String[] detailLines = new String[ThorContextDetails.COUNT];
        private int enabledActionMask;
        private int activeActionMask;
        private ThorHeroRoster heroes = ThorHeroRoster.EMPTY;
        private ThorTownRoster towns = ThorTownRoster.EMPTY;
        private ThorHeroMeetingArmies heroMeetingArmies = ThorHeroMeetingArmies.EMPTY;
        private int adventureTab;
        private int townPage;
        private int selectedMeetingSlot = -1;
        private boolean pendingMeetingAction;
        private final ThorHeroMeetingGesture heroMeetingGesture = new ThorHeroMeetingGesture();
        private final ThorHeroMeetingSplitState heroMeetingSplit = new ThorHeroMeetingSplitState();
        private final int touchSlop;
        private boolean heroMeetingTouchSequence;
        private boolean heroMeetingTouchCancelled;
        private long heroMeetingTouchRevision;
        private final Runnable heroMeetingLongPress = () ->
        {
            if (heroMeetingGesture.activateLongPress(revision, ThorContextIds.HERO_MEETING.equals(contextId)))
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

        ThorFoundationView(final Context context)
        {
            super(context);
            title = context.getString(R.string.thor_deck_title);
            status = context.getString(R.string.thor_deck_status);
            touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
            setBackgroundColor(BACKGROUND);
            setClickable(true);
            setFocusable(false);
        }

        void updateContext(final long revision, final String contextId, final String publishedTitle,
                           final String publishedStatus, final String[] publishedDetails,
                           final int enabledActionMask, final int activeActionMask)
        {
            final boolean retainMeetingArmies = ThorHeroMeetingGesture.retainsArmies(
                    this.revision, this.contextId, revision, contextId);
            if (revision != this.revision || !contextId.equals(this.contextId))
            {
                heroes = ThorHeroRoster.EMPTY;
                towns = ThorTownRoster.EMPTY;
                heroMeetingArmies = ThorHeroMeetingArmies.EMPTY;
                townPage = 0;
                selectedMeetingSlot = -1;
                pendingMeetingAction = false;
                heroMeetingSplit.cancel();
                cancelHeroMeetingGesture();
            }
            this.revision = revision;
            this.contextId = contextId;
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
                title = getContext().getString(R.string.thor_context_new_game);
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
                title = getContext().getString(R.string.thor_context_load_game);
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
            cancelHeroMeetingGesture();
            heroMeetingArmies = ThorContextIds.HERO_MEETING.equals(contextId) && armies.complete()
                    ? armies : ThorHeroMeetingArmies.EMPTY;
            selectedMeetingSlot = -1;
            pendingMeetingAction = false;
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
            final float dividerY = frame.top + contentHeight * (battleDashboard ? 0.55f
                    : adventure ? ThorAdventureLayout.DIVIDER : 0.34f);
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
            else if (ThorContextIds.HERO_WINDOW.equals(contextId))
            {
                drawHeroDashboard(canvas, frame, dividerY, bevel, density);
            }
            else if (ThorContextIds.TOWN_WINDOW.equals(contextId))
            {
                drawTownDashboard(canvas, frame, dividerY, bevel, density);
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
                drawFittedText(canvas, title, getWidth() * 0.5f,
                        frame.top + contentHeight * (adventure ? ThorAdventureLayout.TITLE : 0.18f), contentWidth * 0.82f,
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
        }

        @Override
        public boolean onTouchEvent(final android.view.MotionEvent event)
        {
            final int action = event.getActionMasked();
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
                if (action == MotionEvent.ACTION_MOVE)
                {
                    if (!heroMeetingTouchCancelled)
                        updateHeroMeetingGesture(event.getX(), event.getY());
                    return true;
                }
                if (action == MotionEvent.ACTION_UP)
                {
                    if (!heroMeetingTouchCancelled)
                        finishHeroMeetingGesture(event.getX(), event.getY());
                    else
                        clearHeroMeetingSelection();
                    heroMeetingTouchSequence = false;
                    heroMeetingTouchCancelled = false;
                    return true;
                }
                if (action == MotionEvent.ACTION_CANCEL)
                {
                    cancelHeroMeetingGesture();
                    heroMeetingTouchSequence = false;
                    heroMeetingTouchCancelled = false;
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
            if (!heroMeetingArmies.complete())
                return;

            final String[] names = heroMeetingArmies.heroNames;
            for (int side = 0; side < 2; ++side)
            {
                final RectF heading = heroMeetingHeadingBounds(side, frame, bevel);
                paint.setColor(PARCHMENT_DARK);
                paint.setFakeBoldText(true);
                drawEllipsizedText(canvas, names[side], heading.centerX(), heading.centerY(), heading.width() * 0.9f,
                        Math.min(26f * density, heading.height() * 0.6f));
                paint.setFakeBoldText(false);
                for (int slot = 0; slot < 7; ++slot)
                {
                    final int index = side * 7 + slot;
                    final RectF row = heroMeetingSlotBounds(index, frame, bevel);
                    final boolean occupied = (heroMeetingArmies.flags[index] & 1) != 0;
                    final boolean selected = selectedMeetingSlot == index;
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(selected ? PARCHMENT_DARK : STONE_DARK);
                    canvas.drawRoundRect(row, bevel, bevel, paint);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(selected ? bevel * 0.8f : bevel * 0.3f);
                    paint.setColor(selected ? GOLD : STONE_LIGHT);
                    canvas.drawRoundRect(row, bevel, bevel, paint);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(selected ? TEXT : (occupied ? TEXT : PARCHMENT_DARK));
                    final String value = occupied ? heroMeetingArmies.creatureNames[index] + " × " + heroMeetingArmies.counts[index]
                            : getContext().getString(R.string.thor_army_empty);
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
            if (heroMeetingSplit.stage() == ThorHeroMeetingSplitState.Stage.AMOUNT)
            {
                drawHeroMeetingSplitEditor(canvas, frame, bevel, density);
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

        private RectF heroMeetingHeadingBounds(final int side, final RectF frame, final float bevel)
        {
            final float gap = Math.max(bevel, 8f);
            final float width = (frame.width() - bevel * 6f - gap) * 0.5f;
            final float left = frame.left + bevel * 3f + side * (width + gap);
            final float top = frame.top + frame.height() * 0.16f;
            return new RectF(left, top, left + width, top + frame.height() * 0.05f);
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

        private void beginHeroMeetingGesture(final float x, final float y)
        {
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
            removeCallbacks(heroMeetingLongPress);
            heroMeetingGesture.cancel();
            heroMeetingSplit.cancel();
            selectedMeetingSlot = -1;
            if (heroMeetingTouchSequence)
                heroMeetingTouchCancelled = true;
            invalidate();
        }

        private void clearHeroMeetingSelection()
        {
            removeCallbacks(heroMeetingLongPress);
            heroMeetingGesture.cancel();
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
            heroMeetingGesture.cancel();
            heroMeetingSplit.cancel();
            selectedMeetingSlot = -1;
            heroMeetingTouchSequence = false;
            heroMeetingTouchCancelled = false;
            super.onDetachedFromWindow();
        }

        @Override
        protected void onWindowVisibilityChanged(final int visibility)
        {
            super.onWindowVisibilityChanged(visibility);
            if (visibility != View.VISIBLE)
            {
                cancelHeroMeetingGesture();
            }
        }

        private void handleHeroMeetingTap(final float x, final float y)
        {
            if (!heroMeetingArmies.complete() || pendingMeetingAction)
                return;
            final RectF frame = adventureFrame();
            final float bevel = adventureBevel();
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
                return;
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
            drawFittedText(canvas, title, frame.centerX(), frame.top + contentHeight * 0.20f,
                    frame.width() * 0.82f, Math.min(42f * density, contentHeight * 0.075f));

            paint.setFakeBoldText(false);
            paint.setColor(TEXT);
            drawFittedText(canvas, status, frame.centerX(), frame.top + contentHeight * 0.29f,
                    frame.width() * 0.8f, Math.min(28f * density, contentHeight * 0.052f));

            paint.setColor(PARCHMENT_DARK);
            final float detailsTop = dividerY + bevel * 3f;
            final float detailsHeight = frame.bottom - bevel * 3f - detailsTop;
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
            final float availableHeight = frame.bottom - bevel * 3f - top;
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

        private String townHeroName(final String heroName)
        {
            return heroName.isEmpty() ? getContext().getString(R.string.thor_town_none) : heroName;
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
            return (enabledActionMask & ThorActionIds.maskFor(actionId)) != 0;
        }

        private boolean isActionActive(final int actionId)
        {
            return (activeActionMask & ThorActionIds.maskFor(actionId)) != 0;
        }

        private String commandDeckDescription()
        {
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
                if (heroMeetingSplit.stage() == ThorHeroMeetingSplitState.Stage.DESTINATION)
                    return title + ". " + getContext().getString(R.string.thor_split_select_destination);
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
