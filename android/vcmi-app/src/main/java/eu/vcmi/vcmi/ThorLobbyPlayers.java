package eu.vcmi.vcmi;

/** Immutable bounded native lobby snapshot; capabilities and option names are native decisions. */
final class ThorLobbyPlayers
{
    static final ThorLobbyPlayers EMPTY = new ThorLobbyPlayers(new int[0], new String[0]);
    private final int[] data;
    private final String[] labels;
    private ThorLobbyPlayers(final int[] data, final String[] labels) { this.data = data; this.labels = labels; }
    int size() { return data.length / 2; }
    int slot(final int row) { return row >= 0 && row < size() ? data[row * 2] : -1; }
    int flags(final int row) { return row >= 0 && row < size() ? data[row * 2 + 1] : 0; }
    String label(final int row, final int column)
    {
        return row >= 0 && row < size() && column >= 0 && column < 5 ? labels[row * 5 + column] : "";
    }
    boolean allows(final int row, final int command)
    {
        if (slot(row) < 0 || command < 6 || command > 12 || (flags(row) & 64) != 0) return false;
        final int bit = command == 6 ? 1 : command <= 8 ? 2 : command <= 10 ? 4 : 8;
        return (flags(row) & bit) != 0;
    }
    static ThorLobbyPlayers copyOf(final int[] data, final String[] labels)
    {
        if (data == null || labels == null || data.length > 16 || data.length % 2 != 0
                || labels.length != data.length / 2 * 5) return EMPTY;
        int seen = 0;
        for (int i = 0; i < data.length; i += 2)
        {
            final int slot = data[i], flags = data[i + 1];
            if (slot < 0 || slot >= 8 || flags < 0 || flags >= 128 || (seen & (1 << slot)) != 0) return EMPTY;
            seen |= 1 << slot;
        }
        for (final String text : labels)
            if (text == null || text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 256) return EMPTY;
        return new ThorLobbyPlayers(data.clone(), labels.clone());
    }
}
