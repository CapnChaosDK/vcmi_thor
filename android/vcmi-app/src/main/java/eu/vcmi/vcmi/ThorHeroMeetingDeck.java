package eu.vcmi.vcmi;

import static eu.vcmi.vcmi.ThorDeckDrawing.*;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

/** Owns the Hero Meeting renderer and all transient army/artifact interaction state. */
final class ThorHeroMeetingDeck
{
    interface Drawing
    {
        boolean asset(Canvas canvas, long key, RectF area);
        void fitted(Canvas canvas, String text, float x, float y, float width, float size);
        void ellipsized(Canvas canvas, String text, float x, float y, float width, float size);
    }
    private final View host;
    private final Paint paint;
    private final int touchSlop;
    private final Drawing drawing;
    private final Runnable changed;
    private long revision;
    private String contextId = ThorContextIds.UNKNOWN;
    private long enabledActionMask;
    private String title = "";
    private String status = "";

    ThorHeroMeetingDeck(View host, Paint paint, int touchSlop, Drawing drawing, Runnable changed)
    {
        this.host = host;
        this.paint = paint;
        this.touchSlop = touchSlop;
        this.drawing = drawing;
        this.changed = changed;
    }
    private void refresh() { changed.run(); }
    private Context getContext() { return host.getContext(); }
    private void invalidate() { host.invalidate(); }
    private void performClick() { host.performClick(); }
    private void removeCallbacks(Runnable runnable) { host.removeCallbacks(runnable); }
    private void postDelayed(Runnable runnable, long delay) { host.postDelayed(runnable, delay); }
    private boolean isActionEnabled(int action) { return ThorLobbyScenarioState.isActionEnabled(enabledActionMask, action); }
    private ThorPresentationLayout layout()
    { return new ThorPresentationLayout(host.getWidth(), host.getHeight(), getContext().getResources().getDisplayMetrics().density); }
    private RectF adventureFrame() { return ThorDeckDrawing.frame(layout()); }
    private float adventureBevel() { return layout().bevel; }
    private boolean drawVisualAsset(Canvas c, long key, RectF area) { return drawing.asset(c, key, area); }
    private void drawFittedText(Canvas c, String t, float x, float y, float w, float size) { drawing.fitted(c, t, x, y, w, size); }
    private void drawEllipsizedText(Canvas c, String t, float x, float y, float w, float size) { drawing.ellipsized(c, t, x, y, w, size); }
    void updateContext(long revision, String contextId, long mask)
    {
        final boolean retain = ThorHeroMeetingGesture.retainsArmies(this.revision, this.contextId, revision, contextId);
        if (revision != this.revision || !contextId.equals(this.contextId))
        {
            heroMeetingArmies = ThorHeroMeetingArmies.EMPTY;
            heroMeetingArtifacts = ThorHeroMeetingArtifacts.EMPTY;
            artifactPage = 0;
            selectedArtifact = -1;
            selectedMeetingSlot = -1;
            pendingMeetingAction = false;
            cancelHeroMeetingGesture();
        }
        this.revision = revision;
        this.contextId = contextId;
        heroMeetingMode.update(revision, ThorContextIds.HERO_MEETING.equals(contextId));
        if (!retain)
        {
            heroMeetingArmies = ThorHeroMeetingArmies.EMPTY;
            selectedMeetingSlot = -1;
            pendingMeetingAction = false;
            cancelHeroMeetingGesture();
        }
        enabledActionMask = mask;
    }
    void clear()
    {
        heroMeetingArmies = ThorHeroMeetingArmies.EMPTY;
        heroMeetingArtifacts = ThorHeroMeetingArtifacts.EMPTY;
        selectedArtifact = -1;
        selectedMeetingSlot = -1;
        pendingMeetingAction = false;
        cancelHeroMeetingGesture();
    }
    void cancel() { cancelHeroMeetingGesture(); }
    void detach()
    {
        clear();
        heroMeetingTouchSequence = false;
        heroMeetingTouchCancelled = false;
        heroMeetingPointerId = -1;
    }
    void draw(Canvas canvas, String title, String status, RectF frame, float bevel, float density)
    {
        this.title = title;
        this.status = status;
        drawHeroMeeting(canvas, frame, bevel, density);
    }
    private ThorHeroMeetingArmies heroMeetingArmies = ThorHeroMeetingArmies.EMPTY;
    private ThorHeroMeetingArtifacts heroMeetingArtifacts = ThorHeroMeetingArtifacts.EMPTY;
    private final ThorHeroMeetingModeState heroMeetingMode = new ThorHeroMeetingModeState();
    private int artifactPage;
    private int selectedArtifact = -1;
    private int selectedMeetingSlot = -1;
    private boolean pendingMeetingAction;
    private final ThorHeroMeetingGesture heroMeetingGesture = new ThorHeroMeetingGesture();
    private final ThorHeroMeetingArtifactGesture artifactGesture = new ThorHeroMeetingArtifactGesture();
    private final ThorHeroMeetingSplitState heroMeetingSplit = new ThorHeroMeetingSplitState();
    private boolean heroMeetingTouchSequence;
    private boolean heroMeetingTouchCancelled;
    private long heroMeetingTouchRevision;
    private int heroMeetingPointerId = -1;
    private float artifactTouchDownX;
    private float artifactTouchDownY;
    private boolean artifactTouchMovedWithoutSource;
    private final Runnable heroMeetingLongPress = () ->
    {
        if (heroMeetingMode.mode() == ThorHeroMeetingModeState.ARMY
                && heroMeetingGesture.activateLongPress(revision, ThorContextIds.HERO_MEETING.equals(contextId)))
        {
            final int source = heroMeetingGesture.sourceKey();
            if (heroMeetingSplit.begin(revision, source, heroMeetingArmies.counts[source]))
            {
                selectedMeetingSlot = source;
                refresh();
                invalidate();
            }
        }
    };

    void updateHeroMeetingArmies(final ThorHeroMeetingArmies armies)
    {
        cancelHeroMeetingGesture();
        heroMeetingArmies = ThorContextIds.HERO_MEETING.equals(contextId) && armies.complete()
                ? armies : ThorHeroMeetingArmies.EMPTY;
        selectedMeetingSlot = -1;
        pendingMeetingAction = false;
        refresh();
        invalidate();
    }

    void updateHeroMeetingArtifacts(final ThorHeroMeetingArtifacts artifacts)
    {
        selectedArtifact = -1;
        artifactGesture.cancel();
        if (heroMeetingTouchSequence && heroMeetingMode.mode() == ThorHeroMeetingModeState.ARTIFACTS)
            heroMeetingTouchCancelled = true;
        heroMeetingArtifacts = ThorContextIds.HERO_MEETING.equals(contextId) && artifacts.complete()
                ? artifacts : ThorHeroMeetingArtifacts.EMPTY;
        refresh();
        invalidate();
    }

    private void drawHeroMeeting(final Canvas canvas, final RectF frame, final float bevel, final float density)
    {
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setColor(TEXT);
        drawFittedText(canvas, title, frame.centerX(), frame.top + frame.height() * 0.08f, frame.width() * 0.8f,
                Math.min(38f * density, frame.height() * 0.05f));
        paint.setFakeBoldText(false);
        paint.setColor(PARCHMENT_DARK);
        drawFittedText(canvas, status, frame.centerX(), frame.top + frame.height() * 0.13f, frame.width() * 0.78f,
                Math.min(24f * density, frame.height() * 0.035f));
        drawHeroMeetingModes(canvas, frame, bevel, density);
        if (heroMeetingMode.mode() == ThorHeroMeetingModeState.ARTIFACTS)
        {
            drawHeroMeetingArtifacts(canvas, frame, bevel, density);
            return;
        }
        if (!heroMeetingArmies.complete())
            return;

        final String[] names = heroMeetingArmies.heroNames;
        for (int side = 0; side < 2; ++side)
        {
            final RectF heading = heroMeetingHeadingBounds(side, frame, bevel);
            drawHeroMeetingHeading(canvas, heading, names[side],
                    heroMeetingArmies.heroPortraitAssetKeys[side], bevel, density);
            for (int slot = 0; slot < 7; ++slot)
            {
                final int index = side * 7 + slot;
                final RectF row = heroMeetingSlotBounds(index, frame, bevel);
                final boolean occupied = (heroMeetingArmies.flags[index] & 1) != 0;
                final boolean selected = selectedMeetingSlot == index;
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
                                heroMeetingArmies.creatureNames[index], heroMeetingArmies.counts[index])
                        : getContext().getString(R.string.thor_army_empty);
                if (occupied)
                {
                    final float iconSize = Math.min(row.height() * 0.78f, row.width() * 0.1f);
                    final float left = row.left + Math.max(bevel * 0.55f, 5f);
                    final RectF iconBounds = new RectF(left, row.centerY() - iconSize * 0.5f,
                            left + iconSize, row.centerY() + iconSize * 0.5f);
                    drawVisualAsset(canvas, heroMeetingArmies.visualAssetKeys[index], iconBounds);
                    final float textLeft = iconBounds.right + Math.max(bevel * 0.7f, 7f);
                    drawEllipsizedText(canvas, value, (textLeft + row.right) * 0.5f, row.centerY(),
                            (row.right - textLeft) * 0.94f, Math.min(22f * density, row.height() * 0.55f));
                }
                else
                    drawEllipsizedText(canvas, value, row.centerX(), row.centerY(), row.width() * 0.88f,
                            Math.min(22f * density, row.height() * 0.55f));
            }
        }
        if (heroMeetingSplit.stage() != ThorHeroMeetingSplitState.Stage.NONE)
        {
            paint.setColor(PARCHMENT_DARK);
            final int instruction = heroMeetingSplit.stage() == ThorHeroMeetingSplitState.Stage.DESTINATION
                    ? R.string.thor_split_select_destination : R.string.thor_split_amount;
            drawFittedText(canvas, getContext().getString(instruction), frame.centerX(),
                    frame.top + frame.height() * 0.775f, frame.width() * 0.8f,
                    Math.min(22f * density, frame.height() * 0.03f));
        }
        if (heroMeetingSplit.stage() == ThorHeroMeetingSplitState.Stage.AMOUNT)
        {
            drawHeroMeetingSplitEditor(canvas, frame, bevel, density);
            return;
        }
        final int[] actions = {ThorActionIds.HERO_MEETING_ARMY_LEFT_TO_RIGHT,
                ThorActionIds.HERO_MEETING_SWAP_ARMIES, ThorActionIds.HERO_MEETING_ARMY_RIGHT_TO_LEFT};
        final int[] labels = {R.string.thor_army_move_right, R.string.thor_army_swap, R.string.thor_army_move_left};
        for (int index = 0; index < actions.length; ++index)
        {
            final RectF button = heroMeetingActionBounds(index, frame, bevel);
            final boolean enabled = isActionEnabled(actions[index]);
            paint.setColor(enabled ? STONE_DARK : Color.rgb(76, 74, 67));
            canvas.drawRoundRect(button, bevel, bevel, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(2f, bevel * 0.35f));
            paint.setColor(enabled ? GOLD : PARCHMENT_DARK);
            canvas.drawRoundRect(button, bevel, bevel, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(enabled ? TEXT : PARCHMENT_DARK);
            drawFittedText(canvas, getContext().getString(labels[index]), button.centerX(), button.centerY(),
                    button.width() * 0.85f, Math.min(22f * density, button.height() * 0.48f));
        }
        drawHeroMeetingDragFeedback(canvas, frame, bevel, density);
    }

    private void drawHeroMeetingModes(final Canvas canvas, final RectF frame, final float bevel, final float density)
    {
        final int[] labels = {R.string.thor_meeting_army, R.string.thor_meeting_artifacts};
        for (int index = 0; index < 2; ++index)
        {
            final RectF tab = heroMeetingModeBounds(index, frame, bevel);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(heroMeetingMode.mode() == index ? STONE_DARK : STONE_LIGHT);
            canvas.drawRoundRect(tab, bevel, bevel, paint);
            paint.setColor(TEXT);
            drawFittedText(canvas, getContext().getString(labels[index]), tab.centerX(), tab.centerY(),
                    tab.width() * 0.85f, Math.min(20f * density, tab.height() * 0.55f));
        }
    }

    private void drawHeroMeetingArtifacts(final Canvas canvas, final RectF frame, final float bevel,
                                           final float density)
    {
        if (!heroMeetingArtifacts.complete())
            return;
        for (int side = 0; side < 2; ++side)
        {
            final RectF heading = heroMeetingHeadingBounds(side, frame, bevel);
            final long portraitKey = heroMeetingArmies.complete()
                    ? heroMeetingArmies.heroPortraitAssetKeys[side] : 0L;
            drawHeroMeetingHeading(canvas, heading, heroMeetingArtifacts.heroNames[side], portraitKey, bevel, density);
            for (int row = 0; row < 6; ++row)
            {
                final int local = artifactPage * 6 + row;
                if (local >= ThorHeroMeetingArtifacts.PER_HERO)
                    continue;
                final int index = side * ThorHeroMeetingArtifacts.PER_HERO + local;
                final RectF bounds = heroMeetingArtifactBounds(side, row, frame, bevel);
                final boolean occupied = (heroMeetingArtifacts.flags[index] & 1) != 0;
                final boolean locked = (heroMeetingArtifacts.flags[index] & 2) != 0;
                final boolean backpack = (heroMeetingArtifacts.flags[index] & 4) != 0;
                paint.setColor(locked ? Color.rgb(76, 74, 67) : STONE_DARK);
                canvas.drawRoundRect(bounds, bevel, bevel, paint);
                if (selectedArtifact == index || artifactGesture.isDragging()
                        && artifactGesture.sourceKey() == index)
                {
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(Math.max(2f, bevel * 0.4f));
                    paint.setColor(GOLD);
                    canvas.drawRoundRect(bounds, bevel, bevel, paint);
                    paint.setStyle(Paint.Style.FILL);
                }
                paint.setColor(occupied ? TEXT : PARCHMENT_DARK);
                final String prefix = backpack ? getContext().getString(R.string.thor_artifact_backpack)
                        : getContext().getString(R.string.thor_artifact_equipped);
                final String value = prefix + " " + (occupied ? heroMeetingArtifacts.names[index]
                        : getContext().getString(R.string.thor_army_empty))
                        + (locked ? " " + getContext().getString(R.string.thor_artifact_locked) : "");
                if (occupied)
                {
                    final float iconSize = Math.min(bounds.height() * 0.72f, bounds.width() * 0.08f);
                    final float left = bounds.left + Math.max(bevel * 0.55f, 5f);
                    final RectF iconBounds = new RectF(left, bounds.centerY() - iconSize * 0.5f,
                            left + iconSize, bounds.centerY() + iconSize * 0.5f);
                    drawVisualAsset(canvas, heroMeetingArtifacts.visualAssetKeys[index], iconBounds);
                    final float textLeft = iconBounds.right + Math.max(bevel * 0.7f, 7f);
                    drawEllipsizedText(canvas, value, (textLeft + bounds.right) * 0.5f, bounds.centerY(),
                            (bounds.right - textLeft) * 0.95f, Math.min(20f * density, bounds.height() * 0.5f));
                }
                else
                    drawEllipsizedText(canvas, value, bounds.centerX(), bounds.centerY(), bounds.width() * 0.9f,
                            Math.min(20f * density, bounds.height() * 0.5f));
            }
        }
        final String page = (artifactPage + 1) + " / 4";
        for (int index = 0; index < 2; ++index)
        {
            final RectF button = heroMeetingArtifactPageBounds(index, frame, bevel);
            paint.setColor(STONE_DARK);
            canvas.drawRoundRect(button, bevel, bevel, paint);
            paint.setColor(TEXT);
            drawFittedText(canvas, getContext().getString(index == 0 ? R.string.thor_previous : R.string.thor_next),
                    button.centerX(), button.centerY(), button.width() * 0.85f,
                    Math.min(20f * density, button.height() * 0.48f));
        }
        paint.setColor(TEXT);
        drawFittedText(canvas, page, frame.centerX(), frame.top + frame.height() * 0.815f,
                frame.width() * 0.3f, 20f * density);
        if (selectedArtifact >= 0)
        {
            paint.setColor(GOLD);
            final String selected = getContext().getString(R.string.thor_artifact_selected,
                    heroMeetingArtifacts.names[selectedArtifact]);
            drawEllipsizedText(canvas, selected, frame.centerX(), frame.top + frame.height() * 0.85f,
                    frame.width() * 0.3f, 17f * density);
        }
        final int[] labels = {R.string.thor_artifact_move_right, R.string.thor_artifact_swap,
                R.string.thor_artifact_move_left};
        for (int index = 0; index < labels.length; ++index)
        {
            final RectF button = heroMeetingArtifactActionBounds(index, frame, bevel);
            final boolean enabled = isActionEnabled(ThorActionIds.artifactBulkActionForButton(index))
                    && !pendingMeetingAction;
            paint.setColor(enabled ? STONE_DARK : Color.rgb(76, 74, 67));
            canvas.drawRoundRect(button, bevel, bevel, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(2f, bevel * 0.35f));
            paint.setColor(enabled ? GOLD : PARCHMENT_DARK);
            canvas.drawRoundRect(button, bevel, bevel, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(enabled ? TEXT : PARCHMENT_DARK);
            drawFittedText(canvas, getContext().getString(labels[index]), button.centerX(), button.centerY(),
                    button.width() * 0.86f, Math.min(20f * density, button.height() * 0.45f));
        }
        drawArtifactDragFeedback(canvas, frame, bevel, density);
    }

    private RectF heroMeetingArtifactPageBounds(final int index, final RectF frame, final float bevel)
    {
        final float width = frame.width() * 0.32f;
        final float left = index == 0 ? frame.left + bevel * 3f : frame.right - bevel * 3f - width;
        return new RectF(left, frame.top + frame.height() * 0.785f, left + width,
                frame.top + frame.height() * 0.88f);
    }

    private RectF heroMeetingArtifactActionBounds(final int index, final RectF frame, final float bevel)
    {
        final float gap = Math.max(bevel, 8f);
        final float width = (frame.width() - bevel * 6f - gap * 2f) / 3f;
        final float left = frame.left + bevel * 3f + index * (width + gap);
        return new RectF(left, frame.top + frame.height() * 0.89f, left + width,
                frame.top + frame.height() * 0.99f);
    }

    private void drawArtifactDragFeedback(final Canvas canvas, final RectF frame,
                                          final float bevel, final float density)
    {
        if (!artifactGesture.isDragging())
            return;
        final int source = artifactGesture.sourceKey();
        final int destination = artifactAt(artifactGesture.pointerX(), artifactGesture.pointerY(), frame, bevel);
        if (ThorHeroMeetingArtifactPair.encode(source, destination) != ThorHeroMeetingArtifactPair.INVALID
                && (heroMeetingArtifacts.flags[destination] & 2) == 0)
        {
            final RectF target = heroMeetingArtifactBounds(destination / ThorHeroMeetingArtifacts.PER_HERO,
                    destination % ThorHeroMeetingArtifacts.PER_HERO - artifactPage * 6, frame, bevel);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(3f * density, bevel * 0.8f));
            paint.setColor(GOLD);
            canvas.drawRoundRect(target, bevel, bevel, paint);
            paint.setStyle(Paint.Style.FILL);
        }
        final String label = heroMeetingArtifacts.names[source];
        paint.setColor(PARCHMENT_DARK);
        paint.setTextSize(22f * density);
        final float width = Math.min(frame.width() * 0.58f, paint.measureText(label) + bevel * 4f);
        final float height = Math.max(38f * density, bevel * 3f);
        final float centerX = Math.max(frame.left + width * 0.5f,
                Math.min(frame.right - width * 0.5f, artifactGesture.pointerX()));
        final float centerY = Math.max(frame.top + height * 0.5f,
                Math.min(frame.bottom - height * 0.5f, artifactGesture.pointerY() - height));
        final RectF cursor = new RectF(centerX - width * 0.5f, centerY - height * 0.5f,
                centerX + width * 0.5f, centerY + height * 0.5f);
        canvas.drawRoundRect(cursor, bevel, bevel, paint);
        paint.setColor(TEXT);
        drawEllipsizedText(canvas, label, centerX, centerY, width * 0.86f,
                Math.min(22f * density, height * 0.56f));
    }

    private RectF heroMeetingModeBounds(final int mode, final RectF frame, final float bevel)
    {
        final float width = frame.width() * 0.18f;
        final float left = frame.centerX() + (mode == 0 ? -width - bevel : bevel);
        return new RectF(left, frame.top + frame.height() * 0.115f, left + width,
                frame.top + frame.height() * 0.155f);
    }

    private RectF heroMeetingArtifactBounds(final int side, final int row, final RectF frame, final float bevel)
    {
        final RectF heading = heroMeetingHeadingBounds(side, frame, bevel);
        final float gap = Math.max(bevel * 0.7f, 6f);
        final float bottom = frame.top + frame.height() * 0.78f;
        final float height = (bottom - heading.bottom - gap * 5f) / 6f;
        final float top = heading.bottom + gap + row * (height + gap);
        return new RectF(heading.left, top, heading.right, top + height);
    }

    private RectF heroMeetingHeadingBounds(final int side, final RectF frame, final float bevel)
    {
        final float gap = Math.max(bevel, 8f);
        final float width = (frame.width() - bevel * 6f - gap) * 0.5f;
        final float left = frame.left + bevel * 3f + side * (width + gap);
        final float top = frame.top + frame.height() * 0.16f;
        return new RectF(left, top, left + width, top + frame.height() * 0.05f);
    }

    private void drawHeroMeetingHeading(final Canvas canvas, final RectF heading, final String heroName,
                                       final long portraitKey, final float bevel, final float density)
    {
        float textLeft = heading.left;
        if (ThorVisualAssetKey.isHeroPortrait(portraitKey))
        {
            final float iconSize = Math.min(heading.height() * 0.9f, heading.width() * 0.14f);
            final RectF icon = new RectF(heading.left, heading.centerY() - iconSize * 0.5f,
                    heading.left + iconSize, heading.centerY() + iconSize * 0.5f);
            if (drawVisualAsset(canvas, portraitKey, icon))
                textLeft = icon.right + bevel * 0.5f;
        }
        paint.setColor(PARCHMENT_DARK);
        paint.setFakeBoldText(true);
        drawEllipsizedText(canvas, heroName, (textLeft + heading.right) * 0.5f, heading.centerY(),
                (heading.right - textLeft) * 0.94f, Math.min(26f * density, heading.height() * 0.6f));
        paint.setFakeBoldText(false);
    }

    private RectF heroMeetingSlotBounds(final int index, final RectF frame, final float bevel)
    {
        final int side = index / 7;
        final int row = index % 7;
        final RectF heading = heroMeetingHeadingBounds(side, frame, bevel);
        final float gap = Math.max(bevel * 0.7f, 6f);
        final float bottom = frame.top + frame.height() * 0.75f;
        final float height = (bottom - heading.bottom - gap * 6f) / 7f;
        final float top = heading.bottom + gap + row * (height + gap);
        return new RectF(heading.left, top, heading.right, top + height);
    }

    private RectF heroMeetingActionBounds(final int index, final RectF frame, final float bevel)
    {
        final float gap = Math.max(bevel, 8f);
        final float width = (frame.width() - bevel * 6f - gap * 2f) / 3f;
        final float left = frame.left + bevel * 3f + index * (width + gap);
        final float top = frame.top + frame.height() * 0.80f;
        return new RectF(left, top, left + width, frame.bottom - bevel * 3f);
    }

    private RectF heroMeetingSplitButtonBounds(final int index, final RectF frame, final float bevel)
    {
        final float gap = Math.max(bevel * 0.5f, 5f);
        final float width = (frame.width() - bevel * 6f - gap * 5f) / 6f;
        final float left = frame.left + bevel * 3f + index * (width + gap);
        final float top = frame.top + frame.height() * 0.82f;
        return new RectF(left, top, left + width, frame.bottom - bevel * 3f);
    }

    private void drawHeroMeetingSplitEditor(final Canvas canvas, final RectF frame, final float bevel,
                                             final float density)
    {
        final int[] labels = {R.string.thor_split_minus_ten, R.string.thor_split_minus_one,
                R.string.thor_split_plus_one, R.string.thor_split_plus_ten,
                R.string.thor_split_cancel, R.string.thor_split_confirm};
        for (int index = 0; index < labels.length; ++index)
        {
            final RectF button = heroMeetingSplitButtonBounds(index, frame, bevel);
            paint.setColor(STONE_DARK);
            canvas.drawRoundRect(button, bevel, bevel, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(2f, bevel * 0.35f));
            paint.setColor(GOLD);
            canvas.drawRoundRect(button, bevel, bevel, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(TEXT);
            final String label = index == 5
                    ? getContext().getString(labels[index]) + " " + heroMeetingSplit.amount()
                    : getContext().getString(labels[index]);
            drawFittedText(canvas, label, button.centerX(), button.centerY(), button.width() * 0.86f,
                    Math.min(20f * density, button.height() * 0.45f));
        }
    }

    private int heroMeetingSlotAt(final float x, final float y, final RectF frame, final float bevel)
    {
        for (int index = 0; index < ThorHeroMeetingArmies.SLOT_COUNT; ++index)
            if (heroMeetingSlotBounds(index, frame, bevel).contains(x, y))
                return index;
        return -1;
    }

    private int artifactAt(final float x, final float y, final RectF frame, final float bevel)
    {
        for (int side = 0; side < 2; ++side)
            for (int row = 0; row < 6; ++row)
            {
                final int local = artifactPage * 6 + row;
                if (local < ThorHeroMeetingArtifacts.PER_HERO
                        && heroMeetingArtifactBounds(side, row, frame, bevel).contains(x, y))
                    return side * ThorHeroMeetingArtifacts.PER_HERO + local;
            }
        return -1;
    }

    private void beginArtifactGesture(final float x, final float y)
    {
        if (pendingMeetingAction || !heroMeetingArtifacts.complete())
            return;
        final int source = artifactAt(x, y, adventureFrame(), adventureBevel());
        final boolean legal = source >= 0 && (heroMeetingArtifacts.flags[source] & 3) == 1
                && isActionEnabled(ThorActionIds.HERO_MEETING_TRANSFER_ARTIFACT);
        artifactGesture.begin(revision, source, legal, x, y);
    }

    private void updateArtifactGesture(final float x, final float y)
    {
        if (artifactGesture.move(x, y, touchSlop, revision,
                ThorContextIds.HERO_MEETING.equals(contextId)
                        && heroMeetingMode.mode() == ThorHeroMeetingModeState.ARTIFACTS))
        {
            selectedArtifact = -1;
            invalidate();
        }
    }

    private void finishArtifactGesture(final float x, final float y)
    {
        if (!artifactGesture.isArmed())
        {
            final float dx = x - artifactTouchDownX;
            final float dy = y - artifactTouchDownY;
            if (!artifactTouchMovedWithoutSource && dx * dx + dy * dy <= touchSlop * touchSlop)
                handleHeroMeetingTap(x, y);
            return;
        }
        final int source = artifactGesture.sourceKey();
        final boolean wasDragging = artifactGesture.isDragging();
        artifactGesture.move(x, y, touchSlop, revision,
                ThorContextIds.HERO_MEETING.equals(contextId)
                        && heroMeetingMode.mode() == ThorHeroMeetingModeState.ARTIFACTS);
        final int destination = artifactAt(x, y, adventureFrame(), adventureBevel());
        final ThorHeroMeetingArtifactGesture.Kind result = artifactGesture.finish(revision,
                ThorContextIds.HERO_MEETING.equals(contextId)
                        && heroMeetingMode.mode() == ThorHeroMeetingModeState.ARTIFACTS, destination);
        if (result == ThorHeroMeetingArtifactGesture.Kind.TAP)
            handleHeroMeetingTap(x, y);
        else if (result == ThorHeroMeetingArtifactGesture.Kind.DROP && !pendingMeetingAction
                && heroMeetingArtifacts.complete()
                && (heroMeetingArtifacts.flags[source] & 3) == 1
                && (heroMeetingArtifacts.flags[destination] & 2) == 0
                && isActionEnabled(ThorActionIds.HERO_MEETING_TRANSFER_ARTIFACT))
        {
            final int pair = ThorHeroMeetingArtifactPair.encode(source, destination);
            if (pair != ThorHeroMeetingArtifactPair.INVALID)
            {
                NativeMethods.submitThorAction(revision, ThorActionIds.HERO_MEETING_TRANSFER_ARTIFACT, pair);
                pendingMeetingAction = true;
                performClick();
            }
        }
        if (wasDragging || result != ThorHeroMeetingArtifactGesture.Kind.TAP)
        {
            refresh();
            invalidate();
        }
    }

    private void beginHeroMeetingGesture(final float x, final float y)
    {
        if (heroMeetingMode.mode() != ThorHeroMeetingModeState.ARMY)
            return;
        if (heroMeetingSplit.stage() != ThorHeroMeetingSplitState.Stage.NONE)
            return;
        if (pendingMeetingAction || !heroMeetingArmies.complete() || !heroMeetingArmies.locallyControllable)
            return;
        final int sourceKey = heroMeetingSlotAt(x, y, adventureFrame(), adventureBevel());
        final boolean movable = sourceKey >= 0 && (heroMeetingArmies.flags[sourceKey] & 1) != 0
                && isActionEnabled(ThorActionIds.HERO_MEETING_TRANSFER_STACK);
        if (heroMeetingGesture.begin(revision, sourceKey, movable, x, y)
                && heroMeetingArmies.counts[sourceKey] >= 2
                && isActionEnabled(ThorActionIds.HERO_MEETING_SPLIT_STACK))
            postDelayed(heroMeetingLongPress, ViewConfiguration.getLongPressTimeout());
    }

    private void updateHeroMeetingGesture(final float x, final float y)
    {
        final boolean wasDragging = heroMeetingGesture.isDragging();
        final boolean dragging = heroMeetingGesture.move(x, y, touchSlop, revision,
                ThorContextIds.HERO_MEETING.equals(contextId));
        if (dragging)
        {
            removeCallbacks(heroMeetingLongPress);
            selectedMeetingSlot = heroMeetingGesture.sourceKey();
        }
        else if (wasDragging)
            selectedMeetingSlot = -1;
        if (dragging || wasDragging)
            invalidate();
    }

    private void finishHeroMeetingGesture(final float x, final float y)
    {
        removeCallbacks(heroMeetingLongPress);
        if (!heroMeetingGesture.isArmed())
        {
            handleHeroMeetingTap(x, y);
            return;
        }

        heroMeetingGesture.move(x, y, touchSlop, revision,
                ThorContextIds.HERO_MEETING.equals(contextId));
        if (!heroMeetingGesture.isArmed())
        {
            clearHeroMeetingSelection();
            return;
        }

        final int destinationKey = heroMeetingSlotAt(x, y, adventureFrame(), adventureBevel());
        final ThorHeroMeetingGesture.Result result = heroMeetingGesture.finish(revision,
                ThorContextIds.HERO_MEETING.equals(contextId), destinationKey);
        selectedMeetingSlot = -1;
        if (result.kind == ThorHeroMeetingGesture.Kind.TAP)
        {
            submitHeroMeetingQuickTap(result.sourceKey);
            return;
        }
        if (result.kind == ThorHeroMeetingGesture.Kind.LONG_PRESS)
        {
            selectedMeetingSlot = result.sourceKey;
            refresh();
            invalidate();
            return;
        }
        if (result.kind == ThorHeroMeetingGesture.Kind.DROP && !pendingMeetingAction
                && isActionEnabled(ThorActionIds.HERO_MEETING_TRANSFER_STACK))
        {
            final int encodedPair = ThorHeroMeetingTransferPair.encode(result.sourceKey, result.destinationKey);
            if (encodedPair != ThorHeroMeetingTransferPair.INVALID)
            {
                NativeMethods.submitThorAction(revision, ThorActionIds.HERO_MEETING_TRANSFER_STACK, encodedPair);
                pendingMeetingAction = true;
                performClick();
            }
        }
        selectedMeetingSlot = -1;
        refresh();
        invalidate();
    }

    private void cancelHeroMeetingGesture()
    {
        selectedArtifact = -1;
        artifactGesture.cancel();
        removeCallbacks(heroMeetingLongPress);
        heroMeetingGesture.cancel();
        heroMeetingSplit.cancel();
        selectedMeetingSlot = -1;
        if (heroMeetingTouchSequence)
            heroMeetingTouchCancelled = true;
        invalidate();
    }

    private void clearHeroMeetingSelection()
    {
        artifactGesture.cancel();
        removeCallbacks(heroMeetingLongPress);
        heroMeetingGesture.cancel();
        selectedMeetingSlot = -1;
        invalidate();
    }

    private void drawHeroMeetingDragFeedback(final Canvas canvas, final RectF frame,
                                              final float bevel, final float density)
    {
        if (!heroMeetingGesture.isDragging())
            return;

        final int destinationKey = heroMeetingSlotAt(heroMeetingGesture.pointerX(), heroMeetingGesture.pointerY(), frame, bevel);
        if (ThorHeroMeetingTransferPair.encode(heroMeetingGesture.sourceKey(), destinationKey)
                != ThorHeroMeetingTransferPair.INVALID)
        {
            final RectF target = heroMeetingSlotBounds(destinationKey, frame, bevel);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(3f * density, bevel * 0.8f));
            paint.setColor(GOLD);
            canvas.drawRoundRect(target, bevel, bevel, paint);
        }

        final int sourceKey = heroMeetingGesture.sourceKey();
        final String label = heroMeetingArmies.creatureNames[sourceKey] + " × " + heroMeetingArmies.counts[sourceKey];
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(PARCHMENT_DARK);
        paint.setTextSize(22f * density);
        final float width = Math.min(frame.width() * 0.58f, paint.measureText(label) + bevel * 4f);
        final float height = Math.max(38f * density, bevel * 3f);
        final float centerX = Math.max(frame.left + width * 0.5f,
                Math.min(frame.right - width * 0.5f, heroMeetingGesture.pointerX()));
        final float centerY = Math.max(frame.top + height * 0.5f,
                Math.min(frame.bottom - height * 0.5f, heroMeetingGesture.pointerY() - height));
        final RectF cursor = new RectF(centerX - width * 0.5f, centerY - height * 0.5f,
                centerX + width * 0.5f, centerY + height * 0.5f);
        canvas.drawRoundRect(cursor, bevel, bevel, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(2f, bevel * 0.45f));
        paint.setColor(GOLD);
        canvas.drawRoundRect(cursor, bevel, bevel, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(TEXT);
        drawEllipsizedText(canvas, label, centerX, centerY, width * 0.86f, Math.min(22f * density, height * 0.56f));
    }

    private void handleHeroMeetingTap(final float x, final float y)
    {
        final RectF frame = adventureFrame();
        final float bevel = adventureBevel();
        for (int mode = 0; mode < 2; ++mode)
        {
            if (heroMeetingModeBounds(mode, frame, bevel).contains(x, y))
            {
                heroMeetingMode.select(revision, mode);
                artifactPage = 0;
                selectedArtifact = -1;
                clearHeroMeetingSelection();
                performClick();
                refresh();
                invalidate();
                return;
            }
        }
        if (heroMeetingMode.mode() == ThorHeroMeetingModeState.ARTIFACTS)
        {
            if (heroMeetingArtifacts.complete() && !pendingMeetingAction)
            {
                for (int side = 0; side < 2; ++side)
                {
                    for (int row = 0; row < 6; ++row)
                    {
                        final int local = artifactPage * 6 + row;
                        if (local >= ThorHeroMeetingArtifacts.PER_HERO
                                || !heroMeetingArtifactBounds(side, row, frame, bevel).contains(x, y))
                            continue;
                        final int index = side * ThorHeroMeetingArtifacts.PER_HERO + local;
                        if ((heroMeetingArtifacts.flags[index] & 2) != 0)
                            return;
                        if (selectedArtifact < 0)
                        {
                            if ((heroMeetingArtifacts.flags[index] & 1) == 0
                                    || !isActionEnabled(ThorActionIds.HERO_MEETING_TRANSFER_ARTIFACT))
                                return;
                            selectedArtifact = index;
                        }
                        else if (selectedArtifact == index)
                            selectedArtifact = -1;
                        else
                        {
                            final int pair = ThorHeroMeetingArtifactPair.encode(selectedArtifact, index);
                            selectedArtifact = -1;
                            if (pair != ThorHeroMeetingArtifactPair.INVALID
                                    && isActionEnabled(ThorActionIds.HERO_MEETING_TRANSFER_ARTIFACT))
                            {
                                NativeMethods.submitThorAction(revision,
                                        ThorActionIds.HERO_MEETING_TRANSFER_ARTIFACT, pair);
                                pendingMeetingAction = true;
                            }
                        }
                        performClick();
                        invalidate();
                        return;
                    }
                }
            }
            for (int index = 0; index < 2; ++index)
            {
                if (!heroMeetingArtifactPageBounds(index, frame, bevel).contains(x, y))
                    continue;
                if (index == 0)
                    artifactPage = Math.max(0, artifactPage - 1);
                else
                    artifactPage = Math.min(3, artifactPage + 1);
                performClick();
                refresh();
                invalidate();
                return;
            }
            for (int index = 0; index < 3; ++index)
            {
                final int action = ThorActionIds.artifactBulkActionForButton(index);
                if (!heroMeetingArtifactActionBounds(index, frame, bevel).contains(x, y)
                        || pendingMeetingAction || !isActionEnabled(action))
                    continue;
                NativeMethods.submitThorAction(revision, action, ThorActionIds.NO_TARGET);
                pendingMeetingAction = true;
                selectedArtifact = -1;
                performClick();
                refresh();
                invalidate();
                return;
            }
            return;
        }
        if (!heroMeetingArmies.complete() || pendingMeetingAction)
            return;
        if (heroMeetingSplit.stage() == ThorHeroMeetingSplitState.Stage.AMOUNT)
        {
            handleHeroMeetingSplitEditorTap(x, y, frame, bevel);
            return;
        }
        for (int index = 0; index < ThorHeroMeetingArmies.SLOT_COUNT; ++index)
        {
            if (!heroMeetingSlotBounds(index, frame, bevel).contains(x, y))
                continue;
            if (heroMeetingSplit.stage() == ThorHeroMeetingSplitState.Stage.DESTINATION)
            {
                if (index == heroMeetingSplit.sourceKey())
                {
                    heroMeetingSplit.cancel();
                    selectedMeetingSlot = -1;
                    refresh();
                    invalidate();
                    return;
                }
                if (heroMeetingSplit.selectDestination(revision, index, heroMeetingArmies))
                    selectedMeetingSlot = heroMeetingSplit.sourceKey();
                refresh();
                invalidate();
                return;
            }
            submitHeroMeetingQuickTap(index);
            return;
        }
        if (heroMeetingSplit.stage() == ThorHeroMeetingSplitState.Stage.DESTINATION)
            return;
        final int[] actions = {ThorActionIds.HERO_MEETING_ARMY_LEFT_TO_RIGHT,
                ThorActionIds.HERO_MEETING_SWAP_ARMIES, ThorActionIds.HERO_MEETING_ARMY_RIGHT_TO_LEFT};
        for (int index = 0; index < actions.length; ++index)
        {
            if (heroMeetingActionBounds(index, frame, bevel).contains(x, y) && isActionEnabled(actions[index]))
            {
                NativeMethods.submitThorAction(revision, actions[index], ThorActionIds.NO_TARGET);
                pendingMeetingAction = true;
                selectedMeetingSlot = -1;
                performClick();
                invalidate();
                return;
            }
        }
    }

    private void handleHeroMeetingSplitEditorTap(final float x, final float y, final RectF frame,
                                                  final float bevel)
    {
        for (int index = 0; index < 6; ++index)
        {
            if (!heroMeetingSplitButtonBounds(index, frame, bevel).contains(x, y))
                continue;
            if (index < 4)
            {
                heroMeetingSplit.adjust(new int[]{-10, -1, 1, 10}[index]);
            }
            else if (index == 4)
            {
                heroMeetingSplit.cancel();
                selectedMeetingSlot = -1;
            }
            else if (isActionEnabled(ThorActionIds.HERO_MEETING_SPLIT_STACK))
            {
                final int source = heroMeetingSplit.sourceKey();
                final int destination = heroMeetingSplit.destinationKey();
                NativeMethods.submitThorHeroMeetingSplit(revision, heroMeetingArmies.armyIds[source / 7], source % 7,
                        heroMeetingArmies.armyIds[destination / 7], destination % 7, heroMeetingSplit.amount());
                pendingMeetingAction = true;
                heroMeetingSplit.cancel();
                selectedMeetingSlot = -1;
            }
            performClick();
            refresh();
            invalidate();
            return;
        }
    }

    private void submitHeroMeetingQuickTap(final int sourceKey)
    {
        if (pendingMeetingAction || !heroMeetingArmies.complete() || !heroMeetingArmies.locallyControllable
                || sourceKey < 0 || sourceKey >= ThorHeroMeetingArmies.SLOT_COUNT
                || (heroMeetingArmies.flags[sourceKey] & 1) == 0
                || !isActionEnabled(ThorActionIds.HERO_MEETING_MOVE_STACK))
            return;
        NativeMethods.submitThorAction(revision, ThorActionIds.HERO_MEETING_MOVE_STACK, sourceKey);
        pendingMeetingAction = true;
        performClick();
        refresh();
        invalidate();
    }

    boolean touch(MotionEvent event)
    {
    final int action = event.getActionMasked();
        if (heroMeetingTouchSequence && action == MotionEvent.ACTION_DOWN)
        {
            cancelHeroMeetingGesture();
            heroMeetingTouchSequence = false;
            heroMeetingTouchCancelled = false;
        }
        if (heroMeetingTouchSequence)
        {
            if (!ThorContextIds.HERO_MEETING.equals(contextId) || revision != heroMeetingTouchRevision)
                cancelHeroMeetingGesture();
            if (heroMeetingMode.mode() == ThorHeroMeetingModeState.ARTIFACTS
                    && (event.getPointerCount() != 1 || event.getPointerId(0) != heroMeetingPointerId))
                cancelHeroMeetingGesture();
            if (action == MotionEvent.ACTION_MOVE)
            {
                if (!heroMeetingTouchCancelled)
                {
                    if (heroMeetingMode.mode() == ThorHeroMeetingModeState.ARTIFACTS)
                    {
                        if (!artifactGesture.isArmed())
                        {
                            final float dx = event.getX() - artifactTouchDownX;
                            final float dy = event.getY() - artifactTouchDownY;
                            artifactTouchMovedWithoutSource |= dx * dx + dy * dy > touchSlop * touchSlop;
                        }
                        updateArtifactGesture(event.getX(), event.getY());
                    }
                    else
                        updateHeroMeetingGesture(event.getX(), event.getY());
                }
                return true;
            }
            if (action == MotionEvent.ACTION_UP)
            {
                if (!heroMeetingTouchCancelled)
                {
                    if (heroMeetingMode.mode() == ThorHeroMeetingModeState.ARTIFACTS)
                        finishArtifactGesture(event.getX(), event.getY());
                    else
                        finishHeroMeetingGesture(event.getX(), event.getY());
                }
                else
                    clearHeroMeetingSelection();
                heroMeetingTouchSequence = false;
                heroMeetingTouchCancelled = false;
                heroMeetingPointerId = -1;
                artifactTouchMovedWithoutSource = false;
                return true;
            }
            if (action == MotionEvent.ACTION_CANCEL)
            {
                cancelHeroMeetingGesture();
                heroMeetingTouchSequence = false;
                heroMeetingTouchCancelled = false;
                heroMeetingPointerId = -1;
                artifactTouchMovedWithoutSource = false;
                return true;
            }
            if (action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_POINTER_UP)
            {
                cancelHeroMeetingGesture();
                return true;
            }
            return true;
        }

        if (ThorContextIds.HERO_MEETING.equals(contextId))
        {
            if (action == MotionEvent.ACTION_DOWN)
            {
                heroMeetingTouchSequence = true;
                heroMeetingTouchCancelled = false;
                heroMeetingTouchRevision = revision;
                heroMeetingPointerId = event.getPointerId(0);
                if (heroMeetingMode.mode() == ThorHeroMeetingModeState.ARTIFACTS)
                {
                    artifactTouchDownX = event.getX();
                    artifactTouchDownY = event.getY();
                    artifactTouchMovedWithoutSource = false;
                    beginArtifactGesture(event.getX(), event.getY());
                }
                else
                    beginHeroMeetingGesture(event.getX(), event.getY());
            }
            return true;
        }

    return false;
    }
    String description(String title, String status)
    {

            if (heroMeetingMode.mode() == ThorHeroMeetingModeState.ARTIFACTS)
            {
                final StringBuilder description = new StringBuilder(title).append(". ")
                        .append(getContext().getString(R.string.thor_meeting_artifacts));
                if (heroMeetingArtifacts.complete())
                    for (int side = 0; side < 2; ++side)
                    {
                        description.append(". ").append(heroMeetingArtifacts.heroNames[side]);
                        for (int row = 0; row < 6; ++row)
                        {
                            final int local = artifactPage * 6 + row;
                            if (local >= ThorHeroMeetingArtifacts.PER_HERO)
                                break;
                            final int index = side * ThorHeroMeetingArtifacts.PER_HERO + local;
                            description.append(". ").append((heroMeetingArtifacts.flags[index] & 1) != 0
                                    ? heroMeetingArtifacts.names[index]
                                    : getContext().getString(R.string.thor_army_empty));
                            if ((heroMeetingArtifacts.flags[index] & 2) != 0)
                                description.append(" ").append(getContext().getString(R.string.thor_artifact_locked));
                        }
                    }
                return description.toString();
            }
            if (heroMeetingSplit.stage() == ThorHeroMeetingSplitState.Stage.DESTINATION)
                return title + ". " + getContext().getString(R.string.thor_split_select_destination);
            if (heroMeetingSplit.stage() == ThorHeroMeetingSplitState.Stage.AMOUNT)
                return title + ". " + getContext().getString(R.string.thor_split_amount) + " "
                        + heroMeetingSplit.amount() + ". "
                        + getContext().getString(R.string.thor_split_minus_ten) + ", "
                        + getContext().getString(R.string.thor_split_minus_one) + ", "
                        + getContext().getString(R.string.thor_split_plus_one) + ", "
                        + getContext().getString(R.string.thor_split_plus_ten) + ", "
                        + getContext().getString(R.string.thor_split_cancel)
                        + ", " + getContext().getString(R.string.thor_split_confirm);
            return title + ". " + status + ". " + getContext().getString(R.string.thor_split_accessibility);
            }
}

