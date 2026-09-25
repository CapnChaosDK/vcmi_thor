package eu.vcmi.vcmi;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ThorHeroMeetingModeStateTest
{
    @Test public void retainsWithinRevisionAndResetsOnReplacement()
    {
        final ThorHeroMeetingModeState state = new ThorHeroMeetingModeState();
        assertEquals(ThorHeroMeetingModeState.ARMY, state.update(7, true));
        assertEquals(ThorHeroMeetingModeState.ARTIFACTS, state.select(7, ThorHeroMeetingModeState.ARTIFACTS));
        assertEquals(ThorHeroMeetingModeState.ARTIFACTS, state.update(7, true));
        assertEquals(ThorHeroMeetingModeState.ARMY, state.update(8, true));
        state.select(8, ThorHeroMeetingModeState.ARTIFACTS);
        assertEquals(ThorHeroMeetingModeState.ARMY, state.update(9, false));
        assertEquals(ThorHeroMeetingModeState.ARMY, state.select(8, ThorHeroMeetingModeState.ARTIFACTS));
    }
}
