package eu.vcmi.vcmi;

/** Fixed Town service targets, geometry, and revision-bound lower-deck gesture. */
final class ThorTownServices
{
    static final int NONE = -1;
    static final int HALL = 0;
    static final int RECRUIT = 1;
    static final int TAVERN = 2;
    static final int MAGE_GUILD = 3;
    static final int MARKETPLACE = 4;
    static final int BLACKSMITH = 5;
    static final int SERVICE_COUNT = 6;

    private final ThorControlGesture gesture = new ThorControlGesture(NONE);

    static float[] bounds(final int service, final float width, final float height)
    {
        if (service < 0 || service >= SERVICE_COUNT || width <= 0f || height <= 0f)
            return new float[]{0f, 0f, 0f, 0f};
        final float side = width * 0.055f;
        final float gap = width * 0.018f;
        final float cell = (width - 2f * side - gap) / 2f;
        final int row = service / 2;
        final int column = service % 2;
        final float top = height * (0.585f + row * 0.098f);
        final float left = side + column * (cell + gap);
        return new float[]{left, top, left + cell, top + height * 0.082f};
    }

    static int serviceAt(final float x, final float y, final float width, final float height)
    {
        for (int candidate = 0; candidate < SERVICE_COUNT; ++candidate)
        {
            final float[] box = bounds(candidate, width, height);
            if (x >= box[0] && x <= box[2] && y >= box[1] && y <= box[3])
                return candidate;
        }
        return NONE;
    }

    void begin(final int pressedService, final long currentRevision, final long currentSession,
               final int pressedPointerId, final boolean enabled)
    {
        cancel();
        if (pressedService < 0 || pressedService >= SERVICE_COUNT || !enabled)
            return;
        gesture.begin(null, pressedService, currentRevision, currentSession, pressedPointerId, enabled);
    }

    boolean isActive() { return gesture.isActive(); }
    int service() { return gesture.control(); }
    int pointerId() { return gesture.pointerId(); }
    long revision() { return gesture.revision(); }

    boolean finish(final int releasedService, final long currentRevision, final long currentSession,
                   final int releasedPointerId, final int pointerCount, final boolean enabled,
                   final boolean sessionIsCurrent)
    {
        return gesture.finish(null, releasedService, pointerCount, releasedPointerId,
                currentRevision, currentSession, enabled, sessionIsCurrent);
    }

    void cancel()
    {
        gesture.cancel();
    }
}
