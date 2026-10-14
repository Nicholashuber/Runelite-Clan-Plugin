package com.corclan.raid;

import com.corclan.sync.SyncModels;
import com.google.gson.Gson;
import java.util.List;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class RaidLinesTest
{
	private static List<RaidListing> board(String raids)
	{
		return RaidListing.board(new Gson().fromJson("{\"raids\":[" + raids + "]}", SyncModels.RaidsResponse.class));
	}

	@Test
	public void newGroupLine()
	{
		assertEquals("CoR raid finder: Bob is looking for ToB (3/5) on W330: need tank", RaidLines.newGroup(board(
			"{\"rsn\":\"Bob\",\"raid\":\"tob\",\"filled\":3,\"size\":5,\"world\":330,\"note\":\"need tank\"}").get(0)));
		assertEquals("CoR raid finder: Al is looking for CoX (2/5)", RaidLines.newGroup(board(
			"{\"rsn\":\"Al\",\"raid\":\"cox\",\"filled\":2,\"size\":5}").get(0)));
	}

	@Test
	public void raidingLine()
	{
		RaidListing l = board("{\"rsn\":\"Bob\",\"raid\":\"tob\",\"filled\":4,\"size\":5,\"raiding\":true}").get(0);
		assertEquals("CoR raid finder: Bob's ToB party is raiding now (4/5)", RaidLines.raiding(l));
		assertEquals("CoR raid finder: ToB 4/5 raiding (Bob). See the CoR side panel.", RaidLines.advert(java.util.Collections.singletonList(l)));
	}

	@Test
	public void advertNamesThreeGroups()
	{
		assertEquals("CoR raid finder: ToB 3/5 (Bob, W330), CoX 2/5 (Al), ToA 1/4 (Cy) and 1 more. See the CoR side panel.",
			RaidLines.advert(board("{\"rsn\":\"Bob\",\"raid\":\"tob\",\"filled\":3,\"size\":5,\"world\":330},"
				+ "{\"rsn\":\"Al\",\"raid\":\"cox\",\"filled\":2,\"size\":5},"
				+ "{\"rsn\":\"Cy\",\"raid\":\"toa\",\"filled\":1,\"size\":4},"
				+ "{\"rsn\":\"Di\",\"raid\":\"toa\",\"filled\":1,\"size\":4}")));
	}
}
