package eu.vcmi.vcmi;

/** Local-only touch state for Hero Meeting rows; it never submits native actions. */
final class ThorHeroMeetingGesture
{
    static boolean retainsArmies(final long previousRevision, final String previousContext,
                                 final long publishedRevision, final String publishedContext)
    {
        return previousRevision == publishedRevision
                && ThorContextIds.HERO_MEETING.equals(previousContext)
                && ThorContextIds.HERO_MEETING.equals(publishedContext);
    }

    enum Kind
    {
        TAP,
        DROP,
        LONG_PRESS,
        CANCELLED
    }

    static final class Result
    {
        final Kind kind;
        final int sourceKey;
        final int destinationKey;

        private Result(final Kind kind, final int sourceKey, final int destinationKey)
        {
            this.kind = kind;
            this.sourceKey = sourceKey;
            this.destinationKey = destinationKey;
        }
    }

    private static final Result CANCELLED = new Result(Kind.CANCELLED, -1, -1);

    private boolean armed;
    private boolean dragging;
    private boolean longPressed;
    private long revision;
    private int sourceKey = -1;
    private float downX;
    private float downY;
    private float pointerX;
    private float pointerY;

    boolean begin(final long renderedRevision, final int key, final boolean movable,
                  final float x, final float y)
    {
        cancel();
        if (renderedRevision <= 0 || !movable || key < 0 || key >= ThorHeroMeetingTransferPair.SLOT_KEY_COUNT)
            return false;
        armed = true;
        revision = renderedRevision;
        sourceKey = key;
        downX = pointerX = x;
        downY = pointerY = y;
        return true;
    }

    boolean move(final float x, final float y, final float touchSlop, final long currentRevision,
                 final boolean stillInHeroMeeting)
    {
        if (!armed)
            return false;
        if (!stillInHeroMeeting || currentRevision != revision || touchSlop < 0)
        {
            cancel();
            return false;
        }
        pointerX = x;
        pointerY = y;
        final float dx = x - downX;
        final float dy = y - downY;
        if (!longPressed && dx * dx + dy * dy > touchSlop * touchSlop)
            dragging = true;
        return dragging;
    }

    boolean activateLongPress(final long currentRevision, final boolean stillInHeroMeeting)
    {
        if (!armed || dragging || currentRevision != revision || !stillInHeroMeeting)
            return false;
        longPressed = true;
        return true;
    }

    Result finish(final long currentRevision, final boolean stillInHeroMeeting, final int destinationKey)
    {
        if (!armed)
            return CANCELLED;
        final int source = sourceKey;
        final boolean wasDragging = dragging;
        final boolean wasLongPressed = longPressed;
        final boolean current = stillInHeroMeeting && currentRevision == revision;
        cancel();
        if (!current)
            return CANCELLED;
        if (wasLongPressed)
            return new Result(Kind.LONG_PRESS, source, -1);
        if (!wasDragging)
            return new Result(Kind.TAP, source, destinationKey);
        if (ThorHeroMeetingTransferPair.encode(source, destinationKey) == ThorHeroMeetingTransferPair.INVALID)
            return CANCELLED;
        return new Result(Kind.DROP, source, destinationKey);
    }

    void cancel()
    {
        armed = false;
        dragging = false;
        longPressed = false;
        revision = 0;
        sourceKey = -1;
        downX = downY = pointerX = pointerY = 0;
    }

    boolean isArmed()
    {
        return armed;
    }

    boolean isDragging()
    {
        return armed && dragging;
    }

    boolean isLongPressed()
    {
        return armed && longPressed;
    }

    int sourceKey()
    {
        return armed ? sourceKey : -1;
    }

    float pointerX()
    {
        return pointerX;
    }

    float pointerY()
    {
        return pointerY;
    }
}
