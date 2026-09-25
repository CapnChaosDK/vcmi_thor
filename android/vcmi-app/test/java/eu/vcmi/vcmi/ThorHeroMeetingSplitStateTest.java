package eu.vcmi.vcmi;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ThorHeroMeetingSplitStateTest
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
    public void selectsLegalSameOrOppositeDestinationAndRejectsIllegalOnes()
    {
        final ThorHeroMeetingSplitState state = new ThorHeroMeetingSplitState();
        assertFalse(state.begin(22, 0, 1));
        assertTrue(state.begin(22, 0, 21));
        assertFalse(state.selectDestination(22, 0, armies()));
        assertFalse(state.selectDestination(22, 2, armies()));
        assertFalse(state.selectDestination(21, 1, armies()));
        assertTrue(state.selectDestination(22, 1, armies()));
        assertEquals(10, state.amount());

        state.cancel();
        assertTrue(state.begin(22, 0, 21));
        assertTrue(state.selectDestination(22, 8, armies()));
    }

    @Test
    public void amountControlsClampAndCancelByRevision()
    {
        final ThorHeroMeetingSplitState state = new ThorHeroMeetingSplitState();
        state.begin(22, 0, 21);
        state.selectDestination(22, 7, armies());
        state.adjust(-10);
        assertEquals(1, state.amount());
        state.adjust(-1);
        assertEquals(1, state.amount());
        state.adjust(10);
        state.adjust(10);
        assertEquals(20, state.amount());
        state.adjust(1);
        assertEquals(20, state.amount());
        assertTrue(state.isCurrent(22));
        assertFalse(state.isCurrent(23));
        state.cancel();
        assertEquals(ThorHeroMeetingSplitState.Stage.NONE, state.stage());

        final int[] creatures = new int[14];
        final int[] counts = new int[14];
        final int[] flags = new int[14];
        final String[] names = new String[14];
        java.util.Arrays.fill(creatures, -1);
        java.util.Arrays.fill(names, "");
        creatures[0] = 3;
        counts[0] = Integer.MAX_VALUE;
        flags[0] = 1;
        final ThorHeroMeetingArmies huge = ThorHeroMeetingArmies.copyOf(1, 2, new String[]{"A", "B"},
                new int[]{1, 2}, creatures, counts, names, flags, 1);
        state.begin(23, 0, Integer.MAX_VALUE);
        state.selectDestination(23, 1, huge);
        state.adjust(Integer.MAX_VALUE);
        state.adjust(10);
        assertEquals(Integer.MAX_VALUE - 1, state.amount());
    }
}
