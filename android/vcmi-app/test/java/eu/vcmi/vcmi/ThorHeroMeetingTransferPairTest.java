package eu.vcmi.vcmi;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotEquals;

public class ThorHeroMeetingTransferPairTest
{
    @Test
    public void encodesAndDecodesEveryCrossArmyPair()
    {
        for (int source = 0; source < ThorHeroMeetingTransferPair.SLOT_KEY_COUNT; ++source)
        {
            for (int destination = 0; destination < ThorHeroMeetingTransferPair.SLOT_KEY_COUNT; ++destination)
            {
                final int encoded = ThorHeroMeetingTransferPair.encode(source, destination);
                if (source / 7 == destination / 7)
                {
                    assertEquals(ThorHeroMeetingTransferPair.INVALID, encoded);
                    continue;
                }
                assertEquals(source * 14 + destination, encoded);
                final ThorHeroMeetingTransferPair.Pair decoded = ThorHeroMeetingTransferPair.decode(encoded);
                assertEquals(source, decoded.sourceKey);
                assertEquals(destination, decoded.destinationKey);
                assertEquals(source < ThorHeroMeetingTransferPair.SLOTS_PER_ARMY, decoded.sourceIsLeft);
                assertEquals(source % ThorHeroMeetingTransferPair.SLOTS_PER_ARMY, decoded.sourceSlot);
                assertEquals(destination < ThorHeroMeetingTransferPair.SLOTS_PER_ARMY, decoded.destinationIsLeft);
                assertEquals(destination % ThorHeroMeetingTransferPair.SLOTS_PER_ARMY, decoded.destinationSlot);
            }
        }
    }

    @Test
    public void rejectsMalformedOverflowSameSlotAndSameSidePairs()
    {
        assertEquals(ThorHeroMeetingTransferPair.INVALID, ThorHeroMeetingTransferPair.encode(-1, 7));
        assertEquals(ThorHeroMeetingTransferPair.INVALID, ThorHeroMeetingTransferPair.encode(14, 0));
        assertEquals(ThorHeroMeetingTransferPair.INVALID, ThorHeroMeetingTransferPair.encode(3, 3));
        assertEquals(ThorHeroMeetingTransferPair.INVALID, ThorHeroMeetingTransferPair.encode(0, 6));
        assertEquals(ThorHeroMeetingTransferPair.INVALID, ThorHeroMeetingTransferPair.encode(7, 7));
        assertNotEquals(ThorHeroMeetingTransferPair.INVALID, ThorHeroMeetingTransferPair.encode(0, 7));
        assertNull(ThorHeroMeetingTransferPair.decode(-1));
        assertNull(ThorHeroMeetingTransferPair.decode(196));
        assertNull(ThorHeroMeetingTransferPair.decode(Integer.MAX_VALUE));
        assertNull(ThorHeroMeetingTransferPair.decode(3 * 14 + 3));
        assertNull(ThorHeroMeetingTransferPair.decode(3 * 14 + 4));
    }
}
