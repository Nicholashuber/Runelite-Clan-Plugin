package com.corclan.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ClanApiTest
{
	@Test
	public void successIsOk()
	{
		assertEquals(ClanApi.Outcome.OK, ClanApi.classify(200, null));
		assertEquals(ClanApi.Outcome.OK, ClanApi.classify(204, null));
	}

	@Test
	public void takenNamesAndOtherClansAreToldApart()
	{
		assertEquals(ClanApi.Outcome.NAME_TAKEN, ClanApi.classify(409, "name_taken"));
		assertEquals(ClanApi.Outcome.WRONG_CLAN, ClanApi.classify(403, "wrong_clan"));
		// an unreadable body falls back to the status
		assertEquals(ClanApi.Outcome.NAME_TAKEN, ClanApi.classify(409, null));
		assertEquals(ClanApi.Outcome.WRONG_CLAN, ClanApi.classify(403, null));
		assertTrue(ClanApi.Outcome.NAME_TAKEN.blocksReporter());
		assertTrue(ClanApi.Outcome.WRONG_CLAN.blocksReporter());
	}

	@Test
	public void outagesAndRateLimitsAreRetried()
	{
		assertEquals(ClanApi.Outcome.RETRY, ClanApi.classify(429, null));
		assertEquals(ClanApi.Outcome.RETRY, ClanApi.classify(500, null));
		assertEquals(ClanApi.Outcome.RETRY, ClanApi.classify(503, "unavailable"));
		assertFalse(ClanApi.Outcome.RETRY.blocksReporter());
	}

	@Test
	public void requestsTheServerWillNeverTakeAreNotRetried()
	{
		assertEquals(ClanApi.Outcome.REFUSED, ClanApi.classify(400, "invalid_events"));
		assertEquals(ClanApi.Outcome.REFUSED, ClanApi.classify(403, "banned"));
		assertEquals(ClanApi.Outcome.REFUSED, ClanApi.classify(404, null));
		assertFalse(ClanApi.Outcome.REFUSED.blocksReporter());
		assertFalse(ClanApi.Outcome.OK.blocksReporter());
	}

	@Test
	public void theServerIsTheClansOwn()
	{
		assertEquals("https://cor-clan-api-production.up.railway.app/v1/clan", ClanApi.BASE_URL.resolve("v1/clan").toString());
	}
}
