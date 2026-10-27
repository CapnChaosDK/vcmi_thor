package eu.vcmi.vcmi;

/** Immutable, bounded native Blacksmith offer or visible Mage Guild scrolls. */
final class ThorTownServiceState
{
    static final int BLACKSMITH = 1, GUILD = 2, MAX_ROWS = 128;
    static final ThorTownServiceState EMPTY = new ThorTownServiceState(0, new int[6], new String[]{""});
    final long revision;
    private final int[] data;
    private final String[] text;

    private ThorTownServiceState(long revision, int[] data, String[] text)
    { this.revision = revision; this.data = data; this.text = text; }

    private static boolean bounded(String value, int maximum)
    { return value != null && value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= maximum; }

    static ThorTownServiceState copyOf(long revision, int[] data, String[] text)
    {
        if (revision <= 0 || data == null || data.length < 6 || data[0] != 1
                || (data[1] != BLACKSMITH && data[1] != GUILD) || data[2] < 0 || data[3] < 0 || data[4] < 0
                || data[5] < 0 || data[5] > MAX_ROWS || data.length != 6 + data[5] * 3
                || data[1] == BLACKSMITH && data[5] != 1 || text == null || text.length != 1 + data[5] * 2
                || !bounded(text[0], 2048)) return EMPTY;
        for (int row = 0; row < data[5]; ++row)
        {
            int at = 6 + row * 3;
            if (data[at] < 0 || data[at + 1] < 0 || data[at + 1] > 5
                    || data[at + 2] < 0 || data[at + 2] > 1
                    || data[1] == BLACKSMITH && (data[at + 1] != 0 || data[at + 2] == 1 && data[4] < data[3])
                    || data[1] == GUILD && data[at + 1] == 0 || !bounded(text[1 + row * 2], 256)
                    || text[1 + row * 2].isEmpty() || !bounded(text[2 + row * 2], 4096)) return EMPTY;
            for (int j = 0; j < row; ++j) if (data[6 + j * 3] == data[at]) return EMPTY;
        }
        return new ThorTownServiceState(revision, data.clone(), text.clone());
    }
    int kind() { return data[1]; }
    int owner() { return data[2]; }
    int cost() { return data[3]; }
    int gold() { return data[4]; }
    int count() { return data[5]; }
    int id(int row) { return data[6 + row * 3]; }
    int level(int row) { return data[7 + row * 3]; }
    String name(int row) { return text[1 + row * 2]; }
    String details(int row) { return text[2 + row * 2]; }
    String summary() { return text[0]; }
    String context() { return kind() == BLACKSMITH ? ThorContextIds.BLACKSMITH : ThorContextIds.MAGE_GUILD; }
    boolean allows(int command, int target)
    {
        if (this == EMPTY || !ThorExtendedCommands.known(command) || !ThorExtendedCommands.validTarget(command, target)) return false;
        if (command == ThorExtendedCommands.SERVICE_CLOSE) return true;
        if (command != (kind() == BLACKSMITH ? ThorExtendedCommands.BLACKSMITH_BUY : ThorExtendedCommands.GUILD_RESEARCH)) return false;
        for (int row = 0; row < count(); ++row) if (id(row) == target) return data[8 + row * 3] == 1;
        return false;
    }
}
