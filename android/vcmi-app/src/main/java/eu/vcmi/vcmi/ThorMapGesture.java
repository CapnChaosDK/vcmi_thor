package eu.vcmi.vcmi;

/** Release-only map/control gesture, bound to the same snapshot, session, level and local mode. */
final class ThorMapGesture
{
    private boolean active, moved;
    private long revision, session;
    private int pointer, level, mode, control;
    float downX, downY;
    ThorMapTransform transform;

    void begin(final long revision, final long session, final int pointer, final int level,
            final int mode, final int control, final float x, final float y, final ThorMapTransform transform)
    {
        cancel();
        if (revision <= 0 || pointer < 0 || (control < 0 && transform.tileAt(x, y) < 0))
            return;
        active = true;
        this.revision = revision;
        this.session = session;
        this.pointer = pointer;
        this.level = level;
        this.mode = mode;
        this.control = control;
        downX = x;
        downY = y;
        this.transform = transform;
    }

    boolean matches(final long revision, final long session, final int pointer, final int count,
            final int level, final int mode, final boolean validSession)
    {
        final boolean matches = active && this.revision == revision && this.session == session && this.pointer == pointer
                && count == 1 && this.level == level && this.mode == mode && validSession;
        if (!matches)
            cancel();
        return matches;
    }

    void move(final float x, final float y, final float slop)
    {
        moved |= (x - downX) * (x - downX) + (y - downY) * (y - downY) > slop * slop;
        if (control < 0 && transform.tileAt(x, y) < 0)
            cancel();
    }

    static final class Release
    {
        static final Release NONE = new Release(ThorActionIds.NONE, ThorActionIds.NO_TARGET, -1);
        final int action, target, localControl;
        Release(final int action, final int target, final int localControl)
        {
            this.action = action;
            this.target = target;
            this.localControl = localControl;
        }
    }

    /** Consumes the gesture once and returns at most one native request. */
    Release release(final long revision, final long session, final int pointer, final int count,
            final int level, final int mode, final boolean validSession, final float x, final float y,
            final int releaseControl, final ThorAdventureMap map, final boolean pan,
            final long enabledActions, final float markerRadius, final float slop)
    {
        if (!matches(revision, session, pointer, count, level, mode, validSession))
            return Release.NONE;
        move(x, y, slop);
        if (!active)
            return Release.NONE;
        final int pressedControl = control;
        final boolean dragged = moved;
        final ThorMapTransform mapping = transform;
        cancel();
        if (pressedControl >= 0)
        {
            if (dragged || pressedControl != releaseControl)
                return Release.NONE;
            if (pressedControl >= 10 && pressedControl <= 13)
                return new Release(ThorActionIds.NONE, ThorActionIds.NO_TARGET, pressedControl);
            if (!map.valid())
                return Release.NONE;
            if (pressedControl == 7 || pressedControl >= 0 && pressedControl < 5)
                return new Release(ThorActionIds.NONE, ThorActionIds.NO_TARGET, pressedControl);
            if (pressedControl >= 5 && pressedControl <= 6 && map.levels == 2
                    && map.level != pressedControl - 5
                    && (enabledActions & ThorActionIds.maskFor(ThorActionIds.ADVENTURE_SET_MAP_LEVEL)) != 0)
                return new Release(ThorActionIds.ADVENTURE_SET_MAP_LEVEL, pressedControl - 5, -1);
            return Release.NONE;
        }
        if (!map.valid() || (pan && dragged))
            return Release.NONE;
        final int tile = mapping.tileAt(x, y);
        if (tile < 0)
            return Release.NONE;
        final int marker = !dragged ? map.markerAt(mapping, x, y, markerRadius) : -1;
        if (marker >= 0 && (enabledActions & ThorActionIds.maskFor(map.markers[marker])) != 0)
            return new Release(map.markers[marker], map.markers[marker + 1], -1);
        if ((enabledActions & ThorActionIds.maskFor(ThorActionIds.ADVENTURE_CENTER_VIEW)) != 0)
            return new Release(ThorActionIds.ADVENTURE_CENTER_VIEW, map.level * ThorAdventureMap.MAX_TILES + tile, -1);
        return Release.NONE;
    }

    /** A held map release is inspection only, including empty/ambiguous hits. */
    boolean inspect(final long revision, final long session, final int pointer, final int count,
            final int level, final int mode, final boolean validSession, final float x, final float y,
            final float slop, final long heldMillis)
    {
        if (!matches(revision, session, pointer, count, level, mode, validSession)) return false;
        move(x, y, slop);
        if (!active || moved || control >= 0 || heldMillis < 500) return false;
        cancel();
        return true;
    }

    boolean active() { return active; }
    boolean moved() { return moved; }
    int control() { return control; }
    void cancel() { active = false; moved = false; transform = null; }
}
