package eu.vcmi.vcmi;

/** Small transport checks run before Android decodes an image. */
final class ThorVisualAssetPayload
{
    static final int MAX_DIMENSION = 64;
    static final int MAX_BYTES = 32 * 1024;
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10};

    private ThorVisualAssetPayload()
    {
    }

    static boolean isMissing(final long key, final int width, final int height, final byte[] bytes)
    {
        return ThorVisualAssetKey.isValid(key) && width == 0 && height == 0 && bytes != null && bytes.length == 0;
    }

    static boolean isBoundedPng(final long key, final int width, final int height, final byte[] bytes)
    {
        if (!ThorVisualAssetKey.isValid(key) || width <= 0 || height <= 0
                || width > MAX_DIMENSION || height > MAX_DIMENSION || bytes == null
                || bytes.length < 33 || bytes.length > MAX_BYTES)
            return false;
        for (int index = 0; index < PNG_SIGNATURE.length; ++index)
            if (bytes[index] != PNG_SIGNATURE[index])
                return false;
        if (bytes[8] != 0 || bytes[9] != 0 || bytes[10] != 0 || bytes[11] != 13
                || bytes[12] != 'I' || bytes[13] != 'H' || bytes[14] != 'D' || bytes[15] != 'R')
            return false;
        return readBigEndianInt(bytes, 16) == width && readBigEndianInt(bytes, 20) == height;
    }

    private static int readBigEndianInt(final byte[] bytes, final int offset)
    {
        return ((bytes[offset] & 0xff) << 24) | ((bytes[offset + 1] & 0xff) << 16)
                | ((bytes[offset + 2] & 0xff) << 8) | (bytes[offset + 3] & 0xff);
    }

    static boolean isBoundedKeyList(final long[] keys)
    {
        if (keys == null || keys.length > ThorVisualAssetKey.MAX_HERO_MEETING_KEYS)
            return false;
        for (final long key : keys)
            if (key != 0L && !ThorVisualAssetKey.isValid(key))
                return false;
        return true;
    }
}
