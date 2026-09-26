package eu.vcmi.vcmi;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Bounded process-local LRU for decoded icons and negative (unavailable) results. */
final class ThorVisualAssetCache<T>
{
    static final int MAX_ENTRIES = 64;
    static final int MAX_BYTES = 1024 * 1024;

    interface Decoder<T>
    {
        T decode(int width, int height, byte[] encoded);
    }

    private static final class Entry<T>
    {
        final T value;
        final int bytes;

        Entry(final T value, final int bytes)
        {
            this.value = value;
            this.bytes = bytes;
        }
    }

    private final LinkedHashMap<Long, Entry<T>> entries = new LinkedHashMap<>(16, 0.75f, true);
    private int currentBytes;

    synchronized boolean knows(final long key)
    {
        return entries.get(key) != null;
    }

    synchronized T get(final long key)
    {
        final Entry<T> entry = entries.get(key);
        return entry == null ? null : entry.value;
    }

    synchronized boolean accept(final long key, final int width, final int height, final byte[] encoded,
                                final Decoder<T> decoder)
    {
        if (!ThorVisualAssetKey.isValid(key) || decoder == null)
            return false;
        if (entries.get(key) != null)
            return true;
        if (ThorVisualAssetPayload.isMissing(key, width, height, encoded))
        {
            markUnavailable(key);
            return true;
        }
        if (!ThorVisualAssetPayload.isBoundedPng(key, width, height, encoded))
        {
            markUnavailable(key);
            return false;
        }
        final T decoded;
        try
        {
            decoded = decoder.decode(width, height, encoded);
        }
        catch (final RuntimeException exception)
        {
            markUnavailable(key);
            return false;
        }
        if (decoded == null)
        {
            markUnavailable(key);
            return false;
        }
        return put(key, decoded, width * height * 4);
    }

    synchronized boolean put(final long key, final T value, final int bytes)
    {
        if (!ThorVisualAssetKey.isValid(key) || value == null || bytes <= 0 || bytes > MAX_BYTES)
            return false;
        if (entries.get(key) != null)
            return true;
        entries.put(key, new Entry<>(value, bytes));
        currentBytes += bytes;
        trim();
        return entries.containsKey(key);
    }

    synchronized void markUnavailable(final long key)
    {
        if (!ThorVisualAssetKey.isValid(key))
            return;
        remove(key);
        entries.put(key, new Entry<>(null, 0));
        trim();
    }

    private void remove(final long key)
    {
        final Entry<T> previous = entries.remove(key);
        if (previous != null)
            currentBytes -= previous.bytes;
    }

    private void trim()
    {
        final Iterator<Map.Entry<Long, Entry<T>>> iterator = entries.entrySet().iterator();
        while ((entries.size() > MAX_ENTRIES || currentBytes > MAX_BYTES) && iterator.hasNext())
        {
            final Entry<T> evicted = iterator.next().getValue();
            currentBytes -= evicted.bytes;
            iterator.remove();
        }
    }

    synchronized int size()
    {
        return entries.size();
    }

    synchronized int bytes()
    {
        return currentBytes;
    }
}
