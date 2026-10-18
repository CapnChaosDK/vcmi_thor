package eu.vcmi.vcmi;

import org.junit.Test;
import static org.junit.Assert.*;

public class ThorLobbyPlayerSetupTest
{
    private String[] labels() { return new String[]{"Red", "Player", "Random", "None", "Gold"}; }
    @Test public void boundedSlotsLabelsAndDefensiveCopies()
    {
        final int[] data = {7, 63}; final String[] labels = labels();
        final ThorLobbyPlayers state = ThorLobbyPlayers.copyOf(data, labels);
        data[0] = 8; labels[2] = "changed";
        assertEquals(7, state.slot(0)); assertEquals("Random", state.label(0, 2));
        assertEquals("None", state.label(0, 3)); assertEquals(-1, state.slot(1));
        assertSame(ThorLobbyPlayers.EMPTY, ThorLobbyPlayers.copyOf(data, labels));
        assertSame(ThorLobbyPlayers.EMPTY, ThorLobbyPlayers.copyOf(new int[]{0}, labels));
        assertSame(ThorLobbyPlayers.EMPTY, ThorLobbyPlayers.copyOf(new int[]{0, 128}, labels));
        assertSame(ThorLobbyPlayers.EMPTY, ThorLobbyPlayers.copyOf(new int[18], new String[45]));
        final String[] duplicate = new String[10]; java.util.Arrays.fill(duplicate, "x");
        assertSame(ThorLobbyPlayers.EMPTY, ThorLobbyPlayers.copyOf(new int[]{0, 15, 0, 15}, duplicate));
        labels[0] = new String(new char[257]).replace('\0', 'x');
        assertSame(ThorLobbyPlayers.EMPTY, ThorLobbyPlayers.copyOf(new int[]{0, 15}, labels));
        assertSame(ThorLobbyPlayers.EMPTY, ThorLobbyPlayers.copyOf(null, null));
    }
    @Test public void nativeCapabilitiesAndPendingNeverInventChoices()
    {
        for (int command = 6; command <= 12; ++command)
        {
            assertTrue(ThorLobbyPlayers.copyOf(new int[]{0, 15}, labels()).allows(0, command));
            assertFalse(ThorLobbyPlayers.copyOf(new int[]{0, 79}, labels()).allows(0, command));
            assertFalse(ThorLobbyPlayers.copyOf(new int[]{0, 0}, labels()).allows(0, command));
            assertArrayEquals(new int[]{1, command, 7}, ThorExtendedCommands.encode(1, command, 7));
            assertNull(ThorExtendedCommands.encode(1, command, 8));
            assertNull(ThorExtendedCommands.encode(1, command, -1));
        }
        assertFalse(ThorLobbyPlayers.copyOf(new int[]{0, 8}, labels()).allows(0, 8));
        assertTrue(ThorLobbyPlayers.copyOf(new int[]{0, 8}, labels()).allows(0, 12));
        assertFalse(ThorLobbyPlayers.EMPTY.allows(0, 6));
    }
    @Test public void geometryControlAndSlotNamespacesAreDistinct()
    {
        final ThorPresentationLayout layout = new ThorPresentationLayout(1080, 1240, 2.3f);
        final float width = layout.right - layout.margin, height = layout.bottom - layout.margin;
        for (int c = 1; c <= 13; ++c)
        {
            final float[] b = ThorLobbyPlayerControls.bounds(c, width, height);
            assertTrue((b[2] - b[0]) / 2.3f >= 48);
            if (c <= 2) assertTrue(b[2] <= width * .57f);
            assertTrue((b[3] - b[1]) / 2.3f >= 48);
            assertEquals(c, ThorLobbyPlayerControls.at((b[0]+b[2])/2, (b[1]+b[3])/2, width, height, true));
            assertEquals(0, ThorLobbyPlayerControls.at(b[0], b[1], width, height, true));
            if (c > 2) assertEquals(0, ThorLobbyPlayerControls.at((b[0]+b[2])/2, (b[1]+b[3])/2, width, height, false));
        }
        assertEquals(0, ThorLobbyPlayerControls.command(12));
        assertEquals(0, ThorLobbyPlayerControls.command(13));
        for (int c = 5; c <= 11; ++c) assertEquals(c+1, ThorLobbyPlayerControls.command(c));
    }
    @Test public void gesturesCancelAcrossScenarioRevisionOwnerAndDisplaySession()
    {
        final ThorControlGesture gesture = new ThorControlGesture(0);
        gesture.begin("setup", 6, 4, 1, 0, true);
        assertFalse(gesture.finish("setup", 6, 1, 0, 5, 1, true, true));
        gesture.begin("setup", 6, 4, 1, 0, true);
        assertFalse(gesture.finish("setup", 6, 1, 0, 4, 2, true, true));
        gesture.begin("setup", 6, 4, 1, 0, true); gesture.cancel();
        assertFalse(gesture.finish("setup", 6, 1, 0, 4, 1, true, true));
        gesture.begin("setup", 6, 4, 1, 0, true);
        assertTrue(gesture.finish("setup", 6, 1, 0, 4, 1, true, true));
        assertFalse(gesture.finish("setup", 6, 1, 0, 4, 1, true, true));
    }
}
