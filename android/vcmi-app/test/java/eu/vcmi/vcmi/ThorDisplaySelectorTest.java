package eu.vcmi.vcmi;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ThorDisplaySelectorTest
{
    private static ThorDisplaySelector.Candidate active(final int id)
    {
        return new ThorDisplaySelector.Candidate(id, true);
    }

    private static ThorDisplaySelector.Candidate inactive(final int id)
    {
        return new ThorDisplaySelector.Candidate(id, false);
    }

    @Test
    public void noSecondaryDisplayReturnsNone()
    {
        assertEquals(ThorDisplaySelector.NO_DISPLAY,
                ThorDisplaySelector.select(0, new ThorDisplaySelector.Candidate[0], new ThorDisplaySelector.Candidate[]{active(0)}));
    }

    @Test
    public void presentationDisplayIsPreferred()
    {
        assertEquals(4, ThorDisplaySelector.select(0,
                new ThorDisplaySelector.Candidate[]{active(4)},
                new ThorDisplaySelector.Candidate[]{active(0), active(2), active(4)}));
    }

    @Test
    public void activeFallbackDisplayIsUsed()
    {
        assertEquals(3, ThorDisplaySelector.select(0,
                new ThorDisplaySelector.Candidate[]{inactive(4)},
                new ThorDisplaySelector.Candidate[]{active(0), active(3), inactive(4)}));
    }

    @Test
    public void selectionIsDeterministicAcrossReordering()
    {
        final ThorDisplaySelector.Candidate[] first = {active(7), active(3), active(5)};
        final ThorDisplaySelector.Candidate[] second = {active(5), active(7), active(3)};
        assertEquals(3, ThorDisplaySelector.select(0, first, first));
        assertEquals(3, ThorDisplaySelector.select(0, second, second));
    }

    @Test
    public void activityAndInactiveDisplaysAreRejected()
    {
        assertEquals(8, ThorDisplaySelector.select(3,
                new ThorDisplaySelector.Candidate[]{active(3), inactive(4), active(8)},
                new ThorDisplaySelector.Candidate[]{active(3), inactive(4), active(8)}));
    }
}
