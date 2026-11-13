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

    @Test
    public void encounterRowsAndFooterHaveSeparateLargeHitRegions()
    {
        final int[] controls = {1, 2, 3, 4, ThorBrowserState.CONTROL_PREVIOUS,
                ThorBrowserState.CONTROL_NEXT, ThorBrowserState.CONTROL_PRIMARY,
                ThorBrowserState.CONTROL_BACK, ThorBrowserState.CONTROL_TEXT_PREVIOUS,
                ThorBrowserState.CONTROL_TEXT_NEXT};
        final float width = 980f, height = 1140f, density = 2.3f;
        for (final int control : controls)
        {
            final float[] bounds = ThorBrowserState.encounterBounds(control, width, height, density, true);
            assertTrue(bounds[2] - bounds[0] >= 48f * density - .001f);
            assertTrue(bounds[3] - bounds[1] >= 48f * density - .001f);
            assertEquals(control, ThorBrowserState.encounterControlAt(
                    (bounds[0] + bounds[2]) / 2, (bounds[1] + bounds[3]) / 2,
                    width, height, density, 4, true));
            for (final int other : controls)
            {
                if (other == control) continue;
                final float[] b = ThorBrowserState.encounterBounds(other, width, height, density, true);
                assertTrue(bounds[2] <= b[0] || b[2] <= bounds[0] || bounds[3] <= b[1] || b[3] <= bounds[1]);
            }
        }
        assertEquals(0, ThorBrowserState.encounterControlAt(150, 130, 300, 200, 2, 4, true));
        assertEquals(ThorActionIds.WINDOW_CONFIRM, ThorBrowserState.actionForControl(
                ThorContextIds.ENCOUNTER_CHOICE, ThorBrowserState.CONTROL_PRIMARY));
        assertEquals(ThorActionIds.WINDOW_NEXT, ThorBrowserState.actionForControl(
                ThorContextIds.ENCOUNTER_CHOICE, ThorBrowserState.CONTROL_TEXT_NEXT));
        assertEquals(ThorActionIds.NONE, ThorBrowserState.actionForControl(ThorContextIds.ENCOUNTER_CHOICE, 5));
    }

    @Test
    public void encounterPagingAndSelectionUseCurrentNativeIds()
    {
        final String context = ThorContextIds.ENCOUNTER_CHOICE;
        final ThorBrowserState page = ThorBrowserState.copyOf(context, 1, 2,
                new int[]{70001, 24}, new String[]{"Inferno", "Castle"}, new int[]{3, 0});
        assertEquals(1, page.page);
        assertTrue(page.selected(0));
        assertFalse(page.enabled(1));
        assertEquals(70001, page.targets[0]);
        assertSame(ThorBrowserState.EMPTY, ThorBrowserState.copyOf(context, 2, 2,
                new int[]{70001}, new String[]{"Inferno"}, new int[]{3}));

        final ThorBrowserGesture gesture = new ThorBrowserGesture();
        gesture.begin(context, 1, 70001, 10, 2, 4, true);
        assertFalse(gesture.finish(context, 1, 70001, 1, 4, 11, 2, true, true));
        gesture.begin(context, 1, 70001, 11, 2, 4, true);
        assertFalse(gesture.finish(context, 1, 70001, 2, 4, 11, 2, true, true));
        gesture.begin(context, 1, 70001, 11, 2, 4, true);
        assertFalse(gesture.finish(context, 1, 70001, 1, 4, 11, 3, true, true));
        gesture.begin(context, 1, 70001, 11, 3, 4, true);
        assertTrue(gesture.finish(context, 1, 70001, 1, 4, 11, 3, true, true));
        assertFalse(gesture.finish(context, 1, 70001, 1, 4, 11, 3, true, true));
    }
}
