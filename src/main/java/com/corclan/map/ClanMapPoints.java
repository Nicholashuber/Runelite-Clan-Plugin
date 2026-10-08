package com.corclan.map;

import com.corclan.party.CorLocation;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.ImageUtil;

/** Draws CoR party members who share their position on the in-game world map. Call on the client thread. */
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
		// the same red rhino shown for CoR Clan in the Plugin Hub
		this.icon = ImageUtil.loadImageResource(ClanMapPoints.class, "/com/corclan/map_marker.png");
	}

	/**
	 * Replaces every marker, plus your own marker labelled "You" so you can see sharing is on even when
	 * nobody else is on the map.
	 *
	 * @param others name -> latest position
	 * @param self your own position, or null when you are not sharing
	 */
	public void update(Map<String, CorLocation> others, CorLocation self)
	{
		clear();
		if (self != null)
		{
			int n = others.size();
			add(self, "You", "You: sharing with the CoR party, " + n + (n == 1 ? " clanmate" : " clanmates") + " on the map");
		}
		others.forEach((name, loc) -> add(loc, name, name + " (W" + loc.getWorld() + ")" + (loc.isWilderness() ? ", Wilderness" : "")));
	}

	private void add(CorLocation p, String label, String tooltip)
	{
		WorldMapPoint point = WorldMapPoint.builder()
			.worldPoint(new WorldPoint(p.getX(), p.getY(), p.getPlane()))
			.image(icon)
			.name(label)
			.tooltip(tooltip)
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
}
