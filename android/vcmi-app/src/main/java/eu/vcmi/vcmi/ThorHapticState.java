package eu.vcmi.vcmi;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

/** Small policy boundary for accepted lower-deck feedback. */
final class ThorHapticState
{
    private boolean enabled;
    private long revision;
    private String contextId = ThorContextIds.UNKNOWN;
    private long callbackToken = 1;
    private final Set<AcceptedAction> acceptedActions = new HashSet<>();
    private final LinkedHashMap<AcceptedAction, Long> submittedActionTokens = new LinkedHashMap<>();

    ThorHapticState(final boolean enabled)
    {
        this.enabled = enabled;
    }

    boolean isEnabled()
    {
        return enabled;
    }

    void setEnabled(final boolean value)
    {
        if (enabled == value)
            return;
        enabled = value;
        resetTransientState();
    }

    long callbackToken()
    {
        return callbackToken;
    }

    void updateContext(final long nextRevision, final String nextContextId)
    {
        if (nextRevision <= revision)
            return;
        final String normalized = nextContextId == null ? ThorContextIds.UNKNOWN : nextContextId;
        if (!normalized.equals(contextId))
        {
            ++callbackToken;
            acceptedActions.clear();
            submittedActionTokens.clear();
        }
        revision = nextRevision;
        contextId = normalized;
    }

    void resetTransientState()
    {
        ++callbackToken;
        acceptedActions.clear();
        submittedActionTokens.clear();
    }

    void registerSubmission(final long submittedRevision, final int actionId)
    {
        if (!enabled || submittedRevision <= 0 || submittedRevision != revision
                || !isEligible(contextId, actionId))
            return;

        final AcceptedAction action = new AcceptedAction(submittedRevision, actionId);
        submittedActionTokens.put(action, callbackToken);
        while (submittedActionTokens.size() > 32)
            submittedActionTokens.remove(submittedActionTokens.keySet().iterator().next());
    }

    long peekSubmissionToken(final long submittedRevision, final int actionId)
    {
        final Long token = submittedActionTokens.get(new AcceptedAction(submittedRevision, actionId));
        return token == null ? 0 : token;
    }

    void cancelSubmission(final long submittedRevision, final int actionId)
    {
        submittedActionTokens.remove(new AcceptedAction(submittedRevision, actionId));
    }

    boolean accept(final long deliveryRevision, final long submittedRevision, final int actionId,
                   final long capturedToken, final boolean presentationReady)
    {
        final AcceptedAction submittedAction = new AcceptedAction(submittedRevision, actionId);
        final Long registeredToken = submittedActionTokens.remove(submittedAction);
        if (!enabled || !presentationReady || capturedToken <= 0 || capturedToken != callbackToken
                || registeredToken == null || registeredToken != capturedToken
                || submittedRevision <= 0 || deliveryRevision <= 0 || deliveryRevision > revision
                || !isEligible(contextId, actionId)
                || acceptedActions.contains(submittedAction))
            return false;

        if (acceptedActions.size() >= 32)
            acceptedActions.remove(acceptedActions.iterator().next());
        acceptedActions.add(submittedAction);
        return true;
    }

    private static final class AcceptedAction
    {
        private final long revision;
        private final int actionId;

        private AcceptedAction(final long revision, final int actionId)
        {
            this.revision = revision;
            this.actionId = actionId;
        }

        @Override
        public boolean equals(final Object other)
        {
            if (!(other instanceof AcceptedAction))
                return false;
            final AcceptedAction that = (AcceptedAction) other;
            return revision == that.revision && actionId == that.actionId;
        }

        @Override
        public int hashCode()
        {
            return (int) (revision ^ (revision >>> 32)) * 31 + actionId;
        }
    }

    static boolean isEligible(final String contextId, final int actionId)
    {
        if (actionId >= ThorActionIds.MOVE_HERO && actionId <= ThorActionIds.END_TURN)
            return ThorContextIds.ADVENTURE_MAP.equals(contextId);
        if (actionId >= ThorActionIds.BATTLE_WAIT && actionId <= ThorActionIds.BATTLE_DEFEND)
            return ThorContextIds.BATTLE.equals(contextId);
        if (actionId >= ThorActionIds.BATTLE_TACTICS_NEXT && actionId <= ThorActionIds.BATTLE_TACTICS_END)
            return ThorContextIds.BATTLE_TACTICS.equals(contextId);
        return actionId >= ThorActionIds.HERO_MEETING_MOVE_STACK
                && actionId <= ThorActionIds.HERO_MEETING_SWAP_ARTIFACTS
                && ThorContextIds.HERO_MEETING.equals(contextId);
    }
}
