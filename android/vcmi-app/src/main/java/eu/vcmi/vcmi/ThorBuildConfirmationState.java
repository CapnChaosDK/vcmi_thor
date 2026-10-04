package eu.vcmi.vcmi;

/** Fixed lower-deck controls for the ordinary Town Hall build confirmation. */
final class ThorBuildConfirmationState
{
    static final int CONTROL_NONE = 0;
    static final int CONTROL_BUY = 1;
    static final int CONTROL_CANCEL = 2;

    private ThorBuildConfirmationState()
    {
    }

    static int actionForControl(final int control)
    {
        if (control == CONTROL_BUY)
            return ThorActionIds.WINDOW_CONFIRM;
        if (control == CONTROL_CANCEL)
            return ThorActionIds.WINDOW_CLOSE;
        return ThorActionIds.NONE;
    }

    static float[] boundsForControl(final int control, final float width, final float height,
                                    final float dividerY)
    {
        if ((control != CONTROL_BUY && control != CONTROL_CANCEL) || width <= 0f
                || height <= dividerY || dividerY < 0f)
            return new float[]{0f, 0f, 0f, 0f};
        final float gap = width * 0.06f;
        final float buttonWidth = (width - gap - width * 0.12f) * 0.5f;
        final float left = width * 0.06f + (control - CONTROL_BUY) * (buttonWidth + gap);
        final float top = dividerY + (height - dividerY) * 0.60f;
        final float buttonHeight = (height - dividerY) * 0.28f;
        return new float[]{left, top, left + buttonWidth, top + buttonHeight};
    }

    static int controlAt(final float x, final float y, final float width, final float height,
                         final float dividerY)
    {
        for (int control = CONTROL_BUY; control <= CONTROL_CANCEL; ++control)
        {
            final float[] bounds = boundsForControl(control, width, height, dividerY);
            if (x >= bounds[0] && x <= bounds[2] && y >= bounds[1] && y <= bounds[3])
                return control;
        }
        return CONTROL_NONE;
    }
}
