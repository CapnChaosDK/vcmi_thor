package eu.vcmi.vcmi;

/** Captures one revision- and presentation-bound tap on the New Game scenario deck. */
final class ThorLobbyScenarioGesture
{
    private final ThorControlGesture gesture = new ThorControlGesture(ThorLobbyScenarioState.CONTROL_NONE);

    void begin(final int pressedControl, final long pressedRevision, final long pressedSession,
               final int pressedPointerId, final boolean actionEnabled)
    {
        gesture.begin(null, pressedControl, pressedRevision, pressedSession, pressedPointerId, actionEnabled);
    }

    boolean isActive()
    {
        return gesture.isActive();
    }

    int control()
    {
        return gesture.control();
    }

    int pointerId()
    {
        return gesture.pointerId();
    }

    long revision()
    {
        return gesture.revision();
    }

    long session()
    {
        return gesture.session();
    }

    boolean finish(final int releasedControl, final int pointerCount, final int releasedPointerId,
                   final long currentRevision, final long currentSession, final boolean actionEnabledAtUp,
                   final boolean sessionIsCurrent)
    {
        return gesture.finish(null, releasedControl, pointerCount, releasedPointerId,
                currentRevision, currentSession,
                actionEnabledAtUp && gesture.control() != ThorLobbyScenarioState.CONTROL_NONE, sessionIsCurrent);
    }

    void cancel()
    {
        gesture.cancel();
    }
}
