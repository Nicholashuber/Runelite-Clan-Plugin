package com.corclan.glow;

import com.corclan.CorClanConfig;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/**
 * The parts of JoeBoob's nature aura ({@link NatureAura}), a green take on DAYLlGHT's {@link FounderGlow}. Each is
 * switchable by JoeBoob only: hidden settings, set from the "Nature glow" section of the CoR side panel (shown only
 * to JoeBoob), the rank card's Glow switch or ::natureglow. Picks travel through the clan server (clan sync) in the
 * same list as the rank glow picks.
 */
public enum NatureGlow
{
	FAIRY_RING("nature_fairy_ring", "natureGlowFairyRing", "ring", "Zanaris fairy ring and leaves"),
	LEAF_SWIRL("nature_leaf_swirl", "natureGlowSwirl", "swirl", "Leaf green swirl"),
	BLOOM("nature_bloom", "natureGlowBloom", "bloom", "Druid bloom"),
	EMERALD_SPARKLE("nature_emerald_sparkle", "natureGlowSparkle", "sparkle", "Emerald sparkle");

	/** sent to the clan server and on to other plugin users; never change one once released */
	public final String id;
	/** the hidden checkbox's key in {@link CorClanConfig} */
	public final String configKey;
	/** its name in ::natureglow, e.g. ::natureglow bloom off */
	public final String shortName;
	/** shown next to its checkbox in the side panel */
	public final String label;

	NatureGlow(String id, String configKey, String shortName, String label)
	{
		this.id = id;
		this.configKey = configKey;
		this.shortName = shortName;
		this.label = label;
	}

	public static boolean picked(CorClanConfig config, NatureGlow glow)
	{
		switch (glow)
		{
			case FAIRY_RING:
				return config.natureGlowFairyRing();
			case LEAF_SWIRL:
				return config.natureGlowSwirl();
			case BLOOM:
				return config.natureGlowBloom();
			case EMERALD_SPARKLE:
				return config.natureGlowSparkle();
			default:
				return false;
		}
	}

	/** The part called {@code name} in ::natureglow (any case), or null. */
	public static NatureGlow byShortName(String name)
	{
		for (NatureGlow glow : values())
		{
			if (glow.shortName.equalsIgnoreCase(name))
			{
				return glow;
			}
		}
		return null;
	}

	/**
	 * What the wearer shows.
	 * @param picks the ids they shared through the clan server (other glow ids mixed in are ignored), or null if they
	 *              shared none: then everything shows
	 */
	public static Set<NatureGlow> active(Collection<String> picks)
	{
		if (picks == null)
		{
			return EnumSet.allOf(NatureGlow.class);
		}
		Set<NatureGlow> active = EnumSet.noneOf(NatureGlow.class);
		for (NatureGlow glow : values())
		{
			if (picks.contains(glow.id))
			{
				active.add(glow);
			}
		}
		return active;
	}
}
