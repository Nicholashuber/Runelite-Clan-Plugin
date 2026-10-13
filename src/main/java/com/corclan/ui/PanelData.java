package com.corclan.ui;

import com.corclan.clan.ClanRoster;
import com.corclan.clan.Promotions;
import com.corclan.raid.RaidAd;
import com.corclan.raid.RaidListing;
import com.corclan.glow.GemGlow;
import com.corclan.gz.BroadcastRecord;
import com.corclan.sync.Announcement;
import com.corclan.sync.RecentPromotion;
import com.corclan.gz.Streaks;
import java.util.List;
import java.util.Map;

/** An immutable snapshot for the sidebar, built on the client thread and drawn on the Swing thread. */
public final class PanelData
{
	final String mine;
	final String summary;
	/** one line about clan sync */
	final String syncStatus;
	/** one line about the CoR party */
	final String partyStatus;
	/** one line about the clan map */
	final String mapStatus;
	/** the gz numbers are the clan server's (clan sync on and loaded), not this client's own counts */
	final boolean clanWide;
	final List<Map.Entry<String, Integer>> givers;
	final List<Map.Entry<String, Integer>> receivers;
	final List<BroadcastRecord> recent;
	/** gz givers since {@link #weekStart} (Sunday 00:00 UTC) */
	final List<Map.Entry<String, Integer>> weeklyGivers;
	final long weekStart;
	/** consecutive weekly #1 runs; null when there are none */
	final Streaks.Streak longestStreak;
	final Streaks.Streak currentStreak;
	/** every giver counted, highest first */
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
	/** the banner clan leaders set, or null when there is none (or clan sync is off) */
	final Announcement announcement;
	/** the banner's line while there is no announcement (sync off, not loaded yet, nothing posted) */
	final String announcementHint;
	/** you are a clan leader with clan sync on: the banner has an Edit button */
	final boolean canEditAnnouncement;
	/** in the CoR party: the panel button says Leave instead of Join */
	final boolean inCorParty;
	/** your character name, or null when not logged in */
	final String myName;
	/** your clan rank title (or "Rank N" when it has none), or null when not in a clan or not loaded yet */
	final String myRankTitle;
	/** the highest Gem League glow your rank unlocks, or null */
	final GemGlow myGem;
	/** your gz counts: given and received, all time and this session (0 when not logged in) */
	final int myGiven, myGivenToday, myReceived, myReceivedToday;
	/** you are JoeBoob: show the Nature glow section */
	final boolean nature;
	/** you are clan staff: the rank card's Glow switch also covers the gold twinkle */
	final boolean staff;
	/** the Gem League promotion assistant; null unless you are clan staff (rank 100 up to the Owner) */
	final Promotions.Result promotions;
	/** the clan's latest promotions for everyone (clan sync on), newest first; empty otherwise */
	final List<RecentPromotion> recentPromotions;
	/** every open raid finder group, newest first (empty with clan sync off) */
	final List<RaidListing> raidBoard;
	/** our own raid finder group as last posted, or null */
	final RaidAd myRaid;
	/** why we can't post a raid group right now (clan sync off, logged out), or null */
	final String raidProblem;

	public PanelData(String mine, String summary, String syncStatus, String partyStatus, String mapStatus, boolean clanWide,
		List<Map.Entry<String, Integer>> givers, List<Map.Entry<String, Integer>> receivers, List<BroadcastRecord> recent,
		List<Map.Entry<String, Integer>> weeklyGivers, long weekStart, Streaks.Streak longestStreak, Streaks.Streak currentStreak,
		List<Map.Entry<String, Integer>> allGivers, List<ClanRoster.RankGroup> clanRoster,
		Map<Integer, String> clanRankTitles, boolean owner, boolean dev, boolean founder, boolean inCorParty,
		Announcement announcement, String announcementHint, boolean canEditAnnouncement, String myName, String myRankTitle,
		GemGlow myGem, int myGiven, int myGivenToday, int myReceived, int myReceivedToday,
		boolean nature, boolean staff, Promotions.Result promotions,
		List<RecentPromotion> recentPromotions, List<RaidListing> raidBoard, RaidAd myRaid, String raidProblem)
	{
		this.raidBoard = raidBoard;
		this.myRaid = myRaid;
		this.raidProblem = raidProblem;
		this.recentPromotions = recentPromotions;
		this.promotions = promotions;
		this.nature = nature;
		this.staff = staff;
		this.myGiven = myGiven;
		this.myGivenToday = myGivenToday;
		this.myReceived = myReceived;
		this.myReceivedToday = myReceivedToday;
		this.myName = myName;
		this.myRankTitle = myRankTitle;
		this.myGem = myGem;
		this.owner = owner;
		this.dev = dev;
		this.founder = founder;
		this.inCorParty = inCorParty;
		this.announcement = announcement;
		this.announcementHint = announcementHint;
		this.canEditAnnouncement = canEditAnnouncement;
		this.clanRoster = clanRoster;
		this.clanRankTitles = clanRankTitles;
		this.allGivers = allGivers;
		this.mine = mine;
		this.summary = summary;
		this.syncStatus = syncStatus;
		this.partyStatus = partyStatus;
		this.mapStatus = mapStatus;
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
