package com.corclan.glow;

import com.corclan.CorClanConfig;
import com.corclan.icons.MemberCosmetics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.gameval.SpotanimID;

/**
 * Lavasockz's aura, built in by name like his founder and dev icons: the Flames of Zamorak burning around him
 * nonstop, restarted the moment each play ends, so it keeps going while he walks, on top of the molten outline
 * drawn by {@link SignatureGlowOverlay}. Every plugin user sees it
 * (no clan sync needed) while "Show clan rank glows" is on. Lavasockz can switch it off ({@link DevGlow}); other
 * players see his choice once it arrives through the clan server (clan sync), and the flames until then.
 * Graphics come from the game cache; drawn on this client only. All methods must run on the client thread.
 */
@Singleton
public class LavaAura
{
	/** Players who wear the aura, as {@link MemberCosmetics#key} names. */
	public static final Set<String> WEARERS = Collections.singleton("lavasockz");

	/** Our own spot-anim slot (Ray's storm uses 3082-3084), so we never replace a graphic the game is playing. */
	static final int SLOT = 3090;
	static final int FLAMES = SpotanimID.ZAMORAK_FLAME;

	private final Client client;
	private final CorClanConfig config;
	/** wearer key -> the parts they shared through the clan server; replaced whole */
	private Map<String, List<String>> sharedPicks = Collections.emptyMap();

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
		return DevGlow.active(sharedPicks.get(MemberCosmetics.key(player.getName())));
	}

	/** The parts this client's player switched on, as ids for the clan server. */
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

	/**
	 * Everyone's glow picks arrived from the clan server; only wearers' are kept.
	 * @param picks {@link MemberCosmetics#key} -> the glow ids that player shared
	 */
	public void setSharedPicks(Map<String, List<String>> picks)
	{
		Map<String, List<String>> kept = new HashMap<>();
		for (String wearer : WEARERS)
		{
			List<String> glows = picks == null ? null : picks.get(wearer);
			if (glows != null)
			{
				kept.put(wearer, glows);
			}
		}
		sharedPicks = kept;
	}

	public void clearSharedPicks()
	{
		sharedPicks = Collections.emptyMap();
	}

	/**
	 * Every client frame (20 ms): the game drops a spot anim from its slot when it finishes, so an empty slot
	 * means the flames just ended and start again straight away. Spot anims ride on the player, so they follow
	 * him as he walks. Anyone who switched the flames off has them removed.
	 */
	public void onClientTick()
	{
		if (!config.rankGlow())
		{
			return;
		}
		for (Player player : client.getTopLevelWorldView().players())
		{
			if (!wears(player))
			{
				continue;
			}
			boolean on = effectsFor(player).contains(DevGlow.ZAMORAK_FLAMES);
			boolean playing = player.getSpotAnims().get(SLOT) != null;
			if (on && !playing)
			{
				player.createSpotAnim(SLOT, FLAMES, 0, 0);
			}
			else if (!on && playing)
			{
				player.removeSpotAnim(SLOT);
			}
		}
	}

	/** ::lavafx: plays the flames on your own character once, to show them off. */
	public void preview()
	{
		Player me = client.getLocalPlayer();
		if (me != null)
		{
			me.createSpotAnim(SLOT, FLAMES, 0, 0);
		}
	}

	/** Removes our flames from everyone (glows turned off or plugin stopped). */
	public void clear()
	{
		for (Player player : client.getTopLevelWorldView().players())
		{
			if (player != null)
			{
				player.removeSpotAnim(SLOT);
			}
		}
	}
}
