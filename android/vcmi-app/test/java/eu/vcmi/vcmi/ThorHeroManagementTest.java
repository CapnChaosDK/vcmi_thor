package eu.vcmi.vcmi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ThorHeroManagementTest
{
    @Test
    public void copiesBoundedSingleHeroArmyAndArtifacts()
    {
        final int[] creatureIds = {-1, 3, -1, -1, -1, -1, -1};
        final int[] counts = {0, 12, 0, 0, 0, 0, 0};
        final String[] creatureNames = {"", "Pikemen", "", "", "", "", ""};
        final long[] creatureKeys = new long[ThorHeroManagement.ARMY_SIZE];
        creatureKeys[1] = ThorVisualAssetKey.forCreature(3);
        final int[] positions = new int[ThorHeroMeetingArtifacts.EQUIPPED_PER_HERO + 1];
        final int[] flags = new int[positions.length];
        final String[] artifactNames = new String[positions.length];
        for (int index = 0; index < positions.length; ++index)
        {
            positions[index] = index;
            artifactNames[index] = "";
        }
        flags[2] = 1;
        artifactNames[2] = "Sword";
        flags[positions.length - 1] = 4;
        final long[] artifactKeys = new long[positions.length];
        artifactKeys[2] = ThorVisualAssetKey.forArtifactType(8);

        final ThorHeroManagement copied = ThorHeroManagement.copyOf(47, "Adelaide", true,
                creatureIds, counts, creatureNames, creatureKeys, positions, flags, artifactNames, artifactKeys);
        assertTrue(copied.complete());
        assertEquals(47, copied.heroId);
        assertEquals(12, copied.counts[1]);
        assertEquals("Sword", copied.artifactNames[2]);
        assertEquals(ThorVisualAssetKey.forCreature(3), copied.creatureVisualAssetKeys[1]);
        assertEquals(ThorVisualAssetKey.forArtifactType(8), copied.artifactVisualAssetKeys[2]);
    }

    @Test
    public void malformedOrOversizedSnapshotsFailClosed()
    {
        final ThorHeroManagement malformed = ThorHeroManagement.copyOf(1, "Hero", true,
                new int[6], new int[6], new String[6], null, new int[19], new int[19], new String[19], null);
        assertFalse(malformed.complete());

        final int[] ids = {-1, -1, -1, -1, -1, -1, -1};
        final int[] counts = new int[ThorHeroManagement.ARMY_SIZE];
        final String[] names = {"", "", "", "", "", "", ""};
        final int[] positions = new int[ThorHeroManagement.ARTIFACT_COUNT + 1];
        final int[] flags = new int[positions.length];
        final String[] artifactNames = new String[positions.length];
        for (int index = 0; index < positions.length; ++index)
        {
            positions[index] = index;
            artifactNames[index] = "";
            if (index >= ThorHeroMeetingArtifacts.EQUIPPED_PER_HERO)
                flags[index] = 4;
        }
        assertFalse(ThorHeroManagement.copyOf(1, "Hero", true, ids, counts, names, null,
                positions, flags, artifactNames, null).complete());
    }

    @Test
    public void actionTargetsStayWithinTheHeroWindowRows()
    {
        assertEquals(1, ThorHeroManagementPair.encodeArmy(0, 1));
        assertEquals(47, ThorHeroManagementPair.encodeArmy(6, 5));
        assertEquals(ThorHeroManagementPair.INVALID, ThorHeroManagementPair.encodeArmy(0, 0));
        assertEquals(ThorHeroManagementPair.INVALID, ThorHeroManagementPair.encodeArmy(7, 0));
        assertEquals(ThorHeroManagement.ARTIFACT_COUNT + 1,
                ThorHeroManagementPair.encodeArtifact(0, 1));
        final int lastPair = ThorHeroManagementPair.encodeArtifact(82, 81);
        assertEquals(82, lastPair / ThorHeroManagement.ARTIFACT_COUNT);
        assertEquals(81, lastPair % ThorHeroManagement.ARTIFACT_COUNT);
        assertEquals(ThorHeroManagementPair.INVALID,
                ThorHeroManagementPair.encodeArtifact(0, ThorHeroManagement.ARTIFACT_COUNT));
    }
}
