package eu.vcmi.vcmi;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Display;

final class ThorSecondScreenController implements DisplayManager.DisplayListener
{
    private static final String LOG_TAG = "vcmi-thor";

    private final VcmiSDLActivity activity;
    private final DisplayManager displayManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ThorHapticPreference.Store hapticPreferenceStore;
    private final ThorHapticState hapticState;

    private ThorSecondScreenPresentation presentation;
    private long presentationSessionId;
    private long contextRevision;
    private String contextId = ThorContextIds.UNKNOWN;
    private String contextTitle = "";
    private String contextStatus = "";
    private long heroPortraitAssetKey;
    private final String[] contextDetails = new String[ThorContextDetails.COUNT];
    private long enabledActionMask;
    private long activeActionMask;
    private ThorHeroRoster heroes = ThorHeroRoster.EMPTY;
    private ThorTownRoster towns = ThorTownRoster.EMPTY;
    private ThorBrowserState browser = ThorBrowserState.EMPTY;
    private ThorHeroMeetingArmies heroMeetingArmies = ThorHeroMeetingArmies.EMPTY;
    private ThorRecruitmentState recruitment = ThorRecruitmentState.EMPTY;
    private final ThorHeroMeetingArtifactCache heroMeetingArtifactCache = new ThorHeroMeetingArtifactCache();
    private final ThorVisualAssetCache<Bitmap> visualAssets = new ThorVisualAssetCache<>();
    private final ThorVisualAssetReferences visualAssetReferences = new ThorVisualAssetReferences();
    private int adventureTab;
    private boolean started;
    private boolean resumed;

    ThorSecondScreenController(final VcmiSDLActivity activity)
    {
        this.activity = activity;
        displayManager = (DisplayManager) activity.getSystemService(Context.DISPLAY_SERVICE);
        final SharedPreferences preferences = activity.getSharedPreferences(
                ThorHapticPreference.FILE_NAME, Context.MODE_PRIVATE);
        hapticPreferenceStore = new ThorHapticPreference.Store()
        {
            @Override
            public boolean getBoolean(final String key, final boolean defaultValue)
            {
                return preferences.getBoolean(key, defaultValue);
            }

            @Override
            public void putBoolean(final String key, final boolean value)
            {
                preferences.edit().putBoolean(key, value).apply();
            }
        };
        hapticState = new ThorHapticState(ThorHapticPreference.load(hapticPreferenceStore));
    }

    void start()
    {
        if (started || displayManager == null)
            return;

        displayManager.registerDisplayListener(this, mainHandler);
        started = true;
        Log.i(LOG_TAG, "Display diagnostics started");
        updatePresentation();
    }

    void resume()
    {
        resumed = true;
        Log.i(LOG_TAG, "Display diagnostics resumed");
        updatePresentation();
    }

    void pause()
    {
        resumed = false;
        NativeMethods.clearThorActions();
        hapticState.resetTransientState();
        Log.i(LOG_TAG, "Display diagnostics paused");
        dismissPresentation();
    }

    void stop()
    {
        resumed = false;
        if (started && displayManager != null)
            displayManager.unregisterDisplayListener(this);

        started = false;
        Log.i(LOG_TAG, "Display diagnostics stopped");
        dismissPresentation();
    }

    void destroy()
    {
        stop();
        mainHandler.removeCallbacksAndMessages(null);
    }

    void registerActionSubmission(final long revision, final int actionId)
    {
        hapticState.registerSubmission(revision, actionId);
    }

    void cancelActionSubmission(final long revision, final int actionId)
    {
        hapticState.cancelSubmission(revision, actionId);
    }

    long peekActionSubmissionToken(final long revision, final int actionId)
    {
        return hapticState.peekSubmissionToken(revision, actionId);
    }

    void acknowledgeAction(final long revision, final long submittedRevision, final int actionId,
                           final long callbackToken)
    {
        if (hapticState.accept(revision, submittedRevision, actionId, callbackToken,
                started && resumed && presentation != null && !activity.isFinishing() && !activity.isDestroyed()))
            presentation.performAcceptedHaptic();
    }

    void publishContext(final long revision, final String id, final String title, final String status,
                        final String detailLine1, final String detailLine2,
                        final String detailLine3, final String detailLine4,
                        final long publishedHeroPortraitAssetKey)
    {
        if (revision <= contextRevision)
            return;

        contextRevision = revision;
        contextId = id == null || id.isEmpty() ? ThorContextIds.UNKNOWN : id;
        hapticState.updateContext(revision, contextId);
        heroPortraitAssetKey = (ThorContextIds.ADVENTURE_MAP.equals(contextId)
                || ThorContextIds.HERO_WINDOW.equals(contextId))
                && ThorVisualAssetKey.isHeroPortrait(publishedHeroPortraitAssetKey)
                ? publishedHeroPortraitAssetKey : 0L;
        visualAssetReferences.updateContext(contextRevision, contextId, heroPortraitAssetKey);
        if (!ThorContextIds.ADVENTURE_MAP.equals(contextId))
            adventureTab = 0;
        heroes = ThorHeroRoster.EMPTY;
        towns = ThorTownRoster.EMPTY;
        browser = ThorBrowserState.EMPTY;
        recruitment = ThorRecruitmentState.EMPTY;
        heroMeetingArmies = ThorHeroMeetingArmies.EMPTY;
        heroMeetingArtifactCache.reset(revision, contextId);
        contextTitle = title == null ? "" : title;
        contextStatus = status == null ? "" : status;
        contextDetails[0] = ThorContextDetails.orEmpty(detailLine1);
        contextDetails[1] = ThorContextDetails.orEmpty(detailLine2);
        contextDetails[2] = ThorContextDetails.orEmpty(detailLine3);
        contextDetails[3] = ThorContextDetails.orEmpty(detailLine4);
        enabledActionMask = 0;
        activeActionMask = 0;
        Log.i(LOG_TAG, "Context " + contextId + " revision " + contextRevision);
        if (presentation != null)
            presentation.updateContext(contextRevision, contextId, contextTitle, contextStatus, heroPortraitAssetKey, contextDetails,
                    enabledActionMask, activeActionMask);
    }

    void publishActionState(final long revision, final long actionMask, final long activeMask)
    {
        if (revision != contextRevision)
            return;

        enabledActionMask = actionMask;
        activeActionMask = activeMask;
        if (presentation != null)
            presentation.updateContext(contextRevision, contextId, contextTitle, contextStatus, heroPortraitAssetKey,
                    contextDetails,
                    enabledActionMask, activeActionMask);
    }

    void publishBrowser(final long revision, final int page, final int pageCount,
                        final int[] targets, final String[] labels, final int[] flags)
    {
        if (revision != contextRevision)
            return;
        browser = ThorBrowserState.copyOf(contextId, page, pageCount, targets, labels, flags);
        if (presentation != null)
            presentation.updateBrowser(browser);
    }

    void publishRecruitment(final long revision, final int mode, final int selectedTarget,
            final int[] targets, final int[] creatureIds, final int[] available, final int[] selectedAmounts,
            final int[] maximum, final int[] variantIndexes, final int[] variantCounts, final int[] flags,
            final long[] visualKeys, final String[] names, final String[] unitCosts, final String[] selectedCosts,
            final String townName, final String totalCost)
    {
        if (revision != contextRevision || !(ThorContextIds.TOWN_RECRUITMENT_QUICK.equals(contextId)
                || ThorContextIds.TOWN_RECRUITMENT_DWELLING.equals(contextId)))
            return;
        final ThorRecruitmentState published = ThorRecruitmentState.copyOf(contextId, revision, mode, selectedTarget, targets, creatureIds,
                available, selectedAmounts, maximum, variantIndexes, variantCounts, flags, visualKeys,
                names, unitCosts, selectedCosts, townName, totalCost);
        recruitment = published.revision == revision ? published : ThorRecruitmentState.EMPTY;
        if (presentation != null)
            presentation.updateRecruitment(recruitment);
    }

    void publishHeroes(final long revision, final ThorHeroRoster roster)
    {
        if (revision != contextRevision || !ThorContextIds.ADVENTURE_MAP.equals(contextId))
            return;
        heroes = roster;
        if (presentation != null)
            presentation.updateHeroes(roster);
    }

    void publishTowns(final long revision, final ThorTownRoster roster)
    {
        if (revision != contextRevision || !ThorContextIds.ADVENTURE_MAP.equals(contextId))
            return;
        towns = roster;
        if (presentation != null)
            presentation.updateTowns(roster);
    }

    void publishHeroMeetingArmies(final long revision, final ThorHeroMeetingArmies armies)
    {
        if (revision != contextRevision || !ThorContextIds.HERO_MEETING.equals(contextId) || !armies.complete())
            return;
        heroMeetingArmies = armies;
        if (presentation != null)
            presentation.updateHeroMeetingArmies(armies);
    }

    void publishHeroMeetingArtifacts(final long revision, final ThorHeroMeetingArtifacts artifacts)
    {
        if (!heroMeetingArtifactCache.accept(revision, artifacts))
            return;
        if (presentation != null)
            presentation.updateHeroMeetingArtifacts(artifacts);
    }

    boolean hasVisualAsset(final long key)
    {
        return ThorVisualAssetKey.isValid(key) && visualAssets.knows(key);
    }

    void publishVisualAsset(final long revision, final long key, final int width, final int height,
                            final byte[] encoded)
    {
        if (!referencesVisualAsset(revision, key))
            return;
        visualAssets.accept(key, width, height, encoded, this::decodeVisualAsset);
        if (presentation != null)
            presentation.invalidateVisualAssets();
    }

    private boolean referencesVisualAsset(final long revision, final long key)
    {
        return visualAssetReferences.references(revision, key, heroMeetingArmies,
                heroMeetingArtifactCache.snapshot(), recruitment);
    }

    private Bitmap decodeVisualAsset(final int width, final int height, final byte[] encoded)
    {
        final BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(encoded, 0, encoded.length, bounds);
        if (bounds.outWidth != width || bounds.outHeight != height
                || bounds.outWidth <= 0 || bounds.outHeight <= 0
                || bounds.outWidth > ThorVisualAssetPayload.MAX_DIMENSION
                || bounds.outHeight > ThorVisualAssetPayload.MAX_DIMENSION)
            return null;
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inScaled = false;
        final Bitmap bitmap = BitmapFactory.decodeByteArray(encoded, 0, encoded.length, options);
        if (bitmap == null || bitmap.getWidth() != width || bitmap.getHeight() != height
                || bitmap.getAllocationByteCount() > ThorVisualAssetPayload.MAX_DIMENSION
                        * ThorVisualAssetPayload.MAX_DIMENSION * 4)
        {
            if (bitmap != null)
                bitmap.recycle();
            return null;
        }
        return bitmap;
    }

    @Override
    public void onDisplayAdded(final int displayId)
    {
        Log.i(LOG_TAG, "Display added: " + displayId);
        updatePresentation();
    }

    @Override
    public void onDisplayRemoved(final int displayId)
    {
        Log.i(LOG_TAG, "Display removed: " + displayId);
        updatePresentation();
    }

    @Override
    public void onDisplayChanged(final int displayId)
    {
        Log.i(LOG_TAG, "Display changed: " + displayId);
        updatePresentation();
    }

    private void updatePresentation()
    {
        if (!started || !resumed || displayManager == null || activity.isFinishing() || activity.isDestroyed())
            return;

        final Display targetDisplay = findSecondaryDisplay();
        if (targetDisplay == null)
        {
            Log.i(LOG_TAG, "No eligible presentation display");
            dismissPresentation();
            return;
        }

        if (presentation != null && presentation.getDisplay().getDisplayId() == targetDisplay.getDisplayId())
            return;

        dismissPresentation();

        hapticState.resetTransientState();

        final ThorSecondScreenPresentation newPresentation = new ThorSecondScreenPresentation(activity, targetDisplay,
                visualAssets, hapticState.isEnabled(), this::setHapticsEnabled,
                ++presentationSessionId, this::isCurrentPresentationSession);
        newPresentation.setOnDismissListener(dialog ->
        {
            if (presentation == dialog)
            {
                presentation = null;
                ++presentationSessionId;
            }
            newPresentation.clearTransientState();
        });

        try
        {
            newPresentation.show();
            presentation = newPresentation;
            newPresentation.setAdventureTab(adventureTab);
            newPresentation.updateContext(contextRevision, contextId, contextTitle, contextStatus, heroPortraitAssetKey,
                    contextDetails,
                    enabledActionMask, activeActionMask);
            newPresentation.updateHeroes(heroes);
            newPresentation.updateTowns(towns);
            newPresentation.updateBrowser(browser);
            newPresentation.updateRecruitment(recruitment);
            newPresentation.updateHeroMeetingArmies(heroMeetingArmies);
            newPresentation.updateHeroMeetingArtifacts(heroMeetingArtifactCache.snapshot());
            Log.i(LOG_TAG, "Companion presentation opened on display " + targetDisplay.getDisplayId());
        }
        catch (final RuntimeException exception)
        {
            Log.e(LOG_TAG, "Unable to open companion presentation on display " + targetDisplay.getDisplayId(), exception);
            newPresentation.dismiss();
        }
    }

    private Display findSecondaryDisplay()
    {
        final Display activityDisplay = activity.getDisplay();
        final int activityDisplayId = activityDisplay == null ? Display.DEFAULT_DISPLAY : activityDisplay.getDisplayId();
        final Display[] preferredDisplays = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);
        final Display[] allDisplays = displayManager.getDisplays();
        final int selectedId = ThorDisplaySelector.select(activityDisplayId, toCandidates(preferredDisplays), toCandidates(allDisplays));
        return selectedId == ThorDisplaySelector.NO_DISPLAY ? null : displayManager.getDisplay(selectedId);
    }

    private static ThorDisplaySelector.Candidate[] toCandidates(final Display[] displays)
    {
        final ThorDisplaySelector.Candidate[] candidates = new ThorDisplaySelector.Candidate[displays.length];
        for (int index = 0; index < displays.length; ++index)
        {
            final Display display = displays[index];
            candidates[index] = new ThorDisplaySelector.Candidate(display.getDisplayId(),
                    display.isValid() && display.getState() == Display.STATE_ON);
        }
        return candidates;
    }

    private void dismissPresentation()
    {
        if (presentation == null)
            return;

        final ThorSecondScreenPresentation oldPresentation = presentation;
        adventureTab = ThorContextIds.ADVENTURE_MAP.equals(contextId) ? oldPresentation.getAdventureTab() : 0;
        presentation = null;
        ++presentationSessionId;
        hapticState.resetTransientState();
        NativeMethods.clearThorActions();
        oldPresentation.clearTransientState();
        try
        {
            oldPresentation.dismiss();
        }
        catch (final RuntimeException exception)
        {
            Log.w(LOG_TAG, "Unable to dismiss companion presentation cleanly", exception);
        }
    }

    private boolean isCurrentPresentationSession(final long sessionId)
    {
        return presentation != null && presentationSessionId == sessionId;
    }

    private void setHapticsEnabled(final boolean enabled)
    {
        if (hapticState.isEnabled() == enabled)
            return;

        ThorHapticPreference.save(hapticPreferenceStore, enabled);
        hapticState.setEnabled(enabled);
        if (presentation != null)
            presentation.setHapticsEnabled(enabled, enabled);
    }

}
