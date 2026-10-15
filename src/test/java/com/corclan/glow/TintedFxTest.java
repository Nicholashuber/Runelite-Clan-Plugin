package com.corclan.glow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
	public void greenKeepsTheShadingInLeafGreen()
	{
		short orange = (short) (6 << 10 | 7 << 7 | 60);
		short green = TintedFx.tint(orange, TintedFx.Tint.GREEN);
		assertEquals(21, hue(green));
		assertEquals(6, saturation(green));
		assertEquals(60, lightness(green));
		short white = (short) (6 << 10 | 1 << 7 | 127);
		assertTrue(lightness(TintedFx.tint(white, TintedFx.Tint.GREEN)) <= 100);
	}

	@Test
	public void defaultsAreInTheCatalog()
	{
		assertNotNull(TintedFx.CATALOG.get(DaylightAura.DEFAULT_FLAMES));
		assertNotNull(TintedFx.CATALOG.get(DaylightAura.DEFAULT_SWIRL));
	}

	@Test
	public void noneKeepsTheColours()
	{
		short orange = (short) (6 << 10 | 7 << 7 | 60);
		assertEquals(orange, TintedFx.tint(orange, TintedFx.Tint.NONE));
	}

	@Test
	public void readsASpotAnimDefinitionFromTheCache()
	{
		byte[] data = {
			1, (byte) 0x9D, 0x5C,     // model 40284
			2, 0x29, 0x5E,            // animation 10590
			4, 0, (byte) 0xA0,        // resize width 160
			9, 'r', 'i', 'n', 'g', 0, // debug name
			40, 1, 0, 1, 0, 2,        // one recolour pair, skipped
			7, 20,                    // ambient
			8, 90,                    // contrast
			0,
		};
		TintedFx.Def def = TintedFx.parseDef(569, data);
		assertNotNull(def);
		assertEquals(40284, def.model);
		assertEquals(10590, def.animation);
		assertEquals(160, def.resizeH);
		assertEquals(128, def.resizeV);
		assertEquals(20, def.ambient);
		assertEquals(90, def.contrast);
	}

	@Test
	public void anUnknownOrCutOffDefinitionIsNotGuessed()
	{
		assertNull(TintedFx.parseDef(1, new byte[]{99, 0}));
		assertNull(TintedFx.parseDef(1, new byte[]{1, 0}));
		assertNull(TintedFx.parseDef(1, new byte[]{2, 0, 5, 0}));
		assertNull(TintedFx.parseDef(1, null));
	}
}
