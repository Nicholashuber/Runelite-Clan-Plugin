package com.corclan.gz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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
}
