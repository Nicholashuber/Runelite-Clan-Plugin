package com.corclan.ui;

import com.corclan.clan.ClanRoster;
import com.corclan.gz.BroadcastRecord;
import com.corclan.gz.Streaks;
import java.util.List;
import java.util.Map;

/** An immutable snapshot for the sidebar, built on the client thread and drawn on the Swing thread. */
public final class PanelData
{
	final String mine;
	final String summary;
	/** one line about the CoR party */
	final String partyStatus;
	/** one line about the clan map */
	final String mapStatus;
	/** " (this client)" or " (CoR party)", after leaderboard titles */
	final String scope;
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
	/** clan members by in-game rank, highest first; null when not logged in or not in a clan */
	final List<ClanRoster.RankGroup> clanRoster;
	/** every rank the clan has set up, rank number to title (empty when not in a clan) */
	final Map<Integer, String> clanRankTitles;
	/** you are the clan Owner: show the Owner glow section */
	final boolean owner;
	/** you are Lavasockz: show the Dev glow section */
	final boolean dev;
	/** you are DAYLlGHT: show the Founder glow section */
	final boolean founder;
	/** in the CoR party: the panel button says Leave instead of Join */
	final boolean inCorParty;

	public PanelData(String mine, String summary, String partyStatus, String mapStatus, String scope,
		List<Map.Entry<String, Integer>> givers, List<Map.Entry<String, Integer>> receivers, List<BroadcastRecord> recent,
		List<Map.Entry<String, Integer>> weeklyGivers, long weekStart, Streaks.Streak longestStreak, Streaks.Streak currentStreak,
		List<Map.Entry<String, Integer>> allGivers, List<ClanRoster.RankGroup> clanRoster,
		Map<Integer, String> clanRankTitles, boolean owner, boolean dev, boolean founder, boolean inCorParty)
	{
		this.owner = owner;
		this.dev = dev;
		this.founder = founder;
		this.inCorParty = inCorParty;
		this.clanRoster = clanRoster;
		this.clanRankTitles = clanRankTitles;
		this.allGivers = allGivers;
		this.mine = mine;
		this.summary = summary;
		this.partyStatus = partyStatus;
		this.mapStatus = mapStatus;
		this.scope = scope;
		this.givers = givers;
		this.receivers = receivers;
		this.recent = recent;
		this.weeklyGivers = weeklyGivers;
		this.weekStart = weekStart;
		this.longestStreak = longestStreak;
		this.currentStreak = currentStreak;
	}
}
