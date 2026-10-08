package eu.vcmi.vcmi;

/** Local control IDs never share map tile or semantic action targets. */
final class ThorMapControls
{
    static float[] bounds(final int control, final float width, final float height)
    {
        if (control == 7)
            return new float[]{width * 0.035f, height * 0.493f, width * 0.965f, height * 0.538f};
        final int columns = control < 5 ? 5 : 2;
        final int column = control < 5 ? control : control - 5;
        final float left = width * 0.035f;
        final float gap = width * 0.008f;
        final float cell = (width * 0.93f - gap * (columns - 1)) / columns;
        final float top = height * (control < 5 ? 0.365f : 0.433f);
        return new float[]{left + column * (cell + gap), top,
                left + column * (cell + gap) + cell, top + height * 0.058f};
    }

    static int label(final int control)
    {
        switch (control)
        {
            case 0: return R.string.thor_map_fit;
            case 1: return R.string.thor_map_detail_two;
            case 2: return R.string.thor_map_detail_four;
            case 3: return R.string.thor_map_navigate;
            case 4: return R.string.thor_map_pan;
            case 5: return R.string.thor_map_surface;
            case 6: return R.string.thor_map_underground;
            default: return 0;
        }
    }
}
