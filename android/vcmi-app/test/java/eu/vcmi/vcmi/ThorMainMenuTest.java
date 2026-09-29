package eu.vcmi.vcmi;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ThorMainMenuTest
{
    private static final String[] CONTEXTS = {
            ThorContextIds.MAIN_MENU,
            ThorContextIds.MAIN_MENU_NEW_GAME,
            ThorContextIds.MAIN_MENU_LOAD_GAME,
            ThorContextIds.MAIN_MENU_CAMPAIGN
    };

    @Test
    public void choicesUseStableDistinctMaskBitsAndLocalizedResources()
    {
        assertEquals(31, ThorActionIds.MAIN_MENU_CHOICE_1);
        assertEquals(32, ThorActionIds.MAIN_MENU_CHOICE_2);
        assertEquals(33, ThorActionIds.MAIN_MENU_CHOICE_3);
        assertEquals(34, ThorActionIds.MAIN_MENU_CHOICE_4);
        assertEquals(35, ThorActionIds.MAIN_MENU_CHOICE_5);
        for (final String context : CONTEXTS)
        {
            assertEquals(5, ThorMainMenuState.choiceCount(context));
            for (int control = 1; control <= 5; ++control)
            {
                final int actionId = ThorMainMenuState.actionForControl(context, control);
                assertEquals(30 + control, actionId);
                assertEquals(1L << (actionId - 1), ThorActionIds.maskFor(actionId));
                assertTrue(ThorMainMenuState.labelForControl(context, control) != 0);
            }
        }
        assertEquals(R.string.thor_context_new_game,
                ThorMainMenuState.labelForControl(ThorContextIds.MAIN_MENU, 1));
        assertEquals(R.string.thor_menu_high_scores,
                ThorMainMenuState.labelForControl(ThorContextIds.MAIN_MENU, 3));
        assertEquals(R.string.thor_menu_quit,
                ThorMainMenuState.labelForControl(ThorContextIds.MAIN_MENU, 5));
        assertEquals(R.string.thor_context_campaign,
                ThorMainMenuState.labelForControl(ThorContextIds.MAIN_MENU_NEW_GAME, 3));
        assertEquals(R.string.thor_context_campaign,
                ThorMainMenuState.labelForControl(ThorContextIds.MAIN_MENU_LOAD_GAME, 3));
        assertEquals(R.string.thor_menu_shadow_of_death,
                ThorMainMenuState.labelForControl(ThorContextIds.MAIN_MENU_CAMPAIGN, 1));
        assertEquals(R.string.thor_lobby_back,
                ThorMainMenuState.labelForControl(ThorContextIds.MAIN_MENU_CAMPAIGN, 5));
        assertEquals(1, ThorMainMenuState.choiceCount(ThorContextIds.MAIN_MENU_CREDITS));
        assertEquals(R.string.thor_lobby_back,
                ThorMainMenuState.labelForControl(ThorContextIds.MAIN_MENU_CREDITS, 1));
        assertEquals(0, ThorMainMenuState.choiceCount(ThorContextIds.UNKNOWN));
        assertEquals(ThorActionIds.NONE, ThorMainMenuState.actionForControl(ThorContextIds.UNKNOWN, 1));
        assertEquals(ThorActionIds.NONE, ThorMainMenuState.actionForControl(ThorContextIds.MAIN_MENU_CREDITS, 2));
        for (int actionId = ThorActionIds.MAIN_MENU_CHOICE_1;
             actionId <= ThorActionIds.MAIN_MENU_CHOICE_5; ++actionId)
            assertFalse(ThorHapticState.isEligible(ThorContextIds.MAIN_MENU, actionId));
    }

    @Test
    public void controlsAreDisjointBelowHeaderAndOutsideTouchesAreInert()
    {
        for (final String context : CONTEXTS)
        {
            for (int control = 1; control <= 5; ++control)
            {
                final float[] bounds = ThorMainMenuState.boundsForControl(context, control, 1000f, 800f, 272f);
                assertTrue(bounds[0] > 0f && bounds[2] < 1000f);
                assertTrue(bounds[1] > 272f && bounds[3] < 800f);
                assertEquals(control, ThorMainMenuState.controlAt(context,
                        (bounds[0] + bounds[2]) / 2f, (bounds[1] + bounds[3]) / 2f,
                        1000f, 800f, 272f));
                if (control > 1)
                {
                    final float[] previous = ThorMainMenuState.boundsForControl(context,
                            control - 1, 1000f, 800f, 272f);
                    assertTrue(previous[3] < bounds[1]);
                }
            }
            assertEquals(ThorMainMenuState.CONTROL_NONE,
                    ThorMainMenuState.controlAt(context, 500f, 100f, 1000f, 800f, 272f));
            assertEquals(ThorMainMenuState.CONTROL_NONE,
                    ThorMainMenuState.controlAt(context, 10f, 600f, 1000f, 800f, 272f));
        }
        assertEquals(ThorMainMenuState.CONTROL_NONE,
                ThorMainMenuState.controlAt(ThorContextIds.UNKNOWN, 500f, 600f, 1000f, 800f, 272f));
    }

    @Test
    public void gestureRequiresSameEnabledControlPointerRevisionAndSession()
    {
        final ThorMainMenuGesture gesture = new ThorMainMenuGesture();
        gesture.begin(ThorContextIds.MAIN_MENU, 1, 10L, 4L, 7, true);
        assertTrue(gesture.finish(ThorContextIds.MAIN_MENU, 1, 1, 7, 10L, 4L, true, true));
        assertFalse(gesture.isActive());
        gesture.begin(ThorContextIds.MAIN_MENU, 1, 10L, 4L, 7, false);
        assertFalse(gesture.finish(ThorContextIds.MAIN_MENU, 1, 1, 7, 10L, 4L, true, true));
        gesture.begin(ThorContextIds.MAIN_MENU, 1, 10L, 4L, 7, true);
        assertFalse(gesture.finish(ThorContextIds.MAIN_MENU, 1, 1, 7, 10L, 4L, false, true));
        gesture.begin(ThorContextIds.MAIN_MENU, 1, 10L, 4L, 7, true);
        assertFalse(gesture.finish(ThorContextIds.MAIN_MENU, 2, 1, 7, 10L, 4L, true, true));
        gesture.begin(ThorContextIds.MAIN_MENU, 1, 10L, 4L, 7, true);
        assertFalse(gesture.finish(ThorContextIds.MAIN_MENU_NEW_GAME, 1, 1, 7, 10L, 4L, true, true));
        gesture.begin(ThorContextIds.MAIN_MENU, 1, 10L, 4L, 7, true);
        assertFalse(gesture.finish(ThorContextIds.MAIN_MENU, 1, 2, 7, 10L, 4L, true, true));
        gesture.begin(ThorContextIds.MAIN_MENU, 1, 10L, 4L, 7, true);
        assertFalse(gesture.finish(ThorContextIds.MAIN_MENU, 1, 1, 8, 10L, 4L, true, true));
        gesture.begin(ThorContextIds.MAIN_MENU, 1, 10L, 4L, 7, true);
        assertFalse(gesture.finish(ThorContextIds.MAIN_MENU, 1, 1, 7, 11L, 4L, true, true));
        gesture.begin(ThorContextIds.MAIN_MENU, 1, 10L, 4L, 7, true);
        assertFalse(gesture.finish(ThorContextIds.MAIN_MENU, 1, 1, 7, 10L, 5L, true, true));
        gesture.begin(ThorContextIds.MAIN_MENU, 1, 10L, 4L, 7, true);
        assertFalse(gesture.finish(ThorContextIds.MAIN_MENU, 1, 1, 7, 10L, 4L, true, false));
        gesture.begin(ThorContextIds.MAIN_MENU, 1, 10L, 4L, 7, true);
        gesture.cancel();
        assertFalse(gesture.finish(ThorContextIds.MAIN_MENU, 1, 1, 7, 10L, 4L, true, true));
    }
}
