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

    void updateContext(final String contextId, final String title, final String status)
    {
        if (foundationView != null)
            foundationView.updateContext(contextId, title, status);
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

        ThorFoundationView(final Context context)
        {
            super(context);
            title = context.getString(R.string.thor_deck_title);
            status = context.getString(R.string.thor_deck_status);
            setBackgroundColor(BACKGROUND);
            setClickable(false);
            setFocusable(false);
        }

        void updateContext(final String contextId, final String publishedTitle, final String publishedStatus)
        {
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
            else
            {
                title = publishedTitle.isEmpty() ? getContext().getString(R.string.thor_deck_title) : publishedTitle;
                status = publishedStatus.isEmpty() ? getContext().getString(R.string.thor_deck_status) : publishedStatus;
            }
            setContentDescription(title + ". " + status);
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
            drawFittedText(canvas, status, getWidth() * 0.5f, dividerY + (frame.bottom - dividerY) * 0.5f,
                    contentWidth * 0.78f, Math.min(30f * density, contentHeight * 0.065f));
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
