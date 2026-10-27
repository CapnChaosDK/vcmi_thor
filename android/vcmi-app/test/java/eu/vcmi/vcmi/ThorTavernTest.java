package eu.vcmi.vcmi;

import org.junit.Test;
import static org.junit.Assert.*;

public class ThorTavernTest
{
    private static int[] data()
    { return new int[]{1, 12, -1, 2500, 5000, 0, 1, 1, 1, 3, 12, 1, 34, 1, 87, 2}; }
    private static String[] text()
    { return new String[]{"A rumor", "First hero", "First is a level 1 Knight with 0 artifacts", "Second hero", "Second details", "Invited hero", ""}; }
    private static ThorTavernState state() { return ThorTavernState.copyOf(7, data(), text()); }

    @Test public void completeSelectionInvitationRecruitAndCloseJourney()
    {
        ThorTavernState current = state();
        assertNotSame(ThorTavernState.EMPTY, current);
        int row = ThorTavernControls.row(current, ThorTavernControls.OFFERS, 0, ThorTavernControls.ROW_FIRST + 1);
        assertEquals(34, current.id(row));
        assertTrue(current.allows(ThorExtendedCommands.TAVERN_SELECT, current.id(row)));
        int[] d = data(); d[1] = 34;
        current = ThorTavernState.copyOf(8, d, text());
        assertEquals("Second details", ThorTavernControls.text(current, ThorTavernControls.DETAILS));
        assertTrue(current.allows(ThorExtendedCommands.TAVERN_INVITE, 87));
        d[2] = 87; current = ThorTavernState.copyOf(9, d, text());
        assertEquals(87, current.invited());
        assertTrue(current.allows(ThorExtendedCommands.TAVERN_RECRUIT, -1));
        assertTrue(current.allows(ThorExtendedCommands.TAVERN_GUILD, -1));
        assertTrue(current.allows(ThorExtendedCommands.TAVERN_CLOSE, -1));
        assertFalse(ThorTavernState.EMPTY.allows(ThorExtendedCommands.TAVERN_RECRUIT, -1));
    }

    @Test public void immutableSnapshotAndMalformedPacketsFailClosed()
    {
        int[] d = data(); String[] t = text();
        ThorTavernState copy = ThorTavernState.copyOf(7, d, t);
        d[1] = 999; t[1] = "changed";
        assertEquals(12, copy.selected()); assertEquals("First hero", copy.name(0));
        assertSame(ThorTavernState.EMPTY, ThorTavernState.copyOf(0, data(), text()));
        assertSame(ThorTavernState.EMPTY, ThorTavernState.copyOf(7, null, text()));
        assertSame(ThorTavernState.EMPTY, ThorTavernState.copyOf(7, data(), null));
        int[][] invalid = {data(), data(), data(), data(), data(), data(), data(), data(), data(), data()};
        invalid[0][0] = 2; invalid[1][9] = 259; invalid[2][1] = 87; invalid[3][2] = 34;
        invalid[4][10] = 34; invalid[5][11] = 4; invalid[6][15] = 1;
        invalid[7][6] = 0; invalid[8][4] = 1; invalid[9][8] = 2;
        for (int[] packet : invalid) assertSame(ThorTavernState.EMPTY, ThorTavernState.copyOf(7, packet, text()));
        for (int index : new int[]{0,1,2})
        {
            t = text(); t[index] = "é".repeat(index == 1 ? 129 : 513);
            assertSame(ThorTavernState.EMPTY, ThorTavernState.copyOf(7, data(), t));
        }
    }

    @Test public void permissionsStayNativeAndFooterControlsNeverBecomeRows()
    {
        ThorTavernState current = state();
        for (int control = ThorTavernControls.OFFERS; control <= ThorTavernControls.CLOSE; ++control)
            assertEquals(-1, ThorTavernControls.row(current, ThorTavernControls.INVITATIONS, 0, control));
        assertEquals(-1, ThorTavernControls.target(current, 1, 0, ThorTavernControls.RECRUIT));
        assertTrue(ThorTavernControls.enabled(current, 1, 0, ThorTavernControls.CLOSE));
        assertEquals(-1, ThorTavernControls.row(current, 1, Integer.MAX_VALUE, ThorTavernControls.ROW_FIRST));
        assertFalse(current.allows(ThorExtendedCommands.TAVERN_SELECT, 87));
        assertFalse(current.allows(ThorExtendedCommands.TAVERN_INVITE, 12));
        assertFalse(current.allows(ThorExtendedCommands.TAVERN_RECRUIT, 12));
        assertFalse(current.allows(ThorExtendedCommands.MARKET_CLOSE, -1));
        for (int reason = 1; reason <= 6; ++reason)
        {
            int[] d = data(); d[5] = reason; d[7] = 0;
            if (reason == 1) d[6] = 0;
            current = ThorTavernState.copyOf(7, d, text());
            assertFalse(current.allows(ThorExtendedCommands.TAVERN_RECRUIT, -1));
            assertTrue(current.allows(ThorExtendedCommands.TAVERN_CLOSE, -1));
        }
        assertNull(ThorExtendedCommands.encode(1, ThorExtendedCommands.TAVERN_CLOSE + 1, -1));
        assertSame(ThorExtendedCommands.EMPTY, ThorExtendedCommands.copyOf(1, new int[]{19}, new int[0]));
        assertArrayEquals(new int[]{1,19,12}, ThorExtendedCommands.encode(1,19,12));
    }

    @Test public void invitationPagesUseStableIdsAndTextPagesPreserveUnicode()
    {
        int n = 256; int[] d = new int[10 + n * 2]; String[] t = new String[1 + n * 2];
        d[0] = 1; d[1] = d[2] = -1; d[6] = 1; d[9] = n; t[0] = "😀".repeat(200);
        for (int i = 0; i < n; ++i) { d[10+i*2] = 1000+i; d[11+i*2] = 2; t[1+i*2] = "Hero "+i; t[2+i*2] = ""; }
        ThorTavernState current = ThorTavernState.copyOf(7,d,t);
        assertEquals(64, ThorTavernControls.pages(current,3));
        int row = ThorTavernControls.row(current,3,63,ThorTavernControls.ROW_LAST);
        assertEquals(1255,current.id(row));
        assertEquals(-1,ThorTavernControls.row(current,3,64,ThorTavernControls.ROW_FIRST));
        assertEquals(t[0],ThorTavernControls.pageText(current,4,0)+ThorTavernControls.pageText(current,4,1));
        assertEquals("",ThorTavernControls.pageText(current,4,Integer.MAX_VALUE));
    }

    @Test public void touchGeometryHasFourRowsAndDistinctFooterTargets()
    {
        ThorPresentationLayout layout = new ThorPresentationLayout(1080,1240,2.3f);
        for (int c : new int[]{1,2,3,4,5,6,7,8,9,100,101,102,103})
        {
            float[] b = ThorTavernControls.bounds(c,layout);
            assertTrue("height "+c, (b[3]-b[1])/2.3f >= 44);
            assertTrue("width "+c, (b[2]-b[0])/2.3f >= 48);
            assertEquals(c,ThorTavernControls.at((b[0]+b[2])/2,(b[1]+b[3])/2,layout,3));
        }
        assertEquals(0,ThorTavernControls.at(500,400,layout,2));
    }

    @Test public void staleDisabledAndCrossControlGesturesCannotRecruit()
    {
        ThorControlGesture tap = new ThorControlGesture(0);
        for (int condition=0; condition<6; ++condition)
        {
            tap.begin(ThorContextIds.TAVERN,7,7,1,4,condition!=0);
            assertFalse(tap.finish(condition==1?ThorContextIds.UNKNOWN:ThorContextIds.TAVERN,
                    condition==2?8:7,condition==3?2:1,4,condition==4?8:7,1,true,condition!=5));
        }
        tap.begin(ThorContextIds.TAVERN,7,7,1,4,true);
        assertTrue(tap.finish(ThorContextIds.TAVERN,7,1,4,7,1,true,true));
        assertFalse(tap.finish(ThorContextIds.TAVERN,7,1,4,7,1,true,true));
    }
}
