package eu.vcmi.vcmi;

/** Bounded touch targets for the active Hero Window only. */
final class ThorHeroManagementPair
{
    static final int INVALID = -1;

    private ThorHeroManagementPair()
    {
    }

    static int encodeArmy(final int sourceSlot, final int destinationSlot)
    {
        if (sourceSlot < 0 || destinationSlot < 0 || sourceSlot >= ThorHeroManagement.ARMY_SIZE
                || destinationSlot >= ThorHeroManagement.ARMY_SIZE || sourceSlot == destinationSlot)
            return INVALID;
        return sourceSlot * ThorHeroManagement.ARMY_SIZE + destinationSlot;
    }

    static int encodeArtifact(final int sourceSlot, final int destinationSlot)
    {
        if (sourceSlot < 0 || destinationSlot < 0 || sourceSlot >= ThorHeroManagement.ARTIFACT_COUNT
                || destinationSlot >= ThorHeroManagement.ARTIFACT_COUNT || sourceSlot == destinationSlot)
            return INVALID;
        return sourceSlot * ThorHeroManagement.ARTIFACT_COUNT + destinationSlot;
    }
}
