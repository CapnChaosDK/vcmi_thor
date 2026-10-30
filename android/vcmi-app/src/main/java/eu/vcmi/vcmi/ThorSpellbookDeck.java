package eu.vcmi.vcmi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

/** Lower-screen spellbook view. Native owners retain casting and targeting. */
final class ThorSpellbookDeck
{
    private final ThorSpellbookJourney journey = new ThorSpellbookJourney();
    private float downX, downY;
    private long downTime;
    void cancel() { journey.cancel(); }
    void withdraw() { journey.withdraw(); }
    void reset() { journey.reset(); }
    void update(ThorSpellbookState state) { journey.update(state); }
    static float[] bounds(int control, ThorPresentationLayout layout)
    {
        float left = layout.margin + layout.bevel * 3, w = layout.right - layout.bevel * 3 - left;
        float top = layout.margin, h = layout.contentHeight;
        if (control == ThorSpellbookJourney.MODE) return new float[]{left, top + h * .07f, left + w * .60f, top + h * .14f};
        if (control >= ThorSpellbookJourney.SCHOOL_PREVIOUS && control <= ThorSpellbookJourney.LEVEL_NEXT)
        {
            boolean school = control <= ThorSpellbookJourney.SCHOOL_NEXT;
            boolean previous = control == ThorSpellbookJourney.SCHOOL_PREVIOUS || control == ThorSpellbookJourney.LEVEL_PREVIOUS;
            float x = left + (previous ? 0 : w * .78f), y = top + h * (school ? .16f : .25f);
            return new float[]{x, y, x + w * .22f, y + h * .07f};
        }
        if (control >= ThorSpellbookJourney.ROW_FIRST && control <= ThorSpellbookJourney.ROW_LAST)
        {
            float y = top + h * (.35f + (control - ThorSpellbookJourney.ROW_FIRST) * .085f);
            return new float[]{left, y, left + w, y + h * .075f};
        }
        if (control == ThorSpellbookJourney.PREVIOUS || control == ThorSpellbookJourney.NEXT)
        {
            float x = left + (control == ThorSpellbookJourney.PREVIOUS ? 0 : w * .78f);
            return new float[]{x, top + h * .73f, x + w * .22f, top + h * .82f};
        }
        if (control >= ThorSpellbookJourney.CAST && control <= ThorSpellbookJourney.CLOSE)
        {
            float x = left + (control - ThorSpellbookJourney.CAST) * w / 3;
            return new float[]{x, top + h * .85f, x + w * .32f, top + h * .95f};
        }
        return null;
    }
    private boolean visible(int control)
    {
        if (control <= ThorSpellbookJourney.LEVEL_NEXT) return journey.selected < 0;
        if (control >= ThorSpellbookJourney.ROW_FIRST) return journey.row(control) >= 0;
        return true;
    }
    private int at(float x, float y, ThorPresentationLayout layout)
    {
        for (int control = 1; control <= ThorSpellbookJourney.CLOSE; ++control)
            if (visible(control) && inside(x, y, bounds(control, layout))) return control;
        for (int control = ThorSpellbookJourney.ROW_FIRST; control <= ThorSpellbookJourney.ROW_LAST; ++control)
            if (visible(control) && inside(x, y, bounds(control, layout))) return control;
        return 0;
    }
    private static boolean inside(float x, float y, float[] box)
    { return box != null && x >= box[0] && x < box[2] && y >= box[1] && y < box[3]; }
    private String label(int control, Context context)
    {
        switch (control)
        {
            case ThorSpellbookJourney.MODE: return context.getString(journey.combat ? R.string.thor_spellbook_battle : R.string.thor_spellbook_adventure);
            case ThorSpellbookJourney.SCHOOL_PREVIOUS:
            case ThorSpellbookJourney.LEVEL_PREVIOUS:
            case ThorSpellbookJourney.PREVIOUS: return context.getString(R.string.thor_previous);
            case ThorSpellbookJourney.SCHOOL_NEXT:
            case ThorSpellbookJourney.LEVEL_NEXT:
            case ThorSpellbookJourney.NEXT: return context.getString(R.string.thor_next);
            case ThorSpellbookJourney.CAST: return context.getString(R.string.thor_spellbook_cast);
            case ThorSpellbookJourney.LIST: return context.getString(R.string.thor_service_spells);
            default: return context.getString(R.string.thor_market_close);
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
        float left = layout.margin + layout.bevel * 3, w = layout.right - layout.bevel * 3 - left;
        float top = layout.margin, h = layout.contentHeight, center = left + w / 2;
        text(canvas, paint, context.getString(R.string.thor_spellbook_title), left + w * .29f,
                top + h * .05f, w * .58f, h * .035f);
        if (journey.state == ThorSpellbookState.EMPTY)
        {
            text(canvas, paint, context.getString(R.string.thor_service_loading), center, top + h * .4f, w, h * .026f);
            return;
        }
        if (journey.selected < 0)
        {
            text(canvas, paint, journey.school < 0 ? context.getString(R.string.thor_spellbook_all_schools)
                    : journey.state.schoolName(journey.schoolIndex()), center, top + h * .205f, w * .50f, h * .023f);
            text(canvas, paint, journey.level < 0 ? context.getString(R.string.thor_service_all_levels)
                    : context.getString(R.string.thor_service_level, journey.level), center, top + h * .295f, w * .50f, h * .023f);
        }
        int selected = journey.selectedRow();
        if (selected >= 0)
        {
            ThorSpellbookState.Row row = journey.state.row(selected);
            text(canvas, paint, journey.state.heroName, center, top + h * .14f, w, h * .026f);
            text(canvas, paint, row.name, center, top + h * .24f, w, h * .03f);
            text(canvas, paint, context.getString(R.string.thor_spellbook_cost, row.level, row.cost, journey.state.mana),
                    center, top + h * .30f, w, h * .024f);
            paragraph(canvas, paint, journey.pageText(), new RectF(left, top + h * .35f, left + w, top + h * .67f), Math.min(h * .02f, w / 40));
        }
        else if (journey.count() == 0)
            text(canvas, paint, context.getString(R.string.thor_service_no_spells), center, top + h * .48f, w, h * .026f);
        else for (int control = ThorSpellbookJourney.ROW_FIRST; control <= ThorSpellbookJourney.ROW_LAST; ++control)
        {
            int index = journey.row(control); if (index < 0) continue;
            ThorSpellbookState.Row row = journey.state.row(index); float[] b = bounds(control, layout);
            ThorDeckDrawing.drawChoiceButton(canvas, paint, new RectF(b[0], b[1], b[2], b[3]),
                    context.getString(R.string.thor_spellbook_row, row.level, row.name, row.cost), true, false, density);
        }
        text(canvas, paint, context.getString(journey.submitted ? R.string.thor_service_pending
                : R.string.thor_spellbook_hint), center, top + h * .705f, w, h * .018f);
        for (int control = 1; control <= ThorSpellbookJourney.CLOSE; ++control)
        {
            if (!visible(control)) continue;
            float[] b = bounds(control, layout);
            ThorDeckDrawing.drawChoiceButton(canvas, paint, new RectF(b[0], b[1], b[2], b[3]), label(control, context),
                    journey.enabled(control), false, density);
        }
        text(canvas, paint, (journey.selected >= 0 ? journey.textPage + 1 : journey.page + 1) + "/"
                + (journey.selected >= 0 ? journey.textPages() : journey.pages()), center, top + h * .79f, w * .4f, h * .022f);
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
                || action == MotionEvent.ACTION_POINTER_UP || event.getPointerCount() != 1) { cancel(); return false; }
        float slop = Math.max(8, layout.bevel * 2);
        if (Math.abs(event.getX() - downX) > slop || Math.abs(event.getY() - downY) > slop || event.getEventTime() - downTime > 700) cancel();
        if (action != MotionEvent.ACTION_UP) return false;
        int[] command = journey.release(control, revision, session, event.getPointerId(0), event.getPointerCount(), currentSession);
        if (command != null) journey.submitted = NativeMethods.submitThorCommand(revision, command[0], command[1]);
        return command != null;
    }
    String description(Context context)
    {
        StringBuilder text = new StringBuilder(context.getString(R.string.thor_spellbook_title));
        if (journey.state == ThorSpellbookState.EMPTY) return text.append(". ").append(context.getString(R.string.thor_service_loading)).toString();
        text.append(". ").append(journey.state.heroName).append(". ").append(label(ThorSpellbookJourney.MODE, context));
        if (journey.selected >= 0)
        {
            ThorSpellbookState.Row row = journey.state.row(journey.selectedRow());
            text.append(". ").append(row.name).append(". ").append(context.getString(R.string.thor_spellbook_cost, row.level, row.cost, journey.state.mana))
                    .append(". ").append(journey.pageText());
        }
        else for (int c = ThorSpellbookJourney.ROW_FIRST; c <= ThorSpellbookJourney.ROW_LAST; ++c)
        { int row = journey.row(c); if (row >= 0) text.append(". ").append(journey.state.row(row).name); }
        text.append(". ").append(context.getString(R.string.thor_spellbook_cast)).append(": ").append(journey.enabled(ThorSpellbookJourney.CAST));
        return text.toString();
    }
}
