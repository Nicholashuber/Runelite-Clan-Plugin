package com.corclan.glow;

import com.corclan.CorClanConfig;
import com.corclan.icons.MemberCosmetics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanRank;

/**
 * Which glow effects each player shows: your own from your glow settings, everyone else's from the
 * CoR party (players outside the party, or who haven't sent picks yet, show their rank's defaults).
 * Always limited to what the wearer's clan rank, as this client sees it, unlocks. Client thread only.
 */
@Singleton
public class GlowPicks
{
	private final Client client;
	private final CorClanConfig config;
	/** member key -> picked effect ids, from party messages */
	private final Map<String, List<String>> partyPicks = new HashMap<>();
	/** party member id -> member key, so picks go when the member leaves */
	private final Map<Long, String> partyNames = new HashMap<>();

	@Inject
	GlowPicks(Client client, CorClanConfig config)
	{
		this.client = client;
		this.config = config;
	}

	/** The effect ids you switched on, locked ones included (the viewer filters by rank). */
	public List<String> localPicks()
	{
		List<String> ids = new ArrayList<>();
		for (GlowEffect effect : GlowEffect.values())
		{
			if (picked(config, effect))
			{
				ids.add(effect.id);
			}
		}
		return ids;
	}

	public static boolean picked(CorClanConfig config, GlowEffect effect)
	{
		switch (effect)
		{
			case GOLD_OUTLINE:
				return config.glowGoldOutline();
			case STORM_CLOUD:
				return config.glowStormCloud();
			case LIGHTNING_STRIKES:
				return config.glowLightningStrikes();
			case SHOCKS:
				return config.glowShocks();
			case BIG_STRIKE:
				return config.glowBigStrike();
			default:
				return false;
		}
	}

	/** A party member's picks arrived (replacing any earlier ones). */
	public void setPartyPicks(long memberId, String name, List<String> glows)
	{
		String key = MemberCosmetics.key(name);
		String old = partyNames.put(memberId, key);
		if (old != null && !old.equals(key))
		{
			partyPicks.remove(old);
		}
		partyPicks.put(key, glows == null ? new ArrayList<>() : new ArrayList<>(glows));
	}

	/** A party member left: they show their rank's defaults again. */
	public void removePartyMember(long memberId)
	{
		String key = partyNames.remove(memberId);
		if (key != null)
		{
			partyPicks.remove(key);
		}
	}

	public void clearPartyPicks()
	{
		partyPicks.clear();
		partyNames.clear();
	}

	/** Your own clan rank, or null when not in a clan (or not loaded yet). */
	public ClanRank localRank()
	{
		Player me = client.getLocalPlayer();
		ClanChannel channel = client.getClanChannel();
		if (me == null || me.getName() == null || channel == null)
		{
			return null;
		}
		ClanChannelMember member = channel.findMember(me.getName());
		return member == null ? null : member.getRank();
	}

	/** The effects {@code player} shows, empty if they aren't in the clan. */
	public Set<GlowEffect> effectsFor(ClanChannel channel, Player player)
	{
		if (channel == null || player == null || !player.isClanMember() || player.getName() == null)
		{
			return Collections.emptySet();
		}
		ClanChannelMember member = channel.findMember(player.getName());
		if (member == null)
		{
			return Collections.emptySet();
		}
		List<String> picks = player == client.getLocalPlayer()
			? localPicks()
			: partyPicks.get(MemberCosmetics.key(player.getName()));
		return GlowEffect.active(member.getRank(), picks);
	}
}
