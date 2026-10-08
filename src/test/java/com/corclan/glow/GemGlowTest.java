package com.corclan.glow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.runelite.api.clan.ClanRank;
import org.junit.Test;

public class GemGlowTest
{
	private static final ClanRank OPAL = new ClanRank(10);
	private static final ClanRank RUBY = new ClanRank(14);
	private static final ClanRank ZENYTE = new ClanRank(18);
	private static final ClanRank STAFF = new ClanRank(100);
	private static final ClanRank RECRUIT = new ClanRank(1);

	private static Map<Integer, String> titles()
	{
		Map<Integer, String> titles = new HashMap<>();
		titles.put(1, "Recruit");
		titles.put(10, "Opal");
		titles.put(11, "Sapphire");
		titles.put(12, "Emerald");
		// clans retype titles freely: case and spacing don't matter
		titles.put(14, " RUBY ");
		titles.put(15, "Diamond");
		titles.put(16, "Dragonstone");
		titles.put(17, "Onyx");
		titles.put(18, "Zenyte");
		titles.put(100, "Marshal");
		return titles;
	}

	@Test
	public void eachGemNeedsItsRankOrHigher()
	{
		assertTrue(GemGlow.OPAL.unlockedBy(OPAL, titles()));
		assertFalse(GemGlow.SAPPHIRE.unlockedBy(OPAL, titles()));
		assertTrue(GemGlow.RUBY.unlockedBy(RUBY, titles()));
		assertTrue(GemGlow.EMERALD.unlockedBy(RUBY, titles()));
		assertFalse(GemGlow.DIAMOND.unlockedBy(RUBY, titles()));
		assertTrue(GemGlow.ZENYTE.unlockedBy(STAFF, titles()));
		assertFalse(GemGlow.OPAL.unlockedBy(RECRUIT, titles()));
		assertFalse(GemGlow.OPAL.unlockedBy(null, titles()));
	}

	@Test
	public void aGemTheClanHasNoRankForStaysLocked()
	{
		Map<Integer, String> titles = titles();
		titles.remove(16);
		assertFalse(GemGlow.DRAGONSTONE.unlockedBy(STAFF, titles));
		assertFalse(GemGlow.OPAL.unlockedBy(STAFF, Collections.emptyMap()));
	}

	@Test
	public void playersWhoNeverPickedShowTheirOwnGem()
	{
		assertEquals(GemGlow.OPAL, GemGlow.shown(OPAL, titles(), null));
		assertEquals(GemGlow.RUBY, GemGlow.shown(RUBY, titles(), null));
		assertNull(GemGlow.shown(STAFF, titles(), null));
		assertNull(GemGlow.shown(RECRUIT, titles(), null));
		assertNull(GemGlow.shown(null, titles(), null));
	}

	@Test
	public void theHighestUnlockedPickShows()
	{
		assertEquals(GemGlow.RUBY, GemGlow.shown(RUBY, titles(), allIds()));
		assertEquals(GemGlow.SAPPHIRE, GemGlow.shown(RUBY, titles(), Arrays.asList("gem_opal", "gem_sapphire")));
		// a forged or locked pick is skipped, not shown
		assertEquals(GemGlow.OPAL, GemGlow.shown(OPAL, titles(), Arrays.asList("gem_zenyte", "gem_opal")));
		assertNull(GemGlow.shown(OPAL, titles(), Collections.singletonList("gem_zenyte")));
		// other glow ids mixed in are ignored; unticking everything shows nothing
		assertEquals(GemGlow.ZENYTE, GemGlow.shown(ZENYTE, titles(), Arrays.asList("gold_outline", "gem_zenyte")));
		assertNull(GemGlow.shown(ZENYTE, titles(), Collections.emptyList()));
	}

	@Test
	public void idsAndConfigKeysAreUnique()
	{
		Set<String> ids = new HashSet<>();
		Set<String> keys = new HashSet<>();
		for (GemGlow gem : GemGlow.values())
		{
			assertTrue(ids.add(gem.id));
			assertTrue(keys.add(gem.configKey));
			assertTrue(GemGlow.isConfigKey(gem.configKey));
			// rank glow picks are resent when any of these settings change
			assertTrue(GlowEffect.isConfigKey(gem.configKey));
			assertNull(GlowEffect.byId(gem.id));
		}
		assertFalse(GemGlow.isConfigKey("glowGoldOutline"));
	}

	@Test
	public void outlineAndSparklesSwitchSeparately()
	{
		// never shared picks: both show
		assertEquals(EnumSet.allOf(GemGlow.Part.class), GemGlow.Part.shown(null));
		assertEquals(EnumSet.of(GemGlow.Part.OUTLINE),
			GemGlow.Part.shown(Arrays.asList("gem_zenyte", "gem_outline")));
		assertEquals(EnumSet.of(GemGlow.Part.SPARKLES),
			GemGlow.Part.shown(Arrays.asList("gem_zenyte", "gem_sparkles")));
		assertTrue(GemGlow.Part.shown(Collections.singletonList("gem_zenyte")).isEmpty());
	}

	@Test
	public void partsHaveCommandsAndResendPicksWhenChanged()
	{
		assertEquals(GemGlow.Part.SPARKLES, GemGlow.Part.byCommand("SPARKLES"));
		assertEquals(GemGlow.Part.OUTLINE, GemGlow.Part.byCommand("outline"));
		assertNull(GemGlow.Part.byCommand("myglow"));
		for (GemGlow.Part part : GemGlow.Part.values())
		{
			assertTrue(GlowEffect.isConfigKey(part.configKey));
			assertNull(GlowEffect.byId(part.id));
			for (GemGlow gem : GemGlow.values())
			{
				assertFalse(gem.id.equals(part.id));
			}
		}
	}

	private static Set<String> allIds()
	{
		Set<String> ids = new HashSet<>();
		for (GemGlow gem : GemGlow.values())
		{
			ids.add(gem.id);
		}
		return ids;
	}
}
