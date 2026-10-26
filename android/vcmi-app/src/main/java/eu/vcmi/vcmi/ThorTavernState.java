package eu.vcmi.vcmi;

/** Immutable versioned native Tavern offers, invitations and availability. */
final class ThorTavernState
{
    static final int MAX_ROWS = 258;
    static final ThorTavernState EMPTY = new ThorTavernState(0, new int[10], new String[]{""});
    final long revision;
    private final int[] data;
    private final String[] text;

    private ThorTavernState(long revision, int[] data, String[] text)
    { this.revision = revision; this.data = data; this.text = text; }

    private static boolean bounded(String value, int maximum)
    { return value != null && value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= maximum; }

    static ThorTavernState copyOf(long revision, int[] data, String[] text)
    {
        if (revision <= 0 || data == null || data.length < 10 || data[0] != 1
                || data[9] < 0 || data[9] > MAX_ROWS || data.length != 10 + data[9] * 2
                || text == null || text.length != 1 + data[9] * 2 || !bounded(text[0], 1024)
                || data[1] < -1 || data[2] < -1 || data[3] < 0 || data[4] < 0
                || data[5] < 0 || data[5] > 6) return EMPTY;
        for (int i = 6; i <= 8; ++i) if (data[i] < 0 || data[i] > 1) return EMPTY;
        int offered = 0, invitations = 0;
        boolean selected = data[1] == -1, invited = data[2] == -1;
        for (int row = 0; row < data[9]; ++row)
        {
            int at = 10 + row * 2, flags = data[at + 1];
            if (data[at] < 0 || flags < 1 || flags > 3 || !bounded(text[1 + row * 2], 256)
                    || text[1 + row * 2].isEmpty() || !bounded(text[2 + row * 2], 1024)) return EMPTY;
            for (int j = 0; j < row; ++j) if (data[10 + j * 2] == data[at]) return EMPTY;
            if ((flags & 1) != 0) { ++offered; selected |= data[at] == data[1]; }
            if ((flags & 2) != 0) { ++invitations; invited |= data[at] == data[2]; }
        }
        if (offered > 2 || invitations > 256 || !selected || !invited
                || data[7] == 1 && (data[6] != 1 || data[1] < 0 || data[4] < data[3] || data[5] != 0)) return EMPTY;
        return new ThorTavernState(revision, data.clone(), text.clone());
    }
    int count() { return data[9]; }
    int id(int row) { return data[10 + row * 2]; }
    int flags(int row) { return data[11 + row * 2]; }
    String name(int row) { return text[1 + row * 2]; }
    String details(int row) { return text[2 + row * 2]; }
    String rumor() { return text[0]; }
    int selected() { return data[1]; }
    int invited() { return data[2]; }
    int cost() { return data[3]; }
    int gold() { return data[4]; }
    int reason() { return data[5]; }
    boolean has(int target, int flag)
    {
        for (int row = 0; row < count(); ++row) if (id(row) == target && (flags(row) & flag) != 0) return true;
        return false;
    }
    boolean allows(int command, int target)
    {
        if (this == EMPTY || !ThorExtendedCommands.known(command) || !ThorExtendedCommands.validTarget(command, target)) return false;
        switch (command)
        {
            case ThorExtendedCommands.TAVERN_CLOSE: return true;
            case ThorExtendedCommands.TAVERN_GUILD: return data[8] == 1;
            case ThorExtendedCommands.TAVERN_RECRUIT: return data[7] == 1;
            case ThorExtendedCommands.TAVERN_SELECT: return data[6] == 1 && has(target, 1);
            case ThorExtendedCommands.TAVERN_INVITE: return data[6] == 1 && has(target, 2);
            default: return false;
        }
    }
}
