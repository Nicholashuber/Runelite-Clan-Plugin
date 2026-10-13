package com.corclan.ui;

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
}
