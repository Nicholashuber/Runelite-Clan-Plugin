package com.corclan.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.corclan.gz.GzStats;
import com.corclan.gz.WeekResult;
import com.google.gson.Gson;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class GzViewTest
{
	private static final long WEEK = Instant.parse("2026-10-04T00:00:00Z").toEpochMilli();
	private static final long NOW = WEEK + 3_600_000L;

	private final Gson gson = new Gson();
	// this client counted 2 from Bob (1 this week) and a gz for Lavasockz
	private final GzStats localAllTime = gson.fromJson("{\"given\":{\"Bob\":2},\"received\":{\"Lavasockz\":1}}", GzStats.class);
	private final GzStats localWeekly = gson.fromJson("{\"given\":{\"Bob\":1}}", GzStats.class);
	private final List<WeekResult> localWeeks = Collections.singletonList(new WeekResult(WEEK - 7 * 86_400_000L, Collections.singletonList("Bob")));

	private final ClanSnapshot server = ClanSnapshot.of(gson.fromJson(
		"{\"weekStart\":\"2026-10-04T00:00:00.000Z\",\"players\":["
			+ "{\"rsn\":\"Lavasockz\",\"given\":41,\"received\":12,\"weeklyGiven\":6},"
			+ "{\"rsn\":\"Bob\",\"given\":30,\"received\":4,\"weeklyGiven\":9}],"
			+ "\"weeks\":[{\"weekStart\":\"2026-09-27T00:00:00.000Z\",\"top\":[{\"rsn\":\"Lavasockz\",\"count\":31}]}]}",
		SyncModels.ClanResponse.class));

	private GzView choose(ClanSnapshot snapshot, long loadedAt)
	{
		return GzView.choose(snapshot, loadedAt, NOW, localAllTime, localWeekly, localWeeks, WEEK);
	}

	@Test
	public void localCountsWhenSyncIsOffOrNothingLoaded()
	{
		GzView view = choose(null, 0L);
		assertFalse(view.clanWide);
		assertEquals(localAllTime.getGiven(), view.given);
		assertEquals(localAllTime.getReceived(), view.received);
		assertEquals(localWeekly.getGiven(), view.weekly);
		assertEquals("Bob", view.weekResults.get(0).getWinners().get(0));
	}

	@Test
	public void serverCountsOnceLoaded()
	{
		GzView view = choose(server, NOW - 120_000L);
		assertTrue(view.clanWide);
		assertEquals(Integer.valueOf(41), view.given.get("Lavasockz"));
		assertEquals(Integer.valueOf(30), view.given.get("Bob"));
		assertEquals(Integer.valueOf(12), view.received.get("Lavasockz"));
		assertEquals(Integer.valueOf(9), view.weekly.get("Bob"));
		assertEquals("Lavasockz", view.weekResults.get(0).getWinners().get(0));
		// nothing of this client's own counts is mixed in
		assertEquals(2, view.given.size());
		assertEquals(1, view.weekResults.size());
	}

	@Test
	public void aStaleServerAnswerHandsBackToLocalCounts()
	{
		assertTrue(choose(server, NOW - GzView.STALE_MILLIS + 1).clanWide);
		GzView stale = choose(server, NOW - GzView.STALE_MILLIS);
		assertFalse(stale.clanWide);
		assertEquals(Integer.valueOf(2), stale.given.get("Bob"));
	}

	@Test
	public void lookupsIgnoreCaseAndSpacing()
	{
		GzView view = choose(server, NOW);
		assertEquals(41, view.givenOf("lavasockz"));
		assertEquals(41, view.givenOf("<img=3>Lavasockz"));
		assertEquals(4, view.receivedOf("BOB"));
		assertEquals(0, view.givenOf("Nobody"));
		assertEquals(0, view.givenOf(null));
		assertEquals(2, choose(null, 0L).givenOf("bob"));
	}

	@Test
	public void localCountingIsUntouchedByTheView()
	{
		choose(server, NOW);
		assertEquals(Integer.valueOf(2), localAllTime.getGiven().get("Bob"));
		assertEquals(1, localAllTime.getGiven().size());
		assertEquals(Integer.valueOf(1), localWeekly.getGiven().get("Bob"));
	}
}
