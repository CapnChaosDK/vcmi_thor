package eu.vcmi.vcmi;

final class ThorAdventureRouteState
{
    private long revision;
    private String context = ThorContextIds.UNKNOWN;
    private ThorAdventureRoute route = ThorAdventureRoute.EMPTY;

    void updateContext(final long revision, final String context)
    {
        if (this.revision != revision || !this.context.equals(context)) route = ThorAdventureRoute.EMPTY;
        this.revision = revision;
        this.context = context;
    }

    boolean publish(final long revision, final ThorAdventureRoute route)
    {
        if (revision != this.revision || !ThorContextIds.ADVENTURE_MAP.equals(context)) return false;
        this.route = route == null ? ThorAdventureRoute.EMPTY : route;
        return true;
    }

    ThorAdventureRoute current() { return route; }
}
