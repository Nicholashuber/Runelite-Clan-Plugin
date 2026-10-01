package com.corclan.glow;

import com.corclan.CorClanConfig;
import com.corclan.icons.MemberCosmetics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.gameval.SpotanimID;

/**
 * Lavasockz's "Molten Lord" aura, built in by name like his founder and dev icons: fire bursting at his feet,
 * a Flames of Zamorak pillar now and then, and a rare meteor strike, on top of the molten outline drawn by
 * {@link SignatureGlowOverlay}. Every plugin user sees it (no party needed) while "Show clan rank glows" is on.
 * Lavasockz switches each part ({@link DevGlow}); other players see his choice once it arrives through the
 * CoR party, and the whole aura until then.
 * Graphics come from the game cache; drawn on this client only. All methods must run on the client thread.
 */
@Singleton
public class LavaAura
{
	/** Players who wear the aura, as {@link MemberCosmetics#key} names. */
	public static final Set<String> WEARERS = Collections.singleton("lavasockz");

	/** One graphic replayed at random intervals in its own spot-anim slot. */
	static final class Burst
	{
		final String label;
		final DevGlow glow;
		/** our own slot (Ray's storm uses 3082-3084), so we never replace a graphic the game is playing */
		final int slot;
		final int spotAnimId;
		/** random gap between plays, in game ticks (0.6 s each), inclusive */
		final int minTicks;
		final int maxTicks;
		int nextTick;

		Burst(String label, DevGlow glow, int slot, int spotAnimId, int minTicks, int maxTicks)
		{
			this.label = label;
			this.glow = glow;
			this.slot = slot;
			this.spotAnimId = spotAnimId;
			this.minTicks = minTicks;
			this.maxTicks = maxTicks;
			this.nextTick = minTicks;
		}

		int delay(Random random)
		{
			return minTicks + random.nextInt(maxTicks - minTicks + 1);
		}
	}

	/** Frequent: a burst of camp fire at his feet, every 2.4 - 6 s. */
	final Burst fire = new Burst("fire burst", DevGlow.FIRE_BURSTS, 3090, SpotanimID.CAMP_FIRE_BURST, 4, 10);
	/** A Flames of Zamorak pillar around him, every 9 - 18 s. */
	final Burst pillar = new Burst("flame pillar", DevGlow.FLAME_PILLAR, 3091, SpotanimID.ZAMORAK_FLAME, 15, 30);
	/** Rare: a Wilderness meteor slamming down on him, every 24 - 48 s. */
	final Burst meteor = new Burst("meteor", DevGlow.METEOR, 3092, SpotanimID.WILD_FALLOFF_METEOR_BLAST, 40, 80);

	final List<Burst> bursts = Collections.unmodifiableList(Arrays.asList(fire, pillar, meteor));

	private final Client client;
	private final CorClanConfig config;
	private final Random random = new Random();
	private int ticks;
	/** wearer key -> the parts they shared through the CoR party */
	private final Map<String, List<String>> partyPicks = new HashMap<>();
	/** party member id -> wearer key, so picks go when the member leaves */
	private final Map<Long, String> partyNames = new HashMap<>();

	@Inject
	LavaAura(Client client, CorClanConfig config)
	{
		this.client = client;
		this.config = config;
	}

	/** True for a clan member whose name wears the aura. */
	static boolean wears(Player player)
	{
		return player != null && player.isClanMember() && player.getName() != null
			&& WEARERS.contains(MemberCosmetics.key(player.getName()));
	}

	/** The parts of the aura {@code player} shows: empty unless they wear it. Client thread. */
	public Set<DevGlow> effectsFor(Player player)
	{
		if (!wears(player))
		{
			return Collections.emptySet();
		}
		if (player == client.getLocalPlayer())
		{
			Set<DevGlow> mine = EnumSet.noneOf(DevGlow.class);
			for (DevGlow glow : DevGlow.values())
			{
				if (DevGlow.picked(config, glow))
				{
					mine.add(glow);
				}
			}
			return mine;
		}
		return DevGlow.active(partyPicks.get(MemberCosmetics.key(player.getName())));
	}

	/** The parts this client's player switched on, as ids for the party message. */
	public List<String> localPicks()
	{
		List<String> ids = new ArrayList<>();
		for (DevGlow glow : DevGlow.values())
		{
			if (DevGlow.picked(config, glow))
			{
				ids.add(glow.id);
			}
		}
		return ids;
	}

	/** A party member's glow picks arrived; only wearers' are kept. */
	public void setPartyPicks(long memberId, String name, List<String> glows)
	{
		String key = MemberCosmetics.key(name);
		if (!WEARERS.contains(key))
		{
			return;
		}
		partyNames.put(memberId, key);
		partyPicks.put(key, glows == null ? new ArrayList<>() : new ArrayList<>(glows));
	}

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

	public void onGameTick()
	{
		if (!config.rankGlow())
		{
			return;
		}
		ticks++;
		Map<Player, Set<DevGlow>> wearers = new HashMap<>();
		for (Player player : client.getTopLevelWorldView().players())
		{
			Set<DevGlow> shown = effectsFor(player);
			if (!shown.isEmpty())
			{
				wearers.put(player, shown);
			}
		}
		for (Burst burst : bursts)
		{
			if (ticks < burst.nextTick)
			{
				continue;
			}
			burst.nextTick = ticks + burst.delay(random);
			wearers.forEach((player, shown) ->
			{
				if (shown.contains(burst.glow))
				{
					player.createSpotAnim(burst.slot, burst.spotAnimId, 0, 0);
				}
			});
		}
	}

	/** ::lavafx: plays every part of the aura on your own character once, to show it off. */
	public void preview()
	{
		Player me = client.getLocalPlayer();
		if (me != null)
		{
			for (Burst burst : bursts)
			{
				me.createSpotAnim(burst.slot, burst.spotAnimId, 0, 0);
			}
		}
	}

	/** Removes our graphics from everyone (glows turned off or plugin stopped). */
	public void clear()
	{
		for (Player player : client.getTopLevelWorldView().players())
		{
			if (player != null)
			{
				for (Burst burst : bursts)
				{
					player.removeSpotAnim(burst.slot);
				}
			}
		}
	}
}
