package com.corclan.glow;

import com.corclan.CorClanConfig;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/**
 * The parts of DAYLlGHT's aura ({@link DaylightAura}), each switchable by DAYLlGHT only: hidden settings, set
 * from the "Founder glow" section of the CoR side panel (shown only to DAYLlGHT) or with ::founderglow. Picks
 * travel through the CoR party inside the same message as the rank glow picks, like {@link DevGlow}.
 */
public enum FounderGlow
{
	DEMON_FLAMES("founder_demon_flames", "founderGlowFlames", "flames", "Red Tormented Demon flames"),
	BLACK_SWIRL("founder_black_swirl", "founderGlowSwirl", "swirl", "Black swirl"),
	SMOKE_CLOUD("founder_smoke_cloud", "founderGlowSmoke", "smoke", "Smoke cloud"),
	GEM_SPARKLE("founder_gem_sparkle", "founderGlowSparkle", "sparkle", "Gem sparkle");

	/** sent to other party members; never change one once released */
	public final String id;
	/** the hidden checkbox's key in {@link CorClanConfig} */
	public final String configKey;
	/** its name in ::founderglow, e.g. ::founderglow smoke off */
	public final String shortName;
	/** shown next to its checkbox in the side panel */
	public final String label;

	FounderGlow(String id, String configKey, String shortName, String label)
	{
		this.id = id;
		this.configKey = configKey;
		this.shortName = shortName;
		this.label = label;
	}

	public static boolean picked(CorClanConfig config, FounderGlow glow)
	{
		switch (glow)
		{
			case DEMON_FLAMES:
				return config.founderGlowFlames();
			case BLACK_SWIRL:
				return config.founderGlowSwirl();
			case SMOKE_CLOUD:
				return config.founderGlowSmoke();
			case GEM_SPARKLE:
				return config.founderGlowSparkle();
			default:
				return false;
		}
	}

	/** The part called {@code name} in ::founderglow (any case), or null. */
	public static FounderGlow byShortName(String name)
	{
		for (FounderGlow glow : values())
		{
			if (glow.shortName.equalsIgnoreCase(name))
			{
				return glow;
			}
		}
		return null;
	}

	public static boolean isConfigKey(String key)
	{
		for (FounderGlow glow : values())
		{
			if (glow.configKey.equals(key))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * What the wearer shows.
	 * @param picks the ids they shared through the party (other glow ids mixed in are ignored), or null if they
	 *              shared none: then everything shows
	 */
	public static Set<FounderGlow> active(Collection<String> picks)
	{
		if (picks == null)
		{
			return EnumSet.allOf(FounderGlow.class);
		}
		Set<FounderGlow> active = EnumSet.noneOf(FounderGlow.class);
		for (FounderGlow glow : values())
		{
			if (picks.contains(glow.id))
			{
				active.add(glow);
			}
		}
		return active;
	}
}
