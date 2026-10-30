package eu.vcmi.vcmi;

import org.junit.Test;
import static org.junit.Assert.*;

public class ThorTownServiceTest
{
    private static ThorTownServiceState guild(long revision)
    {
        return ThorTownServiceState.copyOf(revision,
                new int[]{1, 2, 42, 0, 0, 6, 10, 1, 1, 11, 1, 0, 12, 2, 1, 13, 3, 0, 14, 4, 0, 15, 5, 1},
                new String[]{"", "One", "A".repeat(340), "Two", "Second", "Three", "Third", "Four", "Fourth", "Five", "Fifth", "Six", "Sixth"});
    }
    private static int[] tap(ThorTownServiceJourney j, int control)
    { j.begin(control, j.state.revision, 8, 4, 1); return j.release(control, j.state.revision, 8, 4, 1, true); }

    @Test public void completeGuildBrowseDetailsResearchAndReturn()
    {
        ThorTownServiceJourney j = new ThorTownServiceJourney(); j.update(guild(9));
        assertEquals(2, j.pages());
        tap(j, ThorTownServiceJourney.NEXT); assertEquals(14, j.state.id(j.row(100)));
        tap(j, ThorTownServiceJourney.PREVIOUS); tap(j, 100);
        assertEquals(10, j.selected); assertEquals(3, j.textPages());
        String first = j.pageText(); tap(j, ThorTownServiceJourney.NEXT);
        String second = j.pageText(); tap(j, ThorTownServiceJourney.NEXT);
        assertEquals("A".repeat(340), first + second + j.pageText());
        assertArrayEquals(new int[]{25,10}, tap(j, ThorTownServiceJourney.ACT));
        j.submitted = true; assertFalse(j.enabled(ThorTownServiceJourney.CLOSE));
        j.update(guild(10)); assertFalse(j.submitted);
        tap(j, ThorTownServiceJourney.LIST); assertEquals(-1, j.selected);
        tap(j, ThorTownServiceJourney.LEVEL_NEXT); assertEquals(1, j.level); assertEquals(2, j.count());
        tap(j, 101); assertFalse(j.enabled(ThorTownServiceJourney.ACT));
        assertArrayEquals(new int[]{26,-1}, tap(j, ThorTownServiceJourney.CLOSE));
    }
    @Test public void completeBlacksmithBuyDisabledAndClose()
    {
        ThorTownServiceJourney j = new ThorTownServiceJourney();
        j.update(ThorTownServiceState.copyOf(7, new int[]{1,1,42,1500,2000,1,4,0,1}, new String[]{"Hero","Ballista","Buy a Ballista"}));
        assertEquals(4, j.selected); assertFalse(j.enabled(ThorTownServiceJourney.LIST));
        assertArrayEquals(new int[]{24,4}, tap(j, ThorTownServiceJourney.ACT));
        j.submitted = true; assertNull(tap(j, ThorTownServiceJourney.ACT));
        j.update(ThorTownServiceState.copyOf(8, new int[]{1,1,42,1500,1000,1,4,0,0}, new String[]{"Hero","Ballista","Unavailable"}));
        assertFalse(j.enabled(ThorTownServiceJourney.ACT));
        assertTrue(j.enabled(ThorTownServiceJourney.CLOSE));
        assertEquals(-1, j.row(ThorTownServiceJourney.CLOSE));
        assertArrayEquals(new int[]{26,-1}, tap(j, ThorTownServiceJourney.CLOSE));
    }
    @Test public void malformedAndOversizedPayloadsFailClosedAndCopiesAreDefensive()
    {
        int[] data = {1,1,42,100,200,1,4,0,1}; String[] text = {"Hero","Machine","Details"};
        ThorTownServiceState state = ThorTownServiceState.copyOf(1,data,text);
        data[8] = 0; text[1] = "Changed";
        assertTrue(state.allows(24,4)); assertEquals("Machine",state.name(0));
        assertSame(ThorTownServiceState.EMPTY,ThorTownServiceState.copyOf(0,data,text));
        assertSame(ThorTownServiceState.EMPTY,ThorTownServiceState.copyOf(1,null,text));
        assertSame(ThorTownServiceState.EMPTY,ThorTownServiceState.copyOf(1,data,null));
        for (int at : new int[]{0,1,2,3,4,5,6,7,8})
        {
            int[] bad = {1,1,42,100,200,1,4,0,1}; bad[at] = -1;
            assertSame(ThorTownServiceState.EMPTY,ThorTownServiceState.copyOf(1,bad,new String[]{"","Machine",""}));
        }
        assertSame(ThorTownServiceState.EMPTY,ThorTownServiceState.copyOf(1,new int[]{1,1,42,100,50,1,4,0,1},text));
        assertSame(ThorTownServiceState.EMPTY,ThorTownServiceState.copyOf(1,new int[]{1,2,42,0,0,2,4,1,1,4,2,1},new String[]{"","A","","B",""}));
        assertSame(ThorTownServiceState.EMPTY,ThorTownServiceState.copyOf(1,data,new String[]{"","A","x".repeat(4097)}));
        assertSame(ThorTownServiceState.EMPTY,ThorTownServiceState.copyOf(1,new int[]{1,2,42,0,0,129},new String[]{""}));
    }
    @Test public void stalePointerSessionContextAndDuplicateReleaseCannotAct()
    {
        ThorTownServiceJourney j = new ThorTownServiceJourney(); j.update(guild(9)); tap(j,100);
        for (int failure = 0; failure < 6; ++failure)
        {
            j.begin(ThorTownServiceJourney.ACT,9,8,4,1);
            if (failure == 0) j.cancel();
            if (failure == 1) j.update(guild(10));
            assertNull(j.release(ThorTownServiceJourney.ACT,failure == 1 ? 10 : 9,
                    failure == 2 ? 9 : 8,failure == 3 ? 5 : 4,failure == 4 ? 2 : 1,failure != 5));
            j.update(guild(9)); tap(j,100);
        }
        j.begin(ThorTownServiceJourney.ACT,9,8,4,1); j.reset(); assertNull(j.release(5,9,8,4,1,true));
        j.update(guild(9)); tap(j,100); assertNotNull(tap(j,5)); assertNull(j.release(5,9,8,4,1,true));
    }
    @Test public void emptySpecialGuildSummaryAndUnicodeAreFullyPageable()
    {
        String summary = "\uD83D\uDC09".repeat(350);
        ThorTownServiceJourney j = new ThorTownServiceJourney();
        j.update(ThorTownServiceState.copyOf(1,new int[]{1,2,4,0,0,0},new String[]{summary}));
        assertEquals(0,j.count()); assertFalse(j.enabled(100)); assertFalse(j.enabled(5)); assertTrue(j.enabled(7));
        StringBuilder all = new StringBuilder(j.pageText());
        while(j.enabled(4)) { tap(j,4); all.append(j.pageText()); }
        assertEquals(summary,all.toString());
        assertEquals(-1,j.row(7)); assertEquals(-1,j.row(104));
        assertArrayEquals(new int[]{26,-1},tap(j,7));
    }
    @Test public void everyLevelAndLastPageRemainBounded()
    {
        ThorTownServiceJourney j = new ThorTownServiceJourney(); j.update(guild(9));
        for(int level=1;level<=5;++level)
        {
            tap(j,2); assertEquals(level,j.level);
            for(int c=100;c<=103;++c) { int row=j.row(c); if(row>=0) assertEquals(level,j.state.level(row)); }
        }
        assertFalse(j.enabled(2)); assertNull(tap(j,2));
        for(int level=4;level>=0;--level) { tap(j,1); assertEquals(level,j.level); }
        assertFalse(j.enabled(1));
        tap(j,4); assertEquals(-1,j.row(102)); assertEquals(-1,j.row(7));
        j.page=Integer.MAX_VALUE; assertEquals(-1,j.row(100));
    }
    @Test public void commandNamespaceIsSeparateAndContextQualified()
    {
        assertEquals(24,ThorExtendedCommands.BLACKSMITH_BUY); assertEquals(25,ThorExtendedCommands.GUILD_RESEARCH);
        assertEquals(26,ThorExtendedCommands.SERVICE_CLOSE);
        assertNull(ThorExtendedCommands.encode(1,24,-1)); assertNull(ThorExtendedCommands.encode(1,25,-1));
        assertNull(ThorExtendedCommands.encode(1,26,0)); assertNull(ThorExtendedCommands.encode(1,27,-1));
        assertFalse(guild(1).allows(24,10)); assertTrue(guild(1).allows(25,10)); assertFalse(guild(1).allows(25,11));
        assertFalse(ThorTownServiceState.EMPTY.allows(26,-1));
        assertFalse(ThorExtendedCommands.copyOf(1,new int[]{24},new int[0]).allows(24));
    }

    @Test public void sameOwnerRefreshRetainsBrowseWhileLifecycleCancelsInputAndPending()
    {
        ThorTownServiceJourney j = new ThorTownServiceJourney(); j.update(guild(9)); tap(j,100); tap(j,4);
        j.begin(5,9,8,4,1); j.withdraw(); j.update(guild(10));
        assertEquals(10,j.selected); assertEquals(1,j.textPage); assertNull(j.release(5,10,8,4,1,true));
        j.submitted=true; j.cancel(); assertFalse(j.submitted);
        j.reset(); j.update(guild(11)); assertEquals(-1,j.selected); assertEquals(0,j.page); assertEquals(0,j.level);
        j.update(ThorTownServiceState.copyOf(12,new int[]{1,2,99,0,0,0},new String[]{"Special\n\nsummary"}));
        assertEquals(-1,j.selected); assertEquals("Special summary",j.pageText());
    }

    @Test public void rowAndFooterGeometryIsDisjointAcrossPanelSizes()
    {
        for (float[] size : new float[][]{{1080,1240},{720,900},{540,620}})
        {
            ThorPresentationLayout layout=new ThorPresentationLayout((int)size[0],(int)size[1],1f);
            int[] controls={1,2,3,4,5,6,7,100,101,102,103};
            for(int i=0;i<controls.length;++i)
            {
                float[] a=ThorTownServiceDeck.bounds(controls[i],layout);
                assertTrue(a[0]<a[2]); assertTrue(a[1]<a[3]);
                for(int k=i+1;k<controls.length;++k)
                {
                    float[] b=ThorTownServiceDeck.bounds(controls[k],layout);
                    assertFalse(a[0]<b[2] && a[2]>b[0] && a[1]<b[3] && a[3]>b[1]);
                }
            }
        }
    }
}
