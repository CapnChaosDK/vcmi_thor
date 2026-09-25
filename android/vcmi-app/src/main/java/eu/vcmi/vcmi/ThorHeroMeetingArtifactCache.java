package eu.vcmi.vcmi;

/** Activity-owned artifact cache used to restore a recreated lower presentation. */
final class ThorHeroMeetingArtifactCache
{
    private long revision;
    private boolean heroMeeting;
    private ThorHeroMeetingArtifacts artifacts = ThorHeroMeetingArtifacts.EMPTY;

    void reset(final long revision, final String contextId)
    {
        this.revision = revision;
        heroMeeting = ThorContextIds.HERO_MEETING.equals(contextId);
        artifacts = ThorHeroMeetingArtifacts.EMPTY;
    }

    boolean accept(final long revision, final ThorHeroMeetingArtifacts candidate)
    {
        if (!heroMeeting || revision != this.revision || candidate == null || !candidate.complete())
            return false;
        artifacts = candidate;
        return true;
    }

    ThorHeroMeetingArtifacts snapshot()
    {
        return artifacts;
    }
}
