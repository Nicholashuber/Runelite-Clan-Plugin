package com.corclan.glow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.runelite.api.clan.ClanRank;
import org.junit.Test;

public class DevGlowTest
{
	@Test
	public void wholeAuraShowsUntilPicksArrive()
	{
		assertEquals(EnumSet.allOf(DevGlow.class), DevGlow.active(null));
		assertTrue(DevGlow.active(Collections.emptyList()).isEmpty());
	}

	@Test
	public void rankAndDevPicksShareOneMessageWithoutMixingUp()
	{
		List<String> picks = Arrays.asList("gold_outline", "dev_zamorak_flames", "storm_cloud");
		assertEquals(EnumSet.of(DevGlow.ZAMORAK_FLAMES), DevGlow.active(picks));
		// Ray's rank glows ignore the dev ids
		assertEquals(EnumSet.of(GlowEffect.GOLD_OUTLINE, GlowEffect.STORM_CLOUD), GlowEffect.active(ClanRank.OWNER, picks));
	}

	@Test
	public void namesKeysAndIdsAreUniqueAndApartFromRankGlows()
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
			assertTrue(ids.add(glow.id));
			assertTrue(keys.add(glow.configKey));
			assertTrue(names.add(glow.shortName));
			assertSame(glow, DevGlow.byShortName(glow.shortName.toUpperCase()));
			assertTrue(DevGlow.isConfigKey(glow.configKey));
		}
		assertNull(DevGlow.byShortName("nope"));
		assertTrue(!DevGlow.isConfigKey("glowGoldOutline"));
	}
}
