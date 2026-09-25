package eu.vcmi.vcmi;

final class ThorHeroMeetingModeState
{
    static final int ARMY = 0;
    static final int ARTIFACTS = 1;
    private long revision = -1;
    private int mode = ARMY;

    int update(final long nextRevision, final boolean heroMeeting)
    {
        if (!heroMeeting || revision != nextRevision)
            mode = ARMY;
        revision = heroMeeting ? nextRevision : -1;
        return mode;
    }

    int select(final long currentRevision, final int requested)
    {
        if (revision == currentRevision && (requested == ARMY || requested == ARTIFACTS))
            mode = requested;
        return mode;
    }

    int mode()
    {
        return mode;
    }
}
