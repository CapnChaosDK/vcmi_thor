package eu.vcmi.vcmi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Revision-scoped, presentation-local plan for splitting one stack across compatible army slots. */
final class ThorHeroMeetingRedistributionState
{
    static final int MAX_DESTINATIONS = ThorHeroMeetingArmies.SLOT_COUNT - 1;

    private long revision;
    private int sourceKey = -1;
    private int sourceCreatureId = -1;
    private int sourceCount;
    private ThorHeroMeetingArmies sourceSnapshot;
    private int maximumAllocated;
    private int selectedDestination = -1;
    private final int[] allocations = new int[ThorHeroMeetingArmies.SLOT_COUNT];

    boolean begin(final long renderedRevision, final int key, final ThorHeroMeetingArmies armies)
    {
        cancel();
        if (renderedRevision <= 0 || !armies.complete() || !armies.locallyControllable
                || key < 0 || key >= ThorHeroMeetingArmies.SLOT_COUNT || (armies.flags[key] & 1) == 0
                || armies.counts[key] < 2 || armies.creatureIds[key] < 0)
            return false;
        final int expectedHero = key < 7 ? armies.leftHeroId : armies.rightHeroId;
        if (armies.armyIds[key / 7] != expectedHero)
            return false;
        revision = renderedRevision;
        sourceKey = key;
        sourceCreatureId = armies.creatureIds[key];
        sourceCount = armies.counts[key];
        sourceSnapshot = armies;
        // Hero Meeting armies must retain a final creature in the source stack.
        maximumAllocated = sourceCount - 1;
        return maximumAllocated > 0;
    }

    boolean selectDestination(final long currentRevision, final int key, final ThorHeroMeetingArmies armies)
    {
        if (!isCurrent(currentRevision) || !isSourceCurrent(armies) || !isCompatibleDestination(key, armies))
            return false;
        final int capacity = destinationCapacity(key, armies);
        if (capacity <= 0 || (allocations[key] == 0 && remaining() <= 0))
            return false;
        selectedDestination = key;
        if (allocations[key] == 0)
            allocations[key] = 1;
        return true;
    }

    boolean isCompatibleDestination(final int key, final ThorHeroMeetingArmies armies)
    {
        if (!isActive() || !isSourceCurrent(armies) || key < 0 || key >= ThorHeroMeetingArmies.SLOT_COUNT
                || key == sourceKey)
            return false;
        final int expectedHero = key < 7 ? armies.leftHeroId : armies.rightHeroId;
        if (armies.armyIds[key / 7] != expectedHero)
            return false;
        final boolean occupied = (armies.flags[key] & 1) != 0;
        return !occupied || (armies.creatureIds[key] == sourceCreatureId && armies.counts[key] > 0);
    }

    void adjust(final long currentRevision, final int delta, final ThorHeroMeetingArmies armies)
    {
        if (!isCurrent(currentRevision) || !isSourceCurrent(armies) || selectedDestination < 0 || delta == 0)
            return;
        final int current = allocations[selectedDestination];
        final int otherAllocations = totalAllocated() - current;
        final long upperBound = Math.min((long) destinationCapacity(selectedDestination, armies),
                (long) maximumAllocated - otherAllocations);
        allocations[selectedDestination] = (int) Math.max(0L,
                Math.min(upperBound, (long) current + delta));
    }

    boolean canConfirm(final long currentRevision, final ThorHeroMeetingArmies armies)
    {
        return isCurrent(currentRevision) && isSourceCurrent(armies)
                && totalAllocated() > 0 && destinationCount() > 0;
    }

    int[] destinationArmyIds(final long currentRevision, final ThorHeroMeetingArmies armies)
    {
        return buildPlan(currentRevision, armies, 0);
    }

    int[] destinationSlots(final long currentRevision, final ThorHeroMeetingArmies armies)
    {
        return buildPlan(currentRevision, armies, 1);
    }

    int[] amounts(final long currentRevision, final ThorHeroMeetingArmies armies)
    {
        return buildPlan(currentRevision, armies, 2);
    }

    private int[] buildPlan(final long currentRevision, final ThorHeroMeetingArmies armies, final int field)
    {
        if (!canConfirm(currentRevision, armies))
            return new int[0];
        final List<Integer> result = new ArrayList<>();
        for (int key = 0; key < allocations.length; ++key)
        {
            if (allocations[key] <= 0)
                continue;
            if (!isCompatibleDestination(key, armies))
                return new int[0];
            if (field == 0)
                result.add(armies.armyIds[key / 7]);
            else if (field == 1)
                result.add(key % 7);
            else
                result.add(allocations[key]);
        }
        if (result.isEmpty() || result.size() > MAX_DESTINATIONS)
            return new int[0];
        final int[] values = new int[result.size()];
        for (int index = 0; index < values.length; ++index)
            values[index] = result.get(index);
        return values;
    }

    private int destinationCapacity(final int key, final ThorHeroMeetingArmies armies)
    {
        if (!isCompatibleDestination(key, armies))
            return 0;
        final boolean occupied = (armies.flags[key] & 1) != 0;
        final long space = occupied ? (long) Integer.MAX_VALUE - armies.counts[key] : maximumAllocated;
        return (int) Math.max(0L, Math.min((long) maximumAllocated, space));
    }

    private int destinationCount()
    {
        int result = 0;
        for (int amount : allocations)
            if (amount > 0)
                ++result;
        return result;
    }

    int totalAllocated()
    {
        long total = 0;
        for (int amount : allocations)
            total += amount;
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    int remaining()
    {
        return Math.max(0, maximumAllocated - totalAllocated());
    }

    int allocationFor(final int key)
    {
        return key < 0 || key >= allocations.length ? 0 : allocations[key];
    }

    boolean isCurrent(final long currentRevision)
    {
        return isActive() && revision == currentRevision;
    }

    boolean isForCurrentArmies(final ThorHeroMeetingArmies armies)
    {
        return isSourceCurrent(armies);
    }

    private boolean isSourceCurrent(final ThorHeroMeetingArmies armies)
    {
        return isActive() && armies.complete() && armies.locallyControllable
                && sourceSnapshot != null
                && sourceSnapshot.leftHeroId == armies.leftHeroId
                && sourceSnapshot.rightHeroId == armies.rightHeroId
                && sourceSnapshot.locallyControllable == armies.locallyControllable
                && Arrays.equals(sourceSnapshot.armyIds, armies.armyIds)
                && Arrays.equals(sourceSnapshot.creatureIds, armies.creatureIds)
                && Arrays.equals(sourceSnapshot.counts, armies.counts)
                && Arrays.equals(sourceSnapshot.creatureNames, armies.creatureNames)
                && Arrays.equals(sourceSnapshot.flags, armies.flags)
                && Arrays.equals(sourceSnapshot.heroNames, armies.heroNames);
    }

    boolean isActive()
    {
        return revision > 0 && sourceKey >= 0;
    }

    void cancel()
    {
        revision = 0;
        sourceKey = -1;
        sourceCreatureId = -1;
        sourceCount = 0;
        sourceSnapshot = null;
        maximumAllocated = 0;
        selectedDestination = -1;
        for (int index = 0; index < allocations.length; ++index)
            allocations[index] = 0;
    }

    int sourceKey() { return sourceKey; }
    int sourceCount() { return sourceCount; }
    int maximumAllocated() { return maximumAllocated; }
    int selectedDestination() { return selectedDestination; }
}
