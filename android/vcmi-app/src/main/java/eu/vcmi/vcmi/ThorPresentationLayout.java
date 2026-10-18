package eu.vcmi.vcmi;

/** Shell geometry in pixels, recomputed from the current view size and display density. */
final class ThorPresentationLayout
{
    // Measured lower-panel reference: 1080 x 1240 px at approximately 369 dpi.
    final float margin;
    final float right;
    final float bottom;
    final float bevel;
    final float contentWidth;
    final float contentHeight;

    ThorPresentationLayout(final int width, final int height, final float density)
    {
        final float referenceScale = Math.min(width / 1080f, height / 1240f);
        margin = Math.max(16f * density, Math.max(Math.min(width, height) * 0.035f,
                Math.min(width, height) * 0.045f * referenceScale));
        right = width - margin;
        bottom = height - margin;
        bevel = Math.max(3f * density, margin * 0.16f);
        // Preserve the original operation order: drawing used these extents while
        // local control geometry used RectF.width()/height() (right/bottom - margin).
        contentWidth = width - margin * 2f;
        contentHeight = height - margin * 2f;
    }

    boolean hasContent()
    {
        return contentWidth > 0f && contentHeight > 0f;
    }
}
