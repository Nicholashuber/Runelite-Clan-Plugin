package com.corclan.party;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.corclan.gz.GzStats;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class PartyGzBookTest
{
	private static final long WEEK = 1_790_000_000_000L;
	private static final long HOUR = 3_600_000L;

	private static Map<String, Integer> counts(Object... nameCount)
	{
		Map<String, Integer> out = new HashMap<>();
		for (int i = 0; i < nameCount.length; i += 2)
		{
			out.put((String) nameCount[i], (Integer) nameCount[i + 1]);
		}
		return out;
	}

	@Test
	public void weOnlyEverSendOurOwnNumbers()
	{
		PartyGzBook book = new PartyGzBook();
		book.rollWeek(WEEK);
		GzStats weekly = new GzStats();
		weekly.getGiven().putAll(counts("Me", 4, "Someone else", 99));
		GzStats allTime = new GzStats();
		allTime.getGiven().putAll(counts("Me", 40, "Someone else", 999));
		allTime.getReceived().putAll(counts("Me", 7, "Someone else", 888));

		CorGzCounts out = book.message("Me", weekly, allTime);
		assertEquals(WEEK, out.getWeekStart());
		assertEquals(40, out.getGiven());
		assertEquals(7, out.getReceived());
		assertEquals(4, out.getWeekly());
	}

	@Test
	public void membersRaiseTheirOwnCountsOnly()
	{
		PartyGzBook book = new PartyGzBook();
		book.rollWeek(WEEK);
		assertTrue(book.merge("Bob", new CorGzCounts(WEEK, 50, 9, 5), WEEK + 10 * HOUR));
		assertFalse(book.merge("Bob", new CorGzCounts(WEEK, 40, 9, 3), WEEK + 10 * HOUR));

		// our own counts for everyone stay; Bob's report raises only Bob
		Map<String, Integer> given = book.given(counts("Bob", 30, "Alice", 12));
		assertEquals(Integer.valueOf(50), given.get("Bob"));
		assertEquals(Integer.valueOf(12), given.get("Alice"));
		assertEquals(Integer.valueOf(9), book.received(Collections.emptyMap()).get("Bob"));
		assertEquals(Integer.valueOf(8), book.weekly(counts("Bob", 8)).get("Bob"));
	}

	@Test
	public void weeklyFromAnotherWeekDoesNotCount()
	{
		PartyGzBook book = new PartyGzBook();
		book.rollWeek(WEEK);
		book.merge("Bob", new CorGzCounts(WEEK, 5, 0, 5), WEEK + HOUR);
		book.rollWeek(WEEK + 7 * 24 * HOUR);
		assertNull(book.weekly(Collections.emptyMap()).get("Bob"));
		assertEquals(Integer.valueOf(5), book.given(Collections.emptyMap()).get("Bob"));
	}

	@Test
	public void impossibleWeeklyCountsAreCapped()
	{
		PartyGzBook book = new PartyGzBook();
		book.rollWeek(WEEK);
		// 30 minutes into the week the hourly cap allows at most 100
		book.merge("Bob", new CorGzCounts(WEEK, 999_999, 0, 999_999), WEEK + HOUR / 2);
		assertEquals(Integer.valueOf(100), book.weekly(Collections.emptyMap()).get("Bob"));
	}

	@Test
	public void missingNameIsIgnored()
	{
		PartyGzBook book = new PartyGzBook();
		assertFalse(book.merge(null, new CorGzCounts(WEEK, 5, 5, 5), WEEK));
		assertFalse(book.merge("", new CorGzCounts(WEEK, 5, 5, 5), WEEK));
		assertTrue(book.isEmpty());
	}
}
