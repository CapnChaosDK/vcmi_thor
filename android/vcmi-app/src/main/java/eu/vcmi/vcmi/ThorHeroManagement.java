package eu.vcmi.vcmi;

final class ThorHeroManagement
{
    static final int ARMY_SIZE = 7;
    static final int ARTIFACT_COUNT = ThorHeroMeetingArtifacts.EQUIPPED_PER_HERO + 64;
    static final ThorHeroManagement EMPTY = new ThorHeroManagement(-1, "", false,
            new int[0], new int[0], new String[0], new long[0], new int[0], new int[0],
            new String[0], new long[0]);

    final int heroId;
    final String heroName;
    final boolean locallyControllable;
    final int[] creatureIds;
    final int[] counts;
    final String[] creatureNames;
    final long[] creatureVisualAssetKeys;
    final int[] artifactPositions;
    final int[] artifactFlags;
    final String[] artifactNames;
    final long[] artifactVisualAssetKeys;

    private ThorHeroManagement(final int heroId, final String heroName, final boolean locallyControllable,
                               final int[] creatureIds, final int[] counts, final String[] creatureNames,
                               final long[] creatureVisualAssetKeys, final int[] artifactPositions,
                               final int[] artifactFlags, final String[] artifactNames,
                               final long[] artifactVisualAssetKeys)
    {
        this.heroId = heroId;
        this.heroName = heroName;
        this.locallyControllable = locallyControllable;
        this.creatureIds = creatureIds;
        this.counts = counts;
        this.creatureNames = creatureNames;
        this.creatureVisualAssetKeys = creatureVisualAssetKeys;
        this.artifactPositions = artifactPositions;
        this.artifactFlags = artifactFlags;
        this.artifactNames = artifactNames;
        this.artifactVisualAssetKeys = artifactVisualAssetKeys;
    }

    static ThorHeroManagement copyOf(final int heroId, final String heroName, final boolean locallyControllable,
                                     final int[] creatureIds, final int[] counts, final String[] creatureNames,
                                     final long[] creatureVisualAssetKeys, final int[] artifactPositions,
                                     final int[] artifactFlags, final String[] artifactNames,
                                     final long[] artifactVisualAssetKeys)
    {
        if (heroId < 0 || heroName == null || creatureIds == null || counts == null || creatureNames == null
                || creatureIds.length != ARMY_SIZE || counts.length != ARMY_SIZE
                || creatureNames.length != ARMY_SIZE || artifactPositions == null || artifactFlags == null
                || artifactNames == null
                || artifactPositions.length < ThorHeroMeetingArtifacts.EQUIPPED_PER_HERO
                || artifactPositions.length > ARTIFACT_COUNT || artifactFlags.length != artifactPositions.length
                || artifactNames.length != artifactPositions.length)
            return EMPTY;
        final boolean hasCreatureKeys = ThorVisualAssetPayload.isBoundedKeyList(creatureVisualAssetKeys)
                && creatureVisualAssetKeys.length == ARMY_SIZE;
        final boolean hasArtifactKeys = visualKeysValid(artifactVisualAssetKeys, artifactPositions.length)
                && artifactVisualAssetKeys.length == artifactPositions.length;
        final long[] copiedCreatureKeys = new long[ARMY_SIZE];
        final long[] copiedArtifactKeys = new long[artifactPositions.length];
        for (int index = 0; index < ARMY_SIZE; ++index)
        {
            final boolean occupied = creatureIds[index] >= 0 && counts[index] > 0;
            if (creatureNames[index] == null || (occupied && counts[index] <= 0)
                    || (!occupied && (creatureIds[index] != -1 || counts[index] != 0)))
                return EMPTY;
            if (occupied && hasCreatureKeys && ThorVisualAssetKey.isCreature(creatureVisualAssetKeys[index])
                    && ThorVisualAssetKey.typeId(creatureVisualAssetKeys[index]) == creatureIds[index])
                copiedCreatureKeys[index] = creatureVisualAssetKeys[index];
        }
        for (int index = 0; index < artifactPositions.length; ++index)
        {
            final boolean occupied = (artifactFlags[index] & 1) != 0;
            final boolean backpack = (artifactFlags[index] & 4) != 0;
            final int expectedPosition = index;
            if (artifactPositions[index] != expectedPosition || (artifactFlags[index] & ~7) != 0
                    || (index < ThorHeroMeetingArtifacts.EQUIPPED_PER_HERO && backpack)
                    || (index >= ThorHeroMeetingArtifacts.EQUIPPED_PER_HERO && !backpack)
                    || artifactNames[index] == null || (!occupied && !artifactNames[index].isEmpty()))
                return EMPTY;
            if (occupied && hasArtifactKeys && ThorVisualAssetKey.isArtifact(artifactVisualAssetKeys[index]))
                copiedArtifactKeys[index] = artifactVisualAssetKeys[index];
        }
        return new ThorHeroManagement(heroId, ThorContextDetails.orEmpty(heroName), locallyControllable,
                creatureIds.clone(), counts.clone(), creatureNames.clone(), copiedCreatureKeys,
                artifactPositions.clone(), artifactFlags.clone(), artifactNames.clone(), copiedArtifactKeys);
    }

    boolean complete()
    {
        return creatureIds.length == ARMY_SIZE
                && artifactPositions.length >= ThorHeroMeetingArtifacts.EQUIPPED_PER_HERO;
    }

    private static boolean visualKeysValid(final long[] keys, final int expectedLength)
    {
        if (keys == null || keys.length != expectedLength || keys.length > ARTIFACT_COUNT)
            return false;
        for (final long key : keys)
            if (key != 0L && !ThorVisualAssetKey.isValid(key))
                return false;
        return true;
    }
}
