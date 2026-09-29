package com.corclan.gz;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

/**
 * Runs of consecutive weeks at #1 on the weekly gz podium. A week with no recorded winner
 * (nobody gz'd, or this client wasn't running) breaks every run.
 */
public final class Streaks
{
	/** One or more players (ties) holding a run of {@link #weeks} weeks. */
	public static final class Streak
	{
		public final Set<String> players;
		public final int weeks;

		Streak(Set<String> players, int weeks)
		{
			this.players = Collections.unmodifiableSet(players);
			this.weeks = weeks;
		}
	}

	private Streaks()
	{
	}

	/** Longest run in the whole history, or null if no week has a winner yet. */
	public static Streak longest(List<WeekResult> history, ZoneId zone)
	{
		int best = 0;
		Set<String> holders = new TreeSet<>();
		for (Map<String, Integer> runs : runsPerWeek(history, zone))
		{
			for (Map.Entry<String, Integer> e : runs.entrySet())
			{
				if (e.getValue() > best)
				{
					best = e.getValue();
					holders.clear();
				}
				if (e.getValue() == best)
				{
					holders.add(e.getKey());
				}
			}
		}
		return best == 0 ? null : new Streak(holders, best);
	}

	/**
	 * Longest run still alive: it must include the week right before {@code currentWeekStart}.
	 * @return null if last week had no winner
	 */
	public static Streak current(List<WeekResult> history, long currentWeekStart, ZoneId zone)
	{
		List<WeekResult> sorted = sorted(history);
		if (sorted.isEmpty() || nextWeek(sorted.get(sorted.size() - 1).getWeekStart(), zone) != currentWeekStart)
		{
			return null;
		}
		List<Map<String, Integer>> runs = runsPerWeek(sorted, zone);
		Map<String, Integer> last = runs.get(runs.size() - 1);
		if (last.isEmpty())
		{
			return null;
		}
		int best = Collections.max(last.values());
		Set<String> holders = new TreeSet<>();
		last.forEach((name, weeks) ->
		{
			if (weeks == best)
			{
				holders.add(name);
			}
		});
		return new Streak(holders, best);
	}

	/** For each week (oldest first), how many weeks in a row each of its winners has been #1. */
	private static List<Map<String, Integer>> runsPerWeek(List<WeekResult> history, ZoneId zone)
	{
		List<Map<String, Integer>> out = new ArrayList<>();
		Map<String, Integer> prevRuns = new HashMap<>();
		Long prevStart = null;
		for (WeekResult week : sorted(history))
		{
			boolean adjacent = prevStart != null && nextWeek(prevStart, zone) == week.getWeekStart();
			Map<String, Integer> runs = new HashMap<>();
			for (String winner : week.getWinners())
			{
				runs.put(winner, (adjacent ? prevRuns.getOrDefault(winner, 0) : 0) + 1);
			}
			out.add(runs);
			prevRuns = runs;
			prevStart = week.getWeekStart();
		}
		return out;
	}

	private static List<WeekResult> sorted(List<WeekResult> history)
	{
		List<WeekResult> sorted = new ArrayList<>(history);
		sorted.sort(Comparator.comparingLong(WeekResult::getWeekStart));
		return sorted;
	}

	/** Start of the week after the one starting at {@code weekStart}; DST-safe (weeks aren't always 7*24h). */
	static long nextWeek(long weekStart, ZoneId zone)
	{
		return GzTracker.weekStartOf(weekStart + TimeUnit.DAYS.toMillis(8), zone);
	}
}
