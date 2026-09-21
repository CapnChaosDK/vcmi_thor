package eu.vcmi.vcmi;

final class ThorTownRoster
{
    static final int MAX_TOWNS = 64;
    static final ThorTownRoster EMPTY = new ThorTownRoster(new int[0], new String[0], new int[0]);

    final int[] ids;
    final String[] names;
    final int[] flags;

    private ThorTownRoster(final int[] ids, final String[] names, final int[] flags)
    {
        this.ids = ids;
        this.names = names;
        this.flags = flags;
    }

    static ThorTownRoster copyOf(final int[] ids, final String[] names, final int[] flags)
    {
        if (ids == null || names == null || flags == null || ids.length > MAX_TOWNS
                || ids.length != names.length || ids.length != flags.length)
            return EMPTY;
        for (int index = 0; index < ids.length; ++index)
            if (ids[index] < 0 || names[index] == null)
                return EMPTY;
        return new ThorTownRoster(ids.clone(), names.clone(), flags.clone());
    }
}
