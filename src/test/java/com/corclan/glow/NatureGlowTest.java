package com.corclan.glow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import com.corclan.icons.MemberCosmetics;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class NatureGlowTest
{
	@Test
	public void wholeAuraShowsUntilPicksArrive()
	{
		assertEquals(EnumSet.allOf(NatureGlow.class), NatureGlow.active(null));
		assertTrue(NatureGlow.active(Collections.emptyList()).isEmpty());
	}

	@Test
	public void sharesTheMessageWithOtherAurasWithoutMixingUp()
	{
		List<String> picks = Arrays.asList("founder_black_swirl", "nature_bloom", "nature_emerald_sparkle");
		assertEquals(EnumSet.of(NatureGlow.BLOOM, NatureGlow.EMERALD_SPARKLE), NatureGlow.active(picks));
		assertEquals(EnumSet.of(FounderGlow.BLACK_SWIRL), FounderGlow.active(picks));
	}

	@Test
	public void sameNumberOfPartsAsTheFounderAura()
	{
		assertEquals(FounderGlow.values().length, NatureGlow.values().length);
	}

	@Test
	public void wornByJoeBoob()
	{
		assertTrue(NatureAura.WEARERS.contains(MemberCosmetics.key("JoeBoob")));
	}

	@Test
	public void namesKeysAndIdsAreUniqueAndApartFromOtherGlows()
	{
		Set<String> ids = new HashSet<>();
		Set<String> keys = new HashSet<>();
		Set<String> names = new HashSet<>();
		for (FounderGlow glow : FounderGlow.values())
		{
			ids.add(glow.id);
			keys.add(glow.configKey);
		}
		for (DevGlow glow : DevGlow.values())
		{
			ids.add(glow.id);
			keys.add(glow.configKey);
		}
		for (NatureGlow glow : NatureGlow.values())
		{
			assertTrue(ids.add(glow.id));
			assertTrue(keys.add(glow.configKey));
			assertTrue(names.add(glow.shortName));
			assertSame(glow, NatureGlow.byShortName(glow.shortName.toUpperCase()));
		}
		assertNull(NatureGlow.byShortName("nope"));
	}

	@Test
	public void ownSpotAnimSlots()
	{
		Set<Integer> slots = new HashSet<>(Arrays.asList(DaylightAura.SMOKE_SLOT, LavaAura.SLOT));
		assertTrue(slots.add(NatureAura.BLOOM_SLOT));
		assertTrue(slots.add(NatureAura.LEAVES_SLOT));
	}
}
