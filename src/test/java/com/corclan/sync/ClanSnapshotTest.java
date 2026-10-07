package com.corclan.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.corclan.glow.DevGlow;
import com.corclan.glow.GemGlow;
import com.corclan.glow.GlowEffect;
import com.corclan.gz.GzTracker;
import com.corclan.gz.Streaks;
import com.corclan.gz.WeekResult;
import com.google.gson.Gson;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import net.runelite.api.clan.ClanRank;
import org.junit.Test;

public class ClanSnapshotTest
{
	// Sundays, 00:00 UTC
	private static final long WEEK = Instant.parse("2026-10-04T00:00:00Z").toEpochMilli();
	private static final long NEXT_WEEK = Instant.parse("2026-10-11T00:00:00Z").toEpochMilli();

	private static ClanSnapshot snapshot(String players, String weeks)
	{
		return ClanSnapshot.of(new Gson().fromJson(
			"{\"weekStart\":\"2026-10-04T00:00:00.000Z\",\"players\":[" + players + "],\"weeks\":[" + weeks + "]}",
			SyncModels.ClanResponse.class));
	}

	private static String player(String rsn, String rank, int given, int received, int weeklyGiven, String glows)
	{
		return "{\"rsn\":\"" + rsn + "\",\"rank\":" + rank + ",\"given\":" + given + ",\"received\":" + received
			+ ",\"weeklyGiven\":" + weeklyGiven + (glows == null ? "" : ",\"glows\":" + glows) + "}";
	}

	private static String week(String start, String... top)
	{
		StringBuilder sb = new StringBuilder("{\"weekStart\":\"" + start + "\",\"top\":[");
		for (int i = 0; i < top.length; i += 2)
		{
			sb.append(i == 0 ? "" : ",").append("{\"rsn\":\"").append(top[i]).append("\",\"count\":").append(top[i + 1]).append("}");
		}
		return sb.append("]}").toString();
	}

	@Test
	public void countsAreKeptPerPlayerAndZerosLeftOut()
	{
		ClanSnapshot s = snapshot(
			player("Lavasockz", "126", 41, 12, 6, null) + "," + player("Bob", "null", 3, 0, 0, null)
				+ "," + player("", "5", 9, 9, 9, null) + ",null",
			"");
		Map<String, Integer> given = new HashMap<>();
		given.put("Lavasockz", 41);
		given.put("Bob", 3);
		assertEquals(given, s.getGiven());
		assertEquals(Collections.singletonMap("Lavasockz", 12), s.getReceived());
		assertEquals(Collections.singletonMap("Lavasockz", 6), s.weeklyGiven(WEEK));
		// nameless and null entries are skipped everywhere
		assertEquals(2, s.getPlayers().size());
	}

	@Test
	public void lastWeeksWeeklyCountsAreNotShownInANewWeek()
	{
		ClanSnapshot s = snapshot(player("Lavasockz", "126", 41, 12, 6, null), "");
		assertTrue(s.weeklyGiven(NEXT_WEEK).isEmpty());
		assertFalse(s.weeklyGiven(WEEK).isEmpty());

		// an unreadable week start is taken as the running week
		ClanSnapshot odd = ClanSnapshot.of(new Gson().fromJson(
			"{\"weekStart\":\"soon\",\"players\":[" + player("Bob", "1", 2, 0, 2, null) + "]}", SyncModels.ClanResponse.class));
		assertEquals(Collections.singletonMap("Bob", 2), odd.weeklyGiven(NEXT_WEEK));
	}

	@Test
	public void closedWeeksGiveTheirWinnersForStreaks()
	{
		ClanSnapshot s = snapshot("",
			week("2026-09-27T00:00:00.000Z", "Lavasockz", "31", "Bob", "20")
				+ "," + week("2026-09-20T00:00:00.000Z", "Lavasockz", "8", "Bob", "8", "Carl", "2")
				+ "," + week("not a date", "Bob", "5")
				+ "," + week("2026-09-13T00:00:00.000Z"));
		List<WeekResult> weeks = s.getWeekResults();
		assertEquals(2, weeks.size());
		assertEquals(Instant.parse("2026-09-27T00:00:00Z").toEpochMilli(), weeks.get(0).getWeekStart());
		assertEquals(Collections.singletonList("Lavasockz"), weeks.get(0).getWinners());
		// a tie at the top is a shared win, sorted by name
		assertEquals(Arrays.asList("Bob", "Lavasockz"), weeks.get(1).getWinners());

		Streaks.Streak current = Streaks.current(weeks, WEEK, GzTracker.WEEK_ZONE);
		assertEquals(new TreeSet<>(Collections.singletonList("Lavasockz")), current.players);
		assertEquals(2, current.weeks);
		assertEquals(2, Streaks.longest(weeks, GzTracker.WEEK_ZONE).weeks);
	}

	@Test
	public void glowPicksAreKeyedByPlayerName()
	{
		ClanSnapshot s = snapshot(
			player("Iron_Nick", "126", 1, 0, 0, "[\"gold_outline\",\"gem_ruby\"]")
				+ "," + player("Lavasockz", "100", 1, 0, 0, "[\"dev_zamorak_flames\"]"),
			"");
		Map<String, List<String>> glows = s.getGlows();
		// looked up the way players in the scene are: lower case, underscores as spaces
		assertEquals(Arrays.asList("gold_outline", "gem_ruby"), glows.get("iron nick"));
		assertEquals(Collections.singletonList("dev_zamorak_flames"), glows.get("lavasockz"));
		assertNull(glows.get("Iron_Nick"));

		// the viewer still decides by the wearer's clan rank as it sees it: the server's rank is never used
		assertEquals(EnumSet.of(GlowEffect.GOLD_OUTLINE), GlowEffect.active(ClanRank.OWNER, glows.get("iron nick")));
		assertTrue(GlowEffect.active(new ClanRank(50), glows.get("iron nick")).isEmpty());
		Map<Integer, String> titles = Collections.singletonMap(14, "Ruby");
		assertEquals(GemGlow.RUBY, GemGlow.shown(new ClanRank(14), titles, glows.get("iron nick")));
		assertNull(GemGlow.shown(new ClanRank(3), titles, glows.get("iron nick")));
		assertEquals(EnumSet.of(DevGlow.ZAMORAK_FLAMES), DevGlow.active(glows.get("lavasockz")));
	}

	@Test
	public void playersWhoNeverSharedPicksShowTheirDefaults()
	{
		ClanSnapshot s = snapshot(
			// no glows field, and an empty list from someone whose client never reported a profile (no rank)
			player("Old Client", "5", 1, 0, 0, null) + "," + player("Counts Only", "null", 1, 0, 0, "[]")
				// their client reported a rank and no picks: they switched everything off
				+ "," + player("Plain", "126", 1, 0, 0, "[]"),
			"");
		assertFalse(s.getGlows().containsKey("old client"));
		assertFalse(s.getGlows().containsKey("counts only"));
		assertEquals(Collections.emptyList(), s.getGlows().get("plain"));
		// null picks mean the rank's own effects, an empty list means none
		assertFalse(GlowEffect.active(ClanRank.OWNER, s.getGlows().get("old client")).isEmpty());
		assertTrue(GlowEffect.active(ClanRank.OWNER, s.getGlows().get("plain")).isEmpty());
	}

	@Test
	public void oversizedPickListsAreCut()
	{
		StringBuilder ids = new StringBuilder("[\"" + "x".repeat(ClanSnapshot.MAX_GLOW_ID_LENGTH + 1) + "\",null");
		for (int i = 0; i < ClanSnapshot.MAX_GLOWS + 10; i++)
		{
			ids.append(",\"id_").append(i).append("\"");
		}
		List<String> picks = snapshot(player("Bob", "1", 0, 0, 0, ids.append("]").toString()), "").getGlows().get("bob");
		assertEquals(ClanSnapshot.MAX_GLOWS, picks.size());
		assertEquals("id_0", picks.get(0));
	}

	@Test
	public void weekStartsSnapToSundayMidnightUtc()
	{
		assertEquals(WEEK, ClanSnapshot.parseWeekStart("2026-10-04T00:00:00.000Z"));
		assertEquals(WEEK, ClanSnapshot.parseWeekStart("2026-10-07T18:20:00Z"));
		assertEquals(0L, ClanSnapshot.parseWeekStart(null));
		assertEquals(0L, ClanSnapshot.parseWeekStart("yesterday"));
	}
}
