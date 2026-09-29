package eu.vcmi.vcmi;

/** Captures one revision- and presentation-bound tap on the New Game scenario deck. */
final class ThorLobbyScenarioGesture
{
    private boolean active;
    private boolean actionEnabledAtDown;
    private int control = ThorLobbyScenarioState.CONTROL_NONE;
    private int pointerId = -1;
    private long revision;
    private long session;

    void begin(final int pressedControl, final long pressedRevision, final long pressedSession,
               final int pressedPointerId, final boolean actionEnabled)
    {
        active = true;
        control = pressedControl;
        revision = pressedRevision;
        session = pressedSession;
        pointerId = pressedPointerId;
        actionEnabledAtDown = actionEnabled;
    }

    boolean isActive()
    {
        return active;
    }

    int control()
    {
        return control;
    }

    int pointerId()
    {
        return pointerId;
    }

    long revision()
    {
        return revision;
    }

    long session()
    {
        return session;
    }

    boolean finish(final int releasedControl, final int pointerCount, final int releasedPointerId,
                   final long currentRevision, final long currentSession, final boolean actionEnabledAtUp,
                   final boolean sessionIsCurrent)
    {
        final boolean accepted = active && control != ThorLobbyScenarioState.CONTROL_NONE
                && control == releasedControl && pointerCount == 1 && pointerId == releasedPointerId
                && revision == currentRevision && session > 0L && session == currentSession
                && actionEnabledAtDown && actionEnabledAtUp && sessionIsCurrent;
        cancel();
        return accepted;
    }

    void cancel()
    {
        active = false;
        actionEnabledAtDown = false;
        control = ThorLobbyScenarioState.CONTROL_NONE;
        pointerId = -1;
        revision = 0L;
        session = 0L;
    }
}
