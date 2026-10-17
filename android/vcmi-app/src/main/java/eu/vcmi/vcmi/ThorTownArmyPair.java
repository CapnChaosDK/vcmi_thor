package eu.vcmi.vcmi;

/** Stable bounded encoding for source/destination slots in the two Town armies. */
final class ThorTownArmyPair
{
    private ThorTownArmyPair() { }

    static int encode(final boolean sourceIsVisiting, final int sourceSlot,
                      final boolean destinationIsVisiting, final int destinationSlot)
    {
        if (sourceSlot < 0 || sourceSlot >= ThorTownManagement.ARMY_SIZE
                || destinationSlot < 0 || destinationSlot >= ThorTownManagement.ARMY_SIZE
                || (sourceIsVisiting == destinationIsVisiting && sourceSlot == destinationSlot))
            return -1;
        final int sourceKey = (sourceIsVisiting ? ThorTownManagement.ARMY_SIZE : 0) + sourceSlot;
        final int destinationKey = (destinationIsVisiting ? ThorTownManagement.ARMY_SIZE : 0) + destinationSlot;
        return sourceKey * ThorTownManagement.ARMY_SIZE * 2 + destinationKey;
    }
}
