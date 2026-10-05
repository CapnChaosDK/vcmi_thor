package eu.vcmi.vcmi;

/** Bounded engine-authored RGB24 minimap; no hidden object semantics are decoded here. */
final class ThorAdventureMap
{
    static final int MAX_SIDE = 512;
    static final int MAX_TILES = MAX_SIDE * MAX_SIDE;
    static final int MAX_BYTES = MAX_TILES * 3;
    static final int MARKER_FIELDS = 6;
    static final int MAX_MARKERS = ThorHeroRoster.MAX_HEROES + ThorTownRoster.MAX_TOWNS;
    static final ThorAdventureMap EMPTY = new ThorAdventureMap(0, 0, 0, 0, 0, 0, 0, 0, 0, null, new int[0]);
    final long contentRevision;
    final int width, height, level, levels, viewportX, viewportY, viewportWidth, viewportHeight;
    final byte[] rgb;
    final int[] markers;

    private ThorAdventureMap(final long contentRevision, final int width, final int height,
            final int level, final int levels, final int vx, final int vy, final int vw, final int vh,
            final byte[] rgb, final int[] markers)
    {
        this.contentRevision = contentRevision;
        this.width = width;
        this.height = height;
        this.level = level;
        this.levels = levels;
        viewportX = vx;
        viewportY = vy;
        viewportWidth = vw;
        viewportHeight = vh;
        this.rgb = rgb;
        this.markers = markers;
    }

    static boolean dimensionsValid(final int width, final int height)
    {
        return width > 0 && height > 0 && width <= MAX_SIDE && height <= MAX_SIDE
                && (long) width * height <= MAX_TILES;
    }

    static ThorAdventureMap copyOf(final ThorAdventureMap cached, final long contentRevision,
            final int width, final int height, final int level, final int levels,
            final int vx, final int vy, final int vw, final int vh, final byte[] rgb, final int[] markers)
    {
        if (contentRevision <= 0 || !dimensionsValid(width, height) || levels < 1 || levels > 2
                || level < 0 || level >= levels || vx < 0 || vy < 0 || vw < 0 || vh < 0
                || vx > width || vy > height || vw > width - vx || vh > height - vy
                || markers == null || markers.length % MARKER_FIELDS != 0
                || markers.length > MAX_MARKERS * MARKER_FIELDS)
            return EMPTY;
        final byte[] content;
        if (rgb != null)
        {
            if (rgb.length != width * height * 3)
                return EMPTY;
            content = rgb.clone();
        }
        else
        {
            if (cached == null || !cached.valid() || cached.contentRevision != contentRevision
                    || cached.width != width || cached.height != height || cached.level != level)
                return EMPTY;
            content = cached.rgb;
        }
        int heroes = 0;
        int towns = 0;
        for (int i = 0; i < markers.length; i += MARKER_FIELDS)
        {
            final int action = markers[i];
            if (action == ThorActionIds.SELECT_HERO)
                ++heroes;
            else if (action == ThorActionIds.SELECT_TOWN)
                ++towns;
            else
                return EMPTY;
            if (markers[i + 1] < 0 || markers[i + 2] < 0 || markers[i + 2] >= width
                    || markers[i + 3] < 0 || markers[i + 3] >= height
                    || markers[i + 4] < 0 || markers[i + 4] >= levels
                    || markers[i + 5] < 0 || markers[i + 5] > 1)
                return EMPTY;
            for (int j = 0; j < i; j += MARKER_FIELDS)
                if (markers[j + 1] == markers[i + 1])
                    return EMPTY;
        }
        if (heroes > ThorHeroRoster.MAX_HEROES || towns > ThorTownRoster.MAX_TOWNS)
            return EMPTY;
        return new ThorAdventureMap(contentRevision, width, height, level, levels, vx, vy, vw, vh,
                content, markers.clone());
    }

    boolean valid()
    {
        return contentRevision > 0;
    }

    static int encodeTarget(final int x, final int y, final int level)
    {
        return x >= 0 && x < MAX_SIDE && y >= 0 && y < MAX_SIDE && level >= 0 && level < 2
                ? level * MAX_TILES + y * MAX_SIDE + x : ThorActionIds.NO_TARGET;
    }

    /** Marker row or -1. Unique selected marker wins; ambiguous overlaps center the camera. */
    int markerAt(final ThorMapTransform transform, final float x, final float y, final float radius)
    {
        if (transform.tileAt(x, y) < 0)
            return -1;
        int candidate = -1;
        int hits = 0;
        int selected = -1;
        int selectedHits = 0;
        for (int i = 0; i < markers.length; i += MARKER_FIELDS)
        {
            if (markers[i + 4] != level)
                continue;
            final float mx = transform.screenX(markers[i + 2] + 0.5f);
            final float my = transform.screenY(markers[i + 3] + 0.5f);
            if ((mx - x) * (mx - x) + (my - y) * (my - y) > radius * radius)
                continue;
            candidate = i;
            ++hits;
            if (markers[i + 5] == 1)
            {
                selected = i;
                ++selectedHits;
            }
        }
        return selectedHits == 1 ? selected : (hits == 1 ? candidate : -1);
    }
}
