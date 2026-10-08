package eu.vcmi.vcmi;

/** Deterministic connected marker clusters built from the immutable current snapshot. */
final class ThorMapInspection
{
    final float[] x, y;
    final int[] count, row, category;
    final int size;

    ThorMapInspection(final ThorAdventureMap map, final ThorMapTransform transform, final float radius)
    {
        this(map, transform, radius, 0);
    }

    ThorMapInspection(final ThorAdventureMap map, final ThorMapTransform transform, final float radius, final int filter)
    {
        final int n = map.objectLabels.length;
        x = new float[n]; y = new float[n]; count = new int[n]; row = new int[n]; category = new int[n];
        final int[] parent = new int[n];
        final boolean[] visible = new boolean[n];
        for (int i = 0; i < n; ++i)
        {
            parent[i] = i;
            x[i] = transform.screenX(map.objects[i * 5 + 1] + 0.5f);
            y[i] = transform.screenY(map.objects[i * 5 + 2] + 0.5f);
            visible[i] = transform.tileAt(x[i], y[i]) >= 0
                    && (filter == 0 || map.objects[i * 5 + 4] == filter);
            if (!visible[i]) continue;
            for (int j = 0; j < i; ++j)
                if (visible[j] && distance(x[i], y[i], x[j], y[j]) <= 4 * radius * radius)
                {
                    final int a = root(parent, i), b = root(parent, j);
                    parent[Math.max(a, b)] = Math.min(a, b);
                }
        }
        final float[] sumX = new float[n], sumY = new float[n];
        final int[] counts = new int[n];
        for (int i = 0; i < n; ++i)
            if (visible[i])
            {
                final int r = root(parent, i);
                sumX[r] += x[i]; sumY[r] += y[i]; ++counts[r];
            }
        int clusters = 0;
        for (int i = 0; i < n; ++i)
            if (counts[i] > 0)
            {
                x[clusters] = sumX[i] / counts[i]; y[clusters] = sumY[i] / counts[i];
                count[clusters] = counts[i]; row[clusters] = i;
                category[clusters] = map.objects[i * 5 + 4];
                ++clusters;
            }
        size = clusters;
    }

    private static int root(final int[] parent, int i)
    {
        while (parent[i] != i) { parent[i] = parent[parent[i]]; i = parent[i]; }
        return i;
    }

    private static float distance(final float x, final float y, final float xx, final float yy)
    { return (x - xx) * (x - xx) + (y - yy) * (y - yy); }

    /** Only one cluster can be inspected. Nearby ambiguous cluster hits fail closed. */
    int at(final float px, final float py, final float radius)
    {
        int hit = -1;
        for (int i = 0; i < size; ++i)
            if (distance(px, py, x[i], y[i]) <= radius * radius)
            {
                if (hit >= 0) return -1;
                hit = i;
            }
        return hit;
    }
}
