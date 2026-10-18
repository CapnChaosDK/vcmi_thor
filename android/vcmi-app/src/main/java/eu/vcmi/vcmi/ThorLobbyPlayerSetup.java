package eu.vcmi.vcmi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

/** Lower-local navigation and native-authorized player setup. */
final class ThorLobbyPlayerSetup
{
    private final ThorControlGesture gesture = new ThorControlGesture(0);
    private ThorLobbyPlayers state = ThorLobbyPlayers.EMPTY;
    private boolean players;
    private int row;
    private long pendingRevision;
    boolean showingPlayers() { return players; }
    void cancel() { gesture.cancel(); }
    void withdraw(final boolean leaving)
    {
        cancel(); state = ThorLobbyPlayers.EMPTY; pendingRevision = 0;
        if (leaving) { players = false; row = 0; }
    }
    void update(final ThorLobbyPlayers snapshot)
    {
        state = snapshot;
        row = Math.max(0, Math.min(row, state.size() - 1));
    }
    private boolean enabled(final int control, final long mask, final long revision)
    {
        if (control == 1 || control == 2) return true;
        if (control == 3) return row > 0;
        if (control == 4) return row + 1 < state.size();
        if (control == 12 || control == 13)
            return (mask & ThorActionIds.maskFor(control == 12 ? ThorActionIds.LOBBY_START_GAME : ThorActionIds.LOBBY_BACK)) != 0;
        return pendingRevision != revision && state.allows(row, ThorLobbyPlayerControls.command(control));
    }
    private RectF box(final int control, final RectF frame)
    {
        final float[] b = ThorLobbyPlayerControls.bounds(control, frame.width(), frame.height());
        return new RectF(frame.left + b[0], frame.top + b[1], frame.left + b[2], frame.top + b[3]);
    }
    private void text(final Canvas canvas, final Paint paint, final RectF frame, final float y, final String label)
    {
        paint.setStyle(Paint.Style.FILL); paint.setColor(ThorDeckDrawing.TEXT); paint.setTextAlign(Paint.Align.CENTER);
        canvas.save();
        canvas.clipRect(frame.left + frame.width() * .05f, frame.top, frame.right - frame.width() * .05f, frame.bottom);
        ThorDeckDrawing.drawFittedText(canvas, paint, label, frame.centerX(), frame.top + frame.height() * y,
                frame.width() * .85f, frame.height() * .030f);
        canvas.restore();
    }
    void draw(final Canvas canvas, final Paint paint, final Context context, final RectF frame,
              final float density, final long mask, final long revision)
    {
        if (players)
        {
            paint.setStyle(Paint.Style.FILL); paint.setColor(ThorDeckDrawing.STONE_LIGHT);
            canvas.drawRect(frame, paint);
        }
        final int[] labels = {0, R.string.thor_setup_scenario, R.string.thor_setup_players,
                R.string.thor_previous, R.string.thor_next, R.string.thor_setup_assign,
                0, 0, 0, 0, 0, 0, R.string.thor_lobby_start, R.string.thor_lobby_back};
        for (int c = 1; c <= (players ? 13 : 2); ++c)
        {
            final String label = c >= 6 && c <= 11 ? (c % 2 == 0 ? "-" : "+") : context.getString(labels[c]);
            ThorDeckDrawing.drawChoiceButton(canvas, paint, box(c, frame), label, enabled(c, mask, revision),
                    c == (players ? 2 : 1) || c == 5 && (state.flags(row) & 32) != 0, density);
        }
        if (!players) return;
        text(canvas, paint, frame, .285f, (state.size() == 0 ? "0" : Integer.toString(row + 1)) + "/" + state.size() + "  " + state.label(row, 0));
        text(canvas, paint, frame, .335f, state.size() == 0 ? context.getString(R.string.thor_setup_unavailable) : state.label(row, 1) + " / " + context.getString(
                (state.flags(row) & 16) != 0 ? R.string.thor_setup_human : R.string.thor_setup_ai)
                + ((state.flags(row) & 32) != 0 ? " / " + context.getString(R.string.thor_setup_local) : ""));
        final int[] names = {R.string.thor_setup_faction, R.string.thor_setup_hero, R.string.thor_setup_bonus};
        for (int i = 0; i < 3; ++i)
        {
            final RectF region = new RectF(frame.left + frame.width() * .23f, frame.top + frame.height() * (.50f + i * .125f),
                    frame.left + frame.width() * .77f, frame.top + frame.height() * (.60f + i * .125f));
            paint.setStyle(Paint.Style.FILL); paint.setColor(ThorDeckDrawing.TEXT); paint.setTextAlign(Paint.Align.CENTER);
            canvas.save();
            canvas.clipRect(region);
            ThorDeckDrawing.drawFittedText(canvas, paint, context.getString(names[i]), region.centerX(), region.top + region.height() * .2f,
                    region.width(), region.height() * .25f);
            ThorDeckDrawing.drawFittedText(canvas, paint, state.label(row, i + 2), region.centerX(), region.top + region.height() * .7f,
                    region.width(), region.height() * .32f);
            canvas.restore();
        }
        text(canvas, paint, frame, .865f, context.getString((state.flags(row) & 64) != 0 || pendingRevision == revision
                ? R.string.thor_setup_pending : (state.flags(row) & 15) == 0
                ? R.string.thor_setup_readonly : R.string.thor_setup_locked));
    }
    boolean touch(final MotionEvent event, final RectF frame, final long revision, final long session,
                  final long mask, final boolean sessionIsCurrent)
    {
        final int control = ThorLobbyPlayerControls.at(event.getX() - frame.left, event.getY() - frame.top,
                frame.width(), frame.height(), players);
        if (!players && control == 0 && !gesture.isActive()) return false;
        final int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN)
            gesture.begin(ThorContextIds.LOBBY_NEW_GAME_SCENARIO, control, revision, session, event.getPointerId(0),
                    event.getPointerCount() == 1 && enabled(control, mask, revision));
        else if (event.getPointerCount() != 1 || action == MotionEvent.ACTION_CANCEL
                || action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_POINTER_UP)
            cancel();
        else if (action == MotionEvent.ACTION_UP && gesture.finish(ThorContextIds.LOBBY_NEW_GAME_SCENARIO,
                control, 1, event.getPointerId(0), revision, session, enabled(control, mask, revision), sessionIsCurrent))
        {
            if (control <= 2) players = control == 2;
            else if (control <= 4) row += control == 3 ? -1 : 1;
            else if (control >= 12)
                NativeMethods.submitThorAction(revision, control == 12 ? ThorActionIds.LOBBY_START_GAME : ThorActionIds.LOBBY_BACK, -1);
            else if (NativeMethods.submitThorCommand(revision, ThorLobbyPlayerControls.command(control), state.slot(row)))
                pendingRevision = revision;
        }
        return true;
    }
    String description(final Context context)
    {
        return context.getString(R.string.thor_setup_players) + ". " + state.label(row, 0) + ". "
                + state.label(row, 1) + ". " + state.label(row, 2) + ". " + state.label(row, 3) + ". " + state.label(row, 4);
    }
}
