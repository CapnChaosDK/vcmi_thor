package eu.vcmi.vcmi;

import org.junit.Test;

import static org.junit.Assert.*;

public class ThorPresentationLayoutTest
{
    @Test
    public void shellMatchesLegacyPixelsAcrossSizesAndDensities()
    {
        for (final int[] size : new int[][]{{1080, 1240}, {540, 620}, {1240, 1080},
                {1920, 1080}, {720, 1600}, {997, 1231}, {0, 0}, {20, 30}})
            for (final float density : new float[]{0.75f, 1f, 2.30625f, 3.5f})
            {
                final int width = size[0];
                final int height = size[1];
                final float scale = Math.min(width / 1080f, height / 1240f);
                final float margin = Math.max(16f * density, Math.max(Math.min(width, height) * 0.035f,
                        Math.min(width, height) * 0.045f * scale));
                final ThorPresentationLayout layout = new ThorPresentationLayout(width, height, density);
                assertEquals(margin, layout.margin, 0f);
                assertEquals(width - margin, layout.right, 0f);
                assertEquals(height - margin, layout.bottom, 0f);
                assertEquals(Math.max(3f * density, margin * 0.16f), layout.bevel, 0f);
                assertEquals(width - margin * 2f, layout.contentWidth, 0f);
                assertEquals(height - margin * 2f, layout.contentHeight, 0f);
                assertEquals(width > margin * 2f && height > margin * 2f, layout.hasContent());
            }
    }

    @Test
    public void referenceAndDensityFloorHaveKnownDimensions()
    {
        final ThorPresentationLayout reference = new ThorPresentationLayout(1080, 1240, 2.30625f);
        assertEquals(48.6f, reference.margin, 0.00001f);
        assertEquals(7.776f, reference.bevel, 0.00001f);
        final ThorPresentationLayout dense = new ThorPresentationLayout(540, 620, 3f);
        assertEquals(48f, dense.margin, 0f);
        assertEquals(9f, dense.bevel, 0f);
        assertFalse(new ThorPresentationLayout(20, 30, 3f).hasContent());
    }
}
