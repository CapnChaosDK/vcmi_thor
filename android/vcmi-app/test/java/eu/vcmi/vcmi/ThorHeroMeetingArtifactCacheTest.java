package eu.vcmi.vcmi;

import org.junit.Test;

import static org.junit.Assert.*;

public class ThorHeroMeetingArtifactCacheTest
{
    private static ThorHeroMeetingArtifacts snapshot()
    {
        final int[] positions = new int[48];
        final int[] flags = new int[48];
        final String[] names = new String[48];
        for (int index = 0; index < 48; ++index)
        {
            positions[index] = index % 24;
            flags[index] = index % 24 >= 19 ? 4 : 0;
            names[index] = "";
        }
        return ThorHeroMeetingArtifacts.copyOf(1, 2, new String[]{"Left", "Right"}, positions, flags, names);
    }

    @Test public void restoresOnlyLatestMatchingHeroMeetingSnapshot()
    {
        final ThorHeroMeetingArtifactCache cache = new ThorHeroMeetingArtifactCache();
        cache.reset(7, ThorContextIds.HERO_MEETING);
        final ThorHeroMeetingArtifacts artifacts = snapshot();
        assertFalse(cache.accept(6, artifacts));
        assertTrue(cache.accept(7, artifacts));
        assertSame(artifacts, cache.snapshot());
        cache.reset(8, ThorContextIds.HERO_WINDOW);
        assertSame(ThorHeroMeetingArtifacts.EMPTY, cache.snapshot());
        assertFalse(cache.accept(8, artifacts));
    }
}
