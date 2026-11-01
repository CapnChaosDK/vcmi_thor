package eu.vcmi.vcmi;

import org.junit.Test;
import static org.junit.Assert.*;

public class ThorSaveLoadTest
{
    @Test public void saveBrowserAcceptsEmptySingleAndMultiplePagesInNativeOrder()
    {
        final String c = ThorContextIds.SAVE_GAME;
        final ThorBrowserState empty = ThorBrowserState.copyOf(c, 0, 1, new int[0], new String[0], new int[0]);
        assertEquals(1, empty.pageCount);
        final ThorBrowserState state = ThorBrowserState.copyOf(c, 2, 3, new int[]{12, 4},
                new String[]{"保存 😀", "x".repeat(128)}, new int[]{1, 3});
        assertEquals(2, state.rowCount());
        assertEquals(12, state.targets[0]);
        assertTrue(state.selected(1));
        assertSame(ThorBrowserState.EMPTY, ThorBrowserState.copyOf(c, 0, 1, new int[]{1}, new String[]{"x".repeat(129)}, new int[]{1}));
        assertEquals(ThorActionIds.WINDOW_CONFIRM, ThorBrowserState.actionForControl(c, 11));
        assertEquals(ThorActionIds.WINDOW_CLOSE, ThorBrowserState.actionForControl(c, 12));
        assertEquals(ThorActionIds.LOCAL_CONTROL, ThorBrowserState.actionForControl(c, 13));
        assertFalse(ThorBrowserState.isRowControl(11));
        assertFalse(ThorBrowserState.isRowControl(12));
        assertFalse(ThorBrowserState.isRowControl(13));
        // Original main-menu Load retains its actual lobby owner and terminal actions.
        assertEquals(ThorActionIds.LOBBY_START_GAME, ThorBrowserState.actionForControl(ThorContextIds.LOBBY_LOAD_GAME_SCENARIO, 11));
        assertEquals(ThorActionIds.LOBBY_BACK, ThorBrowserState.actionForControl(ThorContextIds.LOBBY_LOAD_GAME_SCENARIO, 12));
        assertEquals(ThorActionIds.NONE, ThorBrowserState.actionForControl(ThorContextIds.LOBBY_LOAD_GAME_SCENARIO, 13));
    }

    @Test public void touchRowsEditorAndFootersNeverOverlap()
    {
        for (float[] size : new float[][]{{982, 1142}, {960, 640}, {640, 480}})
        {
            for (int control : new int[]{1, 2, 3, 4, 5, 9, 10, 11, 12, 13})
            {
                final float[] a = ThorBrowserState.boundsForControl(ThorContextIds.SAVE_GAME, control, size[0], size[1]);
                assertEquals(control, ThorBrowserState.controlAt(ThorContextIds.SAVE_GAME,
                        (a[0] + a[2]) / 2, (a[1] + a[3]) / 2, size[0], size[1], 5));
                assertTrue(a[1] >= 0 && a[3] <= size[1]);
                for (int other : new int[]{1, 2, 3, 4, 5, 9, 10, 11, 12, 13})
                {
                    if (other == control) continue;
                    final float[] b = ThorBrowserState.boundsForControl(ThorContextIds.SAVE_GAME, other, size[0], size[1]);
                    assertFalse(a[0] < b[2] && b[0] < a[2] && a[1] < b[3] && b[1] < a[3]);
                }
            }
        }
    }

    @Test public void changedNativeSelectionModalSessionAndDisabledStateCancelSaveReleases()
    {
        ThorBrowserGesture g = new ThorBrowserGesture();
        g.begin(ThorContextIds.SAVE_GAME, 11, -1, 3, 4, 0, true);
        assertFalse(g.finish(ThorContextIds.SAVE_GAME, 11, -1, 1, 0, 4, 4, true, true));
        g.begin(ThorContextIds.SAVE_GAME, 11, -1, 3, 4, 0, true);
        assertFalse(g.finish(ThorContextIds.DIALOG_CONFIRMATION, 11, -1, 1, 0, 3, 4, true, true));
        g.begin(ThorContextIds.SAVE_GAME, 11, -1, 3, 4, 0, true);
        assertFalse(g.finish(ThorContextIds.SAVE_GAME, 11, -1, 1, 0, 3, 5, true, true));
        g.begin(ThorContextIds.SAVE_GAME, 11, -1, 3, 4, 0, true);
        assertFalse(g.finish(ThorContextIds.SAVE_GAME, 11, -1, 1, 0, 3, 4, false, true));
        g.begin(ThorContextIds.SAVE_GAME, 11, -1, 3, 4, 0, true);
        assertFalse(g.finish(ThorContextIds.SAVE_GAME, 12, -1, 1, 0, 3, 4, true, true));
        g.begin(ThorContextIds.SAVE_GAME, 13, -1, 3, 4, 0, true);
        assertFalse(g.finish(ThorContextIds.SAVE_GAME, 13, -1, 2, 0, 3, 4, true, true));
        g.begin(ThorContextIds.SAVE_GAME, 13, -1, 3, 4, 0, true);
        assertTrue(g.finish(ThorContextIds.SAVE_GAME, 13, -1, 1, 0, 3, 4, true, true));
        assertFalse(g.finish(ThorContextIds.SAVE_GAME, 13, -1, 1, 0, 3, 4, true, true));
    }

    @Test public void filenameDraftsBoundUtf8AndCancelAcrossLifecycle()
    {
        assertTrue(ThorSaveNameState.valid("保存 😀"));
        assertTrue(ThorSaveNameState.valid("x".repeat(240)));
        for (String invalid : new String[]{"", ".", "..", "x".repeat(241), "😀".repeat(61), "\ud800", "\udc00", "a\u0000b", "a\nb"})
            assertFalse(ThorSaveNameState.valid(invalid));
        ThorSaveNameState s = new ThorSaveNameState();
        s.begin(3, 4);
        assertFalse(s.commit("", 3, 4));
        assertFalse(s.commit("valid", 4, 4));
        assertFalse(s.commit("valid", 3, 5));
        assertTrue(s.commit("保存 😀", 3, 4));
        assertFalse(s.commit("again", 3, 4));
        // The editor calls cancel for Back/IME dismissal, pause, toggle and display detach.
        s.begin(5, 6);
        s.cancel();
        assertFalse(s.commit("abandoned", 5, 6));
        s.begin(7, 8);
        assertFalse(s.current(5, 6));
        assertTrue(s.current(7, 8));
    }
}
