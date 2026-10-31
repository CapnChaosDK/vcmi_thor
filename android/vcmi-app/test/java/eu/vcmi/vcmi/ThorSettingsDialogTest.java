package eu.vcmi.vcmi;

import org.junit.Test;
import static org.junit.Assert.*;

public class ThorSettingsDialogTest
{
    @Test
    public void settingsRowsAndFooterRemainDisjoint()
    {
        for (String context : new String[]{ThorContextIds.SETTINGS, ThorContextIds.SETTINGS_SELECTION})
        {
            ThorBrowserState state = ThorBrowserState.copyOf(context, 1, 3,
                    new int[]{5, 6}, new String[]{"Music: 50 +", "Disabled"}, new int[]{3, 0});
            assertEquals(2, state.rowCount());
            assertTrue(state.enabled(0));
            assertTrue(state.selected(0));
            assertFalse(state.enabled(1));
            for (int row = 1; row <= 5; ++row)
                assertEquals(ThorActionIds.LOAD_BROWSER_SELECT, ThorBrowserState.actionForControl(context, row));
            for (int row = 6; row <= 8; ++row)
                assertEquals(ThorActionIds.NONE, ThorBrowserState.actionForControl(context, row));
            for (int control : new int[]{9, 10, 12})
            {
                float[] bounds = ThorBrowserState.boundsForControl(context, control, 960, 640);
                assertEquals(control, ThorBrowserState.controlAt(context,
                        (bounds[0] + bounds[2]) / 2, (bounds[1] + bounds[3]) / 2, 960, 640, 5));
            }
            assertEquals(ThorActionIds.WINDOW_CLOSE, ThorBrowserState.actionForControl(context, 12));
            assertSame(ThorBrowserState.EMPTY, ThorBrowserState.copyOf(context, 0, 1,
                    new int[]{1, 2, 3, 4, 5, 6}, new String[]{"a", "b", "c", "d", "e", "f"},
                    new int[]{1, 1, 1, 1, 1, 1}));
        }
        assertEquals(ThorActionIds.NONE, ThorBrowserState.actionForControl(ThorContextIds.SETTINGS, 11));
        assertEquals(ThorActionIds.WINDOW_CONFIRM,
                ThorBrowserState.actionForControl(ThorContextIds.SETTINGS_SELECTION, 11));
    }

    @Test
    public void dialogsOnlyExposeTheirOwnButtonsAndCompleteTextPages()
    {
        for (String context : new String[]{ThorContextIds.DIALOG_CONFIRMATION, ThorContextIds.DIALOG_INFORMATION})
        {
            ThorBrowserState state = ThorBrowserState.copyOf(context, 2, 4,
                    new int[0], new String[0], new int[0]);
            assertEquals(2, state.page);
            assertEquals(4, state.pageCount);
            assertEquals(0, state.rowCount());
            assertSame(ThorBrowserState.EMPTY, ThorBrowserState.copyOf(context, 0, 1,
                    new int[]{0}, new String[]{"Unexpected option"}, new int[]{1}));
            assertEquals(ThorActionIds.NONE, ThorBrowserState.actionForControl(context, 1));
            assertEquals(ThorActionIds.WINDOW_PREVIOUS, ThorBrowserState.actionForControl(context, 9));
            assertEquals(ThorActionIds.WINDOW_NEXT, ThorBrowserState.actionForControl(context, 10));
            assertEquals(ThorActionIds.WINDOW_CONFIRM, ThorBrowserState.actionForControl(context, 11));
        }
        assertEquals(ThorActionIds.NONE, ThorBrowserState.actionForControl(ThorContextIds.DIALOG_INFORMATION, 12));
        assertEquals(ThorActionIds.WINDOW_CLOSE,
                ThorBrowserState.actionForControl(ThorContextIds.DIALOG_CONFIRMATION, 12));
    }

    @Test
    public void staleDuplicateAndMultitouchConfirmationReleasesAreRejected()
    {
        for (String context : new String[]{ThorContextIds.DIALOG_CONFIRMATION, ThorContextIds.DIALOG_INFORMATION})
        {
            ThorBrowserGesture gesture = new ThorBrowserGesture();
            gesture.begin(context, 11, -1, 40, 3, 0, true);
            assertFalse(gesture.finish(context, 11, -1, 1, 0, 41, 3, true, true));
            gesture.begin(context, 11, -1, 40, 3, 0, true);
            assertFalse(gesture.finish(context, 11, -1, 1, 0, 40, 4, true, true));
            gesture.begin(context, 11, -1, 40, 3, 0, true);
            assertFalse(gesture.finish(context, 11, -1, 2, 0, 40, 3, true, true));
            gesture.begin(context, 11, -1, 40, 3, 0, true);
            assertTrue(gesture.finish(context, 11, -1, 1, 0, 40, 3, true, true));
            assertFalse(gesture.finish(context, 11, -1, 1, 0, 40, 3, true, true));
        }
    }
}
