package eu.vcmi.vcmi;

/** One tile-to-panel affine transform for raster, overlays and hit testing. */
final class ThorMapTransform
{
    final int width, height;
    final float left, top, right, bottom, scale, originX, originY;

    ThorMapTransform(final int width, final int height, final float left, final float top,
            final float right, final float bottom, final int zoom, final float centerX, final float centerY)
    {
        this.width = width;
        this.height = height;
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        scale = ThorAdventureMap.dimensionsValid(width, height) && right > left && bottom > top
                && (zoom == 1 || zoom == 2 || zoom == 4)
                && Float.isFinite(centerX) && Float.isFinite(centerY)
                ? Math.min((right - left) / width, (bottom - top) / height) * zoom : 0;
        final float cx = clampedCenter(centerX, width, scale > 0 ? (right - left) / scale : width);
        final float cy = clampedCenter(centerY, height, scale > 0 ? (bottom - top) / scale : height);
        originX = (left + right) * 0.5f - cx * scale;
        originY = (top + bottom) * 0.5f - cy * scale;
    }

    static float clampedCenter(final float value, final int size, final float visible)
    {
        return visible >= size ? size * 0.5f : Math.max(visible * 0.5f, Math.min(size - visible * 0.5f, value));
    }

    float screenX(final float tileX) { return originX + tileX * scale; }
    float screenY(final float tileY) { return originY + tileY * scale; }

    int tileAt(final float x, final float y)
    {
        if (!(scale > 0) || !Float.isFinite(x) || !Float.isFinite(y)
                || x < left || x >= right || y < top || y >= bottom)
            return -1;
        final float tx = (x - originX) / scale;
        final float ty = (y - originY) / scale;
        if (tx < 0 || tx >= width || ty < 0 || ty >= height)
            return -1;
        return (int) ty * ThorAdventureMap.MAX_SIDE + (int) tx;
    }
}
