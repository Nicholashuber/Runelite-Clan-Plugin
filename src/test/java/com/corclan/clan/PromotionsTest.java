package com.corclan.clan;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PromotionsTest
{
	private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);

	private static Promotions.Member member(String name, String title, int daysAgo)
	{
		return new Promotions.Member(name, title, TODAY.minusDays(daysAgo), false);
	}

	@Test
	public void firstStepNeedsAWeekAndAHundredGz()
	{
		assertTrue(Promotions.check(Collections.singletonList(member("Bob", "Opal", 400)), TODAY,
			Collections.singletonMap("bob", 99)).due.isEmpty());
		assertTrue(Promotions.check(Collections.singletonList(member("Bob", "Opal", 6)), TODAY,
			Collections.singletonMap("bob", 200)).due.isEmpty());
		Promotions.Result result = Promotions.check(Collections.singletonList(member("Bob", "Opal", 7)), TODAY,
			Collections.singletonMap("bob", 100));
		assertEquals(1, result.due.size());
		assertEquals("Opal", result.due.get(0).from);
		assertEquals("Sapphire", result.due.get(0).to);
	}

	@Test
	public void gzShortHoldsThePromotionBack()
	{
		Promotions.Result result = Promotions.check(Collections.singletonList(member("Bob", "Sapphire", 100)), TODAY,
			Collections.singletonMap("bob", 249));
		assertTrue(result.due.isEmpty());
		assertEquals(1, result.soon.size());
		assertEquals("1 gz", result.soon.get(0).still());
	}

	@Test
	public void uncountedGzAreNeverDue()
	{
		Promotions.Result result = Promotions.check(Arrays.asList(member("No Plugin", "Opal", 400),
			member("Old Sapphire", "Sapphire", 400)), TODAY, Collections.emptyMap());
		assertTrue(result.due.isEmpty());
		assertTrue(result.soon.isEmpty());
	}

	@Test
	public void countedGzCanSkipSeveralGems()
	{
		Promotions.Result result = Promotions.check(Collections.singletonList(member("Giver", "Opal", 60)), TODAY,
			Collections.singletonMap("giver", 1500));
		assertEquals("Diamond", result.due.get(0).to);
	}

	@Test
	public void comingUpWithinAWeek()
	{
		Map<String, Integer> gz = new HashMap<>();
		gz.put("soon", 100);
		gz.put("later", 85);
		Promotions.Result result = Promotions.check(Arrays.asList(member("Soon", "Opal", 4), member("Later", "Opal", 0)),
			TODAY, gz);
		assertTrue(result.due.isEmpty());
		assertEquals(2, result.soon.size());
		assertEquals("Soon", result.soon.get(0).name);
		assertEquals("3 days", result.soon.get(0).still());
		assertEquals("7 days, 15 gz", result.soon.get(1).still());
	}

	@Test
	public void staffZenyteUnknownJoinDateAndOtherRanksAreSkipped()
	{
		Promotions.Result result = Promotions.check(Arrays.asList(member("Ray", "Owner", 900), member("Top", "Zenyte", 900),
			new Promotions.Member("Unknown", "Opal", null, false), member("Guest", "Recruit", 900)), TODAY, Collections.emptyMap());
		assertTrue(result.due.isEmpty());
		assertTrue(result.soon.isEmpty());
	}

	@Test
	public void gzAreLookedUpByNameKeyAndDueIsSortedHighestFirst()
	{
		Map<String, Integer> gz = new HashMap<>();
		gz.put("big giver", 50000);
		gz.put("small", 100);
		Promotions.Result result = Promotions.check(Arrays.asList(member("Small", "Opal", 400), member("Big Giver", "Opal", 400)),
			TODAY, gz);
		assertEquals("Big Giver", result.due.get(0).name);
		assertEquals("Zenyte", result.due.get(0).to);
		assertEquals("Sapphire", result.due.get(1).to);
	}

	@Test
	public void ladderMatchesTheGemLeague()
	{
		for (Promotions.Step step : Promotions.LADDER)
		{
			assertTrue(OrgChart.GEM_LEAGUE.titles.contains(step.from));
			assertTrue(OrgChart.GEM_LEAGUE.titles.contains(step.to));
		}
	}
}
