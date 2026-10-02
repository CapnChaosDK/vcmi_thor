package eu.vcmi.vcmi;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ThorWindowNavigationTest
{
    @Test
    public void sharedControlsFitBelowBothDashboardsAndKeepActionIdsStable()
    {
        assertEquals(49, ThorActionIds.LOAD_BROWSER_NEXT_PAGE);
        assertEquals(50, ThorActionIds.WINDOW_PREVIOUS);
        assertEquals(51, ThorActionIds.WINDOW_NEXT);
        assertEquals(52, ThorActionIds.WINDOW_CLOSE);
        assertEquals(1L << 51, ThorActionIds.maskFor(ThorActionIds.WINDOW_CLOSE));
        assertEquals(53, ThorActionIds.TOWN_OPEN_SERVICE);
        assertEquals(1L << 52, ThorActionIds.maskFor(ThorActionIds.TOWN_OPEN_SERVICE));
        for (int control = 0; control < 3; ++control)
        {
            final float[] box = ThorWindowNavigation.bounds(control, 1000f, 1200f);
            assertTrue(box[1] >= 1200f * 0.895f);
            assertTrue(box[3] <= 1200f);
            assertEquals(control, ThorWindowNavigation.controlAt((box[0] + box[2]) / 2f,
                    (box[1] + box[3]) / 2f, 1000f, 1200f));
        }
        assertEquals(ThorWindowNavigation.NONE,
                ThorWindowNavigation.controlAt(500f, 600f, 1000f, 1200f));
        assertTrue(ThorWindowNavigation.isWindow(ThorContextIds.HERO_WINDOW));
        assertTrue(ThorWindowNavigation.isWindow(ThorContextIds.TOWN_WINDOW));
        assertFalse(ThorWindowNavigation.isWindow(ThorContextIds.ADVENTURE_MAP));
    }

    @Test
    public void gestureRejectsCrossControlPointerRevisionContextAndSessionChanges()
    {
        final ThorWindowNavigation gesture = new ThorWindowNavigation();
        gesture.begin(ThorContextIds.HERO_WINDOW, 1, 7L, 3L, 4, true);
        assertFalse(gesture.finish(ThorContextIds.HERO_WINDOW, 2, 7L, 3L, 4, 1, true, true));
        gesture.begin(ThorContextIds.HERO_WINDOW, 1, 7L, 3L, 4, true);
        assertFalse(gesture.finish(ThorContextIds.HERO_WINDOW, 1, 8L, 3L, 4, 1, true, true));
        gesture.begin(ThorContextIds.HERO_WINDOW, 1, 7L, 3L, 4, true);
        assertFalse(gesture.finish(ThorContextIds.TOWN_WINDOW, 1, 7L, 3L, 4, 1, true, true));
        gesture.begin(ThorContextIds.TOWN_WINDOW, 1, 7L, 3L, 4, true);
        assertFalse(gesture.finish(ThorContextIds.TOWN_WINDOW, 1, 7L, 4L, 4, 1, true, true));
        gesture.begin(ThorContextIds.TOWN_WINDOW, 1, 7L, 3L, 4, true);
        assertFalse(gesture.finish(ThorContextIds.TOWN_WINDOW, 1, 7L, 3L, 5, 1, true, true));
        gesture.begin(ThorContextIds.TOWN_WINDOW, 1, 7L, 3L, 4, true);
        assertFalse(gesture.finish(ThorContextIds.TOWN_WINDOW, 1, 7L, 3L, 4, 2, true, true));
        gesture.begin(ThorContextIds.TOWN_WINDOW, 1, 7L, 3L, 4, true);
        assertFalse(gesture.finish(ThorContextIds.TOWN_WINDOW, 1, 7L, 3L, 4, 1, false, true));
        gesture.begin(ThorContextIds.TOWN_WINDOW, 1, 7L, 3L, 4, true);
        assertTrue(gesture.finish(ThorContextIds.TOWN_WINDOW, 1, 7L, 3L, 4, 1, true, true));
        assertFalse(gesture.isActive());
        gesture.begin(ThorContextIds.HERO_WINDOW, 0, 8L, 4L, 1, false);
        assertFalse(gesture.isActive());
        gesture.begin(ThorContextIds.HERO_WINDOW, 0, 8L, 4L, 1, true);
        gesture.cancel();
        assertFalse(gesture.isActive());
    }
}
