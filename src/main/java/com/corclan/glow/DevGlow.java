package com.corclan.glow;

import com.corclan.CorClanConfig;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/**
 * The parts of Lavasockz's aura ({@link LavaAura}), each switchable by Lavasockz only: hidden settings, set
 * from the "Dev glow" section of the CoR side panel (shown only to Lavasockz) or with ::devglow. Picks travel
 * through the clan server (clan sync) in the same list as the rank glow picks, so other plugin users see
 * what he chose. Separate from {@link GlowEffect}, which is unlocked by clan rank, not by name.
 */
public enum DevGlow
{
	MOLTEN_OUTLINE("dev_molten_outline", "devGlowOutline", "lava", "Molten outline"),
	ZAMORAK_FLAMES("dev_zamorak_flames", "devGlowFlames", "flames", "Flames of Zamorak");

	/** sent to the clan server and on to other plugin users; never change one once released */
	public final String id;
	/** the hidden checkbox's key in {@link CorClanConfig} */
	public final String configKey;
	/** its name in ::devglow, e.g. ::devglow flames off */
	public final String shortName;
	/** shown next to its checkbox in the side panel */
	public final String label;

	DevGlow(String id, String configKey, String shortName, String label)
	{
		this.id = id;
		this.configKey = configKey;
		this.shortName = shortName;
		this.label = label;
	}

	public static boolean picked(CorClanConfig config, DevGlow glow)
	{
		switch (glow)
		{
			case MOLTEN_OUTLINE:
				return config.devGlowOutline();
			case ZAMORAK_FLAMES:
				return config.devGlowFlames();
			default:
				return false;
		}
	}

	/** The part called {@code name} in ::devglow (any case), or null. */
	public static DevGlow byShortName(String name)
	{
		for (DevGlow glow : values())
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
		for (DevGlow glow : values())
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
	 * @param picks the ids they shared through the clan server (rank glow ids mixed in are ignored), or null if they
	 *              shared none: then everything shows
	 */
	public static Set<DevGlow> active(Collection<String> picks)
	{
		if (picks == null)
		{
			return EnumSet.allOf(DevGlow.class);
		}
		Set<DevGlow> active = EnumSet.noneOf(DevGlow.class);
		for (DevGlow glow : values())
		{
			if (picks.contains(glow.id))
			{
				active.add(glow);
			}
		}
		return active;
	}
}
