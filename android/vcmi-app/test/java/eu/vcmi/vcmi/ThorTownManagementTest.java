package eu.vcmi.vcmi;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ThorTownManagementTest
{
    private static final int[] EMPTY_IDS = {-1, -1, -1, -1, -1, -1, -1};
    private static final int[] EMPTY_COUNTS = {0, 0, 0, 0, 0, 0, 0};
    private static final String[] EMPTY_NAMES = {"", "", "", "", "", "", ""};

    @Test
    public void copiesTwoBoundedArmiesAndRejectsMalformedLayouts()
    {
        final int[] ids = {1, -1, -1, -1, -1, -1, -1};
        final int[] counts = {12, 0, 0, 0, 0, 0, 0};
        final String[] names = {"Pikemen", "", "", "", "", "", ""};
        final ThorTownManagement town = ThorTownManagement.copyOf(8, 9, 10, "Hero", true,
                ids, counts, names, EMPTY_IDS, EMPTY_COUNTS, EMPTY_NAMES);
        assertTrue(town.complete());
        assertTrue(town.locallyControllable);
        ids[0] = 3;
        assertEquals(1, town.garrisonCreatureIds[0]);
        assertEquals(12, town.garrisonCounts[0]);
        assertEquals("Pikemen", town.garrisonNames[0]);

        assertSame(ThorTownManagement.EMPTY, ThorTownManagement.copyOf(8, 9, 10, "Hero", true,
                new int[6], new int[7], new String[7], EMPTY_IDS, EMPTY_COUNTS, EMPTY_NAMES));
        assertSame(ThorTownManagement.EMPTY, ThorTownManagement.copyOf(8, 9, 10, "Hero", true,
                new int[]{1, -1, -1, -1, -1, -1, -1}, new int[7], names,
                EMPTY_IDS, EMPTY_COUNTS, EMPTY_NAMES));
        assertSame(ThorTownManagement.EMPTY, ThorTownManagement.copyOf(8, 9, 10, "Hero", true,
                new int[]{-2, -1, -1, -1, -1, -1, -1}, EMPTY_COUNTS, EMPTY_NAMES,
                EMPTY_IDS, EMPTY_COUNTS, EMPTY_NAMES));
        assertSame(ThorTownManagement.EMPTY, ThorTownManagement.copyOf(8, 9, -1, "", false,
                new int[7], new int[7], new String[7], ids, counts, names));
    }

    @Test
    public void absentVisitorProducesEmptyArmyAndDisablesTransferOwnership()
    {
        final ThorTownManagement town = ThorTownManagement.copyOf(8, 9, -1, "", true,
                EMPTY_IDS, EMPTY_COUNTS, EMPTY_NAMES, EMPTY_IDS, EMPTY_COUNTS, EMPTY_NAMES);
        assertTrue(town.complete());
        assertFalse(town.locallyControllable);
        assertArrayEquals(new int[7], town.visitingCreatureIds);
    }

    @Test
    public void armyPairEncodingSeparatesDirectionAndRejectsOverflow()
    {
        assertEquals(1, ThorTownArmyPair.encode(false, 0, false, 1));
        assertEquals(97, ThorTownArmyPair.encode(false, 6, true, 6));
        assertEquals(98, ThorTownArmyPair.encode(true, 0, false, 0));
        assertEquals(194, ThorTownArmyPair.encode(true, 6, true, 5));
        assertEquals(-1, ThorTownArmyPair.encode(false, -1, false, 0));
        assertEquals(-1, ThorTownArmyPair.encode(false, 0, true, 7));
        assertEquals(-1, ThorTownArmyPair.encode(false, 0, false, 0));
    }
}
