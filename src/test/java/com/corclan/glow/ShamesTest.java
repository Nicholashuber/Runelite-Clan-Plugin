package com.corclan.glow;

import com.corclan.clan.ClanLeaders;
import com.corclan.sync.SyncModels;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ShamesTest
{
	private static final long NOW = 1_790_000_000_000L;

	private static SyncModels.ShamesResponse response(String json)
	{
		return new Gson().fromJson(json, SyncModels.ShamesResponse.class);
	}

	@Test
	public void countsDownFromWhenTheAnswerArrived()
	{
		Shames shames = Shames.of(response("{\"shames\":[{\"rsn\":\"Runny Sharts\",\"by\":\"Lavasockz\",\"secondsLeft\":60}]}"), NOW);
		assertTrue(shames.isShamed("Runny Sharts", NOW));
		assertTrue(shames.isShamed("Runny Sharts", NOW + 59_999));
		assertFalse(shames.isShamed("Runny Sharts", NOW + 60_000));
		assertFalse(shames.isShamed("Bob", NOW));
		assertFalse(shames.isEmpty(NOW));
		assertTrue(shames.isEmpty(NOW + 60_000));
	}

	@Test
	public void matchesNamesAsChatSpellsThem()
	{
		Shames shames = Shames.of(response("{\"shames\":[{\"rsn\":\"Runny Sharts\",\"secondsLeft\":30}]}"), NOW);
		assertTrue(shames.isShamed("runny sharts", NOW));
		assertTrue(shames.isShamed("Runny Sharts", NOW));
		assertTrue(shames.isShamed("Runny_Sharts", NOW));
	}

	@Test
	public void neverLastsLongerThanAShameCan()
	{
		Shames shames = Shames.of(response("{\"shames\":[{\"rsn\":\"Bob\",\"secondsLeft\":999999}]}"), NOW);
		assertTrue(shames.isShamed("Bob", NOW + Shames.DURATION_MILLIS - 1));
		assertFalse(shames.isShamed("Bob", NOW + Shames.DURATION_MILLIS));
	}

	@Test
	public void skipsBrokenEntries()
	{
		Shames shames = Shames.of(response("{\"shames\":[null,{\"secondsLeft\":30},{\"rsn\":\"Bob\",\"secondsLeft\":-5},{\"rsn\":\"Ann\",\"secondsLeft\":0}]}"), NOW);
		assertTrue(shames.isEmpty(NOW));
		assertTrue(Shames.of(response("{}"), NOW).isEmpty(NOW));
	}

	@Test
	public void capsHowManyItKeeps()
	{
		List<SyncModels.ActiveShame> list = new ArrayList<>();
		for (int i = 0; i < Shames.MAX_SHAMES + 10; i++)
		{
			list.add(new SyncModels.ActiveShame("P" + i, "Lavasockz", 60));
		}
		SyncModels.ShamesResponse r = response(new Gson().toJson(java.util.Collections.singletonMap("shames", list)));
		Shames shames = Shames.of(r, NOW);
		int shamed = 0;
		for (int i = 0; i < Shames.MAX_SHAMES + 10; i++)
		{
			if (shames.isShamed("P" + i, NOW))
			{
				shamed++;
			}
		}
		assertEquals(Shames.MAX_SHAMES, shamed);
	}

	@Test
	public void ownShameShowsBeforeTheNextRefresh()
	{
		Shames shamed = Shames.NONE.with("Bob", NOW);
		assertTrue(shamed.isShamed("bob", NOW + Shames.DURATION_MILLIS - 1));
		assertFalse(shamed.isShamed("bob", NOW + Shames.DURATION_MILLIS));
		assertFalse(shamed.without("BOB").isShamed("Bob", NOW));
		// copies: the original is untouched
		assertTrue(Shames.NONE.isEmpty(NOW));
	}

	@Test
	public void onlyTheFourLeadersCanShame()
	{
		assertTrue(ClanLeaders.isLeader("UimFartQueef"));
		assertTrue(ClanLeaders.isLeader("JoeBoob"));
		assertTrue(ClanLeaders.isLeader("Lavasockz"));
		assertTrue(ClanLeaders.isLeader("DAYLlGHT"));
		// DAYLlGHT has a lowercase L, not an I
		assertFalse(ClanLeaders.isLeader("DAYLIGHT"));
		assertFalse(ClanLeaders.isLeader("Runny Sharts"));
		assertFalse(ClanLeaders.isLeader(null));
	}
}
