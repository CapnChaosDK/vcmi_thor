package eu.vcmi.vcmi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

/** Tavern presentation state is local; native owns offers, invitations and recruitment. */
final class ThorTavernDeck
{
    private ThorTavernState state = ThorTavernState.EMPTY;
    private final ThorControlGesture gesture = new ThorControlGesture(0);
    private int tab = ThorTavernControls.OFFERS, page;
    private boolean submitted;
    private float downX, downY;
    private long downTime;

    void cancel() { gesture.cancel(); }
    void withdraw() { cancel(); state = ThorTavernState.EMPTY; submitted = false; }
    void reset() { withdraw(); tab = ThorTavernControls.OFFERS; page = 0; }
    void update(ThorTavernState state)
    {
        cancel(); submitted = false; this.state = state;
        page = Math.min(page, ThorTavernControls.pages(state, tab) - 1);
    }
    private boolean enabled(int control)
    { return !submitted && ThorTavernControls.enabled(state, tab, page, control); }
    private static int label(int control)
    {
        switch (control)
        {
            case ThorTavernControls.OFFERS: return R.string.thor_tavern_offers;
            case ThorTavernControls.DETAILS: return R.string.thor_tavern_details;
            case ThorTavernControls.INVITATIONS: return R.string.thor_tavern_invite;
            case ThorTavernControls.RUMOR: return R.string.thor_tavern_rumor;
            case ThorTavernControls.PREVIOUS: return R.string.thor_previous;
            case ThorTavernControls.NEXT: return R.string.thor_next;
            case ThorTavernControls.RECRUIT: return R.string.thor_tavern_recruit;
            case ThorTavernControls.GUILD: return R.string.thor_tavern_guild;
            default: return R.string.thor_market_close;
        }
    }
    private static int reason(int reason)
    {
        final int[] labels = {R.string.thor_tavern_ready, R.string.thor_tavern_not_turn,
                R.string.thor_tavern_gold, R.string.thor_tavern_cap, R.string.thor_tavern_occupied,
                R.string.thor_tavern_no_heroes, R.string.thor_tavern_unavailable};
        return labels[reason];
    }
    private void text(Canvas canvas, Paint paint, String value, float x, float y, float width, float size)
    {
        paint.setStyle(Paint.Style.FILL); paint.setColor(ThorDeckDrawing.TEXT); paint.setTextAlign(Paint.Align.CENTER);
        canvas.save(); canvas.clipRect(x - width / 2, y - size, x + width / 2, y + size);
        ThorDeckDrawing.drawFittedText(canvas, paint, value, x, y, width, size);
        canvas.restore();
    }
    private void paragraph(Canvas canvas, Paint paint, String value, RectF area, float size)
    {
        paint.setTextSize(size); paint.setColor(ThorDeckDrawing.TEXT); paint.setTextAlign(Paint.Align.LEFT);
        canvas.save(); canvas.clipRect(area);
        float y = area.top + size;
        while (!value.isEmpty() && y <= area.bottom)
        {
            int length = paint.breakText(value, true, area.width(), null);
            if (length <= 0) break;
            if (length < value.length() && Character.isHighSurrogate(value.charAt(length - 1))) --length;
            if (length <= 0) break;
            int newline = value.indexOf('\n');
            if (newline >= 0 && newline < length) length = newline + 1;
            canvas.drawText(value.substring(0, length).trim(), area.left, y, paint);
            value = value.substring(length); y += size * 1.4f;
        }
        canvas.restore();
    }
    void draw(Canvas canvas, Paint paint, Context context, String title, ThorPresentationLayout layout, float density)
    {
        float left = layout.margin + layout.bevel * 3, width = layout.right - layout.bevel * 3 - left;
        float top = layout.margin, h = layout.contentHeight;
        text(canvas, paint, title, left + width / 2, top + h * .05f, width, h * .035f);
        text(canvas, paint, context.getString(R.string.thor_tavern_cost, state.cost(), state.gold()),
                left + width / 2, top + h * .10f, width, h * .025f);
        text(canvas, paint, context.getString(reason(state.reason())), left + width / 2, top + h * .145f, width, h * .021f);
        for (int control = ThorTavernControls.OFFERS; control <= ThorTavernControls.CLOSE; ++control)
        {
            float[] b = ThorTavernControls.bounds(control, layout);
            ThorDeckDrawing.drawChoiceButton(canvas, paint, new RectF(b[0], b[1], b[2], b[3]), context.getString(label(control)),
                    enabled(control), tab == control, density);
        }
        if (tab == ThorTavernControls.OFFERS || tab == ThorTavernControls.INVITATIONS)
        {
            for (int control = ThorTavernControls.ROW_FIRST; control <= ThorTavernControls.ROW_LAST; ++control)
            {
                int row = ThorTavernControls.row(state, tab, page, control);
                if (row < 0) continue;
                float[] b = ThorTavernControls.bounds(control, layout);
                ThorDeckDrawing.drawChoiceButton(canvas, paint, new RectF(b[0], b[1], b[2], b[3]), state.name(row),
                        enabled(control), state.id(row) == (tab == ThorTavernControls.OFFERS ? state.selected() : state.invited()), density);
            }
            final String hint = tab == ThorTavernControls.INVITATIONS ? context.getString(R.string.thor_tavern_invite_hint)
                    : context.getString(ThorTavernControls.count(state, tab) == 0 ? R.string.thor_tavern_no_heroes : R.string.thor_tavern_detail_hint);
            paragraph(canvas, paint, hint, new RectF(left, top + h * .71f, left + width, top + h * .75f), h * .016f);
        }
        else
            paragraph(canvas, paint, ThorTavernControls.pageText(state, tab, page),
                    new RectF(left, top + h * .29f, left + width, top + h * .75f), h * .026f);
        text(canvas, paint, (page + 1) + "/" + ThorTavernControls.pages(state, tab),
                left + width / 2, top + h * .82f, width * .4f, h * .022f);
    }
    boolean touch(MotionEvent event, long revision, long session, ThorPresentationLayout layout, boolean currentSession)
    {
        int control = ThorTavernControls.at(event.getX(), event.getY(), layout, tab), action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN)
        {
            downX = event.getX(); downY = event.getY(); downTime = event.getEventTime();
            gesture.begin(ThorContextIds.TAVERN, control, revision, session, event.getPointerId(0),
                    enabled(control) && state.revision == revision && event.getPointerCount() == 1);
            return false;
        }
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_POINTER_DOWN
                || action == MotionEvent.ACTION_POINTER_UP || event.getPointerCount() != 1)
        { cancel(); return false; }
        float slop = Math.max(8, layout.bevel * 2);
        if (Math.abs(event.getX() - downX) > slop || Math.abs(event.getY() - downY) > slop || event.getEventTime() - downTime > 700) cancel();
        if (action != MotionEvent.ACTION_UP || !gesture.finish(ThorContextIds.TAVERN, control, event.getPointerCount(),
                event.getPointerId(0), revision, session, enabled(control) && state.revision == revision, currentSession)) return false;
        if (control >= ThorTavernControls.OFFERS && control <= ThorTavernControls.RUMOR)
        { tab = control; page = 0; return true; }
        if (control == ThorTavernControls.PREVIOUS || control == ThorTavernControls.NEXT)
        { page += control == ThorTavernControls.PREVIOUS ? -1 : 1; return true; }
        submitted = NativeMethods.submitThorCommand(revision, ThorTavernControls.command(control, tab),
                ThorTavernControls.target(state, tab, page, control));
        return submitted;
    }
    String description(Context context)
    {
        StringBuilder result = new StringBuilder(context.getString(label(tab))).append(". ")
                .append(context.getString(R.string.thor_tavern_cost, state.cost(), state.gold())).append(". ")
                .append(context.getString(reason(state.reason())));
        if (tab == ThorTavernControls.DETAILS || tab == ThorTavernControls.RUMOR)
            result.append(". ").append(ThorTavernControls.pageText(state, tab, page));
        else
            for (int c = ThorTavernControls.ROW_FIRST; c <= ThorTavernControls.ROW_LAST; ++c)
            {
                int row = ThorTavernControls.row(state, tab, page, c);
                if (row >= 0) result.append(". ").append(state.name(row));
            }
        return result.toString();
    }
}
