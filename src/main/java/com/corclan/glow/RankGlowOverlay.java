package com.corclan.glow;

import com.corclan.CorClanConfig;
import com.corclan.icons.MemberCosmetics;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * One outline per player: the Owner's {@link GlowStyle#holy}, else their {@link GemStyle} gem glow, with
 * twinkling stars from Diamond up. Players wearing a signature outline (drawn by
 * {@link SignatureGlowOverlay}) get no gem glow on top of it.
 */
public class RankGlowOverlay extends Overlay
{
	/** how far a star's inner corners sit from its centre, as a share of its points' reach */
	private static final double STAR_WAIST = 0.22;
	/** how far past the body's bounds stars may appear, as a share of its width and height on each side */
	private static final double STAR_SPILL = 0.15;

	private final Client client;
	private final CorClanConfig config;
	private final ModelOutlineRenderer outlines;
	private final GlowPicks picks;
	private final LavaAura lavaAura;

	@Inject
	RankGlowOverlay(Client client, CorClanConfig config, ModelOutlineRenderer outlines, GlowPicks picks, LavaAura lavaAura)
	{
		this.client = client;
		this.config = config;
		this.outlines = outlines;
		this.picks = picks;
		this.lavaAura = lavaAura;
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
		Map<Integer, String> rankTitles = picks.clanRankTitles();
		for (Player player : client.getTopLevelWorldView().players())
		{
			if (picks.effectsFor(channel, player).contains(GlowEffect.GOLD_OUTLINE))
			{
				draw(player, GlowStyle.holy(now));
				continue;
			}
			if (lavaAura.effectsFor(player).contains(DevGlow.MOLTEN_OUTLINE))
			{
				continue;
			}
			GemGlow gem = picks.gemFor(channel, rankTitles, player);
			if (gem != null)
			{
				Set<GemGlow.Part> parts = picks.gemPartsFor(player);
				if (parts.contains(GemGlow.Part.OUTLINE))
				{
					draw(player, GemStyle.layers(gem, now));
				}
				GemStyle.Sparkle sparkle = GemStyle.sparkle(gem);
				if (sparkle != null && parts.contains(GemGlow.Part.SPARKLES))
				{
					twinkle(graphics, player, sparkle, now);
				}
			}
		}
		return null;
	}

	private void draw(Player player, List<GlowStyle.Layer> layers)
	{
		for (GlowStyle.Layer layer : layers)
		{
			outlines.drawOutline(player, layer.width, layer.color, layer.feather);
		}
	}

	/** Stars blooming and fading over the player's body. */
	static void twinkle(Graphics2D graphics, Player player, GemStyle.Sparkle sparkle, long now)
	{
		Shape hull = player.getConvexHull();
		if (hull == null)
		{
			return;
		}
		// let the stars spill a little past the body
		Rectangle bounds = hull.getBounds();
		bounds.grow((int) (bounds.width * STAR_SPILL), (int) (bounds.height * STAR_SPILL));
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		long seed = MemberCosmetics.key(player.getName()).hashCode();
		for (Twinkle.Star star : Twinkle.stars(seed, sparkle.count, now))
		{
			double r = sparkle.radius * star.size;
			if (r < 1)
			{
				continue;
			}
			double x = bounds.x + star.x * bounds.width;
			double y = bounds.y + star.y * bounds.height;
			graphics.setColor(alpha(sparkle.color, 0.45 * star.size));
			graphics.fill(new Ellipse2D.Double(x - r * 0.8, y - r * 0.8, r * 1.6, r * 1.6));
			// a smaller diagonal star behind the main one makes it read as a sparkle
			graphics.setColor(alpha(sparkle.color, 0.8 * star.size));
			graphics.fill(star(x, y, r * 0.55, Math.PI / 4));
			graphics.setColor(alpha(sparkle.color, star.size));
			graphics.fill(star(x, y, r, 0));
			graphics.setColor(alpha(sparkle.core, star.size));
			graphics.fill(new Ellipse2D.Double(x - r * 0.2, y - r * 0.2, r * 0.4, r * 0.4));
		}
	}

	/** A four-pointed star centred on (x, y), points reaching {@code r}, turned {@code angle} radians. */
	private static Shape star(double x, double y, double r, double angle)
	{
		double w = r * STAR_WAIST;
		Path2D.Double path = new Path2D.Double();
		for (int i = 0; i < 8; i++)
		{
			double reach = i % 2 == 0 ? r : w * Math.sqrt(2);
			double a = angle - Math.PI / 2 + i * Math.PI / 4;
			double px = x + reach * Math.cos(a);
			double py = y + reach * Math.sin(a);
			if (i == 0)
			{
				path.moveTo(px, py);
			}
			else
			{
				path.lineTo(px, py);
			}
		}
		path.closePath();
		return path;
	}

	private static Color alpha(Color color, double opacity)
	{
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) Math.round(255 * opacity));
	}
}
