package eu.vcmi.vcmi;

import static eu.vcmi.vcmi.ThorDeckDrawing.*;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

/** Main-menu family presentation. Snapshots, lifecycle and native submission stay with the view. */
final class ThorMainMenuDeck
{
    private final ThorMainMenuGesture gesture = new ThorMainMenuGesture();

    void draw(final Canvas canvas, final Paint paint, final Context context, final String contextId,
              final String title, final String status, final long enabledActionMask, final long activeActionMask,
              final RectF frame, final float dividerY, final float density)
    {
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setColor(TEXT);
        ThorDeckDrawing.drawFittedText(canvas, paint, title, frame.centerX(), frame.top + frame.height() * 0.16f,
                frame.width() * 0.78f, Math.min(42f * density, frame.height() * 0.09f));
        paint.setFakeBoldText(false);
        paint.setColor(PARCHMENT_DARK);
        ThorDeckDrawing.drawFittedText(canvas, paint, status, frame.centerX(), frame.top + frame.height() * 0.27f,
                frame.width() * 0.78f, Math.min(27f * density, frame.height() * 0.055f));

        for (int control = 1; control <= ThorMainMenuState.choiceCount(contextId); ++control)
        {
            final int actionId = ThorMainMenuState.actionForControl(contextId, control);
            final float[] bounds = ThorMainMenuState.boundsForControl(contextId, control,
                    frame.width(), frame.height(), dividerY - frame.top);
            final RectF button = new RectF(frame.left + bounds[0], frame.top + bounds[1],
                    frame.left + bounds[2], frame.top + bounds[3]);
            ThorDeckDrawing.drawChoiceButton(canvas, paint, button,
                    context.getString(ThorMainMenuState.labelForControl(contextId, control)),
                    ThorLobbyScenarioState.isActionEnabled(enabledActionMask, actionId), ThorContextIds.HIGH_SCORES.equals(contextId)
                            && (activeActionMask & ThorActionIds.maskFor(actionId)) != 0, density);
        }
    }

    int handleTouch(final int action, final float x, final float y, final int pointerCount,
                    final int pointerId, final String contextId, final long revision,
                    final long presentationSessionId, final long enabledActionMask,
                    final ThorPresentationLayout layout, final boolean sessionIsCurrent)
    {
        if (action == MotionEvent.ACTION_DOWN)
        {
            final int control = controlAt(contextId, x, y, layout);
            final int actionId = ThorMainMenuState.actionForControl(contextId, control);
            gesture.begin(contextId, control, revision, presentationSessionId,
                    pointerId, actionId != ThorActionIds.NONE
                            && ThorLobbyScenarioState.isActionEnabled(enabledActionMask, actionId));
            return ThorActionIds.NONE;
        }
        if (!gesture.isActive())
            return ThorActionIds.NONE;
        if (pointerCount != 1 || pointerId != gesture.pointerId()
                || action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_POINTER_UP)
        {
            cancel();
            return ThorActionIds.NONE;
        }
        if (action == MotionEvent.ACTION_MOVE)
            return ThorActionIds.NONE;
        if (action == MotionEvent.ACTION_CANCEL)
        {
            cancel();
            return ThorActionIds.NONE;
        }
        if (action != MotionEvent.ACTION_UP)
            return ThorActionIds.NONE;

        final int control = gesture.control();
        final int releasedControl = controlAt(contextId, x, y, layout);
        final int actionId = ThorMainMenuState.actionForControl(contextId, control);
        final boolean enabledAtUp = actionId != ThorActionIds.NONE
                && ThorLobbyScenarioState.isActionEnabled(enabledActionMask, actionId);
        if (gesture.finish(contextId, releasedControl, pointerCount, pointerId,
                revision, presentationSessionId, enabledAtUp, sessionIsCurrent))
            return actionId;
        return ThorActionIds.NONE;
    }

    int controlAt(final String contextId, final float x, final float y, final ThorPresentationLayout layout)
    {
        return ThorMainMenuState.controlAt(contextId, x - layout.margin, y - layout.margin,
                layout.right - layout.margin, layout.bottom - layout.margin,
                (layout.bottom - layout.margin) * 0.34f);
    }

    void cancel()
    {
        gesture.cancel();
    }

    String description(final Context context, final String contextId, final String title,
                       final String status, final long enabledActionMask)
    {
        final StringBuilder description = new StringBuilder(title).append(". ").append(status);
        for (int control = 1; control <= ThorMainMenuState.choiceCount(contextId); ++control)
        {
            final int actionId = ThorMainMenuState.actionForControl(contextId, control);
            if (ThorLobbyScenarioState.isActionEnabled(enabledActionMask, actionId))
                description.append(". ").append(context.getString(ThorMainMenuState.labelForControl(contextId, control)));
        }
        return description.toString();
    }
}
