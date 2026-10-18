package eu.vcmi.vcmi;

/** V1 extended namespace. Never pass these IDs to ThorActionIds.maskFor. */
final class ThorExtendedCommands
{
    static final int VERSION = 1, MAX_AVAILABLE = 16;
    static final int AUTO_START = 1, AUTO_STOP = 2, CAST_SPELL = 3, RETREAT = 4, SURRENDER = 5;
    static final ThorExtendedCommands EMPTY = new ThorExtendedCommands(new int[0], new int[0]);
    private final int[] enabled, active;

    private ThorExtendedCommands(final int[] enabled, final int[] active)
    {
        this.enabled = enabled;
        this.active = active;
    }

    static boolean known(final int command) { return command >= AUTO_START && command <= SURRENDER; }

    static int[] encode(final int version, final int command, final int target)
    {
        return version == VERSION && known(command) && target == -1
                ? new int[]{version, command, target} : null;
    }

    private static boolean valid(final int[] values)
    {
        if (values == null || values.length > MAX_AVAILABLE)
            return false;
        for (int i = 0; i < values.length; ++i)
        {
            if (!known(values[i])) return false;
            for (int j = 0; j < i; ++j)
                if (values[i] == values[j]) return false;
        }
        return true;
    }

    static ThorExtendedCommands copyOf(final int version, final int[] enabled, final int[] active)
    {
        return version == VERSION && valid(enabled) && valid(active)
                ? new ThorExtendedCommands(enabled.clone(), active.clone()) : EMPTY;
    }

    boolean allows(final int command) { return contains(enabled, command); }
    boolean active(final int command) { return contains(active, command); }
    private static boolean contains(final int[] values, final int command)
    {
        for (final int value : values) if (value == command) return true;
        return false;
    }
}
