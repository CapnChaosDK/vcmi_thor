package eu.vcmi.vcmi;

/** Keeps every pointer in a lower-deck haptics toggle gesture away from game controls. */
final class ThorHapticsToggleGesture
{
    private boolean active;
    private boolean toggleCandidate;

    boolean begin(final boolean startedInsideToggle)
    {
        if (!startedInsideToggle)
            return false;
        active = true;
        toggleCandidate = true;
        return true;
    }

    boolean isActive()
    {
        return active;
    }

    void pointerAdded()
    {
        if (active)
            toggleCandidate = false;
    }

    boolean finish(final boolean endedInsideToggle)
    {
        if (!active)
            return false;
        final boolean shouldToggle = toggleCandidate && endedInsideToggle;
        cancel();
        return shouldToggle;
    }

    void cancel()
    {
        active = false;
        toggleCandidate = false;
    }
}
