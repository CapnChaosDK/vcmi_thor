package eu.vcmi.vcmi;

import static eu.vcmi.vcmi.ThorDeckDrawing.*;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import java.util.function.BooleanSupplier;

/** Owns Hero overview, army/artifact rendering, local selection and touch cancellation. */
final class ThorHeroManagementDeck
{
    interface Drawing
    {
        boolean asset(Canvas canvas, long key, RectF area);
        void fitted(Canvas canvas, String text, float x, float y, float width, float size);
        void ellipsized(Canvas canvas, String text, float x, float y, float width, float size);
    }

    private final View host;
    private final Paint paint;
    private final Drawing drawing;
    private final BooleanSupplier sessionCurrent;
    private final Runnable changed;
    private long revision;
    private String title = "";
    private String status = "";
    private long heroPortraitAssetKey;
    private String[] detailLines = new String[ThorContextDetails.COUNT];
    private final ThorHeroManagementState state = new ThorHeroManagementState();

    ThorHeroManagementDeck(View host, Paint paint, Drawing drawing, BooleanSupplier sessionCurrent, Runnable changed)
    {
        this.host = host;
        this.paint = paint;
        this.drawing = drawing;
        this.sessionCurrent = sessionCurrent;
        this.changed = changed;
    }

    private Context getContext() { return host.getContext(); }
    private void performClick() { host.performClick(); }
    private ThorPresentationLayout layout()
    { return new ThorPresentationLayout(host.getWidth(), host.getHeight(), getContext().getResources().getDisplayMetrics().density); }
    private RectF adventureFrame() { return ThorDeckDrawing.frame(layout()); }
    private float adventureBevel() { return layout().bevel; }
    private boolean drawVisualAsset(Canvas c, long key, RectF area) { return drawing.asset(c, key, area); }
    private void drawFittedText(Canvas c, String t, float x, float y, float w, float size) { drawing.fitted(c, t, x, y, w, size); }

    private void drawEllipsizedText(Canvas c, String t, float x, float y, float w, float size)
    { drawing.ellipsized(c, t, x, y, w, size); }

    void updateContext(long revision, String contextId, long mask)
    {
        state.updateContext(revision, contextId, mask);
        this.revision = revision;
    }

    void clear() { state.clear(); }
    void cancel() { state.cancel(); }
    boolean isTouchActive() { return state.touching; }

    void draw(Canvas canvas, String title, String status, long portrait, String[] details,
              RectF frame, float dividerY, float bevel, float density)
    {
        this.title = title;
        this.status = status;
        heroPortraitAssetKey = portrait;
        detailLines = details;
        drawHeroManagement(canvas, frame, dividerY, bevel, density);
    }

    String description(String title, String status, String[] details)
    {
        return title + ". " + status + ". " + details[0] + ". " + details[1] + ". "
                + details[2] + (details[3].isEmpty() ? "" : ". " + details[3]);
    }

    private void drawHeroDashboard(final Canvas canvas, final RectF frame, final float dividerY,
                                   final float bevel, final float density)
    {
        drawHeroManagementHeader(canvas, frame, bevel, density);
        drawHeroManagementTabs(canvas, frame, bevel, density);
        paint.setColor(PARCHMENT_DARK);
        final float detailsTop = dividerY + bevel * 3f;
        final float detailsHeight = frame.top + frame.height() * 0.87f - detailsTop;
        for (int index = 0; index < ThorContextDetails.COUNT; ++index)
            drawFittedText(canvas, detailLines[index], frame.centerX(),
                    detailsTop + detailsHeight * (index + 0.5f) / ThorContextDetails.COUNT,
                    frame.width() * 0.84f, Math.min(29f * density, detailsHeight * 0.13f));
    }

    private void drawHeroManagement(final Canvas canvas, final RectF frame, final float dividerY,
                                    final float bevel, final float density)
    {
        if (state.tab == 0 || !state.management.complete())
        {
            drawHeroDashboard(canvas, frame, dividerY, bevel, density);
            return;
        }
        drawHeroManagementHeader(canvas, frame, bevel, density);
        drawHeroManagementTabs(canvas, frame, bevel, density);
        if (state.tab == 1)
            drawHeroManagementArmy(canvas, frame, dividerY, bevel, density);
        else
            drawHeroManagementArtifacts(canvas, frame, dividerY, bevel, density);
    }

    private void drawHeroManagementHeader(final Canvas canvas, final RectF frame,
                                          final float bevel, final float density)
    {
        final float contentHeight = frame.height();
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setColor(TEXT);
        drawFittedText(canvas, getContext().getString(R.string.thor_context_hero), frame.centerX(),
                frame.top + contentHeight * 0.09f, frame.width() * 0.8f,
                Math.min(24f * density, contentHeight * 0.045f));
        final float titleY = frame.top + contentHeight * 0.20f;
        boolean portraitDrawn = false;
        float portraitTextLeft = 0f;
        if (ThorVisualAssetKey.isHeroPortrait(heroPortraitAssetKey))
        {
            final float iconSize = Math.min(76f * density, contentHeight * 0.16f);
            final RectF icon = new RectF(frame.left + bevel * 3f, titleY - iconSize * 0.5f,
                    frame.left + bevel * 3f + iconSize, titleY + iconSize * 0.5f);
            portraitDrawn = drawVisualAsset(canvas, heroPortraitAssetKey, icon);
            if (portraitDrawn)
                portraitTextLeft = icon.right + bevel;
        }
        if (portraitDrawn)
        {
            drawFittedText(canvas, title, (portraitTextLeft + frame.right - bevel * 3f) * 0.5f, titleY,
                    frame.right - bevel * 3f - portraitTextLeft, Math.min(42f * density, contentHeight * 0.075f));
        }
        else
            drawFittedText(canvas, title, frame.centerX(), titleY,
                    frame.width() * 0.82f, Math.min(42f * density, contentHeight * 0.075f));

        paint.setFakeBoldText(false);
        paint.setColor(TEXT);
        drawFittedText(canvas, status, frame.centerX(), frame.top + contentHeight * 0.29f,
                frame.width() * 0.8f, Math.min(28f * density, contentHeight * 0.052f));
    }

    private void drawHeroManagementTabs(final Canvas canvas, final RectF frame,
                                        final float bevel, final float density)
    {
        final int[] labels = {R.string.thor_hero_overview, R.string.thor_hero_army, R.string.thor_hero_artifacts};
        for (int index = 0; index < labels.length; ++index)
        {
            final RectF bounds = heroManagementTabBounds(index, frame, bevel);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(state.tab == index ? STONE_DARK : STONE_LIGHT);
            canvas.drawRoundRect(bounds, bevel, bevel, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1f, bevel * 0.3f));
            paint.setColor(state.tab == index ? GOLD : PARCHMENT_DARK);
            canvas.drawRoundRect(bounds, bevel, bevel, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(TEXT);
            drawFittedText(canvas, getContext().getString(labels[index]), bounds.centerX(), bounds.centerY(),
                    bounds.width() * 0.9f, Math.min(20f * density, bounds.height() * 0.52f));
        }
    }

    private void drawHeroManagementArmy(final Canvas canvas, final RectF frame, final float dividerY,
                                        final float bevel, final float density)
    {
        final RectF message = new RectF(frame.left + bevel * 3f, dividerY + bevel * 0.8f,
                frame.right - bevel * 3f, dividerY + bevel * 2.8f);
        paint.setColor(PARCHMENT_DARK);
        drawFittedText(canvas, getContext().getString(R.string.thor_hero_army_help), message.centerX(),
                message.centerY(), message.width() * 0.94f, Math.min(20f * density, message.height() * 0.75f));
        for (int slot = 0; slot < ThorHeroManagement.ARMY_SIZE; ++slot)
        {
            final RectF row = heroManagementArmyBounds(slot, frame, dividerY, bevel);
            final boolean occupied = state.management.creatureIds[slot] >= 0;
            final boolean selected = state.selectedArmy == slot;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(selected ? PARCHMENT_DARK : STONE_DARK);
            canvas.drawRoundRect(row, bevel, bevel, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(selected ? bevel * 0.8f : bevel * 0.3f);
            paint.setColor(selected ? GOLD : STONE_LIGHT);
            canvas.drawRoundRect(row, bevel, bevel, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(selected ? TEXT : (occupied ? TEXT : PARCHMENT_DARK));
            final String value = occupied
                    ? getContext().getString(R.string.thor_army_stack_value,
                            state.management.creatureNames[slot], state.management.counts[slot])
                    : getContext().getString(R.string.thor_army_empty);
            if (occupied)
            {
                final float iconSize = Math.min(row.height() * 0.76f, row.width() * 0.1f);
                final RectF icon = new RectF(row.left + bevel, row.centerY() - iconSize * 0.5f,
                        row.left + bevel + iconSize, row.centerY() + iconSize * 0.5f);
                drawVisualAsset(canvas, state.management.creatureVisualAssetKeys[slot], icon);
                final float textLeft = icon.right + bevel;
                drawEllipsizedText(canvas, value, (textLeft + row.right) * 0.5f, row.centerY(),
                        (row.right - textLeft) * 0.92f, Math.min(22f * density, row.height() * 0.52f));
            }
            else
                drawEllipsizedText(canvas, value, row.centerX(), row.centerY(), row.width() * 0.9f,
                        Math.min(22f * density, row.height() * 0.52f));
        }
    }

    private void drawHeroManagementArtifacts(final Canvas canvas, final RectF frame, final float dividerY,
                                             final float bevel, final float density)
    {
        if (!state.management.complete())
            return;
        paint.setColor(PARCHMENT_DARK);
        drawFittedText(canvas, getContext().getString(R.string.thor_hero_artifact_help), frame.centerX(),
                dividerY + bevel * 0.6f, frame.width() * 0.9f, Math.min(19f * density, bevel * 0.95f));
        final int start = state.page * 6;
        final int visible = Math.min(6, state.management.artifactPositions.length - start);
        for (int row = 0; row < visible; ++row)
        {
            final int index = start + row;
            final RectF bounds = heroManagementArtifactBounds(row, frame, dividerY, bevel);
            final boolean occupied = (state.management.artifactFlags[index] & 1) != 0;
            final boolean locked = (state.management.artifactFlags[index] & 2) != 0;
            final boolean selected = state.selectedArtifact == index;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(locked ? Color.rgb(76, 74, 67) : (selected ? PARCHMENT_DARK : STONE_DARK));
            canvas.drawRoundRect(bounds, bevel, bevel, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(selected ? bevel * 0.8f : bevel * 0.3f);
            paint.setColor(selected ? GOLD : STONE_LIGHT);
            canvas.drawRoundRect(bounds, bevel, bevel, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(occupied ? TEXT : PARCHMENT_DARK);
            final String value = occupied ? state.management.artifactNames[index]
                    : getContext().getString((state.management.artifactFlags[index] & 4) != 0
                            ? R.string.thor_hero_backpack_empty : R.string.thor_hero_equipment_empty);
            if (occupied)
            {
                final float iconSize = Math.min(bounds.height() * 0.78f, bounds.width() * 0.1f);
                final RectF icon = new RectF(bounds.left + bevel, bounds.centerY() - iconSize * 0.5f,
                        bounds.left + bevel + iconSize, bounds.centerY() + iconSize * 0.5f);
                drawVisualAsset(canvas, state.management.artifactVisualAssetKeys[index], icon);
                final float textLeft = icon.right + bevel;
                drawEllipsizedText(canvas, value, (textLeft + bounds.right) * 0.5f, bounds.centerY(),
                        (bounds.right - textLeft) * 0.92f, Math.min(21f * density, bounds.height() * 0.5f));
            }
            else
                drawEllipsizedText(canvas, value, bounds.centerX(), bounds.centerY(), bounds.width() * 0.9f,
                        Math.min(21f * density, bounds.height() * 0.5f));
        }
        final RectF previous = heroManagementPageBounds(false, frame, bevel);
        final RectF next = heroManagementPageBounds(true, frame, bevel);
        drawHeroManagementPageButton(canvas, previous,
                getContext().getString(R.string.thor_browser_previous_page), state.page > 0, density, bevel);
        drawHeroManagementPageButton(canvas, next,
                getContext().getString(R.string.thor_browser_next_page),
                state.page + 1 < heroManagementPageCount(), density, bevel);
        paint.setColor(PARCHMENT_DARK);
        drawFittedText(canvas, getContext().getString(R.string.thor_browser_page,
                        state.page + 1, heroManagementPageCount()), frame.centerX(), previous.centerY(),
                previous.width() * 0.7f, Math.min(18f * density, previous.height() * 0.48f));
    }

    private void drawHeroManagementPageButton(final Canvas canvas, final RectF bounds, final String label,
                                              final boolean enabled, final float density, final float bevel)
    {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(enabled ? STONE_DARK : Color.rgb(76, 74, 67));
        canvas.drawRoundRect(bounds, bevel, bevel, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, bevel * 0.3f));
        paint.setColor(enabled ? GOLD : PARCHMENT_DARK);
        canvas.drawRoundRect(bounds, bevel, bevel, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(enabled ? TEXT : PARCHMENT_DARK);
        drawFittedText(canvas, label, bounds.centerX(), bounds.centerY(), bounds.width() * 0.88f,
                Math.min(16f * density, bounds.height() * 0.48f));
    }

    private RectF heroManagementTabBounds(final int tab, final RectF frame, final float bevel)
    {
        final float gap = Math.max(bevel * 0.7f, 6f);
        final float width = (frame.width() - bevel * 6f - gap * 2f) / 3f;
        final float left = frame.left + bevel * 3f + tab * (width + gap);
        final float top = frame.top + frame.height() * 0.345f;
        return new RectF(left, top, left + width, top + frame.height() * 0.045f);
    }

    private RectF heroManagementArmyBounds(final int slot, final RectF frame, final float dividerY,
                                           final float bevel)
    {
        final float gap = Math.max(bevel * 0.45f, 4f);
        final float top = dividerY + bevel * 3.4f;
        final float bottom = frame.top + frame.height() * 0.85f;
        final float height = (bottom - top - gap * (ThorHeroManagement.ARMY_SIZE - 1))
                / ThorHeroManagement.ARMY_SIZE;
        final float rowTop = top + slot * (height + gap);
        return new RectF(frame.left + bevel * 3f, rowTop, frame.right - bevel * 3f, rowTop + height);
    }

    private RectF heroManagementArtifactBounds(final int row, final RectF frame, final float dividerY,
                                               final float bevel)
    {
        final float gap = Math.max(bevel * 0.5f, 5f);
        final float top = dividerY + bevel * 1.6f;
        final float bottom = frame.top + frame.height() * 0.765f;
        final float height = (bottom - top - gap * 5f) / 6f;
        final float rowTop = top + row * (height + gap);
        return new RectF(frame.left + bevel * 3f, rowTop, frame.right - bevel * 3f, rowTop + height);
    }

    private RectF heroManagementPageBounds(final boolean next, final RectF frame, final float bevel)
    {
        final float left = frame.left + bevel * 3f;
        final float right = frame.right - bevel * 3f;
        final float width = (right - left) * 0.29f;
        final float top = frame.top + frame.height() * 0.785f;
        final float bottom = frame.top + frame.height() * 0.85f;
        return next ? new RectF(right - width, top, right, bottom) : new RectF(left, top, left + width, bottom);
    }

    private int heroManagementPageCount()
    {
        return state.pageCount();
    }

    private int heroManagementArmyAt(final float x, final float y, final RectF frame,
                                     final float dividerY, final float bevel)
    {
        for (int slot = 0; slot < ThorHeroManagement.ARMY_SIZE; ++slot)
            if (heroManagementArmyBounds(slot, frame, dividerY, bevel).contains(x, y))
                return slot;
        return -1;
    }

    private int heroManagementArtifactAt(final float x, final float y, final RectF frame,
                                         final float dividerY, final float bevel)
    {
        final int first = state.page * 6;
        final int count = Math.min(6, state.management.artifactPositions.length - first);
        for (int row = 0; row < count; ++row)
            if (heroManagementArtifactBounds(row, frame, dividerY, bevel).contains(x, y))
                return first + row;
        return -1;
    }

    boolean touch(final MotionEvent event)
    {
        final int action = event.getActionMasked();
        if (state.touching)
        {
            if (!state.acceptsPointer(event.getPointerId(0), event.getPointerCount())
                    || action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_POINTER_UP
                    || action == MotionEvent.ACTION_CANCEL)
            {
                state.finish(false, false);
                return true;
            }
            if (action == MotionEvent.ACTION_UP)
                finishTouch(event.getX(), event.getY());
            return true;
        }
        if (action != MotionEvent.ACTION_DOWN)
            return true;
        int pressedTab = -1;
        for (int tab = 0; tab < 3; ++tab)
            if (heroManagementTabBounds(tab, adventureFrame(), adventureBevel()).contains(event.getX(), event.getY()))
                pressedTab = tab;
        state.begin(event.getPointerId(0), pressedTab);
        // A malformed initial multi-pointer event must not seed a usable gesture.
        state.acceptsPointer(event.getPointerId(0), event.getPointerCount());
        return true;
    }

    private void finishTouch(float x, float y)
    {
        final int pressedTab = state.pressedTab;
        final long submittedRevision = revision;
        if (!state.finish(true, sessionCurrent.getAsBoolean())) return;
        final RectF frame = adventureFrame();
        final float bevel = adventureBevel();
        if (pressedTab >= 0 && heroManagementTabBounds(pressedTab, frame, bevel).contains(x, y))
        {
            state.selectTab(pressedTab);
            changed.run();
            performClick();
            return;
        }
        if (!state.management.complete()) return;
        final float dividerY = frame.top + frame.height() * ThorAdventureLayout.DIVIDER;
        int pair = ThorHeroManagementPair.INVALID;
        int action = ThorActionIds.NONE;
        if (state.tab == 1)
        {
            pair = state.armyPair(heroManagementArmyAt(x, y, frame, dividerY, bevel));
            action = ThorActionIds.HERO_WINDOW_TRANSFER_STACK;
        }
        else if (state.tab == 2)
        {
            if (heroManagementPageBounds(false, frame, bevel).contains(x, y)) state.changePage(-1);
            else if (heroManagementPageBounds(true, frame, bevel).contains(x, y)) state.changePage(1);
            else pair = state.artifactPair(heroManagementArtifactAt(x, y, frame, dividerY, bevel));
            action = ThorActionIds.HERO_WINDOW_TRANSFER_ARTIFACT;
        }
        if (pair != ThorHeroManagementPair.INVALID)
        {
            performClick();
            NativeMethods.submitThorAction(submittedRevision, action, pair);
        }
        changed.run();
    }

    void updateHeroManagement(final ThorHeroManagement management)
    {
        state.update(management);
        changed.run();
    }
}
