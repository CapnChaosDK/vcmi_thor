package eu.vcmi.vcmi;

import org.junit.Test;

import static org.junit.Assert.*;

public class ThorHeroMeetingArtifactsTest
{
    private static ThorHeroMeetingArtifacts valid()
    {
        final int[] positions = new int[ThorHeroMeetingArtifacts.SLOT_COUNT];
        final int[] flags = new int[ThorHeroMeetingArtifacts.SLOT_COUNT];
        final String[] names = new String[ThorHeroMeetingArtifacts.SLOT_COUNT];
        for (int index = 0; index < positions.length; ++index)
        {
            positions[index] = index % ThorHeroMeetingArtifacts.PER_HERO;
            flags[index] = index % ThorHeroMeetingArtifacts.PER_HERO >= ThorHeroMeetingArtifacts.EQUIPPED_PER_HERO ? 4 : 0;
            names[index] = "";
        }
        flags[0] |= 3;
        names[0] = "Spell Scroll — Haste";
        return ThorHeroMeetingArtifacts.copyOf(1, 2, new String[]{"Left", "Right"}, positions, flags, names);
    }

    @Test public void acceptsCompleteImmutableShape()
    {
        final ThorHeroMeetingArtifacts snapshot = valid();
        assertTrue(snapshot.complete());
        assertEquals("Spell Scroll — Haste", snapshot.names[0]);
        assertEquals(3, snapshot.flags[0]);
    }

    @Test public void rejectsMalformedArraysAndBackpackFlags()
    {
        assertSame(ThorHeroMeetingArtifacts.EMPTY, ThorHeroMeetingArtifacts.copyOf(1, 2,
                new String[]{"Left", "Right"}, new int[0], new int[0], new String[0]));
        final ThorHeroMeetingArtifacts snapshot = valid();
        snapshot.flags[19] = 0;
        assertSame(ThorHeroMeetingArtifacts.EMPTY, ThorHeroMeetingArtifacts.copyOf(1, 2, snapshot.heroNames,
                snapshot.positions, snapshot.flags, snapshot.names));
    }
}
