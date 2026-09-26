package eu.vcmi.vcmi;

/** Local touch arbitration for one visible artifact row. Native code validates every submitted pair. */
final class ThorHeroMeetingArtifactGesture
{
    enum Kind { TAP, DROP, CANCELLED }

    private boolean armed;
    private boolean dragging;
    private long revision;
    private int source = -1;
    private float downX;
    private float downY;
    private float pointerX;
    private float pointerY;

    boolean begin(final long currentRevision, final int sourceKey, final boolean legal,
                  final float x, final float y)
    {
        cancel();
        if (currentRevision <= 0 || !legal || sourceKey < 0 || sourceKey >= ThorHeroMeetingArtifacts.SLOT_COUNT)
            return false;
        armed = true;
        revision = currentRevision;
        source = sourceKey;
        downX = pointerX = x;
        downY = pointerY = y;
        return true;
    }

    boolean move(final float x, final float y, final float slop, final long currentRevision,
                 final boolean artifactsMode)
    {
        if (!armed)
            return false;
        if (currentRevision != revision || !artifactsMode || slop < 0)
        {
            cancel();
            return false;
        }
        pointerX = x;
        pointerY = y;
        final float dx = x - downX;
        final float dy = y - downY;
        if (dx * dx + dy * dy > slop * slop)
            dragging = true;
        return dragging;
    }

    Kind finish(final long currentRevision, final boolean artifactsMode, final int destination)
    {
        if (!armed)
            return Kind.CANCELLED;
        final boolean wasDragging = dragging;
        final int sourceKey = source;
        final boolean current = currentRevision == revision && artifactsMode;
        cancel();
        if (!current)
            return Kind.CANCELLED;
        if (!wasDragging)
            return Kind.TAP;
        return ThorHeroMeetingArtifactPair.encode(sourceKey, destination) == ThorHeroMeetingArtifactPair.INVALID
                ? Kind.CANCELLED : Kind.DROP;
    }

    void cancel()
    {
        armed = dragging = false;
        revision = 0;
        source = -1;
        downX = downY = pointerX = pointerY = 0;
    }

    boolean isArmed() { return armed; }
    boolean isDragging() { return armed && dragging; }
    int sourceKey() { return armed ? source : -1; }
    float pointerX() { return pointerX; }
    float pointerY() { return pointerY; }
}
