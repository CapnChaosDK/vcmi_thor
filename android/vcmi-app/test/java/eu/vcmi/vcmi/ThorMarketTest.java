package eu.vcmi.vcmi;

import org.junit.Test;
import static org.junit.Assert.*;

public class ThorMarketTest
{
    private int[] quote()
    {
        return new int[]{1,0,1,0,6,10,1,2,10,2, 0,100,1,1, 6,50,1,1};
    }
    private ThorMarketState state(final int[] quote)
    { return ThorMarketState.copyOf(7, quote, new String[]{"Wood", "Gold"}); }

    @Test public void completeQuoteQuantityAndPendingJourney()
    {
        final int[] data = quote();
        ThorMarketState state = state(data);
        assertTrue(state.allows(13,0)); assertTrue(state.allows(14,6));
        assertEquals(10, ThorMarketControls.target(ThorMarketControls.MAX, state));
        assertEquals(0, ThorMarketControls.target(ThorMarketControls.LESS_TEN, state));
        assertEquals(3, ThorMarketControls.target(ThorMarketControls.MORE, state));
        assertTrue(state.allows(15,10)); assertFalse(state.allows(15,11));
        assertTrue(state.allows(16,-1));
        data[1] = 1;
        state = state(data);
        for (int command : new int[]{13,14,15,16,18})
            assertFalse(state.allows(command, command < 16 ? 0 : -1));
        assertTrue(state.allows(17,-1));
        data[1] = 2; data[7] = 0;
        state = state(data);
        assertFalse(state.allows(16,-1)); assertTrue(state.allows(18,-1));
        data[1] = 3; data[2] = 0;
        state = state(data);
        assertFalse(state.allows(13,0)); assertTrue(state.allows(17,-1));
    }
    @Test public void immutableBoundedAndMalformedSnapshotsFailClosed()
    {
        final int[] data = quote(); final String[] names = {"Wood", "Gold"};
        final ThorMarketState state = ThorMarketState.copyOf(7,data,names);
        data[11] = 0; names[0] = "changed";
        assertEquals(100,state.balance(0)); assertEquals("Wood",state.name(0));
        for (int index : new int[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13})
        {
            final int[] bad = quote(); bad[index] = -2;
            assertSame("field " + index, ThorMarketState.EMPTY, state(bad));
        }
        final int[] duplicate = quote(); duplicate[14] = duplicate[10];
        assertSame(ThorMarketState.EMPTY,state(duplicate));
        final int[] overflow = quote(); overflow[5] = Integer.MAX_VALUE;
        assertSame(ThorMarketState.EMPTY,state(overflow));
        final int[] same = quote(); same[4] = same[3];
        assertSame(ThorMarketState.EMPTY,state(same));
        assertSame(ThorMarketState.EMPTY,ThorMarketState.copyOf(7,quote(),new String[]{null,"Gold"}));
        assertSame(ThorMarketState.EMPTY,ThorMarketState.copyOf(7,quote(),new String[]{"x".repeat(257),"Gold"}));
        assertSame(ThorMarketState.EMPTY,ThorMarketState.copyOf(0,quote(),names));
        assertSame(ThorMarketState.EMPTY,ThorMarketState.copyOf(7,null,names));
    }
    @Test public void resourceIdsAndFootersUseSeparateControlRanges()
    {
        final ThorMarketState state = state(quote());
        assertFalse(ThorMarketControls.enabled(9,state));
        assertFalse(ThorMarketControls.enabled(18,state));
        assertEquals(16,ThorMarketControls.command(24));
        assertEquals(-1,ThorMarketControls.target(24,state));
        assertTrue(ThorMarketControls.enabled(24,state));
        assertTrue(ThorMarketControls.enabled(26,state));
        assertFalse(ThorMarketControls.enabled(0,state));
        assertFalse(ThorMarketControls.enabled(27,state,0));
        assertFalse(ThorMarketControls.enabled(28,state,0));
        assertEquals(0,ThorMarketControls.command(27));
        assertEquals(0,ThorMarketControls.command(28));
        assertEquals(-1,ThorMarketControls.target(28,state));
        assertNull(ThorExtendedCommands.encode(1,16,0));
        assertNull(ThorExtendedCommands.encode(1,15,-1));
        assertNull(ThorExtendedCommands.encode(2,13,0));
        assertArrayEquals(new int[]{1,13,6},ThorExtendedCommands.encode(1,13,6));
        assertSame(ThorExtendedCommands.EMPTY,ThorExtendedCommands.copyOf(1,new int[]{13},new int[0]));
    }
    @Test public void pagedControlsAreTouchSizedAndDoNotOverlap()
    {
        final ThorPresentationLayout layout = new ThorPresentationLayout(1080,1240,2.3f);
        for (int page = 0; page < 3; ++page)
            for (int control = 1; control <= 28; ++control)
            {
                final float[] b = ThorMarketControls.bounds(control,layout,9,page);
                if (b == null) continue;
                assertTrue("width " + control,(b[2]-b[0])/2.3f >= 48);
                assertTrue("height " + control,(b[3]-b[1])/2.3f >= 48);
                assertEquals(control,ThorMarketControls.at((b[0]+b[2])/2,(b[1]+b[3])/2,layout,9,page));
                for (int other = control+1; other <= 28; ++other)
                {
                    final float[] c = ThorMarketControls.bounds(other,layout,9,page);
                    if (c != null) assertFalse("overlap " + control + "/" + other,
                            b[0] < c[2] && c[0] < b[2] && b[1] < c[3] && c[1] < b[3]);
                }
            }
        assertNull(ThorMarketControls.bounds(1,layout,9,1));
        final int[] data = new int[10 + 9 * 4];
        System.arraycopy(quote(),0,data,0,10); data[9] = 9;
        final String[] names = new String[9];
        for (int i = 0; i < 9; ++i)
        { data[10+i*4] = i; data[11+i*4] = 100; data[12+i*4] = data[13+i*4] = 1; names[i] = "Resource " + i; }
        final ThorMarketState paged = ThorMarketState.copyOf(7,data,names);
        assertTrue(ThorMarketControls.enabled(28,paged,0));
        assertTrue(ThorMarketControls.enabled(27,paged,1));
        assertFalse(ThorMarketControls.enabled(28,paged,2));
        assertEquals(0,ThorMarketControls.at(0,0,layout,9,0));
    }
    @Test public void staleHeldDraggedAndMultiplePointersNeverSubmit()
    {
        final ThorMarketGesture gesture = new ThorMarketGesture();
        gesture.begin(24,7,1,0,true,10,10,100);
        assertTrue(gesture.finish(24,7,1,1,0,true,true,10,10,200,8));
        assertFalse(gesture.finish(24,7,1,1,0,true,true,10,10,200,8));
        gesture.begin(24,7,1,0,true,10,10,100);
        assertFalse(gesture.finish(24,8,1,1,0,true,true,10,10,200,8));
        gesture.begin(24,7,1,0,true,10,10,100);
        assertFalse(gesture.finish(24,7,2,1,0,true,true,10,10,200,8));
        gesture.begin(24,7,1,0,true,10,10,100);
        assertFalse(gesture.finish(24,7,1,2,0,true,true,10,10,200,8));
        gesture.begin(24,7,1,0,true,10,10,100);
        assertFalse(gesture.finish(24,7,1,1,0,true,true,10,10,900,8));
        gesture.begin(24,7,1,0,true,10,10,100);
        gesture.move(30,10,150,8);
        assertFalse(gesture.finish(24,7,1,1,0,true,true,10,10,200,8));
        gesture.begin(24,7,1,0,true,10,10,100);
        gesture.cancel();
        assertFalse(gesture.finish(24,7,1,1,0,true,true,10,10,200,8));
    }

    @Test public void onlySuccessfulRegisteredTradeFeedbackCanSurviveQuoteRefresh()
    {
        final ThorHapticState feedback = new ThorHapticState(true);
        feedback.updateContext(7,ThorContextIds.RESOURCE_MARKET);
        feedback.registerSubmission(7,-16);
        final long token = feedback.peekSubmissionToken(7,-16);
        assertTrue(token > 0);
        feedback.updateContext(8,ThorContextIds.RESOURCE_MARKET);
        assertTrue(feedback.accept(7,7,-16,token,true));
        assertFalse(feedback.accept(7,7,-16,token,true));
        assertFalse(ThorHapticState.isEligible(ThorContextIds.RESOURCE_MARKET,-15));
        feedback.registerSubmission(8,-16);
        final long detached = feedback.peekSubmissionToken(8,-16);
        feedback.resetTransientState();
        assertFalse(feedback.accept(8,8,-16,detached,true));
        feedback.registerSubmission(8,-16);
        final long closed = feedback.peekSubmissionToken(8,-16);
        feedback.updateContext(9,ThorContextIds.TOWN_WINDOW);
        assertFalse(feedback.accept(8,8,-16,closed,true));
    }
}
