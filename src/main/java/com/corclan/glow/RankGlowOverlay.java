package com.corclan.glow;

import com.corclan.CorClanConfig;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.clan.ClanChannel;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

/**
 * Outlines clan members in the game world by the glow they picked ({@link GlowPicks}). Drawn on this
 * client only; players without the plugin see nothing.
 * For now the only outline is the Owner's {@link GlowStyle#holy}.
 */
public class RankGlowOverlay extends Overlay
{
	private final Client client;
	private final CorClanConfig config;
	private final ModelOutlineRenderer outlines;
	private final GlowPicks picks;

	@Inject
	RankGlowOverlay(Client client, CorClanConfig config, ModelOutlineRenderer outlines, GlowPicks picks)
	{
		this.client = client;
		this.config = config;
		this.outlines = outlines;
		this.picks = picks;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(PRIORITY_HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.rankGlow())
		{
			return null;
		}
		ClanChannel channel = client.getClanChannel();
		if (channel == null)
		{
			return null;
		}
		long now = System.currentTimeMillis();
		for (Player player : client.getTopLevelWorldView().players())
		{
			if (picks.effectsFor(channel, player).contains(GlowEffect.GOLD_OUTLINE))
			{
				for (GlowStyle.Layer layer : GlowStyle.holy(now))
				{
					outlines.drawOutline(player, layer.width, layer.color, layer.feather);
				}
			}
		}
		return null;
	}
}
