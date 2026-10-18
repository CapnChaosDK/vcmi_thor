package eu.vcmi.vcmi;

/** Shared Hero/Town lower-deck navigation geometry and revision-bound gesture. */
final class ThorWindowNavigation
{
    static final int NONE = -1;
    static final int PREVIOUS = 0;
    static final int NEXT = 1;
    static final int CLOSE = 2;

    private final ThorControlGesture gesture = new ThorControlGesture(NONE);

    static boolean isWindow(final String contextId)
    {
        return ThorContextIds.HERO_WINDOW.equals(contextId) || ThorContextIds.TOWN_WINDOW.equals(contextId);
    }

    static int actionFor(final int control)
    {
        switch (control)
        {
            case PREVIOUS: return ThorActionIds.WINDOW_PREVIOUS;
            case NEXT: return ThorActionIds.WINDOW_NEXT;
            case CLOSE: return ThorActionIds.WINDOW_CLOSE;
            default: return ThorActionIds.NONE;
        }
    }

    static float[] bounds(final int control, final float width, final float height)
    {
        if (control < PREVIOUS || control > CLOSE || width <= 0f || height <= 0f)
            return new float[]{0f, 0f, 0f, 0f};
        final float side = width * 0.055f;
        final float gap = width * 0.018f;
        final float cell = (width - 2f * side - 2f * gap) / 3f;
        final float left = side + control * (cell + gap);
        return new float[]{left, height * 0.895f, left + cell, height * 0.985f};
    }

    static int controlAt(final float x, final float y, final float width, final float height)
    {
        for (int candidate = PREVIOUS; candidate <= CLOSE; ++candidate)
        {
            final float[] box = bounds(candidate, width, height);
            if (x >= box[0] && x <= box[2] && y >= box[1] && y <= box[3])
                return candidate;
        }
        return NONE;
    }

    void begin(final String contextId, final int pressedControl, final long currentRevision,
               final long currentSession, final int pressedPointerId, final boolean enabled)
    {
        cancel();
        if (!isWindow(contextId) || actionFor(pressedControl) == ThorActionIds.NONE || !enabled)
            return;
        gesture.begin(contextId, pressedControl, currentRevision, currentSession, pressedPointerId, enabled);
    }

    boolean isActive() { return gesture.isActive(); }
    int pointerId() { return gesture.pointerId(); }
    int control() { return gesture.control(); }
    long revision() { return gesture.revision(); }

    boolean finish(final String currentContext, final int releasedControl, final long currentRevision,
                   final long currentSession, final int releasedPointerId, final int pointerCount,
                   final boolean enabled, final boolean sessionIsCurrent)
    {
        return gesture.finish(currentContext, releasedControl, pointerCount, releasedPointerId,
                currentRevision, currentSession, enabled, sessionIsCurrent);
    }

    void cancel()
    {
        gesture.cancel();
    }
}
