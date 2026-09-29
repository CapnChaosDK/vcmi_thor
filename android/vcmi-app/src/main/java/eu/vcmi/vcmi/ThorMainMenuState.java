package eu.vcmi.vcmi;

/** Context-qualified choice positions and bounded lower-deck menu hit regions. */
final class ThorMainMenuState
{
    static final int CONTROL_NONE = 0;
    static final int MAX_CHOICES = 5;

    private ThorMainMenuState()
    {
    }

    static int choiceCount(final String contextId)
    {
        if (ThorContextIds.MAIN_MENU_CREDITS.equals(contextId))
            return 1;
        return ThorContextIds.MAIN_MENU.equals(contextId)
                || ThorContextIds.MAIN_MENU_NEW_GAME.equals(contextId)
                || ThorContextIds.MAIN_MENU_LOAD_GAME.equals(contextId)
                || ThorContextIds.MAIN_MENU_CAMPAIGN.equals(contextId) ? MAX_CHOICES : 0;
    }

    static int actionForControl(final String contextId, final int control)
    {
        return control > CONTROL_NONE && control <= choiceCount(contextId)
                ? ThorActionIds.MAIN_MENU_CHOICE_1 + control - 1 : ThorActionIds.NONE;
    }

    static int labelForControl(final String contextId, final int control)
    {
        if (actionForControl(contextId, control) == ThorActionIds.NONE)
            return 0;
        if (ThorContextIds.MAIN_MENU.equals(contextId))
        {
            switch (control)
            {
                case 1: return R.string.thor_context_new_game;
                case 2: return R.string.thor_context_load_game;
                case 3: return R.string.thor_menu_high_scores;
                case 4: return R.string.thor_context_credits;
                default: return R.string.thor_menu_quit;
            }
        }
        if (ThorContextIds.MAIN_MENU_NEW_GAME.equals(contextId)
                || ThorContextIds.MAIN_MENU_LOAD_GAME.equals(contextId))
        {
            switch (control)
            {
                case 1: return R.string.thor_menu_single_player;
                case 2: return R.string.thor_menu_multiplayer;
                case 3: return R.string.thor_context_campaign;
                case 4: return R.string.thor_menu_tutorial;
                default: return R.string.thor_lobby_back;
            }
        }
        if (ThorContextIds.MAIN_MENU_CAMPAIGN.equals(contextId))
        {
            switch (control)
            {
                case 1: return R.string.thor_menu_shadow_of_death;
                case 2: return R.string.thor_menu_restoration_of_erathia;
                case 3: return R.string.thor_menu_armageddons_blade;
                case 4: return R.string.thor_menu_custom_campaign;
                default: return R.string.thor_lobby_back;
            }
        }
        return R.string.thor_lobby_back;
    }

    static float[] boundsForControl(final String contextId, final int control,
                                    final float width, final float height, final float dividerY)
    {
        final int count = choiceCount(contextId);
        if (control <= CONTROL_NONE || control > count || width <= 0f || height <= dividerY || dividerY < 0f)
            return new float[]{0f, 0f, 0f, 0f};
        final float lowerHeight = height - dividerY;
        final float left = width * 0.09f;
        final float right = width - left;
        if (count == 1)
            return new float[]{left, dividerY + lowerHeight * 0.38f,
                    right, dividerY + lowerHeight * 0.66f};
        final float top = dividerY + lowerHeight * 0.11f;
        final float gap = lowerHeight * 0.025f;
        final float rowHeight = (lowerHeight * 0.78f - gap * (count - 1)) / count;
        final float rowTop = top + (control - 1) * (rowHeight + gap);
        return new float[]{left, rowTop, right, rowTop + rowHeight};
    }

    static int controlAt(final String contextId, final float x, final float y,
                         final float width, final float height, final float dividerY)
    {
        for (int control = 1; control <= choiceCount(contextId); ++control)
        {
            final float[] bounds = boundsForControl(contextId, control, width, height, dividerY);
            if (x > bounds[0] && x < bounds[2] && y > bounds[1] && y < bounds[3])
                return control;
        }
        return CONTROL_NONE;
    }
}
