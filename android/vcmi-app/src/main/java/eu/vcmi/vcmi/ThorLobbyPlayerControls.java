package eu.vcmi.vcmi;

/** Local view geometry; control IDs are separate from native player-color targets. */
final class ThorLobbyPlayerControls
{
    static int command(final int control) { return control >= 5 && control <= 11 ? control + 1 : 0; }
    static float[] bounds(final int control, final float width, final float height)
    {
        if (width <= 0 || height <= 0 || control < 1 || control > 13) return null;
        float left = .05f, right = .95f, top, bottom;
        if (control <= 4 || control >= 12)
        {
            final boolean rightSide = control == 2 || control == 4 || control == 13;
            left = rightSide ? .515f : .05f; right = rightSide ? .95f : .485f;
            top = control <= 2 ? .01f : control <= 4 ? .14f : .89f;
            bottom = top + .10f;
            // Reserve the existing top-right haptics toggle without changing its input route.
            if (control <= 2) { left = control == 1 ? .05f : .32f; right = control == 1 ? .30f : .57f; }
        }
        else if (control == 5) { top = .375f; bottom = .475f; }
        else
        {
            final int row = (control - 6) / 2;
            left = control % 2 == 0 ? .05f : .79f;
            right = control % 2 == 0 ? .21f : .95f;
            top = .50f + row * .125f; bottom = top + .10f;
        }
        return new float[]{width * left, height * top, width * right, height * bottom};
    }
    static int at(final float x, final float y, final float width, final float height, final boolean players)
    {
        for (int c = 1; c <= (players ? 13 : 2); ++c)
        {
            final float[] b = bounds(c, width, height);
            if (b != null && x > b[0] && x < b[2] && y > b[1] && y < b[3]) return c;
        }
        return 0;
    }
}
