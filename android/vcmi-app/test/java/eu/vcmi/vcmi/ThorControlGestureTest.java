package eu.vcmi.vcmi;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ThorControlGestureTest
{
    @Test
    public void everyTapIdentityAndPermissionGuardIsRequiredAndReleaseConsumesThePress()
    {
        final ThorControlGesture gesture = new ThorControlGesture(-1);
        // Each iteration changes exactly one acceptance condition.
        for (int changed = 0; changed < 10; ++changed)
        {
            gesture.begin("context", 0, 7L, changed == 7 ? 0L : 3L, 4, changed != 0);
            assertFalse(gesture.finish(changed == 1 ? "other" : "context", changed == 2 ? 1 : 0,
                    changed == 3 ? 2 : 1, changed == 4 ? 5 : 4,
                    changed == 5 ? 8L : 7L, changed == 6 ? 4L : 3L, changed != 8, changed != 9));
            assertFalse(gesture.isActive());
            assertFalse(gesture.finish("context", 0, 1, 4, 7L, 3L, true, true));
        }
        gesture.begin("context", 0, 7L, 3L, 4, true);
        assertTrue(gesture.finish("context", 0, 1, 4, 7L, 3L, true, true));
        assertFalse(gesture.finish("context", 0, 1, 4, 7L, 3L, true, true));
    }

    @Test
    public void cancellationAndReplacementKeepTheOwnersSentinelAndLatestPress()
    {
        for (final int none : new int[]{0, -1})
        {
            final ThorControlGesture gesture = new ThorControlGesture(none);
            assertEquals(none, gesture.control());
            gesture.begin(null, 2, 7L, 3L, 4, true);
            gesture.cancel();
            assertFalse(gesture.isActive());
            assertEquals(none, gesture.control());
            assertEquals(-1, gesture.pointerId());
            assertEquals(0L, gesture.revision());
            assertEquals(0L, gesture.session());
            assertFalse(gesture.finish(null, 2, 1, 4, 7L, 3L, true, true));
            gesture.begin(null, 2, 7L, 3L, 4, true);
            gesture.begin(null, 3, 8L, 4L, 5, true);
            assertTrue(gesture.finish(null, 3, 1, 5, 8L, 4L, true, true));
        }
    }

    @Test
    public void browserRevisionExceptionStillRequiresExactTargetPointerContextAndSession()
    {
        final ThorBrowserGesture browser = new ThorBrowserGesture();
        final String context = ThorContextIds.LOBBY_LOAD_GAME_SCENARIO;
        final int back = ThorBrowserState.CONTROL_BACK;
        for (int changed = 0; changed < 6; ++changed)
        {
            browser.begin(context, back, ThorActionIds.NO_TARGET, 7L, 3L, 4, true);
            assertFalse(browser.finish(changed == 0 ? ThorContextIds.CAMPAIGN_BROWSER : context,
                    back, changed == 1 ? 42 : ThorActionIds.NO_TARGET,
                    changed == 2 ? 2 : 1, changed == 3 ? 5 : 4,
                    8L, changed == 4 ? 4L : 3L, true, changed != 5, true));
            assertFalse(browser.isActive());
        }
        browser.begin(context, back, ThorActionIds.NO_TARGET, 7L, 3L, 4, true);
        assertTrue(browser.finish(context, back, ThorActionIds.NO_TARGET,
                1, 4, 8L, 3L, true, true, true));
        assertEquals(ThorActionIds.NO_TARGET, browser.target());
    }
}
