package com.corclan.glow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.runelite.api.gameval.SpotanimID;
import org.junit.Test;

public class LavaAuraTest
{
	@Test
	public void flickerStaysInRangeAndNeverSettles()
	{
		Set<Long> seen = new HashSet<>();
		for (long t = 0; t < 20_000; t += 50)
		{
			double f = MoltenStyle.flicker(t);
			assertTrue(f >= 0 && f <= 1);
			seen.add(Math.round(f * 20));
		}
		assertTrue("flicker should cover most of its range", seen.size() >= 15);
	}

	@Test
	public void moltenOutlineIsRedHazeToWhiteHotCore()
	{
		for (long t = 0; t < 10_000; t += 97)
		{
			List<GlowStyle.Layer> layers = MoltenStyle.molten(t);
			assertEquals(3, layers.size());
			Color haze = layers.get(0).color;
			Color core = layers.get(2).color;
			assertTrue(haze.getRed() > haze.getGreen() * 3);
			assertTrue(core.getGreen() >= 200);
			assertTrue(layers.get(0).width > layers.get(1).width && layers.get(1).width > layers.get(2).width);
		}
	}

	@Test
	public void flamesUseTheirOwnSlotAwayFromRaysStorm()
	{
		assertTrue(LavaAura.SLOT < 3082 || LavaAura.SLOT > 3084);
		assertEquals(SpotanimID.ZAMORAK_FLAME, LavaAura.FLAMES);
	}

	@Test
	public void lavasockzWearsIt()
	{
		assertTrue(LavaAura.WEARERS.contains("lavasockz"));
	}
}
