package eu.vcmi.vcmi;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.StaticLayout;
import android.view.MotionEvent;

/** Shared journal reader and one-shot, already-covered native Puzzle Map image. */
final class ThorReferenceDeck
{
    final ThorReferenceJourney journey = new ThorReferenceJourney();
    private Bitmap puzzle;
    private float downX, downY;
    private long downTime;
    void cancel() { journey.cancel(); }
    void withdraw() { journey.withdraw(); puzzle = null; }
    void reset() { journey.reset(); puzzle = null; }
    void update(ThorReferenceState state)
    {
        journey.update(state);
        puzzle = state.width() == 0 ? null : Bitmap.createBitmap(state.pixels(), state.width(), state.height(), Bitmap.Config.ARGB_8888);
    }
    static float[] bounds(int control, ThorPresentationLayout layout)
    {
        float left = layout.margin + layout.bevel * 3, width = layout.right - layout.bevel * 3 - left;
        float top = layout.margin, h = layout.contentHeight;
        if (control == ThorReferenceJourney.QUESTS || control == ThorReferenceJourney.EVENTS)
        {
            float x = left + (control - 1) * width * .51f;
            return new float[]{x, top + h * .12f, x + width * .49f, top + h * .22f};
        }
        if (control >= ThorReferenceJourney.ROW_FIRST && control <= ThorReferenceJourney.ROW_LAST)
        {
            float y = top + h * (.25f + (control - ThorReferenceJourney.ROW_FIRST) * .09f);
            return new float[]{left, y, left + width, y + h * .08f};
        }
        if (control == ThorReferenceJourney.PREVIOUS || control == ThorReferenceJourney.NEXT)
        {
            float x = left + (control == ThorReferenceJourney.PREVIOUS ? 0 : width * .76f);
            return new float[]{x, top + h * .73f, x + width * .24f, top + h * .83f};
        }
        int column = control == ThorReferenceJourney.READ || control == ThorReferenceJourney.LIST ? 0
                : control == ThorReferenceJourney.CENTER ? 1 : 2;
        if (control >= ThorReferenceJourney.READ && control <= ThorReferenceJourney.CLOSE)
        {
            float x = left + column * width / 3;
            return new float[]{x, top + h * .86f, x + width * .32f, top + h * .96f};
        }
        return null;
    }
    private boolean visible(int control)
    {
        if (journey.state.kind() == 3) return control == ThorReferenceJourney.CLOSE;
        if (journey.reading && (control == ThorReferenceJourney.QUESTS || control == ThorReferenceJourney.EVENTS)) return false;
        if (control == ThorReferenceJourney.READ) return !journey.reading;
        if (control == ThorReferenceJourney.LIST) return journey.reading;
        if (control >= ThorReferenceJourney.ROW_FIRST)
            return !journey.reading && control - ThorReferenceJourney.ROW_FIRST < journey.state.count();
        return true;
    }
    private int at(float x, float y, ThorPresentationLayout layout)
    {
        for (int c = 1; c <= ThorReferenceJourney.CLOSE; ++c) if (visible(c) && inside(x, y, bounds(c, layout))) return c;
        for (int c = ThorReferenceJourney.ROW_FIRST; c <= ThorReferenceJourney.ROW_LAST; ++c)
            if (visible(c) && inside(x, y, bounds(c, layout))) return c;
        return 0;
    }
    private static boolean inside(float x, float y, float[] b)
    { return b != null && x >= b[0] && x < b[2] && y >= b[1] && y < b[3]; }
    private int label(int c)
    {
        switch (c)
        {
            case ThorReferenceJourney.QUESTS: return R.string.thor_reference_quests;
            case ThorReferenceJourney.EVENTS: return R.string.thor_reference_events;
            case ThorReferenceJourney.PREVIOUS: return R.string.thor_previous;
            case ThorReferenceJourney.NEXT: return R.string.thor_next;
            case ThorReferenceJourney.READ: return R.string.thor_reference_read;
            case ThorReferenceJourney.LIST: return R.string.thor_reference_list;
            case ThorReferenceJourney.CENTER: return R.string.thor_reference_center;
            default: return R.string.thor_market_close;
        }
    }
    private static void text(Canvas canvas, Paint paint, String value, float x, float y, float width, float size, int color)
    {
        paint.setStyle(Paint.Style.FILL); paint.setColor(color); paint.setTextAlign(Paint.Align.CENTER);
        ThorDeckDrawing.drawFittedText(canvas, paint, value, x, y, width, size);
    }
    void draw(Canvas canvas, Paint paint, Context context, ThorPresentationLayout layout, float density)
    {
        float left = layout.margin + layout.bevel * 3, width = layout.right - layout.bevel * 3 - left;
        float top = layout.margin, h = layout.contentHeight, center = left + width / 2;
        ThorReferenceState state = journey.state;
        int title = state.kind() == 3 ? R.string.thor_context_puzzle_map
                : state.kind() == 2 ? R.string.thor_reference_events : R.string.thor_reference_quests;
        text(canvas, paint, context.getString(title), left + width * .29f, top + h * .05f, width * .58f, h * .035f, ThorDeckDrawing.TEXT);
        if (state == ThorReferenceState.EMPTY)
        {
            text(canvas, paint, context.getString(R.string.thor_reference_unavailable), center, top + h * .4f, width, h * .025f, ThorDeckDrawing.PARCHMENT_DARK);
            return;
        }
        if (state.kind() == 3)
        {
            text(canvas, paint, context.getString(R.string.thor_reference_progress, state.progress()), center, top + h * .17f, width, h * .025f, ThorDeckDrawing.TEXT);
            if (puzzle != null)
            {
                float scale = Math.min(width / puzzle.getWidth(), h * .61f / puzzle.getHeight());
                float w = puzzle.getWidth() * scale, height = puzzle.getHeight() * scale;
                paint.setStyle(Paint.Style.FILL); paint.setAlpha(255); paint.setFilterBitmap(true);
                canvas.drawBitmap(puzzle, null, new RectF(center - w / 2, top + h * .23f, center + w / 2, top + h * .23f + height), paint);
            }
            else text(canvas, paint, context.getString(R.string.thor_reference_image_pending), center, top + h * .5f, width, h * .024f, ThorDeckDrawing.PARCHMENT_DARK);
        }
        else if (journey.reading)
        {
            final RectF area = new RectF(left, top + h * .25f, left + width, top + h * .71f);
            final StaticLayout body = ThorDeckDrawing.paragraph(state.body(), ThorDeckDrawing.PARCHMENT_DARK,
                    Math.min(h * .03f, width / 38f), width);
            final java.util.ArrayList<Integer> starts = new java.util.ArrayList<>();
            starts.add(0);
            for (int line = 1; line < body.getLineCount(); ++line)
                if (body.getLineBottom(line) - starts.get(starts.size() - 1) > area.height())
                    starts.add(body.getLineTop(line));
            journey.localPages = starts.size();
            journey.localPage = Math.min(journey.localPage, journey.localPages - 1);
            final int start = starts.get(journey.localPage);
            if (journey.localPage + 1 < journey.localPages)
                area.bottom = area.top + starts.get(journey.localPage + 1) - start;
            ThorDeckDrawing.drawParagraph(canvas, body, area, start);
        }
        else if (state.count() == 0)
            text(canvas, paint, context.getString(R.string.thor_reference_empty), center, top + h * .42f, width, h * .025f, ThorDeckDrawing.PARCHMENT_DARK);
        for (int c = 1; c <= ThorReferenceJourney.CLOSE; ++c)
        {
            if (!visible(c)) continue;
            float[] b = bounds(c, layout);
            ThorDeckDrawing.drawChoiceButton(canvas, paint, new RectF(b[0], b[1], b[2], b[3]), context.getString(label(c)),
                    journey.enabled(c), c == (state.kind() == 1 ? ThorReferenceJourney.QUESTS
                            : state.kind() == 2 ? ThorReferenceJourney.EVENTS : -1), density);
        }
        if (state.kind() != 3)
        {
            for (int c = ThorReferenceJourney.ROW_FIRST; c <= ThorReferenceJourney.ROW_LAST; ++c)
            {
                if (!visible(c)) continue;
                float[] b = bounds(c, layout); int row = c - ThorReferenceJourney.ROW_FIRST;
                ThorDeckDrawing.drawChoiceButton(canvas, paint, new RectF(b[0], b[1], b[2], b[3]), state.label(row),
                        journey.enabled(c), state.selected() == state.target(row), density);
            }
            text(canvas, paint, (journey.reading ? state.textPage() + 1 : state.pages() == 0 ? 0 : state.page() + 1)
                    + "/" + (journey.reading ? state.textPages() : state.pages())
                    + (journey.reading && journey.localPages > 1 ? " (" + (journey.localPage + 1) + "/" + journey.localPages + ")" : ""), center, top + h * .79f, width * .45f, h * .025f, ThorDeckDrawing.PARCHMENT_DARK);
            if (!journey.reading && state.selected() >= 0 && state.textPages() == 0)
                text(canvas, paint, context.getString(R.string.thor_reference_unavailable), center, top + h * .715f, width, h * .018f, ThorDeckDrawing.PARCHMENT_DARK);
        }
    }
    boolean touch(MotionEvent event, long revision, long session, ThorPresentationLayout layout, boolean currentSession)
    {
        int control = at(event.getX(), event.getY(), layout), action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN)
        {
            downX = event.getX(); downY = event.getY(); downTime = event.getEventTime();
            journey.begin(control, revision, session, event.getPointerId(0), event.getPointerCount()); return false;
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
        ThorReferenceState state = journey.state;
        StringBuilder result = new StringBuilder(context.getString(state.kind() == 3 ? R.string.thor_context_puzzle_map
                : state.kind() == 2 ? R.string.thor_reference_events : R.string.thor_reference_quests));
        if (state == ThorReferenceState.EMPTY) return result.append(". ").append(context.getString(R.string.thor_reference_unavailable)).toString();
        if (state.kind() == 3) result.append(". ").append(context.getString(R.string.thor_reference_progress, state.progress()));
        else
        {
            for (int i = 0; i < state.count(); ++i) result.append(". ").append(state.label(i));
            result.append(". ").append(state.body());
        }
        return result.toString();
    }
}
