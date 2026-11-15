package eu.vcmi.vcmi;

/** Local control IDs never share map tile or semantic action targets. */
final class ThorMapControls
{
    static final int PLAN = 8, CONFIRM = 20, CANCEL_PREVIEW = 21, CLEAR_ROUTE = 22, STOP = 23, CONTINUE = 24;

    /** One native-authorized mapping shared by rendering and release handling. */
    static int routeAction(final int control, final ThorAdventureMap map,
            final ThorAdventureRoute route, final long mask)
    {
        final int flag;
        switch (control)
        {
            case CONFIRM: flag = ThorAdventureRoute.CAN_CONFIRM; break;
            case CANCEL_PREVIEW: flag = ThorAdventureRoute.CAN_CANCEL; break;
            case CLEAR_ROUTE: flag = ThorAdventureRoute.CAN_CLEAR; break;
            case STOP: flag = ThorAdventureRoute.CAN_STOP; break;
            case CONTINUE: flag = ThorAdventureRoute.CAN_CONTINUE; break;
            default: return ThorActionIds.NONE;
        }
        final int action = control == CONTINUE ? ThorActionIds.MOVE_HERO : ThorActionIds.ADVENTURE_ROUTE_COMMAND;
        return route.fits(map) && route.allows(flag) && (mask & ThorActionIds.maskFor(action)) != 0
                ? action : ThorActionIds.NONE;
    }

    static int routeTarget(final int control)
    {
        switch (control)
        {
            case CONFIRM: return ThorAdventureRoute.CONFIRM;
            case CANCEL_PREVIEW: return ThorAdventureRoute.CANCEL_PREVIEW;
            case CLEAR_ROUTE: return ThorAdventureRoute.CLEAR_ROUTE;
            case STOP: return ThorAdventureRoute.STOP;
            default: return ThorActionIds.NO_TARGET;
        }
    }

    static final int ZOOM = 0, NAVIGATE = 3, PAN = 4, FILTER = 7, TOGGLE_MAP_FIRST = 9;
    static final float MAP_TOP = 0.46f, MAP_BOTTOM = 0.945f, MAP_BOTTOM_WITH_ACTIONS = 0.825f;
    static final float MAP_FIRST_DIVIDER = 0.125f;
    private static final float MAP_FIRST_OFFSET = 0.195f;

    static float mapTop(final boolean mapFirst)
    {
        return mapFirst ? MAP_TOP - MAP_FIRST_OFFSET : MAP_TOP;
    }

    static float[] bounds(final int column, final int columns, final boolean route, final boolean mapFirst,
            final float width, final float height)
    {
        final float top = route ? 0.835f : mapFirst ? 0.34f - MAP_FIRST_OFFSET : 0.34f;
        final float left = width * 0.035f, gap = width * 0.008f;
        final float cell = (width * 0.93f - gap * (columns - 1)) / columns;
        return new float[]{left + column * (cell + gap), height * top,
                left + column * (cell + gap) + cell, height * (top + 0.113f)};
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
