package eu.vcmi.vcmi;

/** Stable action mapping for the bounded campaign scenario and starting-bonus dashboard. */
final class ThorCampaignState
{
    private ThorCampaignState()
    {
    }

    static int actionForControl(final int control)
    {
        if (control == ThorLobbyScenarioState.CONTROL_PREVIOUS_SCENARIO)
            return ThorActionIds.CAMPAIGN_PREVIOUS_SCENARIO;
        if (control == ThorLobbyScenarioState.CONTROL_NEXT_SCENARIO)
            return ThorActionIds.CAMPAIGN_NEXT_SCENARIO;
        if (control >= ThorLobbyScenarioState.CONTROL_DIFFICULTY_FIRST
                && control < ThorLobbyScenarioState.CONTROL_DIFFICULTY_FIRST + 3)
            return ThorActionIds.CAMPAIGN_SELECT_BONUS_1
                    + control - ThorLobbyScenarioState.CONTROL_DIFFICULTY_FIRST;
        if (control == ThorLobbyScenarioState.CONTROL_START)
            return ThorActionIds.CAMPAIGN_START;
        if (control == ThorLobbyScenarioState.CONTROL_BACK)
            return ThorActionIds.CAMPAIGN_BACK;
        return ThorActionIds.NONE;
    }

    static int bonusIndexForControl(final int control)
    {
        return control >= ThorLobbyScenarioState.CONTROL_DIFFICULTY_FIRST
                && control < ThorLobbyScenarioState.CONTROL_DIFFICULTY_FIRST + 3
                ? control - ThorLobbyScenarioState.CONTROL_DIFFICULTY_FIRST : -1;
    }
}
