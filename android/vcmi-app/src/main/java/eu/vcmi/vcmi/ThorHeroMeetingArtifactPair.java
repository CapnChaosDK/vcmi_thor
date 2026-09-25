package eu.vcmi.vcmi;

final class ThorHeroMeetingArtifactPair
{
    static final int INVALID = -1;

    private ThorHeroMeetingArtifactPair()
    {
    }

    static int encode(final int source, final int destination)
    {
        if (source < 0 || destination < 0 || source >= ThorHeroMeetingArtifacts.SLOT_COUNT
                || destination >= ThorHeroMeetingArtifacts.SLOT_COUNT
                || source / ThorHeroMeetingArtifacts.PER_HERO == destination / ThorHeroMeetingArtifacts.PER_HERO)
            return INVALID;
        return source * ThorHeroMeetingArtifacts.SLOT_COUNT + destination;
    }
}
