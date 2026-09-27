package eu.vcmi.vcmi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ThorHapticStateTest
{
    @Test
    public void onlyEnabledSemanticAcceptedActionsCanTickOnce()
    {
        final ThorHapticState state = stateAt(10, ThorContextIds.ADVENTURE_MAP);
        final long token = state.callbackToken();
        assertFalse(state.accept(10, ThorActionIds.NEXT_HERO, token, true));
        assertTrue(state.accept(10, ThorActionIds.END_TURN, token, true));
        assertFalse(state.accept(10, ThorActionIds.END_TURN, token, true));
        state.updateContext(11, ThorContextIds.ADVENTURE_MAP);
        assertTrue(state.accept(11, ThorActionIds.MOVE_HERO, token, true));
        assertFalse(state.accept(10, ThorActionIds.END_TURN, token, true));
        assertFalse(state.accept(10, ThorActionIds.SELECT_HERO, token, true));
        assertFalse(state.accept(10, ThorActionIds.SELECT_TOWN, token, true));
        assertFalse(state.accept(10, ThorActionIds.OPEN_QUEST_LOG, token, true));
        assertFalse(state.accept(10, ThorActionIds.NONE, token, true));

        state.setEnabled(false);
        assertFalse(state.accept(10, ThorActionIds.MOVE_HERO, token, true));
    }

    @Test
    public void adventureBattleAndHeroMeetingSemanticActionsAreEligible()
    {
        assertTrue(ThorHapticState.isEligible(ThorContextIds.ADVENTURE_MAP, ThorActionIds.END_TURN));
        assertFalse(ThorHapticState.isEligible(ThorContextIds.ADVENTURE_MAP, ThorActionIds.NEXT_HERO));
        assertTrue(ThorHapticState.isEligible(ThorContextIds.BATTLE, ThorActionIds.BATTLE_WAIT));
        assertTrue(ThorHapticState.isEligible(ThorContextIds.BATTLE_TACTICS, ThorActionIds.BATTLE_TACTICS_END));
        assertTrue(ThorHapticState.isEligible(ThorContextIds.HERO_MEETING,
                ThorActionIds.HERO_MEETING_REDISTRIBUTE_STACK));
        assertTrue(ThorHapticState.isEligible(ThorContextIds.HERO_MEETING,
                ThorActionIds.HERO_MEETING_TRANSFER_ARTIFACT));
        assertFalse(ThorHapticState.isEligible(ThorContextIds.BATTLE, ThorActionIds.NEXT_HERO));
        assertFalse(ThorHapticState.isEligible(ThorContextIds.HERO_MEETING, ThorActionIds.SELECT_HERO));
    }

    @Test
    public void staleContextRevisionAndPresentationCallbacksAreRejected()
    {
        final ThorHapticState state = stateAt(20, ThorContextIds.HERO_MEETING);
        final long originalToken = state.callbackToken();
        state.updateContext(21, ThorContextIds.BATTLE);
        assertFalse(state.accept(20, ThorActionIds.HERO_MEETING_MOVE_STACK, originalToken, true));
        assertFalse(state.accept(18, ThorActionIds.BATTLE_WAIT, state.callbackToken(), true));
        assertFalse(state.accept(21, ThorActionIds.BATTLE_WAIT, state.callbackToken(), false));

        state.resetTransientState();
        assertFalse(state.accept(21, ThorActionIds.BATTLE_WAIT, originalToken, true));
    }

    @Test
    public void acceptedActionCanArriveAfterItsSameContextRevisionRefresh()
    {
        final ThorHapticState state = stateAt(30, ThorContextIds.BATTLE);
        final long token = state.callbackToken();
        state.updateContext(31, ThorContextIds.BATTLE);
        assertTrue(state.accept(30, ThorActionIds.BATTLE_DEFEND, token, true));
    }

    @Test
    public void preferenceTransitionsInvalidateAlreadyQueuedAcknowledgements()
    {
        final ThorHapticState state = stateAt(40, ThorContextIds.ADVENTURE_MAP);
        final long beforeToggle = state.callbackToken();
        state.setEnabled(false);
        state.setEnabled(true);
        assertFalse(state.accept(40, ThorActionIds.END_TURN, beforeToggle, true));
        assertTrue(state.accept(40, ThorActionIds.END_TURN, state.callbackToken(), true));
    }

    @Test
    public void hapticPreferenceDefaultsOnAndSurvivesHelperReconstruction()
    {
        final Map<String, Boolean> values = new HashMap<>();
        final ThorHapticPreference.Store store = new ThorHapticPreference.Store()
        {
            @Override
            public boolean getBoolean(final String key, final boolean defaultValue)
            {
                return values.getOrDefault(key, defaultValue);
            }

            @Override
            public void putBoolean(final String key, final boolean value)
            {
                values.put(key, value);
            }
        };

        assertTrue(ThorHapticPreference.load(store));
        ThorHapticPreference.save(store, false);
        assertFalse(ThorHapticPreference.load(store));
        ThorHapticPreference.save(store, true);
        assertTrue(ThorHapticPreference.load(store));
        assertEquals(1, values.size());
    }

    private static ThorHapticState stateAt(final long revision, final String context)
    {
        final ThorHapticState state = new ThorHapticState(true);
        state.updateContext(revision, context);
        return state;
    }
}
