package eu.vcmi.vcmi;

import org.junit.Test;
import static org.junit.Assert.*;

public class ThorReferenceTest
{
    private static int[] page() { return new int[]{1, 1, 0, 2, 0, 0, 3, 15, 0, 0, 2, 0, 0, 1}; }
    private static ThorReferenceState journal(long revision)
    { return ThorReferenceState.copyOf(revision, page(), new String[]{"Requirements 😀", "Quest A", "Quest B"}, new byte[0]); }
    @Test public void visibleRowsTextAndNativeAvailability()
    {
        ThorReferenceState s = journal(7);
        assertNotSame(ThorReferenceState.EMPTY, s);
        assertTrue(s.allows(ThorExtendedCommands.REFERENCE_SELECT, 1));
        assertFalse(s.allows(ThorExtendedCommands.REFERENCE_SELECT, 5));
        assertFalse(s.allows(ThorExtendedCommands.REFERENCE_PREVIOUS, -1));
        assertTrue(s.allows(ThorExtendedCommands.REFERENCE_TEXT_NEXT, -1));
        assertTrue(s.allows(ThorExtendedCommands.REFERENCE_CENTER, -1));
        assertTrue(s.allows(ThorExtendedCommands.REFERENCE_EVENTS, -1));
        assertFalse(s.allows(ThorExtendedCommands.REFERENCE_QUESTS, -1));
        assertFalse(s.allows(ThorExtendedCommands.REFERENCE_CLOSE, 0));
    }
    @Test public void malformedOrOversizedPayloadsFailClosedAndCopiesAreImmutable()
    {
        int[] d = page(); String[] text = {"Body", "A", "B"};
        ThorReferenceState s = ThorReferenceState.copyOf(7, d, text, new byte[0]);
        d[12] = 99; text[0] = "Changed";
        assertEquals(0, s.target(0)); assertEquals("Body", s.body());
        assertSame(ThorReferenceState.EMPTY, ThorReferenceState.copyOf(7, d, text, new byte[0]));
        assertSame(ThorReferenceState.EMPTY, ThorReferenceState.copyOf(7, page(), new String[]{"x".repeat(385), "A", "B"}, new byte[0]));
        d = page(); d[7] = 16;
        assertSame(ThorReferenceState.EMPTY, ThorReferenceState.copyOf(7, d, new String[]{"", "A", "B"}, new byte[0]));
    }
    @Test public void localReadingSurvivesTextRevisionsButNotContextReplacement()
    {
        ThorReferenceJourney j = new ThorReferenceJourney(); j.update(journal(7));
        j.begin(ThorReferenceJourney.READ, 7, 3, 0, 1);
        assertNull(j.release(ThorReferenceJourney.READ, 7, 3, 0, 1, true));
        assertTrue(j.reading);
        j.withdraw(); j.update(journal(8)); assertTrue(j.reading);
        assertEquals(ThorExtendedCommands.REFERENCE_TEXT_NEXT, j.command(ThorReferenceJourney.NEXT));
        j.reset(); j.update(journal(9)); assertFalse(j.reading);
        assertEquals(ThorExtendedCommands.REFERENCE_NEXT, j.command(ThorReferenceJourney.NEXT));
    }
    @Test public void staleSessionRevisionMultitouchAndDuplicateReleaseCannotDispatch()
    {
        ThorReferenceJourney j = new ThorReferenceJourney(); j.update(journal(7));
        j.begin(ThorReferenceJourney.ROW_FIRST, 7, 3, 0, 1);
        assertNull(j.release(ThorReferenceJourney.ROW_FIRST, 8, 3, 0, 1, true));
        j.begin(ThorReferenceJourney.ROW_FIRST, 7, 3, 0, 1);
        assertNull(j.release(ThorReferenceJourney.ROW_FIRST, 7, 4, 0, 1, true));
        j.begin(ThorReferenceJourney.ROW_FIRST, 7, 3, 0, 2);
        assertNull(j.release(ThorReferenceJourney.ROW_FIRST, 7, 3, 0, 1, true));
        j.begin(ThorReferenceJourney.ROW_FIRST, 7, 3, 0, 1);
        assertArrayEquals(new int[]{ThorExtendedCommands.REFERENCE_SELECT, 0}, j.release(ThorReferenceJourney.ROW_FIRST, 7, 3, 0, 1, true));
        assertNull(j.release(ThorReferenceJourney.ROW_FIRST, 7, 3, 0, 1, true));
        j.submitted = true; assertFalse(j.enabled(ThorReferenceJourney.CLOSE));
        j.cancel(); assertFalse(j.enabled(ThorReferenceJourney.CLOSE));
        j.update(journal(8)); assertTrue(j.enabled(ThorReferenceJourney.CLOSE));
    }
    @Test public void puzzleHasOnlyCloseAndBoundedPixels()
    {
        int[] d = {1, 3, 0, 0, -1, 0, 0, 8, 592, 544, 0, 50};
        byte[] rgb = new byte[592 * 544 * 3]; rgb[0] = (byte)255;
        ThorReferenceState s = ThorReferenceState.copyOf(7, d, new String[]{""}, rgb);
        assertNotSame(ThorReferenceState.EMPTY, s); assertEquals(0xffff0000, s.pixels()[0]);
        assertTrue(s.allows(ThorExtendedCommands.REFERENCE_CLOSE, -1));
        assertFalse(s.allows(ThorExtendedCommands.REFERENCE_CENTER, -1));
        rgb[0] = 0; assertEquals(0xffff0000, s.pixels()[0]);
        d[7] = 12;
        assertSame(ThorReferenceState.EMPTY, ThorReferenceState.copyOf(7, d, new String[]{""}, rgb));
    }
    @Test public void readableLocalSubpagesPrecedeNativeTextNavigation()
    {
        ThorReferenceJourney j = new ThorReferenceJourney(); j.update(journal(7)); j.reading = true;
        j.localPages = 3;
        j.begin(ThorReferenceJourney.NEXT, 7, 3, 0, 1);
        assertNull(j.release(ThorReferenceJourney.NEXT, 7, 3, 0, 1, true)); assertEquals(1, j.localPage);
        // Previous-native-page restoration may await the next draw. Its sentinel
        // must not overflow into a negative local page on an immediate new tap.
        j.localPage = Integer.MAX_VALUE;
        j.begin(ThorReferenceJourney.NEXT, 7, 3, 0, 1);
        assertArrayEquals(new int[]{ThorExtendedCommands.REFERENCE_TEXT_NEXT, -1}, j.release(ThorReferenceJourney.NEXT, 7, 3, 0, 1, true));
        assertTrue(j.localPage >= 0);
        j.localPage = 2;
        j.begin(ThorReferenceJourney.NEXT, 7, 3, 0, 1);
        assertArrayEquals(new int[]{ThorExtendedCommands.REFERENCE_TEXT_NEXT, -1}, j.release(ThorReferenceJourney.NEXT, 7, 3, 0, 1, true));
        int[] d = page(); d[5] = 1;
        j.withdraw(); j.update(ThorReferenceState.copyOf(8, d, new String[]{"Next", "A", "B"}, new byte[0]));
        assertEquals(0, j.localPage); assertTrue(j.reading);
    }

    @Test public void controlsFitMeasuredDisplaysAndDoNotOverlap()
    {
        for (int[] size : new int[][]{{1080, 1240}, {720, 900}})
        {
            ThorPresentationLayout layout = new ThorPresentationLayout(size[0], size[1], 2);
            int[] visible = {1, 2, 3, 4, 5, 7, 8, 100, 101, 102, 103, 104};
            for (int i = 0; i < visible.length; ++i)
            {
                float[] a = ThorReferenceDeck.bounds(visible[i], layout);
                assertTrue(a[0] >= layout.margin && a[1] >= layout.margin && a[2] <= layout.right && a[3] <= layout.bottom);
                for (int j = 0; j < i; ++j)
                {
                    float[] b = ThorReferenceDeck.bounds(visible[j], layout);
                    assertTrue(a[2] <= b[0] || a[0] >= b[2] || a[3] <= b[1] || a[1] >= b[3]);
                }
            }
        }
    }

}
