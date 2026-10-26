package eu.vcmi.vcmi;

/** One-shot market taps reject held, dragged, stale and multi-pointer input. */
final class ThorMarketGesture
{
    private final ThorControlGesture tap = new ThorControlGesture(0);
    private float x, y;
    private long time;
    void cancel() { tap.cancel(); }
    void begin(final int control, final long revision, final long session, final int pointer,
               final boolean enabled, final float x, final float y, final long time)
    {
        this.x = x; this.y = y; this.time = time;
        tap.begin(ThorContextIds.RESOURCE_MARKET, control, revision, session, pointer, enabled);
    }
    void move(final float x, final float y, final long time, final float slop)
    {
        if (Math.abs(x - this.x) > slop || Math.abs(y - this.y) > slop || time - this.time > 700)
            cancel();
    }
    boolean finish(final int control, final long revision, final long session, final int pointerCount,
                   final int pointer, final boolean enabled, final boolean currentSession,
                   final float x, final float y, final long time, final float slop)
    {
        move(x, y, time, slop);
        return tap.finish(ThorContextIds.RESOURCE_MARKET, control, pointerCount, pointer,
                revision, session, enabled, currentSession);
    }
}
