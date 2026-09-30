package com.corclan.glow;

import com.corclan.CorClanConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanRank;
import net.runelite.api.gameval.SpotanimID;

/**
 * The Owner's storm, on top of {@link RankGlowOverlay}'s outline: an upside-down cloud overhead
 * ({@link StormCloud}), frequent lightning strikes, electric shocks, and a rare big strike, each at
 * random moments. Graphics come from the game cache. Client-side only: other players never see
 * it. All methods must run on the client thread.
 */
@Singleton
public class HolyAura
{
	/** One graphic replayed at random intervals in its own spot-anim slot. */
	public static final class Effect
	{
		/** chat command that previews / swaps this effect, e.g. "glowzap" for ::glowzap */
		public final String command;
		public final String label;
		/** our own slot, so we never replace a graphic the game is playing on the player */
		final int slot;
		/** random gap between plays, in game ticks (0.6 s each), inclusive */
		final int minTicks;
		final int maxTicks;
		public final int defaultSpotAnimId;
		int spotAnimId;
		int nextTick;

		Effect(String command, String label, int slot, int spotAnimId, int minTicks, int maxTicks)
		{
			this.command = command;
			this.label = label;
			this.slot = slot;
			this.defaultSpotAnimId = spotAnimId;
			this.spotAnimId = spotAnimId;
			this.minTicks = minTicks;
			this.maxTicks = maxTicks;
			this.nextTick = minTicks;
		}

		/** Ticks until the next play: uniformly minTicks..maxTicks, so it never falls into a rhythm. */
		int delay(Random random)
		{
			return minTicks + random.nextInt(maxTicks - minTicks + 1);
		}
	}

	/** Frequent: the Hallowed Sepulchre statue's lightning strike, every 1.2 - 3.6 s. */
	final Effect strikes = new Effect("glowzap", "strikes", 3083, SpotanimID.HALLOWED_STATUE_LIGHTNING_STRIKE, 2, 6);
	/** The Killerwatts' electric shock crackling on the body, every 1.8 - 4.8 s. */
	final Effect shock = new Effect("glowshock", "shock", 3084, SpotanimID.SKELETON_KILLERWATT_ELECTRICSHOCK, 3, 8);
	/** Rare: Lucien's lightning, every 10 - 20 s. */
	final Effect bigStrike = new Effect("glowstrike", "big strike", 3082, SpotanimID.LUC2_LUCIEN_LIGHTNING_SPOT, 17, 33);

	final List<Effect> effects = Collections.unmodifiableList(Arrays.asList(strikes, shock, bigStrike));

	private final Client client;
	private final CorClanConfig config;
	private final StormCloud cloud;
	private final Random random = new Random();
	private int ticks;
	/** Owners in view as of the last game tick. */
	private final List<Player> owners = new ArrayList<>();

	@Inject
	HolyAura(Client client, CorClanConfig config, StormCloud cloud)
	{
		this.client = client;
		this.config = config;
		this.cloud = cloud;
	}

	/** True if {@code player} holds the clan's Owner rank. */
	static boolean isOwner(ClanChannel channel, Player player)
	{
		if (channel == null || player == null || !player.isClanMember() || player.getName() == null)
		{
			return false;
		}
		ClanChannelMember member = channel.findMember(player.getName());
		return member != null && ClanRank.OWNER.equals(member.getRank());
	}

	public void onGameTick()
	{
		if (!config.rankGlow())
		{
			return;
		}
		ticks++;
		findOwners();
		cloud.update(owners);

		for (Effect effect : effects)
		{
			if (ticks < effect.nextTick)
			{
				continue;
			}
			effect.nextTick = ticks + effect.delay(random);
			for (Player player : owners)
			{
				player.createSpotAnim(effect.slot, effect.spotAnimId, 0, 0);
			}
		}
	}

	/** Every client frame (20 ms): keeps the cloud over its Owner as they move. */
	public void onClientTick()
	{
		if (config.rankGlow())
		{
			cloud.follow();
		}
	}

	private void findOwners()
	{
		owners.clear();
		ClanChannel channel = client.getClanChannel();
		for (Player player : client.getTopLevelWorldView().players())
		{
			if (isOwner(channel, player))
			{
				owners.add(player);
			}
		}
	}

	/** The effect previewed by chat command {@code command} (without "::"), or null. */
	public Effect forCommand(String command)
	{
		for (Effect effect : effects)
		{
			if (effect.command.equals(command))
			{
				return effect;
			}
		}
		return null;
	}

	/** Plays {@code id} on your own character now and uses it for {@code effect} this session. */
	public void preview(Effect effect, int id)
	{
		effect.spotAnimId = id;
		Player me = client.getLocalPlayer();
		if (me != null)
		{
			me.createSpotAnim(effect.slot, id, 0, 0);
		}
	}

	/** Removes the cloud and our graphics from everyone (glow turned off or plugin stopped). */
	public void clear()
	{
		owners.clear();
		cloud.clear();
		for (Player player : client.getTopLevelWorldView().players())
		{
			if (player != null)
			{
				for (Effect effect : effects)
				{
					player.removeSpotAnim(effect.slot);
				}
			}
		}
	}
}
