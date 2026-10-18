package eu.vcmi.vcmi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

/** Battle quick-control drawing and one-shot gestures, using shared shell utilities. */
final class ThorBattleDeck
{
    private final ThorControlGesture gesture = new ThorControlGesture(0);
    private ThorExtendedCommands commands = ThorExtendedCommands.EMPTY;
    void cancel() { gesture.cancel(); }
    void reset() { cancel(); commands = ThorExtendedCommands.EMPTY; }
    void update(final ThorExtendedCommands state) { cancel(); commands = state; }

    private int label(final String context, final int control)
    {
        if (ThorContextIds.BATTLE_TACTICS.equals(context))
            return control == 1 ? R.string.thor_action_next_unit : R.string.thor_action_start_battle;
        switch (control)
        {
            case 1: return R.string.thor_action_wait;
            case 2: return R.string.thor_action_defend;
            case 3: return commands.active(ThorExtendedCommands.AUTO_START)
                    ? R.string.thor_action_auto_stop : R.string.thor_action_auto_start;
            case 4: return R.string.thor_action_cast_spell;
            case 5: return R.string.thor_action_retreat;
            default: return R.string.thor_action_surrender;
        }
    }

    void draw(final Canvas canvas, final Paint paint, final Context context, final String id,
              final long mask, final ThorPresentationLayout layout, final float density)
    {
        for (int control = 1; control <= ThorBattleControls.count(id); ++control)
        {
            final float[] b = ThorBattleControls.bounds(control, id, layout);
            if (b == null) continue;
            ThorDeckDrawing.drawChoiceButton(canvas, paint, new RectF(b[0], b[1], b[2], b[3]),
                    context.getString(label(id, control)), ThorBattleControls.enabled(id, control, mask, commands),
                    control == 3 && commands.active(ThorExtendedCommands.AUTO_START), density);
        }
    }

    boolean touch(final MotionEvent event, final String id, final long revision, final long session,
                  final long mask, final ThorPresentationLayout layout, final boolean sessionIsCurrent)
    {
        final int action = event.getActionMasked();
        final int control = ThorBattleControls.controlAt(event.getX(), event.getY(), id, layout);
        if (action == MotionEvent.ACTION_DOWN)
        {
            gesture.begin(id, control, revision, session, event.getPointerId(0),
                    ThorBattleControls.enabled(id, control, mask, commands) && event.getPointerCount() == 1);
            return false;
        }
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_POINTER_DOWN
                || action == MotionEvent.ACTION_POINTER_UP || event.getPointerCount() != 1)
        {
            cancel();
            return false;
        }
        if (action != MotionEvent.ACTION_UP) return false;
        final int pressed = gesture.control();
        if (!gesture.finish(id, control, event.getPointerCount(), event.getPointerId(0), revision, session,
                ThorBattleControls.enabled(id, control, mask, commands), sessionIsCurrent)) return false;
        final int legacy = ThorBattleControls.legacy(id, pressed);
        if (legacy != ThorActionIds.NONE)
            NativeMethods.submitThorAction(revision, legacy, ThorActionIds.NO_TARGET);
        else
            NativeMethods.submitThorCommand(revision, ThorBattleControls.command(pressed, commands));
        return true;
    }

    String description(final Context context, final String id)
    {
        final StringBuilder result = new StringBuilder();
        for (int control = 1; control <= ThorBattleControls.count(id); ++control)
            result.append(". ").append(context.getString(label(id, control)));
        return result.toString();
    }
}
