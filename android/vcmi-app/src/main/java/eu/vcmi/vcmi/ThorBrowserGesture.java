package eu.vcmi.vcmi;

/** A page-row gesture bound to context, revision and the current Presentation. */
final class ThorBrowserGesture
{
    private final ThorControlGesture gesture = new ThorControlGesture(ThorBrowserState.CONTROL_NONE);
    private int target = ThorActionIds.NO_TARGET;

    void begin(final String context, final int pressedControl, final int pressedTarget,
               final long pressedRevision, final long pressedSession, final int pressedPointerId,
               final boolean enabled)
    {
        cancel();
        if (ThorBrowserState.actionForControl(context, pressedControl) == ThorActionIds.NONE)
            return;
        target = pressedTarget;
        gesture.begin(context, pressedControl, pressedRevision, pressedSession, pressedPointerId, enabled);
    }

    boolean isActive() { return gesture.isActive(); }
    int control() { return gesture.control(); }
    int target() { return target; }
    int pointerId() { return gesture.pointerId(); }
    long revision() { return gesture.revision(); }
    long session() { return gesture.session(); }

    boolean retainsLoadBackAfterBrowserUpdate(final String currentContext, final boolean backEnabled)
    {
        return gesture.isActive() && backEnabled && ThorContextIds.LOBBY_LOAD_GAME_SCENARIO.equals(gesture.context())
                && gesture.context().equals(currentContext) && gesture.control() == ThorBrowserState.CONTROL_BACK;
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
        // Only the existing Load Back caller may relax revision matching; identity and session stay exact.
        final boolean accepted = gesture.finish(releasedContext, releasedControl, pointerCount, releasedPointerId,
                allowRevisionChange ? gesture.revision() : currentRevision, currentSession,
                enabledAtUp && target == releasedTarget, sessionIsCurrent);
        cancel();
        return accepted;
    }

    void cancel()
    {
        gesture.cancel();
        target = ThorActionIds.NO_TARGET;
    }
}
