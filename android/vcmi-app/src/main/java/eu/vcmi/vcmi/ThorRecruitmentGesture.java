package eu.vcmi.vcmi;

/** Captures one pointer's exact recruitment command and rejects changed contexts or sessions. */
final class ThorRecruitmentGesture
{
    static final int PAGE_PREVIOUS = -1;
    static final int PAGE_NEXT = -2;

    private boolean active;
    private long revision;
    private long sessionId;
    private int pointerId = -1;
    private int target = -1;
    private int operation;

    boolean begin(final long revision, final long sessionId, final int pointerId,
                  final int target, final int operation)
    {
        cancel();
        if (revision <= 0L || sessionId <= 0L || pointerId < 0 || operation == 0)
            return false;
        this.revision = revision;
        this.sessionId = sessionId;
        this.pointerId = pointerId;
        this.target = target;
        this.operation = operation;
        active = true;
        return true;
    }

    int finish(final long revision, final long sessionId, final int pointerId,
               final int target, final int operation)
    {
        if (!active || this.revision != revision || this.sessionId != sessionId
                || this.pointerId != pointerId || this.target != target || this.operation != operation)
        {
            cancel();
            return 0;
        }
        final int result = this.operation;
        cancel();
        return result;
    }

    boolean isActive()
    {
        return active;
    }

    int pointerId()
    {
        return pointerId;
    }

    void cancel()
    {
        active = false;
        revision = 0L;
        sessionId = 0L;
        pointerId = -1;
        target = -1;
        operation = 0;
    }
}
