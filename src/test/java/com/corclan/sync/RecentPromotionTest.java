package com.corclan.sync;

import com.google.gson.Gson;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class RecentPromotionTest
{
	private static List<RecentPromotion> parse(String json)
	{
		return ClanSnapshot.of(new Gson().fromJson(json, SyncModels.ClanResponse.class)).getRecentPromotions();
	}

	@Test
	public void readsTheFeedNewestFirstAndCleansIt()
	{
		List<RecentPromotion> list = parse("{\"players\":[],\"weeks\":[],\"promotions\":["
			+ "{\"rsn\":\"<col=ff0000>Bob</col>\",\"from\":\"Opal\",\"to\":\"Sapphire\",\"at\":\"2026-10-12T20:00:00.000Z\"},"
			+ "{\"rsn\":\"Alice\",\"from\":\"Sapphire\",\"to\":\"Emerald\",\"at\":\"bad\"},"
			+ "{\"rsn\":\"\",\"from\":\"Opal\",\"to\":\"Sapphire\"},"
			+ "{\"rsn\":\"C\",\"from\":\"Opal\",\"to\":\"Sapphire\"},"
			+ "{\"rsn\":\"D\",\"from\":\"Opal\",\"to\":\"Sapphire\"}]}");
		assertEquals(RecentPromotion.MAX, list.size());
		assertEquals("Bob", list.get(0).name);
		assertEquals("Sapphire", list.get(0).to);
		assertEquals(1791835200000L, list.get(0).at);
		assertEquals(0L, list.get(1).at);
		assertEquals("C", list.get(2).name);
	}

	@Test
	public void olderServersSendNone()
	{
		assertTrue(parse("{\"players\":[],\"weeks\":[]}").isEmpty());
	}
}
