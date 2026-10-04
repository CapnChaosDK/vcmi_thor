package eu.vcmi.vcmi;

/** A page-row gesture bound to context, revision and the current Presentation. */
final class ThorBrowserGesture
{
    private String contextId;
    private int control;
    private int target;
    private int pointerId;
    private long revision;
    private long session;
    private boolean enabledAtDown;
    private boolean active;

    void begin(final String context, final int pressedControl, final int pressedTarget,
               final long pressedRevision, final long pressedSession, final int pressedPointerId,
               final boolean enabled)
    {
        cancel();
        if (ThorBrowserState.actionForControl(context, pressedControl) == ThorActionIds.NONE)
            return;
        contextId = context;
        control = pressedControl;
        target = pressedTarget;
        revision = pressedRevision;
        session = pressedSession;
        pointerId = pressedPointerId;
        enabledAtDown = enabled;
        active = true;
    }

    boolean isActive() { return active; }
    int control() { return control; }
    int target() { return target; }
    int pointerId() { return pointerId; }
    long revision() { return revision; }
    long session() { return session; }

    boolean retainsLoadBackAfterBrowserUpdate(final String currentContext, final boolean backEnabled)
    {
        return active && backEnabled && ThorContextIds.LOBBY_LOAD_GAME_SCENARIO.equals(contextId)
                && contextId.equals(currentContext) && control == ThorBrowserState.CONTROL_BACK;
    }

    boolean retainsLoadBackAcrossContextUpdate(final String currentContext,
                                               final long previousMask, final long newMask,
                                               final boolean revisionChanged)
    {
        final long backBit = ThorActionIds.maskFor(ThorActionIds.LOBBY_BACK);
        return retainsLoadBackAfterBrowserUpdate(currentContext,
                (newMask & backBit) != 0L || (revisionChanged && (previousMask & backBit) != 0L));
    }

    boolean finish(final String releasedContext, final int releasedControl, final int releasedTarget,
                   final int pointerCount, final int releasedPointerId, final long currentRevision,
                   final long currentSession, final boolean enabledAtUp, final boolean sessionIsCurrent)
    {
        return finish(releasedContext, releasedControl, releasedTarget, pointerCount, releasedPointerId,
                currentRevision, currentSession, enabledAtUp, sessionIsCurrent, false);
    }

    boolean finish(final String releasedContext, final int releasedControl, final int releasedTarget,
                   final int pointerCount, final int releasedPointerId, final long currentRevision,
                   final long currentSession, final boolean enabledAtUp, final boolean sessionIsCurrent,
                   final boolean allowRevisionChange)
    {
        final boolean accepted = active && enabledAtDown && enabledAtUp && sessionIsCurrent
                && contextId.equals(releasedContext) && control == releasedControl && target == releasedTarget
                && pointerCount == 1 && pointerId == releasedPointerId
                && (allowRevisionChange || revision == currentRevision)
                && session > 0L && session == currentSession;
        cancel();
        return accepted;
    }

    void cancel()
    {
        active = false;
        enabledAtDown = false;
        contextId = null;
        control = ThorBrowserState.CONTROL_NONE;
        target = ThorActionIds.NO_TARGET;
        pointerId = -1;
        revision = 0L;
        session = 0L;
    }
}
