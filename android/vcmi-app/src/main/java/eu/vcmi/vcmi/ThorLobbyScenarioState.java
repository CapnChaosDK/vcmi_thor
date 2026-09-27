package eu.vcmi.vcmi;

/** Bounded state and hit regions for the New Game scenario companion panel. */
final class ThorLobbyScenarioState
{
    static final int DIFFICULTY_COUNT = 5;
    static final int CONTROL_NONE = 0;
    static final int CONTROL_DIFFICULTY_FIRST = 1;
    static final int CONTROL_DIFFICULTY_LAST = CONTROL_DIFFICULTY_FIRST + DIFFICULTY_COUNT - 1;
    static final int CONTROL_START = 6;
    static final int CONTROL_BACK = 7;

    static boolean canCompleteTap(final int capturedControl, final int releasedControl,
                                  final long capturedRevision, final long currentRevision,
                                  final long capturedSession, final long currentSession,
                                  final boolean actionEnabledAtDown, final boolean actionEnabledAtUp)
    {
        return capturedControl != CONTROL_NONE && capturedControl == releasedControl
                && capturedRevision == currentRevision && capturedSession > 0L
                && capturedSession == currentSession && actionEnabledAtDown && actionEnabledAtUp;
    }

    private ThorLobbyScenarioState()
    {
    }

    static int difficultyIndex(final String[] details)
    {
        if (details == null || details.length <= 2 || details[2] == null)
            return -1;
        try
        {
            final int value = Integer.parseInt(details[2]);
            return value >= 0 && value < DIFFICULTY_COUNT ? value : -1;
        }
        catch (final NumberFormatException ignored)
        {
            return -1;
        }
    }

    static int difficultyTargetForIndex(final int index)
    {
        return index >= 0 && index < DIFFICULTY_COUNT ? index : ThorActionIds.NO_TARGET;
    }

    static boolean shouldDispatchDifficulty(final int currentDifficulty, final int targetDifficulty)
    {
        return difficultyTargetForIndex(targetDifficulty) != ThorActionIds.NO_TARGET
                && targetDifficulty != currentDifficulty;
    }

    static boolean isActionEnabled(final long mask, final int actionId)
    {
        return (mask & ThorActionIds.maskFor(actionId)) != 0L;
    }

    static float[] boundsForControl(final int control, final float width, final float height,
                                    final float dividerY)
    {
        if (width <= 0f || height <= dividerY || dividerY < 0f)
            return new float[]{0f, 0f, 0f, 0f};

        final float lowerHeight = height - dividerY;
        final float side = width * 0.055f;
        final float gap = width * 0.012f;
        if (control >= CONTROL_DIFFICULTY_FIRST && control <= CONTROL_DIFFICULTY_LAST)
        {
            final float tileWidth = (width - side * 2f - gap * (DIFFICULTY_COUNT - 1)) / DIFFICULTY_COUNT;
            final int index = control - CONTROL_DIFFICULTY_FIRST;
            final float left = side + index * (tileWidth + gap);
            return new float[]{left, dividerY + lowerHeight * 0.20f,
                    left + tileWidth, dividerY + lowerHeight * 0.47f};
        }

        if (control == CONTROL_START || control == CONTROL_BACK)
        {
            final float actionGap = width * 0.025f;
            final float actionWidth = (width - side * 2f - actionGap) / 2f;
            final float left = control == CONTROL_START ? side : side + actionWidth + actionGap;
            return new float[]{left, dividerY + lowerHeight * 0.61f,
                    left + actionWidth, dividerY + lowerHeight * 0.94f};
        }
        return new float[]{0f, 0f, 0f, 0f};
    }

    static int controlAt(final float x, final float y, final float width, final float height,
                         final float dividerY)
    {
        for (int control = CONTROL_DIFFICULTY_FIRST; control <= CONTROL_BACK; ++control)
        {
            final float[] bounds = boundsForControl(control, width, height, dividerY);
            if (x >= bounds[0] && y >= bounds[1] && x <= bounds[2] && y <= bounds[3])
                return control;
        }
        return CONTROL_NONE;
    }

    static int actionForControl(final int control)
    {
        if (control >= CONTROL_DIFFICULTY_FIRST && control <= CONTROL_DIFFICULTY_LAST)
            return ThorActionIds.LOBBY_SET_DIFFICULTY;
        if (control == CONTROL_START)
            return ThorActionIds.LOBBY_START_GAME;
        if (control == CONTROL_BACK)
            return ThorActionIds.LOBBY_BACK;
        return ThorActionIds.NONE;
    }

    static int difficultyTargetForControl(final int control)
    {
        return control >= CONTROL_DIFFICULTY_FIRST && control <= CONTROL_DIFFICULTY_LAST
                ? difficultyTargetForIndex(control - CONTROL_DIFFICULTY_FIRST) : ThorActionIds.NO_TARGET;
    }

    static String scenarioName(final String value, final String fallback)
    {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
