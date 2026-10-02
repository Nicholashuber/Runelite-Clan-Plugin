package com.corclan.glow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.gameval.SpotanimID;
import org.junit.Test;

public class HolyAuraTest
{
	private static final HolyAura AURA = new HolyAura(null, null, null, null);

	private static void assertDelaysCoverRange(HolyAura.Effect effect)
	{
		Random random = new Random(42);
		Set<Integer> seen = new HashSet<>();
		for (int i = 0; i < 2000; i++)
		{
			int d = effect.delay(random);
			assertTrue(d >= effect.minTicks && d <= effect.maxTicks);
			seen.add(d);
		}
		// every gap in the range turns up, so effects don't fall into a fixed rhythm
		assertEquals(effect.maxTicks - effect.minTicks + 1, seen.size());
	}

	@Test
	public void theStormLayers()
	{
		assertEquals(SpotanimID.HALLOWED_STATUE_LIGHTNING_STRIKE, AURA.strikes.spotAnimId);
		assertEquals(SpotanimID.SKELETON_KILLERWATT_ELECTRICSHOCK, AURA.shock.spotAnimId);
		assertEquals(SpotanimID.LUC2_LUCIEN_LIGHTNING_SPOT, AURA.bigStrike.spotAnimId);
	}

	@Test
	public void randomLayersVaryWithinTheirRanges()
	{
		assertDelaysCoverRange(AURA.strikes);
		assertDelaysCoverRange(AURA.shock);
		assertDelaysCoverRange(AURA.bigStrike);
		// frequent strikes every 1.2 - 3.6 s, the big one every 10 - 20 s (0.6 s per tick)
		assertEquals(2, AURA.strikes.minTicks);
		assertEquals(6, AURA.strikes.maxTicks);
		assertTrue(AURA.bigStrike.minTicks * 0.6 >= 10 && AURA.bigStrike.maxTicks * 0.6 <= 20);
	}

	@Test
	public void everyLayerHasItsOwnSlotAndCommand()
	{
		assertEquals(AURA.effects.size(), AURA.effects.stream().map(e -> e.slot).collect(Collectors.toSet()).size());
		assertSame(AURA.strikes, AURA.forCommand("glowzap"));
		assertSame(AURA.shock, AURA.forCommand("glowshock"));
		assertSame(AURA.bigStrike, AURA.forCommand("glowstrike"));
		assertNull(AURA.forCommand("glowcloud"));
		assertNull(AURA.forCommand("cor"));
	}
}
