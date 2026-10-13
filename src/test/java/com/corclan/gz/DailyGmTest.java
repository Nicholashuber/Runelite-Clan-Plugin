package com.corclan.gz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.time.Instant;
import org.junit.Test;

public class DailyGmTest
{
	@Test
	public void generousAboutHowPeopleSayIt()
	{
		for (String t : new String[]{"gm", "GM", "gm!", "gmgm", "gmmm", "gm all", "Gm fam <3", "good morning", "Good Morning!!"})
		{
			assertTrue(t, DailyGm.isGm(t));
		}
	}

	@Test
	public void notFooledByWordsThatContainGm()
	{
		StringBuilder longLine = new StringBuilder();
		for (int i = 0; i < 100; i++)
		{
			longLine.append('x');
		}
		for (String t : new String[]{"", "gz", "dogma", "gmail", "egm", "good night", longLine + " gm"})
		{
			assertFalse(t, DailyGm.isGm(t));
		}
		assertFalse(DailyGm.isGm(null));
	}

	@Test
	public void theDayTurnsAtMidnightUtc()
	{
		long beforeMidnight = Instant.parse("2026-10-12T23:59:59Z").toEpochMilli();
		long afterMidnight = Instant.parse("2026-10-13T00:00:00Z").toEpochMilli();
		assertEquals(DailyGm.utcDay(beforeMidnight) + 1, DailyGm.utcDay(afterMidnight));
	}
}
