package eu.vcmi.vcmi;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ThorBuildConfirmationTest
{
    @Test
    public void confirmationUsesStableActionAndContextContract()
    {
        assertEquals(57, ThorActionIds.WINDOW_CONFIRM);
        assertEquals(1L << 56, ThorActionIds.maskFor(ThorActionIds.WINDOW_CONFIRM));
        assertEquals("BUILD_CONFIRMATION", ThorContextIds.BUILD_CONFIRMATION);
        assertEquals("MENU_QUIT_CONFIRMATION", ThorContextIds.MENU_QUIT_CONFIRMATION);
        assertEquals("HIGH_SCORE_RESET_CONFIRMATION", ThorContextIds.HIGH_SCORE_RESET_CONFIRMATION);
        assertEquals(4, ThorContextDetails.COUNT);
    }

    @Test
    public void confirmAndCancelRequireSamePointerRevisionAndPresentation()
    {
        final ThorLobbyScenarioGesture gesture = new ThorLobbyScenarioGesture();
        gesture.begin(ThorBuildConfirmationState.CONTROL_BUY, 11L, 4L, 2, true);
        assertTrue(gesture.finish(ThorBuildConfirmationState.CONTROL_BUY, 1, 2, 11L, 4L, true, true));

        gesture.begin(ThorBuildConfirmationState.CONTROL_BUY, 12L, 4L, 2, true);
        assertFalse(gesture.finish(ThorBuildConfirmationState.CONTROL_CANCEL, 1, 2, 12L, 4L, true, true));

        gesture.begin(ThorBuildConfirmationState.CONTROL_BUY, 13L, 4L, 2, true);
        assertFalse(gesture.finish(ThorBuildConfirmationState.CONTROL_BUY, 1, 2, 13L, 5L, true, true));

        gesture.begin(ThorBuildConfirmationState.CONTROL_BUY, 14L, 4L, 2, false);
        assertFalse(gesture.finish(ThorBuildConfirmationState.CONTROL_BUY, 1, 2, 14L, 4L, true, true));
    }

    @Test
    public void controlsFitLowerPanelAndMapToNativeConfirmAndClose()
    {
        final float width = 1000f;
        final float height = 1100f;
        final float divider = 374f;
        final float[] buy = ThorBuildConfirmationState.boundsForControl(
                ThorBuildConfirmationState.CONTROL_BUY, width, height, divider);
        final float[] cancel = ThorBuildConfirmationState.boundsForControl(
                ThorBuildConfirmationState.CONTROL_CANCEL, width, height, divider);
        assertTrue(buy[0] >= 0f && buy[1] >= divider && buy[2] <= width && buy[3] <= height);
        assertTrue(cancel[0] >= 0f && cancel[1] >= divider && cancel[2] <= width && cancel[3] <= height);
        assertTrue(buy[2] < cancel[0]);
        assertEquals(ThorActionIds.WINDOW_CONFIRM,
                ThorBuildConfirmationState.actionForControl(ThorBuildConfirmationState.CONTROL_BUY));
        assertEquals(ThorActionIds.WINDOW_CLOSE,
                ThorBuildConfirmationState.actionForControl(ThorBuildConfirmationState.CONTROL_CANCEL));
        assertEquals(ThorBuildConfirmationState.CONTROL_BUY,
                ThorBuildConfirmationState.controlAt((buy[0] + buy[2]) / 2f, (buy[1] + buy[3]) / 2f,
                        width, height, divider));
        assertEquals(ThorBuildConfirmationState.CONTROL_CANCEL,
                ThorBuildConfirmationState.controlAt((cancel[0] + cancel[2]) / 2f, (cancel[1] + cancel[3]) / 2f,
                        width, height, divider));
        assertEquals(ThorBuildConfirmationState.CONTROL_NONE,
                ThorBuildConfirmationState.controlAt(2f, 2f, width, height, divider));
    }

    @Test
    public void menuConfirmationsReuseBoundedYesNoControls()
    {
        final String[] contexts = {
                ThorContextIds.MENU_QUIT_CONFIRMATION,
                ThorContextIds.HIGH_SCORE_RESET_CONFIRMATION
        };
        for (final String context : contexts)
        {
            assertEquals(0, ThorMainMenuState.choiceCount(context));
            assertEquals(ThorActionIds.WINDOW_CONFIRM,
                    ThorBuildConfirmationState.actionForControl(ThorBuildConfirmationState.CONTROL_BUY));
            assertEquals(ThorActionIds.WINDOW_CLOSE,
                    ThorBuildConfirmationState.actionForControl(ThorBuildConfirmationState.CONTROL_CANCEL));
        }
        assertEquals(ThorActionIds.NONE,
                ThorBuildConfirmationState.actionForControl(ThorBuildConfirmationState.CONTROL_NONE));
    }
}
