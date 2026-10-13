package com.corclan.raid;

import com.corclan.sync.SyncModels;
import com.google.gson.Gson;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class RaidListingTest
{
	private static List<RaidListing> board(String raids)
	{
		return RaidListing.board(new Gson().fromJson("{\"raids\":[" + raids + "]}", SyncModels.RaidsResponse.class));
	}

	@Test
	public void readsAndCleansTheBoard()
	{
		List<RaidListing> board = board(
			"{\"rsn\":\"<col=ff0000>Bob</col>\",\"raid\":\"tob\",\"filled\":3,\"size\":5,\"world\":330,"
				+ "\"note\":\"need <b>tank</b>\",\"postedAt\":\"2026-10-12T20:00:00.000Z\"},"
				+ "{\"rsn\":\"Al\",\"raid\":\"cox\",\"filled\":2,\"size\":40,\"world\":null},"
				+ "{\"rsn\":\"Bad\",\"raid\":\"tob\",\"filled\":3,\"size\":6},"
				+ "{\"rsn\":\"Bad\",\"raid\":\"nightmare\",\"filled\":1,\"size\":5},"
				+ "{\"rsn\":\"Bad\",\"raid\":\"toa\",\"filled\":5,\"size\":4}");
		assertEquals(2, board.size());
		RaidListing bob = board.get(0);
		assertEquals("Bob", bob.name);
		assertEquals(Raid.TOB, bob.raid);
		assertEquals("3/5", bob.slots());
		assertEquals(330, bob.world);
		assertEquals("need tank", bob.note);
		assertEquals(1791835200000L, bob.postedAt);
		assertEquals(0, board.get(1).world);
		assertEquals("", board.get(1).note);
	}

	@Test
	public void keyTellsANewGroupFromAnUpdate()
	{
		RaidListing first = board("{\"rsn\":\"Bob\",\"raid\":\"tob\",\"filled\":2,\"size\":5,\"postedAt\":\"2026-10-12T20:00:00.000Z\"}").get(0);
		RaidListing update = board("{\"rsn\":\"bob\",\"raid\":\"tob\",\"filled\":4,\"size\":5,\"postedAt\":\"2026-10-12T20:00:00.000Z\"}").get(0);
		RaidListing other = board("{\"rsn\":\"Bob\",\"raid\":\"toa\",\"filled\":2,\"size\":5,\"postedAt\":\"2026-10-12T20:05:00.000Z\"}").get(0);
		assertEquals(first.key(), update.key());
		assertNotEquals(first.key(), other.key());
		assertTrue(first.isHost("BOB"));
		assertFalse(first.isHost(null));
	}

	@Test
	public void adKeepsSizesInRangeAndCleansTheNote()
	{
		RaidAd ad = new RaidAd(Raid.TOB, 9, 9, "  <col=ff0000>need</col> a   tank ");
		assertEquals(5, ad.size);
		assertEquals(5, ad.filled);
		assertEquals("need a tank", ad.note);
		RaidAd low = new RaidAd(Raid.COX, 0, 1, null);
		assertEquals(2, low.size);
		assertEquals(1, low.filled);
		assertEquals("", low.note);
		assertEquals(RaidAd.MAX_NOTE, new RaidAd(Raid.TOA, 1, 4, String.join("", Collections.nCopies(100, "x"))).note.length());
	}

	@Test
	public void hostIsAwayAfterSevenQuietMinutes()
	{
		RaidListing l = board("{\"rsn\":\"Bob\",\"raid\":\"tob\",\"filled\":2,\"size\":5,\"postedAt\":\"2026-10-12T20:00:00.000Z\",\"updatedAt\":\"2026-10-12T20:10:00.000Z\"}").get(0);
		long updated = 1791835800000L;
		assertFalse(l.isAway(updated + RaidListing.AWAY_MILLIS));
		assertTrue(l.isAway(updated + RaidListing.AWAY_MILLIS + 1));
	}
}
