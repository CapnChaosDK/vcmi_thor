package eu.vcmi.vcmi;

import java.util.HashSet;
import java.util.Set;

/** Small policy boundary for accepted lower-deck feedback. */
final class ThorHapticState
{
    private static final long MAX_ACK_REVISION_AGE = 1;

    private boolean enabled;
    private long revision;
    private String contextId = ThorContextIds.UNKNOWN;
    private long callbackToken = 1;
    private final Set<AcceptedAction> acceptedActions = new HashSet<>();

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
        }
        revision = nextRevision;
        contextId = normalized;
    }

    void resetTransientState()
    {
        ++callbackToken;
        acceptedActions.clear();
    }

    boolean accept(final long acceptedRevision, final int actionId, final long capturedToken,
                   final boolean presentationReady)
    {
        if (!enabled || !presentationReady || capturedToken != callbackToken
                || acceptedRevision <= 0 || acceptedRevision > revision
                || revision - acceptedRevision > MAX_ACK_REVISION_AGE
                || !isEligible(contextId, actionId)
                || acceptedActions.contains(new AcceptedAction(acceptedRevision, actionId)))
            return false;

        if (acceptedActions.size() >= 32)
            acceptedActions.remove(acceptedActions.iterator().next());
        acceptedActions.add(new AcceptedAction(acceptedRevision, actionId));
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
