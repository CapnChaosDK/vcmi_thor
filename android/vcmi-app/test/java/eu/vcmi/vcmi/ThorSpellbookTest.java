package eu.vcmi.vcmi;

import org.junit.Test;

/** Full local journeys; main also permits execution with only the JDK compiler. */
public class ThorSpellbookTest
{
    private static void check(boolean condition) { if (!condition) throw new AssertionError(); }
    private static final int[] DATA = {1, 11, 20, 0, 2, 3, 0, 7,
            9, 2, 8, 0, 1, 1, 0,
            18, 3, 12, 1, 0, 2, 0, 7,
            25, 0, 25, 0, 0, 1, 7};
    private static final String[] TEXT = {"Solmyr", "Air", "Custom", "View Air", "Adventure description",
            "Lightning", "Battle description", "Special", "Expensive spell"};
    private static ThorSpellbookState fixture(long revision)
    { ThorSpellbookState s = ThorSpellbookState.copyOf(revision, DATA.clone(), TEXT.clone()); check(s != ThorSpellbookState.EMPTY); return s; }
    private static int[] tap(ThorSpellbookJourney j, int control)
    {
        j.begin(control, j.state.revision, 4, 1, 1);
        return j.release(control, j.state.revision, 4, 1, 1, true);
    }
    @Test public void adventureBrowseFilterDetailsAndCast()
    {
        ThorSpellbookJourney j = new ThorSpellbookJourney(); j.update(fixture(1));
        check(!j.combat && j.count() == 2 && j.pages() == 1);
        check(tap(j, ThorSpellbookJourney.ROW_FIRST) == null && j.selected == 9);
        check(j.pageText().equals("Adventure description"));
        int[] cast = tap(j, ThorSpellbookJourney.CAST);
        check(cast[0] == 27 && cast[1] == 9);
        check(j.release(ThorSpellbookJourney.CAST, 1, 4, 1, 1, true) == null); // consumed release cannot replay
        tap(j, ThorSpellbookJourney.LIST); tap(j, ThorSpellbookJourney.SCHOOL_NEXT);
        check(j.school == 0 && j.count() == 1);
        tap(j, ThorSpellbookJourney.SCHOOL_NEXT); check(j.school == 7 && j.count() == 1);
        tap(j, ThorSpellbookJourney.LEVEL_NEXT); check(j.level == 0 && j.count() == 1);
        tap(j, ThorSpellbookJourney.LEVEL_NEXT); check(j.count() == 0);
        tap(j, ThorSpellbookJourney.LEVEL_PREVIOUS); check(j.count() == 1);
    }
    @Test public void battleBookAndOppositeCategoryStayReadOnly()
    {
        int[] data = DATA.clone(); data[3] = 1; data[12] = 0; data[19] = 1;
        ThorSpellbookJourney j = new ThorSpellbookJourney(); j.update(ThorSpellbookState.copyOf(2, data, TEXT));
        check(j.combat && j.count() == 1);
        tap(j, ThorSpellbookJourney.ROW_FIRST); check(tap(j, ThorSpellbookJourney.CAST)[1] == 18);
        tap(j, ThorSpellbookJourney.LIST); tap(j, ThorSpellbookJourney.MODE);
        check(!j.combat && j.count() == 2);
        tap(j, ThorSpellbookJourney.ROW_FIRST); check(!j.enabled(ThorSpellbookJourney.CAST));
        check(tap(j, ThorSpellbookJourney.CAST) == null);
    }
    @Test public void immutableCopiesAndMalformedPayloads()
    {
        int[] data = DATA.clone(); String[] text = TEXT.clone();
        ThorSpellbookState s = ThorSpellbookState.copyOf(1, data, text);
        data[8] = 999; text[3] = "Changed";
        check(s.row(0).id == 9 && s.row(0).name.equals("View Air"));
        for (int index : new int[]{0, 1, 2, 3, 4, 5, 8, 9, 10, 11, 12, 13, 14})
        { data = DATA.clone(); data[index] = -1; check(ThorSpellbookState.copyOf(1, data, TEXT) == ThorSpellbookState.EMPTY); }
        data = DATA.clone(); data[2] = 1; check(ThorSpellbookState.copyOf(1, data, TEXT) == ThorSpellbookState.EMPTY);
        data = DATA.clone(); data[7] = 0; check(ThorSpellbookState.copyOf(1, data, TEXT) == ThorSpellbookState.EMPTY);
        data = DATA.clone(); data[14] = 99; check(ThorSpellbookState.copyOf(1, data, TEXT) == ThorSpellbookState.EMPTY);
        data = DATA.clone(); data[15] = 9; check(ThorSpellbookState.copyOf(1, data, TEXT) == ThorSpellbookState.EMPTY);
        check(ThorSpellbookState.copyOf(1, java.util.Arrays.copyOf(DATA, DATA.length - 1), TEXT) == ThorSpellbookState.EMPTY);
        check(ThorSpellbookState.copyOf(1, java.util.Arrays.copyOf(DATA, DATA.length + 1), TEXT) == ThorSpellbookState.EMPTY);
        text = TEXT.clone(); text[4] = "x".repeat(4097); check(ThorSpellbookState.copyOf(1, DATA, text) == ThorSpellbookState.EMPTY);
        check(ThorSpellbookState.copyOf(0, DATA, TEXT) == ThorSpellbookState.EMPTY);
    }
    @Test public void staleRevisionSessionPointerAndLifecycleCancel()
    {
        ThorSpellbookJourney j = new ThorSpellbookJourney(); j.update(fixture(1));
        j.begin(100, 1, 4, 1, 1); j.update(fixture(2));
        check(j.release(100, 2, 4, 1, 1, true) == null && j.selected == -1);
        j.begin(100, 2, 4, 1, 1); check(j.release(100, 2, 5, 1, 1, true) == null && j.selected == -1);
        j.begin(100, 2, 4, 1, 1); check(j.release(100, 2, 4, 2, 1, true) == null && j.selected == -1);
        j.begin(100, 2, 4, 1, 1); check(j.release(100, 2, 4, 1, 2, true) == null && j.selected == -1);
        j.begin(100, 2, 4, 1, 1); check(j.release(100, 2, 4, 1, 1, false) == null && j.selected == -1);
        j.begin(100, 2, 4, 1, 1); j.cancel(); check(j.release(100, 2, 4, 1, 1, true) == null && j.selected == -1);
        tap(j, 100); j.submitted = true; check(!j.enabled(ThorSpellbookJourney.CLOSE));
        j.withdraw(); j.update(fixture(3)); check(!j.submitted && j.selected == 9);
        j.reset(); j.update(fixture(4)); check(j.selected == -1 && !j.combat);
    }
    @Test public void refreshClampsAndHeroChangesResetNavigation()
    {
        ThorSpellbookJourney j = new ThorSpellbookJourney(); j.update(fixture(1));
        tap(j, ThorSpellbookJourney.SCHOOL_NEXT); tap(j, 100); j.update(fixture(2));
        check(j.school == 0 && j.selected == 9);
        int[] data = DATA.clone(); data[1] = 12;
        j.update(ThorSpellbookState.copyOf(3, data, TEXT)); check(j.school == -1 && j.selected == -1);
        tap(j, 100); data = DATA.clone(); data[12] = 0;
        j.update(ThorSpellbookState.copyOf(4, data, TEXT)); check(!j.enabled(ThorSpellbookJourney.CAST));
    }
    @Test public void zeroCostEmptyAndDisabledBooks()
    {
        ThorSpellbookJourney j = new ThorSpellbookJourney();
        j.update(ThorSpellbookState.copyOf(1, new int[]{1, 1, 0, 0, 0, 0}, new String[]{"Hero"}));
        check(j.count() == 0 && !j.enabled(100) && j.enabled(ThorSpellbookJourney.CLOSE));
        int[] close = tap(j, ThorSpellbookJourney.CLOSE); check(close[0] == 28 && close[1] == -1);
        j.update(ThorSpellbookState.copyOf(2, new int[]{1, 1, 0, 0, 0, 1, 8, 0, 0, 0, 1, 0}, new String[]{"Hero", "Free", ""}));
        tap(j, 100); check(j.enabled(ThorSpellbookJourney.CAST));
        check(!j.enabled(104) && !j.enabled(99));
    }
    @Test public void allPagesUnicodeAndSchoollessSpellsRemainReachable()
    {
        java.util.ArrayList<Integer> data = new java.util.ArrayList<>();
        for (int v : new int[]{1, 1, 20, 0, 0, 9}) data.add(v);
        java.util.ArrayList<String> text = new java.util.ArrayList<>(); text.add("Hero");
        String details = "🧙".repeat(200) + " end";
        for (int i = 0; i < 9; ++i)
        { for (int v : new int[]{i, 1, 1, 0, 1, 0}) data.add(v); text.add("Spell " + i); text.add(details); }
        ThorSpellbookJourney j = new ThorSpellbookJourney();
        j.update(ThorSpellbookState.copyOf(1, data.stream().mapToInt(Integer::intValue).toArray(), text.toArray(new String[0])));
        check(j.pages() == 3); tap(j, ThorSpellbookJourney.NEXT); tap(j, ThorSpellbookJourney.NEXT);
        check(j.row(100) == 8 && j.row(101) == -1); tap(j, 100);
        StringBuilder result = new StringBuilder(j.pageText());
        while (j.enabled(ThorSpellbookJourney.NEXT)) { tap(j, ThorSpellbookJourney.NEXT); result.append(j.pageText()); }
        check(result.toString().equals(details));
        tap(j, ThorSpellbookJourney.LIST); check(j.page == 2 && j.school == -1);
    }
    @Test public void commandNamespaceIsIndependent()
    {
        check(ThorExtendedCommands.encode(1, 27, 9) != null && ThorExtendedCommands.encode(1, 28, -1) != null);
        check(ThorExtendedCommands.encode(1, 27, -1) == null && ThorExtendedCommands.encode(1, 28, 1) == null);
        check(ThorExtendedCommands.copyOf(1, new int[]{27}, new int[0]) == ThorExtendedCommands.EMPTY);
        check(!fixture(1).allows(ThorExtendedCommands.GUILD_RESEARCH, 9));
        check(ThorExtendedCommands.encode(1, 29, -1) != null && ThorExtendedCommands.encode(1, 29, 1) == null);
        check(ThorExtendedCommands.copyOf(1, new int[]{29}, new int[0]).allows(29));
    }
    @Test public void controlsAreDisjointAndInsideTheFrame()
    {
        for (int[] size : new int[][]{{1080,1240}, {800,600}, {540,620}})
        {
            ThorPresentationLayout layout = new ThorPresentationLayout(size[0], size[1], 1);
            java.util.ArrayList<float[]> boxes = new java.util.ArrayList<>();
            for (int c = 1; c <= ThorSpellbookJourney.CLOSE; ++c) boxes.add(ThorSpellbookDeck.bounds(c, layout));
            for (int c = 100; c <= 103; ++c) boxes.add(ThorSpellbookDeck.bounds(c, layout));
            for (int i = 0; i < boxes.size(); ++i)
            {
                float[] box = boxes.get(i); check(box != null && box[0] < box[2] && box[1] < box[3]);
                check(box[0] >= layout.margin && box[1] >= layout.margin && box[2] <= layout.right && box[3] <= layout.bottom);
                for (int j = 0; j < i; ++j)
                { float[] other = boxes.get(j); check(box[0] >= other[2] || box[2] <= other[0] || box[1] >= other[3] || box[3] <= other[1]); }
            }
        }
        check(ThorSpellbookDeck.bounds(104, new ThorPresentationLayout(1080,1240,1)) == null);
    }
    public static void main(String[] args)
    {
        ThorSpellbookTest test = new ThorSpellbookTest();
        test.adventureBrowseFilterDetailsAndCast(); test.battleBookAndOppositeCategoryStayReadOnly();
        test.immutableCopiesAndMalformedPayloads(); test.staleRevisionSessionPointerAndLifecycleCancel();
        test.refreshClampsAndHeroChangesResetNavigation(); test.zeroCostEmptyAndDisabledBooks();
        test.allPagesUnicodeAndSchoollessSpellsRemainReachable(); test.commandNamespaceIsIndependent();
        test.controlsAreDisjointAndInsideTheFrame();
        System.out.println("9 spellbook journey/geometry tests passed");
    }
}
