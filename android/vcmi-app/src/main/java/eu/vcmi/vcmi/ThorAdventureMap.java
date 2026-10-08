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
    final int[] objects;
    final String[] objectLabels;
    final boolean objectsLimited;
    static final int MAX_OBJECTS = 1024, OBJECT_FIELDS = 5, MAX_LABEL_BYTES = 256;

    private ThorAdventureMap(final long contentRevision, final int width, final int height,
            final int level, final int levels, final int vx, final int vy, final int vw, final int vh,
            final byte[] rgb, final int[] markers)
    {
        this(contentRevision, width, height, level, levels, vx, vy, vw, vh, rgb, markers,
                new int[0], new String[0], false);
    }

    private ThorAdventureMap(final long contentRevision, final int width, final int height,
            final int level, final int levels, final int vx, final int vy, final int vw, final int vh,
            final byte[] rgb, final int[] markers, final int[] objects, final String[] labels, final boolean limited)
    {
        this.objects = objects;
        this.objectLabels = labels;
        this.objectsLimited = limited;
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

    static boolean objectsBounded(final int[] objects, final String[] labels)
    {
        if (objects == null || labels == null || objects.length % OBJECT_FIELDS != 0
                || objects.length > MAX_OBJECTS * OBJECT_FIELDS || labels.length != objects.length / OBJECT_FIELDS)
            return false;
        for (final String label : labels)
        {
            if (label == null || label.isEmpty() || label.length() > MAX_LABEL_BYTES
                    || label.indexOf('\0') >= 0 || label.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_LABEL_BYTES)
                return false;
            for (int i = 0; i < label.length(); ++i)
                if (Character.isHighSurrogate(label.charAt(i)))
                {
                    if (++i >= label.length() || !Character.isLowSurrogate(label.charAt(i))) return false;
                }
                else if (Character.isLowSurrogate(label.charAt(i))) return false;
        }
        return true;
    }

    static ThorAdventureMap copyOf(final ThorAdventureMap cached, final long contentRevision,
            final int width, final int height, final int level, final int levels,
            final int vx, final int vy, final int vw, final int vh, final byte[] rgb, final int[] markers,
            final int[] objects, final String[] labels, final boolean limited)
    {
        if (!objectsBounded(objects, labels))
            return EMPTY;
        int previous = -1;
        for (int i = 0; i < objects.length; i += OBJECT_FIELDS)
        {
            if (objects[i] <= previous || objects[i + 1] < 0 || objects[i + 1] >= width
                    || objects[i + 2] < 0 || objects[i + 2] >= height || objects[i + 3] != level
                    || objects[i + 4] < 1 || objects[i + 4] > 5)
                return EMPTY;
            previous = objects[i];
        }
        final ThorAdventureMap map = copyOf(cached, contentRevision, width, height, level, levels,
                vx, vy, vw, vh, rgb, markers);
        if (!map.valid())
            return EMPTY;
        return new ThorAdventureMap(contentRevision, width, height, level, levels, vx, vy, vw, vh,
                map.rgb, map.markers, objects.clone(), labels.clone(), limited);
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
