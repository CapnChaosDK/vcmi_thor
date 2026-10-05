package eu.vcmi.vcmi;

import android.content.Context;
import android.os.Build;
import android.os.Messenger;
import android.os.VibrationEffect;
import android.os.Vibrator;

import org.libsdl.app.SDL;
import org.libsdl.app.SDLActivity;

import java.io.File;
import java.lang.ref.WeakReference;

import eu.vcmi.vcmi.util.Log;
import eu.vcmi.vcmi.util.Notifications;

/**
 * @author F
 */
public class NativeMethods
{
    private static WeakReference<Messenger> serverMessengerRef;

    // The server runs in a process without an activity, and SDL3 only holds one of those
    private static Context serviceContext;

    public NativeMethods()
    {
    }

    public static void setServiceContext(final Context ctx)
    {
        serviceContext = ctx;
    }

    private static Context context()
    {
        final Context ctx = SDL.getContext();
        return ctx != null ? ctx : serviceContext;
    }

    public static native void initClassloader();
    public static native void heroesDataUpdate();

    public static void submitThorAction(final long revision, final int actionId, final int targetId)
    {
        if (BuildConfig.AYN_THOR_BUILD)
        {
            final Context ctx = context();
            if (ctx instanceof VcmiSDLActivity)
                ((VcmiSDLActivity) ctx).registerThorActionSubmission(revision, actionId);
        }
        submitThorActionNative(revision, actionId, targetId);
    }

    private static native void submitThorActionNative(long revision, int actionId, int targetId);

    static boolean submitThorRecruitmentEdit(final long revision, final int targetId, final int operationId)
    {
        return BuildConfig.AYN_THOR_BUILD && submitThorRecruitmentEditNative(revision, targetId, operationId);
    }

    private static native boolean submitThorRecruitmentEditNative(long revision, int targetId, int operationId);
    public static boolean submitThorHeroMeetingSplit(final long revision, final int sourceArmyId,
            final int sourceSlot, final int destinationArmyId, final int destinationSlot, final int amount)
    {
        final Context ctx = context();
        final VcmiSDLActivity activity = ctx instanceof VcmiSDLActivity ? (VcmiSDLActivity) ctx : null;
        if (BuildConfig.AYN_THOR_BUILD && activity != null)
            activity.registerThorActionSubmission(revision, ThorActionIds.HERO_MEETING_SPLIT_STACK);
        final boolean submitted = submitThorHeroMeetingSplitNative(revision, sourceArmyId, sourceSlot,
                destinationArmyId, destinationSlot, amount);
        if (!submitted && BuildConfig.AYN_THOR_BUILD && activity != null)
            activity.cancelThorActionSubmission(revision, ThorActionIds.HERO_MEETING_SPLIT_STACK);
        return submitted;
    }

    private static native boolean submitThorHeroMeetingSplitNative(long revision, int sourceArmyId, int sourceSlot,
            int destinationArmyId, int destinationSlot, int amount);
    public static boolean submitThorHeroMeetingRedistribution(final long revision, final int leftHeroId,
            final int rightHeroId, final int sourceArmyId, final int sourceSlot, final int sourceCreatureId,
            final int sourceCount, final int[] destinationArmyIds, final int[] destinationSlots, final int[] amounts)
    {
        final Context ctx = context();
        final VcmiSDLActivity activity = ctx instanceof VcmiSDLActivity ? (VcmiSDLActivity) ctx : null;
        if (BuildConfig.AYN_THOR_BUILD && activity != null)
            activity.registerThorActionSubmission(revision, ThorActionIds.HERO_MEETING_REDISTRIBUTE_STACK);
        final boolean submitted = submitThorHeroMeetingRedistributionNative(revision, leftHeroId, rightHeroId,
                sourceArmyId, sourceSlot, sourceCreatureId, sourceCount, destinationArmyIds, destinationSlots, amounts);
        if (!submitted && BuildConfig.AYN_THOR_BUILD && activity != null)
            activity.cancelThorActionSubmission(revision, ThorActionIds.HERO_MEETING_REDISTRIBUTE_STACK);
        return submitted;
    }

    private static native boolean submitThorHeroMeetingRedistributionNative(long revision, int leftHeroId,
            int rightHeroId, int sourceArmyId, int sourceSlot, int sourceCreatureId, int sourceCount,
            int[] destinationArmyIds, int[] destinationSlots, int[] amounts);
    public static native void clearThorActions();

    @SuppressWarnings(Const.JNI_METHOD_SUPPRESS)
    public static void acknowledgeThorAction(final long revision, final long submittedRevision, final int actionId)
    {
        if (!BuildConfig.AYN_THOR_BUILD)
            return;

        final Context ctx = context();
        if (!(ctx instanceof VcmiSDLActivity))
            return;

        final VcmiSDLActivity activity = (VcmiSDLActivity) ctx;
        activity.runOnUiThread(() ->
        {
            final long callbackToken = activity.peekThorActionSubmissionToken(submittedRevision, actionId);
            if (callbackToken > 0)
                activity.acknowledgeThorAction(revision, submittedRevision, actionId, callbackToken);
        });
    }

    @SuppressWarnings(Const.JNI_METHOD_SUPPRESS)
    public static boolean hasThorVisualAsset(final long key)
    {
        if (!BuildConfig.AYN_THOR_BUILD || !ThorVisualAssetKey.isValid(key))
            return false;
        final Context ctx = context();
        return ctx instanceof VcmiSDLActivity && ((VcmiSDLActivity) ctx).hasThorVisualAsset(key);
    }

    @SuppressWarnings(Const.JNI_METHOD_SUPPRESS)
    public static void publishThorContext(final long revision, final String contextId,
                                          final String title, final String status,
                                          final String detailLine1, final String detailLine2,
                                          final String detailLine3, final String detailLine4,
                                          final long heroPortraitAssetKey)
    {
        if (!BuildConfig.AYN_THOR_BUILD)
            return;

        final Context ctx = context();
        if (!(ctx instanceof VcmiSDLActivity))
            return;

        ((VcmiSDLActivity) ctx).runOnUiThread(() ->
                ((VcmiSDLActivity) ctx).publishThorContext(revision, contextId, title, status,
                        detailLine1, detailLine2, detailLine3, detailLine4, heroPortraitAssetKey));
    }

    @SuppressWarnings(Const.JNI_METHOD_SUPPRESS)
    public static void publishThorActionState(final long revision, final long enabledActionMask,
                                              final long activeActionMask)
    {
        if (!BuildConfig.AYN_THOR_BUILD)
            return;

        final Context ctx = context();
        if (!(ctx instanceof VcmiSDLActivity))
            return;

        ((VcmiSDLActivity) ctx).runOnUiThread(() ->
                ((VcmiSDLActivity) ctx).publishThorActionState(revision, enabledActionMask, activeActionMask));
    }

    @SuppressWarnings(Const.JNI_METHOD_SUPPRESS)
    public static void publishThorAdventureMap(final long revision, final long contentRevision,
            final int width, final int height, final int level, final int levels,
            final int vx, final int vy, final int vw, final int vh, final byte[] rgb, final int[] markers)
    {
        if (!BuildConfig.AYN_THOR_BUILD)
            return;
        final Context ctx = context();
        if (!(ctx instanceof VcmiSDLActivity))
            return;
        // Bound before cloning or queuing. Invalid packets explicitly clear the active surface.
        final boolean bounded = ThorAdventureMap.dimensionsValid(width, height)
                && (rgb == null || rgb.length == width * height * 3)
                && markers != null && markers.length <= ThorAdventureMap.MAX_MARKERS * ThorAdventureMap.MARKER_FIELDS;
        final byte[] copiedRgb = bounded && rgb != null ? rgb.clone() : null;
        final int[] copiedMarkers = bounded ? markers.clone() : null;
        ((VcmiSDLActivity) ctx).runOnUiThread(() -> ((VcmiSDLActivity) ctx).publishThorAdventureMap(
                revision, bounded ? contentRevision : 0, width, height, level, levels,
                vx, vy, vw, vh, copiedRgb, copiedMarkers));
    }

    @SuppressWarnings(Const.JNI_METHOD_SUPPRESS)
    public static void publishThorBrowser(final long revision, final int page, final int pageCount,
                                          final int[] targets, final String[] labels, final int[] flags)
    {
        if (!BuildConfig.AYN_THOR_BUILD)
            return;
        final Context ctx = context();
        if (!(ctx instanceof VcmiSDLActivity))
            return;
        ((VcmiSDLActivity) ctx).runOnUiThread(() ->
                ((VcmiSDLActivity) ctx).publishThorBrowser(revision, page, pageCount, targets, labels, flags));
    }

    @SuppressWarnings(Const.JNI_METHOD_SUPPRESS)
    public static void publishThorRecruitment(final long revision, final int mode, final int selectedTarget,
            final int[] targets, final int[] creatureIds, final int[] available, final int[] selectedAmounts,
            final int[] maximum, final int[] variantIndexes, final int[] variantCounts, final int[] flags,
            final long[] visualKeys, final String[] names, final String[] unitCosts, final String[] selectedCosts,
            final String townName, final String totalCost)
    {
        if (!BuildConfig.AYN_THOR_BUILD)
            return;
        final Context ctx = context();
        if (!(ctx instanceof VcmiSDLActivity))
            return;
        ((VcmiSDLActivity) ctx).runOnUiThread(() ->
                ((VcmiSDLActivity) ctx).publishThorRecruitment(revision, mode, selectedTarget, targets, creatureIds,
                        available, selectedAmounts, maximum, variantIndexes, variantCounts, flags, visualKeys,
                        names, unitCosts, selectedCosts, townName, totalCost));
    }

    @SuppressWarnings(Const.JNI_METHOD_SUPPRESS)
    public static void publishThorHeroes(final long revision, final int[] ids, final String[] names,
                                         final int[] movement, final int[] maximum, final int[] flags)
    {
        if (!BuildConfig.AYN_THOR_BUILD)
            return;
        final Context ctx = context();
        if (!(ctx instanceof VcmiSDLActivity))
            return;
        final ThorHeroRoster roster = ThorHeroRoster.copyOf(ids, names, movement, maximum, flags);
        ((VcmiSDLActivity) ctx).runOnUiThread(() -> ((VcmiSDLActivity) ctx).publishThorHeroes(revision, roster));
    }

    @SuppressWarnings(Const.JNI_METHOD_SUPPRESS)
    public static void publishThorTowns(final long revision, final int[] ids, final String[] names, final int[] flags)
    {
        if (!BuildConfig.AYN_THOR_BUILD)
            return;
        final Context ctx = context();
        if (!(ctx instanceof VcmiSDLActivity))
            return;
        final ThorTownRoster roster = ThorTownRoster.copyOf(ids, names, flags);
        ((VcmiSDLActivity) ctx).runOnUiThread(() -> ((VcmiSDLActivity) ctx).publishThorTowns(revision, roster));
    }

    @SuppressWarnings(Const.JNI_METHOD_SUPPRESS)
    public static void publishThorHeroMeetingArmies(final long revision, final int leftHeroId, final int rightHeroId,
                                                    final String[] heroNames, final int[] armyIds, final int[] creatureIds,
                                                    final int[] counts, final String[] creatureNames, final int[] flags,
                                                    final int locallyControllable, final long[] visualAssetKeys,
                                                    final long[] heroPortraitAssetKeys)
    {
        if (!BuildConfig.AYN_THOR_BUILD)
            return;
        final Context ctx = context();
        if (!(ctx instanceof VcmiSDLActivity))
            return;
        final ThorHeroMeetingArmies armies = ThorHeroMeetingArmies.copyOf(leftHeroId, rightHeroId, heroNames, armyIds,
                creatureIds, counts, creatureNames, flags, locallyControllable, visualAssetKeys, heroPortraitAssetKeys);
        ((VcmiSDLActivity) ctx).runOnUiThread(() -> ((VcmiSDLActivity) ctx).publishThorHeroMeetingArmies(revision, armies));
    }

    @SuppressWarnings(Const.JNI_METHOD_SUPPRESS)
    public static void publishThorHeroMeetingArtifacts(final long revision, final int leftHeroId, final int rightHeroId,
                                                       final String[] heroNames, final int[] positions,
                                                       final int[] flags, final String[] names,
                                                       final long[] visualAssetKeys)
    {
        if (!BuildConfig.AYN_THOR_BUILD)
            return;
        final Context ctx = context();
        if (!(ctx instanceof VcmiSDLActivity))
            return;
        final ThorHeroMeetingArtifacts artifacts = ThorHeroMeetingArtifacts.copyOf(leftHeroId, rightHeroId,
                heroNames, positions, flags, names, visualAssetKeys);
        ((VcmiSDLActivity) ctx).runOnUiThread(() ->
                ((VcmiSDLActivity) ctx).publishThorHeroMeetingArtifacts(revision, artifacts));
    }

    @SuppressWarnings(Const.JNI_METHOD_SUPPRESS)
    public static void publishThorVisualAsset(final long revision, final long key, final int width, final int height,
                                              final byte[] encoded)
    {
        if (!BuildConfig.AYN_THOR_BUILD)
            return;
        final Context ctx = context();
        if (!(ctx instanceof VcmiSDLActivity) || encoded == null || encoded.length > ThorVisualAssetPayload.MAX_BYTES)
            return;
        final byte[] copiedBytes = encoded.clone();
        ((VcmiSDLActivity) ctx).runOnUiThread(() ->
                ((VcmiSDLActivity) ctx).publishThorVisualAsset(revision, key, width, height, copiedBytes));
    }

    public static void setupMsg(final Messenger msg)
    {
        serverMessengerRef = new WeakReference<>(msg);
    }

    @SuppressWarnings(Const.JNI_METHOD_SUPPRESS)
    public static String dataRoot()
    {
        final Context ctx = context();
        String root = Storage.getVcmiDataDir(ctx).getAbsolutePath();

        Log.i("Accessing data root: " + root);
        return root;
    }

    // this path is visible only to this application; we can store base vcmi configs etc. there
    @SuppressWarnings(Const.JNI_METHOD_SUPPRESS)
    public static String internalDataRoot()
    {
        final Context ctx = context();
        String root = new File(ctx.getFilesDir(), Const.VCMI_DATA_ROOT_FOLDER_NAME).getAbsolutePath();
        Log.i("Accessing internal data root: " + root);
        return root;
    }

    /// shown when the game wants the player's attention while it is in the background
    @SuppressWarnings(Const.JNI_METHOD_SUPPRESS)
    public static void showNotification(final String message)
    {
        Notifications.showGameNotification(message);
    }

    @SuppressWarnings(Const.JNI_METHOD_SUPPRESS)
    public static void showProgress()
    {
        internalProgressDisplay(true);
    }

    @SuppressWarnings(Const.JNI_METHOD_SUPPRESS)
    public static void hideProgress()
    {
        internalProgressDisplay(false);
    }
    
    @SuppressWarnings(Const.JNI_METHOD_SUPPRESS)
    public static void hapticFeedback()
    {
        final Context ctx = context();
        if (Build.VERSION.SDK_INT >= 29) {
            ((Vibrator) ctx.getSystemService(ctx.VIBRATOR_SERVICE)).vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK));
        } else {
            ((Vibrator) ctx.getSystemService(ctx.VIBRATOR_SERVICE)).vibrate(30);
        }
    }

    private static void internalProgressDisplay(final boolean show)
    {
        final Context ctx = context();
        if (!(ctx instanceof VcmiSDLActivity))
        {
            return;
        }
        ((SDLActivity) ctx).runOnUiThread(() -> ((VcmiSDLActivity) ctx).displayProgress(show));
    }

    private static Messenger requireServerMessenger()
    {
        Messenger msg = serverMessengerRef.get();
        if (msg == null)
        {
            throw new RuntimeException("Broken server messenger");
        }
        return msg;
    }
}
