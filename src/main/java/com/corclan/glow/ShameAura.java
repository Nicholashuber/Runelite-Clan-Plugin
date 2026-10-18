package com.corclan.glow;

import com.corclan.CorClanConfig;
import com.corclan.clan.ClanLeaders;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.gameval.SpotanimID;

/**
 * The Kephri dung: ::shame &lt;name&gt; from one of the {@link ClanLeaders} drops Kephri's dung on that clanmate's head
 * over and over until a leader takes it off with ::unshame. Shames travel through the clan server (clan sync), so every
 * plugin user with sync on sees it, the shamed player included. Only the master switch "Show clan rank glows"
 * hides it: unlike the other auras, the wearer cannot switch their dung off. Drawn on this client only.
 * All methods must run on the client thread.
 */
@Singleton
public class ShameAura
{
	/** Our own spot-anim slot (Ray's storm uses 3082-3084, Lavasockz's flames 3090, DAYLlGHT's smoke 3092). */
	static final int SLOT = 3094;
	static final int DUNG = SpotanimID.KEPHRI_DUNG_DROP;
	/** a fresh dung drops this often, in game ticks (0.6 s each) */
	static final int DROP_TICKS = 5;

	private final Client client;
	private final CorClanConfig config;
	private Shames shames = Shames.NONE;
	private int ticks;

	@Inject
	ShameAura(Client client, CorClanConfig config)
	{
		this.client = client;
		this.config = config;
	}

	/** The clan server's list arrived (or our own ::shame / ::unshame went through). */
	public void setShames(Shames shames)
	{
		this.shames = shames == null ? Shames.NONE : shames;
	}

	public Shames getShames()
	{
		return shames;
	}

	/** Clan sync went off or the plugin stopped: nobody wears the dung any more. */
	public void clearShames()
	{
		shames = Shames.NONE;
		clear();
	}

	/** Every game tick: drops the dung on every shamed clanmate in view, yourself included. */
	public void onGameTick()
	{
		if (!config.rankGlow() || ++ticks % DROP_TICKS != 0)
		{
			return;
		}
		long now = System.currentTimeMillis();
		Player me = client.getLocalPlayer();
		boolean previewing = now < previewUntil;
		if (previewing && me != null)
		{
			me.createSpotAnim(SLOT, DUNG, 0, 0);
		}
		if (shames.isEmpty(now))
		{
			return;
		}
		for (Player player : client.getTopLevelWorldView().players())
		{
			if (player != null && player.getName() != null && (player == me || player.isClanMember())
				&& !(previewing && player == me) && shames.isShamed(player.getName(), now))
			{
				player.createSpotAnim(SLOT, DUNG, 0, 0);
			}
		}
	}

	/** How long ::dungfx keeps dropping the dung on you. */
	static final long PREVIEW_MILLIS = 30_000L;
	/** when the ::dungfx preview ends, epoch millis; 0 = not previewing */
	private long previewUntil;

	/**
	 * ::dungfx: drops the dung on your own character the way a real shame does, every {@link #DROP_TICKS} ticks, for
	 * 30 seconds; typed again it stops. Local only.
	 * @return true if the preview started, false if it was stopped
	 */
	public boolean preview()
	{
		long now = System.currentTimeMillis();
		if (now < previewUntil)
		{
			previewUntil = 0;
			Player me = client.getLocalPlayer();
			if (me != null)
			{
				me.removeSpotAnim(SLOT);
			}
			return false;
		}
		previewUntil = now + PREVIEW_MILLIS;
		ticks = 0;
		Player me = client.getLocalPlayer();
		if (me != null)
		{
			me.createSpotAnim(SLOT, DUNG, 0, 0);
		}
		return true;
	}

	/** Removes our dung from everyone (glows turned off or plugin stopped). */
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
