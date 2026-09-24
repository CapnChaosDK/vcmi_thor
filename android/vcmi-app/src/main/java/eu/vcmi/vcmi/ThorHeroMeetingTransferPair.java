package eu.vcmi.vcmi;

/**
 * Encodes two stable Hero Meeting slot keys into the bounded action-16 payload.
 * Keys 0..6 are the left army and 7..13 are the right army; the payload is
 * sourceKey * 14 + destinationKey. Same-side pairs are deliberately invalid.
 */
final class ThorHeroMeetingTransferPair
{
    static final int SLOT_KEY_COUNT = ThorHeroMeetingArmies.SLOT_COUNT;
    static final int SLOTS_PER_ARMY = SLOT_KEY_COUNT / 2;
    static final int INVALID = -1;

    static final class Pair
    {
        final int sourceKey;
        final int destinationKey;
        final boolean sourceIsLeft;
        final int sourceSlot;
        final boolean destinationIsLeft;
        final int destinationSlot;

        Pair(final int sourceKey, final int destinationKey)
        {
            this.sourceKey = sourceKey;
            this.destinationKey = destinationKey;
            sourceIsLeft = sourceKey < SLOTS_PER_ARMY;
            sourceSlot = sourceKey % SLOTS_PER_ARMY;
            destinationIsLeft = destinationKey < SLOTS_PER_ARMY;
            destinationSlot = destinationKey % SLOTS_PER_ARMY;
        }
    }

    private ThorHeroMeetingTransferPair()
    {
    }

    static int encode(final int sourceKey, final int destinationKey)
    {
        if (!isValidKey(sourceKey) || !isValidKey(destinationKey)
                || sourceKey / SLOTS_PER_ARMY == destinationKey / SLOTS_PER_ARMY)
            return INVALID;
        return sourceKey * SLOT_KEY_COUNT + destinationKey;
    }

    static Pair decode(final int encoded)
    {
        if (encoded < 0 || encoded >= SLOT_KEY_COUNT * SLOT_KEY_COUNT)
            return null;
        final int sourceKey = encoded / SLOT_KEY_COUNT;
        final int destinationKey = encoded % SLOT_KEY_COUNT;
        return encode(sourceKey, destinationKey) == encoded ? new Pair(sourceKey, destinationKey) : null;
    }

    private static boolean isValidKey(final int key)
    {
        return key >= 0 && key < SLOT_KEY_COUNT;
    }
}
