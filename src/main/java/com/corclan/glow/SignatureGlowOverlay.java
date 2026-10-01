package com.corclan.glow;

import com.corclan.CorClanConfig;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

/**
 * Outlines for named members' signature auras (for now Lavasockz's molten outline, see {@link MoltenStyle}).
 * Drawn on this client only; players without the plugin see nothing.
 */
public class SignatureGlowOverlay extends Overlay
{
	private final Client client;
	private final CorClanConfig config;
	private final ModelOutlineRenderer outlines;

	@Inject
	SignatureGlowOverlay(Client client, CorClanConfig config, ModelOutlineRenderer outlines)
	{
		this.client = client;
		this.config = config;
		this.outlines = outlines;
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
		long now = System.currentTimeMillis();
		for (Player player : client.getTopLevelWorldView().players())
		{
			if (LavaAura.wears(player))
			{
				for (GlowStyle.Layer layer : MoltenStyle.molten(now))
				{
					outlines.drawOutline(player, layer.width, layer.color, layer.feather);
				}
			}
		}
		return null;
	}
}
