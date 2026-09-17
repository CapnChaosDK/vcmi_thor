package eu.vcmi.vcmi;

import android.app.Presentation;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
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
                       final int enabledActionMask)
    {
        if (foundationView != null)
            foundationView.updateContext(revision, contextId, title, status, enabledActionMask);
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
        private int enabledActionMask;

        ThorFoundationView(final Context context)
        {
            super(context);
            title = context.getString(R.string.thor_deck_title);
            status = context.getString(R.string.thor_deck_status);
            setBackgroundColor(BACKGROUND);
            setClickable(true);
            setFocusable(false);
        }

        void updateContext(final long revision, final String contextId, final String publishedTitle,
                           final String publishedStatus, final int enabledActionMask)
        {
            this.revision = revision;
            this.contextId = contextId;
            this.enabledActionMask = enabledActionMask;
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
                status = getContext().getString(R.string.thor_context_adventure_map_status);
            }
            else if (ThorContextIds.HERO_WINDOW.equals(contextId))
            {
                title = getContext().getString(R.string.thor_context_hero);
                status = getContext().getString(R.string.thor_context_hero_status);
            }
            else if (ThorContextIds.TOWN_WINDOW.equals(contextId))
            {
                title = getContext().getString(R.string.thor_context_town);
                status = getContext().getString(R.string.thor_context_town_status);
            }
            else if (ThorContextIds.HERO_MEETING.equals(contextId))
            {
                title = getContext().getString(R.string.thor_context_hero_meeting);
                status = getContext().getString(R.string.thor_context_hero_meeting_status);
            }
            else if (ThorContextIds.BATTLE.equals(contextId))
            {
                title = getContext().getString(R.string.thor_context_battle);
                status = getContext().getString(R.string.thor_context_battle_status);
            }
            else if (ThorContextIds.BATTLE_TACTICS.equals(contextId))
            {
                title = getContext().getString(R.string.thor_context_battle_tactics);
                status = getContext().getString(R.string.thor_context_battle_tactics_status);
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

            final float dividerY = frame.top + contentHeight * 0.34f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(PARCHMENT_DARK);
            canvas.drawRect(frame.left + bevel * 2f, frame.top + bevel * 2f, frame.right - bevel * 2f, dividerY - bevel, paint);
            paint.setColor(PARCHMENT);
            canvas.drawRect(frame.left + bevel * 2f, dividerY + bevel, frame.right - bevel * 2f, frame.bottom - bevel * 2f, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(2f, bevel * 0.55f));
            paint.setColor(GOLD);
            canvas.drawLine(frame.left + bevel * 2f, dividerY, frame.right - bevel * 2f, dividerY, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(true);
            paint.setColor(TEXT);
            drawFittedText(canvas, title, getWidth() * 0.5f, frame.top + contentHeight * 0.18f, contentWidth * 0.82f,
                    Math.min(42f * density, contentHeight * 0.09f));

            paint.setFakeBoldText(false);
            paint.setColor(PARCHMENT_DARK);
            drawFittedText(canvas, status, getWidth() * 0.5f, dividerY + contentHeight * 0.075f,
                    contentWidth * 0.78f, Math.min(30f * density, contentHeight * 0.055f));

            if (ThorContextIds.ADVENTURE_MAP.equals(contextId))
                drawAdventureActions(canvas, frame, dividerY, bevel, density);
        }

        @Override
        public boolean onTouchEvent(final android.view.MotionEvent event)
        {
            if (!ThorContextIds.ADVENTURE_MAP.equals(contextId))
                return false;

            if (event.getAction() == android.view.MotionEvent.ACTION_UP)
            {
                final int actionId = actionAt(event.getX(), event.getY());
                if (actionId != ThorActionIds.NONE && isActionEnabled(actionId))
                {
                    performClick();
                    NativeMethods.submitThorAction(revision, actionId);
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

        private void drawAdventureActions(final Canvas canvas, final RectF frame, final float dividerY,
                                          final float bevel, final float density)
        {
            final int[] actions = {
                    ThorActionIds.OPEN_KINGDOM_OVERVIEW,
                    ThorActionIds.OPEN_QUEST_LOG,
                    ThorActionIds.OPEN_PUZZLE_MAP,
                    ThorActionIds.OPEN_SAVE_GAME
            };
            final String[] labels = {
                    getContext().getString(R.string.thor_action_kingdom),
                    getContext().getString(R.string.thor_action_quest_log),
                    getContext().getString(R.string.thor_action_puzzle_map),
                    getContext().getString(R.string.thor_action_save_game)
            };

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

        private RectF actionBounds(final int index, final RectF frame, final float dividerY, final float bevel)
        {
            final float gap = Math.max(bevel * 1.25f, 10f);
            final float left = frame.left + bevel * 3f;
            final float right = frame.right - bevel * 3f;
            final float top = dividerY + (frame.bottom - dividerY) * 0.19f;
            final float bottom = frame.bottom - bevel * 3f;
            final float columnWidth = (right - left - gap) / 2f;
            final float rowHeight = (bottom - top - gap) / 2f;
            final int column = index % 2;
            final int row = index / 2;
            return new RectF(left + column * (columnWidth + gap), top + row * (rowHeight + gap),
                    left + column * (columnWidth + gap) + columnWidth, top + row * (rowHeight + gap) + rowHeight);
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
            final float dividerY = frame.top + (getHeight() - margin * 2f) * 0.34f;
            final int[] actions = {
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

        private boolean isActionEnabled(final int actionId)
        {
            return (enabledActionMask & ThorActionIds.maskFor(actionId)) != 0;
        }

        private String commandDeckDescription()
        {
            if (!ThorContextIds.ADVENTURE_MAP.equals(contextId))
                return title + ". " + status;

            return title + ". " + status + ". "
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
    }
}
