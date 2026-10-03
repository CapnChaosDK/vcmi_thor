package eu.vcmi.vcmi;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/** Bounded, revision-bound snapshot for either Town recruitment window. */
final class ThorRecruitmentState
{
    static final int QUICK_TOWN = 1;
    static final int TOWN_DWELLING = 2;
    static final int MAX_ROWS = 64;
    static final int PAGE_SIZE = 5;
    static final int MAX_VARIANTS = 16;
    static final int SELECT_ROW = 1;
    static final int DECREASE_10 = 2;
    static final int DECREASE_1 = 3;
    static final int INCREASE_1 = 4;
    static final int INCREASE_10 = 5;
    static final int SET_MINIMUM = 6;
    static final int SET_MAXIMUM = 7;
    static final int CYCLE_VARIANT = 8;
    static final int BUY_CONTROL = 101;
    static final int BACK_CONTROL = 102;
    static final ThorRecruitmentState EMPTY = new ThorRecruitmentState(0L, 0, -1, "", "", new Row[0]);

    static final class Row
    {
        final int target;
        final int creatureId;
        final String name;
        final String unitCost;
        final String selectedCost;
        final int available;
        final int selectedAmount;
        final int maximum;
        final int variantIndex;
        final int variantCount;
        final boolean enabled;
        final boolean selected;
        final boolean armyAvailable;
        final long visualAssetKey;

        Row(final int target, final int creatureId, final String name, final String unitCost,
            final String selectedCost, final int available, final int selectedAmount, final int maximum,
            final int variantIndex, final int variantCount, final boolean enabled, final boolean selected,
            final boolean armyAvailable,
            final long visualAssetKey)
        {
            this.target = target;
            this.creatureId = creatureId;
            this.name = name;
            this.unitCost = unitCost;
            this.selectedCost = selectedCost;
            this.available = available;
            this.selectedAmount = selectedAmount;
            this.maximum = maximum;
            this.variantIndex = variantIndex;
            this.variantCount = variantCount;
            this.enabled = enabled;
            this.selected = selected;
            this.armyAvailable = armyAvailable;
            this.visualAssetKey = visualAssetKey;
        }
    }

    final long revision;
    final int mode;
    final int selectedTarget;
    final String townName;
    final String totalCost;
    final Row[] rows;

    private ThorRecruitmentState(final long revision, final int mode, final int selectedTarget,
                                 final String townName, final String totalCost, final Row[] rows)
    {
        this.revision = revision;
        this.mode = mode;
        this.selectedTarget = selectedTarget;
        this.townName = townName;
        this.totalCost = totalCost;
        this.rows = rows;
    }

    static ThorRecruitmentState copyOf(final String contextId, final long revision, final int mode,
            final int selectedTarget, final int[] targets, final int[] creatureIds, final int[] available,
            final int[] selectedAmounts, final int[] maximum, final int[] variantIndexes,
            final int[] variantCounts, final int[] flags, final long[] visualKeys, final String[] names,
            final String[] unitCosts, final String[] selectedCosts, final String townName, final String totalCost)
    {
        if (revision <= 0L || targets == null || creatureIds == null || available == null || selectedAmounts == null
                || maximum == null || variantIndexes == null || variantCounts == null || flags == null
                || visualKeys == null || names == null || unitCosts == null || selectedCosts == null)
            return EMPTY;
        if (!((mode == QUICK_TOWN && ThorContextIds.TOWN_RECRUITMENT_QUICK.equals(contextId))
                || (mode == TOWN_DWELLING && ThorContextIds.TOWN_RECRUITMENT_DWELLING.equals(contextId))))
            return EMPTY;
        final int count = targets.length;
        if (count > MAX_ROWS || creatureIds.length != count || available.length != count
                || selectedAmounts.length != count || maximum.length != count || variantIndexes.length != count
                || variantCounts.length != count || flags.length != count || visualKeys.length != count
                || names.length != count || unitCosts.length != count || selectedCosts.length != count
                || !bounded(townName) || !bounded(totalCost))
            return EMPTY;

        final Set<Integer> seen = new HashSet<>();
        final Row[] rows = new Row[count];
        int selectedCount = 0;
        boolean selectedTargetFound = selectedTarget == -1;
        for (int index = 0; index < count; ++index)
        {
            final String name = names[index];
            final String unitCost = unitCosts[index];
            final String selectedCost = selectedCosts[index];
            if (targets[index] < 0 || creatureIds[index] < 0 || !seen.add(targets[index])
                    || available[index] < 0 || selectedAmounts[index] < 0 || maximum[index] < 0
                    || selectedAmounts[index] > maximum[index] || maximum[index] > available[index]
                    || variantCounts[index] < 1 || variantCounts[index] > MAX_VARIANTS
                    || variantIndexes[index] < 0 || variantIndexes[index] >= variantCounts[index]
                    || (flags[index] & ~7) != 0 || name == null || name.isEmpty() || !bounded(name)
                    || unitCost == null || selectedCost == null || !bounded(unitCost) || !bounded(selectedCost)
                    || (visualKeys[index] != 0L && (!ThorVisualAssetKey.isCreature(visualKeys[index])
                            || visualKeys[index] != ThorVisualAssetKey.forCreature(creatureIds[index]))))
                return EMPTY;
            final boolean enabled = (flags[index] & 1) != 0;
            final boolean selected = (flags[index] & 2) != 0;
            final boolean armyAvailable = (flags[index] & 4) != 0;
            if (enabled && !armyAvailable)
                return EMPTY;
            if (mode == TOWN_DWELLING && !selected && selectedAmounts[index] != 0)
                return EMPTY;
            if (selected && targets[index] == selectedTarget)
                selectedTargetFound = true;
            else if (selected)
                return EMPTY;
            if (selected)
                ++selectedCount;
            rows[index] = new Row(targets[index], creatureIds[index], name, unitCost, selectedCost,
                    available[index], selectedAmounts[index], maximum[index], variantIndexes[index],
                    variantCounts[index], enabled, selected, armyAvailable, visualKeys[index]);
        }
        if (!selectedTargetFound || selectedCount > 1 || (selectedTarget == -1 ? selectedCount != 0 : selectedCount != 1))
            return EMPTY;
        return new ThorRecruitmentState(revision, mode, selectedTarget,
                townName == null ? "" : townName, totalCost == null ? "" : totalCost, rows);
    }

    int pageCount()
    {
        return Math.max(1, (rows.length + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    int rowIndexOnPage(final int page, final int position)
    {
        if (page < 0 || page >= pageCount() || position < 0 || position >= PAGE_SIZE)
            return -1;
        final int index = page * PAGE_SIZE + position;
        return index < rows.length ? index : -1;
    }

    long[] visualAssetKeys()
    {
        final long[] keys = new long[rows.length];
        for (int index = 0; index < rows.length; ++index)
            keys[index] = rows[index].visualAssetKey;
        return keys;
    }

    private static boolean bounded(final String text)
    {
        return text == null || text.getBytes(StandardCharsets.UTF_8).length <= 128;
    }
}
