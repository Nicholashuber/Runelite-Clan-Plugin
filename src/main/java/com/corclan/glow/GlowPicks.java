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
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanRank;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.clan.ClanTitle;
import net.runelite.client.config.ConfigManager;

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
	private final ConfigManager configManager;
	/** member key -> picked effect ids, from party messages */
	private final Map<String, List<String>> partyPicks = new HashMap<>();
	/** party member id -> member key, so picks go when the member leaves */
	private final Map<Long, String> partyNames = new HashMap<>();

	@Inject
	GlowPicks(Client client, CorClanConfig config, ConfigManager configManager)
	{
		this.client = client;
		this.config = config;
		this.configManager = configManager;
	}

	/** The effect and gem glow ids you switched on, locked ones included (the viewer filters by rank). */
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
		for (GemGlow gem : GemGlow.values())
		{
			if (GemGlow.picked(config, gem))
			{
				ids.add(gem.id);
			}
		}
		for (GemGlow.Part part : GemGlow.Part.values())
		{
			if (GemGlow.Part.picked(config, part))
			{
				ids.add(part.id);
			}
		}
		return ids;
	}

	/**
	 * ::outline and ::sparkles switch that half of your gem glow; on or off sets it, nothing toggles it.
	 * @return false when {@code command} isn't one of them
	 */
	public boolean gemPartCommand(String command, String[] args)
	{
		GemGlow.Part part = GemGlow.Part.byCommand(command);
		if (part == null)
		{
			return false;
		}
		String state = args.length > 0 ? args[0].toLowerCase() : "";
		if (!(state.isEmpty() || state.equals("on") || state.equals("off")))
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Usage: ::" + part.command + " [on|off]", null);
			return true;
		}
		boolean on = state.isEmpty() ? !GemGlow.Part.picked(config, part) : state.equals("on");
		configManager.setConfiguration(CorClanConfig.GROUP, part.configKey, on);
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "CoR: gem " + part.command + (on ? " on" : " off"), null);
		return true;
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

	/**
	 * Every rank the clan has set up (rank number to title), for unlocking {@link GemGlow}s. Empty when not
	 * in a clan.
	 */
	public Map<Integer, String> clanRankTitles()
	{
		ClanSettings settings = client.getClanSettings();
		Map<Integer, String> titles = new HashMap<>();
		if (settings == null)
		{
			return titles;
		}
		for (int rank = 0; rank <= ClanRank.OWNER.getRank(); rank++)
		{
			ClanTitle title = settings.titleForRank(new ClanRank(rank));
			if (title != null && title.getName() != null && !title.getName().isEmpty())
			{
				titles.put(rank, title.getName());
			}
		}
		return titles;
	}

	/** The effects {@code player} shows, empty if they aren't in the clan. */
	public Set<GlowEffect> effectsFor(ClanChannel channel, Player player)
	{
		ClanChannelMember member = clanMember(channel, player);
		return member == null ? Collections.emptySet() : GlowEffect.active(member.getRank(), picksOf(player));
	}

	/**
	 * The gem glow {@code player} shows, or null.
	 * @param rankTitles from {@link #clanRankTitles}
	 */
	public GemGlow gemFor(ClanChannel channel, Map<Integer, String> rankTitles, Player player)
	{
		ClanChannelMember member = clanMember(channel, player);
		return member == null ? null : GemGlow.shown(member.getRank(), rankTitles, picksOf(player));
	}

	/** Which halves of their gem glow {@code player} shows: both unless they switched one off. */
	public Set<GemGlow.Part> gemPartsFor(Player player)
	{
		return GemGlow.Part.shown(picksOf(player));
	}

	private ClanChannelMember clanMember(ClanChannel channel, Player player)
	{
		if (channel == null || player == null || !player.isClanMember() || player.getName() == null)
		{
			return null;
		}
		return channel.findMember(player.getName());
	}

	/** null if they never shared any picks */
	private List<String> picksOf(Player player)
	{
		return player == client.getLocalPlayer()
			? localPicks()
			: partyPicks.get(MemberCosmetics.key(player.getName()));
	}
}
