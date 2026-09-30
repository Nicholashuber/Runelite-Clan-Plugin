package com.corclan.glow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.util.List;
import org.junit.Test;

public class GlowStyleTest
{
	private static final long P = GlowStyle.HOLY_PULSE_MILLIS;

	@Test
	public void breathGoesDimToBrightToDim()
	{
		assertEquals(0.0, GlowStyle.breath(0, P), 1e-9);
		assertEquals(1.0, GlowStyle.breath(P / 2, P), 1e-9);
		assertEquals(0.0, GlowStyle.breath(P, P), 1e-9);
		for (long t = 0; t < 2 * P; t += 37)
		{
			double b = GlowStyle.breath(t, P);
			assertTrue(b >= 0 && b <= 1);
		}
	}

	@Test
	public void holyGlowIsGoldAuraDownToCrispCore()
	{
		for (long t = 0; t < 2 * P; t += 113)
		{
			List<GlowStyle.Layer> layers = GlowStyle.holy(t);
			assertEquals(3, layers.size());
			// drawn outermost first: widths shrink, the edge is hard and fully opaque
			assertTrue(layers.get(0).width > layers.get(1).width && layers.get(1).width > layers.get(2).width);
			assertEquals(0, layers.get(2).feather);
			assertEquals(255, layers.get(2).color.getAlpha());
			for (GlowStyle.Layer layer : layers)
			{
				assertTrue(layer.feather >= 0 && layer.feather <= 4);
				// gold: red channel full, blue channel lowest
				assertEquals(255, layer.color.getRed());
				assertTrue(layer.color.getBlue() <= layer.color.getGreen());
			}
		}
	}

	@Test
	public void auraSwellsAndBrightensWithEachBreath()
	{
		GlowStyle.Layer rest = GlowStyle.holy(0).get(0);
		GlowStyle.Layer peak = GlowStyle.holy(P / 2).get(0);
		assertTrue(peak.width > rest.width);
		assertTrue(peak.color.getAlpha() > rest.color.getAlpha());
		// bright even at rest
		assertTrue(rest.color.getAlpha() >= 100);
	}

	@Test
	public void coreShimmersWhiteToPaleGold()
	{
		Color white = GlowStyle.holy(0).get(2).color;
		Color gold = GlowStyle.holy(GlowStyle.HOLY_SHIMMER_MILLIS / 2).get(2).color;
		assertEquals(new Color(255, 255, 255, 255), white);
		assertTrue(gold.getBlue() < white.getBlue());
	}
}
