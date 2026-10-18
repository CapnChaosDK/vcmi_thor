package eu.vcmi.vcmi;

import android.view.MotionEvent;
import org.junit.Test;

import static org.junit.Assert.*;

public class ThorMainMenuDeckTest
{
    private final ThorMainMenuDeck deck = new ThorMainMenuDeck();
    private final ThorPresentationLayout layout = new ThorPresentationLayout(1080, 1240, 2.30625f);
    private final long mask = ThorActionIds.maskFor(ThorActionIds.MAIN_MENU_CHOICE_1);

    private int touch(final int event, final int control, final long revision, final long session,
                      final long enabled, final int pointers, final int pointerId, final boolean current)
    {
        final float[] bounds = ThorMainMenuState.boundsForControl(ThorContextIds.MAIN_MENU, control,
                layout.right - layout.margin, layout.bottom - layout.margin,
                (layout.bottom - layout.margin) * 0.34f);
        return deck.handleTouch(event, layout.margin + (bounds[0] + bounds[2]) * 0.5f,
                layout.margin + (bounds[1] + bounds[3]) * 0.5f, pointers, pointerId,
                ThorContextIds.MAIN_MENU, revision, session, enabled, layout, current);
    }

    private void down()
    {
        assertEquals(ThorActionIds.NONE, touch(MotionEvent.ACTION_DOWN, 1, 7, 3, mask, 1, 0, true));
    }

    @Test
    public void renderedMenuFamilyRowsHitTheirOwnControlAcrossDisplays()
    {
        for (final String context : new String[]{ThorContextIds.MAIN_MENU, ThorContextIds.MAIN_MENU_NEW_GAME,
                ThorContextIds.MAIN_MENU_LOAD_GAME, ThorContextIds.MAIN_MENU_CAMPAIGN,
                ThorContextIds.MULTI_MODE_NEW_GAME, ThorContextIds.MULTI_MODE_LOAD_GAME,
                ThorContextIds.HIGH_SCORES, ThorContextIds.TUTORIAL_MISSING_DIALOG})
            for (final int[] size : new int[][]{{1080, 1240}, {540, 620}, {1920, 1080}, {997, 1231}})
                for (final float density : new float[]{1f, 2.30625f, 3.5f})
                {
                    final ThorPresentationLayout geometry = new ThorPresentationLayout(size[0], size[1], density);
                    final float width = geometry.right - geometry.margin;
                    final float height = geometry.bottom - geometry.margin;
                    // Drawing computes the absolute divider before converting back to local coordinates.
                    final float divider = geometry.margin + geometry.contentHeight * 0.34f - geometry.margin;
                    for (int control = 1; control <= ThorMainMenuState.choiceCount(context); ++control)
                    {
                        final float[] bounds = ThorMainMenuState.boundsForControl(context, control, width, height, divider);
                        assertEquals(control, deck.controlAt(context,
                                geometry.margin + (bounds[0] + bounds[2]) * 0.5f,
                                geometry.margin + (bounds[1] + bounds[3]) * 0.5f, geometry));
                    }
                    assertEquals(ThorMainMenuState.CONTROL_NONE, deck.controlAt(context, 0f, 0f, geometry));
                    assertEquals(ThorMainMenuState.CONTROL_NONE,
                            deck.controlAt(context, geometry.margin + width * 0.5f, geometry.margin, geometry));
                }
    }

    @Test
    public void acceptedTapReturnsOneSemanticActionAndMoveRetainsExistingContract()
    {
        down();
        assertEquals(ThorActionIds.NONE, touch(MotionEvent.ACTION_MOVE, 2, 7, 3, mask, 1, 0, true));
        assertEquals(ThorActionIds.MAIN_MENU_CHOICE_1,
                touch(MotionEvent.ACTION_UP, 1, 7, 3, mask, 1, 0, true));
        assertEquals(ThorActionIds.NONE, touch(MotionEvent.ACTION_UP, 1, 7, 3, mask, 1, 0, true));
    }

    @Test
    public void lifecycleAndChangedInputCannotSubmitAnOldTap()
    {
        down();
        deck.cancel();
        assertEquals(ThorActionIds.NONE, touch(MotionEvent.ACTION_UP, 1, 7, 3, mask, 1, 0, true));
        for (int changed = 0; changed < 7; ++changed)
        {
            down();
            assertEquals(ThorActionIds.NONE, touch(MotionEvent.ACTION_UP, changed == 0 ? 2 : 1,
                    changed == 1 ? 8 : 7, changed == 2 ? 4 : 3, changed == 3 ? 0L : mask,
                    changed == 4 ? 2 : 1, changed == 5 ? 1 : 0, changed != 6));
        }
        down();
        touch(MotionEvent.ACTION_CANCEL, 1, 7, 3, mask, 1, 0, true);
        assertEquals(ThorActionIds.NONE, touch(MotionEvent.ACTION_UP, 1, 7, 3, mask, 1, 0, true));
        down();
        touch(MotionEvent.ACTION_POINTER_DOWN, 1, 7, 3, mask, 2, 0, true);
        assertEquals(ThorActionIds.NONE, touch(MotionEvent.ACTION_UP, 1, 7, 3, mask, 1, 0, true));
        touch(MotionEvent.ACTION_DOWN, 1, 7, 3, 0L, 1, 0, true);
        assertEquals(ThorActionIds.NONE, touch(MotionEvent.ACTION_UP, 1, 7, 3, mask, 1, 0, true));
    }
}
