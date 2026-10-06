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

public class FounderGlowTest
{
	@Test
	public void wholeAuraShowsUntilPicksArrive()
	{
		assertEquals(EnumSet.allOf(FounderGlow.class), FounderGlow.active(null));
		assertTrue(FounderGlow.active(Collections.emptyList()).isEmpty());
	}

	@Test
	public void sharesTheMessageWithRankAndDevPicksWithoutMixingUp()
	{
		List<String> picks = Arrays.asList("gold_outline", "dev_zamorak_flames", "founder_smoke_cloud");
		assertEquals(EnumSet.of(FounderGlow.SMOKE_CLOUD), FounderGlow.active(picks));
		assertEquals(EnumSet.of(DevGlow.ZAMORAK_FLAMES), DevGlow.active(picks));
	}

	@Test
	public void wearerIsSpeltWithALowercaseL()
	{
		assertTrue(DaylightAura.WEARERS.contains(MemberCosmetics.key("DAYLlGHT")));
		assertTrue(!DaylightAura.WEARERS.contains(MemberCosmetics.key("DAYLIGHT")));
	}

	@Test
	public void namesKeysAndIdsAreUniqueAndApartFromOtherGlows()
	{
		Set<String> ids = new HashSet<>();
		Set<String> keys = new HashSet<>();
		Set<String> names = new HashSet<>();
		for (GlowEffect effect : GlowEffect.values())
		{
			ids.add(effect.id);
			keys.add(effect.configKey);
		}
		for (DevGlow glow : DevGlow.values())
		{
			ids.add(glow.id);
			keys.add(glow.configKey);
		}
		for (FounderGlow glow : FounderGlow.values())
		{
			assertTrue(ids.add(glow.id));
			assertTrue(keys.add(glow.configKey));
			assertTrue(names.add(glow.shortName));
			assertSame(glow, FounderGlow.byShortName(glow.shortName.toUpperCase()));
			assertTrue(FounderGlow.isConfigKey(glow.configKey));
		}
		assertNull(FounderGlow.byShortName("nope"));
		assertTrue(!FounderGlow.isConfigKey("devGlowFlames"));
	}
}
