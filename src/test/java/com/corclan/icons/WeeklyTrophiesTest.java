package com.corclan.icons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.corclan.gz.GzStats;
import java.util.Map;
import org.junit.Test;

public class WeeklyTrophiesTest
{
	private static GzStats given(Object... nameCount)
	{
		GzStats stats = new GzStats();
		for (int i = 0; i < nameCount.length; i += 2)
		{
			stats.getGiven().put((String) nameCount[i], (Integer) nameCount[i + 1]);
		}
		return stats;
	}

	@Test
	public void topThreeGetGoldSilverBronze()
	{
		Map<String, String> t = WeeklyTrophies.of(given("Zezima", 5, "Iron Nick", 9, "Bob", 2, "Carl", 1));
		assertEquals(3, t.size());
		assertEquals("week_1", t.get("iron nick"));
		assertEquals("week_2", t.get("zezima"));
		assertEquals("week_3", t.get("bob"));
	}

	@Test
	public void tiesGoByNameLikeThePodium()
	{
		Map<String, String> t = WeeklyTrophies.of(given("Bob", 3, "Alice", 3));
		assertEquals("week_1", t.get("alice"));
		assertEquals("week_2", t.get("bob"));
	}

	@Test
	public void quietWeekHasNoTrophies()
	{
		assertTrue(WeeklyTrophies.of(new GzStats()).isEmpty());
	}
}
