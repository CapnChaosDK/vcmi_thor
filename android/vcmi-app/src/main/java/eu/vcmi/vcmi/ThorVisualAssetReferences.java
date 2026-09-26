package eu.vcmi.vcmi;

/** Checks that a decorative asset belongs to the currently accepted context revision. */
final class ThorVisualAssetReferences
{
    private long revision = -1L;
    private String contextId = ThorContextIds.UNKNOWN;
    private long heroPortraitAssetKey;

    void updateContext(final long revision, final String contextId, final long heroPortraitAssetKey)
    {
        this.revision = revision;
        this.contextId = contextId == null ? ThorContextIds.UNKNOWN : contextId;
        this.heroPortraitAssetKey = (ThorContextIds.ADVENTURE_MAP.equals(this.contextId)
                || ThorContextIds.HERO_WINDOW.equals(this.contextId))
                && ThorVisualAssetKey.isHeroPortrait(heroPortraitAssetKey) ? heroPortraitAssetKey : 0L;
    }

    boolean references(final long revision, final long key, final ThorHeroMeetingArmies armies,
                       final ThorHeroMeetingArtifacts artifacts)
    {
        if (revision != this.revision || !ThorVisualAssetKey.isValid(key))
            return false;
        if ((ThorContextIds.ADVENTURE_MAP.equals(contextId) || ThorContextIds.HERO_WINDOW.equals(contextId))
                && key == heroPortraitAssetKey)
            return true;
        if (!ThorContextIds.HERO_MEETING.equals(contextId))
            return false;
        if (armies != null)
        {
            for (final long referenced : armies.heroPortraitAssetKeys)
                if (referenced == key)
                    return true;
            for (final long referenced : armies.visualAssetKeys)
                if (referenced == key)
                    return true;
        }
        if (artifacts != null)
            for (final long referenced : artifacts.visualAssetKeys)
                if (referenced == key)
                    return true;
        return false;
    }
}
