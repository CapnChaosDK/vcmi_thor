package eu.vcmi.vcmi;

final class ThorHeroMeetingArmies
{
    static final int SLOT_COUNT = 14;
    static final ThorHeroMeetingArmies EMPTY = new ThorHeroMeetingArmies(-1, -1, new String[0], new int[0],
            new int[0], new int[0], new String[0], new int[0], false);

    final int leftHeroId;
    final int rightHeroId;
    final String[] heroNames;
    final int[] armyIds;
    final int[] creatureIds;
    final int[] counts;
    final String[] creatureNames;
    final int[] flags;
    final boolean locallyControllable;

    private ThorHeroMeetingArmies(final int leftHeroId, final int rightHeroId, final String[] heroNames,
                                  final int[] armyIds, final int[] creatureIds, final int[] counts,
                                  final String[] creatureNames, final int[] flags, final boolean locallyControllable)
    {
        this.leftHeroId = leftHeroId;
        this.rightHeroId = rightHeroId;
        this.heroNames = heroNames;
        this.armyIds = armyIds;
        this.creatureIds = creatureIds;
        this.counts = counts;
        this.creatureNames = creatureNames;
        this.flags = flags;
        this.locallyControllable = locallyControllable;
    }

    static ThorHeroMeetingArmies copyOf(final int leftHeroId, final int rightHeroId, final String[] heroNames,
                                        final int[] armyIds, final int[] creatureIds, final int[] counts,
                                        final String[] creatureNames, final int[] flags, final int controllable)
    {
        if (leftHeroId < 0 || rightHeroId < 0 || leftHeroId == rightHeroId || heroNames == null || armyIds == null
                || creatureIds == null || counts == null || creatureNames == null || flags == null
                || heroNames.length != 2 || armyIds.length != 2 || armyIds[0] < 0 || armyIds[1] < 0
                || armyIds[0] == armyIds[1] || creatureIds.length != SLOT_COUNT || counts.length != SLOT_COUNT
                || creatureNames.length != SLOT_COUNT || flags.length != SLOT_COUNT)
            return EMPTY;
        for (int index = 0; index < SLOT_COUNT; ++index)
        {
            final boolean occupied = (flags[index] & 1) != 0;
            if (heroNames[index / 7] == null || creatureNames[index] == null || counts[index] < 0
                    || (occupied && (creatureIds[index] < 0 || counts[index] <= 0))
                    || (!occupied && (creatureIds[index] != -1 || counts[index] != 0)))
                return EMPTY;
        }
        return new ThorHeroMeetingArmies(leftHeroId, rightHeroId, heroNames.clone(), armyIds.clone(), creatureIds.clone(),
                counts.clone(), creatureNames.clone(), flags.clone(), controllable != 0);
    }

    boolean complete()
    {
        return creatureIds.length == SLOT_COUNT;
    }
}
