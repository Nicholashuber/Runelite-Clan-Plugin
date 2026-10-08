package com.corclan.sync;

import com.corclan.gz.GzStats;
import com.corclan.gz.WeekResult;
import com.corclan.icons.MemberCosmetics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The gz numbers shown everywhere (podiums, givers list, GZ King, weekly trophies, streaks, !rank): the clan
 * server's while clan sync is on and its answer is recent, otherwise the ones this client counted. The local
 * tracker keeps counting either way, so turning sync off loses nothing. Immutable.
 */
public final class GzView
{
	/** A server answer older than this (five missed refreshes) is no longer shown; local counts take over. */
	public static final long STALE_MILLIS = 600_000L;

	/** true when these are the clan server's numbers */
	public final boolean clanWide;
	public final Map<String, Integer> given;
	public final Map<String, Integer> received;
	/** gz's given this week */
	public final Map<String, Integer> weekly;
	/** #1 giver(s) of past weeks, for streaks */
	public final List<WeekResult> weekResults;
	/** {@link MemberCosmetics#key} -> count, for lookups by a name as chat spells it */
	private final Map<String, Integer> givenByKey = new HashMap<>();
	private final Map<String, Integer> receivedByKey = new HashMap<>();

	private GzView(boolean clanWide, Map<String, Integer> given, Map<String, Integer> received,
		Map<String, Integer> weekly, List<WeekResult> weekResults)
	{
		this.clanWide = clanWide;
		this.given = Collections.unmodifiableMap(given);
		this.received = Collections.unmodifiableMap(received);
		this.weekly = Collections.unmodifiableMap(weekly);
		this.weekResults = Collections.unmodifiableList(weekResults);
		given.forEach((name, n) -> givenByKey.merge(MemberCosmetics.key(name), n, Math::max));
		received.forEach((name, n) -> receivedByKey.merge(MemberCosmetics.key(name), n, Math::max));
	}

	/**
	 * @param server           the last answer from the clan server, or null when clan sync is off or nothing
	 *                         has loaded yet
	 * @param serverLoadedAt   when {@code server} arrived, epoch millis
	 * @param currentWeekStart start of the running week (Sunday 00:00 UTC), epoch millis
	 */
	public static GzView choose(ClanSnapshot server, long serverLoadedAt, long now,
		GzStats localAllTime, GzStats localWeekly, List<WeekResult> localWeekResults, long currentWeekStart)
	{
		if (server != null && now - serverLoadedAt < STALE_MILLIS)
		{
			return new GzView(true, server.getGiven(), server.getReceived(), server.weeklyGiven(currentWeekStart),
				server.getWeekResults());
		}
		return new GzView(false, new HashMap<>(localAllTime.getGiven()), new HashMap<>(localAllTime.getReceived()),
			new HashMap<>(localWeekly.getGiven()), new ArrayList<>(localWeekResults));
	}

	/** All-time gz's given by {@code name}, however its case and spaces are written. */
	public int givenOf(String name)
	{
		return givenByKey.getOrDefault(MemberCosmetics.key(name), 0);
	}

	public int receivedOf(String name)
	{
		return receivedByKey.getOrDefault(MemberCosmetics.key(name), 0);
	}
}
