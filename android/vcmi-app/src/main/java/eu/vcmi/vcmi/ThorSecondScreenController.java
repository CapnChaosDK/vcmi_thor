package eu.vcmi.vcmi;

import android.content.Context;
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

    private ThorSecondScreenPresentation presentation;
    private long contextRevision;
    private String contextId = ThorContextIds.UNKNOWN;
    private String contextTitle = "";
    private String contextStatus = "";
    private boolean started;
    private boolean resumed;

    ThorSecondScreenController(final VcmiSDLActivity activity)
    {
        this.activity = activity;
        displayManager = (DisplayManager) activity.getSystemService(Context.DISPLAY_SERVICE);
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

    void publishContext(final long revision, final String id, final String title, final String status)
    {
        if (revision <= contextRevision)
            return;

        contextRevision = revision;
        contextId = id == null || id.isEmpty() ? ThorContextIds.UNKNOWN : id;
        contextTitle = title == null ? "" : title;
        contextStatus = status == null ? "" : status;
        Log.i(LOG_TAG, "Context " + contextId + " revision " + contextRevision);
        if (presentation != null)
            presentation.updateContext(contextId, contextTitle, contextStatus);
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

        final ThorSecondScreenPresentation newPresentation = new ThorSecondScreenPresentation(activity, targetDisplay);
        newPresentation.setOnDismissListener(dialog ->
        {
            if (presentation == dialog)
                presentation = null;
        });

        try
        {
            newPresentation.show();
            presentation = newPresentation;
            newPresentation.updateContext(contextId, contextTitle, contextStatus);
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
        presentation = null;
        try
        {
            oldPresentation.dismiss();
        }
        catch (final RuntimeException exception)
        {
            Log.w(LOG_TAG, "Unable to dismiss companion presentation cleanly", exception);
        }
    }
}
