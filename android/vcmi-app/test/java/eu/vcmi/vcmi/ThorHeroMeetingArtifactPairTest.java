package eu.vcmi.vcmi;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ThorHeroMeetingArtifactPairTest
{
    @Test public void onlyOppositeHeroRowsEncode()
    {
        assertEquals(24, ThorHeroMeetingArtifactPair.encode(0, 24));
        assertEquals(47 * 48 + 23, ThorHeroMeetingArtifactPair.encode(47, 23));
        assertEquals(ThorHeroMeetingArtifactPair.INVALID, ThorHeroMeetingArtifactPair.encode(0, 23));
        assertEquals(ThorHeroMeetingArtifactPair.INVALID, ThorHeroMeetingArtifactPair.encode(24, 47));
        assertEquals(ThorHeroMeetingArtifactPair.INVALID, ThorHeroMeetingArtifactPair.encode(-1, 24));
        assertEquals(ThorHeroMeetingArtifactPair.INVALID, ThorHeroMeetingArtifactPair.encode(0, 48));
    }
}
