package eu.vcmi.vcmi;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ThorTownHallBrowserTest
{
    private ThorBrowserState state(final int start, final int count)
    {
        final int[] targets = new int[count];
        final String[] labels = new String[count];
        final int[] flags = new int[count];
        for (int index = 0; index < count; ++index)
        {
            targets[index] = 50000 + start + index;
            labels[index] = "Building " + (start + index) + "\n" + (start + index) + " gold";
            final boolean built = index % 4 == 0;
            flags[index] = (!built && index % 3 == 0 ? 1 : 0) | (built ? 4 : 0);
        }
        return ThorBrowserState.copyOf(ThorContextIds.TOWN_HALL, 0,
                (count + ThorTownHallBrowser.PAGE_SIZE - 1) / ThorTownHallBrowser.PAGE_SIZE,
                targets, labels, flags);
    }

    @Test
    public void boundedListPagesLocallyAcrossFirstMiddleAndFinalPages()
    {
        final ThorBrowserState rows = state(0, 12);
        assertEquals(12, rows.rowCount());
        final ThorTownHallBrowser browser = new ThorTownHallBrowser();
        browser.update(ThorContextIds.TOWN_HALL, 20, 4, rows);
        assertEquals(3, browser.pageCount());
        assertEquals(5, browser.rowCount());
        assertEquals(50000, browser.targetAt(0));
        assertFalse(browser.previousPage());
        assertTrue(browser.nextPage());
        assertEquals(1, browser.page());
        assertEquals(50005, browser.targetAt(0));
        assertTrue(browser.nextPage());
        assertEquals(2, browser.page());
        assertEquals(2, browser.rowCount());
        assertEquals(50010, browser.targetAt(0));
        assertFalse(browser.nextPage());
        assertTrue(browser.previousPage());
        assertEquals(1, browser.page());
    }

    @Test
    public void selectionIsRevisionScopedAndDisappearingTargetsAreCleared()
    {
        final ThorTownHallBrowser browser = new ThorTownHallBrowser();
        browser.update(ThorContextIds.TOWN_HALL, 20, 4, state(0, 8));
        assertTrue(browser.select(50001));
        assertFalse(browser.canBuildSelected());
        assertFalse(browser.select(99999));
        browser.update(ThorContextIds.TOWN_HALL, 20, 4, state(0, 8));
        assertEquals(50001, browser.selectedTarget());
        browser.update(ThorContextIds.TOWN_HALL, 21, 4, state(0, 8));
        assertEquals(ThorActionIds.NO_TARGET, browser.selectedTarget());
        assertTrue(browser.select(50000));
        assertFalse(browser.canBuildSelected()); // Built row remains selectable but never dispatches.
        assertTrue(browser.builtAt(0));
        assertTrue(browser.select(50003));
        assertTrue(browser.canBuildSelected());
        browser.update(ThorContextIds.TOWN_HALL, 21, 4, state(10, 2));
        assertEquals(ThorActionIds.NO_TARGET, browser.selectedTarget());
        assertFalse(browser.canBuildSelected());
    }

    @Test
    public void sessionContextAndPresentationReplacementResetLocalState()
    {
        final ThorTownHallBrowser browser = new ThorTownHallBrowser();
        browser.update(ThorContextIds.TOWN_HALL, 30, 6, state(0, 6));
        browser.nextPage();
        browser.select(50005);
        browser.update(ThorContextIds.TOWN_HALL, 30, 7, state(0, 6));
        assertEquals(0, browser.page());
        assertEquals(ThorActionIds.NO_TARGET, browser.selectedTarget());
        browser.update(ThorContextIds.TOWN_WINDOW, 30, 7, state(0, 6));
        assertEquals(0, browser.rowCount());
        assertEquals(ThorActionIds.NO_TARGET, browser.selectedTarget());
    }

    @Test
    public void buildAndBackControlsStaySeparateFromFiveLocalRows()
    {
        final float width = 982f;
        final float height = 1142f;
        for (int row = 0; row < ThorTownHallBrowser.PAGE_SIZE; ++row)
        {
            final float[] bounds = ThorBrowserState.boundsForControl(ThorContextIds.TOWN_HALL,
                    ThorBrowserState.CONTROL_FIRST_ROW + row, width, height);
            assertEquals(ThorBrowserState.CONTROL_FIRST_ROW + row,
                    ThorBrowserState.controlAt(ThorContextIds.TOWN_HALL, (bounds[0] + bounds[2]) / 2,
                            (bounds[1] + bounds[3]) / 2, width, height, 5));
        }
        assertEquals(ThorActionIds.LOCAL_CONTROL, ThorBrowserState.actionForControl(
                ThorContextIds.TOWN_HALL, ThorBrowserState.CONTROL_PREVIOUS));
        assertEquals(ThorActionIds.TOWN_HALL_BUILD, ThorBrowserState.actionForControl(
                ThorContextIds.TOWN_HALL, ThorBrowserState.CONTROL_PRIMARY));
        assertEquals(ThorActionIds.WINDOW_CLOSE, ThorBrowserState.actionForControl(
                ThorContextIds.TOWN_HALL, ThorBrowserState.CONTROL_BACK));
        assertTrue(ThorBrowserState.boundsForControl(ThorContextIds.TOWN_HALL,
                ThorBrowserState.CONTROL_FIRST_ROW + 4, width, height)[3]
                < ThorBrowserState.boundsForControl(ThorContextIds.TOWN_HALL,
                ThorBrowserState.CONTROL_PREVIOUS, width, height)[1]);
    }

    @Test
    public void nativeListBoundAcceptsSixtyFourAndRejectsSixtyFiveWithoutTruncation()
    {
        final int[] targets = new int[ThorBrowserState.MAX_ROWS];
        final String[] labels = new String[targets.length];
        final int[] flags = new int[targets.length];
        for (int index = 0; index < targets.length; ++index)
        {
            targets[index] = 100000 + index;
            labels[index] = "Mod building " + index;
            flags[index] = index == 63 ? 0 : 1;
        }
        final int pages = (targets.length + ThorTownHallBrowser.PAGE_SIZE - 1) / ThorTownHallBrowser.PAGE_SIZE;
        assertEquals(64, ThorBrowserState.copyOf(ThorContextIds.TOWN_HALL, 0, pages,
                targets, labels, flags).rowCount());
        assertEquals(0, ThorBrowserState.copyOf(ThorContextIds.TOWN_HALL, 0, pages,
                java.util.Arrays.copyOf(targets, 65), java.util.Arrays.copyOf(labels, 65),
                java.util.Arrays.copyOf(flags, 65)).rowCount());
    }

    @Test
    public void buildGestureRequiresSameEnabledTargetAndCancelsOnPointerOrSessionChanges()
    {
        final ThorBrowserGesture gesture = new ThorBrowserGesture();
        gesture.begin(ThorContextIds.TOWN_HALL, ThorBrowserState.CONTROL_PRIMARY, 50000, 8, 3, 0, true);
        assertTrue(gesture.finish(ThorContextIds.TOWN_HALL, ThorBrowserState.CONTROL_PRIMARY,
                50000, 1, 0, 8, 3, true, true));
        gesture.begin(ThorContextIds.TOWN_HALL, ThorBrowserState.CONTROL_PRIMARY, 50000, 8, 3, 0, true);
        assertFalse(gesture.finish(ThorContextIds.TOWN_HALL, ThorBrowserState.CONTROL_PRIMARY,
                50000, 1, 0, 9, 3, true, true));
        gesture.begin(ThorContextIds.TOWN_HALL, ThorBrowserState.CONTROL_PRIMARY, 50000, 8, 3, 0, true);
        assertFalse(gesture.finish(ThorContextIds.TOWN_HALL, ThorBrowserState.CONTROL_PRIMARY,
                50000, 2, 0, 8, 3, true, true));
        gesture.begin(ThorContextIds.TOWN_HALL, ThorBrowserState.CONTROL_PRIMARY, 50000, 8, 3, 0, false);
        assertFalse(gesture.finish(ThorContextIds.TOWN_HALL, ThorBrowserState.CONTROL_PRIMARY,
                50000, 1, 0, 8, 3, true, true));
        gesture.begin(ThorContextIds.TOWN_HALL, ThorBrowserState.CONTROL_PRIMARY, 50000, 8, 3, 0, true);
        assertFalse(gesture.finish(ThorContextIds.TOWN_HALL, ThorBrowserState.CONTROL_BACK,
                ThorActionIds.NO_TARGET, 1, 0, 8, 3, true, true));
        gesture.begin(ThorContextIds.TOWN_HALL, ThorBrowserState.CONTROL_PRIMARY, 50000, 8, 3, 0, true);
        gesture.cancel();
        assertFalse(gesture.isActive());
    }
}
