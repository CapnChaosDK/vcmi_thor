package eu.vcmi.vcmi;

/** Immutable, bounded native market quote. Quantity is measured in native slider batches. */
final class ThorMarketState
{
    static final int MAX_ROWS = 9;
    static final ThorMarketState EMPTY = new ThorMarketState(0, new int[10], new String[0]);
    final long revision;
    private final int[] data;
    private final String[] names;
    private ThorMarketState(final long revision, final int[] data, final String[] names)
    { this.revision = revision; this.data = data; this.names = names; }

    static ThorMarketState copyOf(final long revision, final int[] data, final String[] names)
    {
        if (revision <= 0 || data == null || names == null || data.length < 10 || data[0] != 1
                || data[1] < 0 || data[1] > 3 || data[2] < 0 || data[2] > 1
                || data[3] < -1 || data[4] < -1 || data[5] < 0 || data[6] < 0 || data[7] < 0
                || data[8] < data[7] || data[9] < 1 || data[9] > MAX_ROWS
                || data.length != 10 + data[9] * 4 || names.length != data[9]) return EMPTY;
        boolean sell = data[3] == -1, buy = data[4] == -1;
        for (int i = 0; i < names.length; ++i)
        {
            final int at = 10 + i * 4;
            if (data[at] < 0 || data[at + 1] < 0 || data[at + 2] < 0 || data[at + 2] > 1
                    || data[at + 3] < 0 || data[at + 3] > 1 || names[i] == null
                    || names[i].getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 256) return EMPTY;
            for (int j = 0; j < i; ++j) if (data[10 + j * 4] == data[at]) return EMPTY;
            sell |= data[at] == data[3] && data[at + 2] == 1;
            buy |= data[at] == data[4] && data[at + 3] == 1;
        }
        if (!sell || !buy || (data[8] > 0 && (data[3] < 0 || data[4] < 0 || data[3] == data[4]
                || data[5] <= 0 || data[6] <= 0 || (long) data[8] * data[5] > Integer.MAX_VALUE
                || (long) data[8] * data[6] > Integer.MAX_VALUE))) return EMPTY;
        if (data[8] > 0)
            for (int i = 0; i < names.length; ++i)
                if (data[10 + i * 4] == data[3] && data[8] > data[11 + i * 4] / data[5]) return EMPTY;
        return new ThorMarketState(revision, data.clone(), names.clone());
    }
    int count() { return names.length; }
    int id(final int row) { return data[10 + row * 4]; }
    int balance(final int row) { return data[11 + row * 4]; }
    String name(final int row) { return names[row]; }
    int selling() { return data[3]; }
    int buying() { return data[4]; }
    int bid() { return data[5]; }
    int offer() { return data[6]; }
    int amount() { return data[7]; }
    int maximum() { return data[8]; }
    int status() { return data[1]; }
    boolean allows(final int command, final int target)
    {
        if (this == EMPTY || !ThorExtendedCommands.known(command)
                || !ThorExtendedCommands.validTarget(command, target)) return false;
        if (command == ThorExtendedCommands.MARKET_CLOSE) return true;
        if (data[2] != 1 || status() == 1) return false;
        if (command == ThorExtendedCommands.MARKET_RESET) return true;
        if (command == ThorExtendedCommands.MARKET_TRADE) return amount() > 0;
        if (command == ThorExtendedCommands.MARKET_AMOUNT) return maximum() > 0 && target <= maximum();
        for (int i = 0; i < count(); ++i)
            if (id(i) == target && (command == ThorExtendedCommands.MARKET_SELL ? data[12 + i * 4] == 1
                    : command == ThorExtendedCommands.MARKET_BUY && data[13 + i * 4] == 1)) return true;
        return false;
    }
}
