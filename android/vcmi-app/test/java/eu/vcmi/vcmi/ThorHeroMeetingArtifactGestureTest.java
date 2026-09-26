package eu.vcmi.vcmi;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ThorHeroMeetingArtifactGestureTest
{
    @Test
    public void shortMovementRetainsTwoTapSelection()
    {
        final ThorHeroMeetingArtifactGesture gesture = new ThorHeroMeetingArtifactGesture();
        assertTrue(gesture.begin(10, 3, true, 20f, 30f));
        assertFalse(gesture.move(23f, 34f, 10f, 10, true));
        assertEquals(ThorHeroMeetingArtifactGesture.Kind.TAP, gesture.finish(10, true, 27));
    }

    @Test
    public void dragUsesExactOppositeHeroPair()
    {
        final ThorHeroMeetingArtifactGesture gesture = new ThorHeroMeetingArtifactGesture();
        assertTrue(gesture.begin(10, 3, true, 0f, 0f));
        assertTrue(gesture.move(12f, 0f, 10f, 10, true));
        assertEquals(3, gesture.sourceKey());
        assertEquals(ThorHeroMeetingArtifactGesture.Kind.DROP, gesture.finish(10, true, 27));
        assertEquals(ThorHeroMeetingArtifactPair.INVALID, ThorHeroMeetingArtifactPair.encode(3, 4));
        assertEquals(3 * ThorHeroMeetingArtifacts.SLOT_COUNT + 27,
                ThorHeroMeetingArtifactPair.encode(3, 27));
        assertEquals(ThorHeroMeetingArtifactGesture.Kind.CANCELLED, gesture.finish(10, true, 27));
    }

    @Test
    public void invalidAndSameSideDropsCancel()
    {
        for (final int destination : new int[]{-1, 4, 48})
        {
            final ThorHeroMeetingArtifactGesture gesture = new ThorHeroMeetingArtifactGesture();
            gesture.begin(10, 3, true, 0f, 0f);
            gesture.move(11f, 0f, 10f, 10, true);
            assertEquals(ThorHeroMeetingArtifactGesture.Kind.CANCELLED,
                    gesture.finish(10, true, destination));
        }
    }

    @Test
    public void cancellationAndRevisionOrModeChangeSuppressSubmission()
    {
        final ThorHeroMeetingArtifactGesture gesture = new ThorHeroMeetingArtifactGesture();
        assertFalse(gesture.begin(10, 3, false, 0f, 0f));
        gesture.begin(10, 3, true, 0f, 0f);
        assertFalse(gesture.move(20f, 0f, 10f, 11, true));
        assertEquals(ThorHeroMeetingArtifactGesture.Kind.CANCELLED, gesture.finish(10, true, 27));
        gesture.begin(10, 3, true, 0f, 0f);
        assertFalse(gesture.move(20f, 0f, 10f, 10, false));
        assertEquals(ThorHeroMeetingArtifactGesture.Kind.CANCELLED, gesture.finish(10, true, 27));
        gesture.begin(10, 3, true, 0f, 0f);
        gesture.cancel();
        assertEquals(ThorHeroMeetingArtifactGesture.Kind.CANCELLED, gesture.finish(10, true, 27));
    }

    @Test
    public void bulkActionsHaveStableIdsAndMasks()
    {
        assertEquals(21, ThorActionIds.HERO_MEETING_TRANSFER_ARTIFACT);
        assertEquals(22, ThorActionIds.HERO_MEETING_REDISTRIBUTE_STACK);
        assertEquals(23, ThorActionIds.HERO_MEETING_ARTIFACTS_LEFT_TO_RIGHT);
        assertEquals(24, ThorActionIds.HERO_MEETING_ARTIFACTS_RIGHT_TO_LEFT);
        assertEquals(25, ThorActionIds.HERO_MEETING_SWAP_ARTIFACTS);
        assertEquals(23, ThorActionIds.artifactBulkActionForButton(0));
        assertEquals(25, ThorActionIds.artifactBulkActionForButton(1));
        assertEquals(24, ThorActionIds.artifactBulkActionForButton(2));
        assertEquals(ThorActionIds.NONE, ThorActionIds.artifactBulkActionForButton(3));
        for (int action = 1; action <= 25; ++action)
            assertEquals(1 << (action - 1), ThorActionIds.maskFor(action));
        assertEquals(0, ThorActionIds.maskFor(26));
    }
}
