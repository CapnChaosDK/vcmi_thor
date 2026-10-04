package eu.vcmi.vcmi;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ThorBrowserTest
{
    @Test
    public void boundedPagesKeepCurrentNativeOrderAndState()
    {
        final ThorBrowserState campaign = ThorBrowserState.copyOf(ThorContextIds.CAMPAIGN_BROWSER, 1, 2,
                new int[]{8, 3, 9}, new String[]{"Completed", "Locked", "Ready"},
                new int[]{5, 0, 1});
        assertEquals(3, campaign.rowCount());
        assertEquals(8, campaign.targets[0]);
        assertTrue(campaign.enabled(0));
        assertTrue(campaign.completed(0));
        assertFalse(campaign.enabled(1));
        assertFalse(campaign.selected(2));
        assertEquals(1, campaign.page);
        assertEquals(2, campaign.pageCount);
        final ThorBrowserState save = ThorBrowserState.copyOf(ThorContextIds.LOBBY_LOAD_GAME_SCENARIO, 2, 3,
                new int[]{2, 4}, new String[]{"A", "B"}, new int[]{1, 3});
        assertEquals(2, save.rowCount());
        assertTrue(save.selected(1));
        assertEquals(2, save.page);
        assertEquals(3, save.pageCount);
    }

    @Test
    public void repeatedBrowserPublicationKeepsCurrentPageUntilItsContentsChange()
    {
        final String context = ThorContextIds.LOBBY_LOAD_GAME_SCENARIO;
        final ThorBrowserState current = ThorBrowserState.copyOf(context, 0, 1,
                new int[]{2, 4}, new String[]{"Autosave", "NEWGAME"}, new int[]{1, 3});
        assertTrue(current.sameContents(ThorBrowserState.copyOf(context, 0, 1,
                new int[]{2, 4}, new String[]{"Autosave", "NEWGAME"}, new int[]{1, 3})));
        assertFalse(current.sameContents(ThorBrowserState.copyOf(context, 0, 1,
                new int[]{2, 4}, new String[]{"Autosave", "NEWGAME"}, new int[]{3, 1})));
    }

    @Test
    public void malformedOrOversizedPayloadFailsClosed()
    {
        assertEquals(0, ThorBrowserState.copyOf(ThorContextIds.UNKNOWN, 0, 1,
                new int[]{0}, new String[]{"A"}, new int[]{1}).rowCount());
        assertEquals(0, ThorBrowserState.copyOf(ThorContextIds.CAMPAIGN_BROWSER, 1, 1,
                new int[]{0}, new String[]{"A"}, new int[]{1}).rowCount());
        assertEquals(0, ThorBrowserState.copyOf(ThorContextIds.LOBBY_LOAD_GAME_SCENARIO, 0, 1,
                new int[]{0, 1, 2, 3, 4, 5}, new String[]{"A", "B", "C", "D", "E", "F"},
                new int[]{1, 1, 1, 1, 1, 1}).rowCount());
        assertEquals(0, ThorBrowserState.copyOf(ThorContextIds.CAMPAIGN_BROWSER, 0, 1,
                new int[]{0, 0}, new String[]{"A", "B"}, new int[]{1, 1}).rowCount());
        assertEquals(0, ThorBrowserState.copyOf(ThorContextIds.CAMPAIGN_BROWSER, 0, 1,
                new int[]{0}, new String[]{"A"}, new int[]{8}).rowCount());
    }

    @Test
    public void rowsAndPageControlsHaveDistinctRegionsAndStableActions()
    {
        final float width = 982f;
        final float height = 1142f;
        for (final String context : new String[]{ThorContextIds.CAMPAIGN_BROWSER,
                ThorContextIds.LOBBY_LOAD_GAME_SCENARIO, ThorContextIds.TOWN_HALL})
        {
            final int rowCount = ThorContextIds.CAMPAIGN_BROWSER.equals(context) ? 8 : 5;
            for (int row = 0; row < rowCount; ++row)
            {
                final float[] bounds = ThorBrowserState.boundsForControl(context,
                        ThorBrowserState.CONTROL_FIRST_ROW + row, width, height);
                assertEquals(ThorBrowserState.CONTROL_FIRST_ROW + row,
                        ThorBrowserState.controlAt(context, (bounds[0] + bounds[2]) / 2,
                                (bounds[1] + bounds[3]) / 2, width, height, rowCount));
            }
            assertEquals(ThorBrowserState.CONTROL_NONE,
                    ThorBrowserState.controlAt(context, width / 2, height * 0.8f, width, height, rowCount));
            assertEquals(ThorContextIds.TOWN_HALL.equals(context) ? ThorActionIds.LOCAL_CONTROL
                            : ThorContextIds.CAMPAIGN_BROWSER.equals(context)
                            ? ThorActionIds.CAMPAIGN_BROWSER_SELECT : ThorActionIds.LOAD_BROWSER_SELECT,
                    ThorBrowserState.actionForControl(context, ThorBrowserState.CONTROL_FIRST_ROW));
        }
        assertEquals(ThorActionIds.NONE, ThorBrowserState.actionForControl(
                ThorContextIds.CAMPAIGN_BROWSER, ThorBrowserState.CONTROL_PRIMARY));
    }

    @Test
    public void loadAndBackControlsHaveDistinctTouchableRegionsAndLobbyActions()
    {
        final String context = ThorContextIds.LOBBY_LOAD_GAME_SCENARIO;
        final float width = 982f;
        final float height = 1142f;
        final int[] controls = {ThorBrowserState.CONTROL_PRIMARY, ThorBrowserState.CONTROL_BACK};
        final int[] actions = {ThorActionIds.LOBBY_START_GAME, ThorActionIds.LOBBY_BACK};
        final float[][] bounds = new float[controls.length][];
        for (int index = 0; index < controls.length; ++index)
        {
            bounds[index] = ThorBrowserState.boundsForControl(context, controls[index], width, height);
            assertTrue(bounds[index][0] >= 0f);
            assertTrue(bounds[index][1] >= 0f);
            assertTrue(bounds[index][2] <= width);
            assertTrue(bounds[index][3] <= height);
            assertTrue(bounds[index][2] > bounds[index][0]);
            assertTrue(bounds[index][3] > bounds[index][1]);
            assertEquals(controls[index], ThorBrowserState.controlAt(context,
                    (bounds[index][0] + bounds[index][2]) / 2, (bounds[index][1] + bounds[index][3]) / 2,
                    width, height, 1));
            assertEquals(actions[index], ThorBrowserState.actionForControl(context, controls[index]));
            final ThorBrowserGesture gesture = new ThorBrowserGesture();
            gesture.begin(context, controls[index], ThorActionIds.NO_TARGET, 7, 9, 1, true);
            if (controls[index] == ThorBrowserState.CONTROL_BACK)
                assertTrue(gesture.finish(context, controls[index], ThorActionIds.NO_TARGET, 1, 1, 8, 9,
                        true, true, true));
            else
                assertFalse(gesture.finish(context, controls[index], ThorActionIds.NO_TARGET, 1, 1, 8, 9,
                        true, true));
        }
        assertTrue(bounds[0][2] <= bounds[1][0] || bounds[1][2] <= bounds[0][0]
                || bounds[0][3] <= bounds[1][1] || bounds[1][3] <= bounds[0][1]);
    }

    @Test
    public void loadBackSurvivesBrowserRefreshWhileRowsAndOtherContextsCancel()
    {
        final ThorBrowserGesture gesture = new ThorBrowserGesture();
        final String context = ThorContextIds.LOBBY_LOAD_GAME_SCENARIO;
        gesture.begin(context, ThorBrowserState.CONTROL_BACK, ThorActionIds.NO_TARGET, 7, 9, 1, true);
        assertTrue(gesture.retainsLoadBackAfterBrowserUpdate(context, true));
        assertFalse(gesture.retainsLoadBackAfterBrowserUpdate(context, false));
        assertFalse(gesture.retainsLoadBackAfterBrowserUpdate(ThorContextIds.CAMPAIGN_BROWSER, true));
        gesture.begin(context, ThorBrowserState.CONTROL_FIRST_ROW, 2, 7, 9, 1, true);
        assertFalse(gesture.retainsLoadBackAfterBrowserUpdate(context, true));
        gesture.cancel();
        assertFalse(gesture.retainsLoadBackAfterBrowserUpdate(context, true));
    }

    @Test
    public void loadBackSurvivesTransientZeroMaskButReleaseStillRequiresEnabledBack()
    {
        final ThorBrowserGesture gesture = new ThorBrowserGesture();
        final String context = ThorContextIds.LOBBY_LOAD_GAME_SCENARIO;
        final long backBit = ThorActionIds.maskFor(ThorActionIds.LOBBY_BACK);
        gesture.begin(context, ThorBrowserState.CONTROL_BACK, ThorActionIds.NO_TARGET, 7, 9, 1, true);
        assertTrue(gesture.retainsLoadBackAcrossContextUpdate(context, backBit, 0L, true));
        assertTrue(gesture.retainsLoadBackAcrossContextUpdate(context, 0L, backBit, false));
        assertFalse(gesture.retainsLoadBackAcrossContextUpdate(context, backBit, 0L, false));
        assertFalse(gesture.retainsLoadBackAcrossContextUpdate(context, 0L, 0L, true));
        assertFalse(gesture.retainsLoadBackAcrossContextUpdate(ThorContextIds.CAMPAIGN_BROWSER, backBit, 0L, true));
        assertFalse(gesture.finish(context, ThorBrowserState.CONTROL_BACK, ThorActionIds.NO_TARGET,
                1, 1, 8, 9, false, true, true));
        gesture.begin(context, ThorBrowserState.CONTROL_BACK, ThorActionIds.NO_TARGET, 7, 9, 1, true);
        assertTrue(gesture.finish(context, ThorBrowserState.CONTROL_BACK, ThorActionIds.NO_TARGET,
                1, 1, 8, 9, true, true, true));
    }

    @Test
    public void gesturesRejectCrossRowPagePointerRevisionAndSessionChanges()
    {
        final ThorBrowserGesture gesture = new ThorBrowserGesture();
        final String context = ThorContextIds.LOBBY_LOAD_GAME_SCENARIO;
        gesture.begin(context, 1, 7, 10, 3, 0, true);
        assertFalse(gesture.finish(context, 2, 8, 1, 0, 10, 3, true, true));
        gesture.begin(context, 1, 7, 10, 3, 0, true);
        assertFalse(gesture.finish(context, 1, 7, 1, 0, 11, 3, true, true));
        gesture.begin(context, 1, 7, 10, 3, 0, true);
        assertFalse(gesture.finish(context, 1, 7, 1, 0, 10, 4, true, true));
        gesture.begin(context, 1, 7, 10, 3, 0, true);
        assertFalse(gesture.finish(context, 1, 7, 2, 0, 10, 3, true, true));
        gesture.begin(context, 1, 7, 10, 3, 0, true);
        assertFalse(gesture.finish(ThorContextIds.CAMPAIGN_BROWSER, 1, 7, 1, 0, 10, 3, true, true));
        gesture.begin(context, 1, 7, 10, 3, 0, true);
        assertFalse(gesture.finish(context, 1, 7, 1, 0, 10, 3, true, false));
        gesture.begin(context, 1, 7, 10, 3, 0, true);
        assertTrue(gesture.finish(context, 1, 7, 1, 0, 10, 3, true, true));
    }
}
