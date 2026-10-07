package com.corclan.sync;

import com.corclan.gz.GzTracker;
import com.corclan.gz.WeekResult;
import com.corclan.icons.MemberCosmetics;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One {@code GET /v1/clan} answer, turned into what the plugin shows: gz counts per player, the past weeks'
 * winners, everyone's icons and titles, and their glow picks keyed by name. Immutable, so it can be built on
 * an OkHttp thread and read on the client thread.
 */
public final class ClanSnapshot
{
	/** Same limits the server puts on a player's glow picks. */
	static final int MAX_GLOWS = 32;
	static final int MAX_GLOW_ID_LENGTH = 32;

	/** start of the week the weekly counts belong to, epoch millis; 0 when the server's value is unreadable */
	private final long weekStart;
	private final Map<String, Integer> given;
	private final Map<String, Integer> received;
	private final Map<String, Integer> weekly;
	private final List<WeekResult> weekResults;
	private final List<SyncModels.ClanPlayer> players;
	private final Map<String, List<String>> glows;

	private ClanSnapshot(long weekStart, Map<String, Integer> given, Map<String, Integer> received,
		Map<String, Integer> weekly, List<WeekResult> weekResults, List<SyncModels.ClanPlayer> players,
		Map<String, List<String>> glows)
	{
		this.weekStart = weekStart;
		this.given = Collections.unmodifiableMap(given);
		this.received = Collections.unmodifiableMap(received);
		this.weekly = Collections.unmodifiableMap(weekly);
		this.weekResults = Collections.unmodifiableList(weekResults);
		this.players = Collections.unmodifiableList(players);
		this.glows = Collections.unmodifiableMap(glows);
	}

	public static ClanSnapshot of(SyncModels.ClanResponse response)
	{
		Map<String, Integer> given = new HashMap<>();
		Map<String, Integer> received = new HashMap<>();
		Map<String, Integer> weekly = new HashMap<>();
		List<SyncModels.ClanPlayer> players = new ArrayList<>();
		Map<String, List<String>> glows = new HashMap<>();
		for (SyncModels.ClanPlayer p : response.getPlayers())
		{
			if (p == null || MemberCosmetics.key(p.getRsn()).isEmpty())
			{
				continue;
			}
			String name = p.getRsn().trim();
			count(given, name, p.getGiven());
			count(received, name, p.getReceived());
			count(weekly, name, p.getWeeklyGiven());
			players.add(p);
			List<String> picks = picksOf(p);
			if (picks != null)
			{
				glows.put(MemberCosmetics.key(name), picks);
			}
		}

		List<WeekResult> weekResults = new ArrayList<>();
		for (SyncModels.Week week : response.getWeeks())
		{
			long start = week == null ? 0L : parseWeekStart(week.getWeekStart());
			List<String> winners = week == null ? Collections.emptyList() : winners(week.getTop());
			if (start != 0L && !winners.isEmpty())
			{
				weekResults.add(new WeekResult(start, winners));
			}
		}
		return new ClanSnapshot(parseWeekStart(response.getWeekStart()), given, received, weekly, weekResults, players, glows);
	}

	private static void count(Map<String, Integer> counts, String name, int value)
	{
		if (value > 0)
		{
			counts.merge(name, value, Math::max);
		}
	}

	/**
	 * The glow ids a player shared, or null if they never shared any (then viewers show their rank's defaults).
	 * A player's rank and glow picks are sent together, so an empty list only means "picked nothing" when
	 * their rank has been reported too.
	 */
	private static List<String> picksOf(SyncModels.ClanPlayer p)
	{
		List<String> raw = p.getGlows();
		if (raw == null || (raw.isEmpty() && p.getRank() == null))
		{
			return null;
		}
		List<String> picks = new ArrayList<>();
		for (String id : raw)
		{
			if (id != null && !id.isEmpty() && id.length() <= MAX_GLOW_ID_LENGTH && picks.size() < MAX_GLOWS)
			{
				picks.add(id);
			}
		}
		return Collections.unmodifiableList(picks);
	}

	/** Everyone tied for the most gz's in a week's top three, sorted by name. */
	private static List<String> winners(List<SyncModels.Entry> top)
	{
		int max = 0;
		for (SyncModels.Entry e : top)
		{
			if (e != null && e.getRsn() != null)
			{
				max = Math.max(max, e.getCount());
			}
		}
		List<String> out = new ArrayList<>();
		for (SyncModels.Entry e : top)
		{
			if (max > 0 && e != null && e.getRsn() != null && e.getCount() == max && !out.contains(e.getRsn()))
			{
				out.add(e.getRsn());
			}
		}
		Collections.sort(out);
		return out;
	}

	/** @return the Sunday 00:00 UTC on or before the server's ISO-8601 instant, or 0 if it is unreadable */
	static long parseWeekStart(String iso)
	{
		if (iso == null)
		{
			return 0L;
		}
		try
		{
			return GzTracker.weekStartOf(Instant.parse(iso).toEpochMilli(), GzTracker.WEEK_ZONE);
		}
		catch (DateTimeParseException | ArithmeticException e)
		{
			return 0L;
		}
	}

	/** gz's given per player name, all time */
	public Map<String, Integer> getGiven()
	{
		return given;
	}

	public Map<String, Integer> getReceived()
	{
		return received;
	}

	/**
	 * gz's given per player name in the week starting at {@code currentWeekStart}. Empty when the server's
	 * counts are still last week's (a new week just began and the next refresh has not arrived).
	 */
	public Map<String, Integer> weeklyGiven(long currentWeekStart)
	{
		return weekStart != 0L && weekStart < currentWeekStart ? Collections.emptyMap() : weekly;
	}

	/** The #1 giver(s) of each closed week the server sent (the last 12 with any gz's). */
	public List<WeekResult> getWeekResults()
	{
		return weekResults;
	}

	/** Every listed player, for their icons and titles. */
	public List<SyncModels.ClanPlayer> getPlayers()
	{
		return players;
	}

	/** {@link MemberCosmetics#key} -> the glow ids that player shared. Players who shared none are absent. */
	public Map<String, List<String>> getGlows()
	{
		return glows;
	}
}
