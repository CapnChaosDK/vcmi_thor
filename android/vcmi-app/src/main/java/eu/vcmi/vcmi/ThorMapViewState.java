package eu.vcmi.vcmi;

/** Android-local map state retained only by the current Adventure controller. */
final class ThorMapViewState
{
    int zoom = 1;
    int categoryFilter;
    boolean pan;
    float centerX, centerY;
    private int width, height, level = -1;

    ThorMapViewState copy()
    {
        final ThorMapViewState copy = new ThorMapViewState();
        copy.zoom = zoom;
        copy.categoryFilter = categoryFilter;
        copy.pan = pan;
        copy.centerX = centerX;
        copy.centerY = centerY;
        copy.width = width;
        copy.height = height;
        copy.level = level;
        return copy;
    }

    void update(final ThorAdventureMap map)
    {
        if (!map.valid())
            return;
        if (width != map.width || height != map.height || level != map.level)
        {
            centerX = map.viewportX + map.viewportWidth * 0.5f;
            centerY = map.viewportY + map.viewportHeight * 0.5f;
        }
        width = map.width;
        height = map.height;
        level = map.level;
    }

    int modeKey() { return (zoom * 2 + (pan ? 1 : 0)) * 6 + categoryFilter; }

    void setZoom(final int value)
    {
        zoom = value == 2 || value == 4 ? value : 1;
        if (zoom == 1)
            pan = false;
    }

    void panFrom(final ThorMapTransform start, final float dx, final float dy)
    {
        if (!pan || zoom == 1 || !(start.scale > 0) || !Float.isFinite(dx) || !Float.isFinite(dy))
            return;
        centerX = ThorMapTransform.clampedCenter(((start.left + start.right) * 0.5f - start.originX - dx)
                / start.scale, start.width, (start.right - start.left) / start.scale);
        centerY = ThorMapTransform.clampedCenter(((start.top + start.bottom) * 0.5f - start.originY - dy)
                / start.scale, start.height, (start.bottom - start.top) / start.scale);
    }
}
