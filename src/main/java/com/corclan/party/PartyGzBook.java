package com.corclan.party;

import com.corclan.gz.GzStats;
import com.corclan.gz.GzTracker;
import java.util.HashMap;
import java.util.Map;

/**
 * Gz totals that CoR party members shared about themselves. Each member only ever reports their own numbers
 * (RuneLite does not allow crowdsourcing data about other players), and this client's own counts are used
 * for everyone else. Per name, the higher of the two is shown. Serialized with Gson so it survives restarts.
 */
public class PartyGzBook
{
	private static final long HOUR_MILLIS = 3_600_000L;

	/** One member's own totals. */
	static final class Self
	{
		int given;
		int received;
		int weekly;
		/** the week {@link #weekly} belongs to */
		long weekStart;
	}

	private long weekStart;
	/** display name -> what that member reported about themselves */
	private Map<String, Self> members = new HashMap<>();

	/** Weekly counts from an older week stop counting. */
	public void rollWeek(long currentWeekStart)
	{
		weekStart = currentWeekStart;
	}

	/**
	 * Stores what {@code name} reported about themselves. Weekly counts can't be higher than the hourly gz cap
	 * allows for the time since the week began.
	 *
	 * @return true if anything they report went up
	 */
	public boolean merge(String name, CorGzCounts msg, long now)
	{
		if (name == null || name.isEmpty() || msg == null)
		{
			return false;
		}
		Self self = members.computeIfAbsent(name, n -> new Self());
		boolean changed = false;
		if (msg.getGiven() > self.given)
		{
			self.given = msg.getGiven();
			changed = true;
		}
		if (msg.getReceived() > self.received)
		{
			self.received = msg.getReceived();
			changed = true;
		}
		if (msg.getWeekStart() == weekStart && now >= weekStart)
		{
			long hours = (now - weekStart) / HOUR_MILLIS + 1;
			int weekly = (int) Math.min(msg.getWeekly(), hours * GzTracker.MAX_GZ_PER_HOUR);
			if (self.weekStart != weekStart)
			{
				self.weekStart = weekStart;
				self.weekly = 0;
			}
			if (weekly > self.weekly)
			{
				self.weekly = weekly;
				changed = true;
			}
		}
		return changed;
	}

	/** This client's counts, with each party member's own report where it is higher. */
	public Map<String, Integer> given(Map<String, Integer> local)
	{
		Map<String, Integer> out = new HashMap<>(local);
		members.forEach((name, self) -> raise(out, name, self.given));
		return out;
	}

	public Map<String, Integer> received(Map<String, Integer> local)
	{
		Map<String, Integer> out = new HashMap<>(local);
		members.forEach((name, self) -> raise(out, name, self.received));
		return out;
	}

	public Map<String, Integer> weekly(Map<String, Integer> local)
	{
		Map<String, Integer> out = new HashMap<>(local);
		members.forEach((name, self) ->
		{
			if (self.weekStart == weekStart)
			{
				raise(out, name, self.weekly);
			}
		});
		return out;
	}

	/** What we send: only our own numbers, as this client counted them. */
	public CorGzCounts message(String me, GzStats localWeekly, GzStats localAllTime)
	{
		return new CorGzCounts(weekStart,
			localAllTime.getGiven().getOrDefault(me, 0),
			localAllTime.getReceived().getOrDefault(me, 0),
			localWeekly.getGiven().getOrDefault(me, 0));
	}

	public boolean isEmpty()
	{
		return members.isEmpty();
	}

	public void clear()
	{
		members = new HashMap<>();
	}

	/** Gson leaves missing fields null (and older saves had a different shape, which is dropped). */
	public PartyGzBook normalized()
	{
		if (members == null)
		{
			members = new HashMap<>();
		}
		members.values().removeIf(self -> self == null);
		return this;
	}

	private static void raise(Map<String, Integer> counts, String name, int value)
	{
		if (value > 0)
		{
			counts.merge(name, value, Math::max);
		}
	}
}
