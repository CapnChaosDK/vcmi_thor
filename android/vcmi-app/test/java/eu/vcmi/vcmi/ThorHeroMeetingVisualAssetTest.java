package eu.vcmi.vcmi;

import org.junit.Test;

import static org.junit.Assert.*;

public class ThorHeroMeetingVisualAssetTest
{
    @Test public void creatureKeysStayBoundToTheCorrectArmySlots()
    {
        final int[] armyIds = {1, 2};
        final int[] creatureIds = new int[ThorHeroMeetingArmies.SLOT_COUNT];
        final int[] counts = new int[ThorHeroMeetingArmies.SLOT_COUNT];
        final String[] names = new String[ThorHeroMeetingArmies.SLOT_COUNT];
        final int[] flags = new int[ThorHeroMeetingArmies.SLOT_COUNT];
        final long[] keys = new long[ThorHeroMeetingArmies.SLOT_COUNT];
        for (int index = 0; index < ThorHeroMeetingArmies.SLOT_COUNT; ++index)
        {
            creatureIds[index] = -1;
            names[index] = "";
        }
        creatureIds[0] = 4;
        counts[0] = 12;
        names[0] = "Pikemen";
        flags[0] = 1;
        keys[0] = ThorVisualAssetKey.forCreature(4);
        creatureIds[8] = 9;
        counts[8] = 3;
        names[8] = "Griffins";
        flags[8] = 1;
        keys[8] = ThorVisualAssetKey.forArtifactType(9); // Mismatched decorative key fails closed.

        final ThorHeroMeetingArmies armies = ThorHeroMeetingArmies.copyOf(1, 2, new String[]{"Left", "Right"},
                armyIds, creatureIds, counts, names, flags, 1, keys);
        assertTrue(armies.complete());
        assertEquals(ThorVisualAssetKey.forCreature(4), armies.visualAssetKeys[0]);
        assertEquals(0L, armies.visualAssetKeys[8]);
        assertEquals(0L, armies.visualAssetKeys[1]);
    }

    @Test public void portraitKeysStayBoundToTheOrderedHeroTypes()
    {
        final int[] armyIds = {1, 2};
        final int[] creatureIds = new int[ThorHeroMeetingArmies.SLOT_COUNT];
        final int[] counts = new int[ThorHeroMeetingArmies.SLOT_COUNT];
        final String[] names = new String[ThorHeroMeetingArmies.SLOT_COUNT];
        final int[] flags = new int[ThorHeroMeetingArmies.SLOT_COUNT];
        for (int index = 0; index < ThorHeroMeetingArmies.SLOT_COUNT; ++index)
        {
            creatureIds[index] = -1;
            names[index] = "";
        }
        final long[] portraitKeys = {ThorVisualAssetKey.forHeroPortrait(17), ThorVisualAssetKey.forHeroPortrait(23)};
        final ThorHeroMeetingArmies armies = ThorHeroMeetingArmies.copyOf(1, 2, new String[]{"Left", "Right"},
                armyIds, creatureIds, counts, names, flags, 1, null, portraitKeys);

        assertTrue(armies.complete());
        assertEquals(portraitKeys[0], armies.heroPortraitAssetKeys[0]);
        assertEquals(portraitKeys[1], armies.heroPortraitAssetKeys[1]);
        assertNotEquals(armies.heroPortraitAssetKeys[0], armies.heroPortraitAssetKeys[1]);

        final long[] wrongKind = {ThorVisualAssetKey.forCreature(17), portraitKeys[1]};
        final ThorHeroMeetingArmies sanitized = ThorHeroMeetingArmies.copyOf(1, 2, new String[]{"Left", "Right"},
                armyIds, creatureIds, counts, names, flags, 1, null, wrongKind);
        assertEquals(0L, sanitized.heroPortraitAssetKeys[0]);
        assertEquals(portraitKeys[1], sanitized.heroPortraitAssetKeys[1]);
    }

    @Test public void lockedArtifactIconAndEmptyRowsKeepTheirExistingState()
    {
        final int[] positions = new int[ThorHeroMeetingArtifacts.SLOT_COUNT];
        final int[] flags = new int[ThorHeroMeetingArtifacts.SLOT_COUNT];
        final String[] names = new String[ThorHeroMeetingArtifacts.SLOT_COUNT];
        final long[] keys = new long[ThorHeroMeetingArtifacts.SLOT_COUNT];
        for (int index = 0; index < ThorHeroMeetingArtifacts.SLOT_COUNT; ++index)
        {
            positions[index] = index % ThorHeroMeetingArtifacts.PER_HERO;
            flags[index] = positions[index] >= ThorHeroMeetingArtifacts.EQUIPPED_PER_HERO ? 4 : 0;
            names[index] = "";
        }
        flags[0] = 1 | 2;
        names[0] = "Locked Relic";
        keys[0] = ThorVisualAssetKey.forArtifactType(16);

        final ThorHeroMeetingArtifacts artifacts = ThorHeroMeetingArtifacts.copyOf(1, 2,
                new String[]{"Left", "Right"}, positions, flags, names, keys);
        assertTrue(artifacts.complete());
        assertEquals(1 | 2, artifacts.flags[0]);
        assertEquals("Locked Relic", artifacts.names[0]);
        assertEquals(ThorVisualAssetKey.forArtifactType(16), artifacts.visualAssetKeys[0]);
        assertEquals(0L, artifacts.visualAssetKeys[1]);
        assertEquals(0L, artifacts.visualAssetKeys[24]);
    }

    @Test public void identicalArtifactTypesUseOneVisualIdentityRegardlessOfInstances()
    {
        final long firstInstanceVisualKey = ThorVisualAssetKey.forArtifactType(21);
        final long secondInstanceVisualKey = ThorVisualAssetKey.forArtifactType(21);
        assertEquals(firstInstanceVisualKey, secondInstanceVisualKey);
        assertNotEquals(firstInstanceVisualKey, ThorVisualAssetKey.forArtifactType(22));
    }
}
