package eu.vcmi.vcmi;

final class ThorHeroRoster
{
    static final int MAX_HEROES = 8; // GameConstants::MAX_HEROES_PER_PLAYER
    static final ThorHeroRoster EMPTY = new ThorHeroRoster(new int[0], new String[0], new int[0], new int[0], new int[0]);

    final int[] ids;
    final String[] names;
    final int[] movement;
    final int[] maximum;
    final int[] flags;

    private ThorHeroRoster(final int[] ids, final String[] names, final int[] movement,
                           final int[] maximum, final int[] flags)
    {
        this.ids = ids;
        this.names = names;
        this.movement = movement;
        this.maximum = maximum;
        this.flags = flags;
    }

    static ThorHeroRoster copyOf(final int[] ids, final String[] names, final int[] movement,
                                 final int[] maximum, final int[] flags)
    {
        if (ids == null || names == null || movement == null || maximum == null || flags == null
                || ids.length > MAX_HEROES || ids.length != names.length || ids.length != movement.length
                || ids.length != maximum.length || ids.length != flags.length)
            return EMPTY;
        for (int index = 0; index < ids.length; ++index)
            if (ids[index] < 0 || names[index] == null)
                return EMPTY;
        return new ThorHeroRoster(ids.clone(), names.clone(), movement.clone(), maximum.clone(), flags.clone());
    }
}
