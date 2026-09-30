package com.corclan.party;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.corclan.gz.GzStats;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import org.junit.Test;

public class PartyGzBookTest
{
	private static final long WEEK = 1_790_000_000_000L;
	private static final long HOUR = 3_600_000L;
	private static final Predicate<String> ANYONE = name -> true;

	private static Map<String, Integer> counts(Object... nameCount)
	{
		Map<String, Integer> out = new HashMap<>();
		for (int i = 0; i < nameCount.length; i += 2)
		{
			out.put((String) nameCount[i], (Integer) nameCount[i + 1]);
		}
		return out;
	}

	private static CorGzCounts msg(long weekStart, Map<String, Integer> weekly, Map<String, Integer> given)
	{
		return new CorGzCounts(weekStart, weekly, given, new HashMap<>());
	}

	@Test
	public void highestCountWinsInsteadOfAdding()
	{
		PartyGzBook book = new PartyGzBook();
		book.rollWeek(WEEK);
		assertTrue(book.merge(msg(WEEK, counts("Bob", 5), counts("Bob", 50)), WEEK + 10 * HOUR, ANYONE));
		assertFalse(book.merge(msg(WEEK, counts("Bob", 3), counts("Bob", 40)), WEEK + 10 * HOUR, ANYONE));

		assertEquals(Integer.valueOf(5), book.weekly(counts("Bob", 4)).get("Bob"));
		assertEquals(Integer.valueOf(9), book.weekly(counts("Bob", 9)).get("Bob"));
		assertEquals(Integer.valueOf(50), book.given(Collections.emptyMap()).get("Bob"));
	}

	@Test
	public void weeklyCountsFromAnotherWeekAreIgnored()
	{
		PartyGzBook book = new PartyGzBook();
		book.rollWeek(WEEK);
		book.merge(msg(WEEK - 7 * 24 * HOUR, counts("Bob", 5), counts("Bob", 5)), WEEK + HOUR, ANYONE);
		assertNull(book.weekly(Collections.emptyMap()).get("Bob"));
		assertEquals(Integer.valueOf(5), book.given(Collections.emptyMap()).get("Bob"));
	}

	@Test
	public void newWeekClearsOnlyWeekly()
	{
		PartyGzBook book = new PartyGzBook();
		book.rollWeek(WEEK);
		book.merge(msg(WEEK, counts("Bob", 5), counts("Bob", 5)), WEEK + HOUR, ANYONE);
		book.rollWeek(WEEK + 7 * 24 * HOUR);
		assertTrue(book.weekly(Collections.emptyMap()).isEmpty());
		assertEquals(Integer.valueOf(5), book.given(Collections.emptyMap()).get("Bob"));
	}

	@Test
	public void outsidersAndImpossibleWeeklyCountsAreDropped()
	{
		PartyGzBook book = new PartyGzBook();
		book.rollWeek(WEEK);
		// 30 minutes into the week the hourly cap allows at most 100
		book.merge(msg(WEEK, counts("Bob", 999_999, "Stranger", 7), counts("Stranger", 7)), WEEK + HOUR / 2, "Bob"::equals);
		assertEquals(Integer.valueOf(100), book.weekly(Collections.emptyMap()).get("Bob"));
		assertNull(book.weekly(Collections.emptyMap()).get("Stranger"));
		assertNull(book.given(Collections.emptyMap()).get("Stranger"));
	}

	@Test
	public void messageCarriesTheMergedView()
	{
		PartyGzBook book = new PartyGzBook();
		book.rollWeek(WEEK);
		book.merge(msg(WEEK, counts("Bob", 5), counts("Bob", 50)), WEEK + HOUR, ANYONE);
		GzStats weekly = new GzStats();
		weekly.getGiven().put("Alice", 2);
		GzStats allTime = new GzStats();
		allTime.getGiven().put("Alice", 20);

		CorGzCounts out = book.message(weekly, allTime);
		assertEquals(WEEK, out.getWeekStart());
		assertEquals(counts("Bob", 5, "Alice", 2), out.getWeekly());
		assertEquals(counts("Bob", 50, "Alice", 20), out.getGiven());
	}
}
