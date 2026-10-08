package com.corclan.glow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import net.runelite.api.clan.ClanRank;
import org.junit.Test;

public class GlowEffectTest
{
	private static final ClanRank CAPTAIN = new ClanRank(5);

	@Test
	public void playersWhoNeverPickedShowTheirOwnRanksEffects()
	{
		assertEquals(EnumSet.allOf(GlowEffect.class), GlowEffect.active(ClanRank.OWNER, null));
		assertTrue(GlowEffect.active(CAPTAIN, null).isEmpty());
		assertTrue(GlowEffect.active(ClanRank.GUEST, null).isEmpty());
	}

	@Test
	public void picksAreLimitedToWhatTheWearersRankUnlocks()
	{
		assertEquals(EnumSet.of(GlowEffect.GOLD_OUTLINE, GlowEffect.SHOCKS),
			GlowEffect.active(ClanRank.OWNER, Arrays.asList("gold_outline", "shocks")));
		// a Captain (or a forged request) picking Owner effects shows nothing
		assertTrue(GlowEffect.active(CAPTAIN, Arrays.asList("gold_outline", "storm_cloud")).isEmpty());
		assertTrue(GlowEffect.active(ClanRank.DEPUTY_OWNER, Collections.singletonList("storm_cloud")).isEmpty());
	}

	@Test
	public void emptyPicksMeanNothingAndUnknownIdsAreIgnored()
	{
		assertTrue(GlowEffect.active(ClanRank.OWNER, Collections.emptyList()).isEmpty());
		assertEquals(EnumSet.of(GlowEffect.BIG_STRIKE),
			GlowEffect.active(ClanRank.OWNER, Arrays.asList("rainbow_from_the_future", "big_strike")));
		assertTrue(GlowEffect.active(null, null).isEmpty());
	}

	@Test
	public void higherRanksUnlockLowerRanksEffects()
	{
		assertTrue(GlowEffect.GOLD_OUTLINE.unlockedBy(ClanRank.OWNER));
		assertTrue(GlowEffect.GOLD_OUTLINE.unlockedBy(ClanRank.JMOD));
		assertTrue(!GlowEffect.GOLD_OUTLINE.unlockedBy(ClanRank.DEPUTY_OWNER));
	}

	@Test
	public void idsAndConfigKeysAreUniqueAndRoundTrip()
	{
		Set<String> ids = new HashSet<>();
		Set<String> keys = new HashSet<>();
		for (GlowEffect effect : GlowEffect.values())
		{
			assertTrue(ids.add(effect.id));
			assertTrue(keys.add(effect.configKey));
			assertSame(effect, GlowEffect.byId(effect.id));
			assertTrue(GlowEffect.isConfigKey(effect.configKey));
			assertSame(effect, GlowEffect.byShortName(effect.shortName.toUpperCase()));
		}
		assertNull(GlowEffect.byId("nope"));
		assertNull(GlowEffect.byShortName("nope"));
		assertTrue(!GlowEffect.isConfigKey("rankGlow"));
	}
}
