package com.corclan.glow;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import net.runelite.api.clan.ClanRank;

/**
 * Every glow effect a clan member can show on themselves. Each needs a clan rank: that rank or any
 * higher one unlocks it. The Owner's effects are hidden settings, switched in the side panel (shown only
 * to the Owner) or with ::myglow, so the rest of the clan never sees them. Picks are shared through the
 * CoR party so other plugin users see them. The viewer always checks the wearer's real clan rank, so
 * picking a locked effect (or a forged pick) shows nothing.
 */
public enum GlowEffect
{
	GOLD_OUTLINE("gold_outline", "glowGoldOutline", "outline", "Gold outline", ClanRank.OWNER),
	STORM_CLOUD("storm_cloud", "glowStormCloud", "cloud", "Storm cloud", ClanRank.OWNER),
	LIGHTNING_STRIKES("lightning_strikes", "glowLightningStrikes", "strikes", "Lightning strikes", ClanRank.OWNER),
	SHOCKS("shocks", "glowShocks", "shocks", "Shocks", ClanRank.OWNER),
	BIG_STRIKE("big_strike", "glowBigStrike", "bigstrike", "Big strike", ClanRank.OWNER);

	/** sent to other party members; never change one once released */
	public final String id;
	/** the checkbox's key in {@link com.corclan.CorClanConfig} */
	public final String configKey;
	/** its name in ::myglow, e.g. ::myglow cloud off */
	public final String shortName;
	/** shown next to its checkbox in the side panel */
	public final String label;
	/** lowest rank that unlocks it */
	public final ClanRank rank;

	GlowEffect(String id, String configKey, String shortName, String label, ClanRank rank)
	{
		this.id = id;
		this.configKey = configKey;
		this.shortName = shortName;
		this.label = label;
		this.rank = rank;
	}

	public boolean unlockedBy(ClanRank wearer)
	{
		return wearer != null && wearer.getRank() >= rank.getRank();
	}

	/** The effect with id {@code id}, or null (e.g. one added by a newer plugin version). */
	public static GlowEffect byId(String id)
	{
		for (GlowEffect effect : values())
		{
			if (effect.id.equals(id))
			{
				return effect;
			}
		}
		return null;
	}

	/** The effect called {@code name} in ::myglow (any case), or null. */
	public static GlowEffect byShortName(String name)
	{
		for (GlowEffect effect : values())
		{
			if (effect.shortName.equalsIgnoreCase(name))
			{
				return effect;
			}
		}
		return null;
	}

	public static boolean isConfigKey(String key)
	{
		for (GlowEffect effect : values())
		{
			if (effect.configKey.equals(key))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * What a player of rank {@code wearer} shows.
	 * @param picks the effect ids they picked, or null if they never shared any picks (not in the CoR
	 *              party, older plugin, not received yet): then they show their own rank's effects
	 */
	public static Set<GlowEffect> active(ClanRank wearer, Collection<String> picks)
	{
		if (wearer == null)
		{
			return Collections.emptySet();
		}
		Set<GlowEffect> active = EnumSet.noneOf(GlowEffect.class);
		if (picks == null)
		{
			for (GlowEffect effect : values())
			{
				if (effect.rank.getRank() == wearer.getRank())
				{
					active.add(effect);
				}
			}
			return active;
		}
		for (String id : picks)
		{
			GlowEffect effect = byId(id);
			if (effect != null && effect.unlockedBy(wearer))
			{
				active.add(effect);
			}
		}
		return active;
	}
}
