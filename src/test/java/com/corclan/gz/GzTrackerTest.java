package com.corclan.gz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.Test;

public class GzTrackerTest
{
	private static final GzTracker.Settings SETTINGS = new GzTracker.Settings(90_000L, true, 40);

	@Test
	public void attributesGzToOpenBroadcast()
	{
		GzTracker t = new GzTracker();
		t.onBroadcast("Zezima", "Zezima has received a drop: Twisted bow", 1_000L);

		assertTrue(t.onClanChat("Nick", "gz", 2_000L, SETTINGS));
		assertTrue(t.onClanChat("Bob", "GZZZ", 3_000L, SETTINGS));

		assertEquals(1, (int) t.getAllTime().getGiven().get("Nick"));
		assertEquals(1, (int) t.getAllTime().getGiven().get("Bob"));
		assertEquals(2, (int) t.getAllTime().getReceived().get("Zezima"));
		assertEquals(2, t.getAllTime().getRecent().get(0).getGzCount());
		assertEquals(2, t.getSession().totalGiven());
	}

	@Test
	public void windowExpires()
	{
		GzTracker t = new GzTracker();
		t.onBroadcast("Zezima", "drop", 1_000L);
		assertTrue(t.onClanChat("Nick", "gz", 1_000L + 90_000L + 1, SETTINGS));

		// given still counts, received does not
		assertEquals(1, (int) t.getAllTime().getGiven().get("Nick"));
		assertNull(t.getAllTime().getReceived().get("Zezima"));
		assertNull(t.currentWindow(200_000L, 90_000L));
	}

	@Test
	public void oneGzPerPersonPerBroadcast()
	{
		GzTracker t = new GzTracker();
		t.onBroadcast("Zezima", "drop", 1_000L);
		t.onClanChat("Nick", "gz", 2_000L, SETTINGS);
		t.onClanChat("Nick", "gz again", 3_000L, SETTINGS);

		assertEquals(2, (int) t.getAllTime().getGiven().get("Nick"));
		assertEquals(1, (int) t.getAllTime().getReceived().get("Zezima"));

		GzTracker.Settings noDedupe = new GzTracker.Settings(90_000L, false, 40);
		t.onClanChat("Nick", "gz", 4_000L, noDedupe);
		assertEquals(2, (int) t.getAllTime().getReceived().get("Zezima"));
	}

	@Test
	public void selfGzDoesNotCountAsReceived()
	{
		GzTracker t = new GzTracker();
		t.onBroadcast("Zezima", "drop", 1_000L);
		t.onClanChat("Zezima", "gz me", 2_000L, SETTINGS);
		assertNull(t.getAllTime().getReceived().get("Zezima"));
		assertEquals(1, (int) t.getAllTime().getGiven().get("Zezima"));
	}

	@Test
	public void newBroadcastReplacesWindow()
	{
		GzTracker t = new GzTracker();
		t.onBroadcast("A", "a", 1_000L);
		t.onBroadcast("B", "b", 2_000L);
		t.onClanChat("Nick", "gz", 3_000L, SETTINGS);
		assertNull(t.getAllTime().getReceived().get("A"));
		assertEquals(1, (int) t.getAllTime().getReceived().get("B"));
	}

	@Test
	public void nonGzIsIgnoredAndRecentIsCapped()
	{
		GzTracker t = new GzTracker();
		assertFalse(t.onClanChat("Nick", "anyone for tob?", 1_000L, SETTINGS));
		for (int i = 0; i < 20; i++)
		{
			t.onBroadcast("P" + i, "x", i);
		}
		assertEquals(GzStats.MAX_RECENT, t.getAllTime().getRecent().size());
		assertEquals("P19", t.getAllTime().getRecent().get(0).getSubject());
		assertNull(t.getAllTime().topGiver());
	}

	@Test
	public void lastGzIsRecordedForIndicator()
	{
		GzTracker t = new GzTracker();
		t.onClanChat("Nick", "gz", 5L, SETTINGS);
		assertEquals(5L, t.getLastGzTime());
		assertEquals("Nick", t.getLastGzGiver());
		assertNull(t.getLastGzSubject());

		t.onBroadcast("Zezima", "drop", 10L);
		t.onClanChat("Bob", "grats", 11L, SETTINGS);
		assertEquals("Bob", t.getLastGzGiver());
		assertEquals("Zezima", t.getLastGzSubject());
	}

	@Test
	public void topGiverAndReset()
	{
		GzTracker t = new GzTracker();
		t.onClanChat("Nick", "gz", 1L, SETTINGS);
		t.onClanChat("Nick", "gz", 2L, SETTINGS);
		t.onClanChat("Bob", "gz", 3L, SETTINGS);
		assertEquals("Nick", t.getAllTime().topGiver());
		t.resetAllTime();
		assertEquals(0, t.getAllTime().totalGiven());
	}

	private static long utc(String iso)
	{
		return Instant.parse(iso).toEpochMilli();
	}

	@Test
	public void weekStartsOnSundayMidnight()
	{
		long sunday = utc("2026-09-27T00:00:00Z");
		assertEquals(sunday, GzTracker.weekStartOf(sunday, ZoneOffset.UTC));
		assertEquals(sunday, GzTracker.weekStartOf(utc("2026-09-29T15:00:00Z"), ZoneOffset.UTC));
		assertEquals(sunday, GzTracker.weekStartOf(utc("2026-10-03T23:59:59Z"), ZoneOffset.UTC));
		assertEquals(utc("2026-10-04T00:00:00Z"), GzTracker.weekStartOf(utc("2026-10-04T00:00:00Z"), ZoneOffset.UTC));
		// local zone decides: Sunday 01:00 in New York is still the week that started the previous Sunday there
		assertEquals(utc("2026-09-27T04:00:00Z"),
			GzTracker.weekStartOf(utc("2026-10-04T03:00:00Z"), ZoneId.of("America/New_York")));
	}

	@Test
	public void weeklyCountsResetOnSunday()
	{
		GzTracker t = new GzTracker(ZoneOffset.UTC);
		t.onBroadcast("Zezima", "drop", utc("2026-10-03T23:58:00Z"));
		t.onClanChat("Nick", "gz", utc("2026-10-03T23:59:00Z"), SETTINGS);
		assertEquals(1, t.getWeekly().totalGiven());
		assertEquals(1, t.getWeekly().totalReceived());
		assertEquals(utc("2026-09-27T00:00:00Z"), t.getWeekStart());

		// first gz after Sunday midnight starts a fresh week; all-time keeps everything
		t.onClanChat("Bob", "gz", utc("2026-10-04T00:00:30Z"), SETTINGS);
		assertEquals(utc("2026-10-04T00:00:00Z"), t.getWeekStart());
		assertEquals(1, t.getWeekly().totalGiven());
		assertEquals(Integer.valueOf(1), t.getWeekly().getGiven().get("Bob"));
		assertNull(t.getWeekly().getGiven().get("Nick"));
		assertEquals(2, t.getAllTime().totalGiven());
	}

	@Test
	public void savedWeekSurvivesRestartUntilItEnds()
	{
		GzTracker t = new GzTracker(ZoneOffset.UTC);
		GzStats saved = new GzStats();
		saved.addGiven("Nick");
		t.loadWeekly(saved, utc("2026-09-27T00:00:00Z"));

		assertFalse(t.rollWeek(utc("2026-09-30T12:00:00Z")));
		assertEquals(1, t.getWeekly().totalGiven());

		assertTrue(t.rollWeek(utc("2026-10-05T12:00:00Z")));
		assertEquals(0, t.getWeekly().totalGiven());
	}

	@Test
	public void resetAllTimeClearsWeekly()
	{
		GzTracker t = new GzTracker(ZoneOffset.UTC);
		t.onClanChat("Nick", "gz", utc("2026-09-29T12:00:00Z"), SETTINGS);
		t.resetAllTime();
		assertEquals(0, t.getWeekly().totalGiven());
	}

	// counts every gz (no one-per-broadcast rule) so only the hourly cap limits them
	private static final GzTracker.Settings UNLIMITED = new GzTracker.Settings(90_000L, false, 40);

	@Test
	public void hourlyCapStopsCountingAt100()
	{
		GzTracker t = new GzTracker(ZoneOffset.UTC);
		long start = utc("2026-09-29T12:00:00Z");
		for (int i = 0; i < GzTracker.MAX_GZ_PER_HOUR; i++)
		{
			assertTrue(t.onClanChat("Spammer", "gz", start + i * 1_000L, UNLIMITED));
		}
		assertFalse(t.onClanChat("Spammer", "gz", start + 200_000L, UNLIMITED));
		assertEquals(100, (int) t.getAllTime().getGiven().get("Spammer"));
		assertEquals(100, (int) t.getWeekly().getGiven().get("Spammer"));
		assertEquals(100, (int) t.getSession().getGiven().get("Spammer"));
	}

	@Test
	public void hourlyCapIsRollingNotPerClockHour()
	{
		GzTracker t = new GzTracker(ZoneOffset.UTC);
		long start = utc("2026-09-29T12:59:00Z");
		for (int i = 0; i < GzTracker.MAX_GZ_PER_HOUR; i++)
		{
			t.onClanChat("Spammer", "gz", start + i * 100L, UNLIMITED);
		}
		// a new clock hour doesn't reset it...
		assertFalse(t.onClanChat("Spammer", "gz", utc("2026-09-29T13:00:30Z"), UNLIMITED));
		// ...but an hour after the first counted gz, one slot frees up
		assertTrue(t.onClanChat("Spammer", "gz", start + GzTracker.HOUR_MILLIS, UNLIMITED));
		assertFalse(t.onClanChat("Spammer", "gz", start + GzTracker.HOUR_MILLIS + 50L, UNLIMITED));
		assertEquals(101, (int) t.getAllTime().getGiven().get("Spammer"));
	}

	@Test
	public void hourlyCapSurvivesARestart()
	{
		GzTracker before = new GzTracker(ZoneOffset.UTC);
		long start = utc("2026-09-29T12:00:00Z");
		for (int i = 0; i < GzTracker.MAX_GZ_PER_HOUR; i++)
		{
			before.onClanChat("Spammer", "gz", start + i * 1_000L, UNLIMITED);
		}
		long restart = start + 10 * 60_000L;

		GzTracker after = new GzTracker(ZoneOffset.UTC);
		after.loadRecentGz(before.getRecentGz(restart), restart);
		assertFalse(after.onClanChat("Spammer", "gz", restart, UNLIMITED));
		// the saved times still expire an hour after they were counted
		assertTrue(after.onClanChat("Spammer", "gz", start + GzTracker.HOUR_MILLIS, UNLIMITED));
	}

	@Test
	public void savedCapTimesOlderThanAnHourAreDropped()
	{
		GzTracker t = new GzTracker(ZoneOffset.UTC);
		long start = utc("2026-09-29T12:00:00Z");
		t.onClanChat("Bob", "gz", start, UNLIMITED);
		assertTrue(t.getRecentGz(start + GzTracker.HOUR_MILLIS).isEmpty());
		assertEquals(1, t.getRecentGz(start + 1_000L).get("bob").size());
	}

	@Test
	public void hourlyCapIsPerPlayerAndIgnoresNameCase()
	{
		GzTracker t = new GzTracker(ZoneOffset.UTC);
		long start = utc("2026-09-29T12:00:00Z");
		for (int i = 0; i < GzTracker.MAX_GZ_PER_HOUR; i++)
		{
			t.onClanChat(i % 2 == 0 ? "Spammer" : "spammer", "gz", start + i, UNLIMITED);
		}
		assertFalse(t.onClanChat("SPAMMER", "gz", start + 500L, UNLIMITED));
		// someone else is unaffected
		assertTrue(t.onClanChat("Honest", "gz", start + 500L, UNLIMITED));
	}

	@Test
	public void cappedGzIsNotCreditedToTheBroadcast()
	{
		GzTracker t = new GzTracker(ZoneOffset.UTC);
		long start = utc("2026-09-29T12:00:00Z");
		for (int i = 0; i < GzTracker.MAX_GZ_PER_HOUR; i++)
		{
			t.onClanChat("Spammer", "gz", start + i, UNLIMITED);
		}
		t.onBroadcast("Zezima", "Zezima has reached 99 Slayer.", start + 1_000L);
		assertFalse(t.onClanChat("Spammer", "gz", start + 2_000L, UNLIMITED));
		assertNull(t.getAllTime().getReceived().get("Zezima"));
	}
}
