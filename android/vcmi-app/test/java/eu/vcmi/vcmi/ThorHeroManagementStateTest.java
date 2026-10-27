package eu.vcmi.vcmi;

import static org.junit.Assert.*;
import java.util.Arrays;
import org.junit.Test;

public final class ThorHeroManagementStateTest
{
    private static final long MASK = (1L << (ThorActionIds.HERO_WINDOW_TRANSFER_STACK - 1))
            | (1L << (ThorActionIds.HERO_WINDOW_TRANSFER_ARTIFACT - 1));

    private static ThorHeroManagement snapshot(int hero, int artifacts)
    {
        int[] ids = {3, 3, -1, -1, -1, -1, -1};
        int[] counts = {12, 4, 0, 0, 0, 0, 0};
        String[] names = {"Pikemen", "Pikemen", "", "", "", "", ""};
        int[] positions = new int[artifacts];
        int[] flags = new int[artifacts];
        String[] artifactNames = new String[artifacts];
        Arrays.fill(artifactNames, "");
        for (int i = 0; i < artifacts; ++i)
        {
            positions[i] = i;
            if (i >= ThorHeroMeetingArtifacts.EQUIPPED_PER_HERO) flags[i] = 4;
        }
        flags[0] = 1;
        flags[1] = 3;
        artifactNames[0] = "Sword";
        artifactNames[1] = "Locked";
        ThorHeroManagement result = ThorHeroManagement.copyOf(hero, "Hero", true,
                ids, counts, names, null, positions, flags, artifactNames, null);
        assertTrue(result.complete());
        return result;
    }

    private static ThorHeroManagementState state()
    {
        ThorHeroManagementState state = new ThorHeroManagementState();
        state.updateContext(10, ThorContextIds.HERO_WINDOW, MASK);
        state.update(snapshot(47, ThorHeroManagement.ARTIFACT_COUNT));
        return state;
    }

    private static boolean release(ThorHeroManagementState state)
    {
        state.begin(7, -1);
        assertTrue(state.acceptsPointer(7, 1));
        return state.finish(true, true);
    }

    @Test public void armySelectionMoveMergeSwapAndDeselectionUseExistingPairs()
    {
        ThorHeroManagementState s = state();
        s.selectTab(1);
        assertTrue(release(s));
        assertEquals(-1, s.armyPair(2)); // Empty slots cannot become a source.
        assertEquals(-1, s.armyPair(0));
        assertEquals(0, s.selectedArmy);
        assertTrue(release(s));
        assertEquals(ThorHeroManagementPair.encodeArmy(0, 1), s.armyPair(1));
        assertEquals(-1, s.selectedArmy);
        s.armyPair(0);
        assertEquals(ThorHeroManagementPair.encodeArmy(0, 2), s.armyPair(2));
        s.armyPair(0);
        assertEquals(-1, s.armyPair(0));
        assertEquals(-1, s.selectedArmy);
        assertEquals(-1, s.armyPair(7));
    }

    @Test public void artifactsRetainSourceAcrossPagesAndRejectLockedAndUnpublishedRows()
    {
        ThorHeroManagementState s = state();
        s.selectTab(2);
        assertEquals(-1, s.artifactPair(1));
        assertEquals(-1, s.selectedArtifact);
        s.artifactPair(0);
        s.changePage(1);
        assertEquals(0, s.selectedArtifact);
        assertEquals(ThorHeroManagementPair.encodeArtifact(0, 20), s.artifactPair(20));
        s.artifactPair(0);
        assertEquals(-1, s.artifactPair(1));
        assertEquals(-1, s.selectedArtifact);
        s.artifactPair(0);
        assertEquals(-1, s.artifactPair(0));
        s.update(snapshot(47, 19));
        assertEquals(-1, s.artifactPair(19));
        assertEquals(-1, s.artifactPair(ThorHeroManagement.ARTIFACT_COUNT));
    }

    @Test public void sameHeroRefreshRetainsTabAndClampsPageButCancelsOldInput()
    {
        ThorHeroManagementState s = state();
        s.selectTab(2);
        s.changePage(100);
        assertEquals(13, s.page);
        s.artifactPair(0);
        s.begin(7, -1);
        s.updateContext(11, ThorContextIds.HERO_WINDOW, MASK);
        assertFalse(s.finish(true, true));
        assertFalse(s.management.complete());
        s.update(snapshot(47, 19));
        assertEquals(2, s.tab);
        assertEquals(3, s.page);
        assertEquals(-1, s.selectedArtifact);
        s.changePage(-100);
        assertEquals(0, s.page);
    }

    @Test public void switchingHeroOrLeavingContextResetsTabsAndPages()
    {
        ThorHeroManagementState s = state();
        s.selectTab(2);
        s.changePage(2);
        s.update(snapshot(48, 19));
        assertEquals(0, s.tab);
        assertEquals(0, s.page);
        s.selectTab(1);
        s.begin(7, 2);
        s.updateContext(11, ThorContextIds.TOWN_WINDOW, MASK);
        assertFalse(s.finish(true, true));
        assertEquals(0, s.tab);
        s.update(snapshot(48, 19));
        assertFalse(s.management.complete());
    }

    @Test public void payloadRefreshWithinRevisionInvalidatesHeldGesture()
    {
        ThorHeroManagementState s = state();
        s.begin(7, 1);
        s.update(snapshot(47, 19));
        assertFalse(s.acceptsPointer(7, 1));
        assertFalse(s.finish(true, true));
        assertTrue(release(s));
        assertFalse(s.finish(true, true)); // A release is consumed once.
    }

    @Test public void pointerChangesCancellationAndExpiredSessionCannotComplete()
    {
        ThorHeroManagementState s = state();
        s.begin(7, 1);
        assertFalse(s.acceptsPointer(8, 1));
        assertFalse(s.acceptsPointer(7, 1));
        assertFalse(s.finish(true, true));
        s.begin(7, 1);
        assertFalse(s.acceptsPointer(7, 2));
        assertFalse(s.finish(true, true));
        s.begin(7, 1);
        assertFalse(s.finish(false, true));
        s.begin(7, 1);
        assertFalse(s.finish(true, false));
        assertTrue(release(s));
    }

    @Test public void lifecycleCancellationClearsSelectionsAndWithdrawsSnapshot()
    {
        ThorHeroManagementState s = state();
        s.selectTab(2);
        s.changePage(1);
        s.artifactPair(0);
        s.begin(7, 1);
        s.cancel(); // Resize, visibility and detach share this boundary.
        assertFalse(s.finish(true, true));
        assertEquals(-1, s.selectedArtifact);
        assertEquals(2, s.tab);
        assertEquals(1, s.page);
        s.clear(); // Pause/display recreation also withdraws the snapshot.
        assertFalse(s.management.complete());
        assertEquals(-1, s.artifactPair(0));
        s.update(snapshot(47, 83));
        assertTrue(release(s));
        assertEquals(2, s.tab);
    }

    @Test public void pendingDisabledAndMalformedStatesCannotProduceActions()
    {
        ThorHeroManagementState s = state();
        s.armyPair(0);
        s.updateContext(10, ThorContextIds.HERO_WINDOW, 0);
        assertEquals(-1, s.armyPair(1));
        s.artifactPair(0);
        assertEquals(-1, s.artifactPair(2));
        s.update(ThorHeroManagement.EMPTY);
        assertFalse(s.management.complete());
        assertEquals(0, s.tab);
        assertEquals(-1, s.armyPair(0));
        assertEquals(-1, s.artifactPair(0));
    }

    @Test public void tabChangesClearBothSourcesAndInvalidTabsAreInert()
    {
        ThorHeroManagementState s = state();
        s.armyPair(0);
        s.selectTab(2);
        assertEquals(-1, s.selectedArmy);
        s.artifactPair(0);
        s.selectTab(0);
        assertEquals(-1, s.selectedArtifact);
        s.selectTab(3);
        assertEquals(0, s.tab);
    }
}
