package eu.vcmi.vcmi;

/** Bounded army snapshots for the active Town Window. */
final class ThorTownManagement
{
    static final int ARMY_SIZE = 7;
    static final ThorTownManagement EMPTY = new ThorTownManagement(-1, -1, -1, "", false,
            new int[0], new int[0], new String[0], new int[0], new int[0], new String[0]);

    final int townId;
    final int garrisonArmyId;
    final int visitingHeroId;
    final String visitingHeroName;
    final boolean locallyControllable;
    final int[] garrisonCreatureIds;
    final int[] garrisonCounts;
    final String[] garrisonNames;
    final int[] visitingCreatureIds;
    final int[] visitingCounts;
    final String[] visitingNames;

    private ThorTownManagement(final int townId, final int garrisonArmyId, final int visitingHeroId,
                               final String visitingHeroName, final boolean locallyControllable,
                               final int[] garrisonCreatureIds, final int[] garrisonCounts,
                               final String[] garrisonNames, final int[] visitingCreatureIds,
                               final int[] visitingCounts, final String[] visitingNames)
    {
        this.townId = townId;
        this.garrisonArmyId = garrisonArmyId;
        this.visitingHeroId = visitingHeroId;
        this.visitingHeroName = ThorContextDetails.orEmpty(visitingHeroName);
        this.locallyControllable = locallyControllable;
        this.garrisonCreatureIds = garrisonCreatureIds;
        this.garrisonCounts = garrisonCounts;
        this.garrisonNames = garrisonNames;
        this.visitingCreatureIds = visitingCreatureIds;
        this.visitingCounts = visitingCounts;
        this.visitingNames = visitingNames;
    }

    static ThorTownManagement copyOf(final int townId, final int garrisonArmyId, final int visitingHeroId,
                                     final String visitingHeroName, final boolean locallyControllable,
                                     final int[] garrisonCreatureIds, final int[] garrisonCounts,
                                     final String[] garrisonNames, final int[] visitingCreatureIds,
                                     final int[] visitingCounts, final String[] visitingNames)
    {
        if (townId < 0 || garrisonArmyId < 0 || (visitingHeroId >= 0 && visitingHeroId == garrisonArmyId)
                || !armyValid(garrisonCreatureIds, garrisonCounts, garrisonNames)
                || !armyValid(visitingCreatureIds, visitingCounts, visitingNames)
                || (visitingHeroId < 0 && hasArmy(visitingCreatureIds)))
            return EMPTY;
        return new ThorTownManagement(townId, garrisonArmyId, visitingHeroId, visitingHeroName,
                locallyControllable && visitingHeroId >= 0, garrisonCreatureIds.clone(), garrisonCounts.clone(),
                cloneNames(garrisonNames), visitingCreatureIds.clone(), visitingCounts.clone(), cloneNames(visitingNames));
    }

    boolean complete()
    {
        return garrisonCreatureIds.length == ARMY_SIZE && visitingCreatureIds.length == ARMY_SIZE;
    }

    private static boolean armyValid(final int[] ids, final int[] counts, final String[] names)
    {
        if (ids == null || counts == null || names == null || ids.length != ARMY_SIZE
                || counts.length != ARMY_SIZE || names.length != ARMY_SIZE)
            return false;
        for (int index = 0; index < ARMY_SIZE; ++index)
            if (names[index] == null || (ids[index] == -1 ? counts[index] != 0 : ids[index] < 0 || counts[index] <= 0))
                return false;
        return true;
    }

    private static boolean hasArmy(final int[] ids)
    {
        for (final int id : ids)
            if (id >= 0)
                return true;
        return false;
    }

    private static String[] cloneNames(final String[] names)
    {
        final String[] copy = names.clone();
        for (int index = 0; index < copy.length; ++index)
            copy[index] = ThorContextDetails.orEmpty(copy[index]);
        return copy;
    }
}
