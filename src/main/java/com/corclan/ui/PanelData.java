package com.corclan.ui;

import com.corclan.gz.BroadcastRecord;
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

	public PanelData(String mine, String summary, String syncStatus, boolean clanWide,
		List<Map.Entry<String, Integer>> givers, List<Map.Entry<String, Integer>> receivers, List<BroadcastRecord> recent)
	{
		this.mine = mine;
		this.summary = summary;
		this.syncStatus = syncStatus;
		this.clanWide = clanWide;
		this.givers = givers;
		this.receivers = receivers;
		this.recent = recent;
	}
}
