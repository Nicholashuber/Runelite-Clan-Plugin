package com.corclan.glow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class TintedFxTest
{
	private static int hue(short hsl)
	{
		return hsl >> 10 & 63;
	}

	private static int saturation(short hsl)
	{
		return hsl >> 7 & 7;
	}

	private static int lightness(short hsl)
	{
		return hsl & 127;
	}

	@Test
	public void redKeepsTheShadingButNotTheHue()
	{
		// an orange fire face: hue 6, saturation 7, lightness 60
		short orange = (short) (6 << 10 | 7 << 7 | 60);
		short red = TintedFx.tint(orange, TintedFx.Tint.RED);
		assertEquals(0, hue(red));
		assertEquals(7, saturation(red));
		assertEquals(60, lightness(red));
		// a white-hot core stays red instead of washing out
		short white = (short) (6 << 10 | 1 << 7 | 127);
		assertTrue(lightness(TintedFx.tint(white, TintedFx.Tint.RED)) <= 90);
	}

	@Test
	public void blackIsGreyAndDark()
	{
		short bright = (short) (40 << 10 | 5 << 7 | 120);
		short black = TintedFx.tint(bright, TintedFx.Tint.BLACK);
		assertEquals(0, saturation(black));
		assertTrue(lightness(black) <= 20);
	}

	@Test
	public void defaultsAreInTheCatalog()
	{
		assertNotNull(TintedFx.CATALOG.get(DaylightAura.DEFAULT_FLAMES));
		assertNotNull(TintedFx.CATALOG.get(DaylightAura.DEFAULT_SWIRL));
	}
}
