package eu.vcmi.vcmi;

/** Immutable native route metadata. Independent of minimap RGB/cache ownership. */
final class ThorAdventureRoute
{
    static final int HEADER_FIELDS = 17, NODE_FIELDS = 7, MAX_NODES = 4096;
    static final int CAN_CONFIRM = 1, CAN_CANCEL = 2, CAN_CLEAR = 4, CAN_STOP = 8,
            CAN_CONTINUE = 16, TRUNCATED = 32;
    static final int CONFIRM = 1, CANCEL_PREVIEW = 2, CLEAR_ROUTE = 3, STOP = 4;
    static final ThorAdventureRoute EMPTY = new ThorAdventureRoute(new int[0], new int[0]);
    private final int[] header, nodes;

    private ThorAdventureRoute(final int[] header, final int[] nodes)
    {
        this.header = header;
        this.nodes = nodes;
    }

    static ThorAdventureRoute copyOf(final int[] header, final int[] nodes)
    {
        if (header == null || nodes == null || header.length != HEADER_FIELDS
                || nodes.length % NODE_FIELDS != 0 || nodes.length > MAX_NODES * NODE_FIELDS)
            return EMPTY;
        final int count = nodes.length / NODE_FIELDS;
        if (header[1] < 0 || header[1] > 11 || header[2] < 0 || (header[2] & ~63) != 0
                || !ThorAdventureMap.dimensionsValid(header[3], header[4]) || header[5] < 1 || header[5] > 2
                || header[12] < 0 || header[13] < 0)
            return EMPTY;
        if (header[1] == 0)
            return header[0] == -1 && absent(header, 6) && absent(header, 9)
                    && header[12] == 0 && header[13] == 0 && header[2] == 0 && count == 0
                    && header[16] == 0 && header[14] == -1 && header[15] == -1
                    ? new ThorAdventureRoute(header.clone(), nodes.clone()) : EMPTY;
        if (header[0] < 0 || !tileValid(header, header[6], header[7], header[8])) return EMPTY;
        final int state = header[1], flags = header[2];
        final boolean preview = state == 2 || state == 11;
        final boolean committed = state == 4 || state == 7 || state == 8 || state == 10;
        if (((flags & CAN_CONFIRM) != 0 && !preview)
                || ((flags & CAN_CANCEL) != 0 && !preview && state != 3)
                || ((flags & (CAN_CLEAR | CAN_CONTINUE)) != 0 && !committed)
                || ((flags & CAN_STOP) != 0 && state != 5))
            return EMPTY;
        final boolean destination = tileValid(header, header[9], header[10], header[11]);
        if (count == 0)
        {
            if (header[16] != 0 || header[14] != -1 || header[15] != -1
                    || (flags & (CAN_CONFIRM | CAN_CONTINUE | TRUNCATED)) != 0)
                return EMPTY;
            if (state == 1)
                return absent(header, 9) && flags == 0 ? new ThorAdventureRoute(header.clone(), nodes.clone()) : EMPTY;
            if (state != 3 && state != 9 && state != 10 && state != 11 && state != 5 && state != 6)
                return EMPTY;
            return destination || absent(header, 9) ? new ThorAdventureRoute(header.clone(), nodes.clone()) : EMPTY;
        }
        if (state == 1 || state == 3 || !destination || header[16] < 2 || header[16] < count
                || header[16] > header[3] * header[4] * header[5] * 5
                || header[14] < 0 || header[14] >= 255 || header[15] < 0
                || nodes[0] != header[6] || nodes[1] != header[7] || nodes[2] != header[8] || nodes[3] != 0)
            return EMPTY;
        final boolean truncated = header[16] > count;
        if (truncated != ((flags & TRUNCATED) != 0) || (truncated && count != MAX_NODES)) return EMPTY;
        int previousTurn = 0;
        for (int i = 0; i < nodes.length; i += NODE_FIELDS)
        {
            if (!tileValid(header, nodes[i], nodes[i + 1], nodes[i + 2])
                    || nodes[i + 3] < previousTurn || nodes[i + 3] >= 255 || nodes[i + 4] < 0
                    || nodes[i + 5] < 0 || nodes[i + 5] > 4 || nodes[i + 6] < 0 || nodes[i + 6] > 9)
                return EMPTY;
            previousTurn = nodes[i + 3];
        }
        final int last = nodes.length - NODE_FIELDS;
        if (nodes[last + 3] > header[14] || (!truncated
                && (nodes[last] != header[9] || nodes[last + 1] != header[10] || nodes[last + 2] != header[11]
                    || nodes[last + 3] != header[14] || nodes[last + 4] != header[15])))
            return EMPTY;
        return new ThorAdventureRoute(header.clone(), nodes.clone());
    }

    private static boolean absent(final int[] header, final int offset)
    {
        return header[offset] == -1 && header[offset + 1] == -1 && header[offset + 2] == -1;
    }

    private static boolean tileValid(final int[] header, final int x, final int y, final int level)
    {
        return x >= 0 && x < header[3] && y >= 0 && y < header[4] && level >= 0 && level < header[5];
    }

    boolean valid() { return header.length == HEADER_FIELDS; }
    int value(final int field) { return valid() && field >= 0 && field < HEADER_FIELDS ? header[field] : -1; }
    int size() { return nodes.length / NODE_FIELDS; }
    int node(final int index, final int field) { return nodes[index * NODE_FIELDS + field]; }
    boolean preview() { return value(1) == 2 || value(1) == 11; }
    boolean allows(final int flag) { return valid() && (header[2] & flag) != 0; }
    boolean fits(final ThorAdventureMap map)
    {
        return valid() && map.valid() && header[3] == map.width && header[4] == map.height && header[5] == map.levels;
    }

    /** Draw only adjacent ordinary edges. Portals/layer changes get endpoint markers, never invented lines. */
    boolean connects(final int index, final int level)
    {
        return index > 0 && index < size() && node(index - 1, 2) == level && node(index, 2) == level
                && node(index - 1, 5) == node(index, 5) && node(index, 6) >= 3 && node(index, 6) <= 6
                && Math.max(Math.abs(node(index, 0) - node(index - 1, 0)),
                        Math.abs(node(index, 1) - node(index - 1, 1))) == 1;
    }

    int commandForControl(final int control, final long mask)
    {
        if ((mask & ThorActionIds.maskFor(ThorActionIds.ADVENTURE_ROUTE_COMMAND)) == 0) return 0;
        switch (control)
        {
            case ThorMapControls.CONFIRM: return allows(CAN_CONFIRM) ? CONFIRM : 0;
            case ThorMapControls.CANCEL_PREVIEW: return allows(CAN_CANCEL) ? CANCEL_PREVIEW : 0;
            case ThorMapControls.CLEAR_ROUTE: return allows(CAN_CLEAR) ? CLEAR_ROUTE : 0;
            case ThorMapControls.STOP: return allows(CAN_STOP) ? STOP : 0;
            default: return 0;
        }
    }
}
