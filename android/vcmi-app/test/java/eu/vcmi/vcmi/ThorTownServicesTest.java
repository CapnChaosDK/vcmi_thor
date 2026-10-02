package eu.vcmi.vcmi;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ThorTownServicesTest
{
    @Test
    public void servicesUseFixedNonOverlappingTargetsAboveWindowNavigation()
    {
        for (int service = 0; service < ThorTownServices.SERVICE_COUNT; ++service)
        {
            final float[] box = ThorTownServices.bounds(service, 1000f, 1200f);
            assertTrue(box[1] >= 1200f * 0.585f);
            assertTrue(box[3] < 1200f * 0.895f);
            assertEquals(service, ThorTownServices.serviceAt((box[0] + box[2]) / 2f,
                    (box[1] + box[3]) / 2f, 1000f, 1200f));
        }
        assertEquals(ThorTownServices.NONE, ThorTownServices.serviceAt(500f, 300f, 1000f, 1200f));
    }

    @Test
    public void gestureRejectsDisabledCrossRowStalePointerAndSessionInput()
    {
        final ThorTownServices gesture = new ThorTownServices();
        gesture.begin(ThorTownServices.HALL, 4L, 2L, 7, false);
        assertFalse(gesture.isActive());
        gesture.begin(ThorTownServices.HALL, 4L, 2L, 7, true);
        assertFalse(gesture.finish(ThorTownServices.RECRUIT, 4L, 2L, 7, 1, true, true));
        gesture.begin(ThorTownServices.HALL, 4L, 2L, 7, true);
        assertFalse(gesture.finish(ThorTownServices.HALL, 5L, 2L, 7, 1, true, true));
        gesture.begin(ThorTownServices.HALL, 4L, 2L, 7, true);
        assertFalse(gesture.finish(ThorTownServices.HALL, 4L, 3L, 7, 1, true, true));
        gesture.begin(ThorTownServices.HALL, 4L, 2L, 7, true);
        assertFalse(gesture.finish(ThorTownServices.HALL, 4L, 2L, 8, 1, true, true));
        gesture.begin(ThorTownServices.HALL, 4L, 2L, 7, true);
        assertFalse(gesture.finish(ThorTownServices.HALL, 4L, 2L, 7, 2, true, true));
        gesture.begin(ThorTownServices.HALL, 4L, 2L, 7, true);
        assertTrue(gesture.finish(ThorTownServices.HALL, 4L, 2L, 7, 1, true, true));
        assertFalse(gesture.isActive());
    }

    @Test
    public void townBrowserPayloadIsBoundedToExactFiveServiceTargets()
    {
        final ThorBrowserState state = ThorBrowserState.copyOf(ThorContextIds.TOWN_WINDOW, 0, 1,
                new int[]{0, 1, 2, 3, 4}, new String[]{"", "", "", "", ""},
                new int[]{1, 0, 1, 1, 0});
        assertEquals(5, state.rowCount());
        assertTrue(state.enabled(0));
        assertFalse(state.enabled(1));
        assertEquals(0, ThorBrowserState.copyOf(ThorContextIds.TOWN_WINDOW, 0, 1,
                new int[]{0, 1}, new String[]{"", ""}, new int[]{1, 1}).rowCount());
        assertEquals(0, ThorBrowserState.copyOf(ThorContextIds.TOWN_WINDOW, 0, 1,
                new int[]{0, 1, 2, 4, 3}, new String[]{"", "", "", "", ""},
                new int[]{1, 1, 1, 1, 1}).rowCount());
    }
}
