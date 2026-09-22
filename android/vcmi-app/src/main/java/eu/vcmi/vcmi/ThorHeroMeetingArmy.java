package eu.vcmi.vcmi;

final class ThorHeroMeetingArmy
{
    static final int SIDE_COUNT = 2;
    static final int ARMY_SIZE = 7;
    static final int SLOT_COUNT = SIDE_COUNT * ARMY_SIZE;
    static final ThorHeroMeetingArmy EMPTY = new ThorHeroMeetingArmy(new int[0], new String[0], new int[0],
            new int[0], new int[0], new int[0], new String[0], new int[0], new int[0]);

    final int[] heroIds, keys, sides, slots, creatureIds, counts, flags;
    final String[] heroNames, creatureNames;

    private ThorHeroMeetingArmy(final int[] heroIds, final String[] heroNames, final int[] keys,
                                final int[] sides, final int[] slots, final int[] creatureIds,
                                final String[] creatureNames, final int[] counts, final int[] flags)
    {
        this.heroIds = heroIds; this.heroNames = heroNames; this.keys = keys; this.sides = sides;
        this.slots = slots; this.creatureIds = creatureIds; this.creatureNames = creatureNames;
        this.counts = counts; this.flags = flags;
    }

    static ThorHeroMeetingArmy copyOf(final int[] heroIds, final String[] heroNames, final int[] keys,
                                      final int[] sides, final int[] slots, final int[] creatureIds,
                                      final String[] creatureNames, final int[] counts, final int[] flags)
    {
        if (heroIds == null || heroNames == null || keys == null || sides == null || slots == null
                || creatureIds == null || creatureNames == null || counts == null || flags == null
                || heroIds.length != SIDE_COUNT || heroNames.length != SIDE_COUNT || keys.length != SLOT_COUNT
                || sides.length != SLOT_COUNT || slots.length != SLOT_COUNT || creatureIds.length != SLOT_COUNT
                || creatureNames.length != SLOT_COUNT || counts.length != SLOT_COUNT || flags.length != SLOT_COUNT)
            return EMPTY;
        final boolean[] seen = new boolean[SLOT_COUNT];
        for (int side = 0; side < SIDE_COUNT; ++side)
            if (heroIds[side] < 0 || heroNames[side] == null)
                return EMPTY;
        for (int index = 0; index < SLOT_COUNT; ++index)
        {
            final boolean occupied = (flags[index] & 1) != 0;
            if (keys[index] < 0 || keys[index] >= SLOT_COUNT || seen[keys[index]] || sides[index] < 0
                    || sides[index] >= SIDE_COUNT || slots[index] < 0 || slots[index] >= ARMY_SIZE
                    || keys[index] != sides[index] * ARMY_SIZE + slots[index] || creatureNames[index] == null
                    || flags[index] < 0 || flags[index] > 3
                    || occupied != (creatureIds[index] >= 0 && counts[index] > 0)
                    || (!occupied && ((flags[index] & 2) != 0 || counts[index] != 0)))
                return EMPTY;
            seen[keys[index]] = true;
        }
        return new ThorHeroMeetingArmy(heroIds.clone(), heroNames.clone(), keys.clone(), sides.clone(), slots.clone(),
                creatureIds.clone(), creatureNames.clone(), counts.clone(), flags.clone());
    }
}
