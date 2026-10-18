package eu.vcmi.vcmi;

import org.junit.Test;
import static org.junit.Assert.*;

public class ThorBattleControlsTest
{
    @Test public void versionTargetsAndBoundedAvailability()
    {
        assertArrayEquals(new int[]{1, 5, -1}, ThorExtendedCommands.encode(1, 5, -1));
        assertNull(ThorExtendedCommands.encode(2, 5, -1));
        assertNull(ThorExtendedCommands.encode(1, 6, -1));
        assertNull(ThorExtendedCommands.encode(1, 5, 0));
        assertSame(ThorExtendedCommands.EMPTY, ThorExtendedCommands.copyOf(2, new int[]{1}, new int[0]));
        assertSame(ThorExtendedCommands.EMPTY, ThorExtendedCommands.copyOf(1, null, new int[0]));
        assertSame(ThorExtendedCommands.EMPTY, ThorExtendedCommands.copyOf(1, new int[17], new int[0]));
        assertSame(ThorExtendedCommands.EMPTY, ThorExtendedCommands.copyOf(1, new int[]{1,1}, new int[0]));
        assertSame(ThorExtendedCommands.EMPTY, ThorExtendedCommands.copyOf(1, new int[]{6}, new int[0]));
        final int[] source = {1,3,4,5};
        final ThorExtendedCommands state = ThorExtendedCommands.copyOf(1, source, new int[0]);
        source[0] = 2;
        assertTrue(state.allows(1)); assertFalse(state.allows(2));
        assertEquals(Long.MIN_VALUE, ThorActionIds.maskFor(64));
        assertEquals(0, ThorActionIds.maskFor(65));
    }

    @Test public void autoStateAndLegacyControlsStaySeparate()
    {
        final ThorExtendedCommands running = ThorExtendedCommands.copyOf(1, new int[]{2}, new int[]{1});
        assertEquals(2, ThorBattleControls.command(3, running));
        assertTrue(ThorBattleControls.enabled(ThorContextIds.BATTLE, 3, 0, running));
        final ThorExtendedCommands unstoppable = ThorExtendedCommands.copyOf(1, new int[0], new int[]{1});
        assertEquals(2, ThorBattleControls.command(3, unstoppable));
        assertFalse(ThorBattleControls.enabled(ThorContextIds.BATTLE, 3, 0, unstoppable));
        assertEquals(1, ThorBattleControls.command(3, ThorExtendedCommands.EMPTY));
        assertEquals(9, ThorBattleControls.legacy(ThorContextIds.BATTLE, 1));
        assertEquals(10, ThorBattleControls.legacy(ThorContextIds.BATTLE, 2));
        assertEquals(11, ThorBattleControls.legacy(ThorContextIds.BATTLE_TACTICS, 1));
        assertEquals(12, ThorBattleControls.legacy(ThorContextIds.BATTLE_TACTICS, 2));
        assertFalse(ThorBattleControls.enabled(ThorContextIds.BATTLE_TACTICS, 3, -1L, running));
        assertFalse(ThorBattleControls.enabled(ThorContextIds.UNKNOWN, 1, -1L, running));
        assertEquals(0, ThorBattleControls.legacy(ThorContextIds.BATTLE, 3));
    }

    @Test public void sixTouchSizedButtonsUseTheSameHitGeometry()
    {
        final ThorPresentationLayout layout = new ThorPresentationLayout(1080,1240,2.3f);
        for (int control = 1; control <= 6; ++control)
        {
            final float[] b = ThorBattleControls.bounds(control, ThorContextIds.BATTLE, layout);
            assertTrue((b[2]-b[0])/2.3f >= 48);
            assertTrue((b[3]-b[1])/2.3f >= 48);
            assertEquals(control, ThorBattleControls.controlAt((b[0]+b[2])/2, (b[1]+b[3])/2,
                    ThorContextIds.BATTLE, layout));
        }
        assertEquals(0, ThorBattleControls.controlAt(0,0, ThorContextIds.BATTLE, layout));
    }

    @Test public void gesturesRejectStaleCrossContextDuplicateAndRecreatedInput()
    {
        final ThorControlGesture tap = new ThorControlGesture(0);
        tap.begin(ThorContextIds.BATTLE,4,7,1,0,true);
        assertFalse(tap.finish(ThorContextIds.BATTLE,4,1,0,8,1,true,true));
        tap.begin(ThorContextIds.BATTLE,4,7,1,0,true);
        assertFalse(tap.finish(ThorContextIds.UNKNOWN,4,1,0,7,1,true,true));
        tap.begin(ThorContextIds.BATTLE,4,7,1,0,true);
        assertFalse(tap.finish(ThorContextIds.BATTLE,4,1,0,7,2,true,true));
        tap.begin(ThorContextIds.BATTLE,4,7,1,0,true); tap.cancel();
        assertFalse(tap.finish(ThorContextIds.BATTLE,4,1,0,7,1,true,true));
        tap.begin(ThorContextIds.BATTLE,4,7,1,0,true);
        assertFalse(tap.finish(ThorContextIds.BATTLE,4,2,0,7,1,true,true));
        tap.begin(ThorContextIds.BATTLE,4,7,1,0,true);
        assertTrue(tap.finish(ThorContextIds.BATTLE,4,1,0,7,1,true,true));
        assertFalse(tap.finish(ThorContextIds.BATTLE,4,1,0,7,1,true,true));
    }

    @Test public void commandFeedbackRequiresNativeAcceptanceAndLiveSession()
    {
        final ThorHapticState haptic = new ThorHapticState(true);
        haptic.updateContext(7, ThorContextIds.BATTLE);
        assertFalse(haptic.accept(7,7,-1,1,true));
        haptic.registerSubmission(7,-1);
        final long token = haptic.peekSubmissionToken(7,-1);
        assertTrue(haptic.accept(7,7,-1,token,true));
        assertFalse(haptic.accept(7,7,-1,token,true));
        haptic.registerSubmission(7,-2); haptic.resetTransientState();
        assertFalse(haptic.accept(7,7,-2,token,true));
        assertFalse(ThorHapticState.isEligible(ThorContextIds.BATTLE,-6));
        assertFalse(ThorHapticState.isEligible(ThorContextIds.BATTLE_TACTICS,-1));
    }
}
