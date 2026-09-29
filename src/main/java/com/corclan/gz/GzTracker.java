package com.corclan.gz;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Attributes gz messages to the most recent clan broadcast while its window is open.
 * Pure Java: the plugin feeds it chat events and persists {@link #getAllTime()} when
 * {@link #onClanChat} reports a change.
 */
public class GzTracker
{
	private final ZoneId zone;
	private GzStats allTime = new GzStats();
	private GzStats session = new GzStats();
	// counts since the most recent Sunday 00:00 in {@link #zone}; rolled over by rollWeek
	private GzStats weekly = new GzStats();
	private long weekStart;
	// #1 giver(s) of each finished week, for streaks
	private List<WeekResult> weekResults = new ArrayList<>();
	private BroadcastRecord current;

	// last counted gz, for the on-screen indicator
	private long lastGzTime;
	private String lastGzGiver;
	private String lastGzSubject;

	public GzTracker()
	{
		this(ZoneId.systemDefault());
	}

	/** @param zone time zone whose Sunday midnight starts a new week */
	public GzTracker(ZoneId zone)
	{
		this.zone = zone;
	}

	/** Epoch millis of the Sunday 00:00 (in {@code zone}) on or before {@code now}. */
	public static long weekStartOf(long now, ZoneId zone)
	{
		return Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
			.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
			.atStartOfDay(zone).toInstant().toEpochMilli();
	}

	/**
	 * Starts a fresh weekly count if {@code now} is in a later week than the current one,
	 * recording the finished week's #1 giver(s) first.
	 * @return true if the weekly stats were reset (caller should persist)
	 */
	public boolean rollWeek(long now)
	{
		long start = weekStartOf(now, zone);
		if (start > weekStart)
		{
			List<String> winners = topGivers(weekly);
			if (weekStart != 0 && !winners.isEmpty())
			{
				weekResults.add(new WeekResult(weekStart, winners));
			}
			weekly = new GzStats();
			weekStart = start;
			return true;
		}
		return false;
	}

	/** Everyone tied for the most gz's given, or empty if nobody gave any. */
	private static List<String> topGivers(GzStats stats)
	{
		List<String> out = new ArrayList<>();
		int max = stats.getGiven().values().stream().mapToInt(Integer::intValue).max().orElse(0);
		if (max > 0)
		{
			stats.getGiven().forEach((name, n) ->
			{
				if (n == max)
				{
					out.add(name);
				}
			});
			Collections.sort(out);
		}
		return out;
	}

	public void loadWeekResults(List<WeekResult> results)
	{
		weekResults = results != null ? new ArrayList<>(results) : new ArrayList<>();
	}

	public List<WeekResult> getWeekResults()
	{
		return weekResults;
	}

	/** Longest run of consecutive weekly #1s ever, or null. */
	public Streaks.Streak longestStreak()
	{
		return Streaks.longest(weekResults, zone);
	}

	/** Run of weekly #1s that includes last week, or null. */
	public Streaks.Streak currentStreak()
	{
		return Streaks.current(weekResults, weekStart, zone);
	}

	public void loadWeekly(GzStats stats, long weekStart)
	{
		weekly = stats != null ? stats : new GzStats();
		this.weekStart = weekStart;
	}

	public GzStats getWeekly()
	{
		return weekly;
	}

	public long getWeekStart()
	{
		return weekStart;
	}

	public long getLastGzTime()
	{
		return lastGzTime;
	}

	public String getLastGzGiver()
	{
		return lastGzGiver;
	}

	/** Who the last gz was credited to, or null if no broadcast window was open. */
	public String getLastGzSubject()
	{
		return lastGzSubject;
	}

	public void load(GzStats stats)
	{
		allTime = stats != null ? stats : new GzStats();
		session = new GzStats();
		current = null;
	}

	public GzStats getAllTime()
	{
		return allTime;
	}

	public GzStats getSession()
	{
		return session;
	}

	public void resetAllTime()
	{
		allTime = new GzStats();
		weekly = new GzStats();
		weekResults = new ArrayList<>();
		current = null;
	}

	public void resetSession()
	{
		session = new GzStats();
	}

	/** @return the broadcast currently accepting gz's, or null if none / expired */
	public BroadcastRecord currentWindow(long now, long windowMillis)
	{
		if (current != null && current.isOpen(now, windowMillis))
		{
			return current;
		}
		return null;
	}

	/**
	 * @param subject the member being congratulated (from {@link BroadcastParser#subjectOf})
	 * @return the record that now accepts gz's
	 */
	public BroadcastRecord onBroadcast(String subject, String text, long now)
	{
		rollWeek(now);
		BroadcastRecord record = new BroadcastRecord(subject, text, now);
		current = record;
		allTime.addRecent(record);
		session.addRecent(record);
		return record;
	}

	/**
	 * @return true if any counter changed (caller should persist + refresh UI)
	 */
	public boolean onClanChat(String sender, String message, long now, Settings settings)
	{
		if (sender == null || sender.isEmpty() || !GzDetector.isGz(message, settings.maxMessageLength))
		{
			return false;
		}
		rollWeek(now);
		allTime.addGiven(sender);
		session.addGiven(sender);
		weekly.addGiven(sender);
		lastGzTime = now;
		lastGzGiver = sender;
		lastGzSubject = null;

		BroadcastRecord window = currentWindow(now, settings.windowMillis);
		if (window != null
			&& !sender.equalsIgnoreCase(window.getSubject())
			&& (!settings.oneGzPerPersonPerBroadcast || !window.getGivers().contains(sender)))
		{
			window.addGz(sender);
			allTime.addReceived(window.getSubject());
			session.addReceived(window.getSubject());
			weekly.addReceived(window.getSubject());
			lastGzSubject = window.getSubject();
		}
		return true;
	}

	/** Snapshot of the config values the tracker needs. */
	public static final class Settings
	{
		public final long windowMillis;
		public final boolean oneGzPerPersonPerBroadcast;
		public final int maxMessageLength;

		public Settings(long windowMillis, boolean oneGzPerPersonPerBroadcast, int maxMessageLength)
		{
			this.windowMillis = windowMillis;
			this.oneGzPerPersonPerBroadcast = oneGzPerPersonPerBroadcast;
			this.maxMessageLength = maxMessageLength;
		}
	}
}
