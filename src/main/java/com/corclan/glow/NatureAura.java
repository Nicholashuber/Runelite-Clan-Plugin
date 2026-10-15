package com.corclan.glow;

import com.corclan.CorClanConfig;
import com.corclan.icons.MemberCosmetics;
import java.awt.Color;
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
 * JoeBoob's nature aura, the green elemental twin of {@link DaylightAura}: a Zanaris fairy ring with leaves springing
 * up around him, the same swirl as DAYLlGHT's turned leaf green ({@link TintedFx.Tint#GREEN}), the Druidic spirit's
 * bloom (each graphic restarted the moment it ends) and emerald sparkles drawn by {@link SignatureGlowOverlay}. Every plugin user sees it (no clan sync needed) while "Show clan
 * rank glows" is on. JoeBoob can switch each part off ({@link NatureGlow}); others see that choice once it arrives
 * through the clan server. Drawn on this client only. All methods must run on the client thread.
 */
@Singleton
public class NatureAura
{
	/** Players who wear the aura, as {@link MemberCosmetics#key} names. */
	public static final Set<String> WEARERS = Collections.singleton("joeboob");

	/** Our own spot-anim slots (storm 3082-3084, Lavasockz 3090, DAYLlGHT 3092). */
	static final int BLOOM_SLOT = 3096;
	static final int LEAVES_SLOT = 3098;
	/** The fairy ring's mushrooms and flowers coming up around a player, as when you step through to Zanaris. */
	static final int DEFAULT_RING = SpotanimID.FAIRY_FLOWER_RING;
	/** Leaves sprouting from the ground (the Druidic spirit's bloom is built from these). */
	static final int DEFAULT_LEAVES = SpotanimID.LEAVES_GROWING;
	static final int DEFAULT_SWIRL = DaylightAura.DEFAULT_SWIRL;
	/** The fairy ring plays at this share of the game's speed, and at twice its size. */
	static final double RING_SPEED = 0.35;
	static final double RING_SCALE = 2.0;
	/** The green swirl turns at this share of the game's speed and climbs this many times slower than DAYLlGHT's. */
	static final double SWIRL_SPEED = 0.35;
	static final long SWIRL_RISE_MILLIS = DaylightAura.SWIRL_RISE_MILLIS * 3;
	/** The bloom the Druidic spirit (Blessed sickle) casts around a player: fungi and leaves springing up. */
	static final int DEFAULT_BLOOM = SpotanimID.DRUIDICSPIRIT_BLOOM_PLAYER_SPOTANIM;
	/** Green stars with a pale lime point, as many as DAYLlGHT's. */
	static final GemStyle.Sparkle SPARKLE = new GemStyle.Sparkle(8, new Color(30, 200, 70), new Color(200, 255, 170), 14);

	private final Client client;
	private final CorClanConfig config;
	/** wearer key -> the parts they shared through the clan server; replaced whole */
	private Map<String, List<String>> sharedPicks = Collections.emptyMap();

	private final TintedFx swirl;
	/** the fairy ring, slowed down and doubled in size, so drawn by us rather than played by the game */
	private final TintedFx ring;
	// the graphics in use this session; ::naturefx swaps them to try other ids
	private int bloomId = DEFAULT_BLOOM;
	private int bloomHeight;
	private int leavesId = DEFAULT_LEAVES;

	@Inject
	NatureAura(Client client, CorClanConfig config)
	{
		this.client = client;
		this.config = config;
		swirl = new TintedFx(client, TintedFx.Tint.GREEN, DEFAULT_SWIRL, DaylightAura.DEFAULT_SWIRL_RISE,
			SWIRL_RISE_MILLIS, SWIRL_SPEED, 1.0);
		ring = new TintedFx(client, TintedFx.Tint.NONE, DEFAULT_RING, 0, 1L, RING_SPEED, RING_SCALE);
	}

	/** True for a clan member whose name wears the aura. */
	static boolean wears(Player player)
	{
		return player != null && player.isClanMember() && player.getName() != null
			&& WEARERS.contains(MemberCosmetics.key(player.getName()));
	}

	/** The parts of the aura {@code player} shows: empty unless they wear it. */
	public Set<NatureGlow> effectsFor(Player player)
	{
		if (!wears(player))
		{
			return Collections.emptySet();
		}
		if (player == client.getLocalPlayer())
		{
			Set<NatureGlow> mine = EnumSet.noneOf(NatureGlow.class);
			for (NatureGlow glow : NatureGlow.values())
			{
				if (NatureGlow.picked(config, glow))
				{
					mine.add(glow);
				}
			}
			return mine;
		}
		return NatureGlow.active(sharedPicks.get(MemberCosmetics.key(player.getName())));
	}

	/** The parts this client's player switched on, as ids for the clan server. */
	public List<String> localPicks()
	{
		List<String> ids = new ArrayList<>();
		for (NatureGlow glow : NatureGlow.values())
		{
			if (NatureGlow.picked(config, glow))
			{
				ids.add(glow.id);
			}
		}
		return ids;
	}

	/** Everyone's glow picks arrived from the clan server; only wearers' are kept. */
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

	/** Every client frame: keeps the green swirl on whoever shows it, and the fairy ring, leaves and bloom going. */
	public void onClientTick()
	{
		if (!config.rankGlow())
		{
			return;
		}
		List<Player> withSwirl = new ArrayList<>();
		List<Player> withRing = new ArrayList<>();
		for (Player player : client.getTopLevelWorldView().players())
		{
			if (!wears(player))
			{
				continue;
			}
			Set<NatureGlow> on = effectsFor(player);
			if (on.contains(NatureGlow.LEAF_SWIRL))
			{
				withSwirl.add(player);
			}
			boolean fairy = on.contains(NatureGlow.FAIRY_RING);
			if (fairy)
			{
				withRing.add(player);
			}
			keepPlaying(player, LEAVES_SLOT, leavesId, 0, fairy);
			keepPlaying(player, BLOOM_SLOT, bloomId, bloomHeight, on.contains(NatureGlow.BLOOM));
		}
		swirl.update(withSwirl);
		ring.update(withRing);
	}

	/** Restarts a graphic the moment it ends (the game empties its slot), or takes it off. */
	private static void keepPlaying(Player player, int slot, int id, int height, boolean on)
	{
		boolean playing = player.getSpotAnims().get(slot) != null;
		if (on && !playing)
		{
			player.createSpotAnim(slot, id, height, 0);
		}
		else if (!on && playing)
		{
			player.removeSpotAnim(slot);
		}
	}

	/**
	 * ::naturefx: uses another game graphic for one part this session, like ::dayfx. Local only.
	 * swirl [rise] is turned green; ring is slowed and doubled like the fairy ring; leaves and bloom [height] are
	 * played as the game draws them.
	 * @param label what to call the graphic in chat
	 * @return the chat line to show, or null if the arguments are wrong
	 */
	public String preview(String part, int id, int extra, String label)
	{
		if (part.equalsIgnoreCase(NatureGlow.LEAF_SWIRL.shortName))
		{
			return swirl.use(id, extra)
				? "CoR: swirl set to " + label + " for this session"
				: "CoR: " + label + " couldn't be read from the game cache";
		}
		if (part.equalsIgnoreCase(NatureGlow.FAIRY_RING.shortName))
		{
			return ring.use(id, 0)
				? "CoR: ring set to " + label + " for this session (slowed and doubled)"
				: "CoR: " + label + " couldn't be read from the game cache";
		}
		int slot;
		if (part.equalsIgnoreCase("leaves"))
		{
			leavesId = id;
			slot = LEAVES_SLOT;
		}
		else if (part.equalsIgnoreCase(NatureGlow.BLOOM.shortName))
		{
			bloomId = id;
			bloomHeight = extra;
			slot = BLOOM_SLOT;
		}
		else
		{
			return null;
		}
		Player me = client.getLocalPlayer();
		if (me != null)
		{
			me.createSpotAnim(slot, id, slot == BLOOM_SLOT ? extra : 0, 0);
		}
		return "CoR: " + part.toLowerCase() + " set to " + label + " for this session";
	}

	/** Removes our graphics from everyone (glows turned off or plugin stopped). */
	public void clear()
	{
		swirl.clear();
		ring.clear();
		for (Player player : client.getTopLevelWorldView().players())
		{
			if (player != null)
			{
				player.removeSpotAnim(BLOOM_SLOT);
				player.removeSpotAnim(LEAVES_SLOT);
			}
		}
	}
}
