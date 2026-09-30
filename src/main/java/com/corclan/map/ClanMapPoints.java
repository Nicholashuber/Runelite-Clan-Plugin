package com.corclan.map;

import com.corclan.sync.SyncModels;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.ImageUtil;

/** Draws clanmates who share their position on the in-game world map. Call on the client thread. */
@Singleton
public class ClanMapPoints
{
	private final WorldMapPointManager worldMapPointManager;
	private final BufferedImage icon;
	private final List<WorldMapPoint> points = new ArrayList<>();

	@Inject
	ClanMapPoints(WorldMapPointManager worldMapPointManager)
	{
		this.worldMapPointManager = worldMapPointManager;
		this.icon = ImageUtil.loadImageResource(ClanMapPoints.class, "/com/corclan/panel_icon.png");
	}

	/**
	 * Replaces every marker with the latest positions from the clan server, plus your own marker so
	 * you can see sharing is working even when nobody else is on the map.
	 *
	 * @param self the position that was just accepted by the server, or null
	 */
	public void update(List<SyncModels.PlayerLocation> players, SyncModels.PlayerLocation self)
	{
		clear();
		if (self != null)
		{
			add(self, "You");
		}
		for (SyncModels.PlayerLocation p : players)
		{
			if (p.getRsn() != null)
			{
				add(p, p.getRsn());
			}
		}
	}

	private void add(SyncModels.PlayerLocation p, String label)
	{
		WorldMapPoint point = WorldMapPoint.builder()
			.worldPoint(new WorldPoint(p.getX(), p.getY(), p.getPlane()))
			.image(icon)
			.name(label)
			.tooltip(label + " (W" + p.getWorld() + ")" + (p.isWilderness() ? ", Wilderness" : ""))
			.snapToEdge(true)
			.jumpOnClick(true)
			.build();
		worldMapPointManager.add(point);
		points.add(point);
	}

	public void clear()
	{
		for (WorldMapPoint point : points)
		{
			worldMapPointManager.remove(point);
		}
		points.clear();
	}

	public int count()
	{
		return points.size();
	}
}
