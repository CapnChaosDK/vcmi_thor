package eu.vcmi.vcmi;

/** V1 extended namespace. Never pass these IDs to ThorActionIds.maskFor. */
final class ThorExtendedCommands
{
    static final int VERSION = 1, MAX_AVAILABLE = 16;
    static final int AUTO_START = 1, AUTO_STOP = 2, CAST_SPELL = 3, RETREAT = 4, SURRENDER = 5;
    static final int PLAYER_SELECT = 6, FACTION_PREVIOUS = 7, FACTION_NEXT = 8,
            HERO_PREVIOUS = 9, HERO_NEXT = 10, BONUS_PREVIOUS = 11, BONUS_NEXT = 12;
    static final int MARKET_SELL = 13, MARKET_BUY = 14, MARKET_AMOUNT = 15,
            MARKET_TRADE = 16, MARKET_CLOSE = 17, MARKET_RESET = 18;
    static final int TAVERN_SELECT = 19, TAVERN_RECRUIT = 20, TAVERN_INVITE = 21,
            TAVERN_GUILD = 22, TAVERN_CLOSE = 23;
    static final ThorExtendedCommands EMPTY = new ThorExtendedCommands(new int[0], new int[0]);
    private final int[] enabled, active;

    private ThorExtendedCommands(final int[] enabled, final int[] active)
    {
        this.enabled = enabled;
        this.active = active;
    }

    static boolean known(final int command) { return command >= AUTO_START && command <= TAVERN_CLOSE; }

    static int[] encode(final int version, final int command, final int target)
    {
        return version == VERSION && known(command) && validTarget(command, target)
                ? new int[]{version, command, target} : null;
    }

    static boolean validTarget(final int command, final int target)
    {
        if (command == TAVERN_SELECT || command == TAVERN_INVITE) return target >= 0;
        if (command >= MARKET_SELL && command <= MARKET_AMOUNT) return target >= 0;
        return command >= PLAYER_SELECT && command <= BONUS_NEXT ? target >= 0 && target < 8 : target == -1;
    }

    private static boolean valid(final int[] values)
    {
        if (values == null || values.length > MAX_AVAILABLE)
            return false;
        for (int i = 0; i < values.length; ++i)
        {
            if (!known(values[i]) || values[i] >= PLAYER_SELECT) return false;
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
