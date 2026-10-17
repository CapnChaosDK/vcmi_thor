package eu.vcmi.vcmi;

/** Local control IDs never share map tile or semantic action targets. */
final class ThorMapControls
{
    static final int PLAN = 8, CONFIRM = 20, CANCEL_PREVIEW = 21, CLEAR_ROUTE = 22, STOP = 23, CONTINUE = 24;

    static float[] bounds(final int control, final float width, final float height)
    {
        final int columns, column;
        final float top;
        if (control >= 20 && control <= 24)
        {
            columns = 5; column = control - 20; top = 0.491f;
        }
        else if (control == 3 || control == 4 || control == 8 || control == 5 || control == 6)
        {
            columns = 5;
            column = control == 3 ? 0 : control == 4 ? 1 : control == 8 ? 2 : control - 2;
            top = 0.428f;
        }
        else
        {
            columns = 4; column = control == 7 ? 3 : control; top = 0.365f;
        }
        final float left = width * 0.035f, gap = width * 0.008f;
        final float cell = (width * 0.93f - gap * (columns - 1)) / columns;
        return new float[]{left + column * (cell + gap), height * top,
                left + column * (cell + gap) + cell, height * (top + 0.056f)};
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
            case 8: return R.string.thor_map_plan;
            case 20: return R.string.thor_route_confirm;
            case 21: return R.string.thor_route_cancel;
            case 22: return R.string.thor_route_clear;
            case 23: return R.string.thor_route_stop;
            case 24: return R.string.thor_route_continue;
            case 5: return R.string.thor_map_surface;
            case 6: return R.string.thor_map_underground;
            default: return 0;
        }
    }
}
