package com.corclan.glow;

import com.corclan.CorClanConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.gameval.SpotanimID;

/**
 * The Owner storm effects, on top of {@link RankGlowOverlay}'s outline: an upside-down cloud overhead
 * ({@link StormCloud}), frequent lightning strikes, electric shocks, and a rare big strike, each at
 * random moments, on every player whose picks ({@link GlowPicks}) include them. Graphics come from
 * the game cache. Drawn by this client only: players without the plugin never see them. All methods
 * must run on the client thread.
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
		/** the glow setting that switches it on for a wearer */
		final GlowEffect glow;
		/** our own slot, so we never replace a graphic the game is playing on the player */
		final int slot;
		/** random gap between plays, in game ticks (0.6 s each), inclusive */
		final int minTicks;
		final int maxTicks;
		public final int defaultSpotAnimId;
		int spotAnimId;
		int nextTick;

		Effect(String command, String label, GlowEffect glow, int slot, int spotAnimId, int minTicks, int maxTicks)
		{
			this.command = command;
			this.label = label;
			this.glow = glow;
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
	final Effect strikes = new Effect("glowzap", "strikes", GlowEffect.LIGHTNING_STRIKES, 3083, SpotanimID.HALLOWED_STATUE_LIGHTNING_STRIKE, 2, 6);
	/** The Killerwatts' electric shock crackling on the body, every 1.8 - 4.8 s. */
	final Effect shock = new Effect("glowshock", "shock", GlowEffect.SHOCKS, 3084, SpotanimID.SKELETON_KILLERWATT_ELECTRICSHOCK, 3, 8);
	/** Rare: Lucien's lightning, every 10 - 20 s. */
	final Effect bigStrike = new Effect("glowstrike", "big strike", GlowEffect.BIG_STRIKE, 3082, SpotanimID.LUC2_LUCIEN_LIGHTNING_SPOT, 17, 33);

	final List<Effect> effects = Collections.unmodifiableList(Arrays.asList(strikes, shock, bigStrike));

	private final Client client;
	private final CorClanConfig config;
	private final StormCloud cloud;
	private final GlowPicks picks;
	private final Random random = new Random();
	private int ticks;
	/** Players in view and the storm effects they show, as of the last game tick. */
	private final Map<Player, Set<GlowEffect>> wearers = new HashMap<>();

	@Inject
	HolyAura(Client client, CorClanConfig config, StormCloud cloud, GlowPicks picks)
	{
		this.client = client;
		this.config = config;
		this.cloud = cloud;
		this.picks = picks;
	}

	public void onGameTick()
	{
		if (!config.rankGlow())
		{
			return;
		}
		ticks++;
		findWearers();
		List<Player> cloudy = new ArrayList<>();
		wearers.forEach((player, shown) ->
		{
			if (shown.contains(GlowEffect.STORM_CLOUD))
			{
				cloudy.add(player);
			}
		});
		cloud.update(cloudy);

		for (Effect effect : effects)
		{
			if (ticks < effect.nextTick)
			{
				continue;
			}
			effect.nextTick = ticks + effect.delay(random);
			wearers.forEach((player, shown) ->
			{
				if (shown.contains(effect.glow))
				{
					player.createSpotAnim(effect.slot, effect.spotAnimId, 0, 0);
				}
			});
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

	private void findWearers()
	{
		wearers.clear();
		ClanChannel channel = client.getClanChannel();
		for (Player player : client.getTopLevelWorldView().players())
		{
			Set<GlowEffect> shown = picks.effectsFor(channel, player);
			if (!shown.isEmpty())
			{
				wearers.put(player, shown);
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
		wearers.clear();
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
