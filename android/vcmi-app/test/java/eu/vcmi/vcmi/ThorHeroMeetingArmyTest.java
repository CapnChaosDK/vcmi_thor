package eu.vcmi.vcmi;

import static org.junit.Assert.*;

import org.junit.Test;

public class ThorHeroMeetingArmyTest
{
    private static ThorHeroMeetingArmy valid()
    {
        final int[] heroes = {10, 11};
        final String[] heroNames = {"Left", "Right"};
        final int[] keys = new int[14], sides = new int[14], slots = new int[14];
        final int[] creatures = new int[14], counts = new int[14], flags = new int[14];
        final String[] names = new String[14];
        for (int i = 0; i < 14; ++i)
        {
            keys[i] = i; sides[i] = i / 7; slots[i] = i % 7; creatures[i] = -1; names[i] = "";
        }
        creatures[0] = 3; counts[0] = 12; flags[0] = 3; names[0] = "Pikemen";
        return ThorHeroMeetingArmy.copyOf(heroes, heroNames, keys, sides, slots, creatures, names, counts, flags);
    }

    @Test public void stableActionContract()
    {
        assertEquals(15, ThorActionIds.HERO_MEETING_MOVE_STACK);
        assertEquals(16384, ThorActionIds.maskFor(ThorActionIds.HERO_MEETING_MOVE_STACK));
        assertEquals(14, ThorActionIds.SELECT_TOWN);
    }

    @Test public void validPayloadIsDefensivelyCopied()
    {
        final ThorHeroMeetingArmy army = valid();
        assertEquals(14, army.keys.length);
        assertEquals("Pikemen", army.creatureNames[0]);
        army.keys[0] = 99;
        assertNotSame(ThorHeroMeetingArmy.EMPTY, valid());
    }

    @Test public void duplicateAndMalformedPayloadsFailClosed()
    {
        final ThorHeroMeetingArmy army = valid();
        army.keys[1] = 0;
        assertSame(ThorHeroMeetingArmy.EMPTY, ThorHeroMeetingArmy.copyOf(army.heroIds, army.heroNames, army.keys,
                army.sides, army.slots, army.creatureIds, army.creatureNames, army.counts, army.flags));
        assertSame(ThorHeroMeetingArmy.EMPTY, ThorHeroMeetingArmy.copyOf(new int[0], new String[0], new int[0],
                new int[0], new int[0], new int[0], new String[0], new int[0], new int[0]));
    }
}
