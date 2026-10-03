package eu.vcmi.vcmi;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ThorRecruitmentTest
{
    private ThorRecruitmentState state(final String context, final int mode, final int count)
    {
        final int[] targets = new int[count];
        final int[] creatureIds = new int[count];
        final int[] available = new int[count];
        final int[] selected = new int[count];
        final int[] maximum = new int[count];
        final int[] variantIndexes = new int[count];
        final int[] variantCounts = new int[count];
        final int[] flags = new int[count];
        final long[] visualKeys = new long[count];
        final String[] names = new String[count];
        final String[] unitCosts = new String[count];
        final String[] selectedCosts = new String[count];
        for (int index = 0; index < count; ++index)
        {
            targets[index] = 100 + index;
            creatureIds[index] = 20 + index;
            available[index] = 15;
            selected[index] = index == 1 ? 4 : 0;
            maximum[index] = index == 1 ? 10 : 15;
            variantCounts[index] = index == 1 && mode == ThorRecruitmentState.QUICK_TOWN ? 2 : 1;
            variantIndexes[index] = variantCounts[index] > 1 ? 1 : 0;
            flags[index] = 1 | 4 | (index == 1 ? 2 : 0);
            visualKeys[index] = ThorVisualAssetKey.forCreature(creatureIds[index]);
            names[index] = "Creature " + index;
            unitCosts[index] = "100 Gold";
            selectedCosts[index] = index == 1 ? "400 Gold" : "";
        }
        return ThorRecruitmentState.copyOf(context, 71L, mode, count > 1 ? 101 : -1, targets,
                creatureIds, available, selected, maximum, variantIndexes, variantCounts, flags,
                visualKeys, names, unitCosts, selectedCosts, "Town", "400 Gold");
    }

    @Test
    public void validatesBoundedRowsAndPagesWithoutChangingNativeTargets()
    {
        final ThorRecruitmentState state = state(ThorContextIds.TOWN_RECRUITMENT_QUICK,
                ThorRecruitmentState.QUICK_TOWN, 12);
        assertEquals(71L, state.revision);
        assertEquals(3, state.pageCount());
        assertEquals(104, state.rows[state.rowIndexOnPage(0, 4)].target);
        assertEquals(105, state.rows[state.rowIndexOnPage(1, 0)].target);
        assertEquals(111, state.rows[state.rowIndexOnPage(2, 1)].target);
        assertEquals(-1, state.rowIndexOnPage(2, 2));
        assertEquals(ThorVisualAssetKey.forCreature(21), state.rows[1].visualAssetKey);
        assertEquals(2, state.rows[1].variantCount);
        assertTrue(state.rows[1].armyAvailable);
    }

    @Test
    public void rejectsWrongContextOversizedListsDuplicatesAndMalformedRows()
    {
        final ThorRecruitmentState valid = state(ThorContextIds.TOWN_RECRUITMENT_QUICK,
                ThorRecruitmentState.QUICK_TOWN, 2);
        assertEquals(ThorRecruitmentState.EMPTY, ThorRecruitmentState.copyOf(ThorContextIds.TOWN_HALL,
                valid.revision, valid.mode, valid.selectedTarget, new int[0], new int[0], new int[0],
                new int[0], new int[0], new int[0], new int[0], new int[0], new long[0],
                new String[0], new String[0], new String[0], "Town", ""));

        final int[] targets = {1, 1};
        final int[] creatureIds = {1, 2};
        final int[] counts = {4, 4};
        final int[] variants = {0, 0};
        final int[] flags = {1, 1};
        final long[] keys = {ThorVisualAssetKey.forCreature(1), ThorVisualAssetKey.forCreature(2)};
        final String[] names = {"One", "Two"};
        final String[] costs = {"", ""};
        assertEquals(ThorRecruitmentState.EMPTY, ThorRecruitmentState.copyOf(
                ThorContextIds.TOWN_RECRUITMENT_DWELLING, 71, ThorRecruitmentState.TOWN_DWELLING, -1,
                targets, creatureIds, counts, new int[2], counts, variants, new int[]{1, 1}, flags,
                keys, names, costs, costs, "Town", ""));

        assertEquals(ThorRecruitmentState.EMPTY, ThorRecruitmentState.copyOf(
                ThorContextIds.TOWN_RECRUITMENT_DWELLING, 71, ThorRecruitmentState.TOWN_DWELLING, 1,
                new int[]{1, 2}, creatureIds, counts, new int[]{0, 1}, counts, variants, new int[]{1, 1},
                new int[]{5, 5}, keys, names, costs, costs, "Town", ""));

        final int[] tooMany = new int[ThorRecruitmentState.MAX_ROWS + 1];
        assertEquals(ThorRecruitmentState.EMPTY, ThorRecruitmentState.copyOf(
                ThorContextIds.TOWN_RECRUITMENT_QUICK, 71, ThorRecruitmentState.QUICK_TOWN, -1,
                tooMany, new int[0], new int[0], new int[0], new int[0], new int[0], new int[0],
                new int[0], new long[0], new String[0], new String[0], new String[0], "Town", ""));

        assertEquals(ThorRecruitmentState.EMPTY, ThorRecruitmentState.copyOf(
                ThorContextIds.TOWN_RECRUITMENT_QUICK, 71, ThorRecruitmentState.QUICK_TOWN, -1,
                new int[]{1}, new int[]{9}, new int[]{5}, new int[]{0}, new int[]{5}, new int[]{0},
                new int[]{1}, new int[]{1}, new long[]{ThorVisualAssetKey.forCreature(9)},
                new String[]{null}, new String[]{""}, new String[]{""}, "Town", ""));
        assertEquals(ThorRecruitmentState.EMPTY, ThorRecruitmentState.copyOf(
                ThorContextIds.TOWN_RECRUITMENT_QUICK, 71, ThorRecruitmentState.QUICK_TOWN, -1,
                new int[]{1}, new int[]{9}, new int[]{5}, new int[]{0}, new int[]{5}, new int[]{0},
                new int[]{1}, new int[]{5}, new long[]{ThorVisualAssetKey.forCreature(9)},
                new String[]{"Troop"}, new String[]{null}, new String[]{""}, "Town", ""));
        assertEquals(ThorRecruitmentState.EMPTY, ThorRecruitmentState.copyOf(
                ThorContextIds.TOWN_RECRUITMENT_QUICK, 71, ThorRecruitmentState.QUICK_TOWN, -1,
                new int[]{1}, new int[]{9}, new int[]{5}, new int[]{0}, new int[]{5}, new int[]{0},
                new int[]{1}, new int[]{5}, new long[]{ThorVisualAssetKey.forCreature(9)},
                new String[]{"Troop"}, new String[]{""}, new String[]{null}, "Town", ""));
    }

    @Test
    public void rejectsOversizedUtf8AndInconsistentSelectionOrVisualIdentity()
    {
        final ThorRecruitmentState valid = state(ThorContextIds.TOWN_RECRUITMENT_DWELLING,
                ThorRecruitmentState.TOWN_DWELLING, 2);
        final int[] targets = {100, 101};
        final int[] creatureIds = {20, 21};
        final int[] counts = {15, 15};
        final int[] selected = {0, 4};
        final int[] maximum = {15, 10};
        final int[] variantIndexes = {0, 0};
        final int[] variantCounts = {1, 1};
        final int[] flags = {5, 7};
        final long[] visualKeys = {ThorVisualAssetKey.forCreature(20), ThorVisualAssetKey.forCreature(21)};
        final String[] names = {"One", "Two"};
        final String[] costs = {"", ""};
        final StringBuilder oversizedName = new StringBuilder();
        for (int index = 0; index < 65; ++index)
            oversizedName.append('é');
        final String tooLong = oversizedName.toString();
        assertEquals(ThorRecruitmentState.EMPTY, ThorRecruitmentState.copyOf(
                ThorContextIds.TOWN_RECRUITMENT_DWELLING, valid.revision, valid.mode, valid.selectedTarget,
                targets, creatureIds, counts, selected, maximum, variantIndexes, variantCounts, flags,
                visualKeys, names, costs, costs, tooLong, ""));
        visualKeys[1] = ThorVisualAssetKey.forCreature(22);
        assertEquals(ThorRecruitmentState.EMPTY, ThorRecruitmentState.copyOf(
                ThorContextIds.TOWN_RECRUITMENT_DWELLING, valid.revision, valid.mode, valid.selectedTarget,
                targets, creatureIds, counts, selected, maximum, variantIndexes, variantCounts, flags,
                visualKeys, names, costs, costs, "Town", ""));
    }

    @Test
    public void keepsArmyCapacitySeparateFromTheNativePurchaseDecision()
    {
        final ThorRecruitmentState valid = state(ThorContextIds.TOWN_RECRUITMENT_QUICK,
                ThorRecruitmentState.QUICK_TOWN, 2);
        final int[] flags = {5, 2};
        final ThorRecruitmentState noSpace = ThorRecruitmentState.copyOf(
                ThorContextIds.TOWN_RECRUITMENT_QUICK, valid.revision, valid.mode, valid.selectedTarget,
                new int[]{100, 101}, new int[]{20, 21}, new int[]{15, 15}, new int[]{0, 4},
                new int[]{15, 10}, new int[]{0, 1}, new int[]{1, 2}, flags,
                new long[]{ThorVisualAssetKey.forCreature(20), ThorVisualAssetKey.forCreature(21)},
                new String[]{"One", "Two"}, new String[]{"", ""}, new String[]{"", ""}, "Town", "");
        assertFalse(noSpace.rows[1].armyAvailable);
        assertFalse(noSpace.rows[1].enabled);
    }

    @Test
    public void gestureConsumesOnlyTheBoundPointerRevisionAndPresentationSession()
    {
        final ThorRecruitmentGesture gesture = new ThorRecruitmentGesture();
        assertTrue(gesture.begin(71, 8, 3, 101, ThorRecruitmentState.INCREASE_10));
        assertEquals(3, gesture.pointerId());
        assertEquals(0, gesture.finish(72, 8, 3, 101, ThorRecruitmentState.INCREASE_10));
        assertFalse(gesture.isActive());

        assertTrue(gesture.begin(71, 8, 3, 101, ThorRecruitmentState.CYCLE_VARIANT));
        assertEquals(0, gesture.finish(71, 9, 3, 101, ThorRecruitmentState.CYCLE_VARIANT));
        assertTrue(gesture.begin(71, 8, 3, 101, ThorRecruitmentState.DECREASE_1));
        assertEquals(0, gesture.finish(71, 8, 4, 101, ThorRecruitmentState.DECREASE_1));

        assertTrue(gesture.begin(71, 8, 3, 101, ThorRecruitmentState.SELECT_ROW));
        assertEquals(ThorRecruitmentState.SELECT_ROW,
                gesture.finish(71, 8, 3, 101, ThorRecruitmentState.SELECT_ROW));
        assertFalse(gesture.isActive());
        assertTrue(gesture.begin(71, 8, 3, 101, ThorRecruitmentState.SELECT_ROW));
        gesture.cancel();
        assertFalse(gesture.isActive());
    }

    @Test
    public void creatureVisualAssetsAreAcceptedOnlyForPublishedRecruitmentRows()
    {
        final ThorRecruitmentState recruitment = state(ThorContextIds.TOWN_RECRUITMENT_QUICK,
                ThorRecruitmentState.QUICK_TOWN, 2);
        final ThorVisualAssetReferences references = new ThorVisualAssetReferences();
        references.updateContext(recruitment.revision, ThorContextIds.TOWN_RECRUITMENT_QUICK, 0L);
        assertTrue(references.references(recruitment.revision, ThorVisualAssetKey.forCreature(21),
                ThorHeroMeetingArmies.EMPTY, ThorHeroMeetingArtifacts.EMPTY, recruitment));
        assertFalse(references.references(recruitment.revision + 1, ThorVisualAssetKey.forCreature(21),
                ThorHeroMeetingArmies.EMPTY, ThorHeroMeetingArtifacts.EMPTY, recruitment));
        assertFalse(references.references(recruitment.revision, ThorVisualAssetKey.forCreature(22),
                ThorHeroMeetingArmies.EMPTY, ThorHeroMeetingArtifacts.EMPTY, recruitment));
    }
}
