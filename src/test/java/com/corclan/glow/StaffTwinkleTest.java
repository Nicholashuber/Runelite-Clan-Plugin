package com.corclan.glow;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import net.runelite.api.clan.ClanRank;
import org.junit.Test;

public class StaffTwinkleTest
{
	@Test
	public void administratorsUpToTheDeputyOwnerTwinkleGold()
	{
		assertTrue(GlowPicks.isStaff(new ClanRank(100)));
		assertTrue(GlowPicks.isStaff(new ClanRank(110)));
		assertTrue(GlowPicks.isStaff(new ClanRank(125)));
	}

	@Test
	public void membersTheOwnerAndJModsDoNot()
	{
		assertFalse(GlowPicks.isStaff(new ClanRank(99)));
		assertFalse(GlowPicks.isStaff(new ClanRank(0)));
		assertFalse(GlowPicks.isStaff(ClanRank.OWNER));
		assertFalse(GlowPicks.isStaff(new ClanRank(127)));
		assertFalse(GlowPicks.isStaff(null));
	}
}
