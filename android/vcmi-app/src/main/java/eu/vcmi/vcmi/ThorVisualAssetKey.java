package eu.vcmi.vcmi;

/** Stable type-specific key shared by native and Android visual-asset caches. */
final class ThorVisualAssetKey
{
    static final int CREATURE_KIND = 1;
    static final int ARTIFACT_KIND = 2;
    static final int HERO_KIND = 3;
    static final int MAX_VISUAL_KEYS = 64;

    private ThorVisualAssetKey()
    {
    }

    static long forCreature(final int creatureId)
    {
        return encode(CREATURE_KIND, creatureId);
    }

    static long forArtifactType(final int artifactTypeId)
    {
        return encode(ARTIFACT_KIND, artifactTypeId);
    }

    static long forHeroPortrait(final int portraitSourceHeroTypeId)
    {
        return encode(HERO_KIND, portraitSourceHeroTypeId);
    }

    private static long encode(final int kind, final int typeId)
    {
        return typeId < 0 ? 0L : (((long) kind) << 56) | ((long) typeId + 1L);
    }

    static boolean isValid(final long key)
    {
        final int kind = (int) (key >>> 56);
        final long typeIdPlusOne = key & 0xffffffffL;
        final long reserved = (key >>> 32) & 0xffffffL;
        return (kind == CREATURE_KIND || kind == ARTIFACT_KIND || kind == HERO_KIND)
                && typeIdPlusOne > 0 && typeIdPlusOne <= ((long) Integer.MAX_VALUE + 1L) && reserved == 0;
    }

    static boolean isCreature(final long key)
    {
        return isValid(key) && (int) (key >>> 56) == CREATURE_KIND;
    }

    static boolean isArtifact(final long key)
    {
        return isValid(key) && (int) (key >>> 56) == ARTIFACT_KIND;
    }

    static boolean isHeroPortrait(final long key)
    {
        return isValid(key) && (int) (key >>> 56) == HERO_KIND;
    }

    static int typeId(final long key)
    {
        return isValid(key) ? (int) ((key & 0xffffffffL) - 1L) : -1;
    }
}
