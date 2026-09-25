package eu.vcmi.vcmi;

final class ThorHeroMeetingArtifacts
{
    static final int EQUIPPED_PER_HERO = 19;
    static final int BACKPACK_PER_HERO = 5;
    static final int PER_HERO = EQUIPPED_PER_HERO + BACKPACK_PER_HERO;
    static final int SLOT_COUNT = PER_HERO * 2;
    static final ThorHeroMeetingArtifacts EMPTY = new ThorHeroMeetingArtifacts(-1, -1, new String[0],
            new int[0], new int[0], new String[0]);

    final int leftHeroId;
    final int rightHeroId;
    final String[] heroNames;
    final int[] positions;
    final int[] flags;
    final String[] names;

    private ThorHeroMeetingArtifacts(final int leftHeroId, final int rightHeroId, final String[] heroNames,
                                     final int[] positions, final int[] flags, final String[] names)
    {
        this.leftHeroId = leftHeroId;
        this.rightHeroId = rightHeroId;
        this.heroNames = heroNames;
        this.positions = positions;
        this.flags = flags;
        this.names = names;
    }

    static ThorHeroMeetingArtifacts copyOf(final int leftHeroId, final int rightHeroId, final String[] heroNames,
                                            final int[] positions, final int[] flags, final String[] names)
    {
        if (leftHeroId < 0 || rightHeroId < 0 || leftHeroId == rightHeroId || heroNames == null
                || positions == null || flags == null || names == null || heroNames.length != 2
                || positions.length != SLOT_COUNT || flags.length != SLOT_COUNT || names.length != SLOT_COUNT
                || heroNames[0] == null || heroNames[1] == null)
            return EMPTY;
        for (int index = 0; index < SLOT_COUNT; ++index)
        {
            final boolean occupied = (flags[index] & 1) != 0;
            final boolean backpack = (flags[index] & 4) != 0;
            final int offset = index % PER_HERO;
            if (positions[index] != offset || names[index] == null || (flags[index] & ~7) != 0
                    || backpack != (offset >= EQUIPPED_PER_HERO)
                    || (!occupied && !names[index].isEmpty()))
                return EMPTY;
        }
        return new ThorHeroMeetingArtifacts(leftHeroId, rightHeroId, heroNames.clone(), positions.clone(),
                flags.clone(), names.clone());
    }

    boolean complete()
    {
        return positions.length == SLOT_COUNT;
    }
}
