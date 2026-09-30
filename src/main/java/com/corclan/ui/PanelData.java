package com.corclan.ui;

import com.corclan.gz.BroadcastRecord;
import com.corclan.gz.Streaks;
import java.util.List;
import java.util.Map;

/** An immutable snapshot for the sidebar, built on the client thread and drawn on the Swing thread. */
public final class PanelData
{
	final String mine;
	final String summary;
	final String syncStatus;
	final boolean clanWide;
	final List<Map.Entry<String, Integer>> givers;
	final List<Map.Entry<String, Integer>> receivers;
	final List<BroadcastRecord> recent;
	/** this client's gz givers since {@link #weekStart} (Sunday 00:00 local time) */
	final List<Map.Entry<String, Integer>> weeklyGivers;
	final long weekStart;
	/** consecutive weekly #1 runs; null when there are none */
	final Streaks.Streak longestStreak;
	final Streaks.Streak currentStreak;
	/** every giver this client has counted, highest first */
	final List<Map.Entry<String, Integer>> allGivers;

	public PanelData(String mine, String summary, String syncStatus, boolean clanWide,
		List<Map.Entry<String, Integer>> givers, List<Map.Entry<String, Integer>> receivers, List<BroadcastRecord> recent,
		List<Map.Entry<String, Integer>> weeklyGivers, long weekStart, Streaks.Streak longestStreak, Streaks.Streak currentStreak,
		List<Map.Entry<String, Integer>> allGivers)
	{
		this.allGivers = allGivers;
		this.mine = mine;
		this.summary = summary;
		this.syncStatus = syncStatus;
		this.clanWide = clanWide;
		this.givers = givers;
		this.receivers = receivers;
		this.recent = recent;
		this.weeklyGivers = weeklyGivers;
		this.weekStart = weekStart;
		this.longestStreak = longestStreak;
		this.currentStreak = currentStreak;
	}
}
