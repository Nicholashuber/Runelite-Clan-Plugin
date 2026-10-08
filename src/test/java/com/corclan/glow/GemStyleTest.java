package com.corclan.glow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.util.List;
import org.junit.Test;

public class GemStyleTest
{
	@Test
	public void everyGemIsAHazeDownToACrispOpaqueEdge()
	{
		for (GemGlow gem : GemGlow.values())
		{
			for (long t = 0; t < 7000; t += 97)
			{
				List<GlowStyle.Layer> layers = GemStyle.layers(gem, t);
				assertTrue(gem + "", layers.size() >= 2);
				for (int i = 1; i < layers.size(); i++)
				{
					assertTrue(gem + "", layers.get(i - 1).width > layers.get(i).width);
				}
				GlowStyle.Layer edge = layers.get(layers.size() - 1);
				assertEquals(0, edge.feather);
				assertEquals(255, edge.color.getAlpha());
				for (GlowStyle.Layer layer : layers)
				{
					assertTrue(layer.feather >= 0 && layer.feather <= 4);
				}
			}
		}
	}

	@Test
	public void hazesMatchTheirGemsColour()
	{
		Color sapphire = haze(GemGlow.SAPPHIRE);
		assertTrue(sapphire.getBlue() > sapphire.getRed() && sapphire.getBlue() > sapphire.getGreen());
		Color emerald = haze(GemGlow.EMERALD);
		assertTrue(emerald.getGreen() > emerald.getRed() && emerald.getGreen() > emerald.getBlue());
		Color ruby = haze(GemGlow.RUBY);
		assertTrue(ruby.getRed() > ruby.getGreen() && ruby.getRed() > ruby.getBlue());
		Color dragonstone = haze(GemGlow.DRAGONSTONE);
		assertTrue(dragonstone.getBlue() > dragonstone.getGreen() && dragonstone.getRed() > dragonstone.getGreen());
		Color onyx = haze(GemGlow.ONYX);
		assertTrue(onyx.getRed() + onyx.getGreen() + onyx.getBlue() < 60);
		Color zenyte = haze(GemGlow.ZENYTE);
		assertTrue(zenyte.getRed() == 255 && zenyte.getGreen() > zenyte.getBlue());
		Color diamond = haze(GemGlow.DIAMOND);
		assertTrue(diamond.getRed() > 150 && diamond.getGreen() > 200 && diamond.getBlue() == 255);
	}

	@Test
	public void eachGemUpGlowsWiderAndBrighter()
	{
		GemGlow[] lowToHigh = {GemGlow.OPAL, GemGlow.SAPPHIRE, GemGlow.EMERALD, GemGlow.RUBY,
			GemGlow.DIAMOND, GemGlow.DRAGONSTONE, GemGlow.ONYX, GemGlow.ZENYTE};
		for (int i = 0; i < lowToHigh.length; i++)
		{
			assertEquals(i, GemStyle.tier(lowToHigh[i]));
		}
		for (int i = 1; i < lowToHigh.length; i++)
		{
			GemGlow lower = lowToHigh[i - 1];
			GemGlow higher = lowToHigh[i];
			assertTrue(higher + "", maxHaze(higher).width > maxHaze(lower).width);
			assertTrue(higher + "", maxHaze(higher).color.getAlpha() > maxHaze(lower).color.getAlpha());
			assertTrue(higher + "", minAlpha(higher) > minAlpha(lower));
		}
		// the climb is obvious, not subtle
		assertTrue(maxHaze(GemGlow.ZENYTE).width >= 3 * maxHaze(GemGlow.OPAL).width);
		assertTrue(minAlpha(GemGlow.ZENYTE) >= 3 * minAlpha(GemGlow.OPAL));
	}

	@Test
	public void opalSheenDriftsThroughColours()
	{
		long period = GemStyle.OPAL_SHEEN_MILLIS;
		assertNotEquals(rgb(GemStyle.layers(GemGlow.OPAL, 0).get(0).color),
			rgb(GemStyle.layers(GemGlow.OPAL, period / 3).get(0).color));
		assertEquals(rgb(GemStyle.layers(GemGlow.OPAL, 0).get(0).color),
			rgb(GemStyle.layers(GemGlow.OPAL, period).get(0).color));
	}

	@Test
	public void starsTwinkleFromDiamondUpMoreAndBiggerEachGem()
	{
		assertNull(GemStyle.sparkle(GemGlow.OPAL));
		assertNull(GemStyle.sparkle(GemGlow.SAPPHIRE));
		assertNull(GemStyle.sparkle(GemGlow.EMERALD));
		assertNull(GemStyle.sparkle(GemGlow.RUBY));
		GemGlow[] lowToHigh = {GemGlow.DIAMOND, GemGlow.DRAGONSTONE, GemGlow.ONYX, GemGlow.ZENYTE};
		for (int i = 1; i < lowToHigh.length; i++)
		{
			GemStyle.Sparkle lower = GemStyle.sparkle(lowToHigh[i - 1]);
			GemStyle.Sparkle higher = GemStyle.sparkle(lowToHigh[i]);
			assertTrue(higher.count > lower.count);
			assertTrue(higher.radius > lower.radius);
		}
		assertTrue(GemStyle.sparkle(GemGlow.DIAMOND).radius >= 10);
		// Onyx twinkles black, Zenyte red-orange
		Color onyx = GemStyle.sparkle(GemGlow.ONYX).color;
		assertTrue(onyx.getRed() + onyx.getGreen() + onyx.getBlue() < 30);
		Color zenyte = GemStyle.sparkle(GemGlow.ZENYTE).color;
		assertTrue(zenyte.getRed() == 255 && zenyte.getGreen() < 100 && zenyte.getGreen() > zenyte.getBlue());
	}

	private static Color haze(GemGlow gem)
	{
		return GemStyle.layers(gem, 0).get(0).color;
	}

	/** the haze at its widest and brightest over a few breaths */
	private static GlowStyle.Layer maxHaze(GemGlow gem)
	{
		GlowStyle.Layer max = null;
		for (long t = 0; t < 7000; t += 13)
		{
			GlowStyle.Layer haze = GemStyle.layers(gem, t).get(0);
			if (max == null || haze.color.getAlpha() > max.color.getAlpha())
			{
				max = haze;
			}
		}
		return max;
	}

	private static int minAlpha(GemGlow gem)
	{
		int min = 255;
		for (long t = 0; t < 7000; t += 13)
		{
			min = Math.min(min, GemStyle.layers(gem, t).get(0).color.getAlpha());
		}
		return min;
	}

	private static int rgb(Color color)
	{
		return color.getRGB() & 0xFFFFFF;
	}
}
