package com.corclan.glow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import java.util.List;
import org.junit.Test;

public class TwinkleTest
{
	private static final long P = Twinkle.PERIOD_MILLIS;

	@Test
	public void starsStayInsideTheBoundsAndBloomZeroToOne()
	{
		for (long t = 0; t < 5 * P; t += 17)
		{
			List<Twinkle.Star> stars = Twinkle.stars(42, 6, t);
			assertEquals(6, stars.size());
			for (Twinkle.Star star : stars)
			{
				assertTrue(star.x >= 0 && star.x < 1);
				assertTrue(star.y >= 0 && star.y < 1);
				assertTrue(star.size >= 0 && star.size <= 1);
			}
		}
	}

	@Test
	public void staggeredSoSomeStarIsAlwaysLit()
	{
		for (long t = 0; t < 5 * P; t += 17)
		{
			double brightest = 0;
			for (Twinkle.Star star : Twinkle.stars(-7, 3, t))
			{
				brightest = Math.max(brightest, star.size);
			}
			assertTrue(brightest > 0.5);
		}
	}

	@Test
	public void aStarHoldsItsPlaceThroughOneBloomThenMoves()
	{
		// the same moment always gives the same stars, so frames and viewers agree
		assertEquals(Twinkle.stars(5, 1, 1000).get(0).x, Twinkle.stars(5, 1, 1000).get(0).x, 0);
		Twinkle.Star a = Twinkle.stars(5, 1, 0).get(0);
		long sameBloom = P - 1 - Math.floorMod(5L, P);
		assertEquals(a.x, Twinkle.stars(5, 1, sameBloom).get(0).x, 0);
		assertNotEquals(a.x, Twinkle.stars(5, 1, sameBloom + 1).get(0).x, 0);
		// other wearers' stars sit elsewhere
		assertNotEquals(a.x, Twinkle.stars(6, 1, 0).get(0).x, 0);
	}
}
