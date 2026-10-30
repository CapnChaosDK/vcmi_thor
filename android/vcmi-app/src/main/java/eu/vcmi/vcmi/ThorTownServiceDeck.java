package eu.vcmi.vcmi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

/** Blacksmith and Guild presentation; native remains the sole action authority. */
final class ThorTownServiceDeck
{
    private final ThorTownServiceJourney journey = new ThorTownServiceJourney();
    private float downX, downY;
    private long downTime;

    void cancel() { journey.cancel(); }
    void withdraw() { journey.withdraw(); }
    void reset() { journey.reset(); }
    void update(ThorTownServiceState state) { journey.update(state); }

    static float[] bounds(int control, ThorPresentationLayout layout)
    {
        float left = layout.margin + layout.bevel * 3, width = layout.right - layout.bevel * 3 - left;
        float top = layout.margin, h = layout.contentHeight;
        if (control == ThorTownServiceJourney.LEVEL_PREVIOUS || control == ThorTownServiceJourney.LEVEL_NEXT)
        {
            float x = left + (control == ThorTownServiceJourney.LEVEL_PREVIOUS ? 0 : width * .76f);
            return new float[]{x, top + h * .12f, x + width * .24f, top + h * .21f};
        }
        if (control >= ThorTownServiceJourney.ROW_FIRST && control <= ThorTownServiceJourney.ROW_LAST)
        {
            float y = top + h * (.24f + (control - ThorTownServiceJourney.ROW_FIRST) * .105f);
            return new float[]{left, y, left + width, y + h * .095f};
        }
        if (control == ThorTownServiceJourney.PREVIOUS || control == ThorTownServiceJourney.NEXT)
        {
            float x = left + (control == ThorTownServiceJourney.PREVIOUS ? 0 : width * .76f);
            return new float[]{x, top + h * .73f, x + width * .24f, top + h * .82f};
        }
        if (control >= ThorTownServiceJourney.ACT && control <= ThorTownServiceJourney.CLOSE)
        {
            float x = left + (control - ThorTownServiceJourney.ACT) * width / 3;
            return new float[]{x, top + h * .85f, x + width * .32f, top + h * .95f};
        }
        return null;
    }
    private boolean visible(int control)
    {
        if (control == ThorTownServiceJourney.LEVEL_PREVIOUS || control == ThorTownServiceJourney.LEVEL_NEXT)
            return journey.state.kind() == ThorTownServiceState.GUILD && journey.selected < 0;
        if (control == ThorTownServiceJourney.LIST) return journey.state.kind() == ThorTownServiceState.GUILD;
        if (control >= ThorTownServiceJourney.ROW_FIRST) return journey.row(control) >= 0;
        return true;
    }
    private int at(float x, float y, ThorPresentationLayout layout)
    {
        for (int control = 1; control <= ThorTownServiceJourney.CLOSE; ++control)
            if (visible(control) && inside(x, y, bounds(control, layout))) return control;
        for (int control = ThorTownServiceJourney.ROW_FIRST; control <= ThorTownServiceJourney.ROW_LAST; ++control)
            if (visible(control) && inside(x, y, bounds(control, layout))) return control;
        return 0;
    }
    private static boolean inside(float x, float y, float[] box)
    { return box != null && x >= box[0] && x < box[2] && y >= box[1] && y < box[3]; }
    private int label(int control)
    {
        switch (control)
        {
            case ThorTownServiceJourney.LEVEL_PREVIOUS:
            case ThorTownServiceJourney.PREVIOUS: return R.string.thor_previous;
            case ThorTownServiceJourney.LEVEL_NEXT:
            case ThorTownServiceJourney.NEXT: return R.string.thor_next;
            case ThorTownServiceJourney.ACT: return journey.state.kind() == ThorTownServiceState.BLACKSMITH
                    ? R.string.thor_service_buy : R.string.thor_service_research;
            case ThorTownServiceJourney.LIST: return R.string.thor_service_spells;
            default: return R.string.thor_market_close;
        }
    }
    private static void text(Canvas canvas, Paint paint, String value, float x, float y, float width, float size)
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
    void draw(Canvas canvas, Paint paint, Context context, ThorPresentationLayout layout, float density)
    {
        float left = layout.margin + layout.bevel * 3, width = layout.right - layout.bevel * 3 - left;
        float top = layout.margin, h = layout.contentHeight, center = left + width / 2;
        boolean blacksmith = journey.state.kind() == ThorTownServiceState.BLACKSMITH;
        text(canvas, paint, context.getString(blacksmith ? R.string.thor_service_blacksmith : R.string.thor_town_service_mage_guild),
                left + width * .29f, top + h * .05f, width * .58f, h * .035f);
        if (journey.state == ThorTownServiceState.EMPTY)
        {
            text(canvas, paint, context.getString(R.string.thor_service_loading), center, top + h * .4f, width, h * .026f);
            return;
        }
        if (blacksmith)
        {
            text(canvas, paint, journey.state.summary(), center, top + h * .10f, width, h * .025f);
            text(canvas, paint, context.getString(R.string.thor_tavern_cost, journey.state.cost(), journey.state.gold()),
                    center, top + h * .17f, width, h * .025f);
        }
        else if (journey.selected < 0)
            text(canvas, paint, journey.level == 0 ? context.getString(R.string.thor_service_all_levels)
                    : context.getString(R.string.thor_service_level, journey.level), center, top + h * .18f, width * .48f, h * .025f);
        int selected = journey.selectedRow();
        if (selected >= 0 || journey.count() == 0)
        {
            if (selected >= 0)
                text(canvas, paint, journey.state.name(selected), center, top + h * .27f, width, h * .03f);
            else if (journey.state.summary().isEmpty())
                text(canvas, paint, context.getString(R.string.thor_service_no_spells), center, top + h * .27f, width, h * .026f);
            paragraph(canvas, paint, journey.pageText(), new RectF(left, top + h * .31f, left + width, top + h * .61f), Math.min(h * .02f, width / 40));
        }
        else
            for (int control = ThorTownServiceJourney.ROW_FIRST; control <= ThorTownServiceJourney.ROW_LAST; ++control)
            {
                int row = journey.row(control);
                if (row < 0) continue;
                float[] b = bounds(control, layout);
                String name = context.getString(R.string.thor_service_spell_row, journey.state.level(row), journey.state.name(row));
                ThorDeckDrawing.drawChoiceButton(canvas, paint, new RectF(b[0], b[1], b[2], b[3]), name,
                        journey.enabled(control), false, density);
            }
        text(canvas, paint, context.getString(journey.submitted ? R.string.thor_service_pending
                : blacksmith ? R.string.thor_service_blacksmith_hint : R.string.thor_service_guild_hint),
                center, top + h * .68f, width, h * .019f);
        for (int control = 1; control <= ThorTownServiceJourney.CLOSE; ++control)
        {
            if (!visible(control)) continue;
            float[] b = bounds(control, layout);
            ThorDeckDrawing.drawChoiceButton(canvas, paint, new RectF(b[0], b[1], b[2], b[3]), context.getString(label(control)),
                    journey.enabled(control), false, density);
        }
        text(canvas, paint, (journey.selected >= 0 || journey.count() == 0 ? journey.textPage + 1 : journey.page + 1)
                + "/" + (journey.selected >= 0 || journey.count() == 0 ? journey.textPages() : journey.pages()),
                center, top + h * .79f, width * .4f, h * .022f);
    }
    boolean touch(MotionEvent event, long revision, long session, ThorPresentationLayout layout, boolean currentSession)
    {
        int control = at(event.getX(), event.getY(), layout), action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN)
        {
            downX = event.getX(); downY = event.getY(); downTime = event.getEventTime();
            journey.begin(control, revision, session, event.getPointerId(0), event.getPointerCount());
            return false;
        }
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_POINTER_DOWN
                || action == MotionEvent.ACTION_POINTER_UP || event.getPointerCount() != 1)
        { cancel(); return false; }
        float slop = Math.max(8, layout.bevel * 2);
        if (Math.abs(event.getX() - downX) > slop || Math.abs(event.getY() - downY) > slop || event.getEventTime() - downTime > 700) cancel();
        if (action != MotionEvent.ACTION_UP) return false;
        int[] command = journey.release(control, revision, session, event.getPointerId(0), event.getPointerCount(), currentSession);
        if (command != null) journey.submitted = NativeMethods.submitThorCommand(revision, command[0], command[1]);
        return command != null;
    }
    String description(Context context)
    {
        StringBuilder text = new StringBuilder(context.getString(journey.state.kind() == ThorTownServiceState.BLACKSMITH
                ? R.string.thor_service_blacksmith : R.string.thor_town_service_mage_guild));
        if (journey.state == ThorTownServiceState.EMPTY) return text.append(". ").append(context.getString(R.string.thor_service_loading)).toString();
        if (journey.selected >= 0) text.append(". ").append(journey.state.name(journey.selectedRow())).append(". ").append(journey.pageText());
        else for (int c = ThorTownServiceJourney.ROW_FIRST; c <= ThorTownServiceJourney.ROW_LAST; ++c)
        { int row = journey.row(c); if (row >= 0) text.append(". ").append(journey.state.name(row)); }
        text.append(". ").append(journey.state.summary());
        if (journey.state.kind() == ThorTownServiceState.BLACKSMITH)
            text.append(". ").append(context.getString(R.string.thor_tavern_cost, journey.state.cost(), journey.state.gold()));
        text.append(". ").append(context.getString(label(ThorTownServiceJourney.ACT)))
                .append(": ").append(journey.enabled(ThorTownServiceJourney.ACT));
        return text.toString();
    }
}
