package com.corclan.glow;

import com.corclan.CorClanConfig;
import com.corclan.icons.MemberCosmetics;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.gameval.SpotanimID;

/**
 * DAYLlGHT's founder aura, built in by name like {@link LavaAura}: a Tormented Demon's fire turned red and a
 * black swirl climbing up them (both {@link TintedFx}), a smoke cloud playing on them nonstop (restarted the
 * moment each play ends), and red gem sparkles drawn by {@link SignatureGlowOverlay}. Every plugin user sees it (no clan sync needed) while "Show clan rank glows" is on.
 * DAYLlGHT can switch each part off ({@link FounderGlow}); other players see their choice once it arrives through
 * the clan server (clan sync), and everything until then. Drawn on this client only. All methods must run on the client thread.
 */
@Singleton
public class DaylightAura
{
	/**
	 * Players who wear the aura, as {@link MemberCosmetics#key} names. DAYLlGHT is spelt with a lowercase L,
	 * not an I. TESTING ONLY: Runny Sharts wears it too so it can be tried without DAYLlGHT's account;
	 * remove before merging.
	 */
	public static final Set<String> WEARERS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
		"dayllght",
		"runny sharts")));

	/** Our own spot-anim slot (Ray's storm uses 3082-3084, Lavasockz's flames 3090). */
	static final int SMOKE_SLOT = 3092;
	/** Tormented Demons are Lucien's undead demons, so the cache calls their graphics LUC2_UNDEAD_DEMON_*. */
	static final int DEFAULT_FLAMES = SpotanimID.LUC2_UNDEAD_DEMON_EXPLOSION_FIRE_SPOT;
	static final int DEFAULT_SWIRL = SpotanimID.WYVERN_CYCLONE_LOOP;
	/** Best match for the Ring of Shadows teleport smoke: the cache does not name the ring, but this is the
	 * shadow teleport added with Desert Treasure II, where the ring comes from. */
	static final int DEFAULT_SMOKE = SpotanimID.VFX_MAHJARRAT_HUMAN_TELEPORT_SHADOW;
	/** The swirl climbs from the feet to about head height, then starts again at the feet. */
	public static final int DEFAULT_SWIRL_RISE = 250;
	static final long SWIRL_RISE_MILLIS = 2400L;
	/** Red stars for the gem sparkle, matching the red flames ({@link TintedFx#tint}), with a hot pink-orange point. */
	static final GemStyle.Sparkle SPARKLE = new GemStyle.Sparkle(8, new Color(225, 20, 20), new Color(255, 150, 120), 14);

	private final Client client;
	private final CorClanConfig config;
	/** wearer key -> the parts they shared through the clan server; replaced whole */
	private Map<String, List<String>> sharedPicks = Collections.emptyMap();

	private final TintedFx flames;
	private final TintedFx swirl;
	// the smoke in use this session; ::dayfx swaps it to try other ids
	private int smokeId = DEFAULT_SMOKE;
	/** how high the smoke plays, in local units above the ground (a character is ~200) */
	private int smokeHeight;

	@Inject
	DaylightAura(Client client, CorClanConfig config)
	{
		this.client = client;
		this.config = config;
		flames = new TintedFx(client, TintedFx.Tint.RED, DEFAULT_FLAMES, 0, 1L);
		swirl = new TintedFx(client, TintedFx.Tint.BLACK, DEFAULT_SWIRL, DEFAULT_SWIRL_RISE, SWIRL_RISE_MILLIS);
	}

	/** True for a clan member whose name wears the aura. */
	static boolean wears(Player player)
	{
		return player != null && player.isClanMember() && player.getName() != null
			&& WEARERS.contains(MemberCosmetics.key(player.getName()));
	}

	/** The parts of the aura {@code player} shows: empty unless they wear it. Client thread. */
	public Set<FounderGlow> effectsFor(Player player)
	{
		if (!wears(player))
		{
			return Collections.emptySet();
		}
		if (player == client.getLocalPlayer())
		{
			Set<FounderGlow> mine = EnumSet.noneOf(FounderGlow.class);
			for (FounderGlow glow : FounderGlow.values())
			{
				if (FounderGlow.picked(config, glow))
				{
					mine.add(glow);
				}
			}
			return mine;
		}
		return FounderGlow.active(sharedPicks.get(MemberCosmetics.key(player.getName())));
	}

	/** The parts this client's player switched on, as ids for the clan server. */
	public List<String> localPicks()
	{
		List<String> ids = new ArrayList<>();
		for (FounderGlow glow : FounderGlow.values())
		{
			if (FounderGlow.picked(config, glow))
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
	 * Every client frame (20 ms): keeps the red flames and black swirl on whoever shows them, and the smoke
	 * going. The game drops a spot anim from its slot when it finishes, so an empty slot means the smoke just
	 * ended and starts again straight away. Parts switched off are removed.
	 */
	public void onClientTick()
	{
		if (!config.rankGlow())
		{
			return;
		}
		List<Player> withFlames = new ArrayList<>();
		List<Player> withSwirl = new ArrayList<>();
		for (Player player : client.getTopLevelWorldView().players())
		{
			if (!wears(player))
			{
				continue;
			}
			Set<FounderGlow> on = effectsFor(player);
			if (on.contains(FounderGlow.DEMON_FLAMES))
			{
				withFlames.add(player);
			}
			if (on.contains(FounderGlow.BLACK_SWIRL))
			{
				withSwirl.add(player);
			}
			keepPlaying(player, SMOKE_SLOT, smokeId, smokeHeight, on.contains(FounderGlow.SMOKE_CLOUD));
		}
		flames.update(withFlames);
		swirl.update(withSwirl);
	}

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
	 * ::dayfx: uses another game graphic for one part this session, to try out effects. Local only.
	 * <ul>
	 * <li>flames|swirl &lt;id&gt;: any id in {@link TintedFx#CATALOG}, recolored red or black; for the swirl an
	 * optional third number is how far it climbs (0 stays at the feet)</li>
	 * <li>smoke &lt;id&gt; [height]: any spot anim id, played as the game draws it (and once on you now)</li>
	 * </ul>
	 * @return the chat line to show, or null if the arguments are wrong
	 */
	public String preview(String part, int id, int extra)
	{
		if (part.equalsIgnoreCase(FounderGlow.DEMON_FLAMES.shortName) || part.equalsIgnoreCase(FounderGlow.BLACK_SWIRL.shortName))
		{
			boolean isFlames = part.equalsIgnoreCase(FounderGlow.DEMON_FLAMES.shortName);
			TintedFx fx = isFlames ? flames : swirl;
			if (!fx.use(id, isFlames ? 0 : extra))
			{
				return "CoR: " + id + " isn't in the list. Try: " + catalogIds();
			}
			return "CoR: " + part.toLowerCase() + " set to " + id + " (" + TintedFx.CATALOG.get(id).name + ") for this session";
		}
		if (part.equalsIgnoreCase(FounderGlow.SMOKE_CLOUD.shortName))
		{
			smokeId = id;
			smokeHeight = extra;
			Player me = client.getLocalPlayer();
			if (me != null)
			{
				me.createSpotAnim(SMOKE_SLOT, id, extra, 0);
			}
			return "CoR: smoke set to " + id + " for this session";
		}
		return null;
	}

	/** The ids ::dayfx flames and swirl accept, for the chat. */
	static String catalogIds()
	{
		StringBuilder ids = new StringBuilder();
		for (int id : TintedFx.CATALOG.keySet())
		{
			ids.append(ids.length() == 0 ? "" : ", ").append(id);
		}
		return ids.toString();
	}

	/** Removes our graphics from everyone (glows turned off or plugin stopped). */
	public void clear()
	{
		flames.clear();
		swirl.clear();
		for (Player player : client.getTopLevelWorldView().players())
		{
			if (player != null)
			{
				player.removeSpotAnim(SMOKE_SLOT);
			}
		}
	}
}
