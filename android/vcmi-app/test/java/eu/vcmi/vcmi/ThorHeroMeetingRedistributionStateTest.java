package eu.vcmi.vcmi;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ThorHeroMeetingRedistributionStateTest
{
    private static ThorHeroMeetingArmies armies()
    {
        final int[] creatures = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
        final int[] counts = new int[14];
        final int[] flags = new int[14];
        final String[] names = {"", "", "", "", "", "", "", "", "", "", "", "", "", ""};
        creatures[0] = 3;
        counts[0] = 21;
        flags[0] = 1;
        names[0] = "Pikemen";
        creatures[2] = 4;
        counts[2] = 5;
        flags[2] = 1;
        names[2] = "Archers";
        creatures[8] = 3;
        counts[8] = 7;
        flags[8] = 1;
        names[8] = "Pikemen";
        return ThorHeroMeetingArmies.copyOf(1, 2, new String[]{"A", "B"}, new int[]{1, 2},
                creatures, counts, names, flags, 1);
    }

    @Test
    public void choosesSeveralEmptyAndMatchingDestinationsAndReportsTotals()
    {
        final ThorHeroMeetingArmies armies = armies();
        final ThorHeroMeetingRedistributionState state = new ThorHeroMeetingRedistributionState();
        assertTrue(state.begin(22, 0, armies));
        assertFalse(state.selectDestination(22, 0, armies));
        assertFalse(state.selectDestination(22, 2, armies));
        assertFalse(state.canConfirm(22, armies));
        assertArrayEquals(new int[0], state.amounts(22, armies));
        assertTrue(state.selectDestination(22, 1, armies));
        state.adjust(22, 10, armies);
        state.adjust(22, 10, armies);
        assertEquals(20, state.totalAllocated());
        assertEquals(0, state.remaining());
        assertFalse(state.selectDestination(22, 8, armies));
        assertTrue(state.selectDestination(22, 1, armies));
        state.adjust(22, -10, armies);
        assertTrue(state.selectDestination(22, 8, armies));
        assertEquals(11, state.totalAllocated());
        assertEquals(9, state.remaining());
        assertTrue(state.canConfirm(22, armies));
        assertArrayEquals(new int[]{1, 2}, state.destinationArmyIds(22, armies));
        assertArrayEquals(new int[]{1, 1}, state.destinationSlots(22, armies));
        assertArrayEquals(new int[]{10, 1}, state.amounts(22, armies));
    }

    @Test
    public void clampsAllocationsAndRejectsMalformedOrStaleSnapshots()
    {
        final ThorHeroMeetingRedistributionState state = new ThorHeroMeetingRedistributionState();
        final ThorHeroMeetingArmies armies = armies();
        assertFalse(state.begin(0, 0, armies));
        assertFalse(state.begin(22, 2, armies));
        assertTrue(state.begin(22, 0, armies));
        assertFalse(state.selectDestination(23, 1, armies));
        assertTrue(state.selectDestination(22, 1, armies));
        state.adjust(22, Integer.MAX_VALUE, armies);
        assertEquals(20, state.totalAllocated());
        assertFalse(state.canConfirm(23, armies));
        assertArrayEquals(new int[0], state.amounts(23, armies));
        state.cancel();
        assertFalse(state.isActive());
        assertFalse(state.canConfirm(22, armies));
        assertArrayEquals(new int[0], state.amounts(22, armies));
    }

    @Test
    public void changedArmySnapshotInvalidatesThePlanEvenIfRevisionWasReused()
    {
        final ThorHeroMeetingArmies original = armies();
        final ThorHeroMeetingRedistributionState state = new ThorHeroMeetingRedistributionState();
        assertTrue(state.begin(22, 0, original));
        assertTrue(state.selectDestination(22, 1, original));

        final int[] changedCounts = original.counts.clone();
        changedCounts[0] = 20;
        final ThorHeroMeetingArmies changed = ThorHeroMeetingArmies.copyOf(original.leftHeroId,
                original.rightHeroId, original.heroNames, original.armyIds, original.creatureIds,
                changedCounts, original.creatureNames, original.flags, 1);
        assertFalse(state.isForCurrentArmies(changed));
        assertFalse(state.canConfirm(22, changed));
        assertArrayEquals(new int[0], state.amounts(22, changed));
    }

    @Test
    public void allocationPlanIsBoundedByAllThirteenOtherArmySlots()
    {
        final int[] creatures = new int[ThorHeroMeetingArmies.SLOT_COUNT];
        final int[] counts = new int[ThorHeroMeetingArmies.SLOT_COUNT];
        final int[] flags = new int[ThorHeroMeetingArmies.SLOT_COUNT];
        final String[] creatureNames = new String[ThorHeroMeetingArmies.SLOT_COUNT];
        Arrays.fill(creatures, -1);
        Arrays.fill(creatureNames, "");
        creatures[0] = 3;
        counts[0] = 14;
        flags[0] = 1;
        creatureNames[0] = "Pikemen";
        final ThorHeroMeetingArmies armies = ThorHeroMeetingArmies.copyOf(1, 2,
                new String[]{"A", "B"}, new int[]{1, 2}, creatures, counts, creatureNames, flags, 1);
        final ThorHeroMeetingRedistributionState state = new ThorHeroMeetingRedistributionState();
        assertTrue(state.begin(26, 0, armies));
        for (int key = 1; key < ThorHeroMeetingArmies.SLOT_COUNT; ++key)
            assertTrue(state.selectDestination(26, key, armies));
        assertEquals(13, state.totalAllocated());
        assertEquals(0, state.remaining());
        assertEquals(13, state.amounts(26, armies).length);
        assertEquals(ThorHeroMeetingRedistributionState.MAX_DESTINATIONS, state.amounts(26, armies).length);
    }

    @Test
    public void destinationCapacityAndLocalControlAreChecked()
    {
        final ThorHeroMeetingArmies armies = armies();
        final int[] creatures = armies.creatureIds.clone();
        final int[] counts = armies.counts.clone();
        counts[8] = Integer.MAX_VALUE;
        final ThorHeroMeetingArmies fullMatchingStack = ThorHeroMeetingArmies.copyOf(1, 2, armies.heroNames,
                armies.armyIds, creatures, counts, armies.creatureNames, armies.flags, 1);
        final ThorHeroMeetingRedistributionState state = new ThorHeroMeetingRedistributionState();
        assertTrue(state.begin(24, 0, fullMatchingStack));
        assertFalse(state.selectDestination(24, 8, fullMatchingStack));
        assertFalse(state.begin(25, 0, ThorHeroMeetingArmies.copyOf(1, 2, armies.heroNames, armies.armyIds,
                creatures, counts, armies.creatureNames, armies.flags, 0)));
        assertFalse(ThorHeroMeetingArmies.copyOf(1, 2, new String[]{"A"}, armies.armyIds,
                creatures, counts, armies.creatureNames, armies.flags, 1).complete());
    }
}
