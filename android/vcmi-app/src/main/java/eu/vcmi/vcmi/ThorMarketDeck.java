package eu.vcmi.vcmi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

/** Resource trading presentation; native state owns the quote and all operations. */
final class ThorMarketDeck
{
    private final ThorMarketGesture gesture = new ThorMarketGesture();
    private ThorMarketState state = ThorMarketState.EMPTY;
    private boolean submitted;
    private int page;
    void cancel() { gesture.cancel(); }
    void withdraw() { cancel(); state = ThorMarketState.EMPTY; submitted = false; }
    void reset() { cancel(); state = ThorMarketState.EMPTY; submitted = false; page = 0; }
    void update(final ThorMarketState state)
    { cancel(); submitted = false; this.state = state; page = Math.min(page, Math.max(0, (state.count() - 1) / ThorMarketControls.PAGE_SIZE)); }

    private boolean enabled(final int control)
    {
        return ThorMarketControls.enabled(control, state, page);
    }
    private String label(final Context context, final int control)
    {
        if (control < ThorMarketControls.LESS_TEN)
        {
            final int row = control < ThorMarketControls.BUY_FIRST ? control - 1 : control - ThorMarketControls.BUY_FIRST;
            return state.name(row) + "  " + state.balance(row);
        }
        if (control == ThorMarketControls.PREVIOUS) return "<";
        if (control == ThorMarketControls.NEXT) return ">";
        switch (control)
        {
            case ThorMarketControls.LESS_TEN: return "−10";
            case ThorMarketControls.LESS: return "−1";
            case ThorMarketControls.MORE: return "+1";
            case ThorMarketControls.MORE_TEN: return "+10";
            case ThorMarketControls.MAX: return context.getString(R.string.thor_market_max);
            case ThorMarketControls.TRADE: return context.getString(R.string.thor_market_trade);
            case ThorMarketControls.RESET: return context.getString(R.string.thor_market_reset);
            default: return context.getString(R.string.thor_market_close);
        }
    }
    private String resourceName(final int id)
    {
        for (int row = 0; row < state.count(); ++row) if (state.id(row) == id) return state.name(row);
        return "";
    }
    private void text(final Canvas canvas, final Paint paint, final String text,
                      final float x, final float y, final float width, final float height)
    {
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(ThorDeckDrawing.TEXT);
        // Bound long mod/localized labels even at the minimum font size.
        canvas.save();
        canvas.clipRect(x - width / 2, y - height, x + width / 2, y + height);
        ThorDeckDrawing.drawFittedText(canvas, paint, text, x, y, width, height);
        canvas.restore();
    }
    void draw(final Canvas canvas, final Paint paint, final Context context,
              final String title, final ThorPresentationLayout layout, final float density)
    {
        final float left = layout.margin + layout.bevel * 3, width = layout.right - layout.bevel * 3 - left;
        final float top = layout.margin, h = layout.contentHeight;
        text(canvas, paint, title + " " + (page + 1) + "/" + Math.max(1, (state.count() + 3) / 4), left + width * .44f, top + h * .055f, width * .8f, h * .03f);
        text(canvas, paint, context.getString(R.string.thor_market_give) + ": " + resourceName(state.selling()), left + width * .24f, top + h * .13f, width * .45f, h * .025f);
        text(canvas, paint, context.getString(R.string.thor_market_get) + ": " + resourceName(state.buying()), left + width * .76f, top + h * .13f, width * .45f, h * .025f);
        for (int control = 1; control <= ThorMarketControls.NEXT; ++control)
        {
            final float[] b = ThorMarketControls.bounds(control, layout, state.count(), page);
            if (b == null) continue;
            final int target = ThorMarketControls.target(control, state);
            final boolean selected = control < ThorMarketControls.BUY_FIRST ? target == state.selling()
                    : control < ThorMarketControls.LESS_TEN && target == state.buying();
            canvas.save();
            canvas.clipRect(b[0], b[1], b[2], b[3]);
            ThorDeckDrawing.drawChoiceButton(canvas, paint, new RectF(b[0], b[1], b[2], b[3]), label(context, control),
                    !submitted && enabled(control), selected, density);
            canvas.restore();
        }
        final boolean quoted = state.bid() > 0 && state.offer() > 0;
        text(canvas, paint, quoted ? context.getString(R.string.thor_market_rate, state.bid(), state.offer())
                : context.getString(R.string.thor_market_select),
                left + width / 2, top + h * .65f, width * .59f, h * .025f);
        if (quoted) text(canvas, paint, context.getString(R.string.thor_market_totals,
                (long) state.bid() * state.amount(), (long) state.offer() * state.amount()),
                left + width / 2, top + h * .687f, width * .59f, h * .025f);
        final int status = state.status() == 1 ? R.string.thor_market_pending
                : state.status() == 2 ? R.string.thor_market_rejected
                : state.status() == 3 ? R.string.thor_market_completed : R.string.thor_market_quantity;
        text(canvas, paint, context.getString(status, state.amount(), state.maximum()),
                left + width / 2, top + h * .725f, width * .59f, h * .022f);
    }
    boolean touch(final MotionEvent event, final long revision, final long session,
                  final ThorPresentationLayout layout, final boolean sessionIsCurrent)
    {
        final int control = ThorMarketControls.at(event.getX(), event.getY(), layout, state.count(), page);
        final int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN)
        {
            gesture.begin(control, revision, session, event.getPointerId(0),
                    !submitted && state.revision == revision && enabled(control)
                            && event.getPointerCount() == 1, event.getX(), event.getY(), event.getEventTime());
            return false;
        }
        final float slop = Math.max(8, layout.bevel * 2);
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_POINTER_DOWN
                || action == MotionEvent.ACTION_POINTER_UP || event.getPointerCount() != 1)
        { cancel(); return false; }
        gesture.move(event.getX(), event.getY(), event.getEventTime(), slop);
        if (action != MotionEvent.ACTION_UP) return false;
        if (!gesture.finish(control, revision, session, event.getPointerCount(), event.getPointerId(0), !submitted && state.revision == revision && enabled(control),
                sessionIsCurrent, event.getX(), event.getY(), event.getEventTime(), slop)) return false;
        if (control == ThorMarketControls.PREVIOUS || control == ThorMarketControls.NEXT)
        { page += control == ThorMarketControls.PREVIOUS ? -1 : 1; return true; }
        submitted = NativeMethods.submitThorCommand(revision, ThorMarketControls.command(control), ThorMarketControls.target(control, state));
        return submitted;
    }
    String description(final Context context)
    {
        return context.getString(R.string.thor_market_give) + ". " + context.getString(R.string.thor_market_get)
                + ". " + context.getString(R.string.thor_market_quantity, state.amount(), state.maximum());
    }
}
