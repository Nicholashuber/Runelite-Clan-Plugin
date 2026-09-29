package com.corclan.sync;

import static org.junit.Assert.assertEquals;
import com.google.gson.Gson;
import java.util.Arrays;
import org.junit.Test;

/** The JSON must match what the CoR clan server accepts (see cor-clan-api README). */
public class SyncModelsTest
{
	private final Gson gson = new Gson();

	@Test
	public void reportSerializesToTheServerShape()
	{
		SyncModels.ReportPayload payload = new SyncModels.ReportPayload(
			new SyncModels.Reporter("-123456789", "Lavasockz", "C o R"),
			1790000000000L,
			Arrays.asList(
				SyncModels.Event.broadcast("Zezima", "Zezima has received a drop: Twisted bow", 1789999970000L),
				SyncModels.Event.gz("Lavasockz", "gz", 1789999975000L)));

		assertEquals(
			"{\"reporter\":{\"accountHash\":\"-123456789\",\"rsn\":\"Lavasockz\",\"clan\":\"C o R\"},"
				+ "\"sentAt\":1790000000000,"
				+ "\"events\":["
				+ "{\"type\":\"broadcast\",\"subject\":\"Zezima\",\"text\":\"Zezima has received a drop: Twisted bow\",\"at\":1789999970000},"
				+ "{\"type\":\"gz\",\"giver\":\"Lavasockz\",\"text\":\"gz\",\"at\":1789999975000}]}",
			gson.toJson(payload));
	}

	@Test
	public void readsLeaderboardAndCosmeticsResponses()
	{
		SyncModels.Leaderboard lb = gson.fromJson(
			"{\"givers\":[{\"rsn\":\"Bob\",\"count\":3}],\"receivers\":[],\"generatedAt\":\"2026-09-29T20:44:10.773Z\"}",
			SyncModels.Leaderboard.class);
		assertEquals("Bob", lb.getGivers().get(0).getRsn());
		assertEquals(3, lb.getGivers().get(0).getCount());
		assertEquals(0, lb.getReceivers().size());

		SyncModels.CosmeticsResponse c = gson.fromJson(
			"{\"players\":[{\"rsn\":\"Lavasockz\",\"icons\":[\"founder\",\"dev\"],\"title\":\"Developer\"}]}",
			SyncModels.CosmeticsResponse.class);
		assertEquals(Arrays.asList("founder", "dev"), c.getPlayers().get(0).getIcons());
		assertEquals("Developer", c.getPlayers().get(0).getTitle());
	}

	@Test
	public void missingListsReadAsEmpty()
	{
		assertEquals(0, gson.fromJson("{}", SyncModels.Leaderboard.class).getGivers().size());
		assertEquals(0, gson.fromJson("{}", SyncModels.CosmeticsResponse.class).getPlayers().size());
		assertEquals(0, gson.fromJson("{\"rsn\":\"A\"}", SyncModels.Cosmetic.class).getIcons().size());
	}
}
