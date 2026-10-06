package com.corclan.glow;

import com.corclan.CorClanConfig;
import com.corclan.clan.OrgChart;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import net.runelite.api.clan.ClanRank;

/**
 * The Gem League rank glows, highest gem first. Each is unlocked by its gem rank or any higher rank.
 * Gem ranks are matched by in-game title (clans renumber and rename ranks freely), so unlocking needs
 * the clan's rank titles. Picks travel through the CoR party in the same message as {@link GlowEffect}'s.
 * A player shows at most one gem glow: the highest one they picked and unlocked. Looks: {@link GemStyle}.
 */
public enum GemGlow
{
	ZENYTE("gem_zenyte", "glowZenyte", "Zenyte"),
	ONYX("gem_onyx", "glowOnyx", "Onyx"),
	DRAGONSTONE("gem_dragonstone", "glowDragonstone", "Dragonstone"),
	DIAMOND("gem_diamond", "glowDiamond", "Diamond"),
	RUBY("gem_ruby", "glowRuby", "Ruby"),
	EMERALD("gem_emerald", "glowEmerald", "Emerald"),
	SAPPHIRE("gem_sapphire", "glowSapphire", "Sapphire"),
	OPAL("gem_opal", "glowOpal", "Opal");

	/** sent to other party members; never change one once released */
	public final String id;
	/** the checkbox's key in {@link CorClanConfig} */
	public final String configKey;
	/** the in-game rank title that unlocks it */
	public final String title;

	GemGlow(String id, String configKey, String title)
	{
		this.id = id;
		this.configKey = configKey;
		this.title = title;
	}

	public static boolean picked(CorClanConfig config, GemGlow gem)
	{
		switch (gem)
		{
			case ZENYTE:
				return config.glowZenyte();
			case ONYX:
				return config.glowOnyx();
			case DRAGONSTONE:
				return config.glowDragonstone();
			case DIAMOND:
				return config.glowDiamond();
			case RUBY:
				return config.glowRuby();
			case EMERALD:
				return config.glowEmerald();
			case SAPPHIRE:
				return config.glowSapphire();
			case OPAL:
				return config.glowOpal();
			default:
				return false;
		}
	}

	/**
	 * The two halves of a gem glow, each switchable on its own (settings, ::outline, ::sparkles) and shared
	 * through the CoR party with the gem picks.
	 */
	public enum Part
	{
		OUTLINE("gem_outline", "glowGemOutline", "outline"),
		SPARKLES("gem_sparkles", "glowGemSparkles", "sparkles");

		/** sent to other party members; never change one once released */
		public final String id;
		/** the checkbox's key in {@link CorClanConfig} */
		public final String configKey;
		/** its chat command, e.g. ::sparkles off */
		public final String command;

		Part(String id, String configKey, String command)
		{
			this.id = id;
			this.configKey = configKey;
			this.command = command;
		}

		public static boolean picked(CorClanConfig config, Part part)
		{
			switch (part)
			{
				case OUTLINE:
					return config.glowGemOutline();
				case SPARKLES:
					return config.glowGemSparkles();
				default:
					return false;
			}
		}

		/** The part switched by ::{@code command} (any case), or null. */
		public static Part byCommand(String command)
		{
			for (Part part : values())
			{
				if (part.command.equalsIgnoreCase(command))
				{
					return part;
				}
			}
			return null;
		}

		/**
		 * The parts a wearer shows.
		 * @param picks the ids they shared, or null if they shared none: then both show
		 */
		public static Set<Part> shown(Collection<String> picks)
		{
			Set<Part> shown = EnumSet.noneOf(Part.class);
			for (Part part : values())
			{
				if (picks == null || picks.contains(part.id))
				{
					shown.add(part);
				}
			}
			return shown;
		}
	}

	/** True for the gem picks' settings and the {@link Part} switches. */
	public static boolean isConfigKey(String key)
	{
		for (GemGlow gem : values())
		{
			if (gem.configKey.equals(key))
			{
				return true;
			}
		}
		for (Part part : Part.values())
		{
			if (part.configKey.equals(key))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * True when {@code wearer} holds this gem's rank or a higher one.
	 * @param rankTitles the clan's rank number to title; no rank titled after this gem means locked
	 */
	public boolean unlockedBy(ClanRank wearer, Map<Integer, String> rankTitles)
	{
		if (wearer == null)
		{
			return false;
		}
		for (Map.Entry<Integer, String> rank : rankTitles.entrySet())
		{
			if (titled(rank.getValue()) && wearer.getRank() >= rank.getKey())
			{
				return true;
			}
		}
		return false;
	}

	private boolean titled(String rankTitle)
	{
		return OrgChart.normalize(title).equals(OrgChart.normalize(rankTitle));
	}

	/**
	 * The gem glow a player of rank {@code wearer} shows, or null.
	 * @param picks the ids they picked (other glow ids mixed in are ignored), or null if they never shared
	 *              any: then they show their own gem rank's glow, if they hold one
	 */
	public static GemGlow shown(ClanRank wearer, Map<Integer, String> rankTitles, Collection<String> picks)
	{
		if (wearer == null)
		{
			return null;
		}
		for (GemGlow gem : values())
		{
			if (picks == null ? gem.titled(rankTitles.get(wearer.getRank()))
				: picks.contains(gem.id) && gem.unlockedBy(wearer, rankTitles))
			{
				return gem;
			}
		}
		return null;
	}
}
