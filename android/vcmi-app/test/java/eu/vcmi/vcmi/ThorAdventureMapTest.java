package eu.vcmi.vcmi;

import org.junit.Test;
import static org.junit.Assert.*;

public class ThorAdventureMapTest
{
    private ThorAdventureMap map(final int width, final int height, final int level, final int[] markers)
    {
        return ThorAdventureMap.copyOf(ThorAdventureMap.EMPTY, 1, width, height, level, 2,
                0, 0, width, height, new byte[width * height * 3], markers);
    }

    @Test
    public void payloadBoundsRejectInvalidDimensionsLevelsBytesViewportAndMarkers()
    {
        assertFalse(ThorAdventureMap.dimensionsValid(0, 20));
        assertFalse(ThorAdventureMap.dimensionsValid(513, 20));
        assertFalse(ThorAdventureMap.dimensionsValid(Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertTrue(map(512, 512, 1, new int[0]).valid());
        assertFalse(map(20, 10, 2, new int[0]).valid());
        assertFalse(map(20, 10, 0, new int[]{13, 1, 20, 0, 0, 0}).valid());
        assertFalse(map(20, 10, 0, new int[]{13, 1, 0, 10, 0, 0}).valid());
        assertFalse(map(20, 10, 0, new int[]{13, 1, 0, 0, 2, 0}).valid());
        assertFalse(map(20, 10, 0, new int[]{13, 1, 0, 0, 0, 2}).valid());
        assertFalse(map(20, 10, 0, new int[]{58, 1, 0, 0, 0, 0}).valid());
        assertFalse(map(20, 10, 0, new int[]{13, -1, 0, 0, 0, 0}).valid());
        assertFalse(map(20, 10, 0, new int[]{13}).valid());
        assertFalse(map(20, 10, 0, null).valid());
        assertFalse(map(20, 10, 0, new int[]{13, 1, 0, 0, 0, 0, 13, 1, 2, 2, 0, 0}).valid());
        assertFalse(ThorAdventureMap.copyOf(ThorAdventureMap.EMPTY, 1, 2, 2, 0, 1,
                0, 0, 2, 2, new byte[11], new int[0]).valid());
        assertFalse(ThorAdventureMap.copyOf(ThorAdventureMap.EMPTY, 1, 2, 2, 0, 1,
                1, 0, 2, 2, new byte[12], new int[0]).valid());
        assertFalse(ThorAdventureMap.copyOf(ThorAdventureMap.EMPTY, 1, 2, 2, 0, 1,
                0, 0, Integer.MAX_VALUE, 2, new byte[12], new int[0]).valid());
    }

    @Test
    public void markerCountLimitsArePerRosterAndAggregate()
    {
        final int[] heroes = new int[9 * 6];
        for (int i = 0; i < 9; ++i)
        {
            heroes[i * 6] = 13;
            heroes[i * 6 + 1] = i;
        }
        assertFalse(map(4, 4, 0, heroes).valid());
        final int[] towns = new int[65 * 6];
        for (int i = 0; i < 65; ++i)
        {
            towns[i * 6] = 14;
            towns[i * 6 + 1] = i;
        }
        assertFalse(map(4, 4, 0, towns).valid());
        assertFalse(map(4, 4, 0, new int[73 * 6]).valid());
    }

    @Test
    public void viewportOnlyReuseRequiresExactContentIdentityAndCopiesExternalData()
    {
        final byte[] bytes = new byte[24];
        final int[] markers = {13, 42, 1, 1, 0, 1};
        final ThorAdventureMap first = ThorAdventureMap.copyOf(ThorAdventureMap.EMPTY, 7, 4, 2, 0, 2,
                0, 0, 4, 2, bytes, markers);
        bytes[0] = 45;
        markers[1] = 90;
        assertEquals(0, first.rgb[0]);
        assertEquals(42, first.markers[1]);
        final ThorAdventureMap reused = ThorAdventureMap.copyOf(first, 7, 4, 2, 0, 2,
                1, 0, 2, 1, null, new int[0]);
        assertTrue(reused.valid());
        assertSame(first.rgb, reused.rgb);
        assertEquals(1, reused.viewportX);
        assertFalse(ThorAdventureMap.copyOf(first, 8, 4, 2, 0, 2, 0, 0, 4, 2, null, new int[0]).valid());
        assertFalse(ThorAdventureMap.copyOf(first, 7, 2, 4, 0, 2, 0, 0, 2, 4, null, new int[0]).valid());
        assertFalse(ThorAdventureMap.copyOf(first, 7, 4, 2, 1, 2, 0, 0, 4, 2, null, new int[0]).valid());
        assertFalse(ThorAdventureMap.copyOf(ThorAdventureMap.EMPTY, 7, 4, 2, 0, 2, 0, 0, 4, 2, null, new int[0]).valid());
    }

    @Test
    public void rectangularFitLetterboxesAndRoundTripsExactTileCenters()
    {
        for (final int[] size : new int[][]{{120, 60}, {60, 120}, {512, 512}, {1, 1}})
        {
            final ThorMapTransform fit = new ThorMapTransform(size[0], size[1], 10, 20, 610, 620,
                    1, size[0] / 2f, size[1] / 2f);
            assertEquals(fit.screenX(1) - fit.screenX(0), fit.screenY(1) - fit.screenY(0), 0.0001f);
            for (int y = 0; y < size[1]; ++y)
                for (int x = 0; x < size[0]; ++x)
                    assertEquals(y * 512 + x, fit.tileAt(fit.screenX(x + 0.5f), fit.screenY(y + 0.5f)));
            assertEquals(-1, fit.tileAt(fit.screenX(size[0]), fit.screenY(0.5f)));
            assertEquals(-1, fit.tileAt(9, 30));
        }
        final ThorMapTransform wide = new ThorMapTransform(120, 60, 0, 0, 600, 600, 1, 60, 30);
        assertEquals(-1, wide.tileAt(300, 100));
        assertEquals(150, wide.screenY(0), 0.001f);
        assertEquals(-1, wide.tileAt(Float.NaN, 250));
        assertEquals(-1, new ThorMapTransform(0, 0, 0, 0, 1, 1, 1, 0, 0).tileAt(0, 0));
    }

    @Test
    public void detailPanClampsWithoutChangingNativeViewportAndStateRestores()
    {
        final ThorAdventureMap map = map(120, 60, 0, new int[0]);
        final ThorMapViewState state = new ThorMapViewState();
        state.update(map);
        state.setZoom(4);
        state.pan = true;
        final ThorMapTransform start = new ThorMapTransform(120, 60, 0, 0, 600, 600, 4, state.centerX, state.centerY);
        state.panFrom(start, 100000, -100000);
        assertEquals(15, state.centerX, 0.001f);
        assertEquals(45, state.centerY, 0.001f);
        assertEquals(0, map.viewportX);
        final ThorMapViewState restored = state.copy();
        assertEquals(state.modeKey(), restored.modeKey());
        assertEquals(state.centerX, restored.centerX, 0);
        restored.setZoom(1);
        assertFalse(restored.pan);
        restored.setZoom(3);
        assertEquals(1, restored.zoom);
        state.update(map(120, 60, 1, new int[0]));
        assertEquals(60, state.centerX, 0);
        assertEquals(30, state.centerY, 0);
        assertEquals(4, state.zoom);
    }

    @Test
    public void detailRasterViewportMarkersAndInputShareTransform()
    {
        final ThorMapTransform detail = new ThorMapTransform(120, 60, 0, 0, 600, 400, 2, 1000, -1000);
        assertEquals(120, detail.screenX(120) / detail.scale + (-detail.originX / detail.scale), 0.001f);
        final float x = detail.screenX(90.5f);
        final float y = detail.screenY(20.5f);
        assertEquals(20 * 512 + 90, detail.tileAt(x, y));
        assertEquals(100, detail.screenX(100) - detail.screenX(90), 0.001f);
    }

    @Test
    public void markerPrecedenceUniqueSelectedAndLevelBound()
    {
        final ThorMapTransform fit = new ThorMapTransform(20, 10, 0, 0, 400, 200, 1, 10, 5);
        final ThorAdventureMap unique = map(20, 10, 0, new int[]{13, 1, 5, 5, 0, 0, 14, 2, 7, 7, 1, 0});
        assertEquals(0, unique.markerAt(fit, fit.screenX(5.5f), fit.screenY(5.5f), 10));
        assertEquals(-1, unique.markerAt(fit, fit.screenX(7.5f), fit.screenY(7.5f), 10));
        final ThorAdventureMap overlap = map(20, 10, 0, new int[]{13, 1, 5, 5, 0, 0, 14, 2, 5, 5, 0, 0});
        assertEquals(-1, overlap.markerAt(fit, 110, 110, 10));
        final ThorAdventureMap selected = map(20, 10, 0, new int[]{13, 1, 5, 5, 0, 0, 14, 2, 5, 5, 0, 1});
        assertEquals(6, selected.markerAt(fit, 110, 110, 10));
        final ThorAdventureMap both = map(20, 10, 0, new int[]{13, 1, 5, 5, 0, 1, 14, 2, 5, 5, 0, 1});
        assertEquals(-1, both.markerAt(fit, 110, 110, 10));
    }

    @Test
    public void gestureBindsRevisionSessionPointerLevelModeAndCancelsOutside()
    {
        final ThorMapTransform fit = new ThorMapTransform(20, 10, 0, 0, 400, 200, 1, 10, 5);
        final ThorMapGesture gesture = new ThorMapGesture();
        for (int invalid = 0; invalid < 7; ++invalid)
        {
            gesture.begin(7, 8, 3, 0, 2, -1, 100, 100, fit);
            assertTrue(gesture.active());
            assertFalse(gesture.matches(invalid == 0 ? 9 : 7, invalid == 1 ? 9 : 8,
                    invalid == 2 ? 4 : 3, invalid == 3 ? 2 : 1, invalid == 4 ? 1 : 0,
                    invalid == 5 ? 4 : 2, invalid != 6));
            assertFalse(gesture.active());
            assertFalse(gesture.matches(7, 8, 3, 1, 0, 2, true));
        }
        gesture.begin(7, 8, 3, 0, 2, -1, 100, 100, fit);
        gesture.move(105, 105, 10);
        assertFalse(gesture.moved());
        gesture.move(200, 100, 10);
        assertTrue(gesture.moved());
        assertTrue(gesture.matches(7, 8, 3, 1, 0, 2, true));
        gesture.move(401, 100, 10);
        assertFalse(gesture.active());
        gesture.begin(7, 8, 3, 0, 2, -1, -1, 100, fit);
        assertFalse(gesture.active());
        gesture.begin(7, 8, 3, 0, 2, 0, 100, 100, fit);
        gesture.cancel();
        assertFalse(gesture.active());
    }

    @Test
    public void fixedControlsAreDistinctBoundedAndHaveAccessibleLocalizedLabels()
    {
        for (int control = 0; control < 7; ++control)
        {
            assertTrue(ThorMapControls.label(control) != 0);
            final float[] box = ThorMapControls.bounds(control, 900, 1100);
            assertTrue(box[0] >= 0 && box[2] <= 900 && box[1] > 0 && box[3] < 1100 * 0.54f);
            assertTrue(box[0] < box[2] && box[1] < box[3]);
            for (int other = 0; other < control; ++other)
            {
                final float[] previous = ThorMapControls.bounds(other, 900, 1100);
                assertTrue(box[0] >= previous[2] || box[1] >= previous[3]);
            }
        }
        assertEquals(R.string.thor_map_navigate, ThorMapControls.label(3));
        assertEquals(R.string.thor_map_pan, ThorMapControls.label(4));
    }

    private ThorMapGesture.Release release(final ThorMapGesture gesture, final ThorAdventureMap map,
            final boolean pan, final float x, final float y, final int control, final long mask)
    {
        return gesture.release(7, 8, 3, 1, map.level, pan ? 5 : 4, true,
                x, y, control, map, pan, mask, 10, 10);
    }

    @Test
    public void releaseSubmitsOneCenterRequestAndDragNeverSelectsEndMarker()
    {
        final ThorAdventureMap map = map(20, 10, 0, new int[]{13, 42, 10, 5, 0, 1});
        final ThorMapTransform fit = new ThorMapTransform(20, 10, 0, 0, 400, 200, 1, 10, 5);
        final ThorMapGesture gesture = new ThorMapGesture();
        gesture.begin(7, 8, 3, 0, 4, -1, 10, 10, fit);
        gesture.move(210, 110, 10);
        final long mask = ThorActionIds.maskFor(58) | ThorActionIds.maskFor(13);
        final ThorMapGesture.Release first = release(gesture, map, false, 210, 110, -1, mask);
        assertEquals(58, first.action);
        assertEquals(5 * 512 + 10, first.target);
        assertEquals(-1, first.localControl);
        assertEquals(0, release(gesture, map, false, 210, 110, -1, mask).action);
        assertFalse(gesture.active());
    }

    @Test
    public void panDragIsLocalButPanAndNavigateTapsStillSelectOrCenter()
    {
        final ThorMapTransform fit = new ThorMapTransform(20, 10, 0, 0, 400, 200, 1, 10, 5);
        for (final int markerAction : new int[]{13, 14})
        {
            final ThorAdventureMap map = map(20, 10, 0, new int[]{markerAction, 42, 10, 5, 0, 1});
            final long mask = ThorActionIds.maskFor(58) | ThorActionIds.maskFor(markerAction);
            for (final boolean pan : new boolean[]{false, true})
            {
                final ThorMapGesture gesture = new ThorMapGesture();
                gesture.begin(7, 8, 3, 0, pan ? 5 : 4, -1, 210, 110, fit);
                // Jitter within slop still uses the unchanged raster transform and remains a tap.
                gesture.move(211, 111, 10);
                assertFalse(gesture.moved());
                final ThorMapGesture.Release selected = release(gesture, map, pan, 211, 111, -1, mask);
                assertEquals(markerAction, selected.action);
                assertEquals(42, selected.target);
                gesture.begin(7, 8, 3, 0, pan ? 5 : 4, -1, 50, 50, fit);
                assertEquals(58, release(gesture, map, pan, 50, 50, -1, mask).action);
            }
            final ThorMapGesture gesture = new ThorMapGesture();
            gesture.begin(7, 8, 3, 0, 5, -1, 10, 10, fit);
            gesture.move(210, 110, 10);
            assertEquals(0, release(gesture, map, true, 210, 110, -1, mask).action);
        }
    }

    @Test
    public void releaseControlsAreLocalExceptValidatedMapLevelChange()
    {
        final ThorAdventureMap map = map(20, 10, 0, new int[0]);
        final ThorMapTransform fit = new ThorMapTransform(20, 10, 0, 0, 400, 200, 1, 10, 5);
        for (final int control : new int[]{0, 1, 2, 3, 4, 10, 11, 12, 13})
        {
            final ThorMapGesture gesture = new ThorMapGesture();
            gesture.begin(7, 8, 3, 0, 4, control, 100, 100, fit);
            final ThorMapGesture.Release result = release(gesture, map, false, 100, 100, control, -1L);
            assertEquals(control, result.localControl);
            assertEquals(0, result.action);
        }
        final ThorMapGesture gesture = new ThorMapGesture();
        gesture.begin(7, 8, 3, 0, 4, 6, 100, 100, fit);
        final ThorMapGesture.Release level = release(gesture, map, false, 100, 100, 6, ThorActionIds.maskFor(59));
        assertEquals(59, level.action);
        assertEquals(1, level.target);
        assertEquals(0, release(gesture, map, false, 100, 100, 6, ThorActionIds.maskFor(59)).action);
        for (final int control : new int[]{5, 6})
        {
            gesture.begin(7, 8, 3, 0, 4, control, 100, 100, fit);
            assertEquals(0, release(gesture, map, false, 100, 100, control, 0).action);
        }
        gesture.begin(7, 8, 3, 0, 4, 5, 100, 100, fit);
        assertEquals(0, release(gesture, map, false, 100, 100, 5, -1L).action);
        gesture.begin(7, 8, 3, 0, 4, 2, 100, 100, fit);
        assertEquals(-1, release(gesture, map, false, 100, 100, 1, -1L).localControl);
    }

    @Test
    public void releaseRejectsStaleCancelledOutsideAndUnavailableRequests()
    {
        final ThorAdventureMap map = map(20, 10, 0, new int[]{13, 42, 10, 5, 0, 1});
        final ThorMapTransform fit = new ThorMapTransform(20, 10, 0, 0, 400, 200, 1, 10, 5);
        final ThorMapGesture gesture = new ThorMapGesture();
        gesture.begin(7, 8, 3, 0, 4, -1, 210, 110, fit);
        assertEquals(58, release(gesture, map, false, 210, 110, -1, ThorActionIds.maskFor(58)).action);
        gesture.begin(7, 8, 3, 0, 4, -1, 210, 110, fit);
        assertEquals(0, release(gesture, map, false, 210, 110, -1, 0).action);
        gesture.begin(7, 8, 3, 0, 4, -1, 210, 110, fit);
        assertEquals(0, release(gesture, map, false, 401, 110, -1, -1L).action);
        gesture.begin(7, 8, 3, 0, 4, -1, 210, 110, fit);
        gesture.cancel();
        assertEquals(0, release(gesture, map, false, 210, 110, -1, -1L).action);
        gesture.begin(7, 8, 3, 0, 4, -1, 210, 110, fit);
        assertEquals(0, gesture.release(9, 8, 3, 1, 0, 4, true, 210, 110, -1, map, false, -1L, 10, 10).action);
        assertEquals(0, release(gesture, map, false, 210, 110, -1, -1L).action);
    }

    @Test
    public void actionsRemainWithinMaskBudgetAndTileTargetsAreBounded()
    {
        assertEquals(58, ThorActionIds.ADVENTURE_CENTER_VIEW);
        assertEquals(59, ThorActionIds.ADVENTURE_SET_MAP_LEVEL);
        assertEquals(59, ThorActionIds.MAX_ACTION_ID);
        assertFalse(ThorHapticState.isEligible(ThorContextIds.ADVENTURE_MAP, 58));
        assertFalse(ThorHapticState.isEligible(ThorContextIds.ADVENTURE_MAP, 59));
        assertFalse(ThorHapticState.isEligible(ThorContextIds.ADVENTURE_MAP, 13));
        assertFalse(ThorHapticState.isEligible(ThorContextIds.ADVENTURE_MAP, 14));
        assertEquals(1L << 57, ThorActionIds.maskFor(58));
        assertEquals(1L << 58, ThorActionIds.maskFor(59));
        assertEquals(0, ThorActionIds.maskFor(60));
        assertEquals(524287, ThorAdventureMap.encodeTarget(511, 511, 1));
        assertEquals(-1, ThorAdventureMap.encodeTarget(512, 0, 0));
        assertEquals(-1, ThorAdventureMap.encodeTarget(0, -1, 0));
        assertEquals(-1, ThorAdventureMap.encodeTarget(0, 0, 2));
    }
}
