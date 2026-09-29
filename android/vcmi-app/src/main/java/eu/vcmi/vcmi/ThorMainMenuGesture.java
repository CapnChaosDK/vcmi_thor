package eu.vcmi.vcmi;

/** A single enabled choice pressed and released on the same presentation snapshot. */
final class ThorMainMenuGesture
{
    private int control;
    private int pointerId = -1;
    private long revision;
    private long session;
    private String contextId;
    private boolean enabledAtDown;
    private boolean active;

    void begin(final String pressedContext, final int pressedControl, final long pressedRevision,
               final long pressedSession, final int pressedPointerId, final boolean enabled)
    {
        cancel();
        if (ThorMainMenuState.actionForControl(pressedContext, pressedControl) == ThorActionIds.NONE)
            return;
        contextId = pressedContext;
        control = pressedControl;
        revision = pressedRevision;
        session = pressedSession;
        pointerId = pressedPointerId;
        enabledAtDown = enabled;
        active = true;
    }

    boolean isActive()
    {
        return active;
    }

    int pointerId()
    {
        return pointerId;
    }

    long revision()
    {
        return revision;
    }

    int control()
    {
        return control;
    }

    long session()
    {
        return session;
    }

    boolean finish(final String releasedContext, final int releasedControl,
                   final int pointerCount, final int releasedPointerId, final long currentRevision,
                   final long currentSession, final boolean enabledAtUp, final boolean sessionIsCurrent)
    {
        final boolean accepted = active && enabledAtDown && enabledAtUp && sessionIsCurrent
                && contextId.equals(releasedContext) && control == releasedControl
                && pointerCount == 1 && pointerId == releasedPointerId
                && revision == currentRevision && session > 0L && session == currentSession;
        cancel();
        return accepted;
    }

    void cancel()
    {
        active = false;
        enabledAtDown = false;
        control = ThorMainMenuState.CONTROL_NONE;
        pointerId = -1;
        revision = 0L;
        session = 0L;
        contextId = null;
    }
}
