package com.corclan.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import com.google.gson.Gson;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

/** The JSON must match what the CoR clan server accepts and sends (see cor-clan-api README, "API for the plugins"). */
public class SyncModelsTest
{
	private static final String REPORTER = "{\"accountHash\":\"-1234567890123\",\"rsn\":\"Lavasockz\",\"clan\":\"C o R\"}";

	private final Gson gson = new Gson();
	private final SyncModels.Reporter reporter = new SyncModels.Reporter("-1234567890123", "Lavasockz", "C o R");

	@Test
	public void reportSerializesToTheServerShape()
	{
		SyncModels.ReportPayload payload = new SyncModels.ReportPayload(reporter, 1790000000000L, Arrays.asList(
			SyncModels.Event.broadcast("Lavasockz has received a drop: Twisted bow", 1789999970000L),
			SyncModels.Event.gz("gz", 1789999975000L)));

		assertEquals(
			"{\"reporter\":" + REPORTER + ","
				+ "\"sentAt\":1790000000000,"
				+ "\"events\":["
				+ "{\"type\":\"broadcast\",\"text\":\"Lavasockz has received a drop: Twisted bow\",\"at\":1789999970000},"
				+ "{\"type\":\"gz\",\"text\":\"gz\",\"at\":1789999975000}]}",
			gson.toJson(payload));
	}

	@Test
	public void eventsNeverNameAPlayer()
	{
		// the server takes the player from the reporter; nothing about anyone else can be sent
		String json = gson.toJson(SyncModels.Event.gz("gz", 1L)) + gson.toJson(SyncModels.Event.broadcast("x", 1L));
		assertEquals(-1, json.indexOf("giver"));
		assertEquals(-1, json.indexOf("subject"));
	}

	@Test
	public void profileSerializesToTheServerShape()
	{
		assertEquals(
			"{\"reporter\":" + REPORTER + ",\"rank\":126,\"glows\":[\"gold_outline\",\"gem_ruby\"]}",
			gson.toJson(new SyncModels.ProfilePayload(reporter, 126, Arrays.asList("gold_outline", "gem_ruby"))));
		// an empty list clears the picks on the server, so it must be sent as one
		assertEquals(
			"{\"reporter\":" + REPORTER + ",\"rank\":-1,\"glows\":[]}",
			gson.toJson(new SyncModels.ProfilePayload(reporter, -1, Collections.emptyList())));
	}

	@Test
	public void ranksSerializeToTheServerShape()
	{
		assertEquals(
			"{\"reporter\":" + REPORTER + ",\"ranks\":[{\"rank\":126,\"title\":\"Owner\"},{\"rank\":100,\"title\":\"Administrator\"}]}",
			gson.toJson(new SyncModels.RanksPayload(reporter, Arrays.asList(
				new SyncModels.RankTitle(126, "Owner"), new SyncModels.RankTitle(100, "Administrator")))));
	}

	@Test
	public void reportersAreEqualByAccountNameAndClan()
	{
		assertEquals(reporter, new SyncModels.Reporter("-1234567890123", "Lavasockz", "C o R"));
		assertEquals(reporter.hashCode(), new SyncModels.Reporter("-1234567890123", "Lavasockz", "C o R").hashCode());
		assertNotEquals(reporter, new SyncModels.Reporter("-1234567890123", "Alt", "C o R"));
		assertNotEquals(reporter, new SyncModels.Reporter("99", "Lavasockz", "C o R"));
	}

	@Test
	public void readsTheClanResponse()
	{
		SyncModels.ClanResponse clan = gson.fromJson(
			"{\"weekStart\":\"2026-10-04T00:00:00.000Z\",\"generatedAt\":\"2026-10-07T18:20:00.000Z\","
				+ "\"players\":[{\"rsn\":\"Lavasockz\",\"rank\":126,\"given\":41,\"received\":12,\"weeklyGiven\":6,"
				+ "\"weeklyReceived\":2,\"icons\":[\"founder\"],\"title\":\"Developer\",\"glows\":[\"gold_outline\"]},"
				+ "{\"rsn\":\"Bob\",\"rank\":null,\"given\":3,\"received\":0,\"weeklyGiven\":0,\"weeklyReceived\":0,"
				+ "\"icons\":[],\"title\":null,\"glows\":[]}],"
				+ "\"weeks\":[{\"weekStart\":\"2026-09-27T00:00:00.000Z\",\"top\":[{\"rsn\":\"Lavasockz\",\"count\":31},"
				+ "{\"rsn\":\"Bob\",\"count\":20}]}]}",
			SyncModels.ClanResponse.class);

		assertEquals("2026-10-04T00:00:00.000Z", clan.getWeekStart());
		SyncModels.ClanPlayer lava = clan.getPlayers().get(0);
		assertEquals("Lavasockz", lava.getRsn());
		assertEquals(Integer.valueOf(126), lava.getRank());
		assertEquals(41, lava.getGiven());
		assertEquals(12, lava.getReceived());
		assertEquals(6, lava.getWeeklyGiven());
		assertEquals(Collections.singletonList("founder"), lava.getIcons());
		assertEquals("Developer", lava.getTitle());
		assertEquals(Collections.singletonList("gold_outline"), lava.getGlows());
		assertNull(clan.getPlayers().get(1).getRank());
		assertNull(clan.getPlayers().get(1).getTitle());

		SyncModels.Week week = clan.getWeeks().get(0);
		assertEquals("2026-09-27T00:00:00.000Z", week.getWeekStart());
		assertEquals("Bob", week.getTop().get(1).getRsn());
		assertEquals(20, week.getTop().get(1).getCount());
	}

	@Test
	public void readsTheIconsResponseAndErrors()
	{
		SyncModels.IconsResponse icons = gson.fromJson(
			"{\"icons\":[{\"key\":\"crown\",\"hash\":\"9f2c0a7d3b1e4c55\",\"png\":\"AAAA\"}],\"rankIcons\":[{\"rank\":126,\"icon\":\"crown\"}]}",
			SyncModels.IconsResponse.class);
		assertEquals("crown", icons.getIcons().get(0).getKey());
		assertEquals("9f2c0a7d3b1e4c55", icons.getIcons().get(0).getHash());
		assertEquals("AAAA", icons.getIcons().get(0).getPng());
		assertEquals(126, icons.getRankIcons().get(0).getRank());
		assertEquals("crown", icons.getRankIcons().get(0).getIcon());

		assertEquals("name_taken", gson.fromJson("{\"error\":\"name_taken\"}", SyncModels.ErrorBody.class).getError());
	}

	@Test
	public void missingListsReadAsEmpty()
	{
		assertEquals(0, gson.fromJson("{}", SyncModels.ClanResponse.class).getPlayers().size());
		assertEquals(0, gson.fromJson("{}", SyncModels.ClanResponse.class).getWeeks().size());
		assertEquals(0, gson.fromJson("{}", SyncModels.IconsResponse.class).getIcons().size());
		assertEquals(0, gson.fromJson("{}", SyncModels.IconsResponse.class).getRankIcons().size());
		assertEquals(0, gson.fromJson("{\"rsn\":\"A\"}", SyncModels.ClanPlayer.class).getIcons().size());
		assertNull(gson.fromJson("{\"rsn\":\"A\"}", SyncModels.ClanPlayer.class).getGlows());
		assertEquals(0, gson.fromJson("{}", SyncModels.Week.class).getTop().size());
	}
}
