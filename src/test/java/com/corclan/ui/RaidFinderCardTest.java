package com.corclan.ui;

import com.corclan.raid.Raid;
import com.corclan.raid.RaidAd;
import com.corclan.raid.RaidListing;
import com.corclan.sync.SyncModels;
import com.google.gson.Gson;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class RaidFinderCardTest
{
	@Test
	public void summaryNamesTheFirstTwoGroups()
	{
		List<RaidListing> board = RaidListing.board(new Gson().fromJson("{\"raids\":["
			+ "{\"rsn\":\"A\",\"raid\":\"tob\",\"filled\":3,\"size\":5},"
			+ "{\"rsn\":\"B\",\"raid\":\"cox\",\"filled\":2,\"size\":5},"
			+ "{\"rsn\":\"C\",\"raid\":\"toa\",\"filled\":1,\"size\":4}]}", SyncModels.RaidsResponse.class));
		assertEquals("Looking: ToB 3/5, CoX 2/5 and 1 more.", RaidFinderCard.summary(board));
		assertEquals("Nobody is looking for a raid party right now.", RaidFinderCard.summary(Collections.emptyList()));
	}

	@Test
	public void yourGroupFollowsTheBoardOnceInside()
	{
		RaidAd ad = new RaidAd(Raid.TFA, 1, 8, "");
		assertEquals("Not looking right now.", RaidFinderCard.mineText(null, null, ""));
		assertEquals("Looking for The Fractured Archive, 1/8 filled.", RaidFinderCard.mineText(ad, null, ""));
		assertEquals("Raiding The Fractured Archive now (1/8)", RaidFinderCard.mineText(ad, listing("\"raiding\":true"), ""));
		assertEquals("Raiding The Fractured Archive now (1/8)\nRoom 1/5: Archive Guardian",
			RaidFinderCard.mineText(ad, listing("\"raiding\":true,\"room\":\"Archive Guardian\""), ""));
		// our own client knows the room before the board does
		assertEquals("Raiding The Fractured Archive now (1/8)\nRoom 2/5: Ezekiel",
			RaidFinderCard.mineText(ad, listing("\"raiding\":true"), "Room 2/5: Ezekiel"));
	}

	private static RaidListing listing(String extra)
	{
		return RaidListing.board(new Gson().fromJson("{\"raids\":[{\"rsn\":\"Me\",\"raid\":\"tfa\",\"filled\":1,\"size\":8,"
			+ extra + "}]}", SyncModels.RaidsResponse.class)).get(0);
	}
}
