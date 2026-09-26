package eu.vcmi.vcmi;

import org.junit.Test;

import static org.junit.Assert.*;

public class ThorVisualAssetCacheTest
{
    private static ThorHeroMeetingArmies emptyArmies(final long[] portraitKeys)
    {
        final int[] creatureIds = new int[ThorHeroMeetingArmies.SLOT_COUNT];
        final int[] counts = new int[ThorHeroMeetingArmies.SLOT_COUNT];
        final String[] names = new String[ThorHeroMeetingArmies.SLOT_COUNT];
        final int[] flags = new int[ThorHeroMeetingArmies.SLOT_COUNT];
        for (int index = 0; index < ThorHeroMeetingArmies.SLOT_COUNT; ++index)
        {
            creatureIds[index] = -1;
            names[index] = "";
        }
        return ThorHeroMeetingArmies.copyOf(1, 2, new String[]{"Left", "Right"}, new int[]{1, 2},
                creatureIds, counts, names, flags, 1, null, portraitKeys);
    }

    private static byte[] pngHeader(final int width, final int height, final int length)
    {
        final byte[] bytes = new byte[length];
        final byte[] signature = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10};
        System.arraycopy(signature, 0, bytes, 0, signature.length);
        bytes[11] = 13;
        bytes[12] = 'I';
        bytes[13] = 'H';
        bytes[14] = 'D';
        bytes[15] = 'R';
        writeInt(bytes, 16, width);
        writeInt(bytes, 20, height);
        return bytes;
    }

    private static void writeInt(final byte[] bytes, final int offset, final int value)
    {
        bytes[offset] = (byte) (value >>> 24);
        bytes[offset + 1] = (byte) (value >>> 16);
        bytes[offset + 2] = (byte) (value >>> 8);
        bytes[offset + 3] = (byte) value;
    }

    @Test public void keyIdentitySeparatesCreaturesAndArtifactTypesFromInstances()
    {
        final long creature = ThorVisualAssetKey.forCreature(7);
        final long artifactType = ThorVisualAssetKey.forArtifactType(7);
        assertTrue(ThorVisualAssetKey.isCreature(creature));
        assertTrue(ThorVisualAssetKey.isArtifact(artifactType));
        final long heroPortrait = ThorVisualAssetKey.forHeroPortrait(7);
        assertTrue(ThorVisualAssetKey.isHeroPortrait(heroPortrait));
        assertEquals(7, ThorVisualAssetKey.typeId(heroPortrait));
        assertNotEquals(creature, artifactType);
        assertNotEquals(creature, heroPortrait);
        assertNotEquals(artifactType, heroPortrait);
        assertEquals(heroPortrait, ThorVisualAssetKey.forHeroPortrait(7));
        assertNotEquals(heroPortrait, ThorVisualAssetKey.forHeroPortrait(8));
        assertEquals(ThorVisualAssetKey.forArtifactType(7), ThorVisualAssetKey.forArtifactType(7));
        assertNotEquals(ThorVisualAssetKey.forArtifactType(7), ThorVisualAssetKey.forArtifactType(700));
        assertFalse(ThorVisualAssetKey.isValid(0L));
    }

    @Test public void payloadChecksRejectWrongDimensionsMalformedAndOversizedImages()
    {
        final long key = ThorVisualAssetKey.forCreature(3);
        final byte[] valid = pngHeader(32, 28, 33);
        assertTrue(ThorVisualAssetPayload.isBoundedPng(key, 32, 28, valid));
        assertFalse(ThorVisualAssetPayload.isBoundedPng(key, 32, 27, valid));
        assertFalse(ThorVisualAssetPayload.isBoundedPng(key, 65, 28, pngHeader(65, 28, 33)));
        assertFalse(ThorVisualAssetPayload.isBoundedPng(key, 32, 28, new byte[33]));
        assertFalse(ThorVisualAssetPayload.isBoundedPng(key, 32, 28,
                pngHeader(32, 28, ThorVisualAssetPayload.MAX_BYTES + 1)));
        assertTrue(ThorVisualAssetPayload.isBoundedKeyList(new long[ThorVisualAssetKey.MAX_VISUAL_KEYS]));
        assertFalse(ThorVisualAssetPayload.isBoundedKeyList(new long[ThorVisualAssetKey.MAX_VISUAL_KEYS + 1]));
    }

    @Test public void missingMalformedAndOversizedAssetsFallBackWithoutAnImage()
    {
        final ThorVisualAssetCache<String> cache = new ThorVisualAssetCache<>();
        final long key = ThorVisualAssetKey.forArtifactType(4);
        final ThorVisualAssetCache.Decoder<String> decoder = (width, height, encoded) -> "decoded";
        assertTrue(cache.accept(key, 0, 0, new byte[0], decoder));
        assertTrue(cache.knows(key));
        assertNull(cache.get(key));

        final long malformedKey = ThorVisualAssetKey.forCreature(5);
        assertFalse(cache.accept(malformedKey, 32, 32, new byte[33], decoder));
        assertTrue(cache.knows(malformedKey));
        assertNull(cache.get(malformedKey));

        final long oversizedKey = ThorVisualAssetKey.forCreature(6);
        assertFalse(cache.accept(oversizedKey, 32, 32,
                pngHeader(32, 32, ThorVisualAssetPayload.MAX_BYTES + 1), decoder));
        assertTrue(cache.knows(oversizedKey));
        assertNull(cache.get(oversizedKey));

        final long unavailablePortrait = ThorVisualAssetKey.forHeroPortrait(9);
        assertTrue(cache.accept(unavailablePortrait, 0, 0, new byte[0], decoder));
        assertTrue(cache.knows(unavailablePortrait));
        assertNull(cache.get(unavailablePortrait));
    }

    @Test public void visualReferencesRejectStaleRevisionsAndOldContexts()
    {
        final ThorVisualAssetReferences references = new ThorVisualAssetReferences();
        final long adventurePortrait = ThorVisualAssetKey.forHeroPortrait(12);
        references.updateContext(10, ThorContextIds.ADVENTURE_MAP, adventurePortrait);
        assertTrue(references.references(10, adventurePortrait, ThorHeroMeetingArmies.EMPTY,
                ThorHeroMeetingArtifacts.EMPTY));
        assertFalse(references.references(9, adventurePortrait, ThorHeroMeetingArmies.EMPTY,
                ThorHeroMeetingArtifacts.EMPTY));

        final long activeWindowPortrait = ThorVisualAssetKey.forHeroPortrait(18);
        references.updateContext(11, ThorContextIds.HERO_WINDOW, activeWindowPortrait);
        assertFalse(references.references(11, adventurePortrait, ThorHeroMeetingArmies.EMPTY,
                ThorHeroMeetingArtifacts.EMPTY));
        assertTrue(references.references(11, activeWindowPortrait, ThorHeroMeetingArmies.EMPTY,
                ThorHeroMeetingArtifacts.EMPTY));

        references.updateContext(12, ThorContextIds.UNKNOWN, activeWindowPortrait);
        assertFalse(references.references(12, activeWindowPortrait, ThorHeroMeetingArmies.EMPTY,
                ThorHeroMeetingArtifacts.EMPTY));
    }

    @Test public void meetingPortraitsAreReferencedOnlyForTheirCurrentOrderedHeroes()
    {
        final long left = ThorVisualAssetKey.forHeroPortrait(4);
        final long right = ThorVisualAssetKey.forHeroPortrait(9);
        final ThorHeroMeetingArmies armies = emptyArmies(new long[]{left, right});
        final ThorVisualAssetReferences references = new ThorVisualAssetReferences();
        references.updateContext(33, ThorContextIds.HERO_MEETING, 0L);

        assertTrue(references.references(33, left, armies, ThorHeroMeetingArtifacts.EMPTY));
        assertTrue(references.references(33, right, armies, ThorHeroMeetingArtifacts.EMPTY));
        assertFalse(references.references(34, left, armies, ThorHeroMeetingArtifacts.EMPTY));
        assertFalse(references.references(33, ThorVisualAssetKey.forHeroPortrait(5), armies,
                ThorHeroMeetingArtifacts.EMPTY));
    }

    @Test public void existingCreatureAndArtifactKeysRemainReferencedInMeeting()
    {
        final int[] creatureIds = new int[ThorHeroMeetingArmies.SLOT_COUNT];
        final int[] counts = new int[ThorHeroMeetingArmies.SLOT_COUNT];
        final String[] creatureNames = new String[ThorHeroMeetingArmies.SLOT_COUNT];
        final int[] armyFlags = new int[ThorHeroMeetingArmies.SLOT_COUNT];
        final long[] creatureKeys = new long[ThorHeroMeetingArmies.SLOT_COUNT];
        for (int index = 0; index < ThorHeroMeetingArmies.SLOT_COUNT; ++index)
        {
            creatureIds[index] = -1;
            creatureNames[index] = "";
        }
        creatureIds[0] = 4;
        counts[0] = 1;
        creatureNames[0] = "Pikeman";
        armyFlags[0] = 1;
        creatureKeys[0] = ThorVisualAssetKey.forCreature(4);
        final ThorHeroMeetingArmies armies = ThorHeroMeetingArmies.copyOf(1, 2, new String[]{"Left", "Right"},
                new int[]{1, 2}, creatureIds, counts, creatureNames, armyFlags, 1, creatureKeys,
                new long[]{ThorVisualAssetKey.forHeroPortrait(3), ThorVisualAssetKey.forHeroPortrait(5)});

        final int[] positions = new int[ThorHeroMeetingArtifacts.SLOT_COUNT];
        final int[] artifactFlags = new int[ThorHeroMeetingArtifacts.SLOT_COUNT];
        final String[] artifactNames = new String[ThorHeroMeetingArtifacts.SLOT_COUNT];
        final long[] artifactKeys = new long[ThorHeroMeetingArtifacts.SLOT_COUNT];
        for (int index = 0; index < positions.length; ++index)
        {
            positions[index] = index % ThorHeroMeetingArtifacts.PER_HERO;
            artifactFlags[index] = positions[index] >= ThorHeroMeetingArtifacts.EQUIPPED_PER_HERO ? 4 : 0;
            artifactNames[index] = "";
        }
        artifactFlags[0] = 1;
        artifactNames[0] = "Relic";
        artifactKeys[0] = ThorVisualAssetKey.forArtifactType(16);
        final ThorHeroMeetingArtifacts artifacts = ThorHeroMeetingArtifacts.copyOf(1, 2,
                new String[]{"Left", "Right"}, positions, artifactFlags, artifactNames, artifactKeys);

        final ThorVisualAssetReferences references = new ThorVisualAssetReferences();
        references.updateContext(40, ThorContextIds.HERO_MEETING, 0L);
        assertTrue(references.references(40, creatureKeys[0], armies, artifacts));
        assertTrue(references.references(40, artifactKeys[0], armies, artifacts));
        assertFalse(references.references(40, ThorVisualAssetKey.forArtifactType(17), armies, artifacts));
    }

    @Test public void duplicateAssetsReuseOneEntryAndEntryLruRemainsBounded()
    {
        final ThorVisualAssetCache<String> cache = new ThorVisualAssetCache<>();
        final long first = ThorVisualAssetKey.forCreature(0);
        assertTrue(cache.put(first, "first", 4));
        assertTrue(cache.put(first, "duplicate", 4));
        assertEquals(1, cache.size());
        assertEquals("first", cache.get(first));

        for (int index = 1; index < ThorVisualAssetCache.MAX_ENTRIES; ++index)
            assertTrue(cache.put(ThorVisualAssetKey.forArtifactType(index), "asset", 4));
        assertEquals(ThorVisualAssetCache.MAX_ENTRIES, cache.size());
        assertNotNull(cache.get(first)); // Refresh the older entry immediately before eviction.
        assertTrue(cache.put(ThorVisualAssetKey.forArtifactType(ThorVisualAssetCache.MAX_ENTRIES), "asset", 4));
        assertEquals(ThorVisualAssetCache.MAX_ENTRIES, cache.size());
        assertNotNull(cache.get(first));
        assertFalse(cache.knows(ThorVisualAssetKey.forArtifactType(1)));
        assertTrue(cache.bytes() <= ThorVisualAssetCache.MAX_BYTES);
    }

    @Test public void byteBudgetEvictsLeastRecentlyUsedAssets()
    {
        final ThorVisualAssetCache<String> cache = new ThorVisualAssetCache<>();
        final long first = ThorVisualAssetKey.forCreature(0);
        assertTrue(cache.put(first, "first", 4));

        for (int index = 1; index <= 52; ++index)
            assertTrue(cache.put(ThorVisualAssetKey.forArtifactType(index), "asset", 20_000));

        assertNotNull(cache.get(first)); // Keep this entry newer than the oversized working set.
        assertTrue(cache.put(ThorVisualAssetKey.forArtifactType(53), "asset", 20_000));
        assertNotNull(cache.get(first));
        assertFalse(cache.knows(ThorVisualAssetKey.forArtifactType(1)));
        assertTrue(cache.bytes() <= ThorVisualAssetCache.MAX_BYTES);
    }
}
