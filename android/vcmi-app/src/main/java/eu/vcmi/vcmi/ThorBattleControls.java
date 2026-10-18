package eu.vcmi.vcmi;

/** Control IDs are view-local; legacy and extended native namespaces stay distinct. */
final class ThorBattleControls
{
    static boolean isBattle(final String context)
    {
        return ThorContextIds.BATTLE.equals(context) || ThorContextIds.BATTLE_TACTICS.equals(context);
    }
    static int count(final String context) { return ThorContextIds.BATTLE_TACTICS.equals(context) ? 2 : 6; }
    static int legacy(final String context, final int control)
    {
        if (control < 1 || control > 2) return ThorActionIds.NONE;
        return ThorContextIds.BATTLE_TACTICS.equals(context)
                ? (control == 1 ? ThorActionIds.BATTLE_TACTICS_NEXT : ThorActionIds.BATTLE_TACTICS_END)
                : (control == 1 ? ThorActionIds.BATTLE_WAIT : ThorActionIds.BATTLE_DEFEND);
    }
    static int command(final int control, final ThorExtendedCommands state)
    {
        switch (control)
        {
            case 3: return state.active(ThorExtendedCommands.AUTO_START)
                    ? ThorExtendedCommands.AUTO_STOP : ThorExtendedCommands.AUTO_START;
            case 4: return ThorExtendedCommands.CAST_SPELL;
            case 5: return ThorExtendedCommands.RETREAT;
            case 6: return ThorExtendedCommands.SURRENDER;
            default: return 0;
        }
    }
    static boolean enabled(final String context, final int control, final long mask,
                           final ThorExtendedCommands state)
    {
        if (!isBattle(context) || control < 1 || control > count(context)) return false;
        final int action = legacy(context, control);
        return action != ThorActionIds.NONE ? (mask & ThorActionIds.maskFor(action)) != 0
                : state.allows(command(control, state));
    }
    static float[] bounds(final int control, final String context, final ThorPresentationLayout layout)
    {
        if (control < 1 || control > count(context) || !layout.hasContent()) return null;
        final boolean tactics = ThorContextIds.BATTLE_TACTICS.equals(context);
        final float gap = Math.max(layout.bevel * 1.5f, 12f);
        final float left = layout.margin + layout.bevel * 3f;
        final float right = layout.right - layout.bevel * 3f;
        final float divider = layout.margin + layout.contentHeight * 0.55f;
        final float top = divider + (layout.bottom - divider) * 0.13f + layout.bevel;
        final float bottom = layout.bottom - layout.bevel * 3f;
        final int columns = tactics ? 1 : 2, rows = tactics ? 2 : 3;
        final float width = (right - left - gap * (columns - 1)) / columns;
        final float height = (bottom - top - gap * (rows - 1)) / rows;
        final int index = control - 1;
        final float x = left + (index % columns) * (width + gap);
        final float y = top + (index / columns) * (height + gap);
        return new float[]{x, y, x + width, y + height};
    }
    static int controlAt(final float x, final float y, final String context, final ThorPresentationLayout layout)
    {
        for (int control = 1; control <= count(context); ++control)
        {
            final float[] b = bounds(control, context, layout);
            if (b != null && x >= b[0] && x < b[2] && y >= b[1] && y < b[3]) return control;
        }
        return 0;
    }
}
