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
