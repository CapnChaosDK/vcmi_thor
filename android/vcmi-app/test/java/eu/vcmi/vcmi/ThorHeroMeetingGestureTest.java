package eu.vcmi.vcmi;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ThorHeroMeetingGestureTest
{
    @Test
    public void movementUnderTouchSlopRemainsATap()
    {
        final ThorHeroMeetingGesture gesture = new ThorHeroMeetingGesture();
        assertTrue(gesture.begin(8, 2, true, 20f, 30f));
        assertFalse(gesture.move(23f, 34f, 10f, 8, true));
        final ThorHeroMeetingGesture.Result result = gesture.finish(8, true, 2);
        assertEquals(ThorHeroMeetingGesture.Kind.TAP, result.kind);
        assertEquals(2, result.sourceKey);

        final ThorHeroMeetingGesture nearRowEdge = new ThorHeroMeetingGesture();
        nearRowEdge.begin(8, 2, true, 20f, 30f);
        nearRowEdge.move(23f, 34f, 10f, 8, true);
        final ThorHeroMeetingGesture.Result edgeResult = nearRowEdge.finish(8, true, 9);
        assertEquals(ThorHeroMeetingGesture.Kind.TAP, edgeResult.kind);
        assertEquals(2, edgeResult.sourceKey); // Quick tap uses the pressed source, even at a row edge.
        assertEquals(9, edgeResult.destinationKey);
    }

    @Test
    public void crossingTouchSlopProducesOneOppositeSideDrop()
    {
        final ThorHeroMeetingGesture gesture = new ThorHeroMeetingGesture();
        assertTrue(gesture.begin(9, 3, true, 10f, 10f));
        assertTrue(gesture.move(21f, 10f, 10f, 9, true));
        final ThorHeroMeetingGesture.Result result = gesture.finish(9, true, 10);
        assertEquals(ThorHeroMeetingGesture.Kind.DROP, result.kind);
        assertEquals(3, result.sourceKey);
        assertEquals(10, result.destinationKey);
        assertEquals(ThorHeroMeetingGesture.Kind.CANCELLED, gesture.finish(9, true, 10).kind);
    }

    @Test
    public void sameSideInvalidAndCancelledDropsProduceNoAction()
    {
        final ThorHeroMeetingGesture sameSide = new ThorHeroMeetingGesture();
        sameSide.begin(10, 1, true, 0f, 0f);
        sameSide.move(12f, 0f, 1f, 10, true);
        assertEquals(ThorHeroMeetingGesture.Kind.CANCELLED, sameSide.finish(10, true, 6).kind);

        final ThorHeroMeetingGesture invalid = new ThorHeroMeetingGesture();
        invalid.begin(10, 8, true, 0f, 0f);
        invalid.move(12f, 0f, 1f, 10, true);
        assertEquals(ThorHeroMeetingGesture.Kind.CANCELLED, invalid.finish(10, true, -1).kind);

        final ThorHeroMeetingGesture cancelled = new ThorHeroMeetingGesture();
        cancelled.begin(10, 8, true, 0f, 0f);
        cancelled.move(12f, 0f, 1f, 10, true);
        cancelled.cancel();
        assertEquals(ThorHeroMeetingGesture.Kind.CANCELLED, cancelled.finish(10, true, 1).kind);
    }

    @Test
    public void emptyOrLockedSourceRowsCannotArm()
    {
        final ThorHeroMeetingGesture gesture = new ThorHeroMeetingGesture();
        assertFalse(gesture.begin(10, 3, false, 0f, 0f));
        assertFalse(gesture.begin(10, -1, true, 0f, 0f));
        assertFalse(gesture.begin(0, 3, true, 0f, 0f));
    }

    @Test
    public void revisionOrContextChangeCancelsTheGesture()
    {
        final ThorHeroMeetingGesture changedRevision = new ThorHeroMeetingGesture();
        changedRevision.begin(11, 0, true, 0f, 0f);
        changedRevision.move(20f, 0f, 1f, 12, true);
        assertFalse(changedRevision.isArmed());

        final ThorHeroMeetingGesture changedContext = new ThorHeroMeetingGesture();
        changedContext.begin(11, 0, true, 0f, 0f);
        changedContext.move(20f, 0f, 1f, 11, false);
        assertFalse(changedContext.isArmed());
    }

    @Test
    public void sameRevisionActionStateRefreshRetainsMeetingArmies()
    {
        assertTrue(ThorHeroMeetingGesture.retainsArmies(21, ThorContextIds.HERO_MEETING,
                21, ThorContextIds.HERO_MEETING));
        assertFalse(ThorHeroMeetingGesture.retainsArmies(21, ThorContextIds.HERO_MEETING,
                22, ThorContextIds.HERO_MEETING));
        assertFalse(ThorHeroMeetingGesture.retainsArmies(21, ThorContextIds.HERO_MEETING,
                21, ThorContextIds.ADVENTURE_MAP));
        assertFalse(ThorHeroMeetingGesture.retainsArmies(21, ThorContextIds.ADVENTURE_MAP,
                21, ThorContextIds.HERO_MEETING));
    }

    @Test
    public void stationaryLongPressSuppressesTapWhileMovementWinsBeforeActivation()
    {
        final ThorHeroMeetingGesture stationary = new ThorHeroMeetingGesture();
        stationary.begin(22, 0, true, 10f, 10f);
        assertTrue(stationary.activateLongPress(22, true));
        assertTrue(stationary.isLongPressed());
        assertEquals(ThorHeroMeetingGesture.Kind.LONG_PRESS, stationary.finish(22, true, 0).kind);

        final ThorHeroMeetingGesture moved = new ThorHeroMeetingGesture();
        moved.begin(22, 0, true, 10f, 10f);
        assertTrue(moved.move(21f, 10f, 10f, 22, true));
        assertFalse(moved.activateLongPress(22, true));
        assertEquals(ThorHeroMeetingGesture.Kind.DROP, moved.finish(22, true, 7).kind);

        final ThorHeroMeetingGesture boundary = new ThorHeroMeetingGesture();
        boundary.begin(22, 0, true, 10f, 10f);
        assertFalse(boundary.move(20f, 10f, 10f, 22, true));
        assertTrue(boundary.activateLongPress(22, true));
        boundary.cancel();
        assertEquals(ThorHeroMeetingGesture.Kind.CANCELLED, boundary.finish(22, true, 7).kind);
    }
}
