package com.corclan.party;

import com.corclan.gz.GzStats;
import com.corclan.gz.GzTracker;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Gz counts shared by CoR party members. Everyone in clan chat sees the same gz's, so the best estimate for a
 * player is the highest count anyone reported (adding them up would count each gz once per viewer).
 * Serialized with Gson so it survives restarts; the weekly part only holds counts for {@link #weekStart}.
 */
public class PartyGzBook
{
	/** Most names per list in one message, to keep party messages small. */
	public static final int MAX_NAMES = 100;
	private static final long HOUR_MILLIS = 3_600_000L;

	private long weekStart;
	private Map<String, Integer> weekly = new HashMap<>();
	private Map<String, Integer> given = new HashMap<>();
	private Map<String, Integer> received = new HashMap<>();

	/** Starts a fresh weekly count when the week changed. */
	public void rollWeek(long currentWeekStart)
	{
		if (currentWeekStart != weekStart)
		{
			weekStart = currentWeekStart;
			weekly = new HashMap<>();
		}
	}

	/**
	 * Merges another member's counts. Names that are not clan members and weekly counts from another week are
	 * ignored, and no weekly count can be higher than the hourly gz cap allows for the time since the week began.
	 *
	 * @return true if any count went up
	 */
	public boolean merge(CorGzCounts msg, long now, Predicate<String> isClanMember)
	{
		if (msg == null)
		{
			return false;
		}
		boolean changed = false;
		if (msg.getWeekStart() == weekStart && now >= weekStart)
		{
			long hours = (now - weekStart) / HOUR_MILLIS + 1;
			int weeklyCap = (int) Math.min(Integer.MAX_VALUE, hours * GzTracker.MAX_GZ_PER_HOUR);
			changed |= mergeInto(weekly, msg.getWeekly(), isClanMember, weeklyCap);
		}
		changed |= mergeInto(given, msg.getGiven(), isClanMember, Integer.MAX_VALUE);
		changed |= mergeInto(received, msg.getReceived(), isClanMember, Integer.MAX_VALUE);
		return changed;
	}

	private static boolean mergeInto(Map<String, Integer> into, Map<String, Integer> from, Predicate<String> isClanMember, int cap)
	{
		if (from == null)
		{
			return false;
		}
		boolean changed = false;
		int added = 0;
		for (Map.Entry<String, Integer> e : from.entrySet())
		{
			if (added >= MAX_NAMES)
			{
				break;
			}
			String name = e.getKey();
			Integer count = e.getValue();
			if (name == null || count == null || count <= 0 || !isClanMember.test(name))
			{
				continue;
			}
			added++;
			int value = Math.min(count, cap);
			if (value > into.getOrDefault(name, 0))
			{
				into.put(name, value);
				changed = true;
			}
		}
		return changed;
	}

	/** This client's counts and the party's, highest per name. */
	public Map<String, Integer> weekly(Map<String, Integer> local)
	{
		return max(local, weekly);
	}

	public Map<String, Integer> given(Map<String, Integer> local)
	{
		return max(local, given);
	}

	public Map<String, Integer> received(Map<String, Integer> local)
	{
		return max(local, received);
	}

	/** The message to send: the merged view, top {@link #MAX_NAMES} of each list. */
	public CorGzCounts message(GzStats localWeekly, GzStats localAllTime)
	{
		return new CorGzCounts(weekStart,
			top(weekly(localWeekly.getGiven())),
			top(given(localAllTime.getGiven())),
			top(received(localAllTime.getReceived())));
	}

	public boolean isEmpty()
	{
		return weekly.isEmpty() && given.isEmpty() && received.isEmpty();
	}

	public void clear()
	{
		weekly = new HashMap<>();
		given = new HashMap<>();
		received = new HashMap<>();
	}

	/** Gson leaves missing fields null. */
	public PartyGzBook normalized()
	{
		if (weekly == null)
		{
			weekly = new HashMap<>();
		}
		if (given == null)
		{
			given = new HashMap<>();
		}
		if (received == null)
		{
			received = new HashMap<>();
		}
		return this;
	}

	private static Map<String, Integer> max(Map<String, Integer> a, Map<String, Integer> b)
	{
		Map<String, Integer> out = new HashMap<>(a);
		b.forEach((name, n) -> out.merge(name, n, Math::max));
		return out;
	}

	private static Map<String, Integer> top(Map<String, Integer> counts)
	{
		Map<String, Integer> out = new HashMap<>();
		List<Map.Entry<String, Integer>> top = GzStats.top(counts, MAX_NAMES);
		for (Map.Entry<String, Integer> e : top)
		{
			out.put(e.getKey(), e.getValue());
		}
		return out;
	}
}
