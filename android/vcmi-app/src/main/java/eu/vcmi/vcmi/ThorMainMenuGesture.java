package eu.vcmi.vcmi;

/** A single enabled choice pressed and released on the same presentation snapshot. */
final class ThorMainMenuGesture
{
    private final ThorControlGesture gesture = new ThorControlGesture(ThorMainMenuState.CONTROL_NONE);

    void begin(final String pressedContext, final int pressedControl, final long pressedRevision,
               final long pressedSession, final int pressedPointerId, final boolean enabled)
    {
        cancel();
        if (ThorMainMenuState.actionForControl(pressedContext, pressedControl) == ThorActionIds.NONE)
            return;
        gesture.begin(pressedContext, pressedControl, pressedRevision, pressedSession, pressedPointerId, enabled);
    }

    boolean isActive() { return gesture.isActive(); }

    int pointerId() { return gesture.pointerId(); }

    long revision() { return gesture.revision(); }

    int control() { return gesture.control(); }

    long session() { return gesture.session(); }

    boolean finish(final String releasedContext, final int releasedControl,
                   final int pointerCount, final int releasedPointerId, final long currentRevision,
                   final long currentSession, final boolean enabledAtUp, final boolean sessionIsCurrent)
    {
        return gesture.finish(releasedContext, releasedControl, pointerCount, releasedPointerId,
                currentRevision, currentSession, enabledAtUp, sessionIsCurrent);
    }

    void cancel()
    {
        gesture.cancel();
    }
}
