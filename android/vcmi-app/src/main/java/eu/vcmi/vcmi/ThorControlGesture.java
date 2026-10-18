package eu.vcmi.vcmi;

/** One-shot tap identity shared by deck owners; callers retain semantic control validation. */
final class ThorControlGesture
{
    private final int noControl;
    private String context;
    private int control;
    private int pointerId = -1;
    private long revision;
    private long session;
    private boolean enabledAtDown;
    private boolean active;

    ThorControlGesture(final int noControl)
    {
        this.noControl = noControl;
        control = noControl;
    }

    void begin(final String context, final int control, final long revision, final long session,
               final int pointerId, final boolean enabled)
    {
        this.context = context;
        this.control = control;
        this.revision = revision;
        this.session = session;
        this.pointerId = pointerId;
        enabledAtDown = enabled;
        active = true;
    }

    boolean isActive() { return active; }
    String context() { return context; }
    int control() { return control; }
    int pointerId() { return pointerId; }
    long revision() { return revision; }
    long session() { return session; }

    boolean finish(final String currentContext, final int releasedControl, final int pointerCount,
                   final int releasedPointerId, final long currentRevision, final long currentSession,
                   final boolean enabledAtUp, final boolean sessionIsCurrent)
    {
        final boolean accepted = active && enabledAtDown && enabledAtUp && sessionIsCurrent
                && java.util.Objects.equals(context, currentContext) && control == releasedControl
                && pointerCount == 1 && pointerId == releasedPointerId
                && revision == currentRevision && session > 0L && session == currentSession;
        cancel();
        return accepted;
    }

    void cancel()
    {
        active = false;
        enabledAtDown = false;
        context = null;
        control = noControl;
        pointerId = -1;
        revision = 0L;
        session = 0L;
    }
}
