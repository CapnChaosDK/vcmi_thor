package eu.vcmi.vcmi;

import android.app.AlertDialog;
import android.app.Presentation;
import android.os.Build;
import android.text.InputType;
import android.text.InputFilter;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.EditorInfo;
import android.content.Context;
import android.widget.EditText;

/** A focusable child exists only while editing. The Presentation never takes SDL focus. */
final class ThorSaveNameEditor
{
    private final Presentation host;
    private final ThorSaveNameState state = new ThorSaveNameState();
    private AlertDialog dialog;

    ThorSaveNameEditor(final Presentation host) { this.host = host; }

    void update(final String context, final long revision, final long session)
    {
        if (dialog != null && (!ThorContextIds.SAVE_GAME.equals(context) || !state.current(revision, session)))
            cancel();
    }

    void cancel()
    {
        state.cancel();
        final AlertDialog previous = dialog;
        dialog = null;
        if (previous != null) previous.dismiss();
    }

    void open(final String initial, final long revision, final long session,
              final ThorSecondScreenPresentation.SessionValidity validity)
    {
        cancel();
        if (validity == null || !validity.isCurrent(session) || host.getWindow() == null) return;
        state.begin(revision, session);
        final EditText input = new EditText(host.getContext())
        {
            @Override public boolean onKeyPreIme(final int keyCode, final KeyEvent event)
            {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP)
                {
                    cancel();
                    return true;
                }
                return super.onKeyPreIme(keyCode, event);
            }
        };
        input.setSingleLine(true);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(ThorSaveNameState.MAX_BYTES)});
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setSelectAllOnFocus(true);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setText(initial);
        input.setHint(R.string.thor_save_filename);
        final AlertDialog next = new AlertDialog.Builder(host.getContext())
                .setTitle(R.string.thor_save_edit)
                .setMessage(R.string.thor_save_name_hint)
                .setView(input)
                .setPositiveButton(R.string.thor_menu_ok, null)
                .setNegativeButton(android.R.string.cancel, (d, which) -> { if (dialog == d) cancel(); })
                .create();
        dialog = next;
        next.setCanceledOnTouchOutside(false);
        next.setOnCancelListener(d -> { if (dialog == next) cancel(); });
        next.setOnDismissListener(d ->
        {
            // Dismiss callbacks can arrive after another draft has opened.
            if (dialog != null && dialog != next) return;
            state.cancel();
            final InputMethodManager ime = (InputMethodManager) input.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (ime != null) ime.hideSoftInputFromWindow(input.getWindowToken(), 0);
            if (dialog == next) dialog = null;
        });
        final Window window = next.getWindow();
        if (window == null) { cancel(); return; }
        window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG);
        final WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.token = host.getWindow().getDecorView().getWindowToken();
        window.setAttributes(attributes);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        next.setOnShowListener(d ->
        {
            next.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v ->
            {
                final String text = input.getText().toString();
                if (!ThorSaveNameState.valid(text))
                {
                    input.setError(host.getContext().getString(R.string.thor_save_name_invalid));
                    return;
                }
                if (!validity.isCurrent(session) || !state.commit(text, revision, session))
                {
                    cancel();
                    return;
                }
                if (NativeMethods.submitThorSaveName(revision, text)) cancel();
                else
                {
                    state.begin(revision, session);
                    input.setError(host.getContext().getString(R.string.thor_browser_unavailable));
                }
            });
            input.setOnEditorActionListener((v, action, event) ->
            {
                if (action != EditorInfo.IME_ACTION_DONE) return false;
                next.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                return true;
            });
            input.requestFocus();
            input.post(() ->
            {
                if (dialog != next || !state.current(revision, session)) return;
                final InputMethodManager ime = (InputMethodManager) input.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (ime != null) ime.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            });
        });
        if (Build.VERSION.SDK_INT >= 30)
        {
            final boolean[] keyboardSeen = {false};
            input.setOnApplyWindowInsetsListener((v, insets) ->
            {
                final boolean visible = insets.isVisible(WindowInsets.Type.ime());
                if (keyboardSeen[0] && !visible) input.post(() -> { if (dialog == next) cancel(); });
                keyboardSeen[0] |= visible;
                return insets;
            });
        }
        try { next.show(); }
        catch (WindowManager.BadTokenException | WindowManager.InvalidDisplayException unavailable) { cancel(); }
    }
}
