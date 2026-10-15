package com.corclan.clan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.time.LocalDate;
import org.junit.Test;

public class InfernoReminderTest
{
	private static final LocalDate MON = LocalDate.of(2026, 10, 12);

	@Test
	public void onlyJoeBoobIsRemindedAndOnlyRayWatches()
	{
		assertTrue(InfernoReminder.isTarget("JoeBoob"));
		assertFalse(InfernoReminder.isTarget("Lavasockz"));
		assertTrue(InfernoReminder.isWatcher("UimFartQueef"));
		assertFalse(InfernoReminder.isWatcher("JoeBoob"));
	}

	@Test
	public void countsOncePerDay()
	{
		assertEquals(1, InfernoReminder.countAfter(null, MON));
		String stored = InfernoReminder.store(MON, 1);
		// logging in again the same day keeps the number
		assertEquals(1, InfernoReminder.countAfter(stored, MON));
		assertEquals(2, InfernoReminder.countAfter(stored, MON.plusDays(1)));
		// days he doesn't log in don't count
		assertEquals(2, InfernoReminder.countAfter(stored, MON.plusDays(9)));
	}

	@Test
	public void aBrokenStoredValueStartsAgain()
	{
		assertEquals(1, InfernoReminder.countAfter("garbage", MON));
		assertEquals(1, InfernoReminder.countAfter("2026-10-12|x", MON));
		assertEquals(1, InfernoReminder.countAfter("2026-10-12|0", MON));
	}

	@Test
	public void linesCarryTheDay()
	{
		assertTrue(InfernoReminder.targetLine(null, 7).contains("Daily reminder #7: go and get your Infernal cape."));
		assertTrue(InfernoReminder.watcherLine("<img=3>", "JoeBoob", 7).startsWith("<img=3> "));
		assertTrue(InfernoReminder.watcherLine(null, "JoeBoob", 7).contains("(day 7)"));
	}
}
