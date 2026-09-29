package com.corclan.gz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.Test;

public class StreaksTest
{
	private static final ZoneId UTC = ZoneOffset.UTC;
	// Sundays
	private static final LocalDate W1 = LocalDate.of(2026, 9, 6);

	private static long week(int n)
	{
		return W1.plusWeeks(n).atStartOfDay(UTC).toInstant().toEpochMilli();
	}

	private static WeekResult won(int n, String... winners)
	{
		return new WeekResult(week(n), Arrays.asList(winners));
	}

	private static Set<String> names(String... n)
	{
		return new TreeSet<>(Arrays.asList(n));
	}

	@Test
	public void noHistoryNoStreak()
	{
		assertNull(Streaks.longest(new ArrayList<>(), UTC));
		assertNull(Streaks.current(new ArrayList<>(), week(0), UTC));
	}

	@Test
	public void longestRunOfConsecutiveWeeks()
	{
		List<WeekResult> h = Arrays.asList(won(0, "Alice"), won(1, "Alice"), won(2, "Bob"), won(3, "Alice"));
		Streaks.Streak s = Streaks.longest(h, UTC);
		assertEquals(names("Alice"), s.players);
		assertEquals(2, s.weeks);
	}

	@Test
	public void missingWeekBreaksTheRun()
	{
		// week 2 has no result (nobody gz'd / client not running)
		List<WeekResult> h = Arrays.asList(won(0, "Alice"), won(1, "Alice"), won(3, "Alice"), won(4, "Alice"), won(5, "Alice"));
		assertEquals(3, Streaks.longest(h, UTC).weeks);
	}

	@Test
	public void tiesShareTheWeekAndTheRecord()
	{
		List<WeekResult> h = Arrays.asList(won(0, "Alice", "Bob"), won(1, "Alice", "Bob"), won(2, "Carol"));
		Streaks.Streak s = Streaks.longest(h, UTC);
		assertEquals(names("Alice", "Bob"), s.players);
		assertEquals(2, s.weeks);
	}

	@Test
	public void currentStreakMustIncludeLastWeek()
	{
		List<WeekResult> h = Arrays.asList(won(0, "Bob"), won(1, "Alice"), won(2, "Alice"));
		Streaks.Streak now = Streaks.current(h, week(3), UTC);
		assertEquals(names("Alice"), now.players);
		assertEquals(2, now.weeks);

		// a week later with no new winner recorded, the run is over
		assertNull(Streaks.current(h, week(4), UTC));
		// but the record stands
		assertEquals(2, Streaks.longest(h, UTC).weeks);
	}

	@Test
	public void orderOfHistoryDoesNotMatter()
	{
		List<WeekResult> h = Arrays.asList(won(2, "Alice"), won(0, "Alice"), won(1, "Alice"));
		assertEquals(3, Streaks.longest(h, UTC).weeks);
	}

	@Test
	public void weeksAcrossDaylightSavingAreStillConsecutive()
	{
		// US clocks go back on Sun 1 Nov 2026, so that week isn't exactly 7*24h long
		ZoneId ny = ZoneId.of("America/New_York");
		long oct25 = LocalDate.of(2026, 10, 25).atStartOfDay(ny).toInstant().toEpochMilli();
		long nov1 = LocalDate.of(2026, 11, 1).atStartOfDay(ny).toInstant().toEpochMilli();
		long nov8 = LocalDate.of(2026, 11, 8).atStartOfDay(ny).toInstant().toEpochMilli();
		List<WeekResult> h = Arrays.asList(
			new WeekResult(oct25, Arrays.asList("Alice")),
			new WeekResult(nov1, Arrays.asList("Alice")),
			new WeekResult(nov8, Arrays.asList("Alice")));
		assertEquals(3, Streaks.longest(h, ny).weeks);
	}

	@Test
	public void trackerRecordsWinnerWhenWeekEnds()
	{
		GzTracker.Settings settings = new GzTracker.Settings(90_000L, true, 40);
		GzTracker t = new GzTracker(UTC);
		long day = 24 * 3600_000L;
		t.onClanChat("Alice", "gz", week(0) + day, settings);
		t.onClanChat("Alice", "gz", week(0) + 2 * day, settings);
		t.onClanChat("Bob", "gz", week(0) + 3 * day, settings);
		assertEquals(0, t.getWeekResults().size());

		t.onClanChat("Bob", "gz", week(1) + day, settings);
		assertEquals(1, t.getWeekResults().size());
		assertEquals(Arrays.asList("Alice"), t.getWeekResults().get(0).getWinners());
		assertEquals(1, t.currentStreak().weeks);

		t.rollWeek(week(2) + day);
		assertEquals(Arrays.asList("Bob"), t.getWeekResults().get(1).getWinners());
		assertEquals(names("Bob"), t.currentStreak().players);

		t.resetAllTime();
		assertNull(t.longestStreak());
	}
}
