package eu.vcmi.vcmi;

final class ThorHeroMeetingArmies
{
    static final int SLOT_COUNT = 14;
    static final ThorHeroMeetingArmies EMPTY = new ThorHeroMeetingArmies(-1, -1, new String[0], new int[0],
            new int[0], new int[0], new String[0], new int[0], new long[0], new long[0], false);

    final int leftHeroId;
    final int rightHeroId;
    final String[] heroNames;
    final int[] armyIds;
    final int[] creatureIds;
    final int[] counts;
    final String[] creatureNames;
    final int[] flags;
    final long[] visualAssetKeys;
    final long[] heroPortraitAssetKeys;
    final boolean locallyControllable;

    private ThorHeroMeetingArmies(final int leftHeroId, final int rightHeroId, final String[] heroNames,
                                  final int[] armyIds, final int[] creatureIds, final int[] counts,
                                  final String[] creatureNames, final int[] flags, final long[] visualAssetKeys,
                                  final long[] heroPortraitAssetKeys, final boolean locallyControllable)
    {
        this.leftHeroId = leftHeroId;
        this.rightHeroId = rightHeroId;
        this.heroNames = heroNames;
        this.armyIds = armyIds;
        this.creatureIds = creatureIds;
        this.counts = counts;
        this.creatureNames = creatureNames;
        this.flags = flags;
        this.visualAssetKeys = visualAssetKeys;
        this.heroPortraitAssetKeys = heroPortraitAssetKeys;
        this.locallyControllable = locallyControllable;
    }

    static ThorHeroMeetingArmies copyOf(final int leftHeroId, final int rightHeroId, final String[] heroNames,
                                        final int[] armyIds, final int[] creatureIds, final int[] counts,
                                        final String[] creatureNames, final int[] flags, final int controllable)
    {
        return copyOf(leftHeroId, rightHeroId, heroNames, armyIds, creatureIds, counts, creatureNames, flags,
                controllable, null, null);
    }

    static ThorHeroMeetingArmies copyOf(final int leftHeroId, final int rightHeroId, final String[] heroNames,
                                        final int[] armyIds, final int[] creatureIds, final int[] counts,
                                        final String[] creatureNames, final int[] flags, final int controllable,
                                        final long[] visualAssetKeys)
    {
        return copyOf(leftHeroId, rightHeroId, heroNames, armyIds, creatureIds, counts, creatureNames, flags,
                controllable, visualAssetKeys, null);
    }

    static ThorHeroMeetingArmies copyOf(final int leftHeroId, final int rightHeroId, final String[] heroNames,
                                        final int[] armyIds, final int[] creatureIds, final int[] counts,
                                        final String[] creatureNames, final int[] flags, final int controllable,
                                        final long[] visualAssetKeys, final long[] heroPortraitAssetKeys)
    {
        if (leftHeroId < 0 || rightHeroId < 0 || leftHeroId == rightHeroId || heroNames == null || armyIds == null
                || creatureIds == null || counts == null || creatureNames == null || flags == null
                || heroNames.length != 2 || armyIds.length != 2 || armyIds[0] < 0 || armyIds[1] < 0
                || armyIds[0] == armyIds[1] || creatureIds.length != SLOT_COUNT || counts.length != SLOT_COUNT
                || creatureNames.length != SLOT_COUNT || flags.length != SLOT_COUNT)
            return EMPTY;
        final long[] copiedAssetKeys = new long[SLOT_COUNT];
        final boolean hasAssetKeys = ThorVisualAssetPayload.isBoundedKeyList(visualAssetKeys)
                && visualAssetKeys.length == SLOT_COUNT;
        final long[] copiedHeroPortraitAssetKeys = new long[2];
        final boolean hasHeroPortraitAssetKeys = ThorVisualAssetPayload.isBoundedKeyList(heroPortraitAssetKeys)
                && heroPortraitAssetKeys.length == copiedHeroPortraitAssetKeys.length;
        for (int index = 0; index < SLOT_COUNT; ++index)
        {
            final boolean occupied = (flags[index] & 1) != 0;
            if (heroNames[index / 7] == null || creatureNames[index] == null || counts[index] < 0
                    || (occupied && (creatureIds[index] < 0 || counts[index] <= 0))
                    || (!occupied && (creatureIds[index] != -1 || counts[index] != 0)))
                return EMPTY;
            if (occupied && hasAssetKeys && ThorVisualAssetKey.isCreature(visualAssetKeys[index])
                    && ThorVisualAssetKey.typeId(visualAssetKeys[index]) == creatureIds[index])
                copiedAssetKeys[index] = visualAssetKeys[index];
        }
        if (hasHeroPortraitAssetKeys)
            for (int side = 0; side < copiedHeroPortraitAssetKeys.length; ++side)
                if (ThorVisualAssetKey.isHeroPortrait(heroPortraitAssetKeys[side]))
                    copiedHeroPortraitAssetKeys[side] = heroPortraitAssetKeys[side];
        return new ThorHeroMeetingArmies(leftHeroId, rightHeroId, heroNames.clone(), armyIds.clone(), creatureIds.clone(),
                counts.clone(), creatureNames.clone(), flags.clone(), copiedAssetKeys, copiedHeroPortraitAssetKeys,
                controllable != 0);
    }

    boolean complete()
    {
        return creatureIds.length == SLOT_COUNT;
    }
}
