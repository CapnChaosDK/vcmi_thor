package eu.vcmi.vcmi;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

/** Existing deck palette and paint primitives, shared without retaining a view. */
final class ThorDeckDrawing
{
    static final int BACKGROUND = Color.rgb(30, 31, 28);
    static final int STONE_DARK = Color.rgb(55, 57, 52);
    static final int STONE_LIGHT = Color.rgb(105, 105, 94);
    static final int PARCHMENT = Color.rgb(189, 166, 119);
    static final int PARCHMENT_DARK = Color.rgb(91, 69, 42);
    static final int GOLD = Color.rgb(205, 166, 66);
    static final int TEXT = Color.rgb(244, 229, 184);

    static void drawChoiceButton(final Canvas canvas, final Paint paint, final RectF bounds, final String label,
                                 final boolean enabled, final boolean selected, final float density)
    {
        final float radius = Math.max(4f * density, bounds.height() * 0.08f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(selected ? Color.rgb(105, 81, 37) : enabled ? STONE_DARK : Color.rgb(76, 74, 67));
        canvas.drawRoundRect(bounds, radius, radius, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(selected ? Math.max(3f, radius * 0.28f) : Math.max(2f, radius * 0.18f));
        paint.setColor(selected || enabled ? GOLD : PARCHMENT_DARK);
        canvas.drawRoundRect(bounds, radius, radius, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(enabled || selected);
        paint.setColor(enabled || selected ? TEXT : PARCHMENT_DARK);
        drawFittedText(canvas, paint, label, bounds.centerX(), bounds.centerY(), bounds.width() * 0.88f,
                Math.min(25f * density, bounds.height() * 0.36f));
        paint.setFakeBoldText(false);
    }

    static void drawFittedText(final Canvas canvas, final Paint paint, final String text, final float centerX,
                               final float baseline, final float maxWidth, final float preferredSize)
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

    static RectF frame(final ThorPresentationLayout layout)
    {
        return new RectF(layout.margin, layout.margin, layout.right, layout.bottom);
    }

    private ThorDeckDrawing() {}
}
