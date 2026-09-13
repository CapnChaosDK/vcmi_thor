package eu.vcmi.vcmi;

final class ThorDisplaySelector
{
    static final int NO_DISPLAY = -1;

    static final class Candidate
    {
        final int id;
        final boolean active;

        Candidate(final int id, final boolean active)
        {
            this.id = id;
            this.active = active;
        }
    }

    private ThorDisplaySelector()
    {
    }

    static int select(final int activityDisplayId, final Candidate[] presentationDisplays, final Candidate[] allDisplays)
    {
        final int preferred = selectFrom(activityDisplayId, presentationDisplays);
        return preferred != NO_DISPLAY ? preferred : selectFrom(activityDisplayId, allDisplays);
    }

    private static int selectFrom(final int activityDisplayId, final Candidate[] candidates)
    {
        if (candidates == null)
            return NO_DISPLAY;

        int selectedId = NO_DISPLAY;
        for (final Candidate candidate : candidates)
        {
            if (candidate == null || !candidate.active || candidate.id == activityDisplayId)
                continue;

            if (selectedId == NO_DISPLAY || candidate.id < selectedId)
                selectedId = candidate.id;
        }
        return selectedId;
    }
}
