package eu.vcmi.vcmi;

import org.junit.Test;

import static org.junit.Assert.*;

public class ThorVisualAssetCacheTest
{
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
        assertNotEquals(creature, artifactType);
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
        assertFalse(ThorVisualAssetPayload.isBoundedKeyList(new long[ThorVisualAssetKey.MAX_HERO_MEETING_KEYS + 1]));
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
    }

    @Test public void duplicateAssetsReuseOneEntryAndLruRemainsBounded()
    {
        final ThorVisualAssetCache<String> cache = new ThorVisualAssetCache<>();
        final long first = ThorVisualAssetKey.forCreature(0);
        assertTrue(cache.put(first, "first", 4));
        assertTrue(cache.put(first, "duplicate", 4));
        assertEquals(1, cache.size());
        assertEquals("first", cache.get(first));

        for (int index = 1; index <= ThorVisualAssetCache.MAX_ENTRIES; ++index)
            assertTrue(cache.put(ThorVisualAssetKey.forArtifactType(index), "asset", 20_000));
        assertTrue(cache.size() <= ThorVisualAssetCache.MAX_ENTRIES);
        assertTrue(cache.bytes() <= ThorVisualAssetCache.MAX_BYTES);
        assertNotNull(cache.get(first));
        assertFalse(cache.knows(ThorVisualAssetKey.forArtifactType(1)));
    }
}
