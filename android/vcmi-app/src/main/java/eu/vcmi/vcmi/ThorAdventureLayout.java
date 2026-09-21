package eu.vcmi.vcmi;

/** Fractions of the bounded lower-display frame, shared by rendering and hit testing. */
final class ThorAdventureLayout
{
    static final float DIVIDER = 0.23f;
    static final float TITLE = 0.09f;
    static final float STATUS = 0.18f;
    static final float TAB_END = 0.13f;
    static final float CONTENT_START = 0.20f;
    static final int HERO_COLUMNS = 2;
    static final int HERO_ROWS = (ThorHeroRoster.MAX_HEROES + HERO_COLUMNS - 1) / HERO_COLUMNS;

    private ThorAdventureLayout()
    {
    }
}
