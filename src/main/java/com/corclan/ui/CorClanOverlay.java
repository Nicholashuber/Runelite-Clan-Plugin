package com.corclan.ui;

import com.corclan.CorClanConfig;
import com.corclan.CorClanPlugin;
import com.corclan.gz.BroadcastRecord;
import com.corclan.gz.GzStats;
import com.corclan.gz.GzTracker;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * Small on-screen box: your own gz counts, the broadcast currently accepting gz's with a countdown,
 * and a flash for a few seconds whenever a gz is counted.
 */
public class CorClanOverlay extends OverlayPanel
{
	/** How long the "+1 gz" flash stays on screen. */
	static final long FLASH_MILLIS = 4000L;

	private static final Color GREEN = new Color(0x4CAF50);
	private static final Color GOLD = new Color(0xFFC107);

	private final CorClanPlugin plugin;
	private final CorClanConfig config;

	@Inject
	CorClanOverlay(CorClanPlugin plugin, CorClanConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		panelComponent.setPreferredSize(new Dimension(150, 0));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showOverlay())
		{
			return null;
		}
		GzTracker tracker = plugin.getTracker();
		long now = System.currentTimeMillis();
		String me = plugin.localPlayerName();

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("CoR gz")
			.color(ColorScheme.BRAND_ORANGE)
			.build());

		if (me != null)
		{
			GzStats session = tracker.getSession();
			GzStats allTime = tracker.getAllTime();
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Gave")
				.right(session.getGiven().getOrDefault(me, 0) + " / " + allTime.getGiven().getOrDefault(me, 0))
				.build());
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Got")
				.right(session.getReceived().getOrDefault(me, 0) + " / " + allTime.getReceived().getOrDefault(me, 0))
				.build());
		}

		long windowMillis = config.gzWindowSeconds() * 1000L;
		BroadcastRecord window = tracker.currentWindow(now, windowMillis);
		if (window != null)
		{
			long left = Math.max(0, (window.getTimestamp() + windowMillis - now) / 1000L);
			panelComponent.getChildren().add(LineComponent.builder()
				.left("gz " + window.getSubject())
				.leftColor(GOLD)
				.right(window.getGzCount() + " (" + left + "s)")
				.rightColor(GOLD)
				.build());
		}

		if (tracker.getLastGzGiver() != null && now - tracker.getLastGzTime() < FLASH_MILLIS)
		{
			String subject = tracker.getLastGzSubject();
			panelComponent.getChildren().add(LineComponent.builder()
				.left("+1 " + tracker.getLastGzGiver())
				.leftColor(GREEN)
				.right(subject != null ? "> " + subject : "")
				.rightColor(GREEN)
				.build());
		}

		return super.render(graphics);
	}
}
